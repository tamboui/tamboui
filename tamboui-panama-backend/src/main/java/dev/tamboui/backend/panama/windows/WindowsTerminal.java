/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.backend.panama.windows;

import dev.tamboui.backend.panama.PlatformTerminal;
import dev.tamboui.layout.Size;

import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Windows terminal operations using Panama FFI.
 * <p>
 * This class provides higher-level terminal operations built on top of
 * the low-level Kernel32 bindings.
 */
public final class WindowsTerminal implements PlatformTerminal {

    private final Arena arena;
    private final MemorySegment inputHandle;
    private final MemorySegment outputHandle;
    private final MemorySegment screenBufferInfo;
    private final MemorySegment inputRecord;
    private final MemorySegment intBuffer;

    private final int savedInputMode;
    private final int savedOutputMode;
    private boolean rawModeEnabled;
    private volatile Runnable resizeHandler;

    /**
     * Creates a new Windows terminal instance.
     *
     * @throws IOException if the terminal cannot be initialized
     */
    public WindowsTerminal() throws IOException {
        this.arena = Arena.ofShared();

        inputHandle = Kernel32.getStdHandle(Kernel32.STD_INPUT_HANDLE);
        outputHandle = Kernel32.getStdHandle(Kernel32.STD_OUTPUT_HANDLE);

        if (inputHandle.address() == Kernel32.INVALID_HANDLE_VALUE ||
            outputHandle.address() == Kernel32.INVALID_HANDLE_VALUE) {
            arena.close();
            throw new IOException("Failed to get console handles");
        }

        screenBufferInfo = Kernel32.allocateConsoleScreenBufferInfo(arena);
        inputRecord = Kernel32.allocateInputRecord(arena);
        intBuffer = arena.allocate(ValueLayout.JAVA_INT);

        // Save original console modes
        if (Kernel32.getConsoleMode(inputHandle, intBuffer) == 0) {
            arena.close();
            throw new IOException("Failed to get input console mode");
        }
        savedInputMode = intBuffer.get(ValueLayout.JAVA_INT, 0);

        if (Kernel32.getConsoleMode(outputHandle, intBuffer) == 0) {
            arena.close();
            throw new IOException("Failed to get output console mode");
        }
        savedOutputMode = intBuffer.get(ValueLayout.JAVA_INT, 0);

        rawModeEnabled = false;
    }

    @Override
    public void enableRawMode() throws IOException {
        if (rawModeEnabled) {
            return;
        }

        // Set input mode: disable line input, echo, and processed input; enable VT input
        var newInputMode = savedInputMode
                & ~(Kernel32.ENABLE_LINE_INPUT | Kernel32.ENABLE_ECHO_INPUT | Kernel32.ENABLE_PROCESSED_INPUT)
                | Kernel32.ENABLE_VIRTUAL_TERMINAL_INPUT | Kernel32.ENABLE_WINDOW_INPUT;

        if (Kernel32.setConsoleMode(inputHandle, newInputMode) == 0) {
            throw new IOException("Failed to set input console mode (error=" + Kernel32.getLastError() + ")");
        }

        // Set output mode: enable VT processing
        var newOutputMode = savedOutputMode
                | Kernel32.ENABLE_VIRTUAL_TERMINAL_PROCESSING | Kernel32.ENABLE_PROCESSED_OUTPUT;

        if (Kernel32.setConsoleMode(outputHandle, newOutputMode) == 0) {
            Kernel32.setConsoleMode(inputHandle, savedInputMode);
            throw new IOException("Failed to set output console mode (error=" + Kernel32.getLastError() + ")");
        }

        rawModeEnabled = true;
    }

    @Override
    public void disableRawMode() throws IOException {
        if (!rawModeEnabled) {
            return;
        }

        if (Kernel32.setConsoleMode(inputHandle, savedInputMode) == 0) {
            throw new IOException("Failed to restore input console mode");
        }

        if (Kernel32.setConsoleMode(outputHandle, savedOutputMode) == 0) {
            throw new IOException("Failed to restore output console mode");
        }

        rawModeEnabled = false;
    }

    @Override
    public Size getSize() throws IOException {
        if (Kernel32.getConsoleScreenBufferInfo(outputHandle, screenBufferInfo) == 0) {
            throw new IOException("Failed to get console screen buffer info (error=" + Kernel32.getLastError() + ")");
        }

        var left = (short) Kernel32.CSBI_WINDOW_LEFT.get(screenBufferInfo, 0L);
        var right = (short) Kernel32.CSBI_WINDOW_RIGHT.get(screenBufferInfo, 0L);
        var top = (short) Kernel32.CSBI_WINDOW_TOP.get(screenBufferInfo, 0L);
        var bottom = (short) Kernel32.CSBI_WINDOW_BOTTOM.get(screenBufferInfo, 0L);

        var cols = right - left + 1;
        var rows = bottom - top + 1;

        if (cols > 0 && rows > 0) {
            return new Size(cols, rows);
        }
        throw new IOException("Invalid console size");
    }

    @Override
    public int read(int timeoutMs) throws IOException {
        if (Kernel32.getNumberOfConsoleInputEvents(inputHandle, intBuffer) == 0) {
            throw new IOException("Failed to get number of console input events");
        }

        if (intBuffer.get(ValueLayout.JAVA_INT, 0) == 0) {
            if (timeoutMs == 0) {
                return -2; // Non-blocking, no data
            }
            // Wait for input with timeout using WaitForSingleObject
            var waitResult = Kernel32.waitForSingleObject(inputHandle, timeoutMs);
            if (waitResult == Kernel32.WAIT_TIMEOUT) {
                return -2; // Timeout, no data
            }
            if (waitResult == Kernel32.WAIT_FAILED) {
                throw new IOException("WaitForSingleObject failed (error=" + Kernel32.getLastError() + ")");
            }
            // WAIT_OBJECT_0: input is available, proceed to read
        }

        if (Kernel32.readConsoleInput(inputHandle, inputRecord, 1, intBuffer) == 0) {
            throw new IOException("Failed to read console input");
        }

        if (intBuffer.get(ValueLayout.JAVA_INT, 0) == 0) {
            return -2; // No data
        }

        var eventType = (short) Kernel32.IR_EVENT_TYPE.get(inputRecord, 0L);
        if (eventType == Kernel32.KEY_EVENT) {
            var keyDown = (int) Kernel32.IR_KEY_DOWN.get(inputRecord, 0L);
            if (keyDown != 0) {
                var ch = (short) Kernel32.IR_CHAR.get(inputRecord, 0L);
                if (ch != 0) {
                    return ch & 0xFFFF;
                }
            }
        } else if (eventType == Kernel32.WINDOW_BUFFER_SIZE_EVENT) {
            var handler = resizeHandler;
            if (handler != null) {
                handler.run();
            }
        }

        return -2; // No relevant data
    }

    @Override
    public int peek(int timeoutMs) throws IOException {
        if (Kernel32.getNumberOfConsoleInputEvents(inputHandle, intBuffer) == 0) {
            throw new IOException("Failed to get number of console input events");
        }
        return intBuffer.get(ValueLayout.JAVA_INT, 0) > 0 ? 0 : -2;
    }

    @Override
    public void write(byte[] data) throws IOException {
        write(data, 0, data.length);
    }

    @Override
    public void write(byte[] buffer, int offset, int length) throws IOException {
        if (length == 0) {
            return;
        }
        // Windows Console API uses UTF-16, so we need to convert from bytes
        write(new String(buffer, offset, length, StandardCharsets.UTF_8));
    }

    @Override
    public void write(String s) throws IOException {
        if (s.isEmpty()) {
            return;
        }

        try (var writeArena = Arena.ofConfined()) {
            var chars = s.toCharArray();
            var buffer = writeArena.allocate(ValueLayout.JAVA_CHAR, chars.length);
            for (var i = 0; i < chars.length; i++) {
                buffer.setAtIndex(ValueLayout.JAVA_CHAR, i, chars[i]);
            }

            var written = writeArena.allocate(ValueLayout.JAVA_INT);
            if (Kernel32.writeConsole(outputHandle, buffer, chars.length, written, MemorySegment.NULL) == 0) {
                throw new IOException("Write failed (error=" + Kernel32.getLastError() + ")");
            }
        }
    }

    @Override
    public Charset getCharset() {
        return StandardCharsets.UTF_8;
    }

    @Override
    public boolean isRawModeEnabled() {
        return rawModeEnabled;
    }

    @Override
    public void onResize(Runnable handler) {
        this.resizeHandler = handler;
    }

    @Override
    public void close() throws IOException {
        try {
            if (rawModeEnabled) {
                disableRawMode();
            }
        } finally {
            arena.close();
        }
    }
}
