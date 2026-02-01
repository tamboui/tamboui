/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.engine;

import dev.tamboui.css.Styleable;
import dev.tamboui.css.cascade.CascadeResolver;
import dev.tamboui.css.cascade.PseudoClassState;
import dev.tamboui.css.cascade.CssStyleResolver;
import dev.tamboui.css.model.Rule;
import dev.tamboui.css.model.Stylesheet;
import dev.tamboui.css.parser.CssParser;
import dev.tamboui.css.property.PropertyConverter;
import dev.tamboui.error.RuntimeIOException;
import dev.tamboui.style.Color;
import dev.tamboui.style.ColorConverter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

/**
 * Main entry point for CSS styling.
 * <p>
 * The StyleEngine manages stylesheets, resolves styles for elements,
 * and supports live stylesheet switching for theming.
 *
 * <h2>Stylesheet Types</h2>
 * <p>
 * The engine supports two types of stylesheets:
 * <ul>
 *   <li><b>Inline stylesheets</b>: Added via {@link #addStylesheet(String)} or {@link #loadStylesheet(String)}.
 *       Multiple inline stylesheets can be registered and are <em>all always applied</em>,
 *       in the order they were added. Use these for base styles that should always be present.</li>
 *   <li><b>Named stylesheets</b>: Added via {@link #addStylesheet(String, String)} or
 *       {@link #loadStylesheet(String, String)}. Only <em>one named stylesheet is active</em> at a time,
 *       selectable via {@link #setActiveStylesheet(String)}. Use these for themes that can be switched
 *       at runtime.</li>
 * </ul>
 *
 * <h2>Cascade Order</h2>
 * <p>
 * When resolving styles, rules are collected in the following order:
 * <ol>
 *   <li>All inline stylesheets (in registration order)</li>
 *   <li>The active named stylesheet</li>
 * </ol>
 * Later rules override earlier ones following standard CSS cascade rules,
 * so the active named stylesheet can override inline styles.
 *
 * <h2>Usage Example</h2>
 * <pre>
 * StyleEngine engine = StyleEngine.create();
 *
 * // Add base styles (always applied)
 * engine.addStylesheet("* { padding: 1; }");
 *
 * // Load theme stylesheets (only one active at a time)
 * engine.loadStylesheet("dark", "/themes/dark.tcss");
 * engine.loadStylesheet("light", "/themes/light.tcss");
 * engine.setActiveStylesheet("dark");
 *
 * // Switch themes at runtime
 * engine.setActiveStylesheet("light");
 * </pre>
 */
public final class StyleEngine {

    private final Map<String, StylesheetEntry> namedStylesheets;
    private final List<Stylesheet> inlineStylesheets;
    private final CascadeResolver cascadeResolver;
    private final List<StyleChangeListener> listeners;

    private String activeStylesheetName;

    private StyleEngine() {
        this.namedStylesheets = new LinkedHashMap<>();
        this.inlineStylesheets = new ArrayList<>();
        this.cascadeResolver = new CascadeResolver();
        this.listeners = new CopyOnWriteArrayList<>();
        this.activeStylesheetName = null;
    }

    /**
     * Creates a new StyleEngine with default configuration.
     *
     * @return a new StyleEngine
     */
    public static StyleEngine create() {
        return new StyleEngine();
    }

    // --- Stylesheet Loading ---

    /**
     * Loads a stylesheet from the classpath.
     *
     * @param classpathResource the classpath resource path (e.g., "/styles/app.tcss")
     * @throws IOException if the resource cannot be read
     */
    public void loadStylesheet(String classpathResource) throws IOException {
        String css = readClasspathResource(classpathResource);
        Stylesheet stylesheet = CssParser.parse(css);
        inlineStylesheets.add(stylesheet);
    }

    /**
     * Loads a named stylesheet from the classpath.
     * <p>
     * Named stylesheets can be switched at runtime using {@link #setActiveStylesheet(String)}.
     *
     * @param name              the stylesheet name (e.g., "dark", "light")
     * @param classpathResource the classpath resource path
     * @throws IOException if the resource cannot be read
     */
    public void loadStylesheet(String name, String classpathResource) throws IOException {
        Supplier<String> source = () -> {
            try {
                return readClasspathResource(classpathResource);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
        Stylesheet stylesheet = CssParser.parse(source.get());
        namedStylesheets.put(name, new StylesheetEntry(stylesheet, source));

        // Auto-activate first loaded stylesheet
        if (activeStylesheetName == null) {
            activeStylesheetName = name;
        }
    }

    /**
     * Loads a stylesheet from a file path.
     *
     * @param path the file path
     * @throws IOException if the file cannot be read
     */
    public void loadStylesheet(Path path) throws IOException {
        String css = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        Stylesheet stylesheet = CssParser.parse(css);
        inlineStylesheets.add(stylesheet);
    }

    /**
     * Loads a named stylesheet from a file path.
     *
     * @param name the stylesheet name
     * @param path the file path
     * @throws IOException if the file cannot be read
     */
    public void loadStylesheet(String name, Path path) throws IOException {
        Supplier<String> source = () -> {
            try {
                return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
        Stylesheet stylesheet = CssParser.parse(source.get());
        namedStylesheets.put(name, new StylesheetEntry(stylesheet, source));

        if (activeStylesheetName == null) {
            activeStylesheetName = name;
        }
    }

    /**
     * Adds an inline stylesheet from a CSS string.
     *
     * @param css the CSS source code
     */
    public void addStylesheet(String css) {
        Stylesheet stylesheet = CssParser.parse(css);
        inlineStylesheets.add(stylesheet);
    }

    /**
     * Adds a named inline stylesheet.
     *
     * @param name the stylesheet name
     * @param css  the CSS source code
     */
    public void addStylesheet(String name, String css) {
        Stylesheet stylesheet = CssParser.parse(css);
        namedStylesheets.put(name, new StylesheetEntry(stylesheet, null));

        if (activeStylesheetName == null) {
            activeStylesheetName = name;
        }
    }

    // --- Stylesheet Switching ---

    /**
     * Sets the active named stylesheet.
     * <p>
     * This enables live theme switching - the UI will use the new
     * stylesheet on the next render cycle.
     *
     * @param name the stylesheet name
     * @throws IllegalArgumentException if no stylesheet with that name exists
     */
    public void setActiveStylesheet(String name) {
        if (!namedStylesheets.containsKey(name)) {
            throw new IllegalArgumentException("No stylesheet named: " + name);
        }
        String oldName = activeStylesheetName;
        activeStylesheetName = name;

        if (!Objects.equals(oldName, name)) {
            notifyListeners();
        }
    }

    /**
     * Returns the name of the active stylesheet.
     *
     * @return the active stylesheet name, or empty if none set
     */
    public Optional<String> getActiveStylesheet() {
        return Optional.ofNullable(activeStylesheetName);
    }

    /**
     * Returns the names of all loaded named stylesheets.
     *
     * @return the stylesheet names
     */
    public Set<String> getStylesheetNames() {
        return Collections.unmodifiableSet(namedStylesheets.keySet());
    }

    /**
     * Reloads a named stylesheet from its original source.
     * <p>
     * Useful for hot-reload during development.
     *
     * @param name the stylesheet name
     */
    public void reloadStylesheet(String name) {
        StylesheetEntry entry = namedStylesheets.get(name);
        if (entry == null) {
            throw new IllegalArgumentException("No stylesheet named: " + name);
        }

        Supplier<String> source = entry.sourceProvider();
        if (source == null) {
            // Inline stylesheet - cannot reload
            return;
        }

        Stylesheet stylesheet = CssParser.parse(source.get());
        namedStylesheets.put(name, new StylesheetEntry(stylesheet, source));

        if (name.equals(activeStylesheetName)) {
            notifyListeners();
        }
    }

    // --- Style Resolution ---

    /**
     * Resolves the style for an element.
     *
     * @param element   the element to style
     * @param state     the pseudo-class state
     * @param ancestors the ancestor chain
     * @return the resolved style
     */
    public CssStyleResolver resolve(Styleable element,
                                     PseudoClassState state,
                                     List<Styleable> ancestors) {
        List<Rule> allRules = collectRules();
        Map<String, String> allVariables = collectVariables();

        return cascadeResolver.resolve(element, state, ancestors, allRules, allVariables);
    }

    /**
     * Resolves the style for an element with default state and no ancestors.
     *
     * @param element the element to style
     * @return the resolved style
     */
    public CssStyleResolver resolve(Styleable element) {
        return resolve(element, PseudoClassState.NONE, Collections.<Styleable>emptyList());
    }

    /**
     * Parses a CSS color value string into a Color.
     * <p>
     * Supports named colors (e.g., "red", "blue"), hex colors (e.g., "#ff0000"),
     * and RGB notation (e.g., "rgb(255,0,0)"), as well as CSS variables.
     *
     * @param colorValue the CSS color value string
     * @return the parsed color, or empty if parsing fails
     */
    public Optional<Color> parseColor(String colorValue) {
        if (colorValue == null || colorValue.isEmpty()) {
            return Optional.empty();
        }
        Map<String, String> variables = collectVariables();
        String resolvedValue = PropertyConverter.resolveVariables(colorValue, variables);
        return ColorConverter.INSTANCE.convert(resolvedValue);
    }

    // --- Change Listeners ---

    /**
     * Adds a listener to be notified when the active stylesheet changes.
     *
     * @param listener the listener
     */
    public void addChangeListener(StyleChangeListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes a change listener.
     *
     * @param listener the listener
     */
    public void removeChangeListener(StyleChangeListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (StyleChangeListener listener : listeners) {
            listener.onStyleChange();
        }
    }

    // --- Internal Methods ---

    private List<Rule> collectRules() {
        List<Rule> rules = new ArrayList<>();

        // Assign global source order so rules from later stylesheets
        // always win over earlier ones with the same specificity.
        // Each stylesheet's rules are parsed with per-stylesheet source orders
        // starting from 0, so we renumber them globally here.
        int globalOrder = 0;

        // Add rules from inline stylesheets
        for (Stylesheet stylesheet : inlineStylesheets) {
            for (Rule rule : stylesheet.rules()) {
                rules.add(new Rule(rule.selector(), rule.declarations(), globalOrder++));
            }
        }

        // Add rules from active named stylesheet
        if (activeStylesheetName != null) {
            StylesheetEntry entry = namedStylesheets.get(activeStylesheetName);
            if (entry != null) {
                for (Rule rule : entry.stylesheet().rules()) {
                    rules.add(new Rule(rule.selector(), rule.declarations(), globalOrder++));
                }
            }
        }

        return rules;
    }

    private Map<String, String> collectVariables() {
        Map<String, String> variables = new LinkedHashMap<>();

        // Collect from inline stylesheets
        for (Stylesheet stylesheet : inlineStylesheets) {
            variables.putAll(stylesheet.variables());
        }

        // Collect from active named stylesheet (overrides inline)
        if (activeStylesheetName != null) {
            StylesheetEntry entry = namedStylesheets.get(activeStylesheetName);
            if (entry != null) {
                variables.putAll(entry.stylesheet().variables());
            }
        }

        return variables;
    }

    private String readClasspathResource(String resource) throws IOException {
        InputStream is = getClass().getResourceAsStream(resource);
        if (is == null) {
            // Try without leading slash
            is = getClass().getClassLoader().getResourceAsStream(
                    resource.startsWith("/") ? resource.substring(1) : resource);
        }
        if (is == null) {
            throw new RuntimeIOException("Classpath resource not found: " + resource);
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        }
    }

    /**
     * Listener for stylesheet changes.
     */
    public interface StyleChangeListener {
        /**
         * Called when the active stylesheet changes.
         */
        void onStyleChange();
    }

    /**
     * Internal entry for named stylesheets.
     */
    private static final class StylesheetEntry {
        private final Stylesheet stylesheet;
        private final Supplier<String> sourceProvider;

        /**
         * @param stylesheet the parsed stylesheet
         * @param sourceProvider optional supplier that provides CSS content for reloading (null for inline stylesheets)
         */
        StylesheetEntry(Stylesheet stylesheet, Supplier<String> sourceProvider) {
            this.stylesheet = stylesheet;
            this.sourceProvider = sourceProvider;
        }

        Stylesheet stylesheet() {
            return stylesheet;
        }

        Supplier<String> sourceProvider() {
            return sourceProvider;
        }
    }
}
