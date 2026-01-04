/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.terminal.Frame;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import static dev.tamboui.toolkit.Toolkit.panel;

/**
 * Custom element that renders and manages floating panels.
 */
final class FloatingPanelsArea implements Element {

    private final AtomicLong tickCount = new AtomicLong(0);
    private final List<FloatingPanel> panels = new ArrayList<>();
    private final Random random = new Random();
    private int nextPanelId = 1;

    FloatingPanelsArea() {
        createInitialPanels();
    }

    private void createInitialPanels() {
        panels.add(new FloatingPanel(nextPanelId++, new ClockPanel(), 50, 1));
        panels.add(new FloatingPanel(nextPanelId++, new SystemInfoPanel(this::formatUptime), 2, 1));
        panels.add(new FloatingPanel(nextPanelId++, new TodoPanel("Learn TamboUI DSL", "Build awesome TUI apps", "Share with the world"), 2, 9));
        panels.add(new FloatingPanel(nextPanelId++, new CounterPanel(), 35, 9));
        panels.add(new FloatingPanel(nextPanelId++, new ProgressPanel(), 50, 9));
    }

    private void createPanel(PanelContent content) {
        var x = 5 + random.nextInt(30);
        var y = 3 + random.nextInt(10);
        panels.add(new FloatingPanel(nextPanelId++, content, x, y));
    }

    @Override
    public void render(Frame frame, Rect area, RenderContext context) {
        var tick = tickCount.incrementAndGet();
        for (var fp : panels) {
            fp.content.onTick(tick);
        }

        for (var fp : panels) {
            renderFloatingPanel(frame, area, context, fp);
        }
    }

    private void renderFloatingPanel(Frame frame, Rect area, RenderContext context, FloatingPanel fp) {
        var content = fp.content;
        var relX = Math.max(0, Math.min(fp.x, area.width() - content.width()));
        var relY = Math.max(0, Math.min(fp.y, area.height() - content.height()));
        var panelArea = new Rect(area.x() + relX, area.y() + relY, content.width(), content.height());

        var focused = context.isFocused(fp.panelId());
        var borderColor = focused ? Color.WHITE : content.color();

        var p = panel(content.title(), () -> content.render(focused))
                .id(fp.panelId())
                .rounded()
                .borderColor(borderColor)
                .focusedBorderColor(Color.WHITE)
                .focusable()
                .onKeyEvent(event -> handlePanelKey(fp, event))
                .draggable((deltaX, deltaY) -> {
                    fp.x += deltaX;
                    fp.y += deltaY;
                });

        p.render(frame, panelArea, context);
    }

    private EventResult handlePanelKey(FloatingPanel fp, KeyEvent event) {
        var result = fp.content.handleKey(event);
        if (result.isHandled()) {
            return result;
        }

        // Handle arrow keys to move the panel
        int moveStep = event.hasShift() ? 5 : 1; // Shift+Arrow moves faster
        if (event.code() == KeyCode.UP) {
            fp.y = Math.max(0, fp.y - moveStep);
            return EventResult.HANDLED;
        }
        if (event.code() == KeyCode.DOWN) {
            fp.y += moveStep;
            return EventResult.HANDLED;
        }
        if (event.code() == KeyCode.LEFT) {
            fp.x = Math.max(0, fp.x - moveStep);
            return EventResult.HANDLED;
        }
        if (event.code() == KeyCode.RIGHT) {
            fp.x += moveStep;
            return EventResult.HANDLED;
        }

        if (event.isCharIgnoreCase('x')) {
            panels.removeIf(p -> p.id == fp.id);
            return EventResult.HANDLED;
        }

        return EventResult.UNHANDLED;
    }

    @Override
    public Constraint constraint() {
        return Constraint.fill();
    }

    @Override
    public EventResult handleKeyEvent(KeyEvent event, boolean focused) {
        if (event.code() == KeyCode.CHAR) {
            switch (event.character()) {
                case '1': createPanel(new ClockPanel()); return EventResult.HANDLED;
                case '2': createPanel(new CounterPanel()); return EventResult.HANDLED;
                case '3': createPanel(new SystemInfoPanel(this::formatUptime)); return EventResult.HANDLED;
                case '4': createPanel(new QuotePanel()); return EventResult.HANDLED;
                case '5': createPanel(new ProgressPanel()); return EventResult.HANDLED;
                case '6': createPanel(new TodoPanel()); return EventResult.HANDLED;
            }
        }
        return EventResult.UNHANDLED;
    }

    private String formatUptime() {
        var ticks = tickCount.get();
        var seconds = ticks / 10;
        var minutes = seconds / 60;
        var hours = minutes / 60;
        return String.format("%02d:%02d:%02d", hours, minutes % 60, seconds % 60);
    }
}
