package dev.zazr.collection;

import java.util.AbstractList;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.function.IntFunction;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

/// `Vector.ofAll` of a sized source (a JDK collection, a Zazr collection, a collection written elsewhere), and the
/// operations that build a `Vector` from an array of their own (`sorted`, `combinations`, `permutations`,
/// `transpose`), against the `Vector` built one element at a time in iteration order: the same elements, as the same
/// objects, in the same order; the same results of later operations; and a `Vector` that a later change of the
/// source, or of what its `toArray()` returned, leaves as it is.
public class VectorOfAllSizedSourceTest {

    private static final int[] SIZES = {0, 1, 31, 32, 33, 1024, 1025, 40000};

    /// An element with an identity of its own, ordered by its id.
    private record Element(int id) implements Comparable<Element> {
        @Override
        public int compareTo(Element that) {
            return Integer.compare(id, that.id);
        }
    }

    private static Element[] elements(int size) {
        Element[] elements = new Element[size];
        for (int i = 0; i < size; i++) {
            elements[i] = new Element(i);
        }
        return elements;
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

    /// `vector` against `expected`, then later operations on both: `append`, `update`, `slice`, each leaving
    /// `vector` as it is.
    private static void assertBehavesAs(Vector<Object> vector, Vector<Object> expected, String where) {
        assertSameElements(vector, expected, where);
        Object extra = new Element(-1);
        assertSameElements(vector.append(extra), expected.append(extra), where + " append");
        if (!expected.isEmpty()) {
            int middle = expected.size() / 2;
            assertSameElements(vector.update(middle, extra), expected.update(middle, extra), where + " update");
            assertSameElements(vector.update(0, 42), expected.update(0, 42), where + " update to another type");
            int from = expected.size() / 3;
            int until = expected.size() - expected.size() / 4;
            assertSameElements(vector.slice(from, until), expected.slice(from, until), where + " slice");
        }
        assertSameElements(vector, expected, where + " after the operations");
    }

    /// A sized source of `size` elements, and a change to make to it once the Vector is built.
    private record Source(Iterable<Object> elements, Runnable change) {}

    private static Source javaList(java.util.List<Object> list) {
        return new Source(list, () -> {
            if (!list.isEmpty()) {
                list.set(0, new Element(-2));
            }
            list.add(new Element(-3));
        });
    }

    private static Source javaCollection(java.util.Collection<Object> collection) {
        return new Source(collection, () -> {
            collection.clear();
            collection.add(new Element(-3));
        });
    }

    /// A view of `map`, and a change of `map`: every value replaced, and one more key.
    private static Source mapView(java.util.Collection<Object> view, java.util.Map<Object, Object> map) {
        return new Source(view, () -> {
            map.replaceAll((key, value) -> new Element(-2));
            map.put(new Element(-3), new Element(-3));
        });
    }

    /// `map` holding each element as its own key and value.
    private static <M extends java.util.Map<Object, Object>> M mapOf(M map, Element[] elements) {
        for (Element element : elements) {
            map.put(element, element);
        }
        return map;
    }

    /// The views of the JDK maps whose key and value views are matched by exact class.
    private static void addMapViews(java.util.List<java.util.Map.Entry<String, Source>> sources, Element[] elements) {
        java.util.HashMap<Object, Object> hashMap = mapOf(new java.util.HashMap<>(), elements);
        sources.add(java.util.Map.entry("HashMap.keySet", mapView(hashMap.keySet(), hashMap)));
        java.util.HashMap<Object, Object> hashMapOfValues = mapOf(new java.util.HashMap<>(), elements);
        sources.add(java.util.Map.entry("HashMap.values", mapView(hashMapOfValues.values(), hashMapOfValues)));
        java.util.LinkedHashMap<Object, Object> linked = mapOf(new java.util.LinkedHashMap<>(), elements);
        sources.add(java.util.Map.entry("LinkedHashMap.keySet", mapView(linked.keySet(), linked)));
        java.util.LinkedHashMap<Object, Object> linkedOfValues = mapOf(new java.util.LinkedHashMap<>(), elements);
        sources.add(java.util.Map.entry("LinkedHashMap.values", mapView(linkedOfValues.values(), linkedOfValues)));
        java.util.LinkedHashMap<Object, Object> reversed = mapOf(new java.util.LinkedHashMap<>(), elements);
        sources.add(java.util.Map.entry(
                "LinkedHashMap.reversed().keySet", mapView(reversed.reversed().keySet(), reversed)));
        java.util.LinkedHashMap<Object, Object> reversedOfValues = mapOf(new java.util.LinkedHashMap<>(), elements);
        sources.add(java.util.Map.entry(
                "LinkedHashMap.reversed().values",
                mapView(reversedOfValues.reversed().values(), reversedOfValues)));
        java.util.TreeMap<Object, Object> tree = mapOf(new java.util.TreeMap<>(), elements);
        sources.add(java.util.Map.entry("TreeMap.keySet", mapView(tree.keySet(), tree)));
        java.util.TreeMap<Object, Object> descending = mapOf(new java.util.TreeMap<>(), elements);
        sources.add(
                java.util.Map.entry("TreeMap.descendingKeySet", mapView(descending.descendingKeySet(), descending)));
        java.util.TreeMap<Object, Object> head = mapOf(new java.util.TreeMap<>(), elements);
        head.put(new Element(elements.length), new Element(elements.length));
        sources.add(java.util.Map.entry(
                "TreeMap.headMap().keySet",
                mapView(head.headMap(new Element(elements.length)).keySet(), head)));
        java.util.TreeMap<Object, Object> treeOfValues = mapOf(new java.util.TreeMap<>(), elements);
        sources.add(java.util.Map.entry("TreeMap.values", mapView(treeOfValues.values(), treeOfValues)));
        ConcurrentHashMap<Object, Object> concurrent = mapOf(new ConcurrentHashMap<>(), elements);
        sources.add(java.util.Map.entry("ConcurrentHashMap.keySet", mapView(concurrent.keySet(), concurrent)));
        ConcurrentHashMap<Object, Object> concurrentOfValues = mapOf(new ConcurrentHashMap<>(), elements);
        sources.add(java.util.Map.entry(
                "ConcurrentHashMap.values", mapView(concurrentOfValues.values(), concurrentOfValues)));
        java.util.Set<Object> keySet = ConcurrentHashMap.newKeySet();
        keySet.addAll(Arrays.asList((Object[]) elements));
        sources.add(java.util.Map.entry("ConcurrentHashMap.newKeySet", javaCollection(keySet)));
        java.util.HashMap<Object, Object> subclass = mapOf(new java.util.HashMap<>() {}, elements);
        sources.add(java.util.Map.entry("HashMap.keySet of a subclass", mapView(subclass.keySet(), subclass)));
    }

    /// The sized sources of `elements`: every JDK collection matched by exact class, a sub-list of each list that
    /// has one, Zazr collections, and collections that are not matched.
    private static java.util.List<java.util.Map.Entry<String, Source>> sources(Element[] elements) {
        java.util.List<Object> asList = Arrays.asList((Object[]) elements);
        java.util.List<java.util.Map.Entry<String, Source>> sources = new ArrayList<>();
        sources.add(java.util.Map.entry("ArrayList", javaList(new ArrayList<>(asList))));
        sources.add(java.util.Map.entry("LinkedList", javaList(new LinkedList<>(asList))));
        sources.add(java.util.Map.entry("ArrayDeque", javaCollection(new ArrayDeque<>(asList))));
        sources.add(java.util.Map.entry("java.util.HashSet", javaCollection(new java.util.HashSet<>(asList))));
        sources.add(
                java.util.Map.entry("java.util.LinkedHashSet", javaCollection(new java.util.LinkedHashSet<>(asList))));
        sources.add(java.util.Map.entry("java.util.TreeSet", javaCollection(new java.util.TreeSet<>(asList))));
        Object[] backing = Arrays.copyOf(elements, elements.length, Object[].class);
        sources.add(java.util.Map.entry(
                "Arrays.asList", new Source(Arrays.asList(backing), () -> Arrays.fill(backing, new Element(-2)))));
        sources.add(java.util.Map.entry("List.of", new Source(java.util.List.copyOf(asList), () -> {})));
        sources.add(java.util.Map.entry("Set.of", new Source(java.util.Set.copyOf(asList), () -> {})));
        ArrayList<Object> parent = new ArrayList<>(asList);
        parent.add(new Element(-4));
        sources.add(java.util.Map.entry("ArrayList.subList", javaList(parent.subList(0, elements.length))));
        java.util.List<Object> immutableParent = new ArrayList<>(asList);
        immutableParent.add(new Element(-4));
        sources.add(java.util.Map.entry(
                "List.of(..).subList",
                new Source(java.util.List.copyOf(immutableParent).subList(0, elements.length), () -> {})));
        sources.add(java.util.Map.entry("PriorityQueue", javaCollection(new PriorityQueue<>(asList))));
        sources.add(java.util.Map.entry("CopyOnWriteArrayList", javaList(new CopyOnWriteArrayList<>(asList))));
        CopyOnWriteArrayList<Object> copyOnWriteParent = new CopyOnWriteArrayList<>(asList);
        copyOnWriteParent.add(new Element(-4));
        sources.add(java.util.Map.entry(
                "CopyOnWriteArrayList.subList", javaList(copyOnWriteParent.subList(0, elements.length))));
        addMapViews(sources, elements);
        CopyOnWriteArrayList<Object> reversedBase = new CopyOnWriteArrayList<>(asList.reversed());
        sources.add(java.util.Map.entry("CopyOnWriteArrayList.reversed", new Source(reversedBase.reversed(), () -> {
            if (!reversedBase.isEmpty()) {
                reversedBase.set(0, new Element(-2));
            }
            reversedBase.add(new Element(-3));
        })));
        ArrayList<Object> wrapped = new ArrayList<>(asList);
        sources.add(java.util.Map.entry(
                "unmodifiableList",
                new Source(java.util.Collections.unmodifiableList(wrapped), () -> wrapped.add(new Element(-3)))));
        ArrayList<Object> subclass = new ArrayList<>(asList) {};
        sources.add(java.util.Map.entry("ArrayList subclass", javaList(subclass)));
        sources.add(java.util.Map.entry("zazr List", new Source(List.ofAll(asList), () -> {})));
        sources.add(java.util.Map.entry("zazr Queue", new Source(Queue.ofAll(asList), () -> {})));
        sources.add(java.util.Map.entry("zazr LinkedHashSet", new Source(LinkedHashSet.ofAll(asList), () -> {})));
        sources.add(java.util.Map.entry("zazr HashSet", new Source(HashSet.ofAll(asList), () -> {})));
        sources.add(java.util.Map.entry(
                "zazr List.asJava", new Source(List.ofAll(asList).asJava(), () -> {})));
        LeakingList leaking = new LeakingList(Arrays.copyOf(elements, elements.length, Object[].class));
        sources.add(java.util.Map.entry("toArray returns its own array", new Source(leaking, leaking::overwrite)));
        sources.add(
                java.util.Map.entry("toArray returns a String[]", new Source(new TypedArrayList(elements), () -> {})));
        return sources;
    }

    /// A list whose `toArray()` returns the array it holds, and writes later: against the contract of
    /// `Collection.toArray()`, so that a `Vector` keeping that array would change.
    private static final class LeakingList extends AbstractList<Object> {
        private final Object[] array;

        LeakingList(Object[] array) {
            this.array = array;
        }

        @Override
        public Object get(int index) {
            return array[index];
        }

        @Override
        public int size() {
            return array.length;
        }

        @Override
        public Object[] toArray() {
            return array;
        }

        void overwrite() {
            Arrays.fill(array, new Element(-2));
        }
    }

    /// A list whose `toArray()` returns an array of a narrower class than `Object[]`, which cannot take an element of
    /// another type.
    private static final class TypedArrayList extends AbstractList<Object> {
        private final Element[] elements;

        TypedArrayList(Element[] elements) {
            this.elements = elements;
        }

        @Override
        public Object get(int index) {
            return elements[index];
        }

        @Override
        public int size() {
            return elements.length;
        }

        @Override
        public Object[] toArray() {
            return elements.clone();
        }
    }

    @Test
    public void ofAllOfASizedSourceIsTheVectorBuiltElementByElement() {
        for (int size : SIZES) {
            for (java.util.Map.Entry<String, Source> source : sources(elements(size))) {
                String where = source.getKey() + ", size " + size;
                Vector<Object> expected = reference(source.getValue().elements());
                Vector<Object> vector = Vector.ofAll(source.getValue().elements());
                assertBehavesAs(vector, expected, where);
                source.getValue().change().run();
                assertSameElements(vector, expected, where + " after the source changed");
            }
        }
    }

    @Test
    public void ofAllOfTheEntriesOfAConcurrentHashMapKeepsTheirValues() {
        for (int size : SIZES) {
            ConcurrentHashMap<Object, Object> map = mapOf(new ConcurrentHashMap<>(), elements(size));
            // the view gives new entries at each iteration: they are compared by key and value
            java.util.List<java.util.Map.Entry<Object, Object>> expected = new ArrayList<>();
            for (java.util.Map.Entry<Object, Object> entry : map.entrySet()) {
                expected.add(java.util.Map.entry(entry.getKey(), entry.getValue()));
            }
            Vector<java.util.Map.Entry<Object, Object>> vector = Vector.ofAll(map.entrySet());
            String where = "size " + size;
            assertThat(vector).as(where).containsExactlyElementsOf(expected);
            assertThat(vector.append(java.util.Map.entry(1, 2)).last()).isEqualTo(java.util.Map.entry(1, 2));
            map.replaceAll((key, value) -> new Element(-2));
            map.put(new Element(-3), new Element(-3));
            assertThat(vector).as(where + " after the map changed").containsExactlyElementsOf(expected);
            map.clear();
            assertThat(vector).as(where + " after the map was cleared").containsExactlyElementsOf(expected);
        }
    }

    /// A `Traversable` written elsewhere whose `toArray()` returns an array it keeps, and may write later: every
    /// method goes to a Zazr `List` but `toArray()`.
    @SuppressWarnings("unchecked")
    private static Traversable<Object> keepingItsArray(List<Object> elements, Object[] kept) {
        return (Traversable<Object>) java.lang.reflect.Proxy.newProxyInstance(
                Traversable.class.getClassLoader(), new Class<?>[] {Traversable.class}, (proxy, method, args) -> {
                    if (method.getName().equals("toArray") && method.getParameterCount() == 0) {
                        return kept;
                    }
                    try {
                        return method.invoke(elements, args);
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        throw e.getCause();
                    }
                });
    }

    @Test
    public void aTraversableIsReadIntoAnArrayOfItsOwnNotThroughItsToArray() {
        for (int size : SIZES) {
            // an Object[], which a vector could keep as its leaf
            Object[] kept = Arrays.copyOf(elements(size), size, Object[].class);
            List<Object> elements = List.of(kept.clone());
            Traversable<Object> source = keepingItsArray(elements, kept);
            assertThat(source.toArray()).isSameAs(kept);
            Vector<Object> expected = reference(elements);
            Vector<Object> vector = Vector.ofAll(source);
            String where = "size " + size;
            assertBehavesAs(vector, expected, where);
            Arrays.fill(kept, new Element(-2));
            assertSameElements(vector, expected, where + " after the kept array was written");
        }
    }

    @Test
    public void ofAllOfAnEmptySourceIsTheEmptyVector() {
        assertThat(Vector.ofAll(new ArrayList<>())).isSameAs(Vector.empty());
        assertThat(Vector.ofAll(java.util.List.of())).isSameAs(Vector.empty());
        assertThat(Vector.ofAll(List.empty())).isSameAs(Vector.empty());
    }

    @Test
    public void aNullElementIsRejected() {
        for (int size : new int[] {1, 32, 33}) {
            for (int at : new int[] {0, size - 1}) {
                ArrayList<Element> list = new ArrayList<>(Arrays.asList(elements(size)));
                list.set(at, null);
                assertThatThrownBy(() -> Vector.ofAll(list))
                        .as("size " + size + ", null at " + at)
                        .isInstanceOf(NullPointerException.class)
                        .hasMessage("Vector: element is null");
                LinkedList<Element> linked = new LinkedList<>(list);
                assertThatThrownBy(() -> Vector.ofAll(linked)).isInstanceOf(NullPointerException.class);
                LeakingList leaking = new LeakingList(list.toArray());
                assertThatThrownBy(() -> Vector.ofAll(leaking)).isInstanceOf(NullPointerException.class);
            }
        }
    }

    @Test
    public void ofKeepsNoReferenceToItsArray() {
        for (int size : SIZES) {
            Element[] array = elements(size);
            Vector<Element> expected = reference(Arrays.asList(array.clone()));
            Vector<Element> vector = Vector.of(array);
            Arrays.fill(array, new Element(-2));
            assertSameElements(vector, expected, "size " + size);
        }
    }

    // the elements of `size` in a shuffled order that depends on size only
    private static Element[] shuffled(int size) {
        Element[] elements = elements(size);
        java.util.Random random = new java.util.Random(size);
        for (int i = size - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            Element swapped = elements[i];
            elements[i] = elements[j];
            elements[j] = swapped;
        }
        return elements;
    }

    @Test
    public void sortedIsTheSortedArray() {
        for (int size : SIZES) {
            Element[] shuffled = shuffled(size);
            Vector<Element> vector = Vector.of(shuffled);
            Element[] ascending = shuffled.clone();
            Arrays.sort(ascending);
            Element[] descending = shuffled.clone();
            Arrays.sort(descending, Comparator.reverseOrder());
            String where = "size " + size;
            assertSameElements(vector.sorted(), reference(Arrays.asList(ascending)), where + " sorted");
            assertSameElements(
                    vector.sorted(Comparator.reverseOrder()),
                    reference(Arrays.asList(descending)),
                    where + " sorted in reverse");
            if (size > 1) {
                Vector<Object> sorted = vector.sorted().map(e -> (Object) e);
                assertBehavesAs(sorted, reference(Arrays.asList((Object[]) ascending)), where + " sorted");
            }
            assertSameElements(vector, reference(Arrays.asList(shuffled)), where + " left as it is");
        }
    }

    // the Vectors of `rows`, each built element by element
    private static <T> Vector<Vector<T>> rowsOf(Iterable<? extends Iterable<T>> rows) {
        Vector.Builder<Vector<T>> builder = Vector.newBuilder();
        for (Iterable<T> row : rows) {
            builder.add(reference(row));
        }
        return builder.result();
    }

    private static <T> void assertSameRows(Vector<Vector<T>> actual, Vector<Vector<T>> expected, String where) {
        assertThat(actual.size()).as(where).isEqualTo(expected.size());
        for (int i = 0; i < expected.size(); i++) {
            assertSameElements(actual.get(i), expected.get(i), where + " row " + i);
        }
    }

    @Test
    public void combinationsArePositionSelections() {
        Element[] elements = elements(34);
        Vector<Element> vector = Vector.of(elements);
        for (int k : new int[] {1, 2, 31, 32, 33, 34}) {
            Vector<Vector<Element>> combinations = vector.combinations(k);
            Vector<Vector<Element>> expected = rowsOf(List.of(elements).combinations(k));
            assertSameRows(combinations, expected, "k " + k);
            Vector<Element> first = combinations.get(0);
            assertSameElements(first.append(elements[0]), expected.get(0).append(elements[0]), "k " + k + " append");
            assertSameElements(first.update(0, elements[33]), expected.get(0).update(0, elements[33]), "k " + k);
        }
    }

    @Test
    public void permutationsAreTheOrderings() {
        for (int n : new int[] {2, 3, 5}) {
            Element[] elements = elements(n);
            Vector<Vector<Element>> permutations = Vector.of(elements).permutations();
            assertSameRows(permutations, rowsOf(List.of(elements).permutations()), "n " + n);
            assertSameElements(
                    permutations.get(1).append(elements[0]),
                    reference(List.of(elements).permutations().get(1).append(elements[0])),
                    "n " + n + " append");
        }
    }

    @Test
    public void transposeSwapsRowsAndColumns() {
        for (int[] shape : new int[][] {{2, 1}, {2, 31}, {3, 32}, {2, 33}, {33, 2}, {40, 40}}) {
            int height = shape[0];
            int width = shape[1];
            IntFunction<Vector<Element>> row = r -> Vector.tabulate(width, c -> new Element(r * width + c));
            Vector<Vector<Element>> matrix = Vector.tabulate(height, row::apply);
            Vector<Vector<Element>> transposed = Vector.transpose(matrix);
            Function<Integer, Vector<Element>> column =
                    c -> Vector.tabulate(height, r -> matrix.get(r).get(c));
            Vector<Vector<Element>> expected = Vector.tabulate(width, column);
            String where = height + "x" + width;
            assertSameRows(transposed, expected, where);
            Vector<Element> first = transposed.get(0);
            assertSameElements(first.append(first.get(0)), expected.get(0).append(first.get(0)), where + " append");
            assertSameRows(Vector.transpose(transposed), matrix, where + " back");
        }
    }
}
