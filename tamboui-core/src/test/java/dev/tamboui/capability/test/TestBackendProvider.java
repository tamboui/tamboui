/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.capability.test;

import dev.tamboui.buffer.CellUpdate;
import dev.tamboui.layout.Position;
import dev.tamboui.layout.Size;
import dev.tamboui.terminal.Backend;
import dev.tamboui.terminal.BackendProvider;

import java.io.IOException;

/**
 * Test {@link BackendProvider} used to validate capability reporting behavior.
 */
public final class TestBackendProvider implements BackendProvider {

    @Override
    public Backend create() {
        return new NoopBackend();
    }

    private static final class NoopBackend implements Backend {
        @Override
        public void draw(Iterable<CellUpdate> updates) {
            // no-op
        }

        @Override
        public void flush() {
            // no-op
        }

        @Override
        public void clear() {
            // no-op
        }

        @Override
        public Size size() {
            return new Size(0, 0);
        }

        @Override
        public void showCursor() {
            // no-op
        }

        @Override
        public void hideCursor() {
            // no-op
        }

        @Override
        public Position getCursorPosition() {
            return Position.ORIGIN;
        }

        @Override
        public void setCursorPosition(Position position) {
            // no-op
        }

        @Override
        public void enterAlternateScreen() {
            // no-op
        }

        @Override
        public void leaveAlternateScreen() {
            // no-op
        }

        @Override
        public void enableRawMode() {
            // no-op
        }

        @Override
        public void disableRawMode() {
            // no-op
        }

        @Override
        public void onResize(Runnable handler) {
            // no-op
        }

        @Override
        public int read(int timeoutMs) {
            return -2;
        }

        @Override
        public int peek(int timeoutMs) {
            return -2;
        }

        @Override
        public void close() throws IOException {
            // no-op
        }
    }
}


