<p align="center">
  <img src="docs/assets/zaz-256.png" alt="Zaz, the zazr capybara, with a yuzu on its head" width="180">
</p>

<h1 align="center">zazr</h1>

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
  <a href="https://guizmaii-opensource.github.io/zazr/"><b>Website</b></a> ·
  <a href="https://guizmaii-opensource.github.io/zazr/getting-started/">Getting started</a> ·
  <a href="https://guizmaii-opensource.github.io/zazr/collections/complexity/">Complexity</a> ·
  <a href="https://guizmaii-opensource.github.io/zazr/design/">Design</a>
</p>

> [!WARNING]
> **Pre-1.0 and changing fast.** Nothing is released yet. Snapshots of `main` are published to Maven Central's
> snapshot repository, and the API changes between them.

## What it is

zazr is a fork of [Vavr](https://github.com/vavr-io/vavr). It keeps Vavr's persistent collections and its
`Option`, `Either`, `Try`, `Validation` and `Lazy`, and reshapes them around a few rules.

| | |
|---|---|
| **Names say what an operation does** | `zip`, `zipWith`, `collectAll`, `forEach`, `mapBoth`, `tap`, `catchAll`, `flip`: the name tells you the result, not which algebra it comes from. |
| **[`zip` at arity 2 to 8](https://guizmaii-opensource.github.io/zazr/zip/)** | One static call per arity replaces `ap` and the builder ladders, and never nests a tuple inside a tuple. |
| **[`Validation` keeps every error](https://guizmaii-opensource.github.io/zazr/validation/)** | The errors accumulate in a `NonEmptyVector`, so an invalid value always carries at least one. |
| **[Non-empty types make partial operations total](https://guizmaii-opensource.github.io/zazr/non-empty-vector/)** | `head()`, `max`, `reduce` on a `NonEmptyVector` cannot fail, and the return types say what can become empty. |
| **[Each collection states its cost](https://guizmaii-opensource.github.io/zazr/collections/complexity/)** | There is no `Seq` promising `get(i)` on a cons list. Every positional method documents its complexity. |
| **[Modern Java](https://guizmaii-opensource.github.io/zazr/java-interop/)** | Sealed interfaces and records you can `switch` over, the JDK's functional interfaces, O(1) `java.util` views through `asJava()`. |
| **[No `null` inside](https://guizmaii-opensource.github.io/zazr/control-types/)** | `Some`, `Right`, `Success`, `Valid` and every collection reject it. Absence is an `Option`. |

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

More in the [guide](https://guizmaii-opensource.github.io/zazr/getting-started/).

## Compared to Vavr

Removed: the `Match` API (use `switch` and record patterns), `Future`, `Promise` and `Task`, `Array`, `CharSeq`,
`Tree`, `BitSet`, `PriorityQueue`, the `Multimap` family, `Seq`, `IndexedSeq`, `LinearSeq`, `Foldable`, `Value`,
`Function0..2` (use `java.util.function`), `Serializable`, and the category-theory names. Control types are no
longer `Iterable`; each has its own conversions. Sets and maps have no positional methods, except the ordered
ones (`TreeSet`, `TreeMap`, `LinkedHashSet`, `LinkedHashMap`).

Every decision and its reason is in the [design record](https://guizmaii-opensource.github.io/zazr/design/) ([source](docs/design.md)); the [comparison page](https://guizmaii-opensource.github.io/zazr/vavr/) has the details.

## Building

```bash
make help      # list the targets
make verify    # what CI runs: tests, formatting, nullness, vocabulary and complexity checks
make test-one TEST=VectorTest MODULE=zazr-core
```

## License

Apache License 2.0. zazr is derived from Vavr, copyright its authors; see [NOTICE](NOTICE).
