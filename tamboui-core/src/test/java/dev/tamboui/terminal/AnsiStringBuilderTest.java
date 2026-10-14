/*
a * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.terminal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.style.Color;
import dev.tamboui.style.Hyperlink;
import dev.tamboui.style.Modifier;
import dev.tamboui.style.Style;

import static org.assertj.core.api.Assertions.assertThat;

class AnsiStringBuilderTest {

    @Test
    @DisplayName("RESET constant is correct ANSI sequence")
    void resetConstant() {
        assertThat(AnsiStringBuilder.RESET).isEqualTo("\u001b[0m");
    }

    @Test
    @DisplayName("styleToAnsi with empty style returns reset only")
    void emptyStyle() {
        String result = AnsiStringBuilder.styleToAnsi(Style.EMPTY);
        assertThat(result).isEqualTo("\u001b[0m");
    }

    @Test
    @DisplayName("styleToAnsi with foreground color")
    void foregroundColor() {
        Style style = Style.EMPTY.fg(Color.RED);
        String result = AnsiStringBuilder.styleToAnsi(style);
        assertThat(result).isEqualTo("\u001b[0;31m");
    }

    @Test
    @DisplayName("styleToAnsi with background color")
    void backgroundColor() {
        Style style = Style.EMPTY.bg(Color.BLUE);
        String result = AnsiStringBuilder.styleToAnsi(style);
        assertThat(result).isEqualTo("\u001b[0;44m");
    }

    @Test
    @DisplayName("styleToAnsi with both foreground and background")
    void foregroundAndBackground() {
        Style style = Style.EMPTY.fg(Color.GREEN).bg(Color.BLACK);
        String result = AnsiStringBuilder.styleToAnsi(style);
        assertThat(result).isEqualTo("\u001b[0;32;40m");
    }

    @Test
    @DisplayName("styleToAnsi with bold modifier")
    void boldModifier() {
        Style style = Style.EMPTY.bold();
        String result = AnsiStringBuilder.styleToAnsi(style);
        assertThat(result).isEqualTo("\u001b[0;1m");
    }

    @Test
    @DisplayName("styleToAnsi with multiple modifiers")
    void multipleModifiers() {
        Style style = Style.EMPTY.bold().italic().underlined();
        String result = AnsiStringBuilder.styleToAnsi(style);
        // Order may vary based on EnumSet iteration
        assertThat(result).startsWith("\u001b[0;");
        assertThat(result).endsWith("m");
        assertThat(result).contains(";1");   // bold
        assertThat(result).contains(";3");   // italic
        assertThat(result).contains(";4");   // underlined
    }

    @Test
    @DisplayName("styleToAnsi with NORMAL keeps the colors")
    void normalModifierKeepsColors() {
        Style style = Style.EMPTY.fg(Color.RED).bg(Color.BLUE).addModifier(Modifier.NORMAL);
        String result = AnsiStringBuilder.styleToAnsi(style);
        assertThat(result).isEqualTo("\u001b[0;31;44m");
    }

    @Test
    @DisplayName("styleToAnsi with NORMAL and another modifier writes only the other modifier")
    void normalModifierWithBold() {
        Style style = Style.EMPTY.fg(Color.RED).addModifier(Modifier.NORMAL).bold();
        String result = AnsiStringBuilder.styleToAnsi(style);
        assertThat(result).isEqualTo("\u001b[0;31;1m");
    }

    @Test
    @DisplayName("styleToAnsi with complete style")
    void completeStyle() {
        Style style = Style.EMPTY.fg(Color.CYAN).bg(Color.MAGENTA).bold();
        String result = AnsiStringBuilder.styleToAnsi(style);
        assertThat(result).startsWith("\u001b[0;");
        assertThat(result).endsWith("m");
        assertThat(result).contains(";36");  // cyan fg
        assertThat(result).contains(";45");  // magenta bg
        assertThat(result).contains(";1");   // bold
    }

    @Test
    @DisplayName("hyperlinkStart without id")
    void hyperlinkStartWithoutId() {
        Hyperlink hyperlink = Hyperlink.of("https://example.com");
        String result = AnsiStringBuilder.hyperlinkStart(hyperlink);
        assertThat(result).isEqualTo("\u001b]8;;https://example.com\u001b\\");
    }

    @Test
    @DisplayName("hyperlinkStart with id")
    void hyperlinkStartWithId() {
        Hyperlink hyperlink = Hyperlink.of("https://example.com", "link-1");
        String result = AnsiStringBuilder.hyperlinkStart(hyperlink);
        assertThat(result).isEqualTo("\u001b]8;id=link-1;https://example.com\u001b\\");
    }

    @Test
    @DisplayName("hyperlinkStart passes semicolons and backslashes through unchanged")
    void hyperlinkStartPassesThroughSeparators() {
        Hyperlink hyperlink = Hyperlink.of("https://example.com/a;b\\c", "id;1\\2");
        String result = AnsiStringBuilder.hyperlinkStart(hyperlink);
        assertThat(result).isEqualTo("\u001b]8;id=id;1\\2;https://example.com/a;b\\c\u001b\\");
    }

    @Test
    @DisplayName("hyperlinkStart percent-encodes control characters in url (GHSA-5m5f-4gqv-8c29)")
    void hyperlinkStartSanitizesControlCharsInUrl() {
        Hyperlink hyperlink = Hyperlink.of("https://example.com/\u0007\u001b[2J");
        String result = AnsiStringBuilder.hyperlinkStart(hyperlink);
        assertThat(result).isEqualTo("\u001b]8;;https://example.com/%07%1B[2J\u001b\\");
        assertThat(result).doesNotContain("\u0007");
        assertThat(result).doesNotContain("\u001b[2J");
    }

    @Test
    @DisplayName("hyperlinkStart percent-encodes control characters in id")
    void hyperlinkStartSanitizesControlCharsInId() {
        Hyperlink hyperlink = Hyperlink.of("https://example.com", "a\u001bb\u007fc");
        String result = AnsiStringBuilder.hyperlinkStart(hyperlink);
        assertThat(result).isEqualTo("\u001b]8;id=a%1Bb%7Fc;https://example.com\u001b\\");
    }

    @Test
    @DisplayName("hyperlinkEnd sequence")
    void hyperlinkEndSequence() {
        String result = AnsiStringBuilder.hyperlinkEnd();
        assertThat(result).isEqualTo("\u001b]8;;\u001b\\");
    }

    @Test
    @DisplayName("windowTitle sequence")
    void windowTitleSequence() {
        String result = AnsiStringBuilder.windowTitle("My App");
        assertThat(result).isEqualTo("\u001b]2;My App\u0007");
    }

    @Test
    @DisplayName("windowTitle strips ESC and BEL to prevent injection")
    void windowTitleStripsControlCharacters() {
        String result = AnsiStringBuilder.windowTitle("evil\u0007\u001b]2;spoofed");
        assertThat(result).isEqualTo("\u001b]2;evil]2;spoofed\u0007");
    }

    @Test
    @DisplayName("saveWindowTitle sequence")
    void saveWindowTitleSequence() {
        assertThat(AnsiStringBuilder.saveWindowTitle()).isEqualTo("\u001b[22;2t");
    }

    @Test
    @DisplayName("restoreWindowTitle sequence")
    void restoreWindowTitleSequence() {
        assertThat(AnsiStringBuilder.restoreWindowTitle()).isEqualTo("\u001b[23;2t");
    }

}
