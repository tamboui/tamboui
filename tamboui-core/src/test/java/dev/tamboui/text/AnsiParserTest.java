/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.text;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import dev.tamboui.assertj.BufferAssertions;
import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.AnsiColor;
import dev.tamboui.style.Color;
import dev.tamboui.style.Modifier;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.AnsiStringBuilder;

import static org.assertj.core.api.Assertions.assertThat;

class AnsiParserTest {

    private static final String ESC = "\u001b";
    private static final String CSI = ESC + "[";
    private static final String CODER = "👩\u200D💻";
    private static final String FAMILY = "👨\u200D👩\u200D👧";
    private static final Style RED = Style.EMPTY.fg(Color.ansi(AnsiColor.RED));
    private static final Style GREEN = Style.EMPTY.fg(Color.ansi(AnsiColor.GREEN));
    private static final Style BLUE = Style.EMPTY.fg(Color.ansi(AnsiColor.BLUE));

    // The style of the text after the given SGR parameters
    private static Style sgr(String parameters) {
        Line line = Line.fromAnsi(CSI + parameters + "mx");
        assertThat(line.spans()).hasSize(1);
        assertThat(line.spans().get(0).content()).isEqualTo("x");
        return line.spans().get(0).style();
    }

    @Nested
    @DisplayName("SGR attributes")
    class Attributes {

        @Test
        @DisplayName("1 to 9 set bold, dim, italic, underlined, blink, reversed, hidden and crossed out")
        void setAttributes() {
            assertThat(sgr("1")).isEqualTo(Style.EMPTY.bold());
            assertThat(sgr("2")).isEqualTo(Style.EMPTY.dim());
            assertThat(sgr("3")).isEqualTo(Style.EMPTY.italic());
            assertThat(sgr("4")).isEqualTo(Style.EMPTY.underlined());
            assertThat(sgr("5")).isEqualTo(Style.EMPTY.slowBlink());
            assertThat(sgr("6")).isEqualTo(Style.EMPTY.rapidBlink());
            assertThat(sgr("7")).isEqualTo(Style.EMPTY.reversed());
            assertThat(sgr("8")).isEqualTo(Style.EMPTY.hidden());
            assertThat(sgr("9")).isEqualTo(Style.EMPTY.crossedOut());
        }

        @Test
        @DisplayName("21 to 29 turn them off again, and 21 (doubly underlined) is underlined")
        void resetAttributes() {
            assertThat(sgr("1;2;22")).isEqualTo(Style.EMPTY);
            assertThat(sgr("1;3;22")).isEqualTo(Style.EMPTY.italic());
            assertThat(sgr("3;23")).isEqualTo(Style.EMPTY);
            assertThat(sgr("4;24")).isEqualTo(Style.EMPTY);
            assertThat(sgr("5;6;25")).isEqualTo(Style.EMPTY);
            assertThat(sgr("7;27")).isEqualTo(Style.EMPTY);
            assertThat(sgr("8;28")).isEqualTo(Style.EMPTY);
            assertThat(sgr("9;29")).isEqualTo(Style.EMPTY);
            assertThat(sgr("21")).isEqualTo(Style.EMPTY.underlined());
        }

        @Test
        @DisplayName("the underline styles 4:1 to 4:5 are underlined, and 4:0 is not")
        void underlineStyles() {
            assertThat(sgr("4:3")).isEqualTo(Style.EMPTY.underlined());
            assertThat(sgr("4;4:0")).isEqualTo(Style.EMPTY);
        }

        @Test
        @DisplayName("0 and an empty parameter reset everything")
        void reset() {
            assertThat(sgr("1;31;44;0")).isEqualTo(Style.EMPTY);
            assertThat(sgr("1;31;;4")).isEqualTo(Style.EMPTY.underlined());
            assertThat(Line.fromAnsi(CSI + "1;31m" + CSI + "mx")).isEqualTo(Line.from("x"));
        }

        @Test
        @DisplayName("attributes add up within a sequence and across sequences")
        void combine() {
            Style expected = RED.bg(Color.ansi(AnsiColor.BLUE)).bold().italic();
            assertThat(sgr("1;3;31;44")).isEqualTo(expected);
            assertThat(Line.fromAnsi(CSI + "1m" + CSI + "3m" + CSI + "31m" + CSI + "44mx"))
                .isEqualTo(Line.from(Span.styled("x", expected)));
        }

        @Test
        @DisplayName("codes without a TamboUI style are ignored")
        void unknownCodes() {
            assertThat(sgr("10;11;20;26;50;51;53;73;108;200")).isEqualTo(Style.EMPTY);
        }
    }

    @Nested
    @DisplayName("SGR colors")
    class Colors {

        @Test
        @DisplayName("30-37 and 90-97 set the foreground to the 16 ANSI colors")
        void foreground() {
            AnsiColor[] colors = AnsiColor.values();
            for (int i = 0; i < 8; i++) {
                assertThat(sgr(String.valueOf(30 + i))).isEqualTo(Style.EMPTY.fg(Color.ansi(colors[i])));
                assertThat(sgr(String.valueOf(90 + i))).isEqualTo(Style.EMPTY.fg(Color.ansi(colors[i + 8])));
            }
        }

        @Test
        @DisplayName("40-47 and 100-107 set the background to the 16 ANSI colors")
        void background() {
            AnsiColor[] colors = AnsiColor.values();
            for (int i = 0; i < 8; i++) {
                assertThat(sgr(String.valueOf(40 + i))).isEqualTo(Style.EMPTY.bg(Color.ansi(colors[i])));
                assertThat(sgr(String.valueOf(100 + i))).isEqualTo(Style.EMPTY.bg(Color.ansi(colors[i + 8])));
            }
        }

        @Test
        @DisplayName("38;5;n and 48;5;n set a 256-color index")
        void indexed() {
            assertThat(sgr("38;5;0")).isEqualTo(Style.EMPTY.fg(Color.indexed(0)));
            assertThat(sgr("38;5;208")).isEqualTo(Style.EMPTY.fg(Color.indexed(208)));
            assertThat(sgr("48;5;255")).isEqualTo(Style.EMPTY.bg(Color.indexed(255)));
        }

        @Test
        @DisplayName("38;2;r;g;b and 48;2;r;g;b set an RGB color")
        void rgb() {
            assertThat(sgr("38;2;255;128;0")).isEqualTo(Style.EMPTY.fg(Color.rgb(255, 128, 0)));
            assertThat(sgr("48;2;0;0;139")).isEqualTo(Style.EMPTY.bg(Color.rgb(0, 0, 139)));
        }

        @Test
        @DisplayName("the colon forms 38:5:n and 38:2:r:g:b, with or without a color space id, set the same colors")
        void colonForms() {
            assertThat(sgr("38:5:208")).isEqualTo(Style.EMPTY.fg(Color.indexed(208)));
            assertThat(sgr("38:2:255:128:0")).isEqualTo(Style.EMPTY.fg(Color.rgb(255, 128, 0)));
            assertThat(sgr("38:2::255:128:0")).isEqualTo(Style.EMPTY.fg(Color.rgb(255, 128, 0)));
            assertThat(sgr("48:2:0:10:20:30")).isEqualTo(Style.EMPTY.bg(Color.rgb(10, 20, 30)));
            assertThat(sgr("1;38:2::255:128:0;4"))
                .isEqualTo(Style.EMPTY.fg(Color.rgb(255, 128, 0)).bold().underlined());
        }

        @Test
        @DisplayName("the values of a color are not read as attributes")
        void valuesAreNotAttributes() {
            assertThat(sgr("38;5;1")).isEqualTo(Style.EMPTY.fg(Color.indexed(1)));
            assertThat(sgr("48;2;1;2;3")).isEqualTo(Style.EMPTY.bg(Color.rgb(1, 2, 3)));
            assertThat(sgr("58;5;9")).isEqualTo(Style.EMPTY.underlineColor(Color.indexed(9)));
        }

        @Test
        @DisplayName("58 and 59 set and clear the underline color")
        void underlineColor() {
            assertThat(sgr("4;58;5;196")).isEqualTo(Style.EMPTY.underlined().underlineColor(Color.indexed(196)));
            assertThat(sgr("58:2::1:2:3")).isEqualTo(Style.EMPTY.underlineColor(Color.rgb(1, 2, 3)));
            assertThat(sgr("58;5;196;59")).isEqualTo(Style.EMPTY);
        }

        @Test
        @DisplayName("39 and 49 go back to no color, so that the colors of the widget show")
        void defaultColors() {
            assertThat(sgr("31;44;39")).isEqualTo(Style.EMPTY.bg(Color.ansi(AnsiColor.BLUE)));
            assertThat(sgr("31;44;49")).isEqualTo(RED);
            assertThat(sgr("31;44;39;49")).isEqualTo(Style.EMPTY);
        }

        @Test
        @DisplayName("the style carries on to the next line of a text")
        void styleCarriesOn() {
            Text text = Text.fromAnsi(CSI + "31mfirst\nsecond" + CSI + "0m\nthird");

            assertThat(text.lines()).containsExactly(
                Line.from(Span.styled("first", RED)),
                Line.from(Span.styled("second", RED)),
                Line.from("third"));
        }
    }

    @Nested
    @DisplayName("dropped sequences")
    class Dropped {

        @Test
        @DisplayName("other CSI sequences, such as cursor movement and erase, are dropped")
        void otherControlSequences() {
            assertThat(Line.fromAnsi(CSI + "2J" + CSI + "1;1H" + CSI + "?25la" + CSI + "Kb" + CSI + "2 q" + CSI + "3Ac"))
                .isEqualTo(Line.from("abc"));
        }

        @Test
        @DisplayName("a CSI ending in m with other bytes is not SGR, such as xterm's CSI > 4 ; 2 m")
        void notSgr() {
            assertThat(Line.fromAnsi(CSI + ">4;2mx")).isEqualTo(Line.from("x"));
            assertThat(Line.fromAnsi(CSI + "?1mx")).isEqualTo(Line.from("x"));
            assertThat(Line.fromAnsi(CSI + "1 mx")).isEqualTo(Line.from("x"));
        }

        @Test
        @DisplayName("OSC up to BEL or ST is dropped, such as a window title")
        void operatingSystemCommands() {
            assertThat(Line.fromAnsi(ESC + "]0;my title\u0007text")).isEqualTo(Line.from("text"));
            assertThat(Line.fromAnsi("a" + ESC + "]2;my title" + ESC + "\\b")).isEqualTo(Line.from("ab"));
            assertThat(Line.fromAnsi(ESC + "]2;my title\u009ctext")).isEqualTo(Line.from("text"));
        }

        @Test
        @DisplayName("DCS, SOS, PM and APC strings are dropped")
        void strings() {
            assertThat(Line.fromAnsi("a" + ESC + "P1$r0m" + ESC + "\\b")).isEqualTo(Line.from("ab"));
            assertThat(Line.fromAnsi("a" + ESC + "Xsos" + ESC + "\\b")).isEqualTo(Line.from("ab"));
            assertThat(Line.fromAnsi("a" + ESC + "^pm" + ESC + "\\b")).isEqualTo(Line.from("ab"));
            assertThat(Line.fromAnsi("a" + ESC + "_Gf=100,a=T;iVBORw0KGgo=" + ESC + "\\b")).isEqualTo(Line.from("ab"));
        }

        @Test
        @DisplayName("two-character sequences and character set selections are dropped")
        void shortSequences() {
            assertThat(Line.fromAnsi(ESC + "7a" + ESC + "8b" + ESC + "Mc" + ESC + "=d")).isEqualTo(Line.from("abcd"));
            // tput sgr0 writes ESC ( B before its reset
            assertThat(Line.fromAnsi(CSI + "1mbold" + ESC + "(B" + CSI + "m plain"))
                .isEqualTo(Line.from(Span.styled("bold", Style.EMPTY.bold()), Span.raw(" plain")));
        }

        @Test
        @DisplayName("a lone ESC is dropped, and the character after it is kept")
        void loneEscape() {
            assertThat(Line.fromAnsi("a" + ESC + "é" + ESC)).isEqualTo(Line.from("aé"));
            assertThat(Line.fromAnsi("a" + ESC + CSI + "1mb"))
                .isEqualTo(Line.from(Span.raw("a"), Span.styled("b", Style.EMPTY.bold())));
        }

        @Test
        @DisplayName("CR is dropped, so CRLF ends a line like LF")
        void carriageReturn() {
            assertThat(Text.fromAnsi("one\r\ntwo\r\n")).isEqualTo(Text.from("one\ntwo"));
            assertThat(Line.fromAnsi("50%\r100%")).isEqualTo(Line.from("50%100%"));
        }

        @Test
        @DisplayName("the other control characters are dropped")
        void controlCharacters() {
            assertThat(Line.fromAnsi("a\u0007b\bc\u0000d\u007fe\u009bf\u000bg\u000ch")).isEqualTo(Line.from("abcdefgh"));
        }

        @Test
        @DisplayName("Line.fromAnsi drops line feeds")
        void lineFeedsInLine() {
            assertThat(Line.fromAnsi("a\nb\r\n")).isEqualTo(Line.from("ab"));
        }
    }

    @Nested
    @DisplayName("tabs")
    class Tabs {

        @Test
        @DisplayName("a tab becomes spaces up to the next multiple of 8 columns")
        void tabStops() {
            assertThat(Line.fromAnsi("\tx").rawContent()).isEqualTo("        x");
            assertThat(Line.fromAnsi("abc\tx").rawContent()).isEqualTo("abc     x");
            assertThat(Line.fromAnsi("abcdefg\tx").rawContent()).isEqualTo("abcdefg x");
            assertThat(Line.fromAnsi("abcdefgh\tx").rawContent()).isEqualTo("abcdefgh        x");
            assertThat(Line.fromAnsi("a\tb\tc").rawContent()).isEqualTo("a       b       c");
        }

        @Test
        @DisplayName("tab stops count display columns of the line, not escape sequences or chars")
        void tabStopsByDisplayWidth() {
            assertThat(Line.fromAnsi(CSI + "31mab" + CSI + "0m\tx").rawContent()).isEqualTo("ab      x");
            assertThat(Line.fromAnsi("世界\tx").rawContent()).isEqualTo("世界    x");
            assertThat(Line.fromAnsi(CODER + "\tx").rawContent()).isEqualTo(CODER + "      x");
            assertThat(Text.fromAnsi("abc\ndefghij\tx").lines().get(1).rawContent()).isEqualTo("defghij x");
        }

        @Test
        @DisplayName("the spaces of a tab take no style, as a terminal does not paint them")
        void tabSpacesTakeNoStyle() {
            Style onRed = Style.EMPTY.bg(Color.ansi(AnsiColor.RED));

            assertThat(Line.fromAnsi(CSI + "41ma\tb"))
                .isEqualTo(Line.from(Span.styled("a", onRed), Span.raw("       "), Span.styled("b", onRed)));
        }
    }

    @Nested
    @DisplayName("malformed input")
    class Malformed {

        @Test
        @DisplayName("a sequence cut off at the end is dropped")
        void cutOffAtTheEnd() {
            String[] cuts = {
                ESC, CSI, CSI + "31", CSI + "38;5", CSI + "38;2;255", CSI + "38:2:",
                ESC + "]0;title", ESC + "]2;title" + ESC, ESC + "]0;",
                ESC + "P1$r", ESC + "_G", ESC + "(",
            };
            for (String cut : cuts) {
                assertThat(Text.fromAnsi("text" + cut)).as(cut.replace(ESC, "ESC")).isEqualTo(Text.from("text"));
            }
        }

        @Test
        @DisplayName("a sequence broken off by a control character, another ESC or other text ends there")
        void brokenOff() {
            assertThat(Text.fromAnsi("a" + CSI + "31\nb")).isEqualTo(Text.from("a\nb"));
            assertThat(Line.fromAnsi(CSI + "3" + CSI + "32mx")).isEqualTo(Line.from(Span.styled("x", GREEN)));
            assertThat(Line.fromAnsi(CSI + "31é")).isEqualTo(Line.from("é"));
        }

        @Test
        @DisplayName("a string without a terminator ends at the end of its line or at the next ESC")
        void unterminatedString() {
            assertThat(Text.fromAnsi("a" + ESC + "]0;title\nb")).isEqualTo(Text.from("a\nb"));
            assertThat(Line.fromAnsi("a" + ESC + "]0;title" + CSI + "1mb"))
                .isEqualTo(Line.from(Span.raw("a"), Span.styled("b", Style.EMPTY.bold())));
        }

        @Test
        @DisplayName("huge or out-of-range values are ignored")
        void hugeValues() {
            assertThat(sgr("99999999999999999999")).isEqualTo(Style.EMPTY);
            assertThat(sgr("38;5;256")).isEqualTo(Style.EMPTY);
            assertThat(sgr("38;5;99999999999999999999")).isEqualTo(Style.EMPTY);
            assertThat(sgr("38;2;256;0;0")).isEqualTo(Style.EMPTY);
            assertThat(sgr("38:2::300:0:0")).isEqualTo(Style.EMPTY);
            assertThat(sgr("31;38;5;999")).isEqualTo(RED);
        }

        @Test
        @DisplayName("a color with missing values or in another color space sets nothing")
        void incompleteColors() {
            assertThat(sgr("38")).isEqualTo(Style.EMPTY);
            assertThat(sgr("38;5")).isEqualTo(Style.EMPTY);
            assertThat(sgr("38;2;1;2")).isEqualTo(Style.EMPTY);
            assertThat(sgr("38:2:1:2")).isEqualTo(Style.EMPTY);
            // CMY: its values are not read as dim, italic and underlined
            assertThat(sgr("38;3;2;3;4")).isEqualTo(Style.EMPTY);
        }

        @Test
        @DisplayName("a sequence with a hundred thousand parameters is read")
        void manyParameters() {
            StringBuilder ansi = new StringBuilder(CSI);
            for (int i = 0; i < 100_000; i++) {
                ansi.append("1;");
            }
            ansi.append("31mx");

            assertThat(Line.fromAnsi(ansi.toString())).isEqualTo(Line.from(Span.styled("x", RED.bold())));
        }

        @Test
        @DisplayName("random input never fails and leaves no escape or control character")
        void randomInput() {
            String[] pieces = {
                ESC, "[", "]", "P", "_", "\\", "(", "0", "1", "3", "8", "5", "2", ";", ":", "m", "H", "?",
                "\u0007", "\t", "\n", "\r", "\u009c", "x", " ", "世", CODER, "\u200D", "\u0301",
            };
            Random random = new Random(42);
            for (int n = 0; n < 20_000; n++) {
                StringBuilder input = new StringBuilder();
                int length = random.nextInt(30);
                for (int i = 0; i < length; i++) {
                    input.append(pieces[random.nextInt(pieces.length)]);
                }
                Text text = Text.fromAnsi(input.toString());
                for (Line line : text.lines()) {
                    for (Span span : line.spans()) {
                        assertThat(span.content()).doesNotContainPattern("[\\x00-\\x1f\\x7f-\\x9f]");
                    }
                }
            }
        }
    }

    @Nested
    @DisplayName("wide characters and grapheme clusters")
    class Clusters {

        @Test
        @DisplayName("wide characters and ZWJ sequences stay whole in their span")
        void keptWhole() {
            Line line = Line.fromAnsi(CSI + "32m" + CODER + " 世界 " + FAMILY + CSI + "0m ok");

            assertThat(line.spans()).containsExactly(
                Span.styled(CODER + " 世界 " + FAMILY, GREEN),
                Span.raw(" ok"));
            assertThat(line.width()).isEqualTo(13);
        }

        @Test
        @DisplayName("a style change inside a ZWJ sequence takes effect after it")
        void styleChangeInsideZwjSequence() {
            // A color per code point, as rainbow tools write them
            Line line = Line.fromAnsi(CSI + "31m👨" + CSI + "32m\u200D" + CSI + "33m👩"
                + CSI + "34m\u200D👧" + CSI + "32mx");

            assertThat(line.spans()).containsExactly(Span.styled(FAMILY, RED), Span.styled("x", GREEN));
            assertThat(line.width()).isEqualTo(3);
        }

        @Test
        @DisplayName("a style change before a combining mark takes effect after it")
        void styleChangeBeforeCombiningMark() {
            Line line = Line.fromAnsi("e" + CSI + "1m\u0301x");

            assertThat(line.spans()).containsExactly(Span.raw("e\u0301"), Span.styled("x", Style.EMPTY.bold()));
        }

        @Test
        @DisplayName("a ZWJ sequence with a style change after its joiner fills one cell")
        void zwjSequenceFillsOneCell() {
            Buffer buffer = Buffer.empty(new Rect(0, 0, 6, 1));

            buffer.setLine(0, 0, Line.fromAnsi(CSI + "31m👩\u200D" + CSI + "34m💻" + CSI + "0mx"));

            assertThat(buffer.get(0, 0).symbol()).isEqualTo(CODER);
            assertThat(buffer.get(0, 0).style()).isEqualTo(RED);
            assertThat(buffer.get(2, 0).symbol()).isEqualTo("x");
        }
    }

    @Nested
    @DisplayName("round trip")
    class RoundTrip {

        @Test
        @DisplayName("a style written by AnsiStringBuilder parses back to the same style")
        void styleToAnsi() {
            List<Style> styles = new ArrayList<>();
            styles.add(Style.EMPTY);
            for (AnsiColor color : AnsiColor.values()) {
                styles.add(Style.EMPTY.fg(Color.ansi(color)));
                styles.add(Style.EMPTY.bg(Color.ansi(color)));
            }
            for (Modifier modifier : Modifier.values()) {
                if (modifier != Modifier.NORMAL) {
                    styles.add(Style.EMPTY.addModifier(modifier));
                }
            }
            for (int index : new int[] {0, 15, 16, 196, 231, 232, 255}) {
                styles.add(Style.EMPTY.fg(Color.indexed(index)));
                styles.add(Style.EMPTY.bg(Color.indexed(index)));
                styles.add(Style.EMPTY.underlined().underlineColor(Color.indexed(index)));
            }
            styles.add(Style.EMPTY.fg(Color.rgb(255, 128, 0)).bg(Color.rgb(0, 0, 0))
                .underlined().underlineColor(Color.rgb(1, 2, 3)));
            styles.add(Style.EMPTY.fg(Color.ansi(AnsiColor.BRIGHT_CYAN)).bg(Color.indexed(236))
                .bold().italic().reversed().crossedOut());

            for (Style style : styles) {
                Line line = Line.fromAnsi(AnsiStringBuilder.styleToAnsi(style) + "x" + AnsiStringBuilder.RESET);
                assertThat(line).as(style.toString()).isEqualTo(Line.from(Span.styled("x", style)));
            }
        }

        @Test
        @DisplayName("a buffer written by AnsiCellWriter parses back into the same cells")
        void bufferToAnsiString() {
            Buffer original = Buffer.empty(new Rect(0, 0, 24, 3));
            original.setLine(0, 0, Line.from(
                Span.styled("12:00:01", Style.EMPTY.fg(Color.ansi(AnsiColor.BRIGHT_BLACK))),
                Span.raw(" "),
                Span.styled(" INFO ", Style.EMPTY.fg(Color.ansi(AnsiColor.BLACK)).bg(Color.ansi(AnsiColor.GREEN)).bold()),
                Span.styled(" started", Style.EMPTY.italic())));
            original.setLine(0, 1, Line.from(
                Span.styled("世界", Style.EMPTY.fg(Color.indexed(208)).underlined().underlineColor(Color.indexed(196))),
                Span.raw(" "),
                Span.styled(CODER + FAMILY, Style.EMPTY.bg(Color.rgb(30, 30, 46))),
                Span.styled(" e\u0301", Style.EMPTY.reversed())));
            original.setLine(2, 2, Line.from(Span.styled("WARN", Style.EMPTY.fg(Color.rgb(255, 165, 0)).dim().crossedOut())));

            Text parsed = Text.fromAnsi(original.toAnsiString());

            assertThat(parsed.height()).isEqualTo(3);
            Buffer copy = Buffer.empty(original.area());
            for (int y = 0; y < parsed.height(); y++) {
                copy.setLine(0, y, parsed.lines().get(y));
            }
            BufferAssertions.assertThat(copy).isEqualTo(original);
        }

        @Test
        @DisplayName("a named color such as Color.RED comes back as the ANSI color it is written as")
        void namedColors() {
            Line line = Line.fromAnsi(AnsiStringBuilder.styleToAnsi(Style.EMPTY.fg(Color.RED).bg(Color.GRAY)) + "x");

            assertThat(line).isEqualTo(Line.from(Span.styled("x", RED.bg(Color.ansi(AnsiColor.WHITE)))));
        }
    }

    @Nested
    @DisplayName("plain text")
    class PlainText {

        @Test
        @DisplayName("text without escape sequences parses as Text.from and Line.from do")
        void sameAsFrom() {
            String[] samples = {"", "plain", "a\nb", "a\n\nb\n", "\n", "世界 " + CODER};
            for (String sample : samples) {
                assertThat(Text.fromAnsi(sample)).as(sample).isEqualTo(Text.from(sample));
            }
            assertThat(Line.fromAnsi("plain")).isEqualTo(Line.from("plain"));
            assertThat(Line.fromAnsi("")).isEqualTo(Line.from(""));
        }

        @Test
        @DisplayName("a line feed at the end does not start another line, even with sequences after it")
        void lineFeedAtTheEnd() {
            assertThat(Text.fromAnsi(CSI + "32mok" + CSI + "0m\n" + CSI + "0m").lines())
                .containsExactly(Line.from(Span.styled("ok", GREEN)));
        }
    }
}
