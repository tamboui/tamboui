/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.bindings;

import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.MouseButton;
import dev.tamboui.tui.event.MouseEventKind;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Factory for predefined binding sets.
 * <p>
 * Available binding sets:
 * <ul>
 *   <li>{@link #standard()} - Arrow keys only, no vim/emacs bindings (default)</li>
 *   <li>{@link #vim()} - Vim-style navigation (hjkl, g/G, Ctrl+u/d)</li>
 *   <li>{@link #emacs()} - Emacs-style navigation (Ctrl+n/p/f/b, etc.)</li>
 *   <li>{@link #intellij()} - IntelliJ IDEA-style bindings</li>
 *   <li>{@link #vscode()} - Visual Studio Code-style bindings</li>
 * </ul>
 *
 * <pre>{@code
 * // Use a predefined binding set
 * Bindings bindings = BindingSets.vim();
 *
 * // Customize a predefined set
 * Bindings custom = BindingSets.standard()
 *     .toBuilder()
 *     .bind(Actions.QUIT, KeyTrigger.ch('x'))
 *     .bind("contextMenu", MouseTrigger.rightClick())
 *     .build();
 * }</pre>
 */
public final class BindingSets {

    private static final String BINDINGS_RESOURCE_PATH = "dev/tamboui/tui/bindings/";

    // Key name to KeyCode mapping for parsing - must be initialized before binding sets
    private static final Map<String, KeyCode> KEY_NAMES = createKeyNameMap();
    private static final Map<String, MouseButton> MOUSE_BUTTONS = createMouseButtonMap();
    private static final Map<String, MouseEventKind> MOUSE_KINDS = createMouseKindMap();

    // Predefined binding sets - loaded from properties files
    private static final Bindings STANDARD = loadBuiltIn("standard.properties", null);
    private static final Bindings VIM = loadBuiltIn("vim.properties", STANDARD);
    private static final Bindings EMACS = loadBuiltIn("emacs.properties", STANDARD);
    private static final Bindings INTELLIJ = loadBuiltIn("intellij.properties", STANDARD);
    private static final Bindings VSCODE = loadBuiltIn("vscode.properties", STANDARD);

    private BindingSets() {
    }

    private static Bindings loadBuiltIn(String resourceName, Bindings base) {
        try (InputStream in = BindingSets.class.getClassLoader()
                .getResourceAsStream(BINDINGS_RESOURCE_PATH + resourceName)) {
            if (in == null) {
                throw new RuntimeException("Built-in bindings not found: " + resourceName);
            }
            return load(in, base);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load built-in bindings: " + resourceName, e);
        }
    }

    /**
     * Standard binding set using only arrow keys and standard keys.
     * <p>
     * No vim or emacs-style bindings. This is the default set,
     * safe for use with text input (no letter keys bound to navigation).
     *
     * @return the standard bindings
     */
    public static Bindings standard() {
        return STANDARD;
    }

    /**
     * Vim-style binding set with hjkl navigation.
     * <p>
     * Includes:
     * <ul>
     *   <li>hjkl for directional navigation</li>
     *   <li>g/G for home/end</li>
     *   <li>Ctrl+u/d for page up/down</li>
     * </ul>
     *
     * @return the vim bindings
     */
    public static Bindings vim() {
        return VIM;
    }

    /**
     * Emacs-style binding set with Ctrl+n/p/f/b navigation.
     * <p>
     * Includes:
     * <ul>
     *   <li>Ctrl+n/p/f/b for directional navigation</li>
     *   <li>Alt+v/Ctrl+v for page up/down</li>
     *   <li>Ctrl+a/e for home/end (line-level)</li>
     *   <li>Ctrl+g as cancel</li>
     * </ul>
     *
     * @return the emacs bindings
     */
    public static Bindings emacs() {
        return EMACS;
    }

    /**
     * IntelliJ IDEA-style binding set.
     * <p>
     * Based on IntelliJ's default key bindings for navigation.
     *
     * @return the IntelliJ bindings
     */
    public static Bindings intellij() {
        return INTELLIJ;
    }

    /**
     * Visual Studio Code-style binding set.
     * <p>
     * Based on VS Code's default key bindings for navigation.
     *
     * @return the VS Code bindings
     */
    public static Bindings vscode() {
        return VSCODE;
    }

    /**
     * Returns the default bindings.
     * <p>
     * Currently returns {@link #standard()}.
     *
     * @return the default bindings
     */
    public static Bindings defaults() {
        return STANDARD;
    }

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

    private static Map<String, MouseButton> createMouseButtonMap() {
        Map<String, MouseButton> map = new HashMap<>();
        map.put("left", MouseButton.LEFT);
        map.put("right", MouseButton.RIGHT);
        map.put("middle", MouseButton.MIDDLE);
        return map;
    }

    private static Map<String, MouseEventKind> createMouseKindMap() {
        Map<String, MouseEventKind> map = new HashMap<>();
        map.put("press", MouseEventKind.PRESS);
        map.put("release", MouseEventKind.RELEASE);
        map.put("drag", MouseEventKind.DRAG);
        map.put("move", MouseEventKind.MOVE);
        map.put("scrollup", MouseEventKind.SCROLL_UP);
        map.put("scroll_up", MouseEventKind.SCROLL_UP);
        map.put("scrolldown", MouseEventKind.SCROLL_DOWN);
        map.put("scroll_down", MouseEventKind.SCROLL_DOWN);
        return map;
    }

    /**
     * Loads bindings from a properties file.
     * <p>
     * The file format uses standard Java properties:
     * <pre>{@code
     * # Navigation
     * moveUp = Up, k, K
     * moveDown = Down, j, J
     * confirm = Enter
     * quit = q, Ctrl+c
     *
     * # Mouse bindings
     * click = Mouse.Left.Press
     * rightClick = Mouse.Right.Press
     * ctrlClick = Ctrl+Mouse.Left.Press
     * }</pre>
     * <p>
     * Key binding syntax:
     * <ul>
     *   <li>Key names: Up, Down, Enter, Tab, Escape, Backspace, Delete, Home, End, PageUp, PageDown, F1-F12</li>
     *   <li>Characters: Single character like k, q</li>
     *   <li>Modifiers: Ctrl+c, Alt+x, Shift+Tab</li>
     *   <li>Space: Use the word "Space"</li>
     * </ul>
     * <p>
     * Mouse binding syntax:
     * <ul>
     *   <li>Format: [Modifiers+]Mouse.Button.Kind</li>
     *   <li>Buttons: Left, Right, Middle</li>
     *   <li>Kinds: Press, Release, Drag, ScrollUp, ScrollDown</li>
     *   <li>Example: Ctrl+Mouse.Left.Press</li>
     * </ul>
     *
     * @param path the path to the properties file
     * @return the loaded bindings
     * @throws IOException if the file cannot be read or parsed
     */
    public static Bindings load(Path path) throws IOException {
        try (InputStream in = Files.newInputStream(path)) {
            return load(in);
        }
    }

    /**
     * Loads bindings from an input stream containing properties.
     * <p>
     * The loaded bindings will be based on the standard bindings,
     * with properties overriding/adding to them.
     *
     * @param input the input stream
     * @return the loaded bindings
     * @throws IOException if the stream cannot be read or parsed
     * @see #load(Path)
     */
    public static Bindings load(InputStream input) throws IOException {
        return load(input, STANDARD);
    }

    /**
     * Loads bindings from an input stream, starting with the given base bindings.
     *
     * @param input the input stream
     * @param base  the base bindings to extend (null for empty)
     * @return the loaded bindings
     * @throws IOException if the stream cannot be read or parsed
     */
    public static Bindings load(InputStream input, Bindings base) throws IOException {
        Properties props = new Properties();
        props.load(input);
        return loadFromProperties(props, base);
    }

    /**
     * Loads bindings from a classpath resource.
     *
     * @param resourcePath the resource path (e.g., "/bindings/custom.properties")
     * @return the loaded bindings
     * @throws IOException if the resource cannot be found or parsed
     * @see #load(Path)
     */
    public static Bindings loadResource(String resourcePath) throws IOException {
        return loadResource(resourcePath, BindingSets.class.getClassLoader());
    }

    /**
     * Loads bindings from a classpath resource using the specified class loader.
     *
     * @param resourcePath the resource path
     * @param classLoader  the class loader to use
     * @return the loaded bindings
     * @throws IOException if the resource cannot be found or parsed
     */
    public static Bindings loadResource(String resourcePath, ClassLoader classLoader) throws IOException {
        try (InputStream in = classLoader.getResourceAsStream(
                resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath)) {
            if (in == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            return load(in);
        }
    }

    private static Bindings loadFromProperties(Properties props, Bindings base) throws IOException {
        DefaultBindings.Builder builder;
        if (base != null) {
            builder = base.toBuilder();
        } else {
            builder = DefaultBindings.builder();
        }

        for (String actionName : props.stringPropertyNames()) {
            String bindingsStr = props.getProperty(actionName);
            String[] parts = bindingsStr.split(",");
            List<InputTrigger> triggers = new ArrayList<>();

            for (String part : parts) {
                triggers.add(parseTrigger(part.trim()));
            }

            builder.bind(actionName, triggers.toArray(new InputTrigger[0]));
        }

        return builder.build();
    }

    private static InputTrigger parseTrigger(String text) throws IOException {
        if (text.isEmpty()) {
            throw new IOException("Empty trigger");
        }

        // Check if it's a mouse trigger
        String lower = text.toLowerCase();
        if (lower.contains("mouse.")) {
            return parseMouseTrigger(text);
        }

        return parseKeyTrigger(text);
    }

    private static KeyTrigger parseKeyTrigger(String text) throws IOException {
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
            if (ctrl) {
                return KeyTrigger.ctrl(' ');
            }
            if (alt) {
                return KeyTrigger.alt(' ');
            }
            return KeyTrigger.ch(' ');
        }

        KeyCode keyCode = KEY_NAMES.get(lowerKey);
        if (keyCode != null) {
            return KeyTrigger.key(keyCode, ctrl, alt, shift);
        }

        // Single character
        if (key.length() == 1) {
            char c = key.charAt(0);
            if (ctrl) {
                return KeyTrigger.ctrl(Character.toLowerCase(c));
            }
            if (alt) {
                return KeyTrigger.alt(Character.toLowerCase(c));
            }
            // Case-sensitive character binding
            return KeyTrigger.ch(c);
        }

        throw new IOException("Unknown key: " + text);
    }

    private static MouseTrigger parseMouseTrigger(String text) throws IOException {
        boolean ctrl = false;
        boolean alt = false;
        boolean shift = false;
        String remaining = text;

        // Parse modifiers before "Mouse."
        while (true) {
            String lower = remaining.toLowerCase();
            if (lower.startsWith("ctrl+")) {
                ctrl = true;
                remaining = remaining.substring(5);
            } else if (lower.startsWith("alt+")) {
                alt = true;
                remaining = remaining.substring(4);
            } else if (lower.startsWith("shift+")) {
                shift = true;
                remaining = remaining.substring(6);
            } else {
                break;
            }
        }

        // Should start with "Mouse."
        if (!remaining.toLowerCase().startsWith("mouse.")) {
            throw new IOException("Invalid mouse trigger (expected Mouse.Button.Kind): " + text);
        }
        remaining = remaining.substring(6); // Remove "Mouse."

        // Parse button and kind
        String[] parts = remaining.split("\\.");
        if (parts.length < 1 || parts.length > 2) {
            throw new IOException("Invalid mouse trigger format: " + text);
        }

        MouseButton button = MouseButton.NONE;
        MouseEventKind kind;

        if (parts.length == 2) {
            // Format: Button.Kind
            button = MOUSE_BUTTONS.get(parts[0].toLowerCase());
            if (button == null) {
                throw new IOException("Unknown mouse button: " + parts[0]);
            }
            kind = MOUSE_KINDS.get(parts[1].toLowerCase());
            if (kind == null) {
                throw new IOException("Unknown mouse event kind: " + parts[1]);
            }
        } else {
            // Format: Kind only (for scroll events)
            kind = MOUSE_KINDS.get(parts[0].toLowerCase());
            if (kind == null) {
                throw new IOException("Unknown mouse event kind: " + parts[0]);
            }
        }

        return MouseTrigger.of(kind, button, ctrl, alt, shift);
    }
}
