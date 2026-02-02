/*
 * Copyright (c) 2026 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.export.svg;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.export.Encoder;
import dev.tamboui.export.Format;
import dev.tamboui.layout.Rect;

/**
 * SVG export format. Options type: {@link SvgOptions}.
 */
public final class SvgFormat implements Format<SvgOptions> {

    private static final SvgFormat INSTANCE = new SvgFormat();
    private static final Encoder<SvgOptions> ENCODER = new Encoder<SvgOptions>() {
        @Override
        public void encode(Buffer buffer, Rect region, SvgOptions options, Appendable out) {
            SvgExporter.encode(buffer, region, options, out);
        }
    };

    SvgFormat() {
    }

    @Override
    public String id() {
        return "svg";
    }

    @Override
    public SvgOptions defaultOptions() {
        return new SvgOptions();
    }

    @Override
    public Encoder<SvgOptions> encoder() {
        return ENCODER;
    }

    /**
     * Returns the singleton SVG format instance.
     *
     * @return the SVG format
     */
    public static Format<SvgOptions> instance() {
        return INSTANCE;
    }
}
