/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Rect;
import dev.tamboui.terminal.Frame;

/// An empty element that takes up space in layouts.
/// Useful for pushing elements apart or creating gaps.
public final class Spacer implements Element {

    private Constraint layoutConstraint = Constraint.fill();

    public Spacer() {
    }

    public Spacer(int length) {
        this.layoutConstraint = Constraint.length(length);
    }

    /// Creates a spacer that fills available space.
    public static Spacer fill() {
        return new Spacer();
    }

    /// Creates a spacer with a fixed length.
    public static Spacer length(int length) {
        return new Spacer(length);
    }

    /// Sets the constraint to fill available space with given weight.
    public Spacer withWeight(int weight) {
        this.layoutConstraint = Constraint.fill(weight);
        return this;
    }

    /// Sets the constraint to a percentage.
    public Spacer percent(int percent) {
        this.layoutConstraint = Constraint.percentage(percent);
        return this;
    }

    @Override
    public Constraint constraint() {
        return layoutConstraint;
    }

    @Override
    public void render(Frame frame, Rect area, RenderContext context) {
        // Spacer renders nothing
    }
}

