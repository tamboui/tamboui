/**
 * Standard widgets for TamboUI TUI library.
 * <p>
 * This module provides a comprehensive set of widgets for building terminal user interfaces:
 * blocks, paragraphs, lists, tables, charts, canvas, gauges, and more.
 */
module dev.tamboui.widgets {
    requires transitive dev.tamboui.core;

    exports dev.tamboui.widgets.barchart;
    exports dev.tamboui.widgets.block;
    exports dev.tamboui.widgets.calendar;
    exports dev.tamboui.widgets.canvas;
    exports dev.tamboui.widgets.canvas.shapes;
    exports dev.tamboui.widgets.chart;
    exports dev.tamboui.widgets.gauge;
    exports dev.tamboui.widgets.input;
    exports dev.tamboui.widgets.list;
    exports dev.tamboui.widgets.logo;
    exports dev.tamboui.widgets.paragraph;
    exports dev.tamboui.widgets.scrollbar;
    exports dev.tamboui.widgets.sparkline;
    exports dev.tamboui.widgets.table;
    exports dev.tamboui.widgets.tabs;
}
