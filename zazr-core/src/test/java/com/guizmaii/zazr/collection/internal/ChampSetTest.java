package com.guizmaii.zazr.collection.internal;

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
import static com.guizmaii.zazr.collection.internal.ChampValidity.internalNodes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// The CHAMP trie of `HashSet`: lookups, persistent updates in canonical form, iteration, and the transient builder.
public class ChampSetTest {

    private static final long SEED = 20260926L;
    private static final int[] SIZES = { 0, 1, 2, 31, 32, 33, 1023, 1024, 1025, 32768, 100_000 };

    /** An element whose hash code is chosen by the test; two elements are equal when both the hash and the id are. */
    record Key(int hash, int id) {
        @Override
        public int hashCode() {
            return hash;
        }
    }

    private static final int[] HOT_HASHES = { 0, 1, 2, 31, 32, 33, 1023, 1024, 1025, -1, Integer.MIN_VALUE, Integer.MAX_VALUE,
            1 << 30, 2 << 30, 3 << 30, 1 << 25, 1 << 20, (1 << 25) | 1, (1 << 30) | 1 };

    private static Key randomKey(Random random) {
        final int hash = switch (random.nextInt(4)) {
            case 0 -> HOT_HASHES[random.nextInt(HOT_HASHES.length)];
            case 1 -> random.nextInt(64);
            case 2 -> random.nextInt(64) << (5 * (1 + random.nextInt(5)));
            default -> random.nextInt();
        };
        return new Key(hash, random.nextInt(3));
    }

    private static <T> BitmapIndexedSetNode<T> persistent(java.util.List<T> elements) {
        BitmapIndexedSetNode<T> trie = SetNode.empty();
        for (T element : elements) {
            trie = trie.updated(element, true);
        }
        return trie;
    }

    private static <T> BitmapIndexedSetNode<T> built(java.util.List<T> elements) {
        final HashSetBuilder<T> builder = new HashSetBuilder<>("test");
        for (T element : elements) {
            builder.checkOpen();
            builder.add(element);
        }
        assertThat(builder.size()).isEqualTo(persistent(elements).size());
        return builder.result();
    }

    private static java.util.List<Integer> ids(int size) {
        final java.util.List<Integer> values = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            values.add(i);
        }
        return values;
    }

    private static <T> java.util.List<T> elements(SetNode<T> trie) {
        final java.util.List<T> result = new ArrayList<>();
        trie.iterator().forEachRemaining(result::add);
        return result;
    }

    // asserts that the trie holds exactly the elements of the model, by the identity of the kept element objects
    private static void assertHolds(BitmapIndexedSetNode<Key> trie, java.util.Map<Key, Key> model) {
        assertThat(trie.size()).isEqualTo(model.size());
        final java.util.Map<Key, Key> actual = new java.util.HashMap<>();
        for (Key element : elements(trie)) {
            assertThat(actual.put(element, element)).as("each element once").isNull();
        }
        assertThat(actual.keySet()).isEqualTo(model.keySet());
        model.forEach((key, kept) -> {
            assertThat(actual.get(key)).isSameAs(kept);
            assertThat(trie.contains(new Key(key.hash(), key.id()))).isTrue();
        });
    }

    private static void assertCanonical(BitmapIndexedSetNode<Key> trie) {
        assertValid(trie);
        final java.util.List<Key> all = elements(trie);
        Collections.reverse(all);
        assertSameShape(persistent(all), trie, false);
    }

    // -- lookups and shape

    @Test
    public void shouldFindPresentElementsAndMissAbsentOnes() {
        BitmapIndexedSetNode<Integer> trie = SetNode.empty();
        assertThat(trie.contains(2)).isFalse();
        trie = trie.updated(1, true).updated(4, true).updated(33, true);
        assertThat(trie.contains(1)).isTrue();
        assertThat(trie.contains(4)).isTrue();
        assertThat(trie.contains(33)).isTrue();
        assertThat(trie.contains(2)).isFalse();
        assertThat(trie.contains(65)).isFalse();
        assertThat(trie.contains(null)).isFalse();
        final BitmapIndexedSetNode<Key> colliding = SetNode.<Key> empty().updated(new Key(7, 0), true).updated(new Key(7, 1), true);
        assertThat(colliding.contains(new Key(7, 1))).isTrue();
        assertThat(colliding.contains(new Key(7, 2))).isFalse();
        assertThat(colliding.contains(new Key(7 + (1 << 31), 0))).isFalse();
    }

    @Test
    public void shouldHoldUpTo32ElementsInlineInTheRootAndPushTheThirtyThirdDown() {
        for (int size : new int[] { 0, 1, 2, 31, 32, 33 }) {
            final BitmapIndexedSetNode<Integer> trie = persistent(ids(size));
            assertValid(trie);
            if (size <= 32) {
                assertThat(trie.nodeMap).isZero();
                assertThat(Integer.bitCount(trie.dataMap)).isEqualTo(size);
            } else {
                assertThat(trie.nodeMap).isEqualTo(1);
                assertThat(Integer.bitCount(trie.dataMap)).isEqualTo(31);
                assertThat(trie.getNode(0).size()).isEqualTo(2);
            }
        }
        for (int size : new int[] { 1023, 1024, 1025 }) {
            final BitmapIndexedSetNode<Integer> trie = persistent(ids(size));
            assertValid(trie);
            assertThat(trie.nodeMap).isEqualTo(-1);
            assertThat(((BitmapIndexedSetNode<Integer>) trie.getNode(0)).nodeMap).isEqualTo(size == 1025 ? 1 : 0);
        }
    }

    @Test
    public void shouldPutElementsOfOneHashInACollisionNodeAndPullTheLastOneBackUp() {
        final BitmapIndexedSetNode<Key> trie = persistent(java.util.List.of(new Key(5, 0), new Key(5, 1), new Key(5, 2)));
        assertValid(trie);
        SetNode<Key> node = trie;
        for (int level = 0; level < 7; level++) {
            node = node.getNode(0);
        }
        assertThat(node).isInstanceOf(HashCollisionSetNode.class);
        final BitmapIndexedSetNode<Key> one = trie.removed(new Key(5, 0)).removed(new Key(5, 2));
        assertValid(one);
        assertThat(one.nodeMap).isZero();
        assertThat(one.dataMap).isEqualTo(1 << 5);
        // a root that keeps other elements inlines the survivor of a deep chain
        final BitmapIndexedSetNode<Key> mixed = persistent(java.util.List.of(new Key(0, 0), new Key(1 << 30, 0), new Key(7, 0)));
        final BitmapIndexedSetNode<Key> after = mixed.removed(new Key(1 << 30, 0));
        assertCanonical(after);
        assertThat(after.nodeMap).isZero();
    }

    // -- which of two equal elements is kept

    @Test
    public void shouldReplaceAnEqualElementOnlyWhenAsked() {
        for (boolean colliding : new boolean[] { false, true }) {
            final Key first = new Key(3, 0);
            final Key second = new Key(3, 0);
            BitmapIndexedSetNode<Key> trie = SetNode.empty();
            if (colliding) {
                trie = trie.updated(new Key(3, 1), true);
            }
            trie = trie.updated(first, true);
            assertThat(trie.updated(first, true)).isSameAs(trie);
            assertThat(trie.updated(second, false)).isSameAs(trie);
            final BitmapIndexedSetNode<Key> replaced = trie.updated(second, true);
            assertValid(replaced);
            assertThat(elements(replaced)).anySatisfy(e -> assertThat(e).isSameAs(second));
            assertThat(elements(replaced)).noneSatisfy(e -> assertThat(e).isSameAs(first));
            assertThat(elements(trie)).anySatisfy(e -> assertThat(e).isSameAs(first));
            assertThat(trie.removed(new Key(3, 2))).isSameAs(trie);
        }
    }

    @Test
    public void shouldMatchAModelAndStayCanonicalOnRandomAdditionsAndRemovals() {
        for (long seed = 0; seed < 20; seed++) {
            final Random random = new Random(SEED + seed);
            final java.util.Map<Key, Key> model = new java.util.HashMap<>();
            final java.util.List<BitmapIndexedSetNode<Key>> versions = new ArrayList<>();
            final java.util.List<java.util.Map<Key, Key>> versionModels = new ArrayList<>();
            BitmapIndexedSetNode<Key> trie = SetNode.empty();
            final int steps = random.nextInt(4) == 0 ? random.nextInt(60) : random.nextInt(2500);
            for (int step = 0; step < steps; step++) {
                final Key key = randomKey(random);
                final int op = random.nextInt(4);
                if (op == 0) {
                    final BitmapIndexedSetNode<Key> after = trie.removed(key);
                    if (model.remove(key) == null) {
                        assertThat(after).isSameAs(trie);
                    }
                    trie = after;
                    assertValid(trie);
                    if (random.nextInt(10) == 0) {
                        assertCanonical(trie);
                    }
                } else if (op == 1) {
                    // an addition that keeps an equal element already there
                    final BitmapIndexedSetNode<Key> after = trie.updated(key, false);
                    if (model.putIfAbsent(key, key) != null) {
                        assertThat(after).isSameAs(trie);
                    }
                    trie = after;
                } else {
                    trie = trie.updated(key, true);
                    model.put(key, key);
                }
                if (random.nextInt(50) == 0) {
                    versions.add(trie);
                    versionModels.add(new java.util.HashMap<>(model));
                }
            }
            assertCanonical(trie);
            assertHolds(trie, model);
            for (int i = 0; i < versions.size(); i++) {
                assertValid(versions.get(i));
                assertHolds(versions.get(i), versionModels.get(i));
            }
            final java.util.List<Key> keys = new ArrayList<>(model.keySet());
            Collections.shuffle(keys, random);
            for (Key key : keys) {
                trie = trie.removed(new Key(key.hash(), key.id()));
                assertValid(trie);
            }
            assertThat(trie.size()).isZero();
        }
    }

    // -- iteration

    @Test
    public void shouldIterateTheElementsOfANodeBeforeItsChildrenAndEachOnce() {
        assertThat(elements(persistent(java.util.List.of(0, 32, 1, 2)))).containsExactly(1, 2, 0, 32);
        for (int size : SIZES) {
            final java.util.Iterator<Integer> it = persistent(ids(size)).iterator();
            final java.util.Set<Integer> seen = new java.util.HashSet<>();
            while (it.hasNext()) {
                assertThat(seen.add(it.next())).isTrue();
            }
            assertThat(seen).hasSize(size);
            assertThatThrownBy(it::next).isInstanceOf(NoSuchElementException.class);
        }
    }

    // -- the builder

    @Test
    public void shouldBuildTheTrieOfSuccessiveAdditionsAtEveryBoundaryAndOnRandomElements() {
        for (int size : SIZES) {
            final java.util.List<Integer> elements = ids(size);
            final BitmapIndexedSetNode<Integer> built = built(elements);
            assertValid(built);
            assertSameShape(persistent(elements), built, true);
        }
        final Random random = new Random(SEED);
        for (int round = 0; round < 150; round++) {
            final int size = random.nextInt(3000);
            final int hashRange = 1 + random.nextInt(round % 3 == 0 ? 50 : Integer.MAX_VALUE);
            final int idRange = 1 + random.nextInt(4);
            final java.util.List<Key> elements = new ArrayList<>(size);
            final java.util.Map<Key, Key> model = new java.util.HashMap<>();
            for (int i = 0; i < size; i++) {
                final int hash = random.nextBoolean() ? random.nextInt(hashRange) : -random.nextInt(hashRange);
                final Key key = new Key(hash, random.nextInt(idRange));
                elements.add(key);
                model.put(key, key);
            }
            final BitmapIndexedSetNode<Key> built = built(elements);
            assertValid(built);
            assertSameShape(persistent(elements), built, true);
            // the last of equal elements is kept
            assertHolds(built, model);
        }
    }

    @Test
    public void shouldOwnEveryNodeItCreatesAndNeverChangeTheReturnedTrie() {
        for (int size : SIZES) {
            final BitmapIndexedSetNode<Integer> built = built(ids(size));
            final Set<Object> owners = Collections.newSetFromMap(new IdentityHashMap<>());
            internalNodes(built).forEach(node -> owners.add(((BitmapIndexedSetNode<?>) node).owner));
            if (size == 0) {
                assertThat(built).isSameAs(SetNode.empty());
            } else {
                assertThat(owners).hasSize(1).doesNotContainNull();
            }
        }
        internalNodes(persistent(ids(1025))).forEach(node -> assertThat(((BitmapIndexedSetNode<?>) node).owner).isNull());
        final BitmapIndexedSetNode<Integer> built = built(ids(1025));
        final String before = describe(built);
        BitmapIndexedSetNode<Integer> updated = built;
        for (int i = 0; i < 2000; i += 3) {
            updated = updated.updated(i + 5000, true).removed(i + 1);
        }
        assertThat(describe(built)).isEqualTo(before);
    }

    @Test
    public void shouldAdoptATrieAndCopyItsNodesOnFirstWrite() {
        final Random random = new Random(SEED);
        for (int size : new int[] { 1, 2, 31, 32, 33, 1023, 1024, 1025, 32768 }) {
            final java.util.List<Integer> elements = ids(size);
            final BitmapIndexedSetNode<Integer> source = persistent(elements);
            final String sourceBefore = describe(source);
            final Set<Object> sourceNodes = internalNodes(source);
            final HashSetBuilder<Integer> builder = new HashSetBuilder<>("test");
            builder.addAll(source);
            final java.util.List<Integer> more = new ArrayList<>(elements);
            for (int i = 0; i < 50; i++) {
                final Integer element = random.nextInt(size * 2 + 1);
                builder.add(element);
                more.add(element);
            }
            final BitmapIndexedSetNode<Integer> built = builder.result();
            assertThat(describe(source)).isEqualTo(sourceBefore);
            assertValid(built);
            assertSameShape(persistent(more), built, true);
            int shared = 0;
            for (Object node : internalNodes(built)) {
                if (sourceNodes.contains(node)) {
                    shared++;
                } else {
                    assertThat(((BitmapIndexedSetNode<?>) node).owner).isNotNull();
                }
            }
            if (size >= 1024) {
                assertThat(shared).isPositive();
            }
        }
        // nothing added after the adoption: the adopted trie itself
        final BitmapIndexedSetNode<Integer> source = persistent(ids(100));
        final HashSetBuilder<Integer> builder = new HashSetBuilder<>("test");
        builder.addAll(source);
        builder.addAll(SetNode.empty());
        assertThat(builder.result()).isSameAs(source);
    }

    @Test
    public void shouldNeverChangeAnAdoptedTrieBuiltByPersistentOperations() {
        for (long seed = 1; seed <= 12; seed++) {
            final Random random = new Random(SEED + seed);
            final java.util.List<BitmapIndexedSetNode<Key>> pool = new ArrayList<>();
            final java.util.List<java.util.Map<Key, Key>> poolContents = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                BitmapIndexedSetNode<Key> trie = SetNode.empty();
                final java.util.Map<Key, Key> model = new java.util.HashMap<>();
                final int size = random.nextInt(4) == 0 ? random.nextInt(40) : random.nextInt(1500);
                for (int j = 0; j < size; j++) {
                    final Key key = randomKey(random);
                    trie = trie.updated(key, true);
                    model.put(key, key);
                }
                for (Key key : new ArrayList<>(model.keySet())) {
                    if (random.nextInt(3) == 0) {
                        trie = trie.removed(key);
                        model.remove(key);
                    }
                }
                pool.add(trie);
                poolContents.add(model);
            }
            for (int step = 0; step < 25; step++) {
                final java.util.List<String> snapshots = pool.stream().map(ChampValidity::describe).toList();
                final int from = random.nextInt(pool.size());
                final HashSetBuilder<Key> left = new HashSetBuilder<>("test");
                final HashSetBuilder<Key> right = new HashSetBuilder<>("test");
                left.addAll(pool.get(from));
                right.addAll(pool.get(from));
                final java.util.Map<Key, Key> leftModel = new java.util.HashMap<>(poolContents.get(from));
                final java.util.Map<Key, Key> rightModel = new java.util.HashMap<>(poolContents.get(from));
                final java.util.List<Key> sourceKeys = new ArrayList<>(leftModel.keySet());
                final int adds = random.nextInt(4) == 0 ? random.nextInt(5) : random.nextInt(600);
                for (int i = 0; i < adds; i++) {
                    final Key key = (!sourceKeys.isEmpty() && random.nextBoolean())
                                    ? sourceKeys.get(random.nextInt(sourceKeys.size()))
                                    : randomKey(random);
                    final Key equal = new Key(key.hash(), key.id());
                    left.add(equal);
                    leftModel.put(equal, equal);
                    final Key other = randomKey(random);
                    right.add(other);
                    rightModel.put(other, other);
                }
                if (random.nextInt(3) == 0) {
                    final int second = random.nextInt(pool.size());
                    left.addAll(pool.get(second));
                    leftModel.putAll(poolContents.get(second));
                }
                final BitmapIndexedSetNode<Key> l = left.result();
                final BitmapIndexedSetNode<Key> r = right.result();
                for (int i = 0; i < pool.size(); i++) {
                    assertThat(describe(pool.get(i))).as("seed %s, step %s, trie %s", seed, step, i).isEqualTo(snapshots.get(i));
                    assertHolds(pool.get(i), poolContents.get(i));
                }
                assertValid(l);
                assertValid(r);
                assertHolds(l, leftModel);
                assertHolds(r, rightModel);
                BitmapIndexedSetNode<Key> derived = l;
                final java.util.Map<Key, Key> derivedModel = new java.util.HashMap<>(leftModel);
                for (Key key : new ArrayList<>(derivedModel.keySet())) {
                    if (random.nextInt(3) == 0) {
                        derived = derived.removed(key);
                        derivedModel.remove(key);
                    }
                }
                for (int i = 0; i < 20; i++) {
                    final Key key = randomKey(random);
                    derived = derived.updated(key, true);
                    derivedModel.put(key, key);
                }
                pool.add(l);
                poolContents.add(leftModel);
                pool.add(r);
                poolContents.add(rightModel);
                pool.add(derived);
                poolContents.add(derivedModel);
                while (pool.size() > 8) {
                    final int drop = random.nextInt(pool.size());
                    pool.remove(drop);
                    poolContents.remove(drop);
                }
            }
        }
    }

    @Test
    public void shouldRefuseAnyUseAfterResultAndBuildTheEmptyTrie() {
        final HashSetBuilder<Integer> builder = new HashSetBuilder<>("Some.Builder");
        builder.add(1);
        final BitmapIndexedSetNode<Integer> built = builder.result();
        assertThatThrownBy(builder::checkOpen).isInstanceOf(IllegalStateException.class)
                .hasMessage("result() has already been called on this Some.Builder");
        assertThatThrownBy(builder::size).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(builder::result).isInstanceOf(IllegalStateException.class);
        assertThat(built.size()).isEqualTo(1);
        assertThat(new HashSetBuilder<Integer>("test").result()).isSameAs(SetNode.empty());
    }
}
