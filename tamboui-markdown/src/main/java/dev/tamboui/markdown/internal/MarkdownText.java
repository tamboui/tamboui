/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.markdown.internal;

import java.util.Arrays;

/**
 * Small text helpers shared by the markdown layout classes.
 */
final class MarkdownText {

    private MarkdownText() {
    }

    /**
     * Returns a string of {@code count} copies of {@code c}, or an empty string when {@code count} is not positive.
     */
    static String repeat(char c, int count) {
        if (count <= 0) {
            return "";
        }
        char[] buf = new char[count];
        Arrays.fill(buf, c);
        return new String(buf);
    }
}
