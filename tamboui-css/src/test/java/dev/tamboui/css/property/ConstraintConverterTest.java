/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.property;

import dev.tamboui.layout.Constraint;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ConstraintConverterTest {

    private final ConstraintConverter converter = new ConstraintConverter();

    @Test
    void convertsFit() {
        Optional<Constraint> result = converter.convert("fit", Collections.emptyMap());
        assertThat(result).contains(Constraint.fit());
    }

    @Test
    void convertsFillWithDefaultWeight() {
        Optional<Constraint> result = converter.convert("fill", Collections.emptyMap());
        assertThat(result).contains(Constraint.fill());
    }

    @Test
    void convertsFillWithCustomWeight() {
        Optional<Constraint> result = converter.convert("fill(3)", Collections.emptyMap());
        assertThat(result).contains(Constraint.fill(3));
    }

    @Test
    void convertsFixedLength() {
        Optional<Constraint> result = converter.convert("10", Collections.emptyMap());
        assertThat(result).contains(Constraint.length(10));
    }

    @Test
    void convertsPercentage() {
        Optional<Constraint> result = converter.convert("50%", Collections.emptyMap());
        assertThat(result).contains(Constraint.percentage(50));
    }

    @Test
    void convertsMin() {
        Optional<Constraint> result = converter.convert("min(5)", Collections.emptyMap());
        assertThat(result).contains(Constraint.min(5));
    }

    @Test
    void convertsMax() {
        Optional<Constraint> result = converter.convert("max(100)", Collections.emptyMap());
        assertThat(result).contains(Constraint.max(100));
    }

    @Test
    void convertsRatio() {
        Optional<Constraint> result = converter.convert("1/3", Collections.emptyMap());
        assertThat(result).contains(Constraint.ratio(1, 3));
    }

    @Test
    void handlesWhitespace() {
        Optional<Constraint> result = converter.convert("  fill  ", Collections.emptyMap());
        assertThat(result).contains(Constraint.fill());
    }

    @Test
    void returnsEmptyForNull() {
        Optional<Constraint> result = converter.convert(null, Collections.emptyMap());
        assertThat(result).isEmpty();
    }

    @Test
    void returnsEmptyForEmptyString() {
        Optional<Constraint> result = converter.convert("", Collections.emptyMap());
        assertThat(result).isEmpty();
    }

    @Test
    void returnsEmptyForInvalidValue() {
        Optional<Constraint> result = converter.convert("invalid", Collections.emptyMap());
        assertThat(result).isEmpty();
    }

    @Test
    void resolvesVariables() {
        Map<String, String> vars = Collections.singletonMap("card-height", "5");
        Optional<Constraint> result = converter.convert("$card-height", vars);
        assertThat(result).contains(Constraint.length(5));
    }

    @Test
    void caseInsensitive() {
        Optional<Constraint> result = converter.convert("FILL", Collections.emptyMap());
        assertThat(result).contains(Constraint.fill());
    }
}
