/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.ratty.widget;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.error.RuntimeIOException;
import dev.tamboui.layout.Rect;
import dev.tamboui.ratty.PlaceOptions;
import dev.tamboui.ratty.UpdateOptions;
import dev.tamboui.ratty.protocol.RattyProtocol;
import dev.tamboui.widget.RawOutputCapable;
import dev.tamboui.widget.Widget;
import dev.tamboui.widgets.block.Block;

/**
 * A widget for displaying 3D objects using Ratty Graphics Protocol.
 * <p>
 * The Ratty widget manages a 3D object's placement in terminal cell space.
 * It handles the object lifecycle: registration (if needed), placement, and updates.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Register an object externally via RattyGraphics
 * rattyGraphics.registerByPath(1, ObjectFormat.OBJ, "model.obj");
 *
 * // Create a widget for the registered object
 * Ratty widget = Ratty.builder()
 *     .objectId(1)
 *     .animate(true)
 *     .rotateY(45)
 *     .scale(1.5f)
 *     .build();
 *
 * // Render it
 * frame.renderWidget(widget, area);
 * }</pre>
 *
 * <h2>Object Lifecycle</h2>
 * <p>
 * This widget assumes objects are pre-registered via {@link dev.tamboui.ratty.RattyGraphics}.
 * It handles placement and updates only. For automatic registration, use a higher-level
 * framework or toolkit element.
 *
 * @see dev.tamboui.ratty.RattyGraphics
 * @see PlaceOptions
 */
public final class Ratty implements Widget, RawOutputCapable {

    private final int objectId;
    private final Block block;
    private final boolean animate;
    private final Float scale;
    private final Float depth;
    private final String color;
    private final Float brightness;
    private final Float px;
    private final Float py;
    private final Float pz;
    private final Float rx;
    private final Float ry;
    private final Float rz;
    private final Float sx;
    private final Float sy;
    private final Float sz;

    // Dirty tracking
    private transient Rect lastArea;
    private transient int lastHashCode;

    private Ratty(Builder builder) {
        this.objectId = builder.objectId;
        this.block = builder.block;
        this.animate = builder.animate;
        this.scale = builder.scale;
        this.depth = builder.depth;
        this.color = builder.color;
        this.brightness = builder.brightness;
        this.px = builder.px;
        this.py = builder.py;
        this.pz = builder.pz;
        this.rx = builder.rx;
        this.ry = builder.ry;
        this.rz = builder.rz;
        this.sx = builder.sx;
        this.sy = builder.sy;
        this.sz = builder.sz;
        this.lastHashCode = computeHashCode();
    }

    /**
     * Creates a new builder.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void render(Rect area, Buffer buffer) {
        render(area, buffer, null);
    }

    @Override
    public void render(Rect area, Buffer buffer, OutputStream rawOutput) {
        if (area.isEmpty()) {
            return;
        }

        // Render block if present and get inner area
        Rect displayArea = area;
        if (block != null) {
            block.render(area, buffer);
            displayArea = block.inner(area);
        }

        if (displayArea.isEmpty()) {
            return;
        }

        // RGP requires raw output
        if (rawOutput == null) {
            return;
        }

        // Check if we need to send a place/update command
        boolean needsPlace = lastArea == null || !lastArea.equals(displayArea);
        boolean needsUpdate = !needsPlace && (computeHashCode() != lastHashCode);

        try {
            if (needsPlace) {
                // Send place command
                PlaceOptions.Builder optionsBuilder = PlaceOptions.builder(
                    displayArea.y() + displayArea.height() / 2,  // center row
                    displayArea.x() + displayArea.width() / 2,   // center col
                    displayArea.width(),
                    displayArea.height()
                );
                applyTransformOptions(optionsBuilder);
                String command = RattyProtocol.place(objectId, optionsBuilder.build());
                rawOutput.write(command.getBytes("UTF-8"));
                rawOutput.flush();
                lastArea = displayArea;
                lastHashCode = computeHashCode();
            } else if (needsUpdate) {
                // Send update command
                UpdateOptions.Builder optionsBuilder = UpdateOptions.builder();
                applyTransformOptions(optionsBuilder);
                String command = RattyProtocol.update(objectId, optionsBuilder.build());
                rawOutput.write(command.getBytes("UTF-8"));
                rawOutput.flush();
                lastHashCode = computeHashCode();
            }
        } catch (IOException e) {
            throw new RuntimeIOException("Failed to render Ratty object " + objectId, e);
        }
    }

    private void applyTransformOptions(PlaceOptions.Builder builder) {
        if (animate) {
            builder.animate(true);
        }
        if (scale != null) {
            builder.scale(scale);
        }
        if (depth != null) {
            builder.depth(depth);
        }
        if (color != null) {
            builder.color(color);
        }
        if (brightness != null) {
            builder.brightness(brightness);
        }
        if (px != null && py != null && pz != null) {
            builder.translate(px, py, pz);
        }
        if (rx != null || ry != null || rz != null) {
            builder.rotate(
                rx != null ? rx : 0f,
                ry != null ? ry : 0f,
                rz != null ? rz : 0f
            );
        }
        if (sx != null && sy != null && sz != null) {
            builder.scaleNonUniform(sx, sy, sz);
        }
    }

    private void applyTransformOptions(UpdateOptions.Builder builder) {
        if (animate) {
            builder.animate(true);
        }
        if (scale != null) {
            builder.scale(scale);
        }
        if (depth != null) {
            builder.depth(depth);
        }
        if (color != null) {
            builder.color(color);
        }
        if (brightness != null) {
            builder.brightness(brightness);
        }
        if (px != null && py != null && pz != null) {
            builder.translate(px, py, pz);
        }
        if (rx != null || ry != null || rz != null) {
            builder.rotate(
                rx != null ? rx : 0f,
                ry != null ? ry : 0f,
                rz != null ? rz : 0f
            );
        }
        if (sx != null && sy != null && sz != null) {
            builder.scaleNonUniform(sx, sy, sz);
        }
    }

    private int computeHashCode() {
        return Objects.hash(animate, scale, depth, color, brightness,
            px, py, pz, rx, ry, rz, sx, sy, sz);
    }

    /**
     * Returns the object ID.
     *
     * @return the object ID
     */
    public int objectId() {
        return objectId;
    }

    /**
     * Builder for {@link Ratty}.
     */
    public static final class Builder {
        private int objectId;
        private Block block;
        private boolean animate;
        private Float scale;
        private Float depth;
        private String color;
        private Float brightness;
        private Float px;
        private Float py;
        private Float pz;
        private Float rx;
        private Float ry;
        private Float rz;
        private Float sx;
        private Float sy;
        private Float sz;

        private Builder() {
        }

        /**
         * Sets the object ID (must be pre-registered).
         *
         * @param objectId the object ID
         * @return this builder
         */
        public Builder objectId(int objectId) {
            this.objectId = objectId;
            return this;
        }

        /**
         * Wraps the object in a block (for borders, titles, etc.).
         *
         * @param block the block wrapper
         * @return this builder
         */
        public Builder block(Block block) {
            this.block = block;
            return this;
        }

        /**
         * Enables default animation.
         *
         * @param animate true to enable animation
         * @return this builder
         */
        public Builder animate(boolean animate) {
            this.animate = animate;
            return this;
        }

        /**
         * Sets the uniform scale factor.
         *
         * @param scale the scale factor
         * @return this builder
         */
        public Builder scale(float scale) {
            this.scale = scale;
            return this;
        }

        /**
         * Sets the depth (z-offset).
         *
         * @param depth the depth
         * @return this builder
         */
        public Builder depth(float depth) {
            this.depth = depth;
            return this;
        }

        /**
         * Sets the color tint as RGB hex (e.g., "ff8844").
         *
         * @param color the color hex string (without '#')
         * @return this builder
         */
        public Builder color(String color) {
            this.color = color;
            return this;
        }

        /**
         * Sets the brightness multiplier.
         *
         * @param brightness the brightness (1.0 = normal)
         * @return this builder
         */
        public Builder brightness(float brightness) {
            this.brightness = brightness;
            return this;
        }

        /**
         * Sets the translation offset.
         *
         * @param px X offset
         * @param py Y offset
         * @param pz Z offset
         * @return this builder
         */
        public Builder translate(float px, float py, float pz) {
            this.px = px;
            this.py = py;
            this.pz = pz;
            return this;
        }

        /**
         * Sets the rotation in degrees.
         *
         * @param rx X rotation
         * @param ry Y rotation
         * @param rz Z rotation
         * @return this builder
         */
        public Builder rotate(float rx, float ry, float rz) {
            this.rx = rx;
            this.ry = ry;
            this.rz = rz;
            return this;
        }

        /**
         * Sets Y-axis rotation in degrees (most common for spinning objects).
         *
         * @param ry Y rotation
         * @return this builder
         */
        public Builder rotateY(float ry) {
            this.ry = ry;
            return this;
        }

        /**
         * Sets non-uniform scale factors.
         *
         * @param sx X scale
         * @param sy Y scale
         * @param sz Z scale
         * @return this builder
         */
        public Builder scaleNonUniform(float sx, float sy, float sz) {
            this.sx = sx;
            this.sy = sy;
            this.sz = sz;
            return this;
        }

        /**
         * Builds the widget.
         *
         * @return the widget
         */
        public Ratty build() {
            return new Ratty(this);
        }
    }
}
