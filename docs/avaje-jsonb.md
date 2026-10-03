---
description: zazr-avaje-jsonb reads and writes the Zazr collections, Option and tuples with avaje-jsonb - the dependency, the JSON of each type, Option and missing properties, the errors, and native images.
---

# avaje-jsonb

`dev.zazr:zazr-avaje-jsonb` teaches [avaje-jsonb](https://avaje.io/jsonb/) to read and write the Zazr collections,
`Option` and the tuples. A record with Zazr fields becomes JSON and comes back, with no code of yours.

It is available from Zazr 0.3.0, and is built against avaje-jsonb 3.16.

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

## Registration

Adding the dependency is enough. avaje-jsonb finds the Zazr adapters as a service when it builds a `Jsonb`, from
`Jsonb.instance()` or `Jsonb.builder().build()`.

With a `module-info.java`, the module `dev.zazr.avaje.jsonb` is found the same way: Java resolves it as a provider of
avaje-jsonb's service, so `requires dev.zazr;` and `requires io.avaje.jsonb;` are all your module needs. An image
built with `jlink` includes it with `--bind-services`, or with `--add-modules dev.zazr.avaje.jsonb`.

## Reading and writing

Annotate your records with `@Json` as usual. Their Zazr fields, generic ones included, are read and written by the
Zazr adapters.

```java
@Json
public record Line(String sku, int quantity) {}

@Json
public record Order(Vector<Line> lines, Option<String> note) {}
```

```java
var jsonb = Jsonb.instance();
var order = new Order(Vector.of(new Line("a-1", 2)), Option.none());

var json = jsonb.toJson(order);                     // {"lines":[{"sku":"a-1","quantity":2}],"note":null}
var back = jsonb.type(Order.class).fromJson(json);  // Order, equal to order
```

## The JSON of each type

| Zazr type | JSON |
|---|---|
| `Vector`, `List`, `Queue`, `LazyList`, `HashSet`, `LinkedHashSet`, `TreeSet` | array |
| `HashMap`, `LinkedHashMap`, `TreeMap` | object |
| `Option` | the value, or `null` for `None` |
| `NonEmptyVector`, `NonEmptySet`, `NonEmptySortedSet` | array, never empty |
| `NonEmptyMap`, `NonEmptySortedMap` | object, never empty |
| `Tuple1` to `Tuple8` | array of exactly 1 to 8 elements |

The details:

- The elements are written in their iteration order. `LinkedHashSet` and `LinkedHashMap` keep the order of the JSON;
  `TreeSet`, `TreeMap` and their non-empty versions sort it in the natural order of their elements or keys.
- A `LazyList` is written by iterating it, so it must be finite.
- A map key that is not a `String` is written as the text of its JSON value: `{"2026-10-03":1}` for a `LocalDate`,
  `{"42":1}` for an `Integer`, the name of the constant for an enum.
- Of two equal keys in the same JSON object, the later wins.

```java
var agenda = HashMap.of(LocalDate.of(2026, 10, 3), Vector.of("standup"));
var type   = Types.newParameterizedType(HashMap.class, LocalDate.class,
        Types.newParameterizedType(Vector.class, String.class));

var json = jsonb.toJson(agenda); // {"2026-10-03":["standup"]}
var back = jsonb.<HashMap<LocalDate, Vector<String>>>type(type).fromJson(json);
```

## `Option` and missing properties

An `Option` property is always written, `None` as `null`, even when the `Jsonb` leaves out `null` and empty
properties. `null` is read as `None`.

A property missing from the JSON is a different case: avaje-jsonb gives it the value `null`, and no adapter is asked.
So a missing `Option` property is `null`, not `None`. To read it as `None`, give it that default in the record's
compact constructor:

```java
@Json
public record Settings(String theme, Option<String> locale) {

    public Settings {
        locale = locale == null ? Option.none() : locale;
    }
}
```

```java
var settings = jsonb.type(Settings.class).fromJson("{\"theme\":\"dark\"}");

var locale = settings.locale(); // Option<String>: None
```

## What reading rejects

Reading fails with a `JsonDataException` that names the type, the problem and the position in the JSON:

- an empty array or object for `NonEmptyVector`, `NonEmptySet`, `NonEmptySortedSet`, `NonEmptyMap` or
  `NonEmptySortedMap`;
- a `null` element, key or value, since Zazr collections hold no `null` (a `null` for an `Option` is `None`);
- an array of the wrong length for a tuple;
- an element or a key of a sorted type that is not `Comparable`.

```java
var vectors = Types.newParameterizedType(NonEmptyVector.class, Integer.class);

jsonb.type(vectors).fromJson("[]"); // throws JsonDataException:
// NonEmptyVector needs at least one element, but the JSON array is empty, at position: 2, following: `[]`
```

## Native images

avaje-jsonb generates the adapters of your records when it compiles them, and the Zazr adapters use no reflection.
The Zazr adapters are registered as a service listed in `META-INF/services`, which GraalVM's `native-image` includes
by default, so a native image needs no configuration for them.
