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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;

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
 * <em>replaces</em> the stored image instead of piling up copies. This bounds terminal
 * memory to O(number of distinct image positions on screen) regardless of how many frames
 * are drawn or how many images are shown over time.
 *
 * <h2>Redraw suppression</h2>
 * Even with a stable id, re-sending the payload on every frame is itself a leak source for
 * the cell-based protocols: iTerm2 decodes and retains every inline image it receives, so a
 * render loop that re-transmits the same picture every frame grows the terminal process
 * without bound. {@link #needsEmit(ImageData, Rect)} returns {@code false} when the same
 * image is already shown at the same position, so an unchanged image is transmitted exactly
 * once. The picture stays on screen because the diff-based renderer leaves the image cells
 * untouched between frames, and the frame still records the area as occupied so it is not
 * treated as stale and cleared.
 * <p>
 * Re-emission is triggered whenever the image content or the display position changes (e.g.
 * a new PDF page, or a resize that re-lays-out the area). The one case not auto-detected is
 * an explicit {@code Terminal.clear()} that wipes the screen without changing the image or
 * its position; callers that clear the screen manually should expect to redraw their images.
 *
 * <h2>Payload cache</h2>
 * Encoded payloads are keyed by the source {@link ImageData} and held weakly, so once an
 * image is no longer referenced anywhere (e.g. a PDF page that has scrolled out of view),
 * its cached payload becomes eligible for garbage collection. This keeps the JVM-side
 * footprint bounded as well.
 */
final class NativeImageCache {

    private final Object lock = new Object();

    /**
     * Stable terminal image id per display position. Keyed by the full display
     * {@link Rect} so distinct positions get distinct ids while a stable position
     * keeps a stable id across frames.
     */
    private final Map<Rect, Integer> idsByPosition = new HashMap<>();

    /**
     * The image currently shown at each display position. Only the latest image per
     * position is retained (a put replaces the previous one), so this holds at most one
     * strong {@link ImageData} reference per on-screen image — superseded images (e.g. old
     * PDF pages) become eligible for garbage collection.
     */
    private final Map<Rect, ImageData> shownByPosition = new HashMap<>();

    /** Encoded payload (base64 string / Sixel bytes) per source image, weakly held. */
    private final Map<ImageData, Object> payloads = new WeakHashMap<>();

    private int nextId = 1;

    /** The screen generation observed at the last {@link #needsEmit} call. */
    private long lastGeneration;

    /**
     * Returns a stable, positive terminal image id for the given display area.
     * <p>
     * The same area always yields the same id, so repeated renders replace the
     * terminal-side image rather than accumulating new copies.
     *
     * @param area the display area in cells
     * @return a stable image id
     */
    int imageId(Rect area) {
        synchronized (lock) {
            Integer id = idsByPosition.get(area);
            if (id == null) {
                id = nextId++;
                idsByPosition.put(area, id);
            }
            return id;
        }
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
     * shown footprints that this emission must clear from the screen first.
     * <p>
     * Returns {@code null} when the same image is already shown at the same footprint (skip).
     * Otherwise records the new footprint and returns the list (possibly empty) of stale
     * footprints whose pixels overlap the new one. Those must be wiped before drawing so that a
     * shrinking footprint (e.g. FILL -> FIT) does not leave the larger previous image behind and
     * repeated emissions at different positions do not stack up on screen.
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
                shownByPosition.clear();
            }
            if (shownByPosition.get(area) == image) {
                return null;
            }
            // Collect previously shown footprints that overlap this one (excluding the identical
            // footprint, which this emission overwrites in place). They must be cleared so the old
            // image does not remain visible around or behind the new one.
            List<Rect> stale = new ArrayList<>();
            Iterator<Map.Entry<Rect, ImageData>> it = shownByPosition.entrySet().iterator();
            while (it.hasNext()) {
                Rect shownArea = it.next().getKey();
                if (!shownArea.equals(area) && !shownArea.intersection(area).isEmpty()) {
                    stale.add(shownArea);
                    it.remove();
                }
            }
            shownByPosition.put(area, image);
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
