/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import java.util.Objects;

import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.element.Size;
import dev.tamboui.toolkit.element.StyledElement;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.MouseEvent;
import dev.tamboui.widgets.toast.ToastEngine;
import dev.tamboui.widgets.toast.ToastInteraction;

/**
 * Leaf toolkit element that renders and routes interactions for a {@link ToastEngine}.
 *
 * <h2>CSS Child Selectors</h2>
 * <ul>
 * <li>{@code Toast-info} - Info toast accent style (default: blue)</li>
 * <li>{@code Toast-success} - Success toast accent style (default: green)</li>
 * <li>{@code Toast-warning} - Warning toast accent style (default: yellow)</li>
 * <li>{@code Toast-error} - Error toast accent style (default: red)</li>
 * <li>{@code Toast-title} - Toast title style</li>
 * <li>{@code Toast-progress} - Toast progress style</li>
 * </ul>
 */
public final class ToastHostElement extends StyledElement<ToastHostElement> {

    private final ToastEngine engine;

    /**
     * Creates a toast host bound to the provided engine.
     *
     * @param engine toast engine instance
     */
    public ToastHostElement(ToastEngine engine) {
        this.engine = Objects.requireNonNull(engine, "engine");
    }

    /**
     * Returns the toast engine rendered by this host.
     *
     * @return the toast engine
     */
    public ToastEngine engine() {
        return engine;
    }

    @Override
    public String styleType() {
        return "Toast";
    }

    @Override
    protected void renderContent(Frame frame, Rect area, RenderContext context) {
        ToastEngine.RenderStyles styles = ToastEngine.RenderStyles.of(
                resolveEffectiveStyle(context, "info", null, Style.EMPTY.fg(Color.BLUE)),
                resolveEffectiveStyle(context, "success", null, Style.EMPTY.fg(Color.GREEN)),
                resolveEffectiveStyle(context, "warning", null, Style.EMPTY.fg(Color.YELLOW)),
                resolveEffectiveStyle(context, "error", null, Style.EMPTY.fg(Color.RED)),
                resolveEffectiveStyle(context, "title", null, Style.EMPTY),
                resolveEffectiveStyle(context, "progress", null, Style.EMPTY));
        engine.render(frame, area, styles);
    }

    @Override
    public Size preferredSize(int availableWidth, int availableHeight, RenderContext context) {
        return Size.UNKNOWN;
    }

    @Override
    public EventResult handleMouseEvent(MouseEvent event) {
        int before = engine.visibleCount();
        ToastInteraction interaction = engine.interact(event);
        return toEventResult(interaction, before);
    }

    @Override
    public EventResult handleKeyEvent(KeyEvent event, boolean focused) {
        int before = engine.visibleCount();
        ToastInteraction interaction = engine.interact(event);
        return toEventResult(interaction, before);
    }

    private EventResult toEventResult(ToastInteraction interaction, int visibleBefore) {
        if (interaction != ToastInteraction.NONE || engine.visibleCount() != visibleBefore) {
            return EventResult.HANDLED;
        }
        return EventResult.UNHANDLED;
    }
}
