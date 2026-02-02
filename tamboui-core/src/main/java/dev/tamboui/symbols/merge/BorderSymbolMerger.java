/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.symbols.merge;

/**
 * Helper class for merging border symbols according to merge strategies.
 * <p>
 * This implementation matches Ratatui's Rust implementation, using BorderSymbol
 * decomposition to handle all Unicode box drawing characters and their combinations.
 */
final class BorderSymbolMerger {

    private BorderSymbolMerger() {
        // Utility class
    }

    /**
     * Merges two border symbols according to the given strategy.
     * <p>
     * This method matches Ratatui's implementation:
     * - If either symbol is not a border symbol, handles them according to Ratatui's rules
     * - If both are border symbols, parses them into BorderSymbol components and merges them
     * - Returns the merged result as a string
     */
    static String merge(String prev, String next, MergeStrategy strategy) {
        // Replace should always just return the last symbol
        if (strategy == MergeStrategy.REPLACE) {
            return next;
        }

        BorderSymbol prevSymbol = SymbolRegistry.fromString(prev);
        BorderSymbol nextSymbol = SymbolRegistry.fromString(next);

        // Handle non-border symbols according to Ratatui's logic
        if (prevSymbol == null && nextSymbol != null) {
            // If prev is not a border but next is, use next (draw the border)
            return next;
        }
        if (prevSymbol != null && nextSymbol == null) {
            // If prev is a border but next is not, keep prev (don't erase border with non-border)
            return prev;
        }
        if (prevSymbol == null && nextSymbol == null) {
            // Both are non-borders: use next (REPLACE behavior)
            return next;
        }

        // Both are border symbols - merge them
        BorderSymbol merged = prevSymbol.merge(nextSymbol, strategy);
        String result = SymbolRegistry.toString(merged);
        
        // If the merged symbol can't be represented, fall back to next
        if (" ".equals(result) && strategy == MergeStrategy.EXACT) {
            return next;
        }
        
        return result;
    }
}
