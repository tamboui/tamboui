/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.backend.aesh;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.aesh.terminal.Attributes;
import org.aesh.terminal.Connection;
import org.aesh.terminal.tty.MouseTracking;
import org.aesh.terminal.tty.Point;
import org.aesh.terminal.tty.TerminalConnection;
import org.aesh.terminal.utils.ANSI;

import dev.tamboui.layout.Position;
import dev.tamboui.layout.Size;
import dev.tamboui.terminal.AbstractBackend;
import dev.tamboui.terminal.Mode2027Status;
import dev.tamboui.terminal.Mode2027Support;

/**
 * Aesh Readline based backend for terminal operations.
 * <p>
 * This backend uses the aesh-readline library's TerminalConnection abstraction
 * for terminal I/O operations.
 * <p>
 * Two construction modes are supported:
 * <ul>
 *   <li>{@link #AeshBackend()} — creates and owns a system terminal connection;
 *       {@link #close()} will close it.</li>
 *   <li>{@link #AeshBackend(Connection)} — uses an externally provided connection
 *       (e.g., from SSH or HTTP); the caller retains ownership and {@link #close()}
 *       will <em>not</em> close the connection.</li>
 * </ul>
 */
public class AeshBackend extends AbstractBackend {

    private static final String CSI = "\033[";

    private final Connection connection;
    private final boolean ownConnection;
    private final StringBuilder outputBuffer;
    private final BlockingQueue<Integer> inputQueue;
    private Attributes savedAttributes;
    private boolean inAlternateScreen;
    private boolean mouseEnabled;
    private boolean mode2027Enabled;
    private Runnable resizeHandler;

    /**
     * Creates a new Aesh backend using a default TerminalConnection.
     * <p>
     * The backend owns the connection and will close it when {@link #close()} is called.
     *
     * @throws IOException if the terminal cannot be initialized
     */
    public AeshBackend() throws IOException {
        this(new TerminalConnection(), true);
    }

    /**
     * Creates a new Aesh backend using the provided Connection.
     * <p>
     * This constructor allows creating backends from SSH or HTTP connections.
     * The caller retains ownership of the connection; it will not be closed
     * when this backend is closed.
     *
     * @param connection the connection to use for terminal I/O
     * @throws IOException if the terminal cannot be initialized
     */
    public AeshBackend(Connection connection) throws IOException {
        this(connection, false);
    }

    private AeshBackend(Connection connection, boolean ownConnection) throws IOException {
        this.connection = Objects.requireNonNull(connection, "connection cannot be null");
        this.ownConnection = ownConnection;
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
    public void flush() throws IOException {
        if (outputBuffer.length() > 0) {
            connection.write(outputBuffer.toString());
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
        try {
            org.aesh.terminal.tty.Size aeshSize = connection.size();
            return new Size(aeshSize.getWidth(), aeshSize.getHeight());
        } catch (Exception e) {
            throw new IOException("Failed to get terminal size", e);
        }
    }

    @Override
    public void showCursor() throws IOException {
        outputBuffer.append(ANSI.CURSOR_SHOW);
        flush();
    }

    @Override
    public void hideCursor() throws IOException {
        outputBuffer.append(ANSI.CURSOR_HIDE);
        flush();
    }

    @Override
    public Position getCursorPosition() throws IOException {
        try {
            if (connection.terminal() != null) {
                Point point = connection.terminal().getCursorPosition();
                if (point != null) {
                    return new Position(point.x(), point.y());
                }
            }
        } catch (Exception e) {
            // Fall through to return origin
        }
        return Position.ORIGIN;
    }

    @Override
    public void enterAlternateScreen() throws IOException {
        outputBuffer.append(ANSI.ALTERNATE_BUFFER);
        flush();
        inAlternateScreen = true;
    }

    @Override
    public void leaveAlternateScreen() throws IOException {
        outputBuffer.append(ANSI.MAIN_BUFFER);
        flush();
        inAlternateScreen = false;
    }

    @Override
    public void enableRawMode() throws IOException {
        savedAttributes = connection.enterRawMode();
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
        // Restore original terminal attributes
        if (savedAttributes != null) {
            connection.setAttributes(savedAttributes);
            savedAttributes = null;
        }
    }

    @Override
    public void enableMouseCapture() throws IOException {
        MouseTracking.enable(outputBuffer, MouseTracking.Protocol.NORMAL);
        MouseTracking.enable(outputBuffer, MouseTracking.Protocol.BUTTON_MOTION);
        MouseTracking.enableEncoding(outputBuffer, MouseTracking.Encoding.URXVT);
        MouseTracking.enableEncoding(outputBuffer, MouseTracking.Encoding.SGR);
        flush();
        mouseEnabled = true;
    }

    @Override
    public void disableMouseCapture() throws IOException {
        MouseTracking.disableEncoding(outputBuffer, MouseTracking.Encoding.SGR);
        MouseTracking.disableEncoding(outputBuffer, MouseTracking.Encoding.URXVT);
        MouseTracking.disable(outputBuffer, MouseTracking.Protocol.BUTTON_MOTION);
        MouseTracking.disable(outputBuffer, MouseTracking.Protocol.NORMAL);
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
        outputBuffer.append(new String(data, StandardCharsets.UTF_8));
    }

    @Override
    public void writeRaw(String data) throws IOException {
        outputBuffer.append(data);
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

    /**
     * Closes this backend, resetting terminal state (mouse capture, alternate screen,
     * cursor visibility, raw mode).
     * <p>
     * If this backend owns the connection (created via {@link #AeshBackend()}), the
     * connection is closed. If an external connection was provided via
     * {@link #AeshBackend(Connection)}, it is left open for the caller to manage.
     */
    @Override
    public void close() throws IOException {
        try {
            // Reset state
            outputBuffer.append(ANSI.RESET);

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
            if (ownConnection) {
                connection.close();
            }
        }
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
