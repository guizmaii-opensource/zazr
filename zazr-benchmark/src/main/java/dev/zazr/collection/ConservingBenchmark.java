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
 * Sequence operations that change nothing, or change one element of a long `List`, on sequences of `size` elements
 * (`0, 1, ..., size - 1`; the Queue enqueued one element at a time, so that most of it is at the back). Only the
 * public API is used, so the class runs on two branches.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Thread)
public class ConservingBenchmark {

    @Param({"10", "1000", "100000"})
    public int size;

    private List<Integer> list;
    private Vector<Integer> vector;
    private Queue<Integer> queue;
    private NonEmptyVector<Integer> nonEmptyVector;
    private Integer middle;

    @Setup(Level.Trial)
    public void setup() {
        list = List.range(0, size);
        vector = Vector.range(0, size);
        Queue<Integer> none = Queue.empty();
        queue = list.foldLeft(none, Queue::enqueue);
        nonEmptyVector = vector.toNonEmptyVector().get();
        middle = list.get(size / 2);
    }

    // -- List: no-ops

    @Benchmark
    public List<Integer> listFilterKeepingAll() {
        return list.filter(x -> true);
    }

    @Benchmark
    public List<Integer> listReplaceAllAbsent() {
        return list.replaceAll(-1, 0);
    }

    @Benchmark
    public Tuple2<List<Integer>, List<Integer>> listPartitionKeepingAll() {
        return list.partition(x -> true);
    }

    @Benchmark
    public Tuple2<List<Integer>, List<Integer>> listSpanOverAll() {
        return list.span(x -> true);
    }

    @Benchmark
    public Tuple2<List<Integer>, List<Integer>> listSplitAtTheEnd() {
        return list.splitAt(size);
    }

    @Benchmark
    public List<Integer> listUpdateWithTheSameElement() {
        return list.update(size / 2, middle);
    }

    @Benchmark
    public List<Integer> listTakeRightWhileAll() {
        return list.takeRightWhile(x -> true);
    }

    @Benchmark
    public List<Integer> listInsertAllNothing() {
        return list.insertAll(size / 2, List.empty());
    }

    // -- List: one element changed near the front, the rest shared

    @Benchmark
    public List<Integer> listFilterRemovingOneNearTheFront() {
        return list.filter(x -> x != 1);
    }

    @Benchmark
    public List<Integer> listReplaceAllOneNearTheFront() {
        return list.replaceAll(1, -1);
    }

    @Benchmark
    public List<Integer> listUpdateNearTheFront() {
        return list.update(1, -1);
    }

    // -- List: the changes that share least

    @Benchmark
    public List<Integer> listFilterRemovingTheLast() {
        return list.filter(x -> x != size - 1);
    }

    @Benchmark
    public List<Integer> listFilterKeepingHalf() {
        return list.filter(x -> x % 2 == 0);
    }

    @Benchmark
    public List<Integer> listReplaceAllTheLast() {
        return list.replaceAll(size - 1, -1);
    }

    @Benchmark
    public List<Integer> listUpdateNearTheEnd() {
        return list.update(size - 2, -1);
    }

    @Benchmark
    public Tuple2<List<Integer>, List<Integer>> listSpanUpToTheLast() {
        return list.span(x -> x < size - 1);
    }

    @Benchmark
    public Tuple2<List<Integer>, List<Integer>> listSplitAtTheMiddle() {
        return list.splitAt(size / 2);
    }

    // -- Vector

    @Benchmark
    public Tuple2<Vector<Integer>, Vector<Integer>> vectorPartitionKeepingAll() {
        return vector.partition(x -> true);
    }

    @Benchmark
    public Vector<Integer> vectorInsertAllNothing() {
        return vector.insertAll(size / 2, List.empty());
    }

    @Benchmark
    public Vector<Integer> vectorPatchWithNothing() {
        return vector.patch(size / 2, List.empty(), 0);
    }

    @Benchmark
    public Vector<Integer> vectorUpdateWithTheSameElement() {
        return vector.update(size / 2, middle);
    }

    @Benchmark
    public Vector<Integer> vectorUpdateChangingTheMiddle() {
        return vector.update(size / 2, -1);
    }

    // -- Queue

    @Benchmark
    public Queue<Integer> queueDistinctWithoutDuplicates() {
        return queue.distinct();
    }

    @Benchmark
    public Tuple2<Queue<Integer>, Queue<Integer>> queuePartitionKeepingAll() {
        return queue.partition(x -> true);
    }

    @Benchmark
    public Queue<Integer> queueSliceOfEverything() {
        return queue.slice(0, size);
    }

    @Benchmark
    public Queue<Integer> queueUpdateWithTheSameElement() {
        return queue.update(size / 2, middle);
    }

    // -- NonEmptyVector

    @Benchmark
    public NonEmptyVector<Integer> nonEmptyVectorPadToBelowTheSize() {
        return nonEmptyVector.padTo(1, 0);
    }

    @Benchmark
    public NonEmptyVector<Integer> nonEmptyVectorDistinctWithoutDuplicates() {
        return nonEmptyVector.distinct();
    }
}
