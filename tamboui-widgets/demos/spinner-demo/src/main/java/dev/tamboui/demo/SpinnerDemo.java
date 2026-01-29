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
import dev.tamboui.widgets.spinner.Spinner;
import dev.tamboui.widgets.spinner.SpinnerFrameSet;
import dev.tamboui.widgets.spinner.SpinnerState;
import dev.tamboui.widgets.spinner.SpinnerStyle;

/**
 * Demo TUI application showcasing the Spinner widget.
 * <p>
 * Demonstrates all built-in spinner styles:
 * <ul>
 *   <li>DOTS - Braille dot pattern</li>
 *   <li>LINE - Classic -\|/ spinner</li>
 *   <li>ARC - Quarter-circle characters</li>
 *   <li>CIRCLE - Clock-position circle</li>
 *   <li>BOUNCING_BAR - Bouncing bar effect</li>
 *   <li>TOGGLE - Two-state toggle</li>
 *   <li>Custom frames</li>
 * </ul>
 */
public class SpinnerDemo {

    private static final Color CYAN = Color.rgb(0, 180, 216);
    private static final Color GREEN = Color.rgb(46, 204, 113);
    private static final Color YELLOW = Color.rgb(241, 196, 15);
    private static final Color MAGENTA = Color.rgb(155, 89, 182);
    private static final Color BLUE = Color.rgb(52, 152, 219);
    private static final Color RED = Color.rgb(231, 76, 60);
    private static final Color ORANGE = Color.rgb(230, 126, 34);

    private boolean running = true;
    private long frameCount = 0;

    // Each spinner needs its own state to animate independently
    private final SpinnerState dotsState = new SpinnerState();
    private final SpinnerState lineState = new SpinnerState();
    private final SpinnerState arcState = new SpinnerState();
    private final SpinnerState circleState = new SpinnerState();
    private final SpinnerState bouncingBarState = new SpinnerState();
    private final SpinnerState toggleState = new SpinnerState();
    private final SpinnerState gaugeState = new SpinnerState();
    private final SpinnerState verticalGaugeState = new SpinnerState();
    private final SpinnerState arrowsState = new SpinnerState();
    private final SpinnerState clockState = new SpinnerState();
    private final SpinnerState moonState = new SpinnerState();
    private final SpinnerState squareCornersState = new SpinnerState();
    private final SpinnerState growingDotsState = new SpinnerState();
    private final SpinnerState bouncingBallState = new SpinnerState();
    private final SpinnerState customState = new SpinnerState();

    // Custom frame set example
    private static final SpinnerFrameSet CUSTOM_FRAMES = SpinnerFrameSet.of(
            "◐", "◓", "◑", "◒"  // rotating half-circle
    );

    private SpinnerDemo() {
    }

    /**
     * Demo entry point.
     * @param args the CLI arguments
     * @throws Exception on unexpected error
     */
    public static void main(String[] args) throws Exception {
        new SpinnerDemo().run();
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

                int c = backend.read(100);
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
        dotsState.advance();
        lineState.advance();
        arcState.advance();
        circleState.advance();
        bouncingBarState.advance();
        toggleState.advance();
        gaugeState.advance();
        verticalGaugeState.advance();
        arrowsState.advance();
        clockState.advance();
        moonState.advance();
        squareCornersState.advance();
        growingDotsState.advance();
        bouncingBallState.advance();
        customState.advance();
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
                    Span.raw("Spinner Demo ").yellow()
                )
            ).centered())
            .build();

        frame.renderWidget(headerBlock, area);
    }

    private void renderMainContent(Frame frame, Rect area) {
        // Split into two columns for more spinners
        var cols = Layout.horizontal()
            .constraints(
                Constraint.percentage(50),
                Constraint.percentage(50)
            )
            .split(area);

        // Left column - basic styles
        var leftRows = Layout.vertical()
            .constraints(
                Constraint.length(3),  // DOTS
                Constraint.length(3),  // LINE
                Constraint.length(3),  // ARC
                Constraint.length(3),  // CIRCLE
                Constraint.length(3),  // BOUNCING_BAR
                Constraint.length(3),  // TOGGLE
                Constraint.length(3)   // GAUGE
            )
            .split(cols.get(0));

        renderSpinnerRow(frame, leftRows.get(0), "DOTS (braille)",
            Spinner.builder().spinnerStyle(SpinnerStyle.DOTS).style(Style.EMPTY.fg(CYAN)).build(),
            dotsState);

        renderSpinnerRow(frame, leftRows.get(1), "LINE (-\\|/)",
            Spinner.builder().spinnerStyle(SpinnerStyle.LINE).style(Style.EMPTY.fg(GREEN)).build(),
            lineState);

        renderSpinnerRow(frame, leftRows.get(2), "ARC",
            Spinner.builder().spinnerStyle(SpinnerStyle.ARC).style(Style.EMPTY.fg(YELLOW)).build(),
            arcState);

        renderSpinnerRow(frame, leftRows.get(3), "CIRCLE",
            Spinner.builder().spinnerStyle(SpinnerStyle.CIRCLE).style(Style.EMPTY.fg(MAGENTA)).build(),
            circleState);

        renderSpinnerRow(frame, leftRows.get(4), "BOUNCING_BAR",
            Spinner.builder().spinnerStyle(SpinnerStyle.BOUNCING_BAR).style(Style.EMPTY.fg(BLUE)).build(),
            bouncingBarState);

        renderSpinnerRow(frame, leftRows.get(5), "TOGGLE",
            Spinner.builder().spinnerStyle(SpinnerStyle.TOGGLE).style(Style.EMPTY.fg(RED)).build(),
            toggleState);

        renderSpinnerRow(frame, leftRows.get(6), "GAUGE (block fill)",
            Spinner.builder().spinnerStyle(SpinnerStyle.GAUGE).style(Style.EMPTY.fg(CYAN)).build(),
            gaugeState);

        // Right column - new styles
        var rightRows = Layout.vertical()
            .constraints(
                Constraint.length(3),  // VERTICAL_GAUGE
                Constraint.length(3),  // ARROWS
                Constraint.length(3),  // CLOCK
                Constraint.length(3),  // MOON
                Constraint.length(3),  // SQUARE_CORNERS
                Constraint.length(3),  // GROWING_DOTS
                Constraint.length(3),  // BOUNCING_BALL
                Constraint.length(3)   // Custom FrameSet
            )
            .split(cols.get(1));

        renderSpinnerRow(frame, rightRows.get(0), "VERTICAL_GAUGE",
            Spinner.builder().spinnerStyle(SpinnerStyle.VERTICAL_GAUGE).style(Style.EMPTY.fg(GREEN)).build(),
            verticalGaugeState);

        renderSpinnerRow(frame, rightRows.get(1), "ARROWS",
            Spinner.builder().spinnerStyle(SpinnerStyle.ARROWS).style(Style.EMPTY.fg(YELLOW)).build(),
            arrowsState);

        renderSpinnerRow(frame, rightRows.get(2), "CLOCK",
            Spinner.builder().spinnerStyle(SpinnerStyle.CLOCK).style(Style.EMPTY.fg(MAGENTA)).build(),
            clockState);

        renderSpinnerRow(frame, rightRows.get(3), "MOON",
            Spinner.builder().spinnerStyle(SpinnerStyle.MOON).style(Style.EMPTY.fg(BLUE)).build(),
            moonState);

        renderSpinnerRow(frame, rightRows.get(4), "SQUARE_CORNERS",
            Spinner.builder().spinnerStyle(SpinnerStyle.SQUARE_CORNERS).style(Style.EMPTY.fg(RED)).build(),
            squareCornersState);

        renderSpinnerRow(frame, rightRows.get(5), "GROWING_DOTS",
            Spinner.builder().spinnerStyle(SpinnerStyle.GROWING_DOTS).style(Style.EMPTY.fg(ORANGE)).build(),
            growingDotsState);

        renderSpinnerRow(frame, rightRows.get(6), "BOUNCING_BALL",
            Spinner.builder().spinnerStyle(SpinnerStyle.BOUNCING_BALL).style(Style.EMPTY.fg(CYAN)).build(),
            bouncingBallState);

        renderSpinnerRow(frame, rightRows.get(7), "Custom FrameSet",
            Spinner.builder().frameSet(CUSTOM_FRAMES).style(Style.EMPTY.fg(MAGENTA)).build(),
            customState);
    }

    private void renderSpinnerRow(Frame frame, Rect area, String label, Spinner spinner, SpinnerState state) {
        // Split into label and spinner areas
        var cols = Layout.horizontal()
            .constraints(
                Constraint.length(35),  // Label
                Constraint.length(10),  // Spinner
                Constraint.fill()       // Spacer
            )
            .split(area);

        // Render label block
        Block labelBlock = Block.builder()
            .borders(Borders.ALL)
            .borderType(BorderType.ROUNDED)
            .borderStyle(Style.EMPTY.fg(Color.DARK_GRAY))
            .build();
        frame.renderWidget(labelBlock, cols.get(0));

        Rect labelInner = labelBlock.inner(cols.get(0));
        frame.buffer().setString(labelInner.x(), labelInner.y(), label, Style.EMPTY.fg(Color.WHITE));

        // Render spinner block
        Block spinnerBlock = Block.builder()
            .borders(Borders.ALL)
            .borderType(BorderType.ROUNDED)
            .borderStyle(Style.EMPTY.fg(Color.BLUE))
            .build();
        frame.renderWidget(spinnerBlock, cols.get(1));

        Rect spinnerInner = spinnerBlock.inner(cols.get(1));
        frame.renderStatefulWidget(spinner, spinnerInner, state);
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
