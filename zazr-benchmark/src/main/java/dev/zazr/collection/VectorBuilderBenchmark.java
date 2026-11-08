package dev.zazr.collection;

import java.util.ArrayList;
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
 * Bulk construction of a Vector from sources of known and unknown size, and the bulk transformations on it.
 * Run on the branch before and after the builder to compare; the benchmark only uses the public API.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Thread)
public class VectorBuilderBenchmark {

    @Param({"1000", "100000"})
    public int size;

    private ArrayList<Integer> list;
    private Vector<Integer> vector;
    private Vector<Integer> range;
    private Vector<Integer> large;
    private List<Integer> zazrList;
    private Queue<Integer> queue;
    private HashSet<Integer> hashSet;
    private LinkedHashSet<Integer> linkedHashSet;
    private TreeSet<Integer> treeSet;
    private NonEmptySet<Integer> nonEmptySet;

    @Setup(Level.Trial)
    public void setup() {
        list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(i);
        }
        vector = Vector.ofAll(list);
        range = Vector.range(0, size);
        large = Vector.range(0, 1_000_000);
        zazrList = List.ofAll(list);
        // half of the elements in the rear list
        queue = Queue.ofAll(list.subList(0, size / 2)).enqueueAll(list.subList(size / 2, size));
        hashSet = HashSet.ofAll(list);
        linkedHashSet = LinkedHashSet.ofAll(list);
        treeSet = TreeSet.ofAll(list);
        nonEmptySet = NonEmptySet.fromIterable(0, list);
    }

    @Benchmark
    public Vector<Integer> ofAll_arrayList() {
        return Vector.ofAll(list);
    }

    /* the Zazr collections, whose elements ofAll writes straight into the leaves */
    @Benchmark
    public Vector<Integer> ofAll_list() {
        return Vector.ofAll(zazrList);
    }

    @Benchmark
    public Vector<Integer> ofAll_queue() {
        return Vector.ofAll(queue);
    }

    @Benchmark
    public Vector<Integer> ofAll_hashSet() {
        return Vector.ofAll(hashSet);
    }

    @Benchmark
    public Vector<Integer> ofAll_linkedHashSet() {
        return Vector.ofAll(linkedHashSet);
    }

    @Benchmark
    public Vector<Integer> ofAll_treeSet() {
        return Vector.ofAll(treeSet);
    }

    @Benchmark
    public Vector<Integer> ofAll_nonEmptySet() {
        return Vector.ofAll(nonEmptySet);
    }

    @Benchmark
    public Vector<Integer> ofAll_iterator() {
        return Vector.ofAll((Iterable<Integer>) list::iterator);
    }

    @Benchmark
    public Vector<Integer> ofAll_stream() {
        return Vector.ofAll(list.stream());
    }

    @Benchmark
    public Vector<Integer> tabulate() {
        return Vector.tabulate(size, i -> i);
    }

    @Benchmark
    public Vector<Integer> fill() {
        return Vector.fill(size, 7);
    }

    @Benchmark
    public Vector<Integer> collector() {
        return list.stream().collect(Vector.collector());
    }

    @Benchmark
    public Vector<Integer> map() {
        return vector.map(i -> i + 1);
    }

    @Benchmark
    public Vector<Integer> filter() {
        return vector.filter(i -> (i & 1) == 0);
    }

    /* int[]-backed receivers */
    @Benchmark
    public Vector<Integer> map_range() {
        return range.map(i -> i + 1);
    }

    @Benchmark
    public Vector<Integer> filter_range() {
        return range.filter(i -> (i & 1) == 0);
    }

    @Benchmark
    public Vector<Integer> flatMap() {
        return vector.flatMap(i -> Vector.of(i, i));
    }

    /* independent of size: a three-element one-shot source appended to a million-element Vector */
    @Benchmark
    public Vector<Integer> appendAll_smallIteratorToLargeVector() {
        return large.appendAll(
                (Iterable<Integer>) () -> java.util.List.of(1, 2, 3).iterator());
    }

    @Benchmark
    public Vector<Integer> appendAll_iterator() {
        return vector.appendAll((Iterable<Integer>) list::iterator);
    }
}
