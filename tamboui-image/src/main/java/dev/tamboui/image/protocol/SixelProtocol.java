/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.image.protocol;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.error.RuntimeIOException;
import dev.tamboui.image.ImageData;
import dev.tamboui.image.capability.TerminalImageProtocol;
import dev.tamboui.layout.Rect;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Renders images using the Sixel graphics protocol.
 * <p>
 * Sixel is a DEC standard from the 1980s that encodes images as 6-pixel-high
 * horizontal strips. Each strip is encoded using ASCII characters where each
 * character represents a vertical column of 6 pixels.
 * <p>
 * Sixel is supported by: xterm (with configuration), mlterm, mintty, WezTerm,
 * Rio, Konsole (22+), and other terminals.
 *
 * <h2>Protocol Format</h2>
 * <pre>
 * ESC P [params] q [data] ESC \
 * </pre>
 *
 * @see <a href="https://en.wikipedia.org/wiki/Sixel">Sixel on Wikipedia</a>
 */
public final class SixelProtocol implements ImageProtocol {

    private static final String DCS = "\033P";  // Device Control String
    private static final String ST = "\033\\";   // String Terminator
    private static final int MAX_COLORS = 256;
    private static final int SIXEL_HEIGHT = 6;

    // Sixel character offset - character '?' (63) represents all-zero, '~' (126) represents all-ones
    private static final int SIXEL_OFFSET = 63;

    private final int maxColors;

    /**
     * Creates a Sixel protocol with default settings (256 colors).
     */
    public SixelProtocol() {
        this(MAX_COLORS);
    }

    /**
     * Creates a Sixel protocol with a custom color limit.
     *
     * @param maxColors maximum number of colors in the palette (1-256)
     */
    public SixelProtocol(int maxColors) {
        this.maxColors = Math.max(1, Math.min(MAX_COLORS, maxColors));
    }

    @Override
    public void render(ImageData image, Rect area, Buffer buffer, OutputStream rawOutput) throws IOException {
        if (rawOutput == null) {
            throw new RuntimeIOException("Sixel protocol requires raw output stream");
        }

        if (area.isEmpty()) {
            return;
        }

        // Move cursor to position
        String cursorMove = String.format("\033[%d;%dH", area.y() + 1, area.x() + 1);
        rawOutput.write(cursorMove.getBytes(StandardCharsets.US_ASCII));

        // Generate and write Sixel data
        // The image should already be scaled by Image.scaleImage() based on the scaling mode
        byte[] sixelData = encodeSixel(image);
        rawOutput.write(sixelData);
        rawOutput.flush();
    }

    @Override
    public boolean requiresRawOutput() {
        return true;
    }

    @Override
    public Resolution resolution() {
        // Sixel can render at pixel level, but we report typical cell pixel ratio
        return new Resolution(8, 16);
    }

    @Override
    public String name() {
        return "Sixel";
    }

    @Override
    public TerminalImageProtocol protocolType() {
        return TerminalImageProtocol.SIXEL;
    }

    /**
     * Encodes an image as Sixel data.
     */
    private byte[] encodeSixel(ImageData image) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // Build color palette using simple color quantization
        Map<Integer, Integer> colorToPaletteIndex = buildPalette(image);

        // Start Sixel sequence
        // Format: DCS P1 ; P2 ; P3 q
        // P1=0: pixel aspect ratio from device
        // P2=1: no background fill (transparent)
        // P3=0: horizontal grid size from device
        out.write((DCS + "0;1;0q").getBytes(StandardCharsets.US_ASCII));

        // Define color palette
        // Format: #Pc;Pu;Px;Py;Pz
        // Pc = color register number
        // Pu = color coordinate system (2 = RGB percentage)
        // Px, Py, Pz = color values (0-100 for RGB percentages)
        for (Map.Entry<Integer, Integer> entry : colorToPaletteIndex.entrySet()) {
            int argb = entry.getKey();
            int index = entry.getValue();
            int r = (ImageData.red(argb) * 100) / 255;
            int g = (ImageData.green(argb) * 100) / 255;
            int b = (ImageData.blue(argb) * 100) / 255;
            String colorDef = String.format("#%d;2;%d;%d;%d", index, r, g, b);
            out.write(colorDef.getBytes(StandardCharsets.US_ASCII));
        }

        // Encode image data in 6-row strips
        int width = image.width();
        int height = image.height();

        for (int stripY = 0; stripY < height; stripY += SIXEL_HEIGHT) {
            // For each color in the palette, output the sixels for that color in this strip
            for (Map.Entry<Integer, Integer> entry : colorToPaletteIndex.entrySet()) {
                int targetArgb = entry.getKey();
                int colorIndex = entry.getValue();

                StringBuilder stripData = new StringBuilder();
                stripData.append("#").append(colorIndex);

                boolean hasPixels = false;
                int repeatCount = 0;
                int lastSixel = -1;

                for (int x = 0; x < width; x++) {
                    // Build sixel value for this column
                    int sixelValue = 0;
                    for (int dy = 0; dy < SIXEL_HEIGHT; dy++) {
                        int y = stripY + dy;
                        if (y < height) {
                            int pixel = image.pixelAt(x, y);
                            if (ImageData.isVisible(pixel) && quantizeColor(pixel) == targetArgb) {
                                sixelValue |= (1 << dy);
                                hasPixels = true;
                            }
                        }
                    }

                    // Run-length encoding
                    if (sixelValue == lastSixel) {
                        repeatCount++;
                    } else {
                        if (lastSixel >= 0) {
                            appendSixel(stripData, lastSixel, repeatCount);
                        }
                        lastSixel = sixelValue;
                        repeatCount = 1;
                    }
                }

                // Flush last run
                if (lastSixel >= 0) {
                    appendSixel(stripData, lastSixel, repeatCount);
                }

                // Only output if this color has pixels in this strip
                if (hasPixels) {
                    out.write(stripData.toString().getBytes(StandardCharsets.US_ASCII));
                    out.write('$'); // Carriage return (back to start of line for next color)
                }
            }

            // Move to next strip
            if (stripY + SIXEL_HEIGHT < height) {
                out.write('-'); // Graphics new line
            }
        }

        // End Sixel sequence
        out.write(ST.getBytes(StandardCharsets.US_ASCII));

        return out.toByteArray();
    }

    /**
     * Appends a sixel character with optional repeat count.
     */
    private void appendSixel(StringBuilder sb, int sixelValue, int count) {
        char sixelChar = (char) (SIXEL_OFFSET + sixelValue);
        if (count > 3) {
            // Use repeat introducer for efficiency
            sb.append('!').append(count).append(sixelChar);
        } else {
            for (int i = 0; i < count; i++) {
                sb.append(sixelChar);
            }
        }
    }

    /**
     * Builds a color palette from the image using simple quantization.
     */
    private Map<Integer, Integer> buildPalette(ImageData image) {
        Map<Integer, Integer> colorCounts = new HashMap<>();

        // Count color occurrences (after quantization)
        for (int y = 0; y < image.height(); y++) {
            for (int x = 0; x < image.width(); x++) {
                int pixel = image.pixelAt(x, y);
                if (ImageData.isVisible(pixel)) {
                    int quantized = quantizeColor(pixel);
                    colorCounts.merge(quantized, 1, Integer::sum);
                }
            }
        }

        // Select most frequent colors up to maxColors
        Map<Integer, Integer> palette = new HashMap<>();
        int index = 0;

        // Sort by frequency and take top colors
        colorCounts.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(maxColors)
            .forEach(entry -> {
                // Use the map size as the index since we can't modify index directly
            });

        for (Map.Entry<Integer, Integer> entry : colorCounts.entrySet()) {
            if (index >= maxColors) {
                break;
            }
            palette.put(entry.getKey(), index++);
        }

        return palette;
    }

    /**
     * Quantizes a color to reduce the number of unique colors.
     * Uses 6-6-6 RGB cube (216 colors) plus grayscale levels.
     */
    private static int quantizeColor(int argb) {
        int r = ImageData.red(argb);
        int g = ImageData.green(argb);
        int b = ImageData.blue(argb);

        // Quantize to 6 levels per channel (216 colors)
        r = (r * 5 / 255) * 51;
        g = (g * 5 / 255) * 51;
        b = (b * 5 / 255) * 51;

        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
