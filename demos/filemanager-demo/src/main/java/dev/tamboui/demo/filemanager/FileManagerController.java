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

    public enum Side { LEFT, RIGHT }

    public enum DialogType { NONE, COPY_CONFIRM, MOVE_CONFIRM, DELETE_CONFIRM, MKDIR_INPUT, GOTO_INPUT, ERROR, VIEW_FILE }

    private final DirectoryBrowserController leftBrowser;
    private final DirectoryBrowserController rightBrowser;
    private Side activeSide = Side.LEFT;

    private DialogType currentDialog = DialogType.NONE;
    private String dialogMessage = "";
    private final TextInputState inputState = new TextInputState();
    private Path viewingFile;
    private int textScrollPosition = 0;

    public FileManagerController(Path leftStart, Path rightStart) {
        this.leftBrowser = new DirectoryBrowserController(leftStart);
        this.rightBrowser = new DirectoryBrowserController(rightStart);
    }

    // ═══════════════════════════════════════════════════════════════
    // QUERIES - Read state
    // ═══════════════════════════════════════════════════════════════

    public DirectoryBrowserController leftBrowser() {
        return leftBrowser;
    }

    public DirectoryBrowserController rightBrowser() {
        return rightBrowser;
    }

    public DirectoryBrowserController activeBrowser() {
        return activeSide == Side.LEFT ? leftBrowser : rightBrowser;
    }

    public DirectoryBrowserController inactiveBrowser() {
        return activeSide == Side.LEFT ? rightBrowser : leftBrowser;
    }

    public boolean isActive(Side side) {
        return activeSide == side;
    }

    public DialogType currentDialog() {
        return currentDialog;
    }

    public String dialogMessage() {
        return dialogMessage;
    }

    public TextInputState inputState() {
        return inputState;
    }

    public boolean hasDialog() {
        return currentDialog != DialogType.NONE;
    }

    public boolean isInputDialog() {
        return currentDialog == DialogType.MKDIR_INPUT || currentDialog == DialogType.GOTO_INPUT;
    }

    public boolean isViewerDialog() {
        return currentDialog == DialogType.VIEW_FILE;
    }

    public Path viewingFile() {
        return viewingFile;
    }

    public int textScrollPosition() {
        return textScrollPosition;
    }

    public void setTextScrollPosition(int position) {
        this.textScrollPosition = Math.max(0, position);
    }

    public void scrollTextUp() {
        textScrollPosition = Math.max(0, textScrollPosition - 1);
    }

    public void scrollTextDown() {
        textScrollPosition = Math.max(0, textScrollPosition + 1);
    }

    public void scrollTextPageUp(int pageSize) {
        textScrollPosition = Math.max(0, textScrollPosition - pageSize);
    }

    public void scrollTextPageDown(int pageSize) {
        textScrollPosition = Math.max(0, textScrollPosition + pageSize);
    }

    /**
     * Returns files to operate on: marked files if any, otherwise the selected file.
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

    public void switchSide() {
        activeSide = (activeSide == Side.LEFT) ? Side.RIGHT : Side.LEFT;
    }

    public void setActiveSide(Side side) {
        activeSide = side;
    }

    // ═══════════════════════════════════════════════════════════════
    // COMMANDS - Dialog management
    // ═══════════════════════════════════════════════════════════════

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

    public void dismissDialog() {
        currentDialog = DialogType.NONE;
        dialogMessage = "";
        inputState.clear();
        viewingFile = null;
        textScrollPosition = 0;
    }

    public void promptMkdir() {
        inputState.clear();
        dialogMessage = "Create in: " + activeBrowser().currentDirectory();
        currentDialog = DialogType.MKDIR_INPUT;
    }

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

    public void promptGoto() {
        inputState.setText(activeBrowser().currentDirectory().toString());
        dialogMessage = "Go to directory:";
        currentDialog = DialogType.GOTO_INPUT;
    }

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

    public void showError(String message) {
        dialogMessage = message;
        currentDialog = DialogType.ERROR;
    }

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
