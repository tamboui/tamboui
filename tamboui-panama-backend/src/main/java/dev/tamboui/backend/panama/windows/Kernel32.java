/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.backend.panama.windows;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;

import dev.tamboui.error.RuntimeIOException;
import dev.tamboui.terminal.BackendException;

/**
 * Panama FFI bindings to Windows Kernel32 functions for console operations.
 * <p>
 * This class provides low-level access to Windows console control functions
 * including console mode manipulation, screen buffer operations, and input handling.
 */
public final class Kernel32 {

    /** Handle identifier for standard input. */
    public static final int STD_INPUT_HANDLE = -10;
    /** Handle identifier for standard output. */
    public static final int STD_OUTPUT_HANDLE = -11;

    /** Console input mode flag: enable processing of control keys (Ctrl+C, etc.). */
    public static final int ENABLE_PROCESSED_INPUT = 0x0001;
    /** Console input mode flag: enable line-at-a-time input. */
    public static final int ENABLE_LINE_INPUT = 0x0002;
    /** Console input mode flag: echo typed characters to the console. */
    public static final int ENABLE_ECHO_INPUT = 0x0004;
    /** Console input mode flag: report window buffer size changes as input events. */
    public static final int ENABLE_WINDOW_INPUT = 0x0008;
    /** Console input mode flag: enable virtual terminal input sequences. */
    public static final int ENABLE_VIRTUAL_TERMINAL_INPUT = 0x0200;

    /** Console output mode flag: enable processing of output control sequences. */
    public static final int ENABLE_PROCESSED_OUTPUT = 0x0001;
    /** Console output mode flag: enable virtual terminal processing of ANSI escape sequences. */
    public static final int ENABLE_VIRTUAL_TERMINAL_PROCESSING = 0x0004;

    /** Input record event type: keyboard event. */
    public static final short KEY_EVENT = 0x0001;
    /** Input record event type: console window buffer size change. */
    public static final short WINDOW_BUFFER_SIZE_EVENT = 0x0004;

    /** Sentinel value returned by handle functions on failure. */
    public static final long INVALID_HANDLE_VALUE = -1L;

    /** WaitForSingleObject return value: the object is signaled. */
    public static final int WAIT_OBJECT_0 = 0x00000000;
    /** WaitForSingleObject return value: the timeout interval elapsed. */
    public static final int WAIT_TIMEOUT = 0x00000102;
    /** WaitForSingleObject return value: the function has failed. */
    public static final int WAIT_FAILED = 0xFFFFFFFF;

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup KERNEL32;

    // Method handles for Kernel32 functions
    private static final MethodHandle GET_STD_HANDLE;
    private static final MethodHandle GET_CONSOLE_MODE;
    private static final MethodHandle SET_CONSOLE_MODE;
    private static final MethodHandle GET_CONSOLE_SCREEN_BUFFER_INFO;
    private static final MethodHandle WRITE_CONSOLE;
    private static final MethodHandle READ_CONSOLE_INPUT;
    private static final MethodHandle GET_NUMBER_OF_CONSOLE_INPUT_EVENTS;
    private static final MethodHandle GET_LAST_ERROR;
    private static final MethodHandle WAIT_FOR_SINGLE_OBJECT;

    static {
        KERNEL32 = SymbolLookup.libraryLookup("kernel32", Arena.global());

        try {
            GET_STD_HANDLE = LINKER.downcallHandle(
                    KERNEL32.find("GetStdHandle").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.JAVA_INT)
            );

            GET_CONSOLE_MODE = LINKER.downcallHandle(
                    KERNEL32.find("GetConsoleMode").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            );

            SET_CONSOLE_MODE = LINKER.downcallHandle(
                    KERNEL32.find("SetConsoleMode").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT)
            );

            GET_CONSOLE_SCREEN_BUFFER_INFO = LINKER.downcallHandle(
                    KERNEL32.find("GetConsoleScreenBufferInfo").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            );

            WRITE_CONSOLE = LINKER.downcallHandle(
                    KERNEL32.find("WriteConsoleW").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            );

            READ_CONSOLE_INPUT = LINKER.downcallHandle(
                    KERNEL32.find("ReadConsoleInputW").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT, ValueLayout.ADDRESS)
            );

            GET_NUMBER_OF_CONSOLE_INPUT_EVENTS = LINKER.downcallHandle(
                    KERNEL32.find("GetNumberOfConsoleInputEvents").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            );

            GET_LAST_ERROR = LINKER.downcallHandle(
                    KERNEL32.find("GetLastError").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT)
            );

            WAIT_FOR_SINGLE_OBJECT = LINKER.downcallHandle(
                    KERNEL32.find("WaitForSingleObject").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT)
            );
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private Kernel32() {
    }

    /**
     * Gets a handle to the specified standard device.
     *
     * @param stdHandle STD_INPUT_HANDLE or STD_OUTPUT_HANDLE
     * @return the handle, or INVALID_HANDLE_VALUE on error
     */
    public static MemorySegment getStdHandle(int stdHandle) {
        try {
            return (MemorySegment) GET_STD_HANDLE.invokeExact(stdHandle);
        } catch (Throwable t) {
            throw new RuntimeIOException("GetStdHandle failed", t);
        }
    }

    /**
     * Gets the current input mode of a console's input buffer or output mode of a screen buffer.
     *
     * @param handle  console handle
     * @param modePtr pointer to receive the mode
     * @return non-zero on success
     */
    public static int getConsoleMode(MemorySegment handle, MemorySegment modePtr) {
        try {
            return (int) GET_CONSOLE_MODE.invokeExact(handle, modePtr);
        } catch (Throwable t) {
            throw new BackendException("GetConsoleMode failed", t);
        }
    }

    /**
     * Sets the input mode of a console's input buffer or output mode of a screen buffer.
     *
     * @param handle console handle
     * @param mode   the mode to set
     * @return non-zero on success
     */
    public static int setConsoleMode(MemorySegment handle, int mode) {
        try {
            return (int) SET_CONSOLE_MODE.invokeExact(handle, mode);
        } catch (Throwable t) {
            throw new BackendException("SetConsoleMode failed", t);
        }
    }

    /**
     * Gets information about the specified console screen buffer.
     *
     * @param handle  console output handle
     * @param infoPtr pointer to CONSOLE_SCREEN_BUFFER_INFO structure
     * @return non-zero on success
     */
    public static int getConsoleScreenBufferInfo(MemorySegment handle, MemorySegment infoPtr) {
        try {
            return (int) GET_CONSOLE_SCREEN_BUFFER_INFO.invokeExact(handle, infoPtr);
        } catch (Throwable t) {
            throw new BackendException("GetConsoleScreenBufferInfo failed", t);
        }
    }

    /**
     * Writes a character string to a console screen buffer.
     *
     * @param handle     console output handle
     * @param buffer     buffer containing characters to write (UTF-16)
     * @param numChars   number of characters to write
     * @param numWritten pointer to receive count of characters written
     * @param reserved   reserved, must be null
     * @return non-zero on success
     */
    public static int writeConsole(MemorySegment handle, MemorySegment buffer,
                                   int numChars, MemorySegment numWritten, MemorySegment reserved) {
        try {
            return (int) WRITE_CONSOLE.invokeExact(handle, buffer, numChars, numWritten, reserved);
        } catch (Throwable t) {
            throw new BackendException("WriteConsoleW failed", t);
        }
    }

    /**
     * Reads data from a console input buffer.
     *
     * @param handle  console input handle
     * @param buffer  pointer to INPUT_RECORD array
     * @param length  size of the array
     * @param numRead pointer to receive count of records read
     * @return non-zero on success
     */
    public static int readConsoleInput(MemorySegment handle, MemorySegment buffer,
                                       int length, MemorySegment numRead) {
        try {
            return (int) READ_CONSOLE_INPUT.invokeExact(handle, buffer, length, numRead);
        } catch (Throwable t) {
            throw new BackendException("ReadConsoleInputW failed", t);
        }
    }

    /**
     * Gets the number of unread input records in the console's input buffer.
     *
     * @param handle    console input handle
     * @param numEvents pointer to receive the count
     * @return non-zero on success
     */
    public static int getNumberOfConsoleInputEvents(MemorySegment handle, MemorySegment numEvents) {
        try {
            return (int) GET_NUMBER_OF_CONSOLE_INPUT_EVENTS.invokeExact(handle, numEvents);
        } catch (Throwable t) {
            throw new BackendException("GetNumberOfConsoleInputEvents failed", t);
        }
    }

    /**
     * Gets the last error code.
     *
     * @return the last error code
     */
    public static int getLastError() {
        try {
            return (int) GET_LAST_ERROR.invokeExact();
        } catch (Throwable t) {
            throw new BackendException("GetLastError failed", t);
        }
    }

    /**
     * Waits until the specified object is in the signaled state or the timeout interval elapses.
     *
     * @param handle    handle to the object
     * @param timeoutMs timeout interval in milliseconds, or -1 (INFINITE) for no timeout
     * @return WAIT_OBJECT_0 if the object is signaled, WAIT_TIMEOUT if timeout elapsed,
     *         or WAIT_FAILED on error
     */
    public static int waitForSingleObject(MemorySegment handle, int timeoutMs) {
        try {
            return (int) WAIT_FOR_SINGLE_OBJECT.invokeExact(handle, timeoutMs);
        } catch (Throwable t) {
            throw new BackendException("WaitForSingleObject failed", t);
        }
    }

    // Structure layouts

    private static final MemoryLayout COORD_LAYOUT = MemoryLayout.structLayout(
            ValueLayout.JAVA_SHORT.withName("X"),
            ValueLayout.JAVA_SHORT.withName("Y")
    );

    private static final MemoryLayout SMALL_RECT_LAYOUT = MemoryLayout.structLayout(
            ValueLayout.JAVA_SHORT.withName("Left"),
            ValueLayout.JAVA_SHORT.withName("Top"),
            ValueLayout.JAVA_SHORT.withName("Right"),
            ValueLayout.JAVA_SHORT.withName("Bottom")
    );

    /**
     * Layout for CONSOLE_SCREEN_BUFFER_INFO structure.
     */
    public static final MemoryLayout CONSOLE_SCREEN_BUFFER_INFO_LAYOUT = MemoryLayout.structLayout(
            COORD_LAYOUT.withName("dwSize"),
            COORD_LAYOUT.withName("dwCursorPosition"),
            ValueLayout.JAVA_SHORT.withName("wAttributes"),
            SMALL_RECT_LAYOUT.withName("srWindow"),
            COORD_LAYOUT.withName("dwMaximumWindowSize")
    );

    private static final MemoryLayout KEY_EVENT_RECORD_LAYOUT = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT.withName("bKeyDown"),
            ValueLayout.JAVA_SHORT.withName("wRepeatCount"),
            ValueLayout.JAVA_SHORT.withName("wVirtualKeyCode"),
            ValueLayout.JAVA_SHORT.withName("wVirtualScanCode"),
            ValueLayout.JAVA_SHORT.withName("uChar"),
            ValueLayout.JAVA_INT.withName("dwControlKeyState")
    );

    /**
     * Layout for INPUT_RECORD structure.
     */
    public static final MemoryLayout INPUT_RECORD_LAYOUT = MemoryLayout.structLayout(
            ValueLayout.JAVA_SHORT.withName("EventType"),
            MemoryLayout.paddingLayout(2),
            KEY_EVENT_RECORD_LAYOUT.withName("Event")
    );

    /** VarHandle for the left coordinate of the console window rectangle. */
    public static final VarHandle CSBI_WINDOW_LEFT = CONSOLE_SCREEN_BUFFER_INFO_LAYOUT.varHandle(
            MemoryLayout.PathElement.groupElement("srWindow"),
            MemoryLayout.PathElement.groupElement("Left"));
    /** VarHandle for the top coordinate of the console window rectangle. */
    public static final VarHandle CSBI_WINDOW_TOP = CONSOLE_SCREEN_BUFFER_INFO_LAYOUT.varHandle(
            MemoryLayout.PathElement.groupElement("srWindow"),
            MemoryLayout.PathElement.groupElement("Top"));
    /** VarHandle for the right coordinate of the console window rectangle. */
    public static final VarHandle CSBI_WINDOW_RIGHT = CONSOLE_SCREEN_BUFFER_INFO_LAYOUT.varHandle(
            MemoryLayout.PathElement.groupElement("srWindow"),
            MemoryLayout.PathElement.groupElement("Right"));
    /** VarHandle for the bottom coordinate of the console window rectangle. */
    public static final VarHandle CSBI_WINDOW_BOTTOM = CONSOLE_SCREEN_BUFFER_INFO_LAYOUT.varHandle(
            MemoryLayout.PathElement.groupElement("srWindow"),
            MemoryLayout.PathElement.groupElement("Bottom"));

    /** VarHandle for the event type field of an INPUT_RECORD. */
    public static final VarHandle IR_EVENT_TYPE = INPUT_RECORD_LAYOUT.varHandle(
            MemoryLayout.PathElement.groupElement("EventType"));
    /** VarHandle for the key-down flag in a KEY_EVENT_RECORD. */
    public static final VarHandle IR_KEY_DOWN = INPUT_RECORD_LAYOUT.varHandle(
            MemoryLayout.PathElement.groupElement("Event"),
            MemoryLayout.PathElement.groupElement("bKeyDown"));
    /** VarHandle for the Unicode character in a KEY_EVENT_RECORD. */
    public static final VarHandle IR_CHAR = INPUT_RECORD_LAYOUT.varHandle(
            MemoryLayout.PathElement.groupElement("Event"),
            MemoryLayout.PathElement.groupElement("uChar"));

    /**
     * Allocates a CONSOLE_SCREEN_BUFFER_INFO structure.
     *
     * @param arena the arena to allocate in
     * @return the allocated memory segment
     */
    public static MemorySegment allocateConsoleScreenBufferInfo(Arena arena) {
        return arena.allocate(CONSOLE_SCREEN_BUFFER_INFO_LAYOUT);
    }

    /**
     * Allocates an INPUT_RECORD structure.
     *
     * @param arena the arena to allocate in
     * @return the allocated memory segment
     */
    public static MemorySegment allocateInputRecord(Arena arena) {
        return arena.allocate(INPUT_RECORD_LAYOUT);
    }
}
