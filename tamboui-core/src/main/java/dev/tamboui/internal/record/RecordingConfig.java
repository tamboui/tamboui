/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.internal.record;

import java.nio.file.Path;
import java.nio.file.Paths;

/// Configuration for Asciinema (.cast) recording, loaded from system properties.
/// This is an internal API and not part of the public contract.
public final class RecordingConfig {

    private static final String PREFIX = "tamboui.record";

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

    /// Loads recording configuration from system properties.
    ///
    /// @return configuration if recording is enabled, null otherwise
    public static RecordingConfig load() {
        String output = System.getProperty(PREFIX);
        if (output == null) {
            return null; // Recording disabled
        }

        String configPath = System.getProperty(PREFIX + ".config");

        return new RecordingConfig(
                Paths.get(output),
                Integer.getInteger(PREFIX + ".fps", 10),
                Integer.getInteger(PREFIX + ".duration", 10000),
                Integer.getInteger(PREFIX + ".width", 80),
                Integer.getInteger(PREFIX + ".height", 24),
                configPath != null ? Paths.get(configPath) : null
        );
    }

    /// Creates a config with explicit values (used by TuiRunner for config file support).
    public static RecordingConfig of(Path outputPath, int fps, int maxDurationMs, int width, int height) {
        return new RecordingConfig(outputPath, fps, maxDurationMs, width, height, null);
    }

    public Path outputPath() {
        return outputPath;
    }

    public int fps() {
        return fps;
    }

    public int maxDurationMs() {
        return maxDurationMs;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public Path configFile() {
        return configFile;
    }
}

