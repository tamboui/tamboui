/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.map;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * A set of coastline polylines used by {@link CoastlineShape}, {@link LandShape} and the
 * {@link WorldMap} land tint.
 * <p>
 * The {@link #bundled()} data is Natural Earth 1:50m, loaded once from a classpath resource with
 * only JDK APIs (native-image packaging must include that resource). To show finer or custom
 * geography without bloating the jar, supply your own via {@link #of(double[][])} — for example a
 * higher-resolution Natural Earth export converted to coordinates. Each line is a flat array of
 * longitude/latitude pairs in degrees, longitude first.
 */
public final class CoastlineData {

    private final double[][] lines;

    private CoastlineData(double[][] lines) {
        this.lines = lines;
    }

    /**
     * Returns the bundled Natural Earth 1:50m coastline.
     *
     * @return the bundled coastline data
     */
    public static CoastlineData bundled() {
        return Bundled.INSTANCE;
    }

    /**
     * Creates coastline data from in-memory polylines, each a flat array of longitude/latitude
     * pairs in degrees. The arrays are copied.
     *
     * @param lines the polylines
     * @return the coastline data
     */
    public static CoastlineData of(double[][] lines) {
        Objects.requireNonNull(lines, "lines");
        double[][] copy = new double[lines.length][];
        for (int i = 0; i < lines.length; i++) {
            double[] line = lines[i];
            if (line == null || line.length < 2 || line.length % 2 != 0) {
                throw new IllegalArgumentException("Line " + i + " must be a non-empty array of lon/lat pairs");
            }
            for (int j = 0; j < line.length; j += 2) {
                double lon = line[j];
                double lat = line[j + 1];
                if (!Double.isFinite(lon) || !Double.isFinite(lat)
                        || lon < -180 || lon > 180 || lat < -90 || lat > 90) {
                    throw new IllegalArgumentException("Invalid point in line " + i);
                }
            }
            copy[i] = line.clone();
        }
        return new CoastlineData(copy);
    }

    // Internal binary format (not public API yet): big-endian int32 line count; then per line an
    // int32 point count followed by pairs of signed int16 longitude, latitude in hundredths of a
    // degree. Written by config/generate-coastlines.java; read here for the bundled resource.
    static CoastlineData load(InputStream input) throws IOException {
        Objects.requireNonNull(input, "input");
        return new CoastlineData(read(input));
    }

    private static double[][] read(InputStream resource) throws IOException {
        try (DataInputStream input = new DataInputStream(resource)) {
            double[][] lines = new double[input.readInt()][];
            for (int i = 0; i < lines.length; i++) {
                double[] line = new double[input.readInt() * 2];
                for (int j = 0; j < line.length; j++) {
                    line[j] = input.readShort() / 100.0;
                }
                lines[i] = line;
            }
            return lines;
        }
    }

    /**
     * Returns a copy of the polylines, each a flat array of longitude/latitude pairs in degrees.
     * Use this to project the geometry yourself (for example into another projection).
     *
     * @return a defensive copy of the polylines
     */
    public double[][] lines() {
        double[][] copy = new double[lines.length][];
        for (int i = 0; i < lines.length; i++) {
            copy[i] = lines[i].clone();
        }
        return copy;
    }

    double[][] linesInternal() {
        return lines;
    }

    private static final class Bundled {
        static final CoastlineData INSTANCE = loadBundled();

        private static CoastlineData loadBundled() {
            InputStream resource = CoastlineData.class.getResourceAsStream("coastlines.bin");
            if (resource == null) {
                throw new IllegalStateException("Bundled coastline resource is missing");
            }
            try {
                return new CoastlineData(read(resource));
            } catch (IOException e) {
                throw new IllegalStateException("Cannot read bundled coastlines", e);
            }
        }
    }
}
