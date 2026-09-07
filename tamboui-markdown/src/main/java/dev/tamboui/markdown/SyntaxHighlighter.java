/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.markdown;

import java.util.ArrayList;
import java.util.List;

import dev.tamboui.style.Style;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;

/**
 * Converts a source code snippet into a list of styled {@link Line}s for
 * rendering inside a markdown code block. The {@link MarkdownView} consults a
 * {@code SyntaxHighlighter} whenever it renders a fenced or indented code
 * block; a plain snippet is returned when no grammar matches the requested
 * language or when the {@link MarkdownView.Builder#syntaxHighlighter} is set
 * to {@link #none()}.
 *
 * <p>Implementations are expected to be stateless and thread-safe. The built-in
 * {@link RegexSyntaxHighlighter} covers a set of popular languages using
 * highlight.js-style regex grammars and is a good template for custom
 * highlighters.
 */
@FunctionalInterface
public interface SyntaxHighlighter {

    /**
     * Highlights {@code code}, which must contain only the code lines, into
     * one {@link Line} per source line. The returned lines may carry multiple
     * spans with distinct styles; texat not matched by a grammar keeps the
     * given {@code base} style.
     *
     * @param code the raw snippet, possibly multi-line
     * @param language the fenced-block info string (e.g. {@code java}), or the
     *        empty string when unknown; may be {@code null}
     * @param base the style applied to non-tokenized text (generally the
     *        code-block style)
     * @param theme the palette mapping token types to styles
     * @return one styled {@link Line} per source line; never null
     */
    List<Line> highlight(String code, String language, Style base, SyntaxTheme theme);

    /**
     * Returns a highlighter that never tokenizes: every source line becomes a
     * single span in the given {@code base} style. Use this to disable syntax
     * highlighting entirely.
     *
     * @return a no-op highlighter
     */
    static SyntaxHighlighter none() {
        return (code, language, base, theme) -> {
            String trimmed = code.endsWith("\n") ? code.substring(0, code.length() - 1) : code;
            String[] lines = trimmed.isEmpty() ? new String[] {""} : trimmed.split("\n", -1);
            List<Line> out = new ArrayList<>(lines.length);
            for (String line : lines) {
                out.add(Line.from(Span.styled(line, base)));
            }
            return out;
        };
    }
}