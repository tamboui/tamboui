/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import dev.tamboui.css.Styleable;
import dev.tamboui.css.cascade.CssStyleResolver;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Layout;
import dev.tamboui.layout.Rect;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.element.ContainerElement;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.element.Size;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.MouseEvent;
import dev.tamboui.tui.event.MouseEventKind;
import dev.tamboui.widgets.scrollbar.Scrollbar;
import dev.tamboui.widgets.scrollbar.ScrollbarOrientation;
import dev.tamboui.widgets.scrollbar.ScrollbarState;

/**
 * A container element with a scrollbar that allows a vertical list that overflows its bounds.
 *
 * Note that each child counts as one scrollable unit; children will not be split across page boundaries.
 */
public final class ScrollableElement extends ContainerElement<ScrollableElement> {

    private final Scrollbar scrollbar;
    private final ScrollbarState state;
    private Element scrollUpIndicator = null;
    private Element scrollDownIndicator = null;

    /** Creates an empty scrollable container. */
    public ScrollableElement() {
        this(new Element[0]);
    }

    /**
     * Creates a scrollable with children.
     *
     * @param children the child elements
     */
    public ScrollableElement(Element... children) {
        this.scrollbar = Scrollbar.builder().orientation(ScrollbarOrientation.VERTICAL_RIGHT).build();
        this.state = new ScrollbarState();
        this.children.addAll(Arrays.asList(children));

        this.registerHandlers();
    }

    /**
     * Creates a scrollable with a custom scrollbar and children.
     *
     * @param scrollbar the scrollbar element
     * @param children the child elements
     */
    public ScrollableElement(Scrollbar scrollbar, Element... children) {
        this.scrollbar = scrollbar;
        this.state = new ScrollbarState();
        this.children.addAll(Arrays.asList(children));

        this.registerHandlers();
    }

    private void registerHandlers() {
        this.mouseHandler =
            (MouseEvent event) -> {
                if (event.kind() == MouseEventKind.SCROLL_UP) {
                    this.state.prev();
                    return EventResult.HANDLED;
                } else if (event.kind() == MouseEventKind.SCROLL_DOWN) {
                    this.state.next();
                    return EventResult.HANDLED;
                }
                return EventResult.UNHANDLED;
            };

        this.keyHandler =
            (KeyEvent event) -> {
                if (event.isUp()) {
                    this.state.prev();
                    return EventResult.HANDLED;
                } else if (event.isDown()) {
                    this.state.next();
                    return EventResult.HANDLED;
                } else if (event.isPageUp()) {
                    this.state.pageUp();
                    return EventResult.HANDLED;
                } else if (event.isPageDown()) {
                    this.state.pageDown();
                    return EventResult.HANDLED;
                }
                return EventResult.UNHANDLED;
            };
    }

    /**
     * Return the internal {@link ScrollbarState}.
     *
     * @return the scrollbar state
     */
    public ScrollbarState getState() {
        return this.state;
    }

    /**
     * Element of height 1 that will be displayed at the top of the area if there are
     * results above the current view.
     * @param scrollUpIndicator the indicator to use
     * @return this, for fluent operations
     */
    public ScrollableElement scrollUpIndicator(Element scrollUpIndicator) {
        this.scrollUpIndicator = scrollUpIndicator;
        return this;
    }

    /**
     * Element of height 1 that will be displayed at the bottom of the area if there are
     * results below the current view.
     * @param scrollDownIndicator the indicator to use
     * @return this, for fluent operations
     */
    public ScrollableElement scrollDownIndicator(Element scrollDownIndicator) {
        this.scrollDownIndicator = scrollDownIndicator;
        return this;
    }

    @Override
    public Size preferredSize(int availableWidth, int availableHeight, RenderContext context) {
        // Calculate width (+1 for scrollbar size)
        int width = 1;
        if (!children.isEmpty()) {
            // Vertical: max width of all children
            for (Element child : children) {
                Size childSize = child.preferredSize(availableWidth - 1, availableHeight, context);
                width = Math.max(width, childSize.widthOr(0) + 1);
            }
        }

        // Calculate height
        int height = 0;

        if (!children.isEmpty()) {
            // Content width for child height calculation
            int contentWidth = availableWidth > 0 ? Math.max(1, availableWidth - 1) : -1;

            for (Element child : children) {
                Size childSize = child.preferredSize(contentWidth, -1, context);
                height += childSize.heightOr(1);
            }
        }

        return Size.of(width, height);
    }

    @Override
    protected void renderContent(Frame frame, Rect area, RenderContext context) {
        if (area.isEmpty() || children.isEmpty()) {
            return;
        }

        // Get inner area for children (-1 width to fit scrollbar)
        Rect innerArea;
        Rect scrollbarArea;
        if (scrollbar.orientation() == ScrollbarOrientation.VERTICAL_LEFT) {
            innerArea = new Rect(area.x() + 1, area.y(), area.width() - 1, area.height());
            scrollbarArea = new Rect(area.x(), area.y(), 1, area.height());
        } else if (scrollbar.orientation() == ScrollbarOrientation.VERTICAL_RIGHT) {
            innerArea = new Rect(area.x(), area.y(), area.width() - 1, area.height());
            scrollbarArea = new Rect(area.x() + area.width() - 1, area.y(), 1, area.height());
        } else {
            throw new IllegalArgumentException("Scrollable only supports vertical scrollbars.");
        }
        if (innerArea.isEmpty() || children.isEmpty()) {
            return;
        }

        // Get child layout constraints
        List<Constraint> constraints = new ArrayList<>();
        for (Element child : children) {
            Constraint c = child.constraint();
            // Check CSS constraint if programmatic is null (width for horizontal, height for vertical)
            if (c == null && child instanceof Styleable) {
                CssStyleResolver childCss = context.resolveStyle((Styleable) child).orElse(null);
                if (childCss != null) {
                    c = childCss.heightConstraint().orElse(null);
                }
            }
            // Handle null constraint by querying preferred size
            if (c == null) {
                Size size = child.preferredSize(innerArea.width(), -1, context);
                int preferred = size.height();
                c = preferred >= 0 ? Constraint.length(preferred) : Constraint.fill();
            }
            constraints.add(c);
        }

        int totalRequiredHeight = constraints
            .stream()
            .mapToInt(c -> {
                if (c instanceof Constraint.Length) {
                    return ((Constraint.Length) c).value();
                } else {
                    return 1;
                }
            })
            .sum();

        state.contentLength(totalRequiredHeight);

        // go to top if all fit on the screen (or something happened and we passed the bottom)
        if (totalRequiredHeight <= innerArea.height() || state.position() >= children.size()) {
            state.position(0);
        }

        List<Element> visibleChildren = new ArrayList<>();
        int heightUtilized = 0;
        if (scrollUpIndicator != null && state.position() > 0) {
            Rect topIndicatorArea = new Rect(innerArea.x(), innerArea.y(), innerArea.width(), 1);
            innerArea = new Rect(innerArea.x(), innerArea.y() + 1, innerArea.width(), innerArea.height() - 1);
            scrollUpIndicator.render(frame, topIndicatorArea, context);
        }
        boolean moreBelow = false;
        int startingPosition = state.position();
        if (startingPosition != 0 && scrollUpIndicator != null) {
            startingPosition++;
        }
        for (int i = startingPosition; i < children.size(); i++) {
            if (constraints.get(i) instanceof Constraint.Length) {
                Constraint.Length constraint = (Constraint.Length) constraints.get(i);
                if (heightUtilized + constraint.value() > innerArea.height()) {
                    moreBelow = true;
                    break;
                }
                heightUtilized += constraint.value();
            } else {
                heightUtilized += 1;
            }
            visibleChildren.add(children.get(i));
        }

        if (moreBelow && scrollDownIndicator != null) {
            if (heightUtilized == innerArea.height()) {
                // remove last child to fit indicator
                visibleChildren.remove(visibleChildren.size() - 1);
            }
            Rect bottomIndicatorArea = new Rect(innerArea.x(), innerArea.y() + innerArea.height() - 1, innerArea.width(), 1);
            innerArea = new Rect(innerArea.x(), innerArea.y(), innerArea.width(), innerArea.height() - 1);
            scrollDownIndicator.render(frame, bottomIndicatorArea, context);
        }

        state.contentLength(children.size()).viewportContentLength(visibleChildren.size());

        frame.renderStatefulWidget(scrollbar, scrollbarArea, state);

        Layout layout = Layout.vertical().constraints(constraints.toArray(new Constraint[0]));
        List<Rect> areas = layout.split(innerArea);

        // Render children
        for (int i = 0; i < visibleChildren.size() && i < areas.size(); i++) {
            Element child = visibleChildren.get(i);
            Rect childArea = areas.get(i);
            context.renderChild(child, frame, childArea);
        }
    }
}
