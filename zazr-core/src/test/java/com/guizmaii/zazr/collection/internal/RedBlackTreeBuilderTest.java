package com.guizmaii.zazr.collection.internal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Random;
import java.util.function.IntFunction;
import org.junit.jupiter.api.Test;

import static com.guizmaii.zazr.collection.internal.RedBlackTreeValidity.assertValid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RedBlackTreeBuilderTest {

    private static final long SEED = 20260925L;
    // the usual boundaries, and every power of two with its neighbours: the size where the deepest level fills up
    private static final int[] SIZES = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 15, 16, 17, 31, 32, 33, 63, 64, 65, 1023, 1024, 1025,
            32767, 32768, 32769, 100_000 };

    private static final Comparator<Integer> NATURAL = Comparators.naturalComparator();
    private static final Comparator<Integer> REVERSED = NATURAL.reversed();

    private static RedBlackTree<Integer> fromOrdered(Comparator<Integer> order, int size) {
        final Object[] sorted = new Object[size];
        for (int i = 0; i < size; i++) {
            sorted[i] = (order == NATURAL) ? i : size - 1 - i;
        }
        return RedBlackTreeModule.Node.fromOrdered(new RedBlackTreeModule.Empty<>(order), sorted, size);
    }

    private static java.util.List<Integer> elements(RedBlackTree<Integer> tree) {
        final java.util.List<Integer> result = new ArrayList<>();
        tree.forEach(result::add);
        return result;
    }

    // the depth of the deepest node, the root being on level 1
    private static int height(RedBlackTree<?> tree) {
        if (tree.isEmpty()) {
            return 0;
        }
        return 1 + Math.max(height(tree.left()), height(tree.right()));
    }

    @Test
    public void shouldBuildAValidTreeOfEverySizeUpTo2100() {
        for (Comparator<Integer> order : java.util.List.of(NATURAL, REVERSED)) {
            for (int size = 0; size <= 2100; size++) {
                final RedBlackTree<Integer> tree = fromOrdered(order, size);
                assertValid(tree);
                assertThat(tree.size()).isEqualTo(size);
                assertThat(tree.comparator()).isSameAs(order);
            }
        }
    }

    @Test
    public void shouldBuildAValidBalancedTreeAtEveryBoundary() {
        for (int size : SIZES) {
            final RedBlackTree<Integer> tree = fromOrdered(NATURAL, size);
            assertValid(tree);
            assertThat(elements(tree)).containsExactlyElementsOf(java.util.stream.IntStream.range(0, size).boxed().toList());
            // perfectly balanced: as shallow as a binary tree of that size can be
            assertThat(height(tree)).isEqualTo(Integer.SIZE - Integer.numberOfLeadingZeros(size));
        }
    }

    @Test
    public void shouldBuildTreesThatTheTreeOperationsKeepValid() {
        final Random random = new Random(SEED);
        for (int size : new int[] { 1, 2, 3, 31, 32, 33, 1023, 1024, 1025 }) {
            final RedBlackTree<Integer> tree = fromOrdered(NATURAL, size);
            for (int i = 0; i < 20; i++) {
                final int value = random.nextInt(size * 2 + 1) - 1;
                final RedBlackTree<Integer> inserted = tree.insert(value);
                assertValid(inserted);
                final RedBlackTree<Integer> deleted = tree.delete(value);
                assertValid(deleted);
                assertThat(deleted.size()).isEqualTo(tree.contains(value) ? size - 1 : size);
            }
            final int n = random.nextInt(size + 1);
            assertValid(RedBlackTreeModule.Node.take(tree, n));
            assertValid(RedBlackTreeModule.Node.drop(tree, n));
            assertValid(tree.union(fromOrdered(NATURAL, size / 2 + 7)));
        }
    }

    // the builder: sort, keep the last of equal elements, build

    private static RedBlackTree<Integer> build(Comparator<Integer> order, Iterable<Integer> elements) {
        final RedBlackTreeBuilder<Integer> builder = new RedBlackTreeBuilder<>(order, "test");
        for (Integer element : elements) {
            builder.checkOpen();
            builder.add(element);
        }
        return builder.result();
    }

    // successive persistent insertions, the reference the builder is compared with
    private static <T> RedBlackTree<T> inserted(Comparator<T> order, Iterable<T> elements) {
        RedBlackTree<T> tree = RedBlackTree.empty(order);
        for (T element : elements) {
            tree = tree.insert(element);
        }
        return tree;
    }

    @Test
    public void shouldMatchTheJdkTreeSetOnRandomInputsWithDuplicates() {
        final Random random = new Random(SEED);
        for (Comparator<Integer> order : java.util.List.of(NATURAL, REVERSED)) {
            for (int round = 0; round < 300; round++) {
                final int size = (round < SIZES.length - 1) ? SIZES[round] : random.nextInt(3000);
                final int range = 1 + random.nextInt(Math.max(1, size * 2));
                final java.util.List<Integer> input = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    input.add(random.nextInt(range));
                }
                final java.util.TreeSet<Integer> oracle = new java.util.TreeSet<>(order);
                oracle.addAll(input);
                final RedBlackTree<Integer> tree = build(order, input);
                assertValid(tree);
                assertThat(elements(tree)).containsExactlyElementsOf(oracle);
                // the same elements as successive insertions
                assertThat(elements(tree)).isEqualTo(elements(inserted(order, input)));
            }
        }
    }

    @Test
    public void shouldKeepTheLastOfEqualElements() {
        final Comparator<String> caseInsensitive = String.CASE_INSENSITIVE_ORDER;
        final RedBlackTreeBuilder<String> builder = new RedBlackTreeBuilder<>(caseInsensitive, "test");
        for (String s : new String[] { "b", "A", "a", "B", "c", "b" }) {
            builder.add(s);
        }
        final RedBlackTree<String> tree = builder.result();
        assertValid(tree);
        final java.util.List<String> actual = new ArrayList<>();
        tree.forEach(actual::add);
        assertThat(actual).containsExactly("a", "b", "c");
        // successive insertions keep the last one too
        assertThat(inserted(caseInsensitive, java.util.List.of("b", "A", "a", "B", "c", "b"))).containsExactly("a", "b", "c");
    }

    @Test
    public void shouldCountDistinctElementsWithoutChangingTheResult() {
        final Random random = new Random(SEED);
        for (int round = 0; round < 50; round++) {
            final RedBlackTreeBuilder<Integer> builder = new RedBlackTreeBuilder<>(NATURAL, "test");
            final java.util.TreeSet<Integer> oracle = new java.util.TreeSet<>();
            final int size = random.nextInt(2000);
            for (int i = 0; i < size; i++) {
                final int value = random.nextInt(size + 1);
                builder.add(value);
                oracle.add(value);
                if (random.nextInt(50) == 0) {
                    assertThat(builder.size()).isEqualTo(oracle.size());
                }
            }
            assertThat(builder.size()).isEqualTo(oracle.size());
            assertThat(builder.size()).isEqualTo(oracle.size());
            final RedBlackTree<Integer> tree = builder.result();
            assertValid(tree);
            assertThat(elements(tree)).containsExactlyElementsOf(oracle);
        }
    }

    @Test
    public void shouldKeepTheLastAddedAcrossACompactedPrefix() {
        // the entries after a size() call are sorted with the compacted prefix: the later one must still win
        final RedBlackTreeBuilder<String> builder = new RedBlackTreeBuilder<>(String.CASE_INSENSITIVE_ORDER, "test");
        builder.add("x");
        builder.add("y");
        assertThat(builder.size()).isEqualTo(2);
        builder.add("X");
        assertThat(builder.size()).isEqualTo(2);
        builder.add("Y");
        builder.add("z");
        final java.util.List<String> actual = new ArrayList<>();
        builder.result().forEach(actual::add);
        assertThat(actual).containsExactly("X", "Y", "z");
    }

    @Test
    public void shouldBuildTheEmptyTreeWithTheComparator() {
        final RedBlackTree<Integer> tree = new RedBlackTreeBuilder<>(REVERSED, "test").result();
        assertThat(tree.isEmpty()).isTrue();
        assertThat(tree.comparator()).isSameAs(REVERSED);
    }

    @Test
    public void shouldRefuseAnyUseAfterResult() {
        final RedBlackTreeBuilder<Integer> builder = new RedBlackTreeBuilder<>(NATURAL, "Some.Builder");
        builder.add(1);
        builder.result();
        assertThatThrownBy(builder::checkOpen).isInstanceOf(IllegalStateException.class)
                .hasMessage("result() has already been called on this Some.Builder");
        assertThatThrownBy(builder::size).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(builder::result).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void shouldRefuseAnyUseAfterTheComparatorThrew() {
        final IntFunction<Comparator<Integer>> failingAfter = n -> new Comparator<>() {
            int calls;

            @Override
            public int compare(Integer a, Integer b) {
                if (++calls > n) {
                    throw new IllegalArgumentException("boom");
                }
                return Integer.compare(a, b);
            }
        };
        final RedBlackTreeBuilder<Integer> builder = new RedBlackTreeBuilder<>(failingAfter.apply(5), "Some.Builder");
        for (int i = 100; i > 0; i--) {
            builder.add(i);
        }
        assertThatThrownBy(builder::result).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(builder::checkOpen).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("the comparator threw");
        assertThatThrownBy(builder::size).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(builder::result).isInstanceOf(IllegalStateException.class);
    }
}
