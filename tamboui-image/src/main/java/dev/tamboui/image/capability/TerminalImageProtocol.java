/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.image.capability;

/**
 * Types of image display support available in terminals.
 * <p>
 * Listed in order of preference (best quality first).
 */
public enum TerminalImageProtocol {
    /**
     * Kitty Graphics Protocol - modern, high-quality image display.
     * Supports PNG, compression, alpha blending, and pixel positioning.
     */
    KITTY,

    /**
     * iTerm2 inline images protocol.
     * Good quality with simple base64 encoding.
     */
    ITERM2,

    /**
     * Sixel graphics protocol (DEC standard).
     * Older but widely supported, palette-based.
     */
    SIXEL,

    /**
     * Unicode half-block character fallback.
     * Provides 1x2 virtual pixel resolution per cell.
     * Works on any terminal with Unicode support.
     */
    HALF_BLOCK,

    /**
     * Unicode Braille pattern fallback.
     * Provides 2x4 virtual pixel resolution per cell.
     * Highest character-based resolution.
     */
    BRAILLE,

    /**
     * No image support available.
     */
    NONE
}
