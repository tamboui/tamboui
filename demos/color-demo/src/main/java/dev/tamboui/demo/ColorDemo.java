///usr/bin/env jbang "$0" "$@" ; exit $?
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
import dev.tamboui.style.AnsiColor;
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
import dev.tamboui.widgets.paragraph.Paragraph;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Demonstrates all color modes supported by TamboUI:
 * <ul>
 *   <li>ANSI 16 colors (8 normal + 8 bright)</li>
 *   <li>256-color indexed palette</li>
 *   <li>RGB true color (24-bit)</li>
 * </ul>
 */
public class ColorDemo {

    private boolean running = true;

    public static void main(String[] args) throws Exception {
        new ColorDemo().run();
    }

    public void run() throws Exception {
        try (Backend backend = BackendFactory.create()) {
            backend.enableRawMode();
            backend.enterAlternateScreen();
            backend.hideCursor();

            Terminal<Backend> terminal = new Terminal<>(backend);

            backend.onResize(() -> {
                try {
                    terminal.draw(this::render);
                } catch (IOException e) {
                    // Ignore
                }
            });

            while (running) {
                terminal.draw(this::render);

                int c = backend.read(100);
                if (c == 'q' || c == 'Q' || c == 3) {
                    running = false;
                }
            }
        }
    }

    private void render(Frame frame) {
        Rect area = frame.area();

        List<Rect> rows = Layout.vertical().constraints(
                Constraint.length(1),  // Title
                Constraint.length(10), // ANSI colors
                Constraint.length(12), // Indexed colors
                Constraint.length(12), // RGB colors
                Constraint.length(1),  // Footer
                Constraint.fill()
        ).split(area);

        renderTitle(frame, rows.get(0));
        renderAnsiColors(frame, rows.get(1));
        renderIndexedColors(frame, rows.get(2));
        renderRgbColors(frame, rows.get(3));
        renderFooter(frame, rows.get(4));
    }

    private void renderTitle(Frame frame, Rect area) {
        Paragraph title = Paragraph.builder()
                .text(Text.from(Line.from(
                        Span.styled("TamboUI Color Modes Demo", Style.EMPTY.fg(Color.CYAN).bold())
                )))
                .alignment(dev.tamboui.layout.Alignment.CENTER)
                .build();
        frame.renderWidget(title, area);
    }

    private void renderAnsiColors(Frame frame, Rect area) {
        List<Line> lines = new ArrayList<>();
        lines.add(Line.from(Span.styled("ANSI 16 Colors", Style.EMPTY.bold())));

        // Normal colors (0-7)
        List<Span> normalRow = new ArrayList<>();
        normalRow.add(Span.styled("Normal: ", Style.EMPTY));
        for (int i = 0; i < 8; i++) {
            AnsiColor ansiColor = AnsiColor.values()[i];
            Color color = Color.ansi(ansiColor);
            normalRow.add(Span.styled("  ", Style.EMPTY.bg(color)));
            normalRow.add(Span.styled(" " + ansiColor.name().toLowerCase().replace("_", "-"), Style.EMPTY.fg(color)));
        }
        lines.add(Line.from(normalRow));

        // Bright colors (8-15)
        List<Span> brightRow = new ArrayList<>();
        brightRow.add(Span.styled("Bright: ", Style.EMPTY));
        for (int i = 8; i < 16; i++) {
            AnsiColor ansiColor = AnsiColor.values()[i];
            Color color = Color.ansi(ansiColor);
            brightRow.add(Span.styled("  ", Style.EMPTY.bg(color)));
            brightRow.add(Span.styled(" " + ansiColor.name().toLowerCase().replace("_", "-"), Style.EMPTY.fg(color)));
        }
        lines.add(Line.from(brightRow));

        // Foreground examples
        lines.add(Line.from(Span.raw("")));
        lines.add(Line.from(Span.styled("Foreground examples:", Style.EMPTY.bold())));
        List<Span> fgRow = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            AnsiColor ansiColor = AnsiColor.values()[i];
            Color color = Color.ansi(ansiColor);
            fgRow.add(Span.styled("█", Style.EMPTY.fg(color)));
        }
        lines.add(Line.from(fgRow));

        // Background examples
        lines.add(Line.from(Span.styled("Background examples:", Style.EMPTY.bold())));
        List<Span> bgRow = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            AnsiColor ansiColor = AnsiColor.values()[i];
            Color color = Color.ansi(ansiColor);
            bgRow.add(Span.styled(" ", Style.EMPTY.bg(color)));
        }
        lines.add(Line.from(bgRow));

        Paragraph p = Paragraph.builder()
                .text(Text.from(lines))
                .block(Block.builder()
                        .borders(Borders.ALL)
                        .borderType(BorderType.ROUNDED)
                        .title(" ANSI Colors ")
                        .build())
                .build();
        frame.renderWidget(p, area);
    }

    private void renderIndexedColors(Frame frame, Rect area) {
        List<Line> lines = new ArrayList<>();
        lines.add(Line.from(Span.styled("256-Color Indexed Palette", Style.EMPTY.bold())));

        // Show ANSI colors (0-15)
        lines.add(Line.from(Span.styled("Standard ANSI indexed(0-15):", Style.EMPTY.bold())));
        List<Span> ansiRow = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            Color color = Color.indexed(i);
            ansiRow.add(Span.styled("█", Style.EMPTY.fg(color)));
        }
        lines.add(Line.from(ansiRow));

        // Show 6x6x6 color cube (16-231) - sample
        lines.add(Line.from(Span.raw("")));
        lines.add(Line.from(Span.styled("6x6x6 Color Cube indexed(16-231) - Sample:", Style.EMPTY.bold())));
        for (int r = 0; r < 6; r++) {
            List<Span> cubeRow = new ArrayList<>();
            for (int g = 0; g < 6; g++) {
                for (int b = 0; b < 6; b++) {
                    int index = 16 + r * 36 + g * 6 + b;
                    Color color = Color.indexed(index);
                    cubeRow.add(Span.styled("█", Style.EMPTY.fg(color)));
                }
                cubeRow.add(Span.raw(" "));
            }
            lines.add(Line.from(cubeRow));
        }

        // Show grayscale ramp (232-255)
        lines.add(Line.from(Span.raw("")));
        lines.add(Line.from(Span.styled("Grayscale Ramp (232-255):", Style.EMPTY.bold())));
        List<Span> grayRow = new ArrayList<>();
        for (int i = 232; i < 256; i++) {
            Color color = Color.indexed(i);
            grayRow.add(Span.styled("█", Style.EMPTY.bg(color)));
        }
        lines.add(Line.from(grayRow));

        Paragraph p = Paragraph.builder()
                .text(Text.from(lines))
                .block(Block.builder()
                        .borders(Borders.ALL)
                        .borderType(BorderType.ROUNDED)
                        .title(" Indexed Colors ")
                        .build())
                .build();
        frame.renderWidget(p, area);
    }

    private void renderRgbColors(Frame frame, Rect area) {
        List<Line> lines = new ArrayList<>();
        lines.add(Line.from(Span.styled("RGB True Color (24-bit)", Style.EMPTY.bold())));

        // Show primary colors
        lines.add(Line.from(Span.styled("Primary Colors:", Style.EMPTY.bold())));
        List<Span> primaryRow = new ArrayList<>();
        primaryRow.add(Span.styled("Red:   ", Style.EMPTY));
        primaryRow.add(Span.styled("████████████████", Style.EMPTY.fg(Color.rgb(255, 0, 0))));
        primaryRow.add(Span.styled("  Green: ", Style.EMPTY));
        primaryRow.add(Span.styled("████████████████", Style.EMPTY.fg(Color.rgb(0, 255, 0))));
        primaryRow.add(Span.styled("  Blue: ", Style.EMPTY));
        primaryRow.add(Span.styled("████████████████", Style.EMPTY.fg(Color.rgb(0, 0, 255))));
        lines.add(Line.from(primaryRow));

        // Show color gradients
        lines.add(Line.from(Span.raw("")));
        lines.add(Line.from(Span.styled("Red Gradient (0-255):", Style.EMPTY.bold())));
        List<Span> redGradient = new ArrayList<>();
        for (int i = 0; i < 32; i++) {
            int value = i * 8;
            Color color = Color.rgb(value, 0, 0);
            redGradient.add(Span.styled("█", Style.EMPTY.fg(color)));
        }
        lines.add(Line.from(redGradient));

        lines.add(Line.from(Span.styled("Green Gradient (0-255):", Style.EMPTY.bold())));
        List<Span> greenGradient = new ArrayList<>();
        for (int i = 0; i < 32; i++) {
            int value = i * 8;
            Color color = Color.rgb(0, value, 0);
            greenGradient.add(Span.styled("█", Style.EMPTY.fg(color)));
        }
        lines.add(Line.from(greenGradient));

        lines.add(Line.from(Span.styled("Blue Gradient (0-255):", Style.EMPTY.bold())));
        List<Span> blueGradient = new ArrayList<>();
        for (int i = 0; i < 32; i++) {
            int value = i * 8;
            Color color = Color.rgb(0, 0, value);
            blueGradient.add(Span.styled("█", Style.EMPTY.fg(color)));
        }
        lines.add(Line.from(blueGradient));

        // Show rainbow gradient
        lines.add(Line.from(Span.raw("")));
        lines.add(Line.from(Span.styled("Rainbow Gradient (HSV):", Style.EMPTY.bold())));
        List<Span> rainbow = new ArrayList<>();
        int width = Math.min(64, area.width() - 4);
        for (int i = 0; i < width; i++) {
            float hue = i * 360.0f / width;
            Color.Rgb rgb = hsvToRgb(hue, 1.0f, 1.0f);
            rainbow.add(Span.styled("█", Style.EMPTY.fg(rgb)));
        }
        lines.add(Line.from(rainbow));

        // Show hex color examples
        lines.add(Line.from(Span.raw("")));
        lines.add(Line.from(Span.styled("Hex Color Examples:", Style.EMPTY.bold())));
        List<Span> hexRow = new ArrayList<>();
        String[] hexColors = {"#FF0000", "#00FF00", "#0000FF", "#FFFF00", "#FF00FF", "#00FFFF", "#FFA500", "#800080"};
        for (String hex : hexColors) {
            Color color = Color.hex(hex);
            hexRow.add(Span.styled("████", Style.EMPTY.fg(color)));
            hexRow.add(Span.styled(" " + hex + " ", Style.EMPTY.fg(color)));
        }
        lines.add(Line.from(hexRow));

        Paragraph p = Paragraph.builder()
                .text(Text.from(lines))
                .block(Block.builder()
                        .borders(Borders.ALL)
                        .borderType(BorderType.ROUNDED)
                        .title(" RGB Colors ")
                        .build())
                .build();
        frame.renderWidget(p, area);
    }

    private void renderFooter(Frame frame, Rect area) {
        Paragraph footer = Paragraph.builder()
                .text(Text.from(Line.from(
                        Span.styled("q", Style.EMPTY.fg(Color.YELLOW).bold()),
                        Span.styled(" Quit", Style.EMPTY.fg(Color.DARK_GRAY))
                )))
                .build();
        frame.renderWidget(footer, area);
    }

    /**
     * Converts HSV color to RGB.
     *
     * @param h hue in degrees (0-360)
     * @param s saturation (0-1)
     * @param v value/brightness (0-1)
     * @return RGB color
     */
    private Color.Rgb hsvToRgb(float h, float s, float v) {
        float c = v * s;
        float hPrime = h / 60.0f;
        float x = c * (1 - Math.abs((hPrime % 2) - 1));
        float m = v - c;

        float r, g, b;
        if (hPrime < 1) {
            r = c;
            g = x;
            b = 0;
        } else if (hPrime < 2) {
            r = x;
            g = c;
            b = 0;
        } else if (hPrime < 3) {
            r = 0;
            g = c;
            b = x;
        } else if (hPrime < 4) {
            r = 0;
            g = x;
            b = c;
        } else if (hPrime < 5) {
            r = x;
            g = 0;
            b = c;
        } else {
            r = c;
            g = 0;
            b = x;
        }

        int red = Math.round((r + m) * 255);
        int green = Math.round((g + m) * 255);
        int blue = Math.round((b + m) * 255);

        // Clamp to valid range
        red = Math.max(0, Math.min(255, red));
        green = Math.max(0, Math.min(255, green));
        blue = Math.max(0, Math.min(255, blue));

        return new Color.Rgb(red, green, blue);
    }
}
