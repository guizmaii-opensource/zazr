package com.guizmaii.zazr.collection.internal;

import com.guizmaii.zazr.collection.internal.HashArrayMappedTrieModule.AbstractNode;
import com.guizmaii.zazr.collection.internal.HashArrayMappedTrieModule.ArrayNode;
import com.guizmaii.zazr.collection.internal.HashArrayMappedTrieModule.EmptyNode;
import com.guizmaii.zazr.collection.internal.HashArrayMappedTrieModule.IndexedNode;
import com.guizmaii.zazr.collection.internal.HashArrayMappedTrieModule.LeafList;
import com.guizmaii.zazr.collection.internal.HashArrayMappedTrieModule.LeafNode;
import com.guizmaii.zazr.collection.internal.HashArrayMappedTrieModule.LeafSingleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class HashArrayMappedTrieBuilderTest {

    private static final long SEED = 20260925L;
    private static final int[] SIZES = { 0, 1, 2, 31, 32, 33, 1023, 1024, 1025, 32768, 100_000 };

    /** A key whose hash code is chosen by the test; two keys are equal when both the hash and the id are. */
    record Key(int hash, int id) {
        @Override
        public int hashCode() {
            return hash;
        }
    }

    // -- builders of the expected and the actual tries

    private static <K, V> HashArrayMappedTrie<K, V> persistent(java.util.List<K> keys, java.util.List<V> values) {
        HashArrayMappedTrie<K, V> trie = HashArrayMappedTrie.empty();
        for (int i = 0; i < keys.size(); i++) {
            trie = trie.put(keys.get(i), values.get(i));
        }
        return trie;
    }

    private static <K, V> HashArrayMappedTrie<K, V> built(java.util.List<K> keys, java.util.List<V> values) {
        final HashArrayMappedTrieBuilder<K, V> builder = new HashArrayMappedTrieBuilder<>("test");
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

    // -- checks

    /** Asserts that two tries are the same node for node: same kinds, bitmaps, counts, sizes, and leaves holding the
     *  same key and value objects in the same order. */
    @SuppressWarnings("unchecked")
    private static void assertSameShape(Object expected, Object actual) {
        assertThat(actual.getClass()).isEqualTo(expected.getClass());
        if (expected instanceof IndexedNode<?, ?> e) {
            final IndexedNode<?, ?> a = (IndexedNode<?, ?>) actual;
            assertThat(a.bitmap).isEqualTo(e.bitmap);
            assertThat(a.size).isEqualTo(e.size);
            assertThat(a.subNodes.length).isEqualTo(e.subNodes.length);
            for (int i = 0; i < e.subNodes.length; i++) {
                assertSameShape(e.subNodes[i], a.subNodes[i]);
            }
        } else if (expected instanceof ArrayNode<?, ?> e) {
            final ArrayNode<?, ?> a = (ArrayNode<?, ?>) actual;
            assertThat(a.count).isEqualTo(e.count);
            assertThat(a.size).isEqualTo(e.size);
            for (int i = 0; i < e.subNodes.length; i++) {
                assertSameShape(e.subNodes[i], a.subNodes[i]);
            }
        } else if (expected instanceof LeafNode<?, ?>) {
            final java.util.List<LeafNode<Object, Object>> e = leaves((LeafNode<Object, Object>) expected);
            final java.util.List<LeafNode<Object, Object>> a = leaves((LeafNode<Object, Object>) actual);
            assertThat(a).hasSameSizeAs(e);
            for (int i = 0; i < e.size(); i++) {
                assertThat(a.get(i).hash()).isEqualTo(e.get(i).hash());
                assertThat(a.get(i).key()).isSameAs(e.get(i).key());
                assertThat(a.get(i).value()).isSameAs(e.get(i).value());
            }
        } else {
            assertThat(actual).isSameAs(EmptyNode.instance());
        }
    }

    private static <K, V> java.util.List<LeafNode<K, V>> leaves(LeafNode<K, V> leaf) {
        final java.util.List<LeafNode<K, V>> result = new ArrayList<>();
        leaf.nodes().forEach(result::add);
        return result;
    }

    /** Asserts the invariants of the trie: bitmap and child count agree, the sizes and counts are the recomputed
     *  ones, an indexed node has at most 16 children, every leaf sits under the hash fragments of its path, and a
     *  collision list holds distinct keys of one hash. Returns the number of entries. */
    private static int assertValid(Object node, int shift, int path, int pathMask) {
        if (node instanceof IndexedNode<?, ?> n) {
            assertThat(Integer.bitCount(n.bitmap)).isEqualTo(n.subNodes.length);
            assertThat(n.subNodes.length).isBetween(1, AbstractNode.MAX_INDEX_NODE);
            int size = 0;
            int bits = n.bitmap;
            for (Object child : n.subNodes) {
                final int fragment = Integer.numberOfTrailingZeros(bits);
                bits &= bits - 1;
                assertThat(((AbstractNode<?, ?>) child).isEmpty()).isFalse();
                size += assertValid(child, shift + AbstractNode.SIZE, path | (fragment << shift), pathMask | (31 << shift));
            }
            assertThat(n.size).isEqualTo(size);
            return size;
        } else if (node instanceof ArrayNode<?, ?> n) {
            assertThat(n.subNodes.length).isEqualTo(AbstractNode.BUCKET_SIZE);
            int size = 0;
            int count = 0;
            for (int fragment = 0; fragment < AbstractNode.BUCKET_SIZE; fragment++) {
                final AbstractNode<?, ?> child = (AbstractNode<?, ?>) n.subNodes[fragment];
                if (!child.isEmpty()) {
                    count++;
                    size += assertValid(child, shift + AbstractNode.SIZE, path | (fragment << shift), pathMask | (31 << shift));
                }
            }
            assertThat(n.count).isEqualTo(count);
            assertThat(n.size).isEqualTo(size);
            return size;
        } else if (node instanceof LeafNode<?, ?> leaf) {
            final java.util.List<? extends LeafNode<?, ?>> all = leaves(leaf);
            final Set<Object> keys = new java.util.HashSet<>();
            for (LeafNode<?, ?> l : all) {
                assertThat(l.hash()).isEqualTo(leaf.hash());
                assertThat(l.hash()).isEqualTo(java.util.Objects.hashCode(l.key()));
                assertThat(keys.add(l.key())).as("distinct keys in a collision list").isTrue();
            }
            assertThat(leaf.hash() & pathMask).as("leaf under its hash fragments").isEqualTo(path);
            assertThat(leaf.size()).isEqualTo(all.size());
            assertThat(leaf instanceof LeafList).isEqualTo(all.size() > 1);
            return all.size();
        } else {
            assertThat(node).isSameAs(EmptyNode.instance());
            return 0;
        }
    }

    private static void assertValid(HashArrayMappedTrie<?, ?> trie) {
        assertThat(assertValid(trie, 0, 0, 0)).isEqualTo(trie.size());
    }

    // the internal nodes of a trie, by identity
    private static Set<Object> internalNodes(Object node) {
        final Set<Object> result = Collections.newSetFromMap(new IdentityHashMap<>());
        collectInternalNodes(node, result);
        return result;
    }

    private static void collectInternalNodes(Object node, Set<Object> result) {
        if (node instanceof IndexedNode<?, ?> n) {
            result.add(n);
            for (Object child : n.subNodes) {
                collectInternalNodes(child, result);
            }
        } else if (node instanceof ArrayNode<?, ?> n) {
            result.add(n);
            for (Object child : n.subNodes) {
                collectInternalNodes(child, result);
            }
        }
    }

    private static Object owner(Object node) {
        return (node instanceof IndexedNode<?, ?> n) ? n.owner : ((ArrayNode<?, ?>) node).owner;
    }

    /** A description of every node of a trie, by identity, with its fields and children: equal descriptions taken
     *  before and after an operation mean that no node of the trie was changed. */
    private static String describe(Object node) {
        final StringBuilder out = new StringBuilder();
        describe(node, out);
        return out.toString();
    }

    private static void describe(Object node, StringBuilder out) {
        out.append(node.getClass().getSimpleName()).append('@').append(System.identityHashCode(node));
        if (node instanceof IndexedNode<?, ?> n) {
            out.append("[b=").append(n.bitmap).append(",s=").append(n.size).append(",a@")
               .append(System.identityHashCode(n.subNodes)).append('(');
            for (Object child : n.subNodes) {
                describe(child, out);
                out.append(' ');
            }
            out.append(")]");
        } else if (node instanceof ArrayNode<?, ?> n) {
            out.append("[c=").append(n.count).append(",s=").append(n.size).append('(');
            for (Object child : n.subNodes) {
                describe(child, out);
                out.append(' ');
            }
            out.append(")]");
        }
    }

    // -- the built trie is the persistent one

    @Test
    public void shouldBuildTheTrieOfSuccessivePutsAtEveryBoundary() {
        for (int size : SIZES) {
            final java.util.List<Integer> keys = ids(size);
            final java.util.List<String> values = keys.stream().map(i -> "v" + i).toList();
            final HashArrayMappedTrie<Integer, String> built = built(keys, values);
            assertValid(built);
            assertSameShape(persistent(keys, values), built);
        }
    }

    @Test
    public void shouldBuildTheTrieOfSuccessivePutsOnRandomKeysWithDuplicatesAndCollisions() {
        final Random random = new Random(SEED);
        for (int round = 0; round < 300; round++) {
            final int size = random.nextInt(3000);
            // a small hash range makes collision lists, a small id range makes duplicate keys
            final int hashRange = 1 + random.nextInt(round % 3 == 0 ? 50 : Integer.MAX_VALUE);
            final int idRange = 1 + random.nextInt(4);
            final java.util.List<Key> keys = new ArrayList<>(size);
            final java.util.List<Integer> values = new ArrayList<>(size);
            final java.util.HashMap<Key, Integer> oracle = new java.util.HashMap<>();
            for (int i = 0; i < size; i++) {
                final int hash = random.nextBoolean() ? random.nextInt(hashRange) : -random.nextInt(hashRange);
                final Key key = new Key(hash, random.nextInt(idRange));
                keys.add(key);
                values.add(i);
                oracle.put(key, i);
            }
            final HashArrayMappedTrie<Key, Integer> built = built(keys, values);
            assertValid(built);
            assertSameShape(persistent(keys, values), built);
            final java.util.Map<Key, Integer> actual = new java.util.HashMap<>();
            built.forEach(t -> actual.put(t._1(), t._2()));
            assertThat(actual).isEqualTo(oracle);
        }
    }

    @Test
    public void shouldExpandAnIndexedNodeIntoAnArrayNodeAtTheSeventeenthChild() {
        final java.util.List<Key> keys = new ArrayList<>();
        for (int fragment = 0; fragment < 17; fragment++) {
            keys.add(new Key(fragment, 0));
            final HashArrayMappedTrie<Key, Integer> built = built(keys, ids(keys.size()));
            assertValid(built);
            assertSameShape(persistent(keys, ids(keys.size())), built);
            if (keys.size() == 1) {
                assertThat(built).isInstanceOf(LeafSingleton.class);
            } else if (keys.size() <= 16) {
                assertThat(built).isInstanceOf(IndexedNode.class);
            } else {
                assertThat(built).isInstanceOf(ArrayNode.class);
            }
        }
        // and a put into an ArrayNode slot that already holds a leaf, and into an empty slot of it
        keys.add(new Key(32, 0));
        keys.add(new Key(20, 0));
        assertSameShape(persistent(keys, ids(keys.size())), built(keys, ids(keys.size())));
    }

    @Test
    public void shouldReachTheDeepestShift() {
        // hashes that differ only in bits 30 and 31: the fragments of shifts 0 to 25 are all 0, that of shift 30 differs
        final java.util.List<Key> keys = java.util.List.of(new Key(0, 0), new Key(1 << 30, 0), new Key(2 << 30, 0),
                new Key(3 << 30, 0), new Key(3 << 30, 1), new Key(0, 1), new Key(0, 0));
        final java.util.List<Integer> values = ids(keys.size());
        final HashArrayMappedTrie<Key, Integer> built = built(keys, values);
        assertValid(built);
        assertSameShape(persistent(keys, values), built);
        assertThat(built.size()).isEqualTo(6);
    }

    @Test
    public void shouldUpdateInsideACollisionListAndReplaceAValueWithoutChangingTheSize() {
        final java.util.List<Key> keys = java.util.List.of(new Key(-7, 0), new Key(-7, 1), new Key(-7, 2), new Key(5, 0),
                new Key(-7, 1), new Key(5, 0), new Key(-7, 0), new Key(-7, 2));
        final java.util.List<String> values = java.util.List.of("a", "b", "c", "d", "e", "f", "g", "h");
        final HashArrayMappedTrie<Key, String> built = built(keys, values);
        assertValid(built);
        assertSameShape(persistent(keys, values), built);
        assertThat(built.size()).isEqualTo(4);
        assertThat(built.getOrElse(new Key(-7, 0), "?")).isEqualTo("g");
        assertThat(built.getOrElse(new Key(-7, 1), "?")).isEqualTo("e");
        assertThat(built.getOrElse(new Key(-7, 2), "?")).isEqualTo("h");
        assertThat(built.getOrElse(new Key(5, 0), "?")).isEqualTo("f");
    }

    @Test
    public void shouldKeepTheLastKeyObjectOfEqualKeys() {
        final String first = new String("k");
        final String last = new String("k");
        final HashArrayMappedTrie<String, Integer> built = built(java.util.List.of(first, "x", last), java.util.List.of(1, 2, 3));
        assertThat(built.getEntry("k").get()._1()).isSameAs(last);
        assertThat(built.getOrElse("k", 0)).isEqualTo(3);
    }

    // -- ownership

    @Test
    public void shouldOwnEveryInternalNodeOfATrieBuiltFromNothing() {
        for (int size : SIZES) {
            final HashArrayMappedTrie<Integer, Integer> built = built(ids(size), ids(size));
            final Set<Object> nodes = internalNodes(built);
            final Set<Object> owners = Collections.newSetFromMap(new IdentityHashMap<>());
            nodes.forEach(node -> owners.add(owner(node)));
            if (nodes.isEmpty()) {
                assertThat(size).isLessThanOrEqualTo(1);
            } else {
                assertThat(owners).hasSize(1);
                assertThat(owners.iterator().next()).isNotNull();
            }
        }
        // a persistent put never owns a node
        final HashArrayMappedTrie<Integer, Integer> persistent = persistent(ids(1025), ids(1025));
        internalNodes(persistent).forEach(node -> assertThat(owner(node)).isNull());
    }

    @Test
    public void shouldNeverChangeTheReturnedTrie() {
        final java.util.List<Integer> keys = ids(1025);
        final HashArrayMappedTrie<Integer, Integer> built = built(keys, keys);
        final String before = describe(built);
        HashArrayMappedTrie<Integer, Integer> updated = built;
        for (int i = 0; i < 2000; i += 3) {
            updated = updated.put(i, -i).remove(i + 1);
        }
        assertThat(describe(built)).isEqualTo(before);
        assertSameShape(persistent(keys, keys), built);
        // the persistent operations on the built trie create nodes of their own
        final Set<Object> builtNodes = internalNodes(built);
        for (Object node : internalNodes(updated)) {
            if (!builtNodes.contains(node)) {
                assertThat(owner(node)).isNull();
            }
        }
    }

    @Test
    public void shouldAdoptATrieOnAnEmptyBuilderAndCopyItsNodesOnFirstWrite() {
        final Random random = new Random(SEED);
        for (int size : new int[] { 1, 2, 31, 32, 33, 1023, 1024, 1025, 32768 }) {
            final java.util.List<Integer> keys = ids(size);
            final HashArrayMappedTrie<Integer, Integer> source = persistent(keys, keys);
            final String sourceBefore = describe(source);
            final Set<Object> sourceNodes = internalNodes(source);

            final HashArrayMappedTrieBuilder<Integer, Integer> builder = new HashArrayMappedTrieBuilder<>("test");
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
            final HashArrayMappedTrie<Integer, Integer> built = builder.result();

            assertThat(describe(source)).as("the adopted trie is unchanged").isEqualTo(sourceBefore);
            assertValid(built);
            assertSameShape(persistent(moreKeys, moreValues), built);
            // every node is either one of the source, untouched, or a copy owned by the builder
            final Set<Object> owners = Collections.newSetFromMap(new IdentityHashMap<>());
            int shared = 0;
            for (Object node : internalNodes(built)) {
                if (sourceNodes.contains(node)) {
                    shared++;
                    assertThat(owner(node)).isNull();
                } else {
                    owners.add(owner(node));
                }
            }
            assertThat(owners).doesNotContainNull().hasSizeLessThanOrEqualTo(1);
            if (size >= 1024) {
                assertThat(shared).as("untouched nodes of the source are shared").isPositive();
            }
        }
    }

    @Test
    public void shouldBuildIndependentTriesFromOneAdoptedTrie() {
        final java.util.List<Integer> keys = ids(1025);
        final HashArrayMappedTrie<Integer, Integer> source = persistent(keys, keys);
        final String sourceBefore = describe(source);
        final HashArrayMappedTrieBuilder<Integer, Integer> left = new HashArrayMappedTrieBuilder<>("test");
        final HashArrayMappedTrieBuilder<Integer, Integer> right = new HashArrayMappedTrieBuilder<>("test");
        left.putAll(source);
        right.putAll(source);
        for (int i = 0; i < 1025; i += 2) {
            left.put(i, -1);
            if (i + 1 < 1025) {
                right.put(i + 1, -2);
            }
        }
        left.put(5000, 5000);
        final HashArrayMappedTrie<Integer, Integer> l = left.result();
        final HashArrayMappedTrie<Integer, Integer> r = right.result();
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
        final HashArrayMappedTrie<Integer, Integer> source = persistent(second, second);
        final String sourceBefore = describe(source);
        final HashArrayMappedTrieBuilder<Integer, Integer> builder = new HashArrayMappedTrieBuilder<>("test");
        for (Integer key : first) {
            builder.put(key, -key);
        }
        builder.putAll(source);
        builder.putAll(HashArrayMappedTrie.empty());
        final HashArrayMappedTrie<Integer, Integer> built = builder.result();
        assertThat(describe(source)).isEqualTo(sourceBefore);
        assertValid(built);
        // the entries of the argument win, as later puts do
        HashArrayMappedTrie<Integer, Integer> expected = persistent(first, first.stream().map(i -> -i).toList());
        for (Tuple2Like entry : entries(source)) {
            expected = expected.put(entry.key, entry.value);
        }
        assertSameShape(expected, built);
        internalNodes(built).forEach(node -> assertThat(owner(node)).isNotNull());
    }

    private record Tuple2Like(Integer key, Integer value) {
    }

    private static java.util.List<Tuple2Like> entries(HashArrayMappedTrie<Integer, Integer> trie) {
        final java.util.List<Tuple2Like> result = new ArrayList<>();
        trie.forEach(t -> result.add(new Tuple2Like(t._1(), t._2())));
        return result;
    }

    @Test
    public void shouldRefuseAnyUseAfterResult() {
        final HashArrayMappedTrieBuilder<Integer, Integer> builder = new HashArrayMappedTrieBuilder<>("Some.Builder");
        builder.put(1, 1);
        final HashArrayMappedTrie<Integer, Integer> built = builder.result();
        assertThatThrownBy(builder::checkOpen).isInstanceOf(IllegalStateException.class)
                .hasMessage("result() has already been called on this Some.Builder");
        assertThatThrownBy(builder::size).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(builder::result).isInstanceOf(IllegalStateException.class);
        assertThat(built.size()).isEqualTo(1);
    }

    @Test
    public void shouldBuildTheEmptyTrie() {
        assertThat(new HashArrayMappedTrieBuilder<Integer, Integer>("test").result()).isSameAs(EmptyNode.instance());
    }
}
