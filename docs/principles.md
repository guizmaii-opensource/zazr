---
title: Design
description: The ideas behind Zazr's API, and the choices they led to.
---

# Design

Zazr starts from Vavr's collections and control types and changes how they are used. The changes follow a few
ideas, most of them borrowed from ZIO, zio-prelude and the Scala standard library. This page explains each one
and what it means for your code.

## Names say what happens

An operation is named after its result, not after the theory it comes from. Combining two values is `zip`.
Turning a list of `Option`s into an `Option` of a list is `collectAll`. Recovering from an error is `catchAll`.
If you have used ZIO, the names are the ones you already know.

## `zip` combines values

To combine several `Option`s, `Either`s, `Try`s or `Validation`s, call `zipWith` once with all of them, up to
eight, and a function that receives their values:

```java
Option<Integer> total = Option.zipWith(Option.some(1), Option.some(2), Option.some(3),
    (a, b, c) -> a + b + c);
// Some(6), and None as soon as one of them is None
```

There is no `ap`, no builder chain and no nested `Tuple2<Tuple2<A, B>, C>` to unpack.

## `Validation` keeps every error

`Either` stops at the first error. `Validation` collects all of them, which is what you want when checking a
form or a configuration:

```java
Validation<String, Integer> age = Validation.invalid("age is negative");
Validation<String, String> email = Validation.invalid("email has no @");
Validation<String, String> both = age.zipWith(email, (a, e) -> a + e);
// Invalid(age is negative, email has no @)
```

The errors are held in a `NonEmptyVector`, so an invalid value always carries at least one. `flatMap` still
stops at the first error, because the next step needs the previous value.

## Collections that cannot be empty

`NonEmptyVector` always has at least one element, so `head`, `max` and `reduce` cannot fail. Its return types
tell you when that guarantee is lost:

```java
NonEmptyVector<Integer> scores = NonEmptyVector.of(7, 3, 9);
int best = scores.max(Integer::compare);             // 9, nothing can go wrong
Vector<Integer> passed = scores.filter(s -> s > 5);  // may be empty, so a Vector
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
Option<String> name = Option.ofNullable(System.getenv("NO_SUCH_VARIABLE"));
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
