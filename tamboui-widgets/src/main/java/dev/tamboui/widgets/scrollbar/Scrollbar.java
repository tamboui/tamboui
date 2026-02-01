/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.scrollbar;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.ColorConverter;
import dev.tamboui.style.PropertyDefinition;
import dev.tamboui.style.PropertyRegistry;
import dev.tamboui.style.StylePropertyResolver;
import dev.tamboui.style.Style;
import dev.tamboui.widget.StatefulWidget;

import java.util.Objects;

/**
 * A scrollbar widget for displaying scroll position.
 * <p>
 * The scrollbar can be oriented vertically (left/right) or horizontally (top/bottom).
 * It consists of:
 * <ul>
 *   <li><b>track</b> - the background line</li>
 *   <li><b>thumb</b> - the position indicator that moves along the track</li>
 *   <li><b>begin/end symbols</b> - optional arrows at the ends</li>
 * </ul>
 *
 * <pre>{@code
 * // Create a vertical scrollbar
 * Scrollbar scrollbar = Scrollbar.builder()
 *     .orientation(ScrollbarOrientation.VERTICAL_RIGHT)
 *     .thumbStyle(Style.EMPTY.fg(Color.YELLOW))
 *     .build();
 *
 * // Create state for 100 items
 * ScrollbarState state = new ScrollbarState()
 *     .contentLength(100)
 *     .position(currentScrollPosition);
 *
 * // Render in a frame
 * frame.renderStatefulWidget(scrollbar, area, state);
 * }</pre>
 *
 * @see ScrollbarState
 * @see ScrollbarOrientation
 */
public final class Scrollbar implements StatefulWidget<ScrollbarState> {

    /**
     * Property key for the thumb (position indicator) color.
     * <p>
     * CSS property name: {@code thumb-color}
     */
    public static final PropertyDefinition<Color> THUMB_COLOR =
            PropertyDefinition.of("thumb-color", ColorConverter.INSTANCE);

    /**
     * Property key for the track (background) color.
     * <p>
     * CSS property name: {@code track-color}
     */
    public static final PropertyDefinition<Color> TRACK_COLOR =
            PropertyDefinition.of("track-color", ColorConverter.INSTANCE);

    static {
        PropertyRegistry.registerAll(THUMB_COLOR, TRACK_COLOR);
    }

    /**
     * Scrollbar symbol set for rendering.
     * <p>
     * Contains characters used for track, thumb, and optional begin/end markers:
     * <ul>
     *   <li><b>track</b> - the character for the scrollbar track/background</li>
     *   <li><b>thumb</b> - the character for the thumb/position indicator</li>
     *   <li><b>begin</b> - the optional character for the start marker (can be null)</li>
     *   <li><b>end</b> - the optional character for the end marker (can be null)</li>
     * </ul>
     */
    public static final class SymbolSet {
        private final String track;
        private final String thumb;
        private final String begin;
        private final String end;

        /**
         * Creates a new symbol set.
         *
         * @param track the track character
         * @param thumb the thumb character
         * @param begin the begin marker character, or null
         * @param end   the end marker character, or null
         */
        public SymbolSet(String track, String thumb, String begin, String end) {
            this.track = track;
            this.thumb = thumb;
            this.begin = begin;
            this.end = end;
        }
        /**
         * Vertical scrollbar with single-line track and arrows.
         */
        public static final SymbolSet VERTICAL = new SymbolSet("│", "█", "↑", "↓");

        /**
         * Horizontal scrollbar with single-line track and arrows.
         */
        public static final SymbolSet HORIZONTAL = new SymbolSet("─", "█", "←", "→");

        /**
         * Vertical scrollbar with double-line track and triangle arrows.
         */
        public static final SymbolSet DOUBLE_VERTICAL = new SymbolSet("║", "█", "▲", "▼");

        /**
         * Horizontal scrollbar with double-line track and triangle arrows.
         */
        public static final SymbolSet DOUBLE_HORIZONTAL = new SymbolSet("═", "█", "◄", "►");

        /**
         * Creates a symbol set without begin/end markers.
         *
         * @param track the track character
         * @param thumb the thumb character
         * @return a new SymbolSet
         */
        public static SymbolSet of(String track, String thumb) {
            return new SymbolSet(track, thumb, null, null);
        }

        /**
         * Creates a symbol set with all components.
         *
         * @param track the track character
         * @param thumb the thumb character
         * @param begin the begin marker character
         * @param end   the end marker character
         * @return a new SymbolSet
         */
        public static SymbolSet of(String track, String thumb, String begin, String end) {
            return new SymbolSet(track, thumb, begin, end);
        }

        /**
         * Returns whether this set has begin/end markers.
         *
         * @return true if both begin and end markers are present
         */
        public boolean hasMarkers() {
            return begin != null && end != null;
        }

        /**
         * Returns the track character.
         *
         * @return the track character
         */
        public String track() {
            return track;
        }

        /**
         * Returns the thumb character.
         *
         * @return the thumb character
         */
        public String thumb() {
            return thumb;
        }

        /**
         * Returns the begin marker character.
         *
         * @return the begin marker, or null
         */
        public String begin() {
            return begin;
        }

        /**
         * Returns the end marker character.
         *
         * @return the end marker, or null
         */
        public String end() {
            return end;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof SymbolSet)) {
                return false;
            }
            SymbolSet symbolSet = (SymbolSet) o;
            return track.equals(symbolSet.track)
                && thumb.equals(symbolSet.thumb)
                && Objects.equals(begin, symbolSet.begin)
                && Objects.equals(end, symbolSet.end);
        }

        @Override
        public int hashCode() {
            int result = track.hashCode();
            result = 31 * result + thumb.hashCode();
            result = 31 * result + (begin != null ? begin.hashCode() : 0);
            result = 31 * result + (end != null ? end.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return String.format("SymbolSet[track=%s, thumb=%s, begin=%s, end=%s]", track, thumb, begin, end);
        }
    }

    private final ScrollbarOrientation orientation;
    private final SymbolSet symbols;
    private final String thumbSymbol;
    private final String trackSymbol;
    private final String beginSymbol;
    private final String endSymbol;
    private final Style style;
    private final Style thumbStyle;
    private final Style trackStyle;
    private final Style beginStyle;
    private final Style endStyle;

    private Scrollbar(Builder builder) {
        this.orientation = builder.orientation;
        this.symbols = builder.symbols;
        this.thumbSymbol = builder.thumbSymbol;
        this.trackSymbol = builder.trackSymbol;
        this.beginSymbol = builder.beginSymbol;
        this.endSymbol = builder.endSymbol;
        this.style = builder.style;
        this.beginStyle = builder.beginStyle;
        this.endStyle = builder.endStyle;

        // Resolve style-aware properties
        Color resolvedThumbColor = builder.resolveThumbColor();
        Color resolvedTrackColor = builder.resolveTrackColor();

        Style baseThumbStyle = builder.thumbStyle;
        if (resolvedThumbColor != null) {
            baseThumbStyle = baseThumbStyle != null
                    ? baseThumbStyle.fg(resolvedThumbColor)
                    : Style.EMPTY.fg(resolvedThumbColor);
        }
        this.thumbStyle = baseThumbStyle;

        Style baseTrackStyle = builder.trackStyle;
        if (resolvedTrackColor != null) {
            baseTrackStyle = baseTrackStyle != null
                    ? baseTrackStyle.fg(resolvedTrackColor)
                    : Style.EMPTY.fg(resolvedTrackColor);
        }
        this.trackStyle = baseTrackStyle;
    }

    /**
     * Creates a new scrollbar builder.
     *
     * @return a new Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a vertical right-aligned scrollbar with default settings.
     *
     * @return a new vertical Scrollbar
     */
    public static Scrollbar vertical() {
        return builder().orientation(ScrollbarOrientation.VERTICAL_RIGHT).build();
    }

    /**
     * Creates a horizontal bottom-aligned scrollbar with default settings.
     *
     * @return a new horizontal Scrollbar
     */
    public static Scrollbar horizontal() {
        return builder().orientation(ScrollbarOrientation.HORIZONTAL_BOTTOM).build();
    }

    /**
     * Returns the orientation of this scrollbar.
     *
     * @return the orientation
     */
    public ScrollbarOrientation orientation() {
        return orientation;
    }

    @Override
    public void render(Rect area, Buffer buffer, ScrollbarState state) {
        if (area.isEmpty() || state.contentLength() == 0) {
            return;
        }

        // Determine the scrollbar track area based on orientation
        Rect trackArea = calculateTrackArea(area);
        if (trackArea.isEmpty()) {
            return;
        }

        // Get effective symbols
        String effectiveThumb = thumbSymbol != null ? thumbSymbol : getDefaultThumb();
        String effectiveTrack = trackSymbol != null ? trackSymbol : getDefaultTrack();
        String effectiveBegin = beginSymbol;
        String effectiveEnd = endSymbol;

        // Calculate track dimensions
        int trackLength = orientation.isVertical() ? trackArea.height() : trackArea.width();

        // Account for begin/end markers
        int beginOffset = 0;
        int endOffset = 0;
        if (effectiveBegin != null) {
            beginOffset = 1;
            trackLength--;
        }
        if (effectiveEnd != null) {
            endOffset = 1;
            trackLength--;
        }

        if (trackLength <= 0) {
            return;
        }

        // Calculate thumb position and size
        int contentLength = state.contentLength();
        int viewportLength = state.viewportContentLength() > 0
            ? state.viewportContentLength()
            : trackLength;

        // Thumb size proportional to viewport/content ratio
        int thumbSize = Math.max(1, (int) Math.ceil((double) viewportLength / contentLength * trackLength));
        thumbSize = Math.min(thumbSize, trackLength);

        // Thumb position
        int scrollableRange = contentLength - viewportLength;
        int thumbPosition;
        if (scrollableRange <= 0) {
            thumbPosition = 0;
        } else {
            double scrollFraction = (double) state.position() / scrollableRange;
            scrollFraction = Math.max(0.0, Math.min(1.0, scrollFraction));
            int thumbRange = trackLength - thumbSize;
            thumbPosition = (int) Math.round(scrollFraction * thumbRange);
        }

        // Get effective styles
        Style effectiveStyle = style != null ? style : Style.EMPTY;
        Style effectiveThumbStyle = thumbStyle != null ? thumbStyle.patch(effectiveStyle) : effectiveStyle;
        Style effectiveTrackStyle = trackStyle != null ? trackStyle.patch(effectiveStyle) : effectiveStyle;
        Style effectiveBeginStyle = beginStyle != null ? beginStyle.patch(effectiveStyle) : effectiveStyle;
        Style effectiveEndStyle = endStyle != null ? endStyle.patch(effectiveStyle) : effectiveStyle;

        // Render based on orientation
        if (orientation.isVertical()) {
            renderVertical(buffer, trackArea, effectiveTrack, effectiveThumb,
                effectiveBegin, effectiveEnd, effectiveTrackStyle, effectiveThumbStyle,
                effectiveBeginStyle, effectiveEndStyle, beginOffset, thumbPosition, thumbSize, trackLength);
        } else {
            renderHorizontal(buffer, trackArea, effectiveTrack, effectiveThumb,
                effectiveBegin, effectiveEnd, effectiveTrackStyle, effectiveThumbStyle,
                effectiveBeginStyle, effectiveEndStyle, beginOffset, thumbPosition, thumbSize, trackLength);
        }
    }

    private void renderVertical(Buffer buffer, Rect area, String track, String thumb,
                                 String begin, String end, Style trackStyle, Style thumbStyle,
                                 Style beginStyle, Style endStyle, int beginOffset,
                                 int thumbPos, int thumbSize, int trackLength) {
        int x = area.x();
        int y = area.y();

        // Render begin marker
        if (begin != null) {
            buffer.setString(x, y, begin, beginStyle);
            y++;
        }

        // Render track with thumb
        for (int i = 0; i < trackLength; i++) {
            if (i >= thumbPos && i < thumbPos + thumbSize) {
                buffer.setString(x, y + i, thumb, thumbStyle);
            } else {
                buffer.setString(x, y + i, track, trackStyle);
            }
        }

        // Render end marker
        if (end != null) {
            buffer.setString(x, y + trackLength, end, endStyle);
        }
    }

    private void renderHorizontal(Buffer buffer, Rect area, String track, String thumb,
                                   String begin, String end, Style trackStyle, Style thumbStyle,
                                   Style beginStyle, Style endStyle, int beginOffset,
                                   int thumbPos, int thumbSize, int trackLength) {
        int x = area.x();
        int y = area.y();

        // Render begin marker
        if (begin != null) {
            buffer.setString(x, y, begin, beginStyle);
            x++;
        }

        // Render track with thumb
        for (int i = 0; i < trackLength; i++) {
            if (i >= thumbPos && i < thumbPos + thumbSize) {
                buffer.setString(x + i, y, thumb, thumbStyle);
            } else {
                buffer.setString(x + i, y, track, trackStyle);
            }
        }

        // Render end marker
        if (end != null) {
            buffer.setString(x + trackLength, y, end, endStyle);
        }
    }

    private Rect calculateTrackArea(Rect area) {
        switch (orientation) {
            case VERTICAL_RIGHT:
                return new Rect(area.right() - 1, area.y(), 1, area.height());
            case VERTICAL_LEFT:
                return new Rect(area.x(), area.y(), 1, area.height());
            case HORIZONTAL_BOTTOM:
                return new Rect(area.x(), area.bottom() - 1, area.width(), 1);
            case HORIZONTAL_TOP:
            default:
                return new Rect(area.x(), area.y(), area.width(), 1);
        }
    }

    private String getDefaultThumb() {
        if (symbols != null) {
            return symbols.thumb();
        }
        return orientation.isVertical() ? SymbolSet.VERTICAL.thumb() : SymbolSet.HORIZONTAL.thumb();
    }

    private String getDefaultTrack() {
        if (symbols != null) {
            return symbols.track();
        }
        return orientation.isVertical() ? SymbolSet.VERTICAL.track() : SymbolSet.HORIZONTAL.track();
    }

    /**
     * Builder for {@link Scrollbar}.
     */
    public static final class Builder {
        private ScrollbarOrientation orientation = ScrollbarOrientation.VERTICAL_RIGHT;
        private SymbolSet symbols;
        private String thumbSymbol;
        private String trackSymbol;
        private String beginSymbol;
        private String endSymbol;
        private Style style;
        private Style thumbStyle;
        private Style trackStyle;
        private Style beginStyle;
        private Style endStyle;
        private StylePropertyResolver styleResolver = StylePropertyResolver.empty();

        // Style-aware properties (resolved via styleResolver in build())
        private Color thumbColor;
        private Color trackColor;

        private Builder() {}

        /**
         * Sets the scrollbar orientation.
         *
         * @param orientation the scrollbar orientation
         * @return this builder
         */
        public Builder orientation(ScrollbarOrientation orientation) {
            this.orientation = orientation;
            return this;
        }

        /**
         * Sets the symbol set for this scrollbar.
         * <p>
         * This sets all symbols at once. Individual symbol setters override these.
         *
         * @param symbols the symbol set
         * @return this builder
         */
        public Builder symbols(SymbolSet symbols) {
            this.symbols = symbols;
            if (symbols != null) {
                this.trackSymbol = symbols.track();
                this.thumbSymbol = symbols.thumb();
                this.beginSymbol = symbols.begin();
                this.endSymbol = symbols.end();
            }
            return this;
        }

        /**
         * Sets the thumb (position indicator) symbol.
         *
         * @param thumbSymbol the thumb symbol
         * @return this builder
         */
        public Builder thumbSymbol(String thumbSymbol) {
            this.thumbSymbol = thumbSymbol;
            return this;
        }

        /**
         * Sets the track (background) symbol.
         *
         * @param trackSymbol the track symbol
         * @return this builder
         */
        public Builder trackSymbol(String trackSymbol) {
            this.trackSymbol = trackSymbol;
            return this;
        }

        /**
         * Sets the begin marker symbol (e.g., up arrow for vertical scrollbar).
         * <p>
         * Set to null to disable the begin marker.
         *
         * @param beginSymbol the begin marker symbol, or null
         * @return this builder
         */
        public Builder beginSymbol(String beginSymbol) {
            this.beginSymbol = beginSymbol;
            return this;
        }

        /**
         * Sets the end marker symbol (e.g., down arrow for vertical scrollbar).
         * <p>
         * Set to null to disable the end marker.
         *
         * @param endSymbol the end marker symbol, or null
         * @return this builder
         */
        public Builder endSymbol(String endSymbol) {
            this.endSymbol = endSymbol;
            return this;
        }

        /**
         * Sets the base style applied to all scrollbar components.
         *
         * @param style the base style
         * @return this builder
         */
        public Builder style(Style style) {
            this.style = style;
            return this;
        }

        /**
         * Sets the style for the thumb (position indicator).
         *
         * @param thumbStyle the thumb style
         * @return this builder
         */
        public Builder thumbStyle(Style thumbStyle) {
            this.thumbStyle = thumbStyle;
            return this;
        }

        /**
         * Sets the style for the track (background).
         *
         * @param trackStyle the track style
         * @return this builder
         */
        public Builder trackStyle(Style trackStyle) {
            this.trackStyle = trackStyle;
            return this;
        }

        /**
         * Sets the style for the begin marker.
         *
         * @param beginStyle the begin marker style
         * @return this builder
         */
        public Builder beginStyle(Style beginStyle) {
            this.beginStyle = beginStyle;
            return this;
        }

        /**
         * Sets the style for the end marker.
         *
         * @param endStyle the end marker style
         * @return this builder
         */
        public Builder endStyle(Style endStyle) {
            this.endStyle = endStyle;
            return this;
        }

        /**
         * Sets the property resolver for style-aware properties.
         * <p>
         * When set, properties like {@code thumb-color} and {@code track-color}
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
         * Sets the thumb (position indicator) color programmatically.
         * <p>
         * This takes precedence over values from the style resolver.
         *
         * @param color the thumb color
         * @return this builder
         */
        public Builder thumbColor(Color color) {
            this.thumbColor = color;
            return this;
        }

        /**
         * Sets the track (background) color programmatically.
         * <p>
         * This takes precedence over values from the style resolver.
         *
         * @param color the track color
         * @return this builder
         */
        public Builder trackColor(Color color) {
            this.trackColor = color;
            return this;
        }

        /**
         * Builds the scrollbar.
         *
         * @return a new Scrollbar
         */
        public Scrollbar build() {
            return new Scrollbar(this);
        }

        // Resolution helpers
        private Color resolveThumbColor() {
            return styleResolver.resolve(THUMB_COLOR, thumbColor);
        }

        private Color resolveTrackColor() {
            return styleResolver.resolve(TRACK_COLOR, trackColor);
        }
    }
}
