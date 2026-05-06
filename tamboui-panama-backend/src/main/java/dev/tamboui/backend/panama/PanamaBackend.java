/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.backend.panama;

import java.io.IOException;
import java.util.Objects;

import dev.tamboui.backend.panama.unix.PlatformConstants;
import dev.tamboui.backend.panama.unix.UnixTerminal;
import dev.tamboui.backend.panama.windows.WindowsTerminal;
import dev.tamboui.layout.Position;
import dev.tamboui.layout.Size;
import dev.tamboui.terminal.AbstractBackend;
import dev.tamboui.terminal.Mode2027Status;
import dev.tamboui.terminal.Mode2027Support;

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
public class PanamaBackend extends AbstractBackend {

    private static final int INITIAL_BUFFER_SIZE = 8192;

    private final PlatformTerminal terminal;
    private final ByteArrayBuilder outputBuffer;
    private boolean inAlternateScreen;
    private boolean mouseEnabled;
    private boolean mode2027Enabled;

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
        this.mode2027Enabled = false;
    }

    PanamaBackend(PlatformTerminal terminal) {
        this.terminal = Objects.requireNonNull(terminal, "terminal");
        this.outputBuffer = new ByteArrayBuilder(INITIAL_BUFFER_SIZE);
        this.inAlternateScreen = false;
        this.mouseEnabled = false;
        this.mode2027Enabled = false;
    }

    private static PlatformTerminal createPlatformTerminal() throws IOException {
        if (PlatformConstants.isWindows()) {
            return new WindowsTerminal();
        } else {
            return new UnixTerminal();
        }
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

        // Query and enable Mode 2027 (grapheme cluster mode) after entering raw mode
        // to prevent the response from being echoed to the terminal
        Mode2027Status status = Mode2027Support.query(this, 500);
        if (status.isSupported()) {
            Mode2027Support.enable(this);
            mode2027Enabled = true;
        }
    }

    @Override
    public void disableRawMode() throws IOException {
        // Disable Mode 2027 if it was enabled
        if (mode2027Enabled) {
            Mode2027Support.disable(this);
            mode2027Enabled = false;
        }

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
    public void beginSynchronizedUpdate() throws IOException {
        outputBuffer.appendAscii(MODE_2026_BSU);
    }

    @Override
    public void endSynchronizedUpdate() throws IOException {
        outputBuffer.appendAscii(MODE_2026_ESU);
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
    public void insertLines(int n) throws IOException {
        if (n <= 0) {
            return;
        }
        outputBuffer.csi().appendInt(n).append((byte) 'L');
    }

    @Override
    public void deleteLines(int n) throws IOException {
        if (n <= 0) {
            return;
        }
        outputBuffer.csi().appendInt(n).append((byte) 'M');
    }

    @Override
    public void moveCursorUp(int n) throws IOException {
        if (n <= 0) {
            return;
        }
        outputBuffer.csi().appendInt(n).append((byte) 'A');
    }

    @Override
    public void moveCursorDown(int n) throws IOException {
        if (n <= 0) {
            return;
        }
        outputBuffer.csi().appendInt(n).append((byte) 'B');
    }

    @Override
    public void moveCursorRight(int n) throws IOException {
        if (n <= 0) {
            return;
        }
        outputBuffer.csi().appendInt(n).append((byte) 'C');
    }

    @Override
    public void moveCursorLeft(int n) throws IOException {
        if (n <= 0) {
            return;
        }
        outputBuffer.csi().appendInt(n).append((byte) 'D');
    }

    @Override
    public void eraseToEndOfLine() throws IOException {
        outputBuffer.csi().append((byte) 'K');
    }

    @Override
    public void carriageReturn() throws IOException {
        outputBuffer.append((byte) '\r');
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
        outputBuffer.append(data);
    }

    @Override
    public void writeRaw(String data) throws IOException {
        outputBuffer.appendUtf8(data);
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
