/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.build.model;

import java.util.Set;

/**
 * Default implementation of {@link DemoModel}.
 */
public class DefaultDemoModel implements DemoModel {

    private static final long serialVersionUID = 1L;

    private final String name;
    private final String displayName;
    private final String description;
    private final String module;
    private final Set<String> tags;
    private final String projectPath;

    /**
     * Creates a new demo model.
     *
     * @param name        the project name
     * @param displayName the display name
     * @param description the description
     * @param module      the module name
     * @param tags        the tags
     * @param projectPath the Gradle project path
     */
    public DefaultDemoModel(String name, String displayName, String description,
                            String module, Set<String> tags, String projectPath) {
        this.name = name;
        this.displayName = displayName;
        this.description = description;
        this.module = module;
        this.tags = tags;
        this.projectPath = projectPath;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getModule() {
        return module;
    }

    @Override
    public Set<String> getTags() {
        return tags;
    }

    @Override
    public String getProjectPath() {
        return projectPath;
    }
}
