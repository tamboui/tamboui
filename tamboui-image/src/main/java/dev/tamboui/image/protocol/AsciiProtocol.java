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
 * Renders images as ASCII art using a brightness gradient.
 * <p>
 * Each terminal cell maps to a single pixel (1x1 resolution). The pixel's
 * luminance selects a character from a ramp that runs from sparse glyphs for
 * dark pixels to dense glyphs for bright ones, giving the classic "ASCII art"
 * look. When colour is enabled each character also carries the pixel's colour as
 * its foreground.
 * <p>
 * This protocol scales the image itself with <strong>nearest-neighbour</strong>
 * sampling (one source pixel per cell), not the smoothing bilinear scaling the
 * {@link dev.tamboui.image.Image} widget applies to photographic protocols.
 * Averaging neighbouring pixels would blend bright palette colours with the
 * dark background and wash the picture toward grey; nearest-neighbour keeps each
 * cell the vivid colour of a real pixel, which is what makes pixel-art and game
 * framebuffers read well as ASCII.
 */
public final class AsciiProtocol implements ImageProtocol {

    /**
     * Brightness ramp from darkest to brightest, matching doom-ascii's gradient.
     */
    private static final char[] GRADIENT =
        (" .'`^\",:;Il!i><~+_-?][}{1)(|\\/tfjrxnuvczXYUJCLQ0OZmwqpdbkhao*#MW&8%B@$")
            .toCharArray();

    private static final Resolution RESOLUTION = new Resolution(1, 1);

    /**
     * Brightness normaliser, matching doom-ascii's {@code RGB_SUM_MAX}. It is
     * slightly above the real maximum (255*3 = 765) so the brightest pixels do
     * not all collapse onto the last glyph.
     */
    private static final int RGB_SUM_MAX = 776;

    /** Gamma ramp: {@code round(sqrt(i/255) * 255)}, matching doom-ascii. */
    private static final int[] GAMMA = new int[256];

    static {
        for (int i = 0; i < 256; i++) {
            GAMMA[i] = (int) Math.round(Math.sqrt(i / 255.0) * 255.0);
        }
    }

    private final boolean colored;
    private final boolean bold;
    private final boolean fixGamma;

    /**
     * Creates a coloured ASCII protocol.
     */
    public AsciiProtocol() {
        this(true, false, false);
    }

    /**
     * Creates an ASCII protocol.
     *
     * @param colored {@code true} to colour each character with its pixel colour,
     *                {@code false} for monochrome glyphs
     */
    public AsciiProtocol(boolean colored) {
        this(colored, false, false);
    }

    /**
     * Creates an ASCII protocol with all options.
     *
     * @param colored  {@code true} to colour each character with its pixel colour
     * @param bold     {@code true} to render glyphs bold (brighter on many terminals)
     * @param fixGamma {@code true} to apply a square-root gamma curve that offsets
     *                 the darkening caused by sparse glyphs
     */
    public AsciiProtocol(boolean colored, boolean bold, boolean fixGamma) {
        this.colored = colored;
        this.bold = bold;
        this.fixGamma = fixGamma;
    }

    @Override
    public void render(ImageData image, Rect area, Buffer buffer, OutputStream rawOutput) {
        if (area.isEmpty()) {
            return;
        }

        int cols = area.width();
        int rows = area.height();
        int imgW = image.width();
        int imgH = image.height();

        for (int cellY = 0; cellY < rows; cellY++) {
            int srcY = Math.min(imgH - 1, cellY * imgH / rows);
            for (int cellX = 0; cellX < cols; cellX++) {
                int srcX = Math.min(imgW - 1, cellX * imgW / cols);
                int pixel = image.pixelAt(srcX, srcY);
                if (!ImageData.isVisible(pixel)) {
                    continue;
                }

                int r = ImageData.red(pixel);
                int g = ImageData.green(pixel);
                int b = ImageData.blue(pixel);
                if (fixGamma) {
                    r = GAMMA[r];
                    g = GAMMA[g];
                    b = GAMMA[b];
                }
                int index = Math.min(GRADIENT.length - 1, (r + g + b) * GRADIENT.length / RGB_SUM_MAX);
                char symbol = GRADIENT[index];
                if (symbol == ' ') {
                    continue;
                }

                Style style = Style.EMPTY;
                if (colored) {
                    style = style.fg(Color.rgb(r, g, b));
                }
                if (bold) {
                    style = style.bold();
                }
                buffer.setString(area.x() + cellX, area.y() + cellY, String.valueOf(symbol), style);
            }
        }
    }

    @Override
    public boolean requiresRawOutput() {
        return false;
    }

    @Override
    public boolean handlesOwnScaling() {
        return true;
    }

    @Override
    public Resolution resolution() {
        return RESOLUTION;
    }

    @Override
    public String name() {
        return "ASCII";
    }

    @Override
    public TerminalImageProtocol protocolType() {
        return TerminalImageProtocol.ASCII;
    }
}
