---
description: Either - a result or an error, right-biased. Construction, pattern matching on Left and Right, operations, conversions and sharp edges.
---

# Either

`Either<L, R>` is one of two values: `Left(L value)` or `Right(R value)`. By convention the left side is the error
and the right side the result.

`Either` is right-biased: `map`, `flatMap` and most other members work on the right value and pass a `Left` through
unchanged.

## When to use it

Use `Either` for a step that can fail with an error you define, when the first error stops the work.

- If the checks are independent and you want every error at once, use [`Validation`](validation.md).
- If the error is an exception thrown by the code you call, use [`Try`](try.md).
- If there is nothing to say about the failure, [`Option`](option.md) is enough.

## Construction

```java
var right = Either.<String, Integer>right(42); // Either<String, Integer>
var left = Either.<String, Integer>left("not a number"); // Either<String, Integer>
var checked = Either.fromPredicate(-1, n -> n >= 0, n -> "negative: " + n); // Either<String, Integer>
// Right(42), Left(not a number), Left(negative: -1)
```

`fromPredicate` keeps the value as a `Right` when the test holds. Otherwise the last function turns the rejected
value into the `Left`, so the error can name it; write `_ -> "negative"` when it doesn't need to.

## Pattern matching over the cases

`Left` and `Right` are records, so pattern matching with a `switch` expression needs no `default`:

```java
var result = Either.<String, Integer>right(42);
var text = switch (result) {
    case Right(var value) -> "got " + value;
    case Left(var error) -> "failed: " + error;
};
// "got 42"
```

`fold(ifLeft, ifRight)` does the same with two functions.

## Operations

### Chaining steps

`flatMap` runs the next step only on a `Right`. `mapLeft` transforms the error.

```java
var total = Either.<String, Integer>right(2)
    .flatMap(n -> n > 0 ? Either.right(n * 10) : Either.left("not positive"))
    .mapLeft(error -> "rejected: " + error); // Either<String, Integer>
// Right(20)
```

### Rejecting a value

`filterOrElse` turns a `Right` that fails a test into a `Left`, built from the rejected value. `flip` swaps the two
sides.

```java
var adult = Either.<String, Integer>right(15)
    .filterOrElse(n -> n >= 18, n -> n + " is under 18"); // Either<String, Integer>
var message = adult.fold(error -> "rejected: " + error, n -> "accepted: " + n); // String
var flipped = adult.flip(); // Either<Integer, String>
// Left(15 is under 18), "rejected: 15 is under 18", Right(15 is under 18)
```

### Other members

- `mapBoth(leftMapper, rightMapper)` transforms whichever side is present.
- `getOrElse(Function)` builds a result from the left value; `getOrElseThrow(Function)` builds an exception from it.
- `tap` runs an action on a right value; `tapLeft` on a left one.
- `isLeft()`, `isRight()` and `getLeft()` read the case directly.
- `zip` and `zipWith` combine several `Either`s and stop at the first `Left`; see [zip at arity N](../zip.md).

## Conversions

- `toOption()` keeps the right value and drops the left one.
- `toTry(Function)` builds the `Failure` cause from the left value.
- `toValidation()` gives `Valid` or an `Invalid` with one error.
- `toVector()` gives zero or one element.

```java
var missing = Either.<String, Integer>left("missing");
var option = missing.toOption(); // Option<Integer>
var attempt = missing.toTry(IllegalArgumentException::new); // Try<Integer>
var validation = missing.toValidation(); // Validation<String, Integer>
// None, Failure(java.lang.IllegalArgumentException: missing), Invalid(missing)
```

## Sharp edges

- Neither side holds `null`: `Either.left(null)` and `Either.right(null)` throw a `NullPointerException`.
- `get()` on a `Left` throws a `NoSuchElementException`. Prefer pattern matching, `fold` or `getOrElse`.
- There is no `filter`: a rejected value needs a left value, so the method is `filterOrElse`.
- `toTry` always takes a function, even when the left side is already a `Throwable`; pass `t -> t`.
- `Either` stops at the first `Left`. To report every error, use [`Validation`](validation.md).
