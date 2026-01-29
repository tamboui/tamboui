/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.spinner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A set of frames that define a spinner animation.
 * <p>
 * Each frame is a string that will be displayed in sequence to create
 * the animation effect. Frames are cycled through repeatedly.
 *
 * <pre>{@code
 * // Use a predefined style
 * SpinnerFrameSet frames = SpinnerStyle.DOTS.frameSet();
 *
 * // Create custom frames
 * SpinnerFrameSet custom = SpinnerFrameSet.of("*", "+", "x", "+");
 *
 * // Use builder for more control
 * SpinnerFrameSet custom = SpinnerFrameSet.builder()
 *     .frame("⠋")
 *     .frame("⠙")
 *     .frame("⠹")
 *     .build();
 * }</pre>
 */
public final class SpinnerFrameSet {

    private final String[] frames;

    private SpinnerFrameSet(String[] frames) {
        if (frames == null || frames.length == 0) {
            throw new IllegalArgumentException("Frames cannot be null or empty");
        }
        this.frames = frames.clone();
    }

    /**
     * Creates a frame set from the given frames.
     *
     * @param frames the frame strings
     * @return a new frame set
     * @throws IllegalArgumentException if frames is null or empty
     */
    public static SpinnerFrameSet of(String... frames) {
        return new SpinnerFrameSet(frames);
    }

    /**
     * Creates a frame set from a list of frames.
     *
     * @param frames the frame strings
     * @return a new frame set
     * @throws IllegalArgumentException if frames is null or empty
     */
    public static SpinnerFrameSet of(List<String> frames) {
        if (frames == null || frames.isEmpty()) {
            throw new IllegalArgumentException("Frames cannot be null or empty");
        }
        return new SpinnerFrameSet(frames.toArray(new String[0]));
    }

    /**
     * Creates a new builder for SpinnerFrameSet.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns all frames in this set.
     *
     * @return a copy of the frame array
     */
    public String[] frames() {
        return frames.clone();
    }

    /**
     * Returns the frame at the given index (wrapping around).
     *
     * @param index the frame index
     * @return the frame string
     */
    public String frame(int index) {
        return frames[Math.floorMod(index, frames.length)];
    }

    /**
     * Returns the number of frames in this set.
     *
     * @return the frame count
     */
    public int frameCount() {
        return frames.length;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SpinnerFrameSet)) {
            return false;
        }
        SpinnerFrameSet that = (SpinnerFrameSet) o;
        return Arrays.equals(frames, that.frames);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(frames);
    }

    @Override
    public String toString() {
        return "SpinnerFrameSet[frames=" + Arrays.toString(frames) + "]";
    }

    /**
     * Builder for SpinnerFrameSet.
     */
    public static final class Builder {
        private final List<String> frames = new ArrayList<>();

        private Builder() {
        }

        /**
         * Adds a frame to the set.
         *
         * @param frame the frame string
         * @return this builder
         */
        public Builder frame(String frame) {
            if (frame != null) {
                frames.add(frame);
            }
            return this;
        }

        /**
         * Adds multiple frames to the set.
         *
         * @param frames the frame strings
         * @return this builder
         */
        public Builder frames(String... frames) {
            if (frames != null) {
                for (String frame : frames) {
                    if (frame != null) {
                        this.frames.add(frame);
                    }
                }
            }
            return this;
        }

        /**
         * Builds the SpinnerFrameSet.
         *
         * @return the configured SpinnerFrameSet
         * @throws IllegalStateException if no frames have been added
         */
        public SpinnerFrameSet build() {
            if (frames.isEmpty()) {
                throw new IllegalStateException("At least one frame must be added");
            }
            return new SpinnerFrameSet(frames.toArray(new String[0]));
        }
    }
}
