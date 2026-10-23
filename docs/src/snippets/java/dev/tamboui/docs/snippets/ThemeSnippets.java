package dev.tamboui.docs.snippets;

import dev.tamboui.color.ColorManipulation;
import dev.tamboui.css.engine.StyleEngine;
import dev.tamboui.css.theme.ThemeEngine;
import dev.tamboui.style.Color;
import dev.tamboui.style.StylePropertyResolver;
import dev.tamboui.theme.ColorSystem;
import dev.tamboui.theme.Theme;
import dev.tamboui.theme.ThemeProperties;
import dev.tamboui.theme.ThemeRegistry;
import dev.tamboui.toolkit.app.ToolkitRunner;
import dev.tamboui.tui.TuiConfig;

import java.io.IOException;

/**
 * Code snippets for the Themes documentation page.
 */
@SuppressWarnings("unused")
public class ThemeSnippets {

    // tag::quick-start[]
    void quickStart() throws Exception {
        // 1. Create engines
        StyleEngine styleEngine = StyleEngine.create();
        styleEngine.loadStylesheet("app", "/app.tcss");
        styleEngine.setActiveStylesheet("app");

        ThemeEngine themeEngine = new ThemeEngine();
        themeEngine.setTheme("nord");

        // 2. Inject theme colors as TCSS variables ($primary, $border, etc.)
        themeEngine.injectVariables(styleEngine);

        // 3. Run with theme-aware stylesheet
        try (var runner = ToolkitRunner.create(TuiConfig.defaults())) {
            runner.styleEngine(styleEngine);
            runner.run(() -> null /* your root element */);
        }
    }
    // end::quick-start[]

    // tag::custom-theme[]
    void customTheme() {
        Theme myTheme = new Theme.Builder()
            .name("my-brand")
            .primary("#ff6600")
            .secondary("#0066ff")
            .error("#cc0000")
            .success("#00cc44")
            .warning("#ffaa00")
            .accent("#ff00ff")
            .background("#0a0a0a")
            .surface("#1a1a1a")
            .dark(true)
            .build();

        ThemeRegistry.register(myTheme);

        // Now usable by name:
        ThemeEngine engine = new ThemeEngine();
        engine.setTheme("my-brand");
    }
    // end::custom-theme[]

    // tag::variable-overrides[]
    void variableOverrides() {
        Theme nordCustom = new Theme.Builder()
            .name("nord-custom")
            .primary("#88c0d0")
            .secondary("#81a1c1")
            .error("#bf616a")
            .success("#a3be8c")
            .warning("#ebcb8b")
            .background("#2e3440")
            .dark(true)
            // Override auto-generated colors:
            .variable("border", "#4c566a")
            .variable("text-muted", "#d8dee9")
            .variable("text-on-primary", "#2e3440")
            .build();
    }
    // end::variable-overrides[]

    // tag::theme-switching[]
    void themeSwitching(ThemeEngine themeEngine, StyleEngine styleEngine) {
        String[] themes = {"nord", "dracula", "catppuccin-mocha", "tokyo-night"};
        int current = 0;

        // Switch to next theme
        current = (current + 1) % themes.length;
        themeEngine.setTheme(themes[current]);
        themeEngine.injectVariables(styleEngine);
        // UI re-renders with new theme colors automatically
    }
    // end::theme-switching[]

    // tag::without-css[]
    void withoutCss() {
        ThemeEngine themeEngine = new ThemeEngine();
        themeEngine.setTheme("dracula");

        // Access colors via typed properties
        StylePropertyResolver resolver = themeEngine.getResolver();
        Color primary = resolver.get(ThemeProperties.PRIMARY).orElse(Color.BLUE);
        Color border = resolver.get(ThemeProperties.BORDER_FOCUS).orElse(Color.WHITE);
        Color textMuted = resolver.get(ThemeProperties.TEXT_MUTED).orElse(Color.GRAY);

        // Or access via ColorSystem by name
        ColorSystem colors = themeEngine.getColorSystem();
        Color selectionBg = colors.get("selection-bg");
        Color hoverBg = colors.get("hover-bg");
    }
    // end::without-css[]

    // tag::color-manipulation[]
    void colorManipulation() {
        Color base = Color.hex("#3366cc");

        // Lighten / darken (HSL space)
        Color lighter = base.lighten(0.2f);
        Color darker = base.darken(0.15f);

        // Blend two colors (linear RGB interpolation)
        Color blended = base.blend(Color.hex("#ff6600"), 0.3f);

        // Get readable text color for a background
        Color textColor = base.getContrastText();  // white or black

        // Invert
        Color inverted = base.inverse();
    }
    // end::color-manipulation[]

    // tag::color-conversions[]
    void colorConversions() {
        // RGB ↔ HSL
        float[] hsl = ColorManipulation.rgbToHsl(136, 192, 208);  // Nord primary
        // hsl = [193.3, 43.4, 67.5]  (hue°, saturation%, lightness%)

        int[] rgb = ColorManipulation.hslToRgb(193.3f, 43.4f, 67.5f);
        // rgb = [136, 192, 208]

        // RGB ↔ HSV
        float[] hsv = ColorManipulation.rgbToHsv(136, 192, 208);
        int[] rgb2 = ColorManipulation.hsvToRgb(hsv[0], hsv[1], hsv[2]);

        // Linear interpolation between two colors
        Color from = Color.hex("#88c0d0");
        Color to = Color.hex("#bf616a");
        Color mid = ColorManipulation.lerp(from, to, 0.5f);
    }
    // end::color-conversions[]
}
