/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.image.protocol;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicInteger;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.error.RuntimeIOException;
import dev.tamboui.image.ImageData;
import dev.tamboui.image.capability.TerminalImageProtocol;
import dev.tamboui.layout.Rect;

/**
 * Renders images using the Kitty Graphics Protocol.
 * <p>
 * The Kitty protocol is a modern graphics protocol that supports PNG images
 * directly, compression, transparency, and pixel-level positioning.
 * <p>
 * Supported by: Kitty, WezTerm, Ghostty, Konsole (recent versions).
 *
 * <h2>Protocol Format</h2>
 * <pre>
 * ESC _ G [control-data] ; [payload] ESC \
 * </pre>
 *
 * <h2>Animation and memory</h2>
 * Frames are transmitted as raw RGB ({@code f=24}) rather than PNG, so there is
 * no per-frame compression cost. Each instance reuses one image id and one
 * placement id, so re-transmitting replaces the previous frame instead of
 * leaving it stored in the terminal; without this a stream of frames makes the
 * terminal accumulate one image per frame. Separate instances get separate ids,
 * so independent images do not clobber each other.
 *
 * @see <a href="https://sw.kovidgoyal.net/kitty/graphics-protocol/">Kitty Graphics Protocol</a>
 */
public final class KittyProtocol implements ImageProtocol {

    private static final String APC = "\033_G";  // Application Program Command + 'G' for graphics
    private static final String ST = "\033\\";    // String Terminator
    private static final int CHUNK_SIZE = 4096;   // Maximum chunk size for transmission
    private static final int PLACEMENT_ID = 1;
    private static final AtomicInteger NEXT_IMAGE_ID = new AtomicInteger(1);

    private final int imageId = NEXT_IMAGE_ID.getAndIncrement();

    /**
     * Creates a new Kitty protocol instance.
     */
    public KittyProtocol() {
    }

    @Override
    public void render(ImageData image, Rect area, Buffer buffer, OutputStream rawOutput) throws IOException {
        if (rawOutput == null) {
            throw new RuntimeIOException("Kitty protocol requires raw output stream");
        }

        if (area.isEmpty()) {
            return;
        }

        // Move cursor to position
        String cursorMove = String.format("\033[%d;%dH", area.y() + 1, area.x() + 1);
        rawOutput.write(cursorMove.getBytes(StandardCharsets.US_ASCII));

        // Transmit raw RGB (no PNG compression) and reuse a stable image/placement
        // id so each frame replaces the previous one. The image arrives at its own
        // size because this protocol handles its own scaling.
        String base64Data = Base64.getEncoder().encodeToString(toRgb(image));
        sendChunked(rawOutput, base64Data, image.width(), image.height(), area.width(), area.height());

        rawOutput.flush();
    }

    /**
     * Packs the image into tightly-packed 24-bit RGB, row-major, for {@code f=24}.
     */
    private static byte[] toRgb(ImageData image) {
        int width = image.width();
        int height = image.height();
        byte[] rgb = new byte[width * height * 3];
        int i = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = image.pixelAt(x, y);
                rgb[i++] = (byte) ImageData.red(pixel);
                rgb[i++] = (byte) ImageData.green(pixel);
                rgb[i++] = (byte) ImageData.blue(pixel);
            }
        }
        return rgb;
    }

    @Override
    public boolean requiresRawOutput() {
        return true;
    }

    @Override
    public boolean handlesOwnScaling() {
        return true;
    }

    @Override
    public Resolution resolution() {
        // Kitty renders at pixel level, report typical cell pixel ratio
        return new Resolution(8, 16);
    }

    @Override
    public String name() {
        return "Kitty";
    }

    @Override
    public TerminalImageProtocol protocolType() {
        return TerminalImageProtocol.KITTY;
    }

    /**
     * Sends image data using chunked transmission.
     * <p>
     * For large images, the data must be split into chunks of at most 4096 bytes.
     */
    private void sendChunked(OutputStream out, String base64Data, int pixelWidth, int pixelHeight,
            int cols, int rows) throws IOException {
        int offset = 0;
        int length = base64Data.length();
        boolean first = true;

        while (offset < length) {
            int chunkEnd = Math.min(offset + CHUNK_SIZE, length);
            String chunk = base64Data.substring(offset, chunkEnd);
            boolean more = chunkEnd < length;

            StringBuilder cmd = new StringBuilder();
            cmd.append(APC);

            if (first) {
                // First chunk carries all control parameters:
                // a=T: transmit and display
                // f=24: format = raw RGB (no compression)
                // t=d: transmission = direct (embedded in escape code)
                // s=w,v=h: pixel dimensions, required for raw formats
                // i,p: stable image and placement ids so each frame replaces the last
                // q=2: quiet mode — suppress terminal OK/error responses on stdin
                // c=cols,r=rows: display size in cells
                // m=0/1: more chunks follow
                cmd.append(String.format("a=T,f=24,t=d,s=%d,v=%d,i=%d,p=%d,q=2,c=%d,r=%d,m=%d;",
                    pixelWidth, pixelHeight, imageId, PLACEMENT_ID, cols, rows, more ? 1 : 0));
                first = false;
            } else {
                // Subsequent chunks only need the 'm' flag
                cmd.append(String.format("m=%d;", more ? 1 : 0));
            }

            cmd.append(chunk);
            cmd.append(ST);

            out.write(cmd.toString().getBytes(StandardCharsets.US_ASCII));
            offset = chunkEnd;
        }
    }
}
