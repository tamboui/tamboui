/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.terminal;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.Queue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KittyKeyboardSupportTest {

    @Nested
    @DisplayName("Escape sequences and flag constants")
    class SequencesAndFlags {

        @Test
        @DisplayName("push sequence formats flags correctly")
        void pushSequence() {
            assertThat(KittyKeyboardSupport.pushSequence(5)).isEqualTo("\033[>5u");
            assertThat(KittyKeyboardSupport.pushSequence(31)).isEqualTo("\033[>31u");
            assertThat(KittyKeyboardSupport.pushSequence(0)).isEqualTo("\033[>0u");
        }

        @Test
        @DisplayName("pop sequence is CSI < u")
        void popSequence() {
            assertThat(KittyKeyboardSupport.popSequence()).isEqualTo("\033[<u");
        }

        @Test
        @DisplayName("query sequence is CSI ? u")
        void querySequence() {
            assertThat(KittyKeyboardSupport.querySequence()).isEqualTo("\033[?u");
        }

        @Test
        @DisplayName("AUTO flags equal disambiguate + alternate keys")
        void autoFlags() {
            assertThat(KittyKeyboardSupport.AUTO_FLAGS).isEqualTo(
                    KittyKeyboardSupport.DISAMBIGUATE_ESCAPE_CODES
                            | KittyKeyboardSupport.REPORT_ALTERNATE_KEYS);
        }

        @Test
        @DisplayName("FULL flags equal all five enhancement flags")
        void fullFlags() {
            assertThat(KittyKeyboardSupport.FULL_FLAGS).isEqualTo(
                    KittyKeyboardSupport.DISAMBIGUATE_ESCAPE_CODES
                            | KittyKeyboardSupport.REPORT_EVENT_TYPES
                            | KittyKeyboardSupport.REPORT_ALTERNATE_KEYS
                            | KittyKeyboardSupport.REPORT_ALL_KEYS_AS_ESCAPE_CODES
                            | KittyKeyboardSupport.REPORT_ASSOCIATED_TEXT);
        }
    }

    @Nested
    @DisplayName("query() terminal detection")
    class QueryTests {

        private QueueBackend backend;

        @org.junit.jupiter.api.BeforeEach
        void setup() {
            backend = new QueueBackend();
        }

        @Test
        @DisplayName("writes CSI ? u on query")
        void writesQuerySequence() throws IOException {
            backend.enqueueResponse("\033[?31u");

            KittyKeyboardSupport.query(backend, 100);

            assertThat(backend.getOutput()).isEqualTo("\033[?u");
        }

        @Test
        @DisplayName("parses non-zero flag response")
        void parsesFlags() throws IOException {
            backend.enqueueResponse("\033[?31u");

            int flags = KittyKeyboardSupport.query(backend, 100);

            assertThat(flags).isEqualTo(31);
        }

        @Test
        @DisplayName("returns 0 when terminal reports zero flags")
        void parsesZeroFlags() throws IOException {
            backend.enqueueResponse("\033[?0u");

            int flags = KittyKeyboardSupport.query(backend, 100);

            assertThat(flags).isZero();
        }

        @Test
        @DisplayName("returns -1 when terminal does not respond within timeout")
        void returnsMinusOneOnTimeout() throws IOException {
            int flags = KittyKeyboardSupport.query(backend, 50);

            assertThat(flags).isEqualTo(-1);
        }

        @Test
        @DisplayName("ignores and recovers from garbage before a valid response")
        void recoversFromGarbage() throws IOException {
            backend.enqueueResponse("garbage\033[?5u");

            int flags = KittyKeyboardSupport.query(backend, 100);

            assertThat(flags).isEqualTo(5);
        }
    }

    /** TestBackend subclass that lets the test enqueue bytes to be returned from read(). */
    private static final class QueueBackend extends TestBackend {
        private final Queue<Integer> input = new LinkedList<>();

        QueueBackend() {
            super(80, 24);
        }

        void enqueueResponse(String s) {
            for (byte b : s.getBytes(StandardCharsets.UTF_8)) {
                input.offer(b & 0xFF);
            }
        }

        String getOutput() {
            return rawOutput();
        }

        @Override
        public int read(int timeoutMs) {
            Integer b = input.poll();
            return b == null ? -1 : b;
        }

        @Override
        public int peek(int timeoutMs) {
            Integer b = input.peek();
            return b == null ? -1 : b;
        }
    }
}
