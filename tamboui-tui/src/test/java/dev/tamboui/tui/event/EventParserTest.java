/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.event;

import java.io.EOFException;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.terminal.TestBackend;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

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
    @DisplayName("EOF is reported separately from an input timeout")
    void eofDoesNotLookLikeTimeout() {
        QueueBackend backend = new QueueBackend(-1);

        assertThatThrownBy(() -> EventParser.readEvent(backend, 0))
            .isInstanceOf(EOFException.class)
            .hasMessageContaining("Terminal input closed");
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
    @DisplayName("bracketed paste that pauses half-way is one PasteEvent, not a paste followed by keys")
    void bracketedPasteWithPauseIsOnePasteEvent() throws IOException {
        // -2 is a read that times out: the rest of the paste arrives later, as over SSH
        QueueBackend backend = new QueueBackend(
            27, '[', '2', '0', '0', '~',
            'a', 'b', -2,
            'c', '\r', 'd',
            27, '[', '2', '0', '1', '~'
        );

        Event event = EventParser.readEvent(backend, 0);

        assertThat(event).isInstanceOf(PasteEvent.class);
        assertThat(((PasteEvent) event).text()).isEqualTo("abc\rd");
        assertThat(EventParser.readEvent(backend, 0)).isNull();
    }

    @Test
    @DisplayName("bracketed paste that pauses inside the end marker does not keep the marker as text")
    void bracketedPasteWithPauseInEndMarker() throws IOException {
        QueueBackend backend = new QueueBackend(
            27, '[', '2', '0', '0', '~',
            'a', 'b', 'c',
            27, -2, '[', '2', '0', '1', '~'
        );

        Event event = EventParser.readEvent(backend, 0);

        assertThat(event).isInstanceOf(PasteEvent.class);
        assertThat(((PasteEvent) event).text()).isEqualTo("abc");
        assertThat(EventParser.readEvent(backend, 0)).isNull();
    }

    @Test
    @DisplayName("EOF in a paste does not emit its partial content")
    void bracketedPasteEofDiscardsPartialContent() {
        QueueBackend backend = new QueueBackend(27, '[', '2', '0', '0', '~', 'a', 'b', -1);

        assertThatThrownBy(() -> EventParser.readEvent(backend, 0))
            .isInstanceOf(EOFException.class)
            .hasMessageContaining("bracketed paste");
    }

    @Test
    @DisplayName("paste waits past two seconds without emitting its newline as an Enter key")
    void bracketedPasteWaitsForEndMarkerAfterLongPause() {
        DelayedBackend backend = new DelayedBackend();
        backend.type("\u001b[200~ab");
        ScheduledExecutorService later = Executors.newSingleThreadScheduledExecutor();
        later.schedule(() -> backend.type("c\rd\u001b[201~"), 2100, TimeUnit.MILLISECONDS);
        try {
            Event event = assertTimeoutPreemptively(Duration.ofSeconds(6), () -> EventParser.readEvent(backend, 0));
            assertThat(event).isInstanceOf(PasteEvent.class);
            assertThat(((PasteEvent) event).text()).isEqualTo("abc\rd");
        } finally {
            later.shutdownNow();
        }
    }

    @Test
    @DisplayName("large paste is delivered intact, then normal keys resume")
    void largePasteIsNotTruncated() throws IOException {
        int[] input = new int[6 + 1_000_001 + 6 + 1];
        int[] start = {27, '[', '2', '0', '0', '~'};
        int[] end = {27, '[', '2', '0', '1', '~'};
        System.arraycopy(start, 0, input, 0, start.length);
        Arrays.fill(input, start.length, input.length - end.length - 1, 'x');
        System.arraycopy(end, 0, input, input.length - end.length - 1, end.length);
        input[input.length - 1] = 'q';
        QueueBackend backend = new QueueBackend(input);

        Event event = EventParser.readEvent(backend, 0);
        assertThat(event).isInstanceOf(PasteEvent.class);
        assertThat(((PasteEvent) event).text()).hasSize(1_000_001).startsWith("xxx").endsWith("xxx");
        assertThat(((KeyEvent) EventParser.readEvent(backend, 0)).isChar('q')).isTrue();
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
    @DisplayName("readEvent maps US (char 31) to Ctrl+/")
    void readEventMapsUsToCtrlSlash() throws IOException {
        QueueBackend backend = new QueueBackend(31);

        Event event = EventParser.readEvent(backend, 0);

        assertThat(event).isInstanceOf(KeyEvent.class);
        KeyEvent keyEvent = (KeyEvent) event;
        assertThat(keyEvent.code()).isEqualTo(KeyCode.CHAR);
        assertThat(keyEvent.isChar('/')).isTrue();
        assertThat(keyEvent.hasCtrl()).isTrue();
    }

    @Test
    @DisplayName("readEvent maps control chars 28-30 to Ctrl+\\, Ctrl+], Ctrl+^")
    void readEventMapsFsGsRsToCtrlChars() throws IOException {
        assertCtrlChar(28, '\\');
        assertCtrlChar(29, ']');
        assertCtrlChar(30, '^');
    }

    private static void assertCtrlChar(int input, char expected) throws IOException {
        KeyEvent keyEvent = (KeyEvent) EventParser.readEvent(new QueueBackend(input), 0);
        assertThat(keyEvent.code()).isEqualTo(KeyCode.CHAR);
        assertThat(keyEvent.isChar(expected)).isTrue();
        assertThat(keyEvent.hasCtrl()).isTrue();
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

    // -- Forms other terminals send: keypad 5, focus reports, Linux console and Konsole F-keys --

    @Test
    @DisplayName("ESC[E / ESC[G (keypad 5) and ESC[I / ESC[O (focus) do not swallow the key typed next")
    void csiFinalByteRightAfterBracketDoesNotSwallowNextKey() throws IOException {
        QueueBackend backend = new QueueBackend(
            27, '[', 'E', 'a',
            27, '[', 'G', 'b',
            27, '[', 'I', 'c',
            27, '[', 'O', 'd');

        List<String> keys = new ArrayList<>();
        Event event;
        while ((event = EventParser.readEvent(backend, 0)) != null) {
            KeyEvent keyEvent = (KeyEvent) event;
            keys.add(keyEvent.code() == KeyCode.CHAR ? keyEvent.string() : keyEvent.code().name());
        }

        assertThat(keys).containsExactly("UNKNOWN", "a", "UNKNOWN", "b", "UNKNOWN", "c", "UNKNOWN", "d");
    }

    @Test
    @DisplayName("Linux console F1-F5 (ESC [ [ A to ESC [ [ E) are F1-F5, not arrow keys")
    void linuxConsoleFunctionKeysParsedCorrectly() throws IOException {
        QueueBackend backend = new QueueBackend(
            27, '[', '[', 'A',
            27, '[', '[', 'B',
            27, '[', '[', 'C',
            27, '[', '[', 'D',
            27, '[', '[', 'E');

        List<KeyCode> codes = new ArrayList<>();
        Event event;
        while ((event = EventParser.readEvent(backend, 0)) != null) {
            codes.add(((KeyEvent) event).code());
        }

        assertThat(codes).containsExactly(KeyCode.F1, KeyCode.F2, KeyCode.F3, KeyCode.F4, KeyCode.F5);
    }

    @Test
    @DisplayName("Konsole's Shift+F1 (ESC O 2 P) and Ctrl+F4 (ESC O 5 S) keep their modifier")
    void ss3WithModifierParsedCorrectly() throws IOException {
        QueueBackend backend = new QueueBackend(27, 'O', '2', 'P', 27, 'O', '5', 'S');

        KeyEvent shiftF1 = (KeyEvent) EventParser.readEvent(backend, 0);
        KeyEvent ctrlF4 = (KeyEvent) EventParser.readEvent(backend, 0);

        assertThat(shiftF1.code()).isEqualTo(KeyCode.F1);
        assertThat(shiftF1.modifiers()).isEqualTo(KeyModifiers.SHIFT);
        assertThat(ctrlF4.code()).isEqualTo(KeyCode.F4);
        assertThat(ctrlF4.modifiers()).isEqualTo(KeyModifiers.CTRL);
        assertThat(EventParser.readEvent(backend, 0)).isNull();
    }

    // -- Sequences the tape player sends for F5-F12, modified cursor keys and Alt --

    @Test
    @DisplayName("F8 (ESC[19~) is parsed as F8")
    void f8ParsedCorrectly() throws IOException {
        QueueBackend backend = new QueueBackend(27, '[', '1', '9', '~');

        Event event = EventParser.readEvent(backend, 0);

        assertThat(event).isInstanceOf(KeyEvent.class);
        KeyEvent keyEvent = (KeyEvent) event;
        assertThat(keyEvent.code()).isEqualTo(KeyCode.F8);
        assertThat(keyEvent.hasShift()).isFalse();
    }

    @Test
    @DisplayName("Shift+F8 (ESC[19;2~) is parsed as F8 with Shift")
    void shiftF8ParsedCorrectly() throws IOException {
        QueueBackend backend = new QueueBackend(27, '[', '1', '9', ';', '2', '~');

        Event event = EventParser.readEvent(backend, 0);

        assertThat(event).isInstanceOf(KeyEvent.class);
        KeyEvent keyEvent = (KeyEvent) event;
        assertThat(keyEvent.code()).isEqualTo(KeyCode.F8);
        assertThat(keyEvent.hasShift()).isTrue();
    }

    @Test
    @DisplayName("Ctrl+Up (ESC[1;5A) is parsed as UP with Ctrl")
    void ctrlUpParsedCorrectly() throws IOException {
        QueueBackend backend = new QueueBackend(27, '[', '1', ';', '5', 'A');

        Event event = EventParser.readEvent(backend, 0);

        assertThat(event).isInstanceOf(KeyEvent.class);
        KeyEvent keyEvent = (KeyEvent) event;
        assertThat(keyEvent.code()).isEqualTo(KeyCode.UP);
        assertThat(keyEvent.hasCtrl()).isTrue();
    }

    @Test
    @DisplayName("Alt+x (ESC x) is parsed as x with Alt")
    void altCharParsedCorrectly() throws IOException {
        QueueBackend backend = new QueueBackend(27, 'x');

        Event event = EventParser.readEvent(backend, 0);

        assertThat(event).isInstanceOf(KeyEvent.class);
        KeyEvent keyEvent = (KeyEvent) event;
        assertThat(keyEvent.isChar('x')).isTrue();
        assertThat(keyEvent.hasAlt()).isTrue();
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

    // -- SGR mouse scroll events --

    @Test
    @DisplayName("SGR mouse button code 64 parsed as SCROLL_UP")
    void sgrScrollUpParsed() throws IOException {
        // ESC [ < 64 ; 10 ; 5 M
        QueueBackend backend = new QueueBackend(
            27, '[', '<', '6', '4', ';', '1', '0', ';', '5', 'M');

        Event event = EventParser.readEvent(backend, 0);

        assertThat(event).isInstanceOf(MouseEvent.class);
        MouseEvent mouse = (MouseEvent) event;
        assertThat(mouse.kind()).isEqualTo(MouseEventKind.SCROLL_UP);
        assertThat(mouse.x()).isEqualTo(9);
        assertThat(mouse.y()).isEqualTo(4);
    }

    @Test
    @DisplayName("SGR mouse button code 65 parsed as SCROLL_DOWN")
    void sgrScrollDownParsed() throws IOException {
        // ESC [ < 65 ; 10 ; 5 M
        QueueBackend backend = new QueueBackend(
            27, '[', '<', '6', '5', ';', '1', '0', ';', '5', 'M');

        Event event = EventParser.readEvent(backend, 0);

        assertThat(event).isInstanceOf(MouseEvent.class);
        MouseEvent mouse = (MouseEvent) event;
        assertThat(mouse.kind()).isEqualTo(MouseEventKind.SCROLL_DOWN);
        assertThat(mouse.x()).isEqualTo(9);
        assertThat(mouse.y()).isEqualTo(4);
    }

    @Test
    @DisplayName("SGR mouse button code 66 parsed as SCROLL_LEFT")
    void sgrScrollLeftParsed() throws IOException {
        // ESC [ < 66 ; 10 ; 5 M
        QueueBackend backend = new QueueBackend(
            27, '[', '<', '6', '6', ';', '1', '0', ';', '5', 'M');

        Event event = EventParser.readEvent(backend, 0);

        assertThat(event).isInstanceOf(MouseEvent.class);
        MouseEvent mouse = (MouseEvent) event;
        assertThat(mouse.kind()).isEqualTo(MouseEventKind.SCROLL_LEFT);
        assertThat(mouse.x()).isEqualTo(9);
        assertThat(mouse.y()).isEqualTo(4);
    }

    @Test
    @DisplayName("SGR mouse button code 67 parsed as SCROLL_RIGHT")
    void sgrScrollRightParsed() throws IOException {
        // ESC [ < 67 ; 10 ; 5 M
        QueueBackend backend = new QueueBackend(
            27, '[', '<', '6', '7', ';', '1', '0', ';', '5', 'M');

        Event event = EventParser.readEvent(backend, 0);

        assertThat(event).isInstanceOf(MouseEvent.class);
        MouseEvent mouse = (MouseEvent) event;
        assertThat(mouse.kind()).isEqualTo(MouseEventKind.SCROLL_RIGHT);
        assertThat(mouse.x()).isEqualTo(9);
        assertThat(mouse.y()).isEqualTo(4);
    }

    @Test
    @DisplayName("SGR horizontal scroll with Shift modifier")
    void sgrHorizontalScrollWithShiftModifier() throws IOException {
        // ESC [ < 70 ; 10 ; 5 M (66 + 4 = 70, Shift+ScrollLeft)
        QueueBackend backend = new QueueBackend(
            27, '[', '<', '7', '0', ';', '1', '0', ';', '5', 'M');

        Event event = EventParser.readEvent(backend, 0);

        assertThat(event).isInstanceOf(MouseEvent.class);
        MouseEvent mouse = (MouseEvent) event;
        assertThat(mouse.kind()).isEqualTo(MouseEventKind.SCROLL_LEFT);
        assertThat(mouse.modifiers().shift()).isTrue();
    }

    // -- SGR mouse sequences (ESC [ < Cb ; Cx ; Cy M/m) --

    @Test
    @DisplayName("SGR button 35 with M is a hover MOVE with no button")
    void sgrHoverParsedAsMove() throws IOException {
        // Button 35 = no-button (3) + motion bit (32); terminator M.
        // This is what a terminal sends for bare mouse motion under DECSET 1003.
        QueueBackend backend = new QueueBackend(27, '[', '<', '3', '5', ';', '1', '0', ';', '5', 'M');

        Event event = EventParser.readEvent(backend, 0);

        assertThat(event).isInstanceOf(MouseEvent.class);
        MouseEvent mouse = (MouseEvent) event;
        assertThat(mouse.kind()).isEqualTo(MouseEventKind.MOVE);
        assertThat(mouse.button()).isEqualTo(MouseButton.NONE);
        assertThat(mouse.x()).isEqualTo(9);  // 1-indexed column 10 -> 0-indexed 9
        assertThat(mouse.y()).isEqualTo(4);  // 1-indexed row 5 -> 0-indexed 4
    }

    @Test
    @DisplayName("SGR button 32 with M is a DRAG with the left button held")
    void sgrLeftDragParsedAsDrag() throws IOException {
        // Button 32 = left (0) + motion bit (32); terminator M.
        // Regression guard: motion with a button held must stay DRAG, not MOVE.
        QueueBackend backend = new QueueBackend(27, '[', '<', '3', '2', ';', '1', '0', ';', '5', 'M');

        Event event = EventParser.readEvent(backend, 0);

        assertThat(event).isInstanceOf(MouseEvent.class);
        MouseEvent mouse = (MouseEvent) event;
        assertThat(mouse.kind()).isEqualTo(MouseEventKind.DRAG);
        assertThat(mouse.button()).isEqualTo(MouseButton.LEFT);
    }

    @Test
    @DisplayName("SGR button 0 with M is a left-button PRESS")
    void sgrLeftPressParsedAsPress() throws IOException {
        // Button 0 = left, no motion bit; terminator M. Regression guard for the
        // non-motion branch after reordering MOVE/DRAG.
        QueueBackend backend = new QueueBackend(27, '[', '<', '0', ';', '1', '0', ';', '5', 'M');

        Event event = EventParser.readEvent(backend, 0);

        assertThat(event).isInstanceOf(MouseEvent.class);
        MouseEvent mouse = (MouseEvent) event;
        assertThat(mouse.kind()).isEqualTo(MouseEventKind.PRESS);
        assertThat(mouse.button()).isEqualTo(MouseButton.LEFT);
    }

    // -- X10 mouse reports (ESC [ M Cb Cx Cy), from terminals without SGR mouse support --

    @Test
    @DisplayName("X10 press and release are mouse events, not stray characters")
    void x10PressAndReleaseParsed() throws IOException {
        // Left press at column 81, row 3: each byte is 32 more than the value, so the column is 'q'.
        // X10 reports a release as button 3, without the button that was released.
        QueueBackend backend = new QueueBackend(
            27, '[', 'M', 32, 32 + 81, 32 + 3,
            27, '[', 'M', 32 + 3, 32 + 81, 32 + 3);

        Event press = EventParser.readEvent(backend, 0);
        Event release = EventParser.readEvent(backend, 0);

        assertThat(press).isInstanceOf(MouseEvent.class);
        MouseEvent pressEvent = (MouseEvent) press;
        assertThat(pressEvent.kind()).isEqualTo(MouseEventKind.PRESS);
        assertThat(pressEvent.button()).isEqualTo(MouseButton.LEFT);
        assertThat(pressEvent.x()).isEqualTo(80);
        assertThat(pressEvent.y()).isEqualTo(2);
        assertThat(release).isInstanceOf(MouseEvent.class);
        MouseEvent releaseEvent = (MouseEvent) release;
        assertThat(releaseEvent.kind()).isEqualTo(MouseEventKind.RELEASE);
        assertThat(releaseEvent.x()).isEqualTo(80);
        assertThat(releaseEvent.y()).isEqualTo(2);
        assertThat(EventParser.readEvent(backend, 0)).isNull();
    }

    @Test
    @DisplayName("X10 hover, drag and wheel are MOVE, DRAG and SCROLL_UP, not arrow keys")
    void x10MotionAndWheelParsed() throws IOException {
        // Button 35 is motion with no button ('C'), 32 is a left-button drag ('@'), 64 is wheel up ('`')
        QueueBackend backend = new QueueBackend(
            27, '[', 'M', 32 + 35, 32 + 10, 32 + 5,
            27, '[', 'M', 32 + 32, 32 + 10, 32 + 5,
            27, '[', 'M', 32 + 64, 32 + 10, 32 + 5);

        List<MouseEventKind> kinds = new ArrayList<>();
        Event event;
        while ((event = EventParser.readEvent(backend, 0)) != null) {
            assertThat(event).isInstanceOf(MouseEvent.class);
            MouseEvent mouse = (MouseEvent) event;
            assertThat(mouse.x()).isEqualTo(9);
            assertThat(mouse.y()).isEqualTo(4);
            kinds.add(mouse.kind());
        }

        assertThat(kinds).containsExactly(MouseEventKind.MOVE, MouseEventKind.DRAG, MouseEventKind.SCROLL_UP);
    }

    @Test
    @DisplayName("X10 report with a coordinate the reader could not decode is dropped whole")
    void x10ReportWithUndecodableCoordinateIsDroppedWhole() throws IOException {
        // Past column 95 the column byte is above 127; a UTF-8 reader turns it into U+FFFD
        QueueBackend backend = new QueueBackend(27, '[', 'M', 32, 0xFFFD, 32 + 3);

        Event event = EventParser.readEvent(backend, 0);

        assertThat(event).isInstanceOf(KeyEvent.class);
        assertThat(((KeyEvent) event).code()).isEqualTo(KeyCode.UNKNOWN);
        assertThat(EventParser.readEvent(backend, 0)).isNull();
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

    private static final class DelayedBackend extends TestBackend {
        private final BlockingQueue<Integer> input = new LinkedBlockingQueue<>();

        private DelayedBackend() {
            super(80, 24);
        }

        void type(String text) {
            text.codePoints().forEach(input::offer);
        }

        @Override
        public int read(int timeoutMs) {
            try {
                Integer c = input.poll(timeoutMs, TimeUnit.MILLISECONDS);
                return c == null ? -2 : c;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return -2;
            }
        }

        @Override
        public int peek(int timeoutMs) {
            Integer c = input.peek();
            return c == null ? -2 : c;
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
