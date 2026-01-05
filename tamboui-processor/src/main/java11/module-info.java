/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */

/**
 * TamboUI annotation processor module.
 * <p>
 * This module provides annotation processors for the TamboUI framework,
 * generating code for declarative action handling with {@code @OnAction}.
 */
module dev.tamboui.processor {
    requires dev.tamboui.annotations;
    requires dev.tamboui.tui;
    requires java.compiler;

    exports dev.tamboui.tui.bindings.processor;

    provides javax.annotation.processing.Processor
            with dev.tamboui.tui.bindings.processor.OnActionProcessor;
}
