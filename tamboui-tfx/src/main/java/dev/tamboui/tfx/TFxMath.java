/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tfx;

/**
 * Math utilities for effects.
 * <p>
 * Provides fast math operations optimized for effect calculations.
 */
public final class TFxMath {
    
    private static final float PI = (float) java.lang.Math.PI;
    private static final float TAU = 2.0f * PI;
    
    private TFxMath() {
        // Utility class
    }
    
    /**
     * Computes the square root of a float value.
     *
     * @param x the value
     * @return the square root of {@code x}
     */
    public static float sqrt(float x) {
        return (float) java.lang.Math.sqrt(x);
    }

    /**
     * Computes the sine of a float value in radians.
     *
     * @param x the angle in radians
     * @return the sine of {@code x}
     */
    public static float sin(float x) {
        return (float) java.lang.Math.sin(x);
    }

    /**
     * Computes the cosine of a float value in radians.
     *
     * @param x the angle in radians
     * @return the cosine of {@code x}
     */
    public static float cos(float x) {
        return (float) java.lang.Math.cos(x);
    }

    /**
     * Raises a float base to a float exponent.
     *
     * @param base the base value
     * @param exp the exponent
     * @return {@code base} raised to the power of {@code exp}
     */
    public static float powf(float base, float exp) {
        return (float) java.lang.Math.pow(base, exp);
    }

    /**
     * Raises a float base to an integer exponent.
     *
     * @param base the base value
     * @param exp the integer exponent
     * @return {@code base} raised to the power of {@code exp}
     */
    public static float powi(float base, int exp) {
        return (float) java.lang.Math.pow(base, exp);
    }

    /**
     * Rounds a float value to the nearest integer.
     *
     * @param x the value to round
     * @return the nearest integer to {@code x}
     */
    public static float round(float x) {
        return java.lang.Math.round(x);
    }

    /**
     * Returns the largest integer value less than or equal to the argument.
     *
     * @param x the value
     * @return the floor of {@code x}
     */
    public static float floor(float x) {
        return (float) java.lang.Math.floor(x);
    }

    /**
     * Returns the smallest integer value greater than or equal to the argument.
     *
     * @param x the value
     * @return the ceiling of {@code x}
     */
    public static float ceil(float x) {
        return (float) java.lang.Math.ceil(x);
    }

    /**
     * Returns the value of pi.
     *
     * @return the value of pi
     */
    public static float pi() {
        return PI;
    }

    /**
     * Returns the value of tau (2 * pi).
     *
     * @return the value of tau
     */
    public static float tau() {
        return TAU;
    }
}

