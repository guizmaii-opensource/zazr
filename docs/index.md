---
template: home.html
title: Zazr
description: Modern Functional Programming for Java 25+. Inspired by modern Scala, ZIO, and zio-prelude.
hide:
  - navigation
  - toc
---

```java
// one call per arity, never a Tuple2<Tuple2<A, B>, C>
Option<Integer> sum = Option.zipWith(Option.some(1), Option.some(2), Option.some(3),
    (a, b, c) -> a + b + c);

// total on a collection that cannot be empty
int best = NonEmptyVector.of(7, 3, 9).max(Integer::compare);
// sum is Some(6), best is 9
```

<!-- /hero -->

<div class="zz-section zz-status" markdown>

!!! warning "Pre-1.0 and changing fast"

    Nothing is released yet. Snapshots of `main` are published to Maven Central's snapshot repository, and the API
    changes between them. Pin a snapshot you have tested.

</div>

<div class="zz-section" markdown>

## What it is { .zz-kicker }

Zazr is a fork of [Vavr](https://github.com/vavr-io/vavr). It keeps Vavr's persistent collections and its `Option`,
`Either`, `Try`, `Validation` and `Lazy`, and reshapes them around a few rules.

<div class="grid cards zz-cards zz-cards--features" markdown>

-   :material-tag-text-outline:{ .lg } __Names say what an operation does__

    ---

    `zip`, `zipWith`, `collectAll`, `forEach`, `mapBoth`, `tap`, `catchAll`, `flip`: the name tells you what you get
    back.

-   :material-numeric-8-box-multiple-outline:{ .lg } __`zip` at arity 2 to 8__

    ---

    Combine up to eight values in one call, with a function that receives them all. No nested tuples.

    [:octicons-arrow-right-24: zip at arity N](zip.md)

-   :material-format-list-checks:{ .lg } __`Validation` keeps every error__

    ---

    The errors accumulate in a `NonEmptyVector`, so an invalid value always carries at least one.

    [:octicons-arrow-right-24: Validation](validation.md)

-   :material-shield-check-outline:{ .lg } __Non-empty types make partial operations total__

    ---

    `head()`, `max`, `reduce` on a `NonEmptyVector` cannot fail, and the return types say what can become empty.

    [:octicons-arrow-right-24: NonEmptyVector](non-empty-vector.md)

-   :material-timer-sand:{ .lg } __Each collection states its cost__

    ---

    No shared interface hides a slow `get(i)`. Every positional method documents its cost, and one page lists them
    all.

    [:octicons-arrow-right-24: Complexity](collections/complexity.md)

-   :material-language-java:{ .lg } __Modern Java__

    ---

    Sealed interfaces and records you can `switch` over, the JDK's functional interfaces, and `java.util` views
    without copying.

    [:octicons-arrow-right-24: Java interop](java-interop.md)

-   :material-null:{ .lg } __No `null` inside__

    ---

    `Some`, `Right`, `Success`, `Valid` and every collection reject it. Absence is an `Option`.

    [:octicons-arrow-right-24: Control types](control-types.md)

</div>

</div>

<div class="zz-section" markdown>

## A short tour { .zz-kicker }

```java
// Validation keeps every error, not the first one
Validation<String, User> user = Validation.zipWith(name(""), age(-1), email("jules"), User::new);
String message = switch (user) {
    case Valid(var u) -> "hello " + u.name();
    case Invalid(var errors) -> errors.mkString(", ");
};
// "name is blank, age is negative, email has no @"

// zip at any arity up to 8, no Tuple2<Tuple2<A, B>, C>
Option<Integer> sum = Option.zipWith(Option.some(1), Option.some(2), Option.some(3), (a, b, c) -> a + b + c);

// total operations on a collection that cannot be empty
NonEmptyVector<Integer> scores = NonEmptyVector.of(7, 3, 9);
int best = scores.max(Integer::compare);

// a builder instead of repeated append
Vector.Builder<Integer> builder = Vector.newBuilder();
for (int i = 0; i < 1_000; i++) {
    builder.add(i);
}
Vector<Integer> numbers = builder.result();
```

`name`, `age` and `email` each return a `Validation<String, ...>`; [Validation](validation.md) shows them.

</div>

<div class="zz-section" markdown>

## Where the ideas come from { .zz-kicker }

<div class="grid cards zz-cards zz-cards--plain" markdown>

-   __ZIO__

    ---

    The operation names (`zipWith`, `collectAll`, `forEach`, `tap`, `catchAll`, `mapBoth`), one name per
    operation, and one default sequence with documented costs, as `Chunk` is in ZIO.

-   __zio-prelude__

    ---

    `Validation` with its errors in a non-empty collection, `zip` to combine independent values, and a non-empty
    collection whose return types say when it may become empty.

-   __Modern Scala__

    ---

    The Scala 3 collections: the `Vector` builder, fast set operations on sorted sets, `grouped` and `sliding` that
    return collections, and a table of what each operation costs. Sealed interfaces, records and `switch` bring
    Scala's pattern matching to Java.

</div>

</div>

<div class="zz-section" markdown>

## Compared to Vavr { .zz-kicker }

Zazr is not a drop-in replacement for Vavr. The main differences:

- The `Match` API is gone: use `switch` with record patterns.
- `Future` and `Promise` are gone, as are the less used collections (`Array`, `CharSeq`, `Tree`, `Multimap`...).
- `Function0` to `Function2` are gone: use `java.util.function`.
- `Option`, `Either`, `Try` and `Validation` are no longer `Iterable`; each has explicit conversions.
- Sets and maps without a defined order have no positional methods such as `head` or `take`.

[:octicons-arrow-right-24: The full comparison](vavr.md){ .md-button }

</div>

<div class="zz-section" markdown>

## Install { .zz-kicker }

JDK 25 or later, no runtime dependencies. Snapshots of `main` are on Maven Central's snapshot repository.

=== "Maven"

    ```xml
    <repositories>
        <repository>
            <id>central-portal-snapshots</id>
            <url>https://central.sonatype.com/repository/maven-snapshots/</url>
            <snapshots><enabled>true</enabled></snapshots>
            <releases><enabled>false</enabled></releases>
        </repository>
    </repositories>

    <dependency>
        <groupId>com.guizmaii</groupId>
        <artifactId>zazr-core</artifactId>
        <version>0.1.0-SNAPSHOT</version>
    </dependency>
    ```

=== "Gradle (Kotlin)"

    ```kotlin
    repositories {
        maven("https://central.sonatype.com/repository/maven-snapshots/")
    }

    dependencies {
        implementation("com.guizmaii:zazr-core:0.1.0-SNAPSHOT")
    }
    ```

`com.guizmaii:zazr-test` adds property-based testing; see [Testing with zazr-test](testing.md).

[Get started :octicons-arrow-right-24:](getting-started.md){ .md-button .md-button--primary }

</div>
