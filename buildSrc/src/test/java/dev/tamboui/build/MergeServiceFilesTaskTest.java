/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.build;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MergeServiceFilesTaskTest {

    @TempDir
    Path tempDir;

    private MergeServiceFilesTask task;
    private Path outputDir;

    @BeforeEach
    void setUp() {
        Project project = ProjectBuilder.builder().build();
        task = project.getTasks().create("mergeServices", MergeServiceFilesTask.class);
        outputDir = tempDir.resolve("output");
        task.getOutputDirectory().set(outputDir.toFile());
    }

    @Test
    void mergesFlatServiceFilesFromJar() throws IOException {
        Path jar = createJarWithServices(
                "test1.jar",
                "META-INF/services/com.example.MyService",
                "com.example.impl.MyServiceImpl\n"
        );
        task.getClasspath().from(jar.toFile());

        task.merge();

        Path serviceFile = outputDir.resolve("META-INF/services/com.example.MyService");
        assertTrue(Files.exists(serviceFile), "Flat service file should be created");
        assertEquals("com.example.impl.MyServiceImpl\n", Files.readString(serviceFile));
    }

    @Test
    void mergesNestedServiceFilesFromJar() throws IOException {
        Path jar = createJarWithServices(
                "jline.jar",
                "META-INF/services/org/jline/terminal/provider/exec",
                "class = org.jline.terminal.impl.exec.ExecTerminalProvider\n"
        );
        task.getClasspath().from(jar.toFile());

        task.merge();

        Path serviceFile = outputDir.resolve("META-INF/services/org/jline/terminal/provider/exec");
        assertTrue(Files.exists(serviceFile),
                "Nested service file should be created (jline3-style providers)");
        assertEquals("class = org.jline.terminal.impl.exec.ExecTerminalProvider\n",
                Files.readString(serviceFile));
    }

    @Test
    void mergesMultipleNestedProvidersFromJar() throws IOException {
        Path jar = createJarWithMultipleServices(
                "jline.jar",
                new String[]{
                        "META-INF/services/org/jline/terminal/provider/exec",
                        "class = org.jline.terminal.impl.exec.ExecTerminalProvider\n",
                        "META-INF/services/org/jline/terminal/provider/ffm",
                        "class = org.jline.terminal.impl.ffm.FfmTerminalProvider\n"
                }
        );
        task.getClasspath().from(jar.toFile());

        task.merge();

        Path execFile = outputDir.resolve("META-INF/services/org/jline/terminal/provider/exec");
        Path ffmFile = outputDir.resolve("META-INF/services/org/jline/terminal/provider/ffm");
        assertTrue(Files.exists(execFile), "exec provider file should exist");
        assertTrue(Files.exists(ffmFile), "ffm provider file should exist");
        assertEquals("class = org.jline.terminal.impl.exec.ExecTerminalProvider\n",
                Files.readString(execFile));
        assertEquals("class = org.jline.terminal.impl.ffm.FfmTerminalProvider\n",
                Files.readString(ffmFile));
    }

    @Test
    void mergesFlatServiceFilesFromDirectory() throws IOException {
        Path dir = createDirectoryWithServices(
                "META-INF/services/com.example.MyService",
                "com.example.impl.Impl1\n"
        );
        task.getClasspath().from(dir.toFile());

        task.merge();

        Path serviceFile = outputDir.resolve("META-INF/services/com.example.MyService");
        assertTrue(Files.exists(serviceFile));
        assertEquals("com.example.impl.Impl1\n", Files.readString(serviceFile));
    }

    @Test
    void mergesNestedServiceFilesFromDirectory() throws IOException {
        Path dir = createDirectoryWithServices(
                "META-INF/services/org/jline/terminal/provider/exec",
                "class = org.jline.terminal.impl.exec.ExecTerminalProvider\n"
        );
        task.getClasspath().from(dir.toFile());

        task.merge();

        Path serviceFile = outputDir.resolve("META-INF/services/org/jline/terminal/provider/exec");
        assertTrue(Files.exists(serviceFile),
                "Nested service file from directory should be created");
        assertEquals("class = org.jline.terminal.impl.exec.ExecTerminalProvider\n",
                Files.readString(serviceFile));
    }

    @Test
    void mergesDuplicateEntriesFromMultipleJars() throws IOException {
        Path jar1 = createJarWithServices(
                "lib1.jar",
                "META-INF/services/com.example.MyService",
                "com.example.impl.Impl1\ncom.example.impl.Impl2\n"
        );
        Path jar2 = createJarWithServices(
                "lib2.jar",
                "META-INF/services/com.example.MyService",
                "com.example.impl.Impl2\ncom.example.impl.Impl3\n"
        );
        task.getClasspath().from(jar1.toFile(), jar2.toFile());

        task.merge();

        Path serviceFile = outputDir.resolve("META-INF/services/com.example.MyService");
        String content = Files.readString(serviceFile);
        assertTrue(content.contains("com.example.impl.Impl1"));
        assertTrue(content.contains("com.example.impl.Impl2"));
        assertTrue(content.contains("com.example.impl.Impl3"));
        // Impl2 should appear only once (deduplication)
        assertEquals(3, content.lines().filter(l -> !l.isBlank()).count(),
                "Duplicate entries should be merged");
    }

    @Test
    void skipsCommentsAndEmptyLines() throws IOException {
        Path jar = createJarWithServices(
                "test.jar",
                "META-INF/services/com.example.MyService",
                "# This is a comment\n\ncom.example.impl.Impl1\ncom.example.impl.Impl2 # inline comment\n"
        );
        task.getClasspath().from(jar.toFile());

        task.merge();

        Path serviceFile = outputDir.resolve("META-INF/services/com.example.MyService");
        String content = Files.readString(serviceFile);
        assertEquals("com.example.impl.Impl1\ncom.example.impl.Impl2\n", content);
    }

    @Test
    void mixesFlatAndNestedServiceFiles() throws IOException {
        Path jar = createJarWithMultipleServices(
                "mixed.jar",
                new String[]{
                        "META-INF/services/com.example.MyService",
                        "com.example.impl.Impl1\n",
                        "META-INF/services/org/jline/terminal/provider/exec",
                        "class = org.jline.terminal.impl.exec.ExecTerminalProvider\n"
                }
        );
        task.getClasspath().from(jar.toFile());

        task.merge();

        Path flatFile = outputDir.resolve("META-INF/services/com.example.MyService");
        Path nestedFile = outputDir.resolve("META-INF/services/org/jline/terminal/provider/exec");
        assertTrue(Files.exists(flatFile), "Flat service file should exist");
        assertTrue(Files.exists(nestedFile), "Nested service file should exist");
    }

    private Path createJarWithServices(String jarName, String entryPath, String content) throws IOException {
        return createJarWithMultipleServices(jarName, new String[]{entryPath, content});
    }

    private Path createJarWithMultipleServices(String jarName, String[] pathsAndContents) throws IOException {
        Path jarPath = tempDir.resolve(jarName);
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jarPath))) {
            for (int i = 0; i < pathsAndContents.length; i += 2) {
                String entryPath = pathsAndContents[i];
                String content = pathsAndContents[i + 1];
                jos.putNextEntry(new JarEntry(entryPath));
                jos.write(content.getBytes(StandardCharsets.UTF_8));
                jos.closeEntry();
            }
        }
        return jarPath;
    }

    private Path createDirectoryWithServices(String relativePath, String content) throws IOException {
        Path dir = tempDir.resolve("classes");
        Path serviceFile = dir.resolve(relativePath);
        Files.createDirectories(serviceFile.getParent());
        Files.writeString(serviceFile, content, StandardCharsets.UTF_8);
        return dir;
    }
}
