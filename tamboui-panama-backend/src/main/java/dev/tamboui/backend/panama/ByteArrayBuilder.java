/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.backend.panama;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Efficient byte array builder optimized for terminal output.
 * <p>
 * This class is designed for the common case of ASCII escape sequences
 * mixed with UTF-8 cell symbols. It avoids the overhead of StringBuilder
 * followed by String.getBytes() by building the byte array directly.
 * <p>
 * ASCII operations (escape sequences, integers) write bytes directly.
 * UTF-8 operations (cell symbols) encode only when necessary.
 */
public final class ByteArrayBuilder {

    private static final byte ESC = 0x1B;
    private static final byte[] CSI_BYTES = {ESC, '['};

    private byte[] buffer;
    private int position;

    /**
     * Creates a new ByteArrayBuilder with the specified initial capacity.
     *
     * @param initialCapacity the initial buffer size in bytes
     */
    public ByteArrayBuilder(int initialCapacity) {
        this.buffer = new byte[initialCapacity];
        this.position = 0;
    }

    /**
     * Ensures the buffer has capacity for at least the specified number of additional bytes.
     *
     * @param additionalBytes the number of additional bytes needed
     */
    private void ensureCapacity(int additionalBytes) {
        int required = position + additionalBytes;
        if (required > buffer.length) {
            int newCapacity = Math.max(buffer.length * 2, required);
            buffer = Arrays.copyOf(buffer, newCapacity);
        }
    }

    /**
     * Appends the CSI (Control Sequence Introducer) escape sequence.
     * <p>
     * This is equivalent to appending ESC followed by '['.
     *
     * @return this builder for chaining
     */
    public ByteArrayBuilder csi() {
        ensureCapacity(2);
        buffer[position++] = ESC;
        buffer[position++] = '[';
        return this;
    }

    /**
     * Appends a single byte.
     *
     * @param b the byte to append
     * @return this builder for chaining
     */
    public ByteArrayBuilder append(byte b) {
        ensureCapacity(1);
        buffer[position++] = b;
        return this;
    }

    /**
     * Appends a byte array.
     *
     * @param bytes the bytes to append
     * @return this builder for chaining
     */
    public ByteArrayBuilder append(byte[] bytes) {
        ensureCapacity(bytes.length);
        System.arraycopy(bytes, 0, buffer, position, bytes.length);
        position += bytes.length;
        return this;
    }

    /**
     * Appends an ASCII string without charset encoding.
     * <p>
     * This method assumes the string contains only ASCII characters (0-127).
     * It is faster than {@link #appendUtf8(String)} for escape sequences
     * and other ASCII-only content.
     *
     * @param s the ASCII string to append
     * @return this builder for chaining
     */
    public ByteArrayBuilder appendAscii(String s) {
        int len = s.length();
        ensureCapacity(len);
        for (int i = 0; i < len; i++) {
            buffer[position++] = (byte) s.charAt(i);
        }
        return this;
    }

    /**
     * Appends a non-negative integer as ASCII digits.
     * <p>
     * This method converts the integer directly to ASCII digit bytes
     * without creating intermediate String objects.
     *
     * @param value the non-negative integer to append
     * @return this builder for chaining
     */
    public ByteArrayBuilder appendInt(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("Negative values not supported");
        }
        if (value == 0) {
            ensureCapacity(1);
            buffer[position++] = '0';
            return this;
        }

        // Count digits
        int temp = value;
        int digits = 0;
        while (temp > 0) {
            digits++;
            temp /= 10;
        }

        ensureCapacity(digits);

        // Write digits in reverse order
        int endPos = position + digits;
        int writePos = endPos - 1;
        temp = value;
        while (temp > 0) {
            buffer[writePos--] = (byte) ('0' + (temp % 10));
            temp /= 10;
        }
        position = endPos;
        return this;
    }

    /**
     * Appends a UTF-8 encoded string.
     * <p>
     * Use this method for cell symbols which may contain multi-byte
     * UTF-8 characters (box-drawing, CJK, emoji, etc.).
     * <p>
     * Optimized for the common case of ASCII-only strings to avoid
     * the overhead of {@link String#getBytes(java.nio.charset.Charset)}.
     *
     * @param s the string to append as UTF-8
     * @return this builder for chaining
     */
    public ByteArrayBuilder appendUtf8(String s) {
        int len = s.length();
        if (len == 0) {
            return this;
        }

        // Fast path for single ASCII character (very common for cell symbols)
        if (len == 1) {
            char c = s.charAt(0);
            if (c < 128) {
                ensureCapacity(1);
                buffer[position++] = (byte) c;
                return this;
            }
        }

        // Check if all characters are ASCII
        boolean allAscii = true;
        for (int i = 0; i < len; i++) {
            if (s.charAt(i) >= 128) {
                allAscii = false;
                break;
            }
        }

        if (allAscii) {
            // Fast path for ASCII-only strings
            ensureCapacity(len);
            for (int i = 0; i < len; i++) {
                buffer[position++] = (byte) s.charAt(i);
            }
            return this;
        }

        // Fall back to full UTF-8 encoding for strings with non-ASCII characters
        byte[] utf8 = s.getBytes(StandardCharsets.UTF_8);
        return append(utf8);
    }

    /**
     * Returns the internal buffer.
     * <p>
     * The returned array may be larger than the actual content.
     * Use {@link #length()} to determine the valid data length.
     *
     * @return the internal byte array
     */
    public byte[] buffer() {
        return buffer;
    }

    /**
     * Returns the current length of valid data in the buffer.
     *
     * @return the number of bytes written to the buffer
     */
    public int length() {
        return position;
    }

    /**
     * Resets the builder for reuse.
     * <p>
     * This method clears the position but retains the allocated buffer
     * for efficient reuse across frames.
     */
    public void reset() {
        position = 0;
    }

    /**
     * Creates a new byte array containing only the valid data.
     * <p>
     * This method allocates a new array. For zero-copy writes,
     * use {@link #buffer()} with {@link #length()} instead.
     *
     * @return a new byte array with the buffer contents
     */
    public byte[] toByteArray() {
        return Arrays.copyOf(buffer, position);
    }
}
