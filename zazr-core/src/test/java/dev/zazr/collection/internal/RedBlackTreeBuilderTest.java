package dev.zazr.collection.internal;

import dev.zazr.collection.Vector;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Random;
import java.util.function.IntFunction;
import org.junit.jupiter.api.Test;

import static dev.zazr.collection.internal.RedBlackTreeValidity.assertValid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RedBlackTreeBuilderTest {

    private static final long SEED = 20260925L;
    // the usual boundaries, and every power of two with its neighbours: the size where the deepest level fills up
    private static final int[] SIZES = {
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 15, 16, 17, 31, 32, 33, 63, 64, 65, 1023, 1024, 1025, 32767, 32768, 32769, 100_000
    };

    private static final Comparator<Integer> NATURAL = Comparators.naturalComparator();
    private static final Comparator<Integer> REVERSED = NATURAL.reversed();

    private static RedBlackTree<Integer> fromOrdered(Comparator<Integer> order, int size) {
        Object[] sorted = new Object[size];
        for (int i = 0; i < size; i++) {
            sorted[i] = (order == NATURAL) ? i : size - 1 - i;
        }
        return RedBlackTreeModule.Node.fromOrdered(new RedBlackTreeModule.Empty<>(order), sorted, size);
    }

    private static java.util.List<Integer> elements(RedBlackTree<Integer> tree) {
        java.util.List<Integer> result = new ArrayList<>();
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
                RedBlackTree<Integer> tree = fromOrdered(order, size);
                assertValid(tree);
                assertThat(tree.size()).isEqualTo(size);
                assertThat(tree.comparator()).isSameAs(order);
            }
        }
    }

    @Test
    public void shouldBuildAValidBalancedTreeAtEveryBoundary() {
        for (int size : SIZES) {
            RedBlackTree<Integer> tree = fromOrdered(NATURAL, size);
            assertValid(tree);
            assertThat(elements(tree))
                    .containsExactlyElementsOf(
                            java.util.stream.IntStream.range(0, size).boxed().toList());
            // perfectly balanced: as shallow as a binary tree of that size can be
            assertThat(height(tree)).isEqualTo(Integer.SIZE - Integer.numberOfLeadingZeros(size));
        }
    }

    @Test
    public void shouldBuildTreesThatTheTreeOperationsKeepValid() {
        Random random = new Random(SEED);
        for (int size : new int[] {1, 2, 3, 31, 32, 33, 1023, 1024, 1025}) {
            RedBlackTree<Integer> tree = fromOrdered(NATURAL, size);
            for (int i = 0; i < 20; i++) {
                int value = random.nextInt(size * 2 + 1) - 1;
                RedBlackTree<Integer> inserted = tree.insert(value);
                assertValid(inserted);
                RedBlackTree<Integer> deleted = tree.delete(value);
                assertValid(deleted);
                assertThat(deleted.size()).isEqualTo(tree.contains(value) ? size - 1 : size);
            }
            int n = random.nextInt(size + 1);
            assertValid(RedBlackTreeModule.Node.take(tree, n));
            assertValid(RedBlackTreeModule.Node.drop(tree, n));
            assertValid(tree.union(fromOrdered(NATURAL, size / 2 + 7)));
        }
    }

    // the builder: sort, keep the last of equal elements, build

    private static RedBlackTree<Integer> build(Comparator<Integer> order, Iterable<Integer> elements) {
        RedBlackTreeBuilder<Integer> builder = new RedBlackTreeBuilder<>(order, "test");
        for (Integer element : elements) {
            builder.checkOpen();
            builder.add(element);
        }
        return builder.result();
    }

    // successive persistent insertions, the reference the builder is compared with
    private static <T> RedBlackTree<T> inserted(Comparator<T> order, Iterable<T> elements) {
        return Vector.ofAll(elements).foldLeft(RedBlackTree.empty(order), RedBlackTree::insert);
    }

    @Test
    public void shouldMatchTheJdkTreeSetOnRandomInputsWithDuplicates() {
        Random random = new Random(SEED);
        for (Comparator<Integer> order : java.util.List.of(NATURAL, REVERSED)) {
            for (int round = 0; round < 300; round++) {
                int size = (round < SIZES.length - 1) ? SIZES[round] : random.nextInt(3000);
                int range = 1 + random.nextInt(Math.max(1, size * 2));
                java.util.List<Integer> input = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    input.add(random.nextInt(range));
                }
                java.util.TreeSet<Integer> oracle = new java.util.TreeSet<>(order);
                oracle.addAll(input);
                RedBlackTree<Integer> tree = build(order, input);
                assertValid(tree);
                assertThat(elements(tree)).containsExactlyElementsOf(oracle);
                // the same elements as successive insertions
                assertThat(elements(tree)).isEqualTo(elements(inserted(order, input)));
            }
        }
    }

    @Test
    public void shouldKeepTheLastOfEqualElements() {
        Comparator<String> caseInsensitive = String.CASE_INSENSITIVE_ORDER;
        RedBlackTreeBuilder<String> builder = new RedBlackTreeBuilder<>(caseInsensitive, "test");
        for (String s : new String[] {"b", "A", "a", "B", "c", "b"}) {
            builder.add(s);
        }
        RedBlackTree<String> tree = builder.result();
        assertValid(tree);
        java.util.List<String> actual = new ArrayList<>();
        tree.forEach(actual::add);
        assertThat(actual).containsExactly("a", "b", "c");
        // successive insertions keep the last one too
        assertThat(inserted(caseInsensitive, java.util.List.of("b", "A", "a", "B", "c", "b")))
                .containsExactly("a", "b", "c");
    }

    @Test
    public void shouldCountDistinctElementsWithoutChangingTheResult() {
        Random random = new Random(SEED);
        for (int round = 0; round < 50; round++) {
            RedBlackTreeBuilder<Integer> builder = new RedBlackTreeBuilder<>(NATURAL, "test");
            java.util.TreeSet<Integer> oracle = new java.util.TreeSet<>();
            int size = random.nextInt(2000);
            for (int i = 0; i < size; i++) {
                int value = random.nextInt(size + 1);
                builder.add(value);
                oracle.add(value);
                if (random.nextInt(50) == 0) {
                    assertThat(builder.size()).isEqualTo(oracle.size());
                }
            }
            assertThat(builder.size()).isEqualTo(oracle.size());
            assertThat(builder.size()).isEqualTo(oracle.size());
            RedBlackTree<Integer> tree = builder.result();
            assertValid(tree);
            assertThat(elements(tree)).containsExactlyElementsOf(oracle);
        }
    }

    @Test
    public void shouldKeepTheLastAddedAcrossACompactedPrefix() {
        // the entries after a size() call are sorted with the compacted prefix: the later one must still win
        RedBlackTreeBuilder<String> builder = new RedBlackTreeBuilder<>(String.CASE_INSENSITIVE_ORDER, "test");
        builder.add("x");
        builder.add("y");
        assertThat(builder.size()).isEqualTo(2);
        builder.add("X");
        assertThat(builder.size()).isEqualTo(2);
        builder.add("Y");
        builder.add("z");
        java.util.List<String> actual = new ArrayList<>();
        builder.result().forEach(actual::add);
        assertThat(actual).containsExactly("X", "Y", "z");
    }

    @Test
    public void shouldBuildTheEmptyTreeWithTheComparator() {
        RedBlackTree<Integer> tree = new RedBlackTreeBuilder<>(REVERSED, "test").result();
        assertThat(tree.isEmpty()).isTrue();
        assertThat(tree.comparator()).isSameAs(REVERSED);
    }

    @Test
    public void shouldRefuseAnyUseAfterResult() {
        RedBlackTreeBuilder<Integer> builder = new RedBlackTreeBuilder<>(NATURAL, "Some.Builder");
        builder.add(1);
        builder.result();
        assertThatThrownBy(builder::checkOpen)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("result() has already been called on this Some.Builder");
        assertThatThrownBy(builder::size).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(builder::result).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void shouldRefuseAnyUseAfterTheComparatorThrew() {
        IntFunction<Comparator<Integer>> failingAfter = n -> new Comparator<>() {
            int calls;

            @Override
            public int compare(Integer a, Integer b) {
                if (++calls > n) {
                    throw new IllegalArgumentException("boom");
                }
                return Integer.compare(a, b);
            }
        };
        RedBlackTreeBuilder<Integer> builder = new RedBlackTreeBuilder<>(failingAfter.apply(5), "Some.Builder");
        for (int i = 100; i > 0; i--) {
            builder.add(i);
        }
        assertThatThrownBy(builder::result).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(builder::checkOpen)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("the comparator threw");
        assertThatThrownBy(builder::size).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(builder::result).isInstanceOf(IllegalStateException.class);
    }

    // equal under the comparator (same value modulo 1000), told apart by reference
    private static final Comparator<Integer> MODULO = Comparator.comparingInt(i -> Math.floorMod(i, 1000));

    @Test
    public void shouldGrowAPresizedBufferOfEveryCapacity() {
        for (int capacity : new int[] {0, 1, 2, 3, 16, 17}) {
            RedBlackTreeBuilder<Integer> builder = new RedBlackTreeBuilder<>(NATURAL, "Some.Builder", capacity, false);
            for (int i = 99; i >= 0; i--) {
                builder.add(i);
            }
            RedBlackTree<Integer> tree = builder.result();
            assertValid(tree);
            assertThat(elements(tree))
                    .isEqualTo(java.util.stream.IntStream.range(0, 100).boxed().toList());
        }
    }

    @Test
    public void shouldKeepTheFirstOrTheLastOfEqualElements() {
        Random random = new Random(SEED);
        for (boolean keepFirst : new boolean[] {false, true}) {
            for (int size : new int[] {0, 1, 2, 31, 32, 33, 1023, 1024, 1025}) {
                RedBlackTreeBuilder<Integer> builder = new RedBlackTreeBuilder<>(MODULO, "Some.Builder", 0, keepFirst);
                // the expected kept object per class, and a size() call now and then, which compacts the buffer
                java.util.Map<Integer, Integer> kept = new java.util.TreeMap<>();
                for (int i = 0; i < 3 * size; i++) {
                    Integer element =
                            Integer.valueOf(random.nextInt(Math.max(1, size)) + 1000 * (1 + random.nextInt(5)));
                    builder.add(element);
                    if (keepFirst) {
                        kept.putIfAbsent(Math.floorMod(element, 1000), element);
                    } else {
                        kept.put(Math.floorMod(element, 1000), element);
                    }
                    if (random.nextInt(8) == 0) {
                        assertThat(builder.size()).isEqualTo(kept.size());
                    }
                }
                RedBlackTree<Integer> tree = builder.result();
                assertValid(tree);
                java.util.List<Integer> actual = elements(tree);
                java.util.List<Integer> expected = new ArrayList<>(kept.values());
                assertThat(actual).hasSameSizeAs(expected);
                for (int i = 0; i < expected.size(); i++) {
                    assertThat(actual.get(i)).isSameAs(expected.get(i));
                }
            }
        }
    }
}
