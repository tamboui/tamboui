/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.internal.record;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.AnsiColor;
import dev.tamboui.style.Color;
import dev.tamboui.style.Modifier;
import dev.tamboui.style.Style;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Captures System.out output by parsing ANSI escape sequences and maintaining
 * a virtual terminal buffer. Used for recording inline demos that write directly
 * to System.out rather than using Backend.draw().
 * <p>
 * This is an internal API and not part of the public contract.
 */
public final class AnsiTerminalCapture extends OutputStream {

    private static final char ESC = '\u001b';

    private static AnsiTerminalCapture instance;
    private static PrintStream originalOut;

    private final PrintStream tee;
    private final Buffer buffer;
    private final List<Buffer> frames;
    private final int fps;
    private final long maxDurationMs;
    private final long startTime;

    // Parser state
    private final StringBuilder escapeBuffer = new StringBuilder();
    private boolean inEscape = false;
    private boolean inCsi = false;

    // Terminal state
    private int cursorX = 0;
    private int cursorY = 0;
    private int savedCursorX = 0;
    private int savedCursorY = 0;
    private Style currentStyle = Style.EMPTY;

    // Frame capture timing
    private long lastCaptureTime = 0;

    // UTF-8 decoding - accumulate bytes and decode on flush
    private final ByteArrayOutputStream pendingBytes = new ByteArrayOutputStream();

    private AnsiTerminalCapture(PrintStream tee, RecordingConfig config) {
        this.tee = tee;
        this.buffer = Buffer.empty(new Rect(0, 0, config.width(), config.height()));
        this.frames = new ArrayList<>();
        this.fps = config.fps();
        this.maxDurationMs = config.maxDurationMs();
        this.startTime = System.currentTimeMillis();
    }

    /**
     * Installs the capture by replacing System.out.
     * Does nothing if already installed or if config is null.
     *
     * @param config the recording configuration
     */
    public static synchronized void install(RecordingConfig config) {
        if (instance != null || config == null) {
            return;
        }

        originalOut = System.out;
        instance = new AnsiTerminalCapture(originalOut, config);
        try {
            System.setOut(new PrintStream(instance, true, "UTF-8"));
        } catch (java.io.UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 not supported", e);
        }
    }

    /**
     * Uninstalls the capture and restores original System.out.
     * Returns the captured frames, or empty list if not installed.
     */
    public static synchronized List<Buffer> uninstall() {
        if (instance == null) {
            return new ArrayList<>();
        }

        // Capture final frame
        instance.captureFrame();

        List<Buffer> capturedFrames = new ArrayList<>(instance.frames);
        System.setOut(originalOut);
        instance = null;
        originalOut = null;

        return capturedFrames;
    }

    /**
     * Returns true if capture is currently installed.
     */
    public static synchronized boolean isInstalled() {
        return instance != null;
    }

    @Override
    public void write(int b) throws IOException {
        // Pass through to original output
        tee.write(b);
        // Force flush on newlines to ensure timely output when piped
        if (b == '\n') {
            tee.flush();
        }

        // Accumulate byte for later decoding
        pendingBytes.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        // Pass through to original output
        tee.write(b, off, len);
        // Force flush to ensure output appears when piped
        tee.flush();

        // Accumulate bytes for later decoding
        pendingBytes.write(b, off, len);
    }

    private void processPendingBytes() {
        if (pendingBytes.size() == 0) {
            return;
        }

        byte[] bytes = pendingBytes.toByteArray();
        pendingBytes.reset();

        // Decode accumulated bytes as UTF-8
        String decoded = new String(bytes, StandardCharsets.UTF_8);

        // Process each character
        for (int i = 0; i < decoded.length(); i++) {
            processChar(decoded.charAt(i));
        }
    }

    @Override
    public void flush() throws IOException {
        tee.flush();
        processPendingBytes();
        captureFrame();
    }

    @Override
    public void close() throws IOException {
        tee.flush();
    }

    private void processChar(char c) {
        if (inEscape) {
            processEscapeChar(c);
        } else if (c == ESC) {
            inEscape = true;
            escapeBuffer.setLength(0);
        } else {
            processRegularChar(c);
        }
    }

    private void processEscapeChar(char c) {
        escapeBuffer.append(c);

        if (!inCsi && c == '[') {
            inCsi = true;
            return;
        }

        if (inCsi) {
            // CSI sequence ends with a letter
            if (Character.isLetter(c)) {
                handleCsiSequence(escapeBuffer.toString());
                inEscape = false;
                inCsi = false;
            }
        } else {
            // Non-CSI escape sequence (we don't handle many of these)
            inEscape = false;
        }
    }

    private void handleCsiSequence(String seq) {
        // seq is like "[0m" or "[10;20H" or "[2J"
        if (seq.length() < 2) return;

        char command = seq.charAt(seq.length() - 1);
        String params = seq.substring(1, seq.length() - 1);

        switch (command) {
            case 'A':
                moveCursorUp(parseIntOrDefault(params, 1));
                break;
            case 'B':
                moveCursorDown(parseIntOrDefault(params, 1));
                break;
            case 'C':
                moveCursorRight(parseIntOrDefault(params, 1));
                break;
            case 'D':
                moveCursorLeft(parseIntOrDefault(params, 1));
                break;
            case 'H':
            case 'f':
                setCursorPosition(params);
                break;
            case 'J':
                eraseDisplay(parseIntOrDefault(params, 0));
                break;
            case 'K':
                eraseLine(parseIntOrDefault(params, 0));
                break;
            case 'L':
                insertLines(parseIntOrDefault(params, 1));
                break;
            case 'M':
                deleteLines(parseIntOrDefault(params, 1));
                break;
            case 'm':
                handleSgr(params);
                break;
            case 's':
                saveCursor();
                break;
            case 'u':
                restoreCursor();
                break;
            default:
                // Ignore unknown sequences
                break;
        }
    }

    private void processRegularChar(char c) {
        switch (c) {
            case '\n':
                cursorY++;
                if (cursorY >= buffer.height()) {
                    scrollUp();
                    cursorY = buffer.height() - 1;
                }
                break;
            case '\r':
                cursorX = 0;
                break;
            case '\t':
                int spaces = 8 - (cursorX % 8);
                for (int i = 0; i < spaces && cursorX < buffer.width(); i++) {
                    buffer.set(cursorX++, cursorY, new Cell(" ", currentStyle));
                }
                break;
            case '\b':
                if (cursorX > 0) cursorX--;
                break;
            default:
                if (c >= 32 && cursorX < buffer.width() && cursorY < buffer.height()) {
                    buffer.set(cursorX, cursorY, new Cell(String.valueOf(c), currentStyle));
                    cursorX++;
                    if (cursorX >= buffer.width()) {
                        cursorX = 0;
                        cursorY++;
                        if (cursorY >= buffer.height()) {
                            scrollUp();
                            cursorY = buffer.height() - 1;
                        }
                    }
                }
                break;
        }
    }

    private void moveCursorUp(int n) {
        cursorY = Math.max(0, cursorY - n);
    }

    private void moveCursorDown(int n) {
        cursorY = Math.min(buffer.height() - 1, cursorY + n);
    }

    private void moveCursorRight(int n) {
        cursorX = Math.min(buffer.width() - 1, cursorX + n);
    }

    private void moveCursorLeft(int n) {
        cursorX = Math.max(0, cursorX - n);
    }

    private void setCursorPosition(String params) {
        String[] parts = params.split(";");
        int row = parts.length > 0 ? parseIntOrDefault(parts[0], 1) : 1;
        int col = parts.length > 1 ? parseIntOrDefault(parts[1], 1) : 1;
        cursorY = Math.max(0, Math.min(buffer.height() - 1, row - 1));
        cursorX = Math.max(0, Math.min(buffer.width() - 1, col - 1));
    }

    private void eraseDisplay(int mode) {
        switch (mode) {
            case 0:
                // Erase from cursor to end of display
                eraseLine(0);
                for (int y = cursorY + 1; y < buffer.height(); y++) {
                    clearLine(y);
                }
                break;
            case 1:
                // Erase from start to cursor
                for (int y = 0; y < cursorY; y++) {
                    clearLine(y);
                }
                for (int x = 0; x <= cursorX && x < buffer.width(); x++) {
                    buffer.set(x, cursorY, Cell.EMPTY);
                }
                break;
            case 2:
            case 3:
                // Erase entire display
                buffer.clear();
                break;
            default:
                break;
        }
    }

    private void eraseLine(int mode) {
        switch (mode) {
            case 0:
                // Erase from cursor to end of line
                for (int x = cursorX; x < buffer.width(); x++) {
                    buffer.set(x, cursorY, Cell.EMPTY);
                }
                break;
            case 1:
                // Erase from start of line to cursor
                for (int x = 0; x <= cursorX && x < buffer.width(); x++) {
                    buffer.set(x, cursorY, Cell.EMPTY);
                }
                break;
            case 2:
                clearLine(cursorY);
                break;
            default:
                break;
        }
    }

    private void clearLine(int y) {
        if (y >= 0 && y < buffer.height()) {
            for (int x = 0; x < buffer.width(); x++) {
                buffer.set(x, y, Cell.EMPTY);
            }
        }
    }

    private void insertLines(int n) {
        // Scroll down from cursor position
        for (int i = 0; i < n; i++) {
            for (int y = buffer.height() - 1; y > cursorY; y--) {
                for (int x = 0; x < buffer.width(); x++) {
                    buffer.set(x, y, buffer.get(x, y - 1));
                }
            }
            clearLine(cursorY);
        }
    }

    private void deleteLines(int n) {
        // Scroll up from cursor position
        for (int i = 0; i < n; i++) {
            for (int y = cursorY; y < buffer.height() - 1; y++) {
                for (int x = 0; x < buffer.width(); x++) {
                    buffer.set(x, y, buffer.get(x, y + 1));
                }
            }
            clearLine(buffer.height() - 1);
        }
    }

    private void scrollUp() {
        for (int y = 0; y < buffer.height() - 1; y++) {
            for (int x = 0; x < buffer.width(); x++) {
                buffer.set(x, y, buffer.get(x, y + 1));
            }
        }
        clearLine(buffer.height() - 1);
    }

    private void saveCursor() {
        savedCursorX = cursorX;
        savedCursorY = cursorY;
    }

    private void restoreCursor() {
        cursorX = savedCursorX;
        cursorY = savedCursorY;
    }

    private void handleSgr(String params) {
        if (params.isEmpty()) {
            currentStyle = Style.EMPTY;
            return;
        }

        String[] codes = params.split(";");
        int i = 0;
        while (i < codes.length) {
            int code = parseIntOrDefault(codes[i], 0);
            switch (code) {
                case 0:
                    currentStyle = Style.EMPTY;
                    break;
                case 1:
                    currentStyle = currentStyle.addModifier(Modifier.BOLD);
                    break;
                case 2:
                    currentStyle = currentStyle.addModifier(Modifier.DIM);
                    break;
                case 3:
                    currentStyle = currentStyle.addModifier(Modifier.ITALIC);
                    break;
                case 4:
                    currentStyle = currentStyle.addModifier(Modifier.UNDERLINED);
                    break;
                case 7:
                    currentStyle = currentStyle.addModifier(Modifier.REVERSED);
                    break;
                case 9:
                    currentStyle = currentStyle.addModifier(Modifier.CROSSED_OUT);
                    break;
                case 22:
                    currentStyle = currentStyle.removeModifier(Modifier.BOLD).removeModifier(Modifier.DIM);
                    break;
                case 23:
                    currentStyle = currentStyle.removeModifier(Modifier.ITALIC);
                    break;
                case 24:
                    currentStyle = currentStyle.removeModifier(Modifier.UNDERLINED);
                    break;
                case 27:
                    currentStyle = currentStyle.removeModifier(Modifier.REVERSED);
                    break;
                case 29:
                    currentStyle = currentStyle.removeModifier(Modifier.CROSSED_OUT);
                    break;
                case 30:
                    currentStyle = currentStyle.fg(Color.ansi(AnsiColor.BLACK));
                    break;
                case 31:
                    currentStyle = currentStyle.fg(Color.ansi(AnsiColor.RED));
                    break;
                case 32:
                    currentStyle = currentStyle.fg(Color.ansi(AnsiColor.GREEN));
                    break;
                case 33:
                    currentStyle = currentStyle.fg(Color.ansi(AnsiColor.YELLOW));
                    break;
                case 34:
                    currentStyle = currentStyle.fg(Color.ansi(AnsiColor.BLUE));
                    break;
                case 35:
                    currentStyle = currentStyle.fg(Color.ansi(AnsiColor.MAGENTA));
                    break;
                case 36:
                    currentStyle = currentStyle.fg(Color.ansi(AnsiColor.CYAN));
                    break;
                case 37:
                    currentStyle = currentStyle.fg(Color.ansi(AnsiColor.WHITE));
                    break;
                case 38:
                    // Extended foreground color
                    Color fgColor = parseExtendedColor(codes, i + 1);
                    if (fgColor != null) {
                        currentStyle = currentStyle.fg(fgColor);
                        i += (codes.length > i + 1 && "5".equals(codes[i + 1])) ? 2 : 4;
                    }
                    break;
                case 39:
                    currentStyle = currentStyle.fg(Color.RESET);
                    break;
                case 40:
                    currentStyle = currentStyle.bg(Color.ansi(AnsiColor.BLACK));
                    break;
                case 41:
                    currentStyle = currentStyle.bg(Color.ansi(AnsiColor.RED));
                    break;
                case 42:
                    currentStyle = currentStyle.bg(Color.ansi(AnsiColor.GREEN));
                    break;
                case 43:
                    currentStyle = currentStyle.bg(Color.ansi(AnsiColor.YELLOW));
                    break;
                case 44:
                    currentStyle = currentStyle.bg(Color.ansi(AnsiColor.BLUE));
                    break;
                case 45:
                    currentStyle = currentStyle.bg(Color.ansi(AnsiColor.MAGENTA));
                    break;
                case 46:
                    currentStyle = currentStyle.bg(Color.ansi(AnsiColor.CYAN));
                    break;
                case 47:
                    currentStyle = currentStyle.bg(Color.ansi(AnsiColor.WHITE));
                    break;
                case 48:
                    // Extended background color
                    Color bgColor = parseExtendedColor(codes, i + 1);
                    if (bgColor != null) {
                        currentStyle = currentStyle.bg(bgColor);
                        i += (codes.length > i + 1 && "5".equals(codes[i + 1])) ? 2 : 4;
                    }
                    break;
                case 49:
                    currentStyle = currentStyle.bg(Color.RESET);
                    break;
                case 90:
                    currentStyle = currentStyle.fg(Color.ansi(AnsiColor.BRIGHT_BLACK));
                    break;
                case 91:
                    currentStyle = currentStyle.fg(Color.ansi(AnsiColor.BRIGHT_RED));
                    break;
                case 92:
                    currentStyle = currentStyle.fg(Color.ansi(AnsiColor.BRIGHT_GREEN));
                    break;
                case 93:
                    currentStyle = currentStyle.fg(Color.ansi(AnsiColor.BRIGHT_YELLOW));
                    break;
                case 94:
                    currentStyle = currentStyle.fg(Color.ansi(AnsiColor.BRIGHT_BLUE));
                    break;
                case 95:
                    currentStyle = currentStyle.fg(Color.ansi(AnsiColor.BRIGHT_MAGENTA));
                    break;
                case 96:
                    currentStyle = currentStyle.fg(Color.ansi(AnsiColor.BRIGHT_CYAN));
                    break;
                case 97:
                    currentStyle = currentStyle.fg(Color.ansi(AnsiColor.BRIGHT_WHITE));
                    break;
                case 100:
                    currentStyle = currentStyle.bg(Color.ansi(AnsiColor.BRIGHT_BLACK));
                    break;
                case 101:
                    currentStyle = currentStyle.bg(Color.ansi(AnsiColor.BRIGHT_RED));
                    break;
                case 102:
                    currentStyle = currentStyle.bg(Color.ansi(AnsiColor.BRIGHT_GREEN));
                    break;
                case 103:
                    currentStyle = currentStyle.bg(Color.ansi(AnsiColor.BRIGHT_YELLOW));
                    break;
                case 104:
                    currentStyle = currentStyle.bg(Color.ansi(AnsiColor.BRIGHT_BLUE));
                    break;
                case 105:
                    currentStyle = currentStyle.bg(Color.ansi(AnsiColor.BRIGHT_MAGENTA));
                    break;
                case 106:
                    currentStyle = currentStyle.bg(Color.ansi(AnsiColor.BRIGHT_CYAN));
                    break;
                case 107:
                    currentStyle = currentStyle.bg(Color.ansi(AnsiColor.BRIGHT_WHITE));
                    break;
                default:
                    // Ignore unknown codes
                    break;
            }
            i++;
        }
    }

    private Color parseExtendedColor(String[] codes, int startIndex) {
        if (startIndex >= codes.length) return null;

        String mode = codes[startIndex];
        if ("5".equals(mode) && startIndex + 1 < codes.length) {
            // 256-color mode: 38;5;n or 48;5;n
            int index = parseIntOrDefault(codes[startIndex + 1], 0);
            return Color.indexed(index);
        } else if ("2".equals(mode) && startIndex + 3 < codes.length) {
            // RGB mode: 38;2;r;g;b or 48;2;r;g;b
            int r = parseIntOrDefault(codes[startIndex + 1], 0);
            int g = parseIntOrDefault(codes[startIndex + 2], 0);
            int b = parseIntOrDefault(codes[startIndex + 3], 0);
            return Color.rgb(r, g, b);
        }
        return null;
    }

    private int parseIntOrDefault(String s, int defaultValue) {
        if (s == null || s.isEmpty()) return defaultValue;
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private void captureFrame() {
        long now = System.currentTimeMillis();
        long elapsed = now - startTime;

        // Check duration limit
        if (elapsed > maxDurationMs) {
            return;
        }

        // Throttle based on FPS
        long frameIntervalMs = 1000 / fps;
        if (now - lastCaptureTime >= frameIntervalMs) {
            frames.add(buffer.copy());
            lastCaptureTime = now;
        }
    }
}
