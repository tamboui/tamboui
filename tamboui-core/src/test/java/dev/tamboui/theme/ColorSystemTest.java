package dev.tamboui.theme;

import org.junit.jupiter.api.Test;

import dev.tamboui.style.Color;

import static org.junit.jupiter.api.Assertions.*;

class ColorSystemTest {

    @Test
    void testGenerateBaseColors() {
        Theme theme = new Theme.Builder()
            .name("test")
            .primary("#0000ff")
            .secondary("#00ff00")
            .error("#ff0000")
            .success("#00ff00")
            .warning("#ffff00")
            .build();

        ColorSystem system = theme.generate();

        assertNotNull(system.get("primary"));
        assertNotNull(system.get("secondary"));
        assertNotNull(system.get("error"));
        assertNotNull(system.get("success"));
        assertNotNull(system.get("warning"));
    }

    @Test
    void testGenerateDerivedColors() {
        Theme theme = new Theme.Builder()
            .name("test")
            .primary("#808080")
            .secondary("#00ff00")
            .error("#ff0000")
            .success("#00ff00")
            .warning("#ffff00")
            .luminositySpread(0.15f)
            .build();

        ColorSystem system = theme.generate();

        // Shades
        assertNotNull(system.get("primary-light"));
        assertNotNull(system.get("primary-dark"));
        assertNotNull(system.get("primary-muted"));

        // Text colors
        assertNotNull(system.get("text"));
        assertNotNull(system.get("text-muted"));
        assertNotNull(system.get("text-disabled"));

        // UI colors
        assertNotNull(system.get("border"));
        assertNotNull(system.get("border-focus"));
        assertNotNull(system.get("selection-bg"));
    }

    @Test
    void testGenerateAutoDerivesBackground() {
        Theme theme = new Theme.Builder()
            .name("test")
            .primary("#0000ff")
            .secondary("#00ff00")
            .error("#ff0000")
            .success("#00ff00")
            .warning("#ffff00")
            .dark(true)
            .build();

        ColorSystem system = theme.generate();

        Color bg = system.get("background");
        assertNotNull(bg);
        // Should be dark (low luminance)
        Color.Rgb rgb = bg.toRgb();
        assertTrue(rgb.r() < 50);
    }

    @Test
    void testVariableOverride() {
        Theme theme = new Theme.Builder()
            .name("test")
            .primary("#0000ff")
            .secondary("#00ff00")
            .error("#ff0000")
            .success("#00ff00")
            .warning("#ffff00")
            .variable("border-focus", "#ff00ff")
            .build();

        ColorSystem system = theme.generate();

        Color borderFocus = system.get("border-focus");
        Color.Rgb rgb = borderFocus.toRgb();
        assertEquals(255, rgb.r());
        assertEquals(0, rgb.g());
        assertEquals(255, rgb.b());
    }
}
