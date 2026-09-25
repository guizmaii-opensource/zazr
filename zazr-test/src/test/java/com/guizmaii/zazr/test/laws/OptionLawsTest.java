package com.guizmaii.zazr.test.laws;

import com.guizmaii.zazr.control.Option;
import com.guizmaii.zazr.test.Gen;

import java.util.function.Function;

/**
 * The laws of {@code Option}.
 */
class OptionLawsTest extends ControlLawsSuite<Option<?>, OptionLawsTest.Subject> {

    static final class Subject implements FlatMapSubject<Option<?>>, ZipSidesSubject<Option<?>> {

        @Override
        public Gen<Option<?>> values() {
            return Gen.option(Values.integers()).map(value -> value);
        }

        @Override
        public Option<?> map(Option<?> fa, Function<Object, Object> f) {
            return fa.map(f);
        }

        @Override
        public Option<?> succeed(Object a) {
            return Option.some(a);
        }

        @Override
        public Option<?> flatMap(Option<?> fa, Function<Object, Option<?>> f) {
            return fa.flatMap(f);
        }

        @Override
        public Option<?> zip(Option<?> fa, Option<?> fb) {
            return fa.zip(fb);
        }

        @Override
        public Option<?> zipLeft(Option<?> fa, Option<?> fb) {
            return fa.zipLeft(fb);
        }

        @Override
        public Option<?> zipRight(Option<?> fa, Option<?> fb) {
            return fa.zipRight(fb);
        }
    }

    @Override
    Subject subject() {
        return new Subject();
    }

    @Override
    EqualitySubject<Option<?>> equality() {
        return new EqualitySubject<>(subject().values(), o -> o.isEmpty() ? Option.none() : Option.some(o.get()), o -> o.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(o.get()));
    }
}
