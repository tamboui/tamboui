///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS dev.tamboui:tamboui-tui:LATEST
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
import dev.tamboui.terminal.Frame;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.tui.TuiRunner;
import dev.tamboui.tui.event.Event;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.MouseEvent;
import dev.tamboui.tui.event.MouseEventKind;
import dev.tamboui.tui.event.ResizeEvent;
import dev.tamboui.tui.event.TickEvent;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.widgets.block.Title;
import dev.tamboui.widgets.paragraph.Paragraph;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Demo showcasing the TuiRunner framework.
 * <p>
 * This demo shows:
 * <ul>
 *   <li>Keyboard handling with vim-style keys and arrows</li>
 *   <li>Mouse event handling (clicks, scroll, drag)</li>
 *   <li>Animation with tick events</li>
 *   <li>Window resize handling</li>
 * </ul>
 * <p>
 * Note how much simpler this is compared to basic-demo - no manual
 * escape sequence parsing, no raw mode management, no event loop boilerplate.
 */
public class TuiDemo {

    private int counter = 0;
    private int selectedPanel = 0;
    private final List<String> eventLog = new ArrayList<>();
    private int mouseX = -1;
    private int mouseY = -1;
    private long tickCount = 0;

    private TuiDemo() {

    }

    /**
     * Demo entry point.
     * @param args the CLI arguments
     * @throws Exception on unexpected error
     */
    public static void main(String[] args) throws Exception {
        new TuiDemo().run();
    }

    /**
     * Runs the demo application.
     *
     * @throws Exception if an error occurs
     */
     public void run() throws Exception {
        // Configure with mouse capture and animation ticks at 10 fps
        TuiConfig config = TuiConfig.builder()
                .mouseCapture(true)
                .tickRate(Duration.ofMillis(100))  // 10 fps for animation
                .build();

        try (TuiRunner tui = TuiRunner.create(config)) {
            tui.run(this::handleEvent, this::render);
        }
    }

    private boolean handleEvent(Event event, TuiRunner runner) {
        // Quit on q, Q, or Ctrl+C
        if (event instanceof KeyEvent && ((KeyEvent) event).isQuit()) {
            runner.quit();
            return false;
        }

        // Handle different event types
        if (event instanceof KeyEvent) {
            return handleKeyEvent((KeyEvent) event);
        }
        if (event instanceof MouseEvent) {
            return handleMouseEvent((MouseEvent) event);
        }
        if (event instanceof TickEvent) {
            return handleTickEvent((TickEvent) event);
        }
        if (event instanceof ResizeEvent) {
            return handleResizeEvent((ResizeEvent) event);
        }
        return true;
    }

    private boolean handleKeyEvent(KeyEvent k) {
        // Navigation with arrows (or vim keys if using vim keymap)
        if (k.isLeft() || k.isUp()) {
            selectedPanel = Math.max(0, selectedPanel - 1);
            logEvent("Key: navigate left/up");
            return true;
        }
        if (k.isRight() || k.isDown()) {
            selectedPanel = Math.min(2, selectedPanel + 1);
            logEvent("Key: navigate right/down");
            return true;
        }

        // Select with Enter or Space
        if (k.isSelect()) {
            counter++;
            logEvent("Key: select (counter=" + counter + ")");
            return true;
        }

        // Function keys
        if (isFunctionKey(k)) {
            int num = functionKeyNumber(k);
            logEvent("Key: F" + num);
            return true;
        }

        // Character input
        if (k.code() == KeyCode.CHAR) {
            logEvent("Key: '" + k.character() + "'");
            return true;
        }

        return false;
    }

    private static boolean isFunctionKey(KeyEvent k) {
        switch (k.code()) {
            case F1: case F2: case F3: case F4: case F5: case F6:
            case F7: case F8: case F9: case F10: case F11: case F12:
                return true;
            default:
                return false;
        }
    }

    private static int functionKeyNumber(KeyEvent k) {
        switch (k.code()) {
            case F1: return 1;
            case F2: return 2;
            case F3: return 3;
            case F4: return 4;
            case F5: return 5;
            case F6: return 6;
            case F7: return 7;
            case F8: return 8;
            case F9: return 9;
            case F10: return 10;
            case F11: return 11;
            case F12: return 12;
            default: return -1;
        }
    }

    private boolean handleMouseEvent(MouseEvent m) {
        mouseX = m.x();
        mouseY = m.y();

        String eventName = switch (m.kind()) {
            case PRESS -> "Mouse press " + m.button();
            case RELEASE -> "Mouse release";
            case DRAG -> "Mouse drag " + m.button();
            case MOVE -> "Mouse move";
            case SCROLL_UP -> "Scroll up";
            case SCROLL_DOWN -> "Scroll down";
        };

        logEvent(eventName + " at (" + m.x() + "," + m.y() + ")");

        // Handle scroll to change counter
        if (m.kind() == MouseEventKind.SCROLL_UP) {
            counter++;
        } else if (m.kind() == MouseEventKind.SCROLL_DOWN) {
            counter = Math.max(0, counter - 1);
        }

        return true;
    }

    private boolean handleTickEvent(TickEvent t) {
        tickCount = t.frameCount();
        // Only redraw every 5th tick to reduce flickering for event log
        return tickCount % 5 == 0;
    }

    private boolean handleResizeEvent(ResizeEvent r) {
        logEvent("Resize: " + r.width() + "x" + r.height());
        return true;
    }

    private void logEvent(String event) {
        eventLog.addFirst(event);
        if (eventLog.size() > 10) {
            eventLog.removeLast();
        }
    }

    private void render(Frame frame) {
        Rect area = frame.area();

        // Split into header, main content, and footer
        List<Rect> layout = Layout.vertical()
                .constraints(
                        Constraint.length(3),
                        Constraint.fill(),
                        Constraint.length(3)
                )
                .split(area);

        renderHeader(frame, layout.get(0));
        renderMain(frame, layout.get(1));
        renderFooter(frame, layout.get(2));
    }

    private void renderHeader(Frame frame, Rect area) {
        // Animated title using tick count
        String animation = switch ((int) (tickCount % 4)) {
            case 0 -> "⠋";
            case 1 -> "⠙";
            case 2 -> "⠹";
            default -> "⠸";
        };

        Block header = Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.CYAN))
                .title(Title.from(
                        Line.from(
                                Span.raw(" " + animation + " ").cyan(),
                                Span.raw("TuiRunner Demo ").bold().cyan(),
                                Span.raw(animation + " ").cyan()
                        )
                ).centered())
                .build();

        frame.renderWidget(header, area);
    }

    private void renderMain(Frame frame, Rect area) {
        // Split into 3 panels
        List<Rect> panels = Layout.horizontal()
                .constraints(
                        Constraint.ratio(1, 3),
                        Constraint.ratio(1, 3),
                        Constraint.ratio(1, 3)
                )
                .spacing(1)
                .split(area);

        renderStatsPanel(frame, panels.get(0), selectedPanel == 0);
        renderMousePanel(frame, panels.get(1), selectedPanel == 1);
        renderEventsPanel(frame, panels.get(2), selectedPanel == 2);
    }

    private void renderStatsPanel(Frame frame, Rect area, boolean focused) {
        Color borderColor = focused ? Color.GREEN : Color.DARK_GRAY;

        Text content = Text.from(
                Line.from(Span.raw("Counter: ").bold(), Span.raw(String.valueOf(counter)).yellow()),
                Line.empty(),
                Line.from(Span.raw("Selected Panel: ").bold(), Span.raw(String.valueOf(selectedPanel + 1)).cyan()),
                Line.empty(),
                Line.from(Span.raw("Tick: ").bold(), Span.raw(String.valueOf(tickCount)).magenta()),
                Line.empty(),
                Line.from(Span.raw("Press ").dim(), Span.raw("Enter/Space").yellow(), Span.raw(" to increment").dim()),
                Line.from(Span.raw("or scroll mouse wheel").dim())
        );

        Paragraph panel = Paragraph.builder()
                .text(content)
                .block(Block.builder()
                        .borders(Borders.ALL)
                        .borderType(BorderType.ROUNDED)
                        .borderStyle(Style.EMPTY.fg(borderColor))
                        .title(Title.from(
                                Line.from(
                                        Span.raw("Stats "),
                                        Span.raw(focused ? "●" : "○").fg(borderColor)
                                )
                        ))
                        .build())
                .build();

        frame.renderWidget(panel, area);
    }

    private void renderMousePanel(Frame frame, Rect area, boolean focused) {
        Color borderColor = focused ? Color.GREEN : Color.DARK_GRAY;

        String posText = mouseX >= 0 ? "(" + mouseX + ", " + mouseY + ")" : "Move mouse here";

        Text content = Text.from(
                Line.from(Span.raw("Mouse Position:").bold()),
                Line.empty(),
                Line.from(Span.raw("  " + posText).cyan()),
                Line.empty(),
                Line.from(Span.raw("Click, drag, or scroll").dim()),
                Line.from(Span.raw("to see mouse events").dim())
        );

        Paragraph panel = Paragraph.builder()
                .text(content)
                .block(Block.builder()
                        .borders(Borders.ALL)
                        .borderType(BorderType.ROUNDED)
                        .borderStyle(Style.EMPTY.fg(borderColor))
                        .title(Title.from(
                                Line.from(
                                        Span.raw("Mouse "),
                                        Span.raw(focused ? "●" : "○").fg(borderColor)
                                )
                        ))
                        .build())
                .build();

        frame.renderWidget(panel, area);
    }

    private void renderEventsPanel(Frame frame, Rect area, boolean focused) {
        Color borderColor = focused ? Color.GREEN : Color.DARK_GRAY;

        List<Line> lines = new ArrayList<>();
        lines.add(Line.from(Span.raw("Recent Events:").bold()));
        lines.add(Line.empty());

        if (eventLog.isEmpty()) {
            lines.add(Line.from(Span.raw("  No events yet").dim()));
        } else {
            for (int i = 0; i < Math.min(eventLog.size(), 8); i++) {
                String event = eventLog.get(i);
                Color color = i == 0 ? Color.GREEN : Color.WHITE;
                lines.add(Line.from(Span.raw("  " + event).fg(color)));
            }
        }

        Paragraph panel = Paragraph.builder()
                .text(Text.from(lines))
                .block(Block.builder()
                        .borders(Borders.ALL)
                        .borderType(BorderType.ROUNDED)
                        .borderStyle(Style.EMPTY.fg(borderColor))
                        .title(Title.from(
                                Line.from(
                                        Span.raw("Events "),
                                        Span.raw(focused ? "●" : "○").fg(borderColor)
                                )
                        ))
                        .build())
                .build();

        frame.renderWidget(panel, area);
    }

    private void renderFooter(Frame frame, Rect area) {
        Line helpLine = Line.from(
                Span.raw(" h/j/k/l/←↑↓→").bold().yellow(),
                Span.raw(" Navigate  ").dim(),
                Span.raw("Enter/Space").bold().yellow(),
                Span.raw(" Select  ").dim(),
                Span.raw("Scroll").bold().yellow(),
                Span.raw(" Counter  ").dim(),
                Span.raw("q/Ctrl+C").bold().yellow(),
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
