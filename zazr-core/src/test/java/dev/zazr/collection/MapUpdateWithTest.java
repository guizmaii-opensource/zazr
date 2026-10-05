package dev.zazr.collection;

import dev.zazr.Tuple;
import dev.zazr.Tuple2;
import dev.zazr.control.Option;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

/// `updateWith` and the lazy `getOrElse` on every map type: present and absent keys, a put, a removal and no
/// change (checked by identity), at the trie boundaries and with colliding hashes, against `put` and `remove` on
/// the same receiver and against a `java.util.HashMap`. `f` and the supplier run once or never, the receiver never
/// changes, and a `LinkedHashMap` key keeps its position.
public class MapUpdateWithTest {

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
    private static final int[] COLLIDING_SIZES = {0, 1, 2, 31, 32, 33, 200};
    private static final int NO_COLLISION = Integer.MAX_VALUE;
    private static final AtomicInteger TAGS = new AtomicInteger();

    // -- the subjects

    /// A receiver type: how to build it, the plain map it is or wraps, and its two new methods, whose results are
    /// plain maps.
    private record Subject(
            String name,
            Function<java.util.List<Tuple2<Key, String>>, Object> build,
            Function<Object, Map<Key, String>> plain,
            UpdateWith updateWith,
            GetOrElse getOrElse,
            boolean nonEmpty) {}

    @FunctionalInterface
    private interface UpdateWith {
        Map<Key, String> apply(
                Object receiver, Key key, Function<? super Option<String>, ? extends Option<? extends String>> f);
    }

    @FunctionalInterface
    private interface GetOrElse {
        String apply(Object receiver, Key key, Supplier<? extends String> supplier);
    }

    @SuppressWarnings("unchecked")
    private static java.util.List<Subject> subjects() {
        Function<Object, Map<Key, String>> itself = map -> (Map<Key, String>) map;
        UpdateWith onMap = (r, k, f) -> ((Map<Key, String>) r).updateWith(k, f);
        GetOrElse getOnMap = (r, k, s) -> ((Map<Key, String>) r).getOrElse(k, s);
        return java.util.List.of(
                new Subject(
                        "HashMap",
                        HashMap::ofEntries,
                        itself,
                        (r, k, f) -> ((HashMap<Key, String>) r).updateWith(k, f),
                        getOnMap,
                        false),
                new Subject(
                        "LinkedHashMap",
                        LinkedHashMap::ofEntries,
                        itself,
                        (r, k, f) -> ((LinkedHashMap<Key, String>) r).updateWith(k, f),
                        getOnMap,
                        false),
                new Subject(
                        "TreeMap",
                        TreeMap::ofEntries,
                        itself,
                        (r, k, f) -> ((TreeMap<Key, String>) r).updateWith(k, f),
                        getOnMap,
                        false),
                new Subject(
                        "TreeMap reversed",
                        entries -> TreeMap.ofEntries(Comparator.<Key>reverseOrder(), entries),
                        itself,
                        (r, k, f) -> ((SortedMap<Key, String>) r).updateWith(k, f),
                        getOnMap,
                        false),
                new Subject("Map (HashMap)", HashMap::ofEntries, itself, onMap, getOnMap, false),
                new Subject(
                        "NonEmptyMap",
                        entries -> NonEmptyMap.fromIterable(entries).get(),
                        r -> ((NonEmptyMap<Key, String>) r).toMap(),
                        (r, k, f) -> ((NonEmptyMap<Key, String>) r).updateWith(k, f),
                        (r, k, s) -> ((NonEmptyMap<Key, String>) r).getOrElse(k, s),
                        true),
                new Subject(
                        "NonEmptySortedMap",
                        entries -> NonEmptySortedMap.fromIterable(entries).get(),
                        r -> ((NonEmptySortedMap<Key, String>) r).toSortedMap(),
                        (r, k, f) -> ((NonEmptySortedMap<Key, String>) r).updateWith(k, f),
                        (r, k, s) -> ((NonEmptySortedMap<Key, String>) r).getOrElse(k, s),
                        true));
    }

    private static Key key(int id, int modulus) {
        return new Key(id, TAGS.incrementAndGet(), modulus);
    }

    private static java.util.List<Tuple2<Key, String>> entries(int n, int modulus) {
        java.util.List<Tuple2<Key, String>> result = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            result.add(Tuple.of(key(i, modulus), "v" + i));
        }
        return result;
    }

    /// The key and value objects of `map`, in its iteration order.
    private static java.util.List<Object[]> identities(Map<Key, String> map) {
        java.util.List<Object[]> result = new ArrayList<>();
        for (Tuple2<Key, String> entry : map) {
            result.add(new Object[] {entry._1(), entry._2()});
        }
        return result;
    }

    private static void assertSameObjects(String scenario, Map<Key, String> expected, Map<Key, String> actual) {
        java.util.List<Object[]> want = identities(expected);
        java.util.List<Object[]> got = identities(actual);
        assertThat(got).as(scenario).hasSameSizeAs(want);
        for (int i = 0; i < want.size(); i++) {
            if (got.get(i)[0] != want.get(i)[0] || got.get(i)[1] != want.get(i)[1]) {
                throw new AssertionError(scenario + ": entry " + i + " is " + got.get(i)[0] + "=" + got.get(i)[1]
                        + ", expected " + want.get(i)[0] + "=" + want.get(i)[1]);
            }
        }
    }

    private static java.util.Map<Key, String> model(Map<Key, String> map) {
        java.util.Map<Key, String> result = new java.util.HashMap<>();
        for (Tuple2<Key, String> entry : map) {
            result.put(entry._1(), entry._2());
        }
        return result;
    }

    // -- the differential test

    @Test
    public void shouldPutRemoveOrKeepTheKeyAsFDecides() {
        for (int modulus : new int[] {NO_COLLISION, 7}) {
            for (int n : modulus == NO_COLLISION ? SIZES : COLLIDING_SIZES) {
                java.util.List<Tuple2<Key, String>> entries = entries(n, modulus);
                for (Subject subject : subjects()) {
                    if (subject.nonEmpty() && n == 0) {
                        continue;
                    }
                    Object receiver = subject.build().apply(entries);
                    Map<Key, String> plain = subject.plain().apply(receiver);
                    java.util.List<Object[]> before = identities(plain);
                    for (int id : new int[] {0, n / 2, n - 1, n, -1}) {
                        String scenario = subject.name() + ", modulus " + modulus + ", n " + n + ", key " + id;
                        check(scenario, subject, receiver, plain, id, modulus, id >= 0 && id < n);
                    }
                    // persistence: the receiver holds the same key and value objects, in the same order
                    assertSameObjects(
                            subject.name() + " receiver, n " + n,
                            plain,
                            subject.plain().apply(receiver));
                    assertThat(identities(plain)).hasSameSizeAs(before);
                }
            }
        }
    }

    private static void check(
            String scenario,
            Subject subject,
            Object receiver,
            Map<Key, String> plain,
            int id,
            int modulus,
            boolean present) {
        Key probe = key(id, modulus);
        String current = present ? plain.get(probe).get() : null;

        // f receives the Some of the value held (the same instance), or None, and runs once
        AtomicReference<Option<String>> received = new AtomicReference<>();
        AtomicInteger calls = new AtomicInteger();
        String replacement = "new" + id;
        Map<Key, String> put = subject.updateWith().apply(receiver, probe, previous -> {
            calls.incrementAndGet();
            received.set(previous);
            return Option.some(replacement);
        });
        assertThat(calls).as(scenario).hasValue(1);
        if (present) {
            assertThat(received.get().get()).as(scenario).isSameAs(current);
        } else {
            assertThat(received.get()).as(scenario).isSameAs(Option.none());
        }
        assertSameObjects(scenario + ", put", plain.put(probe, replacement), put);
        java.util.Map<Key, String> expectedPut = model(plain);
        expectedPut.put(probe, replacement);
        assertThat(put.asJavaMap()).as(scenario).isEqualTo(expectedPut);

        // None removes a present key, and keeps an absent one absent: the same map back
        Map<Key, String> removed = subject.updateWith().apply(receiver, probe, previous -> Option.none());
        if (present) {
            assertSameObjects(scenario + ", remove", plain.remove(probe), removed);
            java.util.Map<Key, String> expectedRemoved = model(plain);
            expectedRemoved.remove(probe);
            assertThat(removed.asJavaMap()).as(scenario).isEqualTo(expectedRemoved);
        } else {
            assertThat(removed).as(scenario).isSameAs(plain);
        }

        // the Option it was given back: no change, the same map back, the key object held kept
        Map<Key, String> same = subject.updateWith().apply(receiver, probe, previous -> previous);
        assertThat(same).as(scenario).isSameAs(plain);

        // an equal value that is another instance is put, as Scala's eq test
        if (present) {
            String copy = new String(current);
            Map<Key, String> equal = subject.updateWith().apply(receiver, probe, previous -> Option.some(copy));
            assertThat(equal).as(scenario).isNotSameAs(plain).isEqualTo(plain);
            assertSameObjects(scenario + ", equal value", plain.put(probe, copy), equal);
        }

        // the lazy getOrElse: the value held without calling the supplier, or the supplier's value after one call
        AtomicInteger supplied = new AtomicInteger();
        String got = subject.getOrElse().apply(receiver, probe, () -> {
            supplied.incrementAndGet();
            return "default";
        });
        if (present) {
            assertThat(got).as(scenario).isSameAs(current);
            assertThat(supplied).as(scenario).hasValue(0);
        } else {
            assertThat(got).as(scenario).isEqualTo("default");
            assertThat(supplied).as(scenario).hasValue(1);
        }
    }

    // -- the key object

    @Test
    public void shouldTakeTheKeyObjectGivenWhenPuttingAndKeepTheOneHeldWhenNothingChanges() {
        for (Subject subject : subjects()) {
            Key held = key(1, NO_COLLISION);
            Object receiver = subject.build().apply(java.util.List.of(Tuple.of(held, "a"), Tuple.of(key(2, 7), "b")));
            Map<Key, String> plain = subject.plain().apply(receiver);
            Key equal = key(1, NO_COLLISION);
            Map<Key, String> put = subject.updateWith().apply(receiver, equal, previous -> Option.some("c"));
            assertThat(put.keySet().find(k -> k.id == 1).get())
                    .as(subject.name())
                    .isSameAs(equal);
            Map<Key, String> same = subject.updateWith().apply(receiver, equal, previous -> previous);
            assertThat(same).as(subject.name()).isSameAs(plain);
            assertThat(same.keySet().find(k -> k.id == 1).get())
                    .as(subject.name())
                    .isSameAs(held);
        }
    }

    // -- the last key

    @Test
    public void shouldGiveAnEmptyMapWhenTheLastKeyIsRemoved() {
        for (Subject subject : subjects()) {
            Object receiver = subject.build().apply(java.util.List.of(Tuple.of(key(1, NO_COLLISION), "a")));
            Map<Key, String> removed =
                    subject.updateWith().apply(receiver, key(1, NO_COLLISION), previous -> Option.none());
            assertThat(removed).as(subject.name()).isEmpty();
            assertThat(removed.getClass())
                    .as(subject.name())
                    .isEqualTo(subject.plain().apply(receiver).getClass());
            // and the receiver still holds its entry
            assertThat(subject.plain().apply(receiver)).as(subject.name()).hasSize(1);
        }
    }

    @Test
    public void shouldReturnTheMapsOwnTypes() {
        HashMap<String, Integer> hash = HashMap.of("a", 1).updateWith("b", count -> Option.some(2));
        LinkedHashMap<String, Integer> linked = LinkedHashMap.of("a", 1).updateWith("b", count -> Option.some(2));
        TreeMap<String, Integer> tree = TreeMap.of("a", 1).updateWith("b", count -> Option.some(2));
        SortedMap<String, Integer> sorted =
                ((SortedMap<String, Integer>) TreeMap.of("a", 1)).updateWith("b", count -> Option.some(2));
        HashMap<String, Integer> fromNonEmpty =
                NonEmptyMap.of(Tuple.of("a", 1)).updateWith("a", count -> Option.none());
        TreeMap<String, Integer> fromNonEmptySorted =
                NonEmptySortedMap.of(Tuple.of("a", 1)).updateWith("a", count -> Option.none());
        assertThat(hash).isEqualTo(HashMap.of("a", 1, "b", 2));
        assertThat(linked).isEqualTo(LinkedHashMap.of("a", 1, "b", 2));
        assertThat(tree).isEqualTo(TreeMap.of("a", 1, "b", 2));
        assertThat(sorted).isEqualTo(TreeMap.of("a", 1, "b", 2));
        assertThat(fromNonEmpty).isEqualTo(HashMap.empty());
        assertThat(fromNonEmptySorted).isEqualTo(TreeMap.empty());
    }

    @Test
    public void shouldGiveTheWrappedMapItselfWhenANonEmptyMapDoesNotChange() {
        NonEmptyMap<String, Integer> nonEmpty = NonEmptyMap.of(Tuple.of("a", 1), Tuple.of("b", 2));
        assertThat(nonEmpty.updateWith("a", count -> count)).isSameAs(nonEmpty.toMap());
        assertThat(nonEmpty.updateWith("z", count -> Option.none())).isSameAs(nonEmpty.toMap());
        NonEmptySortedMap<String, Integer> sorted = NonEmptySortedMap.of(Tuple.of("a", 1), Tuple.of("b", 2));
        assertThat(sorted.updateWith("a", count -> count)).isSameAs(sorted.toSortedMap());
        assertThat(sorted.updateWith("z", count -> Option.none())).isSameAs(sorted.toSortedMap());
    }

    // -- LinkedHashMap order

    @Test
    public void shouldKeepThePositionOfAnUpdatedKeyInALinkedHashMap() {
        LinkedHashMap<String, Integer> map = LinkedHashMap.of("c", 1, "a", 2, "b", 3);
        assertThat(map.updateWith("a", n -> Option.some(n.get() * 10)).keySet().toList())
                .containsExactly("c", "a", "b");
        assertThat(map.updateWith("c", n -> Option.some(0)).toList())
                .containsExactly(Tuple.of("c", 0), Tuple.of("a", 2), Tuple.of("b", 3));
        assertThat(map.updateWith("b", n -> Option.some(0)).toList())
                .containsExactly(Tuple.of("c", 1), Tuple.of("a", 2), Tuple.of("b", 0));
        assertThat(map.updateWith("d", n -> Option.some(4)).keySet().toList()).containsExactly("c", "a", "b", "d");
        assertThat(map.updateWith("a", n -> Option.none()).keySet().toList()).containsExactly("c", "b");
        // removed, then added back: at the end
        LinkedHashMap<String, Integer> removed = map.updateWith("c", n -> Option.none());
        assertThat(removed.updateWith("c", n -> Option.some(9)).keySet().toList())
                .containsExactly("a", "b", "c");
    }

    @Test
    public void shouldKeepPositionsInALinkedHashMapWithGapsAndInItsOlderVersions() {
        LinkedHashMap<Integer, String> full =
                LinkedHashMap.ofEntries(Vector.range(0, 40).map(i -> Tuple.of(i, "v" + i)));
        java.util.List<Integer> fullKeys = list(Vector.range(0, 40));
        // gaps in the insertion order, from removals on a newer version
        LinkedHashMap<Integer, String> gaps = Vector.range(0, 40)
                .filter(i -> i % 3 == 0)
                .foldLeft(full, (map, i) -> map.updateWith(i, v -> Option.none()));
        java.util.List<Integer> gapKeys = list(gaps.keySet());
        assertThat(gapKeys).hasSize(26);
        LinkedHashMap<Integer, String> updated = gaps.updateWith(20, v -> Option.some("x"));
        assertThat(list(updated.keySet())).isEqualTo(gapKeys);
        assertThat(updated.get(20)).isEqualTo(Option.some("x"));
        LinkedHashMap<Integer, String> added = gaps.updateWith(0, v -> Option.some("back"));
        java.util.List<Integer> addedKeys = new ArrayList<>(gapKeys);
        addedKeys.add(0);
        assertThat(list(added.keySet())).isEqualTo(addedKeys);
        // the older version is untouched, and updates on it keep its own order
        assertThat(list(full.keySet())).isEqualTo(fullKeys);
        assertThat(list(full.updateWith(3, v -> Option.some("y")).keySet())).isEqualTo(fullKeys);
        assertThat(full.get(3)).isEqualTo(Option.some("v3"));
    }

    private static <T> java.util.List<T> list(Iterable<T> elements) {
        java.util.List<T> result = new ArrayList<>();
        elements.forEach(result::add);
        return result;
    }

    // -- f and the supplier

    @Test
    public void shouldLeaveTheMapUnchangedWhenFThrows() {
        HashMap<String, Integer> map = HashMap.of("a", 1);
        IllegalStateException boom = new IllegalStateException("boom");
        assertThatThrownBy(() -> map.updateWith("a", n -> {
                    throw boom;
                }))
                .isSameAs(boom);
        assertThat(map).isEqualTo(HashMap.of("a", 1));
    }

    @Test
    public void shouldAcceptAWideningFunction() {
        Function<Option<?>, Option<String>> describe = previous -> Option.some(previous.isEmpty() ? "none" : "some");
        HashMap<Integer, CharSequence> map = HashMap.of(1, "x");
        assertThat(map.updateWith(1, describe)).isEqualTo(HashMap.of(1, "some"));
        assertThat(map.updateWith(2, describe)).isEqualTo(HashMap.of(1, "x", 2, "none"));
    }

    @Test
    public void shouldKeepTheEagerGetOrElse() {
        HashMap<String, Integer> map = HashMap.of("a", 1);
        assertThat(map.getOrElse("a", 0)).isEqualTo(1);
        assertThat(map.getOrElse("b", 0)).isEqualTo(0);
        assertThat(map.getOrElse("b", (Integer) null)).isNull();
        assertThat(map.getOrElse("b", () -> 7)).isEqualTo(7);
    }

    // -- nulls

    @Test
    public void shouldRejectANullFOrSupplierWhateverTheMapHolds() {
        for (Subject subject : subjects()) {
            for (int n : new int[] {0, 1}) {
                if (subject.nonEmpty() && n == 0) {
                    continue;
                }
                Object receiver = subject.build().apply(entries(n, NO_COLLISION));
                for (int id : new int[] {0, 1}) {
                    Key probe = key(id, NO_COLLISION);
                    String scenario = subject.name() + ", n " + n + ", key " + id;
                    assertThatNullPointerException()
                            .as(scenario)
                            .isThrownBy(() -> subject.updateWith().apply(receiver, probe, null))
                            .withMessage("f is null");
                    assertThatNullPointerException()
                            .as(scenario)
                            .isThrownBy(() -> subject.getOrElse().apply(receiver, probe, null))
                            .withMessage("supplier is null");
                }
            }
        }
    }

    @Test
    public void shouldRejectANullResultNamingTheType() {
        for (Subject subject : subjects()) {
            Object receiver = subject.build().apply(entries(2, NO_COLLISION));
            Map<Key, String> plain = subject.plain().apply(receiver);
            java.util.List<Object[]> before = identities(plain);
            String type = subject.name().startsWith("TreeMap")
                    ? "TreeMap"
                    : subject.name().startsWith("Map") ? "HashMap" : subject.name();
            for (int id : new int[] {0, 5}) {
                Key probe = key(id, NO_COLLISION);
                assertThatNullPointerException()
                        .as(subject.name())
                        .isThrownBy(() -> subject.updateWith().apply(receiver, probe, previous -> null))
                        .withMessage(type + ".updateWith: f returned null");
            }
            assertThatNullPointerException()
                    .as(subject.name())
                    .isThrownBy(() -> subject.getOrElse().apply(receiver, key(5, NO_COLLISION), () -> null))
                    .withMessage(type + ".getOrElse: supplier returned null");
            // a supplier that would return null is not called for a present key
            assertThat(subject.getOrElse().apply(receiver, key(0, NO_COLLISION), () -> null))
                    .isEqualTo("v0");
            assertSameObjects(subject.name(), plain, subject.plain().apply(receiver));
            assertThat(identities(plain)).hasSameSizeAs(before);
        }
    }

    @Test
    public void shouldTreatANullKeyAsPutRemoveAndGetDo() {
        HashMap<String, Integer> hash = HashMap.of("a", 1);
        LinkedHashMap<String, Integer> linked = LinkedHashMap.of("a", 1);
        for (Map<String, Integer> map : java.util.List.<Map<String, Integer>>of(hash, linked)) {
            // the lookup of null finds nothing: Some is a put, which rejects the key; None leaves the map as it is
            Throwable expected = catchThrowable(() -> map.put(null, 2));
            assertThat(expected).as(map.toString()).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> map.updateWith(null, n -> Option.some(2)))
                    .as(map.toString())
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage(expected.getMessage());
            assertThat(map.remove(null)).as(map.toString()).isSameAs(map);
            assertThat(map.updateWith(null, n -> Option.none()))
                    .as(map.toString())
                    .isSameAs(map);
            assertThat(map.getOrElse(null, () -> 9)).as(map.toString()).isEqualTo(9);
        }
        // a TreeMap cannot look null up in its natural order: the lookup throws, as get does, before f runs. The
        // NPE comes from the JVM inside compareTo, so only its type is compared: once the JIT has compiled that
        // path, the JVM may throw a preallocated NPE without a message
        TreeMap<String, Integer> tree = TreeMap.of("a", 1);
        assertThatThrownBy(() -> tree.get(null)).isInstanceOf(NullPointerException.class);
        AtomicInteger calls = new AtomicInteger();
        assertThatThrownBy(() -> tree.updateWith(null, n -> {
                    calls.incrementAndGet();
                    return Option.none();
                }))
                .isInstanceOf(NullPointerException.class);
        assertThat(calls).hasValue(0);
        assertThatThrownBy(() -> tree.getOrElse(null, () -> 9)).isInstanceOf(NullPointerException.class);
    }
}
