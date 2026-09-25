---
description: Try - a computation that may have thrown. Construction, pattern matching on Success and Failure, recovery, conversions and sharp edges.
---

# Try

`Try<A>` is the outcome of a computation that may throw: `Success(A value)` or `Failure(Throwable cause)`.

## When to use it

Use `Try` around code that throws: parsing, I/O, a library that reports errors with exceptions.

- If the error is a value you define rather than an exception, use [`Either`](either.md). `toEither()` converts.
- If several independent checks must all be reported, use [`Validation`](validation.md).

## Construction

`Try.of` takes a `Callable`, so checked exceptions need no wrapping. `Try.run` is for a computation with no result;
its success value is the empty tuple `()`.

```java
var parsed = Try.of(() -> Integer.parseInt("42")); // Try<Integer>
var failed = Try.of(() -> Integer.parseInt("forty-two")); // Try<Integer>
var ran = Try.run(() -> Thread.sleep(1)); // Try<Tuple0>
// Success(42), Failure(java.lang.NumberFormatException: For input string: "forty-two"), Success(())
```

`Try.success` and `Try.failure` build a case directly.

### Resources

To open resources, use them and close them with the outcome in a `Try`, see [`Using`](using.md).

### What is captured

`Try` captures every exception or error except the fatal ones, which are rethrown: `InterruptedException`,
`LinkageError`, `ThreadDeath` and `VirtualMachineError` (so `OutOfMemoryError` and `StackOverflowError`).

## Pattern matching over the cases

`Success` and `Failure` are records, so pattern matching with a `switch` expression needs no `default`:

```java
var result = Try.of(() -> Integer.parseInt("x")); // Try<Integer>
var report = switch (result) {
    case Success(var value) -> "parsed " + value;
    case Failure(var cause) -> "failed: " + cause.getClass().getSimpleName();
};
// "failed: NumberFormatException"
```

`fold(ifFailure, ifSuccess)` does the same with two functions.

## Recovering from a failure

`catchSome` recovers from one type of exception and leaves the others alone. `catchAll` recovers from any.

```java
var port = Try.of(() -> Integer.parseInt("80a"))
    .catchSome(NumberFormatException.class, e -> 8080)
    .map(p -> p + 1); // Try<Integer>
// Success(8081)
```

`mapError` replaces the cause, typically to wrap it in an exception of your domain.

```java
var recovered = Try.of(() -> Integer.parseInt("x")).catchAll(e -> 0); // Try<Integer>
var wrapped = Try.<Integer>failure(new java.io.IOException("disk"))
    .mapError(e -> new IllegalStateException("cannot read the configuration", e)); // Try<Integer>
// Success(0), Failure(java.lang.IllegalStateException: cannot read the configuration)
```

`catchAllWith` and `catchSomeWith` recover with another `Try`, which may fail in turn. `getOrElse(Function)` builds
a value from the cause.

## Chaining steps that throw

`map` and `flatMap` run under `Try`: an exception thrown by the function becomes the `Failure`. `filter` fails a
value that does not pass a test, with the exception you build.

```java
var ratio = Try.of(() -> 10).map(n -> 100 / (n - 10)); // Try<Integer>
var positive = Try.success(-1)
    .filter(n -> n > 0, n -> new IllegalArgumentException("not positive: " + n)); // Try<Integer>
// Failure(java.lang.ArithmeticException: / by zero), Failure(java.lang.IllegalArgumentException: not positive: -1)
```

`ensuring` runs an action whatever the outcome, like a `finally` block.

```java
var log = new StringBuilder();
var done = Try.of(() -> 1).ensuring(() -> log.append("closed")); // Try<Integer>
// Success(1), and log is "closed"
```

Other members:

- `mapTry` and `flatMapTry` take a function that throws checked exceptions.
- `andThen` and `andThenTry` run a side effect on a success; if it throws, the result is a `Failure`.
- `tap` runs an action on the value; `tapError` on the cause, optionally only for one exception type.
- `zip` and `zipWith` combine several `Try`s and stop at the first `Failure`; see [zip at arity N](../zip.md).

## Conversions

- `toEither()` gives an `Either<Throwable, A>`; `toValidation()` a `Validation<Throwable, A>`.
- `toOption()` and `toVector()` keep the value and drop the cause.
- `toCompletableFuture()` gives an already completed future. `Try.fromCompletableFuture` waits for a future and
  captures its outcome.

```java
var either = Try.of(() -> Integer.parseInt("7")).toEither(); // Either<Throwable, Integer>
var future = Try.success(7).toCompletableFuture(); // java.util.concurrent.CompletableFuture<Integer>
var back = Try.fromCompletableFuture(future); // Try<Integer>
// Right(7), a completed future, Success(7)
```

## Sharp edges

### Two failures are rarely equal

`Failure` compares its cause with the cause's own `equals`. JDK exceptions do not override it, so two failures are
equal only if they hold the same exception object.

Two failures of the same input are not equal. Compare the class or the message of the cause instead.

```java
var first = Try.of(() -> Integer.parseInt("x")); // Try<Integer>
var second = Try.of(() -> Integer.parseInt("x")); // Try<Integer>
var same = first.equals(second);
var sameClass = first.getCause().getClass() == second.getCause().getClass();
// same is false, sameClass is true
```

### `get()` rethrows the cause

`get()` on a `Failure` throws the cause itself, even a checked exception that the method does not declare. Prefer
pattern matching, `fold` or `getOrElse`.

### `null` is a failure

A `Success` never holds `null`. A computation that returns `null` gives a `Failure` of a `NullPointerException`.

This includes `Try.fromCompletableFuture` on a future completed with `null`, such as a `CompletableFuture<Void>`: map
it to a value first.

### Fatal errors escape

The fatal errors listed above are rethrown by every method, never captured. `Try` is not a way to survive an
`OutOfMemoryError`.
