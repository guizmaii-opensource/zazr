---
description: Property-based testing with zazr-test - Arbitrary, Gen, Property and CheckResult, in any JUnit test.
---

# Testing with `zazr-test`

`com.guizmaii:zazr-test` checks properties against random inputs: you state what must hold for every value, it
generates many values and reports the first one that breaks it. It runs inside any test framework; a check is a method
call that returns a result you assert on.

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

The types are in `com.guizmaii.zazr.test`:

| Type | Role |
|---|---|
| `Gen<T>` | a generator: a function from a `java.util.Random` to a `T`, with `map`, `flatMap`, `filter`, `choose`, `oneOf`, `frequency` |
| `Arbitrary<T>` | a generator per size hint: `Arbitrary.integer()`, `string(Gen<Character>)`, `list(Arbitrary)`, `stream(Arbitrary)`, `of(values...)`, `localDateTime()` |
| `Property` | the builder: `Property.def(name).forAll(arbitraries...).suchThat(predicate)`, from 1 to 8 arbitraries |
| `CheckResult` | the outcome: satisfied, falsified (with the sample) or erroneous; `assertIsSatisfied()` throws an `AssertionError` otherwise |

## A property

```java
CheckResult result = Property.def("reversing twice gives the list back")
    .forAll(Arbitrary.list(Arbitrary.integer()))
    .suchThat(list -> list.reverse().reverse().equals(list))
    .check();
result.assertIsSatisfied();
```

`check()` tries 1,000 samples with a size hint of 100; `check(size, tries)` chooses both. A falsified result carries
the sample that broke the property in `sample()`.

```java
CheckResult broken = Property.def("every list is short")
    .forAll(Arbitrary.list(Arbitrary.integer()))
    .suchThat(list -> list.length() < 5)
    .check(100, 1_000);
boolean falsified = broken.isFalsified();
// true, and broken.sample() holds the first list of 5 elements or more
```

## Generators

Build a `Gen` from the combinators, then turn it into an `Arbitrary` (which ignores the size hint) or write an
`Arbitrary` that uses the size.

```java
Gen<Integer> dice = Gen.choose(1, 6);
Arbitrary<Tuple2<Integer, Integer>> pairs = dice.flatMap(a -> dice.map(b -> Tuple.of(a, b))).arbitrary();
CheckResult sums = Property.def("two dice sum to 2..12")
    .forAll(pairs)
    .suchThat(p -> p._1() + p._2() >= 2 && p._1() + p._2() <= 12)
    .check();
sums.assertIsSatisfied();
```

## Preconditions

`implies` turns the property into a precondition: samples that fail it are not counted against the property, only
the ones that pass it are checked against the postcondition.

```java
Checkable halving = Property.def("an even number is twice its half")
    .forAll(Arbitrary.integer())
    .suchThat(n -> n % 2 == 0)
    .implies(n -> (n / 2) * 2 == n);
halving.check().assertIsSatisfied();
```

Properties compose with `and` and `or`. `suchThatResult` and `impliesResult` take a predicate returning a
`PredicateResult`, whose failure message ends up in the `CheckResult`.
