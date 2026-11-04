---
description: HashSet, LinkedHashSet and TreeSet - the set algebra, iteration order, and the positional subset of the ordered sets.
---

# Sets

Three sets with the same operations: `add`, `remove`, `contains`, `union`, `intersect`, `diff`, and the usual
`filter`, `map` and `fold`. They differ in how they order their elements.

| Type | Representation | Iteration order | Positional methods |
|---|---|---|---|
| `HashSet` | a compressed hash trie (CHAMP) | not defined | none |
| `LinkedHashSet` | a hash-based set that also records the insertion order | insertion order | yes |
| `TreeSet` | a sorted, balanced tree | the comparator's | yes |

`HashSet` is the structure of the immutable `HashSet` of the Scala 2.13 collections library, which Scala 3 uses
unchanged: a tree of nodes with up to 32 slots each, where five bits of a mix of the element's hash code pick the
slot at each level. A node stores its elements inline, so an element costs no object of its own. Removing an element
folds the tree back, so equal sets have the same shape whatever order their elements came in.

## When to choose which

- `HashSet` by default.
- `LinkedHashSet` when the order the elements arrived in matters, for example to remove duplicates from a sequence
  and keep its order. A repeated element stays where it first appeared.
- `TreeSet` when you need the elements sorted, or the least and the greatest.

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

## Ranges

A `TreeSet` cuts out the elements between two bounds in its comparator's order. The bounds need not be elements.

- `rangeFrom(from)` keeps the elements at or after `from`.
- `rangeUntil(until)` keeps those before `until`, and `rangeTo(to)` those at or before `to`.
- `rangeFromUntil(from, until)` keeps those at or after `from` and before `until`. It is empty when `from` is not
  before `until`; for `from` after `until`, `java.util.TreeSet.subSet` throws instead.

Each result is a `TreeSet` with the same comparator, built in O(log n): it shares the rest of the tree with the set
it came from, and is that set itself when nothing is cut.

`minAfter(element)` is the least element at or after `element`, and `maxBefore(element)` the greatest strictly before
it, both as an `Option`. `iteratorFrom(start)` walks the elements from `start` on, without building a range.

```java
var scores = TreeSet.of(10, 20, 30, 40, 50);
var middle = scores.rangeFromUntil(20, 40);  // TreeSet<Integer>
var upTo30 = scores.rangeTo(30);             // TreeSet<Integer>
var next   = scores.minAfter(25);            // Option<Integer>
var before = scores.maxBefore(20);           // Option<Integer>
// middle is TreeSet(20, 30), upTo30 is TreeSet(10, 20, 30), next is Some(30), before is Some(10)
```

## When nothing changes

An operation that changes nothing returns the set itself, not a copy: no allocation, and `==` tells you nothing
changed.

- `add` of an element already there, `remove` of an absent one, and `addAll`, `union`, `removeAll` or `diff` that
  add or remove nothing.
- `filter`, `reject`, `intersect` and `retainAll` that keep every element; `partition` returns the set as the side
  that gets everything.
- `replace` of an absent element, or by the very object already there.
- `take` and `drop` of a `LinkedHashSet` or `TreeSet`, and the ranges of a `TreeSet`, that cut nothing.
- `union` or `addAll` on an empty set returns the argument when it is a set of the same kind.

An equal element is not the same object: `replace(x, equalX)` stores `equalX`, so the result is a new set.

## Which of two equal elements stays

When the set and the argument hold equal elements that are different objects, `add`, `addAll`, `union`,
`intersect` and `retainAll` keep the set's own. So a set united with, or intersected with, an equal set comes back
unchanged.

When something changes, the parts of the tree or trie that do not are shared with the set, not copied: `diff` of a
`TreeSet` copies only the paths to the elements it removes, and `partition` of a `HashSet` gives each side the parts
whose elements all go to it.

## Sharp edges

- Do not rely on the iteration order of a `HashSet`: it depends on the hashes and may change between versions.
  `fold` and `reduce` see the elements in that order, so give them an operation where the order does not matter.
- `TreeSet.groupMap` puts the values of each group in a `HashSet`: they need not be comparable, and the set's
  comparator orders elements, not values. `groupBy` keeps the comparator, since its groups hold elements.
- `max()` and `min()` use the natural order of the elements, which must be `Comparable`, and walk them all, even on a
  `TreeSet`. The least and greatest elements in a `TreeSet`'s own order are `head()` and `last()`, in O(log n).
- `TreeSet` decides membership with its comparator, not `equals`, in every operation: `contains`, `add`, `remove`,
  `union`, `intersect`, `diff`, `removeAll` and `retainAll`, whatever the kind of the argument. `intersect` and
  `retainAll` keep each element of the set that the comparator equates with a given one, as the set's own object.
- `union`, `intersect` and `diff` are fast on two `TreeSet`s with the same comparator, and `union`, `diff`,
  `containsAll` and `equals` on two `HashSet`s: they work on whole parts of the trees. With different comparators,
  or another kind of set, they process the elements one by one.
- `HashSet.removeAll` and `diff` remove the given elements one by one, effectively O(1) each, and compare a
  `HashSet` argument part by part. `diff` of a larger `LinkedHashSet` looks up each element of the set in it
  instead. `intersect` is cheap when the argument is a `HashSet`: the smaller set is checked against the larger.
- `TreeSet` has no `partitionMap`.
