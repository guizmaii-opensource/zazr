---
description: HashMap, LinkedHashMap and TreeMap - Option-returning lookups, updates, iteration order and the positional subset.
---

# Maps

A map is a `Traversable` of its entries, `Tuple2<K, V>`. `get` returns an `Option`; `put`, `remove` and their
variants return a new map.

| Type | Representation | Iteration order | Positional methods |
|---|---|---|---|
| `HashMap` | a hash array mapped trie (HAMT), 32-way | not promised | none |
| `LinkedHashMap` | a `HashMap` of slots plus a `Vector` of keys in insertion order | insertion order | yes |
| `TreeMap` | a red-black tree of entries ordered by a key `Comparator` | the comparator's | yes |

## When to choose which

`HashMap` by default. `LinkedHashMap` when the order the keys were inserted in matters; overwriting a key keeps its
position. `TreeMap` for keys in sorted order, ranges, or the least and the greatest key.

```java
HashMap<String, Integer> stock = HashMap.of("apple", 3, "pear", 0);
HashMap<String, Integer> restocked = stock.put("pear", 5, Integer::sum).put("fig", 1);
Option<Integer> pears = restocked.get("pear");
int kiwis = restocked.getOrElse("kiwi", 0);
// pears is Some(5), kiwis is 0
```

```java
TreeMap<String, Integer> byName = TreeMap.of("b", 2, "a", 1, "c", 3);
Vector<Integer> values = byName.values();
Tuple2<String, Integer> first = byName.head();
// values is Vector(1, 2, 3), first is (a, 1)
```

The map-shaped operations take a `BiFunction` or `BiPredicate` over key and value (`map`, `flatMap`, `filter`,
`reject`, `forEach`), or work on one side (`mapKeys`, `mapValues`, `filterKeys`, `filterValues`, `rejectKeys`,
`rejectValues`). `keySet()` is a set of the keys and `values()` a `Vector` of the values, in iteration order.

## Costs

=== "HashMap"

    --8<-- "HashMap.md"

=== "LinkedHashMap"

    --8<-- "LinkedHashMap.md"

=== "TreeMap"

    --8<-- "TreeMap.md"

Every method: [complexity page](complexity.md#maps). Most `TreeMap` notes live on its interface, `SortedMap`.

## Sharp edges

- Do not rely on the iteration order of a `HashMap`: it depends on the hashes and may change between versions.
- `LinkedHashMap.remove` leaves a marker in the insertion order; the order is rebuilt, in O(n), when the markers
  outnumber the entries, so the cost is amortised.
- `asJava()` on a map is a `java.util.Collection` of its `Tuple2` entries, not a `java.util.Map`
  ([Java interop](../java-interop.md)).
- Neither keys nor values can be `null`.
