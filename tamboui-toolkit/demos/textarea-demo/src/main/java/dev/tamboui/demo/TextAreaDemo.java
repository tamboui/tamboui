//DEPS dev.tamboui:tamboui-toolkit:LATEST
//DEPS dev.tamboui:tamboui-jline3-backend:LATEST
/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;


import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.text.CharWidth;
import dev.tamboui.toolkit.app.ToolkitRunner;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.element.Size;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.widgets.input.TextAreaState;

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
 *   <li>Tab - Indent 4 spaces in the main editor (tabIndents())</li>
 *   <li>Shift+Tab - Switch focus between text areas</li>
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

    private TextAreaDemo() {
        // Main editor with sample text
        mainEditorState = new TextAreaState("""
            Welcome to the TextArea Demo!

            This is a multi-line text editor.
            You can:
              - Type text freely
              - Use arrow keys to navigate
              - Press Enter for new lines
              - Use Home/End for line navigation

            This editor uses the default clip mode, so this long line simply runs off the right edge of the pane instead of wrapping.

            Try editing this text!

            Horizontal scroll test: press Up and Down on the two lines below.
            世abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789
            abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789""");

        // Notes area demonstrates word wrapping while editing
        notesState = new TextAreaState(
            "This pane wraps at word boundaries: type or paste a long sentence and watch it "
            + "reflow as you edit. Up/Down move by visual row, not logical line.");

        // Read-only area demonstrates character wrapping
        readOnlyState = new TextAreaState("""
            This read-only pane wraps by character, breaking anywhere:
            ACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGT
            (also shows line numbers and a disabled cursor)""");

        // Initialize counts
        updateCounts();
    }

    /**
     * Demo entry point.
     * @param args the CLI arguments
     * @throws Exception on unexpected error
     */
    public static void main(String[] args) throws Exception {
        var demo = new TextAreaDemo();
        demo.run();
    }

    /**
     * Runs the demo application.
     *
     * @throws Exception if an error occurs
     */
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
                text(" [Shift+Tab] Switch ").dim(),
                text(" [CTRL+c] Quit ").dim()
            )).rounded().length(3),

            // Main content
            row(
                // Left column - Main editor
                column(
                    textArea(mainEditorState)
                        .title("Main Editor (clip, Tab indents)")
                        .tabIndents()
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
                        .title("Notes (word wrap)")
                        .wrapWord()
                        .placeholder("Type your notes here...")
                        .rounded()
                        .focusedBorderColor(Color.CYAN)
                        .id("notes")
                        .fill(),

                    // Read-only display area (not focusable)
                    textArea(readOnlyState)
                        .title("Display (char wrap, read-only)")
                        .wrapCharacter()
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
                        text(" Tab - indent 4 spaces (main editor)").dim(),
                        text(" Shift+Tab - switch pane").dim(),
                        text(" Backspace/Del - Delete").dim(),
                        text(" Panes: clip / word / char wrap").dim()
                    )).title("Help").rounded().length(10)
                ).fill()
            ).fill(),

            // Footer with current cursor position
            panel(() -> row(
                text("Cursor: ").dim(),
                text("Line " + (mainEditorState.cursorRow() + 1)).cyan(),
                text(", ").dim(),
                text("Col " + (mainEditorState.cursorCol() + 1)).cyan(),
                text("  View col " + mainViewportColumn()).yellow(),
                spacer(),
                text("Scroll: Row " + mainEditorState.scrollRow()).dim()
            )).rounded().length(3)
        ).render(frame, area, context);
    }

    private int mainViewportColumn() {
        int scrollCol = mainEditorState.scrollCol();
        int cursorCol = mainEditorState.cursorCol();
        if (cursorCol < scrollCol) {
            return -1;
        }
        String line = mainEditorState.getLine(mainEditorState.cursorRow());
        return CharWidth.of(line.substring(Math.min(scrollCol, line.length()), Math.min(cursorCol, line.length())));
    }

    private void updateCounts() {
        String text = mainEditorState.text();
        charCount = text.length();
        lineCount = mainEditorState.lineCount();
    }

    @Override
    public Size preferredSize(int availableWidth, int availableHeight, RenderContext context) {
        return Size.UNKNOWN;
    }

    @Override
    public Constraint constraint() {
        return Constraint.fill();
    }

}
