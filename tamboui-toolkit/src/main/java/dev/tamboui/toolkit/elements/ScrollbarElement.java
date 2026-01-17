/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.element.StyledElement;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.widgets.scrollbar.Scrollbar;
import dev.tamboui.widgets.scrollbar.ScrollbarOrientation;
import dev.tamboui.widgets.scrollbar.ScrollbarState;

/**
 * A DSL wrapper for the Scrollbar widget.
 * <p>
 * Displays a scrollbar for scroll position indication.
 * <pre>{@code
 * scrollbar()
 *     .vertical()
 *     .state(scrollbarState)
 *     .thumbColor(Color.YELLOW)
 * }</pre>
 *
 * <h2>CSS Child Selectors</h2>
 * <p>
 * The following child selectors can be used to style sub-components:
 * <ul>
 *   <li>{@code ScrollbarElement-thumb} - The thumb/handle of the scrollbar</li>
 *   <li>{@code ScrollbarElement-track} - The track/background of the scrollbar</li>
 *   <li>{@code ScrollbarElement-begin} - The begin marker (up/left arrow)</li>
 *   <li>{@code ScrollbarElement-end} - The end marker (down/right arrow)</li>
 * </ul>
 * <p>
 * Example CSS:
 * <pre>{@code
 * ScrollbarElement-thumb { color: yellow; }
 * ScrollbarElement-track { color: gray; }
 * ScrollbarElement-begin { color: white; }
 * ScrollbarElement-end { color: white; }
 * }</pre>
 * <p>
 * Note: Programmatic styles set via the corresponding setter methods take precedence over CSS styles.
 */
public final class ScrollbarElement extends StyledElement<ScrollbarElement> {

    private static final Style DEFAULT_THUMB_STYLE = Style.EMPTY;
    private static final Style DEFAULT_TRACK_STYLE = Style.EMPTY;
    private static final Style DEFAULT_BEGIN_STYLE = Style.EMPTY;
    private static final Style DEFAULT_END_STYLE = Style.EMPTY;

    private ScrollbarOrientation orientation = ScrollbarOrientation.VERTICAL_RIGHT;
    private ScrollbarState state;
    private Scrollbar.SymbolSet symbols;
    private String thumbSymbol;
    private String trackSymbol;
    private String beginSymbol;
    private String endSymbol;
    private Style thumbStyle;
    private Style trackStyle;
    private Style beginStyle;
    private Style endStyle;

    public ScrollbarElement() {
    }

    /**
     * Sets vertical right-aligned orientation.
     */
    public ScrollbarElement vertical() {
        this.orientation = ScrollbarOrientation.VERTICAL_RIGHT;
        return this;
    }

    /**
     * Sets vertical left-aligned orientation.
     */
    public ScrollbarElement verticalLeft() {
        this.orientation = ScrollbarOrientation.VERTICAL_LEFT;
        return this;
    }

    /**
     * Sets horizontal bottom-aligned orientation.
     */
    public ScrollbarElement horizontal() {
        this.orientation = ScrollbarOrientation.HORIZONTAL_BOTTOM;
        return this;
    }

    /**
     * Sets horizontal top-aligned orientation.
     */
    public ScrollbarElement horizontalTop() {
        this.orientation = ScrollbarOrientation.HORIZONTAL_TOP;
        return this;
    }

    /**
     * Sets the orientation.
     */
    public ScrollbarElement orientation(ScrollbarOrientation orientation) {
        this.orientation = orientation;
        return this;
    }

    /**
     * Sets the scrollbar state.
     */
    public ScrollbarElement state(ScrollbarState state) {
        this.state = state;
        return this;
    }

    /**
     * Creates a new state with the given parameters.
     */
    public ScrollbarElement state(int contentLength, int viewportLength, int position) {
        this.state = new ScrollbarState()
            .contentLength(contentLength)
            .viewportContentLength(viewportLength)
            .position(position);
        return this;
    }

    /**
     * Sets the symbol set.
     */
    public ScrollbarElement symbols(Scrollbar.SymbolSet symbols) {
        this.symbols = symbols;
        return this;
    }

    /**
     * Uses double-line symbols.
     */
    public ScrollbarElement doubleLine() {
        this.symbols = orientation.isVertical()
            ? Scrollbar.SymbolSet.DOUBLE_VERTICAL
            : Scrollbar.SymbolSet.DOUBLE_HORIZONTAL;
        return this;
    }

    /**
     * Sets the thumb symbol.
     */
    public ScrollbarElement thumbSymbol(String symbol) {
        this.thumbSymbol = symbol;
        return this;
    }

    /**
     * Sets the track symbol.
     */
    public ScrollbarElement trackSymbol(String symbol) {
        this.trackSymbol = symbol;
        return this;
    }

    /**
     * Sets the begin marker symbol.
     */
    public ScrollbarElement beginSymbol(String symbol) {
        this.beginSymbol = symbol;
        return this;
    }

    /**
     * Sets the end marker symbol.
     */
    public ScrollbarElement endSymbol(String symbol) {
        this.endSymbol = symbol;
        return this;
    }

    /**
     * Hides the begin/end markers.
     */
    public ScrollbarElement hideMarkers() {
        this.beginSymbol = null;
        this.endSymbol = null;
        return this;
    }

    /**
     * Sets the thumb style.
     */
    public ScrollbarElement thumbStyle(Style style) {
        this.thumbStyle = style;
        return this;
    }

    /**
     * Sets the thumb color.
     */
    public ScrollbarElement thumbColor(Color color) {
        this.thumbStyle = Style.EMPTY.fg(color);
        return this;
    }

    /**
     * Sets the track style.
     */
    public ScrollbarElement trackStyle(Style style) {
        this.trackStyle = style;
        return this;
    }

    /**
     * Sets the track color.
     */
    public ScrollbarElement trackColor(Color color) {
        this.trackStyle = Style.EMPTY.fg(color);
        return this;
    }

    /**
     * Sets the begin marker style.
     */
    public ScrollbarElement beginStyle(Style style) {
        this.beginStyle = style;
        return this;
    }

    /**
     * Sets the end marker style.
     */
    public ScrollbarElement endStyle(Style style) {
        this.endStyle = style;
        return this;
    }

    @Override
    protected void renderContent(Frame frame, Rect area, RenderContext context) {
        if (area.isEmpty()) {
            return;
        }

        // Resolve styles with priority: explicit > CSS > default
        Style effectiveThumbStyle = resolveEffectiveStyle(context, "thumb", thumbStyle, DEFAULT_THUMB_STYLE);
        Style effectiveTrackStyle = resolveEffectiveStyle(context, "track", trackStyle, DEFAULT_TRACK_STYLE);
        Style effectiveBeginStyle = resolveEffectiveStyle(context, "begin", beginStyle, DEFAULT_BEGIN_STYLE);
        Style effectiveEndStyle = resolveEffectiveStyle(context, "end", endStyle, DEFAULT_END_STYLE);

        Scrollbar.Builder builder = Scrollbar.builder()
            .orientation(orientation)
            .style(context.currentStyle());

        if (symbols != null) {
            builder.symbols(symbols);
        }

        if (thumbSymbol != null) {
            builder.thumbSymbol(thumbSymbol);
        }

        if (trackSymbol != null) {
            builder.trackSymbol(trackSymbol);
        }

        if (beginSymbol != null) {
            builder.beginSymbol(beginSymbol);
        }

        if (endSymbol != null) {
            builder.endSymbol(endSymbol);
        }

        // Apply resolved styles (only if they have styling, not just EMPTY)
        if (!effectiveThumbStyle.equals(Style.EMPTY)) {
            builder.thumbStyle(effectiveThumbStyle);
        }

        if (!effectiveTrackStyle.equals(Style.EMPTY)) {
            builder.trackStyle(effectiveTrackStyle);
        }

        if (!effectiveBeginStyle.equals(Style.EMPTY)) {
            builder.beginStyle(effectiveBeginStyle);
        }

        if (!effectiveEndStyle.equals(Style.EMPTY)) {
            builder.endStyle(effectiveEndStyle);
        }

        Scrollbar widget = builder.build();
        ScrollbarState effectiveState = state != null ? state : new ScrollbarState().contentLength(100);
        frame.renderStatefulWidget(widget, area, effectiveState);
    }
}
