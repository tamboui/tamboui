/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

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
import dev.tamboui.toolkit.element.StyledElement;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.toolkit.focus.FocusManager;
import dev.tamboui.toolkit.id.IdGenerator;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.MouseEvent;
import dev.tamboui.tui.event.MouseEventKind;
import dev.tamboui.widgets.scrollbar.Scrollbar;
import dev.tamboui.widgets.scrollbar.ScrollbarOrientation;
import dev.tamboui.widgets.scrollbar.ScrollbarState;

/**
 * A scrollable container element with a built-in vertical scrollbar and focus-aware scrolling.
 * <p>
 * Each child counts as one scrollable unit; children will not be split across page boundaries.
 * The scrollbar automatically adjusts based on the number of children and the visible viewport.
 * When focusable children are present, focus navigation (Tab/Shift-Tab and Up/Down arrows)
 * moves focus between children and automatically scrolls the viewport to keep the focused child
 * in view. Non-focusable content continues to scroll directly with Up/Down arrows.
 * Mouse wheel and Page Up/Down keys always scroll the viewport independently.
 *
 * <h2>Keyboard Bindings</h2>
 * <ul>
 *   <li>Up / Down — navigate focus between focusable children, or scroll one item for non-focusable content</li>
 *   <li>Page Up / Page Down — scroll one page</li>
 * </ul>
 *
 * <h2>Mouse Bindings</h2>
 * <ul>
 *   <li>Scroll wheel up/down — scroll one item</li>
 * </ul>
 */
public final class ScrollableElement extends ContainerElement<ScrollableElement> {

    private final Scrollbar scrollbar;
    private final ScrollbarState state;
    private Element scrollUpIndicator = null;
    private Element scrollDownIndicator = null;

    private String lastFocusedId = null;
    private int focusedChildIndex = -1;
    private int pendingScrollIntoView = -1;
    private int lastViewportHeight = 0;
    private List<Constraint> lastConstraints = Collections.emptyList();

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
    }

    @Override
    public EventResult handleKeyEvent(KeyEvent event, boolean focused) {
        // Page Up and Page Down scroll the viewport directly
        if (event.isPageUp()) {
            this.state.pageUp();
            return EventResult.HANDLED;
        } else if (event.isPageDown()) {
            this.state.pageDown();
            return EventResult.HANDLED;
        }

        // Custom key handler runs first if explicitly set
        if (this.keyHandler != null) {
            EventResult result = this.keyHandler.handle(event);
            if (result.isHandled() || result == EventResult.FOCUS_NEXT || result == EventResult.FOCUS_PREVIOUS) {
                return result;
            }
        }

        // Defer to focused child first if one exists
        Element focusedChild = (focusedChildIndex >= 0 && focusedChildIndex < children.size())
                ? children.get(focusedChildIndex)
                : null;
        if (focusedChild != null) {
            EventResult result = focusedChild.handleKeyEvent(event, true);
            if (result.isHandled() || result == EventResult.FOCUS_NEXT || result == EventResult.FOCUS_PREVIOUS) {
                return result;
            }
        }

        // Forward to children if container itself is focused
        if (focused && focusedChild == null) {
            for (Element child : children) {
                EventResult result = child.handleKeyEvent(event, focused);
                if (result.isHandled() || result == EventResult.FOCUS_NEXT || result == EventResult.FOCUS_PREVIOUS) {
                    return result;
                }
            }
        }

        // Arrow navigation / scrolling
        if (event.isUp()) {
            if (focusedChild != null) {
                return EventResult.FOCUS_PREVIOUS;
            } else if (!hasAnyFocusableChildren()) {
                this.state.prev();
                return EventResult.HANDLED;
            } else {
                return EventResult.FOCUS_PREVIOUS;
            }
        } else if (event.isDown()) {
            if (focusedChild != null) {
                return EventResult.FOCUS_NEXT;
            } else if (!hasAnyFocusableChildren()) {
                this.state.next();
                return EventResult.HANDLED;
            } else {
                return EventResult.FOCUS_NEXT;
            }
        }

        return EventResult.UNHANDLED;
    }

    /**
     * Scrolls the viewport so that the child element at the specified index is visible.
     *
     * @param targetIndex the index of the child to scroll into view
     * @return this element for chaining
     */
    public ScrollableElement scrollIntoView(int targetIndex) {
        if (targetIndex < 0 || targetIndex >= children.size()) {
            return this;
        }
        if (lastViewportHeight > 0) {
            adjustScrollIntoView(targetIndex, lastViewportHeight, lastConstraints);
        } else {
            this.pendingScrollIntoView = targetIndex;
            if (targetIndex < state.position()) {
                state.position(targetIndex);
            }
        }
        return this;
    }

    /**
     * Scrolls the viewport so that the specified child element is visible.
     *
     * @param child the child element to scroll into view
     * @return this element for chaining
     */
    public ScrollableElement scrollIntoView(Element child) {
        int index = children.indexOf(child);
        if (index >= 0) {
            scrollIntoView(index);
        }
        return this;
    }

    private void adjustScrollIntoView(int targetIndex, int viewportHeight, List<Constraint> constraints) {
        if (targetIndex < 0 || targetIndex >= children.size()) {
            return;
        }
        if (targetIndex < state.position()) {
            // Target is above current viewport: scroll up so it's at the top
            state.position(targetIndex);
            return;
        }

        // Check if target is already fully visible from current state.position()
        int effectiveHeight = viewportHeight;
        if (scrollUpIndicator != null && state.position() > 0) {
            effectiveHeight = Math.max(1, effectiveHeight - 1);
        }
        if (scrollDownIndicator != null) {
            effectiveHeight = Math.max(1, effectiveHeight - 1);
        }

        int heightSum = 0;
        boolean visible = false;
        for (int i = state.position(); i <= targetIndex; i++) {
            int h = getChildHeight(constraints, i);
            if (i == targetIndex) {
                if (heightSum + h <= viewportHeight) {
                    visible = true;
                }
                break;
            }
            heightSum += h;
            if (heightSum >= viewportHeight) {
                break;
            }
        }

        if (visible) {
            return; // Already in view!
        }

        // Target is below current viewport: scroll down so target is at the bottom of the viewport
        int used = getChildHeight(constraints, targetIndex);
        int newPos = targetIndex;
        while (newPos > 0) {
            int prevHeight = getChildHeight(constraints, newPos - 1);
            if (used + prevHeight > effectiveHeight) {
                break;
            }
            used += prevHeight;
            newPos--;
        }
        state.position(newPos);
    }

    private int getChildHeight(List<Constraint> constraints, int index) {
        if (constraints != null && index >= 0 && index < constraints.size()) {
            Constraint c = constraints.get(index);
            if (c instanceof Constraint.Length) {
                return ((Constraint.Length) c).value();
            }
        }
        return 1;
    }

    /**
     * Returns the internal {@link ScrollbarState}.
     *
     * @return the scrollbar state
     */
    public ScrollbarState state() {
        return this.state;
    }

    /**
     * Sets an indicator element (height 1) displayed at the top of the area when there
     * are items above the current view.
     *
     * @param scrollUpIndicator the indicator element
     * @return this element for chaining
     */
    public ScrollableElement scrollUpIndicator(Element scrollUpIndicator) {
        this.scrollUpIndicator = scrollUpIndicator;
        return this;
    }

    /**
     * Sets an indicator element (height 1) displayed at the bottom of the area when there
     * are items below the current view.
     *
     * @param scrollDownIndicator the indicator element
     * @return this element for chaining
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

        // Ensure IDs exist for focusable children
        for (Element child : children) {
            ensureIds(child);
        }

        // Register focusables in FocusManager so tab order and focus navigation include all items
        if (context != null) {
            FocusManager fm = context.focusManager();
            if (fm != null) {
                for (Element child : children) {
                    registerFocusables(child, fm);
                }
            }
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

        this.lastViewportHeight = innerArea.height();
        this.lastConstraints = constraints;

        // Focus detection and auto-scroll
        String currentFocusedId = findFocusedElementId(context);
        int currentFocusedChildIndex = findFocusedChildIndex(context);

        if (pendingScrollIntoView >= 0) {
            adjustScrollIntoView(pendingScrollIntoView, innerArea.height(), constraints);
            pendingScrollIntoView = -1;
        } else if (currentFocusedChildIndex >= 0 && currentFocusedId != null) {
            boolean focusChanged = !Objects.equals(currentFocusedId, lastFocusedId);
            if (focusChanged) {
                adjustScrollIntoView(currentFocusedChildIndex, innerArea.height(), constraints);
            }
        }

        this.lastFocusedId = currentFocusedId;
        this.focusedChildIndex = currentFocusedChildIndex;

        int totalRequiredHeight = 0;
        for (Constraint c : constraints) {
            totalRequiredHeight += (c instanceof Constraint.Length)
                    ? ((Constraint.Length) c).value()
                    : 1;
        }

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

        List<Constraint> visibleConstraints = new ArrayList<>(visibleChildren.size());
        for (Element child : visibleChildren) {
            int childIdx = children.indexOf(child);
            visibleConstraints.add(constraints.get(childIdx));
        }
        Layout layout = Layout.vertical().constraints(visibleConstraints.toArray(new Constraint[0]));
        List<Rect> areas = layout.split(innerArea);

        // Render children
        for (int i = 0; i < visibleChildren.size() && i < areas.size(); i++) {
            Element child = visibleChildren.get(i);
            Rect childArea = areas.get(i);
            context.renderChild(child, frame, childArea);
        }
    }

    private void ensureIds(Element element) {
        if (element.isFocusable() && element instanceof StyledElement && element.id() == null) {
            ((StyledElement<?>) element).id(IdGenerator.newId(element));
        }
        if (element instanceof ContainerElement) {
            for (Element child : ((ContainerElement<?>) element).children()) {
                ensureIds(child);
            }
        }
    }

    private void registerFocusables(Element element, FocusManager focusManager) {
        if (element.isFocusable()) {
            String id = element.id();
            if (id != null) {
                focusManager.registerFocusable(id, new Rect(-1, -1, 0, 0));
            }
        }
        if (element instanceof ContainerElement) {
            for (Element child : ((ContainerElement<?>) element).children()) {
                registerFocusables(child, focusManager);
            }
        }
    }

    private String findFocusedElementId(RenderContext context) {
        String focusedId = context != null ? context.focusedId() : null;
        if (focusedId == null) {
            return null;
        }
        for (Element child : children) {
            if (containsElementId(child, focusedId)) {
                return focusedId;
            }
        }
        return null;
    }

    private int findFocusedChildIndex(RenderContext context) {
        if (context == null) {
            return -1;
        }
        for (int i = 0; i < children.size(); i++) {
            if (isChildOrDescendantFocused(children.get(i), context)) {
                return i;
            }
        }
        return -1;
    }

    private boolean isChildOrDescendantFocused(Element element, RenderContext context) {
        String id = element.id();
        if (id != null && context.isFocused(id)) {
            return true;
        }
        if (element instanceof ContainerElement) {
            for (Element child : ((ContainerElement<?>) element).children()) {
                if (isChildOrDescendantFocused(child, context)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean containsElementId(Element element, String targetId) {
        if (targetId.equals(element.id())) {
            return true;
        }
        if (element instanceof ContainerElement) {
            for (Element child : ((ContainerElement<?>) element).children()) {
                if (containsElementId(child, targetId)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasAnyFocusableChildren() {
        for (Element child : children) {
            if (isOrContainsFocusable(child)) {
                return true;
            }
        }
        return false;
    }

    private boolean isOrContainsFocusable(Element element) {
        if (element.isFocusable()) {
            return true;
        }
        if (element instanceof ContainerElement) {
            for (Element child : ((ContainerElement<?>) element).children()) {
                if (isOrContainsFocusable(child)) {
                    return true;
                }
            }
        }
        return false;
    }
}
