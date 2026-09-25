package com.guizmaii.zazr.test.laws;

import com.guizmaii.zazr.collection.Vector;
import com.guizmaii.zazr.test.Arbitrary;
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
     * @param <T>       the element type
     * @param <F>       the collection type
     */
    public record CollectorSubject<T, F>(Arbitrary<? extends Iterable<T>> elements, Collector<T, ?, F> collector,
                                         Function<Iterable<T>, F> ofAll) {

        /**
         * Creates a subject.
         *
         * @param elements  arbitrary lists of elements
         * @param collector the collector under test
         * @param ofAll     the collection type's {@code ofAll}
         * @throws NullPointerException if an argument is null
         */
        public CollectorSubject {
            Objects.requireNonNull(elements, "elements is null");
            Objects.requireNonNull(collector, "collector is null");
            Objects.requireNonNull(ofAll, "ofAll is null");
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
     * collection equal to {@code ofAll(elements)}.
     *
     * @param <T> the element type
     * @param <F> the collection type
     * @return the law
     */
    public static <T, F> Law<CollectorSubject<T, F>> collectorResultEqualsOfAll() {
        return Law.of("collectorResultEqualsOfAll", subject -> Property.named("collectorResultEqualsOfAll")
                .forAll(subject.elements())
                .suchThatResult(xs -> {
                    final ArrayList<T> list = CollectionLaws.elements(xs);
                    final F expected = subject.ofAll().apply(list);
                    return Results.both(Results.equal(list.stream().collect(subject.collector()), expected),
                            Results.equal(list.parallelStream().collect(subject.collector()), expected));
                }));
    }

    private static ArrayList<Object> elements(Iterable<?> xs) {
        final ArrayList<Object> elements = new ArrayList<>();
        xs.forEach(elements::add);
        return elements;
    }
}
