///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 17
//DEPS dev.tamboui:tamboui-toolkit:LATEST
//DEPS dev.tamboui:tamboui-image:LATEST
//DEPS dev.tamboui:tamboui-jline3-backend:LATEST
//DEPS com.fasterxml.jackson.core:jackson-databind:2.17.2
/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import javax.imageio.ImageIO;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.tamboui.image.Image;
import dev.tamboui.image.ImageData;
import dev.tamboui.image.ImageScaling;
import dev.tamboui.image.protocol.HalfBlockProtocol;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.tui.TuiRunner;
import dev.tamboui.tui.event.Event;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.MouseEvent;
import dev.tamboui.tui.event.MouseEventKind;
import dev.tamboui.tui.event.TickEvent;
import dev.tamboui.widgets.canvas.Canvas;
import dev.tamboui.widgets.canvas.Marker;
import dev.tamboui.widgets.canvas.shapes.Line;

/**
 * Live OpenStreetMap slippy map in the terminal: Web Mercator, raster tiles rendered as a bitmap
 * (half blocks) or an OSM vector overlay (roads via Overpass), with asynchronous fetching that
 * never blocks rendering, and keyboard/mouse pan and zoom.
 * <p>
 * This is an optional, network-backed demo. It deliberately lives outside the base map widget,
 * which stays offline and dependency-free. Keys: arrows/hjkl pan, +/- zoom, m cycles detail
 * (bundled outline -> OSM vector -> raster tiles), b cycles the raster base layer
 * (OSM / OpenTopoMap / CARTO dark / satellite), p toggles fast half-blocks vs full pixels
 * (auto-detected Kitty/iTerm/Sixel), n toggles place names (Natural Earth cities), u toggles the
 * jbang.dev version-check heatmap overlay, d toggles country/province borders + lakes (Natural
 * Earth), q quits.
 * <p>
 * No-key raster sources are reliable; the OSM vector layer uses public Overpass mirrors, which are
 * often overloaded, so it is best-effort and falls back to "overpass unavailable".
 */
public final class WorldMapLiveDemo {

    /** No-key raster tile sources. ESRI uses z/y/x ordering and serves JPEG. */
    private static final TileSource[] SOURCES = {
        new TileSource("OSM", "https://tile.openstreetmap.org/%d/%d/%d.png", false),
        new TileSource("OpenTopoMap", "https://a.tile.opentopomap.org/%d/%d/%d.png", false),
        new TileSource("CARTO dark", "https://basemaps.cartocdn.com/dark_all/%d/%d/%d.png", false),
        new TileSource("Satellite",
            "https://services.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/%d/%d/%d", true)
    };
    private static final String PLACES_URL =
        "https://raw.githubusercontent.com/nvkelso/natural-earth-vector/master/geojson/"
        + "ne_50m_populated_places_simple.geojson";
    private static final String VERSIONCHECKS_URL =
        "https://www.jbang.dev/assets/data/jbang-versionchecks.csv";
    private static final String NE_BASE =
        "https://raw.githubusercontent.com/nvkelso/natural-earth-vector/master/geojson/";
    private static final String ADMIN0_URL = NE_BASE + "ne_50m_admin_0_boundary_lines_land.geojson";
    private static final String ADMIN1_URL = NE_BASE + "ne_50m_admin_1_states_provinces_lines.geojson";
    private static final String LAKES_URL = NE_BASE + "ne_50m_lakes.geojson";
    private static final String[] OVERPASS_URLS = {
        "https://overpass.osm.ch/api/interpreter",
        "https://overpass-api.de/api/interpreter",
        "https://maps.mail.ru/osm/tools/overpass/api/interpreter"
    };
    private static final String USER_AGENT = "tamboui-worldmap-live-demo/1.0 (https://tamboui.dev)";
    private static final int TILE = 256;
    private static final int MIN_ZOOM = 1;
    private static final int MAX_ZOOM = 18;
    private static final int VECTOR_MIN_ZOOM = 12;
    private static final long RETRY_COOLDOWN_MS = 5000;
    // Source pixels per cell: half-block is 1x2; full-pixel mode composes denser for crisp protocols.
    private static final int HALFBLOCK_W = 1;
    private static final int HALFBLOCK_H = 2;
    private static final int FULLPIXEL_W = 8;
    private static final int FULLPIXEL_H = 16;
    private static final int MODE_BUNDLED = 0;
    private static final int MODE_VECTOR = 1;
    private static final int MODE_RASTER = 2;
    private static final String[] MODE_NAMES = {"bundled", "vector", "raster"};
    // Hometowns that are too small for Natural Earth, always shown.
    private static final Place[] HOMETOWNS = {
        new Place(10.49, 55.28, "Pederstrup", Integer.MAX_VALUE),
        new Place(-1.55, 47.22, "Nantes", Integer.MAX_VALUE)
    };

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ExecutorService fetchers = Executors.newFixedThreadPool(4);
    private final Map<String, BufferedImage> tileCache = new ConcurrentHashMap<>();
    private final Map<String, Boolean> tileInFlight = new ConcurrentHashMap<>();
    private final Map<String, Long> tileRetryAt = new ConcurrentHashMap<>();
    private final AtomicReference<VectorResult> vector = new AtomicReference<>();
    private final ObjectMapper json = new ObjectMapper();

    private double centerLon = 10.49;    // Pederstrup, Fyn
    private double centerLat = 55.28;
    private int zoom = 6;
    private int mode = MODE_BUNDLED;
    private int baseSource;   // index into SOURCES for raster mode
    private boolean fullPixels;   // false = fast half-blocks, true = auto-detected pixel protocol
    private boolean showNames = true;
    private boolean detailOn = true;   // country/province borders + lakes (Natural Earth)
    private boolean heatOn;   // jbang.dev version-check heatmap overlay, off by default
    private volatile List<Place> fetchedPlaces = List.of();   // Natural Earth cities, loaded once
    private volatile List<double[]> heat = List.of();         // {lat, lng, count} version checks
    private volatile List<double[]> borders = List.of();     // admin-0 country boundary lines
    private volatile List<double[]> provinces = List.of();   // admin-1 state/province lines
    private volatile List<double[]> lakes = List.of();       // lake polygon rings
    private boolean vectorRequested;
    private double[][] coastline;        // bundled outline, loaded lazily
    private int lastPxW = 80;
    private int lastPxH = 120;
    private int lastCellW = HALFBLOCK_W;
    private int lastCellH = HALFBLOCK_H;
    private int tick;
    private int lastMouseX = -1;
    private int lastMouseY = -1;

    /**
     * Demo entry point. Pass {@code probe} for a headless pipeline check.
     *
     * @param args optional {@code probe}
     * @throws Exception on unexpected error
     */
    public static void main(String[] args) throws Exception {
        WorldMapLiveDemo demo = new WorldMapLiveDemo();
        if (args.length > 0 && args[0].equals("probe")) {
            demo.probe(args);
        } else {
            demo.run();
        }
        demo.fetchers.shutdownNow();
    }

    private WorldMapLiveDemo() {
    }

    // ---- Web Mercator -----------------------------------------------------

    private static double lonToWorldX(double lon, int z) {
        return (lon + 180) / 360 * TILE * (1 << z);
    }

    private static double latToWorldY(double lat, int z) {
        double rad = Math.toRadians(lat);
        return (1 - Math.log(Math.tan(rad) + 1 / Math.cos(rad)) / Math.PI) / 2 * TILE * (1 << z);
    }

    private static double worldXToLon(double wx, int z) {
        return wx / (TILE * (1 << z)) * 360 - 180;
    }

    private static double worldYToLat(double wy, int z) {
        double n = Math.PI - 2 * Math.PI * wy / (TILE * (1 << z));
        return Math.toDegrees(Math.atan(Math.sinh(n)));
    }

    // ---- Tiles ------------------------------------------------------------

    private BufferedImage tile(int z, int x, int y) {
        int span = 1 << z;
        if (y < 0 || y >= span) {
            return null;
        }
        int wx = Math.floorMod(x, span);
        TileSource source = SOURCES[baseSource];
        String key = baseSource + "/" + z + "/" + wx + "/" + y;   // cache per source
        BufferedImage cached = tileCache.get(key);
        if (cached != null) {
            return cached;
        }
        Long retryAt = tileRetryAt.get(key);
        if (retryAt != null && System.currentTimeMillis() < retryAt) {
            return null;   // back off after a failure instead of hammering the server every frame
        }
        if (tileInFlight.putIfAbsent(key, Boolean.TRUE) == null) {
            fetchers.submit(() -> {
                try {
                    byte[] png = get(source.url(z, wx, y));
                    BufferedImage bi = png == null ? null : ImageIO.read(new ByteArrayInputStream(png));
                    if (bi != null) {
                        tileCache.put(key, bi);
                    } else {
                        failTile(key);
                    }
                } catch (Exception ignored) {
                    failTile(key);
                }
            });
        }
        return null;
    }

    private void failTile(String key) {
        tileRetryAt.put(key, System.currentTimeMillis() + RETRY_COOLDOWN_MS);
        tileInFlight.remove(key);
    }

    private BufferedImage compose(int pxW, int pxH) {
        double cwx = lonToWorldX(centerLon, zoom);
        double cwy = latToWorldY(centerLat, zoom);
        double ox = cwx - pxW / 2.0;
        double oy = cwy - pxH / 2.0;
        BufferedImage img = new BufferedImage(pxW, pxH, BufferedImage.TYPE_INT_RGB);
        for (int py = 0; py < pxH; py++) {
            for (int px = 0; px < pxW; px++) {
                double wx = ox + px;
                double wy = oy + py;
                int tx = (int) Math.floor(wx / TILE);
                int ty = (int) Math.floor(wy / TILE);
                BufferedImage t = tile(zoom, tx, ty);
                if (t == null) {
                    img.setRGB(px, py, 0x202830);
                } else {
                    int ix = (int) Math.floorMod((long) Math.floor(wx), (long) TILE);
                    int iy = (int) Math.floorMod((long) Math.floor(wy), (long) TILE);
                    img.setRGB(px, py, t.getRGB(ix, iy));
                }
            }
        }
        return img;
    }

    // ---- Vector (Overpass) ------------------------------------------------

    private void requestVector(int pxW, int pxH) {
        if (zoom < VECTOR_MIN_ZOOM || vectorRequested) {
            return;
        }
        vectorRequested = true;
        double cwx = lonToWorldX(centerLon, zoom);
        double cwy = latToWorldY(centerLat, zoom);
        double west = worldXToLon(cwx - pxW / 2.0, zoom);
        double east = worldXToLon(cwx + pxW / 2.0, zoom);
        double north = worldYToLat(cwy - pxH / 2.0, zoom);
        double south = worldYToLat(cwy + pxH / 2.0, zoom);
        fetchers.submit(() -> {
            String q = "[out:json][timeout:25];"
                    + "way[highway~\"^(motorway|trunk|primary|secondary|tertiary|"
                    + "unclassified|residential|living_street)$\"]"
                    + "(" + south + "," + west + "," + north + "," + east + ");out geom;";
            // Overpass mirrors are often overloaded (502/504); try them in turn.
            for (String url : OVERPASS_URLS) {
                try {
                    HttpResponse<byte[]> resp = postStatus(url, "data=" + enc(q));
                    if (resp.statusCode() == 200) {
                        vector.set(parseVector(resp.body()));
                        return;
                    }
                } catch (Exception ignored) {
                    // try the next mirror
                }
            }
            vector.set(VectorResult.failure());
        });
    }

    private VectorResult parseVector(byte[] body) throws Exception {
        JsonNode root = json.readTree(body);
        List<double[]> ways = new ArrayList<>();
        for (JsonNode el : root.get("elements")) {
            JsonNode geom = el.get("geometry");
            if (geom == null) {
                continue;
            }
            double[] line = new double[geom.size() * 2];
            int i = 0;
            for (JsonNode p : geom) {
                line[i++] = p.get("lon").asDouble();
                line[i++] = p.get("lat").asDouble();
            }
            ways.add(line);
        }
        return new VectorResult(ways, false);
    }

    // ---- HTTP -------------------------------------------------------------

    private byte[] get(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", USER_AGENT).GET().build();
        HttpResponse<byte[]> resp = http.send(req, HttpResponse.BodyHandlers.ofByteArray());
        return resp.statusCode() == 200 ? resp.body() : null;
    }

    private HttpResponse<byte[]> postStatus(String url, String form) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", USER_AGENT)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(form)).build();
        return http.send(req, HttpResponse.BodyHandlers.ofByteArray());
    }

    private static String enc(String s) {
        return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);
    }

    // ---- Interactive ------------------------------------------------------

    private void run() throws Exception {
        // TuiRunner decodes raw input into KeyEvent/MouseEvent (arrows, wheel, drag); never parse ESC here.
        TuiConfig config = TuiConfig.builder()
                .mouseCapture(true)
                .tickRate(Duration.ofMillis(150))   // periodic redraw so async tiles appear
                .build();
        fetchers.submit(this::loadPlaces);   // Natural Earth city names
        fetchers.submit(this::loadHeat);     // jbang.dev usage heatmap
        fetchers.submit(() -> borders = fetchLines(ADMIN0_URL));    // country borders
        fetchers.submit(() -> provinces = fetchLines(ADMIN1_URL));  // state/province borders
        fetchers.submit(() -> lakes = fetchLines(LAKES_URL));       // lakes
        try (TuiRunner tui = TuiRunner.create(config)) {
            tui.run(this::onEvent, this::ui);
        }
    }

    private boolean onEvent(Event event, TuiRunner runner) {
        if (event instanceof KeyEvent) {
            return onKey((KeyEvent) event, runner);
        }
        if (event instanceof MouseEvent) {
            return onMouse((MouseEvent) event);
        }
        if (event instanceof TickEvent) {
            return true;   // redraw to show tiles/vector that arrived asynchronously
        }
        return false;
    }

    private boolean onKey(KeyEvent k, TuiRunner runner) {
        if (k.isQuit() || k.isChar('q')) {
            runner.quit();
            return false;
        }
        if (k.isLeft() || k.isCharIgnoreCase('h')) {
            pan(-0.2, 0);
        } else if (k.isRight() || k.isCharIgnoreCase('l')) {
            pan(0.2, 0);
        } else if (k.isUp() || k.isCharIgnoreCase('k')) {
            pan(0, -0.2);
        } else if (k.isDown() || k.isCharIgnoreCase('j')) {
            pan(0, 0.2);
        } else if (k.isChar('+') || k.isChar('=')) {
            zoom = Math.min(MAX_ZOOM, zoom + 1);
            viewportChanged();
        } else if (k.isChar('-') || k.isChar('_')) {
            zoom = Math.max(MIN_ZOOM, zoom - 1);
            viewportChanged();
        } else if (k.isCharIgnoreCase('v') || k.isCharIgnoreCase('m')) {
            mode = (mode + 1) % 3;   // bundled -> vector -> raster
            viewportChanged();
        } else if (k.isCharIgnoreCase('b')) {
            baseSource = (baseSource + 1) % SOURCES.length;   // OSM/topo/dark/satellite
        } else if (k.isCharIgnoreCase('p')) {
            fullPixels = !fullPixels;   // half-blocks <-> full pixel protocol (Kitty/iTerm/Sixel)
        } else if (k.isCharIgnoreCase('n')) {
            showNames = !showNames;
        } else if (k.isCharIgnoreCase('u')) {
            heatOn = !heatOn;
        } else if (k.isCharIgnoreCase('d')) {
            detailOn = !detailOn;
        } else {
            return false;
        }
        return true;
    }

    private boolean onMouse(MouseEvent m) {
        if (m.kind() == MouseEventKind.SCROLL_UP) {
            zoom = Math.min(MAX_ZOOM, zoom + 1);
            viewportChanged();
            return true;
        }
        if (m.kind() == MouseEventKind.SCROLL_DOWN) {
            zoom = Math.max(MIN_ZOOM, zoom - 1);
            viewportChanged();
            return true;
        }
        if (m.kind() == MouseEventKind.PRESS) {
            lastMouseX = m.x();
            lastMouseY = m.y();
            return false;
        }
        if (m.kind() == MouseEventKind.DRAG) {
            if (lastMouseX >= 0) {
                // Dragging moves the map with the cursor, so the view shifts the opposite way.
                // Convert cell deltas to source/world pixels using the current cell size.
                panByPixels(-(m.x() - lastMouseX) * (double) lastCellW, -(m.y() - lastMouseY) * (double) lastCellH);
            }
            lastMouseX = m.x();
            lastMouseY = m.y();
            return true;
        }
        if (m.kind() == MouseEventKind.RELEASE) {
            lastMouseX = -1;
            lastMouseY = -1;
        }
        return false;
    }

    private void pan(double fracX, double fracY) {
        panByPixels(fracX * lastPxW, fracY * lastPxH);
    }

    private void panByPixels(double dpxX, double dpxY) {
        double cwx = lonToWorldX(centerLon, zoom) + dpxX;
        double cwy = latToWorldY(centerLat, zoom) + dpxY;
        centerLon = worldXToLon(cwx, zoom);
        centerLat = Math.max(-85, Math.min(85, worldYToLat(cwy, zoom)));
        viewportChanged();
    }

    private void viewportChanged() {
        // Drop stale vector data (it was projected for the old view) and allow a refetch.
        vectorRequested = false;
        vector.set(null);
    }

    private void ui(Frame frame) {
        Rect area = frame.area();
        if (area.isEmpty()) {
            return;
        }
        Rect map = new Rect(area.x(), area.y(), area.width(), Math.max(1, area.height() - 1));
        int cellW = fullPixels ? FULLPIXEL_W : HALFBLOCK_W;
        int cellH = fullPixels ? FULLPIXEL_H : HALFBLOCK_H;
        int pxW = map.width() * cellW;
        int pxH = map.height() * cellH;
        lastPxW = pxW;
        lastPxH = pxH;
        lastCellW = cellW;
        lastCellH = cellH;
        tick++;

        String status;
        if (mode == MODE_RASTER) {
            BufferedImage composed = compose(pxW, pxH);
            // No forced protocol in full-pixel mode: Image auto-detects Kitty/iTerm/Sixel; else half-blocks.
            Image.Builder builder = Image.builder().data(ImageData.fromBufferedImage(composed))
                    .scaling(ImageScaling.STRETCH);
            if (!fullPixels) {
                builder.protocol(new HalfBlockProtocol());
            }
            frame.renderWidget(builder.build(), map);
            status = "";
        } else {
            status = renderVectorModes(frame, map, pxW, pxH, mode == MODE_VECTOR);
        }

        renderHeatmap(frame, map, pxW, pxH, cellW, cellH);
        renderMarkers(frame, map, pxW, pxH, cellW, cellH);

        String detail = mode == MODE_RASTER ? MODE_NAMES[mode] + ":" + SOURCES[baseSource].name : MODE_NAMES[mode];
        String hud = String.format(" z%d [%s] m mode b base p %s d borders[%s] n names u heat[%s]%s q quit ",
                zoom, detail, fullPixels ? "px" : "blk", detailOn ? "on" : "off", heatOn ? "on" : "off",
                status.isEmpty() ? "" : "  " + status);
        frame.buffer().setString(area.x(), area.y() + area.height() - 1, hud,
                Style.EMPTY.fg(Color.WHITE).bg(Color.rgb(20, 24, 32)));
    }

    /** Draws the bundled coastline outline, plus OSM roads when {@code withRoads}. Never blank. */
    private String renderVectorModes(Frame frame, Rect map, int pxW, int pxH, boolean withRoads) {
        if (coastline == null) {
            coastline = dev.tamboui.widgets.map.CoastlineData.bundled().lines();
        }
        double ox = lonToWorldX(centerLon, zoom) - pxW / 2.0;
        double oy = latToWorldY(centerLat, zoom) - pxH / 2.0;
        String status = "";
        List<double[]> roads = List.of();
        if (withRoads) {
            if (zoom < VECTOR_MIN_ZOOM) {
                status = "zoom to z" + VECTOR_MIN_ZOOM + "+ for roads";
            } else {
                requestVector(pxW, pxH);
                VectorResult v = vector.get();
                if (v == null) {
                    status = "fetching roads " + "|/-\\".charAt(tick % 4);
                } else if (v.failed()) {
                    status = "overpass unavailable";
                } else if (v.ways().isEmpty()) {
                    status = "no roads here";
                } else {
                    roads = v.ways();
                    status = v.ways().size() + " roads";
                }
            }
        }
        List<double[]> finalRoads = roads;
        Canvas canvas = Canvas.builder().xBounds(0, pxW).yBounds(0, pxH).marker(Marker.BRAILLE)
                .backgroundColor(Color.rgb(16, 20, 26))
                .paint(ctx -> {
                    if (detailOn) {
                        drawSegments(ctx, lakes, Color.rgb(50, 90, 150), ox, oy, pxH);
                    }
                    drawSegments(ctx, java.util.Arrays.asList(coastline), Color.rgb(90, 130, 160), ox, oy, pxH);
                    if (detailOn) {
                        drawSegments(ctx, provinces, Color.rgb(90, 90, 100), ox, oy, pxH);
                        drawSegments(ctx, borders, Color.rgb(180, 170, 150), ox, oy, pxH);
                    }
                    drawSegments(ctx, finalRoads, Color.rgb(120, 220, 255), ox, oy, pxH);
                }).build();
        frame.renderWidget(canvas, map);
        return status;
    }

    /** Draws named place markers as a buffer overlay on top of any mode, clipped to the map edges. */
    private void renderMarkers(Frame frame, Rect map, int pxW, int pxH, int cellW, int cellH) {
        if (!showNames) {
            return;
        }
        double ox = lonToWorldX(centerLon, zoom) - pxW / 2.0;
        double oy = latToWorldY(centerLat, zoom) - pxH / 2.0;
        int rightEdge = map.x() + map.width();
        int bottom = map.y() + map.height();
        int minPop = minPopForZoom();
        java.util.Set<Integer> used = new java.util.HashSet<>();
        // Hometowns first (always), then Natural Earth cities (sorted by population) filtered by zoom.
        List<Place> places = new java.util.ArrayList<>(java.util.Arrays.asList(HOMETOWNS));
        for (Place p : fetchedPlaces) {
            if (p.pop >= minPop) {
                places.add(p);
            }
        }
        for (Place place : places) {
            double sx = lonToWorldX(place.lon, zoom) - ox;
            double sy = latToWorldY(place.lat, zoom) - oy;
            if (sx < 0 || sy < 0 || sx >= pxW || sy >= pxH) {
                continue;
            }
            int col = map.x() + (int) (sx / cellW);
            int row = map.y() + (int) (sy / cellH);
            if (col < map.x() || col >= rightEdge || row < map.y() || row >= bottom || used.contains(row * 100000 + col)) {
                continue;
            }
            frame.buffer().setString(col, row, "\u25cf", Style.EMPTY.fg(Color.rgb(255, 210, 60)));
            used.add(row * 100000 + col);
            int labelX = col + 1;
            if (labelX < rightEdge) {
                String label = " " + place.name + " ";
                int avail = rightEdge - labelX;
                if (label.length() > avail) {
                    label = label.substring(0, avail);
                }
                frame.buffer().setString(labelX, row, label,
                        Style.EMPTY.fg(Color.WHITE).bg(Color.rgb(20, 24, 32)));
                for (int i = 0; i < label.length(); i++) {
                    used.add(row * 100000 + labelX + i);
                }
            }
        }
    }

    private int minPopForZoom() {
        return (int) (8_000_000 / Math.pow(2, Math.max(0, zoom - 2)));
    }

    /** Overlays the jbang.dev version-check heatmap (lat,lng,count) binned per cell. */
    private void renderHeatmap(Frame frame, Rect map, int pxW, int pxH, int cellW, int cellH) {
        List<double[]> points = heat;
        if (!heatOn || points.isEmpty()) {
            return;
        }
        double ox = lonToWorldX(centerLon, zoom) - pxW / 2.0;
        double oy = latToWorldY(centerLat, zoom) - pxH / 2.0;
        int rightEdge = map.x() + map.width();
        int bottom = map.y() + map.height();
        Map<Integer, Double> weight = new java.util.HashMap<>();
        for (double[] p : points) {
            double sx = lonToWorldX(p[1], zoom) - ox;
            double sy = latToWorldY(p[0], zoom) - oy;
            if (sx < 0 || sy < 0 || sx >= pxW || sy >= pxH) {
                continue;
            }
            int col = map.x() + (int) (sx / cellW);
            int row = map.y() + (int) (sy / cellH);
            if (col < map.x() || col >= rightEdge || row < map.y() || row >= bottom) {
                continue;
            }
            weight.merge(row * 100000 + col, p[2], Double::sum);
        }
        double max = weight.values().stream().mapToDouble(Double::doubleValue).max().orElse(1);
        double logMax = Math.log1p(max);
        String[] shades = {"\u2591", "\u2592", "\u2593", "\u2588"};   // light..full block
        for (Map.Entry<Integer, Double> e : weight.entrySet()) {
            double t = logMax == 0 ? 0 : Math.log1p(e.getValue()) / logMax;
            int col = e.getKey() % 100000;
            int row = e.getKey() / 100000;
            frame.buffer().setString(col, row, shades[Math.min(3, (int) (t * 4))], Style.EMPTY.fg(heatColor(t)));
        }
    }

    private static Color heatColor(double t) {
        t = Math.max(0, Math.min(1, t));
        int r = (int) (255 + (189 - 255) * t);
        int g = (int) (237 + (0 - 237) * t);
        int b = (int) (160 + (38 - 160) * t);
        return Color.rgb(r, g, b);
    }

    /** Fetches a Natural Earth GeoJSON and flattens every line/polygon ring into lon/lat polylines. */
    private List<double[]> fetchLines(String url) {
        try {
            byte[] body = get(url);
            if (body == null) {
                return List.of();
            }
            JsonNode root = json.readTree(body);
            List<double[]> out = new ArrayList<>();
            for (JsonNode f : root.get("features")) {
                JsonNode g = f.get("geometry");
                if (g == null) {
                    continue;
                }
                JsonNode c = g.get("coordinates");
                switch (g.get("type").asText()) {
                    case "LineString": addLine(out, c); break;
                    case "MultiLineString": for (JsonNode l : c) { addLine(out, l); } break;
                    case "Polygon": for (JsonNode r : c) { addLine(out, r); } break;
                    case "MultiPolygon": for (JsonNode p : c) { for (JsonNode r : p) { addLine(out, r); } } break;
                    default: break;
                }
            }
            return out;
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private static void addLine(List<double[]> out, JsonNode coords) {
        double[] line = new double[coords.size() * 2];
        int i = 0;
        for (JsonNode p : coords) {
            line[i++] = p.get(0).asDouble();
            line[i++] = p.get(1).asDouble();
        }
        out.add(line);
    }

    private void loadPlaces() {
        try {
            byte[] body = get(PLACES_URL);
            if (body == null) {
                return;
            }
            JsonNode root = json.readTree(body);
            List<Place> list = new ArrayList<>();
            for (JsonNode f : root.get("features")) {
                JsonNode c = f.get("geometry").get("coordinates");
                JsonNode props = f.get("properties");
                JsonNode name = props.get("name");
                if (name == null) {
                    continue;
                }
                int pop = props.hasNonNull("pop_max") ? props.get("pop_max").asInt() : 0;
                list.add(new Place(c.get(0).asDouble(), c.get(1).asDouble(), name.asText(), pop));
            }
            list.sort((a, b) -> Integer.compare(b.pop, a.pop));
            fetchedPlaces = list;
        } catch (Exception ignored) {
            // keep hometowns only
        }
    }

    private void loadHeat() {
        try {
            byte[] body = get(VERSIONCHECKS_URL);
            if (body == null) {
                return;
            }
            List<double[]> list = new ArrayList<>();
            String[] lines = new String(body, java.nio.charset.StandardCharsets.UTF_8).split("\n");
            for (int i = 1; i < lines.length; i++) {
                String[] c = lines[i].split(",");
                if (c.length < 3) {
                    continue;
                }
                try {
                    list.add(new double[] {Double.parseDouble(c[0].trim()),
                            Double.parseDouble(c[1].trim()), Double.parseDouble(c[2].trim())});
                } catch (NumberFormatException ignored) {
                    // skip bad row
                }
            }
            heat = list;
        } catch (Exception ignored) {
            // heatmap stays empty
        }
    }

    private void drawSegments(dev.tamboui.widgets.canvas.Context ctx, Iterable<double[]> lines,
            Color color, double ox, double oy, int pxH) {
        for (double[] line : lines) {
            for (int i = 2; i < line.length; i += 2) {
                double x1 = lonToWorldX(line[i - 2], zoom) - ox;
                double y1 = latToWorldY(line[i - 1], zoom) - oy;
                double x2 = lonToWorldX(line[i], zoom) - ox;
                double y2 = latToWorldY(line[i + 1], zoom) - oy;
                ctx.draw(new Line(x1, pxH - y1, x2, pxH - y2, color));   // Canvas y is bottom-up
            }
        }
    }

    // ---- Headless probe ---------------------------------------------------

    private void probe(String[] args) throws Exception {
        if (args.length > 1) {
            zoom = Integer.parseInt(args[1]);
        }
        System.out.println("probe: composing raster at z" + zoom + " around " + centerLat + "," + centerLon);
        BufferedImage img = compose(80, 120);
        // give the async fetchers a moment, then recompose so cached tiles land
        Thread.sleep(4000);
        img = compose(80, 120);
        int sampled = img.getRGB(40, 60);
        System.out.println("  composed " + img.getWidth() + "x" + img.getHeight()
                + " center px=0x" + Integer.toHexString(sampled) + " tilesCached=" + tileCache.size());
        javax.imageio.ImageIO.write(img, "png", new java.io.File("/tmp/worldmap-live-probe.png"));
        System.out.println("  wrote /tmp/worldmap-live-probe.png");

        zoom = 14;
        centerLon = 10.3870;
        centerLat = 55.3959; // Odense
        vectorRequested = false;
        requestVector(80, 120);
        for (int i = 0; i < 30 && vector.get() == null; i++) {
            Thread.sleep(1000);
        }
        VectorResult v = vector.get();
        System.out.println("  overpass " + (v == null ? "(pending)" : v.failed() ? "failed" : "ways=" + v.ways().size()));
        loadPlaces();
        loadHeat();
        System.out.println("  places=" + fetchedPlaces.size() + " heatPoints=" + heat.size());
        System.out.println("  borders=" + fetchLines(ADMIN0_URL).size() + " provinces="
                + fetchLines(ADMIN1_URL).size() + " lakes=" + fetchLines(LAKES_URL).size());
    }

    private static final class Place {
        private final double lon;
        private final double lat;
        private final String name;
        private final int pop;

        Place(double lon, double lat, String name, int pop) {
            this.lon = lon;
            this.lat = lat;
            this.name = name;
            this.pop = pop;
        }
    }

    private static final class TileSource {
        private final String name;
        private final String template;
        private final boolean yxOrder;

        TileSource(String name, String template, boolean yxOrder) {
            this.name = name;
            this.template = template;
            this.yxOrder = yxOrder;
        }

        String url(int z, int x, int y) {
            return yxOrder ? String.format(template, z, y, x) : String.format(template, z, x, y);
        }
    }

    private static final class VectorResult {
        private final List<double[]> ways;
        private final boolean failed;

        VectorResult(List<double[]> ways, boolean failed) {
            this.ways = ways;
            this.failed = failed;
        }

        static VectorResult failure() {
            return new VectorResult(List.of(), true);
        }

        List<double[]> ways() {
            return ways;
        }

        boolean failed() {
            return failed;
        }
    }
}
