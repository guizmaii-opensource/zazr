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
    // the boundary at 32^4 = 1 048 576, where a fourth trie level is pushed and finalised; kept out of SIZES for the tests
    // that build several Vectors per size
    private static final int[] LEVEL4_SIZES = { 1_048_575, 1_048_576, 1_048_577 };

    @Test
    public void shouldBuildEmptyVector() {
        final Vector<Integer> actual = Vector.<Integer> newBuilder().result();
        assertThat(actual).isEmpty();
        assertThat(actual).isSameAs(Vector.empty());
    }

    @Test
    public void shouldPushAndFinaliseAFourthLevel() {
        for (int size : LEVEL4_SIZES) {
            final Vector.Builder<Integer> builder = Vector.newBuilder();
            for (int i = 0; i < size; i++) {
                builder.add(i);
            }
            assertSameShape(builder.result(), Vector.range(0, size), size);
            // the same boundary reached through shared leaves of an Object[]-backed source
            final Vector<Integer> boxed = Vector.ofAll(IntStream.range(0, size).boxed().toList());
            assertSameShape(Vector.<Integer> newBuilder().addAll(boxed).result(), boxed, size);
            assertSameShape(Vector.<Integer> newBuilder().add(-1).addAll(boxed).result(), Vector.range(-1, size), size + 1);
        }
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
                final Vector<Integer> built = Vector.<Integer> newBuilder().addAll(source).result();
                assertSameShape(built, boxed, size);
                assertSharesFullLeaves(built, source, 0, size);
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
                final Vector<Integer> built = Vector.<Integer> newBuilder().addAll(slice).result();
                assertSameShape(built, expected, n);
                if (k % 32 != 0 && n > 0) {
                    // the source's first leaf is only partially live: the builder must copy it, never share it
                    assertThat(built.trie.getLeaf(0)).isNotSameAs(slice.trie.getLeaf(0));
                    // and, being copied, the built leaves are not aligned with the source's: nothing can be shared
                    for (int i = 0; i < n; i += 32) {
                        assertThat(built.trie.getLeaf(i)).isNotSameAs(slice.trie.getLeaf(i));
                    }
                } else {
                    // aligned: every full leaf is shared, a partially live last leaf is copied
                    assertSharesFullLeaves(built, slice, 0, n);
                }
                assertSameShape(Vector.<Integer> newBuilder().addAll(slice).add(k + n).result(), Vector.range(k, k + n + 1), n + 1);
                assertSameShape(Vector.<Integer> newBuilder().add(k - 1).addAll(slice).result(), Vector.range(k - 1, k + n), n + 1);
                assertSameShape(Vector.<Integer> newBuilder().addAll(slice).addAll(slice).result(), expected.appendAll(expected), 2 * n);
            }
        }
    }

    @Test
    public void shouldShareTheSourceLeafWhenTheCurrentLeafIsExactlyFullButNotYetPushed() {
        final Vector<Integer> v32 = Vector.ofAll(IntStream.range(0, 32).boxed().toList());
        // 32 adds leave the current leaf full but unpushed; the following addAll must close it and share the source leaf
        final Vector.Builder<Integer> builder = Vector.newBuilder();
        for (int i = 0; i < 32; i++) {
            builder.add(-i);
        }
        final Vector<Integer> built = builder.addAll(v32).result();
        assertThat(built.trie.getLeaf(32)).isSameAs(v32.trie.getLeaf(0));
        assertSameShape(built, Vector.tabulate(32, i -> -i).appendAll(v32), 64);
        // the same through a sized builder whose first leaf is exactly the hint
        final Vector.Builder<Integer> hinted = Vector.newBuilder(32);
        for (int i = 0; i < 32; i++) {
            hinted.add(i);
        }
        assertThat(hinted.addAll(v32).result().trie.getLeaf(32)).isSameAs(v32.trie.getLeaf(0));
        // and a collector combiner whose left side ends on a full leaf
        final Vector<Integer> combined = Vector.<Integer> newBuilder().addAll(Vector.tabulate(32, i -> i)).addAll(v32).result();
        assertThat(combined.trie.getLeaf(32)).isSameAs(v32.trie.getLeaf(0));
        // a partially filled current leaf still copies
        assertThat(Vector.<Integer> newBuilder().add(0).addAll(v32).result().trie.getLeaf(32)).isNotSameAs(v32.trie.getLeaf(0));
    }

    @Test
    public void shouldNeverWriteIntoSharedLeavesFromEitherSide() {
        for (int size : new int[] { 32, 64, 1024, 1056 }) {
            final java.util.List<Integer> list = IntStream.range(0, size).boxed().toList();
            final Vector<Integer> source = Vector.ofAll(list);
            final Vector<Integer> built = Vector.<Integer> newBuilder().addAll(source).result();
            assertSharesFullLeaves(built, source, 0, size);
            final java.util.List<Object> sourceLeaves = leafIdentities(source);
            final java.util.List<Object> builtLeaves = leafIdentities(built);
            for (Vector<Integer> mutated : mutations(built, size)) {
                assertThat(mutated).isNotNull();
                assertThat(source.toJavaList()).as("source after mutating built").isEqualTo(list);
                assertThat(leafIdentities(source)).as("source leaves after mutating built").isEqualTo(sourceLeaves);
            }
            for (Vector<Integer> mutated : mutations(source, size)) {
                assertThat(mutated).isNotNull();
                assertThat(built.toJavaList()).as("built after mutating source").isEqualTo(list);
                assertThat(leafIdentities(built)).as("built leaves after mutating source").isEqualTo(builtLeaves);
            }
        }
    }

    private static java.util.List<Vector<Integer>> mutations(Vector<Integer> v, int size) {
        return java.util.List.of(
                v.update(0, -1), v.update(size - 1, -1), v.update(size / 2, -1),
                v.append(-1), v.prepend(-1), v.appendAll(Vector.range(0, 40)), v.prependAll(Vector.range(0, 40)),
                v.take(size - 1), v.take(1), v.drop(1), v.drop(size - 1), v.slice(1, size - 1),
                v.insert(0, -1), v.insert(size / 2, -1), v.insert(size, -1),
                v.removeAt(0), v.removeAt(size / 2), v.removeAt(size - 1),
                v.reverse(), v.sorted(java.util.Comparator.reverseOrder()));
    }

    private static java.util.List<Object> leafIdentities(Vector<?> v) {
        final java.util.List<Object> leaves = new ArrayList<>();
        for (int i = 0; i < v.size(); i += 32) {
            leaves.add(v.trie.getLeaf(i));
        }
        return leaves;
    }

    @Test
    public void shouldRouteTheRemainingShapesThroughTheBuilder() {
        final java.util.List<Integer> list32 = IntStream.range(0, 32).boxed().toList();
        final Vector<Integer> full32 = Vector.ofAll(list32);
        final Vector<Integer> range = Vector.range(0, 100);
        // flatMap whose mapper returns primitive-backed Vectors: the boxing branch of addLeafRange
        final Vector<Integer> boxed = range.flatMap(i -> Vector.range(0, 40));
        assertSameShape(boxed, Vector.ofAll(range.toJavaList().stream().flatMap(i -> IntStream.range(0, 40).boxed()).toList()), 4000);
        // flatMap whose mapper returns the same full Object[] Vector: its leaf is shared in every position
        final Vector<Integer> shared = range.flatMap(i -> full32);
        assertSameShape(shared, Vector.ofAll(java.util.Collections.nCopies(100, list32).stream().flatMap(java.util.List::stream).toList()), 3200);
        for (int i = 0; i < 3200; i += 32) {
            assertThat(shared.trie.getLeaf(i)).isSameAs(full32.trie.getLeaf(0));
        }
        // ofAll of a parallel stream relies on forEachOrdered
        final java.util.List<Integer> big = IntStream.range(0, 100_000).boxed().toList();
        assertSameShape(Vector.ofAll(big.parallelStream()), Vector.range(0, 100_000), 100_000);
        assertSameShape(Vector.ofAll(big.parallelStream().filter(i -> i % 2 == 0)), Vector.rangeBy(0, 100_000, 2), 50_000);
        // map / filter on receivers with an offset, so the first visited leaf starts after index 0
        for (Vector<Integer> receiver : java.util.List.of(Vector.ofAll(big).take(5000), Vector.range(0, 5000))) {
            for (int k : new int[] { 1, 5, 31, 32, 33, 1025 }) {
                final Vector<Integer> offset = receiver.prepend(-1).drop(k);
                final java.util.List<Integer> expected = receiver.prepend(-1).drop(k).toJavaList();
                assertSameShape(offset.map(i -> i * 2), Vector.ofAll(expected.stream().map(i -> i * 2).toList()), expected.size());
                assertSameShape(offset.filter(i -> i % 3 == 0), Vector.ofAll(expected.stream().filter(i -> i % 3 == 0).toList()), (int) expected.stream().filter(i -> i % 3 == 0).count());
                assertThat(offset.filter(i -> true)).isSameAs(offset);
            }
        }
        // a primitive-backed receiver keeps primitive leaves through filter
        assertThat(Vector.range(0, 5000).filter(i -> i % 2 == 0).trie.getLeaf(0)).isInstanceOf(int[].class);
        // a plain java.lang.Iterable (neither Collection nor Traversable) is a one-shot source for ofAll and appendAll
        final Iterable<Integer> plain = big::iterator;
        assertSameShape(Vector.ofAll(plain), Vector.range(0, 100_000), 100_000);
        assertSameShape(Vector.of(-1).appendAll(plain), Vector.range(-1, 100_000), 100_001);
        assertThat(Vector.<Integer> empty().appendAll(plain)).isEqualTo(Vector.range(0, 100_000));
        // tabulate and fill at the boundaries, including n <= 0
        for (int n : new int[] { -1, 0, 1, 32, 33, 1024, 1025 }) {
            assertSameShape(Vector.tabulate(n, i -> i), Vector.range(0, Math.max(n, 0)), Math.max(n, 0));
            assertSameShape(Vector.fill(n, () -> 7), Vector.ofAll(java.util.Collections.nCopies(Math.max(n, 0), 7)), Math.max(n, 0));
            assertSameShape(Vector.fill(n, 7), Vector.ofAll(java.util.Collections.nCopies(Math.max(n, 0), 7)), Math.max(n, 0));
        }
        // ofAll of a sized Traversable that is not a Vector
        assertSameShape(Vector.ofAll(Array.ofAll(big)), Vector.range(0, 100_000), 100_000);
        assertSameShape(Vector.ofAll(List.ofAll(list32)), full32, 32);
        assertSameShape(Vector.ofAll(HashSet.of(1)), Vector.of(1), 1);
    }

    @Test
    public void shouldAddAllObjectBackedVectorsIntoAHintedLeafShorterThan32() {
        // a hinted leaf shorter than 32 is grown, not closed, inside addLeafRange: the copy loop has to split the source
        // leaf across the grown leaf and a fresh one, and nothing can be shared until the leaves realign
        for (int hint : new int[] { 1, 5, 31 }) {
            for (int size : new int[] { 1, 31, 32, 33, 64, 65, 1023, 1024, 1025 }) {
                final Vector<Integer> source = Vector.ofAll(IntStream.range(0, size).boxed().toList());
                for (int prefix : new int[] { 0, 1, hint, hint + 1 }) {
                    final Vector.Builder<Integer> builder = Vector.newBuilder(hint);
                    for (int i = 0; i < prefix; i++) {
                        builder.add(-1 - i);
                    }
                    final Vector<Integer> expected = Vector.tabulate(prefix, i -> -1 - i).appendAll(source);
                    final Vector<Integer> built = builder.addAll(source).addAll(source).result();
                    assertSameShape(built, expected.appendAll(source), prefix + 2 * size);
                    if (prefix == 0) {
                        assertSharesFullLeaves(built, source, 0, size);
                    } else if (size >= 32) {
                        // shared only when the prefix realigns the leaves (32 adds into a hinted leaf grown to 32)
                        if (prefix % 32 == 0) {
                            assertThat(built.trie.getLeaf(prefix)).isSameAs(source.trie.getLeaf(0));
                        } else {
                            assertThat(built.trie.getLeaf(prefix)).isNotSameAs(source.trie.getLeaf(0));
                        }
                    }
                }
            }
        }
    }

    @Test
    public void shouldSurviveJavaSerialisation() {
        final java.util.List<Integer> list = IntStream.range(0, 1056).boxed().toList();
        final Vector<Integer> source = Vector.ofAll(list);
        // shared leaves at two levels, a handed-over single leaf, a hinted leaf, and the empty result
        final java.util.List<Vector<Integer>> built = java.util.List.of(
                Vector.<Integer> newBuilder().addAll(source).result(),
                Vector.<Integer> newBuilder().add(-1).addAll(source).result(),
                Vector.<Integer> newBuilder().addAll(source.take(32)).result(),
                Vector.<Integer> newBuilder().addAll(source.take(7)).result(),
                Vector.<Integer> newBuilder(7).addAll(source.take(7)).result(),
                Vector.<Integer> newBuilder().result());
        for (Vector<Integer> vector : built) {
            final Vector<Integer> copy = io.vavr.Serializables.deserialize(io.vavr.Serializables.serialize(vector));
            assertSameShape(copy, vector, vector.size());
            assertThat(copy.hashCode()).isEqualTo(vector.hashCode());
            if (vector.isEmpty()) {
                assertThat(copy).isSameAs(Vector.empty());
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
        // the state check comes before the argument check
        assertThatThrownBy(() -> builder.addAll(null)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void shouldRejectNullIterableWhileOpen() {
        assertThatThrownBy(() -> Vector.<Integer> newBuilder().addAll(null)).isInstanceOf(NullPointerException.class);
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
        final Vector<Integer> boxed3000 = Vector.ofAll(IntStream.range(0, 3000).boxed().toList());
        for (int round = 0; round < 50; round++) {
            final ArrayList<Integer> expected = new ArrayList<>();
            final Vector.Builder<Integer> builder = Vector.newBuilder(random.nextInt(50));
            final int steps = 1 + random.nextInt(200);
            for (int step = 0; step < steps; step++) {
                switch (random.nextInt(5)) {
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
                    case 3 -> {
                        // an Object[]-backed sliced source: shared leaves once aligned, copied ones otherwise
                        final int from = random.nextInt(2000), count = random.nextInt(1100);
                        final Vector<Integer> vector = boxed3000.slice(from, Math.min(3000, from + count));
                        expected.addAll(vector.toJavaList());
                        builder.addAll(vector);
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

    /* every leaf of [from, to) that is full in the source (32 elements from an aligned index) is the same array in the built Vector; a trailing partial leaf is a copy */
    private static <T> void assertSharesFullLeaves(Vector<T> built, Vector<T> source, int from, int to) {
        for (int i = from; i < to; i += 32) {
            final Object sourceLeaf = source.trie.getLeaf(i);
            if (i + 32 <= to && ((Object[]) sourceLeaf).length == 32) {
                assertThat(built.trie.getLeaf(i)).as("leaf at %d is shared", i).isSameAs(sourceLeaf);
            } else {
                assertThat(built.trie.getLeaf(i)).as("partial leaf at %d is copied", i).isNotSameAs(sourceLeaf);
            }
        }
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
