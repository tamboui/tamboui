/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.backend.aesh;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.aesh.terminal.Connection;
import org.aesh.terminal.tty.Point;
import org.aesh.terminal.tty.TerminalConnection;

import dev.tamboui.buffer.Cell;
import dev.tamboui.buffer.CellUpdate;
import dev.tamboui.layout.Position;
import dev.tamboui.layout.Size;
import dev.tamboui.style.Hyperlink;
import dev.tamboui.style.Modifier;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.AnsiStringBuilder;
import dev.tamboui.terminal.Backend;
import dev.tamboui.terminal.Mode2027Status;
import dev.tamboui.terminal.Mode2027Support;

/**
 * Aesh Readline based backend for terminal operations.
 * <p>
 * This backend uses the aesh-readline library's TerminalConnection abstraction
 * for terminal I/O operations.
 */
public class AeshBackend implements Backend {

    private static final String ESC = "\033";
    private static final String CSI = ESC + "[";

    private final Connection connection;
    private final StringBuilder outputBuffer;
    private final BlockingQueue<Integer> inputQueue;
    private boolean inAlternateScreen;
    private boolean mouseEnabled;
    private boolean mode2027Enabled;
    private Runnable resizeHandler;

    /**
     * Creates a new Aesh backend using a default TerminalConnection.
     *
     * @throws IOException if the terminal cannot be initialized
     */
    public AeshBackend() throws IOException {
        this(new TerminalConnection());
    }

    /**
     * Creates a new Aesh backend using the provided Connection.
     * <p>
     * This constructor allows creating backends from SSH or HTTP connections.
     *
     * @param connection the connection to use for terminal I/O
     * @throws IOException if the terminal cannot be initialized
     */
    public AeshBackend(Connection connection) throws IOException {
        this.connection = Objects.requireNonNull(connection, "connection cannot be null");
        this.connection.openNonBlocking();
        this.outputBuffer = new StringBuilder();
        this.inputQueue = new LinkedBlockingQueue<>();
        this.inAlternateScreen = false;
        this.mouseEnabled = false;
        this.mode2027Enabled = false;

        // Set up input handler to queue characters
        connection.setStdinHandler(ints -> {
            for (int codePoint : ints) {
                inputQueue.offer(codePoint);
            }
        });

        // Set up resize handler
        connection.setSizeHandler(size -> {
            if (resizeHandler != null) {
                resizeHandler.run();
            }
        });
    }

    @Override
    public void draw(Iterable<CellUpdate> updates) throws IOException {
        Style lastStyle = null;
        Hyperlink lastHyperlink = null;

        for (CellUpdate update : updates) {
            Cell cell = update.cell();

            // Skip continuation cells - the terminal fills them automatically
            // when printing a wide character
            if (cell.isContinuation()) {
                continue;
            }

            // Move cursor
            moveCursor(update.x(), update.y());

            // Apply style if changed
            if (!cell.style().equals(lastStyle)) {
                // Check if hyperlink changed
                Hyperlink currentHyperlink = cell.style().hyperlink().orElse(null);
                if (!Objects.equals(currentHyperlink, lastHyperlink)) {
                    // End previous hyperlink if any
                    if (lastHyperlink != null) {
                        outputBuffer.append(AnsiStringBuilder.hyperlinkEnd());
                    }
                    // Start new hyperlink if any
                    if (currentHyperlink != null) {
                        outputBuffer.append(AnsiStringBuilder.hyperlinkStart(currentHyperlink));
                    }
                    lastHyperlink = currentHyperlink;
                }

                applyStyle(cell.style());
                lastStyle = cell.style();
            }

            // Write symbol
            outputBuffer.append(cell.symbol());
        }

        // End any active hyperlink
        if (lastHyperlink != null) {
            outputBuffer.append(AnsiStringBuilder.hyperlinkEnd());
        }

        // Reset style after drawing
        outputBuffer.append(CSI).append("0m");
    }

    @Override
    public void flush() throws IOException {
        if (outputBuffer.length() > 0) {
            connection.write(outputBuffer.toString());
            outputBuffer.setLength(0);
        }
    }

    @Override
    public void clear() throws IOException {
        outputBuffer.append(CSI).append("2J");  // Clear entire screen
        outputBuffer.append(CSI).append("H");    // Move cursor to home
        flush();
    }

    @Override
    public Size size() throws IOException {
        try {
            org.aesh.terminal.tty.Size aeshSize = connection.size();
            return new Size(aeshSize.getWidth(), aeshSize.getHeight());
        } catch (Exception e) {
            throw new IOException("Failed to get terminal size", e);
        }
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
        try {
            Point point = connection.getCursorPosition();
            if (point != null) {
                return new Position(point.x(), point.y());
            }
        } catch (Exception e) {
            // Fall through to return origin
        }
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
        connection.enterRawMode();
        // Query and enable Mode 2027 (grapheme cluster mode) after entering raw mode
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
        // Restore original attributes - Connection doesn't have a direct disableRawMode,
        // but enterRawMode() returns the previous attributes which we could save/restore
        // For now, we'll rely on the connection's internal state management
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
    public void insertLines(int n) throws IOException {
        if (n <= 0) {
            return;
        }
        outputBuffer.append(CSI).append(n).append("L");
    }

    @Override
    public void deleteLines(int n) throws IOException {
        if (n <= 0) {
            return;
        }
        outputBuffer.append(CSI).append(n).append("M");
    }

    @Override
    public void moveCursorUp(int n) throws IOException {
        if (n <= 0) {
            return;
        }
        outputBuffer.append(CSI).append(n).append("A");
    }

    @Override
    public void moveCursorDown(int n) throws IOException {
        if (n <= 0) {
            return;
        }
        outputBuffer.append(CSI).append(n).append("B");
    }

    @Override
    public void moveCursorRight(int n) throws IOException {
        if (n <= 0) {
            return;
        }
        outputBuffer.append(CSI).append(n).append("C");
    }

    @Override
    public void moveCursorLeft(int n) throws IOException {
        if (n <= 0) {
            return;
        }
        outputBuffer.append(CSI).append(n).append("D");
    }

    @Override
    public void eraseToEndOfLine() throws IOException {
        outputBuffer.append(CSI).append("K");
    }

    @Override
    public void carriageReturn() throws IOException {
        outputBuffer.append("\r");
    }

    @Override
    public void writeRaw(byte[] data) throws IOException {
        String str = new String(data, StandardCharsets.UTF_8);
        connection.write(str);
    }

    @Override
    public void writeRaw(String data) throws IOException {
        connection.write(data);
    }

    @Override
    public void onResize(Runnable handler) {
        this.resizeHandler = handler;
    }

    @Override
    public int read(int timeoutMs) throws IOException {
        try {
            Integer ch = null;
            if (timeoutMs < 0) {
                // Blocking read
                ch = inputQueue.take();
            } else if (timeoutMs == 0) {
                // Non-blocking read
                ch = inputQueue.poll();
            } else {
                // Timeout read
                ch = inputQueue.poll(timeoutMs, TimeUnit.MILLISECONDS);
            }

            if (ch == null) {
                return -2;  // Timeout
            }
            return ch;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return -2;
        }
    }

    @Override
    public int peek(int timeoutMs) throws IOException {
        Integer val = inputQueue.peek(); // TODO: we just return if nothing in the queue - do we need to wait?
        return val == null ? -2 : val;
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
            disableRawMode();

            flush();
        } finally {
            connection.close();
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
            outputBuffer.append(color.toAnsiForeground());
        });

        // Background color
        style.bg().ifPresent(color -> {
            outputBuffer.append(";");
            outputBuffer.append(color.toAnsiBackground());
        });

        // Modifiers
        EnumSet<Modifier> modifiers = style.effectiveModifiers();
        for (Modifier mod : modifiers) {
            outputBuffer.append(";").append(mod.code());
        }

        // Underline color (if supported)
        style.underlineColor().ifPresent(color -> {
            outputBuffer.append(";");
            outputBuffer.append(color.toAnsiUnderline());
        });

        outputBuffer.append("m");
    }

    /**
     * Returns the underlying TerminalConnection for advanced operations.
     *
     * @return the TerminalConnection instance
     */
    public Connection terminalConnection() {
        return connection;
    }
}
