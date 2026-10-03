package dev.zazr.jackson.internal;

import dev.zazr.Tuple2;
import dev.zazr.collection.HashMap;
import dev.zazr.collection.LinkedHashMap;
import dev.zazr.collection.Map;
import dev.zazr.collection.NonEmptyMap;
import dev.zazr.collection.NonEmptySortedMap;
import dev.zazr.collection.SortedMap;
import dev.zazr.collection.TreeMap;
import org.jspecify.annotations.Nullable;

/// The Zazr map a JSON object is read into, chosen by the declared class, and how it is built from the entries read,
/// in their JSON order. `Map` reads as a `HashMap` and `SortedMap` as a `TreeMap`.
enum MapKind {
    HASH_MAP("HashMap"),
    LINKED_HASH_MAP("LinkedHashMap"),
    TREE_MAP("TreeMap"),
    NON_EMPTY_MAP("NonEmptyMap"),
    NON_EMPTY_SORTED_MAP("NonEmptySortedMap");

    private final String typeName;

    MapKind(String typeName) {
        this.typeName = typeName;
    }

    /// The kind read for the declared class `raw`, or `null` when `raw` is not a map this module reads.
    static @Nullable MapKind of(Class<?> raw) {
        if (raw == HashMap.class || raw == Map.class) {
            return HASH_MAP;
        } else if (raw == LinkedHashMap.class) {
            return LINKED_HASH_MAP;
        } else if (raw == TreeMap.class || raw == SortedMap.class) {
            return TREE_MAP;
        } else if (raw == NonEmptyMap.class) {
            return NON_EMPTY_MAP;
        } else if (raw == NonEmptySortedMap.class) {
            return NON_EMPTY_SORTED_MAP;
        } else {
            return null;
        }
    }

    /// The name of the type built, for the error messages.
    String typeName() {
        return typeName;
    }

    /// Whether the keys are kept in their natural order, so their type must be `Comparable`.
    boolean sorted() {
        return this == TREE_MAP || this == NON_EMPTY_SORTED_MAP;
    }

    /// Whether the map needs at least one entry.
    boolean nonEmpty() {
        return this == NON_EMPTY_MAP || this == NON_EMPTY_SORTED_MAP;
    }

    /// The map of `entries`, in their JSON order, no key or value `null`; a later entry replaces an earlier one with
    /// the same key. For a non-empty kind, there is at least one entry.
    Object build(java.util.List<Tuple2<Object, Object>> entries) {
        return switch (this) {
            case HASH_MAP -> HashMap.ofEntries(entries);
            case LINKED_HASH_MAP -> LinkedHashMap.ofEntries(entries);
            case TREE_MAP -> TreeMap.ofEntries(natural(entries));
            case NON_EMPTY_MAP -> NonEmptyMap.fromIterable(entries.getFirst(), tail(entries));
            case NON_EMPTY_SORTED_MAP -> {
                var natural = natural(entries);
                yield NonEmptySortedMap.fromIterable(natural.getFirst(), tail(natural));
            }
        };
    }

    private static <T> java.util.List<T> tail(java.util.List<T> entries) {
        return entries.subList(1, entries.size());
    }

    /// The entries, typed for the natural-order factories. The key type was checked to be `Comparable` before any
    /// entry was read; the factories then use the same comparator as `TreeMap.empty()`.
    @SuppressWarnings("unchecked")
    private static java.util.List<Tuple2<Comparable<Object>, Object>> natural(
            java.util.List<Tuple2<Object, Object>> entries) {
        return (java.util.List<Tuple2<Comparable<Object>, Object>>) (java.util.List<?>) entries;
    }
}
