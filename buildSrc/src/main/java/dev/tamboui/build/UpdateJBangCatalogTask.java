/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.build;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;
import org.gradle.process.ExecOperations;
import org.gradle.process.ExecResult;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Gradle task that updates the jbang-catalog.json file by scanning
 * demo projects and discovering their main classes.
 */
public abstract class UpdateJBangCatalogTask extends DefaultTask {

    private static final String DEMO_SELECTOR = "demo-selector";

    /**
     * Returns the ExecOperations service for executing external processes.
     *
     * @return the ExecOperations service
     */
    @Inject
    protected abstract ExecOperations getExecOperations();

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
     * Returns whether to verify builds after writing the catalog.
     *
     * @return the verify builds property
     */
    @Option(option = "verify-builds", description = "Verify builds after writing the catalog")
    @Input
    @Optional
    public abstract Property<Boolean> getVerifyBuilds();

    /**
     * Fail-fast mode for build verification.
     */
    @Option(option = "verify-fail-fast", description = "Fail fast if any build verification fails (not effective if parallel is enabled)")
    @Input
    @Optional
    public abstract Property<Boolean> getFailFast();

     /**
     * Verbose mode for build verification.
     */
     @Option(option = "verify-verbose", description = "Verbose output for build verification")
     @Input
     @Optional
     public abstract Property<Boolean> getVerbose();

    /**
     * Parallel mode for build verification.
     */
    @Option(option = "verify-parallel", description = "Verify aliases in parallel")
    @Input
    @Optional
    public abstract Property<Boolean> getParallel();
    
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

        // Optionally verify builds
        if (getVerifyBuilds().getOrElse(false)) {
            verifyBuilds(aliases.keySet(), getFailFast().getOrElse(false), getParallel().getOrElse(false), getVerbose().getOrElse(false));
        }
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

    /**
     * Verifies builds for all aliases by running jbang build on each one.
     *
     * @param aliases the set of alias names to verify
     * @param failFast whether to fail fast if any build fails
     * @param parallel whether to build aliases in parallel (may cause JBang cache contention)
     */
    private void verifyBuilds(Set<String> aliases, boolean failFast, boolean parallel, boolean verbose) {
        getLogger().lifecycle("Verifying builds for {} aliases{}...", aliases.size(), parallel ? " (parallel)" : "", failFast ? " (fail fast)" : "");

        var stream = parallel ? aliases.parallelStream() : aliases.stream();
        var failedAliases = stream
                .map(alias -> {
                    getLogger().lifecycle("Building alias: {}", alias);
                    if (!buildAlias(alias, failFast, verbose)) {
                        return alias;
                    }
                    return null;
                }).filter(Objects::nonNull)
                .toList();

        if (!failedAliases.isEmpty()) {
            throw new GradleException(String.format("Build verification failed for %d alias(es): %s", failedAliases.size(), String.join(", ", failedAliases)));
        } else {
            getLogger().lifecycle("All {} aliases built successfully", aliases.size());
        }
    }

    /**
     * Builds a single alias using jbang build command.
     *
     * @param aliasName the alias name to build
     * @return true if the build succeeded (exit code 0), false otherwise
     */
    private boolean buildAlias(String aliasName, boolean failFast, boolean verbose) {
        try {
            ExecResult result = getExecOperations().exec(execSpec -> {
                execSpec.commandLine(
                        "jbang", "build",
                        "--verbose=" + verbose,
                        "-C=-Xdiags:compact",
                        "-C=-Xmaxerrs",
                        "-C=1",
                        aliasName
                );
                execSpec.setIgnoreExitValue(true);
                // Output is automatically captured and logged by Gradle
            });

            if (result.getExitValue() != 0) {
                getLogger().warn("Build failed for alias '{}' with exit code {}", aliasName, result.getExitValue());
                if (failFast) {
                    throw new GradleException(String.format("Build failed for alias '%s' with exit code %d", aliasName, result.getExitValue()));
                }
                return false;
            }
            return true;
        } catch (Exception e) {
            getLogger().error("Failed to execute jbang build for alias '{}': {}", aliasName, e.getMessage(), e);
            if (failFast) {
                throw new GradleException(String.format("Failed to execute jbang build for alias '%s': %s", aliasName, e.getMessage()), e);
            }
            return false;
        }
    }
}
