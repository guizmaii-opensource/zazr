package com.guizmaii.zazr.test.laws;

import com.guizmaii.zazr.collection.Stream;
import com.guizmaii.zazr.control.Option;
import com.guizmaii.zazr.test.Gen;


import java.util.function.Function;

/**
 * The laws of {@code Stream}.
 */
class StreamLawsTest extends SequenceLawsSuite<Stream<?>, Stream<Integer>, StreamLawsTest.Subject> {

    static final class Subject implements FlatMapSubject<Stream<?>>, ZipSubject<Stream<?>> {

        @Override
        public Gen<Stream<?>> values() {
            return Gen.stream(Values.integers()).map(value -> value);
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
        return new CollectionSubject<>(Gen.stream(Values.integers()), Stream::ofAll, Stream::size, Stream::toList, true, Option.some(IterationOrder.input()));
    }

    @Override
    BuilderLaws.CollectorSubject<Integer, Stream<Integer>> collector() {
        return new BuilderLaws.CollectorSubject<>(Gen.list(Values.integers()), Stream.collector(), Stream::ofAll, Option.some(IterationOrder.input()));
    }
}
