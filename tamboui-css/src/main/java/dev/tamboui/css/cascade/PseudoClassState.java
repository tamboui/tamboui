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

    /**
     * Creates a pseudo-class state with the given flags.
     *
     * @param focused    whether the element has keyboard focus
     * @param hovered    whether the mouse is over the element
     * @param disabled   whether the element is disabled
     * @param active     whether the element is being activated
     * @param selected   whether the element is selected
     * @param firstChild whether the element is the first child of its parent
     * @param lastChild  whether the element is the last child of its parent
     * @param nthChild   the 1-based child position, or 0 if not set
     */
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
     *
     * @return a focused pseudo-class state
     */
    public static PseudoClassState ofFocused() {
        return FOCUSED;
    }

    /**
     * Returns a state with only the hovered flag set.
     *
     * @return a hovered pseudo-class state
     */
    public static PseudoClassState ofHovered() {
        return HOVERED;
    }

    /**
     * Returns a state with only the disabled flag set.
     *
     * @return a disabled pseudo-class state
     */
    public static PseudoClassState ofDisabled() {
        return DISABLED;
    }

    /**
     * Returns a state with only the selected flag set.
     *
     * @return a selected pseudo-class state
     */
    public static PseudoClassState ofSelected() {
        return SELECTED;
    }

    /**
     * Returns a state with all pseudo-classes matching.
     * Useful for lenient/structural matching where pseudo-class state should be ignored.
     *
     * @return a pseudo-class state with all flags set to true
     */
    public static PseudoClassState allMatch() {
        return ALL_MATCH;
    }

    /**
     * Returns whether the element has keyboard focus.
     *
     * @return {@code true} if focused
     */
    public boolean isFocused() {
        return focused;
    }

    /**
     * Returns whether the mouse is over the element.
     *
     * @return {@code true} if hovered
     */
    public boolean isHovered() {
        return hovered;
    }

    /**
     * Returns whether the element is disabled.
     *
     * @return {@code true} if disabled
     */
    public boolean isDisabled() {
        return disabled;
    }

    /**
     * Returns whether the element is being activated.
     *
     * @return {@code true} if active
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Returns whether the element is selected.
     *
     * @return {@code true} if selected
     */
    public boolean isSelected() {
        return selected;
    }

    /**
     * Returns whether the element is the first child of its parent.
     *
     * @return {@code true} if first child
     */
    public boolean isFirstChild() {
        return firstChild;
    }

    /**
     * Returns whether the element is the last child of its parent.
     *
     * @return {@code true} if last child
     */
    public boolean isLastChild() {
        return lastChild;
    }

    /**
     * Returns the 1-based child position, or 0 if not set.
     *
     * @return the child position
     */
    public int nthChild() {
        return nthChild;
    }

    /**
     * Returns a new state with the focused flag set.
     *
     * @param focused the focused flag value
     * @return a new state with the updated flag
     */
    public PseudoClassState withFocused(boolean focused) {
        return new PseudoClassState(focused, hovered, disabled, active, selected, firstChild, lastChild, nthChild);
    }

    /**
     * Returns a new state with the hovered flag set.
     *
     * @param hovered the hovered flag value
     * @return a new state with the updated flag
     */
    public PseudoClassState withHovered(boolean hovered) {
        return new PseudoClassState(focused, hovered, disabled, active, selected, firstChild, lastChild, nthChild);
    }

    /**
     * Returns a new state with the disabled flag set.
     *
     * @param disabled the disabled flag value
     * @return a new state with the updated flag
     */
    public PseudoClassState withDisabled(boolean disabled) {
        return new PseudoClassState(focused, hovered, disabled, active, selected, firstChild, lastChild, nthChild);
    }

    /**
     * Returns a new state with the active flag set.
     *
     * @param active the active flag value
     * @return a new state with the updated flag
     */
    public PseudoClassState withActive(boolean active) {
        return new PseudoClassState(focused, hovered, disabled, active, selected, firstChild, lastChild, nthChild);
    }

    /**
     * Returns a new state with the selected flag set.
     *
     * @param selected the selected flag value
     * @return a new state with the updated flag
     */
    public PseudoClassState withSelected(boolean selected) {
        return new PseudoClassState(focused, hovered, disabled, active, selected, firstChild, lastChild, nthChild);
    }

    /**
     * Returns a new state with the firstChild flag set.
     *
     * @param firstChild the firstChild flag value
     * @return a new state with the updated flag
     */
    public PseudoClassState withFirstChild(boolean firstChild) {
        return new PseudoClassState(focused, hovered, disabled, active, selected, firstChild, lastChild, nthChild);
    }

    /**
     * Returns a new state with the lastChild flag set.
     *
     * @param lastChild the lastChild flag value
     * @return a new state with the updated flag
     */
    public PseudoClassState withLastChild(boolean lastChild) {
        return new PseudoClassState(focused, hovered, disabled, active, selected, firstChild, lastChild, nthChild);
    }

    /**
     * Returns a new state with the nthChild position set.
     *
     * @param nthChild the 1-based child position
     * @return a new state with the updated position
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
