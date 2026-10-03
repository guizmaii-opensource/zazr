---
description: Read and write the Zazr collections, Option and the tuples as JSON with Jackson 3, through the zazr-jackson module.
---

# Jackson

`dev.zazr:zazr-jackson` is a Jackson 3 module. Once it is registered, Jackson reads and writes the Zazr collections,
`Option` and the tuples, and so the records and classes that hold them.

## Add the dependency

`zazr-jackson` ships from Zazr 0.3.0. It depends on `zazr-core` and on Jackson 3
(`tools.jackson.core:jackson-databind`), which your project already has if it uses Jackson 3.

=== "Maven"

    ```xml
    <dependency>
        <groupId>dev.zazr</groupId>
        <artifactId>zazr-jackson</artifactId>
        <version>0.3.0</version>
    </dependency>
    ```

=== "Gradle (Kotlin)"

    ```kotlin
    dependencies {
        implementation("dev.zazr:zazr-jackson:0.3.0")
    }
    ```

If your project uses Java modules, add `requires dev.zazr.jackson;` to its `module-info.java`.

## Register the module

Add `ZazrModule` when you build the mapper:

```java
var mapper = JsonMapper.builder().addModule(new ZazrModule()).build(); // JsonMapper
```

`findAndAddModules()` on the builder finds it too, as it finds every Jackson module on the class path or module path.

A record that holds Zazr types then reads and writes like any other:

```java
record Line(String sku, int quantity) {}

record Order(Vector<Line> lines, Option<String> note) {}

var order = new Order(Vector.of(new Line("A-1", 2)), Option.none());
var json  = mapper.writeValueAsString(order);
var back  = mapper.readValue(json, Order.class); // Order
// json is {"lines":[{"sku":"A-1","quantity":2}],"note":null}, back equals order
```

## The JSON format

| Zazr type | JSON | Example |
|---|---|---|
| `Vector`, `List`, `Queue`, `LazyList` | array | `[1,2,3]` |
| `HashSet`, `LinkedHashSet`, `TreeSet` | array | `["a","b"]` |
| `NonEmptyVector`, `NonEmptySet`, `NonEmptySortedSet` | array of at least one element | `[1]` |
| `HashMap`, `LinkedHashMap`, `TreeMap` | object | `{"a":1}` |
| `NonEmptyMap`, `NonEmptySortedMap` | object of at least one entry | `{"a":1}` |
| `Option` | the value, or `null` for `None` | `"x"`, `null` |
| `Tuple1` to `Tuple8` | array of 1 to 8 elements | `[1,"a"]` |

Map keys go through Jackson's key serializers and deserializers, so a key can be a `String`, a number, an enum, a
`UUID`, a `java.time` value, or any type Jackson has a key deserializer for.

## Reading

Jackson reads each element, key and value with the type the declaration gives it, at any depth:

```java
var byDate = new TypeReference<HashMap<LocalDate, Vector<String>>>() {};
var dates  = mapper.readValue("{\"2026-10-03\":[\"a\"]}", byDate);
var counts = mapper.readValue("[1,null,3]", new TypeReference<Vector<Option<Integer>>>() {});
// dates is HashMap((2026-10-03, Vector(a))), counts is Vector(Some(1), None, Some(3))
```

A property declared as one of the interfaces reads into a concrete type:

| Declared as | Reads as |
|---|---|
| `Set` | `HashSet` |
| `SortedSet` | `TreeSet` |
| `Map` | `HashMap` |
| `SortedMap` | `TreeMap` |

To read a property declared as `Traversable`, name the type with `@JsonDeserialize(as = Vector.class)`.

### Order

- `TreeSet`, `TreeMap`, `NonEmptySortedSet` and `NonEmptySortedMap` use the natural order: their element or key type
  must implement `Comparable`. If it does not, reading fails with a message that names the type.
- `LinkedHashSet` and `LinkedHashMap` keep the order of the JSON.
- `HashSet` and `HashMap` have no order; written to JSON, they follow their iteration order.

### `Option`

- `None` is written as `null`, and `null` reads as `None`.
- A record component or a constructor parameter that is absent from the JSON reads as `None`.
- A field or a setter property that is absent keeps its initial value: initialise it with `Option.none()`.
- `@JsonInclude(JsonInclude.Include.NON_ABSENT)` on a property leaves it out of the JSON when it is `None`.

## What fails

Reading fails, with a message that says what and where, on:

- a `null` element, key or value of a collection, unless its type reads `null` as a value, as `Option` does;
- an empty array or object for `NonEmptyVector`, `NonEmptySet`, `NonEmptySortedSet`, `NonEmptyMap` and
  `NonEmptySortedMap`;
- an array whose length differs from the size of the tuple it is read into.

```java
var numbers = new TypeReference<Vector<Integer>>() {};
var failure = Try.of(() -> mapper.readValue("[1,null]", numbers)).getCause(); // Throwable
// failure.getMessage() starts with "Element 1 of the Vector is null: Zazr collections hold no null."
```

A tuple holds `null` components, as `Tuple.of(1, null)` does: `[1,null]` reads as that tuple.

A collection property that is `null` in the JSON reads as `null`, as a `java.util` collection does. With
`@JsonSetter(nulls = Nulls.AS_EMPTY)` on the property, it reads as the empty collection.

## Writing

- A value declared as `Object` or as an interface is written by its runtime type.
- A `LazyList` is written by iterating it, so it must be finite.

## Not covered

- `Either`, `Try`, `Validation`, `Lazy` and `Tuple0` have no JSON format in this module. Map them to a covered type
  first, for example with `toOption()`.
- The module works with Jackson 3 (`tools.jackson`), the version Spring Boot 4 uses. Jackson 2
  (`com.fasterxml.jackson`) is out of its scope.
