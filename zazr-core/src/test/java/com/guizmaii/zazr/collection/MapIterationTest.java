package com.guizmaii.zazr.collection;

import com.guizmaii.zazr.Tuple;
import com.guizmaii.zazr.Tuple2;
import com.guizmaii.zazr.collection.internal.Iterator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.function.IntPredicate;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Iteration order, contents and `values()` of the insertion-ordered and sorted maps and sets, checked against the
/// `java.util` collections given the same operations, with removed keys at the head, the tail, in the middle, below and
/// above the proportion that makes a LinkedHashMap compact its insertion order.
public class MapIterationTest {

    private static final int[] SIZES = { 0, 1, 2, 3, 31, 32, 33, 1023, 1024, 1025 };

    // which keys of 0 until size are removed after they were all put
    private static final java.util.List<Removal> REMOVALS = java.util.List.of(
            new Removal("none", size -> i -> false),
            new Removal("head", size -> i -> i == 0),
            new Removal("tail", size -> i -> i == size - 1),
            new Removal("middle", size -> i -> i == size / 2),
            new Removal("both ends", size -> i -> i == 0 || i == size - 1),
            new Removal("every other interior key", size -> i -> i > 0 && i < size - 1 && i % 2 == 1),
            new Removal("two interior keys in three", size -> i -> i > 0 && i < size - 1 && i % 3 != 0),
            new Removal("all but the last", size -> i -> i < size - 1),
            new Removal("all", size -> i -> true));

    private record Removal(String name, java.util.function.IntFunction<IntPredicate> removed) {}

    private static final Comparator<Integer> NATURAL = Comparator.naturalOrder();
    private static final Comparator<Integer> REVERSED = NATURAL.reversed();

    // -- LinkedHashMap

    @Test
    public void shouldIterateLinkedHashMapInInsertionOrderAfterRemovals() {
        for (int size : SIZES) {
            for (Removal removal : REMOVALS) {
                final IntPredicate removed = removal.removed().apply(size);
                LinkedHashMap<Integer, String> actual = LinkedHashMap.empty();
                final java.util.LinkedHashMap<Integer, String> expected = new java.util.LinkedHashMap<>();
                for (int i = 0; i < size; i++) {
                    actual = actual.put(i, "v" + i);
                    expected.put(i, "v" + i);
                }
                for (int i = 0; i < size; i++) {
                    if (removed.test(i)) {
                        actual = actual.remove(i);
                        expected.remove(i);
                    }
                }
                assertLinkedHashMap(actual, expected, size + " " + removal.name());
            }
        }
    }

    @Test
    public void shouldIterateLinkedHashMapAfterRemovalsAndReinsertions() {
        for (int size : SIZES) {
            LinkedHashMap<Integer, String> actual = LinkedHashMap.empty();
            final java.util.LinkedHashMap<Integer, String> expected = new java.util.LinkedHashMap<>();
            for (int i = 0; i < size; i++) {
                actual = actual.put(i, "v" + i);
                expected.put(i, "v" + i);
            }
            // remove two keys in three, put a third of them back (at the end) and replace some values in place
            for (int i = 0; i < size; i++) {
                if (i % 3 != 0) {
                    actual = actual.remove(i);
                    expected.remove(i);
                }
            }
            for (int i = 0; i < size; i += 3) {
                actual = actual.put(i + 1, "w" + i).put(i, "x" + i);
                expected.put(i + 1, "w" + i);
                expected.put(i, "x" + i);
            }
            assertLinkedHashMap(actual, expected, "size " + size);
        }
    }

    @Test
    public void shouldIterateOlderLinkedHashMapVersionsUnchanged() {
        for (int size : SIZES) {
            if (size == 0) {
                continue;
            }
            LinkedHashMap<Integer, String> m0 = LinkedHashMap.empty();
            final java.util.LinkedHashMap<Integer, String> e0 = new java.util.LinkedHashMap<>();
            for (int i = 0; i < size; i++) {
                m0 = m0.put(i, "v" + i);
                e0.put(i, "v" + i);
            }
            final int k = size / 2;
            final LinkedHashMap<Integer, String> removed = m0.remove(k);
            final LinkedHashMap<Integer, String> added = m0.put(size, "new");
            final LinkedHashMap<Integer, String> replaced = m0.put(k, "replaced");
            final LinkedHashMap<Integer, String> reinserted = m0.remove(k).put(k, "again");
            final LinkedHashMap<Integer, String> headRemoved = m0.remove(0);

            final java.util.LinkedHashMap<Integer, String> eAdded = new java.util.LinkedHashMap<>(e0);
            eAdded.put(size, "new");
            final java.util.LinkedHashMap<Integer, String> eReplaced = new java.util.LinkedHashMap<>(e0);
            eReplaced.put(k, "replaced");
            final java.util.LinkedHashMap<Integer, String> eReinserted = withoutKey(e0, k);
            eReinserted.put(k, "again");

            assertLinkedHashMap(removed, withoutKey(e0, k), "removed " + size);
            assertLinkedHashMap(added, eAdded, "added " + size);
            assertLinkedHashMap(replaced, eReplaced, "replaced " + size);
            assertLinkedHashMap(reinserted, eReinserted, "reinserted " + size);
            assertLinkedHashMap(headRemoved, withoutKey(e0, 0), "head removed " + size);
            // the version they all came from is untouched
            assertLinkedHashMap(m0, e0, "original " + size);
            final java.util.LinkedHashMap<Integer, String> eBack = withoutKey(e0, k);
            eBack.put(k, "back");
            assertLinkedHashMap(removed.put(k, "back"), eBack, "removed then back " + size);
        }
    }

    @Test
    public void shouldKeepAnIteratorOfAnOlderLinkedHashMapValidAfterWrites() {
        LinkedHashMap<Integer, Integer> map = LinkedHashMap.empty();
        for (int i = 0; i < 100; i++) {
            map = map.put(i, i);
        }
        final java.util.Iterator<Tuple2<Integer, Integer>> iterator = map.iterator();
        LinkedHashMap<Integer, Integer> other = map;
        for (int i = 0; i < 100; i += 2) {
            other = other.remove(i).put(i + 1000, i);
        }
        final java.util.List<Integer> keys = new ArrayList<>();
        iterator.forEachRemaining(entry -> keys.add(entry._1()));
        assertThat(keys).containsExactlyElementsOf(java.util.stream.IntStream.range(0, 100).boxed().toList());
    }

    @Test
    public void shouldReturnLinkedHashSetElementsInInsertionOrderAfterRemovals() {
        for (int size : SIZES) {
            for (Removal removal : REMOVALS) {
                final IntPredicate removed = removal.removed().apply(size);
                LinkedHashSet<Integer> actual = LinkedHashSet.empty();
                final java.util.LinkedHashSet<Integer> expected = new java.util.LinkedHashSet<>();
                for (int i = 0; i < size; i++) {
                    actual = actual.add(i);
                    expected.add(i);
                }
                for (int i = 0; i < size; i++) {
                    if (removed.test(i)) {
                        actual = actual.remove(i);
                        expected.remove(i);
                    }
                }
                assertThat(javaList(actual)).as(size + " " + removal.name()).containsExactlyElementsOf(expected);
            }
        }
    }

    // -- TreeMap

    @Test
    public void shouldIterateTreeMapInKeyOrderAfterRemovals() {
        for (Comparator<Integer> order : java.util.List.of(NATURAL, REVERSED)) {
            for (int size : SIZES) {
                for (Removal removal : REMOVALS) {
                    final IntPredicate removed = removal.removed().apply(size);
                    TreeMap<Integer, String> actual = TreeMap.empty(order);
                    final java.util.TreeMap<Integer, String> expected = new java.util.TreeMap<>(order);
                    // inserted in a scrambled order so that the tree is built by rebalancing
                    for (int j = 0; j < size; j++) {
                        final int i = (int) ((j * 7919L) % Math.max(size, 1));
                        actual = actual.put(i, "v" + i);
                        expected.put(i, "v" + i);
                    }
                    for (int i = 0; i < size; i++) {
                        if (removed.test(i)) {
                            actual = actual.remove(i);
                            expected.remove(i);
                        }
                    }
                    assertTreeMap(actual, expected, order + " " + size + " " + removal.name());
                }
            }
        }
    }

    @Test
    public void shouldIterateTreeMapBuiltFromOrderedEntries() {
        for (int size : SIZES) {
            final java.util.List<Tuple2<Integer, String>> entries = new ArrayList<>();
            final java.util.TreeMap<Integer, String> expected = new java.util.TreeMap<>();
            for (int i = 0; i < size; i++) {
                entries.add(Tuple.of(i, "v" + i));
                expected.put(i, "v" + i);
            }
            assertTreeMap(TreeMap.ofEntries(entries), expected, "ofEntries " + size);
        }
    }

    @Test
    public void shouldIterateOlderTreeMapVersionsUnchanged() {
        for (int size : SIZES) {
            if (size == 0) {
                continue;
            }
            TreeMap<Integer, String> m0 = TreeMap.empty();
            final java.util.TreeMap<Integer, String> e0 = new java.util.TreeMap<>();
            for (int i = 0; i < size; i++) {
                m0 = m0.put(i, "v" + i);
                e0.put(i, "v" + i);
            }
            final int k = size / 2;
            final TreeMap<Integer, String> removed = m0.remove(k);
            final TreeMap<Integer, String> added = m0.put(-1, "new");
            final TreeMap<Integer, String> replaced = m0.put(k, "replaced");

            final java.util.TreeMap<Integer, String> eRemoved = new java.util.TreeMap<>(e0);
            eRemoved.remove(k);
            final java.util.TreeMap<Integer, String> eAdded = new java.util.TreeMap<>(e0);
            eAdded.put(-1, "new");
            final java.util.TreeMap<Integer, String> eReplaced = new java.util.TreeMap<>(e0);
            eReplaced.put(k, "replaced");

            assertTreeMap(removed, eRemoved, "removed " + size);
            assertTreeMap(added, eAdded, "added " + size);
            assertTreeMap(replaced, eReplaced, "replaced " + size);
            assertTreeMap(m0, e0, "original " + size);
        }
    }

    @Test
    public void shouldIterateTreeSetInOrderAfterRemovals() {
        for (Comparator<Integer> order : java.util.List.of(NATURAL, REVERSED)) {
            for (int size : SIZES) {
                for (Removal removal : REMOVALS) {
                    final IntPredicate removed = removal.removed().apply(size);
                    TreeSet<Integer> actual = TreeSet.empty(order);
                    final java.util.TreeSet<Integer> expected = new java.util.TreeSet<>(order);
                    for (int j = 0; j < size; j++) {
                        final int i = (int) ((j * 7919L) % Math.max(size, 1));
                        actual = actual.add(i);
                        expected.add(i);
                    }
                    for (int i = 0; i < size; i++) {
                        if (removed.test(i)) {
                            actual = actual.remove(i);
                            expected.remove(i);
                        }
                    }
                    assertThat(javaList(actual)).as(order + " " + size + " " + removal.name())
                            .containsExactlyElementsOf(expected);
                }
            }
        }
    }

    // -- helpers

    private static <T> java.util.List<T> javaList(Iterable<T> elements) {
        final java.util.List<T> result = new ArrayList<>();
        elements.forEach(result::add);
        return result;
    }

    private static <K, V> java.util.LinkedHashMap<K, V> withoutKey(java.util.LinkedHashMap<K, V> map, K key) {
        final java.util.LinkedHashMap<K, V> copy = new java.util.LinkedHashMap<>(map);
        copy.remove(key);
        return copy;
    }

    private static <K, V> void assertLinkedHashMap(LinkedHashMap<K, V> actual, java.util.LinkedHashMap<K, V> expected, String description) {
        assertEntries(actual, expected, description);
        // the reversed view walks the insertion order backwards without the forward iterator
        final java.util.List<K> reversedKeys = new ArrayList<>(actual.asJavaMap().reversed().keySet());
        java.util.Collections.reverse(reversedKeys);
        assertThat(reversedKeys).as(description + " reversed view").containsExactlyElementsOf(expected.keySet());
        assertThat(javaList(actual.keySet())).as(description + " keySet").containsExactlyElementsOf(expected.keySet());
    }

    private static <K, V> void assertTreeMap(TreeMap<K, V> actual, java.util.TreeMap<K, V> expected, String description) {
        assertEntries(actual, expected, description);
    }

    private static <K, V> void assertEntries(Map<K, V> actual, java.util.Map<K, V> expected, String description) {
        final java.util.List<Tuple2<K, V>> expectedEntries = new ArrayList<>();
        expected.forEach((k, v) -> expectedEntries.add(Tuple.of(k, v)));
        final java.util.List<V> expectedValues = new ArrayList<>(expected.values());

        final java.util.List<Tuple2<K, V>> iterated = new ArrayList<>();
        final java.util.Iterator<Tuple2<K, V>> iterator = actual.iterator();
        while (iterator.hasNext()) {
            // hasNext() is idempotent: asking twice does not skip an entry
            assertThat(iterator.hasNext()).isTrue();
            iterated.add(iterator.next());
        }
        assertThat(iterator.hasNext()).isFalse();
        assertThat(iterated).as(description + " iterator").containsExactlyElementsOf(expectedEntries);

        final java.util.List<Tuple2<K, V>> visited = new ArrayList<>();
        actual.forEach((k, v) -> visited.add(Tuple.of(k, v)));
        assertThat(visited).as(description + " forEach").containsExactlyElementsOf(expectedEntries);

        final Vector<V> values = actual.values();
        assertThat(values.size()).as(description + " values size").isEqualTo(expected.size());
        assertThat(javaList(values)).as(description + " values").containsExactlyElementsOf(expectedValues);
        assertThat(values).as(description + " values against the entries").isEqualTo(Vector.ofAll(Iterator.ofAll(iterated).map(Tuple2::_2)));
        assertThat(actual.size()).as(description + " size").isEqualTo(expected.size());
    }
}
