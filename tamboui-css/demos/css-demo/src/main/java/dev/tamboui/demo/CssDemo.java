///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS dev.tamboui:tamboui-toolkit:LATEST
//DEPS dev.tamboui:tamboui-jline3-backend:LATEST
//FILES themes-css/themed.tcss=../../../../resources/themes-css/themed.tcss
/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.List;

import dev.tamboui.css.engine.StyleEngine;
import dev.tamboui.css.theme.ThemeEngine;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.terminal.Frame;
import dev.tamboui.theme.ColorSystem;
import dev.tamboui.theme.ThemeRegistry;
import dev.tamboui.toolkit.app.ToolkitRunner;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.element.Size;
import dev.tamboui.toolkit.elements.ListElement;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.tui.event.KeyEvent;

import static dev.tamboui.toolkit.Toolkit.*;

/**
 * CSS Demo showcasing theme-aware styling with semantic colors.
 * <p>
 * A single TCSS stylesheet references theme variables ({@code $primary},
 * {@code $border}, etc.) that are injected by {@link ThemeEngine}.
 * Switching themes re-injects the variables so the same stylesheet
 * produces a completely different look.
 * <p>
 * Features demonstrated:
 * <ul>
 *   <li>Theme-aware CSS using injected {@code $variables}</li>
 *   <li>Live theme switching across 7 built-in themes with number keys</li>
 *   <li>Semantic color classes (primary, error, success, …)</li>
 *   <li>Auto-generated palette: shades, text colors, UI colors</li>
 *   <li>Pseudo-class states (:focus) – Tab to navigate</li>
 *   <li>List selection highlighting from theme colors</li>
 * </ul>
 */
public class CssDemo implements Element {

    private static final String[] THEME_NAMES = ThemeRegistry.all().keySet().stream()
        .sorted()
        .toArray(String[]::new);

    private int currentThemeIndex;
    private final StyleEngine styleEngine;
    private final ThemeEngine themeEngine;

    private final List<String> listItems = List.of(
        "Dashboard",
        "Settings",
        "Profile",
        "Messages",
        "Notifications"
    );
    private final ListElement<?> navList;

    private CssDemo() {
        styleEngine = StyleEngine.create();
        try {
            styleEngine.loadStylesheet("themed", "/themes-css/themed.tcss");
            styleEngine.setActiveStylesheet("themed");
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load CSS stylesheet", e);
        }

        themeEngine = new ThemeEngine();

        // Default to tamboui-default (or first available)
        currentThemeIndex = indexOf("tamboui-default");
        applyTheme();

        navList = list(listItems)
            .id("nav-list")
            .title("Navigation")
            .rounded()
            .autoScroll();
    }

    /**
     * Demo entry point.
     *
     * @param args the CLI arguments
     * @throws Exception on unexpected error
     */
    public static void main(String[] args) throws Exception {
        new CssDemo().run();
    }

    /**
     * Runs the demo application.
     *
     * @throws Exception if an error occurs
     */
    public void run() throws Exception {
        var config = TuiConfig.builder()
            .mouseCapture(true)
            .tickRate(Duration.ofMillis(100))
            .build();

        try (var runner = ToolkitRunner.create(config)) {
            runner.styleEngine(styleEngine);
            runner.run(() -> this);
        }
    }

    @Override
    public void render(Frame frame, Rect area, RenderContext context) {
        String themeName = THEME_NAMES[currentThemeIndex];

        dock()
            // Header
            .top(panel(() -> row(
                text(" CSS + Themes ").addClass("header"),
                spacer(),
                text(" Theme: ").addClass("dim"),
                text(themeName).id("theme-indicator"),
                text(" [t/T] Switch ").addClass("dim"),
                text(" [Tab] Focus ").addClass("dim"),
                text(" [q] Quit ").addClass("dim")
            )).rounded().addClass("status"))

            // Left sidebar – navigation list
            .left(navList, Constraint.length(20))

            // Center – semantic colors + generated palette
            .center(column(
                // Semantic colors
                panel(() -> column(
                    text("Primary").addClass("primary"),
                    text("Secondary").addClass("secondary"),
                    text("Warning").addClass("warning"),
                    text("Error").addClass("error"),
                    text("Success").addClass("success"),
                    text("Info").addClass("info"),
                    text("Accent").addClass("accent")
                )).id("styles-panel").focusable().title("Semantic Colors").rounded(),

                // Generated palette
                panel(() -> buildPaletteView())
                    .id("palette-panel").focusable().title("Generated Palette").rounded()
            ))

            // Right panel – about / help
            .right(panel(() -> column(
                text("Theme-Aware CSS").addClass("primary"),
                spacer(1),
                text("One TCSS stylesheet uses"),
                text("$variables injected from the"),
                text("active theme's ColorSystem."),
                spacer(1),
                text("Keys:").addClass("info"),
                text("  [t]      Next theme"),
                text("  [T]      Previous theme"),
                text("  [Tab]    Cycle focus"),
                text("  [Up/Dn]  Navigate list"),
                text("  [q]      Quit"),
                spacer(1),
                text("Themes:").addClass("info"),
                buildThemeList()
            )).id("about-panel").focusable().title("About").rounded(), Constraint.length(30))

            // Footer
            .bottom(panel(() -> row(
                text("CSS $variables ").addClass("primary"),
                text("+ ThemeEngine ").addClass("secondary"),
                text("= Semantic Styling").addClass("success")
            )).rounded())

        .render(frame, area, context);
    }

    private Element buildThemeList() {
        var elements = new Element[THEME_NAMES.length];
        for (int i = 0; i < THEME_NAMES.length; i++) {
            String marker = (i == currentThemeIndex) ? " ▸ " : "   ";
            var item = text(marker + THEME_NAMES[i]);
            elements[i] = (i == currentThemeIndex) ? item.addClass("primary") : item.addClass("muted");
        }
        return column(elements);
    }

    private Element buildPaletteView() {
        ColorSystem cs = themeEngine.getColorSystem();
        return column(
            swatchRow("Surfaces", cs, "background", "surface", "panel"),
            swatchRow("Primary",  cs, "primary-light", "primary-dark", "primary-muted"),
            swatchRow("Second.",  cs, "secondary-light", "secondary-dark", "secondary-muted"),
            swatchRow("Error",    cs, "error-light", "error-dark"),
            swatchRow("Text",     cs, "text", "text-muted", "text-disabled"),
            swatchRow("UI",       cs, "border", "border-focus", "selection-bg", "hover-bg")
        );
    }

    private Element swatchRow(String label, ColorSystem cs, String... names) {
        // label ██ name ██ name ...
        var elements = new Element[1 + names.length * 2];
        elements[0] = text(String.format("%-9s", label)).addClass("dim");
        for (int i = 0; i < names.length; i++) {
            Color color = cs.get(names[i]);
            if (color != null) {
                Color.Rgb rgb = color.toRgb();
                elements[1 + i * 2] = text("██").fg(Color.rgb(rgb.r(), rgb.g(), rgb.b()));
                elements[2 + i * 2] = text(names[i] + " ").addClass("muted");
            } else {
                elements[1 + i * 2] = text("  ");
                elements[2 + i * 2] = text(names[i] + "? ").addClass("disabled");
            }
        }
        return row(elements);
    }

    @Override
    public Size preferredSize(int availableWidth, int availableHeight, RenderContext context) {
        return Size.UNKNOWN;
    }

    @Override
    public Constraint constraint() {
        return Constraint.fill();
    }

    @Override
    public EventResult handleKeyEvent(KeyEvent event, boolean focused) {
        // Theme switching
        if (event.isChar('t')) {
            currentThemeIndex = (currentThemeIndex + 1) % THEME_NAMES.length;
            applyTheme();
            return EventResult.HANDLED;
        }
        if (event.isChar('T')) {
            currentThemeIndex = (currentThemeIndex - 1 + THEME_NAMES.length) % THEME_NAMES.length;
            applyTheme();
            return EventResult.HANDLED;
        }
        // List navigation
        if (event.isUp()) {
            navList.selectPrevious();
            return EventResult.HANDLED;
        }
        if (event.isDown()) {
            navList.selectNext(listItems.size());
            return EventResult.HANDLED;
        }
        return EventResult.UNHANDLED;
    }

    private void applyTheme() {
        themeEngine.setTheme(THEME_NAMES[currentThemeIndex]);
        themeEngine.injectVariables(styleEngine);
    }

    private static int indexOf(String name) {
        for (int i = 0; i < THEME_NAMES.length; i++) {
            if (THEME_NAMES[i].equals(name)) {
                return i;
            }
        }
        return 0;
    }
}
