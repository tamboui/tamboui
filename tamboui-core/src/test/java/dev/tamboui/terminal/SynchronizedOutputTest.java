/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.terminal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.DiffResult;
import dev.tamboui.layout.Position;
import dev.tamboui.layout.Rect;
import dev.tamboui.layout.Size;
import dev.tamboui.style.Style;
import dev.tamboui.widget.Widget;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that Terminal.draw() wraps rendering with Mode 2026
 * synchronized output (BSU/ESU) and flushes in the correct order.
 */
class SynchronizedOutputTest {

    /** Simple widget that writes a character at (0,0). */
    private static final Widget SIMPLE_WIDGET = (area, buf) ->
            buf.setString(0, 0, "A", Style.EMPTY);

    @Test
    @DisplayName("draw() wraps rendering with BSU/ESU in correct order")
    void drawWrapsWithSynchronizedUpdate() {
        RecordingBackend backend = new RecordingBackend(10, 5);
        Terminal<RecordingBackend> terminal = new Terminal<>(backend);

        terminal.draw(frame ->
                frame.renderWidget(SIMPLE_WIDGET, Rect.of(10, 5)));

        List<String> calls = backend.calls();
        assertThat(calls).containsExactly(
                "beginSynchronizedUpdate",
                "draw",
                "hideCursor", // no cursor set on the frame -> hidden on first draw
                "endSynchronizedUpdate",
                "flush");
    }

    @Test
    @DisplayName("draw() writes nothing at all when nothing changed")
    void drawSkipsAllOutputWhenNothingChanged() {
        RecordingBackend backend = new RecordingBackend(10, 5);
        Terminal<RecordingBackend> terminal = new Terminal<>(backend);

        // First draw to establish the buffer
        terminal.draw(frame -> {});
        backend.clearCalls();

        // Second draw with same content — zero bytes must reach the terminal:
        // some terminals (e.g. Ghostty) reset the cursor blink timer on ANY
        // output, so even an empty BSU/ESU pair keeps the cursor from blinking.
        terminal.draw(frame -> {});

        assertThat(backend.calls()).isEmpty();
    }

    @Test
    @DisplayName("unchanged frame with unchanged cursor position writes nothing")
    void unchangedCursorWritesNothing() {
        RecordingBackend backend = new RecordingBackend(10, 5);
        Terminal<RecordingBackend> terminal = new Terminal<>(backend);

        terminal.draw(frame -> frame.setCursorPosition(new Position(3, 0)));
        backend.clearCalls();

        terminal.draw(frame -> frame.setCursorPosition(new Position(3, 0)));

        assertThat(backend.calls()).isEmpty();
    }

    @Test
    @DisplayName("moved cursor on an unchanged frame is repositioned inside BSU/ESU")
    void movedCursorIsWritten() {
        RecordingBackend backend = new RecordingBackend(10, 5);
        Terminal<RecordingBackend> terminal = new Terminal<>(backend);

        terminal.draw(frame -> frame.setCursorPosition(new Position(3, 0)));
        backend.clearCalls();

        terminal.draw(frame -> frame.setCursorPosition(new Position(4, 0)));

        assertThat(backend.calls()).containsExactly(
                "beginSynchronizedUpdate",
                "setCursorPosition",
                "endSynchronizedUpdate",
                "flush");
    }

    @Test
    @DisplayName("hiding the cursor on an unchanged frame still writes")
    void cursorHideIsWritten() {
        RecordingBackend backend = new RecordingBackend(10, 5);
        Terminal<RecordingBackend> terminal = new Terminal<>(backend);

        terminal.draw(frame -> frame.setCursorPosition(new Position(3, 0)));
        backend.clearCalls();

        // No cursor set this frame -> cursor must be hidden -> output required
        terminal.draw(frame -> {});

        assertThat(backend.calls()).contains("hideCursor", "flush");
    }

    @Test
    @DisplayName("endSynchronizedUpdate is always before flush")
    void endSyncAlwaysBeforeFlush() {
        RecordingBackend backend = new RecordingBackend(10, 5);
        Terminal<RecordingBackend> terminal = new Terminal<>(backend);

        terminal.draw(frame ->
                frame.renderWidget(SIMPLE_WIDGET, Rect.of(10, 5)));

        List<String> calls = backend.calls();
        int esuIndex = calls.indexOf("endSynchronizedUpdate");
        int flushIndex = calls.indexOf("flush");
        assertThat(esuIndex).isGreaterThanOrEqualTo(0);
        assertThat(flushIndex).isGreaterThan(esuIndex);
    }

    @Test
    @DisplayName("beginSynchronizedUpdate is always before draw")
    void beginSyncAlwaysBeforeDraw() {
        RecordingBackend backend = new RecordingBackend(10, 5);
        Terminal<RecordingBackend> terminal = new Terminal<>(backend);

        terminal.draw(frame ->
                frame.renderWidget(SIMPLE_WIDGET, Rect.of(10, 5)));

        List<String> calls = backend.calls();
        int bsuIndex = calls.indexOf("beginSynchronizedUpdate");
        int drawIndex = calls.indexOf("draw");
        assertThat(bsuIndex).isGreaterThanOrEqualTo(0);
        assertThat(drawIndex).isGreaterThan(bsuIndex);
    }

    /**
     * A minimal backend that records the order of synchronized output
     * and flush calls for assertion.
     */
    private static class RecordingBackend implements Backend {

        private final int width;
        private final int height;
        private final List<String> calls = new ArrayList<>();

        RecordingBackend(int width, int height) {
            this.width = width;
            this.height = height;
        }

        List<String> calls() {
            return Collections.unmodifiableList(calls);
        }

        void clearCalls() {
            calls.clear();
        }

        @Override
        public void beginSynchronizedUpdate() throws IOException {
            calls.add("beginSynchronizedUpdate");
        }

        @Override
        public void endSynchronizedUpdate() throws IOException {
            calls.add("endSynchronizedUpdate");
        }

        @Override
        public void draw(DiffResult diff) throws IOException {
            calls.add("draw");
        }

        @Override
        public void flush() throws IOException {
            calls.add("flush");
        }

        @Override
        public void clear() throws IOException {
        }

        @Override
        public Size size() throws IOException {
            return new Size(width, height);
        }

        @Override
        public void showCursor() throws IOException {
            calls.add("showCursor");
        }

        @Override
        public void hideCursor() throws IOException {
            calls.add("hideCursor");
        }

        @Override
        public Position getCursorPosition() throws IOException {
            return Position.ORIGIN;
        }

        @Override
        public void setCursorPosition(Position position) throws IOException {
            calls.add("setCursorPosition");
        }

        @Override
        public void enterAlternateScreen() throws IOException {
        }

        @Override
        public void leaveAlternateScreen() throws IOException {
        }

        @Override
        public void enableRawMode() throws IOException {
        }

        @Override
        public void disableRawMode() throws IOException {
        }

        @Override
        public void onResize(Runnable handler) {
        }

        @Override
        public int read(int timeoutMs) throws IOException {
            return -2;
        }

        @Override
        public int peek(int timeoutMs) throws IOException {
            return -2;
        }

        @Override
        public void close() throws IOException {
        }
    }
}
