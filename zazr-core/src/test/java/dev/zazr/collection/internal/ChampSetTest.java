package dev.zazr.collection.internal;

import dev.zazr.collection.Vector;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static dev.zazr.collection.internal.ChampValidity.assertSameShape;
import static dev.zazr.collection.internal.ChampValidity.assertValid;
import static dev.zazr.collection.internal.ChampValidity.describe;
import static dev.zazr.collection.internal.ChampValidity.internalNodes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// The CHAMP trie of `HashSet`: lookups, persistent updates in canonical form, iteration, and the transient builder.
public class ChampSetTest {

    private static final long SEED = 20260926L;
    private static final int[] SIZES = {0, 1, 2, 31, 32, 33, 1023, 1024, 1025, 32768, 100_000};

    /** A key placed by the test: `hash` is its mixed hash (the one whose 5-bit fragments pick its slots), and its hash
     *  code the one that mixes to it. Two keys are equal when both the hash and the id are. */
    record Key(int hash, int id) {
        @Override
        public int hashCode() {
            return ChampValidity.hashCodeFor(hash);
        }
    }

    // keys of mixed hashes 0 to size - 1: the first 32 fill the root, the first 1024 two levels
    private static java.util.List<Key> keys(int size) {
        java.util.List<Key> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            result.add(new Key(i, 0));
        }
        return result;
    }

    private static final int[] HOT_HASHES = {
        0,
        1,
        2,
        31,
        32,
        33,
        1023,
        1024,
        1025,
        -1,
        Integer.MIN_VALUE,
        Integer.MAX_VALUE,
        1 << 30,
        2 << 30,
        3 << 30,
        1 << 25,
        1 << 20,
        (1 << 25) | 1,
        (1 << 30) | 1
    };

    private static Key randomKey(Random random) {
        int hash = switch (random.nextInt(4)) {
            case 0 -> HOT_HASHES[random.nextInt(HOT_HASHES.length)];
            case 1 -> random.nextInt(64);
            case 2 -> random.nextInt(64) << (5 * (1 + random.nextInt(5)));
            default -> random.nextInt();
        };
        return new Key(hash, random.nextInt(3));
    }

    // successive persistent additions, each keeping an equal element already there, as the builder does
    private static <T> BitmapIndexedSetNode<T> persistent(java.util.List<T> elements) {
        return Vector.ofAll(elements).foldLeft(SetNode.<T>empty(), (trie, element) -> trie.updated(element, false));
    }

    private static <T> BitmapIndexedSetNode<T> built(java.util.List<T> elements) {
        HashSetBuilder<T> builder = new HashSetBuilder<>("test");
        for (T element : elements) {
            builder.checkOpen();
            builder.add(element);
        }
        assertThat(builder.size()).isEqualTo(persistent(elements).size());
        return builder.result();
    }

    private static java.util.List<Integer> ids(int size) {
        java.util.List<Integer> values = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            values.add(i);
        }
        return values;
    }

    private static <T> java.util.List<T> elements(SetNode<T> trie) {
        java.util.List<T> result = new ArrayList<>();
        trie.iterator().forEachRemaining(result::add);
        return result;
    }

    // asserts that the trie holds exactly the elements of the model, by the identity of the kept element objects
    private static void assertHolds(BitmapIndexedSetNode<Key> trie, java.util.Map<Key, Key> model) {
        assertThat(trie.size()).isEqualTo(model.size());
        java.util.Map<Key, Key> actual = new java.util.HashMap<>();
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
        java.util.List<Key> all = elements(trie);
        Collections.reverse(all);
        assertSameShape(persistent(all), trie, false);
    }

    // -- lookups and shape

    @Test
    public void shouldFindPresentElementsAndMissAbsentOnes() {
        BitmapIndexedSetNode<Integer> empty = SetNode.empty();
        assertThat(empty.contains(2)).isFalse();
        BitmapIndexedSetNode<Integer> trie =
                empty.updated(1, true).updated(4, true).updated(33, true);
        assertThat(trie.contains(1)).isTrue();
        assertThat(trie.contains(4)).isTrue();
        assertThat(trie.contains(33)).isTrue();
        assertThat(trie.contains(2)).isFalse();
        assertThat(trie.contains(65)).isFalse();
        assertThat(trie.contains(null)).isFalse();
        BitmapIndexedSetNode<Key> colliding =
                SetNode.<Key>empty().updated(new Key(7, 0), true).updated(new Key(7, 1), true);
        assertThat(colliding.contains(new Key(7, 1))).isTrue();
        assertThat(colliding.contains(new Key(7, 2))).isFalse();
        assertThat(colliding.contains(new Key(7 + (1 << 31), 0))).isFalse();
    }

    @Test
    public void shouldHoldUpTo32ElementsInlineInTheRootAndPushTheThirtyThirdDown() {
        for (int size : new int[] {0, 1, 2, 31, 32, 33}) {
            BitmapIndexedSetNode<Key> trie = persistent(keys(size));
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
        for (int size : new int[] {1023, 1024, 1025}) {
            BitmapIndexedSetNode<Key> trie = persistent(keys(size));
            assertValid(trie);
            assertThat(trie.nodeMap).isEqualTo(-1);
            assertThat(((BitmapIndexedSetNode<Key>) trie.getNode(0)).nodeMap).isEqualTo(size == 1025 ? 1 : 0);
        }
    }

    @Test
    public void shouldPutElementsOfOneHashInACollisionNodeAndPullTheLastOneBackUp() {
        BitmapIndexedSetNode<Key> trie = persistent(java.util.List.of(new Key(5, 0), new Key(5, 1), new Key(5, 2)));
        assertValid(trie);
        SetNode<Key> node = Vector.range(0, 7).foldLeft((SetNode<Key>) trie, (level, i) -> level.getNode(0));
        assertThat(node).isInstanceOf(HashCollisionSetNode.class);
        BitmapIndexedSetNode<Key> one = trie.removed(new Key(5, 0)).removed(new Key(5, 2));
        assertValid(one);
        assertThat(one.nodeMap).isZero();
        assertThat(one.dataMap).isEqualTo(1 << 5);
        // a root that keeps other elements inlines the survivor of a deep chain
        BitmapIndexedSetNode<Key> mixed =
                persistent(java.util.List.of(new Key(0, 0), new Key(1 << 30, 0), new Key(7, 0)));
        BitmapIndexedSetNode<Key> after = mixed.removed(new Key(1 << 30, 0));
        assertCanonical(after);
        assertThat(after.nodeMap).isZero();
    }

    // -- which of two equal elements is kept

    @Test
    public void shouldReplaceAnEqualElementOnlyWhenAsked() {
        for (boolean colliding : new boolean[] {false, true}) {
            Key first = new Key(3, 0);
            Key second = new Key(3, 0);
            BitmapIndexedSetNode<Key> empty = SetNode.empty();
            BitmapIndexedSetNode<Key> trie =
                    (colliding ? empty.updated(new Key(3, 1), true) : empty).updated(first, true);
            assertThat(trie.updated(first, true)).isSameAs(trie);
            assertThat(trie.updated(second, false)).isSameAs(trie);
            BitmapIndexedSetNode<Key> replaced = trie.updated(second, true);
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
            Random random = new Random(SEED + seed);
            java.util.Map<Key, Key> model = new java.util.HashMap<>();
            java.util.List<BitmapIndexedSetNode<Key>> versions = new ArrayList<>();
            java.util.List<java.util.Map<Key, Key>> versionModels = new ArrayList<>();
            int steps = random.nextInt(4) == 0 ? random.nextInt(60) : random.nextInt(2500);
            BitmapIndexedSetNode<Key> trie = Vector.range(0, steps).foldLeft(SetNode.empty(), (acc, step) -> {
                BitmapIndexedSetNode<Key> next = randomStep(random, acc, model);
                if (random.nextInt(50) == 0) {
                    versions.add(next);
                    versionModels.add(new java.util.HashMap<>(model));
                }
                return next;
            });
            assertCanonical(trie);
            assertHolds(trie, model);
            for (int i = 0; i < versions.size(); i++) {
                assertValid(versions.get(i));
                assertHolds(versions.get(i), versionModels.get(i));
            }
            java.util.List<Key> keys = new ArrayList<>(model.keySet());
            Collections.shuffle(keys, random);
            BitmapIndexedSetNode<Key> emptied = Vector.ofAll(keys).foldLeft(trie, (acc, key) -> {
                BitmapIndexedSetNode<Key> removed = acc.removed(new Key(key.hash(), key.id()));
                assertValid(removed);
                return removed;
            });
            assertThat(emptied.size()).isZero();
        }
    }

    // a random removal (a quarter of the steps), addition keeping an equal element (a quarter) or addition replacing
    // it (half) on `trie`, applied to `model` too
    private static BitmapIndexedSetNode<Key> randomStep(
            Random random, BitmapIndexedSetNode<Key> trie, java.util.Map<Key, Key> model) {
        Key key = randomKey(random);
        int op = random.nextInt(4);
        if (op == 0) {
            BitmapIndexedSetNode<Key> after = trie.removed(key);
            if (model.remove(key) == null) {
                assertThat(after).isSameAs(trie);
            }
            assertValid(after);
            if (random.nextInt(10) == 0) {
                assertCanonical(after);
            }
            return after;
        } else if (op == 1) {
            // an addition that keeps an equal element already there
            BitmapIndexedSetNode<Key> after = trie.updated(key, false);
            if (model.putIfAbsent(key, key) != null) {
                assertThat(after).isSameAs(trie);
            }
            return after;
        }
        model.put(key, key);
        return trie.updated(key, true);
    }

    // -- iteration

    @Test
    public void shouldIterateTheElementsOfANodeBeforeItsChildrenAndEachOnce() {
        assertThat(elements(persistent(java.util.List.of(new Key(0, 0), new Key(32, 0), new Key(1, 0), new Key(2, 0)))))
                .containsExactly(new Key(1, 0), new Key(2, 0), new Key(0, 0), new Key(32, 0));
        for (int size : SIZES) {
            java.util.Iterator<Integer> it = persistent(ids(size)).iterator();
            java.util.Set<Integer> seen = new java.util.HashSet<>();
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
            java.util.List<Key> placed = keys(size);
            assertSameShape(persistent(placed), built(placed), true);
            java.util.List<Integer> elements = ids(size);
            BitmapIndexedSetNode<Integer> built = built(elements);
            assertValid(built);
            assertSameShape(persistent(elements), built, true);
        }
        Random random = new Random(SEED);
        for (int round = 0; round < 150; round++) {
            int size = random.nextInt(3000);
            int hashRange = 1 + random.nextInt(round % 3 == 0 ? 50 : Integer.MAX_VALUE);
            int idRange = 1 + random.nextInt(4);
            java.util.List<Key> elements = new ArrayList<>(size);
            java.util.Map<Key, Key> model = new java.util.HashMap<>();
            for (int i = 0; i < size; i++) {
                int hash = random.nextBoolean() ? random.nextInt(hashRange) : -random.nextInt(hashRange);
                Key key = new Key(hash, random.nextInt(idRange));
                elements.add(key);
                model.putIfAbsent(key, key);
            }
            BitmapIndexedSetNode<Key> built = built(elements);
            assertValid(built);
            assertSameShape(persistent(elements), built, true);
            // the first of equal elements is kept
            assertHolds(built, model);
        }
    }

    @Test
    public void shouldOwnEveryNodeItCreatesAndNeverChangeTheReturnedTrie() {
        for (int size : SIZES) {
            BitmapIndexedSetNode<Integer> built = built(ids(size));
            Set<Object> owners = Collections.newSetFromMap(new IdentityHashMap<>());
            internalNodes(built).forEach(node -> owners.add(((BitmapIndexedSetNode<?>) node).owner));
            if (size == 0) {
                assertThat(built).isSameAs(SetNode.empty());
            } else {
                assertThat(owners).hasSize(1).doesNotContainNull();
            }
        }
        internalNodes(persistent(ids(1025)))
                .forEach(node ->
                        assertThat(((BitmapIndexedSetNode<?>) node).owner).isNull());
        BitmapIndexedSetNode<Integer> built = built(ids(1025));
        String before = describe(built);
        BitmapIndexedSetNode<Integer> updated = Vector.rangeBy(0, 2000, 3)
                .foldLeft(built, (acc, i) -> acc.updated(i + 5000, true).removed(i + 1));
        assertThat(describe(built)).isEqualTo(before);
    }

    @Test
    public void shouldAdoptATrieAndCopyItsNodesOnFirstWrite() {
        Random random = new Random(SEED);
        for (int size : new int[] {1, 2, 31, 32, 33, 1023, 1024, 1025, 32768}) {
            java.util.List<Integer> elements = ids(size);
            BitmapIndexedSetNode<Integer> source = persistent(elements);
            String sourceBefore = describe(source);
            Set<Object> sourceNodes = internalNodes(source);
            HashSetBuilder<Integer> builder = new HashSetBuilder<>("test");
            builder.addAll(source);
            java.util.List<Integer> more = new ArrayList<>(elements);
            for (int i = 0; i < 50; i++) {
                Integer element = random.nextInt(size * 2 + 1);
                builder.add(element);
                more.add(element);
            }
            BitmapIndexedSetNode<Integer> built = builder.result();
            assertThat(describe(source)).isEqualTo(sourceBefore);
            assertValid(built);
            assertSameShape(persistent(more), built, true);
            for (Object node : internalNodes(built)) {
                if (!sourceNodes.contains(node)) {
                    assertThat(((BitmapIndexedSetNode<?>) node).owner).isNotNull();
                }
            }
            long shared =
                    internalNodes(built).stream().filter(sourceNodes::contains).count();
            if (size >= 1024) {
                assertThat(shared).isPositive();
            }
        }
        // nothing added after the adoption: the adopted trie itself
        BitmapIndexedSetNode<Integer> source = persistent(ids(100));
        HashSetBuilder<Integer> builder = new HashSetBuilder<>("test");
        builder.addAll(source);
        builder.addAll(SetNode.empty());
        assertThat(builder.result()).isSameAs(source);
    }

    @Test
    public void shouldNeverChangeAnAdoptedTrieBuiltByPersistentOperations() {
        for (long seed = 1; seed <= 12; seed++) {
            Random random = new Random(SEED + seed);
            java.util.List<BitmapIndexedSetNode<Key>> pool = new ArrayList<>();
            java.util.List<java.util.Map<Key, Key>> poolContents = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                java.util.Map<Key, Key> model = new java.util.HashMap<>();
                int size = random.nextInt(4) == 0 ? random.nextInt(40) : random.nextInt(1500);
                BitmapIndexedSetNode<Key> added = Vector.range(0, size).foldLeft(SetNode.<Key>empty(), (acc, j) -> {
                    Key key = randomKey(random);
                    model.put(key, key);
                    return acc.updated(key, true);
                });
                pool.add(removeSome(random, added, model));
                poolContents.add(model);
            }
            for (int step = 0; step < 25; step++) {
                java.util.List<String> snapshots =
                        pool.stream().map(ChampValidity::describe).toList();
                int from = random.nextInt(pool.size());
                HashSetBuilder<Key> left = new HashSetBuilder<>("test");
                HashSetBuilder<Key> right = new HashSetBuilder<>("test");
                left.addAll(pool.get(from));
                right.addAll(pool.get(from));
                java.util.Map<Key, Key> leftModel = new java.util.HashMap<>(poolContents.get(from));
                java.util.Map<Key, Key> rightModel = new java.util.HashMap<>(poolContents.get(from));
                java.util.List<Key> sourceKeys = new ArrayList<>(leftModel.keySet());
                int adds = random.nextInt(4) == 0 ? random.nextInt(5) : random.nextInt(600);
                for (int i = 0; i < adds; i++) {
                    Key key = (!sourceKeys.isEmpty() && random.nextBoolean())
                            ? sourceKeys.get(random.nextInt(sourceKeys.size()))
                            : randomKey(random);
                    Key equal = new Key(key.hash(), key.id());
                    left.add(equal);
                    leftModel.putIfAbsent(equal, equal);
                    Key other = randomKey(random);
                    right.add(other);
                    rightModel.putIfAbsent(other, other);
                }
                if (random.nextInt(3) == 0) {
                    int second = random.nextInt(pool.size());
                    left.addAll(pool.get(second));
                    poolContents.get(second).forEach(leftModel::putIfAbsent);
                }
                BitmapIndexedSetNode<Key> l = left.result();
                BitmapIndexedSetNode<Key> r = right.result();
                for (int i = 0; i < pool.size(); i++) {
                    assertThat(describe(pool.get(i)))
                            .as("seed %s, step %s, trie %s", seed, step, i)
                            .isEqualTo(snapshots.get(i));
                    assertHolds(pool.get(i), poolContents.get(i));
                }
                assertValid(l);
                assertValid(r);
                assertHolds(l, leftModel);
                assertHolds(r, rightModel);
                java.util.Map<Key, Key> derivedModel = new java.util.HashMap<>(leftModel);
                BitmapIndexedSetNode<Key> derived = Vector.range(0, 20)
                        .foldLeft(removeSome(random, l, derivedModel), (acc, i) -> {
                            Key key = randomKey(random);
                            derivedModel.put(key, key);
                            return acc.updated(key, true);
                        });

                pool.add(l);
                poolContents.add(leftModel);
                pool.add(r);
                poolContents.add(rightModel);
                pool.add(derived);
                poolContents.add(derivedModel);
                while (pool.size() > 8) {
                    int drop = random.nextInt(pool.size());
                    pool.remove(drop);
                    poolContents.remove(drop);
                }
            }
        }
    }

    @Test
    public void shouldRefuseAnyUseAfterResultAndBuildTheEmptyTrie() {
        HashSetBuilder<Integer> builder = new HashSetBuilder<>("Some.Builder");
        builder.add(1);
        BitmapIndexedSetNode<Integer> built = builder.result();
        assertThatThrownBy(builder::checkOpen)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("result() has already been called on this Some.Builder");
        assertThatThrownBy(builder::size).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(builder::result).isInstanceOf(IllegalStateException.class);
        assertThat(built.size()).isEqualTo(1);
        assertThat(new HashSetBuilder<Integer>("test").result()).isSameAs(SetNode.empty());
    }

    // `trie` without a third of the elements of `model`, chosen by `random`, removed from `model` too
    private static BitmapIndexedSetNode<Key> removeSome(
            Random random, BitmapIndexedSetNode<Key> trie, java.util.Map<Key, Key> model) {
        return Vector.ofAll(new ArrayList<>(model.keySet())).foldLeft(trie, (acc, key) -> {
            if (random.nextInt(3) != 0) {
                return acc;
            }
            model.remove(key);
            return acc.removed(key);
        });
    }
}
