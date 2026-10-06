package dev.zazr.collection;

import dev.zazr.Tuple2;
import java.util.ArrayList;
import java.util.Random;
import java.util.function.IntFunction;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// `HashMap.keySet` copies the trie of the entries: the result is the HashSet built from the keys, in iteration order,
/// and it behaves as that set under later additions and removals.
public class HashMapKeySetTest {

    private static final long SEED = 20261006L;
    private static final int[] SIZES = {0, 1, 2, 31, 32, 33, 1023, 1024, 1025};

    /// A key whose hash code is `hash`: keys of one hash and different ids collide.
    private record Colliding(int hash, int id) {
        @Override
        public int hashCode() {
            return hash;
        }
    }

    private static <T> java.util.List<T> list(Iterable<T> iterable) {
        java.util.List<T> result = new ArrayList<>();
        iterable.forEach(result::add);
        return result;
    }

    private static <K, V> java.util.List<K> keysOf(HashMap<K, V> map) {
        java.util.List<K> result = new ArrayList<>();
        for (Tuple2<K, V> entry : map) {
            result.add(entry._1());
        }
        return result;
    }

    private static <K> HashMap<K, Integer> mapOf(int size, IntFunction<K> key) {
        return Vector.range(0, size).foldLeft(HashMap.empty(), (acc, i) -> acc.put(key.apply(i), i));
    }

    private static <K> void assertIsTheRebuiltSet(HashMap<K, ?> map) {
        Set<K> keys = map.keySet();
        HashSet<K> rebuilt = HashSet.ofAll(keysOf(map));
        assertThat(keys).isInstanceOf(HashSet.class);
        assertThat(keys.size()).isEqualTo(map.size());
        assertThat(keys).isEqualTo(rebuilt);
        assertThat(rebuilt).isEqualTo(keys);
        assertThat(keys.hashCode()).isEqualTo(rebuilt.hashCode());
        assertThat(list(keys)).isEqualTo(list(rebuilt));
        assertThat(list(keys)).isEqualTo(keysOf(map));
        for (K key : keysOf(map)) {
            assertThat(keys.contains(key)).isTrue();
        }
    }

    @Test
    public void keySetOfTheEmptyMapIsTheEmptySet() {
        assertThat(HashMap.<Integer, String>empty().keySet()).isSameAs(HashSet.empty());
        assertThat(HashMap.of(1, "a").remove(1).keySet()).isSameAs(HashSet.empty());
    }

    @Test
    public void keySetIsTheRebuiltSetAtEveryBoundary() {
        for (int size : SIZES) {
            assertIsTheRebuiltSet(mapOf(size, i -> i));
            assertIsTheRebuiltSet(mapOf(size, i -> "k" + i));
            // every key of one hash: one collision node under seven levels of single children
            assertIsTheRebuiltSet(mapOf(size, i -> new Colliding(42, i)));
            // eight keys per hash: collision nodes beside inline keys and children
            assertIsTheRebuiltSet(mapOf(size, i -> new Colliding(i / 8, i)));
        }
    }

    @Test
    public void keySetOfABuiltMapIsTheRebuiltSet() {
        for (int size : SIZES) {
            HashMap.Builder<Colliding, Integer> builder = HashMap.newBuilder();
            for (int i = 0; i < size; i++) {
                builder.put(new Colliding(i / 3, i), i);
            }
            assertIsTheRebuiltSet(builder.result());
        }
    }

    @Test
    public void keySetHoldsTheKeyObjectsTheMapKept() {
        Colliding first = new Colliding(1, 0);
        Colliding second = new Colliding(1, 0);
        HashMap<Colliding, String> map = HashMap.<Colliding, String>empty()
                .put(first, "a")
                .put(new Colliding(1, 1), "b")
                .put(second, "c");
        Set<Colliding> keys = map.keySet();
        assertThat(keys.size()).isEqualTo(2);
        assertThat(list(keys).stream().filter(key -> key == second)).hasSize(1);
        assertThat(list(keys).stream().filter(key -> key == first)).isEmpty();
        assertIsTheRebuiltSet(map);
    }

    @Test
    public void keySetBehavesAsTheRebuiltSetUnderAdditionsAndRemovals() {
        Random random = new Random(SEED);
        for (int size : SIZES) {
            HashMap<Colliding, Integer> map = mapOf(size, i -> new Colliding(random.nextInt(size + 1), i % 4));
            java.util.List<Tuple2<Colliding, Integer>> entriesBefore = list(map);
            Set<Colliding> keys = map.keySet();
            java.util.List<Colliding> keysBefore = list(keys);
            Set<Colliding> rebuilt = HashSet.ofAll(keysOf(map));
            java.util.Set<Colliding> model = new java.util.HashSet<>(keysOf(map));
            @SuppressWarnings("Var")
            Set<Colliding> changed = keys;
            @SuppressWarnings("Var")
            Set<Colliding> expected = rebuilt;
            for (int step = 0; step < 3 * size + 20; step++) {
                Colliding element = new Colliding(random.nextInt(size + 2), random.nextInt(5));
                if (random.nextBoolean()) {
                    changed = changed.add(element);
                    expected = expected.add(element);
                    model.add(element);
                } else {
                    changed = changed.remove(element);
                    expected = expected.remove(element);
                    model.remove(element);
                }
                assertThat(changed.contains(element)).isEqualTo(model.contains(element));
            }
            assertThat(list(changed)).isEqualTo(list(expected));
            assertThat(changed).isEqualTo(expected);
            assertThat(new java.util.HashSet<>(list(changed))).isEqualTo(model);
            // the map and the first key set are unchanged
            assertThat(list(map)).isEqualTo(entriesBefore);
            assertThat(list(keys)).isEqualTo(keysBefore);
            assertIsTheRebuiltSet(map);
        }
    }

    @Test
    public void keySetBehavesAsTheRebuiltSetUnderBulkOperations() {
        HashMap<Colliding, Integer> map = mapOf(1025, i -> new Colliding(i / 2, i % 2));
        Set<Colliding> keys = map.keySet();
        Set<Colliding> rebuilt = HashSet.ofAll(keysOf(map));
        HashSet<Colliding> other = HashSet.ofAll(Vector.range(500, 1500).map(i -> new Colliding(i / 2, i % 3)));
        assertThat(list(keys.union(other))).isEqualTo(list(rebuilt.union(other)));
        assertThat(list(keys.diff(other))).isEqualTo(list(rebuilt.diff(other)));
        assertThat(list(keys.intersect(other))).isEqualTo(list(rebuilt.intersect(other)));
        assertThat(list(keys.filter(key -> key.hash() % 3 == 0)))
                .isEqualTo(list(rebuilt.filter(key -> key.hash() % 3 == 0)));
        assertThat(list(builtFrom(keys, other))).isEqualTo(list(builtFrom(rebuilt, other)));
        assertIsTheRebuiltSet(map);
    }

    // a builder that adopts `first`, then adds `second` through its nodes
    private static <T> HashSet<T> builtFrom(Set<T> first, Iterable<T> second) {
        HashSet.Builder<T> builder = HashSet.newBuilder();
        builder.addAll(first);
        second.forEach(builder::add);
        return builder.result();
    }

    @Test
    public void keySetOfANonEmptyMapIsTheRebuiltSet() {
        NonEmptyMap<Colliding, Integer> map = NonEmptyMap.single(new Colliding(3, 0), 0)
                .put(new Colliding(3, 1), 1)
                .put(new Colliding(4, 0), 2);
        NonEmptySet<Colliding> keys = map.keySet();
        assertThat(list(keys)).isEqualTo(list(HashSet.ofAll(keysOf(map.toMap()))));
        assertThat(keys.toSet()).isEqualTo(map.toMap().keySet());
    }
}
