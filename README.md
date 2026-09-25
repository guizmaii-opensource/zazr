<p align="center">
  <img src="docs/assets/zaz-256.png" alt="Zaz, the Zazr capybara, with a yuzu on its head" width="180">
</p>

<h1 align="center">Zazr</h1>

<p align="center">
  <b>Modern Functional Programming for Java 25+</b><br>
  Inspired by modern <a href="https://www.scala-lang.org">Scala</a>, <a href="https://zio.dev">ZIO</a>,
  and <a href="https://zio.dev/zio-prelude/">zio-prelude</a>
</p>

<p align="center">
  <a href="https://github.com/guizmaii-opensource/zazr/actions/workflows/ci.yml"><img src="https://github.com/guizmaii-opensource/zazr/actions/workflows/ci.yml/badge.svg" alt="CI"></a>
  <img src="https://img.shields.io/badge/Java-25%2B-f5a524" alt="Java 25+">
  <img src="https://img.shields.io/badge/runtime%20dependencies-none-8b5a2b" alt="No runtime dependencies">
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-Apache%202.0-8b5a2b" alt="Apache 2.0"></a>
</p>

<p align="center">
  <a href="https://zazr.dev/"><b>Website</b></a> ·
  <a href="https://zazr.dev/getting-started/">Getting started</a> ·
  <a href="https://zazr.dev/collections/complexity/">Complexity</a> ·
  <a href="https://zazr.dev/principles/">Design</a>
</p>

> [!WARNING]
> **Pre-1.0 and changing fast.** Nothing is released yet. Snapshots of `main` are published to Maven Central's
> snapshot repository, and the API changes between them.

## What Zazr is

Zazr gives Java immutable collections and the types that make functional code pleasant: `Option`, `Either`, `Try`,
`Validation` and `Lazy`. It is built for Java 25: sealed interfaces, records and pattern matching are part of its
API, not an afterthought. Its design comes from modern Scala's collections and from ZIO.

## What it brings

- **[Collections that state their cost.](https://zazr.dev/collections/complexity/)** Every operation whose cost
  depends on the size documents it, and the complexity page lists them all. You choose a collection for what you do
  with it, not by guessing.
- **[Persistent collections you can build fast.](https://zazr.dev/builders/)** A builder fills a collection in place
  and hands it over once, so building one in a loop copies nothing.
- **[Errors you do not lose.](https://zazr.dev/validation/)** `Validation` collects every error instead of stopping
  at the first, in a list that is never empty.
- **[Combine up to eight values in one call.](https://zazr.dev/zip/)** `zipWith` takes all of them and a function of
  their values, with no nested tuples to unpack.
- **[Collections that cannot be empty.](https://zazr.dev/non-empty-vector/)** On a `NonEmptyVector`, `head`, `max`
  and `reduce` cannot fail, and the return types tell you when that guarantee is lost.
- **[Pattern matching on results.](https://zazr.dev/control-types/)** `Option`, `Either`, `Try` and `Validation` are
  sealed interfaces of records, so a `switch` over them is checked by the compiler.
- **No `null` inside.** Values and collections reject it, and absence is an `Option`.
- **[Java interop without copies.](https://zazr.dev/java-interop/)** `asJava()` gives a read-only `java.util` view in
  constant time, and the way back does not copy either.
- **Names that say what happens.** `zip`, `collectAll`, `catchAll`, `mapBoth`: the vocabulary of ZIO, with no theory
  to learn first.

## Installation

JDK 25 or later. No runtime dependencies.

Maven:

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

Gradle:

```kotlin
repositories {
    maven("https://central.sonatype.com/repository/maven-snapshots/")
}

dependencies {
    implementation("com.guizmaii:zazr-core:0.1.0-SNAPSHOT")
}
```

`com.guizmaii:zazr-test` adds property-based testing (`Arbitrary`, `Gen`, `Checkable`).

## A short tour

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

More in the [guide](https://zazr.dev/getting-started/), and the ideas behind the API on the [Design](https://zazr.dev/principles/) page.

## Building

```bash
make help      # list the targets
make verify    # what CI runs: tests, formatting, nullness, vocabulary and complexity checks
make test-one TEST=VectorTest MODULE=zazr-core
```

## License

Apache License 2.0. Zazr started as a fork of [Vavr](https://github.com/vavr-io/vavr), copyright its authors; see
[NOTICE](NOTICE). Coming from Vavr? [This page](https://zazr.dev/vavr/) lists what changed.
