/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.cascade;

import java.util.Objects;

/// Represents the current pseudo-class state of an element.
///
///
///
/// This is used during selector matching to determine if pseudo-class
/// selectors like :focus, :hover, :disabled match the element.
///
///
///
/// For positional pseudo-classes like :nth-child(even), the nthChild field
/// stores the 1-based position (0 means position is not set).
public final class PseudoClassState {

    /// Empty state with all flags set to false.
    public static final PseudoClassState NONE = new PseudoClassState(
            false, false, false, false, false, false, false, 0
    );

    private final boolean focused;
    private final boolean hovered;
    private final boolean disabled;
    private final boolean active;
    private final boolean selected;
    private final boolean firstChild;
    private final boolean lastChild;
    private final int nthChild; // 1-based position, 0 means not set

    /// @deprecated Use the constructor with nthChild parameter instead.
    @Deprecated
    public PseudoClassState(boolean focused,
                            boolean hovered,
                            boolean disabled,
                            boolean active,
                            boolean selected,
                            boolean firstChild,
                            boolean lastChild) {
        this(focused, hovered, disabled, active, selected, firstChild, lastChild, 0);
    }

    public PseudoClassState(boolean focused,
                            boolean hovered,
                            boolean disabled,
                            boolean active,
                            boolean selected,
                            boolean firstChild,
                            boolean lastChild,
                            int nthChild) {
        this.focused = focused;
        this.hovered = hovered;
        this.disabled = disabled;
        this.active = active;
        this.selected = selected;
        this.firstChild = firstChild;
        this.lastChild = lastChild;
        this.nthChild = nthChild;
    }

    /// Creates a state with only the focused flag set.
    public static PseudoClassState ofFocused() {
        return new PseudoClassState(true, false, false, false, false, false, false);
    }

    /// Creates a state with only the hovered flag set.
    public static PseudoClassState ofHovered() {
        return new PseudoClassState(false, true, false, false, false, false, false);
    }

    /// Creates a state with only the disabled flag set.
    public static PseudoClassState ofDisabled() {
        return new PseudoClassState(false, false, true, false, false, false, false);
    }

    /// Creates a state with only the selected flag set.
    public static PseudoClassState ofSelected() {
        return new PseudoClassState(false, false, false, false, true, false, false);
    }

    public boolean isFocused() {
        return focused;
    }

    public boolean isHovered() {
        return hovered;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isSelected() {
        return selected;
    }

    public boolean isFirstChild() {
        return firstChild;
    }

    public boolean isLastChild() {
        return lastChild;
    }

    /// Returns the 1-based child position, or 0 if not set.
    public int nthChild() {
        return nthChild;
    }

    /// Returns a new state with the focused flag set.
    public PseudoClassState withFocused(boolean focused) {
        return new PseudoClassState(focused, hovered, disabled, active, selected, firstChild, lastChild, nthChild);
    }

    /// Returns a new state with the hovered flag set.
    public PseudoClassState withHovered(boolean hovered) {
        return new PseudoClassState(focused, hovered, disabled, active, selected, firstChild, lastChild, nthChild);
    }

    /// Returns a new state with the disabled flag set.
    public PseudoClassState withDisabled(boolean disabled) {
        return new PseudoClassState(focused, hovered, disabled, active, selected, firstChild, lastChild, nthChild);
    }

    /// Returns a new state with the active flag set.
    public PseudoClassState withActive(boolean active) {
        return new PseudoClassState(focused, hovered, disabled, active, selected, firstChild, lastChild, nthChild);
    }

    /// Returns a new state with the selected flag set.
    public PseudoClassState withSelected(boolean selected) {
        return new PseudoClassState(focused, hovered, disabled, active, selected, firstChild, lastChild, nthChild);
    }

    /// Returns a new state with the firstChild flag set.
    public PseudoClassState withFirstChild(boolean firstChild) {
        return new PseudoClassState(focused, hovered, disabled, active, selected, firstChild, lastChild, nthChild);
    }

    /// Returns a new state with the lastChild flag set.
    public PseudoClassState withLastChild(boolean lastChild) {
        return new PseudoClassState(focused, hovered, disabled, active, selected, firstChild, lastChild, nthChild);
    }

    /// Returns a new state with the nthChild position set.
    ///
    /// @param nthChild the 1-based child position
    public PseudoClassState withNthChild(int nthChild) {
        return new PseudoClassState(focused, hovered, disabled, active, selected, firstChild, lastChild, nthChild);
    }

    /// Checks if the given pseudo-class is active.
    ///
    ///
    ///
    /// Supports simple pseudo-classes (focus, hover, etc.) and functional
    /// pseudo-classes like nth-child(even) and nth-child(odd).
    ///
    /// @param pseudoClass the pseudo-class name (without colon)
    /// @return true if the pseudo-class is active
    public boolean has(String pseudoClass) {
        switch (pseudoClass) {
            case "focus":
                return focused;
            case "hover":
                return hovered;
            case "disabled":
                return disabled;
            case "active":
                return active;
            case "selected":
                return selected;
            case "first-child":
                return firstChild;
            case "last-child":
                return lastChild;
            case "nth-child(even)":
                return nthChild > 0 && nthChild % 2 == 0;
            case "nth-child(odd)":
                return nthChild > 0 && nthChild % 2 == 1;
            default:
                return false;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PseudoClassState)) {
            return false;
        }
        PseudoClassState that = (PseudoClassState) o;
        return focused == that.focused &&
                hovered == that.hovered &&
                disabled == that.disabled &&
                active == that.active &&
                selected == that.selected &&
                firstChild == that.firstChild &&
                lastChild == that.lastChild &&
                nthChild == that.nthChild;
    }

    @Override
    public int hashCode() {
        return Objects.hash(focused, hovered, disabled, active, selected, firstChild, lastChild, nthChild);
    }

    @Override
    public String toString() {
        return "PseudoClassState{" +
                "focused=" + focused +
                ", hovered=" + hovered +
                ", disabled=" + disabled +
                ", active=" + active +
                ", selected=" + selected +
                ", firstChild=" + firstChild +
                ", lastChild=" + lastChild +
                ", nthChild=" + nthChild +
                '}';
    }
}

