/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import dev.tamboui.css.cascade.CssStyleResolver;
import dev.tamboui.css.cascade.PseudoClassState;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.IntegerConverter;
import dev.tamboui.style.PropertyConverter;
import dev.tamboui.style.PropertyDefinition;
import dev.tamboui.style.PropertyRegistry;
import dev.tamboui.style.StringConverter;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.element.Size;
import dev.tamboui.toolkit.element.StyledElement;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.bindings.Actions;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.MouseEvent;
import dev.tamboui.tui.event.MouseEventKind;
import dev.tamboui.widget.Widget;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.widgets.block.Title;
import dev.tamboui.widgets.common.ScrollBarPolicy;
import dev.tamboui.widgets.common.SizedWidget;
import dev.tamboui.widgets.tree.GuideStyle;
import dev.tamboui.widgets.tree.TreeNode;
import dev.tamboui.widgets.tree.TreeState;
import dev.tamboui.widgets.tree.TreeWidget;

/**
 * A scrollable, keyboard-navigable hierarchical tree view.
 * <p>
 * The tree flattens visible nodes (only expanded branches) into a list
 * for rendering, with guide characters showing the hierarchy.
 *
 * <pre>{@code
 * tree(
 *     TreeNode.of("src",
 *         TreeNode.of("main"),
 *         TreeNode.of("test")
 *     ).expanded(),
 *     TreeNode.of("README.md").leaf()
 * ).title("Project")
 *  .rounded()
 *  .highlightColor(Color.CYAN)
 * }</pre>
 *
 * <h2>CSS Child Selectors</h2>
 * <ul>
 *   <li>{@code TreeElement-node} - styles each tree node</li>
 *   <li>{@code TreeElement-node:selected} - styles the selected node</li>
 *   <li>{@code TreeElement-guide} - styles the guide/branch characters</li>
 *   <li>{@code TreeElement-scrollbar-thumb} - styles the scrollbar thumb</li>
 *   <li>{@code TreeElement-scrollbar-track} - styles the scrollbar track</li>
 * </ul>
 *
 * <h2>CSS Properties</h2>
 * <ul>
 *   <li>{@code guide-style} - guide character style: "unicode", "ascii", "none"</li>
 *   <li>{@code scrollbar-policy} - when to show scrollbar: "none", "always", "as-needed"</li>
 *   <li>{@code highlight-symbol} - symbol shown before selected item (default: "&gt; ")</li>
 *   <li>{@code indent-width} - space per depth level in cells</li>
 * </ul>
 *
 * <h2>Keyboard Navigation</h2>
 * <ul>
 *   <li>Up/Down - move selection</li>
 *   <li>Right - expand node or move to first child</li>
 *   <li>Left - collapse node or move to parent</li>
 *   <li>Enter/Space - toggle expand/collapse</li>
 *   <li>Home/End - first/last visible node</li>
 *   <li>Page Up/Down - scroll by viewport height</li>
 * </ul>
 *
 * @param <T> the type of data associated with tree nodes
 * @see TreeNode
 */
public final class TreeElement<T> extends StyledElement<TreeElement<T>> {

    // ═══════════════════════════════════════════════════════════════
    // CSS Property Definitions
    // ═══════════════════════════════════════════════════════════════

    private static final Style DEFAULT_HIGHLIGHT_STYLE = Style.EMPTY.reversed();
    private static final String DEFAULT_HIGHLIGHT_SYMBOL = "> ";

    private static final PropertyConverter<GuideStyle> GUIDE_STYLE_CONVERTER = value -> {
        if (value == null || value.trim().isEmpty()) {
            return Optional.empty();
        }
        String normalized = value.trim().toLowerCase();
        for (GuideStyle style : GuideStyle.values()) {
            if (style.name().toLowerCase().replace('_', '-').equals(normalized)) {
                return Optional.of(style);
            }
        }
        return Optional.empty();
    };

    /**
     * CSS property for guide style. Values: "unicode", "ascii", "none".
     */
    public static final PropertyDefinition<GuideStyle> GUIDE_STYLE =
            PropertyDefinition.builder("guide-style", GUIDE_STYLE_CONVERTER)
                    .defaultValue(GuideStyle.UNICODE)
                    .build();

    /**
     * CSS property for scrollbar policy. Values: "none", "always", "as-needed".
     */
    public static final PropertyDefinition<ScrollBarPolicy> SCROLLBAR_POLICY =
            PropertyDefinition.builder("scrollbar-policy", ScrollBarPolicy.CONVERTER)
                    .defaultValue(ScrollBarPolicy.NONE)
                    .build();

    /**
     * CSS property for highlight symbol shown before selected item.
     */
    public static final PropertyDefinition<String> HIGHLIGHT_SYMBOL =
            PropertyDefinition.builder("highlight-symbol", StringConverter.INSTANCE)
                    .defaultValue(DEFAULT_HIGHLIGHT_SYMBOL)
                    .build();

    /**
     * CSS property for indent width (space per depth level).
     */
    public static final PropertyDefinition<Integer> INDENT_WIDTH =
            PropertyDefinition.of("indent-width", IntegerConverter.INSTANCE);

    static {
        PropertyRegistry.registerAll(
                GUIDE_STYLE,
                SCROLLBAR_POLICY,
                HIGHLIGHT_SYMBOL,
                INDENT_WIDTH
        );
    }

    private final List<TreeNode<T>> roots = new ArrayList<>();
    private Function<TreeNode<T>, StyledElement<?>> nodeRenderer;
    private GuideStyle guideStyle = GuideStyle.UNICODE;
    private Style highlightStyle;
    private String highlightSymbol;
    private String title;
    private BorderType borderType;
    private Color borderColor;
    private ScrollBarPolicy scrollBarPolicy = ScrollBarPolicy.NONE;
    private Color scrollbarThumbColor;
    private Color scrollbarTrackColor;
    private int indentWidth = -1; // -1 means use guide style width

    // TreeState for widget delegation
    private final TreeState treeState = new TreeState();

    // Cached flat entries from last render (for navigation)
    private List<TreeWidget.FlatEntry<TreeNode<T>>> lastFlatEntries = Collections.emptyList();
    private int lastViewportHeight;

    /**
     * Creates an empty tree element.
     */
    public TreeElement() {
    }

    /**
     * Creates a tree element with the given root nodes.
     *
     * @param roots the root nodes
     */
    @SafeVarargs
    @SuppressWarnings("varargs")
    public TreeElement(TreeNode<T>... roots) {
        this.roots.addAll(Arrays.asList(roots));
    }

    /**
     * Sets the root nodes.
     *
     * @param roots the root nodes
     * @return this element for chaining
     */
    @SafeVarargs
    @SuppressWarnings("varargs")
    public final TreeElement<T> roots(TreeNode<T>... roots) {
        this.roots.clear();
        this.roots.addAll(Arrays.asList(roots));
        return this;
    }

    /**
     * Adds a root node.
     *
     * @param node the root node to add
     * @return this element for chaining
     */
    public TreeElement<T> add(TreeNode<T> node) {
        this.roots.add(node);
        return this;
    }

    /**
     * Sets a custom renderer for tree nodes.
     * <p>
     * When set, each node is rendered using the provided function instead
     * of the default label text. This allows for rich node content including
     * icons, colored text, progress bars, or any other styled elements.
     * <p>
     * Example:
     * <pre>{@code
     * tree(...)
     *     .nodeRenderer(node -> row(
     *         text(node.isLeaf() ? "📄 " : "📁 "),
     *         text(node.label()).bold(),
     *         spacer(),
     *         text(formatSize(node.data())).dim()
     *     ))
     * }</pre>
     *
     * @param renderer the function that converts a tree node to a styled element
     * @return this element for chaining
     */
    public TreeElement<T> nodeRenderer(Function<TreeNode<T>, StyledElement<?>> renderer) {
        this.nodeRenderer = renderer;
        return this;
    }

    /**
     * Sets the guide style for tree branch characters.
     *
     * @param guideStyle the guide style
     * @return this element for chaining
     */
    public TreeElement<T> guideStyle(GuideStyle guideStyle) {
        this.guideStyle = guideStyle != null ? guideStyle : GuideStyle.UNICODE;
        return this;
    }

    /**
     * Sets the highlight style for the selected node.
     *
     * @param style the highlight style
     * @return this element for chaining
     */
    public TreeElement<T> highlightStyle(Style style) {
        this.highlightStyle = style;
        return this;
    }

    /**
     * Sets the highlight color for the selected node.
     *
     * @param color the highlight color
     * @return this element for chaining
     */
    public TreeElement<T> highlightColor(Color color) {
        this.highlightStyle = Style.EMPTY.fg(color).bold();
        return this;
    }

    /**
     * Sets the symbol displayed before the selected node.
     *
     * @param symbol the highlight symbol
     * @return this element for chaining
     */
    public TreeElement<T> highlightSymbol(String symbol) {
        this.highlightSymbol = symbol;
        return this;
    }

    /**
     * Sets the title.
     *
     * @param title the tree title
     * @return this element for chaining
     */
    public TreeElement<T> title(String title) {
        this.title = title;
        return this;
    }

    /**
     * Uses rounded borders.
     *
     * @return this element for chaining
     */
    public TreeElement<T> rounded() {
        this.borderType = BorderType.ROUNDED;
        return this;
    }

    /**
     * Sets the border color.
     *
     * @param color the border color
     * @return this element for chaining
     */
    public TreeElement<T> borderColor(Color color) {
        this.borderColor = color;
        return this;
    }

    /**
     * Enables showing a scrollbar (always visible).
     *
     * @return this element for chaining
     */
    public TreeElement<T> scrollbar() {
        this.scrollBarPolicy = ScrollBarPolicy.ALWAYS;
        return this;
    }

    /**
     * Sets the scrollbar policy.
     *
     * @param policy the scrollbar display policy
     * @return this element for chaining
     */
    public TreeElement<T> scrollbar(ScrollBarPolicy policy) {
        this.scrollBarPolicy = policy != null ? policy : ScrollBarPolicy.NONE;
        return this;
    }

    /**
     * Sets the scrollbar thumb color.
     *
     * @param color the thumb color
     * @return this element for chaining
     */
    public TreeElement<T> scrollbarThumbColor(Color color) {
        this.scrollbarThumbColor = color;
        return this;
    }

    /**
     * Sets the scrollbar track color.
     *
     * @param color the track color
     * @return this element for chaining
     */
    public TreeElement<T> scrollbarTrackColor(Color color) {
        this.scrollbarTrackColor = color;
        return this;
    }

    /**
     * Sets the indent width per depth level.
     * <p>
     * By default, the guide style determines the indent width.
     *
     * @param width the indent width in characters
     * @return this element for chaining
     */
    public TreeElement<T> indentWidth(int width) {
        this.indentWidth = Math.max(0, width);
        return this;
    }

    /**
     * Sets the selected index in the flattened visible list.
     *
     * @param index the index to select
     * @return this element for chaining
     */
    public TreeElement<T> selected(int index) {
        this.treeState.select(index);
        return this;
    }

    /**
     * Returns the currently selected index in the flattened visible list.
     *
     * @return the selected index
     */
    public int selected() {
        return treeState.selected();
    }

    /**
     * Returns the currently selected tree node, or {@code null} if the tree is empty.
     *
     * @return the selected node, or null
     */
    public TreeNode<T> selectedNode() {
        if (lastFlatEntries.isEmpty()) {
            return null;
        }
        int idx = Math.min(treeState.selected(), lastFlatEntries.size() - 1);
        return lastFlatEntries.get(idx).node();
    }

    // ═══════════════════════════════════════════════════════════════
    // Navigation methods
    // ═══════════════════════════════════════════════════════════════

    /**
     * Selects the previous visible node.
     */
    public void selectPrevious() {
        treeState.selectPrevious();
    }

    /**
     * Selects the next visible node.
     */
    public void selectNext() {
        if (!lastFlatEntries.isEmpty()) {
            treeState.selectNext(lastFlatEntries.size() - 1);
        }
    }

    /**
     * Expands the selected node, or moves to the first child if already expanded.
     */
    public void expandSelected() {
        if (lastFlatEntries.isEmpty()) {
            return;
        }
        int idx = Math.min(treeState.selected(), lastFlatEntries.size() - 1);
        TreeNode<T> node = lastFlatEntries.get(idx).node();
        if (node.isLeaf()) {
            return;
        }
        if (node.isExpanded()) {
            // Move to first child if there are children
            if (!node.children().isEmpty() && idx + 1 < lastFlatEntries.size()) {
                treeState.select(idx + 1);
            }
        } else {
            node.expanded(true);
        }
    }

    /**
     * Collapses the selected node, or moves to its parent if already collapsed.
     */
    public void collapseSelected() {
        if (lastFlatEntries.isEmpty()) {
            return;
        }
        int idx = Math.min(treeState.selected(), lastFlatEntries.size() - 1);
        TreeWidget.FlatEntry<TreeNode<T>> entry = lastFlatEntries.get(idx);
        TreeNode<T> node = entry.node();
        if (node.isExpanded() && !node.isLeaf()) {
            node.expanded(false);
        } else {
            // Move to parent
            TreeNode<T> parent = entry.parent();
            if (parent != null) {
                for (int i = 0; i < lastFlatEntries.size(); i++) {
                    if (lastFlatEntries.get(i).node() == parent) {
                        treeState.select(i);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Toggles the expanded state of the selected node.
     */
    public void toggleSelected() {
        if (lastFlatEntries.isEmpty()) {
            return;
        }
        int idx = Math.min(treeState.selected(), lastFlatEntries.size() - 1);
        TreeNode<T> node = lastFlatEntries.get(idx).node();
        if (!node.isLeaf()) {
            node.toggleExpanded();
        }
    }

    /**
     * Selects the first visible node.
     */
    public void selectFirst() {
        treeState.selectFirst();
    }

    /**
     * Selects the last visible node.
     */
    public void selectLast() {
        if (!lastFlatEntries.isEmpty()) {
            treeState.selectLast(lastFlatEntries.size() - 1);
        }
    }

    @Override
    public Size preferredSize(int availableWidth, int availableHeight, RenderContext context) {
        if (roots.isEmpty()) {
            return Size.of(0, 0);
        }

        // Calculate max label width across all visible nodes
        int maxWidth = 0;
        for (TreeNode<T> root : roots) {
            maxWidth = Math.max(maxWidth, computeMaxLabelWidth(root, 0));
        }

        // Add highlight symbol width
        String effectiveSymbol = highlightSymbol != null ? highlightSymbol : DEFAULT_HIGHLIGHT_SYMBOL;
        maxWidth += effectiveSymbol.length();

        // Add border width if present
        if (title != null || borderType != null) {
            maxWidth += 2;
        }

        // Count visible nodes
        int count = 0;
        for (TreeNode<T> root : roots) {
            count += countVisibleNodes(root);
        }

        // Add border height if present
        if (title != null || borderType != null) {
            count += 2;
        }

        return Size.of(maxWidth, count);
    }

    private int computeMaxLabelWidth(TreeNode<T> node, int depth) {
        int indentW = indentWidth >= 0 ? indentWidth : guideStyle.space().length();
        int labelWidth = node.label() != null ? node.label().length() : 0;
        int width = depth * indentW + labelWidth;

        if (node.isExpanded() && !node.isLeaf()) {
            for (TreeNode<T> child : node.children()) {
                width = Math.max(width, computeMaxLabelWidth(child, depth + 1));
            }
        }
        return width;
    }

    private int countVisibleNodes(TreeNode<T> node) {
        int count = 1;
        if (node.isExpanded() && !node.isLeaf()) {
            for (TreeNode<T> child : node.children()) {
                count += countVisibleNodes(child);
            }
        }
        return count;
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
        if (area.isEmpty() || roots.isEmpty()) {
            if (title != null || borderType != null) {
                renderBorder(frame, area, context);
            }
            return;
        }

        Rect treeArea = renderBorder(frame, area, context);
        if (treeArea.isEmpty()) {
            return;
        }

        this.lastViewportHeight = treeArea.height();

        CssStyleResolver cssResolver = context.resolveStyle(this).orElse(CssStyleResolver.empty());
        TreeWidget<TreeNode<T>> widget = buildTreeWidget(frame, treeArea, context, cssResolver);

        frame.renderStatefulWidget(widget, treeArea, treeState);
        this.lastFlatEntries = widget.lastFlatEntries();
    }

    private TreeWidget<TreeNode<T>> buildTreeWidget(Frame frame, Rect treeArea,
                                                    RenderContext context, CssStyleResolver cssResolver) {
        TreeWidget.Builder<TreeNode<T>> builder = TreeWidget.<TreeNode<T>>builder()
                .roots(roots)
                .children(TreeNode::children)
                .isLeaf(TreeNode::isLeaf)
                .expansionState(TreeNode::isExpanded, TreeNode::expanded);

        configureGuideStyle(builder, cssResolver);
        configureHighlight(builder, context, cssResolver);
        configureIndentWidth(builder, cssResolver);
        configureScrollbar(builder, context, cssResolver);
        configureNodeRenderer(builder, frame, treeArea, context);

        return builder.build();
    }

    private void configureGuideStyle(TreeWidget.Builder<TreeNode<T>> builder, CssStyleResolver cssResolver) {
        GuideStyle effectiveGuideStyle = cssResolver.resolve(GUIDE_STYLE, this.guideStyle);
        builder.guideStyle(effectiveGuideStyle);
    }

    private void configureHighlight(TreeWidget.Builder<TreeNode<T>> builder,
                                    RenderContext context, CssStyleResolver cssResolver) {
        String effectiveHighlightSymbol = cssResolver.resolve(HIGHLIGHT_SYMBOL, this.highlightSymbol);
        if (effectiveHighlightSymbol == null) {
            effectiveHighlightSymbol = DEFAULT_HIGHLIGHT_SYMBOL;
        }
        builder.highlightSymbol(effectiveHighlightSymbol);

        Style effectiveHighlightStyle = resolveEffectiveStyle(
                context, "node", PseudoClassState.ofSelected(),
                highlightStyle, DEFAULT_HIGHLIGHT_STYLE);
        builder.highlightStyle(effectiveHighlightStyle);
    }

    private void configureIndentWidth(TreeWidget.Builder<TreeNode<T>> builder, CssStyleResolver cssResolver) {
        Integer programmaticIndent = this.indentWidth >= 0 ? this.indentWidth : null;
        Integer cssIndent = cssResolver.get(INDENT_WIDTH).orElse(null);

        if (programmaticIndent != null) {
            builder.indentWidth(programmaticIndent);
        } else if (cssIndent != null) {
            builder.indentWidth(cssIndent);
        }
        // Otherwise let widget use its default
    }

    private void configureScrollbar(TreeWidget.Builder<TreeNode<T>> builder,
                                    RenderContext context, CssStyleResolver cssResolver) {
        ScrollBarPolicy effectivePolicy = cssResolver.resolve(SCROLLBAR_POLICY, this.scrollBarPolicy);
        boolean showScrollbar = effectivePolicy == ScrollBarPolicy.ALWAYS
                || effectivePolicy == ScrollBarPolicy.AS_NEEDED;

        if (showScrollbar) {
            builder.scrollbar();

            Style explicitThumbStyle = scrollbarThumbColor != null ? Style.EMPTY.fg(scrollbarThumbColor) : null;
            Style thumbStyle = resolveEffectiveStyle(context, "scrollbar-thumb", explicitThumbStyle, Style.EMPTY);
            if (!thumbStyle.equals(Style.EMPTY)) {
                builder.scrollbarThumbStyle(thumbStyle);
            }

            Style explicitTrackStyle = scrollbarTrackColor != null ? Style.EMPTY.fg(scrollbarTrackColor) : null;
            Style trackStyle = resolveEffectiveStyle(context, "scrollbar-track", explicitTrackStyle, Style.EMPTY);
            if (!trackStyle.equals(Style.EMPTY)) {
                builder.scrollbarTrackStyle(trackStyle);
            }
        }
    }

    private void configureNodeRenderer(TreeWidget.Builder<TreeNode<T>> builder, Frame frame,
                                       Rect treeArea, RenderContext context) {
        if (nodeRenderer != null) {
            builder.nodeRenderer(node -> adaptNodeElement(node, frame, treeArea, context));
        } else {
            builder.simpleNodeRenderer(node -> createLabelWidget(node, context));
        }
    }

    private SizedWidget adaptNodeElement(TreeNode<T> node,
                                         Frame frame,
                                         Rect treeArea,
                                         RenderContext context) {
        StyledElement<?> element = nodeRenderer.apply(node);
        if (element == null) {
            return SizedWidget.of(createLabelWidget(node, context));
        }

        int availableWidth = treeArea.width();
        Size size = element.preferredSize(availableWidth, -1, context);
        int preferredWidth = size.widthOr(0);
        int preferredHeight = size.heightOr(0);

        Widget adapted = createElementAdapter(element, frame, context);

        if (preferredWidth > 0 && preferredHeight > 0) {
            return SizedWidget.of(adapted, preferredWidth, preferredHeight);
        } else if (preferredHeight > 0) {
            return SizedWidget.ofHeight(adapted, preferredHeight);
        } else if (preferredWidth > 0) {
            return SizedWidget.ofWidth(adapted, preferredWidth);
        }
        return SizedWidget.of(adapted);
    }

    /**
     * Creates a simple label widget for a node.
     */
    private Widget createLabelWidget(TreeNode<T> node, RenderContext context) {
        String label = node.label() != null ? node.label() : "";
        return (rect, buffer) -> {
            if (!rect.isEmpty() && !label.isEmpty()) {
                buffer.setString(rect.left(), rect.top(), label, context.currentStyle());
            }
        };
    }

    /**
     * Creates a Widget adapter for a StyledElement.
     */
    private Widget createElementAdapter(StyledElement<?> element, Frame frame, RenderContext context) {
        return (rect, buffer) -> {
            if (!rect.isEmpty()) {
                element.constraint(Constraint.fill());
                context.renderChild(element, frame, rect);
            }
        };
    }

    private Rect renderBorder(Frame frame, Rect area, RenderContext context) {
        if (title != null || borderType != null) {
            Block.Builder blockBuilder = Block.builder()
                    .borders(Borders.ALL)
                    .styleResolver(styleResolver(context));
            if (title != null) {
                blockBuilder.title(Title.from(title));
            }
            if (borderType != null) {
                blockBuilder.borderType(borderType);
            }
            if (borderColor != null) {
                blockBuilder.borderColor(borderColor);
            }
            Block block = blockBuilder.build();
            block.render(area, frame.buffer());
            return block.inner(area);
        }
        return area;
    }

    // ═══════════════════════════════════════════════════════════════
    // Event handling
    // ═══════════════════════════════════════════════════════════════

    @Override
    public EventResult handleKeyEvent(KeyEvent event, boolean focused) {
        EventResult result = super.handleKeyEvent(event, focused);
        if (result.isHandled()) {
            return result;
        }

        if (lastFlatEntries.isEmpty()) {
            return EventResult.UNHANDLED;
        }

        if (event.matches(Actions.MOVE_UP)) {
            selectPrevious();
            return EventResult.HANDLED;
        }

        if (event.matches(Actions.MOVE_DOWN)) {
            selectNext();
            return EventResult.HANDLED;
        }

        if (event.matches(Actions.MOVE_RIGHT)) {
            expandSelected();
            return EventResult.HANDLED;
        }

        if (event.matches(Actions.MOVE_LEFT)) {
            collapseSelected();
            return EventResult.HANDLED;
        }

        if (event.matches(Actions.SELECT)) {
            toggleSelected();
            return EventResult.HANDLED;
        }

        if (event.matches(Actions.HOME)) {
            selectFirst();
            return EventResult.HANDLED;
        }

        if (event.matches(Actions.END)) {
            selectLast();
            return EventResult.HANDLED;
        }

        if (event.matches(Actions.PAGE_UP)) {
            int steps = Math.max(1, lastViewportHeight - 1);
            for (int i = 0; i < steps; i++) {
                selectPrevious();
            }
            return EventResult.HANDLED;
        }

        if (event.matches(Actions.PAGE_DOWN)) {
            int steps = Math.max(1, lastViewportHeight - 1);
            for (int i = 0; i < steps; i++) {
                selectNext();
            }
            return EventResult.HANDLED;
        }

        return EventResult.UNHANDLED;
    }

    @Override
    public EventResult handleMouseEvent(MouseEvent event) {
        EventResult result = super.handleMouseEvent(event);
        if (result.isHandled()) {
            return result;
        }

        if (!lastFlatEntries.isEmpty()) {
            if (event.kind() == MouseEventKind.SCROLL_UP) {
                for (int i = 0; i < 3; i++) {
                    selectPrevious();
                }
                return EventResult.HANDLED;
            }
            if (event.kind() == MouseEventKind.SCROLL_DOWN) {
                for (int i = 0; i < 3; i++) {
                    selectNext();
                }
                return EventResult.HANDLED;
            }
        }

        return EventResult.UNHANDLED;
    }
}
