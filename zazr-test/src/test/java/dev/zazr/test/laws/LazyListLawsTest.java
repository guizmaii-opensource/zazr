package dev.zazr.test.laws;

import dev.zazr.collection.LazyList;
import dev.zazr.control.Option;
import dev.zazr.test.Gen;


import java.util.function.Function;

/**
 * The laws of {@code LazyList}.
 */
class LazyListLawsTest extends SequenceLawsSuite<LazyList<?>, LazyList<Integer>, LazyListLawsTest.Subject> {

    static final class Subject implements FlatMapSubject<LazyList<?>>, ZipSubject<LazyList<?>> {

        @Override
        public Gen<LazyList<?>> values() {
            return Gen.lazyList(Values.integers()).map(value -> value);
        }

        @Override
        public LazyList<?> map(LazyList<?> fa, Function<Object, Object> f) {
            return fa.map(f);
        }

        @Override
        public LazyList<?> succeed(Object a) {
            return LazyList.of(a);
        }

        @Override
        public LazyList<?> flatMap(LazyList<?> fa, Function<Object, LazyList<?>> f) {
            return fa.flatMap(f);
        }

        @Override
        public LazyList<?> zip(LazyList<?> fa, LazyList<?> fb) {
            return fa.zip(fb);
        }
    }

    @Override
    Subject subject() {
        return new Subject();
    }

    @Override
    CollectionSubject<Integer, LazyList<Integer>> collection() {
        return new CollectionSubject<>(Gen.lazyList(Values.integers()), LazyList::ofAll, LazyList::size, LazyList::toList, true, Option.some(IterationOrder.input()));
    }

    @Override
    BuilderLaws.CollectorSubject<Integer, LazyList<Integer>> collector() {
        return new BuilderLaws.CollectorSubject<>(Gen.list(Values.integers()), LazyList.collector(), LazyList::ofAll, Option.some(IterationOrder.input()));
    }
}
