/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.spinner;

/**
 * Built-in spinner frame sets for the {@link Spinner} widget.
 * <p>
 * Each style defines a sequence of frames that are cycled through
 * to create an animated loading indicator.
 *
 * <pre>{@code
 * // Use the DOTS style (braille characters)
 * Spinner spinner = Spinner.builder()
 *     .spinnerStyle(SpinnerStyle.DOTS)
 *     .build();
 *
 * // Use the LINE style (classic -\|/)
 * Spinner spinner = Spinner.builder()
 *     .spinnerStyle(SpinnerStyle.LINE)
 *     .build();
 *
 * // Use a custom frame set
 * Spinner spinner = Spinner.builder()
 *     .frameSet(SpinnerFrameSet.of("*", "+", "x", "+"))
 *     .build();
 * }</pre>
 *
 * @see SpinnerFrameSet
 */
public enum SpinnerStyle {

    /** Braille dot pattern spinner. */
    DOTS(SpinnerFrameSet.of("\u2800", "\u2801", "\u2803", "\u2807", "\u280f", "\u281f", "\u283f", "\u287f", "\u28ff",
            "\u28fe", "\u28fc", "\u28f8", "\u28f0", "\u28e0", "\u28c0", "\u2880")),

    /** Classic line spinner: {@code -\|/}. */
    LINE(SpinnerFrameSet.of("-", "\\", "|", "/")),

    /** Arc spinner using quarter-circle characters. */
    ARC(SpinnerFrameSet.of("\u25dc", "\u25dd", "\u25de", "\u25df")),

    /** Circle spinner using clock-position characters. */
    CIRCLE(SpinnerFrameSet.of("\u25cb", "\u25d4", "\u25d1", "\u25d5", "\u25cf", "\u25d5", "\u25d1", "\u25d4")),

    /** Bouncing bar spinner. */
    BOUNCING_BAR(SpinnerFrameSet.of("[    ]", "[=   ]", "[==  ]", "[=== ]", "[ ===]", "[  ==]", "[   =]", "[    ]",
            "[   =]", "[  ==]", "[ ===]", "[=== ]", "[==  ]", "[=   ]")),

    /** Toggle spinner with two states. */
    TOGGLE(SpinnerFrameSet.of("\u25b6", "\u25b7")),

    /** Gauge-style spinner using block fill characters (left to right). */
    GAUGE(SpinnerFrameSet.of("\u258f", "\u258e", "\u258d", "\u258c", "\u258b", "\u258a", "\u2589", "\u2588")),

    /** Growing vertical bar spinner (bottom to top). */
    VERTICAL_GAUGE(SpinnerFrameSet.of("\u2581", "\u2582", "\u2583", "\u2584", "\u2585", "\u2586", "\u2587", "\u2588")),

    /** Rotating arrow spinner. */
    ARROWS(SpinnerFrameSet.of("\u2190", "\u2196", "\u2191", "\u2197", "\u2192", "\u2198", "\u2193", "\u2199")),

    /** Clock spinner using clock face characters. */
    CLOCK(SpinnerFrameSet.of("\ud83d\udd50", "\ud83d\udd51", "\ud83d\udd52", "\ud83d\udd53", "\ud83d\udd54", "\ud83d\udd55",
            "\ud83d\udd56", "\ud83d\udd57", "\ud83d\udd58", "\ud83d\udd59", "\ud83d\udd5a", "\ud83d\udd5b")),

    /** Moon phases spinner. */
    MOON(SpinnerFrameSet.of("\ud83c\udf11", "\ud83c\udf12", "\ud83c\udf13", "\ud83c\udf14", "\ud83c\udf15",
            "\ud83c\udf16", "\ud83c\udf17", "\ud83c\udf18")),

    /** Square corners spinner. */
    SQUARE_CORNERS(SpinnerFrameSet.of("\u25f0", "\u25f3", "\u25f2", "\u25f1")),

    /** Growing dots spinner using braille. */
    GROWING_DOTS(SpinnerFrameSet.of("\u2800", "\u2840", "\u28c0", "\u28e0", "\u28f0", "\u28f8", "\u28fc", "\u28fe", "\u28ff")),

    /** Bouncing ball spinner. */
    BOUNCING_BALL(SpinnerFrameSet.of("\u2801", "\u2802", "\u2804", "\u2840", "\u2880", "\u2820", "\u2810", "\u2808"));

    private final SpinnerFrameSet frameSet;

    SpinnerStyle(SpinnerFrameSet frameSet) {
        this.frameSet = frameSet;
    }

    /**
     * Returns the frame set for this spinner style.
     *
     * @return the frame set
     */
    public SpinnerFrameSet frameSet() {
        return frameSet;
    }

    /**
     * Returns all frames for this spinner style.
     *
     * @return the frame array
     */
    public String[] frames() {
        return frameSet.frames();
    }

    /**
     * Returns the frame at the given index (wrapping around).
     *
     * @param index the frame index
     * @return the frame string
     */
    public String frame(int index) {
        return frameSet.frame(index);
    }

    /**
     * Returns the number of frames in this style.
     *
     * @return the frame count
     */
    public int frameCount() {
        return frameSet.frameCount();
    }
}
