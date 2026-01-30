/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.style;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TagsTest {

    @Test
    @DisplayName("empty() returns an empty Tags instance")
    void emptyReturnsEmptyTags() {
        Tags tags = Tags.empty();

        assertThat(tags.isEmpty()).isTrue();
        assertThat(tags.values()).isEmpty();
    }

    @Test
    @DisplayName("of(String) creates Tags with single tag")
    void ofStringCreatesSingleTag() {
        Tags tags = Tags.of("bold");

        assertThat(tags.isEmpty()).isFalse();
        assertThat(tags.contains("bold")).isTrue();
        assertThat(tags.values()).containsExactly("bold");
    }

    @Test
    @DisplayName("of(String) with null returns empty Tags")
    void ofNullStringReturnsEmpty() {
        Tags tags = Tags.of((String) null);

        assertThat(tags.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("of(String) with empty string returns empty Tags")
    void ofEmptyStringReturnsEmpty() {
        Tags tags = Tags.of("");

        assertThat(tags.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("of(String...) creates Tags with multiple tags")
    void ofVarargsCreatesMultipleTags() {
        Tags tags = Tags.of("bold", "italic", "red");

        assertThat(tags.isEmpty()).isFalse();
        assertThat(tags.contains("bold")).isTrue();
        assertThat(tags.contains("italic")).isTrue();
        assertThat(tags.contains("red")).isTrue();
        assertThat(tags.values()).containsExactlyInAnyOrder("bold", "italic", "red");
    }

    @Test
    @DisplayName("of(String...) with null or empty values filters them out")
    void ofVarargsFiltersNullAndEmpty() {
        Tags tags = Tags.of("bold", null, "", "italic");

        assertThat(tags.values()).containsExactlyInAnyOrder("bold", "italic");
    }

    @Test
    @DisplayName("of(Set) creates Tags from set")
    void ofSetCreatesTags() {
        Set<String> set = new HashSet<>();
        set.add("bold");
        set.add("italic");

        Tags tags = Tags.of(set);

        assertThat(tags.values()).containsExactlyInAnyOrder("bold", "italic");
    }

    @Test
    @DisplayName("of(Set) with null returns empty Tags")
    void ofNullSetReturnsEmpty() {
        Tags tags = Tags.of((Set<String>) null);

        assertThat(tags.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("patch() with null returns this")
    void patchWithNullReturnsThis() {
        Tags tags = Tags.of("bold");

        Tags result = tags.patch(null);

        assertThat(result).isSameAs(tags);
    }

    @Test
    @DisplayName("patch() with empty Tags returns this")
    void patchWithEmptyReturnsThis() {
        Tags tags = Tags.of("bold");

        Tags result = tags.patch(Tags.empty());

        assertThat(result).isSameAs(tags);
    }

    @Test
    @DisplayName("patch() on empty Tags returns other")
    void patchOnEmptyReturnsOther() {
        Tags other = Tags.of("bold");

        Tags result = Tags.empty().patch(other);

        assertThat(result).isSameAs(other);
    }

    @Test
    @DisplayName("patch() merges (unions) tag sets")
    void patchMergesTagSets() {
        Tags tags1 = Tags.of("bold", "red");
        Tags tags2 = Tags.of("italic", "green");

        Tags result = tags1.patch(tags2);

        assertThat(result.values()).containsExactlyInAnyOrder("bold", "red", "italic", "green");
    }

    @Test
    @DisplayName("patch() with overlapping tags produces union without duplicates")
    void patchWithOverlappingTags() {
        Tags tags1 = Tags.of("bold", "red");
        Tags tags2 = Tags.of("bold", "italic");

        Tags result = tags1.patch(tags2);

        assertThat(result.values()).containsExactlyInAnyOrder("bold", "red", "italic");
    }

    @Test
    @DisplayName("equals() returns true for equal Tags")
    void equalsForEqualTags() {
        Tags tags1 = Tags.of("bold", "italic");
        Tags tags2 = Tags.of("bold", "italic");

        assertThat(tags1).isEqualTo(tags2);
        assertThat(tags1.hashCode()).isEqualTo(tags2.hashCode());
    }

    @Test
    @DisplayName("equals() returns false for different Tags")
    void equalsForDifferentTags() {
        Tags tags1 = Tags.of("bold");
        Tags tags2 = Tags.of("italic");

        assertThat(tags1).isNotEqualTo(tags2);
    }

    @Test
    @DisplayName("toString() includes tag values")
    void toStringIncludesValues() {
        Tags tags = Tags.of("bold");

        String str = tags.toString();

        assertThat(str).contains("bold");
    }
}
