package dev.zazr.avaje.jsonb.internal;

import dev.zazr.collection.HashSet;
import dev.zazr.collection.LazyList;
import dev.zazr.collection.LinkedHashSet;
import dev.zazr.collection.List;
import dev.zazr.collection.NonEmptySet;
import dev.zazr.collection.NonEmptySortedSet;
import dev.zazr.collection.NonEmptyVector;
import dev.zazr.collection.Queue;
import dev.zazr.collection.TreeSet;
import dev.zazr.collection.Vector;
import java.util.ArrayList;
import org.jspecify.annotations.Nullable;

/// The Zazr types written as a JSON array of their elements, and how each is built from the elements read.
enum SequenceShape {
    VECTOR("Vector", false, false) {
        @Override
        Object build(ArrayList<?> elements) {
            return Vector.ofAll(elements);
        }
    },
    LIST("List", false, false) {
        @Override
        Object build(ArrayList<?> elements) {
            return List.ofAll(elements);
        }
    },
    QUEUE("Queue", false, false) {
        @Override
        Object build(ArrayList<?> elements) {
            return Queue.ofAll(elements);
        }
    },
    LAZY_LIST("LazyList", false, false) {
        @Override
        Object build(ArrayList<?> elements) {
            return LazyList.ofAll(elements);
        }
    },
    HASH_SET("HashSet", false, false) {
        @Override
        Object build(ArrayList<?> elements) {
            return HashSet.ofAll(elements);
        }
    },
    LINKED_HASH_SET("LinkedHashSet", false, false) {
        @Override
        Object build(ArrayList<?> elements) {
            return LinkedHashSet.ofAll(elements);
        }
    },
    TREE_SET("TreeSet", false, true) {
        @Override
        Object build(ArrayList<?> elements) {
            return NaturalOrder.treeSet(elements);
        }
    },
    NON_EMPTY_VECTOR("NonEmptyVector", true, false) {
        @Override
        Object build(ArrayList<?> elements) {
            return Vector.ofAll(elements).toNonEmptyVector().get();
        }
    },
    NON_EMPTY_SET("NonEmptySet", true, false) {
        @Override
        Object build(ArrayList<?> elements) {
            return HashSet.ofAll(elements).toNonEmptySet().get();
        }
    },
    NON_EMPTY_SORTED_SET("NonEmptySortedSet", true, true) {
        @Override
        Object build(ArrayList<?> elements) {
            return NaturalOrder.treeSet(elements).toNonEmptySortedSet().get();
        }
    };

    /// The simple name of the type, for the error messages.
    final String typeName;

    /// Whether an empty array is rejected.
    final boolean nonEmpty;

    /// Whether the elements are sorted in their natural order, and so must be `Comparable`.
    final boolean sorted;

    SequenceShape(String typeName, boolean nonEmpty, boolean sorted) {
        this.typeName = typeName;
        this.nonEmpty = nonEmpty;
        this.sorted = sorted;
    }

    /// The value of this type holding `elements`, in their order (sorted for a sorted set); `elements` holds no
    /// `null`, is not empty for a non-empty type, and holds only `Comparable` values for a sorted one.
    abstract Object build(ArrayList<?> elements);

    /// The shape of `rawType`, or `null` when it is not written as an array of elements. `List` and `LazyList` are
    /// interfaces: a class implementing them, the class of a value written with `Jsonb.toJson(Object)`, has their
    /// shape too.
    static @Nullable SequenceShape of(Class<?> rawType) {
        if (rawType == Vector.class) {
            return VECTOR;
        } else if (List.class.isAssignableFrom(rawType)) {
            return LIST;
        } else if (rawType == Queue.class) {
            return QUEUE;
        } else if (LazyList.class.isAssignableFrom(rawType)) {
            return LAZY_LIST;
        } else if (rawType == HashSet.class) {
            return HASH_SET;
        } else if (rawType == LinkedHashSet.class) {
            return LINKED_HASH_SET;
        } else if (rawType == TreeSet.class) {
            return TREE_SET;
        } else if (rawType == NonEmptyVector.class) {
            return NON_EMPTY_VECTOR;
        } else if (rawType == NonEmptySet.class) {
            return NON_EMPTY_SET;
        } else if (rawType == NonEmptySortedSet.class) {
            return NON_EMPTY_SORTED_SET;
        }
        return null;
    }
}
