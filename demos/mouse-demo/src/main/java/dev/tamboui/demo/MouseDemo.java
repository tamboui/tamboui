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
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.tui.event.MouseButton;
import dev.tamboui.tui.event.MouseEvent;
import dev.tamboui.tui.event.MouseEventKind;

import static dev.tamboui.toolkit.Toolkit.*;

/**
 * Demonstrates toolkit mouse routing for every button.
 * <p>
 * Each button press on the target panel is routed through the toolkit
 * {@code EventRouter} to the element's {@code onMouseEvent()} handler. Left,
 * middle, and right presses are all delivered (see #621/#622); before that fix
 * only left presses reached element handlers.
 * <p>
 * On a Mac trackpad: left = tap, right = two-finger tap. Middle usually needs a
 * three-button mouse or a helper such as MiddleClick.
 */
public class MouseDemo extends ToolkitApp {

    private static final int LOG_SIZE = 12;

    private final Deque<String> log = new ArrayDeque<>();
    private int leftCount;
    private int middleCount;
    private int rightCount;
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
        return TuiConfig.builder().mouseCapture(true).build();
    }

    @Override
    protected Element render() {
        return column(
            panel(text(" Mouse Demo - click the target with each button ").bold().cyan())
                .rounded().borderColor(Color.CYAN).length(3),

            row(
                counterTile("LEFT", leftCount, Color.GREEN, lastButton == MouseButton.LEFT),
                counterTile("MIDDLE", middleCount, Color.YELLOW, lastButton == MouseButton.MIDDLE),
                counterTile("RIGHT", rightCount, Color.MAGENTA, lastButton == MouseButton.RIGHT)
            ).length(5),

            panel(
                column(
                    text(""),
                    text("   Click here with left, middle, or right   ").bold().centered(),
                    text("   (right = two-finger tap on a trackpad)   ").dim().centered()
                )
            )
            .title("Target")
            .rounded()
            .borderColor(colorFor(lastButton))
            .id("target")
            .focusable()
            .onMouseEvent(this::handleMouse)
            .length(6),

            panel(eventLog())
                .title("Recent presses")
                .rounded()
                .borderColor(Color.DARK_GRAY)
                .fill(),

            panel(text(" Click the target | q: Quit ").dim())
                .rounded().borderColor(Color.DARK_GRAY).length(3)
        );
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
        .constraint(Constraint.percentage(33));
    }

    private Element eventLog() {
        if (log.isEmpty()) {
            return text("No presses yet - click the target above.").dim();
        }
        Element[] lines = log.stream().map(line -> (Element) text(line)).toArray(Element[]::new);
        return column(lines);
    }

    private EventResult handleMouse(MouseEvent event) {
        if (event.kind() != MouseEventKind.PRESS) {
            return EventResult.UNHANDLED;
        }
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
}
