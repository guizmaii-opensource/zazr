package com.guizmaii.zazr.collection.internal;

import com.guizmaii.zazr.Tuple2;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

import static com.guizmaii.zazr.collection.internal.RedBlackTreeValidity.assertValid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@code Node.filter} and {@code Node.partition}: the kept elements, as a valid tree that shares the subtrees the
 * predicate leaves whole, the predicate called once per element in order, and the source untouched.
 */
public class RedBlackTreeFilterTest {

    private static final long SEED = 20260925L;
    private static final int[] SIZES = { 0, 1, 2, 3, 4, 5, 7, 8, 9, 31, 32, 33, 63, 64, 65, 127, 128, 129, 1023, 1024, 1025 };

    private static final Comparator<Integer> NATURAL = Comparators.naturalComparator();
    private static final Comparator<Integer> REVERSED = NATURAL.reversed();

    // the ways a tree gets its shape: one insert at a time (ascending, then random order, which puts red nodes at every
    // depth), inserts followed by deletes (the rebalancing of deletion), and the bottom-up construction
    private static java.util.List<RedBlackTree<Integer>> sources(Comparator<Integer> order, int size, Random random) {
        final java.util.List<RedBlackTree<Integer>> trees = new ArrayList<>();
        RedBlackTree<Integer> ascending = RedBlackTree.empty(order);
        for (int i = 0; i < size; i++) {
            ascending = ascending.insert(i);
        }
        trees.add(ascending);
        RedBlackTree<Integer> descending = RedBlackTree.empty(order);
        for (int i = size - 1; i >= 0; i--) {
            descending = descending.insert(i);
        }
        trees.add(descending);
        RedBlackTree<Integer> shuffled = RedBlackTree.empty(order);
        while (shuffled.size() < size) {
            shuffled = shuffled.insert(random.nextInt(4 * size + 1) - 2 * size);
        }
        trees.add(shuffled);
        RedBlackTree<Integer> deleted = RedBlackTree.empty(order);
        while (deleted.size() < 2 * size) {
            deleted = deleted.insert(random.nextInt(8 * size + 1) - 4 * size);
        }
        final java.util.List<Integer> elements = elements(deleted);
        java.util.Collections.shuffle(elements, random);
        for (int i = 0; i < size; i++) {
            deleted = deleted.delete(elements.get(i));
        }
        trees.add(deleted);
        final Object[] sorted = new Object[size];
        for (int i = 0; i < size; i++) {
            sorted[i] = (order == NATURAL) ? i : size - 1 - i;
        }
        trees.add(RedBlackTreeModule.Node.fromOrdered(new RedBlackTreeModule.Empty<>(order), sorted, size));
        return trees;
    }

    private static <T> java.util.List<T> elements(RedBlackTree<T> tree) {
        final java.util.List<T> result = new ArrayList<>();
        tree.forEach(result::add);
        return result;
    }

    private static IdentityHashMap<Object, Boolean> nodes(RedBlackTree<?> tree) {
        final IdentityHashMap<Object, Boolean> nodes = new IdentityHashMap<>();
        collect(tree, nodes);
        return nodes;
    }

    private static void collect(RedBlackTree<?> tree, IdentityHashMap<Object, Boolean> nodes) {
        if (!tree.isEmpty()) {
            nodes.put(tree, true);
            collect(tree.left(), nodes);
            collect(tree.right(), nodes);
        }
    }

    private static int height(RedBlackTree<?> tree) {
        return tree.isEmpty() ? 0 : 1 + Math.max(height(tree.left()), height(tree.right()));
    }

    // the number of nodes of `result` that are not nodes of `source`
    private static int freshNodes(RedBlackTree<?> result, RedBlackTree<?> source) {
        final IdentityHashMap<Object, Boolean> old = nodes(source);
        int fresh = 0;
        for (Object node : nodes(result).keySet()) {
            if (!old.containsKey(node)) {
                fresh++;
            }
        }
        return fresh;
    }

    // what the result of keeping `expected` out of `source` must be: valid, the kept elements themselves in order, the
    // source's comparator, `source` itself when nothing is dropped, and a new node for at most a few per level of the
    // source for each dropped element (the untouched subtrees are shared)
    private static <T> void assertKept(RedBlackTree<T> result, RedBlackTree<T> source, java.util.List<T> expected) {
        assertValid(result);
        final java.util.List<T> actual = elements(result);
        assertThat(actual).hasSameSizeAs(expected);
        for (int i = 0; i < expected.size(); i++) {
            assertThat(actual.get(i)).isSameAs(expected.get(i));
        }
        assertThat(result.comparator()).isSameAs(source.comparator());
        if (expected.size() == source.size()) {
            assertThat(result).isSameAs(source);
        }
        final int dropped = source.size() - expected.size();
        assertThat(freshNodes(result, source)).as("new nodes of %s from %s", result, source)
                .isLessThanOrEqualTo(dropped * 3 * (height(source) + 1));
    }

    private static <T> void check(RedBlackTree<T> source, Predicate<? super T> keep) {
        final java.util.List<T> before = elements(source);
        final java.util.List<T> kept = new ArrayList<>();
        final java.util.List<T> rejected = new ArrayList<>();
        for (T element : before) {
            (keep.test(element) ? kept : rejected).add(element);
        }

        final java.util.List<T> calls = new ArrayList<>();
        final RedBlackTree<T> filtered = RedBlackTreeModule.Node.filter(source, element -> {
            calls.add(element);
            return keep.test(element);
        });
        assertThat(calls).as("the predicate is called once per element, in order").isEqualTo(before);
        assertKept(filtered, source, kept);

        calls.clear();
        final Tuple2<RedBlackTree<T>, RedBlackTree<T>> partition = RedBlackTreeModule.Node.partition(source, element -> {
            calls.add(element);
            return keep.test(element);
        });
        assertThat(calls).as("the predicate is called once per element, in order").isEqualTo(before);
        assertKept(partition._1(), source, kept);
        assertKept(partition._2(), source, rejected);

        // persistence: the source is unchanged
        assertValid(source);
        assertThat(elements(source)).isEqualTo(before);
    }

    @Test
    public void shouldFilterAndPartitionEverySubsetOfSmallTrees() {
        final Random random = new Random(SEED);
        for (int size = 0; size <= 12; size++) {
            for (Comparator<Integer> order : java.util.List.of(NATURAL, REVERSED)) {
                for (RedBlackTree<Integer> source : sources(order, size, random)) {
                    final java.util.List<Integer> elements = elements(source);
                    for (int mask = 0; mask < (1 << size); mask++) {
                        final int bits = mask;
                        check(source, element -> (bits & (1 << elements.indexOf(element))) != 0);
                    }
                }
            }
        }
    }

    @Test
    public void shouldFilterAndPartitionAtEveryBoundary() {
        final Random random = new Random(SEED + 1);
        for (int size : SIZES) {
            for (Comparator<Integer> order : java.util.List.of(NATURAL, REVERSED)) {
                for (RedBlackTree<Integer> source : sources(order, size, random)) {
                    final java.util.List<Integer> elements = elements(source);
                    final int middle = elements.isEmpty() ? 0 : elements.get(size / 2);
                    check(source, element -> true);
                    check(source, element -> false);
                    check(source, element -> (element & 1) == 0);
                    check(source, element -> element % 3 != 0);
                    check(source, element -> NATURAL.compare(element, middle) < 0);
                    check(source, element -> NATURAL.compare(element, middle) >= 0);
                    final java.util.Set<Integer> chosen = new java.util.HashSet<>();
                    for (Integer element : elements) {
                        if (random.nextInt(10) == 0) {
                            chosen.add(element);
                        }
                    }
                    check(source, chosen::contains);
                    check(source, element -> !chosen.contains(element));
                }
            }
        }
    }

    @Test
    public void shouldDropEachSingleElement() {
        final Random random = new Random(SEED + 2);
        for (int size : new int[] { 1, 2, 3, 31, 32, 33, 64, 65 }) {
            for (RedBlackTree<Integer> source : sources(NATURAL, size, random)) {
                for (Integer dropped : elements(source)) {
                    check(source, element -> !element.equals(dropped));
                    check(source, element -> element.equals(dropped));
                }
            }
        }
    }

    @Test
    public void shouldShareTheUntouchedPartOfALargeTree() {
        final int size = 4095;
        final Object[] sorted = new Object[size];
        for (int i = 0; i < size; i++) {
            sorted[i] = i;
        }
        final RedBlackTree<Integer> source = RedBlackTreeModule.Node.fromOrdered(new RedBlackTreeModule.Empty<>(NATURAL), sorted, size);
        for (int dropped : new int[] { 0, 1, size / 3, size / 2, size - 2, size - 1 }) {
            final RedBlackTree<Integer> filtered = RedBlackTreeModule.Node.filter(source, element -> element != dropped);
            assertValid(filtered);
            assertThat(filtered.size()).isEqualTo(size - 1);
            // one element dropped: new nodes only along a few paths from the root, everything else shared
            assertThat(freshNodes(filtered, source)).isLessThanOrEqualTo(3 * (height(source) + 1));
        }
        // the whole left half dropped: the right subtree of the root is shared, apart from its leftmost path
        final int rootValue = source.value();
        final RedBlackTree<Integer> upper = RedBlackTreeModule.Node.filter(source, element -> element > rootValue);
        assertValid(upper);
        assertThat(nodes(upper).containsKey(source.right().right())).isTrue();
        final Tuple2<RedBlackTree<Integer>, RedBlackTree<Integer>> halves =
                RedBlackTreeModule.Node.partition(source, element -> element < rootValue);
        assertThat(nodes(halves._1()).containsKey(source.left().left())).isTrue();
        assertThat(nodes(halves._2()).containsKey(source.right().right())).isTrue();
    }

    @Test
    public void shouldNeverCallTheComparator() {
        final AtomicInteger comparisons = new AtomicInteger();
        final Comparator<Integer> counting = (a, b) -> {
            comparisons.incrementAndGet();
            return Integer.compare(a, b);
        };
        final Random random = new Random(SEED + 3);
        for (int size : SIZES) {
            RedBlackTree<Integer> source = RedBlackTree.empty(counting);
            while (source.size() < size) {
                source = source.insert(random.nextInt(4 * size + 1));
            }
            comparisons.set(0);
            RedBlackTreeModule.Node.filter(source, element -> (element & 1) == 0);
            RedBlackTreeModule.Node.filter(source, element -> random.nextBoolean());
            RedBlackTreeModule.Node.partition(source, element -> element % 3 == 0);
            assertThat(comparisons.get()).isZero();
        }
    }

    @Test
    public void shouldKeepTheElementsOfAComparatorInconsistentWithEquals() {
        RedBlackTree<String> source = RedBlackTree.empty(String.CASE_INSENSITIVE_ORDER);
        for (String s : java.util.List.of("delta", "Alpha", "charlie", "Bravo", "echo", "ALPHA", "Foxtrot", "golf")) {
            source = source.insert(s);
        }
        final RedBlackTree<String> tree = source;
        check(tree, s -> Character.isUpperCase(s.charAt(0)));
        check(tree, s -> s.length() > 4);
        assertThat(elements(RedBlackTreeModule.Node.filter(tree, s -> s.startsWith("A")))).containsExactly("ALPHA");
    }

    @Test
    public void shouldPropagateAThrowingPredicateAndLeaveTheSourceIntact() {
        final Random random = new Random(SEED + 4);
        final RedBlackTree<Integer> source = sources(NATURAL, 100, random).get(2);
        final java.util.List<Integer> before = elements(source);
        final AtomicInteger calls = new AtomicInteger();
        final Predicate<Integer> throwing = element -> {
            if (calls.incrementAndGet() == 50) {
                throw new IllegalStateException("boom");
            }
            return (element & 1) == 0;
        };
        assertThatThrownBy(() -> RedBlackTreeModule.Node.filter(source, throwing)).hasMessage("boom");
        calls.set(0);
        assertThatThrownBy(() -> RedBlackTreeModule.Node.partition(source, throwing)).hasMessage("boom");
        assertValid(source);
        assertThat(elements(source)).isEqualTo(before);
    }

    @Test
    public void shouldReturnTheEmptyTreeItselfForAnEmptySource() {
        final RedBlackTree<Integer> empty = RedBlackTree.empty(NATURAL);
        assertThat(RedBlackTreeModule.Node.filter(empty, element -> true)).isSameAs(empty);
        final Tuple2<RedBlackTree<Integer>, RedBlackTree<Integer>> partition = RedBlackTreeModule.Node.partition(empty, element -> true);
        assertThat(partition._1()).isSameAs(empty);
        assertThat(partition._2()).isSameAs(empty);
    }

    @Test
    public void shouldJoinWithAnEmptySideWithoutCallingTheComparator() {
        final AtomicInteger comparisons = new AtomicInteger();
        final Comparator<Integer> counting = (a, b) -> {
            comparisons.incrementAndGet();
            return Integer.compare(a, b);
        };
        for (int size : SIZES) {
            RedBlackTree<Integer> tree = RedBlackTree.empty(counting);
            for (int i = 1; i <= size; i++) {
                tree = tree.insert(i);
            }
            final RedBlackTree<Integer> empty = tree.emptyInstance();
            comparisons.set(0);
            final RedBlackTree<Integer> withMin = RedBlackTreeModule.Node.join(empty, 0, tree);
            final RedBlackTree<Integer> withMax = RedBlackTreeModule.Node.join(tree, size + 1, empty);
            assertThat(comparisons.get()).isZero();
            assertValid(withMin);
            assertValid(withMax);
            assertThat(elements(withMin)).isEqualTo(java.util.stream.IntStream.rangeClosed(0, size).boxed().toList());
            assertThat(elements(withMax)).isEqualTo(java.util.stream.IntStream.rangeClosed(1, size + 1).boxed().toList());
        }
    }
}
