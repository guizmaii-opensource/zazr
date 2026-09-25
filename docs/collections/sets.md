---
description: HashSet, LinkedHashSet and TreeSet - the set algebra, iteration order, and the positional subset of the ordered sets.
---

# Sets

Three sets, one algebra: `add`, `addAll`, `remove`, `removeAll`, `contains`, `union`, `intersect`, `diff`,
`retainAll`, plus `filter`, `reject`, `map`, `flatMap`, `partition`, `groupBy`, `fold`, `reduce`, `max`, `min` and
the static `flatten`. `HashSet` and `LinkedHashSet` also have `partitionMap`; `TreeSet` does not, since each side
would need a comparator of its own.

| Type | Representation | Iteration order | Positional methods |
|---|---|---|---|
| `HashSet` | a hash array mapped trie (HAMT), 32-way | not promised | none |
| `LinkedHashSet` | a `LinkedHashMap` of the elements: a hash map plus a `Vector` of the insertion order | insertion order | yes |
| `TreeSet` | a red-black tree ordered by a `Comparator` | the comparator's | yes |

## When to choose which

`HashSet` by default. `LinkedHashSet` when the order the elements arrived in matters (deduplicating a sequence while
keeping its order). `TreeSet` when you need the elements sorted, a range of them, or the least and the greatest.

```java
HashSet<String> tags = HashSet.of("java", "scala");
HashSet<String> more = tags.add("zio").remove("scala");
HashSet<String> common = tags.intersect(HashSet.of("scala", "kotlin"));
// more contains java and zio, common is HashSet(scala)
```

```java
LinkedHashSet<String> seen = LinkedHashSet.of("b", "a").add("c").add("a");
TreeSet<Integer> sorted = TreeSet.of(5, 1, 4, 2);
Integer smallest = sorted.head();
TreeSet<Integer> firstTwo = sorted.take(2);
// seen is LinkedHashSet(b, a, c), smallest is 1, firstTwo is TreeSet(1, 2)
```

## Costs

=== "HashSet"

    --8<-- "HashSet.md"

=== "LinkedHashSet"

    --8<-- "LinkedHashSet.md"

=== "TreeSet"

    --8<-- "TreeSet.md"

Every method: [complexity page](complexity.md#sets). The `TreeSet` notes live on its interface, `SortedSet`.

## The positional subset of the ordered sets

`LinkedHashSet` and `TreeSet` promise an order, so they have `head`, `last`, `init`, `tail` and their `Option`
forms, `take`, `drop` and their right-hand and `While`/`Until` forms, `zipWithIndex`, `sliding`, `grouped` and
`slideBy`. `HashSet` has none of them: its order is the hash order, which the type does not promise.

## Sharp edges

- Do not rely on the iteration order of a `HashSet`: it depends on the hashes and may change between versions.
  `fold`, `reduce` and `mkString` see the elements in that order, so give `fold` and `reduce` an operation that
  does not depend on it.
- `max()` and `min()` use the natural order of the elements, which must be `Comparable`, and walk them all, on a
  `TreeSet` too. The least and greatest elements in a `TreeSet`'s own order are `head()` and `last()`, O(log n).
- `TreeSet` decides membership with its comparator, not `equals`.
- `union`, `intersect` and `diff` of two `TreeSet`s with the same comparator split and join the trees; with
  different comparators, or another kind of set, they fall back to element-by-element work.
