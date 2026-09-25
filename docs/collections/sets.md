---
description: HashSet, LinkedHashSet and TreeSet - the set algebra, iteration order, and the positional subset of the ordered sets.
---

# Sets

Three sets with the same operations: `add`, `remove`, `contains`, `union`, `intersect`, `diff`, and the usual
`filter`, `map` and `fold`. They differ in how they order their elements.

| Type | Representation | Iteration order | Positional methods |
|---|---|---|---|
| `HashSet` | a hash-based tree | not defined | none |
| `LinkedHashSet` | a hash-based set that also records the insertion order | insertion order | yes |
| `TreeSet` | a sorted, balanced tree | the comparator's | yes |

## When to choose which

- `HashSet` by default.
- `LinkedHashSet` when the order the elements arrived in matters, for example to remove duplicates from a sequence
  and keep its order.
- `TreeSet` when you need the elements sorted, a range of them, or the least and the greatest.

```java
var tags   = HashSet.of("java", "scala");
var more   = tags.add("zio").remove("scala");                // HashSet<String>
var common = tags.intersect(HashSet.of("scala", "kotlin"));  // HashSet<String>
// more contains java and zio, common is HashSet(scala)
```

```java
var seen     = LinkedHashSet.of("b", "a").add("c").add("a");  // LinkedHashSet<String>
var sorted   = TreeSet.of(5, 1, 4, 2);
var smallest = sorted.head();                                 // Integer
var firstTwo = sorted.take(2);                                // TreeSet<Integer>
// seen is LinkedHashSet(b, a, c), smallest is 1, firstTwo is TreeSet(1, 2)
```

## Costs

=== "HashSet"

    --8<-- "HashSet.md"

=== "LinkedHashSet"

    --8<-- "LinkedHashSet.md"

=== "TreeSet"

    --8<-- "TreeSet.md"

Every method: [complexity page](complexity.md#sets).

## Positional methods

`LinkedHashSet` and `TreeSet` have a defined order, so they have the methods that depend on it: `head`, `last`,
`tail`, `take`, `drop`, `zipWithIndex`, `sliding`, `grouped` and their variants.

`HashSet` has none of them, because its order is not defined.

## Sharp edges

- Do not rely on the iteration order of a `HashSet`: it depends on the hashes and may change between versions.
  `fold` and `reduce` see the elements in that order, so give them an operation where the order does not matter.
- `max()` and `min()` use the natural order of the elements, which must be `Comparable`, and walk them all, even on a
  `TreeSet`. The least and greatest elements in a `TreeSet`'s own order are `head()` and `last()`, in O(log n).
- `TreeSet` decides membership with its comparator, not `equals`.
- `union`, `intersect` and `diff` are fast on two `TreeSet`s with the same comparator. With different comparators,
  or another kind of set, they process the elements one by one.
- `TreeSet` has no `partitionMap`.
