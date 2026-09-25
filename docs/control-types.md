---
description: Option, Either, Try and Lazy - construction, switch over their records, the members they share, conversions and the null policy.
---

# Control types

`Option`, `Either`, `Try` and `Validation` are sealed interfaces whose cases are records. They are values, not
collections: none of them is `Iterable`, and each has a short list of explicit conversions. `Validation` has
[its own page](validation.md); `Lazy` is at the end of this one.

| Type | Cases | Means |
|---|---|---|
| `Option<A>` | `Some(A value)`, `None()` | a value that may be absent |
| `Either<L, R>` | `Left(L value)`, `Right(R value)` | a result that is either an `L` or an `R`; right-biased |
| `Try<A>` | `Success(A value)`, `Failure(Throwable cause)` | a computation that may have thrown |
| `Validation<E, A>` | `Valid(A value)`, `Invalid(NonEmptyVector<E> errors)` | a check that keeps every error |

## Construction

```java
Option<Integer> some = Option.some(1);
Option<Integer> none = Option.none();
Option<String> fromNullable = Option.ofNullable(null);
Option<Integer> when = Option.when(3 > 2, () -> 3);
// Some(1), None, None, Some(3)
```

```java
Either<String, Integer> right = Either.right(42);
Either<String, Integer> left = Either.left("not a number");
Either<String, Integer> checked = Either.fromPredicate(-1, n -> n >= 0, () -> "negative");
// Right(42), Left(not a number), Left(negative)
```

`Try.of` takes a `Callable`, so checked exceptions need no wrapping. `Try.run` is for a computation with no result.

```java
Try<Integer> parsed = Try.of(() -> Integer.parseInt("42"));
Try<Integer> failed = Try.of(() -> Integer.parseInt("forty-two"));
Try<Tuple0> ran = Try.run(() -> Thread.sleep(1));
// Success(42), Failure(java.lang.NumberFormatException: For input string: "forty-two"), Success(())
```

`Try` captures every non-fatal throwable. `InterruptedException`, `LinkageError`, `ThreadDeath` and
`VirtualMachineError` (so `OutOfMemoryError` and `StackOverflowError`) are fatal: they are rethrown, never wrapped.

## `switch` over the cases

The cases are records of a sealed interface, so a `switch` is exhaustive without a `default`, can deconstruct nested
records and can guard with `when`. It is the primary way to take a value apart; `fold` is there for expression
position.

```java
Option<Integer> age = Option.some(17);
String label = switch (age) {
    case Some(var years) when years >= 18 -> "adult";
    case Some(var years) -> "minor, " + years;
    case None() -> "unknown";
};
// "minor, 17"
```

```java
Try<Integer> result = Try.of(() -> Integer.parseInt("x"));
String report = switch (result) {
    case Success(var value) -> "parsed " + value;
    case Failure(var cause) -> "failed: " + cause.getClass().getSimpleName();
};
// "failed: NumberFormatException"
```

## Members they share

Each type declares these itself, with the same names and argument order, the failure side first:

| Member | `Option` | `Either` | `Try` |
|---|---|---|---|
| `map`, `flatMap` | on `Some` | on `Right` | on `Success` |
| `fold(ifFailure, ifSuccess)` | `Supplier`, `Function` | `Function<L>`, `Function<R>` | `Function<Throwable>`, `Function<A>` |
| `getOrElse(value)`, `getOrElse(Supplier)` | yes | yes, plus `getOrElse(Function<L, R>)` | yes, plus `getOrElse(Function<Throwable, A>)` |
| `getOrElseThrow`, `getOrNull` | yes | yes | yes |
| `contains`, `exists`, `forAll`, `forEach` | yes | on the right side | on the success |
| `tap` | `tap`, `tapNone` | `tap`, `tapLeft` | `tap`, `tapError` |
| `orElse(other)`, `orElse(Supplier)` | yes | yes | yes |
| `zip`, `zipWith`, `zipLeft`, `zipRight` | yes | yes | yes |
| static `collectAll`, `forEach` | yes | yes | yes |

And the members only one of them has: `Option.filter`, `Option.collect`; `Either.mapLeft`, `Either.mapBoth`,
`Either.flip`, `Either.filterOrElse`; `Try.catchAll`, `Try.catchSome`, `Try.catchAllWith`, `Try.catchSomeWith`,
`Try.mapError`, `Try.ensuring`, `Try.mapTry`, `Try.withResources`.

```java
Try<Integer> port = Try.of(() -> Integer.parseInt("80a"))
    .catchSome(NumberFormatException.class, e -> 8080)
    .map(p -> p + 1);
// Success(8081)
```

```java
Either<String, Integer> total = Either.<String, Integer>right(2)
    .flatMap(n -> n > 0 ? Either.right(n * 10) : Either.left("not positive"))
    .mapLeft(error -> "rejected: " + error);
// Right(20)
```

`static collectAll` turns many values into one, stopping at the first `None`, `Left` or `Failure`; `static forEach`
maps first. Both return a `Vector`.

```java
Option<Vector<Integer>> all = Option.collectAll(Vector.of(Option.some(1), Option.some(2)));
Either<String, Vector<Integer>> parsed = Either.forEach(Vector.of("1", "x", "3"),
    s -> s.chars().allMatch(Character::isDigit) ? Either.right(Integer.parseInt(s)) : Either.left("bad: " + s));
// Some(Vector(1, 2)), Left(bad: x)
```

## Conversions

The conversions are explicit and short; there is no `Iterable` to lean on.

| From | Conversions |
|---|---|
| `Option` | `toEither(Supplier<L>)`, `toTry(Supplier<Throwable>)`, `toValidation(Supplier<E>)`, `toVector()`, `toList()`, `toOptional()`, `stream()`; `Option.ofOptional(Optional)` the other way |
| `Either` | `toOption()`, `toTry(Function<L, Throwable>)`, `toValidation()`, `toVector()` |
| `Try` | `toOption()`, `toEither()`, `toValidation()`, `toVector()`, `toCompletableFuture()`; `Try.fromCompletableFuture` the other way |
| `Validation` | `toOption()`, `toEither()`, `toEitherWith(Function)`, `toTry(Function)`, `toVector()` |

```java
Either<String, Integer> fromOption = Option.<Integer>none().toEither(() -> "missing");
java.util.Optional<Integer> optional = Option.some(5).toOptional();
Option<Integer> fromTry = Try.of(() -> Integer.parseInt("7")).toOption();
// Left(missing), Optional[5], Some(7)
```

## Null policy

`Some`, `Right`, `Success` and `Valid` never hold `null`: their factories and record constructors throw
`NullPointerException`. `Option.ofNullable` is the door from nullable code. `Try.of`, `mapTry` and
`fromCompletableFuture` capture a `null` result as a `Failure` of a `NullPointerException`, like any other non-fatal
outcome. `getOrNull()` is the door back.

```java
Option<String> absent = Option.ofNullable(null);
Try<String> nullResult = Try.of(() -> null);
boolean npe = nullResult.getCause() instanceof NullPointerException;
// None, Failure(java.lang.NullPointerException: ...), true
```

## `Lazy`

`Lazy<A>` is a value computed on first access and cached. It is not a control type in the sense above: it has no
failure case, is never empty, and `get()` is its only conversion. If the computation throws, nothing is cached and the
next `get()` runs it again. Unlike the four types above, a `Lazy` may hold `null`.

```java
Lazy<Integer> answer = Lazy.of(() -> 6 * 7);
boolean before = answer.isEvaluated();
int value = answer.map(n -> n + 1).get();
boolean after = answer.isEvaluated();
// before is false, value is 43, after is true
```

`map`, `flatMap`, `zip`, `zipWith` and `tap` return a new unevaluated `Lazy`; `collectAll` turns many into one.
`toSupplier()` hands it to an API that takes a `Supplier`.
