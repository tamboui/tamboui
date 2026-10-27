/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.bindings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;

import dev.tamboui.tui.event.Event;

/**
 * Default implementation of {@link Bindings} using an ordered map of actions to triggers.
 * <p>
 * Instances are immutable; use {@link #toBuilder()} to create modified versions.
 */
public final class DefaultBindings implements Bindings {

    private final Map<String, List<InputTrigger>> triggers;

    private DefaultBindings(Map<String, List<InputTrigger>> triggers) {
        // Create immutable copy preserving insertion order
        Map<String, List<InputTrigger>> copy = new LinkedHashMap<>();
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
    public List<String> actionsFor(Event event) {
        if (event == null) {
            return Collections.emptyList();
        }

        List<Candidate> candidates = new ArrayList<>();
        int index = 0;
        for (Map.Entry<String, List<InputTrigger>> entry : triggers.entrySet()) {
            String action = entry.getKey();
            int maxSpec = Integer.MIN_VALUE;
            boolean matched = false;

            for (InputTrigger trigger : entry.getValue()) {
                if (trigger.matches(event)) {
                    matched = true;
                    int spec = trigger.specificity(event);
                    if (spec > maxSpec) {
                        maxSpec = spec;
                    }
                }
            }

            if (matched) {
                candidates.add(new Candidate(action, maxSpec, index));
            }
            index++;
        }

        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }

        // Sort by specificity descending, then by orderIndex descending (later bound wins)
        candidates.sort((a, b) -> {
            int cmp = Integer.compare(b.maxSpecificity, a.maxSpecificity);
            if (cmp != 0) {
                return cmp;
            }
            return Integer.compare(b.orderIndex, a.orderIndex);
        });

        List<String> result = new ArrayList<>(candidates.size());
        for (Candidate c : candidates) {
            result.add(c.action);
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public Optional<String> actionFor(Event event) {
        List<String> actions = actionsFor(event);
        return actions.isEmpty() ? Optional.empty() : Optional.of(actions.get(0));
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
        private final Map<String, List<InputTrigger>> triggers = new LinkedHashMap<>();

        BuilderImpl() {
        }

        BuilderImpl(DefaultBindings source) {
            source.triggers.forEach((action, list) ->
                triggers.put(action, new ArrayList<>(list)));
        }

        private static String validateActionName(String action) {
            if (action == null) {
                throw new IllegalArgumentException("Action name cannot be null");
            }
            if (!action.matches("[a-zA-Z0-9_]+")) {
                throw new IllegalArgumentException("Action name must be alphanumeric with underscores only: " + action);
            }
            return action;
        }

        @Override
        public Builder bind(InputTrigger trigger, String action) {
            String validName = validateActionName(action);
            List<InputTrigger> list = triggers.remove(validName);
            if (list == null) {
                list = new ArrayList<>();
            }
            list.add(trigger);
            triggers.put(validName, list);
            return this;
        }

        @Override
        public Builder rebind(InputTrigger trigger, String action) {
            String validName = validateActionName(action);
            triggers.remove(validName);
            List<InputTrigger> list = new ArrayList<>();
            list.add(trigger);
            triggers.put(validName, list);
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
                        List<InputTrigger> list = triggers.remove(action);
                        if (list == null) {
                            list = new ArrayList<>();
                        }
                        list.addAll(otherTriggers);
                        triggers.put(action, list);
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

    private static final class Candidate {
        private final String action;
        private final int maxSpecificity;
        private final int orderIndex;

        Candidate(String action, int maxSpecificity, int orderIndex) {
            this.action = action;
            this.maxSpecificity = maxSpecificity;
            this.orderIndex = orderIndex;
        }
    }
}
