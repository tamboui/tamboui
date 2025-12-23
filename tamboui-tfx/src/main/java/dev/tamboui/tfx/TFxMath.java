/*
 * Copyright (c) 2025 TamboUI Contributors
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
    
    public static float sqrt(float x) {
        return (float) java.lang.Math.sqrt(x);
    }
    
    public static float sin(float x) {
        return (float) java.lang.Math.sin(x);
    }
    
    public static float cos(float x) {
        return (float) java.lang.Math.cos(x);
    }
    
    public static float powf(float base, float exp) {
        return (float) java.lang.Math.pow(base, exp);
    }
    
    public static float powi(float base, int exp) {
        return (float) java.lang.Math.pow(base, exp);
    }
    
    public static float round(float x) {
        return java.lang.Math.round(x);
    }
    
    public static float floor(float x) {
        return (float) java.lang.Math.floor(x);
    }
    
    public static float ceil(float x) {
        return (float) java.lang.Math.ceil(x);
    }
    
    public static float pi() {
        return PI;
    }
    
    public static float tau() {
        return TAU;
    }
}

