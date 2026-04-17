package dev.tamboui.css.theme;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import dev.tamboui.css.cascade.CssStyleResolver;
import dev.tamboui.style.Color;
import dev.tamboui.style.ColorConverter;
import dev.tamboui.style.PropertyDefinition;
import dev.tamboui.style.StandardProperties;
import dev.tamboui.style.StylePropertyResolver;
import dev.tamboui.theme.Theme;
import dev.tamboui.theme.ThemeProperties;
import dev.tamboui.theme.ThemeRegistry;

import static org.junit.jupiter.api.Assertions.*;

class ThemeEngineTest {

    @Test
    void testDefaultTheme() {
        ThemeEngine engine = new ThemeEngine();
        assertNotNull(engine.getTheme());
        assertEquals("auto-terminal", engine.getTheme().name());
    }

    @Test
    void testSetThemeByInstance() {
        Theme nord = ThemeRegistry.get("nord")
            .orElseThrow(() -> new IllegalStateException("nord theme not found"));
        ThemeEngine engine = new ThemeEngine(nord);

        assertEquals("nord", engine.getTheme().name());
        assertNotNull(engine.getColorSystem());
    }

    @Test
    void testSetThemeByName() {
        ThemeEngine engine = new ThemeEngine();
        engine.setTheme("gruvbox-dark");

        assertEquals("gruvbox-dark", engine.getTheme().name());
    }

    @Test
    void testGetResolver() {
        ThemeEngine engine = new ThemeEngine();
        assertNotNull(engine.getResolver());
    }

    @Test
    void composeWithCss_cssValueTakesPrecedenceOverTheme() {
        ThemeEngine engine = new ThemeEngine();
        engine.setTheme("nord");

        // CSS explicitly sets a color property
        Color cssColor = Color.hex("#ff0000");
        CssStyleResolver cssResolver = CssStyleResolver.builder()
            .set(StandardProperties.COLOR, cssColor)
            .build();

        StylePropertyResolver composed = engine.composeWithCss(cssResolver);

        // CSS value wins
        Optional<Color> resolved = composed.get(StandardProperties.COLOR);
        assertTrue(resolved.isPresent());
        assertEquals(cssColor, resolved.get());
    }

    @Test
    void composeWithCss_fallsBackToThemeWhenCssIsEmpty() {
        ThemeEngine engine = new ThemeEngine();
        engine.setTheme("nord");

        // CSS defines nothing
        CssStyleResolver cssResolver = CssStyleResolver.empty();

        StylePropertyResolver composed = engine.composeWithCss(cssResolver);

        // Theme provides primary via fallback
        Optional<Color> primary = composed.get(ThemeProperties.PRIMARY);
        assertTrue(primary.isPresent(), "Theme primary should be available as fallback");

        // Theme provides border-focus via fallback
        Optional<Color> borderFocus = composed.get(ThemeProperties.BORDER_FOCUS);
        assertTrue(borderFocus.isPresent(), "Theme border-focus should be available as fallback");
    }

    @Test
    void composeWithCss_cssPropertyDoesNotAffectUnrelatedThemeProperty() {
        ThemeEngine engine = new ThemeEngine();
        engine.setTheme("dracula");

        // CSS sets only color
        CssStyleResolver cssResolver = CssStyleResolver.builder()
            .set(StandardProperties.COLOR, Color.hex("#aabbcc"))
            .build();

        StylePropertyResolver composed = engine.composeWithCss(cssResolver);

        // CSS value for color
        assertEquals(Color.hex("#aabbcc"), composed.get(StandardProperties.COLOR).orElse(null));

        // Theme value for selection-bg (not in CSS)
        Optional<Color> selectionBg = composed.get(ThemeProperties.SELECTION_BG);
        assertTrue(selectionBg.isPresent(), "Theme selection-bg should be available when CSS doesn't define it");
    }

    @Test
    void composeWithCss_returnsEmptyWhenNeitherHasProperty() {
        ThemeEngine engine = new ThemeEngine();
        engine.setTheme("nord");

        CssStyleResolver cssResolver = CssStyleResolver.empty();
        StylePropertyResolver composed = engine.composeWithCss(cssResolver);

        // A property that neither CSS nor theme defines
        PropertyDefinition<Color> unknownProp =
            PropertyDefinition.of("nonexistent-color", ColorConverter.INSTANCE);
        Optional<Color> result = composed.get(unknownProp);
        assertFalse(result.isPresent());
    }
}
