# Zazr design: what changes from Vavr, and why

Zazr is a fork of the latest Vavr, its `main` branch after the 1.0.1 release (Maven, single `vavr` module at fork time; now JDK 25+, becoming a mono-repo). Its goal is to bring the
API-design lessons of ZIO and zio-prelude to a Java FP library: plain-English names, one way to do each
thing, non-empty types that make partial operations total, error accumulation that is the default rather
than a bolt-on, and collections whose interfaces don't promise more than their implementations can deliver.

This document is the analysis of the three code bases and the resulting list of changes. It is written as
a proposal: sections marked **Decision** are recommended defaults, sections marked **Open** need your call.

Sources analysed (local clones): `zazr/` (Vavr master), `zio/` (2.1.x), `zio-prelude/`, and the Scala
2.13.16 standard library sources (for `VectorBuilder`).

---

## 1. Where Vavr stood when Zazr forked it

Facts that shape every decision below (all verified in the fork):

| Fact | Where |
|---|---|
| Java 21 toolchain (now 25 in the fork), but **no** `sealed`, no `record` (one private helper aside), no pattern `switch`. The only Java 21 feature used is `Executors.newVirtualThreadPerTaskExecutor()`. | `concurrent/Future.java:64` |
| `Value<T> extends Iterable<T>` is the root of everything: `Option`, `Either`, `Try`, `Validation`, `Future`, `Lazy`, both `Either` projections, and all collections. It declares **85 members**, 52 of which are `toXxx` conversions (9 already deprecated), plus `stdout()`, `stderr()`, `out(PrintStream)`, `spliterator()`, `eq(Object)`, `stringPrefix()`. | `Value.java:96` |
| `Seq` declares 137 members; `IndexedSeq` (105) and `LinearSeq` (92) are almost entirely covariant-return boilerplate. `Seq extends PartialFunction<Integer,T>`; `Set extends Function1<T,Boolean>`; `Map extends PartialFunction<K,V>`. | `collection/Seq.java:56`, `Set.java:43`, `Map.java:41` |
| Complexity is not documented: 13 hits for `O(1)|O(n)|O(log|amortized` in all of `src/main`, zero in `IndexedSeq`/`LinearSeq`. | grep |
| `Validation<E,T>` holds a **single** `E` on the invalid side. Accumulation happens only through `ap`, whose return type changes from `Validation<E,T>` to `Validation<Seq<E>,U>`, so `ap` cannot be chained. The `combine(...)` → `Builder..Builder8` → `ap(FunctionN)` ladder works around that, capped at arity 8. `Validation.sequence` requires callers to be *already* in `Seq<E>`. | `control/Validation.java:108, 641, 256-415, 152` |
| The Match API was **not** removed: `API.java` is 11,655 generated lines (`Match`, `Case0..8`, `$()`, 42 `ForLazyN*` classes, `For(` ×99). `$.java` holds 19 `@Unapply` patterns processed by the external `vavr-match-processor`. | `src-gen/main/java/io/vavr/API.java:10519`, `src/main/java/io/vavr/$.java` |
| Category-theory words are absent from method names (no `pure`, `bind`, `unit`, `point`) but present in the **javadoc and the generator**: "monadic container type" (`Option.java:41`), "behave like a monad over its `Right` type" (`Either.java:43`), "an applicative functor, not a Monad" + "the applicative functor's ap operation" (`Validation.java:40-41, 634`), "more like a *Functor* than a *Monad*" (`Lazy.java:44`), `// -- Value & Monad implementation` (`Future.java:1081`, `Either.java:433`), and `monadicTypesFor` / "For-comprehension" throughout `Generator.scala:811-3324`. The structural jargon in names is `ap`, `sequence`, `traverse`, `bimap`, `Builder.ap`. | grep |
| `Vector` wraps a `BitMappedTrie` (offset + depthShift, leaves typed at runtime by a generated `ArrayType` with 8 primitive specialisations selected by catching `ClassCastException`). There is no builder. `append(x)` is `appendAll(List.of(x))`. `map`/`filter` allocate a full flat array then regroup it into 32-wide leaves via `BitMappedTrie.ofAll`; `flatMap`, `ofAll(Iterable)` and `collector()` go through an `ArrayList`/`Object[]` first. | `collection/Vector.java:738, 858, 982, 81, 183`, `BitMappedTrie.java:81-93, 338-365` |
| `Serializable` is implemented by 61 files including every interface, tuple and function. | grep |
| `Some(null)` is representable (`Option.some(null)` accepted; `Option.of(null)` is `None`). | `control/Option.java:508` |
| `Try.Failure.equals` compares stack traces; `hashCode` does not. | `control/Try.java:1482` |
| Generated code (`Tuple0..8`, `Function0..8`, `CheckedFunction0..8`, `API`, `ArrayType`) is regenerated on every build by `scala-maven-plugin` running `generator/Generator.scala` (4,466 lines, Scala 3.8.1) after `maven-clean-plugin` wipes `src-gen`. | `vavr/pom.xml:128-165` |
| No CHANGELOG; the `vavr-test` property-testing module was deleted (`da4baffb8`), so there is no law-testing infrastructure. JaCoCo is referenced in the README but not configured. | git log, `pom.xml` |

---

## 2. Principles borrowed from ZIO and zio-prelude

These are the rules every change below follows.

1. **Name the purpose, not the algebra.** ZIO 2's migration guide: names should "reflect more about their
   purpose rather than just using idiomatic jargon of category theory". `effect` became `attempt`,
   `bimap` became `mapBoth`, `foldM` became `foldZIO`. zio-prelude decomposes `Applicative` into
   `Covariant` + `IdentityBoth`, naming interfaces after the *law* (associativity, identity) and the
   *shape* they produce (a tuple, an either).
2. **One spelling per operation.** ZIO removed `>>=`, collapsed `effectTotal` into `succeed`, and keeps
   pruning duplicates after 2.0 (`catchNonFatalOrDie`, `infinity`, `raceAwait` all deprecated since).
   Java has no symbolic operators, so this rule costs nothing to apply.
3. **A suffix vocabulary carries the variations.** `With` = takes the combining function (`zipWith`,
   `validateWith`); `Discard` = same operation, result dropped; `Some`/`All` = partial vs total;
   `OrElse`/`OrFail`/`OrDie`/`OrThrow` = what happens on the empty/failure path; `from*` = constructor
   named after the input type; `to*` = conversion to another type; `as` = replace the value.
4. **`zip` is the product, never `ap`.** `ap : F<Function<A,B>> -> F<A> -> F<B>` forces currying and reads
   inside-out. `zipWith(that, f)` reads left-to-right and has a concrete arity. Everything that
   `Applicative` gives you is derived from `zip` + `map` (zio-prelude's `AssociativeBoth` docs derive
   `zipWith`, `zipLeft`, `zipRight` from `both` in three lines).
5. **Non-empty types make partial operations total.** `NonEmptyChunk`/`NonEmptyList` give total
   `head`, `last`, `max`, `min`, `reduce`, and errors that can never be an empty list. The contract:
   operations that can shrink return the empty-able type; operations that preserve or grow return the
   non-empty type; narrowing from the empty-able type returns `Option`.
6. **Interfaces promise only what every implementation can honour.** ZIO's `Chunk` is one concrete type.
   Vavr's `Seq` promises `get(int)` on a cons list.
7. **Don't invent what the platform has; use every modern Java feature that fits.** ZIO 2's `Duration` is
   `java.time.Duration`. Java 21+ has sealed hierarchies, records, record patterns, pattern-matching
   `switch`, `SequencedCollection`, virtual threads, `Callable`, `java.util.function.*`, stream
   `Gatherers`, Markdown javadoc; each of those replaces something Vavr hand-rolled in 2014 for Java 8
   (`Match`/`Case`/`$`/`@Unapply`, `Function0..2`, the `toJava*` copies, `CheckedFunction0` as a `Callable`
   stand-in). The fork has no compatibility debt, so the rule is: **if the JDK has it, Zazr uses it.**
8. **Lazy means less code, not less correctness.** Delete before adding. Every deleted method is one the
   fork no longer has to keep binary-compatible.

---

## 3. Changes

### 3.1 Modern Java everywhere

**Decision.** Zazr uses the newest Java features that fit, not just a Java 21 toolchain. The concrete list:

**Baseline JDK: 25+ (decided).** Java 25 is the current LTS (September 2025); the fork has no users to
keep on 21, and `<java.version>` in `pom.xml`, the CI matrix (baseline 25 and the released versions above it; `25, 26, 27` as of September 2026; Amazon Corretto, which ships GA releases only, so no early-access slot) and the release workflow
are already set to it. Over 21 this gives: unnamed variables and patterns (`_`) so `case Invalid(_) ->`
compiles; Markdown javadoc (`///`, JEP 467) so the whole doc rewrite is written in Markdown; flexible
constructor bodies (JEP 513) for validation before `super()`; `Stream.gather` and
`Gatherers.windowFixed/windowSliding/fold/scan` (JEP 485), the JDK's own `grouped`/`sliding`/`scan`;
module import declarations; `ScopedValue` (final) for anything `Future` keeps; and `StableValue`
(preview, JEP 502), the JDK's own `Lazy` and the natural future implementation of ours. No preview features in
main code (decided 2026-09-16; PR #34 allowed them for a few hours and was reversed the same day, see `CLAUDE.md`
for the JDK-pinning cost): `Lazy` moves to `StableValue` when JEP 502 goes final.

**Sealed interfaces + records for every sum type.**

```java
public sealed interface Option<A> permits Option.Some, Option.None {
    record Some<A>(A value) implements Option<A> { public Some { Objects.requireNonNull(value); } }
    record None<A>() implements Option<A> { ... singleton via static NONE }
}
public sealed interface Either<L, R> permits Either.Left, Either.Right { record Left<L,R>(L value) ...; record Right<L,R>(R value) ...; }
public sealed interface Try<A>       permits Try.Success, Try.Failure { record Success<A>(A value) ...; record Failure<A>(Throwable cause) ...; }
public sealed interface Validation<E, A> permits Validation.Valid, Validation.Invalid {
    record Valid<E,A>(A value) ...; record Invalid<E,A>(NonEmptyVector<E> errors) ...;
}
public sealed interface List<A> permits List.Cons, List.Nil { record Cons<A>(A head, List<A> tail) ...; record Nil<A>() ...; }
```

Users then write exhaustive, nested, guarded matches with no library support at all:

```java
return switch (parse(input)) {
    case Valid(User(var name, _)) when name.isBlank() -> reject("blank name");
    case Valid(var user)                              -> save(user);
    case Invalid(var errors)                          -> reject(errors.head());
};
```

Why `switch` beats Vavr's `Match`, so that deleting it is not a regression:

| | Vavr `Match` | Java 21+ `switch` |
|---|---|---|
| Exhaustiveness | none; runtime `MatchError` | compile error on a sealed hierarchy |
| Nested deconstruction | generated `Patterns`, arity ≤ 8, annotation processor | arbitrary depth, no codegen, generics inferred |
| Guards | `$(predicate)` | `when` with all bindings in scope |
| Shadowed/unreachable case | silent | compile error (dominance) |
| `null` | `$(isNull())` | `case null` |
| Equality in nested position | `Some($(42))` | not expressible; `case Some(var x) when x == 42` |
| Non-record classes | `@Unapply` on anything | records only (class deconstruction JEP still a draft at JDK 25); all Zazr sum types are records |
| Case as a value / `Match.option` | `Case` is a `PartialFunction` | `switch` in a lambda, `default -> Option.none()` |

Neither can destructure a `Vector` the way Scala's `case Seq(a, b, rest*)` does; the cons `List` with
public `Cons`/`Nil` records is the one structural collection match Java can express.

Consequences:
- **Delete** `API.java` (11,655 lines), `$.java`, `MatchError`, the `vavr-match`/`vavr-match-processor`
  dependencies, `requires static io.vavr.match`, `PartialFunction`, `Predicates`, `Memoized` (an empty
  marker), `NotImplementedError`/`TODO()`. The `For(...)` comprehensions go too: they exist to work around
  the absence of `flatMap` chains reading well, and `zip`/`zipWith` at arity N covers the common case.
- Tuples become records: `record Tuple2<T1,T2>(T1 _1, T2 _2)`. That gives deconstruction patterns
  (`case Tuple2(var a, var b)`), `equals`/`hashCode`/`toString` for free, and removes ~60% of the
  generated tuple code. Keep `map`, `mapN`, `apply(FunctionN)`, `append`, `concat`, `swap`, `toEntry`.
- `fold(...)` methods stay for expression-position use, but the javadoc points to `switch` as the
  primary way to eliminate a value.

**JDK functional interfaces first.** Signatures use `Function`, `BiFunction`, `Supplier`, `Predicate`,
`BiPredicate`, `UnaryOperator`, `BinaryOperator`, `Consumer`, `Runnable`, `Callable`, `Comparator`.
Vavr's `Function0..2` exist only as adapters (`Function1 extends java.util.function.Function` already);
Zazr keeps `Function3..8` and `CheckedFunction1..8` because the JDK has nothing at those arities or
with checked exceptions, and drops `Function0`, `Function1`, `Function2`, `CheckedFunction0` (that is
`Callable<A>`: `Try.of(Callable<A>)`). What is kept loses `Serializable`, `memoized()` (belongs on
`Lazy`), `arity()`, `reversed()`; keeps `andThen`, `compose`, `curried`, `tupled`, partial `apply`,
`lift`/`liftTry`, `unchecked`. `CheckedRunnable`, `CheckedConsumer`, `CheckedPredicate` stay
(`Try.run(CheckedRunnable)`), since the JDK has no checked variants.

**JDK collection interop via O(1) views, Scala's `asJava` (decided).** Zazr collections do **not**
implement `java.util.List`/`Set`/`Map` themselves; the type never advertises `add()` or `put()`. Instead
each collection has `asJava()`, returning a wrapper that implements the JDK interface over the persistent
value with no copy: `Vector.asJava() : java.util.List<A>` (hence `SequencedCollection`),
`HashSet.asJava() : java.util.Set<A>`, `LinkedHashSet.asJava() : SequencedSet<A>`,
`TreeSet.asJava() : NavigableSet<A>`, `HashMap.asJava() : java.util.Map<K,V>`,
`LinkedHashMap.asJava() : SequencedMap<K,V>`, `TreeMap.asJava() : NavigableMap<K,V>`. Mutators on the
view throw `UnsupportedOperationException`, as `Collections.unmodifiableList` does. `Traversable.asJava()` itself
is an O(1) unmodifiable `java.util.Collection<T>` view (step 3 of #24, 3.7), which every type has today; the
sequences override it with their `java.util.List` view. A map is a `Traversable<Tuple2<K, V>>`, so its `asJava()`
is a `Collection` of entries and cannot be overridden with a `java.util.Map`: the map views above are named
`asJavaMap()` (decided in step 3 of #24, implemented by #26), the set views keep `asJava()`. This is what Vavr
already has for sequences (`Seq.asJava()` → `JavaConverters.ListView`, `Seq.java:130`,
`JavaConverters.java:104`, 469 lines) and what `scala.jdk.CollectionConverters` does; Zazr keeps
`ListView`, adds `SetView`/`MapView` (plus the `Sequenced*`/`Navigable*` variants), and deletes the rest of
the zoo: `toJavaList`/`toJavaSet`/`toJavaMap`/`toJavaCollection`/`toJavaArray`×3 (a copy is
`new ArrayList<>(v.asJava())`), `asJavaMutable` and the `asJava(Consumer)`/`asJavaMutable(Consumer)`
mutation scopes (clever, unused), `toJavaStream`/`toJavaParallelStream` (replaced by `stream()` directly
on the Zazr type). The other direction stays `Vector.ofAll(Iterable)` with a fast path for
`java.util.Collection` (`toArray`) and for an `asJava()` view (unwrap the delegate, as `ListView` already
does). Trade-off accepted: a call to `asJava()` is needed at every JDK boundary, in exchange for Zazr
types that only expose the operations they support.

**Implemented (#26).** The views, all in `collection.internal`, never named by a public signature:

| type | method | view |
|---|---|---|
| `Vector`, `List`, `Queue`, `Stream` (and `NonEmptyVector`, whose view is its `Vector`'s) | `asJava()` | `java.util.List` (`JavaConverters.ListView`, one subclass per type) |
| every other `Traversable` | `asJava()` | `java.util.Collection` (`JavaConverters.CollectionView`) |
| `HashSet` | `asJava()` | `java.util.Set` (`SetViews.HashSetView`) |
| `LinkedHashSet` | `asJava()` | `SequencedSet` (`SetViews.SequencedSetView`) |
| `TreeSet` (and `SortedSet`) | `asJava()` | `NavigableSet` (`TreeViews.NavigableKeySetView`) |
| `HashMap` (and `Map`) | `asJavaMap()` | `java.util.Map` (`MapViews.HashMapView`) |
| `LinkedHashMap` | `asJavaMap()` | `SequencedMap` (`MapViews.SequencedMapView`) |
| `TreeMap` (and `SortedMap`) | `asJavaMap()` | `NavigableMap` (`TreeViews.NavigableMapView`) |

Deleted on every type: `toJavaList` ×2, `toJavaSet` ×2, `toJavaMap` ×3 (and the abstract `Set.toJavaSet()`,
`SortedSet.toJavaSet()`, `Map.toJavaMap()`, `SortedMap.toJavaMap()`), `toJavaCollection`, `toJavaParallelStream`
(`stream().parallel()`), `asJavaMutable` ×2, `asJava(Consumer)`, with the list view's `mutable` flag,
`ChangePolicy` and `ensureMutable()`. `toJavaArray` and `toJavaStream` were already gone (step 3 of #24). A copy is
`new ArrayList<>(x.asJava())`, `new HashMap<>(m.asJavaMap())`, which the javadoc of each `asJava`/`asJavaMap` says.
Decided while implementing:

- **Unmodifiable, unconditionally.** Every mutator throws `UnsupportedOperationException` whatever its arguments
  and whether or not it would change anything: `clear()` on an empty view, `removeIf` matching nothing,
  `remove` of an absent element, `sort`/`replaceAll` on an empty list, `retainAll` of the same elements, the
  `compute*`/`merge`/`putIfAbsent`/`replace` family of a map whatever its function returns, `iterator().remove()`,
  the `ListIterator` mutators, `addFirst`/`addLast`/`removeFirst`/`removeLast`, `putFirst`/`putLast`, and
  `Map.Entry.setValue` (entries are `AbstractMap.SimpleImmutableEntry`). The JDK defaults that would return early
  (`List.removeFirst` on an empty list, `SequencedMap.pollFirstEntry` on an empty map) are overridden.
  **`pollFirst`/`pollLast` and `pollFirstEntry`/`pollLastEntry` throw** too: they remove, as
  `Collections.unmodifiableNavigableSet` has them throw.
- **Live without staleness.** A view reads the persistent value it was made from; nothing can change it, so a
  sub-list, a sub-map or a key set is a view of the same value for as long as it lives (the old "detached snapshot"
  note of `subList` is gone: a snapshot of a value that never changes is the value).
- **`Stream`'s list view forces no more than the read needs**: `get(i)` forces `i + 1` cells, the iterator and the
  spliterator one per step, `isEmpty`, `contains`, `indexOf` and `equals` until they have the answer, the bound
  checks of `subList(from, to)` and `listIterator(i)` `to` and `i` cells; `size()`, `lastIndexOf`, `hashCode`,
  `getLast()` and every read of `reversed()` force the whole Stream and do not terminate on an infinite one
  (the javadoc of `Stream.asJava()` says so).
- **`reversed()` of a list view** is a view of the same sequence with the positions mirrored. Its iterator walks
  a `Vector` down by index (effectively O(1) per step) and reverses a `List`, `Queue` or `Stream` once when it is
  created (O(n)). A list iterator moves forward on the delegate's own iterator (O(1) per step on every type) and
  backward by positional `get` (O(i) on the linear types).
- **`subList` range errors follow the JDK class of the reference**: the forward view throws as `ArrayList` does
  (`IndexOutOfBoundsException` for `from < 0` or `to > size`, `IllegalArgumentException` for `from > to`), the
  reversed view as the JDK's reversed list views do (`Objects.checkFromToIndex`, an `IndexOutOfBoundsException` in
  every case).
- **The tree views read the red-black tree.** `TreeSet`/`TreeMap` have no public key navigation (`ceiling`,
  `floor`, split by key), so `asJava()`/`asJavaMap()` hand their private tree to `TreeViews`, as they already hand
  it to `RedBlackTreeModule`; no member is widened. A `TreeViews.TreeRange` is the tree, the key comparator and
  optional bounds; the sub-views, the descending views and a map's key sets share the tree and carry other bounds or
  the other direction. `ceiling`/`floor`/`higher`/`lower`, `first`/`last`, `contains`/`get` and `size` (a rank
  difference over the subtree sizes every node stores) are O(log n), an iterator is O(log n) to create (an array
  stack down to the lower bound) and amortized O(1) per step; a `TreeMap` lookup compares the probe with each
  node's key in place, with no probe `Tuple2`. The bounds rules are those of `java.util.TreeMap`'s sub-maps: the
  bounds are compared when a sub-view is made (a bound the comparator cannot compare fails then, even on an empty
  view), a lower bound above the upper bound and a sub-view outside its parent's range are
  `IllegalArgumentException`s, and under the natural order `get`/`containsKey`/`contains` reject a `null` or
  non-`Comparable` key even when the view is empty. `comparator()` is `null` for the natural order
  (`Comparators.naturalComparator()`), and a descending view's is `Collections.reverseOrder(comparator())`, so both
  equal the JDK's.
- **`LinkedHashSet`/`LinkedHashMap` walk their insertion order backward** for the reversed views through a
  package-private `LinkedHashMap.reverseIterator()` (an index walk down the insertion-order `Vector`, skipping the
  removed keys' markers: O(1) to create, effectively O(1) per step), handed to the view as an `Iterable` by the
  owning class; `LinkedHashSet` reaches it because both are in `collection`.
- **Map views**: `keySet()`, `values()` and `entrySet()` are views of the same map (not the Zazr `keySet()`/
  `values()`, which copy); on the `LinkedHashMap` and `TreeMap` views they are `SequencedSet`/`SequencedCollection`
  (a `TreeMap` view's key set is its `NavigableSet`), and `sequencedKeySet()`/`sequencedValues()`/
  `sequencedEntrySet()` are the same views. The `HashMap` view reads the trie directly (its key and value
  iterators, and its leaf nodes for the entries), so no `Tuple2` is made per entry. Lookups answer the value or
  `Maps.ABSENT`, so `get`, `containsKey` and `getOrDefault` tell a missing key from a present one without an
  `Option`.
- **Equality** follows the JDK interface: a list view equals any `java.util.List` with the same elements in the
  same order (ordered hash), a set view any `java.util.Set` with the same elements (sum of the hashes), a map view
  any `java.util.Map` with the same mappings (sum of `key.hashCode() ^ value.hashCode()`); the set and map views
  inherit `equals`/`hashCode` from `AbstractSet`/`AbstractMap`.
- **The `ofAll` fast path reads `JavaConverters.View.underlying()`**, implemented by every view: the persistent value
  the view shows exactly, or `null` for a view that shows something else (a reversed or descending view, a key range,
  the keys or the values of a map). `Vector`/`List`/`Queue`/`Stream.ofAll`, `HashSet`/`LinkedHashSet.ofAll`,
  `TreeSet.ofAll` (same comparator instance, as for a `TreeSet` argument), `HashMap`/`LinkedHashMap.ofAll(Map)`,
  `TreeMap.ofAll(Map)` ×2 (same key comparator) and `HashMap`/`LinkedHashMap.ofEntries` (given a map's
  `asJava()`) return that value instead of copying. A sub-list's underlying value is the sub-sequence it shows.
- **Tests**: a local contract suite in the style of Guava's testlib (`JavaViewContract`, no new dependency) runs
  every read of `Collection`, `List`, `Set`, `SequencedSet`, `SortedSet`/`NavigableSet`, `Map`, `SequencedMap` and
  `SortedMap`/`NavigableMap` differentially against `ArrayList`, `java.util.HashSet`/`LinkedHashSet`/`TreeSet` and
  `java.util.HashMap`/`LinkedHashMap`/`TreeMap` with the same content (the same result or the same exception type),
  on 0, 1, 32, 33 and 1025 elements, natural and reversed comparators, down to sub-views of sub-views and the
  descending and reversed views; `JavaViewMutatorTest` asserts that every mutator of every view and derived view
  throws, and `JavaViewOfAllTest` checks the fast path by identity and its absence on the views that show something
  else. `JavaConvertersTest` loses its mutable rows; its cases that expected a no-op or an argument check from a
  mutator of a Zazr view now expect `UnsupportedOperationException`.

**`Optional` interop only.** `java.util.Optional` is not sealed, cannot be pattern-matched, and is
documented as a return type only. `Option` stays, with `toOptional()`/`fromOptional()`.

**Streams.** `Vector.collector()` stays (it is the `Collectors.toList()` of Zazr). `grouped(n)`,
`sliding(n)`, `scanLeft` are kept as methods but implemented over the builder, not over `Gatherers`
(a `Stream` round-trip allocates more than a leaf loop). Users who want gatherers call `stream()`.

**Concurrency.** If `Future` survives (3.10) it is virtual-thread-first (already
`newVirtualThreadPerTaskExecutor()`), uses `ScopedValue` rather than `ThreadLocal`, and bridges to
`CompletableFuture`; no executor-overload doubling.

**Javadoc in Markdown** (`///`, JEP 467). Every class comment is rewritten while it is being converted,
which is also when the category-theory prose is scrubbed (3.3).

**Internal types live in `.internal` packages (decided, #73).** The API packages hold the API and nothing else:
`com.guizmaii.zazr.collection` had ten package-private top-level types (`Iterator`, `AbstractIterator`,
`BitMappedTrie`, `HashArrayMappedTrie`, `RedBlackTree`, `Collections`, `Maps`, `Comparators`, `JavaConverters`, the
generated `ArrayType`) and `*Module` helper interfaces at the bottom of the public files. The convention:
collection internals go to `com.guizmaii.zazr.collection.internal`, everything else to `com.guizmaii.zazr.internal`
(next to `Throwables`; `TryModule` is there). Each `*Module` helper has its own file (`VectorModule`, `ListModule`,
`StreamModule`, `TraversableModule`, `IteratorModule`, `HashArrayMappedTrieModule`, `RedBlackTreeModule`,
`TryModule`), and `BitMappedTrie`'s `LeafVisitor`, which `Vector` implements with lambdas, has its own file too.
An internal type is `public` where the public packages call it, and so are the members they call; the packages are
never exported (`module-info.java` exports exactly `com.guizmaii.zazr`, `com.guizmaii.zazr.collection` and
`com.guizmaii.zazr.control`), the javadoc build excludes them (`excludePackageNames` `*.internal:*.internal.*` in the
root `pom.xml`), each has a `package-info.java` saying it is not API and may change without notice, and both are
`@NullMarked` like the API packages (NullAway's `AnnotatedPackages=com.guizmaii.zazr` already covers them). No public
or protected signature of an exported type names an internal type: checked on the compiled classes (`javap
-protected` over the three exported packages, public types only, has no `.internal.`), since a source grep would
miss simple-name imports. The tests of internal types (`IteratorTest`, `RedBlackTreeTest`, `HashArrayMappedTrieTest`,
`ComparatorsTest`, `CollectionsTest`, `JavaConvertersTest`) live in `collection.internal` with them.
The other direction, internal code needing a package-private member of a public class, is met through the public
API first, and the member is never widened, since a public member of an exported class is API:
- `Collections.reverseIterator` used the package-private `reverseIterator()` of `Vector` and `Queue`. It gets the same
  results at the same cost from the public API: on a `Vector` an index walk from `length() - 1` down to 0 over
  `get(i)` (O(1) to create, effectively O(1) per step), on a `Queue` `reverse().iterator()` (O(n) to create, as the
  old `reverseIterator` was, which reversed the front). `Queue.reverseIterator` is deleted; `Vector.reverseIterator`
  is private, used by `Vector.reverse`.
- `Stream.Cons` is a public class whose constructors and `head`/`tail` fields are package-private; its two
  implementations (`ConsImpl`, `AppendElements`) extend it and read `tail` on each other, which a subclass in another
  package cannot do without `protected` members, and `protected` on a public non-final class is API. They are
  private nested classes of `Stream.Cons` instead (a member of the interface `Stream` would be implicitly public),
  reachable from the rest of `Stream`'s body; the other `StreamModule` classes use only public members of `Cons`
  and moved.

Trade-off: package-private hides on the classpath and on the module path; a `public` type in an unexported package
hides on the module path only, and on the classpath the package name is the warning. Accepted for an API package that
lists only the API and for shorter public files.

**Open.** Whether `List` keeps `Cons`/`Nil` as public record cases (nice for `switch`) or stays opaque.
Recommendation: public, since head/tail pattern matching is the one reason to keep a cons list at all.

### 3.2 Remove `Value`

**Decision.** Delete `Value` and `Iterable` from the control types. `Option`, `Either`, `Try`,
`Validation`, `Lazy` and `Future` stop being iterable, stop sharing `get()`, and stop carrying 52
conversions, `stdout()`, `spliterator()`, `eq()`, `stringPrefix()`, `isAsync()/isLazy()/isSingleValued()`.

Why: `for (String s : myOption)` and `list.addAll(someTry)` compile today. `Future.iterator()` blocks.
`Either.iterator()` silently skips `Left`. `Traversable.get()` is `head()`, so `get()` means "the value"
on `Option` and "an arbitrary element" on `HashSet`. ZIO's `Option`/`Either`/`Exit` are not iterable and
nobody misses it.

What replaces it, per type, is a **short, explicit** conversion set:

| Type | conversions kept |
|---|---|
| `Option` | `toEither(Supplier<L>)`, `toTry(Supplier<Throwable>)`, `toValidation(Supplier<E>)`, `toVector()`, `toList()`, `toOptional()`, `stream()` |
| `Either` | `toOption()`, `toTry(Function<L,Throwable>)` (Java cannot restrict an instance method to `L extends Throwable`, so the mapping is always explicit; `toTry(t -> t)` for an `Either<Throwable, R>`; decided 2026-09-17, #19), `toValidation()`, `toVector()` |
| `Try` | `toOption()`, `toEither()`, `toValidation()`, `toVector()`, `toCompletableFuture()` |
| `Validation` | `toOption()`, `toEither()` (`Either<NonEmptyVector<E>,A>`), `toEitherWith(Function<NonEmptyVector<E>,E2>)`, `toTry(Function<NonEmptyVector<E>,Throwable>)`, `toVector()` |
| `Lazy` | none; `get()` is the conversion |

`getOrElse`, `getOrElseGet`, `getOrElseThrow`, `getOrNull`, `contains`, `exists`, `forAll`, `forEach`
stay as instance methods where they make sense, defined per type (they are five one-liners each; the
duplication is cheaper than a god interface).

`collect(Collector)` disappears from control types. `collect(PartialFunction)` disappears everywhere
(see 3.1); the replacement is `filter(...).map(...)` or `flatMap(x -> switch (x) { ... })`.

### 3.3 Naming table (ZIO/prelude vocabulary applied to Vavr)

| Vavr today | Zazr | Rationale |
|---|---|---|
| `ap`, `combine(...).ap(f)` | `zip`, `zipWith`, static `zip(a,b,c)`, `zipWith(a,b,c,f)` | Principle 4 |
| `sequence(Iterable<F<A>>)` | `collectAll(Iterable<F<A>>)` | ZIO name; "sequence" means nothing to a Java dev |
| `Tuple.sequence1..8(Iterable<TupleN<...>>)` | `Tuple.unzip1..8(Iterable<TupleN<...>>)` | not a `collectAll`: it splits a sequence of tuples into a tuple of `Seq`s, i.e. an unzip, matching `Seq.unzip`/`unzip3` (3.7) rather than the control-type `sequence`/`collectAll` family; `Tuple.unzip2`/`unzip3` delegate to `Stream`'s implementation so there is one implementation (#59) |
| `traverse(Iterable<A>, A->F<B>)` | `forEach(Iterable<A>, A->F<B>)` | ZIO/prelude name. Static on the companion, so no clash with `Iterable.forEach`. |
| `Either.sequence` (accumulates *both* sides) vs `sequenceRight` (short-circuits) | delete the accumulating one; `Either.collectAll` short-circuits. Accumulation is `Validation`'s job. | one semantics per name |
| `bimap` | `mapBoth` | ZIO 2 rename |
| `peek` | `tap` | ZIO name; `peek` collides with `java.util.stream.Stream.peek` semantics only by accident. On a collection `tap` runs the action on **every** element (Vavr's `peek` ran it on the head only, a `Value` artifact); `Stream` runs it on the head now and on the rest lazily, `Iterator` lazily (decided, #20) |
| `peekLeft` / `onFailure` / `onSuccess` / `onEmpty` / `andThen(Consumer)` | `tapLeft`, `tapError`, `tap`, `tapNone`; drop `andThen(Consumer)` | `tap*` family |
| `mapTo(U)` | `as(U)` | ZIO name |
| `mapToVoid()` | `unit()` or delete | rarely useful without an effect type; delete |
| `swap` | `flip` on `Either`; deleted on `Validation` | ZIO name. (`Tuple2.swap` stays; it's not an error/success swap.) `Validation` has no `flip` since #22: its sides are not symmetric, one is a `NonEmptyVector` (3.5) |
| `getOrElseGet(Function<L,R>)` | `getOrElse(Function<L,R>)` overload | keep one name |
| `orElseRun`, `orElseTry` | `orElse` + `tapError`/`getOrElseThrow` | fewer near-duplicates |
| `transform(Function<Option<T>,U>)` | delete | it is just function application |
| `collect(PartialFunction)` on collections, `Option`, `Try`, `Iterator`; `PartialFunction` itself; `Function1.partial` | `collect(Function<? super A, Option<? extends B>>)`: one pass, match-and-transform, no double evaluation; the `case` ergonomics come from a `switch` inside the lambda (`collect(s -> switch (s) { case Circle c -> Option.some(c.radius()); default -> Option.none(); })`). `PartialFunction` is deleted (decided): Java has no pattern literal that produces one, so it was two hand-written methods bundling `filter` and `map`. Added in the naming pass (#20); absent between #13 and #20. | Java 21 `switch` + `Option` replace Scala's partial-function literal |
| `Try.of` / `ofSupplier` / `ofCallable` / `run` / `runRunnable` | `Try.of(Callable<A>)`, `Try.run(CheckedRunnable)` | ZIO: `attempt` is one method; `Callable` is the JDK's checked supplier |
| javadoc: "monadic container type", "behave like a monad", "applicative functor, not a Monad", "more like a Functor than a Monad", `// Monad implementation`, "For-comprehension" | rewritten in plain English: "a value that may be absent", "a computation that either fails with `L` or succeeds with `R`", "`Validation` keeps *all* errors: combining two invalid values with `zip` concatenates their errors, whereas `Either` stops at the first". No mention of Monad/Functor/Applicative anywhere in the repo, generator included (`monadicTypesFor` etc.). | Principle 1 applies to prose too; a grep for `monad\|functor\|applicative` in CI keeps it that way |
| `Option.of(nullable)` / `Option.some` / `Option.when` | `Option.ofNullable`, `Option.some` (**rejects null**), `Option.when(boolean, Supplier)` | see 3.9 |
| `Validation.valid/invalid`, `Either.right/left`, `Try.success/failure` | keep | already purpose-named |
| `Validation.cond`, `Either.cond` | `Either.fromPredicate(A, Predicate<A>, Supplier<L>)`; `Validation.fromPredicate(A, Predicate<A>, Function<A, E>)` | prelude name; reads as what it does. `Validation`'s takes the rejected value (3.5, #22) so the error can name it, the everyday case for a field check; `Either`'s keeps the supplier |
| `Try.failed()` | `Try.flip()`? no. Delete; use `fold`. | |
| `Try.recover(Class<X>, Function)` ×4 / `recoverWith` ×3 / `recoverAllAndTry` / `recoverAndTry` | `catchAll(Function<Throwable,A>)`, `catchSome(Class<X>, Function<X,A>)`, `catchAllWith(Function<Throwable,Try<A>>)`, `catchSomeWith(Class<X>, ...)` | ZIO `catchAll`/`catchSome` |
| `Try.mapFailure(Case...)` | `mapError(Function<Throwable,Throwable>)` | Match API is gone |
| `Try.andFinally`, `andFinallyTry` | `ensuring(CheckedRunnable)` only | ZIO name. One overload, not two: `ensuring(Runnable)` next to `ensuring(CheckedRunnable)` is ambiguous for every lambda (javac: both `void` functional interfaces match), and a `Runnable` lambda already is a `CheckedRunnable` lambda; a `Runnable` variable is passed as `r::run` (decided, #20) |
| `Try.withResources(...)` ×8 + `WithResources1..8` | keep one `Try.withResources(Callable<R>, CheckedFunction1<R,A>)`; N resources nest | arity ladder not worth it |
| `Try.filter` ×3, `filterTry` ×3 | one `filter(Predicate, Function<A,Throwable>)` and `filter(Predicate)` | |
| `Either.left()/right()` projections + `LeftProjection`/`RightProjection` (24 members each) | delete; `Either` is right-biased and has `mapLeft`, `flip`, `fold` | ZIO 2 deleted all arrow combinators for the same reason |
| `Either.filterOrElse`, `filter -> Option<Either>` | `filterOrElse(Predicate, Function<R,L>)` only | `filter` returning `Option<Either>` is a type pun |
| `Either.getOrElseThrow(Function<L,X>)` | keep | good |
| `Option.fold(Supplier, Function)` / `Either.fold` / `Try.fold` / `Validation.fold` | keep, uniform argument order (failure first, success second) | ZIO `fold(failure, success)` |
| `Value.exists/forAll` on `Option` | `exists`, `forAll` stay on `Option` only | |
| `Lazy.val(Supplier, Class)` (dynamic proxy) | delete | magic |
| `Lazy.filter -> Option` | delete | |
| `isAsync`, `isLazy`, `isSingleValued`, `isTraversableAgain`, `hasDefiniteSize`, `isSequential`, `isOrdered`, `isDistinct` | delete | reflection-on-the-type flags that no caller should branch on |

Things ZIO does that **do not** port and should not be imitated:
- `Par` suffix: there is no parallelism in pure data. Do **not** name `Validation`'s accumulating zip
  `zipPar` as zio-prelude does; for `Validation` there is only one sensible `zip`, and it accumulates.
- `Cause<E>` trees, `Exit`, `orDie`/`refineOrDie`, `sandbox`: they exist because a fiber can fail in
  parallel, be interrupted, and fail in a finalizer. A pure `Try` has none of those. Keep `Try` as
  `Success | Failure(Throwable)`, keep the existing fatal-error rethrow (`TryModule.isFatal`).
- `Zippable` (implicit tuple flattening): impossible in Java. See 3.4 for the arity-N alternative.

### 3.4 `zip` at arity N (the replacement for `ap` and `Builder`)

**Decision.** Every control type gets the same family, arities 2..8 (matching `Tuple8`):

```java
// instance
<B> Option<Tuple2<A,B>> zip(Option<B> that);
<B, C> Option<C> zipWith(Option<B> that, BiFunction<A,B,C> f);
<B> Option<A> zipLeft(Option<B> that);
<B> Option<B> zipRight(Option<B> that);
// static, for N = 2..8
static <A,B,C> Option<Tuple3<A,B,C>> zip(Option<A> a, Option<B> b, Option<C> c);
static <A,B,C,D> Option<D> zipWith(Option<A> a, Option<B> b, Option<C> c, Function3<A,B,C,D> f);
```

Same for `Either<L,·>`, `Try`, `Validation<E,·>` and `Lazy`. `Tuple` gets no new methods: `TupleN.concat(TupleM)` is
already the flattening operation for tuples.
The rule that makes chaining unnecessary: **arity is fixed at the call site by the static overload**, so
users never see `Tuple2<Tuple2<A,B>,C>`. This is ZIO's `Zippable` result without the type-level
machinery, and it is exactly what the migration guide says higher-arity zips were for.

Semantics per type: `Option`/`Either`/`Try` fail-fast (first `None`/`Left`/`Failure` wins), `Lazy` defers.
`Validation` accumulates: `Invalid(e1) zip Invalid(e2) == Invalid(e1 ++ e2)`.

`Validation.Builder..Builder8` and instance `combine` are deleted (done in #22, with the instance `zip` family; the
static arities came in #23).

**Decided while implementing (#23):**
- **Instance family** on `Option`, `Either`, `Try` and `Lazy` (`Validation` has had it since #22, with the same
  signatures, wildcards and messages): `zip(that)` returning a `Tuple2`, `zipWith(that, BiFunction)`, `zipLeft(that)`,
  `zipRight(that)`. Fail-fast in receiver-then-argument order: `Option` yields `None` when either side is `None`;
  `Either` returns the first `Left` and `Try` the first `Failure`, unchanged (narrowed, no reallocation).
  `zipLeft`/`zipRight` inspect both sides and keep one, like ZIO's `<*` and `*>`; they are not `orElse`:
  `Some(1).zipLeft(None)` is `None`. When both sides succeed, `zipLeft` returns the receiver and `zipRight` the
  argument, as is.
- **Static family**, N = 2..8: `zip(a1..aN)` returning the type wrapping `TupleN`, and `zipWith(a1..aN, f)` where `f`
  is a `BiFunction` for N = 2 and `Function3..Function8` for N = 3..8. Fail-fast in argument order for
  `Option`/`Either`/`Try`: the first failure is returned as is, `f` is not called, and no argument after the failing
  one is inspected (every argument is null-checked first). `Validation` accumulates: `Invalid` of the errors of every
  `Invalid` argument, concatenated in argument order (one `Vector.Builder`, created by the first `Invalid`, as
  `forEach` does), `Valid` only when every argument is. Type parameters follow the wildcard style of the rest of the
  API (`Option<? extends B>`, `BiFunction<? super A, ? super B, ? extends C>`), and the error side of
  `Either`/`Validation` is shared across arguments (`Either<? extends L, ? extends A1>` ... yielding
  `Either<L, TupleN<...>>`).
- **`Lazy`** has no failure: the result, instance or static, is an unevaluated `Lazy` that, when first forced, forces
  the receiver then the argument (the arguments in argument order), applies `f` and caches the result. `f` runs
  once on a successful evaluation; if it throws, nothing is cached and it runs again on the next `get()`, as for
  `Lazy.of`. A `Lazy` may hold `null` (3.11), so `f` may return `null` and a `null` operand value is passed through.
- **Null policy.** A null argument, container or `f`, is a `NullPointerException` with `<parameter> is null`
  (`that is null`, `o1 is null`, `f is null`). `f` returning null where the wrapper cannot hold it is rejected at the
  call site with `<Type>.zipWith: f returned null`, as 3.5 states for `Validation`, on `Option`, `Either` and
  `Validation`. `Try` is different because `f` runs under `Try` like a `map` mapper: what it throws is captured as a
  `Failure` (fatal throwables propagate) and a null result is a `Failure` of a `NullPointerException` carrying the
  same message, not a thrown one; every non-fatal outcome of the computation ends up in the returned `Try`, as
  `Try.of` and `Try.map` already promise.
- **Where the code lives.** The main-code methods are hand-written in `Option.java`, `Either.java`, `Try.java`,
  `Validation.java` and `Lazy.java`: the generator cannot own part of a hand-written file. `zipWith` at every arity
  applies `f` to the unwrapped values directly, never through an intermediate `TupleN` or a chain of arity-2 zips
  producing nested tuples; `zip` is `zipWith(..., Tuple::of)`, one tuple allocation. The static family's test matrix
  is generated (`genControlZipTests` in `Generator.scala`, one `<Type>ZipTest` class per type under `src-gen/test`);
  the instance methods are tested by hand in `OptionTest`, `EitherTest`, `TryTest` and `LazyTest`.

### 3.5 `Validation<E, A>` with a `NonEmptyVector<E>` error side

**Decision.** Modelled on zio-prelude's `ZValidation` minus the log channel:

```java
public sealed interface Validation<E, A> {
    record Valid<E, A>(A value)                         implements Validation<E, A> {}
    record Invalid<E, A>(NonEmptyVector<E> errors)      implements Validation<E, A> {}

    // constructors
    static <E,A> Validation<E,A> valid(A a);
    static <E,A> Validation<E,A> invalid(E e);                       // wraps in NonEmptyVector.of(e)
    static <E,A> Validation<E,A> invalidAll(NonEmptyVector<E> es);
    static <E,A> Validation<E,A> fromEither(Either<E,A> e);
    static <E,A> Validation<E,A> fromOption(Option<A> o, Supplier<E> ifNone);
    static <E,A> Validation<E,A> fromPredicate(A a, Predicate<A> p, Function<A,E> ifFalse);
    static <A>   Validation<Throwable,A> fromTry(Try<A> t);
    static <E,A> Validation<E,A> of(Callable<A> f, Function<Throwable,E> onError);

    // accumulate
    <B> Validation<E,Tuple2<A,B>> zip(Validation<E,B> that);
    <B,C> Validation<E,C> zipWith(Validation<E,B> that, BiFunction<A,B,C> f);
    static ... zip(v1..vN) / zipWith(v1..vN, f)                       // generated, 2..8
    static <E,A>   Validation<E,Vector<A>> collectAll(Iterable<Validation<E,A>> vs);
    static <E,A,B> Validation<E,Vector<B>> forEach(Iterable<A> as, Function<A,Validation<E,B>> f);
    static <E,A,B> Validation<E,NonEmptyVector<B>> forEach(NonEmptyVector<A> as, Function<A,Validation<E,B>> f);
    static <E,A,B> Tuple2<Vector<E>,Vector<B>> partition(Iterable<A> as, Function<A,Validation<E,B>> f); // cannot fail
    Validation<E,A> orElse(Supplier<Validation<E,A>> that);           // errors of the discarded side are dropped

    // short-circuit (documented loudly: errors of `f` are never accumulated with this one's)
    <B> Validation<E,B> flatMap(Function<A,Validation<E,B>> f);
    <B> Validation<E,B> flatMapEither(Function<A,Either<E,B>> f);   // same, for an Either-returning step
    <B> Validation<E,Tuple2<A,B>> zip(Either<E,B> that);            // overload is unambiguous: not a lambda

    // transform / eliminate
    <B> Validation<E,B> map(Function<A,B> f);
    <E2> Validation<E2,A> mapError(Function<E,E2> f);
    <E2> Validation<E2,A> mapErrorAll(Function<NonEmptyVector<E>,NonEmptyVector<E2>> f);
    <B> B fold(Function<NonEmptyVector<E>,B> ifInvalid, Function<A,B> ifValid);
    A getOrElse(A a); A getOrElse(Function<NonEmptyVector<E>,A> f);
    <X extends Throwable> A getOrElseThrow(Function<NonEmptyVector<E>,X> f) throws X;
    Validation<E,A> tap(Consumer<A>); Validation<E,A> tapError(Consumer<NonEmptyVector<E>>);
    // no flip/swap: the sides are no longer symmetric (one is non-empty)
    boolean isValid(); boolean isInvalid();
    Either<NonEmptyVector<E>,A> toEither(); <E2> Either<E2,A> toEitherWith(Function<NonEmptyVector<E>,E2>);
    Option<A> toOption(); Try<A> toTry(Function<NonEmptyVector<E>,Throwable>);
}
```

Notes:
- **`flatMap` stays (decided) and is the documented exception.** It gets a sibling `flatMapEither` for
  the common "validate all fields, then run a cross-field rule that returns `Either`" case, so callers
  don't write `flatMap(a -> Validation.fromEither(rule(a)))`. It cannot be an overload of `flatMap`:
  two `Function`-typed overloads are ambiguous for an implicitly typed lambda. `zip(Either)` *can* be an
  overload because the argument is a value, not a lambda. zio-prelude keeps `flatMap` and says: "when we chain
  validations like this we only do the second validation if the first one is successful... If all we
  are doing is chaining then we don't actually need `Validation` and could just use `Either`." Copy that
  sentence into the javadoc. Alternative if you want a stronger invariant: drop `flatMap` and force
  `toEither().flatMap(...)`. Recommendation: keep it; mixed "validate all fields, then check a cross-field
  rule" is the everyday case.
- `Invalid.equals` in zio-prelude is order-insensitive (via a multiset). Recommendation: **order-sensitive**
  (record default). Simpler, and error order is usually the field order, which is meaningful.
- `Validation.filter -> Option<Validation>` is deleted, like `Either.filter`.
- The `W` warnings/log channel is **not** ported. In Scala it is free (`Validation[E,A] = ZValidation[Nothing,E,A]`);
  in Java it would be a third type parameter on every signature. zio-prelude's `orElseLog` idea (demote the
  discarded alternative's errors to warnings) can come back later as a separate type if anyone asks.
- `These<A,B>` is not ported either: every combinator needs a user-supplied merge for the left side.

**Decided while implementing (#22):**
- **The surface is the sketch above plus the common member set of 3.2 and `get()`, `mapBoth`, `toVector()`.**
  The sketch lists only what is specific to `Validation`; the members every control type has (`getOrElse(A)`,
  `getOrElse(Supplier)`, `getOrElseThrow(Supplier)`, `getOrNull()`, `contains`, `exists`, `forAll`, the instance
  `forEach(Consumer)`) stay, with `Either`'s right-side semantics (`forAll` holds vacuously on `Invalid`), next to
  the `Function<NonEmptyVector<E>, ...>` forms of `getOrElse` and `getOrElseThrow`. `get()` throws
  `NoSuchElementException` on `Invalid`, like `Either.get()` and `Try.get()`; `mapBoth` and `toVector()` follow the
  naming table and the conversion sets of 3.2. Not kept: `isEmpty` (a `Validation` has no empty case; `Invalid` is
  a failure, not an absence, and `isInvalid()` is the question) and `orElse(Validation)` (`orElse(Supplier)` only).
  Everything else the Vavr type had is gone: `Builder..Builder8`, `combine`, `ap`, `getError`, `flip`, `filter`,
  `narrow` and the `Seq<E>`-accumulating `collectAll`/`forEach`. The `Invalid` errors are reached by the record
  accessor `errors()` (`case Invalid(var errors)` in a `switch`), by `fold`, `tapError`, `getOrElse(Function)` or
  `toEither()`.
- **`fromPredicate(A, Predicate<A>, Function<A, E>)`**, as sketched, not the `Supplier<E>` of the 3.3 row: the
  function receives the rejected value so the error can name it. `Either.fromPredicate` keeps its supplier.
- **`of(Callable, Function<Throwable, E>)` is `Try.of` followed by a conversion**, so it has exactly `Try.of`'s
  policy: a fatal throwable is rethrown, a `null` result is a `NullPointerException` handed to `onError`.
- **`forEach(NonEmptyVector, f)` and `forEach(Iterable, f)` are overloads.** Unlike 3.6's `flatMap`/`flatMapAll`,
  the lambda is the second argument and the first argument's type is a plain value: javac picks the
  `NonEmptyVector` overload as the more specific one, with an implicitly typed lambda too. It delegates to the
  `Iterable` one and re-wraps the result (`unsafeFromVector`: as many results as inputs).
- **Accumulation is builder-based.** `forEach`/`collectAll`/`partition` are one loop over a `Vector.Builder` per
  side (results and errors); the errors builder is created on the first `Invalid` and the result builder is left
  alone from then on. `collectAll` is `forEach` with the identity. `zip` on two `Invalid`s is one
  `NonEmptyVector.appendAll`; an `Invalid` operand is returned as is when the other side is `Valid`.
- **`zip(Either)`** treats a `Left` as a validation with one error, so `Invalid(a) zip Left(b)` is `Invalid(a, b)`.
- **`orElse` drops the errors of the discarded side** (as sketched): `Invalid(a) orElse Invalid(b)` is `Invalid(b)`.
  There is no `orElseLog` without the log channel.
- **`toString` is `Invalid(a, b)`**, the errors spread like a collection's, next to `Valid(1)`; the `NonEmptyVector`
  wrapper is not printed.
- Null messages: a null argument is `<parameter> is null` (`value is null`, `errors is null`, `f is null`); a
  function or supplier handed to a method that returns null where a value is required is rejected at the call
  site, before any constructor sees it, with `Validation.<method>: <parameter> returned null`
  (`Validation.mapErrorAll: f returned null`, `Validation.getOrElseThrow: exceptionFunction returned null`, so
  `getOrElseThrow` never executes `throw null`); a null element of `collectAll` is
  `Validation.collectAll: element is null`.

### 3.6 `NonEmptyVector<A>`

**Decision.** A `final class NonEmptyVector<A>` wrapping a `Vector<A>` (ZIO's `NonEmptyChunk` design),
**not** a subtype of `Vector`. If it were a subtype, `Vector.filter` would be inherited unchanged and the
type would guarantee nothing at the use site. Return-type contract, copied from `NonEmptyOps`:

| returns `NonEmptyVector` | returns `Vector` | total (no `Option`) | returns `Option` |
|---|---|---|---|
| `map`, `flatMap(A->NonEmptyVector<B>)`, `append`, `appendAll(Vector)`, `appendAll(NonEmptyVector)`, `prepend`, `prependAll(Vector)`, `prependAll(NonEmptyVector)`, `concat(NonEmptyVector)`, `reverse`, `distinct`, `distinctBy` ×2, `sorted` ×2, `sortBy` ×2, `zip(NonEmptyVector)`, `zipWith(NonEmptyVector, ·)`, `zipWithIndex` ×2, `scanLeft` (n+1), `update(i,·)` ×2, `tap`; `grouped(n)` as `Vector<NonEmptyVector<A>>`, `groupBy` as `HashMap<K,NonEmptyVector<A>>` | `filter`, `reject`, `collect(A->Option<B>)`, `flatMapAll(A->Iterable<B>)`, `partitionMap`, `duplicates`, `duplicatesBy`, `tail`, `init`, `drop*`, `take*`, `slice`, `removeAt`, `remove`, `removeAll` ×3, `toVector()` | `head`, `last`, `max(Comparator)`, `min(Comparator)`, `maxBy(A->U)`, `minBy(A->U)`, `reduce`, `reduceLeft`, `reduceRight`, `reduceMap(A->B, (B,B)->B)`, `size` (≥1), `get`, `mkString` ×3, `foldLeft`, `foldRight`, `contains`, `exists`, `forAll`, `count`, `indexOf`, `iterator`, `stream`, `asJava`, `toList`, `toSet` | `find`, `findLast`, `indexOfOption`, `tailNonEmpty()`, `initNonEmpty()` |

Constructors: `of(A head, A... tail)`, `fromIterable(A head, Iterable<? extends A> tail)`, `single(A)`,
`fromVector(Vector<A>) : Option<NonEmptyVector<A>>`, `fromIterable(Iterable<? extends A>) : Option<...>`,
`unsafeFromVector(Vector<A>)` (throws `IllegalArgumentException`), static
`flatten(NonEmptyVector<? extends NonEmptyVector<? extends A>>)`. On `Vector`:
`Option<NonEmptyVector<A>> toNonEmptyVector()`.

Accept the weak type, return the strong one: `appendAll(Vector<A>) : NonEmptyVector<A>` (ZIO's
`NonEmptyChunk.append(Chunk)` does exactly this).

Implementation cost is low: every method is a one-line delegation to the wrapped `Vector` plus an
`unsafe` re-wrap. `NonEmptyVector` implements `Iterable<A>` (it *is* a collection).

**Decided while implementing (#21), where Java forced a choice:**
- **`flatMap` / `flatMapAll`.** The two `flatMap`s of the table cannot be overloads: a lambda argument
  (`x -> ...`) is compatible with both `Function<A, NonEmptyVector<B>>` and `Function<A, Iterable<B>>`, and
  javac reports an ambiguity on every call. `flatMap` keeps the name for the function returning a
  `NonEmptyVector` (the result stays non-empty); the function returning any `Iterable` is `flatMapAll`,
  returning `Vector` (`All` from the suffix vocabulary of section 2: the total, possibly empty, variant).
- **`fromIterable(head, tail)`, not `of(head, Iterable)`.** Next to `of(A head, A... tail)`, the overload
  `of(A head, Iterable<? extends A> tail)` is selected by javac's first (non-varargs) phase whenever the
  *elements* are themselves iterables: `NonEmptyVector.of(nev1, nev2)` or `of(List.of(1), List.of(2))`
  silently becomes "head `nev1`, then the elements of `nev2`", typed `NonEmptyVector<Object>`, or fails
  to compile under a target type. A `Vector<Vector<A>>` from `grouped`/`combinations` is a common input,
  so the head-plus-iterable constructor is `fromIterable(A head, Iterable<? extends A> tail)`, ZIO's own
  name (`NonEmptyChunk.fromIterable(a, as)`), distinguished from `fromIterable(Iterable) : Option` by arity.
- **`Vector.toNonEmptyVector()`, not `nonEmpty()`.** Java cannot override the boolean
  `Traversable.nonEmpty()` (kept, 3.7) with `Option<NonEmptyVector<T>> nonEmpty()`, and `Vector` implements
  `Traversable`. The narrowing takes the `to*` conversion name of the suffix vocabulary (section 2, rule 3),
  next to `toVector()`, `toList()`, `toSet()`.
- **Null messages name the type.** The wrapped `Vector` already rejects nulls; the paths where a
  `NonEmptyVector` receives an element itself (`of`, `fromIterable`, `single`, `append`, `prepend`,
  `update(i, a)`) check first, with messages `NonEmptyVector: head is null`, `NonEmptyVector: element is null`,
  `NonEmptyVector.append: element is null`, and so on. Mapper results are checked by `Vector`'s own paths.
- **No `Option` on the way to a total result.** `max`, `min`, `maxBy`, `minBy` and `reduceMap` are loops over
  the iterator (the key function applied once per element, ties resolved to the first element as on
  `Vector`), not `vector.maxBy(f).get()`, so nothing is wrapped to be unwrapped on the next line.
- `reduce`, `reduceLeft`, `reduceRight`, `reduceMap` take `BiFunction<? super A, ? super A, ? extends A>`
  like `Vector` does; a `BinaryOperator<A>` lambda or method reference fits. `iterator()` returns
  `java.util.Iterator<A>`: the Zazr `Iterator` is deleted in 3.7, and the type declares only what survives.
- Equality is structural over the elements and only against another `NonEmptyVector`: a `Vector` and a
  `NonEmptyVector` with the same elements are not equal (compare through `toVector()`).
  `toString` is `NonEmptyVector(a, b)`.

**The rest of `Vector`'s API (decided 2026-09-25, #90).** The table above was the first subset; `NonEmptyVector`
now has every operation of `Vector`, each under the same contract, delegating to `Vector` and re-wrapping:

- **Returns `NonEmptyVector`:** `as`, `appendAll(Iterable)`, `prependAll(Iterable)`, `insert`, `insertAll`,
  `intersperse`, `padTo`, `leftPadTo`, `rotateLeft`, `rotateRight`, `shuffle`, `replace`, `replaceAll`, `scan` and
  `scanRight` (n+1), `zipAll(Iterable, A, B)` (max(n, m) ≥ 1, so any `Iterable` is accepted), `distinctByKeepLast` ×2,
  `permutations` as `NonEmptyVector<NonEmptyVector<A>>`, `combinations()` as `NonEmptyVector<Vector<A>>` (it always
  holds the empty combination and the whole), `crossProduct()`, `crossProduct(NonEmptyVector)` (like `zip`, the
  non-empty argument keeps the result non-empty), static `transpose(NonEmptyVector<? extends NonEmptyVector<? extends A>>)`
  as `NonEmptyVector<NonEmptyVector<A>>`; `unzip`/`unzip3` as tuples of `NonEmptyVector`s; `sliding(size)`,
  `sliding(size, step)` and `slideBy` as `Vector<NonEmptyVector<A>>` like `grouped`; `splitAtInclusive` as
  `Tuple2<NonEmptyVector<A>, Vector<A>>`, because its first part always ends with the matching element or holds
  everything.
- **Returns `Vector` (or a tuple of them):** `splitAt(int)`, `splitAt(Predicate)`, `span`, `partition`, `retainAll`,
  `removeFirst`, `removeLast` (both take `Predicate<? super A>`, where `Vector` takes `Predicate<T>`), `patch`,
  `subSequence` ×2, `combinations(int)` and `crossProduct(int)` as `Vector<Vector<A>>` (`k > size()` or a negative
  power gives none, 0 gives one empty vector), and the `Iterable` forms `zip(Iterable)`, `zipWith(Iterable, ·)`,
  `crossProduct(Iterable)`, empty when the argument is.
- **Total:** `max()`, `min()` (natural order; `min` returns a `NaN` whenever one is present, as `Vector.min()` does),
  `maxBy(Comparator)`, `minBy(Comparator)`, `average()` as a `double` (the value `Vector.average()` holds, from the
  same compensated sum, without the `Option`), `single()` (throws when there is more than one element), `fold`, `sum`,
  `product`, and the queries `indexOf(a, from)`, `indexWhere` ×2, `lastIndexOf` ×2, `lastIndexWhere` ×2,
  `indexOfSlice` ×2, `lastIndexOfSlice` ×2, `startsWith` ×2, `endsWith`, `containsSlice`, `containsAll`, `search` ×2,
  `segmentLength`, `prefixLength`, `existsUnique`, `forEachWithIndex`, `collect(Collector)`,
  `collect(Supplier, BiConsumer, BiConsumer)`.
- **Returns `Option`:** `arrangeBy` (two elements can share a key), `indexOfOption(a, from)`, `indexWhereOption` ×2,
  `lastIndexOfOption` ×2, `lastIndexWhereOption` ×2, `indexOfSliceOption` ×2, `lastIndexOfSliceOption` ×2.
- **Conversions:** `toQueue`, `toStream`, `toLinkedSet`, `toSortedSet` ×2, `toArray` ×2, `toMap` ×2, `toLinkedMap` ×2,
  `toSortedMap` ×4.
- **Nulls.** An element argument is checked before delegating, even where `Vector` would not look at it
  (`intersperse` on one element, `padTo` to a size already reached, `replace` of an absent element), with a message
  naming the method (`NonEmptyVector.padTo: element is null`). An `Iterable` argument is copied through the same check
  as the constructors (`NonEmptyVector: element is null`), which also reads it once. A user function whose result is
  stored is wrapped so that a null result names the method: `scan`, `scanRight`, `zipWith(Iterable, ·)`, `unzip`,
  `unzip3` (the tuple and its components), `arrangeBy`, `distinctByKeepLast` (whose keys `Vector` rejects too), and the
  key, value and entry of the `to*Map` conversions. `fold` and `slideBy` let a null through as `Vector` does: `fold`'s
  result is returned, never stored, and `slideBy` only compares its keys.
- **Deliberately absent:** `headOption`, `lastOption`, `reduceOption`, `reduceLeftOption`, `reduceRightOption`,
  `singleOption` (the `Option` forms of what is total here, or of `single`); `tailOption`, `initOption` (`tail` and
  `init` already return a `Vector`, and `tailNonEmpty`/`initNonEmpty` are the narrowing); `isEmpty`, `nonEmpty`,
  `orElse`, `toNonEmptyVector` (constant on this type: false, true, `this`, `Some(this)`); `length` (`size` is the one
  spelling, decided with the maintainer on 2026-09-25; the removal of `length` from `Vector`, `List`, `Queue` and
  `Stream` is a separate change, #90 again, which also takes it off this list). `NonEmptyVectorTest` asserts
  reflectively that every public instance method name of `Vector` exists on `NonEmptyVector` except exactly this
  list, and that every method returning a `NonEmptyVector` is exercised by the non-empty guarantee test.

**Decided.** `NonEmptyVector` only; no `NonEmptyList` in v1. `Validation` errors and `reduce`/`max` are the
motivating cases and `NonEmptyVector` covers them. Add `NonEmptySet`/`NonEmptyMap` only on demand.

### 3.7 Removing the `Seq` abstraction

**Decision.** Delete `Seq`, `IndexedSeq`, `LinearSeq`, `Foldable`, `Ordered`, and the
`PartialFunction`/`Function1` supertypes of `Seq`/`Set`/`Map`/`Multimap`. Each concrete collection is
`final` (or sealed with record cases, for `List`) and declares its own full API with its own return types
(`Vector.map -> Vector`, no covariant-override ladders). This is ZIO's `Chunk`: one concrete type, one
documented cost model.

What stays shared is one small read-only interface, kept under the name `Traversable<T>`, containing only
operations that are **order-agnostic and O(n) on every implementation**:

```
iterator, size, isEmpty, nonEmpty, contains, containsAll, exists, forAll, count, find,
foldLeft, reduceLeft? (no: order) -> reduce(BinaryOperator) only on ordered types,
mkString ×3, forEach, toVector, toList, toSet, stream(), toArray, asJava()   (3.1: O(1) view)
```

(`nonEmpty` stays boolean; the narrowing to `NonEmptyVector` is `Vector.toNonEmptyVector()`, 3.6.)

Everything positional or complexity-sensitive moves to the concrete types: `get`, `update`, `insert`,
`removeAt`, `last`, `init`, `slice`, `take*`, `drop*`, `reverse`, `sorted`, `zip*`, `sliding`,
`grouped`, `scan*`, `head`, `tail`, `indexOf*`, `search`, `padTo`, `patch`, `permutations`,
`combinations`, `crossProduct`, `intersperse`, `rotate*`, `shuffle`, `splitAt`, `startsWith`, `endsWith`.
`Map`/`Set` lose `head`, `tail`, `zipWithIndex`, `sliding`, `scan`, `take`, `drop` etc. (they are
currently declared on `Map.java:842-857` and `Set.java:265-280` over an undefined iteration order).

New on every sequence and on the hash sets (decided; implemented, `Vector`/`NonEmptyVector` with #21, the others with
#25): `partitionMap`, the generalisation of `partition(Predicate)` (Scala 2.13, zio-prelude `ForEach`):

```java
<L, R> Tuple2<Vector<L>, Vector<R>> partitionMap(Function<? super A, ? extends Either<? extends L, ? extends R>> f); // Vector, NonEmptyVector (result sides may be empty)
<L, R> Tuple2<List<L>, List<R>>     partitionMap(...);                                    // List, Queue, LazyList likewise
<L, R> Tuple2<HashSet<L>, HashSet<R>> partitionMap(...);                                  // HashSet, LinkedHashSet
```

Not on `TreeSet` (the two result sides need comparators for `L` and `R`) nor on maps (entries are
tuples; use `entries().partitionMap(...)`). Implemented with two builders (3.8.1), one pass, no
intermediate `Either` list. `Validation.partition` (3.5) is the same idea for validations.

Also new on `Vector` (and `List`, `NonEmptyVector`, since it costs one line each) (decided; on `Vector` and
`NonEmptyVector` since #21, `List` with #24/#25, `Queue` and `Stream` with #25): `duplicates`, the complement of
`distinct`:

```java
Vector<A> duplicates();                                        // elements occurring more than once, each once, in order of first occurrence
<K> Vector<A> duplicatesBy(Function<? super A, ? extends K> key); // same, keyed; the first occurrence of each duplicated key is returned
```

`Vector.of(3, 1, 3, 2, 1, 3).duplicates()` is `Vector.of(3, 1)`; `Vector.of(1, 2, 2, 1).duplicates()` is
`Vector.of(1, 2)` (first-occurrence order, not the order in which the repeats are met). One pass over the
elements with a `java.util.LinkedHashMap<K, A>` of first occurrences plus a `HashSet<K>` of the keys seen
again, the key computed once per element, then one pass over the distinct keys into the builder (sized to
the result); O(n) time; `isEmpty()` on the result is the "all distinct" test, so no separate `isDistinct`
is needed (it is deleted with `Value`).

`flatten` (decided; implemented, `Vector`/`NonEmptyVector` with #21, every other collection but the maps and every
control type with #25), as a **static** method on every collection and control type, because Java cannot
type an instance `flatten()`: Scala's needs evidence that the element type is itself a collection
(`implicit ev: A => IterableOnce[B]`), which a Java method cannot demand of its receiver's type
parameter. Vavr had an unchecked `<U> Value<U> flatten()` (runtime `ClassCastException` on misuse) and
removed it in 2015 for that reason.

```java
static <A> Vector<A>         flatten(Iterable<? extends Iterable<? extends A>> nested);        // Vector.flatten(vectorOfVectors)
static <A> NonEmptyVector<A> flatten(NonEmptyVector<? extends NonEmptyVector<? extends A>> nested);
static <A> HashSet<A>        flatten(Iterable<? extends Iterable<? extends A>> nested);        // HashSet, LinkedHashSet likewise
static <A> Option<A>         flatten(Option<? extends Option<? extends A>> nested);            // Either, Try, Validation, Lazy likewise
```

`List`, `Queue`, `LazyList` likewise. Implemented as `flatMap(identity)` over the builder (3.8.1). Misuse
is a compile error, and the call reads `Vector.flatten(vv)`.

Every positional method on `List` gets a one-line complexity note in its javadoc (`get(i)` is O(i),
`append` is O(n), `prepend`/`head`/`tail` are O(1)); on `Vector` the same (effectively O(1) for `get`,
`update`, `append`, `prepend`, `take`, `drop`).

**Decided while implementing step 1 (#66, `Vector`):**

- **The `Complexity:` convention.** Every positional or complexity-sensitive method carries, in its javadoc, one
  paragraph starting with `Complexity:` (e.g. `Complexity: effectively O(1) (O(log32 n) trie access).`,
  `Complexity: O(n).`, `Complexity: O(k) for k taken elements, then one effectively O(1) take.`). "Effectively
  O(1)" means O(log32 n) on the trie. Methods that only override a `Traversable` default keep `{@inheritDoc}` and
  add the paragraph. `make complexity` (run by `make verify` and by the `complexity` CI job) runs
  `scripts/check-complexity.scala` (Scala, run with scala-cli) over the files listed in the Makefile (`Vector.java` for now; #67 adds `List`,
  `Queue`, `Stream`) and fails when a method whose name is in the script's fixed list of positional names is declared
  without such a line; the files are parsed with the JDK's own compiler (`javax.tools`, `com.sun.source`), so the
  javadoc checked is the doc comment javac attaches to the method, `/** */` or `///` alike.
- **`Iterator`-returning methods stay as they are for now.** `crossProduct()`, `crossProduct(int)`,
  `crossProduct(Iterable)`, `grouped`, `sliding` and `slideBy` on `Vector` keep returning the lazy
  `Iterator<Vector<T>>` / `Iterator<Tuple2<...>>` they returned as a `Seq`, because `Traversable` still declares
  `grouped`/`sliding`/`slideBy` with an `Iterator` result; #68 decides the final shape when `Traversable` is slimmed.
  A lazy result keeps its argument lazy: `crossProduct(Iterable)` memoises `that` with `Stream.ofAll` (as the `Seq`
  default did; `Stream` survives as `LazyList`, #28), so `Vector.of(1).crossProduct(Iterator.from(0)).take(3)` works.
- **`Seq` methods kept on `Vector` although 3.7 does not list them** (unused or slated for deletion elsewhere, kept
  so that nothing changes behaviour or loses a test in this step): `asJava(Consumer)`, `asJavaMutable()`,
  `asJavaMutable(Consumer)` (3.1 deletes the mutable views and the consumer scopes; that is #26's PR),
  `removeAll(Predicate)` (deprecated, `reject`), `iterator(int)`, `containsSlice`, `indexOfSlice`/`lastIndexOfSlice`
  and the `*Option` variants of every index search, `prefixLength`/`segmentLength`, `distinctByKeepLast`,
  `dropRightUntil`/`dropRightWhile`/`takeRightUntil`/`takeRightWhile`, `splitAtInclusive`, `leftPadTo`,
  `reverseIterator`, `unzip`/`unzip3`. `endsWith` takes an `Iterable` (it took a `Seq`).
- **Equality across sequence types is unchanged for now**: a `Vector` equals any ordered sequence (`Vector`,
  `List`, `Queue`, `Stream`) with the same elements in the same order, and vice versa, as it did as a `Seq`
  (`Collections.isSequence`). #68 decides whether that survives once `Seq` is gone.
- **`java.util.List` views without a shared sequence interface.** `JavaConverters.ListView` is abstract over
  the delegate type and calls the positional operations through per-type hooks (`VectorListView`, and
  `SeqListView` for `List`/`Queue`/`Stream` until #67): the view asks the concrete type, no package-private
  mini-`Seq` is reintroduced.
- **`Option`, `Either`, `Try`, `Lazy` and `Tuple.unzip1..8` return `Vector`**, built with one `Vector.Builder` per
  side in one pass (no `Stream` round trip). `Tuple.toSeq()` is renamed `toVector()`, since its result is a `Vector`.
- **Two `Seq`-typed signatures that returned a `Vector` at runtime.** `Iterator.grouped`/`sliding` now say
  `Iterator<Vector<T>>` (their groups always were Vectors, one leaf array each; `Vector.sliding` keeps its cost).
  `Map.scanLeft`/`scanRight` still say `Seq<U>` and build a `List` instead of a `Vector`; both leave `Map` in #68.
- `VectorTest` no longer shares `AbstractSeqTest` (its hooks return `Seq<T>`, which a `Vector` is not): the `Seq`
  cases are folded into `VectorTest`, as #67 does for `ListTest`, `QueueTest` and `StreamTest`; the two `narrow`
  tests of `Seq`/`IndexedSeq` go with the interfaces.

**Decided while implementing step 2 (#67, `List`, `Queue`, `Stream`):**

- **`Seq` methods kept on the three types although 3.7 does not list them**, for the same reason as on `Vector` (unused
  or slated for deletion elsewhere, kept so that nothing changes behaviour or loses a test in this step):
  `asJava(Consumer)`, `asJavaMutable()`, `asJavaMutable(Consumer)`, `removeAll(Predicate)` (deprecated, `reject`),
  `iterator(int)`, `containsSlice`, `indexOfSlice`/`lastIndexOfSlice` and the `*Option` variants of every index
  search, `prefixLength`/`segmentLength`, `distinctByKeepLast`, `dropRightUntil`/`dropRightWhile`/
  `takeRightUntil`/`takeRightWhile`, `splitAtInclusive`, `leftPadTo`, `reverseIterator`, `unzip`/`unzip3`. As on
  `Vector`, `endsWith` takes an `Iterable` (it took a `Seq`), so any sequence, a JDK collection or a one-shot
  iterator is accepted.
- **Where a `Seq` return type was named, the type that was already returned at runtime is declared.**
  `Map.map`, `flatMap`, `collect`, `as`, `zip`, `zipWith`, `zipAll`, `zipWithIndex`, `unzip`, `unzip3` and
  `values()` say `Stream` (they all build one through `Iterator.toStream` / `Stream.ofAll`; `HashMap.values()`
  already said `Stream`). `Map.scanLeft`/`scanRight` say `List` (they finish with `Iterator::toList`).
  `Iterator.slideBy` says `Iterator<Stream<T>>` (its groups always were `Stream`s, unlike `grouped`/`sliding`,
  whose groups are `Vector`s). All of these leave `Map` and `Iterator` in #68.
- **Implementation sources.** Where a `Seq`/`LinearSeq` default was written over `iterator()` or a `Collections`
  helper and is right for the type, the helper is called and its bound generalised from `Seq` to `Traversable`
  (`shuffle`, `removeAll`, `retainAll`, `scanLeft`/`scanRight`, `groupBy`, `reject`, `last`). Where the type has a
  better native implementation it is used: a cons-list walk on `List` (`indexOf`, `indexWhere`, `lastIndexWhere`,
  `segmentLength`, `endsWith`, `ListModule.Slice`, `ListModule.Search`), front/rear on `Queue` (`reverseIterator`
  walks the rear, which is already stored reversed, then the front; everything that needs a single ordered walk
  goes through `toList()`, as `Queue.lastIndexOf` already did), and a lazy cons walk on `Stream`
  (`indexWhere`, `lastIndexWhere`, `segmentLength`, `StreamModule.Slice`, which never computes the length so that
  an infinite `Stream` can still be searched). The four `Seq`-bound helpers that could not be generalised
  (`Collections.asJava`, `crossProduct`, `rotateLeft`, `rotateRight`, `sortBy`) are deleted and inlined on each
  type, as #66 did on `Vector`: `rotateLeft`/`rotateRight` use `Math.floorMod`, so `Integer.MIN_VALUE` no longer
  recurses forever, and `sortBy(comparator, mapper)` is `sorted` over the composed comparator (the same stable sort
  the helper performed).
- **`java.util.List` views**: `JavaConverters.SeqListView` becomes one `ListView` subclass per type
  (`ListListView`, `QueueListView`, `StreamListView`, beside `VectorListView`), each calling its own type's
  methods, so the unchecked casts the shared subclass needed are gone.
- **Cross-type equality is unchanged**: `Collections.isSequence` now names the four sequence types
  (`Vector`, `List`, `Queue`, `Stream`), so a sequence still equals any other sequence with the same elements in
  the same order, and `hashCode` is still the ordered hash. #68 decides whether that survives.
- **`duplicates`/`duplicatesBy` on `List`** have `Vector`'s contract and algorithm (one `LinkedHashMap` of first
  occurrences plus a `HashSet` of the keys seen again, one pass, the key computed once), building a `List`. A null
  key is handled: an element is never null, so `putIfAbsent` still tells a first occurrence from a repeat.
- **The complexity list grows with the three types' own names**: `scripts/check-complexity.scala` adds
  `asJavaMutable`, `length`, `reverseIterator`, `containsSlice`, `indexOfSlice`, `lastIndexOfSlice`,
  `prefixLength`, `segmentLength`, `splitAtInclusive`, `distinctByKeepLast`, `dropRightUntil`/`dropRightWhile`/
  `takeRightUntil`/`takeRightWhile`, `duplicates`/`duplicatesBy`, the stack names (`peek`, `peekOption`, `pop`,
  `popOption`, `pop2`, `pop2Option`, `push`, `pushAll`), the queue names (`enqueue`, `enqueueAll`, `dequeue`,
  `dequeueOption`) and the lazy-list names (`cycle`, `extend`, `appendSelf`). The `*Option` index variants stay
  out: they are one-line wrappers whose javadoc points at the method that carries the cost. On a lazy type the
  note says what is forced and what is deferred (`Complexity: lazy; each element is forced when the result reaches
  it.`, `Complexity: O(n); the whole Stream is forced, so it does not terminate on an infinite Stream.`).
- **`AbstractSeqTest` and `AbstractLinearSeqTest` are deleted**, their cases folded into `ListTest`, `QueueTest`
  and `StreamTest`, each now extending `AbstractTraversableRangeTest` as `VectorTest` does. Every case is kept per
  type; the only cases that go are the two `narrow` tests of `Seq` and `LinearSeq`, which go with the interfaces
  (each type keeps its own `narrow` test). Where `StreamTest` already overrode a shared case for laziness, its
  version is the one kept. Each test class gains a `TraversableOnlyTests` case that walks the reflective supertype
  chain, so no sequence interface can come back above `Traversable` unnoticed.

**Decided while implementing step 3 (#68, `Traversable`, `Set`, `Map`, `Iterator`):**

- **`Traversable` declares exactly** (decided): `iterator()` (a `java.util.Iterator`), `size`, `isEmpty`, `nonEmpty`,
  `contains`, `containsAll`, `exists`, `forAll`, `count`, `find`, `foldLeft`, `mkString` ×3, `forEach`, `toVector`,
  `toList`, `toSet`, `stream()`, `toArray()`/`toArray(IntFunction)`, `asJava()`, plus `spliterator()` (an `Iterable`
  member, overridden so that `stream()` reports the size and the `DISTINCT`/`ORDERED` characteristics of the type,
  `SORTED` with the set's comparator on a `SortedSet`, and no `SORTED` on a `SortedMap`, whose keys are ordered but
  whose entries are not sorted in their own order) and the static `narrow`. `Foldable` and `Ordered` are deleted (`SortedSet`/`SortedMap` declare `comparator()`
  themselves); `Set` no longer extends `Predicate` and `Map` has no function supertype. The equality contract
  (sequences equal sequences element by element in order, sets equal sets, maps equal maps, `hashCode` ordered or
  summed accordingly) is stated in the interface javadoc, and `AbstractTraversableTest` asserts the exact member set
  reflectively, so no member can come back unnoticed. Everything else `Traversable` declared is declared by the
  concrete types with the same names and semantics: the sequences take all of it, the sets and maps what is
  order-agnostic (below). The bodies live once in `TraversableModule` (a package-private helper next to the
  interface; an unexported `collection.internal` type since #73, 3.1), each type declaring a one-line delegation with its own javadoc; the `toJava*` copies and the `to*`
  conversions move with the rest and are deleted by #26, as 3.1 says.
- **`toArray`, not `toJavaArray`** (decided): the JDK's own name (`Collection.toArray`, `java.util.stream.Stream.toArray`),
  with the same two overloads as the JDK stream (`toArray()` to `Object[]`, `toArray(IntFunction<T[]>)` to a typed
  array); unlike `toList`/`toJavaList` there is no Zazr array type the `Java` prefix would tell apart. The deprecated
  `toJavaArray(Class)` is deleted with it. `toJavaStream()` is `stream()`, as 3.1 wanted; `toJavaParallelStream()`
  moves to the concrete types until #26.
- **`Traversable.asJava()`** (decided) returns an O(1) unmodifiable `java.util.Collection<T>` view
  (`JavaConverters.CollectionView`: the delegate's iterator and size, every mutator throws
  `UnsupportedOperationException` whether or not it would change anything, as `Collections.unmodifiableCollection`
  does); the sequences keep their `java.util.List` view as a covariant override.
  Consequence for 3.1: a map's `asJava()` is that `Collection<Tuple2<K, V>>` (a `java.util.Map` is not a
  `Collection`, so it cannot be the override), and the `java.util.Map` views of 3.1 take the name `asJavaMap()` in
  #26; the set views stay `asJava()` (a `java.util.Set` is a `Collection`).
- **The Zazr `Iterator` leaves the public API** (decided): `interface Iterator<T> extends java.util.Iterator<T>,
  Iterable<T>` is package-private in `com.guizmaii.zazr.collection` (an unexported `collection.internal` type since
  #73, 3.1), no longer a `Traversable`, and keeps only what
  the internals compose: the `range*`/`from`/`continually`/`iterate`/`unfold*`/`tabulate`/`fill`/`concat`/`of`/`ofAll`
  factories, the lazy `map`, `filter`, `collect`, `flatMap`, `take*`, `drop*`, `zip*`, `intersperse`,
  `distinctBy`/`distinctByKeepLast`, `scanLeft`, `sliding`, `slideBy` (its runs are `Vector`s now), `span`, and the
  one-pass `foldLeft`, `find`, `headOption`, `mkString`, `toList`/`toVector`/`toQueue`/`toStream`. The
  `AbstractIterator.next()` funnel still rejects a null element. `Iterator.ofAll(x)` returns `x` itself when it
  already is one, so `Iterator.ofAll(this).map(...)` inside a collection allocates no wrapper: every public
  `iterator()` returns a `java.util.Iterator` and the internals re-enter the helper through `ofAll`. `IteratorTest` is
  a package-private test of the helper (in `collection.internal` with it since #73). The control types, `Lazy`, `zazr-test` (`Gen`: a stateful lambda for
  `of(seed, next)`, a toggle for `intersperse`, `Vector` for `choose`/`frequency`) and `zazr-benchmark` no longer
  import it.
- **Own-type results for the windows and the products** (decided): `grouped`, `sliding` ×2 and `slideBy` return
  `Vector<Vector<T>>`, `List<List<T>>`, `Queue<Queue<T>>`, `Stream<Stream<T>>`; `crossProduct()` returns
  `X<Tuple2<T, T>>`, `crossProduct(int)` `X<X<T>>`, `crossProduct(Iterable)` `X<Tuple2<T, U>>`. They are eager on
  `Vector` (each window or run is an effectively O(1) slice sharing the leaves; `crossProduct(Iterable)` walks
  `that` once into a `Vector`), `List` and `Queue` (each window copied), and lazy on `Stream` in both dimensions:
  head-strict as every `Stream` operation is, so the first window is built when the method is called and each
  further one when the result reaches it, and `that` is memoised as a `Stream` so that an infinite argument works
  with `take`. The window rule is unchanged (`[1, 2, 3, 4, 5].sliding(2, 4)` is `[[1, 2], [5]]`; a window whose
  elements all belong to the previous one is not produced). The sets and maps lose them. `reverseIterator()` and
  `iterator(int)` are deleted everywhere (`reverse().iterator()`, `drop(n).iterator()`); `Vector` and `Queue` keep
  a package-private `reverseIterator()` for `Collections.reverseIterator`, which `prependAll` and `scanRight` use,
  because it is O(1) to create on both (since #73, 3.1, `Collections.reverseIterator` uses the public API instead:
  `Queue.reverseIterator` is deleted and `Vector.reverseIterator` is private).
- **Cross-type sequence equality stays** (decided, final): `Vector`, `List`, `Queue` and `Stream` equal each other
  element by element in order with the ordered `hashCode` (`Collections.isSequence`); sets equal sets, maps equal
  maps, and no kind equals another.
- **What `Set` keeps** (decided): the set algebra (`add`, `addAll`, `remove`, `removeAll(Iterable)`, `union`,
  `intersect`, `diff`, `contains`), `filter`, `reject`, `map`, `flatMap`, `collect(Function)`, `as`, `partition`,
  `groupBy`, `orElse` ×2, `tap`, `replace`, `replaceAll` (the same thing on a set), `retainAll`, `existsUnique`,
  `max`/`maxBy` ×2/`min`/`minBy` ×2, `sum`/`product`/`average`, `fold`/`reduce`/`reduceOption`, `single`/
  `singleOption`, `arrangeBy`, `collect(Collector)` ×2, the `toJava*` and `to*` conversions, `toJavaSet()`;
  `SortedSet` adds `comparator()` and the comparator-taking `map`/`flatMap`/`collect`. **What it drops**: `head`,
  `headOption`, `last`, `lastOption`, `init`, `initOption`, `tail`, `tailOption`, `take`/`takeRight`/`takeUntil`/
  `takeWhile`, `drop`/`dropRight`/`dropUntil`/`dropWhile`, `zip`/`zipAll`/`zipWith`/`zipWithIndex` ×2, `unzip`/
  `unzip3`, `sliding` ×2, `slideBy`, `grouped`, `scan`/`scanLeft`/`scanRight`, `span`, `distinct` (a set is
  distinct), `distinctBy` ×2 (which representative survives depends on the iteration order), `foldRight`,
  `reduceLeft`/`reduceRight` and their `Option` variants (they name a direction), `findLast`, `forEachWithIndex`,
  `length` (`size`), `test` (with the `Predicate` supertype). A `TreeSet` thus has no `head`/`last`: its first
  element is `iterator().next()`, and the `NavigableSet` view of #26 gives `first()`/`last()`.
- **What `Map` keeps** (decided): the map API (`get`, `getOrElse`, `put` ×4, `remove`, `removeAll` ×2, `containsKey`,
  `containsValue`, `keySet`, `values()` as a `Vector<V>` in iteration order, `filter`/`reject` in both the
  `Predicate` and the `BiPredicate` form, `filterKeys`/`rejectKeys`/`filterValues`/`rejectValues`, the deprecated
  `removeKeys`/`removeValues`, `map(BiFunction)`, `mapBoth`, `mapKeys` ×2, `mapValues`, `flatMap(BiFunction)`,
  `collect(BiFunction)`, `merge` ×2, `replace(K, V, V)`, `replaceValue`, `replaceAll(BiFunction)`, the entry-typed
  `replace`/`replaceAll`, `retainAll`, `partition`, `groupBy`, `orElse` ×2, `tap`, `forEach(BiConsumer)`,
  `computeIfAbsent`/`computeIfPresent`, `toJavaMap()`), plus `existsUnique`, `max`/`maxBy` ×2/`min`/`minBy` ×2 over
  the entries, `fold`/`reduce`/`reduceOption`, `single`/`singleOption`, `arrangeBy`, `collect(Collector)` ×2 and
  the conversions; `SortedMap` adds `comparator()` and the comparator-taking forms. **What it drops**: the same
  positional names as `Set`, `length`, `sum`/`product`/`average` (entries are never numbers), and every
  sequence-shaped method step 2 had typed `Stream`: `map(Function)`, `flatMap(Function)`, `collect(Function)`,
  `as`, `zip`/`zipWith`/`zipAll`/`zipWithIndex` ×2, `unzip` ×3, `unzip3` ×2, `scanLeft`/`scanRight`, together with
  `keysIterator`, `valuesIterator` and `iterator(BiFunction)` (`keySet()` and `values()` are the replacements).
- **`fold`, `reduce`, `reduceOption` on the sets and the maps** combine the elements in the iteration order, which a
  `HashSet`/`HashMap` does not define: their javadoc says the operation should be associative and commutative for
  the result to be independent of it. On the sequences `reduce` is `reduceLeft`.
- **The complexity guard is unchanged**: the six set and map files stay out of `COMPLEXITY_FILES` because after this
  step they declare no positional member; the names they still share with the guard's list (`remove`,
  `removeAll`, `replace`, `replaceAll`, `retainAll`, `get`, `iterator`) are keyed lookups on a set or a map, not
  positions. `crossProduct`, `grouped`, `sliding` and `reverseIterator` keep their notes on the sequences.
- **Tests**: `AbstractTraversableTest` is rewritten as the shared class of the 3.7 members (plus the factories every
  type has, `empty`/`of`/`ofAll`/`tabulate`/`fill`/`collector`, equality and `toString`), run by all ten concrete
  test classes through the same factory hooks (`IntMap`, the `Traversable` view of a map's values, is trimmed to the
  slim interface); `AbstractTraversableRangeTest`, `AbstractSetTest`, `AbstractSortedSetTest`, `AbstractMapTest` and
  `AbstractSortedMapTest` are folded into the per-type classes, every case of a surviving member kept, the cases of
  the dropped members removed with them.
- **Every method taking an `Iterable` reads it once** (`Queue.ofAll`, `TreeSet.ofAll`, `Vector.appendAll`/`prependAll`,
  `Stream.appendAll`, the `lastIndexOfSlice` of `List`/`Queue`/`Stream` and `Iterator.concat(Iterable<Iterable>)`
  answered the emptiness from a first `iterator()` and read from a second), so a one-shot `Iterable` such as
  `java.util.stream.Stream.of(1, 2)::iterator` is accepted everywhere, `crossProduct(Iterable)`, `insertAll`,
  `patch` and `Stream.concat` included. The pattern: a `Traversable` or a `java.util.Collection` argument keeps
  the O(1) emptiness fast path; any other `Iterable` is materialised first and the copy answers the emptiness.
- **`Option.forEach`, `Either.forEach`, `Try.forEach`** loop over the elements straight into the `Vector.Builder`
  (the mapper stops at the first `None`/`Left`/`Failure`, as before, and a null result is rejected by name).

**Positional subset on the ordered sets and maps (#72, decided).** Step 3 took every positional method off `Set` and
`Map` because a `HashSet`/`HashMap` iterates in hash order, which the type does not promise. The ordered sets and maps
do promise an order, insertion order on `LinkedHashSet`/`LinkedHashMap` and the comparator's order on
`TreeSet`/`TreeMap`, so they get a positional subset back, where `head` is the first element (the minimum, or the first
inserted), `take(n)` the first n, and so on:

- **Members**: `head`, `headOption`, `last`, `lastOption`, `init`, `initOption`, `tail`, `tailOption`, `take`,
  `takeRight`, `takeWhile`, `takeUntil`, `drop`, `dropRight`, `dropWhile`, `dropUntil`, `zipWithIndex` (a
  `Vector<Tuple2<T, Integer>>`, of entries on the maps), `sliding` ×2, `grouped` and `slideBy` (a `Vector` of the
  receiver's type, with the sequences' window rule). They are declared on `SortedSet` and `SortedMap` (implemented by
  `TreeSet`/`TreeMap` with their own return types; the interfaces say `Vector<? extends SortedSet<T>>` and
  `Option<? extends SortedSet<T>>` where the type is nested) and on the final classes `LinkedHashSet` and
  `LinkedHashMap`. Nothing is added to `Set`, `Map`, `HashSet`, `HashMap` or `Traversable`; `HashSetTest` and
  `HashMapTest` assert reflectively that none of these names is a member of `HashSet`, `HashMap`, `Set` or `Map`. An
  empty receiver throws `NoSuchElementException` from `head`/`last` and `UnsupportedOperationException` from
  `init`/`tail` ("head of empty TreeSet", as `Vector` words it); `take`/`drop` clamp `n` as `Vector` does: a count of
  zero or less keeps nothing for `take`/`takeRight` and everything for `drop`/`dropRight`, a count of `size()` or more
  the reverse. The sorted results keep the receiver's comparator.
- **`TreeSet`/`TreeMap`: a rank split of the red-black tree.** `RedBlackTreeModule.Node.splitAt(tree, n)` is
  `split(tree, value)` with the left subtree's size (a field of every node) in place of the comparison; it is built
  from the one-sided `take(tree, n)` and `drop(tree, n)`, one descent and a `join` per level each, so O(log n), and the
  operations that need one half call that half alone (no `Tuple2`, no second half built). `take`, `takeRight`,
  `drop`, `dropRight`, `init` and `tail` are O(log n); `head`/`last` walk the leftmost or rightmost path (O(log n));
  `takeWhile` and its siblings walk the prefix once, then split once (O(k + log n)); every window or run of `sliding`,
  `grouped` and `slideBy` is one rank slice sharing the untouched subtrees, so O((n / step) log n), respectively
  O(n + r log n) for r runs, with the classifier called once per element. A `TreeMap` is a tree of entries under the
  key comparator, so it uses the same functions. `RedBlackTreeTest` splits trees of sizes 0 to 70 (random, ascending
  and after deletions, a fixed seed) at every rank and checks the red-black invariants of both halves, including the
  stored `blackHeight` and `size` fields that later joins rely on.
- **`LinkedHashSet`/`LinkedHashMap`: a slice of the insertion order.** A `LinkedHashMap` pairs a `HashMap` of slots
  (entry and absolute index) with a `Vector` of keys in insertion order, where a removed key leaves a marker until the
  markers outnumber the entries. A positional result slices that `Vector` and moves the offset, so every kept slot
  stays valid, and changes the `HashMap` by the smaller side: rebuilt from the kept keys or the dropped keys removed.
  `head`/`last` are effectively O(1) (the order never starts or ends with a marker), `init`/`tail` too plus a walk
  past the markers next to the removed end; `take`/`drop` and their right-hand forms are effectively
  O(min(n, size - n)), plus a walk from the nearer end past the markers when there are any. The windows and runs
  first drop the markers (O(n), only if there are any), so each window is found by index. `LinkedHashSet` is a
  `LinkedHashMap<T, Object>` and delegates.
- **Complexity guard**: `SortedSet.java`, `SortedMap.java`, `LinkedHashSet.java` and `LinkedHashMap.java` join
  `COMPLEXITY_FILES`, and `slideBy` joins the script's names (`headOption`/`lastOption` stay out, one-line wrappers
  like the `*Option` index variants). This reverses the step-3 note for these four files: the keyed members they share
  with the list (`get`, `remove`, `removeAll`, `replace`, `replaceAll`, `retainAll`, `iterator`) now carry their
  `Complexity:` line too. The notes of `TreeSet`/`TreeMap` live on the interface declarations, which the classes
  inherit.

**Decided while implementing #25 (`partitionMap`, `duplicates`, static `flatten`):**

- **Where they are.** `partitionMap` on `List`, `Queue`, `Stream` (`LazyList` after #28), `HashSet`, `LinkedHashSet`
  (beside `Vector` and `NonEmptyVector`), each side of the receiver's own type; not on `TreeSet` nor on the maps, as above.
  `duplicates`/`duplicatesBy` on `Queue` and `Stream` too, because both have `distinct`/`distinctBy`; the sets have
  no `distinct` (a set is distinct) and get neither. Static `flatten(Iterable<? extends Iterable<? extends A>>)` on
  `List`, `Queue`, `Stream`, `HashSet`, `LinkedHashSet`, and on `TreeSet` in two overloads,
  `flatten(Comparator, Iterable)` and the natural-order `flatten(Iterable)` (for `T extends Comparable<? super T>`),
  mirroring `TreeSet.ofAll`; of equal elements the last one met is kept, as `ofAll` keeps it. The maps get no
  `flatten`, for the reason they get no `partitionMap` (their elements are entries). On the control types:
  `Option.flatten(Option<? extends Option<? extends A>>)`, `Either.flatten(Either<? extends L, ? extends Either<?
  extends L, ? extends R>>)`, `Try.flatten`, `Validation.flatten(Validation<? extends E, ? extends Validation<?
  extends E, ? extends A>>)` and `Lazy.flatten`.
- **Implementation.** "`flatMap(identity)` over the builder" is how the result reads, not literally the code: only
  `Vector` has a builder yet (3.8.1, #27), so each type accumulates as its own `partition` and `ofAll` do (a reversed
  cons list for `List` and `Queue`, the private `addAll` path of `ofAll` for `HashSet`/`LinkedHashSet`, `ofAll` over
  the concatenation for `TreeSet`). Each reads its argument once, outer and inner iterables alike, so one-shot
  iterables work. `partitionMap` switches on the `Either` records, one pass, no list of `Either`s, and rejects a null
  result naming the type (`List.partitionMap: f returned null`). `duplicatesBy` on `Queue` and `Stream` runs the
  `Vector` algorithm over the receiver itself (`Collections.duplicatesBy` in `collection.internal`, no `toList()`
  copy first). The control types switch on the outer record and return the inner instance or the outer failure
  unchanged; `Validation.flatten` accumulates nothing, since the outer is either `Invalid` with its own errors or
  `Valid` with the inner validation as its value, never both. `Lazy.flatten` evaluates nothing when called: the
  result evaluates the outer, then the inner, on its first `get()` (the one outer type that may hold null, rejected
  there as `Lazy.flatten: the outer Lazy holds null`).
- **`Stream` stays lazy** (decided). `Stream.partition` is two lazy `filter`s, so `partitionMap` is lazy too, with
  the mapper called once per element: both sides read one memoised `Stream` of the mapper's results (`map`, whose
  tail is a `Lazy`), each skipping the other side's values when it reaches them. That memoised `Stream` of `Either`s
  is the one intermediate structure, and it is what laziness requires: a lazy list has no builder (3.8.1), and the
  second side must see the results the first side has already computed. Every `Stream` is head-strict, so each side
  is forced to its first element by the call; on an infinite `Stream` where one side never receives an element the
  call does not return, exactly as `partition`'s second `filter` does. `Stream.flatten` is lazy in both dimensions
  (the internal `FlatMapIterator` behind `Stream.flatMap`, not `Stream.concat(Iterable)`, which opens every inner
  iterable up front): an infinite outer or inner iterable works with `take`, and only an outer with infinitely many
  empty inner iterables and nothing after them never yields. `Stream.duplicates` is eager and does not terminate on
  an infinite `Stream`: in first-occurrence order, whether an element is emitted, and so what comes after it, is
  known only at the end.
- **Complexity notes.** Every new method carries a `Complexity:` line (and `Vector.partitionMap`/`Vector.flatten`
  gained theirs). `partitionMap` and `flatten` are not added to the guard's list in `scripts/check-complexity.scala`:
  they are whole-collection operations, O(n) on every type, not positional ones; `duplicates`/`duplicatesBy` already
  were in it, so the new `Queue` and `Stream` ones are checked.

**Complexity page and vocabulary (#80, decided).** The `Complexity:` notes are the source of a generated documentation
page, `docs/collections/complexity.md`, the Zazr counterpart of Scala's performance-characteristics table, exhaustive
and unable to drift:

- **Every note starts with an expression of a fixed vocabulary** (`O(1)`, `effectively O(1)`, `amortised O(1)`,
  `O(log n)`, `lazy`, and the listed `O(...)` forms in n, m, k and the named arguments), each mapped to a class of the
  legend (constant, effectively constant, amortised constant, logarithmic, lazy, linear, n log n, polynomial,
  combinatorial); free text follows. `scripts/check-complexity.scala` owns the list and fails on a note outside it, so a
  new expression is added there, with its class, before it is used. Every note of an API method is checked this way,
  whatever the method's name (`partitionMap`, the static `flatten`, the `*Option` wrappers), and appears on the page. A note that referred to another method ("that of
  `take(int)`") now states its class first and keeps the reference as the explanation.
- **The guard covers every collection and the set and map core operations.** `NonEmptyVector`, `HashSet`, `HashMap`,
  `TreeSet` and `TreeMap` join `COMPLEXITY_FILES`, and the names grow with `contains`, `concat`, `add`, `addAll`, `put`,
  `min`, `max`, `union`, `intersect`, `diff`, `containsKey`, `keySet`, `values`. Only API members are checked
  (public or protected, or not private in an interface). A method without its own note passes when it overrides a
  supertype declaration that has one (the `TreeSet`/`TreeMap` notes stay on `SortedSet`/`SortedMap`); `Traversable`,
  `Set` and `Map` are read for that chain without being checked themselves, and the defaults the page reaches through
  them (`Traversable.contains`, `Set.min`/`max`) carry a note.
- **The page** (`make docs-complexity`) has a legend comparing the classes with Scala's `C`/`eC`/`aC`/`Log`/`L`,
  matrices for the sequences, sets and maps (`n/a` where a type does not declare the operation; overloads split into
  rows only when their costs differ; the whole note as the cell's tooltip), then every documented method per type. It
  also writes one table per type under `docs/collections/.costs/`, included by the collection pages.
  `make docs-complexity-check` (in `make verify` and CI) regenerates both and fails when the committed files differ.
- Facts the page made visible, stated in the notes rather than changed here: `min()`/`max()` on the sets use the
  natural order of the elements and walk them all, including on a `TreeSet` (whose least and greatest elements in its
  own order are `head()` and `last()`, O(log n)).

Which concrete collections survive (decided):

| Keep | Why |
|---|---|
| `Vector` | the default sequence (like `Chunk`) |
| `NonEmptyVector` | new, 3.6 |
| `List` | cons list, O(1) head/tail, `switch` on `Cons`/`Nil` |
| `HashMap`, `HashSet` (HAMT) | the default map/set |
| `TreeMap`, `TreeSet` (red-black) | sorted map/set |
| `LinkedHashMap`, `LinkedHashSet` | insertion order; recent perf work landed here |
| `Stream` renamed `LazyList` | lazy memoising list; the rename avoids the `java.util.stream.Stream` clash |
| `Queue` | banker's queue over two `List`s, O(1) amortised `enqueue`/`dequeue`; kept (decided). `AbstractQueue` is folded into it once `PriorityQueue` is gone |

| Delete (or move to a separate module later) | Why |
|---|---|
| `Array` | `Vector` covers it; one fewer sequence type to keep in sync |
| `CharSeq` (3,566 lines) | a `String` wrapper; Java's `String` + `Vector<Character>` cover it |
| `PriorityQueue` | leftist heap; niche, no internal users. Reinstate on demand as a single file |
| `Tree` | niche |
| `BitSet` | niche |
| `Multimap`, `HashMultimap`, `LinkedHashMultimap`, `TreeMultimap`, `SortedMultimap`, `AbstractMultimap`, `Multimaps` (3,896 lines) | `Map<K, Vector<V>>` / `Map<K, HashSet<V>>` with `groupBy` covers most uses. Scala's stdlib has only a deprecated mutable `MultiMap` mixin and points to `MultiDict` in the optional `scala-collection-contrib`; same move here: out of `zazr-core`, back as a `zazr-multimap` module if wanted |
| `Iterator extends Traversable` (2,651 lines) | a mutable single-pass object implementing the persistent-collection interface. Replace with a package-private helper over `java.util.Iterator` |
| `toJava*` copies, `asJavaMutable`, `asJava(Consumer)` scopes | keep only the `asJava()`/`asJavaMap()` O(1) views (3.1, implemented by #26: `SetViews`, `MapViews`, `TreeViews` beside `JavaConverters`) |

Decided as above: `Queue` survives; `Array`, `CharSeq`, `Tree`, `BitSet`, `PriorityQueue` and the `Multimap`
family are deleted from `zazr-core`. Nothing that survives depends on anything deleted, so any of them can
come back later as a file or a module without touching the core.

### 3.8 `Vector` builder

**Decision.** Add a mutable, single-owner `Vector.Builder<A>` and route every bulk operation through it.

Today's cost model (verified): `append(x)` allocates a cons cell, an `IterableWithSize`, an iterator and
then path-copies (`Vector.java:738` → `BitMappedTrie.appendAll`). `map` allocates one full-size flat
array, then `BitMappedTrie.ofAll` regroups it into 32-wide leaves (a second full copy) and builds the
internal levels (`BitMappedTrie.java:355-365, 86-93`). `filter` allocates a full-size array, copies the
survivors into a range copy, then regroups (three copies). `flatMap`, `ofAll(Iterable)` (non-collection
input) and `collector()` first fill an `ArrayList`/`Object[]`, then do the same regrouping.

The Scala 2.13 `VectorBuilder` (`scala/collection/immutable/Vector.scala:1397`) avoids all of that. Its
state is six arrays `a1..a6` (one per trie level), `len1` (fill of the current leaf) and `lenRest`:

```scala
def addOne(elem: A): this.type = {
  if(len1 == WIDTH) advance()      // current leaf full: allocate a new leaf and link it into a2..a6
  a1(len1) = elem.asInstanceOf[AnyRef]
  len1 += 1
  this
}
```

`advance()` computes `xor = (lenRest + WIDTH) ^ lenRest` and uses it to decide how many levels need a
fresh array, allocating exactly those. `addAll(vector)` copies whole leaf arrays with `System.arraycopy`
(`addArr1`, `addArrN`), and `result()` hands the arrays over to the immutable `Vector` **without
copying** (only the last, partially-filled leaf is trimmed with `copyIfDifferentSize`). Elements are
written once, into their final leaf.

For zazr:

```java
public static <A> Builder<A> newBuilder();
public static <A> Builder<A> newBuilder(int sizeHint);
public static final class Builder<A> {
    public Builder<A> add(A a);
    public Builder<A> addAll(Iterable<? extends A> as);    // fast path: Vector -> leaf-array copy
    public int size();
    public Vector<A> result();                             // single-shot: throws on further add()
}
```

Single-shot (no `clear()`/reuse) is the lazy choice: it makes the "who owns the arrays after `result()`"
question disappear. Scala's reusable builder needs `copyIfDifferentSize` and `alignTo`; not needed.

Internally, `Builder` is the source of truth for: `ofAll(Iterable)` when the input is not a `Vector` or
sized collection, `collector()`, `map`, `filter`, `reject`, `flatMap`, `collect`-free `filter+map`
pipelines, `zip*`, `distinct*`, `scan*`, `tabulate`, `fill`, `unfold*`, `appendAll(Iterable)` when the
argument is large, and `NonEmptyVector` equivalents. `map`/`filter` on the existing trie can keep their
leaf-visiting loops but write into the builder instead of a flat array; that removes the `ofAll` regroup
copy and the `copyRange` copy.

Keep `BitMappedTrie`'s primitive-leaf specialisation (`ArrayType`) for `ofAll(int[])` etc., but stop
selecting it by catching `ClassCastException` in `appendAll`/`prependAll` (`BitMappedTrie.java:136-143`):
the builder always produces `Object[]` leaves, and `ofAll(int[])` is the only primitive entry point.

**Implemented (PR #4), with two corrections measured on the way (decided):**
- `filter` and `reject` stay on the trie's flat-array filter, not the builder. It keeps primitive leaves
  unboxed, and the builder version was 2x slower at 1 000 elements on `Object[]` receivers (2.25 vs
  1.14 µs, 2 forks) and equal at 100 000, whatever the builder's fixed cost. `map` uses the builder only for
  primitive-backed receivers, where it is 1.5x faster at 100 000 (351 → 227 µs, 3 forks); on `Object[]`
  receivers two independent 3-fork runs measured the flat-array map on par at 100 000 and 1.2x faster at
  1 000, so it stays on the trie. Likewise `ofAll` of any sized,
  traversable-again source (`Collection`, `Array`, `List`...) keeps the flat-array path (`ofAll(Array)` was
  2x slower through per-element adds); the builder is for one-shot and unsized sources, where it removes the
  intermediate `List`/`ArrayList` (`ofAll(Iterator)` 2.7-3.1x, `ofAll(Stream)` 4-7x, `flatMap` 2.1-2.5x).
  `collect`, `distinct*`, `zip*`, `intersperse`, `scan*` and `unfold*` reach the builder through
  `ofAll(Iterator)`; nothing else needs routing.
- The `ClassCastException` catch blocks stay for now: the trie paths this PR keeps (`append`, `prepend`,
  `update`, `appendAll` of a sized source) rely on them to fall back from primitive leaves. Their removal
  is deferred to the follow-up that reworks the primitive specialisation to detect the element type up front.

**Decided: builder over the existing `BitMappedTrie` first; finger tree deferred.** Scala's `Vector` since 2.13.2 is a radix-balanced finger tree (`Vector0..Vector6` with
`prefix1`, `data`, `suffix1`) giving amortised O(1) append *and* prepend without the `offset` trick and
with much cheaper `tail`/`init`. Porting it is ~2,500 lines of dense code. The builder lands first over the
existing `BitMappedTrie`, with JMH numbers; the finger tree is reconsidered only if those numbers say so.

#### 3.8.1 Builders for the other collections

**Decision.** Every persistent collection gets a nested `static final class Builder` with the same
shape as `Vector.Builder` (`add`/`put`, `addAll`/`putAll`, `size`, single-shot `result()`, not
thread-safe), and every `ofAll`/`ofEntries`/`collector()`/`map`/`filter`/`flatMap`/`groupBy`/
`partition`/`distinct` is implemented over it. There is **no shared `Builder` interface**: the JDK's
`java.util.stream.Collector` is the generic accumulation abstraction, and each `X.collector()` is a
`Collector` over `X.Builder` (combiner: `left.addAll(right.result())`).

What each one replaces, and how (Scala 2.13 is the reference for all of them):

| Collection | Today (Vavr) | Zazr builder |
|---|---|---|
| `HashMap`, `HashSet` | `ofEntries`/`ofAll` do one persistent `put` per entry (`HashMap.java:511-514`, `HashSet.java:170`): each put path-copies 1..7 `IndexedNode`/`ArrayNode` arrays via `arraycopy`. | **Transient HAMT**, the Clojure/Scala-CHAMP pattern (`HashMapBuilder`, `HashMap.scala:2218`): nodes created by the builder carry an owner token and are mutated in place; foreign nodes are copied on first touch; after `result()` the root is handed to the immutable map and the builder is marked `aliased`, so any further `add` copies first. Needs an owner field on `IndexedNode`/`ArrayNode`/`LeafList` in `HashArrayMappedTrie` (internal, so contained). Biggest win after `Vector`: `groupBy`, `distinct`, `HashMap.collector()`, `map`/`filter` on maps all go from O(n log32 n) allocations to O(n / 32). |
| `TreeMap`, `TreeSet` | `createTreeMap` does one persistent `insert` per entry (`TreeMap.java:1512-1515`), each allocating O(log n) nodes plus rebalancing. | **Sort-then-build**: buffer entries into an array, on `result()` stable-sort with the comparator, drop adjacent duplicate keys keeping the last, then build the balanced tree bottom-up in O(n) (port of `RedBlackTree.fromOrderedEntries`, `RedBlackTree.scala:956`, ~20 lines: recursive split, black nodes, red leaves only at the deepest level). Total O(n log n) compares, one array plus exactly n nodes. Scala's alternative, in-place `mutableUpd` on builder-owned nodes, is more code for the same result; not needed. Also gives the `ofEntries(alreadySorted)` O(n) fast path. |
| `LinkedHashMap`, `LinkedHashSet` | HashMap plus a `Vector` of insertion order with tombstone slots (recent commits `37e4fc110`, `dc152270a`). | Composite: `HashMap.Builder` + `Vector.Builder`. |
| `List` | `ofAll` prepends back-to-front over a `java.util.List` (`List.java:259-264`), optimal; other iterables reverse first (2n cells). | Array buffer, then build back-to-front: n cells + one array. Scala's `ListBuffer` trick (mutating the last cell's `tail`, `ListBuffer.scala:118`) is unavailable because `Cons` is a record. |
| `NonEmptyVector` | new | none; `NonEmptyVector.fromVector(builder.result())` returns `Option`, or `NonEmptyVector.fromIterable(head, vector)`. |
| `LazyList` | lazy | none (a builder would force it). |

Order of implementation: `Vector.Builder` (3.8), then `TreeMap`/`TreeSet` (cheap, self-contained), then
the transient HAMT (the only one that touches a data-structure's node types), then the composites.
Each comes with a JMH before/after on `ofAll`, `collector()`, `map`, `groupBy`.

### 3.9 Null, equality, serialisation

- **`Some(null)` is forbidden.** `Option.some(null)` throws; `Option.ofNullable(null)` is `None`. This is
  not a ZIO lesson: Scala's `Some(null)` is legal (`Option(null)` is `None`) and ZIO/zio-prelude keep it.
  It is the JDK's choice, `Optional.of(null)` throws and `Optional.ofNullable` is the escape hatch, and
  it is what Scala 3 does under `-Yexplicit-nulls`. Java has no explicit-nulls mode, so the record
  constructor plus JSpecify (`record Some<A extends @NonNull Object>`) is the nearest equivalent. In Java
  `Some(null)` almost always arrives by accident (`Option.some(map.get(k))`), and Vavr's own
  `Option.java:508` documents the value as "may be null" without any caller ever wanting that.
- **Collections reject null elements, keys and values too (decided 2026-09-16).** Forbidding `Some(null)`
  while allowing null elements left `find`, `headOption`, `Map.get` and every other `Option`-returning
  method with no representable answer for a stored null (PR #47 had to make them throw). The JDK's own
  immutable collections (`List.of`, `Set.of`, `Map.of`) reject null since Java 9, and "absence is an
  `Option`, not a null" is the library's message. Every constructor, factory, `ofAll`, builder, insertion
  and update path under `com.guizmaii.zazr.collection` throws `NullPointerException` on a null element,
  key or value; the entry-level detours and the "throws on a stored null" javadoc from #47 are then
  removed. Tuples are not collections and keep allowing null components.
  Same for `Right(null)`, `Success(null)`, `Valid(null)`: records with `requireNonNull` in the compact
  constructor. Collections keep allowing null elements (Java's do), but document it.
- **`Try.Failure` equality** stops comparing stack traces (`Try.java:1482`). Two failures are equal when
  their causes are the same object, or same class + message. Or simply make `Failure` a record and
  accept reference equality on the `Throwable`. Recommendation: record default (reference equality on the
  cause) plus a javadoc note. It is honest: two exceptions are not "the same" because they print alike.
- **`Serializable` (decided): dropped everywhere.** Interfaces, records, tuples, functions, collections.
  Every `SerializationProxy`, `readResolve`, `serialVersionUID` and `@SuppressWarnings("serial")` goes
  with it (61 files). If a concrete need appears later (Kafka Streams state stores, Spark, HTTP sessions),
  it comes back per type, on request, with a test.
- `Value.eq()` (structural deep equality) is deleted; records give structural `equals`.
- Keep JSpecify (`@Nullable`/`@NonNull`) and the NullAway CI job; they are the fork's best asset for a
  "null cannot happen here" story.

### 3.10 `Future`, `Promise`, `Task`: deleted (decided)

 Vavr's `Future` is a small effect system (executor pairs on every static, `await`, `cancel`,
`onComplete`, `Promise`, `FutureImpl` 470 lines). ZIO would say: either it is a full runtime or it should
not exist. Java 21 has virtual threads, `CompletableFuture`, and structured concurrency in preview.

Decision: **`io.vavr.concurrent` is deleted** (`Future`, `FutureImpl`, `Promise`, `PromiseImpl`, `Task`) and
only `Try.fromCompletableFuture` / `Try.toCompletableFuture` bridge to the JDK. If an async type comes
back later it will be a new design on virtual threads and structured concurrency, not a slimmed `Future`.
If kept, it must lose `Value`, lose the executor-overload doubling (one `Executor` parameter on
`Future.of` only), lose `executorService() throws UnsupportedOperationException`, and adopt the naming
table (`tap`, `catchAll`, `zip`, `collectAll`, `forEach`).

### 3.11 `Lazy`

Keep. `Lazy.of(Supplier)`, `get()`, `isEvaluated()`, `map`, `flatMap`, `zip`/`zipWith`, `tap`,
`collectAll`. Remove `Value`, `Iterable`, `Supplier` supertype (keep a `toSupplier()`), `val()`,
`filter`, `transform`, and the custom `writeObject`. The `ReentrantLock` double-checked implementation is
fine.

### 3.12 Primitive (unboxed) `Option`: not in v1

A generic `Option<A>` cannot hold an unboxed `int` on JDK 25: `A` erases to `Object`, so `some(42)`
allocates an `Integer` (outside the -128..127 cache) plus a `Some`. The options and the decision:

- **Specialised `OptionInt`/`OptionLong`/`OptionDouble`** (sealed, `record SomeInt(int value)`), the
  JDK's `OptionalInt` design. Pattern-matchable (`case SomeInt(int i)`), but a parallel API with no
  boxing-free `flatMap` across the primitive/reference boundary, and nothing in Zazr produces them: the
  collections store primitives unboxed (`BitMappedTrie` leaves via `ArrayType`) but box on `get(i)`, and
  there is no `IntVector`-style primitive collection API. **Deferred** until a JMH benchmark on a real
  hot path shows the boxing, and then added together with the producing methods (`indexOfOption`,
  numeric `max`/`sum` folds).
- **JIT escape analysis** already removes both allocations for the common inline pattern
  (`find(...).map(...).getOrElse(...)` in one method) once C2 inlines; records make that more likely.
  Measure before adding types.
- **Sentinel encoding** (`None` as a private sentinel inside one final `Option` class) saves the `Some`
  but not the `Integer`, and gives up sealed/records/`switch`. Rejected.
- **Valhalla**: JEP 401 (value classes) is not final as of JDK 25 and the "no preview in main code" rule
  applies. When it lands, `value record Some<A>(A value)` removes the wrapper's identity, but unboxed
  `Option<int>` needs the later parametric-JVM phase, which has no date. What Zazr does now is keep every
  record `value`-ready: no `==` on instances, no `synchronized`, no `IdentityHashMap`, so adding the
  modifier later is a one-word, source-compatible change.

### 3.13 What is deliberately **not** ported from zio-prelude

- `Associative`/`Identity`/`Commutative` etc. as interfaces: without higher-kinded types they reduce to
  `BinaryOperator<A>` plus a constant. The naming lesson is applied to method names instead
  (`reduce(BinaryOperator)`, `fold(A identity, BinaryOperator)`), and `combine`-style helpers take a
  `BinaryOperator` parameter. Adding a `Semigroup`-by-another-name interface is the abstraction nobody asked for.
- `ForEach`/`NonEmptyForEach`/`AssociativeBoth` as generic interfaces, i.e. higher-kinded types. Java *can*
  encode them (the witness-type encoding: `interface Kind<F, A> {}`, `Option<A> extends Kind<Option.W, A>`
  with an empty `enum W` as the witness, one unsafe-but-sound `narrow(Kind<W, A>) : Option<A>` per type,
  typeclass instances passed explicitly; prior art highj, derive4j/hkt, Cyclops, Arrow-kt before 1.0).
  Rejected (decided): every abstract signature returns `Kind<F, B>` and every call site ends in
  `narrow(...)`, instances are explicit parameters everywhere, error messages name `Kind<W, ...>`, and
  Arrow-kt dropped the same encoding in 1.0 for exactly those reasons on a language with better
  inference than Java. Inside Zazr the type set is small and fixed, so per-type generated
  `forEach`/`collectAll`/`zip` cover the need. If wanted later, it is a `zazr-hkt` module experiment,
  never a dependency of `zazr-core`. Their *operator inventory*
  (`partitionMap`, `reduceMap`, `mapAccum`, `groupByNonEmpty`, `intersperse`, `maxByOption`...) is the
  checklist for `Vector`/`NonEmptyVector` methods.
- `Newtype`/`Subtype` (re-examined and rejected with the maintainer, 2026-09-25). zio-prelude's newtypes are free
  because Scala erases them to the underlying type, and `Subtype` makes a refined value usable wherever the
  underlying type is expected. Java has neither: a class cannot extend `String`, `Long` or a record, so a newtype
  is always a wrapper, and without `Subtype` every call into code expecting the underlying type means unwrapping,
  which is what makes newtypes tiresome to use. A public record cannot even enforce its refinement without
  throwing, since its canonical constructor is public; a final class with a private constructor and a
  `Validation`-returning `make` can, at the cost of record patterns. The one true `Subtype` in Java is a type
  qualifier checked at compile time (`@Email String`, the Checker Framework's subtyping checker), which exists
  without Zazr and only protects users who run the checker. So no newtype module; revisit only if Valhalla's value
  classes and a subtyping story change the trade-off.
- `Derive`, `ZPure`/`State`/`Reader`/`Writer`, `ZSet`/`MultiSet`, the `experimental`
  algebra module, `Debug`/`Repr`, `Equal`/`Hash`/`Ord` as typeclasses (Java has `Comparator`; structural
  equality is `equals`). A three-valued `Ordering` enum is cute but `Comparator` returns `int`; skip.
- `Assertion<A>` (the refinement DSL: `greaterThan`, `matches`, `hasLength`, `&&`/`||`, returning
  `Validation<String,A>`) is the one candidate worth revisiting in v2: it composes naturally with
  `Validation` and has no Java equivalent. Not in v1.

---

## 4. Build, tooling, packaging

- **Package rename (decided)**: `io.vavr` → `com.guizmaii.zazr` (`com.guizmaii.zazr.collection`, `.control`), module `com.guizmaii.zazr`, Maven groupId `com.guizmaii`,
  Maven coordinates changed so the fork can never be confused with Vavr on a classpath. Do this in the
  first commit; every later diff is then unambiguous.
- **Generator**: keep `Generator.scala` but shrink it to `Tuple0..8` (records), `Function3..8`,
  `CheckedFunction1..8`, and the `zip`/`zipWith` arity-N statics for each control type. `API.java`,
  `ArrayType`'s eight specialisations (keep, they are real), `CaseN`, `ForLazyN` go. Consider replacing the
  Scala script with a plain Java `main` (`java Generator.java`, single-file source launch) so the build
  needs no Scala toolchain; **Open**, cosmetic.
- **Jargon guard**: a CI step that fails on `monad|functor|applicative|semigroup|monoid` anywhere under `src/`, `src-gen/`, `generator/`.
- **Mono-repo (decided)**, Kyo-style: one Maven reactor, one version, one release, several artifacts.
  Vavr had this layout (`vavr`, `vavr-test`, `vavr-benchmark`, `vavr-match`, `vavr-match-processor` as
  modules of the parent pom) until commit `da4baffb8` split them into separate repositories; Zazr
  reverses that split. Modules:

  | module | artifact | content |
  |---|---|---|
  | `zazr-core` | `com.guizmaii:zazr-core` | everything in this document |
  | `zazr-test` | `com.guizmaii:zazr-test` | property-based testing + law suites (below); depends on `zazr-core`; used by `zazr-core`'s own tests (test scope, no cycle: Maven allows a module's tests to depend on a sibling as long as the sibling's *main* code does not depend back) |
  | `zazr-benchmark` | not published | JMH, currently `vavr/src/test/java/io/vavr/JmhRunner.java` behind the `benchmark` profile; moves back to its own module as in the old `vavr-benchmark` |

  Later candidates that a mono-repo makes cheap: `zazr-jackson`, `zazr-gson`, `zazr-jmh-annotations`.
  The `match` modules are **not** restored (3.1). Each module keeps its own `generator/Generator.scala`
  as before.
- **Property-based testing: re-integrate `vavr-test` as `zazr-test` (decided; no jqwik).** Restore it
  from history (`git checkout da4baffb8^ -- vavr-test`: `Gen`, `Arbitrary`, `Checkable`, `CheckResult`,
  a generated `Property` with `forAll(a1..a8).suchThat(...).implies(...).check(random, size, tries)`,
  6,218 lines incl. tests) and adapt it:
  - rename to `com.guizmaii.zazr.test`; `CheckResult` becomes a sealed interface with records
    `Satisfied`, `Falsified`, `Erroneous`; `Property.def(name)` → `Property.named(name)`;
    `suchThat` keeps its name (it reads well); `Gen.peek` → `tap`, `Gen.transform` deleted, per 3.3.
  - `Arbitrary`/`Gen` instances for every Zazr type: `option`, `either`, `try`, `validation`, `lazy`,
    `tuple2..8`, `vector`, `nonEmptyVector`, `list`, `lazyList`, `hashMap`, `hashSet`, `treeMap`,
    `treeSet`, `linkedHashMap`, `linkedHashSet`, each parameterised by element arbitraries.
  - a `laws` package, modelled on zio-prelude's `laws` module: each law is a named value
    (`Laws.zipAssociativity`, `mapIdentity`, `mapComposition`, `flatMapAssociativity`,
    `zipLeftIdentity`, `validationZipAccumulatesBothSides`, `nonEmptyVectorHeadIsTotal`,
    `builderResultEqualsOfAll`), law sets compose (`Laws.zip = zipAssociativity + zipLeftIdentity + ...`),
    and a failing law reports its name. One `*LawsTest` per type in `zazr-core` runs the relevant sets.
    This is the "runnable check" for the whole refactor.
  - shrinking is absent from `vavr-test`; add it only if a falsified case is ever unreadable.
- **JMH**: the `benchmark` profile exists; add `VectorBuilderBenchmark` (append ×N, `map`, `filter`,
  `flatMap`, `collector`) before and after 3.8 so the builder claim is measured, not asserted.
- **JaCoCo**: either wire the plugin or delete the README line.
- **Publishing (decided)**, same recipe as `guizmaii-opensource/vavr-test`: coordinates `com.guizmaii:zazr-core`
  (parent `com.guizmaii:zazr-parent`), version `0.1.0-SNAPSHOT` on `main`; snapshots deployed to the Central
  Portal on every push to `main`; a release is made by publishing a GitHub release whose tag is `vX.Y.Z`
  (or by dispatching the `release` workflow with that tag): the workflow sets the Maven version from the
  tag with `versions:set`, signs with the imported PGP key and deploys with `-Pmaven-central-release`.
  Secrets, at the `guizmaii-opensource` organisation level: `SONATYPE_USERNAME`, `SONATYPE_PASSWORD`,
  `PGP_SECRET` (armored private key, base64-encoded or plain), `PGP_PASSPHRASE`. The `vavr-match`
  artifacts stay `io.vavr:*:1.0.0` until PR 3 deletes them.
- **Changelog**: start `CHANGELOG.md` with this document's section numbers as the first entry.
- **Documentation lives in the repo (decided).** Vavr's user guide is a separate repository
  (`vavr-io/vavr-docs`, AsciiDoc, published at docs.vavr.io) and is not forked: it teaches `Match`,
  `For`, `ap`, `Seq`, `Value` and `Future`, all of which Zazr removes. Zazr keeps a `docs/` folder in the
  mono-repo: the rewritten user guide in Markdown (one page per area: control types, `Validation`,
  collections and builders, `NonEmptyVector`, JDK interop via `asJava`, `zazr-test` and laws), this
  document moved to `docs/design.md`, and `CHANGELOG.md` at the root. The API reference is the Markdown
  javadoc in the sources (JEP 467), published as the `-javadoc.jar`; no second copy of it in `docs/`.
  Site generation (if any) comes later; plain Markdown rendered by GitHub is enough for v1.
  **Not a rewrite of Vavr's guide** (decided): that guide is long and says little about the data
  structures themselves. The Zazr guide is short and factual; each collection page answers, in this
  order: what it is and how it is represented (trie of 32-wide leaves, cons cells, HAMT, red-black tree,
  two lists); a complexity table covering every operation on the type (`get`, `update`, `append`,
  `prepend`, `head`/`tail`, `take`/`drop`, `contains`, `size`, iteration), with amortised vs worst case
  stated; when to choose it over its siblings (`Vector` vs `List` vs `Queue` vs `LazyList`; `HashMap` vs
  `TreeMap` vs `LinkedHashMap`); its invariants (order, null policy, non-emptiness, comparator
  consistency); memory footprint per element; interop (`asJava`, `ofAll` fast paths); and known sharp
  edges. Control-type pages do the same for semantics: fail-fast vs accumulating, what `flatMap` does
  on `Validation`, which errors `Try` does not catch. Examples are five lines or fewer and show a real
  use, not a tour of the method list; the method list is the javadoc. Per-method complexity lives in
  the javadoc too, so the guide tables and the javadoc are written from the same source of truth.
- **README rewrite** (decided): the current one is Vavr's (1.0.1 coordinates, Vavr badges, stargazer chart,
  "led and maintained by" line). The Zazr README states the fork's purpose in the terms of section 2,
  the JDK 25+ requirement, the `com.guizmaii:zazr` coordinates, a ten-line tour (`switch` over
  `Validation`, `zip` at arity N, `NonEmptyVector`, `Vector.newBuilder()`), the list of things it does
  *not* have compared to Vavr with a pointer to this document, and the Apache-2.0 attribution to Vavr.

---

## 5. Order of work and tickets

The plan is tracked on GitHub: tracking issue **#32** (milestone `0.1.0`, label `design-plan`), one issue per
PR-sized item with its design section, scope, dependencies and done-when. Each item is one PR, stacked on
the previous item's branch where it depends on it, rebased on `main` before review; the rules are in
`CLAUDE.md`. Done so far: #1 (this document, JDK 25), #4 (`Vector.Builder`, 3.8), #5 (headers), #7
(NOTICE), #3/#8 (design additions), #6/#9/#10 (`CLAUDE.md`).

| # | item | design | after |
|---|---|---|---|
| #11 | Mono-repo layout: `zazr-core`, `zazr-test`, `zazr-benchmark` | 4 | |
| #12 | Rename to `com.guizmaii.zazr` | 4 | #11 |
| #13 | Delete the Match API and its helpers | 3.1 | #12 |
| #14 | Delete `io.vavr.concurrent` | 3.10 | |
| #15 | Delete `Array`, `CharSeq`, `Tree`, `BitSet`, `PriorityQueue`, `Multimap*` | 3.7 | |
| #16 | Sealed interfaces and records; null policy; `Try.Failure` equality | 3.1, 3.9 | #13, #14 |
| #17 | Drop `Serializable` | 3.9 | #16 |
| #18 | JDK functional interfaces first | 3.1 | #16 |
| #19 | Remove `Value`; conversion sets | 3.2 | #16 |
| #20 | Naming table; vocabulary scrub; CI jargon guard | 3.3, 4 | #19 |
| #21 | `NonEmptyVector` | 3.6 | #20 |
| #22 | `Validation` with a `NonEmptyVector` error side | 3.5 | #21 |
| #23 | Generated `zip`/`zipWith` at arities 2..8 | 3.4 | #22 |
| #24 | Remove `Seq`; concrete collection APIs; complexity notes. Done in three stacked steps: #66 (`Vector` declares its own API, `IndexedSeq` deleted; PR #69), #67 (`List`, `Queue`, `Stream`; `Seq`, `LinearSeq` deleted; PR #70), #68 (`Traversable` slimmed to the 3.7 list, `Foldable`/`Ordered` deleted, `Map`/`Set` lose the sequence methods, `Iterator` leaves the public API, own-type `grouped`/`sliding`/`crossProduct`) | 3.7 | #20 |
| #72 | Positional subset on the ordered sets and maps: `SortedSet`/`SortedMap` (rank split of the red-black tree), `LinkedHashSet`/`LinkedHashMap` (slice of the insertion order) | 3.7 | #24 |
| #73 | Internal types move to `.internal` packages: `collection.internal` for the collection internals, `com.guizmaii.zazr.internal` for the rest | 3.1 | #72 |
| #25 | `partitionMap`, `duplicates`, static `flatten` | 3.7 | #24, #21 |
| #26 | `asJava` views for every collection (`asJavaMap` for the maps); the `toJava*` copies deleted | 3.1 | #24 |
| #27 | Builders for the other collections | 3.8.1 | #24 |
| #28 | Rename `Stream` to `LazyList` | 3.7 | #24 |
| #29 | Primitive specialisation without `ClassCastException` fallbacks; `collector()` decision | 3.8 | #12 |
| #30 | `zazr-test` adapted; law suites | 4 | #11 |
| #31 | Documentation: `docs/`, README, CHANGELOG, JaCoCo | 4 | the API items |

---

## 6. Open questions (summary)

None. Every question raised in this document has been decided; the decisions are marked **(decided)**
in their sections. New questions go here.
