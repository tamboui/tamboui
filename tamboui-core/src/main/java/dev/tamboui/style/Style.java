/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.style;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

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
    private final Map<Class<?>, Object> extensions;
    private final int cachedHashCode;

    /** An empty style with no colors or modifiers set. */
    public static final Style EMPTY = new Style(
        null,
        null,
        null,
        EnumSet.noneOf(Modifier.class),
        EnumSet.noneOf(Modifier.class),
        Collections.emptyMap()
    );

    /**
     * Creates a style with the given colors and modifiers.
     *
     * @param fg the foreground color, or null
     * @param bg the background color, or null
     * @param underlineColor the underline color, or null
     * @param addModifiers the modifiers to add
     * @param subModifiers the modifiers to subtract
     */
    public Style(
        Color fg,
        Color bg,
        Color underlineColor,
        EnumSet<Modifier> addModifiers,
        EnumSet<Modifier> subModifiers
    ) {
        this(fg, bg, underlineColor, addModifiers, subModifiers, Collections.emptyMap());
    }

    /**
     * Creates a style with the given colors, modifiers, and extensions.
     *
     * @param fg the foreground color, or null
     * @param bg the background color, or null
     * @param underlineColor the underline color, or null
     * @param addModifiers the modifiers to add
     * @param subModifiers the modifiers to subtract
     * @param extensions the extensions map
     */
    public Style(
        Color fg,
        Color bg,
        Color underlineColor,
        EnumSet<Modifier> addModifiers,
        EnumSet<Modifier> subModifiers,
        Map<Class<?>, Object> extensions
    ) {
        this.fg = fg;
        this.bg = bg;
        this.underlineColor = underlineColor;
        // Defensive copy of mutable EnumSets
        this.addModifiers = EnumSet.copyOf(addModifiers);
        this.subModifiers = EnumSet.copyOf(subModifiers);
        this.extensions = extensions.isEmpty() ? Collections.emptyMap() : Collections.unmodifiableMap(new HashMap<>(extensions));
        this.cachedHashCode = computeHashCode();
    }

    /**
     * Private constructor that allows skipping defensive copies for internal use.
     * This is used by performance-critical methods like {@link #patch(Style)} that
     * have already created properly isolated EnumSets and Maps.
     *
     * @param fg the foreground color
     * @param bg the background color
     * @param underlineColor the underline color
     * @param addModifiers the modifiers to add (must be an isolated copy)
     * @param subModifiers the modifiers to subtract (must be an isolated copy)
     * @param extensions the extensions map (must be unmodifiable or empty)
     * @param skipCopy marker parameter to distinguish from public constructor
     */
    @SuppressWarnings("unused")
    private Style(
        Color fg,
        Color bg,
        Color underlineColor,
        EnumSet<Modifier> addModifiers,
        EnumSet<Modifier> subModifiers,
        Map<Class<?>, Object> extensions,
        boolean skipCopy
    ) {
        this.fg = fg;
        this.bg = bg;
        this.underlineColor = underlineColor;
        this.addModifiers = addModifiers;
        this.subModifiers = subModifiers;
        this.extensions = extensions;
        this.cachedHashCode = computeHashCode();
    }

    private int computeHashCode() {
        return Objects.hash(fg, bg, underlineColor, addModifiers, subModifiers, extensions);
    }

    /**
     * Creates a new style builder.
     *
     * @return a new empty style
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
        return new Style(color, bg, underlineColor, addModifiers, subModifiers, extensions);
    }

    /** Shorthand for {@link #fg(Color)} with {@link Color#BLACK}.
     * @return a new style with black foreground
     */
    public Style black() {
        return fg(Color.BLACK);
    }

    /** Shorthand for {@link #fg(Color)} with {@link Color#RED}.
     * @return a new style with red foreground
     */
    public Style red() {
        return fg(Color.RED);
    }

    /** Shorthand for {@link #fg(Color)} with {@link Color#GREEN}.
     * @return a new style with green foreground
     */
    public Style green() {
        return fg(Color.GREEN);
    }

    /** Shorthand for {@link #fg(Color)} with {@link Color#YELLOW}.
     * @return a new style with yellow foreground
     */
    public Style yellow() {
        return fg(Color.YELLOW);
    }

    /** Shorthand for {@link #fg(Color)} with {@link Color#BLUE}.
     * @return a new style with blue foreground
     */
    public Style blue() {
        return fg(Color.BLUE);
    }

    /** Shorthand for {@link #fg(Color)} with {@link Color#MAGENTA}.
     * @return a new style with magenta foreground
     */
    public Style magenta() {
        return fg(Color.MAGENTA);
    }

    /** Shorthand for {@link #fg(Color)} with {@link Color#CYAN}.
     * @return a new style with cyan foreground
     */
    public Style cyan() {
        return fg(Color.CYAN);
    }

    /** Shorthand for {@link #fg(Color)} with {@link Color#WHITE}.
     * @return a new style with white foreground
     */
    public Style white() {
        return fg(Color.WHITE);
    }

    /** Shorthand for {@link #fg(Color)} with {@link Color#GRAY}.
     * @return a new style with gray foreground
     */
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
        return new Style(fg, color, underlineColor, addModifiers, subModifiers, extensions);
    }

    /** Shorthand for {@link #bg(Color)} with {@link Color#BLACK}.
     * @return a new style with black background
     */
    public Style onBlack() {
        return bg(Color.BLACK);
    }

    /** Shorthand for {@link #bg(Color)} with {@link Color#RED}.
     * @return a new style with red background
     */
    public Style onRed() {
        return bg(Color.RED);
    }

    /** Shorthand for {@link #bg(Color)} with {@link Color#GREEN}.
     * @return a new style with green background
     */
    public Style onGreen() {
        return bg(Color.GREEN);
    }

    /** Shorthand for {@link #bg(Color)} with {@link Color#YELLOW}.
     * @return a new style with yellow background
     */
    public Style onYellow() {
        return bg(Color.YELLOW);
    }

    /** Shorthand for {@link #bg(Color)} with {@link Color#BLUE}.
     * @return a new style with blue background
     */
    public Style onBlue() {
        return bg(Color.BLUE);
    }

    /** Shorthand for {@link #bg(Color)} with {@link Color#MAGENTA}.
     * @return a new style with magenta background
     */
    public Style onMagenta() {
        return bg(Color.MAGENTA);
    }

    /** Shorthand for {@link #bg(Color)} with {@link Color#CYAN}.
     * @return a new style with cyan background
     */
    public Style onCyan() {
        return bg(Color.CYAN);
    }

    /** Shorthand for {@link #bg(Color)} with {@link Color#WHITE}.
     * @return a new style with white background
     */
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
        return new Style(fg, bg, color, addModifiers, subModifiers, extensions);
    }

    // Modifier methods

    /**
     * Returns a new style with the given modifier enabled.
     * If the modifier was previously removed (subtracted), it is added back.
     *
     * @param modifier the modifier to enable
     * @return a new style with the modifier enabled
     */
    public Style addModifier(Modifier modifier) {
        EnumSet<Modifier> newAdd = EnumSet.copyOf(addModifiers);
        newAdd.add(modifier);
        EnumSet<Modifier> newSub = EnumSet.copyOf(subModifiers);
        newSub.remove(modifier);
        return new Style(fg, bg, underlineColor, newAdd, newSub, extensions);
    }

    /**
     * Returns a new style with the given modifier removed (subtracted).
     * If the modifier was previously added, it is removed.
     *
     * @param modifier the modifier to remove
     * @return a new style with the modifier removed
     */
    public Style removeModifier(Modifier modifier) {
        EnumSet<Modifier> newAdd = EnumSet.copyOf(addModifiers);
        newAdd.remove(modifier);
        EnumSet<Modifier> newSub = EnumSet.copyOf(subModifiers);
        newSub.add(modifier);
        return new Style(fg, bg, underlineColor, newAdd, newSub, extensions);
    }

    /** Enables bold text.
     * @return a new style with bold enabled
     */
    public Style bold() {
        return addModifier(Modifier.BOLD);
    }

    /** Disables bold text.
     * @return a new style with bold disabled
     */
    public Style notBold() {
        return removeModifier(Modifier.BOLD);
    }

    /** Enables dim text.
     * @return a new style with dim enabled
     */
    public Style dim() {
        return addModifier(Modifier.DIM);
    }

    /** Disables dim text.
     * @return a new style with dim disabled
     */
    public Style notDim() {
        return removeModifier(Modifier.DIM);
    }

    /** Enables italic text.
     * @return a new style with italic enabled
     */
    public Style italic() {
        return addModifier(Modifier.ITALIC);
    }

    /** Disables italic text.
     * @return a new style with italic disabled
     */
    public Style notItalic() {
        return removeModifier(Modifier.ITALIC);
    }

    /** Enables underline.
     * @return a new style with underline enabled
     */
    public Style underlined() {
        return addModifier(Modifier.UNDERLINED);
    }

    /** Disables underline.
     * @return a new style with underline disabled
     */
    public Style notUnderlined() {
        return removeModifier(Modifier.UNDERLINED);
    }

    /** Enables slow blink.
     * @return a new style with slow blink enabled
     */
    public Style slowBlink() {
        return addModifier(Modifier.SLOW_BLINK);
    }

    /** Enables rapid blink.
     * @return a new style with rapid blink enabled
     */
    public Style rapidBlink() {
        return addModifier(Modifier.RAPID_BLINK);
    }

    /** Enables reverse video.
     * @return a new style with reverse video enabled
     */
    public Style reversed() {
        return addModifier(Modifier.REVERSED);
    }

    /** Disables reverse video.
     * @return a new style with reverse video disabled
     */
    public Style notReversed() {
        return removeModifier(Modifier.REVERSED);
    }

    /** Hides text.
     * @return a new style with hidden enabled
     */
    public Style hidden() {
        return addModifier(Modifier.HIDDEN);
    }

    /** Unhides text.
     * @return a new style with hidden disabled
     */
    public Style notHidden() {
        return removeModifier(Modifier.HIDDEN);
    }

    /** Enables strikethrough.
     * @return a new style with strikethrough enabled
     */
    public Style crossedOut() {
        return addModifier(Modifier.CROSSED_OUT);
    }

    /** Disables strikethrough.
     * @return a new style with strikethrough disabled
     */
    public Style notCrossedOut() {
        return removeModifier(Modifier.CROSSED_OUT);
    }

    // Hyperlink methods

    /**
     * Returns a new style with the given hyperlink.
     *
     * @param url the URL for the hyperlink
     * @return a new style instance with the hyperlink
     */
    public Style hyperlink(String url) {
        return withExtension(Hyperlink.class, Hyperlink.of(url));
    }

    /**
     * Returns a new style with the given hyperlink and ID.
     * The ID can be used to group multiple cells into a single link.
     *
     * @param url the URL for the hyperlink
     * @param id the optional ID for grouping cells
     * @return a new style instance with the hyperlink
     */
    public Style hyperlink(String url, String id) {
        return withExtension(Hyperlink.class, Hyperlink.of(url, id));
    }

    /**
     * Returns a new style with the given hyperlink.
     *
     * @param hyperlink the hyperlink to attach
     * @return a new style instance with the hyperlink
     */
    public Style hyperlink(Hyperlink hyperlink) {
        return withExtension(Hyperlink.class, hyperlink);
    }

    /**
     * Returns the hyperlink attached to this style, if any.
     *
     * @return the hyperlink, or empty if not set
     */
    public Optional<Hyperlink> hyperlink() {
        return extension(Hyperlink.class);
    }

    // Extension methods for storing additional properties

    /**
     * Returns a new style with the given extension property set.
     * Extensions allow modules to attach additional data to styles without coupling.
     *
     * @param type the extension type (class)
     * @param value the extension value
     * @param <T> the type of the extension
     * @return a new style with the extension set
     */
    public <T> Style withExtension(Class<T> type, T value) {
        Map<Class<?>, Object> newExtensions = new HashMap<>(extensions);
        newExtensions.put(type, value);
        return new Style(fg, bg, underlineColor, addModifiers, subModifiers, newExtensions);
    }

    /**
     * Returns the extension value for the given type, if present.
     *
     * @param type the extension type (class)
     * @param <T> the type of the extension
     * @return the extension value, or empty if not set
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> extension(Class<T> type) {
        return Optional.ofNullable((T) extensions.get(type));
    }

    /**
     * Returns the extension value for the given type, or a default value.
     *
     * @param type the extension type (class)
     * @param defaultValue the default value if not set
     * @param <T> the type of the extension
     * @return the extension value, or the default
     */
    @SuppressWarnings("unchecked")
    public <T> T extension(Class<T> type, T defaultValue) {
        Object value = extensions.get(type);
        return value != null ? (T) value : defaultValue;
    }

    /**
     * Combines this style with another. The other style's values override this style's
     * values where they are set; null values in {@code other} leave the current value unchanged.
     *
     * @param other the style to overlay
     * @return combined style
     */
    public Style patch(Style other) {
        // Fast path: if other is EMPTY, return this unchanged
        if (other == EMPTY) {
            return this;
        }

        // Fast path: if this is EMPTY and other has no sub-modifiers, return other
        if (this == EMPTY && other.subModifiers.isEmpty()) {
            return other;
        }

        Color newFg = other.fg != null ? other.fg : this.fg;
        Color newBg = other.bg != null ? other.bg : this.bg;
        Color newUnderlineColor = other.underlineColor != null ? other.underlineColor : this.underlineColor;

        // Only copy EnumSets if modifications are needed
        EnumSet<Modifier> newAddModifiers;
        EnumSet<Modifier> newSubModifiers;

        if (other.addModifiers.isEmpty() && other.subModifiers.isEmpty()) {
            // No modifier changes - reuse existing sets (safe because EnumSets are defensively copied in constructor)
            newAddModifiers = this.addModifiers;
            newSubModifiers = this.subModifiers;
        } else {
            newAddModifiers = EnumSet.copyOf(this.addModifiers);
            newAddModifiers.removeAll(other.subModifiers);
            newAddModifiers.addAll(other.addModifiers);

            newSubModifiers = EnumSet.copyOf(this.subModifiers);
            newSubModifiers.removeAll(other.addModifiers);
            newSubModifiers.addAll(other.subModifiers);
        }

        // Fast path for extensions
        Map<Class<?>, Object> newExtensions;
        if (this.extensions.isEmpty() && other.extensions.isEmpty()) {
            newExtensions = Collections.emptyMap();
        } else if (other.extensions.isEmpty()) {
            newExtensions = this.extensions;
        } else if (this.extensions.isEmpty()) {
            newExtensions = other.extensions;
        } else {
            // Merge extensions, using Patchable.patch() when applicable
            newExtensions = new HashMap<>(this.extensions);
            for (Map.Entry<Class<?>, Object> entry : other.extensions.entrySet()) {
                Class<?> type = entry.getKey();
                Object otherValue = entry.getValue();
                Object thisValue = this.extensions.get(type);

                if (thisValue instanceof Patchable && otherValue != null) {
                    @SuppressWarnings("unchecked")
                    Patchable<Object> patchable = (Patchable<Object>) thisValue;
                    newExtensions.put(type, patchable.patch(otherValue));
                } else {
                    newExtensions.put(type, otherValue);
                }
            }
        }

        // Use private constructor to avoid redundant defensive copies
        return new Style(newFg, newBg, newUnderlineColor, newAddModifiers, newSubModifiers, newExtensions, false);
    }

    /**
     * Returns the effective set of modifiers (add - sub).
     *
     * @return the effective modifiers
     */
    public EnumSet<Modifier> effectiveModifiers() {
        EnumSet<Modifier> result = EnumSet.copyOf(addModifiers);
        result.removeAll(subModifiers);
        return result;
    }

    /**
     * Returns the foreground color if set.
     *
     * @return the foreground color, or empty if not set
     */
    public Optional<Color> fg() {
        return Optional.ofNullable(fg);
    }

    /**
     * Returns the background color if set.
     *
     * @return the background color, or empty if not set
     */
    public Optional<Color> bg() {
        return Optional.ofNullable(bg);
    }

    /**
     * Returns the underline color if set.
     *
     * @return the underline color, or empty if not set
     */
    public Optional<Color> underlineColor() {
        return Optional.ofNullable(underlineColor);
    }

    /**
     * Returns the modifiers explicitly added to this style.
     *
     * @return the added modifiers
     */
    public EnumSet<Modifier> addModifiers() {
        return EnumSet.copyOf(addModifiers);
    }

    /**
     * Returns the modifiers explicitly removed from this style.
     *
     * @return the subtracted modifiers
     */
    public EnumSet<Modifier> subModifiers() {
        return EnumSet.copyOf(subModifiers);
    }

    /**
     * Returns the style names implied by this style's Named colors and modifiers.
     * <p>
     * Named foreground colors contribute their name (e.g., "red"),
     * Named background colors contribute "bg-" + name (e.g., "bg-red"),
     * and modifiers contribute their {@link Modifier#implicitStyleName()} (e.g., "bold").
     *
     * @return an unmodifiable set of implied CSS class names
     */
    public Set<String> implicitStyleNames() {
        Set<String> classes = new LinkedHashSet<>();
        if (fg instanceof Color.Named) {
            classes.add(((Color.Named) fg).name());
        }
        if (bg instanceof Color.Named) {
            classes.add("bg-" + ((Color.Named) bg).name());
        }
        for (Modifier mod : addModifiers) {
            classes.add(mod.implicitStyleName());
        }
        return Collections.unmodifiableSet(classes);
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
        // Fast inequality check using cached hash
        if (cachedHashCode != style.cachedHashCode) {
            return false;
        }
        return Objects.equals(fg, style.fg)
            && Objects.equals(bg, style.bg)
            && Objects.equals(underlineColor, style.underlineColor)
            && addModifiers.equals(style.addModifiers)
            && subModifiers.equals(style.subModifiers)
            && extensions.equals(style.extensions);
    }

    @Override
    public int hashCode() {
        return cachedHashCode;
    }

    @Override
    public String toString() {
        if (extensions.isEmpty()) {
            return String.format(
                "Style[fg=%s, bg=%s, underlineColor=%s, addModifiers=%s, subModifiers=%s]",
                fg, bg, underlineColor, addModifiers, subModifiers);
        }
        return String.format(
            "Style[fg=%s, bg=%s, underlineColor=%s, addModifiers=%s, subModifiers=%s, extensions=%s]",
            fg, bg, underlineColor, addModifiers, subModifiers, extensions);
    }
}
