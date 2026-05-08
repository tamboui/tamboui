///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS dev.tamboui:tamboui-toolkit-markdown:LATEST
//DEPS dev.tamboui:tamboui-css:LATEST
//DEPS dev.tamboui:tamboui-jline3-backend:LATEST
//FILES sample.md=../../../../resources/sample.md

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import dev.tamboui.css.engine.StyleEngine;
import dev.tamboui.toolkit.app.ToolkitApp;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.toolkit.markdown.MarkdownElement;
import dev.tamboui.tui.event.Event;
import dev.tamboui.tui.event.KeyEvent;

import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.panel;
import static dev.tamboui.toolkit.Toolkit.row;
import static dev.tamboui.toolkit.Toolkit.spacer;
import static dev.tamboui.toolkit.Toolkit.text;

/**
 * Demo for {@code MarkdownElement}: renders a markdown document inside a
 * toolkit panel and switches between three CSS themes loaded into the
 * {@link StyleEngine}.
 *
 * <p>Controls:
 * <ul>
 *   <li>{@code ↑} / {@code ↓} — scroll up / down</li>
 *   <li>{@code t} — cycle theme (default → vivid → minimal)</li>
 *   <li>{@code q} — quit</li>
 * </ul>
 */
public final class MarkdownToolkitDemo extends ToolkitApp {

    private static final String[] THEMES = {"default", "vivid", "minimal"};

    private static final String VIVID_CSS = ""
        + "MarkdownElement {\n"
        + "  heading-1-color: magenta; heading-1-text-style: bold reversed;\n"
        + "  heading-2-color: cyan;    heading-2-text-style: bold;\n"
        + "  heading-3-color: yellow;  heading-3-text-style: bold;\n"
        + "  link-color: green;        link-text-style: underlined;\n"
        + "  inline-code-color: yellow;\n"
        + "  blockquote-color: cyan;   blockquote-prefix: \"❯\";\n"
        + "  task-checked-color: green;  task-checked-symbol: \"✓\";\n"
        + "  task-unchecked-color: gray; task-unchecked-symbol: \"·\";\n"
        + "}\n";

    private static final String MINIMAL_CSS = ""
        + "MarkdownElement {\n"
        + "  heading-1-color: white; heading-1-text-style: bold;\n"
        + "  heading-2-color: white; heading-2-text-style: bold;\n"
        + "  heading-3-color: white; heading-3-text-style: bold;\n"
        + "  link-color: white;      link-text-style: underlined;\n"
        + "  inline-code-color: gray;\n"
        + "  blockquote-color: gray;\n"
        + "  task-checked-color: white;  task-checked-symbol: \"[x]\";\n"
        + "  task-unchecked-color: gray; task-unchecked-symbol: \"[ ]\";\n"
        + "  list-marker-color: gray;\n"
        + "}\n";

    private final String source;
    private final StyleEngine styleEngine = StyleEngine.create();
    private int themeIndex;
    private int scroll;

    private MarkdownToolkitDemo(String source) {
        this.source = source;
        applyTheme();
    }

    /**
     * Demo entry point.
     *
     * @param args ignored
     * @throws Exception on startup failure
     */
    public static void main(String[] args) throws Exception {
        new MarkdownToolkitDemo(loadSample()).run();
    }

    private static String loadSample() throws IOException {
        try (InputStream in = MarkdownToolkitDemo.class.getResourceAsStream("/sample.md")) {
            if (in == null) {
                throw new IOException("sample.md missing from resources");
            }
            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(in, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        }
    }

    @Override
    protected void onStart() {
        runner().styleEngine(styleEngine);
        runner().eventRouter().addGlobalHandler(this::handleEvent);
    }

    @Override
    protected Element render() {
        return column(
            panel(() -> row(
                text(" markdown-toolkit-demo ").bold().cyan(),
                spacer(),
                text(" theme: " + THEMES[themeIndex] + " ").yellow(),
                text(" [t] cycle ").dim(),
                text(" [↑/↓] scroll ").dim(),
                text(" [q] quit ").dim()
            )).rounded().length(3),
            panel(
                MarkdownElement.markdown(source).scroll(scroll).fill()
            ).rounded().fill()
        );
    }

    private EventResult handleEvent(Event event) {
        if (!(event instanceof KeyEvent)) {
            return EventResult.UNHANDLED;
        }
        KeyEvent key = (KeyEvent) event;
        if (key.isDown()) {
            scroll++;
            return EventResult.HANDLED;
        }
        if (key.isUp()) {
            scroll = Math.max(0, scroll - 1);
            return EventResult.HANDLED;
        }
        switch (key.character()) {
            case 't':
            case 'T':
                themeIndex = (themeIndex + 1) % THEMES.length;
                applyTheme();
                return EventResult.HANDLED;
            case 'q':
            case 'Q':
                quit();
                return EventResult.HANDLED;
            default:
                return EventResult.UNHANDLED;
        }
    }

    private void applyTheme() {
        String name = THEMES[themeIndex];
        switch (name) {
            case "vivid":
                styleEngine.addStylesheet("theme", VIVID_CSS);
                break;
            case "minimal":
                styleEngine.addStylesheet("theme", MINIMAL_CSS);
                break;
            case "default":
            default:
                styleEngine.addStylesheet("theme", "MarkdownElement {}\n");
                break;
        }
    }
}
