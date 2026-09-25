package com.guizmaii.zazr.docs;

import com.guizmaii.zazr.Tuple2;
import com.guizmaii.zazr.Tuple3;
import com.guizmaii.zazr.collection.HashMap;
import com.guizmaii.zazr.collection.List;
import com.guizmaii.zazr.collection.NonEmptyVector;
import com.guizmaii.zazr.collection.TreeSet;
import com.guizmaii.zazr.collection.Vector;
import com.guizmaii.zazr.control.Either;
import com.guizmaii.zazr.control.Option;
import com.guizmaii.zazr.control.Option.None;
import com.guizmaii.zazr.control.Option.Some;
import com.guizmaii.zazr.control.Try;
import com.guizmaii.zazr.control.Using;
import com.guizmaii.zazr.control.Validation;
import com.guizmaii.zazr.control.Validation.Invalid;
import com.guizmaii.zazr.control.Validation.Valid;
import java.io.BufferedReader;
import java.io.StringReader;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every fenced {@code java} block of the Agent Skill under skills/zazr (except references/functional-java.md, whose
 * blocks are in {@code SkillFunctionalJavaExamplesTest}) is pasted here verbatim, compiled and run, one nested class
 * per file, with assertions on what the snippet computes. {@code make docs-examples} fails when a block is missing.
 * Edit a snippet here and in the skill together.
 */
public class SkillExamplesTest {

    @Nested
    class SkillMd {

        record Signup(String name, NonEmptyVector<String> emails) {}

        static Validation<String, Signup> signup(String name, Vector<String> emails) {
            return Validation.zipWith(
                Validation.fromPredicate(name.trim(), n -> !n.isEmpty(), n -> "name is required"),
                Validation.fromOption(emails.toNonEmptyVector(), () -> "at least one email is required"),
                Signup::new);
        }

        @Test
        void theTenRules() {
            var reply = switch (signup("", Vector.empty())) { // Validation<String, Signup>
                case Valid(var s) -> "welcome, " + s.name();
                case Invalid(var errors) -> errors.mkString("; ");
            };
            // "name is required; at least one email is required"

            assertThat(reply).isEqualTo("name is required; at least one email is required");
            assertThat(signup(" Ada ", Vector.of("ada@example.com")))
                .isEqualTo(Validation.valid(new Signup("Ada", NonEmptyVector.of("ada@example.com"))));
        }
    }

    @Nested
    class ControlTypes {

        record User(String name, int age) {}

        static Validation<String, String> name(String value) {
            return value.isBlank() ? Validation.invalid("name is blank") : Validation.valid(value);
        }

        static Validation<String, Integer> age(int value) {
            return Validation.fromPredicate(value, v -> v >= 0, v -> "age is negative");
        }

        @Test
        void patternMatching() {
            var age = Option.some(17); // Option<Integer>
            var label = switch (age) {
                case Some(var years) when years >= 18 -> "adult";
                case Some(var years) -> "minor, " + years;
                case None() -> "unknown";
            };
            // "minor, 17"

            assertThat(label).isEqualTo("minor, 17");
        }

        @Test
        void membersTheyShare() {
            var parsed = Either.forEach(Vector.of("1", "x", "3"), // Either<String, Vector<Integer>>
                s -> s.chars().allMatch(Character::isDigit) ? Either.right(Integer.parseInt(s)) : Either.left("bad: " + s));
            var all = Option.collectAll(Vector.of(Option.some(1), Option.some(2))); // Option<Vector<Integer>>
            // Left(bad: x), Some(Vector(1, 2))

            assertThat(parsed).isEqualTo(Either.left("bad: x"));
            assertThat(all).isEqualTo(Option.some(Vector.of(1, 2)));
        }

        @Test
        void option() {
            var env = java.util.Map.of("HOME", "/home/ada");
            var shell = Option.some("SHELL").flatMap(key -> Option.ofNullable(env.get(key))); // Option<String>
            // None, where map(env::get) would throw

            assertThat(shell).isEqualTo(Option.none());
            assertThat(Try.of(() -> Option.some("SHELL").map(env::get)).getCause())
                .isInstanceOf(NullPointerException.class);
        }

        @Test
        void either() {
            var total = Either.<String, Integer>right(2) // Either<String, Integer>
                .flatMap(n -> n > 0 ? Either.right(n * 10) : Either.left("not positive"))
                .filterOrElse(n -> n < 100, n -> n + " is too large")
                .mapLeft(error -> "rejected: " + error);
            var positive = Either.fromPredicate(-1, n -> n > 0, n -> n + " is not positive"); // Either<String, Integer>
            var named = Either.fromPredicate("", s -> !s.isBlank(), _ -> "name is blank"); // Either<String, String>
            // Right(20), Left(-1 is not positive), Left(name is blank)

            assertThat(total).isEqualTo(Either.right(20));
            assertThat(positive).isEqualTo(Either.left("-1 is not positive"));
            assertThat(named).isEqualTo(Either.left("name is blank"));
            assertThat(Either.fromPredicate(3, n -> n > 0, n -> n + " is not positive")).isEqualTo(Either.right(3));
        }

        @Test
        void tryType() {
            var port = Try.of(() -> Integer.parseInt("80a")) // Try<Integer>
                .catchSome(NumberFormatException.class, e -> 8080)
                .map(p -> p + 1);
            var config = Try.<String>failure(new java.io.IOException("disk")) // Try<String>
                .mapError(e -> new IllegalStateException("cannot read the configuration", e));
            // Success(8081), Failure(java.lang.IllegalStateException: cannot read the configuration)

            assertThat(port).isEqualTo(Try.success(8081));
            assertThat(config.getCause())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("cannot read the configuration");
        }

        @Test
        void validation() {
            var user = Validation.zipWith(name(""), age(-1), User::new); // Validation<String, User>
            var adult = Validation.zipWith(name("Ada"), age(15), User::new) // Validation<String, User>
                .flatMapEither(u -> u.age() >= 18 ? Either.right(u) : Either.left(u.name() + " is under 18"));
            var ages = Validation.forEach(Vector.of(3, -1, 7), n -> age(n)); // Validation<String, Vector<Integer>>
            // Invalid(name is blank, age is negative), Invalid(Ada is under 18), Invalid(age is negative)

            assertThat(user).isEqualTo(Validation.invalidAll(NonEmptyVector.of("name is blank", "age is negative")));
            assertThat(adult).isEqualTo(Validation.invalid("Ada is under 18"));
            assertThat(ages).isEqualTo(Validation.invalid("age is negative"));
        }

        @Test
        void using() {
            var sources = Vector.of("alpha", "beta");
            var length = Using.manager(use -> { // Try<Integer>
                var total = 0;
                for (var source : sources) {
                    var reader = use.acquire(new BufferedReader(new StringReader(source)));
                    total += reader.readLine().length();
                }
                return total;
            });
            // Success(9), and both readers are closed

            assertThat(length).isEqualTo(Try.success(9));
        }
    }

    @Nested
    class Collections {

        @Test
        void whatEveryCollectionShares() {
            var split = List.of(1, 2, 3, 4) // Tuple2<List<Integer>, List<String>>
                .partitionMap(n -> n % 2 == 0 ? Either.left(n) : Either.right("odd " + n));
            var stock = HashMap.of("apple", 3, "pear", 0) // HashMap<String, Integer>
                .put("pear", 5, Integer::sum)
                .put("fig", 1);
            var pears = stock.get("pear"); // Option<Integer>
            var kiwis = stock.getOrElse("kiwi", 0); // Integer
            // split is (List(2, 4), List(odd 1, odd 3)), pears is Some(5), kiwis is 0

            assertThat(split).isEqualTo(new Tuple2<>(List.of(2, 4), List.of("odd 1", "odd 3")));
            assertThat(pears).isEqualTo(Option.some(5));
            assertThat(kiwis).isEqualTo(0);
        }

        @Test
        void buildInBulk() {
            var builder = Vector.<String>newBuilder(); // Vector.Builder<String>
            for (var word : "the quick brown fox".split(" ")) {
                builder.add(word.toUpperCase());
            }
            var words = builder.result(); // Vector<String>
            var sorted = java.util.stream.Stream.of("b", "a", "b").collect(TreeSet.collector()); // TreeSet<String>
            // Vector(THE, QUICK, BROWN, FOX), TreeSet(a, b)

            assertThat(words).isEqualTo(Vector.of("THE", "QUICK", "BROWN", "FOX"));
            assertThat(sorted).isEqualTo(TreeSet.of("a", "b"));
        }

        @Test
        void nonEmptyVector() {
            var input = Vector.of("ada@shop.com", "grace@shop.com");
            var recipients = input.toNonEmptyVector().toEither(() -> "at least one recipient is required"); // Either<String, NonEmptyVector<String>>
            var first = recipients.map(NonEmptyVector::head).getOrElse("nobody"); // String
            var scores = NonEmptyVector.of(7, 3, 9);
            var best = scores.max(Integer::compare); // int, nothing can go wrong
            var passed = scores.filter(s -> s > 5); // Vector<Integer>, may be empty
            // first is "ada@shop.com", best is 9, passed is Vector(7, 9)

            assertThat(first).isEqualTo("ada@shop.com");
            assertThat(best).isEqualTo(9);
            assertThat(passed).isEqualTo(Vector.of(7, 9));
        }

        @Test
        void javaInterop() {
            var names = Vector.of("Ada", "Grace");
            var view = names.asJava(); // java.util.List<String>, no copy
            var back = Vector.ofAll(view); // Vector<String>, the same instance
            var fromJdk = Vector.ofAll(java.util.List.of(3, 1, 2)); // Vector<Integer>
            // view.get(1) is "Grace", back == names, fromJdk is Vector(3, 1, 2)

            assertThat(view.get(1)).isEqualTo("Grace");
            assertThat(back).isSameAs(names);
            assertThat(fromJdk).isEqualTo(Vector.of(3, 1, 2));
        }
    }

    @Nested
    class Zip {

        @Test
        void arity() {
            var sum = Option.zipWith(Option.some(1), Option.some(2), Option.some(3), (a, b, c) -> a + b + c); // Option<Integer>
            var triple = Option.zip(Option.some(1), Option.some("a"), Option.some(true)); // Option<Tuple3<Integer, String, Boolean>>
            // Some(6), Some((1, a, true))

            assertThat(sum).isEqualTo(Option.some(6));
            assertThat(triple).isEqualTo(Option.some(new Tuple3<>(1, "a", true)));
        }

        @Test
        void onFailure() {
            var first = Either.zipWith(Either.left("no a"), Either.<String, Integer>right(2), // Either<String, Integer>
                Either.<String, Integer>left("no c"), (a, b, c) -> 0);
            var all = Validation.zipWith(Validation.<String, Integer>invalid("no a"), // Validation<String, Integer>
                Validation.<String, Integer>valid(2), Validation.<String, Integer>invalid("no c"), (a, b, c) -> 0);
            // Left(no a), Invalid(no a, no c)

            assertThat(first).isEqualTo(Either.left("no a"));
            assertThat(all).isEqualTo(Validation.invalidAll(NonEmptyVector.of("no a", "no c")));
            assertThat(Option.some(1).zipLeft(Option.none())).isEqualTo(Option.none());
        }

        @Test
        void tuples() {
            var pair = Option.some(1).zip(Option.some("one")); // Option<Tuple2<Integer, String>>
            var text = switch (pair) {
                case Some(Tuple2(var n, var name)) -> n + " is " + name;
                case None() -> "nothing";
            };
            // "1 is one"

            assertThat(text).isEqualTo("1 is one");
        }
    }
}
