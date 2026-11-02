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
 * `combinations(k)` of `Vector`, `List` and `LazyList`, every combination read to the end. A k near n is where the
 * search of every shorter choice cost the most. Only the public API is used, so the class runs on two branches.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Thread)
public class CombinationsBenchmark {

    @Param({"16"})
    public int n;

    @Param({"2", "8", "14"})
    public int k;

    private Vector<Integer> vector;
    private List<Integer> list;
    private LazyList<Integer> lazyList;

    @Setup(Level.Trial)
    public void setup() {
        vector = Vector.range(0, n);
        list = List.range(0, n);
        lazyList = LazyList.range(0, n);
    }

    @Benchmark
    public void vector(Blackhole blackhole) {
        consumeAll(vector.combinations(k), blackhole);
    }

    @Benchmark
    public void list(Blackhole blackhole) {
        consumeAll(list.combinations(k), blackhole);
    }

    @Benchmark
    public void lazyList(Blackhole blackhole) {
        consumeAll(lazyList.combinations(k), blackhole);
    }

    // reads every element of every arrangement, so that a LazyList computes them all
    private static void consumeAll(Iterable<? extends Iterable<?>> arrangements, Blackhole blackhole) {
        for (Iterable<?> arrangement : arrangements) {
            for (Object element : arrangement) {
                blackhole.consume(element);
            }
        }
    }
}
