/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.toolkit.element.RenderContext;

import static dev.tamboui.toolkit.Toolkit.text;
import static org.assertj.core.api.Assertions.assertThat;

class InlineScopeElementTest {

    @Test
    @DisplayName("preferredHeight returns 0 when hidden")
    void preferredHeightReturnsZeroWhenHidden() {
        InlineScopeElement scope = new InlineScopeElement(text("Line 1"), text("Line 2"),
                text("Line 3")).visible(false);

        int height = scope.preferredHeight(80, RenderContext.empty());
        assertThat(height).isEqualTo(0);
    }

    @Test
    @DisplayName("preferredHeight returns 0 when no children")
    void preferredHeightReturnsZeroWhenNoChildren() {
        InlineScopeElement scope = new InlineScopeElement();

        int height = scope.preferredHeight(80, RenderContext.empty());
        assertThat(height).isEqualTo(0);
    }

    @Test
    @DisplayName("preferredHeight sums children heights when visible")
    void preferredHeightSumsChildrenWhenVisible() {
        InlineScopeElement scope = new InlineScopeElement(text("Line 1"), text("Line 2"),
                text("Line 3")).visible(true);

        int height = scope.preferredHeight(80, RenderContext.empty());
        // Each text element should have height 1
        assertThat(height).isEqualTo(3);
    }

    @Test
    @DisplayName("show() sets visible to true")
    void showSetsVisibleTrue() {
        InlineScopeElement scope = new InlineScopeElement().visible(false);
        scope.show();
        assertThat(scope.isVisible()).isTrue();
    }

    @Test
    @DisplayName("hide() sets visible to false")
    void hideSetsVisibleFalse() {
        InlineScopeElement scope = new InlineScopeElement().visible(true);
        scope.hide();
        assertThat(scope.isVisible()).isFalse();
    }

    @Test
    @DisplayName("constraint returns zero height when hidden")
    void constraintReturnsZeroHeightWhenHidden() {
        InlineScopeElement scope = new InlineScopeElement(text("Line 1"), text("Line 2"))
                .visible(false);

        assertThat(scope.constraint().toString()).contains("Length[value=0]");
    }

    @Test
    @DisplayName("toggling visibility changes preferred height")
    void togglingVisibilityChangesPreferredHeight() {
        InlineScopeElement scope = new InlineScopeElement(text("Line 1"), text("Line 2"),
                text("Line 3"));

        // Initially visible (default)
        int heightWhenVisible = scope.preferredHeight(80, RenderContext.empty());
        assertThat(heightWhenVisible).isEqualTo(3);

        // Hide
        scope.hide();
        int heightWhenHidden = scope.preferredHeight(80, RenderContext.empty());
        assertThat(heightWhenHidden).isEqualTo(0);

        // Show again
        scope.show();
        int heightWhenVisibleAgain = scope.preferredHeight(80, RenderContext.empty());
        assertThat(heightWhenVisibleAgain).isEqualTo(3);
    }

    @Test
    @DisplayName("nested scopes calculate height correctly")
    void nestedScopesCalculateHeightCorrectly() {
        InlineScopeElement innerScope = new InlineScopeElement(text("Inner line 1"),
                text("Inner line 2"));

        InlineScopeElement outerScope = new InlineScopeElement(text("Outer line 1"), innerScope,
                text("Outer line 3"));

        // Both visible: 1 + 2 + 1 = 4
        int heightBothVisible = outerScope.preferredHeight(80, RenderContext.empty());
        assertThat(heightBothVisible).isEqualTo(4);

        // Hide inner scope: 1 + 0 + 1 = 2
        innerScope.hide();
        int heightInnerHidden = outerScope.preferredHeight(80, RenderContext.empty());
        assertThat(heightInnerHidden).isEqualTo(2);

        // Hide outer scope: 0
        outerScope.hide();
        int heightOuterHidden = outerScope.preferredHeight(80, RenderContext.empty());
        assertThat(heightOuterHidden).isEqualTo(0);
    }
}
