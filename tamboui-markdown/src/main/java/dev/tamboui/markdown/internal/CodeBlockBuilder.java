/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.markdown.internal;

import java.util.ArrayList;
import java.util.List;

import dev.tamboui.markdown.MarkdownStyles;
import dev.tamboui.style.Overflow;
import dev.tamboui.style.Style;
import dev.tamboui.text.CharWidth;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.widgets.block.Title;
import dev.tamboui.widgets.paragraph.Paragraph;
import dev.tamboui.widgets.syntax.SyntaxHighlighter;
import dev.tamboui.widgets.syntax.SyntaxTheme;

/**
 * Builds a fenced or indented code block as a {@link WidgetChunk} that
 * delegates to the existing {@code Block} + {@code Paragraph} widgets.
 */
final class CodeBlockBuilder {

    private CodeBlockBuilder() {
    }

    static RenderedChunk build(String literal, String info, int width, MarkdownStyles styles,
                               SyntaxHighlighter highlighter, SyntaxTheme theme) {
        String trimmed = literal.endsWith("\n") ? literal.substring(0, literal.length() - 1) : literal;

        Block.Builder blockBuilder = Block.builder()
            .borders(Borders.ALL)
            .borderType(BorderType.ROUNDED);
        if (info != null && !info.isEmpty()) {
            blockBuilder.title(Title.from(info));
        }
        Block block = blockBuilder.build();

        Style codeStyle = styles.codeBlock();
        List<Line> lines = highlighter.highlight(trimmed, languageFromInfo(info), codeStyle, theme);

        int innerWidth = Math.max(1, width - 2);
        List<Line> wrapped = clip(lines, innerWidth);

        Text text = Text.from(wrapped);
        Paragraph paragraph = Paragraph.builder()
            .text(text)
            .block(block)
            .style(codeStyle)
            .overflow(Overflow.CLIP)
            .build();
        return new WidgetChunk(paragraph, wrapped.size() + 2);
    }

    /**
     * Builds the code block as plain lines, its rounded border drawn with box characters, for places that can only
     * hold lines, such as the content of a list item.
     */
    static List<Line> buildLines(String literal, String info, int width, MarkdownStyles styles,
                                 SyntaxHighlighter highlighter, SyntaxTheme theme) {
        String trimmed = literal.endsWith("\n") ? literal.substring(0, literal.length() - 1) : literal;
        Style codeStyle = styles.codeBlock();
        int innerWidth = Math.max(1, width - 2);
        List<Line> wrapped = clip(highlighter.highlight(trimmed, languageFromInfo(info), codeStyle, theme), innerWidth);

        List<Line> out = new ArrayList<>(wrapped.size() + 2);
        String title = info != null && !info.isEmpty() ? CharWidth.substringByWidth(info, innerWidth) : "";
        out.add(Line.from(Span.styled("\u256d" + title + MarkdownText.repeat('\u2500', innerWidth - CharWidth.of(title)) + "\u256e",
            codeStyle)));
        for (Line line : wrapped) {
            List<Span> spans = new ArrayList<>(line.spans().size() + 3);
            spans.add(Span.styled("\u2502", codeStyle));
            spans.addAll(line.spans());
            spans.add(Span.styled(MarkdownText.repeat(' ', innerWidth - line.width()), codeStyle));
            spans.add(Span.styled("\u2502", codeStyle));
            out.add(Line.from(spans));
        }
        out.add(Line.from(Span.styled("\u2570" + MarkdownText.repeat('\u2500', innerWidth) + "\u256f", codeStyle)));
        return out;
    }

    private static String languageFromInfo(String info) {
        if (info == null) {
            return null;
        }
        String trimmed = info.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        for (int i = 0; i < trimmed.length(); i++) {
            if (Character.isWhitespace(trimmed.charAt(i))) {
                return trimmed.substring(0, i);
            }
        }
        return trimmed;
    }

    private static List<Line> clip(List<Line> lines, int width) {
        List<Line> result = new ArrayList<>(lines.size());
        for (Line line : lines) {
            if (line.width() <= width) {
                result.add(line);
                continue;
            }
            List<Span> clipped = new ArrayList<>();
            int remaining = width;
            for (Span span : line.spans()) {
                int w = span.width();
                if (w <= remaining) {
                    clipped.add(span);
                    remaining -= w;
                } else {
                    String head = CharWidth.substringByWidth(span.content(), remaining);
                    if (!head.isEmpty()) {
                        clipped.add(Span.styled(head, span.style()));
                    }
                    break;
                }
            }
            result.add(Line.from(clipped));
        }
        return result;
    }
}
