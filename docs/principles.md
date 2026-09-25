---
title: Design
description: The ideas behind Zazr's API, and the choices they led to.
---

# Design

Zazr starts from Vavr's collections and control types and changes how they are used. The changes follow a few
ideas, most of them borrowed from the Scala standard library, ZIO and zio-prelude. This page explains each one
and what it means for your code.

## Copy from the best: Scala 2.13+

Scala 2.13 rewrote its collections library, and Scala 3 uses that library unchanged. The rewrite made the
collections simpler to use and faster, and it is the best-tested design of persistent collections on the JVM.
When Zazr has a choice to make about a collection, it starts from what Scala 2.13+ does:

- An operation returns the same kind of collection it was called on: `grouped` on a `List` gives a `List` of
  `List`s, on a `Vector` a `Vector` of `Vector`s.
- A `Vector` can be built with a builder that fills its arrays in place, like Scala's `VectorBuilder`.
- `partitionMap` splits a collection in one pass, and every collection documents the cost of its operations,
  like Scala's performance characteristics page.
- Sorted sets combine with `union`, `intersect` and `diff` using Scala's red-black tree algorithms.
- Java interop goes through views, like `scala.jdk.CollectionConverters`, instead of copies.

## Build a collection once, not once per element

A persistent collection never changes: `append` returns a new collection that shares most of the old one. That
is what makes it safe to pass around, but it has a cost when you build a collection in a loop. Each `append`
copies part of the structure, so a loop of a million `append`s creates a million intermediate collections that
are thrown away at once.

Java code builds collections in loops all the time, and Vavr offered no better way. Zazr has builders, as Scala
does: a builder collects the elements in place, where nobody else can see them, and `result()` turns them into
the collection once.

```java
var squares = Vector.<Integer>newBuilder();
for (int i = 1; i <= 5; i++) {
    squares.add(i * i);
}
var result = squares.result();  // Vector(1, 4, 9, 16, 25)
```

Each element is written once and nothing is copied. A builder is used once: after `result()` it refuses more
elements, so the collection it returned can never change behind your back.

## Names say what happens

An operation is named after its result, not after the theory it comes from. Combining two values is `zip`.
Turning a list of `Option`s into an `Option` of a list is `collectAll`. Recovering from an error is `catchAll`.
If you have used ZIO, the names are the ones you already know.

## `zip` combines values

To combine several `Option`s, `Either`s, `Try`s or `Validation`s, call `zipWith` once with all of them, up to
eight, and a function that receives their values:

```java
var total = Option.zipWith(Option.some(1), Option.some(2), Option.some(3),
    (a, b, c) -> a + b + c); // Option<Integer>
// Some(6), and None as soon as one of them is None
```

There is no `ap`, no builder chain and no nested `Tuple2<Tuple2<A, B>, C>` to unpack.

## `Validation` keeps every error

`Either` stops at the first error. `Validation` collects all of them, which is what you want when checking a
form or a configuration:

```java
var age   = Validation.<String, Integer>invalid("age is negative");
var email = Validation.<String, String>invalid("email has no @");
var both  = age.zipWith(email, (a, e) -> a + e); // Validation<String, String>
// Invalid(age is negative, email has no @)
```

The errors are held in a `NonEmptyVector`, so an invalid value always carries at least one. `flatMap` still
stops at the first error, because the next step needs the previous value.

## Collections that cannot be empty

`NonEmptyVector` always has at least one element, so `head`, `max` and `reduce` cannot fail. Its return types
tell you when that guarantee is lost:

```java
var scores = NonEmptyVector.of(7, 3, 9);
var best   = scores.max(Integer::compare);  // Integer: 9, nothing can go wrong
var passed = scores.filter(s -> s > 5);     // Vector<Integer>: may be empty, so a Vector
```

## Every collection states its cost

Vavr has a `Seq` interface shared by all sequences. It lets you call `get(i)` on a `List`, where that walks
`i` elements. Zazr has no such interface: each collection declares its own operations, and every positional
operation documents its cost. The [complexity page](collections/complexity.md) lists them all in one place,
so you can choose a collection for what you do with it.

## Order is only promised where it exists

A `HashSet` or `HashMap` has no meaningful order, so it has no `head`, `take` or `zipWithIndex`. The ordered
ones do: `TreeSet` and `TreeMap` follow their comparator, `LinkedHashSet` and `LinkedHashMap` follow insertion
order.

## No `null` inside

`Some`, `Right`, `Success`, `Valid` and every collection reject `null`. Absence is an `Option`, created from a
nullable value with `ofNullable`:

```java
var name = Option.ofNullable(System.getenv("NO_SUCH_VARIABLE")); // Option<String>
// None: Option.some(null) and Vector.of(1, null) throw instead
```

## Modern Java first

Zazr requires Java 25 and uses what the language now offers instead of rebuilding it:

- `Option`, `Either`, `Try` and `Validation` are sealed interfaces with record cases, so you take them apart with
  `switch` and record patterns. Vavr's `Match` API is gone.
- Functions are the JDK's own (`Function`, `BiFunction`, `Supplier`). Zazr adds only what the JDK lacks: functions
  of three to eight arguments, and functions that may throw a checked exception.
- To pass a collection to Java code, `asJava()` gives a read-only `java.util` view in constant time, without
  copying.

## What was left out

Some parts of Vavr are not in Zazr: `Future` and `Promise`, the `Match` API, and the less used collections such
as `Array`, `CharSeq`, `Tree` and `Multimap`. [Compared to Vavr](vavr.md) lists every difference.

## The decision log

Every decision above, with the alternatives considered, is recorded in the
[decision log](https://github.com/guizmaii-opensource/zazr/blob/main/docs/design.md) in the repository. It is
written for contributors.
