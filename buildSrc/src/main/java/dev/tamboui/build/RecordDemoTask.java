/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.build;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.process.ExecOperations;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Locale;

/**
 * Task to record a demo to an Asciinema cast file.
 */
@CacheableTask
public abstract class RecordDemoTask extends DefaultTask {

    @Input
    public abstract Property<String> getMainClass();

    @InputFiles
    @Classpath
    public abstract ConfigurableFileCollection getClasspath();

    @Input
    public abstract Property<Integer> getFps();

    @Input
    public abstract Property<Integer> getDuration();

    @Input
    public abstract Property<Integer> getWidth();

    @Input
    public abstract Property<Integer> getHeight();

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getConfigFile();

    @Internal
    public abstract DirectoryProperty getOutputDirectory();

    @OutputFile
    public Provider<RegularFile> getOutputCastFile() {
        return getOutputDirectory().map(dir -> dir.file(getProject().getName() + ".cast"));
    }

    @OutputDirectory
    public Provider<Directory> getScreenshotsDirectory() {
        return getOutputDirectory().map(dir -> dir.dir("screenshots"));
    }

    @Inject
    protected abstract ExecOperations getExecOperations();

    public RecordDemoTask() {
        setGroup("documentation");
        setDescription("Record the demo to an Asciinema cast file");

        // Defaults
        getFps().convention(10);
        getDuration().convention(10000);
        getWidth().convention(80);
        getHeight().convention(24);
    }

    /**
     * Calculate total wait time from a VHS tape file.
     * Parses Sleep commands in VHS format: Sleep 1s, Sleep 500ms, Sleep 0.5
     */
    private long calculateTotalWaitTime(File file) {
        if (!file.exists()) {
            return 0L;
        }
        var totalMs = 0L;
        try {
            for (var line : Files.readAllLines(file.toPath())) {
                var trimmed = line.trim().toLowerCase(Locale.ROOT);
                if (trimmed.startsWith("sleep ")) {
                    var duration = trimmed.substring(6).trim();
                    totalMs += parseDuration(duration);
                }
            }
        } catch (IOException e) {
            getLogger().warn("Failed to read config file: " + e.getMessage());
        }
        return totalMs;
    }

    /**
     * Parse VHS duration format: 1s, 500ms, 0.5 (seconds), or bare integer (seconds)
     */
    private long parseDuration(String duration) {
        try {
            if (duration.endsWith("ms")) {
                return Long.parseLong(duration.substring(0, duration.length() - 2));
            } else if (duration.endsWith("s")) {
                return (long) (Double.parseDouble(duration.substring(0, duration.length() - 1)) * 1000);
            } else if (duration.contains(".")) {
                return (long) (Double.parseDouble(duration) * 1000);
            } else {
                return Long.parseLong(duration) * 1000; // Bare integer = seconds in VHS
            }
        } catch (NumberFormatException e) {
            return 1000L; // Default 1 second
        }
    }

    @TaskAction
    public void record() {
        var outDir = getOutputDirectory().get().getAsFile();
        outDir.mkdirs();

        var outFile = getOutputCastFile().get().getAsFile();

        // Calculate appropriate duration based on config file wait times
        var configFileObj = getConfigFile().isPresent() ? getConfigFile().get().getAsFile() : null;
        var totalWaitMs = 0L;
        if (configFileObj != null && configFileObj.exists()) {
            totalWaitMs = calculateTotalWaitTime(configFileObj);
        }

        // Duration should be at least the total wait time plus buffer for processing
        var effectiveDuration = Math.max(getDuration().get().longValue(), totalWaitMs + 2000L);

        var result = getExecOperations().javaexec(spec -> {
            spec.getMainClass().set(getMainClass().get());
            spec.classpath(getClasspath());
            spec.jvmArgs("--enable-native-access=ALL-UNNAMED");

            // Recording settings - output is always controlled by Gradle
            spec.systemProperty("tamboui.record", outFile.getAbsolutePath());
            spec.systemProperty("tamboui.record.fps", String.valueOf(getFps().get()));
            spec.systemProperty("tamboui.record.duration", String.valueOf(effectiveDuration));
            spec.systemProperty("tamboui.record.width", String.valueOf(getWidth().get()));
            spec.systemProperty("tamboui.record.height", String.valueOf(getHeight().get()));

            // Config file provides interactions (optional)
            if (configFileObj != null && configFileObj.exists()) {
                spec.systemProperty("tamboui.record.config", configFileObj.getAbsolutePath());
            }

            // Ensure stdout/stderr go to real streams, not Gradle's logging
            // This is needed for AnsiTerminalCapture to work properly
            spec.setStandardOutput(OutputStream.nullOutputStream());
            spec.setErrorOutput(OutputStream.nullOutputStream());

            // Don't fail if the process is killed due to timeout
            spec.setIgnoreExitValue(true);
        });

        // Check if the process timed out or exited normally
        if (result.getExitValue() != 0) {
            getLogger().warn("Demo process exited with code " + result.getExitValue());
        }
    }
}
