---
description: Learn why Zazr is useful and how it makes your code better.
---

# New to functional programming?

Learn why Zazr is useful and how it makes your code better.

This page is for Java developers who have never written functional code. It needs no theory: each idea is a habit
you can adopt one method at a time, and each one moves a mistake from production to the compiler.

## `Option` instead of `null`

### The problem with `null`

A method that returns `null` looks exactly like one that never does. Nothing in its signature tells you to check.

```java
java.util.Map<String, Customer> customers =
    java.util.Map.of("c-1", new Customer("c-1", "Ada", "ada@example.com"));
Customer customer = customers.get("c-2");       // null: nothing in the type says it can be
String greeting = "Hello " + customer.name();   // NullPointerException
```

Here the crash is one line away from its cause. In real code the `null` travels through fields and method calls, and
the `NullPointerException` appears far from the place that produced it.

### The absence is in the type

An `Option<Customer>` is either `Some(customer)` or `None()`. The possible absence is part of the type, so the
compiler makes you deal with it: `customer.name()` does not compile on an `Option<Customer>`.

Zazr's `HashMap.get` returns an `Option`:

```java
HashMap<String, Customer> customers =
    HashMap.of("c-1", new Customer("c-1", "Ada", "ada@example.com"));
Option<Customer> customer = customers.get("c-2");
String greeting = customer.map(c -> "Hello " + c.name()).getOrElse("Hello, guest");
// "Hello, guest"
```

### Chaining operations

`map` transforms the value when there is one. `flatMap` chains a step that may itself find nothing. A `None` goes
through the whole chain untouched, with no `if` in sight.

```java
Option<String> domain = customers.get("c-1")
    .map(Customer::email)
    .flatMap(email -> Option.when(email.contains("@"), () -> email.split("@")[1]));
// Some(example.com)
```

### `switch` over `Some` and `None`

`Option` is a sealed interface of two records, so a `switch` over it covers both cases, and the compiler checks that
it does.

```java
String message = switch (customers.get("c-1")) {
    case Some(var c) -> "Welcome back, " + c.name();
    case None() -> "Please sign up";
};
// "Welcome back, Ada"
```

### What about `java.util.Optional`?

`Optional` has the same idea, and `map`, `flatMap` and `orElse` too. But it is a final class, so you cannot `switch`
over its cases. It also cannot be combined with other values (`zip`, `zipWith`) or turned into an `Either` or a
`Validation`.

`Option` is also used across the whole library: `HashMap.get`, `find`, `headOption` and the `...Option` methods of
the collections all return one. `toOptional()` and `Option.ofOptional` convert at the boundary with code that uses
`Optional`.

## `Either` instead of throwing

### The problem with exceptions

An unchecked exception is invisible in the signature. The caller learns about it from the javadoc, if there is one,
or from a stack trace in production.

```java
// throws two kinds of exception, and only this comment says so
static int parseQuantityOrThrow(String input) {
    int quantity = Integer.parseInt(input.trim());
    if (quantity <= 0) {
        throw new IllegalArgumentException("quantity must be positive");
    }
    return quantity;
}
```

A `throw` is also a hidden jump: it leaves the method, and every caller up the stack, until some `catch` somewhere
stops it.

### The error is in the signature

An `Either<String, Integer>` is a `Left` holding an error or a `Right` holding the result. The caller sees the error
in the type and cannot forget it.

```java
static Either<String, Integer> parseQuantity(String input) {
    return Try.of(() -> Integer.parseInt(input.trim()))
        .toEither()
        .mapLeft(e -> "not a number: " + input)
        .flatMap(q -> q > 0 ? Either.right(q) : Either.left("quantity must be positive"));
}
```

Reading the result is a `switch`, like for `Option`:

```java
String reply = switch (parseQuantity("0")) {
    case Right(var quantity) -> "added " + quantity + " to the cart";
    case Left(var error) -> error;
};
// "quantity must be positive"
```

### Errors compose

`map` and `flatMap` work on the `Right` side. A `Left` passes through the rest of the chain unchanged, so you write
the happy path and the error arrives at the end on its own.

```java
Either<String, Integer> totalInCents = parseQuantity("3").map(q -> q * 1_250);
Either<String, Integer> rejected = parseQuantity("three").map(q -> q * 1_250);
// Right(3750), Left(not a number: three)
```

### When an exception is still right

Exceptions are still the right tool for a failure that nobody can handle where it happens:

- A bug: an index out of range, a broken invariant, an argument that the caller should never have passed.
- The environment failing: the database is down, the disk is full. Let it reach the layer that retries or reports it.

`Either` is for the failures that are part of the business: a bad quantity, an unknown customer, a declined payment.

### Where `Try` fits

`Try` wraps code that throws, typically a JDK or third-party call, and turns the exception into a value. `toEither()`
then gives an `Either` for the rest of your code, as `parseQuantity` does above.

```java
Try<LocalDate> deliveryDate = Try.of(() -> LocalDate.parse("2026-02-30"));
// Failure(java.time.format.DateTimeParseException: ...)
```

## `Validation`: report every error at once

### A sign-up form

A sign-up form has a name, an email, an age and a password. Each field has its own check, and each check returns a
`Validation`: `Valid(value)` or `Invalid(errors)`.

```java
record Person(String name, String email, int age, String password) {}

static Validation<String, String> checkName(String name) {
    return Validation.fromPredicate(name.trim(), n -> !n.isEmpty(), n -> "name is required");
}

static Validation<String, String> checkEmail(String email) {
    return Validation.fromPredicate(email.trim(), e -> e.contains("@"), e -> "email has no @");
}

static Validation<String, Integer> checkAge(int age) {
    return Validation.fromPredicate(age, a -> a >= 18, a -> "you must be 18 or older");
}

static Validation<String, String> checkPassword(String password) {
    return Validation.fromPredicate(password, p -> p.length() >= 12, p -> "password is too short");
}
```

### Every error, not only the first

`Validation.zipWith` runs every check, and builds the `Person` only when all of them pass. Otherwise it returns all
the errors, in the order of the fields.

```java
Validation<String, Person> person = Validation.zipWith(
    checkName(""), checkEmail("ada.example.com"), checkAge(16), checkPassword("hunter2"),
    Person::new);

String response = switch (person) {
    case Valid(var p) -> "Welcome, " + p.name();
    case Invalid(var errors) -> "Fix: " + errors.mkString("; ");
};
// "Fix: name is required; email has no @; you must be 18 or older; password is too short"
```

### Why not `Either`?

`Either` stops at the first error. With it, the user would fix the name, submit again, then learn about the email,
then the age. `Validation` shows the four problems on the first submit.

Use `Either` when each step needs the result of the previous one, and `Validation` when the checks are independent.

## Parse, don't validate

### A check that returns `boolean` forgets

Alexis King's article [Parse, don't validate](https://lexi-lambda.github.io/blog/2019/11/05/parse-don-t-validate/)
names a common trap. A method that returns `boolean` checks the data, then throws that knowledge away:

```java
static boolean isValidEmail(String input) {
    return input.contains("@");
}
```

After the check you still hold a `String`. A method that takes a `String email` cannot know whether anyone checked it,
so it checks again, or trusts and hopes.

### A parser returns a more precise type

A parser checks the data and returns a type that can only hold good data. Here the constructor is private, so
`parse` is the only way to get an `Email`:

```java
final class Email {
    private final String value;

    private Email(String value) {
        this.value = value;
    }

    static Validation<String, Email> parse(String input) {
        String trimmed = input.trim();
        return trimmed.contains("@")
            ? Validation.valid(new Email(trimmed))
            : Validation.invalid("email has no @");
    }

    String value() {
        return value;
    }
}
```

A method that takes an `Email` cannot receive a bad address. The check happens once, at the edge of your program,
where the input arrives.

### `NonEmptyVector` instead of "is it empty?"

The same idea works for collections. Instead of a `List` plus a check that it is not empty, parse it into a
`NonEmptyVector`. From then on `head()`, `max` and `reduce` cannot fail.

```java
static Either<String, NonEmptyVector<String>> recipients(Vector<String> input) {
    return input.toNonEmptyVector().toEither(() -> "at least one recipient is required");
}
```

```java
Either<String, NonEmptyVector<String>> none = recipients(Vector.empty());
Either<String, NonEmptyVector<String>> some = recipients(Vector.of("ada@shop.com", "bob@shop.com"));
String first = some.map(NonEmptyVector::head).getOrElse("nobody");
// none is Left(at least one recipient is required)
// some is Right(NonEmptyVector(ada@shop.com, bob@shop.com))
// first is "ada@shop.com"
```

## Make invalid states impossible to represent

### Fields that can disagree

Yaron Minsky puts it as "make illegal states unrepresentable", in
[Effective ML Revisited](https://blog.janestreet.com/effective-ml-revisited/). Here is a connection state written
the usual way:

```java
class ConnectionState {
    boolean connected;
    String sessionId;   // set when connected, hopefully
    String error;       // set when it failed, hopefully
}
```

Nothing stops `connected == true` with an `error`, or `connected == false` with neither a session nor an error. Is
that "connecting" or a bug? Every reader has to guess, and every method has to check.

### One record per state

With a sealed interface and one record per state, each state holds exactly the data it has:

```java
sealed interface Connection {}
record Connecting() implements Connection {}
record Connected(String sessionId) implements Connection {}
record Failed(String error) implements Connection {}
```

A `Connected` always has a session, a `Failed` always has an error, and a `Connecting` has neither. The wrong
combinations cannot be written.

### The compiler finds the missing case

A `switch` over a sealed interface must cover every case, and needs no `default`:

```java
static String describe(Connection connection) {
    return switch (connection) {
        case Connecting() -> "connecting...";
        case Connected(var sessionId) -> "connected, session " + sessionId;
        case Failed(var error) -> "failed: " + error;
    };
}
```

Add a `Disconnected` state later, and every `switch` that does not handle it stops compiling:

```text
error: the switch expression does not cover all possible input values
        return switch (connection) {
               ^
```

`Option`, `Either`, `Try` and `Validation` are built the same way, which is why a `switch` over them works.

## Where to go next

- [Getting started](getting-started.md): add Zazr to your build.
- [Control types](control-types.md): `Option`, `Either` and `Try` in detail.
- [Validation](validation.md): every way to combine checks.
- [NonEmptyVector](non-empty-vector.md): the collection that cannot be empty.
- [zip at arity N](zip.md): combine up to eight values in one call.
- [Collections](collections/index.md): which collection to choose.
