/* ____  ______________  ________________________  __________
 * \   \/   /      \   \/   /   __/   /      \   \/   /      \
 *  \______/___/\___\______/___/_____/___/\___\______/___/\___\
 *
 * Copyright 2014-2026 Vavr, https://vavr.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.vavr.collection;

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

    @Param({ "1000", "100000" })
    public int size;

    private ArrayList<Integer> list;
    private Vector<Integer> vector;

    @Setup(Level.Trial)
    public void setup() {
        list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(i);
        }
        vector = Vector.ofAll(list);
    }

    @Benchmark
    public Vector<Integer> ofAll_arrayList() {
        return Vector.ofAll(list);
    }

    @Benchmark
    public Vector<Integer> ofAll_iterator() {
        return Vector.ofAll(Iterator.ofAll(list.iterator()));
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

    @Benchmark
    public Vector<Integer> flatMap() {
        return vector.flatMap(i -> Vector.of(i, i));
    }

    @Benchmark
    public Vector<Integer> appendAll_iterator() {
        return vector.appendAll(Iterator.ofAll(list.iterator()));
    }
}
