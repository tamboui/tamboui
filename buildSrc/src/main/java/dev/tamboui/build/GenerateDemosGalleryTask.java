/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.build;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Task to generate AsciiDoc gallery pages from demo metadata files.
 * Generates an index page plus one page per module to keep each page lightweight.
 */
@CacheableTask
public abstract class GenerateDemosGalleryTask extends DefaultTask {

    private static final Pattern JSON_FIELD = Pattern.compile("\"(\\w+)\"\\s*:\\s*(?:\"([^\"]*)\"|\\[([^\\]]*)\\])");

    /**
     * Defines the display order of module sections.
     * Modules not in this list will appear at the end in alphabetical order.
     * "Core" is treated specially and displayed as "Other" at the end.
     */
    private static final List<String> MODULE_ORDER = List.of(
            "Widgets", "TUI", "Toolkit", "CSS", "Image"
    );

    /**
     * Module name that should be displayed last as "Other".
     */
    private static final String OTHER_MODULE = "Core";

    @InputFiles
    @PathSensitive(PathSensitivity.NAME_ONLY)
    public abstract ConfigurableFileCollection getMetadataFiles();

    @Input
    public abstract Property<String> getTitle();

    @Input
    public abstract Property<String> getCastBasePath();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDirectory();

    public GenerateDemosGalleryTask() {
        setGroup("documentation");
        setDescription("Generate the demos gallery AsciiDoc pages (index + per-module)");

        getTitle().convention("Demo Gallery");
        getCastBasePath().convention("demos");
    }

    @TaskAction
    public void generate() throws IOException {
        var demos = parseMetadataFiles();
        demos.sort(Comparator.comparing(DemoInfo::displayName));

        // Group by module with custom ordering
        var byModule = groupByModuleOrdered(demos);

        var outputDir = getOutputDirectory().get().getAsFile().toPath();
        Files.createDirectories(outputDir);

        // Generate index page
        var indexAdoc = generateIndexAdoc(byModule);
        Files.writeString(outputDir.resolve("demos.adoc"), indexAdoc);

        // Generate per-module pages
        for (var entry : byModule.entrySet()) {
            var module = entry.getKey();
            var moduleDemos = entry.getValue();
            var moduleAdoc = generateModuleAdoc(module, moduleDemos);
            var fileName = "demos-" + module.toLowerCase(Locale.ROOT) + ".adoc";
            Files.writeString(outputDir.resolve(fileName), moduleAdoc);
        }
    }

    private Map<String, List<DemoInfo>> groupByModuleOrdered(List<DemoInfo> demos) {
        // First, collect all demos by module
        Map<String, List<DemoInfo>> unordered = new LinkedHashMap<>();
        for (var demo : demos) {
            unordered.computeIfAbsent(demo.module(), k -> new ArrayList<>()).add(demo);
        }

        // Extract "Core" demos to add at the end as "Other"
        var otherDemos = unordered.remove(OTHER_MODULE);

        // Build ordered map: first the known modules in order, then any remaining alphabetically
        Map<String, List<DemoInfo>> ordered = new LinkedHashMap<>();

        // Add modules in the defined order
        for (var module : MODULE_ORDER) {
            var moduleDemos = unordered.remove(module);
            if (moduleDemos != null) {
                ordered.put(module, moduleDemos);
            }
        }

        // Add any remaining modules alphabetically (excluding Core which was already removed)
        unordered.keySet().stream()
                .sorted()
                .forEach(module -> ordered.put(module, unordered.get(module)));

        // Add "Other" (Core demos) at the very end
        if (otherDemos != null && !otherDemos.isEmpty()) {
            ordered.put("Other", otherDemos);
        }

        return ordered;
    }

    private List<DemoInfo> parseMetadataFiles() {
        List<DemoInfo> demos = new ArrayList<>();

        for (var file : getMetadataFiles().getFiles()) {
            if (file.getName().endsWith(".json")) {
                try {
                    var content = Files.readString(file.toPath());
                    var info = parseJson(content);
                    // Filter out internal demos
                    if (info != null && !info.internal()) {
                        demos.add(info);
                    }
                } catch (IOException e) {
                    getLogger().warn("Failed to read metadata file: " + file, e);
                }
            }
        }

        return demos;
    }

    private DemoInfo parseJson(String json) {
        String id = null;
        String displayName = null;
        var description = "";
        var module = "Other";
        String castFileName = null;
        var internal = false;

        var matcher = JSON_FIELD.matcher(json);
        while (matcher.find()) {
            var field = matcher.group(1);
            var value = matcher.group(2); // For string values
            switch (field) {
                case "id" -> id = value;
                case "displayName" -> displayName = value;
                case "description" -> description = value != null ? value : "";
                case "module" -> module = value != null ? value : "Other";
                case "castFileName" -> castFileName = value;
            }
        }

        // Parse internal field separately (it's a boolean, not a string)
        if (json.contains("\"internal\": true")) {
            internal = true;
        }

        if (id != null && displayName != null && castFileName != null) {
            return new DemoInfo(id, displayName, description, module, castFileName, internal);
        }
        return null;
    }

    private String generateIndexAdoc(Map<String, List<DemoInfo>> byModule) {
        var title = getTitle().get();

        var sb = new StringBuilder();

        // AsciiDoc header with TOC support
        sb.append("= ").append(title).append("\n");
        sb.append(":doctype: book\n");
        sb.append(":icons: font\n");
        sb.append(":toc: left\n");
        sb.append(":toclevels: 2\n");
        sb.append("\n");
        sb.append("Interactive demos of TamboUI applications. ");
        sb.append("Each demo can be played, paused, and scrubbed through.\n");
        sb.append("\n");
        sb.append("Demos are organized by module. Click on a module to view its demos.\n");
        sb.append("\n");

        // Module sections with links
        for (var entry : byModule.entrySet()) {
            var module = entry.getKey();
            var moduleDemos = entry.getValue();
            var fileName = "demos-" + module.toLowerCase(Locale.ROOT) + ".adoc";

            // Section header for TOC
            sb.append("== ").append(module).append("\n\n");

            // Link to module page
            var demoCount = moduleDemos.size() + " demo" + (moduleDemos.size() != 1 ? "s" : "");
            sb.append("xref:").append(fileName).append("[View all ").append(module).append(" demos]")
                    .append(" (").append(demoCount).append(")\n\n");

            // List demo names
            var demoNames = moduleDemos.stream()
                    .map(DemoInfo::displayName)
                    .toList();
            sb.append(String.join(", ", demoNames)).append("\n\n");
        }

        return sb.toString();
    }

    private String generateModuleAdoc(String module, List<DemoInfo> demos) {
        var castBasePath = getCastBasePath().get();

        var sb = new StringBuilder();

        // AsciiDoc header with TOC support
        sb.append("= ").append(module).append(" Demos\n");
        sb.append(":doctype: book\n");
        sb.append(":icons: font\n");
        sb.append(":toc: left\n");
        sb.append(":toclevels: 2\n");
        sb.append("\n");
        sb.append("xref:demos.adoc[Back to Demo Gallery]\n");
        sb.append("\n");

        // Demo sections
        for (var demo : demos) {
            sb.append("== ").append(demo.displayName()).append("\n\n");

            if (!demo.description().isEmpty()) {
                sb.append(demo.description()).append("\n\n");
            }

            // Use passthrough block to embed the asciinema player
            sb.append("++++\n");
            sb.append("<div id=\"player-").append(demo.id()).append("\" class=\"demo-player\"></div>\n");
            sb.append("<script>\n");
            sb.append("AsciinemaPlayer.create('").append(castBasePath).append("/")
                    .append(demo.castFileName()).append("', document.getElementById('player-")
                    .append(demo.id()).append("'), {\n");
            sb.append("    autoPlay: true, terminalLineHeight: 1, loop: true, fit: 'width'\n");
            sb.append("});\n");
            sb.append("</script>\n");
            sb.append("++++\n\n");
        }

        return sb.toString();
    }

    private record DemoInfo(String id, String displayName, String description, String module, String castFileName, boolean internal) {
    }
}
