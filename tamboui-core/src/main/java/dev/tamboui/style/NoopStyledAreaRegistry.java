/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.style;

import java.util.Collections;
import java.util.List;

import dev.tamboui.layout.Rect;

/**
 * A no-op implementation of {@link StyledAreaRegistry} that ignores all
 * operations.
 * <p>
 * This is useful when styled area tracking is not needed, as it avoids the
 * overhead of maintaining data structures.
 */
final class NoopStyledAreaRegistry implements StyledAreaRegistry {

    static final NoopStyledAreaRegistry INSTANCE = new NoopStyledAreaRegistry();

    private NoopStyledAreaRegistry() {
    }

    @Override
    public void register(Style style, Rect area, String contextKey) {
        // No-op
    }

    @Override
    public List<StyledAreaInfo> all() {
        return Collections.emptyList();
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public void clear() {
        // No-op
    }
}
