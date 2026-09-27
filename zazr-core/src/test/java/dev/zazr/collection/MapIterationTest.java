package dev.zazr.collection;

import dev.zazr.Tuple;
import dev.zazr.Tuple2;
import dev.zazr.collection.internal.Iterator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.function.IntPredicate;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Iteration order, contents and `values()` of the insertion-ordered and sorted maps and sets, checked against the
/// `java.util` collections given the same operations, with removed keys at the head, the tail, in the middle, below and
/// above the proportion that makes a LinkedHashMap compact its insertion order.
public class MapIterationTest {

    private static final int[] SIZES = {0, 1, 2, 3, 31, 32, 33, 1023, 1024, 1025};

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
                IntPredicate removed = removal.removed().apply(size);
                java.util.LinkedHashMap<Integer, String> expected = ascendingJava(size);
                Vector<Integer> gone = Vector.range(0, size).filter(removed::test);
                gone.forEach(expected::remove);
                LinkedHashMap<Integer, String> actual = gone.foldLeft(ascending(size), LinkedHashMap::remove);
                assertLinkedHashMap(actual, expected, size + " " + removal.name());
            }
        }
    }

    @Test
    public void shouldIterateLinkedHashMapAfterRemovalsAndReinsertions() {
        for (int size : SIZES) {
            java.util.LinkedHashMap<Integer, String> expected = ascendingJava(size);
            // remove two keys in three, put a third of them back (at the end) and replace some values in place
            Vector<Integer> gone = Vector.range(0, size).filter(i -> i % 3 != 0);
            gone.forEach(expected::remove);
            Vector<Integer> thirds = Vector.rangeBy(0, size, 3);
            thirds.forEach(i -> {
                expected.put(i + 1, "w" + i);
                expected.put(i, "x" + i);
            });
            LinkedHashMap<Integer, String> actual = thirds.foldLeft(
                    gone.foldLeft(ascending(size), LinkedHashMap::remove),
                    (acc, i) -> acc.put(i + 1, "w" + i).put(i, "x" + i));
            assertLinkedHashMap(actual, expected, "size " + size);
        }
    }

    @Test
    public void shouldIterateOlderLinkedHashMapVersionsUnchanged() {
        for (int size : SIZES) {
            if (size == 0) {
                continue;
            }
            LinkedHashMap<Integer, String> m0 = ascending(size);
            java.util.LinkedHashMap<Integer, String> e0 = ascendingJava(size);
            int k = size / 2;
            LinkedHashMap<Integer, String> removed = m0.remove(k);
            LinkedHashMap<Integer, String> added = m0.put(size, "new");
            LinkedHashMap<Integer, String> replaced = m0.put(k, "replaced");
            LinkedHashMap<Integer, String> reinserted = m0.remove(k).put(k, "again");
            LinkedHashMap<Integer, String> headRemoved = m0.remove(0);

            java.util.LinkedHashMap<Integer, String> eAdded = new java.util.LinkedHashMap<>(e0);
            eAdded.put(size, "new");
            java.util.LinkedHashMap<Integer, String> eReplaced = new java.util.LinkedHashMap<>(e0);
            eReplaced.put(k, "replaced");
            java.util.LinkedHashMap<Integer, String> eReinserted = withoutKey(e0, k);
            eReinserted.put(k, "again");

            assertLinkedHashMap(removed, withoutKey(e0, k), "removed " + size);
            assertLinkedHashMap(added, eAdded, "added " + size);
            assertLinkedHashMap(replaced, eReplaced, "replaced " + size);
            assertLinkedHashMap(reinserted, eReinserted, "reinserted " + size);
            assertLinkedHashMap(headRemoved, withoutKey(e0, 0), "head removed " + size);
            // the version they all came from is untouched
            assertLinkedHashMap(m0, e0, "original " + size);
            java.util.LinkedHashMap<Integer, String> eBack = withoutKey(e0, k);
            eBack.put(k, "back");
            assertLinkedHashMap(removed.put(k, "back"), eBack, "removed then back " + size);
        }
    }

    @Test
    public void shouldKeepAnIteratorOfAnOlderLinkedHashMapValidAfterWrites() {
        LinkedHashMap<Integer, Integer> map =
                Vector.range(0, 100).foldLeft(LinkedHashMap.<Integer, Integer>empty(), (acc, i) -> acc.put(i, i));
        java.util.Iterator<Tuple2<Integer, Integer>> iterator = map.iterator();
        Vector.rangeBy(0, 100, 2).foldLeft(map, (acc, i) -> acc.remove(i).put(i + 1000, i));
        java.util.List<Integer> keys = new ArrayList<>();
        iterator.forEachRemaining(entry -> keys.add(entry._1()));
        assertThat(keys)
                .containsExactlyElementsOf(
                        java.util.stream.IntStream.range(0, 100).boxed().toList());
    }

    @Test
    public void shouldReturnLinkedHashSetElementsInInsertionOrderAfterRemovals() {
        for (int size : SIZES) {
            for (Removal removal : REMOVALS) {
                IntPredicate removed = removal.removed().apply(size);
                java.util.LinkedHashSet<Integer> expected =
                        new java.util.LinkedHashSet<>(Vector.range(0, size).asJava());
                Vector<Integer> gone = Vector.range(0, size).filter(removed::test);
                gone.forEach(expected::remove);
                LinkedHashSet<Integer> actual = gone.foldLeft(
                        Vector.range(0, size).foldLeft(LinkedHashSet.empty(), LinkedHashSet::add),
                        LinkedHashSet::remove);
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
                    IntPredicate removed = removal.removed().apply(size);
                    java.util.TreeMap<Integer, String> expected = new java.util.TreeMap<>(order);
                    // inserted in a scrambled order so that the tree is built by rebalancing
                    Vector<Integer> scrambled = Vector.range(0, size).map(j -> (int) ((j * 7919L) % Math.max(size, 1)));
                    scrambled.forEach(i -> expected.put(i, "v" + i));
                    Vector<Integer> gone = Vector.range(0, size).filter(removed::test);
                    gone.forEach(expected::remove);
                    TreeMap<Integer, String> actual = gone.foldLeft(
                            scrambled.foldLeft(TreeMap.<Integer, String>empty(order), (acc, i) -> acc.put(i, "v" + i)),
                            TreeMap::remove);
                    assertTreeMap(actual, expected, order + " " + size + " " + removal.name());
                }
            }
        }
    }

    @Test
    public void shouldIterateTreeMapBuiltFromOrderedEntries() {
        for (int size : SIZES) {
            java.util.List<Tuple2<Integer, String>> entries = new ArrayList<>();
            java.util.TreeMap<Integer, String> expected = new java.util.TreeMap<>();
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
            TreeMap<Integer, String> m0 =
                    Vector.range(0, size).foldLeft(TreeMap.<Integer, String>empty(), (acc, i) -> acc.put(i, "v" + i));
            java.util.TreeMap<Integer, String> e0 = new java.util.TreeMap<>();
            for (int i = 0; i < size; i++) {
                e0.put(i, "v" + i);
            }
            int k = size / 2;
            TreeMap<Integer, String> removed = m0.remove(k);
            TreeMap<Integer, String> added = m0.put(-1, "new");
            TreeMap<Integer, String> replaced = m0.put(k, "replaced");

            java.util.TreeMap<Integer, String> eRemoved = new java.util.TreeMap<>(e0);
            eRemoved.remove(k);
            java.util.TreeMap<Integer, String> eAdded = new java.util.TreeMap<>(e0);
            eAdded.put(-1, "new");
            java.util.TreeMap<Integer, String> eReplaced = new java.util.TreeMap<>(e0);
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
                    IntPredicate removed = removal.removed().apply(size);
                    java.util.TreeSet<Integer> expected = new java.util.TreeSet<>(order);
                    Vector<Integer> scrambled = Vector.range(0, size).map(j -> (int) ((j * 7919L) % Math.max(size, 1)));
                    expected.addAll(scrambled.asJava());
                    Vector<Integer> gone = Vector.range(0, size).filter(removed::test);
                    gone.forEach(expected::remove);
                    TreeSet<Integer> actual =
                            gone.foldLeft(scrambled.foldLeft(TreeSet.empty(order), TreeSet::add), TreeSet::remove);
                    assertThat(javaList(actual))
                            .as(order + " " + size + " " + removal.name())
                            .containsExactlyElementsOf(expected);
                }
            }
        }
    }

    // -- helpers

    private static <T> java.util.List<T> javaList(Iterable<T> elements) {
        java.util.List<T> result = new ArrayList<>();
        elements.forEach(result::add);
        return result;
    }

    private static <K, V> java.util.LinkedHashMap<K, V> withoutKey(java.util.LinkedHashMap<K, V> map, K key) {
        java.util.LinkedHashMap<K, V> copy = new java.util.LinkedHashMap<>(map);
        copy.remove(key);
        return copy;
    }

    private static <K, V> void assertLinkedHashMap(
            LinkedHashMap<K, V> actual, java.util.LinkedHashMap<K, V> expected, String description) {
        assertEntries(actual, expected, description);
        // the reversed view walks the insertion order backwards without the forward iterator
        java.util.List<K> reversedKeys =
                new ArrayList<>(actual.asJavaMap().reversed().keySet());
        java.util.Collections.reverse(reversedKeys);
        assertThat(reversedKeys).as(description + " reversed view").containsExactlyElementsOf(expected.keySet());
        assertThat(javaList(actual.keySet())).as(description + " keySet").containsExactlyElementsOf(expected.keySet());
    }

    private static <K, V> void assertTreeMap(
            TreeMap<K, V> actual, java.util.TreeMap<K, V> expected, String description) {
        assertEntries(actual, expected, description);
    }

    private static <K, V> void assertEntries(Map<K, V> actual, java.util.Map<K, V> expected, String description) {
        java.util.List<Tuple2<K, V>> expectedEntries = new ArrayList<>();
        expected.forEach((k, v) -> expectedEntries.add(Tuple.of(k, v)));
        java.util.List<V> expectedValues = new ArrayList<>(expected.values());

        java.util.List<Tuple2<K, V>> iterated = new ArrayList<>();
        java.util.Iterator<Tuple2<K, V>> iterator = actual.iterator();
        while (iterator.hasNext()) {
            // hasNext() is idempotent: asking twice does not skip an entry
            assertThat(iterator.hasNext()).isTrue();
            iterated.add(iterator.next());
        }
        assertThat(iterator.hasNext()).isFalse();
        assertThat(iterated).as(description + " iterator").containsExactlyElementsOf(expectedEntries);

        java.util.List<Tuple2<K, V>> visited = new ArrayList<>();
        actual.forEach((k, v) -> visited.add(Tuple.of(k, v)));
        assertThat(visited).as(description + " forEach").containsExactlyElementsOf(expectedEntries);

        Vector<V> values = actual.values();
        assertThat(values.size()).as(description + " values size").isEqualTo(expected.size());
        assertThat(javaList(values)).as(description + " values").containsExactlyElementsOf(expectedValues);
        assertThat(values)
                .as(description + " values against the entries")
                .isEqualTo(Vector.ofAll(Iterator.ofAll(iterated).map(Tuple2::_2)));
        assertThat(actual.size()).as(description + " size").isEqualTo(expected.size());
    }

    // the keys 0 to size - 1, each mapped to "v" and itself, put in ascending order
    private static LinkedHashMap<Integer, String> ascending(int size) {
        return Vector.range(0, size).foldLeft(LinkedHashMap.<Integer, String>empty(), (map, i) -> map.put(i, "v" + i));
    }

    // the java.util.LinkedHashMap of the same entries
    private static java.util.LinkedHashMap<Integer, String> ascendingJava(int size) {
        java.util.LinkedHashMap<Integer, String> map = new java.util.LinkedHashMap<>();
        for (int i = 0; i < size; i++) {
            map.put(i, "v" + i);
        }
        return map;
    }
}
