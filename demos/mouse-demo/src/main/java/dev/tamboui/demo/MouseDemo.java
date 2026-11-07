//DEPS dev.tamboui:tamboui-toolkit:LATEST
//DEPS dev.tamboui:tamboui-jline3-backend:LATEST
/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.toolkit.app.ToolkitApp;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.event.DragHandler;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.RenderClock;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.MouseButton;
import dev.tamboui.tui.event.MouseEvent;
import dev.tamboui.widget.Widget;

import static dev.tamboui.toolkit.Toolkit.*;

/**
 * Demonstrates toolkit mouse routing. Drag on the canvas to draw; each button and the scroll wheel
 * light their tile, whose background glows by activity and fades back to black when you stop.
 * <p>
 * Right and middle presses reach the handler thanks to #621/#622; before that fix only left did.
 * On a Mac trackpad: left = tap, right = two-finger tap, scroll = two-finger slide, draw = drag.
 * Middle usually needs a three-button mouse or a helper such as MiddleClick. Press {@code c} to
 * clear the canvas.
 */
public class MouseDemo extends ToolkitApp {

    private static final double FADE_SECONDS = 1.2;

    private final RenderClock clock = new RenderClock();
    private final PaintWidget canvas = new PaintWidget();

    private final Tile left = new Tile("LEFT", 0, 255, 0);
    private final Tile middle = new Tile("MIDDLE", 255, 255, 0);
    private final Tile right = new Tile("RIGHT", 255, 0, 255);
    private final Tile scroll = new Tile("SCROLL", 0, 200, 255);

    // Ink the next stroke uses: the colour of the last thing clicked/scrolled before the drag
    // (defaults to the LEFT colour).
    private int inkR = 0;
    private int inkG = 255;
    private int inkB = 0;

    // Starts in the hands-free attract loop so a recording with no input still shows everything;
    // the first real mouse event hands control to the user, and 'a' re-arms it.
    private boolean auto = true;
    private double autoTime;
    private double autoLastSwitch = -1.0;
    private int autoColor;
    private int autoPrevX = Integer.MIN_VALUE;
    private int autoPrevY = Integer.MIN_VALUE;

    /**
     * Demo entry point.
     *
     * @param args the CLI arguments
     * @throws Exception on unexpected error
     */
    public static void main(String[] args) throws Exception {
        new MouseDemo().run();
    }

    @Override
    protected TuiConfig configure() {
        return TuiConfig.builder()
            .mouseCapture(true)
            .mouseMotion(true)
            .tickRate(Duration.ofMillis(50))
            .build();
    }

    @Override
    protected Element render() {
        double dt = clock.tick().toNanos() / 1_000_000_000.0;
        left.decay(dt);
        middle.decay(dt);
        right.decay(dt);
        scroll.decay(dt);
        canvas.decay(dt);
        if (auto) {
            drive(dt);
        }

        String mode = auto ? "AUTO (move mouse to take over)" : "manual (a: auto)";
        return column(
            panel(text(" Mouse Demo - drag to draw; buttons & scroll glow and fade - " + mode + " ")
                .bold().fg(auto ? Color.CYAN : Color.WHITE))
                .rounded().borderColor(auto ? Color.CYAN : Color.DARK_GRAY).length(3),

            row(left.render(), middle.render(), right.render(), scroll.render()).length(5),

            panel(widget(canvas))
                .title("Canvas - drag to draw  (c: clear)")
                .rounded()
                .borderColor(canvas.empty() ? Color.DARK_GRAY : Color.CYAN)
                .id("canvas")
                .focusable()
                .onKeyEvent(this::handleKey)
                .onMouseEvent(this::handleMouse)
                .onDrag(new PaintDrag())
                .fill(),

            panel(text(" Drag: draw | Click/scroll: glow a tile | a: auto | c: clear | q: Quit ").dim())
                .rounded().borderColor(Color.DARK_GRAY).length(3)
        );
    }

    private void stopAuto() {
        auto = false;
        autoPrevX = Integer.MIN_VALUE;
        autoPrevY = Integer.MIN_VALUE;
    }

    private void startAuto() {
        auto = true;
        autoLastSwitch = -1.0;
        autoPrevX = Integer.MIN_VALUE;
        autoPrevY = Integer.MIN_VALUE;
    }

    /**
     * Hands-free attract loop for the demo reel: cycles the ink colour (flashing the matching tile,
     * as if it were clicked) and traces a Lissajous curve across the canvas so the strokes draw and
     * fade on their own.
     */
    private void drive(double dt) {
        autoTime += dt;
        if (autoLastSwitch < 0 || autoTime - autoLastSwitch > 1.6) {
            autoLastSwitch = autoTime;
            autoColor = (autoColor + 1) % 4;
            switch (autoColor) {
                case 0:
                    left.flash();
                    setInk(0, 255, 0);
                    break;
                case 1:
                    middle.flash();
                    setInk(255, 255, 0);
                    break;
                case 2:
                    right.flash();
                    setInk(255, 0, 255);
                    break;
                default:
                    scroll.flash();
                    setInk(0, 200, 255);
                    break;
            }
        }
        Rect a = canvas.area();
        if (a == null || a.width() < 4 || a.height() < 3) {
            return;
        }
        int cx = a.left() + a.width() / 2;
        int cy = a.top() + a.height() / 2;
        int rx = a.width() / 2 - 2;
        int ry = a.height() / 2 - 1;
        double t = autoTime * 0.8;
        int x = cx + (int) Math.round(rx * Math.sin(3 * t));
        int y = cy + (int) Math.round(ry * Math.sin(2 * t + Math.PI / 2));
        if (autoPrevX != Integer.MIN_VALUE) {
            canvas.paintLineAbsolute(autoPrevX, autoPrevY, x, y, inkR, inkG, inkB);
        } else {
            canvas.paintAbsolute(x, y, inkR, inkG, inkB);
        }
        autoPrevX = x;
        autoPrevY = y;
    }

    private EventResult handleKey(KeyEvent event) {
        if (event.isChar('c')) {
            canvas.clear();
            return EventResult.HANDLED;
        }
        if (event.isChar('a')) {
            if (auto) {
                stopAuto();
            } else {
                startAuto();
            }
            return EventResult.HANDLED;
        }
        return EventResult.UNHANDLED;
    }

    private EventResult handleMouse(MouseEvent event) {
        // Any real mouse event hands control from the attract loop back to the user.
        if (auto) {
            stopAuto();
        }
        switch (event.kind()) {
            case PRESS:
                if (event.button() == MouseButton.MIDDLE) {
                    middle.flash();
                    setInk(255, 255, 0);
                    return EventResult.HANDLED;
                }
                if (event.button() == MouseButton.RIGHT) {
                    right.flash();
                    setInk(255, 0, 255);
                    return EventResult.HANDLED;
                }
                return EventResult.UNHANDLED;
            case SCROLL_UP:
            case SCROLL_DOWN:
                scroll.flash();
                setInk(0, 200, 255);
                return EventResult.HANDLED;
            default:
                return EventResult.UNHANDLED;
        }
    }

    private void setInk(int r, int g, int b) {
        inkR = r;
        inkG = g;
        inkB = b;
    }

    /**
     * A button tile whose background glows on activity and fades to black over {@link #FADE_SECONDS}.
     */
    private static final class Tile {
        private final String name;
        private final int r;
        private final int g;
        private final int b;
        private int count;
        private double intensity;

        Tile(String name, int r, int g, int b) {
            this.name = name;
            this.r = r;
            this.g = g;
            this.b = b;
        }

        void flash() {
            intensity = 1.0;
            count++;
        }

        void decay(double seconds) {
            intensity = Math.max(0.0, intensity - seconds / FADE_SECONDS);
        }

        Element render() {
            Color bg = Color.rgb((int) (r * intensity), (int) (g * intensity), (int) (b * intensity));
            Color fg = intensity > 0.5 ? Color.BLACK : Color.rgb(r, g, b);
            return panel(
                column(
                    text(name).bold().fg(fg).centered(),
                    text(Integer.toString(count)).bold().fg(fg).centered()
                )
            )
            .bg(bg)
            .rounded()
            .borderColor(Color.rgb(r, g, b))
            .constraint(Constraint.percentage(25));
        }
    }

    /**
     * A widget that paints the cells it is told to, recording its current area so drag coordinates
     * (which are absolute) can be clipped to the drawing surface. Each painted cell glows at full
     * intensity and fades to black over {@link #DRAW_FADE_SECONDS}.
     */
    private static final class PaintWidget implements Widget {
        private static final double DRAW_FADE_SECONDS = 3.0;

        private final Map<Long, Ink> cells = new ConcurrentHashMap<>();
        private Rect area;

        @Override
        public void render(Rect renderArea, Buffer buffer) {
            this.area = renderArea;
            for (Map.Entry<Long, Ink> entry : cells.entrySet()) {
                long key = entry.getKey();
                Ink ink = entry.getValue();
                int x = (int) (key >> 32);
                int y = (int) (key & 0xFFFFFFFFL);
                if (renderArea.contains(x, y)) {
                    Color color = Color.rgb(
                        (int) (ink.r * ink.intensity),
                        (int) (ink.g * ink.intensity),
                        (int) (ink.b * ink.intensity));
                    buffer.setString(x, y, "\u2588", Style.EMPTY.fg(color));
                }
            }
        }

        void decay(double seconds) {
            double step = seconds / DRAW_FADE_SECONDS;
            Iterator<Map.Entry<Long, Ink>> it = cells.entrySet().iterator();
            while (it.hasNext()) {
                Ink ink = it.next().getValue();
                ink.intensity -= step;
                if (ink.intensity <= 0.0) {
                    it.remove();
                }
            }
        }

        void paintAbsolute(int x, int y, int r, int g, int b) {
            if (area != null && area.contains(x, y)) {
                cells.put(((long) x << 32) | (y & 0xFFFFFFFFL), new Ink(r, g, b));
            }
        }

        /** Paints a straight line between two absolute points so a fast drag draws a continuous stroke. */
        void paintLineAbsolute(int x0, int y0, int x1, int y1, int r, int g, int b) {
            int dx = Math.abs(x1 - x0);
            int dy = -Math.abs(y1 - y0);
            int sx = x0 < x1 ? 1 : -1;
            int sy = y0 < y1 ? 1 : -1;
            int err = dx + dy;
            int x = x0;
            int y = y0;
            while (true) {
                paintAbsolute(x, y, r, g, b);
                if (x == x1 && y == y1) {
                    break;
                }
                int e2 = 2 * err;
                if (e2 >= dy) {
                    err += dy;
                    x += sx;
                }
                if (e2 <= dx) {
                    err += dx;
                    y += sy;
                }
            }
        }

        /** A painted cell: its base colour and its current fade intensity. */
        private static final class Ink {
            private final int r;
            private final int g;
            private final int b;
            private double intensity = 1.0;

            Ink(int r, int g, int b) {
                this.r = r;
                this.g = g;
                this.b = b;
            }
        }

        void clear() {
            cells.clear();
        }

        boolean empty() {
            return cells.isEmpty();
        }

        Rect area() {
            return area;
        }
    }

    private final class PaintDrag implements DragHandler {
        private int lastX;
        private int lastY;

        @Override
        public void onDragStart(int startX, int startY) {
            if (auto) {
                stopAuto();
            }
            left.flash();
            lastX = startX;
            lastY = startY;
            canvas.paintAbsolute(startX, startY, inkR, inkG, inkB);
        }

        @Override
        public void onDrag(int currentX, int currentY, int deltaX, int deltaY) {
            canvas.paintLineAbsolute(lastX, lastY, currentX, currentY, inkR, inkG, inkB);
            lastX = currentX;
            lastY = currentY;
        }

        @Override
        public void onDragEnd(int endX, int endY) {
            canvas.paintLineAbsolute(lastX, lastY, endX, endY, inkR, inkG, inkB);
        }
    }
}
