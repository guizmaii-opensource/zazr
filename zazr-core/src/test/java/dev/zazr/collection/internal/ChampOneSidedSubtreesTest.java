package dev.zazr.collection.internal;

import dev.zazr.Tuple2;
import dev.zazr.collection.HashMap;
import dev.zazr.collection.HashSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// `filter`, `reject`, `partition` and `diff` of `HashSet` and `HashMap` on a trie of a chosen shape, where whole
/// subtrees go to one side: the result holds the expected elements, has the shape of the same elements added one by
/// one, and shares by identity every subtree of the original whose elements all go to it. The keys are placed slot by
/// slot by their hash code, so the cases reach the children dropped before and after one that changes in part, the
/// last child, a child left with one element, inline elements beside children, and collision nodes.
class ChampOneSidedSubtreesTest {

    /// A key of a chosen hash code: two of one hash and different ids collide.
    record Key(int hash, int id) {
        @Override
        public int hashCode() {
            return hash;
        }
    }

    // the inverse of `ChampNode.improve`: every step of it is a bijection of the 32-bit integers
    private static int unimprove(int improved) {
        int h3 = improved ^ (improved >>> 10) ^ (improved >>> 20) ^ (improved >>> 30);
        int h2 = h3 * inverse(17);
        int h1 = h2 ^ (h2 >>> 14) ^ (h2 >>> 28);
        // h1 = hashCode + ~(hashCode << 9) = hashCode * -511 - 1
        return (h1 + 1) * inverse(-511);
    }

    // the inverse of an odd number modulo 2^32, by Newton's iteration
    private static int inverse(int odd) {
        return java.util.stream.IntStream.range(0, 5).reduce(odd, (x, i) -> x * (2 - odd * x));
    }

    // a key in slot `s0` at the root, `s1` at the first level and `s2` at the second
    private static Key key(int s0, int s1, int s2, int id) {
        int hash = unimprove(s0 | (s1 << 5) | (s2 << 10) | (1 << 20));
        return new Key(hash, id);
    }

    /// The trie of the tests: an inline key at the root (slot 0); four subtrees of depth 3 (slots 1 to 4), each an
    /// inline key and two children of three and two keys; one subtree (slot 5) with an inline key and two collision
    /// nodes, of three and two keys.
    private static final List<Key> KEYS = keys();

    private static List<Key> keys() {
        List<Key> keys = new ArrayList<>();
        keys.add(key(0, 0, 0, 0));
        for (int s0 = 1; s0 <= 4; s0++) {
            keys.add(key(s0, 0, 0, 0));
            for (int s2 = 0; s2 < 3; s2++) {
                keys.add(key(s0, 1, s2, 0));
            }
            for (int s2 = 0; s2 < 2; s2++) {
                keys.add(key(s0, 2, s2, 0));
            }
        }
        keys.add(key(5, 0, 0, 0));
        for (int id = 0; id < 3; id++) {
            keys.add(key(5, 1, 0, id));
        }
        for (int id = 0; id < 2; id++) {
            keys.add(key(5, 2, 0, id));
        }
        return keys;
    }

    // the keys of the subtree at slot `s0` of the root, and at slot `s1` below it when `s1` is not negative
    private static List<Key> under(int s0, int s1) {
        return KEYS.stream()
                .filter(k -> slot(k, 0) == s0 && (s1 < 0 || slot(k, 5) == s1))
                .toList();
    }

    private static int slot(Key key, int shift) {
        return (ChampNode.improve(key.hashCode()) >>> shift) & 31;
    }

    @SafeVarargs
    private static Set<Key> union(List<Key>... parts) {
        Set<Key> all = new LinkedHashSet<>();
        for (List<Key> part : parts) {
            all.addAll(part);
        }
        return all;
    }

    private static Set<Key> without(Set<Key> keys, Key... removed) {
        Set<Key> result = new LinkedHashSet<>(keys);
        result.removeAll(List.of(removed));
        return result;
    }

    /// The keys for which the predicate of each case holds.
    private static Map<String, Set<Key>> cases() {
        Map<String, Set<Key>> cases = new LinkedHashMap<>();
        cases.put(
                "whole subtrees at depth 2 on each side",
                union(under(0, -1), under(1, -1), under(3, -1), under(2, 0), under(2, 1)));
        cases.put("the last child of the root and of its node differs", without(union(KEYS), key(5, 2, 0, 1)));
        cases.put("only one key of the last child", union(List.of(key(5, 2, 0, 1))));
        cases.put(
                "whole children dropped before and after one changed in part",
                without(union(under(2, -1), under(3, -1), List.of(key(5, 1, 0, 0))), key(2, 1, 0, 0)));
        cases.put(
                "a child left with one key between dropped ones",
                union(under(0, -1), List.of(key(2, 2, 1, 0)), under(4, -1)));
        cases.put(
                "collision nodes split first kept then not, and the reverse",
                without(union(KEYS), key(5, 1, 0, 1), key(5, 1, 0, 2), key(5, 2, 0, 0)));
        cases.put("collision nodes whole on one side", union(under(5, 1), under(1, -1)));
        cases.put("the inline key only", union(under(0, -1)));
        cases.put("every key", union(KEYS));
        cases.put("no key", union());
        Random random = new Random(20261104L);
        for (int round = 0; round < 40; round++) {
            Set<Key> side = new LinkedHashSet<>();
            for (int s0 = 0; s0 <= 5; s0++) {
                for (int s1 = 0; s1 <= 2; s1++) {
                    // each group of keys goes whole to one side, or is split at random
                    int mode = random.nextInt(3);
                    for (Key key : under(s0, s1)) {
                        if (mode == 0 || (mode == 2 && random.nextBoolean())) {
                            side.add(key);
                        }
                    }
                }
            }
            cases.put("random " + round, side);
        }
        return cases;
    }

    private static <T> T field(Object owner, Class<?> type, String name) {
        try {
            java.lang.reflect.Field field = type.getDeclaredField(name);
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            T value = (T) field.get(owner);
            return value;
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private static SetNode<Key> tree(HashSet<Key> set) {
        return field(set, HashSet.class, "tree");
    }

    private static MapNode<Key, Integer> trie(HashMap<Key, Integer> map) {
        return field(map, HashMap.class, "trie");
    }

    // the keys of a subtree
    private static void keysOf(ChampNode<?> node, Set<Object> into) {
        for (int i = 0; i < node.payloadArity(); i++) {
            into.add(
                    switch (node) {
                        case SetNode<?> set -> set.getPayload(i);
                        case MapNode<?, ?> map -> map.getKey(i);
                        default -> throw new AssertionError(node);
                    });
        }
        for (int i = 0; i < node.nodeArity(); i++) {
            keysOf((ChampNode<?>) node.getNode(i), into);
        }
    }

    private static int dataMap(ChampNode<?> node) {
        return switch (node) {
            case BitmapIndexedSetNode<?> set -> set.dataMap;
            case BitmapIndexedMapNode<?, ?> map -> map.dataMap;
            default -> 0;
        };
    }

    private static int nodeMap(ChampNode<?> node) {
        return switch (node) {
            case BitmapIndexedSetNode<?> set -> set.nodeMap;
            case BitmapIndexedMapNode<?, ?> map -> map.nodeMap;
            default -> 0;
        };
    }

    /// Asserts that `result`, a node of a side, is the node of `original` at the same place when all the keys of that
    /// node go to the side, and walks down the children otherwise.
    private static void assertShares(ChampNode<?> result, ChampNode<?> original, Set<Key> side, String path) {
        Set<Object> keys = new LinkedHashSet<>();
        keysOf(original, keys);
        if (side.containsAll(keys)) {
            assertThat(result).as("the subtree at " + path).isSameAs(original);
            return;
        }
        assertThat(result).as("a new node at " + path).isNotSameAs(original);
        @SuppressWarnings("Var")
        int bits = nodeMap(result);
        for (int i = 0; i < result.nodeArity(); i++) {
            int bitpos = Integer.lowestOneBit(bits);
            bits ^= bitpos;
            assertThat(nodeMap(original) & bitpos)
                    .as("a child of " + path + " the original has")
                    .isNotZero();
            int index = Integer.bitCount(nodeMap(original) & (bitpos - 1));
            assertShares(
                    (ChampNode<?>) result.getNode(i),
                    (ChampNode<?>) original.getNode(index),
                    side,
                    path + "/" + Integer.numberOfTrailingZeros(bitpos));
        }
    }

    // the shape of a subtree: its bitmaps, size and key hash sum, node by node
    private static String shape(ChampNode<?> node) {
        StringBuilder shape = new StringBuilder();
        shape.append(node.getClass().getSimpleName())
                .append('(')
                .append(Integer.toHexString(dataMap(node)))
                .append(',')
                .append(Integer.toHexString(nodeMap(node)))
                .append(',')
                .append(node.size())
                .append(',')
                .append(node.keyHashSum())
                .append(")[");
        for (int i = 0; i < node.nodeArity(); i++) {
            shape.append(shape((ChampNode<?>) node.getNode(i)));
        }
        return shape.append(']').toString();
    }

    private static final HashSet<Key> SET = HashSet.ofAll(KEYS);

    private static HashMap<Key, Integer> map() {
        @SuppressWarnings("Var")
        HashMap<Key, Integer> map = HashMap.empty();
        for (Key key : KEYS) {
            map = map.put(key, key.hash() + key.id());
        }
        return map;
    }

    private static final HashMap<Key, Integer> MAP = map();

    private static void assertSetSide(HashSet<Key> result, Set<Key> side, String description) {
        assertThat(javaSet(result)).as(description).isEqualTo(side);
        HashSet<Key> fresh = HashSet.ofAll(side);
        assertThat(shape(tree(result))).as(description).isEqualTo(shape(tree(fresh)));
        assertThat(SetNode.sameElements(tree(result), tree(fresh)))
                .as(description)
                .isTrue();
        assertThat(result).as(description).isEqualTo(fresh);
        assertThat(fresh).as(description).isEqualTo(result);
        assertShares(tree(result), tree(SET), side, description + ": root");
        if (side.size() == KEYS.size()) {
            assertThat(result).as(description).isSameAs(SET);
        }
    }

    private static void assertMapSide(HashMap<Key, Integer> result, Set<Key> side, String description) {
        assertThat(javaSet(result.keySet())).as(description).isEqualTo(side);
        @SuppressWarnings("Var")
        HashMap<Key, Integer> fresh = HashMap.empty();
        for (Key key : side) {
            fresh = fresh.put(key, MAP.get(key).get());
            assertThat(result.get(key).get())
                    .as(description)
                    .isSameAs(MAP.get(key).get());
        }
        assertThat(shape(trie(result))).as(description).isEqualTo(shape(trie(fresh)));
        assertThat(MapNode.sameEntries(trie(result), trie(fresh)))
                .as(description)
                .isTrue();
        assertThat(result).as(description).isEqualTo(fresh);
        assertThat(fresh).as(description).isEqualTo(result);
        assertShares(trie(result), trie(MAP), side, description + ": root");
        if (side.size() == KEYS.size()) {
            assertThat(result).as(description).isSameAs(MAP);
        }
    }

    private static Set<Key> javaSet(Iterable<Key> keys) {
        Set<Key> set = new LinkedHashSet<>();
        keys.forEach(set::add);
        return set;
    }

    private static Set<Key> complement(Set<Key> side) {
        Set<Key> others = new LinkedHashSet<>(KEYS);
        others.removeAll(side);
        return others;
    }

    @Test
    void theTrieHasTheShapeTheCasesNeed() {
        for (int s = 0; s < 64; s++) {
            int improved = s * 0x9E3779B9;
            assertThat(ChampNode.improve(unimprove(improved))).isEqualTo(improved);
        }
        SetNode<Key> root = tree(SET);
        assertThat(root.payloadArity()).isEqualTo(1);
        assertThat(root.nodeArity()).isEqualTo(5);
        for (int i = 0; i < 4; i++) {
            SetNode<Key> child = root.getNode(i);
            assertThat(child.payloadArity()).isEqualTo(1);
            assertThat(child.nodeArity()).isEqualTo(2);
        }
        assertThat(shape(root.getNode(4))).contains("HashCollisionSetNode");
        assertThat(shape(trie(MAP).getNode(4))).contains("HashCollisionMapNode");
    }

    @Test
    void hashSetFilterAndReject() {
        cases().forEach((name, side) -> {
            assertSetSide(SET.filter(side::contains), side, "filter, " + name);
            assertSetSide(SET.reject(side::contains), complement(side), "reject, " + name);
        });
    }

    @Test
    void hashSetPartition() {
        cases().forEach((name, side) -> {
            Tuple2<HashSet<Key>, HashSet<Key>> sides = SET.partition(side::contains);
            assertSetSide(sides._1(), side, "partition kept, " + name);
            assertSetSide(sides._2(), complement(side), "partition rejected, " + name);
        });
    }

    @Test
    void hashSetRemoveAllOfAHashSet() {
        cases().forEach((name, side) -> {
            HashSet<Key> removed = HashSet.ofAll(complement(side));
            assertSetSide(SET.removeAll(removed), side, "removeAll, " + name);
        });
    }

    @Test
    void hashMapFilterAndReject() {
        cases().forEach((name, side) -> {
            assertMapSide(MAP.filter((k, v) -> side.contains(k)), side, "filter, " + name);
            assertMapSide(MAP.reject((k, v) -> side.contains(k)), complement(side), "reject, " + name);
        });
    }

    @Test
    void hashMapPartition() {
        cases().forEach((name, side) -> {
            Tuple2<HashMap<Key, Integer>, HashMap<Key, Integer>> sides = MAP.partition(e -> side.contains(e._1()));
            assertMapSide(sides._1(), side, "partition kept, " + name);
            assertMapSide(sides._2(), complement(side), "partition rejected, " + name);
        });
    }
}
