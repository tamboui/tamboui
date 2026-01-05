package dev.tamboui.build;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.FileTree;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * A task that checks for split packages across source sets.
 * <p>
 * Split packages occur when the same Java package exists in multiple modules,
 * which causes problems with the Java Platform Module System. This task
 * analyzes source directories and reports any packages that appear in more than one module.
 */
@CacheableTask
public abstract class SplitPackageCheckTask extends DefaultTask {

    /**
     * Represents a source set entry with its name and source files.
     */
    public interface SourceSetEntry {
        /**
         * Returns the name of the source set (typically the module/project name).
         *
         * @return the source set name
         */
        @Input
        String getName();

        /**
         * Returns the Java source files in this source set.
         *
         * @return the file tree of Java sources
         */
        @InputFiles
        @PathSensitive(PathSensitivity.RELATIVE)
        FileTree getSources();
    }

    /**
     * The source set entries to analyze for split packages.
     *
     * @return the list of source set entries
     */
    @Nested
    public abstract ListProperty<SourceSetEntry> getSourceSets();

    /**
     * The output report file containing the split package analysis results.
     *
     * @return the regular file property for the report
     */
    @OutputFile
    public abstract RegularFileProperty getReportFile();

    /**
     * Adds a source set entry to be analyzed.
     *
     * @param name    the name of the source set (module/project name)
     * @param sources the file tree of Java sources
     */
    public void sourceSet(String name, FileTree sources) {
        getSourceSets().add(new SourceSetEntryImpl(name, sources));
    }

    /**
     * Executes the split package check.
     * <p>
     * Analyzes all source sets, identifies packages in each, and reports
     * any packages that exist in multiple modules. The task fails if split packages
     * are detected.
     */
    @TaskAction
    public void checkSplitPackages() {
        Map<String, Set<String>> packageToModules = new HashMap<>();

        for (SourceSetEntry entry : getSourceSets().get()) {
            String moduleName = entry.getName();
            Set<String> packages = extractPackagesFromSources(entry.getSources());
            for (String pkg : packages) {
                packageToModules.computeIfAbsent(pkg, k -> new HashSet<>()).add(moduleName);
            }
        }

        // Find split packages (packages in more than one module)
        Map<String, Set<String>> splitPackages = new TreeMap<>();
        for (Map.Entry<String, Set<String>> entry : packageToModules.entrySet()) {
            if (entry.getValue().size() > 1) {
                splitPackages.put(entry.getKey(), new TreeSet<>(entry.getValue()));
            }
        }

        // Write report
        writeReport(splitPackages);

        // Fail if split packages found
        if (!splitPackages.isEmpty()) {
            StringBuilder message = new StringBuilder();
            message.append("Split packages detected:\n");
            for (Map.Entry<String, Set<String>> entry : splitPackages.entrySet()) {
                message.append("  Package '")
                        .append(entry.getKey())
                        .append("' found in: ")
                        .append(String.join(", ", entry.getValue()))
                        .append("\n");
            }
            message.append("\nSplit packages are not allowed as they cause issues with the Java Module System.");
            throw new GradleException(message.toString());
        }
    }

    private Set<String> extractPackagesFromSources(FileTree sources) {
        Set<String> packages = new HashSet<>();
        for (File file : sources) {
            if (file.getName().endsWith(".java") && !file.getName().equals("module-info.java")) {
                String pkg = extractPackageFromFile(file);
                if (pkg != null && !pkg.isEmpty()) {
                    packages.add(pkg);
                }
            }
        }
        return packages;
    }

    private String extractPackageFromFile(File javaFile) {
        try (Stream<String> lines = Files.lines(javaFile.toPath())) {
            return lines
                    .map(String::trim)
                    .filter(line -> line.startsWith("package "))
                    .findFirst()
                    .map(line -> {
                        // Extract package name: "package com.example;" -> "com.example"
                        String pkg = line.substring("package ".length());
                        int semicolon = pkg.indexOf(';');
                        if (semicolon > 0) {
                            pkg = pkg.substring(0, semicolon);
                        }
                        return pkg.trim();
                    })
                    .orElse(null);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read Java file: " + javaFile, e);
        }
    }

    private void writeReport(Map<String, Set<String>> splitPackages) {
        File reportFile = getReportFile().get().getAsFile();
        try {
            Files.createDirectories(reportFile.getParentFile().toPath());
            StringBuilder report = new StringBuilder();
            report.append("Split Package Check Report\n");
            report.append("==========================\n\n");

            if (splitPackages.isEmpty()) {
                report.append("No split packages detected.\n");
            } else {
                report.append("Split packages found:\n\n");
                for (Map.Entry<String, Set<String>> entry : splitPackages.entrySet()) {
                    report.append("Package: ").append(entry.getKey()).append("\n");
                    report.append("  Found in:\n");
                    for (String module : entry.getValue()) {
                        report.append("    - ").append(module).append("\n");
                    }
                    report.append("\n");
                }
            }

            Files.writeString(reportFile.toPath(), report.toString());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write report file: " + reportFile, e);
        }
    }

    /**
     * Implementation of SourceSetEntry.
     */
    private static final class SourceSetEntryImpl implements SourceSetEntry {
        private final String name;
        private final FileTree sources;

        SourceSetEntryImpl(String name, FileTree sources) {
            this.name = name;
            this.sources = sources;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public FileTree getSources() {
            return sources;
        }
    }
}
