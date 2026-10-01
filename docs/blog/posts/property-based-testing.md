---
date: 2026-10-01
authors:
  - guizmaii
categories:
  - Testing
slug: property-based-testing
description: "Property-based testing in your JUnit tests with zazr-test: generators, checks, assertions that explain failures, seeds that replay them, and laws for your own types."
---

# Property-based testing with zazr-test

Zazr comes with a second artifact, `zazr-test`, for property-based testing. You state a rule that must hold for every
input, and it tries the rule on hundreds of generated inputs. It runs inside the tests you already have, JUnit or
any other framework.

This post shows what property-based testing is, how to write a check and read its failure, how to generate values
and explain failures, how to replay a failure, and how to check the rules of your own types.

<!-- more -->

## Property-based testing, simply

Before anything else, let's make property-based testing less mysterious. It is often explained in abstract terms: you
don't test examples, you check that "properties of the system hold". That can make it sound like a whole new
discipline.

It's simpler than that. A property-based test is a unit test where you don't have to invent the inputs: they are
generated for you. And because they are generated, the test doesn't run on one input but on hundreds of variants,
including the ones you would never have thought of. That's how it finds the edge cases your code doesn't handle yet.

## From picked inputs to generated ones

A unit test checks the inputs you picked: reverse `[1, 2, 3]` and expect `[3, 2, 1]`. It is precise, and it is
limited by your imagination. The bug is often in the case you didn't write: the empty list, the negative
number, the value next to the limit.

A property-based test is the same unit test, with a generated input. Since you don't know the input in advance, you
don't write the exact result you expect; you write what is always true, such as "reversing a list twice gives the
list back". A generator produces the inputs: short lists and long ones, empty ones, lists with duplicates, large and
negative numbers. The test runs on each of them, and stops at the first one that fails.

The two work well together. Tests with picked inputs document the cases that matter to you; properties explore the
ones you didn't list.

## Your first check

Here is that rule, in an ordinary JUnit 5 test class:

```java
class ReverseTest {

    @Test
    void reversingTwiceGivesTheListBack() {
        var lists = Gen.list(Gen.integers()); // Gen<List<Integer>>
        Check.check(lists, list -> list.reverse().reverse().equals(list));
    }
}
```

`Gen.list(Gen.integers())` is a generator of lists of integers. `Check.check` takes the generator and the property, a
lambda that receives one generated value. Here the property returns a `boolean`: `true` when the rule holds.

The check runs 200 samples. When every sample passes, it returns, and the test is green.

`zazr-test` is not a test runner and not a JUnit engine. `Check.check` is a method call inside a test method, so
`mvn test` or `gradle test` runs it like any other test, with no plugin and no extension to register. It depends on
no test framework, only on `zazr-core`. JUnit 4, TestNG and Spock work the same way.

### When a property breaks

Let's test something that has a bug. `midpoint` should return a value between its two arguments:

```java
class MidpointTest {

    static int midpoint(int a, int b) {
        return (a + b) / 2;
    }

    @Test
    void theMidpointIsBetweenItsArguments() {
        var naturals = Gen.integers(0, Integer.MAX_VALUE); // Gen<Integer>
        Check.check(naturals, naturals, (a, b) -> {
            var mid = midpoint(a, b);
            return mid >= Math.min(a, b) && mid <= Math.max(a, b);
        });
    }
}
```

With two generators, the property takes two values. When a sample breaks the property, `Check.check` throws an
`AssertionError`, and your test framework reports it as a normal test failure. Run with the seed 42, Maven prints:

```text
[ERROR] Failures:
[ERROR]   MidpointTest.theMidpointIsBetweenItsArguments:14 falsified at sample 2 by (2147483647, 513683364) (seed 42, replay with -Dzazr.check.seed=42)
```

The message has four parts:

- the counterexample, `(2147483647, 513683364)`: the values that broke the property, one per generator;
- the sample number, 2: the second sample failed;
- the seed of the run, 42;
- how to replay it: `-Dzazr.check.seed=42`.

The bug is an overflow: `a + b` is larger than `Integer.MAX_VALUE`, so the sum wraps around to a negative number.
Writing `a + (b - a) / 2` fixes it. The check found it at the second sample because a range generator often draws
its edges, here `Integer.MAX_VALUE`.

## Generators

A `Gen<A>` produces values of type `A`. The scalar generators have plural names, since each one produces many values:
`integers()`, `longs()`, `doubles()`, `booleans()`, `chars()`, `strings()`, `alphaNumericStrings()`,
`localDateTimes()`, most of them with a range.

You build the generators of your own types from these. `map`, `zipWith` and `flatMap` combine them, and `Gen` has
generators for the Zazr types, from `option` and `either` to `vector`, `hashMap` and `treeMap`. You pass them the
generators of the elements.

```java
record User(String name, int age) {}

var names   = Gen.alphaNumericStrings();       // Gen<String>
var ages    = Gen.integers(0, 120);            // Gen<Integer>
var users   = names.zipWith(ages, User::new);  // Gen<User>
var teams   = Gen.vector(users);               // Gen<Vector<User>>
var lookups = Gen.option(users);               // Gen<Option<User>>
var byName  = Gen.hashMap(names, users);       // Gen<HashMap<String, User>>
Check.check(teams, team -> team.sortBy(User::age).map(User::age).equals(team.map(User::age).sorted()));
```

The property says that sorting a team by age puts the ages in sorted order.

### Small values first

A check grows the size of its values over the run. The size starts at 0 for the first sample and reaches 100 for the
last one, and a collection or a string is at most as long as the size. So the first samples are empty or short, and
larger values come later.

Generated values also favour the cases where bugs hide. Half of the draws of a range are its edges: the bounds, their
neighbours, -1, 0 and 1. Half of the lengths of a collection are 0, 1, the size or the size minus one.

### Finite generators

Some inputs are few enough to try them all. `Gen.fromIterable` gives its values in order, then stops, and
`Check.checkAll` checks each value of finite generators exactly once:

```java
var days = Gen.fromIterable(EnumSet.allOf(DayOfWeek.class)); // Gen<DayOfWeek>
Check.checkAll(days, day -> DayOfWeek.of(day.getValue()) == day);
```

This checks the seven days, no more.

## Assertions that explain failures

A `boolean` says that a property broke, not why. An assertion says why. `assertThat(value, assertion)` returns a
`TestResult`: a success, or a failure with an explanation. The assertions are static methods of `Assertion`
(`import static dev.zazr.test.Assertion.*;`).

Here is the midpoint property again, with an assertion:

```java
var naturals = Gen.integers(0, Integer.MAX_VALUE); // Gen<Integer>
Check.check(naturals, naturals, (a, b) ->
        assertThat(midpoint(a, b), isWithin(Math.min(a, b), Math.max(a, b))));
```

With the seed 42, the error now says what went wrong:

```text
falsified at sample 2 by (2147483647, 513683364): -816900142 is not within 513683364 and 2147483647 (seed 42, replay with -Dzazr.check.seed=42)
```

The catalogue covers values (`equalTo`, `isGreaterThan`, `isWithin`), strings (`startsWithString`, `matchesRegex`),
any `Iterable` (`hasSize`, `contains`, `forall`, `isSorted`), the Zazr types (`isSome`, `isRight`, `isValid`), and
code that must throw (`throwsA`). Many of them take another assertion, such as `hasSize(isLessThan(3))`.

`and`, `or` and `not` combine assertions, and `label` gives one a name for its explanation. `assertThat` takes several
assertions, and a failure lists every one that fails, not only the first:

```java
// Assertion<Integer>
var isPercentage = isGreaterThanOrEqualTo(0).and(isLessThanOrEqualTo(100)).label("a percentage");
var result       = assertThat(Vector.of(20, 150, -3), forall(isPercentage), hasSize(isLessThan(3)));
```

```text
Vector(20, 150, -3) has 150 at index 1:
  a percentage: 150 is greater than 100
Vector(20, 150, -3) has size 3:
  3 is not less than 3
```

When an assertion contains another, the explanation shows the part that failed: `forall` names the first element
that breaks it, and the label says which rule that was.

When the property only asserts on the generated value, `Check.check(gen, assertions...)` skips the lambda:

```java
var sortedScores = Gen.vector(Gen.integers(0, 100)).map(Vector::sorted); // Gen<Vector<Integer>>
Check.check(sortedScores, forall(isPercentage), isSorted());
```

You can keep the assertions you already use, too. A property may throw a JUnit or AssertJ assertion error: that
falsifies the sample, and the error's message goes into the failure.

## Reading the result

`Check.check` fails the test. `Check.evaluate` runs the same check and returns a `CheckResult` instead, for code that
wants to look at the outcome. A `CheckResult` is one of three records, so a `switch` over it covers every case, and
the compiler checks it. Here, with the `midpoint` from above, before the fix:

```java
var config = CheckConfig.defaults().withSeed(42);                                        // CheckConfig
var result = Check.evaluate(config, naturals, naturals, (a, b) -> midpoint(a, b) >= 0);  // CheckResult
var summary = switch (result) {
    case CheckResult.Satisfied(var samples) -> "passed " + samples + " samples";
    case CheckResult.Falsified(var sample, var seed, var counterexample, var _) ->
            "sample " + sample + " broke it: " + counterexample + ", seed " + seed;
    case CheckResult.Erroneous(var sample, var _, var cause, var _) -> "sample " + sample + " threw " + cause;
};
// "sample 2 broke it: (2147483647, 513683364), seed 42"
```

- `Satisfied`: every sample passed.
- `Falsified`: a sample broke the property. It returned `false` or a failed `TestResult`, or threw an
  `AssertionError`.
- `Erroneous`: a generator threw, or the property threw another exception.

`isSatisfied()`, `isFalsified()` and `isErroneous()` answer the same question without pattern matching.

## Replaying a failure

The same seed gives the same values. So a failure replays from the seed it prints, and the check fails again at the
same sample, with the same counterexample.

The failure tells you how: pass the seed to the test JVM as a system property, with no change to the code.

```bash
mvn test -Dtest=MidpointTest -Dzazr.check.seed=42
```

Maven passes it on to the tests as it is. With Gradle, forward it with `systemProperty` in the `test` task.

To keep the case in the code, while you fix it or after, set the seed in a `CheckConfig`:

```java
var config = CheckConfig.defaults().withSeed(42);                        // CheckConfig
Check.check(config, naturals, naturals, (a, b) -> midpoint(a, b) >= 0);  // throws an AssertionError
```

A seed set with `withSeed` takes precedence over the system property.

## Checking the rules of your own types

Some rules come with every Java class. Override `equals`, and Java expects equal values to have equal hash codes, a
value to equal its copy, and nothing to equal `null`. Break one, and a `HashMap` or a `HashSet` quietly loses your
values.

`zazr-test` has these rules ready to check, as laws. You don't write the test: you give a generator of your values,
and the check tries the rules on hundreds of them.

Here is a `Money` record that compares currencies ignoring their case, but kept the `hashCode` that Java generated,
which doesn't:

```java
record Money(long cents, String currency) {
    @Override
    public boolean equals(Object o) {
        return o instanceof Money(var c, var cur) && c == cents && cur.equalsIgnoreCase(currency);
    }
}

var money = new EqualitySubject<Money>(
        Gen.longs(0, 100_000).zipWith(Gen.elements("EUR", "USD"), Money::new), // the values
        m -> new Money(m.cents(), m.currency().toLowerCase()),                 // an equal copy
        m -> Tuple.of(m.cents(), m.currency().toUpperCase()));                 // what equals compares
EqualityLaws.<Money>all().assertSatisfied(money); // throws an AssertionError
```

The subject gives the check three things: how to generate values, how to build an equal copy of one, and what two
equal values have in common. With the seed 42, the error names the rule that broke and shows why:

```text
1 law(s) failed:
equalsHashCodeConsistency: falsified at sample 1 by (Money[cents=1, currency=EUR], Money[cents=100000, currency=USD]): Money[cents=1, currency=EUR] and Money[cents=1, currency=eur] are equal but hash to 69057 and 100833 (seed 42, replay with -Dzazr.check.seed=42)
```

`EUR` and `eur` are equal, but their hash codes differ, so a `HashSet` could hold both. The fix is a `hashCode` that
ignores the case too.

The package `dev.zazr.test.laws` has other sets of laws, for types with `map`, `flatMap` or `zip`, and for
collections.

## No shrinking

`zazr-test` does not shrink a failing value into a smaller one. The counterexample is the sample that failed, as it
was generated.

The growing size keeps it small in practice: the first samples are the small ones, so the first failure is usually
small too. When a counterexample is still large, the seed replays it, so you can look at it in a debugger.

## Thank you, ZIO Test

`zazr-test` follows the design of [ZIO Test](https://zio.dev/reference/test/), the testing library of ZIO: one
generator type whose values grow over a run, assertions that explain why they failed, and seeds that replay a run.
We ported those ideas to Java. Thank you to the ZIO contributors, who created ZIO Test and keep making it better.

## Getting it

`zazr-test` 0.1.0 is on Maven Central. Add it with the test scope:

=== "Maven"

    ```xml
    <dependency>
        <groupId>dev.zazr</groupId>
        <artifactId>zazr-test</artifactId>
        <version>0.1.0</version>
        <scope>test</scope>
    </dependency>
    ```

=== "Gradle (Kotlin)"

    ```kotlin
    dependencies {
        testImplementation("dev.zazr:zazr-test:0.1.0")
    }
    ```

The [Testing](../../testing.md) page covers everything in this post and more: the full list of
[assertions](../../testing.md#assertions) and [generators](../../testing.md#generators), filtering, the
configuration, and every set of [laws](../../testing.md#laws).

Feedback is very welcome. If something is missing, unclear or broken, please
[open an issue](https://github.com/guizmaii-opensource/zazr/issues).

Thank you for reading.
