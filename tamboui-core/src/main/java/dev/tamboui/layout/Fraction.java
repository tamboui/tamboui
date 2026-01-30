/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.layout;

/**
 * An immutable exact rational number represented as a fraction.
 *
 * <p>
 * Fractions avoid the cumulative rounding errors that occur with floating-point
 * arithmetic in constraint solving. All arithmetic operations return exact
 * results by maintaining numerator and denominator as long integers.
 *
 * <p>
 * Fractions are always stored in normalized form: the denominator is positive,
 * and the numerator and denominator share no common factors other than 1.
 *
 * <p>
 * Example usage:
 * 
 * <pre>
 * Fraction a = Fraction.of(1, 3);
 * Fraction b = Fraction.of(1, 6);
 * Fraction sum = a.add(b); // 1/2
 * </pre>
 */
public final class Fraction implements Comparable<Fraction> {

    /** The fraction representing zero (0/1). */
    public static final Fraction ZERO = new Fraction(0, 1);

    /** The fraction representing one (1/1). */
    public static final Fraction ONE = new Fraction(1, 1);

    /** The fraction representing negative one (-1/1). */
    public static final Fraction NEG_ONE = new Fraction(-1, 1);

    private final long numerator;
    private final long denominator;

    private Fraction(long numerator, long denominator) {
        this.numerator = numerator;
        this.denominator = denominator;
    }

    /**
     * Creates a fraction from a numerator and denominator.
     *
     * <p>
     * The fraction is automatically normalized (reduced to lowest terms, with a
     * positive denominator).
     *
     * @param numerator
     *            the numerator
     * @param denominator
     *            the denominator (must not be zero)
     * @return a normalized fraction
     * @throws ArithmeticException
     *             if the denominator is zero
     */
    public static Fraction of(long numerator, long denominator) {
        if (denominator == 0) {
            throw new ArithmeticException("Division by zero");
        }
        if (numerator == 0) {
            return ZERO;
        }
        return normalize(numerator, denominator);
    }

    /**
     * Creates a fraction from an integer value.
     *
     * @param value
     *            the integer value
     * @return a fraction representing the integer
     */
    public static Fraction of(long value) {
        if (value == 0) {
            return ZERO;
        }
        if (value == 1) {
            return ONE;
        }
        if (value == -1) {
            return NEG_ONE;
        }
        return new Fraction(value, 1);
    }

    /**
     * Creates a fraction from a double value.
     *
     * <p>
     * This attempts to find a reasonable fraction approximation of the double. For
     * exact integer values, an exact fraction is returned. For other values, a
     * continued fraction algorithm is used to find a close approximation.
     *
     * @param value
     *            the double value
     * @return a fraction approximating the double
     */
    public static Fraction fromDouble(double value) {
        if (value == 0.0) {
            return ZERO;
        }
        if (value == 1.0) {
            return ONE;
        }
        if (value == -1.0) {
            return NEG_ONE;
        }

        // Check for near-integer values
        long rounded = Math.round(value);
        if (Math.abs(value - rounded) < 1e-10) {
            return of(rounded);
        }

        // Use continued fraction algorithm to convert double to fraction
        // with reasonable precision
        return continuedFraction(value, 1_000_000_000L);
    }

    /**
     * Converts a double to a fraction using continued fraction expansion.
     */
    private static Fraction continuedFraction(double value, long maxDenominator) {
        boolean negative = value < 0;
        if (negative) {
            value = -value;
        }

        long intPart = (long) value;
        double fracPart = value - intPart;

        if (fracPart < 1e-10) {
            return negative ? of(-intPart) : of(intPart);
        }

        // Continued fraction expansion
        long p0 = intPart;
        long q0 = 1;
        long p1 = 1;
        long q1 = 0;

        double x = fracPart;
        for (int i = 0; i < 64 && x != 0; i++) {
            x = 1.0 / x;
            long a = (long) x;
            x = x - a;

            long p2 = a * p0 + p1;
            long q2 = a * q0 + q1;

            if (q2 > maxDenominator) {
                break;
            }

            p1 = p0;
            q1 = q0;
            p0 = p2;
            q0 = q2;

            if (Math.abs(value - (double) p0 / q0) < 1e-10) {
                break;
            }
        }

        return negative ? of(-p0, q0) : of(p0, q0);
    }

    private static Fraction normalize(long numerator, long denominator) {
        // Ensure denominator is positive
        if (denominator < 0) {
            numerator = -numerator;
            denominator = -denominator;
        }

        // Reduce to lowest terms
        long gcd = gcd(Math.abs(numerator), denominator);
        if (gcd > 1) {
            numerator /= gcd;
            denominator /= gcd;
        }

        // Use cached constants where possible
        if (denominator == 1) {
            if (numerator == 0) {
                return ZERO;
            }
            if (numerator == 1) {
                return ONE;
            }
            if (numerator == -1) {
                return NEG_ONE;
            }
        }

        return new Fraction(numerator, denominator);
    }

    private static long gcd(long a, long b) {
        while (b != 0) {
            long temp = b;
            b = a % b;
            a = temp;
        }
        return a;
    }

    /**
     * Returns the numerator of this fraction.
     *
     * @return the numerator
     */
    public long numerator() {
        return numerator;
    }

    /**
     * Returns the denominator of this fraction.
     *
     * @return the denominator (always positive)
     */
    public long denominator() {
        return denominator;
    }

    /**
     * Returns true if this fraction represents zero.
     *
     * @return true if zero
     */
    public boolean isZero() {
        return numerator == 0;
    }

    /**
     * Returns true if this fraction represents a positive value.
     *
     * @return true if positive
     */
    public boolean isPositive() {
        return numerator > 0;
    }

    /**
     * Returns true if this fraction represents a negative value.
     *
     * @return true if negative
     */
    public boolean isNegative() {
        return numerator < 0;
    }

    /**
     * Returns the sum of this fraction and another.
     *
     * @param other
     *            the fraction to add
     * @return the sum
     */
    public Fraction add(Fraction other) {
        if (this.isZero()) {
            return other;
        }
        if (other.isZero()) {
            return this;
        }
        // a/b + c/d = (a*d + c*b) / (b*d)
        long num = this.numerator * other.denominator + other.numerator * this.denominator;
        long den = this.denominator * other.denominator;
        return of(num, den);
    }

    /**
     * Returns the difference of this fraction and another.
     *
     * @param other
     *            the fraction to subtract
     * @return the difference
     */
    public Fraction subtract(Fraction other) {
        if (other.isZero()) {
            return this;
        }
        return add(other.negate());
    }

    /**
     * Returns the product of this fraction and another.
     *
     * @param other
     *            the fraction to multiply by
     * @return the product
     */
    public Fraction multiply(Fraction other) {
        if (this.isZero() || other.isZero()) {
            return ZERO;
        }
        if (this == ONE) {
            return other;
        }
        if (other == ONE) {
            return this;
        }
        if (this == NEG_ONE) {
            return other.negate();
        }
        if (other == NEG_ONE) {
            return this.negate();
        }
        return of(this.numerator * other.numerator, this.denominator * other.denominator);
    }

    /**
     * Returns the quotient of this fraction divided by another.
     *
     * @param other
     *            the fraction to divide by
     * @return the quotient
     * @throws ArithmeticException
     *             if dividing by zero
     */
    public Fraction divide(Fraction other) {
        if (other.isZero()) {
            throw new ArithmeticException("Division by zero");
        }
        if (this.isZero()) {
            return ZERO;
        }
        if (other == ONE) {
            return this;
        }
        if (other == NEG_ONE) {
            return this.negate();
        }
        return of(this.numerator * other.denominator, this.denominator * other.numerator);
    }

    /**
     * Returns the negation of this fraction.
     *
     * @return the negated fraction
     */
    public Fraction negate() {
        if (this.isZero()) {
            return ZERO;
        }
        if (this == ONE) {
            return NEG_ONE;
        }
        if (this == NEG_ONE) {
            return ONE;
        }
        return new Fraction(-numerator, denominator);
    }

    /**
     * Returns the absolute value of this fraction.
     *
     * @return the absolute value
     */
    public Fraction abs() {
        return isNegative() ? negate() : this;
    }

    /**
     * Returns the reciprocal of this fraction.
     *
     * @return the reciprocal (1/this)
     * @throws ArithmeticException
     *             if this fraction is zero
     */
    public Fraction reciprocal() {
        if (isZero()) {
            throw new ArithmeticException("Cannot compute reciprocal of zero");
        }
        return of(denominator, numerator);
    }

    /**
     * Converts this fraction to a double.
     *
     * @return the double value
     */
    public double toDouble() {
        return (double) numerator / denominator;
    }

    /**
     * Converts this fraction to a long by truncating toward zero.
     *
     * @return the truncated long value
     */
    public long toLong() {
        return numerator / denominator;
    }

    /**
     * Converts this fraction to an int by truncating toward zero.
     *
     * @return the truncated int value
     */
    public int toInt() {
        return (int) toLong();
    }

    /**
     * Returns the sign of this fraction.
     *
     * @return -1 if negative, 0 if zero, 1 if positive
     */
    public int signum() {
        return Long.signum(numerator);
    }

    @Override
    public int compareTo(Fraction other) {
        // Compare a/b with c/d by comparing a*d with c*b
        long left = this.numerator * other.denominator;
        long right = other.numerator * this.denominator;
        return Long.compare(left, right);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Fraction)) {
            return false;
        }
        Fraction that = (Fraction) o;
        // Fractions are always normalized, so direct comparison works
        return numerator == that.numerator && denominator == that.denominator;
    }

    @Override
    public int hashCode() {
        return 31 * Long.hashCode(numerator) + Long.hashCode(denominator);
    }

    @Override
    public String toString() {
        if (denominator == 1) {
            return String.valueOf(numerator);
        }
        return numerator + "/" + denominator;
    }
}
