---
description: Option - a value that may be absent. Construction, switch over Some and None, operations, conversions and sharp edges.
---

# Option

`Option<A>` is a value that may be absent: `Some(A value)` or `None()`. It replaces `java.util.Optional`.

## When to use it

Use `Option` when a value may be missing and there is nothing more to say about why: a lookup, an optional field.

- If the caller needs to know why the value is missing, return an [`Either`](either.md).
- If the value comes from code that throws, use [`Try`](try.md).

## Construction

```java
Option<Integer> some = Option.some(1);
Option<Integer> none = Option.none();
Option<String> fromNullable = Option.ofNullable(null);
Option<Integer> when = Option.when(3 > 2, () -> 3);
// Some(1), None, None, Some(3)
```

`Option.some(null)` throws: use `Option.ofNullable` for a value that may be `null`. `Option.when` calls its supplier
only when the condition holds.

## `switch` over the cases

`Some` and `None` are records, so a `switch` can take the value out and add conditions with `when`. It needs no
`default`.

```java
Option<Integer> age = Option.some(17);
String label = switch (age) {
    case Some(var years) when years >= 18 -> "adult";
    case Some(var years) -> "minor, " + years;
    case None() -> "unknown";
};
// "minor, 17"
```

`fold(ifNone, ifSome)` does the same with a `Supplier` and a `Function`.

## Operations

`filter`, `map` and `flatMap` work on the value of a `Some` and leave a `None` alone. `getOrElse` gives the value
or a default.

```java
int port = Option.some("8080")
    .filter(s -> s.chars().allMatch(Character::isDigit))
    .map(Integer::parseInt)
    .getOrElse(80);
String shown = Option.<Integer>none().fold(() -> "no value", n -> "n = " + n);
// 8080, "no value"
```

`mapTry` runs a function that may throw, and returns a `Try`. On a `None`, the result is a `Failure` of a
`NoSuchElementException`.

```java
Try<Integer> parsed = Option.some("x").mapTry(Integer::parseInt);
Try<Integer> absent = Option.<String>none().mapTry(Integer::parseInt);
// Failure(java.lang.NumberFormatException: For input string: "x"), Failure(java.util.NoSuchElementException: ...)
```

Other members:

- `isDefined()` and `isEmpty()` test the case.
- `tap` runs an action on the value; `tapNone` runs one when there is none.
- `orElse` gives another `Option` when this one is `None`.
- `collect` is `flatMap` spelled the way the collections spell it.
- `zip` and `zipWith` combine several options; see [zip at arity N](../zip.md).

## Conversions

- `toEither(Supplier)`, `toTry(Supplier)` and `toValidation(Supplier)` supply the failure for a `None`.
- `toVector()`, `toList()` and `stream()` give zero or one element.
- `toOptional()` and `Option.ofOptional(Optional)` go to and from `java.util.Optional`.

```java
Option<Integer> fromOptional = Option.ofOptional(java.util.Optional.of(3));
Either<String, Integer> either = fromOptional.toEither(() -> "missing");
Validation<String, Integer> validation = Option.<Integer>none().toValidation(() -> "missing");
java.util.Optional<Integer> back = fromOptional.toOptional();
// Some(3), Right(3), Invalid(missing), Optional[3]
```

## Sharp edges

### `map` to `null` throws

`Optional.map` turns a `null` result into an empty `Optional`. `Option.map` throws a `NullPointerException`
instead, because `Some` cannot hold `null`.

To map to a value that may be absent, use `flatMap` with `Option.ofNullable`:

```java
java.util.Map<String, String> env = java.util.Map.of("HOME", "/home/ada");
Option<String> shell = Option.some("SHELL").flatMap(key -> Option.ofNullable(env.get(key)));
// None, where map(env::get) would throw
```

### `get()` on `None` throws

`get()` throws a `NoSuchElementException` on a `None`. Prefer a `switch`, `fold` or `getOrElse`.

### Not a collection

An `Option` is not `Iterable`, so it cannot go in a `for` loop. Convert it with `toVector()` or `stream()`.
