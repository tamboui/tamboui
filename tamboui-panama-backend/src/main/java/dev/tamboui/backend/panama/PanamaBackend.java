/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.backend.panama;

import dev.tamboui.backend.panama.unix.PlatformConstants;
import dev.tamboui.backend.panama.unix.UnixTerminal;
import dev.tamboui.backend.panama.windows.WindowsTerminal;
import dev.tamboui.buffer.Cell;
import dev.tamboui.buffer.CellUpdate;
import dev.tamboui.layout.Position;
import dev.tamboui.layout.Size;
import dev.tamboui.style.AnsiColor;
import dev.tamboui.style.Color;
import dev.tamboui.style.Modifier;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Backend;

import java.io.IOException;
import java.util.EnumSet;

/**
 * Terminal backend implementation using Panama FFI.
 * <p>
 * This backend provides direct native access to terminal operations
 * without requiring external dependencies like JLine. It uses the
 * Java Foreign Function and Memory API (Panama FFI) to call native
 * platform functions directly.
 * <p>
 * Supports Unix-like systems (Linux and macOS) and Windows.
 */
public class PanamaBackend implements Backend {

    private static final int INITIAL_BUFFER_SIZE = 8192;

    private final PlatformTerminal terminal;
    private final ByteArrayBuilder outputBuffer;
    private boolean inAlternateScreen;
    private boolean mouseEnabled;

    /**
     * Creates a new Panama backend.
     * <p>
     * Automatically detects the platform and creates the appropriate
     * terminal implementation.
     *
     * @throws IOException if the terminal cannot be initialized
     */
    public PanamaBackend() throws IOException {
        this.terminal = createPlatformTerminal();
        this.outputBuffer = new ByteArrayBuilder(INITIAL_BUFFER_SIZE);
        this.inAlternateScreen = false;
        this.mouseEnabled = false;
    }

    private static PlatformTerminal createPlatformTerminal() throws IOException {
        if (PlatformConstants.isWindows()) {
            return new WindowsTerminal();
        } else {
            return new UnixTerminal();
        }
    }

    @Override
    public void draw(Iterable<CellUpdate> updates) throws IOException {
        Style lastStyle = null;

        for (CellUpdate update : updates) {
            // Move cursor
            moveCursor(update.x(), update.y());

            // Apply style if changed
            Cell cell = update.cell();
            if (!cell.style().equals(lastStyle)) {
                applyStyle(cell.style());
                lastStyle = cell.style();
            }

            // Write symbol (may contain UTF-8 multi-byte characters)
            outputBuffer.appendUtf8(cell.symbol());
        }

        // Reset style after drawing
        outputBuffer.csi().appendAscii("0m");
    }

    @Override
    public void flush() throws IOException {
        if (outputBuffer.length() > 0) {
            terminal.write(outputBuffer.buffer(), 0, outputBuffer.length());
            outputBuffer.reset();
        }
    }

    @Override
    public void clear() throws IOException {
        outputBuffer.csi().appendAscii("2J");  // Clear entire screen
        outputBuffer.csi().appendAscii("H");   // Move cursor to home
        flush();
    }

    @Override
    public Size size() throws IOException {
        return terminal.getSize();
    }

    @Override
    public void showCursor() throws IOException {
        outputBuffer.csi().appendAscii("?25h");
        flush();
    }

    @Override
    public void hideCursor() throws IOException {
        outputBuffer.csi().appendAscii("?25l");
        flush();
    }

    @Override
    public Position getCursorPosition() throws IOException {
        // Getting cursor position requires sending a query and parsing the response
        // This is complex to implement reliably, so we return origin as fallback
        return Position.ORIGIN;
    }

    @Override
    public void setCursorPosition(Position position) throws IOException {
        moveCursor(position.x(), position.y());
        flush();
    }

    @Override
    public void enterAlternateScreen() throws IOException {
        outputBuffer.csi().appendAscii("?1049h");
        flush();
        inAlternateScreen = true;
    }

    @Override
    public void leaveAlternateScreen() throws IOException {
        outputBuffer.csi().appendAscii("?1049l");
        flush();
        inAlternateScreen = false;
    }

    @Override
    public void enableRawMode() throws IOException {
        terminal.enableRawMode();
    }

    @Override
    public void disableRawMode() throws IOException {
        terminal.disableRawMode();
    }

    @Override
    public void enableMouseCapture() throws IOException {
        // Enable mouse tracking modes
        outputBuffer.csi().appendAscii("?1000h");  // Normal tracking
        outputBuffer.csi().appendAscii("?1002h");  // Button event tracking
        outputBuffer.csi().appendAscii("?1015h");  // urxvt style
        outputBuffer.csi().appendAscii("?1006h");  // SGR extended mode
        flush();
        mouseEnabled = true;
    }

    @Override
    public void disableMouseCapture() throws IOException {
        outputBuffer.csi().appendAscii("?1006l");
        outputBuffer.csi().appendAscii("?1015l");
        outputBuffer.csi().appendAscii("?1002l");
        outputBuffer.csi().appendAscii("?1000l");
        flush();
        mouseEnabled = false;
    }

    @Override
    public void scrollUp(int lines) throws IOException {
        outputBuffer.csi().appendInt(lines).append((byte) 'S');
        flush();
    }

    @Override
    public void scrollDown(int lines) throws IOException {
        outputBuffer.csi().appendInt(lines).append((byte) 'T');
        flush();
    }

    @Override
    public void onResize(Runnable handler) {
        terminal.onResize(handler);
    }

    @Override
    public int read(int timeoutMs) throws IOException {
        return terminal.read(timeoutMs);
    }

    @Override
    public int peek(int timeoutMs) throws IOException {
        return terminal.peek(timeoutMs);
    }

    @Override
    public void writeRaw(byte[] data) throws IOException {
        terminal.write(data);
    }

    @Override
    public void writeRaw(String data) throws IOException {
        terminal.write(data);
    }

    @Override
    public void close() throws IOException {
        try {
            // Reset state
            outputBuffer.csi().appendAscii("0m");  // Reset style

            if (mouseEnabled) {
                disableMouseCapture();
            }

            if (inAlternateScreen) {
                leaveAlternateScreen();
            }

            showCursor();
            flush();
        } finally {
            terminal.close();
        }
    }

    private void moveCursor(int x, int y) {
        // ANSI uses 1-based coordinates
        outputBuffer.csi()
                .appendInt(y + 1)
                .append((byte) ';')
                .appendInt(x + 1)
                .append((byte) 'H');
    }

    private void applyStyle(Style style) {
        outputBuffer.csi().append((byte) '0');  // Reset first

        // Foreground color
        style.fg().ifPresent(color -> {
            outputBuffer.append((byte) ';');
            appendColorToAnsi(color, true);
        });

        // Background color
        style.bg().ifPresent(color -> {
            outputBuffer.append((byte) ';');
            appendColorToAnsi(color, false);
        });

        // Modifiers
        EnumSet<Modifier> modifiers = style.effectiveModifiers();
        for (Modifier mod : modifiers) {
            outputBuffer.append((byte) ';').appendInt(mod.code());
        }

        // Underline color (if supported)
        style.underlineColor().ifPresent(color -> {
            outputBuffer.append((byte) ';');
            appendUnderlineColorToAnsi(color);
        });

        outputBuffer.append((byte) 'm');
    }

    private void appendColorToAnsi(Color color, boolean foreground) {
        if (color instanceof Color.Reset) {
            outputBuffer.appendInt(foreground ? 39 : 49);
        } else if (color instanceof Color.Ansi ansi) {
            outputBuffer.appendInt(foreground ? ansi.color().fgCode() : ansi.color().bgCode());
        } else if (color instanceof Color.Indexed indexed) {
            outputBuffer.appendInt(foreground ? 38 : 48)
                    .appendAscii(";5;")
                    .appendInt(indexed.index());
        } else if (color instanceof Color.Rgb rgb) {
            outputBuffer.appendInt(foreground ? 38 : 48)
                    .appendAscii(";2;")
                    .appendInt(rgb.r())
                    .append((byte) ';')
                    .appendInt(rgb.g())
                    .append((byte) ';')
                    .appendInt(rgb.b());
        }
    }

    private void appendUnderlineColorToAnsi(Color color) {
        if (color instanceof Color.Indexed indexed) {
            outputBuffer.appendAscii("58;5;").appendInt(indexed.index());
        } else if (color instanceof Color.Rgb rgb) {
            outputBuffer.appendAscii("58;2;")
                    .appendInt(rgb.r())
                    .append((byte) ';')
                    .appendInt(rgb.g())
                    .append((byte) ';')
                    .appendInt(rgb.b());
        }
    }

    /**
     * Returns the underlying Unix terminal for advanced operations.
     * <p>
     * This method is only available when running on Unix-like systems.
     *
     * @return the Unix terminal instance, or null if running on Windows
     */
    public UnixTerminal unixTerminal() {
        if (terminal instanceof UnixTerminal unixTerminal) {
            return unixTerminal;
        }
        return null;
    }

    /**
     * Returns the underlying Windows terminal for advanced operations.
     * <p>
     * This method is only available when running on Windows.
     *
     * @return the Windows terminal instance, or null if running on Unix
     */
    public WindowsTerminal windowsTerminal() {
        if (terminal instanceof WindowsTerminal windowsTerminal) {
            return windowsTerminal;
        }
        return null;
    }

    /**
     * Returns the underlying platform terminal.
     *
     * @return the platform terminal instance
     */
    public PlatformTerminal platformTerminal() {
        return terminal;
    }
}
