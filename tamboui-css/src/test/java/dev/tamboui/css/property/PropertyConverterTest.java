/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.property;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PropertyConverterTest {

    @Test
    void resolveVariables_simpleReplacement() {
        Map<String, String> variables = new HashMap<>();
        variables.put("color", "red");
        String result = PropertyConverter.resolveVariables("$color", variables);
        assertThat(result).isEqualTo("red");
    }

    @Test
    void resolveVariables_hyphenatedVariableName() {
        // Variable names can contain hyphens (like CSS custom properties)
        Map<String, String> variables = new HashMap<>();
        variables.put("primary-color", "blue");
        String result = PropertyConverter.resolveVariables("color: $primary-color;", variables);
        assertThat(result).isEqualTo("color: blue;");
    }

    @Test
    void resolveVariables_multipleVariables() {
        Map<String, String> variables = new HashMap<>();
        variables.put("fg", "black");
        variables.put("bg", "white");
        String result = PropertyConverter.resolveVariables("fg=$fg bg=$bg", variables);
        assertThat(result).isEqualTo("fg=black bg=white");
    }

    @Test
    void resolveVariables_noVariables() {
        Map<String, String> variables = new HashMap<>();
        variables.put("color", "red");
        String result = PropertyConverter.resolveVariables("plain text", variables);
        assertThat(result).isEqualTo("plain text");
    }

    /**
     * This test demonstrates deterministic behavior when variable values
     * contain references to other variables.
     * <p>
     * Given:
     * - variables: {accent: "bold $primary", primary: "blue"}
     * - input: "style: $accent; color: $primary;"
     * <p>
     * The EXPECTED deterministic result should be: "style: bold $primary; color: blue;"
     * (each $var in the INPUT is replaced with its value, but $primary inside accent's value
     * should NOT be expanded - we're only resolving variables in the original input)
     * <p>
     * The old implementation iterated over the map and did sequential replacements,
     * which would produce different results depending on HashMap iteration order.
     * The new implementation extracts variable patterns from the input first,
     * ensuring deterministic behavior.
     */
    @Test
    void resolveVariables_shouldBeDeterministicWhenVariableValuesContainOtherVariables() {
        // Use LinkedHashMap to control iteration order
        Map<String, String> variablesOrderAP = new LinkedHashMap<>();
        variablesOrderAP.put("accent", "bold $primary");
        variablesOrderAP.put("primary", "blue");

        Map<String, String> variablesOrderPA = new LinkedHashMap<>();
        variablesOrderPA.put("primary", "blue");
        variablesOrderPA.put("accent", "bold $primary");

        String input = "style: $accent; color: $primary;";

        String resultAP = PropertyConverter.resolveVariables(input, variablesOrderAP);
        String resultPA = PropertyConverter.resolveVariables(input, variablesOrderPA);

        // The expected deterministic result: replace $accent and $primary in the input
        // with their literal values, without recursively expanding variables inside values
        String expected = "style: bold $primary; color: blue;";

        // Both results should be the same regardless of iteration order
        assertThat(resultAP)
                .as("Result should be the same regardless of variable iteration order")
                .isEqualTo(resultPA);

        // Both should equal the expected deterministic result
        assertThat(resultAP).isEqualTo(expected);
        assertThat(resultPA).isEqualTo(expected);
    }

    /**
     * Additional test showing the issue with a more realistic CSS scenario.
     */
    @Test
    void resolveVariables_cssVariableReferencingAnotherVariable() {
        // Simulate a theme where accent-color is derived from primary-color
        Map<String, String> variablesOrder1 = new LinkedHashMap<>();
        variablesOrder1.put("primary-color", "blue");
        variablesOrder1.put("accent-color", "$primary-color");

        Map<String, String> variablesOrder2 = new LinkedHashMap<>();
        variablesOrder2.put("accent-color", "$primary-color");
        variablesOrder2.put("primary-color", "blue");

        String input = "color: $accent-color;";

        String result1 = PropertyConverter.resolveVariables(input, variablesOrder1);
        String result2 = PropertyConverter.resolveVariables(input, variablesOrder2);

        // Both should produce the same result
        assertThat(result1)
                .as("Variable resolution should be deterministic")
                .isEqualTo(result2);
    }
}
