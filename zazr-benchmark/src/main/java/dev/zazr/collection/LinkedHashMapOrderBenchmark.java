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
import org.openjdk.jmh.infra.Blackhole;

/**
 * The insertion order of `LinkedHashMap` and `LinkedHashSet` after removals, each call on one older map:
 * <ul>
 *   <li>{@code front*}: 2 * size keys put, then those of ranks 1 to size - 1 removed, so that a run of size - 1
 *   removed keys follows the first one: {@code tail}, {@code drop(2)} and {@code take(2)} cut past it;</li>
 *   <li>{@code back*}: the mirror, a run of size - 2 removed keys before the last one: {@code init},
 *   {@code dropRight(2)} and {@code takeRight(2)};</li>
 *   <li>{@code removeAtThreshold}: 2 * size keys put, then size of them removed inside the order (one key in two and
 *   one more), and one more removed from that same map at each call;</li>
 *   <li>{@code head}, {@code last} and {@code iterate} on a map with no removal and on the {@code front} map.</li>
 * </ul>
 * {@link LinkedHashMapBenchmark} covers put, get, remove and iteration of a map with no removal. Only the public API
 * is used, so the class runs on two branches.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Thread)
public class LinkedHashMapOrderBenchmark {

    @Param({"1000", "100000"})
    public int size;

    private LinkedHashMap<Integer, Integer> full;
    private LinkedHashMap<Integer, Integer> front;
    private LinkedHashMap<Integer, Integer> back;
    private LinkedHashMap<Integer, Integer> atThreshold;
    private LinkedHashSet<Integer> frontSet;
    private Integer[] atThresholdKeys;
    private int cursor;

    @Setup(Level.Trial)
    public void setup() {
        int keys = 2 * size;
        @SuppressWarnings("Var") // the setup builds the maps one key at a time, as a user would
        LinkedHashMap<Integer, Integer> map = LinkedHashMap.empty();
        for (int key = 0; key < keys; key++) {
            map = map.put(key, key);
        }
        full = map;

        @SuppressWarnings("Var")
        LinkedHashMap<Integer, Integer> holes = full;
        for (int key = 1; key < size; key++) {
            holes = holes.remove(key);
        }
        front = holes;

        holes = full;
        for (int key = size + 1; key < keys - 1; key++) {
            holes = holes.remove(key);
        }
        back = holes;

        // the odd keys but the last one, and key 2: size keys removed, size kept, the first and the last kept
        holes = full;
        for (int key = 1; key < keys - 1; key += 2) {
            holes = holes.remove(key);
        }
        atThreshold = holes.remove(2);
        java.util.List<Integer> kept = new java.util.ArrayList<>();
        for (int key = 4; key < keys - 2; key += 2) {
            kept.add(key);
        }
        atThresholdKeys = kept.toArray(new Integer[0]);

        @SuppressWarnings("Var")
        LinkedHashSet<Integer> set = LinkedHashSet.empty();
        for (int key = 0; key < keys; key++) {
            set = set.add(key);
        }
        for (int key = 1; key < size; key++) {
            set = set.remove(key);
        }
        frontSet = set;
    }

    // -- a run of removed keys after the first one

    @Benchmark
    public LinkedHashMap<Integer, Integer> frontTail() {
        return front.tail();
    }

    @Benchmark
    public LinkedHashMap<Integer, Integer> frontDrop2() {
        return front.drop(2);
    }

    @Benchmark
    public LinkedHashMap<Integer, Integer> frontTake2() {
        return front.take(2);
    }

    @Benchmark
    public LinkedHashSet<Integer> frontSetTail() {
        return frontSet.tail();
    }

    // -- a run of removed keys before the last one

    @Benchmark
    public LinkedHashMap<Integer, Integer> backInit() {
        return back.init();
    }

    @Benchmark
    public LinkedHashMap<Integer, Integer> backDropRight2() {
        return back.dropRight(2);
    }

    @Benchmark
    public LinkedHashMap<Integer, Integer> backTakeRight2() {
        return back.takeRight(2);
    }

    // -- one more removal from a map with as many removed keys as kept ones

    @Benchmark
    public LinkedHashMap<Integer, Integer> removeAtThreshold() {
        return atThreshold.remove(atThresholdKeys[cursor++ % atThresholdKeys.length]);
    }

    // -- must not regress

    @Benchmark
    public Tuple2<Integer, Integer> fullHead() {
        return full.head();
    }

    @Benchmark
    public Tuple2<Integer, Integer> fullLast() {
        return full.last();
    }

    @Benchmark
    public LinkedHashMap<Integer, Integer> fullTail() {
        return full.tail();
    }

    @Benchmark
    public LinkedHashMap<Integer, Integer> fullInit() {
        return full.init();
    }

    @Benchmark
    public void fullIterate(Blackhole bh) {
        for (Tuple2<Integer, Integer> entry : full) {
            bh.consume(entry);
        }
    }

    @Benchmark
    public void frontIterate(Blackhole bh) {
        for (Tuple2<Integer, Integer> entry : front) {
            bh.consume(entry);
        }
    }

    @Benchmark
    public LinkedHashMap<Integer, Integer> tailChain() {
        @SuppressWarnings("Var") // the loop is what the benchmark measures
        LinkedHashMap<Integer, Integer> m = front;
        while (!m.isEmpty()) {
            m = m.tail();
        }
        return m;
    }
}
