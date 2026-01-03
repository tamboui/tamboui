/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.figlet;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class FigletTest {

    @Test
    @DisplayName("Bundled fonts load from classpath (no external figlet binary)")
    void bundledFontsLoad() {
        assertThat(FigletFont.bundled(BundledFigletFont.MINI).height()).isGreaterThan(0);
        assertThat(FigletFont.bundled(BundledFigletFont.SMALL).height()).isGreaterThan(0);
        assertThat(FigletFont.bundled(BundledFigletFont.STANDARD).height()).isGreaterThan(0);
        assertThat(FigletFont.bundled(BundledFigletFont.SLANT).height()).isGreaterThan(0);
        assertThat(FigletFont.bundled(BundledFigletFont.BIG).height()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Figlet renders MINI font")
    void rendersMini() {
        Figlet widget = Figlet.builder()
            .text("Hi")
            .font(BundledFigletFont.MINI)
            .build();

        Rect area = new Rect(0, 0, 20, 6);
        Buffer buffer = Buffer.empty(area);
        widget.render(area, buffer);

        // Basic sanity checks (no FIGlet endmarks / hardblanks)
        assertThat(row(buffer, 1, 20)).contains("|").contains("o").doesNotContain("@").doesNotContain("$");
        assertThat(row(buffer, 2, 20)).contains("|").doesNotContain("@").doesNotContain("$");
    }

    @Test
    @DisplayName("Figlet renders SMALL font")
    void rendersSmall() {
        Figlet widget = Figlet.builder()
            .text("Hi")
            .font(BundledFigletFont.SMALL)
            .build();

        Rect area = new Rect(0, 0, 30, 10);
        Buffer buffer = Buffer.empty(area);
        widget.render(area, buffer);

        assertThat(row(buffer, 0, 30)).contains("_").doesNotContain("@").doesNotContain("$");
        assertThat(row(buffer, 1, 30)).contains("|").doesNotContain("@").doesNotContain("$");
        assertThat(row(buffer, 2, 30)).contains("|").doesNotContain("@").doesNotContain("$");
    }

    private static String row(Buffer buffer, int y, int width) {
        StringBuilder sb = new StringBuilder(width);
        for (int x = 0; x < width; x++) {
            sb.append(buffer.get(x, y).symbol());
        }
        return rtrim(sb.toString());
    }

    private static String rtrim(String s) {
        int end = s.length();
        while (end > 0 && s.charAt(end - 1) == ' ') {
            end--;
        }
        return s.substring(0, end);
    }
}

