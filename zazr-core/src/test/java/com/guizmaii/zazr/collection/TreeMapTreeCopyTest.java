package com.guizmaii.zazr.collection;

import com.guizmaii.zazr.Tuple;
import com.guizmaii.zazr.Tuple2;
import com.guizmaii.zazr.collection.internal.Iterator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Random;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@code keySet}, {@code mapValues} and {@code replaceAll(BiFunction)} of TreeMap copy the entry tree keeping its
 * shape: the result equals the one of rebuilding a tree from the entries, under the same comparator.
 */
public class TreeMapTreeCopyTest {

    private static final long SEED = 20260925L;
    private static final int[] SIZES = { 0, 1, 2, 3, 31, 32, 33, 1023, 1024, 1025 };

    private static final Comparator<Integer> NATURAL = Comparator.naturalOrder();
    private static final Comparator<Integer> REVERSED = Comparator.reverseOrder();
    // two keys a multiple of 1000 apart are the same key: the map keeps the one put last
    private static final Comparator<Integer> MODULO = Comparator.comparingInt(i -> Math.floorMod(i, 1000));

    private static TreeMap<Integer, String> randomMap(Comparator<Integer> order, int size, Random random) {
        TreeMap<Integer, String> map = TreeMap.empty(order);
        while (map.size() < size) {
            final int key = random.nextInt(4 * size + 1) - 2 * size;
            map = map.put(key, "v" + key);
        }
        // some deletions, so that the shape is not only the one of insertions
        final java.util.List<Integer> keys = new ArrayList<>();
        map.forEach(entry -> keys.add(entry._1()));
        for (Integer key : keys.subList(0, size / 3)) {
            map = map.remove(key);
        }
        while (map.size() < size) {
            final int key = random.nextInt(4 * size + 1) - 2 * size;
            map = map.put(key, "w" + key);
        }
        return map;
    }

    private static <K, V> SortedSet<K> keySetByRebuilding(TreeMap<K, V> map) {
        return TreeSet.ofAll(map.comparator(), Iterator.ofAll(map).map(Tuple2::_1));
    }

    private static <T> java.util.List<T> list(Iterable<T> iterable) {
        final java.util.List<T> result = new ArrayList<>();
        iterable.forEach(result::add);
        return result;
    }

    private static java.util.List<Comparator<Integer>> orders() {
        return java.util.List.of(NATURAL, REVERSED, MODULO);
    }

    @Test
    public void keySetEqualsTheRebuiltKeySetAtEveryBoundary() {
        final Random random = new Random(SEED);
        for (Comparator<Integer> order : orders()) {
            for (int size : SIZES) {
                final TreeMap<Integer, String> map = randomMap(order, Math.min(size, order == MODULO ? 1000 : size), random);
                final SortedSet<Integer> keys = map.keySet();
                final SortedSet<Integer> expected = keySetByRebuilding(map);
                assertThat(keys).isEqualTo(expected);
                assertThat(keys.size()).isEqualTo(map.size());
                assertThat(list(keys)).isEqualTo(list(expected));
                assertThat(list(keys)).isEqualTo(list(Iterator.ofAll(map).map(Tuple2::_1)));
                assertThat(keys.comparator()).isSameAs(map.comparator());
                for (Tuple2<Integer, String> entry : map) {
                    assertThat(keys.contains(entry._1())).isTrue();
                }
            }
        }
    }

    @Test
    public void keySetOfAnEmptyMapKeepsTheComparator() {
        final TreeMap<Integer, String> map = TreeMap.empty(REVERSED);
        final SortedSet<Integer> keys = map.keySet();
        assertThat(keys.isEmpty()).isTrue();
        assertThat(keys.comparator()).isSameAs(REVERSED);
        assertThat(list(keys.add(1).add(2))).containsExactly(2, 1);
    }

    @Test
    public void keySetHoldsTheKeyPutLastUnderACaseInsensitiveComparator() {
        final TreeMap<String, Integer> map = TreeMap.<String, Integer> empty(String.CASE_INSENSITIVE_ORDER)
                .put("b", 1).put("A", 2).put("c", 3).put("a", 4).put("B", 5);
        final SortedSet<String> keys = map.keySet();
        assertThat(list(keys)).containsExactly("a", "B", "c");
        assertThat(list(keys)).isEqualTo(list(keySetByRebuilding(map)));
        assertThat(keys.contains("C")).isTrue();
        assertThat(keys.comparator()).isSameAs(String.CASE_INSENSITIVE_ORDER);
    }

    @Test
    public void keySetIsAPersistentTreeSet() {
        final TreeMap<Integer, String> map = randomMap(NATURAL, 1025, new Random(SEED));
        final java.util.List<Tuple2<Integer, String>> entriesBefore = list(map);
        final SortedSet<Integer> keys = map.keySet();
        final java.util.TreeSet<Integer> reference = new java.util.TreeSet<>(list(keys));
        final Random random = new Random(SEED + 1);
        SortedSet<Integer> changed = keys;
        for (int i = 0; i < 2000; i++) {
            final int element = random.nextInt(8000) - 4000;
            if (random.nextBoolean()) {
                changed = changed.add(element);
                reference.add(element);
            } else {
                changed = changed.remove(element);
                reference.remove(element);
            }
        }
        assertThat(list(changed)).isEqualTo(new ArrayList<>(reference));
        assertThat(keys).isEqualTo(keySetByRebuilding(map));
        assertThat(list(map)).isEqualTo(entriesBefore);
        assertThat(keys.head()).isEqualTo(map.head()._1());
        assertThat(keys.last()).isEqualTo(map.last()._1());
        assertThat(list(keys.take(10))).isEqualTo(list(Iterator.ofAll(map.take(10)).map(Tuple2::_1)));
    }

    @Test
    public void mapValuesEqualsMappingEveryEntryAtEveryBoundary() {
        final Random random = new Random(SEED);
        for (Comparator<Integer> order : orders()) {
            for (int size : SIZES) {
                final TreeMap<Integer, String> map = randomMap(order, Math.min(size, order == MODULO ? 1000 : size), random);
                final java.util.List<String> calls = new ArrayList<>();
                final TreeMap<Integer, Integer> actual = map.mapValues(v -> {
                    calls.add(v);
                    return v.length();
                });
                final TreeMap<Integer, Integer> expected = map.map(map.comparator(), (k, v) -> Tuple.of(k, v.length()));
                assertThat(actual).isEqualTo(expected);
                assertThat(list(actual)).isEqualTo(list(expected));
                assertThat(actual.comparator()).isSameAs(map.comparator());
                assertThat(calls).as("called once per entry, in key order").isEqualTo(list(map.values()));
                for (Tuple2<Integer, String> entry : map) {
                    assertThat(actual.get(entry._1()).get()).isEqualTo(entry._2().length());
                }
            }
        }
    }

    @Test
    public void replaceAllEqualsMappingEveryEntryAtEveryBoundary() {
        final Random random = new Random(SEED);
        for (Comparator<Integer> order : orders()) {
            for (int size : SIZES) {
                final TreeMap<Integer, String> map = randomMap(order, Math.min(size, order == MODULO ? 1000 : size), random);
                final java.util.List<Integer> calls = new ArrayList<>();
                final TreeMap<Integer, String> actual = map.replaceAll((k, v) -> {
                    calls.add(k);
                    return v + "/" + k;
                });
                final TreeMap<Integer, String> expected = map.map(map.comparator(), (k, v) -> Tuple.of(k, v + "/" + k));
                assertThat(actual).isEqualTo(expected);
                assertThat(list(actual)).isEqualTo(list(expected));
                assertThat(actual.comparator()).isSameAs(map.comparator());
                assertThat(calls).as("called once per entry, in key order").isEqualTo(list(map.keySet()));
                // the new map can be updated like any other
                if (!actual.isEmpty()) {
                    final Integer first = actual.head()._1();
                    assertThat(actual.remove(first).size()).isEqualTo(map.size() - 1);
                    assertThat(actual.put(first, "x").get(first).get()).isEqualTo("x");
                }
            }
        }
    }

    @Test
    public void mapValuesAndReplaceAllKeepTheKeysOfACaseInsensitiveMap() {
        final TreeMap<String, Integer> map = TreeMap.<String, Integer> empty(String.CASE_INSENSITIVE_ORDER)
                .put("b", 1).put("A", 2).put("c", 3).put("a", 4);
        assertThat(list(map.mapValues(v -> v * 10)))
                .containsExactly(Tuple.of("a", 40), Tuple.of("b", 10), Tuple.of("c", 30));
        assertThat(list(map.replaceAll((k, v) -> k.charAt(0) * 100 + v)))
                .containsExactly(Tuple.of("a", 'a' * 100 + 4), Tuple.of("b", 'b' * 100 + 1), Tuple.of("c", 'c' * 100 + 3));
    }

    @Test
    public void mapValuesAndReplaceAllOfAnEmptyMapKeepTheComparator() {
        final TreeMap<Integer, String> map = TreeMap.empty(REVERSED);
        final TreeMap<Integer, Integer> mapped = map.mapValues(String::length);
        assertThat(mapped.isEmpty()).isTrue();
        assertThat(mapped.comparator()).isSameAs(REVERSED);
        assertThat(list(mapped.put(1, 1).put(2, 2).keySet())).containsExactly(2, 1);
        final TreeMap<Integer, String> replaced = map.replaceAll((k, v) -> v);
        assertThat(replaced.isEmpty()).isTrue();
        assertThat(replaced.comparator()).isSameAs(REVERSED);
    }

    @Test
    public void mapValuesAndReplaceAllRejectANullValue() {
        final TreeMap<Integer, String> map = TreeMap.of(1, "a", 2, "b", 3, "c");
        assertThatThrownBy(() -> map.mapValues(v -> v.equals("b") ? null : v))
                .isInstanceOf(NullPointerException.class).hasMessage("TreeMap: value is null");
        assertThatThrownBy(() -> map.replaceAll((k, v) -> k == 3 ? null : v))
                .isInstanceOf(NullPointerException.class).hasMessage("TreeMap: value is null");
        assertThatThrownBy(() -> map.mapValues(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    public void mapValuesAndReplaceAllLeaveTheMapUnchanged() {
        final TreeMap<Integer, String> map = randomMap(NATURAL, 1024, new Random(SEED));
        final java.util.List<Tuple2<Integer, String>> before = list(map);
        map.mapValues(String::length);
        map.replaceAll((k, v) -> v + k);
        assertThat(list(map)).isEqualTo(before);
    }
}
