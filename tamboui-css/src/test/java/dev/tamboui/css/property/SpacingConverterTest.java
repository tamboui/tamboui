/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.property;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import dev.tamboui.layout.Padding;

import static org.assertj.core.api.Assertions.assertThat;

class SpacingConverterTest {

    private final SpacingConverter converter = SpacingConverter.INSTANCE;

    @Test
    void convertsUniformPadding() {
        Optional<Padding> padding = converter.convert("2", Collections.emptyMap());

        assertThat(padding).hasValue(Padding.uniform(2));
    }

    @Test
    void convertsSymmetricPadding() {
        Optional<Padding> padding = converter.convert("1 2", Collections.emptyMap());

        assertThat(padding).isPresent();
        Padding p = padding.get();
        assertThat(p.top()).isEqualTo(1);
        assertThat(p.bottom()).isEqualTo(1);
        assertThat(p.left()).isEqualTo(2);
        assertThat(p.right()).isEqualTo(2);
    }

    @Test
    void convertsFourValuePadding() {
        Optional<Padding> padding = converter.convert("1 2 3 4", Collections.emptyMap());

        assertThat(padding).isPresent();
        Padding p = padding.get();
        assertThat(p.top()).isEqualTo(1);
        assertThat(p.right()).isEqualTo(2);
        assertThat(p.bottom()).isEqualTo(3);
        assertThat(p.left()).isEqualTo(4);
    }

    @Test
    void handlesExtraWhitespace() {
        Optional<Padding> padding = converter.convert("  1   2  ", Collections.emptyMap());

        assertThat(padding).isPresent();
        assertThat(padding.get().top()).isEqualTo(1);
        assertThat(padding.get().left()).isEqualTo(2);
    }

    @Test
    void resolvesVariableReference() {
        Map<String, String> variables = new HashMap<>();
        variables.put("spacing", "3");

        Optional<Padding> padding = converter.convert("$spacing", variables);

        assertThat(padding).hasValue(Padding.uniform(3));
    }

    @Test
    void returnsEmptyForInvalidValue() {
        assertThat(converter.convert("invalid", Collections.emptyMap())).isEmpty();
        assertThat(converter.convert("", Collections.emptyMap())).isEmpty();
        assertThat(converter.convert(null, Collections.emptyMap())).isEmpty();
        assertThat(converter.convert("1 2 3", Collections.emptyMap())).isEmpty(); // 3 values not
                                                                                  // supported
    }

    @Test
    void returnsEmptyForNonNumericValues() {
        assertThat(converter.convert("abc", Collections.emptyMap())).isEmpty();
        assertThat(converter.convert("1px", Collections.emptyMap())).isEmpty();
    }
}
