/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.logo;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.text.Line;
import dev.tamboui.text.Text;
import dev.tamboui.widget.Widget;

/**
 * A widget that renders the Tamboui logo.
 *
 * <p>
 * The Tamboui logo takes up two lines of text. This may be used in an
 * application's help or about screen to show that it is powered by Tamboui.
 *
 * <h2>Examples</h2>
 *
 * <h3>Tiny (default, 2 lines)</h3>
 *
 * <pre>{@code
 * Logo logo = Logo.tiny();
 * frame.renderWidget(logo, area);
 * }</pre>
 *
 * <p>
 * Renders:
 *
 * <pre>
 * ▜▘▗▀▖▛▜▜ ▙▄▖▗▀▖▌ ▌ ▜▘
 * ▐ ▐▀▌▌▐▐ ▙▄▘▝▄▘▝▄▘ ▟▖
 * </pre>
 *
 */
public final class Logo implements Widget {

    private final Size size;

    private Logo(Size size) {
        this.size = size;
    }

    /**
     * Create a new Tamboui logo widget.
     *
     * @param size
     *            the size of the logo
     * @return a new Tamboui logo widget
     */
    public static Logo of(Size size) {
        return new Logo(size);
    }

    /**
     * Set the size of the logo.
     *
     * @param size
     *            the size of the logo
     * @return a new Tamboui logo widget with the specified size
     */
    public Logo size(Size size) {
        return new Logo(size);
    }

    /**
     * Create a new Tamboui logo widget with a tiny size.
     *
     * @return a new Tamboui logo widget with tiny size
     */
    public static Logo tiny() {
        return new Logo(Size.TINY);
    }

    /**
     * Returns the default Tamboui logo (tiny size).
     *
     * @return a new Tamboui logo widget with tiny size
     */
    public static Logo ofDefault() {
        return tiny();
    }

    @Override
    public void render(Rect area, Buffer buffer) {
        if (area.isEmpty()) {
            return;
        }

        String logoText = size.getText();
        Text text = Text.raw(logoText);

        // Render each line of the logo
        int y = area.top();
        for (int i = 0; i < text.lines().size() && y < area.bottom(); i++) {
            Line line = text.lines().get(i);
            int x = area.left();
            buffer.setLine(x, y, line);
            y++;
        }
    }

    /**
     * The size of the logo.
     */
    public enum Size {
        /**
         * A tiny logo (2 lines).
         *
         * <pre>
         * ▜▘▗▀▖▛▜▜ ▙▄▖▗▀▖▌ ▌ ▜▘
         * ▐ ▐▀▌▌▐▐ ▙▄▘▝▄▘▝▄▘ ▟▖
         * </pre>
         */
        TINY {
            @Override
            String getText() {
                return "▜▘▗▀▖▛▜▜ ▙▄▖▗▀▖▌ ▌ ▜▘\n" + "▐ ▐▀▌▌▐▐ ▙▄▘▝▄▘▝▄▘ ▟▖\n";
            }
        };

        /**
         * Returns the text representation of the logo for this size.
         *
         * @return the logo text
         */
        abstract String getText();
    }
}
