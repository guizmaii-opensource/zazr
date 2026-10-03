package dev.zazr.jackson;

import dev.zazr.jackson.internal.ZazrDeserializers;
import dev.zazr.jackson.internal.ZazrSerializers;
import dev.zazr.jackson.internal.ZazrTypeModifier;
import tools.jackson.core.Version;
import tools.jackson.databind.JacksonModule;

/// The Jackson 3 module that reads and writes the Zazr types as JSON.
///
/// Register it once, on the mapper builder:
///
/// ```java
/// var mapper = JsonMapper.builder().addModule(new ZazrModule()).build();
/// ```
///
/// `findAndAddModules()` on the builder finds it too, through `java.util.ServiceLoader`.
///
/// | Zazr type | JSON |
/// |---|---|
/// | `Vector`, `List`, `Queue`, `LazyList` | array |
/// | `HashSet`, `LinkedHashSet`, `TreeSet` | array |
/// | `NonEmptyVector`, `NonEmptySet`, `NonEmptySortedSet` | array of at least one element |
/// | `HashMap`, `LinkedHashMap`, `TreeMap` | object |
/// | `NonEmptyMap`, `NonEmptySortedMap` | object of at least one entry |
/// | `Option` | the value, or `null` for `None` |
/// | `Tuple1` to `Tuple8` | array of 1 to 8 elements |
///
/// The keys of a map go through Jackson's key serializers and key deserializers, so any key type Jackson can write
/// as a JSON property name works: `String`, numbers, enums, `java.time` types, `UUID`.
///
/// Reading:
///
/// - A property declared as `Set`, `SortedSet`, `Map` or `SortedMap` reads into a `HashSet`, `TreeSet`, `HashMap` or
///   `TreeMap`.
/// - `TreeSet`, `TreeMap` and the sorted non-empty types use the natural order: their element or key type must
///   implement `Comparable`.
/// - `LinkedHashSet` and `LinkedHashMap` keep the order of the JSON.
/// - `null` reads as `None` into an `Option`, and so does an absent creator property (a record component, a
///   constructor parameter).
/// - A `null` element, key or value of a collection fails, unless its type reads `null` as a value, as `Option` does.
///   A tuple holds a `null` component.
/// - An empty array or object fails for the non-empty types, and an array of the wrong length for a tuple.
///
/// Writing a `LazyList` iterates it: it must be finite.
public final class ZazrModule extends JacksonModule {

    /// A module to register on a mapper builder.
    public ZazrModule() {}

    @Override
    public String getModuleName() {
        return "ZazrModule";
    }

    @Override
    public Version version() {
        return Version.unknownVersion();
    }

    @Override
    public void setupModule(SetupContext context) {
        context.addTypeModifier(new ZazrTypeModifier());
        context.addSerializers(new ZazrSerializers());
        context.addDeserializers(new ZazrDeserializers());
    }
}
