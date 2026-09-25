package com.guizmaii.zazr.test;

import com.guizmaii.zazr.Function3;
import com.guizmaii.zazr.Tuple;
import com.guizmaii.zazr.Tuple2;
import com.guizmaii.zazr.collection.HashMap;
import com.guizmaii.zazr.collection.HashSet;
import com.guizmaii.zazr.collection.LinkedHashMap;
import com.guizmaii.zazr.collection.LinkedHashSet;
import com.guizmaii.zazr.collection.List;
import com.guizmaii.zazr.collection.NonEmptyVector;
import com.guizmaii.zazr.collection.Queue;
import com.guizmaii.zazr.collection.Stream;
import com.guizmaii.zazr.collection.TreeMap;
import com.guizmaii.zazr.collection.TreeSet;
import com.guizmaii.zazr.collection.Vector;

import java.util.ArrayList;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The generators behind the collection arbitraries. Each one draws its elements into a {@link java.util.ArrayList}
 * first, so a defect in the collection under test cannot shape its own input, then builds the collection along one
 * of several construction paths, each reaching a different internal representation of the same elements.
 */
final class Shapes {

    /// The largest number of extra elements drawn to be dropped or removed again.
    private static final int MAX_EXTRA = 70;

    private Shapes() {
    }

    // -- element draws

    static <T> ArrayList<T> draw(Gen<T> gen, int count, Random random) {
        final ArrayList<T> elements = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            elements.add(gen.apply(random));
        }
        return elements;
    }

    static <T> Gen<ArrayList<T>> elements(Gen<T> gen, int size) {
        final Gen<Integer> lengths = Gen.choose(0, Math.max(0, size));
        return random -> draw(gen, lengths.apply(random), random);
    }

    /// Between 1 and {@link #MAX_EXTRA} elements, enough to push a 32-wide trie past its first leaf; none for a
    /// nonpositive size, which draws no element at all.
    static <T> ArrayList<T> extra(Gen<T> gen, int size, Random random) {
        return size <= 0 ? new ArrayList<>() : draw(gen, 1 + random.nextInt(MAX_EXTRA), random);
    }

    static <T> ArrayList<T> concat(java.util.List<T> first, java.util.List<T> second) {
        final ArrayList<T> all = new ArrayList<>(first.size() + second.size());
        all.addAll(first);
        all.addAll(second);
        return all;
    }

    // -- sequences

    /// `ofAll`, a builder, appends, prepends, a dropped prefix (the trie keeps an offset) and a slice.
    static <T> Gen<Vector<T>> vector(Gen<T> gen, int size) {
        final Gen<ArrayList<T>> elements = elements(gen, size);
        return random -> {
            final ArrayList<T> xs = elements.apply(random);
            return switch (random.nextInt(6)) {
                case 0 -> Vector.ofAll(xs);
                case 1 -> {
                    final Vector.Builder<T> builder = Vector.newBuilder();
                    xs.forEach(builder::add);
                    yield builder.result();
                }
                case 2 -> {
                    Vector<T> vector = Vector.empty();
                    for (T x : xs) {
                        vector = vector.append(x);
                    }
                    yield vector;
                }
                case 3 -> {
                    Vector<T> vector = Vector.empty();
                    for (int i = xs.size() - 1; i >= 0; i--) {
                        vector = vector.prepend(xs.get(i));
                    }
                    yield vector;
                }
                case 4 -> {
                    final ArrayList<T> prefix = extra(gen, size, random);
                    yield Vector.ofAll(concat(prefix, xs)).drop(prefix.size());
                }
                default -> {
                    final ArrayList<T> prefix = extra(gen, size, random);
                    final ArrayList<T> suffix = extra(gen, size, random);
                    yield Vector.ofAll(concat(concat(prefix, xs), suffix)).drop(prefix.size()).dropRight(suffix.size());
                }
            };
        };
    }

    /// `ofAll`, prepends, and the tail of a longer list.
    static <T> Gen<List<T>> list(Gen<T> gen, int size) {
        final Gen<ArrayList<T>> elements = elements(gen, size);
        return random -> {
            final ArrayList<T> xs = elements.apply(random);
            return switch (random.nextInt(3)) {
                case 0 -> List.ofAll(xs);
                case 1 -> {
                    List<T> list = List.empty();
                    for (int i = xs.size() - 1; i >= 0; i--) {
                        list = list.prepend(xs.get(i));
                    }
                    yield list;
                }
                default -> {
                    final ArrayList<T> prefix = extra(gen, size, random);
                    yield List.ofAll(concat(prefix, xs)).drop(prefix.size());
                }
            };
        };
    }

    /// Front list only (`ofAll`), one element in front and the rest in the rear list (enqueues), both lists
    /// holding several elements (`ofAll` then `enqueueAll`), and a rear list reversed into the front by `drop`.
    static <T> Gen<Queue<T>> queue(Gen<T> gen, int size) {
        final Gen<ArrayList<T>> elements = elements(gen, size);
        return random -> {
            final ArrayList<T> xs = elements.apply(random);
            return switch (random.nextInt(4)) {
                case 0 -> Queue.ofAll(xs);
                case 1 -> {
                    Queue<T> queue = Queue.empty();
                    for (T x : xs) {
                        queue = queue.enqueue(x);
                    }
                    yield queue;
                }
                case 2 -> {
                    final int split = xs.size() < 2 ? xs.size() : 1 + random.nextInt(xs.size() - 1);
                    yield Queue.ofAll(xs.subList(0, split)).enqueueAll(new ArrayList<>(xs.subList(split, xs.size())));
                }
                default -> {
                    final ArrayList<T> prefix = extra(gen, size, random);
                    yield Queue.ofAll(prefix).enqueueAll(xs).drop(prefix.size());
                }
            };
        };
    }

    /// `ofAll`, a chain of lazy tails, the same chain with a prefix already evaluated, an eager prefix with a
    /// lazy suffix appended, and the rest of a longer lazy chain after `drop`. Always finite.
    static <T> Gen<Stream<T>> stream(Gen<T> gen, int size) {
        final Gen<ArrayList<T>> elements = elements(gen, size);
        return random -> {
            final ArrayList<T> xs = elements.apply(random);
            return switch (random.nextInt(5)) {
                case 0 -> Stream.ofAll(xs);
                case 1 -> lazyStream(xs, 0);
                case 2 -> {
                    final Stream<T> stream = lazyStream(xs, 0);
                    final int evaluated = random.nextInt(xs.size() + 1);
                    Stream<T> cursor = stream;
                    for (int i = 0; i < evaluated && !cursor.isEmpty(); i++) {
                        cursor = cursor.tail();
                    }
                    yield stream;
                }
                case 3 -> {
                    final int split = random.nextInt(xs.size() + 1);
                    yield Stream.ofAll(xs.subList(0, split)).appendAll(lazyStream(xs, split));
                }
                default -> {
                    final ArrayList<T> prefix = extra(gen, size, random);
                    yield lazyStream(concat(prefix, xs), 0).drop(prefix.size());
                }
            };
        };
    }

    private static <T> Stream<T> lazyStream(java.util.List<T> xs, int from) {
        return from == xs.size() ? Stream.empty() : Stream.cons(xs.get(from), () -> lazyStream(xs, from + 1));
    }

    /// The first element followed by the {@link #vector(Gen, int)} shapes, through `of`, `fromVector` and `prepend`.
    static <T> Gen<NonEmptyVector<T>> nonEmptyVector(Gen<T> gen, int size) {
        final Gen<Vector<T>> tails = vector(gen, Math.max(0, size - 1));
        return random -> {
            final T head = gen.apply(random);
            final Vector<T> tail = tails.apply(random);
            return switch (random.nextInt(3)) {
                case 0 -> NonEmptyVector.single(head).appendAll(tail);
                case 1 -> NonEmptyVector.fromVector(tail.prepend(head)).get();
                default -> tail.isEmpty()
                        ? NonEmptyVector.single(head)
                        : NonEmptyVector.fromVector(tail).get().prepend(head);
            };
        };
    }

    // -- sets

    /// The operations one set type offers to the set shapes.
    record SetOps<T, S>(Supplier<S> empty, Function<Iterable<T>, S> ofAll, BiFunction<S, T, S> add,
                        BiFunction<S, T, S> remove, BiPredicate<S, T> contains) {
    }

    static <T> SetOps<T, HashSet<T>> hashSetOps() {
        return new SetOps<T, HashSet<T>>(HashSet::empty, HashSet::ofAll, HashSet::add, HashSet::remove, HashSet::contains);
    }

    static <T> SetOps<T, LinkedHashSet<T>> linkedHashSetOps() {
        return new SetOps<T, LinkedHashSet<T>>(LinkedHashSet::empty, LinkedHashSet::ofAll, LinkedHashSet::add,
                LinkedHashSet::remove, LinkedHashSet::contains);
    }

    static <T extends Comparable<? super T>> SetOps<T, TreeSet<T>> treeSetOps() {
        return new SetOps<T, TreeSet<T>>(TreeSet::empty, TreeSet::ofAll, TreeSet::add, TreeSet::remove, TreeSet::contains);
    }

    /// `ofAll`, one `add` at a time, extra elements added then removed again, and elements removed then added back.
    static <T, S> Gen<S> set(Gen<T> gen, int size, SetOps<T, S> ops) {
        final Gen<ArrayList<T>> elements = elements(gen, size);
        return random -> {
            final ArrayList<T> xs = elements.apply(random);
            return switch (random.nextInt(4)) {
                case 0 -> ops.ofAll().apply(xs);
                case 1 -> {
                    S set = ops.empty().get();
                    for (T x : xs) {
                        set = ops.add().apply(set, x);
                    }
                    yield set;
                }
                case 2 -> {
                    final S base = ops.ofAll().apply(xs);
                    final ArrayList<T> extra = extra(gen, size, random);
                    S set = base;
                    for (T x : extra) {
                        set = ops.add().apply(set, x);
                    }
                    for (T x : extra) {
                        if (!ops.contains().test(base, x)) {
                            set = ops.remove().apply(set, x);
                        }
                    }
                    yield set;
                }
                default -> {
                    S set = ops.ofAll().apply(xs);
                    final int removed = random.nextInt(xs.size() + 1);
                    for (int i = 0; i < removed; i++) {
                        set = ops.remove().apply(set, xs.get(i));
                    }
                    for (int i = 0; i < removed; i++) {
                        set = ops.add().apply(set, xs.get(i));
                    }
                    yield set;
                }
            };
        };
    }

    // -- maps

    /// The operations one map type offers to the map shapes.
    record MapOps<K, V, M>(Supplier<M> empty, Function<java.util.Map<K, V>, M> ofAll, Function3<M, K, V, M> put,
                           BiFunction<M, K, M> remove, BiPredicate<M, K> containsKey) {
    }

    static <K, V> MapOps<K, V, HashMap<K, V>> hashMapOps() {
        return new MapOps<K, V, HashMap<K, V>>(HashMap::empty, HashMap::ofAll, HashMap::put, HashMap::remove,
                HashMap::containsKey);
    }

    static <K, V> MapOps<K, V, LinkedHashMap<K, V>> linkedHashMapOps() {
        return new MapOps<K, V, LinkedHashMap<K, V>>(LinkedHashMap::empty, LinkedHashMap::ofAll, LinkedHashMap::put,
                LinkedHashMap::remove, LinkedHashMap::containsKey);
    }

    static <K extends Comparable<? super K>, V> MapOps<K, V, TreeMap<K, V>> treeMapOps() {
        return new MapOps<K, V, TreeMap<K, V>>(TreeMap::empty, TreeMap::ofAll, TreeMap::put, TreeMap::remove,
                TreeMap::containsKey);
    }

    /// `ofAll` of a JDK map, one `put` at a time, extra keys put then removed again, and every key first put with
    /// another value then overwritten.
    static <K, V, M> Gen<M> map(Gen<K> keys, Gen<V> values, int size, MapOps<K, V, M> ops) {
        final Gen<Tuple2<K, V>> entries = random -> Tuple.of(keys.apply(random), values.apply(random));
        final Gen<ArrayList<Tuple2<K, V>>> elements = elements(entries, size);
        return random -> {
            final ArrayList<Tuple2<K, V>> xs = elements.apply(random);
            return switch (random.nextInt(4)) {
                case 0 -> {
                    final java.util.LinkedHashMap<K, V> javaMap = new java.util.LinkedHashMap<>();
                    xs.forEach(entry -> javaMap.put(entry._1(), entry._2()));
                    yield ops.ofAll().apply(javaMap);
                }
                case 1 -> putAll(ops.empty().get(), xs, ops);
                case 2 -> {
                    final M base = putAll(ops.empty().get(), xs, ops);
                    final ArrayList<Tuple2<K, V>> extra = extra(entries, size, random);
                    M map = putAll(base, extra, ops);
                    for (Tuple2<K, V> entry : extra) {
                        if (!ops.containsKey().test(base, entry._1())) {
                            map = ops.remove().apply(map, entry._1());
                        }
                    }
                    yield map;
                }
                default -> {
                    M map = ops.empty().get();
                    for (Tuple2<K, V> entry : xs) {
                        map = ops.put().apply(map, entry._1(), values.apply(random));
                    }
                    yield putAll(map, xs, ops);
                }
            };
        };
    }

    private static <K, V, M> M putAll(M map, java.util.List<Tuple2<K, V>> entries, MapOps<K, V, M> ops) {
        M result = map;
        for (Tuple2<K, V> entry : entries) {
            result = ops.put().apply(result, entry._1(), entry._2());
        }
        return result;
    }
}
