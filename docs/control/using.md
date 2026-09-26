---
description: Using - resources released after use, whatever happened, with the outcome in a Try. One resource, many resources, release order and which exception surfaces.
---

# Using

`Using` runs code over resources and releases them afterwards, whatever happened. The outcome is a
[`Try`](try.md).

## When to use it

Java's `try`-with-resources releases resources too. Use `Using` when:

- you want the outcome as a `Try` rather than a thrown exception;
- the number of resources is known only at run time, such as one per file of a list;
- a value to release is not an `AutoCloseable`, such as a held lock;
- an error thrown by `close()`, such as an `OutOfMemoryError`, must not be hidden under the exception of your code.

## One resource

`Using.of` takes the code that opens the resource and the code that uses it. The resource is closed after use.

```java
var firstLine = Using.of(() -> new BufferedReader(new StringReader("a\nb")), BufferedReader::readLine); // Try<String>
// Success(a)
```

If opening or using the resource throws, the result is a `Failure`. A resource that was opened is always closed.

## Many resources

`Using.manager` passes a manager to your code. Every resource given to its `acquire` is released when your code
ends. `acquire` returns what it was given, so it wraps the expression that opens the resource.

```java
var sources = Vector.of("alpha", "beta", "gamma");
var length = Using.manager(use -> { // Try<Integer>
    var total = 0;
    for (var source : sources) {
        var reader = use.acquire(new BufferedReader(new StringReader(source)));
        total += reader.readLine().length();
    }
    return total;
});
// Success(14), and the three readers are closed
```

The readers stay open until your code returns, then they are all closed.

## A value that is not `AutoCloseable`

`acquire(value, release)` registers any value together with the code that releases it.

```java
var lock = new ReentrantLock();
var held = Using.manager(use -> { // Try<Boolean>
    lock.lock();
    use.acquire(lock, ReentrantLock::unlock);
    return lock.isHeldByCurrentThread();
});
// Success(true), and the lock is released
```

A lambda is an `AutoCloseable`, so `acquire(() -> ...)` registers a release action on its own.

## Release order

Resources are released in reverse order of acquisition, as nested `try`-with-resources blocks would. A resource that
depends on an earlier one is released first.

```java
var log = new StringBuilder();
var result = Using.manager(use -> { // Try<String>
    use.acquire(() -> log.append("connection closed; "));
    use.acquire(() -> log.append("statement closed; "));
    return "done";
});
// Success(done), and log is "statement closed; connection closed; "
```

Every release runs, even when your code or another release throws.

## Which exception surfaces

When your code throws, its exception is the `Failure`. An exception thrown by a release afterwards is added to it
as suppressed, as `try`-with-resources does.

```java
AutoCloseable resource = () -> {
    throw new IllegalStateException("close failed");
};
var result = Using.<AutoCloseable, String>of(() -> resource, r -> { // Try<String>
    throw new IOException("read failed");
});
var suppressed = result.getCause().getSuppressed(); // Throwable[]
// Failure(java.io.IOException: read failed), and suppressed holds the IllegalStateException
```

The same holds for any number of exceptions: the first one thrown surfaces and the later ones are suppressed in it.

### A more severe exception wins

Some throwables are more severe than others. From the most severe:

1. `VirtualMachineError`, such as `OutOfMemoryError` and `StackOverflowError`;
2. `LinkageError`;
3. `InterruptedException` and `ThreadDeath`;
4. any other exception.

When a later throwable is more severe, it surfaces instead, with the earlier one suppressed in it. So an
`OutOfMemoryError` thrown by `close()` is never hidden under the exception of your code, as it would be by
`try`-with-resources.

The first three levels are fatal for `Try`: they are rethrown rather than returned as a `Failure`. See
[what `Try` captures](try.md#what-is-captured).

### Nothing is suppressed in itself

A `close()` often rethrows the exception your code threw, when both fail for the same reason. That exception
surfaces once, with nothing added to it.

## Sharp edges

### The manager works only inside your code

Once your code has returned, the manager releases anything given to `acquire` at once and throws an
`IllegalStateException`. Do not keep it for later, and do not acquire through it from a release.

```java
var escaped = new AtomicReference<Using.Manager>();
var done = Using.manager(use -> { // Try<String>
    escaped.set(use);
    return "done";
});
var log  = new StringBuilder();
var late = Try.run(() -> escaped.get().acquire(() -> log.append("released at once")));
// Failure(java.lang.IllegalStateException: ...), and log is "released at once"
```

A manager is not safe to share between threads.

### `null` is a failure

A `null` result gives a `Failure` of a `NullPointerException`, as with `Try.of`. So does a `null` resource, after the
resources acquired before it are released.
