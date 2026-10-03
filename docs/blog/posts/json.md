---
date: 2026-10-04
authors:
  - guizmaii
categories:
  - JSON
slug: json
description: "Zazr 0.3.0 reads and writes Zazr types as JSON with Jackson or avaje-jsonb: one dependency, one JSON format, and the guarantees of the types kept at the boundary."
---

# Zazr types in JSON, with Jackson and avaje-jsonb

Zazr 0.3.0 comes with two new modules: `zazr-jackson` for Jackson 3, and `zazr-avaje-jsonb` for avaje-jsonb. With
either of them, a record that holds Zazr collections, `Option` or tuples becomes JSON and comes back, without any
code of your own.

This post shows why a module is needed, how to add each one, the JSON they write, and what they check when they read.

<!-- more -->

## A record that needs help

Here is an order, with its lines in a `Vector` and an optional note:

```java
record Line(String sku, int quantity) {}

record Order(Vector<Line> lines, Option<String> note) {}
```

To read an `Order` from JSON, a JSON library has to build its `Vector` and its `Option`. It knows how to build the
types it was taught: for a collection, it calls a constructor and adds the elements one by one; for an object, it
calls a constructor or setters.

Zazr's types have neither. A `Vector` is immutable: there is no public constructor to call, and adding an element
returns a new `Vector` rather than changing the one you have. An `Option` is `Some` or `None`, not a box with a
setter. So the library needs to be told how to build them, and that is what the two modules do.

## Jackson: one dependency, one line

Add `zazr-jackson`. It depends on `zazr-core` and on Jackson 3 (`tools.jackson.core:jackson-databind`).

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

Then add `ZazrModule` when you build the mapper. The `Order` writes and reads back like any other record:

```java
var mapper = JsonMapper.builder().addModule(new ZazrModule()).build(); // JsonMapper

var order = new Order(Vector.of(new Line("A-1", 2)), Option.some("ring twice"));
var json  = mapper.writeValueAsString(order);
var back  = mapper.readValue(json, Order.class); // Order
// json is {"lines":[{"sku":"A-1","quantity":2}],"note":"ring twice"}, back equals order
```

If you already call `findAndAddModules()` on the builder, it finds `ZazrModule` too, as it finds every Jackson module
on the class path or module path.

## avaje-jsonb: the dependency alone

Add `zazr-avaje-jsonb`. It is built against avaje-jsonb 3.16.

=== "Maven"

    ```xml
    <dependency>
        <groupId>dev.zazr</groupId>
        <artifactId>zazr-avaje-jsonb</artifactId>
        <version>0.3.0</version>
    </dependency>
    ```

=== "Gradle (Kotlin)"

    ```kotlin
    dependencies {
        implementation("dev.zazr:zazr-avaje-jsonb:0.3.0")
    }
    ```

There is no line to add. avaje-jsonb finds the Zazr adapters as a service when it builds a `Jsonb`. Annotate the
records with `@Json`, as usual with avaje-jsonb:

```java
@Json
public record Line(String sku, int quantity) {}

@Json
public record Order(Vector<Line> lines, Option<String> note) {}
```

The same order gives the same JSON:

```java
var jsonb = Jsonb.instance();
var order = new Order(Vector.of(new Line("A-1", 2)), Option.some("ring twice"));
var json  = jsonb.toJson(order);
var back  = jsonb.type(Order.class).fromJson(json); // Order
// json is {"lines":[{"sku":"A-1","quantity":2}],"note":"ring twice"}, back equals order
```

## One JSON format

The two modules write the same JSON. A sequence or a set is an array, a map is an object, and `Option` is its value
or `null`:

| Zazr type | JSON | Example |
|---|---|---|
| `Vector`, `List`, `Queue`, `LazyList` | array | `[1,2,3]` |
| `HashSet`, `LinkedHashSet`, `TreeSet` | array | `["a","b"]` |
| `NonEmptyVector`, `NonEmptySet`, `NonEmptySortedSet` | array of at least one element | `[1]` |
| `HashMap`, `LinkedHashMap`, `TreeMap` | object | `{"a":1}` |
| `NonEmptyMap`, `NonEmptySortedMap` | object of at least one entry | `{"a":1}` |
| `Option` | the value, or `null` for `None` | `"x"`, `null` |
| `Tuple1` to `Tuple8` | array of 1 to 8 elements | `[1,"a"]` |

This is plain JSON that any other program can read. And since the format is shared, a service that writes with one
module can be read by a service that uses the other.

## The guarantees, kept at the boundary

A Zazr type makes promises: a `NonEmptyVector` is never empty, and a collection never holds `null`. JSON that comes
from outside makes no such promise, so the modules check it as they read, and reject what would break the type.

### Non-empty stays non-empty

Here is a shipment, which always has at least one line:

```java
@Json
public record Shipment(NonEmptyVector<Line> lines) {}
```

Reading a shipment with no lines fails:

```java
jsonb.type(Shipment.class).fromJson("{\"lines\":[]}"); // throws JsonDataException
```

avaje-jsonb throws a `JsonDataException` that names the type, the problem and where it is in the JSON:

```text
A NonEmptyVector needs at least one element: the JSON array is empty. (at position: 11, following: `{"lines":[]`, before: `}`)
```

With Jackson, the message starts with the same sentence, followed by Jackson's own location. An empty object for a
`NonEmptyMap` or a `NonEmptySortedMap` fails the same way.

### No `null` inside a collection

A `null` element, key or value of a collection is rejected on read, since Zazr collections hold no `null`. The
message says where the `null` is, and suggests `Option` for a value that may be missing.

`Option` itself reads `null` as `None`, so a `Vector<Option<Integer>>` reads `[1,null,3]` as
`Vector(Some(1), None, Some(3))`. And a tuple can hold `null` components, as `Tuple.of(1, null)` does.

### Map keys of any type

A JSON object only has text keys. A map whose keys are not `String`, such as dates, numbers or enums, is written with
the text of each key, and read back into the key type:

```java
var stock = HashMap.of(LocalDate.of(2026, 10, 4), 3);
var json  = mapper.writeValueAsString(stock); // {"2026-10-04":3}
var back  = mapper.readValue(json, new TypeReference<HashMap<LocalDate, Integer>>() {});
// back equals stock
```

avaje-jsonb writes the same `{"2026-10-04":3}`.

## What is out of scope

The modules cover the collections, `Option` and the tuples. `Either`, `Try`, `Validation` and `Lazy` have no JSON
format: map them to a covered type first, for example with `toOption()`.

The two modules differ in two places, each coming from the libraries themselves:

- an `Option` property that is missing from the JSON reads as `None` with Jackson, and as `null` with avaje-jsonb;
- an enum whose class overrides `toString()` can be written differently by the two libraries.

The [Jackson](../../jackson.md) and [avaje-jsonb](../../avaje-jsonb.md) pages give the details: how to read a
missing `Option` as `None` with avaje-jsonb, the order of sorted collections, type ids with Jackson, and the
[known issues of avaje-jsonb 3.16](../../avaje-jsonb.md#known-avaje-jsonb-316-issues).

## Thank you, vavr-jackson

`zazr-jackson` ports parts of [vavr-jackson](https://github.com/vavr-io/vavr-jackson), the Jackson module for Vavr:
how the types are registered with Jackson, and the shape of the readers for collections, maps and tuples. It gave us
a tested starting point. Thank you to its authors and contributors.

## What's next

The [Roadmap](../../roadmap.md) lists what the next releases bring. If a type you need is missing, or something is
unclear or broken, please [open an issue](https://github.com/guizmaii-opensource/zazr/issues): feedback is very
welcome.

Thank you for reading.
