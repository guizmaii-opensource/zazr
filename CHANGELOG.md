# Changelog

The notable changes of each Zazr release. The format follows [Keep a Changelog](https://keepachangelog.com). Zazr is
pre-1.0: the API can change between releases. From 1.0 it follows [Semantic Versioning](https://semver.org).

## 0.1.0 (unreleased)

The first release.

Zazr is a fork of the latest [Vavr](https://github.com/vavr-io/vavr), its `main` branch after 1.0.1, redesigned for
Java 25 and for the API style of ZIO, zio-prelude and modern Scala. It is not a drop-in replacement: the package is
`com.guizmaii.zazr`, the Maven group is `com.guizmaii`, and much of the API changed on purpose. The entries below
compare Zazr 0.1.0 with Vavr.

- Artifacts: `com.guizmaii:zazr-core` and `com.guizmaii:zazr-test`.
- JDK 25 or later. No runtime dependencies.
- The website, [zazr.dev](https://zazr.dev/), has a guide and a
  [Compared to Vavr](https://zazr.dev/vavr/) page with the full list of renamed operations.

### Added

**Control types**

- `Validation` keeps every error: its error side is a `NonEmptyVector<E>`, and `zip` accumulates errors without
  changing the type. `collectAll`, `forEach` and `partition` validate a whole collection; `fromPredicate` builds a
  check whose error can name the rejected value; `flatMapEither` chains a step that returns an `Either`.
- `zip`, `zipWith`, `zipLeft` and `zipRight` on `Option`, `Either`, `Try`, `Validation` and `Lazy`.
- Static `zip` and `zipWith` taking 2 to 8 values at once, such as
  `Validation.zipWith(name, age, email, User::new)`, with no nested tuples.
- Static `flatten` on every control type, such as `Option.flatten(Option<Option<A>>)`.
- `Try.fromCompletableFuture` and `Try.toCompletableFuture`.

**`Using`**

- `Using.of(resource, block)` runs a block with a resource and returns a `Try`.
- `Using.manager(block)` handles any number of resources, known only at run time. Resources are released in reverse
  order, each once, even when an acquisition fails.
- A value that is not an `AutoCloseable`, such as a held lock, is registered with its own release action.
- An error thrown while releasing is never lost: the more severe one surfaces and the other is attached to it as
  suppressed.

**`NonEmptyVector`**

- A new collection that always holds at least one element, with the whole `Vector` API.
- `head`, `last`, `max`, `min` and `reduce` cannot fail, since there is no empty case.
- Operations that keep at least one element (`map`, `append`, `sorted`...) return a `NonEmptyVector`. Operations
  that can remove every element (`filter`, `tail`, `take`...) return a `Vector`.
- `Vector.toNonEmptyVector()` and `NonEmptyVector.fromIterable(...)` narrow to it, returning an `Option`.

**Collections**

- `partitionMap` on the sequences and the hash sets: one pass, with a function that returns an `Either`.
- `duplicates` and `duplicatesBy` on the sequences: the elements that occur more than once.
- Static `flatten` on every collection except the maps.
- Positional operations (`head`, `last`, `take`, `drop`, `sliding`...) on the ordered sets and maps:
  `LinkedHashSet` and `LinkedHashMap` in insertion order, `TreeSet` and `TreeMap` in sorted order.
- Every operation whose cost depends on the size documents it in its javadoc. The
  [complexity page](https://zazr.dev/collections/complexity/) lists them all.

**Builders**

- `Vector.Builder`, `HashMap.Builder`, `HashSet.Builder`, `TreeMap.Builder` and `TreeSet.Builder`: fill a
  collection in place, then get it once with `result()`. Building in a loop no longer copies the collection at each
  step.
- `ofAll`, `ofEntries` and `collector()` of these five types use their builder.

**Java interop**

- `asJava()` on every collection: a read-only `java.util` view made in constant time, with no copy. The sequences
  give a `java.util.List`, `HashSet` a `Set`, `LinkedHashSet` a `SequencedSet` and `TreeSet` a `NavigableSet`.
- `asJavaMap()` on the maps: a `java.util.Map`, a `SequencedMap` for `LinkedHashMap`, a `NavigableMap` for
  `TreeMap`.
- `ofAll` given such a view returns the original collection without copying.

**`zazr-test`**

- A new artifact for property-based testing in any test framework, such as JUnit: `Property`, `Gen`, `Arbitrary`
  and a `CheckResult` you can pattern match on (`Satisfied`, `Falsified`, `Erroneous`).
- Ready-made arbitraries for every Zazr type, including unusual shapes: vectors built by dropping a prefix, sets and
  maps that went through removals, streams with an unevaluated tail.
- Law suites that check the equality, `map`, `flatMap`, `zip`, builder and collection rules of your own types, and
  name the law that failed.

**Documentation**

- The website [zazr.dev](https://zazr.dev/): getting started, a page per control type and per collection, the
  complexity page, `zip`, builders, Java interop, testing, a page for readers new to functional programming, and the
  comparison with Vavr.

### Changed

**Whole library**

- Java 25 is the minimum. The module is `com.guizmaii.zazr` and exports `com.guizmaii.zazr`,
  `com.guizmaii.zazr.collection` and `com.guizmaii.zazr.control`.
- No `null` inside. `Some`, `Right`, `Success`, `Valid` and every collection reject it with a
  `NullPointerException`. `Option.some(null)` throws; use `Option.ofNullable`.
- Operations use the JDK's functional interfaces: `Supplier`, `Function`, `BiFunction`, `Callable`. `Function3` to
  `Function8` and `CheckedFunction1` to `CheckedFunction8` remain, since the JDK has nothing at those arities or with
  checked exceptions.
- Names say what an operation does, following ZIO: `ap` becomes `zip`/`zipWith`, `sequence` becomes `collectAll`,
  `traverse` becomes the static `forEach`, `bimap` becomes `mapBoth`, `peek` becomes `tap`, `mapTo` becomes `as`.
  The [Compared to Vavr](https://zazr.dev/vavr/) page has the full table.
- The javadoc is written in Markdown.

**Control types**

- `Option`, `Either`, `Try` and `Validation` are sealed interfaces of records (`Some`/`None`, `Left`/`Right`,
  `Success`/`Failure`, `Valid`/`Invalid`), so pattern matching on them is checked by the compiler.
- They are no longer `Iterable`, and they no longer share a common supertype with the collections. Each has a short
  set of conversions: `toOption()`, `toEither()`, `toTry(...)`, `toValidation(...)`, `toVector()`...
- `Try`: `recover` and `recoverWith` become `catchAll`, `catchSome`, `catchAllWith` and `catchSomeWith`;
  `mapFailure` becomes `mapError`; `andFinally` becomes `ensuring`; `onFailure` becomes `tapError`. `Try.of` takes a
  `Callable`.
- Two `Try.Failure`s are equal when they hold the same `Throwable` instance, not when their stack traces match.
- `Either`: `swap` becomes `flip`, `peekLeft` becomes `tapLeft`, and `Either.collectAll` stops at the first `Left`.
  To collect every error, use `Validation`.
- `Validation.cond` and `Either.cond` become `fromPredicate`.

**Collections**

- Each collection is a concrete type that declares its own API and returns its own type: `Vector.map` returns a
  `Vector`, `List.filter` a `List`. The only shared interface is `Traversable`, holding what every collection does
  at the same cost (`size`, `contains`, `exists`, `foldLeft`, `mkString`, `stream()`...).
- `List` is a sealed interface of two records, `Cons` and `Nil`, so you can pattern match on its head and tail.
- Tuples are records, so `case Tuple2(var a, var b)` works.
- Sets and maps have no positional methods (`head`, `take`, `sliding`...), except the ordered ones listed above.
- `grouped`, `sliding`, `slideBy` and `crossProduct` return the collection's own type, such as
  `Vector<Vector<T>>`, instead of an `Iterator`.
- `tap` on a collection runs the action on every element. Vavr's `peek` ran it on the first one only.
- `iterator()` returns a `java.util.Iterator`.
- `Map.values()` returns a `Vector`.
- `toJavaArray` becomes `toArray()` and `toArray(IntFunction)`, as on the JDK's collections.
- `Tuple.sequence1` to `sequence8` become `unzip1` to `unzip8`, and `Tuple.toSeq()` becomes `toVector()`.
- Every method that takes an `Iterable` reads it once, so a one-shot `Iterable` works everywhere.
- `Vector.takeRight` and `dropRight` no longer overflow at `Integer.MIN_VALUE`, and `rotateLeft` and `rotateRight`
  on `List`, `Queue` and `Stream` no longer overflow the stack there.
- `TreeSet` and `TreeMap` stay balanced after a difference or an intersection.
- `Stream.slice` no longer overflows the stack on a long `Stream`.

### Removed

**Control types**

- The `Match` API (`Match`, `Case`, `$`, the `Patterns`) and the `For` comprehensions. Use pattern matching with
  record patterns and `when` guards, and `zip`/`zipWith` to combine values.
- `Future`, `Promise` and `Task`. Use `CompletableFuture` and virtual threads; `Try` converts to and from a
  `CompletableFuture`.
- `Value`, the common supertype of the control types and the collections, with its conversions and its printing
  methods.
- The `Either` projections (`left()`, `right()`). Use `mapLeft`, `flip` and `fold`.
- `Validation.Builder`, `combine` and `ap`. Use `zip` and `zipWith`.
- `Try.withResources`. Use `Using`.
- `Lazy.val` and `Lazy.filter`.
- `PartialFunction` and `collect(PartialFunction)`. Use `collect(Function<A, Option<B>>)`, with pattern matching
  inside the lambda.

**Collections**

- `Array` and `CharSeq`. Use `Vector`, `String` or `Vector<Character>`.
- `Tree`, `BitSet`, `PriorityQueue` and the `Multimap` family. For a multimap, use `Map<K, Vector<V>>` with
  `groupBy`.
- `Seq`, `IndexedSeq`, `LinearSeq` and `Foldable`. Use the concrete type, or `Traversable`.
- The public `Iterator` type. `iterator()` returns a `java.util.Iterator`.
- The `toJava*` copies (`toJavaList`, `toJavaSet`, `toJavaMap`...), `asJavaMutable` and `asJava(Consumer)`. Use
  `asJava()` or `asJavaMap()`; a copy is `new ArrayList<>(vector.asJava())`.
- `reverseIterator()` and `iterator(int)`. Use `reverse().iterator()` and `drop(n).iterator()`.

**Whole library**

- `Function0`, `Function1`, `Function2` and `CheckedFunction0`. Use `Supplier`, `Function`, `BiFunction` and
  `Callable`.
- `Serializable`: no Zazr type implements it.
- The flags that described a type rather than a value: `isAsync`, `isLazy`, `isSingleValued`, `isOrdered`,
  `isDistinct`...

<!--
pending: open pull requests, to fold into 0.1.0 as they merge.

- PR 124 (cost fixes found by the complexity review): user-visible. List `take`, `drop`, `takeWhile`, `slice`,
  `subSequence`, `remove`, `leftPadTo` and `combinations(k)` no longer walk the whole List; Queue `startsWith`,
  `zip`, `zipWith`, `prefixLength` and `segmentLength` no longer reverse the rear list first. One line under
  Changed > Collections, such as "Several List and Queue operations walk only the elements they need".
- PR 128 (Scala's radix-balanced vector as an internal structure): no entry; nothing changes for users until Vector
  switches to it. That later change gets an entry (cheaper prepend, tail and init).
- PR 131 (docs examples with var), PR 134 (coverage threshold), PR 135 (incremental generator): no entry; docs and
  build only.

Open 0.1.0 issues that change this file if they land before the release:
- Stream renamed LazyList: Changed > Collections.
- zazr-test with one Gen and no Arbitrary: rewrite the zazr-test entry under Added.
- HashMap and HashSet on CHAMP: Changed > Collections, if the behaviour or the iteration order changes.
- NonEmptySet and NonEmptyMap: Added.
- zipWithPrevious, zipWithNext, mapAccum, foldWhile, collectWhile, splitWhere, dedupe: Added > Collections.
- Builders for LinkedHashMap, LinkedHashSet and List: Added > Builders.
- Functions returning null rejected everywhere: Changed > Whole library.
- LinkedHashMap keeps a repeated key's first position in every factory: Changed > Collections.
- length removed in favour of size on the sequences: Changed > Collections.
-->
