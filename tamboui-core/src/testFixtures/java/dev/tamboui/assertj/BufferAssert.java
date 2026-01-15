/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.assertj;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;
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

    private String formatBuffer(Buffer buffer) {
        return BufferDiffFormatter.formatBuffer(buffer);
    }
}


