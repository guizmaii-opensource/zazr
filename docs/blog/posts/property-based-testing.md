---
date: 2026-09-28
authors:
  - guizmaii
categories:
  - Testing
slug: property-based-testing
description: Property-based testing in your JUnit tests with zazr-test - generators, checks, assertions that explain failures, seeds that replay them, and laws for your own types.
---

# Property-based testing with zazr-test

Zazr comes with a second artifact, `zazr-test`, for property-based testing. You state a rule that must hold for every
input, and it tries the rule on a few hundred generated inputs. It runs inside the tests you already have, JUnit or
any other framework.

This post shows what property-based testing is, how to write a check, how to read a failure, and how to replay it.

<!-- more -->

## The idea

An example-based test checks the cases you thought of: reverse `[1, 2, 3]` and expect `[3, 2, 1]`. It is precise, and
it is limited by your imagination. The bug is often in the case you didn't write: the empty list, the negative
number, the value next to the limit.

A property-based test states a rule instead, one that must hold for every input. "Reversing a list twice gives the
list back" is such a rule. A generator produces the inputs: short lists and long ones, empty ones, lists with
duplicates, large and negative numbers. The check runs the rule on each of them, and stops at the first one that
breaks it.

The two styles work well together. Examples document the cases that matter to you; properties explore the ones you
didn't list.

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

You build the generators of your own types from these. `map`, `zipWith` and `flatMap` combine them, and `Gen` has one
generator per Zazr type, from `option` and `either` to `vector`, `hashMap` and `treeMap`. You pass it the generators
of the elements.

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
var percentage = isGreaterThanOrEqualTo(0).and(isLessThanOrEqualTo(100)).label("a percentage");
var result     = assertThat(Vector.of(20, 150, -3), forall(percentage), hasSize(isLessThan(3)));
```

```text
Vector(20, 150, -3) has 150 at index 1:
  a percentage: 150 is greater than 100
Vector(20, 150, -3) has size 3:
  3 is not less than 3
```

A nested assertion's explanation follows the part that failed: `forall` names the first element that breaks it, and
the label says which rule that was.

When the property only asserts on the generated value, `Check.check(gen, assertions...)` skips the lambda:

```java
var scores = Gen.vector(Gen.integers(0, 100)); // Gen<Vector<Integer>>
Check.check(scores, forall(percentage), hasSize(isLessThanOrEqualTo(100)));
```

You can keep the assertions you already use, too. A property may throw a JUnit or AssertJ assertion error: that
falsifies the sample, and the error's message goes into the failure.

## Reading the result

`Check.check` fails the test. `Check.evaluate` runs the same check and returns a `CheckResult` instead, for code that
wants to look at the outcome. A `CheckResult` is one of three records, so pattern matching over it covers every case.
Here, with the `midpoint` from above, before the fix:

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

Maven passes it to the tests. A Gradle build needs `systemProperty` in its `test` task.

To keep the case in the code, while you fix it or after, set the seed in a `CheckConfig`:

```java
var config = CheckConfig.defaults().withSeed(42);                        // CheckConfig
Check.check(config, naturals, naturals, (a, b) -> midpoint(a, b) >= 0);  // throws an AssertionError
```

A seed set with `withSeed` takes precedence over the system property.

## Laws for your own types

Some rules are shared by many types. Mapping the identity function over a value must change nothing: that rule is
called `mapIdentity`. `zazr-test` states such rules once, as laws in the package `dev.zazr.test.laws`, and you check
them against your own types.

A law needs two things from your type: a generator of its values, and the operation under test. You give them in a
subject. Here is a `Bag` whose `map` removes duplicates by mistake:

```java
record Bag(Vector<?> items) {
    Bag map(Function<Object, Object> f) { return new Bag(items.map(f).distinct()); }
}
var bags = new MapSubject<Bag>() {
    public Gen<Bag> values() { return Gen.vector(Gen.integers(0, 9)).map(Bag::new); }
    public Bag map(Bag bag, Function<Object, Object> f) { return bag.map(f); }
}; // MapSubject<Bag>
MapLaws.<Bag>all().assertSatisfied(bags); // throws an AssertionError
```

`MapLaws.all()` is two laws, `mapIdentity` and `mapComposition`. `assertSatisfied` checks both, then throws one error
that names each law that failed, the value that broke it, and the two sides of the rule. With the seed 42:

```text
1 law(s) failed:
mapIdentity: falsified at sample 13 by (Bag[items=Vector(9, 1, 2, 9, 8, 9)]): left = Bag[items=Vector(9, 1, 2, 8)], right = Bag[items=Vector(9, 1, 2, 9, 8, 9)] (seed 42, replay with -Dzazr.check.seed=42)
```

Mapping the identity over `Bag(9, 1, 2, 9, 8, 9)` lost the duplicates. `mapComposition` is not listed: it holds,
since removing the duplicates after the first `map` or only after the second gives the same bag.

The package has other sets: `FlatMapLaws`, `ZipLaws`, `EqualityLaws` (such as `equals` agreeing with `hashCode`) and
`CollectionLaws`, each with its subject. `Law.of` states a law of your own.

## No shrinking

`zazr-test` does not shrink a failing value into a smaller one. The counterexample is the sample that failed, as it
was generated.

The growing size keeps it small in practice: the first samples are the small ones, so the first failure is usually
small too. When a counterexample is still large, the seed replays it, so you can look at it in a debugger.

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
