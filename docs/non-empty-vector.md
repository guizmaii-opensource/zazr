---
description: NonEmptyVector makes head, max and reduce total; its return types say which operations keep it non-empty.
---

# `NonEmptyVector`

`NonEmptyVector<A>` is a sequence with at least one element. It holds a `Vector` and has the same costs.

## Total operations

On a `Vector`, `head`, `max` or `reduce` can fail or return an `Option`, because the `Vector` may be empty. On a
`NonEmptyVector` they return the value itself. The same holds for `last`, `min`, `maxBy`, `minBy` and the other
`reduce` methods.

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
| `NonEmptyVector` | the operation keeps or grows the size | `map`, `append`, `appendAll`, `sorted`, `distinct`, `zip` |
| `Vector` | the operation may remove elements | `filter`, `collect`, `tail`, `take`, `drop`, `remove` |
| `Option` | you ask for a part that may not exist | `find`, `tailNonEmpty()`, `initNonEmpty()` |

`grouped` returns a `Vector` of `NonEmptyVector`s, and `groupBy` a `HashMap` whose values are `NonEmptyVector`s.
`appendAll` and `prependAll` accept a `Vector` that may be empty and still return a `NonEmptyVector`.

```java
var grown = NonEmptyVector.of(1).appendAll(Vector.empty()); // NonEmptyVector<Integer>
var evens = NonEmptyVector.of(1, 2, 3).filter(n -> n % 2 == 0); // Vector<Integer>
var rest = NonEmptyVector.of(1).tailNonEmpty(); // Option<NonEmptyVector<Integer>>
// NonEmptyVector(1), Vector(2), None
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
