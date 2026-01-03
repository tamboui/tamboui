/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.figlet;

/**
 * Built-in FIGlet fonts shipped with {@code tamboui-widgets}.
 */
public enum BundledFigletFont {
    MINI("mini"),
    SMALL("small"),
    STANDARD("standard"),
    SLANT("slant"),
    BIG("big");

    private final String id;

    BundledFigletFont(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public String resourcePath() {
        return "/dev/tamboui/widgets/figlet/fonts/" + id + ".flf";
    }
}

