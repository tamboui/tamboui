///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 17
//DEPS com.fasterxml.jackson.core:jackson-databind:2.17.2

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 *
 * Regenerate the bundled coastline binary from the reviewed Natural Earth GeoJSON.
 *
 *   jbang config/generate-coastlines.java /path/to/ne_50m_coastline.geojson
 *
 * The source URL and SHA-256 are recorded alongside the binary in NOTICE.txt; this tool
 * refuses to run unless the input matches that checksum. The binary format is internal to
 * the map package (see CoastlineData): big-endian int32 line count; then per line an int32
 * point count followed by pairs of signed int16 longitude, latitude in hundredths of a degree.
 */

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.DataOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

class GenerateCoastlines {

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Usage: jbang config/generate-coastlines.java /path/to/ne_50m_coastline.geojson");
            System.exit(2);
        }
        Path root = Paths.get(GenerateCoastlines.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI()).getParent();
        // Fall back to the current directory when run via jbang (code source is a cache jar).
        if (!Files.isDirectory(root.resolve("tamboui-widgets"))) {
            root = Paths.get("").toAbsolutePath();
        }
        Path resource = root.resolve("tamboui-widgets/src/main/resources/dev/tamboui/widgets/map");

        byte[] source = Files.readAllBytes(Paths.get(args[0]));
        String notice = new String(Files.readAllBytes(resource.resolve("NOTICE.txt")));
        String expected = notice.split("Source SHA-256: ")[1].split("\\R", 2)[0].trim();
        String actual = sha256(source);
        if (!actual.equals(expected)) {
            System.err.println("Source checksum differs from the reviewed dataset");
            System.err.println("  expected: " + expected);
            System.err.println("  actual:   " + actual);
            System.exit(1);
        }

        List<JsonNode> lines = new ArrayList<>();
        JsonNode features = new ObjectMapper().readTree(source).get("features");
        for (JsonNode feature : features) {
            JsonNode geometry = feature.get("geometry");
            String type = geometry.get("type").asText();
            JsonNode coordinates = geometry.get("coordinates");
            if (type.equals("LineString")) {
                lines.add(coordinates);
            } else if (type.equals("MultiLineString")) {
                for (JsonNode line : coordinates) {
                    lines.add(line);
                }
            } else {
                System.err.println("Unsupported geometry type: " + type);
                System.exit(1);
            }
        }

        Path output = resource.resolve("coastlines.bin");
        try (DataOutputStream out = new DataOutputStream(Files.newOutputStream(output))) {
            out.writeInt(lines.size());
            int points = 0;
            for (JsonNode line : lines) {
                out.writeInt(line.size());
                for (JsonNode point : line) {
                    // rint rounds half-to-even, matching Python's round() so the artifact is reproducible.
                    out.writeShort((int) Math.rint(point.get(0).asDouble() * 100));
                    out.writeShort((int) Math.rint(point.get(1).asDouble() * 100));
                    points++;
                }
            }
            System.out.println("Wrote " + output + " (" + lines.size() + " lines, " + points + " points)");
        }
    }

    private static String sha256(byte[] data) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(data);
        StringBuilder hex = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            hex.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
        }
        return hex.toString();
    }
}
