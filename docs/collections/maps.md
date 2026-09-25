---
description: HashMap, LinkedHashMap and TreeMap - Option-returning lookups, updates, iteration order and the positional subset.
---

# Maps

A map is a collection of its entries, each a `Tuple2<K, V>`. `get` returns an `Option`; `put` and `remove` return a
new map.

| Type | Representation | Iteration order | Positional methods |
|---|---|---|---|
| `HashMap` | a hash-based tree | not defined | none |
| `LinkedHashMap` | a hash-based map that also records the insertion order | insertion order | yes |
| `TreeMap` | a sorted, balanced tree of entries | the key comparator's | yes |

## When to choose which

- `HashMap` by default.
- `LinkedHashMap` when the order the keys were inserted in matters. Overwriting a key keeps its position.
- `TreeMap` for keys in sorted order, or the least and the greatest key.

```java
var stock = HashMap.of("apple", 3, "pear", 0);
var restocked = stock.put("pear", 5, Integer::sum).put("fig", 1); // HashMap<String, Integer>
var pears = restocked.get("pear"); // Option<Integer>
var kiwis = restocked.getOrElse("kiwi", 0); // Integer
// pears is Some(5), kiwis is 0
```

```java
var byName = TreeMap.of("b", 2, "a", 1, "c", 3);
var values = byName.values(); // Vector<Integer>
var first = byName.head(); // Tuple2<String, Integer>
// values is Vector(1, 2, 3), first is (a, 1)
```

`map`, `filter` and `forEach` take a function of the key and the value. `mapValues`, `filterKeys` and similar
methods work on one side. `keySet()` returns the keys as a set, and `values()` the values as a `Vector`, in iteration
order.

## Costs

=== "HashMap"

    --8<-- "HashMap.md"

=== "LinkedHashMap"

    --8<-- "LinkedHashMap.md"

=== "TreeMap"

    --8<-- "TreeMap.md"

Every method: [complexity page](complexity.md#maps).

## Sharp edges

- Do not rely on the iteration order of a `HashMap`: it depends on the hashes and may change between versions.
- `LinkedHashMap.remove` is amortised: most calls are effectively O(1), and now and then one pays O(n) to
  clean up the insertion order.
- `asJava()` on a map is a `java.util.Collection` of its `Tuple2` entries; the `java.util.Map` view is `asJavaMap()`
  ([Java interop](../java-interop.md)).
- Neither keys nor values can be `null`.
