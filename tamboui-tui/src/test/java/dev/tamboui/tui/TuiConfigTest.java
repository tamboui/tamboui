/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Duration;

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
        assertThat(config.pollTimeout()).isEqualTo(Duration.ofMillis(100));
        assertThat(config.ticksEnabled()).isTrue();
        assertThat(config.tickRate()).isEqualTo(Duration.ofMillis(100));
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
}
