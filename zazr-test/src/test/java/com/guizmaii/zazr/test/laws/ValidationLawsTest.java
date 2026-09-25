package com.guizmaii.zazr.test.laws;

import com.guizmaii.zazr.control.Validation;
import com.guizmaii.zazr.test.legacy.Arbitrary;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

/**
 * The laws of {@code Validation}.
 */
class ValidationLawsTest extends ControlLawsSuite<Validation<?, ?>, ValidationLawsTest.Subject> {

    static final class Subject implements FlatMapSubject<Validation<?, ?>>, ZipSidesSubject<Validation<?, ?>> {

        @Override
        public Arbitrary<Validation<?, ?>> values() {
            return Arbitrary.validation(Arbitrary.integer(), Arbitrary.integer()).map(value -> value);
        }

        @Override
        public Validation<?, ?> map(Validation<?, ?> fa, Function<Object, Object> f) {
            return fa.map(f);
        }

        @Override
        public Validation<?, ?> succeed(Object a) {
            return Validation.valid(a);
        }

        @Override
        public Validation<?, ?> flatMap(Validation<?, ?> fa, Function<Object, Validation<?, ?>> f) {
            return Casts.<Validation<Object, Object>>cast(fa).flatMap(x -> Casts.<Validation<Object, Object>>cast(f.apply(x)));
        }

        @Override
        public Validation<?, ?> zip(Validation<?, ?> fa, Validation<?, ?> fb) {
            return Casts.<Validation<Object, Object>>cast(fa).zip(Casts.<Validation<Object, Object>>cast(fb));
        }

        @Override
        public Validation<?, ?> zipLeft(Validation<?, ?> fa, Validation<?, ?> fb) {
            return Casts.<Validation<Object, Object>>cast(fa).zipLeft(Casts.<Validation<Object, Object>>cast(fb));
        }

        @Override
        public Validation<?, ?> zipRight(Validation<?, ?> fa, Validation<?, ?> fb) {
            return Casts.<Validation<Object, Object>>cast(fa).zipRight(Casts.<Validation<Object, Object>>cast(fb));
        }
    }

    @Override
    Subject subject() {
        return new Subject();
    }

    @Override
    EqualitySubject<Validation<?, ?>> equality() {
        return new EqualitySubject<>(subject().values(), v -> switch (v) {
            case Validation.Valid<?, ?>(var a) -> Validation.valid(a);
            case Validation.Invalid<?, ?>(var errors) -> Validation.invalidAll(errors);
        }, v -> switch (v) {
            case Validation.Valid<?, ?>(var a) -> java.util.List.of("valid", a);
            case Validation.Invalid<?, ?>(var errors) -> java.util.List.of("invalid", CollectionLaws.elements(errors));
        });
    }

    @Test
    void validationZipAccumulatesBothSides() {
        LawChecks.check(ValidationLaws.validationZipAccumulatesBothSides(), subject().values());
    }
}
