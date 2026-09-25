package com.guizmaii.zazr.test.laws;

import com.guizmaii.zazr.control.Either;
import com.guizmaii.zazr.test.Arbitrary;

import java.util.function.Function;

/**
 * The laws of {@code Either}.
 */
class EitherLawsTest extends ControlLawsSuite<Either<?, ?>, EitherLawsTest.Subject> {

    static final class Subject implements FlatMapSubject<Either<?, ?>>, ZipSidesSubject<Either<?, ?>> {

        @Override
        public Arbitrary<Either<?, ?>> values() {
            return Arbitrary.either(Arbitrary.integer(), Arbitrary.integer()).map(value -> value);
        }

        @Override
        public Either<?, ?> map(Either<?, ?> fa, Function<Object, Object> f) {
            return fa.map(f);
        }

        @Override
        public Either<?, ?> succeed(Object a) {
            return Either.right(a);
        }

        @Override
        public Either<?, ?> flatMap(Either<?, ?> fa, Function<Object, Either<?, ?>> f) {
            return Casts.<Either<Object, Object>>cast(fa).flatMap(x -> Casts.<Either<Object, Object>>cast(f.apply(x)));
        }

        @Override
        public Either<?, ?> zip(Either<?, ?> fa, Either<?, ?> fb) {
            return Casts.<Either<Object, Object>>cast(fa).zip(Casts.<Either<Object, Object>>cast(fb));
        }

        @Override
        public Either<?, ?> zipLeft(Either<?, ?> fa, Either<?, ?> fb) {
            return Casts.<Either<Object, Object>>cast(fa).zipLeft(Casts.<Either<Object, Object>>cast(fb));
        }

        @Override
        public Either<?, ?> zipRight(Either<?, ?> fa, Either<?, ?> fb) {
            return Casts.<Either<Object, Object>>cast(fa).zipRight(Casts.<Either<Object, Object>>cast(fb));
        }
    }

    @Override
    Subject subject() {
        return new Subject();
    }

    @Override
    EqualitySubject<Either<?, ?>> equality() {
        return new EqualitySubject<>(subject().values(), e -> switch (e) {
            case Either.Left<?, ?>(var l) -> Either.left(l);
            case Either.Right<?, ?>(var r) -> Either.right(r);
        });
    }
}
