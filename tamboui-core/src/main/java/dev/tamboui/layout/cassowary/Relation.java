/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.layout.cassowary;

/// The type of relationship in a constraint.
///
///
///
/// Cassowary supports three types of linear constraints:
///
/// - Equality: expression == 0
/// - Less than or equal: expression &lt;= 0
/// - Greater than or equal: expression &gt;= 0
public enum Relation {

    /// Equality constraint: the expression must equal zero.
    EQ("=="),

    /// Less than or equal constraint: the expression must be &lt;= 0.
    LE("<="),

    /// Greater than or equal constraint: the expression must be &gt;= 0.
    GE(">=");

    private final String symbol;

    Relation(String symbol) {
        this.symbol = symbol;
    }

    /// Returns the symbol representation of this relation.
    ///
    /// @return the symbol (==, &lt;=, or &gt;=)
    public String symbol() {
        return symbol;
    }

    @Override
    public String toString() {
        return symbol;
    }
}

