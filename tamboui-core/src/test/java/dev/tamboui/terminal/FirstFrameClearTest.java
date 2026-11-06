/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.terminal;

import java.io.IOException;
import java.util.ArrayList;
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
 * Tests that {@link Terminal} clears the screen once before the first frame (issue #597). Without
 * it, the first frame is diffed against an empty buffer, so cells that stay blank are never written,
 * and a terminal that keeps its contents when entering the alternate screen shows the old output
 * through those blank cells.
 */
class FirstFrameClearTest {

    @Test
    @DisplayName("the first frame clears the screen, flushed before the frame is drawn")
    void firstFrameClears() {
        RecordingBackend backend = new RecordingBackend(10, 5);
        Terminal<RecordingBackend> terminal = new Terminal<>(backend);

        terminal.draw(frame -> frame.renderWidget(fill('x'), Rect.of(10, 5)));

        List<String> calls = backend.calls();
        assertThat(calls).startsWith("clear", "flush");
        assertThat(calls.indexOf("clear")).isLessThan(calls.indexOf("draw"));
        assertThat(frequency(calls, "clear")).isEqualTo(1);
    }

    @Test
    @DisplayName("a blank first frame still clears and flushes (the actual bug)")
    void blankFirstFrameStillClears() {
        RecordingBackend backend = new RecordingBackend(10, 5);
        Terminal<RecordingBackend> terminal = new Terminal<>(backend);

        // Nothing is rendered: the diff against the empty buffer is empty, so without the initial
        // clear nothing at all would reach the terminal and old content would show through.
        terminal.draw(frame -> {
        });

        assertThat(backend.calls()).contains("clear", "flush");
        assertThat(frequency(backend.calls(), "clear")).isEqualTo(1);
    }

    @Test
    @DisplayName("later frames do not clear again")
    void subsequentFramesDoNotClear() {
        RecordingBackend backend = new RecordingBackend(10, 5);
        Terminal<RecordingBackend> terminal = new Terminal<>(backend);

        terminal.draw(frame -> frame.renderWidget(fill('x'), Rect.of(10, 5)));
        backend.clearCalls();

        terminal.draw(frame -> frame.renderWidget(fill('y'), Rect.of(10, 5)));

        assertThat(backend.calls()).doesNotContain("clear");
    }

    private static int frequency(List<String> calls, String value) {
        return (int) calls.stream().filter(value::equals).count();
    }

    private static Widget fill(char c) {
        return (area, buffer) -> {
            for (int y = area.y(); y < area.y() + area.height(); y++) {
                for (int x = area.x(); x < area.x() + area.width(); x++) {
                    buffer.setString(x, y, String.valueOf(c), Style.EMPTY);
                }
            }
        };
    }

    /** A minimal backend recording the order of clear/draw/flush calls. */
    private static final class RecordingBackend implements Backend {

        private final int width;
        private final int height;
        private final List<String> calls = new ArrayList<>();

        RecordingBackend(int width, int height) {
            this.width = width;
            this.height = height;
        }

        List<String> calls() {
            return calls;
        }

        void clearCalls() {
            calls.clear();
        }

        @Override
        public void clear() throws IOException {
            calls.add("clear");
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
        public void beginSynchronizedUpdate() throws IOException {
            calls.add("beginSynchronizedUpdate");
        }

        @Override
        public void endSynchronizedUpdate() throws IOException {
            calls.add("endSynchronizedUpdate");
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
