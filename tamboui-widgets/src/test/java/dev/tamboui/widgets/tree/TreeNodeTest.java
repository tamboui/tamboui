/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.tree;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for {@link TreeNode}.
 */
class TreeNodeTest {

    @Test
    @DisplayName("TreeNode.of(label) creates a node with label")
    void ofLabel() {
        TreeNode<Void> node = TreeNode.of("Root");
        assertThat(node.label()).isEqualTo("Root");
        assertThat(node.data()).isNull();
        assertThat(node.children()).isEmpty();
    }

    @Test
    @DisplayName("TreeNode.of(label, data) creates a node with data")
    void ofLabelAndData() {
        TreeNode<Integer> node = TreeNode.of("Item", 42);
        assertThat(node.label()).isEqualTo("Item");
        assertThat(node.data()).isEqualTo(42);
    }

    @Test
    @DisplayName("TreeNode.of(label, children...) creates a node with children")
    void ofLabelAndChildren() {
        TreeNode<Void> node = TreeNode.of("Parent",
                TreeNode.of("Child 1"),
                TreeNode.of("Child 2")
        );
        assertThat(node.label()).isEqualTo("Parent");
        assertThat(node.children()).hasSize(2);
        assertThat(node.children().get(0).label()).isEqualTo("Child 1");
        assertThat(node.children().get(1).label()).isEqualTo("Child 2");
    }

    @Test
    @DisplayName("add() appends child nodes")
    void addChild() {
        TreeNode<Void> node = TreeNode.<Void>of("Root")
                .add(TreeNode.of("A"))
                .add(TreeNode.of("B"));

        assertThat(node.children()).hasSize(2);
    }

    @Test
    @DisplayName("expanded() sets expanded state")
    void expandedState() {
        TreeNode<Void> node = TreeNode.of("Root");
        assertThat(node.isExpanded()).isFalse();

        node.expanded();
        assertThat(node.isExpanded()).isTrue();
    }

    @Test
    @DisplayName("expanded(boolean) sets expanded state explicitly")
    void expandedBoolean() {
        TreeNode<Void> node = TreeNode.<Void>of("Root").expanded(true);
        assertThat(node.isExpanded()).isTrue();

        node.expanded(false);
        assertThat(node.isExpanded()).isFalse();
    }

    @Test
    @DisplayName("leaf() marks node as leaf")
    void leafNode() {
        TreeNode<Void> node = TreeNode.<Void>of("File").leaf();
        assertThat(node.isLeaf()).isTrue();
    }

    @Test
    @DisplayName("Node without children and no loader is a leaf")
    void emptyNodeIsLeaf() {
        TreeNode<Void> node = TreeNode.of("Empty");
        assertThat(node.isLeaf()).isTrue();
    }

    @Test
    @DisplayName("Node with children is not a leaf")
    void nodeWithChildrenIsNotLeaf() {
        TreeNode<Void> node = TreeNode.<Void>of("Parent")
                .add(TreeNode.of("Child"));
        assertThat(node.isLeaf()).isFalse();
    }

    @Test
    @DisplayName("Node with childrenLoader is not a leaf")
    void nodeWithLoaderIsNotLeaf() {
        TreeNode<Void> node = TreeNode.<Void>of("Dynamic")
                .childrenLoader(() -> Arrays.asList(TreeNode.of("Loaded")));
        assertThat(node.isLeaf()).isFalse();
    }

    @Test
    @DisplayName("childrenLoader triggers on first children() call")
    void lazyLoadingTriggersOnFirstCall() {
        boolean[] loaded = {false};
        TreeNode<Void> node = TreeNode.<Void>of("Lazy")
                .childrenLoader(() -> {
                    loaded[0] = true;
                    return Arrays.asList(TreeNode.of("Child A"), TreeNode.of("Child B"));
                });

        assertThat(loaded[0]).isFalse();

        List<TreeNode<Void>> children = node.children();
        assertThat(loaded[0]).isTrue();
        assertThat(children).hasSize(2);
        assertThat(children.get(0).label()).isEqualTo("Child A");
    }

    @Test
    @DisplayName("childrenLoader only invoked once")
    void lazyLoadingOnlyOnce() {
        int[] callCount = {0};
        TreeNode<Void> node = TreeNode.<Void>of("Lazy")
                .childrenLoader(() -> {
                    callCount[0]++;
                    return Arrays.asList(TreeNode.of("Loaded"));
                });

        node.children();
        node.children();
        assertThat(callCount[0]).isEqualTo(1);
    }

    @Test
    @DisplayName("childrenLoader not called if children already added")
    void loaderNotCalledIfChildrenExist() {
        boolean[] loaded = {false};
        TreeNode<Void> node = TreeNode.<Void>of("Parent")
                .add(TreeNode.of("Static"))
                .childrenLoader(() -> {
                    loaded[0] = true;
                    return Arrays.asList(TreeNode.of("Dynamic"));
                });

        node.children();
        assertThat(loaded[0]).isFalse();
        assertThat(node.children()).hasSize(1);
    }

    @Test
    @DisplayName("toggleExpanded toggles state")
    void toggleExpanded() {
        TreeNode<Void> node = TreeNode.of("Root");
        assertThat(node.isExpanded()).isFalse();

        node.toggleExpanded();
        assertThat(node.isExpanded()).isTrue();

        node.toggleExpanded();
        assertThat(node.isExpanded()).isFalse();
    }

    @Test
    @DisplayName("children() returns unmodifiable list")
    void childrenUnmodifiable() {
        TreeNode<Void> node = TreeNode.<Void>of("Root")
                .add(TreeNode.of("Child"));

        assertThatThrownBy(() -> node.children().add(TreeNode.of("Illegal")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("toString returns label")
    void toStringReturnsLabel() {
        assertThat(TreeNode.of("MyNode").toString()).isEqualTo("MyNode");
    }

    @Test
    @DisplayName("Fluent API chaining works end-to-end")
    void fluentApiChaining() {
        TreeNode<String> tree = TreeNode.of("Project", "project-data")
                .add(TreeNode.of("src", "src-data")
                        .add(TreeNode.<String>of("main")
                                .add(TreeNode.<String>of("App.java").leaf()))
                        .expanded())
                .add(TreeNode.<String>of("README.md").leaf())
                .expanded();

        assertThat(tree.isExpanded()).isTrue();
        assertThat(tree.children()).hasSize(2);
        assertThat(tree.children().get(0).isExpanded()).isTrue();
        assertThat(tree.children().get(0).children()).hasSize(1);
        assertThat(tree.children().get(1).isLeaf()).isTrue();
    }

    @Test
    @DisplayName("TreeNode implements TreeModel - root returns itself")
    void treeModelRoot() {
        TreeNode<Void> node = TreeNode.of("Root");
        assertThat(node.root()).isSameAs(node);
    }

    @Test
    @DisplayName("TreeNode implements TreeModel - children delegation")
    void treeModelChildren() {
        TreeNode<Void> parent = TreeNode.<Void>of("Parent")
                .add(TreeNode.of("Child 1"))
                .add(TreeNode.of("Child 2"));

        // TreeModel.children(parent) should return parent.children()
        assertThat(parent.children(parent)).hasSize(2);
    }

    @Test
    @DisplayName("TreeNode implements TreeModel - isLeaf delegation")
    void treeModelIsLeaf() {
        TreeNode<Void> leaf = TreeNode.<Void>of("Leaf").leaf();
        TreeNode<Void> branch = TreeNode.<Void>of("Branch").add(TreeNode.of("Child"));

        assertThat(leaf.isLeaf(leaf)).isTrue();
        assertThat(branch.isLeaf(branch)).isFalse();
    }

    @Test
    @DisplayName("TreeNode implements TreeModel - isExpanded/setExpanded")
    void treeModelExpanded() {
        TreeNode<Void> node = TreeNode.of("Node");
        assertThat(node.isExpanded(node)).isFalse();

        node.setExpanded(node, true);
        assertThat(node.isExpanded(node)).isTrue();

        node.setExpanded(node, false);
        assertThat(node.isExpanded(node)).isFalse();
    }
}
