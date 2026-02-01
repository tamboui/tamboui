/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.style;

/**
 * Terminal colors supporting ANSI 16, 256-color indexed, and RGB true color modes.
 */
public interface Color {

    /**
     * Reset to default terminal color.
     */
    final class Reset implements Color {
        /** Creates a reset color instance. */
        public Reset() {
        }

        @Override
        public String toAnsiForeground() {
            return "39";
        }

        @Override
        public String toAnsiBackground() {
            return "49";
        }

        @Override
        public Rgb toRgb() {
            return new Rgb(255, 255, 255);
        }

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
         *
         * @return the ANSI palette entry
         */
        public AnsiColor color() {
            return color;
        }

        @Override
        public String toAnsiForeground() {
            return String.valueOf(color.fgCode());
        }

        @Override
        public String toAnsiBackground() {
            return String.valueOf(color.bgCode());
        }

        @Override
        public Rgb toRgb() {
            return ansiToRgb(color);
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

        /**
         * Returns the palette index.
         *
         * @return the 256-color palette index
         */
        public int index() {
            return index;
        }

        @Override
        public String toAnsiForeground() {
            return "38;5;" + index;
        }

        @Override
        public String toAnsiBackground() {
            return "48;5;" + index;
        }

        @Override
        public String toAnsiUnderline() {
            return "58;5;" + index;
        }

        @Override
        public Rgb toRgb() {
            return indexedToRgb(index);
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
     * A named color reference that maps to a style resolver class.
     * <p>
     * When used with {@code .fg(Color.RED)}, the element automatically gets the style resolver class "red",
     * allowing themes to override the color via standard class selectors like {@code .red { color: #FF5555; }}.
     */
    final class Named implements Color {
        private final String name;
        private final Color defaultValue;

        /**
         * Creates a named color.
         *
         * @param name the CSS class name (e.g., "red", "blue")
         * @param defaultValue the fallback color when no CSS rule matches
         */
        public Named(String name, Color defaultValue) {
            this.name = name;
            this.defaultValue = defaultValue;
        }

        /**
         * Returns the CSS class name for this color.
         *
         * @return the CSS class name
         */
        public String name() {
            return name;
        }

        /**
         * Returns the default color value.
         *
         * @return the fallback color
         */
        public Color defaultValue() {
            return defaultValue;
        }

        @Override
        public String toAnsiForeground() {
            return defaultValue.toAnsiForeground();
        }

        @Override
        public String toAnsiBackground() {
            return defaultValue.toAnsiBackground();
        }

        @Override
        public String toAnsiUnderline() {
            return defaultValue.toAnsiUnderline();
        }

        @Override
        public Rgb toRgb() {
            return defaultValue.toRgb();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Named)) {
                return false;
            }
            Named named = (Named) o;
            return name.equals(named.name);
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public String toString() {
            return String.format("Named[name=%s, default=%s]", name, defaultValue);
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

        /**
         * Returns the red component.
         *
         * @return the red value (0-255)
         */
        public int r() {
            return r;
        }

        /**
         * Returns the green component.
         *
         * @return the green value (0-255)
         */
        public int g() {
            return g;
        }

        /**
         * Returns the blue component.
         *
         * @return the blue value (0-255)
         */
        public int b() {
            return b;
        }

        @Override
        public String toAnsiForeground() {
            return "38;2;" + r + ";" + g + ";" + b;
        }

        @Override
        public String toAnsiBackground() {
            return "48;2;" + r + ";" + g + ";" + b;
        }

        @Override
        public String toAnsiUnderline() {
            return "58;2;" + r + ";" + g + ";" + b;
        }

        @Override
        public Rgb toRgb() {
            return this;
        }

        /**
         * Creates an RGB color from a hex string (e.g. {@code #112233} or {@code 112233}).
         *
         * @param hex the hex color string with or without leading {@code #}
         * @return the parsed RGB color
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

    /** Reset to default terminal color. */
    Color RESET = new Reset();

    /** Black. */
    Color BLACK = new Named("black", new Ansi(AnsiColor.BLACK));
    /** Red. */
    Color RED = new Named("red", new Ansi(AnsiColor.RED));
    /** Green. */
    Color GREEN = new Named("green", new Ansi(AnsiColor.GREEN));
    /** Yellow. */
    Color YELLOW = new Named("yellow", new Ansi(AnsiColor.YELLOW));
    /** Blue. */
    Color BLUE = new Named("blue", new Ansi(AnsiColor.BLUE));
    /** Magenta. */
    Color MAGENTA = new Named("magenta", new Ansi(AnsiColor.MAGENTA));
    /** Cyan. */
    Color CYAN = new Named("cyan", new Ansi(AnsiColor.CYAN));
    /** White. */
    Color WHITE = new Named("white", new Ansi(AnsiColor.BRIGHT_WHITE));
    /** Gray (ANSI WHITE renders as light gray). */
    Color GRAY = new Named("gray", new Ansi(AnsiColor.WHITE));

    /** Dark gray (ANSI BRIGHT_BLACK renders as dark gray). */
    Color DARK_GRAY = new Named("dark-gray", new Ansi(AnsiColor.BRIGHT_BLACK));
    /** Light red. */
    Color LIGHT_RED = new Named("light-red", new Ansi(AnsiColor.BRIGHT_RED));
    /** Light green. */
    Color LIGHT_GREEN = new Named("light-green", new Ansi(AnsiColor.BRIGHT_GREEN));
    /** Light yellow. */
    Color LIGHT_YELLOW = new Named("light-yellow", new Ansi(AnsiColor.BRIGHT_YELLOW));
    /** Light blue. */
    Color LIGHT_BLUE = new Named("light-blue", new Ansi(AnsiColor.BRIGHT_BLUE));
    /** Light magenta. */
    Color LIGHT_MAGENTA = new Named("light-magenta", new Ansi(AnsiColor.BRIGHT_MAGENTA));
    /** Light cyan. */
    Color LIGHT_CYAN = new Named("light-cyan", new Ansi(AnsiColor.BRIGHT_CYAN));
    /** Bright white. */
    Color BRIGHT_WHITE = new Named("bright-white", new Ansi(AnsiColor.BRIGHT_WHITE));

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
     * @return a color instance
     */
    static Color indexed(int index) {
        return new Indexed(index);
    }

    /**
     * Creates an RGB true-color value.
     *
     * @param r red component (0-255)
     * @param g green component (0-255)
     * @param b blue component (0-255)
     * @return a color instance
     */
    static Color rgb(int r, int g, int b) {
        return new Rgb(r, g, b);
    }

    /**
     * Creates an RGB true-color value from a {@code #rrggbb} string.
     *
     * @param hex the hex color string with or without leading {@code #}
     * @return a color instance
     */
    static Color hex(String hex) {
        return Rgb.fromHex(hex);
    }

    /**
     * Converts this color to an ANSI foreground color code.
     *
     * @return the ANSI code string (without CSI prefix or 'm' suffix),
     *         or empty string if not applicable
     */
    default String toAnsiForeground() {
        return "";
    }

    /**
     * Converts this color to an ANSI background color code.
     *
     * @return the ANSI code string (without CSI prefix or 'm' suffix),
     *         or empty string if not applicable
     */
    default String toAnsiBackground() {
        return "";
    }

    /**
     * Converts this color to an ANSI underline color code.
     * Note that underline colors are only supported by some terminal emulators.
     *
     * @return the ANSI code string (without CSI prefix or 'm' suffix),
     *         or empty string if the color type doesn't support underline coloring
     */
    default String toAnsiUnderline() {
        return "";
    }

    /**
     * Converts this color to RGB.
     * <p>
     * ANSI and indexed colors are converted using standard terminal color palettes.
     * Named colors delegate to their default value. Reset colors default to white.
     *
     * @return the RGB representation of this color
     */
    default Rgb toRgb() {
        return new Rgb(255, 255, 255);
    }

    /**
     * Converts an ANSI color to RGB using standard terminal colors.
     *
     * @param ansi the ANSI color
     * @return the RGB representation
     */
    static Rgb ansiToRgb(AnsiColor ansi) {
        switch (ansi) {
            case BLACK: return new Rgb(0, 0, 0);
            case RED: return new Rgb(170, 0, 0);
            case GREEN: return new Rgb(0, 170, 0);
            case YELLOW: return new Rgb(170, 85, 0);
            case BLUE: return new Rgb(0, 0, 170);
            case MAGENTA: return new Rgb(170, 0, 170);
            case CYAN: return new Rgb(0, 170, 170);
            case WHITE: return new Rgb(170, 170, 170);
            case BRIGHT_BLACK: return new Rgb(85, 85, 85);
            case BRIGHT_RED: return new Rgb(255, 85, 85);
            case BRIGHT_GREEN: return new Rgb(85, 255, 85);
            case BRIGHT_YELLOW: return new Rgb(255, 255, 85);
            case BRIGHT_BLUE: return new Rgb(85, 85, 255);
            case BRIGHT_MAGENTA: return new Rgb(255, 85, 255);
            case BRIGHT_CYAN: return new Rgb(85, 255, 255);
            case BRIGHT_WHITE: return new Rgb(255, 255, 255);
            default: return new Rgb(255, 255, 255);
        }
    }

    /**
     * Converts a 256-color indexed value to RGB.
     * <p>
     * The 256-color palette is:
     * <ul>
     *   <li>0-15: Standard ANSI colors</li>
     *   <li>16-231: 6x6x6 color cube</li>
     *   <li>232-255: Grayscale ramp</li>
     * </ul>
     *
     * @param index the palette index (0-255)
     * @return the RGB representation
     */
    static Rgb indexedToRgb(int index) {
        if (index < 16) {
            // Standard ANSI colors
            return ansiToRgb(AnsiColor.values()[index]);
        } else if (index < 232) {
            // 6x6x6 color cube (indices 16-231)
            int i = index - 16;
            int r = (i / 36) % 6;
            int g = (i / 6) % 6;
            int b = i % 6;
            // Each component: 0, 95, 135, 175, 215, 255
            return new Rgb(
                    r == 0 ? 0 : 55 + r * 40,
                    g == 0 ? 0 : 55 + g * 40,
                    b == 0 ? 0 : 55 + b * 40
            );
        } else {
            // Grayscale ramp (indices 232-255)
            int gray = 8 + (index - 232) * 10;
            return new Rgb(gray, gray, gray);
        }
    }
}
