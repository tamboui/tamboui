/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.tui.error.ErrorAction;
import dev.tamboui.tui.error.RenderErrorHandlers;

import static org.assertj.core.api.Assertions.*;

class TuiConfigTest {

    @Test
    @DisplayName("defaults() creates config with sensible defaults")
    void defaultsCreatesSensibleDefaults() {
        TuiConfig config = TuiConfig.defaults();

        assertThat(config.rawMode()).isTrue();
        assertThat(config.alternateScreen()).isTrue();
        assertThat(config.hideCursor()).isTrue();
        assertThat(config.mouseCapture()).isFalse();
        assertThat(config.pollTimeout()).isEqualTo(Duration.ofMillis(TuiConfig.DEFAULT_POLL_TIMEOUT));
        assertThat(config.ticksEnabled()).isTrue();
        assertThat(config.tickRate()).isEqualTo(Duration.ofMillis(TuiConfig.DEFAULT_TICK_TIMEOUT));
        assertThat(config.shutdownHook()).isTrue();
    }

    @Test
    @DisplayName("withAnimation() enables ticks at specified rate")
    void withAnimationEnablesTicks() {
        TuiConfig config = TuiConfig.withAnimation(Duration.ofMillis(16));

        assertThat(config.ticksEnabled()).isTrue();
        assertThat(config.tickRate()).isEqualTo(Duration.ofMillis(16));
    }

    @Test
    @DisplayName("builder creates config with custom values")
    void builderCreatesCustomConfig() {
        TuiConfig config = TuiConfig.builder()
                .rawMode(false)
                .alternateScreen(false)
                .hideCursor(false)
                .mouseCapture(true)
                .pollTimeout(Duration.ofMillis(50))
                .tickRate(Duration.ofMillis(33))
                .build();

        assertThat(config.rawMode()).isFalse();
        assertThat(config.alternateScreen()).isFalse();
        assertThat(config.hideCursor()).isFalse();
        assertThat(config.mouseCapture()).isTrue();
        assertThat(config.pollTimeout()).isEqualTo(Duration.ofMillis(50));
        assertThat(config.tickRate()).isEqualTo(Duration.ofMillis(33));
        assertThat(config.ticksEnabled()).isTrue();
    }

    @Test
    @DisplayName("builder starts with default values")
    void builderStartsWithDefaults() {
        TuiConfig config = TuiConfig.builder().build();

        assertThat(config.rawMode()).isTrue();
        assertThat(config.alternateScreen()).isTrue();
        assertThat(config.hideCursor()).isTrue();
        assertThat(config.mouseCapture()).isFalse();
    }

    @Test
    @DisplayName("builder allows partial customization")
    void builderAllowsPartialCustomization() {
        TuiConfig config = TuiConfig.builder()
                .mouseCapture(true)
                .build();

        assertThat(config.rawMode()).isTrue();
        assertThat(config.mouseCapture()).isTrue();
    }

    @Test
    @DisplayName("ticksEnabled returns false when noTick() is used")
    void ticksEnabledFalseWhenNoTick() {
        TuiConfig config = TuiConfig.builder().noTick().build();
        assertThat(config.ticksEnabled()).isFalse();
        assertThat(config.tickRate()).isNull();
    }

    @Test
    @DisplayName("ticksEnabled returns true when tickRate is set")
    void ticksEnabledTrueWhenRateSet() {
        TuiConfig config = TuiConfig.builder()
                .tickRate(Duration.ofMillis(16))
                .build();
        assertThat(config.ticksEnabled()).isTrue();
    }

    @Test
    @DisplayName("shutdownHook can be disabled via builder")
    void shutdownHookCanBeDisabled() {
        TuiConfig config = TuiConfig.builder()
                .shutdownHook(false)
                .build();
        assertThat(config.shutdownHook()).isFalse();
    }

    @Test
    @DisplayName("builder defaults shutdownHook to true")
    void builderDefaultsShutdownHookToTrue() {
        TuiConfig config = TuiConfig.builder().build();
        assertThat(config.shutdownHook()).isTrue();
    }

    @Test
    @DisplayName("defaults() sets errorHandler to displayAndQuit")
    void defaultsSetErrorHandlerToDisplayAndQuit() {
        TuiConfig config = TuiConfig.defaults();

        assertThat(config.errorHandler()).isNotNull();
        // Verify it's the display-and-quit handler by checking its behavior
        ErrorAction action = config.errorHandler().handle(
                dev.tamboui.tui.error.RenderError.from(new RuntimeException("test")),
                new dev.tamboui.tui.error.ErrorContext() {
                    @Override
                    public PrintStream errorOutput() {
                        return System.err;
                    }

                    @Override
                    public void quit() {
                    }
                }
        );
        assertThat(action).isEqualTo(ErrorAction.DISPLAY_AND_QUIT);
    }

    @Test
    @DisplayName("defaults() sets errorOutput to System.err")
    void defaultsSetErrorOutputToSystemErr() {
        TuiConfig config = TuiConfig.defaults();
        assertThat(config.errorOutput()).isSameAs(System.err);
    }

    @Test
    @DisplayName("builder allows custom errorHandler")
    void builderAllowsCustomErrorHandler() {
        TuiConfig config = TuiConfig.builder()
                .errorHandler(RenderErrorHandlers.suppress())
                .build();

        assertThat(config.errorHandler()).isNotNull();
    }

    @Test
    @DisplayName("builder allows custom errorOutput")
    void builderAllowsCustomErrorOutput() {
        PrintStream customOutput = new PrintStream(new ByteArrayOutputStream());
        TuiConfig config = TuiConfig.builder()
                .errorOutput(customOutput)
                .build();

        assertThat(config.errorOutput()).isSameAs(customOutput);
    }

    @Test
    @DisplayName("builder defaults errorHandler when null is passed")
    void builderDefaultsErrorHandlerWhenNull() {
        TuiConfig config = TuiConfig.builder()
                .errorHandler(null)
                .build();

        assertThat(config.errorHandler()).isNotNull();
    }

    @Test
    @DisplayName("builder defaults errorOutput when null is passed")
    void builderDefaultsErrorOutputWhenNull() {
        TuiConfig config = TuiConfig.builder()
                .errorOutput(null)
                .build();

        assertThat(config.errorOutput()).isSameAs(System.err);
    }
}
