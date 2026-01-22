/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.scrollbar;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.*;

class ScrollbarTest {

    @Test
    @DisplayName("Vertical scrollbar renders on right edge")
    void verticalRightRendersOnRightEdge() {
        Scrollbar scrollbar = Scrollbar.builder()
            .orientation(ScrollbarOrientation.VERTICAL_RIGHT)
            .build();
        ScrollbarState state = new ScrollbarState(100).position(0);
        Rect area = new Rect(0, 0, 10, 5);
        Buffer buffer = Buffer.empty(area);

        scrollbar.render(area, buffer, state);

        // Track should be on rightmost column (x=9)
        assertThat(buffer.get(9, 0).symbol()).isIn("│", "█");
        // Content area should be empty
        assertThat(buffer.get(5, 2).symbol()).isEqualTo(" ");
    }

    @Test
    @DisplayName("Vertical scrollbar renders on left edge")
    void verticalLeftRendersOnLeftEdge() {
        Scrollbar scrollbar = Scrollbar.builder()
            .orientation(ScrollbarOrientation.VERTICAL_LEFT)
            .build();
        ScrollbarState state = new ScrollbarState(100).position(0);
        Rect area = new Rect(0, 0, 10, 5);
        Buffer buffer = Buffer.empty(area);

        scrollbar.render(area, buffer, state);

        // Track should be on leftmost column (x=0)
        assertThat(buffer.get(0, 0).symbol()).isIn("│", "█");
    }

    @Test
    @DisplayName("Horizontal scrollbar renders on bottom edge")
    void horizontalBottomRendersOnBottomEdge() {
        Scrollbar scrollbar = Scrollbar.builder()
            .orientation(ScrollbarOrientation.HORIZONTAL_BOTTOM)
            .build();
        ScrollbarState state = new ScrollbarState(100).position(0);
        Rect area = new Rect(0, 0, 10, 5);
        Buffer buffer = Buffer.empty(area);

        scrollbar.render(area, buffer, state);

        // Track should be on bottom row (y=4)
        assertThat(buffer.get(0, 4).symbol()).isIn("─", "█");
    }

    @Test
    @DisplayName("Horizontal scrollbar renders on top edge")
    void horizontalTopRendersOnTopEdge() {
        Scrollbar scrollbar = Scrollbar.builder()
            .orientation(ScrollbarOrientation.HORIZONTAL_TOP)
            .build();
        ScrollbarState state = new ScrollbarState(100).position(0);
        Rect area = new Rect(0, 0, 10, 5);
        Buffer buffer = Buffer.empty(area);

        scrollbar.render(area, buffer, state);

        // Track should be on top row (y=0)
        assertThat(buffer.get(0, 0).symbol()).isIn("─", "█");
    }

    @Test
    @DisplayName("Thumb moves with scroll position")
    void thumbMovesWithPosition() {
        Scrollbar scrollbar = Scrollbar.builder()
            .orientation(ScrollbarOrientation.VERTICAL_RIGHT)
            .build();
        Rect area = new Rect(0, 0, 5, 10);
        ScrollbarState state = new ScrollbarState(100).viewportContentLength(10);

        // At start
        state.position(0);
        Buffer buffer1 = Buffer.empty(area);
        scrollbar.render(area, buffer1, state);
        assertThat(buffer1.get(4, 0).symbol()).isEqualTo("█");

        // At end
        state.position(90);
        Buffer buffer2 = Buffer.empty(area);
        scrollbar.render(area, buffer2, state);
        assertThat(buffer2.get(4, 9).symbol()).isEqualTo("█");
    }

    @Test
    @DisplayName("Scrollbar renders with begin and end symbols")
    void rendersWithBeginEndSymbols() {
        Scrollbar scrollbar = Scrollbar.builder()
            .orientation(ScrollbarOrientation.VERTICAL_RIGHT)
            .beginSymbol("↑")
            .endSymbol("↓")
            .build();
        ScrollbarState state = new ScrollbarState(100).position(50);
        Rect area = new Rect(0, 0, 5, 5);
        Buffer buffer = Buffer.empty(area);

        scrollbar.render(area, buffer, state);

        assertThat(buffer.get(4, 0).symbol()).isEqualTo("↑");
        assertThat(buffer.get(4, 4).symbol()).isEqualTo("↓");
    }

    @Test
    @DisplayName("Scrollbar renders with custom symbols")
    void rendersWithCustomSymbols() {
        Scrollbar scrollbar = Scrollbar.builder()
            .orientation(ScrollbarOrientation.VERTICAL_RIGHT)
            .trackSymbol("░")
            .thumbSymbol("▓")
            .build();
        ScrollbarState state = new ScrollbarState(10).position(0);
        Rect area = new Rect(0, 0, 5, 5);
        Buffer buffer = Buffer.empty(area);

        scrollbar.render(area, buffer, state);

        // Should have custom symbols
        boolean hasThumb = false;
        boolean hasTrack = false;
        for (int y = 0; y < 5; y++) {
            String symbol = buffer.get(4, y).symbol();
            if (symbol.equals("▓")) {
                hasThumb = true;
            }
            if (symbol.equals("░")) {
                hasTrack = true;
            }
        }
        assertThat(hasThumb).isTrue();
        assertThat(hasTrack).isTrue();
    }

    @Test
    @DisplayName("Scrollbar with symbol set")
    void withSymbolSet() {
        Scrollbar scrollbar = Scrollbar.builder()
            .symbols(Scrollbar.SymbolSet.DOUBLE_VERTICAL)
            .build();
        ScrollbarState state = new ScrollbarState(100).position(0);
        Rect area = new Rect(0, 0, 5, 5);
        Buffer buffer = Buffer.empty(area);

        scrollbar.render(area, buffer, state);

        // Should use double vertical symbols
        assertThat(buffer.get(4, 0).symbol()).isIn("║", "█", "▲");
    }

    @Test
    @DisplayName("Scrollbar applies thumb style")
    void appliesThumbStyle() {
        Style thumbStyle = Style.EMPTY.fg(Color.YELLOW);
        Scrollbar scrollbar = Scrollbar.builder()
            .orientation(ScrollbarOrientation.VERTICAL_RIGHT)
            .thumbStyle(thumbStyle)
            .build();
        ScrollbarState state = new ScrollbarState(10).position(0);
        Rect area = new Rect(0, 0, 5, 5);
        Buffer buffer = Buffer.empty(area);

        scrollbar.render(area, buffer, state);

        // Find the thumb and check its style
        boolean foundThumb = false;
        for (int y = 0; y < 5; y++) {
            Cell cell = buffer.get(4, y);
            if (cell.symbol().equals("█")) {
                assertThat(cell.style().fg()).contains(Color.YELLOW);
                foundThumb = true;
                break;
            }
        }
        assertThat(foundThumb).isTrue();
    }

    @Test
    @DisplayName("Scrollbar applies track style")
    void appliesTrackStyle() {
        Style trackStyle = Style.EMPTY.fg(Color.DARK_GRAY);
        Scrollbar scrollbar = Scrollbar.builder()
            .orientation(ScrollbarOrientation.VERTICAL_RIGHT)
            .trackStyle(trackStyle)
            .build();
        ScrollbarState state = new ScrollbarState(100).position(50);
        Rect area = new Rect(0, 0, 5, 10);
        Buffer buffer = Buffer.empty(area);

        scrollbar.render(area, buffer, state);

        // Find track cells and check their style
        boolean foundTrack = false;
        for (int y = 0; y < 10; y++) {
            Cell cell = buffer.get(4, y);
            if (cell.symbol().equals("│")) {
                assertThat(cell.style().fg()).contains(Color.DARK_GRAY);
                foundTrack = true;
                break;
            }
        }
        assertThat(foundTrack).isTrue();
    }

    @Test
    @DisplayName("Empty area renders nothing")
    void emptyAreaRendersNothing() {
        Scrollbar scrollbar = Scrollbar.vertical();
        ScrollbarState state = new ScrollbarState(100);
        Rect area = new Rect(0, 0, 0, 0);
        Buffer buffer = Buffer.empty(new Rect(0, 0, 10, 10));

        // Should not crash
        scrollbar.render(area, buffer, state);
    }

    @Test
    @DisplayName("Zero content length renders nothing")
    void zeroContentRendersNothing() {
        Scrollbar scrollbar = Scrollbar.vertical();
        ScrollbarState state = new ScrollbarState(0);
        Rect area = new Rect(0, 0, 5, 10);
        Buffer buffer = Buffer.empty(area);

        scrollbar.render(area, buffer, state);

        // Buffer should remain empty
        for (int y = 0; y < 10; y++) {
            assertThat(buffer.get(4, y).symbol()).isEqualTo(" ");
        }
    }

    @Test
    @DisplayName("Factory method vertical() creates right-aligned scrollbar")
    void factoryVertical() {
        Scrollbar scrollbar = Scrollbar.vertical();
        assertThat(scrollbar.orientation()).isEqualTo(ScrollbarOrientation.VERTICAL_RIGHT);
    }

    @Test
    @DisplayName("Factory method horizontal() creates bottom-aligned scrollbar")
    void factoryHorizontal() {
        Scrollbar scrollbar = Scrollbar.horizontal();
        assertThat(scrollbar.orientation()).isEqualTo(ScrollbarOrientation.HORIZONTAL_BOTTOM);
    }

    @Test
    @DisplayName("SymbolSet constants have correct values")
    void symbolSetConstants() {
        assertThat(Scrollbar.SymbolSet.VERTICAL.thumb()).isEqualTo("█");
        assertThat(Scrollbar.SymbolSet.VERTICAL.track()).isEqualTo("│");
        assertThat(Scrollbar.SymbolSet.VERTICAL.begin()).isEqualTo("↑");
        assertThat(Scrollbar.SymbolSet.VERTICAL.end()).isEqualTo("↓");

        assertThat(Scrollbar.SymbolSet.HORIZONTAL.thumb()).isEqualTo("█");
        assertThat(Scrollbar.SymbolSet.HORIZONTAL.track()).isEqualTo("─");

        assertThat(Scrollbar.SymbolSet.DOUBLE_VERTICAL.begin()).isEqualTo("▲");
        assertThat(Scrollbar.SymbolSet.DOUBLE_VERTICAL.end()).isEqualTo("▼");
    }

    @Test
    @DisplayName("SymbolSet.of creates custom symbol set")
    void symbolSetOf() {
        Scrollbar.SymbolSet set = Scrollbar.SymbolSet.of("|", "#");
        assertThat(set.track()).isEqualTo("|");
        assertThat(set.thumb()).isEqualTo("#");
        assertThat(set.begin()).isNull();
        assertThat(set.end()).isNull();
        assertThat(set.hasMarkers()).isFalse();
    }

    @Test
    @DisplayName("SymbolSet.hasMarkers returns true when markers present")
    void symbolSetHasMarkers() {
        assertThat(Scrollbar.SymbolSet.VERTICAL.hasMarkers()).isTrue();
        assertThat(Scrollbar.SymbolSet.of("|", "#").hasMarkers()).isFalse();
    }

    @Test
    @DisplayName("Thumb size proportional to viewport")
    void thumbSizeProportionalToViewport() {
        Scrollbar scrollbar = Scrollbar.vertical();
        ScrollbarState state = new ScrollbarState(100).viewportContentLength(50);
        Rect area = new Rect(0, 0, 5, 10);
        Buffer buffer = Buffer.empty(area);

        scrollbar.render(area, buffer, state);

        // With viewport=50 out of content=100, thumb should be ~half the track
        int thumbCells = 0;
        for (int y = 0; y < 10; y++) {
            if (buffer.get(4, y).symbol().equals("█")) {
                thumbCells++;
            }
        }
        // Thumb should be around 5 cells (half of 10)
        assertThat(thumbCells).isBetween(4, 6);
    }
}
