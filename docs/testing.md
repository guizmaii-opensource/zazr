---
description: Property-based testing with zazr-test - Arbitrary, Gen, Property and CheckResult, in any JUnit test.
---

# Testing with `zazr-test`

`com.guizmaii:zazr-test` checks properties against random inputs. You state what must hold for every value; it
generates many values and reports the first one that breaks the rule.

It works in any test framework, such as JUnit: a check is a method call that returns a result you assert on.

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
| `Arbitrary<T>` | a generator whose values grow with a size: `Arbitrary.integer()`, `string(Gen<Character>)`, `list(Arbitrary)`, `of(values...)` |
| `Property` | the builder: `Property.def(name).forAll(arbitraries...).suchThat(predicate)`, from 1 to 8 arbitraries |
| `CheckResult` | the outcome: satisfied, falsified (with the sample) or erroneous; `assertIsSatisfied()` throws an `AssertionError` otherwise |

## A property

```java
var result = Property.def("reversing twice gives the list back")
    .forAll(Arbitrary.list(Arbitrary.integer()))
    .suchThat(list -> list.reverse().reverse().equals(list))
    .check(); // CheckResult
result.assertIsSatisfied();
```

`check()` tries 1,000 samples with a size of 100: lists of up to 100 elements, integers between -100 and 100.
`check(size, tries)` chooses both. When the property fails, `sample()` returns the value that broke it.

```java
var broken = Property.def("every list is short")
    .forAll(Arbitrary.list(Arbitrary.integer()))
    .suchThat(list -> list.length() < 5)
    .check(100, 1_000); // CheckResult
var falsified = broken.isFalsified();
// true, and broken.sample() holds the first list of 5 elements or more
```

## Generators

Build a `Gen` with `choose`, `map`, `flatMap` and the others, then turn it into an `Arbitrary` with
`arbitrary()`.

```java
var dice = Gen.choose(1, 6); // Gen<Integer>
var pairs = dice.flatMap(a -> dice.map(b -> Tuple.of(a, b)))
    .arbitrary(); // Arbitrary<Tuple2<Integer, Integer>>
var sums = Property.def("two dice sum to 2..12")
    .forAll(pairs)
    .suchThat(p -> p._1() + p._2() >= 2 && p._1() + p._2() <= 12)
    .check(); // CheckResult
sums.assertIsSatisfied();
```

## Preconditions

With `implies`, the `suchThat` condition becomes a precondition. Samples that fail it are skipped; the others must
satisfy the `implies` condition.

```java
var halving = Property.def("an even number is twice its half")
    .forAll(Arbitrary.integer())
    .suchThat(n -> n % 2 == 0)
    .implies(n -> (n / 2) * 2 == n); // Checkable
halving.check().assertIsSatisfied();
```

Properties combine with `and` and `or`.
