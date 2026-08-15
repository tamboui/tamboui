/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.theme;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import dev.tamboui.style.Color;

/**
 * Registry of built-in and user-registered themes.
 */
public final class ThemeRegistry {

    private static final Map<String, Theme> THEMES = new HashMap<>();

    static {
        // Auto-terminal theme - adapts to terminal's ANSI colors
        THEMES.put("auto-terminal", new Theme.Builder()
            .name("auto-terminal")
            .primary(Color.BLUE)
            .secondary(Color.CYAN)
            .error(Color.RED)
            .success(Color.GREEN)
            .warning(Color.YELLOW)
            .accent(Color.LIGHT_BLUE)
            .info(Color.LIGHT_CYAN)
            .dark(true)
            .build());

        // TamboUI branded theme (cyan/teal)
        THEMES.put("tamboui-default", new Theme.Builder()
            .name("tamboui-default")
            .primary("#00ffff")
            .secondary("#0080ff")
            .error("#ff6b6b")
            .success("#4caf50")
            .warning("#ffeb3b")
            .accent("#00ffff")
            .info("#00bcd4")
            .background("#000000")
            .surface("#1a1a1a")
            .panel("#333333")
            .dark(true)
            .build());

        // Nord - Arctic, north-bluish color palette
        // Source: https://www.nordtheme.com/
        // Palette: https://www.nordtheme.com/docs/colors-and-palettes
        THEMES.put("nord", new Theme.Builder()
            .name("nord")
            .primary("#88c0d0")
            .secondary("#81a1c1")
            .error("#bf616a")
            .success("#a3be8c")
            .warning("#ebcb8b")
            .accent("#b48ead")
            .info("#8fbcbb")
            .background("#2e3440")
            .surface("#3b4252")
            .panel("#434c5e")
            .foreground("#eceff4")
            .dark(true)
            .variable("text-muted", "#d8dee9")
            .variable("text-disabled", "#4c566a")
            .variable("border", "#4c566a")
            .variable("border-muted", "#434c5e")
            .variable("text-on-primary", "#2e3440")
            .build());

        // Gruvbox Dark - Retro groove color scheme
        // Source: https://github.com/morhetz/gruvbox
        // Palette: https://github.com/morhetz/gruvbox#dark-mode
        THEMES.put("gruvbox-dark", new Theme.Builder()
            .name("gruvbox-dark")
            .primary("#83a598")
            .secondary("#d3869b")
            .error("#fb4934")
            .success("#b8bb26")
            .warning("#fabd2f")
            .accent("#fe8019")
            .info("#8ec07c")
            .background("#282828")
            .surface("#3c3836")
            .panel("#504945")
            .foreground("#ebdbb2")
            .dark(true)
            .variable("text-on-primary", "#282828")
            .build());

        // Dracula - Dark theme with vibrant colors
        // Source: https://draculatheme.com/
        // Specification: https://spec.draculatheme.com/
        THEMES.put("dracula", new Theme.Builder()
            .name("dracula")
            .primary("#bd93f9")
            .secondary("#8be9fd")
            .error("#ff5555")
            .success("#50fa7b")
            .warning("#f1fa8c")
            .accent("#ff79c6")
            .info("#8be9fd")
            .background("#282a36")
            .surface("#44475a")
            .panel("#44475a")
            .foreground("#f8f8f2")
            .dark(true)
            .variable("text-on-primary", "#282a36")
            .build());

        // Catppuccin Mocha - Soothing pastel theme for cozy coding
        // Source: https://github.com/catppuccin/catppuccin
        // Palette: https://catppuccin.com/palette
        THEMES.put("catppuccin-mocha", new Theme.Builder()
            .name("catppuccin-mocha")
            .primary("#89b4fa")
            .secondary("#cba6f7")
            .error("#f38ba8")
            .success("#a6e3a1")
            .warning("#f9e2af")
            .accent("#f5c2e7")
            .info("#94e2d5")
            .background("#1e1e2e")
            .surface("#313244")
            .panel("#45475a")
            .foreground("#cdd6f4")
            .dark(true)
            .variable("text-on-primary", "#1e1e2e")
            .build());

        // Tokyo Night - Clean dark theme inspired by Tokyo's night skyline
        // Source: https://github.com/enkia/tokyo-night-vscode-theme
        // Also: https://github.com/folke/tokyonight.nvim
        THEMES.put("tokyo-night", new Theme.Builder()
            .name("tokyo-night")
            .primary("#7aa2f7")
            .secondary("#bb9af7")
            .error("#f7768e")
            .success("#9ece6a")
            .warning("#e0af68")
            .accent("#ff9e64")
            .info("#7dcfff")
            .background("#1a1b26")
            .surface("#24283b")
            .panel("#414868")
            .foreground("#a9b1d6")
            .dark(true)
            .variable("text-on-primary", "#24283b")
            .build());

        // TODO: Light themes when requested
        // - gruvbox-light: https://github.com/morhetz/gruvbox#light-mode
        // - catppuccin-latte: https://catppuccin.com/palette
        // - solarized-light: https://ethanschoonover.com/solarized/
    }

    /**
     * Gets a theme by name.
     *
     * @param name the theme name
     * @return the theme, or empty if not found
     */
    public static Optional<Theme> get(String name) {
        return Optional.ofNullable(THEMES.get(name));
    }

    /**
     * Registers a custom theme.
     *
     * @param theme the theme to register
     */
    public static void register(Theme theme) {
        THEMES.put(theme.name(), theme);
    }

    /**
     * Gets all registered themes.
     *
     * @return a map of theme name to theme
     */
    public static Map<String, Theme> all() {
        return new HashMap<>(THEMES);
    }

    private ThemeRegistry() {
        // Utility class
    }
}
