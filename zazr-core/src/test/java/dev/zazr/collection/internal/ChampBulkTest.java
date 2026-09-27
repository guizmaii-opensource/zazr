package dev.zazr.collection.internal;

import dev.zazr.collection.Vector;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

import static dev.zazr.collection.internal.ChampValidity.assertSameShape;
import static dev.zazr.collection.internal.ChampValidity.assertValid;
import static dev.zazr.collection.internal.ChampValidity.describe;
import static dev.zazr.collection.internal.ChampValidity.entries;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// The operations on whole subtrees of the CHAMP tries: concat, filter, transform and equality of maps; concat,
/// filter, diff, subset and equality of sets. Each is checked against a model of the kept key and value objects, and
/// every result against the invariants and the canonical form.
public class ChampBulkTest {

    private static final long SEED = 20260927L;

    /** A key placed by the test: `hash` is its mixed hash (the one whose 5-bit fragments pick its slots), and its hash
     *  code the one that mixes to it. Two keys are equal when both the hash and the id are. */
    record Key(int hash, int id) {
        @Override
        public int hashCode() {
            return ChampValidity.hashCodeFor(hash);
        }
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

    private static Key randomKey(Random random, int range) {
        int hash = switch (random.nextInt(4)) {
            case 0 -> HOT_HASHES[random.nextInt(HOT_HASHES.length)];
            case 1 -> random.nextInt(64);
            case 2 -> random.nextInt(64) << (5 * (1 + random.nextInt(5)));
            default -> random.nextInt(range);
        };
        return new Key(hash, random.nextInt(3));
    }

    /** The kept key object and its value, the model of a map entry. */
    private record Kept(Object key, Object value) {}

    // a map trie of random keys, some shared with `pool` (as new, equal objects), and the model of what it keeps
    private static BitmapIndexedMapNode<Key, Object> randomMap(
            Random random, java.util.List<Key> pool, java.util.Map<Key, Kept> model) {
        int size = switch (random.nextInt(4)) {
            case 0 -> random.nextInt(3);
            case 1 -> random.nextInt(40);
            default -> random.nextInt(1500);
        };
        int range = random.nextBoolean() ? 100 : Integer.MAX_VALUE;
        return Vector.range(0, size).foldLeft(MapNode.<Key, Object>empty(), (trie, i) -> {
            Key key = sharedOrRandomKey(random, pool, range);
            Object value = new Object();
            model.put(key, new Kept(key, value));
            return trie.updated(key, value);
        });
    }

    // a new key equal to one of `pool` for a third of the calls, else a random key, added to `pool`
    private static Key sharedOrRandomKey(Random random, java.util.List<Key> pool, int range) {
        if (!pool.isEmpty() && random.nextInt(3) == 0) {
            Key shared = pool.get(random.nextInt(pool.size()));
            return new Key(shared.hash(), shared.id());
        }
        Key key = randomKey(random, range);
        pool.add(key);
        return key;
    }

    private static void assertHolds(MapNode<Key, ?> trie, java.util.Map<Key, Kept> model) {
        assertThat(trie.size()).isEqualTo(model.size());
        java.util.Map<Key, Kept> actual = new java.util.HashMap<>();
        for (Object[] entry : entries(trie)) {
            assertThat(actual.put((Key) entry[0], new Kept(entry[0], entry[1]))).isNull();
        }
        assertThat(actual.keySet()).isEqualTo(model.keySet());
        model.forEach((key, kept) -> {
            assertThat(actual.get(key).key()).isSameAs(kept.key());
            assertThat(actual.get(key).value()).isSameAs(kept.value());
        });
    }

    // valid, and the same trie as a fresh one of the same entries put in another order
    private static void assertCanonical(BitmapIndexedMapNode<Key, ?> trie) {
        assertValid(trie);
        java.util.List<Object[]> all = entries(trie);
        Collections.reverse(all);
        BitmapIndexedMapNode<Key, Object> fresh = Vector.ofAll(all)
                .foldLeft(MapNode.<Key, Object>empty(), (acc, entry) -> acc.updated((Key) entry[0], entry[1]));
        assertSameShape(fresh, trie, false);
    }

    // -- maps

    @Test
    public void shouldConcatTwoMapsKeepingTheEntriesOfTheRightSide() {
        Random random = new Random(SEED);
        for (int round = 0; round < 400; round++) {
            java.util.List<Key> pool = new ArrayList<>();
            java.util.Map<Key, Kept> leftModel = new java.util.HashMap<>();
            java.util.Map<Key, Kept> rightModel = new java.util.HashMap<>();
            BitmapIndexedMapNode<Key, Object> left = randomMap(random, pool, leftModel);
            BitmapIndexedMapNode<Key, Object> right =
                    random.nextInt(10) == 0 ? left : randomMap(random, pool, rightModel);
            if (right == left) {
                rightModel.putAll(leftModel);
            }
            String leftBefore = describe(left);
            String rightBefore = describe(right);
            BitmapIndexedMapNode<Key, Object> result = left.concat(right, 0);
            java.util.Map<Key, Kept> model = new java.util.HashMap<>(leftModel);
            model.putAll(rightModel);
            assertCanonical(result);
            assertHolds(result, model);
            assertThat(describe(left)).isEqualTo(leftBefore);
            assertThat(describe(right)).isEqualTo(rightBefore);
            if (rightModel.keySet().containsAll(leftModel.keySet())) {
                assertThat(result).as("nothing of the left side is needed").isSameAs(right);
            }
        }
    }

    @Test
    public void shouldConcatMapsAtTheNodeBoundaries() {
        int[] sizes = {0, 1, 2, 31, 32, 33, 1023, 1024, 1025};
        for (int leftSize : sizes) {
            for (int rightSize : sizes) {
                BitmapIndexedMapNode<Key, String> left = Vector.range(0, leftSize)
                        .foldLeft(MapNode.<Key, String>empty(), (acc, i) -> acc.updated(new Key(i, 0), "l" + i));
                // keys of mixed hashes 0, 1, 2...: the node boundaries; the right side overlaps the upper half of the
                // left one, and goes beyond it
                BitmapIndexedMapNode<Key, String> right = Vector.range(0, rightSize)
                        .foldLeft(
                                MapNode.<Key, String>empty(),
                                (acc, i) -> acc.updated(new Key(leftSize / 2 + i, 0), "r" + i));
                BitmapIndexedMapNode<Key, String> result = left.concat(right, 0);
                assertValid(result);
                java.util.Map<Key, String> expected = new java.util.HashMap<>();
                for (int i = 0; i < leftSize; i++) {
                    expected.put(new Key(i, 0), "l" + i);
                }
                for (int i = 0; i < rightSize; i++) {
                    expected.put(new Key(leftSize / 2 + i, 0), "r" + i);
                }
                java.util.Map<Key, String> actual = new java.util.HashMap<>();
                result.forEach(actual::put);
                assertThat(actual).isEqualTo(expected);
            }
        }
    }

    @Test
    public void shouldFilterAMapNodeByNodeInIterationOrder() {
        Random random = new Random(SEED + 1);
        for (int round = 0; round < 300; round++) {
            java.util.Map<Key, Kept> model = new java.util.HashMap<>();
            BitmapIndexedMapNode<Key, Object> trie = randomMap(random, new ArrayList<>(), model);
            String before = describe(trie);
            int mode = random.nextInt(4);
            java.util.Set<Key> chosen = new java.util.HashSet<>();
            for (Key key : model.keySet()) {
                if (mode == 0 || (mode == 2 && random.nextBoolean()) || (mode == 3 && random.nextInt(20) == 0)) {
                    chosen.add(key);
                }
            }
            boolean keep = random.nextBoolean();
            java.util.List<Object> seen = new ArrayList<>();
            BitmapIndexedMapNode<Key, Object> result = trie.filter(
                    (k, v) -> {
                        seen.add(k);
                        return chosen.contains(k);
                    },
                    keep);
            // the predicate sees each entry once, in iteration order
            java.util.List<Object> order = new ArrayList<>();
            trie.keysIterator().forEachRemaining(order::add);
            assertThat(seen).containsExactlyElementsOf(order);
            java.util.Map<Key, Kept> expected = new java.util.HashMap<>(model);
            expected.keySet().removeIf(k -> chosen.contains(k) != keep);
            assertCanonical(result);
            assertHolds(result, expected);
            assertThat(describe(trie)).isEqualTo(before);
            if (expected.size() == model.size()) {
                assertThat(result).isSameAs(trie);
            }
        }
    }

    @Test
    public void shouldTransformTheValuesKeepingTheShape() {
        Random random = new Random(SEED + 2);
        for (int round = 0; round < 100; round++) {
            java.util.Map<Key, Kept> model = new java.util.HashMap<>();
            BitmapIndexedMapNode<Key, Object> trie = randomMap(random, new ArrayList<>(), model);
            String before = describe(trie);
            // the same objects: nothing changes
            assertThat(trie.transform((k, v) -> v)).isSameAs(trie);
            java.util.List<Object> seen = new ArrayList<>();
            BitmapIndexedMapNode<Key, Object> result = trie.transform((k, v) -> {
                seen.add(k);
                return k.id() == 0 ? v : java.util.List.of(v);
            });
            java.util.List<Object> order = new ArrayList<>();
            trie.keysIterator().forEachRemaining(order::add);
            assertThat(seen).containsExactlyElementsOf(order);
            assertValid(result);
            assertThat(result.dataMap).isEqualTo(trie.dataMap);
            assertThat(result.nodeMap).isEqualTo(trie.nodeMap);
            assertThat(result.hashes).isEqualTo(trie.hashes);
            java.util.Map<Key, Kept> expected = new java.util.HashMap<>();
            model.forEach((k, kept) -> expected.put(k, kept));
            for (Object[] entry : entries(result)) {
                Kept kept = model.get((Key) entry[0]);
                assertThat(entry[0]).isSameAs(kept.key());
                if (((Key) entry[0]).id() == 0) {
                    assertThat(entry[1]).isSameAs(kept.value());
                } else {
                    assertThat(entry[1]).isEqualTo(java.util.List.of(kept.value()));
                }
            }
            assertThat(describe(trie)).isEqualTo(before);
            if (trie.size() > 0) {
                assertThatThrownBy(() -> trie.transform((k, v) -> null))
                        .isInstanceOf(NullPointerException.class)
                        .hasMessage("HashMap: value is null");
            }
        }
    }

    @Test
    public void shouldTellEqualMapsFromDifferentOnesNodeByNode() {
        Random random = new Random(SEED + 3);
        for (int round = 0; round < 200; round++) {
            java.util.Map<Key, Kept> model = new java.util.HashMap<>();
            BitmapIndexedMapNode<Key, Object> trie = randomMap(random, new ArrayList<>(), model);
            // the same entries, equal but not identical keys and values, in another order
            java.util.List<Key> keys = new ArrayList<>(model.keySet());
            Collections.shuffle(keys, random);
            BitmapIndexedMapNode<Key, Object> copy = Vector.ofAll(keys)
                    .foldLeft(
                            MapNode.<Key, Object>empty(),
                            (acc, key) -> acc.updated(
                                    new Key(key.hash(), key.id()),
                                    new String("v"
                                            + System.identityHashCode(
                                                    model.get(key).value()))));
            BitmapIndexedMapNode<Key, Object> same = Vector.ofAll(keys)
                    .foldLeft(
                            MapNode.<Key, Object>empty(),
                            (acc, key) -> acc.updated(
                                    new Key(key.hash(), key.id()),
                                    model.get(key).value()));
            assertThat(MapNode.sameEntries(trie, same)).isTrue();
            assertThat(MapNode.sameEntries(same, trie)).isTrue();
            assertThat(MapNode.sameEntries(trie, trie)).isTrue();
            if (!keys.isEmpty()) {
                assertThat(MapNode.sameEntries(trie, copy)).as("other values").isFalse();
                Key one = keys.get(0);
                // one value differs
                assertThat(MapNode.sameEntries(trie, same.updated(one, "other")))
                        .isFalse();
                // one key missing, one key more
                assertThat(MapNode.sameEntries(trie, same.removed(one))).isFalse();
                assertThat(MapNode.sameEntries(trie.removed(one), same)).isFalse();
                // one key swapped for another of the same hash: same size and hashes, different keys
                Key swapped = new Key(one.hash(), 7);
                assertThat(MapNode.sameEntries(
                                trie,
                                same.removed(one)
                                        .updated(swapped, model.get(one).value())))
                        .isFalse();
            }
        }
    }

    // -- sets

    private static BitmapIndexedSetNode<Key> randomSet(
            Random random, java.util.List<Key> pool, java.util.Map<Key, Key> model) {
        int size = switch (random.nextInt(4)) {
            case 0 -> random.nextInt(3);
            case 1 -> random.nextInt(40);
            default -> random.nextInt(1500);
        };
        int range = random.nextBoolean() ? 100 : Integer.MAX_VALUE;
        return Vector.range(0, size).foldLeft(SetNode.<Key>empty(), (trie, i) -> {
            Key key = sharedOrRandomKey(random, pool, range);
            model.put(key, key);
            return trie.updated(key, true);
        });
    }

    private static java.util.List<Key> elements(SetNode<Key> trie) {
        java.util.List<Key> result = new ArrayList<>();
        trie.iterator().forEachRemaining(result::add);
        return result;
    }

    private static void assertHolds(SetNode<Key> trie, java.util.Map<Key, Key> model) {
        assertThat(trie.size()).isEqualTo(model.size());
        java.util.Map<Key, Key> actual = new java.util.HashMap<>();
        for (Key element : elements(trie)) {
            assertThat(actual.put(element, element)).isNull();
        }
        assertThat(actual.keySet()).isEqualTo(model.keySet());
        model.forEach((key, kept) -> assertThat(actual.get(key)).isSameAs(kept));
    }

    private static void assertCanonical(BitmapIndexedSetNode<Key> trie) {
        assertValid(trie);
        java.util.List<Key> all = elements(trie);
        Collections.reverse(all);
        BitmapIndexedSetNode<Key> fresh =
                Vector.ofAll(all).foldLeft(SetNode.<Key>empty(), (acc, element) -> acc.updated(element, true));
        assertSameShape(fresh, trie, false);
    }

    @Test
    public void shouldConcatDiffAndCompareTwoSets() {
        Random random = new Random(SEED + 4);
        for (int round = 0; round < 400; round++) {
            java.util.List<Key> pool = new ArrayList<>();
            java.util.Map<Key, Key> leftModel = new java.util.HashMap<>();
            java.util.Map<Key, Key> rightModel = new java.util.HashMap<>();
            BitmapIndexedSetNode<Key> left = randomSet(random, pool, leftModel);
            BitmapIndexedSetNode<Key> right;
            switch (random.nextInt(6)) {
                case 0 -> {
                    right = left;
                    rightModel.putAll(leftModel);
                }
                case 1 -> {
                    // a subset of the left side, as new equal objects
                    right = Vector.ofAll(leftModel.keySet()).foldLeft(SetNode.<Key>empty(), (subset, key) -> {
                        if (!random.nextBoolean()) {
                            return subset;
                        }
                        Key equal = new Key(key.hash(), key.id());
                        rightModel.put(equal, equal);
                        return subset.updated(equal, true);
                    });
                }
                default -> right = randomSet(random, pool, rightModel);
            }
            String leftBefore = describe(left);
            String rightBefore = describe(right);

            BitmapIndexedSetNode<Key> union = left.concat(right, 0);
            java.util.Map<Key, Key> unionModel = new java.util.HashMap<>(leftModel);
            unionModel.putAll(rightModel);
            assertCanonical(union);
            assertHolds(union, unionModel);
            if (rightModel.keySet().containsAll(leftModel.keySet())) {
                assertThat(union).isSameAs(right);
            }

            BitmapIndexedSetNode<Key> diff = left.diff(right, 0);
            java.util.Map<Key, Key> diffModel = new java.util.HashMap<>(leftModel);
            diffModel.keySet().removeAll(rightModel.keySet());
            assertCanonical(diff);
            assertHolds(diff, diffModel);
            if (diffModel.size() == leftModel.size()) {
                assertThat(diff).isSameAs(left);
            }

            assertThat(left.subsetOf(right, 0)).isEqualTo(rightModel.keySet().containsAll(leftModel.keySet()));
            assertThat(right.subsetOf(left, 0)).isEqualTo(leftModel.keySet().containsAll(rightModel.keySet()));
            assertThat(SetNode.sameElements(left, right))
                    .isEqualTo(leftModel.keySet().equals(rightModel.keySet()));
            assertThat(SetNode.sameElements(right, left))
                    .isEqualTo(leftModel.keySet().equals(rightModel.keySet()));

            assertThat(describe(left)).isEqualTo(leftBefore);
            assertThat(describe(right)).isEqualTo(rightBefore);
        }
    }

    @Test
    public void shouldConcatAndDiffSetsAtTheNodeBoundaries() {
        int[] sizes = {0, 1, 2, 31, 32, 33, 1023, 1024, 1025};
        Function<int[], BitmapIndexedSetNode<Key>> range = bounds -> {
            return Vector.range(bounds[0], bounds[1])
                    .foldLeft(SetNode.<Key>empty(), (trie, i) -> trie.updated(new Key(i, 0), true));
        };
        for (int leftSize : sizes) {
            for (int rightSize : sizes) {
                BitmapIndexedSetNode<Key> left = range.apply(new int[] {0, leftSize});
                BitmapIndexedSetNode<Key> right = range.apply(new int[] {leftSize / 2, leftSize / 2 + rightSize});
                BitmapIndexedSetNode<Key> union = left.concat(right, 0);
                assertValid(union);
                assertThat(union.size()).isEqualTo(Math.max(leftSize, leftSize / 2 + rightSize));
                BitmapIndexedSetNode<Key> diff = left.diff(right, 0);
                assertValid(diff);
                assertThat(diff.size())
                        .isEqualTo(
                                Math.min(leftSize, leftSize / 2) + Math.max(0, leftSize - (leftSize / 2 + rightSize)));
                for (int i = 0; i < leftSize; i++) {
                    assertThat(diff.contains(new Key(i, 0)))
                            .isEqualTo(i < leftSize / 2 || i >= leftSize / 2 + rightSize);
                }
                assertThat(right.subsetOf(union, 0)).isTrue();
                assertThat(left.subsetOf(union, 0)).isTrue();
                assertThat(union.subsetOf(left, 0)).isEqualTo(union.size() == leftSize);
            }
        }
    }

    @Test
    public void shouldFilterASetNodeByNodeInIterationOrder() {
        Random random = new Random(SEED + 5);
        for (int round = 0; round < 300; round++) {
            java.util.Map<Key, Key> model = new java.util.HashMap<>();
            BitmapIndexedSetNode<Key> trie = randomSet(random, new ArrayList<>(), model);
            String before = describe(trie);
            int mode = random.nextInt(4);
            java.util.Set<Key> chosen = new java.util.HashSet<>();
            for (Key key : model.keySet()) {
                if (mode == 0 || (mode == 2 && random.nextBoolean()) || (mode == 3 && random.nextInt(20) == 0)) {
                    chosen.add(key);
                }
            }
            boolean keep = random.nextBoolean();
            java.util.List<Key> seen = new ArrayList<>();
            BitmapIndexedSetNode<Key> result = trie.filter(
                    k -> {
                        seen.add(k);
                        return chosen.contains(k);
                    },
                    keep);
            assertThat(seen).containsExactlyElementsOf(elements(trie));
            java.util.Map<Key, Key> expected = new java.util.HashMap<>(model);
            expected.keySet().removeIf(k -> chosen.contains(k) != keep);
            assertCanonical(result);
            assertHolds(result, expected);
            assertThat(describe(trie)).isEqualTo(before);
            if (expected.size() == model.size()) {
                assertThat(result).isSameAs(trie);
            }
        }
    }
}
