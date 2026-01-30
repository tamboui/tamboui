/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tfx.toolkit;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Style;
import dev.tamboui.style.StyledAreaRegistry;
import dev.tamboui.style.Tags;
import dev.tamboui.tfx.Effect;
import dev.tamboui.tfx.Shader;
import dev.tamboui.tfx.TFxDuration;
import dev.tamboui.toolkit.element.ElementRegistry;
import dev.tamboui.toolkit.focus.FocusManager;

import static org.assertj.core.api.Assertions.assertThat;

class ElementEffectRegistryTest {

    private ElementEffectRegistry effectRegistry;
    private ElementRegistry elementRegistry;
    private StyledAreaRegistry styledAreaRegistry;
    private FocusManager focusManager;
    private Buffer buffer;

    @BeforeEach
    void setUp() {
        effectRegistry = new ElementEffectRegistry();
        elementRegistry = new ElementRegistry();
        styledAreaRegistry = StyledAreaRegistry.create();
        focusManager = new FocusManager();
        buffer = Buffer.empty(new Rect(0, 0, 80, 24));
    }

    @Test
    @DisplayName("styled area effects follow regions after simulated resize")
    void styledAreaEffectsFollowRegionsAfterResize() {
        // Track areas that the effect was processed with
        List<Rect> processedAreas = new ArrayList<>();
        Effect trackingEffect = Effect.of(new AreaTrackingShader(processedAreas));

        // Register a styled area at initial position
        Style taggedStyle = Style.EMPTY.withExtension(Tags.class, Tags.of("highlight"));
        Rect initialArea = new Rect(10, 5, 20, 1);
        styledAreaRegistry.register(taggedStyle, initialArea, null);

        // Add effect targeting .highlight
        effectRegistry.addEffectBySelector(".highlight", trackingEffect);

        // Expand selectors (captures initial matches)
        effectRegistry.expandSelectors(elementRegistry, styledAreaRegistry);

        // Process once at initial position
        TFxDuration delta = TFxDuration.fromMillis(16);
        effectRegistry.processEffects(delta, buffer, buffer.area(), elementRegistry,
                styledAreaRegistry, focusManager);

        assertThat(processedAreas).hasSize(1);
        assertThat(processedAreas.get(0)).isEqualTo(initialArea);

        // Simulate resize: clear and re-register at NEW position
        styledAreaRegistry.clear();
        Rect newArea = new Rect(15, 8, 25, 1); // Different position and size
        styledAreaRegistry.register(taggedStyle, newArea, null);

        // Process again - should use NEW area
        processedAreas.clear();
        effectRegistry.processEffects(delta, buffer, buffer.area(), elementRegistry,
                styledAreaRegistry, focusManager);

        assertThat(processedAreas).hasSize(1);
        assertThat(processedAreas.get(0)).isEqualTo(newArea);
    }

    @Test
    @DisplayName("styled area effects handle region disappearing after resize")
    void styledAreaEffectsHandleRegionDisappearing() {
        List<Rect> processedAreas = new ArrayList<>();
        Effect trackingEffect = Effect.of(new AreaTrackingShader(processedAreas));

        // Register a styled area
        Style taggedStyle = Style.EMPTY.withExtension(Tags.class, Tags.of("highlight"));
        styledAreaRegistry.register(taggedStyle, new Rect(10, 5, 20, 1), null);

        // Add and expand effect
        effectRegistry.addEffectBySelector(".highlight", trackingEffect);
        effectRegistry.expandSelectors(elementRegistry, styledAreaRegistry);

        // Process once
        TFxDuration delta = TFxDuration.fromMillis(16);
        effectRegistry.processEffects(delta, buffer, buffer.area(), elementRegistry,
                styledAreaRegistry, focusManager);
        assertThat(processedAreas).hasSize(1);

        // Simulate resize where the region no longer exists
        styledAreaRegistry.clear();
        // Don't re-register the highlight

        // Process again - should not process any effect (no matching areas)
        processedAreas.clear();
        effectRegistry.processEffects(delta, buffer, buffer.area(), elementRegistry,
                styledAreaRegistry, focusManager);

        assertThat(processedAreas).isEmpty();
    }

    /**
     * A shader that tracks the areas it was processed with.
     */
    private static class AreaTrackingShader implements Shader {
        private final List<Rect> processedAreas;
        private Rect area;

        AreaTrackingShader(List<Rect> processedAreas) {
            this.processedAreas = processedAreas;
        }

        @Override
        public TFxDuration process(TFxDuration duration, Buffer buffer, Rect area) {
            processedAreas.add(area);
            return null; // Still running
        }

        @Override
        public boolean done() {
            return false;
        }

        @Override
        public boolean running() {
            return true;
        }

        @Override
        public String name() {
            return "AreaTrackingShader";
        }

        @Override
        public Shader copy() {
            AreaTrackingShader copy = new AreaTrackingShader(processedAreas);
            copy.area = this.area;
            return copy;
        }

        @Override
        public Rect area() {
            return area;
        }

        @Override
        public void setArea(Rect area) {
            this.area = area;
        }

        @Override
        public void setCellFilter(dev.tamboui.tfx.CellFilter filter) {
        }

        @Override
        public void setColorSpace(dev.tamboui.tfx.TFxColorSpace colorSpace) {
        }

        @Override
        public void setPattern(dev.tamboui.tfx.pattern.Pattern pattern) {
        }

        @Override
        public void reverse() {
        }

        @Override
        public void setLoopMode(dev.tamboui.tfx.LoopMode loopMode) {
        }
    }
}
