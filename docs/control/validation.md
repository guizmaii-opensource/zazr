---
description: Validation keeps every error in a NonEmptyVector - accumulation with zip, collectAll, forEach and partition.
---

# Validation

`Validation<E, A>` is `Valid(A value)` or `Invalid(NonEmptyVector<E> errors)`. `Either` stops at the first
failure; combining validations keeps the errors of every one of them, in order.

The errors are held in a [`NonEmptyVector`](../non-empty-vector.md), so an `Invalid` always carries at least one.

## When to use it

Use `Validation` for independent checks whose errors should all be reported at once: the fields of a form, the
entries of a configuration file.

- If each step needs the result of the previous one, the first error stops the work anyway: use
  [`Either`](either.md).
- If the error is an exception thrown by the code you call, use [`Try`](try.md), or `Validation.of` below.

## Checks

A check returns a `Validation`. `fromPredicate` builds one from a test, and from a function that turns the rejected
value into an error message.

```java
static Validation<String, String> name(String value) {
    return value.isBlank() ? Validation.invalid("name is blank") : Validation.valid(value);
}

static Validation<String, Integer> age(int value) {
    return Validation.fromPredicate(value, v -> v >= 0, v -> "age is negative");
}

static Validation<String, String> email(String value) {
    return Validation.fromPredicate(value, v -> v.contains("@"), v -> "email has no @");
}
```

`fromEither`, `fromOption` and `fromTry` convert the other control types. `Validation.of` runs code that may throw
and turns the exception into an error.

```java
var port = Validation.of(() -> Integer.parseInt("80a"),
    e -> "port is not a number"); // Validation<String, Integer>
// Invalid(port is not a number)
```

## Accumulation with `zip` and `zipWith`

`zipWith` combines independent checks and calls the function only when all of them are valid. The static form
takes 2 to 8 checks ([zip at arity N](../zip.md)); the instance form combines two.

```java
record User(String name, int age, String email) {}

var user = Validation.zipWith(name(""), age(-1), email("jules"), User::new); // Validation<String, User>
// Invalid(name is blank, age is negative, email has no @)
```

```java
var pair = name("Ada").zip(age(36)); // Validation<String, Tuple2<String, Integer>>
var both = name("").zipWith(age(-1), (n, a) -> n + a); // Validation<String, String>
// Valid((Ada, 36)), Invalid(name is blank, age is negative)
```

`zip(Either)` treats a `Left` as one more error: `Invalid(a)` zipped with `Left(b)` is `Invalid(a, b)`.

## Reading the result

Pattern match on the records, or `fold` with the errors first:

```java
var checked = age(-5); // Validation<String, Integer>
var message = switch (checked) {
    case Valid(var years) -> "age " + years;
    case Invalid(var errors) -> errors.size() + " error(s): " + errors.mkString("; ");
};
// "1 error(s): age is negative"
```

`toEither()` gives an `Either<NonEmptyVector<E>, A>`, for code that expects one.

## Many values: `collectAll`, `forEach`, `partition`

`collectAll` turns many validations into one validation of a `Vector`, keeping every error. `forEach` does the
same after running a check on each input.

```java
var ages = Validation.forEach(Vector.of(3, -1, 7, -2),
    n -> age(n)); // Validation<String, Vector<Integer>>
// Invalid(age is negative, age is negative)
```

```java
var names = Validation.collectAll(
    Vector.of(name("Ada"), name("Grace"))); // Validation<String, Vector<String>>
// Valid(Vector(Ada, Grace))
```

`forEach` over a `NonEmptyVector` returns a `NonEmptyVector` on the valid side too.

```java
var scores = Validation.forEach(NonEmptyVector.of(1, 2),
    n -> age(n)); // Validation<String, NonEmptyVector<Integer>>
// Valid(NonEmptyVector(1, 2))
```

`partition` never fails: it returns the errors and the successes side by side.

```java
var split = Validation.partition(Vector.of(4, -1, 9),
    n -> age(n)); // Tuple2<Vector<String>, Vector<Integer>>
// (Vector(age is negative), Vector(4, 9))
```

## Short-circuiting on purpose: `flatMap`

`flatMap` runs the next check only when this one is valid, so the two checks' errors are never combined. Use it
for a rule that needs the valid value, after the independent checks:

```java
var adult = Validation.zipWith(name("Ada"), age(15), email("ada@example.com"), User::new)
    .flatMapEither(u -> u.age() >= 18 ? Either.right(u) : Either.left(u.name() + " is under 18"));
// Invalid(Ada is under 18)
```

`flatMapEither` is the same for a step that returns an `Either`. If every step depends on the previous one, `Either`
is the simpler type.

## Transforming the errors

`mapError` transforms each error; `mapErrorAll` transforms the `NonEmptyVector` of errors as a whole, and the result
stays non-empty. `mapBoth` transforms the errors and the value at once.

## Conversions

- `toEither()` gives an `Either<NonEmptyVector<E>, A>`; `toEitherWith(Function)` builds the left value from the
  errors.
- `toTry(Function)` builds the `Failure` cause from the errors.
- `toOption()` and `toVector()` keep the value and drop the errors.

```java
var negative = age(-3); // Validation<String, Integer>
var joined = negative.toEitherWith(errors -> errors.mkString("; ")); // Either<String, Integer>
var failure = negative.toTry(
    errors -> new IllegalArgumentException(errors.mkString("; "))); // Try<Integer>
// Left(age is negative), Failure(java.lang.IllegalArgumentException: age is negative)
```

## Sharp edges

- `orElse` does not combine errors: when both sides are invalid, it returns the second one and drops the first
  one's errors.
- Equality depends on the order of the errors: `Invalid(a, b)` is not equal to `Invalid(b, a)`.
- There is no `flip`: the two sides are not alike, since one of them is never empty.
- Neither case holds `null`: `valid(null)` and `invalid(null)` throw a `NullPointerException`.
