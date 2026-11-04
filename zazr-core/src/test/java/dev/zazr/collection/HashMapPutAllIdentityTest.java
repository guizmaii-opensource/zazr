package dev.zazr.collection;

import dev.zazr.Tuple2;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// `HashMap.putAll` of a HashMap built on its own, the trie of the argument never derived from the receiver's: the
/// receiver comes back when the argument holds only its very key and value objects, the argument when the receiver
/// adds nothing to it and it brings something, and in every case the content is that of successive puts, with the
/// argument's key and value objects on an equal key.
class HashMapPutAllIdentityTest {

    private static final int[] SIZES = {1, 2, 31, 32, 33, 1025};

    /// A key of a chosen hash code: two of one hash and different ids collide.
    record Key(int hash, int id) {
        @Override
        public int hashCode() {
            return hash;
        }
    }

    // the map of `keys` to `values`, put one by one in that order
    private static HashMap<Key, Object> mapOf(List<Key> keys, List<Object> values) {
        @SuppressWarnings("Var")
        HashMap<Key, Object> map = HashMap.empty();
        for (int i = 0; i < keys.size(); i++) {
            map = map.put(keys.get(i), values.get(i));
        }
        return map;
    }

    private static HashMap<Key, Object> mapOf(Object... keysAndValues) {
        @SuppressWarnings("Var")
        HashMap<Key, Object> map = HashMap.empty();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            map = map.put((Key) keysAndValues[i], keysAndValues[i + 1]);
        }
        return map;
    }

    // `map` with the entries of `other` put one by one, the definition of `putAll`
    private static HashMap<Key, Object> successivePuts(HashMap<Key, Object> map, HashMap<Key, Object> other) {
        @SuppressWarnings("Var")
        HashMap<Key, Object> result = map;
        for (Tuple2<Key, Object> entry : other) {
            result = result.put(entry._1(), entry._2());
        }
        return result;
    }

    // `map.putAll(other)`, checked against successive puts: equal, with the very key and value objects
    private static HashMap<Key, Object> putAll(HashMap<Key, Object> map, HashMap<Key, Object> other) {
        HashMap<Key, Object> result = map.putAll(other);
        HashMap<Key, Object> expected = successivePuts(map, other);
        assertThat(result).isEqualTo(expected);
        assertThat(result.size()).isEqualTo(expected.size());
        java.util.Map<Key, Tuple2<Key, Object>> byKey = new java.util.HashMap<>();
        for (Tuple2<Key, Object> entry : result) {
            byKey.put(entry._1(), entry);
        }
        for (Tuple2<Key, Object> entry : expected) {
            Tuple2<Key, Object> found = byKey.get(entry._1());
            assertThat(found._1()).isSameAs(entry._1());
            assertThat(found._2()).isSameAs(entry._2());
        }
        assertThat(result.hashCode()).isEqualTo(expected.hashCode());
        return result;
    }

    // the bits of a hash code a trie reads, its first level in the lowest five
    private static int rootBits(int hashCode) {
        int h1 = hashCode + ~(hashCode << 9);
        int h2 = h1 ^ (h1 >>> 14);
        int h3 = h2 + (h2 << 4);
        return h3 ^ (h3 >>> 10);
    }

    // a hash code other than `hash` whose key goes to the same slot of the root as one of `hash`, or to another slot
    private static int hashInRootSlotOf(int hash, boolean sameSlot) {
        for (int candidate = hash + 1; ; candidate++) {
            if (((rootBits(candidate) & 31) == (rootBits(hash) & 31)) == sameSlot) {
                return candidate;
            }
        }
    }

    private static List<Object> values(int size) {
        List<Object> values = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            values.add(new Object());
        }
        return values;
    }

    private static <T> List<T> reversed(List<T> list) {
        List<T> result = new ArrayList<>(list);
        Collections.reverse(result);
        return result;
    }

    // -- the reported inputs

    @Test
    void putAllOfTheReceiversObjectsInAnotherTrieWithASubNodeIsTheReceiver() {
        // the first two keys share a slot of the root, so each map has a sub-node there, a different object
        Key k1 = new Key(904090557, 0);
        Key k2 = new Key(-120478767, 2);
        Key k3 = new Key(1434408274, 2);
        Key k4 = new Key(985720182, 2);
        Object v1 = new Object();
        Object v2 = new Object();
        Object v3 = new Object();
        Object v4 = new Object();
        HashMap<Key, Object> map = mapOf(k1, v1, k2, v2, k3, v3, k4, v4);
        assertThat(putAll(map, mapOf(k2, v2, k1, v1, k3, v3))).isSameAs(map);
        assertThat(putAll(map, mapOf(k4, v4, k3, v3, k2, v2, k1, v1))).isSameAs(map);
    }

    @Test
    void putAllOfASupersetHoldingTheReceiversCollidingKeysInAnotherTrieIsTheArgument() {
        Key c1 = new Key(0, 0);
        Key c2 = new Key(0, 1);
        Key c3 = new Key(5, 0);
        Object w1 = new Object();
        Object w2 = new Object();
        Object w3 = new Object();
        HashMap<Key, Object> map = mapOf(c1, w1, c2, w2);
        HashMap<Key, Object> other = mapOf(c2, w2, c1, w1, c3, w3);
        assertThat(putAll(map, other)).isSameAs(other);
    }

    @Test
    void putAllOfTheReceiversCollidingKeysAndSubNodeInAnotherTrieIsTheReceiver() {
        Key c1 = new Key(0, 0);
        Key c2 = new Key(0, 1);
        Key k1 = new Key(904090557, 0);
        Key k2 = new Key(-120478767, 2);
        Object w1 = new Object();
        Object w2 = new Object();
        Object v1 = new Object();
        Object v2 = new Object();
        HashMap<Key, Object> map = mapOf(c1, w1, c2, w2, k1, v1, k2, v2);
        assertThat(putAll(map, mapOf(k2, v2, k1, v1, c2, w2, c1, w1))).isSameAs(map);
    }

    @Test
    void putAllOfACollidingKeyWithAnotherValueObjectTakesTheArgumentsValue() {
        // the argument holds `c1` inline where the receiver has the node of its colliding keys
        for (int size : SIZES) {
            List<Key> padding = new ArrayList<>();
            for (int i = 0; padding.size() < size; i++) {
                padding.add(new Key(i * 0x9E3779B9, 0));
            }
            List<Object> paddingValues = values(size);
            int hash = hashInRootSlotOf(padding.get(0).hash(), false);
            Key c1 = new Key(hash, 0);
            Key c2 = new Key(hash, 1);
            Object v1 = new Object();
            Object w = new Object();
            HashMap<Key, Object> map = mapOf(padding, paddingValues).put(c1, v1).put(c2, new Object());
            HashMap<Key, Object> other =
                    mapOf(reversed(padding), reversed(paddingValues)).put(c1, w);
            HashMap<Key, Object> result = putAll(map, other);
            assertThat(result.get(c1).get()).isSameAs(w);
            assertThat(result).isNotSameAs(map).isNotSameAs(other);
        }
    }

    @Test
    void putAllOfAMapHoldingSomeOfTheReceiversCollidingKeysAndAnotherKeyIsNeitherMap() {
        // the colliding keys of the argument come back as the receiver's node, which holds one more of them
        Key c1 = new Key(7, 0);
        Key c2 = new Key(7, 1);
        Key c3 = new Key(7, 2);
        Key y = new Key(hashInRootSlotOf(7, false), 0);
        Object w1 = new Object();
        Object w2 = new Object();
        HashMap<Key, Object> map = mapOf(c1, w1, c2, w2, c3, new Object());
        HashMap<Key, Object> other = mapOf(c2, w2, c1, w1, y, new Object());
        HashMap<Key, Object> result = putAll(map, other);
        assertThat(result.size()).isEqualTo(4);
        assertThat(result).isNotSameAs(map).isNotSameAs(other);
    }

    @Test
    void putAllOfASupersetWithASubNodeWhereTheReceiverHasAnEntryIsTheArgument() {
        // the argument's sub-node faces an entry of the receiver, and its colliding keys the receiver's node of them
        Key x = new Key(3, 0);
        Key y = new Key(hashInRootSlotOf(3, true), 0);
        int hash = hashInRootSlotOf(3, false);
        Key c1 = new Key(hash, 0);
        Key c2 = new Key(hash, 1);
        Object vx = new Object();
        Object w1 = new Object();
        Object w2 = new Object();
        HashMap<Key, Object> map = mapOf(x, vx, c1, w1, c2, w2);
        HashMap<Key, Object> other = mapOf(c2, w2, c1, w1, y, new Object(), x, vx);
        assertThat(putAll(map, other)).isSameAs(other);
    }

    @Test
    void putAllOfASeparatelyBuiltMapOfOneEntryWithTheSameObjectsIsTheReceiver() {
        Key key = new Key(1, 0);
        Object value = new Object();
        HashMap<Key, Object> map = HashMap.of(key, value);
        assertThat(putAll(map, HashMap.of(key, value))).isSameAs(map);
        HashMap<Key, Object> replaced = HashMap.of(key, new Object());
        assertThat(putAll(map, replaced)).isSameAs(replaced);
    }

    // -- the boundary sizes, with spread and with colliding hash codes

    @Test
    void putAllOfSeparatelyBuiltMapsAtTheBoundarySizes() {
        for (int size : SIZES) {
            for (boolean colliding : new boolean[] {false, true}) {
                List<Key> keys = new ArrayList<>();
                for (int i = 0; i < size; i++) {
                    keys.add(colliding ? new Key(i / 3, i % 3) : new Key(i * 0x9E3779B9, 0));
                }
                List<Object> values = values(size);
                HashMap<Key, Object> map = mapOf(keys, values);
                String shape = "size " + size + (colliding ? ", colliding" : ", spread");

                // the same objects, put in the other order
                assertThat(putAll(map, mapOf(reversed(keys), reversed(values))))
                        .as(shape + ": the same objects")
                        .isSameAs(map);

                // some of them
                if (size > 1) {
                    List<Key> someKeys = new ArrayList<>(keys);
                    List<Object> someValues = new ArrayList<>(values);
                    someKeys.remove(size / 2);
                    someValues.remove(size / 2);
                    assertThat(putAll(map, mapOf(reversed(someKeys), reversed(someValues))))
                            .as(shape + ": some of the objects")
                            .isSameAs(map);
                }

                // all of them and a new key, colliding with the first one
                List<Key> moreKeys = new ArrayList<>(reversed(keys));
                List<Object> moreValues = new ArrayList<>(reversed(values));
                moreKeys.add(new Key(keys.get(0).hash(), 99));
                moreValues.add(new Object());
                HashMap<Key, Object> more = mapOf(moreKeys, moreValues);
                assertThat(putAll(map, more)).as(shape + ": a new key").isSameAs(more);

                // all of them, with another value object for one key
                List<Object> otherValues = new ArrayList<>(values);
                Object replacement = new Object();
                otherValues.set(size / 2, replacement);
                HashMap<Key, Object> replaced = mapOf(reversed(keys), reversed(otherValues));
                HashMap<Key, Object> result = putAll(map, replaced);
                assertThat(result).as(shape + ": another value").isSameAs(replaced);
                assertThat(result.get(keys.get(size / 2)).get()).isSameAs(replacement);

                // all of them, with another key object, equal to one of them
                List<Key> otherKeys = new ArrayList<>(keys);
                Key twin = new Key(keys.get(size / 2).hash(), keys.get(size / 2).id());
                otherKeys.set(size / 2, twin);
                HashMap<Key, Object> twinned = mapOf(reversed(otherKeys), reversed(values));
                assertThat(putAll(map, twinned)).as(shape + ": another key").isSameAs(twinned);

                // a new key and the receiver's other objects: neither map
                if (size > 1) {
                    List<Key> mixedKeys = new ArrayList<>(moreKeys);
                    List<Object> mixedValues = new ArrayList<>(moreValues);
                    mixedKeys.remove(0);
                    mixedValues.remove(0);
                    HashMap<Key, Object> mixed = mapOf(mixedKeys, mixedValues);
                    assertThat(putAll(map, mixed))
                            .as(shape + ": a new key, one missing")
                            .isNotSameAs(map)
                            .isNotSameAs(mixed);
                }
            }
        }
    }
}
