package dev.zazr.jackson.internal;

import dev.zazr.Tuple1;
import dev.zazr.Tuple2;
import dev.zazr.Tuple3;
import dev.zazr.Tuple4;
import dev.zazr.Tuple5;
import dev.zazr.Tuple6;
import dev.zazr.Tuple7;
import dev.zazr.Tuple8;
import dev.zazr.collection.LazyList;
import dev.zazr.collection.List;
import dev.zazr.collection.Map;
import dev.zazr.collection.NonEmptyMap;
import dev.zazr.collection.NonEmptySet;
import dev.zazr.collection.NonEmptySortedMap;
import dev.zazr.collection.NonEmptySortedSet;
import dev.zazr.collection.NonEmptyVector;
import dev.zazr.collection.Traversable;

/// The Zazr types the module handles, recognised by their class: the runtime class of a value being written (a
/// `List.Cons`, an `Option.Some`, a `LazyList` implementation) or the declared class of a value being read.
final class ZazrTypes {

    private ZazrTypes() {}

    /// A Zazr map: written as a JSON object.
    static boolean isMap(Class<?> raw) {
        return Map.class.isAssignableFrom(raw) || raw == NonEmptyMap.class || raw == NonEmptySortedMap.class;
    }

    /// The supertype whose two type parameters are the key and value types of the map class `raw`.
    static Class<?> mapSupertype(Class<?> raw) {
        return Map.class.isAssignableFrom(raw) ? Map.class : raw;
    }

    /// A Zazr collection other than a map: written as a JSON array. A map is a `Traversable` of its entries, so it is
    /// excluded first.
    static boolean isCollection(Class<?> raw) {
        return !isMap(raw)
                && (Traversable.class.isAssignableFrom(raw)
                        || raw == NonEmptyVector.class
                        || raw == NonEmptySet.class
                        || raw == NonEmptySortedSet.class);
    }

    /// The class to write as the type id of a value of the runtime class `raw`: the public type for the classes of
    /// `List` (`List.Cons`, `List.Nil`) and of `LazyList` (classes of `dev.zazr.collection.internal`), `raw` itself
    /// for the other collections, which are final public classes.
    static Class<?> idType(Class<?> raw) {
        if (List.class.isAssignableFrom(raw)) {
            return List.class;
        } else if (LazyList.class.isAssignableFrom(raw)) {
            return LazyList.class;
        } else {
            return raw;
        }
    }

    /// The number of components of the tuple class `raw`, from 1 to 8, or 0 when `raw` is not one of `Tuple1` to
    /// `Tuple8`.
    static int tupleArity(Class<?> raw) {
        if (raw == Tuple1.class) {
            return 1;
        } else if (raw == Tuple2.class) {
            return 2;
        } else if (raw == Tuple3.class) {
            return 3;
        } else if (raw == Tuple4.class) {
            return 4;
        } else if (raw == Tuple5.class) {
            return 5;
        } else if (raw == Tuple6.class) {
            return 6;
        } else if (raw == Tuple7.class) {
            return 7;
        } else if (raw == Tuple8.class) {
            return 8;
        } else {
            return 0;
        }
    }
}
