/*
 * Copyright (c) 2026 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.export.html;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;
import dev.tamboui.export.ThemeColors;
import dev.tamboui.style.Color;
import dev.tamboui.style.Modifier;
import dev.tamboui.style.Style;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Exports a {@link Buffer} to HTML in the same spirit as Rich's {@code Console.export_html}.
 * <p>
 * Produces a full HTML document with a {@code <pre><code>} block. Styling is either embedded
 * in a stylesheet (with {@code <span class="r1">}) or inlined in spans ({@code <span style="...">}).
 * Uses {@link ThemeColors} for default colors (same as SVG export).
 *
 * <h2>Notes</h2>
 * <ul>
 *     <li>This is a pure string export; it does not require a backend.</li>
 *     <li>Colors are resolved via {@link Color#toRgb()} plus theme fallback.</li>
 * </ul>
 */
public final class HtmlExporter {

    private HtmlExporter() {
    }

    /**
     * Encodes the buffer to HTML and appends to the given output.
     * Used by the fluent export API.
     *
     * @param buffer  the buffer to export
     * @param options export options
     * @param out     where to append the HTML
     */
    static void encode(Buffer buffer, HtmlOptions options, Appendable out) {
        String html = buildHtml(buffer, options);
        try {
            out.append(html);
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String buildHtml(Buffer buffer, HtmlOptions options) {
        Objects.requireNonNull(buffer, "buffer");
        Objects.requireNonNull(options, "options");
        Objects.requireNonNull(options.theme, "options.theme");
        Objects.requireNonNull(options.codeFormat, "options.codeFormat");

        ThemeColors themeColors = options.theme;
        int widthCells = buffer.width();
        int heightCells = buffer.height();
        int baseX = buffer.area().x();
        int baseY = buffer.area().y();

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

                String htmlStyle = styleToHtmlCss(style, themeColors);
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

        String foreground = toHex(themeColors.foreground());
        String background = toHex(themeColors.background());

        return options.codeFormat
            .replace("{code}", code.toString())
            .replace("{stylesheet}", stylesheet.toString())
            .replace("{foreground}", foreground)
            .replace("{background}", background);
    }

    private static String styleToHtmlCss(Style style, ThemeColors themeColors) {
        EnumSet<Modifier> mods = style.effectiveModifiers();
        boolean reversed = mods.contains(Modifier.REVERSED);

        Color.Rgb fg = style.fg().orElse(Color.RESET).equals(Color.RESET) ? themeColors.foreground() : style.fg().get().toRgb();
        Color.Rgb bg = style.bg().orElse(Color.RESET).equals(Color.RESET) ? themeColors.background() : style.bg().get().toRgb();

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
