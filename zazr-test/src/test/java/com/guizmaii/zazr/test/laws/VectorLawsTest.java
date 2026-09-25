package com.guizmaii.zazr.test.laws;

import com.guizmaii.zazr.collection.Vector;
import com.guizmaii.zazr.control.Option;
import com.guizmaii.zazr.test.Arbitrary;
import java.util.Random;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

/**
 * The laws of {@code Vector}.
 */
class VectorLawsTest extends SequenceLawsSuite<Vector<?>, Vector<Integer>, VectorLawsTest.Subject> {

    static final class Subject implements FlatMapSubject<Vector<?>>, ZipSubject<Vector<?>> {

        @Override
        public Arbitrary<Vector<?>> values() {
            return Arbitrary.vector(Arbitrary.integer()).map(value -> value);
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
        return new CollectionSubject<>(Arbitrary.vector(Arbitrary.integer()), Vector::ofAll, Vector::size, Vector::toList, true, Option.some(IterationOrder.input()));
    }

    @Override
    BuilderLaws.CollectorSubject<Integer, Vector<Integer>> collector() {
        return new BuilderLaws.CollectorSubject<>(Arbitrary.list(Arbitrary.integer()), Vector.collector(), Vector::ofAll, Option.some(IterationOrder.input()));
    }

    @Test
    void builderResultEqualsOfAll() {
        LawChecks.check(BuilderLaws.builderResultEqualsOfAll(), Arbitrary.list(Arbitrary.integer()));
    }

    /// Sizes at the boundaries of a 32-wide trie: the arbitraries favour the size and the size minus one.
    private static final int[] TRIE_BOUNDARIES = { 32, 33, 1024, 1025, 32_769 };

    @Test
    void builderResultEqualsOfAllAtTrieBoundaries() {
        for (int size : TRIE_BOUNDARIES) {
            Laws.of(BuilderLaws.builderResultEqualsOfAll())
                    .assertSatisfied(Arbitrary.list(Arbitrary.integer()), new Random(LawChecks.SEED + size), size, 40);
        }
    }

    @Test
    void sequenceLawsAtTrieBoundaries() {
        for (int size : TRIE_BOUNDARIES) {
            CollectionLaws.<Integer, Vector<Integer>>sequence().assertSatisfied(collection(), new Random(LawChecks.SEED + size), size, 40);
        }
    }
}
