/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.build.model;

import java.util.List;

/**
 * Default implementation of {@link DemosModel}.
 */
public class DefaultDemosModel implements DemosModel {

    private static final long serialVersionUID = 1L;

    private final List<DemoModel> demos;

    /**
     * Creates a new demos model.
     *
     * @param demos the list of demos
     */
    public DefaultDemosModel(List<DemoModel> demos) {
        this.demos = demos;
    }

    @Override
    public List<DemoModel> getDemos() {
        return demos;
    }
}
