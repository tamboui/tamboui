///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS dev.tamboui:tamboui-widgets:LATEST
//DEPS dev.tamboui:tamboui-jline:LATEST

/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import dev.tamboui.layout.Alignment;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Layout;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Backend;
import dev.tamboui.terminal.BackendFactory;
import dev.tamboui.terminal.Frame;
import dev.tamboui.terminal.Terminal;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.widgets.block.Title;
import dev.tamboui.widgets.figlet.BundledFigletFont;
import dev.tamboui.widgets.figlet.Figlet;
import dev.tamboui.widgets.figlet.FigletFont;
import dev.tamboui.widgets.input.TextInput;
import dev.tamboui.widgets.input.TextInputState;
import dev.tamboui.widgets.list.ListItem;
import dev.tamboui.widgets.list.ListState;
import dev.tamboui.widgets.list.ListWidget;
import dev.tamboui.widgets.paragraph.Paragraph;
import dev.tamboui.widgets.Clear;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Demo showcasing the Figlet widget (bundled fonts, different sizes, styling).
 *
 * <p>Keys:
 * <ul>
 *   <li>Up/Down or j/k: select font</li>
 *   <li>Tab: switch focus (font list / input)</li>
 *   <li>a: add font from URL (opens popup)</li>
 *   <li>q: quit (when font list is focused)</li>
 * </ul>
 */
public final class FigletDemo {

    private enum Focus {
        FONT_LIST,
        INPUT,
        POPUP
    }

    private static final class FontEntry {
        final String id;
        final FigletFont font;
        final boolean isBundled;

        FontEntry(String id, FigletFont font, boolean isBundled) {
            this.id = id;
            this.font = font;
            this.isBundled = isBundled;
        }

        static FontEntry bundled(BundledFigletFont bundled) {
            return new FontEntry(bundled.id(), FigletFont.bundled(bundled), true);
        }

        static FontEntry custom(String id, FigletFont font) {
            return new FontEntry(id, font, false);
        }
    }

    private static final List<FontEntry> INITIAL_FONTS = List.of(
        FontEntry.bundled(BundledFigletFont.BIG),
        FontEntry.bundled(BundledFigletFont.SLANT),
        FontEntry.bundled(BundledFigletFont.STANDARD),
        FontEntry.bundled(BundledFigletFont.SMALL),
        FontEntry.bundled(BundledFigletFont.MINI)
    );

    private static final Color[] PALETTE = {
        Color.CYAN,
        Color.MAGENTA,
        Color.YELLOW,
        Color.GREEN,
        Color.BLUE,
        Color.RED
    };

    private boolean running = true;
    private final ListState fontListState = new ListState();
    private final TextInputState inputState = new TextInputState();
    private final TextInputState urlInputState = new TextInputState();
    private final List<FontEntry> fonts = new ArrayList<>(INITIAL_FONTS);
    private Focus focus = Focus.FONT_LIST;
    private boolean showPopup = false;
    private String errorMessage = null;

    public static void main(String[] args) throws Exception {
        new FigletDemo().run();
    }

    public void run() throws Exception {
        inputState.insert("TamboUI");
        fontListState.select(0);

        try (Backend backend = BackendFactory.create()) {
            backend.enableRawMode();
            backend.enterAlternateScreen();
            backend.hideCursor();

            Terminal<Backend> terminal = new Terminal<>(backend);

            backend.onResize(() -> {
                try {
                    terminal.draw(this::ui);
                } catch (Exception ignored) {
                }
            });

            terminal.draw(this::ui);

            while (running) {
                int c = backend.read(250);
                if (c == -1 || c == -2) {
                    continue;
                }
                if (handleInput(c, backend)) {
                    terminal.draw(this::ui);
                }
            }
        }
    }

    private boolean handleInput(int c, Backend backend) throws IOException {
        // Escape sequences (arrows, etc.)
        if (c == 27) { // ESC
            int next = backend.peek(50);
            if (next == '[') {
                backend.read(50); // consume '['
                int code = backend.read(50);
                return handleEscapeSequence(code, backend);
            }
            return false;
        }

        if (focus == Focus.POPUP) {
            return handlePopupKey(c, backend);
        }

        if (c == '\t' || c == 9) {
            focus = (focus == Focus.FONT_LIST) ? Focus.INPUT : Focus.FONT_LIST;
            return true;
        }

        if (focus == Focus.FONT_LIST) {
            return handleFontListKey(c);
        }

        return handleInputKey(c);
    }

    private boolean handleEscapeSequence(int code, Backend backend) throws IOException {
        return switch (code) {
            case 'A' -> { // Up
                if (focus == Focus.FONT_LIST) {
                    fontListState.selectPrevious();
                    normalizeSelection();
                    yield true;
                }
                yield false;
            }
            case 'B' -> { // Down
                if (focus == Focus.FONT_LIST) {
                    fontListState.selectNext(fonts.size());
                    normalizeSelection();
                    yield true;
                }
                yield false;
            }
            case 'C' -> { // Right
                if (focus == Focus.INPUT) {
                    inputState.moveCursorRight();
                    yield true;
                }
                yield false;
            }
            case 'D' -> { // Left
                if (focus == Focus.INPUT) {
                    inputState.moveCursorLeft();
                    yield true;
                }
                yield false;
            }
            case 'H' -> { // Home
                if (focus == Focus.INPUT) {
                    inputState.moveCursorToStart();
                    yield true;
                }
                yield false;
            }
            case 'F' -> { // End
                if (focus == Focus.INPUT) {
                    inputState.moveCursorToEnd();
                    yield true;
                }
                yield false;
            }
            case '3' -> { // Delete (ESC[3~)
                int tilde = backend.read(50);
                if (tilde == '~' && focus == Focus.INPUT) {
                    inputState.deleteForward();
                    yield true;
                }
                yield false;
            }
            default -> false;
        };
    }

    private boolean handleFontListKey(int c) {
        return switch (c) {
            case 'q', 'Q' -> {
                running = false;
                yield true;
            }
            case 'a', 'A' -> {
                showPopup = true;
                focus = Focus.POPUP;
                urlInputState.clear();
                errorMessage = null;
                yield true;
            }
            case 'j', 'J' -> {
                fontListState.selectNext(fonts.size());
                normalizeSelection();
                yield true;
            }
            case 'k', 'K' -> {
                fontListState.selectPrevious();
                normalizeSelection();
                yield true;
            }
            default -> false;
        };
    }

    private boolean handlePopupKey(int c, Backend backend) throws IOException {
        // Escape sequences (arrows, etc.)
        if (c == 27) { // ESC
            int next = backend.peek(50);
            if (next == '[') {
                backend.read(50); // consume '['
                int code = backend.read(50);
                return handlePopupEscapeSequence(code, backend);
            }
            // Plain ESC - close popup
            showPopup = false;
            focus = Focus.FONT_LIST;
            errorMessage = null;
            return true;
        }

        return switch (c) {
            case 3 -> { // Ctrl+C
                running = false;
                yield true;
            }
            case '\r', '\n' -> { // Enter - load font
                loadFontFromUrl();
                yield true;
            }
            case 127, 8 -> { // Backspace
                urlInputState.deleteBackward();
                yield true;
            }
            case 21 -> { // Ctrl+U
                urlInputState.clear();
                yield true;
            }
            default -> {
                // Regular character input
                if (c >= 32 && c < 127) {
                    urlInputState.insert((char) c);
                    yield true;
                }
                yield false;
            }
        };
    }

    private boolean handlePopupEscapeSequence(int code, Backend backend) throws IOException {
        return switch (code) {
            case 'C' -> { // Right
                urlInputState.moveCursorRight();
                yield true;
            }
            case 'D' -> { // Left
                urlInputState.moveCursorLeft();
                yield true;
            }
            case 'H' -> { // Home
                urlInputState.moveCursorToStart();
                yield true;
            }
            case 'F' -> { // End
                urlInputState.moveCursorToEnd();
                yield true;
            }
            case '3' -> { // Delete (ESC[3~)
                int tilde = backend.read(50);
                if (tilde == '~') {
                    urlInputState.deleteForward();
                    yield true;
                }
                yield false;
            }
            default -> false;
        };
    }

    private void loadFontFromUrl() {
        String url = urlInputState.text().trim();
        if (url.isBlank()) {
            errorMessage = "URL cannot be empty";
            return;
        }

        try {
            URI uri = URI.create(url);
            URL fontUrl = uri.toURL();
            
            try (InputStream in = fontUrl.openStream()) {
                FigletFont font = FigletFont.fromInputStream(in);
                String fontId = extractFontId(url);
                fonts.add(FontEntry.custom(fontId, font));
                fontListState.select(fonts.size() - 1);
                normalizeSelection();
                showPopup = false;
                focus = Focus.FONT_LIST;
                errorMessage = null;
                urlInputState.clear();
            }
        } catch (Exception e) {
            errorMessage = "Failed to load font: " + e.getMessage();
        }
    }

    private String extractFontId(String url) {
        // Extract filename from URL, remove .flf extension if present
        String filename = url.substring(url.lastIndexOf('/') + 1);
        if (filename.endsWith(".flf")) {
            filename = filename.substring(0, filename.length() - 4);
        }
        if (filename.isBlank()) {
            filename = "custom-" + fonts.size();
        }
        return filename;
    }

    private boolean handleInputKey(int c) {
        return switch (c) {
            case 3 -> { // Ctrl+C
                running = false;
                yield true;
            }
            case 127, 8 -> { // Backspace
                inputState.deleteBackward();
                yield true;
            }
            case '\r', '\n' -> true; // Enter: keep value, just redraw
            case 21 -> { // Ctrl+U
                inputState.clear();
                yield true;
            }
            default -> {
                // Regular character input
                if (c >= 32 && c < 127) {
                    inputState.insert((char) c);
                    yield true;
                }
                yield false;
            }
        };
    }

    private void normalizeSelection() {
        Integer sel = fontListState.selected();
        if (sel == null) {
            fontListState.select(0);
            return;
        }
        if (sel < 0) {
            fontListState.select(0);
        } else if (sel >= fonts.size()) {
            fontListState.select(fonts.size() - 1);
        }
    }

    private void ui(Frame frame) {
        Rect area = frame.area();
        List<Rect> layout = Layout.vertical()
            .constraints(
                Constraint.length(3),
                Constraint.fill(),
                Constraint.length(3)
            )
            .split(area);

        renderHeader(frame, layout.get(0));
        renderBody(frame, layout.get(1));
        renderFooter(frame, layout.get(2));

        if (showPopup) {
            renderPopup(frame, area);
        }
    }

    private void renderHeader(Frame frame, Rect area) {
        Paragraph header = Paragraph.builder()
            .text(Text.from(Line.from(
                Span.raw(" Figlet ").bold().cyan(),
                Span.raw("widget demo — bundled fonts, sizes, styling").dim()
            )))
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.CYAN))
                .build())
            .build();

        frame.renderWidget(header, area);
    }

    private void renderBody(Frame frame, Rect area) {
        List<Rect> columns = Layout.horizontal()
            .constraints(
                Constraint.length(22),
                Constraint.fill()
            )
            .spacing(1)
            .split(area);

        renderFontList(frame, columns.get(0));
        renderFigletPanels(frame, columns.get(1));
    }

    private void renderFontList(Frame frame, Rect area) {
        List<ListItem> items = fonts.stream()
            .map(f -> {
                String label = f.id + " (" + f.font.height() + "h)";
                if (!f.isBundled) {
                    label = label + " [custom]";
                }
                return ListItem.from(label);
            })
            .toList();

        boolean focused = focus == Focus.FONT_LIST;
        Color borderColor = focused ? Color.GREEN : Color.DARK_GRAY;

        ListWidget list = ListWidget.builder()
            .items(items)
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(borderColor))
                .title(Title.from(Line.from(
                    Span.raw("Fonts ").bold(),
                    Span.raw(focused ? "(focused)" : "").dim()
                )))
                .titleBottom(Title.from("↑↓ / j k • a: add URL").right())
                .build())
            .highlightSymbol("▶ ")
            .highlightStyle(Style.EMPTY.bg(Color.BLUE).fg(Color.WHITE).bold())
            .build();

        frame.renderStatefulWidget(list, area, fontListState);
    }

    private void renderFigletPanels(Frame frame, Rect area) {
        List<Rect> rows = Layout.vertical()
            .constraints(
                Constraint.fill(),
                Constraint.length(8)
            )
            .spacing(1)
            .split(area);

        renderMainFiglet(frame, rows.get(0));
        renderPreviewFiglets(frame, rows.get(1));
    }

    private void renderMainFiglet(Frame frame, Rect area) {
        FontEntry selected = selectedFont();
        if (selected == null) {
            return;
        }
        String text = inputState.text().isBlank() ? "TamboUI" : inputState.text();

        Color color = PALETTE[Math.floorMod(selected.id.hashCode(), PALETTE.length)];
        Style style = Style.EMPTY.fg(color).bold();

        Figlet figlet = Figlet.builder()
            .text(text)
            .font(selected.font)
            .kerning(true)
            .letterSpacing(0)
            .alignment(Alignment.LEFT)
            .style(style)
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(color))
                .title(Title.from(Line.from(
                    Span.raw(selected.id.toUpperCase()).bold(),
                    Span.raw(" — main render").dim()
                )))
                .titleBottom(Title.from("Tab: focus input").right())
                .build())
            .build();

        frame.renderWidget(figlet, area);
    }

    private void renderPreviewFiglets(Frame frame, Rect area) {
        List<Rect> cols = Layout.horizontal()
            .constraints(
                Constraint.percentage(50),
                Constraint.percentage(50)
            )
            .spacing(1)
            .split(area);

        renderPreview(frame, cols.get(0), BundledFigletFont.MINI, Style.EMPTY.fg(Color.DARK_GRAY));
        renderPreview(frame, cols.get(1), BundledFigletFont.SMALL, Style.EMPTY.fg(Color.WHITE));
    }

    private void renderPreview(Frame frame, Rect area, BundledFigletFont font, Style style) {
        String text = inputState.text().isBlank() ? "TamboUI" : inputState.text();
        Figlet figlet = Figlet.builder()
            .text(text)
            .font(font)
            .kerning(true)
            .letterSpacing(0)
            .alignment(Alignment.LEFT)
            .style(style)
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.PLAIN)
                .borderStyle(Style.EMPTY.fg(Color.DARK_GRAY))
                .title(font.id() + " preview")
                .build())
            .build();
        frame.renderWidget(figlet, area);
    }

    private FontEntry selectedFont() {
        Integer sel = fontListState.selected();
        if (sel == null || fonts.isEmpty()) {
            return fonts.isEmpty() ? null : fonts.get(0);
        }
        return fonts.get(Math.max(0, Math.min(fonts.size() - 1, sel)));
    }

    private void renderPopup(Frame frame, Rect area) {
        // Create centered popup area (60% width, 30% height)
        int popupWidth = (int) (area.width() * 0.6);
        int popupHeight = 8;
        int popupX = area.x() + (area.width() - popupWidth) / 2;
        int popupY = area.y() + (area.height() - popupHeight) / 2;
        Rect popupArea = new Rect(popupX, popupY, popupWidth, popupHeight);

        // Clear the popup area
        frame.renderWidget(Clear.INSTANCE, popupArea);

        // Render popup content
        List<Rect> popupLayout = Layout.vertical()
            .constraints(
                Constraint.length(1),
                Constraint.length(3),
                Constraint.length(1),
                Constraint.fill()
            )
            .spacing(0)
            .split(popupArea);

        // Title
        Paragraph title = Paragraph.builder()
            .text(Text.from(Line.from(
                Span.raw(" Add Font from URL ").bold().cyan()
            )))
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.CYAN))
                .build())
            .build();
        frame.renderWidget(title, popupLayout.get(0));

        // URL input
        TextInput urlInput = TextInput.builder()
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.PLAIN)
                .borderStyle(Style.EMPTY.fg(Color.YELLOW))
                .title("URL to .flf file")
                .build())
            .style(Style.EMPTY.fg(Color.WHITE))
            .cursorStyle(Style.EMPTY.reversed())
            .placeholder("https://github.com/xero/figlet-fonts/blob/main/3D-ASCII.flf")
            .placeholderStyle(Style.EMPTY.dim().italic())
            .build();
        urlInput.renderWithCursor(popupLayout.get(1), frame.buffer(), urlInputState, frame);

        // Error message or instructions
        String message = errorMessage != null ? errorMessage : "Enter URL and press Enter to load";
        Color messageColor = errorMessage != null ? Color.RED : Color.DARK_GRAY;
        Paragraph messagePara = Paragraph.builder()
            .text(Text.from(Line.from(
                Span.raw(message).fg(messageColor)
            )))
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.PLAIN)
                .borderStyle(Style.EMPTY.fg(Color.DARK_GRAY))
                .build())
            .build();
        frame.renderWidget(messagePara, popupLayout.get(2));

        // Instructions
        Paragraph instructions = Paragraph.builder()
            .text(Text.from(Line.from(
                Span.raw("ESC: cancel • Enter: load").dim()
            )))
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.DARK_GRAY))
                .build())
            .build();
        frame.renderWidget(instructions, popupLayout.get(3));
    }

    private void renderFooter(Frame frame, Rect area) {
        boolean inputFocused = focus == Focus.INPUT;
        Color borderColor = inputFocused ? Color.YELLOW : Color.DARK_GRAY;

        TextInput input = TextInput.builder()
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(borderColor))
                .title(Title.from(Line.from(
                    Span.raw("Text ").bold(),
                    Span.raw(inputFocused ? "(focused)" : "").dim(),
                    Span.raw(" — type to update").dim()
                )))
                .titleBottom(Title.from(inputFocused ? "Backspace/Ctrl+U • Tab to switch • Ctrl+C to quit" : "Tab to focus input • q to quit").right())
                .build())
            .style(Style.EMPTY.fg(Color.WHITE))
            .cursorStyle(Style.EMPTY.reversed())
            .placeholder("Type text…")
            .placeholderStyle(Style.EMPTY.dim().italic())
            .build();

        if (inputFocused) {
            input.renderWithCursor(area, frame.buffer(), inputState, frame);
        } else {
            frame.renderStatefulWidget(input, area, inputState);
        }
    }
}

