/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.property;

import dev.tamboui.layout.Margin;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MarginConverterTest {

    private final MarginConverter converter = new MarginConverter();

    @Test
    void convertsUniformMargin() {
        assertThat(converter.convert("5", Collections.emptyMap()))
            .hasValue(Margin.uniform(5));
    }

    @Test
    void convertsSymmetricMargin() {
        assertThat(converter.convert("2 4", Collections.emptyMap()))
            .hasValue(Margin.symmetric(2, 4));
    }

    @Test
    void convertsFourValueMargin() {
        assertThat(converter.convert("1 2 3 4", Collections.emptyMap()))
            .hasValue(new Margin(1, 2, 3, 4));
    }

    @Test
    void handlesWhitespace() {
        assertThat(converter.convert("  5  ", Collections.emptyMap()))
            .hasValue(Margin.uniform(5));
        assertThat(converter.convert("  1   2   3   4  ", Collections.emptyMap()))
            .hasValue(new Margin(1, 2, 3, 4));
    }

    @Test
    void resolvesVariableReference() {
        Map<String, String> variables = new HashMap<>();
        variables.put("margin-size", "3");

        assertThat(converter.convert("$margin-size", variables))
            .hasValue(Margin.uniform(3));
    }

    @Test
    void returnsEmptyForInvalidValue() {
        assertThat(converter.convert("invalid", Collections.emptyMap())).isEmpty();
        assertThat(converter.convert("", Collections.emptyMap())).isEmpty();
        assertThat(converter.convert(null, Collections.emptyMap())).isEmpty();
        assertThat(converter.convert("1 2 3", Collections.emptyMap())).isEmpty(); // 3 values not supported
        assertThat(converter.convert("1 2 3 4 5", Collections.emptyMap())).isEmpty(); // 5 values not supported
    }
}
