---
description: Vector.Builder, the cheapest way to build a Vector element by element, and the collectors of every collection.
---

# Builders

## `Vector.Builder`

A mutable, single-use accumulator for a `Vector`. Use it when you build a `Vector` in a loop: each `append` on a
`Vector` copies part of it, while the builder writes each element once.

```java
Vector.Builder<String> builder = Vector.newBuilder();
for (String word : "the quick brown fox".split(" ")) {
    builder.add(word.toUpperCase());
}
Vector<String> words = builder.result();
// Vector(THE, QUICK, BROWN, FOX)
```

| Member | Does |
|---|---|
| `Vector.newBuilder()` | an empty builder |
| `add(a)` | appends one element |
| `addAll(iterable)` | appends every element |
| `size()` | the number of elements added so far |
| `result()` | the `Vector`; afterwards every method throws `IllegalStateException` |

A builder is not thread-safe and not reusable: after `result()`, create a new one. Adding `null` throws a
`NullPointerException`.

`newBuilder(sizeHint)` takes the expected size. It only helps for small vectors, of 32 elements or fewer.

```java
Vector.Builder<Integer> both = Vector.newBuilder(8);
both.addAll(Vector.of(1, 2, 3)).add(4);
int added = both.size();
Vector<Integer> result = both.result();
// added is 4, result is Vector(1, 2, 3, 4)
```

`Vector`'s own bulk operations, such as `ofAll`, `flatMap` or `collect`, already use a builder.

## Collectors

Every collection has a `collector()` for `java.util.stream.Stream.collect`. `Vector.collector()` uses a
`Vector.Builder`.

```java
Vector<Integer> lengths = java.util.stream.Stream.of("a", "bb", "ccc").map(String::length).collect(Vector.collector());
TreeSet<String> sorted = java.util.stream.Stream.of("b", "a", "b").collect(TreeSet.collector());
// Vector(1, 2, 3), TreeSet(a, b)
```

The other collections have no builder. Their `ofAll` factories take any `Iterable` or `java.util.stream.Stream`.
