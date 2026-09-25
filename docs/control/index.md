---
description: Option, Either, Try, Validation and Lazy - which one to pick, the members they share, conversions and the null policy.
---

# Control types

The control types hold the result of a step: a value that may be absent, a success or a failure, or a value computed
later. Each one has its own page:

- [`Option`](option.md): a value that may be absent.
- [`Either`](either.md): a result or an error, stopping at the first error.
- [`Try`](try.md): a computation that may have thrown.
- [`Validation`](validation.md): checks that keep every error.
- [`Lazy`](lazy.md): a value computed on first access, then cached.

## Values, not collections

`Option`, `Either`, `Try` and `Validation` are sealed interfaces whose cases are records. A `switch` over them needs
no `default`, and the compiler checks that every case is handled.

None of them is `Iterable`. Each has a short list of explicit conversions instead.

`Lazy` is a class of its own: it has no cases, is never empty, and has no failure side.

## Which one to pick

| You have | Use | Cases |
|---|---|---|
| a value that may be missing, and nothing to say about why | `Option<A>` | `Some(A value)`, `None()` |
| a result or an error value, and the first error stops the work | `Either<L, R>` | `Left(L value)`, `Right(R value)` |
| code that throws exceptions | `Try<A>` | `Success(A value)`, `Failure(Throwable cause)` |
| independent checks, and you want every error at once | `Validation<E, A>` | `Valid(A value)`, `Invalid(NonEmptyVector<E> errors)` |
| a value that is costly to compute and may not be needed | `Lazy<A>` | none: `get()` computes it |

## Members they share

The types share most of their members, with the same names and the same argument order, failure side first:

| Member | `Option` | `Either` | `Try` | `Validation` |
|---|---|---|---|---|
| `map`, `flatMap` | on `Some` | on `Right` | on `Success` | on `Valid` |
| `fold(ifFailure, ifSuccess)` | `Supplier`, `Function` | `Function<L>`, `Function<R>` | `Function<Throwable>`, `Function<A>` | `Function<NonEmptyVector<E>>`, `Function<A>` |
| `getOrElse(value)`, `getOrElse(Supplier)` | yes | yes, plus `getOrElse(Function<L, R>)` | yes, plus `getOrElse(Function<Throwable, A>)` | yes, plus `getOrElse(Function<NonEmptyVector<E>, A>)` |
| `getOrElseThrow`, `getOrNull` | yes | yes | yes | yes |
| `contains`, `exists`, `forAll`, `forEach` | yes | on the right side | on the success | on the valid value |
| `tap` | `tap`, `tapNone` | `tap`, `tapLeft` | `tap`, `tapError` | `tap`, `tapError` |
| `orElse` | `orElse(other)`, `orElse(Supplier)` | `orElse(other)`, `orElse(Supplier)` | `orElse(other)`, `orElse(Supplier)` | `orElse(Supplier)` |
| `zip`, `zipWith`, `zipLeft`, `zipRight` | yes | yes | yes | yes, keeping every error |
| static `collectAll`, `forEach`, `flatten` | yes | yes | yes | yes, keeping every error |

`Lazy` has `map`, `flatMap`, `tap`, the `zip` family, and the static `collectAll` and `flatten`.

### Many values at once

The static `collectAll` turns a collection of values into one value holding a `Vector`. The static `forEach` does the
same after applying a function to each element.

`Option`, `Either` and `Try` stop at the first `None`, `Left` or `Failure`. `Validation` keeps going and gathers
every error.

```java
Option<Vector<Integer>> all = Option.collectAll(Vector.of(Option.some(1), Option.some(2)));
Either<String, Vector<Integer>> parsed = Either.forEach(Vector.of("1", "x", "3"),
    s -> s.chars().allMatch(Character::isDigit) ? Either.right(Integer.parseInt(s)) : Either.left("bad: " + s));
// Some(Vector(1, 2)), Left(bad: x)
```

### Nested values

The static `flatten` removes one level of nesting, on every type including `Validation` and `Lazy`.

```java
Option<Integer> flat = Option.flatten(Option.some(Option.some(1)));
Either<String, Integer> inner = Either.flatten(Either.right(Either.left("inner failure")));
// Some(1), Left(inner failure)
```

## Conversions

The conversions are explicit and short; there is no `Iterable` to lean on.

| From | Conversions |
|---|---|
| `Option` | `toEither(Supplier<L>)`, `toTry(Supplier<Throwable>)`, `toValidation(Supplier<E>)`, `toVector()`, `toList()`, `toOptional()`, `stream()`; `Option.ofOptional(Optional)` the other way |
| `Either` | `toOption()`, `toTry(Function<L, Throwable>)`, `toValidation()`, `toVector()` |
| `Try` | `toOption()`, `toEither()`, `toValidation()`, `toVector()`, `toCompletableFuture()`; `Try.fromCompletableFuture` the other way |
| `Validation` | `toOption()`, `toEither()`, `toEitherWith(Function)`, `toTry(Function)`, `toVector()`; `fromOption`, `fromEither`, `fromTry` the other way |
| `Lazy` | `get()`, `toSupplier()` |

```java
Either<String, Integer> fromOption = Option.<Integer>none().toEither(() -> "missing");
java.util.Optional<Integer> optional = Option.some(5).toOptional();
Option<Integer> fromTry = Try.of(() -> Integer.parseInt("7")).toOption();
// Left(missing), Optional[5], Some(7)
```

## Null policy

`Some`, `Left`, `Right`, `Success` and `Valid` never hold `null`:

- Creating one with `null` throws a `NullPointerException`.
- `Option.ofNullable` turns a value that may be `null` into an `Option`, and `getOrNull()` goes back.
- A `Try` whose computation returns `null` is a `Failure` holding a `NullPointerException`.

```java
Option<String> absent = Option.ofNullable(null);
Try<String> nullResult = Try.of(() -> null);
boolean npe = nullResult.getCause() instanceof NullPointerException;
// None, Failure(java.lang.NullPointerException: ...), true
```

`Lazy` is the exception: it may hold `null`. Wrap its value explicitly, with `Option.ofNullable(lazy.get())`.

<script>
  // links to the sections of the former single page land on the page that now holds them
  (function () {
    var moved = {
      "lazy": "lazy/",
      "construction": "option/#construction",
      "switch-over-the-cases": "option/#switch-over-the-cases"
    };
    var target = moved[window.location.hash.slice(1)];
    if (target) {
      window.location.replace(target);
    }
  })();
</script>
