/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.backend.jline3;

import java.io.IOException;
import java.io.PrintWriter;

import org.jline.terminal.Attributes;
import org.jline.terminal.Terminal;
import org.jline.terminal.Terminal.Signal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp;
import org.jline.utils.NonBlockingReader;

import dev.tamboui.layout.Position;
import dev.tamboui.layout.Size;
import dev.tamboui.terminal.AbstractBackend;
import dev.tamboui.terminal.Mode2027Status;
import dev.tamboui.terminal.Mode2027Support;

/**
 * JLine 3 based backend for terminal operations.
 */
public class JLineBackend extends AbstractBackend {

    private static final String ESC = "\033";
    private static final String CSI = ESC + "[";
    private static final boolean IS_WINDOWS = System.getProperty("os.name", "").toLowerCase().contains("win");

    private final Terminal terminal;
    private final PrintWriter writer;
    private final NonBlockingReader reader;
    private Attributes savedAttributes;
    private boolean inAlternateScreen;
    private boolean mouseEnabled;
    private boolean mode2027Enabled;

    /**
     * Creates a new JLine 3 backend using the system terminal.
     *
     * @throws IOException if the terminal cannot be opened
     */
    public JLineBackend() throws IOException {
        this.terminal = TerminalBuilder.builder()
            .system(true)
            .jansi(true)
            .build();
        this.writer = terminal.writer();
        this.reader = terminal.reader();
        this.inAlternateScreen = false;
        this.mouseEnabled = false;
        this.mode2027Enabled = false;
    }

    @Override
    public void flush() throws IOException {
        writer.flush();
    }

    @Override
    public void clear() throws IOException {
        writer.print(CSI + "2J");  // Clear entire screen
        writer.print(CSI + "H");    // Move cursor to home
        writer.flush();
    }

    @Override
    public Size size() throws IOException {
        return new Size(terminal.getWidth(), terminal.getHeight());
    }

    @Override
    public void showCursor() throws IOException {
        writer.print(CSI + "?25h");
        writer.flush();
    }

    @Override
    public void hideCursor() throws IOException {
        writer.print(CSI + "?25l");
        writer.flush();
    }

    @Override
    public Position getCursorPosition() throws IOException {
        // JLine doesn't provide a direct way to query cursor position
        // Return origin as fallback
        return Position.ORIGIN;
    }

    @Override
    public void enterAlternateScreen() throws IOException {
        terminal.puts(InfoCmp.Capability.enter_ca_mode);
        writer.flush();
        inAlternateScreen = true;
    }

    @Override
    public void leaveAlternateScreen() throws IOException {
        terminal.puts(InfoCmp.Capability.exit_ca_mode);
        writer.flush();
        inAlternateScreen = false;
    }

    @Override
    public void enableRawMode() throws IOException {
        savedAttributes = terminal.getAttributes();
        terminal.enterRawMode();
        // Disable signal generation so Ctrl+C goes through the event system
        // instead of generating SIGINT. This allows bindings to control quit behavior.
        Attributes attrs = terminal.getAttributes();
        attrs.setLocalFlag(Attributes.LocalFlag.ISIG, false);
        terminal.setAttributes(attrs);

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

        if (savedAttributes != null) {
            terminal.setAttributes(savedAttributes);
        }
    }

    @Override
    public void enableMouseCapture() throws IOException {
        // On Windows, use JLine3's native mouse tracking API.
        // This sets the correct console mode (ENABLE_MOUSE_INPUT) via Win32 API,
        // enabling mouse events in CMD/PowerShell. JLine3 then translates native
        // mouse events into X10 escape sequences for EventParser to parse.
        // Only enable on Windows to avoid duplicate events on Unix terminals.
        if (IS_WINDOWS) {
            terminal.trackMouse(Terminal.MouseTracking.Any);
        }

        // Send ANSI escape sequences for terminals that support them
        // (e.g. Windows Terminal, xterm, etc.)
        writer.print(CSI + "?1000h");  // Normal tracking
        writer.print(CSI + "?1002h");  // Button event tracking
        writer.print(CSI + "?1015h");  // urxvt style
        writer.print(CSI + "?1006h");  // SGR extended mode
        writer.flush();
        mouseEnabled = true;
    }

    @Override
    public void disableMouseCapture() throws IOException {
        // Disable JLine3 native mouse tracking (Windows only)
        if (IS_WINDOWS) {
            terminal.trackMouse(Terminal.MouseTracking.Off);
        }

        writer.print(CSI + "?1006l");
        writer.print(CSI + "?1015l");
        writer.print(CSI + "?1002l");
        writer.print(CSI + "?1000l");
        writer.flush();
        mouseEnabled = false;
    }

    @Override
    public void scrollUp(int lines) throws IOException {
        writer.print(CSI + lines + "S");
        writer.flush();
    }

    @Override
    public void scrollDown(int lines) throws IOException {
        writer.print(CSI + lines + "T");
        writer.flush();
    }

    @Override
    public void insertLines(int n) throws IOException {
        if (n <= 0) {
            return;
        }
        writer.print(CSI + n + "L");
    }

    @Override
    public void deleteLines(int n) throws IOException {
        if (n <= 0) {
            return;
        }
        writer.print(CSI + n + "M");
    }

    @Override
    public void moveCursorUp(int n) throws IOException {
        if (n <= 0) {
            return;
        }
        writer.print(CSI + n + "A");
    }

    @Override
    public void moveCursorDown(int n) throws IOException {
        if (n <= 0) {
            return;
        }
        writer.print(CSI + n + "B");
    }

    @Override
    public void moveCursorRight(int n) throws IOException {
        if (n <= 0) {
            return;
        }
        writer.print(CSI + n + "C");
    }

    @Override
    public void moveCursorLeft(int n) throws IOException {
        if (n <= 0) {
            return;
        }
        writer.print(CSI + n + "D");
    }

    @Override
    public void eraseToEndOfLine() throws IOException {
        writer.print(CSI + "K");
    }

    @Override
    public void carriageReturn() throws IOException {
        writer.print("\r");
    }

    @Override
    public void writeRaw(byte[] data) throws IOException {
        terminal.output().write(data);
    }

    @Override
    public void writeRaw(String data) {
        writer.print(data);
    }

    @Override
    public void onResize(Runnable handler) {
        terminal.handle(Signal.WINCH, signal -> handler.run());
    }

    @Override
    public int read(int timeoutMs) throws IOException {
        return reader.read(timeoutMs);
    }

    @Override
    public int peek(int timeoutMs) throws IOException {
        return reader.peek(timeoutMs);
    }

    @Override
    public void close() throws IOException {
        // Reset state
        writer.print(CSI + "0m");  // Reset style

        if (mouseEnabled) {
            disableMouseCapture();
        }

        if (inAlternateScreen) {
            leaveAlternateScreen();
        }

        showCursor();
        disableRawMode();

        writer.flush();
        terminal.close();
    }

    /**
     * Returns the underlying JLine terminal for advanced operations.
     *
     * @return the JLine terminal instance
     */
    public Terminal jlineTerminal() {
        return terminal;
    }
}
