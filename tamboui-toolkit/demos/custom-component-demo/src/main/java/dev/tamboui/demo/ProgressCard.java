/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import dev.tamboui.style.Color;
import dev.tamboui.toolkit.component.Component;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.annotations.bindings.OnAction;
import dev.tamboui.tui.event.Event;

import static dev.tamboui.toolkit.Toolkit.*;

/**
 * A custom component demonstrating @OnAction annotations for keyboard handling.
 * Displays a task card with title, description, and progress bar.
 * <p>
 * Uses the Component base class which automatically:
 * <ul>
 *   <li>Registers @OnAction annotated methods for event handling</li>
 *   <li>Manages focus state via isFocused()</li>
 * </ul>
 */
public final class ProgressCard extends Component<ProgressCard> {

    /**
     * Status of the task shown in the card.
     */
    public enum Status {
        PENDING("pending"),
        IN_PROGRESS("in-progress"),
        COMPLETE("complete");

        private final String cssClass;

        Status(String cssClass) {
            this.cssClass = cssClass;
        }

        public String cssClass() {
            return cssClass;
        }
    }

    private String title = "";
    private String description = "";
    private double progress = 0.0;
    private Status status = Status.PENDING;

    /**
     * Creates a new ProgressCard.
     */
    public ProgressCard() {
    }

    /**
     * Sets the card title.
     */
    public ProgressCard title(String title) {
        this.title = title != null ? title : "";
        return this;
    }

    /**
     * Sets the card description.
     */
    public ProgressCard description(String description) {
        this.description = description != null ? description : "";
        return this;
    }

    /**
     * Sets the progress value (0.0 to 1.0).
     */
    public ProgressCard progress(double progress) {
        this.progress = Math.max(0.0, Math.min(1.0, progress));
        updateStatus();
        return this;
    }

    /**
     * Returns the current progress value.
     */
    public double progress() {
        return progress;
    }

    /**
     * Returns the current status.
     */
    public Status status() {
        return status;
    }

    // ═══════════════════════════════════════════════════════════════
    // @OnAction handlers - automatically registered by Component
    // ═══════════════════════════════════════════════════════════════

    @OnAction("increment")
    void onIncrement(Event event) {
        progress(Math.min(1.0, progress + 0.1));
    }

    @OnAction("decrement")
    void onDecrement(Event event) {
        progress(Math.max(0.0, progress - 0.1));
    }

    // ═══════════════════════════════════════════════════════════════
    // Rendering
    // ═══════════════════════════════════════════════════════════════

    @Override
    protected Element render() {
        var focused = isFocused();

        // Use double border + cyan when focused for clear visual feedback
        var panel = panel(() -> column(
                // Title with CSS class
                text(title).bold().addClass("card-title"),
                // Description with CSS class
                text(description).addClass("card-description"),
                // Progress bar with status-based CSS class
                gauge(progress)
                        .label(String.format("%.0f%%", progress * 100))
                        .addClass("progress-" + status.cssClass())
        ))
                .title(status.name())
                .addClass(status.cssClass());

        if (focused) {
            // Clear focus indicator: double border + cyan
            panel.doubleBorder().borderColor(Color.CYAN);
        } else {
            panel.rounded();
        }

        return panel.fill();
    }

    private void updateStatus() {
        // Use epsilon comparison to handle floating point precision
        // (0.1 + 0.1 + ... 10 times = 0.9999999999999999, not 1.0)
        if (this.progress >= 0.999) {
            this.status = Status.COMPLETE;
        } else if (this.progress > 0.001) {
            this.status = Status.IN_PROGRESS;
        } else {
            this.status = Status.PENDING;
        }
    }
}
