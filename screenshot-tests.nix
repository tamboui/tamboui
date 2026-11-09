# Cross-terminal screenshot tests (issue #19). EXPERIMENTAL prototype.
#
# Boots a NixOS VM running Sway + a real terminal emulator, runs a prebuilt self-contained
# TamboUI demo jar in it (jbang export fatjar -> `java -jar`), then screenshots the VM
# framebuffer unconditionally after a short settle sleep (never fails), so we always get a
# PNG to eyeball. No marker file, no flag, no TamboUI source change.
# (OCR-based wait_for_text proved unreliable on TUI glyphs; revisit once the basics work.)
#
# Adapted from benjajaja/ratatui-image/screenshot-tests.nix.
#
# `demoJar` is the path to the exported fat jar. The workflow passes it impurely:
#   nix build --impure --expr 'import ./screenshot-tests.nix {
#     pkgs = import <nixpkgs> {}; demoJar = /abs/path/demo.jar; }.foot'
# Pure upgrade (later): make demoJar a fixed-output derivation running `jbang export`.

{ pkgs, demoJar }:

let
  jre = pkgs.jdk_headless;

  makeScreenshotTest = { terminal, terminalCommand, terminalPackages, xwayland ? false }:
    pkgs.testers.nixosTest {
      name = "tamboui-screenshot-${terminal}";

      nodes.machine = { pkgs, ... }: {
        virtualisation.memorySize = 4096;
        virtualisation.resolution = { x = 1920; y = 1080; };

        programs.sway.enable = true;
        programs.xwayland.enable = xwayland;

        services.displayManager.sddm.enable = true;
        services.displayManager.sddm.wayland.enable = true;
        services.displayManager.autoLogin = { enable = true; user = "test"; };
        services.displayManager.defaultSession = "sway";

        users.users.test = { isNormalUser = true; extraGroups = [ "wheel" "video" ]; };

        fonts.packages = with pkgs; [
          unifont noto-fonts noto-fonts-cjk-sans noto-fonts-color-emoji
          dejavu_fonts fira-mono
        ];

        environment.systemPackages = terminalPackages ++ [ jre ];
      };

      testScript = ''
        machine.wait_for_unit("graphical.target")
        machine.wait_until_succeeds("pgrep -f sway")

        machine.succeed("mkdir -p /tmp/app")
        machine.copy_from_host("${demoJar}", "/tmp/app/demo.jar")

        machine.wait_until_succeeds(
          "systemd-run --uid=test --setenv=XDG_RUNTIME_DIR=/run/user/1000 "
          "--setenv=WAYLAND_DISPLAY=wayland-1 -- swaymsg -t get_version"
        )

        machine.succeed("""
          systemd-run --uid=test \
            --setenv=XDG_RUNTIME_DIR=/run/user/1000 \
            --setenv=WAYLAND_DISPLAY=wayland-1 \
            ${if xwayland then "--setenv=DISPLAY=:0" else ""} \
            --working-directory=/tmp/app \
            -- ${terminalCommand}
        """)

        # Give the JVM + demo time to start and paint, then screenshot unconditionally.
        machine.sleep(40)
        # Diagnostics: the demo's own output + whether a window exists.
        print("=== demo.log ===")
        print(machine.execute("cat /tmp/demo.log 2>&1")[1])
        print("=== sway tree (window titles) ===")
        print(machine.execute(
          "systemd-run --uid=test --setenv=XDG_RUNTIME_DIR=/run/user/1000 "
          "--setenv=WAYLAND_DISPLAY=wayland-1 --pipe --wait -- "
          "swaymsg -t get_tree 2>&1 | grep -iE '\"name\"|app_id|pid' || true")[1])
        machine.screenshot("screenshot-${terminal}")
      '';
    };

  # Runs the demo, logs everything, and keeps the terminal alive even if the JVM exits
  # (so a crash still leaves a visible window + a readable /tmp/demo.log).
  launcher = pkgs.writeShellScript "run-demo" ''
    echo "launcher: starting $(date)" > /tmp/demo.log
    ${jre}/bin/java -jar /tmp/app/demo.jar >> /tmp/demo.log 2>&1
    echo "launcher: java exited $?" >> /tmp/demo.log
    sleep 600
  '';
in
{
  # foot: wayland-native, supports sixel.
  foot = makeScreenshotTest {
    terminal = "foot";
    terminalCommand = "foot ${launcher}";
    terminalPackages = [ pkgs.foot ];
  };

  # xterm in VT340 mode: classic sixel path, via xwayland.
  xterm-vt340 = makeScreenshotTest {
    terminal = "xterm-vt340";
    terminalCommand = "xterm -ti vt340 -fa 'Noto Sans Mono' -fs 16 -e ${launcher}";
    terminalPackages = [ pkgs.xterm ];
    xwayland = true;
  };
}
