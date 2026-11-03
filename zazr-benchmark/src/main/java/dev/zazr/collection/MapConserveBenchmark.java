package dev.zazr.collection;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;
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
 * `map` against `mapConserve` on a `List` and a `Vector` of `size` elements (`0, 1, ..., size - 1`), with a mapper
 * that changes no element (`unchanged`, the identity), every element (`all`, `x + 1`) or only the first one
 * (`first`). The `map` methods use only the API of `main`, so they run on both branches; `mapConserve` runs on the
 * branch that has it.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Thread)
public class MapConserveBenchmark {

    @Param({"10", "1000", "100000"})
    public int size;

    @Param({"unchanged", "all", "first"})
    public String change;

    private List<Integer> list;
    private Vector<Integer> vector;
    private Function<Integer, Integer> mapper;

    @Setup(Level.Trial)
    public void setup() {
        list = List.range(0, size);
        vector = Vector.range(0, size);
        Integer replacement = -1;
        mapper = switch (change) {
            case "unchanged" -> x -> x;
            case "all" -> x -> x + 1;
            case "first" -> x -> (x == 0) ? replacement : x;
            default -> throw new IllegalArgumentException(change);
        };
    }

    @Benchmark
    public List<Integer> listMap() {
        return list.map(mapper);
    }

    @Benchmark
    public List<Integer> listMapConserve() {
        return list.mapConserve(mapper);
    }

    @Benchmark
    public Vector<Integer> vectorMap() {
        return vector.map(mapper);
    }

    @Benchmark
    public Vector<Integer> vectorMapConserve() {
        return vector.mapConserve(mapper);
    }
}
