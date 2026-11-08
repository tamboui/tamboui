/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.backend.panama.unix;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Decodes the bytes read from the terminal into Unicode code points, in the terminal's charset.
 * <p>
 * In a single-byte charset, such as ISO-8859-1, each byte is one character. Any other charset
 * is read as UTF-8: the lead byte says how many continuation bytes follow, and they are waited
 * for however long they take, so what a sequence decodes to never depends on timing. A byte
 * that does not continue the sequence is read again as the next key, and the lead byte comes
 * out as it is.
 */
final class InputDecoder {

    /**
     * Reads single bytes from the terminal.
     */
    @FunctionalInterface
    interface ByteReader {

        /**
         * Reads one byte.
         *
         * @param timeoutMs timeout in milliseconds (-1 for infinite, 0 for non-blocking)
         * @return the byte, from 0 to 255, -1 for EOF, or -2 for timeout
         * @throws IOException if reading fails
         */
        int read(int timeoutMs) throws IOException;
    }

    private static final int TIMEOUT = -2;

    private final ByteReader reader;
    // The character of each byte in a single-byte charset, or null to decode UTF-8
    private final char[] singleByteChars;
    // Bytes read after a lead byte that turned out not to be UTF-8; they are read again first
    private final Deque<Integer> unread = new ArrayDeque<>();
    // The UTF-8 sequence read so far, kept between reads until its last byte arrives
    private final int[] sequence = new int[4];
    private int sequenceLength;
    private int bytesRead;

    /**
     * Creates a decoder of the bytes from the given reader.
     *
     * @param reader the reader of the terminal bytes
     * @param charset the charset of the terminal
     */
    InputDecoder(ByteReader reader, Charset charset) {
        this.reader = reader;
        this.singleByteChars = singleByteChars(charset);
    }

    /**
     * Reads the next character.
     * <p>
     * When the timeout passes in the middle of a UTF-8 sequence, this returns -2 and the next
     * call carries on with the sequence.
     *
     * @param timeoutMs timeout in milliseconds (-1 for infinite, 0 for non-blocking)
     * @return the code point, -1 for EOF, or -2 for timeout
     * @throws IOException if reading fails
     */
    int read(int timeoutMs) throws IOException {
        if (bytesRead == 0) {
            int first = nextByte(timeoutMs);
            if (first < 0x80) {
                // ASCII, EOF (-1), or timeout (-2)
                return first;
            }
            if (singleByteChars != null) {
                return singleByteChars[first];
            }
            sequenceLength = sequenceLength(first);
            if (sequenceLength == 1) {
                // A continuation byte, or a byte that is never part of UTF-8
                return first;
            }
            sequence[0] = first;
            bytesRead = 1;
        }
        while (bytesRead < sequenceLength) {
            int b = nextByte(timeoutMs);
            if (b == TIMEOUT) {
                return TIMEOUT;
            }
            if (b < 0 || (b & 0xC0) != 0x80) {
                // EOF, or not a continuation byte: return the lead byte as it is, and read the
                // bytes after it again
                if (b >= 0) {
                    unread.addFirst(b);
                }
                for (int i = bytesRead - 1; i > 0; i--) {
                    unread.addFirst(sequence[i]);
                }
                bytesRead = 0;
                return sequence[0];
            }
            sequence[bytesRead++] = b;
        }
        int codePoint = sequence[0] & (0x3F >> (sequenceLength - 2));
        for (int i = 1; i < sequenceLength; i++) {
            codePoint = (codePoint << 6) | (sequence[i] & 0x3F);
        }
        bytesRead = 0;
        return codePoint;
    }

    private int nextByte(int timeoutMs) throws IOException {
        Integer b = unread.pollFirst();
        return b != null ? b : reader.read(timeoutMs);
    }

    // The bytes of the UTF-8 sequence that starts with this byte, or 1 if it cannot start one
    private static int sequenceLength(int lead) {
        if (lead >= 0xC0 && lead < 0xE0) {
            return 2;
        } else if (lead >= 0xE0 && lead < 0xF0) {
            return 3;
        } else if (lead >= 0xF0 && lead < 0xF8) {
            return 4;
        }
        return 1;
    }

    // The character of each byte value, if the charset uses one byte per character
    private static char[] singleByteChars(Charset charset) {
        if (StandardCharsets.UTF_8.equals(charset) || !charset.canEncode()
                || charset.newEncoder().maxBytesPerChar() != 1.0f) {
            return null;
        }
        byte[] bytes = new byte[256];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) i;
        }
        // a byte the charset does not map comes out as U+FFFD
        String chars = new String(bytes, charset);
        return chars.length() == bytes.length ? chars.toCharArray() : null;
    }
}
