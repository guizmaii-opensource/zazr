package com.guizmaii.zazr.collection.internal;

import com.guizmaii.zazr.Tuple2;
import com.guizmaii.zazr.collection.internal.RedBlackTreeModule.Empty;
import com.guizmaii.zazr.collection.internal.RedBlackTreeModule.Node;
import com.guizmaii.zazr.control.Option;
import java.util.Arrays;
import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

import static com.guizmaii.zazr.collection.internal.RedBlackTree.Color.BLACK;
import static com.guizmaii.zazr.collection.internal.RedBlackTree.Color.RED;

/**
 * Purely functional Red/Black Tree, inspired by <a href="https://github.com/kazu-yamamoto/llrbtree/blob/master/Data/Set/RBTree.hs">Kazu Yamamoto's Haskell implementation</a>.
 * <p>
 * Based on
 * <ul>
 * <li><a href="http://www.eecs.usma.edu/webs/people/okasaki/pubs.html#jfp99">Chris Okasaki, "Red-Black Trees in a Functional Setting", Journal of Functional Programming, 9(4), pp 471-477, July 1999</a></li>
 * <li>Stefan Kahrs, "Red-black trees with types", Journal of functional programming, 11(04), pp 425-432, July 2001</li>
 * </ul>
 *
 * @param <T> Component type
 * @author Daniel Dietrich
 */
public interface RedBlackTree<T extends @Nullable Object> extends Iterable<T> {

    static <T extends @Nullable Object> RedBlackTree<T> empty(Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator, "comparator is null");
        return new Empty<>(comparator);
    }

    static <T extends @Nullable Object> RedBlackTree<T> of(Comparator<? super T> comparator, T value) {
        Objects.requireNonNull(comparator, "comparator is null");
        Objects.requireNonNull(value, "TreeSet: element is null");
        final Empty<T> empty = new Empty<>(comparator);
        return new Node<>(BLACK, 1, empty, value, empty, empty);
    }

    @SafeVarargs
    static <T extends @Nullable Object> RedBlackTree<T> of(Comparator<? super T> comparator, T... values) {
        Objects.requireNonNull(comparator, "comparator is null");
        Objects.requireNonNull(values, "values is null");
        // sort-then-build, keeping the last of equal values as successive insertions would; `values` is copied, never
        // reordered
        final RedBlackTreeBuilder<T> builder = new RedBlackTreeBuilder<>(comparator, "TreeSet.Builder", values.length, false);
        for (T value : values) {
            builder.add(Objects.requireNonNull(value, "TreeSet: element is null"));
        }
        return builder.result();
    }

    @SuppressWarnings("unchecked")
    static <T extends @Nullable Object> RedBlackTree<T> ofAll(Comparator<? super T> comparator, Iterable<? extends T> values) {
        Objects.requireNonNull(comparator, "comparator is null");
        Objects.requireNonNull(values, "values is null");
        // function equality is not computable => same object check
        if (values instanceof RedBlackTree && ((RedBlackTree<T>) values).comparator() == comparator) {
            return (RedBlackTree<T>) values;
        } else {
            // sort-then-build: one array and one node per distinct element, instead of a rebalancing insert per element
            final RedBlackTreeBuilder<T> builder = new RedBlackTreeBuilder<>(comparator, "TreeSet.Builder");
            for (T value : values) {
                builder.add(Objects.requireNonNull(value, "TreeSet: element is null"));
            }
            return builder.result();
        }
    }

    /**
     * Inserts a new value into this tree.
     * <p>
     * A new tree instance is always returned. If a comparator-equal value is already present, it is replaced
     * by the given value.
     *
     * @param value A value.
     * @return A new tree containing the given value.
     */
    default RedBlackTree<T> insert(T value) {
        java.util.Objects.requireNonNull(value, "TreeSet: element is null");
        return Node.insert(this, value).color(BLACK);
    }

    /**
     * Return the {@link Color} of this Red/Black Tree node.
     * <p>
     * An empty node is {@code BLACK} by definition.
     *
     * @return Either {@code RED} or {@code BLACK}.
     */
    Color color();

    /**
     * Returns the underlying {@link java.util.Comparator} of this RedBlackTree.
     *
     * @return The comparator.
     */
    Comparator<T> comparator();

    /**
     * Checks, if this {@code RedBlackTree} contains the given {@code value}.
     *
     * @param value A value.
     * @return true, if this tree contains the value, false otherwise.
     */
    boolean contains(T value);

    /**
     * Deletes a value from this RedBlackTree.
     * <p>
     * A new instance is returned even if the value is not present in this tree, except when this tree is
     * already empty, in which case {@code this} is returned.
     *
     * @param value A value
     * @return A RedBlackTree without the given value.
     */
    default RedBlackTree<T> delete(T value) {
        final RedBlackTree<T> tree = Node.delete(this, value)._1();
        return Node.color(tree, BLACK);
    }

    default RedBlackTree<T> difference(RedBlackTree<T> tree) {
        Objects.requireNonNull(tree, "tree is null");
        if (isEmpty() || tree.isEmpty()) {
            return this;
        } else {
            final Node<T> that = (Node<T>) tree;
            final Tuple2<RedBlackTree<T>, RedBlackTree<T>> split = Node.split(this, that.value);
            return Node.merge(split._1().difference(that.left), split._2().difference(that.right));
        }
    }

    /**
     * Returns the empty instance of this RedBlackTree.
     *
     * @return An empty ReadBlackTree
     */
    RedBlackTree<T> emptyInstance();

    /**
     * Finds the value stored in this tree, if exists, by applying the underlying comparator to the tree elements and
     * the given element.
     * <p>
     * Especially the value returned may differ from the given value, even if the underlying comparator states that
     * both are equal.
     *
     * @param value A value
     * @return Some value, if this tree contains a value equal to the given value according to the underlying comparator. Otherwise None.
     */
    Option<T> find(T value);

    default RedBlackTree<T> intersection(RedBlackTree<T> tree) {
        Objects.requireNonNull(tree, "tree is null");
        if (isEmpty()) {
            return this;
        } else if (tree.isEmpty()) {
            return tree;
        } else {
            final Node<T> that = (Node<T>) tree;
            final Tuple2<RedBlackTree<T>, RedBlackTree<T>> split = Node.split(this, that.value);
            if (contains(that.value)) {
                return Node.join(split._1().intersection(that.left), that.value, split._2().intersection(that.right));
            } else {
                return Node.merge(split._1().intersection(that.left), split._2().intersection(that.right));
            }
        }
    }

    /**
     * Checks if this {@code RedBlackTree} is empty, i.e. an instance of {@code Empty}.
     *
     * @return true, if it is empty, false otherwise.
     */
    boolean isEmpty();

    /**
     * Returns the left child if this is a non-empty node, otherwise throws.
     *
     * @return The left child.
     * @throws UnsupportedOperationException if this RedBlackTree is empty
     */
    RedBlackTree<T> left();

    /**
     * Returns the maximum element of this tree according to the underlying comparator.
     *
     * @return Some element, if this is not empty, otherwise None
     */
    default Option<T> max() {
        return isEmpty() ? Option.none() : Option.some(Node.maximum((Node<T>) this));
    }

    /**
     * Returns the minimum element of this tree according to the underlying comparator.
     *
     * @return Some element, if this is not empty, otherwise None
     */
    default Option<T> min() {
        return isEmpty() ? Option.none() : Option.some(Node.minimum((Node<T>) this));
    }

    /**
     * Returns the right child if this is a non-empty node, otherwise throws.
     *
     * @return The right child.
     * @throws UnsupportedOperationException if this RedBlackTree is empty
     */
    RedBlackTree<T> right();

    /**
     * Returns the size of this tree.
     *
     * @return the number of nodes of this tree and 0 if this is the empty tree
     */
    int size();

    /**
     * Adds all of the elements of the given {@code tree} to this tree. When an element of the given tree is
     * comparator-equal to one already present in this tree, the given tree's element replaces this tree's.
     *
     * @param tree The RedBlackTree to form the union with.
     * @return A RedBlackTree that contains all distinct elements of this and the given {@code tree}
     *         (may be {@code this} or the given {@code tree} itself if the other one is empty).
     */
    default RedBlackTree<T> union(RedBlackTree<T> tree) {
        Objects.requireNonNull(tree, "tree is null");
        if (tree.isEmpty()) {
            return this;
        } else {
            final Node<T> that = (Node<T>) tree;
            if (isEmpty()) {
                return that.color(BLACK);
            } else {
                final Tuple2<RedBlackTree<T>, RedBlackTree<T>> split = Node.split(this, that.value);
                return Node.join(split._1().union(that.left), that.value, split._2().union(that.right));
            }
        }
    }

    /**
     * Returns the value of the current tree node or throws if this is empty.
     *
     * @return The value.
     * @throws NoSuchElementException if this is the empty node.
     */
    T value();

    /**
     * Returns an Iterator that iterates elements in the order induced by the underlying Comparator.
     * <p>
     * Internally an in-order traversal of the RedBlackTree is performed.
     * <p>
     * Example:
     *
     * <pre>{@code
     *       4
     *      / \
     *     2   6
     *    / \ / \
     *   1  3 5  7
     * }</pre>
     *
     * Iteration order: 1, 2, 3, 4, 5, 6, 7
     * <p>
     * See also <a href="http://n00tc0d3r.blogspot.de/2013/08/implement-iterator-for-binarytree-i-in.html">Implement Iterator for BinaryTree I (In-order)</a>.
     */
    @Override
    default Iterator<T> iterator() {
        if (isEmpty()) {
            return Iterator.empty();
        } else {
            final Node<T> that = (Node<T>) this;
            return new AbstractIterator<T>() {

                // The path of nodes whose value is still to be returned, the next one on top. A red-black tree is at
                // most twice as high as its black height, so the first array is almost always big enough; it grows
                // otherwise.
                private @Nullable Node<?>[] stack = new Node<?>[Math.max(4, 2 * that.blackHeight + 2)];
                private int depth = 0;

                {
                    pushLeftChildren(that);
                }

                @Override
                public boolean hasNext() {
                    return depth > 0;
                }

                // AbstractIterator only calls getNext() after hasNext() returned true: the top of the stack is a node
                @SuppressWarnings({"unchecked", "NullAway"})
                @Override
                public T getNext() {
                    final Node<T> node = (Node<T>) stack[--depth];
                    stack[depth] = null;
                    if (!node.right.isEmpty()) {
                        pushLeftChildren((Node<T>) node.right);
                    }
                    return node.value;
                }

                private void pushLeftChildren(Node<T> that) {
                    RedBlackTree<T> tree = that;
                    while (!tree.isEmpty()) {
                        final Node<T> node = (Node<T>) tree;
                        if (depth == stack.length) {
                            stack = Arrays.copyOf(stack, depth * 2);
                        }
                        stack[depth++] = node;
                        tree = node.left;
                    }
                }
            };
        }
    }

    /**
     * Returns a Lisp like representation of this tree.
     *
     * @return This Tree as Lisp like String.
     */
    @Override
    String toString();

    enum Color {

        RED, BLACK;

        @Override
        public String toString() {
            return (this == RED) ? "R" : "B";
        }
    }
}
