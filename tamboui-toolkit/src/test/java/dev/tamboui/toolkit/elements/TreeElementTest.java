/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Modifier;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.Toolkit;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.widgets.tree.GuideStyle;
import dev.tamboui.widgets.tree.TreeNode;

import static dev.tamboui.assertj.BufferAssertions.assertThat;
import static dev.tamboui.toolkit.Toolkit.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TreeElement}.
 */
class TreeElementTest {

    @Test
    @DisplayName("TreeElement fluent API chains correctly")
    void fluentApiChaining() {
        TreeElement<Void> element = tree(
                TreeNode.<Void>of("Root",
                        TreeNode.of("Child 1"),
                        TreeNode.of("Child 2"))
        )
                .title("Tree")
                .rounded()
                .highlightColor(Color.CYAN)
                .scrollbar()
                .guideStyle(GuideStyle.UNICODE);

        assertThat(element).isInstanceOf(TreeElement.class);
    }

    @Test
    @DisplayName("tree() creates empty element")
    void emptyTree() {
        TreeElement<Void> element = tree();
        assertThat(element).isNotNull();
    }

    @Test
    @DisplayName("tree(roots...) creates tree with roots")
    void treeWithRoots() {
        TreeElement<Void> element = tree(
                TreeNode.of("A"),
                TreeNode.of("B")
        );
        assertThat(element).isNotNull();
    }

    @Test
    @DisplayName("Empty tree renders nothing (or just border)")
    void emptyTreeRenders() {
        Rect area = new Rect(0, 0, 20, 5);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        TreeElement<Void> element = tree();
        element.render(frame, area, RenderContext.empty());
        // Should not throw
    }

    @Test
    @DisplayName("Tree renders with Unicode guide characters")
    void rendersWithUnicodeGuides() {
        TreeNode<Void> root = TreeNode.<Void>of("Root")
                .add(TreeNode.of("Child 1"))
                .add(TreeNode.of("Child 2"))
                .expanded();

        Rect area = new Rect(0, 0, 40, 10);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        tree(root).render(frame, area, RenderContext.empty());

        // Check that content was rendered
        StringBuilder firstLine = new StringBuilder();
        for (int x = 0; x < area.width(); x++) {
            firstLine.append(buffer.get(x, 0).symbol());
        }
        // First entry should contain "Root" (after highlight symbol)
        assertThat(firstLine.toString()).contains("Root");
    }

    @Test
    @DisplayName("Selection navigation up/down")
    void selectionNavigation() {
        TreeNode<Void> root = TreeNode.<Void>of("Root")
                .add(TreeNode.of("Child 1"))
                .add(TreeNode.of("Child 2"))
                .expanded();

        TreeElement<Void> element = tree(root);

        // Render to populate flat entries
        Rect area = new Rect(0, 0, 30, 10);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);
        element.render(frame, area, RenderContext.empty());

        assertThat(element.selected()).isZero();

        element.selectNext();
        assertThat(element.selected()).isEqualTo(1);

        element.selectNext();
        assertThat(element.selected()).isEqualTo(2);

        // Should not go past last item
        element.selectNext();
        assertThat(element.selected()).isEqualTo(2);

        element.selectPrevious();
        assertThat(element.selected()).isEqualTo(1);
    }

    @Test
    @DisplayName("Expand via expandSelected adds children to view")
    void expandSelectedAddsChildren() {
        TreeNode<Void> root = TreeNode.<Void>of("Root")
                .add(TreeNode.<Void>of("Child")
                        .add(TreeNode.of("Grandchild")));

        TreeElement<Void> element = tree(root);

        Rect area = new Rect(0, 0, 30, 10);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        // Initially collapsed - only root visible
        element.render(frame, area, RenderContext.empty());

        // Expand root
        element.expandSelected();
        element.render(frame, area, RenderContext.empty());

        // Now root + child should be visible
        element.selectNext();
        assertThat(element.selected()).isEqualTo(1);
        assertThat(element.selectedNode().label()).isEqualTo("Child");
    }

    @Test
    @DisplayName("Collapse via collapseSelected hides children")
    void collapseSelectedHidesChildren() {
        TreeNode<Void> root = TreeNode.<Void>of("Root")
                .add(TreeNode.of("Child 1"))
                .add(TreeNode.of("Child 2"))
                .expanded();

        TreeElement<Void> element = tree(root);

        Rect area = new Rect(0, 0, 30, 10);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);
        element.render(frame, area, RenderContext.empty());

        // Root is expanded, collapse it
        element.collapseSelected();
        element.render(frame, area, RenderContext.empty());

        // After collapsing, selectNext should not move (only root is visible)
        element.selectNext();
        assertThat(element.selected()).isZero();
    }

    @Test
    @DisplayName("Toggle via toggleSelected toggles expand state")
    void toggleSelectedToggles() {
        TreeNode<Void> root = TreeNode.<Void>of("Root")
                .add(TreeNode.of("Child"));

        TreeElement<Void> element = tree(root);

        Rect area = new Rect(0, 0, 30, 10);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);
        element.render(frame, area, RenderContext.empty());

        assertThat(root.isExpanded()).isFalse();

        element.toggleSelected();
        assertThat(root.isExpanded()).isTrue();

        element.toggleSelected();
        assertThat(root.isExpanded()).isFalse();
    }

    @Test
    @DisplayName("Toggle on leaf node does nothing")
    void toggleOnLeafDoesNothing() {
        TreeNode<Void> root = TreeNode.<Void>of("Leaf").leaf();

        TreeElement<Void> element = tree(root);

        Rect area = new Rect(0, 0, 30, 10);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);
        element.render(frame, area, RenderContext.empty());

        element.toggleSelected();
        assertThat(root.isExpanded()).isFalse();
    }

    @Test
    @DisplayName("selectFirst and selectLast work")
    void selectFirstAndLast() {
        TreeNode<Void> root = TreeNode.<Void>of("Root")
                .add(TreeNode.of("Child 1"))
                .add(TreeNode.of("Child 2"))
                .add(TreeNode.of("Child 3"))
                .expanded();

        TreeElement<Void> element = tree(root);

        Rect area = new Rect(0, 0, 30, 10);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);
        element.render(frame, area, RenderContext.empty());

        element.selectLast();
        assertThat(element.selected()).isEqualTo(3);

        element.selectFirst();
        assertThat(element.selected()).isZero();
    }

    @Test
    @DisplayName("selectedNode returns correct node")
    void selectedNodeReturnsCorrectNode() {
        TreeNode<Void> child1 = TreeNode.of("Child 1");
        TreeNode<Void> root = TreeNode.<Void>of("Root")
                .add(child1)
                .expanded();

        TreeElement<Void> element = tree(root);

        Rect area = new Rect(0, 0, 30, 10);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);
        element.render(frame, area, RenderContext.empty());

        assertThat(element.selectedNode()).isSameAs(root);

        element.selectNext();
        element.render(frame, area, RenderContext.empty());
        assertThat(element.selectedNode()).isSameAs(child1);
    }

    @Test
    @DisplayName("Scrolling when tree exceeds viewport")
    void scrollingWhenExceedsViewport() {
        TreeNode<Void> root = TreeNode.<Void>of("Root").expanded();
        for (int i = 0; i < 20; i++) {
            root.add(TreeNode.of("Item " + i));
        }

        TreeElement<Void> element = tree(root);

        Rect area = new Rect(0, 0, 30, 5);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);
        element.render(frame, area, RenderContext.empty());

        // Navigate to the bottom
        for (int i = 0; i < 15; i++) {
            element.selectNext();
        }

        // Render again - should scroll
        element.render(frame, area, RenderContext.empty());
        assertThat(element.selected()).isEqualTo(15);
    }

    @Test
    @DisplayName("Custom guide styles render correctly")
    void customGuideStyles() {
        TreeNode<Void> root = TreeNode.<Void>of("Root")
                .add(TreeNode.of("Child"))
                .expanded();

        Rect area = new Rect(0, 0, 30, 5);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        tree(root)
                .guideStyle(GuideStyle.ASCII)
                .render(frame, area, RenderContext.empty());

        // Should render without error
        assertThat(buffer).isNotNull();
    }

    @Test
    @DisplayName("NONE guide style renders no guides")
    void noneGuideStyle() {
        TreeNode<Void> root = TreeNode.<Void>of("Root")
                .add(TreeNode.of("Child"))
                .expanded();

        Rect area = new Rect(0, 0, 30, 5);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        tree(root)
                .guideStyle(GuideStyle.NONE)
                .render(frame, area, RenderContext.empty());

        assertThat(buffer).isNotNull();
    }

    @Test
    @DisplayName("Tree with title renders border")
    void treeWithTitleRendersBorder() {
        Rect area = new Rect(0, 0, 30, 5);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        tree(TreeNode.of("Item"))
                .title("My Tree")
                .rounded()
                .render(frame, area, RenderContext.empty());

        // Check for rounded border
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("\u256d");
    }

    @Test
    @DisplayName("styleAttributes exposes title")
    void styleAttributesExposesTitle() {
        assertThat(tree().title("Files").styleAttributes()).containsEntry("title", "Files");
    }

    @Test
    @DisplayName("Empty area does not render")
    void emptyAreaNoRender() {
        Rect emptyArea = new Rect(0, 0, 0, 0);
        Buffer buffer = Buffer.empty(new Rect(0, 0, 20, 10));
        Frame frame = Frame.forTesting(buffer);

        // Should not throw
        tree(TreeNode.of("Root"))
                .render(frame, emptyArea, RenderContext.empty());
    }

    @Test
    @DisplayName("Multiple roots render correctly")
    void multipleRoots() {
        Rect area = new Rect(0, 0, 30, 10);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        TreeElement<Void> element = tree(
                TreeNode.of("Root 1"),
                TreeNode.of("Root 2"),
                TreeNode.of("Root 3")
        );
        element.render(frame, area, RenderContext.empty());

        // Should have 3 entries
        element.selectLast();
        assertThat(element.selected()).isEqualTo(2);
    }

    @Test
    @DisplayName("Collapse moves to parent when already collapsed")
    void collapseMovesToParent() {
        TreeNode<Void> child = TreeNode.of("Child");
        TreeNode<Void> root = TreeNode.<Void>of("Root")
                .add(child)
                .expanded();

        TreeElement<Void> element = tree(root);

        Rect area = new Rect(0, 0, 30, 10);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);
        element.render(frame, area, RenderContext.empty());

        // Select child
        element.selectNext();
        assertThat(element.selectedNode()).isSameAs(child);

        // Collapse on child (which is a leaf) should move to parent
        element.collapseSelected();
        element.render(frame, area, RenderContext.empty());
        assertThat(element.selected()).isZero();
    }

    @Test
    @DisplayName("Expand moves to first child when already expanded")
    void expandMovesToFirstChild() {
        TreeNode<Void> child = TreeNode.of("Child");
        TreeNode<Void> root = TreeNode.<Void>of("Root")
                .add(child)
                .expanded();

        TreeElement<Void> element = tree(root);

        Rect area = new Rect(0, 0, 30, 10);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);
        element.render(frame, area, RenderContext.empty());

        // Root is already expanded, expandSelected should move to first child
        element.expandSelected();
        element.render(frame, area, RenderContext.empty());
        assertThat(element.selectedNode()).isSameAs(child);
    }

    @Test
    @DisplayName("add() appends root nodes")
    void addAppendsRoots() {
        TreeElement<Void> element = Toolkit.<Void>tree()
                .add(TreeNode.of("A"))
                .add(TreeNode.of("B"));

        Rect area = new Rect(0, 0, 30, 10);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);
        element.render(frame, area, RenderContext.empty());

        element.selectLast();
        assertThat(element.selected()).isEqualTo(1);
    }

    // ═══════════════════════════════════════════════════════════════
    // BufferAssertions tests for rendering verification
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Unicode guide characters render correctly")
    void unicodeGuideCharactersRender() {
        TreeNode<Void> root = TreeNode.<Void>of("Root")
                .add(TreeNode.<Void>of("Child 1").leaf())
                .add(TreeNode.<Void>of("Child 2").leaf())
                .expanded();

        Rect area = new Rect(0, 0, 20, 3);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        tree(root)
                .highlightSymbol("")
                .guideStyle(GuideStyle.UNICODE)
                .render(frame, area, RenderContext.empty());

        // Layout: guide(4) + label (leaf indicator is empty by default)
        assertThat(buffer).hasContent(
                "▼ Root              ",
                "├── Child 1         ",
                "└── Child 2         "
        );

        // Selected row (first) should have REVERSED style
        assertThat(buffer).at(0, 0).hasStyle(Style.EMPTY.addModifier(Modifier.REVERSED));
        // Non-selected rows should not have REVERSED style
        assertThat(buffer).at(0, 1).hasStyle(Style.EMPTY);
    }

    @Test
    @DisplayName("ASCII guide characters render correctly")
    void asciiGuideCharactersRender() {
        TreeNode<Void> root = TreeNode.<Void>of("Root")
                .add(TreeNode.<Void>of("Child 1").leaf())
                .add(TreeNode.<Void>of("Child 2").leaf())
                .expanded();

        Rect area = new Rect(0, 0, 20, 3);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        tree(root)
                .highlightSymbol("")
                .guideStyle(GuideStyle.ASCII)
                .render(frame, area, RenderContext.empty());

        assertThat(buffer).hasContent(
                "▼ Root              ",
                "+-- Child 1         ",
                "+-- Child 2         "
        );
    }

    @Test
    @DisplayName("indentWidth controls prefix width")
    void indentWidthControlsPrefixWidth() {
        TreeNode<Void> root = TreeNode.<Void>of("Root")
                .add(TreeNode.<Void>of("Child")
                        .add(TreeNode.<Void>of("GC").leaf())
                        .expanded())
                .expanded();

        Rect area = new Rect(0, 0, 16, 3);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        tree(root)
                .highlightSymbol("")
                .indentWidth(2)
                .guideStyle(GuideStyle.UNICODE)
                .render(frame, area, RenderContext.empty());

        // With indent=2: depth-1 prefix=2, depth-2 prefix=4
        assertThat(buffer).hasContent(
                "▼ Root          ",
                "└─▼ Child       ",
                "  └─GC          "
        );
    }

    @Test
    @DisplayName("Default indent width uses guide style width (4)")
    void defaultIndentWidthUsesGuideStyleWidth() {
        TreeNode<Void> root = TreeNode.<Void>of("Root")
                .add(TreeNode.<Void>of("Child")
                        .add(TreeNode.<Void>of("GC").leaf())
                        .expanded())
                .expanded();

        Rect area = new Rect(0, 0, 24, 3);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        tree(root)
                .highlightSymbol("")
                .guideStyle(GuideStyle.UNICODE)
                .render(frame, area, RenderContext.empty());

        // With default indent=4: depth-1 prefix=4, depth-2 prefix=8
        assertThat(buffer).hasContent(
                "▼ Root                  ",
                "└── ▼ Child             ",
                "    └── GC              "
        );
    }

    @Test
    @DisplayName("Expand indicator shows ▼ for expanded, ▶ for collapsed")
    void expandIndicators() {
        TreeNode<Void> expanded = TreeNode.<Void>of("Expanded")
                .add(TreeNode.<Void>of("Child").leaf())
                .expanded();
        TreeNode<Void> collapsed = TreeNode.<Void>of("Collapsed")
                .add(TreeNode.<Void>of("Hidden").leaf());

        Rect area = new Rect(0, 0, 20, 3);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        tree(expanded, collapsed)
                .highlightSymbol("")
                .render(frame, area, RenderContext.empty());

        // Child is shown under Expanded with guide character
        assertThat(buffer).hasContent(
                "▼ Expanded          ",
                "└── Child           ",
                "▶ Collapsed         "
        );
    }

    @Test
    @DisplayName("Leaf node shows no expand indicator (empty by default)")
    void leafNodeShowsSpacesForIndicator() {
        TreeNode<Void> leaf = TreeNode.<Void>of("Leaf").leaf();

        Rect area = new Rect(0, 0, 10, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        tree(leaf)
                .highlightSymbol("")
                .render(frame, area, RenderContext.empty());

        assertThat(buffer).hasContent(
                "Leaf      "
        );
    }

    @Test
    @DisplayName("Highlight symbol renders before selected item")
    void highlightSymbolRendersBeforeSelectedItem() {
        TreeNode<Void> root = TreeNode.<Void>of("A").leaf();

        Rect area = new Rect(0, 0, 10, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        tree(root)
                .highlightSymbol(">> ")
                .render(frame, area, RenderContext.empty());

        assertThat(buffer).hasContent(
                ">> A      "
        );
    }

    @Test
    @DisplayName("NONE guide style renders no guides")
    void noneGuideStyleRendersNoGuides() {
        TreeNode<Void> root = TreeNode.<Void>of("Root")
                .add(TreeNode.<Void>of("Child").leaf())
                .expanded();

        Rect area = new Rect(0, 0, 12, 2);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        tree(root)
                .highlightSymbol("")
                .guideStyle(GuideStyle.NONE)
                .render(frame, area, RenderContext.empty());

        assertThat(buffer).hasContent(
                "▼ Root      ",
                "Child       "
        );
    }

    @Test
    @DisplayName("Custom nodeRenderer renders styled content")
    void customNodeRenderer() {
        TreeNode<String> root = TreeNode.of("Root", "data").leaf();

        Rect area = new Rect(0, 0, 15, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        tree(root)
                .highlightSymbol("")
                .nodeRenderer(node -> text("[" + node.data() + "]").bold())
                .render(frame, area, RenderContext.empty());

        assertThat(buffer).hasContent(
                "[data]         "
        );
    }

    @Test
    @DisplayName("Custom nodeRenderer returning null uses label fallback")
    void nodeRendererNullFallback() {
        TreeNode<Void> root = TreeNode.<Void>of("Fallback").leaf();

        Rect area = new Rect(0, 0, 15, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        tree(root)
                .highlightSymbol("")
                .nodeRenderer(node -> null)
                .render(frame, area, RenderContext.empty());

        assertThat(buffer).hasContent(
                "Fallback       "
        );
    }

    @Test
    @DisplayName("Custom nodeRenderer with multi-line element")
    void nodeRendererMultiLine() {
        TreeNode<Void> root = TreeNode.<Void>of("Root").leaf();

        Rect area = new Rect(0, 0, 10, 2);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        tree(root)
                .highlightSymbol("")
                .nodeRenderer(node -> column(
                        text("Line 1"),
                        text("Line 2")
                ))
                .render(frame, area, RenderContext.empty());

        assertThat(buffer).hasContent(
                "Line 1    ",
                "Line 2    "
        );
    }

    @Test
    @DisplayName("Custom nodeRenderer with explicit width constraint")
    void nodeRendererWithWidth() {
        TreeNode<Void> root = TreeNode.<Void>of("Root").leaf();

        Rect area = new Rect(0, 0, 20, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        tree(root)
                .highlightSymbol("")
                .nodeRenderer(node -> text("Fixed").length(8))
                .render(frame, area, RenderContext.empty());

        // The node should render with width constraint respected
        assertThat(buffer).hasContent(
                "Fixed               "
        );
    }
}
