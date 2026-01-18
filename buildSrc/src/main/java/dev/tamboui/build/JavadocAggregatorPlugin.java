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
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.external.javadoc.StandardJavadocDocletOptions;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

            // Add theme stylesheet to have similar style as main docs content.
            File themeStylesheet = project.getRootProject().file("docs/src/theme/javadoc.css");
            if (themeStylesheet.exists()) {
                options.addFileOption("-add-stylesheet", themeStylesheet);
            }

            Provider<RegularFile> combinedScriptProvider = project.getLayout().getBuildDirectory().file("tmp/javadoc/tamboui-javadoc.js");
            javadoc.doFirst(task -> {
                File combinedScript = combinedScriptProvider.get().getAsFile();
                combinedScript.getParentFile().mkdirs();

                String repo = project.getProviders().gradleProperty("tamboui.githubRepo").orElse("tamboui/tamboui").get();
                String ref = project.getProviders().gradleProperty("tamboui.githubRef").orElse("main").get();

                File themeScript = project.getRootProject().file("docs/src/theme/javadoc-theme.js");
                String themeContent = "";
                if (themeScript.exists()) {
                    try {
                        themeContent = Files.readString(themeScript.toPath(), StandardCharsets.UTF_8);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to read " + themeScript, e);
                    }
                }

                // For aggregated Javadoc we can't infer the Gradle module name from document.title.
                // Generate a small mapping so the JS can build correct GitHub links.
                List<Project> participantProjects = findParticipantProjects(project);
                Map<String, String> packageToDirPath = computePackageToDirPath(participantProjects);
                String pkgMapJson = toJsonObject(packageToDirPath);

                String content =
                        "window.__TAMBOUI_GITHUB_REPO=" + "'" + repo + "'" + ";\n" +
                                "window.__TAMBOUI_GITHUB_REF=" + "'" + ref + "'" + ";\n" +
                                "window.__TAMBOUI_GITHUB_PACKAGE_TO_DIR_PATH=" + pkgMapJson + ";\n" +
                                themeContent;

                try {
                    Files.writeString(combinedScript.toPath(), content, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to write " + combinedScript, e);
                }
            });

            options.addFileOption("-add-script", combinedScriptProvider.get().getAsFile());
        });
    }

    private static List<Project> findParticipantProjects(Project project) {
        List<Project> participants = new ArrayList<>();
        project.getRootProject().getSubprojects().forEach(p -> {
            if (p == project) {
                return;
            }
            if (p.getPlugins().hasPlugin(AggregatedJavadocParticipantPlugin.class)) {
                participants.add(p);
            }
        });
        return participants;
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

    private static Map<String, String> computePackageToDirPath(List<Project> participantProjects) {
        Map<String, String> packageToDirPath = new LinkedHashMap<>();
        Set<String> ambiguousPackages = new java.util.HashSet<>();

        for (Project p : participantProjects) {
            File srcDir = new File(p.getProjectDir(), "src/main/java");
            if (!srcDir.exists()) {
                continue;
            }
            // Walk the tree with JDK APIs.
            try {
                Files.walk(srcDir.toPath())
                        .filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".java"))
                        .forEach(path -> {
                            String rel = srcDir.toPath().relativize(path).toString().replace(File.separatorChar, '/');
                            int lastSlash = rel.lastIndexOf('/');
                            String pkgPath = lastSlash >= 0 ? rel.substring(0, lastSlash) : "";
                            if (pkgPath.isEmpty()) {
                                return;
                            }
                            String pkg = pkgPath.replace('/', '.');
                            String dirPath = p.getName() + "/src/main/java/" + pkgPath;

                            String existing = packageToDirPath.get(pkg);
                            if (existing == null) {
                                if (!ambiguousPackages.contains(pkg)) {
                                    packageToDirPath.put(pkg, dirPath);
                                }
                            } else if (!existing.equals(dirPath)) {
                                packageToDirPath.remove(pkg);
                                ambiguousPackages.add(pkg);
                            }
                        });
            } catch (IOException e) {
                throw new RuntimeException("Failed to scan sources under " + srcDir, e);
            }
        }

        return packageToDirPath;
    }

    private static String toJsonObject(Map<String, String> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        for (Map.Entry<String, String> e : map.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            first = false;
            sb.append("\"").append(jsonEscape(e.getKey())).append("\":\"").append(jsonEscape(e.getValue())).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    private static String jsonEscape(String s) {
        return s
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
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
