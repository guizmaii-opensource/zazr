---
description: LazyList, the lazy list that keeps what it computed - possibly infinite, and nothing computed before it is read.
---

# `LazyList`

A lazy list that remembers what it computed. Nothing is computed before it is read, not even its first element or
whether it is empty: each element is computed when it is first reached, and then kept. It can be infinite.

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

## Nothing is computed before it is read

Building a `LazyList` and calling `map`, `filter` or `appendAll` on it compute nothing. Reading an element computes
the elements it needs, once, even when several threads read it.

```java
var seen    = new java.util.ArrayList<Integer>();
var squares = LazyList.from(1).tap(seen::add).map(n -> n * n); // LazyList<Integer>
// seen is empty: nothing is computed yet
var third = squares.get(2); // 9, and seen is [1, 2, 3]
```

`LazyList.cons(head, () -> tail)` takes its first element as a value. `LazyList.defer(() -> ...)` computes the whole
list, first element included, when it is first read.

`toString` computes nothing either. It shows the elements already computed, then `<not computed>` for the rest:
`squares` above prints `LazyList(1, 4, 9, <not computed>)`. When the computed elements loop back, it ends with
`<cycle>` instead: `LazyList.of(1, 2, 3).cycle()`, read past its third element, prints `LazyList(1, 2, 3, <cycle>)`.

It shows the elements computed by the time it reaches them. If another thread keeps computing more of an infinite
`LazyList` at the same time, `toString` keeps showing them too, and may not return.

## Costs

`lazy` means the call computes nothing: each element of the result is computed when it is first read. The note
says what reading the first element computes: `filter` and the calls like it the elements up to the first one they
keep, `drop`, `slice` and `dropRight` the elements they skip or hold back, `rotateLeft` or `takeRight` the whole
list. `reverse`, `sorted` and `scanRight` compute the whole list when they are called.

--8<-- "LazyList.md"

Every method: [complexity page](complexity.md#lazylist).

## Sharp edges

- Operations that need the whole sequence compute it and never return on an infinite `LazyList`: `size`, `last`,
  `lastOption`, `findLast`, `endsWith`, `max`, `min`, `reduceLeft`, `reduceRight`, `foldRight`, `groupBy`, `groupMap`,
  `groupMapReduce`, `arrangeBy`, `hashCode`, and `lastIndexOf`, `lastIndexWhere` and `lastIndexOfSlice` without an end
  index. So does anything else that reads every element, such as `foldLeft`, `mkString` or `toVector`.
  `sizeCompare(n)` computes at most `n + 1` elements, so it returns.
- `reverse`, `sorted`, `sortBy`, `shuffle`, `scanRight` and `LazyList.transpose` return a `LazyList`, but compute the
  whole sequence at the call: on an infinite `LazyList` they never return either.
- `equals` stops at the first difference or at the end of the shorter side, so an infinite `LazyList` compared with a
  finite `List`, `Vector`, `Queue` or `LazyList` returns. Two infinite `LazyList`s with the same elements never do.
- `filter` and the calls like it (`reject`, `retainAll`, `removeAll`, `collect`, `flatMap`, `distinct`) compute
  elements until they find one to keep, each time the result is read further. On an infinite `LazyList` with nothing
  more to keep, reading the next element never returns. `isEmpty()` reads: it runs that search too.
- `partition` and `partitionMap` return at once, but reading a side that stays empty on an infinite `LazyList` never
  returns.
- An out-of-range index given to `insert`, `insertAll`, `removeAt` or `update` throws when the result is read that
  far, not when the method is called (unless the `LazyList` is already known to be empty). A negative index throws
  at once.
- `LazyList.cons(head, supplier)` calls the supplier when the tail is read, not when `tail()` returns it: a supplier
  that returns null fails there.
- When computing an element throws, the `LazyList` keeps the exception in its place: reading that element again throws
  the same exception, and never skips to the next one. Only a `VirtualMachineError`, such as a stack overflow, lets a
  later read try again.
- A chain of thousands of lazy operations (`map`, `filter`, `take`, `defer` inside `defer`) built without reading
  anything is evaluated recursively on the first read, as in Scala, and can overflow the stack then. Reading as you
  go, or reading on a thread with a bigger stack, avoids it. A chain of `drop`s, or a loop of `append` or
  `appendAll`, has no such limit.
- A `LazyList` keeps every element it computed. Holding on to the start of a long `LazyList` while walking it keeps all
  of it in memory.
