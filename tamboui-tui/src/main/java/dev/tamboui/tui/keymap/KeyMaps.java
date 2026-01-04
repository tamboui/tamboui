/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.keymap;

import dev.tamboui.tui.event.KeyCode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static dev.tamboui.tui.keymap.KeyBinding.alt;
import static dev.tamboui.tui.keymap.KeyBinding.ch;
import static dev.tamboui.tui.keymap.KeyBinding.chIgnoreCase;
import static dev.tamboui.tui.keymap.KeyBinding.ctrl;
import static dev.tamboui.tui.keymap.KeyBinding.key;

/**
 * Factory for predefined keymaps.
 * <p>
 * Available keymaps:
 * <ul>
 *   <li>{@link #standard()} - Arrow keys only, no vim/emacs bindings (default)</li>
 *   <li>{@link #vim()} - Vim-style navigation (hjkl, g/G, Ctrl+u/d)</li>
 *   <li>{@link #emacs()} - Emacs-style navigation (Ctrl+n/p/f/b, etc.)</li>
 *   <li>{@link #intellij()} - IntelliJ IDEA-style bindings</li>
 *   <li>{@link #vscode()} - Visual Studio Code-style bindings</li>
 * </ul>
 *
 * <pre>{@code
 * // Use a predefined keymap
 * KeyMap keymap = KeyMaps.vim();
 *
 * // Customize a predefined keymap
 * KeyMap custom = KeyMaps.standard()
 *     .toBuilder()
 *     .bind(Action.QUIT, KeyBinding.ch('x'))
 *     .build();
 * }</pre>
 */
public final class KeyMaps {

    private static final KeyMap STANDARD = createStandard();
    private static final KeyMap VIM = createVim();
    private static final KeyMap EMACS = createEmacs();
    private static final KeyMap INTELLIJ = createIntelliJ();
    private static final KeyMap VSCODE = createVSCode();

    private KeyMaps() {
    }

    /**
     * Standard keymap using only arrow keys and standard keys.
     * <p>
     * No vim or emacs-style bindings. This is the default keymap,
     * safe for use with text input (no letter keys bound to navigation).
     *
     * @return the standard keymap
     */
    public static KeyMap standard() {
        return STANDARD;
    }

    /**
     * Vim-style keymap with hjkl navigation.
     * <p>
     * Includes:
     * <ul>
     *   <li>hjkl for directional navigation</li>
     *   <li>g/G for home/end</li>
     *   <li>Ctrl+u/d for page up/down</li>
     * </ul>
     *
     * @return the vim keymap
     */
    public static KeyMap vim() {
        return VIM;
    }

    /**
     * Emacs-style keymap with Ctrl+n/p/f/b navigation.
     * <p>
     * Includes:
     * <ul>
     *   <li>Ctrl+n/p/f/b for directional navigation</li>
     *   <li>Alt+v/Ctrl+v for page up/down</li>
     *   <li>Ctrl+a/e for home/end (line-level)</li>
     *   <li>Ctrl+g as cancel</li>
     * </ul>
     *
     * @return the emacs keymap
     */
    public static KeyMap emacs() {
        return EMACS;
    }

    /**
     * IntelliJ IDEA-style keymap.
     * <p>
     * Based on IntelliJ's default key bindings for navigation.
     *
     * @return the IntelliJ keymap
     */
    public static KeyMap intellij() {
        return INTELLIJ;
    }

    /**
     * Visual Studio Code-style keymap.
     * <p>
     * Based on VS Code's default key bindings for navigation.
     *
     * @return the VS Code keymap
     */
    public static KeyMap vscode() {
        return VSCODE;
    }

    /**
     * Returns the default keymap.
     * <p>
     * Currently returns {@link #standard()}.
     *
     * @return the default keymap
     */
    public static KeyMap defaults() {
        return STANDARD;
    }

    private static KeyMap createStandard() {
        return DefaultKeyMap.builder()
            // Navigation - arrow keys only
            .bind(Action.MOVE_UP, key(KeyCode.UP))
            .bind(Action.MOVE_DOWN, key(KeyCode.DOWN))
            .bind(Action.MOVE_LEFT, key(KeyCode.LEFT))
            .bind(Action.MOVE_RIGHT, key(KeyCode.RIGHT))

            // Page navigation
            .bind(Action.PAGE_UP, key(KeyCode.PAGE_UP))
            .bind(Action.PAGE_DOWN, key(KeyCode.PAGE_DOWN))
            .bind(Action.HOME, key(KeyCode.HOME))
            .bind(Action.END, key(KeyCode.END))

            // Selection
            .bind(Action.SELECT, key(KeyCode.ENTER), ch(' '))
            .bind(Action.CONFIRM, key(KeyCode.ENTER))
            .bind(Action.CANCEL, key(KeyCode.ESCAPE))

            // Focus
            .bind(Action.FOCUS_NEXT, key(KeyCode.TAB))
            .bind(Action.FOCUS_PREVIOUS, key(KeyCode.TAB, false, false, true)) // Shift+Tab

            // Editing
            .bind(Action.DELETE_BACKWARD, key(KeyCode.BACKSPACE))
            .bind(Action.DELETE_FORWARD, key(KeyCode.DELETE))

            // Application
            .bind(Action.QUIT, chIgnoreCase('q'), ctrl('c'))
            .build();
    }

    private static KeyMap createVim() {
        return DefaultKeyMap.builder()
            // Navigation - vim style + arrows
            .bind(Action.MOVE_UP, key(KeyCode.UP), chIgnoreCase('k'))
            .bind(Action.MOVE_DOWN, key(KeyCode.DOWN), chIgnoreCase('j'))
            .bind(Action.MOVE_LEFT, key(KeyCode.LEFT), chIgnoreCase('h'))
            .bind(Action.MOVE_RIGHT, key(KeyCode.RIGHT), chIgnoreCase('l'))

            // Page navigation - vim style
            .bind(Action.PAGE_UP, key(KeyCode.PAGE_UP), ctrl('u'))
            .bind(Action.PAGE_DOWN, key(KeyCode.PAGE_DOWN), ctrl('d'))
            .bind(Action.HOME, key(KeyCode.HOME), ch('g'))
            .bind(Action.END, key(KeyCode.END), ch('G'))

            // Selection
            .bind(Action.SELECT, key(KeyCode.ENTER), ch(' '))
            .bind(Action.CONFIRM, key(KeyCode.ENTER))
            .bind(Action.CANCEL, key(KeyCode.ESCAPE))

            // Focus
            .bind(Action.FOCUS_NEXT, key(KeyCode.TAB))
            .bind(Action.FOCUS_PREVIOUS, key(KeyCode.TAB, false, false, true))

            // Editing
            .bind(Action.DELETE_BACKWARD, key(KeyCode.BACKSPACE))
            .bind(Action.DELETE_FORWARD, key(KeyCode.DELETE), ch('x'))

            // Application
            .bind(Action.QUIT, chIgnoreCase('q'), ctrl('c'))
            .build();
    }

    private static KeyMap createEmacs() {
        return DefaultKeyMap.builder()
            // Navigation - emacs style + arrows
            .bind(Action.MOVE_UP, key(KeyCode.UP), ctrl('p'))
            .bind(Action.MOVE_DOWN, key(KeyCode.DOWN), ctrl('n'))
            .bind(Action.MOVE_LEFT, key(KeyCode.LEFT), ctrl('b'))
            .bind(Action.MOVE_RIGHT, key(KeyCode.RIGHT), ctrl('f'))

            // Page navigation - emacs style
            .bind(Action.PAGE_UP, key(KeyCode.PAGE_UP), alt('v'))
            .bind(Action.PAGE_DOWN, key(KeyCode.PAGE_DOWN), ctrl('v'))
            .bind(Action.HOME, key(KeyCode.HOME), ctrl('a'))
            .bind(Action.END, key(KeyCode.END), ctrl('e'))

            // Selection
            .bind(Action.SELECT, key(KeyCode.ENTER), ch(' '))
            .bind(Action.CONFIRM, key(KeyCode.ENTER))
            .bind(Action.CANCEL, key(KeyCode.ESCAPE), ctrl('g'))

            // Focus
            .bind(Action.FOCUS_NEXT, key(KeyCode.TAB))
            .bind(Action.FOCUS_PREVIOUS, key(KeyCode.TAB, false, false, true))

            // Editing
            .bind(Action.DELETE_BACKWARD, key(KeyCode.BACKSPACE), ctrl('h'))
            .bind(Action.DELETE_FORWARD, key(KeyCode.DELETE), ctrl('d'))

            // Application
            .bind(Action.QUIT, ctrl('c'))
            .build();
    }

    private static KeyMap createIntelliJ() {
        // IntelliJ uses mostly standard arrow keys with some Ctrl combinations
        return DefaultKeyMap.builder()
            // Navigation - arrows
            .bind(Action.MOVE_UP, key(KeyCode.UP))
            .bind(Action.MOVE_DOWN, key(KeyCode.DOWN))
            .bind(Action.MOVE_LEFT, key(KeyCode.LEFT))
            .bind(Action.MOVE_RIGHT, key(KeyCode.RIGHT))

            // Page navigation - Ctrl+Home/End for document start/end
            .bind(Action.PAGE_UP, key(KeyCode.PAGE_UP))
            .bind(Action.PAGE_DOWN, key(KeyCode.PAGE_DOWN))
            .bind(Action.HOME, key(KeyCode.HOME), key(KeyCode.HOME, true, false, false))
            .bind(Action.END, key(KeyCode.END), key(KeyCode.END, true, false, false))

            // Selection
            .bind(Action.SELECT, key(KeyCode.ENTER), ch(' '))
            .bind(Action.CONFIRM, key(KeyCode.ENTER))
            .bind(Action.CANCEL, key(KeyCode.ESCAPE))

            // Focus
            .bind(Action.FOCUS_NEXT, key(KeyCode.TAB))
            .bind(Action.FOCUS_PREVIOUS, key(KeyCode.TAB, false, false, true))

            // Editing
            .bind(Action.DELETE_BACKWARD, key(KeyCode.BACKSPACE))
            .bind(Action.DELETE_FORWARD, key(KeyCode.DELETE))

            // Application - IntelliJ doesn't have 'q' to quit by default
            .bind(Action.QUIT, ctrl('c'))
            .build();
    }

    private static KeyMap createVSCode() {
        // VS Code uses mostly standard arrow keys
        return DefaultKeyMap.builder()
            // Navigation - arrows
            .bind(Action.MOVE_UP, key(KeyCode.UP))
            .bind(Action.MOVE_DOWN, key(KeyCode.DOWN))
            .bind(Action.MOVE_LEFT, key(KeyCode.LEFT))
            .bind(Action.MOVE_RIGHT, key(KeyCode.RIGHT))

            // Page navigation
            .bind(Action.PAGE_UP, key(KeyCode.PAGE_UP))
            .bind(Action.PAGE_DOWN, key(KeyCode.PAGE_DOWN))
            .bind(Action.HOME, key(KeyCode.HOME))
            .bind(Action.END, key(KeyCode.END))

            // Selection
            .bind(Action.SELECT, key(KeyCode.ENTER), ch(' '))
            .bind(Action.CONFIRM, key(KeyCode.ENTER))
            .bind(Action.CANCEL, key(KeyCode.ESCAPE))

            // Focus
            .bind(Action.FOCUS_NEXT, key(KeyCode.TAB))
            .bind(Action.FOCUS_PREVIOUS, key(KeyCode.TAB, false, false, true))

            // Editing
            .bind(Action.DELETE_BACKWARD, key(KeyCode.BACKSPACE))
            .bind(Action.DELETE_FORWARD, key(KeyCode.DELETE))

            // Application
            .bind(Action.QUIT, ctrl('c'))
            .build();
    }

    // Key name to KeyCode mapping for parsing
    private static final Map<String, KeyCode> KEY_NAMES = createKeyNameMap();

    private static Map<String, KeyCode> createKeyNameMap() {
        Map<String, KeyCode> map = new HashMap<>();
        map.put("up", KeyCode.UP);
        map.put("down", KeyCode.DOWN);
        map.put("left", KeyCode.LEFT);
        map.put("right", KeyCode.RIGHT);
        map.put("enter", KeyCode.ENTER);
        map.put("tab", KeyCode.TAB);
        map.put("escape", KeyCode.ESCAPE);
        map.put("esc", KeyCode.ESCAPE);
        map.put("backspace", KeyCode.BACKSPACE);
        map.put("delete", KeyCode.DELETE);
        map.put("del", KeyCode.DELETE);
        map.put("insert", KeyCode.INSERT);
        map.put("ins", KeyCode.INSERT);
        map.put("home", KeyCode.HOME);
        map.put("end", KeyCode.END);
        map.put("pageup", KeyCode.PAGE_UP);
        map.put("pgup", KeyCode.PAGE_UP);
        map.put("pagedown", KeyCode.PAGE_DOWN);
        map.put("pgdn", KeyCode.PAGE_DOWN);
        map.put("f1", KeyCode.F1);
        map.put("f2", KeyCode.F2);
        map.put("f3", KeyCode.F3);
        map.put("f4", KeyCode.F4);
        map.put("f5", KeyCode.F5);
        map.put("f6", KeyCode.F6);
        map.put("f7", KeyCode.F7);
        map.put("f8", KeyCode.F8);
        map.put("f9", KeyCode.F9);
        map.put("f10", KeyCode.F10);
        map.put("f11", KeyCode.F11);
        map.put("f12", KeyCode.F12);
        return map;
    }

    /**
     * Loads a keymap from a properties file.
     * <p>
     * The file format uses standard Java properties:
     * <pre>{@code
     * # Navigation
     * MOVE_UP = Up, k, K
     * MOVE_DOWN = Down, j, J
     * CONFIRM = Enter
     * QUIT = q, Ctrl+c
     * }</pre>
     * <p>
     * Binding syntax:
     * <ul>
     *   <li>Key names: Up, Down, Enter, Tab, Escape, Backspace, Delete, Home, End, PageUp, PageDown, F1-F12</li>
     *   <li>Characters: Single character like k, q</li>
     *   <li>Modifiers: Ctrl+c, Alt+x, Shift+Tab</li>
     *   <li>Space: Use the word "Space"</li>
     * </ul>
     *
     * @param path the path to the properties file
     * @return the loaded keymap
     * @throws IOException if the file cannot be read or parsed
     */
    public static KeyMap load(Path path) throws IOException {
        try (InputStream in = Files.newInputStream(path)) {
            return load(in);
        }
    }

    /**
     * Loads a keymap from an input stream containing properties.
     *
     * @param input the input stream
     * @return the loaded keymap
     * @throws IOException if the stream cannot be read or parsed
     * @see #load(Path)
     */
    public static KeyMap load(InputStream input) throws IOException {
        Properties props = new Properties();
        props.load(input);
        return loadFromProperties(props);
    }

    /**
     * Loads a keymap from a classpath resource.
     *
     * @param resourcePath the resource path (e.g., "/keymaps/custom.properties")
     * @return the loaded keymap
     * @throws IOException if the resource cannot be found or parsed
     * @see #load(Path)
     */
    public static KeyMap loadResource(String resourcePath) throws IOException {
        return loadResource(resourcePath, KeyMaps.class.getClassLoader());
    }

    /**
     * Loads a keymap from a classpath resource using the specified class loader.
     *
     * @param resourcePath the resource path
     * @param classLoader  the class loader to use
     * @return the loaded keymap
     * @throws IOException if the resource cannot be found or parsed
     */
    public static KeyMap loadResource(String resourcePath, ClassLoader classLoader) throws IOException {
        try (InputStream in = classLoader.getResourceAsStream(
                resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath)) {
            if (in == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            return load(in);
        }
    }

    private static KeyMap loadFromProperties(Properties props) throws IOException {
        DefaultKeyMap.Builder builder = DefaultKeyMap.builder();

        for (String actionName : props.stringPropertyNames()) {
            Action action;
            try {
                action = Action.valueOf(actionName.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                // Unknown action - skip with warning (forward compatibility)
                continue;
            }

            String bindings = props.getProperty(actionName);
            String[] parts = bindings.split(",");
            KeyBinding[] keyBindings = new KeyBinding[parts.length];

            for (int i = 0; i < parts.length; i++) {
                keyBindings[i] = parseBinding(parts[i].trim());
            }

            builder.bind(action, keyBindings);
        }

        return builder.build();
    }

    private static KeyBinding parseBinding(String text) throws IOException {
        if (text.isEmpty()) {
            throw new IOException("Empty binding");
        }

        boolean ctrl = false;
        boolean alt = false;
        boolean shift = false;
        String key = text;

        // Parse modifiers
        while (true) {
            String lower = key.toLowerCase();
            if (lower.startsWith("ctrl+")) {
                ctrl = true;
                key = key.substring(5);
            } else if (lower.startsWith("alt+")) {
                alt = true;
                key = key.substring(4);
            } else if (lower.startsWith("shift+")) {
                shift = true;
                key = key.substring(6);
            } else {
                break;
            }
        }

        // Check for special key name
        String lowerKey = key.toLowerCase();
        if ("space".equals(lowerKey)) {
            return new KeyBindingBuilder()
                .character(' ')
                .ctrl(ctrl).alt(alt).shift(shift)
                .build();
        }

        KeyCode keyCode = KEY_NAMES.get(lowerKey);
        if (keyCode != null) {
            return key(keyCode, ctrl, alt, shift);
        }

        // Single character
        if (key.length() == 1) {
            char c = key.charAt(0);
            if (ctrl) {
                return ctrl(Character.toLowerCase(c));
            }
            if (alt) {
                return alt(Character.toLowerCase(c));
            }
            // Case-sensitive character binding
            return ch(c);
        }

        throw new IOException("Unknown key: " + text);
    }

    // Helper class to build KeyBinding with modifiers for space
    private static class KeyBindingBuilder {
        private char character;
        private boolean hasCtrl;
        private boolean hasAlt;
        private boolean hasShift;

        KeyBindingBuilder character(char c) {
            this.character = c;
            return this;
        }

        KeyBindingBuilder ctrl(boolean v) {
            this.hasCtrl = v;
            return this;
        }

        KeyBindingBuilder alt(boolean v) {
            this.hasAlt = v;
            return this;
        }

        KeyBindingBuilder shift(boolean v) {
            this.hasShift = v;
            return this;
        }

        KeyBinding build() {
            if (hasCtrl) {
                return KeyBinding.ctrl(character);
            }
            if (hasAlt) {
                return KeyBinding.alt(character);
            }
            return KeyBinding.ch(character);
        }
    }
}
