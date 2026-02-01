/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.selector;

import dev.tamboui.css.Styleable;
import dev.tamboui.css.cascade.PseudoClassState;

import java.util.List;
import java.util.Objects;

/**
 * An ID selector that matches elements by their unique identifier.
 * <p>
 * Example: {@code #sidebar { ... }} matches the element with id="sidebar".
 */
public final class IdSelector implements Selector {

    private final String id;

    /**
     * Creates an ID selector for the given identifier.
     *
     * @param id the element identifier
     */
    public IdSelector(String id) {
        this.id = Objects.requireNonNull(id);
    }

    /**
     * Returns the identifier this selector matches.
     *
     * @return the element identifier
     */
    public String id() {
        return id;
    }

    @Override
    public int specificity() {
        return 100; // (1, 0, 0)
    }

    @Override
    public boolean matches(Styleable element, PseudoClassState state, List<Styleable> ancestors) {
        return element.cssId().map(id::equals).orElse(false);
    }

    @Override
    public String toCss() {
        return "#" + id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IdSelector)) {
            return false;
        }
        IdSelector that = (IdSelector) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "IdSelector{" + id + "}";
    }
}
