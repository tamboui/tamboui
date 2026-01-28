///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS dev.tamboui:tamboui-widgets:LATEST
//DEPS dev.tamboui:tamboui-jline3-backend:LATEST
/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Demo TUI application showcasing RGB color support.
 * <p>
 * Displays the full range of RGB colors in an animated gradient.
 * Uses half-block characters (▀) to display two rows of pixels per screen row.
 * <p>
 * Requires a terminal that supports 24-bit color (true color).
 */
public class ColorsRgbDemo {

    private boolean running = true;
    private final FpsWidget fpsWidget = new FpsWidget();
    private final ColorsWidget colorsWidget = new ColorsWidget();

    private ColorsRgbDemo() {

    }

    /**
     * Demo entry point.
     * @param args the CLI arguments
     * @throws Exception on unexpected error
     */
    public static void main(String[] args) throws Exception {
        new ColorsRgbDemo().run();
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

            // Event loop - target ~60 FPS
            // For high FPS, we check input less frequently to avoid blocking
            int frameCount = 0;
            while (running) {
                terminal.draw(this::ui);

                // Check for input every 10 frames (~6 times per second at 60 FPS)
                // This avoids blocking on every frame while still being responsive
                if (frameCount % 10 == 0) {
                    int c = backend.read(1); // 1ms timeout
                    if (c >= 0) {
                        if (c == 'q' || c == 'Q' || c == 3) {
                            running = false;
                        }
                    }
                }
                frameCount++;
            }
        }
    }

    private void ui(Frame frame) {
        Rect area = frame.area();

        var layout = Layout.vertical()
            .constraints(
                Constraint.length(1),  // Title bar
                Constraint.fill()      // Colors
            )
            .split(area);

        var titleLayout = Layout.horizontal()
            .constraints(
                Constraint.fill(),     // Title
                Constraint.length(8)   // FPS
            )
            .split(layout.get(0));

        renderTitleBar(frame, titleLayout.get(0));
        fpsWidget.render(frame, titleLayout.get(1));
        colorsWidget.render(frame, layout.get(1));
    }

    private void renderTitleBar(Frame frame, Rect area) {
        Line titleLine = Line.from("colors_rgb example. Press q to quit").alignment(Alignment.CENTER);
        int titleX = area.left() + (area.width() - titleLine.width()) / 2;
        frame.buffer().setLine(titleX, area.top(), titleLine);
    }

    /**
     * Widget that displays the current frames per second.
     */
    private static class FpsWidget {
        private int frameCount = 0;
        private long lastTime = System.currentTimeMillis();
        private Float fps = null;

        public void render(Frame frame, Rect area) {
            calculateFps();
            if (fps != null) {
                String text = String.format("%.1f fps", fps);
                Line line = Line.from(text);
                frame.buffer().setLine(area.left(), area.top(), line);
            }
        }

        private void calculateFps() {
            frameCount++;
            long now = System.currentTimeMillis();
            long elapsed = now - lastTime;
            if (elapsed > 1000 && frameCount > 2) {
                fps = (float) frameCount / (elapsed / 1000.0f);
                frameCount = 0;
                lastTime = now;
            }
        }
    }

    /**
     * Widget that displays the full range of RGB colors.
     * <p>
     * Uses half-block characters (▀) to display two rows of pixels per screen row.
     */
    private static class ColorsWidget {
        private final List<List<Color.Rgb>> colors = new ArrayList<>();
        private int frameCount = 0;

        public void render(Frame frame, Rect area) {
            setupColors(area);
            Buffer buffer = frame.buffer();

            int width = area.width();
            int height = area.height();
            int areaX = area.x();
            int areaY = area.y();
            
            // Pre-calculate the symbol string to avoid repeated string creation
            String halfBlock = "▀";
            
            // Pre-calculate modulo for animation
            int frameMod = frameCount % width;
            
            // Optimize: cache color lists to avoid repeated get() calls
            // Reorder loops to cache row lookups
            for (int y = 0; y < height; y++) {
                int colorY = y * 2;
                List<Color.Rgb> fgRow = colors.get(colorY);
                List<Color.Rgb> bgRow = colors.get(colorY + 1);
                
                for (int x = 0; x < width; x++) {
                    // Animate the colors by shifting the x index by the frame number
                    int xi = (x + frameMod) % width;
                    
                    // Render a half block character for each row of pixels with the foreground color
                    // set to the color of the pixel and the background color set to the color of the
                    // pixel below it
                    Color.Rgb fg = fgRow.get(xi);
                    Color.Rgb bg = bgRow.get(xi);
                    
                    // Create style and cell directly
                    Style style = Style.EMPTY.fg(fg).bg(bg);
                    Cell cell = new Cell(halfBlock, style);
                    buffer.set(areaX + x, areaY + y, cell);
                }
            }
            frameCount++;
        }

        private void setupColors(Rect area) {
            int width = area.width();
            int height = area.height() * 2; // Double height because each screen row has two rows of half block pixels

            // Only update the colors if the size has changed
            if (colors.size() == height && !colors.isEmpty() && colors.get(0).size() == width) {
                return;
            }

            colors.clear();
            for (int y = 0; y < height; y++) {
                List<Color.Rgb> row = new ArrayList<>();
                for (int x = 0; x < width; x++) {
                    // Generate colors using HSV color space
                    // Hue varies from 0 to 360 across the width
                    // Value (brightness) varies from 1.0 to 0.0 from top to bottom
                    // Saturation is always maximum (1.0)
                    float hue = x * 360.0f / width;
                    float value = (height - y) / (float) height;
                    float saturation = 1.0f;

                    // Convert HSV to RGB
                    Color.Rgb rgb = hsvToRgb(hue, saturation, value);
                    row.add(rgb);
                }
                colors.add(row);
            }
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
}

