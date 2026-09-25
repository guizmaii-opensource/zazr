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
 * Every method is checked at sizes 1, 2, 31, 32, 33, 1023, 1024 and 1025 (the boundaries of the 32-wide trie) against
 * the equivalent {@link HashMap} call, and on keys whose hashes collide, which the trie keeps in collision leaves.
 */
public class NonEmptyMapTest {

    static final int[] SIZES = { 1, 2, 31, 32, 33, 1023, 1024, 1025 };

    /* i -> "v" + i for i in [0, n) */
    static HashMap<Integer, String> entries(int from, int to) {
        return HashMap.ofEntries(Vector.range(from, to).map(i -> Tuple.of(i, "v" + i)));
    }

    /* (size, map) */
    static Stream<Arguments> maps() {
        final ArrayList<Arguments> cases = new ArrayList<>();
        for (int n : SIZES) {
            cases.add(Arguments.of(n, entries(0, n)));
        }
        return cases.stream();
    }

    /* a key whose hash is shared by four values, so that maps of them hold collision leaves */
    record Colliding(int value) implements Comparable<Colliding> {
        @Override
        public int hashCode() { return value >> 2; }

        @Override
        public int compareTo(Colliding that) { return Integer.compare(value, that.value); }
    }

    static <K, V> NonEmptyMap<K, V> nem(HashMap<K, V> map) {
        return NonEmptyMap.unsafeFromMap(map);
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
        public void shouldBuildFromEntriesTheLaterWinning() {
            assertThat(NonEmptyMap.single(1, "a").toMap()).isEqualTo(HashMap.of(1, "a"));
            assertThat(NonEmptyMap.of(Tuple.of(1, "a"), Tuple.of(2, "b"), Tuple.of(1, "c")).toMap()).isEqualTo(HashMap.of(1, "c", 2, "b"));
            for (int n : SIZES) {
                final HashMap<Integer, String> expected = entries(0, n);
                final Vector<Tuple2<Integer, String>> tail = entries(1, n).toVector();
                @SuppressWarnings("unchecked")
                final Tuple2<Integer, String>[] array = tail.toArray(Tuple2[]::new);
                assertThat(NonEmptyMap.of(Tuple.of(0, "v0"), array).toMap()).isEqualTo(expected);
                assertThat(NonEmptyMap.fromIterable(Tuple.of(0, "v0"), once(tail)).toMap()).isEqualTo(expected);
                assertThat(NonEmptyMap.fromIterable(Tuple.of(0, "v0"), entries(1, n)).toMap()).isEqualTo(expected);
                assertThat(NonEmptyMap.fromIterable(once(expected.toVector())).get().toMap()).isEqualTo(expected);
                assertThat(NonEmptyMap.fromIterable(Tuple.of(0, "x"), once(expected.toVector())).get(0)).isEqualTo(Option.some("v0"));
            }
            assertThat(NonEmptyMap.fromIterable(Tuple.of(0, "a"), List.<Tuple2<Integer, String>> empty()).toMap()).isEqualTo(HashMap.of(0, "a"));
        }

        @Test
        public void shouldWrapWithoutCopyingAndNarrow() {
            for (int n : SIZES) {
                final HashMap<Integer, String> map = entries(0, n);
                assertThat(NonEmptyMap.fromMap(map).get().toMap()).isSameAs(map);
                assertThat(NonEmptyMap.unsafeFromMap(map).toMap()).isSameAs(map);
                assertThat(map.toNonEmptyMap().get().toMap()).isSameAs(map);
                assertThat(NonEmptyMap.fromIterable(map).get().toMap()).isSameAs(map);
            }
            assertThat(NonEmptyMap.fromMap(HashMap.<Integer, String> empty())).isEqualTo(Option.none());
            assertThat(HashMap.<Integer, String> empty().toNonEmptyMap()).isEqualTo(Option.none());
            assertThat(NonEmptyMap.fromIterable(HashMap.<Integer, String> empty())).isEqualTo(Option.none());
            assertThat(NonEmptyMap.fromIterable(once(List.<Tuple2<Integer, String>> empty()))).isEqualTo(Option.none());
            assertThatIllegalArgumentException()
              .isThrownBy(() -> NonEmptyMap.unsafeFromMap(HashMap.empty()))
              .withMessage("NonEmptyMap.unsafeFromMap: map is empty");
        }

        @Test
        @SuppressWarnings("unchecked")
        public void shouldRejectNullsNamingTheType() {
            assertThatNullPointerException().isThrownBy(() -> NonEmptyMap.single(null, "a")).withMessage("NonEmptyMap: key is null");
            assertThatNullPointerException().isThrownBy(() -> NonEmptyMap.single(1, null)).withMessage("NonEmptyMap: value is null");
            assertThatNullPointerException().isThrownBy(() -> NonEmptyMap.of(null)).withMessage("NonEmptyMap: head is null");
            assertThatNullPointerException().isThrownBy(() -> NonEmptyMap.of(Tuple.of(1, "a"), (Tuple2<Integer, String>[]) null)).withMessage("NonEmptyMap: tail is null");
            assertThatNullPointerException().isThrownBy(() -> NonEmptyMap.of(Tuple.of(1, "a"), (Tuple2<Integer, String>) null)).withMessage("NonEmptyMap: entry is null");
            assertThatNullPointerException().isThrownBy(() -> NonEmptyMap.of(Tuple.of(null, "a"))).withMessage("NonEmptyMap: key is null");
            assertThatNullPointerException().isThrownBy(() -> NonEmptyMap.of(Tuple.of(1, null))).withMessage("NonEmptyMap: value is null");
            assertThatNullPointerException().isThrownBy(() -> NonEmptyMap.fromIterable(null, List.of(Tuple.of(1, "a")))).withMessage("NonEmptyMap: head is null");
            assertThatNullPointerException().isThrownBy(() -> NonEmptyMap.fromIterable(Tuple.of(1, "a"), null)).withMessage("NonEmptyMap: tail is null");
            assertThatNullPointerException().isThrownBy(() -> NonEmptyMap.fromIterable(Tuple.of(1, "a"), Arrays.asList(Tuple.of(2, (String) null)))).withMessage("NonEmptyMap: value is null");
            assertThatNullPointerException().isThrownBy(() -> NonEmptyMap.fromIterable(null)).withMessage("NonEmptyMap.fromIterable: entries is null");
            assertThatNullPointerException().isThrownBy(() -> NonEmptyMap.fromIterable(Arrays.asList(Tuple.of(1, "a"), null))).withMessage("NonEmptyMap: entry is null");
            assertThatNullPointerException().isThrownBy(() -> NonEmptyMap.fromMap(null)).withMessage("NonEmptyMap.fromMap: map is null");
            assertThatNullPointerException().isThrownBy(() -> NonEmptyMap.unsafeFromMap(null)).withMessage("NonEmptyMap.unsafeFromMap: map is null");
            final NonEmptyMap<Integer, String> nem = NonEmptyMap.of(Tuple.of(1, "a"), Tuple.of(2, "b"));
            assertThatNullPointerException().isThrownBy(() -> nem.put(null, "a")).withMessage("NonEmptyMap.put: key is null");
            assertThatNullPointerException().isThrownBy(() -> nem.put(1, null)).withMessage("NonEmptyMap.put: value is null");
            assertThatNullPointerException().isThrownBy(() -> nem.put((Tuple2<Integer, String>) null)).withMessage("NonEmptyMap.put: entry is null");
            assertThatNullPointerException().isThrownBy(() -> nem.put(Tuple.of(1, (String) null))).withMessage("NonEmptyMap.put: value is null");
            assertThatNullPointerException().isThrownBy(() -> nem.put(3, null, (a, b) -> a)).withMessage("NonEmptyMap.put: value is null");
            assertThatNullPointerException().isThrownBy(() -> nem.put((Tuple2<Integer, String>) null, (a, b) -> a)).withMessage("NonEmptyMap.put: entry is null");
            assertThatNullPointerException().isThrownBy(() -> nem.replace(Tuple.of(9, "z"), null)).withMessage("NonEmptyMap.replace: newElement is null");
            assertThatNullPointerException().isThrownBy(() -> nem.replace(Tuple.of(9, "z"), Tuple.of(null, "x"))).withMessage("NonEmptyMap.replace: key is null");
            assertThatNullPointerException().isThrownBy(() -> nem.replace(Tuple.of(9, "z"), Tuple.of(1, null))).withMessage("NonEmptyMap.replace: value is null");
            assertThatNullPointerException().isThrownBy(() -> nem.replace(9, "z", null)).withMessage("NonEmptyMap.replace: newValue is null");
            assertThatNullPointerException().isThrownBy(() -> nem.replaceValue(9, null)).withMessage("NonEmptyMap.replaceValue: value is null");
            assertThatNullPointerException().isThrownBy(() -> nem.flatMap((k, v) -> null)).withMessage("NonEmptyMap.flatMap: mapper returned null");
            assertThatNullPointerException().isThrownBy(() -> nem.flatMap(null));
            assertThatNullPointerException().isThrownBy(() -> nem.map((k, v) -> null));
            assertThatNullPointerException().isThrownBy(() -> nem.mapValues(v -> null));
            assertThatNullPointerException().isThrownBy(() -> nem.merge(null));
            assertThatNullPointerException().isThrownBy(() -> nem.computeIfAbsent(3, k -> null));
            assertThatNullPointerException().isThrownBy(() -> nem.maxBy((Comparator<Tuple2<Integer, String>>) null));
            assertThatNullPointerException().isThrownBy(() -> nem.minBy((Function<Tuple2<Integer, String>, Integer>) null));
            assertThatNullPointerException().isThrownBy(() -> nem.reduce(null));
            assertThatNullPointerException().isThrownBy(() -> nem.groupBy(null));
        }
    }

    @Nested
    class ReturnsNonEmptyMap {

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyMapTest#maps")
        public void shouldPutAndKeepTheOriginal(int n, HashMap<Integer, String> map) {
            final NonEmptyMap<Integer, String> nem = nem(map);
            assertThat(nem.put(-1, "x").toMap()).isEqualTo(map.put(-1, "x"));
            assertThat(nem.put(-1, "x").size()).isEqualTo(n + 1);
            assertThat(nem.put(0, "x").toMap()).isEqualTo(map.put(0, "x"));
            assertThat(nem.put(Tuple.of(-1, "x")).toMap()).isEqualTo(map.put(Tuple.of(-1, "x")));
            assertThat(nem.put(0, "x", String::concat).toMap()).isEqualTo(map.put(0, "x", String::concat));
            assertThat(nem.put(Tuple.of(0, "x"), String::concat).toMap()).isEqualTo(map.put(Tuple.of(0, "x"), String::concat));
            assertThat(nem.toMap()).isSameAs(map);
            assertThat(nem.size()).isEqualTo(n);
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyMapTest#maps")
        public void shouldMergeAndCompute(int n, HashMap<Integer, String> map) {
            final NonEmptyMap<Integer, String> nem = nem(map);
            for (int m : new int[] { 0, 1, 33, 1025 }) {
                final HashMap<Integer, String> that = HashMap.ofEntries(Vector.range(n / 2, n / 2 + m).map(i -> Tuple.of(i, "w" + i)));
                assertThat(nem.merge(that).toMap()).isEqualTo(map.merge(that));
                assertThat(nem.merge(that, String::concat).toMap()).isEqualTo(map.merge(that, String::concat));
                assertThat(nem.merge(that.toSortedMap(t -> t)).toMap()).isEqualTo(map.merge(that));
            }
            assertThat(nem.merge(HashMap.empty())).isSameAs(nem);
            assertThat(nem.computeIfAbsent(-1, k -> "c")._1()).isEqualTo("c");
            assertThat(nem.computeIfAbsent(-1, k -> "c")._2().toMap()).isEqualTo(map.computeIfAbsent(-1, k -> "c")._2());
            assertThat(nem.computeIfAbsent(0, k -> "c")._1()).isEqualTo("v0");
            assertThat(nem.computeIfPresent(0, (k, v) -> v + "!")).isEqualTo(Tuple.of(Option.some("v0!"), nem.put(0, "v0!")));
            assertThat(nem.computeIfPresent(-1, (k, v) -> v + "!")).isEqualTo(Tuple.of(Option.none(), nem));
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyMapTest#maps")
        public void shouldMapCollapsingEqualKeys(int n, HashMap<Integer, String> map) {
            final NonEmptyMap<Integer, String> nem = nem(map);
            assertThat(nem.map((k, v) -> Tuple.of(-k, v + "!")).toMap()).isEqualTo(map.map((k, v) -> Tuple.of(-k, v + "!")));
            assertThat(nem.map((k, v) -> Tuple.of(0, v)).size()).isEqualTo(1);
            assertThat(nem.mapBoth(k -> k % 3, String::length).toMap()).isEqualTo(map.mapBoth(k -> k % 3, String::length));
            assertThat(nem.mapKeys(k -> k % 3).toMap()).isEqualTo(map.mapKeys(k -> k % 3));
            assertThat(nem.mapKeys(k -> 0, String::concat).size()).isEqualTo(1);
            assertThat(nem.mapKeys(k -> k % 3, String::concat).toMap()).isEqualTo(map.mapKeys(k -> k % 3, String::concat));
            assertThat(nem.mapValues(String::length).toMap()).isEqualTo(map.mapValues(String::length));
            assertThat(nem.flatMap((k, v) -> NonEmptyMap.of(Tuple.of(k, v), Tuple.of(-k - 1, v))).toMap())
              .isEqualTo(map.flatMap((k, v) -> List.of(Tuple.of(k, v), Tuple.of(-k - 1, v))));
            assertThat(nem.flatMap((k, v) -> NonEmptyMap.single(0, v)).size()).isEqualTo(1);
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyMapTest#maps")
        public void shouldReplace(int n, HashMap<Integer, String> map) {
            final NonEmptyMap<Integer, String> nem = nem(map);
            assertThat(nem.replace(Tuple.of(0, "v0"), Tuple.of(-1, "x")).toMap()).isEqualTo(map.replace(Tuple.of(0, "v0"), Tuple.of(-1, "x")));
            // replacing by an entry whose key is present shrinks the map, never below one entry
            assertThat(nem.replace(Tuple.of(0, "v0"), Tuple.of(n - 1, "x")).size()).isEqualTo(Math.max(1, n - 1));
            assertThat(nem.replace(Tuple.of(0, "nope"), Tuple.of(-1, "x"))).isEqualTo(nem);
            assertThat(nem.replaceAll(Tuple.of(0, "v0"), Tuple.of(-1, "x")).toMap()).isEqualTo(map.replaceAll(Tuple.of(0, "v0"), Tuple.of(-1, "x")));
            assertThat(nem.replace(0, "v0", "x").toMap()).isEqualTo(map.replace(0, "v0", "x"));
            assertThat(nem.replace(0, "nope", "x")).isEqualTo(nem);
            assertThat(nem.replaceAll((k, v) -> v + k).toMap()).isEqualTo(map.replaceAll((k, v) -> v + k));
            assertThat(nem.replaceValue(0, "x").toMap()).isEqualTo(map.replaceValue(0, "x"));
            assertThat(nem.replaceValue(-1, "x")).isEqualTo(nem);
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyMapTest#maps")
        public void shouldTapKeySetValuesAndGroupBy(int n, HashMap<Integer, String> map) {
            final NonEmptyMap<Integer, String> nem = nem(map);
            final java.util.List<Tuple2<Integer, String>> seen = new java.util.ArrayList<>();
            assertThat(nem.tap(seen::add)).isSameAs(nem);
            assertThat(seen).containsExactlyElementsOf(map);
            assertThat(nem.keySet().toSet()).isEqualTo(map.keySet());
            assertThat(nem.values().toVector()).isEqualTo(map.values());
            for (Function<Tuple2<Integer, String>, Integer> classifier : java.util.List.<Function<Tuple2<Integer, String>, Integer>> of(t -> 0, t -> t._1() % 3, Tuple2::_1)) {
                final HashMap<Integer, NonEmptyMap<Integer, String>> groups = nem.groupBy(classifier);
                assertThat(groups.mapValues(NonEmptyMap::toMap)).isEqualTo(map.groupBy(classifier));
            }
        }
    }

    @Nested
    class ReturnsHashMap {

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyMapTest#maps")
        public void shouldShrinkDownToEmpty(int n, HashMap<Integer, String> map) {
            final NonEmptyMap<Integer, String> nem = nem(map);
            assertThat(nem.filter((k, v) -> k % 2 == 0)).isEqualTo(map.filter((k, v) -> k % 2 == 0));
            assertThat(nem.filter(t -> t._1() % 2 == 0)).isEqualTo(map.filter(t -> t._1() % 2 == 0));
            assertThat(nem.filter((k, v) -> false)).isEmpty();
            assertThat(nem.filterKeys(k -> k % 2 == 0)).isEqualTo(map.filterKeys(k -> k % 2 == 0));
            assertThat(nem.filterValues(v -> v.endsWith("1"))).isEqualTo(map.filterValues(v -> v.endsWith("1")));
            assertThat(nem.reject((k, v) -> k % 2 == 0)).isEqualTo(map.reject((k, v) -> k % 2 == 0));
            assertThat(nem.reject(t -> true)).isEmpty();
            assertThat(nem.rejectKeys(k -> true)).isEmpty();
            assertThat(nem.rejectValues(v -> v.endsWith("1"))).isEqualTo(map.rejectValues(v -> v.endsWith("1")));
            assertThat(nem.collect((k, v) -> k % 2 == 0 ? Option.some(Tuple.of(v, k)) : Option.none())).isEqualTo(map.collect((k, v) -> k % 2 == 0 ? Option.some(Tuple.of(v, k)) : Option.none()));
            assertThat(nem.collect((k, v) -> Option.<Tuple2<Integer, String>> none())).isEmpty();
            assertThat(nem.flatMapAll((k, v) -> List.<Tuple2<Integer, String>> empty())).isEmpty();
            assertThat(nem.flatMapAll((k, v) -> List.of(Tuple.of(-k, v)))).isEqualTo(map.flatMap((k, v) -> List.of(Tuple.of(-k, v))));
            assertThat(nem.remove(0)).isEqualTo(map.remove(0));
            assertThat(nem.remove(0).size()).isEqualTo(n - 1);
            assertThat(nem.removeAll(once(map.keySet()))).isEmpty();
            assertThat(nem.removeAll(List.of(0, -1))).isEqualTo(map.removeAll(List.of(0, -1)));
            assertThat(nem.retainAll(once(List.of(Tuple.of(0, "v0"), Tuple.of(1, "nope"))))).isEqualTo(HashMap.of(0, "v0"));
            assertThat(nem.retainAll(List.empty())).isEmpty();
            assertThat(nem.partition(t -> t._1() % 2 == 0)).isEqualTo(map.partition(t -> t._1() % 2 == 0));
            assertThat(nem.partition(t -> true)._2()).isEmpty();
        }
    }

    @Nested
    class Total {

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyMapTest#maps")
        public void shouldReturnAggregates(int n, HashMap<Integer, String> map) {
            final NonEmptyMap<Integer, String> nem = nem(map);
            assertThat(nem.max()).isEqualTo(map.max().get()).isEqualTo(Tuple.of(n - 1, "v" + (n - 1)));
            assertThat(nem.min()).isEqualTo(map.min().get()).isEqualTo(Tuple.of(0, "v0"));
            assertThat(nem.maxBy(Comparator.comparing(Tuple2::_1))).isEqualTo(Tuple.of(n - 1, "v" + (n - 1)));
            assertThat(nem.minBy(Comparator.comparing((Tuple2<Integer, String> t) -> t._1()).reversed())).isEqualTo(Tuple.of(n - 1, "v" + (n - 1)));
            assertThat(nem.maxBy(t -> -t._1())).isEqualTo(Tuple.of(0, "v0"));
            assertThat(nem.minBy(t -> -t._1())).isEqualTo(Tuple.of(n - 1, "v" + (n - 1)));
            assertThat(nem.maxBy(t -> 0)).isEqualTo(map.maxBy(t -> 0).get());
            assertThat(nem.minBy((a, b) -> 0)).isEqualTo(map.minBy((a, b) -> 0).get());
            assertThat(nem.reduce((a, b) -> Tuple.of(a._1() + b._1(), "s"))._1()).isEqualTo(n * (n - 1) / 2);
            assertThat(nem.reduce((a, b) -> Tuple.of(a._1() + b._1(), "s"))).isEqualTo(map.reduce((a, b) -> Tuple.of(a._1() + b._1(), "s")));
            assertThat(nem.<Long> reduceMap(t -> (long) t._1(), Long::sum)).isEqualTo((long) n * (n - 1) / 2);
            assertThat(nem.fold(Tuple.of(0, ""), (a, b) -> Tuple.of(a._1() + b._1(), ""))).isEqualTo(Tuple.of(n * (n - 1) / 2, ""));
            if (n == 1) {
                assertThat(nem.single()).isEqualTo(Tuple.of(0, "v0"));
            } else {
                assertThatThrownBy(nem::single).isInstanceOf(java.util.NoSuchElementException.class);
            }
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyMapTest#maps")
        public void shouldQuery(int n, HashMap<Integer, String> map) {
            final NonEmptyMap<Integer, String> nem = nem(map);
            assertThat(nem.size()).isEqualTo(n);
            assertThat(nem.get(n - 1)).isEqualTo(Option.some("v" + (n - 1)));
            assertThat(nem.get(n)).isEqualTo(Option.none());
            assertThat(nem.getOrElse(n, "d")).isEqualTo("d");
            assertThat(nem.containsKey(0)).isTrue();
            assertThat(nem.containsKey(-1)).isFalse();
            assertThat(nem.containsValue("v0")).isTrue();
            assertThat(nem.containsValue("x")).isFalse();
            assertThat(nem.contains(Tuple.of(0, "v0"))).isTrue();
            assertThat(nem.contains(Tuple.of(0, "x"))).isFalse();
            assertThat(nem.containsAll(once(map))).isTrue();
            assertThat(nem.exists(t -> t._1() == n - 1)).isTrue();
            assertThat(nem.existsUnique(t -> t._1() >= 0)).isEqualTo(n == 1);
            assertThat(nem.forAll(t -> t._1() < n)).isTrue();
            assertThat(nem.count(t -> t._1() % 2 == 0)).isEqualTo((n + 1) / 2);
            assertThat(nem.find(t -> t._1() == 0)).isEqualTo(Option.some(Tuple.of(0, "v0")));
            assertThat(nem.foldLeft(0, (acc, t) -> acc + t._1())).isEqualTo(n * (n - 1) / 2);
            assertThat(nem.arrangeBy(Tuple2::_2)).isEqualTo(map.arrangeBy(Tuple2::_2));
            final java.util.Map<Integer, String> seen = new java.util.HashMap<>();
            nem.forEach(seen::put);
            assertThat(seen).isEqualTo(map.asJavaMap());
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyMapTest#maps")
        public void shouldIterateStreamAndConvert(int n, HashMap<Integer, String> map) {
            final NonEmptyMap<Integer, String> nem = nem(map);
            assertThat(nem).containsExactlyElementsOf(map);
            assertThat(nem.spliterator().characteristics()).isEqualTo(map.spliterator().characteristics());
            assertThat(nem.spliterator().getExactSizeIfKnown()).isEqualTo(n);
            assertThat(nem.stream().collect(Collectors.toList())).containsExactlyElementsOf(map);
            assertThat(nem.asJava()).containsExactlyElementsOf(map);
            assertThat(nem.asJavaMap()).isEqualTo(map.asJavaMap());
            assertThatThrownBy(() -> nem.asJavaMap().put(-1, "x")).isInstanceOf(UnsupportedOperationException.class);
            assertThat(nem.mkString()).isEqualTo(map.mkString());
            assertThat(nem.mkString(", ")).isEqualTo(map.mkString(", "));
            assertThat(nem.mkString("<", ", ", ">")).isEqualTo(map.mkString("<", ", ", ">"));
            assertThat(nem.collect(Collectors.toList())).isEqualTo(map.collect(Collectors.toList()));
            assertThat(nem.<java.util.ArrayList<Tuple2<Integer, String>>> collect(java.util.ArrayList::new, java.util.ArrayList::add, java.util.ArrayList::addAll)).containsExactlyElementsOf(map);
            assertThat(nem.toArray()).isEqualTo(map.toArray());
            @SuppressWarnings("unchecked")
            final Tuple2<Integer, String>[] array = nem.toArray(Tuple2[]::new);
            assertThat(array).containsExactlyElementsOf(map);
            assertThat(nem.toVector()).isEqualTo(map.toVector());
            assertThat(nem.toList()).isEqualTo(map.toList());
            assertThat(nem.toQueue()).isEqualTo(map.toQueue());
            assertThat(nem.toStream()).isEqualTo(map.toStream());
            assertThat(nem.toSet()).isEqualTo(map.toSet());
            assertThat(nem.toLinkedSet()).isEqualTo(map.toLinkedSet());
            assertThat(nem.toSortedSet()).isEqualTo(map.toSortedSet());
            assertThat(nem.toSortedSet(Comparator.comparing(Tuple2::_1)).head()).isEqualTo(Tuple.of(0, "v0"));
            assertThat(nem.toMap(Tuple2::_2, Tuple2::_1)).isEqualTo(map.toMap(Tuple2::_2, Tuple2::_1));
            assertThat(nem.toMap(Tuple2::swap)).isEqualTo(map.toMap(Tuple2::swap));
            assertThat(nem.toLinkedMap(Tuple2::_2, Tuple2::_1)).isEqualTo(map.toLinkedMap(Tuple2::_2, Tuple2::_1));
            assertThat(nem.toLinkedMap(Tuple2::swap)).isEqualTo(map.toLinkedMap(Tuple2::swap));
            assertThat(nem.toSortedMap(Tuple2::_2, Tuple2::_1)).isEqualTo(map.toSortedMap(Tuple2::_2, Tuple2::_1));
            assertThat(nem.toSortedMap(Tuple2::swap)).isEqualTo(map.toSortedMap(Tuple2::swap));
            assertThat(nem.toSortedMap(Comparator.<Integer> reverseOrder(), Tuple2::_1, Tuple2::_2).head()._1()).isEqualTo(n - 1);
            assertThat(nem.toSortedMap(Comparator.<Integer> reverseOrder(), t -> t).head()._1()).isEqualTo(n - 1);
        }
    }

    @Nested
    class Collisions {

        @Test
        public void shouldKeepTheContractOnCollidingKeys() {
            for (int n : SIZES) {
                final HashMap<Colliding, Integer> map = HashMap.ofEntries(Vector.range(0, n).map(i -> Tuple.of(new Colliding(i), i)));
                final NonEmptyMap<Colliding, Integer> nem = nem(map);
                assertThat(nem.size()).isEqualTo(n);
                assertThat(nem.put(new Colliding(n), n).toMap()).isEqualTo(map.put(new Colliding(n), n));
                assertThat(nem.put(new Colliding(0), -1).get(new Colliding(0))).isEqualTo(Option.some(-1));
                assertThat(nem.remove(new Colliding(0))).isEqualTo(map.remove(new Colliding(0)));
                assertThat(nem.get(new Colliding(n - 1))).isEqualTo(Option.some(n - 1));
                assertThat(nem.containsKey(new Colliding(n))).isFalse();
                assertThat(nem.keySet().toSet()).isEqualTo(map.keySet());
                assertThat(nem.mapKeys(c -> c.value() >> 2).size()).isEqualTo((n + 3) / 4);
                assertThat(nem.filterKeys(c -> c.value() % 4 == 0)).isEqualTo(map.filterKeys(c -> c.value() % 4 == 0));
                assertThat(nem.max()._1()).isEqualTo(new Colliding(n - 1));
                assertThat(nem).isEqualTo(NonEmptyMap.fromIterable(map.toVector().reverse()).get());
            }
        }
    }

    @Nested
    class NonEmptyGuarantee {

        static java.util.Map<String, Function<NonEmptyMap<Integer, String>, java.util.List<Object>>> calls() {
            final java.util.Map<String, Function<NonEmptyMap<Integer, String>, java.util.List<Object>>> calls = new java.util.HashMap<>();
            // constructors and narrowings
            calls.put("single(Object, Object)", m -> java.util.List.of(NonEmptyMap.single(0, "a")));
            calls.put("of(Tuple2, Tuple2[])", m -> java.util.List.of(NonEmptyMap.of(m.max()), NonEmptyMap.of(m.max(), m.max())));
            calls.put("fromIterable(Tuple2, Iterable)", m -> java.util.List.of(NonEmptyMap.fromIterable(m.max(), List.empty()), NonEmptyMap.fromIterable(m.max(), m)));
            calls.put("fromIterable(Iterable)", m -> java.util.List.of(NonEmptyMap.fromIterable(m), NonEmptyMap.fromIterable(List.<Tuple2<Integer, String>> empty())));
            calls.put("fromMap(HashMap)", m -> java.util.List.of(NonEmptyMap.fromMap(m.toMap()), NonEmptyMap.fromMap(HashMap.<Integer, String> empty())));
            calls.put("unsafeFromMap(HashMap)", m -> java.util.List.of(NonEmptyMap.unsafeFromMap(m.toMap())));
            // at least one entry is left
            calls.put("put(Object, Object)", m -> java.util.List.of(m.put(0, "x"), m.put(-1, "x")));
            calls.put("put(Tuple2)", m -> java.util.List.of(m.put(Tuple.of(0, "x"))));
            calls.put("put(Object, Object, BiFunction)", m -> java.util.List.of(m.put(0, "x", String::concat)));
            calls.put("put(Tuple2, BiFunction)", m -> java.util.List.of(m.put(Tuple.of(0, "x"), String::concat)));
            calls.put("merge(Map)", m -> java.util.List.of(m.merge(HashMap.empty()), m.merge(m.toMap())));
            calls.put("merge(Map, BiFunction)", m -> java.util.List.of(m.merge(HashMap.<Integer, String> empty(), String::concat)));
            calls.put("computeIfAbsent(Object, Function)", m -> java.util.List.of(m.computeIfAbsent(0, k -> "x"), m.computeIfAbsent(-1, k -> "x")));
            calls.put("computeIfPresent(Object, BiFunction)", m -> java.util.List.of(m.computeIfPresent(0, (k, v) -> "x"), m.computeIfPresent(-1, (k, v) -> "x")));
            calls.put("map(BiFunction)", m -> java.util.List.of(m.map((k, v) -> Tuple.of(0, v))));
            calls.put("mapBoth(Function, Function)", m -> java.util.List.of(m.mapBoth(k -> 0, v -> v)));
            calls.put("mapKeys(Function)", m -> java.util.List.of(m.mapKeys(k -> 0)));
            calls.put("mapKeys(Function, BiFunction)", m -> java.util.List.of(m.mapKeys(k -> 0, String::concat)));
            calls.put("mapValues(Function)", m -> java.util.List.of(m.mapValues(v -> 0)));
            calls.put("flatMap(BiFunction)", m -> java.util.List.of(m.flatMap((k, v) -> NonEmptyMap.single(0, v))));
            calls.put("replace(Tuple2, Tuple2)", m -> java.util.List.of(m.replace(m.min(), Tuple.of(m.max()._1(), "x")), m.replace(Tuple.of(-1, "x"), Tuple.of(-2, "y"))));
            calls.put("replaceAll(Tuple2, Tuple2)", m -> java.util.List.of(m.replaceAll(m.min(), Tuple.of(m.max()._1(), "x"))));
            calls.put("replace(Object, Object, Object)", m -> java.util.List.of(m.replace(0, "v0", "x"), m.replace(0, "nope", "x")));
            calls.put("replaceAll(BiFunction)", m -> java.util.List.of(m.replaceAll((k, v) -> "x")));
            calls.put("replaceValue(Object, Object)", m -> java.util.List.of(m.replaceValue(0, "x"), m.replaceValue(-1, "x")));
            calls.put("tap(Consumer)", m -> java.util.List.of(m.tap(t -> { })));
            // non-empty collections inside another type
            calls.put("keySet()", m -> java.util.List.of(m.keySet()));
            calls.put("values()", m -> java.util.List.of(m.values()));
            calls.put("groupBy(Function)", m -> java.util.List.of(m.groupBy(t -> 0), m.groupBy(Tuple2::_1)));
            return calls;
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyMapTest#maps")
        public void shouldReturnOnlyNonEmptyNonEmptyCollections(int n, HashMap<Integer, String> map) {
            for (var call : calls().entrySet()) {
                for (Object result : call.getValue().apply(nem(map))) {
                    NonEmptyChecks.assertEveryNonEmptyCollectionIsNonEmpty(result, call.getKey());
                }
            }
        }

        @Test
        public void shouldCoverEveryOverloadWhoseResultHoldsANonEmptyCollection() {
            assertThat(calls().keySet()).containsExactlyInAnyOrderElementsOf(NonEmptyChecks.holding(NonEmptyMap.class));
        }
    }

    /**
     * {@code NonEmptyMap} has every operation of {@code HashMap}, under the name {@code HashMap} gives it, except the
     * ones listed here on purpose.
     */
    @Nested
    class SameApiAsHashMap {

        static final java.util.Set<String> DELIBERATELY_ABSENT = java.util.Set.of(
                // the Option forms of what is total on a non-empty map
                "reduceOption", "singleOption",
                // constant on a non-empty map: false, true, this, Some(this)
                "isEmpty", "nonEmpty", "orElse", "toNonEmptyMap",
                // deprecated on the plain maps in favour of rejectKeys and rejectValues
                "removeKeys", "removeValues"
        );

        @Test
        public void shouldHaveEveryHashMapMethodButTheDeliberateAbsences() {
            assertThat(NonEmptyChecks.missing(HashMap.class, NonEmptyMap.class)).containsExactlyInAnyOrderElementsOf(DELIBERATELY_ABSENT);
        }
    }

    @Nested
    class ObjectMethods {

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptyMapTest#maps")
        public void shouldBeEqualToANonEmptyMapWithTheSameEntries(int n, HashMap<Integer, String> map) {
            final NonEmptyMap<Integer, String> nem = nem(map);
            final NonEmptyMap<Integer, String> copy = NonEmptyMap.fromIterable(map.toVector().reverse()).get();
            assertThat(nem).isEqualTo(nem);
            assertThat(nem).isEqualTo(copy);
            assertThat(nem.hashCode()).isEqualTo(copy.hashCode());
            assertThat(nem.hashCode()).isEqualTo(map.hashCode());
            assertThat(nem).isNotEqualTo(nem.put(0, "x"));
            assertThat(nem).isNotEqualTo(map);
            assertThat(map).isNotEqualTo(nem);
            assertThat(nem).isNotEqualTo(null);
            final NonEmptySortedMap<Integer, String> sorted = NonEmptySortedMap.unsafeFromSortedMap(TreeMap.ofEntries(Comparator.reverseOrder(), map));
            assertThat(nem).isEqualTo(sorted);
            assertThat(sorted).isEqualTo(nem);
            assertThat(nem.hashCode()).isEqualTo(sorted.hashCode());
            assertThat(nem).isNotEqualTo(sorted.put(0, "x"));
        }

        @Test
        public void shouldStringify() {
            assertThat(NonEmptyMap.single(1, "a").toString()).isEqualTo("NonEmptyMap((1, a))");
        }
    }
}
