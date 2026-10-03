package dev.zazr.collection;

import dev.zazr.Tuple;
import dev.zazr.Tuple2;
import dev.zazr.collection.internal.RedBlackTree;
import dev.zazr.collection.internal.RedBlackTreeValidity;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// `putAll` on every map type against successive `put`s of the same entries, compared by identity: the same key and
/// value objects, and the same iteration order where the type defines one (and on a `HashMap` without colliding
/// hashes, whose shape is canonical). Every kind of argument goes through it: the three map types (a `TreeMap` of the
/// receiver's comparator and one of another), the non-empty maps, the `asJava()` views, a `java.util.List`, a
/// `Vector` and a one-shot iterable, the last three with repeated keys.
public class MapPutAllTest {

    /// Equal and ordered by `id` alone; `tag` tells equal keys apart. The hash code is `id % modulus`, so a small
    /// modulus puts many keys in each collision node.
    static final class Key implements Comparable<Key> {
        final int id;
        final int tag;
        final int modulus;

        Key(int id, int tag, int modulus) {
            this.id = id;
            this.tag = tag;
            this.modulus = modulus;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Key that && that.id == id;
        }

        @Override
        public int hashCode() {
            return id % modulus;
        }

        @Override
        public int compareTo(Key that) {
            return Integer.compare(id, that.id);
        }

        @Override
        public String toString() {
            return id + "/" + tag;
        }
    }

    private static final int[] SIZES = {0, 1, 2, 31, 32, 33, 1023, 1024, 1025};
    /// With colliding hashes, a few large collision nodes: each lookup in one is a linear scan.
    private static final int[] COLLIDING_SIZES = {0, 1, 2, 31, 32, 33, 200};
    private static final int NO_COLLISION = Integer.MAX_VALUE;
    private static final int[] MODULI = {NO_COLLISION, 7};
    private static final Comparator<Key> REVERSED = Comparator.reverseOrder();
    private static final AtomicInteger TAGS = new AtomicInteger();

    // -- the subjects

    /// A receiver type: how to build it from entries, the plain map it is or wraps (where successive puts start
    /// from), and `putAll`, whose result is unwrapped to a plain map.
    private record Receiver(
            String name,
            Function<java.util.List<Tuple2<Key, String>>, Object> build,
            Function<Object, Map<Key, String>> plain,
            BiFunction<Object, Iterable<Tuple2<Key, String>>, Object> putAll,
            boolean ordered,
            boolean nonEmpty) {}

    @SuppressWarnings("unchecked")
    private static java.util.List<Receiver> receivers() {
        Function<Object, Map<Key, String>> itself = map -> (Map<Key, String>) map;
        return java.util.List.of(
                new Receiver(
                        "HashMap",
                        HashMap::ofEntries,
                        itself,
                        (r, a) -> ((HashMap<Key, String>) r).putAll(a),
                        false,
                        false),
                new Receiver(
                        "LinkedHashMap",
                        LinkedHashMap::ofEntries,
                        itself,
                        (r, a) -> ((LinkedHashMap<Key, String>) r).putAll(a),
                        true,
                        false),
                new Receiver(
                        "TreeMap",
                        TreeMap::ofEntries,
                        itself,
                        (r, a) -> ((TreeMap<Key, String>) r).putAll(a),
                        true,
                        false),
                new Receiver(
                        "TreeMap reversed",
                        r -> TreeMap.ofEntries(REVERSED, r),
                        itself,
                        (r, a) -> ((TreeMap<Key, String>) r).putAll(a),
                        true,
                        false),
                new Receiver(
                        "NonEmptyMap",
                        r -> NonEmptyMap.fromIterable(r).get(),
                        r -> ((NonEmptyMap<Key, String>) r).toMap(),
                        (r, a) -> ((NonEmptyMap<Key, String>) r).putAll(a),
                        false,
                        true),
                new Receiver(
                        "NonEmptySortedMap",
                        r -> NonEmptySortedMap.fromSortedMap(TreeMap.ofEntries(r))
                                .get(),
                        r -> ((NonEmptySortedMap<Key, String>) r).toSortedMap(),
                        (r, a) -> ((NonEmptySortedMap<Key, String>) r).putAll(a),
                        true,
                        true));
    }

    /// An argument: its name and the iterable, built from distinct entries and a few entries repeating their keys.
    private record Argument(
            String name, Iterable<Tuple2<Key, String>> entries, java.util.List<Tuple2<Key, String>> order) {}

    private static java.util.List<Argument> arguments(
            java.util.List<Tuple2<Key, String>> distinct, java.util.List<Tuple2<Key, String>> withRepeats) {
        java.util.List<Argument> result = new ArrayList<>();
        java.util.List<Iterable<Tuple2<Key, String>>> maps = new ArrayList<>();
        maps.add(HashMap.ofEntries(distinct));
        maps.add(LinkedHashMap.ofEntries(distinct));
        maps.add(TreeMap.ofEntries(distinct));
        maps.add(TreeMap.ofEntries(REVERSED, distinct));
        maps.add(HashMap.ofEntries(distinct).asJava());
        maps.add(LinkedHashMap.ofEntries(distinct).asJava());
        maps.add(TreeMap.ofEntries(distinct).asJava());
        maps.add(TreeMap.ofEntries(REVERSED, distinct).asJava());
        if (!distinct.isEmpty()) {
            maps.add(NonEmptyMap.fromIterable(distinct).get());
            maps.add(NonEmptySortedMap.fromIterable(distinct).get());
        }
        for (Iterable<Tuple2<Key, String>> map : maps) {
            result.add(new Argument(map.getClass().getSimpleName(), map, listOf(map)));
        }
        result.add(new Argument("ArrayList", new ArrayList<>(withRepeats), withRepeats));
        result.add(new Argument("Vector", Vector.ofAll(withRepeats), withRepeats));
        result.add(new Argument("one-shot", oneShot(withRepeats), withRepeats));
        return result;
    }

    // -- the differential test

    @Test
    public void shouldPutAllAsSuccessivePuts() {
        Random random = new Random(20260928L);
        for (int modulus : MODULI) {
            int[] sizes = modulus == NO_COLLISION ? SIZES : COLLIDING_SIZES;
            for (int n : sizes) {
                for (int m : sizes) {
                    for (int start : new int[] {0, n / 2, n}) {
                        // start 0: every key shared when m <= n; start n: no key shared
                        check(random, modulus, n, m, start);
                    }
                }
            }
        }
    }

    private static void check(Random random, int modulus, int n, int m, int start) {
        java.util.List<Tuple2<Key, String>> receiverEntries = entries(random, 0, n, modulus);
        java.util.List<Tuple2<Key, String>> distinct = entries(random, start, start + m, modulus);
        java.util.List<Tuple2<Key, String>> withRepeats = new ArrayList<>(distinct);
        for (int i = 0; i < Math.min(m, 3); i++) {
            // a key put twice more: its last entry wins
            withRepeats.add(entry(distinct.get(random.nextInt(m))._1().id, modulus));
        }
        for (Receiver receiver : receivers()) {
            if (receiver.nonEmpty() && n == 0) {
                continue;
            }
            Object built = receiver.build().apply(receiverEntries);
            Map<Key, String> plain = receiver.plain().apply(built);
            java.util.List<Object[]> before = identities(plain);
            for (Argument argument : arguments(distinct, withRepeats)) {
                String scenario = receiver.name() + " <- " + argument.name() + ", modulus " + modulus + ", n " + n
                        + ", m " + m + ", start " + start;
                Map<Key, String> expected = successivePuts(plain, argument.order());
                Map<Key, String> actual =
                        receiver.plain().apply(receiver.putAll().apply(built, argument.entries()));
                assertSameEntries(scenario, expected, actual, receiver.ordered() || modulus == NO_COLLISION);
                if (actual instanceof TreeMap<Key, String> tree) {
                    RedBlackTreeValidity.assertValid(tree(tree));
                }
            }
            // the receiver still holds the same key and value objects, in the same order
            java.util.List<Object[]> after = identities(receiver.plain().apply(built));
            assertThat(after).hasSameSizeAs(before);
            for (int i = 0; i < before.size(); i++) {
                if (after.get(i)[0] != before.get(i)[0] || after.get(i)[1] != before.get(i)[1]) {
                    throw new AssertionError(receiver.name() + ": the receiver's entry " + i + " changed");
                }
            }
        }
        // the inputs are never changed: the lists hold the same objects
        assertThat(receiverEntries).hasSize(n);
        assertThat(distinct).hasSize(m);
    }

    private static Map<Key, String> successivePuts(Map<Key, String> start, Iterable<Tuple2<Key, String>> entries) {
        return Vector.ofAll(entries).foldLeft(start, Map::put);
    }

    private static void assertSameEntries(
            String scenario, Map<Key, String> expected, Map<Key, String> actual, boolean ordered) {
        assertThat(actual).as(scenario).isEqualTo(expected);
        assertThat(actual.size()).as(scenario).isEqualTo(expected.size());
        java.util.List<Object[]> actualIds = identities(actual);
        java.util.List<Object[]> expectedIds = identities(expected);
        if (!ordered) {
            Comparator<Object[]> byId = Comparator.comparingInt(e -> ((Key) e[0]).id);
            actualIds.sort(byId);
            expectedIds.sort(byId);
        }
        // a plain check per entry: no AssertJ assertion or description is built unless it fails
        for (int i = 0; i < expectedIds.size(); i++) {
            if (actualIds.get(i)[0] != expectedIds.get(i)[0]) {
                throw new AssertionError(scenario + ": key " + i + " is not the expected instance");
            }
            if (actualIds.get(i)[1] != expectedIds.get(i)[1]) {
                throw new AssertionError(scenario + ": value " + i + " is not the expected instance");
            }
        }
    }

    // -- which instance comes back

    @Test
    public void shouldReturnTheReceiverOrTheArgumentWhenOneSideIsEmpty() {
        HashMap<Key, String> hash = HashMap.ofEntries(entries(new Random(1), 0, 40, 7));
        assertThat(hash.putAll(HashMap.empty())).isSameAs(hash);
        assertThat(hash.putAll(java.util.List.of())).isSameAs(hash);
        assertThat(HashMap.<Key, String>empty().putAll(hash)).isSameAs(hash);
        assertThat(HashMap.<Key, String>empty().putAll(hash.asJava())).isSameAs(hash);

        LinkedHashMap<Key, String> linked = LinkedHashMap.ofEntries(hash);
        assertThat(linked.putAll(LinkedHashMap.empty())).isSameAs(linked);
        assertThat(LinkedHashMap.<Key, String>empty().putAll(linked)).isSameAs(linked);
        assertThat(LinkedHashMap.<Key, String>empty().putAll(linked.asJava())).isSameAs(linked);

        TreeMap<Key, String> tree = TreeMap.ofEntries(hash);
        assertThat(tree.putAll(TreeMap.empty())).isSameAs(tree);
        assertThat(tree.putAll(java.util.List.of())).isSameAs(tree);
        assertThat(TreeMap.<Key, String>empty().putAll(tree)).isSameAs(tree);
        assertThat(TreeMap.<Key, String>empty().putAll(tree.asJava())).isSameAs(tree);
        // another comparator: the entries are put one by one into a map of this comparator
        TreeMap<Key, String> reversed = TreeMap.ofEntries(REVERSED, hash);
        TreeMap<Key, String> natural = TreeMap.<Key, String>empty().putAll(reversed);
        assertThat(natural).isNotSameAs(reversed).isEqualTo(tree);
        assertThat(natural.comparator()).isNotSameAs(REVERSED);
        assertThat(natural.head()._1().id).isZero();

        NonEmptyMap<Key, String> nonEmpty = NonEmptyMap.fromMap(hash).get();
        assertThat(nonEmpty.putAll(HashMap.empty())).isSameAs(nonEmpty);
        NonEmptySortedMap<Key, String> nonEmptySorted =
                NonEmptySortedMap.fromSortedMap(tree).get();
        assertThat(nonEmptySorted.putAll(TreeMap.empty())).isSameAs(nonEmptySorted);
    }

    @Test
    public void shouldReturnTheReceiverWhenEveryEntryIsAlreadyThere() {
        HashMap<Key, String> hash = HashMap.ofEntries(entries(new Random(2), 0, 100, 7));
        // the same key and value objects, not a HashMap: one put each, none of which changes anything
        assertThat(hash.putAll(Vector.ofAll(hash))).isSameAs(hash);
        assertThat(hash.putAll(hash)).isSameAs(hash);
        NonEmptyMap<Key, String> nonEmpty = NonEmptyMap.fromMap(hash).get();
        assertThat(nonEmpty.putAll(Vector.ofAll(hash))).isSameAs(nonEmpty);
        assertThat(nonEmpty.putAll(nonEmpty)).isSameAs(nonEmpty);
        // an equal key object that is another instance is a change
        Tuple2<Key, String> first = hash.iterator().next();
        Tuple2<Key, String> copy = Tuple.of(new Key(first._1().id, -1, 7), first._2());
        HashMap<Key, String> replaced = hash.putAll(java.util.List.of(copy));
        assertThat(replaced).isNotSameAs(hash).isEqualTo(hash);
        assertThat(replaced.keySet().find(k -> k.id == first._1().id).get()).isSameAs(copy._1());
    }

    @Test
    public void shouldLetTheArgumentWinWhereMergeKeepsTheReceiver() {
        var stock = HashMap.of("apple", 3, "pear", 0);
        assertThat(stock.putAll(HashMap.of("pear", 5, "fig", 1)))
                .isEqualTo(HashMap.of("apple", 3, "pear", 5, "fig", 1));
        assertThat(stock.merge(HashMap.of("pear", 5, "fig", 1))).isEqualTo(HashMap.of("apple", 3, "pear", 0, "fig", 1));
        var order = LinkedHashMap.of("a", 1, "b", 2);
        assertThat(order.putAll(LinkedHashMap.of("c", 3, "a", 10)).toVector())
                .containsExactly(Tuple.of("a", 10), Tuple.of("b", 2), Tuple.of("c", 3));
    }

    @Test
    public void shouldIterateAOneShotArgumentOnce() {
        java.util.List<Tuple2<Key, String>> entries = entries(new Random(3), 0, 50, 7);
        assertThat(HashMap.<Key, String>empty().putAll(oneShot(entries))).hasSize(50);
        assertThat(LinkedHashMap.<Key, String>empty().putAll(oneShot(entries))).hasSize(50);
        assertThat(TreeMap.<Key, String>empty().putAll(oneShot(entries))).hasSize(50);
        assertThat(HashMap.of(new Key(0, 0, 7), "x").putAll(oneShot(entries))).hasSize(50);
        assertThat(LinkedHashMap.of(new Key(0, 0, 7), "x").putAll(oneShot(entries)))
                .hasSize(50);
        assertThat(TreeMap.of(new Key(0, 0, 7), "x").putAll(oneShot(entries))).hasSize(50);
    }

    // -- nulls

    @Test
    public void shouldRejectNulls() {
        java.util.List<Map<Integer, String>> maps = java.util.List.of(
                HashMap.empty(),
                HashMap.of(1, "a"),
                LinkedHashMap.empty(),
                LinkedHashMap.of(1, "a"),
                TreeMap.empty(),
                TreeMap.of(1, "a"));
        for (Map<Integer, String> map : maps) {
            assertThatThrownBy(() -> map.putAll(null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> map.putAll(nullEntry()))
                    .as(map.getClass().getSimpleName())
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> map.putAll(java.util.List.of(Tuple.of(2, "b"), Tuple.of(null, "c"))))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("key is null");
            assertThatThrownBy(() -> map.putAll(java.util.List.of(Tuple.of(2, "b"), Tuple.of(3, null))))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("value is null");
            // the map itself never changes
            assertThat(map.size()).isLessThanOrEqualTo(1);
        }
        NonEmptyMap<Integer, String> nonEmpty = NonEmptyMap.of(Tuple.of(1, "a"));
        assertThatThrownBy(() -> nonEmpty.putAll(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> nonEmpty.putAll(java.util.List.of(Tuple.of(null, "c"))))
                .isInstanceOf(NullPointerException.class);
        NonEmptySortedMap<Integer, String> nonEmptySorted = NonEmptySortedMap.of(Tuple.of(1, "a"));
        assertThatThrownBy(() -> nonEmptySorted.putAll(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> nonEmptySorted.putAll(java.util.List.of(Tuple.of(2, null))))
                .isInstanceOf(NullPointerException.class);
    }

    private static java.util.List<Tuple2<Integer, String>> nullEntry() {
        java.util.List<Tuple2<Integer, String>> entries = new ArrayList<>();
        entries.add(Tuple.of(2, "b"));
        entries.add(null);
        return entries;
    }

    // -- helpers

    /// The entries of the ids `from` until `to`, in a random order, each with a new key object and a new value.
    private static java.util.List<Tuple2<Key, String>> entries(Random random, int from, int to, int modulus) {
        java.util.List<Tuple2<Key, String>> result = new ArrayList<>();
        for (int id = from; id < to; id++) {
            result.add(entry(id, modulus));
        }
        java.util.Collections.shuffle(result, random);
        return result;
    }

    private static Tuple2<Key, String> entry(int id, int modulus) {
        int tag = TAGS.incrementAndGet();
        return Tuple.of(new Key(id, tag, modulus), new String("v" + tag));
    }

    private static java.util.List<Tuple2<Key, String>> listOf(Iterable<Tuple2<Key, String>> entries) {
        java.util.List<Tuple2<Key, String>> result = new ArrayList<>();
        entries.forEach(result::add);
        return result;
    }

    private static java.util.List<Object[]> identities(Map<Key, String> map) {
        java.util.List<Object[]> result = new ArrayList<>();
        map.forEach((k, v) -> result.add(new Object[] {k, v}));
        return result;
    }

    /// An iterable whose second `iterator()` call fails.
    private static <T> Iterable<T> oneShot(java.util.List<T> elements) {
        AtomicInteger calls = new AtomicInteger();
        return () -> {
            if (calls.incrementAndGet() > 1) {
                throw new IllegalStateException("iterated twice");
            }
            return elements.iterator();
        };
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
}
