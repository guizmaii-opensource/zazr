---
description: NonEmptyVector makes head, max and reduce total; its return types say which operations keep it non-empty.
---

# `NonEmptyVector`

`NonEmptyVector<A>` is a sequence with at least one element. It holds a `Vector` and has the same costs.

It has every operation of `Vector`, under the same names, except those that mean nothing on a non-empty sequence:

- `isEmpty`, `nonEmpty`, `orElse` and `toNonEmptyVector`, which would always give the same answer;
- the `Option` forms of what is total here: `headOption`, `lastOption`, `reduceOption`, `reduceLeftOption`,
  `reduceRightOption`, `singleOption`;
- `tailOption` and `initOption`: `tail` and `init` already return a `Vector`, and `tailNonEmpty()` and
  `initNonEmpty()` narrow back;
- `length`: the size is `size()`.

## Total operations

On a `Vector`, `head`, `max` or `reduce` can fail or return an `Option`, because the `Vector` may be empty. On a
`NonEmptyVector` they return the value itself. The same holds for `last`, `min`, `maxBy`, `minBy`, `average` and the
other `reduce` methods.

```java
var scores = NonEmptyVector.of(7, 3, 9);
var best = scores.max(Integer::compare); // Integer
var total = scores.reduce(Integer::sum); // Integer
var first = scores.head(); // Integer
// 9, 19, 7
```

## The return-type contract

The return type tells you whether the result can be empty:

| Returns | When | For example |
|---|---|---|
| `NonEmptyVector` | the operation keeps or grows the size | `map`, `append`, `insert`, `sorted`, `distinct`, `zip`, `scan`, `rotateLeft` |
| `Vector` | the operation may remove elements | `filter`, `collect`, `tail`, `take`, `drop`, `remove`, `patch` |
| a tuple of `Vector`s | a split, where either part may be empty | `splitAt`, `span`, `partition` |
| `Option` | you ask for a part that may not exist | `find`, `indexWhereOption`, `tailNonEmpty()`, `initNonEmpty()` |

`grouped`, `sliding` and `slideBy` return a `Vector` of `NonEmptyVector`s, and `groupBy` a `HashMap` whose values are
`NonEmptyVector`s. `unzip` returns `NonEmptyVector`s, and the first part of `splitAtInclusive` is one.
`appendAll`, `prependAll`, `insertAll` and `zipAll` accept an `Iterable` that may be empty and still return a
`NonEmptyVector`. `zip` and `crossProduct` return a `NonEmptyVector` when given one, and a `Vector` when given any
other `Iterable`.

```java
var grown = NonEmptyVector.of(1).appendAll(Vector.empty()); // NonEmptyVector<Integer>
var evens = NonEmptyVector.of(1, 2, 3).filter(n -> n % 2 == 0); // Vector<Integer>
var rest = NonEmptyVector.of(1).tailNonEmpty(); // Option<NonEmptyVector<Integer>>
// NonEmptyVector(1), Vector(2), None
```

```java
var xs = NonEmptyVector.of(1, 2, 3, 4);
var halves = xs.splitAt(2); // Tuple2<Vector<Integer>, Vector<Integer>>
var windows = xs.sliding(3); // Vector<NonEmptyVector<Integer>>
var mean = xs.average(); // double
// (Vector(1, 2), Vector(3, 4)), Vector(NonEmptyVector(1, 2, 3), NonEmptyVector(2, 3, 4)), 2.5
```

`flatMap` takes a function that returns a `NonEmptyVector`, and keeps the result non-empty. `flatMapAll` takes a
function that returns any `Iterable`, and returns a `Vector`.

## Construction

| Constructor | Returns |
|---|---|
| `of(head, rest...)`, `single(a)` | `NonEmptyVector<A>` |
| `fromIterable(head, Iterable tail)` | `NonEmptyVector<A>` |
| `fromVector(Vector)`, `fromIterable(Iterable)` | `Option<NonEmptyVector<A>>` |
| `Vector.toNonEmptyVector()` | `Option<NonEmptyVector<A>>` |
| `unsafeFromVector(Vector)` | `NonEmptyVector<A>`, or `IllegalArgumentException` when empty |
| static `flatten(NonEmptyVector<NonEmptyVector<A>>)` | `NonEmptyVector<A>` |

```java
var fromInput = Vector.of("a", "b").toNonEmptyVector(); // Option<NonEmptyVector<String>>
var fromNothing = Vector.<String>empty().toNonEmptyVector(); // Option<NonEmptyVector<String>>
// Some(NonEmptyVector(a, b)), None
```

## Sharp edges

- A `NonEmptyVector` is not a `Vector` and is not equal to one with the same elements; compare through `toVector()`.
- It is `Iterable`, but not a `Traversable`: a method that takes a `Traversable` needs `toVector()`.
- `Validation` uses it for its errors, and `Validation.forEach` over a `NonEmptyVector` returns one
  ([Validation](control/validation.md)).
