/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.style;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dev.tamboui.layout.Rect;

/**
 * Default implementation of {@link StyledAreaRegistry}.
 */
final class DefaultStyledAreaRegistry implements StyledAreaRegistry {

    private final List<StyledAreaInfo> allAreas = new ArrayList<>();

    @Override
    public void register(Style style, Rect area, String contextKey) {
        if (style == null || area == null) {
            return;
        }

        Tags tags = style.extension(Tags.class, Tags.empty());

        // Only register if there are tags (CSS classes)
        if (tags.isEmpty()) {
            return;
        }

        StyledAreaInfo info = new StyledAreaInfo(tags, area, contextKey);
        allAreas.add(info);
    }

    @Override
    public List<StyledAreaInfo> all() {
        return Collections.unmodifiableList(allAreas);
    }

    @Override
    public int size() {
        return allAreas.size();
    }

    @Override
    public void clear() {
        allAreas.clear();
    }
}
