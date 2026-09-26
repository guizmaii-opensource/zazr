package dev.zazr.test.laws;

import dev.zazr.collection.Queue;
import dev.zazr.control.Option;
import dev.zazr.test.Gen;


import java.util.function.Function;

/**
 * The laws of {@code Queue}.
 */
class QueueLawsTest extends SequenceLawsSuite<Queue<?>, Queue<Integer>, QueueLawsTest.Subject> {

    static final class Subject implements FlatMapSubject<Queue<?>>, ZipSubject<Queue<?>> {

        @Override
        public Gen<Queue<?>> values() {
            return Gen.queue(Values.integers()).map(value -> value);
        }

        @Override
        public Queue<?> map(Queue<?> fa, Function<Object, Object> f) {
            return fa.map(f);
        }

        @Override
        public Queue<?> succeed(Object a) {
            return Queue.of(a);
        }

        @Override
        public Queue<?> flatMap(Queue<?> fa, Function<Object, Queue<?>> f) {
            return fa.flatMap(f);
        }

        @Override
        public Queue<?> zip(Queue<?> fa, Queue<?> fb) {
            return fa.zip(fb);
        }
    }

    @Override
    Subject subject() {
        return new Subject();
    }

    @Override
    CollectionSubject<Integer, Queue<Integer>> collection() {
        return new CollectionSubject<>(Gen.queue(Values.integers()), Queue::ofAll, Queue::size, Queue::toList, true, Option.some(IterationOrder.input()));
    }

    @Override
    BuilderLaws.CollectorSubject<Integer, Queue<Integer>> collector() {
        return new BuilderLaws.CollectorSubject<>(Gen.list(Values.integers()), Queue.collector(), Queue::ofAll, Option.some(IterationOrder.input()));
    }
}
