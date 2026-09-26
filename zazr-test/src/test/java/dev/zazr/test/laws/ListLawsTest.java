package dev.zazr.test.laws;

import dev.zazr.collection.List;
import dev.zazr.control.Option;
import dev.zazr.test.Gen;


import java.util.function.Function;

/**
 * The laws of {@code List}.
 */
class ListLawsTest extends SequenceLawsSuite<List<?>, List<Integer>, ListLawsTest.Subject> {

    static final class Subject implements FlatMapSubject<List<?>>, ZipSubject<List<?>> {

        @Override
        public Gen<List<?>> values() {
            return Gen.list(Values.integers()).map(value -> value);
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
        return new CollectionSubject<>(Gen.list(Values.integers()), List::ofAll, List::size, List::toList, true, Option.some(IterationOrder.input()));
    }

    @Override
    BuilderLaws.CollectorSubject<Integer, List<Integer>> collector() {
        return new BuilderLaws.CollectorSubject<>(Gen.list(Values.integers()), List.collector(), List::ofAll, Option.some(IterationOrder.input()));
    }
}
