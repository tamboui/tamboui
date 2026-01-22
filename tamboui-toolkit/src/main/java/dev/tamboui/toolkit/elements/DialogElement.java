/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import dev.tamboui.css.Styleable;
import dev.tamboui.css.cascade.CssStyleResolver;
import dev.tamboui.toolkit.element.ContainerElement;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Direction;
import dev.tamboui.layout.Flex;
import dev.tamboui.layout.Layout;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.widgets.Clear;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.widgets.block.Title;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A dialog element that auto-centers in its parent area.
 * <p>
 * DialogElement simplifies creating modal dialogs by automatically:
 * <ul>
 *   <li>Centering the dialog in the parent area</li>
 *   <li>Clearing the background before rendering</li>
 *   <li>Calculating dimensions from content (or using fixed dimensions)</li>
 * </ul>
 * <p>
 * Layout properties for dialog content can be set via CSS or programmatically:
 * <ul>
 *   <li>{@code direction} - Layout direction: "horizontal"/"row" or "vertical"/"column"</li>
 *   <li>{@code flex} - Flex positioning mode: "start", "center", "end", "space-between", "space-around", "space-evenly"</li>
 *   <li>{@code spacing} - Gap between children in cells</li>
 * </ul>
 * <p>
 * Programmatic values override CSS values when both are set.
 *
 * <pre>{@code
 * dialog("Confirm Delete",
 *     text("Delete 3 files?"),
 *     text("[y] Yes  [n] No").dim()
 * ).rounded().borderColor(Color.YELLOW)
 * }</pre>
 */
public final class DialogElement extends ContainerElement<DialogElement> {

    private String title;
    private BorderType borderType = BorderType.PLAIN;
    private Color borderColor;
    private Integer fixedWidth;
    private Integer fixedHeight;
    private int minWidth = 20;
    private int padding = 2;
    private Direction direction;
    private Flex flex;
    private Integer spacing;
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
     * Sets the layout direction for children.
     * <p>
     * Can also be set via CSS {@code direction} property.
     *
     * @param direction the layout direction
     * @return this dialog for chaining
     */
    public DialogElement direction(Direction direction) {
        this.direction = direction;
        return this;
    }

    /**
     * Sets the layout direction to horizontal.
     *
     * @return this dialog for chaining
     */
    public DialogElement horizontal() {
        this.direction = Direction.HORIZONTAL;
        return this;
    }

    /**
     * Sets the layout direction to vertical.
     *
     * @return this dialog for chaining
     */
    public DialogElement vertical() {
        this.direction = Direction.VERTICAL;
        return this;
    }

    /**
     * Sets the flex layout mode for positioning children.
     * <p>
     * Can also be set via CSS {@code flex} property.
     *
     * @param flex the flex mode
     * @return this dialog for chaining
     */
    public DialogElement flex(Flex flex) {
        this.flex = flex;
        return this;
    }

    /**
     * Sets the spacing (gap) between children.
     * <p>
     * Can also be set via CSS {@code spacing} property.
     *
     * @param spacing the spacing in cells
     * @return this dialog for chaining
     */
    public DialogElement spacing(int spacing) {
        this.spacing = spacing;
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
     * Routes events to children first (via ContainerElement).
     * Then handles Enter for confirm and Escape for cancel.
     * Being modal, the dialog consumes all key events.
     */
    @Override
    public EventResult handleKeyEvent(KeyEvent event, boolean focused) {
        // Route to children first (via ContainerElement)
        EventResult result = super.handleKeyEvent(event, focused);
        if (result.isHandled()) {
            return result;
        }

        // Handle Escape to cancel
        if (event.code() == KeyCode.ESCAPE) {
            if (onCancel != null) {
                onCancel.run();
            }
            return EventResult.HANDLED;
        }

        // Handle Enter to confirm
        if (event.isConfirm()) {
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
    public Map<String, String> styleAttributes() {
        Map<String, String> attrs = new LinkedHashMap<>(super.styleAttributes());
        if (title != null) {
            attrs.put("title", title);
        }
        return Collections.unmodifiableMap(attrs);
    }

    @Override
    protected void renderContent(Frame frame, Rect area, RenderContext context) {
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
            .style(context.currentStyle())
            .styleResolver(styleResolver(context));

        if (borderColor != null) {
            blockBuilder.borderColor(borderColor);
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

        // Get CSS resolver for property resolution
        CssStyleResolver cssResolver = context.resolveStyle(this).orElse(null);

        // Resolve direction: programmatic > CSS > VERTICAL
        Direction effectiveDirection = this.direction;
        if (effectiveDirection == null && cssResolver != null) {
            effectiveDirection = cssResolver.direction().orElse(Direction.VERTICAL);
        } else if (effectiveDirection == null) {
            effectiveDirection = Direction.VERTICAL;
        }

        // Resolve flex: programmatic > CSS > START
        Flex effectiveFlex = this.flex;
        if (effectiveFlex == null && cssResolver != null) {
            effectiveFlex = cssResolver.flex().orElse(Flex.START);
        } else if (effectiveFlex == null) {
            effectiveFlex = Flex.START;
        }

        // Resolve spacing: programmatic > CSS > 0
        int effectiveSpacing = this.spacing != null ? this.spacing : 0;
        if (this.spacing == null && cssResolver != null) {
            effectiveSpacing = cssResolver.spacing().orElse(0);
        }

        // Layout children using resolved direction, flex, and spacing
        List<Constraint> constraints = new ArrayList<>();
        boolean isHorizontal = effectiveDirection == Direction.HORIZONTAL;
        for (Element child : children) {
            Constraint c = child.constraint();
            // Check CSS constraint if programmatic is null (width for horizontal, height for vertical)
            if (c == null && child instanceof Styleable) {
                CssStyleResolver childCss = context.resolveStyle((Styleable) child).orElse(null);
                if (childCss != null) {
                    c = isHorizontal
                            ? childCss.widthConstraint().orElse(null)
                            : childCss.heightConstraint().orElse(null);
                }
            }
            constraints.add(c != null ? c : Constraint.length(1));
        }

        Layout layout = effectiveDirection == Direction.HORIZONTAL
            ? Layout.horizontal()
            : Layout.vertical();

        layout = layout.constraints(constraints.toArray(new Constraint[0]))
            .flex(effectiveFlex);

        if (effectiveSpacing > 0) {
            layout = layout.spacing(effectiveSpacing);
        }

        List<Rect> areas = layout.split(innerArea);

        // Render children
        for (int i = 0; i < children.size() && i < areas.size(); i++) {
            context.renderChild(children.get(i), frame, areas.get(i));
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
