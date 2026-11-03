package dev.zazr.collection;

import dev.zazr.Tuple;
import dev.zazr.Tuple2;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/// `partition` of `HashSet` and `HashMap`, split node by node, and `HashMap.putAll` of another HashMap, compared with
/// their definitions on random tries, hash collisions included: each side is the set or map built from its elements
/// (the same shape, so the same iteration order), the predicate is called once per element in iteration order, a side
/// that gets everything is the receiver, and `putAll` returns the receiver or the argument when the result is either.
class HashSharingTest {

    private static final long SEED = 20261103L;
    private static final int[] SIZES = {0, 1, 2, 3, 31, 32, 33, 100, 1023, 1024, 1025, 3000};

    /// An element of a chosen hash code: two of one hash and different ids collide.
    record Key(int hash, int id) {
        @Override
        public int hashCode() {
            return hash;
        }
    }

    // a key in `range` hashes, few of them colliding when the range is wide and many when it is narrow
    private static Key randomKey(Random random, int range) {
        int hash = (random.nextInt(8) == 0) ? random.nextInt() : random.nextInt(range);
        return new Key(hash, random.nextInt(2));
    }

    private static List<Key> randomKeys(Random random, int size, int round) {
        int range = (round % 2 == 0) ? 4 * size + 1 : size / 2 + 1;
        List<Key> keys = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            keys.add(randomKey(random, range));
        }
        return keys;
    }

    // the predicates: none, all, a random half (every node splits), and by the slot of the root (whole subtrees go
    // to one side)
    private static List<Predicate<Key>> predicates(Random random) {
        long salt = random.nextLong();
        return List.of(
                key -> false,
                key -> true,
                key -> Long.hashCode(salt * 31 + key.hash() * 7L + key.id()) % 2 == 0,
                key -> (rootBits(key.hash()) & 31) < 16);
    }

    // the bits of a hash code a trie reads, its first level in the lowest five
    private static int rootBits(int hashCode) {
        int h1 = hashCode + ~(hashCode << 9);
        int h2 = h1 ^ (h1 >>> 14);
        int h3 = h2 + (h2 << 4);
        return h3 ^ (h3 >>> 10);
    }

    private static <T> List<T> order(Iterable<T> collection) {
        List<T> result = new ArrayList<>();
        collection.forEach(result::add);
        return result;
    }

    // -- HashSet.partition

    @Test
    void hashSetPartitionIsThatOfTheDefinition() {
        Random random = new Random(SEED);
        for (int size : SIZES) {
            for (int round = 0; round < 4; round++) {
                HashSet<Key> set = HashSet.ofAll(randomKeys(random, size, round));
                List<Key> before = order(set);
                for (Predicate<Key> predicate : predicates(random)) {
                    List<Key> calls = new ArrayList<>();
                    Tuple2<HashSet<Key>, HashSet<Key>> sides = set.partition(key -> {
                        calls.add(key);
                        return predicate.test(key);
                    });
                    assertThat(calls).containsExactlyElementsOf(before);
                    List<Key> kept = new ArrayList<>();
                    List<Key> rejected = new ArrayList<>();
                    for (Key key : before) {
                        (predicate.test(key) ? kept : rejected).add(key);
                    }
                    assertSameSet(HashSet.ofAll(kept), sides._1());
                    assertSameSet(HashSet.ofAll(rejected), sides._2());
                    assertSide(set, sides._1(), kept.size());
                    assertSide(set, sides._2(), rejected.size());
                    assertThat(order(set)).containsExactlyElementsOf(before);
                }
            }
        }
    }

    private static void assertSameSet(HashSet<Key> expected, HashSet<Key> actual) {
        assertThat(actual).isEqualTo(expected);
        assertThat(order(actual)).containsExactlyElementsOf(order(expected));
        assertThat(actual.hashCode()).isEqualTo(expected.hashCode());
    }

    // the side that gets every element is the set itself, and an empty side the empty set
    private static void assertSide(HashSet<Key> set, HashSet<Key> side, int size) {
        if (size == 0) {
            assertThat(side).isSameAs(HashSet.empty());
        } else if (size == set.size()) {
            assertThat(side).isSameAs(set);
        }
    }

    @Test
    void hashSetPartitionKeepsTheObjectsOfTheSet() {
        Key a = new Key(1, 0);
        HashSet<Key> set = HashSet.of(a, new Key(2, 0), new Key(1, 1));
        Tuple2<HashSet<Key>, HashSet<Key>> sides = set.partition(key -> key.id() == 0);
        assertThat(sides._1().find(key -> key.equals(new Key(1, 0))).get()).isSameAs(a);
    }

    @Test
    void hashSetPartitionRejectsANullPredicate() {
        assertThatNullPointerException()
                .isThrownBy(() -> HashSet.of(1).partition(null))
                .withMessage("predicate is null");
    }

    // -- HashMap.partition

    @Test
    void hashMapPartitionIsThatOfTheDefinition() {
        Random random = new Random(SEED + 1);
        for (int size : SIZES) {
            for (int round = 0; round < 4; round++) {
                HashMap<Key, Object> map = mapOf(randomKeys(random, size, round));
                List<Tuple2<Key, Object>> before = order(map);
                for (Predicate<Key> predicate : predicates(random)) {
                    List<Tuple2<Key, Object>> calls = new ArrayList<>();
                    Tuple2<HashMap<Key, Object>, HashMap<Key, Object>> sides = map.partition(entry -> {
                        calls.add(entry);
                        return predicate.test(entry._1());
                    });
                    assertThat(calls).containsExactlyElementsOf(before);
                    List<Tuple2<Key, Object>> kept = new ArrayList<>();
                    List<Tuple2<Key, Object>> rejected = new ArrayList<>();
                    for (Tuple2<Key, Object> entry : before) {
                        (predicate.test(entry._1()) ? kept : rejected).add(entry);
                    }
                    assertSameMap(HashMap.ofEntries(kept), sides._1());
                    assertSameMap(HashMap.ofEntries(rejected), sides._2());
                    if (kept.isEmpty()) {
                        assertThat(sides._1()).isSameAs(HashMap.empty());
                    } else if (rejected.isEmpty()) {
                        assertThat(sides._1()).isSameAs(map);
                    }
                    if (rejected.isEmpty()) {
                        assertThat(sides._2()).isSameAs(HashMap.empty());
                    } else if (kept.isEmpty()) {
                        assertThat(sides._2()).isSameAs(map);
                    }
                    assertThat(order(map)).containsExactlyElementsOf(before);
                }
            }
        }
    }

    // a new value object per key
    private static HashMap<Key, Object> mapOf(List<Key> keys) {
        @SuppressWarnings("Var")
        HashMap<Key, Object> map = HashMap.empty();
        for (Key key : keys) {
            map = map.put(key, new Object());
        }
        return map;
    }

    // equal, in the same order, with the very key and value objects
    private static void assertSameMap(HashMap<Key, Object> expected, HashMap<Key, Object> actual) {
        assertThat(actual).isEqualTo(expected);
        List<Tuple2<Key, Object>> actualOrder = order(actual);
        List<Tuple2<Key, Object>> expectedOrder = order(expected);
        assertThat(actualOrder).containsExactlyElementsOf(expectedOrder);
        for (int i = 0; i < actualOrder.size(); i++) {
            assertThat(actualOrder.get(i)._1()).isSameAs(expectedOrder.get(i)._1());
            assertThat(actualOrder.get(i)._2()).isSameAs(expectedOrder.get(i)._2());
        }
        assertThat(actual.hashCode()).isEqualTo(expected.hashCode());
    }

    @Test
    void hashMapPartitionRejectsANullPredicate() {
        assertThatNullPointerException()
                .isThrownBy(() -> HashMap.of(1, 2).partition(null))
                .withMessage("predicate is null");
    }

    // -- HashMap.putAll of a HashMap

    @Test
    void putAllOfAHashMapIsThatOfTheDefinition() {
        Random random = new Random(SEED + 2);
        for (int size : SIZES) {
            for (int round = 0; round < 4; round++) {
                HashMap<Key, Object> map = mapOf(randomKeys(random, size, round));
                for (HashMap<Key, Object> other : others(map, size, round, random)) {
                    HashMap<Key, Object> result = map.putAll(other);
                    @SuppressWarnings("Var")
                    HashMap<Key, Object> expected = map;
                    for (Tuple2<Key, Object> entry : order(other)) {
                        expected = expected.put(entry._1(), entry._2());
                    }
                    assertSameMap(expected, result);
                    if (expected == map) {
                        assertThat(result).isSameAs(map);
                    }
                }
            }
        }
    }

    // the maps given to putAll: independent, derived from `map` by removals, by a filter, by puts of new values and
    // keys, and `map` itself
    private static List<HashMap<Key, Object>> others(HashMap<Key, Object> map, int size, int round, Random random) {
        List<Key> keys = order(map.keySet());
        List<HashMap<Key, Object>> others = new ArrayList<>();
        others.add(mapOf(randomKeys(random, size, round)));
        others.add(map);
        others.add(map.filter((k, v) -> (k.hash() & 3) != 0));
        if (!keys.isEmpty()) {
            Key key = keys.get(random.nextInt(keys.size()));
            others.add(map.remove(key));
            others.add(map.removeAll(keys.subList(0, Math.min(keys.size(), 1 + size / 10))));
            others.add(map.put(key, new Object()));
            others.add(map.put(new Key(key.hash(), key.id()), map.get(key).get()));
        }
        others.add(map.put(randomKey(random, 4 * size + 1), new Object()));
        return others;
    }

    @Test
    void putAllOfASubsetOfTheSameObjectsIsTheReceiver() {
        Random random = new Random(SEED + 3);
        for (int size : SIZES) {
            for (int round = 0; round < 4; round++) {
                HashMap<Key, Object> map = mapOf(randomKeys(random, size, round));
                List<Key> keys = order(map.keySet());
                assertThat(map.putAll(map)).isSameAs(map);
                assertThat(map.putAll(map.filter((k, v) -> (k.hash() & 3) != 0))).isSameAs(map);
                assertThat(map.putAll(map.filter((k, v) -> k.id() == 0))).isSameAs(map);
                if (!keys.isEmpty()) {
                    Key key = keys.get(random.nextInt(keys.size()));
                    assertThat(map.putAll(map.remove(key))).isSameAs(map);
                    assertThat(map.putAll(map.removeAll(keys.subList(0, Math.min(keys.size(), 1 + size / 10)))))
                            .isSameAs(map);
                    // another key object: the entry of the argument wins, key and value
                    Key twin = new Key(key.hash(), key.id());
                    HashMap<Key, Object> other = map.remove(key).put(twin, map.get(key).get());
                    HashMap<Key, Object> result = map.putAll(other);
                    assertThat(result).isNotSameAs(map).isEqualTo(map);
                    assertThat(order(result.keySet()).stream().filter(k -> k == twin)).hasSize(1);
                }
            }
        }
    }

    @Test
    void putAllOfASupersetOfTheSameObjectsIsTheArgument() {
        Random random = new Random(SEED + 4);
        for (int size : SIZES) {
            for (int round = 0; round < 4; round++) {
                HashMap<Key, Object> map = mapOf(randomKeys(random, size, round));
                HashMap<Key, Object> added = map.put(randomKey(random, 4 * size + 1), new Object());
                if (added.size() > map.size()) {
                    assertThat(map.putAll(added)).isSameAs(added);
                }
                List<Key> keys = order(map.keySet());
                if (!keys.isEmpty()) {
                    HashMap<Key, Object> replaced = map.put(keys.get(random.nextInt(keys.size())), new Object());
                    assertThat(map.putAll(replaced)).isSameAs(replaced);
                }
            }
        }
    }

    @Test
    void putAllOfAnOverlappingMapLeavesBothMapsAsTheyWere() {
        HashMap<Integer, String> map = HashMap.of(1, "a", 2, "b", 3, "c");
        HashMap<Integer, String> other = map.remove(2).put(4, "d");
        HashMap<Integer, String> result = map.putAll(other);
        assertThat(result).isEqualTo(HashMap.of(1, "a", 2, "b", 3, "c", 4, "d"));
        assertThat(map).isEqualTo(HashMap.of(1, "a", 2, "b", 3, "c"));
        assertThat(other).isEqualTo(HashMap.of(1, "a", 3, "c", 4, "d"));
        assertThat(Tuple.of(result.size(), map.size(), other.size())).isEqualTo(Tuple.of(4, 3, 3));
    }
}
