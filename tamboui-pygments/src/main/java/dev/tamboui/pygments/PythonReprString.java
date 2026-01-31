/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.pygments;

/**
 * Decodes a Python {@code repr()} string literal as emitted by Pygments raw formatter.
 * <p>
 * This is a pragmatic decoder: it supports the common escape sequences used by repr()
 * for code token streams (\n, \t, \r, \\, \', \", hex, unicode, and octal).
 */
final class PythonReprString {

    private PythonReprString() {
    }

    static String decode(String repr) {
        if (repr == null) {
            return "";
        }
        String s = repr.trim();
        if (s.isEmpty()) {
            return "";
        }

        // Strip prefixes like u'', r'', b'' (and combinations).
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == 'u' || c == 'U' || c == 'r' || c == 'R' || c == 'b' || c == 'B' || c == 'f' || c == 'F') {
                i++;
                continue;
            }
            break;
        }
        s = s.substring(i).trim();
        if (s.length() < 2) {
            return "";
        }

        char quote = s.charAt(0);
        if (quote != '\'' && quote != '"') {
            return "";
        }
        if (s.charAt(s.length() - 1) != quote) {
            return "";
        }
        String body = s.substring(1, s.length() - 1);

        StringBuilder out = new StringBuilder(body.length());
        for (int p = 0; p < body.length(); p++) {
            char ch = body.charAt(p);
            if (ch != '\\') {
                out.append(ch);
                continue;
            }
            if (p + 1 >= body.length()) {
                out.append('\\');
                break;
            }
            char e = body.charAt(++p);
            switch (e) {
                case 'n': out.append('\n'); break;
                case 't': out.append('\t'); break;
                case 'r': out.append('\r'); break;
                case '\\': out.append('\\'); break;
                case '\'': out.append('\''); break;
                case '"': out.append('"'); break;
                case 'a': out.append('\u0007'); break;
                case 'b': out.append('\b'); break;
                case 'f': out.append('\f'); break;
                case 'v': out.append('\u000b'); break;
                case 'x': {
                    int cp = readHex(body, p + 1, 2);
                    if (cp >= 0) {
                        out.append((char) cp);
                        p += 2;
                    } else {
                        out.append("\\x");
                    }
                    break;
                }
                case 'u': {
                    int cp = readHex(body, p + 1, 4);
                    if (cp >= 0) {
                        out.append((char) cp);
                        p += 4;
                    } else {
                        out.append("\\u");
                    }
                    break;
                }
                case 'U': {
                    int cp = readHex(body, p + 1, 8);
                    if (cp >= 0) {
                        out.appendCodePoint(cp);
                        p += 8;
                    } else {
                        out.append("\\U");
                    }
                    break;
                }
                default:
                    // Octal escape \ooo
                    if (e >= '0' && e <= '7') {
                        int oct = e - '0';
                        int consumed = 0;
                        while (consumed < 2 && p + 1 < body.length()) {
                            char o = body.charAt(p + 1);
                            if (o < '0' || o > '7') {
                                break;
                            }
                            oct = (oct * 8) + (o - '0');
                            p++;
                            consumed++;
                        }
                        out.append((char) oct);
                    } else {
                        // Unknown escape, keep the escaped char
                        out.append(e);
                    }
                    break;
            }
        }
        return out.toString();
    }

    private static int readHex(String s, int start, int digits) {
        if (start + digits > s.length()) {
            return -1;
        }
        int v = 0;
        for (int i = 0; i < digits; i++) {
            int d = Character.digit(s.charAt(start + i), 16);
            if (d < 0) {
                return -1;
            }
            v = (v << 4) | d;
        }
        return v;
    }
}

