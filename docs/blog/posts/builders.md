---
date: 2026-10-05
authors:
  - guizmaii
categories:
  - Collections
slug: builders
description: "Builders in Zazr: fill a persistent collection in a loop, hand it over once with result(), and pick the builder that goes with each collection."
---

# Building collections in a loop, with builders

Zazr's collections never change. That is what makes them safe to share, but it raises a question the first time you
fill one in a loop: if adding an element gives a new collection, what does the loop cost?

This post shows the answer, a builder: what it is, how to use one, what it doesn't let you do, and which builder goes
with which collection.

<!-- more -->

## The problem, simply

Say you read the lines of a CSV file into a `Vector` of orders. With a collection that never changes, the first idea
looks like this:

```java
record Order(String id, int amount) {
    static Order parse(String row) {
        var fields = row.split(",");
        return new Order(fields[0], Integer.parseInt(fields[1]));
    }
}

var rows   = java.util.List.of("o-1,30", "o-2,12", "o-3,45"); // the lines of a CSV file
var orders = Vector.<Order>empty();
for (var row : rows) {
    orders = orders.append(Order.parse(row));
}
```

It works, and the result is right. But each `append` gives a new `Vector`, and the loop drops the previous one at
once. A thousand rows make a thousand `Vector`s, and only the last one is kept. Each one shares most of its
structure with the one before, but each `append` still copies a part of it.

Keeping the old versions is useful when some code still holds them. Here, nobody does: the loop builds one
collection from scratch, and nobody looks at it before the end.

## A builder

A builder is made for this case. It fills the collection inside the builder, where no other code can see it. When you are done, `result()` hands over the finished collection, once.

```java
var builder = Vector.<Order>newBuilder(); // Vector.Builder<Order>
for (var row : rows) {
    builder.add(Order.parse(row));
}
var orders = builder.result(); // Vector<Order>
// Vector(Order[id=o-1, amount=30], Order[id=o-2, amount=12], Order[id=o-3, amount=45])
```

If you know `StringBuilder`, this is the same idea: you add the pieces to a buffer that changes, and `toString()`
gives you a `String` that doesn't.

The maps work the same way, with `put` instead of `add`. Here, the orders are indexed by their id:

```java
var index = HashMap.<String, Order>newBuilder(); // HashMap.Builder<String, Order>
for (var order : orders) {
    index.put(order.id(), order);
}
var byId  = index.result();   // HashMap<String, Order>
var found = byId.get("o-2");  // Option<Order>
// Some(Order[id=o-2, amount=12])
```

When two orders have the same id, the one put last wins, as with successive `put` calls.

### Builders come from Scala

Builders are not our idea. Scala's collections have them, and Zazr follows them. `Vector.Builder` is a port of
Scala's `VectorBuilder`, and the `TreeMap` and `TreeSet` builders build their tree in one pass, the way Scala's
`RedBlackTree` does. Both come from the Scala 2.13 collections library, which Scala 3 uses unchanged.

## What a builder doesn't let you do

A builder has few methods: `add` and `addAll` (`put` and `putAll` on the maps), `size()`, and `result()`. You can't
read an element back, remove one, or look up a key. Elements go in, and the collection comes out.

And a builder is used once. After `result()`, every method throws an `IllegalStateException`, a second `result()`
included:

```java
builder.add(new Order("o-4", 7)); // throws IllegalStateException
```

That is what keeps things safe. The builder is the only thing that could change the collection, and once it has
handed it over, it can't. So the `Vector` you got from `result()` is like any other `Vector`: it never changes, and
you can pass it to any code. To build another collection, create a new builder.

Two more rules. A builder is not thread-safe, so keep it in a local variable, used by one thread. And, like the
collections, it refuses `null`: adding `null` throws a `NullPointerException`.

## Without writing the loop

Often, you don't need to write the loop at all. Many factories that take all the elements at once fill a builder for
you, such as `Vector.ofAll`, `HashSet.ofAll` and `HashMap.ofEntries`.

When the elements come from a Java stream, each of these collections has a `collector()`. On the collections that have a
builder, the collector fills one:

```java
var large = rows.stream()
    .map(Order::parse)
    .filter(order -> order.amount() > 20)
    .collect(Vector.collector()); // Vector<Order>
// Vector(Order[id=o-1, amount=30], Order[id=o-3, amount=45])
```

## Which builder for which collection

Eight collections have a builder, and `newBuilder()` creates one:

| Collection | Builder | Good to know |
|---|---|---|
| `Vector` | `Vector.Builder` | fills the arrays of the `Vector` in place |
| `List` | `List.Builder` | makes one cell per element, from the last to the first |
| `HashMap` | `HashMap.Builder` | of equal keys, the one put last wins |
| `HashSet` | `HashSet.Builder` | of equal elements, the one added first stays |
| `LinkedHashMap` | `LinkedHashMap.Builder` | a key keeps the position where it was first put |
| `LinkedHashSet` | `LinkedHashSet.Builder` | an element keeps the position where it was first added |
| `TreeMap` | `TreeMap.Builder` | sorts once, at the first `size()` or `result()`; `newBuilder(comparator)` sets the order |
| `TreeSet` | `TreeSet.Builder` | sorts once, at the first `size()` or `result()`; `newBuilder(comparator)` sets the order |

`Queue` has no builder yet. Its `ofAll` factories take any `Iterable` or Java stream.

## A note on cost

Every collection says what its operations cost, on the [complexity page](../../collections/complexity.md). It shows
where a builder saves work:

- `Vector.append` copies the last array of the `Vector`, up to 32 elements, at every call. The builder writes each
  element once.
- `List.append` copies every element of the `List`, at every call. The builder makes one cell per element, once.
- A persistent `put` on a `HashMap` copies the path from the root to the entry. A `HashMap` or `HashSet` builder
  changes its own nodes in place.
- A `TreeMap` or `TreeSet` builder sorts the elements once and builds a balanced tree in one pass: O(m log m) for m
  elements, and O(m) when they come already sorted.

## What's next

The [Builders](../../builders.md) page covers every builder in more detail: `addAll` given a collection of the same
type, the comparator of the sorted builders, and the collectors. The [Roadmap](../../roadmap.md) lists what the next
releases bring.

If something is missing, unclear or broken, please
[open an issue](https://github.com/guizmaii-opensource/zazr/issues): feedback is very welcome.

Thank you for reading.
