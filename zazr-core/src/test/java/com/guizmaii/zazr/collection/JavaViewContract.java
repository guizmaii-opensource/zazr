package com.guizmaii.zazr.collection;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.SequencedCollection;
import java.util.SequencedMap;
import java.util.SequencedSet;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.Spliterator;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * A dependency-free contract suite for the read operations of the JDK collection interfaces, in the style of
 * Guava's testlib, run differentially: every operation is applied to the view under test and to a JDK collection
 * with the same content (the reference), and both must give the same result or fail with the same exception type.
 * Sub-views (sub-lists, reversed and descending views, head, tail and sub sets and maps, key sets, values, entry
 * sets) are checked the same way, recursively, down to {@link #MAX_DEPTH}.
 * <p>
 * Results are compared after {@link #normalize normalization}: a collection becomes a list in iteration order when
 * the view promises an order and a set otherwise, a map its list or set of entries, an iterator what it yields, an
 * array its elements and component type. Exceptions are compared by family (the exception types the JDK contracts
 * name), so that an {@code ArrayIndexOutOfBoundsException} and an {@code IndexOutOfBoundsException} agree.
 */
final class JavaViewContract {

    /** Sub-views of sub-views are checked; a third level is not. */
    static final int MAX_DEPTH = 2;

    private static final java.util.List<Class<? extends RuntimeException>> FAMILIES = java.util.List.of(
            UnsupportedOperationException.class, NoSuchElementException.class, IndexOutOfBoundsException.class,
            IllegalArgumentException.class, NullPointerException.class, ClassCastException.class, IllegalStateException.class);

    /** A probe of a type the elements are not, for {@code contains} and the lookups. */
    static final Object FOREIGN = "not an element";

    private final boolean ordered;
    private int checks;

    private JavaViewContract(boolean ordered) {
        this.ordered = ordered;
    }

    static JavaViewContract ordered() {
        return new JavaViewContract(true);
    }

    static JavaViewContract unordered() {
        return new JavaViewContract(false);
    }

    int checks() {
        return checks;
    }

    // -- the differential core

    private record Outcome(Object value, String failure) {
        static Outcome of(Supplier<?> operation, boolean ordered) {
            try {
                return new Outcome(normalize(operation.get(), ordered), null);
            } catch (RuntimeException e) {
                return new Outcome(null, family(e));
            }
        }
    }

    private static String family(RuntimeException e) {
        for (Class<? extends RuntimeException> family : FAMILIES) {
            if (family.isInstance(e)) {
                return family.getSimpleName();
            }
        }
        return e.getClass().getName();
    }

    /** The view and the reference give the same result, or fail with the same exception type. */
    void same(String path, String operation, Supplier<?> actual, Supplier<?> expected) {
        checks++;
        final Outcome a = Outcome.of(actual, ordered);
        final Outcome e = Outcome.of(expected, ordered);
        if (!Objects.equals(a, e)) {
            fail(path + "." + operation + ": expected " + describe(e) + " but was " + describe(a));
        }
    }

    /** Whether both give a value (so that a sub-view can be checked recursively). */
    private static boolean bothSucceed(Supplier<?> actual, Supplier<?> expected) {
        try {
            actual.get();
            expected.get();
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static String describe(Outcome outcome) {
        if (outcome.failure() != null) {
            return "a " + outcome.failure();
        }
        final String value = String.valueOf(outcome.value());
        return value.length() > 300 ? value.substring(0, 300) + "..." : value;
    }

    static Object normalize(Object value, boolean ordered) {
        if (value == null) {
            return null;
        } else if (value instanceof java.util.Map.Entry<?, ?> entry) {
            return Arrays.asList("entry", entry.getKey(), entry.getValue());
        } else if (value instanceof java.util.Map<?, ?> map) {
            return normalize(map.entrySet(), ordered);
        } else if (value instanceof Collection<?> collection) {
            return normalizeAll(collection.iterator(), ordered);
        } else if (value instanceof java.util.Iterator<?> iterator) {
            return normalizeAll(iterator, ordered);
        } else if (value instanceof Object[] array) {
            return Arrays.asList(array.getClass().getComponentType(), normalizeAll(Arrays.asList(array).iterator(), ordered));
        } else {
            return value;
        }
    }

    private static Object normalizeAll(java.util.Iterator<?> iterator, boolean ordered) {
        final Collection<Object> result = ordered ? new ArrayList<>() : new HashSet<>();
        int count = 0;
        while (iterator.hasNext()) {
            result.add(normalize(iterator.next(), ordered));
            count++;
        }
        return ordered ? result : Arrays.asList(count, result);
    }

    // -- java.util.Collection

    <E> void collection(String path, Collection<E> view, Collection<E> reference, java.util.List<Object> probes) {
        collection(path, view, reference, probes, true);
    }

    /**
     * @param containsAllOfItself whether to check {@code containsAll} of every element, which is quadratic where
     *                            {@code contains} is linear (the values of a map)
     */
    <E> void collection(String path, Collection<E> view, Collection<E> reference, java.util.List<Object> probes, boolean containsAllOfItself) {
        same(path, "size()", view::size, reference::size);
        same(path, "isEmpty()", view::isEmpty, reference::isEmpty);
        same(path, "iterator()", view::iterator, reference::iterator);
        same(path, "iterator() past the end", () -> exhaust(view.iterator()), () -> exhaust(reference.iterator()));
        same(path, "forEach", () -> {
            final java.util.List<E> seen = new ArrayList<>();
            view.forEach(seen::add);
            return seen;
        }, () -> {
            final java.util.List<E> seen = new ArrayList<>();
            reference.forEach(seen::add);
            return seen;
        });
        same(path, "iterator().forEachRemaining", () -> {
            final java.util.List<E> seen = new ArrayList<>();
            final java.util.Iterator<E> iterator = view.iterator();
            if (iterator.hasNext()) {
                iterator.next();
            }
            iterator.forEachRemaining(seen::add);
            return seen;
        }, () -> {
            final java.util.List<E> seen = new ArrayList<>();
            final java.util.Iterator<E> iterator = reference.iterator();
            if (iterator.hasNext()) {
                iterator.next();
            }
            iterator.forEachRemaining(seen::add);
            return seen;
        });
        same(path, "stream()", () -> view.stream().toList(), () -> reference.stream().toList());
        same(path, "parallelStream().count()", () -> view.parallelStream().count(), () -> reference.parallelStream().count());
        same(path, "spliterator().getExactSizeIfKnown() or size", () -> sizeOf(view.spliterator()), () -> (long) reference.size());
        same(path, "toArray()", view::toArray, reference::toArray);
        same(path, "toArray(new Object[0])", () -> view.toArray(new Object[0]), () -> reference.toArray(new Object[0]));
        same(path, "toArray(Integer[0])", () -> view.toArray(new Integer[0]), () -> reference.toArray(new Integer[0]));
        same(path, "toArray(bigger array)", () -> fillAndCopy(view, reference.size()), () -> fillAndCopy(reference, reference.size()));
        same(path, "toArray(same size array)", () -> view.toArray(new Object[reference.size()]), () -> reference.toArray(new Object[reference.size()]));
        same(path, "toArray(IntFunction)", () -> view.toArray(Object[]::new), () -> reference.toArray(Object[]::new));
        same(path, "toArray((Object[]) null)", () -> view.toArray((Object[]) null), () -> reference.toArray((Object[]) null));
        for (Object probe : probes) {
            same(path, "contains(" + probe + ")", () -> view.contains(probe), () -> reference.contains(probe));
        }
        if (containsAllOfItself) {
            same(path, "containsAll(itself)", () -> view.containsAll(reference), () -> reference.containsAll(reference));
        }
        same(path, "containsAll(empty)", () -> view.containsAll(java.util.List.of()), () -> reference.containsAll(java.util.List.of()));
        same(path, "containsAll(probes)", () -> view.containsAll(nonNull(probes)), () -> reference.containsAll(nonNull(probes)));
        same(path, "containsAll(null)", () -> view.containsAll(null), () -> reference.containsAll(null));
        if (ordered) {
            same(path, "toString()", view::toString, reference::toString);
        }
    }

    private static java.util.List<Object> nonNull(java.util.List<Object> probes) {
        final java.util.List<Object> result = new ArrayList<>();
        for (Object probe : probes) {
            if (probe != null && probe != FOREIGN) {
                result.add(probe);
            }
        }
        return result;
    }

    private static long sizeOf(Spliterator<?> spliterator) {
        final long exact = spliterator.getExactSizeIfKnown();
        if (exact >= 0) {
            return exact;
        }
        final long[] count = { 0 };
        spliterator.forEachRemaining(ignored -> count[0]++);
        return count[0];
    }

    private static String exhaust(java.util.Iterator<?> iterator) {
        while (iterator.hasNext()) {
            iterator.next();
        }
        try {
            iterator.next();
            return "no exception";
        } catch (NoSuchElementException e) {
            return "NoSuchElementException";
        }
    }

    private static Object[] fillAndCopy(Collection<?> collection, int size) {
        final Object[] array = new Object[size + 2];
        Arrays.fill(array, "sentinel");
        return collection.toArray(array);
    }

    // -- java.util.List

    <E> void list(String path, java.util.List<E> view, java.util.List<E> reference, java.util.List<Object> probes, int depth) {
        collection(path, view, reference, probes);
        final int n = reference.size();
        final int[] indexes = distinct(-1, 0, 1, n / 2, n - 1, n, n + 1);
        for (int i : indexes) {
            same(path, "get(" + i + ")", () -> view.get(i), () -> reference.get(i));
        }
        for (Object probe : probes) {
            same(path, "indexOf(" + probe + ")", () -> view.indexOf(probe), () -> reference.indexOf(probe));
            same(path, "lastIndexOf(" + probe + ")", () -> view.lastIndexOf(probe), () -> reference.lastIndexOf(probe));
        }
        same(path, "getFirst()", view::getFirst, reference::getFirst);
        same(path, "getLast()", view::getLast, reference::getLast);
        same(path, "listIterator()", () -> walk(view.listIterator()), () -> walk(reference.listIterator()));
        for (int i : indexes) {
            same(path, "listIterator(" + i + ")", () -> walk(view.listIterator(i)), () -> walk(reference.listIterator(i)));
        }
        equality(path, view, reference, new LinkedList<>(reference), new LinkedHashSet<>(reference));
        for (int from : indexes) {
            for (int to : indexes) {
                same(path, "subList(" + from + ", " + to + ")", () -> view.subList(from, to), () -> reference.subList(from, to));
                if (depth < MAX_DEPTH && (depth == 0 || from == 1 || to == n - 1) && bothSucceed(() -> view.subList(from, to), () -> reference.subList(from, to))) {
                    list(path + ".subList(" + from + ", " + to + ")", view.subList(from, to), reference.subList(from, to), probes, depth + 1);
                }
            }
        }
        if (depth < MAX_DEPTH) {
            list(path + ".reversed()", view.reversed(), reference.reversed(), probes, depth + 1);
        }
    }

    /** Every move of a list iterator, forward to the end and back to the start, with the indexes it reports. */
    private static java.util.List<Object> walk(ListIterator<?> iterator) {
        final java.util.List<Object> steps = new ArrayList<>();
        steps.add(iterator.nextIndex());
        steps.add(iterator.previousIndex());
        while (iterator.hasNext()) {
            steps.add(iterator.next());
            steps.add(iterator.nextIndex());
        }
        steps.add(attempt(iterator::next));
        while (iterator.hasPrevious()) {
            steps.add(iterator.previous());
            steps.add(iterator.previousIndex());
        }
        steps.add(attempt(iterator::previous));
        if (iterator.hasNext()) {
            steps.add(iterator.next());
            steps.add(iterator.previous());
        }
        return steps;
    }

    private static Object attempt(Supplier<?> operation) {
        try {
            return operation.get();
        } catch (RuntimeException e) {
            return family(e);
        }
    }

    private static int[] distinct(int... values) {
        return Arrays.stream(values).distinct().toArray();
    }

    // -- equality

    /**
     * {@code equals} both ways with the reference and with an equal collection of another class, {@code hashCode},
     * and inequality with a collection of the other kind (a list is never equal to a set).
     */
    <E> void equality(String path, Object view, Object reference, Object equalOtherClass, Object otherKind) {
        same(path, "equals(reference)", () -> view.equals(reference), () -> reference.equals(reference));
        same(path, "reference.equals(view)", () -> reference.equals(view), () -> reference.equals(reference));
        same(path, "equals(other class)", () -> view.equals(equalOtherClass), () -> reference.equals(equalOtherClass));
        same(path, "equals(itself)", () -> view.equals(view), () -> reference.equals(reference));
        same(path, "equals(null)", () -> view.equals(null), () -> reference.equals(null));
        same(path, "equals(other kind)", () -> view.equals(otherKind), () -> reference.equals(otherKind));
        same(path, "hashCode()", view::hashCode, reference::hashCode);
    }

    // -- java.util.Set and its sequenced, sorted and navigable kinds

    <E> void set(String path, java.util.Set<E> view, java.util.Set<E> reference, java.util.List<Object> probes) {
        collection(path, view, reference, probes);
        equality(path, view, reference, new HashSet<>(reference), new ArrayList<>(reference));
    }

    <E> void sequencedSet(String path, SequencedSet<E> view, SequencedSet<E> reference, java.util.List<Object> probes, int depth) {
        set(path, view, reference, probes);
        same(path, "getFirst()", view::getFirst, reference::getFirst);
        same(path, "getLast()", view::getLast, reference::getLast);
        if (depth < MAX_DEPTH) {
            sequencedSet(path + ".reversed()", view.reversed(), reference.reversed(), probes, depth + 1);
        }
    }

    <E> void navigableSet(String path, NavigableSet<E> view, NavigableSet<E> reference, java.util.List<Object> probes, java.util.List<Object> bounds, int depth) {
        set(path, view, reference, probes);
        same(path, "comparator()", view::comparator, reference::comparator);
        same(path, "first()", view::first, reference::first);
        same(path, "last()", view::last, reference::last);
        same(path, "getFirst()", view::getFirst, reference::getFirst);
        same(path, "getLast()", view::getLast, reference::getLast);
        same(path, "descendingIterator()", view::descendingIterator, reference::descendingIterator);
        spliteratorReportsTheComparator(path, view);
        for (Object probe : probes) {
            @SuppressWarnings("unchecked") final E key = (E) probe;
            same(path, "lower(" + probe + ")", () -> view.lower(key), () -> reference.lower(key));
            same(path, "floor(" + probe + ")", () -> view.floor(key), () -> reference.floor(key));
            same(path, "ceiling(" + probe + ")", () -> view.ceiling(key), () -> reference.ceiling(key));
            same(path, "higher(" + probe + ")", () -> view.higher(key), () -> reference.higher(key));
        }
        if (depth >= MAX_DEPTH) {
            return;
        }
        navigableSet(path + ".descendingSet()", view.descendingSet(), reference.descendingSet(), probes, bounds, depth + 1);
        same(path, "reversed()", view::reversed, reference::reversed);
        final java.util.List<Object> subBounds = depth == 0 ? bounds : bounds.subList(0, Math.min(3, bounds.size()));
        for (Object from : subBounds) {
            @SuppressWarnings("unchecked") final E lo = (E) from;
            for (boolean fromInclusive : new boolean[] { true, false }) {
                final String head = "headSet(" + from + ", " + fromInclusive + ")";
                subSet(path, head, () -> view.headSet(lo, fromInclusive), () -> reference.headSet(lo, fromInclusive), probes, bounds, depth);
                final String tail = "tailSet(" + from + ", " + fromInclusive + ")";
                subSet(path, tail, () -> view.tailSet(lo, fromInclusive), () -> reference.tailSet(lo, fromInclusive), probes, bounds, depth);
                for (Object to : subBounds) {
                    @SuppressWarnings("unchecked") final E hi = (E) to;
                    for (boolean toInclusive : new boolean[] { true, false }) {
                        if (depth > 0 && toInclusive != fromInclusive) {
                            continue; // the nested sub-views take the two same-kind bound pairs only
                        }
                        final String sub = "subSet(" + from + ", " + fromInclusive + ", " + to + ", " + toInclusive + ")";
                        subSet(path, sub, () -> view.subSet(lo, fromInclusive, hi, toInclusive), () -> reference.subSet(lo, fromInclusive, hi, toInclusive), probes, bounds, depth);
                    }
                }
            }
            same(path, "headSet(" + from + ")", () -> view.headSet(lo), () -> reference.headSet(lo));
            same(path, "tailSet(" + from + ")", () -> view.tailSet(lo), () -> reference.tailSet(lo));
            for (Object to : subBounds) {
                @SuppressWarnings("unchecked") final E hi = (E) to;
                same(path, "subSet(" + from + ", " + to + ")", () -> view.subSet(lo, hi), () -> reference.subSet(lo, hi));
                if (bothSucceed(() -> view.subSet(lo, hi), () -> reference.subSet(lo, hi))) {
                    sortedSet(path + ".subSet(" + from + ", " + to + ")", view.subSet(lo, hi), reference.subSet(lo, hi), probes);
                }
            }
        }
    }

    private <E> void subSet(String path, String operation, Supplier<NavigableSet<E>> view, Supplier<NavigableSet<E>> reference,
                            java.util.List<Object> probes, java.util.List<Object> bounds, int depth) {
        same(path, operation, view::get, reference::get);
        if (bothSucceed(view, reference)) {
            navigableSet(path + "." + operation, view.get(), reference.get(), probes, bounds, depth + 1);
        }
    }

    /** The {@link SortedSet} reads of the sub-set views the one-bound overloads return. */
    private <E> void sortedSet(String path, SortedSet<E> view, SortedSet<E> reference, java.util.List<Object> probes) {
        set(path, view, reference, probes);
        same(path, "comparator()", view::comparator, reference::comparator);
        same(path, "first()", view::first, reference::first);
        same(path, "last()", view::last, reference::last);
    }

    private void spliteratorReportsTheComparator(String path, SortedSet<?> view) {
        checks++;
        final Spliterator<?> spliterator = view.spliterator();
        if (!spliterator.hasCharacteristics(Spliterator.SORTED) || !Objects.equals(spliterator.getComparator(), view.comparator())) {
            fail(path + ".spliterator(): not SORTED by comparator() " + view.comparator());
        }
    }

    // -- java.util.Map and its sequenced, sorted and navigable kinds

    @SuppressWarnings("unchecked")
    <K, V> void map(String path, java.util.Map<K, V> view, java.util.Map<K, V> reference, java.util.List<Object> probes, java.util.List<Object> values) {
        same(path, "size()", view::size, reference::size);
        same(path, "isEmpty()", view::isEmpty, reference::isEmpty);
        same(path, "entrySet() iteration", view::entrySet, reference::entrySet);
        for (Object probe : probes) {
            same(path, "get(" + probe + ")", () -> view.get(probe), () -> reference.get(probe));
            same(path, "containsKey(" + probe + ")", () -> view.containsKey(probe), () -> reference.containsKey(probe));
            same(path, "getOrDefault(" + probe + ")", () -> view.getOrDefault(probe, null), () -> reference.getOrDefault(probe, null));
            same(path, "getOrDefault(" + probe + ", default)", () -> view.getOrDefault(probe, (V) "default"), () -> reference.getOrDefault(probe, (V) "default"));
        }
        for (Object value : values) {
            same(path, "containsValue(" + value + ")", () -> view.containsValue(value), () -> reference.containsValue(value));
        }
        same(path, "forEach", () -> {
            final java.util.List<Object> seen = new ArrayList<>();
            view.forEach((k, v) -> seen.add(Arrays.asList(k, v)));
            return seen;
        }, () -> {
            final java.util.List<Object> seen = new ArrayList<>();
            reference.forEach((k, v) -> seen.add(Arrays.asList(k, v)));
            return seen;
        });
        if (ordered) {
            same(path, "toString()", view::toString, reference::toString);
        }
        equality(path, view, reference, new java.util.HashMap<>(reference), new ArrayList<>(reference.entrySet()));
        final java.util.List<Object> entryProbes = entryProbes(reference, probes);
        set(path + ".entrySet()", view.entrySet(), reference.entrySet(), entryProbes);
        set(path + ".keySet()", view.keySet(), reference.keySet(), probes);
        collection(path + ".values()", view.values(), reference.values(), values, reference.size() <= 64);
    }

    /** Entries of the map, entries with a present key and another value, with an absent key, and non-entries. */
    private static <K, V> java.util.List<Object> entryProbes(java.util.Map<K, V> reference, java.util.List<Object> probes) {
        final java.util.List<Object> result = new ArrayList<>();
        for (Object probe : probes) {
            result.add(new AbstractMap.SimpleImmutableEntry<>(probe, "v" + probe));
            result.add(new AbstractMap.SimpleImmutableEntry<>(probe, "other"));
        }
        if (!reference.isEmpty()) {
            final java.util.Map.Entry<K, V> first = reference.entrySet().iterator().next();
            result.add(new AbstractMap.SimpleImmutableEntry<>(first.getKey(), first.getValue()));
            result.add(new AbstractMap.SimpleImmutableEntry<>(first.getKey(), null));
        }
        result.add(null);
        result.add(FOREIGN);
        return result;
    }

    <K, V> void sequencedMap(String path, SequencedMap<K, V> view, SequencedMap<K, V> reference, java.util.List<Object> probes, java.util.List<Object> values, int depth) {
        map(path, view, reference, probes, values);
        sequencedReads(path, view, reference, probes, values, depth);
        same(path, "values() is sequenced", () -> view.values() instanceof SequencedCollection, () -> reference.values() instanceof SequencedCollection);
        if (depth < MAX_DEPTH) {
            sequencedMap(path + ".reversed()", view.reversed(), reference.reversed(), probes, values, depth + 1);
        }
    }

    private <K, V> void sequencedReads(String path, SequencedMap<K, V> view, SequencedMap<K, V> reference, java.util.List<Object> probes, java.util.List<Object> values, int depth) {
        same(path, "firstEntry()", view::firstEntry, reference::firstEntry);
        same(path, "lastEntry()", view::lastEntry, reference::lastEntry);
        sequencedCollection(path + ".sequencedValues()", view.sequencedValues(), reference.sequencedValues(), values, depth, reference.size() <= 64);
        sequencedCollection(path + ".sequencedKeySet()", view.sequencedKeySet(), reference.sequencedKeySet(), probes, depth, true);
        sequencedCollection(path + ".sequencedEntrySet()", view.sequencedEntrySet(), reference.sequencedEntrySet(), entryProbes(reference, probes), depth, true);
        same(path, "keySet() is sequenced", () -> view.keySet() instanceof SequencedSet, () -> reference.keySet() instanceof SequencedSet);
    }

    private <E> void sequencedCollection(String path, SequencedCollection<E> view, SequencedCollection<E> reference, java.util.List<Object> probes, int depth,
                                         boolean containsAllOfItself) {
        collection(path, view, reference, probes, containsAllOfItself);
        same(path, "getFirst()", view::getFirst, reference::getFirst);
        same(path, "getLast()", view::getLast, reference::getLast);
        if (depth < MAX_DEPTH) {
            collection(path + ".reversed()", view.reversed(), reference.reversed(), probes, containsAllOfItself);
            same(path, "reversed().getFirst()", () -> view.reversed().getFirst(), () -> reference.reversed().getFirst());
            same(path, "reversed().reversed()", () -> view.reversed().reversed(), () -> reference.reversed().reversed());
        }
    }

    <K, V> void navigableMap(String path, NavigableMap<K, V> view, NavigableMap<K, V> reference, java.util.List<Object> probes, java.util.List<Object> values,
                             java.util.List<Object> bounds, int depth) {
        map(path, view, reference, probes, values);
        sequencedReads(path, view, reference, probes, values, depth);
        same(path, "comparator()", view::comparator, reference::comparator);
        same(path, "firstKey()", view::firstKey, reference::firstKey);
        same(path, "lastKey()", view::lastKey, reference::lastKey);
        for (Object probe : probes) {
            @SuppressWarnings("unchecked") final K key = (K) probe;
            same(path, "lowerEntry(" + probe + ")", () -> view.lowerEntry(key), () -> reference.lowerEntry(key));
            same(path, "lowerKey(" + probe + ")", () -> view.lowerKey(key), () -> reference.lowerKey(key));
            same(path, "floorEntry(" + probe + ")", () -> view.floorEntry(key), () -> reference.floorEntry(key));
            same(path, "floorKey(" + probe + ")", () -> view.floorKey(key), () -> reference.floorKey(key));
            same(path, "ceilingEntry(" + probe + ")", () -> view.ceilingEntry(key), () -> reference.ceilingEntry(key));
            same(path, "ceilingKey(" + probe + ")", () -> view.ceilingKey(key), () -> reference.ceilingKey(key));
            same(path, "higherEntry(" + probe + ")", () -> view.higherEntry(key), () -> reference.higherEntry(key));
            same(path, "higherKey(" + probe + ")", () -> view.higherKey(key), () -> reference.higherKey(key));
        }
        if (depth >= MAX_DEPTH) {
            return;
        }
        // the key sets share the map's tree range: their own sub-views are those of the sets, checked from depth 0 only
        final int keySetDepth = depth == 0 ? depth + 1 : MAX_DEPTH;
        navigableSet(path + ".navigableKeySet()", view.navigableKeySet(), reference.navigableKeySet(), probes, bounds, keySetDepth);
        navigableSet(path + ".descendingKeySet()", view.descendingKeySet(), reference.descendingKeySet(), probes, bounds, keySetDepth);
        same(path, "keySet() is navigable", () -> view.keySet() instanceof NavigableSet, () -> reference.keySet() instanceof NavigableSet);
        navigableMap(path + ".descendingMap()", view.descendingMap(), reference.descendingMap(), probes, values, bounds, depth + 1);
        same(path, "reversed()", view::reversed, reference::reversed);
        final java.util.List<Object> subBounds = depth == 0 ? bounds : bounds.subList(0, Math.min(3, bounds.size()));
        for (Object from : subBounds) {
            @SuppressWarnings("unchecked") final K lo = (K) from;
            for (boolean fromInclusive : new boolean[] { true, false }) {
                final String head = "headMap(" + from + ", " + fromInclusive + ")";
                subMap(path, head, () -> view.headMap(lo, fromInclusive), () -> reference.headMap(lo, fromInclusive), probes, values, bounds, depth);
                final String tail = "tailMap(" + from + ", " + fromInclusive + ")";
                subMap(path, tail, () -> view.tailMap(lo, fromInclusive), () -> reference.tailMap(lo, fromInclusive), probes, values, bounds, depth);
                for (Object to : subBounds) {
                    @SuppressWarnings("unchecked") final K hi = (K) to;
                    for (boolean toInclusive : new boolean[] { true, false }) {
                        if (depth > 0 && toInclusive != fromInclusive) {
                            continue; // the nested sub-views take the two same-kind bound pairs only
                        }
                        final String sub = "subMap(" + from + ", " + fromInclusive + ", " + to + ", " + toInclusive + ")";
                        subMap(path, sub, () -> view.subMap(lo, fromInclusive, hi, toInclusive), () -> reference.subMap(lo, fromInclusive, hi, toInclusive), probes, values, bounds, depth);
                    }
                }
            }
            same(path, "headMap(" + from + ")", () -> view.headMap(lo), () -> reference.headMap(lo));
            same(path, "tailMap(" + from + ")", () -> view.tailMap(lo), () -> reference.tailMap(lo));
            for (Object to : subBounds) {
                @SuppressWarnings("unchecked") final K hi = (K) to;
                same(path, "subMap(" + from + ", " + to + ")", () -> view.subMap(lo, hi), () -> reference.subMap(lo, hi));
                if (bothSucceed(() -> view.subMap(lo, hi), () -> reference.subMap(lo, hi))) {
                    sortedMap(path + ".subMap(" + from + ", " + to + ")", view.subMap(lo, hi), reference.subMap(lo, hi), probes, values);
                }
            }
        }
    }

    private <K, V> void subMap(String path, String operation, Supplier<NavigableMap<K, V>> view, Supplier<NavigableMap<K, V>> reference,
                               java.util.List<Object> probes, java.util.List<Object> values, java.util.List<Object> bounds, int depth) {
        same(path, operation, view::get, reference::get);
        if (bothSucceed(view, reference)) {
            navigableMap(path + "." + operation, view.get(), reference.get(), probes, values, bounds, depth + 1);
        }
    }

    private <K, V> void sortedMap(String path, SortedMap<K, V> view, SortedMap<K, V> reference, java.util.List<Object> probes, java.util.List<Object> values) {
        map(path, view, reference, probes, values);
        same(path, "comparator()", view::comparator, reference::comparator);
        same(path, "firstKey()", view::firstKey, reference::firstKey);
        same(path, "lastKey()", view::lastKey, reference::lastKey);
    }

    // -- fixtures

    /** The keys of the set and map fixtures: the even numbers {@code 0, 2, ..., 2(n - 1)}. */
    static java.util.List<Integer> evens(int n) {
        final java.util.List<Integer> result = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            result.add(2 * i);
        }
        return result;
    }

    /**
     * Present and absent keys at every boundary of {@link #evens(int)}, a {@code null} and a key of another type:
     * below the smallest, the smallest, between two keys, the middle, the largest, above the largest.
     */
    static java.util.List<Object> keyProbes(int n) {
        final int max = 2 * (n - 1);
        final int middle = 2 * (n / 2);
        final java.util.Set<Object> probes = new LinkedHashSet<>(java.util.List.of(-2, -1, 0, 1, 2, 3, middle - 1, middle, middle + 1, max - 1, max, max + 1, max + 2));
        final java.util.List<Object> result = new ArrayList<>(probes);
        result.add(null);
        result.add(FOREIGN);
        return result;
    }

    /** Bounds for the sub-views: outside, on and between the keys. The first three serve the nested sub-views. */
    static java.util.List<Object> bounds(int n) {
        final int max = 2 * (n - 1);
        final int middle = 2 * (n / 2);
        return new ArrayList<>(new LinkedHashSet<>(java.util.List.of(middle, -1, max + 1, 0, 1, max - 1, max)));
    }

    /** The values of the map fixtures, {@code "v" + key}, present and absent, and a {@code null}. */
    static java.util.List<Object> valueProbes(int n) {
        final java.util.List<Object> result = new ArrayList<>();
        for (Object key : keyProbes(n)) {
            if (key instanceof Integer) {
                result.add("v" + key);
            }
        }
        result.add(null);
        result.add(FOREIGN);
        return result;
    }

    /** Elements with repeats, so that {@code indexOf} and {@code lastIndexOf} differ: {@code 0, 0, 1, 1, 2, ...}. */
    static java.util.List<Integer> pairs(int n) {
        final java.util.List<Integer> result = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            result.add(i / 2);
        }
        return result;
    }

    static java.util.List<Object> elementProbes(int n) {
        final int max = (n - 1) / 2;
        final java.util.List<Object> result = new ArrayList<>(new LinkedHashSet<>(java.util.List.of(-1, 0, 1, max / 2, max, max + 1)));
        result.add(null);
        result.add(FOREIGN);
        return result;
    }

    /** The comparators the sorted fixtures are built with: the natural order and its reverse. */
    static java.util.List<Comparator<Integer>> comparators() {
        return java.util.List.of(Comparator.naturalOrder(), Comparator.reverseOrder());
    }

    static final int[] SIZES = { 0, 1, 32, 33, 1025 };
}
