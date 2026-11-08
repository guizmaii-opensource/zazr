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
 * {@code values()} of HashMap, TreeMap and LinkedHashMap on random distinct keys, through the public API only, so the
 * same class runs on two branches to compare them.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(3)
@State(Scope.Thread)
public class MapValuesBenchmark {

    @Param({"1000", "100000"})
    public int size;

    private HashMap<Integer, Integer> hashMap;
    private TreeMap<Integer, Integer> treeMap;
    private LinkedHashMap<Integer, Integer> linkedHashMap;

    @Setup(Level.Trial)
    public void setup() {
        Random random = new Random(42);
        java.util.LinkedHashSet<Integer> keys = new java.util.LinkedHashSet<>();
        while (keys.size() < size) {
            keys.add(random.nextInt());
        }
        Vector<Integer> all = Vector.ofAll(keys);
        hashMap = all.foldLeft(HashMap.empty(), (m, key) -> m.put(key, key));
        treeMap = all.foldLeft(TreeMap.empty(), (m, key) -> m.put(key, key));
        linkedHashMap = all.foldLeft(LinkedHashMap.empty(), (m, key) -> m.put(key, key));
    }

    @Benchmark
    public Vector<Integer> hashMapValues() {
        return hashMap.values();
    }

    @Benchmark
    public Vector<Integer> treeMapValues() {
        return treeMap.values();
    }

    @Benchmark
    public Vector<Integer> linkedHashMapValues() {
        return linkedHashMap.values();
    }
}
