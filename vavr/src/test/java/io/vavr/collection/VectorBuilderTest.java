package io.vavr.collection;

import java.util.ArrayList;
import java.util.Random;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class VectorBuilderTest {

    // sizes around every leaf (32), node (1024) and second-level node (32768) boundary
    private static final int[] SIZES = { 0, 1, 31, 32, 33, 63, 64, 65, 1023, 1024, 1025, 32767, 32768, 32769, 1_000_000 };

    @Test
    public void shouldBuildEmptyVector() {
        final Vector<Integer> actual = Vector.<Integer> newBuilder().result();
        assertThat(actual).isEmpty();
        assertThat(actual).isSameAs(Vector.empty());
    }

    @Test
    public void shouldBuildTheSameVectorAsOfAllAtEveryBoundary() {
        for (int size : SIZES) {
            final Vector.Builder<Integer> builder = Vector.newBuilder();
            for (int i = 0; i < size; i++) {
                builder.add(i);
            }
            assertThat(builder.size()).isEqualTo(size);
            assertSameElements(builder.result(), Vector.ofAll(IntStream.range(0, size).boxed().toList()), size);
        }
    }

    @Test
    public void shouldHonourSizeHintAtEveryBoundary() {
        for (int size : SIZES) {
            for (int hint : new int[] { 0, 1, size / 2, size, size + 1, 2 * size + 1 }) {
                final Vector.Builder<Integer> builder = Vector.newBuilder(hint);
                for (int i = 0; i < size; i++) {
                    builder.add(i);
                }
                assertSameElements(builder.result(), Vector.range(0, size), size);
            }
        }
    }

    @Test
    public void shouldRejectNegativeSizeHint() {
        assertThatThrownBy(() -> Vector.newBuilder(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void shouldAddAllVectorsAtEveryBoundary() {
        for (int prefix : SIZES) {
            if (prefix > 32769) {
                continue;
            }
            for (int size : SIZES) {
                if (size > 32769) {
                    continue;
                }
                final Vector.Builder<Integer> builder = Vector.newBuilder();
                for (int i = 0; i < prefix; i++) {
                    builder.add(i);
                }
                builder.addAll(Vector.range(prefix, prefix + size));
                assertSameElements(builder.result(), Vector.range(0, prefix + size), prefix + size);
            }
        }
    }

    @Test
    public void shouldAddAllSlicedVectorsWhoseLeavesAreNotAligned() {
        final Vector<Integer> source = Vector.range(0, 5000);
        for (int from : new int[] { 0, 1, 17, 31, 32, 33, 1000, 1024, 1025 }) {
            for (int to : new int[] { from, from + 1, from + 31, from + 32, from + 33, 3000, 4095, 4096, 4097, 5000 }) {
                if (to < from) {
                    continue;
                }
                final Vector<Integer> slice = source.slice(from, to);
                final Vector<Integer> actual = Vector.<Integer> newBuilder().add(-1).addAll(slice).add(-2).result();
                assertSameElements(actual, Vector.of(-1).appendAll(slice).append(-2), slice.size() + 2);
            }
        }
    }

    @Test
    public void shouldAddAllObjectBackedVectorsAndKeepTheShapeOfOfAll() {
        for (int size : new int[] { 1, 31, 32, 33, 63, 64, 65, 1023, 1024, 1025, 32768, 32769 }) {
            final java.util.List<Integer> list = IntStream.range(0, size).boxed().toList();
            final Vector<Integer> boxed = Vector.ofAll(list);                 // Object[] leaves via toArray
            final Vector<Integer> varargs = Vector.of(list.toArray(new Integer[0])); // Object[] leaves via of(T...)
            assertThat(boxed.trie.getLeaf(0)).isInstanceOf(Object[].class);
            for (Vector<Integer> source : java.util.List.of(boxed, varargs)) {
                assertSameShape(Vector.<Integer> newBuilder().addAll(source).result(), boxed, size);
                // shared full leaves followed by more elements, and a prefix before the shared leaves
                assertSameShape(Vector.<Integer> newBuilder().addAll(source).add(size).result(), Vector.range(0, size + 1), size + 1);
                assertSameShape(Vector.<Integer> newBuilder().add(-1).addAll(source).result(), Vector.range(-1, size), size + 1);
                assertSameShape(Vector.<Integer> newBuilder().addAll(source).addAll(source).result(), boxed.appendAll(boxed), 2 * size);
            }
        }
    }

    @Test
    public void shouldNotLeaveAnEmptyTrailingLeafAfterSharingFullLeaves() {
        final Vector<Integer> v32 = Vector.ofAll(IntStream.range(0, 32).boxed().toList());
        final Vector<Integer> built32 = Vector.<Integer> newBuilder().addAll(v32).result();
        assertThat(built32.trie.depthShift()).isZero();
        assertThat(built32.trie.getLeaf(0)).isInstanceOf(Object[].class).satisfies(leaf -> assertThat(((Object[]) leaf).length).isEqualTo(32));
        final Vector<Integer> v1024 = Vector.ofAll(IntStream.range(0, 1024).boxed().toList());
        final Vector<Integer> built1024 = Vector.<Integer> newBuilder().addAll(v1024).result();
        assertThat(built1024.trie.depthShift()).isEqualTo(5);
        assertSameShape(built1024, v1024, 1024);
    }

    @Test
    public void shouldAddAllSlicedObjectBackedVectorsWhosePartiallyLiveLeavesAreCopiedAndFullyLiveOnesShared() {
        // drop(k) leaves the first leaf partially live (k % 32 elements dead at its front) when k is not a multiple of 32,
        // take(n) leaves the last leaf partially live: addLeafRange must copy those and may share only the full ones between
        final Vector<Integer> source = Vector.ofAll(IntStream.range(0, 4200).boxed().toList());
        assertThat(source.trie.getLeaf(0)).isInstanceOf(Object[].class);
        for (int k : new int[] { 1, 5, 31, 32, 33 }) {
            for (int n : new int[] { 0, 1, 31, 32, 33, 63, 64, 65, 1023, 1024, 1025, 2047, 2048, 2049 }) {
                final Vector<Integer> slice = source.drop(k).take(n);
                assertThat(slice.trie.getLeaf(0)).isInstanceOf(Object[].class);
                final Vector<Integer> expected = Vector.range(k, k + n);
                assertSameShape(Vector.<Integer> newBuilder().addAll(slice).result(), expected, n);
                assertSameShape(Vector.<Integer> newBuilder().addAll(slice).add(k + n).result(), Vector.range(k, k + n + 1), n + 1);
                assertSameShape(Vector.<Integer> newBuilder().add(k - 1).addAll(slice).result(), Vector.range(k - 1, k + n), n + 1);
                assertSameShape(Vector.<Integer> newBuilder().addAll(slice).addAll(slice).result(), expected.appendAll(expected), 2 * n);
            }
        }
    }

    @Test
    public void shouldAddAllPrimitiveBackedVector() {
        final Vector<Integer> ints = Vector.ofAll(IntStream.range(0, 1025).toArray());
        assertSameElements(Vector.<Integer> newBuilder().addAll(ints).result(), ints, 1025);
    }

    @Test
    public void shouldAddAllNonVectorIterables() {
        final Vector<Integer> actual = Vector.<Integer> newBuilder()
                .addAll(List.range(0, 100))
                .addAll(java.util.List.of(100, 101))
                .addAll(HashSet.of(102))
                .result();
        assertSameElements(actual, Vector.range(0, 103), 103);
    }

    @Test
    public void shouldAcceptNullElements() {
        final Vector<Integer> actual = Vector.<Integer> newBuilder().add(null).add(1).add(null).result();
        assertThat(actual).containsExactly(null, 1, null);
    }

    @Test
    public void shouldThrowOnAnyUseAfterResult() {
        final Vector.Builder<Integer> builder = Vector.newBuilder();
        builder.add(1);
        builder.result();
        assertThatThrownBy(() -> builder.add(2)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> builder.addAll(Vector.of(2))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(builder::result).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(builder::size).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void shouldProduceVectorsThatSupportEveryOperation() {
        final Vector<Integer> built = Vector.<Integer> newBuilder().addAll(Vector.range(0, 5000)).add(5000).result();
        assertThat(built.append(5001).get(5001)).isEqualTo(5001);
        assertThat(built.prepend(-1).get(0)).isEqualTo(-1);
        assertThat(built.update(4999, -2).get(4999)).isEqualTo(-2);
        assertThat(built.drop(1024).head()).isEqualTo(1024);
        assertThat(built.take(1025).last()).isEqualTo(1024);
        assertThat(built.reverse().head()).isEqualTo(5000);
        assertThat(built.map(i -> i * 2).get(2500)).isEqualTo(5000);
        assertThat(built.filter(i -> i % 2 == 0).size()).isEqualTo(2501);
        assertThat(built.iterator().toList().size()).isEqualTo(5001);
    }

    @Test
    public void shouldMatchArrayListOracleUnderRandomMixOfAddAndAddAll() {
        final Random random = new Random(20260915L);
        for (int round = 0; round < 50; round++) {
            final ArrayList<Integer> expected = new ArrayList<>();
            final Vector.Builder<Integer> builder = Vector.newBuilder(random.nextInt(50));
            final int steps = 1 + random.nextInt(200);
            for (int step = 0; step < steps; step++) {
                switch (random.nextInt(4)) {
                    case 0 -> {
                        final int value = random.nextInt();
                        expected.add(value);
                        builder.add(value);
                    }
                    case 1 -> {
                        final int from = random.nextInt(2000), count = random.nextInt(1100);
                        final Vector<Integer> vector = Vector.range(0, 3000).slice(from, Math.min(3000, from + count));
                        expected.addAll(vector.toJavaList());
                        builder.addAll(vector);
                    }
                    case 2 -> {
                        final java.util.List<Integer> list = IntStream.range(0, random.nextInt(70)).boxed().toList();
                        expected.addAll(list);
                        builder.addAll(list);
                    }
                    default -> {
                        final Vector<Integer> vector = Vector.ofAll(IntStream.range(0, random.nextInt(100)).toArray());
                        expected.addAll(vector.toJavaList());
                        builder.addAll(vector);
                    }
                }
                assertThat(builder.size()).isEqualTo(expected.size());
            }
            assertSameElements(builder.result(), Vector.ofAll(expected), expected.size());
        }
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 32, 33, 1024, 1025, 100_000 })
    public void shouldRouteBulkOperationsThroughTheBuilder(int size) {
        final Vector<Integer> range = Vector.range(0, size);
        final java.util.List<Integer> list = IntStream.range(0, size).boxed().toList();
        assertSameElements(Vector.ofAll(Iterator.ofAll(list.iterator())), range, size);
        assertSameElements(Vector.ofAll(list.stream()), range, size);
        assertSameElements(list.stream().collect(Vector.collector()), range, size);
        assertSameElements(list.parallelStream().collect(Vector.collector()), range, size);
        assertSameElements(Vector.tabulate(size, i -> i), range, size);
        assertSameElements(Vector.fill(size, 7), Vector.ofAll(java.util.Collections.nCopies(size, 7)), size);
        assertSameElements(range.map(i -> i + 1), Vector.range(1, size + 1), size);
        assertSameElements(range.filter(i -> i % 3 == 0), Vector.ofAll(list.stream().filter(i -> i % 3 == 0).toList()), (size + 2) / 3);
        assertSameElements(range.reject(i -> i % 3 == 0), Vector.ofAll(list.stream().filter(i -> i % 3 != 0).toList()), size - (size + 2) / 3);
        assertSameElements(range.flatMap(i -> Vector.of(i, i)), Vector.ofAll(list.stream().flatMap(i -> java.util.stream.Stream.of(i, i)).toList()), 2 * size);
        assertSameElements(range.appendAll(Iterator.ofAll(list.iterator())), range.appendAll(range), 2 * size);
        assertThat(range.filter(i -> true)).isSameAs(range);
    }

    private static <T> void assertSameShape(Vector<T> actual, Vector<T> expected, int size) {
        assertSameElements(actual, expected, size);
        assertThat(actual.trie.depthShift()).as("depthShift for size %d", size).isEqualTo(expected.trie.depthShift());
        assertThat(actual.trie.depthShift()).isEqualTo(Vector.range(0, size).trie.depthShift());
    }

    private static <T> void assertSameElements(Vector<T> actual, Vector<T> expected, int size) {
        assertThat(actual.size()).isEqualTo(size);
        assertThat(expected.size()).isEqualTo(size);
        final java.util.Iterator<T> it = actual.iterator();
        for (int i = 0; i < size; i++) {
            final T e = expected.get(i);
            assertThat(actual.get(i)).as("get(%d)", i).isEqualTo(e);
            assertThat(it.next()).as("iterator element %d", i).isEqualTo(e);
        }
        assertThat(it.hasNext()).isFalse();
        assertThat(actual).isEqualTo(expected);
    }
}
