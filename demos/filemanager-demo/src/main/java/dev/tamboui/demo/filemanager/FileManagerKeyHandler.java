/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo.filemanager;

import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.Keys;
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

    public FileManagerKeyHandler(FileManagerController manager) {
        this.manager = manager;
    }

    public EventResult handle(KeyEvent event) {
        // Confirmation dialog mode (y/n dialogs)
        if (manager.hasDialog() && !manager.isInputDialog()) {
            return handleDialogKey(event);
        }

        return handleBrowserKey(event);
    }

    private EventResult handleDialogKey(KeyEvent event) {
        var dialogType = manager.currentDialog();

        if (dialogType == FileManagerController.DialogType.ERROR) {
            if (Keys.isSelect(event) || Keys.isEscape(event)) {
                manager.dismissDialog();
                return EventResult.HANDLED;
            }
            return EventResult.UNHANDLED;
        }

        // Confirmation dialogs
        if (Keys.isChar(event, 'y') || Keys.isChar(event, 'Y')) {
            manager.confirmDialog();
            return EventResult.HANDLED;
        }
        if (Keys.isChar(event, 'n') || Keys.isChar(event, 'N') || Keys.isEscape(event)) {
            manager.dismissDialog();
            return EventResult.HANDLED;
        }

        return EventResult.UNHANDLED;
    }

    private EventResult handleBrowserKey(KeyEvent event) {
        var browser = manager.activeBrowser();

        // Navigation
        if (Keys.isUp(event)) {
            browser.cursorUp();
            return EventResult.HANDLED;
        }
        if (Keys.isDown(event)) {
            browser.cursorDown();
            return EventResult.HANDLED;
        }
        if (Keys.isPageUp(event)) {
            browser.pageUp();
            return EventResult.HANDLED;
        }
        if (Keys.isPageDown(event)) {
            browser.pageDown();
            return EventResult.HANDLED;
        }
        if (Keys.isHome(event)) {
            browser.cursorToStart();
            return EventResult.HANDLED;
        }
        if (Keys.isEnd(event)) {
            browser.cursorToEnd();
            return EventResult.HANDLED;
        }

        // Enter directory or open file (Enter only, not Space)
        if (Keys.isEnter(event)) {
            browser.enter();
            return EventResult.HANDLED;
        }

        // Go to parent directory
        if (event.code() == KeyCode.BACKSPACE) {
            browser.navigateUp();
            return EventResult.HANDLED;
        }

        // Switch between panels
        if (Keys.isTab(event) || Keys.isBackTab(event)) {
            manager.switchSide();
            return EventResult.HANDLED;
        }
        if (Keys.isLeft(event)) {
            manager.setActiveSide(FileManagerController.Side.LEFT);
            return EventResult.HANDLED;
        }
        if (Keys.isRight(event)) {
            manager.setActiveSide(FileManagerController.Side.RIGHT);
            return EventResult.HANDLED;
        }

        // Marking files
        if (event.code() == KeyCode.INSERT || Keys.isChar(event, ' ')) {
            browser.toggleMark();
            return EventResult.HANDLED;
        }
        if (Keys.isChar(event, '+')) {
            browser.markAll();
            return EventResult.HANDLED;
        }
        if (Keys.isChar(event, '-')) {
            browser.unmarkAll();
            return EventResult.HANDLED;
        }
        if (Keys.isChar(event, '*')) {
            browser.invertMarks();
            return EventResult.HANDLED;
        }

        // File operations (F5, F6, F7, F8)
        if (event.code() == KeyCode.F5 || Keys.isChar(event, 'c') || Keys.isChar(event, 'C')) {
            manager.promptCopy();
            return EventResult.HANDLED;
        }
        if (event.code() == KeyCode.F6 || Keys.isChar(event, 'm') || Keys.isChar(event, 'M')) {
            manager.promptMove();
            return EventResult.HANDLED;
        }
        if (event.code() == KeyCode.F7 || Keys.isChar(event, 'n') || Keys.isChar(event, 'N')) {
            manager.promptMkdir();
            return EventResult.HANDLED;
        }
        if (event.code() == KeyCode.F8 || Keys.isChar(event, 'd') || Keys.isChar(event, 'D')) {
            manager.promptDelete();
            return EventResult.HANDLED;
        }

        // Refresh
        if (Keys.isChar(event, 'r') || Keys.isChar(event, 'R')) {
            manager.leftBrowser().refresh();
            manager.rightBrowser().refresh();
            return EventResult.HANDLED;
        }

        // Go to directory
        if (Keys.isChar(event, 'o') || Keys.isChar(event, 'O')) {
            manager.promptGoto();
            return EventResult.HANDLED;
        }

        return EventResult.UNHANDLED;
    }
}
