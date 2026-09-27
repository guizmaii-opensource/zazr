---
description: LazyList, the lazy list that keeps what it computed - possibly infinite, and which calls compute its elements.
---

# `LazyList`

A lazy list that remembers what it computed. The first element is computed when the `LazyList` is built, each of the
others when it is first reached, and then kept. It can be infinite.

Unlike a `java.util.stream.Stream`, which is a one-shot pipeline, a `LazyList` is a collection: it can be read
many times, and each read after the first reuses what was computed.

## When to choose it

For a sequence computed on demand: an infinite series, a sequence whose elements are expensive and only partly
read, or a sequence defined in terms of itself.

```java
var naturals = LazyList.from(1); // LazyList<Integer>
var squares  = naturals.map(n -> n * n).filter(n -> n % 2 == 1).take(4).toVector();
// Vector(1, 9, 25, 49)
```

```java
var fibonacci = LazyList.of(0L, 1L).appendSelf(self -> self.zipWith(self.tail(), Long::sum));
var firstTen  = fibonacci.take(10).toVector(); // Vector<Long>
// Vector(0, 1, 1, 2, 3, 5, 8, 13, 21, 34)
```

`LazyList.iterate(seed, f)`, `LazyList.continually(supplier)` and `LazyList.ofAll` are other ways to create one.

## Costs

`lazy` means the call returns without walking the `LazyList`: each element is computed when the result reaches
it. Some calls compute a prefix now, and their note says how much: `filter` and the calls like it up to the first
element they keep, `drop`, `slice` and `dropRight` the elements they skip or hold back.

--8<-- "LazyList.md"

Every method: [complexity page](complexity.md#lazylist).

## Sharp edges

- Operations that need the whole sequence compute it and never return on an infinite `LazyList`: `size`,
  `last`, `reverse`, `sorted`, `max`, `min`, `foldRight`, `groupBy`, `lastIndexOfSlice(that)` and `hashCode`, and
  anything else that reads every element, such as `foldLeft`, `mkString` or `toVector`.
- `equals` stops at the first difference or at the end of the shorter side, so an infinite `LazyList` compared with a
  finite `List`, `Vector`, `Queue` or `LazyList` returns. Two infinite `LazyList`s with the same elements never do.
- `filter` and the calls like it (`reject`, `retainAll`, `removeAll`, `collect`, `flatMap`, `distinct`) compute
  elements until they find one to keep, when they are called and each time the result moves on. On an infinite
  `LazyList` with nothing more to keep, that search never ends.
- `partition` and `partitionMap` look for the first element of each side right away. On an infinite `LazyList` whose
  elements all go to one side, they never return.
- The first element is never lazy: building a `LazyList` computes it, and `map`, `tap` and the others compute the first
  element of their result.
- When computing an element throws, the `LazyList` keeps the exception in its place: reading that element again throws
  the same exception, and never skips to the next one. Only a `VirtualMachineError`, such as a stack overflow, lets a
  later read try again.
- A `LazyList` keeps every element it computed. Holding on to the start of a long `LazyList` while walking it keeps all
  of it in memory.
