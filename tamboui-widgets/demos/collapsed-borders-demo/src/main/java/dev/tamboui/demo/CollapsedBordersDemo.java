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
import dev.tamboui.layout.Spacing;
import dev.tamboui.symbols.merge.MergeStrategy;
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
import dev.tamboui.widgets.paragraph.Paragraph;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.widgets.block.Title;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Demo TUI application showcasing collapsed borders with Block widgets.
 * <p>
 * Demonstrates:
 * - Multiple blocks with overlapping borders
 * - Selected pane with thick border
 * - Border style changes on selection
 * - Interactive selection (arrow keys)
 * - Rendering order (selected last)
 * <p>
 * This demo uses Spacing.overlap() and MergeStrategy.EXACT to achieve
 * collapsed borders, where adjacent blocks share border pixels.
 */
public class CollapsedBordersDemo {

    private enum Pane {
        TOP,
        LEFT,
        RIGHT,
        BOTTOM
    }

    private boolean running = true;
    private Pane selectedPane = Pane.TOP;

    private CollapsedBordersDemo() {

    }

    /**
     * Demo entry point.
     * @param args the CLI arguments
     * @throws Exception on unexpected error
     */
    public static void main(String[] args) throws Exception {
        new CollapsedBordersDemo().run();
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
                if (c == -2 || c == -1) {
                    continue;
                }

                boolean needsRedraw = handleInput(c, backend);
                if (needsRedraw) {
                    // Redraw handled in loop
                }
            }
        }
    }

    private boolean handleInput(int c, Backend backend) throws IOException {
        // Handle escape sequences
        if (c == 27) {
            int next = backend.peek(50);
            if (next == '[') {
                backend.read(50);
                int code = backend.read(50);
                return handleEscapeSequence(code);
            }
            return false;
        }

        return switch (c) {
            case 'q', 'Q', 3 -> {
                running = false;
                yield true;
            }
            default -> false;
        };
    }

    private boolean handleEscapeSequence(int code) {
        return switch (code) {
            case 'A' -> { // Up arrow
                selectedPane = Pane.TOP;
                yield true;
            }
            case 'B' -> { // Down arrow
                selectedPane = Pane.BOTTOM;
                yield true;
            }
            case 'C' -> { // Right arrow
                selectedPane = Pane.RIGHT;
                yield true;
            }
            case 'D' -> { // Left arrow
                selectedPane = Pane.LEFT;
                yield true;
            }
            default -> false;
        };
    }

    private void ui(Frame frame) {
        Rect area = frame.area();
        if (area.isEmpty()) {
            return;
        }

        // Split into title and content
        var rows = Layout.vertical()
            .constraints(
                Constraint.length(1),
                Constraint.fill(1)
            )
            .split(area);

        renderTitle(frame, rows.get(0));
        renderBlocks(frame, rows.get(1));
    }

    private void renderTitle(Frame frame, Rect area) {
        Line title = Line.from(
            Span.raw("Block With Collapsed Borders").bold(),
            Span.raw(" (Press 'q' to quit)")
        );
        Paragraph paragraph = Paragraph.builder()
            .text(Text.from(title))
            .alignment(dev.tamboui.layout.Alignment.CENTER)
            .build();
        frame.renderWidget(paragraph, area);
    }

    private void renderBlocks(Frame frame, Rect area) {
        // Split vertically into 3 sections (top, middle, bottom)
        // Use Spacing.overlap(1) to create overlap for collapsed borders
        var verticalRows = Layout.vertical()
            .constraints(
                Constraint.fill(1),
                Constraint.fill(1),
                Constraint.fill(1)
            )
            .spacing(Spacing.overlap(1))  // Overlap by 1 cell for border merging
            .split(area);

        Rect top = verticalRows.get(0);
        Rect middle = verticalRows.get(1);
        Rect bottom = verticalRows.get(2);

        // Split middle horizontally into left and right
        var horizontalCols = Layout.horizontal()
            .constraints(
                Constraint.fill(1),
                Constraint.fill(1)
            )
            .spacing(Spacing.overlap(1))  // Overlap by 1 cell for border merging
            .split(middle);

        Rect left = horizontalCols.get(0);
        Rect right = horizontalCols.get(1);

        // Store pane areas and titles
        Map<Pane, Rect> panes = new HashMap<>();
        panes.put(Pane.TOP, top);
        panes.put(Pane.LEFT, left);
        panes.put(Pane.RIGHT, right);
        panes.put(Pane.BOTTOM, bottom);

        // Render all panes except the selected one first
        // MergeStrategy::EXACT causes the borders to collapse
        for (Map.Entry<Pane, Rect> entry : panes.entrySet()) {
            if (entry.getKey() != selectedPane) {
                Block block = Block.builder()
                    .borders(Borders.ALL)
                    .borderType(BorderType.PLAIN)
                    .mergeBorders(MergeStrategy.EXACT)  // Enable border merging
                    .title(Title.from(getPaneTitle(entry.getKey())))
                    .build();
                frame.renderWidget(block, entry.getValue());
            }
        }

        // Render the selected pane last (so it appears on top) with a thick border
        // MergeStrategy::EXACT causes the borders to collapse
        Rect selectedArea = panes.get(selectedPane);
        if (selectedArea != null) {
            Block block = Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.THICK)
                .borderStyle(Style.EMPTY.fg(Color.YELLOW))
                .mergeBorders(MergeStrategy.EXACT)  // Enable border merging
                .title(Title.from(getPaneTitle(selectedPane)))
                .build();
            frame.renderWidget(block, selectedArea);
        }
    }

    private String getPaneTitle(Pane pane) {
        return switch (pane) {
            case TOP -> "Top Block";
            case LEFT -> "Left Block";
            case RIGHT -> "Right Block";
            case BOTTOM -> "Bottom Block";
        };
    }
}

