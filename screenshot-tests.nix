# Cross-terminal screenshot tests (issue #19).
#
# Boots a NixOS VM running Sway + a real terminal emulator, runs a prebuilt self-contained
# TamboUI demo jar in it (jbang export fatjar -> `java -jar`), then screenshots the VM
# framebuffer after a short settle sleep. The screenshot is unconditional (the test never
# fails), so CI always has a PNG to review. No marker file, no flag, no TamboUI source change.
#
# Adapted from benjajaja/ratatui-image/screenshot-tests.nix.
#
# `demoJar` is the path to the exported fat jar (built for Java 21 to match the VM JRE). The
# workflow passes it impurely:
#   nix build --impure --expr '(import ./screenshot-tests.nix {
#     pkgs = import <nixpkgs> {}; demoJar = /abs/path/demo.jar; }).foot'

{ pkgs, demoJar }:

let
  jre = pkgs.jdk_headless;

  # Run the demo attached to the terminal (no output redirection: the TUI must draw to the
  # terminal, not a file). The demo blocks until quit, keeping the window on screen.
  launcher = pkgs.writeShellScript "run-demo" ''
    exec ${jre}/bin/java -jar /tmp/app/demo.jar
  '';

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
        machine.screenshot("screenshot-${terminal}")
      '';
    };
in
{
  # Wayland-native terminals.
  foot = makeScreenshotTest {
    terminal = "foot";
    terminalCommand = "foot ${launcher}";
    terminalPackages = [ pkgs.foot ];
  };

  kitty = makeScreenshotTest {
    terminal = "kitty";
    terminalCommand = "kitty ${launcher}";
    terminalPackages = [ pkgs.kitty ];
  };

  wezterm = makeScreenshotTest {
    terminal = "wezterm";
    terminalCommand = "wezterm start --always-new-process --cwd /tmp/app -- ${launcher}";
    terminalPackages = [ pkgs.wezterm ];
  };

  ghostty = makeScreenshotTest {
    terminal = "ghostty";
    terminalCommand = "ghostty -e ${launcher}";
    terminalPackages = [ pkgs.ghostty ];
  };

  alacritty = makeScreenshotTest {
    terminal = "alacritty";
    terminalCommand = "alacritty -e ${launcher}";
    terminalPackages = [ pkgs.alacritty ];
  };

  konsole = makeScreenshotTest {
    terminal = "konsole";
    terminalCommand = "konsole -e ${launcher}";
    terminalPackages = [ pkgs.kdePackages.konsole pkgs.kdePackages.qtwayland ];
  };

  # xterm in VT340 mode: classic sixel path, via xwayland.
  xterm-vt340 = makeScreenshotTest {
    terminal = "xterm-vt340";
    terminalCommand = "xterm -ti vt340 -fa 'Noto Sans Mono' -fs 16 -e ${launcher}";
    terminalPackages = [ pkgs.xterm ];
    xwayland = true;
  };
}
