/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.build.model;

import dev.tamboui.build.DemoExtension;
import org.gradle.api.Project;
import org.gradle.tooling.provider.model.ToolingModelBuilder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Builds the {@link DemosModel} for the Gradle Tooling API.
 * <p>
 * This builder scans all projects for the demo extension and
 * collects their metadata into a model that can be queried
 * via the Tooling API.
 */
public class DemosModelBuilder implements ToolingModelBuilder {

    private static final String DEMO_SELECTOR = "demo-selector";

    @Override
    public boolean canBuild(String modelName) {
        return DemosModel.class.getName().equals(modelName);
    }

    @Override
    public Object buildAll(String modelName, Project project) {
        List<DemoModel> demos = new ArrayList<>();

        // Iterate through all projects
        for (var p : project.getRootProject().getAllprojects()) {
            // Check if this project has the demo extension
            var extension = p.getExtensions().findByType(DemoExtension.class);
            if (extension != null && !DEMO_SELECTOR.equals(p.getName())) {
                // Extension conventions provide defaults, so .get() is safe
                demos.add(new DefaultDemoModel(
                        p.getName(),
                        extension.getDisplayName().get(),
                        extension.getDescription().get(),
                        extension.getModule().get(),
                        Set.copyOf(extension.getTags().getOrElse(Set.of())),
                        p.getPath()
                ));
            }
        }

        // Sort by module then display name
        demos.sort(Comparator.comparing(DemoModel::getModule).thenComparing(DemoModel::getDisplayName));

        return new DefaultDemosModel(demos);
    }
}
