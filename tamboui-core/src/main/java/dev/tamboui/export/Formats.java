/*
 * Copyright (c) 2026 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.export;

import dev.tamboui.export.html.HtmlFormat;
import dev.tamboui.export.html.HtmlOptions;
import dev.tamboui.export.svg.SvgFormat;
import dev.tamboui.export.svg.SvgOptions;
import dev.tamboui.export.text.TextFormat;
import dev.tamboui.export.text.TextOptions;

/**
 * Built-in export formats. Use with the fluent API via {@link ExportRequest#as(Format)}
 * or the shorthands {@link ExportRequest#svg()}, {@link ExportRequest#html()}, {@link ExportRequest#text()}:
 *
 * <pre>{@code
 * import static dev.tamboui.export.ExportRequest.export;
 *
 * export(buffer).svg().options(o -> o.title("App")).toString();
 * export(buffer).html().options(o -> o.inlineStyles(true)).toFile(path);
 * export(buffer).text().options(o -> o.styles(true)).toString();
 * }</pre>
 */
public final class Formats {

    private Formats() {
    }

    /** SVG format. Options: title, chrome, theme, fontAspectRatio, uniqueId. */
    public static final Format<SvgOptions> SVG = SvgFormat.instance();

    /** HTML format. Options: theme, inlineStyles. */
    public static final Format<HtmlOptions> HTML = HtmlFormat.instance();

    /** Text format (plain or ANSI). Options: styles (true = include ANSI codes). */
    public static final Format<TextOptions> TEXT = TextFormat.instance();
}
