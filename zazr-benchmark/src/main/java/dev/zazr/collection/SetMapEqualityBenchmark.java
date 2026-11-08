package dev.zazr.collection;

import dev.zazr.Tuple;
import dev.zazr.Tuple2;
import java.util.ArrayList;
import java.util.Comparator;
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
 * {@code equals} between two sets, or two maps, of different kinds or comparators, through the public API only, so
 * that the same class runs on two branches to compare them. Each pair is made of two collections built separately
 * from distinct {@code Integer} objects, so that no comparison stops at an identity; the unequal ones differ only in
 * the greatest element (key), replaced by one that is absent, at the same size. The maps map each key to itself, and
 * the unequal ones have the same keys and a different value for the greatest one.
 *
 * <p>The pairs: a {@code HashSet} against a {@code TreeSet} ({@code hashTree}); two {@code TreeSet}s, natural and
 * reversed ({@code treeTwoOrders}); two {@code TreeSet}s of comparators that are not equal but give the same order
 * ({@code treeOneOrder}); the same three for the maps ({@code hashMapTreeMap}, {@code treeMapTwoOrders},
 * {@code treeMapOneOrder}).
 *
 * <p>Run via {@code dev.zazr.JmhRunner}, or {@code org.openjdk.jmh.Main} for {@code -prof gc}.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Thread)
public class SetMapEqualityBenchmark {

    @Param({"1000", "100000"})
    public int size;

    @Param({"hashTree", "treeTwoOrders", "treeOneOrder", "hashMapTreeMap", "treeMapTwoOrders", "treeMapOneOrder"})
    public String pair;

    @Param({"true", "false"})
    public boolean equal;

    private Object left;
    private Object right;

    // the natural order of the integers, as a comparator that is not equal to the natural one
    private static final Comparator<Integer> BY_VALUE = Comparator.comparingInt(Integer::intValue);

    @Setup(Level.Trial)
    public void setup() {
        Random random = new Random(362);
        java.util.List<Integer> keys = new ArrayList<>();
        java.util.List<Integer> others = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            // above the Integer cache, so that two calls give two objects
            keys.add(Integer.valueOf(1000 + i));
            boolean last = !equal && i == size - 1;
            others.add(Integer.valueOf(last ? 1000 + 2 * size : 1000 + i));
        }
        java.util.Collections.shuffle(keys, random);
        java.util.Collections.shuffle(others, random);
        java.util.List<Tuple2<Integer, Integer>> entries = new ArrayList<>();
        java.util.List<Tuple2<Integer, Integer>> otherEntries = new ArrayList<>();
        for (Integer key : keys) {
            entries.add(Tuple.of(key, key));
        }
        for (Integer key : keys) {
            // the same keys, as other objects; the greatest one with another value when unequal
            int value = !equal && key == 1000 + size - 1 ? -1 : key;
            otherEntries.add(Tuple.of(Integer.valueOf(key.intValue()), Integer.valueOf(value)));
        }
        switch (pair) {
            case "hashTree" -> {
                left = HashSet.ofAll(keys);
                right = TreeSet.ofAll(others);
            }
            case "treeTwoOrders" -> {
                left = TreeSet.ofAll(keys);
                right = TreeSet.ofAll(Comparator.<Integer>reverseOrder(), others);
            }
            case "treeOneOrder" -> {
                left = TreeSet.ofAll(keys);
                right = TreeSet.ofAll(BY_VALUE, others);
            }
            case "hashMapTreeMap" -> {
                left = HashMap.ofEntries(entries);
                right = TreeMap.ofEntries(otherEntries);
            }
            case "treeMapTwoOrders" -> {
                left = TreeMap.ofEntries(entries);
                right = TreeMap.ofEntries(Comparator.<Integer>reverseOrder(), otherEntries);
            }
            case "treeMapOneOrder" -> {
                left = TreeMap.ofEntries(entries);
                right = TreeMap.ofEntries(BY_VALUE, otherEntries);
            }
            default -> throw new IllegalArgumentException(pair);
        }
        if (left.equals(right) != equal || right.equals(left) != equal) {
            throw new IllegalStateException(pair + ": equals is not " + equal);
        }
    }

    @Benchmark
    public boolean leftEqualsRight() {
        return left.equals(right);
    }

    @Benchmark
    public boolean rightEqualsLeft() {
        return right.equals(left);
    }
}
