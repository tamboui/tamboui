/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.ConstraintConverter;
import dev.tamboui.style.PropertyConverter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Converts a space-separated string of constraint tokens to a {@code List<Constraint>}.
 * <p>
 * Each token is delegated to {@link ConstraintConverter} for parsing.
 * Returns {@link Optional#empty()} if any token fails to parse.
 * <p>
 * Example: {@code "fill fill(2) 20"} → {@code [Constraint.fill(), Constraint.fill(2), Constraint.length(20)]}
 */
public final class ConstraintListConverter implements PropertyConverter<List<Constraint>> {

    /**
     * Singleton instance.
     */
    public static final ConstraintListConverter INSTANCE = new ConstraintListConverter();

    private ConstraintListConverter() {
    }

    @Override
    public Optional<List<Constraint>> convert(String value) {
        if (value == null || value.trim().isEmpty()) {
            return Optional.empty();
        }

        String[] tokens = value.trim().split("\\s+");
        List<Constraint> constraints = new ArrayList<>(tokens.length);
        for (String token : tokens) {
            Optional<Constraint> parsed = ConstraintConverter.INSTANCE.convert(token);
            if (!parsed.isPresent()) {
                return Optional.empty();
            }
            constraints.add(parsed.get());
        }
        return Optional.of(constraints);
    }
}
