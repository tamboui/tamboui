# Native image rendering: memory & correctness rationale

This note explains **why** the native image protocols (Kitty, iTerm2, Sixel) are built the way
they are. Each rule below exists to fix a concrete, observed failure. Please don't undo one
without understanding the failure it prevents — they are subtle and terminal-specific.

## The original symptom

Showing an image inside a render loop (e.g. an image/PDF viewer that redraws every frame) made the
**terminal process** grow without bound — observed climbing to ~150 GB until the terminal had to be
killed. The JVM heap was fine; the leak was entirely terminal-side. On large images the UI also
froze for many seconds, left overlapping/stacked copies on screen, and on macOS stole window focus.

The root cause was that a native protocol re-transmits its payload on **every** `render()` call,
and `render()` is called once per frame. Everything below follows from making per-frame
re-transmission safe — or avoiding it.

## The rules and why they exist

### 1. Skip re-transmission when nothing changed
`NativeImageCache` tracks the last emitted `(area, image)` pair. If the same image is already shown
at the same position, `staleAreasToClear()` returns `null` and the protocol emits **nothing**.

*Why:* iTerm2 decodes and **retains every inline image it receives**. Re-sending the same picture
60×/second grows the terminal's memory unboundedly. The image stays on screen because the
diff-based renderer leaves the (unchanged) image cells alone between frames, and the frame still
records the area as occupied so cleanup doesn't wipe it.

### 2. Track only the *last* pair, not a map of every position
A protocol instance renders one image at a time, so only the last `(area, image)` is remembered.

*Why:* an earlier version kept a map of every position ever shown. After the image moved to a
**non-overlapping** position (which `Terminal.cleanupRawOutput()` wipes from screen) and back, the
stale map entry made the return trip wrongly *skip* — the image never reappeared. Tracking only the
last pair means any difference (content, move, or return) is correctly re-emitted.

### 3. Stable, deterministic Kitty image ids derived from the area
`NativeImageCache.imageId(area)` is computed from the area geometry — **not** an instance counter.

*Why:* without an `i=` id, Kitty stores a brand-new image per transmission and never frees the old
ones (the core leak). Giving each position a stable id makes a re-transmission *replace* the stored
image. Crucially the id must be **instance-independent**: when an app switches protocols at runtime
it creates a fresh protocol instance, and a per-instance counter would hand the same position a
*different* id than the image already on screen — leaving the old image orphaned on the graphics
layer ("stuck on the old frame" until a resize cleared everything). A geometry-derived id means any
instance computes the same id for the same position and replaces correctly.

### 4. Clear the previous footprint on move/resize
When the footprint changes, `staleAreasToClear()` returns the old footprint. Cell-based protocols
(iTerm2, Sixel) overwrite it with spaces; Kitty deletes the old image id (`a=d,d=I`).

*Why:* otherwise a shrinking image (FILL → FIT) or a moved image leaves the previous, larger copy
visible around/behind the new one — the "stacked images" artifact.

Sixel is a special case: it receives the whole cell slot (not the exact display footprint like
Kitty/iTerm2), and the pre-scaled image only fills a sub-region of it (FIT letterboxes; FILL/STRETCH
differ), while the slot does not change when only the scaling changes. So Sixel additionally clears
its whole slot before drawing — otherwise a smaller image leaves the previous, larger one around it
even at the same position.

### 5. Cap the transmitted resolution for self-scaling protocols
For Kitty/iTerm2, `Image` does not send the full-resolution source; it caps it to the on-screen
display size × a supersampling factor (headroom for high-DPI cells), never upscaling.

*Why:* a multi-megapixel source displayed in a small cell box meant pushing megabytes to the
terminal on every change. Combined with resize-drag (many emissions) and a stuck transfer, this
froze the UI for 10–20 s and triggered iTerm2's "a file is being downloaded" prompt. The terminal
still downsamples crisply from a source ≥ the cell box.

### 6. `q=2` on every Kitty command; `d=A` on delete-all
All Kitty graphics commands — transmit **and** the delete commands — set `q=2`.
`Terminal.KITTY_DELETE_ALL` uses `d=A` (uppercase).

*Why `q=2`:* without quiet mode the terminal replies (e.g. `ESC_Gi=1;OK ESC\`). The app reads that
reply as keyboard input — stray digits land in the focused field and the UI appears to hang. This
bit us specifically on the footprint-delete command, which originally omitted `q=2`.
*Why `d=A`:* lowercase `d=a` only removes placements and leaves the image **data** in terminal
memory; uppercase `d=A` actually frees it.

### 7. Screen generation invalidates the cache on `clear()`/resize
The raw output stream exposes a `generation` counter (`RawOutputContext`) that `Terminal` bumps in
`clear()` and `resize()`. The cache discards its last-pair state when the generation changes.

*Why:* after the screen is wiped, nothing we transmitted is still on screen. Without this signal a
cached "already shown" state would suppress the redraw and the image would vanish after a
`Terminal.clear()`. This keeps `clear()` a fully supported public API for image-bearing UIs.

### 8. Default AWT to headless
`ImageData`'s static initializer sets `java.awt.headless=true` unless the application set it
explicitly.

*Why:* the library uses AWT (`BufferedImage`/`ImageIO`) purely for off-screen image processing. On
macOS, initialising AWT non-headless registers the JVM as a Dock app and steals focus from the
terminal. A terminal library should stay a terminal library. An app that deliberately set the
property (e.g. it also runs Swing) is respected.

### 9. Detect Ghostty
Capability detection recognises Ghostty (Kitty protocol) via `TERM=xterm-ghostty`, and also via
`TERM_PROGRAM=ghostty` / `GHOSTTY_RESOURCES_DIR`.

*Why:* `TERM` is only `xterm-ghostty` when Ghostty's bundled terminfo is installed; the env vars are
always present, so auto-detect picks Kitty for Ghostty even without the terminfo.

## Verifying

- `NativeImageProtocolSkipTest` pins the byte-level contract for all three protocols: skip on
  unchanged, re-emit on content/position/generation change, re-emit on overwrite-return and
  non-overlapping move-and-return, `q=2` on Kitty deletes, and a stable image id across instances.
- `image-stress-test` is a manual demo (run in iTerm2/Kitty/Ghostty, watch terminal RSS): it stays
  flat in SAME mode and bounded in CHANGING mode (a new random image every frame).

## Known limitations (out of scope here)

- The first PNG encode of a given image is synchronous and one-time (cached afterwards).
- Forcing a protocol a terminal does not support renders poorly. Ghostty does not appear to handle
  the iTerm2 inline-image protocol (it implements Kitty), and iTerm2 does not appear to fully
  implement the Kitty graphics protocol (forcing Kitty there can render an artifact and disrupt
  input). Auto-detection picks a supported protocol, so this only happens on a manual override.
- Switching *away* from Kitty to a non-Kitty protocol can leave the last Kitty image on the graphics
  layer until the next screen clear — a framework-level protocol-switch cleanup gap.
- Cell-based protocols (iTerm2, Sixel) can show a hole where cell text was. If one frame draws text
  inside the image's area (e.g. a "hidden" placeholder) and the next frame shows the image at the
  same spot, the cell diff that clears the old text is written *after* the image was emitted during
  the same draw, punching it out. Re-transmitting every frame used to mask this (the next frame
  redrew over the hole); with skip suppression an unchanged frame is not re-sent, so it persists.
  Kitty is unaffected — its graphics live on a separate layer that cell writes don't touch. A proper
  fix is framework-level: defer raw output until after the cell diff so graphics composite on top.
- Sixel cannot update smoothly when the image changes every frame. Unlike Kitty (replace by id) and
  iTerm2 (replace the inline image in place), Sixel has no in-place replacement, so each new image
  means clear-slot → re-encode → re-send → terminal re-decode. That flickers under rapid change.
  This is inherent to Sixel; ordinary use (occasional changes, e.g. flipping a page) is unaffected.
  Terminal-side Sixel handling also varies between emulators.
