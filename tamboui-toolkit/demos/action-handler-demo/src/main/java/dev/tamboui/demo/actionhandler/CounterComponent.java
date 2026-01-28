/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo.actionhandler;

import dev.tamboui.style.Color;
import dev.tamboui.toolkit.component.Component;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.tui.bindings.Actions;
import dev.tamboui.annotations.bindings.OnAction;
import dev.tamboui.tui.event.Event;

import java.util.function.Consumer;

import static dev.tamboui.toolkit.Toolkit.*;

/**
 * A counter component demonstrating @OnAction annotations.
 * <p>
 * No manual event wiring needed - the Component base class automatically
 * discovers and registers @OnAction methods.
 */
public class CounterComponent extends Component<CounterComponent> {

    private final String title;
    private final Color color;
    private final Consumer<String> logger;
    private int count = 0;

    /**
     * Creates a counter component.
     * @param title the title
     * @param color the color
     * @param logger the logger consumer
     */
    public CounterComponent(String title, Color color, Consumer<String> logger) {
        this.title = title;
        this.color = color;
        this.logger = logger;
    }

    @OnAction(Actions.MOVE_UP)
    void increment(Event event) {
        count++;
        logger.accept(title + ": Up -> " + count);
    }

    @OnAction(Actions.MOVE_DOWN)
    void decrement(Event event) {
        count--;
        logger.accept(title + ": Down -> " + count);
    }

    @OnAction(Actions.MOVE_LEFT)
    void decrementTen(Event event) {
        count -= 10;
        logger.accept(title + ": Left -> " + count);
    }

    @OnAction(Actions.MOVE_RIGHT)
    void incrementTen(Event event) {
        count += 10;
        logger.accept(title + ": Right -> " + count);
    }

    /**
     * Gets the current count.
     * @return the count
     */
    public int count() {
        return count;
    }

    /**
     * Resets the counter to zero.
     */
    public void reset() {
        count = 0;
    }

    @Override
    protected Element render() {
        // Use isFocused() since the Component owns focus, not the inner panel
        var borderColor = isFocused() ? color : Color.DARK_GRAY;

        return panel(() -> column(
                row(
                        text("Value: "),
                        text(String.valueOf(count))
                                .bold()
                                .fg(count >= 0 ? Color.GREEN : Color.RED)
                ),
                text("(arrows/hjkl when focused)").dim()
        ))
                .title(title)
                .rounded()
                .borderColor(borderColor)
                .fill();
    }
}
