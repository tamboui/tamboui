/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.backend.panama.windows;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

/**
 * Verifies the {@code peek()}/{@code read()} contract on Windows.
 *
 * <p>Ensures {@code peek()} returns the actual character (or -2 if empty),
 * which is critical for CSI escape sequence parsing.
 */
@EnabledOnOs(OS.WINDOWS)
class WindowsTerminalPeekTest {

    private WindowsTerminal terminal;

    @BeforeEach
    void setUp() throws Exception {
        terminal = new WindowsTerminal();
        terminal.enableRawMode();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (terminal != null) {
            terminal.close();
        }
    }

    @Test
    @DisplayName("peek() with no queued input returns -2, not 0")
    void peekWithNoInputReturnsMinusTwo() throws Exception {
        // Non-blocking peek (timeout=0): nothing is in the buffer so the contract
        // requires -2. The broken implementation returned 0 here because it checked
        // GetNumberOfConsoleInputEvents and returned the count directly as its result.
        int result = terminal.peek(0);

        assertThat(result)
            .as("peek() with no queued input must return -2, never 0")
            .isEqualTo(-2);
    }

    @Test
    @DisplayName("read() with no queued input returns -2")
    void readWithNoInputReturnsMinusTwo() throws Exception {
        int result = terminal.read(0);

        assertThat(result).isEqualTo(-2);
    }

    @Test
    @DisplayName("peek() followed by read() both return -2 when nothing is queued")
    void peekThenReadBothReturnMinusTwoWhenEmpty() throws Exception {
        // Verifies that peek() doesn't corrupt the peekedChar field in a way that
        // causes a subsequent read() to return a stale non-negative value.
        int peeked = terminal.peek(0);
        int read   = terminal.read(0);

        assertThat(peeked).isEqualTo(-2);
        assertThat(read)
            .as("read() after an empty peek() must still return -2, not a stale value")
            .isEqualTo(-2);
    }
}
