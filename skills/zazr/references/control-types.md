# Control types

`Option`, `Either`, `Try`, `Validation` (in `com.guizmaii.zazr.control`) and `Lazy` (in `com.guizmaii.zazr`) hold
the result of a step. `Using` releases resources and returns a `Try`. Full pages: https://zazr.dev/control/.

## Which one

| The code has | Use | Cases |
|---|---|---|
| a value that may be missing, nothing to say why | `Option<A>` | `Some(A value)`, `None()` |
| a result or an error value; the first error stops the work | `Either<L, R>` | `Left(L value)`, `Right(R value)` |
| code that throws | `Try<A>` | `Success(A value)`, `Failure(Throwable cause)` |
| independent checks; every error must be reported | `Validation<E, A>` | `Valid(A value)`, `Invalid(NonEmptyVector<E> errors)` |
| a value costly to compute and maybe not needed | `Lazy<A>` | none: `get()` computes it once |
| resources to release, outcome wanted as a `Try` | `Using.of`, `Using.manager` | returns a `Try` |

Rules of thumb:

- Each step needs the previous result: `Either` with `flatMap`.
- The checks do not depend on each other (form fields, config entries): `Validation` with the static `zipWith`.
- A JDK or library call throws: `Try.of(...)`, then `toEither()` to continue with an error type of your own.
- Exceptions are still right for bugs (a broken invariant, a bad argument) and a failing environment (database
  down). `Either` and `Validation` are for failures that are part of the business.

## Reading a value: pattern matching

The four are sealed interfaces whose cases are records. A `switch` expression over them needs no `default`, and a
`when` guard adds a condition. Import the cases: `import com.guizmaii.zazr.control.Option.Some;` and so on.

```java
var age = Option.some(17); // Option<Integer>
var label = switch (age) {
    case Some(var years) when years >= 18 -> "adult";
    case Some(var years) -> "minor, " + years;
    case None() -> "unknown";
};
// "minor, 17"
```

`fold(ifFailure, ifSuccess)` does the same with two functions, failure side first. `getOrElse` gives a default.
Avoid `get()`: it throws `NoSuchElementException` on `None`, `Left` and `Invalid`, and rethrows the cause on a
`Failure`.

None of them is `Iterable`: no `for (x : option)`. Convert with `toVector()` (or `stream()` on `Option`).

## Members they share

Same names and argument order on the four types, failure side first:

| Member | `Option` | `Either` | `Try` | `Validation` |
|---|---|---|---|---|
| `map`, `flatMap` | on `Some` | on `Right` | on `Success` | on `Valid` |
| `fold(ifFailure, ifSuccess)` | `Supplier`, `Function` | `Function<L>`, `Function<R>` | `Function<Throwable>`, `Function<A>` | `Function<NonEmptyVector<E>>`, `Function<A>` |
| `getOrElse` | value, `Supplier` | also `Function<L, R>` | also `Function<Throwable, A>` | also `Function<NonEmptyVector<E>, A>` |
| `getOrElseThrow`, `getOrNull` | yes | yes | yes | yes |
| `contains`, `exists`, `forAll`, `forEach` | yes | right side | success | valid value |
| `tap` | `tap`, `tapNone` | `tap`, `tapLeft` | `tap`, `tapError` | `tap`, `tapError` |
| `orElse` | value, `Supplier` | value, `Supplier` | value, `Supplier` | `Supplier` |
| `zip`, `zipWith`, `zipLeft`, `zipRight` | yes | yes | yes | yes, keeping every error |
| static `collectAll`, `forEach`, `flatten` | yes | yes | yes | yes, keeping every error |

The static `collectAll` turns a collection of values into one value holding a `Vector`; the static `forEach` applies
a function first. `Option`, `Either` and `Try` stop at the first failure; `Validation` gathers every error.

```java
var parsed = Either.forEach(Vector.of("1", "x", "3"), // Either<String, Vector<Integer>>
    s -> s.chars().allMatch(Character::isDigit) ? Either.right(Integer.parseInt(s)) : Either.left("bad: " + s));
var all = Option.collectAll(Vector.of(Option.some(1), Option.some(2))); // Option<Vector<Integer>>
// Left(bad: x), Some(Vector(1, 2))
```

## `Option`

- Build: `Option.some(v)`, `Option.none()`, `Option.ofNullable(v)`, `Option.when(condition, supplier)`,
  `Option.ofOptional(optional)`. There is no `Option.of`.
- `filter`, `map`, `flatMap`, `getOrElse`, `isDefined()`, `isEmpty()`, `orElse`, `mapTry` (a function that throws,
  returning a `Try`).
- Convert: `toEither(Supplier)`, `toTry(Supplier)`, `toValidation(Supplier)`, `toVector()`, `toList()`,
  `toOptional()`, `stream()`.
- `map` to `null` throws a `NullPointerException`, where `Optional.map` gives empty. Map to something that may be
  absent with `flatMap` and `Option.ofNullable`:

```java
var env = java.util.Map.of("HOME", "/home/ada");
var shell = Option.some("SHELL").flatMap(key -> Option.ofNullable(env.get(key))); // Option<String>
// None, where map(env::get) would throw
```

## `Either`

- Right-biased: `map`, `flatMap` and most members work on `Right` and pass a `Left` through.
- Build: `Either.right(v)`, `Either.left(e)`, `Either.fromPredicate(value, test, ifFalse)`.
- `mapLeft` transforms the error, `mapBoth` both sides, `flip` swaps them, `filterOrElse(test, value -> error)`
  rejects a `Right` (there is no `filter`).
- Convert: `toOption()`, `toTry(Function<L, Throwable>)` (pass `t -> t` when the left side is already a
  `Throwable`), `toValidation()`, `toVector()`.

```java
var total = Either.<String, Integer>right(2) // Either<String, Integer>
    .flatMap(n -> n > 0 ? Either.right(n * 10) : Either.left("not positive"))
    .filterOrElse(n -> n < 100, n -> n + " is too large")
    .mapLeft(error -> "rejected: " + error);
// Right(20)
```

## `Try`

- `Try.of(callable)` takes a `Callable`, so checked exceptions need no wrapping. `Try.run(action)` has the
  success value `Tuple0`. `Try.success`, `Try.failure` build a case.
- `map`, `flatMap` and `filter(test, value -> exception)` run under `Try`: an exception thrown by the function
  becomes the `Failure`.
- Recover: `catchAll(e -> value)`, `catchSome(ExceptionType.class, e -> value)`, and `catchAllWith`,
  `catchSomeWith` returning another `Try`. `mapError` wraps the cause. `ensuring(action)` is a `finally`.
- Convert: `toEither()` (an `Either<Throwable, A>`), `toValidation()`, `toOption()`, `toVector()`,
  `toCompletableFuture()`; `Try.fromCompletableFuture(future)` waits and captures.
- Fatal errors are rethrown, never captured: `InterruptedException`, `LinkageError`, `ThreadDeath`,
  `VirtualMachineError`.
- A computation that returns `null` gives a `Failure` of a `NullPointerException`, a `CompletableFuture<Void>`
  included.
- Two `Failure`s are equal only when they hold the same exception object. Compare the cause's class or message.

```java
var port = Try.of(() -> Integer.parseInt("80a")) // Try<Integer>
    .catchSome(NumberFormatException.class, e -> 8080)
    .map(p -> p + 1);
var config = Try.<String>failure(new java.io.IOException("disk")) // Try<String>
    .mapError(e -> new IllegalStateException("cannot read the configuration", e));
// Success(8081), Failure(java.lang.IllegalStateException: cannot read the configuration)
```

## `Validation`

- A check returns a `Validation`: `Validation.valid(v)`, `Validation.invalid(error)`,
  `Validation.fromPredicate(value, test, value -> error)`, `Validation.of(callable, exception -> error)`,
  `fromEither`, `fromOption(option, () -> error)`, `fromTry`.
- Combine independent checks with the static `zipWith` (2 to 8 checks and a constructor or function): every error
  is kept, in argument order. The instance `zip` and `zipWith` combine two.
- `flatMap` and `flatMapEither` short-circuit: use them only for a rule that needs the valid value, after the
  independent checks.
- Many inputs: static `collectAll`, `forEach(values, check)`, and `partition(values, check)` which returns the
  errors and the successes side by side. `forEach` over a `NonEmptyVector` returns a `NonEmptyVector`.
- `mapError` maps each error, `mapErrorAll` the whole `NonEmptyVector`.
- Convert: `toEither()` (an `Either<NonEmptyVector<E>, A>`), `toEitherWith(errors -> left)`,
  `toTry(errors -> exception)`, `toOption()`, `toVector()`.
- Sharp edges: `orElse` keeps only the second side's errors; equality depends on the order of the errors; there is
  no `flip`.

```java
record User(String name, int age) {}

static Validation<String, String> name(String value) {
    return value.isBlank() ? Validation.invalid("name is blank") : Validation.valid(value);
}

static Validation<String, Integer> age(int value) {
    return Validation.fromPredicate(value, v -> v >= 0, v -> "age is negative");
}
```

```java
var user = Validation.zipWith(name(""), age(-1), User::new); // Validation<String, User>
var adult = Validation.zipWith(name("Ada"), age(15), User::new) // Validation<String, User>
    .flatMapEither(u -> u.age() >= 18 ? Either.right(u) : Either.left(u.name() + " is under 18"));
var ages = Validation.forEach(Vector.of(3, -1, 7), n -> age(n)); // Validation<String, Vector<Integer>>
// Invalid(name is blank, age is negative), Invalid(Ada is under 18), Invalid(age is negative)
```

## `Lazy`

- `Lazy.of(supplier)` computes nothing; `get()` computes the value on first call and caches it; `isEvaluated()`
  tells whether it has been computed.
- `map`, `flatMap`, `zipWith` and the static `collectAll` and `flatten` return a new `Lazy` that computes nothing
  yet. `toSupplier()` shares the cache.
- `Lazy` may hold `null`; wrap it with `Option.ofNullable(lazy.get())`.
- A computation that throws is not cached: the next `get()` runs it again. `equals`, `hashCode` and `tap` compute
  the value; `toString` does not.

## `Using`

Use it instead of nested `try`-with-resources when the outcome should be a `Try`, when the number of resources is
known only at run time, or when the thing to release is not an `AutoCloseable` (a lock).

```java
var sources = Vector.of("alpha", "beta");
var length = Using.manager(use -> { // Try<Integer>
    var total = 0;
    for (var source : sources) {
        var reader = use.acquire(new BufferedReader(new StringReader(source)));
        total += reader.readLine().length();
    }
    return total;
});
// Success(9), and both readers are closed
```

- `Using.of(() -> open, resource -> use)` for one resource.
- `use.acquire(value, release)` registers any value with the code that releases it; a lambda is an `AutoCloseable`.
- Resources are released in reverse order, every release runs, and a later exception is added as suppressed. A more
  severe throwable (`VirtualMachineError`, then `LinkageError`, then `InterruptedException`) surfaces instead.
- The manager works only inside the function; do not keep it, and do not share it between threads.

## Null policy

`Some`, `Left`, `Right`, `Success` and `Valid` never hold `null`: creating one with `null` throws a
`NullPointerException`. `Option.ofNullable` goes in, `getOrNull()` goes out.
