/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demos;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Layout;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.text.MarkupParser;
import dev.tamboui.text.Text;
import dev.tamboui.tui.InlineTuiConfig;
import dev.tamboui.tui.InlineTuiRunner;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.widgets.block.Title;
import dev.tamboui.widgets.common.SizedWidget;
import dev.tamboui.widgets.input.TextInput;
import dev.tamboui.widgets.input.TextInputState;
import dev.tamboui.widgets.list.ListItem;
import dev.tamboui.widgets.list.ListState;
import dev.tamboui.widgets.list.ListWidget;

/**
 * Main launcher for the TamboUI demos fat jar.
 * <p>
 * Usage:
 * <ul>
 *   <li>{@code java -jar tamboui-demos.jar} - List all available demos</li>
 *   <li>{@code java -jar tamboui-demos.jar <demo-name>} - Run a specific demo</li>
 *   <li>{@code java -jar tamboui-demos.jar <demo-name> [args...]} - Run a demo with arguments</li>
 *   <li>{@code java -jar tamboui-demos.jar -i} or {@code --interactive} - Interactive demo selector</li>
 * </ul>
 */
public class DemoLauncher {

    private static final String MANIFEST_PATH = "/demos-manifest.json";

    private DemoLauncher() {
        // Utility class - prevent instantiation
    }

    /**
     * Demo entry containing metadata and main class.
     */
    static final class DemoEntry {
        private final String id;
        private final String displayName;
        private final String projectPath;
        private final String description;
        private final String module;
        private final String mainClass;
        private final Set<String> tags;
        
        /**
         * Creates a new demo entry.
         * @param id          the unique demo ID (used for CLI)
         * @param displayName the human-friendly name of the demo
         * @param description a brief description of the demo
         * @param module      the module/category this demo belongs to
         * @param mainClass   the fully qualified main class to launch for this demo
         * @param tags        the tags associated with this demo
         */
        DemoEntry(String id, String displayName, String projectPath, String description, String module, String mainClass, Set<String> tags) {
            this.id = id;
            this.displayName = displayName;
            this.projectPath = projectPath;
            this.description = description;
            this.module = module;
            this.mainClass = mainClass;
            this.tags = tags;
        }

        public String id() {
            return id;
        }

        public String displayName() {
            return displayName;
        }

        public String description() {
            return description;
        }

        public String module() {
            return module;
        }

        public String mainClass() {
            return mainClass;
        }

        public Set<String> tags() {
            return tags;
        }

        public String projectPath() {
            return projectPath;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            DemoEntry that = (DemoEntry) obj;
            return Objects.equals(this.id, that.id) &&
                    Objects.equals(this.projectPath, that.projectPath) &&
                    Objects.equals(this.displayName, that.displayName) &&
                    Objects.equals(this.description, that.description) &&
                    Objects.equals(this.module, that.module) &&
                    Objects.equals(this.mainClass, that.mainClass);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, displayName, projectPath, description, module, mainClass);
        }

        @Override
        public String toString() {
            return "DemoEntry[" +
                    "id=" + id + ", " +
                    "projectPath=" + projectPath + ", " +
                    "displayName=" + displayName + ", " +
                    "description=" + description + ", " +
                    "module=" + module + ", " +
                    "mainClass=" + mainClass + ", " +
                    "tags=" + tags + ']';
        }

        public String url() {
            return "https://github.com/tamboui/tamboui/blob/main" + projectPath.replace(":", "/") + "/src/main/java";
        }

    }

    /**
     * Main entry point.
     *
     * @param args the CLI arguments
     * @throws Exception on unexpected error
     */
    public static void main(String[] args) throws Exception {
        Map<String, DemoEntry> demos = loadDemoManifest();

        if (args.length == 0) {
            runInteractiveMode(demos);
            System.exit(0);
        }

        String demoName = args[0];

        if ("--help".equals(demoName) || "-h".equals(demoName)) {
            printUsage(demos);
            System.exit(0);
        }

        if ("--list".equals(demoName) || "-l".equals(demoName)) {
            printDemoList(demos);
            System.exit(0);
        }

        if ("--interactive".equals(demoName) || "-i".equals(demoName)) {
            runInteractiveMode(demos);
            System.exit(0);
        }

        String[] demoArgs = Arrays.copyOfRange(args, 1, args.length);
        launchDemo(demos, demoName, demoArgs);
    }

    private static Map<String, DemoEntry> loadDemoManifest() throws IOException {
        try (InputStream is = DemoLauncher.class.getResourceAsStream(MANIFEST_PATH)) {
            if (is == null) {
                throw new IOException("Demo manifest not found: " + MANIFEST_PATH);
            }
            return parseManifest(is);
        }
    }

    /**
     * Simple JSON parser for the demos manifest.
     * Parses the JSON without external dependencies.
     */
    private static Map<String, DemoEntry> parseManifest(InputStream is) throws IOException {
        Map<String, DemoEntry> demos = new LinkedHashMap<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }

            String json = content.toString();

            // Simple parsing: find each demo object
            int idx = 0;
            while ((idx = json.indexOf("{", idx + 1)) != -1) {
                int end = json.indexOf("}", idx);
                if (end == -1) {
                    break;
                }

                String obj = json.substring(idx, end + 1);

                // Check if this is a demo entry (has mainClass field)
                if (obj.contains("\"mainClass\"")) {
                    String id = extractField(obj, "id");
                    String displayName = extractField(obj, "displayName");
                    String projectPath = extractField(obj, "projectPath");
                    String description = extractField(obj, "description");
                    String module = extractField(obj, "module");
                    String mainClass = extractField(obj, "mainClass");
                    Set<String> tags = extractTags(obj);

                    if (id != null && mainClass != null) {
                        demos.put(id, new DemoEntry(id, displayName, projectPath, description, module, mainClass, tags));
                    }
                }

                idx = end;
            }
        }

        return demos;
    }

    private static Set<String> extractTags(String json) {
        String tags = extractField(json, "tags");
        if (tags == null) {
            return Collections.emptySet();
        }
        return Arrays.stream(tags.split(",")).map(String::trim).collect(Collectors.toSet());
    }

    private static String extractField(String json, String field) {
        String pattern = "\"" + field + "\"";
        int idx = json.indexOf(pattern);
        if (idx == -1) {
            return null;
        }

        idx = json.indexOf(":", idx);
        if (idx == -1) {
            return null;
        }

        idx = json.indexOf("\"", idx);
        if (idx == -1) {
            return null;
        }

        int start = idx + 1;
        int end = start;

        // Find closing quote, handling escapes
        while (end < json.length()) {
            char c = json.charAt(end);
            if (c == '\\' && end + 1 < json.length()) {
                end += 2; // Skip escaped character
            } else if (c == '"') {
                break;
            } else {
                end++;
            }
        }

        String value = json.substring(start, end);
        // Unescape common escape sequences
        return value
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t");
    }

    private static void printUsage(Map<String, DemoEntry> demos) {
        System.out.println("TamboUI Demo Launcher");
        System.out.println("=====================");
        System.out.println();
        System.out.println("Usage: java -jar tamboui-demos.jar [options] [demo-name] [demo-args...]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -h, --help       Show this help message");
        System.out.println("  -l, --list       List all available demos");
        System.out.println("  -i, --interactive Interactive demo selector");
        System.out.println();
        System.out.println("Available demos (" + demos.size() + "):");
        System.out.println();
        printDemoList(demos);
    }

    private static void printDemoList(Map<String, DemoEntry> demos) {
        // Group by module
        Map<String, List<DemoEntry>> byModule = new TreeMap<>();
        for (DemoEntry demo : demos.values()) {
            byModule.computeIfAbsent(demo.module(), k -> new ArrayList<>()).add(demo);
        }

        // Define module order
        List<String> moduleOrder = Arrays.asList("Core", "Widgets", "TUI", "Toolkit", "CSS", "Image", "Picocli", "TFX");

        // Sort each module's demos
        for (List<DemoEntry> list : byModule.values()) {
            list.sort(Comparator.comparing(DemoEntry::displayName));
        }

        // Print in order
        List<String> orderedModules = new ArrayList<>();
        for (String m : moduleOrder) {
            if (byModule.containsKey(m)) {
                orderedModules.add(m);
            }
        }
        // Add any remaining modules
        for (String m : byModule.keySet()) {
            if (!orderedModules.contains(m)) {
                orderedModules.add(m);
            }
        }

        for (String module : orderedModules) {
            List<DemoEntry> moduleDemos = byModule.get(module);
            if (moduleDemos == null) {
                continue;
            }

            System.out.println(module + ":");
            for (DemoEntry demo : moduleDemos) {
                System.out.printf("  %-30s %s%n", demo.id(), demo.displayName());
            }
            System.out.println();
        }
    }

    private static void launchDemo(Map<String, DemoEntry> demos, String name, String[] args) throws Exception {
        DemoEntry demo = demos.get(name);

        if (demo == null) {
            // Try case-insensitive match
            for (DemoEntry d : demos.values()) {
                if (d.id().equalsIgnoreCase(name)) {
                    demo = d;
                    break;
                }
            }
        }

        if (demo == null) {
            System.err.println("Unknown demo: " + name);
            System.err.println();
            System.err.println("Use --list to see available demos.");
            System.exit(1);
        }

        System.out.println("Launching: " + demo.displayName() + " (" + demo.module() + ")");
        System.out.println();

        Class<?> clazz = Class.forName(demo.mainClass());
        Method main;
        // Try static main(String[] args) first (standard Java/jbang)
        try {
            main = clazz.getMethod("main", String[].class);
        } catch (NoSuchMethodException e1) {
            try {
                main = clazz.getMethod("main");
            } catch (NoSuchMethodException e2) {
                try {
                    main = clazz.getDeclaredMethod("main");
                } catch (ReflectiveOperationException e3) {
                    NoSuchMethodException ex = new NoSuchMethodException(
                            "Could not find main(String[] args), main(), or compact main() in " + clazz.getName());
                    ex.initCause(e3);
                    throw ex;
                }
            }
        }

        main.setAccessible(true);

        Object instance = null;
        // Instance main (e.g. compact / unnamed class source): create instance if needed
        if (!Modifier.isStatic(main.getModifiers()) && instance == null) {
            Constructor<?> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            instance = constructor.newInstance();
        }

        if (main.getParameterCount() == 0) {
            main.invoke(instance);
        } else {
            main.invoke(instance, (Object) args);
        }
    }

    /**
     * Runs the interactive demo selector mode using InlineTuiRunner.
     * Provides a filterable list interface for selecting demos.
     * Loops back to the selector after each demo exits.
     *
     * @param demos the map of available demos
     * @throws Exception if an error occurs
     */
    private static void runInteractiveMode(Map<String, DemoEntry> demos) throws Exception {
        // Convert demos to a sorted list
        List<DemoEntry> allDemos = demos.values().stream()
                .sorted(Comparator.comparing(DemoEntry::module)
                        .thenComparing(DemoEntry::displayName))
                .collect(Collectors.toList());

        // Create inline TUI runner configuration
        int displayHeight = Math.min(20, allDemos.size() + 4); // Input + list + borders
        InlineTuiConfig config = InlineTuiConfig.builder(displayHeight)
                .noTick()
                .clearOnClose(true)
                .build();

        // Remember state across iterations (use arrays for lambda capture)
        String[] lastFilterTextRef = new String[]{""};
        String[] lastSelectedDemoIdRef = new String[]{null};

        // Main loop: keep showing selector until user quits
        while (true) {
            // State for filtering and selection (restore from previous iteration)
            TextInputState filterState = new TextInputState(lastFilterTextRef[0]);
            ListState listState = new ListState();
            @SuppressWarnings("unchecked")
            List<DemoEntry>[] filteredDemosRef = (List<DemoEntry>[]) new List<?>[]{new ArrayList<>(allDemos)};
            DemoEntry[] selectedDemoRef = new DemoEntry[1];
            boolean[] shouldQuitRef = new boolean[1];

            // Apply filter if we have one
            if (!lastFilterTextRef[0].isEmpty()) {
                String filter = lastFilterTextRef[0].toLowerCase();
                List<DemoEntry> filtered = new ArrayList<>();
                for (DemoEntry demo : allDemos) {
                    if (demo.id().toLowerCase().contains(filter) ||
                            demo.displayName().toLowerCase().contains(filter) ||
                            (demo.description() != null && demo.description().toLowerCase().contains(filter)) ||
                            demo.module().toLowerCase().contains(filter) ||
                            demo.tags().stream().anyMatch(tag -> tag.toLowerCase().contains(filter))) {
                        filtered.add(demo);
                    }
                }
                filteredDemosRef[0] = filtered;
            }

            // Restore selection position: try to find the last selected demo
            if (!filteredDemosRef[0].isEmpty()) {
                if (lastSelectedDemoIdRef[0] != null) {
                    int foundIndex = -1;
                    for (int i = 0; i < filteredDemosRef[0].size(); i++) {
                        if (filteredDemosRef[0].get(i).id().equals(lastSelectedDemoIdRef[0])) {
                            foundIndex = i;
                            break;
                        }
                    }
                    if (foundIndex >= 0) {
                        listState.select(foundIndex);
                    } else {
                        // Last selected demo is filtered out, select first
                        listState.selectFirst();
                    }
                } else {
                    // First time, select first item
                    listState.selectFirst();
                }
            }

            // Create and run the inline TUI runner for this iteration
            try (InlineTuiRunner runner = InlineTuiRunner.create(config)) {
                runner.run(
                        (event, r) -> {
                            if (event instanceof KeyEvent) {
                                KeyEvent keyEvent = (KeyEvent) event;

                                // Quit on Ctrl+C or Escape
                                if (keyEvent.isQuit() || keyEvent.isCancel()) {
                                    shouldQuitRef[0] = true;
                                    r.quit();
                                    return true;
                                }

                                // Handle Enter to select demo
                                if (keyEvent.isConfirm()) {
                                    Integer selected = listState.selected();
                                    if (selected != null && selected < filteredDemosRef[0].size()) {
                                        selectedDemoRef[0] = filteredDemosRef[0].get(selected);
                                        // Remember the selected demo ID for next iteration
                                        lastSelectedDemoIdRef[0] = selectedDemoRef[0].id();
                                        r.quit();
                                        return true;
                                    }
                                    return false;
                                }

                                // Handle list navigation
                                if (keyEvent.isUp()) {
                                    listState.selectPrevious();
                                    // Remember current selection
                                    Integer selected = listState.selected();
                                    if (selected != null && selected < filteredDemosRef[0].size()) {
                                        lastSelectedDemoIdRef[0] = filteredDemosRef[0].get(selected).id();
                                    }
                                    return true;
                                }
                                if (keyEvent.isDown()) {
                                    listState.selectNext(filteredDemosRef[0].size());
                                    // Remember current selection
                                    Integer selected = listState.selected();
                                    if (selected != null && selected < filteredDemosRef[0].size()) {
                                        lastSelectedDemoIdRef[0] = filteredDemosRef[0].get(selected).id();
                                    }
                                    return true;
                                }
                                if (keyEvent.isHome()) {
                                    listState.selectFirst();
                                    // Remember current selection
                                    if (!filteredDemosRef[0].isEmpty()) {
                                        lastSelectedDemoIdRef[0] = filteredDemosRef[0].get(0).id();
                                    }
                                    return true;
                                }
                                if (keyEvent.isEnd()) {
                                    listState.selectLast(filteredDemosRef[0].size());
                                    // Remember current selection
                                    Integer selected = listState.selected();
                                    if (selected != null && selected < filteredDemosRef[0].size()) {
                                        lastSelectedDemoIdRef[0] = filteredDemosRef[0].get(selected).id();
                                    }
                                    return true;
                                }

                                // Handle text input for filtering
                                if (handleTextInputKey(filterState, keyEvent)) {
                                    // Remember filter text
                                    lastFilterTextRef[0] = filterState.text();
                                    // Filter demos based on input
                                    String filter = lastFilterTextRef[0].toLowerCase();
                                    List<DemoEntry> filtered = new ArrayList<>();
                                    if (filter.isEmpty()) {
                                        filtered.addAll(allDemos);
                                    } else {
                                        for (DemoEntry demo : allDemos) {
                                            if (demo.id().toLowerCase().contains(filter) ||
                                                    demo.displayName().toLowerCase().contains(filter) ||
                                                    (demo.description() != null && demo.description().toLowerCase().contains(filter)) ||
                                                    demo.module().toLowerCase().contains(filter) ||
                                                    demo.tags().stream().anyMatch(tag -> tag.toLowerCase().contains(filter))) {
                                                filtered.add(demo);
                                            }
                                        }
                                    }
                                    filteredDemosRef[0] = filtered;
                                    // Try to restore selection, or select first item after filtering
                                    if (!filteredDemosRef[0].isEmpty()) {
                                        if (lastSelectedDemoIdRef[0] != null) {
                                            int foundIndex = -1;
                                            for (int i = 0; i < filteredDemosRef[0].size(); i++) {
                                                if (filteredDemosRef[0].get(i).id().equals(lastSelectedDemoIdRef[0])) {
                                                    foundIndex = i;
                                                    break;
                                                }
                                            }
                                            if (foundIndex >= 0) {
                                                listState.select(foundIndex);
                                            } else {
                                                // Last selected demo is filtered out, select first
                                                listState.selectFirst();
                                                lastSelectedDemoIdRef[0] = filteredDemosRef[0].get(0).id();
                                            }
                                        } else {
                                            listState.selectFirst();
                                            lastSelectedDemoIdRef[0] = filteredDemosRef[0].get(0).id();
                                        }
                                    } else {
                                        listState.select(null);
                                        lastSelectedDemoIdRef[0] = null;
                                    }
                                    return true;
                                }
                            }
                            return false;
                        },
                        frame -> {
                            Rect area = frame.area();

                            // Layout: filter input at top, list below
                            List<Rect> layout = Layout.vertical()
                                    .constraints(
                                            Constraint.length(3),  // Filter input with border
                                            Constraint.fill()      // List
                                    )
                                    .split(area);

                            Rect filterArea = layout.get(0);
                            Rect listArea = layout.get(1);

                            // Render filter input
                            renderFilterInput(frame, filterArea, filterState, filteredDemosRef[0].size(), allDemos.size());

                            // Render demo list
                            renderDemoList(frame, listArea, filteredDemosRef[0], listState);
                        }
                );
            }
            // Runner is now closed

            // Check if user wants to quit
            if (shouldQuitRef[0]) {
                break;
            }

            // Launch selected demo if one was chosen
            if (selectedDemoRef[0] != null) {
                System.out.println();
                System.out.println("Launching: " + selectedDemoRef[0].displayName() + " (" + selectedDemoRef[0].module() + ")");
                System.out.println();
                Instant start = Instant.now();
                launchDemo(demos, selectedDemoRef[0].id(), new String[0]);
                if (Duration.between(start, Instant.now()).getSeconds() < 1) {
                    System.out.println();
                    System.out.print("Press Enter to continue (demo ran in less than 1 second)...");
                    try {
                        // Wait for ENTER/keypress before returning to selector
                        System.in.read();
                    } catch (Exception ignore) {
                    }
                    System.out.println();
                }
                // After demo exits, loop will automatically return to selector (creating a new runner)
            }
        }
    }

    /**
     * Renders the filter input widget.
     */
    private static void renderFilterInput(Frame frame, Rect area, TextInputState state, int filteredCount, int totalCount) {
        String title = String.format("Filter (%d/%d)", filteredCount, totalCount);
        TextInput input = TextInput.builder()
                .placeholder("Type to filter demos...")
                .block(Block.builder()
                        .borders(Borders.ALL)
                        .borderType(BorderType.ROUNDED)
                        .title(Title.from(title))
                        .borderStyle(Style.EMPTY.fg(Color.CYAN))
                        .build())
                .build();

        frame.renderStatefulWidget(input, area, state);
    }

    /**
     * Renders the demo list widget.
     */
    private static void renderDemoList(Frame frame, Rect area, List<DemoEntry> demos, ListState state) {
        if (demos.isEmpty()) {
            // Show empty message
            Block emptyBlock = Block.builder()
                    .borders(Borders.ALL)
                    .borderType(BorderType.ROUNDED)
                    .title(Title.from("No demos found"))
                    .borderStyle(Style.EMPTY.fg(Color.RED))
                    .build();
            frame.renderWidget(emptyBlock, area);
            return;
        }

        // Convert demos to list items
        List<SizedWidget> items = new ArrayList<>();
        for (DemoEntry demo : demos) {
            // Format: "Module: Display Name - Description"
            String description = demo.description() != null && !demo.description().isEmpty()
                    ? " - " + demo.description()
                    : "";
            String itemText = String.format("[dim]%s[/]: [bold]%s[/][dim]%s[/] [link=%s]source[/link]", demo.module(), demo.displayName(), description, demo.url());
           /*  if (!demo.tags().isEmpty()) {
                itemText += "\n";
                itemText += " [blue]" + String.join(", ", demo.tags()) + "[/blue]";
            }*/
            Text markup = MarkupParser.parse(itemText);
            items.add(ListItem.from(markup).toSizedWidget());
        }

        ListWidget list = ListWidget.builder()
                .items(items)
                .highlightStyle(Style.EMPTY.reversed().fg(Color.YELLOW))
                .highlightSymbol("> ")
                .block(Block.builder()
                        .borders(Borders.ALL)
                        .borderType(BorderType.ROUNDED)
                        .title(Title.from("Select a demo (Enter to launch, Esc to quit launcher)"))
                        .borderStyle(Style.EMPTY.fg(Color.CYAN))
                        .build())
                .build();

        frame.renderStatefulWidget(list, area, state);
    }

    /**
     * Handles key events for text input, similar to Toolkit.handleTextInputKey.
     * This is a simplified version that doesn't require the Toolkit dependency.
     */
    private static boolean handleTextInputKey(TextInputState state, KeyEvent event) {
        switch (event.code()) {
            case BACKSPACE:
                state.deleteBackward();
                return true;
            case DELETE:
                state.deleteForward();
                return true;
            case LEFT:
                state.moveCursorLeft();
                return true;
            case RIGHT:
                state.moveCursorRight();
                return true;
            case HOME:
                state.moveCursorToStart();
                return true;
            case END:
                state.moveCursorToEnd();
                return true;
            case CHAR:
                // Don't consume characters with Ctrl or Alt modifiers - those are control sequences
                if (event.modifiers().ctrl() || event.modifiers().alt()) {
                    return false;
                }
                char c = event.character();
                if (c >= 32 && c < 127) {
                    state.insert(c);
                    return true;
                }
                return false;
            default:
                return false;
        }
    }
}
