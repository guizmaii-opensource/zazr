---
description: zip and zipWith from 2 to 8 arguments on Option, Either, Try, Validation and Lazy, without nested tuples.
---

# `zip` at arity N

Combining independent values is `zip`: it returns a tuple of the values, and `zipWith` applies a function to them.
Every control type has the same family, with the same names and the same argument order.

| Form | Arity | Returns |
|---|---|---|
| `a.zip(b)` | 2 | `F<Tuple2<A, B>>` |
| `a.zipWith(b, f)` | 2 | `F<C>`, `f` a `BiFunction` |
| `a.zipLeft(b)`, `a.zipRight(b)` | 2 | `F<A>`, `F<B>`: both sides are inspected, one value is kept |
| `F.zip(a1, ..., aN)` | 2 to 8 | `F<TupleN<...>>` |
| `F.zipWith(a1, ..., aN, f)` | 2 to 8 | `F<R>`, `f` a `BiFunction` or `Function3` to `Function8` |

The arity is fixed at the call site by the static overload, so a result is never a `Tuple2<Tuple2<A, B>, C>`, and
`zipWith` passes the values straight to `f` without building a tuple.

```java
Option<Integer> sum = Option.zipWith(Option.some(1), Option.some(2), Option.some(3), (a, b, c) -> a + b + c);
Option<Tuple3<Integer, String, Boolean>> triple = Option.zip(Option.some(1), Option.some("a"), Option.some(true));
// Some(6), Some((1, a, true))
```

## What happens on failure

| Type | Semantics |
|---|---|
| `Option`, `Either`, `Try` | fail fast, in argument order: the first `None`, `Left` or `Failure` is returned as is, `f` is not called and the later arguments are not inspected |
| `Validation` | accumulates: `Invalid` with the errors of every invalid argument, in argument order; `Valid` only when all are |
| `Lazy` | nothing fails: the result is an unevaluated `Lazy` that forces the arguments in order on first `get()` |

```java
Either<String, Integer> first = Either.zipWith(Either.left("no a"), Either.<String, Integer>right(2),
    Either.<String, Integer>left("no c"), (a, b, c) -> 0);
Validation<String, Integer> all = Validation.zipWith(Validation.<String, Integer>invalid("no a"),
    Validation.<String, Integer>valid(2), Validation.<String, Integer>invalid("no c"), (a, b, c) -> 0);
// Left(no a), Invalid(no a, no c)
```

`zipLeft` and `zipRight` are not `orElse`: they fail when either side fails.

```java
Option<Integer> left = Option.some(1).zipLeft(Option.none());
Option<String> right = Option.some(1).zipRight(Option.some("kept"));
// None, Some(kept)
```

## Null

A `null` argument is a `NullPointerException`. On `Option`, `Either` and `Validation`, `f` returning `null` is
rejected at the call site; on `Try`, `f` runs like a `map` mapper, so what it throws, and a `null` result, end up in a
`Failure`. On `Lazy`, `f` may return `null`.
