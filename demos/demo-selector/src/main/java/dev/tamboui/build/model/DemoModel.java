/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.build.model;

import java.util.Set;

/**
 * Tooling API model for a single demo project.
 */
public interface DemoModel {

    /**
     * Returns the project name (e.g., "barchart-demo").
     *
     * @return the project name
     */
    String getName();

    /**
     * Returns the human-readable display name (e.g., "Bar Chart").
     *
     * @return the display name
     */
    String getDisplayName();

    /**
     * Returns the demo description.
     *
     * @return the description
     */
    String getDescription();

    /**
     * Returns the module this demo belongs to (e.g., "Widgets", "Toolkit").
     *
     * @return the module name
     */
    String getModule();

    /**
     * Returns the tags associated with this demo.
     *
     * @return the set of tags
     */
    Set<String> getTags();

    /**
     * Returns the Gradle project path (e.g., ":tamboui-widgets:demos:barchart-demo").
     *
     * @return the project path
     */
    String getProjectPath();
}
