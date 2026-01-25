/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.text;

import java.util.Arrays;

/**
 * Utility for determining the display width of Unicode code points in a terminal.
 * <p>
 * Terminals render characters at different widths:
 * <ul>
 *   <li><b>2-wide</b>: CJK ideographs, Hangul syllables, fullwidth forms, most emoji</li>
 *   <li><b>0-wide</b>: Combining marks, zero-width joiners, variation selectors</li>
 *   <li><b>1-wide</b>: Latin, Cyrillic, Arabic, and most other characters</li>
 * </ul>
 * <p>
 * Uses a pre-computed lookup table for BMP characters (O(1)) and binary search
 * for supplementary plane characters.
 */
public final class CharWidth {

    private CharWidth() {
    }

    // Pre-computed width lookup for BMP characters (0x0000-0xFFFF).
    // Each byte stores the display width (0, 1, or 2) for that code point.
    private static final byte[] BMP_WIDTHS = new byte[0x10000];

    // Sorted start values of supplementary plane wide ranges
    private static final int[] SUPPLEMENTARY_WIDE_STARTS = {
            0x1F000, // Mahjong Tiles, Domino Tiles
            0x1F0A0, // Playing Cards
            0x1F100, // Enclosed Alphanumeric Supplement
            0x1F1E0, // Regional Indicator Symbols (Flags)
            0x1F200, // Enclosed Ideographic Supplement
            0x1F300, // Miscellaneous Symbols and Pictographs
            0x1F600, // Emoticons
            0x1F680, // Transport and Map Symbols
            0x1F900, // Supplemental Symbols and Pictographs
            0x1FA00, // Chess Symbols
            0x1FA70, // Symbols and Pictographs Extended-A
            0x20000, // CJK Unified Ideographs Extension B-F, CJK Compat Supplement
    };
    private static final int[] SUPPLEMENTARY_WIDE_ENDS = {
            0x1F02F, 0x1F0FF, 0x1F1DF, 0x1F1FF, 0x1F2FF,
            0x1F5FF, 0x1F64F, 0x1F6FF, 0x1F9FF, 0x1FA6F,
            0x1FAFF, 0x2FA1F,
    };

    // Sorted start/end values of supplementary plane zero-width ranges
    private static final int[] SUPPLEMENTARY_ZERO_STARTS = {
            0xE0001, // Tags
            0xE0100, // Variation Selectors Supplement (VS17-VS256)
    };
    private static final int[] SUPPLEMENTARY_ZERO_ENDS = {
            0xE007F, 0xE01EF,
    };

    static {
        // Initialize all BMP code points to width 1 (default)
        Arrays.fill(BMP_WIDTHS, (byte) 1);

        // Mark zero-width BMP ranges
        int[][] zeroWidthRanges = {
                {0x00AD, 0x00AD},   // Soft hyphen
                {0x0300, 0x036F},   // Combining Diacritical Marks
                {0x0483, 0x0489},   // Cyrillic combining marks
                {0x0591, 0x05BD},   // Hebrew combining marks
                {0x05BF, 0x05BF},
                {0x05C1, 0x05C2},
                {0x05C4, 0x05C5},
                {0x05C7, 0x05C7},
                {0x0610, 0x061A},   // Arabic combining marks
                {0x064B, 0x065F},
                {0x0670, 0x0670},
                {0x06D6, 0x06DC},
                {0x06DF, 0x06E4},
                {0x06E7, 0x06E8},
                {0x06EA, 0x06ED},
                {0x0711, 0x0711},   // Syriac
                {0x0730, 0x074A},
                {0x0900, 0x0902},   // Devanagari combining marks
                {0x093A, 0x093A},
                {0x093C, 0x093C},
                {0x0941, 0x0948},
                {0x094D, 0x094D},
                {0x0951, 0x0957},
                {0x0962, 0x0963},
                {0x0981, 0x0981},   // Bengali combining marks
                {0x09BC, 0x09BC},
                {0x09C1, 0x09C4},
                {0x09CD, 0x09CD},
                {0x09E2, 0x09E3},
                {0x0A01, 0x0A02},   // Gurmukhi combining marks
                {0x0A3C, 0x0A3C},
                {0x0A41, 0x0A42},
                {0x0A47, 0x0A48},
                {0x0A4B, 0x0A4D},
                {0x0A51, 0x0A51},
                {0x0A70, 0x0A71},
                {0x0A75, 0x0A75},
                {0x0E31, 0x0E31},   // Thai combining marks
                {0x0E34, 0x0E3A},
                {0x0E47, 0x0E4E},
                {0x1AB0, 0x1AFF},   // Combining Diacritical Marks Extended
                {0x1DC0, 0x1DFF},   // Combining Diacritical Marks Supplement
                {0x200B, 0x200F},   // Zero-width space, ZWNJ, ZWJ, directional marks
                {0x2028, 0x202F},   // Line/paragraph separators, directional formatting
                {0x2060, 0x2064},   // Word joiner, invisible operators
                {0x2066, 0x206F},   // Directional isolates and formatting
                {0x20D0, 0x20FF},   // Combining Diacritical Marks for Symbols
                {0xFE00, 0xFE0F},   // Variation Selectors (VS1-VS16)
                {0xFE20, 0xFE2F},   // Combining Half Marks
                {0xFEFF, 0xFEFF},   // Zero-width no-break space (BOM)
        };
        for (int[] range : zeroWidthRanges) {
            for (int cp = range[0]; cp <= range[1]; cp++) {
                BMP_WIDTHS[cp] = 0;
            }
        }

        // Mark wide (width 2) BMP ranges
        // For U+2000-U+2BFF: only characters with East_Asian_Width=W or
        // Emoji_Presentation property (rendered as 2-wide by terminals).
        int[][] wideRanges = {
                {0x231A, 0x231B},   // Watch, Hourglass
                {0x23E9, 0x23EC},   // Fast-forward, Rewind, Fast-up, Fast-down
                {0x23F0, 0x23F0},   // Alarm Clock
                {0x23F3, 0x23F3},   // Hourglass Not Done
                {0x25FD, 0x25FE},   // Medium Small Squares
                {0x2614, 0x2615},   // Umbrella with Rain, Hot Beverage
                {0x2630, 0x2637},   // Trigrams (EAW=W)
                {0x2648, 0x2653},   // Zodiac Signs
                {0x267F, 0x267F},   // Wheelchair Symbol
                {0x268A, 0x268F},   // I Ching Monograms/Digrams (EAW=W)
                {0x2693, 0x2693},   // Anchor
                {0x26A1, 0x26A1},   // High Voltage
                {0x26AA, 0x26AB},   // Medium White/Black Circle
                {0x26BD, 0x26BE},   // Soccer Ball, Baseball
                {0x26C4, 0x26C5},   // Snowman, Sun Behind Cloud
                {0x26CE, 0x26CE},   // Ophiuchus
                {0x26D4, 0x26D4},   // No Entry
                {0x26EA, 0x26EA},   // Church
                {0x26F2, 0x26F3},   // Fountain, Flag in Hole
                {0x26F5, 0x26F5},   // Sailboat
                {0x26FA, 0x26FA},   // Tent
                {0x26FD, 0x26FD},   // Fuel Pump
                {0x2705, 0x2705},   // White Heavy Check Mark
                {0x270A, 0x270B},   // Raised Fist, Raised Hand
                {0x2728, 0x2728},   // Sparkles
                {0x274C, 0x274C},   // Cross Mark
                {0x274E, 0x274E},   // Cross Mark Button
                {0x2753, 0x2755},   // Question/Exclamation Ornaments
                {0x2757, 0x2757},   // Heavy Exclamation Mark
                {0x2795, 0x2797},   // Heavy Plus, Minus, Division
                {0x27B0, 0x27B0},   // Curly Loop
                {0x27BF, 0x27BF},   // Double Curly Loop
                {0x2B05, 0x2B07},   // Arrows with emoji presentation
                {0x2B1B, 0x2B1C},   // Large Black/White Square
                {0x2B50, 0x2B50},   // Star
                {0x2B55, 0x2B55},   // Heavy Large Circle
                {0x2E80, 0x2FDF},   // CJK Radicals Supplement, Kangxi Radicals
                {0x2FF0, 0x303E},   // CJK Symbols and Punctuation
                {0x3041, 0x33FF},   // Hiragana, Katakana, Bopomofo, CJK Compatibility
                {0x3400, 0x4DBF},   // CJK Unified Ideographs Extension A
                {0x4E00, 0x9FFF},   // CJK Unified Ideographs
                {0xA000, 0xA4CF},   // Yi Syllables and Radicals
                {0xAC00, 0xD7AF},   // Hangul Syllables
                {0xF900, 0xFAFF},   // CJK Compatibility Ideographs
                {0xFF01, 0xFF60},   // Fullwidth Forms
                {0xFFE0, 0xFFE6},   // Fullwidth Signs
        };
        for (int[] range : wideRanges) {
            for (int cp = range[0]; cp <= range[1]; cp++) {
                BMP_WIDTHS[cp] = 2;
            }
        }
    }

    /**
     * Returns the display width (0, 1, or 2) of a Unicode code point.
     *
     * @param codePoint the Unicode code point
     * @return 0 for zero-width characters, 2 for wide characters, 1 otherwise
     */
    public static int of(int codePoint) {
        if (codePoint < 0x10000) {
            return BMP_WIDTHS[codePoint];
        }

        // Supplementary plane: use Arrays.binarySearch on start values
        if (inRanges(codePoint, SUPPLEMENTARY_WIDE_STARTS, SUPPLEMENTARY_WIDE_ENDS)) {
            return 2;
        }

        if (inRanges(codePoint, SUPPLEMENTARY_ZERO_STARTS, SUPPLEMENTARY_ZERO_ENDS)) {
            return 0;
        }

        return 1;
    }

    /**
     * Returns the total display width of a string.
     *
     * @param s the string to measure
     * @return the total display width in terminal columns
     */
    public static int of(String s) {
        if (s == null || s.isEmpty()) {
            return 0;
        }
        int width = 0;
        int i = 0;
        while (i < s.length()) {
            int codePoint = s.codePointAt(i);
            width += of(codePoint);
            i += Character.charCount(codePoint);
        }
        return width;
    }

    /**
     * Returns a substring that fits within the given display width,
     * respecting code point boundaries.
     * <p>
     * If a wide character would exceed maxWidth, it is not included.
     *
     * @param s the source string
     * @param maxWidth the maximum display width in columns
     * @return a substring fitting within maxWidth columns
     */
    public static String substringByWidth(String s, int maxWidth) {
        if (s == null || s.isEmpty() || maxWidth <= 0) {
            return "";
        }
        int width = 0;
        int i = 0;
        while (i < s.length()) {
            int codePoint = s.codePointAt(i);
            int charWidth = of(codePoint);
            if (width + charWidth > maxWidth) {
                break;
            }
            width += charWidth;
            i += Character.charCount(codePoint);
        }
        return s.substring(0, i);
    }

    /**
     * Returns a substring starting from the end that fits within the given display width.
     *
     * @param s the source string
     * @param maxWidth the maximum display width in columns
     * @return a suffix substring fitting within maxWidth columns
     */
    public static String substringByWidthFromEnd(String s, int maxWidth) {
        if (s == null || s.isEmpty() || maxWidth <= 0) {
            return "";
        }
        int i = s.length();
        int width = 0;
        while (i > 0) {
            int codePoint = s.codePointBefore(i);
            int charWidth = of(codePoint);
            if (width + charWidth > maxWidth) {
                break;
            }
            width += charWidth;
            i -= Character.charCount(codePoint);
        }
        return s.substring(i);
    }

    private static boolean inRanges(int codePoint, int[] starts, int[] ends) {
        int idx = Arrays.binarySearch(starts, codePoint);
        if (idx >= 0) {
            // Exact match on a start value - it's within the range
            return true;
        }
        // Insertion point: the index of the first element greater than codePoint
        int insertionPoint = -(idx + 1);
        // Check if codePoint falls within the preceding range
        return insertionPoint > 0 && codePoint <= ends[insertionPoint - 1];
    }
}
