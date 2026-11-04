package dev.zazr.collection;

import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Sequence operations that change nothing whatever the length: sorting, reversing and interspersing one element, and
 * the `LazyList` calls that can tell without reading an element (on a LazyList of 1000 elements, all read). Only the
 * public API is used, so the class runs on two branches.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Thread)
public class ConservingSmallBenchmark {

    private Vector<Integer> vector;
    private List<Integer> list;
    private Queue<Integer> queue;
    private LazyList<Integer> lazyList;
    private Vector<Integer> longVector;

    @Setup(Level.Trial)
    public void setup() {
        vector = Vector.of(1);
        list = List.of(1);
        queue = Queue.of(1);
        lazyList = LazyList.range(0, 1000);
        lazyList.size();
        longVector = Vector.range(0, 1000);
    }

    @Benchmark
    public Vector<Integer> vectorSortedOne() {
        return vector.sorted(Comparator.naturalOrder());
    }

    @Benchmark
    public Vector<Integer> vectorIntersperseOne() {
        return vector.intersperse(0);
    }

    @Benchmark
    public Vector<Integer> vectorReplaceAbsent() {
        return longVector.replace(-1, 0);
    }

    @Benchmark
    public List<Integer> listSortedOne() {
        return list.sorted();
    }

    @Benchmark
    public List<Integer> listIntersperseOne() {
        return list.intersperse(0);
    }

    @Benchmark
    public Queue<Integer> queueReverseOne() {
        return queue.reverse();
    }

    @Benchmark
    public Queue<Integer> queueSortedOne() {
        return queue.sorted();
    }

    @Benchmark
    public LazyList<Integer> lazyListOrElseOnARead() {
        return lazyList.orElse(List.of(0));
    }

    @Benchmark
    public LazyList<Integer> lazyListPatchWithNothing() {
        return lazyList.patch(500, List.empty(), 0);
    }

    @Benchmark
    public LazyList<Integer> lazyListCycleOnce() {
        return lazyList.cycle(1);
    }
}
