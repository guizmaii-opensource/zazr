package com.guizmaii.zazr.test.laws;

import com.guizmaii.zazr.Tuple2;
import com.guizmaii.zazr.collection.HashMap;
import com.guizmaii.zazr.collection.HashSet;
import com.guizmaii.zazr.collection.LinkedHashMap;
import com.guizmaii.zazr.collection.LinkedHashSet;
import com.guizmaii.zazr.collection.List;
import com.guizmaii.zazr.collection.Queue;
import com.guizmaii.zazr.collection.Stream;
import com.guizmaii.zazr.collection.TreeMap;
import com.guizmaii.zazr.collection.TreeSet;
import com.guizmaii.zazr.collection.Vector;
import com.guizmaii.zazr.test.legacy.PredicateResult;
import com.guizmaii.zazr.test.legacy.Property;

import java.util.ArrayList;
import java.util.Collections;

/**
 * The contracts every collection keeps: {@code size}, {@code toList}, the iteration order, and an {@code equals}
 * decided by the elements alone (in order for the sequences, as a set for the sets and maps), across the types of one family.
 */
public final class CollectionLaws {

    private CollectionLaws() {
    }

    /**
     * {@code size()} equals the number of elements the iterator returns.
     *
     * @param <T> the element type
     * @param <F> the collection type
     * @return the law
     */
    public static <T, F extends Iterable<T>> Law<CollectionSubject<T, F>> sizeEqualsIterationCount() {
        return Law.of("sizeEqualsIterationCount", subject -> Property.named("sizeEqualsIterationCount")
                .forAll(subject.values())
                .suchThatResult(fa -> Results.equal(subject.size().applyAsInt(fa), elements(fa).size())));
    }

    /**
     * {@code toList()} holds the elements in iteration order, and {@code ofAll(fa.toList())} equals {@code fa}.
     *
     * @param <T> the element type
     * @param <F> the collection type
     * @return the law
     */
    public static <T, F extends Iterable<T>> Law<CollectionSubject<T, F>> toListRoundTrip() {
        return Law.of("toListRoundTrip", subject -> Property.named("toListRoundTrip")
                .forAll(subject.values())
                .suchThatResult(fa -> {
                    final List<T> list = subject.toList().apply(fa);
                    return Results.both(Results.equal(elements(list), elements(fa)),
                            Results.equal(subject.ofAll().apply(list), fa));
                }));
    }

    /**
     * For collections {@code a} and {@code b}: {@code a.equals(b)} holds exactly when both have the same elements
     * (in the same order for a sequence), and equal collections have equal hash codes. Besides an independent
     * {@code b}, it compares {@code a} with its elements reversed and with its first element dropped.
     *
     * @param <T> the element type
     * @param <F> the collection type
     * @return the law
     */
    public static <T, F extends Iterable<T>> Law<CollectionSubject<T, F>> equalsAgreesWithElements() {
        return Law.of("equalsAgreesWithElements", subject -> Property.named("equalsAgreesWithElements")
                .forAll(subject.values(), subject.values())
                .suchThatResult((a, b) -> {
                    final ArrayList<T> reversed = elements(a);
                    Collections.reverse(reversed);
                    final ArrayList<T> dropped = elements(a);
                    if (!dropped.isEmpty()) {
                        dropped.removeFirst();
                    }
                    return Results.both(agrees(subject, a, b), Results.both(agrees(subject, a, subject.ofAll().apply(reversed)),
                            agrees(subject, a, subject.ofAll().apply(dropped))));
                }));
    }

    /**
     * A collection built by {@code ofAll} iterates in the subject's {@link IterationOrder}: input order for a
     * sequence, first occurrence for an insertion-ordered set, sorted for a sorted set, and so on. The input is the
     * elements of one collection followed by those of another, reversed, so that it repeats elements. A subject
     * without an order satisfies the law.
     *
     * @param <T> the element type
     * @param <F> the collection type
     * @return the law
     */
    public static <T, F extends Iterable<T>> Law<CollectionSubject<T, F>> iterationOrder() {
        return Law.of("iterationOrder", subject -> Property.named("iterationOrder")
                .forAll(subject.values(), subject.values())
                .suchThatResult((a, b) -> {
                    final ArrayList<T> input = elements(a);
                    final ArrayList<T> second = elements(b);
                    Collections.reverse(second);
                    input.addAll(second);
                    return subject.order()
                            .map(order -> Results.equal(elements(subject.ofAll().apply(input)), order.of(input)))
                            .getOrElse(PredicateResult.success());
                }));
    }

    /**
     * A sequence equals a {@code Vector}, a {@code List}, a {@code Queue} and a {@code Stream} of the same elements
     * in the same order, both ways, with the same hash code, and never equals a set.
     *
     * @param <T> the element type
     * @param <F> the collection type
     * @return the law
     */
    public static <T, F extends Iterable<T>> Law<CollectionSubject<T, F>> sequenceEqualsAcrossTypes() {
        return Law.of("sequenceEqualsAcrossTypes", subject -> Property.named("sequenceEqualsAcrossTypes")
                .forAll(subject.values())
                .suchThatResult(fa -> {
                    final ArrayList<T> xs = elements(fa);
                    return Results.both(
                            allEqual(fa, Vector.ofAll(xs), List.ofAll(xs), Queue.ofAll(xs), Stream.ofAll(xs)),
                            noneEqual(fa, HashSet.ofAll(xs), LinkedHashSet.ofAll(xs)));
                }));
    }

    /**
     * A set equals a {@code HashSet}, a {@code LinkedHashSet} and (for comparable elements) a {@code TreeSet} of the
     * same elements, both ways, with the same hash code, and never equals a sequence.
     *
     * @param <T> the element type
     * @param <F> the collection type
     * @return the law
     */
    public static <T, F extends Iterable<T>> Law<CollectionSubject<T, F>> setEqualsAcrossTypes() {
        return Law.of("setEqualsAcrossTypes", subject -> Property.named("setEqualsAcrossTypes")
                .forAll(subject.values())
                .suchThatResult(fa -> {
                    final ArrayList<T> xs = elements(fa);
                    final PredicateResult hashed = allEqual(fa, HashSet.ofAll(xs), LinkedHashSet.ofAll(xs));
                    final PredicateResult sorted = comparable(xs) ? allEqual(fa, treeSet(xs)) : PredicateResult.success();
                    return Results.both(hashed, Results.both(sorted, noneEqual(fa, Vector.ofAll(xs), List.ofAll(xs))));
                }));
    }

    /**
     * A map equals a {@code HashMap}, a {@code LinkedHashMap} and (for comparable keys) a {@code TreeMap} of the same
     * entries, both ways, with the same hash code, and never equals the sequence of its entries.
     *
     * @param <T> the entry type
     * @param <F> the map type
     * @return the law
     */
    public static <T extends Tuple2<?, ?>, F extends Iterable<T>> Law<CollectionSubject<T, F>> mapEqualsAcrossTypes() {
        return Law.of("mapEqualsAcrossTypes", subject -> Property.named("mapEqualsAcrossTypes")
                .forAll(subject.values())
                .suchThatResult(fa -> {
                    final ArrayList<T> entries = elements(fa);
                    final PredicateResult hashed = allEqual(fa, HashMap.ofEntries(entries), LinkedHashMap.ofEntries(entries));
                    final PredicateResult sorted = comparable(entries.stream().map(Tuple2::_1).toList())
                            ? allEqual(fa, treeMap(entries))
                            : PredicateResult.success();
                    return Results.both(hashed, Results.both(sorted, noneEqual(fa, Vector.ofAll(entries))));
                }));
    }

    /**
     * {@link #sizeEqualsIterationCount()}, {@link #toListRoundTrip()}, {@link #equalsAgreesWithElements()} and
     * {@link #iterationOrder()}.
     *
     * @param <T> the element type
     * @param <F> the collection type
     * @return the law set
     */
    public static <T, F extends Iterable<T>> Laws<CollectionSubject<T, F>> all() {
        return Laws.of(sizeEqualsIterationCount(), toListRoundTrip(), equalsAgreesWithElements(), iterationOrder());
    }

    /**
     * {@link #all()} and {@link #sequenceEqualsAcrossTypes()}.
     *
     * @param <T> the element type
     * @param <F> the collection type
     * @return the law set
     */
    public static <T, F extends Iterable<T>> Laws<CollectionSubject<T, F>> sequence() {
        return CollectionLaws.<T, F>all().and(sequenceEqualsAcrossTypes());
    }

    /**
     * {@link #all()} and {@link #setEqualsAcrossTypes()}.
     *
     * @param <T> the element type
     * @param <F> the collection type
     * @return the law set
     */
    public static <T, F extends Iterable<T>> Laws<CollectionSubject<T, F>> set() {
        return CollectionLaws.<T, F>all().and(setEqualsAcrossTypes());
    }

    /**
     * {@link #all()} and {@link #mapEqualsAcrossTypes()}.
     *
     * @param <T> the entry type
     * @param <F> the map type
     * @return the law set
     */
    public static <T extends Tuple2<?, ?>, F extends Iterable<T>> Laws<CollectionSubject<T, F>> map() {
        return CollectionLaws.<T, F>all().and(mapEqualsAcrossTypes());
    }

    // -- helpers

    static <T> ArrayList<T> elements(Iterable<T> iterable) {
        final ArrayList<T> elements = new ArrayList<>();
        iterable.forEach(elements::add);
        return elements;
    }

    private static <T, F extends Iterable<T>> PredicateResult agrees(CollectionSubject<T, F> subject, F a, F b) {
        final boolean expected = subject.ordered()
                ? elements(a).equals(elements(b))
                : new java.util.HashSet<>(elements(a)).equals(new java.util.HashSet<>(elements(b)));
        return Results.both(
                Results.check(a.equals(b) == expected, a + (expected ? " differs from " : " equals ") + b),
                EqualityLaws.consistent(a, b));
    }

    private static PredicateResult allEqual(Object fa, Object... others) {
        PredicateResult result = PredicateResult.success();
        for (Object other : others) {
            result = Results.both(result, Results.both(
                    Results.check(fa.equals(other) && other.equals(fa), fa + " differs from " + other + " of the same elements"),
                    EqualityLaws.consistent(fa, other)));
        }
        return result;
    }

    private static PredicateResult noneEqual(Object fa, Object... others) {
        PredicateResult result = PredicateResult.success();
        for (Object other : others) {
            result = Results.both(result, Results.check(!fa.equals(other) && !other.equals(fa),
                    fa + " equals " + other.getClass().getSimpleName() + " " + other));
        }
        return result;
    }

    private static boolean comparable(java.util.List<?> xs) {
        if (xs.isEmpty()) {
            return true;
        }
        final Class<?> type = xs.getFirst().getClass();
        return Comparable.class.isAssignableFrom(type) && xs.stream().allMatch(x -> x.getClass() == type);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static TreeSet<?> treeSet(ArrayList<?> xs) {
        return TreeSet.ofAll((Iterable) xs);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static TreeMap<?, ?> treeMap(ArrayList<? extends Tuple2<?, ?>> entries) {
        return TreeMap.ofEntries((Iterable) entries);
    }
}
