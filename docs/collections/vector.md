---
description: Vector, the default sequence - a 32-wide bit-mapped trie with effectively constant positional access.
---

# `Vector`

The default sequence, like ZIO's `Chunk`. A `Vector` is a bit-mapped trie: a tree with 32-wide nodes, at most six
levels deep, whose leaves hold the elements. "Effectively O(1)" means O(log32 n): a walk or a path copy of at most six
nodes. The leaves of a `Vector` built from a primitive array (`Vector.ofAll(int[])` and the like) stay unboxed.

## When to choose it

Whenever you need a sequence and have no reason to pick another one: positional access, updates, both ends,
`take`/`drop`/`slice` and bulk operations are all cheap. Choose [`List`](list.md) to deconstruct head first with a
`switch`, [`Queue`](queue.md) for first in, first out, [`Stream`](stream.md) for a lazy or infinite sequence.

```java
Vector<String> letters = Vector.of("a", "b", "c", "d");
Vector<String> changed = letters.update(1, "B").prepend("z").drop(2);
Tuple2<Vector<String>, Vector<String>> halves = letters.splitAt(2);
// changed is Vector(B, c, d), halves is (Vector(a, b), Vector(c, d))
```

```java
Vector<Integer> numbers = Vector.range(0, 10);
Vector<Vector<Integer>> windows = numbers.sliding(3, 3);
Tuple2<Vector<Integer>, Vector<String>> parts = numbers.partitionMap(
    n -> n % 2 == 0 ? Either.left(n) : Either.right("odd " + n));
// windows is Vector(Vector(0, 1, 2), Vector(3, 4, 5), Vector(6, 7, 8), Vector(9))
```

## Costs

--8<-- "Vector.md"

Every method: [complexity page](complexity.md#vector).

## Sharp edges

- `sliding`, `grouped` and `crossProduct` return `Vector`s (of `Vector`s or of tuples), built eagerly; each window
  is an effectively O(1) slice that shares the leaves.
- `insert` and `removeAt` in the middle are O(min(i, n - i)): the shorter side is re-appended element by element.
- Building element by element is cheapest through [`Vector.Builder`](../builders.md), or `Vector.collector()` from a
  `java.util.stream.Stream`.
