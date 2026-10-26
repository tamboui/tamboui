/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.app;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.terminal.TestBackend;
import dev.tamboui.tui.InlineTuiConfig;

import static dev.tamboui.toolkit.Toolkit.row;
import static dev.tamboui.toolkit.Toolkit.table;
import static dev.tamboui.toolkit.Toolkit.text;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Deliberately does <b>not</b> extend {@code AbstractElementTest}: that marks the thread as a render
 * thread in setup, which would hide the bug this guards. With no render loop running, printing an
 * element must work on an ordinary thread, as it does from a user's {@code main}.
 */
class InlineToolkitRunnerPrintlnTest {

    @Test
    @DisplayName("println of a toolkit element works when no render loop is active")
    void printlnOfAnElementWorksWithNoActiveRenderLoop() throws Exception {
        try (InlineToolkitRunner runner = InlineToolkitRunner.create(new TestBackend(80, 24),
                InlineTuiConfig.builder(1).noTick().build())) {
            assertThatCode(() -> runner.println(row(text("A"), text("B"))))
                .doesNotThrowAnyException();
        }
    }

    @Test
    @DisplayName("println of a table without widths renders content")
    void printlnOfTableWithoutWidthsRendersContent() throws Exception {
        TestBackend backend = new TestBackend(80, 24);
        try (InlineToolkitRunner runner = InlineToolkitRunner.create(backend,
                InlineTuiConfig.builder(1).noTick().build())) {
            runner.println(table().header("Name", "Age", "City")
                    .row("Alice", "30", "NYC")
                    .row("Bob", "25", "LA")
                    .row("Charlie", "35", "Chicago"));
        }
        assertThat(backend.rawOutput())
            .contains("Name")
            .contains("Alice")
            .contains("Bob")
            .contains("Charlie");
    }
}
