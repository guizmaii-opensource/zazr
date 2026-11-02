package dev.zazr.collection;

import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

/**
 * `rotateRight` by a third of the length on `List`, `Queue` (built by `ofAll`, and one element at a time, so that
 * most of it is at the back) and `LazyList` (every element read). Only the public API is used, so the class runs on
 * two branches.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Thread)
public class RotateRightBenchmark {

    @Param({"10", "1000", "100000"})
    public int size;

    private int distance;
    private List<Integer> list;
    private Queue<Integer> queue;
    private Queue<Integer> enqueued;
    private LazyList<Integer> lazyList;

    @Setup(Level.Trial)
    public void setup() {
        distance = size / 3;
        list = List.range(0, size);
        queue = Queue.ofAll(list);
        Queue<Integer> none = Queue.empty();
        enqueued = List.range(0, size).foldLeft(none, (q, i) -> q.enqueue(i));
        lazyList = LazyList.range(0, size);
    }

    @Benchmark
    public List<Integer> list() {
        return list.rotateRight(distance);
    }

    @Benchmark
    public Queue<Integer> queue() {
        return queue.rotateRight(distance);
    }

    @Benchmark
    public Queue<Integer> queueEnqueued() {
        return enqueued.rotateRight(distance);
    }

    @Benchmark
    public void lazyList(Blackhole blackhole) {
        for (Integer element : lazyList.rotateRight(distance)) {
            blackhole.consume(element);
        }
    }
}
