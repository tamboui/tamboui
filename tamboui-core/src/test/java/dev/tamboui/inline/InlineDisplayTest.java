/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.inline;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.Cell;
import dev.tamboui.buffer.DiffResult;
import dev.tamboui.layout.Position;
import dev.tamboui.layout.Size;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.AnsiStringBuilder;
import dev.tamboui.terminal.Backend;
import dev.tamboui.terminal.TestBackend;
import dev.tamboui.terminal.TestBackend.Op;
import dev.tamboui.terminal.TestBackend.OpType;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;

import static org.assertj.core.api.Assertions.assertThat;

class InlineDisplayTest {

    private static final Pattern SGR = Pattern.compile("\u001b\\[[0-9;]*m");

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

    @Test
    @DisplayName("auto-width displays track terminal resize on the next render")
    void autoWidthDisplayTracksTerminalResize() throws IOException {
        InlineDisplay display = InlineDisplay.withBackend(2, mockBackend);

        display.render((area, buf) -> assertThat(area.width()).isEqualTo(80));

        mockBackend.resize(48, 24);

        display.render((area, buf) -> assertThat(area.width()).isEqualTo(48));

        assertThat(display.width()).isEqualTo(48);
    }

    @Test
    @DisplayName("fixed-width displays ignore terminal resize")
    void fixedWidthDisplayIgnoresTerminalResize() throws IOException {
        InlineDisplay display = InlineDisplay.withBackend(2, 40, mockBackend);

        mockBackend.resize(72, 24);

        display.render((area, buf) -> assertThat(area.width()).isEqualTo(40));

        assertThat(display.width()).isEqualTo(40);
    }

    @Test
    @DisplayName("render() draws a styled space at the end of a line, such as a text input cursor")
    void renderDrawsStyledSpaceAtEndOfLine() throws IOException {
        TestBackend backend = new TestBackend(20, 10);
        InlineDisplay display = InlineDisplay.withBackend(1, 20, backend);
        Style cursorStyle = Style.EMPTY.reversed();

        display.render((area, buf) -> {
            buf.setString(0, 0, "abc", Style.EMPTY);
            buf.set(3, 0, new Cell(" ", cursorStyle));
        }, 1, 3, 0);

        assertThat(backend.rawOutput()).contains("abc" + AnsiStringBuilder.styleToAnsi(cursorStyle) + " ");
    }

    @Test
    @DisplayName("render() draws a background up to the last styled cell of a line")
    void renderDrawsBackgroundUpToLastStyledCell() throws IOException {
        TestBackend backend = new TestBackend(12, 10);
        InlineDisplay display = InlineDisplay.withBackend(1, 12, backend);
        Style barStyle = Style.EMPTY.bg(Color.BLUE);

        display.render((area, buf) -> {
            buf.setStyle(area, barStyle);
            buf.setString(0, 0, "Status", barStyle);
        });

        assertThat(backend.rawOutput()).contains(AnsiStringBuilder.styleToAnsi(barStyle) + "Status      ");
    }

    @Test
    @DisplayName("render() erases each line with the default style, so a background does not bleed into the next line")
    void renderErasesEachLineWithDefaultStyle() throws IOException {
        TestBackend backend = new TestBackend(20, 10);
        InlineDisplay display = InlineDisplay.withBackend(2, 20, backend);

        display.render((area, buf) -> {
            buf.setString(0, 0, "Header", Style.EMPTY.bg(Color.BLUE));
            buf.setString(0, 1, "plain", Style.EMPTY);
        });

        assertThat(stylesActiveAtEachErase(backend))
            .containsExactly(AnsiStringBuilder.RESET, AnsiStringBuilder.RESET);
    }

    /**
     * Returns the SGR sequence in effect each time a line is erased; terminals erase
     * with the background of that style.
     */
    private static List<String> stylesActiveAtEachErase(TestBackend backend) {
        List<String> styles = new ArrayList<>();
        String active = AnsiStringBuilder.RESET;
        for (Object entry : backend.transcript()) {
            if (entry instanceof String) {
                Matcher sgr = SGR.matcher((String) entry);
                while (sgr.find()) {
                    active = sgr.group();
                }
            } else if (((Op) entry).type() == OpType.ERASE_EOL) {
                styles.add(active);
            }
        }
        return styles;
    }

    /**
     * A minimal mock Backend for testing.
     */
    private static class MockBackend implements Backend {
        boolean closed = false;
        int width = 80;
        int height = 24;
        Runnable resizeHandler;

        @Override
        public void draw(DiffResult diff) throws IOException {
        }        @Override
        public void flush() throws IOException {
        }

        @Override
        public void clear() throws IOException {
        }

        @Override
        public Size size() throws IOException {
            return new Size(width, height);
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
        public void writeRaw(byte[] data) throws IOException {
            // Write to nowhere - just accept the output for testing
        }

        @Override
        public void onResize(Runnable handler) {
            this.resizeHandler = handler;
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

        void resize(int width, int height) {
            this.width = width;
            this.height = height;
            if (resizeHandler != null) {
                resizeHandler.run();
            }
        }
    }
}
