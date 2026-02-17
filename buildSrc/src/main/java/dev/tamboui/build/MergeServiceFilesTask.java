/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.build;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Task to merge META-INF/services/* files from all dependencies.
 * This is required for SPI (Service Provider Interface) support in fat jars.
 */
@CacheableTask
public abstract class MergeServiceFilesTask extends DefaultTask {

    private static final String SERVICES_PREFIX = "META-INF/services/";

    @Classpath
    public abstract ConfigurableFileCollection getClasspath();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDirectory();

    public MergeServiceFilesTask() {
        setGroup("build");
        setDescription("Merge META-INF/services files from all dependencies");
    }

    @TaskAction
    public void merge() throws IOException {
        // Collect all service entries: service name -> set of implementations
        Map<String, Set<String>> services = new LinkedHashMap<>();

        for (File file : getClasspath().getFiles()) {
            if (file.isFile() && file.getName().endsWith(".jar")) {
                collectServicesFromJar(file, services);
            } else if (file.isDirectory()) {
                collectServicesFromDirectory(file, services);
            }
        }

        // Write merged service files
        Path outputDir = getOutputDirectory().get().getAsFile().toPath();
        Path servicesDir = outputDir.resolve("META-INF/services");
        Files.createDirectories(servicesDir);

        for (Map.Entry<String, Set<String>> entry : services.entrySet()) {
            String serviceName = entry.getKey();
            Set<String> implementations = entry.getValue();

            Path serviceFile = servicesDir.resolve(serviceName);
            Files.createDirectories(serviceFile.getParent());
            StringBuilder content = new StringBuilder();
            for (String impl : implementations) {
                content.append(impl).append("\n");
            }
            Files.writeString(serviceFile, content.toString(), StandardCharsets.UTF_8);
        }
    }

    private void collectServicesFromJar(File jarFile, Map<String, Set<String>> services) throws IOException {
        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.startsWith(SERVICES_PREFIX) && !entry.isDirectory()) {
                    String serviceName = name.substring(SERVICES_PREFIX.length());
                    if (!serviceName.isEmpty()) {
                        Set<String> implementations = services.computeIfAbsent(serviceName, k -> new LinkedHashSet<>());
                        try (BufferedReader reader = new BufferedReader(
                                new InputStreamReader(jar.getInputStream(entry), StandardCharsets.UTF_8))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                line = line.trim();
                                // Skip comments and empty lines
                                int commentIdx = line.indexOf('#');
                                if (commentIdx >= 0) {
                                    line = line.substring(0, commentIdx).trim();
                                }
                                if (!line.isEmpty()) {
                                    implementations.add(line);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void collectServicesFromDirectory(File dir, Map<String, Set<String>> services) throws IOException {
        Path servicesPath = dir.toPath().resolve("META-INF/services");
        if (Files.isDirectory(servicesPath)) {
            try (var stream = Files.walk(servicesPath)) {
                stream.filter(Files::isRegularFile).forEach(serviceFile -> {
                    String serviceName = servicesPath.relativize(serviceFile).toString();
                    Set<String> implementations = services.computeIfAbsent(serviceName, k -> new LinkedHashSet<>());
                    try {
                        for (String line : Files.readAllLines(serviceFile, StandardCharsets.UTF_8)) {
                            line = line.trim();
                            int commentIdx = line.indexOf('#');
                            if (commentIdx >= 0) {
                                line = line.substring(0, commentIdx).trim();
                            }
                            if (!line.isEmpty()) {
                                implementations.add(line);
                            }
                        }
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to read service file: " + serviceFile, e);
                    }
                });
            }
        }
    }
}
