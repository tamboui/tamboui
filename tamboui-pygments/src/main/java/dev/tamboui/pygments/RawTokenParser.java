/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.pygments;

import dev.tamboui.style.Style;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Parser for Pygments RawTokenFormatter output.
 * <p>
 * Package-private on purpose; public API is {@link Pygments}.
 */
final class RawTokenParser {

    private RawTokenParser() {
    }

    static Text parse(String raw, Pygments.TokenStyleResolver resolver) {
        if (raw == null || raw.isEmpty()) {
            return Text.empty();
        }

        List<Line> lines = new ArrayList<>();
        List<Span> current = new ArrayList<>();
        boolean sawNonEmptyContent = false;

        int idx = 0;
        while (idx < raw.length()) {
            int end = raw.indexOf('\n', idx);
            String line = end >= 0 ? raw.substring(idx, end) : raw.substring(idx);
            idx = end >= 0 ? end + 1 : raw.length();
            if (line.isEmpty()) {
                continue;
            }

            int split = firstWhitespaceIndex(line);
            if (split < 0) {
                continue;
            }
            String tokenType = line.substring(0, split).trim();
            String rest = line.substring(split).trim();
            if (tokenType.isEmpty() || rest.isEmpty()) {
                continue;
            }

            String tokenText = PythonReprString.decode(rest);
            // Defensive normalization: some environments may double-escape control characters,
            // leaving literal "\n" in the decoded text. For whitespace tokens, treat those
            // sequences as the intended control characters.
            if (tokenType.startsWith("Token.Text.Whitespace") || tokenType.equals("Token.Text")) {
                tokenText = normalizeCommonWhitespaceEscapes(tokenText);
            }
            if (tokenText.isEmpty()) {
                continue;
            }

            Style style = resolver.resolve(tokenType);
            if (!tokenText.equals("\n") && tokenText.trim().length() > 0) {
                sawNonEmptyContent = true;
            }
            appendText(tokenText, style, lines, current);
        }

        if (!current.isEmpty()) {
            lines.add(Line.from(current));
        }

        // Pygments commonly emits a final Token.Text.Whitespace '\n' for files ending with a newline.
        // In a "text editor" mental model this should not add an extra blank *visible* line at the end,
        // so we trim exactly one trailing empty line in that case.
        if (sawNonEmptyContent && !lines.isEmpty() && lines.get(lines.size() - 1).isEmpty()) {
            lines.remove(lines.size() - 1);
        }

        return Text.from(lines);
    }

    private static String normalizeCommonWhitespaceEscapes(String s) {
        if (s == null) {
            return "";
        }
        // Only handle the exact whole-token cases; do not replace inside larger strings.
        if ("\\n".equals(s)) {
            return "\n";
        }
        if ("\\r".equals(s)) {
            return "\r";
        }
        if ("\\t".equals(s)) {
            return "\t";
        }
        if ("\\r\\n".equals(s)) {
            return "\r\n";
        }
        return s;
    }

    private static void appendText(String text, Style style, List<Line> lines, List<Span> current) {
        int start = 0;
        while (start < text.length()) {
            int nl = text.indexOf('\n', start);
            if (nl < 0) {
                current.add(Span.styled(text.substring(start), style));
                return;
            }
            String chunk = text.substring(start, nl);
            if (!chunk.isEmpty()) {
                current.add(Span.styled(chunk, style));
            }
            lines.add(Line.from(new ArrayList<>(current)));
            current.clear();
            start = nl + 1;
        }
    }

    private static int firstWhitespaceIndex(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\t' || c == ' ') {
                return i;
            }
        }
        return -1;
    }
}

