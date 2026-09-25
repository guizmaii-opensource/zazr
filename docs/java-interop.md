---
description: asJava and asJavaMap views of the Zazr collections, the copies into java.util types, and the way back.
---

# Java interop

Zazr collections do not implement `java.util.List`, `Set` or `Map`, because they cannot support `add()` or `put()`.
To pass one to Java code, `asJava()` (`asJavaMap()` for a map) gives a read-only view, without copying.

## Views

| Receiver | Method | Returns |
|---|---|---|
| `Vector`, `List`, `Queue`, `Stream`, `NonEmptyVector` | `asJava()` | `java.util.List` |
| `HashSet`, `NonEmptySet` | `asJava()` | `java.util.Set` |
| `LinkedHashSet` | `asJava()` | `SequencedSet` |
| `TreeSet`, `NonEmptySortedSet` | `asJava()` | `NavigableSet` |
| `HashMap`, `NonEmptyMap` | `asJavaMap()` | `java.util.Map` |
| `LinkedHashMap` | `asJavaMap()` | `SequencedMap` |
| `TreeMap`, `NonEmptySortedMap` | `asJavaMap()` | `NavigableMap` |

Every view is O(1) to create. Reads go through to the Zazr value, which never changes, so a view never goes stale.

Every method that would modify the view throws `UnsupportedOperationException`, even one that would change nothing,
such as `clear()` on an empty view. That includes `pollFirst()` on a `NavigableSet` view and `setValue` on a map
entry.

```java
var names = Vector.of("Ada", "Grace");
var view = names.asJava(); // java.util.List<String>
var second = view.get(1);
var rejected = Try.run(() -> view.add("Linus")).getCause() instanceof UnsupportedOperationException;
// second is "Grace", rejected is true
```

A view is equal to any JDK collection with the same content: a list view to a `java.util.List` with the same
elements in the same order, a set view to any `java.util.Set`, a map view to any `java.util.Map`.

### Sorted views

The views of `TreeSet` and `TreeMap` navigate the tree: `ceiling`, `floor`, `first`, `get` and `size` are
O(log n). `subSet`, `headMap`, `descendingSet` and the like are views too.

```java
var scores = TreeSet.of(10, 20, 30, 40).asJava(); // java.util.NavigableSet<Integer>
var atLeast25 = scores.ceiling(25); // Integer
var top = scores.tailSet(20, true); // java.util.NavigableSet<Integer>
var ages = TreeMap.of("Ada", 36, "Grace", 85).asJavaMap(); // java.util.NavigableMap<String, Integer>
var grace = ages.get("Grace"); // Integer
// atLeast25 is 30, top is [20, 30, 40], grace is 85
```

### Lazy `Stream`

The view of a `Stream` computes no element before a read needs it: `get(i)` computes the first `i + 1`, an iterator
one per step. `size()`, `lastIndexOf` and `hashCode` compute the whole `Stream`, so avoid them on an infinite one.

## Copies

When a JDK API needs a collection it can modify, copy the view with the JDK constructor you need:
`new java.util.ArrayList<>(vector.asJava())`, `new java.util.HashMap<>(map.asJavaMap())`.

```java
var mutable = new java.util.ArrayList<>(Vector.of(1, 2).asJava()); // java.util.ArrayList<Integer>
mutable.add(3);
var jdkSet = new java.util.HashSet<>(HashSet.of("a", "b").asJava()); // java.util.HashSet<String>
// mutable is [1, 2, 3], jdkSet holds a and b
```

## The way back, and streams

To come back from Java:

- `ofAll` takes any `Iterable` or `java.util.stream.Stream`.
- `collector()` collects a `java.util.stream.Stream` ([Builders](builders.md)).
- `ofAll` of the view of a value of its own type returns that value, without copying: `Vector.ofAll(vector.asJava())`
  is `vector`, and `TreeMap.ofAll(map.asJavaMap())` is `map` when the comparator is the same.

In the other direction, `stream()` on any Zazr collection returns a `java.util.stream.Stream`.

```java
var fromJdk = Vector.ofAll(java.util.List.of(3, 1, 2)); // Vector<Integer>
var sum = fromJdk.stream().mapToInt(Integer::intValue).sum();
var fromOptional = Option.ofOptional(java.util.Optional.of(4)); // Option<Integer>
// Vector(3, 1, 2), 6, Some(4)
```

`Option` converts to and from `java.util.Optional` with `toOptional()` and `Option.ofOptional`; `Try` to and from
`CompletableFuture` with `toCompletableFuture()` and `Try.fromCompletableFuture`.
