//DEPS dev.tamboui:tamboui-toolkit:LATEST
//DEPS dev.tamboui:tamboui-jline3-backend:LATEST
/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Flex;
import dev.tamboui.toolkit.app.InlineApp;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.InlineTuiConfig;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.widgets.input.TextInputState;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static dev.tamboui.toolkit.InlineToolkit.*;
import static dev.tamboui.toolkit.Toolkit.*;

/**
 * NPM-style package installation demo using InlineApp.
 * <p>
 * This demo recreates the same multi-phase UI as InlineProgressDemo but using
 * the toolkit's element-based DSL instead of manual widget composition.
 * <p>
 * Compare the ~400 lines of InlineProgressDemo with this ~200 line version
 * to see how the toolkit simplifies inline progress displays.
 */
public class InlineToolkitDemo extends InlineApp {

    private static final String[] SPINNER = {"⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏"};

    private static final List<Package> PACKAGES = Arrays.asList(
            new Package("lodash", "4.17.21", 150),
            new Package("express", "4.18.2", 280),
            new Package("typescript", "5.3.3", 420),
            new Package("react", "18.2.0", 310),
            new Package("webpack", "5.89.0", 890)
    );

    private static final String[] NATIVE_MODULES = {"node-sass", "bcrypt", "sharp"};

    private static final int TOTAL_SIZE;
    static {
        int total = 0;
        for (Package pkg : PACKAGES) {
            total += pkg.sizeKb;
        }
        TOTAL_SIZE = total;
    }

    // State
    private Phase phase = Phase.FORM_INPUT;
    private int currentIndex = 0;
    private int pkgProgress = 0;
    private int downloadedSize = 0;
    private int spinnerIndex = 0;
    private int pauseCounter = 0;
    private boolean showDetails = true;

    // Form state
    private final TextInputState projectNameState = new TextInputState();
    private final TextInputState authorState = new TextInputState();

    private InlineToolkitDemo() {

    }

    /**
     * Demo entry point.
     * @param args the CLI arguments
     * @throws Exception on unexpected error
     */
    public static void main(String[] args) throws Exception {
        System.out.println("=== Inline Toolkit Demo ===\n");
        System.out.println("--- NPM-style Install (Toolkit Version) ---\n");
        System.out.println("npm install");
        System.out.println("Press 'd' to show/hide verbose details during install\n");
        new InlineToolkitDemo().run();
    }

    @Override
    protected int height() {
        return 6;
    }

    @Override
    protected InlineTuiConfig configure(int height) {
        return InlineTuiConfig.builder(height)
                .tickRate(Duration.ofMillis(30))
                .clearOnClose(false)
                .build();
    }

    @Override
    protected Element render() {
        return switch (phase) {
            case FORM_INPUT -> renderFormInput();
            case NPM_INSTALL -> renderNpmInstall();
            case NPM_COMPLETE -> renderNpmComplete();
            case PROMPT_CONTINUE -> renderPromptContinue();
            case NATIVE_BUILD -> renderNativeBuild();
            case BUILD_COMPLETE -> renderBuildComplete();
            case DONE -> renderDone();
        };
    }

    private Element renderFormInput() {
        return column(
                row(
                        text("Project: ").bold().fit(),
                        textInput(projectNameState)
                                .id("project")
                                .placeholder("my-project")
                                .constraint(Constraint.length(25))
                                .onSubmit(() -> runner().focusManager().focusNext())
                ).flex(Flex.START),
                row(
                        text("Author:  ").bold().fit(),
                        textInput(authorState)
                                .id("author")
                                .placeholder("Your Name")
                                .constraint(Constraint.length(25))
                                .onSubmit(this::submitForm)
                ).flex(Flex.START),
                spacer(),
                text("[Tab/Enter] Next field  [Enter on last] Start install  [q] Quit").dim()
        );
    }

    private void submitForm() {
        String project = projectNameState.text().isEmpty() ? "my-project" : projectNameState.text();
        String author = authorState.text().isEmpty() ? "anonymous" : authorState.text();
        println(text(""));
        println(row(
                text("Creating ").fit(),
                text(project).cyan().fit(),
                text(" by ").fit(),
                text(author).green().fit()
        ).flex(Flex.START));
        println(text(""));
        phase = Phase.NPM_INSTALL;
    }


    private Element renderNpmInstall() {
        Package pkg = PACKAGES.get(currentIndex);
        int currentDownloaded = downloadedSize + (pkg.sizeKb * pkgProgress / 100);

        return column(
                row(
                        text(SPINNER[spinnerIndex] + " ").cyan(),
                        text("Installing "),
                        text(pkg.name).bold()
                ).flex(Flex.START),
                gauge((double) currentDownloaded / TOTAL_SIZE).green(),
                scope(showDetails,
                        text("  resolving: " + pkg.name + "@^" + pkg.version).dim().fit(),
                        text("  registry: https://registry.npmjs.org/" + pkg.name).dim().fit()
                ),
                row(
                        text(currentIndex + "/" + PACKAGES.size()).yellow(),
                        text(" packages  "),
                        text(formatSize(currentDownloaded)).cyan(),
                        text(" / " + formatSize(TOTAL_SIZE) + "  "),
                        text("[d] " + (showDetails ? "hide" : "show") + " details").dim()
                ).flex(Flex.START)
        ).focusable().onKeyEvent(this::handleDetailsKey);
    }

    private Element renderNpmComplete() {
        return column(
                row(
                        text("added "),
                        text(String.valueOf(PACKAGES.size())).green(),
                        text(" packages in 2.3s")
                ).flex(Flex.START),
                spacer(),
                spacer(),
                spacer()
        );
    }

    private Element renderPromptContinue() {
        // Use element-level key handler - element must be focusable
        return column(
                row(
                        text("? ").cyan().bold().constraint(Constraint.fit()),
                        text("Continue with native module build? ").constraint(Constraint.fit()),
                        text("[Y/n] ").dim().constraint(Constraint.fit())
                ).flex(Flex.START),
                spacer(),
                spacer(),
                spacer()
        ).focusable().onKeyEvent(this::handlePromptKey);
    }

    private EventResult handlePromptKey(KeyEvent keyEvent) {
        char c = keyEvent.character();
        if (c == 'y' || c == 'Y' || keyEvent.isConfirm()) {
            println(text(""));
            println(text("--- Building Native Modules ---").bold());
            println(text(""));
            phase = Phase.NATIVE_BUILD;
            currentIndex = 0;
            pkgProgress = 0;
            return EventResult.HANDLED;
        } else if (c == 'n' || c == 'N') {
            println(text(""));
            println(text("Skipped native module build."));
            phase = Phase.DONE;
            pauseCounter = 0;
            return EventResult.HANDLED;
        }
        return EventResult.UNHANDLED;
    }

    private EventResult handleDetailsKey(KeyEvent keyEvent) {
        if (keyEvent.character() == 'd' || keyEvent.character() == 'D') {
            showDetails = !showDetails;
            return EventResult.HANDLED;
        }
        return EventResult.UNHANDLED;
    }

    private Element renderNativeBuild() {
        String module = NATIVE_MODULES[currentIndex];

        return column(
                row(
                        text(SPINNER[spinnerIndex] + " ").cyan(),
                        text("Compiling "),
                        text(module).bold(),
                        text("...")
                ).flex(Flex.START),
                gauge(pkgProgress / 100.0).green(),
                scope(showDetails,
                        text("  cc: " + module + "/src/binding.cc -o build/" + module + ".o").dim().fit(),
                        text("  link: build/" + module + ".node").dim().fit()
                ),
                row(
                        text((currentIndex + 1) + "/" + NATIVE_MODULES.length).yellow(),
                        text(" modules  "),
                        text("[d] " + (showDetails ? "hide" : "show") + " details").dim()
                ).flex(Flex.START)
        ).focusable().onKeyEvent(this::handleDetailsKey);
    }

    private Element renderBuildComplete() {
        return column(
                row(
                        text("✓ ").green().fit(),
                        text("Built " + NATIVE_MODULES.length + " native modules").fit()
                ).flex(Flex.START)
        );
    }

    private Element renderDone() {
        return column(
                text("Installation complete!").green().bold(),
                text("Run 'npm start' to begin.")
        );
    }

    @Override
    protected void onStart() {
        runner().scheduleRepeating(() -> {
            runner().runOnRenderThread(this::simulateWork);
        }, Duration.ofMillis(50));
    }

    private void simulateWork() {
        spinnerIndex = (spinnerIndex + 1) % SPINNER.length;

        switch (phase) {
            case FORM_INPUT -> {
                // Wait for user input
            }
            case NPM_INSTALL -> tickNpmInstall();
            case NPM_COMPLETE -> {
                pauseCounter++;
                if (pauseCounter > 15) {
                    println(text(""));
                    phase = Phase.PROMPT_CONTINUE;
                    pauseCounter = 0;
                }
            }
            case PROMPT_CONTINUE -> {
                // Wait for user input - handled by global key handler
            }
            case NATIVE_BUILD -> tickNativeBuild();
            case BUILD_COMPLETE -> {
                pauseCounter++;
                if (pauseCounter > 15) {
                    phase = Phase.DONE;
                    pauseCounter = 0;
                }
            }
            case DONE -> {
                pauseCounter++;
                if (pauseCounter > 30) {
                    quit();
                }
            }
        }
    }

    private void tickNpmInstall() {
        pkgProgress += 10;

        if (pkgProgress > 100) {
            Package pkg = PACKAGES.get(currentIndex);
            println(row(
                    text("+ ").green().constraint(Constraint.fit()),
                    text(pkg.name).constraint(Constraint.fit()),
                    text("@" + pkg.version).dim().constraint(Constraint.fit())
            ).flex(Flex.START));

            downloadedSize += pkg.sizeKb;
            currentIndex++;
            pkgProgress = 0;

            if (currentIndex >= PACKAGES.size()) {
                phase = Phase.NPM_COMPLETE;
                pauseCounter = 0;
            }
        }
    }

    private void tickNativeBuild() {
        pkgProgress += 10;

        if (pkgProgress > 100) {
            String module = NATIVE_MODULES[currentIndex];
            println(row(
                    text("✓ ").green().constraint(Constraint.fit()),
                    text("Built ").constraint(Constraint.fit()),
                    text(module).constraint(Constraint.fit())
            ).flex(Flex.START));

            currentIndex++;
            pkgProgress = 0;

            if (currentIndex >= NATIVE_MODULES.length) {
                phase = Phase.BUILD_COMPLETE;
                pauseCounter = 0;
            }
        }
    }

    private static String formatSize(int kb) {
        if (kb >= 1024) {
            return String.format("%.1f MB", kb / 1024.0);
        }
        return kb + " kB";
    }

    private enum Phase {
        FORM_INPUT,
        NPM_INSTALL,
        NPM_COMPLETE,
        PROMPT_CONTINUE,
        NATIVE_BUILD,
        BUILD_COMPLETE,
        DONE
    }

    private static class Package {
        final String name;
        final String version;
        final int sizeKb;

        Package(String name, String version, int sizeKb) {
            this.name = name;
            this.version = version;
            this.sizeKb = sizeKb;
        }
    }
}
