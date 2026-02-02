/*
 * Copyright (c) 2026 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.export;

import dev.tamboui.style.Color;
import dev.tamboui.style.ColorConverter;
import dev.tamboui.style.PropertyDefinition;
import dev.tamboui.style.PropertyRegistry;

/**
 * Property definitions for export default colors.
 * <p>
 * When a cell style does not specify foreground or background (e.g. after RESET),
 * exporters use these properties to resolve default colors. Use
 * {@link dev.tamboui.export.svg.SvgOptions#styles(dev.tamboui.style.StylePropertyResolver)}
 * or {@link dev.tamboui.export.html.HtmlOptions#styles(dev.tamboui.style.StylePropertyResolver)}
 * to supply a resolver (e.g. from the toolkit style engine); when no resolver is
 * provided, the property defaults below are used (dark-theme aligned with Rich's export).
 */
public final class ExportProperties {

    private ExportProperties() {
    }

    /**
     * Default foreground color for export when a style has no foreground (e.g. RESET).
     */
    public static final PropertyDefinition<Color> EXPORT_FOREGROUND =
            PropertyDefinition.builder("export-foreground", ColorConverter.INSTANCE)
                    .defaultValue(Color.rgb(197, 200, 198))
                    .build();

    /**
     * Default background color for export when a style has no background (e.g. RESET).
     */
    public static final PropertyDefinition<Color> EXPORT_BACKGROUND =
            PropertyDefinition.builder("export-background", ColorConverter.INSTANCE)
                    .defaultValue(Color.rgb(41, 41, 41))
                    .build();

    static {
        PropertyRegistry.register(EXPORT_FOREGROUND);
        PropertyRegistry.register(EXPORT_BACKGROUND);
    }
}
