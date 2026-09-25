package com.guizmaii.zazr.collection;

import com.guizmaii.zazr.Tuple;
import com.guizmaii.zazr.Tuple2;
import java.util.Comparator;
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
}
