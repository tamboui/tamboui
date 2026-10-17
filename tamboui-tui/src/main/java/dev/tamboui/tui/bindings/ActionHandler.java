/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.bindings;

import java.lang.reflect.Method;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import dev.tamboui.annotations.bindings.OnAction;
import dev.tamboui.tui.event.ActionEvent;
import dev.tamboui.tui.event.Event;

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
    // Linked, so dispatch tries the other actions an event matches in a stable order
    private final Map<String, List<BiConsumer<Event, String>>> handlers = new LinkedHashMap<>();

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
        handlers.computeIfAbsent(action, k -> new ArrayList<>()).add((e, a) -> handler.accept(e));
        return this;
    }

    /**
     * Registers a handler for the specified action that also receives the action name.
     * <p>
     * This is useful when the same handler is registered for multiple actions
     * and needs to know which action triggered it.
     * <p>
     * Multiple handlers can be registered for the same action; they will
     * be invoked in registration order when the action is triggered.
     *
     * @param action  the action name to handle
     * @param handler the handler to invoke, receiving the event and action name
     * @return this handler for method chaining
     */
    public ActionHandler on(String action, BiConsumer<Event, String> handler) {
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
     * Fires a named action directly, bypassing bindings resolution.
     * <p>
     * Unlike {@link #dispatch(Event)}, this method does not resolve the action
     * from the event via bindings. Instead, the action name is provided explicitly,
     * allowing programmatic triggering of actions.
     * <p>
     * This is useful when an element interaction (e.g., a click) should trigger
     * a named action that is also bound to a keyboard shortcut:
     * <pre>{@code
     * // Key binding: Ctrl+S triggers "save" (via bindings)
     * // Element click also fires "save" (via fire):
     * text("Save").focusable().on(MouseTrigger.click(), e -> handler.fire("save", e));
     * }</pre>
     *
     * @param action the action name to fire
     * @param event  the event that triggered this action
     * @return true if the action was handled (handlers registered and invoked),
     *         false otherwise
     */
    public boolean fire(String action, Event event) {
        List<BiConsumer<Event, String>> list = handlers.get(action);
        if (list != null && !list.isEmpty()) {
            for (BiConsumer<Event, String> h : list) {
                h.accept(event, action);
            }
            return true;
        }
        return false;
    }

    /**
     * Fires a named action without an originating event.
     * <p>
     * An {@link dev.tamboui.tui.event.ActionEvent} is created to represent the
     * programmatic trigger. This is useful when no input event is involved
     * (e.g., firing an action from a timer or application logic).
     *
     * @param action the action name to fire
     * @return true if the action was handled (handlers registered and invoked),
     *         false otherwise
     */
    public boolean fire(String action) {
        return fire(action, ActionEvent.of(action));
    }

    /**
     * Dispatches an event to registered handlers.
     * <p>
     * The event is matched against the bindings, and the handlers of one matching action are
     * invoked in registration order. An event can match several actions (with the standard
     * bindings Enter is both {@link Actions#SELECT} and {@link Actions#CONFIRM}), so the action
     * fired is the first one that has handlers: the action the bindings resolve the event to
     * ({@link Bindings#actionFor}), otherwise the other matching actions in the order their
     * handlers were first registered.
     *
     * @param event the event to dispatch
     * @return true if the event was handled (a matching action with registered handlers),
     *         false otherwise
     */
    public boolean dispatch(Event event) {
        Optional<String> resolved = bindings.actionFor(event);
        if (resolved.isPresent() && fire(resolved.get(), event)) {
            return true;
        }
        for (String action : handlers.keySet()) {
            if (!action.equals(resolved.orElse(null)) && bindings.matches(event, action)) {
                return fire(action, event);
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
        List<BiConsumer<Event, String>> list = handlers.get(action);
        return list != null && !list.isEmpty();
    }

    /**
     * Checks if any action handlers are registered.
     *
     * @return true if no handlers are registered, false otherwise
     */
    public boolean isEmpty() {
        return handlers.isEmpty();
    }

    /**
     * Discovers and registers action handlers from the target object.
     * <p>
     * First attempts to find generated {@link ActionHandlerRegistrar} implementations
     * via ServiceLoader. If no registrar is found for a class in the target's class hierarchy,
     * falls back to reflection-based discovery of {@code @OnAction} annotated methods.
     *
     * @param target the object containing {@code @OnAction} annotated methods
     * @param <T>    the target type
     * @return this handler for method chaining
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> ActionHandler registerAnnotated(T target) {
        Class<?> targetClass = target.getClass();
        ClassLoader classLoader = targetClass.getClassLoader();
        ServiceLoader<ActionHandlerRegistrar> loader = classLoader != null
                ? ServiceLoader.load(ActionHandlerRegistrar.class, classLoader)
                : ServiceLoader.load(ActionHandlerRegistrar.class);
        List<ActionHandlerRegistrar<?>> registrars = new ArrayList<>();
        for (ActionHandlerRegistrar<?> registrar : loader) {
            registrars.add(registrar);
        }

        Set<String> seenMethods = new HashSet<>();
        Class<?> clazz = targetClass;
        while (clazz != null && clazz != Object.class) {
            boolean foundRegistrar = false;
            for (ActionHandlerRegistrar<?> registrar : registrars) {
                if (registrar.targetType() == clazz) {
                    ((ActionHandlerRegistrar<Object>) registrar).register(target, this);
                    foundRegistrar = true;
                }
            }

            if (!foundRegistrar) {
                registerViaReflection(target, clazz, seenMethods);
            }
            clazz = clazz.getSuperclass();
        }

        return this;
    }

    /**
     * Registers action handlers via reflection by scanning for {@code @OnAction} methods on the given class.
     */
    private <T> void registerViaReflection(T target, Class<?> clazz, Set<String> seenMethods) {
        for (Method method : clazz.getDeclaredMethods()) {
            OnAction annotation = method.getAnnotation(OnAction.class);
            if (annotation != null) {
                String signature = method.getName() + Arrays.toString(method.getParameterTypes());
                if (seenMethods.add(signature)) {
                    String action = annotation.value();
                    method.setAccessible(true);
                    Class<?>[] parameterTypes = method.getParameterTypes();
                    on(action, event -> {
                        // A method taking a subtype such as KeyEvent is only called for events of that type
                        if (parameterTypes.length == 1 && !parameterTypes[0].isInstance(event)) {
                            return;
                        }
                        try {
                            if (parameterTypes.length == 0) {
                                method.invoke(target);
                            } else {
                                method.invoke(target, event);
                            }
                        } catch (Exception e) {
                            throw new IllegalStateException(
                                    "Failed to invoke @OnAction method: " + method.getName(), e);
                        }
                    });
                }
            }
        }
    }
}
