package dev.zazr.collection;

import dev.zazr.Tuple2;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Operations that can share the subtrees of their inputs: {@code TreeSet.diff} with a TreeSet, {@code partition} of
 * {@code HashSet} and {@code HashMap}, {@code putAll} of a {@code HashMap} or a {@code TreeMap} of the same kind. Each
 * is given arguments where sharing pays (derived from the receiver, a few elements) and where it cannot (disjoint,
 * built independently, every node split), so a regression shows. Only the public API is used, so the class runs on
 * two branches.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Thread)
public class ShareSubtreesBenchmark {

    @Param({"10", "10000"})
    public int size;

    private TreeSet<Integer> treeSet;
    // three elements of treeSet, the same objects
    private TreeSet<Integer> treeSetFew;
    // every second element of treeSet
    private TreeSet<Integer> treeSetHalf;
    // treeSet without a tenth of its elements: most of its nodes are those of treeSet
    private TreeSet<Integer> treeSetDerived;
    // numbers between those of treeSet, none equal to one of them
    private TreeSet<Integer> treeSetDisjoint;
    // the elements of treeSet, in a tree built on its own
    private TreeSet<Integer> treeSetCopy;

    private HashSet<Integer> hashSet;
    private HashMap<Integer, Integer> hashMap;
    // hashMap without one key, without a tenth of its keys, with one new key
    private HashMap<Integer, Integer> hashMapRemoved;
    private HashMap<Integer, Integer> hashMapFiltered;
    private HashMap<Integer, Integer> hashMapAdded;
    // keys none of hashMap holds, and the entries of hashMap in a trie built on its own
    private HashMap<Integer, Integer> hashMapDisjoint;
    private HashMap<Integer, Integer> hashMapCopy;

    private TreeMap<Integer, Integer> treeMap;
    // treeMap with one value replaced, with one new key, without one key
    private TreeMap<Integer, Integer> treeMapReplaced;
    private TreeMap<Integer, Integer> treeMapAdded;
    private TreeMap<Integer, Integer> treeMapRemoved;
    private TreeMap<Integer, Integer> treeMapDisjoint;
    private TreeMap<Integer, Integer> treeMapCopy;

    // half the elements, scattered over every node of a trie
    private final Predicate<Integer> half = i -> (Integer.rotateRight(i * 0x9E3779B9, 7) & 1) == 0;
    // the elements of half the slots of the root of a trie: whole subtrees go to one side
    private final Predicate<Integer> halfOfTheRoot = i -> (spread(i) & 31) < 16;
    private final Predicate<Tuple2<Integer, Integer>> halfEntries = e -> half.test(e._1());
    private final Predicate<Tuple2<Integer, Integer>> halfOfTheRootEntries = e -> halfOfTheRoot.test(e._1());

    // the bits of a hash code a trie reads, its first level in the lowest five
    private static int spread(int hashCode) {
        int h1 = hashCode + ~(hashCode << 9);
        int h2 = h1 ^ (h1 >>> 14);
        int h3 = h2 + (h2 << 4);
        return h3 ^ (h3 >>> 10);
    }

    @Setup
    public void setup() {
        java.util.Random random = new java.util.Random(42);
        java.util.LinkedHashSet<Integer> distinct = new java.util.LinkedHashSet<>();
        while (distinct.size() < size) {
            // even numbers, so that the odd ones are free for the disjoint arguments
            distinct.add(2 * random.nextInt(Integer.MAX_VALUE / 2));
        }
        java.util.List<Integer> keys = new java.util.ArrayList<>(distinct);
        java.util.List<Integer> odd = new java.util.ArrayList<>();
        java.util.List<Integer> copies = new java.util.ArrayList<>();
        for (Integer k : keys) {
            odd.add(k + 1);
            copies.add(Integer.valueOf(k.intValue()));
        }
        int tenth = Math.max(1, size / 10);

        treeSet = TreeSet.ofAll(keys);
        treeSetFew = TreeSet.of(keys.get(0), keys.get(size / 2), keys.get(size - 1));
        java.util.List<Integer> sorted = new java.util.ArrayList<>(keys);
        java.util.Collections.sort(sorted);
        java.util.List<Integer> everySecond = new java.util.ArrayList<>();
        for (int i = 0; i < size; i += 2) {
            everySecond.add(sorted.get(i));
        }
        treeSetHalf = TreeSet.ofAll(everySecond);
        treeSetDerived = treeSet.removeAll(keys.subList(0, tenth));
        treeSetDisjoint = TreeSet.ofAll(odd);
        treeSetCopy = TreeSet.ofAll(copies);

        java.util.List<Tuple2<Integer, Integer>> entries = new java.util.ArrayList<>();
        java.util.List<Tuple2<Integer, Integer>> oddEntries = new java.util.ArrayList<>();
        for (int i = 0; i < size; i++) {
            entries.add(new Tuple2<>(keys.get(i), odd.get(i)));
            oddEntries.add(new Tuple2<>(odd.get(i), keys.get(i)));
        }
        hashSet = HashSet.ofAll(keys);
        hashMap = HashMap.ofEntries(entries);
        hashMapRemoved = hashMap.remove(keys.get(size / 2));
        hashMapFiltered = hashMap.removeAll(keys.subList(0, tenth));
        hashMapAdded = hashMap.put(odd.get(0), keys.get(0));
        hashMapDisjoint = HashMap.ofEntries(oddEntries);
        hashMapCopy = HashMap.ofEntries(new java.util.ArrayList<>(entries));

        treeMap = TreeMap.ofEntries(entries);
        treeMapReplaced = treeMap.put(keys.get(size / 2), Integer.valueOf(-1));
        treeMapAdded = treeMap.put(odd.get(size / 2), keys.get(0));
        treeMapRemoved = treeMap.remove(keys.get(size / 2));
        treeMapDisjoint = TreeMap.ofEntries(oddEntries);
        treeMapCopy = TreeMap.ofEntries(new java.util.ArrayList<>(entries));
    }

    // -- TreeSet.diff with a TreeSet of the same comparator

    @Benchmark
    public TreeSet<Integer> treeSetDiffFew() {
        return treeSet.diff(treeSetFew);
    }

    @Benchmark
    public TreeSet<Integer> treeSetDiffHalf() {
        return treeSet.diff(treeSetHalf);
    }

    @Benchmark
    public TreeSet<Integer> treeSetDiffDerived() {
        return treeSet.diff(treeSetDerived);
    }

    @Benchmark
    public TreeSet<Integer> treeSetDiffDisjoint() {
        return treeSet.diff(treeSetDisjoint);
    }

    @Benchmark
    public TreeSet<Integer> treeSetDiffCopy() {
        return treeSet.diff(treeSetCopy);
    }

    @Benchmark
    public TreeSet<Integer> treeSetDiffOfFew() {
        return treeSetFew.diff(treeSet);
    }

    // -- partition

    @Benchmark
    public Tuple2<HashSet<Integer>, HashSet<Integer>> hashSetPartitionHalf() {
        return hashSet.partition(half);
    }

    @Benchmark
    public Tuple2<HashSet<Integer>, HashSet<Integer>> hashSetPartitionHalfOfTheRoot() {
        return hashSet.partition(halfOfTheRoot);
    }

    @Benchmark
    public Tuple2<HashSet<Integer>, HashSet<Integer>> hashSetPartitionAll() {
        return hashSet.partition(i -> true);
    }

    @Benchmark
    public Tuple2<HashMap<Integer, Integer>, HashMap<Integer, Integer>> hashMapPartitionHalf() {
        return hashMap.partition(halfEntries);
    }

    @Benchmark
    public Tuple2<HashMap<Integer, Integer>, HashMap<Integer, Integer>> hashMapPartitionHalfOfTheRoot() {
        return hashMap.partition(halfOfTheRootEntries);
    }

    @Benchmark
    public Tuple2<HashMap<Integer, Integer>, HashMap<Integer, Integer>> hashMapPartitionAll() {
        return hashMap.partition(e -> true);
    }

    // the filter shares its rebuilding with the partition
    @Benchmark
    public HashMap<Integer, Integer> hashMapFilterHalf() {
        return hashMap.filter(halfEntries);
    }

    // -- HashMap.putAll of a HashMap

    @Benchmark
    public HashMap<Integer, Integer> hashMapPutAllRemoved() {
        return hashMap.putAll(hashMapRemoved);
    }

    @Benchmark
    public HashMap<Integer, Integer> hashMapPutAllFiltered() {
        return hashMap.putAll(hashMapFiltered);
    }

    @Benchmark
    public HashMap<Integer, Integer> hashMapPutAllAdded() {
        return hashMap.putAll(hashMapAdded);
    }

    @Benchmark
    public HashMap<Integer, Integer> hashMapPutAllDisjoint() {
        return hashMap.putAll(hashMapDisjoint);
    }

    @Benchmark
    public HashMap<Integer, Integer> hashMapPutAllCopy() {
        return hashMap.putAll(hashMapCopy);
    }

    // -- TreeMap.putAll of a TreeMap of the same comparator

    @Benchmark
    public TreeMap<Integer, Integer> treeMapPutAllReplaced() {
        return treeMap.putAll(treeMapReplaced);
    }

    @Benchmark
    public TreeMap<Integer, Integer> treeMapPutAllAdded() {
        return treeMap.putAll(treeMapAdded);
    }

    @Benchmark
    public TreeMap<Integer, Integer> treeMapPutAllRemoved() {
        return treeMap.putAll(treeMapRemoved);
    }

    @Benchmark
    public TreeMap<Integer, Integer> treeMapPutAllDisjoint() {
        return treeMap.putAll(treeMapDisjoint);
    }

    @Benchmark
    public TreeMap<Integer, Integer> treeMapPutAllCopy() {
        return treeMap.putAll(treeMapCopy);
    }
}
