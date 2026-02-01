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
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.widgets.block.Title;
import dev.tamboui.widgets.paragraph.Paragraph;
import dev.tamboui.widgets.wavetext.WaveText;
import dev.tamboui.widgets.wavetext.WaveTextState;

import java.io.IOException;

/**
 * Demo TUI application showcasing the WaveText widget.
 * <p>
 * Demonstrates various configuration options:
 * <ul>
 *   <li>Default mode (dark shadow on bright text)</li>
 *   <li>Inverted mode (bright peak on dim text)</li>
 *   <li>Different speeds</li>
 *   <li>Different peak widths</li>
 *   <li>Multiple peaks</li>
 *   <li>Loop vs oscillate modes</li>
 * </ul>
 */
public class WaveTextDemo {

    private static final Color CYAN = Color.rgb(0, 180, 216);
    private static final Color GREEN = Color.rgb(46, 204, 113);
    private static final Color YELLOW = Color.rgb(241, 196, 15);
    private static final Color MAGENTA = Color.rgb(155, 89, 182);
    private static final Color ORANGE = Color.rgb(230, 126, 34);
    private static final Color PINK = Color.rgb(232, 67, 147);

    private boolean running = true;
    private long frameCount = 0;

    // Each wave text needs its own state to animate independently
    private final WaveTextState defaultState = new WaveTextState();
    private final WaveTextState invertedState = new WaveTextState();
    private final WaveTextState slowState = new WaveTextState();
    private final WaveTextState fastState = new WaveTextState();
    private final WaveTextState narrowState = new WaveTextState();
    private final WaveTextState wideState = new WaveTextState();
    private final WaveTextState multiPeakState = new WaveTextState();
    private final WaveTextState oscillateState = new WaveTextState();

    private WaveTextDemo() {

    }

    /**
     * Demo entry point.
     * @param args the CLI arguments
     * @throws Exception on unexpected error
     */
    public static void main(String[] args) throws Exception {
        new WaveTextDemo().run();
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

            backend.onResize(() -> {
                terminal.draw(this::ui);
            });

            while (running) {
                terminal.draw(this::ui);

                int c = backend.read(50);
                if (c == 'q' || c == 'Q' || c == 3) {
                    running = false;
                }

                // Advance all states
                advanceStates();
                frameCount++;
            }
        }
    }

    private void advanceStates() {
        defaultState.advance();
        invertedState.advance();
        slowState.advance();
        fastState.advance();
        narrowState.advance();
        wideState.advance();
        multiPeakState.advance();
        oscillateState.advance();
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
                    Span.raw("WaveText Demo ").yellow()
                )
            ).centered())
            .build();

        frame.renderWidget(headerBlock, area);
    }

    private void renderMainContent(Frame frame, Rect area) {
        // Split into rows for each example
        var rows = Layout.vertical()
            .constraints(
                Constraint.length(3),  // Default
                Constraint.length(3),  // Inverted
                Constraint.length(3),  // Slow speed
                Constraint.length(3),  // Fast speed
                Constraint.length(3),  // Narrow peak
                Constraint.length(3),  // Wide peak
                Constraint.length(3),  // Multiple peaks
                Constraint.length(3)   // Oscillate
            )
            .split(area);

        renderExample(frame, rows.get(0), "Default (dark shadow on bright text)",
            WaveText.builder()
                .text("Loading resources...")
                .color(CYAN)
                .build(),
            defaultState);

        renderExample(frame, rows.get(1), "Inverted (bright peak on dim text)",
            WaveText.builder()
                .text("Processing data...")
                .color(GREEN)
                .inverted(true)
                .build(),
            invertedState);

        renderExample(frame, rows.get(2), "Slow speed (0.5x)",
            WaveText.builder()
                .text("Slow wave effect...")
                .color(YELLOW)
                .speed(0.5)
                .build(),
            slowState);

        renderExample(frame, rows.get(3), "Fast speed (3.0x)",
            WaveText.builder()
                .text("Fast wave effect...")
                .color(MAGENTA)
                .speed(3.0)
                .build(),
            fastState);

        renderExample(frame, rows.get(4), "Narrow peak (width=1)",
            WaveText.builder()
                .text("Narrow shadow peak...")
                .color(ORANGE)
                .peakWidth(1)
                .build(),
            narrowState);

        renderExample(frame, rows.get(5), "Wide peak (width=8)",
            WaveText.builder()
                .text("Wide shadow peak effect...")
                .color(PINK)
                .peakWidth(8)
                .build(),
            wideState);

        renderExample(frame, rows.get(6), "Multiple peaks (count=3)",
            WaveText.builder()
                .text("Multiple waves moving through text...")
                .color(CYAN)
                .peakCount(3)
                .build(),
            multiPeakState);

        renderExample(frame, rows.get(7), "Oscillate mode (back and forth)",
            WaveText.builder()
                .text("Bouncing wave effect...")
                .color(GREEN)
                .mode(WaveText.Mode.OSCILLATE)
                .build(),
            oscillateState);
    }

    private void renderExample(Frame frame, Rect area, String label, WaveText waveText, WaveTextState state) {
        // Split into label and wave text areas
        var cols = Layout.horizontal()
            .constraints(
                Constraint.length(45),  // Label
                Constraint.fill()       // Wave text
            )
            .split(area);

        // Render label
        Block labelBlock = Block.builder()
            .borders(Borders.ALL)
            .borderType(BorderType.ROUNDED)
            .borderStyle(Style.EMPTY.fg(Color.DARK_GRAY))
            .build();
        frame.renderWidget(labelBlock, cols.get(0));

        Rect labelInner = labelBlock.inner(cols.get(0));
        frame.buffer().setString(labelInner.x(), labelInner.y(), label, Style.EMPTY.fg(Color.WHITE));

        // Render wave text
        Block waveBlock = Block.builder()
            .borders(Borders.ALL)
            .borderType(BorderType.ROUNDED)
            .borderStyle(Style.EMPTY.fg(Color.BLUE))
            .build();
        frame.renderWidget(waveBlock, cols.get(1));

        Rect waveInner = waveBlock.inner(cols.get(1));
        frame.renderStatefulWidget(waveText, waveInner, state);
    }

    private void renderFooter(Frame frame, Rect area) {
        Line helpLine = Line.from(
            Span.raw(" Frame: ").dim(),
            Span.raw(String.valueOf(frameCount)).bold().cyan(),
            Span.raw("   "),
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
