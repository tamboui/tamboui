/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

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
import dev.tamboui.text.CharWidth;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.element.StyledElement;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.bindings.Actions;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.MouseEvent;
import dev.tamboui.tui.event.MouseEventKind;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.widgets.block.Title;
import dev.tamboui.widgets.scrollbar.Scrollbar;
import dev.tamboui.widgets.scrollbar.ScrollbarOrientation;
import dev.tamboui.widgets.scrollbar.ScrollbarState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

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

    /**
     * Style for the tree guide/branch characters.
     */
    public enum GuideStyle {
        /** Unicode box-drawing characters: {@code \u251c\u2500\u2500}, {@code \u2502}, {@code \u2514\u2500\u2500}. */
        UNICODE("\u251c\u2500\u2500 ", "\u2502   ", "\u2514\u2500\u2500 ", "    "),
        /** ASCII characters: {@code +--}, {@code |}, {@code +--}. */
        ASCII("+-- ", "|   ", "+-- ", "    "),
        /** No guide characters. */
        NONE("", "", "", "");

        private final String branch;
        private final String vertical;
        private final String lastBranch;
        private final String space;

        GuideStyle(String branch, String vertical, String lastBranch, String space) {
            this.branch = branch;
            this.vertical = vertical;
            this.lastBranch = lastBranch;
            this.space = space;
        }

        /**
         * Returns the branch connector string (for non-last children).
         *
         * @return the branch string
         */
        public String branch() {
            return branch;
        }

        /**
         * Returns the vertical continuation string.
         *
         * @return the vertical string
         */
        public String vertical() {
            return vertical;
        }

        /**
         * Returns the last-branch connector string.
         *
         * @return the last branch string
         */
        public String lastBranch() {
            return lastBranch;
        }

        /**
         * Returns the space string (for children of last items).
         *
         * @return the space string
         */
        public String space() {
            return space;
        }
    }

    /**
     * Policy for displaying the scrollbar.
     */
    public enum ScrollBarPolicy {
        /** Never show the scrollbar. */
        NONE,
        /** Always show the scrollbar. */
        ALWAYS,
        /** Show the scrollbar only when content exceeds the viewport. */
        AS_NEEDED
    }

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

    private static final PropertyConverter<ScrollBarPolicy> SCROLLBAR_POLICY_CONVERTER = value -> {
        if (value == null || value.trim().isEmpty()) {
            return Optional.empty();
        }
        String normalized = value.trim().toLowerCase();
        for (ScrollBarPolicy policy : ScrollBarPolicy.values()) {
            if (policy.name().toLowerCase().replace('_', '-').equals(normalized)) {
                return Optional.of(policy);
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
            PropertyDefinition.builder("scrollbar-policy", SCROLLBAR_POLICY_CONVERTER)
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
    private int selectedIndex = 0;
    private int scrollOffset = 0;
    private Style highlightStyle;
    private String highlightSymbol;
    private String title;
    private BorderType borderType;
    private Color borderColor;
    private ScrollBarPolicy scrollBarPolicy = ScrollBarPolicy.NONE;
    private Color scrollbarThumbColor;
    private Color scrollbarTrackColor;
    private int indentWidth = -1; // -1 means use guide style width

    // Cached flat entries from last render
    private List<FlatEntry<T>> lastFlatEntries = Collections.emptyList();
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
        this.selectedIndex = Math.max(0, index);
        return this;
    }

    /**
     * Returns the currently selected index in the flattened visible list.
     *
     * @return the selected index
     */
    public int selected() {
        return selectedIndex;
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
        int idx = Math.min(selectedIndex, lastFlatEntries.size() - 1);
        return lastFlatEntries.get(idx).node;
    }

    // ═══════════════════════════════════════════════════════════════
    // Navigation methods
    // ═══════════════════════════════════════════════════════════════

    /**
     * Selects the previous visible node.
     */
    public void selectPrevious() {
        if (selectedIndex > 0) {
            selectedIndex--;
        }
    }

    /**
     * Selects the next visible node.
     */
    public void selectNext() {
        if (!lastFlatEntries.isEmpty() && selectedIndex < lastFlatEntries.size() - 1) {
            selectedIndex++;
        }
    }

    /**
     * Expands the selected node, or moves to the first child if already expanded.
     */
    public void expandSelected() {
        if (lastFlatEntries.isEmpty()) {
            return;
        }
        int idx = Math.min(selectedIndex, lastFlatEntries.size() - 1);
        TreeNode<T> node = lastFlatEntries.get(idx).node;
        if (node.isLeaf()) {
            return;
        }
        if (node.isExpanded()) {
            // Move to first child if there are children
            if (!node.children().isEmpty() && idx + 1 < lastFlatEntries.size()) {
                selectedIndex = idx + 1;
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
        int idx = Math.min(selectedIndex, lastFlatEntries.size() - 1);
        FlatEntry<T> entry = lastFlatEntries.get(idx);
        TreeNode<T> node = entry.node;
        if (node.isExpanded() && !node.isLeaf()) {
            node.expanded(false);
        } else {
            // Move to parent
            TreeNode<T> parent = entry.parent;
            if (parent != null) {
                for (int i = 0; i < lastFlatEntries.size(); i++) {
                    if (lastFlatEntries.get(i).node == parent) {
                        selectedIndex = i;
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
        int idx = Math.min(selectedIndex, lastFlatEntries.size() - 1);
        TreeNode<T> node = lastFlatEntries.get(idx).node;
        if (!node.isLeaf()) {
            node.toggleExpanded();
        }
    }

    /**
     * Selects the first visible node.
     */
    public void selectFirst() {
        selectedIndex = 0;
        scrollOffset = 0;
    }

    /**
     * Selects the last visible node.
     */
    public void selectLast() {
        if (!lastFlatEntries.isEmpty()) {
            selectedIndex = lastFlatEntries.size() - 1;
        }
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

        // Flatten the visible tree
        List<FlatEntry<T>> flatEntries = flattenTree();
        this.lastFlatEntries = flatEntries;

        int totalItems = flatEntries.size();
        if (totalItems == 0) {
            // Still render border if configured
            renderBorder(frame, area, context);
            return;
        }

        // Clamp selection
        selectedIndex = Math.max(0, Math.min(selectedIndex, totalItems - 1));

        // Render border/block
        Rect treeArea = renderBorder(frame, area, context);
        if (treeArea.isEmpty()) {
            return;
        }

        int visibleHeight = treeArea.height();
        this.lastViewportHeight = visibleHeight;

        CssStyleResolver cssResolver = context.resolveStyle(this).orElse(CssStyleResolver.empty());

        // Resolve properties: programmatic > CSS > property default
        GuideStyle effectiveGuideStyle = cssResolver.resolve(GUIDE_STYLE, this.guideStyle);
        ScrollBarPolicy effectiveScrollBarPolicy = cssResolver.resolve(SCROLLBAR_POLICY, this.scrollBarPolicy);
        String effectiveHighlightSymbol = cssResolver.resolve(HIGHLIGHT_SYMBOL, this.highlightSymbol);
        if (effectiveHighlightSymbol == null) {
            effectiveHighlightSymbol = DEFAULT_HIGHLIGHT_SYMBOL;
        }
        int symbolWidth = CharWidth.of(effectiveHighlightSymbol);

        // Resolve indent width: programmatic > CSS > guide style default
        Integer programmaticIndent = this.indentWidth >= 0 ? this.indentWidth : null;
        Integer cssIndent = cssResolver.get(INDENT_WIDTH).orElse(null);
        int effectiveIndentWidth;
        if (programmaticIndent != null) {
            effectiveIndentWidth = programmaticIndent;
        } else if (cssIndent != null) {
            effectiveIndentWidth = cssIndent;
        } else {
            // Default to guide style width (4 for UNICODE/ASCII, 0 for NONE)
            effectiveIndentWidth = CharWidth.of(effectiveGuideStyle.branch());
        }

        // Resolve highlight style
        Style effectiveHighlightStyle = resolveEffectiveStyle(
                context, "node", PseudoClassState.ofSelected(),
                highlightStyle, DEFAULT_HIGHLIGHT_STYLE);

        // Compute content width
        int contentWidth = treeArea.width() - symbolWidth;

        // Compute heights for each entry and cumulative positions
        int totalContentHeight = 0;
        for (FlatEntry<T> entry : flatEntries) {
            entry.cumulativeTop = totalContentHeight;
            entry.height = computeEntryHeight(entry, contentWidth, context, effectiveGuideStyle, effectiveIndentWidth);
            totalContentHeight += entry.height;
        }

        // Determine scrollbar visibility based on total content height
        boolean showScrollbar = effectiveScrollBarPolicy == ScrollBarPolicy.ALWAYS
                || (effectiveScrollBarPolicy == ScrollBarPolicy.AS_NEEDED && totalContentHeight > visibleHeight);

        if (showScrollbar) {
            contentWidth -= 1;
            // Recompute heights with adjusted width
            totalContentHeight = 0;
            for (FlatEntry<T> entry : flatEntries) {
                entry.cumulativeTop = totalContentHeight;
                entry.height = computeEntryHeight(entry, contentWidth, context, effectiveGuideStyle, effectiveIndentWidth);
                totalContentHeight += entry.height;
            }
        }

        if (contentWidth <= 0) {
            return;
        }

        // Auto-scroll to keep selected item visible (using cumulative heights)
        FlatEntry<T> selectedEntry = flatEntries.get(selectedIndex);
        int selectedTop = selectedEntry.cumulativeTop;
        int selectedBottom = selectedTop + selectedEntry.height;

        if (selectedTop < scrollOffset) {
            scrollOffset = selectedTop;
        } else if (selectedBottom > scrollOffset + visibleHeight) {
            scrollOffset = selectedBottom - visibleHeight;
        }
        scrollOffset = Math.max(0, Math.min(scrollOffset, Math.max(0, totalContentHeight - visibleHeight)));

        // Resolve guide style from CSS
        Style guideLineStyle = resolveEffectiveStyle(context, "guide", null, Style.EMPTY);

        // Render visible entries
        int contentX = treeArea.left() + symbolWidth;

        for (int entryIndex = 0; entryIndex < totalItems; entryIndex++) {
            FlatEntry<T> entry = flatEntries.get(entryIndex);

            // Skip entries completely above the viewport
            if (entry.cumulativeTop + entry.height <= scrollOffset) {
                continue;
            }

            // Stop if we've gone past the viewport
            if (entry.cumulativeTop >= scrollOffset + visibleHeight) {
                break;
            }

            // Calculate Y position relative to viewport
            int entryY = treeArea.top() + (entry.cumulativeTop - scrollOffset);

            // Clip to viewport bounds
            int renderStartY = Math.max(entryY, treeArea.top());
            int renderEndY = Math.min(entryY + entry.height, treeArea.top() + visibleHeight);
            int renderHeight = renderEndY - renderStartY;

            if (renderHeight <= 0) {
                continue;
            }

            boolean isSelected = (entryIndex == selectedIndex);

            // Draw highlight symbol for selected item (on first line only)
            if (isSelected && symbolWidth > 0 && entryY >= treeArea.top()) {
                frame.buffer().setString(treeArea.left(), entryY, effectiveHighlightSymbol, effectiveHighlightStyle);
            }

            // Build the prefix (guide characters)
            String prefix = buildPrefix(entry, effectiveGuideStyle, effectiveIndentWidth);
            int prefixWidth = CharWidth.of(prefix);

            // Draw expand indicator
            String indicator;
            if (!entry.node.isLeaf()) {
                indicator = entry.node.isExpanded() ? "▼ " : "▶ ";
            } else {
                indicator = "  ";
            }
            int indicatorWidth = CharWidth.of(indicator);

            // Draw prefix and indicator on first line (if visible)
            if (entryY >= treeArea.top() && entryY < treeArea.top() + visibleHeight) {
                // Draw prefix with guide style
                if (!prefix.isEmpty()) {
                    Style prefixStyle = guideLineStyle.equals(Style.EMPTY) ? context.currentStyle() : guideLineStyle;
                    if (isSelected) {
                        prefixStyle = prefixStyle.patch(effectiveHighlightStyle);
                    }
                    frame.buffer().setString(contentX, entryY, CharWidth.substringByWidth(prefix, contentWidth), prefixStyle);
                }

                // Draw indicator
                int indicatorX = contentX + prefixWidth;
                if (indicatorX < contentX + contentWidth) {
                    Style indicatorStyle = isSelected ? context.currentStyle().patch(effectiveHighlightStyle) : context.currentStyle();
                    frame.buffer().setString(indicatorX, entryY, indicator, indicatorStyle);
                }
            }

            // Draw label or custom node content
            int labelX = contentX + prefixWidth + indicatorWidth;
            int labelAvailable = contentWidth - prefixWidth - indicatorWidth;
            if (labelAvailable > 0) {
                if (nodeRenderer != null) {
                    // Use custom renderer for rich node content
                    StyledElement<?> nodeElement = nodeRenderer.apply(entry.node);
                    if (nodeElement != null) {
                        // Create area for the full entry height, clipped to viewport
                        int nodeY = Math.max(entryY, treeArea.top());
                        int nodeHeight = Math.min(entry.height, treeArea.top() + visibleHeight - nodeY);
                        if (nodeHeight > 0) {
                            Rect nodeArea = new Rect(labelX, nodeY, labelAvailable, nodeHeight);
                            nodeElement.constraint(Constraint.fill());
                            // Apply highlight style for selected items (from CSS or programmatic)
                            if (isSelected) {
                                nodeElement.style(effectiveHighlightStyle);
                            }
                            context.renderChild(nodeElement, frame, nodeArea);
                        }
                    }
                } else if (entry.node.label() != null && entryY >= treeArea.top()) {
                    // Default: render label as text
                    Style labelStyle = isSelected ? context.currentStyle().patch(effectiveHighlightStyle) : context.currentStyle();
                    String truncated = CharWidth.substringByWidth(entry.node.label(), labelAvailable);
                    frame.buffer().setString(labelX, entryY, truncated, labelStyle);
                }
            }
        }

        // Render scrollbar
        if (showScrollbar && totalContentHeight > 0) {
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

            Style explicitThumbStyle = scrollbarThumbColor != null ? Style.EMPTY.fg(scrollbarThumbColor) : null;
            Style explicitTrackStyle = scrollbarTrackColor != null ? Style.EMPTY.fg(scrollbarTrackColor) : null;
            Style thumbStyle = resolveEffectiveStyle(context, "scrollbar-thumb", explicitThumbStyle, Style.EMPTY);
            Style trackStyle = resolveEffectiveStyle(context, "scrollbar-track", explicitTrackStyle, Style.EMPTY);

            Scrollbar.Builder scrollbarBuilder = Scrollbar.builder()
                    .orientation(ScrollbarOrientation.VERTICAL_RIGHT);
            if (!thumbStyle.equals(Style.EMPTY)) {
                scrollbarBuilder.thumbStyle(thumbStyle);
            }
            if (!trackStyle.equals(Style.EMPTY)) {
                scrollbarBuilder.trackStyle(trackStyle);
            }

            frame.renderStatefulWidget(scrollbarBuilder.build(), scrollbarArea, scrollbarState);
        }
    }

    /**
     * Computes the height of a tree entry, considering the nodeRenderer's preferred height.
     */
    private int computeEntryHeight(FlatEntry<T> entry, int contentWidth,
                                   RenderContext context, GuideStyle effectiveGuideStyle,
                                   int effectiveIndentWidth) {
        if (nodeRenderer == null) {
            return 1; // Default text label is always 1 line
        }

        StyledElement<?> nodeElement = nodeRenderer.apply(entry.node);
        if (nodeElement == null) {
            return 1;
        }

        // Calculate available width for the node content
        String prefix = buildPrefix(entry, effectiveGuideStyle, effectiveIndentWidth);
        int prefixWidth = CharWidth.of(prefix);
        int indicatorWidth = 2; // expand/collapse indicator
        int labelAvailable = contentWidth - prefixWidth - indicatorWidth;

        if (labelAvailable <= 0) {
            return 1;
        }

        return Math.max(1, nodeElement.preferredHeight(labelAvailable, context));
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
    // Tree flattening
    // ═══════════════════════════════════════════════════════════════

    private List<FlatEntry<T>> flattenTree() {
        List<FlatEntry<T>> entries = new ArrayList<>();
        for (int i = 0; i < roots.size(); i++) {
            boolean isLastRoot = (i == roots.size() - 1);
            flattenNode(roots.get(i), null, 0, new ArrayList<>(), isLastRoot, entries);
        }
        return entries;
    }

    private void flattenNode(TreeNode<T> node, TreeNode<T> parent, int depth,
                             List<Boolean> parentIsLast, boolean isLast,
                             List<FlatEntry<T>> entries) {
        List<Boolean> guides = new ArrayList<>(parentIsLast);
        entries.add(new FlatEntry<>(node, parent, depth, guides, isLast));

        if (node.isExpanded() && !node.isLeaf()) {
            List<TreeNode<T>> children = node.children();
            List<Boolean> childParentIsLast = new ArrayList<>(parentIsLast);
            childParentIsLast.add(isLast);
            for (int i = 0; i < children.size(); i++) {
                boolean childIsLast = (i == children.size() - 1);
                flattenNode(children.get(i), node, depth + 1, childParentIsLast, childIsLast, entries);
            }
        }
    }

    private String buildPrefix(FlatEntry<T> entry, GuideStyle effectiveGuideStyle, int indentWidth) {
        if (entry.depth == 0 || effectiveGuideStyle == GuideStyle.NONE) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        // Add vertical/space guides for ancestor levels
        for (int i = 0; i < entry.parentIsLast.size(); i++) {
            String guide = entry.parentIsLast.get(i)
                    ? effectiveGuideStyle.space()
                    : effectiveGuideStyle.vertical();
            sb.append(padToWidth(guide, indentWidth));
        }

        // Add the branch connector for this node
        String branch = entry.isLast
                ? effectiveGuideStyle.lastBranch()
                : effectiveGuideStyle.branch();
        sb.append(padToWidth(branch, indentWidth));

        return sb.toString();
    }

    /**
     * Pads or truncates a string to the specified display width.
     */
    private String padToWidth(String s, int width) {
        int currentWidth = CharWidth.of(s);
        if (currentWidth == width) {
            return s;
        } else if (currentWidth < width) {
            // Pad with spaces
            StringBuilder sb = new StringBuilder(s);
            for (int i = currentWidth; i < width; i++) {
                sb.append(' ');
            }
            return sb.toString();
        } else {
            // Truncate to width
            return CharWidth.substringByWidth(s, width);
        }
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

    // ═══════════════════════════════════════════════════════════════
    // Internal flat entry
    // ═══════════════════════════════════════════════════════════════

    /**
     * A flattened entry representing a visible tree node.
     *
     * @param <T> the data type
     */
    static final class FlatEntry<T> {
        final TreeNode<T> node;
        final TreeNode<T> parent;
        final int depth;
        final List<Boolean> parentIsLast;
        final boolean isLast;
        int height = 1; // computed during render
        int cumulativeTop = 0; // Y position from start

        FlatEntry(TreeNode<T> node, TreeNode<T> parent, int depth, List<Boolean> parentIsLast, boolean isLast) {
            this.node = node;
            this.parent = parent;
            this.depth = depth;
            this.parentIsLast = parentIsLast;
            this.isLast = isLast;
        }
    }
}
