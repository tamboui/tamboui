/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.toast;

import java.time.Duration;
import java.util.Objects;

import dev.tamboui.style.Color;

/**
 * Immutable description of a toast notification to display.
 * <p>
 * A toast is either sticky (manual dismiss, no lifetime) or timed (positive {@link Duration} lifetime).
 * Build instances via {@link ToastBuilder}.
 */
public final class Toast {

    private final ToastType type;
    private final String title;
    private final String message;
    private final boolean sticky;
    private final Duration lifetime;
    private final Color backgroundColor;
    private final TitleLayout titleLayout;
    private final TitleSeparator titleSeparator;
    private final TitleAlignment titleAlignment;
    private final boolean highlightTitle;
    private final ProgressStyle progressStyle;
    private final String deduplicationKey;

    Toast(ToastBuilder builder) {
        Objects.requireNonNull(builder.type, "type");
        Objects.requireNonNull(builder.message, "message");

        if (builder.sticky && builder.lifetime != null) {
            throw new IllegalArgumentException("sticky toast cannot have a lifetime");
        }

        this.type = builder.type;
        this.title = builder.title;
        this.message = builder.message;
        this.sticky = builder.sticky;
        if (builder.sticky) {
            this.lifetime = null;
        } else {
            Duration resolved = builder.lifetime != null ? builder.lifetime : ToastBuilder.DEFAULT_DURATION;
            if (resolved.isZero() || resolved.isNegative()) {
                throw new IllegalArgumentException("lifetime must be positive");
            }
            this.lifetime = resolved;
        }
        this.backgroundColor = builder.backgroundColor;
        this.titleLayout = builder.titleLayout;
        this.titleSeparator = builder.titleSeparator;
        this.titleAlignment = builder.titleAlignment;
        this.highlightTitle = builder.highlightTitle;
        this.progressStyle = builder.progressStyle;
        this.deduplicationKey = builder.deduplicationKey;
    }

    /**
     * Returns the semantic toast type.
     *
     * @return the toast type
     */
    public ToastType type() {
        return type;
    }

    /**
     * Returns the optional title, or {@code null} when absent.
     *
     * @return the title, or {@code null}
     */
    public String title() {
        return title;
    }

    /**
     * Returns the toast message body.
     *
     * @return the message
     */
    public String message() {
        return message;
    }

    /**
     * Returns whether this toast stays until manually dismissed.
     *
     * @return {@code true} when sticky
     */
    public boolean sticky() {
        return sticky;
    }

    /**
     * Returns the auto-dismiss lifetime for timed toasts.
     *
     * @return the lifetime, or {@code null} for sticky toasts
     */
    public Duration lifetime() {
        return lifetime;
    }

    /**
     * Returns the optional background color override, or {@code null} to use defaults.
     *
     * @return the background color, or {@code null}
     */
    public Color backgroundColor() {
        return backgroundColor;
    }

    /**
     * Returns the title row layout mode.
     *
     * @return the title layout
     */
    public TitleLayout titleLayout() {
        return titleLayout;
    }

    /**
     * Returns the separator rendered between title segments.
     *
     * @return the title separator
     */
    public TitleSeparator titleSeparator() {
        return titleSeparator;
    }

    /**
     * Returns the horizontal alignment of the title row.
     *
     * @return the title alignment
     */
    public TitleAlignment titleAlignment() {
        return titleAlignment;
    }

    /**
     * Returns whether the title band uses the type accent as a highlighted background.
     *
     * @return {@code true} when the title is highlighted
     */
    public boolean highlightTitle() {
        return highlightTitle;
    }

    /**
     * Returns the visual style for the auto-dismiss progress indicator.
     *
     * @return the progress style
     */
    public ProgressStyle progressStyle() {
        return progressStyle;
    }

    /**
     * Returns the deduplication key, or {@code null} when this toast is not deduplicated.
     * <p>
     * The engine merges a newly shown toast into an existing one only when both carry the same
     * non-null key, so deduplication is opt-in per toast.
     *
     * @return the deduplication key, or {@code null}
     */
    public String deduplicationKey() {
        return deduplicationKey;
    }
}
