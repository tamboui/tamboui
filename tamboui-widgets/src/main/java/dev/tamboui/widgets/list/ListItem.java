/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.list;

import dev.tamboui.style.Style;
import dev.tamboui.text.Line;
import dev.tamboui.text.Text;
import dev.tamboui.widgets.common.SizedWidget;
import dev.tamboui.widgets.paragraph.Paragraph;

/**
 * An item in a list widget.
 */
public final class ListItem {

    private final Text content;
    private final Style style;

    /**
     * Creates a new list item with the given content and style.
     *
     * @param content the item content
     * @param style the item style
     */
    public ListItem(Text content, Style style) {
        this.content = content;
        this.style = style;
    }

    /**
     * Creates a list item from a string.
     *
     * @param text the item text
     * @return a new list item
     */
    public static ListItem from(String text) {
        return new ListItem(Text.from(text), Style.EMPTY);
    }

    /**
     * Creates a list item from a styled line.
     *
     * @param line the item content as a line
     * @return a new list item
     */
    public static ListItem from(Line line) {
        return new ListItem(Text.from(line), Style.EMPTY);
    }

    /**
     * Creates a list item from a text.
     *
     * @param text the item content
     * @return a new list item
     */
    public static ListItem from(Text text) {
        return new ListItem(text, Style.EMPTY);
    }

    /**
     * Returns a new list item with the given style applied.
     *
     * @param style the style to apply
     * @return a new list item with the given style
     */
    public ListItem style(Style style) {
        return new ListItem(content, style);
    }

    /**
     * Returns the height of this item in lines.
     *
     * @return the height in lines
     */
    public int height() {
        return content.height();
    }

    /**
     * Returns the item content.
     *
     * @return the content
     */
    public Text content() {
        return content;
    }

    /**
     * Returns the item style.
     *
     * @return the style
     */
    public Style style() {
        return style;
    }

    /**
     * Converts this list item to a {@link SizedWidget} for use with the new
     * {@link ListWidget} builder.
     * <p>
     * The widget renders the item's {@link Text} content using a {@link Paragraph},
     * with the item's style applied as background.
     *
     * @return a SizedWidget wrapping this item's content
     */
    public SizedWidget toSizedWidget() {
        Paragraph paragraph = style.equals(Style.EMPTY)
                ? Paragraph.from(content)
                : Paragraph.builder().text(content).style(style).build();
        return SizedWidget.ofHeight(paragraph, content.height());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ListItem)) {
            return false;
        }
        ListItem listItem = (ListItem) o;
        return content.equals(listItem.content) && style.equals(listItem.style);
    }

    @Override
    public int hashCode() {
        int result = content.hashCode();
        result = 31 * result + style.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return String.format("ListItem[content=%s, style=%s]", content, style);
    }
}
