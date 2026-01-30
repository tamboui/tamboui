/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.layout.cassowary;

import dev.tamboui.layout.Fraction;

/**
 * Constraint strength representing priority in the constraint hierarchy.
 *
 * <p>
 * Cassowary uses a hierarchical constraint system where higher-strength
 * constraints take absolute precedence over lower-strength ones. The strength
 * is computed using three weight levels that are combined into a single value.
 *
 * <p>
 * This implementation uses {@link Fraction} for exact arithmetic, avoiding the
 * cumulative rounding errors that occur with floating-point.
 *
 * <p>
 * Predefined strengths in decreasing order of priority:
 * <ul>
 * <li>{@link #REQUIRED} - Must be satisfied; failure throws an exception</li>
 * <li>{@link #STRONG} - High priority preference</li>
 * <li>{@link #MEDIUM} - Medium priority preference</li>
 * <li>{@link #WEAK} - Low priority preference</li>
 * </ul>
 */
public final class Strength {

    private static final Fraction THOUSAND = Fraction.of(1000);
    private static final Fraction MILLION = Fraction.of(1_000_000);

    /**
     * Required constraints must be satisfied. Adding an unsatisfiable required
     * constraint will throw an exception.
     */
    public static final Strength REQUIRED = new Strength(THOUSAND, THOUSAND, THOUSAND);

    /**
     * Strong preference - high priority but can be violated if necessary.
     */
    public static final Strength STRONG = new Strength(Fraction.ONE, Fraction.ZERO, Fraction.ZERO);

    /**
     * Medium preference - moderate priority.
     */
    public static final Strength MEDIUM = new Strength(Fraction.ZERO, Fraction.ONE, Fraction.ZERO);

    /**
     * Weak preference - low priority, easily overridden.
     */
    public static final Strength WEAK = new Strength(Fraction.ZERO, Fraction.ZERO, Fraction.ONE);

    private final Fraction strong;
    private final Fraction medium;
    private final Fraction weak;

    private Strength(Fraction strong, Fraction medium, Fraction weak) {
        this.strong = strong;
        this.medium = medium;
        this.weak = weak;
    }

    /**
     * Creates a custom strength with the given weights.
     *
     * <p>
     * The weights are combined using a polynomial scheme where the strong weight
     * has the highest significance, followed by medium, then weak.
     *
     * @param strong
     *            the strong weight (highest priority)
     * @param medium
     *            the medium weight
     * @param weak
     *            the weak weight (lowest priority)
     * @return a new strength with the given weights
     */
    public static Strength create(Fraction strong, Fraction medium, Fraction weak) {
        return new Strength(strong, medium, weak);
    }

    /**
     * Creates a custom strength with the given weights.
     *
     * <p>
     * The weights are combined using a polynomial scheme where the strong weight
     * has the highest significance, followed by medium, then weak.
     *
     * @param strong
     *            the strong weight (highest priority)
     * @param medium
     *            the medium weight
     * @param weak
     *            the weak weight (lowest priority)
     * @return a new strength with the given weights
     */
    public static Strength create(long strong, long medium, long weak) {
        return create(Fraction.of(strong), Fraction.of(medium), Fraction.of(weak));
    }

    /**
     * Computes the numeric value of this strength for use in the simplex objective.
     *
     * <p>
     * Uses a polynomial scheme to ensure hierarchical ordering: strong weights
     * dominate medium weights which dominate weak weights.
     *
     * @return the computed strength value
     */
    public Fraction computeValue() {
        return strong.multiply(MILLION).add(medium.multiply(THOUSAND)).add(weak);
    }

    /**
     * Returns true if this is the REQUIRED strength.
     *
     * @return true if this constraint is required
     */
    public boolean isRequired() {
        return strong.compareTo(THOUSAND) >= 0 && medium.compareTo(THOUSAND) >= 0
                && weak.compareTo(THOUSAND) >= 0;
    }

    /**
     * Returns the strong weight component.
     *
     * @return the strong weight
     */
    public Fraction strong() {
        return strong;
    }

    /**
     * Returns the medium weight component.
     *
     * @return the medium weight
     */
    public Fraction medium() {
        return medium;
    }

    /**
     * Returns the weak weight component.
     *
     * @return the weak weight
     */
    public Fraction weak() {
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
        return strong.equals(strength.strong) && medium.equals(strength.medium)
                && weak.equals(strength.weak);
    }

    @Override
    public int hashCode() {
        int result = strong.hashCode();
        result = 31 * result + medium.hashCode();
        result = 31 * result + weak.hashCode();
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
