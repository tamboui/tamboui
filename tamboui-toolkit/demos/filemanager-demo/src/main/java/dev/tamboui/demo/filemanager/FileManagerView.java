/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo.filemanager;

import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.elements.DialogElement;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.event.KeyEvent;

import static dev.tamboui.toolkit.Toolkit.*;

/**
 * Main view implementing Element directly for proper event handling.
 * This follows the pattern used by FloatingPanelsArea in toolkit-demo.
 */
public class FileManagerView implements Element {

    private final FileManagerController manager;
    private final FileManagerKeyHandler keyHandler;
    private DialogElement currentDialog;

    public FileManagerView(FileManagerController manager) {
        this.manager = manager;
        this.keyHandler = new FileManagerKeyHandler(manager);
    }

    @Override
    public void render(Frame frame, Rect area, RenderContext context) {

        // Build and render the UI tree
        var ui = column(
                header(),
                browserRow(context),
                helpBar()
        );
        ui.render(frame, area, context);

        // Render dialog on top if present
        if (manager.hasDialog()) {
            renderDialog(frame, area, context);
        } else {
            currentDialog = null;
        }
    }

    private void renderDialog(Frame frame, Rect area, RenderContext context) {
        var type = manager.currentDialog();
        var message = manager.dialogMessage();

        // Handle input dialogs separately
        if (type == FileManagerController.DialogType.MKDIR_INPUT) {
            currentDialog = createInputDialog("New Directory", message, manager::confirmMkdir);
            currentDialog.render(frame, area, context);
            return;
        }
        if (type == FileManagerController.DialogType.GOTO_INPUT) {
            currentDialog = createInputDialog("Go To Directory", message, manager::confirmGoto);
            currentDialog.render(frame, area, context);
            return;
        }

        var titleColor = switch (type) {
            case ERROR -> Color.RED;
            case DELETE_CONFIRM -> Color.YELLOW;
            default -> Color.CYAN;
        };

        var title = switch (type) {
            case COPY_CONFIRM -> "Copy";
            case MOVE_CONFIRM -> "Move";
            case DELETE_CONFIRM -> "Delete";
            case ERROR -> "Error";
            default -> "";
        };

        // Confirmation dialogs don't use Enter for confirm (they use y/n)
        // so we just create a simple dialog without onConfirm
        currentDialog = dialog(title,
                text(message),
                text(""),
                text(type == FileManagerController.DialogType.ERROR
                        ? "[Enter] OK"
                        : "[y] Yes  [n] No  [Esc] Cancel").dim()
        ).rounded()
                .borderColor(titleColor)
                .width(Math.max(40, message.length() + 4))
                .onCancel(manager::dismissDialog);

        // For error dialogs, Enter also dismisses
        if (type == FileManagerController.DialogType.ERROR) {
            currentDialog.onConfirm(manager::dismissDialog);
        }

        currentDialog.render(frame, area, context);
    }

    private DialogElement createInputDialog(String title, String prompt, Runnable onConfirm) {
        return dialog(title,
                text(prompt),
                textInput(manager.inputState()).cursorStyle(dev.tamboui.style.Style.EMPTY.fg(Color.CYAN).reversed()),
                text("[Enter] Confirm  [Esc] Cancel").dim()
        ).rounded()
                .borderColor(Color.CYAN)
                .width(Math.max(50, prompt.length() + 4))
                .onConfirm(onConfirm)
                .onCancel(manager::dismissDialog);
    }

    @Override
    public Constraint constraint() {
        return Constraint.fill();
    }

    @Override
    public EventResult handleKeyEvent(KeyEvent event, boolean focused) {
        // Route to input dialog if one is present (modal behavior with text input)
        if (currentDialog != null && manager.isInputDialog()) {
            return currentDialog.handleKeyEvent(event, true);
        }
        // For confirmation dialogs and browser, use the key handler
        return keyHandler.handle(event);
    }

    private Element header() {
        return row(
                text(" File Manager ").bold().cyan(),
                spacer(),
                text(" [Tab] Switch ").dim(),
                text(" [Space] Mark ").dim(),
                text(" [F5] Copy ").dim(),
                text(" [F6] Move ").dim(),
                text(" [F7] Mkdir ").dim(),
                text(" [F8] Delete ").dim(),
                text(" [o] Goto ").dim(),
                text(" [q] Quit ").dim()
        ).length(1);
    }

    private Element browserRow(RenderContext context) {
        var leftActive = manager.isActive(FileManagerController.Side.LEFT);
        var rightActive = manager.isActive(FileManagerController.Side.RIGHT);

        return row(
                browserPanel(manager.leftBrowser(), leftActive, "left", context),
                browserPanel(manager.rightBrowser(), rightActive, "right", context)
        ).fill();
    }

    private Element browserPanel(DirectoryBrowserController browser, boolean active, String id, RenderContext context) {
        // Create a wrapper element that sets visible rows based on actual available height
        Element fileListWrapper = new Element() {
            @Override
            public void render(Frame frame, Rect area, RenderContext ctx) {
                // Update visible rows based on actual available height
                browser.setVisibleRows(area.height());
                // Now render the file list with correct visible count
                fileList(browser, active).render(frame, area, ctx);
            }

            @Override
            public Constraint constraint() {
                return Constraint.fill();
            }
        };

        return panel(browser.currentDirectory().toString(), fileListWrapper)
                .id(id)
                .rounded()
                .titleEllipsisStart()
                .borderColor(active ? Color.CYAN : Color.DARK_GRAY)
                .fill();
    }

    private Element fileList(DirectoryBrowserController browser, boolean active) {
        var entries = browser.entries();
        var offset = browser.scrollOffset();
        var cursor = browser.cursorIndex();
        var visibleCount = Math.min(browser.visibleRows(), Math.max(0, entries.size() - offset));

        if (visibleCount == 0) {
            return text("Empty").dim().fill();
        }

        var elements = new Element[visibleCount];
        for (int i = 0; i < visibleCount; i++) {
            var entryIndex = offset + i;
            var entry = entries.get(entryIndex);
            var isSelected = entryIndex == cursor;
            elements[i] = fileEntry(browser, entry, isSelected, active);
        }

        return column(elements).fill();
    }

    private Element fileEntry(DirectoryBrowserController browser, DirectoryBrowserController.FileEntry entry, boolean selected, boolean active) {
        var name = entry.name();
        var isMarked = browser.isMarked(name);
        var marker = isMarked ? "*" : " ";
        var displayName = entry.isDirectory() && !name.equals("..") ? name + "/" : name;
        var size = entry.isDirectory() ? "<DIR>" : formatSize(entry.size());

        // Combine marker + space + name into single left-aligned text
        var fullName = marker + " " + displayName;

        // Determine colors based on state
        Color fg, bg;
        if (selected && active) {
            fg = Color.BLACK;
            bg = Color.WHITE;
        } else if (selected) {
            fg = Color.WHITE;
            bg = Color.rgb(60, 60, 60);
        } else {
            fg = isMarked ? Color.YELLOW
                    : entry.name().equals("..") ? Color.BLUE
                    : entry.isDirectory() ? Color.CYAN
                    : Color.WHITE;
            bg = null;
        }

        var nameText = text(fullName).fg(fg).ellipsis().fill();
        var sizeText = text(" " + size).fg(selected ? fg : Color.GRAY);

        if (bg != null) {
            return row(nameText.bg(bg), sizeText.bg(bg)).length(1);
        }
        return row(nameText, sizeText).length(1);
    }

    private Element helpBar() {
        var browser = manager.activeBrowser();
        var entry = browser.selectedEntry();

        String info;
        if (entry == null) {
            info = "Empty directory";
        } else if (entry.isDirectory()) {
            info = entry.name().equals("..") ? "Parent directory" : "Directory: " + entry.name();
        } else {
            info = String.format("%s (%s)", entry.name(), formatSize(entry.size()));
        }

        return row(
                text(info).fill(),
                text("[Enter] Open  [Backspace] Up  [+] Mark All  [-] Unmark").dim()
        ).length(1);
    }

    private static String formatSize(long size) {
        if (size < 1024) {
            return size + " B";
        }
        if (size < 1024 * 1024) {
            return String.format("%.1f K", size / 1024.0);
        }
        if (size < 1024 * 1024 * 1024) {
            return String.format("%.1f M", size / (1024.0 * 1024));
        }
        return String.format("%.1f G", size / (1024.0 * 1024 * 1024));
    }
}
