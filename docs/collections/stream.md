---
description: Stream, the lazy memoising list - head-strict, possibly infinite, and which operations force it.
---

# `Stream`

A lazy list that remembers what it computed. The first element is computed when the `Stream` is built, each of the
others when it is first reached, and then kept. It can be infinite.

Its name clashes with `java.util.stream.Stream`: import `com.guizmaii.zazr.collection.Stream`, and write the JDK one
in full when you need both.

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

`Stream.iterate(seed, f)`, `Stream.continually(supplier)` and `Stream.ofAll` are other ways to create one.

## Costs

`lazy` means the call does no work beyond the first element; each element is computed when it is read. The notes
say which elements a call computes ("forces") right away.

--8<-- "Stream.md"

Every method: [complexity page](complexity.md#stream).

## Sharp edges

- Operations that need the whole sequence force it and never return on an infinite `Stream`: `length`, `size`,
  `last`, `reverse`, `sorted`, `max` and `min` (their notes say so), and anything that reads every element, such as
  `foldLeft`, `mkString` or `toVector`.
- The first element is never lazy: building a `Stream` computes it, and `map`, `filter` and the others compute the
  first element of their result.
- `partitionMap` looks for the first element of each side right away. On an infinite `Stream` whose elements all go
  to one side, it never returns.
- A `Stream` keeps every element it computed. Holding on to the start of a long `Stream` while walking it keeps all
  of it in memory.
