package com.guizmaii.zazr.collection;

import com.guizmaii.zazr.collection.internal.Iterator;
import java.util.ArrayList;
import java.util.Random;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.guizmaii.zazr.collection.internal.RadixVectorShapes.depth;
import static com.guizmaii.zazr.collection.internal.RadixVectorShapes.indexInLeaf;
import static com.guizmaii.zazr.collection.internal.RadixVectorShapes.leafAt;
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
            final Vector<Integer> boxed = Vector.ofAll(list);                 // via toArray
            final Vector<Integer> varargs = Vector.of(list.toArray(new Integer[0])); // via of(T...)
            for (Vector<Integer> source : java.util.List.of(boxed, varargs)) {
                final Vector<Integer> built = Vector.<Integer> newBuilder().addAll(source).result();
                assertSameShape(built, boxed, size);
                assertSharesFullLeaves(built, source);
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
        assertThat(depth(built32.trie)).isEqualTo(1);
        assertThat(leafAt(built32.trie, 0)).hasSize(32);
        final Vector<Integer> v1024 = Vector.ofAll(IntStream.range(0, 1024).boxed().toList());
        final Vector<Integer> built1024 = Vector.<Integer> newBuilder().addAll(v1024).result();
        assertThat(depth(built1024.trie)).isEqualTo(2);
        assertSameShape(built1024, v1024, 1024);
    }

    @Test
    public void shouldAddAllSlicedVectorsByReusingTheirArrays() {
        // a slice's outer leaves are trimmed copies, so every leaf of a slice holds only live elements: a builder that
        // starts from the slice keeps its arrays, at the same positions, the partial first leaf of a slice of two levels
        // or more included (a slice of one leaf is copied into the builder's leaf, which is filled in place)
        final Vector<Integer> source = Vector.ofAll(IntStream.range(0, 4200).boxed().toList());
        for (int k : new int[] { 1, 5, 31, 32, 33 }) {
            for (int n : new int[] { 0, 1, 31, 32, 33, 63, 64, 65, 1023, 1024, 1025, 2047, 2048, 2049 }) {
                final Vector<Integer> slice = source.drop(k).take(n);
                final Vector<Integer> expected = Vector.range(k, k + n);
                final Vector<Integer> built = Vector.<Integer> newBuilder().addAll(slice).result();
                assertSameElements(built, expected, n);
                assertThat(depth(built.trie)).isEqualTo(depth(slice.trie));
                assertSharesFullLeaves(built, slice);
                if (depth(slice.trie) > 1) {
                    assertThat(leafAt(built.trie, 0)).isSameAs(leafAt(slice.trie, 0));
                }
                // a builder started from a slice counts the free slots in front of the slice's first leaf as positions, so
                // its result may be one level deeper than the minimal depth: only the elements are compared
                assertSameElements(Vector.<Integer> newBuilder().addAll(slice).add(k + n).result(), Vector.range(k, k + n + 1), n + 1);
                assertSameShape(Vector.<Integer> newBuilder().add(k - 1).addAll(slice).result(), Vector.range(k - 1, k + n), n + 1);
                assertSameElements(Vector.<Integer> newBuilder().addAll(slice).addAll(slice).result(), expected.appendAll(expected), 2 * n);
            }
        }
    }

    @Test
    public void shouldShareTheInnerLeavesOfTheSourceWhenTheCurrentLeafIsExactlyFull() {
        // 32 adds leave the current leaf full; the following addAll shares the source's inner leaves (its slices of
        // dimension 2 and more) and copies its outer leaves, prefix1 and suffix1, which are single arrays
        final Vector<Integer> v2048 = Vector.ofAll(IntStream.range(0, 2048).boxed().toList());
        final Vector.Builder<Integer> builder = Vector.newBuilder();
        for (int i = 0; i < 32; i++) {
            builder.add(-i);
        }
        final Vector<Integer> built = builder.addAll(v2048).result();
        assertSameElements(built, Vector.tabulate(32, i -> -i).appendAll(v2048), 2080);
        assertSharesInnerLeaves(built, 32, v2048);
        // the same through a sized builder
        final Vector.Builder<Integer> hinted = Vector.newBuilder(32);
        for (int i = 0; i < 32; i++) {
            hinted.add(i);
        }
        assertSharesInnerLeaves(hinted.addAll(v2048).result(), 32, v2048);
        // and a collector combiner whose left side ends on a full leaf
        final Vector<Integer> combined = Vector.<Integer> newBuilder().addAll(Vector.tabulate(32, i -> i)).addAll(v2048).result();
        assertSharesInnerLeaves(combined, 32, v2048);
        // a partially filled current leaf copies every leaf
        final Vector<Integer> unaligned = Vector.<Integer> newBuilder().add(0).addAll(v2048).result();
        assertSameElements(unaligned, Vector.of(0).appendAll(v2048), 2049);
        for (int i = 0; i < 2048; i += 32) {
            assertThat(leafAt(unaligned.trie, 1 + i)).isNotSameAs(leafAt(v2048.trie, i));
        }
    }

    /* every leaf of source strictly between its first and last leaves is the same array in built, from position offset */
    private static <T> void assertSharesInnerLeaves(Vector<T> built, int offset, Vector<T> source) {
        for (int i = 32; i < source.size() - 32; i += 32) {
            assertThat(leafAt(built.trie, offset + i)).as("leaf at %d is shared", i).isSameAs(leafAt(source.trie, i));
        }
    }

    @Test
    public void shouldNeverWriteIntoSharedLeavesFromEitherSide() {
        for (int size : new int[] { 32, 64, 1024, 1056 }) {
            final java.util.List<Integer> list = IntStream.range(0, size).boxed().toList();
            final Vector<Integer> source = Vector.ofAll(list);
            final Vector<Integer> built = Vector.<Integer> newBuilder().addAll(source).result();
            assertSharesFullLeaves(built, source);
            final java.util.List<Object> sourceLeaves = leafIdentities(source);
            final java.util.List<Object> builtLeaves = leafIdentities(built);
            for (Vector<Integer> mutated : mutations(built, size)) {
                assertThat(mutated).isNotNull();
                assertThat(new java.util.ArrayList<>(source.asJava())).as("source after mutating built").isEqualTo(list);
                assertThat(leafIdentities(source)).as("source leaves after mutating built").isEqualTo(sourceLeaves);
            }
            for (Vector<Integer> mutated : mutations(source, size)) {
                assertThat(mutated).isNotNull();
                assertThat(new java.util.ArrayList<>(built.asJava())).as("built after mutating source").isEqualTo(list);
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
            leaves.add(leafAt(v.trie, i));
        }
        return leaves;
    }

    @Test
    public void shouldRouteTheRemainingShapesThroughTheBuilder() {
        final java.util.List<Integer> list32 = IntStream.range(0, 32).boxed().toList();
        final Vector<Integer> full32 = Vector.ofAll(list32);
        final Vector<Integer> range = Vector.range(0, 100);
        // flatMap whose mapper returns Vectors of two leaves
        final Vector<Integer> boxed = range.flatMap(i -> Vector.range(0, 40));
        assertSameShape(boxed, Vector.ofAll(new java.util.ArrayList<>(range.asJava()).stream().flatMap(i -> IntStream.range(0, 40).boxed()).toList()), 4000);
        // flatMap whose mapper returns the same one-leaf Vector: the first is the builder's start, the others are copied
        final Vector<Integer> shared = range.flatMap(i -> full32);
        assertSameShape(shared, Vector.ofAll(java.util.Collections.nCopies(100, list32).stream().flatMap(java.util.List::stream).toList()), 3200);
        assertThat(leafAt(shared.trie, 0)).isSameAs(leafAt(full32.trie, 0));
        // ofAll of a parallel stream relies on forEachOrdered
        final java.util.List<Integer> big = IntStream.range(0, 100_000).boxed().toList();
        assertSameShape(Vector.ofAll(big.parallelStream()), Vector.range(0, 100_000), 100_000);
        assertSameShape(Vector.ofAll(big.parallelStream().filter(i -> i % 2 == 0)), Vector.rangeBy(0, 100_000, 2), 50_000);
        // map / filter on receivers with an offset, so the first visited leaf starts after index 0
        for (Vector<Integer> receiver : java.util.List.of(Vector.ofAll(big).take(5000), Vector.range(0, 5000))) {
            for (int k : new int[] { 1, 5, 31, 32, 33, 1025 }) {
                final Vector<Integer> offset = receiver.prepend(-1).drop(k);
                final java.util.List<Integer> expected = new java.util.ArrayList<>(receiver.prepend(-1).drop(k).asJava());
                assertSameShape(offset.map(i -> i * 2), Vector.ofAll(expected.stream().map(i -> i * 2).toList()), expected.size());
                assertSameShape(offset.filter(i -> i % 3 == 0), Vector.ofAll(expected.stream().filter(i -> i % 3 == 0).toList()), (int) expected.stream().filter(i -> i % 3 == 0).count());
                assertThat(offset.filter(i -> true)).isSameAs(offset);
            }
        }
        // filter starts from the kept prefix: its arrays are shared
        final Vector<Integer> big5000 = Vector.ofAll(big).take(5000);
        final Vector<Integer> filtered = big5000.filter(i -> i < 4000 || i % 2 == 0);
        assertSameElements(filtered, Vector.range(0, 4000).appendAll(Vector.rangeBy(4000, 5000, 2)), 4500);
        assertSharesInnerLeaves(filtered, 0, big5000.take(4000));
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
        assertSameShape(Vector.ofAll(Queue.ofAll(big)), Vector.range(0, 100_000), 100_000);
        assertSameShape(Vector.ofAll(List.ofAll(list32)), full32, 32);
        assertSameShape(Vector.ofAll(HashSet.of(1)), Vector.of(1), 1);
    }

    @Test
    public void shouldShareInnerLeavesOnlyWhenTheBuilderIsAligned() {
        // after a prefix that ends inside a leaf, every source leaf is split across two leaves of the builder: nothing
        // can be shared; after a prefix of whole leaves, the source's inner leaves are
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
                        assertSharesFullLeaves(built, source);
                    } else if (size >= 65) {
                        if (prefix % 32 == 0) {
                            assertSharesInnerLeaves(built, prefix, source);
                        } else {
                            assertThat(leafAt(built.trie, prefix + 32)).isNotSameAs(leafAt(source.trie, 32));
                        }
                    }
                }
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
    public void shouldRejectNullElements() {
        assertThatThrownBy(() -> Vector.<Integer> newBuilder().add(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    public void shouldKeepElementsAddedBeforeANullOnRejectedAddAll() {
        final java.util.List<Integer> withNullElement = new java.util.ArrayList<>();
        withNullElement.add(1);
        withNullElement.add(2);
        withNullElement.add(null);
        withNullElement.add(3);
        final Vector.Builder<Integer> builder = Vector.newBuilder();
        assertThatThrownBy(() -> builder.addAll(withNullElement)).isInstanceOf(NullPointerException.class);
        assertThat(builder.result()).isEqualTo(Vector.of(1, 2));
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
        assertThatThrownBy(() -> builder.add(null)).isInstanceOf(IllegalStateException.class);
        // the bulk loops too, even for zero elements, where no leaf boundary would be crossed
        assertThatThrownBy(() -> builder.addTabulated(0, i -> i)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> builder.addRepeated(0, 1)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> builder.addTabulated(5, i -> i)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void shouldRunTheBulkLoopsAcrossLeafBoundaries() {
        // tabulate and fill start from an empty builder; here the bulk loops also run after other elements
        for (int n : new int[] { 6, 31, 32, 33, 64, 65, 1025 }) {
            final Vector.Builder<Integer> tabulated = Vector.newBuilder(5);
            tabulated.addTabulated(n, i -> i);
            assertSameShape(tabulated.result(), Vector.range(0, n), n);

            final Vector.Builder<Integer> repeated = Vector.newBuilder(5);
            repeated.addRepeated(n, 7);
            assertSameShape(repeated.result(), Vector.fill(n, 7), n);

            // and after a partial prefix, so a leaf is split between prefix and bulk elements
            final Vector.Builder<Integer> prefixed = Vector.newBuilder(5);
            prefixed.add(-1).add(-2);
            prefixed.addTabulated(n, i -> i);
            assertSameShape(prefixed.result(), Vector.of(-1, -2).appendAll(Vector.range(0, n)), n + 2);

            final Vector.Builder<Integer> prefixedRepeat = Vector.newBuilder();
            prefixedRepeat.add(-1).add(-2);
            prefixedRepeat.addRepeated(n, 7);
            assertSameShape(prefixedRepeat.result(), Vector.of(-1, -2).appendAll(Vector.fill(n, 7)), n + 2);
        }
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
        assertThat(List.ofAll(built).size()).isEqualTo(5001);
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
                        expected.addAll(new java.util.ArrayList<>(vector.asJava()));
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
                        expected.addAll(new java.util.ArrayList<>(vector.asJava()));
                        builder.addAll(vector);
                    }
                    default -> {
                        final Vector<Integer> vector = Vector.ofAll(IntStream.range(0, random.nextInt(100)).toArray());
                        expected.addAll(new java.util.ArrayList<>(vector.asJava()));
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

    /* every full leaf of the source (32 elements) is the same array, at the same position, in the built Vector */
    private static <T> void assertSharesFullLeaves(Vector<T> built, Vector<T> source) {
        int shared = 0;
        for (int i = 0; i < source.size(); ) {
            final Object[] sourceLeaf = leafAt(source.trie, i);
            final int start = i - indexInLeaf(source.trie, i);
            if (sourceLeaf.length == 32) {
                assertThat(leafAt(built.trie, start)).as("leaf at %d is shared", start).isSameAs(sourceLeaf);
                assertThat(indexInLeaf(built.trie, start)).isZero();
                shared++;
            }
            i = start + sourceLeaf.length;
        }
        assertThat(shared).isGreaterThanOrEqualTo((source.size() - 62) / 32);
    }

    private static <T> void assertSameShape(Vector<T> actual, Vector<T> expected, int size) {
        assertSameElements(actual, expected, size);
        assertThat(depth(actual.trie)).as("depth for size %d", size).isEqualTo(depth(expected.trie));
        assertThat(depth(actual.trie)).isEqualTo(depth(Vector.range(0, size).trie));
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
