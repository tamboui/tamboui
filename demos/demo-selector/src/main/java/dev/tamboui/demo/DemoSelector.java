/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import dev.tamboui.build.model.DemosModel;
import dev.tamboui.layout.Constraint;
import dev.tamboui.style.Color;
import dev.tamboui.toolkit.app.ToolkitApp;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.bindings.Actions;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.toolkit.elements.ListElement;
import dev.tamboui.style.Overflow;
import org.gradle.tooling.GradleConnector;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static dev.tamboui.toolkit.Toolkit.*;

/**
 * Interactive TUI demo selector with collapsible module groups.
 */
public class DemoSelector extends ToolkitApp {

    private static final String SELF_NAME = "demo-selector";

    private final ListElement<?> demoList;
    private final Map<String, List<DemoInfo>> demosByModule = new TreeMap<>();
    private final Set<String> expandedModules = new HashSet<>();
    private String filter = "";
    private String selectedDemo = null;

    // Display list state
    private final List<DisplayItem> displayItems = new ArrayList<>();

    private DemoSelector() {
        demoList = list()
            .highlightSymbol("> ")
            .highlightColor(Color.YELLOW)
            .autoScroll()
            .scrollbar()
            .scrollbarThumbColor(Color.CYAN);
    }

    /**
     * Demo entry point.
     * @param args the CLI arguments
     * @throws Exception on unexpected error
     */
    public static void main(String[] args) throws Exception {
        var selector = new DemoSelector();
        selector.run();

        if (selector.selectedDemo != null) {
            System.out.println(selector.selectedDemo);
            System.exit(0);
        } else {
            System.exit(1);
        }
    }

    @Override
    protected void onStart() {
        discoverDemos();
        // Expand all modules by default
        expandedModules.addAll(demosByModule.keySet());
        rebuildDisplayList();
        if (!displayItems.isEmpty()) {
            demoList.selected(findFirstSelectable());
        }
    }

    /**
     * Rebuilds the display list based on current filter and expanded state.
     */
    private void rebuildDisplayList() {
        displayItems.clear();
        var lowerFilter = filter.toLowerCase(Locale.ROOT);

        for (var entry : demosByModule.entrySet()) {
            var module = entry.getKey();
            var demos = entry.getValue();

            // Filter demos
            var filteredDemos = filter.isEmpty() ? demos : demos.stream()
                    .filter(d -> d.displayName().toLowerCase(Locale.ROOT).contains(lowerFilter)
                            || d.name().toLowerCase(Locale.ROOT).contains(lowerFilter)
                            || d.description().toLowerCase(Locale.ROOT).contains(lowerFilter)
                            || d.tags().stream().anyMatch(t -> t.toLowerCase(Locale.ROOT).contains(lowerFilter)))
                    .toList();

            if (filteredDemos.isEmpty() && !filter.isEmpty()) {
                continue; // Skip empty groups when filtering
            }

            // Add module header
            var expanded = expandedModules.contains(module);
            var demoCount = filter.isEmpty() ? demos.size() : filteredDemos.size();
            displayItems.add(new DisplayItem(module, null, expanded, demoCount));

            // Add demos if expanded
            if (expanded) {
                for (var demo : filteredDemos) {
                    displayItems.add(new DisplayItem(module, demo, false, 0));
                }
            }
        }
    }

    /**
     * Returns the currently selected item.
     */
    private DisplayItem selectedItem() {
        var idx = demoList.selected();
        if (idx >= 0 && idx < displayItems.size()) {
            return displayItems.get(idx);
        }
        return null;
    }

    /**
     * Returns the total demo count across all groups.
     */
    private int totalDemoCount() {
        return demosByModule.values().stream().mapToInt(List::size).sum();
    }

    @Override
    protected Element render() {
        List<String> lines = new ArrayList<>();
        for (var item : displayItems) {
            lines.add(item.toDisplayString());
        }

        var title = filter.isEmpty()
                ? "Demos (" + totalDemoCount() + ")"
                : "Filter: " + filter;

        var selected = selectedItem();

        // Build description panel content based on selection
        Element descriptionContent;
        if (selected != null && selected.demo() != null) {
            var tags = selected.demo().tags();
            var tagsLine = tags.isEmpty() ? "" : "Tags: " + String.join(", ", tags);
            descriptionContent = column(
                    text(tagsLine).magenta().overflow(Overflow.WRAP_WORD),
                    text(""),
                    text(selected.demo().description()).overflow(Overflow.WRAP_WORD)
            );
        } else if (selected != null) {
            // Module header selected
            var count = demosByModule.get(selected.module()).size();
            descriptionContent = column(
                    text(selected.module()).bold().cyan().length(1),
                    text(""),
                    text(count + " demo" + (count != 1 ? "s" : "") + " in this module.").length(1),
                    text(""),
                    text(selected.expanded() ? "Press ← or Enter to collapse." : "Press → or Enter to expand.")
                            .dim().length(1)
            );
        } else {
            descriptionContent = text("");
        }

        return column(
                // Header
                panel(
                        text(" TamboUI Demo Selector ").bold().cyan()
                ).rounded().borderColor(Color.CYAN).length(3),

                // Main content
                row(
                        // Demo list
                        panel(
                                demoList.items(lines)
                        )
                                .title(title)
                                .rounded()
                                .borderColor(filter.isEmpty() ? Color.WHITE : Color.YELLOW)
                                .id("demo-list")
                                .focusable()
                                .onKeyEvent(this::handleKey)
                                .constraint(Constraint.percentage(50)),

                        // Description panel
                        panel(descriptionContent)
                                .title("Description")
                                .rounded()
                                .borderColor(Color.DARK_GRAY)
                                .constraint(Constraint.percentage(50))
                ),

                // Footer
                panel(
                        text(" Type: Filter | ←/→: Collapse/Expand | ↑↓: Navigate | PgUp/PgDn: Sections | Enter: Select | Ctrl+C: Quit ").dim()
                ).rounded().borderColor(Color.DARK_GRAY).length(3)
        );
    }

    private EventResult handleKey(KeyEvent event) {
        var listSize = displayItems.size();
        var current = demoList.selected();

        // Note: Basic navigation (UP/DOWN/HOME/END) is now handled automatically
        // by ListElement via ContainerElement forwarding. Only custom behavior
        // (section jumping, collapse/expand, filtering) needs manual handling.

        // PAGE_DOWN: Jump to next section
        if (event.matches(Actions.PAGE_DOWN)) {
            for (int i = current + 1; i < listSize; i++) {
                if (displayItems.get(i).demo() == null) {
                    demoList.selected(i);
                    break;
                }
            }
            return EventResult.HANDLED;
        }

        // PAGE_UP: Jump to previous section
        if (event.matches(Actions.PAGE_UP)) {
            for (int i = current - 1; i >= 0; i--) {
                if (displayItems.get(i).demo() == null) {
                    demoList.selected(i);
                    break;
                }
            }
            return EventResult.HANDLED;
        }

        // Left: collapse current group or go to parent
        if (event.matches(Actions.MOVE_LEFT)) {
            var selected = selectedItem();
            if (selected != null) {
                if (selected.demo() == null) {
                    // On module header - collapse if expanded
                    if (selected.expanded()) {
                        toggleModule(selected.module());
                    }
                } else {
                    // On demo - go to parent module header
                    for (var i = current - 1; i >= 0; i--) {
                        if (displayItems.get(i).demo() == null) {
                            demoList.selected(i);
                            break;
                        }
                    }
                }
            }
            return EventResult.HANDLED;
        }

        // Right: expand current group or go to first child
        if (event.matches(Actions.MOVE_RIGHT)) {
            var selected = selectedItem();
            if (selected != null && selected.demo() == null) {
                if (!selected.expanded()) {
                    // Expand
                    toggleModule(selected.module());
                } else if (current + 1 < listSize && displayItems.get(current + 1).demo() != null) {
                    // Already expanded - go to first child
                    demoList.selected(current + 1);
                }
            }
            return EventResult.HANDLED;
        }

        // Select: toggle module or select demo
        if (event.matches(Actions.SELECT)) {
            var selected = selectedItem();
            if (selected != null) {
                if (selected.demo() == null) {
                    // Module header - toggle
                    toggleModule(selected.module());
                } else {
                    // Demo - select and quit
                    selectedDemo = selected.demo().name();
                    quit();
                }
            }
            return EventResult.HANDLED;
        }

        // Clear filter with Escape/Cancel
        if (event.matches(Actions.CANCEL) && !filter.isEmpty()) {
            filter = "";
            rebuildDisplayList();
            demoList.selected(findFirstSelectable());
            return EventResult.HANDLED;
        }

        // Backspace
        if (event.matches(Actions.DELETE_BACKWARD) && !filter.isEmpty()) {
            filter = filter.substring(0, filter.length() - 1);
            rebuildDisplayList();
            demoList.selected(findFirstSelectable());
            return EventResult.HANDLED;
        }

        // Type to filter
        if (event.code() == KeyCode.CHAR && !event.hasCtrl() && !event.hasAlt()) {
            var c = event.character();
            if (Character.isLetterOrDigit(c) || c == '-' || c == '_') {
                filter += c;
                rebuildDisplayList();
                demoList.selected(findFirstSelectable());
                return EventResult.HANDLED;
            }
        }

        return EventResult.UNHANDLED;
    }

    /**
     * Toggles the expanded state of a module.
     */
    private void toggleModule(String module) {
        if (expandedModules.contains(module)) {
            expandedModules.remove(module);
        } else {
            expandedModules.add(module);
        }
        // Remember current selection context
        var current = selectedItem();
        var currentModule = current != null ? current.module() : null;

        rebuildDisplayList();

        // Try to stay on the same module header
        if (currentModule != null) {
            for (var i = 0; i < displayItems.size(); i++) {
                var item = displayItems.get(i);
                if (item.demo() == null && item.module().equals(currentModule)) {
                    demoList.selected(i);
                    return;
                }
            }
        }
        demoList.selected(findFirstSelectable());
    }

    private int findFirstSelectable() {
        return 0;
    }

    private void discoverDemos() {
        var projectRoot = findProjectRoot();
        if (projectRoot == null) {
            return;
        }

        var stdout = System.out;
        var stderr = System.err;
        System.setOut(new PrintStream(OutputStream.nullOutputStream()));
        System.setErr(new PrintStream(OutputStream.nullOutputStream()));

        // Use Gradle Tooling API to fetch the demos model
        try (var connection = GradleConnector.newConnector()
                .forProjectDirectory(projectRoot)
                .connect()) {

            // Suppress Gradle output by redirecting to null streams
            var model = connection.model(DemosModel.class)
                    .setColorOutput(false)
                    .setStandardOutput(OutputStream.nullOutputStream())
                    .setStandardError(OutputStream.nullOutputStream())
                    .get();

            for (var demo : model.getDemos()) {
                if (SELF_NAME.equals(demo.getName())) {
                    continue;
                }

                var module = demo.getModule();
                var demoInfo = new DemoInfo(
                        demo.getName(),
                        demo.getDisplayName(),
                        demo.getDescription(),
                        module,
                        demo.getTags()
                );

                demosByModule.computeIfAbsent(module, k -> new ArrayList<>()).add(demoInfo);
            }
        } catch (Exception e) {
            // Fall back to empty list if Tooling API fails
            throw new RuntimeException("Warning: Could not fetch demos via Tooling API: " + e.getMessage());
        } finally {
            System.setOut(stdout);
            System.setErr(stderr);
        }
    }

    private File findProjectRoot() {
        var cwd = Paths.get(System.getProperty("user.dir"));
        if (Files.exists(cwd.resolve("settings.gradle.kts"))) {
            return cwd.toFile();
        }
        var parent = cwd.getParent();
        while (parent != null) {
            if (Files.exists(parent.resolve("settings.gradle.kts"))) {
                return parent.toFile();
            }
            parent = parent.getParent();
        }
        return null;
    }

    private record DemoInfo(String name, String displayName, String description, String module, Set<String> tags) {
    }

    /**
     * Represents an item in the display list (either a module header or a demo).
     */
    private record DisplayItem(String module, DemoInfo demo, boolean expanded, int demoCount) {

        String toDisplayString() {
            if (demo == null) {
                // Module header
                var icon = expanded ? "▼" : "▶";
                return icon + " " + module + " (" + demoCount + ")";
            } else {
                // Demo entry (indented)
                return "    " + demo.displayName();
            }
        }
    }
}
