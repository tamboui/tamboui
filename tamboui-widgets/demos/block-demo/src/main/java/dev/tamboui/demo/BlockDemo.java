///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS dev.tamboui:tamboui-widgets:LATEST
//DEPS dev.tamboui:tamboui-jline3-backend:LATEST

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Layout;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Backend;
import dev.tamboui.terminal.BackendFactory;
import dev.tamboui.terminal.Frame;
import dev.tamboui.terminal.Terminal;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.BorderSet;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.widgets.block.Title;
import dev.tamboui.widgets.paragraph.Paragraph;

/**
 * Demo TUI application showcasing the Block widget.
 * <p>
 * Demonstrates various Block features:
 * - Different border types (plain, rounded, double, thick)
 * - Border styles (colors)
 * - Block styles (background colors, text styles)
 * - Titles (top and bottom)
 * - Padding
 * - Different border combinations
 * - Custom border character sets
 * - Hyperlinks in titles
 */
public class BlockDemo {

    private boolean running = true;

    private BlockDemo() {

    }

    /**
     * Demo entry point.
     * @param args the CLI arguments
     * @throws Exception on unexpected error
     */
    public static void main(String[] args) throws Exception {
        new BlockDemo().run();
    }

    /**
     * Runs the demo application.
     *
     * @throws Exception if an error occurs
     */
     public void run() throws Exception {
        try (Backend backend = BackendFactory.create()) {
            backend.enableRawMode();
            backend.enterAlternateScreen();
            backend.hideCursor();

            Terminal<Backend> terminal = new Terminal<>(backend);

            // Handle resize
            backend.onResize(() -> {
                terminal.draw(this::ui);
            });

            // Event loop
            while (running) {
                terminal.draw(this::ui);

                int c = backend.read(100);
                if (c == 'q' || c == 'Q' || c == 3) {
                    running = false;
                }
            }
        }
    }

    private void ui(Frame frame) {
        Rect area = frame.area();

        var layout = Layout.vertical()
            .constraints(
                Constraint.length(3),  // Header
                Constraint.fill(),     // Main content
                Constraint.length(3)   // Footer
            )
            .split(area);

        renderHeader(frame, layout.get(0));
        renderMainContent(frame, layout.get(1));
        renderFooter(frame, layout.get(2));
    }

    private void renderHeader(Frame frame, Rect area) {
        Block headerBlock = Block.builder()
            .borders(Borders.ALL)
            .borderType(BorderType.ROUNDED)
            .borderStyle(Style.EMPTY.fg(Color.CYAN))
            .title(Title.from(
                Line.from(
                    Span.raw(" TamboUI ").bold().cyan(),
                    Span.raw("Block Demo ").yellow()
                )
            ).centered())
            .build();

        frame.renderWidget(headerBlock, area);
    }

    private void renderMainContent(Frame frame, Rect area) {
        // Split into 2x2 grid
        var rows = Layout.vertical()
            .constraints(
                Constraint.percentage(50),
                Constraint.percentage(50)
            )
            .split(area);

        var topCols = Layout.horizontal()
            .constraints(
                Constraint.percentage(50),
                Constraint.percentage(50)
            )
            .split(rows.get(0));

        var bottomCols = Layout.horizontal()
            .constraints(
                Constraint.percentage(50),
                Constraint.percentage(50)
            )
            .split(rows.get(1));

        // Top left: Basic bordered block
        renderBorderedBlock(frame, topCols.get(0));

        // Top right: Styled block
        renderStyledBlock(frame, topCols.get(1));

        // Bottom left: Custom border types
        renderCustomBorders(frame, bottomCols.get(0));

        // Bottom right: Padding and titles
        renderPaddingAndTitles(frame, bottomCols.get(1));
    }

    /**
     * Render a basic block with borders.
     */
    private void renderBorderedBlock(Frame frame, Rect area) {
        Block block = Block.builder()
            .borders(Borders.ALL)
            .title("Bordered block")
            .build();

        frame.renderWidget(block, area);

        // Add some content
        Paragraph content = Paragraph.builder()
            .text(Text.from("This is a simple block with all borders."))
            .block(Block.empty())
            .build();

        frame.renderWidget(content, block.inner(area));
    }

    /**
     * Render a styled block with colors and modifiers.
     */
    private void renderStyledBlock(Frame frame, Rect area) {
        Block block = Block.builder()
            .borders(Borders.ALL)
            .style(Style.EMPTY.fg(Color.BLUE).bg(Color.BLACK).bold().italic())
            .borderStyle(Style.EMPTY.fg(Color.BLUE))
            .title("Styled block")
            .build();

        frame.renderWidget(block, area);

        // Add some content
        Paragraph content = Paragraph.builder()
            .text(Text.from(
                Line.from("This block has custom styling:"),
                Line.from("  - Blue text on black background"),
                Line.from("  - Bold and italic text"),
                Line.from("  - Blue border")
            ))
            .block(Block.empty())
            .build();

        frame.renderWidget(content, block.inner(area));
    }

    /**
     * Render blocks with different border types and custom border sets.
     */
    private void renderCustomBorders(Frame frame, Rect area) {
        // Split into 2 rows:
        // - Top: border types
        // - Bottom: truly custom BorderSet examples
        var rows = Layout.vertical()
            .constraints(
                Constraint.percentage(50),
                Constraint.percentage(50)
            )
            .split(area);

        var topCols = Layout.horizontal()
            .constraints(
                Constraint.percentage(25),
                Constraint.percentage(25),
                Constraint.percentage(25),
                Constraint.percentage(25)
            )
            .split(rows.get(0));

        var bottomCols = Layout.horizontal()
            .constraints(
                Constraint.percentage(25),
                Constraint.percentage(25),
                Constraint.percentage(25),
                Constraint.percentage(25)
            )
            .split(rows.get(1));

        // Rounded borders
        Block rounded = Block.builder()
            .borders(Borders.ALL)
            .borderType(BorderType.ROUNDED)
            .borderStyle(Style.EMPTY.fg(Color.RED))
            .title(Title.from("Rounded").centered())
            .build();
        frame.renderWidget(rounded, topCols.get(0));

        // Double borders
        Block doubled = Block.builder()
            .borders(Borders.ALL)
            .borderType(BorderType.DOUBLE)
            .borderStyle(Style.EMPTY.fg(Color.GREEN))
            .title(Title.from("Double").centered())
            .build();
        frame.renderWidget(doubled, topCols.get(1));

        // Thick borders
        Block thick = Block.builder()
            .borders(Borders.ALL)
            .borderType(BorderType.THICK)
            .borderStyle(Style.EMPTY.fg(Color.YELLOW))
            .title(Title.from("Thick").centered())
            .build();
        frame.renderWidget(thick, topCols.get(2));

        // Quadrant borders
        Block quadrant = Block.builder()
            .borders(Borders.ALL)
            .borderType(BorderType.QUADRANT_INSIDE)
            .borderStyle(Style.EMPTY.fg(Color.MAGENTA))
            .title(Title.from("Quadrant").centered())
            .build();
        frame.renderWidget(quadrant, topCols.get(3));

        // Custom: corners-only (no sides) using BorderSet (demonstrates partial borders)
        Block cornersOnly = Block.builder()
            .borders(Borders.NONE)
            .customBorderSet(new BorderSet("", "", "", "", "◜", "◝", "◟", "◞"))
            .borderStyle(Style.EMPTY.fg(Color.CYAN))
            .title(Title.from("Corners").centered())
            .build();
        frame.renderWidget(cornersOnly, bottomCols.get(0));

        // Custom: classic ASCII borders
        Block ascii = Block.builder()
            .borders(Borders.ALL)
            .customBorderSet(new BorderSet("-", "-", "|", "|", "+", "+", "+", "+"))
            .borderStyle(Style.EMPTY.fg(Color.WHITE))
            .title(Title.from("ASCII").centered())
            .build();
        frame.renderWidget(ascii, bottomCols.get(1));

        // Custom: asymmetric sides (different characters per edge)
        Block asymmetric = Block.builder()
            .borders(Borders.ALL)
            .customBorderSet(BorderSet.builder()
                .topHorizontal("═")
                .bottomHorizontal("─")
                .leftVertical("║")
                .rightVertical("│")
                .topLeft("╔")
                .topRight("╗")
                .bottomLeft("└")
                .bottomRight("┘")
                .build())
            .borderStyle(Style.EMPTY.fg(Color.BLUE))
            .title(Title.from("Mixed").centered())
            .build();
        frame.renderWidget(asymmetric, bottomCols.get(2));

        // Custom: only a left marker bar (no other borders)
        Block leftMarker = Block.builder()
            .borders(Borders.LEFT_ONLY)
            .customBorderSet(new BorderSet("", "", "▌", "", "", "", "", ""))
            .borderStyle(Style.EMPTY.fg(Color.YELLOW))
            .title(Title.from("Left").centered())
            .build();
        frame.renderWidget(leftMarker, bottomCols.get(3));
    }

    /**
     * Render blocks with padding and multiple titles.
     */
    private void renderPaddingAndTitles(Frame frame, Rect area) {
        // Split vertically
        var rows = Layout.vertical()
            .constraints(
                Constraint.percentage(50),
                Constraint.percentage(50)
            )
            .split(area);

        // Block with padding
        Block padded = Block.builder()
            .borders(Borders.ALL)
            .borderType(BorderType.ROUNDED)
            .borderStyle(Style.EMPTY.fg(Color.CYAN))
            .padding(2)
            .title(Title.from("With Padding").centered())
            .build();
        frame.renderWidget(padded, rows.get(0));

        Paragraph paddedContent = Paragraph.builder()
            .text(Text.from("This block has 2 units of padding on all sides."))
            .block(Block.empty())
            .build();
        frame.renderWidget(paddedContent, padded.inner(rows.get(0)));

        // Block with top and bottom titles (with hyperlinks)
        Block titled = Block.builder()
            .borders(Borders.ALL)
            .borderType(BorderType.ROUNDED)
            .borderStyle(Style.EMPTY.fg(Color.YELLOW))
            .title(Title.from(
                Line.from(
                    Span.raw("TamboUI ").bold().yellow(),
                    Span.raw("Docs").hyperlink("https://tamboui.dev").underlined().cyan()
                )
            ).centered())
            .titleBottom(Title.from(
                Line.from(
                    Span.raw("GitHub: ").dim(),
                    Span.raw("tamboui/tamboui")
                        .hyperlink("https://github.com/tamboui/tamboui")
                        .underlined()
                        .blue()
                )
            ).right())
            .build();
        frame.renderWidget(titled, rows.get(1));

        Paragraph titledContent = Paragraph.builder()
            .text(Text.from(
                Line.from("This block has both"),
                Line.from("a top title with hyperlink"),
                Line.from("and a bottom title with hyperlink.")
            ))
            .block(Block.empty())
            .build();
        frame.renderWidget(titledContent, titled.inner(rows.get(1)));
    }

    private void renderFooter(Frame frame, Rect area) {
        Line helpLine = Line.from(
            Span.raw("q").bold().yellow(),
            Span.raw(" Quit").dim()
        );

        Paragraph footer = Paragraph.builder()
            .text(Text.from(helpLine))
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.DARK_GRAY))
                .build())
            .build();

        frame.renderWidget(footer, area);
    }
}

