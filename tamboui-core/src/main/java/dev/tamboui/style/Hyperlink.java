/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.style;

import java.util.Objects;
import java.util.Optional;

/**
 * Represents a hyperlink that can be attached to a Style.
 * <p>
 * Hyperlinks use the OSC8 protocol
 * (https://gist.github.com/egmontkob/eb114294efbcd5adb1944c9f3cb5feda) to
 * create clickable links in terminal emulators that support it.
 * <p>
 * The hyperlink consists of:
 * <ul>
 * <li>A URL - the target of the link (required)</li>
 * <li>An optional ID - used to group multiple cells into a single link</li>
 * </ul>
 * <p>
 * Example usage:
 * 
 * <pre>{@code
 * Style style = Style.create().hyperlink("https://example.com");
 * // Or with an ID to group multiple cells:
 * Style style = Style.create().hyperlink("https://example.com", "link1");
 * }</pre>
 */
public final class Hyperlink {

    private final String url;
    private final String id;

    private Hyperlink(String url, String id) {
        this.url = Objects.requireNonNull(url, "URL cannot be null");
        this.id = id;
    }

    /**
     * Creates a hyperlink with just a URL.
     *
     * @param url
     *            the URL for the hyperlink
     * @return a new Hyperlink instance
     */
    public static Hyperlink of(String url) {
        return new Hyperlink(url, null);
    }

    /**
     * Creates a hyperlink with a URL and an ID. The ID can be used to group
     * multiple cells into a single link.
     *
     * @param url
     *            the URL for the hyperlink
     * @param id
     *            the optional ID for grouping cells
     * @return a new Hyperlink instance
     */
    public static Hyperlink of(String url, String id) {
        return new Hyperlink(url, id);
    }

    /**
     * Returns the URL of this hyperlink.
     *
     * @return the URL
     */
    public String url() {
        return url;
    }

    /**
     * Returns the ID of this hyperlink, if set.
     *
     * @return the ID, or empty if not set
     */
    public Optional<String> id() {
        return Optional.ofNullable(id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Hyperlink)) {
            return false;
        }
        Hyperlink hyperlink = (Hyperlink) o;
        return url.equals(hyperlink.url) && Objects.equals(id, hyperlink.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, id);
    }

    @Override
    public String toString() {
        if (id != null) {
            return String.format("Hyperlink[url=%s, id=%s]", url, id);
        }
        return String.format("Hyperlink[url=%s]", url);
    }
}
