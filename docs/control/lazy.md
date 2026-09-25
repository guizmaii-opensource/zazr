---
description: Lazy - a value computed on first access, then cached. Construction, operations, conversions and sharp edges.
---

# Lazy

`Lazy<A>` is a value computed the first time it is read, then cached. Later reads return the cached value without
computing it again.

## When to use it

Use `Lazy` for a value that is costly to compute and may never be needed.

A plain `Supplier` computes its value on every call; a `Lazy` computes it once. Unlike the other control types, it
has no failure case and is never empty.

## Construction and reading

`Lazy.of` takes a `Supplier` and computes nothing. `get()` computes the value on the first call. `isEvaluated()`
tells whether that has happened.

```java
var answer = Lazy.of(() -> 6 * 7); // Lazy<Integer>
var before = answer.isEvaluated();
var value = answer.map(n -> n + 1).get(); // Integer
var after = answer.isEvaluated();
// before is false, value is 43, after is true
```

`Lazy` is a class with no cases, so there is nothing to `switch` over: `get()` is how you read it.

## Operations

`map`, `flatMap`, `zip`, `zipWith` and the other combinators return a new `Lazy` that is not computed yet. The
values they depend on are computed when the result is.

```java
var host = Lazy.of(() -> "localhost"); // Lazy<String>
var port = Lazy.of(() -> 8080); // Lazy<Integer>
var address = host.zipWith(port, (h, p) -> h + ":" + p); // Lazy<String>
var evaluated = host.isEvaluated();
var value = address.get(); // String
// evaluated is false, value is "localhost:8080"
```

The static `collectAll` turns many `Lazy` values into one `Lazy` of a `Vector`, and `flatten` removes one level of
nesting. Both compute nothing until the result is read.

```java
var all = Lazy.collectAll(Vector.of(Lazy.of(() -> 1), Lazy.of(() -> 2))); // Lazy<Vector<Integer>>
// all.get() is Vector(1, 2)
```

## Conversions

`get()` gives the value. `toSupplier()` passes a `Lazy` to an API that takes a `Supplier`; the supplier shares the
cache, so the value is still computed once.

```java
var calls = new int[] {0};
var greeting = Lazy.of(() -> {
    calls[0]++;
    return "hello";
}); // Lazy<String>
var supplier = greeting.toSupplier(); // java.util.function.Supplier<String>
var twice = supplier.get() + supplier.get();
// twice is "hellohello", calls[0] is 1: computed once
```

A `Lazy` may hold `null`, so wrapping its value in another control type is explicit: `Option.ofNullable(lazy.get())`.

## Sharp edges

### A failed computation is not cached

If the computation throws, the exception reaches the caller of `get()` and nothing is cached. The next `get()` runs
the computation again.

```java
var attempts = new java.util.concurrent.atomic.AtomicInteger();
var flaky = Lazy.of(() -> {
    if (attempts.incrementAndGet() == 1) {
        throw new IllegalStateException("not yet");
    }
    return "ready";
}); // Lazy<String>
var first = Try.of(flaky::get); // Try<String>
var second = flaky.get(); // String
// first is Failure(java.lang.IllegalStateException: not yet), second is "ready", attempts is 2
```

### `equals` and `hashCode` compute the value

Two `Lazy` values are equal when their values are, so `equals` and `hashCode` compute them. `toString` does not: it
prints `Lazy(?)` until the value is computed.

```java
var unread = Lazy.of(() -> 1); // Lazy<Integer>
var other = Lazy.of(() -> 1); // Lazy<Integer>
var shown = unread.toString();
var equal = unread.equals(other);
// shown is "Lazy(?)", equal is true, and both are now evaluated
```

### `tap` computes the value

`tap` runs its action on the value right away, so it computes the value if needed. It does not wait for a later
`get()`.
