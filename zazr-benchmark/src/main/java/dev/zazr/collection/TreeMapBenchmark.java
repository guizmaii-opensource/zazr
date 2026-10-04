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
 * Benchmarks the key lookups of {@link TreeMap} through its public API only: one lookup per invocation, so that the
 * bytes allocated per operation ({@code -prof gc}) are the bytes of one lookup. The keys are boxed once, in the
 * setup; a miss looks up an odd key, between two stored ones or, for the largest, above them all.
 *
 * <p>Run via {@code dev.zazr.JmhRunner}, or {@code org.openjdk.jmh.Main} for {@code -prof gc}.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Thread)
public class TreeMapBenchmark {

    @Param({"1000"})
    public int size;

    private Integer[] hits;
    private Integer[] misses;
    private TreeMap<Integer, Integer> map;
    private int cursor;

    @Setup(Level.Trial)
    public void setup() {
        Random random = new Random(0xC0FFEE);
        // the stored keys are even, the missing ones odd
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
        map = Vector.ofAll(stored).foldLeft(TreeMap.empty(), (m, key) -> m.put(key, key));
    }

    private Integer nextHit() {
        return hits[cursor++ % size];
    }

    private Integer nextMiss() {
        return misses[cursor++ % size];
    }

    @Benchmark
    public Object getHit() {
        return map.get(nextHit());
    }

    @Benchmark
    public Object getMiss() {
        return map.get(nextMiss());
    }

    @Benchmark
    public Integer getOrElseHit() {
        return map.getOrElse(nextHit(), -1);
    }

    @Benchmark
    public Integer getOrElseMiss() {
        return map.getOrElse(nextMiss(), -1);
    }

    @Benchmark
    public boolean containsKeyHit() {
        return map.containsKey(nextHit());
    }

    @Benchmark
    public boolean containsKeyMiss() {
        return map.containsKey(nextMiss());
    }
}
