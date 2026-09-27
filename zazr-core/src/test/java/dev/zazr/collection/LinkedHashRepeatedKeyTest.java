package dev.zazr.collection;

import dev.zazr.Tuple;
import dev.zazr.Tuple2;
import dev.zazr.control.Either;
import dev.zazr.control.Option;
import java.util.ArrayList;
import java.util.Random;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Every factory, builder, collector and bulk operation of `LinkedHashMap` and `LinkedHashSet` gives the map or the set
/// that successive `put`s or `add`s give: a repeated key keeps the position of its first occurrence and takes the key
/// object and the value of its last; a repeated element keeps its first position and its first object. The keys are
/// equal but not identical, so the comparison checks which object is kept.
public class LinkedHashRepeatedKeyTest {

    /// Equal by `id` alone; `tag` tells equal keys apart.
    static final class Key {
        final int id;
        final String tag;

        Key(int id, String tag) {
            this.id = id;
            this.tag = tag;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Key that && that.id == id;
        }

        @Override
        public int hashCode() {
            return id;
        }

        @Override
        public String toString() {
            return id + "/" + tag;
        }
    }

    private static final int[] SIZES = {0, 1, 2, 3, 10, 31, 32, 33, 70};
    private static final int[] DISTINCT_IDS = {1, 2, 5, 40};

    /// Random inputs with repeated keys; with `sharedValues`, the values come from a small pool, so that equal keys
    /// often carry the identical value object.
    private static java.util.List<java.util.List<Tuple2<Key, String>>> inputs(boolean sharedValues) {
        Random random = new Random(42);
        String[] pool = {new String("x"), new String("y")};
        java.util.List<java.util.List<Tuple2<Key, String>>> inputs = new ArrayList<>();
        for (int size : SIZES) {
            for (int ids : DISTINCT_IDS) {
                for (int round = 0; round < 3; round++) {
                    java.util.List<Tuple2<Key, String>> entries = new ArrayList<>();
                    for (int i = 0; i < size; i++) {
                        String value = sharedValues ? pool[random.nextInt(pool.length)] : new String("v" + i);
                        entries.add(Tuple.of(new Key(random.nextInt(ids), "t" + i), value));
                    }
                    inputs.add(entries);
                }
            }
        }
        return inputs;
    }

    private static java.util.List<java.util.List<Tuple2<Key, String>>> allInputs() {
        java.util.List<java.util.List<Tuple2<Key, String>>> all = new ArrayList<>(inputs(false));
        all.addAll(inputs(true));
        return all;
    }

    private static java.util.List<Key> keys(java.util.List<Tuple2<Key, String>> entries) {
        java.util.List<Key> keys = new ArrayList<>();
        entries.forEach(entry -> keys.add(entry._1()));
        return keys;
    }

    private static LinkedHashMap<Key, String> puts(Iterable<Tuple2<Key, String>> entries) {
        return Vector.ofAll(entries)
                .foldLeft(LinkedHashMap.<Key, String>empty(), (map, entry) -> map.put(entry._1(), entry._2()));
    }

    private static <T> java.util.List<T> javaList(Iterable<T> elements) {
        java.util.List<T> result = new ArrayList<>();
        elements.forEach(result::add);
        return result;
    }

    private static LinkedHashSet<Key> adds(Iterable<Key> elements) {
        return addEach(LinkedHashSet.empty(), elements);
    }

    /// An iterable that can be iterated once.
    private static <T> Iterable<T> oneShot(java.util.List<T> elements) {
        boolean[] used = {false};
        return () -> {
            assertThat(used[0]).as("iterated twice").isFalse();
            used[0] = true;
            return elements.iterator();
        };
    }

    // -- the comparison: the same entries in the same order, the same objects, the same positional behaviour

    private static void assertSameMap(
            String what, LinkedHashMap<Key, String> actual, LinkedHashMap<Key, String> expected) {
        List<Tuple2<Key, String>> actualEntries = actual.toList();
        List<Tuple2<Key, String>> expectedEntries = expected.toList();
        assertThat(actualEntries).as(what).isEqualTo(expectedEntries);
        for (int i = 0; i < expectedEntries.size(); i++) {
            assertThat(actualEntries.get(i)._1())
                    .as(what + ": key object at " + i)
                    .isSameAs(expectedEntries.get(i)._1());
            assertThat(actualEntries.get(i)._2())
                    .as(what + ": value object at " + i)
                    .isSameAs(expectedEntries.get(i)._2());
        }
        assertSameKeys(
                what + ": keySet", actual.keySet().toList(), expected.keySet().toList());
        assertSameKeys(
                what + ": keySet order",
                ((LinkedHashSet<Key>) actual.keySet())
                        .zipWithIndex()
                        .map(Tuple2::_1)
                        .toList(),
                ((LinkedHashSet<Key>) expected.keySet())
                        .zipWithIndex()
                        .map(Tuple2::_1)
                        .toList());
        // the built map is a regular map: removing, slicing and putting again agree with the reference
        for (Tuple2<Key, String> entry : expectedEntries) {
            assertThat(actual.remove(entry._1()).toList())
                    .as(what + ": remove " + entry._1())
                    .isEqualTo(expected.remove(entry._1()).toList());
        }
        for (int n = 0; n <= expectedEntries.size(); n++) {
            assertThat(actual.take(n).toList())
                    .as(what + ": take " + n)
                    .isEqualTo(expected.take(n).toList());
            assertThat(actual.drop(n).toList())
                    .as(what + ": drop " + n)
                    .isEqualTo(expected.drop(n).toList());
        }
        Key fresh = new Key(-1, "fresh");
        assertThat(actual.put(fresh, "f").toList())
                .as(what + ": put")
                .isEqualTo(expected.put(fresh, "f").toList());
    }

    private static void assertSameKeys(String what, List<Key> actual, List<Key> expected) {
        assertThat(actual).as(what).isEqualTo(expected);
        for (int i = 0; i < expected.size(); i++) {
            assertThat(actual.get(i)).as(what + ": object at " + i).isSameAs(expected.get(i));
        }
    }

    private static void assertSameSet(String what, LinkedHashSet<Key> actual, LinkedHashSet<Key> expected) {
        assertSameKeys(what, actual.toList(), expected.toList());
        assertSameKeys(
                what + ": zipWithIndex",
                actual.zipWithIndex().map(Tuple2::_1).toList(),
                expected.zipWithIndex().map(Tuple2::_1).toList());
        for (Key element : expected) {
            assertThat(actual.remove(element).toList())
                    .as(what + ": remove " + element)
                    .isEqualTo(expected.remove(element).toList());
        }
        for (int n = 0; n <= expected.size(); n++) {
            assertThat(actual.take(n).toList())
                    .as(what + ": take " + n)
                    .isEqualTo(expected.take(n).toList());
        }
    }

    // -- LinkedHashMap

    @SuppressWarnings("unchecked")
    private static Tuple2<Key, String>[] array(java.util.List<Tuple2<Key, String>> entries) {
        return entries.toArray(new Tuple2[0]);
    }

    @SuppressWarnings("unchecked")
    private static java.util.Map.Entry<Key, String>[] javaEntries(java.util.List<Tuple2<Key, String>> entries) {
        java.util.Map.Entry<Key, String>[] array = new java.util.Map.Entry[entries.size()];
        for (int i = 0; i < array.length; i++) {
            array[i] = java.util.Map.entry(entries.get(i)._1(), entries.get(i)._2());
        }
        return array;
    }

    @Test
    public void mapFactoriesAgreeWithSuccessivePuts() {
        for (java.util.List<Tuple2<Key, String>> entries : allInputs()) {
            LinkedHashMap<Key, String> expected = puts(entries);
            String input = entries.toString();
            assertSameMap("ofEntries(Tuple2...) " + input, LinkedHashMap.ofEntries(array(entries)), expected);
            assertSameMap("ofEntries(Map.Entry...) " + input, LinkedHashMap.ofEntries(javaEntries(entries)), expected);
            assertSameMap("ofEntries(java.util.List) " + input, LinkedHashMap.ofEntries(entries), expected);
            assertSameMap("ofEntries(List) " + input, LinkedHashMap.ofEntries(List.ofAll(entries)), expected);
            assertSameMap("ofEntries(one-shot) " + input, LinkedHashMap.ofEntries(oneShot(entries)), expected);
            assertSameMap("collector() " + input, entries.stream().collect(LinkedHashMap.collector()), expected);
            assertSameMap(
                    "parallel collector() " + input,
                    entries.parallelStream().collect(LinkedHashMap.collector()),
                    expected);
            assertSameMap(
                    "collector(key, value) " + input,
                    entries.parallelStream()
                            .collect(LinkedHashMap.<Key, String, Tuple2<Key, String>>collector(Tuple2::_1, Tuple2::_2)),
                    expected);
            assertSameMap("tabulate " + input, LinkedHashMap.tabulate(entries.size(), entries::get), expected);
            java.util.Iterator<Tuple2<Key, String>> supplied = entries.iterator();
            assertSameMap("fill " + input, LinkedHashMap.fill(entries.size(), supplied::next), expected);
            assertSameMap(
                    "ofAll(LazyList, entryMapper) " + input,
                    LinkedHashMap.ofAll(entries.stream(), Function.identity()),
                    expected);
            assertSameMap(
                    "ofAll(LazyList, key, value) " + input,
                    LinkedHashMap.ofAll(entries.stream(), Tuple2::_1, Tuple2::_2),
                    expected);
            assertSameMap("orElse " + input, LinkedHashMap.<Key, String>empty().orElse(entries), expected);
            assertSameMap(
                    "orElse(Supplier) " + input,
                    LinkedHashMap.<Key, String>empty().orElse(() -> entries),
                    expected);
            if (entries.size() >= 1 && entries.size() <= 10) {
                assertSameMap("of arity " + entries.size() + " " + input, ofArity(entries), expected);
            }
        }
    }

    @Test
    public void mapFixedArityFactoriesAgreeWithSuccessivePuts() {
        Random random = new Random(7);
        for (int arity = 1; arity <= 10; arity++) {
            for (int ids = 1; ids <= arity; ids++) {
                for (int round = 0; round < 5; round++) {
                    java.util.List<Tuple2<Key, String>> entries = new ArrayList<>();
                    for (int i = 0; i < arity; i++) {
                        entries.add(Tuple.of(new Key(random.nextInt(ids), "t" + i), new String("v" + i)));
                    }
                    assertSameMap("of " + entries, ofArity(entries), puts(entries));
                }
            }
        }
    }

    private static LinkedHashMap<Key, String> ofArity(java.util.List<Tuple2<Key, String>> e) {
        return switch (e.size()) {
            case 1 -> LinkedHashMap.of(k(e, 0), v(e, 0));
            case 2 -> LinkedHashMap.of(k(e, 0), v(e, 0), k(e, 1), v(e, 1));
            case 3 -> LinkedHashMap.of(k(e, 0), v(e, 0), k(e, 1), v(e, 1), k(e, 2), v(e, 2));
            case 4 -> LinkedHashMap.of(k(e, 0), v(e, 0), k(e, 1), v(e, 1), k(e, 2), v(e, 2), k(e, 3), v(e, 3));
            case 5 ->
                LinkedHashMap.of(
                        k(e, 0), v(e, 0), k(e, 1), v(e, 1), k(e, 2), v(e, 2), k(e, 3), v(e, 3), k(e, 4), v(e, 4));
            case 6 ->
                LinkedHashMap.of(
                        k(e, 0), v(e, 0), k(e, 1), v(e, 1), k(e, 2), v(e, 2), k(e, 3), v(e, 3), k(e, 4), v(e, 4),
                        k(e, 5), v(e, 5));
            case 7 ->
                LinkedHashMap.of(
                        k(e, 0), v(e, 0), k(e, 1), v(e, 1), k(e, 2), v(e, 2), k(e, 3), v(e, 3), k(e, 4), v(e, 4),
                        k(e, 5), v(e, 5), k(e, 6), v(e, 6));
            case 8 ->
                LinkedHashMap.of(
                        k(e, 0), v(e, 0), k(e, 1), v(e, 1), k(e, 2), v(e, 2), k(e, 3), v(e, 3), k(e, 4), v(e, 4),
                        k(e, 5), v(e, 5), k(e, 6), v(e, 6), k(e, 7), v(e, 7));
            case 9 ->
                LinkedHashMap.of(
                        k(e, 0), v(e, 0), k(e, 1), v(e, 1), k(e, 2), v(e, 2), k(e, 3), v(e, 3), k(e, 4), v(e, 4),
                        k(e, 5), v(e, 5), k(e, 6), v(e, 6), k(e, 7), v(e, 7), k(e, 8), v(e, 8));
            case 10 ->
                LinkedHashMap.of(
                        k(e, 0), v(e, 0), k(e, 1), v(e, 1), k(e, 2), v(e, 2), k(e, 3), v(e, 3), k(e, 4), v(e, 4),
                        k(e, 5), v(e, 5), k(e, 6), v(e, 6), k(e, 7), v(e, 7), k(e, 8), v(e, 8), k(e, 9), v(e, 9));
            default -> throw new IllegalArgumentException("arity " + e.size());
        };
    }

    private static Key k(java.util.List<Tuple2<Key, String>> entries, int i) {
        return entries.get(i)._1();
    }

    private static String v(java.util.List<Tuple2<Key, String>> entries, int i) {
        return entries.get(i)._2();
    }

    @Test
    public void mapSingletonFactoriesAgreeWithPut() {
        Key key = new Key(1, "a");
        String value = new String("v");
        LinkedHashMap<Key, String> expected = LinkedHashMap.<Key, String>empty().put(key, value);
        assertSameMap("of(key, value)", LinkedHashMap.of(key, value), expected);
        assertSameMap("of(entry)", LinkedHashMap.of(Tuple.of(key, value)), expected);
    }

    /// Bulk operations that can map several entries to one key agree with putting the mapped entries one by one.
    @Test
    public void mapBulkOperationsAgreeWithSuccessivePuts() {
        Function<Key, Key> collapse = key -> new Key(key.id / 2, key.tag + "'");
        for (java.util.List<Tuple2<Key, String>> entries : allInputs()) {
            LinkedHashMap<Key, String> source = puts(entries);
            String input = entries.toString();
            // the mapped keys are created once, so that the reference and the operation see the same objects
            java.util.Map<Key, Key> mappedKeys = new java.util.IdentityHashMap<>();
            source.forEach(entry -> mappedKeys.put(entry._1(), collapse.apply(entry._1())));
            java.util.List<Tuple2<Key, String>> mapped = new ArrayList<>();
            source.forEach(entry -> mapped.add(Tuple.of(mappedKeys.get(entry._1()), entry._2())));
            LinkedHashMap<Key, String> expected = puts(mapped);

            assertSameMap("mapBoth " + input, source.mapBoth(mappedKeys::get, Function.identity()), expected);
            assertSameMap("mapKeys " + input, source.mapKeys(mappedKeys::get), expected);
            assertSameMap("map " + input, source.map((key, value) -> Tuple.of(mappedKeys.get(key), value)), expected);
            assertSameMap(
                    "flatMap " + input,
                    source.flatMap((key, value) -> List.of(Tuple.of(mappedKeys.get(key), value))),
                    expected);
            assertSameMap(
                    "collect " + input,
                    source.collect((key, value) -> Option.some(Tuple.of(mappedKeys.get(key), value))),
                    expected);

            LinkedHashMap<Key, String> merged = Vector.ofAll(mapped)
                    .foldLeft(
                            LinkedHashMap.<Key, String>empty(),
                            (acc, entry) -> acc.put(entry._1(), entry._2(), (a, b) -> a));
            assertSameMap("mapKeys(merge) " + input, source.mapKeys(mappedKeys::get, (a, b) -> a), merged);
        }
    }

    @Test
    public void mapMergeAgreesWithSuccessivePuts() {
        java.util.List<java.util.List<Tuple2<Key, String>>> inputs = allInputs();
        for (int i = 0; i + 1 < inputs.size(); i += 2) {
            LinkedHashMap<Key, String> left = puts(inputs.get(i));
            LinkedHashMap<Key, String> right = puts(inputs.get(i + 1));
            String input = left + " " + right;

            LinkedHashMap<Key, String> kept = right.foldLeft(
                    left, (acc, entry) -> acc.containsKey(entry._1()) ? acc : acc.put(entry._1(), entry._2()));
            assertSameMap("merge " + input, left.merge(right), kept);
            assertSameMap(
                    "merge into empty " + input,
                    LinkedHashMap.<Key, String>empty().merge(right),
                    right);

            LinkedHashMap<Key, String> resolved =
                    right.foldLeft(left, (acc, entry) -> acc.put(entry._1(), entry._2(), (a, b) -> a));
            assertSameMap("merge(resolution) " + input, left.merge(right, (a, b) -> a), resolved);
        }
    }

    /// `put` on an existing key keeps its position and takes the given key object, in every view of the key.
    @Test
    public void putOfAnEqualKeyReplacesTheKeyObjectInPlace() {
        Key first = new Key(1, "first");
        Key second = new Key(1, "second");
        Key other = new Key(2, "other");
        LinkedHashMap<Key, String> map = LinkedHashMap.<Key, String>empty()
                .put(first, "a")
                .put(other, "b")
                .put(second, "c");
        assertThat(map.toList()).isEqualTo(List.of(Tuple.of(first, "c"), Tuple.of(other, "b")));
        assertThat(map.head()._1()).isSameAs(second);
        assertThat(((LinkedHashSet<Key>) map.keySet()).head()).isSameAs(second);
        assertThat(((LinkedHashSet<Key>) map.keySet()).zipWithIndex().head()._1())
                .isSameAs(second);
        assertThat(((LinkedHashSet<Key>) map.keySet())
                        .takeWhile(key -> key.tag.equals("second"))
                        .toList())
                .containsExactly(second);
        assertThat(map.remove(other).head()._1()).isSameAs(second);
    }

    /// The builder, fed one entry at a time, in bulk, or after adopting a map (with or without removals), gives the
    /// map that successive puts give.
    @Test
    public void mapBuilderAgreesWithSuccessivePuts() {
        for (java.util.List<Tuple2<Key, String>> entries : allInputs()) {
            LinkedHashMap<Key, String> expected = puts(entries);
            String input = entries.toString();
            LinkedHashMap.Builder<Key, String> byKeyValue = LinkedHashMap.newBuilder();
            LinkedHashMap.Builder<Key, String> byEntry = LinkedHashMap.newBuilder();
            LinkedHashMap.Builder<Key, String> byPutAll = LinkedHashMap.newBuilder();
            for (Tuple2<Key, String> entry : entries) {
                byKeyValue.put(entry._1(), entry._2());
                byEntry.put(entry);
                byPutAll.putAll(java.util.List.of(entry));
            }
            assertSameMap("builder put(key, value) " + input, byKeyValue.result(), expected);
            assertSameMap("builder put(entry) " + input, byEntry.result(), expected);
            assertSameMap("builder putAll(singletons) " + input, byPutAll.result(), expected);
            assertSameMap(
                    "builder putAll(one-shot) " + input,
                    LinkedHashMap.<Key, String>newBuilder()
                            .putAll(oneShot(entries))
                            .result(),
                    expected);

            for (int split = 0; split <= entries.size(); split += Math.max(1, entries.size() / 4)) {
                java.util.List<Tuple2<Key, String>> prefix = entries.subList(0, split);
                java.util.List<Tuple2<Key, String>> suffix = entries.subList(split, entries.size());
                LinkedHashMap<Key, String> prefixMap = puts(prefix);
                java.util.List<Tuple2<Key, String>> prefixBefore = javaList(prefixMap);
                // adopted, then extended
                assertSameMap(
                        "builder adopting at " + split + " " + input,
                        LinkedHashMap.<Key, String>newBuilder()
                                .putAll(prefixMap)
                                .putAll(suffix)
                                .result(),
                        expected);
                assertSameMap(
                        "builder adopting the view at " + split + " " + input,
                        LinkedHashMap.<Key, String>newBuilder()
                                .putAll(prefixMap.asJava())
                                .putAll(suffix)
                                .result(),
                        expected);
                // a map put into a builder that is not empty is put entry by entry
                LinkedHashMap<Key, String> suffixMap = puts(suffix);
                LinkedHashMap<Key, String> both =
                        suffixMap.foldLeft(prefixMap, (acc, entry) -> acc.put(entry._1(), entry._2()));
                assertSameMap(
                        "builder putAll(map) after entries at " + split + " " + input,
                        LinkedHashMap.<Key, String>newBuilder()
                                .putAll(prefix)
                                .putAll(suffixMap)
                                .result(),
                        both);
                assertSameMap(
                        "builder putAll(map) twice at " + split + " " + input,
                        LinkedHashMap.<Key, String>newBuilder()
                                .putAll(prefixMap)
                                .putAll(suffixMap)
                                .result(),
                        both);
                assertThat(javaList(prefixMap)).as("adopted map unchanged").isEqualTo(prefixBefore);
            }

            // an adopted map with removals: the builder extends it as successive puts would, markers and all
            if (!expected.isEmpty()) {
                java.util.List<Tuple2<Key, String>> kept = javaList(expected);
                LinkedHashMap<Key, String> first = expected.remove(kept.get(0)._1());
                LinkedHashMap<Key, String> removed =
                        kept.size() > 2 ? first.remove(kept.get(kept.size() / 2)._1()) : first;
                LinkedHashMap<Key, String> extended =
                        Vector.ofAll(entries).foldLeft(removed, (acc, entry) -> acc.put(entry._1(), entry._2()));
                assertSameMap(
                        "builder adopting a map with removals " + input,
                        LinkedHashMap.<Key, String>newBuilder()
                                .putAll(removed)
                                .putAll(entries)
                                .result(),
                        extended);
            }
        }
    }

    // -- LinkedHashSet

    @Test
    public void setFactoriesAgreeWithSuccessiveAdds() {
        for (java.util.List<Tuple2<Key, String>> entries : inputs(false)) {
            java.util.List<Key> elements = keys(entries);
            LinkedHashSet<Key> expected = adds(elements);
            String input = elements.toString();
            assertSameSet("of(T...) " + input, LinkedHashSet.of(elements.toArray(new Key[0])), expected);
            assertSameSet("ofAll(java.util.List) " + input, LinkedHashSet.ofAll(elements), expected);
            assertSameSet("ofAll(List) " + input, LinkedHashSet.ofAll(List.ofAll(elements)), expected);
            assertSameSet("ofAll(one-shot) " + input, LinkedHashSet.ofAll(oneShot(elements)), expected);
            assertSameSet("ofAll(LazyList) " + input, LinkedHashSet.ofAll(elements.stream()), expected);
            assertSameSet("collector " + input, elements.stream().collect(LinkedHashSet.collector()), expected);
            assertSameSet(
                    "parallel collector " + input,
                    elements.parallelStream().collect(LinkedHashSet.collector()),
                    expected);
            assertSameSet("tabulate " + input, LinkedHashSet.tabulate(elements.size(), elements::get), expected);
            java.util.Iterator<Key> supplied = elements.iterator();
            assertSameSet("fill " + input, LinkedHashSet.fill(elements.size(), supplied::next), expected);
            int half = elements.size() / 2;
            assertSameSet(
                    "flatten " + input,
                    LinkedHashSet.flatten(
                            java.util.List.of(elements.subList(0, half), elements.subList(half, elements.size()))),
                    expected);
            if (elements.size() == 1) {
                assertSameSet("of(T) " + input, LinkedHashSet.of(elements.get(0)), expected);
            }
        }
    }

    @Test
    public void setBulkOperationsAgreeWithSuccessiveAdds() {
        for (java.util.List<Tuple2<Key, String>> entries : inputs(false)) {
            java.util.List<Key> elements = keys(entries);
            LinkedHashSet<Key> expected = adds(elements);
            String input = elements.toString();
            for (int split = 0; split <= elements.size(); split += Math.max(1, elements.size() / 4)) {
                LinkedHashSet<Key> prefix = adds(elements.subList(0, split));
                java.util.List<Key> suffix = elements.subList(split, elements.size());
                assertSameSet("addAll at " + split + " " + input, prefix.addAll(suffix), expected);
                assertSameSet("addAll(one-shot) at " + split + " " + input, prefix.addAll(oneShot(suffix)), expected);
                LinkedHashSet<Key> suffixSet = adds(suffix);
                assertSameSet("union at " + split + " " + input, prefix.union(suffixSet), addEach(prefix, suffixSet));
            }
            // every element again, as other objects: nothing changes, not even the objects
            java.util.List<Key> copies = new ArrayList<>();
            elements.forEach(element -> copies.add(new Key(element.id, element.tag + "'")));
            assertSameSet("addAll of copies " + input, expected.addAll(copies), expected);
            assertSameSet("union of copies " + input, expected.union(LinkedHashSet.ofAll(copies)), expected);
            assertSameSet(
                    "addAll into empty " + input, LinkedHashSet.<Key>empty().addAll(elements), expected);

            java.util.Map<Key, Key> mappedKeys = new java.util.IdentityHashMap<>();
            expected.forEach(element -> mappedKeys.put(element, new Key(element.id / 2, element.tag + "'")));
            java.util.List<Key> mapped = new ArrayList<>();
            expected.forEach(element -> mapped.add(mappedKeys.get(element)));
            LinkedHashSet<Key> expectedMapped = adds(mapped);
            assertSameSet("map " + input, expected.map(mappedKeys::get), expectedMapped);
            assertSameSet(
                    "collect " + input,
                    expected.collect(element -> Option.some(mappedKeys.get(element))),
                    expectedMapped);
            assertSameSet(
                    "flatMap " + input, expected.flatMap(element -> List.of(mappedKeys.get(element))), expectedMapped);
            Tuple2<LinkedHashSet<Key>, LinkedHashSet<Key>> partitioned = expected.partitionMap(element ->
                    element.id % 2 == 0 ? Either.left(mappedKeys.get(element)) : Either.right(mappedKeys.get(element)));
            java.util.List<Key> lefts = new ArrayList<>();
            java.util.List<Key> rights = new ArrayList<>();
            expected.forEach(element -> (element.id % 2 == 0 ? lefts : rights).add(mappedKeys.get(element)));
            assertSameSet("partitionMap left " + input, partitioned._1(), adds(lefts));
            assertSameSet("partitionMap right " + input, partitioned._2(), adds(rights));
        }
    }

    /// The builder, fed one element at a time, in bulk, or after adopting a set (with or without removals), gives the
    /// set that successive adds give.
    @Test
    public void setBuilderAgreesWithSuccessiveAdds() {
        for (java.util.List<Tuple2<Key, String>> entries : inputs(false)) {
            java.util.List<Key> elements = keys(entries);
            LinkedHashSet<Key> expected = adds(elements);
            String input = elements.toString();
            LinkedHashSet.Builder<Key> byElement = LinkedHashSet.newBuilder();
            LinkedHashSet.Builder<Key> byAddAll = LinkedHashSet.newBuilder();
            for (Key element : elements) {
                byElement.add(element);
                byAddAll.addAll(java.util.List.of(element));
            }
            assertSameSet("builder add " + input, byElement.result(), expected);
            assertSameSet("builder addAll(singletons) " + input, byAddAll.result(), expected);
            assertSameSet(
                    "builder addAll(one-shot) " + input,
                    LinkedHashSet.<Key>newBuilder().addAll(oneShot(elements)).result(),
                    expected);

            for (int split = 0; split <= elements.size(); split += Math.max(1, elements.size() / 4)) {
                java.util.List<Key> prefix = elements.subList(0, split);
                java.util.List<Key> suffix = elements.subList(split, elements.size());
                LinkedHashSet<Key> prefixSet = adds(prefix);
                java.util.List<Key> prefixBefore = javaList(prefixSet);
                assertSameSet(
                        "builder adopting at " + split + " " + input,
                        LinkedHashSet.<Key>newBuilder()
                                .addAll(prefixSet)
                                .addAll(suffix)
                                .result(),
                        expected);
                assertSameSet(
                        "builder adopting the view at " + split + " " + input,
                        LinkedHashSet.<Key>newBuilder()
                                .addAll(prefixSet.asJava())
                                .addAll(suffix)
                                .result(),
                        expected);
                LinkedHashSet<Key> suffixSet = adds(suffix);
                assertSameSet(
                        "builder addAll(set) after elements at " + split + " " + input,
                        LinkedHashSet.<Key>newBuilder()
                                .addAll(prefix)
                                .addAll(suffixSet)
                                .result(),
                        addEach(prefixSet, suffixSet));
                assertSameSet(
                        "builder addAll(set) twice at " + split + " " + input,
                        LinkedHashSet.<Key>newBuilder()
                                .addAll(prefixSet)
                                .addAll(suffixSet)
                                .result(),
                        addEach(prefixSet, suffixSet));
                assertThat(javaList(prefixSet)).as("adopted set unchanged").isEqualTo(prefixBefore);
            }

            // every element again, as other objects: the first objects stay, and an adopted set comes back as it is
            java.util.List<Key> copies = new ArrayList<>();
            elements.forEach(element -> copies.add(new Key(element.id, element.tag + "'")));
            assertSameSet(
                    "builder add of copies " + input,
                    LinkedHashSet.<Key>newBuilder()
                            .addAll(elements)
                            .addAll(copies)
                            .result(),
                    expected);
            assertThat(LinkedHashSet.<Key>newBuilder()
                            .addAll(expected)
                            .addAll(copies)
                            .result())
                    .isSameAs(expected);

            if (!expected.isEmpty()) {
                java.util.List<Key> kept = javaList(expected);
                LinkedHashSet<Key> first = expected.remove(kept.get(0));
                LinkedHashSet<Key> removed = kept.size() > 2 ? first.remove(kept.get(kept.size() / 2)) : first;
                assertSameSet(
                        "builder adopting a set with removals " + input,
                        LinkedHashSet.<Key>newBuilder()
                                .addAll(removed)
                                .addAll(copies)
                                .result(),
                        addEach(removed, copies));
            }
        }
    }

    private static LinkedHashSet<Key> addEach(LinkedHashSet<Key> set, Iterable<Key> elements) {
        return Vector.ofAll(elements).foldLeft(set, LinkedHashSet::add);
    }
}
