/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.backend.panama.unix;

import java.lang.foreign.AddressLayout;
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

    /** Standard input file descriptor. */
    public static final int STDIN_FILENO = 0;
    /** Standard output file descriptor. */
    public static final int STDOUT_FILENO = 1;
    /** Standard error file descriptor. */
    public static final int STDERR_FILENO = 2;

    /** Open flag for read/write access. */
    public static final int O_RDWR = 2;
    /** Open flag to not make the opened file the controlling terminal. */
    public static final int O_NOCTTY = 0x100;

    /** Apply termios changes immediately. */
    public static final int TCSANOW = 0;
    /** Apply termios changes after all output has been transmitted. */
    public static final int TCSADRAIN = 1;
    /** Apply termios changes after all output has been transmitted, discarding pending input. */
    public static final int TCSAFLUSH = 2;

    /** ioctl request code to get terminal window size. */
    public static final long TIOCGWINSZ = PlatformConstants.TIOCGWINSZ;

    /** Local flag: enable echo of input characters. */
    public static final int ECHO = PlatformConstants.ECHO;
    /** Local flag: enable canonical (line-by-line) input mode. */
    public static final int ICANON = PlatformConstants.ICANON;
    /** Local flag: enable signal generation for INTR, QUIT, SUSP characters. */
    public static final int ISIG = PlatformConstants.ISIG;
    /** Local flag: enable implementation-defined input processing. */
    public static final int IEXTEN = PlatformConstants.IEXTEN;

    /** Input flag: enable XON/XOFF flow control on output. */
    public static final int IXON = PlatformConstants.IXON;
    /** Input flag: translate carriage return to newline on input. */
    public static final int ICRNL = PlatformConstants.ICRNL;
    /** Input flag: signal interrupt on break. */
    public static final int BRKINT = PlatformConstants.BRKINT;
    /** Input flag: enable input parity check. */
    public static final int INPCK = PlatformConstants.INPCK;
    /** Input flag: strip eighth bit off input characters. */
    public static final int ISTRIP = PlatformConstants.ISTRIP;

    /** Output flag: enable implementation-defined output processing. */
    public static final int OPOST = PlatformConstants.OPOST;

    /** Control flag: set character size to 8 bits. */
    public static final int CS8 = PlatformConstants.CS8;

    /** Control character index for minimum number of bytes for non-canonical read. */
    public static final int VMIN = PlatformConstants.VMIN;
    /** Control character index for timeout in deciseconds for non-canonical read. */
    public static final int VTIME = PlatformConstants.VTIME;

    /** Poll event: there is data to read. */
    public static final short POLLIN = 0x0001;
    /** Poll event: there is urgent data to read. */
    public static final short POLLPRI = 0x0002;
    /** Poll event: error condition on the file descriptor. */
    public static final short POLLERR = 0x0008;
    /** Poll event: hang up on the file descriptor. */
    public static final short POLLHUP = 0x0010;
    /** Poll event: invalid file descriptor. */
    public static final short POLLNVAL = 0x0020;

    /** Signal number for terminal window size change. */
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

    // Canonical layouts (matching jextract pattern)
    private static final ValueLayout.OfInt C_INT = 
            (ValueLayout.OfInt) Linker.nativeLinker().canonicalLayouts().get("int");
    private static final ValueLayout.OfLong C_LONG = 
            (ValueLayout.OfLong) Linker.nativeLinker().canonicalLayouts().get("long");
    private static final AddressLayout C_POINTER = 
            ((AddressLayout) Linker.nativeLinker().canonicalLayouts().get("void*"))
                    .withTargetLayout(MemoryLayout.sequenceLayout(java.lang.Long.MAX_VALUE, 
                            (ValueLayout.OfByte) Linker.nativeLinker().canonicalLayouts().get("char")));
    
    // Function descriptor for signal handlers: void handler(int signum)
    // Use canonical C_INT layout (same as jextract) for proper platform-specific handling
    private static final FunctionDescriptor SIGNAL_HANDLER_DESC =
            FunctionDescriptor.ofVoid(C_INT);

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
    private static final MethodHandle SIGACTION;

    /** Errno value indicating an interrupted system call. */
    public static final int EINTR = 4;

    /** Sigaction flag: restart interrupted system calls. */
    public static final int SA_RESTART = 2;
    /** Sigaction flag: reset signal handler to default after first signal. */
    public static final int SA_RESETHAND = 4;

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

            // ioctl(int fd, unsigned long request, ...) - variadic function
            // Use canonical layouts matching jextract: C_INT return, C_INT fd, C_LONG request
            // For TIOCGWINSZ, the variadic arg is a pointer to winsize struct
            // Use firstVariadicArg option to mark where variadic args start (after request)
            FunctionDescriptor ioctlDesc = FunctionDescriptor.of(C_INT, C_INT, C_LONG, C_POINTER);
            Linker.Option firstVariadicArg = Linker.Option.firstVariadicArg(2); // variadic args start at index 2 (after fd and request)
            IOCTL = LINKER.downcallHandle(
                    LIBC.find("ioctl").orElseThrow(),
                    ioctlDesc,
                    firstVariadicArg,
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
            // Use canonical layouts matching jextract: C_POINTER return, C_INT signum, C_POINTER handler
            SIGNAL = LINKER.downcallHandle(
                    LIBC.find("signal").orElseThrow(),
                    FunctionDescriptor.of(C_POINTER, C_INT, C_POINTER)
            );
            
            // sigaction(int signum, const struct sigaction *act, struct sigaction *oldact)
            // Returns 0 on success, -1 on error
            SIGACTION = LINKER.downcallHandle(
                    LIBC.find("sigaction").orElseThrow(),
                    FunctionDescriptor.of(C_INT, C_INT, C_POINTER, C_POINTER)
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
     * Installs a signal handler using signal().
     * <p>
     * Note: This is deprecated on macOS. Use {@link #sigaction(int, MemorySegment, MemorySegment)} instead.
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
     * Installs a signal handler using sigaction().
     * <p>
     * This is the preferred method on macOS as signal() is deprecated.
     *
     * @param signum the signal number
     * @param act    the new sigaction structure (can be null)
     * @param oldact the old sigaction structure to save previous handler (can be null)
     * @return 0 on success, -1 on error
     */
    public static int sigaction(int signum, MemorySegment act, MemorySegment oldact) {
        try {
            return (int) SIGACTION.invokeExact(signum, act, oldact);
        } catch (Throwable t) {
            throw new RuntimeException("sigaction failed", t);
        }
    }
    
    /**
     * Allocates a sigaction structure in the given arena.
     *
     * @param arena the arena to allocate in
     * @return a memory segment for the sigaction struct
     */
    public static MemorySegment allocateSigaction(Arena arena) {
        return arena.allocate(SIGACTION_LAYOUT);
    }
    
    /**
     * Sets the handler pointer in a sigaction structure.
     *
     * @param sigactionStruct the sigaction structure
     * @param handler         the handler function pointer (upcall stub)
     */
    public static void setSigactionHandler(MemorySegment sigactionStruct, MemorySegment handler) {
        SIGACTION_HANDLER.set(sigactionStruct, 0L, handler);
    }

    /**
     * Sets the flags in a sigaction structure.
     * <p>
     * Note: On Linux, sa_flags is a long (8 bytes), on macOS it's an int (4 bytes).
     *
     * @param sigactionStruct the sigaction structure
     * @param flags           the flags (e.g., SA_RESTART)
     */
    public static void setSigactionFlags(MemorySegment sigactionStruct, int flags) {
        if (PlatformConstants.isMacOS()) {
            SIGACTION_FLAGS.set(sigactionStruct, 0L, flags);
        } else {
            // On Linux, sa_flags is a long
            SIGACTION_FLAGS.set(sigactionStruct, 0L, (long) flags);
        }
    }

    /**
     * Sets the trampoline pointer in a sigaction structure.
     * For simple signal handlers, this should be set to NULL.
     * <p>
     * Note: This is macOS-specific. On Linux, this method does nothing.
     *
     * @param sigactionStruct the sigaction structure
     * @param tramp           the trampoline function pointer (or NULL)
     */
    public static void setSigactionTramp(MemorySegment sigactionStruct, MemorySegment tramp) {
        if (SIGACTION_TRAMP != null) {
            SIGACTION_TRAMP.set(sigactionStruct, 0L, tramp);
        }
        // On Linux, there's no trampoline field - do nothing
    }

    /**
     * Sets the signal mask in a sigaction structure.
     * <p>
     * Note: On macOS, sa_mask is a simple int. On Linux, it's a 128-byte sigset_t.
     * This method only sets the mask on macOS. On Linux, the mask is zeroed by default.
     *
     * @param sigactionStruct the sigaction structure
     * @param mask            the signal mask (only used on macOS)
     */
    public static void setSigactionMask(MemorySegment sigactionStruct, int mask) {
        if (SIGACTION_MASK_MACOS != null) {
            SIGACTION_MASK_MACOS.set(sigactionStruct, 0L, mask);
        }
        // On Linux, sa_mask is a 128-byte sigset_t - leave it zeroed (empty mask)
    }

    /**
     * Gets the handler pointer from a sigaction structure.
     *
     * @param sigactionStruct the sigaction structure
     * @return the handler function pointer
     */
    public static MemorySegment getSigactionHandler(MemorySegment sigactionStruct) {
        return (MemorySegment) SIGACTION_HANDLER.get(sigactionStruct, 0L);
    }

    /**
     * Creates an upcall stub for a signal handler.
     * <p>
     * The returned memory segment is valid for the lifetime of the provided arena.
     * The handler will be called with the signal number as parameter.
     * <p>
     * This follows the jextract pattern: create unbound MethodHandle first,
     * then bind when creating the upcall stub.
     *
     * @param arena   the arena to allocate the stub in
     * @param handler the Java handler to call (receives signal number)
     * @return a memory segment that can be passed to signal()
     */
    public static MemorySegment createSignalHandler(Arena arena, java.util.function.IntConsumer handler) {
        try {
            // Create unbound MethodHandle first (like jextract does)
            MethodHandle unboundHandle = java.lang.invoke.MethodHandles.lookup()
                    .findVirtual(java.util.function.IntConsumer.class, "accept",
                            SIGNAL_HANDLER_DESC.toMethodType());
            // Bind handler when creating upcall stub (matches jextract pattern)
            return LINKER.upcallStub(unboundHandle.bindTo(handler), SIGNAL_HANDLER_DESC, arena);
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
     * Layout for the __sigaction_u union (macOS).
     * This union contains either __sa_handler or __sa_sigaction pointer.
     * Both are at offset 0 since it's a union.
     */
    private static final MemoryLayout SIGACTION_U_LAYOUT_MACOS = MemoryLayout.unionLayout(
            C_POINTER.withName("__sa_handler"),
            C_POINTER.withName("__sa_sigaction")
    );

    /**
     * Layout for the sigaction structure.
     * <p>
     * macOS struct __sigaction (24 bytes):
     * <pre>
     *     union __sigaction_u __sigaction_u;  // 8 bytes - handler pointer
     *     void (*sa_tramp)(...);              // 8 bytes - trampoline pointer
     *     int sa_mask;                        // 4 bytes - signal mask
     *     int sa_flags;                       // 4 bytes - flags
     * </pre>
     * <p>
     * Linux struct sigaction (152 bytes):
     * <pre>
     *     void (*sa_handler)(int);            // 8 bytes - handler pointer
     *     unsigned long sa_flags;             // 8 bytes - flags
     *     void (*sa_restorer)(void);          // 8 bytes - restorer (unused)
     *     sigset_t sa_mask;                   // 128 bytes - signal mask (1024 bits)
     * </pre>
     */
    public static final MemoryLayout SIGACTION_LAYOUT = PlatformConstants.isMacOS()
            ? MemoryLayout.structLayout(
                    SIGACTION_U_LAYOUT_MACOS.withName("__sigaction_u"),
                    C_POINTER.withName("sa_tramp"),
                    C_INT.withName("sa_mask"),
                    C_INT.withName("sa_flags")
            )
            : MemoryLayout.structLayout(
                    C_POINTER.withName("sa_handler"),
                    ValueLayout.JAVA_LONG.withName("sa_flags"),
                    C_POINTER.withName("sa_restorer"),
                    MemoryLayout.sequenceLayout(128, ValueLayout.JAVA_BYTE).withName("sa_mask")
            );

    // VarHandles for sigaction - platform specific
    private static final VarHandle SIGACTION_HANDLER = PlatformConstants.isMacOS()
            ? SIGACTION_LAYOUT.varHandle(
                    MemoryLayout.PathElement.groupElement("__sigaction_u"),
                    MemoryLayout.PathElement.groupElement("__sa_handler"))
            : SIGACTION_LAYOUT.varHandle(
                    MemoryLayout.PathElement.groupElement("sa_handler"));

    private static final VarHandle SIGACTION_FLAGS = PlatformConstants.isMacOS()
            ? SIGACTION_LAYOUT.varHandle(
                    MemoryLayout.PathElement.groupElement("sa_flags"))
            : SIGACTION_LAYOUT.varHandle(
                    MemoryLayout.PathElement.groupElement("sa_flags"));

    // macOS-only VarHandles
    private static final VarHandle SIGACTION_TRAMP = PlatformConstants.isMacOS()
            ? SIGACTION_LAYOUT.varHandle(
                    MemoryLayout.PathElement.groupElement("sa_tramp"))
            : null;

    private static final VarHandle SIGACTION_MASK_MACOS = PlatformConstants.isMacOS()
            ? SIGACTION_LAYOUT.varHandle(
                    MemoryLayout.PathElement.groupElement("sa_mask"))
            : null;

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
