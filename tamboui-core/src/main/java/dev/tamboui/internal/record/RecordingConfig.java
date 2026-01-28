/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.internal.record;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Configuration for Asciinema (.cast) recording, loaded from system properties.
 * This is an internal API and not part of the public contract.
 */
public final class RecordingConfig {

    private static final String PREFIX = "tamboui.record";
    private static RecordingConfig activeConfig = null;
    private static volatile boolean shutdownHookRegistered = false;

    private final Path outputPath;
    private final int fps;
    private final int maxDurationMs;
    private final int width;
    private final int height;
    private final Path configFile;

    RecordingConfig(Path outputPath, int fps, int maxDurationMs, int width, int height, Path configFile) {
        this.outputPath = outputPath;
        this.fps = fps;
        this.maxDurationMs = maxDurationMs;
        this.width = width;
        this.height = height;
        this.configFile = configFile;
    }

    /**
     * Loads recording configuration from system properties and installs capture.
     * This method installs AnsiTerminalCapture immediately so that all System.out
     * output is captured from the start, not just after the first Backend is created.
     *
     * @return configuration if recording is enabled, null otherwise
     */
    public static synchronized RecordingConfig load() {
        // Return cached config if already loaded
        if (activeConfig != null) {
            return activeConfig;
        }

        String output = System.getProperty(PREFIX);
        if (output == null) {
            return null; // Recording disabled
        }

        String configPath = System.getProperty(PREFIX + ".config");

        RecordingConfig config = new RecordingConfig(
                Paths.get(output),
                Integer.getInteger(PREFIX + ".fps", 10),
                Integer.getInteger(PREFIX + ".duration", 10000),
                Integer.getInteger(PREFIX + ".width", 80),
                Integer.getInteger(PREFIX + ".height", 24),
                configPath != null ? Paths.get(configPath) : null
        );

        // Install System.out capture immediately so all output is captured
        AnsiTerminalCapture.install(config);
        activeConfig = config;

        // Register shutdown hook to write cast file at process exit
        if (!shutdownHookRegistered) {
            shutdownHookRegistered = true;
            Runtime.getRuntime().addShutdownHook(new Thread(RecordingConfig::writeShutdownCast));
        }

        return config;
    }

    /**
     * Returns the active recording config, or null if not recording.
     *
     * @return the active config, or null
     */
    public static synchronized RecordingConfig active() {
        return activeConfig;
    }

    /**
     * Checks if recording is enabled via system properties.
     * This does NOT install the capture - use {@link #load()} for that.
     *
     * @return true if recording is configured
     */
    public static boolean isEnabled() {
        return System.getProperty(PREFIX) != null;
    }

    /**
     * Clears the active config (called when RecordingBackend writes from draw frames).
     */
    static synchronized void clearActive() {
        activeConfig = null;
    }

    /**
     * Shutdown hook to write captured frames to cast file.
     */
    private static void writeShutdownCast() {
        RecordingConfig config;
        int captureWidth;
        int captureHeight;
        synchronized (RecordingConfig.class) {
            config = activeConfig;
            if (config == null || !AnsiTerminalCapture.isInstalled()) {
                return;
            }
            // Capture dimensions before uninstalling
            captureWidth = AnsiTerminalCapture.getWidth();
            captureHeight = AnsiTerminalCapture.getHeight();
        }

        try {
            List<RawFrame> capturedFrames = AnsiTerminalCapture.uninstall();
            if (capturedFrames.isEmpty()) {
                return;
            }

            String cast = AsciinemaAnimation.fromRawFrames(capturedFrames, captureWidth, captureHeight);

            Path outputPath = config.outputPath();
            Path parent = outputPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            Files.write(outputPath, cast.getBytes(StandardCharsets.UTF_8));
            // Use original System.out since we just uninstalled the capture
            System.out.println("Recording saved to: " + outputPath);
            System.out.println("Frames captured: " + capturedFrames.size());
        } catch (IOException e) {
            System.err.println("Failed to write recording: " + e.getMessage());
        } finally {
            synchronized (RecordingConfig.class) {
                activeConfig = null;
            }
        }
    }

    /**
     * Creates a config with explicit values (used by TuiRunner for config file support).
     *
     * @param outputPath    the output file path
     * @param fps           the frames per second
     * @param maxDurationMs the maximum recording duration in milliseconds
     * @param width         the recording width in columns
     * @param height        the recording height in rows
     * @return a new recording config
     */
    public static RecordingConfig of(Path outputPath, int fps, int maxDurationMs, int width, int height) {
        return new RecordingConfig(outputPath, fps, maxDurationMs, width, height, null);
    }

    /**
     * Returns the output file path.
     *
     * @return the output path
     */
    public Path outputPath() {
        return outputPath;
    }

    /**
     * Returns the frames per second.
     *
     * @return the FPS
     */
    public int fps() {
        return fps;
    }

    /**
     * Returns the maximum recording duration in milliseconds.
     *
     * @return the maximum duration
     */
    public int maxDurationMs() {
        return maxDurationMs;
    }

    /**
     * Returns the recording width in columns.
     *
     * @return the width
     */
    public int width() {
        return width;
    }

    /**
     * Returns the recording height in rows.
     *
     * @return the height
     */
    public int height() {
        return height;
    }

    /**
     * Returns the config file path, or null if loaded from system properties.
     *
     * @return the config file path, or null
     */
    public Path configFile() {
        return configFile;
    }
}
