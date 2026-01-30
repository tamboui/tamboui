/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.style;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import dev.tamboui.layout.Rect;

import static org.assertj.core.api.Assertions.assertThat;

class StyledAreaRegistryTest {

    private StyledAreaRegistry registry;

    @BeforeEach
    void setUp() {
        registry = StyledAreaRegistry.create();
    }

    @Nested
    @DisplayName("register and all")
    class RegisterAndAll {

        @Test
        @DisplayName("registers and retrieves area with tags")
        void registerAndRetrieveArea() {
            Style style = Style.EMPTY.withExtension(Tags.class, Tags.of("highlight"));
            Rect area = new Rect(10, 5, 20, 1);

            registry.register(style, area, null);

            List<StyledAreaInfo> results = registry.all();
            assertThat(results).hasSize(1);
            assertThat(results.get(0).tags().contains("highlight")).isTrue();
            assertThat(results.get(0).area()).isEqualTo(area);
        }

        @Test
        @DisplayName("registers area with multiple tags")
        void registerAreaWithMultipleTags() {
            Style style = Style.EMPTY.withExtension(Tags.class, Tags.of("highlight", "important"));
            Rect area = new Rect(0, 0, 10, 1);

            registry.register(style, area, null);

            List<StyledAreaInfo> results = registry.all();
            assertThat(results).hasSize(1);
            assertThat(results.get(0).tags().contains("highlight")).isTrue();
            assertThat(results.get(0).tags().contains("important")).isTrue();
        }

        @Test
        @DisplayName("does not register style without tags")
        void doesNotRegisterWithoutTags() {
            Style style = Style.EMPTY.red();
            registry.register(style, new Rect(0, 0, 10, 1), null);

            assertThat(registry.all()).isEmpty();
            assertThat(registry.size()).isZero();
        }

        @Test
        @DisplayName("registers with context key")
        void registerWithContextKey() {
            Style style = Style.EMPTY.withExtension(Tags.class, Tags.of("highlight"));

            registry.register(style, new Rect(0, 0, 10, 1), "panel1");

            List<StyledAreaInfo> results = registry.all();
            assertThat(results).hasSize(1);
            assertThat(results.get(0).contextKey()).isEqualTo("panel1");
        }
    }

    @Nested
    @DisplayName("size")
    class Size {

        @Test
        @DisplayName("returns count of registered areas")
        void sizeReturnsCount() {
            assertThat(registry.size()).isZero();

            Style style = Style.EMPTY.withExtension(Tags.class, Tags.of("tag"));
            registry.register(style, new Rect(0, 0, 10, 1), null);
            assertThat(registry.size()).isEqualTo(1);

            registry.register(style, new Rect(10, 0, 10, 1), null);
            assertThat(registry.size()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("clear")
    class Clear {

        @Test
        @DisplayName("removes all entries")
        void clearRemovesAllEntries() {
            Style style = Style.EMPTY.withExtension(Tags.class, Tags.of("tag"));
            registry.register(style, new Rect(0, 0, 10, 1), null);
            registry.register(style, new Rect(10, 0, 10, 1), null);

            assertThat(registry.size()).isEqualTo(2);

            registry.clear();

            assertThat(registry.size()).isZero();
            assertThat(registry.all()).isEmpty();
        }
    }

    @Nested
    @DisplayName("noop registry")
    class NoopRegistry {

        @Test
        @DisplayName("ignores registrations")
        void noopRegistryIgnoresRegistrations() {
            StyledAreaRegistry noop = StyledAreaRegistry.noop();
            Style style = Style.EMPTY.withExtension(Tags.class, Tags.of("tag"));

            noop.register(style, new Rect(0, 0, 10, 1), null);

            assertThat(noop.size()).isZero();
            assertThat(noop.all()).isEmpty();
        }
    }

    @Nested
    @DisplayName("edge cases")
    class EdgeCases {

        @Test
        @DisplayName("handles null style gracefully")
        void handlesNullStyle() {
            registry.register(null, new Rect(0, 0, 10, 1), null);
            assertThat(registry.size()).isZero();
        }

        @Test
        @DisplayName("handles null area gracefully")
        void handlesNullArea() {
            Style style = Style.EMPTY.withExtension(Tags.class, Tags.of("tag"));
            registry.register(style, null, null);
            assertThat(registry.size()).isZero();
        }
    }
}
