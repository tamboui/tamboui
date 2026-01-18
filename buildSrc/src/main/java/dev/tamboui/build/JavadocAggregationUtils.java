/*
 * Copyright (c) 2026 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.build;

import org.gradle.api.artifacts.Configuration;
import org.gradle.api.attributes.Category;
import org.gradle.api.attributes.Usage;
import org.gradle.api.model.ObjectFactory;

abstract class JavadocAggregationUtils {
    public static final String AGGREGATED_JAVADOC_PARTICIPANT_SOURCES = "aggregatedJavadocParticipantSources";
    public static final String AGGREGATED_JAVADOC_PARTICIPANT_DEPS = "aggregatedJavadocParticipantDependencies";

    static void configureJavadocSourcesAggregationAttributes(ObjectFactory objects, Configuration conf) {
        conf.attributes(attrs -> {
            attrs.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.class, AGGREGATED_JAVADOC_PARTICIPANT_SOURCES));
            attrs.attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.class, Category.DOCUMENTATION));
        });
    }

}
