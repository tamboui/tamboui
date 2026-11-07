/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.text;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import dev.tamboui.style.AnsiColor;
import dev.tamboui.style.Color;
import dev.tamboui.style.Modifier;
import dev.tamboui.style.Style;

/**
 * Parses text with ANSI escape sequences into styled lines, for {@link Text#fromAnsi(String)} and
 * {@link Line#fromAnsi(String)}.
 * <p>
 * Sequences are recognized as a terminal recognizes them (ECMA-48), with two differences that keep
 * the text intact: a sequence broken off by a character that cannot be part of it ends there, and
 * that character is read as usual; and a string (OSC, DCS, SOS, PM, APC) without a terminator ends
 * at the end of its line. Style changes are resolved to grapheme cluster boundaries, so that a
 * change in the middle of a cluster takes effect after it and the cluster stays in one span.
 */
final class AnsiParser {

    private static final char ESC = '\u001b';
    private static final char BEL = '\u0007';
    private static final char ST_8BIT = '\u009c';
    private static final int TAB_WIDTH = 8;
    private static final String TAB_SPACES = "        ";
    // Parameter values stop growing here; every SGR code and color value is far below it
    private static final int MAX_PARAMETER = 100_000;
    private static final int MAX_SUB_PARAMETERS = 8;
    private static final EnumSet<Modifier> NO_MODIFIERS = EnumSet.noneOf(Modifier.class);
    private static final Color[] ANSI_COLORS = new Color[16];

    static {
        for (AnsiColor color : AnsiColor.values()) {
            ANSI_COLORS[color.code()] = Color.ansi(color);
        }
    }

    private final String input;
    private int pos;

    // SGR state, which carries on from one line to the next
    private Color fg;
    private Color bg;
    private Color underlineColor;
    private final EnumSet<Modifier> modifiers = EnumSet.noneOf(Modifier.class);
    private Style style = Style.EMPTY;

    // The line being read: its text, where its style changes, and the column after its last tab
    private final StringBuilder plain = new StringBuilder();
    private final List<StyleChange> changes = new ArrayList<>();
    private int tabEnd;
    private int tabEndColumn;

    // The SGR parameters being read: the current parameter and its sub-parameters (38:2::r:g:b)
    private int parameterPos;
    private int parameterEnd;
    private final int[] parameter = new int[MAX_SUB_PARAMETERS];
    private int parameterSize;

    private AnsiParser(String input) {
        this.input = input;
    }

    /**
     * Parses text into lines, one per line feed.
     *
     * @param input the text with ANSI escape sequences
     * @return the styled text
     */
    static Text parseText(String input) {
        return new AnsiParser(input).text();
    }

    /**
     * Parses text into a single line, dropping line feeds.
     *
     * @param input the text with ANSI escape sequences
     * @return the styled line
     */
    static Line parseLine(String input) {
        AnsiParser parser = new AnsiParser(input);
        parser.read(null);
        return parser.endLine();
    }

    private Text text() {
        List<Line> lines = new ArrayList<>();
        read(lines);
        // Like Text.from(String), a line feed at the end does not start another line
        if (plain.length() > 0) {
            lines.add(endLine());
        }
        return Text.from(lines);
    }

    // Reads the whole input; a line feed ends a line when lines is given and is dropped otherwise
    private void read(List<Line> lines) {
        int length = input.length();
        int textStart = pos;
        while (pos < length) {
            char c = input.charAt(pos);
            if (!isControl(c)) {
                pos++;
                continue;
            }
            appendText(textStart, pos);
            if (c == ESC) {
                escape();
            } else if (c == '\t') {
                appendTab();
                pos++;
            } else if (c == '\n' && lines != null) {
                lines.add(endLine());
                pos++;
            } else {
                // CR and the other control characters take no cell
                pos++;
            }
            textStart = pos;
        }
        appendText(textStart, pos);
    }

    private static boolean isControl(char c) {
        return c < 0x20 || (c >= 0x7f && c <= 0x9f);
    }

    private void escape() {
        int length = input.length();
        pos++;
        if (pos >= length) {
            return;
        }
        char c = input.charAt(pos);
        if (c == '[') {
            pos++;
            controlSequence();
        } else if (c == ']' || c == 'P' || c == 'X' || c == '^' || c == '_') {
            // OSC, DCS, SOS, PM and APC carry a string up to a terminator
            pos++;
            skipString();
        } else if (c >= 0x20 && c <= 0x2f) {
            // Intermediate bytes, then a final byte, e.g. ESC ( B, which selects the ASCII character set
            while (pos < length && input.charAt(pos) >= 0x20 && input.charAt(pos) <= 0x2f) {
                pos++;
            }
            if (pos < length && input.charAt(pos) >= 0x30 && input.charAt(pos) <= 0x7e) {
                pos++;
            }
        } else if (c >= 0x30 && c <= 0x7e) {
            // A two-character sequence, e.g. ESC 7 (save cursor) or ESC M (reverse index)
            pos++;
        }
        // Otherwise the ESC stands alone, and the character after it is read as usual
    }

    private void controlSequence() {
        int length = input.length();
        int start = pos;
        // Parameter bytes (0x30-0x3f) and intermediate bytes (0x20-0x2f), then a final byte (0x40-0x7e)
        while (pos < length && input.charAt(pos) >= 0x20 && input.charAt(pos) <= 0x3f) {
            pos++;
        }
        if (pos >= length || input.charAt(pos) < 0x40 || input.charAt(pos) > 0x7e) {
            return;
        }
        pos++;
        if (input.charAt(pos - 1) == 'm' && isSgr(start, pos - 1)) {
            applySgr(start, pos - 1);
        }
    }

    // SGR parameters are digits separated by ';', and ':' between sub-parameters; anything
    // else, such as the '>' of CSI > 4 ; 2 m (an xterm key mode), makes it another sequence
    private boolean isSgr(int start, int end) {
        for (int i = start; i < end; i++) {
            char c = input.charAt(i);
            if ((c < '0' || c > '9') && c != ';' && c != ':') {
                return false;
            }
        }
        return true;
    }

    private void skipString() {
        int length = input.length();
        while (pos < length) {
            char c = input.charAt(pos);
            if (c == BEL || c == ST_8BIT) {
                pos++;
                return;
            }
            if (c == ESC) {
                // ESC \ is the string terminator; any other ESC starts the next sequence
                if (pos + 1 < length && input.charAt(pos + 1) == '\\') {
                    pos += 2;
                }
                return;
            }
            if (c == '\n') {
                return;
            }
            pos++;
        }
    }

    private void applySgr(int start, int end) {
        parameterPos = start;
        parameterEnd = end;
        while (nextParameter()) {
            int code = parameter[0];
            switch (code) {
                case 0:
                    fg = null;
                    bg = null;
                    underlineColor = null;
                    modifiers.clear();
                    break;
                case 1:
                    modifiers.add(Modifier.BOLD);
                    break;
                case 2:
                    modifiers.add(Modifier.DIM);
                    break;
                case 3:
                    modifiers.add(Modifier.ITALIC);
                    break;
                case 4:
                    // 4:0 is no underline; 4:1 to 4:5 are underline styles, shown as one
                    if (parameterSize > 1 && parameter[1] == 0) {
                        modifiers.remove(Modifier.UNDERLINED);
                    } else {
                        modifiers.add(Modifier.UNDERLINED);
                    }
                    break;
                case 5:
                    modifiers.add(Modifier.SLOW_BLINK);
                    break;
                case 6:
                    modifiers.add(Modifier.RAPID_BLINK);
                    break;
                case 7:
                    modifiers.add(Modifier.REVERSED);
                    break;
                case 8:
                    modifiers.add(Modifier.HIDDEN);
                    break;
                case 9:
                    modifiers.add(Modifier.CROSSED_OUT);
                    break;
                case 21:
                    // Doubly underlined
                    modifiers.add(Modifier.UNDERLINED);
                    break;
                case 22:
                    modifiers.remove(Modifier.BOLD);
                    modifiers.remove(Modifier.DIM);
                    break;
                case 23:
                    modifiers.remove(Modifier.ITALIC);
                    break;
                case 24:
                    modifiers.remove(Modifier.UNDERLINED);
                    break;
                case 25:
                    modifiers.remove(Modifier.SLOW_BLINK);
                    modifiers.remove(Modifier.RAPID_BLINK);
                    break;
                case 27:
                    modifiers.remove(Modifier.REVERSED);
                    break;
                case 28:
                    modifiers.remove(Modifier.HIDDEN);
                    break;
                case 29:
                    modifiers.remove(Modifier.CROSSED_OUT);
                    break;
                case 38:
                    fg = extendedColor(fg);
                    break;
                case 39:
                    fg = null;
                    break;
                case 48:
                    bg = extendedColor(bg);
                    break;
                case 49:
                    bg = null;
                    break;
                case 58:
                    underlineColor = extendedColor(underlineColor);
                    break;
                case 59:
                    underlineColor = null;
                    break;
                default:
                    if (code >= 30 && code <= 37) {
                        fg = ANSI_COLORS[code - 30];
                    } else if (code >= 40 && code <= 47) {
                        bg = ANSI_COLORS[code - 40];
                    } else if (code >= 90 && code <= 97) {
                        fg = ANSI_COLORS[code - 90 + 8];
                    } else if (code >= 100 && code <= 107) {
                        bg = ANSI_COLORS[code - 100 + 8];
                    }
                    // Other codes (fonts, frames, overline, ...) have no TamboUI style
                    break;
            }
        }
        if (fg == null && bg == null && underlineColor == null && modifiers.isEmpty()) {
            style = Style.EMPTY;
        } else {
            style = new Style(fg, bg, underlineColor, modifiers, NO_MODIFIERS);
        }
    }

    // Reads the color of 38, 48 or 58, or returns the current one if it is missing or out of range
    private Color extendedColor(Color current) {
        Color color = null;
        if (parameterSize > 1) {
            // 38:5:n, or 38:2:r:g:b with or without a color space id before r (38:2::r:g:b)
            if (parameter[1] == 5 && parameterSize > 2) {
                color = indexed(parameter[2]);
            } else if (parameter[1] == 2 && parameterSize > 5) {
                color = rgb(parameter[3], parameter[4], parameter[5]);
            } else if (parameter[1] == 2 && parameterSize == 5) {
                color = rgb(parameter[2], parameter[3], parameter[4]);
            }
        } else if (nextParameter()) {
            // 38;5;n or 38;2;r;g;b: the values are the next parameters
            if (parameter[0] == 5) {
                color = nextParameter() ? indexed(parameter[0]) : null;
            } else if (parameter[0] == 2) {
                int r = nextParameter() ? parameter[0] : -1;
                int g = nextParameter() ? parameter[0] : -1;
                int b = nextParameter() ? parameter[0] : -1;
                color = rgb(r, g, b);
            } else {
                // Another color space: where its values end is unknown, so the rest is not read
                parameterPos = parameterEnd + 1;
            }
        }
        return color != null ? color : current;
    }

    private static Color indexed(int index) {
        return index <= 255 ? Color.indexed(index) : null;
    }

    private static Color rgb(int r, int g, int b) {
        if (r < 0 || r > 255 || g < 0 || g > 255 || b < 0 || b > 255) {
            return null;
        }
        return Color.rgb(r, g, b);
    }

    // Reads the next parameter and its sub-parameters; an empty value is 0
    private boolean nextParameter() {
        if (parameterPos > parameterEnd) {
            return false;
        }
        parameterSize = 0;
        int value = 0;
        while (true) {
            char c = parameterPos < parameterEnd ? input.charAt(parameterPos) : ';';
            parameterPos++;
            if (c == ';' || c == ':') {
                if (parameterSize < parameter.length) {
                    parameter[parameterSize++] = value;
                }
                if (c == ';') {
                    return true;
                }
                value = 0;
            } else if (value < MAX_PARAMETER) {
                value = value * 10 + (c - '0');
            }
        }
    }

    private void appendText(int start, int end) {
        if (start < end) {
            changeStyle(style);
            plain.append(input, start, end);
        }
    }

    // Expands a tab to the next multiple of 8 columns. The spaces take no style, as a terminal
    // moves the cursor over these cells without painting them.
    private void appendTab() {
        int column = tabEndColumn + CharWidth.of(plain.substring(tabEnd));
        int spaces = TAB_WIDTH - column % TAB_WIDTH;
        changeStyle(Style.EMPTY);
        plain.append(TAB_SPACES, 0, spaces);
        tabEnd = plain.length();
        tabEndColumn = column + spaces;
    }

    private void changeStyle(Style newStyle) {
        if (changes.isEmpty() || !changes.get(changes.size() - 1).style.equals(newStyle)) {
            changes.add(new StyleChange(plain.length(), newStyle));
        }
    }

    private Line endLine() {
        Line line;
        if (plain.length() == 0) {
            line = Line.from("");
        } else if (changes.size() == 1) {
            line = Line.from(new Span(plain.toString(), changes.get(0).style));
        } else {
            line = Line.from(spans());
        }
        plain.setLength(0);
        changes.clear();
        tabEnd = 0;
        tabEndColumn = 0;
        return line;
    }

    private List<Span> spans() {
        List<Span> spans = new ArrayList<>();
        int spanStart = 0;
        Style spanStyle = changes.get(0).style;
        int boundary = 0;
        int i = 1;
        while (i < changes.size()) {
            // A change inside a grapheme cluster takes effect after it, so that the cluster stays whole
            while (boundary < changes.get(i).offset) {
                boundary = GraphemeClusters.clusterEnd(plain, boundary);
            }
            // Of the changes that take effect at this boundary, the last one counts
            Style changed = changes.get(i).style;
            i++;
            while (i < changes.size() && changes.get(i).offset <= boundary) {
                changed = changes.get(i).style;
                i++;
            }
            if (boundary == plain.length()) {
                break;
            }
            if (!changed.equals(spanStyle)) {
                spans.add(new Span(plain.substring(spanStart, boundary), spanStyle));
                spanStart = boundary;
                spanStyle = changed;
            }
        }
        spans.add(new Span(plain.substring(spanStart), spanStyle));
        return spans;
    }

    private static final class StyleChange {
        private final int offset;
        private final Style style;

        StyleChange(int offset, Style style) {
            this.offset = offset;
            this.style = style;
        }
    }
}
