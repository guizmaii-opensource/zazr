package com.guizmaii.zazr.test.laws;

import com.guizmaii.zazr.test.Arbitrary;
import com.guizmaii.zazr.test.Gen;

import java.util.Objects;
import java.util.Random;
import java.util.function.Function;

/**
 * The functions the laws quantify over. Each is drawn from a family by its random constants, is pure (the same
 * argument always gives the same result) and prints those constants, so a counterexample can be replayed.
 */
final class Functions {

    private Functions() {
    }

    /// Integer functions `x -> (a * x + b) mod m`: affine, and folding onto a small range when `m` is small, so that
    /// a set's `map` merges elements. Elements that are not integers go through their hash code.
    static Gen<Function<Object, Object>> integers() {
        return random -> new IntegerFunction(random.nextInt(21) - 10, random.nextInt(201) - 100,
                random.nextInt(3) == 0 ? 1 + random.nextInt(7) : 0);
    }

    /// Functions to values of `values`: the argument's hash code, mixed with a random seed, seeds the draw.
    static <F> Gen<Function<Object, F>> to(Arbitrary<F> values, int size) {
        final Gen<F> gen = values.apply(size);
        return random -> new SeededFunction<>(gen, random.nextLong());
    }

    /// The increment of SplitMix64.
    private static final long GOLDEN_GAMMA = 0x9E3779B97F4A7C15L;

    /// The SplitMix64 finaliser: every bit of the result depends on every bit of `z`, so adjacent arguments give
    /// unrelated seeds.
    static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    record IntegerFunction(int a, int b, int m) implements Function<Object, Object> {

        @Override
        public Object apply(Object x) {
            final int n = x instanceof Integer i ? i : Objects.hashCode(x);
            final int affine = a * n + b;
            return m == 0 ? affine : Math.floorMod(affine, m);
        }

        @Override
        public String toString() {
            return m == 0 ? "x -> " + a + " * x + " + b : "x -> (" + a + " * x + " + b + ") mod " + m;
        }
    }

    record SeededFunction<F>(Gen<F> gen, long seed) implements Function<Object, F> {

        @Override
        public F apply(Object x) {
            return gen.apply(new Random(mix(seed + GOLDEN_GAMMA * Objects.hashCode(x))));
        }

        @Override
        public String toString() {
            return "x -> draw(seed = " + seed + ", x)";
        }
    }
}
