---
description: Stream, the lazy list that keeps what it computed - possibly infinite, and which calls compute its elements.
---

# `Stream`

A lazy list that remembers what it computed. The first element is computed when the `Stream` is built, each of the
others when it is first reached, and then kept. It can be infinite.

Its name clashes with `java.util.stream.Stream`: import `dev.zazr.collection.Stream`, and write the JDK one
in full when you need both.

## When to choose it

For a sequence computed on demand: an infinite series, a sequence whose elements are expensive and only partly
read, or a sequence defined in terms of itself.

```java
var naturals = Stream.from(1); // Stream<Integer>
var squares  = naturals.map(n -> n * n).filter(n -> n % 2 == 1).take(4).toVector();
// Vector(1, 9, 25, 49)
```

```java
var fibonacci = Stream.of(0L, 1L).appendSelf(self -> self.zipWith(self.tail(), Long::sum));
var firstTen  = fibonacci.take(10).toVector(); // Vector<Long>
// Vector(0, 1, 1, 2, 3, 5, 8, 13, 21, 34)
```

`Stream.iterate(seed, f)`, `Stream.continually(supplier)` and `Stream.ofAll` are other ways to create one.

## Costs

`lazy` means the call returns without walking the `Stream`: each element is computed when the result reaches
it. Some calls compute a prefix now, and their note says how much: `filter` and the calls like it up to the first
element they keep, `drop`, `slice` and `dropRight` the elements they skip or hold back.

--8<-- "Stream.md"

Every method: [complexity page](complexity.md#stream).

## Sharp edges

- Operations that need the whole sequence compute it and never return on an infinite `Stream`: `size`,
  `last`, `reverse`, `sorted`, `max`, `min`, `foldRight`, `groupBy`, `lastIndexOfSlice(that)`, `equals` and
  `hashCode`, and anything else that reads every element, such as `foldLeft`, `mkString` or `toVector`.
- `filter` and the calls like it (`reject`, `retainAll`, `removeAll`, `collect`, `flatMap`, `distinct`) compute
  elements until they find one to keep, when they are called and each time the result moves on. On an infinite
  `Stream` with nothing more to keep, that search never ends.
- `partition` and `partitionMap` look for the first element of each side right away. On an infinite `Stream` whose
  elements all go to one side, they never return.
- The first element is never lazy: building a `Stream` computes it, and `map`, `tap` and the others compute the first
  element of their result.
- When computing an element throws, the `Stream` keeps the exception in its place: reading that element again throws
  the same exception, and never skips to the next one. Only a `VirtualMachineError`, such as a stack overflow, lets a
  later read try again.
- A `Stream` keeps every element it computed. Holding on to the start of a long `Stream` while walking it keeps all
  of it in memory.
