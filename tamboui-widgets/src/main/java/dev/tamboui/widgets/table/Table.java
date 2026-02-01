/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.table;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Layout;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.ColorConverter;
import dev.tamboui.style.PropertyDefinition;
import dev.tamboui.style.PropertyRegistry;
import dev.tamboui.style.StylePropertyResolver;
import dev.tamboui.style.StandardProperties;
import dev.tamboui.style.Style;
import dev.tamboui.text.CharWidth;
import dev.tamboui.text.Line;
import dev.tamboui.text.Text;
import dev.tamboui.text.Span;
import dev.tamboui.widget.StatefulWidget;
import dev.tamboui.widgets.block.Block;
import static dev.tamboui.util.CollectionUtil.listCopyOf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;

/**
 * A table widget for displaying data in rows and columns.
 * <p>
 * The table supports optional header and footer rows, column width constraints,
 * row selection with highlighting, and scrolling for large datasets.
 *
 * <pre>{@code
 * Table table = Table.builder()
 *     .header(Row.from("Name", "Age", "City").style(Style.EMPTY.bold()))
 *     .rows(List.of(
 *         Row.from("Alice", "30", "New York"),
 *         Row.from("Bob", "25", "Los Angeles"),
 *         Row.from("Charlie", "35", "Chicago")
 *     ))
 *     .widths(
 *         Constraint.percentage(40),
 *         Constraint.length(10),
 *         Constraint.fill()
 *     )
 *     .highlightStyle(Style.EMPTY.bg(Color.BLUE))
 *     .highlightSymbol(">> ")
 *     .block(Block.bordered().title("Users"))
 *     .build();
 *
 * frame.renderStatefulWidget(table, area, tableState);
 * }</pre>
 */
public final class Table implements StatefulWidget<TableState> {

    /**
     * Property key for the row highlight (selection) color.
     * <p>
     * CSS property name: {@code highlight-color}
     */
    public static final PropertyDefinition<Color> HIGHLIGHT_COLOR =
            PropertyDefinition.of("highlight-color", ColorConverter.INSTANCE);

    static {
        PropertyRegistry.register(HIGHLIGHT_COLOR);
    }

    private final List<Row> rows;
    private final List<Constraint> widths;
    private final Row header;
    private final Row footer;
    private final Block block;
    private final Style style;
    private final Style rowHighlightStyle;
    private final String highlightSymbol;
    private final int columnSpacing;
    private final HighlightSpacing highlightSpacing;

    private Table(Builder builder) {
        this.widths = listCopyOf(builder.widths);
        this.header = builder.header;
        this.footer = builder.footer;
        this.block = builder.block;
        this.highlightSymbol = builder.highlightSymbol;
        this.columnSpacing = builder.columnSpacing;
        this.highlightSpacing = builder.highlightSpacing;

        // Resolve style-aware properties
        Color resolvedBg = builder.resolveBackground();
        Color resolvedHighlightColor = builder.resolveHighlightColor();

        Style baseStyle = builder.style;
        if (resolvedBg != null) {
            baseStyle = baseStyle.bg(resolvedBg);
        }
        this.style = baseStyle;

        Style baseHighlightStyle = builder.rowHighlightStyle;
        if (resolvedHighlightColor != null) {
            baseHighlightStyle = baseHighlightStyle.bg(resolvedHighlightColor);
        }
        this.rowHighlightStyle = baseHighlightStyle;

        // Apply row style resolver if provided
        if (builder.rowStyleResolver != null) {
            List<Row> styledRows = new ArrayList<>(builder.rows.size());
            int total = builder.rows.size();
            for (int i = 0; i < total; i++) {
                Row row = builder.rows.get(i);
                Style rowStyle = builder.rowStyleResolver.apply(i, total);
                if (rowStyle != null && !rowStyle.equals(Style.EMPTY)) {
                    styledRows.add(row.style(row.style().patch(rowStyle)));
                } else {
                    styledRows.add(row);
                }
            }
            this.rows = listCopyOf(styledRows);
        } else {
            this.rows = listCopyOf(builder.rows);
        }
    }

    /**
     * Creates a new table builder.
     *
     * @return a new Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the rows in this table.
     *
     * @return the rows
     */
    public List<Row> rows() {
        return rows;
    }

    @Override
    public void render(Rect area, Buffer buffer, TableState state) {
        if (area.isEmpty()) {
            return;
        }

        // Apply background style
        buffer.setStyle(area, style);

        // Render block if present
        Rect tableArea = area;
        if (block != null) {
            block.render(area, buffer);
            tableArea = block.inner(area);
        }

        if (tableArea.isEmpty() || widths.isEmpty()) {
            return;
        }

        // Calculate column widths
        int highlightWidth = calculateHighlightWidth(state);
        int availableWidth = tableArea.width() - highlightWidth;
        if (availableWidth <= 0) {
            return;
        }

        // Account for column spacing
        int totalSpacing = columnSpacing * Math.max(0, widths.size() - 1);
        availableWidth = Math.max(0, availableWidth - totalSpacing);

        List<Integer> columnWidths = calculateColumnWidths(availableWidth);

        // Ensure selected row is visible
        if (state.selected() != null) {
            int visibleHeight = tableArea.height();
            if (header != null) {
                visibleHeight -= header.totalHeight();
            }
            if (footer != null) {
                visibleHeight -= footer.totalHeight();
            }
            state.scrollToSelected(visibleHeight, rows);
        }

        int y = tableArea.top();

        // Render header
        if (header != null) {
            y = renderRow(buffer, tableArea, y, header, columnWidths, highlightWidth, false, Style.EMPTY);
        }

        // Calculate available height for data rows
        int dataHeight = tableArea.bottom() - y;
        if (footer != null) {
            dataHeight -= footer.totalHeight();
        }

        // Render data rows
        int offset = state.offset();
        int currentOffset = 0;

        for (int i = 0; i < rows.size() && y < tableArea.top() + tableArea.height() - (footer != null ? footer.totalHeight() : 0); i++) {
            Row row = rows.get(i);
            int rowHeight = row.totalHeight();

            // Skip rows before visible area
            if (currentOffset + rowHeight <= offset) {
                currentOffset += rowHeight;
                continue;
            }

            boolean isSelected = state.selected() != null && state.selected() == i;
            Style highlightStyle = isSelected ? rowHighlightStyle : Style.EMPTY;

            // Render highlight symbol
            if (isSelected && highlightWidth > 0) {
                buffer.setString(tableArea.left(), y, highlightSymbol, rowHighlightStyle);
            }

            y = renderRow(buffer, tableArea, y, row, columnWidths, highlightWidth, isSelected, highlightStyle);
            currentOffset += rowHeight;
        }

        // Render footer at the bottom
        if (footer != null) {
            int footerY = tableArea.bottom() - footer.totalHeight();
            renderRow(buffer, tableArea, footerY, footer, columnWidths, highlightWidth, false, Style.EMPTY);
        }
    }

    private int calculateHighlightWidth(TableState state) {
        switch (highlightSpacing) {
            case ALWAYS:
                return CharWidth.of(highlightSymbol);
            case WHEN_SELECTED:
                return state.selected() != null ? CharWidth.of(highlightSymbol) : 0;
            case NEVER:
            default:
                return 0;
        }
    }

    private List<Integer> calculateColumnWidths(int availableWidth) {
        // Use Layout to calculate column widths based on constraints
        Rect fakeArea = new Rect(0, 0, availableWidth, 1);
        List<Rect> columnRects = Layout.horizontal()
            .constraints(widths)
            .split(fakeArea);

        List<Integer> widthList = new ArrayList<>(columnRects.size());
        for (Rect rect : columnRects) {
            widthList.add(rect.width());
        }
        return widthList;
    }

    private int renderRow(Buffer buffer, Rect tableArea, int y, Row row,
                          List<Integer> columnWidths, int highlightWidth,
                          boolean isSelected, Style highlightStyle) {
        int rowHeight = row.height();
        Style rowStyle = row.style().patch(highlightStyle);

        // Apply row style to the entire row area
        if (!rowStyle.equals(Style.EMPTY)) {
            Rect rowArea = new Rect(tableArea.left() + highlightWidth, y,
                                    tableArea.width() - highlightWidth, rowHeight);
            buffer.setStyle(rowArea, rowStyle);
        }

        int x = tableArea.left() + highlightWidth;
        List<Cell> cells = row.cells();

        for (int col = 0; col < columnWidths.size(); col++) {
            int colWidth = columnWidths.get(col);

            if (col < cells.size()) {
                Cell cell = cells.get(col);
                Style cellStyle = rowStyle.patch(cell.style());
                Text content = cell.content();

                // Render each line of the cell
                List<Line> lines = content.lines();
                for (int lineIdx = 0; lineIdx < Math.min(lines.size(), rowHeight); lineIdx++) {
                    Line line = lines.get(lineIdx);
                    int lineY = y + lineIdx;
                    if (lineY >= tableArea.bottom()) {
                        break;
                    }

                    // Render line content, truncated to column width
                    int col_x = x;
                    List<Span> patchedSpans = line.patchStyle(cellStyle).spans();
                    for (int spanIdx = 0; spanIdx < patchedSpans.size(); spanIdx++) {
                        Span span = patchedSpans.get(spanIdx);
                        String text = span.content();
                        int textWidth = CharWidth.of(text);
                        int remainingWidth = colWidth - (col_x - x);
                        if (remainingWidth > 0 && textWidth > 0) {
                            String toRender = textWidth <= remainingWidth ? text : CharWidth.substringByWidth(text, remainingWidth);
                            buffer.setString(col_x, lineY, toRender, span.style());
                            col_x += CharWidth.of(toRender);
                        }
                    }
                }
            }

            x += colWidth + columnSpacing;
        }

        return y + rowHeight + row.bottomMargin();
    }

    /**
     * Controls when space is allocated for the highlight symbol.
     */
    public enum HighlightSpacing {
        /** Always allocate space for the highlight symbol. */
        ALWAYS,
        /** Only allocate space when a row is selected. */
        WHEN_SELECTED,
        /** Never allocate space for the highlight symbol. */
        NEVER
    }

    /**
     * Builder for {@link Table}.
     */
    public static final class Builder {
        private List<Row> rows = new ArrayList<>();
        private List<Constraint> widths = new ArrayList<>();
        private Row header;
        private Row footer;
        private Block block;
        private Style style = Style.EMPTY;
        private Style rowHighlightStyle = Style.EMPTY.reversed();
        private String highlightSymbol = ">> ";
        private int columnSpacing = 1;
        private HighlightSpacing highlightSpacing = HighlightSpacing.WHEN_SELECTED;
        private StylePropertyResolver styleResolver = StylePropertyResolver.empty();
        private BiFunction<Integer, Integer, Style> rowStyleResolver;

        // Style-aware properties (resolved via styleResolver in build())
        private Color background;
        private Color highlightColor;

        private Builder() {}

        /**
         * Sets the data rows.
         *
         * @param rows the rows to display
         * @return this builder
         */
        public Builder rows(List<Row> rows) {
            this.rows = new ArrayList<>(rows);
            return this;
        }

        /**
         * Sets the data rows.
         *
         * @param rows the rows to display
         * @return this builder
         */
        public Builder rows(Row... rows) {
            this.rows = new ArrayList<>(Arrays.asList(rows));
            return this;
        }

        /**
         * Adds a row.
         *
         * @param row the row to add
         * @return this builder
         */
        public Builder addRow(Row row) {
            this.rows.add(row);
            return this;
        }

        /**
         * Sets the column width constraints.
         * <p>
         * This is required - columns will have 0 width without constraints.
         *
         * @param widths the column width constraints
         * @return this builder
         */
        public Builder widths(List<Constraint> widths) {
            this.widths = new ArrayList<>(widths);
            return this;
        }

        /**
         * Sets the column width constraints.
         *
         * @param widths the column width constraints
         * @return this builder
         */
        public Builder widths(Constraint... widths) {
            this.widths = new ArrayList<>(Arrays.asList(widths));
            return this;
        }

        /**
         * Sets the header row.
         *
         * @param header the header row
         * @return this builder
         */
        public Builder header(Row header) {
            this.header = header;
            return this;
        }

        /**
         * Sets the footer row.
         *
         * @param footer the footer row
         * @return this builder
         */
        public Builder footer(Row footer) {
            this.footer = footer;
            return this;
        }

        /**
         * Wraps the table in a block.
         *
         * @param block the block to wrap in
         * @return this builder
         */
        public Builder block(Block block) {
            this.block = block;
            return this;
        }

        /**
         * Sets the base style.
         *
         * @param style the base style
         * @return this builder
         */
        public Builder style(Style style) {
            this.style = style;
            return this;
        }

        /**
         * Sets the style for the selected row.
         *
         * @param style the highlight style
         * @return this builder
         */
        public Builder highlightStyle(Style style) {
            this.rowHighlightStyle = style;
            return this;
        }

        /**
         * Sets the symbol displayed before the selected row.
         *
         * @param symbol the highlight symbol
         * @return this builder
         */
        public Builder highlightSymbol(String symbol) {
            this.highlightSymbol = symbol != null ? symbol : "";
            return this;
        }

        /**
         * Sets the spacing between columns.
         *
         * @param spacing the column spacing
         * @return this builder
         */
        public Builder columnSpacing(int spacing) {
            this.columnSpacing = Math.max(0, spacing);
            return this;
        }

        /**
         * Sets when to allocate space for the highlight symbol.
         *
         * @param spacing the highlight spacing mode
         * @return this builder
         */
        public Builder highlightSpacing(HighlightSpacing spacing) {
            this.highlightSpacing = spacing;
            return this;
        }

        /**
         * Sets the property resolver for style-aware properties.
         * <p>
         * When set, properties like {@code background} and {@code highlight-color}
         * will be resolved if not set programmatically.
         *
         * @param resolver the property resolver
         * @return this builder
         */
        public Builder styleResolver(StylePropertyResolver resolver) {
            this.styleResolver = resolver != null ? resolver : StylePropertyResolver.empty();
            return this;
        }

        /**
         * Sets the background color programmatically.
         * <p>
         * This takes precedence over values from the style resolver.
         *
         * @param color the background color
         * @return this builder
         */
        public Builder background(Color color) {
            this.background = color;
            return this;
        }

        /**
         * Sets the row highlight (selection) color programmatically.
         * <p>
         * This takes precedence over values from the style resolver.
         *
         * @param color the highlight color
         * @return this builder
         */
        public Builder highlightColor(Color color) {
            this.highlightColor = color;
            return this;
        }

        /**
         * Sets a function to resolve styles for each row based on position.
         * <p>
         * The function receives the row index (0-based) and total row count,
         * and returns a Style to apply to that row. This enables positional
         * styling like alternating row colors.
         *
         * @param resolver function that takes (index, totalCount) and returns a Style
         * @return this builder
         */
        public Builder rowStyleResolver(BiFunction<Integer, Integer, Style> resolver) {
            this.rowStyleResolver = resolver;
            return this;
        }

        /**
         * Builds the table.
         *
         * @return a new Table
         */
        public Table build() {
            return new Table(this);
        }

        // Resolution helpers
        private Color resolveBackground() {
            return styleResolver.resolve(StandardProperties.BACKGROUND, background);
        }

        private Color resolveHighlightColor() {
            return styleResolver.resolve(HIGHLIGHT_COLOR, highlightColor);
        }
    }
}
