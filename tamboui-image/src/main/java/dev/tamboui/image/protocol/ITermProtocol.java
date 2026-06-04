/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.image.protocol;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.error.RuntimeIOException;
import dev.tamboui.image.ImageData;
import dev.tamboui.image.capability.TerminalImageProtocol;
import dev.tamboui.layout.Rect;

/**
 * Renders images using the iTerm2 inline images protocol.
 * <p>
 * The iTerm2 protocol uses OSC (Operating System Command) escape sequences
 * with base64-encoded image data. It's simpler than Kitty but widely supported.
 * <p>
 * Supported by: iTerm2, WezTerm, Ghostty, Konsole, mintty.
 *
 * <h2>Protocol Format</h2>
 * <pre>
 * ESC ] 1337 ; File = [arguments] : base64-data BEL
 * </pre>
 *
 * @see <a href="https://iterm2.com/documentation-images.html">iTerm2 Inline Images</a>
 */
public final class ITermProtocol implements ImageProtocol {

    private static final String OSC = "\033]1337;File=";
    private static final String BEL = "\007";
    private static final String ST = "\033\\";  // Alternative terminator

    private final boolean useStTerminator;
    private final NativeImageCache cache = new NativeImageCache();

    /**
     * Creates an iTerm2 protocol instance using BEL terminator.
     */
    public ITermProtocol() {
        this(false);
    }

    /**
     * Creates an iTerm2 protocol instance.
     *
     * @param useStTerminator if true, use ESC \ instead of BEL as terminator
     */
    public ITermProtocol(boolean useStTerminator) {
        this.useStTerminator = useStTerminator;
    }

    @Override
    public void render(ImageData image, Rect area, Buffer buffer, OutputStream rawOutput) throws IOException {
        if (rawOutput == null) {
            throw new RuntimeIOException("iTerm2 protocol requires raw output stream");
        }

        if (area.isEmpty()) {
            return;
        }

        // Skip re-transmission when the same image is already shown at the same position.
        // This is essential for iTerm2: it decodes and retains every inline image it receives,
        // so re-sending the picture on every frame of the render loop would grow the terminal
        // process memory without bound. The image stays on screen because the diff-based
        // renderer leaves the image cells untouched between frames. A change in screen
        // generation (clear/resize) forces a redraw.
        List<Rect> stale = cache.staleAreasToClear(image, area, NativeImageCache.generationOf(rawOutput));
        if (stale == null) {
            return;
        }
        // Wipe any previously shown image whose footprint this one does not fully cover, so a
        // shrinking image (e.g. FILL -> FIT) does not leave the larger one behind.
        NativeImageCache.clearAreas(stale, rawOutput);

        // Move cursor to position
        String cursorMove = String.format("\033[%d;%dH", area.y() + 1, area.x() + 1);
        rawOutput.write(cursorMove.getBytes(StandardCharsets.US_ASCII));

        // Encode image as PNG then base64. The payload depends only on the pixels, so it is
        // cached per image to avoid re-encoding on every frame of the render loop.
        String base64Data = cache.payload(image, () -> encodeBase64(image));

        // Build the iTerm2 escape sequence
        StringBuilder cmd = new StringBuilder();
        cmd.append(OSC);

        // Arguments:
        // inline=1: display inline (not as download)
        // width=N: display width in cells
        // height=N: display height in cells
        // preserveAspectRatio=0: do not apply additional scaling (Image.scaleImage() handles it)
        cmd.append(String.format("inline=1;width=%d;height=%d;preserveAspectRatio=0:",
            area.width(), area.height()));

        // Append base64 data
        cmd.append(base64Data);

        // Terminator
        cmd.append(useStTerminator ? ST : BEL);

        rawOutput.write(cmd.toString().getBytes(StandardCharsets.US_ASCII));
        rawOutput.flush();
    }

    /**
     * Encodes the image as base64-encoded PNG, wrapping the checked IO exception so it
     * can be used from a cache supplier.
     */
    private static String encodeBase64(ImageData image) {
        try {
            return Base64.getEncoder().encodeToString(image.toPng());
        } catch (IOException e) {
            throw new RuntimeIOException("Failed to encode image as PNG for iTerm2 protocol", e);
        }
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
        // iTerm2 renders at pixel level, report typical cell pixel ratio
        return new Resolution(8, 16);
    }

    @Override
    public String name() {
        return "iTerm2";
    }

    @Override
    public TerminalImageProtocol protocolType() {
        return TerminalImageProtocol.ITERM2;
    }
}
