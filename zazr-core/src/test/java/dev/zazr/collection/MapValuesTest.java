package dev.zazr.collection;

import dev.zazr.Tuple2;
import java.util.Comparator;
import java.util.Random;
import java.util.function.IntFunction;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// `values()` of `HashMap`, `TreeMap` and `LinkedHashMap` (and of the non-empty maps that wrap them) against the
/// Vector built the old way, one value at a time in the iteration order of the entries: the same values, as the same
/// objects, in the same order, at the sizes around the leaf and branch boundaries of the Vector.
public class MapValuesTest {

    private static final int[] SIZES = {0, 1, 2, 31, 32, 33, 1023, 1024, 1025, 32768, 40000};

    /// A value with an identity of its own: no two values are the same object unless the test shares one on purpose.
    private record Value(int id) {}

    /// A key whose hash code is `hash`: keys of one hash and different ids collide.
    private record Colliding(int hash, int id) {
        @Override
        public int hashCode() {
            return hash;
        }
    }

    /// The Vector of the values, built one by one in the iteration order of the entries.
    private static <K, V> Vector<V> reference(Map<K, V> map) {
        Vector.Builder<V> builder = Vector.newBuilder();
        for (Tuple2<K, V> entry : map) {
            builder.add(entry._2());
        }
        return builder.result();
    }

    private static <K, V> void assertValuesOf(Map<K, V> map) {
        Vector<V> expected = reference(map);
        Vector<V> values = map.values();
        assertThat(values.size()).isEqualTo(map.size());
        assertThat(values).isEqualTo(expected);
        for (int i = 0; i < expected.size(); i++) {
            assertThat(values.get(i)).isSameAs(expected.get(i));
        }
        // the Vector behaves as one built element by element: iteration, a slice, and an update that leaves it as is
        java.util.Iterator<V> iterator = values.iterator();
        for (V value : expected) {
            assertThat(iterator.next()).isSameAs(value);
        }
        assertThat(iterator.hasNext()).isFalse();
        assertThat(values.slice(1, values.size() - 1)).isEqualTo(expected.slice(1, expected.size() - 1));
        if (!values.isEmpty()) {
            Vector<V> updated = values.update(0, values.get(values.size() - 1));
            assertThat(updated.get(0)).isSameAs(expected.get(expected.size() - 1));
            assertThat(values.get(0)).isSameAs(expected.get(0));
        }
        // each call gives a Vector of its own
        assertThat(map.values()).isEqualTo(expected);
    }

    private static <K> HashMap<K, Value> hashMapOf(int size, IntFunction<K> key) {
        HashMap.Builder<K, Value> builder = HashMap.newBuilder();
        for (int i = 0; i < size; i++) {
            builder.put(key.apply(i), new Value(i));
        }
        return builder.result();
    }

    private static <K> LinkedHashMap<K, Value> linkedHashMapOf(int size, IntFunction<K> key) {
        LinkedHashMap.Builder<K, Value> builder = LinkedHashMap.newBuilder();
        for (int i = 0; i < size; i++) {
            builder.put(key.apply(i), new Value(i));
        }
        return builder.result();
    }

    private static TreeMap<Integer, Value> treeMapOf(
            int size, Comparator<Integer> comparator, IntFunction<Integer> key) {
        TreeMap.Builder<Integer, Value> builder = TreeMap.newBuilder(comparator);
        for (int i = 0; i < size; i++) {
            builder.put(key.apply(i), new Value(i));
        }
        return builder.result();
    }

    // a permutation of 0 .. size - 1, so that the keys do not come in order
    private static IntFunction<Integer> shuffled(int size) {
        java.util.List<Integer> keys =
                new java.util.ArrayList<>(Vector.range(0, size).asJava());
        java.util.Collections.shuffle(keys, new Random(309));
        return keys::get;
    }

    @Test
    public void valuesOfAnEmptyMapAreTheEmptyVector() {
        assertThat(HashMap.<Integer, Value>empty().values()).isSameAs(Vector.empty());
        assertThat(HashMap.of(1, new Value(1)).remove(1).values()).isSameAs(Vector.empty());
        assertThat(TreeMap.<Integer, Value>empty().values()).isSameAs(Vector.empty());
        assertThat(TreeMap.of(1, new Value(1)).remove(1).values()).isSameAs(Vector.empty());
        assertThat(LinkedHashMap.<Integer, Value>empty().values()).isSameAs(Vector.empty());
        assertThat(LinkedHashMap.of(1, new Value(1)).remove(1).values()).isSameAs(Vector.empty());
    }

    @Test
    public void hashMapValuesAreTheValuesInIterationOrder() {
        for (int size : SIZES) {
            assertValuesOf(hashMapOf(size, i -> i));
            assertValuesOf(hashMapOf(size, i -> "k" + i));
            // eight keys per hash: collision nodes beside inline keys and children
            assertValuesOf(hashMapOf(size, i -> new Colliding(i / 8, i)));
            if (size <= 1025) {
                // every key of one hash: one collision node under seven levels of single children
                assertValuesOf(hashMapOf(size, i -> new Colliding(42, i)));
            }
        }
    }

    @Test
    public void hashMapValuesAfterPersistentUpdatesAndRemovals() {
        for (int size : SIZES) {
            HashMap<Integer, Value> map = hashMapOf(size, i -> i);
            // every third key removed, every fifth value replaced: inline entries move into and out of children
            HashMap<Integer, Value> removed = Vector.rangeBy(0, size, 3).foldLeft(map, HashMap::remove);
            HashMap<Integer, Value> replaced =
                    Vector.rangeBy(1, size, 5).foldLeft(removed, (m, i) -> m.put(i, new Value(-i)));
            assertValuesOf(removed);
            assertValuesOf(replaced);
            assertValuesOf(map);
            HashMap<Colliding, Value> colliding = Vector.rangeBy(0, Math.min(size, 1025), 2)
                    .foldLeft(
                            hashMapOf(Math.min(size, 1025), i -> new Colliding(i / 8, i)),
                            (m, i) -> m.remove(new Colliding(i / 8, i)));
            assertValuesOf(colliding);
        }
    }

    @Test
    public void treeMapValuesAreTheValuesInKeyOrder() {
        for (int size : SIZES) {
            IntFunction<Integer> shuffled = shuffled(size);
            assertValuesOf(treeMapOf(size, Comparator.naturalOrder(), i -> i));
            assertValuesOf(treeMapOf(size, Comparator.naturalOrder(), shuffled));
            assertValuesOf(treeMapOf(size, Comparator.reverseOrder(), shuffled));
            // keys put one at a time, then every third removed: the shapes of persistent insertion and deletion
            TreeMap<Integer, Value> persistent = Vector.range(0, size)
                    .foldLeft(TreeMap.<Integer, Value>empty(), (m, i) -> m.put(shuffled.apply(i), new Value(i)));
            assertValuesOf(persistent);
            assertValuesOf(Vector.rangeBy(0, size, 3).foldLeft(persistent, TreeMap::remove));
        }
    }

    @Test
    public void linkedHashMapValuesAreTheValuesInInsertionOrder() {
        for (int size : SIZES) {
            assertValuesOf(linkedHashMapOf(size, i -> i));
            assertValuesOf(linkedHashMapOf(size, shuffled(size)));
            assertValuesOf(linkedHashMapOf(size, i -> new Colliding(i / 8, i)));
        }
    }

    @Test
    public void linkedHashMapValuesAfterRemovalsAndReplacements() throws Exception {
        java.util.Set<String> shapes = new java.util.HashSet<>();
        for (int size : SIZES) {
            LinkedHashMap<Integer, Value> map = linkedHashMapOf(size, shuffled(size));
            java.util.List<LinkedHashMap<Integer, Value>> receivers = java.util.List.of(
                    map,
                    // the keys in the last block only, put one at a time
                    Vector.range(0, Math.min(size, 20))
                            .foldLeft(LinkedHashMap.<Integer, Value>empty(), (m, i) -> m.put(i, new Value(i))),
                    // keys removed inside leaves, and whole leaves removed from the middle
                    Vector.rangeBy(0, size, 3).foldLeft(map, LinkedHashMap::remove),
                    Vector.range(size / 4, size / 2).foldLeft(linkedHashMapOf(size, i -> i), LinkedHashMap::remove),
                    // the last block emptied
                    map.take(size - size % 32),
                    // a value replaced keeps the place of its key; a key removed then put again goes last
                    Vector.rangeBy(0, size, 7).foldLeft(map, (m, i) -> m.put(i, new Value(-i))),
                    Vector.rangeBy(0, size, 11)
                            .foldLeft(map, (m, i) -> m.remove(i).put(i, new Value(-i))));
            for (LinkedHashMap<Integer, Value> receiver : receivers) {
                assertValuesOf(receiver);
                shapes.addAll(InsertionOrderShapes.of(receiver));
            }
        }
        assertThat(shapes).isEqualTo(InsertionOrderShapes.ALL);
    }

    @Test
    public void linkedHashMapValuesNearTheOrdinalLimit() {
        for (int size : new int[] {1, 33, 1025}) {
            LinkedHashMap<Integer, Value> map = Vector.range(0, size)
                    .foldLeft(
                            LinkedHashMap.<Integer, Value>emptyStartingAt(Integer.MAX_VALUE - 40),
                            (m, i) -> m.put(i, new Value(i)));
            assertValuesOf(map);
            assertValuesOf(Vector.rangeBy(0, size, 2).foldLeft(map, LinkedHashMap::remove));
        }
    }

    @Test
    public void sharedValuesAreKeptAsTheSameObjectAtEachPlace() {
        Value shared = new Value(0);
        for (int size : SIZES) {
            HashMap<Integer, Value> hashMap = Vector.range(0, size)
                    .foldLeft(HashMap.empty(), (m, i) -> m.put(i, i % 2 == 0 ? shared : new Value(i)));
            assertValuesOf(hashMap);
            assertValuesOf(TreeMap.ofAll(hashMap.asJavaMap()));
            assertValuesOf(LinkedHashMap.ofAll(hashMap.asJavaMap()));
        }
    }

    @Test
    public void nonEmptyMapValuesAreTheValuesOfTheirMap() {
        for (int size : SIZES) {
            if (size == 0) {
                continue;
            }
            HashMap<Integer, Value> hashMap = hashMapOf(size, i -> i);
            TreeMap<Integer, Value> treeMap = treeMapOf(size, Comparator.reverseOrder(), shuffled(size));
            assertThat(hashMap.toNonEmptyMap().get().values().toVector()).isEqualTo(reference(hashMap));
            assertThat(treeMap.toNonEmptySortedMap().get().values().toVector()).isEqualTo(reference(treeMap));
            assertThat(treeMap.toNonEmptySortedMap().get().values().head())
                    .isSameAs(reference(treeMap).head());
        }
    }

    @Test
    public void ownedArrayIsRejectedWhenItHoldsANull() {
        Object[] small = {new Value(0), null};
        Object[] large = new Object[33];
        java.util.Arrays.fill(large, new Value(0));
        large[32] = null;
        assertThatThrownBy(() -> Vector.ofOwnedArray(small))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Vector: element is null");
        assertThatThrownBy(() -> Vector.ofOwnedArray(large))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Vector: element is null");
        assertThat(Vector.ofOwnedArray(new Object[0])).isSameAs(Vector.empty());
    }
}
