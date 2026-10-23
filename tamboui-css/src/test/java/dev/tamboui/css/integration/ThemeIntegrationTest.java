package dev.tamboui.css.integration;

import org.junit.jupiter.api.Test;

import dev.tamboui.css.engine.StyleEngine;
import dev.tamboui.css.theme.ThemeEngine;
import dev.tamboui.theme.Theme;
import dev.tamboui.theme.ThemeRegistry;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for theme system end-to-end flow.
 */
class ThemeIntegrationTest {

    @Test
    void testThemeToCSS_NordTheme() {
        // 1. Get theme
        Theme nord = ThemeRegistry.get("nord")
            .orElseThrow(() -> new IllegalStateException("nord not found"));

        // 2. Create theme engine
        ThemeEngine themeEngine = new ThemeEngine(nord);

        // 3. Create style engine
        StyleEngine styleEngine = StyleEngine.create();

        // 4. Inject theme variables
        themeEngine.injectVariables(styleEngine);

        // 5. Verify variables are resolvable
        String primary = styleEngine.resolveVariable("primary");
        assertNotNull(primary);
        assertEquals("#88c0d0", primary);

        String primaryLight = styleEngine.resolveVariable("primary-light");
        assertNotNull(primaryLight);
    }

    @Test
    void testThemeSwitching() {
        ThemeEngine themeEngine = new ThemeEngine();
        StyleEngine styleEngine = StyleEngine.create();

        // Start with nord
        themeEngine.setTheme("nord");
        themeEngine.injectVariables(styleEngine);

        String nordPrimary = styleEngine.resolveVariable("primary");
        assertEquals("#88c0d0", nordPrimary);

        // Switch to gruvbox
        themeEngine.setTheme("gruvbox-dark");
        themeEngine.injectVariables(styleEngine);

        String gruvboxPrimary = styleEngine.resolveVariable("primary");
        assertEquals("#83a598", gruvboxPrimary);
    }

    @Test
    void testAutoTerminalThemeUsesANSI() {
        Theme autoTerminal = ThemeRegistry.get("auto-terminal")
            .orElseThrow(() -> new IllegalStateException("auto-terminal not found"));
        ThemeEngine themeEngine = new ThemeEngine(autoTerminal);
        StyleEngine styleEngine = StyleEngine.create();

        themeEngine.injectVariables(styleEngine);

        String primary = styleEngine.resolveVariable("primary");
        assertNotNull(primary);
        // Should be ANSI color name, not hex
        assertTrue(primary.equals("blue") || primary.startsWith("ansi") || primary.startsWith("bright"),
            "Expected ANSI color name but got: " + primary);
    }

    @Test
    void testAllBuiltInThemesWork() {
        StyleEngine styleEngine = StyleEngine.create();
        ThemeEngine themeEngine = new ThemeEngine();

        // Test all 7 built-in themes can be loaded and injected
        String[] themes = {"auto-terminal", "tamboui-default", "nord",
            "gruvbox-dark", "dracula", "catppuccin-mocha", "tokyo-night"};

        for (String themeName : themes) {
            themeEngine.setTheme(themeName);
            themeEngine.injectVariables(styleEngine);

            // Verify basic colors are available
            assertNotNull(styleEngine.resolveVariable("primary"),
                "Theme " + themeName + " missing primary color");
            assertNotNull(styleEngine.resolveVariable("background"),
                "Theme " + themeName + " missing background color");
            assertNotNull(styleEngine.resolveVariable("text"),
                "Theme " + themeName + " missing text color");
        }
    }
}
