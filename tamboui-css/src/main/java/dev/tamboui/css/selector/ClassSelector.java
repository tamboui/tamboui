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
 * A class selector that matches elements by their CSS class.
 * <p>
 * Example: {@code .primary { ... }} matches elements with class="primary".
 */
public final class ClassSelector implements Selector {

    private final String className;

    /**
     * Creates a class selector for the given CSS class name.
     *
     * @param className the CSS class name
     */
    public ClassSelector(String className) {
        this.className = Objects.requireNonNull(className);
    }

    /**
     * Returns the CSS class name this selector matches.
     *
     * @return the CSS class name
     */
    public String className() {
        return className;
    }

    @Override
    public int specificity() {
        return 10; // (0, 1, 0)
    }

    @Override
    public boolean matches(Styleable element, PseudoClassState state, List<Styleable> ancestors) {
        return element.cssClasses().contains(className);
    }

    @Override
    public String toCss() {
        return "." + className;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ClassSelector)) {
            return false;
        }
        ClassSelector that = (ClassSelector) o;
        return className.equals(that.className);
    }

    @Override
    public int hashCode() {
        return className.hashCode();
    }

    @Override
    public String toString() {
        return "ClassSelector{" + className + "}";
    }
}
