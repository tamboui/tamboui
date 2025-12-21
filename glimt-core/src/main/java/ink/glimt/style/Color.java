/*
 * Copyright (c) 2025 Glimt Contributors
 * SPDX-License-Identifier: MIT
 */
package ink.glimt.style;

/**
 * Terminal colors supporting ANSI 16, 256-color indexed, and RGB true color modes.
 */
public interface Color {

    /**
     * Reset to default terminal color.
     */
    final class Reset implements Color {
        @Override
        public boolean equals(Object o) {
            return o instanceof Reset;
        }

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public String toString() {
            return "Reset";
        }
    }

    /**
     * Standard ANSI 16 colors.
     */
    final class Ansi implements Color {
        private final AnsiColor color;

        /**
         * Creates an ANSI color.
         *
         * @param color the ANSI palette entry
         */
        public Ansi(AnsiColor color) {
            this.color = color;
        }

        /**
         * Returns the ANSI color.
         */
        public AnsiColor color() {
            return color;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Ansi)) {
                return false;
            }
            Ansi ansi = (Ansi) o;
            return color == ansi.color;
        }

        @Override
        public int hashCode() {
            return color != null ? color.hashCode() : 0;
        }

        @Override
        public String toString() {
            return String.format("Ansi[color=%s]", color);
        }
    }

    /**
     * 256-color palette index (0-255).
     */
    final class Indexed implements Color {
        private final int index;

        /**
         * Creates an indexed color.
         *
         * @param index palette index (0-255)
         */
        public Indexed(int index) {
            if (index < 0 || index > 255) {
                throw new IllegalArgumentException("Color index must be 0-255: " + index);
            }
            this.index = index;
        }

        /** Returns the palette index. */
        public int index() {
            return index;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Indexed)) {
                return false;
            }
            Indexed indexed = (Indexed) o;
            return index == indexed.index;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(index);
        }

        @Override
        public String toString() {
            return String.format("Indexed[index=%d]", index);
        }
    }

    /**
     * RGB true color (24-bit).
     */
    final class Rgb implements Color {
        private final int r;
        private final int g;
        private final int b;

        /**
         * Creates an RGB color.
         *
         * @param r red component (0-255)
         * @param g green component (0-255)
         * @param b blue component (0-255)
         */
        public Rgb(int r, int g, int b) {
            if (r < 0 || r > 255 || g < 0 || g > 255 || b < 0 || b > 255) {
                throw new IllegalArgumentException(
                    String.format("RGB values must be 0-255: (%d, %d, %d)", r, g, b));
            }
            this.r = r;
            this.g = g;
            this.b = b;
        }

        /** Returns the red component. */
        public int r() {
            return r;
        }

        /** Returns the green component. */
        public int g() {
            return g;
        }

        /** Returns the blue component. */
        public int b() {
            return b;
        }

        /**
         * Creates an RGB color from a hex string (e.g. {@code #112233} or {@code 112233}).
         */
        public static Rgb fromHex(String hex) {
            String h = hex.startsWith("#") ? hex.substring(1) : hex;
            if (h.length() != 6) {
                throw new IllegalArgumentException("Hex color must be 6 characters: " + hex);
            }
            int r = Integer.parseInt(h.substring(0, 2), 16);
            int g = Integer.parseInt(h.substring(2, 4), 16);
            int b = Integer.parseInt(h.substring(4, 6), 16);
            return new Rgb(r, g, b);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Rgb)) {
                return false;
            }
            Rgb rgb = (Rgb) o;
            return r == rgb.r && g == rgb.g && b == rgb.b;
        }

        @Override
        public int hashCode() {
            int result = Integer.hashCode(r);
            result = 31 * result + Integer.hashCode(g);
            result = 31 * result + Integer.hashCode(b);
            return result;
        }

        @Override
        public String toString() {
            return String.format("Rgb[r=%d, g=%d, b=%d]", r, g, b);
        }
    }

    // Singleton for reset
    Color RESET = new Reset();

    // Standard ANSI colors
    Color BLACK = new Ansi(AnsiColor.BLACK);
    Color RED = new Ansi(AnsiColor.RED);
    Color GREEN = new Ansi(AnsiColor.GREEN);
    Color YELLOW = new Ansi(AnsiColor.YELLOW);
    Color BLUE = new Ansi(AnsiColor.BLUE);
    Color MAGENTA = new Ansi(AnsiColor.MAGENTA);
    Color CYAN = new Ansi(AnsiColor.CYAN);
    Color WHITE = new Ansi(AnsiColor.WHITE);
    Color GRAY = new Ansi(AnsiColor.BRIGHT_BLACK);

    // Bright ANSI colors
    Color DARK_GRAY = new Ansi(AnsiColor.BRIGHT_BLACK);
    Color LIGHT_RED = new Ansi(AnsiColor.BRIGHT_RED);
    Color LIGHT_GREEN = new Ansi(AnsiColor.BRIGHT_GREEN);
    Color LIGHT_YELLOW = new Ansi(AnsiColor.BRIGHT_YELLOW);
    Color LIGHT_BLUE = new Ansi(AnsiColor.BRIGHT_BLUE);
    Color LIGHT_MAGENTA = new Ansi(AnsiColor.BRIGHT_MAGENTA);
    Color LIGHT_CYAN = new Ansi(AnsiColor.BRIGHT_CYAN);
    Color BRIGHT_WHITE = new Ansi(AnsiColor.BRIGHT_WHITE);

    // Factory methods
    /**
     * Creates an ANSI 16 color.
     *
     * @param color the ANSI color
     * @return a color instance
     */
    static Color ansi(AnsiColor color) {
        return new Ansi(color);
    }

    /**
     * Creates a 256-color indexed value.
     *
     * @param index palette index (0-255)
     */
    static Color indexed(int index) {
        return new Indexed(index);
    }

    /**
     * Creates an RGB true-color value.
     */
    static Color rgb(int r, int g, int b) {
        return new Rgb(r, g, b);
    }

    /**
     * Creates an RGB true-color value from a {@code #rrggbb} string.
     */
    static Color hex(String hex) {
        return Rgb.fromHex(hex);
    }
}
