/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.symbols.merge;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry for mapping between Unicode box drawing characters and their BorderSymbol representations.
 */
final class SymbolRegistry {
    private static final Map<String, BorderSymbol> STRING_TO_SYMBOL = new HashMap<>();
    private static final Map<BorderSymbol, String> SYMBOL_TO_STRING = new HashMap<>();

    static {
        // Define all Unicode box drawing characters
        // Format: symbol => (right, up, left, down)
        register("─", LineStyle.PLAIN, LineStyle.NOTHING, LineStyle.PLAIN, LineStyle.NOTHING);
        register("━", LineStyle.THICK, LineStyle.NOTHING, LineStyle.THICK, LineStyle.NOTHING);
        register("│", LineStyle.NOTHING, LineStyle.PLAIN, LineStyle.NOTHING, LineStyle.PLAIN);
        register("┃", LineStyle.NOTHING, LineStyle.THICK, LineStyle.NOTHING, LineStyle.THICK);
        register("┄", LineStyle.TRIPLE_DASH, LineStyle.NOTHING, LineStyle.TRIPLE_DASH, LineStyle.NOTHING);
        register("┅", LineStyle.TRIPLE_DASH_THICK, LineStyle.NOTHING, LineStyle.TRIPLE_DASH_THICK, LineStyle.NOTHING);
        register("┆", LineStyle.NOTHING, LineStyle.TRIPLE_DASH, LineStyle.NOTHING, LineStyle.TRIPLE_DASH);
        register("┇", LineStyle.NOTHING, LineStyle.TRIPLE_DASH_THICK, LineStyle.NOTHING, LineStyle.TRIPLE_DASH_THICK);
        register("┈", LineStyle.QUADRUPLE_DASH, LineStyle.NOTHING, LineStyle.QUADRUPLE_DASH, LineStyle.NOTHING);
        register("┉", LineStyle.QUADRUPLE_DASH_THICK, LineStyle.NOTHING, LineStyle.QUADRUPLE_DASH_THICK, LineStyle.NOTHING);
        register("┊", LineStyle.NOTHING, LineStyle.QUADRUPLE_DASH, LineStyle.NOTHING, LineStyle.QUADRUPLE_DASH);
        register("┋", LineStyle.NOTHING, LineStyle.QUADRUPLE_DASH_THICK, LineStyle.NOTHING, LineStyle.QUADRUPLE_DASH_THICK);
        register("┌", LineStyle.PLAIN, LineStyle.NOTHING, LineStyle.NOTHING, LineStyle.PLAIN);
        register("┍", LineStyle.THICK, LineStyle.NOTHING, LineStyle.NOTHING, LineStyle.PLAIN);
        register("┎", LineStyle.PLAIN, LineStyle.NOTHING, LineStyle.NOTHING, LineStyle.THICK);
        register("┏", LineStyle.THICK, LineStyle.NOTHING, LineStyle.NOTHING, LineStyle.THICK);
        register("┐", LineStyle.NOTHING, LineStyle.NOTHING, LineStyle.PLAIN, LineStyle.PLAIN);
        register("┑", LineStyle.NOTHING, LineStyle.NOTHING, LineStyle.THICK, LineStyle.PLAIN);
        register("┒", LineStyle.NOTHING, LineStyle.NOTHING, LineStyle.PLAIN, LineStyle.THICK);
        register("┓", LineStyle.NOTHING, LineStyle.NOTHING, LineStyle.THICK, LineStyle.THICK);
        register("└", LineStyle.PLAIN, LineStyle.PLAIN, LineStyle.NOTHING, LineStyle.NOTHING);
        register("┕", LineStyle.THICK, LineStyle.PLAIN, LineStyle.NOTHING, LineStyle.NOTHING);
        register("┖", LineStyle.PLAIN, LineStyle.THICK, LineStyle.NOTHING, LineStyle.NOTHING);
        register("┗", LineStyle.THICK, LineStyle.THICK, LineStyle.NOTHING, LineStyle.NOTHING);
        register("┘", LineStyle.NOTHING, LineStyle.PLAIN, LineStyle.PLAIN, LineStyle.NOTHING);
        register("┙", LineStyle.NOTHING, LineStyle.PLAIN, LineStyle.THICK, LineStyle.NOTHING);
        register("┚", LineStyle.NOTHING, LineStyle.THICK, LineStyle.PLAIN, LineStyle.NOTHING);
        register("┛", LineStyle.NOTHING, LineStyle.THICK, LineStyle.THICK, LineStyle.NOTHING);
        register("├", LineStyle.PLAIN, LineStyle.PLAIN, LineStyle.NOTHING, LineStyle.PLAIN);
        register("┝", LineStyle.THICK, LineStyle.PLAIN, LineStyle.NOTHING, LineStyle.PLAIN);
        register("┞", LineStyle.PLAIN, LineStyle.THICK, LineStyle.NOTHING, LineStyle.PLAIN);
        register("┟", LineStyle.PLAIN, LineStyle.PLAIN, LineStyle.NOTHING, LineStyle.THICK);
        register("┠", LineStyle.PLAIN, LineStyle.THICK, LineStyle.NOTHING, LineStyle.THICK);
        register("┡", LineStyle.THICK, LineStyle.THICK, LineStyle.NOTHING, LineStyle.PLAIN);
        register("┢", LineStyle.THICK, LineStyle.PLAIN, LineStyle.NOTHING, LineStyle.THICK);
        register("┣", LineStyle.THICK, LineStyle.THICK, LineStyle.NOTHING, LineStyle.THICK);
        register("┤", LineStyle.NOTHING, LineStyle.PLAIN, LineStyle.PLAIN, LineStyle.PLAIN);
        register("┥", LineStyle.NOTHING, LineStyle.PLAIN, LineStyle.THICK, LineStyle.PLAIN);
        register("┦", LineStyle.NOTHING, LineStyle.THICK, LineStyle.PLAIN, LineStyle.PLAIN);
        register("┧", LineStyle.NOTHING, LineStyle.PLAIN, LineStyle.PLAIN, LineStyle.THICK);
        register("┨", LineStyle.NOTHING, LineStyle.THICK, LineStyle.PLAIN, LineStyle.THICK);
        register("┩", LineStyle.NOTHING, LineStyle.THICK, LineStyle.THICK, LineStyle.PLAIN);
        register("┪", LineStyle.NOTHING, LineStyle.PLAIN, LineStyle.THICK, LineStyle.THICK);
        register("┫", LineStyle.NOTHING, LineStyle.THICK, LineStyle.THICK, LineStyle.THICK);
        register("┬", LineStyle.PLAIN, LineStyle.NOTHING, LineStyle.PLAIN, LineStyle.PLAIN);
        register("┭", LineStyle.PLAIN, LineStyle.NOTHING, LineStyle.THICK, LineStyle.PLAIN);
        register("┮", LineStyle.THICK, LineStyle.NOTHING, LineStyle.PLAIN, LineStyle.PLAIN);
        register("┯", LineStyle.THICK, LineStyle.NOTHING, LineStyle.THICK, LineStyle.PLAIN);
        register("┰", LineStyle.PLAIN, LineStyle.NOTHING, LineStyle.PLAIN, LineStyle.THICK);
        register("┱", LineStyle.PLAIN, LineStyle.NOTHING, LineStyle.THICK, LineStyle.THICK);
        register("┲", LineStyle.THICK, LineStyle.NOTHING, LineStyle.PLAIN, LineStyle.THICK);
        register("┳", LineStyle.THICK, LineStyle.NOTHING, LineStyle.THICK, LineStyle.THICK);
        register("┴", LineStyle.PLAIN, LineStyle.PLAIN, LineStyle.PLAIN, LineStyle.NOTHING);
        register("┵", LineStyle.PLAIN, LineStyle.PLAIN, LineStyle.THICK, LineStyle.NOTHING);
        register("┶", LineStyle.THICK, LineStyle.PLAIN, LineStyle.PLAIN, LineStyle.NOTHING);
        register("┷", LineStyle.THICK, LineStyle.PLAIN, LineStyle.THICK, LineStyle.NOTHING);
        register("┸", LineStyle.PLAIN, LineStyle.THICK, LineStyle.PLAIN, LineStyle.NOTHING);
        register("┹", LineStyle.PLAIN, LineStyle.THICK, LineStyle.THICK, LineStyle.NOTHING);
        register("┺", LineStyle.THICK, LineStyle.THICK, LineStyle.PLAIN, LineStyle.NOTHING);
        register("┻", LineStyle.THICK, LineStyle.THICK, LineStyle.THICK, LineStyle.NOTHING);
        register("┼", LineStyle.PLAIN, LineStyle.PLAIN, LineStyle.PLAIN, LineStyle.PLAIN);
        register("┽", LineStyle.PLAIN, LineStyle.PLAIN, LineStyle.THICK, LineStyle.PLAIN);
        register("┾", LineStyle.THICK, LineStyle.PLAIN, LineStyle.PLAIN, LineStyle.PLAIN);
        register("┿", LineStyle.THICK, LineStyle.PLAIN, LineStyle.THICK, LineStyle.PLAIN);
        register("╀", LineStyle.PLAIN, LineStyle.THICK, LineStyle.PLAIN, LineStyle.PLAIN);
        register("╁", LineStyle.PLAIN, LineStyle.PLAIN, LineStyle.PLAIN, LineStyle.THICK);
        register("╂", LineStyle.PLAIN, LineStyle.THICK, LineStyle.PLAIN, LineStyle.THICK);
        register("╃", LineStyle.PLAIN, LineStyle.THICK, LineStyle.THICK, LineStyle.PLAIN);
        register("╄", LineStyle.THICK, LineStyle.THICK, LineStyle.PLAIN, LineStyle.PLAIN);
        register("╅", LineStyle.PLAIN, LineStyle.PLAIN, LineStyle.THICK, LineStyle.THICK);
        register("╆", LineStyle.THICK, LineStyle.PLAIN, LineStyle.PLAIN, LineStyle.THICK);
        register("╇", LineStyle.THICK, LineStyle.THICK, LineStyle.THICK, LineStyle.PLAIN);
        register("╈", LineStyle.THICK, LineStyle.PLAIN, LineStyle.THICK, LineStyle.THICK);
        register("╉", LineStyle.PLAIN, LineStyle.THICK, LineStyle.THICK, LineStyle.THICK);
        register("╊", LineStyle.THICK, LineStyle.THICK, LineStyle.PLAIN, LineStyle.THICK);
        register("╋", LineStyle.THICK, LineStyle.THICK, LineStyle.THICK, LineStyle.THICK);
        register("╌", LineStyle.DOUBLE_DASH, LineStyle.NOTHING, LineStyle.DOUBLE_DASH, LineStyle.NOTHING);
        register("╍", LineStyle.DOUBLE_DASH_THICK, LineStyle.NOTHING, LineStyle.DOUBLE_DASH_THICK, LineStyle.NOTHING);
        register("╎", LineStyle.NOTHING, LineStyle.DOUBLE_DASH, LineStyle.NOTHING, LineStyle.DOUBLE_DASH);
        register("╏", LineStyle.NOTHING, LineStyle.DOUBLE_DASH_THICK, LineStyle.NOTHING, LineStyle.DOUBLE_DASH_THICK);
        register("═", LineStyle.DOUBLE, LineStyle.NOTHING, LineStyle.DOUBLE, LineStyle.NOTHING);
        register("║", LineStyle.NOTHING, LineStyle.DOUBLE, LineStyle.NOTHING, LineStyle.DOUBLE);
        register("╒", LineStyle.DOUBLE, LineStyle.NOTHING, LineStyle.NOTHING, LineStyle.PLAIN);
        register("╓", LineStyle.PLAIN, LineStyle.NOTHING, LineStyle.NOTHING, LineStyle.DOUBLE);
        register("╔", LineStyle.DOUBLE, LineStyle.NOTHING, LineStyle.NOTHING, LineStyle.DOUBLE);
        register("╕", LineStyle.NOTHING, LineStyle.NOTHING, LineStyle.DOUBLE, LineStyle.PLAIN);
        register("╖", LineStyle.NOTHING, LineStyle.NOTHING, LineStyle.PLAIN, LineStyle.DOUBLE);
        register("╗", LineStyle.NOTHING, LineStyle.NOTHING, LineStyle.DOUBLE, LineStyle.DOUBLE);
        register("╘", LineStyle.DOUBLE, LineStyle.PLAIN, LineStyle.NOTHING, LineStyle.NOTHING);
        register("╙", LineStyle.PLAIN, LineStyle.DOUBLE, LineStyle.NOTHING, LineStyle.NOTHING);
        register("╚", LineStyle.DOUBLE, LineStyle.DOUBLE, LineStyle.NOTHING, LineStyle.NOTHING);
        register("╛", LineStyle.NOTHING, LineStyle.PLAIN, LineStyle.DOUBLE, LineStyle.NOTHING);
        register("╜", LineStyle.NOTHING, LineStyle.DOUBLE, LineStyle.PLAIN, LineStyle.NOTHING);
        register("╝", LineStyle.NOTHING, LineStyle.DOUBLE, LineStyle.DOUBLE, LineStyle.NOTHING);
        register("╞", LineStyle.DOUBLE, LineStyle.PLAIN, LineStyle.NOTHING, LineStyle.PLAIN);
        register("╟", LineStyle.PLAIN, LineStyle.DOUBLE, LineStyle.NOTHING, LineStyle.DOUBLE);
        register("╠", LineStyle.DOUBLE, LineStyle.DOUBLE, LineStyle.NOTHING, LineStyle.DOUBLE);
        register("╡", LineStyle.NOTHING, LineStyle.PLAIN, LineStyle.DOUBLE, LineStyle.PLAIN);
        register("╢", LineStyle.NOTHING, LineStyle.DOUBLE, LineStyle.PLAIN, LineStyle.DOUBLE);
        register("╣", LineStyle.NOTHING, LineStyle.DOUBLE, LineStyle.DOUBLE, LineStyle.DOUBLE);
        register("╤", LineStyle.DOUBLE, LineStyle.NOTHING, LineStyle.DOUBLE, LineStyle.PLAIN);
        register("╥", LineStyle.PLAIN, LineStyle.NOTHING, LineStyle.PLAIN, LineStyle.DOUBLE);
        register("╦", LineStyle.DOUBLE, LineStyle.NOTHING, LineStyle.DOUBLE, LineStyle.DOUBLE);
        register("╧", LineStyle.DOUBLE, LineStyle.PLAIN, LineStyle.DOUBLE, LineStyle.NOTHING);
        register("╨", LineStyle.PLAIN, LineStyle.DOUBLE, LineStyle.PLAIN, LineStyle.NOTHING);
        register("╩", LineStyle.DOUBLE, LineStyle.DOUBLE, LineStyle.DOUBLE, LineStyle.NOTHING);
        register("╪", LineStyle.DOUBLE, LineStyle.PLAIN, LineStyle.DOUBLE, LineStyle.PLAIN);
        register("╫", LineStyle.PLAIN, LineStyle.DOUBLE, LineStyle.PLAIN, LineStyle.DOUBLE);
        register("╬", LineStyle.DOUBLE, LineStyle.DOUBLE, LineStyle.DOUBLE, LineStyle.DOUBLE);
        register("╭", LineStyle.ROUNDED, LineStyle.NOTHING, LineStyle.NOTHING, LineStyle.ROUNDED);
        register("╮", LineStyle.NOTHING, LineStyle.NOTHING, LineStyle.ROUNDED, LineStyle.ROUNDED);
        register("╯", LineStyle.NOTHING, LineStyle.ROUNDED, LineStyle.ROUNDED, LineStyle.NOTHING);
        register("╰", LineStyle.ROUNDED, LineStyle.ROUNDED, LineStyle.NOTHING, LineStyle.NOTHING);
        register("╴", LineStyle.NOTHING, LineStyle.NOTHING, LineStyle.PLAIN, LineStyle.NOTHING);
        register("╵", LineStyle.NOTHING, LineStyle.PLAIN, LineStyle.NOTHING, LineStyle.NOTHING);
        register("╶", LineStyle.PLAIN, LineStyle.NOTHING, LineStyle.NOTHING, LineStyle.NOTHING);
        register("╷", LineStyle.NOTHING, LineStyle.NOTHING, LineStyle.NOTHING, LineStyle.PLAIN);
        register("╸", LineStyle.NOTHING, LineStyle.NOTHING, LineStyle.THICK, LineStyle.NOTHING);
        register("╹", LineStyle.NOTHING, LineStyle.THICK, LineStyle.NOTHING, LineStyle.NOTHING);
        register("╺", LineStyle.THICK, LineStyle.NOTHING, LineStyle.NOTHING, LineStyle.NOTHING);
        register("╻", LineStyle.NOTHING, LineStyle.NOTHING, LineStyle.NOTHING, LineStyle.THICK);
        register("╼", LineStyle.THICK, LineStyle.NOTHING, LineStyle.PLAIN, LineStyle.NOTHING);
        register("╽", LineStyle.NOTHING, LineStyle.PLAIN, LineStyle.NOTHING, LineStyle.THICK);
        register("╾", LineStyle.PLAIN, LineStyle.NOTHING, LineStyle.THICK, LineStyle.NOTHING);
        register("╿", LineStyle.NOTHING, LineStyle.THICK, LineStyle.NOTHING, LineStyle.PLAIN);
    }

    private static void register(String symbol, LineStyle right, LineStyle up, LineStyle left, LineStyle down) {
        BorderSymbol borderSymbol = BorderSymbol.of(right, up, left, down);
        STRING_TO_SYMBOL.put(symbol, borderSymbol);
        SYMBOL_TO_STRING.put(borderSymbol, symbol);
    }

    static BorderSymbol fromString(String symbol) {
        return STRING_TO_SYMBOL.get(symbol);
    }

    static String toString(BorderSymbol symbol) {
        String result = SYMBOL_TO_STRING.get(symbol);
        return result != null ? result : " ";
    }

    static boolean canConvert(BorderSymbol symbol) {
        return SYMBOL_TO_STRING.containsKey(symbol);
    }
}
