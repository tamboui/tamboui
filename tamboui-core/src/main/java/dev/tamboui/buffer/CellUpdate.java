/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.buffer;

/// Represents a cell update to be sent to the terminal backend.
public final class CellUpdate {
    private final int x;
    private final int y;
    private final Cell cell;

    public CellUpdate(int x, int y, Cell cell) {
        this.x = x;
        this.y = y;
        this.cell = cell;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public Cell cell() {
        return cell;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CellUpdate)) {
            return false;
        }
        CellUpdate that = (CellUpdate) o;
        return x == that.x && y == that.y && cell.equals(that.cell);
    }

    @Override
    public int hashCode() {
        int result = Integer.hashCode(x);
        result = 31 * result + Integer.hashCode(y);
        result = 31 * result + cell.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return String.format("CellUpdate[x=%d, y=%d, cell=%s]", x, y, cell);
    }
}

