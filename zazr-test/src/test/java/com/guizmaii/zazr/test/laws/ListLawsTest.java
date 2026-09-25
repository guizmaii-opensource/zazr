package com.guizmaii.zazr.test.laws;

import com.guizmaii.zazr.collection.List;
import com.guizmaii.zazr.control.Option;
import com.guizmaii.zazr.test.Arbitrary;


import java.util.function.Function;

/**
 * The laws of {@code List}.
 */
class ListLawsTest extends SequenceLawsSuite<List<?>, List<Integer>, ListLawsTest.Subject> {

    static final class Subject implements FlatMapSubject<List<?>>, ZipSubject<List<?>> {

        @Override
        public Arbitrary<List<?>> values() {
            return Arbitrary.list(Arbitrary.integer()).map(value -> value);
        }

        @Override
        public List<?> map(List<?> fa, Function<Object, Object> f) {
            return fa.map(f);
        }

        @Override
        public List<?> succeed(Object a) {
            return List.of(a);
        }

        @Override
        public List<?> flatMap(List<?> fa, Function<Object, List<?>> f) {
            return fa.flatMap(f);
        }

        @Override
        public List<?> zip(List<?> fa, List<?> fb) {
            return fa.zip(fb);
        }
    }

    @Override
    Subject subject() {
        return new Subject();
    }

    @Override
    CollectionSubject<Integer, List<Integer>> collection() {
        return new CollectionSubject<>(Arbitrary.list(Arbitrary.integer()), List::ofAll, List::size, List::toList, true, Option.some(IterationOrder.input()));
    }

    @Override
    BuilderLaws.CollectorSubject<Integer, List<Integer>> collector() {
        return new BuilderLaws.CollectorSubject<>(Arbitrary.list(Arbitrary.integer()), List.collector(), List::ofAll, Option.some(IterationOrder.input()));
    }
}
