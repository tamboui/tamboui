//DEPS dev.tamboui:tamboui-toolkit:LATEST
//DEPS dev.tamboui:tamboui-jline3-backend:LATEST
/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.inline.InlineDisplay;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Layout;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Overflow;
import dev.tamboui.style.Style;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.paragraph.Paragraph;

/**
 * Demonstrates correct rendering of wide Unicode characters (emoji, CJK, fullwidth).
 * <p>
 * Verifies:
 * <ul>
 *   <li>Emoji rendering with correct 2-column width</li>
 *   <li>CJK (Chinese, Japanese, Korean) text</li>
 *   <li>Mixed ASCII + wide character content</li>
 *   <li>Clipping and ellipsis with wide characters</li>
 *   <li>Word/character wrapping with wide characters</li>
 * </ul>
 */
public class UnicodeDemo {
    private UnicodeDemo() {
    }

    /**
     * Demo entry point.
     * @param args the CLI arguments
     * @throws Exception on unexpected error
     */
    public static void main(String[] args) throws Exception {
        try (InlineDisplay display = InlineDisplay.create(20)) {
            display.render(UnicodeDemo::renderDemo);
            Thread.sleep(5000);
        }
    }

    private static void renderDemo(Rect area, Buffer buffer) {
        java.util.List<Rect> rows = Layout.vertical().constraints(
                Constraint.length(5),
                Constraint.length(4),
                Constraint.length(4),
                Constraint.length(4),
                Constraint.fill()
        ).split(area);

        renderEmojiSection(rows.get(0), buffer);
        renderCjkSection(rows.get(1), buffer);
        renderMixedSection(rows.get(2), buffer);
        renderWrapSection(rows.get(3), buffer);
        renderEllipsisSection(rows.get(4), buffer);
    }

    private static void renderEmojiSection(Rect area, Buffer buffer) {
        Paragraph p = Paragraph.builder()
                .text(Text.from(
                        Line.from(Span.styled("Emoji Rendering", Style.EMPTY.bold())),
                        Line.from(Span.raw("\uD83D\uDD25 Fire  \uD83C\uDF89 Party  \uD83D\uDE80 Rocket  \uD83C\uDFB5 Music")),
                        Line.from(Span.raw("\u2764  Heart  \u2600  Sun  \u2B50 Star  \u26A1 Zap")),
                        Line.from(Span.raw("Status: \uD83D\uDFE2 OK  \uD83D\uDFE1 Warn  \uD83D\uDD34 Error"))
                ))
                .block(Block.builder().borders(Borders.ALL).borderType(BorderType.ROUNDED).title("Emoji").build())
                .build();
        p.render(area, buffer);
    }

    private static void renderCjkSection(Rect area, Buffer buffer) {
        Paragraph p = Paragraph.builder()
                .text(Text.from(
                        Line.from(
                                Span.styled("CN:  ", Style.EMPTY.fg(Color.CYAN)),
                                Span.raw("\u4E16\u754C\u4F60\u597D")  // 世界你好
                        ),
                        Line.from(
                                Span.styled("JP:  ", Style.EMPTY.fg(Color.MAGENTA)),
                                Span.raw("\u3053\u3093\u306B\u3061\u306F")  // こんにちは
                        ),
                        Line.from(
                                Span.styled("KR:  ", Style.EMPTY.fg(Color.YELLOW)),
                                Span.raw("\uC548\uB155\uD558\uC138\uC694")  // 안녕하세요
                        )
                ))
                .block(Block.builder().borders(Borders.ALL).borderType(BorderType.ROUNDED).title("CJK").build())
                .build();
        p.render(area, buffer);
    }

    private static void renderMixedSection(Rect area, Buffer buffer) {
        Paragraph p = Paragraph.builder()
                .text(Text.from(
                        Line.from(Span.raw("Build: \uD83D\uDD25 compiling \u4E16\u754C.java")),
                        Line.from(Span.raw("Test: \u2705 \u30C6\u30B9\u30C8 passed (12/12)"))  // テスト
                ))
                .block(Block.builder().borders(Borders.ALL).borderType(BorderType.ROUNDED).title("Mixed").build())
                .build();
        p.render(area, buffer);
    }

    private static void renderWrapSection(Rect area, Buffer buffer) {
        // Split into two columns
        java.util.List<Rect> cols = Layout.horizontal().constraints(
                Constraint.percentage(50),
                Constraint.percentage(50)
        ).split(area);

        Paragraph charWrap = Paragraph.builder()
                .text(Text.from(Line.from(Span.raw("\u4E16\u754C\u4F60\u597D\uD83D\uDD25\uD83C\uDF89"))))
                .overflow(Overflow.WRAP_CHARACTER)
                .block(Block.builder().borders(Borders.ALL).borderType(BorderType.ROUNDED).title("CharWrap").build())
                .build();
        charWrap.render(cols.get(0), buffer);

        Paragraph wordWrap = Paragraph.builder()
                .text(Text.from(Line.from(Span.raw("Hello \u4E16\u754C and \uD83D\uDD25 fire"))))
                .overflow(Overflow.WRAP_WORD)
                .block(Block.builder().borders(Borders.ALL).borderType(BorderType.ROUNDED).title("WordWrap").build())
                .build();
        wordWrap.render(cols.get(1), buffer);
    }

    private static void renderEllipsisSection(Rect area, Buffer buffer) {
        java.util.List<Rect> cols = Layout.horizontal().constraints(
                Constraint.percentage(33),
                Constraint.percentage(34),
                Constraint.percentage(33)
        ).split(area);

        Paragraph ellipsisEnd = Paragraph.builder()
                .text(Text.from(Line.from(Span.raw("\u4E16\u754C\u4F60\u597D\u5566"))))
                .overflow(Overflow.ELLIPSIS)
                .block(Block.builder().borders(Borders.ALL).borderType(BorderType.ROUNDED).title("End").build())
                .build();
        ellipsisEnd.render(cols.get(0), buffer);

        Paragraph ellipsisStart = Paragraph.builder()
                .text(Text.from(Line.from(Span.raw("\u4E16\u754C\u4F60\u597D\u5566"))))
                .overflow(Overflow.ELLIPSIS_START)
                .block(Block.builder().borders(Borders.ALL).borderType(BorderType.ROUNDED).title("Start").build())
                .build();
        ellipsisStart.render(cols.get(1), buffer);

        Paragraph ellipsisMid = Paragraph.builder()
                .text(Text.from(Line.from(Span.raw("\u4E16\u754C\u4F60\u597D\u5566"))))
                .overflow(Overflow.ELLIPSIS_MIDDLE)
                .block(Block.builder().borders(Borders.ALL).borderType(BorderType.ROUNDED).title("Mid").build())
                .build();
        ellipsisMid.render(cols.get(2), buffer);
    }
}
