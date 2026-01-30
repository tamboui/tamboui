/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit;

import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.elements.InlineScopeElement;

/**
 * DSL factory for inline-specific elements.
 * <p>
 * This class provides static factory methods for elements designed for inline
 * display mode (non-alternate-screen rendering). Use these alongside the
 * general-purpose methods in {@link Toolkit}.
 *
 * <pre>{@code
 * import static dev.tamboui.toolkit.InlineToolkit.*;
 * import static dev.tamboui.toolkit.Toolkit.*;
 *
 * Element ui = column(text("Status: downloading").cyan(),
 *         scope(downloading, row(text("file1.zip: "), gauge(progress[0])),
 *                 row(text("file2.zip: "), gauge(progress[1]))),
 *         text("Footer").dim());
 * }</pre>
 *
 * @see Toolkit
 * @see InlineScopeElement
 */
public final class InlineToolkit {

    private InlineToolkit() {
    }

    /**
     * Creates an inline scope element with the given children.
     * <p>
     * A scope is a container that can be shown or hidden dynamically. When hidden,
     * it collapses to zero height.
     *
     * <pre>{@code
     * scope(row(text("file1.zip: "), gauge(progress[0])), row(text("file2.zip: "), gauge(progress[1])))
     *         .visible(downloading)
     * }</pre>
     *
     * @param children
     *            the child elements
     * @return a new scope element
     */
    public static InlineScopeElement scope(Element... children) {
        return new InlineScopeElement(children);
    }

    /**
     * Creates an inline scope element with the given visibility and children.
     * <p>
     * Convenience method to set visibility at creation time.
     *
     * <pre>{@code
     * scope(downloading, row(text("file1.zip: "), gauge(progress[0])),
     *         row(text("file2.zip: "), gauge(progress[1])))
     * }</pre>
     *
     * @param visible
     *            whether the scope is visible
     * @param children
     *            the child elements
     * @return a new scope element
     */
    public static InlineScopeElement scope(boolean visible, Element... children) {
        return new InlineScopeElement(children).visible(visible);
    }
}
