package com.guizmaii.zazr.collection.internal;

import static org.assertj.core.api.Assertions.assertThat;

/** The validity checker of red-black trees, shared by the tests of the tree operations and of the tree builder. */
final class RedBlackTreeValidity {

    private RedBlackTreeValidity() {
    }

    /** Asserts that {@code tree} is a valid red-black tree: black root, no red node with a red child, the same number
     *  of black nodes on every path, the {@code blackHeight} and {@code size} fields equal to the recomputed values,
     *  and the elements strictly increasing. */
    static <T> void assertValid(RedBlackTree<T> tree) {
        assertThat(tree.color()).as("root of %s", tree).isEqualTo(RedBlackTree.Color.BLACK);
        blackNodesBelow(tree);
        T previous = null;
        int count = 0;
        for (T value : tree) {
            if (previous != null) {
                assertThat(tree.comparator().compare(previous, value)).as("order of %s", tree).isNegative();
            }
            previous = value;
            count++;
        }
        assertThat(tree.size()).isEqualTo(count);
    }

    // the number of black nodes on every path from the root of `tree` down, counting the empty leaf as one
    private static <T> int blackNodesBelow(RedBlackTree<T> tree) {
        if (tree.isEmpty()) {
            return 1;
        }
        final RedBlackTreeModule.Node<T> node = (RedBlackTreeModule.Node<T>) tree;
        if (node.color == RedBlackTree.Color.RED) {
            assertThat(node.left.color()).as("red-red in %s", tree).isEqualTo(RedBlackTree.Color.BLACK);
            assertThat(node.right.color()).as("red-red in %s", tree).isEqualTo(RedBlackTree.Color.BLACK);
        }
        final int left = blackNodesBelow(node.left);
        final int right = blackNodesBelow(node.right);
        assertThat(left).as("black height of %s", tree).isEqualTo(right);
        assertThat(node.blackHeight).as("blackHeight field of %s", tree).isEqualTo(left);
        assertThat(node.size).as("size field of %s", tree).isEqualTo(node.left.size() + node.right.size() + 1);
        return left + (node.color == RedBlackTree.Color.BLACK ? 1 : 0);
    }
}
