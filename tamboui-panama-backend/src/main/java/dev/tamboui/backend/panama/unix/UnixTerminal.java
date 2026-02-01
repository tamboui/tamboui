/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.backend.panama.unix;

import dev.tamboui.terminal.BackendException;
import dev.tamboui.backend.panama.PlatformTerminal;
import dev.tamboui.error.RuntimeIOException;
import dev.tamboui.layout.Size;

import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.VarHandle;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Locale;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Unix terminal operations using Panama FFI.
 * <p>
 * This class provides higher-level terminal operations built on top of
 * the low-level libc bindings in {@link LibC}.
 */
public final class UnixTerminal implements PlatformTerminal {

    private static final VarHandle WS_ROW = LibC.WINSIZE_LAYOUT.varHandle(
            MemoryLayout.PathElement.groupElement("ws_row"));
    private static final VarHandle WS_COL = LibC.WINSIZE_LAYOUT.varHandle(
            MemoryLayout.PathElement.groupElement("ws_col"));

    private static final VarHandle POLLFD_FD = LibC.POLLFD_LAYOUT.varHandle(
            MemoryLayout.PathElement.groupElement("fd"));
    private static final VarHandle POLLFD_EVENTS = LibC.POLLFD_LAYOUT.varHandle(
            MemoryLayout.PathElement.groupElement("events"));
    private static final VarHandle POLLFD_REVENTS = LibC.POLLFD_LAYOUT.varHandle(
            MemoryLayout.PathElement.groupElement("revents"));

    private static final VarHandle TERMIOS_IFLAG = LibC.TERMIOS_LAYOUT.varHandle(
            MemoryLayout.PathElement.groupElement("c_iflag"));
    private static final VarHandle TERMIOS_OFLAG = LibC.TERMIOS_LAYOUT.varHandle(
            MemoryLayout.PathElement.groupElement("c_oflag"));
    private static final VarHandle TERMIOS_CFLAG = LibC.TERMIOS_LAYOUT.varHandle(
            MemoryLayout.PathElement.groupElement("c_cflag"));
    private static final VarHandle TERMIOS_LFLAG = LibC.TERMIOS_LAYOUT.varHandle(
            MemoryLayout.PathElement.groupElement("c_lflag"));

    private static final String DEV_TTY = "/dev/tty";

    // Offset to c_cc array in termios struct (platform-specific)
    private static final long TERMIOS_CC_OFFSET = PlatformConstants.TERMIOS_CC_OFFSET;

    // Environment variables to check for charset detection, in order of precedence
    private static final String[] LOCALE_ENV_VARS = {"LC_ALL", "LC_CTYPE", "LANG"};

    // Size of the reusable write buffer
    private static final int WRITE_BUFFER_SIZE = 8192;

    private final Arena arena;
    private final MemorySegment savedTermios;
    private final MemorySegment currentTermios;
    private final MemorySegment winsize;
    private final MemorySegment pollfd;
    private final MemorySegment readBuffer;
    private final MemorySegment writeBuffer;
    private final int ttyFd;
    private final Charset charset;

    private boolean rawModeEnabled;
    private int peekedChar = -2;
    private final ReentrantLock resizeLock = new ReentrantLock();
    private Runnable resizeHandler;
    private boolean resizePending;
    private MemorySegment previousSigaction;  // Previous sigaction struct (for restoration)
    private Arena signalArena;

    /**
     * Creates a new Unix terminal instance.
     *
     * @throws IOException if the terminal cannot be initialized
     */
    public UnixTerminal() throws IOException {
        // On macOS, use stdin directly for input (poll doesn't work well with /dev/tty)
        // On Linux, open /dev/tty to bypass any stdin/stdout redirection
        int fd;
        if (PlatformConstants.isMacOS()) {
            fd = LibC.STDIN_FILENO;
        } else {
            fd = LibC.open(DEV_TTY, LibC.O_RDWR);
            if (fd < 0) {
                throw new RuntimeIOException("Failed to open " + DEV_TTY + " (errno=" + LibC.getLastErrno() + ")");
            }
        }
        this.ttyFd = fd;
        this.charset = detectCharset();

        this.arena = Arena.ofShared();
        this.savedTermios = LibC.allocateTermios(arena);
        this.currentTermios = LibC.allocateTermios(arena);
        this.winsize = LibC.allocateWinsize(arena);
        this.pollfd = LibC.allocatePollfd(arena);
        this.readBuffer = arena.allocate(1);
        this.writeBuffer = arena.allocate(WRITE_BUFFER_SIZE);
        this.rawModeEnabled = false;

        // Save original terminal attributes
        int tcgetattrResult = LibC.tcgetattr(ttyFd, savedTermios);
        if (tcgetattrResult != 0) {
            LibC.close(ttyFd);
            arena.close();
            throw new RuntimeIOException("Failed to get terminal attributes");
        }

        // Copy to current
        MemorySegment.copy(savedTermios, 0, currentTermios, 0, LibC.TERMIOS_LAYOUT.byteSize());
    }

    /**
     * Detects the terminal charset from environment variables.
     * <p>
     * Checks LC_ALL, LC_CTYPE, and LANG in order of precedence.
     * Falls back to UTF-8 if no encoding is detected or if the
     * detected encoding is not supported.
     *
     * @return the detected charset, or UTF-8 as default
     */
    private static Charset detectCharset() {
        // Check environment variables in order of precedence
        for (var envVar : LOCALE_ENV_VARS) {
            var value = System.getenv(envVar);
            if (value != null && !value.isEmpty()) {
                var detected = parseCharsetFromLocale(value);
                if (detected != null) {
                    return detected;
                }
            }
        }
        // Default to UTF-8
        return StandardCharsets.UTF_8;
    }

    /**
     * Parses a charset from a locale string like "en_US.UTF-8" or "C.UTF-8".
     *
     * @param locale the locale string
     * @return the parsed charset, or null if not found or not supported
     */
    private static Charset parseCharsetFromLocale(String locale) {
        var upper = locale.toUpperCase(Locale.ROOT);

        // Handle common UTF-8 patterns
        if (upper.contains("UTF-8") || upper.contains("UTF8")) {
            return StandardCharsets.UTF_8;
        }

        // Handle explicit charset after dot (e.g., "en_US.ISO-8859-1")
        var dotIndex = locale.indexOf('.');
        if (dotIndex >= 0 && dotIndex < locale.length() - 1) {
            var charsetPart = locale.substring(dotIndex + 1);
            // Remove any modifier after @ (e.g., "UTF-8@euro")
            var atIndex = charsetPart.indexOf('@');
            if (atIndex >= 0) {
                charsetPart = charsetPart.substring(0, atIndex);
            }
            try {
                return Charset.forName(charsetPart);
            } catch (UnsupportedCharsetException e) {
                // Fall through to default
            }
        }

        // "C" or "POSIX" locale typically means ASCII, but UTF-8 is safer for TUI
        if ("C".equals(locale) || "POSIX".equals(locale)) {
            return StandardCharsets.UTF_8;
        }

        return null;
    }

    /**
     * Enables raw mode on the terminal.
     * <p>
     * Raw mode disables line buffering, echo, and signal processing,
     * allowing direct character-by-character input.
     *
     * @throws IOException if raw mode cannot be enabled
     */
    public void enableRawMode() throws IOException {
        if (rawModeEnabled) {
            return;
        }

        // Re-read current attributes
        if (LibC.tcgetattr(ttyFd, currentTermios) != 0) {
            throw new RuntimeIOException("Failed to get terminal attributes");
        }

        // Get current flags
        var iflag = getTermiosFlag(TERMIOS_IFLAG);
        var oflag = getTermiosFlag(TERMIOS_OFLAG);
        var cflag = getTermiosFlag(TERMIOS_CFLAG);
        var lflag = getTermiosFlag(TERMIOS_LFLAG);

        // Disable various input processing
        iflag &= ~(LibC.BRKINT | LibC.ICRNL | LibC.INPCK | LibC.ISTRIP | LibC.IXON);

        // Disable output processing
        oflag &= ~(LibC.OPOST);

        // Set character size to 8 bits
        cflag |= LibC.CS8;

        // Disable echo, canonical mode, signals, and extended functions
        lflag &= ~(LibC.ECHO | LibC.ICANON | LibC.IEXTEN | LibC.ISIG);

        // Set the modified flags
        setTermiosFlag(TERMIOS_IFLAG, iflag);
        setTermiosFlag(TERMIOS_OFLAG, oflag);
        setTermiosFlag(TERMIOS_CFLAG, cflag);
        setTermiosFlag(TERMIOS_LFLAG, lflag);

        // Set VMIN and VTIME to 0 for non-blocking reads
        clearControlChar(currentTermios, LibC.VMIN);
        clearControlChar(currentTermios, LibC.VTIME);

        if (LibC.tcsetattr(ttyFd, LibC.TCSAFLUSH, currentTermios) != 0) {
            throw new RuntimeIOException("Failed to set terminal attributes");
        }

        rawModeEnabled = true;
    }

    /**
     * Disables raw mode and restores original terminal attributes.
     *
     * @throws IOException if raw mode cannot be disabled
     */
    public void disableRawMode() throws IOException {
        if (!rawModeEnabled) {
            return;
        }

        if (LibC.tcsetattr(ttyFd, LibC.TCSAFLUSH, savedTermios) != 0) {
            throw new RuntimeIOException("Failed to restore terminal attributes");
        }

        rawModeEnabled = false;
    }

    /**
     * Gets the current terminal size.
     *
     * @return the terminal size
     * @throws IOException if the size cannot be determined
     */
    public Size getSize() throws IOException {
        int ioctlResult = LibC.ioctl(ttyFd, LibC.TIOCGWINSZ, winsize);
        if (ioctlResult == 0) {
            var cols = Short.toUnsignedInt((short) WS_COL.get(winsize, 0L));
            var rows = Short.toUnsignedInt((short) WS_ROW.get(winsize, 0L));
            if (cols > 0 && rows > 0) {
                return new Size(cols, rows);
            }
        }
        throw new RuntimeIOException("Failed to get terminal size (errno=" + LibC.getLastErrno() + ")");
    }

    /**
     * Reads a single character from the terminal with timeout.
     * <p>
     * This method also checks for and dispatches pending resize events,
     * ensuring resize handlers are called from the main event loop context
     * rather than from signal handler context.
     *
     * @param timeoutMs timeout in milliseconds (-1 for infinite, 0 for non-blocking)
     * @return the character read, -1 for EOF, or -2 for timeout
     * @throws IOException if reading fails
     */
    public int read(int timeoutMs) throws IOException {
        // Check for pending resize events (set by signal handler)
        checkResizePending();

        // Return peeked character if available
        if (peekedChar != -2) {
            var c = peekedChar;
            peekedChar = -2;
            return c;
        }

        return readInternal(timeoutMs);
    }

    /**
     * Peeks at the next character without consuming it.
     *
     * @param timeoutMs timeout in milliseconds
     * @return the character peeked, -1 for EOF, or -2 for timeout
     * @throws IOException if reading fails
     */
    public int peek(int timeoutMs) throws IOException {
        if (peekedChar != -2) {
            return peekedChar;
        }

        peekedChar = readInternal(timeoutMs);
        return peekedChar;
    }

    /**
     * Writes data to the terminal.
     *
     * @param data the data to write
     * @throws IOException if writing fails
     */
    public void write(byte[] data) throws IOException {
        write(data, 0, data.length);
    }

    /**
     * Writes a portion of a byte array to the terminal.
     * <p>
     * This method uses a reusable buffer to avoid per-call memory allocation.
     * For large writes exceeding the buffer size, data is written in chunks.
     *
     * @param buffer the byte array containing data
     * @param offset the start offset in the buffer
     * @param length the number of bytes to write
     * @throws IOException if writing fails
     */
    public void write(byte[] buffer, int offset, int length) throws IOException {
        if (length == 0) {
            return;
        }

        int remaining = length;
        int currentOffset = offset;

        while (remaining > 0) {
            int chunkSize = Math.min(remaining, WRITE_BUFFER_SIZE);
            MemorySegment.copy(buffer, currentOffset, writeBuffer, ValueLayout.JAVA_BYTE, 0, chunkSize);

            long written = 0;
            while (written < chunkSize) {
                long result = LibC.write(ttyFd, writeBuffer.asSlice(written), chunkSize - written);
                if (result < 0) {
                    throw new RuntimeIOException("Write failed (errno=" + LibC.getLastErrno() + ")");
                }
                written += result;
            }

            currentOffset += chunkSize;
            remaining -= chunkSize;
        }
    }

    /**
     * Writes a string to the terminal.
     *
     * @param s the string to write
     * @throws IOException if writing fails
     */
    public void write(String s) throws IOException {
        write(s.getBytes(charset));
    }

    /**
     * Returns the charset used for terminal I/O.
     *
     * @return the terminal charset
     */
    public Charset getCharset() {
        return charset;
    }

    /**
     * Checks if raw mode is currently enabled.
     *
     * @return true if raw mode is enabled
     */
    public boolean isRawModeEnabled() {
        return rawModeEnabled;
    }

    /**
     * Registers a handler to be called when the terminal is resized.
     * <p>
     * On Unix systems, this installs a SIGWINCH signal handler using Panama FFI.
     * The signal handler sets a flag which is checked from the main event loop
     * (via {@link #read(int)}), ensuring the handler is called from a safe context.
     * <p>
     * Only one handler can be registered at a time; subsequent calls
     * will replace the previous handler.
     *
     * @param handler the handler to call on resize, or null to remove
     */
    public void onResize(Runnable handler) {
        resizeLock.lock();
        try {
            this.resizeHandler = handler;
            if (handler != null && signalArena == null) {
                // Create a dedicated arena for the signal handler that lives as long as needed
                signalArena = Arena.ofShared();

                // Create the upcall stub for our signal handler
                // IMPORTANT: We only set a flag here, NOT call the handler directly.
                // Calling complex code from signal context can cause crashes.
                var signalHandlerStub = LibC.createSignalHandler(signalArena, signum -> {
                    resizeLock.lock();
                    try {
                        resizePending = true;
                    } finally {
                        resizeLock.unlock();
                    }
                });

                // Use sigaction() instead of signal() for better reliability on macOS
                // Allocate sigaction structs
                MemorySegment newSigaction = LibC.allocateSigaction(signalArena);
                MemorySegment oldSigaction = LibC.allocateSigaction(signalArena);
                
                // Set up new sigaction: handler pointer, NULL trampoline, empty mask, SA_RESTART flag
                LibC.setSigactionHandler(newSigaction, signalHandlerStub);
                LibC.setSigactionTramp(newSigaction, MemorySegment.NULL);  // NULL for simple handlers
                LibC.setSigactionMask(newSigaction, 0);
                LibC.setSigactionFlags(newSigaction, LibC.SA_RESTART);
                
                // Install the signal handler and save the previous one
                int sigactionResult = LibC.sigaction(LibC.SIGWINCH, newSigaction, oldSigaction);
                
                if (sigactionResult != 0) {
                    throw new BackendException("Failed to install signal handler for Unix terminal (errno=" + LibC.getLastErrno() + ")");
                }
                
                // Save the old sigaction for restoration on close
                previousSigaction = oldSigaction;
            }
        } finally {
            resizeLock.unlock();
        }
    }

    /**
     * Checks if a resize event is pending and dispatches it.
     * <p>
     * This should be called from the main event loop, not from signal context.
     */
    private void checkResizePending() {
        Runnable handler = null;
        resizeLock.lock();
        try {
            if (resizePending) {
                resizePending = false;
                handler = resizeHandler;
            }
        } finally {
            resizeLock.unlock();
        }
        // Call handler outside of lock to avoid deadlock
        if (handler != null) {
            handler.run();
        }
    }

    @Override
    public void close() throws IOException {
        try {
            if (rawModeEnabled) {
                disableRawMode();
            }
        } finally {
            // Restore previous SIGWINCH handler using sigaction
            if (previousSigaction != null) {
                LibC.sigaction(LibC.SIGWINCH, previousSigaction, MemorySegment.NULL);
                previousSigaction = null;
            }
            resizeHandler = null;

            // Close the signal arena (this invalidates the upcall stub)
            if (signalArena != null) {
                signalArena.close();
                signalArena = null;
            }

            // Don't close stdin on macOS
            if (!PlatformConstants.isMacOS()) {
                LibC.close(ttyFd);
            }
            arena.close();
        }
    }

    private int readInternal(int timeoutMs) throws IOException {
        // Set up poll
        POLLFD_FD.set(pollfd, 0L, ttyFd);
        POLLFD_EVENTS.set(pollfd, 0L, LibC.POLLIN);
        POLLFD_REVENTS.set(pollfd, 0L, (short) 0);

        int result;
        // Retry poll if interrupted by signal (EINTR)
        while (true) {
            result = LibC.poll(pollfd, 1, timeoutMs);
            if (result >= 0) {
                break;
            }
            // Check if interrupted by signal - if so, check for pending resize and retry
            if (LibC.getLastErrno() == LibC.EINTR) {
                checkResizePending();
                continue;
            }
            throw new RuntimeIOException("poll() failed (errno=" + LibC.getLastErrno() + ")");
        }

        if (result == 0) {
            return -2; // Timeout
        }

        var revents = (short) POLLFD_REVENTS.get(pollfd, 0L);

        if ((revents & LibC.POLLHUP) != 0 || (revents & LibC.POLLERR) != 0) {
            return -1; // EOF or error
        }

        if ((revents & LibC.POLLIN) != 0) {
            long bytesRead = LibC.read(ttyFd, readBuffer, 1);
            if (bytesRead <= 0) {
                return -1; // EOF
            }
            return Byte.toUnsignedInt(readBuffer.get(ValueLayout.JAVA_BYTE, 0));
        }

        return -2; // No data available
    }

    private void clearControlChar(MemorySegment termios, int index) {
        termios.set(ValueLayout.JAVA_BYTE, TERMIOS_CC_OFFSET + index, (byte) 0);
    }

    private long getTermiosFlag(VarHandle handle) {
        if (PlatformConstants.isMacOS()) {
            return (long) handle.get(currentTermios, 0L);
        } else {
            return Integer.toUnsignedLong((int) handle.get(currentTermios, 0L));
        }
    }

    private void setTermiosFlag(VarHandle handle, long value) {
        if (PlatformConstants.isMacOS()) {
            handle.set(currentTermios, 0L, value);
        } else {
            handle.set(currentTermios, 0L, (int) value);
        }
    }
}
