---
name: zazr
description: >-
  Writes idiomatic Java 25 code with Zazr (package com.guizmaii.zazr, Maven artifact com.guizmaii:zazr-core), a
  library of immutable collections and functional types forked from Vavr: Option, Either, Try, Validation, Lazy,
  Using, Vector, NonEmptyVector, List, Queue, Stream, HashSet, TreeSet, HashMap, TreeMap. Use it when the code
  imports com.guizmaii.zazr, when porting Vavr code to Zazr, or when the user asks for immutable or persistent
  collections, Option, Either or Validation, errors as values, or functional-style code in Java 25.
license: Apache-2.0
---

# Zazr

Zazr gives Java 25 immutable collections and the types of functional code: `Option`, `Either`, `Try`, `Validation`,
`Lazy` and `Using`. Its control types are sealed interfaces of records, so the code takes them apart with pattern
matching. Its names come from ZIO and its collections from Scala's. Website: https://zazr.dev/.

## When to use this skill

- The code imports `com.guizmaii.zazr`, or the build depends on `com.guizmaii:zazr-core`.
- The user wants immutable collections, `Option`/`Either`/`Validation`, or errors as values in Java.
- The code is being moved from Vavr (`io.vavr`) to Zazr.

## Where things are

| Package | Types |
|---|---|
| `com.guizmaii.zazr.control` | `Option` (`Some`, `None`), `Either` (`Left`, `Right`), `Try` (`Success`, `Failure`), `Validation` (`Valid`, `Invalid`), `Using` |
| `com.guizmaii.zazr.collection` | `Vector`, `NonEmptyVector`, `List` (`Cons`, `Nil`), `Queue`, `Stream`, `HashSet`, `LinkedHashSet`, `TreeSet`, `HashMap`, `LinkedHashMap`, `TreeMap`, `Traversable` |
| `com.guizmaii.zazr` | `Lazy`, `Tuple`, `Tuple0` to `Tuple8`, `Function3` to `Function8`, the `Checked*` functional interfaces |

The cases are nested records: `import com.guizmaii.zazr.control.Option.Some;`. With a `module-info.java`, add
`requires com.guizmaii.zazr;`.

## The ten rules

1. **Pattern match, don't `get()`.** Read an `Option`, `Either`, `Try`, `Validation` or `List` with a `switch`
   expression over its records (`case Some(var v) ->`, `case None() ->`); it needs no `default`. Otherwise use
   `fold`, `map` or `getOrElse`. `get()` throws on the empty or failed case.
2. **No `null` inside.** `Some`, `Left`, `Right`, `Success`, `Valid` and every collection reject `null`. Wrap a
   nullable value at the boundary with `Option.ofNullable`. `Option.map` to `null` throws: use
   `flatMap(x -> Option.ofNullable(...))`.
3. **Pick the right result type.** `Option` when a value may be missing and there is nothing to say why. `Either`
   when each step needs the previous one and the first error stops the work. `Validation` for independent checks
   whose errors must all be reported. `Try` around code that throws. `Using` to release resources and get a `Try`.
   Exceptions stay for bugs and for the failing environment.
4. **Combine with `zipWith`.** `Option.zipWith(a, b, c, f)` (and on `Either`, `Try`, `Validation`, `Lazy`) takes
   2 to 8 values and a function of them. Do not nest `flatMap`s to combine independent values.
5. **Parse, don't check.** Turn input into a precise type once, at the edge: `vector.toNonEmptyVector()` instead of
   `if (list.isEmpty())`, a `Validation` or `Either` returning a record instead of a `boolean`.
6. **`Vector` is the default sequence**, `HashSet` the default set, `HashMap` the default map. `List` only to take
   a sequence apart from the front. Choose by the cost of what you do; `HashSet` and `HashMap` have no positional
   methods (`head`, `take`) because their order is not defined.
7. **Build in bulk.** A loop of `append` copies each time. Use `Vector.newBuilder()` (or the `HashMap`, `HashSet`,
   `TreeMap`, `TreeSet` builders), `collector()` from a `java.util.stream.Stream`, or `ofAll`.
8. **Use Zazr's names, not Vavr's.** `Option.ofNullable` (no `Option.of`), `zipWith`, `collectAll`, `forEach`,
   `mapBoth`, `tap`, `catchAll`, `flip`, `fromPredicate`. No `Match`, no `Seq`, no `Function1`. No
   category-theory words (`ap`, `pure`, `traverse`, `sequence`) in names or comments.
9. **Cross to the JDK with views.** `asJava()` (`asJavaMap()` for a map) gives a read-only `java.util` view in
   O(1); `ofAll` and `collector()` come back. `List` and `Stream` clash with the JDK names: import Zazr's and write
   `java.util.List` and `java.util.stream.Stream` in full. Use `size()`: every collection has it, `NonEmptyVector`
   has no `length()`.
10. **Write functional Java.** Records and persistent collections instead of setters, expressions instead of
    statements that assign, effects at the edges. Locals are `var`, with the type in a comment when it is not
    obvious.

```java
record Signup(String name, NonEmptyVector<String> emails) {}

static Validation<String, Signup> signup(String name, Vector<String> emails) {
    return Validation.zipWith(
        Validation.fromPredicate(name.trim(), n -> !n.isEmpty(), n -> "name is required"),
        Validation.fromOption(emails.toNonEmptyVector(), () -> "at least one email is required"),
        Signup::new);
}
```

```java
var reply = switch (signup("", Vector.empty())) { // Validation<String, Signup>
    case Valid(var s) -> "welcome, " + s.name();
    case Invalid(var errors) -> errors.mkString("; ");
};
// "name is required; at least one email is required"
```

## Reference files

Read the one that matches the task:

- [references/control-types.md](references/control-types.md): `Option`, `Either`, `Try`, `Validation`, `Lazy`,
  `Using`: when to use each, the members they share, conversions, sharp edges.
- [references/collections.md](references/collections.md): which collection to choose, costs, builders and
  collectors, `NonEmptyVector`, Java interop, sharp edges.
- [references/zip.md](references/zip.md): `zip` and `zipWith` at arity 2 to 8, and what happens on failure.
- [references/from-vavr.md](references/from-vavr.md): the Vavr names and types an assistant tends to write, and
  the Zazr ones to write instead.
- [references/functional-java.md](references/functional-java.md): how to write functional Java 25, with Zazr as
  the toolkit.

For tests, `com.guizmaii:zazr-test` adds property-based testing; see https://zazr.dev/testing/.
