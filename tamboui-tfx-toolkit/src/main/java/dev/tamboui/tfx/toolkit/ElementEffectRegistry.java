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
import java.util.concurrent.atomic.AtomicBoolean;
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
    private ElementRegistry elementRegistry;
    private final Map<String, List<Effect>> pendingEffects = new LinkedHashMap<>();
    private final List<SelectorEffect> pendingSelectors = new ArrayList<>();
    private final List<SelectorEffect> activeSelectors = new ArrayList<>();
    private final Map<SelectorEffect, List<Effect>> selectorEffects = new LinkedHashMap<>();
    private final EffectManager globalEffects = new EffectManager();
    private final EffectManager runningEffects = new EffectManager();
    private final AtomicBoolean refreshRequested = new AtomicBoolean();

    /**
     * A pending effect targeting elements matching a CSS selector.
     */
    private static final class SelectorEffect {
        final String selector;
        final Effect effect;

        SelectorEffect(String selector, Effect effect) {
            this.selector = selector;
            this.effect = effect;
        }
    }

    /**
     * Creates a new ElementEffectRegistry.
     * <p>
     * The ElementRegistry must be set via {@link #setElementRegistry(ElementRegistry)}
     * before calling {@link #resolvePendingEffects()}.
     */
    public ElementEffectRegistry() {
    }

    /**
     * Sets the ElementRegistry used to resolve element areas.
     *
     * @param elementRegistry the element registry
     */
    public void setElementRegistry(ElementRegistry elementRegistry) {
        this.elementRegistry = elementRegistry;
    }

    /**
     * Adds an effect that targets a specific element by ID.
     * <p>
     * The effect will be resolved to the element's rendered area when
     * {@link #resolvePendingEffects()} is called.
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
     * Adds an effect that targets elements matching a CSS-like selector.
     * <p>
     * The effect will be applied to all elements matching the selector when
     * {@link #resolvePendingEffects()} is called. Each matching
     * element receives a copy of the effect.
     * <p>
     * Supported selectors:
     * <ul>
     *   <li>{@code #id} - matches element by ID</li>
     *   <li>{@code .class} - matches elements by CSS class</li>
     *   <li>{@code Type} - matches elements by type name</li>
     *   <li>{@code Type.class} - combined type and class</li>
     *   <li>{@code .class1.class2} - multiple classes</li>
     * </ul>
     *
     * @param selector the CSS-like selector
     * @param effect   the effect to add (copied for each matching element)
     * @return the number of elements that matched (0 if selector is deferred)
     */
    public int addEffectBySelector(String selector, Effect effect) {
        lock.lock();
        try {
            pendingSelectors.add(new SelectorEffect(selector, effect));
            return 0; // Matches counted during resolution
        } finally {
            lock.unlock();
        }
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
     * Requests a refresh of effect areas on the next call to {@link #resolvePendingEffects()}.
     * <p>
     * Call this when element positions may have changed (e.g., after a resize).
     * The actual refresh happens during the next {@link #resolvePendingEffects()} call,
     * ensuring the ElementRegistry has up-to-date data after rendering.
     */
    public void requestRefresh() {
        refreshRequested.set(true);
    }

    /**
     * Resolves pending effects to their target element areas.
     * <p>
     * This method queries the ElementRegistry for element areas and moves
     * pending effects to the running effects list. Effects targeting
     * elements that are not currently rendered remain pending.
     * <p>
     * Selector-based effects are resolved using the registry's query methods
     * and apply to all matching elements. If a refresh was requested via
     * {@link #requestRefresh()}, existing effect areas are also updated.
     * <p>
     * This should be called after rendering completes but before
     * {@link #processEffects(TFxDuration, Buffer, Rect)}.
     */
    public void resolvePendingEffects() {
        lock.lock();
        try {
            // Handle refresh request first (uses fresh registry data after render)
            if (refreshRequested.getAndSet(false)) {
                doRefreshAreas();
            }

            // Resolve ID-based effects
            List<String> resolved = new ArrayList<>();

            for (Map.Entry<String, List<Effect>> entry : pendingEffects.entrySet()) {
                String elementId = entry.getKey();
                Rect area = elementRegistry.getArea(elementId);

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

            // Resolve selector-based effects (iterate copy to allow clear)
            for (SelectorEffect se : new ArrayList<>(pendingSelectors)) {
                List<ElementRegistry.ElementInfo> matches = elementRegistry.queryAll(se.selector);
                // Track selector and effect instances for later area refresh
                activeSelectors.add(se);
                List<Effect> effects = new ArrayList<>();
                for (ElementRegistry.ElementInfo info : matches) {
                    Effect effectCopy = se.effect.copy().withArea(info.area());
                    effects.add(effectCopy);
                    runningEffects.addEffect(effectCopy);
                }
                selectorEffects.put(se, effects);
            }
            pendingSelectors.clear();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Internal method to refresh selector-based effect areas.
     * Called within the lock from resolvePendingEffects when refresh is requested.
     */
    private void doRefreshAreas() {
        // Iterate over a copy to avoid ConcurrentModificationException
        for (SelectorEffect se : new ArrayList<>(activeSelectors)) {
            List<ElementRegistry.ElementInfo> matches = elementRegistry.queryAll(se.selector);
            List<Effect> currentEffects = selectorEffects.get(se);
            if (currentEffects != null) {
                int matchCount = matches.size();
                int effectCount = currentEffects.size();

                // Update areas for effects that still have matching elements
                for (int i = 0; i < Math.min(matchCount, effectCount); i++) {
                    Effect effect = currentEffects.get(i);
                    Rect newArea = matches.get(i).area();
                    effect.setArea(newArea);
                }

                // If there are more matches than effects, create new effect instances
                if (matchCount > effectCount) {
                    for (int i = effectCount; i < matchCount; i++) {
                        Effect newEffect = se.effect.copy().withArea(matches.get(i).area());
                        currentEffects.add(newEffect);
                        runningEffects.addEffect(newEffect);
                    }
                }
                // Note: If there are fewer matches than effects, the extra effects
                // will render to areas that may no longer exist, but this is acceptable
                // since they'll just not be visible
            }
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
                   !pendingEffects.isEmpty() ||
                   !pendingSelectors.isEmpty();
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
            pendingSelectors.clear();
            activeSelectors.clear();
            selectorEffects.clear();
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
