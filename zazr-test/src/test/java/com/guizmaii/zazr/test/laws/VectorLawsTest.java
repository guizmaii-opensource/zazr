package com.guizmaii.zazr.test.laws;

import com.guizmaii.zazr.collection.Vector;
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
        return new CollectionSubject<>(Arbitrary.vector(Arbitrary.integer()), Vector::ofAll, Vector::size, Vector::toList, true);
    }

    @Override
    BuilderLaws.CollectorSubject<Integer, Vector<Integer>> collector() {
        return new BuilderLaws.CollectorSubject<>(Arbitrary.list(Arbitrary.integer()), Vector.collector(), Vector::ofAll);
    }

    @Test
    void builderResultEqualsOfAll() {
        LawChecks.check(BuilderLaws.builderResultEqualsOfAll(), Arbitrary.list(Arbitrary.integer()));
    }

    @Test
    void builderResultEqualsOfAllAcrossTrieLevels() {
        Laws.of(BuilderLaws.builderResultEqualsOfAll())
                .assertSatisfied(Arbitrary.list(Arbitrary.integer()), new Random(LawChecks.SEED), 2_000, 100);
    }

    @Test
    void sequenceLawsAcrossTrieLevels() {
        CollectionLaws.<Integer, Vector<Integer>>sequence().assertSatisfied(collection(), new Random(LawChecks.SEED), 2_000, 100);
    }
}
