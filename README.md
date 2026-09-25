# zazr

[![CI](https://github.com/guizmaii-opensource/zazr/actions/workflows/ci.yml/badge.svg)](https://github.com/guizmaii-opensource/zazr/actions/workflows/ci.yml)

Immutable collections and control types for Java 25+, with an API inspired by [ZIO](https://zio.dev),
[zio-prelude](https://zio.dev/zio-prelude/) and modern [Scala](https://www.scala-lang.org).

zazr is a fork of [Vavr](https://github.com/vavr-io/vavr). It keeps Vavr's persistent collections and its
`Option`, `Either`, `Try`, `Validation` and `Lazy`, and reshapes them:

- **Names say what an operation does**, not which algebra it comes from: `zip`, `zipWith`, `collectAll`,
  `forEach`, `mapBoth`, `tap`, `catchAll`, `flip`.
- **`zip` at arity 2 to 8** replaces `ap` and builders, and never nests tuples.
- **`Validation` accumulates every error** in a `NonEmptyVector`, so an invalid value always carries at least one.
- **Non-empty types make partial operations total**: `NonEmptyVector.head()`, `max`, `reduce` cannot fail.
- **Each collection declares its own API and states its cost.** There is no `Seq` promising `get(i)` on a
  cons list; every positional method documents its complexity.
- **Modern Java**: sealed interfaces and records you can `switch` over, JDK functional interfaces,
  O(1) `java.util` views through `asJava()`.
- **No `null` inside**: `Some`, `Right`, `Valid` and every collection reject it.

## Status

Pre-1.0 and changing fast. Nothing is released yet. Snapshots of `main` are published to Maven Central's
snapshot repository; expect breaking changes between them.

## Requirements

JDK 25 or later. No runtime dependencies.

## Installation

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

## Compared to Vavr

Removed: the `Match` API (use `switch` and record patterns), `Future`, `Promise` and `Task`, `Array`, `CharSeq`,
`Tree`, `BitSet`, `PriorityQueue`, the `Multimap` family, `Seq`, `IndexedSeq`, `LinearSeq`, `Foldable`, `Value`,
`Function0..2` (use `java.util.function`), `Serializable`, and the category-theory names. Control types are no
longer `Iterable`; each has its own conversions. Sets and maps have no positional methods, except the ordered
ones (`TreeSet`, `TreeMap`, `LinkedHashSet`, `LinkedHashMap`).

Every decision and its reason is in [docs/design.md](docs/design.md).

## Building

```bash
make help      # list the targets
make verify    # what CI runs: tests, formatting, nullness, vocabulary and complexity checks
make test-one TEST=VectorTest MODULE=zazr-core
```

## License

Apache License 2.0. zazr is derived from Vavr, copyright its authors; see [NOTICE](NOTICE).
