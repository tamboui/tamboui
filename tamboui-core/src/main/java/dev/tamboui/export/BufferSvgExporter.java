/*
 * Copyright (c) 2026 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.export;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;
import dev.tamboui.style.Color;
import dev.tamboui.style.Modifier;
import dev.tamboui.style.Style;

import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.zip.Adler32;

/**
 * Exports a {@link Buffer} to an SVG in the same spirit as Rich's {@code Console.export_svg}.
 * <p>
 * This exporter treats the buffer as a fixed-width grid of cells and generates:
 * <ul>
 *     <li>CSS classes for unique {@link Style}s</li>
 *     <li>Background rectangles for runs with a background color (including {@link Modifier#REVERSED})</li>
 *     <li>Text nodes clipped per line for crisp rendering</li>
 * </ul>
 *
 * <h2>Notes</h2>
 * <ul>
 *     <li>This is a pure string export; it does not require a backend.</li>
 *     <li>Colors are resolved via {@link Color#toRgb()} plus a simple theme fallback.</li>
 * </ul>
 */
public final class BufferSvgExporter {

    private BufferSvgExporter() {
    }

    /**
     * Export options (all values have reasonable defaults).
     */
    public static final class Options {
        private String title = "TamboUI";
        private Theme theme = Theme.svgExportTheme();
        private String codeFormat = DEFAULT_SVG_FORMAT;
        private double fontAspectRatio = 0.61;
        private String uniqueId;

        /** Creates default export options. */
        public Options() {
        }

        /**
         * Sets the window title.
         *
         * @param title the SVG window title
         * @return this options instance
         */
        public Options title(String title) {
            this.title = title;
            return this;
        }

        /**
         * Sets the color theme.
         *
         * @param theme the SVG color theme
         * @return this options instance
         */
        public Options theme(Theme theme) {
            this.theme = theme;
            return this;
        }

        /**
         * Sets the SVG template format string.
         *
         * @param codeFormat the SVG format template with placeholders
         * @return this options instance
         */
        public Options codeFormat(String codeFormat) {
            this.codeFormat = codeFormat;
            return this;
        }

        /**
         * Sets the font aspect ratio used for width calculations.
         *
         * @param fontAspectRatio the character width-to-height ratio
         * @return this options instance
         */
        public Options fontAspectRatio(double fontAspectRatio) {
            this.fontAspectRatio = fontAspectRatio;
            return this;
        }

        /**
         * Sets a custom unique ID prefix for CSS classes and clip paths.
         *
         * @param uniqueId the unique ID prefix, or {@code null} for auto-generated
         * @return this options instance
         */
        public Options uniqueId(String uniqueId) {
            this.uniqueId = uniqueId;
            return this;
        }
    }

    /**
     * A minimal theme for SVG export.
     */
    public static final class Theme {
        private final Color.Rgb background;
        private final Color.Rgb foreground;

        /**
         * Creates a theme with the given colors.
         *
         * @param background the background color
         * @param foreground the foreground (text) color
         */
        public Theme(Color.Rgb background, Color.Rgb foreground) {
            this.background = Objects.requireNonNull(background, "background");
            this.foreground = Objects.requireNonNull(foreground, "foreground");
        }

        /**
         * Returns the background color.
         *
         * @return the background color
         */
        public Color.Rgb background() {
            return background;
        }

        /**
         * Returns the foreground color.
         *
         * @return the foreground color
         */
        public Color.Rgb foreground() {
            return foreground;
        }

        /**
         * Default theme aligned with Rich's {@code SVG_EXPORT_THEME}.
         *
         * @return the default SVG export theme
         */
        public static Theme svgExportTheme() {
            return new Theme(new Color.Rgb(41, 41, 41), new Color.Rgb(197, 200, 198));
        }
    }

    /**
     * Exports the given buffer to SVG with default options.
     *
     * @param buffer the buffer to export
     * @return the SVG string
     */
    public static String exportSvg(Buffer buffer) {
        return exportSvg(buffer, new Options());
    }

    /**
     * Exports the given buffer to SVG with the specified options.
     *
     * @param buffer  the buffer to export
     * @param options export options (title, theme, format, etc.)
     * @return the SVG string
     */
    public static String exportSvg(Buffer buffer, Options options) {
        Objects.requireNonNull(buffer, "buffer");
        Objects.requireNonNull(options, "options");
        Objects.requireNonNull(options.theme, "options.theme");
        Objects.requireNonNull(options.codeFormat, "options.codeFormat");

        final Theme theme = options.theme;
        final int widthCells = buffer.width();
        final int heightCells = buffer.height();

        final int charHeight = 20;
        final double charWidth = charHeight * options.fontAspectRatio;
        final double lineHeight = charHeight * 1.22;

        final int marginTop = 1;
        final int marginRight = 1;
        final int marginBottom = 1;
        final int marginLeft = 1;

        final int paddingTop = 40;
        final int paddingRight = 8;
        final int paddingBottom = 8;
        final int paddingLeft = 8;

        final int paddingWidth = paddingLeft + paddingRight;
        final int paddingHeight = paddingTop + paddingBottom;
        final int marginWidth = marginLeft + marginRight;
        final int marginHeight = marginTop + marginBottom;

        final int terminalWidth = (int) Math.ceil(widthCells * charWidth + paddingWidth);
        final int terminalHeight = (int) Math.ceil(heightCells * lineHeight + paddingHeight);

        final String uniqueId = options.uniqueId != null ? options.uniqueId : "terminal-" + computeStableHash(buffer, theme);

        // Stable insertion order so class numbers are deterministic
        final Map<String, Integer> cssToClassNo = new LinkedHashMap<>();
        int nextClassNo = 1;

        final StringBuilder backgrounds = new StringBuilder();
        final StringBuilder matrix = new StringBuilder();

        for (int y = 0; y < heightCells; y++) {
            int x = 0;
            while (x < widthCells) {
                Cell cell = buffer.get(x + buffer.area().x(), y + buffer.area().y());
                Style style = cell.style();

                // Build a run of same style for fewer nodes
                int runStart = x;
                StringBuilder runText = new StringBuilder();
                while (x < widthCells) {
                    Cell c = buffer.get(x + buffer.area().x(), y + buffer.area().y());
                    if (!c.style().equals(style)) {
                        break;
                    }
                    runText.append(c.symbol());
                    x++;
                }
                int runLen = x - runStart;

                ResolvedColors colors = resolveColors(style, theme);
                boolean hasBackground = colors.hasBackground;

                String css = styleToCss(style, colors, theme);
                Integer classNo = cssToClassNo.get(css);
                if (classNo == null) {
                    classNo = nextClassNo++;
                    cssToClassNo.put(css, classNo);
                }
                String className = uniqueId + "-r" + classNo;

                if (hasBackground) {
                    backgrounds.append(makeTag(
                        "rect",
                        null,
                        "fill", colors.backgroundHex,
                        "x", format(runStart * charWidth),
                        "y", format(y * lineHeight + 1.5),
                        "width", format(charWidth * runLen),
                        "height", format(lineHeight + 0.25),
                        "shape-rendering", "crispEdges"
                    ));
                }

                String text = runText.toString();
                if (!isAllSpaces(text)) {
                    matrix.append(makeTag(
                        "text",
                        escapeText(text),
                        "class", className,
                        "x", format(runStart * charWidth),
                        "y", format(y * lineHeight + charHeight),
                        "textLength", format(charWidth * text.length()),
                        "clip-path", "url(#" + uniqueId + "-line-" + y + ")"
                    ));
                }
            }
        }

        StringBuilder lines = new StringBuilder();
        for (int y = 0; y < heightCells; y++) {
            double offset = y * lineHeight + 1.5;
            lines.append("<clipPath id=\"").append(uniqueId).append("-line-").append(y).append("\">")
                .append(makeTag(
                    "rect",
                    null,
                    "x", "0",
                    "y", format(offset),
                    "width", format(charWidth * widthCells),
                    "height", format(lineHeight + 0.25)
                ))
                .append("</clipPath>");
        }

        StringBuilder styles = new StringBuilder();
        for (Map.Entry<String, Integer> entry : cssToClassNo.entrySet()) {
            styles.append(".").append(uniqueId).append("-r").append(entry.getValue())
                .append(" { ").append(entry.getKey()).append(" }");
        }

        String chrome = buildChrome(uniqueId, options.title, theme, terminalWidth, terminalHeight, marginLeft, marginTop, charHeight);

        // Match Rich template variables
        return options.codeFormat
            .replace("{unique_id}", uniqueId)
            .replace("{char_width}", format(charWidth))
            .replace("{char_height}", String.valueOf(charHeight))
            .replace("{line_height}", format(lineHeight))
            .replace("{terminal_width}", String.valueOf((int) Math.ceil(charWidth * widthCells - 1)))
            .replace("{terminal_height}", String.valueOf((int) Math.ceil(heightCells * lineHeight - 1)))
            .replace("{width}", String.valueOf(terminalWidth + marginWidth))
            .replace("{height}", String.valueOf(terminalHeight + marginHeight))
            .replace("{terminal_x}", String.valueOf(marginLeft + paddingLeft))
            .replace("{terminal_y}", String.valueOf(marginTop + paddingTop))
            .replace("{styles}", styles.toString())
            .replace("{chrome}", chrome)
            .replace("{backgrounds}", backgrounds.toString())
            .replace("{matrix}", matrix.toString())
            .replace("{lines}", lines.toString());
    }

    private static String buildChrome(
        String uniqueId,
        String title,
        Theme theme,
        int terminalWidth,
        int terminalHeight,
        int marginLeft,
        int marginTop,
        int charHeight
    ) {
        String chrome = makeTag(
            "rect",
            null,
            "fill", toHex(theme.background()),
            "stroke", "rgba(255,255,255,0.35)",
            "stroke-width", "1",
            "x", String.valueOf(marginLeft),
            "y", String.valueOf(marginTop),
            "width", String.valueOf(terminalWidth),
            "height", String.valueOf(terminalHeight),
            "rx", "8"
        );

        if (title != null && !title.isEmpty()) {
            // Position title below the dots (dots center at y=22, radius 7, so extend to y=29)
            // Title font is 18px, so position baseline at y=22+7+4=33 to give clearance
            chrome += makeTag(
                "text",
                escapeText(title),
                "class", uniqueId + "-title",
                "fill", toHex(theme.foreground()),
                "text-anchor", "middle",
                "x", String.valueOf(terminalWidth / 2),
                "y", String.valueOf(marginTop + 33)
            );
        }

        chrome += ""
            + "<g transform=\"translate(26,22)\">"
            + "<circle cx=\"0\" cy=\"0\" r=\"7\" fill=\"#ff5f57\"/>"
            + "<circle cx=\"22\" cy=\"0\" r=\"7\" fill=\"#febc2e\"/>"
            + "<circle cx=\"44\" cy=\"0\" r=\"7\" fill=\"#28c840\"/>"
            + "</g>";

        return chrome;
    }

    private static final class ResolvedColors {
        private final String foregroundHex;
        private final String backgroundHex;
        private final boolean hasBackground;

        private ResolvedColors(String foregroundHex, String backgroundHex, boolean hasBackground) {
            this.foregroundHex = foregroundHex;
            this.backgroundHex = backgroundHex;
            this.hasBackground = hasBackground;
        }
    }

    private static ResolvedColors resolveColors(Style style, Theme theme) {
        EnumSet<Modifier> mods = style.effectiveModifiers();
        boolean reversed = mods.contains(Modifier.REVERSED);

        Color.Rgb fg = style.fg().orElse(Color.RESET).equals(Color.RESET) ? theme.foreground() : style.fg().get().toRgb();
        Color.Rgb bg = style.bg().orElse(Color.RESET).equals(Color.RESET) ? theme.background() : style.bg().get().toRgb();

        if (reversed) {
            Color.Rgb tmp = fg;
            fg = bg;
            bg = tmp;
        }

        boolean hasBackground = reversed || (style.bg().isPresent() && !style.bg().get().equals(Color.RESET));
        return new ResolvedColors(toHex(fg), toHex(bg), hasBackground);
    }

    private static String styleToCss(Style style, ResolvedColors colors, Theme theme) {
        EnumSet<Modifier> mods = style.effectiveModifiers();
        boolean dim = mods.contains(Modifier.DIM);

        String fill = colors.foregroundHex;
        if (dim) {
            // Blend foreground towards background: 40% fg, 60% bg (matches Rich's dim blend factor)
            Color.Rgb fg = Color.Rgb.fromHex(colors.foregroundHex);
            Color.Rgb bg = Color.Rgb.fromHex(colors.backgroundHex);
            fill = toHex(blend(fg, bg, 0.4));
        }

        StringBuilder css = new StringBuilder();
        css.append("fill: ").append(fill);
        if (mods.contains(Modifier.BOLD)) {
            css.append(";font-weight: bold");
        }
        if (mods.contains(Modifier.ITALIC)) {
            css.append(";font-style: italic");
        }

        boolean underline = mods.contains(Modifier.UNDERLINED);
        boolean strike = mods.contains(Modifier.CROSSED_OUT);
        if (underline || strike) {
            css.append(";text-decoration: ");
            if (underline) {
                css.append("underline");
            }
            if (underline && strike) {
                css.append(" ");
            }
            if (strike) {
                css.append("line-through");
            }
        }

        if (mods.contains(Modifier.HIDDEN)) {
            css.append(";fill: ").append(colors.backgroundHex);
        }

        return css.toString();
    }

    private static Color.Rgb blend(Color.Rgb fg, Color.Rgb bg, double fgFactor) {
        double bgFactor = 1.0 - fgFactor;
        int r = clamp((int) Math.round(fg.r() * fgFactor + bg.r() * bgFactor));
        int g = clamp((int) Math.round(fg.g() * fgFactor + bg.g() * bgFactor));
        int b = clamp((int) Math.round(fg.b() * fgFactor + bg.b() * bgFactor));
        return new Color.Rgb(r, g, b);
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    private static boolean isAllSpaces(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) != ' ') {
                return false;
            }
        }
        return true;
    }

    private static String escapeText(String text) {
        // Basic XML escaping + match Rich's nbsp for spaces
        StringBuilder sb = new StringBuilder(text.length() + 16);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '&':
                    sb.append("&amp;");
                    break;
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '"':
                    sb.append("&quot;");
                    break;
                case '\'':
                    sb.append("&#39;");
                    break;
                case ' ':
                    sb.append("&#160;");
                    break;
                default:
                    sb.append(c);
                    break;
            }
        }
        return sb.toString();
    }

    private static String makeTag(String name, String content, Object... attribPairs) {
        StringBuilder attribs = new StringBuilder();
        for (int i = 0; i < attribPairs.length; i += 2) {
            if (i > 0) {
                attribs.append(' ');
            }
            String key = String.valueOf(attribPairs[i]);
            String value = String.valueOf(attribPairs[i + 1]);
            attribs.append(key).append("=\"").append(value).append("\"");
        }
        if (content != null) {
            return "<" + name + " " + attribs + ">" + content + "</" + name + ">";
        }
        return "<" + name + " " + attribs + "/>";
    }

    private static String format(double value) {
        // Similar to Python's format(value, "g") for these magnitudes
        String s = Double.toString(value);
        if (s.endsWith(".0")) {
            return s.substring(0, s.length() - 2);
        }
        return s;
    }

    private static String toHex(Color.Rgb rgb) {
        return String.format("#%02x%02x%02x", rgb.r(), rgb.g(), rgb.b());
    }

    private static String computeStableHash(Buffer buffer, Theme theme) {
        Adler32 adler32 = new Adler32();
        adler32.update((byte) 1);
        adler32.update((byte) theme.background().r());
        adler32.update((byte) theme.background().g());
        adler32.update((byte) theme.background().b());
        adler32.update((byte) theme.foreground().r());
        adler32.update((byte) theme.foreground().g());
        adler32.update((byte) theme.foreground().b());

        for (int y = 0; y < buffer.height(); y++) {
            for (int x = 0; x < buffer.width(); x++) {
                Cell cell = buffer.get(x + buffer.area().x(), y + buffer.area().y());
                Style s = cell.style();
                ResolvedColors colors = resolveColors(s, theme);
                adler32.update(cell.symbol().getBytes(StandardCharsets.UTF_8));
                adler32.update((byte) 0);
                adler32.update(colors.foregroundHex.getBytes(StandardCharsets.US_ASCII));
                adler32.update((byte) 0);
                adler32.update(colors.backgroundHex.getBytes(StandardCharsets.US_ASCII));
                adler32.update((byte) 0);
                EnumSet<Modifier> mods = s.effectiveModifiers();
                int modBits = 0;
                for (Modifier m : Modifier.values()) {
                    if (mods.contains(m)) {
                        modBits |= (1 << m.ordinal());
                    }
                }
                adler32.update((byte) (modBits & 0xff));
                adler32.update((byte) ((modBits >> 8) & 0xff));
            }
        }
        return Long.toString(adler32.getValue());
    }

    /**
     * SVG template derived from Rich's {@code CONSOLE_SVG_FORMAT} with branding adjusted.
     */
    public static final String DEFAULT_SVG_FORMAT = "" +
        "<svg class=\"rich-terminal\" viewBox=\"0 0 {width} {height}\" xmlns=\"http://www.w3.org/2000/svg\">" +
        "    <!-- Generated with TamboUI -->" +
        "    <style>" +
        "    @font-face {" +
        "        font-family: \"Fira Code\";" +
        "        src: local(\"FiraCode-Regular\")," +
        "                url(\"https://cdnjs.cloudflare.com/ajax/libs/firacode/6.2.0/woff2/FiraCode-Regular.woff2\") format(\"woff2\")," +
        "                url(\"https://cdnjs.cloudflare.com/ajax/libs/firacode/6.2.0/woff/FiraCode-Regular.woff\") format(\"woff\");" +
        "        font-style: normal;" +
        "        font-weight: 400;" +
        "    }" +
        "    @font-face {" +
        "        font-family: \"Fira Code\";" +
        "        src: local(\"FiraCode-Bold\")," +
        "                url(\"https://cdnjs.cloudflare.com/ajax/libs/firacode/6.2.0/woff2/FiraCode-Bold.woff2\") format(\"woff2\")," +
        "                url(\"https://cdnjs.cloudflare.com/ajax/libs/firacode/6.2.0/woff/FiraCode-Bold.woff\") format(\"woff\");" +
        "        font-style: bold;" +
        "        font-weight: 700;" +
        "    }" +
        "    .{unique_id}-matrix {" +
        "        font-family: Fira Code, monospace;" +
        "        font-size: {char_height}px;" +
        "        line-height: {line_height}px;" +
        "        font-variant-east-asian: full-width;" +
        "    }" +
        "    .{unique_id}-title {" +
        "        font-size: 18px;" +
        "        font-weight: bold;" +
        "        font-family: arial;" +
        "    }" +
        "    {styles}" +
        "    </style>" +
        "    <defs>" +
        "    <clipPath id=\"{unique_id}-clip-terminal\">" +
        "      <rect x=\"0\" y=\"0\" width=\"{terminal_width}\" height=\"{terminal_height}\" />" +
        "    </clipPath>" +
        "    {lines}" +
        "    </defs>" +
        "    {chrome}" +
        "    <g transform=\"translate({terminal_x}, {terminal_y})\" clip-path=\"url(#{unique_id}-clip-terminal)\">" +
        "    {backgrounds}" +
        "    <g class=\"{unique_id}-matrix\">" +
        "    {matrix}" +
        "    </g>" +
        "    </g>" +
        "</svg>";
}
