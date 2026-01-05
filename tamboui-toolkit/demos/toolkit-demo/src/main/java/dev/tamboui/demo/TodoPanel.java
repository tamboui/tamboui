/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.style.Color;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;

import java.util.ArrayList;
import java.util.List;

import static dev.tamboui.toolkit.Toolkit.*;

/**
 * A panel with a TODO list and input field.
 */
final class TodoPanel extends PanelContent {
    private final List<String> items = new ArrayList<>();
    private final StringBuilder inputBuffer = new StringBuilder();
    private int selectedIndex = -1;
    private int cursorPosition = 0;
    private boolean cursorVisible = true;

    TodoPanel(String... initialItems) {
        super("[Todo List]", 32, 12, Color.rgb(255, 165, 0));
        items.addAll(List.of(initialItems));
    }

    @Override
    void onTick(long tick) {
        if (tick % 5 == 0) {
            cursorVisible = !cursorVisible;
        }
    }

    @Override
    Element render(boolean focused) {
        var elements = new ArrayList<Element>();

        // Input field with cursor
        var inputText = inputBuffer.toString();
        var displayText = new StringBuilder();

        if (selectedIndex == -1 && focused) {
            var before = inputText.substring(0, cursorPosition);
            var after = inputText.substring(cursorPosition);
            var cursor = cursorVisible ? "│" : " ";
            displayText.append(before).append(cursor).append(after);
            if (displayText.length() < 26) {
                displayText.append("_".repeat(26 - displayText.length()));
            }
        } else {
            displayText.append(inputText);
            if (displayText.length() < 26) {
                displayText.append("_".repeat(26 - displayText.length()));
            }
        }

        var inputStyle = selectedIndex == -1 && focused ? Color.WHITE : Color.GRAY;
        elements.add(row(
            text("> ").fg(selectedIndex == -1 ? Color.GREEN : Color.DARK_GRAY).length(2),
            text(displayText.toString()).fg(inputStyle)
        ));

        elements.add(text("─".repeat(26)).dim());

        // Todo items
        var startIdx = Math.max(0, selectedIndex - 5);
        var endIdx = Math.min(items.size(), startIdx + 6);

        if (items.isEmpty()) {
            elements.add(text("  (no items yet)").dim().italic());
        } else {
            for (var i = startIdx; i < endIdx; i++) {
                var item = items.get(i);
                var isSelected = i == selectedIndex && focused;
                var isDone = item.startsWith("✓ ");

                var prefix = isSelected ? "► " : "  ";
                var itemColor = isDone ? Color.GREEN : (isSelected ? Color.WHITE : Color.GRAY);
                var displayItem = isDone ? item : "○ " + item;
                if (displayItem.length() > 26) {
                    displayItem = displayItem.substring(0, 25) + "…";
                }

                elements.add(text(prefix + displayItem).fg(itemColor));
            }
        }

        elements.add(spacer());
        elements.add(text("[↑↓] [Enter] [Del]").dim());

        return column(elements.toArray(Element[]::new));
    }

    @Override
    EventResult handleKey(KeyEvent event) {
        // Navigation
        if (event.isUp()) {
            if (selectedIndex > -1) {
                selectedIndex--;
            }
            return EventResult.HANDLED;
        }
        if (event.isDown()) {
            if (selectedIndex < items.size() - 1) {
                selectedIndex++;
            }
            return EventResult.HANDLED;
        }

        // Input field selected
        if (selectedIndex == -1) {
            if (event.code() == KeyCode.CHAR) {
                if (inputBuffer.length() < 25) {
                    inputBuffer.insert(cursorPosition, event.character());
                    cursorPosition++;
                }
                return EventResult.HANDLED;
            }
            if (event.code() == KeyCode.BACKSPACE) {
                if (cursorPosition > 0) {
                    inputBuffer.deleteCharAt(cursorPosition - 1);
                    cursorPosition--;
                }
                return EventResult.HANDLED;
            }
            if (event.code() == KeyCode.DELETE) {
                if (cursorPosition < inputBuffer.length()) {
                    inputBuffer.deleteCharAt(cursorPosition);
                }
                return EventResult.HANDLED;
            }
            if (event.code() == KeyCode.LEFT) {
                if (cursorPosition > 0) {
                    cursorPosition--;
                }
                return EventResult.HANDLED;
            }
            if (event.code() == KeyCode.RIGHT) {
                if (cursorPosition < inputBuffer.length()) {
                    cursorPosition++;
                }
                return EventResult.HANDLED;
            }
            if (event.code() == KeyCode.ENTER) {
                var text = inputBuffer.toString().trim();
                if (!text.isEmpty()) {
                    items.add(text);
                    inputBuffer.setLength(0);
                    cursorPosition = 0;
                }
                return EventResult.HANDLED;
            }
        } else {
            // Item selected
            if (event.code() == KeyCode.ENTER) {
                var item = items.get(selectedIndex);
                if (item.startsWith("✓ ")) {
                    items.set(selectedIndex, item.substring(2));
                } else {
                    items.set(selectedIndex, "✓ " + item);
                }
                return EventResult.HANDLED;
            }
            if (event.code() == KeyCode.DELETE || event.code() == KeyCode.BACKSPACE) {
                items.remove(selectedIndex);
                if (selectedIndex >= items.size()) {
                    selectedIndex = items.size() - 1;
                }
                return EventResult.HANDLED;
            }
        }

        return EventResult.UNHANDLED;
    }
}
