---
description: asJava views of the Zazr collections, the copies into java.util types, and the way back.
---

# Java interop

Zazr collections do not implement `java.util.List`, `Set` or `Map`: a Zazr type never advertises `add()` or `put()`
it cannot honour. At a JDK boundary, `asJava()` gives a view with no copy.

## `asJava()` views

| Receiver | `asJava()` returns | Cost |
|---|---|---|
| `Vector`, `List`, `Queue`, `Stream`, `NonEmptyVector` | an unmodifiable `java.util.List` | O(1) to create; `get(i)` costs what the type's `get(i)` costs |
| sets and maps (any `Traversable`) | an unmodifiable `java.util.Collection` of the elements, of `Tuple2` entries for a map | O(1) to create |

Reads go through to the Zazr value. Every mutator throws `UnsupportedOperationException`, whether or not it would
change anything, as `Collections.unmodifiableList` does.

```java
Vector<String> names = Vector.of("Ada", "Grace");
java.util.List<String> view = names.asJava();
String second = view.get(1);
boolean rejected = Try.run(() -> view.add("Linus")).getCause() instanceof UnsupportedOperationException;
// second is "Grace", rejected is true
```

A `java.util.List` view is a `SequencedCollection`, so `getFirst()`, `getLast()` and `reversed()` work on it.

## Copies

When a JDK API needs a collection it can modify, copy the view with the JDK constructor you need.

```java
java.util.ArrayList<Integer> mutable = new java.util.ArrayList<>(Vector.of(1, 2).asJava());
mutable.add(3);
java.util.Set<String> jdkSet = new java.util.HashSet<>(HashSet.of("a", "b").asJava());
// mutable is [1, 2, 3], jdkSet holds a and b
```

## The way back, and streams

`ofAll` takes any `Iterable` or a `java.util.stream.Stream`; `collector()` collects a stream
([Builders](builders.md)). `Vector.ofAll` copies a `java.util.Collection` in one bulk copy, and returns the `Vector`
itself, with no copy, when given the `asJava()` view of a `Vector`. `stream()` on any Zazr collection is a
`java.util.stream.Stream` that reports the size and the ordering of the type.

```java
Vector<Integer> fromJdk = Vector.ofAll(java.util.List.of(3, 1, 2));
int sum = fromJdk.stream().mapToInt(Integer::intValue).sum();
Option<Integer> fromOptional = Option.ofOptional(java.util.Optional.of(4));
// Vector(3, 1, 2), 6, Some(4)
```

`Option` converts to and from `java.util.Optional` with `toOptional()` and `Option.ofOptional`; `Try` to and from
`CompletableFuture` with `toCompletableFuture()` and `Try.fromCompletableFuture`.
