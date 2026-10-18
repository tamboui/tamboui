/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.event;

import java.io.EOFException;
import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
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

    // ==================== Kitty Keyboard Protocol Tests ====================

    @Test
    @DisplayName("Kitty CSI 27u is parsed as ESCAPE key")
    void kittyEscapeKey() throws IOException {
        QueueBackend backend = backendFor("\033[27u");
        Event event = EventParser.readEvent(backend, 0);

        assertThat(event).isInstanceOf(KeyEvent.class);
        KeyEvent key = (KeyEvent) event;
        assertThat(key.code()).isEqualTo(KeyCode.ESCAPE);
        assertThat(key.modifiers()).isEqualTo(KeyModifiers.NONE);
        assertThat(key.kind()).isEqualTo(KeyEventKind.PRESS);
        assertThat(key.isPress()).isTrue();
    }

    @Test
    @DisplayName("Kitty CSI 27;1:3u is parsed as ESCAPE release")
    void kittyEscapeRelease() throws IOException {
        QueueBackend backend = backendFor("\033[27;1:3u");
        Event event = EventParser.readEvent(backend, 0);

        assertThat(event).isInstanceOf(KeyEvent.class);
        KeyEvent key = (KeyEvent) event;
        assertThat(key.code()).isEqualTo(KeyCode.ESCAPE);
        assertThat(key.kind()).isEqualTo(KeyEventKind.RELEASE);
        assertThat(key.isRelease()).isTrue();
    }

    @Test
    @DisplayName("Kitty disambiguates Tab from Ctrl+I")
    void kittyDisambiguatesTabFromCtrlI() throws IOException {
        // Tab key: CSI 9 u
        QueueBackend tabBackend = backendFor("\033[9u");
        KeyEvent tabEvent = (KeyEvent) EventParser.readEvent(tabBackend, 0);
        assertThat(tabEvent.code()).isEqualTo(KeyCode.TAB);
        assertThat(tabEvent.modifiers()).isEqualTo(KeyModifiers.NONE);

        // Ctrl+I: CSI 105;5 u
        QueueBackend ctrlIBackend = backendFor("\033[105;5u");
        KeyEvent ctrlIEvent = (KeyEvent) EventParser.readEvent(ctrlIBackend, 0);
        assertThat(ctrlIEvent.code()).isEqualTo(KeyCode.CHAR);
        assertThat(ctrlIEvent.isChar('i')).isTrue();
        assertThat(ctrlIEvent.hasCtrl()).isTrue();
        assertThat(ctrlIEvent.hasShift()).isFalse();

        // Ctrl+Tab: CSI 9;5 u
        QueueBackend ctrlTabBackend = backendFor("\033[9;5u");
        KeyEvent ctrlTabEvent = (KeyEvent) EventParser.readEvent(ctrlTabBackend, 0);
        assertThat(ctrlTabEvent.code()).isEqualTo(KeyCode.TAB);
        assertThat(ctrlTabEvent.hasCtrl()).isTrue();
    }

    @Test
    @DisplayName("Kitty disambiguates Enter from Ctrl+M")
    void kittyDisambiguatesEnterFromCtrlM() throws IOException {
        // Enter key: CSI 13 u
        QueueBackend enterBackend = backendFor("\033[13u");
        KeyEvent enterEvent = (KeyEvent) EventParser.readEvent(enterBackend, 0);
        assertThat(enterEvent.code()).isEqualTo(KeyCode.ENTER);
        assertThat(enterEvent.modifiers()).isEqualTo(KeyModifiers.NONE);

        // Ctrl+M: CSI 109;5 u
        QueueBackend ctrlMBackend = backendFor("\033[109;5u");
        KeyEvent ctrlMEvent = (KeyEvent) EventParser.readEvent(ctrlMBackend, 0);
        assertThat(ctrlMEvent.code()).isEqualTo(KeyCode.CHAR);
        assertThat(ctrlMEvent.isChar('m')).isTrue();
        assertThat(ctrlMEvent.hasCtrl()).isTrue();
    }

    @Test
    @DisplayName("Kitty parses Ctrl+Shift combinations")
    void kittyCtrlShift() throws IOException {
        // Ctrl+Shift+A: CSI 97;6 u (modifier 6 = Shift(1) + Ctrl(4) + 1 = 6)
        QueueBackend backend = backendFor("\033[97;6u");
        KeyEvent event = (KeyEvent) EventParser.readEvent(backend, 0);

        assertThat(event.code()).isEqualTo(KeyCode.CHAR);
        assertThat(event.hasCtrl()).isTrue();
        assertThat(event.hasShift()).isTrue();
        assertThat(event.keyCodePoint()).isEqualTo(97);
        assertThat(event.isChar('a')).isTrue();
        assertThat(event.isCharIgnoreCase('a')).isTrue();
    }

    @Test
    @DisplayName("Kitty parses extended modifiers: Super, Hyper, Meta")
    void kittyExtendedModifiers() throws IOException {
        // Super+A: modifier 8 + 1 = 9
        QueueBackend superBackend = backendFor("\033[97;9u");
        KeyEvent superEvent = (KeyEvent) EventParser.readEvent(superBackend, 0);
        assertThat(superEvent.hasSuper()).isTrue();
        assertThat(superEvent.hasCtrl()).isFalse();

        // Hyper+A: modifier 16 + 1 = 17
        QueueBackend hyperBackend = backendFor("\033[97;17u");
        KeyEvent hyperEvent = (KeyEvent) EventParser.readEvent(hyperBackend, 0);
        assertThat(hyperEvent.hasHyper()).isTrue();

        // Meta+A: modifier 32 + 1 = 33
        QueueBackend metaBackend = backendFor("\033[97;33u");
        KeyEvent metaEvent = (KeyEvent) EventParser.readEvent(metaBackend, 0);
        assertThat(metaEvent.hasMeta()).isTrue();

        // Super+Alt+A: modifier 8 + 2 + 1 = 11
        QueueBackend superAltBackend = backendFor("\033[97;11u");
        KeyEvent superAltEvent = (KeyEvent) EventParser.readEvent(superAltBackend, 0);
        assertThat(superAltEvent.hasSuper()).isTrue();
        assertThat(superAltEvent.hasAlt()).isTrue();
    }

    @Test
    @DisplayName("Kitty parses event kinds: Press, Repeat, Release")
    void kittyEventKinds() throws IOException {
        // Press: :1
        QueueBackend pressBackend = backendFor("\033[97;1:1u");
        KeyEvent press = (KeyEvent) EventParser.readEvent(pressBackend, 0);
        assertThat(press.kind()).isEqualTo(KeyEventKind.PRESS);
        assertThat(press.isPress()).isTrue();

        // Repeat: :2
        QueueBackend repeatBackend = backendFor("\033[97;1:2u");
        KeyEvent repeat = (KeyEvent) EventParser.readEvent(repeatBackend, 0);
        assertThat(repeat.kind()).isEqualTo(KeyEventKind.REPEAT);
        assertThat(repeat.isRepeat()).isTrue();

        // Release: :3
        QueueBackend releaseBackend = backendFor("\033[97;1:3u");
        KeyEvent release = (KeyEvent) EventParser.readEvent(releaseBackend, 0);
        assertThat(release.kind()).isEqualTo(KeyEventKind.RELEASE);
        assertThat(release.isRelease()).isTrue();
    }

    @Test
    @DisplayName("Modified VT arrow key parses event kind")
    void modifiedVtEventKind() throws IOException {
        // Shift+Up Release: ESC[1;2:3A
        QueueBackend backend = backendFor("\033[1;2:3A");
        KeyEvent event = (KeyEvent) EventParser.readEvent(backend, 0);

        assertThat(event.code()).isEqualTo(KeyCode.UP);
        assertThat(event.hasShift()).isTrue();
        assertThat(event.kind()).isEqualTo(KeyEventKind.RELEASE);
        assertThat(event.isRelease()).isTrue();
    }

    @Test
    @DisplayName("Kitty parses alternate shifted and base layout keys")
    void kittyAlternateKeys() throws IOException {
        // Key '=', Shift pressed, shifted key '+' (43): CSI 61:43;2 u
        QueueBackend shiftedBackend = backendFor("\033[61:43;2u");
        KeyEvent shiftedEvent = (KeyEvent) EventParser.readEvent(shiftedBackend, 0);
        assertThat(shiftedEvent.keyCodePoint()).isEqualTo(61);
        assertThat(shiftedEvent.shiftedKeyCodePoint()).hasValue(43);
        assertThat(shiftedEvent.isChar('+')).isTrue();

        // Cyrillic 'с' (1089) with base layout 'c' (99): CSI 1089::99;1 u
        QueueBackend cyrillicBackend = backendFor("\033[1089::99;1u");
        KeyEvent cyrillicEvent = (KeyEvent) EventParser.readEvent(cyrillicBackend, 0);
        assertThat(cyrillicEvent.keyCodePoint()).isEqualTo(1089);
        assertThat(cyrillicEvent.baseLayoutKeyCodePoint()).hasValue(99);
        assertThat(cyrillicEvent.baseLayoutKeyCodePoint().getAsInt()).isEqualTo((int) 'c');
    }

    @Test
    @DisplayName("Kitty parses associated text subfield")
    void kittyAssociatedText() throws IOException {
        // Code 97 ('a'), text 'A' (65): CSI 97;1;65 u
        QueueBackend backend = backendFor("\033[97;1;65u");
        KeyEvent event = (KeyEvent) EventParser.readEvent(backend, 0);

        assertThat(event.code()).isEqualTo(KeyCode.CHAR);
        assertThat(event.text()).isEqualTo("A");
        assertThat(event.string()).isEqualTo("A");
        assertThat(event.keyCodePoint()).isEqualTo(97);
    }

    @Test
    @DisplayName("Kitty parses functional, keypad, and media keys")
    void kittyFunctionalAndMediaKeys() throws IOException {
        // Keypad 5: 57404
        QueueBackend kp5Backend = backendFor("\033[57404u");
        KeyEvent kp5 = (KeyEvent) EventParser.readEvent(kp5Backend, 0);
        assertThat(kp5.code()).isEqualTo(KeyCode.KP_5);
        assertThat(kp5.isKeypad()).isTrue();

        // Keypad Add: 57413
        QueueBackend kpAddBackend = backendFor("\033[57413u");
        KeyEvent kpAdd = (KeyEvent) EventParser.readEvent(kpAddBackend, 0);
        assertThat(kpAdd.code()).isEqualTo(KeyCode.KP_ADD);

        // Keypad Enter: 57414
        QueueBackend kpEnterBackend = backendFor("\033[57414u");
        KeyEvent kpEnter = (KeyEvent) EventParser.readEvent(kpEnterBackend, 0);
        assertThat(kpEnter.code()).isEqualTo(KeyCode.KP_ENTER);

        // F13: 57376
        QueueBackend f13Backend = backendFor("\033[57376u");
        KeyEvent f13 = (KeyEvent) EventParser.readEvent(f13Backend, 0);
        assertThat(f13.code()).isEqualTo(KeyCode.F13);

        // F35: 57398
        QueueBackend f35Backend = backendFor("\033[57398u");
        KeyEvent f35 = (KeyEvent) EventParser.readEvent(f35Backend, 0);
        assertThat(f35.code()).isEqualTo(KeyCode.F35);

        // Lock keys: CapsLock (57358), NumLock (57360)
        QueueBackend capsBackend = backendFor("\033[57358u");
        KeyEvent caps = (KeyEvent) EventParser.readEvent(capsBackend, 0);
        assertThat(caps.code()).isEqualTo(KeyCode.CAPS_LOCK);

        QueueBackend numBackend = backendFor("\033[57360u");
        KeyEvent num = (KeyEvent) EventParser.readEvent(numBackend, 0);
        assertThat(num.code()).isEqualTo(KeyCode.NUM_LOCK);

        // Media key: Play (57428)
        QueueBackend playBackend = backendFor("\033[57428u");
        KeyEvent play = (KeyEvent) EventParser.readEvent(playBackend, 0);
        assertThat(play.code()).isEqualTo(KeyCode.MEDIA_PLAY);
    }

    @Test
    @DisplayName("Atomic CSI parsing consumes unknown sequences without corrupting input stream")
    void atomicCsiParsingConsumesUnknownSequence() throws IOException {
        // An unknown CSI sequence e.g. ESC[999;888z followed immediately by normal key 'a'
        QueueBackend backend = backendFor("\033[999;888za");

        // The unknown CSI sequence should be consumed and ignored (return null), then 'a' is read
        Event first = EventParser.readEvent(backend, 0);
        Event second = EventParser.readEvent(backend, 0);

        assertThat(second).isInstanceOf(KeyEvent.class);
        KeyEvent key = (KeyEvent) second;
        assertThat(key.code()).isEqualTo(KeyCode.CHAR);
        assertThat(key.isChar('a')).isTrue();
    }

    @Test
    @DisplayName("Oversized CSI sequence exceeding 128 chars is bounded safely")
    void oversizedCsiSequenceBoundedSafely() throws IOException {
        // Create a 140 character parameter sequence inside CSI ... u
        StringBuilder sb = new StringBuilder("\033[");
        for (int i = 0; i < 140; i++) {
            sb.append('1');
        }
        sb.append("ux"); // Sequence followed by 'x'
        QueueBackend backend = backendFor(sb.toString());

        // Oversized sequence is consumed/aborted without throwing
        EventParser.readEvent(backend, 0);
        Event event = EventParser.readEvent(backend, 0);

        assertThat(event).isInstanceOf(KeyEvent.class);
        KeyEvent key = (KeyEvent) event;
        assertThat(key.code()).isEqualTo(KeyCode.CHAR);
        assertThat(key.isChar('x')).isTrue();
    }

    private static QueueBackend backendFor(String sequence) {
        return new QueueBackend(sequence.codePoints().toArray());
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
