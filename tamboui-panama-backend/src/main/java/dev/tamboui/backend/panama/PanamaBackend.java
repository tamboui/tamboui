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

    private static final String ESC = "\033";
    private static final String CSI = ESC + "[";

    private final PlatformTerminal terminal;
    private final StringBuilder outputBuffer;
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
        this.outputBuffer = new StringBuilder(4096);
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

            // Write symbol
            outputBuffer.append(cell.symbol());
        }

        // Reset style after drawing
        outputBuffer.append(CSI).append("0m");
    }

    @Override
    public void flush() throws IOException {
        if (outputBuffer.length() > 0) {
            terminal.write(outputBuffer.toString());
            outputBuffer.setLength(0);
        }
    }

    @Override
    public void clear() throws IOException {
        outputBuffer.append(CSI).append("2J");  // Clear entire screen
        outputBuffer.append(CSI).append("H");   // Move cursor to home
        flush();
    }

    @Override
    public Size size() throws IOException {
        return terminal.getSize();
    }

    @Override
    public void showCursor() throws IOException {
        outputBuffer.append(CSI).append("?25h");
        flush();
    }

    @Override
    public void hideCursor() throws IOException {
        outputBuffer.append(CSI).append("?25l");
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
        outputBuffer.append(CSI).append("?1049h");
        flush();
        inAlternateScreen = true;
    }

    @Override
    public void leaveAlternateScreen() throws IOException {
        outputBuffer.append(CSI).append("?1049l");
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
        outputBuffer.append(CSI).append("?1000h");  // Normal tracking
        outputBuffer.append(CSI).append("?1002h");  // Button event tracking
        outputBuffer.append(CSI).append("?1015h");  // urxvt style
        outputBuffer.append(CSI).append("?1006h");  // SGR extended mode
        flush();
        mouseEnabled = true;
    }

    @Override
    public void disableMouseCapture() throws IOException {
        outputBuffer.append(CSI).append("?1006l");
        outputBuffer.append(CSI).append("?1015l");
        outputBuffer.append(CSI).append("?1002l");
        outputBuffer.append(CSI).append("?1000l");
        flush();
        mouseEnabled = false;
    }

    @Override
    public void scrollUp(int lines) throws IOException {
        outputBuffer.append(CSI).append(lines).append("S");
        flush();
    }

    @Override
    public void scrollDown(int lines) throws IOException {
        outputBuffer.append(CSI).append(lines).append("T");
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
    public void close() throws IOException {
        try {
            // Reset state
            outputBuffer.append(CSI).append("0m");  // Reset style

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
        outputBuffer.append(CSI).append(y + 1).append(";").append(x + 1).append("H");
    }

    private void applyStyle(Style style) {
        outputBuffer.append(CSI).append("0");  // Reset first

        // Foreground color
        style.fg().ifPresent(color -> {
            outputBuffer.append(";");
            outputBuffer.append(colorToAnsi(color, true));
        });

        // Background color
        style.bg().ifPresent(color -> {
            outputBuffer.append(";");
            outputBuffer.append(colorToAnsi(color, false));
        });

        // Modifiers
        EnumSet<Modifier> modifiers = style.effectiveModifiers();
        for (Modifier mod : modifiers) {
            outputBuffer.append(";").append(mod.code());
        }

        // Underline color (if supported)
        style.underlineColor().ifPresent(color -> {
            outputBuffer.append(";");
            outputBuffer.append(underlineColorToAnsi(color));
        });

        outputBuffer.append("m");
    }

    private String colorToAnsi(Color color, boolean foreground) {
        if (color instanceof Color.Reset) {
            return foreground ? "39" : "49";
        } else if (color instanceof Color.Ansi) {
            AnsiColor c = ((Color.Ansi) color).color();
            return String.valueOf(foreground ? c.fgCode() : c.bgCode());
        } else if (color instanceof Color.Indexed) {
            int idx = ((Color.Indexed) color).index();
            return (foreground ? "38;5;" : "48;5;") + idx;
        } else if (color instanceof Color.Rgb rgb) {
            return (foreground ? "38;2;" : "48;2;") + rgb.r() + ";" + rgb.g() + ";" + rgb.b();
        }
        return "";
    }

    private String underlineColorToAnsi(Color color) {
        if (color instanceof Color.Indexed) {
            int idx = ((Color.Indexed) color).index();
            return "58;5;" + idx;
        } else if (color instanceof Color.Rgb rgb) {
            return "58;2;" + rgb.r() + ";" + rgb.g() + ";" + rgb.b();
        }
        return "";
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
