//DEPS dev.tamboui:tamboui-toolkit:LATEST
//DEPS dev.tamboui:tamboui-jline:LATEST
/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Rect;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.app.ToolkitRunner;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.widgets.input.TextAreaState;

import java.time.Duration;

import static dev.tamboui.toolkit.Toolkit.*;

/**
 * Demo showcasing the TextArea multi-line text input widget.
 * <p>
 * Features demonstrated:
 * <ul>
 *   <li>Multi-line text editing</li>
 *   <li>Line numbers</li>
 *   <li>Placeholder text</li>
 *   <li>Text change listener</li>
 *   <li>Cursor navigation (arrows, home, end)</li>
 *   <li>Text modification (insert, delete, backspace, enter)</li>
 * </ul>
 * <p>
 * Controls:
 * <ul>
 *   <li>Tab - Switch focus between text areas</li>
 *   <li>Arrow keys - Move cursor</li>
 *   <li>Home/End - Move to start/end of line</li>
 *   <li>Enter - Insert new line</li>
 *   <li>Backspace/Delete - Remove characters</li>
 *   <li>Ctrl+C or q (when unfocused) - Quit</li>
 * </ul>
 */
public class TextAreaDemo implements Element {

    private final TextAreaState mainEditorState;
    private final TextAreaState notesState;
    private final TextAreaState readOnlyState;
    private int lineCount = 0;
    private int charCount = 0;

    public TextAreaDemo() {
        // Main editor with sample text
        mainEditorState = new TextAreaState("""
            Welcome to the TextArea Demo!

            This is a multi-line text editor.
            You can:
              - Type text freely
              - Use arrow keys to navigate
              - Press Enter for new lines
              - Use Home/End for line navigation

            Try editing this text!""");

        // Notes area starts empty with placeholder
        notesState = new TextAreaState();

        // Read-only area with pre-filled content
        readOnlyState = new TextAreaState("""
            This area demonstrates:
            - showLineNumbers()
            - Custom lineNumberStyle()
            - Text that cannot be edited
            (Focus is disabled)""");

        // Initialize counts
        updateCounts();
    }

    public static void main(String[] args) throws Exception {
        var demo = new TextAreaDemo();
        demo.run();
    }

    public void run() throws Exception {
        var config = TuiConfig.builder()
            .mouseCapture(true)
            .noTick()
            .build();

        try (var runner = ToolkitRunner.create(config)) {
            runner.run(() -> this);
        }
    }

    @Override
    public void render(Frame frame, Rect area, RenderContext context) {
        column(
            // Header
            panel(() -> row(
                text(" TextArea Demo ").bold().cyan(),
                spacer(),
                text(" Lines: " + lineCount + " ").yellow(),
                text(" Chars: " + charCount + " ").yellow(),
                text(" [Tab] Switch ").dim(),
                text(" [CTRL+c] Quit ").dim()
            )).rounded().length(3),

            // Main content
            row(
                // Left column - Main editor
                column(
                    textArea(mainEditorState)
                        .title("Main Editor")
                        .showLineNumbers()
                        .rounded()
                        .focusedBorderColor(Color.CYAN)
                        .id("main-editor")
                        .onTextChange(text -> updateCounts())
                        .fill()
                ).percent(50),

                // Right column - Notes and info
                column(
                    // Notes area with placeholder
                    textArea(notesState)
                        .title("Notes")
                        .placeholder("Type your notes here...")
                        .rounded()
                        .focusedBorderColor(Color.CYAN)
                        .id("notes")
                        .fill(),

                    // Read-only display area (not focusable)
                    textArea(readOnlyState)
                        .title("Display (Read-only)")
                        .showLineNumbers()
                        .lineNumberStyle(Style.EMPTY.fg(Color.CYAN))
                        .showCursor(false)
                        .rounded()
                        .length(8),

                    // Help panel
                    panel(() -> column(
                        text("Keyboard Shortcuts:").bold().cyan(),
                        text(" Arrow keys - Navigate").dim(),
                        text(" Home/End - Line start/end").dim(),
                        text(" Enter - New line").dim(),
                        text(" Tab - 4 spaces").dim(),
                        text(" Backspace/Del - Delete").dim()
                    )).title("Help").rounded().length(9)
                ).fill()
            ).fill(),

            // Footer with current cursor position
            panel(() -> row(
                text("Cursor: ").dim(),
                text("Line " + (mainEditorState.cursorRow() + 1)).cyan(),
                text(", ").dim(),
                text("Col " + (mainEditorState.cursorCol() + 1)).cyan(),
                spacer(),
                text("Scroll: Row " + mainEditorState.scrollRow()).dim()
            )).rounded().length(3)
        ).render(frame, area, context);
    }

    private void updateCounts() {
        String text = mainEditorState.text();
        charCount = text.length();
        lineCount = mainEditorState.lineCount();
    }

    @Override
    public Constraint constraint() {
        return Constraint.fill();
    }

}
