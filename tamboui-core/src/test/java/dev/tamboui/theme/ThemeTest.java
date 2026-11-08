package dev.tamboui.theme;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ThemeTest {

    @Test
    void testBuilderMinimal() {
        Theme theme = new Theme.Builder()
            .name("test")
            .primary("#ff0000")
            .secondary("#00ff00")
            .error("#ff0000")
            .success("#00ff00")
            .warning("#ffff00")
            .build();

        assertEquals("test", theme.name());
        assertTrue(theme.dark());  // default
        assertNotNull(theme.primary());
    }

    @Test
    void testBuilderComplete() {
        Theme theme = new Theme.Builder()
            .name("complete")
            .primary("#0000ff")
            .secondary("#00ffff")
            .error("#ff0000")
            .success("#00ff00")
            .warning("#ffff00")
            .accent("#ff00ff")
            .info("#00ffff")
            .background("#000000")
            .surface("#1a1a1a")
            .panel("#333333")
            .foreground("#ffffff")
            .dark(false)
            .luminositySpread(0.2f)
            .textAlpha(0.9f)
            .variable("custom", "#abcdef")
            .build();

        assertEquals("complete", theme.name());
        assertFalse(theme.dark());
        assertEquals(0.2f, theme.luminositySpread(), 0.01f);
        assertEquals(0.9f, theme.textAlpha(), 0.01f);
    }

    @Test
    void testBuilderRequiresName() {
        assertThrows(IllegalStateException.class, () -> {
            new Theme.Builder()
                .primary("#ff0000")
                .build();
        });
    }

    @Test
    void testBuilderRequiresPrimary() {
        assertThrows(IllegalStateException.class, () -> {
            new Theme.Builder()
                .name("test")
                .build();
        });
    }
}
