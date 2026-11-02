package dev.zazr.collection;

import dev.zazr.Tuple2;
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

/**
 * The rebalancing of `Queue`: `tail`, `dequeue` and `init` called again and again on one older Queue that is about to
 * be rebalanced, the same after one `enqueue` on that Queue (a new version each time), and the chains every Queue
 * user runs (enqueue then drain, enqueue and dequeue alternating). Only the public API is used, so the class runs on
 * two branches.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Thread)
public class QueueRebalanceBenchmark {

    @Param({"10", "1000", "100000"})
    public int size;

    // one element at the front, all the others at the back: tail() and dequeue() put the back in order
    private Queue<Integer> enqueued;
    // every element at the front: init() moves half of them to the back
    private Queue<Integer> ofAll;
    private int counter;

    @Setup(Level.Trial)
    public void setup() {
        Queue<Integer> none = Queue.empty();
        enqueued = List.range(0, size).foldLeft(none, (q, i) -> q.enqueue(i));
        ofAll = Queue.ofAll(List.range(0, size));
    }

    // -- the same older version again and again

    @Benchmark
    public Queue<Integer> tailOfOlderVersion() {
        return enqueued.tail();
    }

    @Benchmark
    public Tuple2<Integer, Queue<Integer>> dequeueOfOlderVersion() {
        return enqueued.dequeue();
    }

    @Benchmark
    public Queue<Integer> initOfOlderVersion() {
        return ofAll.init();
    }

    // -- a new version of the older one each time: the rebalancing is paid again

    @Benchmark
    public Queue<Integer> tailAfterEnqueueOnOlderVersion() {
        return enqueued.enqueue(counter++).tail();
    }

    // -- the chains, each call on the result of the previous one

    @Benchmark
    public Queue<Integer> enqueueThenDrain() {
        @SuppressWarnings("Var") // the loop is what the benchmark measures
        Queue<Integer> q = Queue.empty();
        for (int i = 0; i < size; i++) {
            q = q.enqueue(i);
        }
        while (!q.isEmpty()) {
            q = q.tail();
        }
        return q;
    }

    @Benchmark
    public Queue<Integer> enqueueDequeueAlternating() {
        @SuppressWarnings("Var") // the loop is what the benchmark measures
        Queue<Integer> q = enqueued;
        for (int i = 0; i < size; i++) {
            q = q.enqueue(i).dequeue()._2();
        }
        return q;
    }

    @Benchmark
    public Queue<Integer> initChain() {
        @SuppressWarnings("Var") // the loop is what the benchmark measures
        Queue<Integer> q = ofAll;
        while (!q.isEmpty()) {
            q = q.init();
        }
        return q;
    }
}
