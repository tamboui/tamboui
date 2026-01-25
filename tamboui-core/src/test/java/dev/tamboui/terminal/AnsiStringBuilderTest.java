/*
a * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.terminal;

import dev.tamboui.style.AnsiColor;
import dev.tamboui.style.Color;
import dev.tamboui.style.Hyperlink;
import dev.tamboui.style.Modifier;
import dev.tamboui.style.Style;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
    @DisplayName("colorToAnsiForeground with Reset")
    void foregroundReset() {
        String result = AnsiStringBuilder.colorToAnsiForeground(Color.RESET);
        assertThat(result).isEqualTo("39");
    }

    @Test
    @DisplayName("colorToAnsiForeground with ANSI color")
    void foregroundAnsiColor() {
        String result = AnsiStringBuilder.colorToAnsiForeground(Color.RED);
        assertThat(result).isEqualTo("31");
    }

    @Test
    @DisplayName("colorToAnsiForeground with bright ANSI color")
    void foregroundBrightAnsiColor() {
        String result = AnsiStringBuilder.colorToAnsiForeground(new Color.Ansi(AnsiColor.BRIGHT_RED));
        assertThat(result).isEqualTo("91");
    }

    @Test
    @DisplayName("colorToAnsiForeground with indexed color")
    void foregroundIndexedColor() {
        String result = AnsiStringBuilder.colorToAnsiForeground(Color.indexed(123));
        assertThat(result).isEqualTo("38;5;123");
    }

    @Test
    @DisplayName("colorToAnsiForeground with RGB color")
    void foregroundRgbColor() {
        String result = AnsiStringBuilder.colorToAnsiForeground(Color.rgb(100, 150, 200));
        assertThat(result).isEqualTo("38;2;100;150;200");
    }

    @Test
    @DisplayName("colorToAnsiBackground with Reset")
    void backgroundReset() {
        String result = AnsiStringBuilder.colorToAnsiBackground(Color.RESET);
        assertThat(result).isEqualTo("49");
    }

    @Test
    @DisplayName("colorToAnsiBackground with ANSI color")
    void backgroundAnsiColor() {
        String result = AnsiStringBuilder.colorToAnsiBackground(Color.RED);
        assertThat(result).isEqualTo("41");
    }

    @Test
    @DisplayName("colorToAnsiBackground with indexed color")
    void backgroundIndexedColor() {
        String result = AnsiStringBuilder.colorToAnsiBackground(Color.indexed(45));
        assertThat(result).isEqualTo("48;5;45");
    }

    @Test
    @DisplayName("colorToAnsiBackground with RGB color")
    void backgroundRgbColor() {
        String result = AnsiStringBuilder.colorToAnsiBackground(Color.rgb(255, 128, 64));
        assertThat(result).isEqualTo("48;2;255;128;64");
    }

    @Test
    @DisplayName("underlineColorToAnsi with indexed color")
    void underlineIndexedColor() {
        String result = AnsiStringBuilder.underlineColorToAnsi(Color.indexed(200));
        assertThat(result).isEqualTo("58;5;200");
    }

    @Test
    @DisplayName("underlineColorToAnsi with RGB color")
    void underlineRgbColor() {
        String result = AnsiStringBuilder.underlineColorToAnsi(Color.rgb(10, 20, 30));
        assertThat(result).isEqualTo("58;2;10;20;30");
    }

    @Test
    @DisplayName("underlineColorToAnsi with unsupported color returns empty")
    void underlineUnsupportedColor() {
        String result = AnsiStringBuilder.underlineColorToAnsi(Color.RED);
        assertThat(result).isEmpty();
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
    @DisplayName("hyperlinkStart escapes id and url")
    void hyperlinkStartEscapesParams() {
        Hyperlink hyperlink = Hyperlink.of("https://example.com/a;b\\c", "id;1\\2");
        String result = AnsiStringBuilder.hyperlinkStart(hyperlink);
        assertThat(result).isEqualTo("\u001b]8;id=id\\;1\\\\2;https://example.com/a\\;b\\\\c\u001b\\");
    }

    @Test
    @DisplayName("hyperlinkEnd sequence")
    void hyperlinkEndSequence() {
        String result = AnsiStringBuilder.hyperlinkEnd();
        assertThat(result).isEqualTo("\u001b]8;;\u001b\\");
    }

}
