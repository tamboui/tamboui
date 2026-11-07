/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.internal.record;

import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

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
     * Wait until the screen shows text matching a pattern, or the timeout passes.
     */
    static final class WaitFor extends Interaction {
        private final Pattern pattern;
        private final boolean screen;
        private final int timeoutMillis;

        WaitFor(Pattern pattern, boolean screen, int timeoutMillis) {
            this.pattern = pattern;
            this.screen = screen;
            this.timeoutMillis = timeoutMillis;
        }

        Pattern pattern() {
            return pattern;
        }

        /**
         * Whether the pattern is matched against the whole screen (lines joined with newlines) rather than each line.
         */
        boolean screen() {
            return screen;
        }

        int timeoutMillis() {
            return timeoutMillis;
        }
    }

    /**
     * Hide or show the following interactions in the recording (they are played either way).
     */
    static final class Visibility extends Interaction {
        private final boolean hidden;

        Visibility(boolean hidden) {
            this.hidden = hidden;
        }

        boolean hidden() {
            return hidden;
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

    /**
     * Simulate one or more low-level mouse reports (SGR 1006 encoding). Each step is a button code, a
     * press/release flag, and a cell position; a position of {@code -1} means the centre of the screen,
     * resolved when the interaction plays.
     */
    static final class Mouse extends Interaction {
        private final List<Step> steps;

        Mouse(List<Step> steps) {
            this.steps = steps;
        }

        List<Step> steps() {
            return steps;
        }

        /** One SGR mouse report. */
        static final class Step {
            private final int buttonCode;
            private final boolean release;
            private final int x;
            private final int y;

            Step(int buttonCode, boolean release, int x, int y) {
                this.buttonCode = buttonCode;
                this.release = release;
                this.x = x;
                this.y = y;
            }

            int buttonCode() {
                return buttonCode;
            }

            boolean release() {
                return release;
            }

            int x() {
                return x;
            }

            int y() {
                return y;
            }
        }
    }
}
