///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS dev.tamboui:tamboui-tui:LATEST
//DEPS dev.tamboui:tamboui-ratty:LATEST
//DEPS dev.tamboui:tamboui-jline3-backend:LATEST

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demos.ratty;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import dev.tamboui.buffer.Cell;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Layout;
import dev.tamboui.layout.Rect;
import dev.tamboui.ratty.ObjectFormat;
import dev.tamboui.ratty.PlaceOptions;
import dev.tamboui.ratty.UpdateOptions;
import dev.tamboui.ratty.protocol.RattyProtocol;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Backend;
import dev.tamboui.terminal.Frame;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.tui.TuiRunner;
import dev.tamboui.tui.event.Event;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.MouseButton;
import dev.tamboui.tui.event.MouseEvent;
import dev.tamboui.tui.event.MouseEventKind;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.widgets.block.Title;
import dev.tamboui.widgets.paragraph.Paragraph;

/**
 * Ratty Graphics Protocol drawing demo - ported from
 * <a href="https://github.com/orhun/ratty/blob/main/widget/examples/draw.rs">ratty draw.rs</a>.
 * <p>
 * Draw 2D pixels on the left canvas with the mouse, and they are rendered as a
 * rotating 3D object in the right pane via the Ratty Graphics Protocol.
 * <p>
 * Controls:
 * <ul>
 *   <li>Left mouse on canvas: draw / draw line while dragging</li>
 *   <li>Right mouse on canvas: erase</li>
 *   <li>Left drag on preview: rotate the 3D object</li>
 *   <li>'a': toggle animation</li>
 *   <li>'c': clear</li>
 *   <li>'q' / Esc: quit</li>
 * </ul>
 */
public class RattyDrawDemo {

    private static final int OBJECT_ID = 700;
    private static final float ROTATION_SCALE = 4.0f;

    private boolean shouldQuit;
    private Rect canvasInner = Rect.ZERO;
    private Rect previewInner = Rect.ZERO;
    private int mouseX = -1;
    private int mouseY = -1;

    // Drawing state - pixels in canvas-local coordinates
    private final Set<Long> points = new LinkedHashSet<>();
    private int[] lastDraw;
    private int[] lastRotate;

    // 3D object state
    private boolean animate = true;
    private float rotX;
    private float rotY;
    private boolean objectRegistered;
    private boolean objectPlaced;

    private Backend backend;

    /**
     * Creates a new draw demo instance.
     */
    public RattyDrawDemo() {
    }

    /**
     * Demo entry point.
     *
     * @param args CLI arguments (unused)
     * @throws Exception on unexpected error
     */
    public static void main(String[] args) throws Exception {
        new RattyDrawDemo().run();
    }

    /**
     * Runs the demo.
     *
     * @throws Exception if an error occurs
     */
    public void run() throws Exception {
        TuiConfig config = TuiConfig.builder()
            .mouseCapture(true)
            .build();

        try (TuiRunner tui = TuiRunner.create(config)) {
            this.backend = tui.backend();
            tui.run(this::handleEvent, this::render);
            // Cleanup: delete the 3D object before exit
            if (objectPlaced || objectRegistered) {
                try {
                    backend.writeRaw(RattyProtocol.delete(OBJECT_ID).getBytes(StandardCharsets.UTF_8));
                    backend.flush();
                } catch (IOException ignored) {
                    // best effort
                }
            }
        }
    }

    private boolean handleEvent(Event event, TuiRunner runner) {
        if (event instanceof KeyEvent) {
            return handleKey((KeyEvent) event, runner);
        }
        if (event instanceof MouseEvent) {
            return handleMouse((MouseEvent) event);
        }
        return false;
    }

    private boolean handleKey(KeyEvent k, TuiRunner runner) {
        if (k.isQuit() || k.code() == KeyCode.ESCAPE) {
            shouldQuit = true;
            runner.quit();
            return false;
        }
        String s = k.string();
        if ("a".equals(s) || "A".equals(s)) {
            animate = !animate;
            tryUpdateObject();
            return true;
        }
        if ("c".equals(s) || "C".equals(s)) {
            clearDrawing();
            return true;
        }
        return false;
    }

    private boolean handleMouse(MouseEvent m) {
        mouseX = m.x();
        mouseY = m.y();

        // Drag in preview area rotates the 3D object
        if (containsPoint(previewInner, m.x(), m.y())) {
            return handlePreviewMouse(m);
        }

        // Otherwise route to canvas
        int[] local = localCanvasPosition(m.x(), m.y());
        if (local == null) {
            // Outside canvas - release any drag state
            if (m.kind() == MouseEventKind.RELEASE) {
                lastDraw = null;
                lastRotate = null;
            }
            return true; // still redraw to update cursor indicator
        }

        switch (m.kind()) {
            case PRESS:
                if (m.button() == MouseButton.LEFT) {
                    addPoint(local[0], local[1]);
                    lastDraw = local;
                    syncPreview();
                } else if (m.button() == MouseButton.RIGHT) {
                    removePoint(local[0], local[1]);
                    lastDraw = local;
                    syncPreview();
                }
                break;
            case DRAG:
                if (m.button() == MouseButton.LEFT) {
                    drawLine(local, true);
                    syncPreview();
                } else if (m.button() == MouseButton.RIGHT) {
                    drawLine(local, false);
                    syncPreview();
                }
                break;
            case RELEASE:
                lastDraw = null;
                lastRotate = null;
                break;
            default:
                // ignore move / scroll
                break;
        }
        return true;
    }

    private boolean handlePreviewMouse(MouseEvent m) {
        switch (m.kind()) {
            case PRESS:
                if (m.button() == MouseButton.LEFT) {
                    lastRotate = new int[]{m.x(), m.y()};
                }
                break;
            case DRAG:
                if (m.button() == MouseButton.LEFT) {
                    if (lastRotate == null) {
                        lastRotate = new int[]{m.x(), m.y()};
                        return true;
                    }
                    int dx = m.x() - lastRotate[0];
                    int dy = m.y() - lastRotate[1];
                    rotY += dx * ROTATION_SCALE;
                    rotX += dy * ROTATION_SCALE;
                    lastRotate = new int[]{m.x(), m.y()};
                    tryUpdateObject();
                }
                break;
            case RELEASE:
                lastRotate = null;
                break;
            default:
                break;
        }
        return true;
    }

    private void clearDrawing() {
        points.clear();
        lastDraw = null;
        lastRotate = null;
        if (objectRegistered) {
            try {
                backend.writeRaw(RattyProtocol.delete(OBJECT_ID).getBytes(StandardCharsets.UTF_8));
                backend.flush();
            } catch (IOException ignored) {
                // best effort
            }
        }
        objectRegistered = false;
        objectPlaced = false;
    }

    private void addPoint(int x, int y) {
        points.add(packPoint(x, y));
    }

    private void removePoint(int x, int y) {
        points.remove(packPoint(x, y));
    }

    private void drawLine(int[] end, boolean add) {
        if (lastDraw == null) {
            if (add) {
                addPoint(end[0], end[1]);
            } else {
                removePoint(end[0], end[1]);
            }
            lastDraw = end;
            return;
        }
        // Bresenham's line
        int x0 = lastDraw[0];
        int y0 = lastDraw[1];
        int x1 = end[0];
        int y1 = end[1];
        int dx = Math.abs(x1 - x0);
        int dy = -Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx + dy;
        while (true) {
            if (add) {
                addPoint(x0, y0);
            } else {
                removePoint(x0, y0);
            }
            if (x0 == x1 && y0 == y1) {
                break;
            }
            int e2 = 2 * err;
            if (e2 >= dy) {
                err += dy;
                x0 += sx;
            }
            if (e2 <= dx) {
                err += dx;
                y0 += sy;
            }
        }
        lastDraw = end;
    }

    private void syncPreview() {
        try {
            if (points.isEmpty()) {
                if (objectRegistered) {
                    backend.writeRaw(RattyProtocol.delete(OBJECT_ID).getBytes(StandardCharsets.UTF_8));
                    backend.flush();
                }
                objectRegistered = false;
                objectPlaced = false;
                return;
            }

            byte[] obj = writeObj(points).getBytes(StandardCharsets.UTF_8);

            // (Re)register the object via payload - small payloads fit in one command,
            // large drawings will be chunked automatically.
            if (obj.length <= RattyProtocol.CHUNK_SIZE) {
                String registerCmd = RattyProtocol.registerByPayload(
                    OBJECT_ID, ObjectFormat.OBJ, obj, "live_draw.obj");
                backend.writeRaw(registerCmd.getBytes(StandardCharsets.UTF_8));
            } else {
                String[] chunks = RattyProtocol.registerByPayloadChunked(
                    OBJECT_ID, ObjectFormat.OBJ, obj, "live_draw.obj");
                for (String chunk : chunks) {
                    backend.writeRaw(chunk.getBytes(StandardCharsets.UTF_8));
                }
            }
            objectRegistered = true;

            // Place the object in the preview area (will be re-placed on next render
            // if the preview area changed). Always send a place command so the
            // terminal anchors the freshly-registered object correctly.
            placeObject();
            backend.flush();
        } catch (IOException ignored) {
            // best effort
        }
    }

    private void placeObject() throws IOException {
        if (previewInner.width() <= 0 || previewInner.height() <= 0) {
            return;
        }
        PlaceOptions.Builder b = PlaceOptions.builder(
                previewInner.y() + previewInner.height() / 2,
                previewInner.x() + previewInner.width() / 2,
                previewInner.width(),
                previewInner.height())
            .animate(animate)
            .scale(0.6f)
            .depth(8.0f)
            .color("ff6060");
        if (rotX != 0f || rotY != 0f) {
            b.rotate(rotX, rotY, 0f);
        }
        backend.writeRaw(RattyProtocol.place(OBJECT_ID, b.build()).getBytes(StandardCharsets.UTF_8));
        objectPlaced = true;
    }

    private void tryUpdateObject() {
        if (!objectRegistered) {
            return;
        }
        try {
            UpdateOptions opts = UpdateOptions.builder()
                .animate(animate)
                .rotate(rotX, rotY, 0f)
                .build();
            backend.writeRaw(RattyProtocol.update(OBJECT_ID, opts).getBytes(StandardCharsets.UTF_8));
            backend.flush();
        } catch (IOException ignored) {
            // best effort
        }
    }

    private void render(Frame frame) {
        Rect area = frame.area();
        List<Rect> rows = Layout.vertical()
            .constraints(Constraint.length(3), Constraint.fill())
            .split(area);

        renderHeader(frame, rows.get(0));

        List<Rect> cols = Layout.horizontal()
            .constraints(Constraint.percentage(50), Constraint.percentage(50))
            .split(rows.get(1));

        renderCanvas(frame, cols.get(0));
        renderPreview(frame, cols.get(1));
    }

    private void renderHeader(Frame frame, Rect area) {
        Line help = Line.from(
            Span.raw(" left mouse").cyan(), Span.raw(": draw  "),
            Span.raw("right mouse").cyan(), Span.raw(": erase  "),
            Span.raw("preview drag").cyan(), Span.raw(": rotate  "),
            Span.raw("a").cyan(), Span.raw(": animate (" + (animate ? "1" : "0") + ")  "),
            Span.raw("c").cyan(), Span.raw(": clear  "),
            Span.raw("q").cyan(), Span.raw(": quit")
        );

        Paragraph header = Paragraph.builder()
            .text(Text.from(help))
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.YELLOW))
                .title(Title.from(Line.from(
                    Span.raw(" TamboUI ").bold().magenta(),
                    Span.raw("• ").dim(),
                    Span.raw("Ratty Drawing Demo ").bold().yellow()
                )))
                .build())
            .build();
        frame.renderWidget(header, area);
    }

    private void renderCanvas(Frame frame, Rect area) {
        Block block = Block.builder()
            .borders(Borders.ALL)
            .borderType(BorderType.ROUNDED)
            .borderStyle(Style.EMPTY.fg(Color.WHITE))
            .title(Title.from(" Canvas "))
            .build();
        Rect inner = block.inner(area);
        this.canvasInner = inner;

        // Render the block first, then paint into the inner area.
        frame.renderWidget(block, area);

        // Fill with light grid dots, then highlight drawn points.
        Style gridStyle = Style.EMPTY.fg(Color.DARK_GRAY);
        Style drawStyle = Style.EMPTY.fg(Color.LIGHT_RED);
        for (int y = 0; y < inner.height(); y++) {
            for (int x = 0; x < inner.width(); x++) {
                Cell cell = frame.buffer().get(inner.x() + x, inner.y() + y);
                if (points.contains(packPoint(x, y))) {
                    cell.symbol("█").style(drawStyle);
                } else {
                    cell.symbol("·").style(gridStyle);
                }
            }
        }

        // Placeholder text when canvas is empty.
        if (points.isEmpty() && inner.height() >= 1 && inner.width() >= 12) {
            String text = "Draw here!";
            int startX = inner.x() + Math.max(0, (inner.width() - text.length()) / 2);
            int row = inner.y() + inner.height() / 2;
            for (int i = 0; i < text.length() && (startX + i) < inner.x() + inner.width(); i++) {
                frame.buffer().get(startX + i, row)
                    .symbol(String.valueOf(text.charAt(i)))
                    .style(Style.EMPTY.fg(Color.GRAY));
            }
        }
    }

    private void renderPreview(Frame frame, Rect area) {
        Block block = Block.builder()
            .borders(Borders.ALL)
            .borderType(BorderType.ROUNDED)
            .borderStyle(Style.EMPTY.fg(Color.WHITE))
            .title(Title.from(" Preview "))
            .build();
        Rect inner = block.inner(area);

        boolean previewChanged = !inner.equals(previewInner);
        this.previewInner = inner;
        frame.renderWidget(block, area);

        // Clear the inner so the terminal-rendered 3D object isn't fighting
        // with leftover text cells.
        Style clearStyle = Style.EMPTY;
        for (int y = 0; y < inner.height(); y++) {
            for (int x = 0; x < inner.width(); x++) {
                frame.buffer().get(inner.x() + x, inner.y() + y).symbol(" ").style(clearStyle);
            }
        }

        if (points.isEmpty()) {
            // Hint text
            if (inner.height() >= 1 && inner.width() >= 24) {
                String text = "Draw on the left canvas";
                int startX = inner.x() + Math.max(0, (inner.width() - text.length()) / 2);
                int row = inner.y() + inner.height() / 2;
                for (int i = 0; i < text.length() && (startX + i) < inner.x() + inner.width(); i++) {
                    frame.buffer().get(startX + i, row)
                        .symbol(String.valueOf(text.charAt(i)))
                        .style(Style.EMPTY.fg(Color.GRAY));
                }
            }
            return;
        }

        // Re-place the object if the preview area changed (window resize, etc.)
        if (previewChanged && objectRegistered) {
            try {
                placeObject();
                backend.flush();
            } catch (IOException ignored) {
                // best effort
            }
        }
    }

    private int[] localCanvasPosition(int x, int y) {
        if (canvasInner.width() == 0 || canvasInner.height() == 0) {
            return null;
        }
        if (!containsPoint(canvasInner, x, y)) {
            return null;
        }
        return new int[]{x - canvasInner.x(), y - canvasInner.y()};
    }

    private static boolean containsPoint(Rect r, int x, int y) {
        return x >= r.x() && x < r.x() + r.width()
            && y >= r.y() && y < r.y() + r.height();
    }

    private static long packPoint(int x, int y) {
        return ((long) (x & 0xFFFF) << 16) | (y & 0xFFFF);
    }

    /**
     * Builds an OBJ file from the drawn pixel set.
     * <p>
     * Each pixel becomes a small unit quad (two triangles) in the XY plane,
     * producing a 3D mesh that can be rotated by the terminal.
     */
    private static String writeObj(Set<Long> packedPoints) {
        // Convert to (x, y) and find bounds so we can center the mesh.
        List<int[]> pts = new ArrayList<>(packedPoints.size());
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (Long p : packedPoints) {
            int x = (int) ((p >> 16) & 0xFFFF);
            int y = (int) (p & 0xFFFF);
            pts.add(new int[]{x, y});
            if (x < minX) minX = x;
            if (x > maxX) maxX = x;
            if (y < minY) minY = y;
            if (y > maxY) maxY = y;
        }
        float cx = (minX + maxX) / 2.0f;
        float cy = (minY + maxY) / 2.0f;

        StringBuilder out = new StringBuilder(pts.size() * 80);
        int vertex = 1;
        for (int[] pt : pts) {
            // Center mesh around origin; flip Y so on-screen down == 3D down.
            float x0 = pt[0] - cx;
            float y0 = -(pt[1] - cy);
            float x1 = x0 + 1.0f;
            float y1 = y0 - 1.0f;
            out.append("v ").append(x0).append(' ').append(y0).append(" 0.0\n");
            out.append("v ").append(x1).append(' ').append(y0).append(" 0.0\n");
            out.append("v ").append(x1).append(' ').append(y1).append(" 0.0\n");
            out.append("v ").append(x0).append(' ').append(y1).append(" 0.0\n");
            out.append("f ").append(vertex).append(' ')
                .append(vertex + 1).append(' ').append(vertex + 2).append('\n');
            out.append("f ").append(vertex).append(' ')
                .append(vertex + 2).append(' ').append(vertex + 3).append('\n');
            vertex += 4;
        }
        return out.toString();
    }
}
