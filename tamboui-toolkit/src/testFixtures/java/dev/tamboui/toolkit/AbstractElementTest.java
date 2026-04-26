/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import dev.tamboui.tui.RenderThreadTestHelper;

public abstract class AbstractElementTest {

    @BeforeEach
    void setUpRenderThread() {
        RenderThreadTestHelper.markAsRenderThread();
    }

    @AfterEach
    void tearDownRenderThread() {
        RenderThreadTestHelper.clearRenderThread();
    }

}
