/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.ratty.protocol;

import java.util.Base64;
import java.util.Objects;

import dev.tamboui.ratty.ObjectFormat;
import dev.tamboui.ratty.PlaceOptions;
import dev.tamboui.ratty.UpdateOptions;

/**
 * Encoder for Ratty Graphics Protocol (RGP) commands.
 * <p>
 * RGP uses APC (Application Program Command) escape sequences:
 * <pre>
 * ESC _ ratty;g;&lt;verb&gt;[;&lt;key=value&gt;...] ESC \
 * </pre>
 * <p>
 * This class provides stateless methods to generate RGP command strings.
 *
 * @see <a href="https://github.com/orhun/ratty/blob/main/protocols/graphics.md">RGP Specification</a>
 */
public final class RattyProtocol {

    private static final String APC = "\u001b_";  // ESC _
    private static final String ST = "\u001b\\";   // ESC \
    private static final String PROTOCOL_PREFIX = "ratty;g;";
    private static final int DEFAULT_CHUNK_SIZE = 4096;

    /**
     * Maximum payload chunk size for base64-encoded data.
     * <p>
     * The RGP spec recommends chunking large assets to avoid terminal buffer issues.
     */
    public static final int CHUNK_SIZE = DEFAULT_CHUNK_SIZE;

    private RattyProtocol() {
        // Utility class
    }

    /**
     * Generates a support query command.
     * <p>
     * Terminal responds with capabilities if RGP is supported.
     *
     * @return the support query command
     */
    public static String supportQuery() {
        return apc("s");
    }

    /**
     * Registers a 3D object by filesystem path.
     *
     * @param id     the object ID
     * @param format the object format
     * @param path   the filesystem path to the object file
     * @return the register command
     */
    public static String registerByPath(int id, ObjectFormat format, String path) {
        Objects.requireNonNull(format, "format");
        Objects.requireNonNull(path, "path");
        return apc("r", "id=" + id, "fmt=" + format.formatString(), "path=" + path);
    }

    /**
     * Registers a 3D object by embedding the asset data.
     * <p>
     * For assets larger than {@link #CHUNK_SIZE}, use {@link #registerByPayloadChunked}.
     *
     * @param id       the object ID
     * @param format   the object format
     * @param data     the raw object file bytes
     * @param filename optional filename for diagnostics (may be null)
     * @return the register command
     */
    public static String registerByPayload(int id, ObjectFormat format, byte[] data, String filename) {
        Objects.requireNonNull(format, "format");
        Objects.requireNonNull(data, "data");
        String base64 = Base64.getEncoder().encodeToString(data);
        StringBuilder cmd = new StringBuilder(APC)
            .append(PROTOCOL_PREFIX)
            .append("r;id=").append(id)
            .append(";fmt=").append(format.formatString())
            .append(";source=payload")
            .append(";more=0");
        if (filename != null) {
            cmd.append(";name=").append(filename);
        }
        cmd.append(';').append(base64).append(ST);
        return cmd.toString();
    }

    /**
     * Registers a 3D object by embedding the asset data in chunks.
     * <p>
     * Returns an array of commands to send sequentially. The terminal accumulates
     * chunks until the final {@code more=0} chunk is received.
     *
     * @param id       the object ID
     * @param format   the object format
     * @param data     the raw object file bytes
     * @param filename optional filename for diagnostics (may be null)
     * @return array of chunk commands to send
     */
    public static String[] registerByPayloadChunked(int id, ObjectFormat format, byte[] data, String filename) {
        Objects.requireNonNull(format, "format");
        Objects.requireNonNull(data, "data");

        String base64 = Base64.getEncoder().encodeToString(data);
        int totalLength = base64.length();
        int numChunks = (totalLength + CHUNK_SIZE - 1) / CHUNK_SIZE;
        String[] commands = new String[numChunks];

        for (int i = 0; i < numChunks; i++) {
            int start = i * CHUNK_SIZE;
            int end = Math.min(start + CHUNK_SIZE, totalLength);
            String chunk = base64.substring(start, end);
            boolean isLast = (i == numChunks - 1);

            StringBuilder cmd = new StringBuilder(APC)
                .append(PROTOCOL_PREFIX)
                .append("r;id=").append(id)
                .append(";fmt=").append(format.formatString())
                .append(";source=payload")
                .append(";more=").append(isLast ? "0" : "1");
            if (filename != null && i == 0) {
                // Only include filename in first chunk
                cmd.append(";name=").append(filename);
            }
            cmd.append(';').append(chunk).append(ST);
            commands[i] = cmd.toString();
        }

        return commands;
    }

    /**
     * Places a registered object into terminal cell space.
     *
     * @param id      the object ID
     * @param options the placement options
     * @return the place command
     */
    public static String place(int id, PlaceOptions options) {
        Objects.requireNonNull(options, "options");
        StringBuilder cmd = new StringBuilder(APC)
            .append(PROTOCOL_PREFIX)
            .append("p;id=").append(id)
            .append(";row=").append(options.row())
            .append(";col=").append(options.col())
            .append(";w=").append(options.width())
            .append(";h=").append(options.height());

        appendOptionalBool(cmd, "animate", options.animate());
        appendOptionalFloat(cmd, "scale", options.scale());
        appendOptionalFloat(cmd, "depth", options.depth());
        appendOptionalString(cmd, "color", options.color());
        appendOptionalFloat(cmd, "brightness", options.brightness());
        appendOptionalFloat(cmd, "px", options.px());
        appendOptionalFloat(cmd, "py", options.py());
        appendOptionalFloat(cmd, "pz", options.pz());
        appendOptionalFloat(cmd, "rx", options.rx());
        appendOptionalFloat(cmd, "ry", options.ry());
        appendOptionalFloat(cmd, "rz", options.rz());
        appendOptionalFloat(cmd, "sx", options.sx());
        appendOptionalFloat(cmd, "sy", options.sy());
        appendOptionalFloat(cmd, "sz", options.sz());

        cmd.append(ST);
        return cmd.toString();
    }

    /**
     * Updates a placed object's styling or transform.
     *
     * @param id      the object ID
     * @param options the update options
     * @return the update command
     */
    public static String update(int id, UpdateOptions options) {
        Objects.requireNonNull(options, "options");
        StringBuilder cmd = new StringBuilder(APC)
            .append(PROTOCOL_PREFIX)
            .append("u;id=").append(id);

        appendOptionalBool(cmd, "animate", options.animate());
        appendOptionalFloat(cmd, "scale", options.scale());
        appendOptionalFloat(cmd, "depth", options.depth());
        appendOptionalString(cmd, "color", options.color());
        appendOptionalFloat(cmd, "brightness", options.brightness());
        appendOptionalFloat(cmd, "px", options.px());
        appendOptionalFloat(cmd, "py", options.py());
        appendOptionalFloat(cmd, "pz", options.pz());
        appendOptionalFloat(cmd, "rx", options.rx());
        appendOptionalFloat(cmd, "ry", options.ry());
        appendOptionalFloat(cmd, "rz", options.rz());
        appendOptionalFloat(cmd, "sx", options.sx());
        appendOptionalFloat(cmd, "sy", options.sy());
        appendOptionalFloat(cmd, "sz", options.sz());

        cmd.append(ST);
        return cmd.toString();
    }

    /**
     * Deletes a specific object.
     *
     * @param id the object ID
     * @return the delete command
     */
    public static String delete(int id) {
        return apc("d", "id=" + id);
    }

    /**
     * Deletes all Ratty graphics objects.
     *
     * @return the delete-all command
     */
    public static String deleteAll() {
        return apc("d");
    }

    /**
     * Parses a support query response from the terminal.
     *
     * @param response the terminal response (APC string)
     * @return the parsed capabilities, or null if not a valid RGP response
     */
    public static RattyCapabilities parseSupportResponse(String response) {
        if (response == null || !response.startsWith(APC) || !response.endsWith(ST)) {
            return null;
        }

        String content = response.substring(APC.length(), response.length() - ST.length());
        if (!content.startsWith("ratty;g;s;")) {
            return null;
        }

        String[] parts = content.split(";");
        if (parts.length < 3 || !parts[0].equals("ratty") || !parts[1].equals("g") || !parts[2].equals("s")) {
            return null;
        }

        RattyCapabilities.Builder builder = RattyCapabilities.builder();
        for (int i = 3; i < parts.length; i++) {
            String[] kv = parts[i].split("=", 2);
            if (kv.length == 2) {
                String key = kv[0];
                String value = kv[1];
                switch (key) {
                    case "v":
                        try {
                            builder.version(Integer.parseInt(value));
                        } catch (NumberFormatException ignored) {
                        }
                        break;
                    case "fmt":
                        builder.formats(value.split("\\|"));
                        break;
                    case "path":
                        builder.pathSupported("1".equals(value));
                        break;
                    case "payload":
                        builder.payloadSupported("1".equals(value));
                        break;
                    case "chunk":
                        builder.chunkSupported("1".equals(value));
                        break;
                    case "anim":
                        builder.animSupported("1".equals(value));
                        break;
                    case "depth":
                        builder.depthSupported("1".equals(value));
                        break;
                    case "color":
                        builder.colorSupported("1".equals(value));
                        break;
                    case "brightness":
                        builder.brightnessSupported("1".equals(value));
                        break;
                    case "transform":
                        builder.transformSupported("1".equals(value));
                        break;
                    case "update":
                        builder.updateSupported("1".equals(value));
                        break;
                    default:
                        // Unknown capability, ignore
                        break;
                }
            }
        }

        return builder.build();
    }

    private static String apc(String verb, String... params) {
        StringBuilder cmd = new StringBuilder(APC).append(PROTOCOL_PREFIX).append(verb);
        for (String param : params) {
            cmd.append(';').append(param);
        }
        cmd.append(ST);
        return cmd.toString();
    }

    private static void appendOptionalBool(StringBuilder cmd, String key, Boolean value) {
        if (value != null) {
            cmd.append(';').append(key).append('=').append(value ? "1" : "0");
        }
    }

    private static void appendOptionalFloat(StringBuilder cmd, String key, Float value) {
        if (value != null) {
            cmd.append(';').append(key).append('=').append(value);
        }
    }

    private static void appendOptionalString(StringBuilder cmd, String key, String value) {
        if (value != null) {
            cmd.append(';').append(key).append('=').append(value);
        }
    }
}
