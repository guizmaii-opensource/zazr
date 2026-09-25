# `zip` and `zipWith`

Combine independent values with `zip` (into a tuple) or `zipWith` (into a function). `Option`, `Either`, `Try`,
`Validation` and `Lazy` have the same methods, names and argument order. Full page: https://zazr.dev/zip/.

| Form | Arity | Returns |
|---|---|---|
| `a.zip(b)` | 2 | `F<Tuple2<A, B>>` |
| `a.zipWith(b, f)` | 2 | `F<C>`, `f` a `BiFunction` |
| `a.zipLeft(b)`, `a.zipRight(b)` | 2 | `F<A>`, `F<B>`: both sides must succeed, one value is kept |
| `F.zip(a1, ..., aN)` | 2 to 8 | `F<TupleN<...>>` |
| `F.zipWith(a1, ..., aN, f)` | 2 to 8 | `F<R>`, `f` a `BiFunction` or `Function3` to `Function8` |

Prefer the static `zipWith` with every value at once: it passes the values straight to `f`, with no tuple and no
`Tuple2<Tuple2<A, B>, C>` to unpack. Do not chain `flatMap`s to combine values that do not depend on each other.

```java
var sum = Option.zipWith(Option.some(1), Option.some(2), Option.some(3), (a, b, c) -> a + b + c); // Option<Integer>
var triple = Option.zip(Option.some(1), Option.some("a"), Option.some(true)); // Option<Tuple3<Integer, String, Boolean>>
// Some(6), Some((1, a, true))
```

## On failure

| Type | What happens |
|---|---|
| `Option`, `Either`, `Try` | fail fast, in argument order: the first `None`, `Left` or `Failure` is returned, `f` is not called |
| `Validation` | accumulates: `Invalid` with the errors of every invalid argument, in argument order |
| `Lazy` | nothing fails: an unevaluated `Lazy` that computes the arguments in order on first `get()` |

```java
var first = Either.zipWith(Either.left("no a"), Either.<String, Integer>right(2), // Either<String, Integer>
    Either.<String, Integer>left("no c"), (a, b, c) -> 0);
var all = Validation.zipWith(Validation.<String, Integer>invalid("no a"), // Validation<String, Integer>
    Validation.<String, Integer>valid(2), Validation.<String, Integer>invalid("no c"), (a, b, c) -> 0);
// Left(no a), Invalid(no a, no c)
```

`zipLeft` and `zipRight` are not `orElse`: `Option.some(1).zipLeft(Option.none())` is `None`.

## Null

A `null` argument throws a `NullPointerException`. When `f` returns `null`: `Option`, `Either` and `Validation`
throw, `Try` gives a `Failure`, `Lazy` holds `null`.

## Tuples

`Tuple2` to `Tuple8` (in `com.guizmaii.zazr`) are records: read them with `_1()`, `_2()`, or take them apart with a
record pattern.

```java
var pair = Option.some(1).zip(Option.some("one")); // Option<Tuple2<Integer, String>>
var text = switch (pair) {
    case Some(Tuple2(var n, var name)) -> n + " is " + name;
    case None() -> "nothing";
};
// "1 is one"
```
