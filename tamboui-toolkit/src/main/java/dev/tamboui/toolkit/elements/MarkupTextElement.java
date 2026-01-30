/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import dev.tamboui.css.cascade.CssStyleResolver;
import dev.tamboui.layout.Alignment;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Overflow;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.text.MarkupParser;
import dev.tamboui.text.Text;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.element.StyledElement;

/**
 * A simple inline element that parses BBCode-style markup and displays styled
 * text.
 * <p>
 * This element is similar to {@link TextElement} but accepts markup syntax like
 * {@code [bold]text[/bold]} for styling.
 * <p>
 * For scrollable markup text with borders, see {@link MarkupTextAreaElement}.
 * <p>
 * Supported markup:
 * <ul>
 * <li>Built-in modifiers: {@code [bold]}, {@code [italic]},
 * {@code [underlined]}, {@code [dim]}, {@code [reversed]},
 * {@code [crossed-out]}</li>
 * <li>Built-in colors: {@code [red]}, {@code [green]}, {@code [blue]},
 * {@code [yellow]}, {@code [cyan]}, {@code [magenta]}, {@code [white]},
 * {@code [black]}, {@code [gray]}</li>
 * <li>Hyperlinks: {@code [link=URL]text[/link]}</li>
 * <li>Custom tags: unknown tags are resolved as CSS class names via TCSS</li>
 * <li>Escaped brackets: {@code [[} produces {@code [}, and {@code ]]} produces
 * {@code ]}</li>
 * <li>Nested tags: {@code [red][bold]text[/bold][/red]}</li>
 * </ul>
 * <p>
 * Example usage:
 * 
 * <pre>{@code
 * import static dev.tamboui.toolkit.Toolkit.*;
 *
 * markupText("This is [red]red[/red] and [bold]bold[/bold].")
 *     .centered()
 * }</pre>
 *
 * @see MarkupTextAreaElement for scrollable markup text areas
 * @see MarkupParser for markup syntax details
 * @see RichTextElement for pre-styled text
 */
public final class MarkupTextElement extends StyledElement<MarkupTextElement> {

    private String markup;
    private MarkupParser.StyleResolver customResolver;
    private Text parsedText;
    private boolean textDirty = true;

    // Delegate to RichTextElement for rendering
    private final RichTextElement delegate;

    /**
     * Creates an empty markup text element.
     */
    public MarkupTextElement() {
        this.markup = "";
        this.delegate = new RichTextElement();
    }

    /**
     * Creates a markup text element with the specified content.
     *
     * @param markup
     *            the markup text to parse and display
     */
    public MarkupTextElement(String markup) {
        this.markup = markup != null ? markup : "";
        this.delegate = new RichTextElement();
    }

    /**
     * Sets the markup content.
     *
     * @param markup
     *            the markup text to parse and display
     * @return this element for chaining
     */
    public MarkupTextElement markup(String markup) {
        this.markup = markup != null ? markup : "";
        this.textDirty = true;
        return this;
    }

    /**
     * Returns the current markup content.
     *
     * @return the markup string
     */
    public String markup() {
        return markup;
    }

    /**
     * Returns the parsed Text object.
     *
     * @return the parsed text
     */
    public Text parsedText() {
        ensureTextParsed();
        return parsedText;
    }

    /**
     * Sets a custom style resolver for tags not covered by built-in styles.
     *
     * @param resolver
     *            the custom resolver
     * @return this element for chaining
     */
    public MarkupTextElement customResolver(MarkupParser.StyleResolver resolver) {
        this.customResolver = resolver;
        this.textDirty = true;
        return this;
    }

    /**
     * Sets the overflow mode.
     *
     * @param overflow
     *            the overflow mode
     * @return this element for chaining
     */
    public MarkupTextElement overflow(Overflow overflow) {
        delegate.overflow(overflow);
        return this;
    }

    /**
     * Sets word wrapping mode.
     *
     * @return this element for chaining
     */
    public MarkupTextElement wrapWord() {
        delegate.wrapWord();
        return this;
    }

    /**
     * Sets character wrapping mode.
     *
     * @return this element for chaining
     */
    public MarkupTextElement wrapCharacter() {
        delegate.wrapCharacter();
        return this;
    }

    /**
     * Truncates with ellipsis at the end.
     *
     * @return this element for chaining
     */
    public MarkupTextElement ellipsis() {
        delegate.ellipsis();
        return this;
    }

    /**
     * Sets the text alignment.
     *
     * @param alignment
     *            the alignment
     * @return this element for chaining
     */
    public MarkupTextElement alignment(Alignment alignment) {
        delegate.alignment(alignment);
        return this;
    }

    /**
     * Centers the text horizontally.
     *
     * @return this element for chaining
     */
    public MarkupTextElement centered() {
        delegate.centered();
        return this;
    }

    /**
     * Aligns text to the right.
     *
     * @return this element for chaining
     */
    public MarkupTextElement right() {
        delegate.right();
        return this;
    }

    @Override
    public Constraint constraint() {
        if (layoutConstraint != null) {
            return layoutConstraint;
        }
        return delegate.constraint();
    }

    @Override
    public int preferredWidth() {
        ensureTextParsed();
        return delegate.preferredWidth();
    }

    @Override
    public int preferredHeight() {
        ensureTextParsed();
        return delegate.preferredHeight();
    }

    private void ensureTextParsed() {
        if (textDirty || parsedText == null) {
            MarkupParser.StyleResolver resolver = createSimpleResolver();
            parsedText = MarkupParser.parse(markup, resolver);
            delegate.text(parsedText);
            textDirty = false;
        }
    }

    private MarkupParser.StyleResolver createSimpleResolver() {
        return tagName -> {
            if (customResolver != null) {
                return customResolver.resolve(tagName);
            }
            return null;
        };
    }

    private MarkupParser.StyleResolver createCombinedResolver(RenderContext context) {
        return tagName -> {
            if (customResolver != null) {
                Style customStyle = customResolver.resolve(tagName);
                if (customStyle != null) {
                    return customStyle;
                }
            }

            // Check TCSS via context (unknown tags are treated as CSS class names)
            // Use resolveStyle with the tag name as a CSS class
            return context.resolveStyle(null, tagName).map(CssStyleResolver::toStyle).orElse(null);
        };
    }

    @Override
    protected void renderContent(Frame frame, Rect area, RenderContext context) {
        // Always parse with the combined resolver that supports TCSS
        // (context may have a StyleEngine with CSS classes for custom tags)
        MarkupParser.StyleResolver combinedResolver = createCombinedResolver(context);
        parsedText = MarkupParser.parse(markup, combinedResolver);
        textDirty = false;

        delegate.text(parsedText);
        delegate.style(this.style);

        // Render using the delegate
        delegate.render(frame, area, context);
    }
}
