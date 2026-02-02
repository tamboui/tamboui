/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.integration;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import dev.tamboui.css.Styleable;
import dev.tamboui.css.cascade.CssStyleResolver;
import dev.tamboui.css.cascade.PseudoClassState;
import dev.tamboui.css.engine.StyleEngine;
import dev.tamboui.layout.Alignment;
import dev.tamboui.style.Color;
import dev.tamboui.style.Modifier;
import dev.tamboui.style.Style;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive integration tests for CSS styling.
 * Tests EVERY element type with expected colors, backgrounds, borders, and modifiers.
 */
class CssDemoIntegrationTest {

    private StyleEngine styleEngine;

    // Path to the demo's theme resources (single source of truth)
    private static final Path THEMES_DIR = Paths.get("demos/css-demo/src/main/resources/themes");

    @BeforeEach
    void setUp() throws IOException {
        styleEngine = StyleEngine.create();
        // Load from the demo's production CSS files
        styleEngine.loadStylesheet("dark", THEMES_DIR.resolve("dark.tcss"));
        styleEngine.loadStylesheet("light", THEMES_DIR.resolve("light.tcss"));
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
        @DisplayName("Universal selector (*) - provides white foreground (background NOT set on *)")
        void universalSelector_providesWhiteForeground() {
            Styleable element = createStyleable("TextElement", null, Collections.emptySet());
            CssStyleResolver resolved = styleEngine.resolve(element);
            Style style = resolved.toStyle();

            // Expected: color: white (background is NOT set on * to allow children to be transparent)
            assertThat(style.fg()).isPresent();
            assertThat(style.fg().get()).isEqualTo(Color.WHITE);

            // Background should NOT be set - children should be transparent over parent backgrounds
            assertThat(style.bg()).isEmpty();
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
            assertThat(resolved.borderColor()).isPresent();
            assertThat(resolved.borderColor().get()).isEqualTo(Color.DARK_GRAY);
        }

        @Test
        @DisplayName("Panel - no padding in demo CSS")
        void panel_hasNoPadding() {
            Styleable panel = createStyleable("Panel", null, Collections.emptySet());
            CssStyleResolver resolved = styleEngine.resolve(panel);

            // Demo CSS doesn't set padding for Panel
            assertThat(resolved.padding()).isEmpty();
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
        @DisplayName(".status - black background (from $bg-primary) with gray border (#666666)")
        void statusClass_hasBlackBackgroundAndBorder() {
            Styleable element = createStyleable("Panel", null, setOf("status"));
            CssStyleResolver resolved = styleEngine.resolve(element);
            Style style = resolved.toStyle();

            // Expected: background: black (from $bg-primary)
            assertThat(style.bg()).isPresent();
            assertThat(style.bg().get()).isEqualTo(Color.BLACK);

            // Expected: color: white
            assertThat(style.fg()).isPresent();
            assertThat(style.fg().get()).isEqualTo(Color.WHITE);

            // Expected: border-color: #666666
            assertThat(resolved.borderColor()).isPresent();
            assertThat(resolved.borderColor().get()).isInstanceOf(Color.Rgb.class);
            Color.Rgb borderColor = (Color.Rgb) resolved.borderColor().get();
            assertThat(borderColor.r()).isEqualTo(0x66);
            assertThat(borderColor.g()).isEqualTo(0x66);
            assertThat(borderColor.b()).isEqualTo(0x66);
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

            // No background - .header class doesn't set background
            assertThat(style.bg()).isEmpty();
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
        @DisplayName("Row - gets white foreground from *, black background from Row rule")
        void row_hasBackgroundFromRowRule() {
            Styleable row = createStyleable("Row", null, Collections.emptySet());
            CssStyleResolver resolved = styleEngine.resolve(row);
            Style style = resolved.toStyle();

            // Gets foreground from * selector
            assertThat(style.fg()).isPresent();
            assertThat(style.fg().get()).isEqualTo(Color.WHITE);
            // Background from Row rule: background: $bg-primary (black)
            assertThat(style.bg()).isPresent();
            assertThat(style.bg().get()).isEqualTo(Color.BLACK);
        }

        @Test
        @DisplayName("Column - gets white foreground from *, black background from Column rule")
        void column_hasBackgroundFromColumnRule() {
            Styleable column = createStyleable("Column", null, Collections.emptySet());
            CssStyleResolver resolved = styleEngine.resolve(column);
            Style style = resolved.toStyle();

            // Gets foreground from * selector
            assertThat(style.fg()).isPresent();
            assertThat(style.fg().get()).isEqualTo(Color.WHITE);
            // Background from Column rule: background: $bg-primary (black)
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
        @DisplayName("Universal selector (*) - provides #1a1a1a foreground (background NOT set on *)")
        void universalSelector_providesBlackForeground() {
            Styleable element = createStyleable("TextElement", null, Collections.emptySet());
            CssStyleResolver resolved = styleEngine.resolve(element);
            Style style = resolved.toStyle();

            // Expected: color: #1a1a1a (from $fg-primary)
            assertThat(style.fg()).isPresent();
            assertThat(style.fg().get()).isInstanceOf(Color.Rgb.class);
            Color.Rgb fg = (Color.Rgb) style.fg().get();
            assertThat(fg.r()).isEqualTo(0x1a);

            // Background should NOT be set - children should be transparent over parent backgrounds
            assertThat(style.bg()).isEmpty();
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
            assertThat(resolved.borderColor()).isPresent();
            assertThat(resolved.borderColor().get()).isInstanceOf(Color.Rgb.class);
            Color.Rgb borderColor = (Color.Rgb) resolved.borderColor().get();
            assertThat(borderColor.r()).isEqualTo(0x88);
            assertThat(borderColor.g()).isEqualTo(0x88);
            assertThat(borderColor.b()).isEqualTo(0x88);
        }

        @Test
        @DisplayName("Panel - no padding in demo CSS")
        void panel_hasNoPadding() {
            Styleable panel = createStyleable("Panel", null, Collections.emptySet());
            CssStyleResolver resolved = styleEngine.resolve(panel);

            // Demo CSS doesn't set padding for Panel
            assertThat(resolved.padding()).isEmpty();
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
        @DisplayName(".status - #eeeeee background (from $bg-primary) with #888888 border")
        void statusClass_hasGrayBackgroundAndBorder() {
            Styleable element = createStyleable("Panel", null, setOf("status"));
            CssStyleResolver resolved = styleEngine.resolve(element);
            Style style = resolved.toStyle();

            // Expected: background: #eeeeee (from $bg-primary)
            assertThat(style.bg()).isPresent();
            assertThat(style.bg().get()).isInstanceOf(Color.Rgb.class);
            Color.Rgb bg = (Color.Rgb) style.bg().get();
            assertThat(bg.r()).isEqualTo(0xee);
            assertThat(bg.g()).isEqualTo(0xee);
            assertThat(bg.b()).isEqualTo(0xee);

            // Expected: color: #1a1a1a
            assertThat(style.fg()).isPresent();
            assertThat(style.fg().get()).isInstanceOf(Color.Rgb.class);
            Color.Rgb fg = (Color.Rgb) style.fg().get();
            assertThat(fg.r()).isEqualTo(0x1a);

            // Expected: border-color: #888888
            assertThat(resolved.borderColor()).isPresent();
            assertThat(resolved.borderColor().get()).isInstanceOf(Color.Rgb.class);
            Color.Rgb borderColor = (Color.Rgb) resolved.borderColor().get();
            assertThat(borderColor.r()).isEqualTo(0x88);
            assertThat(borderColor.g()).isEqualTo(0x88);
            assertThat(borderColor.b()).isEqualTo(0x88);
        }

        @Test
        @DisplayName(".header - #0066cc foreground with bold text-style")
        void headerClass_hasBlueBold() {
            Styleable element = createStyleable("TextElement", null, setOf("header"));
            CssStyleResolver resolved = styleEngine.resolve(element);
            Style style = resolved.toStyle();

            // Expected: color: #0066cc (from $accent variable)
            assertThat(style.fg()).isPresent();
            assertThat(style.fg().get()).isInstanceOf(Color.Rgb.class);
            Color.Rgb fg = (Color.Rgb) style.fg().get();
            assertThat(fg.r()).isEqualTo(0x00);
            assertThat(fg.g()).isEqualTo(0x66);
            assertThat(fg.b()).isEqualTo(0xcc);

            // Expected: text-style: bold
            assertThat(resolved.modifiers()).contains(Modifier.BOLD);
        }

        @Test
        @DisplayName(".primary - #0055aa foreground with bold")
        void primaryClass_hasBlueBold() {
            Styleable element = createStyleable("TextElement", null, setOf("primary"));
            CssStyleResolver resolved = styleEngine.resolve(element);
            Style style = resolved.toStyle();

            // Expected: color: #0055aa
            assertThat(style.fg()).isPresent();
            assertThat(style.fg().get()).isInstanceOf(Color.Rgb.class);
            Color.Rgb fg = (Color.Rgb) style.fg().get();
            assertThat(fg.r()).isEqualTo(0x00);
            assertThat(fg.g()).isEqualTo(0x55);
            assertThat(fg.b()).isEqualTo(0xaa);

            // Expected: text-style: bold
            assertThat(resolved.modifiers()).contains(Modifier.BOLD);
        }

        @Test
        @DisplayName(".secondary - #555555 foreground")
        void secondaryClass_hasGray() {
            Styleable element = createStyleable("TextElement", null, setOf("secondary"));
            CssStyleResolver resolved = styleEngine.resolve(element);
            Style style = resolved.toStyle();

            // Expected: color: #555555
            assertThat(style.fg()).isPresent();
            assertThat(style.fg().get()).isInstanceOf(Color.Rgb.class);
            Color.Rgb fg = (Color.Rgb) style.fg().get();
            assertThat(fg.r()).isEqualTo(0x55);
        }

        @Test
        @DisplayName(".warning - #996600 foreground with bold")
        void warningClass_hasYellow() {
            Styleable element = createStyleable("TextElement", null, setOf("warning"));
            CssStyleResolver resolved = styleEngine.resolve(element);
            Style style = resolved.toStyle();

            // Expected: color: #996600
            assertThat(style.fg()).isPresent();
            assertThat(style.fg().get()).isInstanceOf(Color.Rgb.class);
            Color.Rgb fg = (Color.Rgb) style.fg().get();
            assertThat(fg.r()).isEqualTo(0x99);
            assertThat(fg.g()).isEqualTo(0x66);
            assertThat(fg.b()).isEqualTo(0x00);

            // Expected: text-style: bold (demo CSS has this)
            assertThat(resolved.modifiers()).contains(Modifier.BOLD);
        }

        @Test
        @DisplayName(".error - #cc0000 bold foreground")
        void errorClass_hasRedBold() {
            Styleable element = createStyleable("TextElement", null, setOf("error"));
            CssStyleResolver resolved = styleEngine.resolve(element);
            Style style = resolved.toStyle();

            // Expected: color: #cc0000, text-style: bold
            assertThat(style.fg()).isPresent();
            assertThat(style.fg().get()).isInstanceOf(Color.Rgb.class);
            Color.Rgb fg = (Color.Rgb) style.fg().get();
            assertThat(fg.r()).isEqualTo(0xcc);
            assertThat(fg.g()).isEqualTo(0x00);
            assertThat(fg.b()).isEqualTo(0x00);
            assertThat(resolved.modifiers()).contains(Modifier.BOLD);
        }

        @Test
        @DisplayName(".success - #007700 foreground")
        void successClass_hasGreen() {
            Styleable element = createStyleable("TextElement", null, setOf("success"));
            CssStyleResolver resolved = styleEngine.resolve(element);
            Style style = resolved.toStyle();

            // Expected: color: #007700
            assertThat(style.fg()).isPresent();
            assertThat(style.fg().get()).isInstanceOf(Color.Rgb.class);
            Color.Rgb fg = (Color.Rgb) style.fg().get();
            assertThat(fg.r()).isEqualTo(0x00);
            assertThat(fg.g()).isEqualTo(0x77);
            assertThat(fg.b()).isEqualTo(0x00);
        }

        @Test
        @DisplayName(".info - #006688 foreground")
        void infoClass_hasCyan() {
            Styleable element = createStyleable("TextElement", null, setOf("info"));
            CssStyleResolver resolved = styleEngine.resolve(element);
            Style style = resolved.toStyle();

            // Expected: color: #006688
            assertThat(style.fg()).isPresent();
            assertThat(style.fg().get()).isInstanceOf(Color.Rgb.class);
            Color.Rgb fg = (Color.Rgb) style.fg().get();
            assertThat(fg.r()).isEqualTo(0x00);
            assertThat(fg.g()).isEqualTo(0x66);
            assertThat(fg.b()).isEqualTo(0x88);
        }

        @Test
        @DisplayName(".dim - has #666666 color")
        void dimClass_hasDimColor() {
            Styleable element = createStyleable("TextElement", null, setOf("dim"));
            CssStyleResolver resolved = styleEngine.resolve(element);
            Style style = resolved.toStyle();

            // Expected: color: #666666 (demo CSS uses color, not text-style: dim)
            assertThat(style.fg()).isPresent();
            assertThat(style.fg().get()).isInstanceOf(Color.Rgb.class);
            Color.Rgb fg = (Color.Rgb) style.fg().get();
            assertThat(fg.r()).isEqualTo(0x66);
        }

        @Test
        @DisplayName("#theme-indicator - #0066cc bold")
        void themeIndicatorId_hasBlueBold() {
            Styleable element = createStyleable("TextElement", "theme-indicator", Collections.emptySet());
            CssStyleResolver resolved = styleEngine.resolve(element);
            Style style = resolved.toStyle();

            // Expected: color: #0066cc, text-style: bold
            assertThat(style.fg()).isPresent();
            assertThat(style.fg().get()).isInstanceOf(Color.Rgb.class);
            Color.Rgb fg = (Color.Rgb) style.fg().get();
            assertThat(fg.r()).isEqualTo(0x00);
            assertThat(fg.g()).isEqualTo(0x66);
            assertThat(fg.b()).isEqualTo(0xcc);
            assertThat(resolved.modifiers()).contains(Modifier.BOLD);
        }

        @Test
        @DisplayName("Row - gets #1a1a1a foreground from *, #eeeeee background from Row rule")
        void row_hasBackgroundFromRowRule() {
            Styleable row = createStyleable("Row", null, Collections.emptySet());
            CssStyleResolver resolved = styleEngine.resolve(row);
            Style style = resolved.toStyle();

            // Gets foreground from * selector
            assertThat(style.fg()).isPresent();
            assertThat(style.fg().get()).isInstanceOf(Color.Rgb.class);
            Color.Rgb fg = (Color.Rgb) style.fg().get();
            assertThat(fg.r()).isEqualTo(0x1a);
            // Background from Row rule: background: $bg-primary (#eeeeee)
            assertThat(style.bg()).isPresent();
            assertThat(style.bg().get()).isInstanceOf(Color.Rgb.class);
            Color.Rgb bg = (Color.Rgb) style.bg().get();
            assertThat(bg.r()).isEqualTo(0xee);
            assertThat(bg.g()).isEqualTo(0xee);
            assertThat(bg.b()).isEqualTo(0xee);
        }

        @Test
        @DisplayName("Column - gets #1a1a1a foreground from *, #eeeeee background from Column rule")
        void column_hasBackgroundFromColumnRule() {
            Styleable column = createStyleable("Column", null, Collections.emptySet());
            CssStyleResolver resolved = styleEngine.resolve(column);
            Style style = resolved.toStyle();

            // Gets foreground from * selector
            assertThat(style.fg()).isPresent();
            assertThat(style.fg().get()).isInstanceOf(Color.Rgb.class);
            Color.Rgb fg = (Color.Rgb) style.fg().get();
            assertThat(fg.r()).isEqualTo(0x1a);
            // Background from Column rule: background: $bg-primary (#eeeeee)
            assertThat(style.bg()).isPresent();
            assertThat(style.bg().get()).isInstanceOf(Color.Rgb.class);
            Color.Rgb bg = (Color.Rgb) style.bg().get();
            assertThat(bg.r()).isEqualTo(0xee);
            assertThat(bg.g()).isEqualTo(0xee);
            assertThat(bg.b()).isEqualTo(0xee);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // THEME SWITCHING TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Theme Switching")
    class ThemeSwitchingTests {

        @Test
        @DisplayName("Switching themes changes foreground colors correctly")
        void switchingThemes_changesForegroundColors() {
            Styleable textElement = createStyleable("TextElement", null, Collections.emptySet());

            // Dark theme
            styleEngine.setActiveStylesheet("dark");
            Style darkStyle = styleEngine.resolve(textElement).toStyle();

            // Light theme
            styleEngine.setActiveStylesheet("light");
            Style lightStyle = styleEngine.resolve(textElement).toStyle();

            // Dark: white foreground (no background on * selector)
            assertThat(darkStyle.fg().get()).isEqualTo(Color.WHITE);
            assertThat(darkStyle.bg()).isEmpty();

            // Light: #1a1a1a foreground (no background on * selector)
            assertThat(lightStyle.fg().get()).isInstanceOf(Color.Rgb.class);
            Color.Rgb lightFg = (Color.Rgb) lightStyle.fg().get();
            assertThat(lightFg.r()).isEqualTo(0x1a);
            assertThat(lightStyle.bg()).isEmpty();
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

            // Dark: BLACK background (from $bg-primary), #666666 border
            assertThat(darkStyle.bg().get()).isEqualTo(Color.BLACK);
            Color.Rgb darkBorder = (Color.Rgb) darkResolved.borderColor().get();
            assertThat(darkBorder.r()).isEqualTo(0x66);

            // Light: #eeeeee background (from $bg-primary), #888888 border
            Color.Rgb lightBg = (Color.Rgb) lightStyle.bg().get();
            assertThat(lightBg.r()).isEqualTo(0xee);
            Color.Rgb lightBorder = (Color.Rgb) lightResolved.borderColor().get();
            assertThat(lightBorder.r()).isEqualTo(0x88);
        }

        @Test
        @DisplayName("Accent colors change between themes (cyan vs #0066cc)")
        void accentColors_changeBetweenThemes() {
            Styleable header = createStyleable("TextElement", null, setOf("header"));

            // Dark theme uses cyan accent
            styleEngine.setActiveStylesheet("dark");
            Style darkStyle = styleEngine.resolve(header).toStyle();
            assertThat(darkStyle.fg().get()).isEqualTo(Color.CYAN);

            // Light theme uses #0066cc accent
            styleEngine.setActiveStylesheet("light");
            Style lightStyle = styleEngine.resolve(header).toStyle();
            assertThat(lightStyle.fg().get()).isInstanceOf(Color.Rgb.class);
            Color.Rgb lightFg = (Color.Rgb) lightStyle.fg().get();
            assertThat(lightFg.r()).isEqualTo(0x00);
            assertThat(lightFg.g()).isEqualTo(0x66);
            assertThat(lightFg.b()).isEqualTo(0xcc);
        }

        @Test
        @DisplayName("Panel border colors change between themes (dark-gray vs gray)")
        void panelBorderColors_changeBetweenThemes() {
            Styleable panel = createStyleable("Panel", null, Collections.emptySet());

            // Dark theme: border-color should be dark-gray
            styleEngine.setActiveStylesheet("dark");
            CssStyleResolver darkResolved = styleEngine.resolve(panel);
            assertThat(darkResolved.borderColor()).isPresent();
            assertThat(darkResolved.borderColor().get()).isEqualTo(Color.DARK_GRAY);

            // Light theme: border-color should be #888888
            styleEngine.setActiveStylesheet("light");
            CssStyleResolver lightResolved = styleEngine.resolve(panel);
            assertThat(lightResolved.borderColor()).isPresent();
            Color.Rgb lightBorder = (Color.Rgb) lightResolved.borderColor().get();
            assertThat(lightBorder.r()).isEqualTo(0x88);
            assertThat(lightBorder.g()).isEqualTo(0x88);
            assertThat(lightBorder.b()).isEqualTo(0x88);
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
            // Light theme has $accent: #0066cc
            assertThat(styleEngine.parseColor("$accent")).isPresent();
            assertThat(styleEngine.parseColor("$accent").get()).isInstanceOf(Color.Rgb.class);
            Color.Rgb lightAccent = (Color.Rgb) styleEngine.parseColor("$accent").get();
            assertThat(lightAccent.r()).isEqualTo(0x00);
            assertThat(lightAccent.g()).isEqualTo(0x66);
            assertThat(lightAccent.b()).isEqualTo(0xcc);
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
