/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.markdown;

import java.util.Objects;

import dev.tamboui.layout.Rect;
import dev.tamboui.markdown.MarkdownStyles;
import dev.tamboui.markdown.MarkdownView;
import dev.tamboui.style.Overflow;
import dev.tamboui.style.Style;
import dev.tamboui.style.StylePropertyResolver;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.element.Size;
import dev.tamboui.toolkit.element.StyledElement;

/**
 * Toolkit DSL element rendering CommonMark + GFM markdown via
 * {@link MarkdownView}. Inherits the standard {@code StyledElement} fluent
 * API (color, modifiers, layout constraints, CSS classes) and adds
 * markdown-specific options.
 *
 * <p>CSS rules attached to {@code MarkdownElement} (or any user-supplied
 * type/id/class) flow through to {@link MarkdownView}, so the same
 * {@code heading-1-color}, {@code link-color}, {@code blockquote-prefix}
 * and so on documented on {@link MarkdownView} apply here.
 *
 * <p>Example:
 * <pre>{@code
 * import static dev.tamboui.toolkit.Toolkit.*;
 * import static dev.tamboui.toolkit.markdown.MarkdownElement.markdown;
 *
 * panel("README",
 *     markdown(readme).overflow(Overflow.WRAP_WORD).fill()
 * ).rounded();
 * }</pre>
 */
public final class MarkdownElement extends StyledElement<MarkdownElement> {

    private String source;
    private MarkdownStyles styles;
    private Overflow overflow;
    private int scroll;

    private MarkdownElement(String source) {
        this.source = Objects.requireNonNull(source, "source");
    }

    /**
     * Creates a new element rendering the given markdown source.
     *
     * @param source the markdown text
     * @return a new element
     */
    public static MarkdownElement of(String source) {
        return new MarkdownElement(source);
    }

    /**
     * Alias of {@link #of(String)} intended for static imports.
     *
     * @param source the markdown text
     * @return a new element
     */
    public static MarkdownElement markdown(String source) {
        return new MarkdownElement(source);
    }

    /**
     * Replaces the markdown source. Useful for re-binding to a streaming
     * buffer on every frame.
     *
     * @param source the markdown text
     * @return this element for chaining
     */
    public MarkdownElement source(String source) {
        this.source = Objects.requireNonNull(source, "source");
        return this;
    }

    /**
     * Sets the programmatic style overrides. Each slot left unset falls back
     * to CSS, then to the built-in default.
     *
     * @param styles the style palette
     * @return this element for chaining
     */
    public MarkdownElement styles(MarkdownStyles styles) {
        this.styles = Objects.requireNonNull(styles, "styles");
        return this;
    }

    /**
     * Sets the overflow mode for prose lines wider than the content area.
     *
     * @param overflow the overflow mode
     * @return this element for chaining
     */
    public MarkdownElement overflow(Overflow overflow) {
        this.overflow = Objects.requireNonNull(overflow, "overflow");
        return this;
    }

    /**
     * Sets the index of the first visible content row.
     *
     * @param scroll first visible row, &gt;= 0
     * @return this element for chaining
     */
    public MarkdownElement scroll(int scroll) {
        this.scroll = Math.max(0, scroll);
        return this;
    }

    /**
     * Returns the current markdown source.
     *
     * @return the markdown source
     */
    public String source() {
        return source;
    }

    @Override
    public Size preferredSize(int availableWidth, int availableHeight, RenderContext context) {
        if (availableWidth <= 0) {
            return Size.UNKNOWN;
        }
        Style effectiveStyle = context != null ? context.currentStyle() : Style.EMPTY;
        StylePropertyResolver resolver = context != null
            ? context.resolveStyle(this).map(r -> (StylePropertyResolver) r).orElse(StylePropertyResolver.empty())
            : StylePropertyResolver.empty();
        MarkdownView view = buildView(effectiveStyle, resolver);
        return Size.of(availableWidth, view.computeHeight(availableWidth));
    }

    @Override
    protected void renderContent(Frame frame, Rect area, RenderContext context) {
        Style effectiveStyle = context.currentStyle();
        StylePropertyResolver resolver = context.resolveStyle(this)
            .map(r -> (StylePropertyResolver) r)
            .orElse(StylePropertyResolver.empty());
        frame.renderWidget(buildView(effectiveStyle, resolver), area);
    }

    private MarkdownView buildView(Style effectiveStyle, StylePropertyResolver resolver) {
        MarkdownView.Builder b = MarkdownView.builder()
            .source(source)
            .style(effectiveStyle)
            .styleResolver(resolver)
            .scroll(scroll);
        if (styles != null) {
            b.styles(styles);
        }
        if (overflow != null) {
            b.overflow(overflow);
        }
        return b.build();
    }
}
