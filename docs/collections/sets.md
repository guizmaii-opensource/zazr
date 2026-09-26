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

`HashSet` is the structure of Scala's immutable `HashSet`: a tree of nodes with up to 32 slots each, where five bits
of a mix of the element's hash code pick the slot at each level. A node stores its elements inline, so an element costs no object
of its own. Removing an element folds the tree back, so equal sets have the same shape whatever order their elements
came in.

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

## Sharp edges

- Do not rely on the iteration order of a `HashSet`: it depends on the hashes and may change between versions.
  `fold` and `reduce` see the elements in that order, so give them an operation where the order does not matter.
- `max()` and `min()` use the natural order of the elements, which must be `Comparable`, and walk them all, even on a
  `TreeSet`. The least and greatest elements in a `TreeSet`'s own order are `head()` and `last()`, in O(log n).
- `TreeSet` decides membership with its comparator, not `equals`, in `contains`, `add`, `remove` and `union`.
  `removeAll` and `retainAll` put their argument in a hash set, so `equals` and `hashCode` decide there. So do
  `intersect` and `diff`, unless the argument is a `TreeSet` with the same comparator.
- `union`, `intersect` and `diff` are fast on two `TreeSet`s with the same comparator, and `union`, `diff`,
  `containsAll` and `equals` on two `HashSet`s: they work on whole parts of the trees. With different comparators,
  or another kind of set, they process the elements one by one.
- `LinkedHashSet.remove` is amortised, as on `LinkedHashMap`: now and then a call pays O(n) to close the gaps
  earlier removals left, and removing again from an older set can pay it every time. After removals, `tail`,
  `init`, `take`, `drop` and a single step of the iterator can cost O(n).
- `HashSet.removeAll` and `diff` walk the whole set, even to remove one element, unless the argument is a `HashSet`:
  use `remove` for a few elements.
  `intersect` is cheap when the argument is a `HashSet`: the smaller set is checked against the larger.
- `TreeSet` has no `partitionMap`.
