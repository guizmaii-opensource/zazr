package dev.zazr.avaje.jsonb.internal;

import dev.zazr.Tuple2;
import dev.zazr.collection.HashMap;
import dev.zazr.collection.LinkedHashMap;
import dev.zazr.collection.Map;
import dev.zazr.collection.NonEmptyMap;
import dev.zazr.collection.NonEmptySortedMap;
import dev.zazr.collection.SortedMap;
import dev.zazr.collection.TreeMap;
import java.util.ArrayList;
import org.jspecify.annotations.Nullable;

/// The Zazr maps, written as a JSON object, and how each is built from the entries read.
enum MapShape {
    HASH_MAP("HashMap", false, false) {
        @Override
        Object build(ArrayList<Tuple2<Object, Object>> entries) {
            return HashMap.newBuilder().putAll(entries).result();
        }
    },
    LINKED_HASH_MAP("LinkedHashMap", false, false) {
        @Override
        Object build(ArrayList<Tuple2<Object, Object>> entries) {
            return LinkedHashMap.newBuilder().putAll(entries).result();
        }
    },
    TREE_MAP("TreeMap", false, true) {
        @Override
        Object build(ArrayList<Tuple2<Object, Object>> entries) {
            return NaturalOrder.treeMap(entries);
        }
    },
    /// A property declared as the `Map` interface: written by iterating the map, whatever its class; read as a
    /// `HashMap`.
    MAP("Map", false, false) {
        @Override
        Object build(ArrayList<Tuple2<Object, Object>> entries) {
            return HashMap.newBuilder().putAll(entries).result();
        }
    },
    /// A property declared as the `SortedMap` interface: read as a `TreeMap` in the natural order of the keys.
    SORTED_MAP("SortedMap", false, true) {
        @Override
        Object build(ArrayList<Tuple2<Object, Object>> entries) {
            return NaturalOrder.treeMap(entries);
        }
    },
    NON_EMPTY_MAP("NonEmptyMap", true, false) {
        @Override
        Object build(ArrayList<Tuple2<Object, Object>> entries) {
            return HashMap.newBuilder().putAll(entries).result().toNonEmptyMap().get();
        }
    },
    NON_EMPTY_SORTED_MAP("NonEmptySortedMap", true, true) {
        @Override
        Object build(ArrayList<Tuple2<Object, Object>> entries) {
            return NaturalOrder.treeMap(entries).toNonEmptySortedMap().get();
        }
    };

    /// The simple name of the type, for the error messages.
    final String typeName;

    /// Whether an empty object is rejected.
    final boolean nonEmpty;

    /// Whether the keys are sorted in their natural order, and so must be `Comparable`.
    final boolean sorted;

    MapShape(String typeName, boolean nonEmpty, boolean sorted) {
        this.typeName = typeName;
        this.nonEmpty = nonEmpty;
        this.sorted = sorted;
    }

    /// The map of `entries`, in their order (sorted for a sorted map); of two entries with equal keys, the later
    /// wins. `entries` holds no `null` key or value, is not empty for a non-empty type, and holds only `Comparable`
    /// keys for a sorted one.
    abstract Object build(ArrayList<Tuple2<Object, Object>> entries);

    /// The shape of `rawType`, or `null` when it is not a Zazr map or one of the `Map` and `SortedMap` interfaces.
    static @Nullable MapShape of(Class<?> rawType) {
        if (rawType == HashMap.class) {
            return HASH_MAP;
        } else if (rawType == LinkedHashMap.class) {
            return LINKED_HASH_MAP;
        } else if (rawType == TreeMap.class) {
            return TREE_MAP;
        } else if (rawType == Map.class) {
            return MAP;
        } else if (rawType == SortedMap.class) {
            return SORTED_MAP;
        } else if (rawType == NonEmptyMap.class) {
            return NON_EMPTY_MAP;
        } else if (rawType == NonEmptySortedMap.class) {
            return NON_EMPTY_SORTED_MAP;
        }
        return null;
    }
}
