/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.build;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Gradle task that updates the jbang-catalog.json file by scanning
 * demo projects and discovering their main classes.
 */
public abstract class UpdateJBangCatalogTask extends DefaultTask {

    private static final String DEMO_SELECTOR = "demo-selector";

    /**
     * Returns the root project directory.
     *
     * @return the root project directory property
     */
    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract DirectoryProperty getProjectDir();

    /**
     * Returns the list of module names that can contain demos.
     *
     * @return the modules property
     */
    @Input
    public abstract ListProperty<String> getModules();

    /**
     * Returns the output jbang-catalog.json file.
     *
     * @return the output file property
     */
    @OutputFile
    public abstract RegularFileProperty getCatalogFile();

    /**
     * Executes the task to update the jbang catalog.
     */
    @TaskAction
    public void updateCatalog() {
        Path projectRoot = getProjectDir().get().getAsFile().toPath();
        Map<String, String> aliases = new LinkedHashMap<>();

        // Add run-demo alias (static entry)
        aliases.put("run-demo", ".jbang/tambouiDemos.java");

        // Scan root demos/ directory
        collectDemos(projectRoot.resolve("demos"), aliases);

        // Scan each module's demos/ directory
        for (String module : getModules().get()) {
            collectDemos(projectRoot.resolve(module).resolve("demos"), aliases);
        }

        // Write the catalog file
        writeCatalog(aliases);
    }

    /**
     * Collects demos from a directory and adds them to the aliases map.
     *
     * @param demosDir the directory to scan for demos
     * @param aliases  the map to add discovered aliases to
     */
    private void collectDemos(Path demosDir, Map<String, String> aliases) {
        if (!Files.isDirectory(demosDir)) {
            return;
        }

        try (Stream<Path> paths = Files.list(demosDir)) {
            paths.filter(Files::isDirectory)
                    .filter(p -> !DEMO_SELECTOR.equals(p.getFileName().toString()))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .forEach(demoDir -> {
                        String demoName = demoDir.getFileName().toString();
                        String mainClassPath = findMainClass(demoDir);
                        if (mainClassPath != null) {
                            Path projectRoot = getProjectDir().get().getAsFile().toPath();
                            String relativePath = projectRoot.relativize(demoDir.resolve(mainClassPath)).toString();
                            aliases.put(demoName, relativePath);
                        }
                    });
        } catch (IOException e) {
            getLogger().warn("Failed to scan demos directory: {}", demosDir, e);
        }
    }

    /**
     * Finds the main class in a demo project by scanning the src/main/java directory.
     *
     * @param demoDir the demo project directory
     * @return the relative path to the main class file, or null if not found
     */
    private String findMainClass(Path demoDir) {
        Path srcMain = demoDir.resolve("src/main/java");
        if (!Files.isDirectory(srcMain)) {
            return null;
        }

        try (Stream<Path> files = Files.walk(srcMain)) {
            return files.filter(p -> p.toString().endsWith(".java"))
                    .filter(this::containsMainMethod)
                    .findFirst()
                    .map(p -> srcMain.getParent().getParent().getParent().relativize(p).toString())
                    .orElse(null);
        } catch (IOException e) {
            getLogger().warn("Failed to scan source directory: {}", srcMain, e);
            return null;
        }
    }

    /**
     * Checks if a Java file contains a main method.
     *
     * @param javaFile the Java file to check
     * @return true if the file contains a main method
     */
    private boolean containsMainMethod(Path javaFile) {
        try {
            String content = Files.readString(javaFile);
            return content.contains("public static void main(");
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Writes the jbang catalog JSON file.
     *
     * @param aliases the map of alias names to script references
     */
    private void writeCatalog(Map<String, String> aliases) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"aliases\": {\n");

        boolean first = true;
        for (Map.Entry<String, String> entry : aliases.entrySet()) {
            if (!first) {
                json.append(",\n");
            }
            first = false;

            String name = entry.getKey();
            String scriptRef = entry.getValue();

            if ("run-demo".equals(name)) {
                // Special case for run-demo with description
                json.append("    \"").append(name).append("\": {\n");
                json.append("      \"script-ref\": \"").append(scriptRef).append("\",\n");
                json.append("      \"description\": \"Run TamboUI demos interactively\"\n");
                json.append("    }");
            } else {
                json.append("    \"").append(name).append("\": {\n");
                json.append("      \"script-ref\": \"").append(scriptRef).append("\"\n");
                json.append("    }");
            }
        }

        json.append("\n  }\n");
        json.append("}\n");

        File catalogFile = getCatalogFile().get().getAsFile();
        try {
            Files.writeString(catalogFile.toPath(), json.toString());
            getLogger().lifecycle("Updated {}", catalogFile);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write catalog file: " + catalogFile, e);
        }
    }
}
