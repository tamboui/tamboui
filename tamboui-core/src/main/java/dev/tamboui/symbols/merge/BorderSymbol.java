/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.symbols.merge;

/**
 * Represents a composite border symbol using individual line components.
 * <p>
 * This is an internal type specifically used to make the merge logic easier to implement.
 */
final class BorderSymbol {
    private final LineStyle right;
    private final LineStyle up;
    private final LineStyle left;
    private final LineStyle down;

    private BorderSymbol(LineStyle right, LineStyle up, LineStyle left, LineStyle down) {
        this.right = right;
        this.up = up;
        this.left = left;
        this.down = down;
    }

    static BorderSymbol of(LineStyle right, LineStyle up, LineStyle left, LineStyle down) {
        return new BorderSymbol(right, up, left, down);
    }

    LineStyle right() {
        return right;
    }

    LineStyle up() {
        return up;
    }

    LineStyle left() {
        return left;
    }

    LineStyle down() {
        return down;
    }

    /**
     * Finds the closest representation of this BorderSymbol that has a corresponding unicode character.
     */
    BorderSymbol fuzzy(BorderSymbol other) {
        BorderSymbol result = this;

        // Dashes only include vertical and horizontal lines.
        if (!result.isStraight()) {
            result = result
                .replace(LineStyle.DOUBLE_DASH, LineStyle.PLAIN)
                .replace(LineStyle.TRIPLE_DASH, LineStyle.PLAIN)
                .replace(LineStyle.QUADRUPLE_DASH, LineStyle.PLAIN)
                .replace(LineStyle.DOUBLE_DASH_THICK, LineStyle.THICK)
                .replace(LineStyle.TRIPLE_DASH_THICK, LineStyle.THICK)
                .replace(LineStyle.QUADRUPLE_DASH_THICK, LineStyle.THICK);
        }

        // Rounded has only corner variants.
        if (!result.isCorner()) {
            result = result.replace(LineStyle.ROUNDED, LineStyle.PLAIN);
        }

        // There are no Double + Thick variants.
        if (result.contains(LineStyle.DOUBLE) && result.contains(LineStyle.THICK)) {
            // Decide whether to use Double or Thick, based on the last merged-in symbol.
            if (other.contains(LineStyle.DOUBLE)) {
                result = result.replace(LineStyle.THICK, LineStyle.DOUBLE);
            } else {
                result = result.replace(LineStyle.DOUBLE, LineStyle.THICK);
            }
        }

        // Some Plain + Double variants don't exist.
        if (!SymbolRegistry.canConvert(result)) {
            // Decide whether to use Double or Plain, based on the last merged-in symbol.
            if (other.contains(LineStyle.DOUBLE)) {
                result = result.replace(LineStyle.PLAIN, LineStyle.DOUBLE);
            } else {
                result = result.replace(LineStyle.DOUBLE, LineStyle.PLAIN);
            }
        }

        return result;
    }

    /**
     * Returns true only if the symbol is a line and both parts have the same LineStyle.
     */
    boolean isStraight() {
        return (up == down && left == right)
            && (up == LineStyle.NOTHING || left == LineStyle.NOTHING);
    }

    /**
     * Returns true only if the symbol is a corner and both parts have the same LineStyle.
     */
    boolean isCorner() {
        if (up != LineStyle.NOTHING && right != LineStyle.NOTHING && down == LineStyle.NOTHING && left == LineStyle.NOTHING) {
            return up == right;
        }
        if (up == LineStyle.NOTHING && right != LineStyle.NOTHING && down != LineStyle.NOTHING && left == LineStyle.NOTHING) {
            return right == down;
        }
        if (up == LineStyle.NOTHING && right == LineStyle.NOTHING && down != LineStyle.NOTHING && left != LineStyle.NOTHING) {
            return down == left;
        }
        if (up != LineStyle.NOTHING && right == LineStyle.NOTHING && down == LineStyle.NOTHING && left != LineStyle.NOTHING) {
            return up == left;
        }
        return false;
    }

    /**
     * Checks if any of the line components matches the given style.
     */
    boolean contains(LineStyle style) {
        return up == style || right == style || down == style || left == style;
    }

    /**
     * Replaces all line styles matching `from` by `to`.
     */
    BorderSymbol replace(LineStyle from, LineStyle to) {
        return new BorderSymbol(
            right == from ? to : right,
            up == from ? to : up,
            left == from ? to : left,
            down == from ? to : down
        );
    }

    /**
     * Merges two border symbols into one.
     */
    BorderSymbol merge(BorderSymbol other, MergeStrategy strategy) {
        BorderSymbol exactResult = BorderSymbol.of(
            right.merge(other.right),
            up.merge(other.up),
            left.merge(other.left),
            down.merge(other.down)
        );

        switch (strategy) {
            case REPLACE:
                return other;
            case FUZZY:
                return exactResult.fuzzy(other);
            case EXACT:
            default:
                return exactResult;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BorderSymbol)) {
            return false;
        }
        BorderSymbol that = (BorderSymbol) o;
        return right == that.right && up == that.up && left == that.left && down == that.down;
    }

    @Override
    public int hashCode() {
        int result = right.hashCode();
        result = 31 * result + up.hashCode();
        result = 31 * result + left.hashCode();
        result = 31 * result + down.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return SymbolRegistry.toString(this);
    }
}

