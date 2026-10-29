/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui;

import java.io.IOException;
import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.terminal.KeyboardProtocol;
import dev.tamboui.terminal.TestBackend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KittyKeyboardLifecycleTest {

    @Test
    @DisplayName("TuiRunner with AUTO protocol pushes flags 5 on startup and pops on close")
    void tuiRunnerAutoProtocolLifecycle() throws Exception {
        java.util.List<String> order = new java.util.ArrayList<>();
        TestBackend backend = new TestBackend(80, 24) {
            @Override
            public void enterAlternateScreen() throws java.io.IOException {
                super.enterAlternateScreen();
                order.add("enterAlternateScreen");
            }

            @Override
            public void leaveAlternateScreen() throws java.io.IOException {
                super.leaveAlternateScreen();
                order.add("leaveAlternateScreen");
            }

            @Override
            public boolean enableKeyboardProtocol(KeyboardProtocol protocol) throws java.io.IOException {
                boolean enabled = super.enableKeyboardProtocol(protocol);
                order.add("enableKeyboardProtocol:" + protocol);
                return enabled;
            }

            @Override
            public void disableKeyboardProtocol() throws java.io.IOException {
                super.disableKeyboardProtocol();
                order.add("disableKeyboardProtocol");
            }
        };
        TuiConfig config = TuiConfig.builder()
                .backend(backend)
                .keyboardProtocol(KeyboardProtocol.AUTO)
                .pollTimeout(Duration.ofMillis(20))
                .build();

        try (TuiRunner runner = TuiRunner.create(config)) {
            assertThat(runner).isNotNull();
            assertThat(backend.rawOutput()).contains("\033[>5u");
            assertThat(order).containsExactly("enterAlternateScreen", "enableKeyboardProtocol:AUTO");
        }

        // After close: pop sequence must be sent, and must precede leaveAlternateScreen
        assertThat(backend.rawOutput()).contains("\033[<u");
        assertThat(order).containsExactly(
                "enterAlternateScreen",
                "enableKeyboardProtocol:AUTO",
                "disableKeyboardProtocol",
                "leaveAlternateScreen"
        );
    }

    @Test
    @DisplayName("TuiRunner with FULL protocol pushes flags 31 on startup and pops on close")
    void tuiRunnerFullProtocolLifecycle() throws Exception {
        TestBackend backend = new TestBackend(80, 24);
        TuiConfig config = TuiConfig.builder()
                .backend(backend)
                .keyboardProtocol(KeyboardProtocol.FULL)
                .pollTimeout(Duration.ofMillis(20))
                .build();

        try (TuiRunner runner = TuiRunner.create(config)) {
            assertThat(runner).isNotNull();
            assertThat(backend.rawOutput()).contains("\033[>31u");
        }

        assertThat(backend.rawOutput()).contains("\033[<u");
    }

    @Test
    @DisplayName("TuiRunner with LEGACY protocol sends no Kitty sequences")
    void tuiRunnerLegacyProtocolLifecycle() throws Exception {
        TestBackend backend = new TestBackend(80, 24);
        TuiConfig config = TuiConfig.builder()
                .backend(backend)
                .keyboardProtocol(KeyboardProtocol.LEGACY)
                .pollTimeout(Duration.ofMillis(20))
                .build();

        try (TuiRunner runner = TuiRunner.create(config)) {
            assertThat(runner).isNotNull();
            assertThat(backend.rawOutput()).doesNotContain("\033[>");
        }

        assertThat(backend.rawOutput()).doesNotContain("\033[<u");
    }

    @Test
    @DisplayName("InlineTuiRunner with AUTO protocol pushes on startup and pops on close")
    void inlineTuiRunnerAutoProtocolLifecycle() {
        TestBackend backend = new TestBackend(80, 24);
        InlineTuiConfig config = InlineTuiConfig.builder(4)
                .keyboardProtocol(KeyboardProtocol.AUTO)
                .pollTimeout(Duration.ofMillis(20))
                .build();

        try (InlineTuiRunner runner = InlineTuiRunner.create(backend, config)) {
            assertThat(runner).isNotNull();
            assertThat(backend.rawOutput()).contains("\033[>5u");
        }

        assertThat(backend.rawOutput()).contains("\033[<u");
    }

    @Test
    @DisplayName("TuiRunner does not pop keyboard protocol when startup fails before push")
    void tuiRunnerDoesNotPopIfPushDidNotHappen() {
        TestBackend backend = new TestBackend(80, 24) {
            @Override
            public void enableRawMode() throws IOException {
                throw new IOException("not a tty");
            }
        };
        TuiConfig config = TuiConfig.builder()
                .backend(backend)
                .keyboardProtocol(KeyboardProtocol.AUTO)
                .pollTimeout(Duration.ofMillis(20))
                .build();

        assertThatThrownBy(() -> TuiRunner.create(config)).isInstanceOf(IOException.class);

        assertThat(backend.rawOutput())
                .as("no Kitty push happened, so no pop may leak to the output")
                .doesNotContain("\033[>")
                .doesNotContain("\033[<u");
    }

    @Test
    @DisplayName("TuiRunner pops exactly once when startup fails after push")
    void tuiRunnerPopsOnceWhenFailureFollowsPush() {
        TestBackend backend = new TestBackend(80, 24) {
            @Override
            public void hideCursor() throws IOException {
                throw new IOException("terminal write failed");
            }
        };
        TuiConfig config = TuiConfig.builder()
                .backend(backend)
                .keyboardProtocol(KeyboardProtocol.AUTO)
                .hideCursor(true)
                .pollTimeout(Duration.ofMillis(20))
                .build();

        assertThatThrownBy(() -> TuiRunner.create(config)).isInstanceOf(IOException.class);

        String output = backend.rawOutput();
        assertThat(countOccurrences(output, "\033[>5u")).as("pushed once").isEqualTo(1);
        assertThat(countOccurrences(output, "\033[<u")).as("popped once").isEqualTo(1);
    }

    @Test
    @DisplayName("InlineTuiRunner does not pop keyboard protocol when startup fails before push")
    void inlineTuiRunnerDoesNotPopIfPushDidNotHappen() {
        TestBackend backend = new TestBackend(80, 24) {
            @Override
            public void enableRawMode() throws IOException {
                throw new IOException("not a tty");
            }
        };
        InlineTuiConfig config = InlineTuiConfig.builder(4)
                .keyboardProtocol(KeyboardProtocol.AUTO)
                .pollTimeout(Duration.ofMillis(20))
                .build();

        assertThatThrownBy(() -> InlineTuiRunner.create(backend, config))
                .isInstanceOf(RuntimeException.class);

        assertThat(backend.rawOutput())
                .as("no Kitty push happened, so no pop may leak to the output")
                .doesNotContain("\033[>")
                .doesNotContain("\033[<u");
    }

    @Test
    @DisplayName("InlineTuiRunner with LEGACY and a failed startup emits no Kitty sequences")
    void inlineTuiRunnerLegacyDoesNotPopOnFailure() {
        TestBackend backend = new TestBackend(80, 24) {
            @Override
            public void enableRawMode() throws IOException {
                throw new IOException("not a tty");
            }
        };
        InlineTuiConfig config = InlineTuiConfig.builder(4)
                .keyboardProtocol(KeyboardProtocol.LEGACY)
                .pollTimeout(Duration.ofMillis(20))
                .build();

        assertThatThrownBy(() -> InlineTuiRunner.create(backend, config))
                .isInstanceOf(RuntimeException.class);

        assertThat(backend.rawOutput())
                .doesNotContain("\033[>")
                .doesNotContain("\033[<u");
    }

    private static int countOccurrences(String haystack, String needle) {
        int count = 0;
        int index = haystack.indexOf(needle);
        while (index >= 0) {
            count++;
            index = haystack.indexOf(needle, index + needle.length());
        }
        return count;
    }

    @Test
    @DisplayName("InlineTuiRunner with LEGACY protocol sends no Kitty sequences")
    void inlineTuiRunnerLegacyProtocolLifecycle() {
        TestBackend backend = new TestBackend(80, 24);
        InlineTuiConfig config = InlineTuiConfig.builder(4)
                .keyboardProtocol(KeyboardProtocol.LEGACY)
                .pollTimeout(Duration.ofMillis(20))
                .build();

        try (InlineTuiRunner runner = InlineTuiRunner.create(backend, config)) {
            assertThat(runner).isNotNull();
            assertThat(backend.rawOutput()).doesNotContain("\033[>");
        }

        assertThat(backend.rawOutput()).doesNotContain("\033[<u");
    }
}
