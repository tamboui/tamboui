/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.selector;

import dev.tamboui.css.Styleable;
import dev.tamboui.css.cascade.PseudoClassState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A compound selector that combines multiple simple selectors.
 * <p>
 * All parts must match for the compound selector to match.
 * Example: {@code Panel.primary#sidebar:focus { ... }}
 */
public final class CompoundSelector implements Selector {

    private final List<Selector> parts;

    /**
     * Creates a compound selector from the given parts.
     *
     * @param parts the selector parts (must not be empty)
     */
    public CompoundSelector(List<Selector> parts) {
        if (parts.isEmpty()) {
            throw new IllegalArgumentException("Compound selector must have at least one part");
        }
        this.parts = Collections.unmodifiableList(new ArrayList<>(parts));
    }

    /**
     * Returns the selector parts that make up this compound selector.
     *
     * @return the unmodifiable list of selector parts
     */
    public List<Selector> parts() {
        return parts;
    }

    @Override
    public int specificity() {
        int sum = 0;
        for (Selector part : parts) {
            sum += part.specificity();
        }
        return sum;
    }

    @Override
    public boolean matches(Styleable element, PseudoClassState state, List<Styleable> ancestors) {
        for (Selector part : parts) {
            if (!part.matches(element, state, ancestors)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toCss() {
        StringBuilder sb = new StringBuilder();
        for (Selector part : parts) {
            sb.append(part.toCss());
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CompoundSelector)) {
            return false;
        }
        CompoundSelector that = (CompoundSelector) o;
        return parts.equals(that.parts);
    }

    @Override
    public int hashCode() {
        return parts.hashCode();
    }

    @Override
    public String toString() {
        return "CompoundSelector{" + toCss() + "}";
    }
}
