/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.ratty.protocol;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Terminal capabilities for Ratty Graphics Protocol.
 * <p>
 * Parsed from the support query response.
 *
 * @see RattyProtocol#supportQuery()
 * @see RattyProtocol#parseSupportResponse(String)
 */
public final class RattyCapabilities {

    private final int version;
    private final Set<String> formats;
    private final boolean pathSupported;
    private final boolean payloadSupported;
    private final boolean chunkSupported;
    private final boolean animSupported;
    private final boolean depthSupported;
    private final boolean colorSupported;
    private final boolean brightnessSupported;
    private final boolean transformSupported;
    private final boolean updateSupported;

    private RattyCapabilities(Builder builder) {
        this.version = builder.version;
        this.formats = Collections.unmodifiableSet(new HashSet<>(builder.formats));
        this.pathSupported = builder.pathSupported;
        this.payloadSupported = builder.payloadSupported;
        this.chunkSupported = builder.chunkSupported;
        this.animSupported = builder.animSupported;
        this.depthSupported = builder.depthSupported;
        this.colorSupported = builder.colorSupported;
        this.brightnessSupported = builder.brightnessSupported;
        this.transformSupported = builder.transformSupported;
        this.updateSupported = builder.updateSupported;
    }

    /**
     * Creates a new builder.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the protocol version.
     *
     * @return the version (e.g., 1)
     */
    public int version() {
        return version;
    }

    /**
     * Returns the set of supported formats (e.g., "obj", "glb").
     *
     * @return the supported formats
     */
    public Set<String> formats() {
        return formats;
    }

    /**
     * Returns true if path-based registration is supported.
     *
     * @return true if supported
     */
    public boolean pathSupported() {
        return pathSupported;
    }

    /**
     * Returns true if payload-based registration is supported.
     *
     * @return true if supported
     */
    public boolean payloadSupported() {
        return payloadSupported;
    }

    /**
     * Returns true if chunked payload registration is supported.
     *
     * @return true if supported
     */
    public boolean chunkSupported() {
        return chunkSupported;
    }

    /**
     * Returns true if animation is supported.
     *
     * @return true if supported
     */
    public boolean animSupported() {
        return animSupported;
    }

    /**
     * Returns true if depth (z-offset) is supported.
     *
     * @return true if supported
     */
    public boolean depthSupported() {
        return depthSupported;
    }

    /**
     * Returns true if color tinting is supported.
     *
     * @return true if supported
     */
    public boolean colorSupported() {
        return colorSupported;
    }

    /**
     * Returns true if brightness adjustment is supported.
     *
     * @return true if supported
     */
    public boolean brightnessSupported() {
        return brightnessSupported;
    }

    /**
     * Returns true if transforms (rotation, translation, scale) are supported.
     *
     * @return true if supported
     */
    public boolean transformSupported() {
        return transformSupported;
    }

    /**
     * Returns true if object updates are supported.
     *
     * @return true if supported
     */
    public boolean updateSupported() {
        return updateSupported;
    }

    @Override
    public String toString() {
        return String.format("RattyCapabilities[v=%d, formats=%s, path=%b, payload=%b, chunk=%b, " +
                "anim=%b, depth=%b, color=%b, brightness=%b, transform=%b, update=%b]",
            version, formats, pathSupported, payloadSupported, chunkSupported,
            animSupported, depthSupported, colorSupported, brightnessSupported,
            transformSupported, updateSupported);
    }

    /**
     * Builder for {@link RattyCapabilities}.
     */
    public static final class Builder {
        private int version = 1;
        private Set<String> formats = new HashSet<>();
        private boolean pathSupported;
        private boolean payloadSupported;
        private boolean chunkSupported;
        private boolean animSupported;
        private boolean depthSupported;
        private boolean colorSupported;
        private boolean brightnessSupported;
        private boolean transformSupported;
        private boolean updateSupported;

        private Builder() {
        }

        /**
         * Sets the protocol version.
         *
         * @param version the version
         * @return this builder
         */
        public Builder version(int version) {
            this.version = version;
            return this;
        }

        /**
         * Sets the supported formats.
         *
         * @param formats the format strings
         * @return this builder
         */
        public Builder formats(String... formats) {
            this.formats.addAll(Arrays.asList(formats));
            return this;
        }

        /**
         * Sets whether path-based registration is supported.
         *
         * @param supported true if supported
         * @return this builder
         */
        public Builder pathSupported(boolean supported) {
            this.pathSupported = supported;
            return this;
        }

        /**
         * Sets whether payload-based registration is supported.
         *
         * @param supported true if supported
         * @return this builder
         */
        public Builder payloadSupported(boolean supported) {
            this.payloadSupported = supported;
            return this;
        }

        /**
         * Sets whether chunked payload registration is supported.
         *
         * @param supported true if supported
         * @return this builder
         */
        public Builder chunkSupported(boolean supported) {
            this.chunkSupported = supported;
            return this;
        }

        /**
         * Sets whether animation is supported.
         *
         * @param supported true if supported
         * @return this builder
         */
        public Builder animSupported(boolean supported) {
            this.animSupported = supported;
            return this;
        }

        /**
         * Sets whether depth is supported.
         *
         * @param supported true if supported
         * @return this builder
         */
        public Builder depthSupported(boolean supported) {
            this.depthSupported = supported;
            return this;
        }

        /**
         * Sets whether color tinting is supported.
         *
         * @param supported true if supported
         * @return this builder
         */
        public Builder colorSupported(boolean supported) {
            this.colorSupported = supported;
            return this;
        }

        /**
         * Sets whether brightness adjustment is supported.
         *
         * @param supported true if supported
         * @return this builder
         */
        public Builder brightnessSupported(boolean supported) {
            this.brightnessSupported = supported;
            return this;
        }

        /**
         * Sets whether transforms are supported.
         *
         * @param supported true if supported
         * @return this builder
         */
        public Builder transformSupported(boolean supported) {
            this.transformSupported = supported;
            return this;
        }

        /**
         * Sets whether object updates are supported.
         *
         * @param supported true if supported
         * @return this builder
         */
        public Builder updateSupported(boolean supported) {
            this.updateSupported = supported;
            return this;
        }

        /**
         * Builds the capabilities.
         *
         * @return the capabilities
         */
        public RattyCapabilities build() {
            return new RattyCapabilities(this);
        }
    }
}
