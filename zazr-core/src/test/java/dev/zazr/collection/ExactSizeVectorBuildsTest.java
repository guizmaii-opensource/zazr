package dev.zazr.collection;

import dev.zazr.Tuple;
import dev.zazr.Tuple2;
import dev.zazr.Tuple3;
import java.util.Arrays;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.fail;

/// The operations that know the size of the `Vector` they build before they build it (`zipWithIndex` of the
/// insertion-ordered and sorted collections, `unzip` and `unzip3`, `NonEmptyVector.of`, `tabulate`, `fill` with a
/// supplier, `duplicatesBy`, `reverse`, `zipWithIndex` with a mapper, `intersperse`), against the `Vector` built one
/// element at a time the old way: the same elements, in the same order, as the same objects, and the same results
/// of later operations, at the sizes around the leaf and branch boundaries.
public class ExactSizeVectorBuildsTest {

    private static final int[] SIZES = {0, 1, 31, 32, 33, 1024, 1025, 40000};

    /// An element with an identity of its own.
    private record Element(int id) {}

    private static Element[] elements(int size) {
        Element[] elements = new Element[size];
        for (int i = 0; i < size; i++) {
            elements[i] = new Element(i);
        }
        return elements;
    }

    /// The Vector of `elements`, built one element at a time.
    private static <T> Vector<T> reference(Iterable<T> elements) {
        Vector.Builder<T> builder = Vector.newBuilder();
        for (T element : elements) {
            builder.add(element);
        }
        return builder.result();
    }

    private static <T> void assertSameElements(Vector<T> actual, Vector<T> expected, String where) {
        assertThat(actual.size()).as(where).isEqualTo(expected.size());
        assertThat(actual).as(where).isEqualTo(expected);
        java.util.Iterator<T> iterator = actual.iterator();
        for (int i = 0; i < expected.size(); i++) {
            T element = expected.get(i);
            if (actual.get(i) != element || iterator.next() != element) {
                fail(where + ": another object at " + i);
            }
        }
        assertThat(iterator.hasNext()).as(where).isFalse();
    }

    /// `actual` against `expected`, then later operations on both (`append`, `update`, `slice`), each leaving
    /// `actual` as it is.
    private static <T> void assertBehavesAs(Vector<T> actual, Vector<T> expected, T extra, String where) {
        assertSameElements(actual, expected, where);
        assertSameElements(actual.append(extra), expected.append(extra), where + " append");
        if (!expected.isEmpty()) {
            int middle = expected.size() / 2;
            assertSameElements(actual.update(middle, extra), expected.update(middle, extra), where + " update");
            int from = expected.size() / 3;
            int until = expected.size() - expected.size() / 4;
            assertSameElements(actual.slice(from, until), expected.slice(from, until), where + " slice");
        }
        assertSameElements(actual, expected, where + " after the operations");
    }

    /// Pairs whose components are the same objects as those of `expected`, which pairs each element with its rank.
    private static <T> void assertZippedWithIndex(Vector<Tuple2<T, Integer>> actual, Vector<T> expected, String where) {
        assertThat(actual.size()).as(where).isEqualTo(expected.size());
        for (int i = 0; i < expected.size(); i++) {
            Tuple2<T, Integer> pair = actual.get(i);
            if (pair._1() != expected.get(i) || pair._2() != i) {
                fail(where + ": another pair at " + i + ": " + pair);
            }
        }
        Vector.Builder<Tuple2<T, Integer>> pairs = Vector.newBuilder();
        for (Tuple2<T, Integer> pair : actual) {
            pairs.add(pair);
        }
        assertBehavesAs(actual, pairs.result(), Tuple.of(expected.isEmpty() ? null : expected.get(0), -1), where);
    }

    @Test
    public void linkedHashMapZipWithIndexPairsTheEntriesWithTheirRank() {
        for (int size : SIZES) {
            Element[] values = elements(size);
            // persistent puts in reverse, with keys that are not in order
            LinkedHashMap<Integer, Element> map = Vector.range(0, size)
                    .reverse()
                    .foldLeft(LinkedHashMap.empty(), (m, i) -> m.put(i * 7 % (size + 1), values[i]));
            Vector<Tuple2<Integer, Element>> entries = reference(map);
            Vector<Tuple2<Tuple2<Integer, Element>, Integer>> zipped = map.zipWithIndex();
            assertThat(zipped.size()).isEqualTo(size);
            for (int i = 0; i < size; i++) {
                Tuple2<Tuple2<Integer, Element>, Integer> pair = zipped.get(i);
                assertThat(pair._1()._1()).isEqualTo(entries.get(i)._1());
                assertThat(pair._1()._2()).isSameAs(entries.get(i)._2());
                assertThat(pair._2()).isEqualTo(i);
            }
            Vector<Tuple2<Integer, Element>> zippedEntries = zipped.map(Tuple2::_1);
            assertZippedWithIndex(zipped, zippedEntries, "LinkedHashMap, size " + size);
            if (size == 0) {
                assertThat(zipped).isSameAs(Vector.empty());
            }
        }
    }

    @Test
    public void linkedHashSetZipWithIndexPairsTheElementsWithTheirRank() {
        for (int size : SIZES) {
            LinkedHashSet<Element> set = LinkedHashSet.ofAll(Arrays.asList(elements(size)));
            assertZippedWithIndex(set.zipWithIndex(), reference(set), "LinkedHashSet, size " + size);
            if (!set.isEmpty()) {
                // a set whose insertion order starts after a removed slot
                LinkedHashSet<Element> rest = set.remove(set.head());
                assertZippedWithIndex(
                        rest.zipWithIndex(), reference(rest), "LinkedHashSet minus its head, size " + size);
            }
        }
        assertThat(LinkedHashSet.empty().zipWithIndex()).isSameAs(Vector.empty());
    }

    @Test
    public void treeZipWithIndexPairsTheElementsWithTheirRank() {
        for (int size : SIZES) {
            Element[] values = elements(size);
            // persistent puts of keys in a scrambled order
            Function<Integer, Integer> key = i -> (int) ((i * 40503L) % 65537);
            TreeMap<Integer, Element> map =
                    Vector.range(0, size).foldLeft(TreeMap.empty(), (m, i) -> m.put(key.apply(i), values[i]));
            TreeSet<Integer> set = Vector.range(0, size).foldLeft(TreeSet.empty(), (t, i) -> t.add(key.apply(i)));
            assertZippedWithIndex(map.zipWithIndex(), reference(map), "TreeMap, size " + size);
            assertZippedWithIndex(set.zipWithIndex(), reference(set), "TreeSet, size " + size);
        }
        assertThat(TreeMap.empty().zipWithIndex()).isSameAs(Vector.empty());
        assertThat(TreeSet.empty().zipWithIndex()).isSameAs(Vector.empty());
    }

    private static Vector<Tuple3<Element, Element, Element>> triples(int size) {
        return Vector.tabulate(size, i -> Tuple.of(new Element(i), new Element(-i), new Element(i + size)));
    }

    @Test
    public void unzipSplitsTheElements() {
        for (int size : SIZES) {
            Vector<Tuple3<Element, Element, Element>> triples = triples(size);
            Vector<Element> firsts = reference(triples.map(Tuple3::_1));
            Vector<Element> seconds = reference(triples.map(Tuple3::_2));
            Vector<Element> thirds = reference(triples.map(Tuple3::_3));
            Element extra = new Element(-1);
            String where = "size " + size;
            Tuple2<Vector<Element>, Vector<Element>> two = triples.unzip(t -> Tuple.of(t._1(), t._2()));
            assertBehavesAs(two._1(), firsts, extra, where + " unzip 1");
            assertBehavesAs(two._2(), seconds, extra, where + " unzip 2");
            Tuple3<Vector<Element>, Vector<Element>, Vector<Element>> three = triples.unzip3(Function.identity());
            assertBehavesAs(three._1(), firsts, extra, where + " unzip3 1");
            assertBehavesAs(three._2(), seconds, extra, where + " unzip3 2");
            assertBehavesAs(three._3(), thirds, extra, where + " unzip3 3");
            if (size == 0) {
                assertThat(two._1()).isSameAs(Vector.empty());
                assertThat(two._2()).isSameAs(Vector.empty());
                assertThat(three._3()).isSameAs(Vector.empty());
                continue;
            }
            NonEmptyVector<Tuple3<Element, Element, Element>> nonEmpty =
                    NonEmptyVector.fromVector(triples).get();
            Tuple2<NonEmptyVector<Element>, NonEmptyVector<Element>> nonEmptyTwo =
                    nonEmpty.unzip(t -> Tuple.of(t._1(), t._2()));
            assertBehavesAs(nonEmptyTwo._1().toVector(), firsts, extra, where + " NonEmptyVector.unzip 1");
            assertBehavesAs(nonEmptyTwo._2().toVector(), seconds, extra, where + " NonEmptyVector.unzip 2");
            Tuple3<NonEmptyVector<Element>, NonEmptyVector<Element>, NonEmptyVector<Element>> nonEmptyThree =
                    nonEmpty.unzip3(Function.identity());
            assertBehavesAs(nonEmptyThree._1().toVector(), firsts, extra, where + " NonEmptyVector.unzip3 1");
            assertBehavesAs(nonEmptyThree._2().toVector(), seconds, extra, where + " NonEmptyVector.unzip3 2");
            assertBehavesAs(nonEmptyThree._3().toVector(), thirds, extra, where + " NonEmptyVector.unzip3 3");
        }
    }

    // the tuples of i, with the component `at` (from 1) null for the last element `last`
    private static Tuple2<Integer, Integer> pair(int i, int last, int at) {
        return Tuple.of(i == last && at == 1 ? null : i, i == last && at == 2 ? null : i);
    }

    private static Tuple3<Integer, Integer, Integer> triple(int i, int last, int at) {
        return Tuple.of(
                i == last && at == 1 ? null : i, i == last && at == 2 ? null : i, i == last && at == 3 ? null : i);
    }

    @Test
    public void unzipRejectsANullInEachComponent() {
        for (int size : new int[] {1, 32, 33}) {
            Vector<Integer> vector = Vector.range(0, size);
            NonEmptyVector<Integer> nonEmpty = NonEmptyVector.fromVector(vector).get();
            int last = size - 1;
            for (int at = 1; at <= 2; at++) {
                int component = at;
                String where = "size " + size + ", component " + component;
                assertThatNullPointerException()
                        .as(where)
                        .isThrownBy(() -> vector.unzip(i -> pair(i, last, component)))
                        .withMessage("Vector.Builder.add: element is null");
                assertThatNullPointerException()
                        .as(where)
                        .isThrownBy(() -> nonEmpty.unzip(i -> pair(i, last, component)))
                        .withMessage("NonEmptyVector.unzip: unzipper returned a null component");
            }
            for (int at = 1; at <= 3; at++) {
                int component = at;
                String where = "size " + size + ", component " + component;
                assertThatNullPointerException()
                        .as(where)
                        .isThrownBy(() -> vector.unzip3(i -> triple(i, last, component)))
                        .withMessage("Vector.Builder.add: element is null");
                assertThatNullPointerException()
                        .as(where)
                        .isThrownBy(() -> nonEmpty.unzip3(i -> triple(i, last, component)))
                        .withMessage("NonEmptyVector.unzip3: unzipper returned a null component");
            }
        }
    }

    @Test
    public void nonEmptyVectorOfIsTheHeadThenTheTail() {
        for (int size : SIZES) {
            if (size == 0) {
                continue;
            }
            Element[] all = elements(size);
            Element[] tail = Arrays.copyOfRange(all, 1, size);
            NonEmptyVector<Element> vector = NonEmptyVector.of(all[0], tail);
            Vector<Element> expected = reference(Arrays.asList(all));
            Arrays.fill(tail, new Element(-2));
            assertBehavesAs(vector.toVector(), expected, new Element(-1), "size " + size);
        }
        assertThatNullPointerException()
                .isThrownBy(() -> NonEmptyVector.of(1, 2, null))
                .withMessage("NonEmptyVector: element is null");
    }

    @Test
    public void tabulateAndFillHoldWhatTheFunctionReturns() {
        for (int size : SIZES) {
            Element[] values = elements(size);
            Vector<Element> expected = reference(Arrays.asList(values));
            String where = "size " + size;
            assertBehavesAs(Vector.tabulate(size, i -> values[i]), expected, new Element(-1), where + " tabulate");
            java.util.Iterator<Element> supplied = Arrays.asList(values).iterator();
            assertBehavesAs(Vector.fill(size, supplied::next), expected, new Element(-1), where + " fill");
            assertThat(supplied.hasNext()).isFalse();
        }
        assertThat(Vector.tabulate(0, i -> i)).isSameAs(Vector.empty());
        assertThat(Vector.tabulate(-1, i -> i)).isSameAs(Vector.empty());
        assertThat(Vector.fill(-1, () -> 1)).isSameAs(Vector.empty());
        for (int size : new int[] {1, 32, 33}) {
            int last = size - 1;
            assertThatNullPointerException()
                    .isThrownBy(() -> Vector.tabulate(size, i -> i == last ? null : i))
                    .withMessage("Vector: element is null");
            int[] calls = {0};
            assertThatNullPointerException()
                    .isThrownBy(() -> Vector.fill(size, () -> calls[0]++ == last ? null : 0))
                    .withMessage("Vector: element is null");
        }
    }

    @Test
    public void duplicatesByIsTheFirstElementOfEachRepeatedKey() {
        for (int size : SIZES) {
            // keys 0 .. size - 1 each once, then the keys that are a multiple of 3 again
            Element[] firsts = elements(size);
            Vector<Element> vector = Vector.of(firsts)
                    .appendAll(Vector.tabulate(size, i -> new Element(i)).filter(e -> e.id() % 3 == 0));
            Vector.Builder<Element> builder = Vector.newBuilder();
            for (Element element : firsts) {
                if (element.id() % 3 == 0) {
                    builder.add(element);
                }
            }
            Vector<Element> expected = builder.result();
            Vector<Element> duplicates = vector.duplicatesBy(Element::id);
            assertBehavesAs(duplicates, expected, new Element(-1), "size " + size);
        }
    }

    @Test
    public void reverseIsTheElementsFromTheLast() {
        for (int size : SIZES) {
            Element[] values = elements(size);
            Vector<Element> vector = Vector.of(values);
            Element[] reversed = values.clone();
            java.util.Collections.reverse(Arrays.asList(reversed));
            assertBehavesAs(vector.reverse(), reference(Arrays.asList(reversed)), new Element(-1), "size " + size);
            assertSameElements(vector.reverse().reverse(), vector, "size " + size + " twice");
            if (size <= 1) {
                assertThat(vector.reverse()).isSameAs(vector);
            }
        }
    }

    @Test
    public void zipWithIndexMapsEachElementWithItsRank() {
        for (int size : SIZES) {
            Vector<Element> vector = Vector.of(elements(size));
            Element[] mapped = new Element[size];
            Vector<Element> result = vector.zipWithIndex((e, i) -> mapped[i] = new Element(e.id() * 2 + i));
            assertBehavesAs(result, reference(Arrays.asList(mapped)), new Element(-1), "size " + size);
            assertZippedWithIndex(vector.zipWithIndex(), vector, "zipWithIndex(), size " + size);
        }
        assertThat(Vector.<Integer>empty().zipWithIndex((e, i) -> e)).isSameAs(Vector.empty());
        for (int size : new int[] {1, 32, 33}) {
            int last = size - 1;
            assertThatNullPointerException()
                    .isThrownBy(() -> Vector.range(0, size).zipWithIndex((e, i) -> i == last ? null : e))
                    .withMessage("Iterator: element is null");
            assertThatNullPointerException()
                    .isThrownBy(() -> NonEmptyVector.fromVector(Vector.range(0, size))
                            .get()
                            .zipWithIndex((e, i) -> i == last ? null : e))
                    .withMessage("Iterator: element is null");
        }
    }

    @Test
    public void intersperseInsertsTheSeparatorBetweenTwoElements() {
        for (int size : SIZES) {
            Element[] values = elements(size);
            Element separator = new Element(-3);
            Vector.Builder<Element> builder = Vector.newBuilder();
            for (int i = 0; i < size; i++) {
                if (i > 0) {
                    builder.add(separator);
                }
                builder.add(values[i]);
            }
            Vector<Element> vector = Vector.of(values);
            assertBehavesAs(vector.intersperse(separator), builder.result(), new Element(-1), "size " + size);
            if (size <= 1) {
                assertThat(vector.intersperse(separator)).isSameAs(vector);
            }
        }
        assertThatNullPointerException()
                .isThrownBy(() -> Vector.of(1, 2).intersperse(null))
                .withMessage("Iterator.intersperse: element is null");
    }
}
