package com.guizmaii.zazr.test.laws;

import com.guizmaii.zazr.collection.Vector;
import com.guizmaii.zazr.control.Option;
import com.guizmaii.zazr.test.Arbitrary;
import com.guizmaii.zazr.test.PredicateResult;
import com.guizmaii.zazr.test.Property;

import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collector;

/**
 * The laws of the builders and collectors: building from elements gives the collection {@code ofAll} gives.
 */
public final class BuilderLaws {

    private BuilderLaws() {
    }

    /**
     * A collector and the {@code ofAll} it must agree with.
     *
     * @param elements  arbitrary lists of elements
     * @param collector the collector under test
     * @param ofAll     the collection type's {@code ofAll}
     * @param order     the order a collected collection must iterate in, or none for a type whose order is
     *                  unspecified
     * @param <T>       the element type
     * @param <F>       the collection type
     */
    public record CollectorSubject<T, F extends Iterable<T>>(Arbitrary<? extends Iterable<T>> elements,
                                                             Collector<T, ?, F> collector, Function<Iterable<T>, F> ofAll,
                                                             Option<IterationOrder<T>> order) {

        /**
         * Creates a subject.
         *
         * @param elements  arbitrary lists of elements
         * @param collector the collector under test
         * @param ofAll     the collection type's {@code ofAll}
         * @param order     the order a collected collection must iterate in, or none
         * @throws NullPointerException if an argument is null
         */
        public CollectorSubject {
            Objects.requireNonNull(elements, "elements is null");
            Objects.requireNonNull(collector, "collector is null");
            Objects.requireNonNull(ofAll, "ofAll is null");
            Objects.requireNonNull(order, "order is null");
        }

        /**
         * Creates a subject whose iteration order is unspecified.
         *
         * @param elements  arbitrary lists of elements
         * @param collector the collector under test
         * @param ofAll     the collection type's {@code ofAll}
         * @throws NullPointerException if an argument is null
         */
        public CollectorSubject(Arbitrary<? extends Iterable<T>> elements, Collector<T, ?, F> collector,
                                Function<Iterable<T>, F> ofAll) {
            this(elements, collector, ofAll, Option.none());
        }
    }

    /**
     * {@code Vector.newBuilder()} fed the elements one at a time, or all at once with {@code addAll}, or with any
     * size hint, returns a vector equal to {@code Vector.ofAll(elements)}.
     *
     * @return the law, checked against arbitrary lists of elements
     */
    public static Law<Arbitrary<? extends Iterable<?>>> builderResultEqualsOfAll() {
        return Law.of("builderResultEqualsOfAll", elements -> Property.named("builderResultEqualsOfAll")
                .forAll(elements, Arbitrary.integer())
                .suchThatResult((xs, hint) -> {
                    final Vector<Object> expected = Vector.ofAll(xs);
                    final Vector.Builder<Object> oneByOne = Vector.newBuilder();
                    xs.forEach(oneByOne::add);
                    final Vector.Builder<Object> hinted = Vector.newBuilder(Math.abs(hint));
                    xs.forEach(hinted::add);
                    final Vector<Object> all = Vector.newBuilder().addAll(xs).result();
                    return Results.both(Results.equal(oneByOne.result(), expected),
                            Results.both(Results.equal(hinted.result(), expected),
                                    Results.both(Results.equal(all, expected),
                                            Results.equal(CollectionLaws.elements(expected), elements(xs)))));
                }));
    }

    /**
     * Collecting the elements with the collection type's collector, sequentially or in parallel, gives a
     * collection equal to {@code ofAll(elements)}, iterating in the subject's order when it has one.
     *
     * @param <T> the element type
     * @param <F> the collection type
     * @return the law
     */
    public static <T, F extends Iterable<T>> Law<CollectorSubject<T, F>> collectorResultEqualsOfAll() {
        return Law.of("collectorResultEqualsOfAll", subject -> Property.named("collectorResultEqualsOfAll")
                .forAll(subject.elements())
                .suchThatResult(xs -> {
                    final ArrayList<T> list = CollectionLaws.elements(xs);
                    final F expected = subject.ofAll().apply(list);
                    final F sequential = list.stream().collect(subject.collector());
                    final F parallel = list.parallelStream().collect(subject.collector());
                    return Results.both(Results.both(Results.equal(sequential, expected), Results.equal(parallel, expected)),
                            subject.order().map(order -> Results.both(
                                    Results.equal(CollectionLaws.elements(sequential), order.of(list)),
                                    Results.equal(CollectionLaws.elements(parallel), order.of(list))))
                                    .getOrElse(PredicateResult.success()));
                }));
    }

    private static ArrayList<Object> elements(Iterable<?> xs) {
        final ArrayList<Object> elements = new ArrayList<>();
        xs.forEach(elements::add);
        return elements;
    }
}
