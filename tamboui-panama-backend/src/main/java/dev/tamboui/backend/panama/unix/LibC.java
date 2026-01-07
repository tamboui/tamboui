/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.backend.panama.unix;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;

/**
 * Panama FFI bindings to libc functions for terminal operations.
 * <p>
 * This class provides low-level access to Unix terminal control functions
 * including termios manipulation, terminal size queries, and non-blocking I/O.
 */
public final class LibC {

    // File descriptors
    public static final int STDIN_FILENO = 0;
    public static final int STDOUT_FILENO = 1;
    public static final int STDERR_FILENO = 2;

    // open() flags
    public static final int O_RDWR = 2;
    public static final int O_NOCTTY = 0x100;

    // termios constants
    public static final int TCSANOW = 0;
    public static final int TCSADRAIN = 1;
    public static final int TCSAFLUSH = 2;

    // ioctl requests
    public static final long TIOCGWINSZ = PlatformConstants.TIOCGWINSZ;

    // Local flags (c_lflag)
    public static final int ECHO = PlatformConstants.ECHO;
    public static final int ICANON = PlatformConstants.ICANON;
    public static final int ISIG = PlatformConstants.ISIG;
    public static final int IEXTEN = PlatformConstants.IEXTEN;

    // Input flags (c_iflag)
    public static final int IXON = PlatformConstants.IXON;
    public static final int ICRNL = PlatformConstants.ICRNL;
    public static final int BRKINT = PlatformConstants.BRKINT;
    public static final int INPCK = PlatformConstants.INPCK;
    public static final int ISTRIP = PlatformConstants.ISTRIP;

    // Output flags (c_oflag)
    public static final int OPOST = PlatformConstants.OPOST;

    // Control flags (c_cflag)
    public static final int CS8 = PlatformConstants.CS8;

    // Control characters indices
    public static final int VMIN = PlatformConstants.VMIN;
    public static final int VTIME = PlatformConstants.VTIME;

    // poll() events
    public static final short POLLIN = 0x0001;
    public static final short POLLPRI = 0x0002;
    public static final short POLLERR = 0x0008;
    public static final short POLLHUP = 0x0010;
    public static final short POLLNVAL = 0x0020;

    // Signal numbers
    public static final int SIGWINCH = PlatformConstants.SIGWINCH;

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LIBC = SymbolLookup.loaderLookup()
            .or(Linker.nativeLinker().defaultLookup());

    // Capture errno state location for error reporting
    private static final Linker.Option CAPTURE_ERRNO = Linker.Option.captureCallState("errno");
    private static final MemoryLayout CALL_STATE_LAYOUT = Linker.Option.captureStateLayout();
    private static final VarHandle ERRNO_HANDLE = CALL_STATE_LAYOUT.varHandle(
            MemoryLayout.PathElement.groupElement("errno"));

    // Thread-local call state segment to avoid per-call Arena allocation
    private static final ThreadLocal<MemorySegment> CALL_STATE_SEGMENT =
            ThreadLocal.withInitial(() -> Arena.global().allocate(CALL_STATE_LAYOUT));

    // Function descriptor for signal handlers: void handler(int signum)
    private static final FunctionDescriptor SIGNAL_HANDLER_DESC =
            FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT);

    // Method handles for libc functions
    private static final MethodHandle TCGETATTR;
    private static final MethodHandle TCSETATTR;
    private static final MethodHandle IOCTL;
    private static final MethodHandle READ;
    private static final MethodHandle WRITE;
    private static final MethodHandle POLL_MACOS;  // nfds_t is unsigned int on macOS
    private static final MethodHandle POLL_LINUX;  // nfds_t is unsigned long on Linux
    private static final MethodHandle ISATTY;
    private static final MethodHandle OPEN;
    private static final MethodHandle CLOSE;
    private static final MethodHandle SIGNAL;

    // EINTR constant for handling interrupted system calls
    public static final int EINTR = 4;

    static {
        try {
            TCGETATTR = LINKER.downcallHandle(
                    LIBC.find("tcgetattr").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS)
            );

            TCSETATTR = LINKER.downcallHandle(
                    LIBC.find("tcsetattr").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS)
            );

            IOCTL = LINKER.downcallHandle(
                    LIBC.find("ioctl").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                    CAPTURE_ERRNO
            );

            READ = LINKER.downcallHandle(
                    LIBC.find("read").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG)
            );

            WRITE = LINKER.downcallHandle(
                    LIBC.find("write").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG),
                    CAPTURE_ERRNO
            );

            // nfds_t is unsigned int (4 bytes) on macOS, unsigned long (8 bytes) on Linux
            // We need separate handles because invokeExact requires exact type matching
            POLL_MACOS = LINKER.downcallHandle(
                    LIBC.find("poll").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT),
                    CAPTURE_ERRNO
            );
            POLL_LINUX = LINKER.downcallHandle(
                    LIBC.find("poll").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT),
                    CAPTURE_ERRNO
            );

            ISATTY = LINKER.downcallHandle(
                    LIBC.find("isatty").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT)
            );

            OPEN = LINKER.downcallHandle(
                    LIBC.find("open").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT),
                    CAPTURE_ERRNO
            );

            CLOSE = LINKER.downcallHandle(
                    LIBC.find("close").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT)
            );

            // signal(int signum, void (*handler)(int)) returns previous handler
            SIGNAL = LINKER.downcallHandle(
                    LIBC.find("signal").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.ADDRESS)
            );
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private LibC() {
    }

    /**
     * Gets terminal attributes.
     *
     * @param fd      file descriptor
     * @param termios memory segment for termios struct
     * @return 0 on success, -1 on error
     */
    public static int tcgetattr(int fd, MemorySegment termios) {
        try {
            return (int) TCGETATTR.invokeExact(fd, termios);
        } catch (Throwable t) {
            throw new RuntimeException("tcgetattr failed", t);
        }
    }

    /**
     * Sets terminal attributes.
     *
     * @param fd              file descriptor
     * @param optionalActions when to apply changes (TCSANOW, TCSADRAIN, TCSAFLUSH)
     * @param termios         memory segment for termios struct
     * @return 0 on success, -1 on error
     */
    public static int tcsetattr(int fd, int optionalActions, MemorySegment termios) {
        try {
            return (int) TCSETATTR.invokeExact(fd, optionalActions, termios);
        } catch (Throwable t) {
            throw new RuntimeException("tcsetattr failed", t);
        }
    }

    /**
     * Performs an I/O control operation.
     * <p>
     * Uses a thread-local call state segment to avoid per-call Arena allocation.
     *
     * @param fd      file descriptor
     * @param request ioctl request code
     * @param arg     argument (typically a memory segment)
     * @return 0 on success, -1 on error
     */
    public static int ioctl(int fd, long request, MemorySegment arg) {
        try {
            MemorySegment callState = CALL_STATE_SEGMENT.get();
            int result = (int) IOCTL.invokeExact(callState, fd, request, arg);
            if (result < 0) {
                lastErrno = (int) ERRNO_HANDLE.get(callState, 0L);
            }
            return result;
        } catch (Throwable t) {
            throw new RuntimeException("ioctl failed", t);
        }
    }

    private static volatile int lastErrno = 0;

    /**
     * Returns the errno from the last failed ioctl call.
     *
     * @return the errno value
     */
    public static int getLastErrno() {
        return lastErrno;
    }

    /**
     * Reads from a file descriptor.
     *
     * @param fd    file descriptor
     * @param buf   buffer to read into
     * @param count maximum bytes to read
     * @return number of bytes read, 0 for EOF, -1 on error
     */
    public static long read(int fd, MemorySegment buf, long count) {
        try {
            return (long) READ.invokeExact(fd, buf, count);
        } catch (Throwable t) {
            throw new RuntimeException("read failed", t);
        }
    }

    /**
     * Writes to a file descriptor.
     * <p>
     * Uses a thread-local call state segment to avoid per-call Arena allocation.
     *
     * @param fd    file descriptor
     * @param buf   buffer to write from
     * @param count number of bytes to write
     * @return number of bytes written, -1 on error
     */
    public static long write(int fd, MemorySegment buf, long count) {
        try {
            MemorySegment callState = CALL_STATE_SEGMENT.get();
            long result = (long) WRITE.invokeExact(callState, fd, buf, count);
            if (result < 0) {
                lastErrno = (int) ERRNO_HANDLE.get(callState, 0L);
            }
            return result;
        } catch (Throwable t) {
            throw new RuntimeException("write failed", t);
        }
    }

    /**
     * Waits for events on file descriptors.
     * <p>
     * Uses a thread-local call state segment to capture errno.
     * Check {@link #getLastErrno()} after a -1 return to determine the cause.
     *
     * @param fds     array of pollfd structures
     * @param nfds    number of file descriptors
     * @param timeout timeout in milliseconds (-1 for infinite)
     * @return number of descriptors with events, 0 for timeout, -1 on error
     */
    public static int poll(MemorySegment fds, int nfds, int timeout) {
        try {
            MemorySegment callState = CALL_STATE_SEGMENT.get();
            int result;
            if (PlatformConstants.isMacOS()) {
                result = (int) POLL_MACOS.invokeExact(callState, fds, nfds, timeout);
            } else {
                result = (int) POLL_LINUX.invokeExact(callState, fds, (long) nfds, timeout);
            }
            if (result < 0) {
                lastErrno = (int) ERRNO_HANDLE.get(callState, 0L);
            }
            return result;
        } catch (Throwable t) {
            throw new RuntimeException("poll failed", t);
        }
    }

    /**
     * Checks if a file descriptor refers to a terminal.
     *
     * @param fd file descriptor
     * @return 1 if terminal, 0 otherwise
     */
    public static int isatty(int fd) {
        try {
            return (int) ISATTY.invokeExact(fd);
        } catch (Throwable t) {
            throw new RuntimeException("isatty failed", t);
        }
    }

    /**
     * Opens a file.
     *
     * @param pathname path to the file
     * @param flags    open flags (O_RDWR, etc.)
     * @return file descriptor on success, -1 on error
     */
    public static int open(String pathname, int flags) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment pathSegment = arena.allocateFrom(pathname);
            MemorySegment callState = arena.allocate(CALL_STATE_LAYOUT);
            int result = (int) OPEN.invokeExact(callState, pathSegment, flags);
            if (result < 0) {
                lastErrno = (int) ERRNO_HANDLE.get(callState, 0L);
            }
            return result;
        } catch (Throwable t) {
            throw new RuntimeException("open failed", t);
        }
    }

    /**
     * Closes a file descriptor.
     *
     * @param fd file descriptor to close
     * @return 0 on success, -1 on error
     */
    public static int close(int fd) {
        try {
            return (int) CLOSE.invokeExact(fd);
        } catch (Throwable t) {
            throw new RuntimeException("close failed", t);
        }
    }

    /**
     * Installs a signal handler.
     *
     * @param signum  the signal number
     * @param handler the handler function pointer (upcall stub)
     * @return the previous handler
     */
    public static MemorySegment signal(int signum, MemorySegment handler) {
        try {
            return (MemorySegment) SIGNAL.invokeExact(signum, handler);
        } catch (Throwable t) {
            throw new RuntimeException("signal failed", t);
        }
    }

    /**
     * Creates an upcall stub for a signal handler.
     * <p>
     * The returned memory segment is valid for the lifetime of the provided arena.
     * The handler will be called with the signal number as parameter.
     *
     * @param arena   the arena to allocate the stub in
     * @param handler the Java handler to call (receives signal number)
     * @return a memory segment that can be passed to signal()
     */
    public static MemorySegment createSignalHandler(Arena arena, java.util.function.IntConsumer handler) {
        try {
            MethodHandle target = java.lang.invoke.MethodHandles.lookup()
                    .findVirtual(java.util.function.IntConsumer.class, "accept",
                            java.lang.invoke.MethodType.methodType(void.class, int.class))
                    .bindTo(handler);
            return LINKER.upcallStub(target, SIGNAL_HANDLER_DESC, arena);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException("Failed to create signal handler", e);
        }
    }

    /**
     * Layout for the termios structure.
     * <p>
     * Platform-specific: Linux uses 4-byte ints for flags, macOS uses 8-byte longs.
     */
    public static final MemoryLayout TERMIOS_LAYOUT = PlatformConstants.TERMIOS_LAYOUT;

    /**
     * Layout for the winsize structure.
     */
    public static final MemoryLayout WINSIZE_LAYOUT = MemoryLayout.structLayout(
            ValueLayout.JAVA_SHORT.withName("ws_row"),
            ValueLayout.JAVA_SHORT.withName("ws_col"),
            ValueLayout.JAVA_SHORT.withName("ws_xpixel"),
            ValueLayout.JAVA_SHORT.withName("ws_ypixel")
    );

    /**
     * Layout for the pollfd structure.
     */
    public static final MemoryLayout POLLFD_LAYOUT = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT.withName("fd"),
            ValueLayout.JAVA_SHORT.withName("events"),
            ValueLayout.JAVA_SHORT.withName("revents")
    );

    /**
     * Creates a new termios struct in the given arena.
     *
     * @param arena the arena to allocate in
     * @return a memory segment for the termios struct
     */
    public static MemorySegment allocateTermios(Arena arena) {
        return arena.allocate(TERMIOS_LAYOUT);
    }

    /**
     * Creates a new winsize struct in the given arena.
     *
     * @param arena the arena to allocate in
     * @return a memory segment for the winsize struct
     */
    public static MemorySegment allocateWinsize(Arena arena) {
        return arena.allocate(WINSIZE_LAYOUT);
    }

    /**
     * Creates a new pollfd struct in the given arena.
     *
     * @param arena the arena to allocate in
     * @return a memory segment for the pollfd struct
     */
    public static MemorySegment allocatePollfd(Arena arena) {
        return arena.allocate(POLLFD_LAYOUT);
    }
}
