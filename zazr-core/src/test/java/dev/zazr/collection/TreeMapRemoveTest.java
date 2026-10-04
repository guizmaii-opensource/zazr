package dev.zazr.collection;

import dev.zazr.Tuple;
import dev.zazr.Tuple2;
import dev.zazr.collection.internal.RedBlackTree;
import dev.zazr.collection.internal.RedBlackTreeValidity;
import dev.zazr.control.Option;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// The removals by key of `TreeMap` (`remove`, `removeAll(Iterable)`, and through them the removal of `updateWith`
/// and the delegations of `NonEmptySortedMap`), against a `java.util.TreeMap` holding the same puts under the same
/// comparator: natural (the default and an explicit one), reversed, and two that map many keys to one. Every result
/// is checked as a valid red-black tree, an absent key gives the map itself, and the receiver never changes.
public class TreeMapRemoveTest {

    private static final long SEED = 20261005L;
    private static final int[] SIZES = {0, 1, 2, 3, 31, 32, 33, 1023, 1024, 1025};

    private static final Comparator<Integer> NATURAL = Comparator.naturalOrder();
    private static final Comparator<Integer> REVERSED = Comparator.reverseOrder();
    // two keys a multiple of 1000 apart are the same key
    private static final Comparator<Integer> MODULO_1000 = Comparator.comparingInt(i -> Math.floorMod(i, 1000));
    // at most 7 distinct keys, whatever is put
    private static final Comparator<Integer> MODULO_7 = Comparator.comparingInt(i -> Math.floorMod(i, 7));

    /// A map and its model, built from the same random puts; `bound` is past every stored key on both sides.
    private record Fixture(
            String name, TreeMap<Integer, String> map, java.util.TreeMap<Integer, String> model, int bound) {

        /// Every key from `-bound` to `bound` on the small maps, every 17th on the large ones.
        int[] sampledKeys() {
            int step = map.size() > 100 ? 17 : 1;
            return java.util.stream.IntStream.iterate(-bound, key -> key <= bound, key -> key + step)
                    .toArray();
        }
    }

    // a put on the model as on the map: the key given replaces an equal stored one
    private static void put(java.util.TreeMap<Integer, String> model, Integer key, String value) {
        model.remove(key);
        model.put(key, value);
    }

    // `defaultNatural`: `TreeMap.empty()`, the natural order the map recognises as its own, rather than `comparator`
    private static Fixture fixture(
            String name, Comparator<Integer> comparator, boolean defaultNatural, int size, Random random) {
        java.util.TreeMap<Integer, String> model = new java.util.TreeMap<>(comparator);
        int bound = 2 * size + 2;
        TreeMap<Integer, String> empty = defaultNatural ? TreeMap.empty() : TreeMap.empty(comparator);
        List<Integer> puts = new ArrayList<>();
        for (int i = 0; i < 2 * size && model.size() < size; i++) {
            puts.add(random.nextInt(2 * bound + 1) - bound);
        }
        TreeMap<Integer, String> map = Vector.ofAll(puts).zipWithIndex().foldLeft(empty, (acc, put) -> {
            String value = "v" + put._2();
            put(model, put._1(), value);
            return acc.put(put._1(), value);
        });
        return new Fixture(name + " size " + map.size(), map, model, bound + 3);
    }

    private static List<Fixture> fixtures() {
        Random random = new Random(SEED);
        List<Fixture> fixtures = new ArrayList<>();
        for (int size : SIZES) {
            fixtures.add(fixture("natural (default)", NATURAL, true, size, random));
            fixtures.add(fixture("natural", NATURAL, false, size, random));
            fixtures.add(fixture("reversed", REVERSED, false, size, random));
            fixtures.add(fixture("modulo 1000", MODULO_1000, false, size, random));
            fixtures.add(fixture("modulo 7", MODULO_7, false, size, random));
        }
        return fixtures;
    }

    private static <K, V> List<Tuple2<K, V>> entries(java.util.Map<K, V> model) {
        List<Tuple2<K, V>> entries = new ArrayList<>();
        model.forEach((key, value) -> entries.add(Tuple.of(key, value)));
        return entries;
    }

    private static <K, V> List<Tuple2<K, V>> entries(Iterable<Tuple2<K, V>> map) {
        List<Tuple2<K, V>> entries = new ArrayList<>();
        map.forEach(entries::add);
        return entries;
    }

    @SuppressWarnings("unchecked")
    private static <K, V> RedBlackTree<Tuple2<K, V>> tree(TreeMap<K, V> map) {
        try {
            java.lang.reflect.Field field = TreeMap.class.getDeclaredField("entries");
            field.setAccessible(true);
            return (RedBlackTree<Tuple2<K, V>>) field.get(map);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    // the entries of `result` are the model's, as a valid tree; an unchanged model means the receiver itself
    private static void assertRemoved(
            String context,
            TreeMap<Integer, String> receiver,
            TreeMap<Integer, String> result,
            java.util.TreeMap<Integer, String> model) {
        assertThat(entries(result)).as(context).isEqualTo(entries(model));
        assertThat(result.size()).as(context).isEqualTo(model.size());
        assertThat(result.comparator()).as(context).isSameAs(receiver.comparator());
        RedBlackTreeValidity.assertValid(tree(result));
        if (model.size() == receiver.size()) {
            assertThat(result).as(context).isSameAs(receiver);
        }
    }

    // -- remove

    @Test
    void removeAgreesWithTheModel() {
        for (Fixture fixture : fixtures()) {
            TreeMap<Integer, String> map = fixture.map();
            List<Tuple2<Integer, String>> before = entries(map);
            for (int key : fixture.sampledKeys()) {
                java.util.TreeMap<Integer, String> model = new java.util.TreeMap<>(fixture.model());
                model.remove(key);
                assertRemoved(fixture.name() + " key " + key, map, map.remove(key), model);
            }
            // persistence: the receiver never changes
            assertThat(entries(map)).isEqualTo(before);
        }
    }

    @Test
    void removingEveryKeyOneAtATimeEndsEmpty() {
        Random random = new Random(SEED + 1);
        for (Fixture fixture : fixtures()) {
            List<Integer> keys = new ArrayList<>(fixture.model().keySet());
            Collections.shuffle(keys, random);
            java.util.TreeMap<Integer, String> model = new java.util.TreeMap<>(fixture.model());
            List<TreeMap<Integer, String>> versions = new ArrayList<>();
            TreeMap<Integer, String> last = Vector.ofAll(keys).foldLeft(fixture.map(), (map, key) -> {
                versions.add(map);
                model.remove(key);
                TreeMap<Integer, String> removed = map.remove(key);
                assertRemoved(fixture.name() + " key " + key, map, removed, model);
                // a key removed is absent: removing it again gives the same map
                assertThat(removed.remove(key)).isSameAs(removed);
                return removed;
            });
            assertThat(last.isEmpty()).as(fixture.name()).isTrue();
            assertThat(last.comparator())
                    .as(fixture.name())
                    .isSameAs(fixture.map().comparator());
            assertThat(last.remove(0)).isSameAs(last);
            // every older version is still whole: its size is the number of keys not yet removed when it was made
            for (int i = 0; i < versions.size(); i++) {
                assertThat(versions.get(i).size()).isEqualTo(keys.size() - i);
                RedBlackTreeValidity.assertValid(tree(versions.get(i)));
            }
        }
    }

    // -- removeAll

    @Test
    void removeAllAgreesWithTheModel() {
        Random random = new Random(SEED + 2);
        for (Fixture fixture : fixtures()) {
            TreeMap<Integer, String> map = fixture.map();
            List<Tuple2<Integer, String>> before = entries(map);
            for (int round = 0; round < 20; round++) {
                // a few keys, present or not, some repeated
                List<Integer> keys = new ArrayList<>();
                int count = random.nextInt(Math.min(2 * fixture.bound(), 64) + 1);
                for (int i = 0; i < count; i++) {
                    keys.add(random.nextInt(2 * fixture.bound() + 1) - fixture.bound());
                }
                if (!keys.isEmpty()) {
                    keys.add(keys.get(0));
                }
                java.util.TreeMap<Integer, String> model = new java.util.TreeMap<>(fixture.model());
                keys.forEach(model::remove);
                String context = fixture.name() + " keys " + keys;
                assertRemoved(context, map, map.removeAll(keys), model);
                // a one-shot iterable gives the same result
                AtomicBoolean iterated = new AtomicBoolean();
                Iterable<Integer> once = () -> {
                    if (iterated.getAndSet(true)) {
                        throw new IllegalStateException("iterated twice");
                    }
                    return keys.iterator();
                };
                assertRemoved(context, map, map.removeAll(once), model);
            }
            // every key at once
            assertRemoved(
                    fixture.name(),
                    map,
                    map.removeAll(List.copyOf(fixture.model().keySet())),
                    new java.util.TreeMap<>(fixture.model().comparator()));
            // no key, or only absent ones: the map itself
            assertThat(map.removeAll(List.of())).isSameAs(map);
            List<Integer> absent = java.util.stream.IntStream.of(fixture.sampledKeys())
                    .boxed()
                    .filter(key -> !fixture.model().containsKey(key))
                    .toList();
            assertThat(map.removeAll(absent)).isSameAs(map);
            assertThat(entries(map)).isEqualTo(before);
        }
    }

    // -- the stored objects

    @Test
    void theStoredKeyObjectsOfTheOtherEntriesStay() {
        // keys equal under the comparator, but not under equals
        List<String> keys = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            keys.add(new String("Key" + i));
        }
        TreeMap<String, String> map =
                Vector.ofAll(keys).foldLeft(TreeMap.empty(String.CASE_INSENSITIVE_ORDER), (m, k) -> m.put(k, k));
        IdentityHashMap<String, Boolean> stored = new IdentityHashMap<>();
        keys.forEach(key -> stored.put(key, true));
        for (int i = 0; i < 100; i += 7) {
            String other = ("KEY" + i).toLowerCase(java.util.Locale.ROOT);
            TreeMap<String, String> removed = map.remove(other);
            assertThat(removed.size()).isEqualTo(99);
            assertThat(removed.containsKey("Key" + i)).isFalse();
            for (Tuple2<String, String> entry : removed) {
                assertThat(stored).containsKey(entry._1());
                assertThat(entry._2()).isSameAs(entry._1());
            }
            RedBlackTreeValidity.assertValid(tree(removed));
        }
        TreeMap<String, String> removedAll = map.removeAll(List.of("KEY1", "key2", "kEy3", "absent"));
        assertThat(removedAll.size()).isEqualTo(97);
        for (Tuple2<String, String> entry : removedAll) {
            assertThat(stored).containsKey(entry._1());
        }
    }

    // -- nulls and keys the comparator rejects

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void aNaturalOrderMapRejectsANullOrIncomparableKeyOnlyWhenNotEmpty() {
        TreeMap<Integer, String> empty = TreeMap.empty();
        assertThat(empty.remove(null)).isSameAs(empty);
        assertThat(empty.removeAll(Arrays.asList((Integer) null))).isSameAs(empty);
        TreeMap<Integer, String> map = TreeMap.of(1, "one", 2, "two");
        assertThatThrownBy(() -> map.remove(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> map.removeAll(Arrays.asList((Integer) null))).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> map.updateWith(null, o -> Option.none())).isInstanceOf(NullPointerException.class);

        TreeMap<Object, String> objects = (TreeMap) map;
        TreeMap<Object, String> emptyObjects = (TreeMap) empty;
        Object incomparable = new Object();
        assertThat(emptyObjects.remove(incomparable)).isSameAs(emptyObjects);
        assertThatThrownBy(() -> objects.remove(incomparable)).isInstanceOf(ClassCastException.class);
        assertThatThrownBy(() -> objects.removeAll(List.of(incomparable))).isInstanceOf(ClassCastException.class);
        // the receiver is unchanged after a throw
        assertThat(entries(map)).isEqualTo(List.of(Tuple.of(1, "one"), Tuple.of(2, "two")));
    }

    @Test
    void aComparatorThatAcceptsNullDecides() {
        TreeMap<Integer, String> map = TreeMap.<Integer, String>empty(Comparator.nullsFirst(NATURAL))
                .put(1, "one")
                .put(2, "two");
        assertThat(map.remove(null)).isSameAs(map);
        assertThat(map.removeAll(Arrays.asList(null, 3))).isSameAs(map);
        assertThat(entries(map.removeAll(Arrays.asList(null, 2)))).isEqualTo(List.of(Tuple.of(1, "one")));
    }

    // -- the comparisons

    @Test
    void theKeyIsTheFirstArgumentOfEachComparison() {
        List<Integer> firsts = new ArrayList<>();
        Comparator<Integer> recording = (a, b) -> {
            firsts.add(a);
            return Integer.compare(a, b);
        };
        TreeMap<Integer, String> map =
                Vector.range(0, 100).map(i -> 2 * i).foldLeft(TreeMap.empty(recording), (m, k) -> m.put(k, "v" + k));
        for (int key : new int[] {-1, 0, 41, 42, 99, 198, 199}) {
            firsts.clear();
            map.remove(key);
            assertThat(firsts).as("remove " + key).isNotEmpty().containsOnly(key);
            firsts.clear();
            map.removeAll(List.of(key));
            assertThat(firsts).as("removeAll " + key).isNotEmpty().containsOnly(key);
        }
    }

    // -- the callers that remove through `remove`

    @Test
    void updateWithAndNonEmptySortedMapRemoveAsTheModel() {
        for (Fixture fixture : fixtures()) {
            TreeMap<Integer, String> map = fixture.map();
            Option<NonEmptySortedMap<Integer, String>> nonEmpty = map.toNonEmptySortedMap();
            for (int key : fixture.sampledKeys()) {
                java.util.TreeMap<Integer, String> model = new java.util.TreeMap<>(fixture.model());
                model.remove(key);
                String context = fixture.name() + " key " + key;
                assertRemoved(context, map, map.updateWith(key, o -> Option.none()), model);
                if (nonEmpty.isDefined()) {
                    NonEmptySortedMap<Integer, String> wrapped = nonEmpty.get();
                    TreeMap<Integer, String> removed = wrapped.remove(key);
                    assertThat(entries(removed)).as(context).isEqualTo(entries(model));
                    RedBlackTreeValidity.assertValid(tree(removed));
                    TreeMap<Integer, String> removedAll = wrapped.removeAll(List.of(key, key));
                    assertThat(entries(removedAll)).as(context).isEqualTo(entries(model));
                }
            }
        }
    }
}
