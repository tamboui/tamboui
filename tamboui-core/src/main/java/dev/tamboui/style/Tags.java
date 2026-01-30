/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.style;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A collection of tag names associated with a Style.
 * <p>
 * Tags are preserved from markup parsing and can be used as CSS class names for
 * TFX effects targeting. When styles are patched together, tags are
 * automatically merged (unioned) due to the {@link Patchable} implementation.
 * <p>
 * Example usage:
 * 
 * <pre>{@code
 * // Create a style with tags
 * Style style = Style.EMPTY.withExtension(Tags.class, Tags.of("bold", "highlight"));
 *
 * // Retrieve tags from a style
 * Tags tags = style.extension(Tags.class, Tags.empty());
 * for (String tag : tags.values()) {
 *     element.addClass(tag);
 * }
 * }</pre>
 *
 * @see Patchable
 * @see Style#withExtension(Class, Object)
 */
public final class Tags implements Patchable<Tags> {

    private static final Tags EMPTY = new Tags(Collections.emptySet());

    private final Set<String> values;

    private Tags(Set<String> values) {
        this.values = values;
    }

    /**
     * Returns an empty Tags instance.
     *
     * @return an empty Tags
     */
    public static Tags empty() {
        return EMPTY;
    }

    /**
     * Creates a Tags instance with a single tag.
     *
     * @param tag
     *            the tag name
     * @return a new Tags instance
     */
    public static Tags of(String tag) {
        if (tag == null || tag.isEmpty()) {
            return EMPTY;
        }
        return new Tags(Collections.singleton(tag));
    }

    /**
     * Creates a Tags instance with multiple tags.
     *
     * @param tags
     *            the tag names
     * @return a new Tags instance
     */
    public static Tags of(String... tags) {
        if (tags == null || tags.length == 0) {
            return EMPTY;
        }
        Set<String> set = new HashSet<>(Arrays.asList(tags));
        set.remove(null);
        set.remove("");
        if (set.isEmpty()) {
            return EMPTY;
        }
        return new Tags(Collections.unmodifiableSet(set));
    }

    /**
     * Creates a Tags instance from a set of tags.
     *
     * @param tags
     *            the tag names
     * @return a new Tags instance
     */
    public static Tags of(Set<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return EMPTY;
        }
        Set<String> set = new HashSet<>(tags);
        set.remove(null);
        set.remove("");
        if (set.isEmpty()) {
            return EMPTY;
        }
        return new Tags(Collections.unmodifiableSet(set));
    }

    /**
     * Returns the set of tag values.
     *
     * @return unmodifiable set of tag names
     */
    public Set<String> values() {
        return values;
    }

    /**
     * Returns whether this Tags instance is empty.
     *
     * @return true if there are no tags
     */
    public boolean isEmpty() {
        return values.isEmpty();
    }

    /**
     * Returns whether this Tags instance contains the given tag.
     *
     * @param tag
     *            the tag to check for
     * @return true if the tag is present
     */
    public boolean contains(String tag) {
        return values.contains(tag);
    }

    @Override
    public Tags patch(Tags other) {
        if (other == null || other.isEmpty()) {
            return this;
        }
        if (this.isEmpty()) {
            return other;
        }
        // Union the tag sets
        Set<String> merged = new HashSet<>(this.values);
        merged.addAll(other.values);
        return new Tags(Collections.unmodifiableSet(merged));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Tags)) {
            return false;
        }
        Tags tags = (Tags) o;
        return values.equals(tags.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(values);
    }

    @Override
    public String toString() {
        return "Tags" + values;
    }
}
