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

`Try` captures every exception except the fatal ones, which are rethrown: `InterruptedException`, `LinkageError`,
`ThreadDeath` and `VirtualMachineError` (so `OutOfMemoryError` and `StackOverflowError`).

## `switch` over the cases

The cases are records of a sealed interface, so a `switch` needs no `default`, can take nested records apart and
can add conditions with `when`. It is the usual way to read a value; `fold` does the same with two functions.

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

The types share most of their members, with the same names and the same argument order, failure side first:

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
| static `collectAll`, `forEach`, `flatten` | yes | yes | yes |

Each type also has members of its own: `Try` recovers from an exception with `catchAll` and `catchSome`, and
`Either` transforms its left side with `mapLeft`.

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

The static `collectAll` turns a collection of values into one value holding a `Vector`. It stops at the first
`None`, `Left` or `Failure`. The static `forEach` does the same after applying a function to each element.

```java
Option<Vector<Integer>> all = Option.collectAll(Vector.of(Option.some(1), Option.some(2)));
Either<String, Vector<Integer>> parsed = Either.forEach(Vector.of("1", "x", "3"),
    s -> s.chars().allMatch(Character::isDigit) ? Either.right(Integer.parseInt(s)) : Either.left("bad: " + s));
// Some(Vector(1, 2)), Left(bad: x)
```

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
| `Validation` | `toOption()`, `toEither()`, `toEitherWith(Function)`, `toTry(Function)`, `toVector()` |

```java
Either<String, Integer> fromOption = Option.<Integer>none().toEither(() -> "missing");
java.util.Optional<Integer> optional = Option.some(5).toOptional();
Option<Integer> fromTry = Try.of(() -> Integer.parseInt("7")).toOption();
// Left(missing), Optional[5], Some(7)
```

## Null policy

`Some`, `Right`, `Success` and `Valid` never hold `null`:

- Creating one with `null` throws a `NullPointerException`.
- `Option.ofNullable` turns a value that may be `null` into an `Option`, and `getOrNull()` goes back.
- A `Try` whose computation returns `null` is a `Failure` holding a `NullPointerException`.

```java
Option<String> absent = Option.ofNullable(null);
Try<String> nullResult = Try.of(() -> null);
boolean npe = nullResult.getCause() instanceof NullPointerException;
// None, Failure(java.lang.NullPointerException: ...), true
```

## `Lazy`

`Lazy<A>` is a value computed on first access, then cached. Unlike the types above, it has no failure case, is
never empty, and may hold `null`. If the computation throws, nothing is cached and the next `get()` runs it again.

```java
Lazy<Integer> answer = Lazy.of(() -> 6 * 7);
boolean before = answer.isEvaluated();
int value = answer.map(n -> n + 1).get();
boolean after = answer.isEvaluated();
// before is false, value is 43, after is true
```

`map`, `flatMap`, `zip` and the other operations return a new `Lazy` that is not computed yet. `toSupplier()`
passes it to an API that takes a `Supplier`.
