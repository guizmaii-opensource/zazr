package dev.zazr.collection.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

import static dev.zazr.collection.internal.RedBlackTreeValidity.assertValid;
import static org.assertj.core.api.Assertions.assertThat;

/// `difference` and `union` of the red-black tree against `java.util.TreeSet` and `java.util.TreeMap`, on pairs of
/// random trees that overlap, are disjoint, or are derived one from the other: each result is a valid tree whose
/// `blackHeight` fields are exact (so deleting from it keeps it valid), holds the expected elements, as the very
/// objects of the side whose element is kept, and shares the subtrees it can.
public class RedBlackTreeSharingTest {

    private static final long SEED = 20261103L;
    private static final int[] SIZES = {0, 1, 2, 3, 4, 5, 6, 7, 8, 15, 16, 17, 31, 32, 33, 63, 64, 65, 1023, 1024, 1025
    };

    private static final Comparator<Integer> NATURAL = Comparators.naturalComparator();
    private static final Comparator<Integer> REVERSED = NATURAL.reversed();
    // equal when they halve to the same number, so that two equal elements can be different objects
    private static final Comparator<Integer> HALVES = Comparator.comparingInt(i -> Math.floorDiv(i, 2));
    private static final List<Comparator<Integer>> ORDERS = List.of(NATURAL, REVERSED, HALVES);

    // -- the trees

    // random insertions of numbers from `from` to `from + 4 * size`, new objects outside the small integer cache
    private static RedBlackTree<Integer> inserted(Comparator<Integer> order, int size, int from, Random random) {
        @SuppressWarnings("Var")
        RedBlackTree<Integer> tree = RedBlackTree.empty(order);
        for (int i = 0; i < 8 * size && tree.size() < size; i++) {
            tree = tree.insert(Integer.valueOf(1_000_000 + from + random.nextInt(4 * size + 1)));
        }
        return tree;
    }

    // `tree` without about a tenth of its elements, deleted one by one, so that it shares most of `tree`
    private static RedBlackTree<Integer> removed(RedBlackTree<Integer> tree, Random random) {
        List<Integer> elements = elements(tree);
        Collections.shuffle(elements, random);
        @SuppressWarnings("Var")
        RedBlackTree<Integer> result = tree;
        for (int i = 0; i < (elements.size() + 9) / 10; i++) {
            result = result.delete(elements.get(i));
        }
        return result;
    }

    // `tree` with a few elements inserted, some of them new, some equal to elements there
    private static RedBlackTree<Integer> added(RedBlackTree<Integer> tree, int size, Random random) {
        @SuppressWarnings("Var")
        RedBlackTree<Integer> result = tree;
        for (int i = 0; i < 3; i++) {
            result = result.insert(Integer.valueOf(1_000_000 + random.nextInt(4 * size + 1)));
        }
        return result;
    }

    // a few elements of `tree`, the same objects, in a tree of their own
    private static RedBlackTree<Integer> few(RedBlackTree<Integer> tree, Random random) {
        List<Integer> elements = elements(tree);
        @SuppressWarnings("Var")
        RedBlackTree<Integer> result = RedBlackTree.empty(tree.comparator());
        for (int i = 0; i < 3 && !elements.isEmpty(); i++) {
            result = result.insert(elements.get(random.nextInt(elements.size())));
        }
        return result;
    }

    // the pairs given to the operations; a pair derived one from the other has the number of elements changed between
    // the two, and -1 otherwise
    private record Pair(RedBlackTree<Integer> first, RedBlackTree<Integer> second, int changes) {}

    private static List<Pair> pairs(Comparator<Integer> order, Random random) {
        List<Pair> pairs = new ArrayList<>();
        for (int size : SIZES) {
            RedBlackTree<Integer> tree = inserted(order, size, 0, random);
            RedBlackTree<Integer> other = inserted(order, size, 0, random);
            RedBlackTree<Integer> disjoint = inserted(order, size, 8 * size + 8, random);
            RedBlackTree<Integer> removed = removed(tree, random);
            RedBlackTree<Integer> added = added(tree, size, random);
            RedBlackTree<Integer> few = few(tree, random);
            RedBlackTree<Integer> empty = RedBlackTree.empty(order);
            pairs.add(new Pair(tree, other, -1));
            pairs.add(new Pair(tree, disjoint, -1));
            pairs.add(new Pair(disjoint, tree, -1));
            pairs.add(new Pair(tree, removed, (size + 9) / 10));
            pairs.add(new Pair(removed, tree, (size + 9) / 10));
            pairs.add(new Pair(tree, added, 3));
            pairs.add(new Pair(added, tree, 3));
            pairs.add(new Pair(tree, few, -1));
            pairs.add(new Pair(few, tree, -1));
            pairs.add(new Pair(tree, tree, 0));
            pairs.add(new Pair(tree, empty, -1));
            pairs.add(new Pair(empty, tree, -1));
        }
        for (int i = 0; i < 200; i++) {
            int size = random.nextInt(100);
            RedBlackTree<Integer> tree = inserted(order, size, 0, random);
            pairs.add(new Pair(tree, inserted(order, size, random.nextInt(4 * size + 1), random), -1));
            pairs.add(new Pair(tree, removed(tree, random), (tree.size() + 9) / 10));
            pairs.add(new Pair(added(tree, size, random), tree, 3));
        }
        return pairs;
    }

    private static <T> List<T> elements(RedBlackTree<T> tree) {
        List<T> result = new ArrayList<>();
        tree.forEach(result::add);
        return result;
    }

    // -- the checks

    // `actual` holds the very objects of `expected`, in order
    private static void assertSameElements(RedBlackTree<Integer> actual, List<Integer> expected, String context) {
        List<Integer> elements = elements(actual);
        assertThat(elements).as(context).isEqualTo(expected);
        for (int i = 0; i < elements.size(); i++) {
            if (elements.get(i) != expected.get(i)) {
                throw new AssertionError(context + ": element " + i + " is not the expected object");
            }
        }
    }

    // a valid tree, and so is every deletion from it: the deletion reads the `blackHeight` fields
    private static void assertValidAndDeletable(RedBlackTree<Integer> tree, String context) {
        assertValid(tree);
        List<Integer> elements = elements(tree);
        int stride = elements.size() > 100 ? 7 : 1;
        for (int i = 0; i < elements.size(); i += stride) {
            RedBlackTree<Integer> deleted = tree.delete(elements.get(i));
            assertValid(deleted);
            assertThat(deleted.size()).as(context).isEqualTo(tree.size() - 1);
        }
    }

    private static int height(RedBlackTree<?> tree) {
        return tree.isEmpty() ? 0 : 1 + Math.max(height(tree.left()), height(tree.right()));
    }

    // the nodes of `result` that are in neither `first` nor `second`
    private static int fresh(RedBlackTree<?> result, RedBlackTree<?> first, RedBlackTree<?> second) {
        Map<Object, Boolean> known = new IdentityHashMap<>();
        collect(first, known);
        collect(second, known);
        return countFresh(result, known);
    }

    private static void collect(RedBlackTree<?> tree, Map<Object, Boolean> known) {
        if (!tree.isEmpty() && known.put(tree, Boolean.TRUE) == null) {
            collect(tree.left(), known);
            collect(tree.right(), known);
        }
    }

    // a known node is the root of a known subtree, which is not walked
    private static int countFresh(RedBlackTree<?> tree, Map<Object, Boolean> known) {
        if (tree.isEmpty() || known.containsKey(tree)) {
            return 0;
        }
        return 1 + countFresh(tree.left(), known) + countFresh(tree.right(), known);
    }

    // -- difference

    @Test
    void differenceHoldsTheElementsOfTheFirstTreeNotInTheSecondAsAValidTree() {
        Random random = new Random(SEED);
        for (Comparator<Integer> order : ORDERS) {
            for (Pair pair : pairs(order, random)) {
                TreeSet<Integer> removed = new TreeSet<>(order);
                removed.addAll(elements(pair.second()));
                List<Integer> expected = new ArrayList<>();
                for (Integer element : elements(pair.first())) {
                    if (!removed.contains(element)) {
                        expected.add(element);
                    }
                }
                RedBlackTree<Integer> result = pair.first().difference(pair.second());
                String context = "difference of " + pair.first() + " and " + pair.second();
                assertSameElements(result, expected, context);
                assertValidAndDeletable(result, context);
                if (expected.size() == pair.first().size()) {
                    assertThat(result).as(context).isSameAs(pair.first());
                }
            }
        }
    }

    @Test
    void differenceOfATreeAndItselfIsEmpty() {
        RedBlackTree<Integer> tree = inserted(NATURAL, 1000, 0, new Random(SEED + 1));
        assertThat(tree.difference(tree).isEmpty()).isTrue();
    }

    @Test
    void differenceWithAFewElementsRebuildsOnlyTheirPaths() {
        Random random = new Random(SEED + 2);
        for (Comparator<Integer> order : ORDERS) {
            for (int size : SIZES) {
                RedBlackTree<Integer> tree = inserted(order, size, 0, random);
                RedBlackTree<Integer> few = few(tree, random);
                RedBlackTree<Integer> result = tree.difference(few);
                // a join per level on the way up from each element removed, and as many below the removed node
                assertThat(fresh(result, tree, few))
                        .as("fresh nodes of the difference of " + tree + " and " + few)
                        .isLessThanOrEqualTo(few.size() * 4 * (height(tree) + 1));
            }
        }
    }

    @Test
    void differenceWithASubtreeSharedByBothSidesRemovesItWhole() {
        Random random = new Random(SEED + 3);
        RedBlackTree<Integer> tree = inserted(NATURAL, 1024, 0, random);
        RedBlackTree<Integer> removed = removed(tree, random);
        // the elements deleted from `removed` are what is left, and the rest of the tree is not walked
        RedBlackTree<Integer> result = tree.difference(removed);
        assertThat(result.size()).isEqualTo(tree.size() - removed.size());
        assertValidAndDeletable(result, "difference of a tree and its deletions");
    }

    // the comparator calls of `difference`, counted
    private static final class Counting implements Comparator<Integer> {

        private int calls;

        @Override
        public int compare(Integer a, Integer b) {
            calls++;
            return Integer.compare(a, b);
        }
    }

    // a tree of `elements` in one of three shapes: inserted in random order, in ascending order, or built balanced
    private static RedBlackTree<Integer> shaped(
            Comparator<Integer> order, List<Integer> elements, int shape, Random random) {
        List<Integer> sorted = new ArrayList<>(elements);
        sorted.sort(order);
        if (shape == 2) {
            Object[] array = sorted.toArray();
            return RedBlackTreeModule.Node.fromOrdered(new RedBlackTreeModule.Empty<>(order), array, array.length);
        }
        List<Integer> inOrder = new ArrayList<>(sorted);
        if (shape == 0) {
            Collections.shuffle(inOrder, random);
        }
        @SuppressWarnings("Var")
        RedBlackTree<Integer> tree = RedBlackTree.empty(order);
        for (Integer element : inOrder) {
            tree = tree.insert(element);
        }
        return tree;
    }

    @Test
    void differenceCallsTheComparatorAtMostOnceForEachLevelOfTheArgumentPerNodeOnASearchPath() {
        Random random = new Random(SEED + 6);
        for (int n : new int[] {1000, 1024, 1025}) {
            List<Integer> even = new ArrayList<>();
            List<Integer> odd = new ArrayList<>();
            List<Integer> copies = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                even.add(Integer.valueOf(1_000_000 + 2 * i));
                odd.add(Integer.valueOf(1_000_001 + 2 * i));
                copies.add(Integer.valueOf(1_000_000 + 2 * i));
            }
            List<Integer> random50 = new ArrayList<>();
            for (int i = 0; i < 2 * n; i++) {
                if (random.nextBoolean()) {
                    random50.add(Integer.valueOf(1_000_000 + i));
                }
            }
            List<Integer> few = List.of(even.get(0), even.get(n / 2), odd.get(n - 1));
            for (List<Integer> argument : List.of(odd, copies, random50, few)) {
                for (int treeShape = 0; treeShape < 3; treeShape++) {
                    for (int argumentShape = 0; argumentShape < 3; argumentShape++) {
                        Counting counting = new Counting();
                        RedBlackTree<Integer> tree = shaped(counting, even, treeShape, random);
                        RedBlackTree<Integer> other = shaped(counting, argument, argumentShape, random);
                        counting.calls = 0;
                        RedBlackTree<Integer> result = tree.difference(other);
                        int calls = counting.calls;
                        // the nodes on the search paths of the elements of `other`, and for each of them and its
                        // two children a walk down `other`, then for each of them a lookup in `other`
                        int onPaths = Math.min(tree.size(), other.size() * height(tree));
                        int bound = onPaths * 4 * (height(other) + 1);
                        String context = "difference of shapes " + treeShape + " and " + argumentShape + ", n = " + n
                                + ", m = " + other.size();
                        assertThat(calls).as(context).isLessThanOrEqualTo(bound);
                        assertValid(result);
                    }
                }
            }
        }
    }

    // -- union

    @Test
    void unionHoldsTheElementsOfBothTreesWithTheSecondTreesOfEqualOnesAsAValidTree() {
        Random random = new Random(SEED + 4);
        for (Comparator<Integer> order : ORDERS) {
            for (Pair pair : pairs(order, random)) {
                TreeMap<Integer, Integer> union = new TreeMap<>(order);
                for (Integer element : elements(pair.first())) {
                    union.put(element, element);
                }
                for (Integer element : elements(pair.second())) {
                    union.put(element, element);
                }
                List<Integer> expected = new ArrayList<>(union.values());
                RedBlackTree<Integer> result = pair.first().union(pair.second());
                String context = "union of " + pair.first() + " and " + pair.second();
                assertSameElements(result, expected, context);
                assertValidAndDeletable(result, context);
                if (pair.second().isEmpty() || pair.first() == pair.second()) {
                    assertThat(result).as(context).isSameAs(pair.first());
                } else if (pair.first().isEmpty()) {
                    assertThat(result).as(context).isSameAs(pair.second());
                }
                if (pair.changes() >= 0) {
                    // the two trees differ along a few paths, which are all the union rebuilds
                    assertThat(fresh(result, pair.first(), pair.second()))
                            .as("fresh nodes of the " + context)
                            .isLessThanOrEqualTo(pair.changes() * 4 * (height(result) + 1));
                }
            }
        }
    }

    @Test
    void unionWithATreeOfTheSameShapeHoldingOtherObjectsIsThatTree() {
        // `insert` of an element equal to one there replaces it in place: the same shape, with one path copied
        Random random = new Random(SEED + 5);
        for (int size : SIZES) {
            RedBlackTree<Integer> tree = inserted(HALVES, size, 0, random);
            if (tree.isEmpty()) {
                continue;
            }
            List<Integer> elements = elements(tree);
            Integer element = elements.get(random.nextInt(elements.size()));
            // the other number of the same half: equal under HALVES, another object
            Integer twin = Integer.valueOf(element % 2 == 0 ? element + 1 : element - 1);
            RedBlackTree<Integer> replaced = tree.insert(twin);
            assertThat(tree.union(replaced)).as("union with " + replaced).isSameAs(replaced);
            assertThat(replaced.union(tree)).as("union with " + tree).isSameAs(tree);
        }
    }
}
