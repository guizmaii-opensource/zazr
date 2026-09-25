package com.guizmaii.zazr.test.laws;

import com.guizmaii.zazr.collection.Queue;
import com.guizmaii.zazr.control.Option;
import com.guizmaii.zazr.test.Arbitrary;


import java.util.function.Function;

/**
 * The laws of {@code Queue}.
 */
class QueueLawsTest extends SequenceLawsSuite<Queue<?>, Queue<Integer>, QueueLawsTest.Subject> {

    static final class Subject implements FlatMapSubject<Queue<?>>, ZipSubject<Queue<?>> {

        @Override
        public Arbitrary<Queue<?>> values() {
            return Arbitrary.queue(Arbitrary.integer()).map(value -> value);
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
        return new CollectionSubject<>(Arbitrary.queue(Arbitrary.integer()), Queue::ofAll, Queue::size, Queue::toList, true, Option.some(IterationOrder.input()));
    }

    @Override
    BuilderLaws.CollectorSubject<Integer, Queue<Integer>> collector() {
        return new BuilderLaws.CollectorSubject<>(Arbitrary.list(Arbitrary.integer()), Queue.collector(), Queue::ofAll, Option.some(IterationOrder.input()));
    }
}
