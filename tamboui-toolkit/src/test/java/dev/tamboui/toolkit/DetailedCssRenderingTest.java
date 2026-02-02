/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;
import dev.tamboui.css.engine.StyleEngine;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.element.DefaultRenderContext;
import dev.tamboui.toolkit.elements.Column;
import dev.tamboui.toolkit.elements.Panel;
import dev.tamboui.toolkit.elements.Row;
import dev.tamboui.toolkit.elements.TextElement;

import static org.assertj.core.api.Assertions.assertThat;

class DetailedCssRenderingTest {

    // Path to the demo's theme resources (single source of truth)
    private static final Path THEMES_DIR = Paths.get("../tamboui-css/demos/css-demo/src/main/resources/themes");

    private StyleEngine styleEngine;
    private DefaultRenderContext context;
    private Buffer buffer;
    private Frame frame;
    private Rect area;

    @BeforeEach
    void setUp() throws IOException {
        styleEngine = StyleEngine.create();
        styleEngine.loadStylesheet("dark", THEMES_DIR.resolve("dark.tcss"));
        styleEngine.loadStylesheet("light", THEMES_DIR.resolve("light.tcss"));

        context = DefaultRenderContext.createEmpty();
        context.setStyleEngine(styleEngine);

        area = new Rect(0, 0, 100, 30);
        buffer = Buffer.empty(area);
        frame = Frame.forTesting(buffer);
    }

    private void resetBuffer() {
        buffer = Buffer.empty(area);
        frame = Frame.forTesting(buffer);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DARK THEME RENDERING TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Dark Theme Rendering")
    class DarkThemeRendering {

        @BeforeEach
        void activateDarkTheme() {
            styleEngine.setActiveStylesheet("dark");
        }

        @Test
        @DisplayName("TextElement renders with white foreground (no background from *)")
        void textElement_rendersWhiteForeground() {
            TextElement text = new TextElement("Hello");
            text.render(frame, new Rect(0, 0, 10, 1), context);

            Cell cell = buffer.get(0, 0);
            assertThat(cell.symbol()).isEqualTo("H");
            assertThat(cell.style().fg()).hasValue(Color.WHITE);
            // Background is NOT set on * selector - TextElement is transparent
            assertThat(cell.style().bg()).isEmpty();
        }

        @Test
        @DisplayName("TextElement.primary renders with cyan foreground")
        void textElement_primary_rendersCyan() {
            TextElement text = new TextElement("Primary");
            text.addClass("primary");
            text.render(frame, new Rect(0, 0, 10, 1), context);

            Cell cell = buffer.get(0, 0);
            assertThat(cell.style().fg()).hasValue(Color.CYAN);
        }

        @Test
        @DisplayName("TextElement.secondary renders with dark-gray foreground")
        void textElement_secondary_rendersDarkGray() {
            TextElement text = new TextElement("Secondary");
            text.addClass("secondary");
            text.render(frame, new Rect(0, 0, 10, 1), context);

            Cell cell = buffer.get(0, 0);
            assertThat(cell.style().fg()).hasValue(Color.DARK_GRAY);
        }

        @Test
        @DisplayName("TextElement.warning renders with light-yellow foreground")
        void textElement_warning_rendersLightYellow() {
            TextElement text = new TextElement("Warning");
            text.addClass("warning");
            text.render(frame, new Rect(0, 0, 10, 1), context);

            Cell cell = buffer.get(0, 0);
            assertThat(cell.style().fg()).hasValue(Color.LIGHT_YELLOW);
        }

        @Test
        @DisplayName("TextElement.error renders with light-red foreground")
        void textElement_error_rendersLightRed() {
            TextElement text = new TextElement("Error");
            text.addClass("error");
            text.render(frame, new Rect(0, 0, 10, 1), context);

            Cell cell = buffer.get(0, 0);
            assertThat(cell.style().fg()).hasValue(Color.LIGHT_RED);
        }

        @Test
        @DisplayName("TextElement.success renders with light-green foreground")
        void textElement_success_rendersLightGreen() {
            TextElement text = new TextElement("Success");
            text.addClass("success");
            text.render(frame, new Rect(0, 0, 10, 1), context);

            Cell cell = buffer.get(0, 0);
            assertThat(cell.style().fg()).hasValue(Color.LIGHT_GREEN);
        }

        @Test
        @DisplayName("TextElement.info renders with light-cyan foreground")
        void textElement_info_rendersLightCyan() {
            TextElement text = new TextElement("Info");
            text.addClass("info");
            text.render(frame, new Rect(0, 0, 10, 1), context);

            Cell cell = buffer.get(0, 0);
            assertThat(cell.style().fg()).hasValue(Color.LIGHT_CYAN);
        }

        @Test
        @DisplayName("TextElement.header renders with cyan foreground")
        void textElement_header_rendersCyan() {
            TextElement text = new TextElement("Header");
            text.addClass("header");
            text.render(frame, new Rect(0, 0, 10, 1), context);

            Cell cell = buffer.get(0, 0);
            assertThat(cell.style().fg()).hasValue(Color.CYAN);
        }

        @Test
        @DisplayName("TextElement#theme-indicator renders with cyan foreground")
        void textElement_themeIndicator_rendersCyan() {
            TextElement text = new TextElement("DARK");
            text.id("theme-indicator");
            text.render(frame, new Rect(0, 0, 10, 1), context);

            Cell cell = buffer.get(0, 0);
            assertThat(cell.style().fg()).hasValue(Color.CYAN);
        }

        @Test
        @DisplayName("Panel border renders with dark-gray foreground")
        void panel_border_rendersDarkGray() {
            Panel panel = new Panel(new TextElement("Content"));
            panel.render(frame, new Rect(0, 0, 20, 5), context);

            // Check border cell (top-left corner)
            Cell borderCell = buffer.get(0, 0);
            System.out.println("Dark Panel border: " + borderCell.style());
            assertThat(borderCell.style().fg()).hasValue(Color.DARK_GRAY);
            assertThat(borderCell.style().bg()).hasValue(Color.BLACK);
        }

        @Test
        @DisplayName("Panel with title renders title with dark-gray foreground")
        void panel_title_rendersDarkGray() {
            Panel panel = new Panel(new TextElement("Content"));
            panel.title("Style Classes");
            panel.render(frame, new Rect(0, 0, 20, 5), context);

            // Find the title "S" character
            Cell titleCell = buffer.get(1, 0);
            System.out.println("Dark Panel title cell: symbol='" + titleCell.symbol() + "' style=" + titleCell.style());
            assertThat(titleCell.symbol()).isEqualTo("S");
            assertThat(titleCell.style().fg()).hasValue(Color.DARK_GRAY);
        }

        @Test
        @DisplayName("Panel.status border renders with #666666 foreground")
        void panel_status_border_rendersGray() {
            Panel panel = new Panel(new TextElement("Status"));
            panel.addClass("status");
            panel.render(frame, new Rect(0, 0, 20, 3), context);

            Cell borderCell = buffer.get(0, 0);
            System.out.println("Dark Status border: " + borderCell.style());
            assertThat(borderCell.style().fg()).isPresent();
            // #666666 = RGB(102, 102, 102)
            Color.Rgb expectedColor = (Color.Rgb) borderCell.style().fg().get();
            assertThat(expectedColor.r()).isEqualTo(0x66);
        }

        @Test
        @DisplayName("Panel.status background renders with BLACK (from $bg-primary)")
        void panel_status_background_rendersBlack() {
            Panel panel = new Panel(new TextElement("Status"));
            panel.addClass("status");
            panel.render(frame, new Rect(0, 0, 20, 3), context);

            Cell borderCell = buffer.get(0, 0);
            assertThat(borderCell.style().bg()).isPresent();
            // Dark theme: .status { background: $bg-primary } which is BLACK
            assertThat(borderCell.style().bg().get()).isEqualTo(Color.BLACK);
        }

        @Test
        @DisplayName("Row fills area with background from Row CSS rule")
        void row_fillsBackgroundFromRowRule() {
            Row row = new Row(new TextElement("Hello"));
            row.render(frame, new Rect(0, 0, 20, 1), context);

            // Check cell beyond text content - Row fills its background from CSS rule
            Cell emptyCell = buffer.get(15, 0);
            assertThat(emptyCell.style().bg()).isPresent();
            // Dark theme: Row { background: $bg-primary } which is BLACK
            assertThat(emptyCell.style().bg().get()).isEqualTo(Color.BLACK);
        }

        @Test
        @DisplayName("Column fills area with background from Column CSS rule")
        void column_fillsBackgroundFromColumnRule() {
            Column column = new Column(new TextElement("Hello"));
            column.render(frame, new Rect(0, 0, 20, 5), context);

            // Check cell below text content - Column fills its background from CSS rule
            Cell emptyCell = buffer.get(0, 3);
            assertThat(emptyCell.style().bg()).isPresent();
            // Dark theme: Column { background: $bg-primary } which is BLACK
            assertThat(emptyCell.style().bg().get()).isEqualTo(Color.BLACK);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // LIGHT THEME RENDERING TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Light Theme Rendering")
    class LightThemeRendering {

        @BeforeEach
        void activateLightTheme() {
            styleEngine.setActiveStylesheet("light");
        }

        @Test
        @DisplayName("TextElement renders with #1a1a1a foreground (no background from *)")
        void textElement_rendersBlackForeground() {
            TextElement text = new TextElement("Hello");
            text.render(frame, new Rect(0, 0, 10, 1), context);

            Cell cell = buffer.get(0, 0);
            assertThat(cell.symbol()).isEqualTo("H");
            // Foreground from * selector: #1a1a1a
            assertThat(cell.style().fg()).isPresent();
            Color.Rgb fg = (Color.Rgb) cell.style().fg().get();
            assertThat(fg.r()).isEqualTo(0x1a);
            // Background is NOT set on * selector - TextElement is transparent
            assertThat(cell.style().bg()).isEmpty();
        }

        @Test
        @DisplayName("TextElement.primary renders with #0055aa foreground")
        void textElement_primary_rendersBlue() {
            TextElement text = new TextElement("Primary");
            text.addClass("primary");
            text.render(frame, new Rect(0, 0, 10, 1), context);

            Cell cell = buffer.get(0, 0);
            // Demo CSS: .primary { color: #0055aa }
            assertThat(cell.style().fg()).isPresent();
            Color.Rgb fg = (Color.Rgb) cell.style().fg().get();
            assertThat(fg.r()).isEqualTo(0x00);
            assertThat(fg.g()).isEqualTo(0x55);
            assertThat(fg.b()).isEqualTo(0xaa);
        }

        @Test
        @DisplayName("TextElement.secondary renders with #555555 foreground")
        void textElement_secondary_rendersGray() {
            TextElement text = new TextElement("Secondary");
            text.addClass("secondary");
            text.render(frame, new Rect(0, 0, 10, 1), context);

            Cell cell = buffer.get(0, 0);
            // Demo CSS: .secondary { color: #555555 }
            assertThat(cell.style().fg()).isPresent();
            Color.Rgb fg = (Color.Rgb) cell.style().fg().get();
            assertThat(fg.r()).isEqualTo(0x55);
        }

        @Test
        @DisplayName("TextElement.warning renders with #996600 foreground")
        void textElement_warning_rendersYellow() {
            TextElement text = new TextElement("Warning");
            text.addClass("warning");
            text.render(frame, new Rect(0, 0, 10, 1), context);

            Cell cell = buffer.get(0, 0);
            // Demo CSS: .warning { color: #996600 }
            assertThat(cell.style().fg()).isPresent();
            Color.Rgb fg = (Color.Rgb) cell.style().fg().get();
            assertThat(fg.r()).isEqualTo(0x99);
            assertThat(fg.g()).isEqualTo(0x66);
            assertThat(fg.b()).isEqualTo(0x00);
        }

        @Test
        @DisplayName("TextElement.error renders with #cc0000 foreground")
        void textElement_error_rendersRed() {
            TextElement text = new TextElement("Error");
            text.addClass("error");
            text.render(frame, new Rect(0, 0, 10, 1), context);

            Cell cell = buffer.get(0, 0);
            // Demo CSS: .error { color: #cc0000 }
            assertThat(cell.style().fg()).isPresent();
            Color.Rgb fg = (Color.Rgb) cell.style().fg().get();
            assertThat(fg.r()).isEqualTo(0xcc);
            assertThat(fg.g()).isEqualTo(0x00);
            assertThat(fg.b()).isEqualTo(0x00);
        }

        @Test
        @DisplayName("TextElement.success renders with #007700 foreground")
        void textElement_success_rendersGreen() {
            TextElement text = new TextElement("Success");
            text.addClass("success");
            text.render(frame, new Rect(0, 0, 10, 1), context);

            Cell cell = buffer.get(0, 0);
            // Demo CSS: .success { color: #007700 }
            assertThat(cell.style().fg()).isPresent();
            Color.Rgb fg = (Color.Rgb) cell.style().fg().get();
            assertThat(fg.r()).isEqualTo(0x00);
            assertThat(fg.g()).isEqualTo(0x77);
            assertThat(fg.b()).isEqualTo(0x00);
        }

        @Test
        @DisplayName("TextElement.info renders with #006688 foreground")
        void textElement_info_rendersCyan() {
            TextElement text = new TextElement("Info");
            text.addClass("info");
            text.render(frame, new Rect(0, 0, 10, 1), context);

            Cell cell = buffer.get(0, 0);
            // Demo CSS: .info { color: #006688 }
            assertThat(cell.style().fg()).isPresent();
            Color.Rgb fg = (Color.Rgb) cell.style().fg().get();
            assertThat(fg.r()).isEqualTo(0x00);
            assertThat(fg.g()).isEqualTo(0x66);
            assertThat(fg.b()).isEqualTo(0x88);
        }

        @Test
        @DisplayName("TextElement.header renders with #0066cc foreground")
        void textElement_header_rendersBlue() {
            TextElement text = new TextElement("Header");
            text.addClass("header");
            text.render(frame, new Rect(0, 0, 10, 1), context);

            Cell cell = buffer.get(0, 0);
            // Demo CSS: .header { color: $accent } where $accent: #0066cc
            assertThat(cell.style().fg()).isPresent();
            Color.Rgb fg = (Color.Rgb) cell.style().fg().get();
            assertThat(fg.r()).isEqualTo(0x00);
            assertThat(fg.g()).isEqualTo(0x66);
            assertThat(fg.b()).isEqualTo(0xcc);
        }

        @Test
        @DisplayName("TextElement#theme-indicator renders with #0066cc foreground")
        void textElement_themeIndicator_rendersBlue() {
            TextElement text = new TextElement("LIGHT");
            text.id("theme-indicator");
            text.render(frame, new Rect(0, 0, 10, 1), context);

            Cell cell = buffer.get(0, 0);
            // Demo CSS: #theme-indicator { color: #0066cc }
            assertThat(cell.style().fg()).isPresent();
            Color.Rgb fg = (Color.Rgb) cell.style().fg().get();
            assertThat(fg.r()).isEqualTo(0x00);
            assertThat(fg.g()).isEqualTo(0x66);
            assertThat(fg.b()).isEqualTo(0xcc);
        }

        @Test
        @DisplayName("Panel border renders with #888888 foreground")
        void panel_border_rendersGray() {
            Panel panel = new Panel(new TextElement("Content"));
            panel.render(frame, new Rect(0, 0, 20, 5), context);

            // Check border cell (top-left corner)
            Cell borderCell = buffer.get(0, 0);
            System.out.println("Light Panel border: " + borderCell.style());
            assertThat(borderCell.style().fg()).isPresent();
            // Demo CSS: Panel { border-color: $border-color } where $border-color: #888888
            Color.Rgb borderColor = (Color.Rgb) borderCell.style().fg().get();
            assertThat(borderColor.r()).isEqualTo(0x88);
        }

        @Test
        @DisplayName("Panel with title renders title with #888888 foreground")
        void panel_title_rendersGray() {
            Panel panel = new Panel(new TextElement("Content"));
            panel.title("Style Classes");
            panel.render(frame, new Rect(0, 0, 20, 5), context);

            // Find the title "S" character
            Cell titleCell = buffer.get(1, 0);
            System.out.println("Light Panel title cell: symbol='" + titleCell.symbol() + "' style=" + titleCell.style());
            assertThat(titleCell.symbol()).isEqualTo("S");
            assertThat(titleCell.style().fg()).isPresent();
            // Title inherits foreground from border-color: #888888
            Color.Rgb titleColor = (Color.Rgb) titleCell.style().fg().get();
            assertThat(titleColor.r()).isEqualTo(0x88);
        }

        @Test
        @DisplayName("Panel.status border renders with #888888 foreground")
        void panel_status_border_rendersGray() {
            Panel panel = new Panel(new TextElement("Status"));
            panel.addClass("status");
            panel.render(frame, new Rect(0, 0, 20, 3), context);

            Cell borderCell = buffer.get(0, 0);
            System.out.println("Light Status border: " + borderCell.style());
            assertThat(borderCell.style().fg()).isPresent();
            // Demo CSS: .status { border-color: #888888 }
            Color.Rgb expectedColor = (Color.Rgb) borderCell.style().fg().get();
            assertThat(expectedColor.r()).isEqualTo(0x88);
        }

        @Test
        @DisplayName("Panel.status background renders with #eeeeee (from $bg-primary)")
        void panel_status_background_rendersLightGray() {
            Panel panel = new Panel(new TextElement("Status"));
            panel.addClass("status");
            panel.render(frame, new Rect(0, 0, 20, 3), context);

            Cell borderCell = buffer.get(0, 0);
            assertThat(borderCell.style().bg()).isPresent();
            // Demo CSS: .status { background: $bg-primary } which is #eeeeee in light theme
            Color.Rgb bgColor = (Color.Rgb) borderCell.style().bg().get();
            assertThat(bgColor.r()).isEqualTo(0xee);
        }

        @Test
        @DisplayName("Row fills area with background from Row CSS rule")
        void row_fillsBackgroundFromRowRule() {
            Row row = new Row(new TextElement("Hello"));
            row.render(frame, new Rect(0, 0, 20, 1), context);

            // Check cell beyond text content - Row fills its background from CSS rule
            Cell emptyCell = buffer.get(15, 0);
            assertThat(emptyCell.style().bg()).isPresent();
            // Light theme: Row { background: $bg-primary } which is #eeeeee
            Color.Rgb bgColor = (Color.Rgb) emptyCell.style().bg().get();
            assertThat(bgColor.r()).isEqualTo(0xee);
            assertThat(bgColor.g()).isEqualTo(0xee);
            assertThat(bgColor.b()).isEqualTo(0xee);
        }

        @Test
        @DisplayName("Column fills area with background from Column CSS rule")
        void column_fillsBackgroundFromColumnRule() {
            Column column = new Column(new TextElement("Hello"));
            column.render(frame, new Rect(0, 0, 20, 5), context);

            // Check cell below text content - Column fills its background from CSS rule
            Cell emptyCell = buffer.get(0, 3);
            assertThat(emptyCell.style().bg()).isPresent();
            // Light theme: Column { background: $bg-primary } which is #eeeeee
            Color.Rgb bgColor = (Color.Rgb) emptyCell.style().bg().get();
            assertThat(bgColor.r()).isEqualTo(0xee);
            assertThat(bgColor.g()).isEqualTo(0xee);
            assertThat(bgColor.b()).isEqualTo(0xee);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // NESTED STRUCTURE TESTS (matching actual demo layout)
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Nested Structure Rendering (like CSS Demo)")
    class NestedStructureRendering {

        @Test
        @DisplayName("LIGHT: Panel inside Row inside Column renders border with #888888 foreground")
        void light_nestedPanel_borderRendersGray() {
            styleEngine.setActiveStylesheet("light");

            // This is the exact structure from CssDemo
            Column column = new Column(
                new Row(
                    new Panel(new TextElement("Content")).title("Style Classes")
                )
            );

            column.render(frame, new Rect(0, 0, 40, 10), context);

            // Find the panel border (top-left corner of the inner panel)
            // The Column/Row don't add borders, so the Panel border should be at (0,0)
            Cell borderCell = buffer.get(0, 0);
            System.out.println("LIGHT nested panel border: " + borderCell.style());

            // Border should be #888888 (CSS: Panel { border-color: $border-color } where $border-color: #888888)
            assertThat(borderCell.style().fg()).isPresent();
            Color.Rgb borderColor = (Color.Rgb) borderCell.style().fg().get();
            assertThat(borderColor.r())
                .describedAs("Light theme panel border should be #888888")
                .isEqualTo(0x88);
        }

        @Test
        @DisplayName("LIGHT: Panel title inside Row inside Column renders with #888888 foreground")
        void light_nestedPanel_titleRendersGray() {
            styleEngine.setActiveStylesheet("light");

            Column column = new Column(
                new Row(
                    new Panel(new TextElement("Content")).title("Style Classes")
                )
            );

            column.render(frame, new Rect(0, 0, 40, 10), context);

            // Find the title "S" character
            Cell titleCell = buffer.get(1, 0);
            System.out.println("LIGHT nested panel title: symbol='" + titleCell.symbol() + "' style=" + titleCell.style());

            assertThat(titleCell.symbol()).isEqualTo("S");
            assertThat(titleCell.style().fg()).isPresent();
            Color.Rgb titleColor = (Color.Rgb) titleCell.style().fg().get();
            assertThat(titleColor.r())
                .describedAs("Light theme panel title should be #888888")
                .isEqualTo(0x88);
        }

        @Test
        @DisplayName("DARK: Panel inside Row inside Column renders border with dark-gray foreground")
        void dark_nestedPanel_borderRendersDarkGray() {
            styleEngine.setActiveStylesheet("dark");

            Column column = new Column(
                new Row(
                    new Panel(new TextElement("Content")).title("Style Classes")
                )
            );

            column.render(frame, new Rect(0, 0, 40, 10), context);

            Cell borderCell = buffer.get(0, 0);
            System.out.println("DARK nested panel border: " + borderCell.style());

            assertThat(borderCell.style().fg())
                .describedAs("Dark theme panel border should be DARK_GRAY")
                .hasValue(Color.DARK_GRAY);
        }

        @Test
        @DisplayName("LIGHT: Exact CssDemo structure - Panel border should be #888888")
        void light_exactDemoStructure_borderRendersGray() {
            styleEngine.setActiveStylesheet("light");

            Column outerColumn = new Column(
                new Row(
                    new Panel(
                        new Column(
                            new TextElement("Primary Action").addClass("primary"),
                            new TextElement("Secondary Info").addClass("secondary")
                        )
                    ).title("Style Classes").rounded()
                )
            );

            outerColumn.render(frame, new Rect(0, 0, 40, 10), context);

            Cell borderCell = buffer.get(0, 0);
            System.out.println("LIGHT exact demo border: " + borderCell.style());

            assertThat(borderCell.style().fg()).isPresent();
            // Demo CSS: Panel { border-color: $border-color } where $border-color: #888888
            Color.Rgb borderColor = (Color.Rgb) borderCell.style().fg().get();
            assertThat(borderColor.r())
                .describedAs("Light theme border should be #888888")
                .isEqualTo(0x88);
        }

        @Test
        @DisplayName("LIGHT: Panel TITLE background should match Panel background")
        void light_panelTitle_backgroundShouldMatchPanelBackground() {
            styleEngine.setActiveStylesheet("light");

            Column outerColumn = new Column(
                new Row(
                    new Panel(
                        new Column(
                            new TextElement("Content")
                        )
                    ).title("Style Classes").rounded()
                )
            );

            outerColumn.render(frame, new Rect(0, 0, 40, 10), context);

            // Title "S" is at position (1, 0) - right after the corner
            Cell titleCell = buffer.get(1, 0);
            System.out.println("LIGHT title cell: symbol='" + titleCell.symbol() + "' style=" + titleCell.style());
            System.out.println("Title fg: " + titleCell.style().fg().orElse(null));
            System.out.println("Title bg: " + titleCell.style().bg().orElse(null));

            assertThat(titleCell.symbol()).isEqualTo("S");

            // Title foreground should be #888888 (border-color)
            assertThat(titleCell.style().fg()).isPresent();
            Color.Rgb titleColor = (Color.Rgb) titleCell.style().fg().get();
            assertThat(titleColor.r())
                .describedAs("Title fg should be #888888")
                .isEqualTo(0x88);

            // Title background should be #eeeeee (Panel background) - NOT null/absent!
            assertThat(titleCell.style().bg())
                .describedAs("Title bg should be present (Panel background #eeeeee)")
                .isPresent();

            Color.Rgb expectedBg = (Color.Rgb) titleCell.style().bg().get();
            assertThat(expectedBg.r())
                .describedAs("Title bg should be #eeeeee")
                .isEqualTo(0xee);
        }

        @Test
        @DisplayName("Nested panels: border color should differ between themes")
        void nestedPanel_borderColorDiffersBetweenThemes() {
            // Dark theme
            styleEngine.setActiveStylesheet("dark");
            Column darkColumn = new Column(
                new Row(
                    new Panel(new TextElement("Content")).title("Test")
                )
            );
            darkColumn.render(frame, new Rect(0, 0, 40, 10), context);
            Cell darkBorder = buffer.get(0, 0);
            Color darkFg = darkBorder.style().fg().orElse(null);
            System.out.println("Dark nested border fg: " + darkFg);

            // Light theme
            resetBuffer();
            styleEngine.setActiveStylesheet("light");
            Column lightColumn = new Column(
                new Row(
                    new Panel(new TextElement("Content")).title("Test")
                )
            );
            lightColumn.render(frame, new Rect(0, 0, 40, 10), context);
            Cell lightBorder = buffer.get(0, 0);
            Color lightFg = lightBorder.style().fg().orElse(null);
            System.out.println("Light nested border fg: " + lightFg);

            // They should be different colors
            assertThat(darkFg)
                .describedAs("Dark and light border colors should be different")
                .isNotEqualTo(lightFg);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // THEME SWITCHING RENDERING TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Theme Switching Rendering")
    class ThemeSwitchingRendering {

        @Test
        @DisplayName("Panel border color changes when theme switches")
        void panel_borderColorChanges() {
            Panel panel = new Panel(new TextElement("Content"));
            panel.title("Test");

            // Dark theme
            styleEngine.setActiveStylesheet("dark");
            panel.render(frame, new Rect(0, 0, 20, 5), context);
            Cell darkBorder = buffer.get(0, 0);
            System.out.println("Dark border: " + darkBorder.style());

            // Reset and switch to light
            resetBuffer();
            styleEngine.setActiveStylesheet("light");
            panel.render(frame, new Rect(0, 0, 20, 5), context);
            Cell lightBorder = buffer.get(0, 0);
            System.out.println("Light border: " + lightBorder.style());

            // Colors should be different
            assertThat(darkBorder.style().fg()).isNotEqualTo(lightBorder.style().fg());
        }

        @Test
        @DisplayName("Panel title color changes when theme switches")
        void panel_titleColorChanges() {
            Panel panel = new Panel(new TextElement("Content"));
            panel.title("Test");

            // Dark theme
            styleEngine.setActiveStylesheet("dark");
            panel.render(frame, new Rect(0, 0, 20, 5), context);
            Cell darkTitle = buffer.get(1, 0);
            System.out.println("Dark title: symbol='" + darkTitle.symbol() + "' " + darkTitle.style());

            // Reset and switch to light
            resetBuffer();
            styleEngine.setActiveStylesheet("light");
            panel.render(frame, new Rect(0, 0, 20, 5), context);
            Cell lightTitle = buffer.get(1, 0);
            System.out.println("Light title: symbol='" + lightTitle.symbol() + "' " + lightTitle.style());

            // Colors should be different
            assertThat(darkTitle.style().fg()).isNotEqualTo(lightTitle.style().fg());
        }

        @Test
        @DisplayName("TextElement foreground color changes when theme switches")
        void textElement_foregroundChanges() {
            TextElement text = new TextElement("Test");

            // Dark theme
            styleEngine.setActiveStylesheet("dark");
            text.render(frame, new Rect(0, 0, 10, 1), context);
            Cell darkCell = buffer.get(0, 0);

            // Reset and switch to light
            resetBuffer();
            styleEngine.setActiveStylesheet("light");
            text.render(frame, new Rect(0, 0, 10, 1), context);
            Cell lightCell = buffer.get(0, 0);

            // Foreground should change (dark = white, light = #1a1a1a)
            assertThat(darkCell.style().fg()).hasValue(Color.WHITE);
            assertThat(lightCell.style().fg()).isPresent();
            Color.Rgb lightFg = (Color.Rgb) lightCell.style().fg().get();
            assertThat(lightFg.r()).isEqualTo(0x1a);

            // Background should be empty in both (no background from * selector)
            assertThat(darkCell.style().bg()).isEmpty();
            assertThat(lightCell.style().bg()).isEmpty();
        }

        @Test
        @DisplayName("Styled text colors change appropriately")
        void styledText_colorsChange() {
            TextElement text = new TextElement("Primary");
            text.addClass("primary");

            // Dark theme - cyan
            styleEngine.setActiveStylesheet("dark");
            text.render(frame, new Rect(0, 0, 10, 1), context);
            Cell darkCell = buffer.get(0, 0);
            assertThat(darkCell.style().fg()).hasValue(Color.CYAN);

            // Reset and switch to light - #0055aa
            resetBuffer();
            styleEngine.setActiveStylesheet("light");
            text.render(frame, new Rect(0, 0, 10, 1), context);
            Cell lightCell = buffer.get(0, 0);
            assertThat(lightCell.style().fg()).isPresent();
            Color.Rgb lightFg = (Color.Rgb) lightCell.style().fg().get();
            assertThat(lightFg.r()).isEqualTo(0x00);
            assertThat(lightFg.g()).isEqualTo(0x55);
            assertThat(lightFg.b()).isEqualTo(0xaa);
        }
    }
}
