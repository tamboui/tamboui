/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.markdown.internal;

/**
 * Trims trailing partial markdown markers so a streaming source (for example
 * one being typed token-by-token by an LLM) does not flicker as it grows. The
 * sanitizer only touches the trailing fragment of the input, so well-formed
 * markdown is returned unchanged.
 *
 * <p>Rules:
 * <ul>
 *   <li>Trailing run of {@code *}, {@code _}, {@code ~}, or {@code `}
 *       (single character or pair) whose count in the last paragraph is odd:
 *       the unmatched run is dropped.</li>
 *   <li>Dangling link {@code [label](} with no closing {@code )} is rewritten
 *       to plain {@code [label]}.</li>
 *   <li>Trailing partial ATX header line ({@code #}+ optional whitespace
 *       with no content) is dropped.</li>
 *   <li>An odd count of triple-backtick fences is left alone: commonmark
 *       parses an unterminated fence as a code block ending at EOF, which
 *       renders sensibly.</li>
 * </ul>
 */
public final class PartialMarkdownSanitizer {

    private PartialMarkdownSanitizer() {
    }

    /**
     * Returns a copy of {@code source} with trailing partial markers stripped.
     * On well-formed input, the original string is returned unchanged.
     *
     * @param source the markdown text, possibly mid-stream
     * @return the sanitized markdown
     */
    public static String sanitize(String source) {
        if (source == null || source.isEmpty()) {
            return source;
        }
        String result = source;
        result = trimTrailingAtxHeader(result);
        result = trimDanglingLink(result);
        result = trimUnmatchedInlineMarker(result);
        return result;
    }

    private static String trimTrailingAtxHeader(String source) {
        int lastNewline = source.lastIndexOf('\n');
        String last = lastNewline >= 0 ? source.substring(lastNewline + 1) : source;
        if (last.isEmpty()) {
            return source;
        }
        int i = 0;
        int len = last.length();
        while (i < len && last.charAt(i) == '#') {
            i++;
        }
        if (i == 0 || i > 6) {
            return source;
        }
        // Drop only when the rest of the line is whitespace.
        for (int j = i; j < len; j++) {
            char c = last.charAt(j);
            if (c != ' ' && c != '\t') {
                return source;
            }
        }
        return lastNewline >= 0 ? source.substring(0, lastNewline) : "";
    }

    private static String trimDanglingLink(String source) {
        int lastOpenParen = source.lastIndexOf('(');
        if (lastOpenParen < 0) {
            return source;
        }
        // After the last '(' there must be no ')' anywhere.
        if (source.indexOf(')', lastOpenParen) >= 0) {
            return source;
        }
        // The character before '(' must be ']' to look like a link target.
        if (lastOpenParen == 0 || source.charAt(lastOpenParen - 1) != ']') {
            return source;
        }
        // The remainder after '(' must not contain a newline (multi-line link
        // targets are not valid CommonMark inline links).
        for (int i = lastOpenParen + 1; i < source.length(); i++) {
            if (source.charAt(i) == '\n') {
                return source;
            }
        }
        return source.substring(0, lastOpenParen);
    }

    private static String trimUnmatchedInlineMarker(String source) {
        // Walk back from the end through a run of identical marker chars.
        int end = source.length();
        if (end == 0) {
            return source;
        }
        char marker = source.charAt(end - 1);
        if (marker != '*' && marker != '_' && marker != '~' && marker != '`') {
            return source;
        }
        int runStart = end;
        while (runStart > 0 && source.charAt(runStart - 1) == marker) {
            runStart--;
        }
        int runLen = end - runStart;
        // Backtick runs of 3+ are code-fence delimiters, not inline code; let
        // commonmark handle them (an unterminated fence is rendered as a
        // code-block-to-EOF in 0.28).
        if (marker == '`' && runLen >= 3) {
            return source;
        }
        // The run is an "opener" (and therefore unmatched at end of stream)
        // when the character preceding it is start-of-string, whitespace, or
        // an opening punctuation character. A run after non-whitespace is a
        // closer, which we leave alone.
        if (runStart == 0 || isOpenerLeftBoundary(source.charAt(runStart - 1))) {
            return source.substring(0, runStart);
        }
        return source;
    }

    private static boolean isOpenerLeftBoundary(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '('
            || c == '[' || c == '{' || c == '"' || c == '\'';
    }
}
