package com.guizmaii.zazr.collection.internal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Random;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

import static com.guizmaii.zazr.collection.internal.RedBlackTreeValidity.assertValid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** {@code Node.mapOrdered}: a copy of a tree with the same shape and colours, holding mapped elements. */
public class RedBlackTreeMapOrderedTest {

    private static final long SEED = 20260925L;
    private static final int[] SIZES = { 0, 1, 2, 3, 4, 5, 7, 8, 9, 31, 32, 33, 63, 64, 65, 1023, 1024, 1025, 4097 };

    private static final Comparator<Integer> NATURAL = Comparators.naturalComparator();
    private static final Comparator<Integer> REVERSED = NATURAL.reversed();
    private static final Comparator<Long> LONG_NATURAL = Comparators.naturalComparator();
    private static final Comparator<Long> LONG_REVERSED = LONG_NATURAL.reversed();

    // the three ways a tree gets its shape: one insert at a time (ascending, then random order, which puts red nodes
    // at every depth), inserts followed by deletes (the rebalancing of deletion), and the bottom-up construction
    private static java.util.List<RedBlackTree<Integer>> sources(Comparator<Integer> order, int size, Random random) {
        final java.util.List<RedBlackTree<Integer>> trees = new ArrayList<>();
        RedBlackTree<Integer> ascending = RedBlackTree.empty(order);
        for (int i = 0; i < size; i++) {
            ascending = ascending.insert(i);
        }
        trees.add(ascending);
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

    private static void assertSameShape(RedBlackTree<?> actual, RedBlackTree<?> expected) {
        assertThat(actual.isEmpty()).isEqualTo(expected.isEmpty());
        if (!expected.isEmpty()) {
            final RedBlackTreeModule.Node<?> a = (RedBlackTreeModule.Node<?>) actual;
            final RedBlackTreeModule.Node<?> e = (RedBlackTreeModule.Node<?>) expected;
            assertThat(a.color).isEqualTo(e.color);
            assertThat(a.blackHeight).isEqualTo(e.blackHeight);
            assertThat(a.size).isEqualTo(e.size);
            assertSameShape(a.left, e.left);
            assertSameShape(a.right, e.right);
        }
    }

    private static <T, R> void check(RedBlackTree<T> source, Comparator<R> order, Function<T, R> mapper) {
        final java.util.List<T> calls = new ArrayList<>();
        final RedBlackTree<R> mapped = RedBlackTreeModule.Node.mapOrdered(source, order, t -> {
            calls.add(t);
            return mapper.apply(t);
        });
        assertValid(mapped);
        assertSameShape(mapped, source);
        assertThat(mapped.comparator()).isSameAs(order);
        assertThat(calls).as("the mapper is called once per element, in order").isEqualTo(elements(source));
        assertThat(elements(mapped)).isEqualTo(elements(source).stream().map(mapper).toList());
        for (T element : source) {
            assertThat(mapped.contains(mapper.apply(element))).isTrue();
        }
    }

    @Test
    public void shouldKeepShapeAndColoursAtEveryBoundary() {
        final Random random = new Random(SEED);
        for (int size : SIZES) {
            for (RedBlackTree<Integer> source : sources(NATURAL, size, random)) {
                assertValid(source);
                check(source, LONG_NATURAL, i -> 2L * i + 1);
                check(source, NATURAL, i -> i);
            }
            for (RedBlackTree<Integer> source : sources(REVERSED, size, random)) {
                assertValid(source);
                check(source, LONG_REVERSED, i -> 3L * i);
            }
        }
    }

    @Test
    public void shouldKeepShapeAndColoursOfEverySizeUpTo300() {
        final Random random = new Random(SEED + 1);
        for (int size = 0; size <= 300; size++) {
            for (RedBlackTree<Integer> source : sources(NATURAL, size, random)) {
                check(source, LONG_NATURAL, Integer::longValue);
            }
        }
    }

    @Test
    public void shouldMapUnderACaseInsensitiveComparator() {
        RedBlackTree<String> source = RedBlackTree.empty(String.CASE_INSENSITIVE_ORDER);
        for (String s : java.util.List.of("delta", "Alpha", "charlie", "Bravo", "echo", "ALPHA")) {
            source = source.insert(s);
        }
        // "ALPHA" replaced "Alpha": the tree holds the element inserted last
        assertThat(elements(source)).containsExactly("ALPHA", "Bravo", "charlie", "delta", "echo");
        check(source, String.CASE_INSENSITIVE_ORDER, s -> s.toLowerCase(java.util.Locale.ROOT));
        final Comparator<String> byLength = Comparator.comparingInt(String::length);
        final java.util.List<String> inOrder = elements(source);
        check(source, byLength, s -> "x".repeat(inOrder.indexOf(s) + 1));
    }

    @Test
    public void shouldReturnAnEmptyTreeOfTheGivenComparatorForAnEmptySource() {
        final RedBlackTree<Long> mapped = RedBlackTreeModule.Node.mapOrdered(RedBlackTree.empty(NATURAL), LONG_REVERSED, Integer::longValue);
        assertThat(mapped.isEmpty()).isTrue();
        assertThat(mapped.comparator()).isSameAs(LONG_REVERSED);
    }

    @Test
    public void shouldLeaveTheSourceUnchanged() {
        final RedBlackTree<Integer> source = sources(NATURAL, 1025, new Random(SEED)).get(1);
        final java.util.List<Integer> before = elements(source);
        RedBlackTreeModule.Node.mapOrdered(source, LONG_NATURAL, Integer::longValue);
        assertValid(source);
        assertThat(elements(source)).isEqualTo(before);
    }

    @Test
    public void shouldPropagateAnExceptionOfTheMapper() {
        final RedBlackTree<Integer> source = sources(NATURAL, 33, new Random(SEED)).get(0);
        final java.util.List<Integer> calls = new ArrayList<>();
        assertThatThrownBy(() -> RedBlackTreeModule.Node.mapOrdered(source, NATURAL, i -> {
            calls.add(i);
            if (i == 10) {
                throw new IllegalStateException("boom");
            }
            return i;
        })).isInstanceOf(IllegalStateException.class).hasMessage("boom");
        assertThat(calls).containsExactly(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }
}
