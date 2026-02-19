/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Style;
import dev.tamboui.style.StylePropertyResolver;
import dev.tamboui.text.CharWidth;
import dev.tamboui.text.Line;
import dev.tamboui.widget.StatefulWidget;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.common.SizedWidget;
import dev.tamboui.widgets.scrollbar.Scrollbar;
import dev.tamboui.widgets.scrollbar.ScrollbarOrientation;
import dev.tamboui.widgets.scrollbar.ScrollbarState;

/**
 * A widget for displaying hierarchical tree data.
 * <p>
 * TreeWidget renders a tree structure with customizable node content,
 * guide characters, and selection highlighting. It supports:
 * <ul>
 *   <li>Arbitrary widget content for each node via {@link SizedWidget}</li>
 *   <li>Multiple data access patterns via {@link TreeModel}</li>
 *   <li>Configurable guide styles (Unicode, ASCII, none)</li>
 *   <li>Scrolling with optional scrollbar</li>
 *   <li>Selection highlighting</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <pre>{@code
 * // Option 1: Using TreeNode (simplest)
 * TreeWidget.<String>builder()
 *     .roots(TreeNode.of("Root",
 *         TreeNode.of("Child 1"),
 *         TreeNode.of("Child 2")).expanded())
 *     .simpleNodeRenderer(node -> Text.from(node.label()))
 *     .build();
 *
 * // Option 2: Functional with domain objects
 * TreeWidget.<File>builder()
 *     .roots(Collections.singletonList(rootDir))
 *     .children(dir -> Arrays.asList(dir.listFiles()))
 *     .isLeaf(File::isFile)
 *     .simpleNodeRenderer(file -> Text.from(file.getName()))
 *     .build();
 *
 * // Option 3: TreeModel interface (for complex/lazy trees)
 * TreeWidget.<File>builder()
 *     .model(new LazyFileTreeModel(rootDir))
 *     .nodeRenderer(file -> SizedWidget.ofHeight(createFileWidget(file), 2))
 *     .build();
 * }</pre>
 *
 * @param <T> the type of data in the tree nodes
 */
public final class TreeWidget<T> implements StatefulWidget<TreeState> {

    private final TreeModel<T> model;
    private final List<T> roots;
    private final Function<T, SizedWidget> nodeRenderer;
    private final GuideStyle guideStyle;
    private final String leafIndicator;
    private final Style style;
    private final Style highlightStyle;
    private final Line highlightSymbol;
    private final Block block;
    private final boolean showScrollbar;
    private final Style scrollbarThumbStyle;
    private final Style scrollbarTrackStyle;
    private final int indentWidth;

    // Cached flat entries from last render (for state access)
    private List<FlatEntry<T>> lastFlatEntries = Collections.emptyList();

    private TreeWidget(Builder<T> builder) {
        this.model = builder.effectiveModel();
        this.roots = builder.roots != null ? new ArrayList<>(builder.roots) : Collections.emptyList();
        this.nodeRenderer = Objects.requireNonNull(builder.nodeRenderer, "nodeRenderer is required");
        this.guideStyle = builder.guideStyle;
        this.leafIndicator = builder.leafIndicator;
        this.style = builder.style;
        this.highlightStyle = builder.highlightStyle;
        this.highlightSymbol = builder.highlightSymbol;
        this.block = builder.block;
        this.showScrollbar = builder.showScrollbar;
        this.scrollbarThumbStyle = builder.scrollbarThumbStyle;
        this.scrollbarTrackStyle = builder.scrollbarTrackStyle;
        this.indentWidth = builder.indentWidth;
    }

    /**
     * Creates a new builder for TreeWidget.
     *
     * @param <T> the node data type
     * @return a new builder
     */
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    /**
     * Returns the last rendered flat entries (for external state access).
     *
     * @return the flattened entries from the last render
     */
    public List<FlatEntry<T>> lastFlatEntries() {
        return lastFlatEntries;
    }

    @Override
    public void render(Rect area, Buffer buffer, TreeState state) {
        if (area.isEmpty()) {
            return;
        }

        // Apply background style
        buffer.setStyle(area, style);

        // Render block if present
        Rect treeArea = area;
        if (block != null) {
            block.render(area, buffer);
            treeArea = block.inner(area);
        }

        if (treeArea.isEmpty()) {
            return;
        }

        // Flatten the visible tree
        List<FlatEntry<T>> flatEntries = flattenTree();
        this.lastFlatEntries = flatEntries;

        int totalItems = flatEntries.size();
        if (totalItems == 0) {
            return;
        }

        // Clamp selection
        int selectedIndex = Math.max(0, Math.min(state.selected(), totalItems - 1));
        state.select(selectedIndex);

        int visibleHeight = treeArea.height();
        int symbolWidth = highlightSymbol.width();
        int contentWidth = treeArea.width() - symbolWidth;

        // Compute heights and cumulative positions
        int totalContentHeight = 0;
        for (FlatEntry<T> entry : flatEntries) {
            entry.cumulativeTop = totalContentHeight;
            entry.height = computeEntryHeight(entry, contentWidth);
            totalContentHeight += entry.height;
        }

        // Adjust for scrollbar if needed
        boolean needsScrollbar = showScrollbar && totalContentHeight > visibleHeight;
        if (needsScrollbar) {
            contentWidth -= 1;
            // Recompute heights
            totalContentHeight = 0;
            for (FlatEntry<T> entry : flatEntries) {
                entry.cumulativeTop = totalContentHeight;
                entry.height = computeEntryHeight(entry, contentWidth);
                totalContentHeight += entry.height;
            }
        }

        if (contentWidth <= 0) {
            return;
        }

        // Auto-scroll to keep selected item visible
        FlatEntry<T> selectedEntry = flatEntries.get(selectedIndex);
        state.scrollToSelected(selectedEntry.cumulativeTop, selectedEntry.height, visibleHeight, totalContentHeight);
        int scrollOffset = state.offset();

        // Render visible entries
        int contentX = treeArea.left() + symbolWidth;

        for (int entryIndex = 0; entryIndex < totalItems; entryIndex++) {
            FlatEntry<T> entry = flatEntries.get(entryIndex);

            // Skip entries completely above viewport
            if (entry.cumulativeTop + entry.height <= scrollOffset) {
                continue;
            }

            // Stop if past viewport
            if (entry.cumulativeTop >= scrollOffset + visibleHeight) {
                break;
            }

            int entryY = treeArea.top() + (entry.cumulativeTop - scrollOffset);
            boolean isSelected = (entryIndex == selectedIndex);

            // Draw highlight symbol (on first visible line only)
            if (isSelected && symbolWidth > 0 && entryY >= treeArea.top() && entryY < treeArea.top() + visibleHeight) {
                buffer.setLine(treeArea.left(), entryY, highlightSymbol.patchStyle(highlightStyle));
            }

            // Build prefix (guide characters)
            String prefix = buildPrefix(entry);
            int prefixWidth = CharWidth.of(prefix);

            // Draw expand indicator
            String indicator;
            if (!model.isLeaf(entry.node)) {
                indicator = model.isExpanded(entry.node) ? "\u25bc " : "\u25b6 "; // ▼ / ▶
            } else {
                indicator = leafIndicator;
            }
            int indicatorWidth = CharWidth.of(indicator);

            // Draw prefix and indicator on first line
            if (entryY >= treeArea.top() && entryY < treeArea.top() + visibleHeight) {
                Style lineStyle = isSelected ? style.patch(highlightStyle) : style;

                if (!prefix.isEmpty()) {
                    buffer.setString(contentX, entryY, CharWidth.substringByWidth(prefix, contentWidth), lineStyle);
                }

                int indicatorX = contentX + prefixWidth;
                if (indicatorX < contentX + contentWidth) {
                    buffer.setString(indicatorX, entryY, indicator, lineStyle);
                }
            }

            // Draw node content
            int nodeX = contentX + prefixWidth + indicatorWidth;
            int nodeWidth = contentWidth - prefixWidth - indicatorWidth;
            if (nodeWidth > 0) {
                SizedWidget sized = nodeRenderer.apply(entry.node);
                int nodeY = Math.max(entryY, treeArea.top());
                int nodeHeight = Math.min(entry.height, treeArea.top() + visibleHeight - nodeY);

                if (nodeHeight > 0) {
                    Rect nodeArea = new Rect(nodeX, nodeY, nodeWidth, nodeHeight);
                    sized.widget().render(nodeArea, buffer);

                    // Apply highlight style over node area if selected
                    if (isSelected) {
                        buffer.setStyle(nodeArea, highlightStyle);
                    }
                }
            }
        }

        // Render scrollbar
        if (needsScrollbar && totalContentHeight > 0) {
            Rect scrollbarArea = new Rect(
                    treeArea.right() - 1,
                    treeArea.top(),
                    1,
                    treeArea.height()
            );

            ScrollbarState scrollbarState = new ScrollbarState()
                    .contentLength(totalContentHeight)
                    .viewportContentLength(visibleHeight)
                    .position(scrollOffset);

            Scrollbar.Builder scrollbarBuilder = Scrollbar.builder()
                    .orientation(ScrollbarOrientation.VERTICAL_RIGHT);
            if (scrollbarThumbStyle != null && !scrollbarThumbStyle.equals(Style.EMPTY)) {
                scrollbarBuilder.thumbStyle(scrollbarThumbStyle);
            }
            if (scrollbarTrackStyle != null && !scrollbarTrackStyle.equals(Style.EMPTY)) {
                scrollbarBuilder.trackStyle(scrollbarTrackStyle);
            }
            scrollbarBuilder.build().render(scrollbarArea, buffer, scrollbarState);
        }
    }

    private int computeEntryHeight(FlatEntry<T> entry, int contentWidth) {
        String prefix = buildPrefix(entry);
        int prefixWidth = CharWidth.of(prefix);
        int indicatorWidth = 2;
        int nodeWidth = contentWidth - prefixWidth - indicatorWidth;

        if (nodeWidth <= 0) {
            return 1;
        }

        SizedWidget sized = nodeRenderer.apply(entry.node);
        return sized.heightOr(1);
    }

    private List<FlatEntry<T>> flattenTree() {
        List<FlatEntry<T>> entries = new ArrayList<>();
        List<T> effectiveRoots = getEffectiveRoots();

        for (int i = 0; i < effectiveRoots.size(); i++) {
            boolean isLastRoot = (i == effectiveRoots.size() - 1);
            flattenNode(effectiveRoots.get(i), null, 0, new ArrayList<>(), isLastRoot, entries);
        }

        return entries;
    }

    private List<T> getEffectiveRoots() {
        if (!roots.isEmpty()) {
            return roots;
        }
        if (model != null) {
            return Collections.singletonList(model.root());
        }
        return Collections.emptyList();
    }

    private void flattenNode(T node, T parent, int depth,
                             List<Boolean> parentIsLast, boolean isLast,
                             List<FlatEntry<T>> entries) {
        List<Boolean> guides = new ArrayList<>(parentIsLast);
        entries.add(new FlatEntry<>(node, parent, depth, guides, isLast));

        if (model.isExpanded(node) && !model.isLeaf(node)) {
            List<T> children = model.children(node);
            List<Boolean> childParentIsLast = new ArrayList<>(parentIsLast);
            // Only add isLast for depth > 0 (don't track root's isLast)
            if (depth > 0) {
                childParentIsLast.add(isLast);
            }
            for (int i = 0; i < children.size(); i++) {
                boolean childIsLast = (i == children.size() - 1);
                flattenNode(children.get(i), node, depth + 1, childParentIsLast, childIsLast, entries);
            }
        }
    }

    private String buildPrefix(FlatEntry<T> entry) {
        if (entry.depth == 0 || guideStyle == GuideStyle.NONE) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        int effectiveIndent = indentWidth > 0 ? indentWidth : CharWidth.of(guideStyle.branch());

        // Add vertical/space guides for ancestor levels
        for (int i = 0; i < entry.parentIsLast.size(); i++) {
            String guide = entry.parentIsLast.get(i)
                    ? guideStyle.space()
                    : guideStyle.vertical();
            sb.append(padToWidth(guide, effectiveIndent));
        }

        // Add branch connector
        String branch = entry.isLast ? guideStyle.lastBranch() : guideStyle.branch();
        sb.append(padToWidth(branch, effectiveIndent));

        return sb.toString();
    }

    private String padToWidth(String s, int width) {
        int currentWidth = CharWidth.of(s);
        if (currentWidth == width) {
            return s;
        } else if (currentWidth < width) {
            StringBuilder sb = new StringBuilder(s);
            for (int i = currentWidth; i < width; i++) {
                sb.append(' ');
            }
            return sb.toString();
        } else {
            return CharWidth.substringByWidth(s, width);
        }
    }

    /**
     * A flattened entry representing a visible tree node.
     *
     * @param <T> the node data type
     */
    public static final class FlatEntry<T> {
        private final T node;
        private final T parent;
        private final int depth;
        private final List<Boolean> parentIsLast;
        private final boolean isLast;
        private int height = 1;
        private int cumulativeTop = 0;

        FlatEntry(T node, T parent, int depth, List<Boolean> parentIsLast, boolean isLast) {
            this.node = node;
            this.parent = parent;
            this.depth = depth;
            this.parentIsLast = parentIsLast;
            this.isLast = isLast;
        }

        /**
         * Returns the node data.
         *
         * @return the node
         */
        public T node() {
            return node;
        }

        /**
         * Returns the parent node, or null for roots.
         *
         * @return the parent
         */
        public T parent() {
            return parent;
        }

        /**
         * Returns the depth in the tree (0 for roots).
         *
         * @return the depth
         */
        public int depth() {
            return depth;
        }
    }

    /**
     * Builder for {@link TreeWidget}.
     *
     * @param <T> the node data type
     */
    public static final class Builder<T> {

        private TreeModel<T> model;
        private List<T> roots;
        private Function<T, List<T>> childrenFn;
        private Predicate<T> isLeafFn;
        private Function<T, SizedWidget> nodeRenderer;
        private GuideStyle guideStyle = GuideStyle.UNICODE;
        private String leafIndicator = "";
        private Style style = Style.EMPTY;
        private Style highlightStyle = Style.EMPTY.reversed();
        private Line highlightSymbol = Line.from("> ");
        private Block block;
        private boolean showScrollbar;
        private Style scrollbarThumbStyle;
        private Style scrollbarTrackStyle;
        private int indentWidth = -1;
        private StylePropertyResolver styleResolver = StylePropertyResolver.empty();

        // Expansion state customization
        private Predicate<T> isExpandedFn;
        private BiConsumer<T, Boolean> setExpandedFn;

        private Builder() {
        }

        /**
         * Sets the tree model for data access.
         *
         * @param model the tree model
         * @return this builder
         */
        public Builder<T> model(TreeModel<T> model) {
            this.model = model;
            return this;
        }

        /**
         * Sets the root nodes (for functional approach or multiple roots).
         *
         * @param roots the root nodes
         * @return this builder
         */
        @SafeVarargs
        @SuppressWarnings("varargs")
        public final Builder<T> roots(T... roots) {
            this.roots = Arrays.asList(roots);
            return this;
        }

        /**
         * Sets the root nodes from a list.
         *
         * @param roots the root nodes
         * @return this builder
         */
        public Builder<T> roots(List<T> roots) {
            this.roots = new ArrayList<>(roots);
            return this;
        }

        /**
         * Sets the function to get children (functional shortcut).
         *
         * @param childrenFn function that returns children for a node
         * @return this builder
         */
        public Builder<T> children(Function<T, List<T>> childrenFn) {
            this.childrenFn = childrenFn;
            return this;
        }

        /**
         * Sets the predicate to check if a node is a leaf (functional shortcut).
         *
         * @param isLeafFn predicate that returns true for leaf nodes
         * @return this builder
         */
        public Builder<T> isLeaf(Predicate<T> isLeafFn) {
            this.isLeafFn = isLeafFn;
            return this;
        }

        /**
         * Sets a simple node renderer that returns a widget with default size.
         *
         * @param renderer function that creates a widget for a node
         * @return this builder
         */
        public Builder<T> simpleNodeRenderer(Function<T, dev.tamboui.widget.Widget> renderer) {
            this.nodeRenderer = node -> SizedWidget.of(renderer.apply(node));
            return this;
        }

        /**
         * Sets the full node renderer with explicit sizing.
         *
         * @param renderer function that creates a SizedWidget for a node
         * @return this builder
         */
        public Builder<T> nodeRenderer(Function<T, SizedWidget> renderer) {
            this.nodeRenderer = renderer;
            return this;
        }

        /**
         * Sets the guide style for branch characters.
         *
         * @param guideStyle the guide style
         * @return this builder
         */
        public Builder<T> guideStyle(GuideStyle guideStyle) {
            this.guideStyle = guideStyle != null ? guideStyle : GuideStyle.UNICODE;
            return this;
        }

        /**
         * Sets the indicator for leaf nodes.
         *
         * @param indicator the leaf indicator string (default: empty)
         * @return this builder
         */
        public Builder<T> leafIndicator(String indicator) {
            this.leafIndicator = indicator != null ? indicator : "";
            return this;
        }

        /**
         * Sets the base style.
         *
         * @param style the base style
         * @return this builder
         */
        public Builder<T> style(Style style) {
            this.style = style != null ? style : Style.EMPTY;
            return this;
        }

        /**
         * Sets the highlight style for selected items.
         *
         * @param style the highlight style
         * @return this builder
         */
        public Builder<T> highlightStyle(Style style) {
            this.highlightStyle = style != null ? style : Style.EMPTY.reversed();
            return this;
        }

        /**
         * Sets the highlight symbol shown before selected items.
         *
         * @param symbol the highlight symbol
         * @return this builder
         */
        public Builder<T> highlightSymbol(Line symbol) {
            this.highlightSymbol = symbol != null ? symbol : Line.from("");
            return this;
        }

        /**
         * Sets the highlight symbol as a string.
         *
         * @param symbol the highlight symbol
         * @return this builder
         */
        public Builder<T> highlightSymbol(String symbol) {
            this.highlightSymbol = symbol != null ? Line.from(symbol) : Line.from("");
            return this;
        }

        /**
         * Wraps the tree in a block.
         *
         * @param block the block
         * @return this builder
         */
        public Builder<T> block(Block block) {
            this.block = block;
            return this;
        }

        /**
         * Enables the scrollbar.
         *
         * @return this builder
         */
        public Builder<T> scrollbar() {
            this.showScrollbar = true;
            return this;
        }

        /**
         * Sets the scrollbar thumb style.
         *
         * @param style the thumb style
         * @return this builder
         */
        public Builder<T> scrollbarThumbStyle(Style style) {
            this.scrollbarThumbStyle = style;
            return this;
        }

        /**
         * Sets the scrollbar track style.
         *
         * @param style the track style
         * @return this builder
         */
        public Builder<T> scrollbarTrackStyle(Style style) {
            this.scrollbarTrackStyle = style;
            return this;
        }

        /**
         * Sets the indent width per depth level.
         *
         * @param width the indent width
         * @return this builder
         */
        public Builder<T> indentWidth(int width) {
            this.indentWidth = Math.max(-1, width);
            return this;
        }

        /**
         * Sets the style property resolver.
         *
         * @param resolver the resolver
         * @return this builder
         */
        public Builder<T> styleResolver(StylePropertyResolver resolver) {
            this.styleResolver = resolver != null ? resolver : StylePropertyResolver.empty();
            return this;
        }

        /**
         * Sets custom functions to check and modify expansion state.
         * <p>
         * Use this when your data model manages its own expansion state,
         * such as when using {@link TreeNode} which has built-in expansion tracking.
         *
         * @param isExpanded predicate to check if a node is expanded
         * @param setExpanded consumer to set a node's expanded state
         * @return this builder
         */
        public Builder<T> expansionState(Predicate<T> isExpanded, BiConsumer<T, Boolean> setExpanded) {
            this.isExpandedFn = isExpanded;
            this.setExpandedFn = setExpanded;
            return this;
        }

        /**
         * Builds the TreeWidget.
         *
         * @return a new TreeWidget
         */
        public TreeWidget<T> build() {
            return new TreeWidget<>(this);
        }

        private TreeModel<T> effectiveModel() {
            if (model != null) {
                return model;
            }

            // Build functional model
            return new FunctionalTreeModel<>(childrenFn, isLeafFn, isExpandedFn, setExpandedFn);
        }
    }

    /**
     * A TreeModel implementation based on functions.
     */
    private static final class FunctionalTreeModel<T> implements TreeModel<T> {
        private final Function<T, List<T>> childrenFn;
        private final Predicate<T> isLeafFn;
        private final Predicate<T> isExpandedFn;
        private final BiConsumer<T, Boolean> setExpandedFn;
        private final Set<T> expandedNodes = new HashSet<>();

        FunctionalTreeModel(Function<T, List<T>> childrenFn, Predicate<T> isLeafFn,
                           Predicate<T> isExpandedFn, BiConsumer<T, Boolean> setExpandedFn) {
            this.childrenFn = childrenFn;
            this.isLeafFn = isLeafFn;
            this.isExpandedFn = isExpandedFn;
            this.setExpandedFn = setExpandedFn;
        }

        @Override
        public T root() {
            return null; // Roots are provided separately
        }

        @Override
        public List<T> children(T parent) {
            if (childrenFn == null) {
                return Collections.emptyList();
            }
            List<T> children = childrenFn.apply(parent);
            return children != null ? children : Collections.emptyList();
        }

        @Override
        public boolean isLeaf(T node) {
            if (isLeafFn != null) {
                return isLeafFn.test(node);
            }
            List<T> children = children(node);
            return children.isEmpty();
        }

        @Override
        public boolean isExpanded(T node) {
            if (isExpandedFn != null) {
                return isExpandedFn.test(node);
            }
            return expandedNodes.contains(node);
        }

        @Override
        public void setExpanded(T node, boolean expanded) {
            if (setExpandedFn != null) {
                setExpandedFn.accept(node, expanded);
            } else {
                if (expanded) {
                    expandedNodes.add(node);
                } else {
                    expandedNodes.remove(node);
                }
            }
        }
    }
}
