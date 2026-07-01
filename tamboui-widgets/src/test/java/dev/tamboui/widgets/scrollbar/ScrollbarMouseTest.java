/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.scrollbar;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import dev.tamboui.layout.Rect;

import static org.assertj.core.api.Assertions.*;

class ScrollbarMouseTest {

    @Nested
    @DisplayName("Click-to-page")
    class ClickToPage {

        @Test
        @DisplayName("Click above thumb pages up")
        void clickAboveThumbPagesUp() {
            Scrollbar scrollbar = Scrollbar.vertical();
            ScrollbarState state = new ScrollbarState(100)
                .viewportContentLength(10)
                .position(50);
            Rect area = new Rect(0, 0, 5, 10);

            boolean consumed = scrollbar.handleMouseAction(4, 0, ScrollbarAction.PRESS, area, state);

            assertThat(consumed).isTrue();
            assertThat(state.position()).isEqualTo(40);
        }

        @Test
        @DisplayName("Click below thumb pages down")
        void clickBelowThumbPagesDown() {
            Scrollbar scrollbar = Scrollbar.vertical();
            ScrollbarState state = new ScrollbarState(100)
                .viewportContentLength(10)
                .position(50);
            Rect area = new Rect(0, 0, 5, 10);

            boolean consumed = scrollbar.handleMouseAction(4, 9, ScrollbarAction.PRESS, area, state);

            assertThat(consumed).isTrue();
            assertThat(state.position()).isEqualTo(60);
        }

        @Test
        @DisplayName("Click on thumb starts drag without changing position")
        void clickOnThumbStartsDrag() {
            Scrollbar scrollbar = Scrollbar.vertical();
            ScrollbarState state = new ScrollbarState(100)
                .viewportContentLength(10)
                .position(0);
            Rect area = new Rect(0, 0, 5, 10);

            boolean consumed = scrollbar.handleMouseAction(4, 0, ScrollbarAction.PRESS, area, state);

            assertThat(consumed).isTrue();
            assertThat(state.isDragging()).isTrue();
            assertThat(state.position()).isEqualTo(0);
        }

        @Test
        @DisplayName("Click outside scrollbar area is not consumed")
        void clickOutsideNotConsumed() {
            Scrollbar scrollbar = Scrollbar.vertical();
            ScrollbarState state = new ScrollbarState(100).position(50);
            Rect area = new Rect(0, 0, 5, 10);

            boolean consumed = scrollbar.handleMouseAction(2, 5, ScrollbarAction.PRESS, area, state);

            assertThat(consumed).isFalse();
            assertThat(state.position()).isEqualTo(50);
        }

        @Test
        @DisplayName("Click pages up repeatedly toward start")
        void repeatedPageUp() {
            Scrollbar scrollbar = Scrollbar.vertical();
            ScrollbarState state = new ScrollbarState(100)
                .viewportContentLength(10)
                .position(15);
            Rect area = new Rect(0, 0, 5, 10);

            scrollbar.handleMouseAction(4, 0, ScrollbarAction.PRESS, area, state);
            assertThat(state.position()).isEqualTo(5);
            state.endDrag();

            scrollbar.handleMouseAction(4, 0, ScrollbarAction.PRESS, area, state);
            assertThat(state.position()).isEqualTo(0);
        }

        @Test
        @DisplayName("Page down clamps at end")
        void pageDownClampsAtEnd() {
            Scrollbar scrollbar = Scrollbar.vertical();
            ScrollbarState state = new ScrollbarState(100)
                .viewportContentLength(10)
                .position(95);
            Rect area = new Rect(0, 0, 5, 10);

            scrollbar.handleMouseAction(4, 9, ScrollbarAction.PRESS, area, state);

            assertThat(state.position()).isEqualTo(99);
        }
    }

    @Nested
    @DisplayName("Drag-to-scroll")
    class DragToScroll {

        @Test
        @DisplayName("Drag thumb moves position proportionally")
        void dragMovesProportionally() {
            Scrollbar scrollbar = Scrollbar.vertical();
            ScrollbarState state = new ScrollbarState(100)
                .viewportContentLength(10)
                .position(0);
            Rect area = new Rect(0, 0, 5, 10);

            scrollbar.handleMouseAction(4, 0, ScrollbarAction.PRESS, area, state);
            assertThat(state.isDragging()).isTrue();

            scrollbar.handleMouseAction(4, 5, ScrollbarAction.DRAG, area, state);
            assertThat(state.position()).isGreaterThan(0);
        }

        @Test
        @DisplayName("Drag to top of track scrolls to start")
        void dragToTopScrollsToStart() {
            Scrollbar scrollbar = Scrollbar.vertical();
            ScrollbarState state = new ScrollbarState(100)
                .viewportContentLength(10)
                .position(50);
            Rect area = new Rect(0, 0, 5, 10);

            state.startDrag(0);

            scrollbar.handleMouseAction(4, 0, ScrollbarAction.DRAG, area, state);
            assertThat(state.position()).isEqualTo(0);
        }

        @Test
        @DisplayName("Drag to bottom of track scrolls to end")
        void dragToBottomScrollsToEnd() {
            Scrollbar scrollbar = Scrollbar.vertical();
            ScrollbarState state = new ScrollbarState(100)
                .viewportContentLength(10)
                .position(0);
            Rect area = new Rect(0, 0, 5, 10);

            state.startDrag(0);

            scrollbar.handleMouseAction(4, 9, ScrollbarAction.DRAG, area, state);
            assertThat(state.position()).isEqualTo(90);
        }

        @Test
        @DisplayName("Drag beyond track bounds is clamped")
        void dragBeyondBoundsIsClamped() {
            Scrollbar scrollbar = Scrollbar.vertical();
            ScrollbarState state = new ScrollbarState(100)
                .viewportContentLength(10)
                .position(50);
            Rect area = new Rect(0, 0, 5, 10);

            state.startDrag(0);

            scrollbar.handleMouseAction(4, -5, ScrollbarAction.DRAG, area, state);
            assertThat(state.position()).isEqualTo(0);

            scrollbar.handleMouseAction(4, 20, ScrollbarAction.DRAG, area, state);
            assertThat(state.position()).isEqualTo(90);
        }

        @Test
        @DisplayName("Drag outside cross-axis still works while dragging")
        void dragOutsideCrossAxisWorks() {
            Scrollbar scrollbar = Scrollbar.vertical();
            ScrollbarState state = new ScrollbarState(100)
                .viewportContentLength(10)
                .position(0);
            Rect area = new Rect(0, 0, 5, 10);

            state.startDrag(0);

            boolean consumed = scrollbar.handleMouseAction(0, 5, ScrollbarAction.DRAG, area, state);
            assertThat(consumed).isTrue();
            assertThat(state.position()).isGreaterThan(0);
        }

        @Test
        @DisplayName("Release ends drag")
        void releaseEndsDrag() {
            Scrollbar scrollbar = Scrollbar.vertical();
            ScrollbarState state = new ScrollbarState(100)
                .viewportContentLength(10)
                .position(0);
            Rect area = new Rect(0, 0, 5, 10);

            scrollbar.handleMouseAction(4, 0, ScrollbarAction.PRESS, area, state);
            assertThat(state.isDragging()).isTrue();

            boolean consumed = scrollbar.handleMouseAction(4, 5, ScrollbarAction.RELEASE, area, state);
            assertThat(consumed).isTrue();
            assertThat(state.isDragging()).isFalse();
        }

        @Test
        @DisplayName("Drag without prior press is not consumed")
        void dragWithoutPressNotConsumed() {
            Scrollbar scrollbar = Scrollbar.vertical();
            ScrollbarState state = new ScrollbarState(100).position(50);
            Rect area = new Rect(0, 0, 5, 10);

            boolean consumed = scrollbar.handleMouseAction(4, 5, ScrollbarAction.DRAG, area, state);
            assertThat(consumed).isFalse();
            assertThat(state.position()).isEqualTo(50);
        }
    }

    @Nested
    @DisplayName("Scroll wheel")
    class ScrollWheel {

        @Test
        @DisplayName("Scroll up within scrollbar scrolls up by one")
        void scrollUpByOne() {
            Scrollbar scrollbar = Scrollbar.vertical();
            ScrollbarState state = new ScrollbarState(100).position(50);
            Rect area = new Rect(0, 0, 5, 10);

            boolean consumed = scrollbar.handleMouseAction(4, 5, ScrollbarAction.SCROLL_UP, area, state);

            assertThat(consumed).isTrue();
            assertThat(state.position()).isEqualTo(49);
        }

        @Test
        @DisplayName("Scroll down within scrollbar scrolls down by one")
        void scrollDownByOne() {
            Scrollbar scrollbar = Scrollbar.vertical();
            ScrollbarState state = new ScrollbarState(100).position(50);
            Rect area = new Rect(0, 0, 5, 10);

            boolean consumed = scrollbar.handleMouseAction(4, 5, ScrollbarAction.SCROLL_DOWN, area, state);

            assertThat(consumed).isTrue();
            assertThat(state.position()).isEqualTo(51);
        }

        @Test
        @DisplayName("Scroll outside scrollbar is not consumed")
        void scrollOutsideNotConsumed() {
            Scrollbar scrollbar = Scrollbar.vertical();
            ScrollbarState state = new ScrollbarState(100).position(50);
            Rect area = new Rect(0, 0, 5, 10);

            boolean consumed = scrollbar.handleMouseAction(2, 5, ScrollbarAction.SCROLL_UP, area, state);

            assertThat(consumed).isFalse();
            assertThat(state.position()).isEqualTo(50);
        }
    }

    @Nested
    @DisplayName("Horizontal orientation")
    class HorizontalOrientation {

        @Test
        @DisplayName("Click before thumb on horizontal scrollbar pages left")
        void clickBeforeThumbPagesLeft() {
            Scrollbar scrollbar = Scrollbar.horizontal();
            ScrollbarState state = new ScrollbarState(100)
                .viewportContentLength(10)
                .position(50);
            Rect area = new Rect(0, 0, 10, 5);

            boolean consumed = scrollbar.handleMouseAction(0, 4, ScrollbarAction.PRESS, area, state);

            assertThat(consumed).isTrue();
            assertThat(state.position()).isEqualTo(40);
        }

        @Test
        @DisplayName("Click after thumb on horizontal scrollbar pages right")
        void clickAfterThumbPagesRight() {
            Scrollbar scrollbar = Scrollbar.horizontal();
            ScrollbarState state = new ScrollbarState(100)
                .viewportContentLength(10)
                .position(50);
            Rect area = new Rect(0, 0, 10, 5);

            boolean consumed = scrollbar.handleMouseAction(9, 4, ScrollbarAction.PRESS, area, state);

            assertThat(consumed).isTrue();
            assertThat(state.position()).isEqualTo(60);
        }

        @Test
        @DisplayName("Drag on horizontal scrollbar uses X axis")
        void dragUsesXAxis() {
            Scrollbar scrollbar = Scrollbar.horizontal();
            ScrollbarState state = new ScrollbarState(100)
                .viewportContentLength(10)
                .position(0);
            Rect area = new Rect(0, 0, 10, 5);

            state.startDrag(0);

            scrollbar.handleMouseAction(9, 4, ScrollbarAction.DRAG, area, state);
            assertThat(state.position()).isEqualTo(90);
        }

        @Test
        @DisplayName("Horizontal top scrollbar handles mouse on top edge")
        void horizontalTopOnTopEdge() {
            Scrollbar scrollbar = Scrollbar.builder()
                .orientation(ScrollbarOrientation.HORIZONTAL_TOP)
                .build();
            ScrollbarState state = new ScrollbarState(100).position(50);
            Rect area = new Rect(0, 0, 10, 5);

            boolean consumed = scrollbar.handleMouseAction(5, 0, ScrollbarAction.SCROLL_DOWN, area, state);

            assertThat(consumed).isTrue();
            assertThat(state.position()).isEqualTo(51);
        }
    }

    @Nested
    @DisplayName("Begin/end markers")
    class BeginEndMarkers {

        @Test
        @DisplayName("Click on begin marker scrolls up by one")
        void clickOnBeginMarker() {
            Scrollbar scrollbar = Scrollbar.builder()
                .orientation(ScrollbarOrientation.VERTICAL_RIGHT)
                .beginSymbol("↑")
                .endSymbol("↓")
                .build();
            ScrollbarState state = new ScrollbarState(100)
                .viewportContentLength(10)
                .position(50);
            Rect area = new Rect(0, 0, 5, 10);

            boolean consumed = scrollbar.handleMouseAction(4, 0, ScrollbarAction.PRESS, area, state);

            assertThat(consumed).isTrue();
            assertThat(state.position()).isEqualTo(49);
        }

        @Test
        @DisplayName("Click on end marker scrolls down by one")
        void clickOnEndMarker() {
            Scrollbar scrollbar = Scrollbar.builder()
                .orientation(ScrollbarOrientation.VERTICAL_RIGHT)
                .beginSymbol("↑")
                .endSymbol("↓")
                .build();
            ScrollbarState state = new ScrollbarState(100)
                .viewportContentLength(10)
                .position(50);
            Rect area = new Rect(0, 0, 5, 10);

            boolean consumed = scrollbar.handleMouseAction(4, 9, ScrollbarAction.PRESS, area, state);

            assertThat(consumed).isTrue();
            assertThat(state.position()).isEqualTo(51);
        }

        @Test
        @DisplayName("Markers reduce effective track length for thumb calculation")
        void markersReduceTrackLength() {
            Scrollbar withMarkers = Scrollbar.builder()
                .orientation(ScrollbarOrientation.VERTICAL_RIGHT)
                .beginSymbol("↑")
                .endSymbol("↓")
                .build();
            Scrollbar noMarkers = Scrollbar.vertical();
            ScrollbarState state = new ScrollbarState(100)
                .viewportContentLength(10)
                .position(0);
            Rect area = new Rect(0, 0, 5, 10);

            state.startDrag(0);
            withMarkers.handleMouseAction(4, 9, ScrollbarAction.DRAG, area, state);
            int posWithMarkers = state.position();
            state.endDrag();

            state.position(0);
            state.startDrag(0);
            noMarkers.handleMouseAction(4, 9, ScrollbarAction.DRAG, area, state);
            int posWithout = state.position();
            state.endDrag();

            assertThat(posWithMarkers).isEqualTo(90);
            assertThat(posWithout).isEqualTo(90);
        }
    }

    @Nested
    @DisplayName("Vertical left orientation")
    class VerticalLeft {

        @Test
        @DisplayName("Click on left-aligned scrollbar uses left edge")
        void clickUsesLeftEdge() {
            Scrollbar scrollbar = Scrollbar.builder()
                .orientation(ScrollbarOrientation.VERTICAL_LEFT)
                .build();
            ScrollbarState state = new ScrollbarState(100).position(50);
            Rect area = new Rect(0, 0, 10, 10);

            boolean consumed = scrollbar.handleMouseAction(0, 5, ScrollbarAction.SCROLL_DOWN, area, state);

            assertThat(consumed).isTrue();
            assertThat(state.position()).isEqualTo(51);
        }

        @Test
        @DisplayName("Click on wrong edge of left-aligned scrollbar is not consumed")
        void clickOnWrongEdge() {
            Scrollbar scrollbar = Scrollbar.builder()
                .orientation(ScrollbarOrientation.VERTICAL_LEFT)
                .build();
            ScrollbarState state = new ScrollbarState(100).position(50);
            Rect area = new Rect(0, 0, 10, 10);

            boolean consumed = scrollbar.handleMouseAction(9, 5, ScrollbarAction.SCROLL_DOWN, area, state);

            assertThat(consumed).isFalse();
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("Empty area returns false")
        void emptyArea() {
            Scrollbar scrollbar = Scrollbar.vertical();
            ScrollbarState state = new ScrollbarState(100);
            Rect area = new Rect(0, 0, 0, 0);

            boolean consumed = scrollbar.handleMouseAction(0, 0, ScrollbarAction.PRESS, area, state);
            assertThat(consumed).isFalse();
        }

        @Test
        @DisplayName("Zero content length returns false")
        void zeroContent() {
            Scrollbar scrollbar = Scrollbar.vertical();
            ScrollbarState state = new ScrollbarState(0);
            Rect area = new Rect(0, 0, 5, 10);

            boolean consumed = scrollbar.handleMouseAction(4, 5, ScrollbarAction.PRESS, area, state);
            assertThat(consumed).isFalse();
        }

        @Test
        @DisplayName("Content fits viewport does not change position on drag")
        void contentFitsViewport() {
            Scrollbar scrollbar = Scrollbar.vertical();
            ScrollbarState state = new ScrollbarState(5)
                .viewportContentLength(10)
                .position(0);
            Rect area = new Rect(0, 0, 5, 10);

            state.startDrag(0);
            scrollbar.handleMouseAction(4, 5, ScrollbarAction.DRAG, area, state);

            assertThat(state.position()).isEqualTo(0);
        }

        @Test
        @DisplayName("Release when not dragging returns false outside bounds")
        void releaseNotDraggingOutside() {
            Scrollbar scrollbar = Scrollbar.vertical();
            ScrollbarState state = new ScrollbarState(100).position(50);
            Rect area = new Rect(0, 0, 5, 10);

            boolean consumed = scrollbar.handleMouseAction(0, 0, ScrollbarAction.RELEASE, area, state);

            assertThat(consumed).isFalse();
        }

        @Test
        @DisplayName("Release when not dragging returns true inside bounds")
        void releaseNotDraggingInside() {
            Scrollbar scrollbar = Scrollbar.vertical();
            ScrollbarState state = new ScrollbarState(100).position(50);
            Rect area = new Rect(0, 0, 5, 10);

            boolean consumed = scrollbar.handleMouseAction(4, 5, ScrollbarAction.RELEASE, area, state);

            assertThat(consumed).isTrue();
        }

        @Test
        @DisplayName("Release during drag with zero content ends drag")
        void releaseWithZeroContentEndsDrag() {
            Scrollbar scrollbar = Scrollbar.vertical();
            ScrollbarState state = new ScrollbarState(0);
            state.startDrag(0);
            Rect area = new Rect(0, 0, 5, 10);

            boolean consumed = scrollbar.handleMouseAction(4, 5, ScrollbarAction.RELEASE, area, state);

            assertThat(consumed).isTrue();
            assertThat(state.isDragging()).isFalse();
        }

        @Test
        @DisplayName("Non-zero offset area works correctly")
        void nonZeroOffsetArea() {
            Scrollbar scrollbar = Scrollbar.vertical();
            ScrollbarState state = new ScrollbarState(100).position(50);
            Rect area = new Rect(10, 5, 5, 10);

            boolean consumed = scrollbar.handleMouseAction(14, 10, ScrollbarAction.SCROLL_DOWN, area, state);

            assertThat(consumed).isTrue();
            assertThat(state.position()).isEqualTo(51);
        }

        @Test
        @DisplayName("Full drag sequence: press, drag, release")
        void fullDragSequence() {
            Scrollbar scrollbar = Scrollbar.vertical();
            ScrollbarState state = new ScrollbarState(100)
                .viewportContentLength(10)
                .position(0);
            Rect area = new Rect(0, 0, 5, 10);

            scrollbar.handleMouseAction(4, 0, ScrollbarAction.PRESS, area, state);
            assertThat(state.isDragging()).isTrue();
            assertThat(state.position()).isEqualTo(0);

            scrollbar.handleMouseAction(4, 4, ScrollbarAction.DRAG, area, state);
            int midPosition = state.position();
            assertThat(midPosition).isGreaterThan(0);
            assertThat(midPosition).isLessThan(90);

            scrollbar.handleMouseAction(4, 9, ScrollbarAction.DRAG, area, state);
            assertThat(state.position()).isEqualTo(90);

            scrollbar.handleMouseAction(4, 9, ScrollbarAction.RELEASE, area, state);
            assertThat(state.isDragging()).isFalse();
            assertThat(state.position()).isEqualTo(90);
        }
    }
}
