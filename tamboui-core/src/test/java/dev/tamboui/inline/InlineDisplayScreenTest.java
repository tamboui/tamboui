/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.inline;

import java.io.IOException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.style.Style;
import dev.tamboui.text.Line;
import dev.tamboui.text.Text;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Plays the output of an {@link InlineDisplay} on a {@link TerminalScreen} and checks the lines
 * a user sees.
 */
class InlineDisplayScreenTest {

    @Test
    @DisplayName("release() with clearOnClose() clears every display line when the cursor is on a lower line")
    void clearOnCloseClearsEveryLineWhenCursorIsOnLowerLine() throws IOException {
        TerminalScreen screen = new TerminalScreen(30, 10);
        screen.writeRaw("$ new-project\r\n");
        InlineDisplay display = InlineDisplay.withBackend(3, 30, screen).clearOnClose();

        // A form with its text input, and so the cursor, on line 1
        display.render((area, buf) -> {
            buf.setString(0, 0, "New project", Style.EMPTY);
            buf.setString(0, 1, "Name: my-app", Style.EMPTY);
            buf.setString(0, 2, "[Enter] create", Style.EMPTY);
        }, 3, 12, 1);
        display.release();

        assertThat(screen.lines()).containsExactly("$ new-project");
    }

    @Test
    @DisplayName("println() prints every line of a multi-line message above the display")
    void printlnPrintsEveryLineOfMultiLineMessage() throws IOException {
        TerminalScreen screen = new TerminalScreen(30, 4);
        InlineDisplay display = InlineDisplay.withBackend(2, 30, screen);
        display.render((area, buf) -> {
            buf.setString(0, 0, "Downloading 42%", Style.EMPTY);
            buf.setString(0, 1, "[q] quit", Style.EMPTY);
        });

        display.println("one\ntwo\nthree");

        assertThat(screen.lines()).containsExactly("one", "two", "three", "Downloading 42%", "[q] quit");
    }

    @Test
    @DisplayName("println(Text) prints every line of the text above the display")
    void printlnTextPrintsEveryLine() throws IOException {
        TerminalScreen screen = new TerminalScreen(30, 10);
        InlineDisplay display = InlineDisplay.withBackend(1, 30, screen);
        display.render((area, buf) -> buf.setString(0, 0, "Downloading 42%", Style.EMPTY));

        display.println(Text.from(Line.from("first"), Line.from("second")));

        assertThat(screen.lines()).containsExactly("first", "second", "Downloading 42%");
    }

    @Test
    @DisplayName("println() starts each line of a message at column 0 when a line feed keeps the column")
    void printlnStartsEachLineAtColumnZeroWithRawOutput() throws IOException {
        TerminalScreen screen = TerminalScreen.withRawOutput(30, 10);
        InlineDisplay display = InlineDisplay.withBackend(1, 30, screen);

        display.println("one\ntwo");
        display.render((area, buf) -> buf.setString(0, 0, "Downloading 42%", Style.EMPTY));
        display.println("three\nfour");

        assertThat(screen.lines()).containsExactly("one", "two", "three", "four", "Downloading 42%");
    }
}
