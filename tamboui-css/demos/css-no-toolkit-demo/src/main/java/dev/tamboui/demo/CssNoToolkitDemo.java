///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS dev.tamboui:tamboui-widgets:LATEST
//DEPS dev.tamboui:tamboui-css:LATEST
//DEPS dev.tamboui:tamboui-jline:LATEST
/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import dev.tamboui.css.Styleable;
import dev.tamboui.css.cascade.PseudoClassState;
import dev.tamboui.css.cascade.ResolvedStyle;
import dev.tamboui.css.engine.StyleEngine;
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
import dev.tamboui.widgets.list.ListItem;
import dev.tamboui.widgets.list.ListState;
import dev.tamboui.widgets.list.ListWidget;
import dev.tamboui.widgets.paragraph.Paragraph;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;

/**
 * CSS Demo showing how to use CSS styling WITHOUT the toolkit module.
 * <p>
 * This demo demonstrates:
 * <ul>
 *   <li>Using Backend/Terminal directly for the event loop</li>
 *   <li>Using widgets (Paragraph, Block, ListWidget) directly</li>
 *   <li>Loading and applying CSS with StyleEngine</li>
 *   <li>Creating Styleable implementations for elements</li>
 *   <li>Live theme switching</li>
 * </ul>
 * <p>
 * Controls:
 * <ul>
 *   <li>t - Toggle between dark and light themes</li>
 *   <li>Tab - Switch focus between panels</li>
 *   <li>Up/Down or j/k - Navigate the list</li>
 *   <li>q or Ctrl+C - Quit</li>
 * </ul>
 */
public class CssNoToolkitDemo {

    private boolean running = true;
    private String currentTheme = "dark";
    private final StyleEngine styleEngine;
    private final ListState listState = new ListState();
    private final List<String> listItems = List.of(
        "Dashboard",
        "Settings",
        "Profile",
        "Messages",
        "Notifications"
    );

    // Track which panel has focus (0 = list, 1 = styles, 2 = about)
    private int focusedPanel = 0;
    private static final int PANEL_COUNT = 3;

    public CssNoToolkitDemo() {
        styleEngine = StyleEngine.create();
        try {
            // Load both themes
            styleEngine.loadStylesheet("dark", "/themes/dark.tcss");
            styleEngine.loadStylesheet("light", "/themes/light.tcss");
            styleEngine.setActiveStylesheet(currentTheme);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load CSS themes", e);
        }
        // Start with first item selected
        listState.selectFirst();
    }

    public static void main(String[] args) throws Exception {
        new CssNoToolkitDemo().run();
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
                    terminal.draw(this::render);
                } catch (IOException e) {
                    // Ignore
                }
            });

            // Event loop
            while (running) {
                terminal.draw(this::render);

                int c = backend.read(100);
                if (c == -2 || c == -1) {
                    continue;
                }

                handleInput(c, backend);
            }
        }
    }

    private void handleInput(int c, Backend backend) throws IOException {
        // Handle escape sequences (arrow keys)
        if (c == 27) {
            int next = backend.peek(50);
            if (next == '[') {
                backend.read(50);
                int code = backend.read(50);
                handleEscapeSequence(code);
                return;
            }
        }

        switch (c) {
            case 'q', 'Q', 3 -> running = false;  // q, Q, or Ctrl+C
            case 't', 'T' -> toggleTheme();
            case '\t' -> focusedPanel = (focusedPanel + 1) % PANEL_COUNT;
            case 'j', 'J' -> {
                if (focusedPanel == 0) {
                    listState.selectNext(listItems.size());
                }
            }
            case 'k', 'K' -> {
                if (focusedPanel == 0) {
                    listState.selectPrevious();
                }
            }
        }
    }

    private void handleEscapeSequence(int code) {
        switch (code) {
            case 'A' -> { // Up arrow
                if (focusedPanel == 0) {
                    listState.selectPrevious();
                }
            }
            case 'B' -> { // Down arrow
                if (focusedPanel == 0) {
                    listState.selectNext(listItems.size());
                }
            }
        }
    }

    private void render(Frame frame) {
        Rect area = frame.area();

        // Fill the entire screen with the base CSS style first
        // This ensures all gaps and unfilled areas have the correct background
        SimpleStyleable rootStyleable = new SimpleStyleable("Screen", null, Set.of());
        ResolvedStyle rootStyle = styleEngine.resolve(rootStyleable);
        frame.buffer().setStyle(area, rootStyle.toStyle());

        // Split into header, main content, footer
        var layout = Layout.vertical()
            .constraints(
                Constraint.length(3),  // Header
                Constraint.fill(),     // Main content
                Constraint.length(3)   // Footer
            )
            .split(area);

        renderHeader(frame, layout.get(0));
        renderMainContent(frame, layout.get(1));
        renderFooter(frame, layout.get(2));
    }

    private void renderHeader(Frame frame, Rect area) {
        // Resolve style for header using CSS
        SimpleStyleable headerStyleable = new SimpleStyleable("Panel", null, Set.of("status"));
        ResolvedStyle headerStyle = styleEngine.resolve(headerStyleable);
        Style resolvedStyle = headerStyle.toStyle();

        // Get base text style (with background) for creating spans
        Style baseText = getBaseTextStyle();

        // Create the outer block first
        Block headerBlock = Block.builder()
            .borders(Borders.ALL)
            .borderType(BorderType.ROUNDED)
            .borderStyle(resolvedStyle)
            .style(resolvedStyle)
            .build();

        // Get inner area after borders
        Rect innerArea = headerBlock.inner(area);

        // Render the block background and borders
        frame.renderWidget(headerBlock, area);

        // Split inner area: left title, spacer (fill), right controls
        var headerLayout = Layout.horizontal()
            .constraints(
                Constraint.length(22),  // "CSS Demo (No Toolkit)"
                Constraint.fill(),       // Spacer
                Constraint.length(50)    // Controls
            )
            .split(innerArea);

        // Left: title - use base style with background, then add foreground/modifiers
        Line titleLine = Line.from(Span.styled(" CSS Demo (No Toolkit) ", baseText.bold().cyan()));
        Paragraph titlePara = Paragraph.builder()
            .text(Text.from(titleLine))
            .style(resolvedStyle)
            .build();
        frame.renderWidget(titlePara, headerLayout.get(0));

        // Right: theme and controls - all spans need background from base style
        Line controlsLine = Line.from(
            Span.styled("Theme: ", baseText.dim()),
            Span.styled(currentTheme.toUpperCase(), getAccentStyle()),
            Span.styled(" [t] Toggle ", baseText.dim()),
            Span.styled(" [Tab] Focus ", baseText.dim()),
            Span.styled(" [q] Quit ", baseText.dim())
        );
        Paragraph controlsPara = Paragraph.builder()
            .text(Text.from(controlsLine))
            .style(resolvedStyle)
            .build();
        frame.renderWidget(controlsPara, headerLayout.get(2));
    }

    private void renderMainContent(Frame frame, Rect area) {
        // Split into 3 columns
        var columns = Layout.horizontal()
            .constraints(
                Constraint.length(20),   // List
                Constraint.fill(),       // Styles panel
                Constraint.fill()        // About panel
            )
            .spacing(1)
            .split(area);

        renderList(frame, columns.get(0));
        renderStylesPanel(frame, columns.get(1));
        renderAboutPanel(frame, columns.get(2));
    }

    private void renderList(Frame frame, Rect area) {
        boolean isFocused = focusedPanel == 0;

        // Resolve styles
        SimpleStyleable listStyleable = new SimpleStyleable("ListContainer", "nav-list", Set.of());
        PseudoClassState state = isFocused ? PseudoClassState.ofFocused() : PseudoClassState.NONE;
        ResolvedStyle listStyle = styleEngine.resolve(listStyleable, state, Collections.emptyList());

        // Get styles for list items using type-based sub-component naming
        SimpleStyleable itemStyleable = new SimpleStyleable("ListContainer-item", null, Set.of());
        ResolvedStyle selectedStyle = styleEngine.resolve(itemStyleable, PseudoClassState.ofSelected(), Collections.emptyList());

        // Create ListItems with positional styles (odd/even)
        List<ListItem> items = new ArrayList<>();
        for (int i = 0; i < listItems.size(); i++) {
            // Build pseudo-class state with nth-child position (1-based)
            PseudoClassState itemState = PseudoClassState.NONE
                .withFirstChild(i == 0)
                .withLastChild(i == listItems.size() - 1)
                .withNthChild(i + 1);  // CSS nth-child is 1-based
            ResolvedStyle posStyle = styleEngine.resolve(itemStyleable, itemState, Collections.emptyList());
            items.add(ListItem.from(listItems.get(i)).style(posStyle.toStyle()));
        }

        Style listResolvedStyle = listStyle.toStyle();
        // Get base item style for the highlight symbol
        ResolvedStyle baseItemStyle = styleEngine.resolve(itemStyleable);
        ListWidget list = ListWidget.builder()
            .items(items)
            .style(baseItemStyle.toStyle())
            .highlightStyle(selectedStyle.toStyle())
            .highlightSymbol(Line.from(Span.styled("> ", baseItemStyle.toStyle())))
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(listResolvedStyle)
                .style(listResolvedStyle)
                .title(Title.from("Navigation"))
                .build())
            .build();

        frame.renderStatefulWidget(list, area, listState);
    }

    private void renderStylesPanel(Frame frame, Rect area) {
        boolean isFocused = focusedPanel == 1;

        // Resolve panel style
        SimpleStyleable panelStyleable = new SimpleStyleable("Panel", "styles-panel", Set.of());
        PseudoClassState state = isFocused ? PseudoClassState.ofFocused() : PseudoClassState.NONE;
        ResolvedStyle panelStyle = styleEngine.resolve(panelStyleable, state, Collections.emptyList());

        // Build styled text lines
        List<Line> lines = new ArrayList<>();
        lines.add(styledLine("Primary Action", "primary"));
        lines.add(styledLine("Secondary Info", "secondary"));
        lines.add(styledLine("Warning Message", "warning"));
        lines.add(styledLine("Error Message", "error"));
        lines.add(styledLine("Success Message", "success"));
        lines.add(styledLine("Info Message", "info"));

        Style stylesPanelResolvedStyle = panelStyle.toStyle();
        Block block = Block.builder()
            .borders(Borders.ALL)
            .borderType(BorderType.ROUNDED)
            .borderStyle(stylesPanelResolvedStyle)
            .style(stylesPanelResolvedStyle)
            .title(Title.from("Style Classes"))
            .build();

        Paragraph paragraph = Paragraph.builder()
            .text(Text.from(lines))
            .block(block)
            .style(stylesPanelResolvedStyle)
            .build();

        frame.renderWidget(paragraph, area);
    }

    private void renderAboutPanel(Frame frame, Rect area) {
        boolean isFocused = focusedPanel == 2;

        // Resolve panel style
        SimpleStyleable panelStyleable = new SimpleStyleable("Panel", "about-panel", Set.of());
        PseudoClassState state = isFocused ? PseudoClassState.ofFocused() : PseudoClassState.NONE;
        ResolvedStyle panelStyle = styleEngine.resolve(panelStyleable, state, Collections.emptyList());

        // Get base text style from CSS
        Style baseTextStyle = getBaseTextStyle();

        List<Line> lines = new ArrayList<>();
        lines.add(Line.from(Span.styled("This demo shows CSS without toolkit.", baseTextStyle)));
        lines.add(Line.from(Span.styled(" ", baseTextStyle)));  // Empty line with background
        lines.add(Line.from(Span.styled("Features demonstrated:", baseTextStyle)));
        lines.add(Line.from(Span.styled("  - Backend/Terminal for event loop", baseTextStyle)));
        lines.add(Line.from(Span.styled("  - Widgets rendered directly", baseTextStyle)));
        lines.add(Line.from(Span.styled("  - StyleEngine for CSS", baseTextStyle)));
        lines.add(Line.from(Span.styled("  - Styleable interface", baseTextStyle)));
        lines.add(Line.from(Span.styled(" ", baseTextStyle)));  // Empty line with background
        lines.add(styledLine("Try pressing [t] to toggle theme!", "info"));

        Style aboutPanelResolvedStyle = panelStyle.toStyle();
        Block block = Block.builder()
            .borders(Borders.ALL)
            .borderType(BorderType.ROUNDED)
            .borderStyle(aboutPanelResolvedStyle)
            .style(aboutPanelResolvedStyle)
            .title(Title.from("About"))
            .build();

        Paragraph paragraph = Paragraph.builder()
            .text(Text.from(lines))
            .block(block)
            .style(aboutPanelResolvedStyle)
            .build();

        frame.renderWidget(paragraph, area);
    }

    private void renderFooter(Frame frame, Rect area) {
        // Resolve base style from CSS
        SimpleStyleable footerStyleable = new SimpleStyleable("Panel", null, Set.of());
        ResolvedStyle resolved = styleEngine.resolve(footerStyleable);
        Style footerStyle = resolved.toStyle();

        // Get base text style with background
        Style baseText = getBaseTextStyle();

        // Match original footer: "Programmatic + CSS = Powerful Styling"
        // All spans need the background from base style
        Line footerLine = Line.from(
            Span.styled("Programmatic ", baseText.bold().cyan()),
            Span.styled("+ CSS ", getStyleForClass("primary")),
            Span.styled("= Powerful Styling", getStyleForClass("success"))
        );

        Block footerBlock = Block.builder()
            .borders(Borders.ALL)
            .borderType(BorderType.ROUNDED)
            .borderStyle(footerStyle)
            .style(footerStyle)
            .build();

        Paragraph footer = Paragraph.builder()
            .text(Text.from(footerLine))
            .block(footerBlock)
            .style(footerStyle)
            .build();

        frame.renderWidget(footer, area);
    }

    /**
     * Creates a styled line using CSS class resolution.
     */
    private Line styledLine(String text, String cssClass) {
        Style style = getStyleForClass(cssClass);
        return Line.from(Span.styled(text, style));
    }

    /**
     * Resolves style for a CSS class.
     */
    private Style getStyleForClass(String cssClass) {
        SimpleStyleable styleable = new SimpleStyleable("Text", null, Set.of(cssClass));
        return styleEngine.resolve(styleable).toStyle();
    }

    private Style getAccentStyle() {
        // Use theme-indicator id for accent
        SimpleStyleable styleable = new SimpleStyleable("Text", "theme-indicator", Set.of());
        return styleEngine.resolve(styleable).toStyle();
    }

    /**
     * Resolves the base text style from CSS (matches * selector).
     */
    private Style getBaseTextStyle() {
        SimpleStyleable styleable = new SimpleStyleable("Text", null, Set.of());
        return styleEngine.resolve(styleable).toStyle();
    }

    private void toggleTheme() {
        currentTheme = currentTheme.equals("dark") ? "light" : "dark";
        styleEngine.setActiveStylesheet(currentTheme);
    }

    /**
     * Simple implementation of Styleable for CSS resolution.
     * <p>
     * This shows how to create Styleable objects for CSS matching
     * without using the toolkit's element abstractions.
     */
    private static final class SimpleStyleable implements Styleable {
        private final String type;
        private final String id;
        private final Set<String> classes;

        SimpleStyleable(String type, String id, Set<String> classes) {
            this.type = type;
            this.id = id;
            this.classes = classes;
        }

        @Override
        public String styleType() {
            return type;
        }

        @Override
        public Optional<String> cssId() {
            return Optional.ofNullable(id);
        }

        @Override
        public Set<String> cssClasses() {
            return classes;
        }

        @Override
        public Optional<Styleable> cssParent() {
            return Optional.empty();
        }
    }
}
