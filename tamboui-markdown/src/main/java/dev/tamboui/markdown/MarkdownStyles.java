/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.markdown;

import java.util.Objects;

import dev.tamboui.style.Color;
import dev.tamboui.style.Style;

/**
 * Programmatic style overrides for {@link MarkdownView}. Each slot is
 * independently overridable; slots the caller does not touch fall back to
 * the CSS resolver (when one is configured) and finally to a built-in
 * default. The precedence is therefore <strong>explicit programmatic
 * value &gt; CSS &gt; default</strong>, matching the convention documented
 * in {@code AGENTS.md} for CSS-compatible widgets.
 *
 * <p>Defaults:
 * <ul>
 *   <li>headings: bold (level 1 also reversed for emphasis)</li>
 *   <li>strong: bold</li>
 *   <li>emphasis: italic</li>
 *   <li>strikethrough: crossed-out</li>
 *   <li>inline code: gray foreground, dim</li>
 *   <li>code block: gray foreground</li>
 *   <li>link: blue, underlined</li>
 *   <li>blockquote: dim</li>
 *   <li>list marker: cyan</li>
 *   <li>html: dim, gray</li>
 * </ul>
 */
public final class MarkdownStyles {

    static final Style[] DEFAULT_HEADINGS = new Style[] {
        Style.EMPTY.bold().fg(Color.WHITE).reversed(),
        Style.EMPTY.bold().fg(Color.YELLOW),
        Style.EMPTY.bold().fg(Color.CYAN),
        Style.EMPTY.bold(),
        Style.EMPTY.bold(),
        Style.EMPTY.bold()
    };
    static final Style DEFAULT_STRONG = Style.EMPTY.bold();
    static final Style DEFAULT_EMPHASIS = Style.EMPTY.italic();
    static final Style DEFAULT_STRIKETHROUGH = Style.EMPTY.crossedOut();
    static final Style DEFAULT_INLINE_CODE = Style.EMPTY.fg(Color.GRAY).dim();
    static final Style DEFAULT_CODE_BLOCK = Style.EMPTY.fg(Color.GRAY);
    static final Style DEFAULT_LINK = Style.EMPTY.fg(Color.BLUE).underlined();
    static final Style DEFAULT_BLOCKQUOTE = Style.EMPTY.dim();
    static final Style DEFAULT_LIST_MARKER = Style.EMPTY.fg(Color.CYAN);
    static final Style DEFAULT_HTML = Style.EMPTY.dim().fg(Color.GRAY);
    static final Style DEFAULT_HORIZONTAL_RULE = Style.EMPTY.fg(Color.GRAY);
    static final Style DEFAULT_TASK_CHECKED = Style.EMPTY.fg(Color.GREEN);
    static final Style DEFAULT_TASK_UNCHECKED = Style.EMPTY.fg(Color.GRAY);
    static final String DEFAULT_BLOCKQUOTE_PREFIX = "│";
    static final String DEFAULT_TASK_CHECKED_SYMBOL = "[x]";
    static final String DEFAULT_TASK_UNCHECKED_SYMBOL = "[ ]";

    /**
     * The default style set. Equivalent to {@code MarkdownStyles.builder().build()}
     * in observable behaviour: every public getter returns its built-in default.
     */
    public static final MarkdownStyles DEFAULTS = new Builder().build();

    private final Style[] heading;
    private final Style strong;
    private final Style emphasis;
    private final Style strikethrough;
    private final Style inlineCode;
    private final Style codeBlock;
    private final Style link;
    private final Style blockquote;
    private final Style listMarker;
    private final Style html;
    private final Style horizontalRule;
    private final Style taskChecked;
    private final Style taskUnchecked;
    private final String blockquotePrefix;
    private final String taskCheckedSymbol;
    private final String taskUncheckedSymbol;

    private MarkdownStyles(Builder builder) {
        this.heading = builder.heading.clone();
        this.strong = builder.strong;
        this.emphasis = builder.emphasis;
        this.strikethrough = builder.strikethrough;
        this.inlineCode = builder.inlineCode;
        this.codeBlock = builder.codeBlock;
        this.link = builder.link;
        this.blockquote = builder.blockquote;
        this.listMarker = builder.listMarker;
        this.html = builder.html;
        this.horizontalRule = builder.horizontalRule;
        this.taskChecked = builder.taskChecked;
        this.taskUnchecked = builder.taskUnchecked;
        this.blockquotePrefix = builder.blockquotePrefix;
        this.taskCheckedSymbol = builder.taskCheckedSymbol;
        this.taskUncheckedSymbol = builder.taskUncheckedSymbol;
    }

    /**
     * Returns the style for the given heading level.
     *
     * @param level heading level (1-6); values outside the range are clamped
     * @return the style for headings at that level
     */
    public Style heading(int level) {
        int clamped = Math.max(1, Math.min(6, level));
        Style explicit = heading[clamped - 1];
        return explicit != null ? explicit : DEFAULT_HEADINGS[clamped - 1];
    }

    /**
     * Returns the style for strong (bold) inline emphasis.
     *
     * @return the strong style
     */
    public Style strong() {
        return strong != null ? strong : DEFAULT_STRONG;
    }

    /**
     * Returns the style for italic inline emphasis.
     *
     * @return the emphasis style
     */
    public Style emphasis() {
        return emphasis != null ? emphasis : DEFAULT_EMPHASIS;
    }

    /**
     * Returns the style for strikethrough text.
     *
     * @return the strikethrough style
     */
    public Style strikethrough() {
        return strikethrough != null ? strikethrough : DEFAULT_STRIKETHROUGH;
    }

    /**
     * Returns the style for inline code spans.
     *
     * @return the inline-code style
     */
    public Style inlineCode() {
        return inlineCode != null ? inlineCode : DEFAULT_INLINE_CODE;
    }

    /**
     * Returns the style for code-block content.
     *
     * @return the code-block style
     */
    public Style codeBlock() {
        return codeBlock != null ? codeBlock : DEFAULT_CODE_BLOCK;
    }

    /**
     * Returns the style for hyperlinks.
     *
     * @return the link style
     */
    public Style link() {
        return link != null ? link : DEFAULT_LINK;
    }

    /**
     * Returns the style for blockquote content.
     *
     * @return the blockquote style
     */
    public Style blockquote() {
        return blockquote != null ? blockquote : DEFAULT_BLOCKQUOTE;
    }

    /**
     * Returns the style for list markers (bullets and numbers).
     *
     * @return the list-marker style
     */
    public Style listMarker() {
        return listMarker != null ? listMarker : DEFAULT_LIST_MARKER;
    }

    /**
     * Returns the style for raw HTML content rendered as escaped text.
     *
     * @return the HTML style
     */
    public Style html() {
        return html != null ? html : DEFAULT_HTML;
    }

    /**
     * Returns the style for the horizontal rule glyphs.
     *
     * @return the horizontal-rule style
     */
    public Style horizontalRule() {
        return horizontalRule != null ? horizontalRule : DEFAULT_HORIZONTAL_RULE;
    }

    /**
     * Returns the prefix string drawn at the start of every blockquote line.
     *
     * @return the blockquote prefix
     */
    public String blockquotePrefix() {
        return blockquotePrefix != null ? blockquotePrefix : DEFAULT_BLOCKQUOTE_PREFIX;
    }

    /**
     * Returns the style for the checked task-list marker.
     *
     * @return the checked-task style
     */
    public Style taskChecked() {
        return taskChecked != null ? taskChecked : DEFAULT_TASK_CHECKED;
    }

    /**
     * Returns the style for the unchecked task-list marker.
     *
     * @return the unchecked-task style
     */
    public Style taskUnchecked() {
        return taskUnchecked != null ? taskUnchecked : DEFAULT_TASK_UNCHECKED;
    }

    /**
     * Returns the symbol drawn for a checked task-list item. A single space
     * is appended after this glyph when rendered.
     *
     * @return the checked-task symbol
     */
    public String taskCheckedSymbol() {
        return taskCheckedSymbol != null ? taskCheckedSymbol : DEFAULT_TASK_CHECKED_SYMBOL;
    }

    /**
     * Returns the symbol drawn for an unchecked task-list item. A single space
     * is appended after this glyph when rendered.
     *
     * @return the unchecked-task symbol
     */
    public String taskUncheckedSymbol() {
        return taskUncheckedSymbol != null ? taskUncheckedSymbol : DEFAULT_TASK_UNCHECKED_SYMBOL;
    }

    // Package-private accessors that return null when the slot was not
    // explicitly set. Used by MarkdownView to decide whether the CSS
    // resolver should fill the gap.

    Style headingOrNull(int level) {
        int clamped = Math.max(1, Math.min(6, level));
        return heading[clamped - 1];
    }

    Style strongOrNull() {
        return strong;
    }

    Style emphasisOrNull() {
        return emphasis;
    }

    Style strikethroughOrNull() {
        return strikethrough;
    }

    Style inlineCodeOrNull() {
        return inlineCode;
    }

    Style codeBlockOrNull() {
        return codeBlock;
    }

    Style linkOrNull() {
        return link;
    }

    Style blockquoteOrNull() {
        return blockquote;
    }

    Style listMarkerOrNull() {
        return listMarker;
    }

    Style htmlOrNull() {
        return html;
    }

    Style horizontalRuleOrNull() {
        return horizontalRule;
    }

    String blockquotePrefixOrNull() {
        return blockquotePrefix;
    }

    Style taskCheckedOrNull() {
        return taskChecked;
    }

    Style taskUncheckedOrNull() {
        return taskUnchecked;
    }

    String taskCheckedSymbolOrNull() {
        return taskCheckedSymbol;
    }

    String taskUncheckedSymbolOrNull() {
        return taskUncheckedSymbol;
    }

    /**
     * Creates a new builder.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /** Builder for {@link MarkdownStyles}. */
    public static final class Builder {

        private final Style[] heading = new Style[6];
        private Style strong;
        private Style emphasis;
        private Style strikethrough;
        private Style inlineCode;
        private Style codeBlock;
        private Style link;
        private Style blockquote;
        private Style listMarker;
        private Style html;
        private Style horizontalRule;
        private Style taskChecked;
        private Style taskUnchecked;
        private String blockquotePrefix;
        private String taskCheckedSymbol;
        private String taskUncheckedSymbol;

        private Builder() {
        }

        /**
         * Sets the heading style for the given level.
         *
         * @param level the heading level (1-6)
         * @param style the style to use
         * @return this builder
         */
        public Builder heading(int level, Style style) {
            if (level < 1 || level > 6) {
                throw new IllegalArgumentException("Heading level must be between 1 and 6");
            }
            heading[level - 1] = Objects.requireNonNull(style, "style");
            return this;
        }

        /**
         * Sets the strong (bold) inline style.
         *
         * @param style the style to use
         * @return this builder
         */
        public Builder strong(Style style) {
            this.strong = Objects.requireNonNull(style, "style");
            return this;
        }

        /**
         * Sets the emphasis (italic) inline style.
         *
         * @param style the style to use
         * @return this builder
         */
        public Builder emphasis(Style style) {
            this.emphasis = Objects.requireNonNull(style, "style");
            return this;
        }

        /**
         * Sets the strikethrough inline style.
         *
         * @param style the style to use
         * @return this builder
         */
        public Builder strikethrough(Style style) {
            this.strikethrough = Objects.requireNonNull(style, "style");
            return this;
        }

        /**
         * Sets the inline code style.
         *
         * @param style the style to use
         * @return this builder
         */
        public Builder inlineCode(Style style) {
            this.inlineCode = Objects.requireNonNull(style, "style");
            return this;
        }

        /**
         * Sets the code-block style.
         *
         * @param style the style to use
         * @return this builder
         */
        public Builder codeBlock(Style style) {
            this.codeBlock = Objects.requireNonNull(style, "style");
            return this;
        }

        /**
         * Sets the link style.
         *
         * @param style the style to use
         * @return this builder
         */
        public Builder link(Style style) {
            this.link = Objects.requireNonNull(style, "style");
            return this;
        }

        /**
         * Sets the blockquote style.
         *
         * @param style the style to use
         * @return this builder
         */
        public Builder blockquote(Style style) {
            this.blockquote = Objects.requireNonNull(style, "style");
            return this;
        }

        /**
         * Sets the list-marker style.
         *
         * @param style the style to use
         * @return this builder
         */
        public Builder listMarker(Style style) {
            this.listMarker = Objects.requireNonNull(style, "style");
            return this;
        }

        /**
         * Sets the style applied to raw HTML rendered as escaped text.
         *
         * @param style the style to use
         * @return this builder
         */
        public Builder html(Style style) {
            this.html = Objects.requireNonNull(style, "style");
            return this;
        }

        /**
         * Sets the horizontal-rule style.
         *
         * @param style the style to use
         * @return this builder
         */
        public Builder horizontalRule(Style style) {
            this.horizontalRule = Objects.requireNonNull(style, "style");
            return this;
        }

        /**
         * Sets the prefix string drawn at the start of every blockquote line.
         *
         * @param prefix the prefix string (must not be null)
         * @return this builder
         */
        public Builder blockquotePrefix(String prefix) {
            this.blockquotePrefix = Objects.requireNonNull(prefix, "prefix");
            return this;
        }

        /**
         * Sets the style of the checked task-list marker.
         *
         * @param style the style to use
         * @return this builder
         */
        public Builder taskChecked(Style style) {
            this.taskChecked = Objects.requireNonNull(style, "style");
            return this;
        }

        /**
         * Sets the style of the unchecked task-list marker.
         *
         * @param style the style to use
         * @return this builder
         */
        public Builder taskUnchecked(Style style) {
            this.taskUnchecked = Objects.requireNonNull(style, "style");
            return this;
        }

        /**
         * Sets the symbol drawn for a checked task-list item.
         *
         * @param symbol the checked symbol
         * @return this builder
         */
        public Builder taskCheckedSymbol(String symbol) {
            this.taskCheckedSymbol = Objects.requireNonNull(symbol, "symbol");
            return this;
        }

        /**
         * Sets the symbol drawn for an unchecked task-list item.
         *
         * @param symbol the unchecked symbol
         * @return this builder
         */
        public Builder taskUncheckedSymbol(String symbol) {
            this.taskUncheckedSymbol = Objects.requireNonNull(symbol, "symbol");
            return this;
        }

        /**
         * Builds an immutable {@link MarkdownStyles} instance.
         *
         * @return the built styles
         */
        public MarkdownStyles build() {
            return new MarkdownStyles(this);
        }
    }
}
