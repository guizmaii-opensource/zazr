package dev.zazr.avaje.jsonb;

import dev.zazr.avaje.jsonb.internal.ZazrAdapterFactory;
import io.avaje.jsonb.Jsonb;
import io.avaje.jsonb.spi.JsonbComponent;

/// Registers the JSON adapters of the Zazr types in avaje-jsonb.
///
/// avaje-jsonb loads this component as a service when it builds a `Jsonb` (`Jsonb.instance()` or
/// `Jsonb.builder().build()`), from `META-INF/services` on the class path and from the `provides` clause of the
/// `dev.zazr.avaje.jsonb` module on the module path. Adding the dependency is enough.
///
/// The JSON of each type:
///
/// - `Vector`, `List`, `Queue`, `LazyList`, `HashSet`, `LinkedHashSet`, `TreeSet`, `NonEmptyVector`, `NonEmptySet`
///   and `NonEmptySortedSet` are arrays, in their iteration order. A `LazyList` is written by iterating it, so it must
///   be finite. `TreeSet` and `NonEmptySortedSet` are read in the natural order of their elements, which must be
///   `Comparable`.
/// - `HashMap`, `LinkedHashMap`, `TreeMap`, `NonEmptyMap` and `NonEmptySortedMap` are objects. A key that is not a
///   `String` is written as the text of its JSON value (`1`, `2026-10-03`), and read back from it.
/// - `Option` is its value, or `null` for `None`. `None` is written as `null` whatever the `serializeNulls` setting,
///   so that reading it back gives `None`.
/// - `Tuple1` to `Tuple8` are arrays of exactly 1 to 8 elements.
///
/// Reading fails with a `JsonDataException` on a `null` element, key or value, on an empty array or object for a
/// non-empty type, and on an array of the wrong length for a tuple.
///
/// The service loader creates the component; [#register] can also be called on a builder directly:
/// `Jsonb.builder().add(new ZazrJsonbComponent())`.
public final class ZazrJsonbComponent implements JsonbComponent {

    /// Creates the component. avaje-jsonb's service loader calls it.
    public ZazrJsonbComponent() {}

    /// Adds the factory of the Zazr adapters to `builder`.
    ///
    /// @param builder the builder of a `Jsonb`
    @Override
    public void register(Jsonb.Builder builder) {
        builder.add(new ZazrAdapterFactory());
    }
}
