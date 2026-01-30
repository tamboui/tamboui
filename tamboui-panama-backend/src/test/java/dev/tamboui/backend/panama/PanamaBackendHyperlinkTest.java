/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.backend.panama;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.Cell;
import dev.tamboui.buffer.CellUpdate;
import dev.tamboui.layout.Size;
import dev.tamboui.style.Style;

import static org.assertj.core.api.Assertions.assertThat;

class PanamaBackendHyperlinkTest {

    @Test
    @DisplayName("draw emits OSC8 start and end around linked cell")
    void hyperlinkStartAndEndAroundCell() throws IOException {
        FakeTerminal terminal = new FakeTerminal();
        PanamaBackend backend = new PanamaBackend(terminal);

        Style style = Style.EMPTY.hyperlink("https://example.com");
        List<CellUpdate> updates = List.of(new CellUpdate(0, 0, new Cell("A", style)));

        backend.draw(updates);
        backend.flush();

        String output = terminal.output();
        String start = "\u001b]8;;https://example.com\u001b\\";
        String end = "\u001b]8;;\u001b\\";

        int startIndex = output.indexOf(start);
        int endIndex = output.indexOf(end);
        int symbolIndex = output.indexOf("A");

        assertThat(startIndex).isGreaterThanOrEqualTo(0);
        assertThat(endIndex).isGreaterThan(startIndex);
        assertThat(symbolIndex).isGreaterThan(startIndex);
        assertThat(symbolIndex).isLessThan(endIndex);
    }

    @Test
    @DisplayName("draw ends hyperlink before next non-linked cell")
    void hyperlinkEndsBeforePlainCell() throws IOException {
        FakeTerminal terminal = new FakeTerminal();
        PanamaBackend backend = new PanamaBackend(terminal);

        Style linkStyle = Style.EMPTY.hyperlink("https://example.com", "link-1");
        List<CellUpdate> updates = Arrays.asList(new CellUpdate(0, 0, new Cell("A", linkStyle)),
                new CellUpdate(1, 0, new Cell("B", Style.EMPTY)));

        backend.draw(updates);
        backend.flush();

        String output = terminal.output();
        String start = "\u001b]8;id=link-1;https://example.com\u001b\\";
        String end = "\u001b]8;;\u001b\\";

        int startIndex = output.indexOf(start);
        int endIndex = output.indexOf(end);
        int bIndex = output.indexOf("B");

        assertThat(startIndex).isGreaterThanOrEqualTo(0);
        assertThat(endIndex).isGreaterThan(startIndex);
        assertThat(bIndex).isGreaterThan(endIndex);
    }

    private static final class FakeTerminal implements PlatformTerminal {
        private final ByteArrayOutputStream output = new ByteArrayOutputStream();

        @Override
        public void enableRawMode() {
        }

        @Override
        public void disableRawMode() {
        }

        @Override
        public Size getSize() {
            return new Size(80, 24);
        }

        @Override
        public int read(int timeoutMs) {
            return -1;
        }

        @Override
        public int peek(int timeoutMs) {
            return -1;
        }

        @Override
        public void write(byte[] data) throws IOException {
            output.write(data);
        }

        @Override
        public void write(byte[] buffer, int offset, int length) throws IOException {
            output.write(buffer, offset, length);
        }

        @Override
        public void write(String s) throws IOException {
            output.write(s.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public Charset getCharset() {
            return StandardCharsets.UTF_8;
        }

        @Override
        public boolean isRawModeEnabled() {
            return false;
        }

        @Override
        public void onResize(Runnable handler) {
        }

        @Override
        public void close() {
        }

        String output() {
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
