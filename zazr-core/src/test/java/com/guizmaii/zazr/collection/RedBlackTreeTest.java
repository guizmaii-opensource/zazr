package com.guizmaii.zazr.collection;

import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class RedBlackTreeTest {

    private static <T> RedBlackTree<T> empty() {
        return RedBlackTree.empty(Comparators.naturalComparator());
    }

    private static <T> RedBlackTree<T> of(T value) {
        return RedBlackTree.of(Comparators.naturalComparator(), value);
    }

    @SuppressWarnings("varargs")
    @SafeVarargs
    private static <T> RedBlackTree<T> of(T... values) {
        return RedBlackTree.<T> of(Comparators.naturalComparator(), values);
    }

    // Rudimentary tests

    // empty tree

    @Test
    public void shouldCreateEmptyTree() {
        final RedBlackTree<Integer> tree = empty();
        assertThat(tree.isEmpty()).isTrue();
        assertThat(tree.size()).isEqualTo(0);
        assertThat(tree.color()).isEqualTo(RedBlackTree.Color.BLACK);
    }

    @Test
    public void shouldFailLeftOfEmpty() {
        assertThrows(UnsupportedOperationException.class, () -> empty().left());
    }

    @Test
    public void shouldFailRightOfEmpty() {
        assertThrows(UnsupportedOperationException.class, () -> empty().right());
    }

    @Test
    public void shouldFailValueOfEmpty() {
        assertThrows(NoSuchElementException.class, () -> empty().value());
    }

    // isEmpty

    @Test
    public void shouldRecognizeEmptyTree() {
        assertThat(empty().isEmpty()).isTrue();
    }

    @Test
    public void shouldRecognizeNonEmptyTree() {
        assertThat(of(1).isEmpty()).isFalse();
    }

    // contains

    @Test
    public void shouldRecognizeContainedElement() {
        assertThat(of(1, 2, 3).contains(2)).isTrue();
    }

    @Test
    public void shouldRecognizeNonContainedElementOfEmptyTree() {
        assertThat(RedBlackTreeTest.<Integer> empty().contains(1)).isFalse();
    }

    @Test
    public void shouldRecognizeNonContainedElementOfNonEmptyTree() {
        assertThat(of(1, 2, 3).contains(0)).isFalse();
    }

    // insert

    @Test
    public void shouldInsert_2_1_4_5_9_3_6_7() {

        RedBlackTree<Integer> tree = empty();
        assertThat(tree.toString()).isEqualTo("()");
        assertThat(tree.size()).isEqualTo(0);

        tree = tree.insert(2);
        assertThat(tree.toString()).isEqualTo("(B:2)");
        assertThat(tree.size()).isEqualTo(1);

        tree = tree.insert(1);
        assertThat(tree.toString()).isEqualTo("(B:2 R:1)");
        assertThat(tree.size()).isEqualTo(2);

        tree = tree.insert(4);
        assertThat(tree.toString()).isEqualTo("(B:2 R:1 R:4)");
        assertThat(tree.size()).isEqualTo(3);

        tree = tree.insert(5);
        assertThat(tree.toString()).isEqualTo("(B:4 (B:2 R:1) B:5)");
        assertThat(tree.size()).isEqualTo(4);

        tree = tree.insert(9);
        assertThat(tree.toString()).isEqualTo("(B:4 (B:2 R:1) (B:5 R:9))");
        assertThat(tree.size()).isEqualTo(5);

        tree = tree.insert(3);
        assertThat(tree.toString()).isEqualTo("(B:4 (B:2 R:1 R:3) (B:5 R:9))");
        assertThat(tree.size()).isEqualTo(6);

        tree = tree.insert(6);
        assertThat(tree.toString()).isEqualTo("(B:4 (B:2 R:1 R:3) (R:6 B:5 B:9))");
        assertThat(tree.size()).isEqualTo(7);

        tree = tree.insert(7);
        assertThat(tree.toString()).isEqualTo("(B:4 (B:2 R:1 R:3) (R:6 B:5 (B:9 R:7)))");
        assertThat(tree.size()).isEqualTo(8);
    }

    @Test
    public void shouldRejectNullElementOnInsert() {
        assertThrows(NullPointerException.class, () -> RedBlackTreeTest.<Integer> empty().insert(null));
    }

    @Test
    public void shouldInsertNonNullIntoEmptyTree() {
        final RedBlackTree<Integer> actual = RedBlackTreeTest.<Integer> empty().insert(2);
        final RedBlackTree<Integer> expected = of(2);
        assertThat(Collections.areEqual(actual, expected)).isTrue();
    }

    @Test
    public void shouldReturnTheSameInstanceWhenInsertingAnAlreadyContainedElement() {
        final RedBlackTree<Integer> testee = of(1, 2, 3);
        final RedBlackTree<Integer> actual = testee.insert(2);
        assertThat(Collections.areEqual(actual, testee)).isTrue();
    }

    // delete

    @Test
    public void shouldDelete_2_from_2_1_4_5_9_3_6_7() {
        final RedBlackTree<Integer> testee = of(2, 1, 4, 5, 9, 3, 6, 7);
        final RedBlackTree<Integer> actual = testee.delete(2);
        assertThat(actual.toString()).isEqualTo("(B:4 (B:3 R:1) (R:6 B:5 (B:9 R:7)))");
        assertThat(actual.size()).isEqualTo(7);
    }

    // difference()

    @Test
    public void shouldSubtractEmptyFromNonEmpty() {
        final RedBlackTree<Integer> t1 = of(3, 5);
        final RedBlackTree<Integer> t2 = empty();
        final RedBlackTree<Integer> actual = t1.difference(t2);
        assertThat(actual).isEqualTo(t1);
    }

    @Test
    public void shouldSubtractNonEmptyFromEmpty() {
        final RedBlackTree<Integer> t1 = empty();
        final RedBlackTree<Integer> t2 = of(5, 7);
        final RedBlackTree<Integer> actual = t1.difference(t2);
        assertThat(actual).isEqualTo(t1);
    }

    @Test
    public void shouldSubtractNonEmptyFromNonEmpty() {
        final RedBlackTree<Integer> t1 = of(3, 5);
        final RedBlackTree<Integer> t2 = of(5, 7);
        final RedBlackTree<Integer> actual = t1.difference(t2);
        final RedBlackTree<Integer> expected = of(3);
        assertThat(Collections.areEqual(actual, expected)).isTrue();
    }

    // intersection()

    @Test
    public void shouldIntersectOnNonEmptyGivenEmpty() {
        final RedBlackTree<Integer> t1 = of(3, 5);
        final RedBlackTree<Integer> t2 = empty();
        final RedBlackTree<Integer> actual = t1.intersection(t2);
        final RedBlackTree<Integer> expected = empty();
        assertThat(Collections.areEqual(actual, expected)).isTrue();
    }

    @Test
    public void shouldIntersectOnEmptyGivenNonEmpty() {
        final RedBlackTree<Integer> t1 = empty();
        final RedBlackTree<Integer> t2 = of(5, 7);
        final RedBlackTree<Integer> actual = t1.intersection(t2);
        final RedBlackTree<Integer> expected = empty();
        assertThat(Collections.areEqual(actual, expected)).isTrue();
    }

    @Test
    public void shouldIntersectOnNonEmptyGivenNonEmpty() {
        final RedBlackTree<Integer> t1 = of(3, 5);
        final RedBlackTree<Integer> t2 = of(5, 7);
        final RedBlackTree<Integer> actual = t1.intersection(t2);
        final RedBlackTree<Integer> expected = of(5);
        assertThat(Collections.areEqual(actual, expected)).isTrue();
    }

    @Test
    public void shouldIntersectOnNonEmptyGivenNonEmptyUnbalancedHeightLeft() {
        // Node::mergeGT
        //
        // Trees have
        // - different values
        // - similar to each other left children
        // - and unlike each other right children
        final RedBlackTree<Integer> t1 = of(1, 2, 3, 4, 5, 6, 7, 8, 60, 66, 67);
        final RedBlackTree<Integer> t2 = of(1, 2, 3, 10, 11, 12, 13, 14, 60, 76, 77);
        final RedBlackTree<Integer> actual = t1.intersection(t2);
        final RedBlackTree<Integer> expected = of(1, 2, 3, 60);
        assertThat(Collections.areEqual(actual, expected)).isTrue();
    }

    @Test
    public void shouldIntersectOnNonEmptyGivenNonEmptyUnbalancedHeightRight() {
        // Node::mergeLT
        //
        // Trees have
        // - different values
        // - unlike each other left children
        // - and similar to each other right children
        final RedBlackTree<Integer> t1 = of(1, 2, 3, 4, 40, 61, 62, 63, 64, 65);
        final RedBlackTree<Integer> t2 = of(2, 7, 8, 9, 50, 61, 62, 63, 64, 65);
        final RedBlackTree<Integer> actual = t1.intersection(t2);
        final RedBlackTree<Integer> expected = of(2, 61, 62, 63, 64, 65);
        assertThat(Collections.areEqual(actual, expected)).isTrue();
    }

    @Test
    public void shouldIntersectOnNonEmptyGivenNonEmptyBalancedHeightRight() {
        // Node::mergeEQ && isRed(n1.right)
        //
        final RedBlackTree<Integer> t1 = of(-10, -20, -30, -40, -50, 1, 10, 20, 30);
        final RedBlackTree<Integer> t2 = of(-10, -20, -30, -40, -50, 2, 10, 20, 30);
        assertThat(Collections.areEqual(t1.intersection(t2), t1.delete(1))).isTrue();
    }

    /*
     * > let tree1 = fromList [8, 14, 0, 7, 9, 3]
     * > let tree2 = fromList [7, 9, 14, 6, 0, 5, 11, 10, 4, 12, 8, 13]
     * > tree1 `intersection` tree2
     * Node B 2 (Node B 1 (Node R 1 Leaf 0 Leaf) 7 Leaf) 8 (Node B 1 (Node R 1 Leaf 9 Leaf) 14 Leaf)
     * > printSet (tree1 `intersection` tree2)
     * B 8 (2)
     * + B 7 (1)
     *   + R 0 (1)
     *     +
     *     +
     *   +
     * + B 14 (1)
     *   + R 9 (1)
     *     +
     *     +
     *   +
     */
    @Test
    public void shouldPassIntersectionRegression1_Issue2098() {
        final RedBlackTree<Integer> tree1 = of(8, 14, 0, 7, 9, 3);
        final RedBlackTree<Integer> tree2 = of(7, 9, 14, 6, 0, 5, 11, 10, 4, 12, 8, 13);
        final String actual = tree1.intersection(tree2).toString();
        final String expected = "(B:8 (B:7 R:0) (B:14 R:9))";
        assertThat(actual).isEqualTo(expected);
    }

    /*
     * > let tree1 = fromList [8, 14, 0, 7, 9, 3]
     * > let tree2 = fromList [7, 9, 14, 6, 0, 5, 11, 10, 4, 12, 8, 13]
     * > let tree3 = fromList [1, 2]
     * > (tree1 `intersection` tree2) `intersection` tree3
     * Leaf
     * > tree1 `intersection` (tree2 `intersection` tree3)
     * Leaf
     */
    @Test
    public void shouldPassIntersectionRegression2_Issue2098() {
        final RedBlackTree<Integer> tree1 = of(8, 14, 0, 7, 9, 3);
        final RedBlackTree<Integer> tree2 = of(7, 9, 14, 6, 0, 5, 11, 10, 4, 12, 8, 13);
        final RedBlackTree<Integer> tree3 = of(1, 2);
        final RedBlackTree<Integer> actual = tree1.intersection(tree2).intersection(tree3);
        final RedBlackTree<Integer> expected = tree1.intersection(tree2.intersection(tree3));
        assertThat(Collections.areEqual(actual, expected)).isTrue();
    }

    /*
     * > let tree1 = [1462193440, 0, 2147483647, -2147483648, 0, 637669539, -1612766076, -1, 1795938819, 1, 0, -420800448, -2147483648, 497885405, 0, 1084073832, 1, 1439964148, 1961646330]
     * > let tree2 = [-1, 1, 2147483647, -1434983536, -2147483648, -1452486079, 1365799971, 231691980, -1780534767, -2147483648, 1448658704, 0, 1526591298]
     * > tree1 `intersection` tree2
     * Node B 2 (Node B 1 (Node R 1 Leaf (-2147483648) Leaf) (-1) Leaf) 0 (Node B 1 (Node R 1 Leaf 1 Leaf) 2147483647 Leaf)
     */
    @Test
    public void shouldPassIntersectionRegression3_Issue2098() {
        final RedBlackTree<Integer> tree1 = of(1462193440, 0, 2147483647, -2147483648, 0, 637669539, -1612766076, -1, 1795938819, 1, 0, -420800448, -2147483648, 497885405, 0, 1084073832, 1, 1439964148, 1961646330);
        final RedBlackTree<Integer> tree2 = of(-1, 1, 2147483647, -1434983536, -2147483648, -1452486079, 1365799971, 231691980, -1780534767, -2147483648, 1448658704, 0, 1526591298);
        final String actual = tree1.intersection(tree2).toString();
        final String expected = "(B:0 (B:-1 R:-2147483648) (B:2147483647 R:1))";
        assertThat(actual).isEqualTo(expected);
    }

    // union()

    @Test
    public void shouldUnionOnNonEmptyGivenEmpty() {
        final RedBlackTree<Integer> t1 = of(3, 5);
        final RedBlackTree<Integer> t2 = empty();
        final RedBlackTree<Integer> actual = t1.union(t2);
        final RedBlackTree<Integer> expected = of(3, 5);
        assertThat(Collections.areEqual(actual, expected)).isTrue();
    }

    @Test
    public void shouldUnionOnEmptyGivenNonEmpty() {
        final RedBlackTree<Integer> t1 = empty();
        final RedBlackTree<Integer> t2 = of(5, 7);
        final RedBlackTree<Integer> actual = t1.union(t2);
        final RedBlackTree<Integer> expected = of(5, 7);
        assertThat(Collections.areEqual(actual, expected)).isTrue();
    }

    @Test
    public void shouldUnionOnNonEmptyGivenNonEmpty() {
        final RedBlackTree<Integer> t1 = of(3, 5);
        final RedBlackTree<Integer> t2 = of(5, 7);
        final RedBlackTree<Integer> actual = t1.union(t2);
        final RedBlackTree<Integer> expected = of(3, 5, 7);
        assertThat(Collections.areEqual(actual, expected)).isTrue();
    }

    @Test
    public void shouldComputeUnionAndEqualTreesOfDifferentShapeButSameElements() {
        final RedBlackTree<Integer> t1 = of(-1, -1, 0, 1);
        final RedBlackTree<Integer> t2 = of(-2, -1, 0, 1);
        final RedBlackTree<Integer> actual = t1.union(t2);
        final RedBlackTree<Integer> expected = of(-2, -1, 0, 1);
        assertThat(Collections.areEqual(actual, expected)).isTrue();
    }

    // iterator()

    @Test
    public void shouldIterateEmptyTree() {
        assertThat(empty().iterator().hasNext()).isFalse();
    }

    @Test
    public void shouldIterateNonEmptyTree() {
        final RedBlackTree<Integer> testee = of(7, 1, 6, 2, 5, 3, 4);
        final List<Integer> actual = testee.iterator().toList();
        assertThat(actual.toString()).isEqualTo("List(1, 2, 3, 4, 5, 6, 7)");
    }


    // rank split: splitAt, take, drop, slice

    private static final long SEED = 20260925L;

    private static RedBlackTree<Integer> shuffledTree(int size, java.util.Random random) {
        final java.util.List<Integer> values = new java.util.ArrayList<>();
        for (int i = 0; i < size; i++) {
            values.add(i);
        }
        java.util.Collections.shuffle(values, random);
        RedBlackTree<Integer> tree = empty();
        for (Integer value : values) {
            tree = tree.insert(value);
        }
        return tree;
    }

    // the trees of sizes 0..70: one random insertion order, one ascending, and one with a third of a larger tree deleted
    private static java.util.List<RedBlackTree<Integer>> treesUpTo70() {
        final java.util.Random random = new java.util.Random(SEED);
        final java.util.List<RedBlackTree<Integer>> trees = new java.util.ArrayList<>();
        for (int size = 0; size <= 70; size++) {
            trees.add(shuffledTree(size, random));
            RedBlackTree<Integer> ascending = empty();
            for (int i = 0; i < size; i++) {
                ascending = ascending.insert(i);
            }
            trees.add(ascending);
            final int larger = size + size / 2;
            RedBlackTree<Integer> deleted = shuffledTree(larger, random);
            for (int i = size; i < larger; i++) {
                deleted = deleted.delete(i);
            }
            trees.add(deleted);
        }
        return trees;
    }

    /** Asserts that {@code tree} is a valid red-black tree: black root, no red node with a red child, the same number
     *  of black nodes on every path, the {@code blackHeight} and {@code size} fields equal to the recomputed values,
     *  and the elements strictly increasing. */
    private static void assertValid(RedBlackTree<Integer> tree) {
        assertThat(tree.color()).as("root of %s", tree).isEqualTo(RedBlackTree.Color.BLACK);
        blackNodesBelow(tree);
        Integer previous = null;
        int count = 0;
        for (Integer value : tree) {
            if (previous != null) {
                assertThat(value).as("order of %s", tree).isGreaterThan(previous);
            }
            previous = value;
            count++;
        }
        assertThat(tree.size()).isEqualTo(count);
    }

    // the number of black nodes on every path from the root of `tree` down, counting the empty leaf as one
    private static int blackNodesBelow(RedBlackTree<Integer> tree) {
        if (tree.isEmpty()) {
            return 1;
        }
        final RedBlackTreeModule.Node<Integer> node = (RedBlackTreeModule.Node<Integer>) tree;
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

    private static java.util.List<Integer> elements(RedBlackTree<Integer> tree) {
        final java.util.List<Integer> result = new java.util.ArrayList<>();
        tree.forEach(result::add);
        return result;
    }

    @Test
    public void shouldBuildValidTreesForTheSplitTests() {
        for (RedBlackTree<Integer> tree : treesUpTo70()) {
            assertValid(tree);
        }
    }

    @Test
    public void shouldSplitAtEveryRankIntoTwoValidTrees() {
        int splits = 0;
        for (RedBlackTree<Integer> tree : treesUpTo70()) {
            final java.util.List<Integer> all = elements(tree);
            for (int n = 0; n <= tree.size(); n++) {
                final com.guizmaii.zazr.Tuple2<RedBlackTree<Integer>, RedBlackTree<Integer>> split = RedBlackTreeModule.Node.splitAt(tree, n);
                assertValid(split._1());
                assertValid(split._2());
                assertThat(split._1().size()).isEqualTo(n);
                assertThat(split._2().size()).isEqualTo(tree.size() - n);
                assertThat(elements(split._1())).isEqualTo(all.subList(0, n));
                assertThat(elements(split._2())).isEqualTo(all.subList(n, all.size()));
                assertThat(split._1().comparator()).isSameAs(tree.comparator());
                assertThat(split._2().comparator()).isSameAs(tree.comparator());
                splits++;
            }
        }
        assertThat(splits).isEqualTo(3 * (71 * 72 / 2));
    }

    @Test
    public void shouldTakeAndDropAtEveryRankIncludingOutOfRange() {
        for (RedBlackTree<Integer> tree : treesUpTo70()) {
            final java.util.List<Integer> all = elements(tree);
            for (int n = -1; n <= tree.size() + 1; n++) {
                final int clamped = Math.max(0, Math.min(n, tree.size()));
                final RedBlackTree<Integer> taken = RedBlackTreeModule.Node.take(tree, n);
                final RedBlackTree<Integer> dropped = RedBlackTreeModule.Node.drop(tree, n);
                assertValid(taken);
                assertValid(dropped);
                assertThat(elements(taken)).isEqualTo(all.subList(0, clamped));
                assertThat(elements(dropped)).isEqualTo(all.subList(clamped, all.size()));
            }
        }
    }

    @Test
    public void shouldSliceEveryRangeIntoAValidTree() {
        final java.util.List<RedBlackTree<Integer>> trees = treesUpTo70();
        // every range on the trees up to size 20, then every range starting or ending at a boundary on the larger ones
        for (RedBlackTree<Integer> tree : trees) {
            final java.util.List<Integer> all = elements(tree);
            final int size = tree.size();
            for (int from = -1; from <= size + 1; from++) {
                for (int until = -1; until <= size + 1; until++) {
                    if (size > 20 && from > 1 && from < size - 1 && until > 1 && until < size - 1) {
                        continue;
                    }
                    final int start = Math.max(0, from);
                    final int end = Math.min(until, size);
                    final RedBlackTree<Integer> slice = RedBlackTreeModule.Node.slice(tree, from, until);
                    assertValid(slice);
                    assertThat(elements(slice)).isEqualTo(start < end ? all.subList(start, end) : java.util.List.of());
                }
            }
        }
    }

    @Test
    public void shouldReturnTheSameTreeForAFullSlice() {
        final RedBlackTree<Integer> tree = of(1, 2, 3);
        assertThat(RedBlackTreeModule.Node.slice(tree, 0, 3)).isSameAs(tree);
        assertThat(RedBlackTreeModule.Node.slice(tree, -5, 10)).isSameAs(tree);
    }

    @Test
    public void shouldKeepTheComparatorOnAnEmptySlice() {
        final RedBlackTree<Integer> tree = RedBlackTree.of(java.util.Comparator.<Integer> reverseOrder(), 1, 2, 3);
        assertThat(RedBlackTreeModule.Node.take(tree, 0).comparator()).isSameAs(tree.comparator());
        assertThat(RedBlackTreeModule.Node.drop(tree, 3).comparator()).isSameAs(tree.comparator());
        assertThat(RedBlackTreeModule.Node.slice(tree, 2, 1).comparator()).isSameAs(tree.comparator());
        assertThat(elements(RedBlackTreeModule.Node.take(tree, 2))).containsExactly(3, 2);
    }

    @Test
    public void shouldJoinAfterSplitIntoTheSameElements() {
        // the halves of a split are used by later joins, so their blackHeight fields must be right
        for (RedBlackTree<Integer> tree : treesUpTo70()) {
            for (int n = 0; n < tree.size(); n++) {
                final RedBlackTree<Integer> left = RedBlackTreeModule.Node.take(tree, n);
                final RedBlackTree<Integer> right = RedBlackTreeModule.Node.drop(tree, n + 1);
                final Integer pivot = elements(tree).get(n);
                final RedBlackTree<Integer> joined = RedBlackTreeModule.Node.join(left, pivot, right);
                assertValid(joined);
                assertThat(elements(joined)).isEqualTo(elements(tree));
                assertValid(left.union(right));
            }
        }
    }

}
