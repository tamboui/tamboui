///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS dev.tamboui:tamboui-widgets:LATEST
//DEPS dev.tamboui:tamboui-jline3-backend:LATEST

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import dev.tamboui.buffer.Cell;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Layout;
import dev.tamboui.layout.Position;
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
import dev.tamboui.widgets.input.TextInputState;

import java.io.IOException;

/**
 * Demo TUI application showcasing input form with focus management.
 * <p>
 * Demonstrates:
 * - Multiple text input fields
 * - Focus management with Tab navigation
 * - Custom number input field (Age)
 * - Cursor positioning for focused field
 * - Form submission and cancellation
 */
public class InputFormDemo {

    private enum Focus {
        FIRST_NAME,
        LAST_NAME,
        AGE
    }

    private enum AppState {
        RUNNING,
        CANCELLED,
        SUBMITTED
    }

    private boolean running = true;
    private AppState state = AppState.RUNNING;
    private Focus focus = Focus.FIRST_NAME;

    private final TextInputState firstNameState = new TextInputState();
    private final TextInputState lastNameState = new TextInputState();
    private int age = 0;

    private InputFormDemo() {

    }

    /**
     * Demo entry point.
     * @param args the CLI arguments
     * @throws Exception on unexpected error
     */
    public static void main(String[] args) throws Exception {
        new InputFormDemo().run();
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

            // Handle resize
            backend.onResize(() -> {
                try {
                    terminal.draw(this::ui);
                } catch (IOException e) {
                    // Ignore
                }
            });

            // Event loop
            while (running) {
                terminal.draw(this::ui);

                int c = backend.read(100);
                if (c == -2 || c == -1) {
                    continue;
                }

                handleInput(c, backend);
            }

            // Restore terminal before printing
            backend.showCursor();
            backend.leaveAlternateScreen();
            backend.disableRawMode();
            
            // Print result
            if (state == AppState.SUBMITTED) {
                System.out.println("{");
                System.out.println("  \"first_name\": \"" + firstNameState.text() + "\",");
                System.out.println("  \"last_name\": \"" + lastNameState.text() + "\",");
                System.out.println("  \"age\": " + age);
                System.out.println("}");
            } else {
                System.out.println("Canceled");
            }
        }
    }

    private void handleInput(int c, Backend backend) throws IOException {
        // Handle escape sequences
        if (c == 27) {
            int next = backend.peek(50);
            if (next == '[') {
                backend.read(50);
                int code = backend.read(50);
                handleEscapeSequence(code);
            } else {
                // Standalone ESC
                state = AppState.CANCELLED;
                running = false;
            }
            return;
        }

        switch (c) {
            case '\t' -> {
                // Tab - move to next field
                focus = switch (focus) {
                    case FIRST_NAME -> Focus.LAST_NAME;
                    case LAST_NAME -> Focus.AGE;
                    case AGE -> Focus.FIRST_NAME;
                };
            }
            case '\r', '\n' -> {
                // Enter - submit form
                state = AppState.SUBMITTED;
                running = false;
            }
            case 'q', 'Q', 3 -> {
                // Quit
                state = AppState.CANCELLED;
                running = false;
            }
            default -> {
                // Pass to focused field
                handleFieldInput(c);
            }
        }
    }

    private void handleEscapeSequence(int code) {
        switch (code) {
            case 'A' -> { // Up arrow
                if (focus == Focus.AGE) {
                    age = Math.min(130, age + 1);
                }
            }
            case 'B' -> { // Down arrow
                if (focus == Focus.AGE) {
                    age = Math.max(0, age - 1);
                }
            }
            default -> {
                // No action
            }
        }
    }

    private void handleFieldInput(int c) {
        switch (focus) {
            case FIRST_NAME -> handleStringInput(c, firstNameState);
            case LAST_NAME -> handleStringInput(c, lastNameState);
            case AGE -> handleAgeInput(c);
        }
    }

    private void handleStringInput(int c, TextInputState state) {
        if (c >= 32 && c <= 126) {
            // Printable ASCII character
            state.insert((char) c);
        } else if (c == 127 || c == 8) {
            // Backspace
            state.deleteBackward();
        } else if (c == 1) {
            // Ctrl+A - move to start
            state.moveCursorToStart();
        } else if (c == 5) {
            // Ctrl+E - move to end
            state.moveCursorToEnd();
        } else if (c == 21) {
            // Ctrl+U - clear
            state.clear();
        }
    }

    private void handleAgeInput(int c) {
        if (c >= '0' && c <= '9') {
            int digit = c - '0';
            int newAge = age * 10 + digit;
            if (newAge <= 130) {
                age = newAge;
            }
        } else if (c == 127 || c == 8) {
            // Backspace
            age = age / 10;
        } else if (c == 'k' || c == 'K') {
            // Increment
            age = Math.min(130, age + 1);
        } else if (c == 'j' || c == 'J') {
            // Decrement
            age = Math.max(0, age - 1);
        }
    }

    private void ui(Frame frame) {
        Rect area = frame.area();

        var layout = Layout.vertical()
            .constraints(
                Constraint.length(1),  // First Name
                Constraint.length(1),  // Last Name
                Constraint.length(1),  // Age
                Constraint.fill()      // Spacer
            )
            .split(area);

        renderFirstNameField(frame, layout.get(0));
        renderLastNameField(frame, layout.get(1));
        renderAgeField(frame, layout.get(2));
    }

    private void renderFirstNameField(Frame frame, Rect area) {
        boolean isFocused = focus == Focus.FIRST_NAME;
        
        // Layout: label area and value area
        var fieldLayout = Layout.horizontal()
            .constraints(
                Constraint.length(13),  // "First Name: " label
                Constraint.fill()        // Value area
            )
            .split(area);

        // Render label
        Line label = Line.from(
            Span.raw("First Name: ").style(Style.EMPTY.bold())
        );
        frame.buffer().setLine(fieldLayout.get(0).left(), fieldLayout.get(0).top(), label);

        // Render value
        String value = firstNameState.text();
        frame.buffer().setString(fieldLayout.get(1).left(), fieldLayout.get(1).top(), value, Style.EMPTY);

        // Set cursor position if focused
        if (isFocused) {
            int cursorX = fieldLayout.get(1).left() + firstNameState.cursorPosition();
            int cursorY = fieldLayout.get(1).top();
            if (fieldLayout.get(1).contains(cursorX, cursorY)) {
                Cell currentCell = frame.buffer().get(cursorX, cursorY);
                frame.buffer().set(cursorX, cursorY, currentCell.patchStyle(Style.EMPTY.reversed()));
                frame.setCursorPosition(new Position(cursorX, cursorY));
            }
        }
    }

    private void renderLastNameField(Frame frame, Rect area) {
        boolean isFocused = focus == Focus.LAST_NAME;
        
        // Layout: label area and value area
        var fieldLayout = Layout.horizontal()
            .constraints(
                Constraint.length(13),  // "Last Name: " label
                Constraint.fill()       // Value area
            )
            .split(area);

        // Render label
        Line label = Line.from(
            Span.raw("Last Name: ").style(Style.EMPTY.bold())
        );
        frame.buffer().setLine(fieldLayout.get(0).left(), fieldLayout.get(0).top(), label);

        // Render value
        String value = lastNameState.text();
        frame.buffer().setString(fieldLayout.get(1).left(), fieldLayout.get(1).top(), value, Style.EMPTY);

        // Set cursor position if focused
        if (isFocused) {
            int cursorX = fieldLayout.get(1).left() + lastNameState.cursorPosition();
            int cursorY = fieldLayout.get(1).top();
            if (fieldLayout.get(1).contains(cursorX, cursorY)) {
                Cell currentCell = frame.buffer().get(cursorX, cursorY);
                frame.buffer().set(cursorX, cursorY, currentCell.patchStyle(Style.EMPTY.reversed()));
                frame.setCursorPosition(new Position(cursorX, cursorY));
            }
        }
    }

    private void renderAgeField(Frame frame, Rect area) {
        boolean isFocused = focus == Focus.AGE;
        
        // Layout: label area and value area
        var fieldLayout = Layout.horizontal()
            .constraints(
                Constraint.length(6),   // "Age: " label
                Constraint.fill()       // Value area
            )
            .split(area);

        // Render label
        Line label = Line.from(
            Span.raw("Age: ").style(Style.EMPTY.bold())
        );
        frame.buffer().setLine(fieldLayout.get(0).left(), fieldLayout.get(0).top(), label);

        // Render age value
        String ageText = String.valueOf(age);
        frame.buffer().setString(fieldLayout.get(1).left(), fieldLayout.get(1).top(), ageText, Style.EMPTY);

        // Set cursor position if focused
        if (isFocused) {
            int cursorX = fieldLayout.get(1).left() + ageText.length();
            int cursorY = fieldLayout.get(1).top();
            if (fieldLayout.get(1).contains(cursorX, cursorY)) {
                Cell currentCell = frame.buffer().get(cursorX, cursorY);
                frame.buffer().set(cursorX, cursorY, currentCell.patchStyle(Style.EMPTY.reversed()));
                frame.setCursorPosition(new Position(cursorX, cursorY));
            }
        }
    }
}

