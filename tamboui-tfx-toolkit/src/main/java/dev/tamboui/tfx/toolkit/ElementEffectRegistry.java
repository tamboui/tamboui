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
import dev.tamboui.tui.RenderThread;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry for effects that target specific elements by ID.
 * <p>
 * ElementEffectRegistry manages effects that are associated with element IDs.
 * Effects are stored with their element ID associations, and areas are looked up
 * dynamically each frame from the ElementRegistry. This ensures effects automatically
 * follow their target elements when the terminal resizes.
 * <p>
 * <b>Design Philosophy:</b>
 * <ul>
 *   <li><b>Dynamic Area Lookup:</b> Effect areas are looked up each frame, so effects
 *       automatically follow elements when they move or resize</li>
 *   <li><b>render thread:</b> All mutating operations must be called from the render thread</li>
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
 * // In render loop, expand selectors and process
 * registry.expandSelectors(elementRegistry);
 * registry.processEffects(delta, buffer, fullArea, elementRegistry);
 * }</pre>
 *
 * @see ToolkitEffects
 */
public final class ElementEffectRegistry {

    // ID-based effects: keep ID→effect mapping for dynamic lookup
    private final Map<String, List<Effect>> idEffects = new LinkedHashMap<>();

    // Selector effects: keep selector→instances mapping
    // (selectors are expanded once, then instances tracked)
    private final List<SelectorEffect> pendingSelectors = new ArrayList<>();
    private final Map<SelectorEffect, List<Effect>> selectorEffects = new LinkedHashMap<>();

    // Global effects don't need element lookup
    private final EffectManager globalEffects = new EffectManager();

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
     */
    public ElementEffectRegistry() {
    }

    /**
     * Adds an effect that targets a specific element by ID.
     * <p>
     * The effect will be applied to the element's current rendered area each frame.
     * The area is looked up dynamically, so effects automatically follow elements
     * when the terminal resizes.
     * <p>
     * Must be called from the render thread.
     *
     * @param elementId the ID of the target element
     * @param effect    the effect to add
     */
    public void addEffect(String elementId, Effect effect) {
        RenderThread.checkRenderThread();
        idEffects.computeIfAbsent(elementId, k -> new ArrayList<>()).add(effect);
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
     * {@link #expandSelectors(ElementRegistry)} is called. Each matching
     * element receives a copy of the effect.
     * <p>
     * Must be called from the render thread.
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
        RenderThread.checkRenderThread();
        pendingSelectors.add(new SelectorEffect(selector, effect));
        return 0; // Matches counted during resolution
    }

    /**
     * Adds a global effect that applies to the entire frame area.
     * <p>
     * Global effects are processed without targeting a specific element.
     * <p>
     * Must be called from the render thread.
     *
     * @param effect the effect to add
     */
    public void addGlobalEffect(Effect effect) {
        RenderThread.checkRenderThread();
        globalEffects.addEffect(effect);
    }

    /**
     * Expands pending selector-based effects to individual effect instances.
     * <p>
     * This method queries the ElementRegistry to find elements matching each
     * pending selector and creates effect copies for each match. The selector
     * and its effect instances are then tracked for dynamic area lookup.
     * <p>
     * This should be called after rendering completes but before
     * {@link #processEffects(TFxDuration, Buffer, Rect, ElementRegistry)}.
     *
     * @param registry the element registry containing element areas
     */
    public void expandSelectors(ElementRegistry registry) {
        RenderThread.checkRenderThread();
        for (SelectorEffect se : pendingSelectors) {
            List<ElementRegistry.ElementInfo> matches = registry.queryAll(se.selector);
            List<Effect> effects = new ArrayList<>();
            for (int i = 0; i < matches.size(); i++) {
                effects.add(se.effect.copy());  // No area baked in!
            }
            selectorEffects.put(se, effects);
        }
        pendingSelectors.clear();
    }

    /**
     * Processes all active effects with dynamic area lookup.
     * <p>
     * This processes global effects, ID-based effects, and selector-based effects.
     * For ID-based and selector-based effects, the element areas are looked up
     * each frame from the registry, so effects automatically follow elements
     * when the terminal resizes.
     * <p>
     * Effects are automatically removed when complete.
     *
     * @param delta    the time elapsed since the last frame
     * @param buffer   the buffer to apply effects to
     * @param area     the default area for global effects
     * @param registry the element registry for area lookup
     */
    public void processEffects(TFxDuration delta, Buffer buffer, Rect area, ElementRegistry registry) {
        // Global effects (no element lookup needed)
        globalEffects.processEffects(delta, buffer, area);

        // ID-based effects: look up current area, then process
        Iterator<Map.Entry<String, List<Effect>>> idIter = idEffects.entrySet().iterator();
        while (idIter.hasNext()) {
            Map.Entry<String, List<Effect>> entry = idIter.next();
            Rect elementArea = registry.getArea(entry.getKey());
            if (elementArea == null) continue;  // Element not rendered yet

            processEffectList(entry.getValue(), delta, buffer, elementArea);
            if (entry.getValue().isEmpty()) {
                idIter.remove();
            }
        }

        // Selector-based effects: look up current areas for each instance
        Iterator<Map.Entry<SelectorEffect, List<Effect>>> selectorIter = selectorEffects.entrySet().iterator();
        while (selectorIter.hasNext()) {
            Map.Entry<SelectorEffect, List<Effect>> entry = selectorIter.next();
            List<ElementRegistry.ElementInfo> matches = registry.queryAll(entry.getKey().selector);
            List<Effect> effects = entry.getValue();

            int count = Math.min(matches.size(), effects.size());
            for (int i = 0; i < count; i++) {
                Effect effect = effects.get(i);
                Rect elementArea = matches.get(i).area();
                effect.process(delta, buffer, elementArea);
            }
            // Remove completed effects
            effects.removeIf(Effect::done);

            // Remove empty selector entries
            if (effects.isEmpty()) {
                selectorIter.remove();
            }
        }
    }

    /**
     * Processes a list of effects, removing completed ones.
     */
    private void processEffectList(List<Effect> effects, TFxDuration delta, Buffer buffer, Rect area) {
        Iterator<Effect> iter = effects.iterator();
        while (iter.hasNext()) {
            Effect effect = iter.next();
            effect.process(delta, buffer, area);
            if (effect.done()) {
                iter.remove();
            }
        }
    }

    /**
     * Returns whether any effects are currently running.
     *
     * @return true if effects are active
     */
    public boolean isRunning() {
        return globalEffects.isRunning() ||
               !idEffects.isEmpty() ||
               !pendingSelectors.isEmpty() ||
               !selectorEffects.isEmpty();
    }

    /**
     * Clears all effects (pending, running, and global).
     * <p>
     * Must be called from the render thread.
     */
    public void clear() {
        RenderThread.checkRenderThread();
        idEffects.clear();
        pendingSelectors.clear();
        selectorEffects.clear();
        globalEffects.clear();
    }

    /**
     * Returns the total number of pending effects.
     *
     * @return pending effect count
     */
    public int pendingCount() {
        return pendingSelectors.size();
    }

    /**
     * Returns the number of running effects (global + element-targeted).
     *
     * @return running effect count
     */
    public int runningCount() {
        int count = globalEffects.size();
        for (List<Effect> effects : idEffects.values()) {
            count += effects.size();
        }
        for (List<Effect> effects : selectorEffects.values()) {
            count += effects.size();
        }
        return count;
    }
}
