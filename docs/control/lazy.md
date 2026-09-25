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
Lazy<Integer> answer = Lazy.of(() -> 6 * 7);
boolean before = answer.isEvaluated();
int value = answer.map(n -> n + 1).get();
boolean after = answer.isEvaluated();
// before is false, value is 43, after is true
```

`Lazy` is a class with no cases, so there is nothing to pattern match on: `get()` is how you read it.

## Operations

`map`, `flatMap`, `zip`, `zipWith` and the other combinators return a new `Lazy` that is not computed yet. The
values they depend on are computed when the result is.

```java
Lazy<String> host = Lazy.of(() -> "localhost");
Lazy<Integer> port = Lazy.of(() -> 8080);
Lazy<String> address = host.zipWith(port, (h, p) -> h + ":" + p);
boolean evaluated = host.isEvaluated();
String value = address.get();
// evaluated is false, value is "localhost:8080"
```

The static `collectAll` turns many `Lazy` values into one `Lazy` of a `Vector`, and `flatten` removes one level of
nesting. Both compute nothing until the result is read.

```java
Lazy<Vector<Integer>> all = Lazy.collectAll(Vector.of(Lazy.of(() -> 1), Lazy.of(() -> 2)));
// all.get() is Vector(1, 2)
```

## Conversions

`get()` gives the value. `toSupplier()` passes a `Lazy` to an API that takes a `Supplier`; the supplier shares the
cache, so the value is still computed once.

```java
int[] calls = {0};
Lazy<String> greeting = Lazy.of(() -> {
    calls[0]++;
    return "hello";
});
java.util.function.Supplier<String> supplier = greeting.toSupplier();
String twice = supplier.get() + supplier.get();
// twice is "hellohello", calls[0] is 1: computed once
```

A `Lazy` may hold `null`, so wrapping its value in another control type is explicit: `Option.ofNullable(lazy.get())`.

## Sharp edges

### A failed computation is not cached

If the computation throws, the exception reaches the caller of `get()` and nothing is cached. The next `get()` runs
the computation again.

```java
java.util.concurrent.atomic.AtomicInteger attempts = new java.util.concurrent.atomic.AtomicInteger();
Lazy<String> flaky = Lazy.of(() -> {
    if (attempts.incrementAndGet() == 1) {
        throw new IllegalStateException("not yet");
    }
    return "ready";
});
Try<String> first = Try.of(flaky::get);
String second = flaky.get();
// first is Failure(java.lang.IllegalStateException: not yet), second is "ready", attempts is 2
```

### `equals` and `hashCode` compute the value

Two `Lazy` values are equal when their values are, so `equals` and `hashCode` compute them. `toString` does not: it
prints `Lazy(?)` until the value is computed.

```java
Lazy<Integer> unread = Lazy.of(() -> 1);
Lazy<Integer> other = Lazy.of(() -> 1);
String shown = unread.toString();
boolean equal = unread.equals(other);
// shown is "Lazy(?)", equal is true, and both are now evaluated
```

### `tap` computes the value

`tap` runs its action on the value right away, so it computes the value if needed. It does not wait for a later
`get()`.
