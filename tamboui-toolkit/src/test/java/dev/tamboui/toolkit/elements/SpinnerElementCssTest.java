/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.css.engine.StyleEngine;
import dev.tamboui.layout.Rect;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.element.DefaultRenderContext;
import dev.tamboui.widgets.spinner.SpinnerFrameSet;
import dev.tamboui.widgets.spinner.SpinnerStyle;

import static dev.tamboui.toolkit.Toolkit.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests CSS property resolution for SpinnerElement.
 */
class SpinnerElementCssTest {

    private DefaultRenderContext context;
    private StyleEngine styleEngine;

    @BeforeEach
    void setUp() {
        context = DefaultRenderContext.createEmpty();
        styleEngine = StyleEngine.create();
        context.setStyleEngine(styleEngine);
    }

    @Test
    @DisplayName("CSS spinner-style property configures spinner style")
    void cssSpinnerStyleProperty() {
        String css = ".test-spinner { spinner-style: line; }";
        styleEngine.addStylesheet("test", css);
        styleEngine.setActiveStylesheet("test");

        Rect area = new Rect(0, 0, 20, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        SpinnerElement spinner = spinner().addClass("test-spinner");
        spinner.render(frame, area, context);

        // LINE style frames are: "-", "\", "|", "/"
        // State advances during render, so first render shows frame 1 which is "\"
        String firstChar = buffer.get(0, 0).symbol();
        assertThat(firstChar).isEqualTo("\\");
    }

    @Test
    @DisplayName("CSS spinner-frames property configures custom frames")
    void cssSpinnerFramesProperty() {
        String css = ".custom-spinner { spinner-frames: \"*\" \"+\" \"x\" \"+\"; }";
        styleEngine.addStylesheet("test", css);
        styleEngine.setActiveStylesheet("test");

        Rect area = new Rect(0, 0, 20, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        SpinnerElement spinner = spinner().addClass("custom-spinner");
        spinner.render(frame, area, context);

        // Custom frames: "*", "+", "x", "+"
        // State advances during render, so first render shows frame 1 which is "+"
        String firstChar = buffer.get(0, 0).symbol();
        assertThat(firstChar).isEqualTo("+");
    }

    @Test
    @DisplayName("CSS spinner-frames overrides spinner-style")
    void cssSpinnerFramesOverridesStyle() {
        String css = ".spinner { spinner-style: line; spinner-frames: \"#\" \"@\"; }";
        styleEngine.addStylesheet("test", css);
        styleEngine.setActiveStylesheet("test");

        Rect area = new Rect(0, 0, 20, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        SpinnerElement spinner = spinner().addClass("spinner");
        spinner.render(frame, area, context);

        // spinner-frames takes precedence: "#", "@"
        // State advances during render, so first render shows frame 1 which is "@"
        String firstChar = buffer.get(0, 0).symbol();
        assertThat(firstChar).isEqualTo("@");
    }

    @Test
    @DisplayName("explicit spinnerStyle overrides CSS")
    void explicitSpinnerStyleOverridesCss() {
        String css = ".spinner { spinner-style: line; }";
        styleEngine.addStylesheet("test", css);
        styleEngine.setActiveStylesheet("test");

        Rect area = new Rect(0, 0, 20, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        // Set explicit ARC style which has frames: "◜", "◝", "◞", "◟"
        SpinnerElement spinner = spinner()
                .addClass("spinner")
                .spinnerStyle(SpinnerStyle.ARC);
        spinner.render(frame, area, context);

        // State advances during render, so first render shows frame 1 which is "◝"
        String firstChar = buffer.get(0, 0).symbol();
        assertThat(firstChar).isEqualTo("\u25dd"); // ◝
    }

    @Test
    @DisplayName("explicit frameSet overrides CSS")
    void explicitFrameSetOverridesCss() {
        String css = ".spinner { spinner-frames: \"X\" \"Y\"; }";
        styleEngine.addStylesheet("test", css);
        styleEngine.setActiveStylesheet("test");

        Rect area = new Rect(0, 0, 20, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        // Set explicit frame set: "A", "B", "C"
        SpinnerElement spinner = spinner()
                .addClass("spinner")
                .frameSet(SpinnerFrameSet.of("A", "B", "C"));
        spinner.render(frame, area, context);

        // State advances during render, so first render shows frame 1 which is "B"
        String firstChar = buffer.get(0, 0).symbol();
        assertThat(firstChar).isEqualTo("B");
    }

    @Test
    @DisplayName("explicit frames method overrides CSS")
    void explicitFramesOverridesCss() {
        String css = ".spinner { spinner-style: gauge; }";
        styleEngine.addStylesheet("test", css);
        styleEngine.setActiveStylesheet("test");

        Rect area = new Rect(0, 0, 20, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        // Set explicit frames via varargs: "!", "?", "&"
        SpinnerElement spinner = spinner()
                .addClass("spinner")
                .frames("!", "?", "&");
        spinner.render(frame, area, context);

        // State advances during render, so first render shows frame 1 which is "?"
        String firstChar = buffer.get(0, 0).symbol();
        assertThat(firstChar).isEqualTo("?");
    }

    @Test
    @DisplayName("default style used when no CSS or explicit style set")
    void defaultStyleWhenNoCssOrExplicit() {
        // No CSS stylesheet set
        Rect area = new Rect(0, 0, 20, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        SpinnerElement spinner = spinner();
        spinner.render(frame, area, context);

        // Default is DOTS style
        // State advances during render, so first render shows frame 1 which is "⠁"
        String firstChar = buffer.get(0, 0).symbol();
        assertThat(firstChar).isEqualTo("\u2801"); // ⠁
    }

    @Test
    @DisplayName("CSS gauge style works correctly")
    void cssGaugeStyle() {
        String css = ".gauge-spinner { spinner-style: gauge; }";
        styleEngine.addStylesheet("test", css);
        styleEngine.setActiveStylesheet("test");

        Rect area = new Rect(0, 0, 20, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        SpinnerElement spinner = spinner().addClass("gauge-spinner");
        spinner.render(frame, area, context);

        // GAUGE style frames: "▏", "▎", "▍", "▌", "▋", "▊", "▉", "█"
        // State advances during render, so first render shows frame 1 which is "▎"
        String firstChar = buffer.get(0, 0).symbol();
        assertThat(firstChar).isEqualTo("\u258e"); // ▎
    }

    @Test
    @DisplayName("CSS bouncing-bar style works correctly")
    void cssBouncingBarStyle() {
        String css = ".bar-spinner { spinner-style: bouncing-bar; }";
        styleEngine.addStylesheet("test", css);
        styleEngine.setActiveStylesheet("test");

        Rect area = new Rect(0, 0, 20, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        SpinnerElement spinner = spinner().addClass("bar-spinner");
        spinner.render(frame, area, context);

        // BOUNCING_BAR frames start with "[    ]", "[=   ]", ...
        // State advances during render, so first render shows frame 1 which starts with "["
        // Second frame is "[=   ]"
        String cell0 = buffer.get(0, 0).symbol();
        assertThat(cell0).isEqualTo("[");
    }
}
