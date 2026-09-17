package com.guizmaii.zazr.collection.euler;

import com.guizmaii.zazr.Function3;
import com.guizmaii.zazr.Tuple;
import com.guizmaii.zazr.Tuple3;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Test-only memoizing cache, standing in for the deleted {@code memoized()} (dropped per
 * {@code docs/design.md} section 3.1: the JDK has no per-argument memoizing function, so it is now the
 * caller's concern rather than the library's).
 */
final class Memoize {

    private Memoize() {
    }

    static <A, R> Function<A, R> of(Function<A, R> f) {
        final ConcurrentHashMap<A, R> cache = new ConcurrentHashMap<>();
        return a -> cache.computeIfAbsent(a, f);
    }

    // A HashMap + explicit get/put, not computeIfAbsent: a recursive function that calls back into its own
    // memoized wrapper (as Euler67's does) would otherwise trip computeIfAbsent's "recursive update" guard.
    static <A, B, C, R> Function3<A, B, C, R> of(Function3<A, B, C, R> f) {
        final Map<Tuple3<A, B, C>, R> cache = new HashMap<>();
        return (a, b, c) -> {
            final Tuple3<A, B, C> key = Tuple.of(a, b, c);
            final R cached = cache.get(key);
            if (cached != null || cache.containsKey(key)) {
                return cached;
            }
            final R value = f.apply(a, b, c);
            cache.put(key, value);
            return value;
        };
    }
}
