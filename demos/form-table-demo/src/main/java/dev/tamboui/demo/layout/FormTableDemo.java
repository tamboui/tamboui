//DEPS dev.tamboui:tamboui-toolkit:LATEST
//DEPS dev.tamboui:tamboui-jline3-backend:LATEST
/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo.layout;

import dev.tamboui.layout.Flex;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.toolkit.app.ToolkitRunner;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.elements.FormFieldElement;
import dev.tamboui.toolkit.elements.Panel;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.widgets.form.FieldType;
import dev.tamboui.widgets.form.FormState;
import dev.tamboui.widgets.form.Validators;
import dev.tamboui.widgets.table.TableState;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import static dev.tamboui.toolkit.Toolkit.*;

/**
 * Demo application showcasing flex layouts for forms and tables.
 */
public final class FormTableDemo {

    private static final String[] TAB_NAMES = { "F1:Form", "F2:Validation", "F3:Table" };
    private static final int LABEL_WIDTH = 14;
    private static final AtomicInteger TAB_INDEX = new AtomicInteger(0);
    private static final TableState TABLE_STATE = new TableState();

    // Centralized form state for all fields
    private static final FormState FORM = FormState.builder()
            // Profile fields (text)
            .textField("fullName", "Ada Lovelace")
            .textField("email", "ada@analytical.io")
            // Preferences fields (boolean + select)
            .selectField("theme", Arrays.asList("Nord", "Dracula", "Solarized", "Monokai"), 0)
            .booleanField("notifications", true)
            .booleanField("darkMode", false)
            // Security fields (boolean + text)
            .booleanField("twoFa", true)
            .textField("sessions", "3 active")
            .build();

    // Validation demo form state - validation results are stored here, not in FormFieldElement
    private static final FormState VALIDATION_FORM = FormState.builder()
            .textField("username", "")
            .textField("email", "")
            .maskedField("password", "")  // masked field for password
            .textField("age", "")
            .textField("phone", "")
            .textField("website", "")
            .build();

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

    /**
     * Demo entry point.
     * @param args the CLI arguments
     * @throws Exception on unexpected error
     */
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
        String hint = switch (tab) {
            case 0 -> "Form fields: text inputs, checkboxes, toggles, and select dropdowns.";
            case 1 -> "Validation: required, email, minLength, maxLength, pattern, range.";
            default -> "Table uses length/percent/fill columns. Use Up/Down to move selection.";
        };

        Element content = switch (tab) {
            case 0 -> renderFormLayout();
            case 1 -> renderValidationLayout();
            default -> renderTableLayout();
        };

        return column(
                header(tab).length(4),
                text(" " + hint).dim().length(1),
                content
        ).spacing(1)
         .fill()
         .focusable()
         .id("root")
         .onKeyEvent(FormTableDemo::handleKey);
    }

    private static EventResult handleKey(KeyEvent event) {
        if (event.code() == KeyCode.F1) {
            TAB_INDEX.set(0);
            return EventResult.HANDLED;
        }
        if (event.code() == KeyCode.F2) {
            TAB_INDEX.set(1);
            return EventResult.HANDLED;
        }
        if (event.code() == KeyCode.F3) {
            TAB_INDEX.set(2);
            return EventResult.HANDLED;
        }
        if (TAB_INDEX.get() == 2) {
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

    private static Panel header(int tab) {
        return panel(() -> column(
                row(
                        tab(0, tab),
                        tab(1, tab),
                        tab(2, tab)
                ).spacing(1).length(1),
                row(
                        text(" Tabs: [F1/F2/F3] ").dim(),
                        text(" Focus: [Tab] ").dim(),
                        text(" Table: [Up/Down] ").dim(),
                        text(" [Ctrl+C] Quit ").dim()
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
                panel("Profile (Text Fields)", column(
                        formField("Full name", FORM.textField("fullName"))
                                .id("full-name").labelWidth(LABEL_WIDTH).rounded()
                                .borderColor(Color.DARK_GRAY).focusedBorderColor(Color.CYAN)
                                .arrowNavigation(true),
                        formField("Email", FORM.textField("email"))
                                .id("email").labelWidth(LABEL_WIDTH).rounded()
                                .borderColor(Color.DARK_GRAY).focusedBorderColor(Color.CYAN)
                                .arrowNavigation(true)
                ))
                .rounded()
                .borderColor(Color.CYAN)
                .fit(),

                row(
                        panel("Preferences (Select + Toggle)", column(
                                formField("Theme", FORM.selectField("theme"))
                                        .id("theme").labelWidth(LABEL_WIDTH),
                                formField("Notifications", FORM.booleanField("notifications"), FieldType.CHECKBOX)
                                        .id("notifications").labelWidth(LABEL_WIDTH)
                                        .checkedColor(Color.GREEN)
                                        .arrowNavigation(true),
                                formField("Dark Mode", FORM.booleanField("darkMode"), FieldType.TOGGLE)
                                        .id("dark-mode").labelWidth(LABEL_WIDTH)
                                        .arrowNavigation(true)
                        ))
                        .rounded()
                        .borderColor(Color.GREEN)
                        .fill(),

                        panel("Security (Checkbox + Text)", column(
                                formField("2FA Enabled", FORM.booleanField("twoFa"), FieldType.CHECKBOX)
                                        .id("2fa").labelWidth(LABEL_WIDTH)
                                        .bulletStyle().checkedColor(Color.GREEN)
                                        .arrowNavigation(true),
                                formField("Sessions", FORM.textField("sessions"))
                                        .id("sessions").labelWidth(LABEL_WIDTH).rounded()
                                        .borderColor(Color.DARK_GRAY).focusedBorderColor(Color.CYAN)
                                        .arrowNavigation(true)
                        ))
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

    private static Element renderValidationLayout() {
        // Form fields are created inline on each render - validation state persists in VALIDATION_FORM
        return column(
                row(
                        panel("Required & Length", column(
                                formField("Username", VALIDATION_FORM.textField("username"))
                                        .formState(VALIDATION_FORM, "username")
                                        .id("val-username").labelWidth(LABEL_WIDTH).rounded()
                                        .borderColor(Color.DARK_GRAY).focusedBorderColor(Color.CYAN)
                                        .errorBorderColor(Color.RED)
                                        .placeholder("3-20 characters")
                                        .validate(Validators.required(), Validators.minLength(3), Validators.maxLength(20))
                                        .showInlineErrors(true)
                                        .arrowNavigation(true),
                                formField("Password", VALIDATION_FORM.textField("password"))
                                        .formState(VALIDATION_FORM, "password")
                                        .id("val-password").labelWidth(LABEL_WIDTH).rounded()
                                        .borderColor(Color.DARK_GRAY).focusedBorderColor(Color.CYAN)
                                        .errorBorderColor(Color.RED)
                                        .placeholder("min 8 characters")
                                        .validate(Validators.required("Password is required"), Validators.minLength(8))
                                        .showInlineErrors(true)
                                        .arrowNavigation(true)
                        ))
                        .rounded()
                        .borderColor(Color.CYAN)
                        .fill(),

                        panel("Format Validation", column(
                                formField("Email", VALIDATION_FORM.textField("email"))
                                        .formState(VALIDATION_FORM, "email")
                                        .id("val-email").labelWidth(LABEL_WIDTH).rounded()
                                        .borderColor(Color.DARK_GRAY).focusedBorderColor(Color.CYAN)
                                        .errorBorderColor(Color.RED)
                                        .placeholder("you@example.com")
                                        .validate(Validators.required(), Validators.email())
                                        .showInlineErrors(true)
                                        .arrowNavigation(true),
                                formField("Phone", VALIDATION_FORM.textField("phone"))
                                        .formState(VALIDATION_FORM, "phone")
                                        .id("val-phone").labelWidth(LABEL_WIDTH).rounded()
                                        .borderColor(Color.DARK_GRAY).focusedBorderColor(Color.CYAN)
                                        .errorBorderColor(Color.RED)
                                        .placeholder("123-456-7890")
                                        .validate(Validators.pattern("\\d{3}-\\d{3}-\\d{4}", "Format: 123-456-7890"))
                                        .showInlineErrors(true)
                                        .arrowNavigation(true)
                        ))
                        .rounded()
                        .borderColor(Color.GREEN)
                        .fill()
                ).spacing(2).fill(),

                row(
                        panel("Range Validation", column(
                                formField("Age", VALIDATION_FORM.textField("age"))
                                        .formState(VALIDATION_FORM, "age")
                                        .id("val-age").labelWidth(LABEL_WIDTH).rounded()
                                        .borderColor(Color.DARK_GRAY).focusedBorderColor(Color.CYAN)
                                        .errorBorderColor(Color.RED)
                                        .placeholder("18-120")
                                        .validate(Validators.required(), Validators.range(18, 120))
                                        .showInlineErrors(true)
                                        .arrowNavigation(true)
                        ))
                        .rounded()
                        .borderColor(Color.YELLOW)
                        .fill(),

                        panel("Pattern (URL)", column(
                                formField("Website", VALIDATION_FORM.textField("website"))
                                        .formState(VALIDATION_FORM, "website")
                                        .id("val-website").labelWidth(LABEL_WIDTH).rounded()
                                        .borderColor(Color.DARK_GRAY).focusedBorderColor(Color.CYAN)
                                        .errorBorderColor(Color.RED)
                                        .placeholder("https://example.com")
                                        .validate(Validators.pattern("https?://.*", "Must start with http(s)://"))
                                        .showInlineErrors(true)
                                        .arrowNavigation(true)
                        ))
                        .rounded()
                        .borderColor(Color.MAGENTA)
                        .fill()
                ).spacing(2).fill(),

                text(" Validation triggers automatically as you type. Errors shown below fields.")
                        .dim().length(1)
        ).spacing(1).fill();
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
