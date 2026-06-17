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

    @Test
    @DisplayName("SS3 F4 (ESC O S) is parsed as F4")
    void ss3F4ParsedCorrectly() throws IOException {
        QueueBackend backend = new QueueBackend(27, 'O', 'S');

        Event event = EventParser.readEvent(backend, 0);

        assertThat(event).isInstanceOf(KeyEvent.class);
        KeyEvent keyEvent = (KeyEvent) event;
        assertThat(keyEvent.code()).isEqualTo(KeyCode.F4);
        assertThat(keyEvent.hasShift()).isFalse();
    }

    @Test
    @DisplayName("Shift+F1 (ESC[1;2P) is parsed as F1 with Shift")
    void shiftF1ParsedCorrectly() throws IOException {
        QueueBackend backend = new QueueBackend(27, '[', '1', ';', '2', 'P');

        Event event = EventParser.readEvent(backend, 0);

        assertThat(event).isInstanceOf(KeyEvent.class);
        KeyEvent keyEvent = (KeyEvent) event;
        assertThat(keyEvent.code()).isEqualTo(KeyCode.F1);
        assertThat(keyEvent.hasShift()).isTrue();
    }

    @Test
    @DisplayName("Shift+F4 (ESC[1;2S) is parsed as F4 with Shift")
    void shiftF4ParsedCorrectly() throws IOException {
        QueueBackend backend = new QueueBackend(27, '[', '1', ';', '2', 'S');

        Event event = EventParser.readEvent(backend, 0);

        assertThat(event).isInstanceOf(KeyEvent.class);
        KeyEvent keyEvent = (KeyEvent) event;
        assertThat(keyEvent.code()).isEqualTo(KeyCode.F4);
        assertThat(keyEvent.hasShift()).isTrue();
    }

    @Test
    @DisplayName("Ctrl+F2 (ESC[1;5Q) is parsed as F2 with Ctrl")
    void ctrlF2ParsedCorrectly() throws IOException {
        QueueBackend backend = new QueueBackend(27, '[', '1', ';', '5', 'Q');

        Event event = EventParser.readEvent(backend, 0);

        assertThat(event).isInstanceOf(KeyEvent.class);
        KeyEvent keyEvent = (KeyEvent) event;
        assertThat(keyEvent.code()).isEqualTo(KeyCode.F2);
        assertThat(keyEvent.hasCtrl()).isTrue();
    }

    @Test
    @DisplayName("Alt+F3 (ESC[1;3R) is parsed as F3 with Alt")
    void altF3ParsedCorrectly() throws IOException {
        QueueBackend backend = new QueueBackend(27, '[', '1', ';', '3', 'R');

        Event event = EventParser.readEvent(backend, 0);

        assertThat(event).isInstanceOf(KeyEvent.class);
        KeyEvent keyEvent = (KeyEvent) event;
        assertThat(keyEvent.code()).isEqualTo(KeyCode.F3);
        assertThat(keyEvent.hasAlt()).isTrue();
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
}
