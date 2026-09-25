package com.guizmaii.zazr.collection.internal;

import com.guizmaii.zazr.Tuple;
import com.guizmaii.zazr.Tuple2;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static com.guizmaii.zazr.collection.internal.ChampValidity.assertSameShape;
import static com.guizmaii.zazr.collection.internal.ChampValidity.assertValid;
import static com.guizmaii.zazr.collection.internal.ChampValidity.describe;
import static com.guizmaii.zazr.collection.internal.ChampValidity.entries;
import static com.guizmaii.zazr.collection.internal.ChampValidity.internalNodes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// The CHAMP trie of `HashMap`: lookups, persistent updates in canonical form, iteration, and the transient builder.
public class ChampMapTest {

    private static final long SEED = 20260925L;
    private static final int[] SIZES = { 0, 1, 2, 31, 32, 33, 1023, 1024, 1025, 32768, 100_000 };

    /** A key whose hash code is chosen by the test; two keys are equal when both the hash and the id are. */
    record Key(int hash, int id) {
        @Override
        public int hashCode() {
            return hash;
        }
    }

    // hash codes that hit the edges of the trie: the first fragments, the 32 and 1024 boundaries, the sign bit, and
    // values that share their low fragments and differ only at a deep shift
    private static final int[] HOT_HASHES = { 0, 1, 2, 31, 32, 33, 1023, 1024, 1025, -1, Integer.MIN_VALUE, Integer.MAX_VALUE,
            1 << 30, 2 << 30, 3 << 30, 1 << 25, 1 << 20, (1 << 25) | 1, (1 << 30) | 1 };

    private static Key randomKey(Random random) {
        final int hash = switch (random.nextInt(4)) {
            case 0 -> HOT_HASHES[random.nextInt(HOT_HASHES.length)];
            case 1 -> random.nextInt(64);
            case 2 -> random.nextInt(64) << (5 * (1 + random.nextInt(5)));
            default -> random.nextInt();
        };
        // a few ids per hash make collision nodes
        return new Key(hash, random.nextInt(3));
    }

    // -- the tries compared

    private static <K, V> BitmapIndexedMapNode<K, V> persistent(java.util.List<K> keys, java.util.List<V> values) {
        BitmapIndexedMapNode<K, V> trie = MapNode.empty();
        for (int i = 0; i < keys.size(); i++) {
            trie = trie.updated(keys.get(i), values.get(i));
        }
        return trie;
    }

    private static <K, V> BitmapIndexedMapNode<K, V> built(java.util.List<K> keys, java.util.List<V> values) {
        final HashMapBuilder<K, V> builder = new HashMapBuilder<>("test");
        for (int i = 0; i < keys.size(); i++) {
            builder.checkOpen();
            builder.put(keys.get(i), values.get(i));
        }
        assertThat(builder.size()).isEqualTo(persistent(keys, values).size());
        return builder.result();
    }

    private static java.util.List<Integer> ids(int size) {
        final java.util.List<Integer> values = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            values.add(i);
        }
        return values;
    }

    /** The kept key object and its value, the model of a map entry. */
    private record Kept(Object key, Object value) {
    }

    // asserts that the trie holds exactly the entries of the model, by the identity of the kept key and value objects
    private static void assertHolds(BitmapIndexedMapNode<Key, ?> trie, java.util.Map<Key, Kept> model) {
        assertThat(trie.size()).isEqualTo(model.size());
        final java.util.Map<Key, Kept> actual = new java.util.HashMap<>();
        for (Object[] entry : entries(trie)) {
            assertThat(actual.put((Key) entry[0], new Kept(entry[0], entry[1]))).as("each key once").isNull();
        }
        assertThat(actual.keySet()).isEqualTo(model.keySet());
        model.forEach((key, kept) -> {
            assertThat(actual.get(key).key()).isSameAs(kept.key());
            assertThat(actual.get(key).value()).isSameAs(kept.value());
            final Key probe = new Key(key.hash(), key.id());
            assertThat(trie.containsKey(probe)).isTrue();
            assertThat(trie.getOrElse(probe, null)).isSameAs(kept.value());
            final Tuple2<Key, ?> found = trie.getEntry(probe);
            assertThat(found).isNotNull();
            assertThat(found._1()).isSameAs(kept.key());
            assertThat(found._2()).isSameAs(kept.value());
        });
    }

    // the canonical form: the trie equals a fresh one of its entries, put in another order
    private static void assertCanonical(BitmapIndexedMapNode<Key, ?> trie) {
        assertValid(trie);
        final java.util.List<Object[]> all = entries(trie);
        Collections.reverse(all);
        BitmapIndexedMapNode<Key, Object> fresh = MapNode.empty();
        for (Object[] entry : all) {
            fresh = fresh.updated((Key) entry[0], entry[1]);
        }
        assertSameShape(fresh, trie, false);
    }

    // -- lookups

    @Test
    public void shouldFindPresentKeysAndMissAbsentOnes() {
        BitmapIndexedMapNode<Integer, Integer> trie = MapNode.empty();
        assertThat(trie.containsKey(2)).isFalse();
        assertThat(trie.getOrElse(2, 42)).isEqualTo(42);
        assertThat(trie.getEntry(2)).isNull();
        trie = trie.updated(1, 2).updated(4, 5);
        assertThat(trie.containsKey(1)).isTrue();
        assertThat(trie.getOrElse(1, 42)).isEqualTo(2);
        assertThat(trie.containsKey(4)).isTrue();
        assertThat(trie.getOrElse(4, 42)).isEqualTo(5);
        assertThat(trie.containsKey(2)).isFalse();
        assertThat(trie.getOrElse(2, 42)).isEqualTo(42);
        assertThat(trie.getEntry(2)).isNull();
        // a key of the same fragment as a present one, and of the same hash as a present one
        trie = trie.updated(33, 33);
        assertThat(trie.containsKey(65)).isFalse();
        assertThat(trie.getOrElse(65, 42)).isEqualTo(42);
        assertThat(trie.getEntry(65)).isNull();
        final BitmapIndexedMapNode<Key, Integer> colliding = MapNode.<Key, Integer> empty().updated(new Key(7, 0), 0).updated(new Key(7, 1), 1);
        assertThat(colliding.containsKey(new Key(7, 2))).isFalse();
        assertThat(colliding.getOrElse(new Key(7, 2), 42)).isEqualTo(42);
        assertThat(colliding.getEntry(new Key(7, 2))).isNull();
        assertThat(colliding.getOrElse(new Key(7 + (1 << 31), 0), 42)).isEqualTo(42);
        assertThat(colliding.getEntry(new Key(7 + (1 << 31), 0))).isNull();
        assertThat(colliding.containsKey(new Key(7 + (1 << 31), 0))).isFalse();
    }

    @Test
    public void shouldLookUpNullAsAnAbsentKeyOfHashZero() {
        final BitmapIndexedMapNode<Integer, Integer> trie = MapNode.<Integer, Integer> empty().updated(0, 0).updated(32, 32);
        assertThat(trie.containsKey(null)).isFalse();
        assertThat(trie.getOrElse(null, 42)).isEqualTo(42);
        assertThat(trie.getEntry(null)).isNull();
        assertThat(trie.removed(null)).isSameAs(trie);
    }

    @Test
    public void shouldKeepKeysWhoseHashesDifferOnlyInOneBit() {
        // every power of two: 32 keys that share the fragments of all but one level
        final java.util.List<Integer> keys = new ArrayList<>();
        for (int i = 0; i < Integer.SIZE; i++) {
            keys.add(1 << i);
        }
        final BitmapIndexedMapNode<Integer, Integer> trie = persistent(keys, keys);
        assertValid(trie);
        assertThat(trie.size()).isEqualTo(32);
        for (Integer key : keys) {
            assertThat(trie.getOrElse(key, null)).isEqualTo(key);
        }
    }

    // -- the shape at the boundaries of a node

    @Test
    public void shouldHoldUpTo32EntriesInlineInTheRootAndPushTheThirtyThirdDown() {
        for (int size : new int[] { 0, 1, 2, 31, 32, 33 }) {
            final BitmapIndexedMapNode<Integer, Integer> trie = persistent(ids(size), ids(size));
            assertValid(trie);
            if (size <= 32) {
                assertThat(trie.nodeMap).isZero();
                assertThat(Integer.bitCount(trie.dataMap)).isEqualTo(size);
                assertThat(trie.content).hasSize(2 * size);
            } else {
                // 0 and 32 share the fragment 0 of the root: a child of two entries
                assertThat(trie.nodeMap).isEqualTo(1);
                assertThat(Integer.bitCount(trie.dataMap)).isEqualTo(31);
                assertThat(trie.getNode(0).size()).isEqualTo(2);
            }
        }
    }

    @Test
    public void shouldFillTwoLevelsWith1024EntriesAndGoThreeDeepWithTheNext() {
        for (int size : new int[] { 1023, 1024, 1025 }) {
            final BitmapIndexedMapNode<Integer, Integer> trie = persistent(ids(size), ids(size));
            assertValid(trie);
            assertThat(trie.dataMap).isZero();
            assertThat(trie.nodeMap).isEqualTo(-1);
            final BitmapIndexedMapNode<Integer, Integer> first = (BitmapIndexedMapNode<Integer, Integer>) trie.getNode(0);
            if (size < 1025) {
                assertThat(first.nodeMap).isZero();
                assertThat(Integer.bitCount(first.dataMap)).isEqualTo(32);
            } else {
                // 0 and 1024 share the fragments 0 and 0: the first child holds them in a child of its own
                assertThat(first.nodeMap).isEqualTo(1);
                assertThat(first.getNode(0).size()).isEqualTo(2);
            }
            // 1023 is the last entry of the last child
            final BitmapIndexedMapNode<Integer, Integer> last = (BitmapIndexedMapNode<Integer, Integer>) trie.getNode(31);
            assertThat(last.size()).isEqualTo(size == 1023 ? 31 : 32);
        }
    }

    @Test
    public void shouldPutKeysOfOneHashInACollisionNodeBelowTheLastLevel() {
        final BitmapIndexedMapNode<Key, Integer> trie = persistent(java.util.List.of(new Key(5, 0), new Key(5, 1), new Key(5, 2)), ids(3));
        assertValid(trie);
        // six single-child levels below the root, then the collision node
        MapNode<Key, Integer> node = trie;
        for (int level = 0; level < 7; level++) {
            assertThat(node).isInstanceOf(BitmapIndexedMapNode.class);
            assertThat(((BitmapIndexedMapNode<Key, Integer>) node).dataMap).isZero();
            node = node.getNode(0);
        }
        assertThat(node).isInstanceOf(HashCollisionMapNode.class);
        assertThat(node.size()).isEqualTo(3);
        // down to one entry, the collision node goes back up to the root
        final BitmapIndexedMapNode<Key, Integer> one = trie.removed(new Key(5, 0)).removed(new Key(5, 2));
        assertValid(one);
        assertThat(one.nodeMap).isZero();
        assertThat(one.dataMap).isEqualTo(1 << 5);
        assertThat(one.getKey(0)).isEqualTo(new Key(5, 1));
    }

    @Test
    public void shouldPullASingleRemainingEntryBackUpToTheRoot() {
        // hashes that share all their fragments up to the last level
        final java.util.List<Key> keys = java.util.List.of(new Key(0, 0), new Key(1 << 30, 0), new Key(2 << 30, 0), new Key(3 << 30, 0));
        BitmapIndexedMapNode<Key, Integer> trie = persistent(keys, ids(4));
        assertValid(trie);
        assertThat(trie.nodeMap).isEqualTo(1);
        for (int i = 0; i < 3; i++) {
            trie = trie.removed(keys.get(i));
            assertCanonical(trie);
        }
        assertThat(trie.nodeMap).isZero();
        assertThat(trie.dataMap).isEqualTo(1);
        assertThat(trie.getKey(0)).isSameAs(keys.get(3));
        // and a root that keeps other entries inlines the survivor
        final BitmapIndexedMapNode<Key, Integer> mixed = persistent(java.util.List.of(new Key(0, 0), new Key(1 << 30, 0), new Key(7, 0)), ids(3));
        final BitmapIndexedMapNode<Key, Integer> after = mixed.removed(new Key(0, 0));
        assertCanonical(after);
        assertThat(after.nodeMap).isZero();
        assertThat(Integer.bitCount(after.dataMap)).isEqualTo(2);
    }

    // -- persistent updates

    @Test
    public void shouldReturnTheSameTrieWhenNothingChanges() {
        final Integer key = 1000;
        final String value = "v";
        final BitmapIndexedMapNode<Integer, String> trie = MapNode.<Integer, String> empty().updated(key, value).updated(2000, "w");
        assertThat(trie.updated(key, value)).isSameAs(trie);
        assertThat(trie.removed(3000)).isSameAs(trie);
        assertThat(trie.removed(1000 + (1 << 20))).isSameAs(trie);
        assertThat(trie.updated(key, "x", key, 0, false)).isSameAs(trie);
        final BitmapIndexedMapNode<Key, String> colliding = MapNode.<Key, String> empty().updated(new Key(1, 0), value).updated(new Key(1, 1), value);
        final Key present = colliding.getEntry(new Key(1, 1))._1();
        assertThat(colliding.updated(present, value)).isSameAs(colliding);
        assertThat(colliding.updated(new Key(1, 1), value, 1, 0, false)).isSameAs(colliding);
        assertThat(colliding.removed(new Key(1, 2))).isSameAs(colliding);
    }

    @Test
    public void shouldReplaceTheKeyAndTheValueOfAnEqualKey() {
        // inline, and inside a collision node
        for (int hash : new int[] { 3, 7 }) {
            final Key first = new Key(hash, 0);
            final Key second = new Key(hash, 0);
            final String v1 = new String("v");
            final String v2 = new String("v");
            BitmapIndexedMapNode<Key, String> trie = MapNode.empty();
            if (hash == 7) {
                trie = trie.updated(new Key(7, 1), "other");
            }
            trie = trie.updated(first, v1);
            final BitmapIndexedMapNode<Key, String> sameValue = trie.updated(second, v1);
            assertThat(sameValue.getEntry(first)._1()).isSameAs(second);
            final BitmapIndexedMapNode<Key, String> sameKey = trie.updated(first, v2);
            assertThat(sameKey.getEntry(first)._2()).isSameAs(v2);
            assertThat(sameKey.getEntry(first)._1()).isSameAs(first);
            // the older trie is unchanged
            assertThat(trie.getEntry(first)._1()).isSameAs(first);
            assertThat(trie.getEntry(first)._2()).isSameAs(v1);
            assertValid(sameValue);
            assertValid(sameKey);
        }
    }

    @Test
    public void shouldMatchAModelAndStayCanonicalOnRandomPutsAndRemovals() {
        for (long seed = 0; seed < 20; seed++) {
            final Random random = new Random(SEED + seed);
            final java.util.Map<Key, Kept> model = new java.util.HashMap<>();
            final java.util.List<BitmapIndexedMapNode<Key, Object>> versions = new ArrayList<>();
            final java.util.List<java.util.Map<Key, Kept>> versionModels = new ArrayList<>();
            BitmapIndexedMapNode<Key, Object> trie = MapNode.empty();
            final int steps = random.nextInt(4) == 0 ? random.nextInt(60) : random.nextInt(2500);
            for (int step = 0; step < steps; step++) {
                final Key key = randomKey(random);
                if (random.nextInt(3) == 0) {
                    final BitmapIndexedMapNode<Key, Object> after = trie.removed(key);
                    if (model.remove(key) == null) {
                        assertThat(after).isSameAs(trie);
                    }
                    trie = after;
                    // the canonical form after every removal: the local invariants, which make the shape unique, and
                    // now and then the comparison with a fresh trie of the same entries
                    assertValid(trie);
                    if (random.nextInt(10) == 0) {
                        assertCanonical(trie);
                    }
                } else {
                    final Object value = random.nextInt(4) == 0 && model.containsKey(key) ? model.get(key).value() : new Object();
                    trie = trie.updated(key, value);
                    model.put(key, new Kept(key, value));
                }
                if (random.nextInt(50) == 0) {
                    versions.add(trie);
                    versionModels.add(new java.util.HashMap<>(model));
                }
            }
            assertCanonical(trie);
            assertHolds(trie, model);
            // older versions are unchanged
            for (int i = 0; i < versions.size(); i++) {
                assertValid(versions.get(i));
                assertHolds(versions.get(i), versionModels.get(i));
            }
            // and removing everything, in random order, keeps it canonical down to the empty trie
            final java.util.List<Key> keys = new ArrayList<>(model.keySet());
            Collections.shuffle(keys, random);
            for (Key key : keys) {
                trie = trie.removed(new Key(key.hash(), key.id()));
                model.remove(key);
                assertValid(trie);
            }
            assertThat(trie.size()).isZero();
            assertThat(trie.dataMap | trie.nodeMap).isZero();
        }
    }

    @Test
    public void shouldMatchTheJdkMapOnLargeRandomData() {
        final Random random = new Random(SEED);
        for (boolean weak : new boolean[] { false, true }) {
            final java.util.Map<Object, Integer> jdk = new java.util.HashMap<>();
            BitmapIndexedMapNode<Object, Integer> trie = MapNode.empty();
            for (int i = 0; i < 5000; i++) {
                final int k = random.nextInt();
                // a weak hash code: ten hashes for all the keys
                final Object key = weak ? new Key(Math.abs(k % 10), k) : Integer.valueOf(k);
                final int value = random.nextInt();
                jdk.put(key, value);
                trie = trie.updated(key, value);
            }
            assertValid(trie);
            assertThat(trie.size()).isEqualTo(jdk.size());
            for (java.util.Map.Entry<Object, Integer> e : jdk.entrySet()) {
                assertThat(trie.getOrElse(e.getKey(), null)).isEqualTo(e.getValue());
            }
            for (Object key : new ArrayList<>(jdk.keySet())) {
                jdk.remove(key);
                trie = trie.removed(key);
            }
            assertThat(trie.size()).isZero();
        }
    }

    // -- iteration

    @Test
    public void shouldIterateTheEntriesOfANodeBeforeItsChildren() {
        // 0 and 32 go to a child of slot 0; 1 and 2 stay inline
        final BitmapIndexedMapNode<Integer, String> trie = persistent(java.util.List.of(0, 32, 1, 2), java.util.List.of("a", "b", "c", "d"));
        final java.util.List<Integer> keys = new ArrayList<>();
        trie.keysIterator().forEachRemaining(keys::add);
        assertThat(keys).containsExactly(1, 2, 0, 32);
        final java.util.List<String> values = new ArrayList<>();
        trie.valuesIterator().forEachRemaining(values::add);
        assertThat(values).containsExactly("c", "d", "a", "b");
        final java.util.List<String> pairs = new ArrayList<>();
        trie.iterator((k, v) -> k + v).forEachRemaining(pairs::add);
        assertThat(pairs).containsExactly("1c", "2d", "0a", "32b");
    }

    @Test
    public void shouldIterateEveryEntryOnceAtEveryBoundary() {
        for (int size : SIZES) {
            final BitmapIndexedMapNode<Integer, Integer> trie = persistent(ids(size), ids(size));
            final java.util.Iterator<Integer> keys = trie.keysIterator();
            final java.util.Set<Integer> seen = new java.util.HashSet<>();
            while (keys.hasNext()) {
                assertThat(seen.add(keys.next())).isTrue();
            }
            assertThat(seen).hasSize(size);
            assertThatThrownBy(keys::next).isInstanceOf(NoSuchElementException.class);
            assertThat(keys.hasNext()).isFalse();
        }
        // a trie whose deepest level is a collision node, and one of a single child chain
        final BitmapIndexedMapNode<Key, Integer> colliding = persistent(java.util.List.of(new Key(9, 0), new Key(9, 1), new Key(9 | (1 << 30), 0)), ids(3));
        final java.util.List<Integer> values = new ArrayList<>();
        colliding.valuesIterator().forEachRemaining(values::add);
        assertThat(values).containsExactlyInAnyOrder(0, 1, 2);
    }

    // -- the builder: the built trie is the persistent one

    @Test
    public void shouldBuildTheTrieOfSuccessivePutsAtEveryBoundary() {
        for (int size : SIZES) {
            final java.util.List<Integer> keys = ids(size);
            final java.util.List<String> values = keys.stream().map(i -> "v" + i).toList();
            final BitmapIndexedMapNode<Integer, String> built = built(keys, values);
            assertValid(built);
            assertSameShape(persistent(keys, values), built, true);
        }
    }

    @Test
    public void shouldBuildTheTrieOfSuccessivePutsOnRandomKeysWithDuplicatesAndCollisions() {
        final Random random = new Random(SEED);
        for (int round = 0; round < 150; round++) {
            final int size = random.nextInt(3000);
            // a small hash range makes collision nodes, a small id range makes duplicate keys
            final int hashRange = 1 + random.nextInt(round % 3 == 0 ? 50 : Integer.MAX_VALUE);
            final int idRange = 1 + random.nextInt(4);
            final java.util.List<Key> keys = new ArrayList<>(size);
            final java.util.List<Integer> values = new ArrayList<>(size);
            final java.util.Map<Key, Kept> model = new java.util.HashMap<>();
            for (int i = 0; i < size; i++) {
                final int hash = random.nextBoolean() ? random.nextInt(hashRange) : -random.nextInt(hashRange);
                final Key key = new Key(hash, random.nextInt(idRange));
                final Integer value = i;
                keys.add(key);
                values.add(value);
                model.put(key, new Kept(key, value));
            }
            final BitmapIndexedMapNode<Key, Integer> built = built(keys, values);
            assertValid(built);
            assertSameShape(persistent(keys, values), built, true);
            assertHolds(built, model);
        }
    }

    @Test
    public void shouldBuildTheDeepestShiftAndUpdateInsideACollisionNode() {
        // hashes that differ only in bits 30 and 31, and keys of one hash
        final java.util.List<Key> keys = java.util.List.of(new Key(0, 0), new Key(1 << 30, 0), new Key(2 << 30, 0),
                new Key(3 << 30, 0), new Key(3 << 30, 1), new Key(0, 1), new Key(0, 0), new Key(-7, 0), new Key(-7, 1),
                new Key(-7, 2), new Key(-7, 1), new Key(-7, 0));
        final java.util.List<Integer> values = ids(keys.size());
        final BitmapIndexedMapNode<Key, Integer> built = built(keys, values);
        assertValid(built);
        assertSameShape(persistent(keys, values), built, true);
        assertThat(built.size()).isEqualTo(9);
        assertThat(built.getOrElse(new Key(-7, 0), null)).isEqualTo(11);
        assertThat(built.getOrElse(new Key(-7, 1), null)).isEqualTo(10);
        assertThat(built.getOrElse(new Key(0, 0), null)).isEqualTo(6);
    }

    @Test
    public void shouldKeepTheLastKeyObjectOfEqualKeys() {
        final String first = new String("k");
        final String last = new String("k");
        final BitmapIndexedMapNode<String, Integer> built = built(java.util.List.of(first, "x", last), java.util.List.of(1, 2, 3));
        assertThat(built.getEntry("k")._1()).isSameAs(last);
        assertThat(built.getOrElse("k", 0)).isEqualTo(3);
    }

    // -- the builder: ownership

    @Test
    public void shouldOwnEveryNodeOfATrieBuiltFromNothing() {
        for (int size : SIZES) {
            final BitmapIndexedMapNode<Integer, Integer> built = built(ids(size), ids(size));
            final Set<Object> owners = Collections.newSetFromMap(new IdentityHashMap<>());
            internalNodes(built).forEach(node -> owners.add(((BitmapIndexedMapNode<?, ?>) node).owner));
            if (size == 0) {
                assertThat(built).isSameAs(MapNode.empty());
            } else {
                assertThat(owners).hasSize(1);
                assertThat(owners.iterator().next()).isNotNull();
            }
        }
        // a persistent update never owns a node
        internalNodes(persistent(ids(1025), ids(1025))).forEach(node -> assertThat(((BitmapIndexedMapNode<?, ?>) node).owner).isNull());
        assertThat(((BitmapIndexedMapNode<?, ?>) MapNode.empty()).owner).isNull();
    }

    @Test
    public void shouldNeverChangeTheReturnedTrie() {
        final java.util.List<Integer> keys = ids(1025);
        final BitmapIndexedMapNode<Integer, Integer> built = built(keys, keys);
        final String before = describe(built);
        BitmapIndexedMapNode<Integer, Integer> updated = built;
        for (int i = 0; i < 2000; i += 3) {
            updated = updated.updated(i, -i).removed(i + 1);
        }
        assertThat(describe(built)).isEqualTo(before);
        assertSameShape(persistent(keys, keys), built, true);
        // the persistent operations on the built trie create nodes of their own
        final Set<Object> builtNodes = internalNodes(built);
        for (Object node : internalNodes(updated)) {
            if (!builtNodes.contains(node)) {
                assertThat(((BitmapIndexedMapNode<?, ?>) node).owner).isNull();
            }
        }
    }

    @Test
    public void shouldAdoptATrieOnAnEmptyBuilderAndCopyItsNodesOnFirstWrite() {
        final Random random = new Random(SEED);
        for (int size : new int[] { 1, 2, 31, 32, 33, 1023, 1024, 1025, 32768 }) {
            final java.util.List<Integer> keys = ids(size);
            final BitmapIndexedMapNode<Integer, Integer> source = persistent(keys, keys);
            final String sourceBefore = describe(source);
            final Set<Object> sourceNodes = internalNodes(source);

            final HashMapBuilder<Integer, Integer> builder = new HashMapBuilder<>("test");
            builder.putAll(source);
            final java.util.List<Integer> moreKeys = new ArrayList<>(keys);
            final java.util.List<Integer> moreValues = new ArrayList<>(keys);
            for (int i = 0; i < 50; i++) {
                // boxed once: the shapes are compared by the identity of keys and values
                final Integer key = random.nextInt(size * 2 + 1);
                final Integer value = -key;
                builder.put(key, value);
                moreKeys.add(key);
                moreValues.add(value);
            }
            final BitmapIndexedMapNode<Integer, Integer> built = builder.result();

            assertThat(describe(source)).as("the adopted trie is unchanged").isEqualTo(sourceBefore);
            assertValid(built);
            assertSameShape(persistent(moreKeys, moreValues), built, true);
            // every node is either one of the source, untouched, or a copy owned by the builder
            final Set<Object> owners = Collections.newSetFromMap(new IdentityHashMap<>());
            int shared = 0;
            for (Object node : internalNodes(built)) {
                if (sourceNodes.contains(node)) {
                    shared++;
                    assertThat(((BitmapIndexedMapNode<?, ?>) node).owner).isNull();
                } else {
                    owners.add(((BitmapIndexedMapNode<?, ?>) node).owner);
                }
            }
            assertThat(owners).doesNotContainNull().hasSizeLessThanOrEqualTo(1);
            if (size >= 1024) {
                assertThat(shared).as("untouched nodes of the source are shared").isPositive();
            }
        }
    }

    @Test
    public void shouldReturnAnAdoptedTrieAsItIsWhenNothingIsPutAfterIt() {
        final BitmapIndexedMapNode<Integer, Integer> source = persistent(ids(100), ids(100));
        final HashMapBuilder<Integer, Integer> builder = new HashMapBuilder<>("test");
        builder.putAll(source);
        builder.putAll(MapNode.empty());
        assertThat(builder.size()).isEqualTo(100);
        assertThat(builder.result()).isSameAs(source);
    }

    @Test
    public void shouldBuildIndependentTriesFromOneAdoptedTrie() {
        final java.util.List<Integer> keys = ids(1025);
        final BitmapIndexedMapNode<Integer, Integer> source = persistent(keys, keys);
        final String sourceBefore = describe(source);
        final HashMapBuilder<Integer, Integer> left = new HashMapBuilder<>("test");
        final HashMapBuilder<Integer, Integer> right = new HashMapBuilder<>("test");
        left.putAll(source);
        right.putAll(source);
        for (int i = 0; i < 1025; i += 2) {
            left.put(i, -1);
            if (i + 1 < 1025) {
                right.put(i + 1, -2);
            }
        }
        left.put(5000, 5000);
        final BitmapIndexedMapNode<Integer, Integer> l = left.result();
        final BitmapIndexedMapNode<Integer, Integer> r = right.result();
        assertThat(describe(source)).isEqualTo(sourceBefore);
        assertValid(l);
        assertValid(r);
        assertThat(l.size()).isEqualTo(1026);
        assertThat(r.size()).isEqualTo(1025);
        for (int i = 0; i < 1025; i++) {
            assertThat(source.getOrElse(i, null)).isEqualTo(i);
            assertThat(l.getOrElse(i, null)).isEqualTo(i % 2 == 0 ? -1 : i);
            assertThat(r.getOrElse(i, null)).isEqualTo(i % 2 == 1 ? -2 : i);
        }
    }

    @Test
    public void shouldPutAllOfATrieIntoANonEmptyBuilderAsSuccessivePuts() {
        final java.util.List<Integer> first = java.util.List.of(1, 2, 3, 40, 500);
        final java.util.List<Integer> second = ids(1025);
        final BitmapIndexedMapNode<Integer, Integer> source = persistent(second, second);
        final String sourceBefore = describe(source);
        final HashMapBuilder<Integer, Integer> builder = new HashMapBuilder<>("test");
        for (Integer key : first) {
            builder.put(key, -key);
        }
        builder.putAll(source);
        builder.putAll(MapNode.empty());
        final BitmapIndexedMapNode<Integer, Integer> built = builder.result();
        assertThat(describe(source)).isEqualTo(sourceBefore);
        assertValid(built);
        // the entries of the argument win, as later puts do
        BitmapIndexedMapNode<Integer, Integer> expected = persistent(first, first.stream().map(i -> -i).toList());
        for (Object[] entry : entries(source)) {
            expected = expected.updated((Integer) entry[0], (Integer) entry[1]);
        }
        assertSameShape(expected, built, true);
        internalNodes(built).forEach(node -> assertThat(((BitmapIndexedMapNode<?, ?>) node).owner).isNotNull());
    }

    @Test
    public void shouldCopyAnAdoptedPersistentNodeBeforeWritingToIt() {
        // the first write through the adopted root adds an entry: the root is not owned and must be copied, not grown
        final BitmapIndexedMapNode<Integer, Integer> source = MapNode.<Integer, Integer> empty().updated(0, 0).updated(1, 1);
        final String before = describe(source);
        final HashMapBuilder<Integer, Integer> builder = new HashMapBuilder<>("test");
        builder.putAll(source);
        builder.put(2, 2);
        // and pushes an entry of it down into a new child, and replaces the value of another
        builder.put(32, 32);
        builder.put(1, -1);
        final BitmapIndexedMapNode<Integer, Integer> built = builder.result();
        assertThat(describe(source)).isEqualTo(before);
        assertThat(source.dataMap).isEqualTo(0b11);
        assertThat(source.size()).isEqualTo(2);
        assertThat(source.getOrElse(1, null)).isEqualTo(1);
        assertValid(built);
        assertThat(built.size()).isEqualTo(4);
        assertThat(built).isNotSameAs(source);
    }

    // a trie made by persistent operations only: puts, and sometimes removals
    private static BitmapIndexedMapNode<Key, Integer> persistentTrie(Random random, java.util.Map<Key, Integer> oracle) {
        BitmapIndexedMapNode<Key, Integer> trie = MapNode.empty();
        final int size = random.nextInt(4) == 0 ? random.nextInt(40) : random.nextInt(1500);
        for (int i = 0; i < size; i++) {
            final Key key = randomKey(random);
            trie = trie.updated(key, i);
            oracle.put(key, i);
        }
        if (random.nextBoolean()) {
            for (Key key : new ArrayList<>(oracle.keySet())) {
                if (random.nextInt(4) != 0) {
                    trie = trie.removed(key);
                    oracle.remove(key);
                }
            }
        }
        return trie;
    }

    private static java.util.Map<Key, Integer> contents(BitmapIndexedMapNode<Key, Integer> trie) {
        final java.util.Map<Key, Integer> result = new java.util.HashMap<>();
        trie.iterator(Tuple::of).forEachRemaining(t -> result.put(t._1(), t._2()));
        return result;
    }

    @Test
    public void shouldNeverChangeAnAdoptedTrieBuiltByPersistentOperations() {
        for (long seed = 1; seed <= 12; seed++) {
            final Random random = new Random(SEED + seed);
            final java.util.List<BitmapIndexedMapNode<Key, Integer>> pool = new ArrayList<>();
            final java.util.List<java.util.Map<Key, Integer>> poolContents = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                final java.util.Map<Key, Integer> oracle = new java.util.HashMap<>();
                pool.add(persistentTrie(random, oracle));
                poolContents.add(oracle);
            }
            for (int step = 0; step < 25; step++) {
                // every node reachable from every trie of the pool, by identity and fields, before the builders run
                final java.util.List<String> snapshots = pool.stream().map(ChampValidity::describe).toList();
                final int from = random.nextInt(pool.size());
                final BitmapIndexedMapNode<Key, Integer> source = pool.get(from);

                // two builders adopt the same trie and are written to in turn
                final HashMapBuilder<Key, Integer> left = new HashMapBuilder<>("test");
                final HashMapBuilder<Key, Integer> right = new HashMapBuilder<>("test");
                left.putAll(source);
                right.putAll(source);
                final java.util.Map<Key, Integer> leftOracle = new java.util.HashMap<>(poolContents.get(from));
                final java.util.Map<Key, Integer> rightOracle = new java.util.HashMap<>(poolContents.get(from));
                final java.util.List<Key> sourceKeys = new ArrayList<>(poolContents.get(from).keySet());
                final int puts = random.nextInt(4) == 0 ? random.nextInt(5) : random.nextInt(600);
                for (int i = 0; i < puts; i++) {
                    // a new object equal to a key of the source, or a fresh key
                    final Key key = (!sourceKeys.isEmpty() && random.nextBoolean())
                                    ? sourceKeys.get(random.nextInt(sourceKeys.size()))
                                    : randomKey(random);
                    final Key equalKey = new Key(key.hash(), key.id());
                    left.put(equalKey, -i);
                    leftOracle.put(equalKey, -i);
                    final Key other = randomKey(random);
                    right.put(other, i);
                    rightOracle.put(other, i);
                }
                if (random.nextInt(3) == 0) {
                    // a second trie of the pool into the non-empty builder
                    final int second = random.nextInt(pool.size());
                    left.putAll(pool.get(second));
                    leftOracle.putAll(poolContents.get(second));
                }
                final BitmapIndexedMapNode<Key, Integer> l = left.result();
                final BitmapIndexedMapNode<Key, Integer> r = right.result();

                for (int i = 0; i < pool.size(); i++) {
                    assertThat(describe(pool.get(i))).as("seed %s, step %s, trie %s of the pool", seed, step, i).isEqualTo(snapshots.get(i));
                    assertThat(contents(pool.get(i))).isEqualTo(poolContents.get(i));
                }
                assertValid(l);
                assertValid(r);
                assertThat(contents(l)).isEqualTo(leftOracle);
                assertThat(contents(r)).isEqualTo(rightOracle);

                // persistent derivatives of the results and of the source join the pool, to be adopted in later steps
                BitmapIndexedMapNode<Key, Integer> derived = random.nextBoolean() ? l : source;
                final java.util.Map<Key, Integer> derivedOracle = new java.util.HashMap<>(derived == l ? leftOracle : poolContents.get(from));
                for (Key key : new ArrayList<>(derivedOracle.keySet())) {
                    if (random.nextInt(3) == 0) {
                        derived = derived.removed(key);
                        derivedOracle.remove(key);
                    }
                }
                for (int i = 0; i < 20; i++) {
                    final Key key = randomKey(random);
                    derived = derived.updated(key, 1000 + i);
                    derivedOracle.put(key, 1000 + i);
                }
                pool.add(l);
                poolContents.add(leftOracle);
                pool.add(r);
                poolContents.add(rightOracle);
                pool.add(derived);
                poolContents.add(derivedOracle);
                while (pool.size() > 8) {
                    final int drop = random.nextInt(pool.size());
                    pool.remove(drop);
                    poolContents.remove(drop);
                }
            }
        }
    }

    @Test
    public void shouldRefuseAnyUseAfterResult() {
        final HashMapBuilder<Integer, Integer> builder = new HashMapBuilder<>("Some.Builder");
        builder.put(1, 1);
        final BitmapIndexedMapNode<Integer, Integer> built = builder.result();
        assertThatThrownBy(builder::checkOpen).isInstanceOf(IllegalStateException.class)
                .hasMessage("result() has already been called on this Some.Builder");
        assertThatThrownBy(builder::size).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(builder::result).isInstanceOf(IllegalStateException.class);
        assertThat(built.size()).isEqualTo(1);
    }

    @Test
    public void shouldBuildTheEmptyTrie() {
        assertThat(new HashMapBuilder<Integer, Integer>("test").result()).isSameAs(MapNode.empty());
    }
}
