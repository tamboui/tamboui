/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo.filemanager;

import dev.tamboui.widgets.input.TextInputState;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Main application state coordinating two directory browsers and file operations.
 * Manages which browser is active and handles copy/move/delete operations.
 */
public class FileManagerController {

    /**
     * Which side is active (left or right).
     */
    public enum Side {
        /** Left pane (typically the first/primary browser). */
        LEFT,
        /** Right pane (typically the second/secondary browser). */
        RIGHT
    }

    /**
     * Types of dialogs that can be shown by the file manager.
     */
    public enum DialogType {
        /** No dialog is currently shown. */
        NONE,
        /** Confirmation dialog asking to copy items. */
        COPY_CONFIRM,
        /** Confirmation dialog asking to move items. */
        MOVE_CONFIRM,
        /** Confirmation dialog asking to delete items. */
        DELETE_CONFIRM,
        /** Input dialog for creating a new directory. */
        MKDIR_INPUT,
        /** Input dialog for moving/go-to a specific directory. */
        GOTO_INPUT,
        /** Dialog showing an error message. */
        ERROR,
        /** Dialog used to view a file's contents. */
        VIEW_FILE
    }

    private final DirectoryBrowserController leftBrowser;
    private final DirectoryBrowserController rightBrowser;
    private Side activeSide = Side.LEFT;

    private DialogType currentDialog = DialogType.NONE;
    private String dialogMessage = "";
    private final TextInputState inputState = new TextInputState();
    private Path viewingFile;
    private int textScrollPosition = 0;

    /**
     * Creates a new FileManagerController with the given starting directories for the left and right browsers.
     *
     * @param leftStart  starting path for the left browser
     * @param rightStart starting path for the right browser
     */
    public FileManagerController(Path leftStart, Path rightStart) {
        this.leftBrowser = new DirectoryBrowserController(leftStart);
        this.rightBrowser = new DirectoryBrowserController(rightStart);
    }

    // ═══════════════════════════════════════════════════════════════
    // QUERIES - Read state
    // ═══════════════════════════════════════════════════════════════

    /**
     * Returns the left directory browser controller.
     *
     * @return the left DirectoryBrowserController
     */
    public DirectoryBrowserController leftBrowser() {
        return leftBrowser;
    }

    /**
     * Returns the right directory browser controller.
     *
     * @return the right DirectoryBrowserController
     */
    public DirectoryBrowserController rightBrowser() {
        return rightBrowser;
    }

    /**
     * Returns the currently active browser (the one that receives commands).
     *
     * @return the active DirectoryBrowserController
     */
    public DirectoryBrowserController activeBrowser() {
        return activeSide == Side.LEFT ? leftBrowser : rightBrowser;
    }

    /**
     * Returns the inactive browser (the opposite of the active one).
     *
     * @return the inactive DirectoryBrowserController
     */
    public DirectoryBrowserController inactiveBrowser() {
        return activeSide == Side.LEFT ? rightBrowser : leftBrowser;
    }

    /**
     * Checks whether the given side (LEFT or RIGHT) is currently active.
     *
     * @param side the Side to check
     * @return true if the provided side is active
     */
    public boolean isActive(Side side) {
        return activeSide == side;
    }

    /**
     * Returns the current dialog type.
     *
     * @return current DialogType
     */
    public DialogType currentDialog() {
        return currentDialog;
    }

    /**
     * Returns the current dialog message text.
     *
     * @return dialog message
     */
    public String dialogMessage() {
        return dialogMessage;
    }

    /**
     * Returns the text input state used for input dialogs.
     *
     * @return the TextInputState instance backing input dialogs
     */
    public TextInputState inputState() {
        return inputState;
    }

    /**
     * Returns whether any dialog is currently displayed.
     *
     * @return true when a dialog other than NONE is active
     */
    public boolean hasDialog() {
        return currentDialog != DialogType.NONE;
    }

    /**
     * Returns whether the active dialog is an input dialog (mkdir or goto).
     *
     * @return true if the current dialog expects textual input
     */
    public boolean isInputDialog() {
        return currentDialog == DialogType.MKDIR_INPUT || currentDialog == DialogType.GOTO_INPUT;
    }

    /**
     * Returns whether the active dialog is a file viewer.
     *
     * @return true if the current dialog is VIEW_FILE
     */
    public boolean isViewerDialog() {
        return currentDialog == DialogType.VIEW_FILE;
    }

    /**
     * Returns the Path of the file currently being viewed, or null if none.
     *
     * @return viewing file Path or null
     */
    public Path viewingFile() {
        return viewingFile;
    }

    /**
     * Returns the current vertical scroll position in the file viewer.
     *
     * @return text scroll position (zero-based line index)
     */
    public int textScrollPosition() {
        return textScrollPosition;
    }

    /**
     * Sets the text viewer scroll position, clamped to zero or greater.
     *
     * @param position desired scroll position
     */
    public void setTextScrollPosition(int position) {
        this.textScrollPosition = Math.max(0, position);
    }

    /**
     * Scrolls the file viewer one line up.
     */
    public void scrollTextUp() {
        textScrollPosition = Math.max(0, textScrollPosition - 1);
    }

    /**
     * Scrolls the file viewer one line down.
     */
    public void scrollTextDown() {
        textScrollPosition = Math.max(0, textScrollPosition + 1);
    }

    /**
     * Scrolls the file viewer up by a page (pageSize lines).
     *
     * @param pageSize number of lines to move up
     */
    public void scrollTextPageUp(int pageSize) {
        textScrollPosition = Math.max(0, textScrollPosition - pageSize);
    }

    /**
     * Scrolls the file viewer down by a page (pageSize lines).
     *
     * @param pageSize number of lines to move down
     */
    public void scrollTextPageDown(int pageSize) {
        textScrollPosition = Math.max(0, textScrollPosition + pageSize);
    }

    /**
     * Returns the list of files to operate on: the marked files (if any), otherwise the currently selected file.
     *
     * @return list of Paths to operate on (may be empty)
     */
    public List<Path> filesToOperate() {
        DirectoryBrowserController browser = activeBrowser();
        if (browser.hasMarkedFiles()) {
            return browser.markedPaths();
        }
        Path selected = browser.selectedPath();
        if (selected != null) {
            return Collections.singletonList(selected);
        }
        return Collections.emptyList();
    }

    // ═══════════════════════════════════════════════════════════════
    // COMMANDS - Navigation
    // ═══════════════════════════════════════════════════════════════

    /**
     * Switches the active side to the other pane.
     */
    public void switchSide() {
        activeSide = (activeSide == Side.LEFT) ? Side.RIGHT : Side.LEFT;
    }

    /**
     * Sets the active side explicitly.
     *
     * @param side the Side to activate
     */
    public void setActiveSide(Side side) {
        activeSide = side;
    }

    // ═══════════════════════════════════════════════════════════════
    // COMMANDS - Dialog management
    // ═══════════════════════════════════════════════════════════════

    /**
     * Prompts a confirmation dialog to copy the selected/marked items to the inactive pane.
     * If there are no files to operate on, this does nothing.
     */
    public void promptCopy() {
        List<Path> files = filesToOperate();
        if (files.isEmpty()) {
            return;
        }

        int count = files.size();
        Path target = inactiveBrowser().currentDirectory();
        dialogMessage = String.format("Copy %d item%s to %s?",
            count, count > 1 ? "s" : "", target);
        currentDialog = DialogType.COPY_CONFIRM;
    }

    /**
     * Prompts a confirmation dialog to move the selected/marked items to the inactive pane.
     * If there are no files to operate on, this does nothing.
     */
    public void promptMove() {
        List<Path> files = filesToOperate();
        if (files.isEmpty()) {
            return;
        }

        int count = files.size();
        Path target = inactiveBrowser().currentDirectory();
        dialogMessage = String.format("Move %d item%s to %s?",
            count, count > 1 ? "s" : "", target);
        currentDialog = DialogType.MOVE_CONFIRM;
    }

    /**
     * Prompts a confirmation dialog to delete the selected/marked items permanently.
     * If there are no files to operate on, this does nothing.
     */
    public void promptDelete() {
        List<Path> files = filesToOperate();
        if (files.isEmpty()) {
            return;
        }

        int count = files.size();
        dialogMessage = String.format("Delete %d item%s permanently?",
            count, count > 1 ? "s" : "");
        currentDialog = DialogType.DELETE_CONFIRM;
    }

    /**
     * Confirms and executes the current confirmation dialog (copy/move/delete) if applicable.
     * After execution the dialog is dismissed.
     */
    public void confirmDialog() {
        switch (currentDialog) {
            case COPY_CONFIRM:
                executeCopy();
                break;
            case MOVE_CONFIRM:
                executeMove();
                break;
            case DELETE_CONFIRM:
                executeDelete();
                break;
            default:
                break;
        }
        dismissDialog();
    }

    /**
     * Dismisses any active dialog and clears related transient state (input, viewer, scroll).
     */
    public void dismissDialog() {
        currentDialog = DialogType.NONE;
        dialogMessage = "";
        inputState.clear();
        viewingFile = null;
        textScrollPosition = 0;
    }

    /**
     * Opens an input dialog for creating a new directory in the active pane.
     */
    public void promptMkdir() {
        inputState.clear();
        dialogMessage = "Create in: " + activeBrowser().currentDirectory();
        currentDialog = DialogType.MKDIR_INPUT;
    }

    /**
     * Confirms the mkdir input dialog and attempts to create the directory.
     * On success the active browser is refreshed, on failure an error dialog is shown.
     */
    public void confirmMkdir() {
        if (inputState.length() > 0) {
            Path targetDir = activeBrowser().currentDirectory().resolve(inputState.text());
            try {
                Files.createDirectory(targetDir);
                activeBrowser().refresh();
            } catch (IOException e) {
                showError("Failed to create directory: " + e.getMessage());
                return;
            }
        }
        dismissDialog();
    }

    /**
     * Opens an input dialog for going to a specific directory.
     * The input is prefilled with the current directory path.
     */
    public void promptGoto() {
        inputState.setText(activeBrowser().currentDirectory().toString());
        dialogMessage = "Go to directory:";
        currentDialog = DialogType.GOTO_INPUT;
    }

    /**
     * Confirms the goto input dialog and navigates the active browser to the provided path.
     * If the path is not a directory, an error dialog is shown.
     */
    public void confirmGoto() {
        if (inputState.length() > 0) {
            Path targetDir = Paths.get(inputState.text());
            if (Files.isDirectory(targetDir)) {
                activeBrowser().navigateTo(targetDir);
            } else {
                showError("Not a directory: " + inputState.text());
                return;
            }
        }
        dismissDialog();
    }

    /**
     * Shows an error dialog with the provided message.
     *
     * @param message the error message to display
     */
    public void showError(String message) {
        dialogMessage = message;
        currentDialog = DialogType.ERROR;
    }

    /**
     * Opens the file viewer for the currently selected file if it is not a directory.
     * If the selection is invalid or a directory, this does nothing.
     */
    public void promptViewFile() {
        Path selected = activeBrowser().selectedPath();
        if (selected == null || Files.isDirectory(selected)) {
            return;
        }
        viewingFile = selected;
        textScrollPosition = 0;
        currentDialog = DialogType.VIEW_FILE;
    }

    // ═══════════════════════════════════════════════════════════════
    // COMMANDS - File operations
    // ═══════════════════════════════════════════════════════════════

    private void executeCopy() {
        List<Path> files = filesToOperate();
        Path targetDir = inactiveBrowser().currentDirectory();

        for (Path source : files) {
            try {
                Path target = targetDir.resolve(source.getFileName());
                if (Files.isDirectory(source)) {
                    copyDirectoryRecursively(source, target);
                } else {
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                showError("Copy failed: " + e.getMessage());
                return;
            }
        }

        activeBrowser().unmarkAll();
        inactiveBrowser().refresh();
    }

    private void executeMove() {
        List<Path> files = filesToOperate();
        Path targetDir = inactiveBrowser().currentDirectory();

        for (Path source : files) {
            try {
                Path target = targetDir.resolve(source.getFileName());
                Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                showError("Move failed: " + e.getMessage());
                return;
            }
        }

        activeBrowser().unmarkAll();
        activeBrowser().refresh();
        inactiveBrowser().refresh();
    }

    private void executeDelete() {
        List<Path> files = filesToOperate();

        for (Path path : files) {
            try {
                if (Files.isDirectory(path)) {
                    deleteDirectoryRecursively(path);
                } else {
                    Files.delete(path);
                }
            } catch (IOException e) {
                showError("Delete failed: " + e.getMessage());
                return;
            }
        }

        activeBrowser().unmarkAll();
        activeBrowser().refresh();
    }

    private void copyDirectoryRecursively(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path targetDir = target.resolve(source.relativize(dir));
                Files.createDirectories(targetDir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path targetFile = target.resolve(source.relativize(file));
                Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void deleteDirectoryRecursively(Path directory) throws IOException {
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
