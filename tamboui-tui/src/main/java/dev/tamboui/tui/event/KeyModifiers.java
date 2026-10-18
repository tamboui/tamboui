/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.event;

/**
 * Represents keyboard modifier keys (Ctrl, Alt, Shift, Super/Command, Hyper, Meta).
 */
public final class KeyModifiers {

    /** No modifiers pressed. */
    public static final KeyModifiers NONE = new KeyModifiers(false, false, false, false, false, false);

    /** Only Ctrl pressed. */
    public static final KeyModifiers CTRL = new KeyModifiers(true, false, false, false, false, false);

    /** Only Alt pressed. */
    public static final KeyModifiers ALT = new KeyModifiers(false, true, false, false, false, false);

    /** Only Shift pressed. */
    public static final KeyModifiers SHIFT = new KeyModifiers(false, false, true, false, false, false);

    /** Only Super (Command on macOS, Windows key on PC) pressed. */
    public static final KeyModifiers SUPER = new KeyModifiers(false, false, false, true, false, false);

    /** Only Hyper pressed. */
    public static final KeyModifiers HYPER = new KeyModifiers(false, false, false, false, true, false);

    /** Only Meta pressed. */
    public static final KeyModifiers META = new KeyModifiers(false, false, false, false, false, true);

    private final boolean ctrl;
    private final boolean alt;
    private final boolean shift;
    private final boolean superKey;
    private final boolean hyper;
    private final boolean meta;

    /**
     * Creates a new KeyModifiers instance with the standard modifier flags.
     *
     * @param ctrl  true if Ctrl is pressed
     * @param alt   true if Alt is pressed
     * @param shift true if Shift is pressed
     */
    public KeyModifiers(boolean ctrl, boolean alt, boolean shift) {
        this(ctrl, alt, shift, false, false, false);
    }

    /**
     * Creates a new KeyModifiers instance with all supported modifier flags.
     *
     * @param ctrl     true if Ctrl is pressed
     * @param alt      true if Alt is pressed
     * @param shift    true if Shift is pressed
     * @param superKey true if Super (Command/Windows) is pressed
     * @param hyper    true if Hyper is pressed
     * @param meta     true if Meta is pressed
     */
    public KeyModifiers(boolean ctrl, boolean alt, boolean shift, boolean superKey, boolean hyper, boolean meta) {
        this.ctrl = ctrl;
        this.alt = alt;
        this.shift = shift;
        this.superKey = superKey;
        this.hyper = hyper;
        this.meta = meta;
    }

    /**
     * Creates modifiers with standard flags.
     *
     * @param ctrl  true if Ctrl is pressed
     * @param alt   true if Alt is pressed
     * @param shift true if Shift is pressed
     * @return the key modifiers instance
     */
    public static KeyModifiers of(boolean ctrl, boolean alt, boolean shift) {
        return of(ctrl, alt, shift, false, false, false);
    }

    /**
     * Creates modifiers with all supported flags.
     *
     * @param ctrl     true if Ctrl is pressed
     * @param alt      true if Alt is pressed
     * @param shift    true if Shift is pressed
     * @param superKey true if Super (Command/Windows) is pressed
     * @param hyper    true if Hyper is pressed
     * @param meta     true if Meta is pressed
     * @return the key modifiers instance
     */
    public static KeyModifiers of(boolean ctrl, boolean alt, boolean shift, boolean superKey, boolean hyper, boolean meta) {
        if (!ctrl && !alt && !shift && !superKey && !hyper && !meta) {
            return NONE;
        }
        return new KeyModifiers(ctrl, alt, shift, superKey, hyper, meta);
    }

    /**
     * Returns whether the Ctrl modifier is active.
     *
     * @return true if Ctrl is pressed
     */
    public boolean ctrl() {
        return ctrl;
    }

    /**
     * Returns whether the Alt modifier is active.
     *
     * @return true if Alt is pressed
     */
    public boolean alt() {
        return alt;
    }

    /**
     * Returns whether the Shift modifier is active.
     *
     * @return true if Shift is pressed
     */
    public boolean shift() {
        return shift;
    }

    /**
     * Returns whether the Super (Command/Windows) modifier is active.
     *
     * @return true if Super is pressed
     */
    public boolean superKey() {
        return superKey;
    }

    /**
     * Returns whether the Hyper modifier is active.
     *
     * @return true if Hyper is pressed
     */
    public boolean hyper() {
        return hyper;
    }

    /**
     * Returns whether the Meta modifier is active.
     *
     * @return true if Meta is pressed
     */
    public boolean meta() {
        return meta;
    }

    /**
     * Returns true if no modifiers are pressed.
     *
     * @return true if no modifiers are active
     */
    public boolean isEmpty() {
        return !ctrl && !alt && !shift && !superKey && !hyper && !meta;
    }

    /**
     * Returns true if any modifier is pressed.
     *
     * @return true if at least one modifier is active
     */
    public boolean hasAny() {
        return ctrl || alt || shift || superKey || hyper || meta;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof KeyModifiers)) {
            return false;
        }
        KeyModifiers that = (KeyModifiers) o;
        return ctrl == that.ctrl
                && alt == that.alt
                && shift == that.shift
                && superKey == that.superKey
                && hyper == that.hyper
                && meta == that.meta;
    }

    @Override
    public int hashCode() {
        int result = Boolean.hashCode(ctrl);
        result = 31 * result + Boolean.hashCode(alt);
        result = 31 * result + Boolean.hashCode(shift);
        result = 31 * result + Boolean.hashCode(superKey);
        result = 31 * result + Boolean.hashCode(hyper);
        result = 31 * result + Boolean.hashCode(meta);
        return result;
    }

    @Override
    public String toString() {
        return String.format(
                "KeyModifiers[ctrl=%s, alt=%s, shift=%s, super=%s, hyper=%s, meta=%s]",
                ctrl, alt, shift, superKey, hyper, meta);
    }

    /**
     * Returns a new builder for constructing {@link KeyModifiers}.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link KeyModifiers}.
     */
    public static final class Builder {
        private boolean ctrl;
        private boolean alt;
        private boolean shift;
        private boolean superKey;
        private boolean hyper;
        private boolean meta;

        private Builder() {
        }

        /**
         * Sets whether the Ctrl modifier is active.
         *
         * @param ctrl true if Ctrl is pressed
         * @return this builder
         */
        public Builder ctrl(boolean ctrl) {
            this.ctrl = ctrl;
            return this;
        }

        /**
         * Sets whether the Alt modifier is active.
         *
         * @param alt true if Alt is pressed
         * @return this builder
         */
        public Builder alt(boolean alt) {
            this.alt = alt;
            return this;
        }

        /**
         * Sets whether the Shift modifier is active.
         *
         * @param shift true if Shift is pressed
         * @return this builder
         */
        public Builder shift(boolean shift) {
            this.shift = shift;
            return this;
        }

        /**
         * Sets whether the Super modifier is active.
         *
         * @param superKey true if Super is pressed
         * @return this builder
         */
        public Builder superKey(boolean superKey) {
            this.superKey = superKey;
            return this;
        }

        /**
         * Sets whether the Hyper modifier is active.
         *
         * @param hyper true if Hyper is pressed
         * @return this builder
         */
        public Builder hyper(boolean hyper) {
            this.hyper = hyper;
            return this;
        }

        /**
         * Sets whether the Meta modifier is active.
         *
         * @param meta true if Meta is pressed
         * @return this builder
         */
        public Builder meta(boolean meta) {
            this.meta = meta;
            return this;
        }

        /**
         * Builds the key modifiers.
         *
         * @return the constructed KeyModifiers
         */
        public KeyModifiers build() {
            return KeyModifiers.of(ctrl, alt, shift, superKey, hyper, meta);
        }
    }
}
