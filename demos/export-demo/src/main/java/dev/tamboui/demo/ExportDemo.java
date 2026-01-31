/*
 * Copyright (c) 2026 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import dev.tamboui.buffer.Buffer;
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
 * Demo that renders a sample screen and exports the buffer to all export variants.
 * <p>
 * Runs without a terminal: builds a buffer, renders a title, table, and footer,
 * then writes:
 * <ul>
 *   <li>{@code export_demo.svg} – SVG</li>
 *   <li>{@code export_demo.html} – HTML (stylesheet / non-embedded styles)</li>
 *   <li>{@code export_demo_inline.html} – HTML (inline / embedded styles)</li>
 *   <li>{@code export_demo.txt} – plain text</li>
 *   <li>{@code export_demo_ansi.txt} – text with ANSI escape codes</li>
 * </ul>
 * Output directory: current working directory, or path given as first argument.
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
     * @param args optional output directory (default: current directory)
     * @throws IOException if writing export files fails
     */
    public static void main(String[] args) throws IOException {
        Path outDir = args.length > 0 ? Paths.get(args[0]) : Paths.get(".");
        if (!Files.isDirectory(outDir)) {
            System.err.println("Not a directory: " + outDir);
            System.exit(1);
        }

        Rect area = new Rect(0, 0, WIDTH, HEIGHT);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        render(frame, area);

        Path svgPath = outDir.resolve("export_demo.svg");
        Path htmlPath = outDir.resolve("export_demo.html");
        Path htmlInlinePath = outDir.resolve("export_demo_inline.html");
        Path txtPath = outDir.resolve("export_demo.txt");
        Path txtAnsiPath = outDir.resolve("export_demo_ansi.txt");
        
        buffer.export().svg().options(o -> o.title("TamboUI Export Demo")).toFile(svgPath);
        buffer.export().html().toFile(htmlPath);
        buffer.export().html().options(o -> o.inlineStyles(true)).toFile(htmlInlinePath);
        buffer.export().text().toFile(txtPath);
        buffer.export().text().options(o -> o.styles(true)).toFile(txtAnsiPath);

        System.out.println("Exported:");
        System.out.println("  " + svgPath.toAbsolutePath());
        System.out.println("  " + htmlPath.toAbsolutePath());
        System.out.println("  " + htmlInlinePath.toAbsolutePath());
        System.out.println("  " + txtPath.toAbsolutePath());
        System.out.println("  " + txtAnsiPath.toAbsolutePath());
    }

    private static void render(Frame frame, Rect area) {
        List<Rect> rows = Layout.vertical().constraints(
            Constraint.length(3),
            Constraint.min(10),
            Constraint.length(2)
        ).split(area);

        renderTitle(frame, rows.get(0));
        renderTable(frame, rows.get(1));
        renderFooter(frame, rows.get(2));
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
                Span.raw(" → .svg, .html, .html_inline, .txt, .txt_ansi").dim()
            )))
            .build();
        frame.renderWidget(footer, area);
    }
}
