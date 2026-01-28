/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo.filemanager;

import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;

/**
 * Handles keyboard events and dispatches to the FileManager.
 * Translates key presses into controller commands.
 * <p>
 * Note: Input dialogs (mkdir, goto) are handled by DialogElement's modal behavior.
 * This handler only deals with confirmation dialogs and browser navigation.
 */
public class FileManagerKeyHandler {

    private final FileManagerController manager;

    /**
     * Creates a new FileManagerKeyHandler.
     * @param manager the file manager controller
     */
    public FileManagerKeyHandler(FileManagerController manager) {
        this.manager = manager;
    }

    /**
     * Handles a key event.
     * @param event the key event
     * @return the event result
     */
    public EventResult handle(KeyEvent event) {
        // Confirmation dialog mode (y/n dialogs)
        if (manager.hasDialog() && !manager.isInputDialog()) {
            return handleDialogKey(event);
        }

        return handleBrowserKey(event);
    }

    private EventResult handleDialogKey(KeyEvent event) {
        FileManagerController.DialogType dialogType = manager.currentDialog();

        if (dialogType == FileManagerController.DialogType.ERROR) {
            if (event.isSelect() || event.isCancel()) {
                manager.dismissDialog();
                return EventResult.HANDLED;
            }
            return EventResult.UNHANDLED;
        }

        // Confirmation dialogs
        if (event.isCharIgnoreCase('y')) {
            manager.confirmDialog();
            return EventResult.HANDLED;
        }
        if (event.isCharIgnoreCase('n') || event.isCancel()) {
            manager.dismissDialog();
            return EventResult.HANDLED;
        }

        return EventResult.UNHANDLED;
    }

    private EventResult handleBrowserKey(KeyEvent event) {
        DirectoryBrowserController browser = manager.activeBrowser();

        // Navigation
        if (event.isUp()) {
            browser.cursorUp();
            return EventResult.HANDLED;
        }
        if (event.isDown()) {
            browser.cursorDown();
            return EventResult.HANDLED;
        }
        if (event.isPageUp()) {
            browser.pageUp();
            return EventResult.HANDLED;
        }
        if (event.isPageDown()) {
            browser.pageDown();
            return EventResult.HANDLED;
        }
        if (event.isHome()) {
            browser.cursorToStart();
            return EventResult.HANDLED;
        }
        if (event.isEnd()) {
            browser.cursorToEnd();
            return EventResult.HANDLED;
        }

        // Enter directory or open file (Enter only, not Space)
        if (event.isConfirm()) {
            browser.enter();
            return EventResult.HANDLED;
        }

        // Go to parent directory
        if (event.isKey(KeyCode.BACKSPACE)) {
            browser.navigateUp();
            return EventResult.HANDLED;
        }

        // Switch between panels (Tab is handled by focus system, left/right for quick switch)
        if (event.isLeft()) {
            manager.setActiveSide(FileManagerController.Side.LEFT);
            return EventResult.HANDLED;
        }
        if (event.isRight()) {
            manager.setActiveSide(FileManagerController.Side.RIGHT);
            return EventResult.HANDLED;
        }

        // Marking files
        if (event.isKey(KeyCode.INSERT) || event.isChar(' ')) {
            browser.toggleMark();
            return EventResult.HANDLED;
        }
        if (event.isChar('+')) {
            browser.markAll();
            return EventResult.HANDLED;
        }
        if (event.isChar('-')) {
            browser.unmarkAll();
            return EventResult.HANDLED;
        }
        if (event.isChar('*')) {
            browser.invertMarks();
            return EventResult.HANDLED;
        }

        // File operations (F5, F6, F7, F8)
        if (event.isKey(KeyCode.F5) || event.isCharIgnoreCase('c')) {
            manager.promptCopy();
            return EventResult.HANDLED;
        }
        if (event.isKey(KeyCode.F6) || event.isCharIgnoreCase('m')) {
            manager.promptMove();
            return EventResult.HANDLED;
        }
        if (event.isKey(KeyCode.F7) || event.isCharIgnoreCase('n')) {
            manager.promptMkdir();
            return EventResult.HANDLED;
        }
        if (event.isKey(KeyCode.F8) || event.isCharIgnoreCase('d')) {
            manager.promptDelete();
            return EventResult.HANDLED;
        }

        // Refresh
        if (event.isCharIgnoreCase('r')) {
            manager.leftBrowser().refresh();
            manager.rightBrowser().refresh();
            return EventResult.HANDLED;
        }

        // Go to directory
        if (event.isCharIgnoreCase('o')) {
            manager.promptGoto();
            return EventResult.HANDLED;
        }

        // View file
        if (event.isCharIgnoreCase('v')) {
            manager.promptViewFile();
            return EventResult.HANDLED;
        }

        return EventResult.UNHANDLED;
    }
}
