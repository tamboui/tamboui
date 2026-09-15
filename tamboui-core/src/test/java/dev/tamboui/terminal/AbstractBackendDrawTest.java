/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.terminal;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;
import dev.tamboui.buffer.DiffResult;
import dev.tamboui.layout.Position;
import dev.tamboui.layout.Rect;
import dev.tamboui.layout.Size;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that drawing a diff as runs of adjacent cells emits exactly the same bytes as
 * visiting the changed cells one at a time and moving the cursor whenever the next cell is
 * not horizontally adjacent.
 * <p>
 * {@link #drawCellByCell} is the reference: the per-cell algorithm the backend used before
 * the diff became run-based. Every scenario asserts byte equality against it, so a change to
 * the run loop that would alter what the terminal displays fails here.
 */
class AbstractBackendDrawTest {

    @Test
    @DisplayName("single changed cell")
    void singleCell() throws IOException {
        Buffer previous = Buffer.empty(Rect.of(10, 3));
        Buffer next = Buffer.empty(Rect.of(10, 3));
        next.setString(4, 1, "X", Style.EMPTY);
        assertDrawMatchesCellByCell(previous, next);
    }

    @Test
    @DisplayName("run of adjacent cells on one row")
    void adjacentRun() throws IOException {
        Buffer previous = Buffer.empty(Rect.of(20, 3));
        Buffer next = Buffer.empty(Rect.of(20, 3));
        next.setString(2, 1, "hello world", Style.EMPTY);
        assertDrawMatchesCellByCell(previous, next);
    }

    @Test
    @DisplayName("two separate runs on the same row")
    void twoRunsSameRow() throws IOException {
        Buffer previous = Buffer.empty(Rect.of(20, 3));
        Buffer next = Buffer.empty(Rect.of(20, 3));
        next.setString(1, 1, "ab", Style.EMPTY);
        next.setString(10, 1, "cd", Style.EMPTY);
        assertDrawMatchesCellByCell(previous, next);
    }

    @Test
    @DisplayName("change at the last column of a row and the first of the next")
    void rowBoundary() throws IOException {
        Buffer previous = Buffer.empty(Rect.of(8, 3));
        Buffer next = Buffer.empty(Rect.of(8, 3));
        next.setString(7, 0, "A", Style.EMPTY);
        next.setString(0, 1, "B", Style.EMPTY);
        assertDrawMatchesCellByCell(previous, next);
    }

    @Test
    @DisplayName("full redraw")
    void fullRedraw() throws IOException {
        Buffer previous = Buffer.empty(Rect.of(16, 4));
        Buffer next = Buffer.empty(Rect.of(16, 4));
        for (int y = 0; y < 4; y++) {
            StringBuilder row = new StringBuilder();
            for (int x = 0; x < 16; x++) {
                row.append((char) ('a' + ((x + y) % 26)));
            }
            next.setString(0, y, row.toString(), Style.EMPTY);
        }
        assertDrawMatchesCellByCell(previous, next);
    }

    @Test
    @DisplayName("scattered single-cell changes")
    void scattered() throws IOException {
        Buffer previous = Buffer.empty(Rect.of(20, 5));
        Buffer next = Buffer.empty(Rect.of(20, 5));
        for (int y = 0; y < 5; y++) {
            previous.setString(0, y, "....................", Style.EMPTY);
            next.setString(0, y, "....................", Style.EMPTY);
        }
        next.setString(3, 0, "X", Style.EMPTY.fg(Color.RED));
        next.setString(11, 2, "Y", Style.EMPTY.fg(Color.GREEN));
        next.setString(19, 4, "Z", Style.EMPTY);
        assertDrawMatchesCellByCell(previous, next);
    }

    @Test
    @DisplayName("wide CJK character drawn over narrow cells")
    void wideOverNarrow() throws IOException {
        Buffer previous = Buffer.empty(Rect.of(12, 2));
        previous.setString(0, 0, "abcdefgh", Style.EMPTY);
        Buffer next = Buffer.empty(Rect.of(12, 2));
        next.setString(0, 0, "ab世界gh", Style.EMPTY);
        assertDrawMatchesCellByCell(previous, next);
    }

    @Test
    @DisplayName("narrow characters drawn over a wide character")
    void narrowOverWide() throws IOException {
        Buffer previous = Buffer.empty(Rect.of(12, 2));
        previous.setString(0, 0, "ab世界gh", Style.EMPTY);
        Buffer next = Buffer.empty(Rect.of(12, 2));
        next.setString(0, 0, "abcdefgh", Style.EMPTY);
        assertDrawMatchesCellByCell(previous, next);
    }

    @Test
    @DisplayName("only the second half of a wide character is dirty")
    void continuationOnlyChange() throws IOException {
        Buffer previous = Buffer.empty(Rect.of(12, 2));
        previous.setString(0, 0, "世", Style.EMPTY);
        previous.set(1, 0, new Cell("?", Style.EMPTY));
        Buffer next = Buffer.empty(Rect.of(12, 2));
        next.setString(0, 0, "世", Style.EMPTY);
        assertDrawMatchesCellByCell(previous, next);
    }

    @Test
    @DisplayName("emoji with zero-width joiner")
    void zwjEmoji() throws IOException {
        Buffer previous = Buffer.empty(Rect.of(12, 2));
        previous.setString(0, 0, "abcd", Style.EMPTY);
        Buffer next = Buffer.empty(Rect.of(12, 2));
        next.setString(0, 0, "a👨‍🦲d", Style.EMPTY);
        assertDrawMatchesCellByCell(previous, next);
    }

    @Test
    @DisplayName("style changes inside a run")
    void styleChangeInsideRun() throws IOException {
        Buffer previous = Buffer.empty(Rect.of(16, 2));
        Buffer next = Buffer.empty(Rect.of(16, 2));
        next.setString(0, 0, "red", Style.EMPTY.fg(Color.RED));
        next.setString(3, 0, "green", Style.EMPTY.fg(Color.GREEN).bold());
        next.setString(8, 0, "plain", Style.EMPTY);
        assertDrawMatchesCellByCell(previous, next);
    }

    @Test
    @DisplayName("hyperlink style inside a run")
    void hyperlinkInsideRun() throws IOException {
        Buffer previous = Buffer.empty(Rect.of(16, 2));
        Buffer next = Buffer.empty(Rect.of(16, 2));
        next.setString(0, 0, "ab", Style.EMPTY);
        next.setString(2, 0, "link", Style.EMPTY.hyperlink("https://example.com"));
        next.setString(6, 0, "cd", Style.EMPTY);
        assertDrawMatchesCellByCell(previous, next);
    }

    @Test
    @DisplayName("area change redraws every cell")
    void areaChange() throws IOException {
        Buffer previous = Buffer.empty(Rect.of(6, 2));
        previous.setString(0, 0, "abcdef", Style.EMPTY);
        Buffer next = Buffer.empty(Rect.of(10, 3));
        next.setString(0, 0, "hello", Style.EMPTY);
        next.setString(0, 2, "世界", Style.EMPTY);
        assertDrawMatchesCellByCell(previous, next);
    }

    @Test
    @DisplayName("run ending at the last column")
    void runToLastColumn() throws IOException {
        Buffer previous = Buffer.empty(Rect.of(6, 2));
        Buffer next = Buffer.empty(Rect.of(6, 2));
        next.setString(0, 0, "abcdef", Style.EMPTY);
        next.setString(0, 1, "ghijkl", Style.EMPTY);
        assertDrawMatchesCellByCell(previous, next);
    }

    @Test
    @DisplayName("area change to a zero-width buffer produces no runs")
    void zeroWidthAreaChange() throws IOException {
        Buffer previous = Buffer.empty(Rect.of(3, 4));
        Buffer next = Buffer.empty(Rect.of(0, 4));

        DiffResult diff = new DiffResult(16);
        previous.diff(next, diff);

        // A zero-length run would make xOf()/yOf() divide by the zero width.
        assertThat(diff.runCount()).isZero();
        assertThat(diff.isEmpty()).isTrue();

        // Drawing it must not position the cursor anywhere (the writer still emits its reset).
        assertDrawMatchesCellByCell(previous, next);
    }

    private void assertDrawMatchesCellByCell(Buffer previous, Buffer next) throws IOException {
        DiffResult diff = new DiffResult(next.area().area());
        previous.diff(next, diff);

        CapturingBackend backend = new CapturingBackend();
        backend.draw(diff);

        StringBuilder expected = new StringBuilder();
        drawCellByCell(diff, expected);

        assertThat(backend.output.toString())
                .as("run-based draw must emit the same bytes as the per-cell draw")
                .isEqualTo(expected.toString());
    }

    /**
     * The per-cell draw algorithm the backend used before the diff became run-based: visit
     * every changed cell in row-major order, tracking the cursor so a move is only emitted
     * when the next cell is not where the cursor already is.
     *
     * @param diff the diff to draw
     * @param out the sink for the emitted bytes
     */
    private void drawCellByCell(DiffResult diff, StringBuilder out) {
        try (AnsiCellWriter cellWriter = new AnsiCellWriter(out::append)) {
            int cursorX = -1;
            int cursorY = -1;
            for (int i = 0; i < diff.size(); i++) {
                Cell cell = diff.getCell(i);
                if (cell.isContinuation()) {
                    cursorX++;
                    continue;
                }
                int x = diff.getX(i);
                int y = diff.getY(i);
                if (x != cursorX || y != cursorY) {
                    out.append("\u001b[").append(y + 1).append(';').append(x + 1).append('H');
                }
                cellWriter.writeCell(cell);
                cursorX = x + 1;
                cursorY = y;
            }
        }
    }

    private static final class CapturingBackend extends AbstractBackend {

        private final StringBuilder output = new StringBuilder();

        @Override
        public void writeRaw(byte[] data) {
            output.append(new String(data, StandardCharsets.UTF_8));
        }

        @Override
        public void flush() {
        }

        @Override
        public void clear() {
        }

        @Override
        public Size size() {
            return new Size(80, 24);
        }

        @Override
        public void showCursor() {
        }

        @Override
        public void hideCursor() {
        }

        @Override
        public Position getCursorPosition() {
            return new Position(0, 0);
        }

        @Override
        public void enterAlternateScreen() {
        }

        @Override
        public void leaveAlternateScreen() {
        }

        @Override
        public void enableRawMode() {
        }

        @Override
        public void disableRawMode() {
        }

        @Override
        public int read(int timeoutMs) {
            return -1;
        }

        @Override
        public int peek(int timeoutMs) {
            return -1;
        }

        @Override
        public void onResize(Runnable handler) {
        }

        @Override
        public void close() {
        }
    }
}
