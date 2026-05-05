///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS dev.tamboui:tamboui-markdown:LATEST
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

import dev.tamboui.markdown.MarkdownView;
import dev.tamboui.terminal.Backend;
import dev.tamboui.terminal.BackendFactory;
import dev.tamboui.terminal.Frame;
import dev.tamboui.terminal.Terminal;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.widgets.block.Title;

/**
 * Interactive demo for {@link MarkdownView}. Loads a sample markdown document
 * (or a user-supplied file via the first argument), renders it inside a
 * bordered block, and supports j/k scrolling and a streaming simulation that
 * reveals the source one character at a time on every tick.
 */
public final class MarkdownDemo {

    private static final String BUNDLED_RESOURCE = "/sample.md";

    private final String fullSource;
    private int scroll;
    private int streamLength;
    private boolean streaming;
    private boolean running = true;

    private MarkdownDemo(String source) {
        this.fullSource = source;
        this.streamLength = source.length();
    }

    /**
     * Demo entry point.
     *
     * @param args optional path to a markdown file
     * @throws Exception on unexpected error
     */
    public static void main(String[] args) throws Exception {
        String source;
        if (args.length > 0) {
            Path path = Path.of(args[0]);
            if (!Files.exists(path)) {
                throw new IOException("File not found: " + path);
            }
            source = Files.readString(path, StandardCharsets.UTF_8);
        } else {
            source = loadResource(BUNDLED_RESOURCE);
        }
        new MarkdownDemo(source).run();
    }

    private static String loadResource(String name) throws IOException {
        try (InputStream in = MarkdownDemo.class.getResourceAsStream(name)) {
            if (in == null) {
                throw new IOException("Resource not found: " + name);
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        }
    }

    private void run() throws Exception {
        try (Backend backend = BackendFactory.create()) {
            backend.enableRawMode();
            backend.enterAlternateScreen();
            backend.hideCursor();

            Terminal<Backend> terminal = new Terminal<>(backend);
            backend.onResize(() -> {
                try {
                    terminal.draw(this::draw);
                } catch (Exception ignored) {
                }
            });

            terminal.draw(this::draw);

            long lastTick = System.nanoTime();
            while (running) {
                int c = backend.read(50);
                if (c >= 0) {
                    handleInput(c);
                }
                if (streaming && streamLength < fullSource.length()) {
                    long now = System.nanoTime();
                    if (now - lastTick > 25_000_000L) {
                        streamLength = Math.min(fullSource.length(), streamLength + 1);
                        lastTick = now;
                    }
                }
                terminal.draw(this::draw);
            }
        }
    }

    private void draw(Frame frame) {
        String visibleSourceText = streaming
            ? fullSource.substring(0, Math.min(streamLength, fullSource.length()))
            : fullSource;
        String title = streaming
            ? " markdown-demo (streaming) "
            : " markdown-demo ";
        Block frameBlock = Block.builder()
            .borders(Borders.ALL)
            .borderType(BorderType.ROUNDED)
            .title(Title.from(title))
            .titleBottom(Title.from(" j/k scroll · s stream · q quit "))
            .build();
        MarkdownView view = MarkdownView.builder()
            .source(visibleSourceText)
            .block(frameBlock)
            .scroll(scroll)
            .build();
        frame.renderWidget(view, frame.area());
    }

    private void handleInput(int c) {
        switch (c) {
            case 'q':
            case 'Q':
            case 3:
                running = false;
                break;
            case 'j':
            case 'J':
                scroll++;
                break;
            case 'k':
            case 'K':
                scroll = Math.max(0, scroll - 1);
                break;
            case 's':
            case 'S':
                streaming = !streaming;
                if (streaming) {
                    streamLength = 0;
                } else {
                    streamLength = fullSource.length();
                }
                break;
            default:
                break;
        }
    }
}
