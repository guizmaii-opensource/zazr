---
description: Add zazr to a build, then meet the control types and the collections in five minutes.
---

# Getting started

## Requirements

JDK 25 or later. zazr has no runtime dependency. `zazr-core` is the Java module `com.guizmaii.zazr`.

## Add the dependency

Nothing is released yet: the snapshots of `main` are on Maven Central's snapshot repository, and the API changes
between them.

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

On the module path, add `requires com.guizmaii.zazr;` to your `module-info.java`.

## Where things are

| Package | Types |
|---|---|
| `com.guizmaii.zazr.control` | `Option`, `Either`, `Try`, `Validation` |
| `com.guizmaii.zazr.collection` | `Vector`, `NonEmptyVector`, `List`, `Queue`, `Stream`, `HashSet`, `LinkedHashSet`, `TreeSet`, `HashMap`, `LinkedHashMap`, `TreeMap` |
| `com.guizmaii.zazr` | `Lazy`, `Tuple` and `Tuple0` to `Tuple8`, `Function3` to `Function8`, the `Checked*` functional interfaces |

`List` and `Stream` share their names with `java.util.List` and `java.util.stream.Stream`: import the zazr ones and
spell the JDK ones out, as the examples on this site do.

## Five minutes of zazr

A value that may be absent is an `Option`. It never holds `null`: `ofNullable` is the door from nullable code.

```java
Option<String> name = Option.ofNullable(System.getenv("ZAZR_DOCS_UNSET"));
String greeting = name.map(n -> "hello " + n).getOrElse("hello stranger");
// "hello stranger"
```

Every sum type is a sealed interface of records, so `switch` is exhaustive and deconstructs them.

```java
Either<String, Integer> parsed = Either.right(42);
String text = switch (parsed) {
    case Right(var n) -> "got " + n;
    case Left(var error) -> "failed: " + error;
};
```

A computation that may throw is a `Try`; `catchAll` recovers.

```java
Try<Integer> port = Try.of(() -> Integer.parseInt("80a"));
int value = port.catchAll(error -> 8080).get();
// 8080
```

`Vector` is the default sequence: every operation returns a new `Vector`, and positional access is effectively O(1).

```java
Vector<Integer> numbers = Vector.of(1, 2, 3, 4);
Vector<Integer> doubled = numbers.map(n -> n * 2).append(10);
int third = doubled.get(2);
// doubled is Vector(2, 4, 6, 8, 10), third is 6
```

## Next

- [Control types](control-types.md): `Option`, `Either`, `Try` and `Lazy` in detail.
- [Validation](validation.md): checks that report every error at once.
- [Collections](collections/index.md): which collection to choose, and what each operation costs.
