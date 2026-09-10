/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.syntax;

import java.util.Objects;

import dev.tamboui.style.Color;
import dev.tamboui.style.Style;

/**
 * A named palette of styles applied to highlighted source-code tokens, in the
 * same spirit as a highlight.js or VS Code theme. Each {@link TokenType} maps
 * to a {@link Style} (typically just a foreground {@link Color}). A token whose
 * style the theme does not override falls back to the enclosing code-block
 * style, so themes can be sparse.
 *
 * <p>An instance is immutable and can be shared. Build a customized theme with
 * {@link #builder()} starting from {@link #DEFAULTS} when a tailored look is
 * wanted:
 * <pre>{@code
 * SyntaxTheme theme = SyntaxTheme.builder()
 *     .token(TokenType.KEYWORD, Style.EMPTY.fg(Color.RED).bold())
 *     .token(TokenType.STRING, Style.EMPTY.fg(Color.GREEN))
 *     .build();
 * }</pre>
 */
public final class SyntaxTheme {

    /**
     * The default theme, a dark, high-contrast palette inspired by common
     * IDE defaults (One Dark / Dark+). Every {@link TokenType} gets a
     * distinct foreground color; none carries a modifier.
     */
    public static final SyntaxTheme DEFAULTS = builder().build();

    private final Style[] tokenStyles;

    private SyntaxTheme(Builder builder) {
        this.tokenStyles = builder.tokenStyles.clone();
    }

    /**
     * Returns the style for a token type, or {@code null} when the theme does
     * not override that type (the caller should fall back to the code-block
     * base style).
     *
     * @param type the token type
     * @return the token style, or {@code null} if unset
     */
    Style styleOrNull(TokenType type) {
        return tokenStyles[type.ordinal()];
    }

    /**
     * Returns the style for a token type. When the theme does not override the
     * type, the provided {@code fallback} is returned unchanged.
     *
     * @param type the token type
     * @param fallback the style to return when the type has no theme entry
     * @return the token style overriding {@code fallback} when set, otherwise {@code fallback}
     */
    public Style style(TokenType type, Style fallback) {
        Style overridden = tokenStyles[type.ordinal()];
        return overridden != null ? fallback.patch(overridden) : fallback;
    }

    /**
     * Creates a new builder pre-populated with the {@link #DEFAULTS} palette.
     *
     * @return a new builder
     */
    public static Builder builder() {
        Builder b = new Builder();
        b.tokenStyles[TokenType.COMMENT.ordinal()] = Style.EMPTY.fg(Color.hex("#6e7681")).italic();
        b.tokenStyles[TokenType.KEYWORD.ordinal()] = Style.EMPTY.fg(Color.hex("#c678dd"));
        b.tokenStyles[TokenType.TYPE.ordinal()] = Style.EMPTY.fg(Color.hex("#56b6c2"));
        b.tokenStyles[TokenType.STRING.ordinal()] = Style.EMPTY.fg(Color.hex("#98c379"));
        b.tokenStyles[TokenType.NUMBER.ordinal()] = Style.EMPTY.fg(Color.hex("#d19a66"));
        b.tokenStyles[TokenType.CONSTANT.ordinal()] = Style.EMPTY.fg(Color.hex("#d19a66"));
        b.tokenStyles[TokenType.FUNCTION.ordinal()] = Style.EMPTY.fg(Color.hex("#61afef"));
        b.tokenStyles[TokenType.ANNOTATION.ordinal()] = Style.EMPTY.fg(Color.hex("#e5c07b"));
        b.tokenStyles[TokenType.OPERATOR.ordinal()] = Style.EMPTY.fg(Color.hex("#abb2bf"));
        b.tokenStyles[TokenType.PUNCTUATION.ordinal()] = Style.EMPTY.fg(Color.hex("#5c6370"));
        b.tokenStyles[TokenType.TAG.ordinal()] = Style.EMPTY.fg(Color.hex("#e06c75"));
        b.tokenStyles[TokenType.ATTRIBUTE.ordinal()] = Style.EMPTY.fg(Color.hex("#d19a66"));
        return b;
    }

    /** Builder for {@link SyntaxTheme}. */
    public static final class Builder {

        private final Style[] tokenStyles = new Style[TokenType.values().length];

        private Builder() {
        }

        /**
         * Sets the style for a token type.
         *
         * @param type the token type
         * @param style the style to apply (typically a foreground color)
         * @return this builder
         */
        public Builder token(TokenType type, Style style) {
            tokenStyles[type.ordinal()] = Objects.requireNonNull(style, "style");
            return this;
        }

        /**
         * Clears the override for a token type so it falls back to the
         * code-block base style.
         *
         * @param type the token type
         * @return this builder
         */
        public Builder clear(TokenType type) {
            tokenStyles[type.ordinal()] = null;
            return this;
        }

        /**
         * Builds an immutable {@link SyntaxTheme}.
         *
         * @return the built theme
         */
        public SyntaxTheme build() {
            return new SyntaxTheme(this);
        }
    }
}