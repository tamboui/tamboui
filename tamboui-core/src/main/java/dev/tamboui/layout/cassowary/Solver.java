/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.layout.cassowary;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cassowary constraint solver using the dual simplex method.
 *
 * <p>The solver maintains a tableau of linear equations and supports:
 * <ul>
 *   <li>Adding and removing constraints dynamically</li>
 *   <li>Suggesting values for edit variables</li>
 *   <li>Hierarchical constraint priorities</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>
 * Solver solver = new Solver();
 * Variable left = new Variable("left");
 * Variable width = new Variable("width");
 * Variable right = new Variable("right");
 *
 * // right == left + width (required)
 * solver.addConstraint(
 *     Expression.variable(right)
 *         .equalTo(Expression.variable(left).plus(Expression.variable(width)),
 *                  Strength.REQUIRED));
 *
 * // width >= 100 (required)
 * solver.addConstraint(
 *     Expression.variable(width)
 *         .greaterThanOrEqual(100, Strength.REQUIRED));
 *
 * solver.updateVariables();
 * double resolvedWidth = solver.valueOf(width);
 * </pre>
 *
 * @see Variable
 * @see CassowaryConstraint
 * @see Expression
 */
public final class Solver {

    private static final double EPSILON = 1e-8;

    /**
     * Tag associated with a constraint in the solver.
     * Contains marker and other symbols created for the constraint.
     */
    private static final class Tag {
        Symbol marker;
        Symbol other;
    }

    /**
     * Information about an edit variable.
     */
    private static final class EditInfo {
        Tag tag;
        CassowaryConstraint constraint;
        double constant;
    }

    // Maps constraints to their internal tags
    private final Map<CassowaryConstraint, Tag> constraints;
    // Maps external variables to their symbols
    private final Map<Variable, Symbol> vars;
    // Maps symbols to their containing rows (basic variables)
    private final Map<Symbol, Row> rows;
    // Maps external variables to their resolved values
    private final Map<Variable, Double> values;
    // Maps edit variables to their edit info
    private final Map<Variable, EditInfo> edits;
    // The objective row for optimization
    private Row objective;
    // Artificial objective for handling infeasibility
    private Row artificial;

    /**
     * Creates a new empty solver.
     */
    public Solver() {
        this.constraints = new LinkedHashMap<>();
        this.vars = new LinkedHashMap<>();
        this.rows = new LinkedHashMap<>();
        this.values = new LinkedHashMap<>();
        this.edits = new LinkedHashMap<>();
        this.objective = new Row();
        this.artificial = null;
    }

    /**
     * Adds a constraint to the solver.
     *
     * @param constraint the constraint to add
     * @throws DuplicateConstraintException     if the constraint already exists
     * @throws UnsatisfiableConstraintException if required and unsatisfiable
     */
    public void addConstraint(CassowaryConstraint constraint) {
        if (constraints.containsKey(constraint)) {
            throw new DuplicateConstraintException(constraint);
        }

        Tag tag = new Tag();
        Row row = createRow(constraint, tag);
        Symbol subject = chooseSubject(row, tag);

        if (subject == null && allDummies(row)) {
            if (!nearZero(row.constant())) {
                throw new UnsatisfiableConstraintException(constraint);
            }
            // The row is trivially satisfied
            subject = tag.marker;
        }

        if (subject == null) {
            if (!addWithArtificialVariable(row)) {
                throw new UnsatisfiableConstraintException(constraint);
            }
        } else {
            row.solveFor(subject);
            substitute(subject, row);
            rows.put(subject, row);
        }

        constraints.put(constraint, tag);
        optimize(objective);
    }

    /**
     * Removes a constraint from the solver.
     *
     * @param constraint the constraint to remove
     * @throws UnknownConstraintException if the constraint is not present
     */
    public void removeConstraint(CassowaryConstraint constraint) {
        Tag tag = constraints.remove(constraint);
        if (tag == null) {
            throw new UnknownConstraintException(constraint);
        }

        // Remove the error effects from the objective
        removeConstraintEffects(constraint, tag);

        // Try to remove the marker from the tableau
        Row row = rows.remove(tag.marker);
        if (row == null) {
            // The marker is not basic - find the row that contains it
            Symbol leaving = findLeavingSymbolForMarker(tag.marker);
            if (leaving == null) {
                throw new InternalSolverException(
                        "Failed to find leaving variable for marker");
            }
            row = rows.remove(leaving);
            row.solveFor(leaving, tag.marker);
            substitute(tag.marker, row);
        }

        optimize(objective);
    }

    /**
     * Checks if the solver contains a constraint.
     *
     * @param constraint the constraint to check
     * @return true if the constraint is in the solver
     */
    public boolean hasConstraint(CassowaryConstraint constraint) {
        return constraints.containsKey(constraint);
    }

    /**
     * Adds an edit variable for interactive updates.
     *
     * @param variable the variable to make editable
     * @param strength the strength of the edit constraint (must not be REQUIRED)
     * @throws IllegalArgumentException if strength is REQUIRED
     */
    public void addEditVariable(Variable variable, Strength strength) {
        if (edits.containsKey(variable)) {
            throw new SolverException("Edit variable already exists: " + variable);
        }
        if (strength.isRequired()) {
            throw new IllegalArgumentException(
                    "Edit variable strength cannot be REQUIRED");
        }

        // Create a stay constraint: variable == currentValue
        CassowaryConstraint constraint = Expression.variable(variable)
                .equalTo(0, strength);

        addConstraint(constraint);

        EditInfo info = new EditInfo();
        info.tag = constraints.get(constraint);
        info.constraint = constraint;
        info.constant = 0.0;
        edits.put(variable, info);
    }

    /**
     * Removes an edit variable.
     *
     * @param variable the variable to remove
     * @throws SolverException if the variable is not an edit variable
     */
    public void removeEditVariable(Variable variable) {
        EditInfo info = edits.remove(variable);
        if (info == null) {
            throw new SolverException("Unknown edit variable: " + variable);
        }
        removeConstraint(info.constraint);
    }

    /**
     * Checks if a variable is an edit variable.
     *
     * @param variable the variable to check
     * @return true if the variable is editable
     */
    public boolean hasEditVariable(Variable variable) {
        return edits.containsKey(variable);
    }

    /**
     * Suggests a new value for an edit variable.
     *
     * <p>Call {@link #updateVariables()} after suggesting values.
     *
     * @param variable the edit variable
     * @param value    the suggested value
     * @throws SolverException if the variable is not an edit variable
     */
    public void suggestValue(Variable variable, double value) {
        EditInfo info = edits.get(variable);
        if (info == null) {
            throw new SolverException("Unknown edit variable: " + variable);
        }

        double delta = value - info.constant;
        info.constant = value;

        // Try to update the marker row directly
        Row row = rows.get(info.tag.marker);
        if (row != null) {
            if (row.constant() - delta < 0) {
                row.setConstant(row.constant() - delta);
            } else {
                row.setConstant(row.constant() - delta);
            }
            return;
        }

        // Try to update the other row directly
        row = rows.get(info.tag.other);
        if (row != null) {
            if (row.constant() + delta < 0) {
                row.setConstant(row.constant() + delta);
            } else {
                row.setConstant(row.constant() + delta);
            }
            return;
        }

        // The symbol is not basic - update all rows that contain it
        for (Map.Entry<Symbol, Row> entry : rows.entrySet()) {
            double coeff = entry.getValue().coefficientFor(info.tag.marker);
            if (!nearZero(coeff)
                    && entry.getKey().type() != Symbol.Type.EXTERNAL) {
                entry.getValue().setConstant(
                        entry.getValue().constant() + delta * coeff);
            }
        }

        dualOptimize();
    }

    /**
     * Updates all variable values after constraint changes.
     *
     * <p>Must be called before reading variable values.
     */
    public void updateVariables() {
        for (Map.Entry<Variable, Symbol> entry : vars.entrySet()) {
            Variable variable = entry.getKey();
            Symbol symbol = entry.getValue();
            Row row = rows.get(symbol);
            if (row == null) {
                values.put(variable, 0.0);
            } else {
                values.put(variable, row.constant());
            }
        }
    }

    /**
     * Returns the current value of a variable.
     *
     * @param variable the variable to query
     * @return the computed value, or 0.0 if not in the system
     */
    public double valueOf(Variable variable) {
        Double value = values.get(variable);
        return value != null ? value : 0.0;
    }

    /**
     * Resets the solver to an empty state.
     */
    public void reset() {
        constraints.clear();
        vars.clear();
        rows.clear();
        values.clear();
        edits.clear();
        objective = new Row();
        artificial = null;
    }

    // --- Internal simplex operations ---

    /**
     * Creates a row from a constraint expression.
     */
    private Row createRow(CassowaryConstraint constraint, Tag tag) {
        Expression expr = constraint.expression();
        Row row = new Row(expr.constant());

        // Add terms, substituting basic variables
        for (Term term : expr.terms()) {
            if (!nearZero(term.coefficient())) {
                Symbol symbol = getVarSymbol(term.variable());
                Row basicRow = rows.get(symbol);
                if (basicRow != null) {
                    row.insertRow(basicRow, term.coefficient());
                } else {
                    row.insertSymbol(symbol, term.coefficient());
                }
            }
        }

        // Handle constraint type
        switch (constraint.relation()) {
            case LE:
            case GE: {
                double coeff = constraint.relation() == Relation.LE ? 1.0 : -1.0;
                Symbol slack = Symbol.slack();
                tag.marker = slack;
                row.insertSymbol(slack, coeff);
                if (!constraint.strength().isRequired()) {
                    Symbol error = Symbol.error();
                    tag.other = error;
                    row.insertSymbol(error, -coeff);
                    objective.insertSymbol(error, constraint.strength().computeValue());
                }
                break;
            }
            case EQ: {
                if (constraint.strength().isRequired()) {
                    Symbol dummy = Symbol.dummy();
                    tag.marker = dummy;
                    row.insertSymbol(dummy, 1.0);
                } else {
                    Symbol errplus = Symbol.error();
                    Symbol errminus = Symbol.error();
                    tag.marker = errplus;
                    tag.other = errminus;
                    row.insertSymbol(errplus, -1.0);
                    row.insertSymbol(errminus, 1.0);
                    double weight = constraint.strength().computeValue();
                    objective.insertSymbol(errplus, weight);
                    objective.insertSymbol(errminus, weight);
                }
                break;
            }
        }

        // Ensure the row constant is non-negative
        if (row.constant() < 0) {
            row.reverseSign();
        }

        return row;
    }

    /**
     * Chooses a subject symbol for a row.
     */
    private Symbol chooseSubject(Row row, Tag tag) {
        // First choice: an external variable
        for (Symbol symbol : row.cells().keySet()) {
            if (symbol.type() == Symbol.Type.EXTERNAL) {
                return symbol;
            }
        }

        // Second choice: a slack or error from the tag
        if (tag.marker != null
                && (tag.marker.type() == Symbol.Type.SLACK
                || tag.marker.type() == Symbol.Type.ERROR)) {
            if (row.coefficientFor(tag.marker) < 0) {
                return tag.marker;
            }
        }
        if (tag.other != null
                && (tag.other.type() == Symbol.Type.SLACK
                || tag.other.type() == Symbol.Type.ERROR)) {
            if (row.coefficientFor(tag.other) < 0) {
                return tag.other;
            }
        }

        return null;
    }

    /**
     * Checks if all symbols in a row are dummies.
     */
    private boolean allDummies(Row row) {
        for (Symbol symbol : row.cells().keySet()) {
            if (symbol.type() != Symbol.Type.DUMMY) {
                return false;
            }
        }
        return true;
    }

    /**
     * Adds a row using an artificial variable.
     *
     * @return true if the constraint can be satisfied
     */
    private boolean addWithArtificialVariable(Row row) {
        // Create the artificial variable and add it to the row
        Symbol art = Symbol.slack();
        rows.put(art, new Row(row));

        // Set up the artificial objective
        artificial = new Row(row);

        // Optimize the artificial objective
        optimize(artificial);

        // Check if the artificial objective was minimized to zero
        boolean success = nearZero(artificial.constant());
        artificial = null;

        // Remove the artificial variable from the tableau
        Row artRow = rows.remove(art);
        if (artRow != null) {
            // The artificial is basic - need to remove it
            if (artRow.cells().isEmpty()) {
                return success;
            }

            // Find a pivot to remove the artificial
            Symbol entering = null;
            for (Symbol symbol : artRow.cells().keySet()) {
                if (symbol.type() != Symbol.Type.DUMMY) {
                    entering = symbol;
                    break;
                }
            }

            if (entering == null) {
                return success;
            }

            artRow.solveFor(art, entering);
            substitute(entering, artRow);
            rows.put(entering, artRow);
        }

        // Remove the artificial from all rows
        for (Row r : rows.values()) {
            r.removeSymbol(art);
        }
        objective.removeSymbol(art);

        return success;
    }

    /**
     * Optimizes the objective function using the simplex method.
     */
    private void optimize(Row objective) {
        while (true) {
            Symbol entering = findEnteringSymbol(objective);
            if (entering == null) {
                return; // Optimal
            }
            Symbol leaving = findLeavingSymbol(entering);
            if (leaving == null) {
                throw new InternalSolverException("Objective function is unbounded");
            }
            pivot(entering, leaving);
        }
    }

    /**
     * Performs dual optimization.
     */
    private void dualOptimize() {
        while (true) {
            Symbol leaving = null;
            for (Map.Entry<Symbol, Row> entry : rows.entrySet()) {
                if (entry.getKey().type() != Symbol.Type.EXTERNAL
                        && entry.getValue().constant() < 0) {
                    leaving = entry.getKey();
                    break;
                }
            }
            if (leaving == null) {
                return;
            }

            Symbol entering = null;
            double minRatio = Double.MAX_VALUE;
            Row row = rows.get(leaving);
            for (Map.Entry<Symbol, Double> entry : row.cells().entrySet()) {
                if (entry.getValue() > 0 && entry.getKey().type() != Symbol.Type.DUMMY) {
                    double objCoeff = objective.coefficientFor(entry.getKey());
                    double ratio = objCoeff / entry.getValue();
                    if (ratio < minRatio) {
                        minRatio = ratio;
                        entering = entry.getKey();
                    }
                }
            }

            if (entering == null) {
                throw new InternalSolverException("Dual optimize failed");
            }
            pivot(entering, leaving);
        }
    }

    /**
     * Finds the entering symbol for the simplex method.
     */
    private Symbol findEnteringSymbol(Row objective) {
        for (Map.Entry<Symbol, Double> entry : objective.cells().entrySet()) {
            if (entry.getKey().type() != Symbol.Type.DUMMY && entry.getValue() < 0) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Finds the leaving symbol for a pivot operation.
     */
    private Symbol findLeavingSymbol(Symbol entering) {
        double minRatio = Double.MAX_VALUE;
        Symbol result = null;
        for (Map.Entry<Symbol, Row> entry : rows.entrySet()) {
            if (entry.getKey().type() != Symbol.Type.EXTERNAL) {
                double coeff = entry.getValue().coefficientFor(entering);
                if (coeff < 0) {
                    double ratio = -entry.getValue().constant() / coeff;
                    if (ratio < minRatio) {
                        minRatio = ratio;
                        result = entry.getKey();
                    }
                }
            }
        }
        return result;
    }

    /**
     * Finds a leaving symbol for removing a marker.
     */
    private Symbol findLeavingSymbolForMarker(Symbol marker) {
        double minRatio = Double.MAX_VALUE;
        Symbol first = null;
        Symbol second = null;
        Symbol third = null;

        for (Map.Entry<Symbol, Row> entry : rows.entrySet()) {
            double coeff = entry.getValue().coefficientFor(marker);
            if (nearZero(coeff)) {
                continue;
            }

            if (entry.getKey().type() == Symbol.Type.EXTERNAL) {
                third = entry.getKey();
            } else if (coeff < 0) {
                double ratio = -entry.getValue().constant() / coeff;
                if (ratio < minRatio) {
                    minRatio = ratio;
                    first = entry.getKey();
                }
            } else {
                second = entry.getKey();
            }
        }

        if (first != null) {
            return first;
        }
        if (second != null) {
            return second;
        }
        return third;
    }

    /**
     * Performs a pivot operation.
     */
    private void pivot(Symbol entering, Symbol leaving) {
        Row row = rows.remove(leaving);
        row.solveFor(leaving, entering);
        substitute(entering, row);
        rows.put(entering, row);
    }

    /**
     * Substitutes a symbol with the given row in all rows.
     */
    private void substitute(Symbol symbol, Row row) {
        for (Row r : rows.values()) {
            r.substitute(symbol, row);
        }
        objective.substitute(symbol, row);
        if (artificial != null) {
            artificial.substitute(symbol, row);
        }
    }

    /**
     * Gets or creates a symbol for a variable.
     */
    private Symbol getVarSymbol(Variable variable) {
        Symbol symbol = vars.get(variable);
        if (symbol == null) {
            symbol = Symbol.external();
            vars.put(variable, symbol);
        }
        return symbol;
    }

    /**
     * Removes the effects of a constraint's error variables from the objective.
     */
    private void removeConstraintEffects(CassowaryConstraint constraint, Tag tag) {
        if (tag.marker != null && tag.marker.type() == Symbol.Type.ERROR) {
            removeMarkerEffects(tag.marker, constraint.strength());
        }
        if (tag.other != null && tag.other.type() == Symbol.Type.ERROR) {
            removeMarkerEffects(tag.other, constraint.strength());
        }
    }

    /**
     * Removes the effects of a marker from the objective.
     */
    private void removeMarkerEffects(Symbol marker, Strength strength) {
        Row row = rows.get(marker);
        if (row != null) {
            objective.insertRow(row, -strength.computeValue());
        } else {
            objective.insertSymbol(marker, -strength.computeValue());
        }
    }

    private static boolean nearZero(double value) {
        return Math.abs(value) < EPSILON;
    }
}
