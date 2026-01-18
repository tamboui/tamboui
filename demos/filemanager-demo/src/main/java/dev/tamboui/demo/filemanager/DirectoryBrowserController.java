/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo.filemanager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/// Manages browsing a directory: navigation, selection, and marking files for operations.
/// This is a pure state holder with no UI dependencies.
public final class DirectoryBrowserController {

    public static final class FileEntry {
        private final String name;
        private final boolean isDirectory;
        private final long size;
        private final boolean isReadable;
        private final boolean isWritable;

        public FileEntry(String name, boolean isDirectory, long size, boolean isReadable, boolean isWritable) {
            this.name = name;
            this.isDirectory = isDirectory;
            this.size = size;
            this.isReadable = isReadable;
            this.isWritable = isWritable;
        }

        public String name() {
            return name;
        }

        public boolean isDirectory() {
            return isDirectory;
        }

        public long size() {
            return size;
        }

        public boolean isReadable() {
            return isReadable;
        }

        public boolean isWritable() {
            return isWritable;
        }

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
        return Collections.unmodifiableList(new ArrayList<>(entries));
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
        FileEntry entry = selectedEntry();
        if (entry == null || entry.name().equals("..")) {
            return null;
        }
        return currentDirectory.resolve(entry.name());
    }

    public boolean isMarked(String name) {
        return markedFiles.contains(name);
    }

    public Set<String> markedFiles() {
        return Collections.unmodifiableSet(new HashSet<>(markedFiles));
    }

    public List<Path> markedPaths() {
        return markedFiles.stream()
            .map(name -> currentDirectory.resolve(name))
            .collect(Collectors.toList());
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
        FileEntry entry = selectedEntry();
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
        Path parent = currentDirectory.getParent();
        if (parent != null) {
            String previousDir = currentDirectory.getFileName().toString();
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
        Path normalized = directory.toAbsolutePath().normalize();
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

        try (Stream<Path> stream = Files.list(currentDirectory)) {
            List<FileEntry> fileEntries = stream
                .map(this::createEntry)
                .sorted(Comparator
                    .comparing((FileEntry e) -> !e.isDirectory())  // Directories first
                    .thenComparing(e -> e.name().toLowerCase()))   // Then alphabetically
                .collect(Collectors.toList());
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
        FileEntry entry = selectedEntry();
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
        for (FileEntry entry : entries) {
            if (!entry.name().equals("..")) {
                markedFiles.add(entry.name());
            }
        }
    }

    public void unmarkAll() {
        markedFiles.clear();
    }

    public void invertMarks() {
        for (FileEntry entry : entries) {
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
            String name = path.getFileName().toString();
            boolean isDir = Files.isDirectory(path);
            long size = isDir ? 0 : Files.size(path);
            boolean readable = Files.isReadable(path);
            boolean writable = Files.isWritable(path);
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

