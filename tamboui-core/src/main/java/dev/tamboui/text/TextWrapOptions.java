/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.text;

import java.util.Objects;

import dev.tamboui.style.ContinuationIndent;
import dev.tamboui.style.Overflow;

/**
 * Options configuring text wrapping and overflow handling.
 * <p>
 * Combines the {@link Overflow} mode (e.g. {@link Overflow#WRAP_WORD}, {@link Overflow#CLIP})
 * with the {@link ContinuationIndent} strategy (e.g. {@link ContinuationIndent#HANGING},
 * {@link ContinuationIndent#FLUSH_LEFT}).
 */
public final class TextWrapOptions {

    private static final TextWrapOptions DEFAULT =
            new TextWrapOptions(Overflow.WRAP_WORD, ContinuationIndent.HANGING);

    private final Overflow overflow;
    private final ContinuationIndent continuationIndent;

    private TextWrapOptions(Overflow overflow, ContinuationIndent continuationIndent) {
        this.overflow = Objects.requireNonNull(overflow, "overflow");
        this.continuationIndent = Objects.requireNonNull(continuationIndent, "continuationIndent");
    }

    /**
     * Returns the default wrap options: {@link Overflow#WRAP_WORD} and {@link ContinuationIndent#HANGING}.
     *
     * @return the default wrap options
     */
    public static TextWrapOptions defaults() {
        return DEFAULT;
    }

    /**
     * Creates options with the specified overflow mode and default {@link ContinuationIndent#HANGING}.
     *
     * @param overflow the overflow mode
     * @return the wrap options
     */
    public static TextWrapOptions of(Overflow overflow) {
        if (overflow == null || overflow == Overflow.WRAP_WORD) {
            return DEFAULT;
        }
        return new TextWrapOptions(overflow, ContinuationIndent.HANGING);
    }

    /**
     * Creates options with the specified overflow mode and continuation indent strategy.
     *
     * @param overflow           the overflow mode, or null for default (WRAP_WORD)
     * @param continuationIndent the continuation indent strategy, or null for default (HANGING)
     * @return the wrap options
     */
    public static TextWrapOptions of(Overflow overflow, ContinuationIndent continuationIndent) {
        Overflow ov = overflow != null ? overflow : Overflow.WRAP_WORD;
        ContinuationIndent ci = continuationIndent != null ? continuationIndent : ContinuationIndent.HANGING;
        if (ov == Overflow.WRAP_WORD && ci == ContinuationIndent.HANGING) {
            return DEFAULT;
        }
        return new TextWrapOptions(ov, ci);
    }

    /**
     * Returns the overflow handling mode.
     *
     * @return the overflow mode
     */
    public Overflow overflow() {
        return overflow;
    }

    /**
     * Returns the continuation indent strategy.
     *
     * @return the continuation indent strategy
     */
    public ContinuationIndent continuationIndent() {
        return continuationIndent;
    }

    /**
     * Returns a new {@code TextWrapOptions} with the specified overflow mode.
     *
     * @param overflow the overflow mode
     * @return the updated options
     */
    public TextWrapOptions withOverflow(Overflow overflow) {
        return of(overflow, this.continuationIndent);
    }

    /**
     * Returns a new {@code TextWrapOptions} with the specified continuation indent strategy.
     *
     * @param continuationIndent the continuation indent strategy
     * @return the updated options
     */
    public TextWrapOptions withContinuationIndent(ContinuationIndent continuationIndent) {
        return of(this.overflow, continuationIndent);
    }

    /**
     * Creates a new builder for wrap options.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link TextWrapOptions}.
     */
    public static final class Builder {
        private Overflow overflow = Overflow.WRAP_WORD;
        private ContinuationIndent continuationIndent = ContinuationIndent.HANGING;

        Builder() {
        }

        /**
         * Sets the overflow mode.
         *
         * @param overflow the overflow mode
         * @return this builder
         */
        public Builder overflow(Overflow overflow) {
            this.overflow = Objects.requireNonNull(overflow, "overflow");
            return this;
        }

        /**
         * Sets the continuation indent strategy.
         *
         * @param continuationIndent the continuation indent strategy
         * @return this builder
         */
        public Builder continuationIndent(ContinuationIndent continuationIndent) {
            this.continuationIndent = Objects.requireNonNull(continuationIndent, "continuationIndent");
            return this;
        }

        /**
         * Builds the {@link TextWrapOptions} instance.
         *
         * @return the wrap options
         */
        public TextWrapOptions build() {
            return of(overflow, continuationIndent);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TextWrapOptions)) {
            return false;
        }
        TextWrapOptions that = (TextWrapOptions) o;
        return overflow == that.overflow && continuationIndent == that.continuationIndent;
    }

    @Override
    public int hashCode() {
        return Objects.hash(overflow, continuationIndent);
    }

    @Override
    public String toString() {
        return "TextWrapOptions{"
                + "overflow=" + overflow
                + ", continuationIndent=" + continuationIndent
                + '}';
    }
}
