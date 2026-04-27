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

/**
 * Builds a fenced or indented code block as a {@link WidgetChunk} that
 * delegates to the existing {@code Block} + {@code Paragraph} widgets.
 */
final class CodeBlockBuilder {

    private CodeBlockBuilder() {
    }

    static RenderedChunk build(String literal, String info, int width, MarkdownStyles styles) {
        String trimmed = literal.endsWith("\n") ? literal.substring(0, literal.length() - 1) : literal;
        String[] codeLines = trimmed.isEmpty() ? new String[] {""} : trimmed.split("\n", -1);

        Block.Builder blockBuilder = Block.builder()
            .borders(Borders.ALL)
            .borderType(BorderType.ROUNDED);
        if (info != null && !info.isEmpty()) {
            blockBuilder.title(Title.from(info));
        }
        Block block = blockBuilder.build();

        Style codeStyle = styles.codeBlock();
        List<Line> lines = new ArrayList<>(codeLines.length);
        for (String codeLine : codeLines) {
            lines.add(Line.from(Span.styled(codeLine, codeStyle)));
        }

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
