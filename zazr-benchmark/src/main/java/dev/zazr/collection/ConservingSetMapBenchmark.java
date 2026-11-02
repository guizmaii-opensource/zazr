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
 * Operations of the sets and maps given arguments that change nothing, each with a control that changes something,
 * so the changing path is shown too. Only the public API is used, so the class runs on two branches.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Thread)
public class ConservingSetMapBenchmark {

    @Param({"10", "10000"})
    public int size;

    private static final Integer OTHER = Integer.MIN_VALUE;

    private HashSet<Integer> hashSet;
    private TreeSet<Integer> treeSet;
    private HashMap<Integer, Integer> hashMap;
    private TreeMap<Integer, Integer> treeMap;
    private LinkedHashMap<Integer, Integer> linkedMap;
    private NonEmptyMap<Integer, Integer> nonEmptyMap;
    // an element and an entry in the middle, the stored objects
    private Integer element;
    private Integer key;
    private Integer value;
    private Integer last;

    @Setup(Level.Trial)
    public void setup() {
        java.util.Random random = new java.util.Random(42);
        java.util.LinkedHashSet<Integer> keys = new java.util.LinkedHashSet<>();
        while (keys.size() < size) {
            keys.add(random.nextInt(Integer.MAX_VALUE));
        }
        java.util.List<Integer> all = new java.util.ArrayList<>(keys);
        java.util.List<Tuple2<Integer, Integer>> entries = new java.util.ArrayList<>();
        for (Integer k : all) {
            entries.add(new Tuple2<>(k, Integer.valueOf(k.intValue() / 2 + 1_000)));
        }
        hashSet = HashSet.ofAll(all);
        treeSet = TreeSet.ofAll(all);
        hashMap = HashMap.ofEntries(entries);
        treeMap = TreeMap.ofEntries(entries);
        linkedMap = LinkedHashMap.ofEntries(entries);
        nonEmptyMap = NonEmptyMap.fromMap(hashMap).get();
        element = hashSet.filter(e -> e.equals(all.get(size / 2))).iterator().next();
        key = treeMap.keySet()
                .filter(k -> k.equals(all.get(size / 2)))
                .iterator()
                .next();
        value = treeMap.get(key).get();
        last = all.get(size - 1);
    }

    // -- TreeMap.put: the key and value already there, and a new value

    @Benchmark
    public TreeMap<Integer, Integer> treeMapPutSame() {
        return treeMap.put(key, value);
    }

    @Benchmark
    public TreeMap<Integer, Integer> treeMapPutChanged() {
        return treeMap.put(key, OTHER);
    }

    // -- TreeMap.mapValues: every value the same object, and every value changed

    @Benchmark
    public TreeMap<Integer, Integer> treeMapMapValuesSame() {
        return treeMap.mapValues(v -> v);
    }

    @Benchmark
    public TreeMap<Integer, Integer> treeMapMapValuesChanged() {
        return treeMap.mapValues(v -> OTHER);
    }

    // -- LinkedHashMap: put, filter and mapValues

    @Benchmark
    public LinkedHashMap<Integer, Integer> linkedMapPutSame() {
        return linkedMap.put(key, value);
    }

    @Benchmark
    public LinkedHashMap<Integer, Integer> linkedMapPutChanged() {
        return linkedMap.put(key, OTHER);
    }

    @Benchmark
    public LinkedHashMap<Integer, Integer> linkedMapFilterAll() {
        return linkedMap.filter((k, v) -> true);
    }

    // the last entry dropped: the entries before it are read twice
    @Benchmark
    public LinkedHashMap<Integer, Integer> linkedMapFilterLast() {
        return linkedMap.filter((k, v) -> !k.equals(last));
    }

    @Benchmark
    public LinkedHashMap<Integer, Integer> linkedMapMapValuesSame() {
        return linkedMap.mapValues(v -> v);
    }

    @Benchmark
    public LinkedHashMap<Integer, Integer> linkedMapMapValuesChanged() {
        return linkedMap.mapValues(v -> OTHER);
    }

    // -- partition: everything on one side, and half on each

    @Benchmark
    public Tuple2<HashSet<Integer>, HashSet<Integer>> hashSetPartitionAll() {
        return hashSet.partition(e -> true);
    }

    @Benchmark
    public Tuple2<HashSet<Integer>, HashSet<Integer>> hashSetPartitionHalf() {
        return hashSet.partition(e -> (e & 1) == 0);
    }

    @Benchmark
    public Tuple2<HashMap<Integer, Integer>, HashMap<Integer, Integer>> hashMapPartitionAll() {
        return hashMap.partition(e -> true);
    }

    // -- TreeSet with itself

    @Benchmark
    public TreeSet<Integer> treeSetUnionSelf() {
        return treeSet.union(treeSet);
    }

    @Benchmark
    public TreeSet<Integer> treeSetIntersectSelf() {
        return treeSet.intersect(treeSet);
    }

    // -- replace of an element by itself, and by another

    @Benchmark
    public HashSet<Integer> hashSetReplaceSame() {
        return hashSet.replace(element, element);
    }

    @Benchmark
    public HashSet<Integer> hashSetReplaceChanged() {
        return hashSet.replace(element, OTHER);
    }

    // -- NonEmptyMap.put of the key and value already there

    @Benchmark
    public NonEmptyMap<Integer, Integer> nonEmptyMapPutSame() {
        return nonEmptyMap.put(key, value);
    }
}
