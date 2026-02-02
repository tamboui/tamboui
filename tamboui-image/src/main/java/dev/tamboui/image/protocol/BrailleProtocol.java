/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.image.protocol;

import java.io.OutputStream;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.image.ImageData;
import dev.tamboui.image.capability.TerminalImageProtocol;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;

/**
 * Renders images using Unicode Braille patterns.
 * <p>
 * Each terminal cell can display a 2x4 grid of Braille dots (8 dots total),
 * providing the highest character-based resolution at 2x4 virtual pixels per cell.
 * <p>
 * Braille patterns use Unicode characters from U+2800 to U+28FF.
 * The 8 dots are mapped to bits as follows:
 * <pre>
 * Dot positions:  Bit values:
 *   1 4           0x01 0x08
 *   2 5           0x02 0x10
 *   3 6           0x04 0x20
 *   7 8           0x40 0x80
 * </pre>
 * <p>
 * Uses adaptive percentile-based thresholding to determine which pixels are "on".
 * By default, the brightest 75% of pixels are shown as dots.
 */
public final class BrailleProtocol implements ImageProtocol {

    /** Unicode code point for the empty Braille pattern. */
    private static final int BRAILLE_BASE = 0x2800;

    /**
     * Braille dot bit values.
     * <p>
     * Index [column][row] where column is 0-1 and row is 0-3.
     */
    private static final int[][] BRAILLE_DOTS = {
        {0x01, 0x02, 0x04, 0x40},  // Column 0: bits for rows 0-3
        {0x08, 0x10, 0x20, 0x80}   // Column 1: bits for rows 0-3
    };

    private static final Resolution RESOLUTION = new Resolution(2, 4);

    /** Percentile threshold (0-100) - pixels above this percentile are "on". */
    private final int percentile;

    /**
     * Creates a new Braille protocol instance with default percentile threshold.
     * <p>
     * By default, the brightest 75% of pixels are shown as dots (25th percentile).
     */
    public BrailleProtocol() {
        this(25);
    }

    /**
     * Creates a new Braille protocol instance with a custom percentile threshold.
     *
     * @param percentile percentile (0-100) - pixels with luminance above this percentile are "on"
     */
    public BrailleProtocol(int percentile) {
        this.percentile = Math.max(0, Math.min(100, percentile));
    }

    @Override
    public void render(ImageData image, Rect area, Buffer buffer, OutputStream rawOutput) {
        if (area.isEmpty()) {
            return;
        }

        // Calculate grid dimensions (2x4 resolution)
        int gridWidth = area.width() * 2;
        int gridHeight = area.height() * 4;

        // Scale image to fit the grid
        ImageData scaled = image.resize(gridWidth, gridHeight);

        // Calculate adaptive threshold based on percentile
        int threshold = calculatePercentileThreshold(scaled);

        // Render each cell
        for (int cellY = 0; cellY < area.height(); cellY++) {
            for (int cellX = 0; cellX < area.width(); cellX++) {
                int pattern = 0;
                int colorCount = 0;
                long rSum = 0, gSum = 0, bSum = 0;

                // Check all 8 dots in this cell (2 columns, 4 rows)
                for (int dy = 0; dy < 4; dy++) {
                    for (int dx = 0; dx < 2; dx++) {
                        int gridX = cellX * 2 + dx;
                        int gridY = cellY * 4 + dy;
                        int pixel = scaled.pixelAt(gridX, gridY);

                        if (isPixelOn(pixel, threshold)) {
                            pattern |= BRAILLE_DOTS[dx][dy];
                            // Accumulate color for averaging
                            rSum += ImageData.red(pixel);
                            gSum += ImageData.green(pixel);
                            bSum += ImageData.blue(pixel);
                            colorCount++;
                        }
                    }
                }

                if (pattern != 0) {
                    // Use average color of all "on" pixels
                    Color cellColor = null;
                    if (colorCount > 0) {
                        int r = (int) (rSum / colorCount);
                        int g = (int) (gSum / colorCount);
                        int b = (int) (bSum / colorCount);
                        cellColor = Color.rgb(r, g, b);
                    }

                    String brailleChar = String.valueOf((char) (BRAILLE_BASE + pattern));
                    Style style = cellColor != null ? Style.EMPTY.fg(cellColor) : Style.EMPTY;
                    buffer.setString(area.x() + cellX, area.y() + cellY, brailleChar, style);
                }
            }
        }
    }

    /**
     * Calculates the luminance threshold based on the configured percentile.
     * <p>
     * Builds a histogram of luminance values and finds the value at the given percentile.
     */
    private int calculatePercentileThreshold(ImageData image) {
        // Build luminance histogram (256 buckets)
        int[] histogram = new int[256];
        int totalPixels = 0;

        for (int y = 0; y < image.height(); y++) {
            for (int x = 0; x < image.width(); x++) {
                int pixel = image.pixelAt(x, y);
                if (ImageData.isVisible(pixel)) {
                    int luminance = luminanceFor(pixel);
                    histogram[luminance]++;
                    totalPixels++;
                }
            }
        }

        if (totalPixels == 0) {
            return 128; // Fallback
        }

        // Find the luminance value at the given percentile
        int targetCount = (totalPixels * percentile) / 100;
        int count = 0;
        for (int i = 0; i < 256; i++) {
            count += histogram[i];
            if (count >= targetCount) {
                return i;
            }
        }
        return 255;
    }

    @Override
    public boolean requiresRawOutput() {
        return false;
    }

    @Override
    public Resolution resolution() {
        return RESOLUTION;
    }

    @Override
    public String name() {
        return "Braille";
    }

    @Override
    public TerminalImageProtocol protocolType() {
        return TerminalImageProtocol.BRAILLE;
    }

    /**
     * Calculates the luminance of a pixel.
     * <p>
     * Uses the standard luminance formula: 0.299*R + 0.587*G + 0.114*B
     */
    private static int luminanceFor(int argb) {
        int r = ImageData.red(argb);
        int g = ImageData.green(argb);
        int b = ImageData.blue(argb);
        return (299 * r + 587 * g + 114 * b) / 1000;
    }

    /**
     * Determines if a pixel should be rendered as "on" (dot visible).
     *
     * @param argb the pixel color
     * @param threshold the luminance threshold
     * @return true if the pixel should be shown as a dot
     */
    private boolean isPixelOn(int argb, int threshold) {
        if (!ImageData.isVisible(argb)) {
            return false;
        }
        return luminanceFor(argb) >= threshold;
    }
}
