---
description: HashMap, LinkedHashMap and TreeMap - Option-returning lookups, updates, iteration order and the positional subset.
---

# Maps

A map is a collection of its entries, each a `Tuple2<K, V>`. `get` returns an `Option`; `put` and `remove` return a
new map.

| Type | Representation | Iteration order | Positional methods |
|---|---|---|---|
| `HashMap` | a compressed hash trie (CHAMP) | not defined | none |
| `LinkedHashMap` | a hash-based map that also records the insertion order | insertion order | yes |
| `TreeMap` | a sorted, balanced tree of entries | the key comparator's | yes |

`HashMap` is the structure of the immutable `HashMap` of the Scala 2.13 collections library, which Scala 3 uses
unchanged: a tree of nodes with up to 32 slots each, where five bits of a mix of the key's hash code pick the slot
at each level. A node stores its entries inline, keys and values side by side in one array, so an entry costs no
object of its own. Removing an entry folds the tree back, so equal maps have the same shape whatever order their
entries came in.

## When to choose which

- `HashMap` by default.
- `LinkedHashMap` when the order the keys were inserted in matters. Overwriting a key keeps its position. A key
  repeated in `of`, `ofEntries` or a collector does the same: it stays where it first appeared, with its last value.
- `TreeMap` for keys in sorted order, or the least and the greatest key.

```java
var stock     = HashMap.of("apple", 3, "pear", 0);
var restocked = stock.put("pear", 5, Integer::sum).put("fig", 1);  // HashMap<String, Integer>
var pears     = restocked.get("pear");                             // Option<Integer>
var kiwis     = restocked.getOrElse("kiwi", 0);                    // Integer
// pears is Some(5), kiwis is 0
```

```java
var byName = TreeMap.of("b", 2, "a", 1, "c", 3);
var values = byName.values();  // Vector<Integer>
var first  = byName.head();    // Tuple2<String, Integer>
// values is Vector(1, 2, 3), first is (a, 1)
```

## Updating one key

`updateWith` reads a key and writes it in one call. Its function receives the key's value as an `Option`, `None`
when the key is absent, and returns the new one: `Some` puts that value, `None` removes the key.

```java
var stock = HashMap.of("apple", 3, "pear", 1);
var sold  = stock.updateWith("pear", count -> count.map(n -> n - 1).filter(n -> n > 0));
var added = stock.updateWith("fig", count -> Option.some(count.getOrElse(0) + 4));
// sold has no pear left, added has fig -> 4
```

When nothing changes, the same map comes back: `None` for an absent key, or `Some` of the very object the key
already holds. On a `LinkedHashMap`, an updated key keeps its position.

`getOrElse` also takes a `Supplier`, for a default that costs something to make. It runs only when the key is
absent.

```java
var prices = HashMap.of("apple", 3);
var apple  = prices.getOrElse("apple", () -> lookUpPrice("apple"));  // 3, lookUpPrice is not called
var kiwi   = prices.getOrElse("kiwi", () -> lookUpPrice("kiwi"));    // what lookUpPrice returns
```

## Putting many entries

`putAll` puts every entry of another map, or of any iterable of `Tuple2`s, as successive `put`s would. On a key both
sides hold, the argument's value wins: this is Scala's `++` on maps.

`merge` is the other way round: this map's value stays, and only the keys it lacks are added.

```java
var prices  = HashMap.of("apple", 3, "pear", 4);
var updates = HashMap.of("pear", 5, "fig", 2);
var updated = prices.putAll(updates);  // HashMap<String, Integer>
var kept    = prices.merge(updates);   // HashMap<String, Integer>
// updated has pear -> 5, kept has pear -> 4, both have fig -> 2
```

On a `LinkedHashMap`, a key already present keeps its position and a new key goes to the end, as with `put`.

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
- `LinkedHashMap.remove` is amortised: most calls are effectively O(1), and now and then one pays O(n) to close
  the gaps that earlier removals left in the insertion order. The average holds over a chain of removals, each on
  the result of the previous one; removing again from an older map can pay O(n) every time.
- After removals, `tail`, `init`, `take`, `drop` and a single step of the iterator can cost O(n): they skip past
  the gaps first.
- `asJava()` on a map is a `java.util.Collection` of its `Tuple2` entries; the `java.util.Map` view is `asJavaMap()`
  ([Java interop](../java-interop.md)).
- Neither keys nor values can be `null`. A function or a supplier that returns `null` fails with a
  `NullPointerException` naming the method.
- With two `getOrElse` overloads, a `null` default needs a cast to the value type: `getOrElse(key, (Integer) null)`.
  Without it, `getOrElse(key, null)` does not compile on most maps, and on a `Map<K, Object>` it picks the `Supplier`
  overload and throws `supplier is null`, even when the key is present. On a `Map<K, Object>`, a `Supplier` passed
  as the default is called, not returned.
- `groupMap` groups what its function returns for each entry in a `Vector`, in the map's iteration order, as
  `values()` does; `groupBy` groups whole entries in maps of the same type.
