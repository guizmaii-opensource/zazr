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

/**
 * `HashSet.removeAll` of a `java.util.List`: one present element, half of the elements, and as many absent ones as
 * the set holds. Only the public API is used, so the class runs on two branches.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Thread)
public class HashSetRemoveAllBenchmark {

    @Param({"10", "1000", "100000"})
    public int size;

    private HashSet<Integer> set;
    private java.util.List<Integer> one;
    private java.util.List<Integer> half;
    private java.util.List<Integer> absent;

    @Setup(Level.Trial)
    public void setup() {
        java.util.Random random = new java.util.Random(42);
        java.util.LinkedHashSet<Integer> keys = new java.util.LinkedHashSet<>();
        while (keys.size() < 2 * size) {
            keys.add(random.nextInt());
        }
        java.util.List<Integer> all = new java.util.ArrayList<>(keys);
        set = HashSet.ofAll(all.subList(0, size));
        one = java.util.List.of(all.get(size / 2));
        half = java.util.List.copyOf(all.subList(0, size / 2));
        absent = java.util.List.copyOf(all.subList(size, 2 * size));
    }

    @Benchmark
    public HashSet<Integer> removeOne() {
        return set.removeAll(one);
    }

    @Benchmark
    public HashSet<Integer> removeHalf() {
        return set.removeAll(half);
    }

    @Benchmark
    public HashSet<Integer> removeAbsent() {
        return set.removeAll(absent);
    }
}
