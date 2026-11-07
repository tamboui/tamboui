//DEPS dev.tamboui:tamboui-toolkit:LATEST
//DEPS dev.tamboui:tamboui-jline3-backend:LATEST
/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import java.util.ArrayDeque;
import java.util.Deque;

import dev.tamboui.layout.Constraint;
import dev.tamboui.style.Color;
import dev.tamboui.toolkit.app.ToolkitApp;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.event.DragHandler;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.tui.event.MouseButton;
import dev.tamboui.tui.event.MouseEvent;

import static dev.tamboui.toolkit.Toolkit.*;

/**
 * Demonstrates toolkit mouse routing for every interaction the {@code EventRouter} delivers:
 * button presses (left/middle/right), scroll wheel, pointer motion, and drag.
 * <p>
 * The <b>Click / Scroll</b> panel uses {@code onMouseEvent()} and reports presses, scroll, and the
 * live pointer position. The <b>Drag</b> panel uses {@code onDrag()} and reports drag start, the
 * running delta, and release.
 * <p>
 * Right and middle presses reach the handler thanks to #621/#622; before that fix only left did.
 * On a Mac trackpad: left = tap, right = two-finger tap, scroll = two-finger slide, drag = press
 * and move. Middle usually needs a three-button mouse or a helper such as MiddleClick.
 */
public class MouseDemo extends ToolkitApp {

    private static final int LOG_SIZE = 10;

    private final Deque<String> log = new ArrayDeque<>();
    private int leftCount;
    private int middleCount;
    private int rightCount;
    private int scrollUpCount;
    private int scrollDownCount;
    private int pointerX = -1;
    private int pointerY = -1;
    private boolean dragging;
    private int dragDx;
    private int dragDy;
    private MouseButton lastButton = MouseButton.NONE;

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
        return TuiConfig.builder().mouseCapture(true).mouseMotion(true).build();
    }

    @Override
    protected Element render() {
        return column(
            panel(text(" Mouse Demo - press, scroll, move and drag ").bold().cyan())
                .rounded().borderColor(Color.CYAN).length(3),

            row(
                counterTile("LEFT", leftCount, Color.GREEN, lastButton == MouseButton.LEFT),
                counterTile("MIDDLE", middleCount, Color.YELLOW, lastButton == MouseButton.MIDDLE),
                counterTile("RIGHT", rightCount, Color.MAGENTA, lastButton == MouseButton.RIGHT),
                counterTile("SCROLL", scrollUpCount + scrollDownCount, Color.CYAN, false)
            ).length(5),

            row(
                panel(
                    column(
                        text(""),
                        text("Click (any button) or scroll here").bold().centered(),
                        text(pointer()).dim().centered(),
                        text("scroll  up " + scrollUpCount + "  /  down " + scrollDownCount).dim().centered()
                    )
                )
                .title("Click / Scroll")
                .rounded()
                .borderColor(colorFor(lastButton))
                .id("click-target")
                .focusable()
                .onMouseEvent(this::handleMouse)
                .constraint(Constraint.percentage(50)),

                panel(
                    column(
                        text(""),
                        text(dragging ? "Dragging..." : "Press and drag here").bold().centered(),
                        text("delta (" + dragDx + ", " + dragDy + ")").dim().centered()
                    )
                )
                .title("Drag")
                .rounded()
                .borderColor(dragging ? Color.BLUE : Color.DARK_GRAY)
                .id("drag-target")
                .onDrag(new DragReadout())
                .constraint(Constraint.percentage(50))
            ).length(6),

            panel(eventLog())
                .title("Recent events")
                .rounded()
                .borderColor(Color.DARK_GRAY)
                .fill(),

            panel(text(" Click / scroll / drag the panels | q: Quit ").dim())
                .rounded().borderColor(Color.DARK_GRAY).length(3)
        );
    }

    private String pointer() {
        return pointerX < 0 ? "pointer: (move over me)" : "pointer @ (" + pointerX + ", " + pointerY + ")";
    }

    private Element counterTile(String name, int count, Color color, boolean active) {
        return panel(
            column(
                text(name).bold().fg(active ? Color.BLACK : color).centered(),
                text(Integer.toString(count)).bold().centered()
            )
        )
        .rounded()
        .borderColor(color)
        .constraint(Constraint.percentage(25));
    }

    private Element eventLog() {
        if (log.isEmpty()) {
            return text("No events yet - press, scroll, or drag a panel above.").dim();
        }
        Element[] lines = log.stream().map(line -> (Element) text(line)).toArray(Element[]::new);
        return column(lines);
    }

    private EventResult handleMouse(MouseEvent event) {
        switch (event.kind()) {
            case PRESS:
                return handlePress(event);
            case SCROLL_UP:
                scrollUpCount++;
                record("SCROLL_UP @ (" + event.x() + ", " + event.y() + ")");
                return EventResult.HANDLED;
            case SCROLL_DOWN:
                scrollDownCount++;
                record("SCROLL_DOWN @ (" + event.x() + ", " + event.y() + ")");
                return EventResult.HANDLED;
            case MOVE:
                pointerX = event.x();
                pointerY = event.y();
                return EventResult.HANDLED;
            default:
                return EventResult.UNHANDLED;
        }
    }

    private EventResult handlePress(MouseEvent event) {
        MouseButton button = event.button();
        switch (button) {
            case LEFT:
                leftCount++;
                break;
            case MIDDLE:
                middleCount++;
                break;
            case RIGHT:
                rightCount++;
                break;
            default:
                return EventResult.UNHANDLED;
        }
        lastButton = button;
        record(button + " press @ (" + event.x() + ", " + event.y() + ")");
        return EventResult.HANDLED;
    }

    private void record(String line) {
        log.addFirst(line);
        while (log.size() > LOG_SIZE) {
            log.removeLast();
        }
    }

    private Color colorFor(MouseButton button) {
        switch (button) {
            case LEFT:
                return Color.GREEN;
            case MIDDLE:
                return Color.YELLOW;
            case RIGHT:
                return Color.MAGENTA;
            default:
                return Color.WHITE;
        }
    }

    private final class DragReadout implements DragHandler {
        @Override
        public void onDragStart(int startX, int startY) {
            dragging = true;
            dragDx = 0;
            dragDy = 0;
            record("DRAG start @ (" + startX + ", " + startY + ")");
        }

        @Override
        public void onDrag(int currentX, int currentY, int deltaX, int deltaY) {
            dragDx = deltaX;
            dragDy = deltaY;
        }

        @Override
        public void onDragEnd(int endX, int endY) {
            dragging = false;
            record("DRAG end @ (" + endX + ", " + endY + "), delta (" + dragDx + ", " + dragDy + ")");
        }
    }
}
