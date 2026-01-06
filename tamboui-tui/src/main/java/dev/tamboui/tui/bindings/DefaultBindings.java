/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.bindings;

import dev.tamboui.tui.event.Event;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;

/**
 * Default implementation of {@link Bindings} using a HashMap of actions to triggers.
 * <p>
 * Instances are immutable; use {@link #toBuilder()} to create modified versions.
 */
public final class DefaultBindings implements Bindings {

    private final Map<String, List<InputTrigger>> triggers;

    private DefaultBindings(Map<String, List<InputTrigger>> triggers) {
        // Create immutable copy
        Map<String, List<InputTrigger>> copy = new HashMap<>();
        triggers.forEach((action, list) ->
            copy.put(action, Collections.unmodifiableList(new ArrayList<>(list))));
        this.triggers = Collections.unmodifiableMap(copy);
    }

    @Override
    public boolean matches(Event event, String action) {
        List<InputTrigger> actionTriggers = triggers.get(action);
        if (actionTriggers == null) {
            return false;
        }
        for (InputTrigger trigger : actionTriggers) {
            if (trigger.matches(event)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Optional<String> actionFor(Event event) {
        for (Map.Entry<String, List<InputTrigger>> entry : triggers.entrySet()) {
            for (InputTrigger trigger : entry.getValue()) {
                if (trigger.matches(event)) {
                    return Optional.of(entry.getKey());
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public List<InputTrigger> triggersFor(String action) {
        return triggers.getOrDefault(action, Collections.emptyList());
    }

    @Override
    public String describeBindings(String action) {
        List<InputTrigger> list = triggersFor(action);
        if (list.isEmpty()) {
            return "(unbound)";
        }
        StringJoiner sj = new StringJoiner(", ");
        for (InputTrigger t : list) {
            sj.add(t.describe());
        }
        return sj.toString();
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    /**
     * Returns all action names that have triggers defined.
     *
     * @return set of action names
     */
    public Set<String> actions() {
        return triggers.keySet();
    }

    /**
     * Creates a new builder for constructing bindings.
     *
     * @return a new empty builder
     */
    public static Builder builder() {
        return new BuilderImpl();
    }

    private static class BuilderImpl implements Builder {
        private final Map<String, List<InputTrigger>> triggers = new HashMap<>();

        BuilderImpl() {
        }

        BuilderImpl(DefaultBindings source) {
            source.triggers.forEach((action, list) ->
                triggers.put(action, new ArrayList<>(list)));
        }

        @Override
        public Builder bind(String action, InputTrigger... newTriggers) {
            triggers.computeIfAbsent(action, k -> new ArrayList<>())
                    .addAll(Arrays.asList(newTriggers));
            return this;
        }

        @Override
        public Builder rebind(String action, InputTrigger... newTriggers) {
            triggers.put(action, new ArrayList<>(Arrays.asList(newTriggers)));
            return this;
        }

        @Override
        public Builder unbind(String action) {
            triggers.remove(action);
            return this;
        }

        @Override
        public Builder copyFrom(Bindings other) {
            if (other instanceof DefaultBindings) {
                DefaultBindings db = (DefaultBindings) other;
                for (String action : db.actions()) {
                    List<InputTrigger> otherTriggers = db.triggersFor(action);
                    if (!otherTriggers.isEmpty()) {
                        triggers.computeIfAbsent(action, k -> new ArrayList<>())
                                .addAll(otherTriggers);
                    }
                }
            }
            return this;
        }

        @Override
        public Bindings build() {
            return new DefaultBindings(triggers);
        }
    }
}
