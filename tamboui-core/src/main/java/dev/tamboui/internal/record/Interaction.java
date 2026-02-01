/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.internal.record;

import java.nio.file.Path;

/**
 * Represents a scripted interaction for demo recording.
 * This is an internal API and not part of the public contract.
 */
abstract class Interaction {

    private Interaction() {
    }

    /**
     * Wait for a specified number of milliseconds.
     */
    static final class Wait extends Interaction {
        private final int millis;

        Wait(int millis) {
            this.millis = millis;
        }

        int millis() {
            return millis;
        }
    }

    /**
     * Take a screenshot.
     */
    static final class Screenshot extends Interaction {
        private final Path path;

        Screenshot(Path path) {
            this.path = path;
        }

        Path path() {
            return path;
        }
    }

    /**
     * Simulate a key press.
     */
    static final class KeyPress extends Interaction {
        private final String key;

        KeyPress(String key) {
            this.key = key;
        }

        String key() {
            return key;
        }
    }
}
