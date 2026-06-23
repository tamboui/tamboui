/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.ratty;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import dev.tamboui.ratty.protocol.RattyProtocol;

/**
 * Manages the lifecycle of 3D objects rendered via Ratty Graphics Protocol.
 * <p>
 * Handles registration, placement, updates, and cleanup of RGP objects.
 * Tracks registered and placed object IDs for proper cleanup on close.
 *
 * <pre>{@code
 * try (RattyGraphics graphics = new RattyGraphics(terminal.rawOutput())) {
 *     // Register a 3D model
 *     graphics.registerByPath(1, ObjectFormat.OBJ, "model.obj");
 *
 *     // Place it in the terminal
 *     graphics.place(1, PlaceOptions.builder(10, 20, 5, 3)
 *         .animate(true)
 *         .rotateY(45)
 *         .build());
 *
 *     // Update rotation
 *     graphics.update(1, UpdateOptions.builder().rotateY(90).build());
 * }
 * // All objects automatically deleted on close
 * }</pre>
 */
public final class RattyGraphics implements Closeable {

    private final OutputStream output;
    private final Set<Integer> registeredIds = new HashSet<>();
    private final Set<Integer> placedIds = new HashSet<>();
    private boolean closed;

    /**
     * Creates a new RattyGraphics instance.
     *
     * @param output the terminal output stream for sending RGP commands
     */
    public RattyGraphics(OutputStream output) {
        this.output = Objects.requireNonNull(output, "output");
    }

    /**
     * Registers a 3D object by filesystem path.
     *
     * @param id     the object ID
     * @param format the object format
     * @param path   the filesystem path to the object file
     * @throws IOException if writing to output fails
     */
    public void registerByPath(int id, ObjectFormat format, String path) throws IOException {
        checkNotClosed();
        String command = RattyProtocol.registerByPath(id, format, path);
        sendCommand(command);
        registeredIds.add(id);
    }

    /**
     * Registers a 3D object by embedding the asset data.
     * <p>
     * For large assets, this automatically chunks the payload.
     *
     * @param id       the object ID
     * @param format   the object format
     * @param data     the raw object file bytes
     * @param filename optional filename for diagnostics (may be null)
     * @throws IOException if writing to output fails
     */
    public void registerByPayload(int id, ObjectFormat format, byte[] data, String filename) throws IOException {
        checkNotClosed();
        Objects.requireNonNull(data, "data");

        int base64Length = ((data.length + 2) / 3) * 4; // Approximate base64 length
        if (base64Length <= RattyProtocol.CHUNK_SIZE) {
            // Small enough to send in one command
            String command = RattyProtocol.registerByPayload(id, format, data, filename);
            sendCommand(command);
        } else {
            // Need to chunk
            String[] commands = RattyProtocol.registerByPayloadChunked(id, format, data, filename);
            for (String command : commands) {
                sendCommand(command);
            }
        }
        registeredIds.add(id);
    }

    /**
     * Registers a 3D object by loading from a file path.
     *
     * @param id     the object ID
     * @param format the object format
     * @param path   the path to the object file
     * @throws IOException if reading the file or writing to output fails
     */
    public void registerFromFile(int id, ObjectFormat format, Path path) throws IOException {
        checkNotClosed();
        byte[] data = Files.readAllBytes(path);
        registerByPayload(id, format, data, path.getFileName().toString());
    }

    /**
     * Places a registered object into terminal cell space.
     *
     * @param id      the object ID
     * @param options the placement options
     * @throws IOException if writing to output fails
     */
    public void place(int id, PlaceOptions options) throws IOException {
        checkNotClosed();
        String command = RattyProtocol.place(id, options);
        sendCommand(command);
        placedIds.add(id);
    }

    /**
     * Updates a placed object's styling or transform.
     *
     * @param id      the object ID
     * @param options the update options
     * @throws IOException if writing to output fails
     */
    public void update(int id, UpdateOptions options) throws IOException {
        checkNotClosed();
        String command = RattyProtocol.update(id, options);
        sendCommand(command);
    }

    /**
     * Deletes a specific object.
     * <p>
     * This removes both the registration and placement.
     *
     * @param id the object ID
     * @throws IOException if writing to output fails
     */
    public void delete(int id) throws IOException {
        checkNotClosed();
        String command = RattyProtocol.delete(id);
        sendCommand(command);
        registeredIds.remove(id);
        placedIds.remove(id);
    }

    /**
     * Deletes all Ratty graphics objects.
     *
     * @throws IOException if writing to output fails
     */
    public void deleteAll() throws IOException {
        checkNotClosed();
        String command = RattyProtocol.deleteAll();
        sendCommand(command);
        registeredIds.clear();
        placedIds.clear();
    }

    /**
     * Returns the set of currently registered object IDs.
     *
     * @return the registered IDs
     */
    public Set<Integer> registeredIds() {
        return new HashSet<>(registeredIds);
    }

    /**
     * Returns the set of currently placed object IDs.
     *
     * @return the placed IDs
     */
    public Set<Integer> placedIds() {
        return new HashSet<>(placedIds);
    }

    /**
     * Closes this graphics manager and deletes all objects.
     */
    @Override
    public void close() {
        if (!closed) {
            closed = true;
            try {
                if (!registeredIds.isEmpty() || !placedIds.isEmpty()) {
                    deleteAll();
                }
            } catch (IOException e) {
                // Best effort cleanup
            }
        }
    }

    private void sendCommand(String command) throws IOException {
        output.write(command.getBytes(StandardCharsets.UTF_8));
        output.flush();
    }

    private void checkNotClosed() {
        if (closed) {
            throw new IllegalStateException("RattyGraphics is closed");
        }
    }
}
