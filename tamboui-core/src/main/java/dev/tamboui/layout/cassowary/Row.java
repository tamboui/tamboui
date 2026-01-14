/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.layout.cassowary;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A row in the simplex tableau.
 *
 * <p>Represents a linear equation of the form:
 * {@code constant + sum(coefficient * symbol) = 0}
 *
 * <p>Each row represents a basic variable expressed in terms of non-basic variables.
 */
final class Row {

    private static final double EPSILON = 1e-8;

    private double constant;
    private final Map<Symbol, Double> cells;

    /**
     * Creates an empty row with constant 0.
     */
    Row() {
        this.constant = 0.0;
        this.cells = new LinkedHashMap<>();
    }

    /**
     * Creates a row with the given constant.
     *
     * @param constant the constant value
     */
    Row(double constant) {
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
    double constant() {
        return constant;
    }

    /**
     * Sets the constant part of this row.
     *
     * @param constant the new constant
     */
    void setConstant(double constant) {
        this.constant = constant;
    }

    /**
     * Returns the cells (symbol-coefficient pairs) of this row.
     *
     * @return the cells map
     */
    Map<Symbol, Double> cells() {
        return cells;
    }

    /**
     * Returns the coefficient for the given symbol, or 0 if not present.
     *
     * @param symbol the symbol to look up
     * @return the coefficient
     */
    double coefficientFor(Symbol symbol) {
        Double coeff = cells.get(symbol);
        return coeff != null ? coeff : 0.0;
    }

    /**
     * Adds a symbol to this row with the given coefficient.
     *
     * <p>If the symbol already exists, the coefficients are added.
     * If the resulting coefficient is near zero, the symbol is removed.
     *
     * @param symbol      the symbol to add
     * @param coefficient the coefficient to add
     */
    void insertSymbol(Symbol symbol, double coefficient) {
        Double existing = cells.get(symbol);
        double newCoeff = (existing != null ? existing : 0.0) + coefficient;

        if (nearZero(newCoeff)) {
            cells.remove(symbol);
        } else {
            cells.put(symbol, newCoeff);
        }
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
    void insertRow(Row other, double coefficient) {
        constant += other.constant * coefficient;

        for (Map.Entry<Symbol, Double> entry : other.cells.entrySet()) {
            insertSymbol(entry.getKey(), entry.getValue() * coefficient);
        }
    }

    /**
     * Reverses the sign of all coefficients and the constant.
     */
    void reverseSign() {
        constant = -constant;
        for (Map.Entry<Symbol, Double> entry : cells.entrySet()) {
            entry.setValue(-entry.getValue());
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
        double coeff = cells.remove(symbol);
        double reciprocal = -1.0 / coeff;

        constant *= reciprocal;
        for (Map.Entry<Symbol, Double> entry : cells.entrySet()) {
            entry.setValue(entry.getValue() * reciprocal);
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
        insertSymbol(lhs, -1.0);
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
        Double coeff = cells.remove(symbol);
        if (coeff != null) {
            insertRow(row, coeff);
        }
    }

    private static boolean nearZero(double value) {
        return Math.abs(value) < EPSILON;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(constant);
        for (Map.Entry<Symbol, Double> entry : cells.entrySet()) {
            double coeff = entry.getValue();
            if (coeff >= 0) {
                sb.append(" + ");
            } else {
                sb.append(" - ");
            }
            sb.append(Math.abs(coeff)).append("*").append(entry.getKey());
        }
        return sb.toString();
    }
}
