/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.symbols.merge;

/**
 * A strategy for merging two border symbols into one.
 * <p>
 * This enum defines how two symbols should be merged together, allowing for different behaviors
 * when combining symbols, such as replacing the previous symbol, merging them if an exact match
 * exists, or using a fuzzy match to find the closest representation.
 * <p>
 * This is useful for collapsing borders in layouts, where multiple symbols may need to be
 * combined to create a single, coherent border representation.
 */
public enum MergeStrategy {
    /**
     * Replaces the previous symbol with the next one.
     * <p>
     * This strategy simply replaces the previous symbol with the next one, without attempting to
     * merge them. This is useful when you want to ensure that the last rendered symbol takes
     * precedence over the previous one, regardless of their compatibility.
     */
    REPLACE,

    /**
     * Merges symbols only if an exact composite unicode character exists.
     * <p>
     * This strategy attempts to merge two symbols into a single composite unicode character if the
     * exact representation exists. If the required unicode symbol does not exist, it falls back to
     * {@link #REPLACE}, replacing the previous symbol with the next one.
     */
    EXACT,

    /**
     * Merges symbols even if an exact composite unicode character doesn't exist, using the closest
     * match.
     * <p>
     * If required unicode symbol exists, acts exactly like {@link #EXACT}, if not, applies fuzzy
     * matching rules to find the closest representation.
     */
    FUZZY;

    /**
     * Merges two symbols using this merge strategy.
     *
     * @param prev the previous symbol
     * @param next the next symbol
     * @return the merged symbol
     */
    public String merge(String prev, String next) {
        return BorderSymbolMerger.merge(prev, next, this);
    }

    /**
     * Checks if a symbol is a border symbol that can be merged.
     *
     * @param symbol the symbol to check
     * @return true if the symbol is a border symbol that can be merged, false otherwise
     */
    public static boolean isBorderSymbol(String symbol) {
        if (symbol == null || symbol.isEmpty()) {
            return false;
        }
        return SymbolRegistry.fromString(symbol) != null;
    }
}

