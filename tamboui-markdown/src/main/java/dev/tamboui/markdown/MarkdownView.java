/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.markdown;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import org.commonmark.node.Node;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.markdown.internal.LinesChunk;
import dev.tamboui.markdown.internal.MarkdownLayout;
import dev.tamboui.markdown.internal.MarkdownParserHolder;
import dev.tamboui.markdown.internal.PartialMarkdownSanitizer;
import dev.tamboui.markdown.internal.RenderedChunk;
import dev.tamboui.markdown.internal.WidgetChunk;
import dev.tamboui.style.Color;
import dev.tamboui.style.ColorConverter;
import dev.tamboui.style.Modifier;
import dev.tamboui.style.ModifierConverter;
import dev.tamboui.style.Overflow;
import dev.tamboui.style.PropertyDefinition;
import dev.tamboui.style.StandardProperties;
import dev.tamboui.style.StringConverter;
import dev.tamboui.style.Style;
import dev.tamboui.style.StylePropertyResolver;
import dev.tamboui.text.Line;
import dev.tamboui.widget.Widget;
import dev.tamboui.widgets.block.Block;

/**
 * A widget that renders CommonMark + GFM markdown to a {@link Buffer}.
 *
 * <p>Robustness against partial input is built in: before every parse the
 * source is run through a sanitizer that trims trailing unmatched inline
 * markers ({@code **}, {@code _}, {@code ~}, {@code `}) and dangling links.
 * Well-formed markdown is returned unchanged. As a result, a consumer can
 * re-bind the source on every render — for example, to display a markdown
 * stream produced by an LLM — without flicker or stray markers showing up.
 *
 <h2>CSS Properties</h2>
 * Same single-resolver pattern as {@link Block}: properties have flat names
 * resolved against one {@link StylePropertyResolver} that the caller has
 * already scoped to this widget. Each markdown sub-element exposes three
 * properties built from the standard {@link ColorConverter} and
 * {@link ModifierConverter}:
 * <ul>
 *   <li>{@code <element>-color} — foreground color</li>
 *   <li>{@code <element>-background} — background color</li>
 *   <li>{@code <element>-text-style} — modifier set</li>
 * </ul>
 * where {@code <element>} is one of {@code heading-1} … {@code heading-6},
 * {@code strong}, {@code emphasis}, {@code strikethrough},
 * {@code inline-code}, {@code code-block}, {@code link}, {@code blockquote},
 * {@code list-marker}, {@code html}, {@code horizontal-rule},
 * {@code task-checked}, or {@code task-unchecked}.
 *
 * <p>Plus a few string properties that mirror the {@code Checkbox} widget
 * convention:
 * <ul>
 *   <li>{@link #BLOCKQUOTE_PREFIX} ({@code blockquote-prefix}) — leading bar
 *       glyph drawn at the start of each quoted line; a single space is
 *       always appended after it.</li>
 *   <li>{@link #TASK_CHECKED_SYMBOL} ({@code task-checked-symbol}) and
 *       {@link #TASK_UNCHECKED_SYMBOL} ({@code task-unchecked-symbol}) —
 *       glyphs drawn for GFM task-list items; defaults are {@code [x]} and
 *       {@code [ ]}, again followed by a single space.</li>
 * </ul>
 *
 * <p>And the widget-wide {@code text-overflow} property
 * ({@link StandardProperties#TEXT_OVERFLOW}) controlling how prose lines
 * wider than the content area are handled.
 *
 * <p>Programmatic {@link MarkdownStyles} overrides take priority; unset
 * slots fall back to CSS, then to built-in defaults.
 *
 * <p>Example TCSS (the type selector matches whatever style type the caller
 * has assigned to this widget):
 * <pre>
 * MarkdownView {
 *     heading-1-color: magenta;
 *     heading-1-text-style: bold;
 *     link-color: green;
 *     link-text-style: underlined;
 *     blockquote-color: yellow;
 *     blockquote-background: black;
 *     blockquote-prefix: "&gt; ";
 * }
 * </pre>
 *
 * <p>Supported markdown elements:
 * <ul>
 *   <li>Headings (1-6) with bold styling and an underline rule for H1/H2</li>
 *   <li>Paragraphs with width-aware word wrap</li>
 *   <li>Bullet, ordered, and task (GFM) lists with nested indentation</li>
 *   <li>Block quotes (single bar, dim style)</li>
 *   <li>Fenced and indented code blocks (rounded {@code Block} border, info
 *       string used as title)</li>
 *   <li>GFM tables (delegated to {@link dev.tamboui.widgets.table.Table})</li>
 *   <li>Inline emphasis, strong, strikethrough, code, and links (links use
 *       the OSC-8 hyperlink extension)</li>
 *   <li>Thematic breaks rendered as a horizontal rule</li>
 *   <li>HTML blocks and inline HTML rendered as dim escaped text</li>
 *   <li>Images rendered as {@code [image: alt](url)} text spans</li>
 * </ul>
 */
public final class MarkdownView implements Widget {

    /** CSS property for the blockquote prefix string drawn at the start of every quoted line. */
    public static final PropertyDefinition<String> BLOCKQUOTE_PREFIX =
        PropertyDefinition.of("blockquote-prefix", StringConverter.INSTANCE);
    /** CSS property for the checked task-list marker glyph. */
    public static final PropertyDefinition<String> TASK_CHECKED_SYMBOL =
        PropertyDefinition.of("task-checked-symbol", StringConverter.INSTANCE);
    /** CSS property for the unchecked task-list marker glyph. */
    public static final PropertyDefinition<String> TASK_UNCHECKED_SYMBOL =
        PropertyDefinition.of("task-unchecked-symbol", StringConverter.INSTANCE);

    private static final ElementProperties[] HEADINGS = new ElementProperties[] {
        ElementProperties.of("heading-1"),
        ElementProperties.of("heading-2"),
        ElementProperties.of("heading-3"),
        ElementProperties.of("heading-4"),
        ElementProperties.of("heading-5"),
        ElementProperties.of("heading-6")
    };
    private static final ElementProperties STRONG = ElementProperties.of("strong");
    private static final ElementProperties EMPHASIS = ElementProperties.of("emphasis");
    private static final ElementProperties STRIKETHROUGH = ElementProperties.of("strikethrough");
    private static final ElementProperties INLINE_CODE = ElementProperties.of("inline-code");
    private static final ElementProperties CODE_BLOCK = ElementProperties.of("code-block");
    private static final ElementProperties LINK = ElementProperties.of("link");
    private static final ElementProperties BLOCKQUOTE = ElementProperties.of("blockquote");
    private static final ElementProperties LIST_MARKER = ElementProperties.of("list-marker");
    private static final ElementProperties HTML = ElementProperties.of("html");
    private static final ElementProperties HORIZONTAL_RULE = ElementProperties.of("horizontal-rule");
    private static final ElementProperties TASK_CHECKED = ElementProperties.of("task-checked");
    private static final ElementProperties TASK_UNCHECKED = ElementProperties.of("task-unchecked");

    private final String source;
    private final Block block;
    private final Style style;
    private final MarkdownStyles styles;
    private final StylePropertyResolver styleResolver;
    private final Overflow overflow;
    private final int scroll;

    private MarkdownView(Builder builder) {
        this.source = Objects.requireNonNull(builder.source, "source");
        this.block = builder.block;
        this.style = builder.style;
        this.styles = builder.styles;
        this.styleResolver = builder.styleResolver;
        // Programmatic value wins; otherwise consult the resolver directly
        // (resolve() would fall back to the property's CLIP default, which is
        // the wrong default for prose-flowing markdown).
        if (builder.overflow != null) {
            this.overflow = builder.overflow;
        } else {
            this.overflow = styleResolver
                .get(StandardProperties.TEXT_OVERFLOW)
                .orElse(Overflow.WRAP_WORD);
        }
        this.scroll = builder.scroll;
    }

    /**
     * Creates a new builder.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Computes the total number of rows the rendered markdown would take at
     * {@code width} columns. Outer {@code Block} chrome (borders, padding,
     * titles) is included if a block has been set. Useful for size-aware
     * layout containers that need to know how tall the widget wants to be
     * given an available width.
     *
     * @param width the available width in columns
     * @return total rows needed; 0 when {@code width <= 0}
     */
    public int computeHeight(int width) {
        if (width <= 0) {
            return 0;
        }
        int contentWidth = width;
        int chrome = 0;
        if (block != null) {
            contentWidth -= block.horizontalChrome();
            chrome = block.verticalChrome();
        }
        if (contentWidth <= 0) {
            return chrome;
        }
        String sanitized = PartialMarkdownSanitizer.sanitize(source);
        Node root = MarkdownParserHolder.parser().parse(sanitized);
        MarkdownStyles resolved = resolveStyles();
        int total = 0;
        for (RenderedChunk chunk : MarkdownLayout.layout(root, contentWidth, resolved, overflow)) {
            total += chunk.height(contentWidth);
        }
        return total + chrome;
    }

    @Override
    public void render(Rect area, Buffer buffer) {
        if (area.isEmpty()) {
            return;
        }
        buffer.setStyle(area, style);
        Rect contentArea = area;
        if (block != null) {
            block.render(area, buffer);
            contentArea = block.inner(area);
        }
        if (contentArea.isEmpty()) {
            return;
        }
        buffer.setStyle(contentArea, style);

        String sanitized = PartialMarkdownSanitizer.sanitize(source);
        Node root = MarkdownParserHolder.parser().parse(sanitized);
        MarkdownStyles resolved = resolveStyles();
        List<RenderedChunk> chunks = MarkdownLayout.layout(root, contentArea.width(), resolved, overflow);

        int totalRows = 0;
        for (RenderedChunk chunk : chunks) {
            totalRows += chunk.height(contentArea.width());
        }
        if (totalRows == 0) {
            return;
        }

        int skip = Math.max(0, Math.min(scroll, totalRows - 1));
        int y = contentArea.top();
        int rowsRemaining = contentArea.height();
        int rowsSkipped = 0;

        for (RenderedChunk chunk : chunks) {
            if (rowsRemaining <= 0) {
                break;
            }
            int chunkHeight = chunk.height(contentArea.width());
            if (rowsSkipped + chunkHeight <= skip) {
                rowsSkipped += chunkHeight;
                continue;
            }
            int chunkSkip = Math.max(0, skip - rowsSkipped);
            int visibleHeight = Math.min(chunkHeight - chunkSkip, rowsRemaining);
            if (visibleHeight <= 0) {
                rowsSkipped += chunkHeight;
                continue;
            }
            if (chunk instanceof WidgetChunk) {
                // Block-bordered widgets (code blocks, tables) need their full
                // geometry to render correctly — a partial slice paints only
                // the top border with no closure. Skip when we can't fit it
                // whole; the user scrolls to align with its top.
                if (chunkSkip == 0 && visibleHeight == chunkHeight) {
                    Rect target = new Rect(contentArea.left(), y, contentArea.width(), chunkHeight);
                    chunk.render(target, buffer);
                }
            } else if (chunk instanceof LinesChunk) {
                LinesChunk lc = (LinesChunk) chunk;
                List<Line> lines = lc.lines();
                int from = Math.min(lines.size(), chunkSkip);
                int to = Math.min(lines.size(), from + visibleHeight);
                for (int i = from; i < to; i++) {
                    buffer.setLine(contentArea.left(), y + (i - from), lines.get(i));
                }
            }
            y += visibleHeight;
            rowsRemaining -= visibleHeight;
            rowsSkipped += chunkHeight;
        }
    }

    private MarkdownStyles resolveStyles() {
        if (styleResolver == StylePropertyResolver.empty()) {
            return styles;
        }
        MarkdownStyles.Builder b = MarkdownStyles.builder();
        for (int level = 1; level <= 6; level++) {
            Style explicit = styles.headingOrNull(level);
            if (explicit != null) {
                b.heading(level, explicit);
            } else {
                Style fromCss = resolveCssStyle(HEADINGS[level - 1]);
                if (fromCss != null) {
                    b.heading(level, fromCss);
                }
            }
        }
        applySlot(b::strong, styles.strongOrNull(), STRONG);
        applySlot(b::emphasis, styles.emphasisOrNull(), EMPHASIS);
        applySlot(b::strikethrough, styles.strikethroughOrNull(), STRIKETHROUGH);
        applySlot(b::inlineCode, styles.inlineCodeOrNull(), INLINE_CODE);
        applySlot(b::codeBlock, styles.codeBlockOrNull(), CODE_BLOCK);
        applySlot(b::link, styles.linkOrNull(), LINK);
        applySlot(b::blockquote, styles.blockquoteOrNull(), BLOCKQUOTE);
        applySlot(b::listMarker, styles.listMarkerOrNull(), LIST_MARKER);
        applySlot(b::html, styles.htmlOrNull(), HTML);
        applySlot(b::horizontalRule, styles.horizontalRuleOrNull(), HORIZONTAL_RULE);
        applySlot(b::taskChecked, styles.taskCheckedOrNull(), TASK_CHECKED);
        applySlot(b::taskUnchecked, styles.taskUncheckedOrNull(), TASK_UNCHECKED);
        applyString(b::blockquotePrefix, styles.blockquotePrefixOrNull(), BLOCKQUOTE_PREFIX);
        applyString(b::taskCheckedSymbol, styles.taskCheckedSymbolOrNull(), TASK_CHECKED_SYMBOL);
        applyString(b::taskUncheckedSymbol, styles.taskUncheckedSymbolOrNull(), TASK_UNCHECKED_SYMBOL);
        return b.build();
    }

    private void applyString(
        Consumer<String> setter, String explicit, PropertyDefinition<String> property) {
        if (explicit != null) {
            setter.accept(explicit);
            return;
        }
        String value = styleResolver.resolve(property, null);
        if (value != null) {
            setter.accept(value);
        }
    }

    private void applySlot(Consumer<Style> setter, Style explicit, ElementProperties props) {
        if (explicit != null) {
            setter.accept(explicit);
            return;
        }
        Style fromCss = resolveCssStyle(props);
        if (fromCss != null) {
            setter.accept(fromCss);
        }
    }

    private Style resolveCssStyle(ElementProperties props) {
        Color color = styleResolver.resolve(props.color, null);
        Color background = styleResolver.resolve(props.background, null);
        Set<Modifier> modifiers = styleResolver.resolve(props.textStyle, null);
        if (color == null && background == null && (modifiers == null || modifiers.isEmpty())) {
            return null;
        }
        Style style = Style.EMPTY;
        if (color != null) {
            style = style.fg(color);
        }
        if (background != null) {
            style = style.bg(background);
        }
        if (modifiers != null) {
            for (Modifier modifier : modifiers) {
                style = style.addModifier(modifier);
            }
        }
        return style;
    }

    private static final class ElementProperties {
        final PropertyDefinition<Color> color;
        final PropertyDefinition<Color> background;
        final PropertyDefinition<Set<Modifier>> textStyle;

        private ElementProperties(
            PropertyDefinition<Color> color,
            PropertyDefinition<Color> background,
            PropertyDefinition<Set<Modifier>> textStyle) {
            this.color = color;
            this.background = background;
            this.textStyle = textStyle;
        }

        static ElementProperties of(String prefix) {
            return new ElementProperties(
                PropertyDefinition.of(prefix + "-color", ColorConverter.INSTANCE),
                PropertyDefinition.of(prefix + "-background", ColorConverter.INSTANCE),
                PropertyDefinition.of(prefix + "-text-style", ModifierConverter.INSTANCE));
        }
    }

    /** Builder for {@link MarkdownView}. */
    public static final class Builder {

        private String source;
        private Block block;
        private Style style = Style.EMPTY;
        private MarkdownStyles styles = MarkdownStyles.DEFAULTS;
        private StylePropertyResolver styleResolver = StylePropertyResolver.empty();
        private Overflow overflow;
        private int scroll;

        private Builder() {
        }

        /**
         * Sets the markdown source to render.
         *
         * @param source the markdown text
         * @return this builder
         */
        public Builder source(String source) {
            this.source = Objects.requireNonNull(source, "source");
            return this;
        }

        /**
         * Wraps the rendered content in the given {@code Block}. The content
         * area is computed via {@link Block#inner(Rect)}.
         *
         * @param block the block, or {@code null} to remove
         * @return this builder
         */
        public Builder block(Block block) {
            this.block = block;
            return this;
        }

        /**
         * Sets the base style applied to the whole content area before
         * rendering. Useful for setting a background color.
         *
         * @param style the base style
         * @return this builder
         */
        public Builder style(Style style) {
            this.style = Objects.requireNonNull(style, "style");
            return this;
        }

        /**
         * Sets the style palette used for headings, code, links, and so on.
         *
         * @param styles the style palette
         * @return this builder
         */
        public Builder styles(MarkdownStyles styles) {
            this.styles = Objects.requireNonNull(styles, "styles");
            return this;
        }

        /**
         * Sets the property resolver consulted for any markdown style not
         * explicitly set on the {@link MarkdownStyles} passed to
         * {@link #styles(MarkdownStyles)}. Each element exposes a
         * {@code -color} and {@code -text-style} property; see
         * {@link MarkdownView} for the full list.
         *
         * @param resolver the property resolver, or {@code null} for no CSS
         * @return this builder
         */
        public Builder styleResolver(StylePropertyResolver resolver) {
            this.styleResolver = resolver != null ? resolver : StylePropertyResolver.empty();
            return this;
        }

        /**
         * Sets how prose lines wider than the content width are handled.
         * Default is {@link Overflow#WRAP_WORD}. Code-block lines are
         * always clipped (their structure is meaningful and ellipsis would
         * mislead) and table cells are sized by the {@code Table} widget.
         *
         * <p>Also resolved from {@code text-overflow} via the configured
         * {@link StylePropertyResolver}; the programmatic value wins when set.
         *
         * @param overflow the overflow mode
         * @return this builder
         */
        public Builder overflow(Overflow overflow) {
            this.overflow = Objects.requireNonNull(overflow, "overflow");
            return this;
        }

        /**
         * Sets the index of the first visible content row. Use this to scroll
         * within longer markdown documents.
         *
         * @param scroll first visible row, &gt;= 0
         * @return this builder
         */
        public Builder scroll(int scroll) {
            this.scroll = Math.max(0, scroll);
            return this;
        }

        /**
         * Builds the {@link MarkdownView} instance.
         *
         * @return a new {@link MarkdownView}
         */
        public MarkdownView build() {
            return new MarkdownView(this);
        }
    }
}
