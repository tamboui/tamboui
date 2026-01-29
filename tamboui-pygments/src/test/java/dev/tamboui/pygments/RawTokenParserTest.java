/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.pygments;

import dev.tamboui.style.Tags;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RawTokenParserTest {

    @Test
    void parsesRawFormatterIntoStyledText() {
        String raw = ""
            + "Token.Keyword\t'class'\n"
            + "Token.Text\t' '\n"
            + "Token.Name.Class\t'A'\n"
            + "Token.Punctuation\t'{' \n"
            + "Token.Comment\t'// hello'\n"
            + "Token.Text.Whitespace\t'\\n'\n"
            + "Token.Literal.String\t'\"x\"'\n"
            + "Token.Text.Whitespace\t'\\n'\n"
            + "Token.Literal.Number\t'42'\n";

        Text text = RawTokenParser.parse(raw, Pygments.DEFAULT_STYLE_RESOLVER);
        assertThat(text.rawContent()).contains("class A");

        Set<String> tags = collectTags(text);
        assertThat(tags).contains("syntax-keyword", "syntax-comment", "syntax-string", "syntax-number", "syntax-class");
    }

    @Test
    void doesNotAddExtraTrailingBlankLineForFinalNewline() {
        String raw = ""
            + "Token.Name\t'a'\n"
            + "Token.Text.Whitespace\t'\\n'\n";

        Text text = RawTokenParser.parse(raw, Pygments.DEFAULT_STYLE_RESOLVER);
        assertThat(text.lines()).hasSize(1);
        assertThat(text.lines().get(0).rawContent()).isEqualTo("a");
    }

    @Test
    void whitespaceTokenWithLiteralBackslashN_isTreatedAsNewline() {
        // If a raw stream ever contains a double-escaped newline, we still want it to break lines.
        String raw = ""
            + "Token.Name\t'a'\n"
            + "Token.Text.Whitespace\t'\\\\n'\n"
            + "Token.Name\t'b'\n";

        Text text = RawTokenParser.parse(raw, Pygments.DEFAULT_STYLE_RESOLVER);
        assertThat(text.lines()).hasSize(2);
        assertThat(text.lines().get(0).rawContent()).isEqualTo("a");
        assertThat(text.lines().get(1).rawContent()).isEqualTo("b");
    }

    private static Set<String> collectTags(Text text) {
        Set<String> out = new HashSet<>();
        for (Line line : text.lines()) {
            for (Span span : line.spans()) {
                Tags tags = span.style().extension(Tags.class, Tags.empty());
                out.addAll(tags.values());
            }
        }
        return out;
    }
}

