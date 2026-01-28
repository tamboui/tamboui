/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.internal.record;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Captures raw bytes written to System.out for recording inline demos.
 * Instead of parsing ANSI sequences into a virtual buffer, this captures
 * the raw byte stream and replays it directly in the cast file.
 * <p>
 * This approach provides perfect fidelity - the cast file contains exactly
 * what was written to stdout, and the asciinema player interprets the
 * ANSI sequences correctly.
 * <p>
 * This is an internal API and not part of the public contract.
 */
public final class AnsiTerminalCapture extends OutputStream {

    private static AnsiTerminalCapture instance;
    private static PrintStream originalOut;

    private final PrintStream tee;
    private final List<RawFrame> frames;
    private final ByteArrayOutputStream currentFrame;
    private final int fps;
    private final long maxDurationMs;
    private final long startTime;
    private final int width;
    private final int height;

    // Frame capture timing
    private long lastCaptureTime;

    private AnsiTerminalCapture(PrintStream tee, RecordingConfig config) {
        this.tee = tee;
        this.frames = new ArrayList<>();
        this.currentFrame = new ByteArrayOutputStream();
        this.fps = config.fps();
        this.maxDurationMs = config.maxDurationMs();
        this.width = config.width();
        this.height = config.height();
        this.startTime = System.currentTimeMillis();
        this.lastCaptureTime = this.startTime;
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
     *
     * @return the captured raw frames, or empty list if not installed
     */
    public static synchronized List<RawFrame> uninstall() {
        if (instance == null) {
            return new ArrayList<>();
        }

        // Capture final frame
        instance.captureFrame(true);

        List<RawFrame> capturedFrames = new ArrayList<>(instance.frames);
        System.setOut(originalOut);
        instance = null;
        originalOut = null;

        return capturedFrames;
    }

    /**
     * Returns true if capture is currently installed.
     *
     * @return true if capture is installed
     */
    public static synchronized boolean isInstalled() {
        return instance != null;
    }

    /**
     * Returns the configured width, or 80 if not installed.
     *
     * @return the terminal width in columns
     */
    public static synchronized int getWidth() {
        return instance != null ? instance.width : 80;
    }

    /**
     * Returns the configured height, or 24 if not installed.
     *
     * @return the terminal height in rows
     */
    public static synchronized int getHeight() {
        return instance != null ? instance.height : 24;
    }

    @Override
    public void write(int b) throws IOException {
        // Pass through to original output
        tee.write(b);
        // Force flush on newlines to ensure timely output when piped
        if (b == '\n') {
            tee.flush();
        }

        // Accumulate byte for the current frame
        currentFrame.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        // Pass through to original output
        tee.write(b, off, len);
        // Force flush to ensure output appears when piped
        tee.flush();

        // Accumulate bytes for the current frame
        currentFrame.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        tee.flush();
        captureFrame(false);
    }

    @Override
    public void close() throws IOException {
        tee.flush();
    }

    private void captureFrame(boolean force) {
        long now = System.currentTimeMillis();
        long elapsed = now - startTime;

        // Check duration limit
        if (elapsed > maxDurationMs) {
            return;
        }

        // Throttle based on FPS (unless forcing final frame)
        long frameIntervalMs = 1000 / fps;
        if (!force && now - lastCaptureTime < frameIntervalMs) {
            return;
        }

        // Only capture if we have data
        if (currentFrame.size() > 0) {
            byte[] data = currentFrame.toByteArray();
            currentFrame.reset();
            frames.add(new RawFrame(data, elapsed));
            lastCaptureTime = now;
        }
    }
}
