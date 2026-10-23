package dev.tamboui.theme;

import org.junit.jupiter.api.Test;

import dev.tamboui.style.Color;
import dev.tamboui.style.ColorConverter;
import dev.tamboui.style.PropertyDefinition;
import dev.tamboui.style.StylePropertyResolver;

import static org.junit.jupiter.api.Assertions.*;

class ThemePropertyResolverTest {

    @Test
    void testResolveColorProperty() {
        Theme theme = new Theme.Builder()
            .name("test")
            .primary("#0000ff")
            .secondary("#00ff00")
            .error("#ff0000")
            .success("#00ff00")
            .warning("#ffff00")
            .build();

        StylePropertyResolver resolver = theme.toResolver();

        PropertyDefinition<Color> primaryProp =
            PropertyDefinition.of("primary", ColorConverter.INSTANCE);

        Color primary = resolver.get(primaryProp).orElse(null);
        assertNotNull(primary);

        Color.Rgb rgb = primary.toRgb();
        assertEquals(0, rgb.r());
        assertEquals(0, rgb.g());
        assertEquals(255, rgb.b());
    }

    @Test
    void testResolveMissingProperty() {
        Theme theme = new Theme.Builder()
            .name("test")
            .primary("#0000ff")
            .secondary("#00ff00")
            .error("#ff0000")
            .success("#00ff00")
            .warning("#ffff00")
            .build();

        StylePropertyResolver resolver = theme.toResolver();

        PropertyDefinition<Color> missing =
            PropertyDefinition.of("nonexistent", ColorConverter.INSTANCE);

        assertFalse(resolver.get(missing).isPresent());
    }
}
