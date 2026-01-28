/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tfx;

/**
 * A simple pseudo-random number generator using the Linear Congruential Generator algorithm.
 * <p>
 * This RNG is fast and uses minimal memory, and is definitely not suitable for
 * cryptographic purposes or high-quality randomness.
 */
public final class SimpleRng {
    
    private static final int A = 1664525;
    private static final int C = 1013904223;
    private static final int EXPONENT = 0x3f800000; // 1.0f32
    
    private int state;
    
    /**
     * Creates a new SimpleRng with the specified seed.
     *
     * @param seed the initial seed value
     */
    public SimpleRng(int seed) {
        this.state = seed;
    }
    
    /**
     * Creates a new SimpleRng with a seed based on the current system time.
     *
     * @return a new SimpleRng with a time-based seed
     */
    public static SimpleRng defaultRng() {
        long seed = System.currentTimeMillis();
        return new SimpleRng((int) (seed & 0xFFFFFFFFL));
    }
    
    /**
     * Generates the next pseudo-random int value.
     * <p>
     * This method updates the internal state and returns the new value.
     * 
     * @return A pseudo-random int value
     */
    public int gen() {
        state = (int) ((state * (long) A + C) & 0xFFFFFFFFL);
        return state;
    }
    
    /**
     * Generates a pseudo-random float value in the range [0, 1).
     * <p>
     * This method uses bit manipulation for efficiency, generating
     * uniformly distributed float values.
     * 
     * @return A pseudo-random float value in the range [0, 1)
     */
    public float genF32() {
        int mantissa = gen() >>> 9; // 23 bits of randomness
        int bits = EXPONENT | mantissa;
        return Float.intBitsToFloat(bits) - 1.0f;
    }
    
    /**
     * Generates a random value in the specified range [start, end).
     * 
     * @param start The start of the range (inclusive)
     * @param end The end of the range (exclusive)
     * @return A random int in the range [start, end)
     */
    public int genRange(int start, int end) {
        if (end <= start) {
            throw new IllegalArgumentException("end must be greater than start");
        }
        int rangeSize = end - start;
        return start + (java.lang.Math.abs(gen()) % rangeSize);
    }
    
    /**
     * Generates a random float value in the specified range [start, end).
     * 
     * @param start The start of the range (inclusive)
     * @param end The end of the range (exclusive)
     * @return A random float in the range [start, end)
     */
    public float genRange(float start, float end) {
        if (end <= start) {
            throw new IllegalArgumentException("end must be greater than start");
        }
        float rangeSize = end - start;
        return start + (genF32() % rangeSize);
    }
    
    /**
     * Returns the current state (for copying/debugging).
     *
     * @return the current internal state
     */
    public int state() {
        return state;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SimpleRng)) return false;
        SimpleRng simpleRng = (SimpleRng) o;
        return state == simpleRng.state;
    }
    
    @Override
    public int hashCode() {
        return state;
    }
    
    @Override
    public String toString() {
        return "SimpleRng{state=" + state + "}";
    }
}

