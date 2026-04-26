/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui;

public final class RenderThreadTestHelper {

    private RenderThreadTestHelper() {
    }

    public static void markAsRenderThread() {
        RenderThread.markAsRenderThread();
    }

    public static void clearRenderThread() {
        RenderThread.clearRenderThread();
    }
}