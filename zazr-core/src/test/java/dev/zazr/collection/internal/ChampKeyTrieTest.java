package dev.zazr.collection.internal;

import dev.zazr.collection.Vector;
import java.util.ArrayList;
import java.util.Random;
import org.junit.jupiter.api.Test;

import static dev.zazr.collection.internal.ChampValidity.assertSameShape;
import static dev.zazr.collection.internal.ChampValidity.assertValid;
import static dev.zazr.collection.internal.ChampValidity.describe;
import static org.assertj.core.api.Assertions.assertThat;

/// The key trie of a map trie: the set trie its keys make, copied node for node. It is the trie successive persistent
/// additions of the keys, in the map's iteration order, produce: same bitmaps, hashes and sizes, the same key objects,
/// and the same order inside a collision node.
public class ChampKeyTrieTest {

    private static final long SEED = 20261006L;
    private static final int[] SIZES = {0, 1, 2, 31, 32, 33, 1023, 1024, 1025, 32768};

    // keys of mixed hashes 0 to size - 1: the first 32 fill the root, the first 1024 two levels
    private static java.util.List<ChampMapTest.Key> keys(int size) {
        java.util.List<ChampMapTest.Key> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            result.add(new ChampMapTest.Key(i, 0));
        }
        return result;
    }

    // hashes that collide often and reach every level, with a few ids per hash for the collision nodes
    private static ChampMapTest.Key randomKey(Random random) {
        int hash = switch (random.nextInt(4)) {
            case 0 -> random.nextInt(8);
            case 1 -> random.nextInt(64);
            case 2 -> random.nextInt(64) << (5 * (1 + random.nextInt(6)));
            default -> random.nextInt();
        };
        return new ChampMapTest.Key(hash, random.nextInt(3));
    }

    private static <K> BitmapIndexedMapNode<K, Integer> persistentMap(java.util.List<K> keys) {
        return Vector.range(0, keys.size())
                .foldLeft(MapNode.<K, Integer>empty(), (trie, i) -> trie.updated(keys.get(i), i));
    }

    private static <K> BitmapIndexedMapNode<K, Integer> builtMap(java.util.List<K> keys) {
        HashMapBuilder<K, Integer> builder = new HashMapBuilder<>("test");
        for (int i = 0; i < keys.size(); i++) {
            builder.put(keys.get(i), i);
        }
        return builder.result();
    }

    // the set of the keys of `map`, by successive persistent additions in its iteration order
    private static <K> BitmapIndexedSetNode<K> rebuilt(BitmapIndexedMapNode<K, ?> map) {
        return Vector.ofAll(keysOf(map)).foldLeft(SetNode.<K>empty(), (trie, key) -> trie.updated(key, false));
    }

    private static <K> java.util.List<K> keysOf(MapNode<K, ?> map) {
        java.util.List<K> result = new ArrayList<>();
        map.keysIterator().forEachRemaining(result::add);
        return result;
    }

    private static <T> java.util.List<T> elements(SetNode<T> trie) {
        java.util.List<T> result = new ArrayList<>();
        trie.iterator().forEachRemaining(result::add);
        return result;
    }

    private static <K> void assertKeyTrie(BitmapIndexedMapNode<K, ?> map) {
        BitmapIndexedSetNode<K> keys = map.keyTrie();
        assertThat(assertValid(keys)).isEqualTo(map.size());
        assertSameShape(rebuilt(map), keys, true);
        assertThat(elements(keys)).containsExactlyElementsOf(keysOf(map));
        assertThat(keys.owner).isNull();
    }

    @Test
    public void keyTrieOfTheEmptyMapIsEmpty() {
        BitmapIndexedSetNode<Object> keys = MapNode.<Object, Object>empty().keyTrie();
        assertThat(assertValid(keys)).isZero();
        assertThat(keys.iterator().hasNext()).isFalse();
    }

    @Test
    public void keyTrieIsTheRebuiltSetAtEveryBoundary() {
        for (int size : SIZES) {
            java.util.List<ChampMapTest.Key> keys = keys(size);
            assertKeyTrie(persistentMap(keys));
            assertKeyTrie(builtMap(keys));
        }
    }

    @Test
    public void keyTrieIsTheRebuiltSetWithCollisionsAndRemovals() {
        Random random = new Random(SEED);
        for (int round = 0; round < 200; round++) {
            int size = random.nextInt(300);
            java.util.List<ChampMapTest.Key> keys = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                keys.add(randomKey(random));
            }
            BitmapIndexedMapNode<ChampMapTest.Key, Integer> map =
                    random.nextBoolean() ? persistentMap(keys) : builtMap(keys);
            BitmapIndexedMapNode<ChampMapTest.Key, Integer> removed = Vector.ofAll(keys.subList(0, size / 3))
                    .foldLeft(map, (acc, key) -> random.nextBoolean() ? acc.removed(key) : acc);
            assertKeyTrie(map);
            assertKeyTrie(removed);
        }
    }

    @Test
    public void keyTrieKeepsTheOrderOfACollisionNode() {
        ChampMapTest.Key a = new ChampMapTest.Key(7, 0);
        ChampMapTest.Key b = new ChampMapTest.Key(7, 1);
        ChampMapTest.Key c = new ChampMapTest.Key(7, 2);
        BitmapIndexedMapNode<ChampMapTest.Key, Integer> map = MapNode.<ChampMapTest.Key, Integer>empty()
                .updated(a, 0)
                .updated(b, 1)
                .updated(c, 2)
                .removed(a)
                .updated(a, 3);
        BitmapIndexedSetNode<ChampMapTest.Key> keys = map.keyTrie();
        assertThat(elements(keys)).containsExactly(b, c, a);
        assertKeyTrie(map);
    }

    @Test
    public void keyTrieHoldsTheKeyObjectsTheMapKept() {
        ChampMapTest.Key first = new ChampMapTest.Key(5, 0);
        ChampMapTest.Key second = new ChampMapTest.Key(5, 0);
        ChampMapTest.Key collidingFirst = new ChampMapTest.Key(5 << 30, 1);
        ChampMapTest.Key collidingSecond = new ChampMapTest.Key(5 << 30, 1);
        BitmapIndexedMapNode<ChampMapTest.Key, Integer> map = MapNode.<ChampMapTest.Key, Integer>empty()
                .updated(first, 0)
                .updated(collidingFirst, 1)
                .updated(new ChampMapTest.Key(5 << 30, 2), 2)
                .updated(second, 3)
                .updated(collidingSecond, 4);
        java.util.List<ChampMapTest.Key> keys = elements(map.keyTrie());
        assertThat(keys).hasSize(3);
        assertThat(keys.stream().filter(key -> key == second)).hasSize(1);
        assertThat(keys.stream().filter(key -> key == collidingSecond)).hasSize(1);
        assertKeyTrie(map);
    }

    @Test
    public void keyTrieLeavesTheMapUnchangedUnderLaterUpdatesOfTheSet() {
        Random random = new Random(SEED + 1);
        java.util.List<ChampMapTest.Key> keys = new ArrayList<>();
        for (int i = 0; i < 2000; i++) {
            keys.add(randomKey(random));
        }
        BitmapIndexedMapNode<ChampMapTest.Key, Integer> map = builtMap(keys);
        String mapBefore = describe(map);
        BitmapIndexedSetNode<ChampMapTest.Key> keyTrie = map.keyTrie();
        String keysBefore = describe(keyTrie);

        // persistent updates of the set, then a builder that adopts it and adds through its nodes in place
        java.util.List<ChampMapTest.Key> more = new ArrayList<>();
        for (int i = 0; i < 2000; i++) {
            more.add(randomKey(random));
        }
        BitmapIndexedSetNode<ChampMapTest.Key> updated = Vector.ofAll(more)
                .foldLeft(keyTrie, (acc, key) -> random.nextBoolean() ? acc.updated(key, true) : acc.removed(key));
        assertValid(updated);
        HashSetBuilder<ChampMapTest.Key> builder = new HashSetBuilder<>("test");
        builder.addAll(keyTrie);
        for (ChampMapTest.Key key : more) {
            builder.add(key);
        }
        assertValid(builder.result());

        // the map's own updates, persistent and through a builder
        BitmapIndexedMapNode<ChampMapTest.Key, Integer> mapUpdated =
                Vector.ofAll(more).foldLeft(map, (acc, key) -> acc.removed(key));
        ChampValidity.assertValid(mapUpdated);

        assertThat(describe(map)).isEqualTo(mapBefore);
        assertThat(describe(keyTrie)).isEqualTo(keysBefore);
        ChampValidity.assertValid(map);
        assertKeyTrie(map);
        assertSameShape(map.keyTrie(), keyTrie, true);
    }
}
