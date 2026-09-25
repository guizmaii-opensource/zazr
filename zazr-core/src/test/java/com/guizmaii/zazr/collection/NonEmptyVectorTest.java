package com.guizmaii.zazr.collection;

import com.guizmaii.zazr.Tuple;
import com.guizmaii.zazr.Tuple2;
import com.guizmaii.zazr.control.Either;
import com.guizmaii.zazr.control.Option;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Every method is checked at sizes 1, 2, 32, 33, 1023, 1024 and 1025 (the leaf and second-level boundaries) against the equivalent {@link Vector} call, on both
 * representations a Vector can have (primitive {@code int[]} leaves from {@code Vector.range}, {@code Object[]} leaves from
 * {@code Vector.ofAll}).
 */
public class NonEmptyVectorTest {

    static final int[] SIZES = { 1, 2, 32, 33, 1023, 1024, 1025 };

    /* (size, vector) for both leaf representations */
    static Stream<Arguments> vectors() {
        final ArrayList<Arguments> cases = new ArrayList<>();
        for (int n : SIZES) {
            final Vector<Integer> primitive = Vector.range(0, n);
            cases.add(Arguments.of(n, primitive));
            cases.add(Arguments.of(n, Vector.ofAll(new java.util.ArrayList<>(primitive.asJava()))));
        }
        return cases.stream();
    }

    static <A> NonEmptyVector<A> nev(Vector<A> vector) {
        return NonEmptyVector.unsafeFromVector(vector);
    }

    /* the wrapper is built without a check on the internal paths, so a result is checked to hold at least one element */
    static <A> NonEmptyVector<A> nonEmpty(NonEmptyVector<A> result) {
        assertThat(result.toVector().isEmpty()).isFalse();
        assertThat(result.size()).isPositive();
        return result;
    }

    /* a one-shot iterable: a second iterator() throws, so an operation that reads its argument twice fails */
    static <A> Iterable<A> once(Vector<A> elements) {
        final boolean[] read = { false };
        return () -> {
            if (read[0]) {
                throw new IllegalStateException("read twice");
            }
            read[0] = true;
            return elements.iterator();
        };
    }

    @Nested
    class Constructors {

        @Test
        public void shouldBuildOfHeadAndVarargsTail() {
            assertThat(NonEmptyVector.of(1).toVector()).isEqualTo(Vector.of(1));
            assertThat(NonEmptyVector.of(1, 2).toVector()).isEqualTo(Vector.of(1, 2));
            for (int n : SIZES) {
                final Integer[] tail = Vector.range(1, n).toArray(Integer[]::new);
                assertThat(NonEmptyVector.of(0, tail).toVector()).isEqualTo(Vector.range(0, n));
                assertThat(NonEmptyVector.of(0, tail).size()).isEqualTo(n);
            }
        }

        @Test
        public void shouldBuildFromHeadAndIterableTail() {
            for (int n : SIZES) {
                final Vector<Integer> expected = Vector.range(0, n);
                assertThat(NonEmptyVector.fromIterable(0, Vector.range(1, n)).toVector()).isEqualTo(expected);
                assertThat(NonEmptyVector.fromIterable(0, new java.util.ArrayList<>(Vector.range(1, n).asJava())).toVector()).isEqualTo(expected);
                assertThat(NonEmptyVector.fromIterable(0, (Iterable<Integer>) () -> Vector.range(1, n).iterator()).toVector()).isEqualTo(expected);
                assertThat(NonEmptyVector.fromIterable(0, List.<Integer> empty()).toVector()).isEqualTo(Vector.of(0));
            }
        }

        @Test
        public void shouldTakeIterablesAsElementsNotAsTail() {
            final NonEmptyVector<Integer> x = NonEmptyVector.of(1);
            final NonEmptyVector<Integer> y = NonEmptyVector.of(2, 3);
            assertThat(NonEmptyVector.of(x, y).size()).isEqualTo(2);
            assertThat(NonEmptyVector.of(x, y).toVector()).isEqualTo(Vector.of(x, y));
            assertThat(NonEmptyVector.of(java.util.List.of(1), java.util.List.of(2, 3)).size()).isEqualTo(2);
            assertThat(NonEmptyVector.fromIterable(x, Vector.of(y)).size()).isEqualTo(2);
        }

        @Test
        public void shouldBuildSingle() {
            assertThat(NonEmptyVector.single("a").toVector()).isEqualTo(Vector.of("a"));
            assertThat(NonEmptyVector.single("a").size()).isEqualTo(1);
        }

        @Test
        public void shouldWrapANonEmptyVectorWithoutCopying() {
            for (int n : SIZES) {
                final Vector<Integer> vector = Vector.range(0, n);
                assertThat(NonEmptyVector.fromVector(vector).get().toVector()).isSameAs(vector);
                assertThat(NonEmptyVector.unsafeFromVector(vector).toVector()).isSameAs(vector);
                assertThat(vector.toNonEmptyVector().get().toVector()).isSameAs(vector);
                assertThat(NonEmptyVector.fromIterable(vector).get().toVector()).isSameAs(vector);
            }
        }

        @Test
        public void shouldReturnNoneFromEmptyVector() {
            assertThat(NonEmptyVector.fromVector(Vector.<Integer> empty())).isEqualTo(Option.none());
            assertThat(Vector.<Integer> empty().toNonEmptyVector()).isEqualTo(Option.none());
            assertThat(NonEmptyVector.fromIterable(Vector.<Integer> empty())).isEqualTo(Option.none());
            assertThat(NonEmptyVector.fromIterable(java.util.List.<Integer> of())).isEqualTo(Option.none());
            assertThat(NonEmptyVector.fromIterable(List.<Integer> empty())).isEqualTo(Option.none());
        }

        @Test
        public void shouldCopyANonEmptyIterable() {
            for (int n : SIZES) {
                final Vector<Integer> expected = Vector.range(0, n);
                assertThat(NonEmptyVector.fromIterable(new java.util.ArrayList<>(expected.asJava())).get().toVector()).isEqualTo(expected);
                assertThat(NonEmptyVector.fromIterable((Iterable<Integer>) expected::iterator).get().toVector()).isEqualTo(expected);
                assertThat(NonEmptyVector.fromIterable(expected.toList()).get().toVector()).isEqualTo(expected);
            }
        }

        @Test
        public void shouldThrowOnUnsafeFromEmptyVector() {
            assertThatIllegalArgumentException()
              .isThrownBy(() -> NonEmptyVector.unsafeFromVector(Vector.empty()))
              .withMessage("NonEmptyVector.unsafeFromVector: vector is empty");
        }

        @Test
        public void shouldRejectNullsNamingTheType() {
            assertThatNullPointerException().isThrownBy(() -> NonEmptyVector.of(null)).withMessage("NonEmptyVector: head is null");
            assertThatNullPointerException().isThrownBy(() -> NonEmptyVector.of(1, (Integer[]) null)).withMessage("NonEmptyVector: tail is null");
            assertThatNullPointerException().isThrownBy(() -> NonEmptyVector.of(1, 2, null)).withMessage("NonEmptyVector: element is null");
            assertThatNullPointerException().isThrownBy(() -> NonEmptyVector.fromIterable(null, Vector.of(1))).withMessage("NonEmptyVector: head is null");
            assertThatNullPointerException().isThrownBy(() -> NonEmptyVector.fromIterable(1, (Iterable<Integer>) null)).withMessage("NonEmptyVector: tail is null");
            assertThatNullPointerException().isThrownBy(() -> NonEmptyVector.fromIterable(1, Arrays.asList(2, null))).withMessage("NonEmptyVector: element is null");
            assertThatNullPointerException().isThrownBy(() -> NonEmptyVector.single(null)).withMessage("NonEmptyVector: element is null");
            assertThatNullPointerException().isThrownBy(() -> NonEmptyVector.fromVector(null)).withMessage("NonEmptyVector.fromVector: vector is null");
            assertThatNullPointerException().isThrownBy(() -> NonEmptyVector.fromIterable(null)).withMessage("NonEmptyVector.fromIterable: iterable is null");
            assertThatNullPointerException().isThrownBy(() -> NonEmptyVector.fromIterable(Arrays.asList(1, null))).withMessage("NonEmptyVector: element is null");
            assertThatNullPointerException().isThrownBy(() -> NonEmptyVector.unsafeFromVector(null)).withMessage("NonEmptyVector.unsafeFromVector: vector is null");
            assertThatNullPointerException().isThrownBy(() -> NonEmptyVector.flatten(null)).withMessage("nested is null");
        }

        @Test
        public void shouldRejectNullsOnInsertionNamingTheType() {
            final NonEmptyVector<Integer> nev = NonEmptyVector.of(1, 2);
            assertThatNullPointerException().isThrownBy(() -> nev.append(null)).withMessage("NonEmptyVector.append: element is null");
            assertThatNullPointerException().isThrownBy(() -> nev.prepend(null)).withMessage("NonEmptyVector.prepend: element is null");
            assertThatNullPointerException().isThrownBy(() -> nev.update(0, (Integer) null)).withMessage("NonEmptyVector.update: element is null");
            assertThatNullPointerException().isThrownBy(() -> nev.update(0, i -> null));
            assertThatNullPointerException().isThrownBy(() -> nev.map(i -> null));
            assertThatNullPointerException().isThrownBy(() -> nev.flatMap(i -> null)).withMessage("NonEmptyVector.flatMap: mapper returned null");
            assertThatNullPointerException().isThrownBy(() -> nev.appendAll((Vector<Integer>) null));
            assertThatNullPointerException().isThrownBy(() -> nev.appendAll((NonEmptyVector<Integer>) null));
            assertThatNullPointerException().isThrownBy(() -> nev.prependAll((Vector<Integer>) null));
            assertThatNullPointerException().isThrownBy(() -> nev.prependAll((NonEmptyVector<Integer>) null));
            assertThatNullPointerException().isThrownBy(() -> nev.concat(null));
            assertThatNullPointerException().isThrownBy(() -> nev.zip(null));
            assertThatNullPointerException().isThrownBy(() -> nev.zipWith(null, (a, b) -> a));
            assertThatNullPointerException().isThrownBy(() -> nev.max(null));
            assertThatNullPointerException().isThrownBy(() -> nev.min(null));
            assertThatNullPointerException().isThrownBy(() -> nev.maxBy((Function<Integer, Integer>) null));
            assertThatNullPointerException().isThrownBy(() -> nev.maxBy((Comparator<Integer>) null));
            assertThatNullPointerException().isThrownBy(() -> nev.minBy((Function<Integer, Integer>) null));
            assertThatNullPointerException().isThrownBy(() -> nev.minBy((Comparator<Integer>) null));
            assertThatNullPointerException().isThrownBy(() -> nev.reduceMap(null, (a, b) -> a));
            assertThatNullPointerException().isThrownBy(() -> nev.reduceMap(i -> i, null));
        }
    }

    @Nested
    class ReturnsNonEmptyVector {

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyVectorTest#vectors")
        public void shouldMap(int n, Vector<Integer> vector) {
            final NonEmptyVector<String> actual = nev(vector).map(i -> "x" + i);
            assertThat(actual.toVector()).isEqualTo(vector.map(i -> "x" + i));
            assertThat(actual.size()).isEqualTo(n);
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyVectorTest#vectors")
        public void shouldFlatMapToNonEmptyVectors(int n, Vector<Integer> vector) {
            final NonEmptyVector<Integer> actual = nev(vector).flatMap(i -> NonEmptyVector.of(i, -i));
            assertThat(actual.toVector()).isEqualTo(vector.flatMap(i -> Vector.of(i, -i)));
            assertThat(actual.size()).isEqualTo(2 * n);
            // a mapper returning 33-element vectors crosses the leaf boundary inside the builder
            assertThat(nev(vector).flatMap(i -> nev(Vector.range(i, i + 33))).toVector()).isEqualTo(vector.flatMap(i -> Vector.range(i, i + 33)));
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyVectorTest#vectors")
        public void shouldAppendAndPrepend(int n, Vector<Integer> vector) {
            final NonEmptyVector<Integer> nev = nev(vector);
            assertThat(nev.append(-1).toVector()).isEqualTo(vector.append(-1));
            assertThat(nev.append(-1).size()).isEqualTo(n + 1);
            assertThat(nev.prepend(-1).toVector()).isEqualTo(vector.prepend(-1));
            assertThat(nev.prepend(-1).size()).isEqualTo(n + 1);
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyVectorTest#vectors")
        public void shouldAppendAllAndPrependAllAVector(int n, Vector<Integer> vector) {
            final NonEmptyVector<Integer> nev = nev(vector);
            for (int m : new int[] { 0, 1, 32, 33 }) {
                final Vector<Integer> that = Vector.range(100, 100 + m);
                final NonEmptyVector<Integer> appended = nev.appendAll(that);
                assertThat(appended.toVector()).isEqualTo(vector.appendAll(that));
                assertThat(appended.size()).isEqualTo(n + m);
                final NonEmptyVector<Integer> prepended = nev.prependAll(that);
                assertThat(prepended.toVector()).isEqualTo(vector.prependAll(that));
                assertThat(prepended.size()).isEqualTo(n + m);
            }
            assertThat(nev.appendAll(Vector.<Integer> empty()).toVector()).isSameAs(vector);
            assertThat(nev.prependAll(Vector.<Integer> empty()).toVector()).isSameAs(vector);
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyVectorTest#vectors")
        public void shouldAppendAllPrependAllAndConcatANonEmptyVector(int n, Vector<Integer> vector) {
            final NonEmptyVector<Integer> nev = nev(vector);
            for (int m : SIZES) {
                final Vector<Integer> that = Vector.range(100, 100 + m);
                assertThat(nev.appendAll(nev(that)).toVector()).isEqualTo(vector.appendAll(that));
                assertThat(nev.concat(nev(that)).toVector()).isEqualTo(vector.appendAll(that));
                assertThat(nev.concat(nev(that)).size()).isEqualTo(n + m);
                assertThat(nev.prependAll(nev(that)).toVector()).isEqualTo(vector.prependAll(that));
            }
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyVectorTest#vectors")
        public void shouldReverse(int n, Vector<Integer> vector) {
            final NonEmptyVector<Integer> actual = nev(vector).reverse();
            assertThat(actual.toVector()).isEqualTo(vector.reverse());
            assertThat(actual.head()).isEqualTo(n - 1);
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyVectorTest#vectors")
        public void shouldDistinct(int n, Vector<Integer> vector) {
            final Vector<Integer> doubled = vector.appendAll(vector);
            assertThat(nev(doubled).distinct().toVector()).isEqualTo(doubled.distinct());
            assertThat(nev(doubled).distinct().size()).isEqualTo(n);
            assertThat(nev(doubled).distinctBy(i -> i % 5).toVector()).isEqualTo(doubled.distinctBy(i -> i % 5));
            assertThat(nev(doubled).distinctBy(Comparator.comparingInt(i -> i % 5)).toVector()).isEqualTo(doubled.distinctBy(Comparator.comparingInt(i -> i % 5)));
            assertThat(nev(doubled).distinctBy(i -> i % 5).size()).isEqualTo(Math.min(n, 5));
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyVectorTest#vectors")
        public void shouldSort(int n, Vector<Integer> vector) {
            final Vector<Integer> shuffled = vector.reverse();
            assertThat(nev(shuffled).sorted().toVector()).isEqualTo(vector);
            assertThat(nev(vector).sorted(Comparator.reverseOrder()).toVector()).isEqualTo(shuffled);
            assertThat(nev(shuffled).sortBy(i -> i % 7).toVector()).isEqualTo(shuffled.sortBy(i -> i % 7));
            assertThat(nev(shuffled).sortBy(Comparator.reverseOrder(), i -> i % 7).toVector()).isEqualTo(shuffled.sortBy(Comparator.reverseOrder(), i -> i % 7));
            assertThat(nev(shuffled).sorted().size()).isEqualTo(n);
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyVectorTest#vectors")
        public void shouldZip(int n, Vector<Integer> vector) {
            final NonEmptyVector<Integer> nev = nev(vector);
            for (int m : SIZES) {
                final Vector<String> that = Vector.range(0, m).map(String::valueOf);
                final NonEmptyVector<Tuple2<Integer, String>> zipped = nev.zip(nev(that));
                assertThat(zipped.toVector()).isEqualTo(vector.zip(that));
                assertThat(zipped.size()).isEqualTo(Math.min(n, m));
                final NonEmptyVector<String> zippedWith = nev.zipWith(nev(that), (i, s) -> i + s);
                assertThat(zippedWith.toVector()).isEqualTo(vector.zipWith(that, (i, s) -> i + s));
            }
            assertThat(nev.zipWithIndex().toVector()).isEqualTo(vector.zipWithIndex());
            assertThat(nev.zipWithIndex().last()).isEqualTo(Tuple.of(n - 1, n - 1));
            assertThat(nev.zipWithIndex((i, index) -> i * index).toVector()).isEqualTo(vector.zipWithIndex((i, index) -> i * index));
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyVectorTest#vectors")
        public void shouldScanLeftWithOneMoreElement(int n, Vector<Integer> vector) {
            final NonEmptyVector<Integer> actual = nev(vector).scanLeft(0, Integer::sum);
            assertThat(actual.toVector()).isEqualTo(vector.scanLeft(0, Integer::sum));
            assertThat(actual.size()).isEqualTo(n + 1);
            assertThat(actual.head()).isEqualTo(0);
            assertThat(actual.last()).isEqualTo(n * (n - 1) / 2);
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyVectorTest#vectors")
        public void shouldUpdate(int n, Vector<Integer> vector) {
            final NonEmptyVector<Integer> nev = nev(vector);
            for (int index : new int[] { 0, n - 1 }) {
                assertThat(nev.update(index, -1).toVector()).isEqualTo(vector.update(index, -1));
                assertThat(nev.update(index, i -> i - 100).toVector()).isEqualTo(vector.update(index, i -> i - 100));
                assertThat(nev.update(index, -1).size()).isEqualTo(n);
            }
            assertThatThrownBy(() -> nev.update(n, -1)).isInstanceOf(IndexOutOfBoundsException.class);
            assertThatThrownBy(() -> nev.update(-1, i -> i)).isInstanceOf(IndexOutOfBoundsException.class);
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyVectorTest#vectors")
        public void shouldTapEveryElementAndReturnItself(int n, Vector<Integer> vector) {
            final NonEmptyVector<Integer> nev = nev(vector);
            final ArrayList<Integer> seen = new ArrayList<>();
            assertThat(nev.tap(seen::add)).isSameAs(nev);
            assertThat(seen).isEqualTo(new java.util.ArrayList<>(vector.asJava()));
            assertThatNullPointerException().isThrownBy(() -> nev.tap(null));
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyVectorTest#vectors")
        public void shouldGroupIntoNonEmptyGroups(int n, Vector<Integer> vector) {
            final NonEmptyVector<Integer> nev = nev(vector);
            for (int size : new int[] { 1, 2, 5, 32, 33, 100 }) {
                final Vector<NonEmptyVector<Integer>> groups = nev.grouped(size);
                assertThat(groups.map(NonEmptyVector::toVector)).isEqualTo(Vector.ofAll(vector.grouped(size)));
                assertThat(groups.size()).isEqualTo((n + size - 1) / size);
                assertThat(groups.forAll(group -> group.size() >= 1 && group.size() <= size)).isTrue();
                assertThat(NonEmptyVector.flatten(nev(groups)).toVector()).isEqualTo(vector);
            }
            assertThatThrownBy(() -> nev.grouped(0)).isInstanceOf(IllegalArgumentException.class);
            // a size beyond the source is one group, and allocates only what the source holds
            final Vector<NonEmptyVector<Integer>> one = nev.grouped(Integer.MAX_VALUE);
            assertThat(one.size()).isEqualTo(1);
            assertThat(one.get(0).toVector()).isEqualTo(vector);
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyVectorTest#vectors")
        public void shouldGroupByIntoNonEmptyGroups(int n, Vector<Integer> vector) {
            final HashMap<Integer, NonEmptyVector<Integer>> groups = nev(vector).groupBy(i -> i % 5);
            final Map<Integer, Vector<Integer>> expected = vector.groupBy(i -> i % 5);
            assertThat(groups.size()).isEqualTo(Math.min(n, 5));
            assertThat(groups.mapValues(NonEmptyVector::toVector)).isEqualTo(HashMap.ofEntries(expected));
            assertThat(groups.values().map(NonEmptyVector::size).foldLeft(0, Integer::sum)).isEqualTo(n);
            assertThat(groups.get(0).get().head()).isEqualTo(0);
            assertThatNullPointerException().isThrownBy(() -> nev(vector).groupBy(null));
            assertThatNullPointerException().isThrownBy(() -> nev(vector).groupBy(i -> null));
        }
    }

    @Nested
    class ReturnsNonEmptyVectorLikeVector {

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyVectorTest#vectors")
        public void shouldReplaceEveryElementWithAValue(int n, Vector<Integer> vector) {
            final NonEmptyVector<String> actual = nonEmpty(nev(vector).as("x"));
            assertThat(actual.toVector()).isEqualTo(vector.as("x"));
            assertThat(actual.size()).isEqualTo(n);
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyVectorTest#vectors")
        public void shouldAppendAllAndPrependAllAnyIterable(int n, Vector<Integer> vector) {
            final NonEmptyVector<Integer> nev = nev(vector);
            for (int m : new int[] { 0, 1, 32, 33 }) {
                final Vector<Integer> that = Vector.range(100, 100 + m);
                final java.util.List<Integer> list = new java.util.ArrayList<>(that.asJava());
                assertThat(nonEmpty(nev.appendAll(list)).toVector()).isEqualTo(vector.appendAll(that));
                assertThat(nonEmpty(nev.prependAll(list)).toVector()).isEqualTo(vector.prependAll(that));
                assertThat(nev.appendAll(once(that)).toVector()).isEqualTo(vector.appendAll(that));
                assertThat(nev.prependAll(once(that)).toVector()).isEqualTo(vector.prependAll(that));
                assertThat(nev.appendAll(that.toList()).size()).isEqualTo(n + m);
            }
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyVectorTest#vectors")
        public void shouldInsert(int n, Vector<Integer> vector) {
            final NonEmptyVector<Integer> nev = nev(vector);
            for (int index : new int[] { 0, n / 2, n }) {
                assertThat(nonEmpty(nev.insert(index, -1)).toVector()).isEqualTo(vector.insert(index, -1));
                assertThat(nev.insert(index, -1).size()).isEqualTo(n + 1);
                for (int m : new int[] { 0, 1, 32, 33 }) {
                    final Vector<Integer> that = Vector.range(100, 100 + m);
                    assertThat(nonEmpty(nev.insertAll(index, that)).toVector()).isEqualTo(vector.insertAll(index, that));
                    assertThat(nev.insertAll(index, once(that)).toVector()).isEqualTo(vector.insertAll(index, that));
                    assertThat(nev.insertAll(index, that).size()).isEqualTo(n + m);
                }
            }
            assertThatThrownBy(() -> nev.insert(-1, 0)).isInstanceOf(IndexOutOfBoundsException.class);
            assertThatThrownBy(() -> nev.insert(n + 1, 0)).isInstanceOf(IndexOutOfBoundsException.class);
            assertThatThrownBy(() -> nev.insertAll(n + 1, Vector.of(0))).isInstanceOf(IndexOutOfBoundsException.class);
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyVectorTest#vectors")
        public void shouldIntersperse(int n, Vector<Integer> vector) {
            final NonEmptyVector<Integer> actual = nonEmpty(nev(vector).intersperse(-1));
            assertThat(actual.toVector()).isEqualTo(vector.intersperse(-1));
            assertThat(actual.size()).isEqualTo(2 * n - 1);
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyVectorTest#vectors")
        public void shouldPad(int n, Vector<Integer> vector) {
            final NonEmptyVector<Integer> nev = nev(vector);
            for (int length : new int[] { Integer.MIN_VALUE, 0, 1, n - 1, n, n + 1, n + 33 }) {
                assertThat(nonEmpty(nev.padTo(length, -1)).toVector()).isEqualTo(vector.padTo(length, -1));
                assertThat(nonEmpty(nev.leftPadTo(length, -1)).toVector()).isEqualTo(vector.leftPadTo(length, -1));
                assertThat(nev.padTo(length, -1).size()).isEqualTo(Math.max(n, length));
            }
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyVectorTest#vectors")
        public void shouldRotate(int n, Vector<Integer> vector) {
            final NonEmptyVector<Integer> nev = nev(vector);
            for (int k : new int[] { Integer.MIN_VALUE, -n - 1, -1, 0, 1, 2, 32, 33, n, n + 1, Integer.MAX_VALUE }) {
                assertThat(nonEmpty(nev.rotateLeft(k)).toVector()).isEqualTo(vector.rotateLeft(k));
                assertThat(nonEmpty(nev.rotateRight(k)).toVector()).isEqualTo(vector.rotateRight(k));
                assertThat(nev.rotateLeft(k).rotateRight(k)).isEqualTo(nev);
            }
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyVectorTest#vectors")
        public void shouldShuffle(int n, Vector<Integer> vector) {
            final NonEmptyVector<Integer> shuffled = nonEmpty(nev(vector).shuffle());
            assertThat(shuffled.size()).isEqualTo(n);
            assertThat(shuffled.sorted().toVector()).isEqualTo(vector);
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyVectorTest#vectors")
        public void shouldReplace(int n, Vector<Integer> vector) {
            final Vector<Integer> doubled = vector.appendAll(vector);
            final NonEmptyVector<Integer> nev = nev(doubled);
            for (int current : new int[] { 0, n - 1, n }) {
                assertThat(nonEmpty(nev.replace(current, -1)).toVector()).isEqualTo(doubled.replace(current, -1));
                assertThat(nonEmpty(nev.replaceAll(current, -1)).toVector()).isEqualTo(doubled.replaceAll(current, -1));
            }
            assertThat(nev.replaceAll(0, -1).count(i -> i == -1)).isEqualTo(2);
            assertThat(NonEmptyVector.single(1).replaceAll(1, 2)).isEqualTo(NonEmptyVector.single(2));
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyVectorTest#vectors")
        public void shouldScanAndScanRightWithOneMoreElement(int n, Vector<Integer> vector) {
            final NonEmptyVector<Integer> nev = nev(vector);
            final NonEmptyVector<Integer> scanned = nonEmpty(nev.scan(0, Integer::sum));
            assertThat(scanned.toVector()).isEqualTo(vector.scan(0, Integer::sum));
            assertThat(scanned.size()).isEqualTo(n + 1);
            final NonEmptyVector<String> scannedRight = nonEmpty(nev.scanRight("", (i, s) -> s + i));
            assertThat(scannedRight.toVector()).isEqualTo(vector.scanRight("", (i, s) -> s + i));
            assertThat(scannedRight.size()).isEqualTo(n + 1);
            assertThat(scannedRight.last()).isEqualTo("");
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyVectorTest#vectors")
        public void shouldZipAllUpToTheLongerSize(int n, Vector<Integer> vector) {
            final NonEmptyVector<Integer> nev = nev(vector);
            for (int m : new int[] { 0, 1, 32, 33, n, n + 33 }) {
                final Vector<String> that = Vector.range(0, m).map(String::valueOf);
                final NonEmptyVector<Tuple2<Integer, String>> zipped = nonEmpty(nev.zipAll(that, -1, "-"));
                assertThat(zipped.toVector()).isEqualTo(vector.zipAll(that, -1, "-"));
                assertThat(zipped.size()).isEqualTo(Math.max(n, m));
                assertThat(nev.zipAll(once(that), -1, "-").toVector()).isEqualTo(vector.zipAll(that, -1, "-"));
            }
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyVectorTest#vectors")
        public void shouldDistinctKeepingTheLast(int n, Vector<Integer> vector) {
            final Vector<Integer> doubled = vector.appendAll(vector);
            final NonEmptyVector<Integer> byKey = nonEmpty(nev(doubled).distinctByKeepLast(i -> i % 5));
            assertThat(byKey.toVector()).isEqualTo(doubled.distinctByKeepLast(i -> i % 5));
            assertThat(byKey.size()).isEqualTo(Math.min(n, 5));
            final Comparator<Integer> mod5 = Comparator.comparingInt(i -> i % 5);
            assertThat(nonEmpty(nev(doubled).distinctByKeepLast(mod5)).toVector()).isEqualTo(doubled.distinctByKeepLast(mod5));
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyVectorTest#vectors")
        public void shouldUnzipIntoNonEmptyVectors(int n, Vector<Integer> vector) {
            final NonEmptyVector<Integer> nev = nev(vector);
            final Tuple2<NonEmptyVector<Integer>, NonEmptyVector<String>> unzipped = nev.unzip(i -> Tuple.of(i, "s" + i));
            final Tuple2<Vector<Integer>, Vector<String>> expected = vector.unzip(i -> Tuple.of(i, "s" + i));
            assertThat(nonEmpty(unzipped._1()).toVector()).isEqualTo(expected._1());
            assertThat(nonEmpty(unzipped._2()).toVector()).isEqualTo(expected._2());
            final com.guizmaii.zazr.Tuple3<NonEmptyVector<Integer>, NonEmptyVector<String>, NonEmptyVector<Long>> unzipped3 = nev.unzip3(i -> Tuple.of(i, "s" + i, (long) i));
            final com.guizmaii.zazr.Tuple3<Vector<Integer>, Vector<String>, Vector<Long>> expected3 = vector.unzip3(i -> Tuple.of(i, "s" + i, (long) i));
            assertThat(nonEmpty(unzipped3._1()).toVector()).isEqualTo(expected3._1());
            assertThat(nonEmpty(unzipped3._2()).toVector()).isEqualTo(expected3._2());
            assertThat(nonEmpty(unzipped3._3()).toVector()).isEqualTo(expected3._3());
            assertThat(unzipped3._3().size()).isEqualTo(n);
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyVectorTest#vectors")
        public void shouldSlideIntoNonEmptyWindows(int n, Vector<Integer> vector) {
            final NonEmptyVector<Integer> nev = nev(vector);
            for (int size : new int[] { 1, 2, 5, 32, 33, 100, Integer.MAX_VALUE }) {
                final Vector<NonEmptyVector<Integer>> windows = nev.sliding(size);
                assertThat(windows.map(NonEmptyVector::toVector)).isEqualTo(vector.sliding(size));
                assertThat(windows.isEmpty()).isFalse();
                windows.forEach(NonEmptyVectorTest::nonEmpty);
                for (int step : new int[] { 1, 2, 33, Integer.MAX_VALUE }) {
                    final Vector<NonEmptyVector<Integer>> stepped = nev.sliding(size, step);
                    assertThat(stepped.map(NonEmptyVector::toVector)).isEqualTo(vector.sliding(size, step));
                    assertThat(stepped.isEmpty()).isFalse();
                    stepped.forEach(NonEmptyVectorTest::nonEmpty);
                }
            }
            assertThatThrownBy(() -> nev.sliding(0)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> nev.sliding(1, 0)).isInstanceOf(IllegalArgumentException.class);
            for (int width : new int[] { 1, 3, 32, 33, Integer.MAX_VALUE }) {
                final Vector<NonEmptyVector<Integer>> runs = nev.slideBy(i -> i / width);
                assertThat(runs.map(NonEmptyVector::toVector)).isEqualTo(vector.slideBy(i -> i / width));
                assertThat(runs.isEmpty()).isFalse();
                runs.forEach(NonEmptyVectorTest::nonEmpty);
                assertThat(NonEmptyVector.flatten(nev(runs)).toVector()).isEqualTo(vector);
            }
        }

        @ParameterizedTest
        @ValueSource(ints = { 1, 2, 3, 6 })
        public void shouldPermuteCombineAndCrossIntoNonEmptyVectors(int n) {
            final Vector<Integer> vector = Vector.range(0, n);
            final NonEmptyVector<Integer> nev = nev(vector);
            final NonEmptyVector<NonEmptyVector<Integer>> permutations = nonEmpty(nev.permutations());
            assertThat(permutations.toVector().map(NonEmptyVector::toVector)).isEqualTo(vector.permutations());
            permutations.forEach(NonEmptyVectorTest::nonEmpty);
            assertThat(permutations.size()).isEqualTo(Vector.rangeClosed(1, n).fold(1, (a, b) -> a * b));
            // equal elements are permuted once
            assertThat(nev(vector.map(i -> 0)).permutations().size()).isEqualTo(1);
            final NonEmptyVector<Vector<Integer>> combinations = nonEmpty(nev.combinations());
            assertThat(combinations.toVector()).isEqualTo(vector.combinations());
            assertThat(combinations.size()).isEqualTo(1 << n);
            assertThat(combinations.head()).isEqualTo(Vector.empty());
            assertThat(combinations.last()).isEqualTo(vector);
            final NonEmptyVector<Tuple2<Integer, Integer>> square = nonEmpty(nev.crossProduct());
            assertThat(square.toVector()).isEqualTo(vector.crossProduct());
            assertThat(square.size()).isEqualTo(n * n);
            final Vector<String> that = Vector.of("a", "b", "c");
            final NonEmptyVector<Tuple2<Integer, String>> product = nonEmpty(nev.crossProduct(nev(that)));
            assertThat(product.toVector()).isEqualTo(vector.crossProduct(that));
            assertThat(product.size()).isEqualTo(n * 3);
        }

        @ParameterizedTest
        @ValueSource(ints = { 1, 2, 3, 6, 32, 33 })
        public void shouldTransposeIntoNonEmptyColumns(int n) {
            // one row of n elements: n columns of one element
            final NonEmptyVector<NonEmptyVector<Integer>> row = NonEmptyVector.single(nev(Vector.range(0, n)));
            final NonEmptyVector<NonEmptyVector<Integer>> columns = nonEmpty(NonEmptyVector.transpose(row));
            assertThat(columns.toVector().map(NonEmptyVector::toVector)).isEqualTo(Vector.range(0, n).map(Vector::of));
            columns.forEach(NonEmptyVectorTest::nonEmpty);
            // one column of n rows: one row of n elements
            final NonEmptyVector<NonEmptyVector<Integer>> column = nev(Vector.range(0, n).map(NonEmptyVector::single));
            assertThat(NonEmptyVector.transpose(column)).isEqualTo(NonEmptyVector.single(nev(Vector.range(0, n))));
            // n rows of 3: 3 rows of n, as Vector.transpose gives
            final NonEmptyVector<NonEmptyVector<Integer>> matrix = nev(Vector.range(0, n).map(i -> nev(Vector.range(3 * i, 3 * i + 3))));
            final NonEmptyVector<NonEmptyVector<Integer>> transposed = nonEmpty(NonEmptyVector.transpose(matrix));
            assertThat(transposed.toVector().map(NonEmptyVector::toVector)).isEqualTo(Vector.transpose(matrix.toVector().map(NonEmptyVector::toVector)));
            assertThat(transposed.size()).isEqualTo(3);
            transposed.forEach(r -> assertThat(nonEmpty(r).size()).isEqualTo(n));
            assertThat(NonEmptyVector.transpose(transposed)).isEqualTo(matrix);
            if (n > 1) {
                final NonEmptyVector<NonEmptyVector<Integer>> ragged = matrix.update(0, NonEmptyVector.single(0));
                assertThatIllegalArgumentException().isThrownBy(() -> NonEmptyVector.transpose(ragged));
            }
            final NonEmptyVector<NonEmptyVector<Number>> covariant = NonEmptyVector.transpose(NonEmptyVector.single(NonEmptyVector.of(1, 2)));
            assertThat(covariant.size()).isEqualTo(2);
        }
    }

    @Nested
    class ReturnsVector {

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyVectorTest#vectors")
        public void shouldReturnTheWrappedVector(int n, Vector<Integer> vector) {
            assertThat(nev(vector).toVector()).isSameAs(vector);
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyVectorTest#vectors")
        public void shouldFilterRejectAndCollect(int n, Vector<Integer> vector) {
            final NonEmptyVector<Integer> nev = nev(vector);
            assertThat(nev.filter(i -> i % 2 == 0)).isEqualTo(vector.filter(i -> i % 2 == 0));
            assertThat(nev.filter(i -> false)).isEqualTo(Vector.empty());
            assertThat(nev.reject(i -> i % 2 == 0)).isEqualTo(vector.reject(i -> i % 2 == 0));
            assertThat(nev.reject(i -> true)).isEqualTo(Vector.empty());
            final Function<Integer, Option<String>> mapper = i -> i % 3 == 0 ? Option.some("x" + i) : Option.none();
            assertThat(nev.collect(mapper)).isEqualTo(vector.collect(mapper));
            assertThat(nev.collect(i -> Option.<Integer> none())).isEqualTo(Vector.empty());
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyVectorTest#vectors")
        public void shouldFlatMapAllToIterables(int n, Vector<Integer> vector) {
            final NonEmptyVector<Integer> nev = nev(vector);
            assertThat(nev.flatMapAll(i -> java.util.List.of(i, i))).isEqualTo(vector.flatMap(i -> java.util.List.of(i, i)));
            assertThat(nev.flatMapAll(i -> Vector.<Integer> empty())).isEqualTo(Vector.empty());
            assertThat(nev.flatMapAll(i -> i % 2 == 0 ? Vector.of(i) : Vector.empty())).isEqualTo(vector.filter(i -> i % 2 == 0));
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyVectorTest#vectors")
        public void shouldTailAndInit(int n, Vector<Integer> vector) {
            final NonEmptyVector<Integer> nev = nev(vector);
            assertThat(nev.tail()).isEqualTo(vector.tail());
            assertThat(nev.tail().size()).isEqualTo(n - 1);
            assertThat(nev.init()).isEqualTo(vector.init());
            assertThat(nev.init().size()).isEqualTo(n - 1);
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyVectorTest#vectors")
        public void shouldDropAndTake(int n, Vector<Integer> vector) {
            final NonEmptyVector<Integer> nev = nev(vector);
            for (int k : new int[] { -1, 0, 1, n - 1, n, n + 1 }) {
                assertThat(nev.drop(k)).isEqualTo(vector.drop(k));
                assertThat(nev.dropRight(k)).isEqualTo(vector.dropRight(k));
                assertThat(nev.take(k)).isEqualTo(vector.take(k));
                assertThat(nev.takeRight(k)).isEqualTo(vector.takeRight(k));
            }
            assertThat(nev.drop(n)).isEqualTo(Vector.empty());
            assertThat(nev.take(0)).isEqualTo(Vector.empty());
            final int half = n / 2;
            assertThat(nev.dropWhile(i -> i < half)).isEqualTo(vector.dropWhile(i -> i < half));
            assertThat(nev.dropUntil(i -> i >= half)).isEqualTo(vector.dropUntil(i -> i >= half));
            assertThat(nev.dropRightWhile(i -> i >= half)).isEqualTo(vector.dropRightWhile(i -> i >= half));
            assertThat(nev.dropRightUntil(i -> i < half)).isEqualTo(vector.dropRightUntil(i -> i < half));
            assertThat(nev.takeWhile(i -> i < half)).isEqualTo(vector.takeWhile(i -> i < half));
            assertThat(nev.takeUntil(i -> i >= half)).isEqualTo(vector.takeUntil(i -> i >= half));
            assertThat(nev.takeRightWhile(i -> i >= half)).isEqualTo(vector.takeRightWhile(i -> i >= half));
            assertThat(nev.takeRightUntil(i -> i < half)).isEqualTo(vector.takeRightUntil(i -> i < half));
            assertThat(nev.dropWhile(i -> true)).isEqualTo(Vector.empty());
            assertThat(nev.takeWhile(i -> false)).isEqualTo(Vector.empty());
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyVectorTest#vectors")
        public void shouldSlice(int n, Vector<Integer> vector) {
            final NonEmptyVector<Integer> nev = nev(vector);
            for (int from : new int[] { -1, 0, 1, n }) {
                for (int to : new int[] { 0, 1, n - 1, n, n + 1 }) {
                    assertThat(nev.slice(from, to)).isEqualTo(vector.slice(from, to));
                }
            }
            assertThat(nev.slice(0, 0)).isEqualTo(Vector.empty());
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyVectorTest#vectors")
        public void shouldRemove(int n, Vector<Integer> vector) {
            final NonEmptyVector<Integer> nev = nev(vector);
            for (int index : new int[] { 0, n - 1 }) {
                assertThat(nev.removeAt(index)).isEqualTo(vector.removeAt(index));
                assertThat(nev.removeAt(index).size()).isEqualTo(n - 1);
            }
            assertThatThrownBy(() -> nev.removeAt(n)).isInstanceOf(IndexOutOfBoundsException.class);
            assertThat(nev.remove(0)).isEqualTo(vector.remove(0));
            assertThat(nev.remove(-1)).isEqualTo(vector);
            assertThat(nev.removeAll(0)).isEqualTo(vector.removeAll(0));
            assertThat(nev.removeAll(Vector.range(0, 2))).isEqualTo(vector.removeAll(Vector.range(0, 2)));
            assertThat(nev.removeAll(i -> i % 2 == 0)).isEqualTo(vector.removeAll(i -> i % 2 == 0));
            assertThat(nev.removeAll(vector)).isEqualTo(Vector.empty());
            assertThat(nev.removeAll(i -> true)).isEqualTo(Vector.empty());
            assertThat(NonEmptyVector.single(7).remove(7)).isEqualTo(Vector.empty());
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyVectorTest#vectors")
        public void shouldPartitionMap(int n, Vector<Integer> vector) {
            final NonEmptyVector<Integer> nev = nev(vector);
            final Function<Integer, Either<String, Integer>> f = i -> i % 2 == 0 ? Either.left("e" + i) : Either.right(i);
            final Tuple2<Vector<String>, Vector<Integer>> actual = nev.partitionMap(f);
            assertThat(actual).isEqualTo(vector.partitionMap(f));
            assertThat(actual._1()).isEqualTo(vector.filter(i -> i % 2 == 0).map(i -> "e" + i));
            assertThat(actual._2()).isEqualTo(vector.filter(i -> i % 2 != 0));
            assertThat(nev.partitionMap(i -> Either.<Integer, Integer> left(i))).isEqualTo(Tuple.of(vector, Vector.empty()));
            assertThat(nev.partitionMap(i -> Either.<Integer, Integer> right(i))).isEqualTo(Tuple.of(Vector.empty(), vector));
            final int last = n - 1;
            assertThatNullPointerException()
                    .isThrownBy(() -> nev.partitionMap(i -> i == last ? null : Either.<Integer, Integer> left(i)))
                    .withMessage("NonEmptyVector.partitionMap: f returned null");
            assertThatNullPointerException().isThrownBy(() -> nev.partitionMap(null)).withMessage("f is null");
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyVectorTest#vectors")
        public void shouldFindDuplicates(int n, Vector<Integer> vector) {
            final NonEmptyVector<Integer> nev = nev(vector);
            assertThat(nev.duplicates()).isEqualTo(Vector.empty());
            assertThat(nev.duplicatesBy(i -> i % 5)).isEqualTo(vector.duplicatesBy(i -> i % 5));
            final Vector<Integer> doubled = vector.appendAll(vector);
            assertThat(nev(doubled).duplicates()).isEqualTo(vector);
            assertThat(nev(doubled).duplicatesBy(i -> i % 5)).isEqualTo(doubled.duplicatesBy(i -> i % 5));
            assertThat(nev(doubled).duplicatesBy(i -> i % 5)).isEqualTo(Vector.range(0, Math.min(n, 5)));
        }
    }

    @Nested
    class ReturnsVectorLikeVector {

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyVectorTest#vectors")
        public void shouldSplitAt(int n, Vector<Integer> vector) {
            final NonEmptyVector<Integer> nev = nev(vector);
            for (int k : new int[] { Integer.MIN_VALUE, -1, 0, 1, 32, 33, n - 1, n, n + 1 }) {
                assertThat(nev.splitAt(k)).isEqualTo(vector.splitAt(k));
            }
            assertThat(nev.splitAt(0)).isEqualTo(Tuple.of(Vector.empty(), vector));
            assertThat(nev.splitAt(n)).isEqualTo(Tuple.of(vector, Vector.empty()));
            final int half = n / 2;
            assertThat(nev.splitAt(i -> i >= half)).isEqualTo(vector.splitAt(i -> i >= half));
            assertThat(nev.splitAt(i -> true)).isEqualTo(Tuple.of(Vector.empty(), vector));
            assertThat(nev.splitAt(i -> false)).isEqualTo(Tuple.of(vector, Vector.empty()));
            assertThat(nev.span(i -> i < half)).isEqualTo(vector.span(i -> i < half));
            assertThat(nev.span(i -> false)).isEqualTo(Tuple.of(Vector.empty(), vector));
            assertThat(nev.span(i -> true)).isEqualTo(Tuple.of(vector, Vector.empty()));
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyVectorTest#vectors")
        public void shouldSplitAtInclusiveWithANonEmptyFirstPart(int n, Vector<Integer> vector) {
            final NonEmptyVector<Integer> nev = nev(vector);
            for (int at : new int[] { 0, 1, 31, 32, n - 1, n }) {
                final Tuple2<NonEmptyVector<Integer>, Vector<Integer>> split = nev.splitAtInclusive(i -> i == at);
                final Tuple2<Vector<Integer>, Vector<Integer>> expected = vector.splitAtInclusive(i -> i == at);
                assertThat(nonEmpty(split._1()).toVector()).isEqualTo(expected._1());
                assertThat(split._2()).isEqualTo(expected._2());
            }
            assertThat(nev.splitAtInclusive(i -> true)._1()).isEqualTo(NonEmptyVector.single(0));
            assertThat(nev.splitAtInclusive(i -> false)._2()).isEqualTo(Vector.empty());
            assertThat(nev.splitAtInclusive(i -> i == n - 1)._2()).isEqualTo(Vector.empty());
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyVectorTest#vectors")
        public void shouldPartition(int n, Vector<Integer> vector) {
            final NonEmptyVector<Integer> nev = nev(vector);
            assertThat(nev.partition(i -> i % 2 == 0)).isEqualTo(vector.partition(i -> i % 2 == 0));
            assertThat(nev.partition(i -> true)).isEqualTo(Tuple.of(vector, Vector.empty()));
            assertThat(nev.partition(i -> false)).isEqualTo(Tuple.of(Vector.empty(), vector));
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyVectorTest#vectors")
        public void shouldRemoveFirstAndLastAndRetainAll(int n, Vector<Integer> vector) {
            final Vector<Integer> doubled = vector.appendAll(vector);
            final NonEmptyVector<Integer> nev = nev(doubled);
            for (int element : new int[] { 0, n - 1, n }) {
                assertThat(nev.removeFirst(i -> i == element)).isEqualTo(doubled.removeFirst(i -> i == element));
                assertThat(nev.removeLast(i -> i == element)).isEqualTo(doubled.removeLast(i -> i == element));
            }
            final Predicate<Object> any = o -> true;
            assertThat(nev.removeFirst(any)).isEqualTo(doubled.tail());
            assertThat(nev.removeLast(any)).isEqualTo(doubled.init());
            assertThat(NonEmptyVector.single(1).removeFirst(i -> true)).isEqualTo(Vector.empty());
            assertThat(NonEmptyVector.single(1).removeLast(i -> true)).isEqualTo(Vector.empty());
            final Vector<Integer> kept = Vector.of(0, n - 1, n + 5);
            assertThat(nev.retainAll(kept)).isEqualTo(doubled.retainAll(kept));
            assertThat(nev.retainAll(Vector.empty())).isEqualTo(Vector.empty());
            assertThat(nev.retainAll(vector)).isEqualTo(doubled);
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyVectorTest#vectors")
        public void shouldPatch(int n, Vector<Integer> vector) {
            final NonEmptyVector<Integer> nev = nev(vector);
            for (int from : new int[] { -1, 0, 1, n - 1, n, n + 1 }) {
                for (int replaced : new int[] { -1, 0, 1, 33, n }) {
                    for (int m : new int[] { 0, 1, 33 }) {
                        final Vector<Integer> that = Vector.range(100, 100 + m);
                        assertThat(nev.patch(from, that, replaced)).isEqualTo(vector.patch(from, that, replaced));
                        assertThat(nev.patch(from, once(that), replaced)).isEqualTo(vector.patch(from, that, replaced));
                    }
                }
            }
            assertThat(nev.patch(0, Vector.empty(), n)).isEqualTo(Vector.empty());
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyVectorTest#vectors")
        public void shouldSubSequence(int n, Vector<Integer> vector) {
            final NonEmptyVector<Integer> nev = nev(vector);
            for (int from : new int[] { 0, 1, n - 1, n }) {
                if (from <= n) {
                    assertThat(nev.subSequence(from)).isEqualTo(vector.subSequence(from));
                }
                for (int to : new int[] { from, n }) {
                    assertThat(nev.subSequence(from, to)).isEqualTo(vector.subSequence(from, to));
                }
            }
            assertThat(nev.subSequence(n)).isEqualTo(Vector.empty());
            assertThat(nev.subSequence(0, 0)).isEqualTo(Vector.empty());
            assertThatThrownBy(() -> nev.subSequence(-1)).isInstanceOf(IndexOutOfBoundsException.class);
            assertThatThrownBy(() -> nev.subSequence(n + 1)).isInstanceOf(IndexOutOfBoundsException.class);
            assertThatThrownBy(() -> nev.subSequence(-1, n)).isInstanceOf(IndexOutOfBoundsException.class);
            assertThatThrownBy(() -> nev.subSequence(0, n + 1)).isInstanceOf(IndexOutOfBoundsException.class);
            assertThatIllegalArgumentException().isThrownBy(() -> nev.subSequence(1, 0));
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyVectorTest#vectors")
        public void shouldZipAndCrossAnyIterable(int n, Vector<Integer> vector) {
            final NonEmptyVector<Integer> nev = nev(vector);
            for (int m : new int[] { 0, 1, 32, 33 }) {
                final Vector<String> that = Vector.range(0, m).map(String::valueOf);
                final java.util.List<String> list = new java.util.ArrayList<>(that.asJava());
                assertThat(nev.zip(list)).isEqualTo(vector.zip(that));
                assertThat(nev.zip(once(that))).isEqualTo(vector.zip(that));
                assertThat(nev.zipWith(list, (i, s) -> i + s)).isEqualTo(vector.zipWith(that, (i, s) -> i + s));
                assertThat(nev.zipWith(once(that), (i, s) -> i + s)).isEqualTo(vector.zipWith(that, (i, s) -> i + s));
                if (n <= 33) {
                    assertThat(nev.crossProduct(list)).isEqualTo(vector.crossProduct(that));
                    assertThat(nev.crossProduct(once(that))).isEqualTo(vector.crossProduct(that));
                }
            }
            assertThat(nev.zip(java.util.List.<String> of())).isEqualTo(Vector.empty());
            assertThat(nev.zipWith(java.util.List.<String> of(), (i, s) -> i + s)).isEqualTo(Vector.empty());
            assertThat(nev.crossProduct(java.util.List.<String> of())).isEqualTo(Vector.empty());
        }

        @ParameterizedTest
        @ValueSource(ints = { 1, 2, 3, 6 })
        public void shouldCombineAndRaiseToAPower(int n) {
            final Vector<Integer> vector = Vector.range(0, n);
            final NonEmptyVector<Integer> nev = nev(vector);
            for (int k : new int[] { -1, 0, 1, n / 2, n, n + 1 }) {
                assertThat(nev.combinations(k)).isEqualTo(vector.combinations(k));
            }
            assertThat(nev.combinations(n + 1)).isEqualTo(Vector.empty());
            assertThat(nev.combinations(0)).isEqualTo(Vector.of(Vector.empty()));
            for (int power : new int[] { -1, 0, 1, 2, 3 }) {
                assertThat(nev.crossProduct(power)).isEqualTo(vector.crossProduct(power));
            }
            assertThat(nev.crossProduct(-1)).isEqualTo(Vector.empty());
            assertThat(nev.crossProduct(0)).isEqualTo(Vector.of(Vector.empty()));
            assertThat(nev.crossProduct(3).size()).isEqualTo(n * n * n);
        }
    }

    @Nested
    class Total {

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyVectorTest#vectors")
        public void shouldReturnHeadLastSizeAndGet(int n, Vector<Integer> vector) {
            final NonEmptyVector<Integer> nev = nev(vector);
            assertThat(nev.head()).isEqualTo(vector.head()).isEqualTo(0);
            assertThat(nev.last()).isEqualTo(vector.last()).isEqualTo(n - 1);
            assertThat(nev.size()).isEqualTo(vector.size()).isEqualTo(n);
            for (int i = 0; i < n; i++) {
                assertThat(nev.get(i)).isEqualTo(vector.get(i));
            }
            assertThatThrownBy(() -> nev.get(n)).isInstanceOf(IndexOutOfBoundsException.class);
            assertThatThrownBy(() -> nev.get(-1)).isInstanceOf(IndexOutOfBoundsException.class);
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyVectorTest#vectors")
        public void shouldReturnMaxAndMin(int n, Vector<Integer> vector) {
            final NonEmptyVector<Integer> nev = nev(vector.reverse());
            assertThat(nev.max(Comparator.naturalOrder())).isEqualTo(vector.max().get()).isEqualTo(n - 1);
            assertThat(nev.min(Comparator.naturalOrder())).isEqualTo(vector.min().get()).isEqualTo(0);
            assertThat(nev.max(Comparator.reverseOrder())).isEqualTo(0);
            assertThat(nev.min(Comparator.reverseOrder())).isEqualTo(n - 1);
            assertThat(nev.maxBy(i -> -i)).isEqualTo(vector.maxBy(i -> -i).get()).isEqualTo(0);
            assertThat(nev.minBy(i -> -i)).isEqualTo(vector.minBy(i -> -i).get()).isEqualTo(n - 1);
            // ties: the first element wins, as on Vector
            final Comparator<Integer> mod2 = Comparator.comparingInt(i -> i % 2);
            assertThat(nev.max(mod2)).isEqualTo(nev.toVector().maxBy(mod2).get());
            assertThat(nev.min(mod2)).isEqualTo(nev.toVector().minBy(mod2).get());
            assertThat(nev.maxBy(i -> i % 2)).isEqualTo(nev.toVector().maxBy(i -> i % 2).get());
            assertThat(nev.minBy(i -> i % 2)).isEqualTo(nev.toVector().minBy(i -> i % 2).get());
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyVectorTest#vectors")
        public void shouldReduce(int n, Vector<Integer> vector) {
            final NonEmptyVector<Integer> nev = nev(vector);
            assertThat(nev.reduce(Integer::sum)).isEqualTo(vector.reduce(Integer::sum));
            assertThat(nev.reduceLeft((a, b) -> a - b)).isEqualTo(vector.reduceLeft((a, b) -> a - b));
            assertThat(nev.reduceRight((a, b) -> a - b)).isEqualTo(vector.reduceRight((a, b) -> a - b));
            final String concatenated = nev.reduceMap(String::valueOf, String::concat);
            assertThat(concatenated).isEqualTo(vector.mkString());
            assertThat(nev.reduceMap(i -> i * 2, Integer::sum)).isEqualTo(n * (n - 1));
            assertThat(NonEmptyVector.single(5).reduce((a, b) -> a * b)).isEqualTo(5);
            final String single = NonEmptyVector.single(5).reduceMap(i -> "v" + i, (a, b) -> { throw new AssertionError(); });
            assertThat(single).isEqualTo("v5");
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyVectorTest#vectors")
        public void shouldMkString(int n, Vector<Integer> vector) {
            final NonEmptyVector<Integer> nev = nev(vector);
            assertThat(nev.mkString()).isEqualTo(vector.mkString());
            assertThat(nev.mkString(", ")).isEqualTo(vector.mkString(", "));
            assertThat(nev.mkString("[", "|", "]")).isEqualTo(vector.mkString("[", "|", "]"));
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyVectorTest#vectors")
        public void shouldFold(int n, Vector<Integer> vector) {
            final NonEmptyVector<Integer> nev = nev(vector);
            assertThat(nev.foldLeft("", (s, i) -> s + i)).isEqualTo(vector.foldLeft("", (s, i) -> s + i));
            assertThat(nev.foldRight("", (i, s) -> s + i)).isEqualTo(vector.foldRight("", (i, s) -> s + i));
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyVectorTest#vectors")
        public void shouldSearch(int n, Vector<Integer> vector) {
            final NonEmptyVector<Integer> nev = nev(vector);
            assertThat(nev.contains(n - 1)).isTrue();
            assertThat(nev.contains(n)).isFalse();
            assertThat(nev.exists(i -> i == n - 1)).isTrue();
            assertThat(nev.exists(i -> i == n)).isFalse();
            assertThat(nev.forAll(i -> i < n)).isTrue();
            assertThat(nev.forAll(i -> i < n - 1)).isFalse();
            assertThat(nev.count(i -> i % 2 == 0)).isEqualTo(vector.count(i -> i % 2 == 0));
            assertThat(nev.indexOf(n - 1)).isEqualTo(n - 1);
            assertThat(nev.indexOf(n)).isEqualTo(-1);
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyVectorTest#vectors")
        public void shouldIterateStreamAndConvert(int n, Vector<Integer> vector) {
            final NonEmptyVector<Integer> nev = nev(vector);
            assertThat(nev).containsExactlyElementsOf(vector);
            assertThat(nev.iterator().hasNext()).isTrue();
            final java.util.Spliterator<Integer> spliterator = nev.spliterator();
            assertThat(spliterator.hasCharacteristics(java.util.Spliterator.SIZED | java.util.Spliterator.SUBSIZED | java.util.Spliterator.ORDERED | java.util.Spliterator.IMMUTABLE | java.util.Spliterator.NONNULL)).isTrue();
            assertThat(spliterator.getExactSizeIfKnown()).isEqualTo(n);
            assertThat(java.util.stream.StreamSupport.stream(nev.spliterator(), false).toList()).isEqualTo(new java.util.ArrayList<>(vector.asJava()));
            assertThat(nev.stream().toList()).isEqualTo(new java.util.ArrayList<>(vector.asJava()));
            assertThat(nev.asJava()).isEqualTo(vector.asJava());
            assertThat(nev.asJava().size()).isEqualTo(n);
            assertThatThrownBy(() -> nev.asJava().add(1)).isInstanceOf(UnsupportedOperationException.class);
            assertThat(nev.toList()).isEqualTo(vector.toList());
            assertThat((Object) nev.toSet()).isEqualTo(vector.toSet());
            assertThat(nev.toSet().size()).isEqualTo(n);
        }
    }

    @Nested
    class QueriesLikeVector {

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyVectorTest#vectors")
        public void shouldFindIndices(int n, Vector<Integer> vector) {
            final Vector<Integer> doubled = vector.appendAll(vector);
            final NonEmptyVector<Integer> nev = nev(doubled);
            for (int element : new int[] { 0, n / 2, n - 1, n }) {
                assertThat(nev.lastIndexOf(element)).isEqualTo(doubled.lastIndexOf(element));
                assertThat(nev.indexWhere(i -> i == element)).isEqualTo(doubled.indexWhere(i -> i == element));
                assertThat(nev.lastIndexWhere(i -> i == element)).isEqualTo(doubled.lastIndexWhere(i -> i == element));
                for (int position : new int[] { Integer.MIN_VALUE, -1, 0, 1, 32, 33, n - 1, n, 2 * n - 1, 2 * n, Integer.MAX_VALUE }) {
                    assertThat(nev.indexOf(element, position)).isEqualTo(doubled.indexOf(element, position));
                    assertThat(nev.lastIndexOf(element, position)).isEqualTo(doubled.lastIndexOf(element, position));
                    assertThat(nev.indexWhere(i -> i == element, position)).isEqualTo(doubled.indexWhere(i -> i == element, position));
                    assertThat(nev.lastIndexWhere(i -> i == element, position)).isEqualTo(doubled.lastIndexWhere(i -> i == element, position));
                }
            }
            assertThat(nev.lastIndexOf(0)).isEqualTo(n);
            assertThat(nev.indexOf(0, 1)).isEqualTo(n);
            assertThat(nev.lastIndexOf(n)).isEqualTo(-1);
            assertThat(nev.indexWhere(i -> i < 0)).isEqualTo(-1);
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyVectorTest#vectors")
        public void shouldFindSlices(int n, Vector<Integer> vector) {
            final Vector<Integer> doubled = vector.appendAll(vector);
            final NonEmptyVector<Integer> nev = nev(doubled);
            for (Vector<Integer> slice : java.util.List.of(Vector.<Integer> empty(), Vector.of(0), vector.take(2), vector.takeRight(2), vector, vector.append(n), Vector.of(n))) {
                assertThat(nev.startsWith(slice)).isEqualTo(doubled.startsWith(slice));
                assertThat(nev.startsWith(slice.toList())).isEqualTo(doubled.startsWith(slice));
                assertThat(nev.endsWith(slice)).isEqualTo(doubled.endsWith(slice));
                assertThat(nev.containsSlice(slice)).isEqualTo(doubled.containsSlice(slice));
                assertThat(nev.indexOfSlice(slice)).isEqualTo(doubled.indexOfSlice(slice));
                assertThat(nev.lastIndexOfSlice(slice)).isEqualTo(doubled.lastIndexOfSlice(slice));
                for (int position : new int[] { -1, 0, 1, n - 1, n, 2 * n }) {
                    assertThat(nev.startsWith(slice, position)).isEqualTo(doubled.startsWith(slice, position));
                    assertThat(nev.indexOfSlice(slice, position)).isEqualTo(doubled.indexOfSlice(slice, position));
                    assertThat(nev.lastIndexOfSlice(slice, position)).isEqualTo(doubled.lastIndexOfSlice(slice, position));
                }
            }
            assertThat(nev.startsWith(vector)).isTrue();
            assertThat(nev.endsWith(vector)).isTrue();
            assertThat(nev.startsWith(vector, n)).isTrue();
            assertThat(nev.containsSlice(Vector.of(n))).isFalse();
            assertThat(nev.lastIndexOfSlice(vector)).isEqualTo(n);
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyVectorTest#vectors")
        public void shouldSearchSortedElements(int n, Vector<Integer> vector) {
            final NonEmptyVector<Integer> nev = nev(vector);
            final NonEmptyVector<Integer> descending = nev(vector.reverse());
            for (int element : new int[] { -1, 0, 1, n / 2, n - 1, n }) {
                assertThat(nev.search(element)).isEqualTo(vector.search(element));
                assertThat(descending.search(element, Comparator.reverseOrder())).isEqualTo(vector.reverse().search(element, Comparator.reverseOrder()));
            }
            assertThat(nev.search(n - 1)).isEqualTo(n - 1);
            assertThat(nev.search(n)).isEqualTo(-n - 1);
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyVectorTest#vectors")
        public void shouldMeasureSegmentsAndTestMembership(int n, Vector<Integer> vector) {
            final NonEmptyVector<Integer> nev = nev(vector);
            final int half = n / 2;
            for (int from : new int[] { Integer.MIN_VALUE, -1, 0, 1, half, n - 1, n, n + 1 }) {
                assertThat(nev.segmentLength(i -> i < half, from)).isEqualTo(vector.segmentLength(i -> i < half, from));
            }
            assertThat(nev.prefixLength(i -> i < half)).isEqualTo(vector.prefixLength(i -> i < half)).isEqualTo(half);
            assertThat(nev.prefixLength(i -> true)).isEqualTo(n);
            assertThat(nev.existsUnique(i -> i == half)).isTrue();
            assertThat(nev.existsUnique(i -> i == n)).isFalse();
            assertThat(nev.existsUnique(i -> true)).isEqualTo(n == 1);
            assertThat(nev.containsAll(Vector.of(0, n - 1))).isTrue();
            assertThat(nev.containsAll(Vector.of(0, n))).isFalse();
            assertThat(nev.containsAll(Vector.empty())).isTrue();
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyVectorTest#vectors")
        public void shouldAggregateTotally(int n, Vector<Integer> vector) {
            final NonEmptyVector<Integer> nev = nev(vector.reverse());
            assertThat(nev.max()).isEqualTo(vector.max().get()).isEqualTo(n - 1);
            assertThat(nev.min()).isEqualTo(vector.min().get()).isEqualTo(0);
            assertThat(nev.maxBy(Comparator.<Integer> reverseOrder())).isEqualTo(vector.maxBy(Comparator.<Integer> reverseOrder()).get()).isEqualTo(0);
            assertThat(nev.minBy(Comparator.<Integer> reverseOrder())).isEqualTo(vector.minBy(Comparator.<Integer> reverseOrder()).get()).isEqualTo(n - 1);
            final Comparator<Integer> mod2 = Comparator.comparingInt(i -> i % 2);
            assertThat(nev.maxBy(mod2)).isEqualTo(nev.toVector().maxBy(mod2).get());
            assertThat(nev.minBy(mod2)).isEqualTo(nev.toVector().minBy(mod2).get());
            assertThat(nev.fold(0, Integer::sum)).isEqualTo(vector.fold(0, Integer::sum));
            assertThat(nev.sum()).isEqualTo(vector.sum()).isEqualTo((long) n * (n - 1) / 2);
            final NonEmptyVector<Integer> small = nev(vector.map(i -> i % 3 + 1));
            assertThat(small.product()).isEqualTo(small.toVector().product());
            final double average = nev.average();
            assertThat(average).isEqualTo(vector.average().get()).isEqualTo((n - 1) / 2.0);
            final NonEmptyVector<Double> doubles = nev(vector.map(i -> i / 3.0));
            assertThat(doubles.average()).isEqualTo(doubles.toVector().average().get());
            assertThat(doubles.sum()).isEqualTo(doubles.toVector().sum());
        }

        @Test
        public void shouldTakeTheMinimumAndMaximumOfFloatingPointNumbersAsVectorDoes() {
            final NonEmptyVector<Double> doubles = NonEmptyVector.of(2.0, Double.NaN, -1.0, 0.0, -0.0);
            assertThat(doubles.min()).isEqualTo(doubles.toVector().min().get()).isNaN();
            assertThat(doubles.max()).isEqualTo(doubles.toVector().max().get());
            final NonEmptyVector<Double> zeros = NonEmptyVector.of(0.0, -0.0);
            assertThat(zeros.min()).isEqualTo(zeros.toVector().min().get()).isEqualTo(-0.0);
            final NonEmptyVector<Float> floats = NonEmptyVector.of(2f, Float.NaN, -1f);
            assertThat(floats.min()).isEqualTo(floats.toVector().min().get()).isNaN();
            assertThat(NonEmptyVector.of(3f, 1f, 2f).min()).isEqualTo(1f);
            assertThat(NonEmptyVector.of("b", "a", "c").min()).isEqualTo("a");
            assertThat(NonEmptyVector.single(1.5).min()).isEqualTo(1.5);
            assertThatThrownBy(() -> NonEmptyVector.of(new Object(), new Object()).max()).isInstanceOf(ClassCastException.class);
            assertThatThrownBy(() -> NonEmptyVector.of("a").average()).isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(() -> NonEmptyVector.of("a").sum()).isInstanceOf(UnsupportedOperationException.class);
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyVectorTest#vectors")
        public void shouldReturnTheSingleElementOnlyWhenThereIsOne(int n, Vector<Integer> vector) {
            final NonEmptyVector<Integer> nev = nev(vector);
            if (n == 1) {
                assertThat(nev.single()).isEqualTo(0);
            } else {
                assertThatThrownBy(nev::single).isInstanceOf(java.util.NoSuchElementException.class);
            }
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyVectorTest#vectors")
        public void shouldArrangeByAUniqueKey(int n, Vector<Integer> vector) {
            final NonEmptyVector<Integer> nev = nev(vector);
            assertThat(nev.arrangeBy(i -> "k" + i)).isEqualTo(vector.arrangeBy(i -> "k" + i));
            assertThat(nev.arrangeBy(i -> "k" + i).get().size()).isEqualTo(n);
            assertThat(nev.arrangeBy(i -> i % 2)).isEqualTo(vector.arrangeBy(i -> i % 2));
            assertThat(nev.arrangeBy(i -> i % 2).isDefined()).isEqualTo(n <= 2);
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyVectorTest#vectors")
        public void shouldVisitWithIndicesAndCollect(int n, Vector<Integer> vector) {
            final NonEmptyVector<Integer> nev = nev(vector);
            final ArrayList<String> seen = new ArrayList<>();
            nev.forEachWithIndex((element, index) -> seen.add(element + "@" + index));
            assertThat(seen).isEqualTo(new ArrayList<>(vector.map(i -> i + "@" + i).asJava()));
            assertThat(nev.collect(java.util.stream.Collectors.toList())).isEqualTo(vector.collect(java.util.stream.Collectors.toList()));
            final ArrayList<Integer> collected = nev.collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
            assertThat(collected).isEqualTo(new ArrayList<>(vector.asJava()));
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyVectorTest#vectors")
        public void shouldConvertLikeVector(int n, Vector<Integer> vector) {
            final Vector<Integer> doubled = vector.appendAll(vector.reverse());
            final NonEmptyVector<Integer> nev = nev(doubled);
            assertThat(nev.toQueue()).isEqualTo(doubled.toQueue());
            assertThat(nev.toStream()).isEqualTo(doubled.toStream());
            assertThat((Object) nev.toLinkedSet()).isEqualTo(doubled.toLinkedSet());
            assertThat(nev.toLinkedSet().toVector()).isEqualTo(vector);
            assertThat((Object) nev.toSortedSet()).isEqualTo(doubled.toSortedSet());
            assertThat(nev.toSortedSet(Comparator.reverseOrder()).toVector()).isEqualTo(vector.reverse());
            assertThat(nev.toArray()).isEqualTo(doubled.toArray());
            assertThat(nev.toArray(Integer[]::new)).isEqualTo(doubled.toArray(Integer[]::new));
            assertThat((Object) nev.toMap(i -> i % 7, i -> i)).isEqualTo(doubled.toMap(i -> i % 7, i -> i));
            assertThat((Object) nev.toMap(i -> Tuple.of(i % 7, i))).isEqualTo(doubled.toMap(i -> Tuple.of(i % 7, i)));
            assertThat(nev.toLinkedMap(i -> i % 7, i -> i).toVector()).isEqualTo(doubled.toLinkedMap(i -> i % 7, i -> i).toVector());
            assertThat(nev.toLinkedMap(i -> Tuple.of(i % 7, i)).toVector()).isEqualTo(doubled.toLinkedMap(i -> Tuple.of(i % 7, i)).toVector());
            assertThat(nev.toSortedMap(i -> i % 7, i -> i).toVector()).isEqualTo(doubled.toSortedMap(i -> i % 7, i -> i).toVector());
            assertThat(nev.toSortedMap(i -> Tuple.of(i % 7, i)).toVector()).isEqualTo(doubled.toSortedMap(i -> Tuple.of(i % 7, i)).toVector());
            final Comparator<Integer> reverse = Comparator.reverseOrder();
            assertThat(nev.toSortedMap(reverse, i -> i % 7, i -> i).toVector()).isEqualTo(doubled.toSortedMap(reverse, i -> i % 7, i -> i).toVector());
            assertThat(nev.toSortedMap(reverse, i -> Tuple.of(i % 7, i)).toVector()).isEqualTo(doubled.toSortedMap(reverse, i -> Tuple.of(i % 7, i)).toVector());
            assertThat(nev.toMap(i -> i, i -> i).size()).isEqualTo(n);
        }
    }

    @Nested
    class ReturnsOption {

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyVectorTest#vectors")
        public void shouldFind(int n, Vector<Integer> vector) {
            final NonEmptyVector<Integer> nev = nev(vector);
            assertThat(nev.find(i -> i % 2 == 0)).isEqualTo(vector.find(i -> i % 2 == 0)).isEqualTo(Option.some(0));
            assertThat(nev.find(i -> i >= n)).isEqualTo(Option.none());
            assertThat(nev.findLast(i -> i % 2 == 0)).isEqualTo(vector.findLast(i -> i % 2 == 0));
            assertThat(nev.findLast(i -> i >= n)).isEqualTo(Option.none());
            assertThat(nev.indexOfOption(n - 1)).isEqualTo(Option.some(n - 1));
            assertThat(nev.indexOfOption(n)).isEqualTo(Option.none());
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyVectorTest#vectors")
        public void shouldNarrowTailAndInit(int n, Vector<Integer> vector) {
            final NonEmptyVector<Integer> nev = nev(vector);
            if (n == 1) {
                assertThat(nev.tailNonEmpty()).isEqualTo(Option.none());
                assertThat(nev.initNonEmpty()).isEqualTo(Option.none());
            } else {
                assertThat(nev.tailNonEmpty().get().toVector()).isEqualTo(vector.tail());
                assertThat(nev.tailNonEmpty().get().size()).isEqualTo(n - 1);
                assertThat(nev.initNonEmpty().get().toVector()).isEqualTo(vector.init());
                assertThat(nev.initNonEmpty().get().size()).isEqualTo(n - 1);
            }
        }
    }

    @Nested
    class ReturnsOptionLikeVector {

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyVectorTest#vectors")
        public void shouldFindIndicesAsOptions(int n, Vector<Integer> vector) {
            final Vector<Integer> doubled = vector.appendAll(vector);
            final NonEmptyVector<Integer> nev = nev(doubled);
            for (int element : new int[] { 0, n - 1, n }) {
                assertThat(nev.indexWhereOption(i -> i == element)).isEqualTo(doubled.indexWhereOption(i -> i == element));
                assertThat(nev.lastIndexOfOption(element)).isEqualTo(doubled.lastIndexOfOption(element));
                assertThat(nev.lastIndexWhereOption(i -> i == element)).isEqualTo(doubled.lastIndexWhereOption(i -> i == element));
                for (int position : new int[] { -1, 0, 1, n - 1, n, 2 * n }) {
                    assertThat(nev.indexOfOption(element, position)).isEqualTo(doubled.indexOfOption(element, position));
                    assertThat(nev.indexWhereOption(i -> i == element, position)).isEqualTo(doubled.indexWhereOption(i -> i == element, position));
                    assertThat(nev.lastIndexOfOption(element, position)).isEqualTo(doubled.lastIndexOfOption(element, position));
                    assertThat(nev.lastIndexWhereOption(i -> i == element, position)).isEqualTo(doubled.lastIndexWhereOption(i -> i == element, position));
                }
            }
            for (Vector<Integer> slice : java.util.List.of(Vector.<Integer> empty(), Vector.of(0), vector, Vector.of(n))) {
                assertThat(nev.indexOfSliceOption(slice)).isEqualTo(doubled.indexOfSliceOption(slice));
                assertThat(nev.lastIndexOfSliceOption(slice)).isEqualTo(doubled.lastIndexOfSliceOption(slice));
                for (int position : new int[] { -1, 0, 1, n, 2 * n }) {
                    assertThat(nev.indexOfSliceOption(slice, position)).isEqualTo(doubled.indexOfSliceOption(slice, position));
                    assertThat(nev.lastIndexOfSliceOption(slice, position)).isEqualTo(doubled.lastIndexOfSliceOption(slice, position));
                }
            }
            assertThat(nev.lastIndexOfOption(0)).isEqualTo(Option.some(n));
            assertThat(nev.indexWhereOption(i -> i < 0)).isEqualTo(Option.none());
            assertThat(nev.lastIndexOfSliceOption(vector)).isEqualTo(Option.some(n));
            assertThat(nev.indexOfSliceOption(Vector.of(n))).isEqualTo(Option.none());
        }
    }

    @Nested
    class NullsNamingTheType {

        @Test
        public void shouldRejectNullElementArguments() {
            final NonEmptyVector<Integer> nev = NonEmptyVector.single(1);
            assertThatNullPointerException().isThrownBy(() -> nev.as(null)).withMessage("NonEmptyVector.as: value is null");
            assertThatNullPointerException().isThrownBy(() -> nev.insert(0, null)).withMessage("NonEmptyVector.insert: element is null");
            // checked even where Vector would not need the element: one element has nothing to intersperse, the size is reached, nothing matches
            assertThatNullPointerException().isThrownBy(() -> nev.intersperse(null)).withMessage("NonEmptyVector.intersperse: element is null");
            assertThatNullPointerException().isThrownBy(() -> nev.padTo(0, null)).withMessage("NonEmptyVector.padTo: element is null");
            assertThatNullPointerException().isThrownBy(() -> nev.leftPadTo(0, null)).withMessage("NonEmptyVector.leftPadTo: element is null");
            assertThatNullPointerException().isThrownBy(() -> nev.replace(2, null)).withMessage("NonEmptyVector.replace: newElement is null");
            assertThatNullPointerException().isThrownBy(() -> nev.replaceAll(2, null)).withMessage("NonEmptyVector.replaceAll: newElement is null");
            assertThatNullPointerException().isThrownBy(() -> nev.zipAll(Vector.of(1), null, 0)).withMessage("NonEmptyVector.zipAll: thisElem is null");
            assertThatNullPointerException().isThrownBy(() -> nev.zipAll(Vector.of(1), 0, null)).withMessage("NonEmptyVector.zipAll: thatElem is null");
            assertThatNullPointerException().isThrownBy(() -> nev.scan(null, Integer::sum)).withMessage("NonEmptyVector.scan: zero is null");
            assertThatNullPointerException().isThrownBy(() -> nev.scanRight(null, Integer::sum)).withMessage("NonEmptyVector.scanRight: zero is null");
        }

        @Test
        public void shouldRejectNullElementsOfIterableArguments() {
            final NonEmptyVector<Integer> nev = NonEmptyVector.of(1, 2);
            final java.util.List<Integer> withNull = Arrays.asList(3, null);
            assertThatNullPointerException().isThrownBy(() -> nev.appendAll(withNull)).withMessage("NonEmptyVector: element is null");
            assertThatNullPointerException().isThrownBy(() -> nev.prependAll(withNull)).withMessage("NonEmptyVector: element is null");
            assertThatNullPointerException().isThrownBy(() -> nev.insertAll(1, withNull)).withMessage("NonEmptyVector: element is null");
            assertThatNullPointerException().isThrownBy(() -> nev.patch(1, withNull, 1)).withMessage("NonEmptyVector: element is null");
            assertThatNullPointerException().isThrownBy(() -> nev.zipAll(withNull, 0, 0)).withMessage("NonEmptyVector: element is null");
            assertThatNullPointerException().isThrownBy(() -> nev.zip(withNull)).withMessage("NonEmptyVector: element is null");
            assertThatNullPointerException().isThrownBy(() -> nev.zipWith(withNull, Integer::sum)).withMessage("NonEmptyVector: element is null");
            assertThatNullPointerException().isThrownBy(() -> nev.crossProduct(withNull)).withMessage("NonEmptyVector: element is null");
            assertThatNullPointerException().isThrownBy(() -> nev.appendAll((Iterable<Integer>) null)).withMessage("elements is null");
            assertThatNullPointerException().isThrownBy(() -> nev.prependAll((Iterable<Integer>) null)).withMessage("elements is null");
            assertThatNullPointerException().isThrownBy(() -> nev.insertAll(0, null)).withMessage("elements is null");
            assertThatNullPointerException().isThrownBy(() -> nev.patch(0, null, 0)).withMessage("that is null");
            assertThatNullPointerException().isThrownBy(() -> nev.zipAll(null, 0, 0)).withMessage("that is null");
            assertThatNullPointerException().isThrownBy(() -> nev.zip((Iterable<Integer>) null)).withMessage("that is null");
            assertThatNullPointerException().isThrownBy(() -> nev.crossProduct((Iterable<Integer>) null)).withMessage("that is null");
            assertThatNullPointerException().isThrownBy(() -> nev.crossProduct((NonEmptyVector<Integer>) null)).withMessage("that is null");
            assertThatNullPointerException().isThrownBy(() -> NonEmptyVector.transpose(null)).withMessage("matrix is null");
        }

        @Test
        public void shouldRejectNullResultsOfFunctions() {
            final NonEmptyVector<Integer> nev = NonEmptyVector.of(1, 2);
            assertThatNullPointerException().isThrownBy(() -> nev.scan(0, (a, b) -> null)).withMessage("NonEmptyVector.scan: operation returned null");
            assertThatNullPointerException().isThrownBy(() -> nev.scanRight(0, (a, b) -> null)).withMessage("NonEmptyVector.scanRight: operation returned null");
            assertThatNullPointerException().isThrownBy(() -> nev.zipWith(Vector.of(1), (a, b) -> null)).withMessage("NonEmptyVector.zipWith: mapper returned null");
            assertThatNullPointerException().isThrownBy(() -> nev.unzip(i -> null)).withMessage("NonEmptyVector.unzip: unzipper returned null");
            assertThatNullPointerException().isThrownBy(() -> nev.unzip(i -> Tuple.of(i, null))).withMessage("NonEmptyVector.unzip: unzipper returned a null component");
            assertThatNullPointerException().isThrownBy(() -> nev.unzip(i -> Tuple.of(null, i))).withMessage("NonEmptyVector.unzip: unzipper returned a null component");
            assertThatNullPointerException().isThrownBy(() -> nev.unzip3(i -> null)).withMessage("NonEmptyVector.unzip3: unzipper returned null");
            assertThatNullPointerException().isThrownBy(() -> nev.unzip3(i -> Tuple.of(i, i, null))).withMessage("NonEmptyVector.unzip3: unzipper returned a null component");
            assertThatNullPointerException().isThrownBy(() -> nev.arrangeBy(i -> null)).withMessage("NonEmptyVector.arrangeBy: getKey returned null");
            assertThatNullPointerException().isThrownBy(() -> nev.toMap(i -> null, i -> i)).withMessage("NonEmptyVector.toMap: keyMapper returned null");
            assertThatNullPointerException().isThrownBy(() -> nev.toMap(i -> i, i -> null)).withMessage("NonEmptyVector.toMap: valueMapper returned null");
            assertThatNullPointerException().isThrownBy(() -> nev.toMap(i -> null)).withMessage("NonEmptyVector.toMap: f returned null");
            assertThatNullPointerException().isThrownBy(() -> nev.toMap(i -> Tuple.of(null, i))).withMessage("NonEmptyVector.toMap: f returned an entry with a null key");
            assertThatNullPointerException().isThrownBy(() -> nev.toMap(i -> Tuple.of(i, null))).withMessage("NonEmptyVector.toMap: f returned an entry with a null value");
            assertThatNullPointerException().isThrownBy(() -> nev.toLinkedMap(i -> null, i -> i)).withMessage("NonEmptyVector.toLinkedMap: keyMapper returned null");
            assertThatNullPointerException().isThrownBy(() -> nev.toLinkedMap(i -> Tuple.of(i, null))).withMessage("NonEmptyVector.toLinkedMap: f returned an entry with a null value");
            assertThatNullPointerException().isThrownBy(() -> nev.toSortedMap(i -> (Integer) null, i -> i)).withMessage("NonEmptyVector.toSortedMap: keyMapper returned null");
            assertThatNullPointerException().isThrownBy(() -> nev.toSortedMap(i -> (Tuple2<Integer, Integer>) null)).withMessage("NonEmptyVector.toSortedMap: f returned null");
            assertThatNullPointerException().isThrownBy(() -> nev.toSortedMap(Comparator.<Integer> naturalOrder(), i -> i, i -> null)).withMessage("NonEmptyVector.toSortedMap: valueMapper returned null");
            assertThatNullPointerException().isThrownBy(() -> nev.toSortedMap(Comparator.<Integer> naturalOrder(), i -> Tuple.of(null, i))).withMessage("NonEmptyVector.toSortedMap: f returned an entry with a null key");
        }

        @Test
        public void shouldRejectNullFunctions() {
            final NonEmptyVector<Integer> nev = NonEmptyVector.of(1, 2);
            assertThatNullPointerException().isThrownBy(() -> nev.scan(0, null));
            assertThatNullPointerException().isThrownBy(() -> nev.scanRight(0, null));
            assertThatNullPointerException().isThrownBy(() -> nev.unzip(null));
            assertThatNullPointerException().isThrownBy(() -> nev.unzip3(null));
            assertThatNullPointerException().isThrownBy(() -> nev.zipWith(Vector.of(1), null));
            assertThatNullPointerException().isThrownBy(() -> nev.arrangeBy(null));
            assertThatNullPointerException().isThrownBy(() -> nev.toMap(null, i -> i));
            assertThatNullPointerException().isThrownBy(() -> nev.toMap(i -> i, null));
            assertThatNullPointerException().isThrownBy(() -> nev.toMap(null));
            assertThatNullPointerException().isThrownBy(() -> nev.toSortedMap((Comparator<Integer>) null, i -> Tuple.of(i, i)));
            assertThatNullPointerException().isThrownBy(() -> nev.slideBy(null));
            assertThatNullPointerException().isThrownBy(() -> nev.removeFirst(null));
            assertThatNullPointerException().isThrownBy(() -> nev.removeLast(null));
            assertThatNullPointerException().isThrownBy(() -> nev.splitAtInclusive(null));
            assertThatNullPointerException().isThrownBy(() -> nev.partition(null));
            assertThatNullPointerException().isThrownBy(() -> nev.forEachWithIndex(null));
            assertThatNullPointerException().isThrownBy(() -> nev.distinctByKeepLast((Comparator<Integer>) null));
        }
    }

    /**
     * Every method returning a {@code NonEmptyVector}, called on the inputs most likely to empty it. The wrapper is built
     * without a check on the internal paths, so this is what stands between a bug and an empty {@code NonEmptyVector}; the
     * last test makes sure no such method is left out.
     */
    @Nested
    class NonEmptyGuarantee {

        /* name -> a call of every overload of that name, with arguments chosen to shrink the result as far as they can */
        static java.util.Map<String, Function<NonEmptyVector<Integer>, java.util.List<NonEmptyVector<?>>>> calls() {
            final java.util.Map<String, Function<NonEmptyVector<Integer>, java.util.List<NonEmptyVector<?>>>> calls = new java.util.HashMap<>();
            calls.put("map", v -> java.util.List.of(v.map(i -> i)));
            calls.put("flatMap", v -> java.util.List.of(v.flatMap(NonEmptyVector::single)));
            calls.put("as", v -> java.util.List.of(v.as("x")));
            calls.put("append", v -> java.util.List.of(v.append(0)));
            calls.put("appendAll", v -> java.util.List.of(v.appendAll(Vector.<Integer> empty()), v.appendAll(java.util.List.<Integer> of()), v.appendAll(v)));
            calls.put("prepend", v -> java.util.List.of(v.prepend(0)));
            calls.put("prependAll", v -> java.util.List.of(v.prependAll(Vector.<Integer> empty()), v.prependAll(java.util.List.<Integer> of()), v.prependAll(v)));
            calls.put("concat", v -> java.util.List.of(v.concat(v)));
            calls.put("insert", v -> java.util.List.of(v.insert(0, 0), v.insert(v.size(), 0)));
            calls.put("insertAll", v -> java.util.List.of(v.insertAll(0, Vector.<Integer> empty()), v.insertAll(v.size(), java.util.List.<Integer> of())));
            calls.put("intersperse", v -> java.util.List.of(v.intersperse(0)));
            calls.put("padTo", v -> java.util.List.of(v.padTo(Integer.MIN_VALUE, 0), v.padTo(0, 0)));
            calls.put("leftPadTo", v -> java.util.List.of(v.leftPadTo(Integer.MIN_VALUE, 0), v.leftPadTo(0, 0)));
            calls.put("reverse", v -> java.util.List.of(v.reverse()));
            calls.put("rotateLeft", v -> java.util.List.of(v.rotateLeft(Integer.MIN_VALUE), v.rotateLeft(v.size()), v.rotateLeft(1)));
            calls.put("rotateRight", v -> java.util.List.of(v.rotateRight(Integer.MIN_VALUE), v.rotateRight(v.size()), v.rotateRight(1)));
            calls.put("shuffle", v -> java.util.List.of(v.shuffle()));
            calls.put("replace", v -> java.util.List.of(v.replace(v.head(), -1), v.replace(-2, -1)));
            calls.put("replaceAll", v -> java.util.List.of(v.replaceAll(v.head(), -1), v.replaceAll(-2, -1)));
            calls.put("distinct", v -> java.util.List.of(v.distinct(), v.as(0).distinct()));
            calls.put("distinctBy", v -> java.util.List.of(v.distinctBy(i -> 0), v.distinctBy((a, b) -> 0)));
            calls.put("distinctByKeepLast", v -> java.util.List.of(v.distinctByKeepLast(i -> 0), v.distinctByKeepLast((a, b) -> 0)));
            calls.put("sorted", v -> java.util.List.of(v.sorted(), v.sorted(Comparator.reverseOrder())));
            calls.put("sortBy", v -> java.util.List.of(v.sortBy(i -> -i), v.sortBy(Comparator.reverseOrder(), i -> i)));
            calls.put("zip", v -> java.util.List.of(v.zip(NonEmptyVector.single(0))));
            calls.put("zipWith", v -> java.util.List.of(v.zipWith(NonEmptyVector.single(0), Integer::sum)));
            calls.put("zipAll", v -> java.util.List.of(v.zipAll(Vector.<Integer> empty(), 0, 0), v.zipAll(java.util.List.<Integer> of(), 0, 0)));
            calls.put("zipWithIndex", v -> java.util.List.of(v.zipWithIndex(), v.zipWithIndex(Integer::sum)));
            calls.put("scan", v -> java.util.List.of(v.scan(0, Integer::sum)));
            calls.put("scanLeft", v -> java.util.List.of(v.scanLeft(0, Integer::sum)));
            calls.put("scanRight", v -> java.util.List.of(v.scanRight(0, Integer::sum)));
            calls.put("update", v -> java.util.List.of(v.update(0, -1), v.update(v.size() - 1, i -> -i)));
            calls.put("tap", v -> java.util.List.of(v.tap(i -> { })));
            calls.put("permutations", v -> java.util.List.of(v.take(4).toNonEmptyVector().get().permutations()));
            calls.put("combinations", v -> java.util.List.of(v.take(4).toNonEmptyVector().get().combinations()));
            calls.put("crossProduct", v -> java.util.List.of(v.crossProduct(), v.crossProduct(NonEmptyVector.single(0))));
            calls.put("transpose", v -> java.util.List.of(NonEmptyVector.transpose(NonEmptyVector.single(v)), NonEmptyVector.transpose(v.map(NonEmptyVector::single))));
            calls.put("flatten", v -> java.util.List.of(NonEmptyVector.flatten(NonEmptyVector.single(v))));
            return calls;
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyVectorTest#vectors")
        public void shouldReturnAtLeastOneElementFromEveryNonEmptyVectorMethod(int n, Vector<Integer> vector) {
            for (var call : calls().entrySet()) {
                for (NonEmptyVector<?> result : call.getValue().apply(nev(vector))) {
                    assertThat(result.toVector().isEmpty()).as(call.getKey()).isFalse();
                    if (result.head() instanceof NonEmptyVector<?> inner) {
                        assertThat(inner.toVector().isEmpty()).as(call.getKey() + " inner").isFalse();
                    }
                }
            }
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyVectorTest#vectors")
        public void shouldReturnNonEmptyPartsAndGroups(int n, Vector<Integer> vector) {
            final NonEmptyVector<Integer> nev = nev(vector);
            nonEmpty(nev.unzip(i -> Tuple.of(i, i))._1());
            nonEmpty(nev.unzip(i -> Tuple.of(i, i))._2());
            nonEmpty(nev.unzip3(i -> Tuple.of(i, i, i))._3());
            nonEmpty(nev.splitAtInclusive(i -> true)._1());
            nonEmpty(nev.splitAtInclusive(i -> false)._1());
            for (int size : new int[] { 1, Integer.MAX_VALUE }) {
                nev.grouped(size).forEach(NonEmptyVectorTest::nonEmpty);
                nev.sliding(size).forEach(NonEmptyVectorTest::nonEmpty);
                nev.sliding(size, Integer.MAX_VALUE).forEach(NonEmptyVectorTest::nonEmpty);
                assertThat(nev.grouped(size).isEmpty()).isFalse();
                assertThat(nev.sliding(size, Integer.MAX_VALUE).isEmpty()).isFalse();
            }
            nev.slideBy(i -> 0).forEach(NonEmptyVectorTest::nonEmpty);
            nev.slideBy(i -> i).forEach(NonEmptyVectorTest::nonEmpty);
            nev.groupBy(i -> 0).forEach(group -> nonEmpty(group._2()));
            nev.tailNonEmpty().forEach(NonEmptyVectorTest::nonEmpty);
            nev.initNonEmpty().forEach(NonEmptyVectorTest::nonEmpty);
        }

        @Test
        public void shouldCoverEveryMethodReturningANonEmptyVector() {
            final java.util.Set<String> returning = new java.util.TreeSet<>();
            for (java.lang.reflect.Method method : NonEmptyVector.class.getMethods()) {
                if (method.getReturnType() == NonEmptyVector.class && !method.getName().startsWith("of") && !method.getName().startsWith("single")
                        && !method.getName().startsWith("unsafe") && !method.getName().equals("fromIterable")) {
                    returning.add(method.getName());
                }
            }
            assertThat(calls().keySet()).containsAll(returning);
        }
    }

    /**
     * {@code NonEmptyVector} has every operation of {@code Vector}, under the name {@code Vector} gives it, except the
     * ones listed here on purpose. The list is exact: a name missing from {@code NonEmptyVector} and not listed fails,
     * and so does a listed name that {@code Vector} lost or {@code NonEmptyVector} gained.
     */
    @Nested
    class SameApiAsVector {

        static final java.util.Set<String> DELIBERATELY_ABSENT = java.util.Set.of(
                // the Option forms of what is total on a non-empty vector
                "headOption", "lastOption", "reduceOption", "reduceLeftOption", "reduceRightOption", "singleOption",
                // tail and init already return a Vector
                "tailOption", "initOption",
                // constant on a non-empty vector: false, true, this, Some(this)
                "isEmpty", "nonEmpty", "orElse", "toNonEmptyVector",
                // size is the one spelling; Vector's length is to go as well, taking this entry with it
                "length"
        );

        static java.util.Set<String> publicInstanceMethodNames(Class<?> type) {
            final java.util.Set<String> names = new java.util.TreeSet<>();
            for (java.lang.reflect.Method method : type.getMethods()) {
                if (!java.lang.reflect.Modifier.isStatic(method.getModifiers()) && !method.isSynthetic() && !method.isBridge()
                        && method.getDeclaringClass() != Object.class) {
                    names.add(method.getName());
                }
            }
            return names;
        }

        @Test
        public void shouldHaveEveryVectorMethodButTheDeliberateAbsences() {
            final java.util.Set<String> missing = new java.util.TreeSet<>(publicInstanceMethodNames(Vector.class));
            missing.removeAll(publicInstanceMethodNames(NonEmptyVector.class));
            assertThat(missing).containsExactlyInAnyOrderElementsOf(DELIBERATELY_ABSENT);
        }

        @Test
        public void shouldHaveTheStaticTransposeAndFlatten() throws NoSuchMethodException {
            assertThat(java.lang.reflect.Modifier.isStatic(NonEmptyVector.class.getMethod("transpose", NonEmptyVector.class).getModifiers())).isTrue();
            assertThat(java.lang.reflect.Modifier.isStatic(NonEmptyVector.class.getMethod("flatten", NonEmptyVector.class).getModifiers())).isTrue();
        }
    }

    @Nested
    class Flatten {

        @ParameterizedTest
        @ValueSource(ints = { 1, 2, 32, 33, 1023, 1024, 1025 })
        public void shouldFlattenNestedNonEmptyVectors(int n) {
            final NonEmptyVector<NonEmptyVector<Integer>> nested = nev(Vector.range(0, n)).map(i -> nev(Vector.range(i, i + n)));
            final NonEmptyVector<Integer> flat = NonEmptyVector.flatten(nested);
            assertThat(flat.toVector()).isEqualTo(Vector.flatten(nested.toVector().map(NonEmptyVector::toVector)));
            assertThat(flat.size()).isEqualTo(n * n);
            assertThat(NonEmptyVector.flatten(NonEmptyVector.single(NonEmptyVector.single(1))).toVector()).isEqualTo(Vector.of(1));
        }

        @Test
        public void shouldFlattenCovariantly() {
            final NonEmptyVector<NonEmptyVector<Integer>> nested = NonEmptyVector.of(NonEmptyVector.of(1), NonEmptyVector.of(2, 3));
            final NonEmptyVector<Number> flat = NonEmptyVector.flatten(nested);
            assertThat(flat.toVector()).isEqualTo(Vector.<Number> of(1, 2, 3));
        }
    }

    @Nested
    class ObjectMethods {

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyVectorTest#vectors")
        public void shouldBeEqualToAnotherNonEmptyVectorWithTheSameElements(int n, Vector<Integer> vector) {
            final NonEmptyVector<Integer> nev = nev(vector);
            final NonEmptyVector<Integer> copy = NonEmptyVector.fromIterable(new java.util.ArrayList<>(vector.asJava())).get();
            assertThat(nev).isEqualTo(nev);
            assertThat(nev).isEqualTo(copy);
            assertThat(nev.hashCode()).isEqualTo(copy.hashCode());
            assertThat(nev).isNotEqualTo(nev.append(n));
            if (n > 1) {
                assertThat(nev).isNotEqualTo(nev.reverse());
            }
            assertThat(nev).isNotEqualTo(vector);
            assertThat(vector).isNotEqualTo(nev);
            assertThat(nev).isNotEqualTo(null);
            assertThat(nev).isNotEqualTo(vector.toList());
        }

        @Test
        public void shouldStringify() {
            assertThat(NonEmptyVector.of(1).toString()).isEqualTo("NonEmptyVector(1)");
            assertThat(NonEmptyVector.of(1, 2).toString()).isEqualTo("NonEmptyVector(1, 2)");
            assertThat(nev(Vector.range(0, 33)).toString()).isEqualTo(Vector.range(0, 33).mkString("NonEmptyVector(", ", ", ")"));
        }
    }
}
