///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS dev.tamboui:tamboui-core:LATEST
//DEPS dev.tamboui:tamboui-widgets:LATEST
//DEPS dev.tamboui:tamboui-jline3-backend:LATEST

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Layout;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.style.StylePropertyResolver;
import dev.tamboui.terminal.Backend;
import dev.tamboui.terminal.BackendFactory;
import dev.tamboui.terminal.Frame;
import dev.tamboui.terminal.Terminal;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;
import dev.tamboui.theme.Theme;
import dev.tamboui.theme.ThemeProperties;
import dev.tamboui.theme.ThemeRegistry;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.widgets.block.Title;
import dev.tamboui.widgets.gauge.Gauge;
import dev.tamboui.widgets.paragraph.Paragraph;

/**
 * Demo TUI application showcasing the Theme system.
 * <p>
 * Demonstrates:
 * <ul>
 *   <li>All 7 built-in themes</li>
 *   <li>Live theme switching with number keys</li>
 *   <li>Semantic color usage (primary, error, success, etc.)</li>
 *   <li>Auto-terminal theme adapting to user's terminal colors</li>
 * </ul>
 */
public class ThemeDemo {

    private static final String[] THEME_NAMES = {
        "auto-terminal",
        "tamboui-default",
        "nord",
        "gruvbox-dark",
        "dracula",
        "catppuccin-mocha",
        "tokyo-night"
    };

    private boolean running = true;
    private int currentThemeIndex = 0;
    private Theme currentTheme;
    private StylePropertyResolver themeResolver;

    private ThemeDemo() {
        loadTheme(0);
    }

    /**
     * Demo entry point.
     * @param args the CLI arguments
     * @throws Exception on unexpected error
     */
    public static void main(String[] args) throws Exception {
        new ThemeDemo().run();
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

            backend.onResize(() -> {
                terminal.draw(this::ui);
            });

            while (running) {
                terminal.draw(this::ui);

                int c = backend.read(100);
                switch (c) {
                    case 'q', 'Q', 3 -> running = false;
                    case '1' -> loadTheme(0);
                    case '2' -> loadTheme(1);
                    case '3' -> loadTheme(2);
                    case '4' -> loadTheme(3);
                    case '5' -> loadTheme(4);
                    case '6' -> loadTheme(5);
                    case '7' -> loadTheme(6);
                    case 'n', 'N' -> loadTheme((currentThemeIndex + 1) % THEME_NAMES.length);
                    case 'p', 'P' -> loadTheme((currentThemeIndex - 1 + THEME_NAMES.length) % THEME_NAMES.length);
                }
            }
        }
    }

    private void loadTheme(int index) {
        currentThemeIndex = index;
        currentTheme = ThemeRegistry.get(THEME_NAMES[index])
            .orElseThrow(() -> new IllegalStateException("Theme not found: " + THEME_NAMES[index]));
        themeResolver = currentTheme.toResolver();
    }

    private void ui(Frame frame) {
        Rect area = frame.area();

        var layout = Layout.vertical()
            .constraints(
                Constraint.length(3),
                Constraint.fill(),
                Constraint.length(6)
            )
            .split(area);

        renderHeader(frame, layout.get(0));
        renderWidgets(frame, layout.get(1));
        renderFooter(frame, layout.get(2));
    }

    private void renderHeader(Frame frame, Rect area) {
        Color primary = themeResolver.get(ThemeProperties.PRIMARY).orElse(Color.CYAN);

        Block headerBlock = Block.builder()
            .borders(Borders.ALL)
            .borderType(BorderType.ROUNDED)
            .borderStyle(Style.EMPTY.fg(primary))
            .title(Title.from(
                Line.from(
                    Span.raw(" TamboUI ").bold().fg(primary),
                    Span.raw("Theme Demo ").fg(themeResolver.get(ThemeProperties.SECONDARY).orElse(Color.YELLOW)),
                    Span.raw("[" + THEME_NAMES[currentThemeIndex] + "]").dim()
                )
            ).centered())
            .build();

        frame.renderWidget(headerBlock, area);
    }

    private void renderWidgets(Frame frame, Rect area) {
        var layout = Layout.vertical()
            .constraints(
                Constraint.percentage(33),
                Constraint.percentage(33),
                Constraint.percentage(34)
            )
            .split(area);

        var topLayout = Layout.horizontal()
            .constraints(
                Constraint.percentage(50),
                Constraint.percentage(50)
            )
            .split(layout.get(0));

        renderNotificationBlocks(frame, topLayout.get(0));
        renderProgressGauges(frame, topLayout.get(1));
        renderListWidget(frame, layout.get(1));
        renderTextStyles(frame, layout.get(2));
    }

    private void renderNotificationBlocks(Frame frame, Rect area) {
        Color borderColor = themeResolver.get(ThemeProperties.BORDER).orElse(Color.DARK_GRAY);

        var vLayout = Layout.vertical()
            .constraints(
                Constraint.length(5),
                Constraint.length(5),
                Constraint.length(5),
                Constraint.fill()
            )
            .split(area);

        // Success notification
        Color success = themeResolver.get(ThemeProperties.SUCCESS).orElse(Color.GREEN);
        Block successBlock = Block.builder()
            .borders(Borders.ALL)
            .borderType(BorderType.ROUNDED)
            .borderStyle(Style.EMPTY.fg(success))
            .title(Title.from(Line.from(Span.raw(" ✓ Success ").bold().fg(success))))
            .build();
        frame.renderWidget(successBlock, vLayout.get(0));
        Paragraph successText = Paragraph.builder()
            .text(Text.from(
                Line.from(Span.raw(" Operation completed").fg(themeResolver.get(ThemeProperties.TEXT).orElse(Color.WHITE))),
                Line.from(Span.raw(" successfully!").fg(themeResolver.get(ThemeProperties.TEXT).orElse(Color.WHITE)))
            ))
            .build();
        frame.renderWidget(successText, successBlock.inner(vLayout.get(0)));

        // Warning notification
        Color warning = themeResolver.get(ThemeProperties.WARNING).orElse(Color.YELLOW);
        Block warningBlock = Block.builder()
            .borders(Borders.ALL)
            .borderType(BorderType.ROUNDED)
            .borderStyle(Style.EMPTY.fg(warning))
            .title(Title.from(Line.from(Span.raw(" ⚠ Warning ").bold().fg(warning))))
            .build();
        frame.renderWidget(warningBlock, vLayout.get(1));
        Paragraph warningText = Paragraph.builder()
            .text(Text.from(
                Line.from(Span.raw(" Low disk space").fg(themeResolver.get(ThemeProperties.TEXT).orElse(Color.WHITE))),
                Line.from(Span.raw(" detected.").fg(themeResolver.get(ThemeProperties.TEXT).orElse(Color.WHITE)))
            ))
            .build();
        frame.renderWidget(warningText, warningBlock.inner(vLayout.get(1)));

        // Error notification
        Color error = themeResolver.get(ThemeProperties.ERROR).orElse(Color.RED);
        Block errorBlock = Block.builder()
            .borders(Borders.ALL)
            .borderType(BorderType.ROUNDED)
            .borderStyle(Style.EMPTY.fg(error))
            .title(Title.from(Line.from(Span.raw(" ✗ Error ").bold().fg(error))))
            .build();
        frame.renderWidget(errorBlock, vLayout.get(2));
        Paragraph errorText = Paragraph.builder()
            .text(Text.from(
                Line.from(Span.raw(" Connection failed").fg(themeResolver.get(ThemeProperties.TEXT).orElse(Color.WHITE))),
                Line.from(Span.raw(" to server.").fg(themeResolver.get(ThemeProperties.TEXT).orElse(Color.WHITE)))
            ))
            .build();
        frame.renderWidget(errorText, errorBlock.inner(vLayout.get(2)));
    }

    private void renderProgressGauges(Frame frame, Rect area) {
        Color borderColor = themeResolver.get(ThemeProperties.BORDER).orElse(Color.DARK_GRAY);

        Block block = Block.builder()
            .borders(Borders.ALL)
            .borderType(BorderType.ROUNDED)
            .borderStyle(Style.EMPTY.fg(borderColor))
            .title(Title.from(Line.from(Span.raw(" Progress ").bold())))
            .build();

        frame.renderWidget(block, area);
        Rect inner = block.inner(area);

        var vLayout = Layout.vertical()
            .constraints(
                Constraint.length(4),
                Constraint.length(4),
                Constraint.length(4),
                Constraint.fill()
            )
            .split(inner);

        Color primary = themeResolver.get(ThemeProperties.PRIMARY).orElse(Color.CYAN);
        Color secondary = themeResolver.get(ThemeProperties.SECONDARY).orElse(Color.MAGENTA);
        Color accent = themeResolver.get(ThemeProperties.ACCENT).orElse(Color.YELLOW);

        // CPU gauge
        Gauge cpuGauge = Gauge.builder()
            .label("CPU")
            .ratio(0.75)
            .gaugeStyle(Style.EMPTY.fg(primary))
            .build();
        frame.renderWidget(cpuGauge, vLayout.get(0));

        // Memory gauge
        Gauge memGauge = Gauge.builder()
            .label("Memory")
            .ratio(0.45)
            .gaugeStyle(Style.EMPTY.fg(secondary))
            .build();
        frame.renderWidget(memGauge, vLayout.get(1));

        // Disk gauge
        Gauge diskGauge = Gauge.builder()
            .label("Disk")
            .ratio(0.90)
            .gaugeStyle(Style.EMPTY.fg(accent))
            .build();
        frame.renderWidget(diskGauge, vLayout.get(2));
    }

    private void renderListWidget(Frame frame, Rect area) {
        Color borderColor = themeResolver.get(ThemeProperties.BORDER).orElse(Color.DARK_GRAY);
        Color primary = themeResolver.get(ThemeProperties.PRIMARY).orElse(Color.CYAN);
        Color text = themeResolver.get(ThemeProperties.TEXT).orElse(Color.WHITE);
        Color success = themeResolver.get(ThemeProperties.SUCCESS).orElse(Color.GREEN);
        Color textMuted = themeResolver.get(ThemeProperties.TEXT_MUTED).orElse(Color.DARK_GRAY);

        Block block = Block.builder()
            .borders(Borders.ALL)
            .borderType(BorderType.ROUNDED)
            .borderStyle(Style.EMPTY.fg(borderColor))
            .title(Title.from(Line.from(Span.raw(" Task List ").bold())))
            .build();

        frame.renderWidget(block, area);
        Rect inner = block.inner(area);

        Paragraph taskList = Paragraph.builder()
            .text(Text.from(
                Line.from(
                    Span.raw(" ✓ ").fg(success),
                    Span.raw("Complete theme system core").fg(text)
                ),
                Line.from(
                    Span.raw(" ✓ ").fg(success),
                    Span.raw("Add CSS integration").fg(text)
                ),
                Line.from(
                    Span.raw(" ▶ ").fg(primary),
                    Span.raw("Build interactive demo").fg(text).bold()
                ),
                Line.from(
                    Span.raw("   ").fg(text),
                    Span.raw("Update documentation").fg(textMuted)
                ),
                Line.from(
                    Span.raw("   ").fg(text),
                    Span.raw("Add more themes").fg(textMuted)
                )
            ))
            .build();

        frame.renderWidget(taskList, inner);
    }

    private void renderTextStyles(Frame frame, Rect area) {
        Color borderColor = themeResolver.get(ThemeProperties.BORDER).orElse(Color.DARK_GRAY);

        Block block = Block.builder()
            .borders(Borders.ALL)
            .borderType(BorderType.ROUNDED)
            .borderStyle(Style.EMPTY.fg(borderColor))
            .title(Title.from(Line.from(Span.raw(" Text Styles ").bold())))
            .build();

        frame.renderWidget(block, area);
        Rect inner = block.inner(area);

        Color primary = themeResolver.get(ThemeProperties.PRIMARY).orElse(Color.CYAN);
        Color secondary = themeResolver.get(ThemeProperties.SECONDARY).orElse(Color.MAGENTA);
        Color text = themeResolver.get(ThemeProperties.TEXT).orElse(Color.WHITE);
        Color textMuted = themeResolver.get(ThemeProperties.TEXT_MUTED).orElse(Color.DARK_GRAY);
        Color accent = themeResolver.get(ThemeProperties.ACCENT).orElse(Color.YELLOW);

        Paragraph textDemo = Paragraph.builder()
            .text(Text.from(
                Line.from(
                    Span.raw("Primary: ").fg(textMuted),
                    Span.raw("The quick brown fox ").fg(primary).bold()
                ),
                Line.from(
                    Span.raw("Secondary: ").fg(textMuted),
                    Span.raw("jumps over the lazy ").fg(secondary).italic()
                ),
                Line.from(
                    Span.raw("Accent: ").fg(textMuted),
                    Span.raw("dog every morning ").fg(accent).underlined()
                ),
                Line.from(
                    Span.raw("Normal: ").fg(textMuted),
                    Span.raw("Regular text example ").fg(text)
                ),
                Line.from(
                    Span.raw("Muted: ").fg(textMuted),
                    Span.raw("Subtle text for hints ").fg(textMuted)
                )
            ))
            .build();

        frame.renderWidget(textDemo, inner);
    }

    private void renderFooter(Frame frame, Rect area) {
        Color textColor = themeResolver.get(ThemeProperties.TEXT).orElse(Color.WHITE);
        Color mutedColor = themeResolver.get(ThemeProperties.TEXT_MUTED).orElse(Color.DARK_GRAY);
        Color primaryColor = themeResolver.get(ThemeProperties.PRIMARY).orElse(Color.CYAN);

        Block block = Block.builder()
            .borders(Borders.ALL)
            .borderType(BorderType.ROUNDED)
            .borderStyle(Style.EMPTY.fg(themeResolver.get(ThemeProperties.BORDER).orElse(Color.DARK_GRAY)))
            .build();

        frame.renderWidget(block, area);
        Rect inner = block.inner(area);

        var vLayout = Layout.vertical()
            .constraints(
                Constraint.length(1),
                Constraint.length(1),
                Constraint.length(1),
                Constraint.length(1)
            )
            .split(inner);

        Line line1 = Line.from(
            Span.raw(" Themes: ").fg(textColor),
            Span.raw("1").bold().fg(primaryColor),
            Span.raw(" Auto  ").fg(mutedColor),
            Span.raw("2").bold().fg(primaryColor),
            Span.raw(" Default  ").fg(mutedColor),
            Span.raw("3").bold().fg(primaryColor),
            Span.raw(" Nord  ").fg(mutedColor),
            Span.raw("4").bold().fg(primaryColor),
            Span.raw(" Gruvbox  ").fg(mutedColor),
            Span.raw("5").bold().fg(primaryColor),
            Span.raw(" Dracula").fg(mutedColor)
        );

        Line line2 = Line.from(
            Span.raw("         ").fg(textColor),
            Span.raw("6").bold().fg(primaryColor),
            Span.raw(" Catppuccin  ").fg(mutedColor),
            Span.raw("7").bold().fg(primaryColor),
            Span.raw(" Tokyo Night").fg(mutedColor)
        );

        Line line3 = Line.from(
            Span.raw(" Navigate: ").fg(textColor),
            Span.raw("n").bold().fg(primaryColor),
            Span.raw("/").fg(mutedColor),
            Span.raw("p").bold().fg(primaryColor),
            Span.raw(" Next/Prev  ").fg(mutedColor),
            Span.raw("q").bold().fg(primaryColor),
            Span.raw(" Quit").fg(mutedColor)
        );

        Line line4 = Line.from(
            Span.raw(" Widgets showcase semantic colors, text styles, and theme-aware components").fg(mutedColor).dim()
        );

        Paragraph footer1 = Paragraph.builder().text(Text.from(line1)).build();
        Paragraph footer2 = Paragraph.builder().text(Text.from(line2)).build();
        Paragraph footer3 = Paragraph.builder().text(Text.from(line3)).build();
        Paragraph footer4 = Paragraph.builder().text(Text.from(line4)).build();

        frame.renderWidget(footer1, vLayout.get(0));
        frame.renderWidget(footer2, vLayout.get(1));
        frame.renderWidget(footer3, vLayout.get(2));
        frame.renderWidget(footer4, vLayout.get(3));
    }
}
