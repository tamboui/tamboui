/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.element;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ChildPositionTest {

    @Nested
    @DisplayName("Factory method")
    class FactoryMethod {

        @Test
        @DisplayName("of() creates position with correct index and total")
        void ofCreatesPosition() {
            ChildPosition pos = ChildPosition.of(2, 5);

            assertThat(pos.index()).isEqualTo(2);
            assertThat(pos.total()).isEqualTo(5);
        }

        @Test
        @DisplayName("of() throws for negative index")
        void throwsForNegativeIndex() {
            assertThatThrownBy(() -> ChildPosition.of(-1, 5))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("index must be >= 0");
        }

        @Test
        @DisplayName("of() throws for zero total")
        void throwsForZeroTotal() {
            assertThatThrownBy(() -> ChildPosition.of(0, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("total must be > 0");
        }

        @Test
        @DisplayName("of() throws for index >= total")
        void throwsForIndexOutOfBounds() {
            assertThatThrownBy(() -> ChildPosition.of(5, 5))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("index must be < total");
        }
    }

    @Nested
    @DisplayName("Position queries")
    class PositionQueries {

        @Test
        @DisplayName("isFirst() returns true for index 0")
        void isFirstForIndexZero() {
            assertThat(ChildPosition.of(0, 5).isFirst()).isTrue();
            assertThat(ChildPosition.of(1, 5).isFirst()).isFalse();
            assertThat(ChildPosition.of(4, 5).isFirst()).isFalse();
        }

        @Test
        @DisplayName("isLast() returns true for last index")
        void isLastForLastIndex() {
            assertThat(ChildPosition.of(4, 5).isLast()).isTrue();
            assertThat(ChildPosition.of(0, 5).isLast()).isFalse();
            assertThat(ChildPosition.of(3, 5).isLast()).isFalse();
        }

        @Test
        @DisplayName("isFirst() and isLast() both true for single element")
        void singleElementIsFirstAndLast() {
            ChildPosition pos = ChildPosition.of(0, 1);
            assertThat(pos.isFirst()).isTrue();
            assertThat(pos.isLast()).isTrue();
        }

        @Test
        @DisplayName("isEven() returns true for even indices (0, 2, 4, ...)")
        void isEvenForEvenIndices() {
            assertThat(ChildPosition.of(0, 5).isEven()).isTrue();
            assertThat(ChildPosition.of(2, 5).isEven()).isTrue();
            assertThat(ChildPosition.of(4, 5).isEven()).isTrue();
            assertThat(ChildPosition.of(1, 5).isEven()).isFalse();
            assertThat(ChildPosition.of(3, 5).isEven()).isFalse();
        }

        @Test
        @DisplayName("isOdd() returns true for odd indices (1, 3, 5, ...)")
        void isOddForOddIndices() {
            assertThat(ChildPosition.of(1, 5).isOdd()).isTrue();
            assertThat(ChildPosition.of(3, 5).isOdd()).isTrue();
            assertThat(ChildPosition.of(0, 5).isOdd()).isFalse();
            assertThat(ChildPosition.of(2, 5).isOdd()).isFalse();
        }

        @Test
        @DisplayName("nthChild() returns 1-based position")
        void nthChildReturnsOneBased() {
            assertThat(ChildPosition.of(0, 5).nthChild()).isEqualTo(1);
            assertThat(ChildPosition.of(1, 5).nthChild()).isEqualTo(2);
            assertThat(ChildPosition.of(4, 5).nthChild()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("equals() and hashCode()")
    class EqualsAndHashCode {

        @Test
        @DisplayName("equals() returns true for same position")
        void equalsForSamePosition() {
            ChildPosition pos1 = ChildPosition.of(2, 5);
            ChildPosition pos2 = ChildPosition.of(2, 5);

            assertThat(pos1).isEqualTo(pos2);
        }

        @Test
        @DisplayName("equals() returns false for different index")
        void notEqualsForDifferentIndex() {
            ChildPosition pos1 = ChildPosition.of(2, 5);
            ChildPosition pos2 = ChildPosition.of(3, 5);

            assertThat(pos1).isNotEqualTo(pos2);
        }

        @Test
        @DisplayName("equals() returns false for different total")
        void notEqualsForDifferentTotal() {
            ChildPosition pos1 = ChildPosition.of(2, 5);
            ChildPosition pos2 = ChildPosition.of(2, 10);

            assertThat(pos1).isNotEqualTo(pos2);
        }

        @Test
        @DisplayName("hashCode() is same for equal positions")
        void hashCodeSameForEqualPositions() {
            ChildPosition pos1 = ChildPosition.of(2, 5);
            ChildPosition pos2 = ChildPosition.of(2, 5);

            assertThat(pos1.hashCode()).isEqualTo(pos2.hashCode());
        }
    }

    @Nested
    @DisplayName("toString()")
    class ToStringTest {

        @Test
        @DisplayName("toString() includes index and total")
        void toStringIncludesFields() {
            String str = ChildPosition.of(2, 5).toString();

            assertThat(str).contains("index=2");
            assertThat(str).contains("total=5");
        }
    }
}
