/*
 * Copyright (c) 2026 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.export.html;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.export.Encoder;
import dev.tamboui.export.Format;
import dev.tamboui.layout.Rect;

/**
 * HTML export format. Options type: {@link HtmlOptions}.
 */
public final class HtmlFormat implements Format<HtmlOptions> {

    private static final HtmlFormat INSTANCE = new HtmlFormat();
    private static final Encoder<HtmlOptions> ENCODER = new Encoder<HtmlOptions>() {
        @Override
        public void encode(Buffer buffer, Rect region, HtmlOptions options, Appendable out) {
            HtmlExporter.encode(buffer, region, options, out);
        }
    };

    HtmlFormat() {
    }

    @Override
    public String id() {
        return "html";
    }

    @Override
    public HtmlOptions defaultOptions() {
        return new HtmlOptions();
    }

    @Override
    public Encoder<HtmlOptions> encoder() {
        return ENCODER;
    }

    /**
     * Returns the singleton HTML format instance.
     *
     * @return the HTML format
     */
    public static Format<HtmlOptions> instance() {
        return INSTANCE;
    }
}
