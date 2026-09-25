---
description: What Zazr changes from Vavr - removed types, renamed operations, and the behaviour that differs.
---

# Compared to Vavr

Zazr is a fork of the latest [Vavr](https://github.com/vavr-io/vavr), reshaped for Java 25 and for the API design of ZIO,
zio-prelude and modern Scala. It is not a drop-in replacement: the package is `com.guizmaii.zazr`, and the changes
below are deliberate. [Design](principles.md) explains the ideas behind them.

## Removed

| Removed | Use instead |
|---|---|
| `Match`, `Case`, `$`, `API.For` | `switch` with record patterns and `when` guards; `zip`/`zipWith` for comprehensions |
| `Future`, `Promise`, `Task` | `CompletableFuture`, virtual threads; `Try.fromCompletableFuture` and `toCompletableFuture` bridge |
| `Array`, `CharSeq` | `Vector`; `String` and `Vector<Character>` |
| `Tree`, `BitSet`, `PriorityQueue`, the `Multimap` family | `Map<K, Vector<V>>` with `groupBy` |
| `Seq`, `IndexedSeq`, `LinearSeq`, `Foldable`, `Value` | the concrete type, or `Traversable` for what every collection does at the same cost |
| `Function0`, `Function1`, `Function2`, `CheckedFunction0` | `Supplier`, `Function`, `BiFunction`, `Callable` |
| `PartialFunction`, `collect(PartialFunction)` | `collect(Function<A, Option<B>>)` with a `switch` inside the lambda |
| `Serializable` on every type | none; no Zazr type is `Serializable` |
| `Either` projections, `Validation.Builder`, `combine`, `ap` | `mapLeft`, `flip`, `fold`; `zip` and `zipWith` at arity N |

## Renamed

| Vavr | Zazr |
|---|---|
| `ap`, `combine(...).ap(f)` | `zip`, `zipWith`, static `zip(a, b, c)`, `zipWith(a, b, c, f)` |
| `sequence` | `collectAll` |
| `traverse` | static `forEach` |
| `bimap` | `mapBoth` |
| `peek`, `peekLeft`, `onFailure`, `onSuccess`, `onEmpty` | `tap`, `tapLeft`, `tapError`, `tap`, `tapNone` |
| `mapTo(value)` | `as(value)` |
| `swap` on `Either` | `flip` |
| `recover`, `recoverWith` | `catchAll`, `catchSome`, `catchAllWith`, `catchSomeWith` |
| `mapFailure(Case...)` | `mapError(Function)` |
| `andFinally` | `ensuring` |
| `Option.of(nullable)` | `Option.ofNullable` |
| `Validation.cond`, `Either.cond` | `fromPredicate` |
| `getOrElseGet(Function)` | a `getOrElse(Function)` overload |
| `toJavaArray` | `toArray` |

## Behaviour that differs

- **No `null` inside.** `Some`, `Right`, `Success`, `Valid` and every collection reject it; `Option.some(null)`
  throws instead of building `Some(null)`.
- **Control types are not `Iterable`.** `for (x : option)` no longer compiles; each type has explicit conversions
  (`toVector()`, `toOption()`...).
- **`Validation` keeps every error.** Its error side is a `NonEmptyVector<E>`, and `zip` accumulates without changing
  the type.
- **Sets and maps have no positional methods** (`head`, `take`, `sliding`...), except the ordered ones:
  `LinkedHashSet`, `LinkedHashMap`, `TreeSet`, `TreeMap`.
- **`grouped`, `sliding` and `crossProduct` return the receiver's type** (`Vector<Vector<T>>`...), not an
  `Iterator`.
- **`tap` on a collection runs on every element.** Vavr's `peek` ran on the first one.
- **Two `Try.Failure`s are equal when they hold the same `Throwable` instance**, not when their stack traces match.
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
