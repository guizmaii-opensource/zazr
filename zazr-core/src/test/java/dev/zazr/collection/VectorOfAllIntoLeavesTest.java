package dev.zazr.collection;

import dev.zazr.Tuple;
import dev.zazr.Tuple2;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

/// `Vector.ofAll` of the Zazr collections whose elements it writes straight into the leaves (`List`, `Queue`,
/// `HashSet`, `LinkedHashSet`, `TreeSet`, the key sets of the maps) and of the non-empty types, which it reads as the
/// collection they wrap, against the `Vector` built one element at a time in iteration order: the same elements, as
/// the same objects, in the same order, at every size around the leaf and level boundaries; and a `Vector` whose later
/// operations work. A `Traversable` written elsewhere whose `size()` is wrong is read as before.
public class VectorOfAllIntoLeavesTest {

    // a size above three levels of the tree, the same on every run, which the message of a failure names
    private static final int RANDOM_SIZE = 32_770 + new java.util.Random(364).nextInt(70_000);

    private static final int[] SIZES = {
        0, 1, 2, 31, 32, 33, 63, 64, 65, 1023, 1024, 1025, 32767, 32768, 32769, RANDOM_SIZE
    };

    /// An element with an identity of its own, ordered by its id, whose hash is given: a hash shared by several
    /// elements puts them in one collision node of a `HashSet`.
    private record Key(int id, int hash) implements Comparable<Key> {
        @Override
        public boolean equals(Object o) {
            return o instanceof Key that && that.id == id;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public int compareTo(Key that) {
            return Integer.compare(id, that.id);
        }
    }

    // the keys 0 .. size - 1 in a shuffled order that depends on size only, each hashed by `hash`
    private static Key[] keys(int size, java.util.function.IntUnaryOperator hash) {
        Key[] keys = new Key[size];
        for (int i = 0; i < size; i++) {
            keys[i] = new Key(i, hash.applyAsInt(i));
        }
        java.util.Random random = new java.util.Random(size);
        for (int i = size - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            Key swapped = keys[i];
            keys[i] = keys[j];
            keys[j] = swapped;
        }
        return keys;
    }

    /// The Vector built one element at a time, in the iteration order of `source`.
    private static <T> Vector<T> reference(Iterable<T> source) {
        Vector.Builder<T> builder = Vector.newBuilder();
        for (T element : source) {
            builder.add(element);
        }
        return builder.result();
    }

    private static <T> void assertSameElements(Vector<T> actual, Vector<T> expected, String where) {
        assertSameElements(actual, expected, where, true);
    }

    /// With `sameObjects`, the elements are the same objects; without, equal ones: a map makes a new entry at each
    /// walk.
    private static <T> void assertSameElements(
            Vector<T> actual, Vector<T> expected, String where, boolean sameObjects) {
        assertThat(actual.size()).as(where).isEqualTo(expected.size());
        java.util.Iterator<T> iterator = actual.iterator();
        for (int i = 0; i < expected.size(); i++) {
            T element = expected.get(i);
            T got = actual.get(i);
            T walked = iterator.next();
            boolean same = sameObjects
                    ? (got == element && walked == element)
                    : (got.equals(element) && walked.equals(element));
            if (!same) {
                fail(where + ": another element at " + i);
            }
        }
        assertThat(iterator.hasNext()).as(where).isFalse();
        assertThat(actual).as(where).isEqualTo(expected);
    }

    /// `vector` against `expected`, then the later operations on both, each leaving `vector` as it is.
    private static void assertBehavesAs(
            Vector<Object> vector, Vector<Object> expected, String where, boolean sameObjects) {
        assertSameElements(vector, expected, where, sameObjects);
        Object extra = new Key(-1, -1);
        assertSameElements(vector.append(extra), expected.append(extra), where + " append", sameObjects);
        assertSameElements(vector.prepend(extra), expected.prepend(extra), where + " prepend", sameObjects);
        int size = expected.size();
        assertSameElements(vector.drop(size / 3), expected.drop(size / 3), where + " drop", sameObjects);
        assertSameElements(vector.dropRight(size / 3), expected.dropRight(size / 3), where + " dropRight", sameObjects);
        if (size > 0) {
            for (int at : new int[] {0, size / 2, size - 1}) {
                assertSameElements(
                        vector.update(at, extra), expected.update(at, extra), where + " update " + at, sameObjects);
            }
            int from = size / 3;
            int until = size - size / 4;
            assertSameElements(vector.slice(from, until), expected.slice(from, until), where + " slice", sameObjects);
            assertSameElements(
                    vector.appendAll(vector),
                    expected.appendAll(expected),
                    where + " appendAll of itself",
                    sameObjects);
        }
        assertSameElements(vector, expected, where + " after the operations", sameObjects);
    }

    /// The Zazr collections of `keys`, by name.
    private static java.util.List<java.util.Map.Entry<String, Iterable<Object>>> sources(Key[] keys) {
        java.util.List<Object> list = Arrays.asList((Object[]) keys);
        java.util.List<java.util.Map.Entry<String, Iterable<Object>>> sources = new ArrayList<>();
        sources.add(java.util.Map.entry("List", List.ofAll(list)));
        sources.add(java.util.Map.entry("Queue", Queue.ofAll(list)));
        // half in the front list, half in the rear one
        sources.add(java.util.Map.entry(
                "Queue with a rear",
                Queue.ofAll(list.subList(0, keys.length / 2)).enqueueAll(list.subList(keys.length / 2, keys.length))));
        sources.add(java.util.Map.entry("HashSet", HashSet.ofAll(list)));
        sources.add(java.util.Map.entry("LinkedHashSet", LinkedHashSet.ofAll(list)));
        sources.add(java.util.Map.entry("TreeSet", TreeSet.ofAll(Comparator.comparing(o -> (Key) o), list)));
        sources.add(java.util.Map.entry(
                "TreeSet in reverse",
                TreeSet.ofAll(Comparator.comparing(o -> (Key) o).reversed(), list)));
        java.util.LinkedHashMap<Object, Object> javaMap = new java.util.LinkedHashMap<>();
        for (Key key : keys) {
            javaMap.put(key, key.id());
        }
        HashMap<Object, Object> hashMap = HashMap.ofAll(javaMap);
        LinkedHashMap<Object, Object> linkedHashMap = LinkedHashMap.ofAll(javaMap);
        TreeMap<Object, Object> treeMap = TreeMap.ofAll(Comparator.comparing(o -> (Key) o), javaMap);
        sources.add(java.util.Map.entry("HashMap.keySet", hashMap.keySet()));
        sources.add(java.util.Map.entry("LinkedHashMap.keySet", linkedHashMap.keySet()));
        sources.add(java.util.Map.entry("TreeMap.keySet", treeMap.keySet()));
        // a set from which elements were removed, whose trie has been rebuilt
        HashSet<Object> removed = HashSet.ofAll(list).addAll(java.util.List.of(new Key(-5, 7), new Key(-6, 7)));
        sources.add(java.util.Map.entry(
                "HashSet after removals", removed.remove(new Key(-5, 7)).remove(new Key(-6, 7))));
        return sources;
    }

    private static void assertOfAllOfTheZazrCollections(java.util.function.IntUnaryOperator hash, String hashing) {
        for (int size : SIZES) {
            for (java.util.Map.Entry<String, Iterable<Object>> source : sources(keys(size, hash))) {
                String where = source.getKey() + ", " + hashing + ", size " + size;
                Vector<Object> expected = reference(source.getValue());
                assertThat(expected.size()).as(where).isEqualTo(size);
                assertBehavesAs(Vector.ofAll(source.getValue()), expected, where, true);
                // the source is left as it was, and a second Vector of it is independent of the first
                assertSameElements(Vector.ofAll(source.getValue()), expected, where + ", read again");
                assertSameElements(reference(source.getValue()), expected, where + ", the source after");
            }
        }
    }

    @Test
    public void ofAllOfAZazrCollectionIsTheVectorBuiltElementByElement() {
        assertOfAllOfTheZazrCollections(i -> i, "distinct hashes");
    }

    @Test
    public void ofAllOfAHashSetWithCollisionsKeepsTheIterationOrder() {
        // four elements per hash: each in a collision node at the bottom of the trie
        assertOfAllOfTheZazrCollections(i -> i / 4, "four elements per hash");
        // most elements alone, every seventh sharing the hash of the one before: collision nodes next to payloads
        assertOfAllOfTheZazrCollections(i -> (i % 7 == 0) ? i - 1 : i, "a few shared hashes");
    }

    private static Tuple2<Object, Object> entry(Key key) {
        return Tuple.of(key, key.id());
    }

    @Test
    public void ofAllOfANonEmptyTypeIsTheVectorBuiltElementByElement() {
        for (int size : SIZES) {
            if (size == 0) {
                continue;
            }
            Key[] keys = keys(size, i -> i);
            Object head = keys[0];
            java.util.List<Object> tail = Arrays.asList((Object[]) keys).subList(1, size);
            java.util.List<Tuple2<Object, Object>> entries = new ArrayList<>();
            for (int i = 1; i < size; i++) {
                entries.add(entry(keys[i]));
            }
            Comparator<Object> byKey = Comparator.comparing(o -> (Key) o);
            java.util.List<java.util.Map.Entry<String, Iterable<Object>>> sources = java.util.List.of(
                    java.util.Map.entry("NonEmptyVector", NonEmptyVector.fromIterable(head, tail)),
                    java.util.Map.entry("NonEmptySet", NonEmptySet.fromIterable(head, tail)),
                    java.util.Map.entry("NonEmptySortedSet", NonEmptySortedSet.fromIterable(byKey, head, tail)),
                    java.util.Map.entry("NonEmptyMap", widen(NonEmptyMap.fromIterable(entry(keys[0]), entries))),
                    java.util.Map.entry(
                            "NonEmptySortedMap",
                            widen(NonEmptySortedMap.fromIterable(byKey, entry(keys[0]), entries))));
            for (java.util.Map.Entry<String, Iterable<Object>> source : sources) {
                String where = source.getKey() + ", size " + size;
                Vector<Object> expected = reference(source.getValue());
                assertThat(expected.size()).as(where).isEqualTo(size);
                // a map makes a new entry at each walk
                assertBehavesAs(
                        Vector.ofAll(source.getValue()),
                        expected,
                        where,
                        !source.getKey().endsWith("Map"));
            }
            NonEmptyVector<Object> nonEmptyVector = NonEmptyVector.fromIterable(head, tail);
            assertThat(Vector.ofAll(nonEmptyVector)).isSameAs(nonEmptyVector.toVector());
        }
    }

    @SuppressWarnings("unchecked")
    private static Iterable<Object> widen(Iterable<? extends Tuple2<Object, Object>> entries) {
        return (Iterable<Object>) (Iterable<?>) entries;
    }

    @Test
    public void ofAllOfAnEmptyZazrCollectionIsTheEmptyVector() {
        assertThat(Vector.ofAll(HashSet.empty())).isSameAs(Vector.empty());
        assertThat(Vector.ofAll(LinkedHashSet.empty())).isSameAs(Vector.empty());
        assertThat(Vector.ofAll(TreeSet.<Integer>empty())).isSameAs(Vector.empty());
        assertThat(Vector.ofAll(List.empty())).isSameAs(Vector.empty());
        assertThat(Vector.ofAll(Queue.empty())).isSameAs(Vector.empty());
    }

    @Test
    public void nullsAreRejectedAsBefore() {
        assertThatThrownBy(() -> Vector.ofAll((Iterable<Object>) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("iterable is null");
        // the Zazr collections hold no null, so the elements written into the leaves are never null
        java.util.List<Object> withNull = new ArrayList<>(Arrays.asList((Object[]) keys(40, i -> i)));
        withNull.set(20, null);
        assertThatThrownBy(() -> List.ofAll(withNull)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Queue.ofAll(withNull)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> HashSet.ofAll(withNull)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> LinkedHashSet.ofAll(withNull)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> TreeSet.ofAll(Comparator.comparing(o -> (Key) o), withNull))
                .isInstanceOf(NullPointerException.class);
        // a JDK collection with a null is still rejected
        assertThatThrownBy(() -> Vector.ofAll(withNull))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Vector: element is null");
    }

    @Test
    public void aHashSetRejectsANullReplacementSoNoNullReachesTheLeaves() {
        for (int size : new int[] {1, 32, 33, 1025}) {
            HashSet<Object> set = HashSet.ofAll(Arrays.asList((Object[]) keys(size, i -> i)));
            Object present = new Key(size / 2, size / 2);
            Object absent = new Key(-7, -7);
            String where = "size " + size;
            assertThatThrownBy(() -> set.replace(present, null))
                    .as(where)
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("HashSet: element is null");
            assertThatThrownBy(() -> set.replaceAll(present, null))
                    .as(where)
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("HashSet: element is null");
            // an absent element is not replaced, as on LinkedHashSet
            assertThat(set.replace(absent, null)).as(where).isSameAs(set);
            assertThat(set.replaceAll(absent, null)).as(where).isSameAs(set);
            // the set that would have held the null is never made, so neither ofAll nor a NonEmptySet reads one
            assertThatThrownBy(() -> Vector.ofAll(set.replace(present, null)))
                    .as(where)
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("HashSet: element is null");
            assertThatThrownBy(() -> Vector.ofAll(
                            NonEmptySet.fromSet(set.replaceAll(present, null)).get()))
                    .as(where)
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("HashSet: element is null");
            // the set is left as it was
            assertSameElements(Vector.ofAll(set), reference(set), where);
            assertThat(set.size()).as(where).isEqualTo(size);
        }
    }

    /// A `Traversable` written elsewhere: every method goes to `elements` but `size()`, which answers `size`.
    @SuppressWarnings("unchecked")
    private static Traversable<Object> withSize(Traversable<Object> elements, int size) {
        return (Traversable<Object>) java.lang.reflect.Proxy.newProxyInstance(
                Traversable.class.getClassLoader(), new Class<?>[] {Traversable.class}, (proxy, method, args) -> {
                    if (method.getName().equals("size") && method.getParameterCount() == 0) {
                        return size;
                    }
                    try {
                        return method.invoke(elements, args);
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        throw e.getCause();
                    }
                });
    }

    @Test
    public void aTraversableWrittenElsewhereWithAWrongSizeIsReadAsBefore() {
        for (int size : new int[] {33, 1025}) {
            HashSet<Object> set = HashSet.ofAll(Arrays.asList((Object[]) keys(size, i -> i)));
            Vector<Object> expected = reference(set);
            // too large: the read runs out of elements
            assertThatThrownBy(() -> Vector.ofAll(withSize(set, size + 1)))
                    .as("size " + size)
                    .isInstanceOf(NoSuchElementException.class);
            // too small: the first size() elements
            assertSameElements(Vector.ofAll(withSize(set, size - 1)), expected.dropRight(1), "size " + size);
            assertSameElements(Vector.ofAll(withSize(set, size)), expected, "size " + size);
        }
    }
}
