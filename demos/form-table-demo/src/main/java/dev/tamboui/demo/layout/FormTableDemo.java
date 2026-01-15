//DEPS dev.tamboui:tamboui-toolkit:LATEST
//DEPS dev.tamboui:tamboui-jline:LATEST
/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo.layout;

import dev.tamboui.layout.Flex;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.toolkit.app.ToolkitRunner;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.elements.Panel;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.widgets.table.TableState;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static dev.tamboui.toolkit.Toolkit.*;

/**
 * Demo application showcasing flex layouts for forms and tables.
 */
public final class FormTableDemo {

    private static final String[] TAB_NAMES = { "1:Form", "2:Table" };
    private static final int LABEL_WIDTH = 14;
    private static final AtomicInteger TAB_INDEX = new AtomicInteger(0);
    private static final TableState TABLE_STATE = new TableState();
    private static final String[][] TABLE_ROWS = {
            { "TX-1001", "Open", "Onboarding", "2d", "Low" },
            { "TX-1002", "In Review", "Billing", "6h", "High" },
            { "TX-1003", "Blocked", "Security", "1d", "Urgent" },
            { "TX-1004", "QA", "Integrations", "3d", "Medium" },
            { "TX-1005", "Scheduled", "Growth", "5d", "Low" },
            { "TX-1006", "In Progress", "Mobile", "12h", "High" }
    };

    static {
        TABLE_STATE.select(0);
    }

    private FormTableDemo() {
    }

    public static void main(String[] args) throws Exception {
        var config = TuiConfig.builder()
                .tickRate(Duration.ofMillis(100))
                .build();

        try (var runner = ToolkitRunner.create(config)) {
            runner.run(FormTableDemo::renderUI);
        }
    }

    private static Element renderUI() {
        int tab = TAB_INDEX.get();
        String hint = tab == 0
                ? "Form uses label length + input fill, with nested rows for alignment."
                : "Table uses length/percent/fill columns. Use Up/Down to move selection.";

        return column(
                header(tab).length(4),
                text(" " + hint).dim().length(1),
                tab == 0 ? renderFormLayout() : renderTableLayout()
        ).spacing(1)
         .fill()
         .focusable()
         .id("root")
         .onKeyEvent(FormTableDemo::handleKey);
    }

    private static EventResult handleKey(KeyEvent event) {
        if (event.isLeft()) {
            shiftTab(-1);
            return EventResult.HANDLED;
        }
        if (event.isRight()) {
            shiftTab(1);
            return EventResult.HANDLED;
        }
        if (event.isChar('1')) {
            TAB_INDEX.set(0);
            return EventResult.HANDLED;
        }
        if (event.isChar('2')) {
            TAB_INDEX.set(1);
            return EventResult.HANDLED;
        }
        if (TAB_INDEX.get() == 1) {
            if (event.isDown()) {
                TABLE_STATE.selectNext(TABLE_ROWS.length);
                return EventResult.HANDLED;
            }
            if (event.isUp()) {
                TABLE_STATE.selectPrevious();
                return EventResult.HANDLED;
            }
        }
        return EventResult.UNHANDLED;
    }

    private static void shiftTab(int delta) {
        int next = (TAB_INDEX.get() + delta + TAB_NAMES.length) % TAB_NAMES.length;
        TAB_INDEX.set(next);
    }

    private static Panel header(int tab) {
        return panel(() -> column(
                row(
                        tab(0, tab),
                        tab(1, tab)
                ).spacing(1).length(1),
                row(
                        text(" Tabs: [1/2] ").dim(),
                        text(" Layout: [<- / ->] ").dim(),
                        text(" Table: [Up/Down] ").dim()
                ).length(1)
        )).rounded().borderColor(Color.DARK_GRAY);
    }

    private static Element tab(int index, int current) {
        String name = TAB_NAMES[index];
        if (index == current) {
            return text(" " + name + " ").bold().black().bg(Color.CYAN);
        }
        return text(" " + name + " ").dim();
    }

    private static Element renderFormLayout() {
        return column(
                panel("Profile", column(
                        formRow("Full name", "Ada Lovelace"),
                        formRow("Email", "ada@analytical.io"),
                        formRow("Role", "Research"),
                        formRow("Time zone", "UTC+1")
                ).spacing(1))
                .rounded()
                .borderColor(Color.CYAN)
                .fit(),

                row(
                        panel("Preferences", column(
                                formRow("Theme", "Nord"),
                                formRow("Density", "Comfortable"),
                                formRow("Notifications", "Mentions + Direct")
                        ).spacing(1))
                        .rounded()
                        .borderColor(Color.GREEN)
                        .fill(),

                        panel("Security", column(
                                formRow("2FA", "Enabled"),
                                formRow("Sessions", "3 active"),
                                formRow("Keys", "2 registered")
                        ).spacing(1))
                        .rounded()
                        .borderColor(Color.YELLOW)
                        .fill()
                ).spacing(2).fill(),

                row(
                        text(" Save ").bold().black().onGreen(),
                        text(" Cancel ").bold().white().bg(Color.DARK_GRAY)
                ).spacing(2).flex(Flex.END).length(1)
        ).spacing(1).fill();
    }

    private static Element formRow(String label, String value) {
        return row(
                text(label).dim().length(LABEL_WIDTH),
                textInput()
                        .text(value)
                        .showCursor(false)
                        .rounded()
                        .borderColor(Color.DARK_GRAY)
                        .fill()
        ).spacing(1).length(3);
    }

    private static Element renderTableLayout() {
        var table = table()
                .header("Ticket", "Status", "Owner", "Age", "Priority")
                .widths(length(8), percent(20), fill(2), length(5), length(8))
                .columnSpacing(1)
                .state(TABLE_STATE)
                .highlightSymbol(">> ")
                .highlightStyle(Style.EMPTY.bg(Color.CYAN).fg(Color.BLACK));

        for (String[] row : TABLE_ROWS) {
            table.row(row);
        }

        return column(
                panel(() -> table)
                        .title("Work Queue")
                        .rounded()
                        .borderColor(Color.CYAN)
                        .fill(),
                text(" Widths: length(8), percent(20), fill(2), length(5), length(8).")
                        .dim()
                        .length(1)
        ).spacing(1).fill();
    }
}
