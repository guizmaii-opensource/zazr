# Collections

Every Zazr collection (in `dev.zazr.collection`) is immutable and persistent: an operation returns a new
collection that shares what it can with the old one, which never changes. Full pages:
https://zazr.dev/collections/, every cost: https://zazr.dev/collections/complexity/.

## Which one to choose

| The code needs | Choose | Because |
|---|---|---|
| a sequence, by default | `Vector` | effectively O(1) `get`, `update`, `append`, `prepend`, `take`, `drop` |
| a sequence with at least one element | `NonEmptyVector` | `head`, `max`, `reduce` cannot fail |
| a set or map with at least one element | `NonEmptySet`, `NonEmptyMap` and their `Sorted` variants | `max`, `reduce` (and `head` when sorted) cannot fail |
| to take a sequence apart from the front, a stack | `List` | O(1) `prepend`, `head`, `tail`; pattern matching on `Cons` and `Nil` |
| first in, first out | `Queue` | O(1) `enqueue`, amortised O(1) `dequeue` |
| a sequence computed on demand, maybe infinite | `LazyList` | lazy and memoised |
| a set, by default | `HashSet` | effectively O(1) `contains`, `add`, `remove` |
| a set in insertion order | `LinkedHashSet` | a `HashSet` plus the order, with positional methods |
| a sorted set | `TreeSet` | O(log n) lookups and updates, positional methods in comparator order |
| a map, by default | `HashMap` | effectively O(1) `get`, `put`, `remove` |
| a map in insertion order | `LinkedHashMap` | a `HashMap` plus the order; overwriting a key keeps its position |
| a sorted map | `TreeMap` | O(log n) lookups and updates, positional methods in key order |

"Effectively O(1)" is a walk down a tree of 32-wide nodes, at most a handful of levels deep at any size.

## Costs that decide the choice

| Operation | `Vector` | `List` | `Queue` | `LazyList` |
|---|---|---|---|---|
| `head`, `prepend` | effectively O(1) | O(1) | O(1) | O(1) |
| `tail` | effectively O(1) | O(1) | amortised O(1) | O(1) |
| `append` | effectively O(1) | O(n) | O(1) | O(1), lazy |
| `get(i)` | effectively O(1) | O(i) | O(i) to O(n) | O(i) |
| `update(i, v)` | effectively O(1) | O(i) | O(n) | lazy |
| `last`, `init` | effectively O(1) | O(n) | O(n) / amortised O(1) | O(n) / lazy |
| `take`, `drop` | effectively O(1) | O(k) | O(n) | lazy |
| `size()` | O(1) | O(n) | O(n) | O(n), forces all |

| Operation | `HashSet` / `HashMap` | `LinkedHashSet` / `LinkedHashMap` | `TreeSet` / `TreeMap` |
|---|---|---|---|
| `contains`, `get`, `containsKey` | effectively O(1) | effectively O(1) | O(log n) |
| `add`, `put`, `remove` | effectively O(1) | effectively O(1), `remove` amortised | O(log n) |
| `head`, `take`, `drop` | none | yes | O(log n) |
| `rangeFrom`, `rangeFromUntil`, `minAfter` | none | none | O(log n) |

`NonEmptyVector` has `Vector`'s costs. `contains` on a sequence is O(n); use a set for membership.

## What every collection shares

Every collection except the non-empty ones (`NonEmptyVector`, `NonEmptySet`, `NonEmptyMap` and their `Sorted` variants)
implements `Traversable<T>`: iteration, `size()`, `sizeCompare(n)` (counts at most n + 1 elements, so it returns on an
infinite `LazyList`), `isEmpty()`,
`contains`, `exists`, `forAll`, `count`, `find` (an `Option`), `foldLeft`, `mkString`, `toVector`, `toList`,
`toSet`, `stream()`, `toArray`, `asJava()`.

`map`, `filter`, `flatMap` and the rest are declared by each type and return that type: `grouped` on a `List` is a
`List` of `List`s. `partitionMap` splits in one pass with a function returning an `Either` (sequences and hash
sets). `groupBy` returns a `Map` of groups, keyed in the order of their first element; `groupMap(key, value)` maps the
elements as it groups them, and `groupMapReduce(key, value, reduce)` combines each group into one value. A map groups
the values of its entries in a `Vector`, a `TreeSet` in a `HashSet`, and a non-empty collection returns a
`NonEmptyMap`. The static `flatten` removes one level of nesting.

```java
var words  = List.of("apple", "bob", "avocado");
var byChar = words.groupMap(w -> w.charAt(0), String::length);              // Map<Character, List<Integer>>
var counts = words.groupMapReduce(w -> w.charAt(0), w -> 1, Integer::sum);  // Map<Character, Integer>
var big    = List.range(0, 1_000_000).sizeCompare(3) > 0;                   // boolean
// byChar is LinkedHashMap((a, List(5, 7)), (b, List(3))), counts is LinkedHashMap((a, 2), (b, 1)), big is true
```

```java
var split = List.of(1, 2, 3, 4) // Tuple2<List<Integer>, List<String>>
    .partitionMap(n -> n % 2 == 0 ? Either.left(n) : Either.right("odd " + n));
var stock = HashMap.of("apple", 3, "pear", 0) // HashMap<String, Integer>
    .put("pear", 5, Integer::sum)
    .put("fig", 1);
var pears = stock.get("pear");           // Option<Integer>
var kiwis = stock.getOrElse("kiwi", 0);  // Integer
// split is (List(2, 4), List(odd 1, odd 3)), pears is Some(5), kiwis is 0
```

Sums, products and averages take the function that reads the number from an element, and its type picks the
arithmetic: `sumInt`, `sumLong`, `sumDouble`, `productInt`, `productLong`, `productDouble` return the primitive,
`average` an `Option<Double>` (a `double` on `NonEmptyVector`, `NonEmptySet` and `NonEmptySortedSet`). The `int` and
`long` forms throw an `ArithmeticException` when the result does not fit; there is no untyped `sum()`.

```java
var words   = List.of("one", "three", "five");
var letters = words.sumInt(String::length);                     // int
var mean    = words.average(String::length);                    // Option<Double>
var big     = Vector.of(Integer.MAX_VALUE, 1).sumLong(n -> n);  // long
// 12, Some(4.0), 2147483648
```

A map is a collection of `Tuple2<K, V>` entries. `get` returns an `Option`; `map`, `filter` and `forEach` on a
map take a function of the key and the value; `mapValues` and `filterKeys` work on one side; `keySet()` gives a set
and `values()` a `Vector`.
`putAll(entries)` puts many entries as successive `put`s would, the argument's value winning on a shared key (Scala's
`++`); `merge(that)` keeps this map's value and adds only the keys it lacks.

`TreeSet` and `TreeMap` (and `NonEmptySortedSet`, `NonEmptySortedMap`, which return the plain types) cut ranges by
element or key in O(log n), sharing the rest of the tree: `rangeFrom(from)` (inclusive), `rangeUntil(until)`
(exclusive), `rangeTo(to)` (inclusive) and `rangeFromUntil(from, until)`, empty when `from` is not before `until`
(there is no instance `range`: `TreeSet.range(int, int)` is a static factory). `minAfter(x)` is the least at or after
`x` (inclusive), `maxBefore(x)` the greatest strictly before `x`, both an `Option` (of a `Tuple2` on a map);
`iteratorFrom(start)` iterates from `start` on.

```java
var scores = TreeSet.of(10, 20, 30, 40);
var middle = scores.rangeFromUntil(20, 40);                    // TreeSet<Integer>
var next   = scores.minAfter(20);                              // Option<Integer>
var before = scores.maxBefore(20);                             // Option<Integer>
var later  = TreeMap.of(1, "a", 2, "b", 3, "c").rangeFrom(2);  // TreeMap<Integer, String>
// middle is TreeSet(20, 30), next is Some(20), before is Some(10), later is TreeMap((2, b), (3, c))
```

## Build in bulk

A loop of `append` or `put` copies part of the structure on every call. Build once:

- `Vector.newBuilder()`, and the builders of `List`, `HashMap`, `HashSet`, `LinkedHashMap`, `LinkedHashSet`,
  `TreeMap` and `TreeSet` (maps use `put` and `putAll`). A builder is mutable, single-use and not thread-safe: after `result()` it throws.
- `collector()` on every collection, for `java.util.stream.Stream.collect`.
- `ofAll(iterable)` or `ofAll(javaStream)`; `Vector.range` and `Vector.ofAll(int...)` box their elements, like every other factory.
- `map`, `flatMap`, `collect`, `filter` on an existing collection.

```java
var builder = Vector.<String>newBuilder(); // Vector.Builder<String>
for (var word : "the quick brown fox".split(" ")) {
    builder.add(word.toUpperCase());
}
var words  = builder.result();                                                        // Vector<String>
var sorted = java.util.stream.Stream.of("b", "a", "b").collect(TreeSet.collector());  // TreeSet<String>
// Vector(THE, QUICK, BROWN, FOX), TreeSet(a, b)
```

`Queue` has no builder; use `ofAll` or `collector()`.

## `NonEmptyVector`

A sequence with at least one element, backed by a `Vector`. Parse into it instead of checking emptiness.

- Build: `NonEmptyVector.of(head, rest...)`, `single(a)`, `fromIterable(head, tail)`; from something that may be
  empty, `vector.toNonEmptyVector()`, `NonEmptyVector.fromVector(v)` or `fromIterable(it)`, all returning an
  `Option`. `unsafeFromVector` throws on an empty `Vector`.
- Total: `head`, `last`, `max(comparator)`, `min`, `reduce`, `average(mapper)` return the value, not an `Option`.
- The return type says whether the result can be empty: `map`, `append`, `sorted`, `distinct` return a
  `NonEmptyVector`; `filter`, `tail`, `take`, `drop` return a `Vector`; `tailNonEmpty()` returns an `Option`.
- `zip` and `crossProduct` return a `NonEmptyVector` when given a `NonEmptyVector`, and a `Vector` when given any
  other `Iterable`. `crossProduct()` returns a `NonEmptyVector`; `crossProduct(int)` and `zipWith(Iterable, f)` a
  `Vector`.
- `flatMap` takes a function returning a `NonEmptyVector`; `flatMapAll` takes any `Iterable` and returns a
  `Vector`.
- It has `size()` but no `isEmpty()` and no `headOption()`.
- It is `Iterable` but not a `Traversable`, and it is not equal to a `Vector` with the same elements; use
  `toVector()`.

```java
var input = Vector.of("ada@shop.com", "grace@shop.com");
// Either<String, NonEmptyVector<String>>
var recipients = input.toNonEmptyVector().toEither(() -> "at least one recipient is required");
var first      = recipients.map(NonEmptyVector::head).getOrElse("nobody");  // String
var scores     = NonEmptyVector.of(7, 3, 9);
var best       = scores.max(Integer::compare);                              // Integer, nothing can go wrong
var passed     = scores.filter(s -> s > 5);                                 // Vector<Integer>, may be empty
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
- JSON: the optional `dev.zazr:zazr-jackson` artifact; register `new ZazrModule()` on a Jackson 3 mapper builder.
  Full page: https://zazr.dev/jackson/.

```java
var names   = Vector.of("Ada", "Grace");
var view    = names.asJava();                            // java.util.List<String>, no copy
var back    = Vector.ofAll(view);                        // Vector<String>, the same instance
var fromJdk = Vector.ofAll(java.util.List.of(3, 1, 2));  // Vector<Integer>
// view.get(1) is "Grace", back == names, fromJdk is Vector(3, 1, 2)
```

`List` clashes with `java.util.List`: import Zazr's, spell the JDK one out.

## Sharp edges

- No collection holds `null`: a `null` element, key or value throws a `NullPointerException`.
- `HashSet` and `HashMap` have no order: no `head`, `take`, `zipWithIndex`, `sliding`. Do not rely on their
  iteration order; `fold` over them needs an operation where order does not matter.
- `max()` and `min()` on a set walk every element in natural order, even on a `TreeSet`; its own least and greatest
  are `head()` and `last()`.
- `TreeSet` and `TreeMap` decide membership with the comparator, not `equals`.
- `LazyList`: nothing is computed before it is read, not even the first element (`LazyList.defer(() -> ...)` for a
  lazy first element too); `size`, `last`, `reverse`, `sorted`, `foldLeft`, `mkString` and `toVector` never return
  on an infinite one; it keeps every element it computed, and every exception.
- `Queue`'s amortised cost holds only when each `dequeue` works on the queue the previous one returned.
  `dequeue()` on an empty queue throws; `dequeueOption()` returns an `Option`.
- `List.size()` and `Queue.size()` are O(n); `isEmpty()` is O(1).
- `grouped`, `sliding` and `crossProduct` return a collection, not an iterator. On `Vector`, `List`, `Queue` and
  `LazyList` it is of the receiver's type (`List<List<T>>`). On `LinkedHashSet`, `TreeSet`, `LinkedHashMap`, `TreeMap`
  and `NonEmptyVector`, `grouped` and `sliding` return a `Vector` of the receiver's type (`Vector<TreeSet<T>>`).
- `tap` on a collection runs on every element.
- Equality: a `Vector`, `List`, `Queue` or `LazyList` equals another of these four with the same elements in the same
  order; sets equal sets and maps equal maps; a `NonEmptyVector` equals only a `NonEmptyVector`.
