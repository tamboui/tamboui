/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.tree;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Modifier;
import dev.tamboui.style.Style;
import dev.tamboui.widgets.common.SizedWidget;
import dev.tamboui.widgets.paragraph.Paragraph;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link TreeWidget}.
 */
class TreeWidgetTest {

    @Test
    @DisplayName("TreeWidget renders with UNICODE guide style")
    void rendersWithUnicodeGuides() {
        TreeNode<Void> root = TreeNode.<Void>of("Root")
                .add(TreeNode.<Void>of("Child 1").leaf())
                .add(TreeNode.<Void>of("Child 2").leaf())
                .expanded();

        // Use model(root) so TreeNode's isExpanded/isLeaf are used
        TreeWidget<TreeNode<Void>> widget = TreeWidget.<TreeNode<Void>>builder()
                .model(root)
                .simpleNodeRenderer(node -> Paragraph.from(node.label()))
                .guideStyle(GuideStyle.UNICODE)
                .highlightSymbol("")
                .build();

        Rect area = new Rect(0, 0, 20, 3);
        Buffer buffer = Buffer.empty(area);
        TreeState state = new TreeState();

        widget.render(area, buffer, state);

        // First line should have ▼ indicator for expanded branch
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("\u25bc"); // ▼
        // Second line should have branch guide ├ (for non-last child)
        assertThat(buffer.get(0, 1).symbol()).isEqualTo("\u251c"); // ├
        // Third line should have last branch guide └
        assertThat(buffer.get(0, 2).symbol()).isEqualTo("\u2514"); // └
    }

    @Test
    @DisplayName("TreeWidget renders with ASCII guide style")
    void rendersWithAsciiGuides() {
        TreeNode<Void> root = TreeNode.<Void>of("Root")
                .add(TreeNode.<Void>of("Child 1").leaf())
                .add(TreeNode.<Void>of("Child 2").leaf())
                .expanded();

        TreeWidget<TreeNode<Void>> widget = TreeWidget.<TreeNode<Void>>builder()
                .model(root)
                .simpleNodeRenderer(node -> Paragraph.from(node.label()))
                .guideStyle(GuideStyle.ASCII)
                .highlightSymbol("")
                .build();

        Rect area = new Rect(0, 0, 20, 3);
        Buffer buffer = Buffer.empty(area);
        TreeState state = new TreeState();

        widget.render(area, buffer, state);

        // Second line should have + for branch (non-last child)
        assertThat(buffer.get(0, 1).symbol()).isEqualTo("+");
        // Third line (last child) should also have + for last branch
        assertThat(buffer.get(0, 2).symbol()).isEqualTo("+");
    }

    @Test
    @DisplayName("TreeWidget renders with NONE guide style")
    void rendersWithNoGuides() {
        TreeNode<Void> root = TreeNode.<Void>of("Root")
                .add(TreeNode.<Void>of("Child").leaf())
                .expanded();

        TreeWidget<TreeNode<Void>> widget = TreeWidget.<TreeNode<Void>>builder()
                .model(root)
                .simpleNodeRenderer(node -> Paragraph.from(node.label()))
                .guideStyle(GuideStyle.NONE)
                .highlightSymbol("")
                .build();

        Rect area = new Rect(0, 0, 12, 2);
        Buffer buffer = Buffer.empty(area);
        TreeState state = new TreeState();

        widget.render(area, buffer, state);

        // First line should have ▼ for expanded
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("\u25bc"); // ▼
        // Second line should have Child text starting at position 0 (no leaf indicator)
        assertThat(buffer.get(0, 1).symbol()).isEqualTo("C");
    }

    @Test
    @DisplayName("TreeWidget uses custom leaf indicator")
    void customLeafIndicator() {
        TreeNode<Void> leaf = TreeNode.<Void>of("Leaf").leaf();

        TreeWidget<TreeNode<Void>> widget = TreeWidget.<TreeNode<Void>>builder()
                .model(leaf)
                .simpleNodeRenderer(node -> Paragraph.from(node.label()))
                .leafIndicator("\u2022 ")  // bullet
                .highlightSymbol("")
                .build();

        Rect area = new Rect(0, 0, 10, 1);
        Buffer buffer = Buffer.empty(area);
        TreeState state = new TreeState();

        widget.render(area, buffer, state);

        // First character should be bullet
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("\u2022");
    }

    @Test
    @DisplayName("TreeWidget uses empty leaf indicator")
    void emptyLeafIndicator() {
        TreeNode<Void> leaf = TreeNode.<Void>of("Leaf").leaf();

        TreeWidget<TreeNode<Void>> widget = TreeWidget.<TreeNode<Void>>builder()
                .model(leaf)
                .simpleNodeRenderer(node -> Paragraph.from(node.label()))
                .leafIndicator("")
                .highlightSymbol("")
                .build();

        Rect area = new Rect(0, 0, 10, 1);
        Buffer buffer = Buffer.empty(area);
        TreeState state = new TreeState();

        widget.render(area, buffer, state);

        // First character should be L from Leaf (no indicator)
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("L");
    }

    @Test
    @DisplayName("TreeWidget shows expand/collapse indicators")
    void expandCollapseIndicators() {
        // Use separate roots, each as its own model
        TreeNode<Void> expanded = TreeNode.<Void>of("Expanded")
                .add(TreeNode.<Void>of("Child").leaf())
                .expanded();

        TreeWidget<TreeNode<Void>> widget = TreeWidget.<TreeNode<Void>>builder()
                .model(expanded)
                .simpleNodeRenderer(node -> Paragraph.from(node.label()))
                .highlightSymbol("")
                .build();

        Rect area = new Rect(0, 0, 20, 2);
        Buffer buffer = Buffer.empty(area);
        TreeState state = new TreeState();

        widget.render(area, buffer, state);

        // ▼ for expanded root
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("\u25bc"); // ▼
    }

    @Test
    @DisplayName("TreeWidget renders highlight symbol for selected item")
    void highlightSymbol() {
        TreeNode<Void> root = TreeNode.<Void>of("Root").leaf();

        TreeWidget<TreeNode<Void>> widget = TreeWidget.<TreeNode<Void>>builder()
                .model(root)
                .simpleNodeRenderer(node -> Paragraph.from(node.label()))
                .highlightSymbol(">> ")
                .build();

        Rect area = new Rect(0, 0, 15, 1);
        Buffer buffer = Buffer.empty(area);
        TreeState state = new TreeState();

        widget.render(area, buffer, state);

        // First two characters should be >>
        assertThat(buffer.get(0, 0).symbol()).isEqualTo(">");
        assertThat(buffer.get(1, 0).symbol()).isEqualTo(">");
    }

    @Test
    @DisplayName("TreeWidget applies highlight style to selected item")
    void highlightStyle() {
        TreeNode<Void> root = TreeNode.<Void>of("A").leaf();

        Style highlight = Style.EMPTY.reversed();
        TreeWidget<TreeNode<Void>> widget = TreeWidget.<TreeNode<Void>>builder()
                .model(root)
                .simpleNodeRenderer(node -> Paragraph.from(node.label()))
                .highlightStyle(highlight)
                .highlightSymbol("")
                .build();

        Rect area = new Rect(0, 0, 5, 1);
        Buffer buffer = Buffer.empty(area);
        TreeState state = new TreeState();

        widget.render(area, buffer, state);

        // Content area should have reversed style
        // Default leaf indicator is empty, so content starts at position 0
        assertThat(buffer.get(0, 0).style().addModifiers()).contains(Modifier.REVERSED);
    }

    @Test
    @DisplayName("TreeWidget with custom indent width")
    void customIndentWidth() {
        TreeNode<Void> root = TreeNode.<Void>of("Root")
                .add(TreeNode.<Void>of("Child").leaf())
                .expanded();

        TreeWidget<TreeNode<Void>> widget = TreeWidget.<TreeNode<Void>>builder()
                .model(root)
                .simpleNodeRenderer(node -> Paragraph.from(node.label()))
                .guideStyle(GuideStyle.UNICODE)
                .indentWidth(2)
                .highlightSymbol("")
                .build();

        Rect area = new Rect(0, 0, 16, 2);
        Buffer buffer = Buffer.empty(area);
        TreeState state = new TreeState();

        widget.render(area, buffer, state);

        // With indent=2, child's └ should be at position 0 (no parent prefix for depth-1)
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("\u25bc"); // ▼ Root at 0
        assertThat(buffer.get(0, 1).symbol()).isEqualTo("\u2514"); // └ at position 0
    }

    @Test
    @DisplayName("TreeWidget with multiple roots")
    void multipleRoots() {
        // When using roots() with leaf nodes, FunctionalTreeModel works fine
        TreeWidget<String> widget = TreeWidget.<String>builder()
                .roots("Root 1", "Root 2", "Root 3")
                .isLeaf(s -> true)
                .simpleNodeRenderer(Paragraph::from)
                .highlightSymbol("")
                .build();

        Rect area = new Rect(0, 0, 12, 3);
        Buffer buffer = Buffer.empty(area);
        TreeState state = new TreeState();

        widget.render(area, buffer, state);

        // All roots are leaves, leaf indicator is empty, so text starts at position 0
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("R"); // Root 1
        assertThat(buffer.get(0, 1).symbol()).isEqualTo("R"); // Root 2
        assertThat(buffer.get(0, 2).symbol()).isEqualTo("R"); // Root 3
    }

    @Test
    @DisplayName("TreeWidget selection navigation")
    void selectionNavigation() {
        TreeNode<Void> root = TreeNode.<Void>of("Root")
                .add(TreeNode.<Void>of("Child 1").leaf())
                .add(TreeNode.<Void>of("Child 2").leaf())
                .expanded();

        TreeWidget<TreeNode<Void>> widget = TreeWidget.<TreeNode<Void>>builder()
                .model(root)
                .simpleNodeRenderer(node -> Paragraph.from(node.label()))
                .highlightSymbol("")
                .build();

        Rect area = new Rect(0, 0, 20, 3);
        Buffer buffer = Buffer.empty(area);
        TreeState state = new TreeState();

        // Initial render
        widget.render(area, buffer, state);
        assertThat(state.selected()).isZero();

        // Navigate down
        state.selectNext(2);
        assertThat(state.selected()).isEqualTo(1);

        state.selectNext(2);
        assertThat(state.selected()).isEqualTo(2);

        // Can't go past end
        state.selectNext(2);
        assertThat(state.selected()).isEqualTo(2);

        // Navigate up
        state.selectPrevious();
        assertThat(state.selected()).isEqualTo(1);
    }

    @Test
    @DisplayName("TreeWidget lastFlatEntries provides node access")
    void lastFlatEntries() {
        TreeNode<Void> root = TreeNode.<Void>of("Root")
                .add(TreeNode.<Void>of("Child").leaf())
                .expanded();

        TreeWidget<TreeNode<Void>> widget = TreeWidget.<TreeNode<Void>>builder()
                .model(root)
                .simpleNodeRenderer(node -> Paragraph.from(node.label()))
                .build();

        Rect area = new Rect(0, 0, 20, 2);
        Buffer buffer = Buffer.empty(area);
        TreeState state = new TreeState();

        widget.render(area, buffer, state);

        List<TreeWidget.FlatEntry<TreeNode<Void>>> entries = widget.lastFlatEntries();
        assertThat(entries).hasSize(2);
        assertThat(entries.get(0).node().label()).isEqualTo("Root");
        assertThat(entries.get(0).depth()).isZero();
        assertThat(entries.get(1).node().label()).isEqualTo("Child");
        assertThat(entries.get(1).depth()).isEqualTo(1);
        assertThat(entries.get(1).parent()).isSameAs(root);
    }

    @Test
    @DisplayName("TreeWidget empty area does not render")
    void emptyAreaNoRender() {
        TreeNode<Void> root = TreeNode.<Void>of("Root").leaf();

        TreeWidget<TreeNode<Void>> widget = TreeWidget.<TreeNode<Void>>builder()
                .model(root)
                .simpleNodeRenderer(node -> Paragraph.from(node.label()))
                .build();

        Rect emptyArea = new Rect(0, 0, 0, 0);
        Buffer buffer = Buffer.empty(new Rect(0, 0, 10, 5));
        TreeState state = new TreeState();

        // Should not throw
        widget.render(emptyArea, buffer, state);
    }

    @Test
    @DisplayName("TreeWidget with SizedWidget for multi-line nodes")
    void multiLineNodes() {
        TreeNode<Void> root = TreeNode.<Void>of("Root").leaf();

        TreeWidget<TreeNode<Void>> widget = TreeWidget.<TreeNode<Void>>builder()
                .model(root)
                .nodeRenderer(node -> SizedWidget.ofHeight(Paragraph.from(node.label()), 2))
                .highlightSymbol("")
                .build();

        Rect area = new Rect(0, 0, 10, 2);
        Buffer buffer = Buffer.empty(area);
        TreeState state = new TreeState();

        widget.render(area, buffer, state);

        // Node should render at position 0 (empty leaf indicator)
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("R");
    }

    @Test
    @DisplayName("Builder requires nodeRenderer")
    void builderRequiresNodeRenderer() {
        assertThatThrownBy(() ->
            TreeWidget.<String>builder()
                    .roots("Root")
                    .build()
        ).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Builder with TreeNode model")
    void builderWithTreeNodeModel() {
        TreeNode<String> node = TreeNode.of("Root", "data").leaf();

        TreeWidget<TreeNode<String>> widget = TreeWidget.<TreeNode<String>>builder()
                .model(node)  // TreeNode implements TreeModel
                .simpleNodeRenderer(n -> Paragraph.from(n.label()))
                .highlightSymbol("")
                .build();

        Rect area = new Rect(0, 0, 10, 1);
        Buffer buffer = Buffer.empty(area);
        TreeState state = new TreeState();

        widget.render(area, buffer, state);

        // Content at position 0 (empty leaf indicator)
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("R");
    }

    @Test
    @DisplayName("Builder with functional model")
    void builderWithFunctionalModel() {
        TreeWidget<String> widget = TreeWidget.<String>builder()
                .roots("Root")
                .children(s -> s.equals("Root") ? Arrays.asList("A", "B") : Collections.emptyList())
                .isLeaf(s -> !s.equals("Root"))
                .simpleNodeRenderer(Paragraph::from)
                .highlightSymbol("")
                .build();

        Rect area = new Rect(0, 0, 10, 1);
        Buffer buffer = Buffer.empty(area);
        TreeState state = new TreeState();

        widget.render(area, buffer, state);

        // Root is not a leaf, so it has ▶ indicator (collapsed)
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("\u25b6"); // ▶
        assertThat(buffer.get(2, 0).symbol()).isEqualTo("R");
    }

    @Test
    @DisplayName("GuideStyle can be set to null and defaults to UNICODE")
    void guideStyleNull() {
        TreeNode<Void> root = TreeNode.<Void>of("Root").leaf();

        TreeWidget<TreeNode<Void>> widget = TreeWidget.<TreeNode<Void>>builder()
                .model(root)
                .simpleNodeRenderer(node -> Paragraph.from(node.label()))
                .guideStyle(null)
                .highlightSymbol("")
                .build();

        Rect area = new Rect(0, 0, 10, 1);
        Buffer buffer = Buffer.empty(area);
        TreeState state = new TreeState();

        // Should not throw
        widget.render(area, buffer, state);
    }

    @Test
    @DisplayName("LeafIndicator can be set to null and defaults to empty")
    void leafIndicatorNull() {
        TreeNode<Void> leaf = TreeNode.<Void>of("L").leaf();

        TreeWidget<TreeNode<Void>> widget = TreeWidget.<TreeNode<Void>>builder()
                .model(leaf)
                .simpleNodeRenderer(node -> Paragraph.from(node.label()))
                .leafIndicator(null)
                .highlightSymbol("")
                .build();

        Rect area = new Rect(0, 0, 5, 1);
        Buffer buffer = Buffer.empty(area);
        TreeState state = new TreeState();

        widget.render(area, buffer, state);

        // Default leaf indicator is empty, so L is at position 0
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("L");
    }

    @Test
    @DisplayName("TreeWidget with SizedWidget explicit width")
    void sizedWidgetWithWidth() {
        TreeNode<Void> root = TreeNode.<Void>of("Root").leaf();

        TreeWidget<TreeNode<Void>> widget = TreeWidget.<TreeNode<Void>>builder()
                .model(root)
                .nodeRenderer(node -> SizedWidget.ofWidth(Paragraph.from(node.label()), 5))
                .highlightSymbol("")
                .build();

        Rect area = new Rect(0, 0, 10, 1);
        Buffer buffer = Buffer.empty(area);
        TreeState state = new TreeState();

        widget.render(area, buffer, state);

        // Content renders (width is a hint, actual rendering depends on available space)
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("R");
    }

    @Test
    @DisplayName("TreeWidget with SizedWidget explicit width and height")
    void sizedWidgetWithBothDimensions() {
        TreeNode<Void> root = TreeNode.<Void>of("Root").leaf();

        TreeWidget<TreeNode<Void>> widget = TreeWidget.<TreeNode<Void>>builder()
                .model(root)
                .nodeRenderer(node -> SizedWidget.of(Paragraph.from(node.label()), 8, 2))
                .highlightSymbol("")
                .build();

        Rect area = new Rect(0, 0, 10, 3);
        Buffer buffer = Buffer.empty(area);
        TreeState state = new TreeState();

        widget.render(area, buffer, state);

        // With height=2, node should take 2 lines
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("R");
        // Second line exists for the node (may be empty)
        assertThat(buffer.get(0, 1).symbol()).isEqualTo(" ");
    }

    @Test
    @DisplayName("TreeWidget with SizedWidget default dimensions")
    void sizedWidgetDefaultDimensions() {
        TreeNode<Void> root = TreeNode.<Void>of("Root").leaf();

        TreeWidget<TreeNode<Void>> widget = TreeWidget.<TreeNode<Void>>builder()
                .model(root)
                .nodeRenderer(node -> SizedWidget.of(Paragraph.from(node.label())))
                .highlightSymbol("")
                .build();

        Rect area = new Rect(0, 0, 10, 1);
        Buffer buffer = Buffer.empty(area);
        TreeState state = new TreeState();

        widget.render(area, buffer, state);

        // Default height is 1, content starts at position 0
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("R");
    }
}
