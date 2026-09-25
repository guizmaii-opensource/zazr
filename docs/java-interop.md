---
description: asJava and asJavaMap views of the Zazr collections, the copies into java.util types, and the way back.
---

# Java interop

Zazr collections do not implement `java.util.List`, `Set` or `Map`: a Zazr type never advertises `add()` or `put()`
it cannot honour. At a JDK boundary, `asJava()` (`asJavaMap()` for a map) gives a view with no copy.

## Views

| Receiver | Method | Returns |
|---|---|---|
| `Vector`, `List`, `Queue`, `Stream`, `NonEmptyVector` | `asJava()` | `java.util.List` |
| `HashSet` | `asJava()` | `java.util.Set` |
| `LinkedHashSet` | `asJava()` | `SequencedSet` |
| `TreeSet` | `asJava()` | `NavigableSet` |
| `HashMap` | `asJavaMap()` | `java.util.Map` |
| `LinkedHashMap` | `asJavaMap()` | `SequencedMap` |
| `TreeMap` | `asJavaMap()` | `NavigableMap` |

Every view is O(1) to create. Reads go through to the Zazr value, which never changes, so a view never goes stale.

Every mutator throws `UnsupportedOperationException`, even one that would change nothing, such as `clear()` on an
empty view. That includes `pollFirst()` on a `NavigableSet` view and `setValue` on a map entry.

```java
Vector<String> names = Vector.of("Ada", "Grace");
java.util.List<String> view = names.asJava();
String second = view.get(1);
boolean rejected = Try.run(() -> view.add("Linus")).getCause() instanceof UnsupportedOperationException;
// second is "Grace", rejected is true
```

A view compares equal to any JDK collection with the same content: a list view to a `java.util.List` with the same
elements in the same order, a set view to any `java.util.Set`, a map view to any `java.util.Map`.

### Sorted views

The views of `TreeSet` and `TreeMap` navigate the tree: `ceiling`, `floor`, `first`, `get` and `size` are
O(log n). `subSet`, `headMap`, `descendingSet` and the like are views too.

```java
java.util.NavigableSet<Integer> scores = TreeSet.of(10, 20, 30, 40).asJava();
Integer atLeast25 = scores.ceiling(25);
java.util.NavigableSet<Integer> top = scores.tailSet(20, true);
java.util.NavigableMap<String, Integer> ages = TreeMap.of("Ada", 36, "Grace", 85).asJavaMap();
Integer grace = ages.get("Grace");
// atLeast25 is 30, top is [20, 30, 40], grace is 85
```

### Lazy `Stream`

The view of a `Stream` computes no element before a read needs it: `get(i)` computes the first `i + 1`, an iterator
one per step. `size()`, `lastIndexOf` and `hashCode` compute the whole `Stream`, so avoid them on an infinite one.

## Copies

When a JDK API needs a collection it can modify, copy the view with the JDK constructor you need:
`new java.util.ArrayList<>(vector.asJava())`, `new java.util.HashMap<>(map.asJavaMap())`.

```java
java.util.ArrayList<Integer> mutable = new java.util.ArrayList<>(Vector.of(1, 2).asJava());
mutable.add(3);
java.util.Set<String> jdkSet = new java.util.HashSet<>(HashSet.of("a", "b").asJava());
// mutable is [1, 2, 3], jdkSet holds a and b
```

## The way back, and streams

`ofAll` takes any `Iterable` or a `java.util.stream.Stream`; `collector()` collects a stream
([Builders](builders.md)). `Vector.ofAll` copies a `java.util.Collection` in one bulk copy.

Given the view of a value of its own type, `ofAll` returns that value, with no copy: `Vector.ofAll(vector.asJava())`
is `vector`, `TreeMap.ofAll(map.asJavaMap())` is `map` (when the comparator is the same). `stream()` on any Zazr collection is a
`java.util.stream.Stream` that reports the size and the ordering of the type.

```java
Vector<Integer> fromJdk = Vector.ofAll(java.util.List.of(3, 1, 2));
int sum = fromJdk.stream().mapToInt(Integer::intValue).sum();
Option<Integer> fromOptional = Option.ofOptional(java.util.Optional.of(4));
// Vector(3, 1, 2), 6, Some(4)
```

`Option` converts to and from `java.util.Optional` with `toOptional()` and `Option.ofOptional`; `Try` to and from
`CompletableFuture` with `toCompletableFuture()` and `Try.fromCompletableFuture`.
