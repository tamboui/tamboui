///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS dev.tamboui:tamboui-toolkit:LATEST
//DEPS dev.tamboui:tamboui-jline3-backend:LATEST
/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import dev.tamboui.widgets.canvas.shapes.Line;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Layout;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Backend;
import dev.tamboui.terminal.BackendFactory;
import dev.tamboui.terminal.Frame;
import dev.tamboui.terminal.Terminal;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.widgets.block.Title;
import dev.tamboui.widgets.canvas.Canvas;
import dev.tamboui.widgets.canvas.Marker;
import dev.tamboui.widgets.canvas.shapes.Circle;
import dev.tamboui.widgets.canvas.shapes.Points;
import dev.tamboui.widgets.canvas.shapes.Rectangle;
import dev.tamboui.widgets.paragraph.Paragraph;

import java.io.IOException;
import java.util.Random;

/**
 * Demo TUI application showcasing the Canvas widget.
 * <p>
 * Demonstrates drawing shapes (lines, rectangles, circles, points)
 * with different markers (braille, half-block, dot, block).
 */
public class CanvasDemo {

    private boolean running = true;
    private final Random random = new Random();
    private long frameCount = 0;
    private double angle = 0;
    private int currentMarker = 0;
    private final Marker[] markers = Marker.values();

    // Bouncing ball state
    private double ballX = 50;
    private double ballY = 50;
    private double ballVx = 2;
    private double ballVy = 1.5;

    // Stars for background
    private final double[][] stars = new double[50][2];

    /**
     * Demo entry point.
     * @param args the CLI arguments
     * @throws Exception on unexpected error
     */
    public static void main(String[] args) throws Exception {
        new CanvasDemo().run();
    }

    private CanvasDemo() {
        // Initialize random stars
        for (int i = 0; i < stars.length; i++) {
            stars[i][0] = random.nextDouble() * 100;
            stars[i][1] = random.nextDouble() * 100;
        }
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
                try {
                    terminal.draw(this::ui);
                } catch (IOException e) {
                    // Ignore
                }
            });

            // Event loop with animation
            while (running) {
                terminal.draw(this::ui);

                int c = backend.read(50);
                if (c == 'q' || c == 'Q' || c == 3) {
                    running = false;
                } else if (c == ' ' || c == 'm' || c == 'M') {
                    // Cycle through markers
                    currentMarker = (currentMarker + 1) % markers.length;
                }

                // Update animation state
                updateAnimation();
                frameCount++;
            }
        }
    }

    private void updateAnimation() {
        // Update rotation angle
        angle += 0.05;
        if (angle > 2 * Math.PI) {
            angle -= 2 * Math.PI;
        }

        // Update bouncing ball
        ballX += ballVx;
        ballY += ballVy;

        if (ballX <= 5 || ballX >= 95) {
            ballVx = -ballVx;
            ballX = Math.max(5, Math.min(95, ballX));
        }
        if (ballY <= 5 || ballY >= 95) {
            ballVy = -ballVy;
            ballY = Math.max(5, Math.min(95, ballY));
        }

        // Slowly move some stars
        for (int i = 0; i < 10; i++) {
            stars[i][0] += 0.1;
            if (stars[i][0] > 100) {
                stars[i][0] = 0;
                stars[i][1] = random.nextDouble() * 100;
            }
        }
    }

    private void ui(Frame frame) {
        Rect area = frame.area();

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
        Block headerBlock = Block.builder()
            .borders(Borders.ALL)
            .borderType(BorderType.ROUNDED)
            .borderStyle(Style.EMPTY.fg(Color.CYAN))
            .title(Title.from(
                dev.tamboui.text.Line.from(
                    Span.raw(" TamboUI ").bold().cyan(),
                    Span.raw("Canvas Demo ").yellow(),
                    Span.raw("[Marker: " + markers[currentMarker].name() + "]").dim()
                )
            ).centered())
            .build();

        frame.renderWidget(headerBlock, area);
    }

    private void renderMainContent(Frame frame, Rect area) {
        // Split into left (main canvas) and right (shapes demo)
        var cols = Layout.horizontal()
            .constraints(
                Constraint.percentage(60),
                Constraint.percentage(40)
            )
            .split(area);

        renderMainCanvas(frame, cols.get(0));

        // Split right side into two canvases
        var rightRows = Layout.vertical()
            .constraints(
                Constraint.percentage(50),
                Constraint.percentage(50)
            )
            .split(cols.get(1));

        renderShapesCanvas(frame, rightRows.get(0));
        renderAnimatedCanvas(frame, rightRows.get(1));
    }

    private void renderMainCanvas(Frame frame, Rect area) {
        Marker marker = markers[currentMarker];

        var canvas = Canvas.builder()
            .xBounds(0, 100)
            .yBounds(0, 100)
            .marker(marker)
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.BLUE))
                .title(Title.from(dev.tamboui.text.Line.from(
                    Span.raw(" Space Scene ").blue()
                )))
                .build())
            .paint(ctx -> {
                // Draw stars as points
                ctx.draw(new Points(stars, Color.WHITE));

                // Draw a spinning planet (circle)
                double planetX = 50 + 30 * Math.cos(angle);
                double planetY = 50 + 30 * Math.sin(angle * 0.7);
                ctx.draw(new Circle(planetX, planetY, 8, Color.CYAN));

                // Draw a satellite orbiting the planet
                double satX = planetX + 15 * Math.cos(angle * 3);
                double satY = planetY + 15 * Math.sin(angle * 3);
                ctx.draw(new Circle(satX, satY, 2, Color.YELLOW));

                // Draw orbit path
                ctx.draw(new Circle(50, 50, 30, Color.DARK_GRAY));

                // Draw bouncing ball
                ctx.draw(new Circle(ballX, ballY, 5, Color.RED));

                // Draw trail
                for (int i = 1; i <= 5; i++) {
                    double trailX = ballX - ballVx * i * 2;
                    double trailY = ballY - ballVy * i * 2;
                    ctx.draw(new Circle(trailX, trailY, 5 - i, Color.MAGENTA));
                }

                // Add label
                ctx.print(50, 95, Span.styled("Center", Style.EMPTY.fg(Color.GREEN)));
            })
            .build();

        frame.renderWidget(canvas, area);
    }

    private void renderShapesCanvas(Frame frame, Rect area) {
        Marker marker = markers[currentMarker];

        var canvas = Canvas.builder()
            .xBounds(0, 100)
            .yBounds(0, 100)
            .marker(marker)
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.GREEN))
                .title(Title.from(dev.tamboui.text.Line.from(
                    Span.raw(" Shapes ").green()
                )))
                .build())
            .paint(ctx -> {
                // Rectangle
                ctx.draw(new Rectangle(10, 10, 30, 20, Color.RED));

                // Circle
                ctx.draw(new Circle(70, 30, 15, Color.BLUE));

                // Lines
                ctx.draw(new Line(10, 60, 40, 90, Color.YELLOW));
                ctx.draw(new Line(40, 60, 10, 90, Color.YELLOW));

                // Points
                double[][] pointData = {
                    {60, 70}, {65, 75}, {70, 70}, {75, 75},
                    {80, 70}, {85, 75}, {90, 70}
                };
                ctx.draw(new Points(pointData, Color.MAGENTA));
            })
            .build();

        frame.renderWidget(canvas, area);
    }

    private void renderAnimatedCanvas(Frame frame, Rect area) {
        Marker marker = markers[currentMarker];

        var canvas = Canvas.builder()
            .xBounds(-50, 50)
            .yBounds(-50, 50)
            .marker(marker)
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.MAGENTA))
                .title(Title.from(dev.tamboui.text.Line.from(
                    Span.raw(" Rotating ").magenta()
                )))
                .build())
            .paint(ctx -> {
                // Draw rotating lines
                for (int i = 0; i < 8; i++) {
                    double a = angle + i * Math.PI / 4;
                    double x1 = 30 * Math.cos(a);
                    double y1 = 30 * Math.sin(a);
                    Color color = switch (i % 4) {
                        case 0 -> Color.RED;
                        case 1 -> Color.GREEN;
                        case 2 -> Color.BLUE;
                        default -> Color.YELLOW;
                    };
                    ctx.draw(new Line(0, 0, x1, y1, color));
                }

                // Draw center circle
                ctx.draw(new Circle(0, 0, 5, Color.WHITE));

                // Draw outer ring
                ctx.draw(new Circle(0, 0, 40, Color.CYAN));
            })
            .build();

        frame.renderWidget(canvas, area);
    }

    private void renderFooter(Frame frame, Rect area) {
        dev.tamboui.text.Line helpLine = dev.tamboui.text.Line.from(
            Span.raw(" Frame: ").dim(),
            Span.raw(String.valueOf(frameCount)).bold().cyan(),
            Span.raw("   "),
            Span.raw("m").bold().yellow(),
            Span.raw(" Change Marker").dim(),
            Span.raw("   "),
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
