/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.property;

import dev.tamboui.layout.Direction;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DirectionConverterTest {

    private final DirectionConverter converter = new DirectionConverter();

    @Test
    void convertsHorizontal() {
        assertThat(converter.convert("horizontal", Collections.emptyMap()))
            .hasValue(Direction.HORIZONTAL);
    }

    @Test
    void convertsRow() {
        assertThat(converter.convert("row", Collections.emptyMap()))
            .hasValue(Direction.HORIZONTAL);
    }

    @Test
    void convertsVertical() {
        assertThat(converter.convert("vertical", Collections.emptyMap()))
            .hasValue(Direction.VERTICAL);
    }

    @Test
    void convertsColumn() {
        assertThat(converter.convert("column", Collections.emptyMap()))
            .hasValue(Direction.VERTICAL);
    }

    @Test
    void caseInsensitive() {
        assertThat(converter.convert("HORIZONTAL", Collections.emptyMap()))
            .hasValue(Direction.HORIZONTAL);
        assertThat(converter.convert("Vertical", Collections.emptyMap()))
            .hasValue(Direction.VERTICAL);
        assertThat(converter.convert("ROW", Collections.emptyMap()))
            .hasValue(Direction.HORIZONTAL);
    }

    @Test
    void handlesWhitespace() {
        assertThat(converter.convert("  vertical  ", Collections.emptyMap()))
            .hasValue(Direction.VERTICAL);
    }

    @Test
    void resolvesVariableReference() {
        Map<String, String> variables = new HashMap<>();
        variables.put("layout-dir", "horizontal");

        assertThat(converter.convert("$layout-dir", variables))
            .hasValue(Direction.HORIZONTAL);
    }

    @Test
    void returnsEmptyForInvalidValue() {
        assertThat(converter.convert("invalid", Collections.emptyMap())).isEmpty();
        assertThat(converter.convert("", Collections.emptyMap())).isEmpty();
        assertThat(converter.convert(null, Collections.emptyMap())).isEmpty();
        assertThat(converter.convert("diagonal", Collections.emptyMap())).isEmpty();
    }
}
