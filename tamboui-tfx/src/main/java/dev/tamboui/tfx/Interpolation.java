/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tfx;

/**
 * Easing functions (interpolation curves) for smooth animations.
 * <p>
 * Interpolation transforms linear progress (0.0 to 1.0) into smooth animation curves.
 * Different interpolation types produce different visual effects, from linear motion
 * to bouncy, elastic, or overshooting animations.
 * <p>
 * <b>Design Philosophy:</b>
 * <p>
 * Interpolation functions separate timing concerns from rendering logic. Effects use
 * the interpolated alpha value to drive their visual transformations, allowing the
 * same effect to produce different feels with different interpolation types.
 * <p>
 * <b>Interpolation Categories:</b>
 * <ul>
 *   <li><b>Linear:</b> Constant speed (no easing)</li>
 *   <li><b>Polynomial:</b> Quad, Cubic, Quart, Quint - progressively stronger acceleration</li>
 *   <li><b>Trigonometric:</b> Sine, Circ - smooth sinusoidal curves</li>
 *   <li><b>Exponential:</b> Expo - rapid acceleration/deceleration</li>
 *   <li><b>Elastic:</b> Elastic - bouncy elastic effect</li>
 *   <li><b>Bounce:</b> Bounce - bouncing effect</li>
 *   <li><b>Back:</b> Back - overshoot effect</li>
 * </ul>
 * <p>
 * <b>Easing Variants:</b>
 * <ul>
 *   <li><b>In:</b> Ease-in (slow start, fast end)</li>
 *   <li><b>Out:</b> Ease-out (fast start, slow end)</li>
 *   <li><b>InOut:</b> Ease-in-out (slow start and end, fast middle)</li>
 * </ul>
 * <p>
 * <b>Usage Pattern:</b>
 * <pre>{@code
 * // Smooth ease-in-out (most common)
 * Effect smooth = Fx.fadeToFg(Color.CYAN, 2000, Interpolation.SineInOut);
 * 
 * // Bouncy effect
 * Effect bouncy = Fx.dissolve(2000, Interpolation.BounceOut);
 * 
 * // Elastic effect
 * Effect elastic = Fx.sweepIn(Motion.LEFT_TO_RIGHT, 10, 0, Color.BLUE, 
 *     2000, Interpolation.ElasticOut);
 * }</pre>
 * <p>
 * <b>Choosing an Interpolation:</b>
 * <ul>
 *   <li><b>SineInOut:</b> Most versatile, smooth and natural (recommended default)</li>
 *   <li><b>QuadOut:</b> Quick and responsive, good for UI transitions</li>
 *   <li><b>BounceOut:</b> Playful, attention-grabbing</li>
 *   <li><b>ElasticOut:</b> Dramatic, elastic feel</li>
 *   <li><b>Linear:</b> Mechanical, constant speed</li>
 * </ul>
 */
public enum Interpolation {
    /** Back easing - in */
    BackIn,
    /** Back easing - out */
    BackOut,
    /** Back easing - in-out */
    BackInOut,
    
    /** Bounce easing - in */
    BounceIn,
    /** Bounce easing - out */
    BounceOut,
    /** Bounce easing - in-out */
    BounceInOut,
    
    /** Circular easing - in */
    CircIn,
    /** Circular easing - out */
    CircOut,
    /** Circular easing - in-out */
    CircInOut,
    
    /** Cubic easing - in */
    CubicIn,
    /** Cubic easing - out */
    CubicOut,
    /** Cubic easing - in-out */
    CubicInOut,
    
    /** Elastic easing - in */
    ElasticIn,
    /** Elastic easing - out */
    ElasticOut,
    /** Elastic easing - in-out */
    ElasticInOut,
    
    /** Exponential easing - in */
    ExpoIn,
    /** Exponential easing - out */
    ExpoOut,
    /** Exponential easing - in-out */
    ExpoInOut,
    
    /** Linear interpolation (no easing) */
    Linear,
    
    /** Quadratic easing - in */
    QuadIn,
    /** Quadratic easing - out */
    QuadOut,
    /** Quadratic easing - in-out */
    QuadInOut,
    
    /** Quartic easing - in */
    QuartIn,
    /** Quartic easing - out */
    QuartOut,
    /** Quartic easing - in-out */
    QuartInOut,
    
    /** Quintic easing - in */
    QuintIn,
    /** Quintic easing - out */
    QuintOut,
    /** Quintic easing - in-out */
    QuintInOut,
    
    /** Reverse (1 - t) */
    Reverse,
    
    /** Sine easing - in */
    SineIn,
    /** Sine easing - out */
    SineOut,
    /** Sine easing - in-out */
    SineInOut;
    
    /**
     * Applies the interpolation function to the given alpha value (0.0 to 1.0).
     *
     * @param a the input alpha value (0.0 to 1.0)
     * @return the interpolated alpha value
     */
    public float alpha(float a) {
        switch (this) {
            case BackIn:
                return easingBackIn(a);
            case BackOut:
                return easingBackOut(a);
            case BackInOut:
                return easingBackInOut(a);
            case BounceIn:
                return easingBounceIn(a);
            case BounceOut:
                return easingBounceOut(a);
            case BounceInOut:
                return easingBounceInOut(a);
            case CircIn:
                return easingCircIn(a);
            case CircOut:
                return easingCircOut(a);
            case CircInOut:
                return easingCircInOut(a);
            case CubicIn:
                return easingCubicIn(a);
            case CubicOut:
                return easingCubicOut(a);
            case CubicInOut:
                return easingCubicInOut(a);
            case ElasticIn:
                return easingElasticIn(a);
            case ElasticOut:
                return easingElasticOut(a);
            case ElasticInOut:
                return easingElasticInOut(a);
            case ExpoIn:
                return easingExpoIn(a);
            case ExpoOut:
                return easingExpoOut(a);
            case ExpoInOut:
                return easingExpoInOut(a);
            case Linear:
                return a;
            case QuadIn:
                return easingQuadIn(a);
            case QuadOut:
                return easingQuadOut(a);
            case QuadInOut:
                return easingQuadInOut(a);
            case QuartIn:
                return easingQuartIn(a);
            case QuartOut:
                return easingQuartOut(a);
            case QuartInOut:
                return easingQuartInOut(a);
            case QuintIn:
                return easingQuintIn(a);
            case QuintOut:
                return easingQuintOut(a);
            case QuintInOut:
                return easingQuintInOut(a);
            case Reverse:
                return easingReverse(a);
            case SineIn:
                return easingSineIn(a);
            case SineOut:
                return easingSineOut(a);
            case SineInOut:
                return easingSineInOut(a);
            default:
                return a;
        }
    }
    
    /**
     * Returns the flipped version of this interpolation (in ↔ out).
     *
     * @return the flipped interpolation
     */
    public Interpolation flipped() {
        switch (this) {
            case BackIn:
                return BackOut;
            case BackOut:
                return BackIn;
            case BackInOut:
                return BackInOut;
            case BounceIn:
                return BounceOut;
            case BounceOut:
                return BounceIn;
            case BounceInOut:
                return BounceInOut;
            case CircIn:
                return CircOut;
            case CircOut:
                return CircIn;
            case CircInOut:
                return CircInOut;
            case CubicIn:
                return CubicOut;
            case CubicOut:
                return CubicIn;
            case CubicInOut:
                return CubicInOut;
            case ElasticIn:
                return ElasticOut;
            case ElasticOut:
                return ElasticIn;
            case ElasticInOut:
                return ElasticInOut;
            case ExpoIn:
                return ExpoOut;
            case ExpoOut:
                return ExpoIn;
            case ExpoInOut:
                return ExpoInOut;
            case Linear:
                return Linear;
            case QuadIn:
                return QuadOut;
            case QuadOut:
                return QuadIn;
            case QuadInOut:
                return QuadInOut;
            case QuartIn:
                return QuartOut;
            case QuartOut:
                return QuartIn;
            case QuartInOut:
                return QuartInOut;
            case QuintIn:
                return QuintOut;
            case QuintOut:
                return QuintIn;
            case QuintInOut:
                return QuintInOut;
            case Reverse:
                return Reverse;
            case SineIn:
                return SineOut;
            case SineOut:
                return SineIn;
            case SineInOut:
                return SineInOut;
            default:
                return this;
        }
    }
    
    // Easing function implementations
    
    private static float easingBackIn(float t) {
        float c1 = 1.70158f;
        float c3 = c1 + 1.0f;
        return c3 * t * t * t - c1 * t * t;
    }
    
    private static float easingBackOut(float t) {
        float c1 = 1.70158f;
        float c3 = c1 + 1.0f;
        return 1.0f + c3 * TFxMath.powi(t - 1.0f, 3) + c1 * TFxMath.powi(t - 1.0f, 2);
    }
    
    private static float easingBackInOut(float t) {
        float c1 = 1.70158f;
        float c2 = c1 * 1.525f;
        if (t < 0.5f) {
            return (TFxMath.powi(2.0f * t, 2) * ((c2 + 1.0f) * 2.0f * t - c2)) / 2.0f;
        } else {
            return (TFxMath.powi(2.0f * t - 2.0f, 2) * ((c2 + 1.0f) * (t * 2.0f - 2.0f) + c2) + 2.0f) / 2.0f;
        }
    }
    
    private static float easingBounceOut(float t) {
        float n1 = 7.5625f;
        float d1 = 2.75f;
        if (t < 1.0f / d1) {
            return n1 * t * t;
        } else if (t < 2.0f / d1) {
            float tAdj = t - 1.5f / d1;
            return n1 * tAdj * tAdj + 0.75f;
        } else if (t < 2.5f / d1) {
            float tAdj = t - 2.25f / d1;
            return n1 * tAdj * tAdj + 0.9375f;
        } else {
            float tAdj = t - 2.625f / d1;
            return n1 * tAdj * tAdj + 0.984375f;
        }
    }
    
    private static float easingBounceIn(float t) {
        return 1.0f - easingBounceOut(1.0f - t);
    }
    
    private static float easingBounceInOut(float t) {
        if (t < 0.5f) {
            return (1.0f - easingBounceOut(1.0f - 2.0f * t)) / 2.0f;
        } else {
            return (1.0f + easingBounceOut(2.0f * t - 1.0f)) / 2.0f;
        }
    }
    
    private static float easingCircIn(float t) {
        return 1.0f - TFxMath.sqrt(1.0f - t * t);
    }
    
    private static float easingCircOut(float t) {
        return TFxMath.sqrt(1.0f - (t - 1.0f) * (t - 1.0f));
    }
    
    private static float easingCircInOut(float t) {
        if (t < 0.5f) {
            return (1.0f - easingCircOut(1.0f - 2.0f * t)) / 2.0f;
        } else {
            return (easingCircOut(2.0f * t - 1.0f) + 1.0f) / 2.0f;
        }
    }
    
    private static float easingCubicIn(float t) {
        return t * t * t;
    }
    
    private static float easingCubicOut(float t) {
        return 1.0f - TFxMath.powi(1.0f - t, 3);
    }
    
    private static float easingCubicInOut(float t) {
        if (t < 0.5f) {
            return 4.0f * t * t * t;
        } else {
            return 1.0f - TFxMath.powi(-2.0f * t + 2.0f, 3) / 2.0f;
        }
    }
    
    private static float easingElasticIn(float t) {
        if (t == 0.0f) {
            return 0.0f;
        } else if (t == 1.0f) {
            return 1.0f;
        } else {
            float c4 = TFxMath.tau() / 3.0f;
            return -TFxMath.powf(2.0f, 10.0f * (t - 1.0f)) * TFxMath.sin((t - 1.0f) * c4 - TFxMath.pi() / 2.0f);
        }
    }
    
    private static float easingElasticOut(float t) {
        if (t == 0.0f) {
            return 0.0f;
        } else if (t == 1.0f) {
            return 1.0f;
        } else {
            float c4 = TFxMath.tau() / 3.0f;
            return TFxMath.powf(2.0f, -10.0f * t) * TFxMath.sin(t * c4 - TFxMath.pi() / 2.0f) + 1.0f;
        }
    }
    
    private static float easingElasticInOut(float t) {
        if (t == 0.0f) {
            return 0.0f;
        } else if (t == 1.0f) {
            return 1.0f;
        } else if (t < 0.5f) {
            return -(easingElasticOut(1.0f - 2.0f * t) - 1.0f) / 2.0f;
        } else {
            return (easingElasticOut(2.0f * t - 1.0f) + 1.0f) / 2.0f;
        }
    }
    
    private static float easingExpoIn(float t) {
        if (t == 0.0f) {
            return 0.0f;
        } else {
            return TFxMath.powf(2.0f, 10.0f * (t - 1.0f));
        }
    }
    
    private static float easingExpoOut(float t) {
        if (t == 1.0f) {
            return 1.0f;
        } else {
            return 1.0f - TFxMath.powf(2.0f, -10.0f * t);
        }
    }
    
    private static float easingExpoInOut(float t) {
        if (t == 0.0f) {
            return 0.0f;
        } else if (t == 1.0f) {
            return 1.0f;
        } else if (t < 0.5f) {
            return easingExpoIn(2.0f * t) / 2.0f;
        } else {
            return (2.0f - easingExpoIn(2.0f * (1.0f - t))) / 2.0f;
        }
    }
    
    private static float easingQuadIn(float t) {
        return t * t;
    }
    
    private static float easingQuadOut(float t) {
        return 1.0f - (1.0f - t) * (1.0f - t);
    }
    
    private static float easingQuadInOut(float t) {
        if (t < 0.5f) {
            return 2.0f * t * t;
        } else {
            return 1.0f - TFxMath.powi(-2.0f * t + 2.0f, 2) / 2.0f;
        }
    }
    
    private static float easingQuartIn(float t) {
        return t * t * t * t;
    }
    
    private static float easingQuartOut(float t) {
        return 1.0f - TFxMath.powi(1.0f - t, 4);
    }
    
    private static float easingQuartInOut(float t) {
        if (t < 0.5f) {
            return 8.0f * t * t * t * t;
        } else {
            return 1.0f - TFxMath.powi(-2.0f * t + 2.0f, 4) / 2.0f;
        }
    }
    
    private static float easingQuintIn(float t) {
        return t * t * t * t * t;
    }
    
    private static float easingQuintOut(float t) {
        return 1.0f - TFxMath.powi(1.0f - t, 5);
    }
    
    private static float easingQuintInOut(float t) {
        if (t < 0.5f) {
            return 16.0f * t * t * t * t * t;
        } else {
            return 1.0f - TFxMath.powi(-2.0f * t + 2.0f, 5) / 2.0f;
        }
    }
    
    private static float easingReverse(float t) {
        return 1.0f - t;
    }
    
    private static float easingSineIn(float t) {
        return 1.0f - TFxMath.cos(t * TFxMath.pi() / 2.0f);
    }
    
    private static float easingSineOut(float t) {
        return TFxMath.sin(t * TFxMath.pi() / 2.0f);
    }
    
    private static float easingSineInOut(float t) {
        return -TFxMath.cos(t * TFxMath.pi()) / 2.0f + 0.5f;
    }
}


