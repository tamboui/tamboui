/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.style;

/**
 * Interface for style extensions that support custom merge behavior during
 * {@link Style#patch(Style)}.
 * <p>
 * When a {@code Style} is patched with another that contains an extension of
 * the same type, if the existing extension implements {@code Patchable}, its
 * {@link #patch(Object)} method is called to merge the two values rather than
 * simply replacing the old value with the new one.
 * <p>
 * Example usage for accumulating tags:
 * 
 * <pre>{@code
 * public class Tags implements Patchable<Tags> {
 *     private final Set<String> values;
 *
 *     @Override
 *     public Tags patch(Tags other) {
 *         if (other == null)
 *             return this;
 *         Set<String> merged = new HashSet<>(this.values);
 *         merged.addAll(other.values);
 *         return new Tags(merged);
 *     }
 * }
 * }</pre>
 *
 * @param <T>
 *            the type of the patchable value (should be the implementing class)
 */
@FunctionalInterface
public interface Patchable<T> {
    /**
     * Merges this value with another value of the same type.
     * <p>
     * This method is called during {@link Style#patch(Style)} when both styles
     * contain an extension of this type.
     *
     * @param other
     *            the other value to merge with (may be null)
     * @return the merged result
     */
    T patch(T other);
}
