/*
 * Copyright TamboUI Contributors
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

/**
 * Manages browsing a directory: navigation, selection, and marking files for operations.
 * This is a pure state holder with no UI dependencies.
 */
public final class DirectoryBrowserController {

    /**
     * Represents a file entry.
     */
    public static final class FileEntry {
        private final String name;
        private final boolean isDirectory;
        private final long size;
        private final boolean isReadable;
        private final boolean isWritable;

        /**
         * Creates a file entry
         * @param name the file name
         * @param isDirectory whether it's a directory
         * @param size the file size
         * @param isReadable readable flag
         * @param isWritable writable flag
         */
        public FileEntry(String name, boolean isDirectory, long size, boolean isReadable, boolean isWritable) {
            this.name = name;
            this.isDirectory = isDirectory;
            this.size = size;
            this.isReadable = isReadable;
            this.isWritable = isWritable;
        }

        /**
         * Returns the file name as shown in the listing.
         * @return the entry name
         */
        public String name() {
            return name;
        }

        /**
         * Returns true when this entry represents a directory.
         * @return true if directory
         */
        public boolean isDirectory() {
            return isDirectory;
        }

        /**
         * Returns the size of the entry in bytes. For directories this is 0.
         * @return size in bytes
         */
        public long size() {
            return size;
        }

        /**
         * Returns whether the entry is readable.
         * @return true if readable
         */
        public boolean isReadable() {
            return isReadable;
        }

        /**
         * Returns whether the entry is writable.
         * @return true if writable
         */
        public boolean isWritable() {
            return isWritable;
        }

        /**
         * Creates a pseudo-entry representing the parent directory ("..").
         * @return a FileEntry for the parent directory
         */
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

    /**
     * Creates a controller for a particular start directory
     * @param startDirectory the start directory
     */
    public DirectoryBrowserController(Path startDirectory) {
        this.currentDirectory = startDirectory.toAbsolutePath().normalize();
        refresh();
    }

    // ═══════════════════════════════════════════════════════════════
    // QUERIES - Read state
    // ═══════════════════════════════════════════════════════════════

    /**
     * Returns the absolute current directory path the controller points to.
     * @return current directory path
     */
    public Path currentDirectory() {
        return currentDirectory;
    }

    /**
     * Returns an unmodifiable list of entries in the current directory.
     * @return list of FileEntry objects
     */
    public List<FileEntry> entries() {
        return Collections.unmodifiableList(new ArrayList<>(entries));
    }

    /**
     * Returns the index of the currently selected entry within entries().
     * @return cursor index, zero-based
     */
    public int cursorIndex() {
        return cursorIndex;
    }

    /**
     * Returns the current vertical scroll offset (first visible row index).
     * @return scroll offset
     */
    public int scrollOffset() {
        return scrollOffset;
    }

    /**
     * Returns the currently selected entry or null if the directory is empty.
     * @return selected FileEntry or null
     */
    public FileEntry selectedEntry() {
        if (entries.isEmpty()) {
            return null;
        }
        return entries.get(cursorIndex);
    }

    /**
     * Returns the resolved Path of the selected entry, or null when none is selected
     * or when the selection is the parent-directory pseudo-entry ("..").
     * @return Path of the selected entry or null
     */
    public Path selectedPath() {
        FileEntry entry = selectedEntry();
        if (entry == null || entry.name().equals("..")) {
            return null;
        }
        return currentDirectory.resolve(entry.name());
    }

    /**
     * Checks whether the given entry name is marked.
     * @param name the entry name to check
     * @return true if marked
     */
    public boolean isMarked(String name) {
        return markedFiles.contains(name);
    }

    /**
     * Returns an unmodifiable set of marked entry names.
     * @return set of marked names
     */
    public Set<String> markedFiles() {
        return Collections.unmodifiableSet(new HashSet<>(markedFiles));
    }

    /**
     * Returns a list of absolute Paths corresponding to marked entries.
     * @return list of Paths for marked entries
     */
    public List<Path> markedPaths() {
        return markedFiles.stream()
            .map(name -> currentDirectory.resolve(name))
            .collect(Collectors.toList());
    }

    /**
     * Returns true if any entries are currently marked.
     * @return true when there is at least one mark
     */
    public boolean hasMarkedFiles() {
        return !markedFiles.isEmpty();
    }

    /**
     * Returns the number of marked entries.
     * @return count of marked entries
     */
    public int markedCount() {
        return markedFiles.size();
    }

    /**
     * Returns the configured number of visible rows used for paging.
     * @return visible row count
     */
    public int visibleRows() {
        return visibleRows;
    }

    // ═══════════════════════════════════════════════════════════════
    // COMMANDS - Modify state
    // ═══════════════════════════════════════════════════════════════

    /**
     * Sets the maximum number of visible rows used for paging and scrolling.
     * The value will be clamped to at least 1.
     * @param rows row count
     */
    public void setVisibleRows(int rows) {
        this.visibleRows = Math.max(1, rows);
        adjustScroll();
    }

    /**
     * Moves the cursor up by one entry if possible.
     */
    public void cursorUp() {
        if (cursorIndex > 0) {
            cursorIndex--;
            adjustScroll();
        }
    }

    /**
     * Moves the cursor down by one entry if possible.
     */
    public void cursorDown() {
        if (cursorIndex < entries.size() - 1) {
            cursorIndex++;
            adjustScroll();
        }
    }

    /**
     * Moves the cursor up by one page (visibleRows).
     */
    public void pageUp() {
        cursorIndex = Math.max(0, cursorIndex - visibleRows);
        adjustScroll();
    }

    /**
     * Moves the cursor down by one page (visibleRows).
     */
    public void pageDown() {
        cursorIndex = Math.min(entries.size() - 1, cursorIndex + visibleRows);
        adjustScroll();
    }

    /**
     * Moves the cursor to the first entry and resets scroll to top.
     */
    public void cursorToStart() {
        cursorIndex = 0;
        scrollOffset = 0;
    }

    /**
     * Moves the cursor to the last entry and adjusts scroll accordingly.
     */
    public void cursorToEnd() {
        cursorIndex = Math.max(0, entries.size() - 1);
        adjustScroll();
    }

    /**
     * Activates the selected entry: navigate into directories or up if selecting "..".
     */
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

    /**
     * Navigate to the parent directory, attempting to position the cursor on the
     * directory we came from when possible.
     */
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

    /**
     * Moves to a new directory if it is a readable directory. Clears marks and resets cursor/scroll.
     * @param directory the target directory
     */
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

    /**
     * Refreshes the file listing for the current directory, including a parent entry if applicable.
     * If the directory cannot be read, the listing will be left empty.
     */
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

    /**
     * Toggles the mark state for the currently selected entry (skips parent "..").
     * After toggling, the cursor moves to the next entry.
     */
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

    /**
     * Marks all entries except the parent "..".
     */
    public void markAll() {
        for (FileEntry entry : entries) {
            if (!entry.name().equals("..")) {
                markedFiles.add(entry.name());
            }
        }
    }

    /**
     * Unmarks all entries.
     */
    public void unmarkAll() {
        markedFiles.clear();
    }

    /**
     * Inverts the mark state of all entries except the parent "..".
     */
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
