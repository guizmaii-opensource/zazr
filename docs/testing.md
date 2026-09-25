---
description: Property-based testing with zazr-test - Gen generators, Check, CheckResult and CheckConfig in any JUnit test, a generator for every Zazr type, seeds that replay a failure, and ready-made laws.
---

# Testing with `zazr-test`

`com.guizmaii:zazr-test` checks properties against generated values. You state what must hold for every value; it
generates a few hundred values and reports the first one that breaks the rule.

A check is a method call that returns a result, so it runs in any test framework, such as JUnit.

=== "Maven"

    ```xml
    <dependency>
        <groupId>com.guizmaii</groupId>
        <artifactId>zazr-test</artifactId>
        <version>0.1.0-SNAPSHOT</version>
        <scope>test</scope>
    </dependency>
    ```

=== "Gradle (Kotlin)"

    ```kotlin
    dependencies {
        testImplementation("com.guizmaii:zazr-test:0.1.0-SNAPSHOT")
    }
    ```

## The types

Four types, all in `com.guizmaii.zazr.test`:

| Type | Role |
|---|---|
| `Gen<A>` | a generator of values of type `A`: scalars, combinators, and one generator per Zazr type |
| `Check` | runs a property: `check`, `checkN` and `checkAll`, for 1 to 8 generators |
| `CheckResult` | the outcome: `Satisfied`, `Falsified` or `Erroneous` |
| `CheckConfig` | the number of samples, the size, the seed and the discard budget |

## A first property

`Check.check` takes the generators and the property. The property returns `true` when it holds.

```java
var lists = Gen.list(Gen.integers()); // Gen<List<Integer>>
Check.check(lists, list -> list.reverse().reverse().equals(list)).assertIsSatisfied();
```

The check runs 200 samples. `assertIsSatisfied()` does nothing when every sample passed, and throws an
`AssertionError` otherwise, so the test fails.

## What a failure prints

The error names the sample that broke the property, its number, and the seed of the run.

```java
var config = CheckConfig.defaults().withSeed(42); // CheckConfig
var shortLists = Check.check(config, Gen.list(Gen.integers()), list -> list.size() < 5); // CheckResult
shortLists.assertIsSatisfied(); // throws an AssertionError
```

```text
falsified at sample 14 by (List(1064429137, -1, 2147483646, -499641955, 2147483647)) (seed 42, replay with -Dzazr.check.seed=42)
```

The sample is shown as a tuple, one value per generator.

## Small counterexamples, no shrinking

`zazr-test` does not shrink a failing value into a smaller one. It finds small values first instead.

The size grows over a run, from 0 for the first sample to 100 for the last. Collections and strings are at most as
long as the size, so the first samples are empty or short, and the first failure is usually a small one.

In the example above, the first list that breaks `size() < 5` has exactly 5 elements.

## Reading a result

A `CheckResult` is one of three records, so pattern matching over it covers every outcome:

- `Satisfied(samples)`: every sample passed.
- `Falsified(sampleNumber, seed, counterexample, message)`: the property returned `false` or threw an
  `AssertionError`.
- `Erroneous(sampleNumber, seed, cause, sample)`: a generator threw, or the property threw another exception.

```java
var result = Check.check(CheckConfig.defaults().withSeed(42), Gen.integers(0, 1000), n -> n < 500); // CheckResult
var summary = switch (result) {
    case CheckResult.Satisfied(var samples) -> "passed " + samples + " samples";
    case CheckResult.Falsified(var sampleNumber, _, var counterexample, _) -> "broken at sample " + sampleNumber + " by " + counterexample;
    case CheckResult.Erroneous(var sampleNumber, _, var cause, _) -> "failed at sample " + sampleNumber + " with " + cause;
};
// "broken at sample 3 by (1000)"
```

`isSatisfied()`, `isFalsified()` and `isErroneous()` answer the same question without pattern matching.

## Assertions in the property

The property may use JUnit or AssertJ assertions. A failed assertion falsifies the sample, and its message is kept
in the result. A block body still ends with `return true`.

```java
var digits = Gen.vector(Gen.integers(0, 9)); // Gen<Vector<Integer>>
var result = Check.check(CheckConfig.defaults().withSeed(42), digits, vector -> {
    assertThat(vector.distinct()).isEqualTo(vector);
    return true;
}); // CheckResult
var message = result.message(); // Option<String>
// Some("expected: Vector(9, 1, 2, 9, 8, 9) but was: Vector(9, 1, 2, 8)"), AssertJ's message on three lines
```

Any other exception thrown by the property makes the result `Erroneous`, with the exception as its cause.

## Generators

### Scalars

| Values | Generators |
|---|---|
| numbers | `integers()`, `integers(min, max)`, `longs()`, `longs(min, max)`, `doubles()`, `doubles(min, max)` |
| booleans | `booleans()` |
| characters | `chars()`, `chars(min, max)`, `alphaChars()`, `numericChars()`, `alphaNumericChars()`, `asciiChars()`, `printableChars()`, `unicodeChars()` |
| strings | `strings()`, `strings(chars)`, `stringsN(n, chars)`, `alphaNumericStrings()` |
| dates | `localDateTimes()`, `localDateTimes(min, max)` |

The scalar generators have plural names, such as `integers()` and `strings()`: each one generates many values.

A range favours its edges. Half of the draws of `integers(min, max)` are the bounds, their neighbours, -1, 0 or 1;
the others are uniform. Off-by-one mistakes show up in a few samples.

### Combining generators

`map`, `flatMap`, `zip`, `zipWith` and `filter` build a generator from others. `constant`, `elements`, `oneOf` and
`weighted` choose among values or generators.

```java
var dice = Gen.integers(1, 6); // Gen<Integer>
var twoDice = dice.zipWith(dice, Integer::sum); // Gen<Integer>
var coin = Gen.elements("heads", "tails"); // Gen<String>
var loadedCoin = Gen.weighted(Tuple.of(Gen.constant("heads"), 9.0), Tuple.of(Gen.constant("tails"), 1.0)); // Gen<String>
Check.check(twoDice, sum -> sum >= 2 && sum <= 12).assertIsSatisfied();
```

`Gen.zip(g1, ..., g8)` and `Gen.zipWith` combine up to eight generators at once.

## Finite generators and `checkAll`

`fromIterable` and `constant` are finite: they give their values in order, then stop. `zip` of two finite generators
gives every combination.

```java
var sizes = Gen.fromIterable(Vector.of("S", "M")); // Gen<String>
var colours = Gen.fromIterable(Vector.of("red", "blue")); // Gen<String>
var variants = sizes.zip(colours).runCollect(); // List<Tuple2<String, String>>
// List((S, red), (S, blue), (M, red), (M, blue))
```

`checkAll` checks every value of finite generators exactly once, instead of 200 samples.

```java
var days = Gen.fromIterable(EnumSet.allOf(DayOfWeek.class)); // Gen<DayOfWeek>
var result = Check.checkAll(days, day -> day.plus(7) == day); // CheckResult
// Satisfied[samples=7]
```

`elements` is random, not finite: each sample is one of its values, drawn at random.

## Size

The size bounds what a generator produces, such as the length of a collection. These generators read or change it:

- `Gen.sized(size -> ...)` builds a generator from the current size;
- `Gen.small(size -> ...)` gives a random size between 0 and the current size, most of them small;
- `Gen.large(size -> ...)` gives a random size between 0 and the current size, all as likely;
- `withSize(n)` runs a generator at a fixed size.

```java
var depth = Gen.sized(size -> Gen.integers(0, size)); // Gen<Integer>
var words = Gen.small(size -> Gen.stringsN(size, Gen.alphaChars())); // Gen<String>
var shortLists = Gen.list(Gen.integers()).withSize(3); // Gen<List<Integer>>, at most 3 elements
```

A check grows the size evenly over its samples, from 0 to the configured size:

```java
var sizes = Gen.size().runCollectN(5, CheckConfig.defaults().withSize(100)); // List<Integer>
// List(0, 25, 50, 75, 100)
```

## Filtering

`filter` keeps the values that satisfy a predicate. A random generator is run again until it gives one.

Filtering has a budget: after more than 1,000 rejected values in a row, the check gives up and the result is `Erroneous`. A
predicate that rejects most values is better written as a `map` that builds the wanted values.

```java
var evens = Gen.integers(-1000, 1000).filter(n -> n % 2 == 0); // Gen<Integer>
var alsoEvens = Gen.integers(-500, 500).map(n -> n * 2); // Gen<Integer>, with no rejected value
var impossible = Check.check(Gen.integers().filter(n -> false), n -> true); // CheckResult
// Erroneous: Gen.filter rejected 1001 values in a row, more than the discard budget of 1000
```

## Configuration

`CheckConfig.defaults()` is 200 samples, a size of 100, a random seed and a budget of 1,000 discards. Each value
has a `with` method, and `check` and `checkAll` take a configuration as their first argument.

```java
var config = CheckConfig.defaults().withSamples(1_000).withSeed(42); // CheckConfig
Check.check(config, Gen.integers(), n -> Integer.parseInt(Integer.toString(n)) == n).assertIsSatisfied();
Check.checkN(50, Gen.alphaNumericStrings(), s -> s.strip().equals(s)).assertIsSatisfied();
```

`checkN(n, ...)` is `check` with `n` samples.

System properties of the test JVM change the defaults for every check, without touching the code:

| Property | Default |
|---|---|
| `zazr.check.samples` | 200 |
| `zazr.check.size` | 100 |
| `zazr.check.seed` | a random seed |
| `zazr.check.maxDiscards` | 1000 |

## Replaying a failure

The same seed gives the same values, so a failure replays from the seed it prints. Put that seed in the code with
`CheckConfig.defaults().withSeed(42)`, and the check fails at the same sample again.

The failure also prints `-Dzazr.check.seed=42`: that system property replays it without touching the code, when the
build passes it to the test JVM. Maven does; a Gradle build needs `systemProperty` in its `test` task. A seed set with
`withSeed` takes precedence over the property.

## Generators for every Zazr type

`Gen` has a generator for each Zazr type. Pass it the generators of the elements.

| Kind | Generators |
|---|---|
| Control types | `option`, `some`, `none`, `either`, `tryOf`, `validation`, `lazy` |
| Tuples | `tuple2` to `tuple8` |
| Sequences | `vector`, `vectorN`, `nonEmptyVector`, `list`, `queue`, `stream` |
| Sets | `hashSet`, `linkedHashSet`, `treeSet` |
| Maps | `hashMap`, `linkedHashMap`, `treeMap` |

```java
var checks = Gen.validation(Gen.elements("too short", "no digit"), Gen.integers()); // Gen<Validation<String, Integer>>
Check.check(checks, checks, (a, b) -> a.zip(b).isValid() == (a.isValid() && b.isValid())).assertIsSatisfied();
```

A collection has up to the current size elements. Half of the lengths are 0, 1, the size or the size minus one, so
empty, single-element and full collections come up often.

## Unusual layouts included

A collection can hold the same elements in different internal layouts, and a bug may hide in only one of them. The
collection generators build each value in several ways, so your properties meet the less common layouts too:

- a `Vector` built by dropping a prefix, by prepending, or as a slice, not only by `ofAll`;
- a `Queue` whose elements sit in both of its internal lists;
- a `Stream` whose tail is not evaluated yet;
- sets and maps that went through removals, or keys overwritten with new values.

## Laws

A law is a named rule that every value of a type must satisfy, such as `mapIdentity`: mapping the identity function
changes nothing. The package `com.guizmaii.zazr.test.laws` states each law once, and you check it against any type.

A law set groups laws. `MapLaws.all()` is `mapIdentity` and `mapComposition`; `and` joins two laws or two sets.

| Laws | What they check |
|---|---|
| `MapLaws` | `mapIdentity`, `mapComposition` |
| `FlatMapLaws` | `flatMapAssociativity`, `flatMapLeftIdentity`, `flatMapRightIdentity`, `mapIsFlatMapSucceed` |
| `ZipLaws` | `zipAssociativity`, `zipLeftIdentity`, `zipRightIdentity`, and `zipLeft`/`zipRight` agreeing with `zip` |
| `EqualityLaws` | `equalsHashCodeConsistency`, and `equalsAgreesWithModel`: `equals` agrees with a simple model of the value, such as its elements as a JDK list |
| `CollectionLaws` | `size`, `toList` and `equals` agreeing with the elements, the iteration order (input, insertion or sorted), and equality across collection types |
| `BuilderLaws` | a builder or a collector gives the same collection as `ofAll` |
| `ValidationLaws` | `zip` of two `Validation`s keeps the errors of both sides, in order |
| `NonEmptyVectorLaws` | `head` never fails, and `equals` between a `NonEmptyVector` and its `Vector` gives the same answer both ways |

`Law.of(name, (subject, config) -> ...)` states a law of your own; its check is usually a `Check.check`.

## Checking your own type

A law needs two things from your type: a generator of its values, and the operation under test. You give them in a
subject, such as a `MapSubject` for the map laws. The elements are integers; the laws see them as `Object`.

```java
record Box(Vector<Object> items) {
    Box map(Function<Object, Object> f) { return new Box(items.map(f)); }
}
var boxes = new MapSubject<Box>() {
    public Gen<Box> values() { return Gen.vector(Gen.integers(-100, 100)).map(v -> new Box(v.map(x -> (Object) x))); }
    public Box map(Box box, Function<Object, Object> f) { return box.map(f); }
}; // MapSubject<Box>
MapLaws.<Box>all().assertSatisfied(boxes);
```

The other subjects work the same way: `FlatMapSubject` adds `succeed` and `flatMap`, `ZipSubject` adds `zip`, and
`EqualitySubject` and `CollectionSubject` describe values and collections.

## When a law fails

`assertSatisfied` checks every law of the set, then throws one `AssertionError` that names each failing law, the
value that broke it, and the two sides of the rule that differed. Here, a `map` that drops the last element:

```java
var broken = new MapSubject<Box>() {
    public Gen<Box> values() { return boxes.values(); }
    public Box map(Box box, Function<Object, Object> f) { return new Box(box.map(f).items().dropRight(1)); }
}; // MapSubject<Box>
MapLaws.<Box>all().assertSatisfied(broken, CheckConfig.defaults().withSeed(42)); // throws an AssertionError
```

```text
2 law(s) failed:
mapIdentity: falsified at sample 5 by (Box[items=Vector(6)]): left = Box[items=Vector()], right = Box[items=Vector(6)] (seed 42, replay with -Dzazr.check.seed=42)
mapComposition: falsified at sample 7 by (Box[items=Vector(99, -6)], x -> -1 * x + -9, x -> -10 * x + 80): left = Box[items=Vector()], right = Box[items=Vector(1160)] (seed 42, replay with -Dzazr.check.seed=42)
```

The laws of a set run with the same seed, so one `-Dzazr.check.seed=42` replays every failure.
