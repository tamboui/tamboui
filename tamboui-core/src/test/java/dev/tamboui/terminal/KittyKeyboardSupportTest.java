/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.terminal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.Queue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.DiffResult;
import dev.tamboui.layout.Position;
import dev.tamboui.layout.Size;

import static org.assertj.core.api.Assertions.assertThat;

class KittyKeyboardSupportTest {

    @Nested
    @DisplayName("Escape sequences")
    class EscapeSequenceTests {

        @Test
        @DisplayName("query sequence is CSI ? u")
        void querySequence() {
            assertThat(KittyKeyboardSupport.querySequence()).isEqualTo("\033[?u");
        }

        @Test
        @DisplayName("push sequence formats flags correctly")
        void pushSequence() {
            assertThat(KittyKeyboardSupport.pushSequence(5)).isEqualTo("\033[>5u");
            assertThat(KittyKeyboardSupport.pushSequence(31)).isEqualTo("\033[>31u");
        }

        @Test
        @DisplayName("pop sequence formats single pop correctly")
        void popSequence() {
            assertThat(KittyKeyboardSupport.popSequence()).isEqualTo("\033[<u");
        }

        @Test
        @DisplayName("pop sequence formats count pop correctly")
        void popSequenceWithCount() {
            assertThat(KittyKeyboardSupport.popSequence(2)).isEqualTo("\033[<2u");
        }
    }

    @Nested
    @DisplayName("Push and pop operations")
    class PushPopTests {

        private ByteArrayOutputStream output;
        private MockBackend backend;

        @BeforeEach
        void setUp() {
            output = new ByteArrayOutputStream();
            backend = new MockBackend(output);
        }

        @Test
        @DisplayName("push writes sequence and flushes")
        void pushWritesAndFlushes() throws IOException {
            KittyKeyboardSupport.push(backend, KittyKeyboardSupport.AUTO_FLAGS);

            String written = new String(output.toByteArray(), StandardCharsets.UTF_8);
            assertThat(written).isEqualTo("\033[>5u");
            assertThat(backend.flushCount).isEqualTo(1);
        }

        @Test
        @DisplayName("pop writes sequence and flushes")
        void popWritesAndFlushes() throws IOException {
            KittyKeyboardSupport.pop(backend);

            String written = new String(output.toByteArray(), StandardCharsets.UTF_8);
            assertThat(written).isEqualTo("\033[<u");
            assertThat(backend.flushCount).isEqualTo(1);
        }

        @Test
        @DisplayName("pop with count writes sequence and flushes")
        void popCountWritesAndFlushes() throws IOException {
            KittyKeyboardSupport.pop(backend, 3);

            String written = new String(output.toByteArray(), StandardCharsets.UTF_8);
            assertThat(written).isEqualTo("\033[<3u");
            assertThat(backend.flushCount).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Query and response parsing")
    class QueryResponseTests {

        private ByteArrayOutputStream output;
        private MockBackend backend;

        @BeforeEach
        void setUp() {
            output = new ByteArrayOutputStream();
            backend = new MockBackend(output);
        }

        @Test
        @DisplayName("valid response returns reported flags")
        void parsesValidResponse() throws IOException {
            backend.setResponseBytes("\033[?31u");

            int flags = KittyKeyboardSupport.query(backend, 100);

            assertThat(flags).isEqualTo(31);
            assertThat(backend.flushCount).isEqualTo(1);
            String written = new String(output.toByteArray(), StandardCharsets.UTF_8);
            assertThat(written).isEqualTo("\033[?u");
        }

        @Test
        @DisplayName("timeout returns -1")
        void timeoutReturnsMinusOne() throws IOException {
            int flags = KittyKeyboardSupport.query(backend, 50);

            assertThat(flags).isEqualTo(-1);
        }

        @Test
        @DisplayName("unexpected response returns -1")
        void unexpectedResponseReturnsMinusOne() throws IOException {
            backend.setResponseBytes("\033[?2027;1$y"); // Mode 2027 response, not kitty

            int flags = KittyKeyboardSupport.query(backend, 100);

            assertThat(flags).isEqualTo(-1);
        }
    }

    private static class MockBackend implements Backend {
        private final ByteArrayOutputStream output;
        private final Queue<Integer> responseQueue = new LinkedList<>();
        int flushCount = 0;

        MockBackend(ByteArrayOutputStream output) {
            this.output = output;
        }

        void setResponseBytes(String response) {
            for (char c : response.toCharArray()) {
                responseQueue.add((int) c);
            }
        }

        @Override
        public void writeRaw(byte[] data) throws IOException {
            output.write(data);
        }

        @Override
        public void flush() throws IOException {
            flushCount++;
        }

        @Override
        public int read(int timeoutMs) throws IOException {
            if (responseQueue.isEmpty()) {
                return -2;
            }
            return responseQueue.poll();
        }

        @Override
        public int peek(int timeoutMs) throws IOException {
            if (responseQueue.isEmpty()) {
                return -2;
            }
            return responseQueue.peek();
        }

        @Override
        public void draw(DiffResult diff) throws IOException {
        }

        @Override
        public void clear() throws IOException {
        }

        @Override
        public Size size() throws IOException {
            return new Size(80, 24);
        }

        @Override
        public Position getCursorPosition() throws IOException {
            return Position.ORIGIN;
        }

        @Override
        public void setCursorPosition(Position position) throws IOException {
        }

        @Override
        public void onResize(Runnable handler) {
        }

        @Override
        public void showCursor() throws IOException {
        }

        @Override
        public void hideCursor() throws IOException {
        }

        @Override
        public void enableRawMode() throws IOException {
        }

        @Override
        public void disableRawMode() throws IOException {
        }

        @Override
        public void enterAlternateScreen() throws IOException {
        }

        @Override
        public void leaveAlternateScreen() throws IOException {
        }

        @Override
        public void enableMouseCapture(boolean motion) throws IOException {
        }

        @Override
        public void disableMouseCapture() throws IOException {
        }

        @Override
        public void enableBracketedPaste() throws IOException {
        }

        @Override
        public void disableBracketedPaste() throws IOException {
        }

        @Override
        public void close() throws IOException {
        }
    }
}
