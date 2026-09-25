---
description: Stream, the lazy memoising list - head-strict, possibly infinite, and which operations force it.
---

# `Stream`

A lazy, memoising cons list: the head is computed when the `Stream` is built, the tail when it is first reached, and
each tail is cached. It can be infinite. The name is Vavr's; import `com.guizmaii.zazr.collection.Stream` and spell
`java.util.stream.Stream` out when you need both.

## When to choose it

For a sequence computed on demand: an infinite series, a sequence whose elements are expensive and only partly
read, or a sequence defined in terms of itself.

```java
Stream<Integer> naturals = Stream.from(1);
Vector<Integer> squares = naturals.map(n -> n * n).filter(n -> n % 2 == 1).take(4).toVector();
// Vector(1, 9, 25, 49)
```

```java
Stream<Long> fibonacci = Stream.of(0L, 1L).appendSelf(self -> self.zipWith(self.tail(), Long::sum));
Vector<Long> firstTen = fibonacci.take(10).toVector();
// Vector(0, 1, 1, 2, 3, 5, 8, 13, 21, 34)
```

Other sources: `Stream.iterate(seed, f)`, `Stream.continually(supplier)`, `Stream.cons(head, () -> tail)`, `cycle()`,
`extend(f)`, and `Stream.ofAll` of any `Iterable` or `java.util.stream.Stream`.

## Costs

The notes of a lazy operation say what it forces. `lazy` means the call does no work beyond the head; each element is
computed when the result reaches it.

--8<-- "Stream.md"

Every method: [complexity page](complexity.md#stream).

## Sharp edges

- Operations that need the whole sequence force it and never return on an infinite `Stream`: `length`, `size`,
  `last`, `reverse`, `sorted`, `max`, `min`, `foldLeft`, `mkString`, `toVector`, `equals`, `hashCode`. Their notes
  say so.
- A `Stream` is head-strict: building one computes its first element, and `map`, `filter` and the others compute the
  first element of their result.
- `partitionMap` is lazy too, but each side is forced to its first element when it is built, which walks the source
  until an element of that side is found: on an infinite `Stream` whose elements all go to one side, it does not
  return.
- Memoisation keeps every computed element reachable as long as the head is: holding the head of a long `Stream`
  while walking it keeps all of it in memory.
