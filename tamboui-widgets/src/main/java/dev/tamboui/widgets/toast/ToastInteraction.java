/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.toast;

import java.util.Objects;

/** Result of a toast interaction. */
public abstract class ToastInteraction {
    private ToastInteraction() {}

    /** No interaction occurred. */
    public static final ToastInteraction NONE = new ToastInteraction() {};

    /** Interaction indicating a toast was dismissed. */
    public static final class Dismissed extends ToastInteraction {
        private final String id;

        /**
         * Creates a dismissed interaction.
         *
         * @param id the dismissed toast identifier
         */
        public Dismissed(String id) {
            this.id = Objects.requireNonNull(id);
        }

        /**
         * Returns the dismissed toast identifier.
         *
         * @return the toast identifier
         */
        public String id() {
            return id;
        }
    }

    /** Interaction indicating the user requested copying toast text. */
    public static final class CopyRequested extends ToastInteraction {
        private final String id;
        private final String text;

        /**
         * Creates a copy-requested interaction.
         *
         * @param id the toast identifier
         * @param text the text to copy
         */
        public CopyRequested(String id, String text) {
            this.id = Objects.requireNonNull(id);
            this.text = Objects.requireNonNull(text);
        }

        /**
         * Returns the toast identifier.
         *
         * @return the toast identifier
         */
        public String id() {
            return id;
        }

        /**
         * Returns the text to copy.
         *
         * @return the copy text
         */
        public String text() {
            return text;
        }
    }
}
