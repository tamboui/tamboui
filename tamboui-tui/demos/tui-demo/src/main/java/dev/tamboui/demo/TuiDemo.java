///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS dev.tamboui:tamboui-tui:LATEST
//DEPS dev.tamboui:tamboui-jline3-backend:LATEST

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

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
import dev.tamboui.tui.bindings.Actions;
import dev.tamboui.tui.bindings.BindingSets;
import dev.tamboui.tui.bindings.Bindings;
import dev.tamboui.tui.bindings.KeyTrigger;
import dev.tamboui.tui.event.Event;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.KeyModifiers;
import dev.tamboui.tui.event.MouseButton;
import dev.tamboui.tui.event.MouseEvent;
import dev.tamboui.tui.event.MouseEventKind;
import dev.tamboui.tui.event.ResizeEvent;
import dev.tamboui.tui.event.TickEvent;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.widgets.block.Title;
import dev.tamboui.widgets.paragraph.Paragraph;

/**
 * Rich event inspector and input device showcase for the TuiRunner framework.
 * <p>
 * This demo provides a visual test harness and demonstration of:
 * <ul>
 *   <li>Interactive ASCII/Unicode mouse device with live buttons, wheel, and hover</li>
 *   <li>Horizontal scroll wheel events (SCROLL_LEFT and SCROLL_RIGHT from #399)</li>
 *   <li>Mouse motion / hover events (DECSET 1003 from #390) with runtime toggle</li>
 *   <li>Rich event inspector stream with color-coded badges and modifiers</li>
 *   <li>Key identity and produced text inspection (aligned with Kitty keyboard #446)</li>
 *   <li>Terminal capability and protocol probe readout</li>
 *   <li>Responsive multi-pane layout adapting dynamically to terminal resize</li>
 * </ul>
 */
public class TuiDemo {

    private static final int MAX_EVENTS = 100;

    private int counter = 0;
    private int selectedPanel = 0;
    private final List<EventRecord> eventLog = new ArrayList<>();

    private int mouseX = -1;
    private int mouseY = -1;
    private boolean mouseMotionEnabled = true;

    private boolean leftDown = false;
    private boolean middleDown = false;
    private boolean rightDown = false;

    private int scrollUpTicks = 0;
    private int scrollDownTicks = 0;
    private int scrollLeftTicks = 0;
    private int scrollRightTicks = 0;
    private int hoverTicks = 0;

    private KeyModifiers lastModifiers = KeyModifiers.NONE;
    private KeyCode lastKeyCode = null;
    private String lastProducedText = "";

    private long tickCount = 0;
    private long totalEventsCount = 0;

    private String backendName = null;
    private int termWidth = 0;
    private int termHeight = 0;

    private Rect capabilitiesPanelRect;
    private Rect mousePanelRect;
    private Rect eventsPanelRect;

    /**
     * Creates a new TuiDemo instance.
     */
    TuiDemo() {
    }

    /**
     * Demo entry point.
     *
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
        // Bind plain F12 to the built-in debug overlay (backend/FPS/runtime popup),
        // in addition to the default Ctrl+Shift+F12.
        Bindings bindings = BindingSets.defaults().toBuilder()
                .bind(KeyTrigger.key(KeyCode.F12), Actions.TOGGLE_DEBUG_OVERLAY)
                .build();

        TuiConfig config = TuiConfig.builder()
                .mouseCapture(true)
                .mouseMotion(true)  // deliver hover (MOVE) events, not just drags
                .tickRate(Duration.ofMillis(100))  // 10 fps for animation
                .bindings(bindings)
                .build();

        try (TuiRunner tui = TuiRunner.create(config)) {
            this.backendName = tui.backend().getClass().getSimpleName();
            tui.run(this::handleEvent, this::render);
        }
    }

    /**
     * Dispatches an incoming event to the corresponding handler.
     *
     * @param event  the input event
     * @param runner the active runner instance
     * @return true to request redraw, false to skip redraw or on quit
     */
    boolean handleEvent(Event event, TuiRunner runner) {
        if (backendName == null && runner != null && runner.backend() != null) {
            backendName = runner.backend().getClass().getSimpleName();
        }

        // Quit on q, Q, or Ctrl+C
        if (event instanceof KeyEvent && ((KeyEvent) event).isQuit()) {
            runner.quit();
            return false;
        }

        // Handle different event types
        if (event instanceof KeyEvent) {
            return handleKeyEvent((KeyEvent) event, runner);
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

    private boolean handleKeyEvent(KeyEvent k, TuiRunner runner) {
        lastKeyCode = k.code();
        lastProducedText = k.string();
        lastModifiers = k.modifiers();

        // Runtime toggle for mouse motion (DECSET 1003) via 'm' or 'M'
        if (k.code() == KeyCode.CHAR && k.string().equalsIgnoreCase("m")) {
            mouseMotionEnabled = !mouseMotionEnabled;
            if (runner != null) {
                try {
                    runner.backend().enableMouseCapture(mouseMotionEnabled);
                } catch (Exception ignored) {
                    // ignore backend toggle errors
                }
            }
            logEvent(new EventRecord(
                    ++totalEventsCount,
                    " SYSTEM ",
                    Color.YELLOW,
                    Color.BLACK,
                    "Motion tracking " + (mouseMotionEnabled ? "ON (DECSET 1003)" : "OFF (DECSET 1002)"),
                    k.modifiers(),
                    "Toggled hover motion capture"
            ));
            return true;
        }

        // Clear event stream via 'c' or 'C'
        if (k.code() == KeyCode.CHAR && k.string().equalsIgnoreCase("c")) {
            eventLog.clear();
            logEvent(new EventRecord(
                    ++totalEventsCount,
                    " SYSTEM ",
                    Color.WHITE,
                    Color.BLACK,
                    "Event inspector log cleared",
                    k.modifiers(),
                    "Log buffer reset"
            ));
            return true;
        }

        // Navigation with arrows or vim keys
        if (k.isLeft() || k.isUp()) {
            selectedPanel = Math.max(0, selectedPanel - 1);
            logEvent(new EventRecord(
                    ++totalEventsCount,
                    " NAV-LT ",
                    Color.CYAN,
                    Color.BLACK,
                    "Nav Left/Up -> Panel " + (selectedPanel + 1),
                    k.modifiers(),
                    "Focus shifted left"
            ));
            return true;
        }
        if (k.isRight() || k.isDown()) {
            selectedPanel = Math.min(2, selectedPanel + 1);
            logEvent(new EventRecord(
                    ++totalEventsCount,
                    " NAV-RT ",
                    Color.CYAN,
                    Color.BLACK,
                    "Nav Right/Down -> Panel " + (selectedPanel + 1),
                    k.modifiers(),
                    "Focus shifted right"
            ));
            return true;
        }

        // Select with Enter or Space
        if (k.isSelect()) {
            counter++;
            logEvent(new EventRecord(
                    ++totalEventsCount,
                    " SELECT ",
                    Color.GREEN,
                    Color.BLACK,
                    "Select (" + k.code().name() + ") counter=" + counter,
                    k.modifiers(),
                    "Counter incremented"
            ));
            return true;
        }

        // Function keys
        if (isFunctionKey(k)) {
            int num = functionKeyNumber(k);
            logEvent(new EventRecord(
                    ++totalEventsCount,
                    " F-KEY  ",
                    Color.MAGENTA,
                    Color.WHITE,
                    "Function Key F" + num,
                    k.modifiers(),
                    "F" + num + " pressed"
            ));
            return true;
        }

        // Printable character input
        if (k.code() == KeyCode.CHAR) {
            String charDisplay = "'" + k.string() + "'";
            String cpHex = String.format("U+%04X", k.codePoint());
            logEvent(new EventRecord(
                    ++totalEventsCount,
                    " KEY    ",
                    Color.CYAN,
                    Color.BLACK,
                    charDisplay + " (" + cpHex + ")",
                    k.modifiers(),
                    "Char input: " + k.string()
            ));
            return true;
        }

        // Other special keys (Escape, Tab, Backspace, etc.)
        logEvent(new EventRecord(
                ++totalEventsCount,
                " KEY    ",
                Color.CYAN,
                Color.BLACK,
                k.code().name(),
                k.modifiers(),
                "Key code: " + k.code()
        ));
        return true;
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
        lastModifiers = m.modifiers();

        // Mouse click on panels selects focus
        if (m.kind() == MouseEventKind.PRESS) {
            if (capabilitiesPanelRect != null && capabilitiesPanelRect.contains(m.x(), m.y())) {
                selectedPanel = 0;
            } else if (mousePanelRect != null && mousePanelRect.contains(m.x(), m.y())) {
                selectedPanel = 1;
            } else if (eventsPanelRect != null && eventsPanelRect.contains(m.x(), m.y())) {
                selectedPanel = 2;
            }
        }

        switch (m.kind()) {
            case PRESS -> {
                if (m.button() == MouseButton.LEFT) {
                    leftDown = true;
                } else if (m.button() == MouseButton.MIDDLE) {
                    middleDown = true;
                } else if (m.button() == MouseButton.RIGHT) {
                    rightDown = true;
                }
                logEvent(new EventRecord(
                        ++totalEventsCount,
                        " PRESS  ",
                        Color.GREEN,
                        Color.BLACK,
                        m.button() + " @ (" + m.x() + ", " + m.y() + ")",
                        m.modifiers(),
                        "Button pressed"
                ));
            }
            case RELEASE -> {
                if (m.button() == MouseButton.LEFT) {
                    leftDown = false;
                } else if (m.button() == MouseButton.MIDDLE) {
                    middleDown = false;
                } else if (m.button() == MouseButton.RIGHT) {
                    rightDown = false;
                } else {
                    leftDown = false;
                    middleDown = false;
                    rightDown = false;
                }
                String btnText = m.button() != MouseButton.NONE ? m.button() + " " : "";
                logEvent(new EventRecord(
                        ++totalEventsCount,
                        " RELEASE",
                        Color.DARK_GRAY,
                        Color.WHITE,
                        btnText + "@ (" + m.x() + ", " + m.y() + ")",
                        m.modifiers(),
                        "Button released"
                ));
            }
            case DRAG -> {
                if (m.button() == MouseButton.LEFT) {
                    leftDown = true;
                } else if (m.button() == MouseButton.MIDDLE) {
                    middleDown = true;
                } else if (m.button() == MouseButton.RIGHT) {
                    rightDown = true;
                }
                hoverTicks = 4;
                logEvent(new EventRecord(
                        ++totalEventsCount,
                        " DRAG   ",
                        Color.YELLOW,
                        Color.BLACK,
                        m.button() + " @ (" + m.x() + ", " + m.y() + ")",
                        m.modifiers(),
                        "Mouse drag with button"
                ));
            }
            case MOVE -> {
                hoverTicks = 4;
                logEvent(new EventRecord(
                        ++totalEventsCount,
                        " MOVE   ",
                        Color.BLUE,
                        Color.WHITE,
                        "Hover @ (" + m.x() + ", " + m.y() + ")",
                        m.modifiers(),
                        "DECSET 1003 motion"
                ));
            }
            case SCROLL_UP -> {
                scrollUpTicks = 4;
                counter++;
                logEvent(new EventRecord(
                        ++totalEventsCount,
                        " SCR-UP ",
                        Color.MAGENTA,
                        Color.WHITE,
                        "▲ Up (c=" + counter + ") @ (" + m.x() + ", " + m.y() + ")",
                        m.modifiers(),
                        "Vertical scroll up"
                ));
            }
            case SCROLL_DOWN -> {
                scrollDownTicks = 4;
                counter = Math.max(0, counter - 1);
                logEvent(new EventRecord(
                        ++totalEventsCount,
                        " SCR-DN ",
                        Color.MAGENTA,
                        Color.WHITE,
                        "▼ Down (c=" + counter + ") @ (" + m.x() + ", " + m.y() + ")",
                        m.modifiers(),
                        "Vertical scroll down"
                ));
            }
            case SCROLL_LEFT -> {
                scrollLeftTicks = 4;
                logEvent(new EventRecord(
                        ++totalEventsCount,
                        " SCR-LT ",
                        Color.CYAN,
                        Color.BLACK,
                        "◄ Left (#399) @ (" + m.x() + ", " + m.y() + ")",
                        m.modifiers(),
                        "Horizontal scroll tilt/gesture"
                ));
            }
            case SCROLL_RIGHT -> {
                scrollRightTicks = 4;
                logEvent(new EventRecord(
                        ++totalEventsCount,
                        " SCR-RT ",
                        Color.CYAN,
                        Color.BLACK,
                        "► Right (#399) @ (" + m.x() + ", " + m.y() + ")",
                        m.modifiers(),
                        "Horizontal scroll tilt/gesture"
                ));
            }
        }
        return true;
    }

    private boolean handleTickEvent(TickEvent t) {
        tickCount = t.frameCount();
        if (scrollUpTicks > 0) {
            scrollUpTicks--;
        }
        if (scrollDownTicks > 0) {
            scrollDownTicks--;
        }
        if (scrollLeftTicks > 0) {
            scrollLeftTicks--;
        }
        if (scrollRightTicks > 0) {
            scrollRightTicks--;
        }
        if (hoverTicks > 0) {
            hoverTicks--;
        }
        // Always return true so title animation and decay highlights render smoothly
        return true;
    }

    private boolean handleResizeEvent(ResizeEvent r) {
        termWidth = r.width();
        termHeight = r.height();
        logEvent(new EventRecord(
                ++totalEventsCount,
                " RESIZE ",
                Color.WHITE,
                Color.BLACK,
                r.width() + "x" + r.height() + " cells",
                KeyModifiers.NONE,
                "Terminal window resized"
        ));
        return true;
    }

    private void logEvent(EventRecord record) {
        eventLog.addFirst(record);
        if (eventLog.size() > MAX_EVENTS) {
            eventLog.removeLast();
        }
    }

    void render(Frame frame) {
        Rect area = frame.area();
        termWidth = area.width();
        termHeight = area.height();

        // Split into header, main multi-pane content, and footer
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
                                Span.raw("TamboUI Input Inspector & Device Demo ").bold().cyan(),
                                Span.raw(animation + " ").cyan()
                        )
                ).centered())
                .build();

        frame.renderWidget(header, area);
    }

    private void renderMain(Frame frame, Rect area) {
        if (area.width() >= 80) {
            int leftWidth = Math.min(32, Math.max(26, area.width() / 4));
            int midWidth = 35;
            List<Rect> panels = Layout.horizontal()
                    .constraints(
                            Constraint.length(leftWidth),
                            Constraint.length(midWidth),
                            Constraint.fill()
                    )
                    .spacing(1)
                    .split(area);

            renderCapabilitiesPanel(frame, panels.get(0), selectedPanel == 0);
            renderMousePanel(frame, panels.get(1), selectedPanel == 1);
            renderEventsPanel(frame, panels.get(2), selectedPanel == 2);
        } else {
            // Responsive fallback for narrower terminals (< 80 columns)
            List<Rect> rows = Layout.vertical()
                    .constraints(
                            Constraint.length(Math.min(14, area.height() / 2)),
                            Constraint.fill()
                    )
                    .split(area);

            List<Rect> topPanels = Layout.horizontal()
                    .constraints(
                            Constraint.ratio(1, 2),
                            Constraint.ratio(1, 2)
                    )
                    .spacing(1)
                    .split(rows.get(0));

            renderCapabilitiesPanel(frame, topPanels.get(0), selectedPanel == 0);
            renderMousePanel(frame, topPanels.get(1), selectedPanel == 1);
            renderEventsPanel(frame, rows.get(1), selectedPanel == 2);
        }
    }

    private void renderCapabilitiesPanel(Frame frame, Rect area, boolean focused) {
        capabilitiesPanelRect = area;
        Color borderColor = focused ? Color.GREEN : Color.DARK_GRAY;

        List<Line> lines = new ArrayList<>();
        lines.add(Line.from(Span.raw("Application State").bold().cyan()));
        lines.add(Line.from(Span.raw("  Counter:      ").dim(), Span.raw(String.valueOf(counter)).yellow().bold()));
        lines.add(Line.from(Span.raw("  Active Panel: ").dim(), Span.raw((selectedPanel + 1) + " of 3").cyan()));
        lines.add(Line.from(Span.raw("  Tick Count:   ").dim(), Span.raw(String.valueOf(tickCount)).magenta()));
        lines.add(Line.from(Span.raw("  Total Events: ").dim(), Span.raw(String.valueOf(totalEventsCount)).green()));
        lines.add(Line.empty());

        lines.add(Line.from(Span.raw("Capability Probe").bold().cyan()));
        lines.add(Line.from(Span.raw("  Backend:      ").dim(), Span.raw(backendName != null ? backendName : "Auto").green()));
        lines.add(Line.from(Span.raw("  Terminal:     ").dim(), Span.raw(termWidth + "x" + termHeight).cyan()));
        lines.add(Line.from(Span.raw("  Mouse SGR:    ").dim(), Span.raw("1006 Extended").green()));
        lines.add(Line.from(Span.raw("  Motion 1003:  ").dim(),
                Span.raw(mouseMotionEnabled ? "ON (Any-event)" : "OFF (Clicks/drag)").style(
                        mouseMotionEnabled ? Style.EMPTY.fg(Color.GREEN).bold() : Style.EMPTY.fg(Color.RED))));
        lines.add(Line.from(Span.raw("  Horiz Scroll: ").dim(), Span.raw("DECSET (#399)").magenta()));
        lines.add(Line.from(Span.raw("  Brkt Paste:   ").dim(), Span.raw("2004 Enabled").dim()));
        lines.add(Line.empty());

        lines.add(Line.from(Span.raw("Live Modifiers").bold().cyan()));
        lines.add(Line.from(
                Span.raw("  "),
                Span.raw(" CTRL ").style(lastModifiers.ctrl()
                        ? Style.EMPTY.bg(Color.YELLOW).fg(Color.BLACK).bold()
                        : Style.EMPTY.fg(Color.DARK_GRAY)),
                Span.raw(" "),
                Span.raw(" ALT ").style(lastModifiers.alt()
                        ? Style.EMPTY.bg(Color.CYAN).fg(Color.BLACK).bold()
                        : Style.EMPTY.fg(Color.DARK_GRAY)),
                Span.raw(" "),
                Span.raw(" SHIFT ").style(lastModifiers.shift()
                        ? Style.EMPTY.bg(Color.MAGENTA).fg(Color.WHITE).bold()
                        : Style.EMPTY.fg(Color.DARK_GRAY))
        ));

        if (area.height() >= 22) {
            lines.add(Line.empty());
            lines.add(Line.from(Span.raw("Last Key Identity (#446)").bold().cyan()));
            lines.add(Line.from(Span.raw("  Code: ").dim(), Span.raw(lastKeyCode != null ? lastKeyCode.name() : "None").yellow()));
            String textDisplay = lastProducedText.isEmpty() ? "None" : "'" + lastProducedText + "'";
            lines.add(Line.from(Span.raw("  Text: ").dim(), Span.raw(textDisplay).white()));
        }

        Paragraph panel = Paragraph.builder()
                .text(Text.from(lines))
                .block(Block.builder()
                        .borders(Borders.ALL)
                        .borderType(BorderType.ROUNDED)
                        .borderStyle(Style.EMPTY.fg(borderColor))
                        .title(Title.from(
                                Line.from(
                                        Span.raw("System & Probe "),
                                        Span.raw(focused ? "●" : "○").fg(borderColor)
                                )
                        ))
                        .build())
                .build();

        frame.renderWidget(panel, area);
    }

    private void renderMousePanel(Frame frame, Rect area, boolean focused) {
        mousePanelRect = area;
        Color borderColor = focused ? Color.GREEN : Color.DARK_GRAY;

        // Visual styles for interactive device components
        Style leftStyle = leftDown
                ? Style.EMPTY.bg(Color.GREEN).fg(Color.BLACK).bold()
                : Style.EMPTY.fg(Color.CYAN);

        Style rightStyle = rightDown
                ? Style.EMPTY.bg(Color.RED).fg(Color.WHITE).bold()
                : Style.EMPTY.fg(Color.CYAN);

        Style middleStyle = middleDown
                ? Style.EMPTY.bg(Color.YELLOW).fg(Color.BLACK).bold()
                : Style.EMPTY.fg(Color.DARK_GRAY);

        Style scrollUpStyle = scrollUpTicks > 0
                ? Style.EMPTY.bg(Color.YELLOW).fg(Color.BLACK).bold()
                : Style.EMPTY.fg(Color.DARK_GRAY);

        Style scrollDownStyle = scrollDownTicks > 0
                ? Style.EMPTY.bg(Color.YELLOW).fg(Color.BLACK).bold()
                : Style.EMPTY.fg(Color.DARK_GRAY);

        Style scrollLeftStyle = scrollLeftTicks > 0
                ? Style.EMPTY.bg(Color.MAGENTA).fg(Color.WHITE).bold()
                : Style.EMPTY.fg(Color.DARK_GRAY);

        Style scrollRightStyle = scrollRightTicks > 0
                ? Style.EMPTY.bg(Color.MAGENTA).fg(Color.WHITE).bold()
                : Style.EMPTY.fg(Color.DARK_GRAY);

        Style hoverStyle = hoverTicks > 0
                ? Style.EMPTY.fg(Color.GREEN).bold()
                : Style.EMPTY.fg(Color.DARK_GRAY);

        int pad = Math.max(0, (area.width() - 2 - 29) / 2);
        String indent = " ".repeat(pad);

        String posStr = mouseX >= 0 ? "(" + mouseX + ", " + mouseY + ")" : "none";
        String posLine = String.format("   Position: %-14s", posStr);

        List<Line> lines = new ArrayList<>();
        // Row 1: Top border
        lines.add(Line.from(Span.raw(indent), Span.raw("╭──────────┬─────┬──────────╮").fg(Color.DARK_GRAY)));

        // Row 2: Top buttons and scroll up arrow
        lines.add(Line.from(
                Span.raw(indent),
                Span.raw("│").fg(Color.DARK_GRAY),
                Span.raw(leftDown ? "   DOWN   " : "          ").style(leftStyle),
                Span.raw("│").fg(Color.DARK_GRAY),
                Span.raw("  ▲  ").style(scrollUpStyle),
                Span.raw("│").fg(Color.DARK_GRAY),
                Span.raw(rightDown ? "   DOWN   " : "          ").style(rightStyle),
                Span.raw("│").fg(Color.DARK_GRAY)
        ));

        // Row 3: Button labels and middle click button
        lines.add(Line.from(
                Span.raw(indent),
                Span.raw("│").fg(Color.DARK_GRAY),
                Span.raw(leftDown ? "  [L-BTN] " : "   LEFT   ").style(leftStyle),
                Span.raw("│").fg(Color.DARK_GRAY),
                Span.raw(middleDown ? "  ◉  " : "  ○  ").style(middleStyle),
                Span.raw("│").fg(Color.DARK_GRAY),
                Span.raw(rightDown ? " [R-BTN]  " : "  RIGHT   ").style(rightStyle),
                Span.raw("│").fg(Color.DARK_GRAY)
        ));

        // Row 4: Held status and scroll down arrow
        lines.add(Line.from(
                Span.raw(indent),
                Span.raw("│").fg(Color.DARK_GRAY),
                Span.raw(leftDown ? "   HELD   " : "          ").style(leftStyle),
                Span.raw("│").fg(Color.DARK_GRAY),
                Span.raw("  ▼  ").style(scrollDownStyle),
                Span.raw("│").fg(Color.DARK_GRAY),
                Span.raw(rightDown ? "   HELD   " : "          ").style(rightStyle),
                Span.raw("│").fg(Color.DARK_GRAY)
        ));

        // Row 5: Middle divider
        lines.add(Line.from(Span.raw(indent), Span.raw("├──────────┴─────┴──────────┤").fg(Color.DARK_GRAY)));

        // Row 6: Horizontal scroll wheel (#399)
        lines.add(Line.from(
                Span.raw(indent),
                Span.raw("│").fg(Color.DARK_GRAY),
                Span.raw("      "),
                Span.raw(" ◄ ").style(scrollLeftStyle),
                Span.raw("  WHEEL  ").fg(Color.DARK_GRAY),
                Span.raw(" ► ").style(scrollRightStyle),
                Span.raw("      "),
                Span.raw("│").fg(Color.DARK_GRAY)
        ));

        // Row 7: Empty mouse body spacer
        lines.add(Line.from(Span.raw(indent), Span.raw("│                           │").fg(Color.DARK_GRAY)));

        // Row 8: Hover motion indicator (#390)
        String hoverText = hoverTicks > 0 ? "     ● MOTION (HOVER)      " : "     ○ IDLE (NO MOTION)    ";
        lines.add(Line.from(
                Span.raw(indent),
                Span.raw("│").fg(Color.DARK_GRAY),
                Span.raw(hoverText).style(hoverStyle),
                Span.raw("│").fg(Color.DARK_GRAY)
        ));

        // Row 9: Live coordinates readout
        lines.add(Line.from(
                Span.raw(indent),
                Span.raw("│").fg(Color.DARK_GRAY),
                Span.raw(posLine).yellow(),
                Span.raw("│").fg(Color.DARK_GRAY)
        ));

        // Row 10: Bottom border
        lines.add(Line.from(Span.raw(indent), Span.raw("╰───────────────────────────╯").fg(Color.DARK_GRAY)));

        // Status lines if panel is tall enough
        if (area.height() >= 17) {
            lines.add(Line.empty());
            lines.add(Line.from(
                    Span.raw("  Buttons: ").dim(),
                    Span.raw("[L: " + (leftDown ? "DOWN" : "UP") + "] ").style(leftDown ? Style.EMPTY.fg(Color.GREEN).bold() : Style.EMPTY.fg(Color.DARK_GRAY)),
                    Span.raw("[M: " + (middleDown ? "DOWN" : "UP") + "] ").style(middleDown ? Style.EMPTY.fg(Color.YELLOW).bold() : Style.EMPTY.fg(Color.DARK_GRAY)),
                    Span.raw("[R: " + (rightDown ? "DOWN" : "UP") + "]").style(rightDown ? Style.EMPTY.fg(Color.RED).bold() : Style.EMPTY.fg(Color.DARK_GRAY))
            ));
            lines.add(Line.from(
                    Span.raw("  Scroll:  ").dim(),
                    Span.raw("▲/▼ Vertical  ").fg(scrollUpTicks > 0 || scrollDownTicks > 0 ? Color.YELLOW : Color.DARK_GRAY),
                    Span.raw("◄/► Horiz (#399)").fg(scrollLeftTicks > 0 || scrollRightTicks > 0 ? Color.MAGENTA : Color.DARK_GRAY)
            ));
        }

        if (area.height() >= 20) {
            lines.add(Line.from(
                    Span.raw("  Motion:  ").dim(),
                    Span.raw(mouseMotionEnabled ? "DECSET 1003 (Active)" : "DECSET 1002 (Clicks/Drag)").fg(mouseMotionEnabled ? Color.GREEN : Color.YELLOW)
            ));
        }

        Paragraph panel = Paragraph.builder()
                .text(Text.from(lines))
                .block(Block.builder()
                        .borders(Borders.ALL)
                        .borderType(BorderType.ROUNDED)
                        .borderStyle(Style.EMPTY.fg(borderColor))
                        .title(Title.from(
                                Line.from(
                                        Span.raw("Mouse Device Inspector "),
                                        Span.raw(focused ? "●" : "○").fg(borderColor)
                                )
                        ))
                        .build())
                .build();

        frame.renderWidget(panel, area);
    }

    private void renderEventsPanel(Frame frame, Rect area, boolean focused) {
        eventsPanelRect = area;
        Color borderColor = focused ? Color.GREEN : Color.DARK_GRAY;

        List<Line> lines = new ArrayList<>();
        lines.add(Line.from(
                Span.raw("Event Stream ").bold().cyan(),
                Span.raw("(" + eventLog.size() + " recorded)").dim()
        ));
        lines.add(Line.empty());

        if (eventLog.isEmpty()) {
            lines.add(Line.from(Span.raw("  No input events received yet").dim()));
            lines.add(Line.from(Span.raw("  Interact using keys, mouse clicks, drags or wheel!").dim()));
        } else {
            int availableLines = Math.max(1, area.height() - 4);
            int count = Math.min(eventLog.size(), availableLines);
            for (int i = 0; i < count; i++) {
                EventRecord rec = eventLog.get(i);
                List<Span> spans = new ArrayList<>();

                if (i == 0) {
                    spans.add(Span.raw("▶ ").green().bold());
                } else {
                    spans.add(Span.raw("  "));
                }

                spans.add(Span.raw(String.format("#%03d ", rec.seq % 1000)).dim());
                spans.add(Span.raw(rec.badge).style(Style.EMPTY.bg(rec.badgeBg).fg(rec.badgeFg).bold()));
                spans.add(Span.raw(" "));

                Color textColor = i == 0 ? Color.WHITE : Color.GRAY;
                Style textStyle = i == 0 ? Style.EMPTY.fg(textColor).bold() : Style.EMPTY.fg(textColor);
                spans.add(Span.raw(rec.summary).style(textStyle));

                if (rec.modifiers.hasAny()) {
                    spans.add(Span.raw(" "));
                    if (rec.modifiers.ctrl()) {
                        spans.add(Span.raw("[Ctrl]").yellow().bold());
                    }
                    if (rec.modifiers.alt()) {
                        spans.add(Span.raw("[Alt]").cyan().bold());
                    }
                    if (rec.modifiers.shift()) {
                        spans.add(Span.raw("[Shift]").magenta().bold());
                    }
                }

                lines.add(Line.from(spans));
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
                                        Span.raw("Events Inspector "),
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
                Span.raw("m").bold().yellow(),
                Span.raw(" Motion:").dim(),
                Span.raw(mouseMotionEnabled ? "ON " : "OFF ").fg(mouseMotionEnabled ? Color.GREEN : Color.RED).bold(),
                Span.raw("c").bold().yellow(),
                Span.raw(" Clear  ").dim(),
                Span.raw("F12").bold().yellow(),
                Span.raw(" Debug  ").dim(),
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

    // Getters for testing and external state inspection

    int counter() {
        return counter;
    }

    int selectedPanel() {
        return selectedPanel;
    }

    int mouseX() {
        return mouseX;
    }

    int mouseY() {
        return mouseY;
    }

    boolean isLeftDown() {
        return leftDown;
    }

    boolean isMiddleDown() {
        return middleDown;
    }

    boolean isRightDown() {
        return rightDown;
    }

    int scrollUpTicks() {
        return scrollUpTicks;
    }

    int scrollDownTicks() {
        return scrollDownTicks;
    }

    int scrollLeftTicks() {
        return scrollLeftTicks;
    }

    int scrollRightTicks() {
        return scrollRightTicks;
    }

    int hoverTicks() {
        return hoverTicks;
    }

    boolean mouseMotionEnabled() {
        return mouseMotionEnabled;
    }

    KeyModifiers lastModifiers() {
        return lastModifiers;
    }

    List<EventRecord> eventLog() {
        return eventLog;
    }

    /**
     * Immutable record representing a single logged input event.
     */
    static final class EventRecord {
        private final long seq;
        private final String badge;
        private final Color badgeBg;
        private final Color badgeFg;
        private final String summary;
        private final KeyModifiers modifiers;
        private final String detail;

        EventRecord(long seq, String badge, Color badgeBg, Color badgeFg,
                    String summary, KeyModifiers modifiers, String detail) {
            this.seq = seq;
            this.badge = badge;
            this.badgeBg = badgeBg;
            this.badgeFg = badgeFg;
            this.summary = summary;
            this.modifiers = modifiers != null ? modifiers : KeyModifiers.NONE;
            this.detail = detail != null ? detail : "";
        }

        public long seq() {
            return seq;
        }

        public String badge() {
            return badge;
        }

        public Color badgeBg() {
            return badgeBg;
        }

        public Color badgeFg() {
            return badgeFg;
        }

        public String summary() {
            return summary;
        }

        public KeyModifiers modifiers() {
            return modifiers;
        }

        public String detail() {
            return detail;
        }
    }
}
