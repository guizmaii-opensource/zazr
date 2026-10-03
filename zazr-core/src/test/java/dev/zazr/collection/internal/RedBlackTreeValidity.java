package dev.zazr.collection.internal;

/** The validity checker of red-black trees, shared by the tests of the tree operations, of the tree builder and of
 *  TreeSet and TreeMap (which reach their tree by reflection). */
public final class RedBlackTreeValidity {

    private RedBlackTreeValidity() {}

    /** Asserts that {@code tree} is a valid red-black tree: black root, no red node with a red child, the same number
     *  of black nodes on every path, the {@code blackHeight} and {@code size} fields equal to the recomputed values,
     *  and the elements strictly increasing. */
    public static <T> void assertValid(RedBlackTree<T> tree) {
        if (tree.color() != RedBlackTree.Color.BLACK) {
            throw new AssertionError("root of " + tree + " is " + tree.color() + ", expected BLACK");
        }
        blackNodesBelow(tree);
        java.util.List<T> values = new java.util.ArrayList<>();
        for (T value : tree) {
            values.add(value);
        }
        for (int i = 1; i < values.size(); i++) {
            if (tree.comparator().compare(values.get(i - 1), values.get(i)) >= 0) {
                throw new AssertionError(
                        "order of " + tree + ": " + values.get(i - 1) + " is not before " + values.get(i));
            }
        }
        if (tree.size() != values.size()) {
            throw new AssertionError("size of " + tree + " is " + tree.size() + ", iterated " + values.size());
        }
    }

    // the number of black nodes on every path from the root of `tree` down, counting the empty leaf as one; plain
    // checks rather than AssertJ assertions, as this runs for every node of every tree the tests build
    private static <T> int blackNodesBelow(RedBlackTree<T> tree) {
        if (tree.isEmpty()) {
            return 1;
        }
        RedBlackTreeModule.Node<T> node = (RedBlackTreeModule.Node<T>) tree;
        if (node.color == RedBlackTree.Color.RED
                && (node.left.color() != RedBlackTree.Color.BLACK || node.right.color() != RedBlackTree.Color.BLACK)) {
            throw new AssertionError("red-red in " + tree);
        }
        int left = blackNodesBelow(node.left);
        int right = blackNodesBelow(node.right);
        if (left != right) {
            throw new AssertionError(
                    "black height of " + tree + ": " + left + " on the left, " + right + " on the right");
        }
        if (node.blackHeight != left) {
            throw new AssertionError("blackHeight field of " + tree + " is " + node.blackHeight + ", expected " + left);
        }
        int size = node.left.size() + node.right.size() + 1;
        if (node.size != size) {
            throw new AssertionError("size field of " + tree + " is " + node.size + ", expected " + size);
        }
        return left + (node.color == RedBlackTree.Color.BLACK ? 1 : 0);
    }
}
