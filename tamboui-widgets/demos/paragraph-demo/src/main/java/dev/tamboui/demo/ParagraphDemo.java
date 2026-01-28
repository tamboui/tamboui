///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS dev.tamboui:tamboui-widgets:LATEST
//DEPS dev.tamboui:tamboui-jline3-backend:LATEST

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import dev.tamboui.layout.Alignment;
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
import dev.tamboui.text.Masked;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.widgets.block.Title;
import dev.tamboui.widgets.paragraph.Paragraph;
import dev.tamboui.style.Overflow;

import java.io.IOException;

/**
 * Demo TUI application showcasing the Paragraph widget.
 * <p>
 * Demonstrates various Paragraph features:
 * - Text alignment (left, center, right)
 * - Text wrapping (word, character)
 * - Multiple lines with different styles
 * - Bold, italic, underlined text
 * - Colored text
 * - Scroll support
 * - Block integration
 * - Hyperlinks (OSC8)
 */
public class ParagraphDemo {

    private boolean running = true;
    private int scrollOffset = 0;

    private ParagraphDemo() {

    }

    /**
     * Demo entry point.
     * @param args the CLI arguments
     * @throws Exception on unexpected error
     */
    public static void main(String[] args) throws Exception {
        new ParagraphDemo().run();
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
                try {
                    terminal.draw(this::ui);
                } catch (IOException e) {
                    // Ignore
                }
            });

            // Event loop
            while (running) {
                terminal.draw(this::ui);

                int c = backend.read(100);
                if (c == 'q' || c == 'Q' || c == 3) {
                    running = false;
                } else if (c == 'j' || c == 'J') {
                    scrollOffset = Math.min(scrollOffset + 1, 10);
                } else if (c == 'k' || c == 'K') {
                    scrollOffset = Math.max(scrollOffset - 1, 0);
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
                    Span.raw("Paragraph Demo ").yellow()
                )
            ).centered())
            .build();

        frame.renderWidget(headerBlock, area);
    }

    private void renderMainContent(Frame frame, Rect area) {
        // Split into 2 columns
        var cols = Layout.horizontal()
            .constraints(
                Constraint.percentage(50),
                Constraint.percentage(50)
            )
            .spacing(1)
            .split(area);

        // Left: Centered paragraph
        renderCenteredParagraph(frame, cols.get(0));

        // Right: Wrapped paragraph with styles
        renderWrappedParagraph(frame, cols.get(1));
    }

    /**
     * Render a paragraph with centered text alignment.
     */
    private void renderCenteredParagraph(Frame frame, Rect area) {
        Text text = Text.from(
            Line.from("Centered text"),
            Line.from("with multiple lines."),
            Line.from(
                Span.raw("Visit "),
                Span.raw("https://tamboui.dev").hyperlink("https://tamboui.dev").underlined().cyan(),
                Span.raw(" for docs!")
            )
        );
        
        Paragraph paragraph = Paragraph.builder()
            .text(text)
            .style(Style.EMPTY.fg(Color.WHITE))
            .alignment(Alignment.CENTER)
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.BLUE))
                .title(Title.from("Centered Alignment").centered())
                .build())
            .build();

        frame.renderWidget(paragraph, area);
    }

    /**
     * Render a long paragraph that wraps text with various styles.
     */
    private void renderWrappedParagraph(Frame frame, Rect area) {
        // Create a long line that will wrap
        String shortLine = "Slice, layer, and bake the vegetables. ";
        int repeatCount = (area.width() / shortLine.length()) + 2;
        String longLine = shortLine.repeat(repeatCount);

        Masked secretIngredient = new Masked("herbs de Provence", '*');

        Text text = Text.from(
            Line.from("Recipe: Ratatouille").bold(),
            Line.from("Ingredients:").bold(),
            Line.from(
                Span.raw("Bell Peppers"),
                Span.raw(", Eggplant").italic(),
                Span.raw(", Tomatoes").bold(),
                Span.raw(", Onion")
            ),
            Line.from(
                Span.raw("Secret Ingredient: ").underlined(),
                Span.styled(secretIngredient, Style.EMPTY.fg(Color.RED).bold())
            ),
            Line.from("Instructions:").bold().fg(Color.YELLOW),
            Line.from(longLine).fg(Color.GREEN).italic(),
            Line.from(""),
            Line.from(
                Span.raw("Full recipe at: ").dim(),
                Span.raw("https://example.com/ratatouille")
                    .hyperlink("https://example.com/ratatouille", "recipe-link")
                    .underlined()
                    .cyan()
            )
        );

        Paragraph paragraph = Paragraph.builder()
            .text(text)
            .style(Style.EMPTY.fg(Color.WHITE))
            .overflow(Overflow.WRAP_CHARACTER)
            .scroll(scrollOffset)
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.GREEN))
                .title(Title.from("Wrapped Text with Styles").centered())
                .build())
            .build();

        frame.renderWidget(paragraph, area);
    }

    private void renderFooter(Frame frame, Rect area) {
        Line helpLine = Line.from(
            Span.raw(" j/k").bold().yellow(),
            Span.raw(" Scroll  ").dim(),
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

