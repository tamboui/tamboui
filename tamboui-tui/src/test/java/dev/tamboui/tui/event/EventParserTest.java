/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.event;

import java.io.IOException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.terminal.TestBackend;

import static org.assertj.core.api.Assertions.*;

class EventParserTest {

    @Test
    @DisplayName("readEvent maps BS (char 8) to BACKSPACE")
    void readEventMapsBsToBackspace() throws IOException {
        QueueBackend backend = new QueueBackend(8);

        Event event = EventParser.readEvent(backend, 0);

        assertThat(event).isInstanceOf(KeyEvent.class);
        KeyEvent keyEvent = (KeyEvent) event;
        assertThat(keyEvent.code()).isEqualTo(KeyCode.BACKSPACE);
        assertThat(keyEvent.modifiers()).isEqualTo(KeyModifiers.NONE);
    }

    @Test
    @DisplayName("readEvent passes through a non-ASCII BMP code point (é = U+00E9)")
    void readEventPassesThroughBmpCodePoint() throws IOException {
        // Backend already decoded UTF-8; EventParser receives the code point directly
        QueueBackend backend = new QueueBackend(0x00E9);

        Event event = EventParser.readEvent(backend, 0);

        assertThat(event).isInstanceOf(KeyEvent.class);
        KeyEvent keyEvent = (KeyEvent) event;
        assertThat(keyEvent.code()).isEqualTo(KeyCode.CHAR);
        assertThat(keyEvent.codePoint()).isEqualTo(0x00E9);
        assertThat(keyEvent.string()).isEqualTo("é");
    }

    @Test
    @DisplayName("readEvent passes through a supplementary code point (😀 = U+1F600)")
    void readEventPassesThroughSupplementaryCodePoint() throws IOException {
        QueueBackend backend = new QueueBackend(0x1F600);

        Event event = EventParser.readEvent(backend, 0);

        assertThat(event).isInstanceOf(KeyEvent.class);
        KeyEvent keyEvent = (KeyEvent) event;
        assertThat(keyEvent.code()).isEqualTo(KeyCode.CHAR);
        assertThat(keyEvent.codePoint()).isEqualTo(0x1F600);
        assertThat(keyEvent.string()).isEqualTo("😀");
    }

    @Test
    @DisplayName("bracketed paste emits PasteEvent with full content")
    void bracketedPasteEmitsPasteEvent() throws IOException {
        // ESC[200~ hello ESC[201~
        QueueBackend backend = new QueueBackend(
            27, '[', '2', '0', '0', '~',  // ESC[200~
            'h', 'e', 'l', 'l', 'o',      // content
            27, '[', '2', '0', '1', '~'   // ESC[201~
        );

        Event event = EventParser.readEvent(backend, 0);

        assertThat(event).isInstanceOf(PasteEvent.class);
        assertThat(((PasteEvent) event).text()).isEqualTo("hello");
    }

    @Test
    @DisplayName("bracketed paste preserves supplementary Unicode (😀 = U+1F600)")
    void bracketedPastePreservesUnicode() throws IOException {
        QueueBackend backend = new QueueBackend(
            27, '[', '2', '0', '0', '~',
            0x1F600,
            27, '[', '2', '0', '1', '~'
        );

        Event event = EventParser.readEvent(backend, 0);

        assertThat(event).isInstanceOf(PasteEvent.class);
        assertThat(((PasteEvent) event).text()).isEqualTo("😀");
    }

    @Test
    @DisplayName("readEvent recognizes ESC[Z as Shift+Tab")
    void readEventRecognizesShiftTab() throws IOException {
        // ESC[Z is the standard escape sequence for Shift+Tab (backtab)
        QueueBackend backend = new QueueBackend(27, '[', 'Z');

        Event event = EventParser.readEvent(backend, 0);

        assertThat(event).isInstanceOf(KeyEvent.class);
        KeyEvent keyEvent = (KeyEvent) event;
        assertThat(keyEvent.code()).isEqualTo(KeyCode.TAB);
        assertThat(keyEvent.modifiers()).isEqualTo(KeyModifiers.SHIFT);
        assertThat(keyEvent.hasShift()).isTrue();
    }

    // -- Arrow key CSI sequences (ESC [ A/B/C/D) --

    @Test
    @DisplayName("ESC[A is parsed as UP arrow")
    void arrowUpParsedFromCsiSequence() throws IOException {
        QueueBackend backend = new QueueBackend(27, '[', 'A');

        Event event = EventParser.readEvent(backend, 0);

        assertThat(event).isInstanceOf(KeyEvent.class);
        assertThat(((KeyEvent) event).code()).isEqualTo(KeyCode.UP);
    }

    @Test
    @DisplayName("ESC[B is parsed as DOWN arrow")
    void arrowDownParsedFromCsiSequence() throws IOException {
        QueueBackend backend = new QueueBackend(27, '[', 'B');

        Event event = EventParser.readEvent(backend, 0);

        assertThat(event).isInstanceOf(KeyEvent.class);
        assertThat(((KeyEvent) event).code()).isEqualTo(KeyCode.DOWN);
    }

    @Test
    @DisplayName("ESC[C is parsed as RIGHT arrow")
    void arrowRightParsedFromCsiSequence() throws IOException {
        QueueBackend backend = new QueueBackend(27, '[', 'C');

        Event event = EventParser.readEvent(backend, 0);

        assertThat(event).isInstanceOf(KeyEvent.class);
        assertThat(((KeyEvent) event).code()).isEqualTo(KeyCode.RIGHT);
    }

    @Test
    @DisplayName("ESC[D is parsed as LEFT arrow")
    void arrowLeftParsedFromCsiSequence() throws IOException {
        QueueBackend backend = new QueueBackend(27, '[', 'D');

        Event event = EventParser.readEvent(backend, 0);

        assertThat(event).isInstanceOf(KeyEvent.class);
        assertThat(((KeyEvent) event).code()).isEqualTo(KeyCode.LEFT);
    }

    @Test
    @DisplayName("consecutive arrow keys are each parsed independently")
    void consecutiveArrowKeysEachParsedIndependently() throws IOException {
        // ESC[A ESC[B — two separate UP and DOWN events
        QueueBackend backend = new QueueBackend(27, '[', 'A', 27, '[', 'B');

        Event first  = EventParser.readEvent(backend, 0);
        Event second = EventParser.readEvent(backend, 0);

        assertThat(((KeyEvent) first).code()).isEqualTo(KeyCode.UP);
        assertThat(((KeyEvent) second).code()).isEqualTo(KeyCode.DOWN);
    }

    @Test
    @DisplayName("arrow keys become UNKNOWN when peek() returns 0 instead of the actual char")
    void arrowKeyBecomesUnknownWhenPeekReturnsFlagInsteadOfChar() throws IOException {
        // Simulates the broken WindowsTerminal.peek() that returned 0 (flag) rather
        // than the next character. ESC [A should be UP, but with wrong peek it is UNKNOWN.
        BrokenPeekBackend backend = new BrokenPeekBackend(27, '[', 'A');

        Event event = EventParser.readEvent(backend, 0);

        assertThat(event).isInstanceOf(KeyEvent.class);
        assertThat(((KeyEvent) event).code())
            .as("peek() returning 0 instead of '[' prevents CSI detection")
            .isEqualTo(KeyCode.UNKNOWN);
    }

    @Test
    @DisplayName("arrow keys are correctly parsed when peek() returns the actual char")
    void arrowKeyCorrectlyParsedWhenPeekReturnsActualChar() throws IOException {
        // The correct implementation: peek() returns the real next character value.
        // This is the contract required by EventParser and fulfilled by the fix.
        QueueBackend backend = new QueueBackend(27, '[', 'A');

        Event event = EventParser.readEvent(backend, 0);

        assertThat(event).isInstanceOf(KeyEvent.class);
        assertThat(((KeyEvent) event).code()).isEqualTo(KeyCode.UP);
    }

    private static final class QueueBackend extends TestBackend {

        private final int[] input;

        private int index;

        private QueueBackend(int... input) {
            super(80, 24);
            this.input = input;
            this.index = 0;
        }

        @Override
        public int read(int timeoutMs) {
            if (index >= input.length) {
                return -2;
            }
            return input[index++];
        }

        @Override
        public int peek(int timeoutMs) {
            if (index >= input.length) {
                return -2;
            }
            return input[index];
        }
    }

    /**
     * Reproduces the broken WindowsTerminal.peek() behaviour: returns 0 when input is
     * available instead of the actual next character value. Used to document and lock in
     * the regression so it is immediately obvious if peek() is ever broken this way again.
     */
    private static final class BrokenPeekBackend extends TestBackend {

        private final int[] input;
        private int index;

        private BrokenPeekBackend(int... input) {
            super(80, 24);
            this.input = input;
            this.index = 0;
        }

        @Override
        public int read(int timeoutMs) {
            if (index >= input.length) {
                return -2;
            }
            return input[index++];
        }

        @Override
        public int peek(int timeoutMs) {
            // Intentionally broken: returns 0 (events available) instead of the actual char.
            // This is exactly what WindowsTerminal.peek() did before the fix.
            return index < input.length ? 0 : -2;
        }
    }
}
