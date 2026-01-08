/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.image;

/**
 * Defines how an image should be scaled to fit the available terminal area.
 */
public enum ImageScaling {
    /**
     * Scale the image to fit within the bounds while maintaining aspect ratio.
     * The image will be as large as possible without exceeding the bounds.
     */
    FIT,

    /**
     * Scale the image to fill the bounds while maintaining aspect ratio.
     * The image may be cropped to fit.
     */
    FILL,

    /**
     * Stretch the image to exactly fill the bounds.
     * The aspect ratio may be distorted.
     */
    STRETCH,

    /**
     * Do not scale the image. Render at original size.
     * If the image is larger than the bounds, it will be cropped.
     */
    NONE
}
