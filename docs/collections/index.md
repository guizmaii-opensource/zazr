---
description: Which Zazr collection to choose, and what the collections have in common.
---

# Collections

Every Zazr collection is persistent: an operation returns a new collection and shares what it can with the old one,
which is never modified. There is no `Seq` promising positional access to every sequence: each type declares its own
API, with its own return types, and every positional method states its cost in a `Complexity:` line of its javadoc.
The [complexity page](complexity.md) gathers them all, generated from the javadoc.

## Which one to choose

| You need | Choose | Because |
|---|---|---|
| a sequence, by default | [`Vector`](vector.md) | effectively O(1) `get`, `update`, `append`, `prepend`, `take`, `drop` |
| a sequence that has at least one element | [`NonEmptyVector`](../non-empty-vector.md) | `head`, `max`, `reduce` cannot fail |
| to take a sequence apart head first, or a stack | [`List`](list.md) | O(1) `prepend`, `head`, `tail`; `switch` on `Cons` and `Nil` |
| first in, first out | [`Queue`](queue.md) | amortised O(1) `enqueue` and `dequeue` |
| a sequence computed on demand, possibly infinite | [`Stream`](stream.md) | lazy and memoised |
| a set, by default | [`HashSet`](sets.md) | effectively O(1) `contains`, `add`, `remove` |
| a set in insertion order | [`LinkedHashSet`](sets.md) | a `HashSet` plus the insertion order, with positional methods |
| a sorted set | [`TreeSet`](sets.md) | O(log n) lookups and updates, positional methods in comparator order |
| a map, by default | [`HashMap`](maps.md) | effectively O(1) `get`, `put`, `remove` |
| a map in insertion order | [`LinkedHashMap`](maps.md) | a `HashMap` plus the insertion order, with positional methods |
| a sorted map | [`TreeMap`](maps.md) | O(log n) lookups and updates, positional methods in key order |

## What they share

The one interface above them is `Traversable<T>`, and it declares only what costs the same on every type: iterating,
`size`, `isEmpty`, `contains`, `exists`, `forAll`, `count`, `find`, `foldLeft`, `mkString`, `forEach`, the conversions
`toVector`, `toList`, `toSet`, `toArray`, `stream()`, and the `asJava()` view ([Java interop](../java-interop.md)).
Everything else, `map` and `filter` included, is declared by each type with its own return type.

Two operations exist on almost every type under the same name: `partitionMap` splits in one pass by a function
returning an `Either` (on the sequences and the hash sets, each side of the receiver's type), and the static
`flatten` removes one level of nesting (every collection; `TreeSet.flatten` also takes a comparator).

```java
Tuple2<List<Integer>, List<String>> split = List.of(1, 2, 3, 4)
    .partitionMap(n -> n % 2 == 0 ? Either.left(n) : Either.right("odd " + n));
Vector<Integer> flat = Vector.flatten(Vector.of(Vector.of(1, 2), List.of(3)));
// split is (List(2, 4), List(odd 1, odd 3)), flat is Vector(1, 2, 3)
```

```java
Vector<Integer> vector = Vector.of(3, 1, 2);
List<Integer> sortedList = vector.toList().sorted();
HashSet<Integer> set = HashSet.ofAll(vector);
boolean same = Vector.of(1, 2, 3).equals(sortedList);
// List(1, 2, 3), a HashSet of 1, 2, 3, and true
```

The four sequences (`Vector`, `List`, `Queue`, `Stream`) are equal to each other when they hold equal elements in the
same order; sets equal sets and maps equal maps. `NonEmptyVector` equals only another `NonEmptyVector`.

## Nulls

No collection holds `null`: every factory, builder, insertion and update throws `NullPointerException` on a `null`
element, key or value. Absence is an `Option`, which is why `find`, `headOption` and `Map.get` can return one.

```java
Option<Integer> missing = HashMap.of("a", 1).get("b");
Option<Integer> firstEven = Vector.of(1, 3, 4).find(n -> n % 2 == 0);
// None, Some(4)
```

## Complexity

The per-type pages include their table of the common operations; [Complexity](complexity.md) has the matrices of all
of them side by side, the legend of the classes, and every documented method.
