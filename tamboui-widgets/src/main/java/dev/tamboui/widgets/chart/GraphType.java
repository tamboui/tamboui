/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.chart;

/**
 * The type of graph to render for a {@link Dataset}.
 */
public enum GraphType {
    /**
     * Renders individual points without connecting lines. This is the default graph
     * type.
     */
    SCATTER,

    /**
     * Connects data points with lines.
     */
    LINE,

    /**
     * Renders vertical bars from the X-axis to each data point.
     */
    BAR
}
