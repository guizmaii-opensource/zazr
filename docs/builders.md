---
description: The builders of Vector, HashMap, HashSet, TreeMap and TreeSet, the cheapest way to build them element by element, and the collectors of every collection.
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

`newBuilder(sizeHint)` takes the expected size. It matters only for vectors of up to 32 elements: the builder then allocates exactly that many slots. Larger vectors are built from full blocks of 32, so the hint changes nothing for them.

```java
Vector.Builder<Integer> both = Vector.newBuilder(8);
both.addAll(Vector.of(1, 2, 3)).add(4);
int added = both.size();
Vector<Integer> result = both.result();
// added is 4, result is Vector(1, 2, 3, 4)
```

`Vector`'s own bulk operations, such as `ofAll`, `flatMap` or `collect`, already use a builder.

## Map and set builders

`HashMap`, `HashSet`, `TreeMap` and `TreeSet` have a builder of the same shape. Maps use `put` and `putAll`
instead of `add` and `addAll`.

```java
var counts = HashMap.<String, Integer>newBuilder(); // HashMap.Builder<String, Integer>
for (var word : "to be or not to be".split(" ")) {
    counts.put(word, word.length());
}
var lengths = counts.result(); // HashMap<String, Integer>
// HashMap((to, 2), (be, 2), (or, 2), (not, 3)), in some order
```

When two entries have equal keys, the one put last wins, as with successive `put` calls. The same goes for equal
elements of a set.

A `HashMap` or `HashSet` builder changes its own nodes in place. A persistent `put` would copy the path from the
root each time. Give an empty builder an existing map with `putAll`, and it starts from that map without copying it.
That map never changes.

A `TreeMap` or `TreeSet` builder collects the elements, then sorts them once in `result()` and builds a balanced
tree in one pass. Input that is already sorted costs a single pass. `newBuilder(comparator)` sets the order; the
natural order is the default.

```java
var sorted = TreeSet.newBuilder(java.util.Comparator.<String>reverseOrder())
    .addAll(List.of("pear", "apple", "fig"))
    .result(); // TreeSet<String>
// TreeSet(pear, fig, apple)
```

The comparator runs in `size()` and `result()`, not in `add`. On a `TreeMap` or `TreeSet` builder, `size()` sorts
what was added since the last call.

The `ofAll` and `ofEntries` factories of these four types, and their collectors, use the builders.

## Collectors

Every collection has a `collector()` for `java.util.stream.Stream.collect`. The collectors of `Vector`, `HashMap`,
`HashSet`, `TreeMap` and `TreeSet` use their builder.

```java
Vector<Integer> lengths = java.util.stream.Stream.of("a", "bb", "ccc").map(String::length).collect(Vector.collector());
TreeSet<String> sorted = java.util.stream.Stream.of("b", "a", "b").collect(TreeSet.collector());
// Vector(1, 2, 3), TreeSet(a, b)
```

`List`, `Queue`, `LinkedHashMap` and `LinkedHashSet` have no builder yet. Their `ofAll` factories take any
`Iterable` or `java.util.stream.Stream`.
