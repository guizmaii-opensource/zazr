package com.guizmaii.zazr.collection;

import com.guizmaii.zazr.Tuple;
import com.guizmaii.zazr.Tuple2;
import java.util.ArrayList;
import java.util.Collections;
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
 * Bulk construction of TreeSet, TreeMap, HashSet, HashMap, LinkedHashSet, LinkedHashMap and List from distinct keys,
 * in random and in sorted order. Run on the branch before and after the builders to compare; the benchmark only uses
 * the public API.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Thread)
public class MapSetBuilderBenchmark {

    @Param({ "10", "1000", "100000" })
    public int size;

    private ArrayList<Integer> shuffled;
    private ArrayList<Integer> sorted;
    private ArrayList<Tuple2<Integer, Integer>> shuffledEntries;
    private ArrayList<Tuple2<Integer, Integer>> sortedEntries;
    /* a collection that is neither a java.util.List nor a NavigableSet, the inputs List.ofAll does not walk backwards */
    private java.util.ArrayDeque<Integer> deque;

    @Setup(Level.Trial)
    public void setup() {
        sorted = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            sorted.add(i);
        }
        shuffled = new ArrayList<>(sorted);
        Collections.shuffle(shuffled, new Random(42));
        sortedEntries = new ArrayList<>(size);
        for (Integer i : sorted) {
            sortedEntries.add(Tuple.of(i, i));
        }
        shuffledEntries = new ArrayList<>(size);
        for (Integer i : shuffled) {
            shuffledEntries.add(Tuple.of(i, i));
        }
        deque = new java.util.ArrayDeque<>(shuffled);
    }

    @Benchmark
    public TreeSet<Integer> treeSet_ofAll() {
        return TreeSet.ofAll(shuffled);
    }

    @Benchmark
    public TreeSet<Integer> treeSet_ofAll_sorted() {
        return TreeSet.ofAll(sorted);
    }

    @Benchmark
    public TreeSet<Integer> treeSet_collector() {
        return shuffled.stream().collect(TreeSet.collector());
    }

    @Benchmark
    public TreeMap<Integer, Integer> treeMap_ofEntries() {
        return TreeMap.ofEntries(shuffledEntries);
    }

    @Benchmark
    public TreeMap<Integer, Integer> treeMap_ofEntries_sorted() {
        return TreeMap.ofEntries(sortedEntries);
    }

    @Benchmark
    public TreeMap<Integer, Integer> treeMap_collector() {
        return shuffledEntries.stream().collect(TreeMap.collector());
    }

    @Benchmark
    public HashSet<Integer> hashSet_ofAll() {
        return HashSet.ofAll(shuffled);
    }

    @Benchmark
    public HashSet<Integer> hashSet_collector() {
        return shuffled.stream().collect(HashSet.collector());
    }

    @Benchmark
    public HashMap<Integer, Integer> hashMap_ofEntries() {
        return HashMap.ofEntries(shuffledEntries);
    }

    @Benchmark
    public HashMap<Integer, Integer> hashMap_collector() {
        return shuffledEntries.stream().collect(HashMap.collector());
    }

    @Benchmark
    public LinkedHashSet<Integer> linkedHashSet_ofAll() {
        return LinkedHashSet.ofAll(shuffled);
    }

    @Benchmark
    public LinkedHashSet<Integer> linkedHashSet_collector() {
        return shuffled.stream().collect(LinkedHashSet.collector());
    }

    @Benchmark
    public LinkedHashMap<Integer, Integer> linkedHashMap_ofEntries() {
        return LinkedHashMap.ofEntries(shuffledEntries);
    }

    @Benchmark
    public LinkedHashMap<Integer, Integer> linkedHashMap_collector() {
        return shuffledEntries.stream().collect(LinkedHashMap.collector());
    }

    @Benchmark
    public List<Integer> list_ofAll() {
        return List.ofAll(deque);
    }

    @Benchmark
    public List<Integer> list_collector() {
        return shuffled.stream().collect(List.collector());
    }
}
