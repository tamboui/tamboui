/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.annotations.bindings;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Marks a method as an action handler for the specified action.
///
///
///
/// When used with the annotation processor, methods annotated with {@code @OnAction}
/// will be automatically registered with an {@code ActionHandler} via generated code.
///
/// ```java
/// public class MyApp {
///     @OnAction(Actions.QUIT)
///     void quit(Event event) {
///         runner.quit();
///     }
///
///     @OnAction("save")
///     void save(Event event) {
///         saveDocument();
///     }
/// }
/// }
/// ```
///
///
///
/// The annotated method must:
///
/// - Have exactly one parameter of type {@code Event} (or a subtype)
/// - Return {@code void}
/// - Be accessible (not private)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface OnAction {
    /// The action name to handle.
    ///
    ///
    ///
    /// Can be a constant from {@code Actions} or a custom string.
    ///
    /// @return the action name
    String value();
}

