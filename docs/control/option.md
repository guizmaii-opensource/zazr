---
description: Option - a value that may be absent. Construction, pattern matching on Some and None, operations, conversions and sharp edges.
---

# Option

`Option<A>` is a value that may be absent: `Some(A value)` or `None()`. It replaces `java.util.Optional`.

## When to use it

Use `Option` when a value may be missing and there is nothing more to say about why: a lookup, an optional field.

- If the caller needs to know why the value is missing, return an [`Either`](either.md).
- If the value comes from code that throws, use [`Try`](try.md).

## Construction

```java
var some         = Option.some(1);                   // Option<Integer>
var none         = Option.<Integer>none();           // Option<Integer>
var fromNullable = Option.<String>ofNullable(null);  // Option<String>
var when         = Option.when(3 > 2, () -> 3);      // Option<Integer>
// Some(1), None, None, Some(3)
```

`Option.some(null)` throws: use `Option.ofNullable` for a value that may be `null`. `Option.when` calls its supplier
only when the condition holds.

## Pattern matching over the cases

`Some` and `None` are records, so a record pattern can take the value out and a `when` guard can add a condition.
Pattern matching with a `switch` expression needs no `default`.

```java
var age = Option.some(17); // Option<Integer>
var label = switch (age) {
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
var port = Option.some("8080")
    .filter(s -> s.chars().allMatch(Character::isDigit))
    .map(Integer::parseInt)
    .getOrElse(80); // Integer
var shown = Option.<Integer>none().fold(() -> "no value", n -> "n = " + n);
// 8080, "no value"
```

`mapTry` runs a function that may throw, and returns a `Try`. On a `None`, the result is a `Failure` of a
`NoSuchElementException`.

```java
var parsed = Option.some("x").mapTry(Integer::parseInt);       // Try<Integer>
var absent = Option.<String>none().mapTry(Integer::parseInt);  // Try<Integer>
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
var fromOptional = Option.ofOptional(java.util.Optional.of(3));           // Option<Integer>
var either       = fromOptional.toEither(() -> "missing");                // Either<String, Integer>
var validation   = Option.<Integer>none().toValidation(() -> "missing");  // Validation<String, Integer>
var back         = fromOptional.toOptional();                             // java.util.Optional<Integer>
// Some(3), Right(3), Invalid(missing), Optional[3]
```

## Sharp edges

### `map` to `null` throws

`Optional.map` turns a `null` result into an empty `Optional`. `Option.map` throws a `NullPointerException`
instead, because `Some` cannot hold `null`.

To map to a value that may be absent, use `flatMap` with `Option.ofNullable`:

```java
var env   = java.util.Map.of("HOME", "/home/ada");
var shell = Option.some("SHELL").flatMap(key -> Option.ofNullable(env.get(key))); // Option<String>
// None, where map(env::get) would throw
```

### `get()` on `None` throws

`get()` throws a `NoSuchElementException` on a `None`. Prefer pattern matching, `fold` or `getOrElse`.

### Not a collection

An `Option` is not `Iterable`, so it cannot go in a `for` loop. Convert it with `toVector()` or `stream()`.
