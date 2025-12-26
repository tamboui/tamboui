///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS dev.tamboui:tamboui-widgets:LATEST
//DEPS dev.tamboui:tamboui-jline:LATEST
/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

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
import dev.tamboui.widgets.paragraph.Paragraph;
import dev.tamboui.widgets.tabs.Tabs;
import dev.tamboui.widgets.tabs.TabsState;

import java.io.IOException;
import java.util.List;

/**
 * Demo TUI application showcasing the Tabs widget.
 */
public class TabsDemo {

    private static final String[] TAB_NAMES = {"Home", "Settings", "Profile", "Help"};

    private static final String[][] TAB_CONTENT = {
        { // Home
            "Welcome to TamboUI!",
            "",
            "TamboUI is a pure Java port of ratatui, the Rust library for",
            "building terminal user interfaces.",
            "",
            "Features:",
            "  - Pure Java implementation (no native dependencies)",
            "  - Modern Java 21+ features (records, sealed classes, pattern matching)",
            "  - JLine backend for cross-platform terminal support",
            "  - GraalVM native image compatible"
        },
        { // Settings
            "Application Settings",
            "",
            "Theme:        Dark",
            "Font Size:    Medium",
            "Auto-save:    Enabled",
            "Notifications: On",
            "",
            "Press 's' to toggle settings (demo only)"
        },
        { // Profile
            "User Profile",
            "",
            "Username:     developer",
            "Email:        dev@example.com",
            "Role:         Administrator",
            "Last Login:   Today",
            "",
            "Account created: January 2025"
        },
        { // Help
            "Keyboard Shortcuts",
            "",
            "Navigation:",
            "  Tab / Right Arrow  - Next tab",
            "  Shift+Tab / Left   - Previous tab",
            "  1-4                - Jump to tab",
            "",
            "General:",
            "  q / Ctrl+C         - Quit application",
            "  ?                  - Show this help"
        }
    };

    private boolean running = true;
    private TabsState tabsState = new TabsState(0);

    public static void main(String[] args) throws Exception {
        new TabsDemo().run();
    }

    public void run() throws Exception {
        try (Backend backend = BackendFactory.create()) {
            backend.enableRawMode();
            backend.enterAlternateScreen();
            backend.hideCursor();

            Terminal<Backend> terminal = new Terminal<>(backend);

            // Handle resize
            backend.onResize(() -> {
                try {
                    terminal.draw(this::ui);
                } catch (IOException e) {
                    // Ignore
                }
            });

            // Initial draw
            terminal.draw(this::ui);

            // Event loop
            while (running) {
                int c = backend.read(100);
                if (c == -2 || c == -1) {
                    continue;
                }

                boolean needsRedraw = handleInput(c, backend);
                if (needsRedraw) {
                    terminal.draw(this::ui);
                }
            }
        }
    }

    private boolean handleInput(int c, Backend backend) throws IOException {
        // Handle escape sequences
        if (c == 27) {
            int next = backend.peek(50);
            if (next == '[') {
                backend.read(50);
                int code = backend.read(50);
                return handleEscapeSequence(code);
            }
            return false;
        }

        return switch (c) {
            case 'q', 'Q', 3 -> {
                running = false;
                yield true;
            }
            case '\t' -> { // Tab
                tabsState.selectNext(TAB_NAMES.length);
                yield true;
            }
            case '1' -> {
                tabsState.select(0);
                yield true;
            }
            case '2' -> {
                tabsState.select(1);
                yield true;
            }
            case '3' -> {
                tabsState.select(2);
                yield true;
            }
            case '4' -> {
                tabsState.select(3);
                yield true;
            }
            default -> false;
        };
    }

    private boolean handleEscapeSequence(int code) {
        return switch (code) {
            case 'C' -> { // Right arrow
                tabsState.selectNext(TAB_NAMES.length);
                yield true;
            }
            case 'D' -> { // Left arrow
                tabsState.selectPrevious(TAB_NAMES.length);
                yield true;
            }
            case 'Z' -> { // Shift+Tab (backtab)
                tabsState.selectPrevious(TAB_NAMES.length);
                yield true;
            }
            default -> false;
        };
    }

    private void ui(Frame frame) {
        Rect area = frame.area();

        List<Rect> layout = Layout.vertical()
            .constraints(
                Constraint.length(3),  // Header
                Constraint.length(3),  // Tabs
                Constraint.fill(),     // Content
                Constraint.length(3)   // Footer
            )
            .split(area);

        renderHeader(frame, layout.get(0));
        renderTabs(frame, layout.get(1));
        renderContent(frame, layout.get(2));
        renderFooter(frame, layout.get(3));
    }

    private void renderHeader(Frame frame, Rect area) {
        Block headerBlock = Block.builder()
            .borders(Borders.ALL)
            .borderType(BorderType.ROUNDED)
            .borderStyle(Style.EMPTY.fg(Color.CYAN))
            .title(Title.from(
                Line.from(
                    Span.raw(" TamboUI ").bold().cyan(),
                    Span.raw("Tabs Demo ").yellow()
                )
            ).centered())
            .build();

        frame.renderWidget(headerBlock, area);
    }

    private void renderTabs(Frame frame, Rect area) {
        // Create styled tab titles
        Line[] tabLines = new Line[TAB_NAMES.length];
        for (int i = 0; i < TAB_NAMES.length; i++) {
            String number = String.valueOf(i + 1);
            tabLines[i] = Line.from(
                Span.raw(number).dim(),
                Span.raw(":"),
                Span.raw(TAB_NAMES[i])
            );
        }

        Tabs tabs = Tabs.builder()
            .titles(tabLines)
            .highlightStyle(Style.EMPTY.fg(Color.YELLOW).bold())
            .style(Style.EMPTY.fg(Color.WHITE))
            .divider(Span.raw(" │ ").fg(Color.DARK_GRAY))
            .padding(" ", " ")
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.GREEN))
                .build())
            .build();

        frame.renderStatefulWidget(tabs, area, tabsState);
    }

    private void renderContent(Frame frame, Rect area) {
        Integer selected = tabsState.selected();
        int tabIndex = selected != null ? selected : 0;

        String[] content = TAB_CONTENT[tabIndex];
        Text contentText = Text.from(String.join("\n", content));

        // Color based on selected tab
        Color borderColor = switch (tabIndex) {
            case 0 -> Color.CYAN;
            case 1 -> Color.YELLOW;
            case 2 -> Color.MAGENTA;
            case 3 -> Color.GREEN;
            default -> Color.WHITE;
        };

        Paragraph paragraph = Paragraph.builder()
            .text(contentText)
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(borderColor))
                .title(Title.from(
                    Line.from(
                        Span.raw(" "),
                        Span.raw(TAB_NAMES[tabIndex]).fg(borderColor).bold(),
                        Span.raw(" ")
                    )
                ))
                .build())
            .build();

        frame.renderWidget(paragraph, area);
    }

    private void renderFooter(Frame frame, Rect area) {
        Line helpLine = Line.from(
            Span.raw(" Tab/→").bold().yellow(),
            Span.raw(" Next  ").dim(),
            Span.raw("←/Shift+Tab").bold().yellow(),
            Span.raw(" Prev  ").dim(),
            Span.raw("1-4").bold().yellow(),
            Span.raw(" Jump  ").dim(),
            Span.raw("q").bold().yellow(),
            Span.raw(" Quit").dim()
        );

        Paragraph footer = Paragraph.builder()
            .text(Text.from(helpLine))
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.DARK_GRAY))
                .build())
            .build();

        frame.renderWidget(footer, area);
    }
}
