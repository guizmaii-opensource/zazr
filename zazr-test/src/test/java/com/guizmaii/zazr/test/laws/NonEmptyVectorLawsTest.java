package com.guizmaii.zazr.test.laws;

import com.guizmaii.zazr.collection.NonEmptyVector;
import com.guizmaii.zazr.collection.Vector;
import com.guizmaii.zazr.test.Arbitrary;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

/**
 * The laws of {@code NonEmptyVector}.
 */
class NonEmptyVectorLawsTest {

    static final class Subject implements FlatMapSubject<NonEmptyVector<?>>, ZipSubject<NonEmptyVector<?>> {

        @Override
        public Arbitrary<NonEmptyVector<?>> values() {
            return Arbitrary.nonEmptyVector(Arbitrary.integer()).map(value -> value);
        }

        @Override
        public NonEmptyVector<?> map(NonEmptyVector<?> fa, Function<Object, Object> f) {
            return fa.map(f);
        }

        @Override
        public NonEmptyVector<?> succeed(Object a) {
            return NonEmptyVector.single(a);
        }

        @Override
        public NonEmptyVector<?> flatMap(NonEmptyVector<?> fa, Function<Object, NonEmptyVector<?>> f) {
            return fa.flatMap(f);
        }

        @Override
        public NonEmptyVector<?> zip(NonEmptyVector<?> fa, NonEmptyVector<?> fb) {
            return fa.zip(fb);
        }
    }

    private static final Subject SUBJECT = new Subject();

    @Test
    void mapIdentity() {
        LawChecks.check(MapLaws.<NonEmptyVector<?>>mapIdentity(), SUBJECT);
    }

    @Test
    void mapComposition() {
        LawChecks.check(MapLaws.<NonEmptyVector<?>>mapComposition(), SUBJECT);
    }

    @Test
    void flatMapAssociativity() {
        LawChecks.check(FlatMapLaws.<NonEmptyVector<?>>flatMapAssociativity(), SUBJECT);
    }

    @Test
    void flatMapLeftIdentity() {
        LawChecks.check(FlatMapLaws.<NonEmptyVector<?>>flatMapLeftIdentity(), SUBJECT);
    }

    @Test
    void flatMapRightIdentity() {
        LawChecks.check(FlatMapLaws.<NonEmptyVector<?>>flatMapRightIdentity(), SUBJECT);
    }

    @Test
    void mapIsFlatMapSucceed() {
        LawChecks.check(FlatMapLaws.<NonEmptyVector<?>>mapIsFlatMapSucceed(), SUBJECT);
    }

    @Test
    void zipAssociativity() {
        LawChecks.check(ZipLaws.<NonEmptyVector<?>>zipAssociativity(), SUBJECT);
    }

    @Test
    void nonEmptyVectorHeadIsTotal() {
        LawChecks.check(NonEmptyVectorLaws.nonEmptyVectorHeadIsTotal(), SUBJECT.values());
    }

    @Test
    void nonEmptyVectorEqualsVectorSymmetry() {
        LawChecks.check(NonEmptyVectorLaws.nonEmptyVectorEqualsVectorSymmetry(), SUBJECT.values());
    }

    @Test
    void equalsHashCodeConsistency() {
        LawChecks.check(EqualityLaws.<NonEmptyVector<?>>equalsHashCodeConsistency(), new EqualitySubject<>(SUBJECT.values(),
                nev -> NonEmptyVector.fromVector(Vector.ofAll(nev.toList())).get()));
    }
}
