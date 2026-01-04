/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.keymap;

import dev.tamboui.tui.event.KeyEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * Default implementation of {@link KeyMap} using an EnumMap of actions to bindings.
 * <p>
 * Instances are immutable; use {@link #toBuilder()} to create modified versions.
 */
public final class DefaultKeyMap implements KeyMap {

    private final Map<Action, List<KeyBinding>> bindings;

    private DefaultKeyMap(Map<Action, List<KeyBinding>> bindings) {
        // Create immutable copy
        Map<Action, List<KeyBinding>> copy = new EnumMap<>(Action.class);
        bindings.forEach((action, list) ->
            copy.put(action, Collections.unmodifiableList(new ArrayList<>(list))));
        this.bindings = Collections.unmodifiableMap(copy);
    }

    @Override
    public boolean matches(KeyEvent event, Action action) {
        List<KeyBinding> actionBindings = bindings.get(action);
        if (actionBindings == null) {
            return false;
        }
        for (KeyBinding binding : actionBindings) {
            if (binding.matches(event)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Optional<Action> actionFor(KeyEvent event) {
        for (Map.Entry<Action, List<KeyBinding>> entry : bindings.entrySet()) {
            for (KeyBinding binding : entry.getValue()) {
                if (binding.matches(event)) {
                    return Optional.of(entry.getKey());
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public List<KeyBinding> bindingsFor(Action action) {
        return bindings.getOrDefault(action, Collections.emptyList());
    }

    @Override
    public String describeBindings(Action action) {
        List<KeyBinding> list = bindingsFor(action);
        if (list.isEmpty()) {
            return "(unbound)";
        }
        StringJoiner sj = new StringJoiner(", ");
        for (KeyBinding b : list) {
            sj.add(b.toString());
        }
        return sj.toString();
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    /**
     * Creates a new builder for constructing a keymap.
     *
     * @return a new empty builder
     */
    public static Builder builder() {
        return new BuilderImpl();
    }

    private static class BuilderImpl implements Builder {
        private final Map<Action, List<KeyBinding>> bindings = new EnumMap<>(Action.class);

        BuilderImpl() {
        }

        BuilderImpl(DefaultKeyMap source) {
            source.bindings.forEach((action, list) ->
                bindings.put(action, new ArrayList<>(list)));
        }

        @Override
        public Builder bind(Action action, KeyBinding... newBindings) {
            bindings.computeIfAbsent(action, k -> new ArrayList<>())
                    .addAll(Arrays.asList(newBindings));
            return this;
        }

        @Override
        public Builder rebind(Action action, KeyBinding... newBindings) {
            bindings.put(action, new ArrayList<>(Arrays.asList(newBindings)));
            return this;
        }

        @Override
        public Builder unbind(Action action) {
            bindings.remove(action);
            return this;
        }

        @Override
        public Builder copyFrom(KeyMap other) {
            for (Action action : Action.values()) {
                List<KeyBinding> otherBindings = other.bindingsFor(action);
                if (!otherBindings.isEmpty()) {
                    bindings.computeIfAbsent(action, k -> new ArrayList<>())
                            .addAll(otherBindings);
                }
            }
            return this;
        }

        @Override
        public KeyMap build() {
            return new DefaultKeyMap(bindings);
        }
    }
}
