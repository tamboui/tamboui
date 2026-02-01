/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Rect;
import dev.tamboui.terminal.Frame;

/**
 * An empty element that takes up space in layouts.
 * Useful for pushing elements apart or creating gaps.
 */
public final class Spacer implements Element {

    private Constraint layoutConstraint = Constraint.fill();

    /** Creates a spacer that fills available space. */
    public Spacer() {
    }

    /**
     * Creates a spacer with a fixed length.
     *
     * @param length the fixed length in cells
     */
    public Spacer(int length) {
        this.layoutConstraint = Constraint.length(length);
    }

    /**
     * Creates a spacer that fills available space.
     *
     * @return a new fill spacer
     */
    public static Spacer fill() {
        return new Spacer();
    }

    /**
     * Creates a spacer with a fixed length.
     *
     * @param length the fixed length in cells
     * @return a new fixed-length spacer
     */
    public static Spacer length(int length) {
        return new Spacer(length);
    }

    /**
     * Sets the constraint to fill available space with given weight.
     *
     * @param weight the fill weight
     * @return this spacer
     */
    public Spacer withWeight(int weight) {
        this.layoutConstraint = Constraint.fill(weight);
        return this;
    }

    /**
     * Sets the constraint to a percentage.
     *
     * @param percent the percentage of available space (0 to 100)
     * @return this spacer
     */
    public Spacer percent(int percent) {
        this.layoutConstraint = Constraint.percentage(percent);
        return this;
    }

    @Override
    public Constraint constraint() {
        return layoutConstraint;
    }

    @Override
    public int preferredWidth() {
        // If spacer has a fixed length constraint, return it
        if (layoutConstraint instanceof Constraint.Length) {
            return ((Constraint.Length) layoutConstraint).value();
        }
        // For fill() and other constraints, return 0 (will expand during layout)
        return 0;
    }

    @Override
    public int preferredHeight() {
        // If spacer has a fixed length constraint, return it
        if (layoutConstraint instanceof Constraint.Length) {
            return ((Constraint.Length) layoutConstraint).value();
        }
        // For fill() and other constraints, return 0 (will expand during layout)
        return 0;
    }

    @Override
    public void render(Frame frame, Rect area, RenderContext context) {
        // Spacer renders nothing
    }
}
