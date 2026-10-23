/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.AbstractElementTest;
import dev.tamboui.toolkit.element.DefaultRenderContext;
import dev.tamboui.widgets.toast.ToastBuilder;
import dev.tamboui.widgets.toast.ToastEngine;

import static dev.tamboui.toolkit.Toolkit.toast;
import static org.assertj.core.api.Assertions.assertThat;

class ToastHostElementCssTest extends AbstractElementTest {

    private DefaultRenderContext context;

    @BeforeEach
    void setUp() {
        context = DefaultRenderContext.createEmpty();
        context.setStyleEngine(dev.tamboui.css.engine.StyleEngine.create());
    }

    @Test
    @DisplayName("Toast child selectors style info title and progress")
    void cssStylesToastChildren() {
        context.styleEngine().get().addStylesheet("test",
                ".toast-host Toast-info { color: magenta; }\n"
                        + ".toast-host Toast-title { color: cyan; }\n"
                        + ".toast-host Toast-progress { color: green; }");
        context.styleEngine().get().setActiveStylesheet("test");

        ToastEngine engine = ToastEngine.builder().build();
        engine.show(ToastBuilder.info("Body")
                .title("Header")
                .duration(Duration.ofSeconds(5))
                .build());
        ToastHostElement host = toast(engine).addClass("toast-host");

        Rect area = new Rect(0, 0, 60, 12);
        Buffer buffer = Buffer.empty(area);
        host.render(Frame.forTesting(buffer), area, context);
        Rect toastRect = engine.lastRenderedRects().get(0);

        assertThat(buffer.get(toastRect.left(), toastRect.top()).style().fg()).contains(Color.MAGENTA);
        assertThat(findFgColorForSymbol(buffer, toastRect, "H")).contains(Color.CYAN);
        assertThat(findAnyCellWithFg(buffer, toastRect, Color.GREEN)).isTrue();
    }

    private static java.util.Optional<Color> findFgColorForSymbol(Buffer buffer, Rect area, String symbol) {
        for (int y = area.top(); y < area.bottom(); y++) {
            for (int x = area.left(); x < area.right(); x++) {
                if (symbol.equals(buffer.get(x, y).symbol())) {
                    return buffer.get(x, y).style().fg();
                }
            }
        }
        return java.util.Optional.empty();
    }

    private static boolean findAnyCellWithFg(Buffer buffer, Rect area, Color color) {
        for (int y = area.top(); y < area.bottom(); y++) {
            for (int x = area.left(); x < area.right(); x++) {
                if (buffer.get(x, y).style().fg().filter(color::equals).isPresent()) {
                    return true;
                }
            }
        }
        return false;
    }
}
