package com.guizmaii.zazr.collection;

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
import org.openjdk.jmh.infra.Blackhole;

/**
 * The single-key and the bulk operations of HashMap and HashSet: lookups that hit and miss, iteration, put, remove,
 * equals, union, merge, diff, filter, containsAll and mapValues, on random distinct keys. Only the public API is used,
 * so the same class runs on two branches to compare them.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Thread)
public class HashMapSetBenchmark {

    @Param({ "10", "1000", "100000" })
    public int size;

    private Integer[] present;
    private Integer[] absent;
    private HashMap<Integer, Integer> map;
    // equal to map, built in another order: a different object with the same content
    private HashMap<Integer, Integer> mapCopy;
    // half of the keys of map and as many others
    private HashMap<Integer, Integer> otherMap;
    private HashSet<Integer> set;
    private HashSet<Integer> setCopy;
    private HashSet<Integer> otherSet;
    // the first half of the elements of set
    private HashSet<Integer> halfSet;

    @Setup(Level.Trial)
    public void setup() {
        final Random random = new Random(42);
        final java.util.LinkedHashSet<Integer> keys = new java.util.LinkedHashSet<>();
        while (keys.size() < 2 * size) {
            keys.add(random.nextInt());
        }
        final ArrayList<Integer> all = new ArrayList<>(keys);
        present = all.subList(0, size).toArray(new Integer[0]);
        absent = all.subList(size, 2 * size).toArray(new Integer[0]);
        final ArrayList<Integer> presentList = new ArrayList<>(java.util.List.of(present));
        HashMap<Integer, Integer> m = HashMap.empty();
        HashSet<Integer> s = HashSet.empty();
        for (Integer key : presentList) {
            m = m.put(key, key);
            s = s.add(key);
        }
        map = m;
        set = s;
        Collections.shuffle(presentList, random);
        HashMap<Integer, Integer> mc = HashMap.empty();
        HashSet<Integer> sc = HashSet.empty();
        for (Integer key : presentList) {
            mc = mc.put(key, key);
            sc = sc.add(key);
        }
        mapCopy = mc;
        setCopy = sc;
        HashMap<Integer, Integer> om = HashMap.empty();
        HashSet<Integer> os = HashSet.empty();
        HashSet<Integer> hs = HashSet.empty();
        for (int i = 0; i < size; i++) {
            final Integer key = (i % 2 == 0) ? present[i] : absent[i];
            om = om.put(key, -key);
            os = os.add(key);
            if (i < size / 2) {
                hs = hs.add(present[i]);
            }
        }
        otherMap = om;
        otherSet = os;
        halfSet = hs;
    }

    // -- single keys

    @Benchmark
    public void mapGetHit(Blackhole bh) {
        for (Integer key : present) {
            bh.consume(map.getOrElse(key, null));
        }
    }

    @Benchmark
    public void mapGetMiss(Blackhole bh) {
        for (Integer key : absent) {
            bh.consume(map.getOrElse(key, null));
        }
    }

    @Benchmark
    public void setContainsHit(Blackhole bh) {
        for (Integer key : present) {
            bh.consume(set.contains(key));
        }
    }

    @Benchmark
    public void setContainsMiss(Blackhole bh) {
        for (Integer key : absent) {
            bh.consume(set.contains(key));
        }
    }

    @Benchmark
    public HashMap<Integer, Integer> mapPut() {
        HashMap<Integer, Integer> result = map;
        for (Integer key : absent) {
            result = result.put(key, key);
        }
        return result;
    }

    @Benchmark
    public HashMap<Integer, Integer> mapRemove() {
        HashMap<Integer, Integer> result = map;
        for (Integer key : present) {
            result = result.remove(key);
        }
        return result;
    }

    @Benchmark
    public HashSet<Integer> setAdd() {
        HashSet<Integer> result = set;
        for (Integer key : absent) {
            result = result.add(key);
        }
        return result;
    }

    @Benchmark
    public HashSet<Integer> setRemove() {
        HashSet<Integer> result = set;
        for (Integer key : present) {
            result = result.remove(key);
        }
        return result;
    }

    // -- walks

    @Benchmark
    public void mapIterate(Blackhole bh) {
        for (Tuple2<Integer, Integer> entry : map) {
            bh.consume(entry);
        }
    }

    @Benchmark
    public void setIterate(Blackhole bh) {
        for (Integer element : set) {
            bh.consume(element);
        }
    }

    @Benchmark
    public boolean mapEquals() {
        return map.equals(mapCopy);
    }

    @Benchmark
    public boolean setEquals() {
        return set.equals(setCopy);
    }

    @Benchmark
    public int setHashCode() {
        return set.hashCode();
    }

    // -- bulk operations

    @Benchmark
    public HashSet<Integer> setUnion() {
        return set.union(otherSet);
    }

    @Benchmark
    public HashMap<Integer, Integer> mapMerge() {
        return map.merge(otherMap);
    }

    @Benchmark
    public HashSet<Integer> setDiff() {
        return set.diff(otherSet);
    }

    @Benchmark
    public HashSet<Integer> setIntersect() {
        return set.intersect(otherSet);
    }

    @Benchmark
    public HashMap<Integer, Integer> mapRemoveAllOfASet() {
        return map.removeAll(otherSet);
    }

    @Benchmark
    public HashSet<Integer> setFilterHalf() {
        return set.filter(i -> (i & 1) == 0);
    }

    @Benchmark
    public HashMap<Integer, Integer> mapFilterHalf() {
        return map.filter((k, v) -> (k & 1) == 0);
    }

    @Benchmark
    public boolean setContainsAllOfASubset() {
        return set.containsAll(halfSet);
    }

    @Benchmark
    public HashMap<Integer, Integer> mapMapValues() {
        return map.mapValues(v -> v + 1);
    }
}
