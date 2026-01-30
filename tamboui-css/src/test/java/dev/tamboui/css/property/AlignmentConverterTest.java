/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.property;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import dev.tamboui.layout.Alignment;

import static org.assertj.core.api.Assertions.assertThat;

class AlignmentConverterTest {

    private final AlignmentConverter converter = AlignmentConverter.INSTANCE;

    @Test
    void convertsLeftAlignment() {
        assertThat(converter.convert("left", Collections.emptyMap())).hasValue(Alignment.LEFT);
    }

    @Test
    void convertsCenterAlignment() {
        assertThat(converter.convert("center", Collections.emptyMap())).hasValue(Alignment.CENTER);
    }

    @Test
    void convertsRightAlignment() {
        assertThat(converter.convert("right", Collections.emptyMap())).hasValue(Alignment.RIGHT);
    }

    @Test
    void caseInsensitive() {
        assertThat(converter.convert("LEFT", Collections.emptyMap())).hasValue(Alignment.LEFT);
        assertThat(converter.convert("Center", Collections.emptyMap())).hasValue(Alignment.CENTER);
        assertThat(converter.convert("RIGHT", Collections.emptyMap())).hasValue(Alignment.RIGHT);
    }

    @Test
    void handlesWhitespace() {
        assertThat(converter.convert("  center  ", Collections.emptyMap()))
                .hasValue(Alignment.CENTER);
    }

    @Test
    void resolvesVariableReference() {
        Map<String, String> variables = new HashMap<>();
        variables.put("align", "center");

        assertThat(converter.convert("$align", variables)).hasValue(Alignment.CENTER);
    }

    @Test
    void returnsEmptyForInvalidValue() {
        assertThat(converter.convert("invalid", Collections.emptyMap())).isEmpty();
        assertThat(converter.convert("", Collections.emptyMap())).isEmpty();
        assertThat(converter.convert(null, Collections.emptyMap())).isEmpty();
        assertThat(converter.convert("justify", Collections.emptyMap())).isEmpty();
    }
}
