/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.bindings;

import dev.tamboui.annotations.bindings.OnAction;
import dev.tamboui.tui.event.Event;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Consumer;

/**
 * Dispatches events to registered action handlers.
 * <p>
 * ActionHandler wraps a {@link Bindings} instance and allows registering
 * callbacks for specific actions. When an event is dispatched, it is matched
 * against the bindings and the corresponding handlers are invoked.
 *
 * <pre>{@code
 * ActionHandler handler = new ActionHandler(BindingSets.vim())
 *     .on(Actions.QUIT, e -> runner.quit())
 *     .on("save", this::save)
 *     .on("delete", this::delete);
 *
 * // In event handler:
 * if (handler.dispatch(event)) {
 *     return EventResult.HANDLED;
 * }
 * }</pre>
 *
 * @see Bindings
 */
public final class ActionHandler {

    private Bindings bindings;
    private final Map<String, List<Consumer<Event>>> handlers = new HashMap<>();

    /**
     * Creates a new action handler with the given bindings.
     *
     * @param bindings the bindings to use for matching events to actions
     */
    public ActionHandler(Bindings bindings) {
        this.bindings = bindings;
    }

    /**
     * Registers a handler for the specified action.
     * <p>
     * Multiple handlers can be registered for the same action; they will
     * be invoked in registration order when the action is triggered.
     *
     * @param action  the action name to handle
     * @param handler the handler to invoke when the action is triggered
     * @return this handler for method chaining
     */
    public ActionHandler on(String action, Consumer<Event> handler) {
        handlers.computeIfAbsent(action, k -> new ArrayList<>()).add(handler);
        return this;
    }

    /**
     * Removes all handlers for the specified action.
     *
     * @param action the action name
     * @return this handler for method chaining
     */
    public ActionHandler off(String action) {
        handlers.remove(action);
        return this;
    }

    /**
     * Sets the bindings used for matching events to actions.
     * <p>
     * This allows changing the key bindings at runtime without
     * re-registering handlers.
     *
     * @param bindings the new bindings to use
     */
    public void setBindings(Bindings bindings) {
        this.bindings = bindings;
    }

    /**
     * Returns the current bindings.
     *
     * @return the bindings
     */
    public Bindings bindings() {
        return bindings;
    }

    /**
     * Dispatches an event to registered handlers.
     * <p>
     * The event is matched against the bindings. If a matching action is found
     * and handlers are registered for that action, all handlers are invoked
     * in registration order.
     *
     * @param event the event to dispatch
     * @return true if the event was handled (action found with registered handlers),
     *         false otherwise
     */
    public boolean dispatch(Event event) {
        Optional<String> action = bindings.actionFor(event);
        if (action.isPresent()) {
            List<Consumer<Event>> list = handlers.get(action.get());
            if (list != null && !list.isEmpty()) {
                for (Consumer<Event> h : list) {
                    h.accept(event);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if any handlers are registered for the specified action.
     *
     * @param action the action name
     * @return true if handlers are registered, false otherwise
     */
    public boolean hasHandlers(String action) {
        List<Consumer<Event>> list = handlers.get(action);
        return list != null && !list.isEmpty();
    }

    /**
     * Discovers and registers action handlers from the target object.
     * <p>
     * First attempts to find generated {@link ActionHandlerRegistrar} implementations
     * via ServiceLoader. If no registrar is found for the target's exact class,
     * falls back to reflection-based discovery of {@code @OnAction} annotated methods.
     *
     * @param target the object containing {@code @OnAction} annotated methods
     * @param <T>    the target type
     * @return this handler for method chaining
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> ActionHandler registerAnnotated(T target) {
        Class<?> targetClass = target.getClass();
        boolean foundRegistrar = false;

        ServiceLoader<ActionHandlerRegistrar> loader =
                ServiceLoader.load(ActionHandlerRegistrar.class);
        for (ActionHandlerRegistrar<?> registrar : loader) {
            if (registrar.targetType() == targetClass) {
                ((ActionHandlerRegistrar<T>) registrar).register(target, this);
                foundRegistrar = true;
            }
        }

        if (!foundRegistrar) {
            registerViaReflection(target);
        }

        return this;
    }

    /**
     * Registers action handlers via reflection by scanning for {@code @OnAction} methods.
     */
    private <T> void registerViaReflection(T target) {
        for (Method method : target.getClass().getDeclaredMethods()) {
            OnAction annotation = method.getAnnotation(OnAction.class);
            if (annotation != null) {
                String action = annotation.value();
                method.setAccessible(true);
                on(action, event -> {
                    try {
                        method.invoke(target, event);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to invoke @OnAction method: " + method.getName(), e);
                    }
                });
            }
        }
    }
}
