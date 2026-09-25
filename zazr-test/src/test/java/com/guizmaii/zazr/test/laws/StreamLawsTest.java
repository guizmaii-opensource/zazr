package com.guizmaii.zazr.test.laws;

import com.guizmaii.zazr.collection.Stream;
import com.guizmaii.zazr.control.Option;
import com.guizmaii.zazr.test.legacy.Arbitrary;


import java.util.function.Function;

/**
 * The laws of {@code Stream}.
 */
class StreamLawsTest extends SequenceLawsSuite<Stream<?>, Stream<Integer>, StreamLawsTest.Subject> {

    static final class Subject implements FlatMapSubject<Stream<?>>, ZipSubject<Stream<?>> {

        @Override
        public Arbitrary<Stream<?>> values() {
            return Arbitrary.stream(Arbitrary.integer()).map(value -> value);
        }

        @Override
        public Stream<?> map(Stream<?> fa, Function<Object, Object> f) {
            return fa.map(f);
        }

        @Override
        public Stream<?> succeed(Object a) {
            return Stream.of(a);
        }

        @Override
        public Stream<?> flatMap(Stream<?> fa, Function<Object, Stream<?>> f) {
            return fa.flatMap(f);
        }

        @Override
        public Stream<?> zip(Stream<?> fa, Stream<?> fb) {
            return fa.zip(fb);
        }
    }

    @Override
    Subject subject() {
        return new Subject();
    }

    @Override
    CollectionSubject<Integer, Stream<Integer>> collection() {
        return new CollectionSubject<>(Arbitrary.stream(Arbitrary.integer()), Stream::ofAll, Stream::size, Stream::toList, true, Option.some(IterationOrder.input()));
    }

    @Override
    BuilderLaws.CollectorSubject<Integer, Stream<Integer>> collector() {
        return new BuilderLaws.CollectorSubject<>(Arbitrary.list(Arbitrary.integer()), Stream.collector(), Stream::ofAll, Option.some(IterationOrder.input()));
    }
}
