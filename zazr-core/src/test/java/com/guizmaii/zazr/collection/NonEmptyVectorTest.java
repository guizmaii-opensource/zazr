package com.guizmaii.zazr.collection;

import com.guizmaii.zazr.Tuple;
import com.guizmaii.zazr.Tuple2;
import com.guizmaii.zazr.control.Either;
import com.guizmaii.zazr.control.Option;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Function;
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
 * Every method is checked at sizes 1, 2, 32 and 33 (the leaf boundary) against the equivalent {@link Vector} call, on both
 * representations a Vector can have (primitive {@code int[]} leaves from {@code Vector.range}, {@code Object[]} leaves from
 * {@code Vector.ofAll}).
 */
public class NonEmptyVectorTest {

    static final int[] SIZES = { 1, 2, 32, 33 };

    /* (size, vector) for both leaf representations */
    static Stream<Arguments> vectors() {
        final ArrayList<Arguments> cases = new ArrayList<>();
        for (int n : SIZES) {
            final Vector<Integer> primitive = Vector.range(0, n);
            cases.add(Arguments.of(n, primitive));
            cases.add(Arguments.of(n, Vector.ofAll(primitive.toJavaList())));
        }
        return cases.stream();
    }

    static <A> NonEmptyVector<A> nev(Vector<A> vector) {
        return NonEmptyVector.unsafeFromVector(vector);
    }

    @Nested
    class Constructors {

        @Test
        public void shouldBuildOfHeadAndVarargsTail() {
            assertThat(NonEmptyVector.of(1).toVector()).isEqualTo(Vector.of(1));
            assertThat(NonEmptyVector.of(1, 2).toVector()).isEqualTo(Vector.of(1, 2));
            for (int n : SIZES) {
                final Integer[] tail = Vector.range(1, n).toJavaArray(Integer[]::new);
                assertThat(NonEmptyVector.of(0, tail).toVector()).isEqualTo(Vector.range(0, n));
                assertThat(NonEmptyVector.of(0, tail).size()).isEqualTo(n);
            }
        }

        @Test
        public void shouldBuildFromHeadAndIterableTail() {
            for (int n : SIZES) {
                final Vector<Integer> expected = Vector.range(0, n);
                assertThat(NonEmptyVector.fromIterable(0, Vector.range(1, n)).toVector()).isEqualTo(expected);
                assertThat(NonEmptyVector.fromIterable(0, Vector.range(1, n).toJavaList()).toVector()).isEqualTo(expected);
                assertThat(NonEmptyVector.fromIterable(0, Vector.range(1, n).iterator()).toVector()).isEqualTo(expected);
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
                assertThat(vector.nonEmpty().get().toVector()).isSameAs(vector);
                assertThat(NonEmptyVector.fromIterable(vector).get().toVector()).isSameAs(vector);
            }
        }

        @Test
        public void shouldReturnNoneFromEmptyVector() {
            assertThat(NonEmptyVector.fromVector(Vector.<Integer> empty())).isEqualTo(Option.none());
            assertThat(Vector.<Integer> empty().nonEmpty()).isEqualTo(Option.none());
            assertThat(NonEmptyVector.fromIterable(Vector.<Integer> empty())).isEqualTo(Option.none());
            assertThat(NonEmptyVector.fromIterable(java.util.List.<Integer> of())).isEqualTo(Option.none());
            assertThat(NonEmptyVector.fromIterable(List.<Integer> empty())).isEqualTo(Option.none());
        }

        @Test
        public void shouldCopyANonEmptyIterable() {
            for (int n : SIZES) {
                final Vector<Integer> expected = Vector.range(0, n);
                assertThat(NonEmptyVector.fromIterable(expected.toJavaList()).get().toVector()).isEqualTo(expected);
                assertThat(NonEmptyVector.fromIterable(expected.iterator()).get().toVector()).isEqualTo(expected);
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
            assertThatNullPointerException().isThrownBy(() -> nev.maxBy(null));
            assertThatNullPointerException().isThrownBy(() -> nev.minBy(null));
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
            assertThat(seen).isEqualTo(vector.toJavaList());
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
            assertThat(nev.stream().toList()).isEqualTo(vector.toJavaList());
            assertThat(nev.asJava()).isEqualTo(vector.asJava());
            assertThat(nev.asJava().size()).isEqualTo(n);
            assertThatThrownBy(() -> nev.asJava().add(1)).isInstanceOf(UnsupportedOperationException.class);
            assertThat(nev.toList()).isEqualTo(vector.toList());
            assertThat((Object) nev.toSet()).isEqualTo(vector.toSet());
            assertThat(nev.toSet().size()).isEqualTo(n);
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
    class Flatten {

        @ParameterizedTest
        @ValueSource(ints = { 1, 2, 32, 33 })
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
            final NonEmptyVector<Integer> copy = NonEmptyVector.fromIterable(vector.toJavaList()).get();
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
