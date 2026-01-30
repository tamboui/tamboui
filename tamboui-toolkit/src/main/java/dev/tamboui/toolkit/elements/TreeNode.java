/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * A node in a tree data structure for use with {@link TreeElement}.
 * <p>
 * Tree nodes can contain user data, child nodes, and support lazy loading
 * of children. Nodes track their expanded/collapsed state.
 *
 * <pre>{@code
 * // Simple nodes
 * TreeNode<Void> root = TreeNode.of("Root")
 *     .add(TreeNode.of("Child 1"))
 *     .add(TreeNode.of("Child 2")
 *         .add(TreeNode.of("Grandchild")))
 *     .expanded();
 *
 * // With data
 * TreeNode<File> fileNode = TreeNode.of("src", srcDir)
 *     .childrenLoader(() -> loadChildren(srcDir));
 *
 * // Varargs children
 * TreeNode<Void> tree = TreeNode.of("Project",
 *     TreeNode.of("src").expanded(),
 *     TreeNode.of("test"),
 *     TreeNode.of("README.md").leaf());
 * }</pre>
 *
 * @param <T> the type of user data associated with this node
 */
public final class TreeNode<T> {

    private final String label;
    private final T data;
    private final List<TreeNode<T>> children;
    private Supplier<List<TreeNode<T>>> childrenLoader;
    private boolean expanded;
    private boolean leaf;
    private boolean childrenLoaded;

    private TreeNode(String label, T data) {
        this.label = label;
        this.data = data;
        this.children = new ArrayList<>();
    }

    /**
     * Creates a tree node with the given label.
     *
     * @param label the display label
     * @param <T> the data type
     * @return a new tree node
     */
    public static <T> TreeNode<T> of(String label) {
        return new TreeNode<>(label, null);
    }

    /**
     * Creates a tree node with the given label and data.
     *
     * @param label the display label
     * @param data the user data
     * @param <T> the data type
     * @return a new tree node
     */
    public static <T> TreeNode<T> of(String label, T data) {
        return new TreeNode<>(label, data);
    }

    /**
     * Creates a tree node with the given label and children.
     *
     * @param label the display label
     * @param children the child nodes
     * @param <T> the data type
     * @return a new tree node
     */
    @SafeVarargs
    @SuppressWarnings("varargs")
    public static <T> TreeNode<T> of(String label, TreeNode<T>... children) {
        TreeNode<T> node = new TreeNode<>(label, null);
        node.children.addAll(Arrays.asList(children));
        return node;
    }

    /**
     * Adds a child node.
     *
     * @param child the child to add
     * @return this node for chaining
     */
    public TreeNode<T> add(TreeNode<T> child) {
        this.children.add(child);
        return this;
    }

    /**
     * Sets this node to expanded state.
     *
     * @return this node for chaining
     */
    public TreeNode<T> expanded() {
        this.expanded = true;
        return this;
    }

    /**
     * Sets this node's expanded state.
     *
     * @param expanded true to expand, false to collapse
     * @return this node for chaining
     */
    public TreeNode<T> expanded(boolean expanded) {
        this.expanded = expanded;
        return this;
    }

    /**
     * Marks this node as a leaf (cannot have children).
     *
     * @return this node for chaining
     */
    public TreeNode<T> leaf() {
        this.leaf = true;
        return this;
    }

    /**
     * Sets a lazy children loader.
     * <p>
     * The loader is called once when the children are first accessed
     * on a node that has no statically-added children.
     *
     * @param loader the supplier that provides child nodes
     * @return this node for chaining
     */
    public TreeNode<T> childrenLoader(Supplier<List<TreeNode<T>>> loader) {
        this.childrenLoader = loader;
        return this;
    }

    /**
     * Returns the display label.
     *
     * @return the label
     */
    public String label() {
        return label;
    }

    /**
     * Returns the user data, or {@code null} if none.
     *
     * @return the data
     */
    public T data() {
        return data;
    }

    /**
     * Returns the children of this node.
     * <p>
     * If a children loader is set and no children have been added statically,
     * the loader is invoked on the first call.
     *
     * @return the list of child nodes
     */
    public List<TreeNode<T>> children() {
        if (!childrenLoaded && childrenLoader != null && children.isEmpty()) {
            List<TreeNode<T>> loaded = childrenLoader.get();
            if (loaded != null) {
                children.addAll(loaded);
            }
            childrenLoaded = true;
        }
        return Collections.unmodifiableList(children);
    }

    /**
     * Returns whether this node is expanded.
     *
     * @return true if expanded
     */
    public boolean isExpanded() {
        return expanded;
    }

    /**
     * Returns whether this node is a leaf.
     * <p>
     * A node is a leaf if explicitly marked as such, or if it has no children
     * and no children loader.
     *
     * @return true if this is a leaf node
     */
    public boolean isLeaf() {
        if (leaf) {
            return true;
        }
        return children.isEmpty() && childrenLoader == null;
    }

    /**
     * Toggles the expanded state.
     */
    public void toggleExpanded() {
        this.expanded = !this.expanded;
    }

    /**
     * Returns a string representation of this node.
     *
     * @return the label
     */
    @Override
    public String toString() {
        return label;
    }
}
