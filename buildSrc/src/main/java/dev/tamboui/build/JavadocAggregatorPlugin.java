/*
 * Copyright (c) 2026 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.build;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.attributes.AttributeCompatibilityRule;
import org.gradle.api.attributes.AttributeDisambiguationRule;
import org.gradle.api.attributes.Bundling;
import org.gradle.api.attributes.Category;
import org.gradle.api.attributes.CompatibilityCheckDetails;
import org.gradle.api.attributes.MultipleCandidatesDetails;
import org.gradle.api.attributes.Usage;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.external.javadoc.StandardJavadocDocletOptions;

import java.util.Set;

public abstract class JavadocAggregatorPlugin implements Plugin<Project> {

    @SuppressWarnings("deprecation")
    @Override
    public void apply(Project project) {
        project.getDependencies().getAttributesSchema().attribute(Usage.USAGE_ATTRIBUTE).getCompatibilityRules().add(AggregationCompatibilityRule.class);
        project.getDependencies().getAttributesSchema().attribute(Usage.USAGE_ATTRIBUTE).getDisambiguationRules().add(AggregationDisambiguationRule.class);
        Configuration javadocAggregatorBase = createAggregationConfigurationBase(project);
        Configuration javadocAggregator = createAggregationConfiguration(project, javadocAggregatorBase);
        Configuration javadocAggregatorClasspath = createAggregationConfigurationClasspath(project, javadocAggregatorBase);
        project.getRootProject().getSubprojects().forEach(subproject -> {
            if (project != subproject) {
                project.evaluationDependsOn(subproject.getPath());
                subproject.getPlugins().withType(AggregatedJavadocParticipantPlugin.class, plugin -> {
                    javadocAggregatorBase.getDependencies().add(
                            project.getDependencies().create(subproject)
                    );
                });
            }
        });
        Provider<Javadoc> javadocProvider = project.getTasks().register("javadoc", Javadoc.class, javadoc -> {
            javadoc.setDescription("Generate javadocs from all child projects as if it was a single project");
            javadoc.setGroup("Documentation");

            javadoc.setDestinationDir(project.getLayout().getBuildDirectory().dir("aggregated-javadocs").get().getAsFile());
            javadoc.setTitle(project.getName() + " " + project.getVersion() + " API");
            StandardJavadocDocletOptions options = (StandardJavadocDocletOptions) javadoc.getOptions();
            options.author(true);
            options.setSource("25");
            options.addBooleanOption("notimestamp", true);
            javadoc.setSource(javadocAggregator);
            javadoc.setClasspath(javadocAggregatorClasspath);
        });
    }

    private Configuration createAggregationConfiguration(Project project, Configuration javadocAggregatorBase) {
        return project.getConfigurations().create("javadocAggregator", conf -> {
            conf.setCanBeConsumed(false);
            conf.setCanBeResolved(true);
            conf.extendsFrom(javadocAggregatorBase);
            JavadocAggregationUtils.configureJavadocSourcesAggregationAttributes(project.getObjects(), conf);
        });
    }

    private Configuration createAggregationConfigurationClasspath(Project project, Configuration javadocAggregatorBase) {
        return project.getConfigurations().create("javadocAggregatorClasspath", conf -> {
            conf.setCanBeConsumed(false);
            conf.setCanBeResolved(true);
            conf.extendsFrom(javadocAggregatorBase);
            conf.attributes(attrs -> {
                attrs.attribute(Usage.USAGE_ATTRIBUTE, project.getObjects().named(Usage.class, JavadocAggregationUtils.AGGREGATED_JAVADOC_PARTICIPANT_DEPS));
                attrs.attribute(Category.CATEGORY_ATTRIBUTE, project.getObjects().named(Category.class, Category.LIBRARY));
                attrs.attribute(Bundling.BUNDLING_ATTRIBUTE, project.getObjects().named(Bundling.class, Bundling.EXTERNAL));
            });
        });
    }

    private Configuration createAggregationConfigurationBase(Project project) {
        return project.getConfigurations().create("javadocAggregatorBase", conf -> {
            conf.setCanBeConsumed(false);
            conf.setCanBeResolved(false);
        });
    }

    public static class AggregationCompatibilityRule implements AttributeCompatibilityRule<Usage> {
        private static final Set<String> COMPATIBLE_VALUES = Set.of(Usage.JAVA_API, Usage.JAVA_RUNTIME);

        @Override
        public void execute(CompatibilityCheckDetails<Usage> details) {
            Usage consumerValue = details.getConsumerValue();
            if (consumerValue != null && consumerValue.getName().equals(JavadocAggregationUtils.AGGREGATED_JAVADOC_PARTICIPANT_DEPS)) {
                Usage producerValue = details.getProducerValue();
                if (producerValue != null && COMPATIBLE_VALUES.contains(producerValue.getName())) {
                    details.compatible();
                }
            }
        }
    }

    public static class AggregationDisambiguationRule implements AttributeDisambiguationRule<Usage> {

        @Override
        public void execute(MultipleCandidatesDetails<Usage> details) {
            for (Usage candidateValue : details.getCandidateValues()) {
                if (candidateValue.getName().equals(Usage.JAVA_RUNTIME)) {
                    details.closestMatch(candidateValue);
                    return;
                }
            }
            for (Usage candidateValue : details.getCandidateValues()) {
                if (candidateValue.getName().equals(Usage.JAVA_API)) {
                    details.closestMatch(candidateValue);
                    return;
                }
            }
        }
    }
}
