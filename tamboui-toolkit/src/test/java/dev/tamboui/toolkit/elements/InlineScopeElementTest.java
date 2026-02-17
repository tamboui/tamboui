/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.element.RenderContext;

import static dev.tamboui.assertj.BufferAssertions.assertThat;
import static dev.tamboui.toolkit.Toolkit.text;
import static org.assertj.core.api.Assertions.assertThat;

class InlineScopeElementTest {

    @Test
    @DisplayName("preferredHeight returns 0 when hidden")
    void preferredHeightReturnsZeroWhenHidden() {
        InlineScopeElement scope = new InlineScopeElement(
            text("Line 1"),
            text("Line 2"),
            text("Line 3")
        ).visible(false);

        int height = scope.preferredSize(80, -1, RenderContext.empty()).heightOr(0);
        assertThat(height).isEqualTo(0);
    }

    @Test
    @DisplayName("preferredHeight returns 0 when no children")
    void preferredHeightReturnsZeroWhenNoChildren() {
        InlineScopeElement scope = new InlineScopeElement();

        int height = scope.preferredSize(80, -1, RenderContext.empty()).heightOr(0);
        assertThat(height).isEqualTo(0);
    }

    @Test
    @DisplayName("preferredHeight sums children heights when visible")
    void preferredHeightSumsChildrenWhenVisible() {
        InlineScopeElement scope = new InlineScopeElement(
            text("Line 1"),
            text("Line 2"),
            text("Line 3")
        ).visible(true);

        int height = scope.preferredSize(80, -1, RenderContext.empty()).heightOr(0);
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
        InlineScopeElement scope = new InlineScopeElement(
            text("Line 1"),
            text("Line 2")
        ).visible(false);

        assertThat(scope.constraint().toString()).contains("Length[value=0]");
    }

    @Test
    @DisplayName("toggling visibility changes preferred height")
    void togglingVisibilityChangesPreferredHeight() {
        InlineScopeElement scope = new InlineScopeElement(
            text("Line 1"),
            text("Line 2"),
            text("Line 3")
        );

        // Initially visible (default)
        int heightWhenVisible = scope.preferredSize(80, -1, RenderContext.empty()).heightOr(0);
        assertThat(heightWhenVisible).isEqualTo(3);

        // Hide
        scope.hide();
        int heightWhenHidden = scope.preferredSize(80, -1, RenderContext.empty()).heightOr(0);
        assertThat(heightWhenHidden).isEqualTo(0);

        // Show again
        scope.show();
        int heightWhenVisibleAgain = scope.preferredSize(80, -1, RenderContext.empty()).heightOr(0);
        assertThat(heightWhenVisibleAgain).isEqualTo(3);
    }

    @Test
    @DisplayName("nested scopes calculate height correctly")
    void nestedScopesCalculateHeightCorrectly() {
        InlineScopeElement innerScope = new InlineScopeElement(
            text("Inner line 1"),
            text("Inner line 2")
        );

        InlineScopeElement outerScope = new InlineScopeElement(
            text("Outer line 1"),
            innerScope,
            text("Outer line 3")
        );

        // Both visible: 1 + 2 + 1 = 4
        int heightBothVisible = outerScope.preferredSize(80, -1, RenderContext.empty()).heightOr(0);
        assertThat(heightBothVisible).isEqualTo(4);

        // Hide inner scope: 1 + 0 + 1 = 2
        innerScope.hide();
        int heightInnerHidden = outerScope.preferredSize(80, -1, RenderContext.empty()).heightOr(0);
        assertThat(heightInnerHidden).isEqualTo(2);

        // Hide outer scope: 0
        outerScope.hide();
        int heightOuterHidden = outerScope.preferredSize(80, -1, RenderContext.empty()).heightOr(0);
        assertThat(heightOuterHidden).isEqualTo(0);
    }

    @Test
    @DisplayName("visible scope renders children vertically")
    void visibleScopeRendersChildren() {
        Rect area = new Rect(0, 0, 10, 3);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        new InlineScopeElement(
            text("AAA"),
            text("BBB"),
            text("CCC")
        ).render(frame, area, RenderContext.empty());

        assertThat(buffer).hasContent(
            "AAA       ",
            "BBB       ",
            "CCC       "
        );
    }

    @Test
    @DisplayName("hidden scope renders nothing")
    void hiddenScopeRendersNothing() {
        Rect area = new Rect(0, 0, 10, 3);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        new InlineScopeElement(
            text("AAA"),
            text("BBB"),
            text("CCC")
        ).visible(false).render(frame, area, RenderContext.empty());

        assertThat(buffer).isEqualTo(Buffer.empty(area));
    }

    @Test
    @DisplayName("preferredWidth returns max child width when visible")
    void preferredWidthReturnsMaxChildWidth() {
        InlineScopeElement scope = new InlineScopeElement(
            text("A"),
            text("BBB"),
            text("CC")
        );
        assertThat(scope.preferredSize(-1, -1, null).widthOr(0)).isEqualTo(3);
    }

    @Test
    @DisplayName("preferredWidth returns 0 when hidden")
    void preferredWidthReturnsZeroWhenHidden() {
        InlineScopeElement scope = new InlineScopeElement(
            text("Hello")
        ).visible(false);
        assertThat(scope.preferredSize(-1, -1, null).widthOr(0)).isEqualTo(0);
    }
}
