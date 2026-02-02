/*
 * Copyright (c) 2026 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.export.html;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;
import dev.tamboui.export.ExportProperties;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Modifier;
import dev.tamboui.style.Style;
import dev.tamboui.style.StylePropertyResolver;

/**
 * Exports a {@link Buffer} to HTML in the same spirit as Rich's {@code Console.export_html}.
 * <p>
 * Produces a full HTML document with a {@code <pre><code>} block. Styling is either embedded
 * in a stylesheet (with {@code <span class="r1">}) or inlined in spans ({@code <span style="...">}).
 * Default colors come from {@link ExportProperties} or the options resolver (same as SVG export).
 *
 * <h2>Notes</h2>
 * <ul>
 *     <li>This is a pure string export; it does not require a backend.</li>
 *     <li>Colors are resolved via {@link Color#toRgb()} plus {@link ExportProperties} defaults or options resolver.</li>
 * </ul>
 */
public final class HtmlExporter {

    private HtmlExporter() {
    }

    /**
     * Encodes the given region of the buffer to HTML and appends to the given output.
     * Used by the fluent export API.
     *
     * @param buffer  the buffer to export from
     * @param region  the rectangle to export (empty produces minimal HTML)
     * @param options export options
     * @param out     where to append the HTML
     */
    static void encode(Buffer buffer, Rect region, HtmlOptions options, Appendable out) {
        String html = buildHtml(buffer, region, options);
        try {
            out.append(html);
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String buildHtml(Buffer buffer, Rect region, HtmlOptions options) {
        Objects.requireNonNull(buffer, "buffer");
        Objects.requireNonNull(region, "region");
        Objects.requireNonNull(options, "options");

        StylePropertyResolver effective = options.styles != null ? options.styles : StylePropertyResolver.empty();
        Color.Rgb defaultForeground = effective.resolve(ExportProperties.EXPORT_FOREGROUND, null).toRgb();
        Color.Rgb defaultBackground = effective.resolve(ExportProperties.EXPORT_BACKGROUND, null).toRgb();

        if (region.isEmpty()) {
            return minimalHtml(defaultForeground, defaultBackground);
        }

        int widthCells = region.width();
        int heightCells = region.height();
        int baseX = region.x();
        int baseY = region.y();

        final Map<String, Integer> cssToClassNo = new LinkedHashMap<>();
        int nextClassNo = 1;

        StringBuilder code = new StringBuilder();

        for (int y = 0; y < heightCells; y++) {
            int x = 0;
            while (x < widthCells) {
                Cell cell = buffer.get(baseX + x, baseY + y);
                Style style = cell.style();

                StringBuilder runText = new StringBuilder();
                while (x < widthCells) {
                    Cell c = buffer.get(baseX + x, baseY + y);
                    if (!c.style().equals(style)) {
                        break;
                    }
                    String sym = c.symbol();
                    if (!sym.isEmpty()) {
                        runText.append(sym);
                    }
                    x++;
                }

                String text = runText.toString();
                if (text.isEmpty()) {
                    continue;
                }

                String htmlStyle = styleToHtmlCss(style, defaultForeground, defaultBackground);
                String escaped = escapeHtml(text);

                if (options.inlineStyles) {
                    if (!htmlStyle.isEmpty()) {
                        code.append("<span style=\"").append(htmlStyle).append("\">").append(escaped).append("</span>");
                    } else {
                        code.append(escaped);
                    }
                } else {
                    Integer classNo = cssToClassNo.get(htmlStyle);
                    if (classNo == null) {
                        classNo = nextClassNo++;
                        cssToClassNo.put(htmlStyle, classNo);
                    }
                    code.append("<span class=\"r").append(classNo).append("\">").append(escaped).append("</span>");
                }
            }
            if (y < heightCells - 1) {
                code.append('\n');
            }
        }

        StringBuilder stylesheet = new StringBuilder();
        if (!options.inlineStyles) {
            for (Map.Entry<String, Integer> e : cssToClassNo.entrySet()) {
                String rule = e.getKey();
                if (!rule.isEmpty()) {
                    stylesheet.append(".r").append(e.getValue()).append(" { ").append(rule).append(" }\n");
                }
            }
        }

        String foreground = toHex(defaultForeground);
        String background = toHex(defaultBackground);

        return DEFAULT_HTML_FORMAT
            .replace("{code}", code.toString())
            .replace("{stylesheet}", stylesheet.toString())
            .replace("{foreground}", foreground)
            .replace("{background}", background);
    }

    private static String minimalHtml(Color.Rgb defaultFg, Color.Rgb defaultBg) {
        String fg = toHex(defaultFg);
        String bg = toHex(defaultBg);
        return DEFAULT_HTML_FORMAT
            .replace("{code}", "")
            .replace("{stylesheet}", "")
            .replace("{foreground}", fg)
            .replace("{background}", bg);
    }

    private static String styleToHtmlCss(Style style, Color.Rgb defaultFg, Color.Rgb defaultBg) {
        EnumSet<Modifier> mods = style.effectiveModifiers();
        boolean reversed = mods.contains(Modifier.REVERSED);

        Color.Rgb fg = style.fg().orElse(Color.RESET).equals(Color.RESET) ? defaultFg : style.fg().get().toRgb();
        Color.Rgb bg = style.bg().orElse(Color.RESET).equals(Color.RESET) ? defaultBg : style.bg().get().toRgb();

        if (reversed) {
            Color.Rgb tmp = fg;
            fg = bg;
            bg = tmp;
        }

        boolean hasBackground = reversed || (style.bg().isPresent() && !style.bg().get().equals(Color.RESET));

        boolean dim = mods.contains(Modifier.DIM);
        String colorHex = toHex(fg);
        if (dim) {
            Color.Rgb blended = blend(fg, bg, 0.4);
            colorHex = toHex(blended);
        }

        StringBuilder css = new StringBuilder();
        css.append("color: ").append(colorHex);
        if (hasBackground) {
            css.append(";background-color: ").append(toHex(bg));
        }
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
            css.append(";color: ").append(toHex(bg));
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

    private static String toHex(Color.Rgb rgb) {
        return String.format("#%02x%02x%02x", rgb.r(), rgb.g(), rgb.b());
    }

    private static String escapeHtml(String text) {
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
                default:
                    sb.append(c);
                    break;
            }
        }
        return sb.toString();
    }

    /**
     * HTML template derived from Rich's {@code CONSOLE_HTML_FORMAT}.
     */
    public static final String DEFAULT_HTML_FORMAT = ""
        + "<!DOCTYPE html>\n"
        + "<html>\n"
        + "<head>\n"
        + "<meta charset=\"UTF-8\">\n"
        + "<style>\n"
        + "{stylesheet}\n"
        + "body {\n"
        + "    color: {foreground};\n"
        + "    background-color: {background};\n"
        + "}\n"
        + "</style>\n"
        + "</head>\n"
        + "<body>\n"
        + "    <pre style=\"font-family:Menlo,'DejaVu Sans Mono',consolas,'Courier New',monospace\"><code style=\"font-family:inherit\">{code}</code></pre>\n"
        + "</body>\n"
        + "</html>";
}
