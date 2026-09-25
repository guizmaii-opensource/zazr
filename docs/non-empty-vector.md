---
description: NonEmptyVector makes head, max and reduce total; its return types say which operations keep it non-empty.
---

# `NonEmptyVector`

`NonEmptyVector<A>` is a `Vector<A>` that has at least one element. It wraps a `Vector` rather than extending it: if
it were a subtype, `filter` would be inherited and the type would promise nothing where it is used. Every method
delegates to the wrapped `Vector`, so the costs are `Vector`'s.

## Total operations

On a `NonEmptyVector`, the operations that are partial on a `Vector` cannot fail and return the value itself, not an
`Option`: `head`, `last`, `max`, `min`, `maxBy`, `minBy`, `reduce`, `reduceLeft`, `reduceRight`, `reduceMap`.

```java
NonEmptyVector<Integer> scores = NonEmptyVector.of(7, 3, 9);
int best = scores.max(Integer::compare);
int total = scores.reduce(Integer::sum);
int first = scores.head();
// 9, 19, 7
```

## The return-type contract

Operations that keep or grow the size return a `NonEmptyVector`; operations that can shrink it return a `Vector`;
narrowing a possibly empty value returns an `Option`.

| Returns `NonEmptyVector` | Returns `Vector` | Returns `Option` |
|---|---|---|
| `map`, `flatMap` (to a `NonEmptyVector`), `append`, `appendAll`, `prepend`, `prependAll`, `concat`, `reverse`, `distinct`, `distinctBy`, `sorted`, `sortBy`, `zip`, `zipWith`, `zipWithIndex`, `scanLeft`, `update`, `tap`; `grouped` returns `Vector<NonEmptyVector<A>>`, `groupBy` a `HashMap<K, NonEmptyVector<A>>` | `filter`, `reject`, `collect`, `flatMapAll` (to any `Iterable`), `partitionMap`, `duplicates`, `duplicatesBy`, `tail`, `init`, `drop*`, `take*`, `slice`, `removeAt`, `remove`, `removeAll`, `toVector()` | `find`, `findLast`, `indexOfOption`, `tailNonEmpty()`, `initNonEmpty()` |

`appendAll` and `prependAll` accept a possibly empty `Vector` and still return a `NonEmptyVector`: accept the weak
type, return the strong one.

```java
NonEmptyVector<Integer> grown = NonEmptyVector.of(1).appendAll(Vector.empty());
Vector<Integer> evens = NonEmptyVector.of(1, 2, 3).filter(n -> n % 2 == 0);
Option<NonEmptyVector<Integer>> rest = NonEmptyVector.of(1).tailNonEmpty();
// NonEmptyVector(1), Vector(2), None
```

`flatMap` and `flatMapAll` are two names because a lambda fits both a function returning a `NonEmptyVector` and one
returning any `Iterable`, and Java would report the overload as ambiguous.

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
Option<NonEmptyVector<String>> fromInput = Vector.of("a", "b").toNonEmptyVector();
Option<NonEmptyVector<String>> fromNothing = Vector.<String>empty().toNonEmptyVector();
// Some(NonEmptyVector(a, b)), None
```

`fromIterable(head, tail)` is not an `of` overload on purpose: `of(nev1, nev2)` would otherwise pick "head, then the
elements of the second" whenever the elements are themselves iterables.

## Sharp edges

- A `NonEmptyVector` is not a `Vector` and is not equal to one with the same elements; compare through `toVector()`.
- It is `Iterable`, but not a `Traversable`: it declares the collection methods it keeps itself.
- `Validation` uses it for its errors, and `Validation.forEach` over a `NonEmptyVector` returns one
  ([Validation](validation.md)).
