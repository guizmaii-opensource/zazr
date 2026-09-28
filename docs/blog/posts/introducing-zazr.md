---
date: 2026-09-28
authors:
  - guizmaii
categories:
  - Announcements
slug: introducing-zazr
description: What Zazr is, why I built it, what it takes from Scala, ZIO and zio-prelude, and where it stands at 0.1.0.
---

# Introducing Zazr

Zazr is a library of immutable collections and functional types for Java 25 and later. It is built on what the
language now offers: records, sealed interfaces and pattern matching. Its tagline says where the ideas come from:
inspired by Scala 2.13+, ZIO, and zio-prelude.

This post says what Zazr is, why I built it, what it borrows and why, and where it stands today.

<!-- more -->

## What Zazr is

Zazr gives Java persistent collections: `Vector`, `List`, `Queue`, `LazyList`, hash and sorted sets and maps, and
collections that cannot be empty. Each operation whose cost depends on the size documents that cost, and one page
lists them all.

On a `NonEmptyVector`, `head`, `max` and `reduce` cannot fail, and the return types tell you when that guarantee is
lost: `filter` gives back a plain `Vector`, since it may be empty.

To build a collection in a loop, a builder fills it in place and hands it over once. To pass one to Java code,
`asJava()` gives a read-only `java.util` view in constant time, with no copy.

It also gives Java the types that make functional code pleasant to write: `Option`, `Either`, `Try`, `Validation`
and `Lazy`. They are sealed interfaces of records, so you take them apart with pattern matching, and the compiler
checks that every case is handled.

It has no runtime dependencies, and nothing inside it is `null`.

## Why I built it

I come from Scala and ZIO. There, I got used to a way of writing code: values that don't change, errors that are
values, and names that say what an operation does.

Java 25 has the pieces that make this style feel at home: records, sealed interfaces, record patterns and
exhaustive `switch`. I wanted a library that builds on those pieces, so that functional code in Java reads like
modern Java.

That is Zazr. The [Design](../../principles.md) page explains the ideas behind it, one by one.

## What I took, and why

Most of Zazr is borrowed. I took the parts I trust from the ecosystems I know, and ported them to Java.

### The Scala 2.13 collections library

Scala 2.13 rewrote its collections library, and Scala 3 uses that library unchanged. Zazr's collections are ported
from it:

- `Vector` is a radix-balanced tree of arrays of 32 elements, so reading, updating, appending and prepending are
  effectively constant time.
- `HashSet` and `HashMap` are CHAMP hash tries, compact and shared between versions.
- `filter` and `partition` on `TreeSet` and `TreeMap` use Scala's red-black tree algorithms, and keep every subtree
  they don't change.
- `LazyList` is fully lazy: building one, or calling a lazy operation on it, evaluates nothing.

The reason: these designs are well studied and have years of use behind them in Scala. So when Zazr has a choice to
make about a collection, it starts from what Scala does.

### ZIO's names

In ZIO, an operation is named after what it does. Combining values is `zip`. Turning a list of results into a
result of a list is `collectAll`. Recovering from an error is `catchAll`. Running a side effect on a value is `tap`.

Zazr uses those names. You can read the code without learning any theory first, and if you know ZIO, you already
know them.

### zio-prelude's `Validation`

When you check a form or a configuration, you want every error at once, not one per attempt. zio-prelude's
`Validation` does that: combining several checks keeps all of their errors, in a collection that is never empty.
Zazr's `Validation` works the same way. `flatMap` still stops at the first error, for the step that needs the
previous value.

### Scala's `Using`

`Using` is a port of Scala's `scala.util.Using`. It releases resources after use, whatever happened, and gives the
outcome as a `Try`. A `Using.manager` block can acquire as many resources as it needs, even when their number is
known only at run time.

## A taste

Here is a signup with two checks. Both fail, and the result keeps both errors:

```java
record Signup(String email, int age) {}

var email  = Validation.fromPredicate("jules", e -> e.contains("@"), e -> "email has no @");
var age    = Validation.fromPredicate(-1, a -> a >= 0, a -> "age is negative");
var signup = Validation.zipWith(email, age, Signup::new); // Validation<String, Signup>

var message = switch (signup) {
    case Valid(var s) -> "welcome, " + s.email();
    case Invalid(var errors) -> errors.mkString(", ");
};
// "email has no @, age is negative"
```

`zipWith` combines up to eight values in one call. `Valid` and `Invalid` are records, so the `switch` takes the
result apart, and the compiler knows those are the two cases. The [Validation](../../control/validation.md) page
goes further.

## Thank you, Vavr

Zazr is a fork of [Vavr](https://github.com/vavr-io/vavr), and it exists because Vavr exists.

Vavr was created by Daniel Dietrich and is maintained by Grzegorz Piwowarek, with its
[contributors](https://github.com/vavr-io/vavr/graphs/contributors). Their work gave Zazr its starting point: its
collections and its control types. Thank you, all of you.

Zazr has changed a lot since the fork. The [Compared to Vavr](../../vavr.md) page lists every difference, for anyone
who knows Vavr.

I also want to thank the Scala and ZIO communities. The collections, the names and `Validation` are their work, and
Zazr builds on it.

## Where it stands

Zazr 0.1.0 is on Maven Central:

```xml
<dependency>
    <groupId>dev.zazr</groupId>
    <artifactId>zazr-core</artifactId>
    <version>0.1.0</version>
</dependency>
```

It is pre-1.0, and the API may still change between releases. The
[release notes](https://github.com/guizmaii-opensource/zazr/releases) list every change.

Feedback is very welcome. If something is missing, unclear or broken, please
[open an issue](https://github.com/guizmaii-opensource/zazr/issues).

## What's next

The next posts will each teach one thing Zazr brings, starting with property-based testing with
[zazr-test](../../testing.md).

Until then:

- [Getting started](../../getting-started.md) takes you from the dependency to your own code.
- [Zazr in your AI assistant](../../ai-assistant.md) teaches your coding assistant to write Zazr code.
- The [GitHub repository](https://github.com/guizmaii-opensource/zazr) has the code, the issues and the
  releases.

Thank you for reading.
