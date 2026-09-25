package com.guizmaii.zazr.collection;

import com.guizmaii.zazr.Tuple;
import com.guizmaii.zazr.control.Option;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Spliterator;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Every method is checked at sizes 1, 2, 31, 32, 33, 1023, 1024 and 1025 against the equivalent {@link TreeSet} call,
 * in the natural order and in the reverse order, where the comparator's order and the natural order of the elements
 * disagree.
 */
public class NonEmptySortedSetTest {

    static final int[] SIZES = { 1, 2, 31, 32, 33, 1023, 1024, 1025 };

    /* (size, set) in the natural and the reverse order */
    static Stream<Arguments> sets() {
        final ArrayList<Arguments> cases = new ArrayList<>();
        for (int n : SIZES) {
            cases.add(Arguments.of(n, TreeSet.range(0, n)));
            cases.add(Arguments.of(n, TreeSet.ofAll(Comparator.reverseOrder(), Vector.range(0, n))));
        }
        return cases.stream();
    }

    static <A> NonEmptySortedSet<A> ness(TreeSet<A> set) {
        return NonEmptySortedSet.unsafeFromSortedSet(set);
    }

    static <A> Iterable<A> once(Iterable<A> elements) {
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
        public void shouldBuildInNaturalOrderOrByAComparator() {
            assertThat(NonEmptySortedSet.of(3, 1, 2, 1).toSortedSet()).isEqualTo(TreeSet.of(1, 2, 3));
            assertThat(NonEmptySortedSet.of(3, 1, 2).head()).isEqualTo(1);
            assertThat(NonEmptySortedSet.of(Comparator.<Integer> reverseOrder(), 3, 1, 2).head()).isEqualTo(3);
            assertThat(NonEmptySortedSet.single(1).toSortedSet()).isEqualTo(TreeSet.of(1));
            assertThat(NonEmptySortedSet.single(Comparator.<Integer> reverseOrder(), 1).comparator().compare(1, 2)).isPositive();
            for (int n : SIZES) {
                final TreeSet<Integer> expected = TreeSet.range(0, n);
                final Vector<Integer> shuffled = Vector.range(0, n).reverse();
                assertThat(NonEmptySortedSet.of(0, shuffled.toArray(Integer[]::new)).toSortedSet()).isEqualTo(expected);
                assertThat(NonEmptySortedSet.fromIterable(0, once(shuffled)).toSortedSet()).isEqualTo(expected);
                assertThat(NonEmptySortedSet.fromIterable(0, TreeSet.range(0, n)).toSortedSet()).isEqualTo(expected);
                assertThat(NonEmptySortedSet.fromIterable(Comparator.reverseOrder(), 0, once(shuffled)).head()).isEqualTo(n - 1);
                assertThat(NonEmptySortedSet.fromIterable(once(shuffled)).get().toSortedSet()).isEqualTo(expected);
                assertThat(NonEmptySortedSet.fromIterable(Comparator.<Integer> reverseOrder(), once(shuffled)).get().last()).isEqualTo(0);
                // a TreeSet argument in another order is re-sorted, not adopted
                assertThat(NonEmptySortedSet.fromIterable(Comparator.<Integer> reverseOrder(), TreeSet.range(0, n)).get().head()).isEqualTo(n - 1);
                assertThat(NonEmptySortedSet.fromIterable(Comparator.reverseOrder(), 0, TreeSet.range(0, n)).head()).isEqualTo(n - 1);
                assertThat(NonEmptySortedSet.fromIterable(0, TreeSet.ofAll(Comparator.reverseOrder(), shuffled)).head()).isEqualTo(0);
            }
            assertThat(NonEmptySortedSet.fromIterable(0, List.<Integer> empty()).toSortedSet()).isEqualTo(TreeSet.of(0));
        }

        @Test
        public void shouldWrapWithoutCopyingAndNarrow() {
            for (int n : SIZES) {
                final TreeSet<Integer> set = TreeSet.ofAll(Comparator.reverseOrder(), Vector.range(0, n));
                assertThat(NonEmptySortedSet.fromSortedSet(set).get().toSortedSet()).isSameAs(set);
                assertThat(NonEmptySortedSet.unsafeFromSortedSet(set).toSortedSet()).isSameAs(set);
                assertThat(set.toNonEmptySortedSet().get().toSortedSet()).isSameAs(set);
                assertThat(set.toNonEmptySortedSet().get().head()).isEqualTo(n - 1);
            }
            assertThat(NonEmptySortedSet.fromSortedSet(TreeSet.<Integer> empty())).isEqualTo(Option.none());
            assertThat(TreeSet.<Integer> empty().toNonEmptySortedSet()).isEqualTo(Option.none());
            assertThat(NonEmptySortedSet.fromIterable(once(List.<Integer> empty()))).isEqualTo(Option.none());
            assertThat(NonEmptySortedSet.fromIterable(Comparator.<Integer> naturalOrder(), List.<Integer> empty())).isEqualTo(Option.none());
            assertThatIllegalArgumentException()
              .isThrownBy(() -> NonEmptySortedSet.unsafeFromSortedSet(TreeSet.<Integer> empty()))
              .withMessage("NonEmptySortedSet.unsafeFromSortedSet: set is empty");
        }

        @Test
        public void shouldRejectNullsNamingTheType() {
            assertThatNullPointerException().isThrownBy(() -> NonEmptySortedSet.of((Integer) null)).withMessage("NonEmptySortedSet: head is null");
            assertThatNullPointerException().isThrownBy(() -> NonEmptySortedSet.of(1, (Integer[]) null)).withMessage("NonEmptySortedSet: tail is null");
            assertThatNullPointerException().isThrownBy(() -> NonEmptySortedSet.of(1, 2, null)).withMessage("NonEmptySortedSet: element is null");
            assertThatNullPointerException().isThrownBy(() -> NonEmptySortedSet.of((Comparator<Integer>) null, 1)).withMessage("comparator is null");
            assertThatNullPointerException().isThrownBy(() -> NonEmptySortedSet.fromIterable((Integer) null, List.of(1))).withMessage("NonEmptySortedSet: head is null");
            assertThatNullPointerException().isThrownBy(() -> NonEmptySortedSet.fromIterable(1, (Iterable<Integer>) null)).withMessage("NonEmptySortedSet: tail is null");
            assertThatNullPointerException().isThrownBy(() -> NonEmptySortedSet.fromIterable(1, Arrays.asList(2, null))).withMessage("NonEmptySortedSet: element is null");
            assertThatNullPointerException().isThrownBy(() -> NonEmptySortedSet.single((Integer) null)).withMessage("NonEmptySortedSet: element is null");
            assertThatNullPointerException().isThrownBy(() -> NonEmptySortedSet.fromSortedSet(null)).withMessage("NonEmptySortedSet.fromSortedSet: set is null");
            assertThatNullPointerException().isThrownBy(() -> NonEmptySortedSet.fromIterable((Iterable<Integer>) null)).withMessage("NonEmptySortedSet.fromIterable: iterable is null");
            assertThatNullPointerException().isThrownBy(() -> NonEmptySortedSet.fromIterable(Arrays.asList(1, null))).withMessage("NonEmptySortedSet: element is null");
            assertThatNullPointerException().isThrownBy(() -> NonEmptySortedSet.unsafeFromSortedSet(null)).withMessage("NonEmptySortedSet.unsafeFromSortedSet: set is null");
            final NonEmptySortedSet<Integer> ness = NonEmptySortedSet.of(1, 2);
            assertThatNullPointerException().isThrownBy(() -> ness.add(null)).withMessage("NonEmptySortedSet.add: element is null");
            assertThatNullPointerException().isThrownBy(() -> ness.replace(null, 1)).withMessage("NonEmptySortedSet.replace: currentElement is null");
            assertThatNullPointerException().isThrownBy(() -> ness.replace(3, null)).withMessage("NonEmptySortedSet.replace: newElement is null");
            assertThatNullPointerException().isThrownBy(() -> ness.as(null)).withMessage("NonEmptySortedSet.as: value is null");
            assertThatNullPointerException().isThrownBy(() -> ness.flatMap(i -> null)).withMessage("NonEmptySortedSet.flatMap: mapper returned null");
            assertThatNullPointerException().isThrownBy(() -> ness.flatMap(null, i -> NonEmptySortedSet.of(i))).withMessage("comparator is null");
            assertThatNullPointerException().isThrownBy(() -> ness.addAll(Arrays.asList(3, null)));
            assertThatNullPointerException().isThrownBy(() -> ness.map(i -> null));
            assertThatNullPointerException().isThrownBy(() -> ness.maxBy((Comparator<Integer>) null));
            assertThatNullPointerException().isThrownBy(() -> ness.minBy((Function<Integer, Integer>) null));
            assertThatNullPointerException().isThrownBy(() -> ness.reduce(null));
            assertThatNullPointerException().isThrownBy(() -> ness.slideBy(null));
        }
    }

    @Nested
    class ReturnsNonEmptySortedSet {

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptySortedSetTest#sets")
        public void shouldAddUnionAndKeepTheComparator(int n, TreeSet<Integer> set) {
            final NonEmptySortedSet<Integer> ness = ness(set);
            assertThat(ness.add(-1).toSortedSet()).isEqualTo(set.add(-1));
            assertThat(ness.add(-1).comparator()).isSameAs(set.comparator());
            assertThat(ness.add(0)).isSameAs(ness);
            assertThat(ness.toSortedSet()).isSameAs(set);
            for (int m : new int[] { 0, 1, 33, 1025 }) {
                final TreeSet<Integer> that = TreeSet.range(n / 2, n / 2 + m);
                assertThat(ness.addAll(once(that)).toSortedSet()).isEqualTo(set.addAll(that));
                assertThat(ness.union(that).toSortedSet()).isEqualTo(set.union(that));
                assertThat(ness.union(that.toSet()).head()).isEqualTo(set.union(that).head());
            }
            assertThat(ness.addAll(List.empty())).isSameAs(ness);
            assertThat(ness.union(HashSet.empty())).isSameAs(ness);
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptySortedSetTest#sets")
        public void shouldMapAndFlatMap(int n, TreeSet<Integer> set) {
            final NonEmptySortedSet<Integer> ness = ness(set);
            assertThat(ness.map(i -> i % 3).toSortedSet()).isEqualTo(set.map(i -> i % 3));
            assertThat(ness.map(i -> 0).size()).isEqualTo(1);
            assertThat(ness.map(Comparator.reverseOrder(), i -> i * 2).toSortedSet()).isEqualTo(set.map(Comparator.reverseOrder(), i -> i * 2));
            assertThat(ness.map(Comparator.reverseOrder(), i -> i * 2).head()).isEqualTo(2 * (n - 1));
            assertThat(ness.flatMap(i -> NonEmptySortedSet.of(i, -i - 1)).toSortedSet()).isEqualTo(set.flatMap(i -> List.of(i, -i - 1)));
            assertThat(ness.flatMap(Comparator.reverseOrder(), i -> NonEmptySortedSet.of(i, -i - 1)).toSortedSet())
              .isEqualTo(set.flatMap(Comparator.reverseOrder(), i -> List.of(i, -i - 1)));
            assertThat(ness.flatMap(i -> NonEmptySortedSet.of(0)).size()).isEqualTo(1);
            assertThat(ness.flatMap(Comparator.reverseOrder(), i -> NonEmptySortedSet.of(i, -i - 1)).head()).isEqualTo(n - 1);
            assertThat(ness.flatMap(Comparator.reverseOrder(), i -> NonEmptySortedSet.of(i, -i - 1)).comparator().compare(0, 1)).isPositive();
            assertThat(ness.flatMap(i -> NonEmptySortedSet.of(Comparator.<Integer> reverseOrder(), i, -i - 1)).head()).isEqualTo(-n);
            assertThat(ness.as("x").toSortedSet()).isEqualTo(TreeSet.of("x"));
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptySortedSetTest#sets")
        public void shouldReplaceAndTap(int n, TreeSet<Integer> set) {
            final NonEmptySortedSet<Integer> ness = ness(set);
            assertThat(ness.replace(0, -1).toSortedSet()).isEqualTo(set.replace(0, -1));
            assertThat(ness.replaceAll(0, -1).toSortedSet()).isEqualTo(set.replaceAll(0, -1));
            assertThat(ness.replace(0, n - 1).size()).isEqualTo(Math.max(1, n - 1));
            final java.util.List<Integer> seen = new java.util.ArrayList<>();
            assertThat(ness.tap(seen::add)).isSameAs(ness);
            assertThat(seen).containsExactlyElementsOf(set);
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptySortedSetTest#sets")
        public void shouldGroupSlideAndZipIntoNonEmptyResults(int n, TreeSet<Integer> set) {
            final NonEmptySortedSet<Integer> ness = ness(set);
            for (int size : new int[] { 1, 2, 32, 33, 2000 }) {
                assertThat(ness.grouped(size).map(NonEmptySortedSet::toSortedSet)).isEqualTo(set.grouped(size));
                assertThat(ness.sliding(size).map(NonEmptySortedSet::toSortedSet)).isEqualTo(set.sliding(size));
                assertThat(ness.sliding(size, 3).map(NonEmptySortedSet::toSortedSet)).isEqualTo(set.sliding(size, 3));
            }
            assertThat(ness.slideBy(i -> i / 10).map(NonEmptySortedSet::toSortedSet)).isEqualTo(set.slideBy(i -> i / 10));
            assertThat(ness.zipWithIndex().toVector()).isEqualTo(set.zipWithIndex());
            assertThat(ness.zipWithIndex().head()).isEqualTo(Tuple.of(set.head(), 0));
            for (Function<Integer, Integer> classifier : java.util.List.<Function<Integer, Integer>> of(i -> 0, i -> i % 3, i -> i)) {
                final HashMap<Integer, NonEmptySortedSet<Integer>> groups = ness.groupBy(classifier);
                assertThat(groups.mapValues(NonEmptySortedSet::toSortedSet)).isEqualTo(set.groupBy(classifier));
                assertThat(groups.values().forAll(group -> group.comparator() == set.comparator())).isTrue();
            }
            assertThatIllegalArgumentException().isThrownBy(() -> ness.grouped(0));
        }
    }

    @Nested
    class ReturnsTreeSet {

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptySortedSetTest#sets")
        public void shouldShrinkDownToEmpty(int n, TreeSet<Integer> set) {
            final NonEmptySortedSet<Integer> ness = ness(set);
            assertThat(ness.filter(i -> i % 2 == 0)).isEqualTo(set.filter(i -> i % 2 == 0));
            assertThat(ness.filter(i -> false)).isEmpty();
            assertThat(ness.reject(i -> true)).isEmpty();
            assertThat(ness.collect(i -> i % 2 == 0 ? Option.some(-i) : Option.none())).isEqualTo(set.collect(i -> i % 2 == 0 ? Option.some(-i) : Option.none()));
            assertThat(ness.collect(Comparator.<Integer> reverseOrder(), i -> Option.some(i)).head()).isEqualTo(n - 1);
            assertThat(ness.collect(i -> Option.none())).isEmpty();
            assertThat(ness.flatMapAll(i -> List.<Integer> empty())).isEmpty();
            assertThat(ness.flatMapAll(i -> List.of(i, -i))).isEqualTo(set.flatMap(i -> List.of(i, -i)));
            assertThat(ness.flatMapAll(Comparator.reverseOrder(), i -> List.of(i, -i))).isEqualTo(set.flatMap(Comparator.reverseOrder(), i -> List.of(i, -i)));
            assertThat(ness.remove(0)).isEqualTo(set.remove(0));
            assertThat(ness.removeAll(once(set))).isEmpty();
            assertThat(ness.retainAll(once(List.of(0)))).isEqualTo(TreeSet.of(0));
            assertThat(ness.intersect(HashSet.of(-1))).isEmpty();
            assertThat(ness.intersect(set.toSet())).isEqualTo(set);
            assertThat(ness.diff(set)).isEmpty();
            assertThat(ness.diff(HashSet.of(0))).isEqualTo(set.diff(HashSet.of(0)));
            assertThat(ness.partition(i -> i % 2 == 0)).isEqualTo(set.partition(i -> i % 2 == 0));
            assertThat(ness.filter(i -> i % 2 == 0).comparator()).isSameAs(set.comparator());
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptySortedSetTest#sets")
        public void shouldTakeAndDropByPosition(int n, TreeSet<Integer> set) {
            final NonEmptySortedSet<Integer> ness = ness(set);
            assertThat(ness.tail()).isEqualTo(set.tail());
            assertThat(ness.init()).isEqualTo(set.init());
            for (int k : new int[] { Integer.MIN_VALUE, -1, 0, 1, n - 1, n, n + 1, Integer.MAX_VALUE }) {
                assertThat(ness.take(k)).isEqualTo(set.take(k));
                assertThat(ness.takeRight(k)).isEqualTo(set.takeRight(k));
                assertThat(ness.drop(k)).isEqualTo(set.drop(k));
                assertThat(ness.dropRight(k)).isEqualTo(set.dropRight(k));
            }
            final int middle = n / 2;
            assertThat(ness.takeWhile(i -> i != middle)).isEqualTo(set.takeWhile(i -> i != middle));
            assertThat(ness.takeUntil(i -> i == middle)).isEqualTo(set.takeUntil(i -> i == middle));
            assertThat(ness.dropWhile(i -> i != middle)).isEqualTo(set.dropWhile(i -> i != middle));
            assertThat(ness.dropUntil(i -> i == middle)).isEqualTo(set.dropUntil(i -> i == middle));
        }
    }

    @Nested
    class Total {

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptySortedSetTest#sets")
        public void shouldReturnHeadLastAndTheNaturalMaxAndMin(int n, TreeSet<Integer> set) {
            final NonEmptySortedSet<Integer> ness = ness(set);
            assertThat(ness.head()).isEqualTo(set.head());
            assertThat(ness.last()).isEqualTo(set.last());
            // max and min ignore the comparator, as on TreeSet
            assertThat(ness.max()).isEqualTo(set.max().get()).isEqualTo(n - 1);
            assertThat(ness.min()).isEqualTo(set.min().get()).isEqualTo(0);
            assertThat(ness.maxBy(Comparator.reverseOrder())).isEqualTo(0);
            assertThat(ness.minBy(Comparator.reverseOrder())).isEqualTo(n - 1);
            assertThat(ness.maxBy(i -> -i)).isEqualTo(0);
            assertThat(ness.minBy(i -> -i)).isEqualTo(n - 1);
            assertThat(ness.maxBy(i -> 0)).isEqualTo(set.maxBy(i -> 0).get()).isEqualTo(set.head());
            assertThat(ness.minBy((a, b) -> 0)).isEqualTo(set.minBy((a, b) -> 0).get()).isEqualTo(set.head());
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptySortedSetTest#sets")
        public void shouldReduceFoldAndQuery(int n, TreeSet<Integer> set) {
            final NonEmptySortedSet<Integer> ness = ness(set);
            assertThat(ness.reduce((a, b) -> a)).isEqualTo(set.head());
            assertThat(ness.reduce(Integer::sum)).isEqualTo(set.reduce(Integer::sum));
            assertThat(ness.<String> reduceMap(String::valueOf, (a, b) -> a + "," + b)).isEqualTo(set.mkString(","));
            assertThat(ness.fold(0, Integer::sum)).isEqualTo(n * (n - 1) / 2);
            assertThat(ness.foldLeft("", (s, i) -> s + i)).isEqualTo(set.foldLeft("", (s, i) -> s + i));
            assertThat(ness.sum()).isEqualTo(set.sum());
            assertThat(ness.product()).isEqualTo(set.product());
            assertThat(ness.average()).isEqualTo(set.average().get());
            assertThat(ness.size()).isEqualTo(n);
            assertThat(ness.contains(n - 1)).isTrue();
            assertThat(ness.contains(n)).isFalse();
            assertThat(ness.containsAll(once(set))).isTrue();
            assertThat(ness.exists(i -> i == 0)).isTrue();
            assertThat(ness.existsUnique(i -> i >= 0)).isEqualTo(n == 1);
            assertThat(ness.forAll(i -> i < n)).isTrue();
            assertThat(ness.count(i -> i % 2 == 0)).isEqualTo((n + 1) / 2);
            assertThat(ness.find(i -> i % 2 == 0)).isEqualTo(set.find(i -> i % 2 == 0));
            assertThat(ness.arrangeBy(i -> -i)).isEqualTo(set.arrangeBy(i -> -i));
            if (n == 1) {
                assertThat(ness.single()).isEqualTo(0);
            } else {
                assertThatThrownBy(ness::single).isInstanceOf(java.util.NoSuchElementException.class);
            }
        }

        @Test
        public void shouldTreatNaNAsTreeSetDoes() {
            final NonEmptySortedSet<Double> doubles = NonEmptySortedSet.of(1.0, Double.NaN, -1.0);
            assertThat(doubles.min()).isEqualTo(doubles.toSortedSet().min().get()).isNaN();
            assertThat(doubles.max()).isEqualTo(doubles.toSortedSet().max().get()).isNaN();
            assertThat(doubles.head()).isEqualTo(-1.0);
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptySortedSetTest#sets")
        public void shouldIterateInOrderAndConvert(int n, TreeSet<Integer> set) {
            final NonEmptySortedSet<Integer> ness = ness(set);
            assertThat(ness).containsExactlyElementsOf(set);
            assertThat(ness.spliterator().characteristics()).isEqualTo(set.spliterator().characteristics());
            assertThat(ness.spliterator().hasCharacteristics(Spliterator.SORTED | Spliterator.DISTINCT)).isTrue();
            assertThat(ness.stream().collect(Collectors.toList())).containsExactlyElementsOf(set);
            assertThat(ness.asJava()).isEqualTo(set.asJava());
            assertThat(ness.asJava().first()).isEqualTo(set.head());
            assertThatThrownBy(() -> ness.asJava().add(-1)).isInstanceOf(UnsupportedOperationException.class);
            assertThat(ness.mkString()).isEqualTo(set.mkString());
            assertThat(ness.mkString(", ")).isEqualTo(set.mkString(", "));
            assertThat(ness.mkString("<", ", ", ">")).isEqualTo(set.mkString("<", ", ", ">"));
            assertThat(ness.collect(Collectors.toList())).isEqualTo(set.collect(Collectors.toList()));
            assertThat(ness.<java.util.ArrayList<Integer>> collect(java.util.ArrayList::new, java.util.ArrayList::add, java.util.ArrayList::addAll)).containsExactlyElementsOf(set);
            assertThat(ness.toArray()).isEqualTo(set.toArray());
            assertThat(ness.toArray(Integer[]::new)).isEqualTo(set.toArray(Integer[]::new));
            assertThat(ness.toVector()).isEqualTo(set.toVector());
            assertThat(ness.toList()).isEqualTo(set.toList());
            assertThat(ness.toQueue()).isEqualTo(set.toQueue());
            assertThat(ness.toStream()).isEqualTo(set.toStream());
            assertThat(ness.toSet()).isEqualTo(set.toSet());
            assertThat(ness.toLinkedSet()).isEqualTo(set.toLinkedSet());
            assertThat(ness.toSortedSet(Comparator.naturalOrder()).head()).isEqualTo(0);
            assertThat(ness.toMap(i -> i, i -> -i)).isEqualTo(set.toMap(i -> i, i -> -i));
            assertThat(ness.toMap(i -> Tuple.of(i % 5, i))).isEqualTo(set.toMap(i -> Tuple.of(i % 5, i)));
            assertThat(ness.toLinkedMap(i -> i, i -> -i)).isEqualTo(set.toLinkedMap(i -> i, i -> -i));
            assertThat(ness.toLinkedMap(i -> Tuple.of(i, -i))).isEqualTo(set.toLinkedMap(i -> Tuple.of(i, -i)));
            assertThat(ness.toSortedMap(i -> i, i -> -i)).isEqualTo(set.toSortedMap(i -> i, i -> -i));
            assertThat(ness.toSortedMap(i -> Tuple.of(i, -i))).isEqualTo(set.toSortedMap(i -> Tuple.of(i, -i)));
            assertThat(ness.toSortedMap(Comparator.<Integer> reverseOrder(), i -> i, i -> -i).head()._1()).isEqualTo(n - 1);
            assertThat(ness.toSortedMap(Comparator.<Integer> reverseOrder(), i -> Tuple.of(i, -i)).head()._1()).isEqualTo(n - 1);
        }
    }

    @Nested
    class ReturnsOption {

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptySortedSetTest#sets")
        public void shouldNarrowTailAndInit(int n, TreeSet<Integer> set) {
            final NonEmptySortedSet<Integer> ness = ness(set);
            if (n == 1) {
                assertThat(ness.tailNonEmpty()).isEqualTo(Option.none());
                assertThat(ness.initNonEmpty()).isEqualTo(Option.none());
            } else {
                assertThat(ness.tailNonEmpty().get().toSortedSet()).isEqualTo(set.tail());
                assertThat(ness.initNonEmpty().get().toSortedSet()).isEqualTo(set.init());
            }
        }
    }

    @Nested
    class NonEmptyGuarantee {

        static java.util.Map<String, Function<NonEmptySortedSet<Integer>, java.util.List<Object>>> calls() {
            final java.util.Map<String, Function<NonEmptySortedSet<Integer>, java.util.List<Object>>> calls = new java.util.HashMap<>();
            final Comparator<Integer> reverse = Comparator.reverseOrder();
            // constructors and narrowings
            calls.put("of(Comparable, Comparable[])", s -> java.util.List.of(NonEmptySortedSet.of(s.head()), NonEmptySortedSet.of(s.head(), s.head())));
            calls.put("of(Comparator, Object, Object[])", s -> java.util.List.of(NonEmptySortedSet.of(reverse, s.head()), NonEmptySortedSet.of(reverse, s.head(), s.head())));
            calls.put("single(Comparable)", s -> java.util.List.of(NonEmptySortedSet.single(s.head())));
            calls.put("single(Comparator, Object)", s -> java.util.List.of(NonEmptySortedSet.single(reverse, s.head())));
            calls.put("fromIterable(Comparable, Iterable)", s -> java.util.List.of(NonEmptySortedSet.fromIterable(s.head(), List.empty()), NonEmptySortedSet.fromIterable(s.head(), s)));
            calls.put("fromIterable(Comparator, Object, Iterable)", s -> java.util.List.of(NonEmptySortedSet.fromIterable(reverse, s.head(), List.empty())));
            calls.put("fromIterable(Iterable)", s -> java.util.List.of(NonEmptySortedSet.fromIterable(s), NonEmptySortedSet.fromIterable(List.<Integer> empty())));
            calls.put("fromIterable(Comparator, Iterable)", s -> java.util.List.of(NonEmptySortedSet.fromIterable(reverse, s), NonEmptySortedSet.fromIterable(reverse, List.empty())));
            calls.put("fromSortedSet(TreeSet)", s -> java.util.List.of(NonEmptySortedSet.fromSortedSet(s.toSortedSet()), NonEmptySortedSet.fromSortedSet(TreeSet.<Integer> empty())));
            calls.put("unsafeFromSortedSet(TreeSet)", s -> java.util.List.of(NonEmptySortedSet.unsafeFromSortedSet(s.toSortedSet())));
            calls.put("flatten(NonEmptySortedSet)", s -> java.util.List.of(NonEmptySortedSet.flatten(NonEmptySortedSet.single(Comparator.comparing(NonEmptySortedSet::head), s))));
            calls.put("flatten(Comparator, NonEmptySortedSet)", s -> java.util.List.of(NonEmptySortedSet.flatten(reverse, NonEmptySortedSet.single(Comparator.comparing(NonEmptySortedSet::head), s))));
            calls.put("tailNonEmpty()", s -> java.util.List.of(s.tailNonEmpty()));
            calls.put("initNonEmpty()", s -> java.util.List.of(s.initNonEmpty()));
            // at least one element is left
            calls.put("add(Object)", s -> java.util.List.of(s.add(s.head()), s.add(-1)));
            calls.put("addAll(Iterable)", s -> java.util.List.of(s.addAll(List.empty()), s.addAll(s)));
            calls.put("union(Set)", s -> java.util.List.of(s.union(HashSet.empty()), s.union(s.toSortedSet())));
            calls.put("map(Function)", s -> java.util.List.of(s.map(i -> 0)));
            calls.put("map(Comparator, Function)", s -> java.util.List.of(s.map(reverse, i -> 0)));
            calls.put("flatMap(Function)", s -> java.util.List.of(s.flatMap(i -> NonEmptySortedSet.single(0))));
            calls.put("flatMap(Comparator, Function)", s -> java.util.List.of(s.flatMap(reverse, i -> NonEmptySortedSet.single(0))));
            calls.put("as(Object)", s -> java.util.List.of(s.as("x")));
            calls.put("replace(Object, Object)", s -> java.util.List.of(s.replace(s.head(), s.last()), s.replace(-2, -1)));
            calls.put("replaceAll(Object, Object)", s -> java.util.List.of(s.replaceAll(s.head(), s.last())));
            calls.put("tap(Consumer)", s -> java.util.List.of(s.tap(i -> { })));
            // non-empty collections inside another type
            calls.put("groupBy(Function)", s -> java.util.List.of(s.groupBy(i -> 0), s.groupBy(i -> i)));
            calls.put("grouped(int)", s -> java.util.List.of(s.grouped(1), s.grouped(Integer.MAX_VALUE)));
            calls.put("sliding(int)", s -> java.util.List.of(s.sliding(1), s.sliding(Integer.MAX_VALUE)));
            calls.put("sliding(int, int)", s -> java.util.List.of(s.sliding(1, Integer.MAX_VALUE), s.sliding(Integer.MAX_VALUE, 1)));
            calls.put("slideBy(Function)", s -> java.util.List.of(s.slideBy(i -> 0), s.slideBy(i -> i)));
            calls.put("zipWithIndex()", s -> java.util.List.of(s.zipWithIndex()));
            return calls;
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptySortedSetTest#sets")
        public void shouldReturnOnlyNonEmptyNonEmptyCollections(int n, TreeSet<Integer> set) {
            for (var call : calls().entrySet()) {
                for (Object result : call.getValue().apply(ness(set))) {
                    NonEmptyChecks.assertEveryNonEmptyCollectionIsNonEmpty(result, call.getKey());
                }
            }
        }

        @Test
        public void shouldCoverEveryOverloadWhoseResultHoldsANonEmptyCollection() {
            assertThat(calls().keySet()).containsExactlyInAnyOrderElementsOf(NonEmptyChecks.holding(NonEmptySortedSet.class));
        }
    }

    /**
     * {@code NonEmptySortedSet} has every operation of {@code TreeSet}, under the name {@code TreeSet} gives it, except
     * the ones listed here on purpose.
     */
    @Nested
    class SameApiAsTreeSet {

        static final java.util.Set<String> DELIBERATELY_ABSENT = java.util.Set.of(
                // the Option forms of what is total on a non-empty set
                "headOption", "lastOption", "reduceOption", "singleOption",
                // tail and init already return a TreeSet, and tailNonEmpty/initNonEmpty are the narrowing
                "tailOption", "initOption",
                // constant on a non-empty set: false, true, this, Some(this)
                "isEmpty", "nonEmpty", "orElse", "toNonEmptySortedSet"
        );

        @Test
        public void shouldHaveEveryTreeSetMethodButTheDeliberateAbsences() {
            assertThat(NonEmptyChecks.missing(TreeSet.class, NonEmptySortedSet.class)).containsExactlyInAnyOrderElementsOf(DELIBERATELY_ABSENT);
        }
    }

    @Nested
    class Flatten {

        @Test
        public void shouldFlattenNestedNonEmptySortedSets() {
            final Comparator<NonEmptySortedSet<Integer>> byHead = Comparator.comparing(NonEmptySortedSet::head);
            for (int n : SIZES) {
                final NonEmptySortedSet<NonEmptySortedSet<Integer>> nested = ness(TreeSet.range(0, n)).map(byHead, i -> ness(TreeSet.range(i, i + 3)));
                assertThat(NonEmptySortedSet.flatten(nested).toSortedSet()).isEqualTo(TreeSet.range(0, n + 2));
                assertThat(NonEmptySortedSet.flatten(Comparator.reverseOrder(), nested).head()).isEqualTo(n + 1);
            }
        }
    }

    @Nested
    class ObjectMethods {

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptySortedSetTest#sets")
        public void shouldBeEqualToANonEmptySetWithTheSameElements(int n, TreeSet<Integer> set) {
            final NonEmptySortedSet<Integer> ness = ness(set);
            final NonEmptySortedSet<Integer> natural = NonEmptySortedSet.fromIterable(set.toVector()).get();
            assertThat(ness).isEqualTo(natural);
            assertThat(ness.hashCode()).isEqualTo(natural.hashCode());
            assertThat(ness).isEqualTo(NonEmptySet.fromIterable(set).get());
            assertThat(ness).isNotEqualTo(ness.add(n));
            assertThat(ness).isNotEqualTo(set);
            assertThat(ness).isNotEqualTo(null);
        }

        @Test
        public void shouldStringifyInOrder() {
            assertThat(NonEmptySortedSet.of(2, 1).toString()).isEqualTo("NonEmptySortedSet(1, 2)");
            assertThat(NonEmptySortedSet.of(Comparator.<Integer> reverseOrder(), 1, 2).toString()).isEqualTo("NonEmptySortedSet(2, 1)");
        }
    }
}
