/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tfx.toolkit;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.css.Styleable;
import dev.tamboui.css.cascade.PseudoClassState;
import dev.tamboui.css.cascade.PseudoClassStateProvider;
import dev.tamboui.css.selector.Selector;
import dev.tamboui.css.selector.SelectorParser;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.StyledAreaInfo;
import dev.tamboui.style.StyledAreaRegistry;
import dev.tamboui.tfx.Effect;
import dev.tamboui.tfx.EffectManager;
import dev.tamboui.tfx.TFxDuration;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.ElementRegistry;
import dev.tamboui.toolkit.element.StyledSpan;
import dev.tamboui.toolkit.focus.FocusManager;
import dev.tamboui.tui.RenderThread;

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
 * <b>Supported Pseudo-Classes:</b>
 * <ul>
 *   <li>{@code :focus} - Matches elements that have focus (via FocusManager)</li>
 * </ul>
 * <p>
 * <b>Limitations - Pseudo-classes NOT supported for effects:</b>
 * <ul>
 *   <li>{@code :hover} - No mouse hover tracking in TUI</li>
 *   <li>{@code :disabled}, {@code :selected}, {@code :active} - Element state not tracked post-render</li>
 *   <li>{@code :first-child}, {@code :last-child}, {@code :nth-child()} - Sibling position not tracked</li>
 * </ul>
 * <p>
 * Note: These pseudo-classes work for CSS styling during render (via {@code childStyle()})
 * but are not available for effect targeting because element state and position are not
 * preserved in the ElementRegistry after rendering.
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

    // Styled area effects: keep selector→effects mapping (areas re-queried each frame)
    private final Map<SelectorEffect, List<Effect>> styledAreaEffects = new LinkedHashMap<>();

    // Global effects don't need element lookup
    private final EffectManager globalEffects = new EffectManager();

    /**
     * A pending effect targeting elements matching a CSS selector.
     */
    private static final class SelectorEffect {
        final String selectorStr;
        final Selector selector; // cached parsed selector
        final Effect effect;

        SelectorEffect(String selectorStr, Effect effect) {
            this.selectorStr = selectorStr;
            this.selector = SelectorParser.parse(selectorStr);
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
     * {@link #processEffects(TFxDuration, Buffer, Rect, ElementRegistry, StyledAreaRegistry, FocusManager)}.
     *
     * @param registry the element registry containing element areas
     */
    public void expandSelectors(ElementRegistry registry) {
        expandSelectors(registry, null);
    }

    /**
     * Expands pending selector-based effects to individual effect instances.
     * <p>
     * This method queries both the ElementRegistry (for elements) and
     * StyledAreaRegistry (for styled spans) to find all targets matching each
     * pending selector. Effect copies are created for each match and tracked
     * for dynamic area lookup.
     * <p>
     * This enables selectors like:
     * <ul>
     *   <li>{@code .highlight} - matches both elements and spans with class "highlight"</li>
     *   <li>{@code #myPanel .highlight} - matches spans inside element #myPanel</li>
     *   <li>{@code Span.error} - matches only styled spans (type "Span")</li>
     * </ul>
     *
     * @param elementRegistry    the element registry containing element areas
     * @param styledAreaRegistry the styled area registry (may be null)
     */
    public void expandSelectors(ElementRegistry elementRegistry, StyledAreaRegistry styledAreaRegistry) {
        RenderThread.checkRenderThread();
        for (SelectorEffect se : pendingSelectors) {
            // Query elements (uses string selector for ElementRegistry API)
            List<ElementRegistry.ElementInfo> elementMatches = elementRegistry.queryAll(se.selectorStr);
            List<Effect> effects = new ArrayList<>();
            for (int i = 0; i < elementMatches.size(); i++) {
                effects.add(se.effect.copy());
            }
            if (!effects.isEmpty()) {
                selectorEffects.put(se, effects);
            }

            // Query styled areas ignoring pseudo-class state to capture potential targets.
            // Actual pseudo-class state is evaluated during processEffects.
            if (styledAreaRegistry != null) {
                List<StyledSpan> styledMatches = queryStyledAreas(se.selector, styledAreaRegistry, elementRegistry, e -> PseudoClassState.allMatch());
                if (!styledMatches.isEmpty()) {
                    List<Effect> styledEffects = new ArrayList<>();
                    for (int i = 0; i < styledMatches.size(); i++) {
                        styledEffects.add(se.effect.copy());
                    }
                    styledAreaEffects.put(se, styledEffects);
                }
            }
        }
        pendingSelectors.clear();
    }

    /**
     * Queries styled areas matching a selector with the given state provider.
     */
    private List<StyledSpan> queryStyledAreas(Selector selector, StyledAreaRegistry styledAreaRegistry,
                                               ElementRegistry elementRegistry, PseudoClassStateProvider stateProvider) {
        List<StyledSpan> matches = new ArrayList<>();
        for (StyledAreaInfo info : styledAreaRegistry.all()) {
            StyledSpan styleable = new StyledSpan(info, elementRegistry);
            List<Styleable> ancestors = buildAncestorChain(styleable);
            if (selector.matches(styleable, stateProvider, ancestors)) {
                matches.add(styleable);
            }
        }
        return matches;
    }

    /**
     * Creates a state provider that computes focus state for any element.
     */
    private PseudoClassStateProvider createStateProvider(String focusedId) {
        if (focusedId == null) {
            return element -> PseudoClassState.NONE;
        }
        return element -> {
            boolean isFocused = element.cssId().map(id -> id.equals(focusedId)).orElse(false);
            return isFocused ? PseudoClassState.ofFocused() : PseudoClassState.NONE;
        };
    }

    /**
     * Builds the ancestor chain for a styled area for selector matching.
     */
    private List<Styleable> buildAncestorChain(StyledSpan styleable) {
        List<Styleable> ancestors = new ArrayList<>();
        styleable.cssParent().ifPresent(parent -> {
            // Add parent and its ancestors
            if (parent instanceof ElementRegistry.ElementInfo) {
                ElementRegistry.ElementInfo elementInfo = (ElementRegistry.ElementInfo) parent;
                ancestors.addAll(elementInfo.ancestors());
            }
            ancestors.add(parent);
        });
        return ancestors;
    }

    /**
     * Processes all active effects with dynamic area lookup from both registries.
     * <p>
     * This processes global effects, ID-based effects, selector-based element effects,
     * and selector-based styled area effects. Areas are looked up dynamically each
     * frame, so effects automatically follow their targets when they move or resize.
     * <p>
     * Pseudo-class selectors like {@code :focus} are supported via the FocusManager.
     * <p>
     * Effects are automatically removed when complete.
     *
     * @param delta              the time elapsed since the last frame
     * @param buffer             the buffer to apply effects to
     * @param area               the default area for global effects
     * @param elementRegistry    the element registry for element area lookup
     * @param styledAreaRegistry the styled area registry
     * @param focusManager       the focus manager for pseudo-class state
     */
    public void processEffects(TFxDuration delta, Buffer buffer, Rect area,
                               ElementRegistry elementRegistry, StyledAreaRegistry styledAreaRegistry,
                               FocusManager focusManager) {
        // Global effects (no element lookup needed)
        globalEffects.processEffects(delta, buffer, area);

        // ID-based effects: look up current area, then process
        Iterator<Map.Entry<String, List<Effect>>> idIter = idEffects.entrySet().iterator();
        while (idIter.hasNext()) {
            Map.Entry<String, List<Effect>> entry = idIter.next();
            Rect elementArea = elementRegistry.getArea(entry.getKey());
            if (elementArea == null) {
                continue;  // Element not rendered yet
            }

            processEffectList(entry.getValue(), delta, buffer, elementArea);
            if (entry.getValue().isEmpty()) {
                idIter.remove();
            }
        }

        // Selector-based element effects: look up current areas for each instance
        Iterator<Map.Entry<SelectorEffect, List<Effect>>> selectorIter = selectorEffects.entrySet().iterator();
        while (selectorIter.hasNext()) {
            Map.Entry<SelectorEffect, List<Effect>> entry = selectorIter.next();
            List<ElementRegistry.ElementInfo> matches = elementRegistry.queryAll(entry.getKey().selectorStr);
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

        // Styled area effects: re-query areas each frame with real pseudo-class state
        if (styledAreaRegistry != null) {
            String focusedId = focusManager != null ? focusManager.focusedId() : null;
            PseudoClassStateProvider stateProvider = createStateProvider(focusedId);

            Iterator<Map.Entry<SelectorEffect, List<Effect>>> styledAreaIter =
                    styledAreaEffects.entrySet().iterator();
            while (styledAreaIter.hasNext()) {
                Map.Entry<SelectorEffect, List<Effect>> entry = styledAreaIter.next();
                List<StyledSpan> matches = queryStyledAreas(entry.getKey().selector, styledAreaRegistry, elementRegistry, stateProvider);
                List<Effect> effects = entry.getValue();

                int count = Math.min(matches.size(), effects.size());
                for (int i = 0; i < count; i++) {
                    Effect effect = effects.get(i);
                    Rect spanArea = matches.get(i).area();
                    effect.process(delta, buffer, spanArea);
                }
                // Remove completed effects
                effects.removeIf(Effect::done);

                // Remove empty selector entries
                if (effects.isEmpty()) {
                    styledAreaIter.remove();
                }
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
               !selectorEffects.isEmpty() ||
               !styledAreaEffects.isEmpty();
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
        styledAreaEffects.clear();
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
     * Returns the number of running effects (global + element-targeted + styled-area-targeted).
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
        for (List<Effect> effects : styledAreaEffects.values()) {
            count += effects.size();
        }
        return count;
    }
}
