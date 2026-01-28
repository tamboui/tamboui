/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo.filemanager;

import dev.tamboui.image.Image;
import dev.tamboui.image.ImageData;
import dev.tamboui.image.ImageScaling;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.terminal.Frame;
import dev.tamboui.text.Text;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.elements.DialogElement;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.widgets.paragraph.Paragraph;
import dev.tamboui.style.Overflow;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static dev.tamboui.toolkit.Toolkit.*;

/**
 * Main view implementing Element directly for proper event handling.
 * This follows the pattern used by FloatingPanelsArea in toolkit-demo.
 */
public class FileManagerView implements Element {

    private final FileManagerController manager;
    private final FileManagerKeyHandler keyHandler;
    private DialogElement currentDialog;

    /**
     * Creates a new FileManagerView.
     * @param manager the file manager controller
     */
    public FileManagerView(FileManagerController manager) {
        this.manager = manager;
        this.keyHandler = new FileManagerKeyHandler(manager);
    }

    /**
     * Renders the file manager UI.
     * @param frame the frame to render to
     * @param area the area to render within
     * @param context the render context providing focus and state information
     */
    @Override
    public void render(Frame frame, Rect area, RenderContext context) {

        // Build and render the UI tree
        Element ui = column(
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
        FileManagerController.DialogType type = manager.currentDialog();
        String message = manager.dialogMessage();

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
        if (type == FileManagerController.DialogType.VIEW_FILE) {
            renderViewerDialog(frame, area, context);
            return;
        }

        Color titleColor;
        switch (type) {
            case ERROR:
                titleColor = Color.RED;
                break;
            case DELETE_CONFIRM:
                titleColor = Color.YELLOW;
                break;
            default:
                titleColor = Color.CYAN;
                break;
        }

        String title;
        switch (type) {
            case COPY_CONFIRM:
                title = "Copy";
                break;
            case MOVE_CONFIRM:
                title = "Move";
                break;
            case DELETE_CONFIRM:
                title = "Delete";
                break;
            case ERROR:
                title = "Error";
                break;
            default:
                title = "";
                break;
        }

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

    /**
     * Returns the layout constraint for this element.
     * @return the constraint
     */
    @Override
    public Constraint constraint() {
        return Constraint.fill();
    }

    /**
     * Handles a key event.
     * @param event the key event
     * @param focused whether this element is currently focused
     * @return the event result
     */
    @Override
    public EventResult handleKeyEvent(KeyEvent event, boolean focused) {
        // Route to input dialog if one is present (modal behavior with text input)
        if (currentDialog != null && manager.isInputDialog()) {
            return currentDialog.handleKeyEvent(event, true);
        }
        // Route to viewer dialog if one is present
        if (manager.isViewerDialog()) {
            return handleViewerKey(event);
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
                text(" [v] View ").dim(),
                text(" [q] Quit ").dim()
        ).length(1);
    }

    private Element browserRow(RenderContext context) {
        // Sync focus with active side - when Tab changes focus, update the controller
        if (context.isFocused("left") && !manager.isActive(FileManagerController.Side.LEFT)) {
            manager.setActiveSide(FileManagerController.Side.LEFT);
        } else if (context.isFocused("right") && !manager.isActive(FileManagerController.Side.RIGHT)) {
            manager.setActiveSide(FileManagerController.Side.RIGHT);
        }

        boolean leftActive = manager.isActive(FileManagerController.Side.LEFT);
        boolean rightActive = manager.isActive(FileManagerController.Side.RIGHT);

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
                .focusable()
                .rounded()
                .titleEllipsisStart()
                .borderColor(active ? Color.CYAN : Color.DARK_GRAY)
                .fill();
    }

    private Element fileList(DirectoryBrowserController browser, boolean active) {
        List<DirectoryBrowserController.FileEntry> entries = browser.entries();
        int offset = browser.scrollOffset();
        int cursor = browser.cursorIndex();
        int visibleCount = Math.min(browser.visibleRows(), Math.max(0, entries.size() - offset));

        if (visibleCount == 0) {
            return text("Empty").dim().fill();
        }

        Element[] elements = new Element[visibleCount];
        for (int i = 0; i < visibleCount; i++) {
            int entryIndex = offset + i;
            DirectoryBrowserController.FileEntry entry = entries.get(entryIndex);
            boolean isSelected = entryIndex == cursor;
            elements[i] = fileEntry(browser, entry, isSelected, active);
        }

        return column(elements).fill();
    }

    private Element fileEntry(DirectoryBrowserController browser, DirectoryBrowserController.FileEntry entry, boolean selected, boolean active) {
        String name = entry.name();
        boolean isMarked = browser.isMarked(name);
        String marker = isMarked ? "*" : " ";
        String displayName = entry.isDirectory() && !name.equals("..") ? name + "/" : name;
        String size = entry.isDirectory() ? "<DIR>" : formatSize(entry.size());

        // Combine marker + space + name into single left-aligned text
        String fullName = marker + " " + displayName;

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

        if (bg != null) {
            return row(
                    text(fullName).fg(fg).bg(bg).ellipsis().fill(),
                    text(" " + size).fg(selected ? fg : Color.GRAY).bg(bg)
            ).length(1);
        }
        return row(
                text(fullName).fg(fg).ellipsis().fill(),
                text(" " + size).fg(selected ? fg : Color.GRAY)
        ).length(1);
    }

    private Element helpBar() {
        DirectoryBrowserController browser = manager.activeBrowser();
        DirectoryBrowserController.FileEntry entry = browser.selectedEntry();

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
                text("[Enter] Open  [v] View  [Backspace] Up  [+] Mark All  [-] Unmark").dim()
        ).length(1);
    }

    private void renderViewerDialog(Frame frame, Rect area, RenderContext context) {
        Path file = manager.viewingFile();
        if (file == null) {
            return;
        }

        String fileName = file.getFileName().toString();
        boolean isImage = isImageFile(fileName);

        // Calculate dialog size (use most of the screen)
        int dialogWidth = Math.min(area.width() - 4, 120);
        int dialogHeight = Math.min(area.height() - 4, 40);

        // For image files, create the image widget first to get protocol name
        String dialogTitle = fileName;
        if (isImage) {
            try {
                ImageData imageData = ImageData.fromPath(file);
                Image image = Image.builder()
                        .data(imageData)
                        .scaling(ImageScaling.FIT)
                        .build();
                String protocolName = image.protocol().name();
                dialogTitle = fileName + " (" + protocolName + ")";
            } catch (IOException e) {
                // If we can't load the image, just use the filename
            }
        }

        // Create a custom element for the viewer
        Element viewerElement = new Element() {
            @Override
            public void render(Frame frame, Rect renderArea, RenderContext ctx) {
                if (isImage) {
                    renderImage(frame, renderArea, file);
                } else {
                    renderTextFile(frame, renderArea, file);
                }
            }

            @Override
            public Constraint constraint() {
                return Constraint.fill();
            }
        };

        // Create dialog with viewer content
        currentDialog = dialog(dialogTitle,
                viewerElement,
                text(isImage 
                        ? "[Esc] Close" 
                        : "[Esc] Close  [↑↓] Scroll  [PgUp/PgDn] Page").dim()
        ).rounded()
                .borderColor(Color.CYAN)
                .width(dialogWidth)
                .height(dialogHeight)
                .onCancel(manager::dismissDialog);

        currentDialog.render(frame, area, context);
    }

    private void renderImage(Frame frame, Rect area, Path imagePath) {
        try {
            ImageData imageData = ImageData.fromPath(imagePath);
            Image image = Image.builder()
                    .data(imageData)
                    .scaling(ImageScaling.FIT)
                    .block(Block.builder()
                            .borders(Borders.NONE)
                            .build())
                    .build();
            frame.renderWidget(image, area);
        } catch (IOException e) {
            // Render error message
            Paragraph error = Paragraph.builder()
                    .text(Text.from("Error loading image: " + e.getMessage()))
                    .style(dev.tamboui.style.Style.EMPTY.fg(Color.RED))
                    .build();
            frame.renderWidget(error, area);
        }
    }

    private void renderTextFile(Frame frame, Rect area, Path textPath) {
        try {
            byte[] bytes = Files.readAllBytes(textPath);
            String content = new String(bytes, StandardCharsets.UTF_8);
            int scrollPos = manager.textScrollPosition();
            
            Paragraph paragraph = Paragraph.builder()
                    .text(Text.from(content))
                    .overflow(Overflow.WRAP_WORD)
                    .scroll(scrollPos)
                    .style(dev.tamboui.style.Style.EMPTY.fg(Color.WHITE))
                    .build();
            frame.renderWidget(paragraph, area);
        } catch (IOException e) {
            // Render error message
            Paragraph error = Paragraph.builder()
                    .text(Text.from("Error reading file: " + e.getMessage()))
                    .style(dev.tamboui.style.Style.EMPTY.fg(Color.RED))
                    .build();
            frame.renderWidget(error, area);
        }
    }

    private EventResult handleViewerKey(KeyEvent event) {
        // Close viewer
        if (event.isCancel()) {
            manager.dismissDialog();
            return EventResult.HANDLED;
        }

        Path file = manager.viewingFile();
        if (file == null) {
            return EventResult.UNHANDLED;
        }

        String fileName = file.getFileName().toString();
        boolean isImage = isImageFile(fileName);

        // Only handle scroll keys for text files
        if (!isImage) {
            if (event.isUp()) {
                manager.scrollTextUp();
                return EventResult.HANDLED;
            }
            if (event.isDown()) {
                manager.scrollTextDown();
                return EventResult.HANDLED;
            }
            if (event.isPageUp()) {
                // Estimate page size based on available area
                manager.scrollTextPageUp(20);
                return EventResult.HANDLED;
            }
            if (event.isPageDown()) {
                manager.scrollTextPageDown(20);
                return EventResult.HANDLED;
            }
        }

        return EventResult.UNHANDLED;
    }

    private static boolean isImageFile(String fileName) {
        String lower = fileName.toLowerCase();
        return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg");
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
