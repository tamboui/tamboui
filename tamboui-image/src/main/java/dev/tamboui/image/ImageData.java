/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.image;

import javax.imageio.ImageIO;

import dev.tamboui.error.RuntimeIOException;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Immutable holder for image pixel data.
 * <p>
 * Images are stored in ARGB format where each pixel is a 32-bit integer:
 * bits 24-31 = alpha, bits 16-23 = red, bits 8-15 = green, bits 0-7 = blue.
 *
 * <pre>{@code
 * ImageData data = ImageData.fromPath(Path.of("photo.png"));
 * ImageData scaled = data.resize(80, 40);
 * }</pre>
 */
public final class ImageData {

    private final int width;
    private final int height;
    private final int[] pixels;
    private final Cache cache = new Cache();

    /**
     * Thread-safe cache for expensive operations.
     */
    private static final class Cache {
        private final ReentrantLock lock = new ReentrantLock();
        private byte[] png;
        private ImageData resized;
        private int resizedWidth;
        private int resizedHeight;

        byte[] getPng() {
            lock.lock();
            try {
                return png;
            } finally {
                lock.unlock();
            }
        }

        void setPng(byte[] data) {
            lock.lock();
            try {
                png = data;
            } finally {
                lock.unlock();
            }
        }

        ImageData getResized(int width, int height) {
            lock.lock();
            try {
                if (resized != null && resizedWidth == width && resizedHeight == height) {
                    return resized;
                }
                return null;
            } finally {
                lock.unlock();
            }
        }

        void setResized(ImageData data, int width, int height) {
            lock.lock();
            try {
                resized = data;
                resizedWidth = width;
                resizedHeight = height;
            } finally {
                lock.unlock();
            }
        }
    }

    private ImageData(int width, int height, int[] pixels) {
        this.width = width;
        this.height = height;
        this.pixels = pixels;
    }

    /**
     * Creates image data from a BufferedImage.
     *
     * @param image the source image
     * @return the image data
     */
    public static ImageData fromBufferedImage(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] pixels = new int[width * height];
        image.getRGB(0, 0, width, height, pixels, 0, width);
        return new ImageData(width, height, pixels);
    }

    /**
     * Loads image data from a file path.
     *
     * @param path the path to the image file
     * @return the image data
     * @throws IOException if the file cannot be read
     */
    public static ImageData fromPath(Path path) throws IOException {
        try (InputStream is = Files.newInputStream(path)) {
            BufferedImage image = ImageIO.read(is);
            if (image == null) {
                throw new RuntimeIOException("Unable to read image: " + path);
            }
            return fromBufferedImage(image);
        }
    }

    /**
     * Loads image data from a classpath resource.
     *
     * @param resourcePath the resource path (e.g., "/images/logo.png")
     * @return the image data
     * @throws IOException if the resource cannot be read
     */
    public static ImageData fromResource(String resourcePath) throws IOException {
        try (InputStream is = ImageData.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new RuntimeIOException("Resource not found: " + resourcePath);
            }
            BufferedImage image = ImageIO.read(is);
            if (image == null) {
                throw new RuntimeIOException("Unable to read image resource: " + resourcePath);
            }
            return fromBufferedImage(image);
        }
    }

    /**
     * Loads image data from a byte array.
     *
     * @param data the image data bytes
     * @return the image data
     * @throws IOException if the data cannot be decoded
     */
    public static ImageData fromBytes(byte[] data) throws IOException {
        try (InputStream is = new ByteArrayInputStream(data)) {
            BufferedImage image = ImageIO.read(is);
            if (image == null) {
                throw new RuntimeIOException("Unable to decode image data");
            }
            return fromBufferedImage(image);
        }
    }

    /**
     * Returns the image width in pixels.
     *
     * @return the width
     */
    public int width() {
        return width;
    }

    /**
     * Returns the image height in pixels.
     *
     * @return the height
     */
    public int height() {
        return height;
    }

    /**
     * Returns the pixel at the specified coordinates in ARGB format.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @return the ARGB pixel value
     * @throws IndexOutOfBoundsException if coordinates are out of bounds
     */
    public int pixelAt(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            throw new IndexOutOfBoundsException(
                String.format("Pixel (%d, %d) out of bounds for image %dx%d", x, y, width, height));
        }
        return pixels[y * width + x];
    }

    /**
     * Returns the alpha component of a pixel (0-255).
     *
     * @param argb the ARGB pixel value
     * @return the alpha component
     */
    public static int alpha(int argb) {
        return (argb >> 24) & 0xFF;
    }

    /**
     * Returns the red component of a pixel (0-255).
     *
     * @param argb the ARGB pixel value
     * @return the red component
     */
    public static int red(int argb) {
        return (argb >> 16) & 0xFF;
    }

    /**
     * Returns the green component of a pixel (0-255).
     *
     * @param argb the ARGB pixel value
     * @return the green component
     */
    public static int green(int argb) {
        return (argb >> 8) & 0xFF;
    }

    /**
     * Returns the blue component of a pixel (0-255).
     *
     * @param argb the ARGB pixel value
     * @return the blue component
     */
    public static int blue(int argb) {
        return argb & 0xFF;
    }

    /**
     * Returns true if the pixel is visible (alpha > 0).
     *
     * @param argb the ARGB pixel value
     * @return true if visible
     */
    public static boolean isVisible(int argb) {
        return alpha(argb) > 0;
    }

    /**
     * Creates a resized copy of this image.
     *
     * @param newWidth  the new width
     * @param newHeight the new height
     * @return the resized image data
     */
    public ImageData resize(int newWidth, int newHeight) {
        if (newWidth <= 0 || newHeight <= 0) {
            throw new IllegalArgumentException(
                String.format("Invalid dimensions: %dx%d", newWidth, newHeight));
        }
        if (newWidth == width && newHeight == height) {
            return this;
        }

        // Check cache
        ImageData cached = cache.getResized(newWidth, newHeight);
        if (cached != null) {
            return cached;
        }

        BufferedImage source = toBufferedImage();
        BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = resized.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(source, 0, 0, newWidth, newHeight, null);
        } finally {
            g.dispose();
        }
        ImageData result = fromBufferedImage(resized);
        cache.setResized(result, newWidth, newHeight);
        return result;
    }

    /**
     * Creates a cropped copy of this image.
     *
     * @param x      the x offset
     * @param y      the y offset
     * @param width  the crop width
     * @param height the crop height
     * @return the cropped image data
     */
    public ImageData crop(int x, int y, int width, int height) {
        if (x < 0 || y < 0 || width <= 0 || height <= 0) {
            throw new IllegalArgumentException(
                String.format("Invalid crop parameters: x=%d, y=%d, w=%d, h=%d", x, y, width, height));
        }
        if (x + width > this.width || y + height > this.height) {
            throw new IllegalArgumentException(
                String.format("Crop region exceeds image bounds: (%d,%d)+(%d,%d) > %dx%d",
                    x, y, width, height, this.width, this.height));
        }

        int[] cropped = new int[width * height];
        for (int row = 0; row < height; row++) {
            System.arraycopy(pixels, (y + row) * this.width + x, cropped, row * width, width);
        }
        return new ImageData(width, height, cropped);
    }

    /**
     * Converts this image data to a BufferedImage.
     *
     * @return the buffered image
     */
    public BufferedImage toBufferedImage() {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, width, height, pixels, 0, width);
        return image;
    }

    /**
     * Encodes this image as PNG bytes.
     * <p>
     * The result is cached for subsequent calls.
     *
     * @return the PNG-encoded bytes
     * @throws IOException if encoding fails
     */
    public byte[] toPng() throws IOException {
        byte[] result = cache.getPng();
        if (result == null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(toBufferedImage(), "PNG", baos);
            result = baos.toByteArray();
            cache.setPng(result);
        }
        return result;
    }

    /**
     * Calculates the scaled dimensions to fit within the given bounds while preserving aspect ratio.
     *
     * @param maxWidth  the maximum width
     * @param maxHeight the maximum height
     * @return an array of [scaledWidth, scaledHeight]
     */
    public int[] scaledDimensionsToFit(int maxWidth, int maxHeight) {
        double widthRatio = (double) maxWidth / width;
        double heightRatio = (double) maxHeight / height;
        double ratio = Math.min(widthRatio, heightRatio);
        int scaledWidth = Math.max(1, (int) Math.round(width * ratio));
        int scaledHeight = Math.max(1, (int) Math.round(height * ratio));
        return new int[] {scaledWidth, scaledHeight};
    }

    /**
     * Calculates the scaled dimensions to fill the given bounds while preserving aspect ratio.
     *
     * @param targetWidth  the target width
     * @param targetHeight the target height
     * @return an array of [scaledWidth, scaledHeight]
     */
    public int[] scaledDimensionsToFill(int targetWidth, int targetHeight) {
        double widthRatio = (double) targetWidth / width;
        double heightRatio = (double) targetHeight / height;
        double ratio = Math.max(widthRatio, heightRatio);
        int scaledWidth = Math.max(1, (int) Math.round(width * ratio));
        int scaledHeight = Math.max(1, (int) Math.round(height * ratio));
        return new int[] {scaledWidth, scaledHeight};
    }
}
