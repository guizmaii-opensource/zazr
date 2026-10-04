---
description: What Zazr changes from Vavr - removed types, renamed operations, and the behaviour that differs.
---

# Compared to Vavr

Zazr is a fork of the latest [Vavr](https://github.com/vavr-io/vavr), reshaped for Java 25. Its collections follow the
Scala 2.13 collections library, which Scala 3 uses unchanged; its names and most of its control types follow ZIO
and zio-prelude, and `Using` follows Scala's `scala.util.Using`. It is not a drop-in replacement: the package is
`dev.zazr`, and the changes below are deliberate. [Design](principles.md) explains the ideas behind them, and the
[release notes](https://github.com/guizmaii-opensource/zazr/releases) list what each release changes.

## Removed

| Removed | Use instead |
|---|---|
| `Match`, `Case`, `$`, `API.For` | `switch` with record patterns and `when` guards; `zip`/`zipWith` for comprehensions |
| `Future`, `Promise`, `Task` | `CompletableFuture`, virtual threads; `Try.fromCompletableFuture` and `toCompletableFuture` bridge |
| `Array`, `CharSeq` | `Vector`; `String` and `Vector<Character>` |
| `Tree`, `BitSet`, `PriorityQueue`, the `Multimap` family | `Map<K, Vector<V>>` with `groupBy` |
| `Seq`, `IndexedSeq`, `LinearSeq`, `Foldable`, `Value` | the concrete type, or `Traversable` for what every collection does at the same cost |
| `Function0`, `Function1`, `Function2`, `CheckedFunction0` | `Supplier`, `Function`, `BiFunction`, `Callable` |
| `PartialFunction`, `collect(PartialFunction)` | `collect(Function<A, Option<B>>)` with pattern matching inside the lambda |
| `Serializable` on every type | none; no Zazr type is `Serializable` |
| `Either` projections, `Validation.Builder`, `combine`, `ap` | `mapLeft`, `flip`, `fold`; `zip` and `zipWith` at arity N |
| `Try.withResources` | [`Using`](control/using.md) |
| the public `Iterator` type, `reverseIterator()`, `iterator(int)` | `iterator()`, a `java.util.Iterator`; `reverse().iterator()`, `drop(n).iterator()` |
| `toJavaList`, `toJavaSet`, `toJavaMap`..., `asJavaMutable` | the view `asJava()` or `asJavaMap()`; copy with `new ArrayList<>(vector.asJava())` |
| `unfoldLeft`, `unfoldRight` | `unfold` (see below) |

## Renamed

| Vavr | Zazr |
|---|---|
| `ap`, `combine(...).ap(f)` | `zip`, `zipWith`, static `zip(a, b, c)`, `zipWith(a, b, c, f)` |
| `sequence` | `collectAll` |
| `traverse` | static `forEach` |
| `bimap` | `mapBoth` |
| `peek`, `peekLeft`, `onFailure`, `onSuccess`, `onEmpty` | `tap`, `tapLeft`, `tapError`, `tap`, `tapNone` |
| `mapTo(value)` | `as(value)` on a sequence or a set; `map(x -> value)` on a control type or a map |
| `swap` on `Either` | `flip` |
| `Stream`, `toStream()` | `LazyList`, `toLazyList()` |
| `length()` | `size()` |
| `Tuple.sequence1` to `sequence8`, `toSeq()` | `Tuple.unzip1` to `unzip8`, `toVector()` |
| `recover`, `recoverWith` | `catchAll`, `catchSome`, `catchAllWith`, `catchSomeWith` |
| `mapFailure(Case...)` | `mapError(Function)` |
| `andFinally` | `ensuring` |
| `Option.of(nullable)` | `Option.ofNullable` |
| `Validation.cond`, `Either.cond` | `fromPredicate` |
| `getOrElseGet(Function)` | a `getOrElse(Function)` overload |
| `toJavaArray` | `toArray` |
| `sum()`, `product()`, `average()` | `sumInt`, `sumLong`, `sumDouble`, `productInt`, `productLong`, `productDouble`, `average`, each taking the function that reads the number from an element |

## Behaviour that differs

- **No `null` inside.** `Some`, `Right`, `Success`, `Valid` and every collection reject it; `Option.some(null)`
  throws instead of building `Some(null)`.
- **Control types are not `Iterable`.** `for (x : option)` no longer compiles; each type has explicit conversions
  (`toVector()`, `toOption()`...).
- **`Validation` keeps every error.** Its error side is a `NonEmptyVector<E>`, and `zip` accumulates without changing
  the type.
- **Sets and maps have no positional methods** (`head`, `take`, `sliding`...), except the ordered ones:
  `LinkedHashSet`, `LinkedHashMap`, `TreeSet`, `TreeMap`.
- **`grouped`, `sliding` and `crossProduct` return a collection**, not an `Iterator`: of the receiver's type on
  the sequences (`Vector<Vector<T>>`...), a `Vector` of the receiver's type for `grouped` and `sliding` on the
  ordered sets and maps and on `NonEmptyVector` (`Vector<TreeSet<T>>`), and a `Vector` for `crossProduct(power)` on
  `NonEmptyVector`.
- **`tap` on a collection runs on every element.** Vavr's `peek` ran on the first one.
- **Two `Try.Failure`s are equal when they hold the same `Throwable` instance**, not when their stack traces match.
- **`unfold` returns the element first, then the next state**, as in Scala and ZIO, and gives the elements in the
  order it produces them: it is Vavr's `unfoldRight`. Vavr's `unfold` and `unfoldLeft` returned
  `(nextState, element)` and gave the elements in reverse order. When the element and the state have the same
  type, Vavr code still compiles with the roles swapped:
  `List.unfold(10, x -> x == 0 ? Option.none() : Option.some(Tuple.of(x - 1, x)))` keeps the state at 10 and never
  returns. Swap the tuple (`Tuple.of(x, x - 1)`) when moving it over, and
  `reverse()` the result where the order matters.
- **`LazyList`, Vavr's `Stream`, is fully lazy.** Nothing is computed before it is read, not even its first element
  or whether it is empty ([LazyList](collections/lazy-list.md)).
- **Every positional method documents its cost** ([complexity](collections/complexity.md)).

## Moving code over

```java
// Vavr: Match(option).of(Case($Some($()), v -> ...), Case($None(), () -> ...))
var maybe = Option.some("Zazr");
var length = switch (maybe) {
    case Some(var value) -> value.length();
    case None() -> 0;
};
// 4
```
