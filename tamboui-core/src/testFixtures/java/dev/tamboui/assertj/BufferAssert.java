/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.assertj;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import org.assertj.core.api.AbstractAssert;

/**
 * AssertJ custom assertion for {@link Buffer}.
 * <p>
 * Provides buffer-specific assertions with detailed diff output similar to ratatui.rs.
 */
public final class BufferAssert extends AbstractAssert<BufferAssert, Buffer> {

    public BufferAssert(Buffer actual) {
        super(actual, BufferAssert.class);
    }

    /**
     * Asserts that the actual buffer is equal to the expected buffer.
     * <p>
     * If the buffers differ, a detailed diff is shown in the error message,
     * displaying both buffers side-by-side with their content formatted as strings.
     *
     * @param expected the expected buffer
     * @return this assertion object
     * @throws AssertionError if the buffers are not equal
     */
    public BufferAssert isEqualTo(Buffer expected) {
        isNotNull();

        if (expected == null) {
            failWithMessage("Expected buffer to be null, but was: %s", formatBuffer(actual));
            return this;
        }

        if (!actual.equals(expected)) {
            String diff = BufferDiffFormatter.formatDiff(actual, expected);
            failWithMessage("Expected buffer to equal expected buffer, but they differ:%n%n%s", diff);
        }

        return this;
    }

    /**
     * Asserts that the actual buffer is not equal to the expected buffer.
     *
     * @param expected the buffer that should not equal the actual buffer
     * @return this assertion object
     * @throws AssertionError if the buffers are equal
     */
    public BufferAssert isNotEqualTo(Buffer expected) {
        isNotNull();

        if (expected != null && actual.equals(expected)) {
            failWithMessage("Expected buffer to not equal expected buffer, but they are equal:%n%n%s", formatBuffer(actual));
        }

        return this;
    }

    /**
     * Asserts that the buffer has the given area.
     *
     * @param expectedArea the expected area
     * @return this assertion object
     */
    public BufferAssert hasArea(dev.tamboui.layout.Rect expectedArea) {
        isNotNull();

        if (!actual.area().equals(expectedArea)) {
            failWithMessage("Expected buffer area to be <%s>, but was <%s>", expectedArea, actual.area());
        }

        return this;
    }

    /**
     * Asserts that the buffer has the given width.
     *
     * @param expectedWidth the expected width
     * @return this assertion object
     */
    public BufferAssert hasWidth(int expectedWidth) {
        isNotNull();

        if (actual.width() != expectedWidth) {
            failWithMessage("Expected buffer width to be <%d>, but was <%d>", expectedWidth, actual.width());
        }

        return this;
    }

    /**
     * Asserts that the buffer has the given height.
     *
     * @param expectedHeight the expected height
     * @return this assertion object
     */
    public BufferAssert hasHeight(int expectedHeight) {
        isNotNull();

        if (actual.height() != expectedHeight) {
            failWithMessage("Expected buffer height to be <%d>, but was <%d>", expectedHeight, actual.height());
        }

        return this;
    }

    /**
     * Asserts that the cell at the given position equals the expected cell.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @param expectedCell the expected cell
     * @return this assertion object
     */
    public BufferAssert hasCellAt(int x, int y, Cell expectedCell) {
        isNotNull();

        Cell actualCell = actual.get(x, y);
        if (!actualCell.equals(expectedCell)) {
            failWithMessage("Expected cell at (%d, %d) to be <%s>, but was <%s>", x, y, expectedCell, actualCell);
        }

        return this;
    }

    /**
     * Asserts that the symbol at the given position equals the expected symbol.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @param expectedSymbol the expected symbol
     * @return this assertion object
     */
    public BufferAssert hasSymbolAt(int x, int y, String expectedSymbol) {
        isNotNull();

        String actualSymbol = actual.get(x, y).symbol();
        if (!actualSymbol.equals(expectedSymbol)) {
            failWithMessage("Expected symbol at (%d, %d) to be <%s>, but was <%s>%n%nBuffer content:%n%s",
                    x, y, expectedSymbol, actualSymbol, formatBuffer(actual));
        }

        return this;
    }

    /**
     * Asserts that the style at the given position equals the expected style.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @param expectedStyle the expected style
     * @return this assertion object
     */
    public BufferAssert hasStyleAt(int x, int y, Style expectedStyle) {
        isNotNull();

        Style actualStyle = actual.get(x, y).style();
        if (!actualStyle.equals(expectedStyle)) {
            failWithMessage("Expected style at (%d, %d) to be <%s>, but was <%s>",
                    x, y, expectedStyle, actualStyle);
        }

        return this;
    }

    /**
     * Asserts that the foreground color at the given position equals the expected color.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @param expectedFg the expected foreground color
     * @return this assertion object
     */
    public BufferAssert hasForegroundAt(int x, int y, Color expectedFg) {
        isNotNull();

        Style actualStyle = actual.get(x, y).style();
        Color actualFg = actualStyle.fg().orElse(null);
        if (expectedFg == null ? actualFg != null : !expectedFg.equals(actualFg)) {
            failWithMessage("Expected foreground at (%d, %d) to be <%s>, but was <%s>",
                    x, y, expectedFg, actualFg);
        }

        return this;
    }

    /**
     * Asserts that the background color at the given position equals the expected color.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @param expectedBg the expected background color
     * @return this assertion object
     */
    public BufferAssert hasBackgroundAt(int x, int y, Color expectedBg) {
        isNotNull();

        Style actualStyle = actual.get(x, y).style();
        Color actualBg = actualStyle.bg().orElse(null);
        if (expectedBg == null ? actualBg != null : !expectedBg.equals(actualBg)) {
            failWithMessage("Expected background at (%d, %d) to be <%s>, but was <%s>",
                    x, y, expectedBg, actualBg);
        }

        return this;
    }

    /**
     * Returns a cell assertion for the cell at the given position.
     * <p>
     * Allows fluent assertions on a specific cell:
     * <pre>{@code
     * assertThat(buffer).at(2, 0)
     *     .hasSymbol("|")
     *     .hasBackground(Color.BLUE);
     * }</pre>
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @return a cell assertion object
     */
    public CellAssert at(int x, int y) {
        isNotNull();
        return new CellAssert(this, actual.get(x, y), x, y);
    }

    /**
     * Fluent assertion for a cell at a specific position.
     */
    public static final class CellAssert {
        private final BufferAssert bufferAssert;
        private final Cell cell;
        private final int x;
        private final int y;

        private CellAssert(BufferAssert bufferAssert, Cell cell, int x, int y) {
            this.bufferAssert = bufferAssert;
            this.cell = cell;
            this.x = x;
            this.y = y;
        }

        /**
         * Asserts that the cell has the expected symbol.
         *
         * @param expectedSymbol the expected symbol
         * @return this assertion object for chaining
         */
        public CellAssert hasSymbol(String expectedSymbol) {
            String actualSymbol = cell.symbol();
            if (!actualSymbol.equals(expectedSymbol)) {
                throw new AssertionError(String.format(
                        "Expected symbol at (%d, %d) to be <%s>, but was <%s>",
                        x, y, expectedSymbol, actualSymbol));
            }
            return this;
        }

        /**
         * Asserts that the cell has the expected style.
         *
         * @param expectedStyle the expected style
         * @return this assertion object for chaining
         */
        public CellAssert hasStyle(Style expectedStyle) {
            Style actualStyle = cell.style();
            if (!actualStyle.equals(expectedStyle)) {
                throw new AssertionError(String.format(
                        "Expected style at (%d, %d) to be <%s>, but was <%s>",
                        x, y, expectedStyle, actualStyle));
            }
            return this;
        }

        /**
         * Asserts that the cell has the expected foreground color.
         *
         * @param expectedFg the expected foreground color
         * @return this assertion object for chaining
         */
        public CellAssert hasForeground(Color expectedFg) {
            Color actualFg = cell.style().fg().orElse(null);
            if (expectedFg == null ? actualFg != null : !expectedFg.equals(actualFg)) {
                throw new AssertionError(String.format(
                        "Expected foreground at (%d, %d) to be <%s>, but was <%s>",
                        x, y, expectedFg, actualFg));
            }
            return this;
        }

        /**
         * Asserts that the cell has the expected background color.
         *
         * @param expectedBg the expected background color
         * @return this assertion object for chaining
         */
        public CellAssert hasBackground(Color expectedBg) {
            Color actualBg = cell.style().bg().orElse(null);
            if (expectedBg == null ? actualBg != null : !expectedBg.equals(actualBg)) {
                throw new AssertionError(String.format(
                        "Expected background at (%d, %d) to be <%s>, but was <%s>",
                        x, y, expectedBg, actualBg));
            }
            return this;
        }

        /**
         * Returns to the buffer assertion for further buffer-level assertions.
         *
         * @return the parent buffer assertion
         */
        public BufferAssert and() {
            return bufferAssert;
        }
    }

    private String formatBuffer(Buffer buffer) {
        return BufferDiffFormatter.formatBuffer(buffer);
    }
}


