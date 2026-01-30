/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.terminal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.CellUpdate;
import dev.tamboui.layout.Position;
import dev.tamboui.layout.Size;

import static org.assertj.core.api.Assertions.assertThat;

class BackendTest {

    private MinimalBackend backend;
    private ByteArrayOutputStream output;

    @BeforeEach
    void setUp() {
        output = new ByteArrayOutputStream();
        backend = new MinimalBackend(output);
    }

    @Test
    @DisplayName("insertLines generates correct ANSI sequence")
    void insertLines() throws IOException {
        backend.insertLines(5);
        assertThat(output.toString(StandardCharsets.UTF_8.name())).isEqualTo("\u001b[5L");
    }

    @Test
    @DisplayName("insertLines with n=0 does nothing")
    void insertLinesZero() throws IOException {
        backend.insertLines(0);
        assertThat(output.toString(StandardCharsets.UTF_8.name())).isEmpty();
    }

    @Test
    @DisplayName("insertLines with negative n does nothing")
    void insertLinesNegative() throws IOException {
        backend.insertLines(-1);
        assertThat(output.toString(StandardCharsets.UTF_8.name())).isEmpty();
    }

    @Test
    @DisplayName("deleteLines generates correct ANSI sequence")
    void deleteLines() throws IOException {
        backend.deleteLines(3);
        assertThat(output.toString(StandardCharsets.UTF_8.name())).isEqualTo("\u001b[3M");
    }

    @Test
    @DisplayName("deleteLines with n=0 does nothing")
    void deleteLinesZero() throws IOException {
        backend.deleteLines(0);
        assertThat(output.toString(StandardCharsets.UTF_8.name())).isEmpty();
    }

    @Test
    @DisplayName("moveCursorUp generates correct ANSI sequence")
    void moveCursorUp() throws IOException {
        backend.moveCursorUp(4);
        assertThat(output.toString(StandardCharsets.UTF_8.name())).isEqualTo("\u001b[4A");
    }

    @Test
    @DisplayName("moveCursorDown generates correct ANSI sequence")
    void moveCursorDown() throws IOException {
        backend.moveCursorDown(2);
        assertThat(output.toString(StandardCharsets.UTF_8.name())).isEqualTo("\u001b[2B");
    }

    @Test
    @DisplayName("moveCursorRight generates correct ANSI sequence")
    void moveCursorRight() throws IOException {
        backend.moveCursorRight(10);
        assertThat(output.toString(StandardCharsets.UTF_8.name())).isEqualTo("\u001b[10C");
    }

    @Test
    @DisplayName("moveCursorLeft generates correct ANSI sequence")
    void moveCursorLeft() throws IOException {
        backend.moveCursorLeft(7);
        assertThat(output.toString(StandardCharsets.UTF_8.name())).isEqualTo("\u001b[7D");
    }

    @Test
    @DisplayName("eraseToEndOfLine generates correct ANSI sequence")
    void eraseToEndOfLine() throws IOException {
        backend.eraseToEndOfLine();
        assertThat(output.toString(StandardCharsets.UTF_8.name())).isEqualTo("\u001b[K");
    }

    @Test
    @DisplayName("carriageReturn generates correct sequence")
    void carriageReturn() throws IOException {
        backend.carriageReturn();
        assertThat(output.toString(StandardCharsets.UTF_8.name())).isEqualTo("\r");
    }

    @Test
    @DisplayName("multiple operations generate correct sequence")
    void multipleOperations() throws IOException {
        backend.moveCursorUp(3);
        backend.insertLines(2);
        backend.moveCursorDown(1);
        backend.eraseToEndOfLine();

        String expected = "\u001b[3A" + // Move up 3
                "\u001b[2L" + // Insert 2 lines
                "\u001b[1B" + // Move down 1
                "\u001b[K"; // Erase to EOL

        assertThat(output.toString(StandardCharsets.UTF_8.name())).isEqualTo(expected);
    }

    /**
     * Minimal Backend implementation for testing default methods.
     */
    private static class MinimalBackend implements Backend {
        private final ByteArrayOutputStream output;

        MinimalBackend(ByteArrayOutputStream output) {
            this.output = output;
        }

        @Override
        public void writeRaw(byte[] data) throws IOException {
            output.write(data);
        }

        @Override
        public void draw(Iterable<CellUpdate> updates) throws IOException {
            // Not used in these tests
        }

        @Override
        public void flush() throws IOException {
            // Not used in these tests
        }

        @Override
        public void clear() throws IOException {
            // Not used in these tests
        }

        @Override
        public Size size() throws IOException {
            return new Size(80, 24);
        }

        @Override
        public void showCursor() throws IOException {
            // Not used in these tests
        }

        @Override
        public void hideCursor() throws IOException {
            // Not used in these tests
        }

        @Override
        public Position getCursorPosition() throws IOException {
            return new Position(0, 0);
        }

        @Override
        public void setCursorPosition(Position position) throws IOException {
            // Not used in these tests
        }

        @Override
        public void enterAlternateScreen() throws IOException {
            // Not used in these tests
        }

        @Override
        public void leaveAlternateScreen() throws IOException {
            // Not used in these tests
        }

        @Override
        public void enableRawMode() throws IOException {
            // Not used in these tests
        }

        @Override
        public void disableRawMode() throws IOException {
            // Not used in these tests
        }

        @Override
        public void onResize(Runnable handler) {
            // Not used in these tests
        }

        @Override
        public int read(int timeoutMs) throws IOException {
            return -1;
        }

        @Override
        public int peek(int timeoutMs) throws IOException {
            return -1;
        }

        @Override
        public void close() throws IOException {
            // Not used in these tests
        }
    }
}
