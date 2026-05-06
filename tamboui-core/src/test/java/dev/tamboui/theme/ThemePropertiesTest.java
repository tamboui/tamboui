package dev.tamboui.theme;

import org.junit.jupiter.api.Test;

import dev.tamboui.style.Color;
import dev.tamboui.style.StylePropertyResolver;

import static org.junit.jupiter.api.Assertions.*;

class ThemePropertiesTest {

    @Test
    void testPrimaryProperty() {
        Theme theme = new Theme.Builder()
            .name("test")
            .primary("#ff0000")
            .secondary("#00ff00")
            .error("#ff0000")
            .success("#00ff00")
            .warning("#ffff00")
            .build();

        StylePropertyResolver resolver = theme.toResolver();

        Color primary = resolver.get(ThemeProperties.PRIMARY).orElse(null);
        assertNotNull(primary);

        Color.Rgb rgb = primary.toRgb();
        assertEquals(255, rgb.r());
        assertEquals(0, rgb.g());
        assertEquals(0, rgb.b());
    }

    @Test
    void testDerivedProperty() {
        Theme theme = new Theme.Builder()
            .name("test")
            .primary("#808080")
            .secondary("#00ff00")
            .error("#ff0000")
            .success("#00ff00")
            .warning("#ffff00")
            .build();

        StylePropertyResolver resolver = theme.toResolver();

        Color primaryLight = resolver.get(ThemeProperties.PRIMARY_LIGHT).orElse(null);
        assertNotNull(primaryLight);
    }
}
