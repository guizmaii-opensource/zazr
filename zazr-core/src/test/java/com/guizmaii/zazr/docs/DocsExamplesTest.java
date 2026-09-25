package com.guizmaii.zazr.docs;

import com.guizmaii.zazr.Lazy;
import com.guizmaii.zazr.Tuple0;
import com.guizmaii.zazr.collection.HashMap;
import com.guizmaii.zazr.collection.HashSet;
import com.guizmaii.zazr.collection.LinkedHashSet;
import com.guizmaii.zazr.collection.List;
import com.guizmaii.zazr.collection.List.Cons;
import com.guizmaii.zazr.collection.List.Nil;
import com.guizmaii.zazr.collection.NonEmptyVector;
import com.guizmaii.zazr.collection.Queue;
import com.guizmaii.zazr.collection.Stream;
import com.guizmaii.zazr.collection.TreeMap;
import com.guizmaii.zazr.collection.TreeSet;
import com.guizmaii.zazr.collection.Vector;
import com.guizmaii.zazr.control.Either;
import com.guizmaii.zazr.control.Either.Left;
import com.guizmaii.zazr.control.Either.Right;
import com.guizmaii.zazr.control.Option;
import com.guizmaii.zazr.control.Option.None;
import com.guizmaii.zazr.control.Option.Some;
import com.guizmaii.zazr.control.Try;
import com.guizmaii.zazr.control.Try.Failure;
import com.guizmaii.zazr.control.Try.Success;
import com.guizmaii.zazr.control.Using;
import com.guizmaii.zazr.control.Validation;
import com.guizmaii.zazr.control.Validation.Invalid;
import com.guizmaii.zazr.control.Validation.Valid;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Every fenced {@code java} block of the website (docs/, except docs/design.md) is pasted here verbatim, compiled
 * and run, one test method per page section, with assertions on what the snippet computes. {@code make docs-examples}
 * fails when a block of the site is missing from this file (or from zazr-test's {@code DocsTestingExamplesTest}, for
 * the page that needs zazr-test). Edit a snippet here and on its page together.
 */
public class DocsExamplesTest {

    // -- the checks of docs/control/validation.md, used by the landing page too

    static Validation<String, String> name(String value) {
        return value.isBlank() ? Validation.invalid("name is blank") : Validation.valid(value);
    }

    static Validation<String, Integer> age(int value) {
        return Validation.fromPredicate(value, v -> v >= 0, v -> "age is negative");
    }

    static Validation<String, String> email(String value) {
        return Validation.fromPredicate(value, v -> v.contains("@"), v -> "email has no @");
    }

    record User(String name, int age, String email) {}

    @Nested
    class Home {

        @Test
        void hero() {
            // one call per arity, never a Tuple2<Tuple2<A, B>, C>
            var sum = Option.zipWith(Option.some(1), Option.some(2), Option.some(3),
                (a, b, c) -> a + b + c); // Option<Integer>

            // total on a collection that cannot be empty
            var best = NonEmptyVector.of(7, 3, 9).max(Integer::compare); // Integer
            // sum is Some(6), best is 9

            assertThat(sum).isEqualTo(Option.some(6));
            assertThat(best).isEqualTo(9);
        }

        @Test
        void shortTour() {
            // Validation keeps every error, not the first one
            var user = Validation.zipWith(name(""), age(-1), email("jules"), User::new); // Validation<String, User>
            var message = switch (user) {
                case Valid(var u) -> "hello " + u.name();
                case Invalid(var errors) -> errors.mkString(", ");
            };
            // "name is blank, age is negative, email has no @"

            // zip at any arity up to 8, no Tuple2<Tuple2<A, B>, C>
            var sum = Option.zipWith(Option.some(1), Option.some(2), Option.some(3),
                (a, b, c) -> a + b + c); // Option<Integer>

            // total operations on a collection that cannot be empty
            var scores = NonEmptyVector.of(7, 3, 9);
            var best = scores.max(Integer::compare); // Integer

            // a builder instead of repeated append
            var builder = Vector.<Integer>newBuilder();
            for (int i = 0; i < 1_000; i++) {
                builder.add(i);
            }
            var numbers = builder.result();

            assertThat(message).isEqualTo("name is blank, age is negative, email has no @");
            assertThat(sum).isEqualTo(Option.some(6));
            assertThat(best).isEqualTo(9);
            assertThat(numbers).isEqualTo(Vector.range(0, 1_000));
        }
    }

    @Nested
    class NewToFpPage {

        record Customer(String name, String email) {}

        // throws two kinds of exception, and only this comment says so
        static int parseQuantityOrThrow(String input) {
            var quantity = Integer.parseInt(input.trim());
            if (quantity <= 0) {
                throw new IllegalArgumentException("quantity must be positive");
            }
            return quantity;
        }

        static Either<String, Integer> parseQuantity(String input) {
            return Try.of(() -> Integer.parseInt(input.trim()))
                .toEither()
                .mapLeft(e -> "not a number: " + input)
                .flatMap(q -> q > 0 ? Either.right(q) : Either.left("quantity must be positive"));
        }

        record Person(String name, String email, int age, String password) {}

        static Validation<String, String> checkName(String name) {
            return Validation.fromPredicate(name.trim(), n -> !n.isEmpty(), n -> "name is required");
        }

        static Validation<String, String> checkEmail(String email) {
            return Validation.fromPredicate(email.trim(), e -> e.contains("@"), e -> "email has no @");
        }

        static Validation<String, Integer> checkAge(int age) {
            return Validation.fromPredicate(age, a -> a >= 18, a -> "you must be 18 or older");
        }

        static Validation<String, String> checkPassword(String password) {
            return Validation.fromPredicate(password, p -> p.length() >= 12, p -> "password is too short");
        }

        static boolean isValidEmail(String input) {
            return input.contains("@");
        }

        static Either<String, NonEmptyVector<String>> recipients(Vector<String> input) {
            return input.toNonEmptyVector().toEither(() -> "at least one recipient is required");
        }

        class ConnectionState {
            boolean connected;
            String sessionId;   // set when connected, hopefully
            String error;       // set when it failed, hopefully
        }

        sealed interface Connection {}
        record Connecting() implements Connection {}
        record Connected(String sessionId) implements Connection {}
        record Failed(String error) implements Connection {}

        static String describe(Connection connection) {
            return switch (connection) {
                case Connecting() -> "connecting...";
                case Connected(var sessionId) -> "connected, session " + sessionId;
                case Failed(var error) -> "failed: " + error;
            };
        }

        @Test
        void nullCrashesFarFromItsCause() {
            assertThatThrownBy(() -> {
                var customers = java.util.Map.of("c-1", new Customer("Ada", "ada@example.com"));
                var customer = customers.get("c-2");        // Customer, yet it is null
                var greeting = "Hello " + customer.name();  // NullPointerException
            }).isInstanceOf(NullPointerException.class);
        }

        @Test
        void optionInsteadOfNull() {
            var customers = HashMap.of("c-1", new Customer("Ada", "ada@example.com"));
            var customer = customers.get("c-2"); // Option<Customer>
            var greeting = customer.map(c -> "Hello " + c.name()).getOrElse("Hello, guest");
            // "Hello, guest"

            var domain = customers.get("c-1") // Option<String>
                .map(Customer::email)
                .flatMap(email -> Option.when(email.contains("@"), () -> email.split("@")[1]));
            // Some(example.com)

            var message = switch (customers.get("c-1")) {
                case Some(var c) -> "Welcome back, " + c.name();
                case None() -> "Please sign up";
            };
            // "Welcome back, Ada"

            assertThat(greeting).isEqualTo("Hello, guest");
            assertThat(domain).isEqualTo(Option.some("example.com"));
            assertThat(message).isEqualTo("Welcome back, Ada");
        }

        @Test
        void eitherInsteadOfThrowing() {
            assertThat(parseQuantityOrThrow(" 3 ")).isEqualTo(3);
            assertThatThrownBy(() -> parseQuantityOrThrow("0")).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> parseQuantityOrThrow("three")).isInstanceOf(NumberFormatException.class);

            var reply = switch (parseQuantity("0")) {
                case Right(var quantity) -> "added " + quantity + " to the cart";
                case Left(var error) -> error;
            };
            // "quantity must be positive"

            var totalInCents = parseQuantity("3").map(q -> q * 1_250);  // Either<String, Integer>
            var rejected = parseQuantity("three").map(q -> q * 1_250);  // Either<String, Integer>
            // Right(3750), Left(not a number: three)

            var deliveryDate = Try.of(() -> LocalDate.parse("2026-02-30")); // Try<LocalDate>
            // Failure(java.time.format.DateTimeParseException: ...)

            assertThat(reply).isEqualTo("quantity must be positive");
            assertThat(totalInCents).isEqualTo(Either.right(3750));
            assertThat(rejected).isEqualTo(Either.left("not a number: three"));
            assertThat(deliveryDate.isFailure()).isTrue();
            assertThat(deliveryDate).isInstanceOfSatisfying(Failure.class,
                failure -> assertThat(failure.cause()).isInstanceOf(DateTimeParseException.class));
        }

        @Test
        void validationReportsEveryError() {
            var person = Validation.zipWith( // Validation<String, Person>
                checkName(""), checkEmail("ada.example.com"), checkAge(16), checkPassword("hunter2"),
                Person::new);

            var response = switch (person) {
                case Valid(var p) -> "Welcome, " + p.name();
                case Invalid(var errors) -> "Fix: " + errors.mkString("; ");
            };
            // "Fix: name is required; email has no @; you must be 18 or older; password is too short"

            assertThat(response).isEqualTo(
                "Fix: name is required; email has no @; you must be 18 or older; password is too short");
            assertThat(Validation.zipWith(
                checkName(" Ada "), checkEmail("ada@example.com"), checkAge(36), checkPassword("correct horse battery"),
                Person::new)).isEqualTo(Validation.valid(new Person("Ada", "ada@example.com", 36, "correct horse battery")));
        }

        @Test
        void parseDontValidate() {
            assertThat(isValidEmail("ada@example.com")).isTrue();
            assertThat(isValidEmail("ada.example.com")).isFalse();
            assertThat(Email.parse(" ada@example.com ").map(Email::value)).isEqualTo(Validation.valid("ada@example.com"));
            assertThat(Email.parse("ada.example.com").map(Email::value)).isEqualTo(Validation.invalid("email has no @"));

            var none = recipients(Vector.empty());          // Either<String, NonEmptyVector<String>>
            var some = recipients(Vector.of("ada@shop.com")); // Either<String, NonEmptyVector<String>>
            var first = some.map(NonEmptyVector::head).getOrElse("nobody");
            // none is Left(at least one recipient is required)
            // some is Right(NonEmptyVector(ada@shop.com))
            // first is "ada@shop.com"

            assertThat(none).isEqualTo(Either.left("at least one recipient is required"));
            assertThat(some).isEqualTo(Either.right(NonEmptyVector.of("ada@shop.com")));
            assertThat(first).isEqualTo("ada@shop.com");
        }

        @Test
        void invalidStatesCannotBeRepresented() {
            ConnectionState state = new ConnectionState();
            state.connected = true;
            state.error = "timeout";
            assertThat(state.connected && state.error != null).isTrue();

            assertThat(describe(new Connecting())).isEqualTo("connecting...");
            assertThat(describe(new Connected("s-1"))).isEqualTo("connected, session s-1");
            assertThat(describe(new Failed("timeout"))).isEqualTo("failed: timeout");
        }
    }

    @Nested
    class Design {

        @Test
        void builders() {
            var squares = Vector.<Integer>newBuilder();
            for (int i = 1; i <= 5; i++) {
                squares.add(i * i);
            }
            var result = squares.result();  // Vector(1, 4, 9, 16, 25)

            assertThat(result).isEqualTo(Vector.of(1, 4, 9, 16, 25));
            assertThatThrownBy(() -> squares.add(36)).isInstanceOf(IllegalStateException.class);
        }

        @Test
        void zipInsteadOfAp() {
            var total = Option.zipWith(Option.some(1), Option.some(2), Option.some(3),
                (a, b, c) -> a + b + c); // Option<Integer>
            // Some(6), and None as soon as one of them is None

            assertThat(total).isEqualTo(Option.some(6));
            assertThat(Option.zipWith(Option.some(1), Option.<Integer> none(), Option.some(3), (a, b, c) -> a + b + c)).isEqualTo(Option.none());
        }

        @Test
        void validationKeepsEveryError() {
            var age = Validation.<String, Integer>invalid("age is negative");
            var email = Validation.<String, String>invalid("email has no @");
            var both = age.zipWith(email, (a, e) -> a + e); // Validation<String, String>
            // Invalid(age is negative, email has no @)

            assertThat(both.isInvalid()).isTrue();
            assertThat(((Invalid<String, String>) both).errors()).isEqualTo(NonEmptyVector.of("age is negative", "email has no @"));
        }

        @Test
        void nonEmptyTypes() {
            var scores = NonEmptyVector.of(7, 3, 9);
            var best = scores.max(Integer::compare);    // Integer: 9, nothing can go wrong
            var passed = scores.filter(s -> s > 5);     // Vector<Integer>: may be empty, so a Vector

            assertThat(best).isEqualTo(9);
            assertThat(passed).isEqualTo(Vector.of(7, 9));
        }

        @Test
        void noNullInside() {
            var name = Option.ofNullable(System.getenv("NO_SUCH_VARIABLE")); // Option<String>
            // None: Option.some(null) and Vector.of(1, null) throw instead

            assertThat(name).isEqualTo(Option.none());
            assertThatThrownBy(() -> Option.some(null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> Vector.of(1, null)).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class GettingStarted {

        @Test
        void fiveMinutes() {
            var name = Option.ofNullable(System.getenv("ZAZR_DOCS_UNSET")); // Option<String>
            var greeting = name.map(n -> "hello " + n).getOrElse("hello stranger");
            // "hello stranger"

            var parsed = Either.<String, Integer>right(42);
            var text = switch (parsed) {
                case Right(var n) -> "got " + n;
                case Left(var error) -> "failed: " + error;
            };

            var port = Try.of(() -> Integer.parseInt("80a")); // Try<Integer>
            var value = port.catchAll(error -> 8080).get();
            // 8080

            var numbers = Vector.of(1, 2, 3, 4);
            var doubled = numbers.map(n -> n * 2).append(10); // Vector<Integer>
            var third = doubled.get(2);
            // doubled is Vector(2, 4, 6, 8, 10), third is 6

            assertThat(greeting).isEqualTo("hello stranger");
            assertThat(text).isEqualTo("got 42");
            assertThat(value).isEqualTo(8080);
            assertThat(doubled).hasToString("Vector(2, 4, 6, 8, 10)");
            assertThat(third).isEqualTo(6);
        }
    }

    @Nested
    class ControlOverview {

        @Test
        void membersTheyShare() {
            var all = Option.collectAll(Vector.of(Option.some(1), Option.some(2))); // Option<Vector<Integer>>
            var parsed = Either.forEach(Vector.of("1", "x", "3"), // Either<String, Vector<Integer>>
                s -> s.chars().allMatch(Character::isDigit) ? Either.right(Integer.parseInt(s)) : Either.left("bad: " + s));
            // Some(Vector(1, 2)), Left(bad: x)

            var flat = Option.flatten(Option.some(Option.some(1))); // Option<Integer>
            var inner = Either.flatten(Either.right(Either.<String, Integer>left("inner failure")));
            // Some(1), Left(inner failure)

            assertThat(all).hasToString("Some(Vector(1, 2))");
            assertThat(parsed).hasToString("Left(bad: x)");
            assertThat(flat).hasToString("Some(1)");
            assertThat(inner).hasToString("Left(inner failure)");
        }

        @Test
        void conversions() {
            var fromOption = Option.<Integer>none().toEither(() -> "missing"); // Either<String, Integer>
            var optional = Option.some(5).toOptional(); // java.util.Optional<Integer>
            var fromTry = Try.of(() -> Integer.parseInt("7")).toOption(); // Option<Integer>
            // Left(missing), Optional[5], Some(7)

            assertThat(fromOption).hasToString("Left(missing)");
            assertThat(optional).hasToString("Optional[5]");
            assertThat(fromTry).hasToString("Some(7)");
        }

        @Test
        void nullPolicy() {
            var absent = Option.<String>ofNullable(null); // Option<String>
            var nullResult = Try.<String>of(() -> null); // Try<String>
            var npe = nullResult.getCause() instanceof NullPointerException;
            // None, Failure(java.lang.NullPointerException: ...), true

            assertThat(absent).isEqualTo(Option.none());
            assertThat(nullResult.isFailure()).isTrue();
            assertThat(npe).isTrue();
            // the claims of the prose around the snippet
            assertThatThrownBy(() -> Option.some(null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> Either.right(null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> Either.left(null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> Try.success(null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> Validation.valid(null)).isInstanceOf(NullPointerException.class);
            assertThat(Lazy.of(() -> null).get()).isNull();
        }
    }

    @Nested
    class OptionPage {

        @Test
        void construction() {
            var some = Option.some(1); // Option<Integer>
            var none = Option.<Integer>none(); // Option<Integer>
            var fromNullable = Option.<String>ofNullable(null); // Option<String>
            var when = Option.when(3 > 2, () -> 3); // Option<Integer>
            // Some(1), None, None, Some(3)

            assertThat(Vector.of(some, none, fromNullable, when)).hasToString("Vector(Some(1), None, None, Some(3))");
        }

        @Test
        void switchOverTheCases() {
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
        void operations() {
            var port = Option.some("8080")
                .filter(s -> s.chars().allMatch(Character::isDigit))
                .map(Integer::parseInt)
                .getOrElse(80); // Integer
            var shown = Option.<Integer>none().fold(() -> "no value", n -> "n = " + n);
            // 8080, "no value"

            var parsed = Option.some("x").mapTry(Integer::parseInt); // Try<Integer>
            var absent = Option.<String>none().mapTry(Integer::parseInt); // Try<Integer>
            // Failure(java.lang.NumberFormatException: For input string: "x"), Failure(java.util.NoSuchElementException: ...)

            assertThat(port).isEqualTo(8080);
            assertThat(shown).isEqualTo("no value");
            assertThat(parsed).hasToString("Failure(java.lang.NumberFormatException: For input string: \"x\")");
            assertThat(absent.getCause()).isInstanceOf(java.util.NoSuchElementException.class);
        }

        @Test
        void conversions() {
            var fromOptional = Option.ofOptional(java.util.Optional.of(3)); // Option<Integer>
            var either = fromOptional.toEither(() -> "missing"); // Either<String, Integer>
            var validation = Option.<Integer>none().toValidation(() -> "missing"); // Validation<String, Integer>
            var back = fromOptional.toOptional(); // java.util.Optional<Integer>
            // Some(3), Right(3), Invalid(missing), Optional[3]

            assertThat(fromOptional).hasToString("Some(3)");
            assertThat(either).hasToString("Right(3)");
            assertThat(validation).hasToString("Invalid(missing)");
            assertThat(back).hasToString("Optional[3]");
        }

        @Test
        void sharpEdges() {
            var env = java.util.Map.of("HOME", "/home/ada"); // java.util.Map<String, String>
            var shell = Option.some("SHELL").flatMap(key -> Option.ofNullable(env.get(key))); // Option<String>
            // None, where map(env::get) would throw

            assertThat(shell).isEqualTo(Option.none());
            assertThatThrownBy(() -> Option.some("SHELL").map(env::get)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> Option.none().get()).isInstanceOf(java.util.NoSuchElementException.class);
        }
    }

    @Nested
    class EitherPage {

        @Test
        void construction() {
            var right = Either.<String, Integer>right(42); // Either<String, Integer>
            var left = Either.<String, Integer>left("not a number"); // Either<String, Integer>
            var checked = Either.fromPredicate(-1, n -> n >= 0, n -> "negative: " + n); // Either<String, Integer>
            // Right(42), Left(not a number), Left(negative: -1)

            assertThat(Vector.of(right, left, checked)).hasToString("Vector(Right(42), Left(not a number), Left(negative: -1))");
        }

        @Test
        void switchOverTheCases() {
            var result = Either.<String, Integer>right(42);
            var text = switch (result) {
                case Right(var value) -> "got " + value;
                case Left(var error) -> "failed: " + error;
            };
            // "got 42"

            assertThat(text).isEqualTo("got 42");
        }

        @Test
        void operations() {
            var total = Either.<String, Integer>right(2)
                .flatMap(n -> n > 0 ? Either.right(n * 10) : Either.left("not positive"))
                .mapLeft(error -> "rejected: " + error); // Either<String, Integer>
            // Right(20)

            var adult = Either.<String, Integer>right(15)
                .filterOrElse(n -> n >= 18, n -> n + " is under 18"); // Either<String, Integer>
            var message = adult.fold(error -> "rejected: " + error, n -> "accepted: " + n); // String
            var flipped = adult.flip(); // Either<Integer, String>
            // Left(15 is under 18), "rejected: 15 is under 18", Right(15 is under 18)

            assertThat(total).hasToString("Right(20)");
            assertThat(adult).hasToString("Left(15 is under 18)");
            assertThat(message).isEqualTo("rejected: 15 is under 18");
            assertThat(flipped).hasToString("Right(15 is under 18)");
        }

        @Test
        void conversions() {
            var missing = Either.<String, Integer>left("missing");
            var option = missing.toOption(); // Option<Integer>
            var attempt = missing.toTry(IllegalArgumentException::new); // Try<Integer>
            var validation = missing.toValidation(); // Validation<String, Integer>
            // None, Failure(java.lang.IllegalArgumentException: missing), Invalid(missing)

            assertThat(option).hasToString("None");
            assertThat(attempt).hasToString("Failure(java.lang.IllegalArgumentException: missing)");
            assertThat(validation).hasToString("Invalid(missing)");
        }

        @Test
        void sharpEdges() {
            assertThatThrownBy(() -> Either.left("x").get()).isInstanceOf(java.util.NoSuchElementException.class);
            IllegalStateException cause = new IllegalStateException("boom");
            assertThat(Either.<Throwable, Integer>left(cause).toTry(t -> t).getCause()).isSameAs(cause);
        }
    }

    @Nested
    class TryPage {

        @Test
        void construction() {
            var parsed = Try.of(() -> Integer.parseInt("42")); // Try<Integer>
            var failed = Try.of(() -> Integer.parseInt("forty-two")); // Try<Integer>
            var ran = Try.run(() -> Thread.sleep(1)); // Try<Tuple0>
            // Success(42), Failure(java.lang.NumberFormatException: For input string: "forty-two"), Success(())

            assertThat(parsed).hasToString("Success(42)");
            assertThat(failed).hasToString("Failure(java.lang.NumberFormatException: For input string: \"forty-two\")");
            assertThat(ran).hasToString("Success(())");
            assertThat(Try.success(1)).hasToString("Success(1)");
            assertThatThrownBy(() -> Try.of(() -> {
                throw new StackOverflowError();
            })).isInstanceOf(StackOverflowError.class);
        }

        @Test
        void switchOverTheCases() {
            var result = Try.of(() -> Integer.parseInt("x")); // Try<Integer>
            var report = switch (result) {
                case Success(var value) -> "parsed " + value;
                case Failure(var cause) -> "failed: " + cause.getClass().getSimpleName();
            };
            // "failed: NumberFormatException"

            assertThat(report).isEqualTo("failed: NumberFormatException");
        }

        @Test
        void recovering() {
            var port = Try.of(() -> Integer.parseInt("80a"))
                .catchSome(NumberFormatException.class, e -> 8080)
                .map(p -> p + 1); // Try<Integer>
            // Success(8081)

            var recovered = Try.of(() -> Integer.parseInt("x")).catchAll(e -> 0); // Try<Integer>
            var wrapped = Try.<Integer>failure(new java.io.IOException("disk"))
                .mapError(e -> new IllegalStateException("cannot read the configuration", e)); // Try<Integer>
            // Success(0), Failure(java.lang.IllegalStateException: cannot read the configuration)

            assertThat(port).hasToString("Success(8081)");
            assertThat(recovered).hasToString("Success(0)");
            assertThat(wrapped).hasToString("Failure(java.lang.IllegalStateException: cannot read the configuration)");
            assertThat(wrapped.getCause().getCause()).isInstanceOf(java.io.IOException.class);
        }

        @Test
        void chaining() {
            var ratio = Try.of(() -> 10).map(n -> 100 / (n - 10)); // Try<Integer>
            var positive = Try.success(-1)
                .filter(n -> n > 0, n -> new IllegalArgumentException("not positive: " + n)); // Try<Integer>
            // Failure(java.lang.ArithmeticException: / by zero), Failure(java.lang.IllegalArgumentException: not positive: -1)

            var log = new StringBuilder();
            var done = Try.of(() -> 1).ensuring(() -> log.append("closed")); // Try<Integer>
            // Success(1), and log is "closed"

            assertThat(ratio).hasToString("Failure(java.lang.ArithmeticException: / by zero)");
            assertThat(positive).hasToString("Failure(java.lang.IllegalArgumentException: not positive: -1)");
            assertThat(done).hasToString("Success(1)");
            assertThat(log).hasToString("closed");
        }

        @Test
        void conversions() {
            var either = Try.of(() -> Integer.parseInt("7")).toEither(); // Either<Throwable, Integer>
            var future = Try.success(7).toCompletableFuture(); // java.util.concurrent.CompletableFuture<Integer>
            var back = Try.fromCompletableFuture(future); // Try<Integer>
            // Right(7), a completed future, Success(7)

            assertThat(either).hasToString("Right(7)");
            assertThat(future).isCompletedWithValue(7);
            assertThat(back).hasToString("Success(7)");
        }

        @Test
        void sharpEdges() {
            var first = Try.of(() -> Integer.parseInt("x")); // Try<Integer>
            var second = Try.of(() -> Integer.parseInt("x")); // Try<Integer>
            var same = first.equals(second);
            var sameClass = first.getCause().getClass() == second.getCause().getClass();
            // same is false, sameClass is true

            assertThat(same).isFalse();
            assertThat(sameClass).isTrue();
            // the same exception object: equal
            assertThat(Try.failure(first.getCause())).isEqualTo(first);
            // an exception class that defines its own equals is compared with it
            class Timeout extends RuntimeException {
                @Override
                public boolean equals(Object o) {
                    return o instanceof Timeout;
                }

                @Override
                public int hashCode() {
                    return 1;
                }
            }
            assertThat(Try.failure(new Timeout())).isEqualTo(Try.failure(new Timeout()));
            // get() throws the cause itself
            assertThatThrownBy(first::get).isSameAs(first.getCause());
            // a future completed with null is a Failure of a NullPointerException
            assertThat(Try.fromCompletableFuture(java.util.concurrent.CompletableFuture.completedFuture(null)).getCause())
                .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class UsingPage {

        @Test
        void oneResource() {
            var firstLine = Using.of(() -> new BufferedReader(new StringReader("a\nb")), BufferedReader::readLine); // Try<String>
            // Success(a)

            Try<String> typed = firstLine;
            assertThat(typed).hasToString("Success(a)");
        }

        @Test
        void manyResources() {
            var sources = Vector.of("alpha", "beta", "gamma");
            var length = Using.manager(use -> { // Try<Integer>
                var total = 0;
                for (var source : sources) {
                    var reader = use.acquire(new BufferedReader(new StringReader(source)));
                    total += reader.readLine().length();
                }
                return total;
            });
            // Success(14), and the three readers are closed

            Try<Integer> typed = length;
            assertThat(typed).isEqualTo(Try.success(14));
        }

        @Test
        void aValueThatIsNotAutoCloseable() {
            var lock = new ReentrantLock();
            var held = Using.manager(use -> { // Try<Boolean>
                lock.lock();
                use.acquire(lock, ReentrantLock::unlock);
                return lock.isHeldByCurrentThread();
            });
            // Success(true), and the lock is released

            Try<Boolean> typed = held;
            assertThat(typed).isEqualTo(Try.success(true));
            assertThat(lock.isLocked()).isFalse();
        }

        @Test
        void releaseOrder() {
            var log = new StringBuilder();
            var result = Using.manager(use -> { // Try<String>
                use.acquire(() -> log.append("connection closed; "));
                use.acquire(() -> log.append("statement closed; "));
                return "done";
            });
            // Success(done), and log is "statement closed; connection closed; "

            Try<String> typed = result;
            assertThat(typed).hasToString("Success(done)");
            assertThat(log).hasToString("statement closed; connection closed; ");
        }

        @Test
        void whichExceptionSurfaces() {
            AutoCloseable resource = () -> {
                throw new IllegalStateException("close failed");
            };
            var result = Using.<AutoCloseable, String>of(() -> resource, r -> { // Try<String>
                throw new IOException("read failed");
            });
            var suppressed = result.getCause().getSuppressed(); // Throwable[]
            // Failure(java.io.IOException: read failed), and suppressed holds the IllegalStateException

            Try<String> typed = result;
            Throwable[] typedSuppressed = suppressed;
            assertThat(typed).hasToString("Failure(java.io.IOException: read failed)");
            assertThat(typedSuppressed).singleElement().isInstanceOf(IllegalStateException.class);
        }

        @Test
        void theManagerWorksOnlyInsideYourCode() {
            var escaped = new AtomicReference<Using.Manager>();
            var done = Using.manager(use -> { // Try<String>
                escaped.set(use);
                return "done";
            });
            var log = new StringBuilder();
            var late = Try.run(() -> escaped.get().acquire(() -> log.append("released at once")));
            // Failure(java.lang.IllegalStateException: ...), and log is "released at once"

            Try<String> typedDone = done;
            Try<Tuple0> typedLate = late;
            assertThat(typedDone).isEqualTo(Try.success("done"));
            assertThat(typedLate.getCause()).isInstanceOf(IllegalStateException.class);
            assertThat(log).hasToString("released at once");
        }

        @Test
        void nullIsAFailure() {
            assertThat(Using.manager(use -> null).getCause()).isInstanceOf(NullPointerException.class);
            assertThat(Using.<AutoCloseable, String>of(() -> null, r -> "x").getCause())
                .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class LazyPage {

        @Test
        void construction() {
            var answer = Lazy.of(() -> 6 * 7); // Lazy<Integer>
            var before = answer.isEvaluated();
            var value = answer.map(n -> n + 1).get(); // Integer
            var after = answer.isEvaluated();
            // before is false, value is 43, after is true

            assertThat(before).isFalse();
            assertThat(value).isEqualTo(43);
            assertThat(after).isTrue();
        }

        @Test
        void operations() {
            var host = Lazy.of(() -> "localhost"); // Lazy<String>
            var port = Lazy.of(() -> 8080); // Lazy<Integer>
            var address = host.zipWith(port, (h, p) -> h + ":" + p); // Lazy<String>
            var evaluated = host.isEvaluated();
            var value = address.get(); // String
            // evaluated is false, value is "localhost:8080"

            var all = Lazy.collectAll(Vector.of(Lazy.of(() -> 1), Lazy.of(() -> 2))); // Lazy<Vector<Integer>>
            // all.get() is Vector(1, 2)

            assertThat(evaluated).isFalse();
            assertThat(value).isEqualTo("localhost:8080");
            assertThat(all.isEvaluated()).isFalse();
            assertThat(all.get()).isEqualTo(Vector.of(1, 2));
        }

        @Test
        void conversions() {
            var calls = new int[] {0};
            var greeting = Lazy.of(() -> {
                calls[0]++;
                return "hello";
            }); // Lazy<String>
            var supplier = greeting.toSupplier(); // java.util.function.Supplier<String>
            var twice = supplier.get() + supplier.get();
            // twice is "hellohello", calls[0] is 1: computed once

            assertThat(twice).isEqualTo("hellohello");
            assertThat(calls[0]).isEqualTo(1);
            assertThat(greeting.get()).isEqualTo("hello");
            assertThat(calls[0]).isEqualTo(1);
        }

        @Test
        void sharpEdges() {
            var attempts = new java.util.concurrent.atomic.AtomicInteger();
            var flaky = Lazy.of(() -> {
                if (attempts.incrementAndGet() == 1) {
                    throw new IllegalStateException("not yet");
                }
                return "ready";
            }); // Lazy<String>
            var first = Try.of(flaky::get); // Try<String>
            var second = flaky.get(); // String
            // first is Failure(java.lang.IllegalStateException: not yet), second is "ready", attempts is 2

            var unread = Lazy.of(() -> 1); // Lazy<Integer>
            var other = Lazy.of(() -> 1); // Lazy<Integer>
            var shown = unread.toString();
            var equal = unread.equals(other);
            // shown is "Lazy(?)", equal is true, and both are now evaluated

            assertThat(first).hasToString("Failure(java.lang.IllegalStateException: not yet)");
            assertThat(second).isEqualTo("ready");
            assertThat(attempts.get()).isEqualTo(2);
            assertThat(shown).isEqualTo("Lazy(?)");
            assertThat(equal).isTrue();
            assertThat(unread.isEvaluated()).isTrue();
            assertThat(other.isEvaluated()).isTrue();
        }
    }

    @Nested
    class ValidationPage {

        @Test
        void accumulation() {
            record User(String name, int age, String email) {}

            var user = Validation.zipWith(name(""), age(-1), email("jules"), User::new); // Validation<String, User>
            // Invalid(name is blank, age is negative, email has no @)

            var pair = name("Ada").zip(age(36)); // Validation<String, Tuple2<String, Integer>>
            var both = name("").zipWith(age(-1), (n, a) -> n + a); // Validation<String, String>
            // Valid((Ada, 36)), Invalid(name is blank, age is negative)

            assertThat(user).hasToString("Invalid(name is blank, age is negative, email has no @)");
            assertThat(pair).hasToString("Valid((Ada, 36))");
            assertThat(both).hasToString("Invalid(name is blank, age is negative)");
            assertThat(Validation.<String, Integer>invalid("a").zip(Either.<String, Integer>left("b")))
                .hasToString("Invalid(a, b)");
        }

        @Test
        void readingTheResult() {
            var checked = age(-5); // Validation<String, Integer>
            var message = switch (checked) {
                case Valid(var years) -> "age " + years;
                case Invalid(var errors) -> errors.size() + " error(s): " + errors.mkString("; ");
            };
            // "1 error(s): age is negative"

            assertThat(message).isEqualTo("1 error(s): age is negative");
        }

        @Test
        void manyValues() {
            var ages = Validation.forEach(Vector.of(3, -1, 7, -2),
                n -> age(n)); // Validation<String, Vector<Integer>>
            // Invalid(age is negative, age is negative)

            var names = Validation.collectAll(
                Vector.of(name("Ada"), name("Grace"))); // Validation<String, Vector<String>>
            // Valid(Vector(Ada, Grace))

            var scores = Validation.forEach(NonEmptyVector.of(1, 2),
                n -> age(n)); // Validation<String, NonEmptyVector<Integer>>
            // Valid(NonEmptyVector(1, 2))

            var split = Validation.partition(Vector.of(4, -1, 9),
                n -> age(n)); // Tuple2<Vector<String>, Vector<Integer>>
            // (Vector(age is negative), Vector(4, 9))

            assertThat(ages).hasToString("Invalid(age is negative, age is negative)");
            assertThat(names).hasToString("Valid(Vector(Ada, Grace))");
            assertThat(scores).hasToString("Valid(NonEmptyVector(1, 2))");
            assertThat(split).hasToString("(Vector(age is negative), Vector(4, 9))");
        }

        @Test
        void shortCircuiting() {
            var adult = Validation.zipWith(name("Ada"), age(15), email("ada@example.com"), User::new)
                .flatMapEither(u -> u.age() >= 18 ? Either.right(u) : Either.left(u.name() + " is under 18"));
            // Invalid(Ada is under 18)

            assertThat(adult).hasToString("Invalid(Ada is under 18)");
            assertThat(Validation.<String, Integer>invalid("a").orElse(() -> Validation.invalid("b")))
                .hasToString("Invalid(b)");
        }

        @Test
        void checks() {
            var port = Validation.of(() -> Integer.parseInt("80a"),
                e -> "port is not a number"); // Validation<String, Integer>
            // Invalid(port is not a number)

            assertThat(port).hasToString("Invalid(port is not a number)");
        }

        @Test
        void conversions() {
            var negative = age(-3); // Validation<String, Integer>
            var joined = negative.toEitherWith(errors -> errors.mkString("; ")); // Either<String, Integer>
            var failure = negative.toTry(
                errors -> new IllegalArgumentException(errors.mkString("; "))); // Try<Integer>
            // Left(age is negative), Failure(java.lang.IllegalArgumentException: age is negative)

            assertThat(joined).hasToString("Left(age is negative)");
            assertThat(failure).hasToString("Failure(java.lang.IllegalArgumentException: age is negative)");
        }

        @Test
        void sharpEdges() {
            Validation<String, Integer> ab = Validation.invalidAll(NonEmptyVector.of("a", "b"));
            Validation<String, Integer> ba = Validation.invalidAll(NonEmptyVector.of("b", "a"));
            assertThat(ab).isNotEqualTo(ba);
            assertThatThrownBy(() -> Validation.invalid(null)).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class ZipPage {

        @Test
        void arity() {
            var sum = Option.zipWith(Option.some(1), Option.some(2), Option.some(3),
                (a, b, c) -> a + b + c); // Option<Integer>
            var triple = Option.zip(Option.some(1), Option.some("a"),
                Option.some(true)); // Option<Tuple3<Integer, String, Boolean>>
            // Some(6), Some((1, a, true))

            assertThat(sum).hasToString("Some(6)");
            assertThat(triple).hasToString("Some((1, a, true))");
        }

        @Test
        void onFailure() {
            var first = Either.zipWith(Either.left("no a"), Either.<String, Integer>right(2),
                Either.<String, Integer>left("no c"), (a, b, c) -> 0); // Either<String, Integer>
            var all = Validation.zipWith(Validation.<String, Integer>invalid("no a"),
                Validation.<String, Integer>valid(2), Validation.<String, Integer>invalid("no c"),
                (a, b, c) -> 0); // Validation<String, Integer>
            // Left(no a), Invalid(no a, no c)

            var left = Option.some(1).zipLeft(Option.none()); // Option<Integer>
            var right = Option.some(1).zipRight(Option.some("kept")); // Option<String>
            // None, Some(kept)

            assertThat(first).hasToString("Left(no a)");
            assertThat(all).hasToString("Invalid(no a, no c)");
            assertThat(left).hasToString("None");
            assertThat(right).hasToString("Some(kept)");
        }
    }

    @Nested
    class NonEmptyVectorPage {

        @Test
        void totalOperations() {
            var scores = NonEmptyVector.of(7, 3, 9);
            var best = scores.max(Integer::compare); // Integer
            var total = scores.reduce(Integer::sum); // Integer
            var first = scores.head(); // Integer
            // 9, 19, 7

            assertThat(best).isEqualTo(9);
            assertThat(total).isEqualTo(19);
            assertThat(first).isEqualTo(7);
        }

        @Test
        void returnTypeContract() {
            var grown = NonEmptyVector.of(1).appendAll(Vector.empty()); // NonEmptyVector<Integer>
            var evens = NonEmptyVector.of(1, 2, 3).filter(n -> n % 2 == 0); // Vector<Integer>
            var rest = NonEmptyVector.of(1).tailNonEmpty(); // Option<NonEmptyVector<Integer>>
            // NonEmptyVector(1), Vector(2), None

            assertThat(grown).hasToString("NonEmptyVector(1)");
            assertThat(evens).hasToString("Vector(2)");
            assertThat(rest).hasToString("None");
        }

        @Test
        void splitsWindowsAndTotalAggregates() {
            var xs = NonEmptyVector.of(1, 2, 3, 4);
            var halves = xs.splitAt(2); // Tuple2<Vector<Integer>, Vector<Integer>>
            var windows = xs.sliding(3); // Vector<NonEmptyVector<Integer>>
            var mean = xs.average(); // double
            // (Vector(1, 2), Vector(3, 4)), Vector(NonEmptyVector(1, 2, 3), NonEmptyVector(2, 3, 4)), 2.5

            assertThat(halves).hasToString("(Vector(1, 2), Vector(3, 4))");
            assertThat(windows).hasToString("Vector(NonEmptyVector(1, 2, 3), NonEmptyVector(2, 3, 4))");
            assertThat(mean).isEqualTo(2.5);
        }

        @Test
        void construction() {
            var fromInput = Vector.of("a", "b").toNonEmptyVector(); // Option<NonEmptyVector<String>>
            var fromNothing = Vector.<String>empty().toNonEmptyVector(); // Option<NonEmptyVector<String>>
            // Some(NonEmptyVector(a, b)), None

            assertThat(fromInput).hasToString("Some(NonEmptyVector(a, b))");
            assertThat(fromNothing).hasToString("None");
            assertThat(NonEmptyVector.of(1, 2)).isNotEqualTo(Vector.of(1, 2));
            assertThatThrownBy(() -> NonEmptyVector.unsafeFromVector(Vector.empty()))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class CollectionsOverview {

        @Test
        void whatTheyShare() {
            var vector = Vector.of(3, 1, 2);
            var sortedList = vector.toList().sorted(); // List<Integer>
            var set = HashSet.ofAll(vector); // HashSet<Integer>
            var same = Vector.of(1, 2, 3).equals(sortedList);
            // List(1, 2, 3), a HashSet of 1, 2, 3, and true

            var split = List.of(1, 2, 3, 4) // Tuple2<List<Integer>, List<String>>
                .partitionMap(n -> n % 2 == 0 ? Either.left(n) : Either.right("odd " + n));
            var flat = Vector.flatten(Vector.of(Vector.of(1, 2), List.of(3))); // Vector<Integer>
            // split is (List(2, 4), List(odd 1, odd 3)), flat is Vector(1, 2, 3)

            assertThat(split).hasToString("(List(2, 4), List(odd 1, odd 3))");
            assertThat(flat).hasToString("Vector(1, 2, 3)");
            assertThat(sortedList).hasToString("List(1, 2, 3)");
            assertThat(set).isEqualTo(HashSet.of(1, 2, 3));
            assertThat(same).isTrue();
        }

        @Test
        void nulls() {
            var missing = HashMap.of("a", 1).get("b"); // Option<Integer>
            var firstEven = Vector.of(1, 3, 4).find(n -> n % 2 == 0); // Option<Integer>
            // None, Some(4)

            assertThat(missing).hasToString("None");
            assertThat(firstEven).hasToString("Some(4)");
            assertThatThrownBy(() -> Vector.of(1).append(null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> HashMap.of("a", 1).put("b", null)).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class VectorPage {

        @Test
        void whenToChooseIt() {
            var letters = Vector.of("a", "b", "c", "d");
            var changed = letters.update(1, "B").prepend("z").drop(2); // Vector<String>
            var halves = letters.splitAt(2); // Tuple2<Vector<String>, Vector<String>>
            // changed is Vector(B, c, d), halves is (Vector(a, b), Vector(c, d))

            var numbers = Vector.range(0, 10); // Vector<Integer>
            var windows = numbers.sliding(3, 3); // Vector<Vector<Integer>>
            var parts = numbers.partitionMap( // Tuple2<Vector<Integer>, Vector<String>>
                n -> n % 2 == 0 ? Either.left(n) : Either.right("odd " + n));
            // windows is Vector(Vector(0, 1, 2), Vector(3, 4, 5), Vector(6, 7, 8), Vector(9))

            assertThat(changed).hasToString("Vector(B, c, d)");
            assertThat(halves).hasToString("(Vector(a, b), Vector(c, d))");
            assertThat(windows).hasToString("Vector(Vector(0, 1, 2), Vector(3, 4, 5), Vector(6, 7, 8), Vector(9))");
            assertThat(parts._1()).isEqualTo(Vector.of(0, 2, 4, 6, 8));
            assertThat(parts._2()).hasSize(5);
        }
    }

    @Nested
    class ListPage {

        @Test
        void whenToChooseIt() {
            var list = List.of(1, 2, 3);
            var first = switch (list) {
                case Cons(var head, var tail) -> "head " + head + ", then " + tail.length() + " more";
                case Nil() -> "empty";
            };
            // "head 1, then 2 more"

            var stack = List.<String>empty().push("a").push("b"); // List<String>
            var top = stack.peek(); // String
            var popped = stack.pop(); // List<String>
            // top is "b", popped is List(a)

            assertThat(first).isEqualTo("head 1, then 2 more");
            assertThat(top).isEqualTo("b");
            assertThat(popped).hasToString("List(a)");
        }
    }

    @Nested
    class QueuePage {

        @Test
        void whenToChooseIt() {
            var queue = Queue.of("a", "b").enqueue("c"); // Queue<String>
            var next = queue.dequeue(); // Tuple2<String, Queue<String>>
            // next is (a, Queue(b, c))

            var work = Queue.of(1);
            var visited = 0;
            while (!work.isEmpty() && visited < 5) {
                var step = work.dequeue(); // Tuple2<Integer, Queue<Integer>>
                work = step._2().enqueue(step._1() * 2, step._1() * 2 + 1);
                visited++;
            }
            // visited is 5, work is Queue(6, 7, 8, 9, 10, 11)

            assertThat(next).hasToString("(a, Queue(b, c))");
            assertThat(visited).isEqualTo(5);
            assertThat(work).hasToString("Queue(6, 7, 8, 9, 10, 11)");
            assertThatThrownBy(() -> Queue.empty().dequeue()).isInstanceOf(java.util.NoSuchElementException.class);
            assertThat(Queue.empty().dequeueOption()).isEqualTo(Option.none());
        }
    }

    @Nested
    class StreamPage {

        @Test
        void whenToChooseIt() {
            var naturals = Stream.from(1); // Stream<Integer>
            var squares = naturals.map(n -> n * n).filter(n -> n % 2 == 1).take(4).toVector();
            // Vector(1, 9, 25, 49)

            var fibonacci = Stream.of(0L, 1L).appendSelf(self -> self.zipWith(self.tail(), Long::sum));
            var firstTen = fibonacci.take(10).toVector(); // Vector<Long>
            // Vector(0, 1, 1, 2, 3, 5, 8, 13, 21, 34)

            assertThat(squares).hasToString("Vector(1, 9, 25, 49)");
            assertThat(firstTen).hasToString("Vector(0, 1, 1, 2, 3, 5, 8, 13, 21, 34)");
        }
    }

    @Nested
    class SetsPage {

        @Test
        void whenToChooseWhich() {
            var tags = HashSet.of("java", "scala");
            var more = tags.add("zio").remove("scala"); // HashSet<String>
            var common = tags.intersect(HashSet.of("scala", "kotlin")); // HashSet<String>
            // more contains java and zio, common is HashSet(scala)

            var seen = LinkedHashSet.of("b", "a").add("c").add("a"); // LinkedHashSet<String>
            var sorted = TreeSet.of(5, 1, 4, 2);
            var smallest = sorted.head(); // Integer
            var firstTwo = sorted.take(2); // TreeSet<Integer>
            // seen is LinkedHashSet(b, a, c), smallest is 1, firstTwo is TreeSet(1, 2)

            assertThat(more).isEqualTo(HashSet.of("java", "zio"));
            assertThat(common).hasToString("HashSet(scala)");
            assertThat(seen).hasToString("LinkedHashSet(b, a, c)");
            assertThat(smallest).isEqualTo(1);
            assertThat(firstTwo).hasToString("TreeSet(1, 2)");
            // min() uses the natural order, whatever the TreeSet's comparator
            TreeSet<Integer> reversed = TreeSet.ofAll(java.util.Comparator.reverseOrder(), Vector.of(1, 2, 3));
            assertThat(reversed.head()).isEqualTo(3);
            assertThat(reversed.min()).isEqualTo(Option.some(1));
        }
    }

    @Nested
    class MapsPage {

        @Test
        void whenToChooseWhich() {
            var stock = HashMap.of("apple", 3, "pear", 0);
            var restocked = stock.put("pear", 5, Integer::sum).put("fig", 1); // HashMap<String, Integer>
            var pears = restocked.get("pear"); // Option<Integer>
            var kiwis = restocked.getOrElse("kiwi", 0); // Integer
            // pears is Some(5), kiwis is 0

            var byName = TreeMap.of("b", 2, "a", 1, "c", 3);
            var values = byName.values(); // Vector<Integer>
            var first = byName.head(); // Tuple2<String, Integer>
            // values is Vector(1, 2, 3), first is (a, 1)

            assertThat(pears).hasToString("Some(5)");
            assertThat(kiwis).isZero();
            assertThat(values).hasToString("Vector(1, 2, 3)");
            assertThat(first).hasToString("(a, 1)");
        }
    }

    @Nested
    class BuildersPage {

        @Test
        void vectorBuilder() {
            var builder = Vector.<String>newBuilder(); // Vector.Builder<String>
            for (String word : "the quick brown fox".split(" ")) {
                builder.add(word.toUpperCase());
            }
            var words = builder.result(); // Vector<String>
            // Vector(THE, QUICK, BROWN, FOX)

            var both = Vector.<Integer>newBuilder(8); // Vector.Builder<Integer>
            both.addAll(Vector.of(1, 2, 3)).add(4);
            var added = both.size();
            var result = both.result(); // Vector<Integer>
            // added is 4, result is Vector(1, 2, 3, 4)

            assertThat(words).hasToString("Vector(THE, QUICK, BROWN, FOX)");
            assertThat(added).isEqualTo(4);
            assertThat(result).hasToString("Vector(1, 2, 3, 4)");
            assertThatThrownBy(() -> both.add(5)).isInstanceOf(IllegalStateException.class);
        }

        @Test
        void mapAndSetBuilders() {
            var counts = HashMap.<String, Integer>newBuilder(); // HashMap.Builder<String, Integer>
            for (var word : "to be or not to be".split(" ")) {
                counts.put(word, word.length());
            }
            var lengths = counts.result(); // HashMap<String, Integer>
            // HashMap((to, 2), (be, 2), (or, 2), (not, 3)), in some order

            assertThat(lengths).isEqualTo(HashMap.of("to", 2, "be", 2, "or", 2, "not", 3));
        }

        @Test
        void treeSetBuilder() {
            var sorted = TreeSet.newBuilder(java.util.Comparator.<String>reverseOrder())
                .addAll(List.of("pear", "apple", "fig"))
                .result(); // TreeSet<String>
            // TreeSet(pear, fig, apple)

            assertThat(sorted).hasToString("TreeSet(pear, fig, apple)");
        }

        @Test
        void collectors() {
            var lengths = java.util.stream.Stream.of("a", "bb", "ccc").map(String::length)
                .collect(Vector.collector()); // Vector<Integer>
            var sorted = java.util.stream.Stream.of("b", "a", "b").collect(TreeSet.collector()); // TreeSet<String>
            // Vector(1, 2, 3), TreeSet(a, b)

            assertThat(lengths).hasToString("Vector(1, 2, 3)");
            assertThat(sorted).hasToString("TreeSet(a, b)");
        }
    }

    @Nested
    class JavaInteropPage {

        @Test
        void views() {
            var names = Vector.of("Ada", "Grace");
            var view = names.asJava(); // java.util.List<String>
            var second = view.get(1);
            var rejected = Try.run(() -> view.add("Linus")).getCause() instanceof UnsupportedOperationException;
            // second is "Grace", rejected is true

            assertThat(second).isEqualTo("Grace");
            assertThat(rejected).isTrue();
            assertThat(view.reversed()).containsExactly("Grace", "Ada");
            assertThat(HashSet.of(1).asJava()).isEqualTo(java.util.Set.of(1));
            assertThat(Vector.ofAll(names.asJava())).isSameAs(names);
        }

        @Test
        void sortedViews() {
            var scores = TreeSet.of(10, 20, 30, 40).asJava(); // java.util.NavigableSet<Integer>
            var atLeast25 = scores.ceiling(25); // Integer
            var top = scores.tailSet(20, true); // java.util.NavigableSet<Integer>
            var ages = TreeMap.of("Ada", 36, "Grace", 85).asJavaMap(); // java.util.NavigableMap<String, Integer>
            var grace = ages.get("Grace"); // Integer
            // atLeast25 is 30, top is [20, 30, 40], grace is 85

            assertThat(atLeast25).isEqualTo(30);
            assertThat(top).hasToString("[20, 30, 40]");
            assertThat(grace).isEqualTo(85);
        }

        @Test
        void copies() {
            var mutable = new java.util.ArrayList<>(Vector.of(1, 2).asJava()); // java.util.ArrayList<Integer>
            mutable.add(3);
            var jdkSet = new java.util.HashSet<>(HashSet.of("a", "b").asJava()); // java.util.HashSet<String>
            // mutable is [1, 2, 3], jdkSet holds a and b

            assertThat(mutable).hasToString("[1, 2, 3]");
            assertThat(jdkSet).containsExactlyInAnyOrder("a", "b");
        }

        @Test
        void theWayBack() {
            var fromJdk = Vector.ofAll(java.util.List.of(3, 1, 2)); // Vector<Integer>
            var sum = fromJdk.stream().mapToInt(Integer::intValue).sum();
            var fromOptional = Option.ofOptional(java.util.Optional.of(4)); // Option<Integer>
            // Vector(3, 1, 2), 6, Some(4)

            assertThat(fromJdk).hasToString("Vector(3, 1, 2)");
            assertThat(sum).isEqualTo(6);
            assertThat(fromOptional).hasToString("Some(4)");
        }
    }

    @Nested
    class VavrPage {

        @Test
        void movingCodeOver() {
            // Vavr: Match(option).of(Case($Some($()), v -> ...), Case($None(), () -> ...))
            var maybe = Option.some("Zazr");
            var length = switch (maybe) {
                case Some(var value) -> value.length();
                case None() -> 0;
            };
            // 4

            assertThat(length).isEqualTo(4);
        }
    }
}

// the parser of docs/new-to-fp.md: a top-level class, since a static method of an inner class cannot call the
// constructor of an inner class
final class Email {
    private final String value;

    private Email(String value) {
        this.value = value;
    }

    static Validation<String, Email> parse(String input) {
        var trimmed = input.trim();
        return trimmed.contains("@")
            ? Validation.valid(new Email(trimmed))
            : Validation.invalid("email has no @");
    }

    String value() {
        return value;
    }
}
