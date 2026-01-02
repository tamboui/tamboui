///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS dev.tamboui:tamboui-picocli:LATEST
//DEPS dev.tamboui:tamboui-jline:LATEST

/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Layout;
import dev.tamboui.layout.Rect;
import dev.tamboui.picocli.TuiCommand;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;
import dev.tamboui.tui.Keys;
import dev.tamboui.tui.TuiRunner;
import dev.tamboui.tui.event.Event;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.ResizeEvent;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.widgets.block.Title;
import dev.tamboui.widgets.list.ListItem;
import dev.tamboui.widgets.list.ListState;
import dev.tamboui.widgets.list.ListWidget;
import dev.tamboui.widgets.paragraph.Paragraph;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.List;

/**
 * Demo showcasing PicoCLI integration with TamboUI.
 * <p>
 * This demo shows how to:
 * <ul>
 *   <li>Use TuiCommand as a base class</li>
 *   <li>Parse CLI arguments with PicoCLI</li>
 *   <li>Pass CLI options into the TUI</li>
 *   <li>Use the built-in TUI options mixin</li>
 * </ul>
 *
 * <p>Try running with different options:
 * <pre>
 * ./gradlew :demos:picocli-demo:run
 * ./gradlew :demos:picocli-demo:run --args="--title 'Custom Title'"
 * ./gradlew :demos:picocli-demo:run --args="--items Apple,Banana,Cherry"
 * ./gradlew :demos:picocli-demo:run --args="--help"
 * ./gradlew :demos:picocli-demo:run --args="--mouse --tick-rate 100"
 * </pre>
 */
@Command(
    name = "picocli-demo",
    description = "Demo TUI application with PicoCLI integration",
    mixinStandardHelpOptions = true,
    version = "1.0.0"
)
public class PicoCLIDemo extends TuiCommand {

    @Option(
        names = {"-t", "--title"},
        description = "Title for the list widget",
        defaultValue = "Items"
    )
    private String title;

    @Option(
        names = {"-i", "--items"},
        description = "Comma-separated list of items",
        split = ",",
        defaultValue = "First Item,Second Item,Third Item,Fourth Item,Fifth Item"
    )
    private List<String> items;

    @Option(
        names = {"-c", "--color"},
        description = "Highlight color (RED, GREEN, BLUE, YELLOW, CYAN, MAGENTA)",
        defaultValue = "BLUE"
    )
    private String highlightColorName;

    private final ListState listState = new ListState();

    private Color getHighlightColor() {
        return switch (highlightColorName.toUpperCase()) {
            case "RED" -> Color.RED;
            case "GREEN" -> Color.GREEN;
            case "YELLOW" -> Color.YELLOW;
            case "CYAN" -> Color.CYAN;
            case "MAGENTA" -> Color.MAGENTA;
            default -> Color.BLUE;
        };
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new PicoCLIDemo()).execute(args);
        System.exit(exitCode);
    }

    @Override
    protected void runTui(TuiRunner runner) throws Exception {
        // Initialize state
        listState.selectFirst();

        // Run the TUI with event handler and renderer
        runner.run(this::handleEvent, this::render);
    }

    private boolean handleEvent(Event event, TuiRunner runner) {
        if (Keys.isQuit(event)) {
            runner.quit();
            return false;
        }

        return switch (event) {
            case KeyEvent k -> handleKey(k);
            case ResizeEvent r -> true;
            default -> false;
        };
    }

    private boolean handleKey(KeyEvent k) {
        if (Keys.isUp(k)) {
            listState.selectPrevious();
            return true;
        }
        if (Keys.isDown(k)) {
            listState.selectNext(items.size());
            return true;
        }
        if (Keys.isHome(k)) {
            listState.selectFirst();
            return true;
        }
        if (Keys.isEnd(k)) {
            listState.select(items.size() - 1);
            return true;
        }

        // 'a' to add item
        if (Keys.isChar(k, 'a') || Keys.isChar(k, 'A')) {
            items.add("New Item " + (items.size() + 1));
            listState.select(items.size() - 1);
            return true;
        }

        // 'd' to delete item
        if (Keys.isChar(k, 'd') || Keys.isChar(k, 'D')) {
            Integer selected = listState.selected();
            if (selected != null && items.size() > 1) {
                items.remove((int) selected);
                if (selected >= items.size()) {
                    listState.select(items.size() - 1);
                }
            }
            return true;
        }

        return false;
    }

    private void render(Frame frame) {
        Rect area = frame.area();

        List<Rect> layout = Layout.vertical()
                .constraints(
                        Constraint.length(3),
                        Constraint.fill(),
                        Constraint.length(5),
                        Constraint.length(3)
                )
                .split(area);

        renderHeader(frame, layout.get(0));
        renderList(frame, layout.get(1));
        renderInfo(frame, layout.get(2));
        renderFooter(frame, layout.get(3));
    }

    private void renderHeader(Frame frame, Rect area) {
        Block header = Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.CYAN))
                .title(Title.from(
                        Line.from(
                                Span.raw(" PicoCLI ").bold().cyan(),
                                Span.raw("+ ").white(),
                                Span.raw("TamboUI ").bold().yellow(),
                                Span.raw("Demo ").white()
                        )
                ).centered())
                .build();

        frame.renderWidget(header, area);
    }

    private void renderList(Frame frame, Rect area) {
        List<ListItem> listItems = items.stream()
                .map(ListItem::from)
                .toList();

        ListWidget list = ListWidget.builder()
                .items(listItems)
                .block(Block.builder()
                        .borders(Borders.ALL)
                        .borderType(BorderType.ROUNDED)
                        .borderStyle(Style.EMPTY.fg(Color.GREEN))
                        .title(title)
                        .titleBottom(Title.from("j/k/↑/↓ navigate, a add, d delete").right())
                        .build())
                .highlightStyle(Style.EMPTY.bg(getHighlightColor()).fg(Color.WHITE).bold())
                .highlightSymbol("▶ ")
                .build();

        frame.renderStatefulWidget(list, area, listState);
    }

    private void renderInfo(Frame frame, Rect area) {
        Integer selected = listState.selected();
        String selectedText = selected != null && selected < items.size()
                ? items.get(selected)
                : "None";

        Text content = Text.from(
                Line.from(
                        Span.raw("Title: ").bold(),
                        Span.raw(title).cyan()
                ),
                Line.from(
                        Span.raw("Items: ").bold(),
                        Span.raw(String.valueOf(items.size())).yellow()
                ),
                Line.from(
                        Span.raw("Selected: ").bold(),
                        Span.raw(selectedText).green()
                )
        );

        Paragraph info = Paragraph.builder()
                .text(content)
                .block(Block.builder()
                        .borders(Borders.ALL)
                        .borderType(BorderType.ROUNDED)
                        .borderStyle(Style.EMPTY.fg(Color.MAGENTA))
                        .title("CLI Options")
                        .build())
                .build();

        frame.renderWidget(info, area);
    }

    private void renderFooter(Frame frame, Rect area) {
        Line helpLine = Line.from(
                Span.raw(" j/k/↑↓").bold().yellow(),
                Span.raw(" Navigate  ").dim(),
                Span.raw("a").bold().yellow(),
                Span.raw(" Add  ").dim(),
                Span.raw("d").bold().yellow(),
                Span.raw(" Delete  ").dim(),
                Span.raw("q/Ctrl+C").bold().yellow(),
                Span.raw(" Quit  ").dim(),
                Span.raw("--help").bold().yellow(),
                Span.raw(" CLI help").dim()
        );

        Paragraph footer = Paragraph.builder()
                .text(Text.from(helpLine))
                .block(Block.builder()
                        .borders(Borders.ALL)
                        .borderType(BorderType.ROUNDED)
                        .borderStyle(Style.EMPTY.fg(Color.DARK_GRAY))
                        .build())
                .build();

        frame.renderWidget(footer, area);
    }
}
