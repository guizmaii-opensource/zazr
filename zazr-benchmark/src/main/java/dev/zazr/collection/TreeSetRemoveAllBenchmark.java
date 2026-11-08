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
 * `TreeSet.removeAll` of k elements of the set, in an order of their own, given as a `Vector` (its size known in
 * advance) and as a one-shot `Iterable` (its size unknown), and of k elements that are not in the set. The counts
 * include those around the switch points: the first `L` = n / (2 b) elements found (b the bit length of n) are
 * deleted one by one, the next `3 L` are kept and deleted when the argument ends there, and past them one walk of
 * the tree removes the elements found. `L+1`, `L+3L` and `L+3L+1` sit on both sides of them (at n = 4096, L = 157;
 * at n = 100,000, L = 2941). Only the public API is used, so the class runs on two branches. Run with `-prof gc` for
 * the allocation per call.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Thread)
public class TreeSetRemoveAllBenchmark {

    @Param({"4096", "100000"})
    public int size;

    @Param({"3", "10", "64", "L+1", "L+3L", "L+3L+1", "n/16", "n/2"})
    public String removed;

    private TreeSet<Integer> set;
    private Vector<Integer> present;
    private Iterable<Integer> presentOneShot;
    private Vector<Integer> absent;

    @Setup(Level.Trial)
    public void setup() {
        int limit = Math.max(1, size / (2 * Math.max(1, 32 - Integer.numberOfLeadingZeros(size))));
        int k = switch (removed) {
            case "L+1" -> limit + 1;
            case "L+3L" -> limit + 3 * limit;
            case "L+3L+1" -> limit + 3 * limit + 1;
            case "n/16" -> size / 16;
            case "n/2" -> size / 2;
            default -> Integer.parseInt(removed);
        };
        java.util.List<Integer> keys = new java.util.ArrayList<>();
        for (int i = 0; i < size; i++) {
            keys.add(2 * i);
        }
        set = TreeSet.ofAll(keys);
        java.util.Collections.shuffle(keys, new java.util.Random(42));
        java.util.List<Integer> chosen = java.util.List.copyOf(keys.subList(0, k));
        present = Vector.ofAll(chosen);
        // a lambda, not a collection: its size is not known in advance
        presentOneShot = chosen::iterator;
        absent = present.map(key -> key + 1);
    }

    @Benchmark
    public TreeSet<Integer> removeVector() {
        return set.removeAll(present);
    }

    @Benchmark
    public TreeSet<Integer> removeOneShot() {
        return set.removeAll(presentOneShot);
    }

    @Benchmark
    public TreeSet<Integer> removeAbsent() {
        return set.removeAll(absent);
    }
}
