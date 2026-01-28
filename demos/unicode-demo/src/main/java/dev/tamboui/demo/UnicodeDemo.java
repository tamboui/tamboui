///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS dev.tamboui:tamboui-widgets:LATEST
//DEPS dev.tamboui:tamboui-jline3-backend:LATEST

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Layout;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Backend;
import dev.tamboui.terminal.BackendFactory;
import dev.tamboui.terminal.Frame;
import dev.tamboui.terminal.Terminal;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.widgets.paragraph.Paragraph;

import java.io.IOException;
import java.util.List;

/**
 * Demonstrates correct rendering of wide Unicode characters (emoji, CJK, Arabic).
 * <p>
 * Showcases:
 * <ul>
 *   <li>Emoji rendering with correct 2-column width</li>
 *   <li>CJK (Chinese, Japanese, Korean) text</li>
 *   <li>Arabic script</li>
 *   <li>Mixed ASCII + wide character content</li>
 * </ul>
 */
public class UnicodeDemo {

    private boolean running = true;

    private UnicodeDemo() {

    }

    /**
     * Demo entry point.
     * @param args the CLI arguments
     * @throws Exception on unexpected error
     */
    public static void main(String[] args) throws Exception {
        new UnicodeDemo().run();
    }

    /**
     * Runs the demo application.
     *
     * @throws Exception if an error occurs
     */
     public void run() throws Exception {
        try (Backend backend = BackendFactory.create()) {
            backend.enableRawMode();
            backend.enterAlternateScreen();
            backend.hideCursor();

            Terminal<Backend> terminal = new Terminal<>(backend);

            backend.onResize(() -> {
                try {
                    terminal.draw(this::render);
                } catch (IOException e) {
                    // Ignore
                }
            });

            while (running) {
                terminal.draw(this::render);

                int c = backend.read(100);
                if (c == 'q' || c == 'Q' || c == 3) {
                    running = false;
                }
            }
        }
    }

    private void render(Frame frame) {
        Rect area = frame.area();

        List<Rect> rows = Layout.vertical().constraints(
                Constraint.length(7),  // Emoji
                Constraint.length(6),  // CJK
                Constraint.length(6),  // Arabic
                Constraint.length(5),  // Mixed
                Constraint.length(1),  // Footer
                Constraint.fill()
        ).split(area);

        renderEmojiSection(frame, rows.get(0));
        renderCjkSection(frame, rows.get(1));
        renderArabicSection(frame, rows.get(2));
        renderMixedSection(frame, rows.get(3));
        renderFooter(frame, rows.get(4));
    }

    private void renderEmojiSection(Frame frame, Rect area) {
        Paragraph p = Paragraph.builder()
                .text(Text.from(
                        Line.from(Span.raw("\uD83D\uDD25 Fire  \uD83C\uDF89 Party  \uD83D\uDE80 Rocket  \uD83C\uDFB5 Music")),
                        Line.from(Span.raw("\u2764  Heart  \u2600  Sun  \u2B50 Star  \u26A1 Zap")),
                        Line.from(Span.raw("\uD83D\uDFE2  OK  \uD83D\uDFE1 Warn  \uD83D\uDD34 Error  \uD83D\uDFE3 Info")),
                        Line.from(Span.raw("\uD83C\uDDEB\uD83C\uDDF7 FR  \uD83C\uDDE9\uD83C\uDDEA DE  \uD83C\uDDEF\uD83C\uDDF5 JP  \uD83C\uDDF0\uD83C\uDDF7 KR")),
                        Line.from(Span.raw("\uD83D\uDC4B\uD83C\uDFFB Hi  \uD83D\uDC4D\uD83C\uDFFC OK  \u270C\uFE0F Peace"))
                ))
                .block(Block.builder().borders(Borders.ALL).borderType(BorderType.ROUNDED).title("Emoji").build())
                .build();
        frame.renderWidget(p, area);
    }

    private void renderCjkSection(Frame frame, Rect area) {
        Paragraph p = Paragraph.builder()
                .text(Text.from(
                        Line.from(
                                Span.styled("CN:  ", Style.EMPTY.fg(Color.CYAN)),
                                Span.raw("\u4E16\u754C\u4F60\u597D")  // 世界你好
                        ),
                        Line.from(
                                Span.styled("JP:  ", Style.EMPTY.fg(Color.MAGENTA)),
                                Span.raw("\u3053\u3093\u306B\u3061\u306F\u4E16\u754C")  // こんにちは世界
                        ),
                        Line.from(
                                Span.styled("KR:  ", Style.EMPTY.fg(Color.YELLOW)),
                                Span.raw("\uC548\uB155\uD558\uC138\uC694")  // 안녕하세요
                        ),
                        Line.from(
                                Span.styled("Mix: ", Style.EMPTY.fg(Color.RED)),
                                Span.raw("\u6F22\u5B57\u304B\u306A\uD55C\uAE00")  // 漢字かな한글
                        )
                ))
                .block(Block.builder().borders(Borders.ALL).borderType(BorderType.ROUNDED).title("CJK").build())
                .build();
        frame.renderWidget(p, area);
    }

    private void renderArabicSection(Frame frame, Rect area) {
        Paragraph p = Paragraph.builder()
                .text(Text.from(
                        Line.from(
                                Span.raw("\u0645\u0631\u062D\u0628\u0627\u064B")  // مرحباً
                        ),
                        Line.from(
                                Span.raw("\u0627\u0644\u0639\u0627\u0644\u0645")  // العالم
                        ),
                        Line.from(
                                Span.raw("\u0627\u0644\u0633\u0644\u0627\u0645 \u0639\u0644\u064A\u0643\u0645")  // السلام عليكم
                        ),
                        Line.from(
                                Span.raw("\u0634\u0643\u0631\u0627\u064B \u0644\u0643")  // شكراً لك
                        )
                ))
                .block(Block.builder().borders(Borders.ALL).borderType(BorderType.ROUNDED).title("Arabic").build())
                .build();
        frame.renderWidget(p, area);
    }

    private void renderMixedSection(Frame frame, Rect area) {
        Paragraph p = Paragraph.builder()
                .text(Text.from(
                        Line.from(Span.raw("Build: \uD83D\uDD25 compiling \u4E16\u754C.java")),
                        Line.from(Span.raw("Test: \u2705 \u30C6\u30B9\u30C8 passed \u0646\u062C\u0627\u062D")),  // テスト + نجاح
                        Line.from(Span.raw("Deploy: \uD83D\uDE80 \u90E8\u7F72\u5B8C\u4E86 \u062A\u0645"))  // 部署完了 + تم
                ))
                .block(Block.builder().borders(Borders.ALL).borderType(BorderType.ROUNDED).title("Mixed").build())
                .build();
        frame.renderWidget(p, area);
    }

    private void renderFooter(Frame frame, Rect area) {
        Paragraph footer = Paragraph.builder()
                .text(Text.from(Line.from(
                        Span.styled("q", Style.EMPTY.fg(Color.YELLOW).bold()),
                        Span.styled(" Quit", Style.EMPTY.fg(Color.DARK_GRAY))
                )))
                .build();
        frame.renderWidget(footer, area);
    }
}
