/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.toast;

import java.time.Duration;
import java.util.Objects;

import dev.tamboui.style.Color;

/**
 * Fluent builder for {@link Toast}, following the {@code Spinner.Builder} idiom.
 *
 * <pre>{@code
 * Toast toast = ToastBuilder.info("Saved")
 *     .title("Done")
 *     .duration(Duration.ofSeconds(3))
 *     .build();
 *
 * Toast sticky = ToastBuilder.warning("Check logs")
 *     .sticky(true)
 *     .build();
 * }</pre>
 */
public final class ToastBuilder {

    ToastType type;
    String title;
    String message;
    boolean sticky;
    Duration lifetime;
    Color backgroundColor;
    TitleLayout titleLayout = TitleLayout.COMPACT;
    TitleSeparator titleSeparator = TitleSeparator.DOT;
    TitleAlignment titleAlignment = TitleAlignment.START;
    boolean highlightTitle;
    ProgressStyle progressStyle = ProgressStyle.FULL_BLOCK;
    String dedupKey;

    private ToastBuilder() {
    }

    /**
     * Creates a new toast builder with no preset type or message.
     *
     * @return a new builder
     */
    public static ToastBuilder builder() {
        return new ToastBuilder();
    }

    /**
     * Creates a builder for an info toast with the given message.
     *
     * @param message the toast message
     * @return a new builder
     */
    public static ToastBuilder info(String message) {
        return builder().type(ToastType.INFO).message(message);
    }

    /**
     * Creates a builder for a success toast with the given message.
     *
     * @param message the toast message
     * @return a new builder
     */
    public static ToastBuilder success(String message) {
        return builder().type(ToastType.SUCCESS).message(message);
    }

    /**
     * Creates a builder for a warning toast with the given message.
     *
     * @param message the toast message
     * @return a new builder
     */
    public static ToastBuilder warning(String message) {
        return builder().type(ToastType.WARNING).message(message);
    }

    /**
     * Creates a builder for an error toast with the given message.
     *
     * @param message the toast message
     * @return a new builder
     */
    public static ToastBuilder error(String message) {
        return builder().type(ToastType.ERROR).message(message);
    }

    /**
     * Sets the semantic toast type.
     *
     * @param type the toast type
     * @return this builder
     */
    public ToastBuilder type(ToastType type) {
        this.type = type;
        return this;
    }

    /**
     * Sets the optional title text.
     *
     * @param title the title, or {@code null} to omit
     * @return this builder
     */
    public ToastBuilder title(String title) {
        this.title = title;
        return this;
    }

    /**
     * Sets the toast message body.
     *
     * @param message the message
     * @return this builder
     */
    public ToastBuilder message(String message) {
        this.message = message;
        return this;
    }

    /**
     * Sets whether the toast stays until manually dismissed.
     * <p>
     * Mutually exclusive with {@link #duration(Duration)}.
     *
     * @param sticky {@code true} for a sticky toast
     * @return this builder
     */
    public ToastBuilder sticky(boolean sticky) {
        this.sticky = sticky;
        return this;
    }

    /**
     * Sets the auto-dismiss lifetime for a timed toast.
     * <p>
     * Mutually exclusive with {@link #sticky(boolean)} when {@code sticky} is {@code true}.
     *
     * @param duration the positive lifetime
     * @return this builder
     */
    public ToastBuilder duration(Duration duration) {
        this.lifetime = Objects.requireNonNull(duration, "duration");
        return this;
    }

    /**
     * Sets an optional background color override.
     *
     * @param backgroundColor the background color, or {@code null} for defaults
     * @return this builder
     */
    public ToastBuilder backgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
        return this;
    }

    /**
     * Sets the title row layout mode.
     *
     * @param titleLayout the layout mode
     * @return this builder
     */
    public ToastBuilder titleLayout(TitleLayout titleLayout) {
        this.titleLayout = Objects.requireNonNull(titleLayout, "titleLayout");
        return this;
    }

    /**
     * Sets the separator rendered between title segments.
     *
     * @param titleSeparator the separator style
     * @return this builder
     */
    public ToastBuilder titleSeparator(TitleSeparator titleSeparator) {
        this.titleSeparator = Objects.requireNonNull(titleSeparator, "titleSeparator");
        return this;
    }

    /**
     * Sets the horizontal alignment of the title row.
     *
     * @param titleAlignment the alignment
     * @return this builder
     */
    public ToastBuilder titleAlignment(TitleAlignment titleAlignment) {
        this.titleAlignment = Objects.requireNonNull(titleAlignment, "titleAlignment");
        return this;
    }

    /**
     * Sets whether the title band uses the type accent as a highlighted background.
     *
     * @param highlight {@code true} to highlight the title
     * @return this builder
     */
    public ToastBuilder highlight(boolean highlight) {
        this.highlightTitle = highlight;
        return this;
    }

    /**
     * Sets the visual style for the auto-dismiss progress indicator.
     *
     * @param progressStyle the progress style
     * @return this builder
     */
    public ToastBuilder progressStyle(ProgressStyle progressStyle) {
        this.progressStyle = Objects.requireNonNull(progressStyle, "progressStyle");
        return this;
    }

    /**
     * Sets the deduplication key used by the toast engine.
     * <p>
     * When not set, defaults to a hash of type, title, and message.
     *
     * @param dedupKey the dedup key
     * @return this builder
     */
    public ToastBuilder dedupKey(String dedupKey) {
        this.dedupKey = dedupKey;
        return this;
    }

    /**
     * Builds the immutable toast definition.
     *
     * @return a new toast
     */
    public Toast build() {
        return new Toast(this);
    }
}
