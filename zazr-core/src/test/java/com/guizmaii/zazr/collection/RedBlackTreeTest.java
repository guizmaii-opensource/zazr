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
        // Trees have
        // - different values
        // - similar to each other left children
        // - and unlike each other right children
        final RedBlackTree<Integer> t1 = of(1, 2, 3, 4, 5, 6, 7, 8, 60, 66, 67);
        final RedBlackTree<Integer> t2 = of(1, 2, 3, 10, 11, 12, 13, 14, 60, 76, 77);
        final RedBlackTree<Integer> actual = t1.intersection(t2);
        final RedBlackTree<Integer> expected = of(1, 2, 3, 60);
        assertValid(actual);
        assertThat(Collections.areEqual(actual, expected)).isTrue();
    }

    @Test
    public void shouldIntersectOnNonEmptyGivenNonEmptyUnbalancedHeightRight() {
        // Trees have
        // - different values
        // - unlike each other left children
        // - and similar to each other right children
        final RedBlackTree<Integer> t1 = of(1, 2, 3, 4, 40, 61, 62, 63, 64, 65);
        final RedBlackTree<Integer> t2 = of(2, 7, 8, 9, 50, 61, 62, 63, 64, 65);
        final RedBlackTree<Integer> actual = t1.intersection(t2);
        final RedBlackTree<Integer> expected = of(2, 61, 62, 63, 64, 65);
        assertValid(actual);
        assertThat(Collections.areEqual(actual, expected)).isTrue();
    }

    @Test
    public void shouldIntersectOnNonEmptyGivenNonEmptyBalancedHeightRight() {
        // the left parts of the two trees are equal
        final RedBlackTree<Integer> t1 = of(-10, -20, -30, -40, -50, 1, 10, 20, 30);
        final RedBlackTree<Integer> t2 = of(-10, -20, -30, -40, -50, 2, 10, 20, 30);
        assertValid(t1.intersection(t2));
        assertThat(Collections.areEqual(t1.intersection(t2), t1.delete(1))).isTrue();
    }

    // the intersection is a valid tree of the common elements; its shape is not fixed
    @Test
    public void shouldPassIntersectionRegression1_Issue2098() {
        final RedBlackTree<Integer> tree1 = of(8, 14, 0, 7, 9, 3);
        final RedBlackTree<Integer> tree2 = of(7, 9, 14, 6, 0, 5, 11, 10, 4, 12, 8, 13);
        final RedBlackTree<Integer> actual = tree1.intersection(tree2);
        assertValid(actual);
        assertThat(elements(actual)).containsExactly(0, 7, 8, 9, 14);
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

    // the intersection is a valid tree of the common elements; its shape is not fixed
    @Test
    public void shouldPassIntersectionRegression3_Issue2098() {
        final RedBlackTree<Integer> tree1 = of(1462193440, 0, 2147483647, -2147483648, 0, 637669539, -1612766076, -1, 1795938819, 1, 0, -420800448, -2147483648, 497885405, 0, 1084073832, 1, 1439964148, 1961646330);
        final RedBlackTree<Integer> tree2 = of(-1, 1, 2147483647, -1434983536, -2147483648, -1452486079, 1365799971, 231691980, -1780534767, -2147483648, 1448658704, 0, 1526591298);
        final RedBlackTree<Integer> actual = tree1.intersection(tree2);
        assertValid(actual);
        assertThat(elements(actual)).containsExactly(-2147483648, -1, 0, 1, 2147483647);
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

    // the trees of sizes 0..70: one random insertion order, one ascending, one with a third of a larger tree deleted,
    // and one with a third of a larger tree subtracted by difference
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
            RedBlackTree<Integer> removed = empty();
            for (int i = size; i < larger; i++) {
                removed = removed.insert(i);
            }
            trees.add(shuffledTree(larger, random).difference(removed));
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
        assertThat(splits).isEqualTo(4 * (71 * 72 / 2));
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

    // validity of the set operations: difference, intersection, union, delete, split, join, merge

    // a random valid tree of 0..100 elements drawn from 0..199, in one of three shapes: shuffled insertion, ascending
    // insertion, or a larger tree with random deletions
    private static RedBlackTree<Integer> randomTree(java.util.Random random, java.util.TreeSet<Integer> model) {
        final int size = random.nextInt(101);
        final java.util.List<Integer> values = new java.util.ArrayList<>();
        while (model.size() < size) {
            final int value = random.nextInt(200);
            if (model.add(value)) {
                values.add(value);
            }
        }
        final int shape = random.nextInt(3);
        RedBlackTree<Integer> tree = empty();
        if (shape == 0) {
            for (Integer value : values) {
                tree = tree.insert(value);
            }
        } else if (shape == 1) {
            for (Integer value : model) {
                tree = tree.insert(value);
            }
        } else {
            final java.util.List<Integer> extra = new java.util.ArrayList<>();
            for (int i = 0; i < size; i++) {
                extra.add(200 + random.nextInt(200));
            }
            final java.util.List<Integer> all = new java.util.ArrayList<>(values);
            all.addAll(extra);
            java.util.Collections.shuffle(all, random);
            for (Integer value : all) {
                tree = tree.insert(value);
            }
            for (Integer value : extra) {
                tree = tree.delete(value);
            }
        }
        return tree;
    }

    private static java.util.List<Integer> javaList(TreeSet<Integer> set) {
        final java.util.List<Integer> result = new java.util.ArrayList<>();
        set.forEach(result::add);
        return result;
    }

    // counts the checks of `operation` that fail: an invalid tree, the wrong elements, or an exception
    private static final class FailureCounter {

        private final java.util.Map<String, Integer> failures = new java.util.TreeMap<>();
        private final java.util.Map<String, String> firstFailure = new java.util.TreeMap<>();
        private final java.util.Map<String, Integer> checks = new java.util.TreeMap<>();

        void check(String operation, java.util.Set<Integer> expected, java.util.function.Supplier<RedBlackTree<Integer>> result) {
            checks.merge(operation, 1, Integer::sum);
            try {
                final RedBlackTree<Integer> tree = result.get();
                assertValid(tree);
                assertThat(elements(tree)).isEqualTo(new java.util.ArrayList<>(expected));
            } catch (AssertionError | RuntimeException e) {
                failures.merge(operation, 1, Integer::sum);
                firstFailure.putIfAbsent(operation, e.toString());
            }
        }

        void assertNoFailure() {
            assertThat(failures).as("failures out of %s checks, first failures: %s", checks, firstFailure).isEmpty();
        }
    }

    private static java.util.TreeSet<Integer> model(java.util.Collection<Integer> values) {
        return new java.util.TreeSet<>(values);
    }

    @Test
    public void shouldBuildValidRandomTrees() {
        final java.util.Random random = new java.util.Random(SEED);
        final FailureCounter counter = new FailureCounter();
        for (int i = 0; i < 2_000; i++) {
            final java.util.TreeSet<Integer> model = new java.util.TreeSet<>();
            final RedBlackTree<Integer> tree = randomTree(random, model);
            counter.check("randomTree", model, () -> tree);
        }
        counter.assertNoFailure();
    }

    @Test
    public void shouldKeepSetOperationsValid() {
        final java.util.Random random = new java.util.Random(SEED);
        final FailureCounter counter = new FailureCounter();
        for (int i = 0; i < 20_000; i++) {
            final java.util.TreeSet<Integer> model1 = new java.util.TreeSet<>();
            final java.util.TreeSet<Integer> model2 = new java.util.TreeSet<>();
            final RedBlackTree<Integer> t1 = randomTree(random, model1);
            final RedBlackTree<Integer> t2 = randomTree(random, model2);

            final java.util.TreeSet<Integer> difference = model(model1);
            difference.removeAll(model2);
            counter.check("difference", difference, () -> t1.difference(t2));

            final java.util.TreeSet<Integer> intersection = model(model1);
            intersection.retainAll(model2);
            counter.check("intersection", intersection, () -> t1.intersection(t2));

            final java.util.TreeSet<Integer> union = model(model1);
            union.addAll(model2);
            counter.check("union", union, () -> t1.union(t2));

            // the results of difference and intersection used as inputs of the other operations
            counter.check("difference then union", union, () -> t1.difference(t2).union(t2));
            counter.check("intersection then insert", model(model2), () -> {
                RedBlackTree<Integer> tree = t2.intersection(t1);
                for (Integer value : model2) {
                    tree = tree.insert(value);
                }
                return tree;
            });

            final int value = random.nextInt(200);
            final java.util.TreeSet<Integer> deleted = model(model1);
            deleted.remove(value);
            counter.check("delete", deleted, () -> t1.delete(value));
            counter.check("difference then delete", model(difference.headSet(value)), () -> {
                RedBlackTree<Integer> tree = t1.difference(t2);
                for (Integer element : difference.tailSet(value)) {
                    tree = tree.delete(element);
                }
                return tree;
            });

            // take, drop and slice by rank of the results of difference and intersection
            final java.util.List<Integer> differenceList = new java.util.ArrayList<>(difference);
            final int rank = random.nextInt(differenceList.size() + 3) - 1;
            final int clamped = Math.max(0, Math.min(rank, differenceList.size()));
            counter.check("take of a difference", model(differenceList.subList(0, clamped)),
                    () -> RedBlackTreeModule.Node.take(t1.difference(t2), rank));
            counter.check("drop of a difference", model(differenceList.subList(clamped, differenceList.size())),
                    () -> RedBlackTreeModule.Node.drop(t1.difference(t2), rank));
            final java.util.List<Integer> intersectionList = new java.util.ArrayList<>(intersection);
            final int from = random.nextInt(intersectionList.size() + 2) - 1;
            final int until = random.nextInt(intersectionList.size() + 2);
            final int start = Math.max(0, from);
            final int end = Math.min(until, intersectionList.size());
            counter.check("slice of an intersection",
                    model(start < end ? intersectionList.subList(start, end) : java.util.List.of()),
                    () -> RedBlackTreeModule.Node.slice(t1.intersection(t2), from, until));

            final com.guizmaii.zazr.Tuple2<RedBlackTree<Integer>, RedBlackTree<Integer>> split = RedBlackTreeModule.Node.split(t1, value);
            counter.check("split left", model(model1.headSet(value)), split::_1);
            counter.check("split right", model(model1.tailSet(value, false)), split::_2);

            // join and merge on trees that are strictly ordered around the value
            final java.util.TreeSet<Integer> joined = model(model1.headSet(value));
            joined.add(value);
            joined.addAll(model2.tailSet(value, false));
            final RedBlackTree<Integer> left = RedBlackTreeModule.Node.split(t1, value)._1();
            final RedBlackTree<Integer> right = RedBlackTreeModule.Node.split(t2, value)._2();
            counter.check("join", joined, () -> RedBlackTreeModule.Node.join(left, value, right));
            final java.util.TreeSet<Integer> merged = model(joined);
            merged.remove(value);
            counter.check("merge", merged, () -> RedBlackTreeModule.Node.merge(left, right));
        }
        counter.assertNoFailure();
    }

    // the children of a node can have a red root, which join and merge receive when a node is taken apart
    @Test
    public void shouldJoinAndMergeTheChildrenOfANode() {
        final java.util.Random random = new java.util.Random(SEED);
        final FailureCounter counter = new FailureCounter();
        for (int i = 0; i < 20_000; i++) {
            final java.util.TreeSet<Integer> model = new java.util.TreeSet<>();
            final RedBlackTree<Integer> tree = randomTree(random, model);
            if (tree.isEmpty()) {
                continue;
            }
            // descend a random path and take apart the node where it stops
            RedBlackTreeModule.Node<Integer> node = (RedBlackTreeModule.Node<Integer>) tree;
            while (random.nextInt(3) != 0) {
                final RedBlackTree<Integer> child = random.nextBoolean() ? node.left : node.right;
                if (child.isEmpty()) {
                    break;
                }
                node = (RedBlackTreeModule.Node<Integer>) child;
            }
            final RedBlackTreeModule.Node<Integer> taken = node;
            final java.util.TreeSet<Integer> expected = model(elements(taken));
            counter.check("join of the children", expected,
                    () -> RedBlackTreeModule.Node.join(taken.left, taken.value, taken.right));
            expected.remove(taken.value);
            counter.check("merge of the children", expected,
                    () -> RedBlackTreeModule.Node.merge(taken.left, taken.right));
            // a split half next to a child subtree, which can have a red root
            final RedBlackTree<Integer> lower = RedBlackTreeModule.Node.split(tree, taken.value)._1();
            final java.util.TreeSet<Integer> lowerAndRight = model(model.headSet(taken.value));
            lowerAndRight.add(taken.value);
            lowerAndRight.addAll(elements(taken.right));
            counter.check("join of a tree and a child", lowerAndRight,
                    () -> RedBlackTreeModule.Node.join(lower, taken.value, taken.right));
        }
        counter.assertNoFailure();
    }

    // the shrunk failing input of a random search: the difference had one more black node on the path to 6 than on
    // the path to 23 and a wrong blackHeight at the root
    @Test
    public void shouldSubtractIntoAValidTree() {
        final RedBlackTree<Integer> t1 = RedBlackTree.ofAll(Comparators.naturalComparator(),
                List.of(56, 31, 20, 10, 6, 42, 11, 28, 23));
        final RedBlackTree<Integer> t2 = RedBlackTree.ofAll(Comparators.naturalComparator(), List.of(32, 19));
        final RedBlackTree<Integer> difference = t1.difference(t2);
        assertValid(difference);
        assertThat(elements(difference)).containsExactly(6, 10, 11, 20, 23, 28, 31, 42, 56);
    }

    private static TreeSet<Integer> treeSet(Integer... values) {
        return TreeSet.ofAll(java.util.Comparator.naturalOrder(), List.of(values));
    }

    @Test
    public void shouldRemoveUnionAndAddOnATreeSetDifference() {
        final TreeSet<Integer> difference = treeSet(56, 31, 20, 10, 6, 42, 11, 28, 23).diff(treeSet(32, 19));
        assertThat(javaList(difference)).containsExactly(6, 10, 11, 20, 23, 28, 31, 42, 56);
        // removing every element in order threw IllegalStateException
        TreeSet<Integer> removed = difference;
        final java.util.List<Integer> remaining = javaList(difference);
        for (Integer element : difference) {
            removed = removed.remove(element);
            remaining.remove(element);
            assertThat(javaList(removed)).isEqualTo(remaining);
        }
        // drop, and union, diff and intersect with a set sharing the comparator, threw ClassCastException
        assertThat(javaList(difference.drop(1))).containsExactly(10, 11, 20, 23, 28, 31, 42, 56);
        assertThat(javaList(difference.take(8))).containsExactly(6, 10, 11, 20, 23, 28, 31, 42);
        assertThat(javaList(difference.union(treeSet(0, 25, 99))))
                .containsExactly(0, 6, 10, 11, 20, 23, 25, 28, 31, 42, 56, 99);
        assertThat(javaList(difference.diff(treeSet(0, 23)))).containsExactly(6, 10, 11, 20, 28, 31, 42, 56);
        assertThat(javaList(difference.intersect(treeSet(0, 6, 56)))).containsExactly(6, 56);
        assertThat(javaList(difference.add(0).add(30).add(100)))
                .containsExactly(0, 6, 10, 11, 20, 23, 28, 30, 31, 42, 56, 100);
    }

}
