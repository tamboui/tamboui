/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.toast;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import dev.tamboui.style.Style;

import static dev.tamboui.util.CollectionUtil.listCopyOf;

/**
 * Immutable per-frame render model for painting one toast via {@code ToastWidget}.
 */
public final class ToastRenderSnapshot {

    private final String id;
    private final ToastType type;
    private final String title;
    private final List<String> messageLines;
    private final double remainingFraction;
    private final BorderMode borderMode;
    private final TitleLayout titleLayout;
    private final TitleSeparator titleSeparator;
    private final TitleAlignment titleAlignment;
    private final boolean highlightTitle;
    private final ProgressStyle progressStyle;
    private final Style typeStyle;
    private final Style titleStyle;
    private final Style messageStyle;
    private final Style progressBarStyle;

    /**
     * Creates a render snapshot with all fields required for one paint pass.
     *
     * @param id the toast instance id
     * @param type the semantic toast type
     * @param title the optional title, or {@code null}
     * @param messageLines wrapped message lines
     * @param remainingFraction fraction of lifetime remaining ({@code 0..1}); ignored for sticky toasts
     * @param borderMode the border rendering mode
     * @param titleLayout the title row layout mode
     * @param titleSeparator the separator between title segments
     * @param titleAlignment the title row alignment
     * @param highlightTitle whether the title band is highlighted
     * @param progressStyle the progress bar visual style
     * @param typeStyle the resolved style for type accent and borders
     * @param titleStyle the resolved style for the title band
     * @param messageStyle the resolved style for message lines
     * @param progressBarStyle the resolved style for the progress bar
     */
    public ToastRenderSnapshot(
            String id,
            ToastType type,
            String title,
            List<String> messageLines,
            double remainingFraction,
            BorderMode borderMode,
            TitleLayout titleLayout,
            TitleSeparator titleSeparator,
            TitleAlignment titleAlignment,
            boolean highlightTitle,
            ProgressStyle progressStyle,
            Style typeStyle,
            Style titleStyle,
            Style messageStyle,
            Style progressBarStyle) {
        this.id = id;
        this.type = Objects.requireNonNull(type, "type");
        this.title = title;
        this.messageLines = listCopyOf(Objects.requireNonNull(messageLines, "messageLines"));
        this.remainingFraction = remainingFraction;
        this.borderMode = Objects.requireNonNull(borderMode, "borderMode");
        this.titleLayout = Objects.requireNonNull(titleLayout, "titleLayout");
        this.titleSeparator = Objects.requireNonNull(titleSeparator, "titleSeparator");
        this.titleAlignment = Objects.requireNonNull(titleAlignment, "titleAlignment");
        this.highlightTitle = highlightTitle;
        this.progressStyle = Objects.requireNonNull(progressStyle, "progressStyle");
        this.typeStyle = Objects.requireNonNull(typeStyle, "typeStyle");
        this.titleStyle = Objects.requireNonNull(titleStyle, "titleStyle");
        this.messageStyle = Objects.requireNonNull(messageStyle, "messageStyle");
        this.progressBarStyle = Objects.requireNonNull(progressBarStyle, "progressBarStyle");
    }

    /**
     * Builds a snapshot from a toast definition and resolved styles.
     *
     * @param toast the toast definition
     * @param lines wrapped message lines
     * @param remainingFraction fraction of lifetime remaining ({@code 0..1})
     * @param borderMode the border rendering mode
     * @param typeStyle the resolved type accent style
     * @param titleStyle the resolved title style
     * @param messageStyle the resolved message style
     * @param progressBarStyle the resolved progress bar style
     * @return a new snapshot with {@code id} set to {@code null}
     */
    public static ToastRenderSnapshot from(
            Toast toast,
            List<String> lines,
            double remainingFraction,
            BorderMode borderMode,
            Style typeStyle,
            Style titleStyle,
            Style messageStyle,
            Style progressBarStyle) {
        return from(null, toast, lines, remainingFraction, borderMode,
                typeStyle, titleStyle, messageStyle, progressBarStyle);
    }

    /**
     * Builds a snapshot from a toast instance id, definition, and resolved styles.
     *
     * @param id the toast instance id, or {@code null} when not yet assigned
     * @param toast the toast definition
     * @param lines wrapped message lines
     * @param remainingFraction fraction of lifetime remaining ({@code 0..1})
     * @param borderMode the border rendering mode
     * @param typeStyle the resolved type accent style
     * @param titleStyle the resolved title style
     * @param messageStyle the resolved message style
     * @param progressBarStyle the resolved progress bar style
     * @return a new snapshot
     */
    public static ToastRenderSnapshot from(
            String id,
            Toast toast,
            List<String> lines,
            double remainingFraction,
            BorderMode borderMode,
            Style typeStyle,
            Style titleStyle,
            Style messageStyle,
            Style progressBarStyle) {
        Objects.requireNonNull(toast, "toast");
        return new ToastRenderSnapshot(
                id,
                toast.type(),
                toast.title(),
                lines,
                remainingFraction,
                borderMode,
                toast.titleLayout(),
                toast.titleSeparator(),
                toast.titleAlignment(),
                toast.highlightTitle(),
                toast.progressStyle(),
                typeStyle,
                titleStyle,
                messageStyle,
                progressBarStyle);
    }

    /**
     * Returns the toast instance id.
     *
     * @return the id, or {@code null} when not assigned
     */
    public String id() {
        return id;
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
     * Returns the wrapped message lines.
     *
     * @return an unmodifiable list of message lines
     */
    public List<String> messageLines() {
        return Collections.unmodifiableList(messageLines);
    }

    /**
     * Returns the fraction of lifetime remaining ({@code 0..1}).
     *
     * @return the remaining fraction
     */
    public double remainingFraction() {
        return remainingFraction;
    }

    /**
     * Returns the border rendering mode.
     *
     * @return the border mode
     */
    public BorderMode borderMode() {
        return borderMode;
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
     * Returns the separator between title segments.
     *
     * @return the title separator
     */
    public TitleSeparator titleSeparator() {
        return titleSeparator;
    }

    /**
     * Returns the title row alignment.
     *
     * @return the title alignment
     */
    public TitleAlignment titleAlignment() {
        return titleAlignment;
    }

    /**
     * Returns whether the title band is highlighted.
     *
     * @return {@code true} when highlighted
     */
    public boolean highlightTitle() {
        return highlightTitle;
    }

    /**
     * Returns the progress bar visual style.
     *
     * @return the progress style
     */
    public ProgressStyle progressStyle() {
        return progressStyle;
    }

    /**
     * Returns the resolved style for type accent and borders.
     *
     * @return the type style
     */
    public Style typeStyle() {
        return typeStyle;
    }

    /**
     * Returns the resolved style for the title band.
     *
     * @return the title style
     */
    public Style titleStyle() {
        return titleStyle;
    }

    /**
     * Returns the resolved style for message lines.
     *
     * @return the message style
     */
    public Style messageStyle() {
        return messageStyle;
    }

    /**
     * Returns the resolved style for the progress bar.
     *
     * @return the progress bar style
     */
    public Style progressBarStyle() {
        return progressBarStyle;
    }
}
