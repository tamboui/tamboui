//DEPS dev.tamboui:tamboui-core:LATEST
//DEPS dev.tamboui:tamboui-widgets:LATEST
/*
 * Copyright (c) 2026 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import dev.tamboui.buffer.Buffer;
import static dev.tamboui.export.ExportRequest.export;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Layout;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.widgets.block.Title;
import dev.tamboui.widgets.paragraph.Paragraph;
import dev.tamboui.widgets.table.Row;
import dev.tamboui.widgets.table.Table;
import dev.tamboui.widgets.table.TableState;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Export demo: renders with widgets (no Toolkit), then exports the full buffer
 * to SVG, HTML (stylesheet and inline), and text (plain and ANSI).
 * <p>
 * Runs without a terminal. Output directory: current working directory, or path given as first argument.
 */
public final class ExportDemo {

    private static final int WIDTH = 80;
    private static final int HEIGHT = 24;

    private static final List<String[]> TABLE_DATA = List.of(
        new String[]{"Dec 20, 2019", "Star Wars: The Rise of Skywalker", "$952,110,690"},
        new String[]{"May 25, 2018", "Solo: A Star Wars Story", "$393,151,347"},
        new String[]{"Dec 15, 2017", "Star Wars Ep. VIII: The Last Jedi", "$1,332,539,889"},
        new String[]{"Dec 16, 2016", "Rogue One: A Star Wars Story", "$1,332,439,889"}
    );

    private ExportDemo() {
    }

    /**
     * Entry point.
     *
     * @param args optional output directory (default: temp directory)
     * @throws IOException if writing export files fails
     */
    public static void main(String[] args) throws IOException {
        Path outDir = args.length > 0 ? Paths.get(args[0]) : Files.createTempDirectory("tamboui-export-");
        if (args.length > 0 && !Files.isDirectory(outDir)) {
            System.err.println("Not a directory: " + outDir);
            System.exit(1);
        }

        Rect area = new Rect(0, 0, WIDTH, HEIGHT);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        render(frame, area);

        Path svg = outDir.resolve("export_demo.svg");
        Path html = outDir.resolve("export_demo.html");
        Path htmlInline = outDir.resolve("export_demo_inline.html");
        Path txt = outDir.resolve("export_demo.txt");
        Path ansi = outDir.resolve("export_demo_ansi.txt");

        export(buffer).svg().options(o -> o.title("TamboUI Export Demo")).toFile(svg);
        export(buffer).html().toFile(html);
        export(buffer).html().options(o -> o.inlineStyles(true)).toFile(htmlInline);
        export(buffer).text().toFile(txt);
        export(buffer).text().options(o -> o.styles(true)).toFile(ansi);

        System.out.println("Exported to " + outDir.toAbsolutePath() + ":");
        System.out.println("  " + svg.getFileName());
        System.out.println("  " + html.getFileName());
        System.out.println("  " + htmlInline.getFileName());
        System.out.println("  " + txt.getFileName());
        System.out.println("  " + ansi.getFileName());
    }

    private static void render(Frame frame, Rect area) {
        List<Rect> rows = Layout.vertical().constraints(
            Constraint.length(3),
            Constraint.min(10),
            Constraint.length(2)
        ).split(area);

        Rect titleRect = rows.get(0);
        Rect mainRect = rows.get(1);
        Rect footerRect = rows.get(2);

        List<Rect> cols = Layout.horizontal().constraints(
            Constraint.length(22),
            Constraint.fill()
        ).split(mainRect);

        renderTitle(frame, titleRect);
        renderSidebar(frame, cols.get(0));
        renderTable(frame, cols.get(1));
        renderFooter(frame, footerRect);
    }

    private static void renderTitle(Frame frame, Rect area) {
        Paragraph title = Paragraph.builder()
            .text(Text.from(
                Line.from(
                    Span.styled("TamboUI ", Style.EMPTY.fg(Color.CYAN).bold()),
                    Span.raw("Export Demo")
                ),
                Line.from(
                    Span.raw("SVG · HTML (stylesheet/inline) · Text (plain/ANSI)").dim()
                )
            ))
            .build();
        frame.renderWidget(title, area);
    }

    private static void renderSidebar(Frame frame, Rect area) {
        Block block = Block.builder()
            .borders(Borders.ALL)
            .borderType(BorderType.ROUNDED)
            .borderStyle(Style.EMPTY.fg(Color.BLUE))
            .title(Title.from(Line.from(Span.raw("Export"))))
            .build();
        frame.renderWidget(block, area);
        Rect inner = block.inner(area);
        if (!inner.isEmpty()) {
            Paragraph content = Paragraph.builder()
                .text(Text.from(
                Line.from(Span.raw("Export buffer to")),
                Line.from(Span.raw("SVG, HTML, or")),
                Line.from(Span.raw("plain/ANSI text."))
                ))
                .build();
            frame.renderWidget(content, inner);
        }
    }

    private static void renderTable(Frame frame, Rect area) {
        Row header = Row.from(
            dev.tamboui.widgets.table.Cell.from("Released").style(Style.EMPTY.bold()),
            dev.tamboui.widgets.table.Cell.from("Title").style(Style.EMPTY.bold()),
            dev.tamboui.widgets.table.Cell.from("Box Office").style(Style.EMPTY.bold())
        ).style(Style.EMPTY.fg(Color.YELLOW));

        List<Row> dataRows = TABLE_DATA.stream()
            .map(arr -> Row.from(arr[0], arr[1], arr[2]))
            .toList();

        Table table = Table.builder()
            .header(header)
            .rows(dataRows)
            .widths(
                Constraint.length(14),
                Constraint.fill(),
                Constraint.length(14)
            )
            .columnSpacing(1)
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.GREEN))
                .title(Title.from(Line.from(Span.raw("Star Wars Movies"))))
                .build())
            .build();

        frame.renderStatefulWidget(table, area, new TableState());
    }

    private static void renderFooter(Frame frame, Rect area) {
        Paragraph footer = Paragraph.builder()
            .text(Text.from(Line.from(
                Span.raw("Generated by ").dim(),
                Span.raw("ExportDemo").fg(Color.CYAN),
                Span.raw(" — SVG, HTML, text").dim()
            )))
            .build();
        frame.renderWidget(footer, area);
    }
}
