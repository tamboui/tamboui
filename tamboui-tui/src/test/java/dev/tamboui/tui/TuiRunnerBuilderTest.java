/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dev.tamboui.terminal.Backend;
import dev.tamboui.terminal.BackendProvider;
import dev.tamboui.terminal.TestBackend;
import dev.tamboui.tui.bindings.BindingSets;
import dev.tamboui.tui.event.Event;
import dev.tamboui.tui.event.KeyEvent;

import static org.assertj.core.api.Assertions.assertThat;

class TuiRunnerBuilderTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("the bindings given to the builder apply to the keys read from the terminal")
    void builderBindingsApplyToKeysReadFromTheTerminal() throws Exception {
        try (TuiRunner.ConfiguredRunner runner = buildOnTypingBackend(TuiRunner.builder()
                .bindings(BindingSets.vim())
                .eventHandler((event, r) -> false)
                .autoBindingRegistration(true))) {
            Event event = runner.runner().pollEvent(Duration.ofSeconds(5));

            assertThat(event).isInstanceOf(KeyEvent.class);
            assertThat(((KeyEvent) event).isUp()).as("'k' is up with the vim bindings").isTrue();
            assertThat(runner.actionHandler().bindings()).isSameAs(BindingSets.vim());
        }
    }

    // The builder takes no backend: it finds one the way an application does, through a
    // BackendProvider service, which the context class loader provides here
    private TuiRunner.ConfiguredRunner buildOnTypingBackend(TuiRunner.Builder builder) throws Exception {
        Path services = tempDir.resolve("META-INF/services/" + BackendProvider.class.getName());
        Files.createDirectories(services.getParent());
        Files.write(services, TypingBackendProvider.class.getName().getBytes(StandardCharsets.UTF_8));
        Thread thread = Thread.currentThread();
        ClassLoader contextLoader = thread.getContextClassLoader();
        try (URLClassLoader loader = new URLClassLoader(new URL[] {tempDir.toUri().toURL()}, contextLoader)) {
            thread.setContextClassLoader(loader);
            return builder
                    .shutdownHook(false)
                    .noTick()
                    .build();
        } finally {
            thread.setContextClassLoader(contextLoader);
        }
    }

    /**
     * Provides a backend on which "k" is typed.
     */
    public static final class TypingBackendProvider implements BackendProvider {

        @Override
        public Backend create() {
            BlockingQueue<Integer> input = new LinkedBlockingQueue<>();
            input.add((int) 'k');
            return new TestBackend(80, 24) {
                @Override
                public int read(int timeoutMs) {
                    try {
                        Integer b = input.poll(timeoutMs, TimeUnit.MILLISECONDS);
                        return b != null ? b : -2;
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return -2;
                    }
                }

                @Override
                public int peek(int timeoutMs) {
                    Integer b = input.peek();
                    return b != null ? b : -2;
                }
            };
        }
    }
}
