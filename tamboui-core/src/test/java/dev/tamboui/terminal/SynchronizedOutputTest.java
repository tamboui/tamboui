/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.terminal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.DiffResult;
import dev.tamboui.layout.Position;
import dev.tamboui.layout.Size;
import dev.tamboui.style.Style;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that Terminal.draw() wraps rendering with Mode 2026
 * synchronized output (BSU/ESU) and flushes in the correct order.
 */
class SynchronizedOutputTest {

    @Test
    void drawEmitsBeginBeforeDrawAndEndBeforeFlush() {
        RecordingBackend backend = new RecordingBackend(40, 10);
        Terminal<RecordingBackend> terminal = new Terminal<>(backend);

        terminal.draw(frame -> {
            frame.buffer().setString(0, 0, "Hello", Style.EMPTY);
        });

        List<String> calls = backend.calls();

        // BSU + draw + ESU are written to the buffer, then flushed atomically
        assertThat(calls).containsSubsequence(
                "beginSynchronizedUpdate",
                "draw",
                "endSynchronizedUpdate",
                "flush");
    }

    @Test
    void drawEmitsBeginAndEndEvenWithNoDiff() {
        RecordingBackend backend = new RecordingBackend(40, 10);
        Terminal<RecordingBackend> terminal = new Terminal<>(backend);

        // First draw to populate the buffer
        terminal.draw(frame -> {
            frame.buffer().setString(0, 0, "Static", Style.EMPTY);
        });

        backend.clearCalls();

        // Second draw with same content — no diff, but BSU/ESU should still be emitted
        terminal.draw(frame -> {
            frame.buffer().setString(0, 0, "Static", Style.EMPTY);
        });

        List<String> calls = backend.calls();

        assertThat(calls).containsSubsequence(
                "beginSynchronizedUpdate",
                "endSynchronizedUpdate",
                "flush");
        // No "draw" call since there's no diff
        assertThat(calls).doesNotContain("draw");
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
        }

        @Override
        public void hideCursor() throws IOException {
        }

        @Override
        public Position getCursorPosition() throws IOException {
            return Position.ORIGIN;
        }

        @Override
        public void setCursorPosition(Position position) throws IOException {
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
