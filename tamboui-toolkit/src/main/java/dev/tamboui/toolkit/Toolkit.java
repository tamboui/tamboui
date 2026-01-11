/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit;

import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.elements.BarChartElement;
import dev.tamboui.toolkit.elements.CalendarElement;
import dev.tamboui.toolkit.elements.CanvasElement;
import dev.tamboui.toolkit.elements.ChartElement;
import dev.tamboui.toolkit.elements.Column;
import dev.tamboui.toolkit.elements.DialogElement;
import dev.tamboui.toolkit.elements.GaugeElement;
import dev.tamboui.toolkit.elements.LazyElement;
import dev.tamboui.toolkit.elements.LineGaugeElement;
import dev.tamboui.toolkit.elements.ListContainer;
import dev.tamboui.toolkit.elements.Panel;
import dev.tamboui.toolkit.elements.Row;
import dev.tamboui.toolkit.elements.ScrollbarElement;
import dev.tamboui.toolkit.elements.Spacer;
import dev.tamboui.toolkit.elements.SparklineElement;
import dev.tamboui.toolkit.elements.TableElement;
import dev.tamboui.toolkit.elements.TabsElement;
import dev.tamboui.toolkit.elements.TextElement;
import dev.tamboui.toolkit.elements.TextAreaElement;
import dev.tamboui.toolkit.elements.TextInputElement;
import dev.tamboui.toolkit.elements.WaveTextElement;
import dev.tamboui.layout.Constraint;
import dev.tamboui.style.Color;
import dev.tamboui.widgets.input.TextAreaState;
import dev.tamboui.widgets.input.TextInputState;
import dev.tamboui.widgets.list.ListItem;
import dev.tamboui.widgets.scrollbar.ScrollbarState;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

/**
 * Static factory methods for building UI elements with the DSL.
 * Import statically to use:
 * <pre>
 * import static toolkit.dev.tamboui.Toolkit.*;
 *
 * panel("Title",
 *     text("Hello").bold().cyan(),
 *     row(
 *         text("Left"),
 *         spacer(),
 *         text("Right")
 *     )
 * ).rounded()
 * </pre>
 */
public final class Toolkit {

    private Toolkit() {
        // Prevent instantiation
    }

    // ==================== Text ====================

    /**
     * Creates a text element with the given content.
     *
     * @param content the text content
     * @return a new text element
     */
    public static TextElement text(String content) {
        return new TextElement(content);
    }

    /**
     * Creates a text element from any value (uses toString).
     *
     * @param value the value to convert to text
     * @return a new text element
     */
    public static TextElement text(Object value) {
        return new TextElement(value);
    }

    /**
     * Creates a text element with multiple values concatenated.
     *
     * @param values the values to concatenate
     * @return a new text element
     */
    public static TextElement text(Object... values) {
        StringBuilder sb = new StringBuilder();
        for (Object value : values) {
            sb.append(value != null ? value.toString() : "");
        }
        return new TextElement(sb.toString());
    }

    // ==================== Wave Text ====================

    /**
     * Creates a wave text element with the given text.
     * <p>
     * By default, a dark "shadow" moves through otherwise bright text.
     * <pre>{@code
     * waveText("Loading...").color(Color.CYAN)
     * waveText("Thinking...").inverted()  // Bright peak on dim text
     * }</pre>
     *
     * @param text the text to display
     * @return a new wave text element
     */
    public static WaveTextElement waveText(String text) {
        return new WaveTextElement(text);
    }

    /**
     * Creates a wave text element with the given text and color.
     *
     * @param text the text to display
     * @param color the base color
     * @return a new wave text element
     */
    public static WaveTextElement waveText(String text, Color color) {
        return new WaveTextElement(text, color);
    }

    // ==================== Containers ====================

    /**
     * Creates a panel with a title and children.
     *
     * @param title the panel title
     * @param children the child elements
     * @return a new panel
     */
    public static Panel panel(String title, Element... children) {
        return new Panel(title, children);
    }

    /**
     * Creates a panel without a title.
     *
     * @param children the child elements
     * @return a new panel
     */
    public static Panel panel(Element... children) {
        return new Panel(children);
    }

    /**
     * Creates an empty panel.
     *
     * @return a new empty panel
     */
    public static Panel panel() {
        return new Panel();
    }

    /**
     * Creates a horizontal row layout.
     *
     * @param children the child elements
     * @return a new row
     */
    public static Row row(Element... children) {
        return new Row(children);
    }

    /**
     * Creates an empty row.
     *
     * @return a new empty row
     */
    public static Row row() {
        return new Row();
    }

    /**
     * Creates a vertical column layout.
     *
     * @param children the child elements
     * @return a new column
     */
    public static Column column(Element... children) {
        return new Column(children);
    }

    /**
     * Creates an empty column.
     *
     * @return a new empty column
     */
    public static Column column() {
        return new Column();
    }

    // ==================== Dialog ====================

    /**
     * Creates a dialog with a title and children.
     * <p>
     * Dialogs auto-center in their parent area and clear the background.
     *
     * @param title the dialog title
     * @param children the child elements
     * @return a new dialog element
     */
    public static DialogElement dialog(String title, Element... children) {
        return new DialogElement(title, children);
    }

    /**
     * Creates a dialog without a title.
     *
     * @param children the child elements
     * @return a new dialog element
     */
    public static DialogElement dialog(Element... children) {
        return new DialogElement(children);
    }

    /**
     * Creates an empty dialog.
     *
     * @return a new empty dialog element
     */
    public static DialogElement dialog() {
        return new DialogElement();
    }

    // ==================== Spacer ====================

    /**
     * Creates a spacer that fills available space.
     *
     * @return a new spacer
     */
    public static Spacer spacer() {
        return Spacer.fill();
    }

    /**
     * Creates a spacer with a fixed length.
     *
     * @param length the fixed length
     * @return a new spacer
     */
    public static Spacer spacer(int length) {
        return Spacer.length(length);
    }

    // ==================== Constraints ====================

    /**
     * Creates a length constraint.
     *
     * @param value the length value
     * @return a new length constraint
     */
    public static Constraint length(int value) {
        return Constraint.length(value);
    }

    /**
     * Creates a percentage constraint.
     *
     * @param value the percentage value
     * @return a new percentage constraint
     */
    public static Constraint percent(int value) {
        return Constraint.percentage(value);
    }

    /**
     * Creates a fill constraint with weight 1.
     *
     * @return a new fill constraint
     */
    public static Constraint fill() {
        return Constraint.fill();
    }

    /**
     * Creates a fill constraint with the given weight.
     *
     * @param weight the fill weight
     * @return a new fill constraint
     */
    public static Constraint fill(int weight) {
        return Constraint.fill(weight);
    }

    /**
     * Creates a minimum constraint.
     *
     * @param value the minimum value
     * @return a new minimum constraint
     */
    public static Constraint min(int value) {
        return Constraint.min(value);
    }

    /**
     * Creates a maximum constraint.
     *
     * @param value the maximum value
     * @return a new maximum constraint
     */
    public static Constraint max(int value) {
        return Constraint.max(value);
    }

    /**
     * Creates a ratio constraint.
     *
     * @param numerator the numerator
     * @param denominator the denominator
     * @return a new ratio constraint
     */
    public static Constraint ratio(int numerator, int denominator) {
        return Constraint.ratio(numerator, denominator);
    }

    // ==================== Lazy Elements ====================

    /**
     * Creates a lazy element that evaluates the supplier on each render.
     * <p>
     * This allows state to be captured in the closure:
     * <pre>{@code
     * int count = 0;
     * lazy(() -> text("Count: " + count))
     * }</pre>
     *
     * @param supplier the supplier that provides the element
     * @return a new lazy element
     */
    public static Element lazy(Supplier<? extends Element> supplier) {
        return new LazyElement(supplier);
    }

    /**
     * Creates a panel with a title and lazy content.
     * The content supplier is evaluated on each render.
     * <pre>{@code
     * panel("Counter", () -> text("Count: " + count))
     * }</pre>
     *
     * @param title the panel title
     * @param contentSupplier the supplier that provides the content
     * @return a new panel
     */
    public static Panel panel(String title, Supplier<? extends Element> contentSupplier) {
        return new Panel(title, new LazyElement(contentSupplier));
    }

    /**
     * Creates a panel with lazy content (no title).
     *
     * @param contentSupplier the supplier that provides the content
     * @return a new panel
     */
    public static Panel panel(Supplier<? extends Element> contentSupplier) {
        return new Panel(new LazyElement(contentSupplier));
    }

    /**
     * Creates a row with lazy content.
     *
     * @param contentSupplier the supplier that provides the content
     * @return a new row
     */
    public static Row row(Supplier<? extends Element> contentSupplier) {
        return new Row(new LazyElement(contentSupplier));
    }

    /**
     * Creates a column with lazy content.
     *
     * @param contentSupplier the supplier that provides the content
     * @return a new column
     */
    public static Column column(Supplier<? extends Element> contentSupplier) {
        return new Column(new LazyElement(contentSupplier));
    }

    // ==================== Gauge ====================

    /**
     * Creates a gauge with the given ratio (0.0-1.0).
     *
     * @param ratio the ratio value (0.0-1.0)
     * @return a new gauge element
     */
    public static GaugeElement gauge(double ratio) {
        return new GaugeElement(ratio);
    }

    /**
     * Creates a gauge with the given percentage (0-100).
     *
     * @param percent the percentage value (0-100)
     * @return a new gauge element
     */
    public static GaugeElement gauge(int percent) {
        return new GaugeElement(percent);
    }

    /**
     * Creates an empty gauge.
     *
     * @return a new empty gauge element
     */
    public static GaugeElement gauge() {
        return new GaugeElement();
    }

    // ==================== Line Gauge ====================

    /**
     * Creates a line gauge with the given ratio (0.0-1.0).
     *
     * @param ratio the ratio value (0.0-1.0)
     * @return a new line gauge element
     */
    public static LineGaugeElement lineGauge(double ratio) {
        return new LineGaugeElement(ratio);
    }

    /**
     * Creates a line gauge with the given percentage (0-100).
     *
     * @param percent the percentage value (0-100)
     * @return a new line gauge element
     */
    public static LineGaugeElement lineGauge(int percent) {
        return new LineGaugeElement(percent);
    }

    /**
     * Creates an empty line gauge.
     *
     * @return a new empty line gauge element
     */
    public static LineGaugeElement lineGauge() {
        return new LineGaugeElement();
    }

    // ==================== Sparkline ====================

    /**
     * Creates a sparkline with the given data values.
     *
     * @param data the data values
     * @return a new sparkline element
     */
    public static SparklineElement sparkline(long... data) {
        return new SparklineElement(data);
    }

    /**
     * Creates a sparkline with the given data values.
     *
     * @param data the data values
     * @return a new sparkline element
     */
    public static SparklineElement sparkline(int... data) {
        return new SparklineElement(data);
    }

    /**
     * Creates a sparkline with the given data values.
     *
     * @param data the data values
     * @return a new sparkline element
     */
    public static SparklineElement sparkline(Collection<? extends Number> data) {
        return new SparklineElement(data);
    }

    /**
     * Creates an empty sparkline.
     *
     * @return a new empty sparkline element
     */
    public static SparklineElement sparkline() {
        return new SparklineElement();
    }

    // ==================== List ====================

    /**
     * Creates a list with the given items.
     *
     * @param items the list items
     * @return a new list element
     */
    public static ListContainer<?> list(String... items) {
        return new ListContainer<>(items);
    }

    /**
     * Creates a list with the given items.
     *
     * @param items the list items
     * @return a new list container
     */
    public static ListContainer<?> list(List<String> items) {
        return new ListContainer<>(items);
    }

    /**
     * Creates an empty list.
     *
     * @return a new empty list container
     */
    public static ListContainer<?> list() {
        return new ListContainer<>();
    }

    /**
     * Creates a list with pre-built ListItem objects.
     * <p>
     * This allows using styled items directly:
     * <pre>{@code
     * list(
     *     ListItem.from(Line.from(Span.styled("Hello", Style.EMPTY.green()))),
     *     ListItem.from(Line.from(Span.styled("World", Style.EMPTY.cyan())))
     * )
     * }</pre>
     *
     * @param items the list items
     * @return a new list container
     */
    public static ListContainer<?> list(ListItem... items) {
        return new ListContainer<>(items);
    }

    /**
     * Creates a list with a collection of pre-built ListItem objects.
     * <p>
     * This allows using styled items from a collection:
     * <pre>{@code
     * var items = List.of(
     *     ListItem.from(Line.from(Span.styled("Hello", Style.EMPTY.green()))),
     *     ListItem.from(Line.from(Span.styled("World", Style.EMPTY.cyan())))
     * );
     * list(items)
     * }</pre>
     *
     * @param items the list items
     * @return a new list container
     */
    public static ListContainer<?> list(Collection<ListItem> items) {
        return new ListContainer<>(items);
    }

    // ==================== Table ====================

    /**
     * Creates an empty table.
     *
     * @return a new empty table element
     */
    public static TableElement table() {
        return new TableElement();
    }

    // ==================== Tabs ====================

    /**
     * Creates tabs with the given titles.
     *
     * @param titles the tab titles
     * @return a new tabs element
     */
    public static TabsElement tabs(String... titles) {
        return new TabsElement(titles);
    }

    /**
     * Creates tabs with the given titles.
     *
     * @param titles the tab titles
     * @return a new tabs element
     */
    public static TabsElement tabs(List<String> titles) {
        return new TabsElement(titles);
    }

    /**
     * Creates empty tabs.
     *
     * @return a new empty tabs element
     */
    public static TabsElement tabs() {
        return new TabsElement();
    }

    // ==================== Text Input ====================

    /**
     * Creates a text input with the given state.
     *
     * @param state the text input state
     * @return a new text input element
     */
    public static TextInputElement textInput(TextInputState state) {
        return new TextInputElement(state);
    }

    /**
     * Creates a text input with a new state.
     *
     * @return a new text input element
     */
    public static TextInputElement textInput() {
        return new TextInputElement();
    }

    // ==================== Text Area ====================

    /**
     * Creates a text area with the given state.
     *
     * @param state the text area state
     * @return a new text area element
     */
    public static TextAreaElement textArea(TextAreaState state) {
        return new TextAreaElement(state);
    }

    /**
     * Creates a text area with a new state.
     *
     * @return a new text area element
     */
    public static TextAreaElement textArea() {
        return new TextAreaElement();
    }

    // ==================== Bar Chart ====================

    /**
     * Creates a bar chart with the given values.
     *
     * @param values the bar chart values
     * @return a new bar chart element
     */
    public static BarChartElement barChart(long... values) {
        return new BarChartElement().data(values);
    }

    /**
     * Creates an empty bar chart.
     *
     * @return a new empty bar chart element
     */
    public static BarChartElement barChart() {
        return new BarChartElement();
    }

    // ==================== Chart ====================

    /**
     * Creates an empty chart.
     *
     * @return a new empty chart element
     */
    public static ChartElement chart() {
        return new ChartElement();
    }

    // ==================== Canvas ====================

    /**
     * Creates a canvas with the given bounds.
     *
     * @param xMin the minimum x coordinate
     * @param xMax the maximum x coordinate
     * @param yMin the minimum y coordinate
     * @param yMax the maximum y coordinate
     * @return a new canvas element
     */
    public static CanvasElement canvas(double xMin, double xMax, double yMin, double yMax) {
        return new CanvasElement().bounds(xMin, xMax, yMin, yMax);
    }

    /**
     * Creates a canvas with default bounds (0.0-1.0).
     *
     * @return a new canvas element
     */
    public static CanvasElement canvas() {
        return new CanvasElement();
    }

    // ==================== Calendar ====================

    /**
     * Creates a calendar showing the given date's month.
     *
     * @param date the date to display
     * @return a new calendar element
     */
    public static CalendarElement calendar(LocalDate date) {
        return new CalendarElement(date);
    }

    /**
     * Creates a calendar showing the current month.
     *
     * @return a new calendar element
     */
    public static CalendarElement calendar() {
        return new CalendarElement();
    }

    // ==================== Scrollbar ====================

    /**
     * Creates a scrollbar with the given state.
     *
     * @param state the scrollbar state
     * @return a new scrollbar element
     */
    public static ScrollbarElement scrollbar(ScrollbarState state) {
        return new ScrollbarElement().state(state);
    }

    /**
     * Creates a scrollbar with the given parameters.
     *
     * @param contentLength the total content length
     * @param viewportLength the visible viewport length
     * @param position the current scroll position
     * @return a new scrollbar element
     */
    public static ScrollbarElement scrollbar(int contentLength, int viewportLength, int position) {
        return new ScrollbarElement().state(contentLength, viewportLength, position);
    }

    /**
     * Creates a scrollbar.
     *
     * @return a new scrollbar element
     */
    public static ScrollbarElement scrollbar() {
        return new ScrollbarElement();
    }

    // ==================== Input Utilities ====================

    /**
     * Handles common key events for text input.
     * <p>
     * Handles: character input, backspace, delete, left/right arrows, home/end.
     *
     * @param state the text input state to modify
     * @param event the key event to handle
     * @return true if the event was handled, false otherwise
     */
    public static boolean handleTextInputKey(TextInputState state, KeyEvent event) {
        switch (event.code()) {
            case BACKSPACE:
                state.deleteBackward();
                return true;
            case DELETE:
                state.deleteForward();
                return true;
            case LEFT:
                state.moveCursorLeft();
                return true;
            case RIGHT:
                state.moveCursorRight();
                return true;
            case HOME:
                state.moveCursorToStart();
                return true;
            case END:
                state.moveCursorToEnd();
                return true;
            case CHAR:
                // Don't consume characters with Ctrl or Alt modifiers - those are control sequences
                if (event.modifiers().ctrl() || event.modifiers().alt()) {
                    return false;
                }
                char c = event.character();
                if (c >= 32 && c < 127) {
                    state.insert(c);
                    return true;
                }
                return false;
            default:
                return false;
        }
    }
}
