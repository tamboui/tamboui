/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.text.CharWidth;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.element.Size;
import dev.tamboui.toolkit.element.StyledElement;

/**
 * A horizontal divider / separator line with optional text on the left, center, or right.
 * <p>
 * Supports customizable line styles and CSS styling for both the line and text positions.
 * <pre>{@code
 * divider()
 *     .line("Section Title")
 *     .style(SINGLE)
 *     .fg(Color.CYAN)
 *
 * divider("Left Text").style(BOLD)
 *
 * divider()
 *     .left("Left").center("Middle").right("Right")
 *     .style(DOUBLE)
 * }</pre>
 *
 * <h2>CSS Child Selectors</h2>
 * <p>
 * The following child selectors can be used to style sub-components:
 * <ul>
 *   <li>{@code DividerElement-line} - The separator line (also the default element style)</li>
 *   <li>{@code DividerElement-left} - Text positioned on the left</li>
 *   <li>{@code DividerElement-center} - Text positioned in the center</li>
 *   <li>{@code DividerElement-right} - Text positioned on the right</li>
 * </ul>
 * <p>
 * Example CSS:
 * <pre>{@code
 * DividerElement { color: gray; }
 * DividerElement-left { color: red; }
 * DividerElement-center { text-style: bold; color: cyan; }
 * DividerElement-right { color: green; }
 * }</pre>
 * <p>
 * Note: Programmatic styles set via the corresponding setter methods take precedence over CSS styles.
 */
public final class DividerElement extends StyledElement<DividerElement> {

    private static final Style DEFAULT_LINE_STYLE = Style.EMPTY;
    private static final Style DEFAULT_LEFT_STYLE = Style.EMPTY;
    private static final Style DEFAULT_CENTER_STYLE = Style.EMPTY;
    private static final Style DEFAULT_RIGHT_STYLE = Style.EMPTY;

    private DividerStyle dividerStyle = DividerStyle.SINGLE;
    private String leftText;
    private String centerText;
    private String rightText;
    private Style lineStyle;
    private Style leftStyle;
    private Style centerStyle;
    private Style rightStyle;

    /** Creates a plain divider with default single-line style and no text. */
    public DividerElement() {
    }

    /**
     * Creates a divider with text on the left side.
     *
     * @param leftText the text to display on the left
     */
    public DividerElement(String leftText) {
        this.leftText = leftText;
    }

    /**
     * Sets the divider line style.
     *
     * @param style the line style
     * @return this element
     */
    public DividerElement style(DividerStyle style) {
        this.dividerStyle = style != null ? style : DividerStyle.SINGLE;
        return this;
    }

    /**
     * Uses single-line style ({@code ─}).
     *
     * @return this element
     */
    public DividerElement single() {
        this.dividerStyle = DividerStyle.SINGLE;
        return this;
    }

    /**
     * Uses double-line style ({@code ═}).
     *
     * @return this element
     */
    public DividerElement doubleLine() {
        this.dividerStyle = DividerStyle.DOUBLE;
        return this;
    }

    /**
     * Uses bold line style ({@code ━}).
     *
     * @return this element
     */
    public DividerElement boldLine() {
        this.dividerStyle = DividerStyle.BOLD;
        return this;
    }

    /**
     * Uses dotted line style ({@code ·}).
     *
     * @return this element
     */
    public DividerElement dotted() {
        this.dividerStyle = DividerStyle.DOTTED;
        return this;
    }

    /**
     * Uses dashed line style ({@code -}).
     *
     * @return this element
     */
    public DividerElement dashed() {
        this.dividerStyle = DividerStyle.DASHED;
        return this;
    }

    /**
     * Uses heavy block style ({@code █}).
     *
     * @return this element
     */
    public DividerElement heavy() {
        this.dividerStyle = DividerStyle.HEAVY;
        return this;
    }

    /**
     * Uses rounded style ({@code ╭─╮}).
     *
     * @return this element
     */
    public DividerElement rounded() {
        this.dividerStyle = DividerStyle.ROUNDED;
        return this;
    }

    /**
     * Sets text on the left side of the divider.
     *
     * @param text the left text
     * @return this element
     */
    public DividerElement left(String text) {
        this.leftText = text;
        return this;
    }

    /**
     * Sets text in the center of the divider.
     *
     * @param text the center text
     * @return this element
     */
    public DividerElement center(String text) {
        this.centerText = text;
        return this;
    }

    /**
     * Sets text on the right side of the divider.
     *
     * @param text the right text
     * @return this element
     */
    public DividerElement right(String text) {
        this.rightText = text;
        return this;
    }

    /**
     * Sets text spanning the full divider (replaces left text, center-aligned).
     * Convenience method for {@code center(text)}.
     *
     * @param text the text to display
     * @return this element
     */
    public DividerElement line(String text) {
        this.centerText = text;
        return this;
    }

    /**
     * Sets the style for the separator line.
     *
     * @param style the line style
     * @return this element
     */
    public DividerElement lineStyle(Style style) {
        this.lineStyle = style;
        return this;
    }

    /**
     * Sets the color for the separator line.
     *
     * @param color the line color
     * @return this element
     */
    public DividerElement lineColor(Color color) {
        this.lineStyle = Style.EMPTY.fg(color);
        return this;
    }

    /**
     * Sets the style for the left text.
     *
     * @param style the left text style
     * @return this element
     */
    public DividerElement leftStyle(Style style) {
        this.leftStyle = style;
        return this;
    }

    /**
     * Sets the color for the left text.
     *
     * @param color the left text color
     * @return this element
     */
    public DividerElement leftColor(Color color) {
        this.leftStyle = Style.EMPTY.fg(color);
        return this;
    }

    /**
     * Sets the style for the center text.
     *
     * @param style the center text style
     * @return this element
     */
    public DividerElement centerStyle(Style style) {
        this.centerStyle = style;
        return this;
    }

    /**
     * Sets the color for the center text.
     *
     * @param color the center text color
     * @return this element
     */
    public DividerElement centerColor(Color color) {
        this.centerStyle = Style.EMPTY.fg(color);
        return this;
    }

    /**
     * Sets the style for the right text.
     *
     * @param style the right text style
     * @return this element
     */
    public DividerElement rightStyle(Style style) {
        this.rightStyle = style;
        return this;
    }

    /**
     * Sets the color for the right text.
     *
     * @param color the right text color
     * @return this element
     */
    public DividerElement rightColor(Color color) {
        this.rightStyle = Style.EMPTY.fg(color);
        return this;
    }

    /**
     * Returns the current divider style.
     *
     * @return the divider style
     */
    public DividerStyle dividerStyle() {
        return dividerStyle;
    }

    /**
     * Returns the left text, or null if not set.
     *
     * @return the left text
     */
    public String leftText() {
        return leftText;
    }

    /**
     * Returns the center text, or null if not set.
     *
     * @return the center text
     */
    public String centerText() {
        return centerText;
    }

    /**
     * Returns the right text, or null if not set.
     *
     * @return the right text
     */
    public String rightText() {
        return rightText;
    }

    @Override
    public Size preferredSize(int availableWidth, int availableHeight, RenderContext context) {
        // Divider is always 1 row tall
        int textWidth = 0;
        if (leftText != null) textWidth = Math.max(textWidth, CharWidth.of(leftText) + 2);
        if (centerText != null) textWidth = Math.max(textWidth, CharWidth.of(centerText) + 2);
        if (rightText != null) textWidth = Math.max(textWidth, CharWidth.of(rightText) + 2);

        int width = availableWidth > 0 ? availableWidth : Math.max(3, textWidth);
        return Size.of(width, 1);
    }

    @Override
    public Map<String, String> styleAttributes() {
        Map<String, String> attrs = new LinkedHashMap<>(super.styleAttributes());
        if (leftText != null) {
            attrs.put("left", leftText);
        }
        if (centerText != null) {
            attrs.put("center", centerText);
        }
        if (rightText != null) {
            attrs.put("right", rightText);
        }
        return Collections.unmodifiableMap(attrs);
    }

    @Override
    protected void renderContent(Frame frame, Rect area, RenderContext context) {
        if (area.isEmpty()) {
            return;
        }

        Style effectiveStyle = context.currentStyle();

        // Resolve styles with priority: explicit > CSS > default
        Style effectiveLineStyle = resolveEffectiveStyle(context, "line", lineStyle, DEFAULT_LINE_STYLE);
        Style effectiveLeftStyle = resolveEffectiveStyle(context, "left", leftStyle, DEFAULT_LEFT_STYLE);
        Style effectiveCenterStyle = resolveEffectiveStyle(context, "center", centerStyle, DEFAULT_CENTER_STYLE);
        Style effectiveRightStyle = resolveEffectiveStyle(context, "right", rightStyle, DEFAULT_RIGHT_STYLE);

        // Merge: if a sub-style has no explicit color, inherit from the element's effective style
        Style mergedLineStyle = mergeWithParent(effectiveLineStyle, effectiveStyle);
        Style mergedLeftStyle = mergeWithParent(effectiveLeftStyle, effectiveStyle);
        Style mergedCenterStyle = mergeWithParent(effectiveCenterStyle, effectiveStyle);
        Style mergedRightStyle = mergeWithParent(effectiveRightStyle, effectiveStyle);

        String lineChar = dividerStyle.line();
        int width = area.width();
        int height = area.height();
        int y = area.top();

        Buffer buffer = frame.buffer();

        // Draw the separator line across the full width for each row
        StringBuilder fullLine = new StringBuilder(width);
        for (int i = 0; i < width; i++) {
            fullLine.append(lineChar);
        }
        String lineStr = fullLine.toString();
        for (int row = 0; row < height; row++) {
            int currentY = y + row;
            buffer.setString(area.left(), currentY, lineStr, mergedLineStyle);
        }

        // Render text overlays on the first row only
        int textRow = y;

        // Left text: flush left with 1 space padding on right
        if (leftText != null && !leftText.isEmpty()) {
            int leftW = Math.min(leftText.length(), width);
            buffer.setString(area.left(), textRow, leftText.substring(0, leftW), mergedLeftStyle);
            // Clear the line char immediately after the text to create a gap
            int gapPos = area.left() + leftW;
            if (gapPos < area.right()) {
                buffer.set(gapPos, textRow, Cell.EMPTY);
            }
        }

        // Right text: flush right with 1 space padding on left
        if (rightText != null && !rightText.isEmpty()) {
            int rightWidth = CharWidth.of(rightText);
            int startPos = Math.max(0, width - rightWidth);
            // 1 space padding before the text
            if (startPos > 0) {
                buffer.set(area.left() + startPos - 1, textRow, Cell.EMPTY);
            }
            buffer.setString(area.left() + startPos, textRow, rightText, mergedRightStyle);
        }

        // Center text: centered with 1 space padding on each side
        if (centerText != null && !centerText.isEmpty()) {
            int centerWidth = CharWidth.of(centerText);
            int startPos = (width - centerWidth) / 2;
            // 1 space padding on each side (clamped)
            if (startPos > 0) {
                buffer.set(area.left() + startPos - 1, textRow, Cell.EMPTY);
            }
            int afterEnd = startPos + centerWidth;
            if (afterEnd < width) {
                buffer.set(area.left() + afterEnd, textRow, Cell.EMPTY);
            }
            buffer.setString(area.left() + startPos, textRow, centerText, mergedCenterStyle);
        }
    }

    /**
     * Merges a sub-component style with the parent element's style.
     * If the sub-style has no explicit foreground/background, it inherits
     * from the parent.
     */
    private static Style mergeWithParent(Style subStyle, Style parentStyle) {
        Style merged = subStyle;
        if (!merged.fg().isPresent() && parentStyle.fg().isPresent()) {
            merged = merged.fg(parentStyle.fg().get());
        }
        if (!merged.bg().isPresent() && parentStyle.bg().isPresent()) {
            merged = merged.bg(parentStyle.bg().get());
        }
        return merged;
    }
}
