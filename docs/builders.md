---
description: The builders of Vector, List, HashMap, HashSet, TreeMap, TreeSet, LinkedHashMap and LinkedHashSet, the cheapest way to build them element by element, and the collectors of every collection.
---

# Builders

## `Vector.Builder`

A mutable, single-use accumulator for a `Vector`. Use it when you build a `Vector` in a loop: each `append` on a
`Vector` copies part of it, while the builder writes each element once.

```java
var builder = Vector.<String>newBuilder(); // Vector.Builder<String>
for (String word : "the quick brown fox".split(" ")) {
    builder.add(word.toUpperCase());
}
var words = builder.result(); // Vector<String>
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

`newBuilder(sizeHint)` takes the expected size. The hint is checked but currently changes nothing: the builder costs
the same as `newBuilder()`.

```java
var both   = Vector.<Integer>newBuilder(8);  // Vector.Builder<Integer>
both.addAll(Vector.of(1, 2, 3)).add(4);
var added  = both.size();
var result = both.result();                  // Vector<Integer>
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

When two entries have equal keys, the one put last wins, as with successive `put` calls. A `HashSet` does the
opposite: of equal elements, it keeps the one added first, as `add` does.

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

## Insertion-ordered builders

`LinkedHashMap` and `LinkedHashSet` have a builder of the same shape. It gives what calling `put` or `add` one by
one gives, without making a new map at each step.

```java
var firstSeen = LinkedHashSet.<String>newBuilder()
    .addAll(List.of("b", "a", "b", "c"))
    .result(); // LinkedHashSet<String>
var latest = LinkedHashMap.<String, Integer>newBuilder()
    .put("b", 1).put("a", 2).put("b", 3)
    .result(); // LinkedHashMap<String, Integer>
// LinkedHashSet(b, a, c), LinkedHashMap((b, 3), (a, 2))
```

A key put twice keeps its first position and takes the last key object and value. An element added twice keeps
its first position and its first object.

Give an empty builder an existing `LinkedHashMap` with `putAll`, or a `LinkedHashSet` with `addAll`, and
`result()` returns it as it is if nothing else is added. That map or set never changes.

`LinkedHashMap.ofEntries`, the `ofAll` factories of both types and their collectors use the builders.

## `List.Builder`

A `List` is built from its last element to its first. The builder keeps the elements in an array, then
`result()` makes one cell per element, from the end. Prepending in a loop and reversing makes every cell twice.

```java
var builder = List.<Integer>newBuilder(); // List.Builder<Integer>
for (int i = 1; i <= 3; i++) {
    builder.add(i * i);
}
var squares = builder.result(); // List<Integer>
// List(1, 4, 9)
```

A `List` given to `addAll` becomes the end of the result as it is, when nothing is added after it. Its cells
are shared, not copied.

```java
var tail  = List.of(8, 9);
var whole = List.<Integer>newBuilder().add(7).addAll(tail).result(); // List<Integer>
// List(7, 8, 9); whole.tail() is tail itself
```

`List.ofAll` of anything but a `java.util.List`, `List.flatten` and the collector use the builder.

## Collectors

Every collection has a `collector()` for `java.util.stream.Stream.collect`. The collectors of `Vector`, `List`,
`HashMap`, `HashSet`, `TreeMap`, `TreeSet`, `LinkedHashMap` and `LinkedHashSet` use their builder.

```java
var lengths = java.util.stream.Stream.of("a", "bb", "ccc").map(String::length)
    .collect(Vector.collector()); // Vector<Integer>
var sorted = java.util.stream.Stream.of("b", "a", "b").collect(TreeSet.collector()); // TreeSet<String>
// Vector(1, 2, 3), TreeSet(a, b)
```

`Queue` has no builder yet. Its `ofAll` factories take any `Iterable` or `java.util.stream.Stream`.
