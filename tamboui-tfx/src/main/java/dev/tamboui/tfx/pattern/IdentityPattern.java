/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tfx.pattern;

import dev.tamboui.layout.Position;
import dev.tamboui.layout.Rect;

/**
 * Identity pattern that returns the global alpha unchanged.
 * <p>
 * This is the default pattern - it allows effects to work without spatial patterns.
 */
public final class IdentityPattern implements Pattern {
    
    /** Singleton instance of the identity pattern. */
    public static final IdentityPattern INSTANCE = new IdentityPattern();
    
    private IdentityPattern() {
    }
    
    @Override
    public float mapAlpha(float globalAlpha, Position position, Rect area) {
        return globalAlpha;
    }

    @Override
    public float mapAlpha(float globalAlpha, int x, int y, Rect area) {
        return globalAlpha;
    }

    @Override
    public String name() {
        return "identity";
    }
}


