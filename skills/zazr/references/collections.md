# Collections

Every Zazr collection (in `com.guizmaii.zazr.collection`) is immutable and persistent: an operation returns a new
collection that shares what it can with the old one, which never changes. Full pages:
https://zazr.dev/collections/, every cost: https://zazr.dev/collections/complexity/.

## Which one to choose

| The code needs | Choose | Because |
|---|---|---|
| a sequence, by default | `Vector` | effectively O(1) `get`, `update`, `append`, `prepend`, `take`, `drop` |
| a sequence with at least one element | `NonEmptyVector` | `head`, `max`, `reduce` cannot fail |
| to take a sequence apart from the front, a stack | `List` | O(1) `prepend`, `head`, `tail`; pattern matching on `Cons` and `Nil` |
| first in, first out | `Queue` | amortised O(1) `enqueue` and `dequeue` |
| a sequence computed on demand, maybe infinite | `Stream` | lazy and memoised |
| a set, by default | `HashSet` | effectively O(1) `contains`, `add`, `remove` |
| a set in insertion order | `LinkedHashSet` | a `HashSet` plus the order, with positional methods |
| a sorted set | `TreeSet` | O(log n) lookups and updates, positional methods in comparator order |
| a map, by default | `HashMap` | effectively O(1) `get`, `put`, `remove` |
| a map in insertion order | `LinkedHashMap` | a `HashMap` plus the order; overwriting a key keeps its position |
| a sorted map | `TreeMap` | O(log n) lookups and updates, positional methods in key order |

"Effectively O(1)" is a walk down a tree of 32-wide nodes, at most a handful of levels deep at any size.

## Costs that decide the choice

| Operation | `Vector` | `List` | `Queue` | `Stream` |
|---|---|---|---|---|
| `head`, `prepend` | effectively O(1) | O(1) | O(1) | O(1) |
| `tail` | effectively O(1) | O(1) | amortised O(1) | O(1) |
| `append` | effectively O(1) | O(n) | amortised O(1) | O(1), lazy |
| `get(i)`, `update(i, v)` | effectively O(1) | O(i) | O(i) to O(n) | O(i) |
| `last`, `init` | effectively O(1) | O(n) | O(n) / amortised O(1) | O(n) / lazy |
| `take`, `drop` | effectively O(1) | O(k) | O(n) | lazy / O(k) |
| `length()` | O(1) | O(n) | O(n) | O(n), forces all |

| Operation | `HashSet` / `HashMap` | `LinkedHashSet` / `LinkedHashMap` | `TreeSet` / `TreeMap` |
|---|---|---|---|
| `contains`, `get`, `containsKey` | effectively O(1) | effectively O(1) | O(log n) |
| `add`, `put`, `remove` | effectively O(1) | effectively O(1), `remove` amortised | O(log n) |
| `head`, `take`, `drop` | none | yes | O(log n) |

`NonEmptyVector` has `Vector`'s costs. `contains` on a sequence is O(n); use a set for membership.

## What every collection shares

Every collection except `NonEmptyVector` implements `Traversable<T>`: iteration, `size()`, `isEmpty()`,
`contains`, `exists`, `forAll`, `count`, `find` (an `Option`), `foldLeft`, `mkString`, `toVector`, `toList`,
`toSet`, `stream()`, `toArray`, `asJava()`.

`map`, `filter`, `flatMap` and the rest are declared by each type and return that type: `grouped` on a `List` is a
`List` of `List`s. `partitionMap` splits in one pass with a function returning an `Either` (sequences and hash
sets). `groupBy` returns a `Map` of groups. The static `flatten` removes one level of nesting.

```java
var split = List.of(1, 2, 3, 4) // Tuple2<List<Integer>, List<String>>
    .partitionMap(n -> n % 2 == 0 ? Either.left(n) : Either.right("odd " + n));
var stock = HashMap.of("apple", 3, "pear", 0) // HashMap<String, Integer>
    .put("pear", 5, Integer::sum)
    .put("fig", 1);
var pears = stock.get("pear"); // Option<Integer>
var kiwis = stock.getOrElse("kiwi", 0); // Integer
// split is (List(2, 4), List(odd 1, odd 3)), pears is Some(5), kiwis is 0
```

A map is a collection of `Tuple2<K, V>` entries. `get` returns an `Option`; `map`, `filter` and `forEach` on a
map take a function of the key and the value; `mapValues` and `filterKeys` work on one side; `keySet()` gives a set
and `values()` a `Vector`.

## Build in bulk

A loop of `append` or `put` copies part of the structure on every call. Build once:

- `Vector.newBuilder()`, and the builders of `HashMap`, `HashSet`, `TreeMap` and `TreeSet` (maps use `put` and
  `putAll`). A builder is mutable, single-use and not thread-safe: after `result()` it throws.
- `collector()` on every collection, for `java.util.stream.Stream.collect`.
- `ofAll(iterable)` or `ofAll(javaStream)`; `Vector.range`, `Vector.ofAll(int...)` store primitives unboxed.
- `map`, `flatMap`, `collect`, `filter` on an existing collection.

```java
var builder = Vector.<String>newBuilder(); // Vector.Builder<String>
for (var word : "the quick brown fox".split(" ")) {
    builder.add(word.toUpperCase());
}
var words = builder.result(); // Vector<String>
var sorted = java.util.stream.Stream.of("b", "a", "b").collect(TreeSet.collector()); // TreeSet<String>
// Vector(THE, QUICK, BROWN, FOX), TreeSet(a, b)
```

`List`, `Queue`, `LinkedHashMap` and `LinkedHashSet` have no builder; use `ofAll` or `collector()`.

## `NonEmptyVector`

A sequence with at least one element, backed by a `Vector`. Parse into it instead of checking emptiness.

- Build: `NonEmptyVector.of(head, rest...)`, `single(a)`, `fromIterable(head, tail)`; from something that may be
  empty, `vector.toNonEmptyVector()`, `NonEmptyVector.fromVector(v)` or `fromIterable(it)`, all returning an
  `Option`. `unsafeFromVector` throws on an empty `Vector`.
- Total: `head`, `last`, `max(comparator)`, `min`, `reduce`, `average` return the value, not an `Option`.
- The return type says whether the result can be empty: `map`, `append`, `sorted`, `distinct`, `zip` return a
  `NonEmptyVector`; `filter`, `tail`, `take`, `drop` return a `Vector`; `tailNonEmpty()` returns an `Option`.
- `flatMap` takes a function returning a `NonEmptyVector`; `flatMapAll` takes any `Iterable` and returns a
  `Vector`.
- It has `size()` but no `length()`, no `isEmpty()` and no `headOption()`.
- It is `Iterable` but not a `Traversable`, and it is not equal to a `Vector` with the same elements; use
  `toVector()`.

```java
var input = Vector.of("ada@shop.com", "grace@shop.com");
var recipients = input.toNonEmptyVector().toEither(() -> "at least one recipient is required"); // Either<String, NonEmptyVector<String>>
var first = recipients.map(NonEmptyVector::head).getOrElse("nobody"); // String
var scores = NonEmptyVector.of(7, 3, 9);
var best = scores.max(Integer::compare); // int, nothing can go wrong
var passed = scores.filter(s -> s > 5); // Vector<Integer>, may be empty
// first is "ada@shop.com", best is 9, passed is Vector(7, 9)
```

## Java interop

Zazr collections do not implement `java.util.List`, `Set` or `Map`. Cross with views and factories:

- `asJava()` gives a read-only view in O(1): a `java.util.List` for sequences, a `Set` for `HashSet`, a
  `SequencedSet` for `LinkedHashSet`, a `NavigableSet` for `TreeSet`. Maps: `asJavaMap()` gives a `java.util.Map`,
  `SequencedMap` or `NavigableMap`; `asJava()` on a map is a collection of its `Tuple2` entries.
- Every mutator of a view throws `UnsupportedOperationException`. When a JDK API must modify it, copy:
  `new java.util.ArrayList<>(vector.asJava())`.
- Back: `ofAll(iterable)`, `ofAll(javaStream)`, `collector()`. `stream()` gives a `java.util.stream.Stream`.
- `Option.ofOptional` and `toOptional()`; `Try.fromCompletableFuture` and `toCompletableFuture()`.

```java
var names = Vector.of("Ada", "Grace");
var view = names.asJava(); // java.util.List<String>, no copy
var back = Vector.ofAll(view); // Vector<String>, the same instance
var fromJdk = Vector.ofAll(java.util.List.of(3, 1, 2)); // Vector<Integer>
// view.get(1) is "Grace", back == names, fromJdk is Vector(3, 1, 2)
```

`List` and `Stream` clash with `java.util.List` and `java.util.stream.Stream`: import Zazr's, spell the JDK ones out.

## Sharp edges

- No collection holds `null`: a `null` element, key or value throws a `NullPointerException`.
- `HashSet` and `HashMap` have no order: no `head`, `take`, `zipWithIndex`, `sliding`. Do not rely on their
  iteration order; `fold` over them needs an operation where order does not matter.
- `max()` and `min()` on a set walk every element in natural order, even on a `TreeSet`; its own least and greatest
  are `head()` and `last()`.
- `TreeSet` and `TreeMap` decide membership with the comparator, not `equals`.
- `Stream`: the first element is computed when the `Stream` is built; `size`, `length`, `last`, `reverse`,
  `sorted`, `foldLeft`, `mkString` and `toVector` never return on an infinite one; it keeps every element it
  computed.
- `Queue`'s amortised cost holds only when each `dequeue` works on the queue the previous one returned.
  `dequeue()` on an empty queue throws; `dequeueOption()` returns an `Option`.
- `List.length()` and `List.size()` are O(n).
- `sliding`, `grouped` and `crossProduct` return a collection of the receiver's type, not an iterator.
- `tap` on a collection runs on every element.
- Equality: a `Vector`, `List`, `Queue` or `Stream` equals another of these four with the same elements in the same
  order; sets equal sets and maps equal maps; a `NonEmptyVector` equals only a `NonEmptyVector`.
