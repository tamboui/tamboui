/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.map;

import java.util.Arrays;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;

/**
 * Raster land/sea shading: fills cell backgrounds with a water-to-land color ramp.
 * <p>
 * Unlike the vector {@link LandShape} (which paints braille dots in the foreground), this writes
 * cell <em>backgrounds</em>, so land and sea read as smooth solid areas. Land coverage is sampled
 * at 2x4 sub-points per cell (matching the braille sub-cell grid), box-blurred to soften the
 * coasts, and mapped continuously from the water color to the land color. It runs as a background
 * pass before the Canvas draws coastlines and markers on top; those foreground glyphs keep this
 * background because {@code setString} patches cell styles. Derived entirely from the bundled
 * coastline, with no extra data.
 */
final class LandTint {

    /** Horizontal and vertical sub-samples per cell, matching the braille 2x4 grid. */
    private static final int SS_X = 2;
    private static final int SS_Y = 4;

    private LandTint() {
    }

    /**
     * Shades the map area into the buffer's cell backgrounds.
     *
     * @param buffer   the target buffer
     * @param mapArea  the rectangle to shade
     * @param viewport the effective geographic bounds of the map area
     * @param water    the sea color
     * @param land     the land color
     * @param data     the coastline polylines
     */
    static void render(Buffer buffer, Rect mapArea, MapViewport viewport, Color water, Color land, CoastlineData data) {
        int cw = mapArea.width();
        int ch = mapArea.height();
        if (cw <= 0 || ch <= 0) {
            return;
        }
        double west = viewport.west();
        double lonSpan = viewport.east() - viewport.west();
        double north = viewport.north();
        double latSpan = viewport.north() - viewport.south();
        if (lonSpan == 0 || latSpan == 0) {
            return;
        }

        float[][] coverage = coverage(mapArea, viewport, west, lonSpan, north, latSpan, data);
        blur(coverage, cw, ch);

        Color.Rgb w = water.toRgb();
        Color.Rgb l = land.toRgb();
        for (int cy = 0; cy < ch; cy++) {
            for (int cx = 0; cx < cw; cx++) {
                Color bg = blend(w, l, coverage[cy][cx]);
                int x = mapArea.x() + cx;
                int y = mapArea.y() + cy;
                buffer.set(x, y, buffer.get(x, y).patchStyle(Style.EMPTY.bg(bg)));
            }
        }
    }

    /** Per-cell land fraction, from an even-odd scanline over every coastline ring. */
    private static float[][] coverage(Rect mapArea, MapViewport viewport,
            double west, double lonSpan, double north, double latSpan, CoastlineData data) {
        int cw = mapArea.width();
        int ch = mapArea.height();
        int subW = cw * SS_X;
        int subH = ch * SS_Y;
        double[][] coastlines = data.linesInternal();
        int totalVertices = 0;
        for (double[] line : coastlines) {
            totalVertices += line.length / 2;
        }
        double[] crossings = new double[Math.max(2, totalVertices)];
        int[][] landCount = new int[ch][cw];
        for (int sy = 0; sy < subH; sy++) {
            double lat = north - (sy + 0.5) / subH * latSpan;
            int count = 0;
            for (double[] line : coastlines) {
                int n = line.length / 2;
                for (int i = 0; i < n; i++) {
                    int j = (i + 1) % n;
                    double y0 = line[i * 2 + 1];
                    double y1 = line[j * 2 + 1];
                    if ((y0 <= lat && y1 > lat) || (y1 <= lat && y0 > lat)) {
                        double t = (lat - y0) / (y1 - y0);
                        crossings[count++] = line[i * 2] + t * (line[j * 2] - line[i * 2]);
                    }
                }
            }
            if (count < 2) {
                continue;
            }
            Arrays.sort(crossings, 0, count);
            int cy = sy / SS_Y;
            // Test each sub-column's normalized longitude so land wraps across the antimeridian
            // (the viewport may extend past 180 after panning); raw column spans would not wrap.
            for (int sx = 0; sx < subW; sx++) {
                double lon = west + (sx + 0.5) / subW * lonSpan;
                double lonN = lon - 360 * Math.floor((lon + 180) / 360);
                if ((countBelow(crossings, count, lonN) & 1) == 1) {
                    landCount[cy][sx / SS_X]++;
                }
            }
        }
        float perCell = SS_X * SS_Y;
        float[][] coverage = new float[ch][cw];
        for (int cy = 0; cy < ch; cy++) {
            for (int cx = 0; cx < cw; cx++) {
                coverage[cy][cx] = landCount[cy][cx] / perCell;
            }
        }
        return coverage;
    }

    /** Number of crossings strictly less than the given longitude, for an even-odd inside test. */
    private static int countBelow(double[] crossings, int count, double longitude) {
        int lo = 0;
        int hi = count;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (crossings[mid] < longitude) {
                lo = mid + 1;
            } else {
                hi = mid;
            }
        }
        return lo;
    }

    /** Separable box blur; wider horizontally since cells are about twice as tall as wide. */
    private static void blur(float[][] a, int w, int h) {
        box(a, w, h, 2, true);
        box(a, w, h, 1, false);
    }

    private static void box(float[][] a, int w, int h, int r, boolean horizontal) {
        if (horizontal) {
            for (int y = 0; y < h; y++) {
                float[] row = a[y].clone();
                for (int x = 0; x < w; x++) {
                    float sum = 0;
                    int n = 0;
                    for (int k = -r; k <= r; k++) {
                        int xx = x + k;
                        if (xx >= 0 && xx < w) {
                            sum += row[xx];
                            n++;
                        }
                    }
                    a[y][x] = sum / n;
                }
            }
        } else {
            for (int x = 0; x < w; x++) {
                float[] col = new float[h];
                for (int y = 0; y < h; y++) {
                    col[y] = a[y][x];
                }
                for (int y = 0; y < h; y++) {
                    float sum = 0;
                    int n = 0;
                    for (int k = -r; k <= r; k++) {
                        int yy = y + k;
                        if (yy >= 0 && yy < h) {
                            sum += col[yy];
                            n++;
                        }
                    }
                    a[y][x] = sum / n;
                }
            }
        }
    }

    private static Color blend(Color.Rgb water, Color.Rgb land, float t) {
        float c = Math.max(0, Math.min(1, t));
        int r = Math.round(water.r() + (land.r() - water.r()) * c);
        int g = Math.round(water.g() + (land.g() - water.g()) * c);
        int b = Math.round(water.b() + (land.b() - water.b()) * c);
        return Color.rgb(r, g, b);
    }
}
