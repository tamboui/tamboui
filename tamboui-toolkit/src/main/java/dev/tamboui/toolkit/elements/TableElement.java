/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import dev.tamboui.css.cascade.PseudoClassState;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.element.StyledElement;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.widgets.block.Title;
import dev.tamboui.widgets.table.Row;
import dev.tamboui.widgets.table.Table;
import dev.tamboui.widgets.table.TableState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A DSL wrapper for the Table widget.
 * <p>
 * Displays data in rows and columns.
 * <pre>{@code
 * table()
 *     .header("Name", "Age", "City")
 *     .row("Alice", "30", "NYC")
 *     .row("Bob", "25", "LA")
 *     .widths(percent(40), length(10), fill())
 *     .state(tableState)
 *     .title("Users")
 *     .rounded()
 * }</pre>
 */
public final class TableElement extends StyledElement<TableElement> {

    private static final Style DEFAULT_HIGHLIGHT_STYLE = Style.EMPTY.reversed();
    private static final Style DEFAULT_HEADER_STYLE = Style.EMPTY.bold();
    private static final String DEFAULT_HIGHLIGHT_SYMBOL = "> ";

    private final List<Row> rows = new ArrayList<>();
    private final List<Constraint> widths = new ArrayList<>();
    private Row header;
    private Row footer;
    private TableState state;
    private Style highlightStyle;  // null means "use CSS or default"
    private String highlightSymbol;  // null means "use CSS or default"
    private int columnSpacing = 1;
    private String title;
    private BorderType borderType;
    private Color borderColor;

    /** Creates a new empty table element. */
    public TableElement() {
    }

    /**
     * Sets the header row from strings.
     * The header style will be resolved from CSS (TableElement-header) or use default bold.
     *
     * @param cells the header cell values
     * @return this builder
     */
    public TableElement header(String... cells) {
        this.header = Row.from(cells);  // Style applied in renderContent from CSS
        return this;
    }

    /**
     * Sets the header row.
     *
     * @param header the header row
     * @return this builder
     */
    public TableElement header(Row header) {
        this.header = header;
        return this;
    }

    /**
     * Sets the footer row from strings.
     *
     * @param cells the footer cell values
     * @return this builder
     */
    public TableElement footer(String... cells) {
        this.footer = Row.from(cells);
        return this;
    }

    /**
     * Sets the footer row.
     *
     * @param footer the footer row
     * @return this builder
     */
    public TableElement footer(Row footer) {
        this.footer = footer;
        return this;
    }

    /**
     * Adds a row from strings.
     *
     * @param cells the row cell values
     * @return this builder
     */
    public TableElement row(String... cells) {
        this.rows.add(Row.from(cells));
        return this;
    }

    /**
     * Adds a row.
     *
     * @param row the row to add
     * @return this builder
     */
    public TableElement row(Row row) {
        this.rows.add(row);
        return this;
    }

    /**
     * Sets all rows.
     *
     * @param rows the rows to set
     * @return this builder
     */
    public TableElement rows(Row... rows) {
        this.rows.clear();
        this.rows.addAll(Arrays.asList(rows));
        return this;
    }

    /**
     * Sets all rows from a list.
     *
     * @param rows the rows to set
     * @return this builder
     */
    public TableElement rows(List<Row> rows) {
        this.rows.clear();
        this.rows.addAll(rows);
        return this;
    }

    /**
     * Sets the column width constraints.
     *
     * @param widths the width constraints
     * @return this builder
     */
    public TableElement widths(Constraint... widths) {
        this.widths.clear();
        this.widths.addAll(Arrays.asList(widths));
        return this;
    }

    /**
     * Sets the column width constraints from a list.
     *
     * @param widths the width constraints
     * @return this builder
     */
    public TableElement widths(List<Constraint> widths) {
        this.widths.clear();
        this.widths.addAll(widths);
        return this;
    }

    /**
     * Sets the table state for selection tracking.
     *
     * @param state the table state
     * @return this builder
     */
    public TableElement state(TableState state) {
        this.state = state;
        return this;
    }

    /**
     * Sets the highlight style for selected rows.
     *
     * @param style the highlight style
     * @return this builder
     */
    public TableElement highlightStyle(Style style) {
        this.highlightStyle = style;
        return this;
    }

    /**
     * Sets the highlight color for selected rows.
     *
     * @param color the highlight color
     * @return this builder
     */
    public TableElement highlightColor(Color color) {
        this.highlightStyle = Style.EMPTY.fg(color).bold();
        return this;
    }

    /**
     * Sets the symbol displayed before the selected row.
     *
     * @param symbol the highlight symbol
     * @return this builder
     */
    public TableElement highlightSymbol(String symbol) {
        this.highlightSymbol = symbol;
        return this;
    }

    /**
     * Sets the spacing between columns.
     *
     * @param spacing the column spacing
     * @return this builder
     */
    public TableElement columnSpacing(int spacing) {
        this.columnSpacing = spacing;
        return this;
    }

    /**
     * Sets the title.
     *
     * @param title the table title
     * @return this builder
     */
    public TableElement title(String title) {
        this.title = title;
        return this;
    }

    /**
     * Uses rounded borders.
     *
     * @return this builder
     */
    public TableElement rounded() {
        this.borderType = BorderType.ROUNDED;
        return this;
    }

    /**
     * Sets the border color.
     *
     * @param color the border color
     * @return this builder
     */
    public TableElement borderColor(Color color) {
        this.borderColor = color;
        return this;
    }

    @Override
    public Map<String, String> styleAttributes() {
        Map<String, String> attrs = new LinkedHashMap<>(super.styleAttributes());
        if (title != null) {
            attrs.put("title", title);
        }
        return Collections.unmodifiableMap(attrs);
    }

    @Override
    protected void renderContent(Frame frame, Rect area, RenderContext context) {
        if (area.isEmpty()) {
            return;
        }

        // Resolve highlight style: explicit > CSS > default
        Style effectiveHighlightStyle = resolveEffectiveStyle(
            context, "row", PseudoClassState.ofSelected(),
            highlightStyle, DEFAULT_HIGHLIGHT_STYLE);

        // Resolve highlight symbol: explicit > default
        String effectiveHighlightSymbol = highlightSymbol != null ? highlightSymbol : DEFAULT_HIGHLIGHT_SYMBOL;

        // Resolve header style from CSS
        Row effectiveHeader = header;
        if (effectiveHeader != null && effectiveHeader.style().equals(Style.EMPTY)) {
            // Header row has no explicit style, resolve from CSS or default
            Style headerStyle = resolveEffectiveStyle(context, "header", null, DEFAULT_HEADER_STYLE);
            effectiveHeader = effectiveHeader.style(headerStyle);
        }

        Table.Builder builder = Table.builder()
            .rows(rows)
            .widths(widths)
            .style(context.currentStyle())
            .highlightStyle(effectiveHighlightStyle)
            .highlightSymbol(effectiveHighlightSymbol)
            .columnSpacing(columnSpacing);

        if (effectiveHeader != null) {
            builder.header(effectiveHeader);
        }

        if (footer != null) {
            builder.footer(footer);
        }

        if (title != null || borderType != null) {
            Block.Builder blockBuilder = Block.builder()
                    .borders(Borders.ALL)
                    .styleResolver(styleResolver(context));
            if (title != null) {
                blockBuilder.title(Title.from(title));
            }
            if (borderType != null) {
                blockBuilder.borderType(borderType);
            }
            if (borderColor != null) {
                blockBuilder.borderColor(borderColor);
            }
            builder.block(blockBuilder.build());
        }

        Table widget = builder.build();
        TableState effectiveState = state != null ? state : new TableState();
        frame.renderStatefulWidget(widget, area, effectiveState);
    }
}
