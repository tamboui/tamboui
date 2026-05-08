/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.terminal;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.widget.RawOutputCapable;
import dev.tamboui.widget.StatefulWidget;
import dev.tamboui.widget.Widget;

import static org.assertj.core.api.Assertions.assertThat;

class RawOutputCleanupTest {

    private static final String KITTY_DELETE_ALL = "\033_Ga=d,d=a\033\\";

    static class RawWidget implements Widget, RawOutputCapable {
        @Override
        public void render(Rect area, Buffer buffer) {
        }

        @Override
        public void render(Rect area, Buffer buffer, OutputStream rawOutput) {
            try {
                rawOutput.write("IMG".getBytes(StandardCharsets.US_ASCII));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    static class PlainWidget implements Widget {
        @Override
        public void render(Rect area, Buffer buffer) {
        }
    }

    static class RawStatefulWidget implements StatefulWidget<Void>, RawOutputCapable {
        @Override
        public void render(Rect area, Buffer buffer, Void state) {
        }

        @Override
        public void render(Rect area, Buffer buffer, OutputStream rawOutput) {
        }
    }

    @Nested
    @DisplayName("Frame raw output tracking")
    class FrameTracking {

        @Test
        @DisplayName("renderWidget tracks RawOutputCapable widget")
        void renderWidgetTracksRawOutput() {
            Buffer buf = Buffer.empty(Rect.of(10, 5));
            Frame frame = new Frame(buf, new NullOutputStream());

            frame.renderWidget(new RawWidget(), new Rect(0, 0, 5, 3));

            assertThat(frame.hadRawOutput()).isTrue();
            assertThat(frame.rawOutputAreas()).containsExactly(new Rect(0, 0, 5, 3));
        }

        @Test
        @DisplayName("renderWidget does not track plain widget")
        void renderWidgetNoTrackingForPlain() {
            Buffer buf = Buffer.empty(Rect.of(10, 5));
            Frame frame = new Frame(buf, new NullOutputStream());

            frame.renderWidget(new PlainWidget(), new Rect(0, 0, 5, 3));

            assertThat(frame.hadRawOutput()).isFalse();
            assertThat(frame.rawOutputAreas()).isEmpty();
        }

        @Test
        @DisplayName("renderStatefulWidget tracks RawOutputCapable widget")
        void renderStatefulWidgetTracksRawOutput() {
            Buffer buf = Buffer.empty(Rect.of(10, 5));
            Frame frame = new Frame(buf, new NullOutputStream());

            frame.renderStatefulWidget(new RawStatefulWidget(), new Rect(1, 1, 4, 2), null);

            assertThat(frame.hadRawOutput()).isTrue();
            assertThat(frame.rawOutputAreas()).containsExactly(new Rect(1, 1, 4, 2));
        }

        @Test
        @DisplayName("multiple raw widgets accumulate areas")
        void multipleRawWidgets() {
            Buffer buf = Buffer.empty(Rect.of(20, 10));
            Frame frame = new Frame(buf, new NullOutputStream());

            frame.renderWidget(new RawWidget(), new Rect(0, 0, 5, 3));
            frame.renderWidget(new RawWidget(), new Rect(10, 0, 5, 3));

            assertThat(frame.hadRawOutput()).isTrue();
            assertThat(frame.rawOutputAreas()).containsExactly(
                    new Rect(0, 0, 5, 3),
                    new Rect(10, 0, 5, 3));
        }
    }

    @Nested
    @DisplayName("Terminal cleanup on image removal")
    class TerminalCleanup {

        private TestBackend backend;
        private Terminal<TestBackend> terminal;

        @BeforeEach
        void setUp() {
            backend = new TestBackend(20, 10);
            terminal = new Terminal<>(backend);
        }

        @Test
        @DisplayName("sends Kitty delete-all and space overwrite when image removed")
        void cleanupOnImageRemoval() {
            // Frame 1: render with raw output widget
            terminal.draw(frame -> frame.renderWidget(new RawWidget(), new Rect(0, 0, 5, 3)));

            backend.reset();

            // Frame 2: render without raw output widget
            terminal.draw(frame -> frame.renderWidget(new PlainWidget(), new Rect(0, 0, 5, 3)));

            String raw = backend.rawOutput();
            assertThat(raw).contains(KITTY_DELETE_ALL);
            // Should contain cursor moves and spaces for the 5x3 area
            assertThat(raw).contains("\033[1;1H");
            assertThat(raw).contains("\033[2;1H");
            assertThat(raw).contains("\033[3;1H");
        }

        @Test
        @DisplayName("no cleanup when no previous raw output")
        void noCleanupWithoutPreviousRawOutput() {
            // Frame 1: render plain widget
            terminal.draw(frame -> frame.renderWidget(new PlainWidget(), new Rect(0, 0, 5, 3)));

            backend.reset();

            // Frame 2: another plain widget
            terminal.draw(frame -> frame.renderWidget(new PlainWidget(), new Rect(0, 0, 5, 3)));

            String raw = backend.rawOutput();
            assertThat(raw).doesNotContain(KITTY_DELETE_ALL);
        }

        @Test
        @DisplayName("no cleanup when image stays in same area")
        void noCleanupWhenImageStays() {
            Rect imageArea = new Rect(0, 0, 5, 3);

            // Frame 1: raw widget at position
            terminal.draw(frame -> frame.renderWidget(new RawWidget(), imageArea));

            backend.reset();

            // Frame 2: raw widget at same position
            terminal.draw(frame -> frame.renderWidget(new RawWidget(), imageArea));

            String raw = backend.rawOutput();
            // Should NOT contain delete-all (image still present)
            assertThat(raw).doesNotContain(KITTY_DELETE_ALL);
            // Should NOT contain space overwrites (area unchanged)
            assertThat(raw).doesNotContain("\033[1;1H");
        }

        @Test
        @DisplayName("overwrites stale area when image moves")
        void cleanupWhenImageMoves() {
            // Frame 1: raw widget at position A
            terminal.draw(frame -> frame.renderWidget(new RawWidget(), new Rect(0, 0, 5, 3)));

            backend.reset();

            // Frame 2: raw widget at position B (moved)
            terminal.draw(frame -> frame.renderWidget(new RawWidget(), new Rect(10, 0, 5, 3)));

            String raw = backend.rawOutput();
            // Should NOT delete-all (current frame still has an image)
            assertThat(raw).doesNotContain(KITTY_DELETE_ALL);
            // Should overwrite the OLD area (0,0 5x3) with spaces
            assertThat(raw).contains("\033[1;1H");
            assertThat(raw).contains("\033[2;1H");
            assertThat(raw).contains("\033[3;1H");
        }

        @Test
        @DisplayName("cleanup on terminal clear()")
        void cleanupOnClear() {
            // Render with raw output
            terminal.draw(frame -> frame.renderWidget(new RawWidget(), new Rect(0, 0, 5, 3)));

            backend.reset();

            terminal.clear();

            String raw = backend.rawOutput();
            assertThat(raw).contains(KITTY_DELETE_ALL);
        }

        @Test
        @DisplayName("cleanup on terminal close()")
        void cleanupOnClose() {
            // Render with raw output
            terminal.draw(frame -> frame.renderWidget(new RawWidget(), new Rect(0, 0, 5, 3)));

            backend.reset();

            terminal.close();

            String raw = backend.rawOutput();
            assertThat(raw).contains(KITTY_DELETE_ALL);
        }

        @Test
        @DisplayName("cleanup on terminal resize")
        void cleanupOnResize() {
            // Render with raw output
            terminal.draw(frame -> frame.renderWidget(new RawWidget(), new Rect(0, 0, 5, 3)));

            backend.reset();

            // Trigger resize by changing backend size
            backend.resize(40, 20);
            terminal.draw(frame -> frame.renderWidget(new PlainWidget(), new Rect(0, 0, 5, 3)));

            String raw = backend.rawOutput();
            assertThat(raw).contains(KITTY_DELETE_ALL);
        }
    }

    private static class NullOutputStream extends OutputStream {
        @Override
        public void write(int b) {
        }

        @Override
        public void write(byte[] b, int off, int len) {
        }
    }
}
