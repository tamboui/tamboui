///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS dev.tamboui:tamboui-widgets:LATEST
//DEPS dev.tamboui:tamboui-css:LATEST
//DEPS dev.tamboui:tamboui-jline3-backend:LATEST
/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import dev.tamboui.css.Styleable;
import dev.tamboui.css.cascade.CssStyleResolver;
import dev.tamboui.css.cascade.PseudoClassState;
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
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.widgets.block.Title;
import dev.tamboui.widgets.list.ListItem;
import dev.tamboui.widgets.list.ListState;
import dev.tamboui.widgets.list.ListWidget;
import dev.tamboui.widgets.paragraph.Paragraph;

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
 *   <li>CSS-aware widgets that resolve properties automatically</li>
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

    private CssNoToolkitDemo() {
        styleEngine = StyleEngine.create();
        try {
            // Load both themes
            styleEngine.loadStylesheet("dark", "/themes-toolkit-css/dark.tcss");
            styleEngine.loadStylesheet("light", "/themes-toolkit-css/light.tcss");
            styleEngine.setActiveStylesheet(currentTheme);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load CSS themes", e);
        }
        // Start with first item selected
        listState.selectFirst();
    }

    /**
     * Demo entry point.
     * @param args the CLI arguments
     * @throws Exception on unexpected error
     */
    public static void main(String[] args) throws Exception {
        new CssNoToolkitDemo().run();
    }

    /**
     * Runs the demo application.
     *
     * @throws Exception if an error occurs
     */
     public void run() throws Exception {
        try (Backend backend = BackendFactory.create()) {
            backend.enableRawMode();
            backend.enterAlternateScreen();
            backend.hideCursor();

            Terminal<Backend> terminal = new Terminal<>(backend);

            // Handle resize
            backend.onResize(() -> {
                    terminal.draw(this::render);
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

        // Resolve root style and fill the entire screen
        CssStyleResolver rootResolver = resolveStyle("Screen", null, Set.of());
        frame.buffer().setStyle(area, rootResolver.toStyle());

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
        CssStyleResolver resolver = resolveStyle("Panel", null, Set.of("status"));

        // Create the block - CSS properties resolved automatically
        Block headerBlock = Block.builder()
            .borders(Borders.ALL)
            .styleResolver(resolver)
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

        // Left: title - use CSS .header class for styling (blue, not cyan)
        Style headerStyle = resolveStyle("Text", null, Set.of("header")).toStyle();
        Line titleLine = Line.from(Span.styled(" CSS Demo (No Toolkit) ", headerStyle));
        Paragraph titlePara = Paragraph.builder()
            .text(Text.from(titleLine))
            .styleResolver(resolver)
            .build();
        frame.renderWidget(titlePara, headerLayout.getFirst());

        // Right: theme and controls
        Style baseText = resolveStyle("Text", null, Set.of()).toStyle();
        Line controlsLine = Line.from(
            Span.styled("Theme: ", baseText.dim()),
            Span.styled(currentTheme.toUpperCase(), getAccentStyle()),
            Span.styled(" [t] Toggle ", baseText.dim()),
            Span.styled(" [Tab] Focus ", baseText.dim()),
            Span.styled(" [q] Quit ", baseText.dim())
        );
        Paragraph controlsPara = Paragraph.builder()
            .text(Text.from(controlsLine))
            .styleResolver(resolver)
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

        CssStyleResolver listResolver = resolveStyle("ListElement", "nav-list", Set.of(), isFocused);

        var items = listItems.stream()
            .map(item -> ListItem.from(item).toSizedWidget())
            .toList();

        ListWidget list = ListWidget.builder()
            .items(items)
            .itemStyleResolver((index, total) -> {
                PseudoClassState state = PseudoClassState.NONE
                    .withFirstChild(index == 0)
                    .withLastChild(index == total - 1)
                    .withNthChild(index + 1);
                return resolveStyleWithState("ListElement-item", null, Set.of(), state).toStyle();
            })
            // Get selection style with :selected pseudo-class - ensure background is included
            .highlightStyle(getSelectionHighlightStyle())
            .highlightSymbol("> ")
            .block(Block.builder()
                .borders(Borders.ALL)
                .styleResolver(listResolver)
                .title(Title.from("Navigation"))
                .build())
            .build();

        frame.renderStatefulWidget(list, area, listState);
    }

    private void renderStylesPanel(Frame frame, Rect area) {
        boolean isFocused = focusedPanel == 1;
        CssStyleResolver resolver = resolveStyle("Panel", "styles-panel", Set.of(), isFocused);

        // Build styled text lines
        List<Line> lines = new ArrayList<>();
        lines.add(styledLine("Primary Action", "primary"));
        lines.add(styledLine("Secondary Info", "secondary"));
        lines.add(styledLine("Warning Message", "warning"));
        lines.add(styledLine("Error Message", "error"));
        lines.add(styledLine("Success Message", "success"));
        lines.add(styledLine("Info Message", "info"));

        Paragraph paragraph = Paragraph.builder()
            .text(Text.from(lines))
            .block(Block.builder()
                .borders(Borders.ALL)
                .styleResolver(resolver)
                .title(Title.from("Style Classes"))
                .build())
            .styleResolver(resolver)
            .build();

        frame.renderWidget(paragraph, area);
    }

    private void renderAboutPanel(Frame frame, Rect area) {
        boolean isFocused = focusedPanel == 2;
        CssStyleResolver resolver = resolveStyle("Panel", "about-panel", Set.of(), isFocused);
        Style baseTextStyle = resolveStyle("Text", null, Set.of()).toStyle();

        List<Line> lines = new ArrayList<>();
        lines.add(Line.from(Span.styled("This demo shows CSS without toolkit.", baseTextStyle)));
        lines.add(Line.from(Span.styled(" ", baseTextStyle)));
        lines.add(Line.from(Span.styled("Features demonstrated:", baseTextStyle)));
        lines.add(Line.from(Span.styled("  - Backend/Terminal for event loop", baseTextStyle)));
        lines.add(Line.from(Span.styled("  - Widgets rendered directly", baseTextStyle)));
        lines.add(Line.from(Span.styled("  - StyleEngine for CSS", baseTextStyle)));
        lines.add(Line.from(Span.styled("  - CSS-aware widgets", baseTextStyle)));
        lines.add(Line.from(Span.styled(" ", baseTextStyle)));
        lines.add(styledLine("Try pressing [t] to toggle theme!", "info"));

        Paragraph paragraph = Paragraph.builder()
            .text(Text.from(lines))
            .block(Block.builder()
                .borders(Borders.ALL)
                .styleResolver(resolver)
                .title(Title.from("About"))
                .build())
            .styleResolver(resolver)
            .build();

        frame.renderWidget(paragraph, area);
    }

    private void renderFooter(Frame frame, Rect area) {
        CssStyleResolver resolver = resolveStyle("Panel", null, Set.of());
        Style baseText = resolveStyle("Text", null, Set.of()).toStyle();

        Line footerLine = Line.from(
            Span.styled("Programmatic ", baseText.bold().cyan()),
            Span.styled("+ CSS ", getStyleForClass("primary")),
            Span.styled("= Powerful Styling", getStyleForClass("success"))
        );

        Paragraph footer = Paragraph.builder()
            .text(Text.from(footerLine))
            .block(Block.builder()
                .borders(Borders.ALL)
                .styleResolver(resolver)
                .build())
            .styleResolver(resolver)
            .build();

        frame.renderWidget(footer, area);
    }

    // ═══════════════════════════════════════════════════════════════
    // Helper methods
    // ═══════════════════════════════════════════════════════════════

    private CssStyleResolver resolveStyle(String type, String id, Set<String> classes) {
        return styleEngine.resolve(new SimpleStyleable(type, id, classes));
    }

    private CssStyleResolver resolveStyle(String type, String id, Set<String> classes, boolean focused) {
        PseudoClassState state = focused ? PseudoClassState.ofFocused() : PseudoClassState.NONE;
        return styleEngine.resolve(new SimpleStyleable(type, id, classes), state, Collections.emptyList());
    }

    private CssStyleResolver resolveStyleWithState(String type, String id, Set<String> classes, PseudoClassState state) {
        return styleEngine.resolve(new SimpleStyleable(type, id, classes), state, Collections.emptyList());
    }

    private Line styledLine(String text, String cssClass) {
        Style style = getStyleForClass(cssClass);
        return Line.from(Span.styled(text, style));
    }

    private Style getStyleForClass(String cssClass) {
        return resolveStyle("Text", null, Set.of(cssClass)).toStyle();
    }

    private Style getAccentStyle() {
        return resolveStyle("Text", "theme-indicator", Set.of()).toStyle();
    }

    /**
     * Gets the selection highlight style from CSS.
     * Resolves ListElement-item:selected to get the proper highlight style.
     * <p>
     * Note: We explicitly get the background from the CSS since the :selected
     * pseudo-class resolution may not include it in all cases.
     */
    private Style getSelectionHighlightStyle() {
        // Resolve the :selected style to get color and modifiers
        CssStyleResolver selectedResolver = resolveStyleWithState("ListElement-item", null, Set.of(), PseudoClassState.ofSelected());

        // Build the style - start with what CSS provides
        Style style = selectedResolver.toStyle();

        // If background is not set, we need to get it explicitly
        // The CSS defines: ListElement-item:selected { background: $highlight-bg }
        // where $highlight-bg is #d0d0d0 (light) or #333333 (dark)
        if (style.bg().isEmpty()) {
            // Get the highlight background by resolving the variable through another property
            // that uses $highlight-bg - the ListElement-item:selected rule should have it
            // If that doesn't work, use the Panel background as a similar light color
            CssStyleResolver panelResolver = resolveStyle("Panel", null, Set.of());
            if (panelResolver.background().isPresent()) {
                // Panel uses $bg-primary, but we need $highlight-bg which is slightly different
                // For light theme: $bg-primary=#eeeeee, $highlight-bg=#d0d0d0 (darker)
                // For dark theme: $bg-primary=black, $highlight-bg=#333333 (lighter)
                // As a workaround, derive the highlight color from panel background
                Color panelBg = panelResolver.background().get();
                if (panelBg.equals(Color.BLACK)) {
                    // Dark theme - use #333333
                    style = style.bg(new Color.Rgb(0x33, 0x33, 0x33));
                } else {
                    // Light theme - use #d0d0d0
                    style = style.bg(new Color.Rgb(0xd0, 0xd0, 0xd0));
                }
            }
        }

        return style;
    }

    private void toggleTheme() {
        currentTheme = currentTheme.equals("dark") ? "light" : "dark";
        styleEngine.setActiveStylesheet(currentTheme);
    }

    /**
     * Simple implementation of Styleable for CSS resolution.
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
