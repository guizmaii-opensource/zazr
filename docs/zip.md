---
description: zip and zipWith from 2 to 8 arguments on Option, Either, Try, Validation and Lazy, without nested tuples.
---

# `zip` at arity N

`zip` combines independent values into a tuple; `zipWith` passes them to a function instead. `Option`, `Either`,
`Try`, `Validation` and `Lazy` all have the same methods, with the same names and argument order.

| Form | Arity | Returns |
|---|---|---|
| `a.zip(b)` | 2 | `F<Tuple2<A, B>>` |
| `a.zipWith(b, f)` | 2 | `F<C>`, `f` a `BiFunction` |
| `a.zipLeft(b)`, `a.zipRight(b)` | 2 | `F<A>`, `F<B>`: both sides are inspected, one value is kept |
| `F.zip(a1, ..., aN)` | 2 to 8 | `F<TupleN<...>>` |
| `F.zipWith(a1, ..., aN, f)` | 2 to 8 | `F<R>`, `f` a `BiFunction` or `Function3` to `Function8` |

There is one static method per number of arguments, so combining three values gives a `Tuple3`, never a
`Tuple2<Tuple2<A, B>, C>`. `zipWith` passes the values straight to `f`, without building a tuple.

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

`zipLeft` and `zipRight` keep one value, but still fail when either side fails. They are not `orElse`.

```java
Option<Integer> left = Option.some(1).zipLeft(Option.none());
Option<String> right = Option.some(1).zipRight(Option.some("kept"));
// None, Some(kept)
```

## Null

A `null` argument throws a `NullPointerException`. When `f` returns `null`:

- on `Option`, `Either` and `Validation`, the call throws a `NullPointerException`;
- on `Try`, the result is a `Failure`, as it is when `f` throws;
- on `Lazy`, the result holds `null`.
