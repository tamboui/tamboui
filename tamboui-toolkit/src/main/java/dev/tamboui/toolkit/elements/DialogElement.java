/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.element.StyledElement;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Layout;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.tui.Keys;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.widgets.Clear;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.widgets.block.Title;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A dialog element that auto-centers in its parent area.
 * <p>
 * DialogElement simplifies creating modal dialogs by automatically:
 * <ul>
 *   <li>Centering the dialog in the parent area</li>
 *   <li>Clearing the background before rendering</li>
 *   <li>Calculating dimensions from content (or using fixed dimensions)</li>
 * </ul>
 *
 * <pre>{@code
 * dialog("Confirm Delete",
 *     text("Delete 3 files?"),
 *     text("[y] Yes  [n] No").dim()
 * ).rounded().borderColor(Color.YELLOW)
 * }</pre>
 */
public final class DialogElement extends StyledElement<DialogElement> {

    private String title;
    private BorderType borderType = BorderType.PLAIN;
    private Color borderColor;
    private final List<Element> children = new ArrayList<>();
    private Integer fixedWidth;
    private Integer fixedHeight;
    private int minWidth = 20;
    private int padding = 2;
    private Runnable onConfirm;
    private Runnable onCancel;

    public DialogElement() {
    }

    public DialogElement(String title, Element... children) {
        this.title = title;
        this.children.addAll(Arrays.asList(children));
    }

    public DialogElement(Element... children) {
        this.children.addAll(Arrays.asList(children));
    }

    /**
     * Sets the dialog title.
     */
    public DialogElement title(String title) {
        this.title = title;
        return this;
    }

    /**
     * Sets the border type to rounded.
     */
    public DialogElement rounded() {
        this.borderType = BorderType.ROUNDED;
        return this;
    }

    /**
     * Sets the border type to double.
     */
    public DialogElement doubleBorder() {
        this.borderType = BorderType.DOUBLE;
        return this;
    }

    /**
     * Sets the border type.
     */
    public DialogElement borderType(BorderType type) {
        this.borderType = type;
        return this;
    }

    /**
     * Sets the border color.
     */
    public DialogElement borderColor(Color color) {
        this.borderColor = color;
        return this;
    }

    /**
     * Sets a fixed width for the dialog.
     */
    public DialogElement width(int width) {
        this.fixedWidth = width;
        return this;
    }

    /**
     * Sets a fixed height for the dialog.
     */
    public DialogElement height(int height) {
        this.fixedHeight = height;
        return this;
    }

    /**
     * Sets the minimum width for the dialog.
     */
    public DialogElement minWidth(int minWidth) {
        this.minWidth = minWidth;
        return this;
    }

    /**
     * Sets the padding around content for width calculation.
     */
    public DialogElement padding(int padding) {
        this.padding = padding;
        return this;
    }

    /**
     * Adds a child element.
     */
    public DialogElement add(Element child) {
        this.children.add(child);
        return this;
    }

    /**
     * Adds multiple child elements.
     */
    public DialogElement add(Element... children) {
        this.children.addAll(Arrays.asList(children));
        return this;
    }

    /**
     * Sets the callback to run when the dialog is confirmed (Enter key).
     *
     * @param callback the callback to run on confirmation
     * @return this element
     */
    public DialogElement onConfirm(Runnable callback) {
        this.onConfirm = callback;
        return this;
    }

    /**
     * Sets the callback to run when the dialog is cancelled (Escape key).
     *
     * @param callback the callback to run on cancellation
     * @return this element
     */
    public DialogElement onCancel(Runnable callback) {
        this.onCancel = callback;
        return this;
    }

    /**
     * Handles key events for the dialog.
     * <p>
     * Routes events to children first (e.g., TextInputElement handles text input).
     * Then handles Enter for confirm and Escape for cancel.
     */
    @Override
    public EventResult handleKeyEvent(KeyEvent event, boolean focused) {
        // Route to children first
        for (Element child : children) {
            if (child.handleKeyEvent(event, true) == EventResult.HANDLED) {
                return EventResult.HANDLED;
            }
        }

        // Handle Escape to cancel
        if (event.code() == KeyCode.ESCAPE) {
            if (onCancel != null) {
                onCancel.run();
            }
            return EventResult.HANDLED;
        }

        // Handle Enter to confirm
        if (Keys.isEnter(event)) {
            if (onConfirm != null) {
                onConfirm.run();
            }
            return EventResult.HANDLED;
        }

        return EventResult.HANDLED; // Modal: consume all events
    }

    @Override
    public Constraint constraint() {
        // Dialog fills parent area (it centers itself within)
        return Constraint.fill();
    }

    @Override
    public void render(Frame frame, Rect area, RenderContext context) {
        if (area.isEmpty()) {
            return;
        }

        // Calculate dialog dimensions
        int dialogWidth = calculateWidth(area);
        int dialogHeight = calculateHeight();

        // Ensure dialog fits in available area
        dialogWidth = Math.min(dialogWidth, area.width());
        dialogHeight = Math.min(dialogHeight, area.height());

        // Center the dialog
        int x = (area.width() - dialogWidth) / 2;
        int y = (area.height() - dialogHeight) / 2;
        Rect dialogArea = new Rect(area.x() + x, area.y() + y, dialogWidth, dialogHeight);

        // Clear the dialog area first
        frame.renderWidget(Clear.INSTANCE, dialogArea);

        // Build the block
        Block.Builder blockBuilder = Block.builder()
            .borders(Borders.ALL)
            .borderType(borderType)
            .style(style);

        if (borderColor != null) {
            blockBuilder.borderStyle(Style.EMPTY.fg(borderColor));
        }

        if (title != null) {
            blockBuilder.title(Title.from(Line.from(Span.raw(title))));
        }

        Block block = blockBuilder.build();

        // Render the block
        frame.renderWidget(block, dialogArea);

        // Get inner area for children
        Rect innerArea = block.inner(dialogArea);
        if (innerArea.isEmpty() || children.isEmpty()) {
            return;
        }

        // Layout children vertically
        List<Constraint> constraints = new ArrayList<>();
        for (Element child : children) {
            Constraint c = child.constraint();
            constraints.add(c != null ? c : Constraint.length(1));
        }

        List<Rect> areas = Layout.vertical()
            .constraints(constraints.toArray(new Constraint[0]))
            .split(innerArea);

        // Render children
        for (int i = 0; i < children.size() && i < areas.size(); i++) {
            children.get(i).render(frame, areas.get(i), context);
        }
    }

    private int calculateWidth(Rect area) {
        if (fixedWidth != null) {
            return fixedWidth;
        }

        // Calculate based on title and minimum
        int titleWidth = title != null ? title.length() + 4 : 0;
        return Math.max(minWidth, titleWidth) + padding * 2;
    }

    private int calculateHeight() {
        if (fixedHeight != null) {
            return fixedHeight;
        }

        // 2 for borders + number of children (1 line each by default)
        return 2 + children.size();
    }
}
