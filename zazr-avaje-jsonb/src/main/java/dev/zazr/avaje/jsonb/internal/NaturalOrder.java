package dev.zazr.avaje.jsonb.internal;

import dev.zazr.Tuple2;
import dev.zazr.collection.TreeMap;
import dev.zazr.collection.TreeSet;

/// `TreeSet` and `TreeMap` in the natural order, for elements and keys whose type is known only at run time. They are
/// built by Zazr's own natural-order builders, so they compare equal to, and combine with, the ones built with
/// `TreeSet.of` or `TreeMap.of`.
final class NaturalOrder {

    /// A type that stands for "some `Comparable` type" in the calls to the natural-order builders; never instantiated.
    private interface Natural extends Comparable<Natural> {}

    private NaturalOrder() {}

    /// The `TreeSet` of `elements` in their natural order.
    ///
    /// @throws ClassCastException when two elements are not comparable with each other
    @SuppressWarnings("unchecked")
    static TreeSet<Object> treeSet(Iterable<?> elements) {
        TreeSet.Builder<Object> builder = (TreeSet.Builder<Object>) (TreeSet.Builder<?>) TreeSet.<Natural>newBuilder();
        return builder.addAll(elements).result();
    }

    /// The `TreeMap` of `entries` in the natural order of their keys; of two entries with equal keys, the later wins.
    ///
    /// @throws ClassCastException when two keys are not comparable with each other
    @SuppressWarnings("unchecked")
    static TreeMap<Object, Object> treeMap(Iterable<Tuple2<Object, Object>> entries) {
        TreeMap.Builder<Object, Object> builder =
                (TreeMap.Builder<Object, Object>) (TreeMap.Builder<?, ?>) TreeMap.<Natural, Object>newBuilder();
        return builder.putAll(entries).result();
    }
}
