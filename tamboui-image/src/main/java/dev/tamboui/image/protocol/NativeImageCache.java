/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.image.protocol;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;

import dev.tamboui.error.RuntimeIOException;
import dev.tamboui.image.ImageData;
import dev.tamboui.layout.Rect;
import dev.tamboui.widget.RawOutputContext;

/**
 * Cross-frame state shared by the native image protocols (Kitty, iTerm2, Sixel).
 * <p>
 * Native protocols emit their payload on every {@code render()} call, which happens
 * once per draw of the render loop. Without the caching this class provides, two costs
 * would recur every single frame:
 * <ul>
 *   <li><b>Re-encoding</b> the image (PNG/base64 for Kitty and iTerm2, Sixel encoding
 *       for Sixel) — wasteful CPU and short-lived allocations that churn the GC.</li>
 *   <li>For Kitty, <b>terminal-side memory growth</b>: a Kitty transmission without an
 *       explicit image id stores a brand-new image in the terminal on every frame and
 *       never frees the previous ones. Over a render loop this accumulates without bound
 *       (the terminal process, not the JVM, is what runs out of memory).</li>
 * </ul>
 *
 * <h2>Stable image ids (Kitty)</h2>
 * Ids are keyed by <em>display position</em> rather than by image content. Re-rendering
 * at the same spot — whether the same picture every frame or a new picture each time the
 * content changes (e.g. flipping PDF pages) — reuses the same id, so the terminal
 * <em>replaces</em> the stored image instead of piling up copies.
 *
 * <h2>Redraw suppression</h2>
 * Even with a stable id, re-sending the payload on every frame is itself a leak source for
 * the cell-based protocols: iTerm2 decodes and retains every inline image it receives, so a
 * render loop that re-transmits the same picture every frame grows the terminal process
 * without bound. {@link #staleAreasToClear(ImageData, Rect, long)} returns {@code null} when
 * the same image is already shown at the same position, so an unchanged image is transmitted
 * exactly once. The picture stays on screen because the diff-based renderer leaves the image
 * cells untouched between frames, and the frame still records the area as occupied so it is
 * not treated as stale and cleared.
 * <p>
 * A protocol instance renders a single image at a time, so only the <em>last</em> emitted
 * {@code (area, image)} pair is tracked. Anything that differs from it — new content, a moved
 * or resized footprint, or a return to a position that was vacated in between — is treated as
 * a change and retransmitted; the previous footprint, if different, is reported for clearing.
 * Tracking only the last pair (rather than a map of every position ever shown) avoids a stale
 * entry wrongly suppressing a redraw after {@code Terminal.cleanupRawOutput()} has already
 * wiped a vacated area from the screen.
 * <p>
 * The one case not auto-detected is an explicit {@code Terminal.clear()} that wipes the screen
 * without changing the image or its position; the screen {@code generation} (see
 * {@link RawOutputContext}) covers {@code clear()}/resize, after which everything is redrawn.
 *
 * <h2>Payload cache</h2>
 * Encoded payloads are keyed by the source {@link ImageData} and held weakly, so once an
 * image is no longer referenced anywhere (e.g. a PDF page that has scrolled out of view),
 * its cached payload becomes eligible for garbage collection. This keeps the JVM-side
 * footprint bounded as well.
 */
final class NativeImageCache {

    private final Object lock = new Object();

    /** Encoded payload (base64 string / Sixel bytes) per source image, weakly held. */
    private final Map<ImageData, Object> payloads = new WeakHashMap<>();

    /** Footprint of the image last emitted, or {@code null} if nothing is currently shown. */
    private Rect lastArea;

    /** Image last emitted at {@link #lastArea} (identity-compared), or {@code null}. */
    private ImageData lastImage;

    /** The screen generation observed at the last {@link #staleAreasToClear} call. */
    private long lastGeneration;

    /**
     * Returns a stable, positive terminal image id for the given display area, derived
     * deterministically from the area's geometry.
     * <p>
     * Because the id depends only on the position (not on per-instance counter state), the same
     * area always yields the same id — even across different protocol instances. A new instance
     * (e.g. created when an app switches protocols at runtime) therefore reuses the id of the
     * image already on screen and <em>replaces</em> it, rather than leaving the old image
     * orphaned on the terminal's graphics layer.
     *
     * @param area the display area in cells
     * @return a stable, positive image id
     */
    static int imageId(Rect area) {
        int id = area.hashCode() & 0x7FFFFFFF;
        return id == 0 ? 1 : id;
    }

    /**
     * Reads the screen generation from a raw output stream, if it exposes one.
     * <p>
     * Streams that do not implement {@link RawOutputContext} (e.g. plain test streams) report
     * a constant generation, so change detection still works frame to frame — it simply will
     * not auto-invalidate on a screen clear, which such streams cannot signal.
     *
     * @param rawOutput the raw output stream
     * @return the current screen generation, or {@code 0} if the stream exposes none
     */
    static long generationOf(OutputStream rawOutput) {
        return rawOutput instanceof RawOutputContext ? ((RawOutputContext) rawOutput).generation() : 0L;
    }

    /**
     * Decides whether the given image must be (re)transmitted and, if so, returns the previously
     * shown footprint that this emission must clear from the screen first.
     * <p>
     * Returns {@code null} when the same image is already shown at the same footprint (skip).
     * Otherwise records the new footprint as the current one and returns the list (empty, or the
     * single previous footprint when it moved) to wipe before drawing, so a moved or shrinking
     * image does not leave the previous one behind on screen.
     *
     * @param image      the image about to be rendered
     * @param area       the display footprint in cells
     * @param generation the current screen generation (see {@code RawOutputContext})
     * @return the footprints to clear before emitting, or {@code null} to skip emission
     */
    List<Rect> staleAreasToClear(ImageData image, Rect area, long generation) {
        synchronized (lock) {
            if (generation != lastGeneration) {
                // Screen was already wiped (Terminal.clear()/resize()): nothing for us to clear,
                // and nothing of ours is still on screen.
                lastGeneration = generation;
                lastArea = null;
                lastImage = null;
            }
            if (area.equals(lastArea) && image == lastImage) {
                return null;
            }
            List<Rect> stale = new ArrayList<>();
            if (lastArea != null && !lastArea.equals(area)) {
                // The previously shown image moved or resized; clear its old footprint so it does
                // not remain visible around or behind the new one.
                stale.add(lastArea);
            }
            lastArea = area;
            lastImage = image;
            return stale;
        }
    }

    /**
     * Overwrites the given cell areas with spaces, removing any cell-based image (iTerm2, Sixel)
     * previously drawn there. Kitty images live on a separate graphics layer and are removed by
     * deleting their image id instead.
     *
     * @param areas the areas to clear
     * @param out   the raw output stream
     * @throws IOException if writing fails
     */
    static void clearAreas(List<Rect> areas, OutputStream out) throws IOException {
        if (areas.isEmpty()) {
            return;
        }
        int maxWidth = 0;
        for (Rect area : areas) {
            maxWidth = Math.max(maxWidth, area.width());
        }
        byte[] spaces = new byte[maxWidth];
        Arrays.fill(spaces, (byte) ' ');
        for (Rect area : areas) {
            for (int y = area.y(); y < area.y() + area.height(); y++) {
                String move = String.format("\033[%d;%dH", y + 1, area.x() + 1);
                out.write(move.getBytes(StandardCharsets.US_ASCII));
                out.write(spaces, 0, area.width());
            }
        }
    }

    /**
     * Encodes an image as a base64-encoded PNG, wrapping the checked IO exception so it can be
     * used from a cache supplier. Shared by the Kitty and iTerm2 protocols.
     *
     * @param image the image to encode
     * @return the base64-encoded PNG
     */
    static String encodeBase64(ImageData image) {
        try {
            return Base64.getEncoder().encodeToString(image.toPng());
        } catch (IOException e) {
            throw new RuntimeIOException("Failed to encode image as PNG", e);
        }
    }

    /**
     * Returns the cached encoded payload for the given image, computing and caching
     * it on first use. The supplier is invoked at most once per distinct image while
     * the image remains reachable.
     *
     * @param image   the source image used as the cache key
     * @param compute supplies the encoded payload on a cache miss
     * @param <T>     the payload type (e.g. {@code String} or {@code byte[]})
     * @return the cached or freshly computed payload
     */
    @SuppressWarnings("unchecked")
    <T> T payload(ImageData image, Supplier<T> compute) {
        synchronized (lock) {
            Object cached = payloads.get(image);
            if (cached == null) {
                cached = compute.get();
                payloads.put(image, cached);
            }
            return (T) cached;
        }
    }
}
