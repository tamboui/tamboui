/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.model;

import java.util.*;

/**
 * Represents a parsed CSS stylesheet.
 * <p>
 * Contains CSS variables and rules that can be applied to elements.
 */
public final class Stylesheet {

    private final Map<String, String> variables;
    private final List<Rule> rules;

    /**
     * Creates a new stylesheet.
     *
     * @param variables the CSS variables ($name: value)
     * @param rules     the CSS rules
     */
    public Stylesheet(Map<String, String> variables, List<Rule> rules) {
        this.variables = Collections.unmodifiableMap(new LinkedHashMap<>(variables));
        this.rules = Collections.unmodifiableList(new ArrayList<>(rules));
    }

    /**
     * Creates an empty stylesheet.
     *
     * @return an empty stylesheet
     */
    public static Stylesheet empty() {
        return new Stylesheet(Collections.emptyMap(), Collections.emptyList());
    }

    /**
     * Returns the CSS variables defined in this stylesheet.
     *
     * @return an unmodifiable map of variable names to values
     */
    public Map<String, String> variables() {
        return variables;
    }

    /**
     * Returns the CSS rules defined in this stylesheet.
     *
     * @return an unmodifiable list of rules
     */
    public List<Rule> rules() {
        return rules;
    }

    /**
     * Resolves a variable reference.
     *
     * @param name the variable name (without $)
     * @return the variable value, or empty if not defined
     */
    public Optional<String> resolveVariable(String name) {
        return Optional.ofNullable(variables.get(name));
    }

    /**
     * Returns a new stylesheet with the given rules appended.
     *
     * @param additionalRules the rules to add
     * @return a new stylesheet with all rules
     */
    public Stylesheet withRules(List<Rule> additionalRules) {
        List<Rule> allRules = new ArrayList<>(rules);
        allRules.addAll(additionalRules);
        return new Stylesheet(variables, allRules);
    }

    /**
     * Returns a new stylesheet with the given variables merged.
     *
     * @param additionalVariables the variables to add (overwrite existing)
     * @return a new stylesheet with merged variables
     */
    public Stylesheet withVariables(Map<String, String> additionalVariables) {
        Map<String, String> allVars = new LinkedHashMap<>(variables);
        allVars.putAll(additionalVariables);
        return new Stylesheet(allVars, rules);
    }

    /**
     * Merges another stylesheet into this one.
     * <p>
     * Variables from the other stylesheet override this one's variables.
     * Rules from the other stylesheet are appended after this one's rules.
     *
     * @param other the stylesheet to merge
     * @return a new merged stylesheet
     */
    public Stylesheet merge(Stylesheet other) {
        return withVariables(other.variables).withRules(other.rules);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Stylesheet)) {
            return false;
        }
        Stylesheet that = (Stylesheet) o;
        return variables.equals(that.variables) && rules.equals(that.rules);
    }

    @Override
    public int hashCode() {
        return Objects.hash(variables, rules);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            sb.append("$").append(entry.getKey()).append(": ").append(entry.getValue()).append(";\n");
        }
        if (!variables.isEmpty()) {
            sb.append("\n");
        }
        for (Rule rule : rules) {
            sb.append(rule).append("\n\n");
        }
        return sb.toString();
    }
}
