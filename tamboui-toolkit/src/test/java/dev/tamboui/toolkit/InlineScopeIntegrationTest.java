/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import dev.tamboui.inline.InlineDisplay;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.TestBackend;
import dev.tamboui.terminal.TestBackend.OpType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for InlineDisplay dynamic resizing behavior
 * when scopes collapse/expand.
 */
class InlineScopeIntegrationTest {

    private TestBackend backend;

    @BeforeEach
    void setUp() {
        backend = new TestBackend(80, 24);
    }

    @Test
    @DisplayName("InlineDisplay resizes when scope collapses from 3 to 0 lines")
    void inlineDisplayResizesOnScopeCollapse() throws IOException {
        InlineDisplay display = InlineDisplay.withBackend(6, 80, backend);

        // Initial render with 3 lines
        display.render((area, buf) -> {}, 3, -1, -1);

        // Collapse to 0 lines
        backend.reset();
        display.render((area, buf) -> {}, 0, -1, -1);

        // Should have deleted 3 lines when shrinking
        backend.assertOps().hasDeleteLines(3);
    }

    @Test
    @DisplayName("InlineDisplay resizes when scope expands from 0 to 3 lines")
    void inlineDisplayResizesOnScopeExpand() throws IOException {
        InlineDisplay display = InlineDisplay.withBackend(6, 80, backend);

        // Initial render with 0 lines (collapsed)
        display.render((area, buf) -> {}, 0, -1, -1);

        // Expand to 3 lines
        backend.reset();
        display.render((area, buf) -> {}, 3, -1, -1);

        // Should have added lines (newlines in raw output) and moved cursor up
        assertThat(backend.rawOutput()).contains("\n");
        backend.assertOps().hasCursorUp(3);
    }

    @Test
    @DisplayName("Rapid collapse and expand handles correctly")
    void rapidCollapseExpandHandlesCorrectly() throws IOException {
        InlineDisplay display = InlineDisplay.withBackend(6, 80, backend);

        // Start with 3 lines
        display.render((area, buf) -> {}, 3, -1, -1);

        // Collapse to 0
        backend.reset();
        display.render((area, buf) -> {}, 0, -1, -1);
        backend.assertOps().hasDeleteLines(3);

        // Expand to 3
        backend.reset();
        display.render((area, buf) -> {}, 3, -1, -1);
        assertThat(backend.rawOutput()).contains("\n");

        // Collapse to 0 again
        backend.reset();
        display.render((area, buf) -> {}, 0, -1, -1);
        backend.assertOps().hasDeleteLines(3);
    }

    @Test
    @DisplayName("Partial resize from 6 to 4 lines works correctly")
    void partialResizeWorks() throws IOException {
        InlineDisplay display = InlineDisplay.withBackend(6, 80, backend);

        // Start with 6 lines
        display.render((area, buf) -> {}, 6, -1, -1);

        // Shrink to 4 lines
        backend.reset();
        display.render((area, buf) -> {}, 4, -1, -1);

        // Should delete 2 lines
        backend.assertOps().hasDeleteLines(2);

        // Grow back to 6
        backend.reset();
        display.render((area, buf) -> {}, 6, -1, -1);

        // Should add 2 lines (newlines in raw output)
        assertThat(backend.rawOutput()).contains("\n");
    }

    @Test
    @DisplayName("Growing from non-zero height uses correct cursor positioning")
    void growingFromNonZeroUsesCorrectCursorUp() throws IOException {
        InlineDisplay display = InlineDisplay.withBackend(6, 80, backend);

        // Start with 3 lines
        display.render((area, buf) -> {}, 3, -1, -1);

        // Grow to 5 lines
        backend.reset();
        display.render((area, buf) -> {}, 5, -1, -1);

        // Should NOT move cursor up by newHeight = 5 (off-by-one bug).
        // Correct value is newHeight - 1 = 4 since cursor ends at line 4
        // after moveCursorDown(2) + 2 newlines from line 2.
        backend.assertOps()
            .hasNo(OpType.CURSOR_UP, 5)
            .hasCursorUp(4);
    }

    @Test
    @DisplayName("Rendering same height does not resize")
    void sameHeightDoesNotResize() throws IOException {
        InlineDisplay display = InlineDisplay.withBackend(6, 80, backend);

        // Render at 3 lines
        display.render((area, buf) -> {}, 3, -1, -1);

        // Render again at 3 lines
        backend.reset();
        display.render((area, buf) -> {}, 3, -1, -1);

        // Should not have any insert/delete operations
        backend.assertOps()
            .hasNoDeleteLines()
            .hasNoInsertLines();
    }

    @Nested
    @DisplayName("Rendering output ordering")
    class RenderingOutputOrdering {

        @Test
        @DisplayName("Cursor positioned before content on initial render")
        void cursorPositionedBeforeContent() throws IOException {
            InlineDisplay display = InlineDisplay.withBackend(3, 80, backend);

            backend.reset();
            display.render((area, buf) -> {
                buf.setString(0, 0, "Hello", Style.EMPTY);
                buf.setString(0, 1, "World", Style.EMPTY);
            }, 2, -1, -1);

            // CR must appear before content in the transcript
            backend.assertTranscript()
                .hasOpBefore(OpType.CARRIAGE_RETURN, 0, "Hello");
        }

        @Test
        @DisplayName("Content on second line appears after newline")
        void secondLineAfterNewline() throws IOException {
            InlineDisplay display = InlineDisplay.withBackend(3, 80, backend);

            backend.reset();
            display.render((area, buf) -> {
                buf.setString(0, 0, "Line1", Style.EMPTY);
                buf.setString(0, 1, "Line2", Style.EMPTY);
            }, 2, -1, -1);

            // In the transcript, newline must appear before Line2 content
            backend.assertTranscript()
                .expectRawContaining("Line1")
                .expectRawContaining("\n")
                .expectEventually(OpType.CARRIAGE_RETURN, 0)
                .expectRawContaining("Line2");
        }

        @Test
        @DisplayName("Repeated renders replace content correctly")
        void repeatedRendersReplaceContent() throws IOException {
            InlineDisplay display = InlineDisplay.withBackend(3, 80, backend);

            // First render
            display.render((area, buf) -> {
                buf.setString(0, 0, "First", Style.EMPTY);
            }, 1, -1, -1);

            // Second render - should position cursor before writing
            backend.reset();
            display.render((area, buf) -> {
                buf.setString(0, 0, "Second", Style.EMPTY);
            }, 1, -1, -1);

            // The transcript must show CR before the content of the second render
            backend.assertTranscript()
                .hasOpBefore(OpType.CARRIAGE_RETURN, 0, "Second");
        }

        @Test
        @DisplayName("Multi-line render has correct ordering per line")
        void multiLineRenderOrdering() throws IOException {
            InlineDisplay display = InlineDisplay.withBackend(4, 80, backend);

            backend.reset();
            display.render((area, buf) -> {
                buf.setString(0, 0, "AAA", Style.EMPTY);
                buf.setString(0, 1, "BBB", Style.EMPTY);
                buf.setString(0, 2, "CCC", Style.EMPTY);
            }, 3, -1, -1);

            // Verify ordering: CR → AAA → \n → CR → BBB → \n → CR → CCC
            backend.assertTranscript()
                .expectEventually(OpType.CARRIAGE_RETURN, 0)
                .expectRawContaining("AAA")
                .expectRawContaining("\n")
                .expectEventually(OpType.CARRIAGE_RETURN, 0)
                .expectRawContaining("BBB")
                .expectRawContaining("\n")
                .expectEventually(OpType.CARRIAGE_RETURN, 0)
                .expectRawContaining("CCC");
        }

        @Test
        @DisplayName("println inserts line before display content")
        void printlnInsertsBeforeDisplay() throws IOException {
            InlineDisplay display = InlineDisplay.withBackend(3, 80, backend);

            // Initial render
            display.render((area, buf) -> {
                buf.setString(0, 0, "Status", Style.EMPTY);
            }, 1, -1, -1);

            // Print above display
            backend.reset();
            display.println("Log message");

            // Should: CR (go to line 0), INSERT_LINES, print message, then redraw
            backend.assertTranscript()
                .expectEventually(OpType.CARRIAGE_RETURN, 0)
                .expectEventually(OpType.INSERT_LINES, 1)
                .expectRawContaining("Log message");
        }

        @Test
        @DisplayName("println followed by resize does not leave stale content")
        void printlnFollowedByResizeNoStaleContent() throws IOException {
            InlineDisplay display = InlineDisplay.withBackend(6, 80, backend);

            // Render with 6 lines
            display.render((area, buf) -> {
                buf.setString(0, 0, "Header", Style.EMPTY);
                buf.setString(0, 5, "Footer", Style.EMPTY);
            }, 6, -1, -1);

            // println + shrink to 4 lines
            display.println("Log entry");
            backend.reset();
            display.render((area, buf) -> {
                buf.setString(0, 0, "Header", Style.EMPTY);
                buf.setString(0, 3, "New Footer", Style.EMPTY);
            }, 4, -1, -1);

            // Should delete 2 lines when shrinking
            backend.assertOps().hasDeleteLines(2);
        }

        @Test
        @DisplayName("println with non-zero lastCursorY moves cursor up first")
        void printlnWithNonZeroCursorMovesCursorUp() throws IOException {
            InlineDisplay display = InlineDisplay.withBackend(3, 80, backend);

            // Render with explicit cursor at line 2
            display.render((area, buf) -> {
                buf.setString(0, 0, "Line0", Style.EMPTY);
                buf.setString(0, 2, "Line2", Style.EMPTY);
            }, 3, 5, 2);

            // Print above display - should move up from cursor position
            backend.reset();
            display.println("Message");

            // Should move cursor up by 2 (lastCursorY) before inserting
            backend.assertTranscript()
                .expectEventually(OpType.CARRIAGE_RETURN, 0)
                .expectEventually(OpType.CURSOR_UP, 2)
                .expectEventually(OpType.INSERT_LINES, 1)
                .expectRawContaining("Message");
        }
    }
}
