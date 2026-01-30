/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.internal.record;

import java.io.IOException;
import java.io.Writer;
import java.util.Locale;

/**
 * Writes Asciinema v2 cast format (newline-delimited JSON). This is an internal
 * API and not part of the public contract.
 *
 * <p>
 * The asciicast v2 format consists of:
 * <ul>
 * <li>Line 1: JSON header with version, width, height, and optional
 * metadata</li>
 * <li>Following lines: Event arrays [time, event_type, data]</li>
 * </ul>
 *
 * @see <a href="https://docs.asciinema.org/manual/asciicast/v2/">asciicast v2
 *      specification</a>
 */
final class AsciinemaWriter {

    private AsciinemaWriter() {
        // Utility class
    }

    /**
     * Writes the cast file header.
     *
     * @param out
     *            the writer to write to
     * @param width
     *            terminal width in columns
     * @param height
     *            terminal height in rows
     * @throws IOException
     *             if an I/O error occurs
     */
    static void writeHeader(Writer out, int width, int height) throws IOException {
        long timestamp = System.currentTimeMillis() / 1000;
        out.write(String.format(
                "{\"version\": 2, \"width\": %d, \"height\": %d, \"timestamp\": %d, \"env\": {\"TERM\": \"xterm-256color\"}}%n",
                width, height, timestamp));
    }

    /**
     * Writes an output event to the cast file.
     *
     * @param out
     *            the writer to write to
     * @param timeSeconds
     *            time offset in seconds from the start of the recording
     * @param data
     *            the terminal output data (may contain ANSI escape sequences)
     * @throws IOException
     *             if an I/O error occurs
     */
    static void writeOutputEvent(Writer out, double timeSeconds, String data) throws IOException {
        out.write(String.format(Locale.US, "[%.6f, \"o\", \"%s\"]%n", timeSeconds,
                escapeJsonString(data)));
    }

    /**
     * Escapes a string for use in JSON. Handles special characters, control codes,
     * and Unicode.
     *
     * @param s
     *            the string to escape
     * @return the escaped string (without surrounding quotes)
     */
    static String escapeJsonString(String s) {
        StringBuilder sb = new StringBuilder(s.length() * 2);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' :
                    sb.append("\\\"");
                    break;
                case '\\' :
                    sb.append("\\\\");
                    break;
                case '\b' :
                    sb.append("\\b");
                    break;
                case '\f' :
                    sb.append("\\f");
                    break;
                case '\n' :
                    sb.append("\\n");
                    break;
                case '\r' :
                    sb.append("\\r");
                    break;
                case '\t' :
                    sb.append("\\t");
                    break;
                default :
                    if (c < 0x20) {
                        // Control character - use Unicode escape
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }
}
