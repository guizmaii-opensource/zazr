---
description: NonEmptySet, NonEmptyMap and their sorted variants make max and reduce total; their return types say which operations keep them non-empty.
---

# `NonEmptySet` and `NonEmptyMap`

Four types hold at least one element, as [`NonEmptyVector`](non-empty-vector.md) does for sequences:

| Type | Holds | Order | Total `head` and `last` |
|---|---|---|---|
| `NonEmptySet<A>` | a `HashSet` | not defined | no |
| `NonEmptySortedSet<A>` | a `TreeSet` | the comparator's | yes |
| `NonEmptyMap<K, V>` | a `HashMap` | not defined | no |
| `NonEmptySortedMap<K, V>` | a `TreeMap` | the comparator's, on the keys | yes |

Each has every operation of the type it holds, under the same names, and the same costs. The exceptions are the
operations that mean nothing on a non-empty collection:

- `isEmpty`, `nonEmpty`, `orElse` and `toNonEmptySet` (or `toNonEmptyMap`, and so on), which would always give the
  same answer;
- the `Option` forms of what is total here: `reduceOption`, `singleOption`, and on the sorted ones `headOption`,
  `lastOption`, `tailOption` and `initOption`;
- on the maps, `removeKeys` and `removeValues`, which the plain maps keep only as older names of `rejectKeys` and
  `rejectValues`.

## Total operations

On a `HashSet`, `max` and `reduce` return an `Option` or fail, because the set may be empty. Here they return the
value itself. The same holds for `min`, `maxBy`, `minBy`, `average` on the sets, and `head` and `last` on the sorted
variants.

```java
var tags = NonEmptySet.of("java", "scala", "java"); // NonEmptySet<String>
var longest = tags.maxBy(String::length); // String
var total = NonEmptySet.of(1, 2, 3).reduce(Integer::sum); // Integer
// scala, 6
```

A hash set has no order, so `NonEmptySet` and `NonEmptyMap` have no `head`. On the sorted variants, `head` and
`last` follow the comparator, while `min` and `max` use the natural order of the elements, as on every set.

## The return-type contract

The return type tells you whether the result can be empty:

| Returns | When | For example |
|---|---|---|
| the non-empty type | the operation cannot remove every element | `add`, `addAll`, `union`, `map`, `put`, `merge`, `mapValues`, `replace` |
| the plain type | the operation may remove elements | `filter`, `remove`, `intersect`, `diff`, `take`, `drop`, `tail` |
| `Option` | you ask for a part that may not exist | `find`, `get`, `tailNonEmpty()`, `initNonEmpty()` |

`addAll`, `union` and `merge` accept a collection that may be empty and still return the non-empty type. `map` on a
set may merge equal results, and `map` on a map equal keys, but never down to nothing.

On a map, `keySet()` returns a `NonEmptySet` (a `NonEmptySortedSet` on a `NonEmptySortedMap`) and `values()` a
`NonEmptyVector`. `groupBy` returns a `HashMap` whose values are non-empty, and on the sorted variants `grouped`,
`sliding` and `slideBy` return a `Vector` of them.

```java
var prices = NonEmptySortedMap.of(Tuple.of("pear", 3), Tuple.of("apple", 2)); // NonEmptySortedMap<String, Integer>
var first = prices.head(); // Tuple2<String, Integer>
var names = prices.keySet(); // NonEmptySortedSet<String>
var cheap = prices.filterValues(price -> price < 3); // TreeMap<String, Integer>
// (apple, 2), NonEmptySortedSet(apple, pear), TreeMap((apple, 2))
```

`flatMap` takes a function that returns the non-empty type, and keeps the result non-empty. `flatMapAll` takes a
function that returns any `Iterable`, and returns the plain type.

## Construction

| Constructor | Returns |
|---|---|
| `NonEmptySet.of(head, rest...)`, `single(a)`, `fromIterable(head, Iterable tail)` | `NonEmptySet<A>` |
| `NonEmptyMap.single(key, value)`, `of(entry, entries...)`, `fromIterable(entry, Iterable entries)` | `NonEmptyMap<K, V>` |
| `fromSet(HashSet)`, `fromMap(HashMap)`, `fromIterable(Iterable)` | an `Option` |
| `HashSet.toNonEmptySet()`, `HashMap.toNonEmptyMap()` | an `Option` |
| `unsafeFromSet(HashSet)`, `unsafeFromMap(HashMap)` | the non-empty type, or `IllegalArgumentException` when empty |

The sorted variants have the same constructors, each in two forms: one for the natural order and one that takes a
`Comparator` first. They wrap a `TreeSet` or a `TreeMap` with `fromSortedSet`, `fromSortedMap`,
`TreeSet.toNonEmptySortedSet()` and `TreeMap.toNonEmptySortedMap()`.

```java
var fromInput = HashMap.of("a", 1).toNonEmptyMap(); // Option<NonEmptyMap<String, Integer>>
var fromNothing = HashSet.<String>empty().toNonEmptySet(); // Option<NonEmptySet<String>>
// Some(NonEmptyMap((a, 1))), None
```

`toSet()`, `toSortedSet()`, `toMap()` and `toSortedMap()` without arguments return the collection held, without
copying.

## Costs

=== "NonEmptySet"

    --8<-- "NonEmptySet.md"

=== "NonEmptySortedSet"

    --8<-- "NonEmptySortedSet.md"

=== "NonEmptyMap"

    --8<-- "NonEmptyMap.md"

=== "NonEmptySortedMap"

    --8<-- "NonEmptySortedMap.md"

Every method: [complexity page](collections/complexity.md).

## Sharp edges

- A non-empty set is not a `Set` and is not equal to one with the same elements; compare through `toSet()` or
  `toSortedSet()`. The same goes for the maps. A `NonEmptySet` and a `NonEmptySortedSet` with the same elements are
  equal, as sets are.
- They are `Iterable`, but not `Traversable`. `union`, `intersect` and `diff` take a `Set`; to pass a non-empty set,
  use `addAll`, `retainAll` and `removeAll`, which take any `Iterable`.
- `reduce` and `fold` on a `NonEmptySet` or a `NonEmptyMap` see the elements in hash order: give them an operation
  where the order does not matter.
