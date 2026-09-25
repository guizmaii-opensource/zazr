---
template: home.html
title: zazr
description: Immutable collections and control types for Java 25+, with an API inspired by ZIO, zio-prelude and modern Scala.
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
    changes between them. Pin a snapshot you have tested, and read the [design record](design.md) before relying on a
    shape that is still marked open.

</div>

<div class="zz-section" markdown>

## What it is { .zz-kicker }

zazr is a fork of [Vavr](https://github.com/vavr-io/vavr). It keeps Vavr's persistent collections and its `Option`,
`Either`, `Try`, `Validation` and `Lazy`, and reshapes them around a few rules.

<div class="grid cards zz-cards zz-cards--features" markdown>

-   :material-tag-text-outline:{ .lg } __Names say what an operation does__

    ---

    `zip`, `zipWith`, `collectAll`, `forEach`, `mapBoth`, `tap`, `catchAll`, `flip`: the name tells you the result,
    not which algebra it comes from.

-   :material-numeric-8-box-multiple-outline:{ .lg } __`zip` at arity 2 to 8__

    ---

    One static call per arity replaces `ap` and the builder ladders, and never nests a tuple inside a tuple.

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

    There is no `Seq` promising `get(i)` on a cons list. Every positional method documents its complexity, and a
    generated table lists them all.

    [:octicons-arrow-right-24: Complexity](collections/complexity.md)

-   :material-language-java:{ .lg } __Modern Java__

    ---

    Sealed interfaces and records you can `switch` over, the JDK's functional interfaces, O(1) `java.util` views
    through `asJava()`.

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

    Names that describe the purpose (`zipWith`, `collectAll`, `forEach`, `tap`, `catchAll`, `mapBoth`), one spelling
    per operation, a suffix vocabulary (`With`, `All`, `OrElse`, `from*`, `to*`), and one concrete collection type
    with one documented cost model, as `Chunk` is.

-   __zio-prelude__

    ---

    `Validation` with its errors in a non-empty collection, `zip` as the way to combine independent values, and
    non-empty types whose return types say which operations keep them non-empty.

-   __Modern Scala__

    ---

    The collections library Scala 3 ships: the `Vector` builder, the red-black tree set operations, own-type results
    for `grouped` and `sliding`, and the performance-characteristics table. Scala 3's idioms map to modern Java:
    sealed hierarchies and records play enums and case classes, and `switch` with record patterns plays pattern
    matching.

</div>

</div>

<div class="zz-section" markdown>

## Compared to Vavr { .zz-kicker }

Removed: the `Match` API (use `switch` and record patterns), `Future`, `Promise` and `Task`, `Array`, `CharSeq`,
`Tree`, `BitSet`, `PriorityQueue`, the `Multimap` family, `Seq`, `IndexedSeq`, `LinearSeq`, `Foldable`, `Value`,
`Function0..2` (use `java.util.function`), `Serializable`, and the category-theory names. Control types are no longer
`Iterable`; each has its own conversions. Sets and maps have no positional methods, except the ordered ones.

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
