package dev.tamboui.theme;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ThemeRegistryTest {

    @Test
    void testGetAutoTerminalTheme() {
        Theme theme = ThemeRegistry.get("auto-terminal").orElse(null);
        assertNotNull(theme);
        assertEquals("auto-terminal", theme.name());
        assertTrue(theme.dark());
    }

    @Test
    void testGetNordTheme() {
        Theme theme = ThemeRegistry.get("nord").orElse(null);
        assertNotNull(theme);
        assertEquals("nord", theme.name());
    }

    @Test
    void testGetMissingTheme() {
        assertFalse(ThemeRegistry.get("nonexistent").isPresent());
    }

    @Test
    void testRegisterCustomTheme() {
        Theme custom = new Theme.Builder()
            .name("custom-test")
            .primary("#abcdef")
            .secondary("#fedcba")
            .error("#ff0000")
            .success("#00ff00")
            .warning("#ffff00")
            .build();

        ThemeRegistry.register(custom);

        Theme retrieved = ThemeRegistry.get("custom-test").orElse(null);
        assertNotNull(retrieved);
        assertEquals("custom-test", retrieved.name());
    }

    @Test
    void testAllThemes() {
        Map<String, Theme> themes = ThemeRegistry.all();
        assertTrue(themes.size() >= 7);  // At least 7 built-in themes
        assertTrue(themes.containsKey("auto-terminal"));
        assertTrue(themes.containsKey("nord"));
        assertTrue(themes.containsKey("gruvbox-dark"));
    }
}
