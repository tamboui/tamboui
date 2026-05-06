/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.braille;

import java.util.ArrayList;
import java.util.List;

/**
 * Static frame patterns for Braille-based animations.
 * <p>
 * Each pattern provides an array offrame strings that can be used
 * with the Spinner widget or SpinnerFrameSet.
 * <p>
 * The patterns are ported from the unicode-animations TypeScript library.
 */
public final class BraillePatterns {

    private BraillePatterns() {
    }

    /**
     * Returns scan animation frames (10 frames).
     * A horizontal scanning line moves across the grid.
     *
     * @return an array of 10 frame strings
     */
    public static String[] scan() {
        return new String[] {
            "\u283f\u2800\u2800",
            "\u283f\u2800\u2800",
            "\u283f\u2800\u2800",
            "\u283f\u2800\u2800",
            "\u283f\u2800 ",
            "\u283f\u2800 ",
            "\u283f\u2800 ",
            "\u283f\u2800 ",
            "\u283f\u2800 ",
            "\u283f\u2800 "
        };
    }

    /**
     * Returns rain animation frames (12 frames).
     * Falling dots with staggered offsets.
     *
     * @return an array of 12 frame strings
     */
    public static String[] rain() {
        return new String[] {
            "\u280b",
            "\u2819",
            "\u2839",
            "\u2838",
            "\u283c",
            "\u2834",
            "\u2826",
            "\u2827",
            "\u2807",
            "\u280f",
            "\u280b",
            "\u2819"
        };
    }

    /**
     * Returns scanline animation frames (6 frames).
     * A vertical scan with fade effect.
     *
     * @return an array of 6 frame strings
     */
    public static String[] scanLine() {
        return new String[] {
            "\u2801",
            "\u2803",
            "\u2807",
            "\u2805",
            "\u2803",
            "\u2801"
        };
    }

    /**
     * Returns pulse animation frames (5 frames).
     * Expanding/contracting circle pattern.
     *
     * @return an array of 5 frame strings
     */
    public static String[] pulse() {
        return new String[] {
            "\u2801",
            "\u2803",
            "\u2807",
            "\u2805",
            "\u2803"
        };
    }

    /**
     * Returns snake animation frames (16 frames).
     * A snake-like path animation.
     *
     * @return an array of 16 frame strings
     */
    public static String[] snake() {
        return new String[] {
            "\u2801",
            "\u2803",
            "\u2805",
            "\u2807",
            "\u280d",
            "\u280b",
            "\u281b",
            "\u281f",
            "\u2800",
            "\u2801",
            "\u2803",
            "\u2805",
            "\u2807",
            "\u280d",
            "\u280b",
            "\u281b"
        };
    }

    /**
     * Returns sparkle animation frames (6 frames).
     * Sparkle patterns with rotation.
     *
     * @return an array of 6 frame strings
     */
    public static String[] sparkle() {
        return new String[] {
            "\u2800\u2800\u2800",
            "\u2800\u2800\u2801",
            "\u2800\u2801\u2802",
            "\u2801\u2802\u2804",
            "\u2802\u2804\u2800",
            "\u2804\u2800\u2800"
        };
    }

    /**
     * Returns cascade animation frames (12 frames).
     * Diagonal cascade pattern.
     *
     * @return an array of 12 frame strings
     */
    public static String[] cascade() {
        return new String[] {
            "\u2808",
            "\u2800\u2800",
            "\u2800\u2800",
            "\u2800\u2800",
            "\u28ff",
            "\u283f",
            "\u283e",
            "\u283c",
            "\u2800",
            "\u2801",
            "\u2802",
            "\u2803"
        };
    }

    /**
     * Returns columns animation frames (26 frames).
     * Column fill animation with all dot combinations.
     *
     * @return an array of 26 frame strings
     */
    public static String[] columns() {
        return new String[] {
            "\u2801",
            "\u2802",
            "\u2804",
            "\u2820",
            "\u2830",
            "\u2838",
            "\u283c",
            "\u2834",
            "\u2826",
            "\u2827",
            "\u2807",
            "\u280f",
            "\u280b",
            "\u2809",
            "\u2819",
            "\u281a",
            "\u2812",
            "\u28f2",
            "\u28b4",
            "\u28b4",
            "\u28a4",
            "\u28c4",
            "\u280b",
            "\u2809",
            "\u2800\u2800",
            "\u2800\u2800"
        };
    }

    /**
     * Returns orbit animation frames (8 frames).
     * Orbiting dot around a center point.
     *
     * @return an array of 8 frame strings
     */
    public static String[] orbit() {
        return new String[] {
            "\u2801",
            "\u2803",
            "\u2805",
            "\u2807",
            "\u280d",
            "\u280b",
            "\u281b",
            "\u281f"
        };
    }

    /**
     * Returns breathe animation frames (18 frames).
     * Breathing/pulsing dots that expand and contract.
     *
     * @return an array of 18 frame strings
     */
    public static String[] breathe() {
        return new String[] {
            " ",
            "\u2801",
            "\u2803",
            "\u2807",
            "\u2805",
            "\u280d",
            "\u280b",
            "\u281b",
            "\u281f",
            "\u28ff",
            "\u28bf",
            "\u289f",
            "\u283f",
            "\u281f",
            "\u281b",
            "\u280f",
            "\u2807",
            " "
        };
    }

    /**
     * Returns wave rows animation frames (16 frames).
     * A wave that moves across multiple rows.
     *
     * @return an array of 16 frame strings
     */
    public static String[] waveRows() {
        return new String[] {
            "\u2800\u2800",
            "\u2800\u2801",
            "\u2801\u2802",
            "\u2802\u2803",
            "\u2803\u2804",
            "\u2804\u2805",
            "\u2805\u2806",
            "\u2806\u2807",
            "\u2807\u2808",
            "\u2808\u2809",
            "\u2809\u280a",
            "\u280a\u280b",
            "\u280b\u280c",
            "\u280c\u280d",
            "\u280d\u280e",
            "\u280e\u280f"
        };
    }

    /**
     * Returns checkerboard animation frames (4 frames).
     * Checkerboard toggle pattern.
     *
     * @return an array of 4 frame strings
     */
    public static String[] checkerboard() {
        return new String[] {
            "\u2801\u2803",
            "\u2805\u2807",
            "\u280d\u280b",
            "\u281b\u281f"
        };
    }

    /**
     * Returns helix animation frames (16 frames).
     * Double helix wave pattern.
     *
     * @return an array of 16 frame strings
     */
    public static String[] helix() {
        return new String[] {
            "\u28ff\u283e",
            "\u283e\u283d",
            "\u283d\u283c",
            "\u283c\u283f",
            "\u283f\u28ff",
            "\u28ff\u28ff",
            "\u28ff\u283e",
            "\u283e\u283d",
            "\u283d\u283c",
            "\u283c\u283f",
            "\u283f\u28ff",
            "\u28ff\u28ff",
            "\u28ff\u283e",
            "\u283e\u283d",
            "\u283d\u283c",
            "\u283c\u283f"
        };
    }

    /**
     * Returns fill sweep animation frames (11 frames).
     * Fill sweep from left to right.
     *
     * @return an array of 11 frame strings
     */
    public static String[] fillSweep() {
        return new String[] {
            " ",
            "\u2801",
            "\u2803",
            "\u2805",
            "\u28ff",
            "\u28ff",
            "\u28ff",
            "\u2820",
            "\u2800\u2800",
            "\u2800\u2800",
            " "
        };
    }

    /**
     * Returns diagonal swipe animation frames (16 frames).
     * Diagonal fill and unfill pattern.
     *
     * @return an array of 16 frame strings
     */
    public static String[] diagonalSwipe() {
        return new String[] {
            " ",
            "\u2801",
            "\u2803",
            "\u2805",
            "\u2820",
            "\u28ff",
            "\u28ff",
            "\u28ff",
            "\u2820",
            "\u2830",
            "\u2800\u2800",
            " ",
            " ",
            "\u2801",
            "\u2803",
            "\u2805"
        };
    }

    /**
     * Returns braille wave animation frames (8 frames).
     * Multi-character wave animation.
     *
     * @return an array of 8 frame strings
     */
    public static String[] brailleWave() {
        return new String[] {
            "\u2801\u2802\u2804\u2800",
            "\u2802\u2804\u2800\u2800",
            "\u2804\u2800\u2800\u2800",
            "\u2800\u2800\u2800\u2800",
            "\u2800\u2800\u2800\u2800",
            "\u2800\u2800\u2800\u2800",
            "\u2800\u2800\u2800\u2800",
            "\u2800\u2800\u2800\u2800"
        };
    }

    /**
     * Returns DNA helix animation frames (12 frames).
     * Double helix pattern with dot combinations.
     *
     * @return an array of 12 frame strings
     */
    public static String[] dna() {
        return new String[] {
            "\u280b\u2809\u2819\u281a",
            "\u2809\u2819\u281a\u2812",
            "\u2819\u281a\u2812\u2812",
            "\u281a\u2812\u2812\u2812",
            "\u2812\u2812\u2812\u28f2",
            "\u2812\u2812\u28f2\u28b4",
            "\u2812\u28f2\u28b4\u28b4",
            "\u28f2\u28b4\u28b4\u28a4",
            "\u28b4\u28b4\u28a4\u28c4",
            "\u28b4\u28a4\u28c4\u280b",
            "\u28a4\u28c4\u280b\u2809",
            "\u28c4\u280b\u2809\u2819"
        };
    }

    /**
     * Creates a SpinnerFrameSet from a frame array.
     *
     * @param frames the frame array to use
     * @return a SpinnerFrameSet containing the frames
     */
    public static dev.tamboui.widgets.spinner.SpinnerFrameSet of(String[] frames) {
        return dev.tamboui.widgets.spinner.SpinnerFrameSet.of(frames);
    }

    /**
     * Gets all available frame patterns.
     *
     * @return a list of pattern method names
     */
    public static List<String> availablePatterns() {
        List<String> patterns = new ArrayList<>();
        patterns.add("scan");
        patterns.add("rain");
        patterns.add("scanLine");
        patterns.add("pulse");
        patterns.add("snake");
        patterns.add("sparkle");
        patterns.add("cascade");
        patterns.add("columns");
        patterns.add("orbit");
        patterns.add("breathe");
        patterns.add("waveRows");
        patterns.add("checkerboard");
        patterns.add("helix");
        patterns.add("fillSweep");
        patterns.add("diagonalSwipe");
        patterns.add("brailleWave");
        patterns.add("dna");
        return patterns;
    }
}