package com.guizmaii.zazr.test.laws;

import com.guizmaii.zazr.control.Try;
import com.guizmaii.zazr.test.Gen;

import java.util.function.Function;

/**
 * The laws of {@code Try}.
 */
class TryLawsTest extends ControlLawsSuite<Try<?>, TryLawsTest.Subject> {

    static final class Subject implements FlatMapSubject<Try<?>>, ZipSidesSubject<Try<?>> {

        @Override
        public Gen<Try<?>> values() {
            return Gen.tryOf(Values.integers()).map(value -> value);
        }

        @Override
        public Try<?> map(Try<?> fa, Function<Object, Object> f) {
            return fa.map(f);
        }

        @Override
        public Try<?> succeed(Object a) {
            return Try.success(a);
        }

        @Override
        public Try<?> flatMap(Try<?> fa, Function<Object, Try<?>> f) {
            return fa.flatMap(f);
        }

        @Override
        public Try<?> zip(Try<?> fa, Try<?> fb) {
            return fa.zip(fb);
        }

        @Override
        public Try<?> zipLeft(Try<?> fa, Try<?> fb) {
            return fa.zipLeft(fb);
        }

        @Override
        public Try<?> zipRight(Try<?> fa, Try<?> fb) {
            return fa.zipRight(fb);
        }
    }

    @Override
    Subject subject() {
        return new Subject();
    }

    @Override
    EqualitySubject<Try<?>> equality() {
        return new EqualitySubject<>(subject().values(), t -> switch (t) {
            case Try.Success<?>(var v) -> Try.success(v);
            case Try.Failure<?>(var e) -> Try.failure(e);
        }, t -> switch (t) {
            case Try.Success<?>(var v) -> java.util.List.of("success", v);
            case Try.Failure<?>(var e) -> java.util.List.of("failure", new Identity(e));
        });
    }

    /// A failure's cause compared by reference, as `Failure` compares it.
    private record Identity(Object value) {

        @Override
        public boolean equals(Object o) {
            return o instanceof Identity that && that.value == value;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(value);
        }
    }
}
