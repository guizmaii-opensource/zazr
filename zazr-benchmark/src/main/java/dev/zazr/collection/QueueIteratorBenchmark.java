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
 * The iterator of `Queue`: read to the end, on a Queue built by `ofAll` (every element at the front) and on one built
 * one element at a time (all but the first at the back), and the first element only of the latter. Only the public
 * API is used, so the class runs on two branches.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Thread)
public class QueueIteratorBenchmark {

    @Param({"10", "1000", "100000"})
    public int size;

    private Queue<Integer> ofAll;
    private Queue<Integer> enqueued;

    @Setup(Level.Trial)
    public void setup() {
        ofAll = Queue.ofAll(List.range(0, size));
        Queue<Integer> none = Queue.empty();
        enqueued = List.range(0, size).foldLeft(none, (q, i) -> q.enqueue(i));
    }

    @Benchmark
    public void ofAll(Blackhole blackhole) {
        for (Integer element : ofAll) {
            blackhole.consume(element);
        }
    }

    @Benchmark
    public void enqueued(Blackhole blackhole) {
        for (Integer element : enqueued) {
            blackhole.consume(element);
        }
    }

    @Benchmark
    public Integer enqueuedFirst() {
        return enqueued.iterator().next();
    }
}
