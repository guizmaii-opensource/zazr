package com.guizmaii.zazr.collection;

import com.guizmaii.zazr.Tuple;
import java.util.Collection;
import java.util.Comparator;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.SequencedCollection;
import java.util.SequencedMap;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every mutator of every view, and of every view a view hands out (sub-lists, reversed and descending views, head,
 * tail and sub sets and maps, key sets, values, entry sets, iterators, list iterators, entries), throws
 * {@link UnsupportedOperationException}, including when the call would change nothing: on an empty view, with an
 * absent element, an empty argument or a predicate that matches nothing.
 */
class JavaViewMutatorTest {

    private static final int[] SIZES = { 0, 1, 3 };

    private final java.util.List<String> failures = new java.util.ArrayList<>();

    private void refused(String what, Runnable mutator) {
        try {
            mutator.run();
            failures.add(what + ": no exception");
        } catch (UnsupportedOperationException expected) {
            // refused
        } catch (RuntimeException e) {
            failures.add(what + ": " + e.getClass().getName());
        }
    }

    private void assertAllRefused() {
        assertThat(failures).isEmpty();
    }

    // -- the mutators of each interface

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void collection(String name, Collection<?> elements) {
        final Collection view = elements;
        final java.util.List<Object> copy = new java.util.ArrayList<>(view);
        refused(name + ".add", () -> view.add(1));
        refused(name + ".addAll(empty)", () -> view.addAll(java.util.List.of()));
        refused(name + ".addAll", () -> view.addAll(java.util.List.of(1, 2)));
        refused(name + ".remove(absent)", () -> view.remove(-7));
        refused(name + ".remove(null)", () -> view.remove(null));
        copy.stream().findFirst().ifPresent(present -> refused(name + ".remove(present)", () -> view.remove(present)));
        refused(name + ".removeAll(empty)", () -> view.removeAll(java.util.List.of()));
        refused(name + ".removeAll", () -> view.removeAll(copy));
        refused(name + ".removeIf(nothing)", () -> view.removeIf(x -> false));
        refused(name + ".removeIf(everything)", () -> view.removeIf(x -> true));
        refused(name + ".retainAll(itself)", () -> view.retainAll(copy));
        refused(name + ".retainAll(empty)", () -> view.retainAll(java.util.List.of()));
        refused(name + ".clear", view::clear);
        iterator(name + ".iterator()", view.iterator());
        final java.util.Iterator<?> advanced = view.iterator();
        if (advanced.hasNext()) {
            advanced.next();
            iterator(name + ".iterator() after next()", advanced);
        }
        if (view instanceof SequencedCollection<?> sequenced) {
            sequenced(name, sequenced);
        }
    }

    private void iterator(String name, java.util.Iterator<?> iterator) {
        refused(name + ".remove", iterator::remove);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void sequenced(String name, SequencedCollection<?> elements) {
        final SequencedCollection view = elements;
        refused(name + ".addFirst", () -> view.addFirst(1));
        refused(name + ".addLast", () -> view.addLast(1));
        refused(name + ".removeFirst", view::removeFirst);
        refused(name + ".removeLast", view::removeLast);
    }

    private void list(String name, java.util.List<Integer> view, int depth) {
        collection(name, view);
        refused(name + ".add(0, x)", () -> view.add(0, 1));
        refused(name + ".addAll(0, empty)", () -> view.addAll(0, java.util.List.of()));
        refused(name + ".remove(0)", () -> view.remove(0));
        refused(name + ".set(0, x)", () -> view.set(0, 1));
        refused(name + ".replaceAll(identity)", () -> view.replaceAll(UnaryOperator.identity()));
        refused(name + ".sort(null)", () -> view.sort(null));
        refused(name + ".sort(natural)", () -> view.sort(Comparator.naturalOrder()));
        for (java.util.ListIterator<Integer> iterator : java.util.List.of(view.listIterator(), view.listIterator(view.size()))) {
            refused(name + ".listIterator().add", () -> iterator.add(1));
            refused(name + ".listIterator().set", () -> iterator.set(1));
            refused(name + ".listIterator().remove", iterator::remove);
            if (iterator.hasPrevious()) {
                iterator.previous();
                refused(name + ".listIterator() after previous().set", () -> iterator.set(1));
                refused(name + ".listIterator() after previous().remove", iterator::remove);
            }
        }
        if (depth < 2) {
            list(name + ".subList(0, size)", view.subList(0, view.size()), depth + 1);
            list(name + ".reversed()", view.reversed(), depth + 1);
        }
    }

    private void set(String name, java.util.Set<Integer> view, int depth) {
        collection(name, view);
        if (view instanceof java.util.SequencedSet<Integer> sequenced && depth < 2) {
            set(name + ".reversed()", sequenced.reversed(), depth + 1);
        }
        if (view instanceof NavigableSet<Integer> navigable) {
            refused(name + ".pollFirst", navigable::pollFirst);
            refused(name + ".pollLast", navigable::pollLast);
            if (depth < 2) {
                set(name + ".descendingSet()", navigable.descendingSet(), depth + 1);
                set(name + ".headSet(1)", navigable.headSet(1, true), depth + 1);
                set(name + ".tailSet(1)", navigable.tailSet(1, true), depth + 1);
                // 1 is inside every range this test makes, so the nested sub-views are within their parent's bounds
                if (depth == 0) {
                    final boolean ascending = navigable.comparator() == null || navigable.comparator().compare(-10, 10) < 0;
                    set(name + ".subSet(-10, 10)", ascending ? navigable.subSet(-10, true, 10, true) : navigable.subSet(10, true, -10, true), depth + 1);
                } else {
                    set(name + ".subSet(1, 1)", navigable.subSet(1, true, 1, true), depth + 1);
                }
            }
        }
    }

    private void map(String name, java.util.Map<Integer, String> view, int depth) {
        refused(name + ".put", () -> view.put(1, "a"));
        refused(name + ".putAll(empty)", () -> view.putAll(java.util.Map.of()));
        refused(name + ".putAll", () -> view.putAll(java.util.Map.of(1, "a")));
        refused(name + ".remove(absent)", () -> view.remove(-7));
        refused(name + ".remove(absent, value)", () -> view.remove(-7, "a"));
        refused(name + ".clear", view::clear);
        refused(name + ".putIfAbsent", () -> view.putIfAbsent(1, "a"));
        refused(name + ".replace(absent, v)", () -> view.replace(-7, "a"));
        refused(name + ".replace(absent, old, new)", () -> view.replace(-7, "a", "b"));
        refused(name + ".replaceAll(identity)", () -> view.replaceAll((k, v) -> v));
        refused(name + ".computeIfAbsent(to null)", () -> view.computeIfAbsent(-7, k -> null));
        refused(name + ".computeIfPresent(absent)", () -> view.computeIfPresent(-7, (k, v) -> v));
        refused(name + ".compute(to null)", () -> view.compute(-7, (k, v) -> null));
        refused(name + ".merge", () -> view.merge(-7, "a", (a, b) -> a));
        for (Integer key : view.keySet()) {
            refused(name + ".remove(present)", () -> view.remove(key));
            refused(name + ".computeIfPresent(present)", () -> view.computeIfPresent(key, (k, v) -> v));
            refused(name + ".replace(present, same)", () -> view.replace(key, view.get(key)));
            break;
        }
        collection(name + ".keySet()", view.keySet());
        collection(name + ".values()", view.values());
        entries(name + ".entrySet()", view.entrySet());
        if (view instanceof SequencedMap<Integer, String> sequenced) {
            refused(name + ".putFirst", () -> sequenced.putFirst(1, "a"));
            refused(name + ".putLast", () -> sequenced.putLast(1, "a"));
            refused(name + ".pollFirstEntry", sequenced::pollFirstEntry);
            refused(name + ".pollLastEntry", sequenced::pollLastEntry);
            if (sequenced.firstEntry() != null) {
                refused(name + ".firstEntry().setValue", () -> sequenced.firstEntry().setValue("b"));
                refused(name + ".lastEntry().setValue", () -> sequenced.lastEntry().setValue("b"));
            }
            collection(name + ".sequencedKeySet()", sequenced.sequencedKeySet());
            collection(name + ".sequencedValues()", sequenced.sequencedValues());
            entries(name + ".sequencedEntrySet()", sequenced.sequencedEntrySet());
            collection(name + ".sequencedKeySet().reversed()", sequenced.sequencedKeySet().reversed());
            entries(name + ".sequencedEntrySet().reversed()", sequenced.sequencedEntrySet().reversed());
            if (depth < 2) {
                map(name + ".reversed()", sequenced.reversed(), depth + 1);
            }
        }
        if (view instanceof NavigableMap<Integer, String> navigable) {
            for (java.util.Map.Entry<Integer, String> entry : java.util.Arrays.asList(navigable.ceilingEntry(0), navigable.floorEntry(10),
                    navigable.higherEntry(-10), navigable.lowerEntry(10))) {
                if (entry != null) {
                    refused(name + " navigation entry.setValue", () -> entry.setValue("b"));
                }
            }
            if (depth < 2) {
                set(name + ".navigableKeySet()", navigable.navigableKeySet(), depth + 1);
                set(name + ".descendingKeySet()", navigable.descendingKeySet(), depth + 1);
                map(name + ".descendingMap()", navigable.descendingMap(), depth + 1);
                map(name + ".headMap(1)", navigable.headMap(1, true), depth + 1);
                map(name + ".tailMap(1)", navigable.tailMap(1, true), depth + 1);
                // 1 is inside every range this test makes, so the nested sub-views are within their parent's bounds
                if (depth == 0) {
                    final boolean ascending = navigable.comparator() == null || navigable.comparator().compare(-10, 10) < 0;
                    map(name + ".subMap(-10, 10)", ascending ? navigable.subMap(-10, true, 10, true) : navigable.subMap(10, true, -10, true), depth + 1);
                } else {
                    map(name + ".subMap(1, 1)", navigable.subMap(1, true, 1, true), depth + 1);
                }
            }
        }
    }

    private void entries(String name, java.util.Set<java.util.Map.Entry<Integer, String>> view) {
        refused(name + ".add", () -> view.add(java.util.Map.entry(1, "a")));
        refused(name + ".remove(absent)", () -> view.remove(java.util.Map.entry(-7, "a")));
        refused(name + ".removeIf(nothing)", () -> view.removeIf(e -> false));
        refused(name + ".retainAll(itself)", () -> view.retainAll(new java.util.ArrayList<>(view)));
        refused(name + ".removeAll(empty)", () -> view.removeAll(java.util.List.of()));
        refused(name + ".addAll(empty)", () -> view.addAll(java.util.List.of()));
        refused(name + ".clear", view::clear);
        iterator(name + ".iterator()", view.iterator());
        for (java.util.Map.Entry<Integer, String> entry : view) {
            refused(name + " entry.setValue", () -> entry.setValue("b"));
        }
        final java.util.Iterator<java.util.Map.Entry<Integer, String>> advanced = view.iterator();
        if (advanced.hasNext()) {
            advanced.next();
            iterator(name + ".iterator() after next()", advanced);
        }
        if (view instanceof SequencedCollection<java.util.Map.Entry<Integer, String>> sequenced) {
            refused(name + ".addFirst", () -> sequenced.addFirst(java.util.Map.entry(1, "a")));
            refused(name + ".addLast", () -> sequenced.addLast(java.util.Map.entry(1, "a")));
            refused(name + ".removeFirst", sequenced::removeFirst);
            refused(name + ".removeLast", sequenced::removeLast);
        }
    }

    // -- the views

    private static java.util.List<Integer> elements(int n) {
        return IntStream.range(0, n).boxed().toList();
    }

    private static java.util.List<com.guizmaii.zazr.Tuple2<Integer, String>> entries(int n) {
        return IntStream.range(0, n).mapToObj(i -> Tuple.of(i, "v" + i)).toList();
    }

    private java.util.stream.Stream<DynamicTest> each(String kind, java.util.function.IntConsumer check) {
        return IntStream.of(SIZES).mapToObj(n -> DynamicTest.dynamicTest(kind + " of " + n, () -> {
            failures.clear();
            check.accept(n);
            assertAllRefused();
        }));
    }

    @TestFactory
    java.util.stream.Stream<DynamicTest> shouldRefuseEveryMutatorOfTheListViews() {
        final java.util.Map<String, Function<java.util.List<Integer>, java.util.List<Integer>>> views = new java.util.LinkedHashMap<>();
        views.put("Vector", elements -> Vector.ofAll(elements).asJava());
        views.put("List", elements -> List.ofAll(elements).asJava());
        views.put("Queue", elements -> Queue.ofAll(elements).asJava());
        views.put("Stream", elements -> Stream.ofAll(elements).asJava());
        return views.entrySet().stream().flatMap(view -> each(view.getKey() + ".asJava()", n -> list(view.getKey(), view.getValue().apply(elements(n)), 0)));
    }

    @TestFactory
    java.util.stream.Stream<DynamicTest> shouldRefuseEveryMutatorOfTheCollectionViews() {
        return java.util.stream.Stream.concat(
                each("HashMap.asJava()", n -> collection("HashMap.asJava()", HashMap.ofEntries(entries(n)).asJava())),
                each("TreeMap.asJava()", n -> collection("TreeMap.asJava()", TreeMap.ofEntries(entries(n)).asJava())));
    }

    @TestFactory
    java.util.stream.Stream<DynamicTest> shouldRefuseEveryMutatorOfTheSetViews() {
        return java.util.stream.Stream.of(
                each("HashSet.asJava()", n -> set("HashSet", HashSet.ofAll(elements(n)).asJava(), 0)),
                each("LinkedHashSet.asJava()", n -> set("LinkedHashSet", LinkedHashSet.ofAll(elements(n)).asJava(), 0)),
                each("TreeSet.asJava()", n -> set("TreeSet", TreeSet.ofAll(elements(n)).asJava(), 0)),
                each("TreeSet(reversed).asJava()", n -> set("TreeSet(reversed)", TreeSet.ofAll(Comparator.<Integer> reverseOrder(), elements(n)).asJava(), 0))
        ).flatMap(Function.identity());
    }

    @TestFactory
    java.util.stream.Stream<DynamicTest> shouldRefuseEveryMutatorOfTheMapViews() {
        return java.util.stream.Stream.of(
                each("HashMap.asJavaMap()", n -> map("HashMap", HashMap.ofEntries(entries(n)).asJavaMap(), 0)),
                each("LinkedHashMap.asJavaMap()", n -> map("LinkedHashMap", LinkedHashMap.ofEntries(entries(n)).asJavaMap(), 0)),
                each("TreeMap.asJavaMap()", n -> map("TreeMap", TreeMap.ofEntries(entries(n)).asJavaMap(), 0)),
                each("TreeMap(reversed).asJavaMap()", n -> map("TreeMap(reversed)", TreeMap.ofEntries(Comparator.<Integer> reverseOrder(), entries(n)).asJavaMap(), 0))
        ).flatMap(Function.identity());
    }
}
