/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.list;

import dev.tamboui.style.Style;
import dev.tamboui.text.Line;
import dev.tamboui.text.Text;

/// An item in a list widget.
public final class ListItem {

    private final Text content;
    private final Style style;

    public ListItem(Text content, Style style) {
        this.content = content;
        this.style = style;
    }

    public static ListItem from(String text) {
        return new ListItem(Text.from(text), Style.EMPTY);
    }

    public static ListItem from(Line line) {
        return new ListItem(Text.from(line), Style.EMPTY);
    }

    public static ListItem from(Text text) {
        return new ListItem(text, Style.EMPTY);
    }

    public ListItem style(Style style) {
        return new ListItem(content, style);
    }

    public int height() {
        return content.height();
    }

    public Text content() {
        return content;
    }

    public Style style() {
        return style;
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

