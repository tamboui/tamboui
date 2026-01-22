/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.integration;

import dev.tamboui.css.Styleable;
import dev.tamboui.css.cascade.PseudoClassState;
import dev.tamboui.css.cascade.CssStyleResolver;
import dev.tamboui.css.engine.StyleEngine;
import dev.tamboui.layout.Alignment;
import dev.tamboui.style.Color;
import dev.tamboui.style.Modifier;
import dev.tamboui.style.Style;
import dev.tamboui.widgets.block.Padding;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive integration tests for CSS styling.
 * Tests EVERY element type with expected colors, backgrounds, borders, and modifiers.
 */
class CssDemoIntegrationTest {

    private StyleEngine styleEngine;

    @BeforeEach
    void setUp() throws IOException {
        styleEngine = StyleEngine.create();
        styleEngine.loadStylesheet("dark", "/themes/dark.tcss");
        styleEngine.loadStylesheet("light", "/themes/light.tcss");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DARK THEME TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Dark Theme")
    class DarkThemeTests {

        @BeforeEach
        void activateDarkTheme() {
            styleEngine.setActiveStylesheet("dark");
        }

        @Test
        @DisplayName("Universal selector (*) - provides white foreground on black background")
        void universalSelector_providesWhiteOnBlack() {
            Styleable element = createStyleable("TextElement", null, Collections.emptySet());
            CssStyleResolver resolved = styleEngine.resolve(element);
            Style style = resolved.toStyle();

            // Expected: color: white, background: black
            assertThat(style.fg()).isPresent();
            assertThat(style.fg().get()).isEqualTo(Color.WHITE);

            assertThat(style.bg()).isPresent();
            assertThat(style.bg().get()).isEqualTo(Color.BLACK);
        }

        @Test
        @DisplayName("Panel - black background and dark-gray border from Panel rule")
        void panel_hasBlackBackgroundAndBorder() {
            Styleable panel = createStyleable("Panel", null, Collections.emptySet());
            CssStyleResolver resolved = styleEngine.resolve(panel);
            Style style = resolved.toStyle();

            // Expected: background: black (from Panel rule)
            assertThat(style.bg()).isPresent();
            assertThat(style.bg().get()).isEqualTo(Color.BLACK);

            // Foreground from * rule
            assertThat(style.fg()).isPresent();
            assertThat(style.fg().get()).isEqualTo(Color.WHITE);

            // Expected: border-color: dark-gray (from Panel rule via $border-color variable)
            assertThat(resolved.getProperty("border-color")).isPresent();
            assertThat(resolved.getProperty("border-color").get()).isEqualTo("dark-gray");
        }

        @Test
        @DisplayName("Panel - has padding: 1")
        void panel_hasPadding() {
            Styleable panel = createStyleable("Panel", null, Collections.emptySet());
            CssStyleResolver resolved = styleEngine.resolve(panel);

            // Expected: padding: 1
            assertThat(resolved.padding()).isPresent();
            assertThat(resolved.padding().get()).isEqualTo(Padding.uniform(1));
        }

        @Test
        @DisplayName(".header - has text-align: center")
        void headerClass_hasCenterAlignment() {
            Styleable element = createStyleable("TextElement", null, setOf("header"));
            CssStyleResolver resolved = styleEngine.resolve(element);

            // Expected: text-align: center
            assertThat(resolved.alignment()).isPresent();
            assertThat(resolved.alignment().get()).isEqualTo(Alignment.CENTER);
        }

        @Test
        @DisplayName("Panel:focus - has bold text-style")
        void panelFocus_hasBoldModifier() {
            Styleable panel = createStyleable("Panel", "my-panel", Collections.emptySet());
            CssStyleResolver resolved = styleEngine.resolve(panel, PseudoClassState.ofFocused(), Collections.emptyList());

            // Expected: text-style: bold
            assertThat(resolved.modifiers()).contains(Modifier.BOLD);
        }

        @Test
        @DisplayName(".status - dark gray background (#333333) with gray border (#666666)")
        void statusClass_hasDarkGrayBackgroundAndBorder() {
            Styleable element = createStyleable("Panel", null, setOf("status"));
            CssStyleResolver resolved = styleEngine.resolve(element);
            Style style = resolved.toStyle();

            // Expected: background: #333333
            assertThat(style.bg()).isPresent();
            assertThat(style.bg().get()).isInstanceOf(Color.Rgb.class);
            Color.Rgb bg = (Color.Rgb) style.bg().get();
            assertThat(bg.r()).isEqualTo(0x33);
            assertThat(bg.g()).isEqualTo(0x33);
            assertThat(bg.b()).isEqualTo(0x33);

            // Expected: color: white
            assertThat(style.fg()).isPresent();
            assertThat(style.fg().get()).isEqualTo(Color.WHITE);

            // Expected: border-color: #666666 (in additionalProperties)
            assertThat(resolved.getProperty("border-color")).isPresent();
            assertThat(resolved.getProperty("border-color").get()).isEqualTo("#666666");
        }

        @Test
        @DisplayName(".header - cyan foreground with bold text-style")
        void headerClass_hasCyanBold() {
            Styleable element = createStyleable("TextElement", null, setOf("header"));
            CssStyleResolver resolved = styleEngine.resolve(element);
            Style style = resolved.toStyle();

            // Expected: color: cyan (from $accent variable)
            assertThat(style.fg()).isPresent();
            assertThat(style.fg().get()).isEqualTo(Color.CYAN);

            // Expected: text-style: bold
            assertThat(resolved.modifiers()).contains(Modifier.BOLD);

            // Background from * rule
            assertThat(style.bg()).isPresent();
            assertThat(style.bg().get()).isEqualTo(Color.BLACK);
        }

        @Test
        @DisplayName(".primary - cyan foreground with bold")
        void primaryClass_hasCyanBold() {
            Styleable element = createStyleable("TextElement", null, setOf("primary"));
            CssStyleResolver resolved = styleEngine.resolve(element);
            Style style = resolved.toStyle();

            // Expected: color: cyan
            assertThat(style.fg()).isPresent();
            assertThat(style.fg().get()).isEqualTo(Color.CYAN);

            // Expected: text-style: bold
            assertThat(resolved.modifiers()).contains(Modifier.BOLD);
        }

        @Test
        @DisplayName(".secondary - dark-gray foreground")
        void secondaryClass_hasDarkGray() {
            Styleable element = createStyleable("TextElement", null, setOf("secondary"));
            CssStyleResolver resolved = styleEngine.resolve(element);
            Style style = resolved.toStyle();

            // Expected: color: dark-gray
            assertThat(style.fg()).isPresent();
            assertThat(style.fg().get()).isEqualTo(Color.DARK_GRAY);
        }

        @Test
        @DisplayName(".warning - light-yellow foreground")
        void warningClass_hasLightYellow() {
            Styleable element = createStyleable("TextElement", null, setOf("warning"));
            CssStyleResolver resolved = styleEngine.resolve(element);
            Style style = resolved.toStyle();

            // Expected: color: light-yellow
            assertThat(style.fg()).isPresent();
            assertThat(style.fg().get()).isEqualTo(Color.LIGHT_YELLOW);
        }

        @Test
        @DisplayName(".error - light-red bold foreground")
        void errorClass_hasLightRedBold() {
            Styleable element = createStyleable("TextElement", null, setOf("error"));
            CssStyleResolver resolved = styleEngine.resolve(element);
            Style style = resolved.toStyle();

            // Expected: color: light-red, text-style: bold
            assertThat(style.fg()).isPresent();
            assertThat(style.fg().get()).isEqualTo(Color.LIGHT_RED);
            assertThat(resolved.modifiers()).contains(Modifier.BOLD);
        }

        @Test
        @DisplayName(".success - light-green foreground")
        void successClass_hasLightGreen() {
            Styleable element = createStyleable("TextElement", null, setOf("success"));
            CssStyleResolver resolved = styleEngine.resolve(element);
            Style style = resolved.toStyle();

            // Expected: color: light-green
            assertThat(style.fg()).isPresent();
            assertThat(style.fg().get()).isEqualTo(Color.LIGHT_GREEN);
        }

        @Test
        @DisplayName(".info - light-cyan foreground")
        void infoClass_hasLightCyan() {
            Styleable element = createStyleable("TextElement", null, setOf("info"));
            CssStyleResolver resolved = styleEngine.resolve(element);
            Style style = resolved.toStyle();

            // Expected: color: light-cyan
            assertThat(style.fg()).isPresent();
            assertThat(style.fg().get()).isEqualTo(Color.LIGHT_CYAN);
        }

        @Test
        @DisplayName(".dim - has dim text-style")
        void dimClass_hasDimModifier() {
            Styleable element = createStyleable("TextElement", null, setOf("dim"));
            CssStyleResolver resolved = styleEngine.resolve(element);

            // Expected: text-style: dim
            assertThat(resolved.modifiers()).contains(Modifier.DIM);
        }

        @Test
        @DisplayName("#theme-indicator - cyan bold")
        void themeIndicatorId_hasCyanBold() {
            Styleable element = createStyleable("TextElement", "theme-indicator", Collections.emptySet());
            CssStyleResolver resolved = styleEngine.resolve(element);
            Style style = resolved.toStyle();

            // Expected: color: cyan, text-style: bold
            assertThat(style.fg()).isPresent();
            assertThat(style.fg().get()).isEqualTo(Color.CYAN);
            assertThat(resolved.modifiers()).contains(Modifier.BOLD);
        }

        @Test
        @DisplayName("Row - inherits white on black from *")
        void row_inheritsFromUniversal() {
            Styleable row = createStyleable("Row", null, Collections.emptySet());
            CssStyleResolver resolved = styleEngine.resolve(row);
            Style style = resolved.toStyle();

            assertThat(style.fg()).isPresent();
            assertThat(style.fg().get()).isEqualTo(Color.WHITE);
            assertThat(style.bg()).isPresent();
            assertThat(style.bg().get()).isEqualTo(Color.BLACK);
        }

        @Test
        @DisplayName("Column - inherits white on black from *")
        void column_inheritsFromUniversal() {
            Styleable column = createStyleable("Column", null, Collections.emptySet());
            CssStyleResolver resolved = styleEngine.resolve(column);
            Style style = resolved.toStyle();

            assertThat(style.fg()).isPresent();
            assertThat(style.fg().get()).isEqualTo(Color.WHITE);
            assertThat(style.bg()).isPresent();
            assertThat(style.bg().get()).isEqualTo(Color.BLACK);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // LIGHT THEME TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Light Theme")
    class LightThemeTests {

        @BeforeEach
        void activateLightTheme() {
            styleEngine.setActiveStylesheet("light");
        }

        @Test
        @DisplayName("Universal selector (*) - provides black foreground on #eeeeee background")
        void universalSelector_providesBlackOnLightGray() {
            Styleable element = createStyleable("TextElement", null, Collections.emptySet());
            CssStyleResolver resolved = styleEngine.resolve(element);
            Style style = resolved.toStyle();

            // Expected: color: black
            assertThat(style.fg()).isPresent();
            assertThat(style.fg().get()).isEqualTo(Color.BLACK);

            // Expected: background: #eeeeee
            assertThat(style.bg()).isPresent();
            assertThat(style.bg().get()).isInstanceOf(Color.Rgb.class);
            Color.Rgb bg = (Color.Rgb) style.bg().get();
            assertThat(bg.r()).isEqualTo(0xee);
            assertThat(bg.g()).isEqualTo(0xee);
            assertThat(bg.b()).isEqualTo(0xee);
        }

        @Test
        @DisplayName("Panel - #eeeeee background and gray border from Panel rule")
        void panel_hasLightGrayBackgroundAndBorder() {
            Styleable panel = createStyleable("Panel", null, Collections.emptySet());
            CssStyleResolver resolved = styleEngine.resolve(panel);
            Style style = resolved.toStyle();

            // Expected: background: #eeeeee (from $bg-primary variable)
            assertThat(style.bg()).isPresent();
            assertThat(style.bg().get()).isInstanceOf(Color.Rgb.class);
            Color.Rgb bg = (Color.Rgb) style.bg().get();
            assertThat(bg.r()).isEqualTo(0xee);
            assertThat(bg.g()).isEqualTo(0xee);
            assertThat(bg.b()).isEqualTo(0xee);

            // Expected: border-color: #888888 (from Panel rule via $border-color variable)
            assertThat(resolved.getProperty("border-color")).isPresent();
            assertThat(resolved.getProperty("border-color").get()).isEqualTo("#888888");
        }

        @Test
        @DisplayName("Panel - has padding: 1")
        void panel_hasPadding() {
            Styleable panel = createStyleable("Panel", null, Collections.emptySet());
            CssStyleResolver resolved = styleEngine.resolve(panel);

            // Expected: padding: 1
            assertThat(resolved.padding()).isPresent();
            assertThat(resolved.padding().get()).isEqualTo(Padding.uniform(1));
        }

        @Test
        @DisplayName(".header - has text-align: center")
        void headerClass_hasCenterAlignment() {
            Styleable element = createStyleable("TextElement", null, setOf("header"));
            CssStyleResolver resolved = styleEngine.resolve(element);

            // Expected: text-align: center
            assertThat(resolved.alignment()).isPresent();
            assertThat(resolved.alignment().get()).isEqualTo(Alignment.CENTER);
        }

        @Test
        @DisplayName("Panel:focus - has bold text-style")
        void panelFocus_hasBoldModifier() {
            Styleable panel = createStyleable("Panel", "my-panel", Collections.emptySet());
            CssStyleResolver resolved = styleEngine.resolve(panel, PseudoClassState.ofFocused(), Collections.emptyList());

            // Expected: text-style: bold
            assertThat(resolved.modifiers()).contains(Modifier.BOLD);
        }

        @Test
        @DisplayName(".status - #c8c8c8 background with #888888 border")
        void statusClass_hasGrayBackgroundAndBorder() {
            Styleable element = createStyleable("Panel", null, setOf("status"));
            CssStyleResolver resolved = styleEngine.resolve(element);
            Style style = resolved.toStyle();

            // Expected: background: #cccccc
            assertThat(style.bg()).isPresent();
            assertThat(style.bg().get()).isInstanceOf(Color.Rgb.class);
            Color.Rgb bg = (Color.Rgb) style.bg().get();
            assertThat(bg.r()).isEqualTo(0xcc);
            assertThat(bg.g()).isEqualTo(0xcc);
            assertThat(bg.b()).isEqualTo(0xcc);

            // Expected: color: black
            assertThat(style.fg()).isPresent();
            assertThat(style.fg().get()).isEqualTo(Color.BLACK);

            // Expected: border-color: #888888 (in additionalProperties)
            assertThat(resolved.getProperty("border-color")).isPresent();
            assertThat(resolved.getProperty("border-color").get()).isEqualTo("#888888");
        }

        @Test
        @DisplayName(".header - blue foreground with bold text-style")
        void headerClass_hasBlueBold() {
            Styleable element = createStyleable("TextElement", null, setOf("header"));
            CssStyleResolver resolved = styleEngine.resolve(element);
            Style style = resolved.toStyle();

            // Expected: color: blue (from $accent variable)
            assertThat(style.fg()).isPresent();
            assertThat(style.fg().get()).isEqualTo(Color.BLUE);

            // Expected: text-style: bold
            assertThat(resolved.modifiers()).contains(Modifier.BOLD);
        }

        @Test
        @DisplayName(".primary - blue foreground with bold")
        void primaryClass_hasBlueBold() {
            Styleable element = createStyleable("TextElement", null, setOf("primary"));
            CssStyleResolver resolved = styleEngine.resolve(element);
            Style style = resolved.toStyle();

            // Expected: color: blue
            assertThat(style.fg()).isPresent();
            assertThat(style.fg().get()).isEqualTo(Color.BLUE);

            // Expected: text-style: bold
            assertThat(resolved.modifiers()).contains(Modifier.BOLD);
        }

        @Test
        @DisplayName(".secondary - gray foreground")
        void secondaryClass_hasGray() {
            Styleable element = createStyleable("TextElement", null, setOf("secondary"));
            CssStyleResolver resolved = styleEngine.resolve(element);
            Style style = resolved.toStyle();

            // Expected: color: gray
            assertThat(style.fg()).isPresent();
            assertThat(style.fg().get()).isEqualTo(Color.GRAY);
        }

        @Test
        @DisplayName(".warning - yellow foreground")
        void warningClass_hasYellow() {
            Styleable element = createStyleable("TextElement", null, setOf("warning"));
            CssStyleResolver resolved = styleEngine.resolve(element);
            Style style = resolved.toStyle();

            // Expected: color: yellow
            assertThat(style.fg()).isPresent();
            assertThat(style.fg().get()).isEqualTo(Color.YELLOW);
        }

        @Test
        @DisplayName(".error - red bold foreground")
        void errorClass_hasRedBold() {
            Styleable element = createStyleable("TextElement", null, setOf("error"));
            CssStyleResolver resolved = styleEngine.resolve(element);
            Style style = resolved.toStyle();

            // Expected: color: red, text-style: bold
            assertThat(style.fg()).isPresent();
            assertThat(style.fg().get()).isEqualTo(Color.RED);
            assertThat(resolved.modifiers()).contains(Modifier.BOLD);
        }

        @Test
        @DisplayName(".success - green foreground")
        void successClass_hasGreen() {
            Styleable element = createStyleable("TextElement", null, setOf("success"));
            CssStyleResolver resolved = styleEngine.resolve(element);
            Style style = resolved.toStyle();

            // Expected: color: green
            assertThat(style.fg()).isPresent();
            assertThat(style.fg().get()).isEqualTo(Color.GREEN);
        }

        @Test
        @DisplayName(".info - cyan foreground")
        void infoClass_hasCyan() {
            Styleable element = createStyleable("TextElement", null, setOf("info"));
            CssStyleResolver resolved = styleEngine.resolve(element);
            Style style = resolved.toStyle();

            // Expected: color: cyan
            assertThat(style.fg()).isPresent();
            assertThat(style.fg().get()).isEqualTo(Color.CYAN);
        }

        @Test
        @DisplayName(".dim - has dim text-style")
        void dimClass_hasDimModifier() {
            Styleable element = createStyleable("TextElement", null, setOf("dim"));
            CssStyleResolver resolved = styleEngine.resolve(element);

            // Expected: text-style: dim
            assertThat(resolved.modifiers()).contains(Modifier.DIM);
        }

        @Test
        @DisplayName("#theme-indicator - blue bold")
        void themeIndicatorId_hasBlueBold() {
            Styleable element = createStyleable("TextElement", "theme-indicator", Collections.emptySet());
            CssStyleResolver resolved = styleEngine.resolve(element);
            Style style = resolved.toStyle();

            // Expected: color: blue, text-style: bold
            assertThat(style.fg()).isPresent();
            assertThat(style.fg().get()).isEqualTo(Color.BLUE);
            assertThat(resolved.modifiers()).contains(Modifier.BOLD);
        }

        @Test
        @DisplayName("Row - inherits black on #eeeeee from *")
        void row_inheritsFromUniversal() {
            Styleable row = createStyleable("Row", null, Collections.emptySet());
            CssStyleResolver resolved = styleEngine.resolve(row);
            Style style = resolved.toStyle();

            assertThat(style.fg()).isPresent();
            assertThat(style.fg().get()).isEqualTo(Color.BLACK);
            assertThat(style.bg()).isPresent();
            assertThat(style.bg().get()).isInstanceOf(Color.Rgb.class);
        }

        @Test
        @DisplayName("Column - inherits black on #eeeeee from *")
        void column_inheritsFromUniversal() {
            Styleable column = createStyleable("Column", null, Collections.emptySet());
            CssStyleResolver resolved = styleEngine.resolve(column);
            Style style = resolved.toStyle();

            assertThat(style.fg()).isPresent();
            assertThat(style.fg().get()).isEqualTo(Color.BLACK);
            assertThat(style.bg()).isPresent();
            assertThat(style.bg().get()).isInstanceOf(Color.Rgb.class);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // THEME SWITCHING TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Theme Switching")
    class ThemeSwitchingTests {

        @Test
        @DisplayName("Switching themes changes all colors correctly")
        void switchingThemes_changesAllColors() {
            Styleable textElement = createStyleable("TextElement", null, Collections.emptySet());

            // Dark theme
            styleEngine.setActiveStylesheet("dark");
            Style darkStyle = styleEngine.resolve(textElement).toStyle();

            // Light theme
            styleEngine.setActiveStylesheet("light");
            Style lightStyle = styleEngine.resolve(textElement).toStyle();

            // Dark: white on black
            assertThat(darkStyle.fg().get()).isEqualTo(Color.WHITE);
            assertThat(darkStyle.bg().get()).isEqualTo(Color.BLACK);

            // Light: black on #eeeeee
            assertThat(lightStyle.fg().get()).isEqualTo(Color.BLACK);
            assertThat(lightStyle.bg().get()).isInstanceOf(Color.Rgb.class);
        }

        @Test
        @DisplayName("Status bar colors change between themes")
        void statusBar_colorsChangeBetweenThemes() {
            Styleable status = createStyleable("Panel", null, setOf("status"));

            // Dark theme
            styleEngine.setActiveStylesheet("dark");
            CssStyleResolver darkResolved = styleEngine.resolve(status);
            Style darkStyle = darkResolved.toStyle();

            // Light theme
            styleEngine.setActiveStylesheet("light");
            CssStyleResolver lightResolved = styleEngine.resolve(status);
            Style lightStyle = lightResolved.toStyle();

            // Dark: #333333 background, #666666 border
            Color.Rgb darkBg = (Color.Rgb) darkStyle.bg().get();
            assertThat(darkBg.r()).isEqualTo(0x33);
            assertThat(darkResolved.getProperty("border-color").get()).isEqualTo("#666666");

            // Light: #cccccc background, #888888 border
            Color.Rgb lightBg = (Color.Rgb) lightStyle.bg().get();
            assertThat(lightBg.r()).isEqualTo(0xcc);
            assertThat(lightResolved.getProperty("border-color").get()).isEqualTo("#888888");
        }

        @Test
        @DisplayName("Accent colors change between themes (cyan vs blue)")
        void accentColors_changeBetweenThemes() {
            Styleable header = createStyleable("TextElement", null, setOf("header"));

            // Dark theme uses cyan accent
            styleEngine.setActiveStylesheet("dark");
            Style darkStyle = styleEngine.resolve(header).toStyle();
            assertThat(darkStyle.fg().get()).isEqualTo(Color.CYAN);

            // Light theme uses blue accent
            styleEngine.setActiveStylesheet("light");
            Style lightStyle = styleEngine.resolve(header).toStyle();
            assertThat(lightStyle.fg().get()).isEqualTo(Color.BLUE);
        }

        @Test
        @DisplayName("Panel border colors change between themes (dark-gray vs gray)")
        void panelBorderColors_changeBetweenThemes() {
            Styleable panel = createStyleable("Panel", null, Collections.emptySet());

            // Dark theme: border-color should be dark-gray
            styleEngine.setActiveStylesheet("dark");
            CssStyleResolver darkResolved = styleEngine.resolve(panel);
            assertThat(darkResolved.getProperty("border-color")).isPresent();
            assertThat(darkResolved.getProperty("border-color").get()).isEqualTo("dark-gray");

            // Light theme: border-color should be #888888
            styleEngine.setActiveStylesheet("light");
            CssStyleResolver lightResolved = styleEngine.resolve(panel);
            assertThat(lightResolved.getProperty("border-color")).isPresent();
            assertThat(lightResolved.getProperty("border-color").get()).isEqualTo("#888888");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // COLOR PARSING TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Color Parsing")
    class ColorParsingTests {

        @Test
        @DisplayName("parseColor handles hex colors")
        void parseColor_handlesHexColors() {
            styleEngine.setActiveStylesheet("dark");

            assertThat(styleEngine.parseColor("#ff0000")).isPresent();
            Color.Rgb red = (Color.Rgb) styleEngine.parseColor("#ff0000").get();
            assertThat(red.r()).isEqualTo(255);
            assertThat(red.g()).isEqualTo(0);
            assertThat(red.b()).isEqualTo(0);

            assertThat(styleEngine.parseColor("#00ff00")).isPresent();
            Color.Rgb green = (Color.Rgb) styleEngine.parseColor("#00ff00").get();
            assertThat(green.r()).isEqualTo(0);
            assertThat(green.g()).isEqualTo(255);
            assertThat(green.b()).isEqualTo(0);
        }

        @Test
        @DisplayName("parseColor handles named colors")
        void parseColor_handlesNamedColors() {
            styleEngine.setActiveStylesheet("dark");

            assertThat(styleEngine.parseColor("red")).contains(Color.RED);
            assertThat(styleEngine.parseColor("blue")).contains(Color.BLUE);
            assertThat(styleEngine.parseColor("cyan")).contains(Color.CYAN);
            assertThat(styleEngine.parseColor("white")).contains(Color.WHITE);
            assertThat(styleEngine.parseColor("black")).contains(Color.BLACK);
        }

        @Test
        @DisplayName("parseColor handles CSS variables")
        void parseColor_handlesCssVariables() {
            styleEngine.setActiveStylesheet("dark");
            // Dark theme has $accent: cyan
            assertThat(styleEngine.parseColor("$accent")).contains(Color.CYAN);

            styleEngine.setActiveStylesheet("light");
            // Light theme has $accent: blue
            assertThat(styleEngine.parseColor("$accent")).contains(Color.BLUE);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════════════════

    private Styleable createStyleable(String type, String id, Set<String> classes) {
        return new TestStyleable(type, id, classes);
    }

    private static Set<String> setOf(String... values) {
        return new HashSet<>(Arrays.asList(values));
    }

    private static class TestStyleable implements Styleable {
        private final String type;
        private final String id;
        private final Set<String> classes;

        TestStyleable(String type, String id, Set<String> classes) {
            this.type = type;
            this.id = id;
            this.classes = classes;
        }

        @Override
        public String styleType() {
            return type;
        }

        @Override
        public Optional<String> cssId() {
            return Optional.ofNullable(id);
        }

        @Override
        public Set<String> cssClasses() {
            return classes;
        }

        @Override
        public Optional<Styleable> cssParent() {
            return Optional.empty();
        }
    }
}
