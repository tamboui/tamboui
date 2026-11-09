# Cross-terminal screenshot tests (issue #19). EXPERIMENTAL prototype.
#
# Boots a NixOS VM running Sway + a real terminal emulator, runs a prebuilt self-contained
# TamboUI demo jar in it (jbang export fatjar -> `java -jar`), waits for the painted UI via
# OCR (machine.wait_for_text), then screenshots the VM framebuffer. No marker file, no flag,
# and no TamboUI source change: readiness is detected from outside the app.
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

  makeScreenshotTest = { terminal, terminalCommand, terminalPackages, readyText, xwayland ? false }:
    pkgs.testers.nixosTest {
      name = "tamboui-screenshot-${terminal}";

      # wait_for_text OCRs the framebuffer; this pulls in tesseract + imagemagick.
      enableOCR = true;

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

        with subtest("wait for first frame (OCR the painted UI; no app cooperation)"):
          machine.wait_for_text("${readyText}")
          machine.screenshot("screenshot-${terminal}")
      '';
    };

  run = "${jre}/bin/java -jar /tmp/app/demo.jar";
in
{
  # foot: wayland-native, supports sixel.
  foot = makeScreenshotTest {
    terminal = "foot";
    terminalCommand = "foot ${run}";
    terminalPackages = [ pkgs.foot ];
    readyText = "Terminal Capabilities"; # image-demo: the capabilities panel title
  };

  # xterm in VT340 mode: classic sixel path, via xwayland.
  xterm-vt340 = makeScreenshotTest {
    terminal = "xterm-vt340";
    terminalCommand = "xterm -ti vt340 -fa 'Noto Sans Mono' -fs 16 -e ${run}";
    terminalPackages = [ pkgs.xterm ];
    xwayland = true;
    readyText = "Terminal Capabilities"; # image-demo: the capabilities panel title
  };
}
