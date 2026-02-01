/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.internal.record;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.error.RuntimeIOException;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Converts multiple Buffer frames into an Asciinema cast file.
 * This is an internal API and not part of the public contract.
 *
 * <p>The asciicast v2 format is a newline-delimited JSON file where:
 * <ul>
 *   <li>First line contains header (terminal size, timestamp, metadata)</li>
 *   <li>Following lines are events [time, "o", data]</li>
 * </ul>
 *
 * @see <a href="https://docs.asciinema.org/manual/asciicast/v2/">asciicast v2 specification</a>
 */
final class AsciinemaAnimation {

    /**
     * ANSI escape sequence to move cursor to home position (top-left).
     */
    private static final String CURSOR_HOME = "\u001b[H";

    /**
     * ANSI escape sequence to clear entire screen.
     */
    private static final String CLEAR_SCREEN = "\u001b[2J";

    private final List<TimedFrame> frames;
    private final int width;
    private final int height;
    private final long totalDurationMs;

    AsciinemaAnimation(List<TimedFrame> frames, int fps) {
        // Capture total duration before deduplication
        this.totalDurationMs = frames.isEmpty() ? 0 :
                frames.get(frames.size() - 1).timestampMs() - frames.get(0).timestampMs();
        this.frames = deduplicateFrames(frames);
        if (!this.frames.isEmpty()) {
            this.width = this.frames.get(0).buffer().width();
            this.height = this.frames.get(0).buffer().height();
        } else {
            this.width = 80;
            this.height = 24;
        }
    }

    /**
     * Removes consecutive duplicate frames, keeping only the first occurrence.
     * The timing is preserved so that the displayed frame duration extends until
     * the next different frame.
     */
    private static List<TimedFrame> deduplicateFrames(List<TimedFrame> frames) {
        if (frames.size() <= 1) {
            return frames;
        }
        List<TimedFrame> deduplicated = new ArrayList<>();
        TimedFrame lastUnique = frames.get(0);
        deduplicated.add(lastUnique);

        for (int i = 1; i < frames.size(); i++) {
            TimedFrame current = frames.get(i);
            if (!current.buffer().equals(lastUnique.buffer())) {
                deduplicated.add(current);
                lastUnique = current;
            }
        }
        return deduplicated;
    }

    /**
     * Creates an animation from plain buffers with evenly-spaced timing based on FPS.
     * Used when real timestamps aren't available (e.g., System.out capture).
     *
     * @param buffers the list of buffer frames
     * @param fps frames per second for timing calculation
     * @return a new AsciinemaAnimation
     */
    static AsciinemaAnimation fromBuffers(List<Buffer> buffers, int fps) {
        List<TimedFrame> timedFrames = new ArrayList<>(buffers.size());
        long frameIntervalMs = 1000 / fps;
        for (int i = 0; i < buffers.size(); i++) {
            timedFrames.add(new TimedFrame(buffers.get(i), i * frameIntervalMs));
        }
        return new AsciinemaAnimation(timedFrames, fps);
    }

    /**
     * Creates a cast file directly from raw captured frames.
     * The raw bytes are output directly, preserving perfect fidelity with the original
     * terminal output including all ANSI escape sequences.
     *
     * @param rawFrames the list of raw captured frames
     * @param width terminal width for header
     * @param height terminal height for header
     * @return the complete cast file content as a string
     */
    static String fromRawFrames(List<RawFrame> rawFrames, int width, int height) {
        if (rawFrames.isEmpty()) {
            return "";
        }

        // Deduplicate consecutive identical frames
        List<RawFrame> deduplicated = deduplicateRawFrames(rawFrames);
        if (deduplicated.isEmpty()) {
            return "";
        }

        StringWriter writer = new StringWriter();
        try {
            AsciinemaWriter.writeHeader(writer, width, height);

            long firstTimestamp = deduplicated.get(0).timestampMs();
            long lastTimestamp = rawFrames.get(rawFrames.size() - 1).timestampMs();

            for (int i = 0; i < deduplicated.size(); i++) {
                RawFrame frame = deduplicated.get(i);
                double timeSeconds = (frame.timestampMs() - firstTimestamp) / 1000.0;

                String output = new String(frame.data(), StandardCharsets.UTF_8);

                AsciinemaWriter.writeOutputEvent(writer, timeSeconds, output);
            }

            // Add a final empty event to preserve total duration
            double finalTimeSeconds = (lastTimestamp - firstTimestamp) / 1000.0;
            double lastFrameTime = (deduplicated.get(deduplicated.size() - 1).timestampMs() - firstTimestamp) / 1000.0;
            if (finalTimeSeconds > lastFrameTime) {
                AsciinemaWriter.writeOutputEvent(writer, finalTimeSeconds, "");
            }
        } catch (IOException e) {
            // StringWriter doesn't throw IOException
            throw new RuntimeIOException("Unexpected IOException when writing Asciinema animation", e);
        }

        return writer.toString();
    }

    /**
     * Removes consecutive duplicate raw frames, keeping only the first occurrence.
     */
    private static List<RawFrame> deduplicateRawFrames(List<RawFrame> frames) {
        if (frames.size() <= 1) {
            return frames;
        }
        List<RawFrame> deduplicated = new ArrayList<>();
        RawFrame lastUnique = frames.get(0);
        deduplicated.add(lastUnique);

        for (int i = 1; i < frames.size(); i++) {
            RawFrame current = frames.get(i);
            if (!Arrays.equals(current.data(), lastUnique.data())) {
                deduplicated.add(current);
                lastUnique = current;
            }
        }
        return deduplicated;
    }

    /**
     * Generates the complete Asciinema cast file as a string.
     *
     * @return the cast file content
     * @throws RuntimeIOException if an I/O error occurs
     */
    String toCast() {
        if (frames.isEmpty()) {
            return "";
        }

        StringWriter writer = new StringWriter();
        try {
            AsciinemaWriter.writeHeader(writer, width, height);

            long firstTimestamp = frames.get(0).timestampMs();

            for (int i = 0; i < frames.size(); i++) {
                TimedFrame frame = frames.get(i);
                double timeSeconds = (frame.timestampMs() - firstTimestamp) / 1000.0;

                // Build the frame output: clear screen + cursor home + buffer content
                String output = buildFrameOutput(frame.buffer(), i == 0);
                AsciinemaWriter.writeOutputEvent(writer, timeSeconds, output);
            }

            // Add a final empty event to preserve total duration
            // This ensures static demos (with only 1 unique frame) display for the full recording time
            double finalTimeSeconds = totalDurationMs / 1000.0;
            double lastFrameTime = (frames.get(frames.size() - 1).timestampMs() - firstTimestamp) / 1000.0;
            if (finalTimeSeconds > lastFrameTime) {
                AsciinemaWriter.writeOutputEvent(writer, finalTimeSeconds, "");
            }
        } catch (IOException e) {
            // StringWriter doesn't throw IOException
            throw new RuntimeIOException("Unexpected IOException when writing Asciinema animation", e);
        }

        return writer.toString();
    }

    /**
     * Builds the output string for a single frame using explicit cursor positioning.
     * This ensures correct rendering in asciinema player by positioning each row explicitly.
     *
     * @param buffer the buffer to render
     * @param isFirst whether this is the first frame
     * @return the ANSI output string
     */
    private String buildFrameOutput(Buffer buffer, boolean isFirst) {
        StringBuilder sb = new StringBuilder();

        // Clear screen on first frame
        if (isFirst) {
            sb.append(CLEAR_SCREEN);
        }

        // Render each row with explicit cursor positioning
        // This avoids issues with newlines in asciinema player
        sb.append(buffer.toAnsiStringWithCursorPositioning());

        return sb.toString();
    }
}
