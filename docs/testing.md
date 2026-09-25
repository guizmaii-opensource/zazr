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
| `Arbitrary<T>` | a generator whose values grow with a size: `Arbitrary.integer()`, `string(Gen<Character>)`, `of(values...)`, and one per Zazr type (`option`, `either`, `tryOf`, `validation`, `lazy`, `tuple2` to `tuple8`, `vector`, `nonEmptyVector`, `list`, `queue`, `stream`, `hashSet`, `linkedHashSet`, `treeSet`, `hashMap`, `linkedHashMap`, `treeMap`) |
| `Property` | the builder: `Property.named(name).forAll(arbitraries...).suchThat(predicate)`, from 1 to 8 arbitraries |
| `CheckResult` | the outcome, one of three records: `Satisfied`, `Falsified` (with the sample) or `Erroneous`; `assertIsSatisfied()` throws an `AssertionError` unless it is `Satisfied` |

## A property

```java
CheckResult result = Property.named("reversing twice gives the list back")
    .forAll(Arbitrary.list(Arbitrary.integer()))
    .suchThat(list -> list.reverse().reverse().equals(list))
    .check();
result.assertIsSatisfied();
```

`check()` tries 1,000 samples with a size of 100: lists of up to 100 elements, integers between -100 and 100.
`check(size, tries)` chooses both. When the property fails, `sample()` returns the value that broke it.

```java
CheckResult broken = Property.named("every list is short")
    .forAll(Arbitrary.list(Arbitrary.integer()))
    .suchThat(list -> list.length() < 5)
    .check(100, 1_000);
boolean falsified = broken.isFalsified();
// true, and broken.sample() holds the first list of 5 elements or more
```

## Reading a result

A `CheckResult` is a sealed interface of three records, so a `switch` covers every outcome:

- `Satisfied`: every sample passed; `count()` is the number of samples.
- `Falsified`: a sample broke the property; `counterexample()` holds it.
- `Erroneous`: the property or a generator threw; `cause()` holds the error.

```java
CheckResult outcome = Property.named("doubling gives an even number")
    .forAll(Arbitrary.integer())
    .suchThat(n -> (n * 2) % 2 == 0)
    .check();
String summary = switch (outcome) {
    case CheckResult.Satisfied satisfied -> "passed " + satisfied.count() + " samples";
    case CheckResult.Falsified falsified -> "broken by " + falsified.counterexample();
    case CheckResult.Erroneous erroneous -> "failed with " + erroneous.cause().getMessage();
};
// "passed 1000 samples"
```

## Generators

Build a `Gen` with `choose`, `map`, `flatMap` and the others, then turn it into an `Arbitrary` with
`arbitrary()`.

```java
Gen<Integer> dice = Gen.choose(1, 6);
Arbitrary<Tuple2<Integer, Integer>> pairs = dice.flatMap(a -> dice.map(b -> Tuple.of(a, b))).arbitrary();
CheckResult sums = Property.named("two dice sum to 2..12")
    .forAll(pairs)
    .suchThat(p -> p._1() + p._2() >= 2 && p._1() + p._2() <= 12)
    .check();
sums.assertIsSatisfied();
```

## Preconditions

With `implies`, the `suchThat` condition becomes a precondition. Samples that fail it are skipped; the others must
satisfy the `implies` condition.

```java
Checkable halving = Property.named("an even number is twice its half")
    .forAll(Arbitrary.integer())
    .suchThat(n -> n % 2 == 0)
    .implies(n -> (n / 2) * 2 == n);
halving.check().assertIsSatisfied();
```

Properties combine with `and` and `or`.

## Arbitraries for every Zazr type

`Arbitrary` has a ready-made generator for each Zazr type. Pass it the arbitraries of the elements.

| Kind | Arbitraries |
|---|---|
| Control types | `option`, `either`, `tryOf`, `validation`, `lazy` |
| Tuples | `tuple2` to `tuple8` |
| Sequences | `vector`, `nonEmptyVector`, `list`, `queue`, `stream` |
| Sets | `hashSet`, `linkedHashSet`, `treeSet` |
| Maps | `hashMap`, `linkedHashMap`, `treeMap` |

```java
Arbitrary<Validation<String, Integer>> checks =
    Arbitrary.validation(Arbitrary.of("too short", "no digit"), Arbitrary.integer());
Property.named("zip is valid only when both sides are")
    .forAll(checks, checks)
    .suchThat((a, b) -> a.zip(b).isValid() == (a.isValid() && b.isValid()))
    .check()
    .assertIsSatisfied();
```

## Unusual shapes included

A collection can hold the same elements in different internal layouts, and a bug may hide in only one of them. The
collection arbitraries build each value in several ways, so your properties meet the less common layouts too:

- a `Vector` built by dropping a prefix or by prepending, not only by `ofAll`;
- a `Queue` whose elements sit in both of its internal lists;
- a `Stream` whose tail is not evaluated yet;
- sets and maps that went through removals, or keys overwritten with new values.

Empty and single-element values come up often; so do values near the size limit.

## Laws

A law is a named rule that every value of a type must satisfy, such as `mapIdentity`: mapping the identity function
changes nothing. The package `com.guizmaii.zazr.test.laws` states each law once, and you check it against any type.

A law set groups laws. `MapLaws.all()` is `mapIdentity` and `mapComposition`; `and` joins two laws or two sets.

| Laws | What they check |
|---|---|
| `MapLaws` | `mapIdentity`, `mapComposition` |
| `FlatMapLaws` | `flatMapAssociativity`, `flatMapLeftIdentity`, `flatMapRightIdentity`, `mapIsFlatMapSucceed` |
| `ZipLaws` | `zipAssociativity`, `zipLeftIdentity`, `zipRightIdentity`, and `zipLeft`/`zipRight` agreeing with `zip` |
| `EqualityLaws` | `equalsHashCodeConsistency` |
| `CollectionLaws` | `size`, `toList` and `equals` agreeing with the elements, and equality across collection types |
| `BuilderLaws` | a builder or a collector gives the same collection as `ofAll` |

## Checking your own type

A law needs two things from your type: arbitrary values of it, and the operation under test. You give them in a
subject, such as a `MapSubject` for the map laws. The elements are integers; the laws see them as `Object`.

```java
record Box(Vector<Object> items) {
    Box map(Function<Object, Object> f) { return new Box(items.map(f)); }
}
MapSubject<Box> boxes = new MapSubject<>() {
    public Arbitrary<Box> values() { return Arbitrary.vector(Arbitrary.integer()).map(v -> new Box(v.map(x -> (Object) x))); }
    public Box map(Box box, Function<Object, Object> f) { return box.map(f); }
};
MapLaws.<Box>all().assertSatisfied(boxes, new Random(42));
```

The other subjects work the same way: `FlatMapSubject` adds `succeed` and `flatMap`, `ZipSubject` adds `zip`, and
`EqualitySubject` and `CollectionSubject` describe values and collections.

## When a law fails

`assertSatisfied` checks every law of the set, then throws one `AssertionError` that names each failing law and the
value that broke it, with the two sides of the rule that differed. For a `map` that drops the last element:

```text
2 law(s) failed:
mapIdentity: falsified at check 3 by (Box[items=Vector(0, 1)]) (left = Box[items=Vector(0)], right = Box[items=Vector(0, 1)])
mapComposition: falsified at check 1 by (Box[items=Vector(-5, -5)], x -> -9 * x + 92, x -> -10 * x + -59) (left = Box[items=Vector()], right = Box[items=Vector(-1429)])
```

Pass the same `Random` seed to replay a failure.
