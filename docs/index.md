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

## What Zazr is { .zz-kicker }

Zazr gives Java immutable collections and the types that make functional code pleasant: `Option`, `Either`,
`Try`, `Validation` and `Lazy`. It is built for Java 25: sealed interfaces, records and pattern matching are part of
its API, not an afterthought. Its design comes from modern Scala's collections and from ZIO.

## What it brings { .zz-kicker }

<div class="grid cards zz-cards zz-cards--features" markdown>

-   :material-timer-sand:{ .lg } __Collections that state their cost__

    ---

    Every operation whose cost depends on the size documents it, and one page lists them all. You choose a
    collection for what you do with it, not by guessing.

    [:octicons-arrow-right-24: Complexity](collections/complexity.md)

-   :material-hammer-wrench:{ .lg } __Persistent collections you can build fast__

    ---

    A builder fills a collection in place and hands it over once, so building one in a loop copies nothing.

    [:octicons-arrow-right-24: Builders](builders.md)

-   :material-format-list-checks:{ .lg } __Errors you do not lose__

    ---

    `Validation` collects every error instead of stopping at the first, in a list that is never empty.

    [:octicons-arrow-right-24: Validation](control/validation.md)

-   :material-numeric-8-box-multiple-outline:{ .lg } __Combine up to eight values in one call__

    ---

    `zipWith` takes all of them and a function of their values, with no nested tuples to unpack.

    [:octicons-arrow-right-24: zip at arity N](zip.md)

-   :material-shield-check-outline:{ .lg } __Collections that cannot be empty__

    ---

    On a `NonEmptyVector`, `head`, `max` and `reduce` cannot fail, and the return types tell you when that
    guarantee is lost.

    [:octicons-arrow-right-24: NonEmptyVector](non-empty-vector.md)

-   :material-source-branch:{ .lg } __Pattern matching on results__

    ---

    `Option`, `Either`, `Try` and `Validation` are sealed interfaces of records, so pattern matching on them is
    checked by the compiler.

    [:octicons-arrow-right-24: Control types](control/index.md)

-   :material-null:{ .lg } __No `null` inside__

    ---

    Values and collections reject it, and absence is an `Option`.

-   :material-language-java:{ .lg } __Java interop without copies__

    ---

    `asJava()` gives a read-only `java.util` view in constant time, and the way back does not copy either.

    [:octicons-arrow-right-24: Java interop](java-interop.md)

-   :material-tag-text-outline:{ .lg } __Names that say what happens__

    ---

    `zip`, `collectAll`, `catchAll`, `mapBoth`: the vocabulary of ZIO, with no theory to learn first.

</div>

</div>

<div class="zz-section" markdown>

## Zazr is AI ready { .zz-kicker }

Teach your coding assistant Zazr. The Zazr Agent Skill gives it the rules, the names and the sharp edges, so it
writes idiomatic Zazr code instead of Vavr from memory.

[Add it to your assistant :octicons-arrow-right-24:](ai-assistant.md){ .md-button .md-button--primary }
[The skill on GitHub](https://github.com/guizmaii-opensource/zazr/tree/main/skills/zazr){ .md-button }

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

`name`, `age` and `email` each return a `Validation<String, ...>`; [Validation](control/validation.md) shows them.

</div>

<div class="zz-section" markdown>

## Where the ideas come from { .zz-kicker }

<div class="grid cards zz-cards zz-cards--plain" markdown>

-   __Modern Scala__

    ---

    The Scala 3 collections: the `Vector` builder, fast set operations on sorted sets, `grouped` and `sliding` that
    return collections, and a table of what each operation costs. Sealed interfaces, records and `switch` bring
    Scala's pattern matching to Java.

-   __ZIO__

    ---

    The operation names (`zipWith`, `collectAll`, `forEach`, `tap`, `catchAll`, `mapBoth`), one name per
    operation, and one default sequence with documented costs, as `Chunk` is in ZIO.

-   __zio-prelude__

    ---

    `Validation` with its errors in a non-empty collection, `zip` to combine independent values, and a non-empty
    collection whose return types say when it may become empty.

</div>

Zazr started as a fork of [Vavr](https://github.com/vavr-io/vavr). Coming from Vavr?
[This page](vavr.md) lists what changed.

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
