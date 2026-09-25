package com.guizmaii.zazr.collection;

import com.guizmaii.zazr.Tuple;
import com.guizmaii.zazr.Tuple2;
import com.guizmaii.zazr.control.Option;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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
 * Every method is checked at sizes 1, 2, 31, 32, 33, 1023, 1024 and 1025 against the equivalent {@link TreeMap} call,
 * with the keys in their natural order and in the reverse order, where the comparator's order and the natural order of
 * the entries disagree.
 */
public class NonEmptySortedMapTest {

    static final int[] SIZES = { 1, 2, 31, 32, 33, 1023, 1024, 1025 };

    static Vector<Tuple2<Integer, String>> entries(int from, int to) {
        return Vector.range(from, to).map(i -> Tuple.of(i, "v" + i));
    }

    /* (size, map) in the natural and the reverse order of the keys */
    static Stream<Arguments> maps() {
        final ArrayList<Arguments> cases = new ArrayList<>();
        for (int n : SIZES) {
            cases.add(Arguments.of(n, TreeMap.ofEntries(entries(0, n))));
            cases.add(Arguments.of(n, TreeMap.ofEntries(Comparator.<Integer> reverseOrder(), entries(0, n))));
        }
        return cases.stream();
    }

    static <K, V> NonEmptySortedMap<K, V> nesm(TreeMap<K, V> map) {
        return NonEmptySortedMap.unsafeFromSortedMap(map);
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
        @SuppressWarnings("unchecked")
        public void shouldBuildInNaturalOrderOrByAComparator() {
            final Comparator<Integer> reverse = Comparator.reverseOrder();
            assertThat(NonEmptySortedMap.single(1, "a").toSortedMap()).isEqualTo(TreeMap.of(1, "a"));
            assertThat(NonEmptySortedMap.single(reverse, 1, "a").comparator().compare(1, 2)).isPositive();
            assertThat(NonEmptySortedMap.of(Tuple.of(2, "b"), Tuple.of(1, "a"), Tuple.of(2, "c")).toSortedMap()).isEqualTo(TreeMap.of(1, "a", 2, "c"));
            assertThat(NonEmptySortedMap.of(reverse, Tuple.of(1, "a"), Tuple.of(2, "b")).head()).isEqualTo(Tuple.of(2, "b"));
            for (int n : SIZES) {
                final TreeMap<Integer, String> expected = TreeMap.ofEntries(entries(0, n));
                final Vector<Tuple2<Integer, String>> tail = entries(1, n).reverse();
                assertThat(NonEmptySortedMap.of(Tuple.of(0, "v0"), tail.toArray(Tuple2[]::new)).toSortedMap()).isEqualTo(expected);
                assertThat(NonEmptySortedMap.fromIterable(Tuple.of(0, "v0"), once(tail)).toSortedMap()).isEqualTo(expected);
                assertThat(NonEmptySortedMap.fromIterable(Tuple.of(0, "v0"), TreeMap.ofEntries(tail)).toSortedMap()).isEqualTo(expected);
                assertThat(NonEmptySortedMap.fromIterable(reverse, Tuple.of(0, "v0"), once(tail)).head()._1()).isEqualTo(n - 1);
                assertThat(NonEmptySortedMap.fromIterable(once(tail.prepend(Tuple.of(0, "v0")))).get().toSortedMap()).isEqualTo(expected);
                assertThat(NonEmptySortedMap.fromIterable(reverse, once(tail.prepend(Tuple.of(0, "v0")))).get().last()).isEqualTo(Tuple.of(0, "v0"));
                // a TreeMap argument in another order is re-sorted, not adopted
                assertThat(NonEmptySortedMap.fromIterable(reverse, expected).get().head()._1()).isEqualTo(n - 1);
                assertThat(NonEmptySortedMap.fromIterable(reverse, Tuple.of(0, "v0"), TreeMap.ofEntries(tail)).head()._1()).isEqualTo(n - 1);
                assertThat(NonEmptySortedMap.fromIterable(Tuple.of(0, "v0"), TreeMap.ofEntries(reverse, tail)).head()._1()).isEqualTo(0);
            }
        }

        @Test
        public void shouldWrapWithoutCopyingAndNarrow() {
            for (int n : SIZES) {
                final TreeMap<Integer, String> map = TreeMap.ofEntries(Comparator.<Integer> reverseOrder(), entries(0, n));
                assertThat(NonEmptySortedMap.fromSortedMap(map).get().toSortedMap()).isSameAs(map);
                assertThat(NonEmptySortedMap.unsafeFromSortedMap(map).toSortedMap()).isSameAs(map);
                assertThat(map.toNonEmptySortedMap().get().toSortedMap()).isSameAs(map);
                assertThat(map.toNonEmptySortedMap().get().head()._1()).isEqualTo(n - 1);
            }
            assertThat(NonEmptySortedMap.fromSortedMap(TreeMap.<Integer, String> empty())).isEqualTo(Option.none());
            assertThat(TreeMap.<Integer, String> empty().toNonEmptySortedMap()).isEqualTo(Option.none());
            assertThat(NonEmptySortedMap.fromIterable(once(List.<Tuple2<Integer, String>> empty()))).isEqualTo(Option.none());
            assertThat(NonEmptySortedMap.fromIterable(Comparator.<Integer> naturalOrder(), List.<Tuple2<Integer, String>> empty())).isEqualTo(Option.none());
            assertThatIllegalArgumentException()
              .isThrownBy(() -> NonEmptySortedMap.unsafeFromSortedMap(TreeMap.<Integer, String> empty()))
              .withMessage("NonEmptySortedMap.unsafeFromSortedMap: map is empty");
        }

        @Test
        @SuppressWarnings("unchecked")
        public void shouldRejectNullsNamingTheType() {
            assertThatNullPointerException().isThrownBy(() -> NonEmptySortedMap.single((Integer) null, "a")).withMessage("NonEmptySortedMap: key is null");
            assertThatNullPointerException().isThrownBy(() -> NonEmptySortedMap.single(1, null)).withMessage("NonEmptySortedMap: value is null");
            assertThatNullPointerException().isThrownBy(() -> NonEmptySortedMap.single(null, 1, "a")).withMessage("keyComparator is null");
            assertThatNullPointerException().isThrownBy(() -> NonEmptySortedMap.of((Tuple2<Integer, String>) null)).withMessage("NonEmptySortedMap: head is null");
            assertThatNullPointerException().isThrownBy(() -> NonEmptySortedMap.of(Tuple.of(1, "a"), (Tuple2<Integer, String>[]) null)).withMessage("NonEmptySortedMap: tail is null");
            assertThatNullPointerException().isThrownBy(() -> NonEmptySortedMap.of(Tuple.of(1, "a"), (Tuple2<Integer, String>) null)).withMessage("NonEmptySortedMap: entry is null");
            assertThatNullPointerException().isThrownBy(() -> NonEmptySortedMap.of(Tuple.of((Integer) null, "a"))).withMessage("NonEmptySortedMap: key is null");
            assertThatNullPointerException().isThrownBy(() -> NonEmptySortedMap.fromIterable(Tuple.of(1, "a"), (Iterable<Tuple2<Integer, String>>) null)).withMessage("NonEmptySortedMap: tail is null");
            assertThatNullPointerException().isThrownBy(() -> NonEmptySortedMap.fromIterable(Tuple.of(1, "a"), Arrays.asList(Tuple.of(2, (String) null)))).withMessage("NonEmptySortedMap: value is null");
            assertThatNullPointerException().isThrownBy(() -> NonEmptySortedMap.fromIterable((Iterable<Tuple2<Integer, String>>) null)).withMessage("NonEmptySortedMap.fromIterable: entries is null");
            assertThatNullPointerException().isThrownBy(() -> NonEmptySortedMap.fromSortedMap(null)).withMessage("NonEmptySortedMap.fromSortedMap: map is null");
            assertThatNullPointerException().isThrownBy(() -> NonEmptySortedMap.unsafeFromSortedMap(null)).withMessage("NonEmptySortedMap.unsafeFromSortedMap: map is null");
            final NonEmptySortedMap<Integer, String> nesm = NonEmptySortedMap.of(Tuple.of(1, "a"), Tuple.of(2, "b"));
            assertThatNullPointerException().isThrownBy(() -> nesm.put(null, "a")).withMessage("NonEmptySortedMap.put: key is null");
            assertThatNullPointerException().isThrownBy(() -> nesm.put(1, null)).withMessage("NonEmptySortedMap.put: value is null");
            assertThatNullPointerException().isThrownBy(() -> nesm.replaceValue(9, null)).withMessage("NonEmptySortedMap.replaceValue: value is null");
            assertThatNullPointerException().isThrownBy(() -> nesm.flatMap((k, v) -> null)).withMessage("NonEmptySortedMap.flatMap: mapper returned null");
            assertThatNullPointerException().isThrownBy(() -> nesm.flatMap(null, (k, v) -> NonEmptySortedMap.single(k, v))).withMessage("keyComparator is null");
            assertThatNullPointerException().isThrownBy(() -> nesm.map((k, v) -> null));
            assertThatNullPointerException().isThrownBy(() -> nesm.slideBy(null));
        }
    }

    @Nested
    class ReturnsNonEmptySortedMap {

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptySortedMapTest#maps")
        public void shouldPutMergeAndComputeKeepingTheComparator(int n, TreeMap<Integer, String> map) {
            final NonEmptySortedMap<Integer, String> nesm = nesm(map);
            assertThat(nesm.put(-1, "x").toSortedMap()).isEqualTo(map.put(-1, "x"));
            assertThat(nesm.put(-1, "x").comparator()).isSameAs(map.comparator());
            assertThat(nesm.put(Tuple.of(-1, "x")).toSortedMap()).isEqualTo(map.put(Tuple.of(-1, "x")));
            assertThat(nesm.put(0, "x", String::concat).toSortedMap()).isEqualTo(map.put(0, "x", String::concat));
            assertThat(nesm.put(Tuple.of(0, "x"), String::concat).toSortedMap()).isEqualTo(map.put(Tuple.of(0, "x"), String::concat));
            assertThat(nesm.toSortedMap()).isSameAs(map);
            final HashMap<Integer, String> that = HashMap.ofEntries(Vector.range(n / 2, n / 2 + 40).map(i -> Tuple.of(i, "w" + i)));
            assertThat(nesm.merge(that).toSortedMap()).isEqualTo(map.merge(that));
            assertThat(nesm.merge(that, String::concat).toSortedMap()).isEqualTo(map.merge(that, String::concat));
            assertThat(nesm.merge(HashMap.empty())).isSameAs(nesm);
            assertThat(nesm.computeIfAbsent(-1, k -> "c")._2().toSortedMap()).isEqualTo(map.computeIfAbsent(-1, k -> "c")._2());
            assertThat(nesm.computeIfPresent(0, (k, v) -> v + "!")._2().toSortedMap()).isEqualTo(map.computeIfPresent(0, (k, v) -> v + "!")._2());
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptySortedMapTest#maps")
        public void shouldMapWithOrWithoutAComparator(int n, TreeMap<Integer, String> map) {
            final NonEmptySortedMap<Integer, String> nesm = nesm(map);
            final Comparator<Integer> reverse = Comparator.reverseOrder();
            assertThat(nesm.map((k, v) -> Tuple.of(-k, v)).toSortedMap()).isEqualTo(map.map((k, v) -> Tuple.of(-k, v)));
            assertThat(nesm.map(reverse, (k, v) -> Tuple.of(-k, v)).toSortedMap()).isEqualTo(map.map(reverse, (k, v) -> Tuple.of(-k, v)));
            assertThat(nesm.map((k, v) -> Tuple.of(0, v)).size()).isEqualTo(1);
            assertThat(nesm.mapBoth(k -> k % 3, String::length).toSortedMap()).isEqualTo(map.mapBoth(k -> k % 3, String::length));
            assertThat(nesm.mapBoth(reverse, k -> k % 3, String::length).toSortedMap()).isEqualTo(map.mapBoth(reverse, k -> k % 3, String::length));
            assertThat(nesm.mapKeys(k -> k % 3).toSortedMap()).isEqualTo(map.mapKeys(k -> k % 3));
            assertThat(nesm.mapKeys(k -> k % 3, String::concat).toSortedMap()).isEqualTo(map.mapKeys(k -> k % 3, String::concat));
            assertThat(nesm.mapValues(String::length).toSortedMap()).isEqualTo(map.mapValues(String::length));
            assertThat(nesm.flatMap((k, v) -> NonEmptySortedMap.of(Tuple.of(k, v), Tuple.of(-k - 1, v))).toSortedMap())
              .isEqualTo(map.flatMap((k, v) -> List.of(Tuple.of(k, v), Tuple.of(-k - 1, v))));
            assertThat(nesm.flatMap(reverse, (k, v) -> NonEmptySortedMap.single(k, v)).head()._1()).isEqualTo(n - 1);
            assertThat(nesm.flatMap((k, v) -> NonEmptySortedMap.single(reverse, k, v)).head()._1()).isEqualTo(0);
            assertThat(nesm.map(reverse, (k, v) -> Tuple.of(k, v)).head()._1()).isEqualTo(n - 1);
            assertThat(nesm.mapBoth(reverse, k -> k, v -> v).head()._1()).isEqualTo(n - 1);
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptySortedMapTest#maps")
        public void shouldReplaceAndTap(int n, TreeMap<Integer, String> map) {
            final NonEmptySortedMap<Integer, String> nesm = nesm(map);
            assertThat(nesm.replace(Tuple.of(0, "v0"), Tuple.of(-1, "x")).toSortedMap()).isEqualTo(map.replace(Tuple.of(0, "v0"), Tuple.of(-1, "x")));
            assertThat(nesm.replace(Tuple.of(0, "v0"), Tuple.of(n - 1, "x")).size()).isEqualTo(Math.max(1, n - 1));
            assertThat(nesm.replaceAll(Tuple.of(0, "v0"), Tuple.of(-1, "x")).toSortedMap()).isEqualTo(map.replaceAll(Tuple.of(0, "v0"), Tuple.of(-1, "x")));
            assertThat(nesm.replace(0, "v0", "x").toSortedMap()).isEqualTo(map.replace(0, "v0", "x"));
            assertThat(nesm.replaceAll((k, v) -> v + k).toSortedMap()).isEqualTo(map.replaceAll((k, v) -> v + k));
            assertThat(nesm.replaceValue(0, "x").toSortedMap()).isEqualTo(map.replaceValue(0, "x"));
            final java.util.List<Tuple2<Integer, String>> seen = new java.util.ArrayList<>();
            assertThat(nesm.tap(seen::add)).isSameAs(nesm);
            assertThat(seen).containsExactlyElementsOf(map);
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptySortedMapTest#maps")
        public void shouldReturnNonEmptyKeysValuesAndGroups(int n, TreeMap<Integer, String> map) {
            final NonEmptySortedMap<Integer, String> nesm = nesm(map);
            assertThat(nesm.keySet().toSortedSet()).isEqualTo(map.keySet());
            assertThat(nesm.keySet().comparator()).isSameAs(map.comparator());
            assertThat(nesm.keySet().head()).isEqualTo(map.head()._1());
            assertThat(nesm.values().toVector()).isEqualTo(map.values());
            for (int size : new int[] { 1, 2, 32, 33, 2000 }) {
                assertThat(nesm.grouped(size).map(NonEmptySortedMap::toSortedMap)).isEqualTo(map.grouped(size));
                assertThat(nesm.sliding(size).map(NonEmptySortedMap::toSortedMap)).isEqualTo(map.sliding(size));
                assertThat(nesm.sliding(size, 3).map(NonEmptySortedMap::toSortedMap)).isEqualTo(map.sliding(size, 3));
            }
            assertThat(nesm.slideBy(t -> t._1() / 10).map(NonEmptySortedMap::toSortedMap)).isEqualTo(map.slideBy(t -> t._1() / 10));
            assertThat(nesm.zipWithIndex().toVector()).isEqualTo(map.zipWithIndex());
            for (Function<Tuple2<Integer, String>, Integer> classifier : java.util.List.<Function<Tuple2<Integer, String>, Integer>> of(t -> 0, t -> t._1() % 3, Tuple2::_1)) {
                final HashMap<Integer, NonEmptySortedMap<Integer, String>> groups = nesm.groupBy(classifier);
                assertThat(groups.mapValues(NonEmptySortedMap::toSortedMap)).isEqualTo(map.groupBy(classifier));
                assertThat(groups.values().forAll(group -> group.comparator() == map.comparator())).isTrue();
            }
        }
    }

    @Nested
    class ReturnsTreeMap {

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptySortedMapTest#maps")
        public void shouldShrinkDownToEmpty(int n, TreeMap<Integer, String> map) {
            final NonEmptySortedMap<Integer, String> nesm = nesm(map);
            final Comparator<Integer> reverse = Comparator.reverseOrder();
            assertThat(nesm.filter((k, v) -> k % 2 == 0)).isEqualTo(map.filter((k, v) -> k % 2 == 0));
            assertThat(nesm.filter(t -> t._1() % 2 == 0)).isEqualTo(map.filter(t -> t._1() % 2 == 0));
            assertThat(nesm.filter((k, v) -> false)).isEmpty();
            assertThat(nesm.filterKeys(k -> k % 2 == 0)).isEqualTo(map.filterKeys(k -> k % 2 == 0));
            assertThat(nesm.filterValues(v -> v.endsWith("1"))).isEqualTo(map.filterValues(v -> v.endsWith("1")));
            assertThat(nesm.reject((k, v) -> true)).isEmpty();
            assertThat(nesm.reject(t -> t._1() == 0)).isEqualTo(map.reject(t -> t._1() == 0));
            assertThat(nesm.rejectKeys(k -> true)).isEmpty();
            assertThat(nesm.rejectValues(v -> true)).isEmpty();
            assertThat(nesm.collect((k, v) -> Option.some(Tuple.of(-k, v)))).isEqualTo(map.collect((k, v) -> Option.some(Tuple.of(-k, v))));
            assertThat(nesm.collect(reverse, (k, v) -> Option.some(Tuple.of(k, v))).head()._1()).isEqualTo(n - 1);
            assertThat(nesm.collect((k, v) -> Option.<Tuple2<Integer, String>> none())).isEmpty();
            assertThat(nesm.flatMapAll((k, v) -> List.<Tuple2<Integer, String>> empty())).isEmpty();
            assertThat(nesm.flatMapAll((k, v) -> List.of(Tuple.of(-k, v)))).isEqualTo(map.flatMap((k, v) -> List.of(Tuple.of(-k, v))));
            assertThat(nesm.flatMapAll(reverse, (k, v) -> List.of(Tuple.of(-k, v)))).isEqualTo(map.flatMap(reverse, (k, v) -> List.of(Tuple.of(-k, v))));
            assertThat(nesm.remove(0)).isEqualTo(map.remove(0));
            assertThat(nesm.removeAll(once(map.keySet()))).isEmpty();
            assertThat(nesm.retainAll(once(List.of(Tuple.of(0, "v0"))))).isEqualTo(TreeMap.of(0, "v0"));
            assertThat(nesm.partition(t -> t._1() % 2 == 0)).isEqualTo(map.partition(t -> t._1() % 2 == 0));
            assertThat(nesm.filterKeys(k -> k % 2 == 0).comparator()).isSameAs(map.comparator());
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptySortedMapTest#maps")
        public void shouldTakeAndDropByPosition(int n, TreeMap<Integer, String> map) {
            final NonEmptySortedMap<Integer, String> nesm = nesm(map);
            assertThat(nesm.tail()).isEqualTo(map.tail());
            assertThat(nesm.init()).isEqualTo(map.init());
            for (int k : new int[] { Integer.MIN_VALUE, -1, 0, 1, n - 1, n, n + 1, Integer.MAX_VALUE }) {
                assertThat(nesm.take(k)).isEqualTo(map.take(k));
                assertThat(nesm.takeRight(k)).isEqualTo(map.takeRight(k));
                assertThat(nesm.drop(k)).isEqualTo(map.drop(k));
                assertThat(nesm.dropRight(k)).isEqualTo(map.dropRight(k));
            }
            final int middle = n / 2;
            assertThat(nesm.takeWhile(t -> t._1() != middle)).isEqualTo(map.takeWhile(t -> t._1() != middle));
            assertThat(nesm.takeUntil(t -> t._1() == middle)).isEqualTo(map.takeUntil(t -> t._1() == middle));
            assertThat(nesm.dropWhile(t -> t._1() != middle)).isEqualTo(map.dropWhile(t -> t._1() != middle));
            assertThat(nesm.dropUntil(t -> t._1() == middle)).isEqualTo(map.dropUntil(t -> t._1() == middle));
        }
    }

    @Nested
    class Total {

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptySortedMapTest#maps")
        public void shouldReturnHeadLastAndTheNaturalMaxAndMin(int n, TreeMap<Integer, String> map) {
            final NonEmptySortedMap<Integer, String> nesm = nesm(map);
            assertThat(nesm.head()).isEqualTo(map.head());
            assertThat(nesm.last()).isEqualTo(map.last());
            // max and min ignore the comparator, as on TreeMap
            assertThat(nesm.max()).isEqualTo(map.max().get()).isEqualTo(Tuple.of(n - 1, "v" + (n - 1)));
            assertThat(nesm.min()).isEqualTo(map.min().get()).isEqualTo(Tuple.of(0, "v0"));
            assertThat(nesm.maxBy(t -> -t._1())).isEqualTo(Tuple.of(0, "v0"));
            assertThat(nesm.minBy(t -> -t._1())).isEqualTo(Tuple.of(n - 1, "v" + (n - 1)));
            assertThat(nesm.maxBy(Comparator.comparing(Tuple2::_1))).isEqualTo(Tuple.of(n - 1, "v" + (n - 1)));
            assertThat(nesm.minBy(Comparator.comparing(Tuple2::_1))).isEqualTo(Tuple.of(0, "v0"));
            assertThat(nesm.maxBy(t -> 0)).isEqualTo(map.maxBy(t -> 0).get()).isEqualTo(map.head());
            assertThat(nesm.reduce((a, b) -> a)).isEqualTo(map.head());
            assertThat(nesm.<String> reduceMap(Tuple2::_2, (a, b) -> a + "," + b)).isEqualTo(map.values().mkString(","));
            assertThat(nesm.fold(Tuple.of(0, ""), (a, b) -> Tuple.of(a._1() + b._1(), ""))).isEqualTo(Tuple.of(n * (n - 1) / 2, ""));
            if (n == 1) {
                assertThat(nesm.single()).isEqualTo(Tuple.of(0, "v0"));
            } else {
                assertThatThrownBy(nesm::single).isInstanceOf(java.util.NoSuchElementException.class);
            }
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptySortedMapTest#maps")
        public void shouldQueryIterateAndConvert(int n, TreeMap<Integer, String> map) {
            final NonEmptySortedMap<Integer, String> nesm = nesm(map);
            assertThat(nesm.size()).isEqualTo(n);
            assertThat(nesm.get(0)).isEqualTo(Option.some("v0"));
            assertThat(nesm.getOrElse(-1, "d")).isEqualTo("d");
            assertThat(nesm.containsKey(n - 1)).isTrue();
            assertThat(nesm.containsValue("v0")).isTrue();
            assertThat(nesm.contains(Tuple.of(0, "v0"))).isTrue();
            assertThat(nesm.containsAll(once(map))).isTrue();
            assertThat(nesm.exists(t -> t._1() == 0)).isTrue();
            assertThat(nesm.existsUnique(t -> t._1() >= 0)).isEqualTo(n == 1);
            assertThat(nesm.forAll(t -> t._1() < n)).isTrue();
            assertThat(nesm.count(t -> t._1() % 2 == 0)).isEqualTo((n + 1) / 2);
            assertThat(nesm.find(t -> t._1() % 2 == 0)).isEqualTo(map.find(t -> t._1() % 2 == 0));
            assertThat(nesm.foldLeft("", (s, t) -> s + t._1())).isEqualTo(map.foldLeft("", (s, t) -> s + t._1()));
            assertThat(nesm.arrangeBy(Tuple2::_2)).isEqualTo(map.arrangeBy(Tuple2::_2));
            final java.util.List<Integer> keys = new java.util.ArrayList<>();
            nesm.forEach((k, v) -> keys.add(k));
            assertThat(keys).containsExactlyElementsOf(map.keySet());
            assertThat(nesm).containsExactlyElementsOf(map);
            assertThat(nesm.spliterator().characteristics()).isEqualTo(map.spliterator().characteristics());
            assertThat(nesm.stream().collect(Collectors.toList())).containsExactlyElementsOf(map);
            assertThat(nesm.asJava()).containsExactlyElementsOf(map);
            assertThat(nesm.asJavaMap()).isEqualTo(map.asJavaMap());
            assertThat(nesm.asJavaMap().firstKey()).isEqualTo(map.head()._1());
            assertThat(nesm.mkString()).isEqualTo(map.mkString());
            assertThat(nesm.mkString(", ")).isEqualTo(map.mkString(", "));
            assertThat(nesm.mkString("<", ", ", ">")).isEqualTo(map.mkString("<", ", ", ">"));
            assertThat(nesm.collect(Collectors.toList())).isEqualTo(map.collect(Collectors.toList()));
            assertThat(nesm.<java.util.ArrayList<Tuple2<Integer, String>>> collect(java.util.ArrayList::new, java.util.ArrayList::add, java.util.ArrayList::addAll)).containsExactlyElementsOf(map);
            assertThat(nesm.toArray()).isEqualTo(map.toArray());
            @SuppressWarnings("unchecked")
            final Tuple2<Integer, String>[] array = nesm.toArray(Tuple2[]::new);
            assertThat(array).containsExactlyElementsOf(map);
            assertThat(nesm.toVector()).isEqualTo(map.toVector());
            assertThat(nesm.toList()).isEqualTo(map.toList());
            assertThat(nesm.toQueue()).isEqualTo(map.toQueue());
            assertThat(nesm.toStream()).isEqualTo(map.toStream());
            assertThat(nesm.toSet()).isEqualTo(map.toSet());
            assertThat(nesm.toLinkedSet()).isEqualTo(map.toLinkedSet());
            assertThat(nesm.toSortedSet()).isEqualTo(map.toSortedSet());
            assertThat(nesm.toSortedSet(Comparator.comparing(Tuple2::_1)).head()).isEqualTo(Tuple.of(0, "v0"));
            assertThat(nesm.toMap(Tuple2::_2, Tuple2::_1)).isEqualTo(map.toMap(Tuple2::_2, Tuple2::_1));
            assertThat(nesm.toMap(Tuple2::swap)).isEqualTo(map.toMap(Tuple2::swap));
            assertThat(nesm.toLinkedMap(Tuple2::_2, Tuple2::_1)).isEqualTo(map.toLinkedMap(Tuple2::_2, Tuple2::_1));
            assertThat(nesm.toLinkedMap(Tuple2::swap)).isEqualTo(map.toLinkedMap(Tuple2::swap));
            assertThat(nesm.toSortedMap(Tuple2::_2, Tuple2::_1)).isEqualTo(map.toSortedMap(Tuple2::_2, Tuple2::_1));
            assertThat(nesm.toSortedMap(Tuple2::swap)).isEqualTo(map.toSortedMap(Tuple2::swap));
            assertThat(nesm.toSortedMap(Comparator.<Integer> reverseOrder(), Tuple2::_1, Tuple2::_2).head()._1()).isEqualTo(n - 1);
            assertThat(nesm.toSortedMap(Comparator.<Integer> reverseOrder(), t -> t).head()._1()).isEqualTo(n - 1);
        }
    }

    @Nested
    class ReturnsOption {

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptySortedMapTest#maps")
        public void shouldNarrowTailAndInit(int n, TreeMap<Integer, String> map) {
            final NonEmptySortedMap<Integer, String> nesm = nesm(map);
            if (n == 1) {
                assertThat(nesm.tailNonEmpty()).isEqualTo(Option.none());
                assertThat(nesm.initNonEmpty()).isEqualTo(Option.none());
            } else {
                assertThat(nesm.tailNonEmpty().get().toSortedMap()).isEqualTo(map.tail());
                assertThat(nesm.initNonEmpty().get().toSortedMap()).isEqualTo(map.init());
            }
        }
    }

    @Nested
    class NonEmptyGuarantee {

        static java.util.Map<String, Function<NonEmptySortedMap<Integer, String>, java.util.List<Object>>> calls() {
            final java.util.Map<String, Function<NonEmptySortedMap<Integer, String>, java.util.List<Object>>> calls = new java.util.HashMap<>();
            final Comparator<Integer> reverse = Comparator.reverseOrder();
            // constructors and narrowings
            calls.put("single(Comparable, Object)", m -> java.util.List.of(NonEmptySortedMap.single(0, "a")));
            calls.put("single(Comparator, Object, Object)", m -> java.util.List.of(NonEmptySortedMap.single(reverse, 0, "a")));
            calls.put("of(Tuple2, Tuple2[])", m -> java.util.List.of(NonEmptySortedMap.of(m.head()), NonEmptySortedMap.of(m.head(), m.head())));
            calls.put("of(Comparator, Tuple2, Tuple2[])", m -> java.util.List.of(NonEmptySortedMap.of(reverse, m.head())));
            calls.put("fromIterable(Tuple2, Iterable)", m -> java.util.List.of(NonEmptySortedMap.fromIterable(m.head(), List.empty()), NonEmptySortedMap.fromIterable(m.head(), m)));
            calls.put("fromIterable(Comparator, Tuple2, Iterable)", m -> java.util.List.of(NonEmptySortedMap.fromIterable(reverse, m.head(), List.empty())));
            calls.put("fromIterable(Iterable)", m -> java.util.List.of(NonEmptySortedMap.fromIterable(m), NonEmptySortedMap.fromIterable(List.<Tuple2<Integer, String>> empty())));
            calls.put("fromIterable(Comparator, Iterable)", m -> java.util.List.of(NonEmptySortedMap.fromIterable(reverse, m), NonEmptySortedMap.fromIterable(reverse, List.<Tuple2<Integer, String>> empty())));
            calls.put("fromSortedMap(TreeMap)", m -> java.util.List.of(NonEmptySortedMap.fromSortedMap(m.toSortedMap()), NonEmptySortedMap.fromSortedMap(TreeMap.<Integer, String> empty())));
            calls.put("unsafeFromSortedMap(TreeMap)", m -> java.util.List.of(NonEmptySortedMap.unsafeFromSortedMap(m.toSortedMap())));
            calls.put("tailNonEmpty()", m -> java.util.List.of(m.tailNonEmpty()));
            calls.put("initNonEmpty()", m -> java.util.List.of(m.initNonEmpty()));
            // at least one entry is left
            calls.put("put(Object, Object)", m -> java.util.List.of(m.put(0, "x"), m.put(-1, "x")));
            calls.put("put(Tuple2)", m -> java.util.List.of(m.put(Tuple.of(0, "x"))));
            calls.put("put(Object, Object, BiFunction)", m -> java.util.List.of(m.put(0, "x", String::concat)));
            calls.put("put(Tuple2, BiFunction)", m -> java.util.List.of(m.put(Tuple.of(0, "x"), String::concat)));
            calls.put("merge(Map)", m -> java.util.List.of(m.merge(HashMap.empty()), m.merge(m.toSortedMap())));
            calls.put("merge(Map, BiFunction)", m -> java.util.List.of(m.merge(HashMap.<Integer, String> empty(), String::concat)));
            calls.put("computeIfAbsent(Object, Function)", m -> java.util.List.of(m.computeIfAbsent(0, k -> "x"), m.computeIfAbsent(-1, k -> "x")));
            calls.put("computeIfPresent(Object, BiFunction)", m -> java.util.List.of(m.computeIfPresent(0, (k, v) -> "x"), m.computeIfPresent(-1, (k, v) -> "x")));
            calls.put("map(BiFunction)", m -> java.util.List.of(m.map((k, v) -> Tuple.of(0, v))));
            calls.put("map(Comparator, BiFunction)", m -> java.util.List.of(m.map(reverse, (k, v) -> Tuple.of(0, v))));
            calls.put("mapBoth(Function, Function)", m -> java.util.List.of(m.mapBoth(k -> 0, v -> v)));
            calls.put("mapBoth(Comparator, Function, Function)", m -> java.util.List.of(m.mapBoth(reverse, k -> 0, v -> v)));
            calls.put("mapKeys(Function)", m -> java.util.List.of(m.mapKeys(k -> 0)));
            calls.put("mapKeys(Function, BiFunction)", m -> java.util.List.of(m.mapKeys(k -> 0, String::concat)));
            calls.put("mapValues(Function)", m -> java.util.List.of(m.mapValues(v -> 0)));
            calls.put("flatMap(BiFunction)", m -> java.util.List.of(m.flatMap((k, v) -> NonEmptySortedMap.single(0, v))));
            calls.put("flatMap(Comparator, BiFunction)", m -> java.util.List.of(m.flatMap(reverse, (k, v) -> NonEmptySortedMap.single(0, v))));
            calls.put("replace(Tuple2, Tuple2)", m -> java.util.List.of(m.replace(m.head(), Tuple.of(m.last()._1(), "x")), m.replace(Tuple.of(-1, "x"), Tuple.of(-2, "y"))));
            calls.put("replaceAll(Tuple2, Tuple2)", m -> java.util.List.of(m.replaceAll(m.head(), Tuple.of(m.last()._1(), "x"))));
            calls.put("replace(Object, Object, Object)", m -> java.util.List.of(m.replace(0, "v0", "x")));
            calls.put("replaceAll(BiFunction)", m -> java.util.List.of(m.replaceAll((k, v) -> "x")));
            calls.put("replaceValue(Object, Object)", m -> java.util.List.of(m.replaceValue(0, "x"), m.replaceValue(-1, "x")));
            calls.put("tap(Consumer)", m -> java.util.List.of(m.tap(t -> { })));
            // non-empty collections inside another type
            calls.put("keySet()", m -> java.util.List.of(m.keySet()));
            calls.put("values()", m -> java.util.List.of(m.values()));
            calls.put("zipWithIndex()", m -> java.util.List.of(m.zipWithIndex()));
            calls.put("groupBy(Function)", m -> java.util.List.of(m.groupBy(t -> 0), m.groupBy(Tuple2::_1)));
            calls.put("grouped(int)", m -> java.util.List.of(m.grouped(1), m.grouped(Integer.MAX_VALUE)));
            calls.put("sliding(int)", m -> java.util.List.of(m.sliding(1), m.sliding(Integer.MAX_VALUE)));
            calls.put("sliding(int, int)", m -> java.util.List.of(m.sliding(1, Integer.MAX_VALUE), m.sliding(Integer.MAX_VALUE, 1)));
            calls.put("slideBy(Function)", m -> java.util.List.of(m.slideBy(t -> 0), m.slideBy(Tuple2::_1)));
            return calls;
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptySortedMapTest#maps")
        public void shouldReturnOnlyNonEmptyNonEmptyCollections(int n, TreeMap<Integer, String> map) {
            for (var call : calls().entrySet()) {
                for (Object result : call.getValue().apply(nesm(map))) {
                    NonEmptyChecks.assertEveryNonEmptyCollectionIsNonEmpty(result, call.getKey());
                }
            }
        }

        @Test
        public void shouldCoverEveryOverloadWhoseResultHoldsANonEmptyCollection() {
            assertThat(calls().keySet()).containsExactlyInAnyOrderElementsOf(NonEmptyChecks.holding(NonEmptySortedMap.class));
        }
    }

    /**
     * {@code NonEmptySortedMap} has every operation of {@code TreeMap}, under the name {@code TreeMap} gives it, except
     * the ones listed here on purpose.
     */
    @Nested
    class SameApiAsTreeMap {

        static final java.util.Set<String> DELIBERATELY_ABSENT = java.util.Set.of(
                // the Option forms of what is total on a non-empty map
                "headOption", "lastOption", "reduceOption", "singleOption",
                // tail and init already return a TreeMap, and tailNonEmpty/initNonEmpty are the narrowing
                "tailOption", "initOption",
                // constant on a non-empty map: false, true, this, Some(this)
                "isEmpty", "nonEmpty", "orElse", "toNonEmptySortedMap",
                // deprecated on the plain maps in favour of rejectKeys and rejectValues
                "removeKeys", "removeValues"
        );

        @Test
        public void shouldHaveEveryTreeMapMethodButTheDeliberateAbsences() {
            assertThat(NonEmptyChecks.missing(TreeMap.class, NonEmptySortedMap.class)).containsExactlyInAnyOrderElementsOf(DELIBERATELY_ABSENT);
        }
    }

    @Nested
    class ObjectMethods {

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptySortedMapTest#maps")
        public void shouldBeEqualToANonEmptyMapWithTheSameEntries(int n, TreeMap<Integer, String> map) {
            final NonEmptySortedMap<Integer, String> nesm = nesm(map);
            final NonEmptySortedMap<Integer, String> natural = NonEmptySortedMap.fromIterable(map.toVector()).get();
            assertThat(nesm).isEqualTo(natural);
            assertThat(nesm.hashCode()).isEqualTo(natural.hashCode());
            assertThat(nesm).isEqualTo(NonEmptyMap.fromIterable(map).get());
            assertThat(nesm).isNotEqualTo(nesm.put(0, "x"));
            assertThat(nesm).isNotEqualTo(map);
            assertThat(nesm).isNotEqualTo(null);
        }

        @Test
        public void shouldStringifyInOrder() {
            assertThat(NonEmptySortedMap.of(Tuple.of(2, "b"), Tuple.of(1, "a")).toString()).isEqualTo("NonEmptySortedMap((1, a), (2, b))");
        }
    }
}
