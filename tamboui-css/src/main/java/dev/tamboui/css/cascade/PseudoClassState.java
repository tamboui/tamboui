/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.cascade;

import java.util.Objects;

/**
 * Represents the current pseudo-class state of an element.
 * <p>
 * This is used during selector matching to determine if pseudo-class
 * selectors like :focus, :hover, :disabled match the element.
 * <p>
 * For positional pseudo-classes like :nth-child(even), the nthChild field
 * stores the 1-based position (0 means position is not set).
 * <p>
 * <b>Usage Context:</b>
 * <ul>
 *   <li><b>CSS Styling (during render):</b> All pseudo-classes work via
 *       {@code RenderContext.childStyle()} which accepts position and state</li>
 *   <li><b>TFX Effects (post-render):</b> Only {@code :focus} is supported
 *       because it's tracked by FocusManager. Other pseudo-classes require
 *       state/position info that isn't preserved after rendering.</li>
 * </ul>
 */
public final class PseudoClassState {

    /**
     * Empty state with all flags set to false.
     */
    public static final PseudoClassState NONE = new PseudoClassState(
            false, false, false, false, false, false, false, 0
    );

    private static final PseudoClassState FOCUSED = new PseudoClassState(true, false, false, false, false, false, false, 0);
    private static final PseudoClassState HOVERED = new PseudoClassState(false, true, false, false, false, false, false, 0);
    private static final PseudoClassState DISABLED = new PseudoClassState(false, false, true, false, false, false, false, 0);
    private static final PseudoClassState SELECTED = new PseudoClassState(false, false, false, false, true, false, false, 0);
    private static final PseudoClassState ALL_MATCH = new PseudoClassState(true, true, true, true, true, true, true, 1);

    private final boolean focused;
    private final boolean hovered;
    private final boolean disabled;
    private final boolean active;
    private final boolean selected;
    private final boolean firstChild;
    private final boolean lastChild;
    private final int nthChild; // 1-based position, 0 means not set

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

    /**
     * Returns a state with only the focused flag set.
     */
    public static PseudoClassState ofFocused() {
        return FOCUSED;
    }

    /**
     * Returns a state with only the hovered flag set.
     */
    public static PseudoClassState ofHovered() {
        return HOVERED;
    }

    /**
     * Returns a state with only the disabled flag set.
     */
    public static PseudoClassState ofDisabled() {
        return DISABLED;
    }

    /**
     * Returns a state with only the selected flag set.
     */
    public static PseudoClassState ofSelected() {
        return SELECTED;
    }

    /**
     * Returns a state with all pseudo-classes matching.
     * Useful for lenient/structural matching where pseudo-class state should be ignored.
     */
    public static PseudoClassState allMatch() {
        return ALL_MATCH;
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

    /**
     * Returns the 1-based child position, or 0 if not set.
     */
    public int nthChild() {
        return nthChild;
    }

    /**
     * Returns a new state with the focused flag set.
     */
    public PseudoClassState withFocused(boolean focused) {
        return new PseudoClassState(focused, hovered, disabled, active, selected, firstChild, lastChild, nthChild);
    }

    /**
     * Returns a new state with the hovered flag set.
     */
    public PseudoClassState withHovered(boolean hovered) {
        return new PseudoClassState(focused, hovered, disabled, active, selected, firstChild, lastChild, nthChild);
    }

    /**
     * Returns a new state with the disabled flag set.
     */
    public PseudoClassState withDisabled(boolean disabled) {
        return new PseudoClassState(focused, hovered, disabled, active, selected, firstChild, lastChild, nthChild);
    }

    /**
     * Returns a new state with the active flag set.
     */
    public PseudoClassState withActive(boolean active) {
        return new PseudoClassState(focused, hovered, disabled, active, selected, firstChild, lastChild, nthChild);
    }

    /**
     * Returns a new state with the selected flag set.
     */
    public PseudoClassState withSelected(boolean selected) {
        return new PseudoClassState(focused, hovered, disabled, active, selected, firstChild, lastChild, nthChild);
    }

    /**
     * Returns a new state with the firstChild flag set.
     */
    public PseudoClassState withFirstChild(boolean firstChild) {
        return new PseudoClassState(focused, hovered, disabled, active, selected, firstChild, lastChild, nthChild);
    }

    /**
     * Returns a new state with the lastChild flag set.
     */
    public PseudoClassState withLastChild(boolean lastChild) {
        return new PseudoClassState(focused, hovered, disabled, active, selected, firstChild, lastChild, nthChild);
    }

    /**
     * Returns a new state with the nthChild position set.
     *
     * @param nthChild the 1-based child position
     */
    public PseudoClassState withNthChild(int nthChild) {
        return new PseudoClassState(focused, hovered, disabled, active, selected, firstChild, lastChild, nthChild);
    }

    /**
     * Checks if the given pseudo-class is active.
     * <p>
     * Supports simple pseudo-classes (focus, hover, etc.) and functional
     * pseudo-classes like nth-child(even) and nth-child(odd).
     *
     * @param pseudoClass the pseudo-class name (without colon)
     * @return true if the pseudo-class is active
     */
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
