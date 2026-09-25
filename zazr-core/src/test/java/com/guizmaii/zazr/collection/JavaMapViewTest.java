package com.guizmaii.zazr.collection;

import com.guizmaii.zazr.Tuple;
import com.guizmaii.zazr.Tuple2;
import java.util.Comparator;
import java.util.function.Function;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import static com.guizmaii.zazr.collection.JavaSetViewTest.name;
import static com.guizmaii.zazr.collection.JavaSetViewTest.shuffled;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The {@link java.util.Map} views against the JDK maps with the same mappings: {@link HashMap} against
 * {@link java.util.HashMap}, {@link LinkedHashMap} against {@link java.util.LinkedHashMap} (with the reversed
 * views), {@link TreeMap} against {@link java.util.TreeMap} in natural and reversed order (with the descending maps,
 * the key sets, and the head, tail and sub maps of each, nested), each with its key set, values and entry set, for
 * 0, 1, 32, 33 and 1025 entries mapping {@code k} to {@code "v" + k}.
 */
class JavaMapViewTest {

    private static java.util.List<Tuple2<Integer, String>> entries(java.util.List<Integer> keys) {
        return keys.stream().map(k -> Tuple.of(k, "v" + k)).toList();
    }

    private static <M extends java.util.Map<Integer, String>> M fill(M map, java.util.List<Integer> keys) {
        for (Integer k : keys) {
            map.put(k, "v" + k);
        }
        return map;
    }

    @TestFactory
    java.util.stream.Stream<DynamicTest> shouldReadAHashMapLikeAJavaHashMap() {
        return IntStream.of(JavaViewContract.SIZES).mapToObj(n -> DynamicTest.dynamicTest("HashMap of " + n, () -> {
            final java.util.List<Integer> keys = JavaViewContract.evens(n);
            JavaViewContract.unordered().map("HashMap(" + n + ").asJavaMap()", HashMap.ofEntries(entries(keys)).asJavaMap(), fill(new java.util.HashMap<>(), keys),
                    JavaViewContract.keyProbes(n), JavaViewContract.valueProbes(n));
        }));
    }

    @TestFactory
    java.util.stream.Stream<DynamicTest> shouldReadALinkedHashMapLikeAJavaLinkedHashMap() {
        return IntStream.of(JavaViewContract.SIZES).mapToObj(n -> DynamicTest.dynamicTest("LinkedHashMap of " + n, () -> {
            final java.util.List<Integer> keys = shuffled(JavaViewContract.evens(n));
            JavaViewContract.ordered().sequencedMap("LinkedHashMap(" + n + ").asJavaMap()", LinkedHashMap.ofEntries(entries(keys)).asJavaMap(),
                    fill(new java.util.LinkedHashMap<>(), keys), JavaViewContract.keyProbes(n), JavaViewContract.valueProbes(n), 0);
        }));
    }

    @TestFactory
    java.util.stream.Stream<DynamicTest> shouldReadALinkedHashMapWithRemovedAndReplacedKeysLikeAJavaLinkedHashMap() {
        return IntStream.of(JavaViewContract.SIZES).mapToObj(n -> DynamicTest.dynamicTest("LinkedHashMap of " + n + " minus every third", () -> {
            final java.util.List<Integer> keys = shuffled(JavaViewContract.evens(n));
            LinkedHashMap<Integer, String> map = LinkedHashMap.ofEntries(entries(keys));
            final java.util.LinkedHashMap<Integer, String> reference = fill(new java.util.LinkedHashMap<>(), keys);
            for (int i = 0; i < keys.size(); i += 3) {
                map = map.remove(keys.get(i));
                reference.remove(keys.get(i));
            }
            for (int i = 1; i < keys.size(); i += 5) {
                map = map.put(keys.get(i), "v" + keys.get(i));
                reference.put(keys.get(i), "v" + keys.get(i));
            }
            JavaViewContract.ordered().sequencedMap("LinkedHashMap(" + n + ").remove(...).asJavaMap()", map.asJavaMap(), reference,
                    JavaViewContract.keyProbes(n), JavaViewContract.valueProbes(n), 0);
        }));
    }

    @TestFactory
    java.util.stream.Stream<DynamicTest> shouldReadATreeMapLikeAJavaTreeMap() {
        return JavaViewContract.comparators().stream().flatMap(comparator -> IntStream.of(JavaViewContract.SIZES).mapToObj(n ->
                DynamicTest.dynamicTest("TreeMap of " + n + " by " + name(comparator), () -> {
                    final java.util.List<Integer> keys = shuffled(JavaViewContract.evens(n));
                    final java.util.TreeMap<Integer, String> reference = fill(new java.util.TreeMap<>(comparator == Comparator.<Integer> naturalOrder() ? null : comparator), keys);
                    final TreeMap<Integer, String> map = comparator == Comparator.<Integer> naturalOrder()
                                                         ? TreeMap.ofEntries(entries(keys))
                                                         : TreeMap.ofEntries(comparator, entries(keys));
                    JavaViewContract.ordered().navigableMap("TreeMap(" + n + ", " + name(comparator) + ").asJavaMap()", map.asJavaMap(), reference,
                            JavaViewContract.keyProbes(n), JavaViewContract.valueProbes(n), JavaViewContract.bounds(n), 0);
                })));
    }

    @TestFactory
    java.util.stream.Stream<DynamicTest> shouldReadTheEntriesOfAMapLikeAnArrayListOfThem() {
        final java.util.Map<String, Function<java.util.List<Tuple2<Integer, String>>, Map<Integer, String>>> maps = new java.util.LinkedHashMap<>();
        maps.put("HashMap", HashMap::ofEntries);
        maps.put("LinkedHashMap", LinkedHashMap::ofEntries);
        maps.put("TreeMap", TreeMap::ofEntries);
        return maps.entrySet().stream().flatMap(map -> IntStream.of(JavaViewContract.SIZES).mapToObj(n -> DynamicTest.dynamicTest(map.getKey() + ".asJava() of " + n, () -> {
            final Map<Integer, String> zazr = map.getValue().apply(entries(shuffled(JavaViewContract.evens(n))));
            final java.util.List<Tuple2<Integer, String>> reference = new java.util.ArrayList<>();
            for (Tuple2<Integer, String> entry : zazr) {
                reference.add(entry);
            }
            final java.util.List<Object> probes = new java.util.ArrayList<>();
            for (Object key : JavaViewContract.keyProbes(n)) {
                probes.add(Tuple.of(key, "v" + key));
                probes.add(Tuple.of(key, "other"));
            }
            probes.add(null);
            probes.add(JavaViewContract.FOREIGN);
            final JavaViewContract contract = map.getKey().equals("HashMap") ? JavaViewContract.unordered() : JavaViewContract.ordered();
            contract.collection(map.getKey() + "(" + n + ").asJava()", zazr.asJava(), reference, probes);
        })));
    }

    @Test
    void shouldGiveImmutableEntries() {
        for (java.util.Map<Integer, String> view : java.util.List.of(HashMap.of(1, "a").asJavaMap(), LinkedHashMap.of(1, "a").asJavaMap(), TreeMap.of(1, "a").asJavaMap())) {
            final java.util.Map.Entry<Integer, String> entry = view.entrySet().iterator().next();
            assertThat(entry).isEqualTo(java.util.Map.entry(1, "a"));
            assertThat(entry.hashCode()).isEqualTo(java.util.Map.entry(1, "a").hashCode());
            assertThat(entry.toString()).isEqualTo("1=a");
            assertThatThrownBy(() -> entry.setValue("b")).isInstanceOf(UnsupportedOperationException.class);
        }
        assertThatThrownBy(() -> TreeMap.of(1, "a").asJavaMap().firstEntry().setValue("b")).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> LinkedHashMap.of(1, "a").asJavaMap().lastEntry().setValue("b")).isInstanceOf(UnsupportedOperationException.class);
    }

    /** A key that counts the calls of its {@code equals} and {@code compareTo}. */
    private record Key(int value, int[] calls) implements Comparable<Key> {

        @Override
        public boolean equals(Object o) {
            calls[0]++;
            return o instanceof Key key && key.value == value;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(value);
        }

        @Override
        public int compareTo(Key that) {
            calls[0]++;
            return Integer.compare(value, that.value);
        }
    }

    @Test
    void shouldLookEntriesUpInsteadOfWalkingThem() {
        final int n = 1_000;
        final int[] calls = new int[1];
        final java.util.List<Tuple2<Key, Integer>> entries = new java.util.ArrayList<>();
        for (int i = 0; i < n; i++) {
            entries.add(Tuple.of(new Key(i, calls), i));
        }
        final java.util.Map<String, Map<Key, Integer>> maps = new java.util.LinkedHashMap<>();
        maps.put("HashMap", HashMap.ofEntries(entries));
        maps.put("LinkedHashMap", LinkedHashMap.ofEntries(entries));
        maps.put("TreeMap", TreeMap.ofEntries(entries));
        for (java.util.Map.Entry<String, Map<Key, Integer>> map : maps.entrySet()) {
            final java.util.Collection<Tuple2<Key, Integer>> view = map.getValue().asJava();
            for (int i : new int[] { 0, n / 2, n - 1 }) {
                calls[0] = 0;
                assertThat(view.contains(Tuple.of(new Key(i, calls), i))).as(map.getKey()).isTrue();
                // one hash lookup, or one descent of a tree of depth at most 2 log2(n): never a walk of the entries
                assertThat(calls[0]).as(map.getKey() + " calls for key " + i).isLessThanOrEqualTo(22);
            }
            calls[0] = 0;
            assertThat(view.contains(Tuple.of(new Key(n, calls), n))).isFalse();
            assertThat(calls[0]).as(map.getKey() + " calls for an absent key").isLessThanOrEqualTo(22);
        }
    }

    @Test
    void shouldAnswerContainsAsJavaUtilTreeMapEntrySetDoes() {
        // the rule: the key is looked up as the map matches keys (a TreeMap by its comparator), then the values are
        // compared with equals; anything that is not a Tuple2, or a key the map's order cannot compare, is false.
        // java.util.TreeMap.entrySet().contains follows the same rule, and so does a java.util.HashMap for the others.
        final java.util.Map<String, Map<Integer, String>> maps = new java.util.LinkedHashMap<>();
        maps.put("HashMap", HashMap.of(1, "a", 2, "b"));
        maps.put("LinkedHashMap", LinkedHashMap.of(1, "a", 2, "b"));
        maps.put("TreeMap", TreeMap.of(1, "a", 2, "b"));
        maps.put("TreeMap(reverse)", TreeMap.of(Comparator.<Integer> reverseOrder(), 1, "a", 2, "b"));
        final java.util.List<Object> probes = java.util.Arrays.asList(Tuple.of(1, "a"), Tuple.of(2, "b"), Tuple.of(1, "b"), Tuple.of(3, "a"),
                Tuple.of("1", "a"), Tuple.of(null, "a"), Tuple.of(1, null), Tuple.of(1, 1), null, "not an entry", java.util.Map.entry(1, "a"),
                Tuple.of(1));
        for (java.util.Map.Entry<String, Map<Integer, String>> map : maps.entrySet()) {
            final java.util.Map<Integer, String> reference = map.getKey().startsWith("TreeMap")
                    ? new java.util.TreeMap<>(map.getValue().asJavaMap()) : new java.util.HashMap<>(map.getValue().asJavaMap());
            for (Object probe : probes) {
                final boolean expected = probe instanceof Tuple2<?, ?> t && lookup(reference, t._1()) && java.util.Objects.equals(reference.get(t._1()), t._2());
                assertThat(map.getValue().asJava().contains(probe)).as(map.getKey() + ".asJava().contains(" + probe + ")").isEqualTo(expected);
            }
        }
    }

    // whether reference holds key, false when its order cannot compare it (java.util.TreeMap throws there)
    private static boolean lookup(java.util.Map<Integer, String> reference, Object key) {
        try {
            return reference.containsKey(key);
        } catch (ClassCastException | NullPointerException e) {
            return false;
        }
    }

    @Test
    void shouldMatchTheKeyByTheComparatorOfATreeMap() {
        final TreeMap<String, Integer> map = TreeMap.of(String.CASE_INSENSITIVE_ORDER, "a", 1, "B", 2);
        final java.util.TreeMap<String, Integer> reference = new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        reference.put("a", 1);
        reference.put("B", 2);
        for (Tuple2<String, Integer> probe : List.of(Tuple.of("A", 1), Tuple.of("a", 1), Tuple.of("b", 2), Tuple.of("A", 2), Tuple.of("c", 1))) {
            assertThat(map.asJava().contains(probe)).as("contains(%s)", probe)
                    .isEqualTo(reference.entrySet().contains(java.util.Map.entry(probe._1(), probe._2())));
        }
        assertThat(map.asJava().contains(Tuple.of("A", 1))).isTrue();
    }

    @Test
    void shouldLetAnExceptionOfAValueEqualsReachTheCaller() {
        record Touchy(int value) {
            @Override
            public boolean equals(Object o) {
                throw new ClassCastException("Touchy.equals");
            }

            @Override
            public int hashCode() {
                return value;
            }
        }
        for (Map<Integer, Touchy> map : List.<Map<Integer, Touchy>> of(HashMap.of(1, new Touchy(1)), LinkedHashMap.of(1, new Touchy(1)), TreeMap.of(1, new Touchy(1)))) {
            assertThatThrownBy(() -> map.asJava().contains(Tuple.of(1, new Touchy(1)))).isInstanceOf(ClassCastException.class).hasMessage("Touchy.equals");
            // a key that is absent never reaches the values
            assertThat(map.asJava().contains(Tuple.of(2, new Touchy(1)))).isFalse();
        }
    }
}
