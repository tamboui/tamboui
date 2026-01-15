/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.layout.cassowary;

/**
 * Constraint strength representing priority in the constraint hierarchy.
 *
 * <p>Cassowary uses a hierarchical constraint system where higher-strength
 * constraints take absolute precedence over lower-strength ones. The strength
 * is computed using three weight levels that are combined into a single value.
 *
 * <p>Predefined strengths in decreasing order of priority:
 * <ul>
 *   <li>{@link #REQUIRED} - Must be satisfied; failure throws an exception</li>
 *   <li>{@link #STRONG} - High priority preference</li>
 *   <li>{@link #MEDIUM} - Medium priority preference</li>
 *   <li>{@link #WEAK} - Low priority preference</li>
 * </ul>
 */
public final class Strength {

    /**
     * Required constraints must be satisfied. Adding an unsatisfiable
     * required constraint will throw an exception.
     */
    public static final Strength REQUIRED = new Strength(1000.0, 1000.0, 1000.0);

    /**
     * Strong preference - high priority but can be violated if necessary.
     */
    public static final Strength STRONG = new Strength(1.0, 0.0, 0.0);

    /**
     * Medium preference - moderate priority.
     */
    public static final Strength MEDIUM = new Strength(0.0, 1.0, 0.0);

    /**
     * Weak preference - low priority, easily overridden.
     */
    public static final Strength WEAK = new Strength(0.0, 0.0, 1.0);

    private final double strong;
    private final double medium;
    private final double weak;

    private Strength(double strong, double medium, double weak) {
        this.strong = strong;
        this.medium = medium;
        this.weak = weak;
    }

    /**
     * Creates a custom strength with the given weights.
     *
     * <p>The weights are combined using a polynomial scheme where the strong
     * weight has the highest significance, followed by medium, then weak.
     *
     * @param strong the strong weight (highest priority)
     * @param medium the medium weight
     * @param weak   the weak weight (lowest priority)
     * @return a new strength with the given weights
     */
    public static Strength create(double strong, double medium, double weak) {
        return new Strength(strong, medium, weak);
    }

    /**
     * Computes the numeric value of this strength for use in the simplex objective.
     *
     * <p>Uses a polynomial scheme to ensure hierarchical ordering:
     * strong weights dominate medium weights which dominate weak weights.
     *
     * @return the computed strength value
     */
    public double computeValue() {
        return strong * 1_000_000.0 + medium * 1_000.0 + weak;
    }

    /**
     * Returns true if this is the REQUIRED strength.
     *
     * @return true if this constraint is required
     */
    public boolean isRequired() {
        return strong >= 1000.0 && medium >= 1000.0 && weak >= 1000.0;
    }

    /**
     * Returns the strong weight component.
     *
     * @return the strong weight
     */
    public double strong() {
        return strong;
    }

    /**
     * Returns the medium weight component.
     *
     * @return the medium weight
     */
    public double medium() {
        return medium;
    }

    /**
     * Returns the weak weight component.
     *
     * @return the weak weight
     */
    public double weak() {
        return weak;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Strength)) {
            return false;
        }
        Strength strength = (Strength) o;
        return Double.compare(strength.strong, strong) == 0
                && Double.compare(strength.medium, medium) == 0
                && Double.compare(strength.weak, weak) == 0;
    }

    @Override
    public int hashCode() {
        int result = Double.hashCode(strong);
        result = 31 * result + Double.hashCode(medium);
        result = 31 * result + Double.hashCode(weak);
        return result;
    }

    @Override
    public String toString() {
        if (equals(REQUIRED)) {
            return "REQUIRED";
        }
        if (equals(STRONG)) {
            return "STRONG";
        }
        if (equals(MEDIUM)) {
            return "MEDIUM";
        }
        if (equals(WEAK)) {
            return "WEAK";
        }
        return String.format("Strength[%s, %s, %s]", strong, medium, weak);
    }
}
