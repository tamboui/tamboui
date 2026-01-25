/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.build;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.external.javadoc.StandardJavadocDocletOptions;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

/**
 * Utility class for configuring Javadoc theming with TamboUI styles.
 * <p>
 * This class provides shared Javadoc configuration logic used by both
 * the convention plugin ({@code dev.tamboui.java-base}) and the
 * {@link JavadocAggregatorPlugin}.
 */
public class JavadocTheming {

    /**
     * Configures Javadoc theming with the standard TamboUI styles.
     *
     * @param javadoc the Javadoc task to configure
     * @param project the project context
     */
    public static void configure(Javadoc javadoc, Project project) {
        configure(javadoc, project, null);
    }

    /**
     * Configures Javadoc theming with optional extra script content.
     *
     * @param javadoc                    the Javadoc task to configure
     * @param project                    the project context
     * @param extraScriptContentProvider optional supplier for additional JavaScript content.
     *                                   The supplier is invoked at task execution time (in doFirst)
     *                                   and its result is inserted between the GitHub config and theme
     *                                   script content. Pass null for no extra content.
     */
    public static void configure(Javadoc javadoc, Project project, Supplier<String> extraScriptContentProvider) {
        Provider<String> githubRepo = project.getProviders().gradleProperty("tamboui.githubRepo").orElse("tamboui/tamboui");
        Provider<String> githubRef = project.getProviders().gradleProperty("tamboui.githubRef").orElse("main");
        File themeStylesheet = project.getRootProject().file("docs/src/theme/javadoc.css");
        File themeScript = project.getRootProject().file("docs/src/theme/javadoc-theme.js");

        // Declare inputs for proper up-to-date checking
        javadoc.getInputs().property("githubRepo", githubRepo);
        javadoc.getInputs().property("githubRef", githubRef);
        if (themeStylesheet.exists()) {
            javadoc.getInputs().file(themeStylesheet);
        }
        if (themeScript.exists()) {
            javadoc.getInputs().file(themeScript);
        }

        StandardJavadocDocletOptions options = (StandardJavadocDocletOptions) javadoc.getOptions();

        // Add theme stylesheet if it exists
        if (themeStylesheet.exists()) {
            options.addFileOption("-add-stylesheet", themeStylesheet);
        }

        // Set up combined script generation
        Path combinedScript = javadoc.getTemporaryDir().toPath().resolve("tamboui-javadoc.js");

        javadoc.doFirst(task -> {
            try {
                Files.createDirectories(combinedScript.getParent());
            } catch (IOException e) {
                throw new GradleException("Unable to create temporary directory", e);
            }

            String repo = githubRepo.get();
            String ref = githubRef.get();

            String themeContent = "";
            if (themeScript.exists()) {
                try {
                    themeContent = Files.readString(themeScript.toPath(), StandardCharsets.UTF_8);
                } catch (IOException e) {
                    throw new GradleException("Failed to read " + themeScript, e);
                }
            }

            String extraContent = "";
            if (extraScriptContentProvider != null) {
                extraContent = extraScriptContentProvider.get();
                if (extraContent == null) {
                    extraContent = "";
                }
            }

            String content =
                    "window.__TAMBOUI_GITHUB_REPO='" + repo + "';\n" +
                            "window.__TAMBOUI_GITHUB_REF='" + ref + "';\n" +
                            extraContent +
                            themeContent;

            try {
                Files.writeString(combinedScript, content, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new GradleException("Failed to write " + combinedScript, e);
            }
        });

        options.addFileOption("-add-script", combinedScript.toFile());
    }
}
