---
description: Vector.Builder, the cheapest way to build a Vector element by element, and the collectors of every collection.
---

# Builders

## `Vector.Builder`

A mutable, single-use accumulator for a `Vector`. Elements are written once, into the 32-wide leaf arrays the
resulting `Vector` uses as they are; only a partially filled last leaf is trimmed, once, by `result()`. Appending
element by element to a `Vector` instead copies a path of the trie at every step.

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
| `Vector.newBuilder()`, `Vector.newBuilder(sizeHint)` | an empty builder; a hint of 32 or less pre-sizes the first leaf |
| `add(a)` | appends one element |
| `addAll(iterable)` | appends every element; from a `Vector`, whole leaves are copied or shared |
| `size()` | the number of elements added so far |
| `result()` | the `Vector`; afterwards every method throws `IllegalStateException` |

A builder is not thread-safe and not reusable: after `result()`, create a new one. A `null` element is rejected with a
`NullPointerException`, like everywhere else.

```java
Vector.Builder<Integer> both = Vector.newBuilder(8);
both.addAll(Vector.of(1, 2, 3)).add(4);
int added = both.size();
Vector<Integer> result = both.result();
// added is 4, result is Vector(1, 2, 3, 4)
```

`Vector`'s own bulk operations (`ofAll` of an iterator or a stream, `flatMap`, `collect`, `zip`, `distinct`,
`scanLeft` and others) already go through a builder.

## Collectors

Every collection has a `collector()` for `java.util.stream.Stream.collect`. `Vector.collector()` accumulates into a
`Vector.Builder`; the collectors of the other collections accumulate into an `ArrayList`, then build the collection
from it.

```java
Vector<Integer> lengths = java.util.stream.Stream.of("a", "bb", "ccc").map(String::length).collect(Vector.collector());
TreeSet<String> sorted = java.util.stream.Stream.of("b", "a", "b").collect(TreeSet.collector());
// Vector(1, 2, 3), TreeSet(a, b)
```

The other collections have no builder of their own yet; their `ofAll` factories take any `Iterable` or
`java.util.stream.Stream`.
