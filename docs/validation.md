---
description: Validation keeps every error in a NonEmptyVector - accumulation with zip, collectAll, forEach and partition.
---

# Validation

`Validation<E, A>` is `Valid(A value)` or `Invalid(NonEmptyVector<E> errors)`. Where `Either` stops at the first
failure, combining validations keeps the errors of every side, in argument order. The error side is a
[`NonEmptyVector`](non-empty-vector.md), so an `Invalid` always carries at least one error and the type says so.

## Checks

A check returns a `Validation`. `fromPredicate` builds one from a test and a function of the rejected value, so the
error can name it.

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

Other ways in: `Validation.valid`, `invalid`, `invalidAll(NonEmptyVector)`, `fromEither`, `fromOption`, `fromTry`, and
`of(Callable, Function<Throwable, E>)`, which is `Try.of` followed by a conversion.

## Accumulation with `zip` and `zipWith`

`zipWith` combines independent checks and calls the function only when all of them are valid. The static forms go
from 2 to 8 arguments ([zip at arity N](zip.md)); the instance forms combine two.

```java
record User(String name, int age, String email) {}

Validation<String, User> user = Validation.zipWith(name(""), age(-1), email("jules"), User::new);
// Invalid(name is blank, age is negative, email has no @)
```

```java
Validation<String, Tuple2<String, Integer>> pair = name("Ada").zip(age(36));
Validation<String, String> both = name("").zipWith(age(-1), (n, a) -> n + a);
// Valid((Ada, 36)), Invalid(name is blank, age is negative)
```

`zip(Either)` treats a `Left` as one more error: `Invalid(a)` zipped with `Left(b)` is `Invalid(a, b)`.

## Reading the result

`switch` over the records, or `fold` with the errors first:

```java
Validation<String, Integer> checked = age(-5);
String message = switch (checked) {
    case Valid(var years) -> "age " + years;
    case Invalid(var errors) -> errors.size() + " error(s): " + errors.mkString("; ");
};
// "1 error(s): age is negative"
```

`getOrElse`, `getOrElseThrow`, `tapError`, `mapError`, `mapErrorAll` and the conversions `toEither()`
(`Either<NonEmptyVector<E>, A>`), `toEitherWith`, `toOption()` and `toTry(Function)` cover the rest.

## Many values: `collectAll`, `forEach`, `partition`

`collectAll` turns many validations into one validation of a `Vector`, accumulating every error. `forEach` maps each
input to a validation first. Both loop once.

```java
Validation<String, Vector<Integer>> ages = Validation.forEach(Vector.of(3, -1, 7, -2), n -> age(n));
// Invalid(age is negative, age is negative)
```

```java
Validation<String, Vector<String>> names = Validation.collectAll(Vector.of(name("Ada"), name("Grace")));
// Valid(Vector(Ada, Grace))
```

`forEach` over a `NonEmptyVector` keeps the result non-empty: its valid side is a `NonEmptyVector` too.

```java
Validation<String, NonEmptyVector<Integer>> scores = Validation.forEach(NonEmptyVector.of(1, 2), n -> age(n));
// Valid(NonEmptyVector(1, 2))
```

`partition` never fails: it returns the errors and the successes side by side.

```java
Tuple2<Vector<String>, Vector<Integer>> split = Validation.partition(Vector.of(4, -1, 9), n -> age(n));
// (Vector(age is negative), Vector(4, 9))
```

## Short-circuiting on purpose: `flatMap`

`flatMap` runs the next check only when this one is valid, so its errors are never accumulated with this one's. That
is the right tool for a rule that needs the valid value, after the independent checks:

```java
Validation<String, User> adult = Validation.zipWith(name("Ada"), age(15), email("ada@example.com"), User::new)
    .flatMapEither(u -> u.age() >= 18 ? Either.right(u) : Either.left(u.name() + " is under 18"));
// Invalid(Ada is under 18)
```

`flatMapEither` is the same for a step that returns an `Either`. If every step depends on the previous one, `Either`
is the simpler type. `orElse` keeps the second validation and drops the first one's errors.

There is no `flip`: the two sides are not symmetric, since one is non-empty.
