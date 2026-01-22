/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.property;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class IntegerConverterTest {

    private final IntegerConverter converter = new IntegerConverter();

    @Test
    void convertsPositiveInteger() {
        assertThat(converter.convert("5", Collections.emptyMap()))
            .hasValue(5);
    }

    @Test
    void convertsZero() {
        assertThat(converter.convert("0", Collections.emptyMap()))
            .hasValue(0);
    }

    @Test
    void convertsNegativeInteger() {
        assertThat(converter.convert("-3", Collections.emptyMap()))
            .hasValue(-3);
    }

    @Test
    void handlesWhitespace() {
        assertThat(converter.convert("  10  ", Collections.emptyMap()))
            .hasValue(10);
    }

    @Test
    void resolvesVariableReference() {
        Map<String, String> variables = new HashMap<>();
        variables.put("gap", "2");

        assertThat(converter.convert("$gap", variables))
            .hasValue(2);
    }

    @Test
    void returnsEmptyForInvalidValue() {
        assertThat(converter.convert("invalid", Collections.emptyMap())).isEmpty();
        assertThat(converter.convert("", Collections.emptyMap())).isEmpty();
        assertThat(converter.convert(null, Collections.emptyMap())).isEmpty();
        assertThat(converter.convert("1.5", Collections.emptyMap())).isEmpty();
        assertThat(converter.convert("10px", Collections.emptyMap())).isEmpty();
    }
}
