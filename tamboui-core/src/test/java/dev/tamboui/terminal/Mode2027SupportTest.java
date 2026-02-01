/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.terminal;

import dev.tamboui.buffer.CellUpdate;
import dev.tamboui.layout.Position;
import dev.tamboui.layout.Size;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.Queue;

import static org.assertj.core.api.Assertions.assertThat;

class Mode2027SupportTest {

    @Nested
    @DisplayName("Mode2027Status")
    class Mode2027StatusTests {

        @Test
        @DisplayName("NOT_SUPPORTED is not supported")
        void notSupportedIsNotSupported() {
            assertThat(Mode2027Status.NOT_SUPPORTED.isSupported()).isFalse();
        }

        @Test
        @DisplayName("SUPPORTED_DISABLED is supported")
        void supportedDisabledIsSupported() {
            assertThat(Mode2027Status.SUPPORTED_DISABLED.isSupported()).isTrue();
        }

        @Test
        @DisplayName("ENABLED is supported")
        void enabledIsSupported() {
            assertThat(Mode2027Status.ENABLED.isSupported()).isTrue();
        }
    }

    @Nested
    @DisplayName("Escape sequences")
    class EscapeSequenceTests {

        @Test
        @DisplayName("query sequence is CSI ? 2027 $ p")
        void querySequence() {
            assertThat(Mode2027Support.querySequence()).isEqualTo("\033[?2027$p");
        }

        @Test
        @DisplayName("enable sequence is CSI ? 2027 h")
        void enableSequence() {
            assertThat(Mode2027Support.enableSequence()).isEqualTo("\033[?2027h");
        }

        @Test
        @DisplayName("disable sequence is CSI ? 2027 l")
        void disableSequence() {
            assertThat(Mode2027Support.disableSequence()).isEqualTo("\033[?2027l");
        }
    }

    @Nested
    @DisplayName("enable and disable operations")
    class EnableDisableTests {

        private MockBackend backend;
        private ByteArrayOutputStream output;

        @BeforeEach
        void setUp() {
            output = new ByteArrayOutputStream();
            backend = new MockBackend(output);
        }

        @Test
        @DisplayName("enable sends CSI ? 2027 h and flushes")
        void enableSendsCorrectSequence() throws IOException {
            Mode2027Support.enable(backend);
            assertThat(output.toString(StandardCharsets.UTF_8.name())).isEqualTo("\033[?2027h");
            assertThat(backend.flushCount).isEqualTo(1);
        }

        @Test
        @DisplayName("disable sends CSI ? 2027 l and flushes")
        void disableSendsCorrectSequence() throws IOException {
            Mode2027Support.disable(backend);
            assertThat(output.toString(StandardCharsets.UTF_8.name())).isEqualTo("\033[?2027l");
            assertThat(backend.flushCount).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Response parsing")
    class ResponseParsingTests {

        private MockBackend backend;
        private ByteArrayOutputStream output;

        @BeforeEach
        void setUp() {
            output = new ByteArrayOutputStream();
            backend = new MockBackend(output);
        }

        @Test
        @DisplayName("valid enabled response (Ps=1) returns ENABLED")
        void validEnabledResponsePs1() throws IOException {
            // Response: CSI ? 2027 ; 1 $ y
            backend.setResponseBytes("\033[?2027;1$y");
            Mode2027Status status = Mode2027Support.query(backend, 100);
            assertThat(status).isEqualTo(Mode2027Status.ENABLED);
        }

        @Test
        @DisplayName("valid enabled response (Ps=3) returns ENABLED")
        void validEnabledResponsePs3() throws IOException {
            // Response: CSI ? 2027 ; 3 $ y (temporarily set)
            backend.setResponseBytes("\033[?2027;3$y");
            Mode2027Status status = Mode2027Support.query(backend, 100);
            assertThat(status).isEqualTo(Mode2027Status.ENABLED);
        }

        @Test
        @DisplayName("valid disabled response (Ps=2) returns SUPPORTED_DISABLED")
        void validDisabledResponsePs2() throws IOException {
            // Response: CSI ? 2027 ; 2 $ y
            backend.setResponseBytes("\033[?2027;2$y");
            Mode2027Status status = Mode2027Support.query(backend, 100);
            assertThat(status).isEqualTo(Mode2027Status.SUPPORTED_DISABLED);
        }

        @Test
        @DisplayName("valid disabled response (Ps=4) returns SUPPORTED_DISABLED")
        void validDisabledResponsePs4() throws IOException {
            // Response: CSI ? 2027 ; 4 $ y (temporarily reset)
            backend.setResponseBytes("\033[?2027;4$y");
            Mode2027Status status = Mode2027Support.query(backend, 100);
            assertThat(status).isEqualTo(Mode2027Status.SUPPORTED_DISABLED);
        }

        @Test
        @DisplayName("unrecognized mode response (Ps=0) returns NOT_SUPPORTED")
        void unrecognizedModeResponsePs0() throws IOException {
            // Response: CSI ? 2027 ; 0 $ y
            backend.setResponseBytes("\033[?2027;0$y");
            Mode2027Status status = Mode2027Support.query(backend, 100);
            assertThat(status).isEqualTo(Mode2027Status.NOT_SUPPORTED);
        }

        @Test
        @DisplayName("timeout returns NOT_SUPPORTED")
        void timeoutReturnsNotSupported() throws IOException {
            // No response - backend returns -2 (timeout)
            Mode2027Status status = Mode2027Support.query(backend, 50);
            assertThat(status).isEqualTo(Mode2027Status.NOT_SUPPORTED);
        }

        @Test
        @DisplayName("malformed response returns NOT_SUPPORTED")
        void malformedResponseReturnsNotSupported() throws IOException {
            // Response with wrong terminator
            backend.setResponseBytes("\033[?2027;1$x");
            Mode2027Status status = Mode2027Support.query(backend, 100);
            assertThat(status).isEqualTo(Mode2027Status.NOT_SUPPORTED);
        }

        @Test
        @DisplayName("partial response before timeout returns NOT_SUPPORTED")
        void partialResponseBeforeTimeout() throws IOException {
            // Incomplete response
            backend.setResponseBytes("\033[?2027;");
            Mode2027Status status = Mode2027Support.query(backend, 50);
            assertThat(status).isEqualTo(Mode2027Status.NOT_SUPPORTED);
        }

        @Test
        @DisplayName("response for different mode number is ignored")
        void differentModeNumberIgnored() throws IOException {
            // Response for mode 2026 instead of 2027
            backend.setResponseBytes("\033[?2026;1$y");
            Mode2027Status status = Mode2027Support.query(backend, 50);
            assertThat(status).isEqualTo(Mode2027Status.NOT_SUPPORTED);
        }

        @Test
        @DisplayName("response with multi-digit Ps value")
        void multiDigitPsValue() throws IOException {
            // Response with Ps=12 (not a standard value, should be NOT_SUPPORTED)
            backend.setResponseBytes("\033[?2027;12$y");
            Mode2027Status status = Mode2027Support.query(backend, 100);
            assertThat(status).isEqualTo(Mode2027Status.NOT_SUPPORTED);
        }

        @Test
        @DisplayName("noise before valid response is handled")
        void noiseBeforeValidResponse() throws IOException {
            // Some garbage before the actual response
            backend.setResponseBytes("abc\033[?2027;1$y");
            Mode2027Status status = Mode2027Support.query(backend, 100);
            assertThat(status).isEqualTo(Mode2027Status.ENABLED);
        }

        @Test
        @DisplayName("query sends correct sequence")
        void querySendsCorrectSequence() throws IOException {
            backend.setResponseBytes("\033[?2027;1$y");
            Mode2027Support.query(backend, 100);
            assertThat(output.toString(StandardCharsets.UTF_8.name())).isEqualTo("\033[?2027$p");
        }
    }

    /**
     * Mock Backend implementation for testing Mode2027Support.
     */
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
                return -2; // timeout
            }
            return responseQueue.poll();
        }

        @Override
        public void draw(Iterable<CellUpdate> updates) throws IOException {
            // Not used
        }

        @Override
        public void clear() throws IOException {
            // Not used
        }

        @Override
        public Size size() throws IOException {
            return new Size(80, 24);
        }

        @Override
        public void showCursor() throws IOException {
            // Not used
        }

        @Override
        public void hideCursor() throws IOException {
            // Not used
        }

        @Override
        public Position getCursorPosition() throws IOException {
            return Position.ORIGIN;
        }

        @Override
        public void setCursorPosition(Position position) throws IOException {
            // Not used
        }

        @Override
        public void enterAlternateScreen() throws IOException {
            // Not used
        }

        @Override
        public void leaveAlternateScreen() throws IOException {
            // Not used
        }

        @Override
        public void enableRawMode() throws IOException {
            // Not used
        }

        @Override
        public void disableRawMode() throws IOException {
            // Not used
        }

        @Override
        public void onResize(Runnable handler) {
            // Not used
        }

        @Override
        public int peek(int timeoutMs) throws IOException {
            return responseQueue.isEmpty() ? -2 : responseQueue.peek();
        }

        @Override
        public void close() throws IOException {
            // Not used
        }
    }
}
