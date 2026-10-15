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
 * @see <a href="https://sw.kovidgoyal.net/kitty/graphics-protocol/">Kitty Graphics Protocol</a>
 */
public final class KittyProtocol implements ImageProtocol {

    private static final String APC = "\033_G";  // Application Program Command + 'G' for graphics
    private static final String ST = "\033\\";    // String Terminator
    private static final int CHUNK_SIZE = 4096;   // Maximum chunk size for transmission
    private static final AtomicInteger ID_GENERATOR = new AtomicInteger(1);

    private int lastImageId = 0;

    /**
     * Creates a new Kitty protocol instance.
     */
    public KittyProtocol() {
    }

    private static int nextImageId() {
        int id = ID_GENERATOR.getAndIncrement();
        if (id <= 0) {
            ID_GENERATOR.set(1);
            id = ID_GENERATOR.getAndIncrement();
        }
        return id;
    }

    @Override
    public void render(ImageData image, Rect area, Buffer buffer, OutputStream rawOutput) throws IOException {
        if (rawOutput == null) {
            throw new RuntimeIOException("Kitty protocol requires raw output stream");
        }

        if (area.isEmpty()) {
            return;
        }

        // Delete previous image if one was drawn by this protocol instance
        if (lastImageId != 0) {
            String deleteCmd = String.format("\033_Ga=d,d=i,i=%d,q=2\033\\", lastImageId);
            rawOutput.write(deleteCmd.getBytes(StandardCharsets.US_ASCII));
        }

        int newImageId = nextImageId();
        lastImageId = newImageId;

        // Move cursor to position
        String cursorMove = String.format("\033[%d;%dH", area.y() + 1, area.x() + 1);
        rawOutput.write(cursorMove.getBytes(StandardCharsets.US_ASCII));

        // Mark covered cells in buffer
        ImageProtocol.markCoveredCells(buffer, area);

        // Encode image as PNG and then base64
        // The image should already be scaled by Image.scaleImage() based on the scaling mode
        byte[] pngData = image.toPng();
        String base64Data = Base64.getEncoder().encodeToString(pngData);

        // Send image using chunked transmission
        sendChunked(rawOutput, base64Data, newImageId, area.width(), area.height());

        rawOutput.flush();
    }

    /**
     * Deletes the last rendered image from the terminal.
     *
     * @param rawOutput the output stream for sending the delete sequence
     * @throws IOException if writing to rawOutput fails
     */
    public void deleteLast(OutputStream rawOutput) throws IOException {
        if (lastImageId != 0 && rawOutput != null) {
            String deleteCmd = String.format("\033_Ga=d,d=i,i=%d,q=2\033\\", lastImageId);
            rawOutput.write(deleteCmd.getBytes(StandardCharsets.US_ASCII));
            rawOutput.flush();
            lastImageId = 0;
        }
    }

    /**
     * Returns the ID of the last rendered image, or 0 if no image has been rendered.
     *
     * @return the last image ID
     */
    public int lastImageId() {
        return lastImageId;
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
    private void sendChunked(OutputStream out, String base64Data, int imageId, int cols, int rows) throws IOException {
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
                // First chunk includes all the control parameters
                // a=T: action = transmit and display
                // f=100: format = PNG
                // t=d: transmission = direct (embedded in escape code)
                // q=2: quiet mode — suppress terminal OK/error responses on stdin
                // i=imageId: unique image ID for explicit deletion
                // c=cols: display width in cells
                // r=rows: display height in cells
                // m=0/1: more chunks follow
                cmd.append(String.format("a=T,f=100,t=d,q=2,i=%d,c=%d,r=%d,m=%d;", imageId, cols, rows, more ? 1 : 0));
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

    /**
     * Sends an image using the simpler single-chunk method.
     * <p>
     * This is used for small images that fit in a single transmission.
     */
    @SuppressWarnings("unused")
    private void sendSimple(OutputStream out, String base64Data, int imageId, int cols, int rows) throws IOException {
        // a=T: action = transmit and display
        // f=100: format = PNG
        // t=d: transmission = direct
        // c=cols, r=rows: display size in cells
        String cmd = String.format("%sa=T,f=100,t=d,q=2,i=%d,c=%d,r=%d;%s%s",
            APC, imageId, cols, rows, base64Data, ST);
        out.write(cmd.getBytes(StandardCharsets.US_ASCII));
    }
}
