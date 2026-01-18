/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tfx;

/// Duration abstraction for effects with millisecond precision.
///
///
///
/// TFxDuration provides a lightweight wrapper around time durations specifically
/// designed for effect timing. It uses millisecond precision (sufficient for
/// frame-based animations) and provides a simpler API than {@link java.time.Duration}
/// for common effect timing operations.
///
///
///
/// **Design Philosophy:**
///
///
///
/// TFxDuration is designed for performance and simplicity in animation contexts:
///
/// - **Millisecond Precision:** Sufficient for 60fps animations (16ms per frame)
/// - **Immutable:** All operations return new instances, ensuring thread safety
/// - **Simple API:** Focused on common operations needed for effect timing
/// - **No Negative Durations:** Enforces non-negative durations to prevent errors
///
///
///
///
/// **Key Operations:**
///
/// - **Creation:** {@code fromMillis}, {@code fromSecs}, {@code fromSecsF32}
/// - **Arithmetic:** {@code add}, {@code sub}, {@code mul} (with overflow protection)
/// - **Comparison:** {@code isZero}, {@code checkedSub} (returns null if negative)
/// - **Conversion:** {@code asMillis}, {@code asSecsF32}, {@code toJavaDuration}
///
///
///
///
/// **Usage Pattern:**
/// ```java
/// // Create duration
/// TFxDuration duration = TFxDuration.fromMillis(2000); // 2 seconds
///
/// // Calculate frame delta
/// long frameTimeMs = 16; // ~60fps
/// TFxDuration delta = TFxDuration.fromMillis(frameTimeMs);
///
/// // Process timer
/// TFxDuration overflow = timer.process(delta);
/// }
/// ```
///
///
///
/// **Why Not Use java.time.Duration?**
///
///
///
/// While {@code java.time.Duration} is more feature-rich, TFxDuration provides:
///
/// - Simpler API for common animation operations
/// - Millisecond precision (sufficient for frame-based animations)
/// - Better performance for frequent operations
/// - Explicit non-negative enforcement
///
///
///
///
/// Conversion methods are provided for interoperability with {@code java.time.Duration}
/// when needed (e.g., for TuiConfig tick rates).
public final class TFxDuration {
    
    public static final TFxDuration ZERO = new TFxDuration(0);
    
    private final long milliseconds;
    
    private TFxDuration(long milliseconds) {
        this.milliseconds = milliseconds;
    }
    
    /// Creates a duration from milliseconds.
    public static TFxDuration fromMillis(long milliseconds) {
        if (milliseconds < 0) {
            throw new IllegalArgumentException("Duration cannot be negative");
        }
        return new TFxDuration(milliseconds);
    }
    
    /// Creates a duration from seconds.
    public static TFxDuration fromSecs(long seconds) {
        return fromMillis(seconds * 1000);
    }
    
    /// Creates a duration from fractional seconds.
    public static TFxDuration fromSecsF32(float seconds) {
        return fromMillis((long) (seconds * 1000.0f));
    }
    
    /// Converts from java.time.Duration.
    public static TFxDuration fromJavaDuration(java.time.Duration duration) {
        return fromMillis(duration.toMillis());
    }
    
    /// Returns the duration in milliseconds.
    public long asMillis() {
        return milliseconds;
    }
    
    /// Returns the duration in seconds as a float.
    public float asSecsF32() {
        return milliseconds / 1000.0f;
    }
    
    /// Converts to java.time.Duration.
    public java.time.Duration toJavaDuration() {
        return java.time.Duration.ofMillis(milliseconds);
    }
    
    /// Returns true if this duration is zero.
    public boolean isZero() {
        return milliseconds == 0;
    }
    
    /// Subtracts another duration, returning null if the result would be negative.
    public TFxDuration checkedSub(TFxDuration other) {
        if (milliseconds < other.milliseconds) {
            return null;
        }
        return fromMillis(milliseconds - other.milliseconds);
    }
    
    /// Adds another duration.
    public TFxDuration add(TFxDuration other) {
        return fromMillis(milliseconds + other.milliseconds);
    }
    
    /// Subtracts another duration.
    public TFxDuration sub(TFxDuration other) {
        return fromMillis(milliseconds - other.milliseconds);
    }
    
    /// Multiplies this duration by a scalar.
    public TFxDuration mul(long scalar) {
        return fromMillis(milliseconds * scalar);
    }
    
    /// Multiplies this duration by a float scalar.
    public TFxDuration mul(float scalar) {
        return fromMillis((long) (milliseconds * scalar));
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TFxDuration)) return false;
        TFxDuration duration = (TFxDuration) o;
        return milliseconds == duration.milliseconds;
    }
    
    @Override
    public int hashCode() {
        return Long.hashCode(milliseconds);
    }
    
    @Override
    public String toString() {
        return "TFxDuration{ms=" + milliseconds + "}";
    }
}


