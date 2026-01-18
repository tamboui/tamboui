/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.layout.cassowary;

import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Flex;

import java.util.List;

/**
 * Cache key for layout solver results.
 *
 * <p>This key captures all inputs to the Cassowary solver that affect the output:
 * constraints, distributable space, spacing, and flex mode.
 */
final class LayoutCacheKey {
    private final List<Constraint> constraints;
    private final int distributable;
    private final int spacing;
    private final Flex flex;
    private final int cachedHashCode;

    LayoutCacheKey(List<Constraint> constraints, int distributable, int spacing, Flex flex) {
        this.constraints = constraints;
        this.distributable = distributable;
        this.spacing = spacing;
        this.flex = flex;
        this.cachedHashCode = computeHashCode();
    }

    private int computeHashCode() {
        int result = constraints.hashCode();
        result = 31 * result + distributable;
        result = 31 * result + spacing;
        result = 31 * result + flex.hashCode();
        return result;
    }

    @Override
    public int hashCode() {
        return cachedHashCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LayoutCacheKey)) {
            return false;
        }
        LayoutCacheKey that = (LayoutCacheKey) o;
        if (cachedHashCode != that.cachedHashCode) {
            return false;
        }
        return distributable == that.distributable
            && spacing == that.spacing
            && flex == that.flex
            && constraints.equals(that.constraints);
    }
}
