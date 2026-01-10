/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.inline;

import dev.tamboui.buffer.CellUpdate;
import dev.tamboui.layout.Position;
import dev.tamboui.layout.Size;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Backend;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;

class InlineDisplayTest {

    private StringWriter stringWriter;
    private PrintWriter printWriter;
    private MockBackend mockBackend;

    @BeforeEach
    void setUp() {
        stringWriter = new StringWriter();
        printWriter = new PrintWriter(stringWriter, true);
        mockBackend = new MockBackend();
    }

    @Test
    @DisplayName("height() returns configured height")
    void heightReturnsConfiguredHeight() {
        InlineDisplay display = new InlineDisplay(5, 80, mockBackend, printWriter);
        assertThat(display.height()).isEqualTo(5);
    }

    @Test
    @DisplayName("width() returns configured width")
    void widthReturnsConfiguredWidth() {
        InlineDisplay display = new InlineDisplay(3, 120, mockBackend, printWriter);
        assertThat(display.width()).isEqualTo(120);
    }

    @Test
    @DisplayName("clearOnClose() returns this for chaining")
    void clearOnCloseReturnsThis() {
        InlineDisplay display = new InlineDisplay(3, 80, mockBackend, printWriter);
        InlineDisplay result = display.clearOnClose();
        assertThat(result).isSameAs(display);
    }

    @Test
    @DisplayName("setLine() with negative line is ignored")
    void setLineWithNegativeLineIsIgnored() {
        InlineDisplay display = new InlineDisplay(3, 80, mockBackend, printWriter);
        // Should not throw
        display.setLine(-1, "test");
        // Nothing written (no initialization triggered)
        assertThat(stringWriter.toString()).isEmpty();
    }

    @Test
    @DisplayName("setLine() with line >= height is ignored")
    void setLineWithLineBeyondHeightIsIgnored() {
        InlineDisplay display = new InlineDisplay(3, 80, mockBackend, printWriter);
        // Should not throw
        display.setLine(3, "test");
        display.setLine(10, "test");
        // Nothing written (no initialization triggered)
        assertThat(stringWriter.toString()).isEmpty();
    }

    @Test
    @DisplayName("setLine() initializes display and writes content")
    void setLineInitializesAndWritesContent() {
        InlineDisplay display = new InlineDisplay(2, 40, mockBackend, printWriter);
        display.setLine(0, "Hello");

        String output = stringWriter.toString();
        // Should contain initialization (blank lines) and content
        assertThat(output).isNotEmpty();
        // Should contain "Hello"
        assertThat(output).contains("Hello");
    }

    @Test
    @DisplayName("render() draws to buffer")
    void renderDrawsToBuffer() {
        InlineDisplay display = new InlineDisplay(2, 40, mockBackend, printWriter);

        display.render((area, buf) -> {
            buf.setString(0, 0, "Test content", Style.EMPTY);
        });

        String output = stringWriter.toString();
        assertThat(output).contains("Test");
    }

    @Test
    @DisplayName("println() outputs text above status area")
    void printlnOutputsTextAboveStatusArea() {
        InlineDisplay display = new InlineDisplay(2, 40, mockBackend, printWriter);
        // Initialize with a render call first
        display.setLine(0, "Status");

        display.println("Log message");

        String output = stringWriter.toString();
        assertThat(output).contains("Log message");
    }

    @Test
    @DisplayName("println(Text) renders styled text")
    void printlnTextRendersStyledText() {
        InlineDisplay display = new InlineDisplay(2, 40, mockBackend, printWriter);
        display.setLine(0, "Status");

        display.println(Text.from(Line.from(
            Span.styled("colored", Style.EMPTY.fg(Color.RED))
        )));

        String output = stringWriter.toString();
        // Should contain ANSI color code for red
        assertThat(output).contains("colored");
    }

    @Test
    @DisplayName("close() writes reset sequence")
    void closeWritesResetSequence() throws IOException {
        InlineDisplay display = new InlineDisplay(2, 40, mockBackend, printWriter);
        display.setLine(0, "Status");

        display.close();

        String output = stringWriter.toString();
        // Should contain reset sequence
        assertThat(output).contains("\u001b[0m");
        assertThat(mockBackend.closed).isTrue();
    }

    @Test
    @DisplayName("release() can be called before close()")
    void releaseCanBeCalledBeforeClose() throws IOException {
        InlineDisplay display = new InlineDisplay(2, 40, mockBackend, printWriter);
        display.setLine(0, "Status");

        display.release();
        display.close();

        // Should not throw, backend should be closed
        assertThat(mockBackend.closed).isTrue();
    }

    @Test
    @DisplayName("release() is idempotent")
    void releaseIsIdempotent() throws IOException {
        InlineDisplay display = new InlineDisplay(2, 40, mockBackend, printWriter);
        display.setLine(0, "Status");

        display.release();
        int outputLengthAfterFirstRelease = stringWriter.toString().length();

        display.release();

        // Second release should not add more output
        assertThat(stringWriter.toString().length()).isEqualTo(outputLengthAfterFirstRelease);
    }

    @Test
    @DisplayName("setLine(Text) renders styled text")
    void setLineTextRendersStyledText() {
        InlineDisplay display = new InlineDisplay(2, 40, mockBackend, printWriter);

        display.setLine(0, Text.from(Line.from(
            Span.styled("styled", Style.EMPTY.fg(Color.GREEN))
        )));

        String output = stringWriter.toString();
        assertThat(output).contains("styled");
    }

    /**
     * A minimal mock Backend for testing.
     */
    private static class MockBackend implements Backend {
        boolean closed = false;

        @Override
        public void draw(Iterable<CellUpdate> updates) throws IOException {
        }

        @Override
        public void flush() throws IOException {
        }

        @Override
        public void clear() throws IOException {
        }

        @Override
        public Size size() throws IOException {
            return new Size(80, 24);
        }

        @Override
        public void showCursor() throws IOException {
        }

        @Override
        public void hideCursor() throws IOException {
        }

        @Override
        public Position getCursorPosition() throws IOException {
            return new Position(0, 0);
        }

        @Override
        public void setCursorPosition(Position position) throws IOException {
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
            closed = true;
        }
    }
}
