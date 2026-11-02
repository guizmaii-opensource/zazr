package dev.zazr.collection;

import java.util.Random;
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
 * Benchmarks the removals of {@link TreeSet} through its public API only: one removal per invocation, so that the
 * bytes allocated per operation ({@code -prof gc}) are the bytes of one of them. The elements are boxed once, in the
 * setup; a miss removes an odd element, between two stored ones or, for the largest, above them all.
 *
 * <p>Run via {@code dev.zazr.JmhRunner}, or {@code org.openjdk.jmh.Main} for {@code -prof gc}.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Thread)
public class TreeSetBenchmark {

    @Param({"1000"})
    public int size;

    private Integer[] hits;
    private Integer[] misses;
    private TreeSet<Integer> set;
    private int cursor;

    @Setup(Level.Trial)
    public void setup() {
        Random random = new Random(0xC0FFEE);
        // the stored elements are even, the missing ones odd
        java.util.List<Integer> stored = new java.util.ArrayList<>();
        java.util.List<Integer> missing = new java.util.ArrayList<>();
        for (int i = 0; i < size; i++) {
            stored.add(2 * i);
            missing.add(2 * i + 1);
        }
        java.util.Collections.shuffle(stored, random);
        java.util.Collections.shuffle(missing, random);
        hits = stored.toArray(new Integer[0]);
        misses = missing.toArray(new Integer[0]);
        set = Vector.ofAll(stored).foldLeft(TreeSet.empty(), TreeSet::add);
    }

    private Integer nextHit() {
        return hits[cursor++ % size];
    }

    private Integer nextMiss() {
        return misses[cursor++ % size];
    }

    // a removal of a stored element rebuilds the path to it; the result is returned so that it is not discarded
    @Benchmark
    public TreeSet<Integer> removeHit() {
        return set.remove(nextHit());
    }

    @Benchmark
    public TreeSet<Integer> removeMiss() {
        return set.remove(nextMiss());
    }
}
