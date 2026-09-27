---
description: Which Zazr collection to choose, and what the collections have in common.
---

# Collections

Every Zazr collection is immutable. An operation returns a new collection and shares what it can with the old
one, which is never modified.

Each type declares its own methods, with its own return types, and every method whose cost depends on the size
documents it. The [complexity page](complexity.md) lists them all.

## Which one to choose

| You need | Choose | Because |
|---|---|---|
| a sequence, by default | [`Vector`](vector.md) | effectively O(1) `get`, `update`, `append`, `prepend`, `take`, `drop` |
| a sequence that has at least one element | [`NonEmptyVector`](../non-empty-vector.md) | `head`, `max`, `reduce` cannot fail |
| to take a sequence apart head first, or a stack | [`List`](list.md) | O(1) `prepend`, `head`, `tail`; pattern matching on `Cons` and `Nil` |
| first in, first out | [`Queue`](queue.md) | O(1) `enqueue`, amortised O(1) `dequeue` |
| a sequence computed on demand, possibly infinite | [`LazyList`](lazy-list.md) | each element computed once, when first read |
| a set, by default | [`HashSet`](sets.md) | effectively O(1) `contains`, `add`, `remove` |
| a set in insertion order | [`LinkedHashSet`](sets.md) | a `HashSet` plus the insertion order, with positional methods |
| a sorted set | [`TreeSet`](sets.md) | O(log n) lookups and updates, positional methods in comparator order |
| a set that has at least one element | [`NonEmptySet`, `NonEmptySortedSet`](../non-empty-set-map.md) | `max`, `reduce` (and `head` on the sorted one) cannot fail |
| a map, by default | [`HashMap`](maps.md) | effectively O(1) `get`, `put`, `remove` |
| a map in insertion order | [`LinkedHashMap`](maps.md) | a `HashMap` plus the insertion order, with positional methods |
| a sorted map | [`TreeMap`](maps.md) | O(log n) lookups and updates, positional methods in key order |
| a map that has at least one entry | [`NonEmptyMap`, `NonEmptySortedMap`](../non-empty-set-map.md) | `keySet` and `values` stay non-empty, `max` and `reduce` cannot fail |

## What they share

Every collection except the non-empty ones (`NonEmptyVector`, `NonEmptySet`, `NonEmptyMap` and their sorted variants)
implements `Traversable<T>`. It has what works the same way on every type: iterating, `size`,
`contains`, `find`, `foldLeft`, `mkString`, the conversions such as `toVector` and `stream()`, and the
[`asJava()` view](../java-interop.md).

`map`, `filter` and the rest are declared by each type, so they return that type.

Two more operations exist on most types:

- `partitionMap` splits a collection in one pass, using a function that returns an `Either`. The sequences and the
  hash sets have it.
- The static `flatten` removes one level of nesting.

```java
var split = List.of(1, 2, 3, 4) // Tuple2<List<Integer>, List<String>>
    .partitionMap(n -> n % 2 == 0 ? Either.left(n) : Either.right("odd " + n));
var flat = Vector.flatten(Vector.of(Vector.of(1, 2), List.of(3))); // Vector<Integer>
// split is (List(2, 4), List(odd 1, odd 3)), flat is Vector(1, 2, 3)
```

```java
var vector     = Vector.of(3, 1, 2);
var sortedList = vector.toList().sorted();  // List<Integer>
var set        = HashSet.ofAll(vector);     // HashSet<Integer>
var same       = Vector.of(1, 2, 3).equals(sortedList);
// List(1, 2, 3), a HashSet of 1, 2, 3, and true
```

A `Vector`, `List`, `Queue` or `LazyList` equals another of these four when they hold equal elements in the same
order. Sets equal sets and maps equal maps. A `NonEmptyVector` equals only another `NonEmptyVector`; a non-empty set
equals only a non-empty set, and a non-empty map only a non-empty map.

## Sums, products and averages

The sequences, the sets, `NonEmptyVector`, `NonEmptySet` and `NonEmptySortedSet` add up, multiply and average the
numbers a function reads from each element. The type of the function picks the arithmetic: `sumInt`, `sumLong`,
`sumDouble`, and `productInt`, `productLong`, `productDouble`.

- An `int` or `long` result is exact. When it does not fit, the method throws an `ArithmeticException` instead of
  wrapping around.
- `sumDouble` and `average` add with compensation, so the rounding errors do not pile up over many elements.
- An empty collection sums to 0 and multiplies to 1. `average` returns an `Option`, empty when there is no element; on
  the non-empty ones it returns the `double` itself.

```java
var words   = List.of("one", "three", "five");
var letters = words.sumInt(String::length);                     // int
var mean    = words.average(String::length);                    // Option<Double>
var big     = Vector.of(Integer.MAX_VALUE, 1).sumLong(n -> n);  // long
// 12, Some(4.0), 2147483648
```

## Nulls

No collection holds `null`: adding a `null` element, key or value throws a `NullPointerException`. Absence is an
`Option`, which is what `find`, `headOption` and `Map.get` return.

```java
var missing   = HashMap.of("a", 1).get("b");               // Option<Integer>
var firstEven = Vector.of(1, 3, 4).find(n -> n % 2 == 0);  // Option<Integer>
// None, Some(4)
```

A function that returns `null` where a collection needs an `Option`, a tuple or an iterable, such as the mapper of
`flatMap` or the function of `toMap`, throws a `NullPointerException` naming the method:
`Vector.flatMap: mapper returned null`. So does a `groupBy` classifier. A function that returns a plain element, such
as the mapper of `map`, is rejected like any other `null` element: `Vector: element is null`. A `LazyList` throws when
it reaches that element.

## Complexity

Each collection's page has a table of its common operations. [Complexity](complexity.md) puts all the collections
side by side and lists every documented method.
