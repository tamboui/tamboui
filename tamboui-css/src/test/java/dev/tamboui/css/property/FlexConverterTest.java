/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.property;

import dev.tamboui.layout.Flex;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FlexConverterTest {

    private final FlexConverter converter = new FlexConverter();

    @Test
    void convertsStart() {
        assertThat(converter.convert("start", Collections.emptyMap()))
            .hasValue(Flex.START);
    }

    @Test
    void convertsCenter() {
        assertThat(converter.convert("center", Collections.emptyMap()))
            .hasValue(Flex.CENTER);
    }

    @Test
    void convertsEnd() {
        assertThat(converter.convert("end", Collections.emptyMap()))
            .hasValue(Flex.END);
    }

    @Test
    void convertsSpaceBetween() {
        assertThat(converter.convert("space-between", Collections.emptyMap()))
            .hasValue(Flex.SPACE_BETWEEN);
    }

    @Test
    void convertsSpaceAround() {
        assertThat(converter.convert("space-around", Collections.emptyMap()))
            .hasValue(Flex.SPACE_AROUND);
    }

    @Test
    void convertsSpaceEvenly() {
        assertThat(converter.convert("space-evenly", Collections.emptyMap()))
            .hasValue(Flex.SPACE_EVENLY);
    }

    @Test
    void caseInsensitive() {
        assertThat(converter.convert("START", Collections.emptyMap()))
            .hasValue(Flex.START);
        assertThat(converter.convert("Space-Between", Collections.emptyMap()))
            .hasValue(Flex.SPACE_BETWEEN);
    }

    @Test
    void handlesWhitespace() {
        assertThat(converter.convert("  center  ", Collections.emptyMap()))
            .hasValue(Flex.CENTER);
    }

    @Test
    void resolvesVariableReference() {
        Map<String, String> variables = new HashMap<>();
        variables.put("flex-mode", "space-evenly");

        assertThat(converter.convert("$flex-mode", variables))
            .hasValue(Flex.SPACE_EVENLY);
    }

    @Test
    void returnsEmptyForInvalidValue() {
        assertThat(converter.convert("invalid", Collections.emptyMap())).isEmpty();
        assertThat(converter.convert("", Collections.emptyMap())).isEmpty();
        assertThat(converter.convert(null, Collections.emptyMap())).isEmpty();
        assertThat(converter.convert("stretch", Collections.emptyMap())).isEmpty();
    }
}
