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
import java.util.Locale;
import java.util.Optional;

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

    static final int EVENT_LOG_LIMIT = 12;
    static final int ACTIVITY_PULSE_TICKS = 5;

    private int counter = 0;
    private int keyCount = 0;
    private int mouseCount = 0;
    private int resizeCount = 0;
    private final List<LoggedEvent> eventLog = new ArrayList<>();
    private int mouseX = -1;
    private int mouseY = -1;
    private int terminalWidth = 0;
    private int terminalHeight = 0;
    private long tickCount = 0;
    private KeySnapshot lastKey = KeySnapshot.empty();
    private MouseSnapshot lastMouse = MouseSnapshot.empty();
    private final MouseDeviceState mouseDevice = new MouseDeviceState();

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
        keyCount++;
        lastKey = KeySnapshot.from(k);
        logEvent(LoggedEvent.key(lastKey));
        if (k.isSelect()) {
            counter++;
        }
        return true;
    }

    private boolean handleMouseEvent(MouseEvent m) {
        mouseCount++;
        mouseX = m.x();
        mouseY = m.y();
        lastMouse = MouseSnapshot.from(m);
        mouseDevice.observe(m);
        logEvent(LoggedEvent.mouse(lastMouse));

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
        boolean animated = mouseDevice.tick();
        return animated || tickCount % 5 == 0;
    }

    private boolean handleResizeEvent(ResizeEvent r) {
        resizeCount++;
        terminalWidth = r.width();
        terminalHeight = r.height();
        logEvent(LoggedEvent.resize(r.width(), r.height()));
        return true;
    }

    private void logEvent(LoggedEvent event) {
        eventLog.addFirst(event);
        if (eventLog.size() > EVENT_LOG_LIMIT) {
            eventLog.removeLast();
        }
    }

    private void render(Frame frame) {
        Rect area = frame.area();
        terminalWidth = area.width();
        terminalHeight = area.height();

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
                                Span.raw("Tui Event Inspector ").bold().cyan(),
                                Span.raw(animation + " ").cyan(),
                                Span.raw("keys " + keyCount + " ").yellow(),
                                Span.raw("mouse " + mouseCount + " ").green(),
                                Span.raw("resize " + resizeCount + " ").magenta()
                        )
                ).centered())
                .build();

        frame.renderWidget(header, area);
    }

    private void renderMain(Frame frame, Rect area) {
        if (area.width() >= 96) {
            List<Rect> columns = Layout.horizontal()
                    .constraints(
                            Constraint.length(38),
                            Constraint.fill()
                    )
                    .spacing(1)
                    .split(area);
            List<Rect> left = Layout.vertical()
                    .constraints(
                            Constraint.length(15),
                            Constraint.fill()
                    )
                    .spacing(1)
                    .split(columns.get(0));
            List<Rect> right = Layout.vertical()
                    .constraints(
                            Constraint.length(11),
                            Constraint.fill()
                    )
                    .spacing(1)
                    .split(columns.get(1));
            renderStatePanel(frame, left.get(0));
            renderCapabilitiesPanel(frame, left.get(1));
            renderMousePanel(frame, right.get(0));
            renderEventsPanel(frame, right.get(1));
            return;
        }

        List<Rect> rows = Layout.vertical()
                .constraints(
                        Constraint.length(15),
                        Constraint.length(11),
                        Constraint.length(8),
                        Constraint.fill()
                )
                .spacing(1)
                .split(area);
        renderStatePanel(frame, rows.get(0));
        renderMousePanel(frame, rows.get(1));
        renderCapabilitiesPanel(frame, rows.get(2));
        renderEventsPanel(frame, rows.get(3));
    }

    private void renderStatePanel(Frame frame, Rect area) {
        List<Line> lines = new ArrayList<>();
        lines.add(labeledValue("Counter", String.valueOf(counter), Color.YELLOW));
        lines.add(labeledValue("Ticks", String.valueOf(tickCount), Color.MAGENTA));
        lines.add(labeledValue("Terminal", terminalWidth + " x " + terminalHeight, Color.CYAN));
        lines.add(Line.empty());
        lines.add(labeledValue("Last key", lastKey.logicalKey(), Color.WHITE));
        lines.add(labeledValue("Text", lastKey.producedText(), Color.GREEN));
        lines.add(labeledValue("Action", lastKey.action(), Color.CYAN));
        lines.add(Line.from(modifierSpans(lastKey.modifiers())));
        lines.add(Line.empty());
        lines.add(Line.from(
                Span.raw("Last mouse ").bold(),
                kindBadge(lastMouse.kindLabel(), mouseSnapshotColor(lastMouse)),
                Span.raw(" " + lastMouse.summary()).white()
        ));
        lines.add(Line.from(modifierSpans(lastMouse.modifiers())));

        Paragraph panel = Paragraph.builder()
                .text(Text.from(lines))
                .block(panelBlock("Live State", Color.GREEN))
                .build();

        frame.renderWidget(panel, area);
    }

    private void renderMousePanel(Frame frame, Rect area) {
        List<Line> lines = new ArrayList<>();
        lines.add(Line.from(Span.raw("      ╭──────────────╮").fg(Color.GRAY)));
        lines.add(Line.from(
                Span.raw("      │ ").fg(Color.GRAY),
                buttonSpan("L", mouseDevice.isLeftPressed(), Color.GREEN),
                Span.raw(" ").fg(Color.GRAY),
                buttonSpan("M", mouseDevice.isMiddlePressed(), Color.YELLOW),
                Span.raw(" ").fg(Color.GRAY),
                buttonSpan("R", mouseDevice.isRightPressed(), Color.CYAN),
                Span.raw(" │").fg(Color.GRAY)
        ));
        lines.add(Line.from(
                Span.raw("      │   ").fg(Color.GRAY),
                scrollArrow("◀", mouseDevice.horizontalScrollPulse() < 0),
                Span.raw("   ").fg(Color.GRAY),
                wheelSpan(),
                Span.raw("   ").fg(Color.GRAY),
                scrollArrow("▶", mouseDevice.horizontalScrollPulse() > 0),
                Span.raw("   │").fg(Color.GRAY)
        ));
        lines.add(Line.from(
                Span.raw("      │ hover ").fg(Color.GRAY),
                Span.raw(mouseDevice.hoverPulse() > 0 ? "●" : "○")
                        .fg(mouseDevice.hoverPulse() > 0 ? Color.CYAN : Color.DARK_GRAY),
                Span.raw("  drag ").fg(Color.GRAY),
                Span.raw(mouseDevice.lastKind() == MouseEventKind.DRAG ? "●" : "○")
                        .fg(mouseDevice.lastKind() == MouseEventKind.DRAG ? Color.YELLOW : Color.DARK_GRAY),
                Span.raw(" │").fg(Color.GRAY)
        ));
        lines.add(Line.from(Span.raw("      ╰──────────────╯").fg(Color.GRAY)));
        lines.add(Line.empty());
        lines.add(labeledValue("Pointer", formatCoordinates(mouseX, mouseY), Color.CYAN));
        lines.add(Line.from(
                Span.raw("Event ").bold(),
                kindBadge(lastMouse.kindLabel(), mouseSnapshotColor(lastMouse)),
                Span.raw("  "),
                Span.raw(lastMouse.buttonLabel()).fg(Color.WHITE)
        ));

        Paragraph panel = Paragraph.builder()
                .text(Text.from(lines))
                .block(panelBlock("Mouse Device", Color.CYAN))
                .build();

        frame.renderWidget(panel, area);
    }

    private void renderCapabilitiesPanel(Frame frame, Rect area) {
        Text content = Text.from(
                labeledValue("Mouse capture", "ON", Color.GREEN),
                labeledValue("Motion 1003", "ON", Color.GREEN),
                labeledValue("Mouse SGR", "CSI ?1006", Color.CYAN),
                labeledValue("Keyboard", "Legacy + bindings", Color.YELLOW),
                labeledValue("Modifier slots", "Ctrl Alt Shift + Super Hyper Meta", Color.WHITE),
                labeledValue("TERM", envValue("TERM"), Color.CYAN),
                labeledValue("TERM_PROGRAM", envValue("TERM_PROGRAM"), Color.CYAN)
        );
        Paragraph panel = Paragraph.builder()
                .text(content)
                .block(panelBlock("Capabilities", Color.MAGENTA))
                .build();

        frame.renderWidget(panel, area);
    }

    private void renderEventsPanel(Frame frame, Rect area) {
        List<Line> lines = new ArrayList<>();
        if (eventLog.isEmpty()) {
            lines.add(Line.from(Span.raw("Press keys, click, drag, or scroll to populate the log.").dim()));
        } else {
            for (int i = 0; i < eventLog.size(); i++) {
                lines.add(eventLog.get(i).toLine(i == 0));
            }
        }

        Paragraph panel = Paragraph.builder()
                .text(Text.from(lines))
                .block(panelBlock("Last " + EVENT_LOG_LIMIT + " Events", Color.YELLOW))
                .build();

        frame.renderWidget(panel, area);
    }

    private void renderFooter(Frame frame, Rect area) {
        Line helpLine = Line.from(
                Span.raw("Keys").bold().yellow(),
                Span.raw(" logical + text + modifiers  ").dim(),
                Span.raw("Mouse").bold().yellow(),
                Span.raw(" buttons + hover + scroll  ").dim(),
                Span.raw("Enter/Space").bold().yellow(),
                Span.raw(" Counter  ").dim(),
                Span.raw("Scroll").bold().yellow(),
                Span.raw(" Wheel demo  ").dim(),
                Span.raw("F12").bold().yellow(),
                Span.raw(" Debug  ").dim(),
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

    static List<ModifierFlag> modifierFlags(KeyModifiers modifiers) {
        return List.of(
                new ModifierFlag("Ctrl", modifiers.ctrl(), true),
                new ModifierFlag("Alt", modifiers.alt(), true),
                new ModifierFlag("Shift", modifiers.shift(), true),
                new ModifierFlag("Super", false, false),
                new ModifierFlag("Hyper", false, false),
                new ModifierFlag("Meta", false, false)
        );
    }

    private static Line labeledValue(String label, String value, Color valueColor) {
        return Line.from(
                Span.raw(label + ": ").bold(),
                Span.raw(value).fg(valueColor)
        );
    }

    private static List<Span> modifierSpans(KeyModifiers modifiers) {
        List<Span> spans = new ArrayList<>();
        for (ModifierFlag flag : modifierFlags(modifiers)) {
            if (!spans.isEmpty()) {
                spans.add(Span.raw(" "));
            }
            spans.add(flag.toSpan());
        }
        return spans;
    }

    private static Span buttonSpan(String label, boolean active, Color activeColor) {
        return Span.raw("[" + label + "]")
                .fg(active ? Color.BLACK : Color.GRAY)
                .bg(active ? activeColor : Color.DARK_GRAY)
                .bold();
    }

    private Span wheelSpan() {
        if (mouseDevice.verticalScrollPulse() > 0) {
            return Span.raw("↑").fg(Color.MAGENTA).bold();
        }
        if (mouseDevice.verticalScrollPulse() < 0) {
            return Span.raw("↓").fg(Color.BLUE).bold();
        }
        return Span.raw("●").fg(Color.GRAY);
    }

    private static Span scrollArrow(String symbol, boolean active) {
        return Span.raw(symbol)
                .fg(active ? Color.YELLOW : Color.DARK_GRAY)
                .bold();
    }

    private static Block panelBlock(String title, Color borderColor) {
        return Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(borderColor))
                .title(Title.from(Line.from(Span.raw(" " + title + " ").fg(borderColor).bold())))
                .build();
    }

    private static Span kindBadge(String label, Color color) {
        return Span.raw(" " + label + " ")
                .fg(Color.BLACK)
                .bg(color)
                .bold();
    }

    private static Color kindColor(MouseEventKind kind) {
        return switch (kind) {
            case PRESS -> Color.GREEN;
            case RELEASE -> Color.GRAY;
            case DRAG -> Color.YELLOW;
            case MOVE -> Color.CYAN;
            case SCROLL_UP -> Color.MAGENTA;
            case SCROLL_DOWN -> Color.BLUE;
            case SCROLL_LEFT, SCROLL_RIGHT -> Color.WHITE;
        };
    }

    private static Color mouseSnapshotColor(MouseSnapshot snapshot) {
        return snapshot.present() ? kindColor(snapshot.kind()) : Color.GRAY;
    }

    private static String envValue(String key) {
        return Optional.ofNullable(System.getenv(key)).filter(value -> !value.isBlank()).orElse("<unset>");
    }

    private static String humanizeEnum(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    private static String formatCoordinates(int x, int y) {
        return x >= 0 ? "(" + x + ", " + y + ")" : "awaiting input";
    }

    static final class ModifierFlag {
        private final String label;
        private final boolean active;
        private final boolean available;

        ModifierFlag(String label, boolean active, boolean available) {
            this.label = label;
            this.active = active;
            this.available = available;
        }

        String label() {
            return label;
        }

        boolean active() {
            return active;
        }

        boolean available() {
            return available;
        }

        Span toSpan() {
            Color foreground = active ? Color.BLACK : available ? Color.GRAY : Color.DARK_GRAY;
            Color background = active ? Color.GREEN : Color.RESET;
            Span span = Span.raw(label)
                    .fg(foreground)
                    .bg(background);
            if (!available) {
                span = span.dim();
            }
            return span;
        }
    }

    static final class KeySnapshot {
        private final boolean present;
        private final String logicalKey;
        private final String producedText;
        private final String action;
        private final KeyModifiers modifiers;

        KeySnapshot(boolean present, String logicalKey, String producedText, String action, KeyModifiers modifiers) {
            this.present = present;
            this.logicalKey = logicalKey;
            this.producedText = producedText;
            this.action = action;
            this.modifiers = modifiers;
        }

        static KeySnapshot empty() {
            return new KeySnapshot(false, "—", "—", "—", KeyModifiers.NONE);
        }

        static KeySnapshot from(KeyEvent event) {
            String producedText = event.code() == KeyCode.CHAR ? event.string() : "∅";
            String action = event.action().map(TuiDemo::humanizeAction).orElse("—");
            return new KeySnapshot(true, logicalKey(event), producedText, action, event.modifiers());
        }

        private static String logicalKey(KeyEvent event) {
            if (event.code() == KeyCode.CHAR) {
                return "CHAR";
            }
            return event.code().name();
        }

        boolean present() {
            return present;
        }

        String logicalKey() {
            return logicalKey;
        }

        String producedText() {
            return producedText;
        }

        String action() {
            return action;
        }

        KeyModifiers modifiers() {
            return modifiers;
        }
    }

    static final class MouseSnapshot {
        private final boolean present;
        private final MouseEventKind kind;
        private final MouseButton button;
        private final int x;
        private final int y;
        private final KeyModifiers modifiers;

        MouseSnapshot(boolean present, MouseEventKind kind, MouseButton button, int x, int y, KeyModifiers modifiers) {
            this.present = present;
            this.kind = kind;
            this.button = button;
            this.x = x;
            this.y = y;
            this.modifiers = modifiers;
        }

        static MouseSnapshot empty() {
            return new MouseSnapshot(false, MouseEventKind.MOVE, MouseButton.NONE, -1, -1, KeyModifiers.NONE);
        }

        static MouseSnapshot from(MouseEvent event) {
            return new MouseSnapshot(true, event.kind(), event.button(), event.x(), event.y(), event.modifiers());
        }

        boolean present() {
            return present;
        }

        MouseEventKind kind() {
            return kind;
        }

        String kindLabel() {
            return present ? kind.name() : "IDLE";
        }

        String buttonLabel() {
            if (!present) {
                return "awaiting input";
            }
            return button == MouseButton.NONE ? "button none" : humanizeEnum(button);
        }

        String summary() {
            if (!present) {
                return "awaiting input";
            }
            return buttonLabel() + " @ " + formatCoordinates(x, y);
        }

        KeyModifiers modifiers() {
            return modifiers;
        }
    }

    static final class LoggedEvent {
        private final String badge;
        private final Color badgeColor;
        private final String primary;
        private final String detail;

        LoggedEvent(String badge, Color badgeColor, String primary, String detail) {
            this.badge = badge;
            this.badgeColor = badgeColor;
            this.primary = primary;
            this.detail = detail;
        }

        static LoggedEvent key(KeySnapshot key) {
            return new LoggedEvent(
                    "KEY",
                    Color.YELLOW,
                    key.logicalKey() + " text=" + key.producedText(),
                    "mods=" + humanizeModifiers(key.modifiers()) + " action=" + key.action()
            );
        }

        static LoggedEvent mouse(MouseSnapshot mouse) {
            return new LoggedEvent(
                    mouse.kindLabel(),
                    kindColor(mouse.kind()),
                    mouse.buttonLabel() + " @ " + formatCoordinates(mouse.x, mouse.y),
                    "mods=" + humanizeModifiers(mouse.modifiers())
            );
        }

        static LoggedEvent resize(int width, int height) {
            return new LoggedEvent("RESIZE", Color.MAGENTA, width + " x " + height, "layout updated");
        }

        Line toLine(boolean latest) {
            List<Span> spans = new ArrayList<>();
            spans.add(kindBadge(badge, badgeColor));
            spans.add(Span.raw(" " + primary + " ").fg(latest ? Color.WHITE : Color.GRAY));
            spans.add(Span.raw(detail).fg(latest ? Color.CYAN : Color.DARK_GRAY));
            return Line.from(spans);
        }
    }

    static final class MouseDeviceState {
        private boolean leftPressed;
        private boolean middlePressed;
        private boolean rightPressed;
        private int verticalScrollPulse;
        private int horizontalScrollPulse;
        private int hoverPulse;
        private MouseEventKind lastKind = MouseEventKind.MOVE;

        void observe(MouseEvent event) {
            lastKind = event.kind();
            switch (event.kind()) {
                case PRESS:
                    setPressed(event.button(), true);
                    break;
                case DRAG:
                    hoverPulse = ACTIVITY_PULSE_TICKS;
                    setPressed(event.button(), true);
                    break;
                case RELEASE:
                    clearButtons();
                    break;
                case MOVE:
                    hoverPulse = ACTIVITY_PULSE_TICKS;
                    break;
                case SCROLL_UP:
                    verticalScrollPulse = ACTIVITY_PULSE_TICKS;
                    break;
                case SCROLL_DOWN:
                    verticalScrollPulse = -ACTIVITY_PULSE_TICKS;
                    break;
                case SCROLL_LEFT:
                    horizontalScrollPulse = -ACTIVITY_PULSE_TICKS;
                    break;
                case SCROLL_RIGHT:
                    horizontalScrollPulse = ACTIVITY_PULSE_TICKS;
                    break;
            }
        }

        boolean tick() {
            boolean animated = verticalScrollPulse != 0 || horizontalScrollPulse != 0 || hoverPulse > 0;
            verticalScrollPulse = decay(verticalScrollPulse);
            horizontalScrollPulse = decay(horizontalScrollPulse);
            if (hoverPulse > 0) {
                hoverPulse--;
            }
            return animated;
        }

        boolean isLeftPressed() {
            return leftPressed;
        }

        boolean isMiddlePressed() {
            return middlePressed;
        }

        boolean isRightPressed() {
            return rightPressed;
        }

        int verticalScrollPulse() {
            return verticalScrollPulse;
        }

        int horizontalScrollPulse() {
            return horizontalScrollPulse;
        }

        int hoverPulse() {
            return hoverPulse;
        }

        MouseEventKind lastKind() {
            return lastKind;
        }

        private void setPressed(MouseButton button, boolean pressed) {
            switch (button) {
                case LEFT:
                    leftPressed = pressed;
                    break;
                case MIDDLE:
                    middlePressed = pressed;
                    break;
                case RIGHT:
                    rightPressed = pressed;
                    break;
                case NONE:
                    break;
            }
        }

        private void clearButtons() {
            leftPressed = false;
            middlePressed = false;
            rightPressed = false;
        }

        private static int decay(int pulse) {
            if (pulse > 0) {
                return pulse - 1;
            }
            if (pulse < 0) {
                return pulse + 1;
            }
            return 0;
        }
    }

    private static String humanizeAction(String action) {
        int dot = action.lastIndexOf('.');
        if (dot >= 0 && dot + 1 < action.length()) {
            return action.substring(dot + 1);
        }
        return action;
    }

    private static String humanizeModifiers(KeyModifiers modifiers) {
        StringBuilder summary = new StringBuilder();
        for (ModifierFlag flag : modifierFlags(modifiers)) {
            if (flag.active()) {
                if (summary.length() > 0) {
                    summary.append('+');
                }
                summary.append(flag.label());
            }
        }
        return summary.length() == 0 ? "none" : summary.toString();
    }
}
