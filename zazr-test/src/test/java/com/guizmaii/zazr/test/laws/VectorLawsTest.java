package com.guizmaii.zazr.test.laws;

import com.guizmaii.zazr.collection.Vector;
import com.guizmaii.zazr.control.Option;
import com.guizmaii.zazr.test.Gen;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

/**
 * The laws of {@code Vector}.
 */
class VectorLawsTest extends SequenceLawsSuite<Vector<?>, Vector<Integer>, VectorLawsTest.Subject> {

    static final class Subject implements FlatMapSubject<Vector<?>>, ZipSubject<Vector<?>> {

        @Override
        public Gen<Vector<?>> values() {
            return Gen.vector(Values.integers()).map(value -> value);
        }

        @Override
        public Vector<?> map(Vector<?> fa, Function<Object, Object> f) {
            return fa.map(f);
        }

        @Override
        public Vector<?> succeed(Object a) {
            return Vector.of(a);
        }

        @Override
        public Vector<?> flatMap(Vector<?> fa, Function<Object, Vector<?>> f) {
            return fa.flatMap(f);
        }

        @Override
        public Vector<?> zip(Vector<?> fa, Vector<?> fb) {
            return fa.zip(fb);
        }
    }

    @Override
    Subject subject() {
        return new Subject();
    }

    @Override
    CollectionSubject<Integer, Vector<Integer>> collection() {
        return new CollectionSubject<>(Gen.vector(Values.integers()), Vector::ofAll, Vector::size, Vector::toList, true, Option.some(IterationOrder.input()));
    }

    @Override
    BuilderLaws.CollectorSubject<Integer, Vector<Integer>> collector() {
        return new BuilderLaws.CollectorSubject<>(Gen.list(Values.integers()), Vector.collector(), Vector::ofAll, Option.some(IterationOrder.input()));
    }

    @Test
    void builderResultEqualsOfAll() {
        LawChecks.check(BuilderLaws.builderResultEqualsOfAll(), Gen.list(Values.integers()));
    }

    /// Sizes at the boundaries of a 32-wide trie.
    private static final int[] TRIE_BOUNDARIES = { 32, 33, 1024, 1025, 32_769 };

    /// The samples of each check at a trie boundary.
    private static final int BOUNDARY_SAMPLES = 40;

    /// Collections at a fixed size: half of exactly `size` elements, half as `Gen.vector`, which favours the size and
    /// the size minus one.
    private static <T> Gen<T> atBoundary(int size, Gen<? extends T> exactly, Gen<? extends T> upTo) {
        return Gen.<T>oneOf(exactly, upTo).withSize(size);
    }

    @Test
    void builderResultEqualsOfAllAtTrieBoundaries() {
        for (int size : TRIE_BOUNDARIES) {
            final Gen<Iterable<Integer>> elements = atBoundary(size, Gen.vectorN(size, Values.integers()), Gen.list(Values.integers()));
            Laws.of(BuilderLaws.builderResultEqualsOfAll())
                    .assertSatisfied(elements, LawChecks.config("builderResultEqualsOfAll" + size).withSamples(BOUNDARY_SAMPLES));
        }
    }

    @Test
    void sequenceLawsAtTrieBoundaries() {
        for (int size : TRIE_BOUNDARIES) {
            final CollectionSubject<Integer, Vector<Integer>> collection = collection();
            final Gen<Vector<Integer>> vectors = atBoundary(size, Gen.vectorN(size, Values.integers()), collection.values());
            CollectionLaws.<Integer, Vector<Integer>>sequence().assertSatisfied(
                    new CollectionSubject<>(vectors, collection.ofAll(), collection.size(), collection.toList(), collection.ordered(), collection.order()),
                    LawChecks.config("sequenceLaws" + size).withSamples(BOUNDARY_SAMPLES));
        }
    }
}
