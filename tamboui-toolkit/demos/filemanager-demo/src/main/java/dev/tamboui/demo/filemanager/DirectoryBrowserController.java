/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo.filemanager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Manages browsing a directory: navigation, selection, and marking files for operations.
 * This is a pure state holder with no UI dependencies.
 */
public final class DirectoryBrowserController {

    public record FileEntry(
        String name,
        boolean isDirectory,
        long size,
        boolean isReadable,
        boolean isWritable
    ) {
        public static FileEntry parentDir() {
            return new FileEntry("..", true, 0, true, false);
        }
    }

    private Path currentDirectory;
    private final List<FileEntry> entries = new ArrayList<>();
    private int cursorIndex = 0;
    private int scrollOffset = 0;
    private final Set<String> markedFiles = new HashSet<>();
    private int visibleRows = 20;

    public DirectoryBrowserController(Path startDirectory) {
        this.currentDirectory = startDirectory.toAbsolutePath().normalize();
        refresh();
    }

    // ═══════════════════════════════════════════════════════════════
    // QUERIES - Read state
    // ═══════════════════════════════════════════════════════════════

    public Path currentDirectory() {
        return currentDirectory;
    }

    public List<FileEntry> entries() {
        return List.copyOf(entries);
    }

    public int cursorIndex() {
        return cursorIndex;
    }

    public int scrollOffset() {
        return scrollOffset;
    }

    public FileEntry selectedEntry() {
        if (entries.isEmpty()) {
            return null;
        }
        return entries.get(cursorIndex);
    }

    public Path selectedPath() {
        var entry = selectedEntry();
        if (entry == null || entry.name().equals("..")) {
            return null;
        }
        return currentDirectory.resolve(entry.name());
    }

    public boolean isMarked(String name) {
        return markedFiles.contains(name);
    }

    public Set<String> markedFiles() {
        return Set.copyOf(markedFiles);
    }

    public List<Path> markedPaths() {
        return markedFiles.stream()
            .map(name -> currentDirectory.resolve(name))
            .toList();
    }

    public boolean hasMarkedFiles() {
        return !markedFiles.isEmpty();
    }

    public int markedCount() {
        return markedFiles.size();
    }

    public int visibleRows() {
        return visibleRows;
    }

    // ═══════════════════════════════════════════════════════════════
    // COMMANDS - Modify state
    // ═══════════════════════════════════════════════════════════════

    public void setVisibleRows(int rows) {
        this.visibleRows = Math.max(1, rows);
        adjustScroll();
    }

    public void cursorUp() {
        if (cursorIndex > 0) {
            cursorIndex--;
            adjustScroll();
        }
    }

    public void cursorDown() {
        if (cursorIndex < entries.size() - 1) {
            cursorIndex++;
            adjustScroll();
        }
    }

    public void pageUp() {
        cursorIndex = Math.max(0, cursorIndex - visibleRows);
        adjustScroll();
    }

    public void pageDown() {
        cursorIndex = Math.min(entries.size() - 1, cursorIndex + visibleRows);
        adjustScroll();
    }

    public void cursorToStart() {
        cursorIndex = 0;
        scrollOffset = 0;
    }

    public void cursorToEnd() {
        cursorIndex = Math.max(0, entries.size() - 1);
        adjustScroll();
    }

    public void enter() {
        var entry = selectedEntry();
        if (entry == null) {
            return;
        }

        if (entry.name().equals("..")) {
            navigateUp();
        } else if (entry.isDirectory()) {
            navigateTo(currentDirectory.resolve(entry.name()));
        }
    }

    public void navigateUp() {
        var parent = currentDirectory.getParent();
        if (parent != null) {
            var previousDir = currentDirectory.getFileName().toString();
            navigateTo(parent);
            // Try to position cursor on the directory we came from
            for (int i = 0; i < entries.size(); i++) {
                if (entries.get(i).name().equals(previousDir)) {
                    cursorIndex = i;
                    adjustScroll();
                    break;
                }
            }
        }
    }

    public void navigateTo(Path directory) {
        var normalized = directory.toAbsolutePath().normalize();
        if (Files.isDirectory(normalized) && Files.isReadable(normalized)) {
            currentDirectory = normalized;
            markedFiles.clear();
            cursorIndex = 0;
            scrollOffset = 0;
            refresh();
        }
    }

    public void refresh() {
        entries.clear();

        // Add parent directory entry if not at root
        if (currentDirectory.getParent() != null) {
            entries.add(FileEntry.parentDir());
        }

        try (var stream = Files.list(currentDirectory)) {
            var fileEntries = stream
                .map(this::createEntry)
                .sorted(Comparator
                    .comparing((FileEntry e) -> !e.isDirectory())  // Directories first
                    .thenComparing(e -> e.name().toLowerCase()))   // Then alphabetically
                .toList();
            entries.addAll(fileEntries);
        } catch (IOException e) {
            // Directory not readable, keep it empty
        }

        // Ensure cursor is valid
        if (cursorIndex >= entries.size()) {
            cursorIndex = Math.max(0, entries.size() - 1);
        }
        adjustScroll();
    }

    public void toggleMark() {
        var entry = selectedEntry();
        if (entry == null || entry.name().equals("..")) {
            return;
        }

        if (markedFiles.contains(entry.name())) {
            markedFiles.remove(entry.name());
        } else {
            markedFiles.add(entry.name());
        }
        cursorDown(); // Move to next file after marking
    }

    public void markAll() {
        for (var entry : entries) {
            if (!entry.name().equals("..")) {
                markedFiles.add(entry.name());
            }
        }
    }

    public void unmarkAll() {
        markedFiles.clear();
    }

    public void invertMarks() {
        for (var entry : entries) {
            if (entry.name().equals("..")) {
                continue;
            }
            if (markedFiles.contains(entry.name())) {
                markedFiles.remove(entry.name());
            } else {
                markedFiles.add(entry.name());
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════════

    private FileEntry createEntry(Path path) {
        try {
            var name = path.getFileName().toString();
            var isDir = Files.isDirectory(path);
            var size = isDir ? 0 : Files.size(path);
            var readable = Files.isReadable(path);
            var writable = Files.isWritable(path);
            return new FileEntry(name, isDir, size, readable, writable);
        } catch (IOException e) {
            return new FileEntry(path.getFileName().toString(), false, 0, false, false);
        }
    }

    private void adjustScroll() {
        // Ensure cursor is visible
        if (cursorIndex < scrollOffset) {
            scrollOffset = cursorIndex;
        } else if (cursorIndex >= scrollOffset + visibleRows) {
            scrollOffset = cursorIndex - visibleRows + 1;
        }
    }
}
