/*
 * Copyright (c) 2026 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.export.text;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.export.Encoder;
import dev.tamboui.export.Format;
import dev.tamboui.layout.Rect;

/**
 * Plain text or ANSI-styled text export format. Options type: {@link TextOptions}.
 */
public final class TextFormat implements Format<TextOptions> {

    private static final TextFormat INSTANCE = new TextFormat();
    private static final Encoder<TextOptions> ENCODER = new Encoder<TextOptions>() {
        @Override
        public void encode(Buffer buffer, Rect region, TextOptions options, Appendable out) {
            TextExporter.encode(buffer, region, options, out);
        }
    };

    TextFormat() {
    }

    @Override
    public String id() {
        return "text";
    }

    @Override
    public TextOptions defaultOptions() {
        return new TextOptions();
    }

    @Override
    public Encoder<TextOptions> encoder() {
        return ENCODER;
    }

    /**
     * Returns the singleton text format instance.
     *
     * @return the text format
     */
    public static Format<TextOptions> instance() {
        return INSTANCE;
    }
}
