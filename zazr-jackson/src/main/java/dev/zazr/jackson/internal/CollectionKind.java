package dev.zazr.jackson.internal;

import dev.zazr.collection.HashSet;
import dev.zazr.collection.LazyList;
import dev.zazr.collection.LinkedHashSet;
import dev.zazr.collection.List;
import dev.zazr.collection.NonEmptySet;
import dev.zazr.collection.NonEmptySortedSet;
import dev.zazr.collection.NonEmptyVector;
import dev.zazr.collection.Queue;
import dev.zazr.collection.Set;
import dev.zazr.collection.SortedSet;
import dev.zazr.collection.TreeSet;
import dev.zazr.collection.Vector;
import org.jspecify.annotations.Nullable;

/// The Zazr collection a JSON array is read into, chosen by the declared class, and how it is built from the
/// elements read, in their JSON order. `Set` reads as a `HashSet` and `SortedSet` as a `TreeSet`.
enum CollectionKind {
    VECTOR("Vector"),
    LIST("List"),
    QUEUE("Queue"),
    LAZY_LIST("LazyList"),
    HASH_SET("HashSet"),
    LINKED_HASH_SET("LinkedHashSet"),
    TREE_SET("TreeSet"),
    NON_EMPTY_VECTOR("NonEmptyVector"),
    NON_EMPTY_SET("NonEmptySet"),
    NON_EMPTY_SORTED_SET("NonEmptySortedSet");

    private final String typeName;

    CollectionKind(String typeName) {
        this.typeName = typeName;
    }

    /// The kind read for the declared class `raw`, or `null` when `raw` is not a collection this module reads.
    /// A subclass of `List` (`List.Cons`, `List.Nil`) or of `LazyList` (its implementation classes) reads as
    /// that type, since a type id may name one.
    static @Nullable CollectionKind of(Class<?> raw) {
        if (raw == Vector.class) {
            return VECTOR;
        } else if (List.class.isAssignableFrom(raw)) {
            return LIST;
        } else if (raw == Queue.class) {
            return QUEUE;
        } else if (LazyList.class.isAssignableFrom(raw)) {
            return LAZY_LIST;
        } else if (raw == HashSet.class || raw == Set.class) {
            return HASH_SET;
        } else if (raw == LinkedHashSet.class) {
            return LINKED_HASH_SET;
        } else if (raw == TreeSet.class || raw == SortedSet.class) {
            return TREE_SET;
        } else if (raw == NonEmptyVector.class) {
            return NON_EMPTY_VECTOR;
        } else if (raw == NonEmptySet.class) {
            return NON_EMPTY_SET;
        } else if (raw == NonEmptySortedSet.class) {
            return NON_EMPTY_SORTED_SET;
        } else {
            return null;
        }
    }

    /// The name of the type built, for the error messages.
    String typeName() {
        return typeName;
    }

    /// Whether the elements are kept in their natural order, so their type must be `Comparable`.
    boolean sorted() {
        return this == TREE_SET || this == NON_EMPTY_SORTED_SET;
    }

    /// Whether the collection needs at least one element.
    boolean nonEmpty() {
        return this == NON_EMPTY_VECTOR || this == NON_EMPTY_SET || this == NON_EMPTY_SORTED_SET;
    }

    /// The collection of `elements`, none of them `null`; for a non-empty kind, there is at least one.
    Object build(java.util.List<Object> elements) {
        return switch (this) {
            case VECTOR -> Vector.ofAll(elements);
            case LIST -> List.ofAll(elements);
            case QUEUE -> Queue.ofAll(elements);
            case LAZY_LIST -> LazyList.ofAll(elements);
            case HASH_SET -> HashSet.ofAll(elements);
            case LINKED_HASH_SET -> LinkedHashSet.ofAll(elements);
            case TREE_SET -> TreeSet.ofAll(natural(elements));
            case NON_EMPTY_VECTOR -> NonEmptyVector.fromIterable(elements.getFirst(), tail(elements));
            case NON_EMPTY_SET -> NonEmptySet.fromIterable(elements.getFirst(), tail(elements));
            case NON_EMPTY_SORTED_SET -> {
                var natural = natural(elements);
                yield NonEmptySortedSet.fromIterable(natural.getFirst(), tail(natural));
            }
        };
    }

    private static <T> java.util.List<T> tail(java.util.List<T> elements) {
        return elements.subList(1, elements.size());
    }

    /// The elements, typed for the natural-order factories. The element type was checked to be `Comparable` before
    /// any element was read, and every element read has that type; the factories then use the same comparator as
    /// `TreeSet.empty()`.
    @SuppressWarnings("unchecked")
    private static java.util.List<Comparable<Object>> natural(java.util.List<Object> elements) {
        return (java.util.List<Comparable<Object>>) (java.util.List<?>) elements;
    }
}
