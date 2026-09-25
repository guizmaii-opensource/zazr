---
description: Try - a computation that may have thrown. Construction, switch over Success and Failure, recovery, conversions and sharp edges.
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
Try<Integer> parsed = Try.of(() -> Integer.parseInt("42"));
Try<Integer> failed = Try.of(() -> Integer.parseInt("forty-two"));
Try<Tuple0> ran = Try.run(() -> Thread.sleep(1));
// Success(42), Failure(java.lang.NumberFormatException: For input string: "forty-two"), Success(())
```

`Try.success` and `Try.failure` build a case directly.

### Resources

`Try.withResources` opens a resource, uses it and closes it, like a `try`-with-resources block:

```java
Try<String> firstLine = Try.withResources(() -> new java.io.BufferedReader(new java.io.StringReader("a\nb")),
    java.io.BufferedReader::readLine);
// Success(a)
```

### What is captured

`Try` captures every exception or error except the fatal ones, which are rethrown: `InterruptedException`,
`LinkageError`, `ThreadDeath` and `VirtualMachineError` (so `OutOfMemoryError` and `StackOverflowError`).

## `switch` over the cases

`Success` and `Failure` are records, so a `switch` over them needs no `default`:

```java
Try<Integer> result = Try.of(() -> Integer.parseInt("x"));
String report = switch (result) {
    case Success(var value) -> "parsed " + value;
    case Failure(var cause) -> "failed: " + cause.getClass().getSimpleName();
};
// "failed: NumberFormatException"
```

`fold(ifFailure, ifSuccess)` does the same with two functions.

## Recovering from a failure

`catchSome` recovers from one type of exception and leaves the others alone. `catchAll` recovers from any.

```java
Try<Integer> port = Try.of(() -> Integer.parseInt("80a"))
    .catchSome(NumberFormatException.class, e -> 8080)
    .map(p -> p + 1);
// Success(8081)
```

`mapError` replaces the cause, typically to wrap it in an exception of your domain.

```java
Try<Integer> recovered = Try.of(() -> Integer.parseInt("x")).catchAll(e -> 0);
Try<Integer> wrapped = Try.<Integer>failure(new java.io.IOException("disk"))
    .mapError(e -> new IllegalStateException("cannot read the configuration", e));
// Success(0), Failure(java.lang.IllegalStateException: cannot read the configuration)
```

`catchAllWith` and `catchSomeWith` recover with another `Try`, which may fail in turn. `getOrElse(Function)` builds
a value from the cause.

## Chaining steps that throw

`map` and `flatMap` run under `Try`: an exception thrown by the function becomes the `Failure`. `filter` fails a
value that does not pass a test, with the exception you build.

```java
Try<Integer> ratio = Try.of(() -> 10).map(n -> 100 / (n - 10));
Try<Integer> positive = Try.success(-1)
    .filter(n -> n > 0, n -> new IllegalArgumentException("not positive: " + n));
// Failure(java.lang.ArithmeticException: / by zero), Failure(java.lang.IllegalArgumentException: not positive: -1)
```

`ensuring` runs an action whatever the outcome, like a `finally` block.

```java
StringBuilder log = new StringBuilder();
Try<Integer> done = Try.of(() -> 1).ensuring(() -> log.append("closed"));
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
Either<Throwable, Integer> either = Try.of(() -> Integer.parseInt("7")).toEither();
java.util.concurrent.CompletableFuture<Integer> future = Try.success(7).toCompletableFuture();
Try<Integer> back = Try.fromCompletableFuture(future);
// Right(7), a completed future, Success(7)
```

## Sharp edges

### Two failures are rarely equal

`Failure` compares its cause with the cause's own `equals`. JDK exceptions do not override it, so two failures are
equal only if they hold the same exception object.

Two failures of the same input are not equal. Compare the class or the message of the cause instead.

```java
Try<Integer> first = Try.of(() -> Integer.parseInt("x"));
Try<Integer> second = Try.of(() -> Integer.parseInt("x"));
boolean same = first.equals(second);
boolean sameClass = first.getCause().getClass() == second.getCause().getClass();
// same is false, sameClass is true
```

### `get()` rethrows the cause

`get()` on a `Failure` throws the cause itself, even a checked exception that the method does not declare. Prefer a
`switch`, `fold` or `getOrElse`.

### `null` is a failure

A `Success` never holds `null`. A computation that returns `null` gives a `Failure` of a `NullPointerException`.

This includes `Try.fromCompletableFuture` on a future completed with `null`, such as a `CompletableFuture<Void>`: map
it to a value first.

### Fatal errors escape

The fatal errors listed above are rethrown by every method, never captured. `Try` is not a way to survive an
`OutOfMemoryError`.
