/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.build.model;

import java.io.Serializable;
import java.util.List;

/**
 * Tooling API model containing all demo projects.
 * <p>
 * This model is used by the demo selector to discover demos
 * without parsing build files directly.
 */
public interface DemosModel extends Serializable {

    /**
     * Returns the list of all demo projects.
     *
     * @return the list of demos
     */
    List<DemoModel> getDemos();
}
