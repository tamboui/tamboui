/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.terminal.KeyboardProtocol;
import dev.tamboui.terminal.TestBackend;

import static org.assertj.core.api.Assertions.assertThat;

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
            public void pushKeyboardProtocol(KeyboardProtocol protocol) throws java.io.IOException {
                super.pushKeyboardProtocol(protocol);
                order.add("pushKeyboardProtocol:" + protocol);
            }

            @Override
            public void popKeyboardProtocol() throws java.io.IOException {
                super.popKeyboardProtocol();
                order.add("popKeyboardProtocol");
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
            assertThat(order).containsExactly("enterAlternateScreen", "pushKeyboardProtocol:AUTO");
        }

        // After close: pop sequence must be sent, and must precede leaveAlternateScreen
        assertThat(backend.rawOutput()).contains("\033[<u");
        assertThat(order).containsExactly(
                "enterAlternateScreen",
                "pushKeyboardProtocol:AUTO",
                "popKeyboardProtocol",
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
