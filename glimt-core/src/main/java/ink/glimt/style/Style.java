/*
 * Copyright (c) 2025 Glimt Contributors
 * SPDX-License-Identifier: MIT
 */
package ink.glimt.style;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;

/**
 * A complete style definition including foreground, background, and modifiers.
 * Styles are immutable and composable.
 */
public final class Style {

    private final Color fg;
    private final Color bg;
    private final Color underlineColor;
    private final EnumSet<Modifier> addModifiers;
    private final EnumSet<Modifier> subModifiers;

    public static final Style EMPTY = new Style(
        null,
        null,
        null,
        EnumSet.noneOf(Modifier.class),
        EnumSet.noneOf(Modifier.class)
    );

    public Style(
        Color fg,
        Color bg,
        Color underlineColor,
        EnumSet<Modifier> addModifiers,
        EnumSet<Modifier> subModifiers
    ) {
        this.fg = fg;
        this.bg = bg;
        this.underlineColor = underlineColor;
        // Defensive copy of mutable EnumSets
        this.addModifiers = EnumSet.copyOf(addModifiers);
        this.subModifiers = EnumSet.copyOf(subModifiers);
    }

    /**
     * Creates a new style builder.
     */
    public static Style create() {
        return EMPTY;
    }

    // Foreground color methods

    /**
     * Returns a new style with the given foreground color.
     *
     * @param color the foreground color, or null to leave unset
     * @return a new style instance
     */
    public Style fg(Color color) {
        return new Style(color, bg, underlineColor, addModifiers, subModifiers);
    }

    /** Shorthand for {@link #fg(Color)} with {@link Color#BLACK}. */
    public Style black() {
        return fg(Color.BLACK);
    }

    /** Shorthand for {@link #fg(Color)} with {@link Color#RED}. */
    public Style red() {
        return fg(Color.RED);
    }

    /** Shorthand for {@link #fg(Color)} with {@link Color#GREEN}. */
    public Style green() {
        return fg(Color.GREEN);
    }

    /** Shorthand for {@link #fg(Color)} with {@link Color#YELLOW}. */
    public Style yellow() {
        return fg(Color.YELLOW);
    }

    /** Shorthand for {@link #fg(Color)} with {@link Color#BLUE}. */
    public Style blue() {
        return fg(Color.BLUE);
    }

    /** Shorthand for {@link #fg(Color)} with {@link Color#MAGENTA}. */
    public Style magenta() {
        return fg(Color.MAGENTA);
    }

    /** Shorthand for {@link #fg(Color)} with {@link Color#CYAN}. */
    public Style cyan() {
        return fg(Color.CYAN);
    }

    /** Shorthand for {@link #fg(Color)} with {@link Color#WHITE}. */
    public Style white() {
        return fg(Color.WHITE);
    }

    /** Shorthand for {@link #fg(Color)} with {@link Color#GRAY}. */
    public Style gray() {
        return fg(Color.GRAY);
    }

    // Background color methods

    /**
     * Returns a new style with the given background color.
     *
     * @param color the background color, or null to leave unset
     * @return a new style instance
     */
    public Style bg(Color color) {
        return new Style(fg, color, underlineColor, addModifiers, subModifiers);
    }

    /** Shorthand for {@link #bg(Color)} with {@link Color#BLACK}. */
    public Style onBlack() {
        return bg(Color.BLACK);
    }

    /** Shorthand for {@link #bg(Color)} with {@link Color#RED}. */
    public Style onRed() {
        return bg(Color.RED);
    }

    /** Shorthand for {@link #bg(Color)} with {@link Color#GREEN}. */
    public Style onGreen() {
        return bg(Color.GREEN);
    }

    /** Shorthand for {@link #bg(Color)} with {@link Color#YELLOW}. */
    public Style onYellow() {
        return bg(Color.YELLOW);
    }

    /** Shorthand for {@link #bg(Color)} with {@link Color#BLUE}. */
    public Style onBlue() {
        return bg(Color.BLUE);
    }

    /** Shorthand for {@link #bg(Color)} with {@link Color#MAGENTA}. */
    public Style onMagenta() {
        return bg(Color.MAGENTA);
    }

    /** Shorthand for {@link #bg(Color)} with {@link Color#CYAN}. */
    public Style onCyan() {
        return bg(Color.CYAN);
    }

    /** Shorthand for {@link #bg(Color)} with {@link Color#WHITE}. */
    public Style onWhite() {
        return bg(Color.WHITE);
    }

    // Underline color

    /**
     * Returns a new style with the given underline color.
     *
     * @param color the underline color, or null to leave unset
     * @return a new style instance
     */
    public Style underlineColor(Color color) {
        return new Style(fg, bg, color, addModifiers, subModifiers);
    }

    // Modifier methods

    /**
     * Returns a new style with the given modifier enabled.
     * If the modifier was previously removed (subtracted), it is added back.
     */
    public Style addModifier(Modifier modifier) {
        EnumSet<Modifier> newAdd = EnumSet.copyOf(addModifiers);
        newAdd.add(modifier);
        EnumSet<Modifier> newSub = EnumSet.copyOf(subModifiers);
        newSub.remove(modifier);
        return new Style(fg, bg, underlineColor, newAdd, newSub);
    }

    /**
     * Returns a new style with the given modifier removed (subtracted).
     * If the modifier was previously added, it is removed.
     */
    public Style removeModifier(Modifier modifier) {
        EnumSet<Modifier> newAdd = EnumSet.copyOf(addModifiers);
        newAdd.remove(modifier);
        EnumSet<Modifier> newSub = EnumSet.copyOf(subModifiers);
        newSub.add(modifier);
        return new Style(fg, bg, underlineColor, newAdd, newSub);
    }

    /** Enables bold text. */
    public Style bold() {
        return addModifier(Modifier.BOLD);
    }

    /** Disables bold text. */
    public Style notBold() {
        return removeModifier(Modifier.BOLD);
    }

    /** Enables dim text. */
    public Style dim() {
        return addModifier(Modifier.DIM);
    }

    /** Disables dim text. */
    public Style notDim() {
        return removeModifier(Modifier.DIM);
    }

    /** Enables italic text. */
    public Style italic() {
        return addModifier(Modifier.ITALIC);
    }

    /** Disables italic text. */
    public Style notItalic() {
        return removeModifier(Modifier.ITALIC);
    }

    /** Enables underline. */
    public Style underlined() {
        return addModifier(Modifier.UNDERLINED);
    }

    /** Disables underline. */
    public Style notUnderlined() {
        return removeModifier(Modifier.UNDERLINED);
    }

    /** Enables slow blink. */
    public Style slowBlink() {
        return addModifier(Modifier.SLOW_BLINK);
    }

    /** Enables rapid blink. */
    public Style rapidBlink() {
        return addModifier(Modifier.RAPID_BLINK);
    }

    /** Enables reverse video. */
    public Style reversed() {
        return addModifier(Modifier.REVERSED);
    }

    /** Disables reverse video. */
    public Style notReversed() {
        return removeModifier(Modifier.REVERSED);
    }

    /** Hides text. */
    public Style hidden() {
        return addModifier(Modifier.HIDDEN);
    }

    /** Unhides text. */
    public Style notHidden() {
        return removeModifier(Modifier.HIDDEN);
    }

    /** Enables strikethrough. */
    public Style crossedOut() {
        return addModifier(Modifier.CROSSED_OUT);
    }

    /** Disables strikethrough. */
    public Style notCrossedOut() {
        return removeModifier(Modifier.CROSSED_OUT);
    }

    /**
     * Combines this style with another. The other style's values override this style's
     * values where they are set; null values in {@code other} leave the current value unchanged.
     *
     * @param other the style to overlay
     * @return combined style
     */
    public Style patch(Style other) {
        Color newFg = other.fg != null ? other.fg : this.fg;
        Color newBg = other.bg != null ? other.bg : this.bg;
        Color newUnderlineColor = other.underlineColor != null ? other.underlineColor : this.underlineColor;

        EnumSet<Modifier> newAddModifiers = EnumSet.copyOf(this.addModifiers);
        newAddModifiers.removeAll(other.subModifiers);
        newAddModifiers.addAll(other.addModifiers);

        EnumSet<Modifier> newSubModifiers = EnumSet.copyOf(this.subModifiers);
        newSubModifiers.removeAll(other.addModifiers);
        newSubModifiers.addAll(other.subModifiers);

        return new Style(newFg, newBg, newUnderlineColor, newAddModifiers, newSubModifiers);
    }

    /**
     * Returns the effective set of modifiers (add - sub).
     */
    public EnumSet<Modifier> effectiveModifiers() {
        EnumSet<Modifier> result = EnumSet.copyOf(addModifiers);
        result.removeAll(subModifiers);
        return result;
    }

    /**
     * Returns the foreground color if set.
     */
    public Optional<Color> fg() {
        return Optional.ofNullable(fg);
    }

    /**
     * Returns the background color if set.
     */
    public Optional<Color> bg() {
        return Optional.ofNullable(bg);
    }

    /**
     * Returns the underline color if set.
     */
    public Optional<Color> underlineColor() {
        return Optional.ofNullable(underlineColor);
    }

    /**
     * Returns the modifiers explicitly added to this style.
     */
    public EnumSet<Modifier> addModifiers() {
        return EnumSet.copyOf(addModifiers);
    }

    /**
     * Returns the modifiers explicitly removed from this style.
     */
    public EnumSet<Modifier> subModifiers() {
        return EnumSet.copyOf(subModifiers);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Style)) {
            return false;
        }
        Style style = (Style) o;
        return Objects.equals(fg, style.fg)
            && Objects.equals(bg, style.bg)
            && Objects.equals(underlineColor, style.underlineColor)
            && addModifiers.equals(style.addModifiers)
            && subModifiers.equals(style.subModifiers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fg, bg, underlineColor, addModifiers, subModifiers);
    }

    @Override
    public String toString() {
        return String.format(
            "Style[fg=%s, bg=%s, underlineColor=%s, addModifiers=%s, subModifiers=%s]",
            fg, bg, underlineColor, addModifiers, subModifiers);
    }
}
