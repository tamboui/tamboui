/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.image;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.error.RuntimeIOException;
import dev.tamboui.image.capability.TerminalImageCapabilities;
import dev.tamboui.image.protocol.ImageProtocol;
import dev.tamboui.layout.Rect;
import dev.tamboui.widget.RawOutputCapable;
import dev.tamboui.widget.Widget;
import dev.tamboui.widgets.block.Block;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;

/**
 * A widget for displaying images in the terminal.
 * <p>
 * The Image widget automatically detects terminal capabilities and uses the best
 * available rendering method: native protocols (Kitty, iTerm2, Sixel) when supported,
 * or character-based fallbacks (half-blocks, Braille) for universal compatibility.
 *
 * <pre>{@code
 * Image image = Image.builder()
 *     .data(ImageData.fromPath(Path.of("photo.png")))
 *     .scaling(ImageScaling.FIT)
 *     .block(Block.bordered().title(Title.from("Photo")))
 *     .build();
 *
 * frame.renderWidget(image, area);
 * }</pre>
 *
 * @see ImageData
 * @see ImageScaling
 * @see dev.tamboui.image.capability.TerminalImageCapabilities
 */
public final class Image implements Widget, RawOutputCapable {

    private final ImageData data;
    private final ImageScaling scaling;
    private final Block block;
    private final ImageProtocol protocol;

    private Image(Builder builder) {
        this.data = builder.data;
        this.scaling = builder.scaling;
        this.block = builder.block;
        this.protocol = builder.protocol != null
            ? builder.protocol
            : TerminalImageCapabilities.detect().bestProtocol();
    }

    /**
     * Creates a new image builder.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates an image widget from the given image data.
     *
     * @param data the image data
     * @return an image widget
     */
    public static Image of(ImageData data) {
        return builder().data(data).build();
    }

    /**
     * Creates an image widget from a file path.
     *
     * @param path the path to the image file
     * @return an image widget
     * @throws IOException if the file cannot be read
     */
    public static Image fromPath(Path path) throws IOException {
        return builder().data(ImageData.fromPath(path)).build();
    }

    /**
     * Creates an image widget from a classpath resource.
     *
     * @param resourcePath the resource path
     * @return an image widget
     * @throws IOException if the resource cannot be read
     */
    public static Image fromResource(String resourcePath) throws IOException {
        return builder().data(ImageData.fromResource(resourcePath)).build();
    }

    @Override
    public void render(Rect area, Buffer buffer) {
        render(area, buffer, null);
    }

    /**
     * Renders the image to the given area with optional raw output support.
     * <p>
     * For native protocols (Sixel, Kitty, iTerm2), the rawOutput stream must be provided.
     * If rawOutput is null and the protocol requires it, the image will not be rendered.
     *
     * @param area      the area to render into
     * @param buffer    the buffer for character-based rendering
     * @param rawOutput the output stream for native protocols (may be null)
     */
    @Override
    public void render(Rect area, Buffer buffer, OutputStream rawOutput) {
        if (area.isEmpty() || data == null) {
            return;
        }

        // Render block if present and get inner area
        Rect imageArea = area;
        if (block != null) {
            block.render(area, buffer);
            imageArea = block.inner(area);
        }

        if (imageArea.isEmpty()) {
            return;
        }

        // Check if protocol requires raw output
        if (protocol.requiresRawOutput() && rawOutput == null) {
            // Cannot render native protocol without raw output
            return;
        }

        // Scale image based on scaling mode and protocol resolution
        ImageProtocol.Resolution res = protocol.resolution();
        int gridWidth = imageArea.width() * res.widthMultiplier();
        int gridHeight = imageArea.height() * res.heightMultiplier();

        ImageData scaledData = scaleImage(data, gridWidth, gridHeight);

        // Render using the selected protocol
        try {
            protocol.render(scaledData, imageArea, buffer, rawOutput);
        } catch (IOException e) {
            throw new RuntimeIOException("Failed to render image using protocol " + protocol.name(), e);
        }
    }

    /**
     * Scales the image according to the scaling mode.
     */
    private ImageData scaleImage(ImageData source, int targetWidth, int targetHeight) {
        switch (scaling) {
            case FIT: {
                int[] dims = source.scaledDimensionsToFit(targetWidth, targetHeight);
                return source.resize(dims[0], dims[1]);
            }
            case FILL: {
                int[] dims = source.scaledDimensionsToFill(targetWidth, targetHeight);
                ImageData scaled = source.resize(dims[0], dims[1]);
                // Crop to target dimensions
                int offsetX = (dims[0] - targetWidth) / 2;
                int offsetY = (dims[1] - targetHeight) / 2;
                if (offsetX > 0 || offsetY > 0) {
                    return scaled.crop(
                        Math.max(0, offsetX),
                        Math.max(0, offsetY),
                        Math.min(scaled.width(), targetWidth),
                        Math.min(scaled.height(), targetHeight)
                    );
                }
                return scaled;
            }
            case STRETCH:
                return source.resize(targetWidth, targetHeight);
            case NONE:
            default:
                if (source.width() > targetWidth || source.height() > targetHeight) {
                    // Crop to fit
                    return source.crop(0, 0,
                        Math.min(source.width(), targetWidth),
                        Math.min(source.height(), targetHeight));
                }
                return source;
        }
    }

    /**
     * Returns the image data.
     *
     * @return the image data
     */
    public ImageData data() {
        return data;
    }

    /**
     * Returns the scaling mode.
     *
     * @return the scaling mode
     */
    public ImageScaling scaling() {
        return scaling;
    }

    /**
     * Returns the block wrapper, if any.
     *
     * @return the block, or null
     */
    public Block block() {
        return block;
    }

    /**
     * Returns the protocol used for rendering.
     *
     * @return the protocol
     */
    public ImageProtocol protocol() {
        return protocol;
    }

    /**
     * Builder for {@link Image}.
     */
    public static final class Builder {
        private ImageData data;
        private ImageScaling scaling = ImageScaling.FIT;
        private Block block;
        private ImageProtocol protocol;

        private Builder() {
        }

        /**
         * Sets the image data.
         *
         * @param data the image data
         * @return this builder
         */
        public Builder data(ImageData data) {
            this.data = data;
            return this;
        }

        /**
         * Sets the scaling mode.
         *
         * @param scaling the scaling mode
         * @return this builder
         */
        public Builder scaling(ImageScaling scaling) {
            this.scaling = scaling != null ? scaling : ImageScaling.FIT;
            return this;
        }

        /**
         * Wraps the image in a block (for borders, titles, etc.).
         *
         * @param block the block wrapper
         * @return this builder
         */
        public Builder block(Block block) {
            this.block = block;
            return this;
        }

        /**
         * Sets a specific protocol for rendering.
         * <p>
         * By default, the best available protocol is auto-detected.
         *
         * @param protocol the protocol to use
         * @return this builder
         */
        public Builder protocol(ImageProtocol protocol) {
            this.protocol = protocol;
            return this;
        }

        /**
         * Builds the image widget.
         *
         * @return the image widget
         */
        public Image build() {
            return new Image(this);
        }
    }
}
