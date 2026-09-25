package com.guizmaii.zazr.test.laws;

import com.guizmaii.zazr.Lazy;
import com.guizmaii.zazr.test.Arbitrary;

import java.util.function.Function;

/**
 * The laws of {@code Lazy}.
 */
class LazyLawsTest extends ControlLawsSuite<Lazy<?>, LazyLawsTest.Subject> {

    static final class Subject implements FlatMapSubject<Lazy<?>>, ZipSidesSubject<Lazy<?>> {

        @Override
        public Arbitrary<Lazy<?>> values() {
            return Arbitrary.lazy(Arbitrary.integer()).map(value -> value);
        }

        @Override
        public Lazy<?> map(Lazy<?> fa, Function<Object, Object> f) {
            return fa.map(f);
        }

        @Override
        public Lazy<?> succeed(Object a) {
            return Lazy.of(() -> a);
        }

        @Override
        public Lazy<?> flatMap(Lazy<?> fa, Function<Object, Lazy<?>> f) {
            return fa.flatMap(f);
        }

        @Override
        public Lazy<?> zip(Lazy<?> fa, Lazy<?> fb) {
            return fa.zip(fb);
        }

        @Override
        public Lazy<?> zipLeft(Lazy<?> fa, Lazy<?> fb) {
            return fa.zipLeft(fb);
        }

        @Override
        public Lazy<?> zipRight(Lazy<?> fa, Lazy<?> fb) {
            return fa.zipRight(fb);
        }
    }

    @Override
    Subject subject() {
        return new Subject();
    }

    @Override
    EqualitySubject<Lazy<?>> equality() {
        return new EqualitySubject<>(subject().values(), l -> Lazy.of(l::get), l -> java.util.List.of(l.get()));
    }
}
