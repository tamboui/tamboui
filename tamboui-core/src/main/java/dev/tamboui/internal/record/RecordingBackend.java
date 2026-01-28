/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.internal.record;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.CellUpdate;
import dev.tamboui.layout.Position;
import dev.tamboui.layout.Rect;
import dev.tamboui.layout.Size;
import dev.tamboui.terminal.Backend;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A backend wrapper that records frames to an Asciinema cast file.
 * This backend is headless - it does not output to the real terminal.
 * This is an internal API and not part of the public contract.
 */
public final class RecordingBackend implements Backend {

    private final Backend delegate;
    private final RecordingConfig config;
    private final Size overrideSize;
    private final Buffer buffer;
    private final List<TimedFrame> frames;
    private final InteractionPlayer interactionPlayer;
    private final long startTimeNanos;
    private long lastCaptureTimeNanos;
    private volatile boolean recording;
    private volatile boolean closed;
    private volatile boolean hasDrawn;  // Track if draw() was ever called

    /**
     * Creates a new recording backend wrapping the given delegate.
     *
     * @param delegate the delegate backend
     * @param config   the recording configuration
     */
    public RecordingBackend(Backend delegate, RecordingConfig config) {
        this.delegate = delegate;
        this.config = config;
        this.overrideSize = new Size(config.width(), config.height());
        this.buffer = Buffer.empty(new Rect(0, 0, config.width(), config.height()));
        this.frames = new ArrayList<>();
        this.interactionPlayer = new InteractionPlayer(
                InteractionPlayer.loadFromFile(config.configFile(), config.outputPath()), buffer);
        this.startTimeNanos = System.nanoTime();
        this.lastCaptureTimeNanos = 0;
        this.recording = true;
        this.closed = false;
        // Note: AnsiTerminalCapture is installed by RecordingConfig.load()
        // which is called before this constructor
    }

    @Override
    public Size size() throws IOException {
        return overrideSize;
    }

    @Override
    public void draw(Iterable<CellUpdate> updates) throws IOException {
        // Apply updates to our buffer
        for (CellUpdate update : updates) {
            buffer.set(update.x(), update.y(), update.cell());
        }
        hasDrawn = true;

        // Capture frame if recording
        if (recording) {
            captureFrame();
        }
        // Don't delegate - headless recording
    }

    private void captureFrame() {
        long nowNanos = System.nanoTime();
        long elapsedNanos = nowNanos - startTimeNanos;
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(elapsedNanos);

        // Check duration limit
        if (elapsedMs > config.maxDurationMs()) {
            recording = false;
            return;
        }

        // Throttle based on FPS
        long frameIntervalNanos = TimeUnit.MILLISECONDS.toNanos(1000 / config.fps());
        if (nowNanos - lastCaptureTimeNanos >= frameIntervalNanos) {
            frames.add(new TimedFrame(buffer.copy(), elapsedMs));
            lastCaptureTimeNanos = nowNanos;
        }
    }

    /**
     * Returns true if recording is still active.
     *
     * @return true if recording
     */
    public boolean isRecording() {
        if (!recording) {
            return false;
        }
        // Check duration limit
        if (TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTimeNanos) > config.maxDurationMs()) {
            recording = false;
            return false;
        }
        return true;
    }

    @Override
    public void flush() throws IOException {
        // Pass through to System.out for AnsiTerminalCapture to capture frames
        System.out.flush();
    }

    @Override
    public void clear() throws IOException {
        buffer.clear();
    }

    @Override
    public void showCursor() throws IOException {
        // Headless - no output
    }

    @Override
    public void hideCursor() throws IOException {
        // Headless - no output
    }

    @Override
    public Position getCursorPosition() throws IOException {
        return new Position(0, 0);
    }

    @Override
    public void setCursorPosition(Position position) throws IOException {
        // Headless - no output
    }

    @Override
    public void enterAlternateScreen() throws IOException {
        // Headless - no output
    }

    @Override
    public void leaveAlternateScreen() throws IOException {
        // Headless - no output
    }

    @Override
    public void enableRawMode() throws IOException {
        // Headless - no output
    }

    @Override
    public void disableRawMode() throws IOException {
        // Headless - no output
    }

    @Override
    public void enableMouseCapture() throws IOException {
        // Headless - no output
    }

    @Override
    public void disableMouseCapture() throws IOException {
        // Headless - no output
    }

    @Override
    public void scrollUp(int lines) throws IOException {
        // Headless - no output
    }

    @Override
    public void scrollDown(int lines) throws IOException {
        // Headless - no output
    }

    @Override
    public void writeRaw(byte[] data) throws IOException {
        // Pass through to System.out for AnsiTerminalCapture to process
        // This allows InlineDisplay output to be captured
        System.out.write(data);
        System.out.flush();
    }

    @Override
    public void writeRaw(String data) throws IOException {
        // Pass through to System.out for AnsiTerminalCapture to process
        // This allows InlineDisplay output to be captured
        System.out.print(data);
        System.out.flush();
    }

    @Override
    public void onResize(Runnable handler) {
        // No resize events in recording mode
    }

    @Override
    public int read(int timeoutMs) throws IOException {
        // If we have interactions, play them back
        if (!interactionPlayer.isFinished()) {
            int b = interactionPlayer.nextByte(Math.min(timeoutMs, 100));
            // Capture frame during read to ensure we get frames during Sleep commands
            // Only capture if draw() has been called (TUI demos), not for inline demos
            // which use System.out and AnsiTerminalCapture instead
            if (recording && hasDrawn) {
                captureFrame();
            }
            return b;
        }

        // Interactions finished - if we HAD interactions, exit now
        // (the recording file should end with a quit key)
        if (!interactionPlayer.hasNoInteractions() && !closed) {
            recording = false;
            closed = true;
            try {
                writeCastFromDrawFrames();
            } catch (IOException e) {
                // Ignore write errors
            }
            System.exit(0);
        }

        // No interactions defined - wait for duration limit
        if (!isRecording() && !closed) {
            closed = true;
            try {
                writeCastFromDrawFrames();
            } catch (IOException e) {
                // Ignore write errors
            }
            System.exit(0);
        }

        // Wait a bit
        try {
            Thread.sleep(Math.min(timeoutMs, 100));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return -2;
    }

    @Override
    public int peek(int timeoutMs) throws IOException {
        // Peek at pending bytes from interaction player
        return interactionPlayer.peekByte();
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        recording = false;

        // Write cast file only if we have frames from Backend.draw() calls (TUI demos)
        // System.out capture is handled by the shutdown hook
        if (!frames.isEmpty()) {
            writeCastFromDrawFrames();
        }

        // Close delegate
        delegate.close();
    }

    private void writeCastFromDrawFrames() throws IOException {
        // Write cast file from Backend.draw() captured frames (TUI demos)
        // For inline demos (no draw() calls), frames will be empty and we let
        // the shutdown hook write the System.out captured frames instead
        if (frames.isEmpty()) {
            return;  // Let shutdown hook handle AnsiTerminalCapture frames
        }

        // We have draw() frames - uninstall System.out capture and write draw frames
        AnsiTerminalCapture.uninstall();
        RecordingConfig.clearActive();  // Prevent shutdown hook from also writing

        AsciinemaAnimation animation = new AsciinemaAnimation(frames, config.fps());
        String cast = animation.toCast();

        Path outputPath = config.outputPath();
        Path parent = outputPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        Files.write(outputPath, cast.getBytes(StandardCharsets.UTF_8));
        System.out.println("Recording saved to: " + outputPath);
        System.out.println("Frames captured: " + frames.size());
    }
}
