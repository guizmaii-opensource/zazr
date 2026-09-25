---
description: Vector, the default sequence - fast access by index, at both ends and in bulk.
---

# `Vector`

The default sequence, like ZIO's `Chunk`. A `Vector` is a tree of arrays of 32 elements, at most six levels deep.

Access by index, `update`, adding at either end, `take`, `drop` and `slice` are "effectively O(1)": their cost grows
with the depth of the tree, which is never more than six levels.

A `Vector` built from a primitive array, such as `Vector.ofAll(int...)` or `Vector.range`, stores the values
unboxed.

## When to choose it

Whenever you need a sequence and have no reason to pick another one. Access by index, updates, adding at either end,
`take`, `drop`, `slice` and bulk operations are all cheap.

Choose another sequence for a specific need:

- [`List`](list.md) to take a sequence apart from the front with pattern matching;
- [`Queue`](queue.md) for first in, first out;
- [`Stream`](stream.md) for a lazy or infinite sequence.

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

- `sliding`, `grouped` and `crossProduct` return a `Vector`, built at once, not an iterator. Each window shares its
  elements with the original `Vector`.
- `insert` and `removeAt` in the middle cost O(min(i, n - i)): the shorter side is copied element by element.
- Building element by element is cheapest through [`Vector.Builder`](../builders.md), or `Vector.collector()` from a
  `java.util.stream.Stream`.
