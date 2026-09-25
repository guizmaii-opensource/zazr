# From Vavr

Zazr is a fork of Vavr, and code written from memory of Vavr often does not compile against it. Write the right
column, never the left one. Full page: https://zazr.dev/vavr/.

## Names and types

| Not this (Vavr) | This (Zazr) |
|---|---|
| `import io.vavr.control.Option` | `import com.guizmaii.zazr.control.Option` (collections in `com.guizmaii.zazr.collection`, `Lazy` and tuples in `com.guizmaii.zazr`) |
| `Option.of(nullable)` | `Option.ofNullable(nullable)`, or `Option.some(value)` for a value that is never `null` |
| `Match(x).of(Case($Some($()), ...), Case($None(), ...))` | a `switch` expression with record patterns: `case Some(var v) ->`, `case None() ->` |
| `API.For(a, b).yield(f)` | `Option.zipWith(a, b, f)` (static, 2 to 8 values), or `flatMap` when a step needs the previous one |
| `Case`, `$`, `Patterns` | record patterns and `when` guards |
| `Seq<T>`, `IndexedSeq<T>`, `LinearSeq<T>` | the concrete type (`Vector<T>` by default), or `Traversable<T>` for what every collection does at the same cost |
| `Array<T>` | `Vector<T>` |
| `CharSeq` | `String`, or `Vector<Character>` |
| `Tree`, `BitSet`, `PriorityQueue`, `Multimap` | `Map<K, Vector<V>>` with `groupBy`, or a JDK type |
| `Future`, `Promise`, `Task` | `CompletableFuture` or virtual threads; `Try.fromCompletableFuture` and `toCompletableFuture()` |
| `Function0`, `Function1`, `Function2` | `Supplier`, `Function`, `BiFunction` (`Function3` to `Function8` exist) |
| `CheckedFunction0` | `Callable` (`Try.of` takes one) |
| `PartialFunction`, `collect(PartialFunction)` | `collect(x -> Option...)`, a function returning an `Option`, with pattern matching inside |
| `Validation<Seq<E>, A>` | `Validation<E, A>`, whose `Invalid` holds a `NonEmptyVector<E>` |
| `Validation.combine(a, b).ap(f)`, `Validation.Builder` | `Validation.zipWith(a, b, f)` |
| `ap` | `zip`, `zipWith`, static `zip(a, b, c)`, `zipWith(a, b, c, f)` |
| `sequence` | static `collectAll` |
| `traverse` | static `forEach(values, f)` |
| `bimap` | `mapBoth` |
| `either.swap()` | `either.flip()` |
| `either.right().map(f)`, `either.left().map(f)` | `either.map(f)`, `either.mapLeft(f)` |
| `peek`, `peekLeft` | `tap`, `tapLeft` |
| `onSuccess`, `onFailure`, `onEmpty` | `tap`, `tapError`, `tapNone` |
| `recover(f)`, `recover(X.class, f)` | `catchAll(f)`, `catchSome(X.class, f)` |
| `recoverWith` | `catchAllWith`, `catchSomeWith` |
| `mapFailure(Case...)` | `mapError(Function)` |
| `andFinally` | `ensuring` |
| `Either.cond`, `Validation.cond` | `Either.fromPredicate`, `Validation.fromPredicate` |
| `getOrElseGet(f)` | `getOrElse(f)`, an overload taking the failure |
| `mapTo(value)` | `as(value)` on a collection; `map(x -> value)` on a control type |
| `toJavaList()`, `toJavaSet()`, `toJavaMap()` | the view `asJava()` or `asJavaMap()`; copy with `new java.util.ArrayList<>(x.asJava())` |
| `toJavaOptional()` | `toOptional()` |
| `toJavaStream()` | `stream()` |
| `toJavaArray` | `toArray` |
| `tuple._1` (field) | `tuple._1()`, or a record pattern `Tuple2(var a, var b)` |
| `for (var x : option)` | pattern matching, or `option.toVector()`: control types are not `Iterable` |

## Behaviour that differs

- **No `null` inside.** `Option.some(null)`, `Either.right(null)`, `Vector.of(1, null)` throw. Vavr built
  `Some(null)`.
- **Control types are not `Iterable`.** Convert with `toVector()`, `toOption()` and the like.
- **`Validation` keeps every error** in a `NonEmptyVector`, and `zip`/`zipWith` accumulate.
- **Sets and maps have no positional methods** (`head`, `take`, `sliding`...), except `LinkedHashSet`,
  `LinkedHashMap`, `TreeSet` and `TreeMap`.
- **`grouped`, `sliding` and `crossProduct` return the receiver's type**, not an `Iterator`.
- **`tap` on a collection runs on every element**; Vavr's `peek` ran on the first one. `List.peek()` is the stack
  top.
- **Two `Try.Failure`s are equal only when they hold the same `Throwable` instance.**
- **No type is `Serializable`.**
- **`NonEmptyVector` has no `length()`**: use `size()`.
