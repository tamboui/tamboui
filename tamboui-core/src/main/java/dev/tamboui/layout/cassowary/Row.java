/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.layout.cassowary;

import dev.tamboui.layout.Fraction;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A row in the simplex tableau.
 *
 * <p>Represents a linear equation of the form:
 * {@code constant + sum(coefficient * symbol) = 0}
 *
 * <p>Each row represents a basic variable expressed in terms of non-basic variables.
 *
 * <p>This implementation uses {@link Fraction} for exact arithmetic,
 * avoiding the cumulative rounding errors that occur with floating-point.
 */
final class Row {

    private Fraction constant;
    private final Map<Symbol, Fraction> cells;

    /**
     * Creates an empty row with constant 0.
     */
    Row() {
        this.constant = Fraction.ZERO;
        this.cells = new LinkedHashMap<>();
    }

    /**
     * Creates a row with the given constant.
     *
     * @param constant the constant value
     */
    Row(Fraction constant) {
        this.constant = constant;
        this.cells = new LinkedHashMap<>();
    }

    /**
     * Creates a copy of another row.
     *
     * @param other the row to copy
     */
    Row(Row other) {
        this.constant = other.constant;
        this.cells = new LinkedHashMap<>(other.cells);
    }

    /**
     * Returns the constant part of this row.
     *
     * @return the constant
     */
    Fraction constant() {
        return constant;
    }

    /**
     * Sets the constant part of this row.
     *
     * @param constant the new constant
     */
    void setConstant(Fraction constant) {
        this.constant = constant;
    }

    /**
     * Returns the cells (symbol-coefficient pairs) of this row.
     *
     * @return the cells map
     */
    Map<Symbol, Fraction> cells() {
        return cells;
    }

    /**
     * Returns the coefficient for the given symbol, or 0 if not present.
     *
     * @param symbol the symbol to look up
     * @return the coefficient
     */
    Fraction coefficientFor(Symbol symbol) {
        Fraction coeff = cells.get(symbol);
        return coeff != null ? coeff : Fraction.ZERO;
    }

    /**
     * Adds a symbol to this row with the given coefficient.
     *
     * <p>If the symbol already exists, the coefficients are added.
     * If the resulting coefficient is zero, the symbol is removed.
     *
     * @param symbol      the symbol to add
     * @param coefficient the coefficient to add
     */
    void insertSymbol(Symbol symbol, Fraction coefficient) {
        // Fast path: adding zero has no effect
        if (coefficient.isZero()) {
            return;
        }
        // Use compute() to perform lookup + update in a single hash operation
        cells.compute(symbol, (k, existing) -> {
            Fraction newCoeff = (existing != null ? existing : Fraction.ZERO).add(coefficient);
            return newCoeff.isZero() ? null : newCoeff;  // null removes the entry
        });
    }

    /**
     * Removes a symbol from this row.
     *
     * @param symbol the symbol to remove
     */
    void removeSymbol(Symbol symbol) {
        cells.remove(symbol);
    }

    /**
     * Inserts another row into this row, scaled by the given coefficient.
     *
     * <p>This is equivalent to: this += other * coefficient
     *
     * @param other       the row to insert
     * @param coefficient the scale factor
     */
    void insertRow(Row other, Fraction coefficient) {
        constant = constant.add(other.constant.multiply(coefficient));

        for (Map.Entry<Symbol, Fraction> entry : other.cells.entrySet()) {
            insertSymbol(entry.getKey(), entry.getValue().multiply(coefficient));
        }
    }

    /**
     * Reverses the sign of all coefficients and the constant.
     */
    void reverseSign() {
        constant = constant.negate();
        for (Map.Entry<Symbol, Fraction> entry : cells.entrySet()) {
            entry.setValue(entry.getValue().negate());
        }
    }

    /**
     * Solves the row for the given symbol.
     *
     * <p>Rearranges the row so that the symbol becomes the basic variable
     * (coefficient of -1), with all other terms on the right-hand side.
     *
     * <p>For example, if the row is: {@code 3 + 2*x + y = 0}
     * and we solve for x, the result is: {@code x = -1.5 - 0.5*y}
     *
     * @param symbol the symbol to solve for
     */
    void solveFor(Symbol symbol) {
        Fraction coeff = cells.remove(symbol);
        Fraction reciprocal = Fraction.NEG_ONE.divide(coeff);

        constant = constant.multiply(reciprocal);
        for (Map.Entry<Symbol, Fraction> entry : cells.entrySet()) {
            entry.setValue(entry.getValue().multiply(reciprocal));
        }
    }

    /**
     * Solves the row for the given symbol, substituting another symbol.
     *
     * <p>This performs a pivot operation: the lhs symbol is moved out
     * and the rhs symbol takes its place as a basic variable.
     *
     * @param lhs the leaving symbol
     * @param rhs the entering symbol
     */
    void solveFor(Symbol lhs, Symbol rhs) {
        insertSymbol(lhs, Fraction.NEG_ONE);
        solveFor(rhs);
    }

    /**
     * Substitutes a symbol with the given row.
     *
     * <p>Replaces all occurrences of the symbol with the expression
     * represented by the row.
     *
     * @param symbol the symbol to substitute
     * @param row    the row to substitute in place of the symbol
     */
    void substitute(Symbol symbol, Row row) {
        Fraction coeff = cells.remove(symbol);
        if (coeff != null) {
            insertRow(row, coeff);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(constant);
        for (Map.Entry<Symbol, Fraction> entry : cells.entrySet()) {
            Fraction coeff = entry.getValue();
            if (!coeff.isNegative()) {
                sb.append(" + ");
            } else {
                sb.append(" - ");
            }
            sb.append(coeff.abs()).append("*").append(entry.getKey());
        }
        return sb.toString();
    }
}
