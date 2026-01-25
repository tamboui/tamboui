/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.build;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Task to generate a JSON metadata file for a demo project.
 * This metadata is consumed by the documentation build to generate the demos gallery.
 */
@CacheableTask
public abstract class GenerateDemoMetadataTask extends DefaultTask {

    @Input
    public abstract Property<String> getDemoId();

    @Input
    public abstract Property<String> getDisplayName();

    @Input
    public abstract Property<String> getDemoDescription();

    @Input
    public abstract Property<String> getModule();

    @Input
    public abstract SetProperty<String> getTags();

    @Input
    public abstract Property<String> getCastFileName();

    @Input
    public abstract Property<Boolean> getInternal();

    @OutputFile
    public abstract RegularFileProperty getOutputFile();

    public GenerateDemoMetadataTask() {
        setGroup("documentation");
        setDescription("Generate metadata JSON file for this demo");

        // Defaults
        getDemoDescription().convention("");
        getModule().convention("Other");
        getTags().convention(Set.of());
        getInternal().convention(false);
    }

    @TaskAction
    public void generate() throws IOException {
        String json = toJson(
                getDemoId().get(),
                getDisplayName().get(),
                getDemoDescription().get(),
                getModule().get(),
                getTags().get(),
                getCastFileName().get(),
                getInternal().get()
        );

        Files.createDirectories(getOutputFile().get().getAsFile().toPath().getParent());
        Files.writeString(getOutputFile().get().getAsFile().toPath(), json);
    }

    private static String toJson(String id, String displayName, String description,
                                  String module, Set<String> tags, String castFileName,
                                  boolean internal) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"id\": ").append(jsonString(id)).append(",\n");
        sb.append("  \"displayName\": ").append(jsonString(displayName)).append(",\n");
        sb.append("  \"description\": ").append(jsonString(description)).append(",\n");
        sb.append("  \"module\": ").append(jsonString(module)).append(",\n");
        sb.append("  \"tags\": [");
        if (!tags.isEmpty()) {
            sb.append(tags.stream()
                    .sorted()
                    .map(GenerateDemoMetadataTask::jsonString)
                    .collect(Collectors.joining(", ")));
        }
        sb.append("],\n");
        sb.append("  \"castFileName\": ").append(jsonString(castFileName)).append(",\n");
        sb.append("  \"internal\": ").append(internal).append("\n");
        sb.append("}");
        return sb.toString();
    }

    private static String jsonString(String value) {
        if (value == null) {
            return "null";
        }
        return "\"" + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                + "\"";
    }
}
