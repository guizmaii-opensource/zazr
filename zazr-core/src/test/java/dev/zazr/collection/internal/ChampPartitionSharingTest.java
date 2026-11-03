package dev.zazr.collection.internal;

import dev.zazr.Tuple2;
import dev.zazr.collection.HashMap;
import dev.zazr.collection.HashSet;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.IntPredicate;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// `partition` of `HashSet` and `HashMap` shares, by identity, every child of the trie whose elements all go to one
/// side: with a predicate on the bits of the hash read by the first level, each side holds no node of its own but its
/// root, and with one on the bits of the second level, none but its root and the root's children. Many keys collide,
/// so the collision nodes below the last level are covered too.
class ChampPartitionSharingTest {

    private static final long SEED = 20261103L;
    private static final int[] SIZES = {33, 1024, 3000};

    /// An element of a chosen hash code: two of one hash and different ids collide.
    record Key(int hash, int id) {
        @Override
        public int hashCode() {
            return hash;
        }
    }

    // keys of few distinct hashes, three ids each, so that most hashes collide
    private static List<Key> keys(Random random, int size) {
        List<Key> keys = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            keys.add(new Key(random.nextInt(size / 3 + 1) * 0x9E3779B9, random.nextInt(3)));
        }
        return keys;
    }

    // the side of a key by the five bits of its hash read at `shift`
    private static IntPredicate bySlot(int shift) {
        return hash -> ((ChampNode.improve(hash) >>> shift) & 31) < 16;
    }

    // every node of the subtree of `node`, by identity
    private static void collect(ChampNode<?> node, Map<Object, Boolean> nodes) {
        nodes.put(node, Boolean.TRUE);
        for (int i = 0; i < node.nodeArity(); i++) {
            collect((ChampNode<?>) node.getNode(i), nodes);
        }
    }

    // the nodes of `node` that are not nodes of the original trie; a shared node is not walked
    private static int fresh(ChampNode<?> node, Map<Object, Boolean> original) {
        if (original.containsKey(node)) {
            return 0;
        }
        return 1
                + java.util.stream.IntStream.range(0, node.nodeArity())
                        .map(i -> fresh((ChampNode<?>) node.getNode(i), original))
                        .sum();
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

    @Test
    void hashSetPartitionSharesTheChildrenThatGoToOneSide() {
        Random random = new Random(SEED);
        for (int size : SIZES) {
            HashSet<Key> set = HashSet.ofAll(keys(random, size));
            BitmapIndexedSetNode<Key> root = field(set, HashSet.class, "tree");
            Map<Object, Boolean> original = new IdentityHashMap<>();
            collect(root, original);
            for (int level = 0; level < 2; level++) {
                IntPredicate side = bySlot(5 * level);
                Tuple2<HashSet<Key>, HashSet<Key>> sides = set.partition(key -> side.test(key.hashCode()));
                int allowed = level == 0 ? 1 : 1 + root.nodeArity();
                for (HashSet<Key> half : List.of(sides._1(), sides._2())) {
                    BitmapIndexedSetNode<Key> trie = field(half, HashSet.class, "tree");
                    assertThat(fresh(trie, original))
                            .as("nodes of their own, level " + level + ", " + size + " elements")
                            .isLessThanOrEqualTo(allowed);
                }
            }
        }
    }

    @Test
    void hashMapPartitionSharesTheChildrenThatGoToOneSide() {
        Random random = new Random(SEED + 1);
        for (int size : SIZES) {
            @SuppressWarnings("Var")
            HashMap<Key, Integer> map = HashMap.empty();
            for (Key key : keys(random, size)) {
                map = map.put(key, key.id());
            }
            BitmapIndexedMapNode<Key, Integer> root = field(map, HashMap.class, "trie");
            Map<Object, Boolean> original = new IdentityHashMap<>();
            collect(root, original);
            for (int level = 0; level < 2; level++) {
                IntPredicate side = bySlot(5 * level);
                Tuple2<HashMap<Key, Integer>, HashMap<Key, Integer>> sides =
                        map.partition(entry -> side.test(entry._1().hashCode()));
                int allowed = level == 0 ? 1 : 1 + root.nodeArity();
                for (HashMap<Key, Integer> half : List.of(sides._1(), sides._2())) {
                    BitmapIndexedMapNode<Key, Integer> trie = field(half, HashMap.class, "trie");
                    assertThat(fresh(trie, original))
                            .as("nodes of their own, level " + level + ", " + size + " entries")
                            .isLessThanOrEqualTo(allowed);
                }
            }
        }
    }
}
