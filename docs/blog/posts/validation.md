---
date: 2026-10-01
authors:
  - guizmaii
categories:
  - Error handling
slug: validation
description: "Every error at once with Validation: checks that name the bad value, whole forms and whole lists checked in one go, and the step that needs a valid value first."
---

# Every error at once with Validation

When users fill in a form, they want to hear about every mistake at once, not one per attempt. Zazr's `Validation`
does that: it checks input and gives back every error it found, not only the first.

This post shows how to write a check, combine checks into a whole form, check a whole list, read the result, and
chain the step that needs a valid value first.

<!-- more -->

## Validation, simply

`Validation` is a way to check input and get back every error at once.

You know the usual ways to report a problem in Java. An exception stops at the first problem: the code that throws
it never reaches the next check. `Either` stops at the first error too: it is made for steps where each one needs
the result of the one before.

That is right when the steps depend on each other. It is wrong for a form. The username, the email address and the
age don't depend on each other, so all three can be checked, and the user can fix all three at once.

A `Validation<E, A>` is one of two things:

- `Valid(value)`: the input passed, and here is the value, of type `A`;
- `Invalid(errors)`: the input failed, and here are the errors, of type `E`.

The errors are held in a `NonEmptyVector`: a vector that always has at least one element. An `Invalid` with no error
can't exist, and the type says so.

## A sign-up form

Our example is a sign-up form with three fields. The username needs at least 3 characters, the email address needs
an `@`, and the user must be 13 or older. Here are the form and one check per field:

```java
record SignUp(String username, String email, int age) {}

static Validation<String, String> username(String value) {
    return Validation.fromPredicate(value, v -> v.length() >= 3, v -> "username '" + v + "' is too short");
}

static Validation<String, String> email(String value) {
    return Validation.fromPredicate(value, v -> v.contains("@"), v -> "email '" + v + "' has no @");
}

static Validation<String, Integer> age(int value) {
    return Validation.fromPredicate(value, v -> v >= 13, v -> "age " + v + " is under 13");
}
```

`fromPredicate` takes a value, a test, and a function that builds the error. When the test passes, the result is
`Valid` with the value. When it fails, the function receives the rejected value, so the error can name it:

```java
var ok  = email("ana@example.com");  // Validation<String, String>
var bad = email("ana");              // Validation<String, String>
// Valid(ana@example.com), Invalid(email 'ana' has no @)
```

Here the errors are strings, which is enough for a form. They can be any type: an enum, or a record with the field
name and a message.

## Every error at once

Now the whole form. `Validation.zipWith` runs every check, then builds the `SignUp` only if all of them passed:

```java
var signUp = Validation.zipWith(username("jo"), email("jules"), age(9), SignUp::new);
// Invalid(username 'jo' is too short, email 'jules' has no @, age 9 is under 13)

var welcome = Validation.zipWith(username("ana"), email("ana@example.com"), age(30), SignUp::new);
// Valid(SignUp[username=ana, email=ana@example.com, age=30])
```

The first form has three mistakes, and the result has three errors, in the order of the checks. The second form is
correct, so the result holds the new `SignUp`. `SignUp::new` is called only in that case.

`zipWith` takes from 2 to 8 checks and a function that receives their values. The same `zip` and `zipWith` exist on
`Option`, `Either`, `Try` and `Lazy`; the [zip](../../zip.md) page shows them all.

## Reading the result

`Valid` and `Invalid` are records, so a `switch` takes the result apart, and the compiler checks that both cases are
handled:

```java
var message = switch (signUp) {
    case Valid(var user) -> "welcome, " + user.username();
    case Invalid(var errors) -> "please fix: " + errors.mkString("; ");
};
// "please fix: username 'jo' is too short; email 'jules' has no @; age 9 is under 13"
```

`errors` is the `NonEmptyVector` of errors. Since it can't be empty, `errors.head()` always returns the first one,
with no check needed.

## A whole list

Now say users can invite friends by pasting a list of email addresses. Each address goes through the same check, and
we want every bad one reported.

`Validation.forEach` runs a check on each element of a list, and keeps every error:

```java
var invites = Vector.of("ana@example.com", "bob", "cleo@example.com", "dan");
var all     = Validation.forEach(invites, a -> email(a)); // Validation<String, Vector<String>>
// Invalid(email 'bob' has no @, email 'dan' has no @)
```

When every address is valid, the result is `Valid` of the whole `Vector` of addresses, in their order. When some
aren't, it is `Invalid` with the errors of all of them.

When you already have a list of results, `Validation.collectAll` turns it into one result, the same way. `forEach`
is the two steps in one call:

```java
var checks = invites.map(a -> email(a));     // Vector<Validation<String, String>>
var same   = Validation.collectAll(checks);  // Validation<String, Vector<String>>
// Invalid(email 'bob' has no @, email 'dan' has no @)
```

Sometimes one bad address should not block the others: send the good invitations, and report the bad ones.
`Validation.partition` does that. It never fails, and returns the errors and the valid values side by side:

```java
var split = Validation.partition(invites, a -> email(a)); // Tuple2<Vector<String>, Vector<String>>
// (Vector(email 'bob' has no @, email 'dan' has no @), Vector(ana@example.com, cleo@example.com))
```

## A step that needs a valid value

Some checks can't run until others have passed. Before we create the account, we must make sure the username isn't
taken. That lookup needs a valid form: there is no point in looking up a username that is too short.

`flatMapEither` chains such a step. It runs only when the result so far is `Valid`, and receives its value. The step
returns an `Either`, and a `Left` becomes the error:

```java
var taken   = HashSet.of("ana", "bob"); // HashSet<String>
var account = Validation.zipWith(username("ana"), email("ana@example.com"), age(30), SignUp::new)
    .flatMapEither(s -> taken.contains(s.username())
        ? Either.left("username '" + s.username() + "' is taken")
        : Either.right(s));
// Invalid(username 'ana' is taken)
```

When the form is invalid, the lookup doesn't run, and the result keeps the form's errors. With the form of `jo`, the
result is the same three errors as before.

`flatMap` is the same, for a step that returns a `Validation`. Both stop at the first error, as `Either` does, since
the next step has no value to work on.

So the rule is short:

- **Combine** with `zipWith`, `forEach` or `collectAll` the checks that don't need each other. You get every error.
- **Chain** with `flatMap` or `flatMapEither` a step that needs the value of the one before. It runs only on a valid
  value.

A form is usually both: its fields combined, then one or two chained steps that need the whole form.

## With Option and Either

The rest of your code doesn't have to know about `Validation`.

A field that may be missing is often an `Option`, like the value of a key in a map. `Validation.fromOption` turns
it into a check, with the error to report when it is empty:

```java
var form     = HashMap.of("username", "ana", "email", "ana@example.com"); // HashMap<String, String>
var birthday = Validation.fromOption(form.get("birthday"), () -> "birthday is missing");
// Invalid(birthday is missing)
```

`Validation.fromEither` does the same for an `Either`. The other way, `toEither()` gives an `Either` with the
`NonEmptyVector` of errors on the left, and `toEitherWith` builds the left value from the errors, for code that
expects one message:

```java
var result = signUp.toEitherWith(errors -> errors.mkString("; ")); // Either<String, SignUp>
// Left(username 'jo' is too short; email 'jules' has no @; age 9 is under 13)
```

## Thank you, zio-prelude

Zazr's `Validation` follows the `Validation` of [zio-prelude](https://zio.dev/zio-prelude/), a library of the ZIO
ecosystem: every error kept in a collection that is never empty, independent checks combined so that none of their
errors is lost, and `flatMap` for the step that needs the previous value. We ported those ideas to Java.
Thank you to the zio-prelude contributors, who built it and keep making it better.

## Going further

The [Validation](../../control/validation.md) page covers what this post leaves out: `Validation.of` for code that
throws, transforming the errors with `mapError`, the other conversions, and the sharp edges.

Zazr 0.1.0 is on Maven Central, and the [Getting started](../../getting-started.md) page has the dependency.

Feedback is very welcome. If something is missing, unclear or broken, please
[open an issue](https://github.com/guizmaii-opensource/zazr/issues).

Thank you for reading.
