/*
 * Copyright (c) 2025 Glimt Contributors
 * SPDX-License-Identifier: MIT
 */
package ink.glimt.demo;

import ink.glimt.style.Color;
import ink.glimt.style.Style;
import ink.glimt.text.Text;
import ink.glimt.toolkit.app.ToolkitRunner;
import ink.glimt.toolkit.element.RenderContext;
import ink.glimt.toolkit.element.StyledElement;
import ink.glimt.toolkit.event.EventResult;
import ink.glimt.toolkit.event.KeyEventHandler;
import ink.glimt.tui.Keys;
import ink.glimt.tui.TuiConfig;
import ink.glimt.toolkit.elements.Column;
import ink.glimt.widgets.list.ListItem;
import ink.glimt.widgets.list.ListState;
import ink.glimt.widgets.paragraph.Paragraph;
import ink.glimt.layout.Rect;
import ink.glimt.terminal.Frame;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static ink.glimt.toolkit.Toolkit.column;
import static ink.glimt.toolkit.Toolkit.fill;
import static ink.glimt.toolkit.Toolkit.list;
import static ink.glimt.toolkit.Toolkit.panel;
import static ink.glimt.toolkit.Toolkit.percent;
import static ink.glimt.toolkit.Toolkit.row;
import static ink.glimt.toolkit.Toolkit.spacer;
import static ink.glimt.toolkit.Toolkit.text;

/**
 * File browser demo built with the Toolkit DSL.
 *
 * Left pane lists files in the current directory.
 * Right pane shows info or a preview (toggle with 'i' and 'v').
 */
public class FileBrowserDemo {

    private static final int MAX_PREVIEW_LINES = 40;
    private static final int MAX_PREVIEW_CHARS = 4000;

    private final ListState listState = new ListState();
    private List<FileEntry> entries = new ArrayList<>();
    private Path currentDir = Paths.get(".").toAbsolutePath().normalize();
    private DetailMode detailMode = DetailMode.INFO;
    private ToolkitRunner runner;

    private enum DetailMode { INFO, VIEW }

    private record FileEntry(Path path, boolean directory, long size, Instant modified) {
        String name() {
            Path fileName = path.getFileName();
            return fileName != null ? fileName.toString() : path.toString();
        }

        String displayName() {
            return directory ? name() + "/" : name();
        }
    }

    public static void main(String[] args) throws Exception {
        new FileBrowserDemo().run();
    }

    public void run() throws Exception {
        refreshEntries();
        if (!entries.isEmpty()) {
            listState.selectFirst();
        }

        TuiConfig config = TuiConfig.builder()
            .mouseCapture(true)
            .build();

        try (ToolkitRunner runner = ToolkitRunner.create(config)) {
            this.runner = runner;
            runner.focusManager().setFocus("files");
            runner.run(this::render);
        }
    }

    private StyledElement<?> render() {
        if (runner != null && runner.focusManager().focusedId() == null && !entries.isEmpty()) {
            runner.focusManager().setFocus("files");
        }

        StyledElement<?> listPanel = buildListPanel();
        StyledElement<?> detailPanel = buildDetailPanel();

        return column(
            panel(() -> row(
                text(" File Browser ").bold().cyan(),
                spacer(),
                text("[j/k/↑/↓] Move  [Enter] Open  [h] Up  [i] Info  [v] View  [q] Quit").dim()
            ))
                .rounded()
                .borderColor(Color.DARK_GRAY)
                .length(3),
            row(
                listPanel.constraint(percent(40)),
                spacer(1),
                detailPanel.constraint(fill(1))
            ).fill()
        );
    }

    private StyledElement<?> buildListPanel() {
        ListItem[] items = entries.stream()
            .map(this::toListItem)
            .toArray(ListItem[]::new);

        return panel("Directory: " + currentDir)
            .rounded()
            .borderColor(Color.GRAY)
            .focusedBorderColor(Color.CYAN)
            .id("files")
            .focusable()
            .onKeyEvent(handleKeys())
            .add(
                list()
                    .state(listState)
                    .listItems(items)
                    .highlightStyle(Style.EMPTY.fg(Color.CYAN).bold())
                    .highlightSymbol("▸ ")
            );
    }

    private StyledElement<?> buildDetailPanel() {
        return panel(detailMode == DetailMode.VIEW ? "Preview" : "Info", this::buildDetailsContent)
            .rounded()
            .borderColor(Color.GRAY);
    }

    private StyledElement<?> buildDetailsContent() {
        Column content = column().spacing(1);
        FileEntry entry = selectedEntry();

        if (entry == null) {
            return content.add(line("Select a file to see details"));
        }

        content.add(line(entry.displayName()).bold());

        if (detailMode == DetailMode.INFO) {
            content.add(line(entry.directory ? "Directory" : "File (" + humanSize(entry.size) + ")"));
            content.add(line("Modified " + formatAge(entry.modified)));
            content.add(paragraph(entry.path.toAbsolutePath().toString()).length(2).dim());
            if (!entry.directory) {
                content.add(line("Press 'v' to view this file").dim());
            }
        } else {
            List<String> previewLines = readPreview(entry);
            String preview = String.join("\n", previewLines);
            content.add(paragraph(preview).fill());
            content.add(line("Press 'i' for file info").dim());
        }

        return content;
    }

    private KeyEventHandler handleKeys() {
        return event -> {
            if (Keys.isUp(event) || event.isChar('k')) {
                listState.selectPrevious();
                return EventResult.HANDLED;
            }
            if (Keys.isDown(event) || event.isChar('j')) {
                listState.selectNext(entries.size());
                return EventResult.HANDLED;
            }
            if (Keys.isEnter(event) || Keys.isRight(event)) {
                openDirectory();
                return EventResult.HANDLED;
            }
            if (Keys.isLeft(event) || event.isChar('h')) {
                goParent();
                return EventResult.HANDLED;
            }
            if (event.isChar('i')) {
                detailMode = DetailMode.INFO;
                return EventResult.HANDLED;
            }
            if (event.isChar('v')) {
                detailMode = DetailMode.VIEW;
                return EventResult.HANDLED;
            }
            if (event.isChar('q')) {
                if (runner != null) {
                    runner.quit();
                }
                return EventResult.HANDLED;
            }
            return EventResult.UNHANDLED;
        };
    }

    private void openDirectory() {
        FileEntry entry = selectedEntry();
        if (entry != null && entry.directory) {
            currentDir = entry.path.toAbsolutePath().normalize();
            detailMode = DetailMode.INFO;
            refreshEntries();
        }
    }

    private void goParent() {
        Path parent = currentDir.getParent();
        if (parent != null) {
            currentDir = parent.normalize();
            detailMode = DetailMode.INFO;
            refreshEntries();
        }
    }

    private void refreshEntries() {
        List<FileEntry> refreshed = new ArrayList<>();
        try (Stream<Path> stream = Files.list(currentDir)) {
            stream.forEach(path -> refreshed.add(toEntry(path)));
        } catch (IOException e) {
            refreshed.clear();
        }

        refreshed.sort(Comparator
            .comparing(FileEntry::directory, Comparator.reverseOrder())
            .thenComparing(FileEntry::name, String.CASE_INSENSITIVE_ORDER));

        entries = refreshed;

        Integer selected = listState.selected();
        if (entries.isEmpty()) {
            listState.select(null);
        } else if (selected == null) {
            listState.selectFirst();
        } else if (selected >= entries.size()) {
            listState.select(entries.size() - 1);
        }
    }

    private FileEntry toEntry(Path path) {
        boolean isDirectory = Files.isDirectory(path);
        long size = 0L;
        Instant modified = Instant.EPOCH;
        try {
            if (!isDirectory) {
                size = Files.size(path);
            }
            modified = Files.getLastModifiedTime(path).toInstant();
        } catch (IOException ignored) {
            // Leave defaults if attributes cannot be read
        }
        return new FileEntry(path, isDirectory, size, modified);
    }

    private FileEntry selectedEntry() {
        Integer selected = listState.selected();
        if (selected == null || selected < 0 || selected >= entries.size()) {
            return null;
        }
        return entries.get(selected);
    }

    private ListItem toListItem(FileEntry entry) {
        Style style = entry.directory
            ? Style.EMPTY.fg(Color.YELLOW).bold()
            : Style.EMPTY;
        return ListItem.from(entry.displayName()).style(style);
    }

    private List<String> readPreview(FileEntry entry) {
        if (entry.directory) {
            return List.of("Directories cannot be previewed.");
        }
        if (!Files.isRegularFile(entry.path)) {
            return List.of("Not a regular file.");
        }

        List<String> lines = new ArrayList<>();
        int totalChars = 0;
        boolean truncated = false;

        try (BufferedReader reader = Files.newBufferedReader(entry.path)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (lines.size() >= MAX_PREVIEW_LINES) {
                    truncated = true;
                    break;
                }
                if (totalChars + line.length() > MAX_PREVIEW_CHARS) {
                    lines.add(line.substring(0, Math.max(0, MAX_PREVIEW_CHARS - totalChars)));
                    truncated = true;
                    break;
                }
                lines.add(line);
                totalChars += line.length();
            }
        } catch (IOException e) {
            lines.add("Unable to read file: " + e.getMessage());
        }

        if (truncated) {
            lines.add("... truncated ...");
        }

        if (lines.isEmpty()) {
            lines.add("[empty file]");
        }
        return lines;
    }

    private String humanSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        String[] units = {"KB", "MB", "GB", "TB"};
        double value = bytes;
        int unitIndex = 0;
        while (value >= 1024 && unitIndex < units.length - 1) {
            value /= 1024;
            unitIndex++;
        }
        return String.format(Locale.US, "%.1f %s", value, units[unitIndex]);
    }

    private String formatAge(Instant modified) {
        Duration age = Duration.between(modified, Instant.now()).abs();

        if (age.minusMinutes(1).isNegative()) {
            return "just now";
        }
        if (age.minusHours(1).isNegative()) {
            return age.toMinutes() + "m ago";
        }
        if (age.minusDays(1).isNegative()) {
            return age.toHours() + "h ago";
        }
        if (age.minusDays(30).isNegative()) {
            return age.toDays() + "d ago";
        }
        long months = age.toDays() / 30;
        if (months < 12) {
            return months + "mo ago";
        }
        long years = months / 12;
        return years + "y ago";
    }

    private ParagraphElement paragraph(String content) {
        return new ParagraphElement(content);
    }

    private ParagraphElement line(String content) {
        return new ParagraphElement(content).length(1);
    }

    private static final class ParagraphElement extends StyledElement<ParagraphElement> {
        private final String content;

        ParagraphElement(String content) {
            this.content = content != null ? content : "";
        }

        @Override
        public void render(Frame frame, Rect area, RenderContext context) {
            if (area.isEmpty() || content.isEmpty()) {
                return;
            }
            frame.renderWidget(
                Paragraph.builder()
                    .text(Text.from(content))
                    .style(style)
                    .build(),
                area
            );
        }
    }
}
