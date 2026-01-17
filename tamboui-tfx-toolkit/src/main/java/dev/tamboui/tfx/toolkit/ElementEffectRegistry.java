/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tfx.toolkit;

import dev.tamboui.layout.Rect;
import dev.tamboui.tfx.Effect;
import dev.tamboui.tfx.EffectManager;
import dev.tamboui.tfx.TFxDuration;
import dev.tamboui.buffer.Buffer;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.ElementRegistry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Registry for effects that target specific elements by ID.
 * <p>
 * ElementEffectRegistry manages effects that are associated with element IDs.
 * When an element is rendered and its area becomes available via the EventRouter,
 * pending effects are resolved to their target areas and begin running.
 * <p>
 * <b>Design Philosophy:</b>
 * <ul>
 *   <li><b>Deferred Resolution:</b> Effects are added by element ID and resolved
 *       to areas when the element is actually rendered</li>
 *   <li><b>Thread-Safe:</b> Uses locking for safe access from multiple threads</li>
 *   <li><b>No Coupling:</b> Does not require Element interface changes</li>
 * </ul>
 * <p>
 * <b>Usage:</b>
 * <pre>{@code
 * ElementEffectRegistry registry = new ElementEffectRegistry();
 *
 * // Add effect targeting an element
 * registry.addEffect("header", Fx.fadeFromFg(Color.BLACK, 800, Interpolation.QuadOut));
 *
 * // In render loop, resolve and process
 * registry.resolvePendingEffects(eventRouter);
 * registry.processEffects(delta, buffer, fullArea);
 * }</pre>
 *
 * @see ToolkitEffects
 */
public final class ElementEffectRegistry {

    private final ReentrantLock lock = new ReentrantLock();
    private final Map<String, List<Effect>> pendingEffects = new LinkedHashMap<>();
    private final EffectManager globalEffects = new EffectManager();
    private final EffectManager runningEffects = new EffectManager();

    /**
     * Creates a new ElementEffectRegistry.
     */
    public ElementEffectRegistry() {
    }

    /**
     * Adds an effect that targets a specific element by ID.
     * <p>
     * The effect will be resolved to the element's rendered area when
     * {@link #resolvePendingEffects(EventRouter)} is called.
     *
     * @param elementId the ID of the target element
     * @param effect    the effect to add
     */
    public void addEffect(String elementId, Effect effect) {
        lock.lock();
        try {
            pendingEffects.computeIfAbsent(elementId, k -> new ArrayList<>()).add(effect);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Adds an effect that targets a specific element.
     * <p>
     * Convenience method that extracts the element's ID.
     *
     * @param element the target element
     * @param effect  the effect to add
     * @throws IllegalArgumentException if the element has no ID
     */
    public void addEffect(Element element, Effect effect) {
        String id = element.id();
        if (id == null) {
            throw new IllegalArgumentException("Element must have an ID to receive effects");
        }
        addEffect(id, effect);
    }

    /**
     * Adds a global effect that applies to the entire frame area.
     * <p>
     * Global effects are processed without targeting a specific element.
     *
     * @param effect the effect to add
     */
    public void addGlobalEffect(Effect effect) {
        lock.lock();
        try {
            globalEffects.addEffect(effect);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Resolves pending effects to their target element areas.
     * <p>
     * This method queries the ElementRegistry for element areas and moves
     * pending effects to the running effects list. Effects targeting
     * elements that are not currently rendered remain pending.
     * <p>
     * This should be called after rendering completes but before
     * {@link #processEffects(TFxDuration, Buffer, Rect)}.
     *
     * @param registry the element registry containing element areas
     */
    public void resolvePendingEffects(ElementRegistry registry) {
        lock.lock();
        try {
            List<String> resolved = new ArrayList<>();

            for (Map.Entry<String, List<Effect>> entry : pendingEffects.entrySet()) {
                String elementId = entry.getKey();
                Rect area = registry.getArea(elementId);

                if (area != null) {
                    // Element is rendered, resolve effects to its area
                    for (Effect effect : entry.getValue()) {
                        runningEffects.addEffect(effect.withArea(area));
                    }
                    resolved.add(elementId);
                }
            }

            // Remove resolved entries
            for (String id : resolved) {
                pendingEffects.remove(id);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Processes all active effects.
     * <p>
     * This processes both global effects and element-targeted effects
     * that have been resolved. Effects are automatically removed when complete.
     *
     * @param delta  the time elapsed since the last frame
     * @param buffer the buffer to apply effects to
     * @param area   the default area for global effects
     */
    public void processEffects(TFxDuration delta, Buffer buffer, Rect area) {
        lock.lock();
        try {
            globalEffects.processEffects(delta, buffer, area);
            runningEffects.processEffects(delta, buffer, area);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns whether any effects are currently running.
     *
     * @return true if effects are active
     */
    public boolean isRunning() {
        lock.lock();
        try {
            return globalEffects.isRunning() ||
                   runningEffects.isRunning() ||
                   !pendingEffects.isEmpty();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Clears all effects (pending, running, and global).
     */
    public void clear() {
        lock.lock();
        try {
            pendingEffects.clear();
            globalEffects.clear();
            runningEffects.clear();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns the total number of pending effects.
     *
     * @return pending effect count
     */
    public int pendingCount() {
        lock.lock();
        try {
            int count = 0;
            for (List<Effect> effects : pendingEffects.values()) {
                count += effects.size();
            }
            return count;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns the number of running effects (global + element-targeted).
     *
     * @return running effect count
     */
    public int runningCount() {
        lock.lock();
        try {
            return globalEffects.size() + runningEffects.size();
        } finally {
            lock.unlock();
        }
    }
}
