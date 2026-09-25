package com.guizmaii.zazr.docs;

import com.guizmaii.zazr.Lazy;
import com.guizmaii.zazr.Tuple0;
import com.guizmaii.zazr.Tuple2;
import com.guizmaii.zazr.Tuple3;
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
import com.guizmaii.zazr.control.Validation;
import com.guizmaii.zazr.control.Validation.Invalid;
import com.guizmaii.zazr.control.Validation.Valid;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
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
            Option<Integer> sum = Option.zipWith(Option.some(1), Option.some(2), Option.some(3),
                (a, b, c) -> a + b + c);

            // total on a collection that cannot be empty
            int best = NonEmptyVector.of(7, 3, 9).max(Integer::compare);
            // sum is Some(6), best is 9

            assertThat(sum).isEqualTo(Option.some(6));
            assertThat(best).isEqualTo(9);
        }

        @Test
        void shortTour() {
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

            assertThat(message).isEqualTo("name is blank, age is negative, email has no @");
            assertThat(sum).isEqualTo(Option.some(6));
            assertThat(best).isEqualTo(9);
            assertThat(numbers).isEqualTo(Vector.range(0, 1_000));
        }
    }

    @Nested
    class NewToFpPage {

        record Customer(String id, String name, String email) {}

        // throws two kinds of exception, and only this comment says so
        static int parseQuantityOrThrow(String input) {
            int quantity = Integer.parseInt(input.trim());
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
                java.util.Map<String, Customer> customers =
                    java.util.Map.of("c-1", new Customer("c-1", "Ada", "ada@example.com"));
                Customer customer = customers.get("c-2");       // null: nothing in the type says it can be
                String greeting = "Hello " + customer.name();   // NullPointerException
            }).isInstanceOf(NullPointerException.class);
        }

        @Test
        void optionInsteadOfNull() {
            HashMap<String, Customer> customers =
                HashMap.of("c-1", new Customer("c-1", "Ada", "ada@example.com"));
            Option<Customer> customer = customers.get("c-2");
            String greeting = customer.map(c -> "Hello " + c.name()).getOrElse("Hello, guest");
            // "Hello, guest"

            Option<String> domain = customers.get("c-1")
                .map(Customer::email)
                .flatMap(email -> Option.when(email.contains("@"), () -> email.split("@")[1]));
            // Some(example.com)

            String message = switch (customers.get("c-1")) {
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

            String reply = switch (parseQuantity("0")) {
                case Right(var quantity) -> "added " + quantity + " to the cart";
                case Left(var error) -> error;
            };
            // "quantity must be positive"

            Either<String, Integer> totalInCents = parseQuantity("3").map(q -> q * 1_250);
            Either<String, Integer> rejected = parseQuantity("three").map(q -> q * 1_250);
            // Right(3750), Left(not a number: three)

            Try<LocalDate> deliveryDate = Try.of(() -> LocalDate.parse("2026-02-30"));
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
            Validation<String, Person> person = Validation.zipWith(
                checkName(""), checkEmail("ada.example.com"), checkAge(16), checkPassword("hunter2"),
                Person::new);

            String response = switch (person) {
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

            Either<String, NonEmptyVector<String>> none = recipients(Vector.empty());
            Either<String, NonEmptyVector<String>> some = recipients(Vector.of("ada@shop.com", "bob@shop.com"));
            String first = some.map(NonEmptyVector::head).getOrElse("nobody");
            // none is Left(at least one recipient is required)
            // some is Right(NonEmptyVector(ada@shop.com, bob@shop.com))
            // first is "ada@shop.com"

            assertThat(none).isEqualTo(Either.left("at least one recipient is required"));
            assertThat(some).isEqualTo(Either.right(NonEmptyVector.of("ada@shop.com", "bob@shop.com")));
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
            Vector.Builder<Integer> squares = Vector.newBuilder();
            for (int i = 1; i <= 5; i++) {
                squares.add(i * i);
            }
            Vector<Integer> result = squares.result();  // Vector(1, 4, 9, 16, 25)

            assertThat(result).isEqualTo(Vector.of(1, 4, 9, 16, 25));
            assertThatThrownBy(() -> squares.add(36)).isInstanceOf(IllegalStateException.class);
        }

        @Test
        void zipInsteadOfAp() {
            Option<Integer> total = Option.zipWith(Option.some(1), Option.some(2), Option.some(3),
                (a, b, c) -> a + b + c);
            // Some(6), and None as soon as one of them is None

            assertThat(total).isEqualTo(Option.some(6));
            assertThat(Option.zipWith(Option.some(1), Option.<Integer> none(), Option.some(3), (a, b, c) -> a + b + c)).isEqualTo(Option.none());
        }

        @Test
        void validationKeepsEveryError() {
            Validation<String, Integer> age = Validation.invalid("age is negative");
            Validation<String, String> email = Validation.invalid("email has no @");
            Validation<String, String> both = age.zipWith(email, (a, e) -> a + e);
            // Invalid(age is negative, email has no @)

            assertThat(both.isInvalid()).isTrue();
            assertThat(((Invalid<String, String>) both).errors()).isEqualTo(NonEmptyVector.of("age is negative", "email has no @"));
        }

        @Test
        void nonEmptyTypes() {
            NonEmptyVector<Integer> scores = NonEmptyVector.of(7, 3, 9);
            int best = scores.max(Integer::compare);             // 9, nothing can go wrong
            Vector<Integer> passed = scores.filter(s -> s > 5);  // may be empty, so a Vector

            assertThat(best).isEqualTo(9);
            assertThat(passed).isEqualTo(Vector.of(7, 9));
        }

        @Test
        void noNullInside() {
            Option<String> name = Option.ofNullable(System.getenv("NO_SUCH_VARIABLE"));
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
            Option<String> name = Option.ofNullable(System.getenv("ZAZR_DOCS_UNSET"));
            String greeting = name.map(n -> "hello " + n).getOrElse("hello stranger");
            // "hello stranger"

            Either<String, Integer> parsed = Either.right(42);
            String text = switch (parsed) {
                case Right(var n) -> "got " + n;
                case Left(var error) -> "failed: " + error;
            };

            Try<Integer> port = Try.of(() -> Integer.parseInt("80a"));
            int value = port.catchAll(error -> 8080).get();
            // 8080

            Vector<Integer> numbers = Vector.of(1, 2, 3, 4);
            Vector<Integer> doubled = numbers.map(n -> n * 2).append(10);
            int third = doubled.get(2);
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
            Option<Vector<Integer>> all = Option.collectAll(Vector.of(Option.some(1), Option.some(2)));
            Either<String, Vector<Integer>> parsed = Either.forEach(Vector.of("1", "x", "3"),
                s -> s.chars().allMatch(Character::isDigit) ? Either.right(Integer.parseInt(s)) : Either.left("bad: " + s));
            // Some(Vector(1, 2)), Left(bad: x)

            Option<Integer> flat = Option.flatten(Option.some(Option.some(1)));
            Either<String, Integer> inner = Either.flatten(Either.right(Either.left("inner failure")));
            // Some(1), Left(inner failure)

            assertThat(all).hasToString("Some(Vector(1, 2))");
            assertThat(parsed).hasToString("Left(bad: x)");
            assertThat(flat).hasToString("Some(1)");
            assertThat(inner).hasToString("Left(inner failure)");
        }

        @Test
        void conversions() {
            Either<String, Integer> fromOption = Option.<Integer>none().toEither(() -> "missing");
            java.util.Optional<Integer> optional = Option.some(5).toOptional();
            Option<Integer> fromTry = Try.of(() -> Integer.parseInt("7")).toOption();
            // Left(missing), Optional[5], Some(7)

            assertThat(fromOption).hasToString("Left(missing)");
            assertThat(optional).hasToString("Optional[5]");
            assertThat(fromTry).hasToString("Some(7)");
        }

        @Test
        void nullPolicy() {
            Option<String> absent = Option.ofNullable(null);
            Try<String> nullResult = Try.of(() -> null);
            boolean npe = nullResult.getCause() instanceof NullPointerException;
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
            Option<Integer> some = Option.some(1);
            Option<Integer> none = Option.none();
            Option<String> fromNullable = Option.ofNullable(null);
            Option<Integer> when = Option.when(3 > 2, () -> 3);
            // Some(1), None, None, Some(3)

            assertThat(Vector.of(some, none, fromNullable, when)).hasToString("Vector(Some(1), None, None, Some(3))");
        }

        @Test
        void switchOverTheCases() {
            Option<Integer> age = Option.some(17);
            String label = switch (age) {
                case Some(var years) when years >= 18 -> "adult";
                case Some(var years) -> "minor, " + years;
                case None() -> "unknown";
            };
            // "minor, 17"

            assertThat(label).isEqualTo("minor, 17");
        }

        @Test
        void operations() {
            int port = Option.some("8080")
                .filter(s -> s.chars().allMatch(Character::isDigit))
                .map(Integer::parseInt)
                .getOrElse(80);
            String shown = Option.<Integer>none().fold(() -> "no value", n -> "n = " + n);
            // 8080, "no value"

            Try<Integer> parsed = Option.some("x").mapTry(Integer::parseInt);
            Try<Integer> absent = Option.<String>none().mapTry(Integer::parseInt);
            // Failure(java.lang.NumberFormatException: For input string: "x"), Failure(java.util.NoSuchElementException: ...)

            assertThat(port).isEqualTo(8080);
            assertThat(shown).isEqualTo("no value");
            assertThat(parsed).hasToString("Failure(java.lang.NumberFormatException: For input string: \"x\")");
            assertThat(absent.getCause()).isInstanceOf(java.util.NoSuchElementException.class);
        }

        @Test
        void conversions() {
            Option<Integer> fromOptional = Option.ofOptional(java.util.Optional.of(3));
            Either<String, Integer> either = fromOptional.toEither(() -> "missing");
            Validation<String, Integer> validation = Option.<Integer>none().toValidation(() -> "missing");
            java.util.Optional<Integer> back = fromOptional.toOptional();
            // Some(3), Right(3), Invalid(missing), Optional[3]

            assertThat(fromOptional).hasToString("Some(3)");
            assertThat(either).hasToString("Right(3)");
            assertThat(validation).hasToString("Invalid(missing)");
            assertThat(back).hasToString("Optional[3]");
        }

        @Test
        void sharpEdges() {
            java.util.Map<String, String> env = java.util.Map.of("HOME", "/home/ada");
            Option<String> shell = Option.some("SHELL").flatMap(key -> Option.ofNullable(env.get(key)));
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
            Either<String, Integer> right = Either.right(42);
            Either<String, Integer> left = Either.left("not a number");
            Either<String, Integer> checked = Either.fromPredicate(-1, n -> n >= 0, () -> "negative");
            // Right(42), Left(not a number), Left(negative)

            assertThat(Vector.of(right, left, checked)).hasToString("Vector(Right(42), Left(not a number), Left(negative))");
        }

        @Test
        void switchOverTheCases() {
            Either<String, Integer> result = Either.right(42);
            String text = switch (result) {
                case Right(var value) -> "got " + value;
                case Left(var error) -> "failed: " + error;
            };
            // "got 42"

            assertThat(text).isEqualTo("got 42");
        }

        @Test
        void operations() {
            Either<String, Integer> total = Either.<String, Integer>right(2)
                .flatMap(n -> n > 0 ? Either.right(n * 10) : Either.left("not positive"))
                .mapLeft(error -> "rejected: " + error);
            // Right(20)

            Either<String, Integer> adult = Either.<String, Integer>right(15)
                .filterOrElse(n -> n >= 18, n -> n + " is under 18");
            String message = adult.fold(error -> "rejected: " + error, n -> "accepted: " + n);
            Either<Integer, String> flipped = adult.flip();
            // Left(15 is under 18), "rejected: 15 is under 18", Right(15 is under 18)

            assertThat(total).hasToString("Right(20)");
            assertThat(adult).hasToString("Left(15 is under 18)");
            assertThat(message).isEqualTo("rejected: 15 is under 18");
            assertThat(flipped).hasToString("Right(15 is under 18)");
        }

        @Test
        void conversions() {
            Either<String, Integer> missing = Either.left("missing");
            Option<Integer> option = missing.toOption();
            Try<Integer> attempt = missing.toTry(IllegalArgumentException::new);
            Validation<String, Integer> validation = missing.toValidation();
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
            Try<Integer> parsed = Try.of(() -> Integer.parseInt("42"));
            Try<Integer> failed = Try.of(() -> Integer.parseInt("forty-two"));
            Try<Tuple0> ran = Try.run(() -> Thread.sleep(1));
            // Success(42), Failure(java.lang.NumberFormatException: For input string: "forty-two"), Success(())

            Try<String> firstLine = Try.withResources(() -> new java.io.BufferedReader(new java.io.StringReader("a\nb")),
                java.io.BufferedReader::readLine);
            // Success(a)

            assertThat(parsed).hasToString("Success(42)");
            assertThat(failed).hasToString("Failure(java.lang.NumberFormatException: For input string: \"forty-two\")");
            assertThat(ran).hasToString("Success(())");
            assertThat(firstLine).hasToString("Success(a)");
            assertThat(Try.success(1)).hasToString("Success(1)");
            assertThatThrownBy(() -> Try.of(() -> {
                throw new StackOverflowError();
            })).isInstanceOf(StackOverflowError.class);
        }

        @Test
        void switchOverTheCases() {
            Try<Integer> result = Try.of(() -> Integer.parseInt("x"));
            String report = switch (result) {
                case Success(var value) -> "parsed " + value;
                case Failure(var cause) -> "failed: " + cause.getClass().getSimpleName();
            };
            // "failed: NumberFormatException"

            assertThat(report).isEqualTo("failed: NumberFormatException");
        }

        @Test
        void recovering() {
            Try<Integer> port = Try.of(() -> Integer.parseInt("80a"))
                .catchSome(NumberFormatException.class, e -> 8080)
                .map(p -> p + 1);
            // Success(8081)

            Try<Integer> recovered = Try.of(() -> Integer.parseInt("x")).catchAll(e -> 0);
            Try<Integer> wrapped = Try.<Integer>failure(new java.io.IOException("disk"))
                .mapError(e -> new IllegalStateException("cannot read the configuration", e));
            // Success(0), Failure(java.lang.IllegalStateException: cannot read the configuration)

            assertThat(port).hasToString("Success(8081)");
            assertThat(recovered).hasToString("Success(0)");
            assertThat(wrapped).hasToString("Failure(java.lang.IllegalStateException: cannot read the configuration)");
            assertThat(wrapped.getCause().getCause()).isInstanceOf(java.io.IOException.class);
        }

        @Test
        void chaining() {
            Try<Integer> ratio = Try.of(() -> 10).map(n -> 100 / (n - 10));
            Try<Integer> positive = Try.success(-1)
                .filter(n -> n > 0, n -> new IllegalArgumentException("not positive: " + n));
            // Failure(java.lang.ArithmeticException: / by zero), Failure(java.lang.IllegalArgumentException: not positive: -1)

            StringBuilder log = new StringBuilder();
            Try<Integer> done = Try.of(() -> 1).ensuring(() -> log.append("closed"));
            // Success(1), and log is "closed"

            assertThat(ratio).hasToString("Failure(java.lang.ArithmeticException: / by zero)");
            assertThat(positive).hasToString("Failure(java.lang.IllegalArgumentException: not positive: -1)");
            assertThat(done).hasToString("Success(1)");
            assertThat(log).hasToString("closed");
        }

        @Test
        void conversions() {
            Either<Throwable, Integer> either = Try.of(() -> Integer.parseInt("7")).toEither();
            java.util.concurrent.CompletableFuture<Integer> future = Try.success(7).toCompletableFuture();
            Try<Integer> back = Try.fromCompletableFuture(future);
            // Right(7), a completed future, Success(7)

            assertThat(either).hasToString("Right(7)");
            assertThat(future).isCompletedWithValue(7);
            assertThat(back).hasToString("Success(7)");
        }

        @Test
        void sharpEdges() {
            Try<Integer> first = Try.of(() -> Integer.parseInt("x"));
            Try<Integer> second = Try.of(() -> Integer.parseInt("x"));
            boolean same = first.equals(second);
            boolean sameClass = first.getCause().getClass() == second.getCause().getClass();
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
    class LazyPage {

        @Test
        void construction() {
            Lazy<Integer> answer = Lazy.of(() -> 6 * 7);
            boolean before = answer.isEvaluated();
            int value = answer.map(n -> n + 1).get();
            boolean after = answer.isEvaluated();
            // before is false, value is 43, after is true

            assertThat(before).isFalse();
            assertThat(value).isEqualTo(43);
            assertThat(after).isTrue();
        }

        @Test
        void operations() {
            Lazy<String> host = Lazy.of(() -> "localhost");
            Lazy<Integer> port = Lazy.of(() -> 8080);
            Lazy<String> address = host.zipWith(port, (h, p) -> h + ":" + p);
            boolean evaluated = host.isEvaluated();
            String value = address.get();
            // evaluated is false, value is "localhost:8080"

            Lazy<Vector<Integer>> all = Lazy.collectAll(Vector.of(Lazy.of(() -> 1), Lazy.of(() -> 2)));
            // all.get() is Vector(1, 2)

            assertThat(evaluated).isFalse();
            assertThat(value).isEqualTo("localhost:8080");
            assertThat(all.isEvaluated()).isFalse();
            assertThat(all.get()).isEqualTo(Vector.of(1, 2));
        }

        @Test
        void conversions() {
            int[] calls = {0};
            Lazy<String> greeting = Lazy.of(() -> {
                calls[0]++;
                return "hello";
            });
            java.util.function.Supplier<String> supplier = greeting.toSupplier();
            String twice = supplier.get() + supplier.get();
            // twice is "hellohello", calls[0] is 1: computed once

            assertThat(twice).isEqualTo("hellohello");
            assertThat(calls[0]).isEqualTo(1);
            assertThat(greeting.get()).isEqualTo("hello");
            assertThat(calls[0]).isEqualTo(1);
        }

        @Test
        void sharpEdges() {
            java.util.concurrent.atomic.AtomicInteger attempts = new java.util.concurrent.atomic.AtomicInteger();
            Lazy<String> flaky = Lazy.of(() -> {
                if (attempts.incrementAndGet() == 1) {
                    throw new IllegalStateException("not yet");
                }
                return "ready";
            });
            Try<String> first = Try.of(flaky::get);
            String second = flaky.get();
            // first is Failure(java.lang.IllegalStateException: not yet), second is "ready", attempts is 2

            Lazy<Integer> unread = Lazy.of(() -> 1);
            Lazy<Integer> other = Lazy.of(() -> 1);
            String shown = unread.toString();
            boolean equal = unread.equals(other);
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

            Validation<String, User> user = Validation.zipWith(name(""), age(-1), email("jules"), User::new);
            // Invalid(name is blank, age is negative, email has no @)

            Validation<String, Tuple2<String, Integer>> pair = name("Ada").zip(age(36));
            Validation<String, String> both = name("").zipWith(age(-1), (n, a) -> n + a);
            // Valid((Ada, 36)), Invalid(name is blank, age is negative)

            assertThat(user).hasToString("Invalid(name is blank, age is negative, email has no @)");
            assertThat(pair).hasToString("Valid((Ada, 36))");
            assertThat(both).hasToString("Invalid(name is blank, age is negative)");
            assertThat(Validation.<String, Integer>invalid("a").zip(Either.<String, Integer>left("b")))
                .hasToString("Invalid(a, b)");
        }

        @Test
        void readingTheResult() {
            Validation<String, Integer> checked = age(-5);
            String message = switch (checked) {
                case Valid(var years) -> "age " + years;
                case Invalid(var errors) -> errors.size() + " error(s): " + errors.mkString("; ");
            };
            // "1 error(s): age is negative"

            assertThat(message).isEqualTo("1 error(s): age is negative");
        }

        @Test
        void manyValues() {
            Validation<String, Vector<Integer>> ages = Validation.forEach(Vector.of(3, -1, 7, -2), n -> age(n));
            // Invalid(age is negative, age is negative)

            Validation<String, Vector<String>> names = Validation.collectAll(Vector.of(name("Ada"), name("Grace")));
            // Valid(Vector(Ada, Grace))

            Validation<String, NonEmptyVector<Integer>> scores = Validation.forEach(NonEmptyVector.of(1, 2), n -> age(n));
            // Valid(NonEmptyVector(1, 2))

            Tuple2<Vector<String>, Vector<Integer>> split = Validation.partition(Vector.of(4, -1, 9), n -> age(n));
            // (Vector(age is negative), Vector(4, 9))

            assertThat(ages).hasToString("Invalid(age is negative, age is negative)");
            assertThat(names).hasToString("Valid(Vector(Ada, Grace))");
            assertThat(scores).hasToString("Valid(NonEmptyVector(1, 2))");
            assertThat(split).hasToString("(Vector(age is negative), Vector(4, 9))");
        }

        @Test
        void shortCircuiting() {
            Validation<String, User> adult = Validation.zipWith(name("Ada"), age(15), email("ada@example.com"), User::new)
                .flatMapEither(u -> u.age() >= 18 ? Either.right(u) : Either.left(u.name() + " is under 18"));
            // Invalid(Ada is under 18)

            assertThat(adult).hasToString("Invalid(Ada is under 18)");
            assertThat(Validation.<String, Integer>invalid("a").orElse(() -> Validation.invalid("b")))
                .hasToString("Invalid(b)");
        }

        @Test
        void checks() {
            Validation<String, Integer> port = Validation.of(() -> Integer.parseInt("80a"), e -> "port is not a number");
            // Invalid(port is not a number)

            assertThat(port).hasToString("Invalid(port is not a number)");
        }

        @Test
        void conversions() {
            Validation<String, Integer> negative = age(-3);
            Either<String, Integer> joined = negative.toEitherWith(errors -> errors.mkString("; "));
            Try<Integer> failure = negative.toTry(errors -> new IllegalArgumentException(errors.mkString("; ")));
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
            Option<Integer> sum = Option.zipWith(Option.some(1), Option.some(2), Option.some(3), (a, b, c) -> a + b + c);
            Option<Tuple3<Integer, String, Boolean>> triple = Option.zip(Option.some(1), Option.some("a"), Option.some(true));
            // Some(6), Some((1, a, true))

            assertThat(sum).hasToString("Some(6)");
            assertThat(triple).hasToString("Some((1, a, true))");
        }

        @Test
        void onFailure() {
            Either<String, Integer> first = Either.zipWith(Either.left("no a"), Either.<String, Integer>right(2),
                Either.<String, Integer>left("no c"), (a, b, c) -> 0);
            Validation<String, Integer> all = Validation.zipWith(Validation.<String, Integer>invalid("no a"),
                Validation.<String, Integer>valid(2), Validation.<String, Integer>invalid("no c"), (a, b, c) -> 0);
            // Left(no a), Invalid(no a, no c)

            Option<Integer> left = Option.some(1).zipLeft(Option.none());
            Option<String> right = Option.some(1).zipRight(Option.some("kept"));
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
            NonEmptyVector<Integer> scores = NonEmptyVector.of(7, 3, 9);
            int best = scores.max(Integer::compare);
            int total = scores.reduce(Integer::sum);
            int first = scores.head();
            // 9, 19, 7

            assertThat(best).isEqualTo(9);
            assertThat(total).isEqualTo(19);
            assertThat(first).isEqualTo(7);
        }

        @Test
        void returnTypeContract() {
            NonEmptyVector<Integer> grown = NonEmptyVector.of(1).appendAll(Vector.empty());
            Vector<Integer> evens = NonEmptyVector.of(1, 2, 3).filter(n -> n % 2 == 0);
            Option<NonEmptyVector<Integer>> rest = NonEmptyVector.of(1).tailNonEmpty();
            // NonEmptyVector(1), Vector(2), None

            assertThat(grown).hasToString("NonEmptyVector(1)");
            assertThat(evens).hasToString("Vector(2)");
            assertThat(rest).hasToString("None");
        }

        @Test
        void construction() {
            Option<NonEmptyVector<String>> fromInput = Vector.of("a", "b").toNonEmptyVector();
            Option<NonEmptyVector<String>> fromNothing = Vector.<String>empty().toNonEmptyVector();
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
            Vector<Integer> vector = Vector.of(3, 1, 2);
            List<Integer> sortedList = vector.toList().sorted();
            HashSet<Integer> set = HashSet.ofAll(vector);
            boolean same = Vector.of(1, 2, 3).equals(sortedList);
            // List(1, 2, 3), a HashSet of 1, 2, 3, and true

            Tuple2<List<Integer>, List<String>> split = List.of(1, 2, 3, 4)
                .partitionMap(n -> n % 2 == 0 ? Either.left(n) : Either.right("odd " + n));
            Vector<Integer> flat = Vector.flatten(Vector.of(Vector.of(1, 2), List.of(3)));
            // split is (List(2, 4), List(odd 1, odd 3)), flat is Vector(1, 2, 3)

            assertThat(split).hasToString("(List(2, 4), List(odd 1, odd 3))");
            assertThat(flat).hasToString("Vector(1, 2, 3)");
            assertThat(sortedList).hasToString("List(1, 2, 3)");
            assertThat(set).isEqualTo(HashSet.of(1, 2, 3));
            assertThat(same).isTrue();
        }

        @Test
        void nulls() {
            Option<Integer> missing = HashMap.of("a", 1).get("b");
            Option<Integer> firstEven = Vector.of(1, 3, 4).find(n -> n % 2 == 0);
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
            Vector<String> letters = Vector.of("a", "b", "c", "d");
            Vector<String> changed = letters.update(1, "B").prepend("z").drop(2);
            Tuple2<Vector<String>, Vector<String>> halves = letters.splitAt(2);
            // changed is Vector(B, c, d), halves is (Vector(a, b), Vector(c, d))

            Vector<Integer> numbers = Vector.range(0, 10);
            Vector<Vector<Integer>> windows = numbers.sliding(3, 3);
            Tuple2<Vector<Integer>, Vector<String>> parts = numbers.partitionMap(
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
            List<Integer> list = List.of(1, 2, 3);
            String first = switch (list) {
                case Cons(var head, var tail) -> "head " + head + ", then " + tail.length() + " more";
                case Nil() -> "empty";
            };
            // "head 1, then 2 more"

            List<String> stack = List.<String>empty().push("a").push("b");
            String top = stack.peek();
            List<String> popped = stack.pop();
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
            Queue<String> queue = Queue.of("a", "b").enqueue("c");
            Tuple2<String, Queue<String>> next = queue.dequeue();
            // next is (a, Queue(b, c))

            Queue<Integer> work = Queue.of(1);
            int visited = 0;
            while (!work.isEmpty() && visited < 5) {
                Tuple2<Integer, Queue<Integer>> step = work.dequeue();
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
            Stream<Integer> naturals = Stream.from(1);
            Vector<Integer> squares = naturals.map(n -> n * n).filter(n -> n % 2 == 1).take(4).toVector();
            // Vector(1, 9, 25, 49)

            Stream<Long> fibonacci = Stream.of(0L, 1L).appendSelf(self -> self.zipWith(self.tail(), Long::sum));
            Vector<Long> firstTen = fibonacci.take(10).toVector();
            // Vector(0, 1, 1, 2, 3, 5, 8, 13, 21, 34)

            assertThat(squares).hasToString("Vector(1, 9, 25, 49)");
            assertThat(firstTen).hasToString("Vector(0, 1, 1, 2, 3, 5, 8, 13, 21, 34)");
        }
    }

    @Nested
    class SetsPage {

        @Test
        void whenToChooseWhich() {
            HashSet<String> tags = HashSet.of("java", "scala");
            HashSet<String> more = tags.add("zio").remove("scala");
            HashSet<String> common = tags.intersect(HashSet.of("scala", "kotlin"));
            // more contains java and zio, common is HashSet(scala)

            LinkedHashSet<String> seen = LinkedHashSet.of("b", "a").add("c").add("a");
            TreeSet<Integer> sorted = TreeSet.of(5, 1, 4, 2);
            Integer smallest = sorted.head();
            TreeSet<Integer> firstTwo = sorted.take(2);
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
            HashMap<String, Integer> stock = HashMap.of("apple", 3, "pear", 0);
            HashMap<String, Integer> restocked = stock.put("pear", 5, Integer::sum).put("fig", 1);
            Option<Integer> pears = restocked.get("pear");
            int kiwis = restocked.getOrElse("kiwi", 0);
            // pears is Some(5), kiwis is 0

            TreeMap<String, Integer> byName = TreeMap.of("b", 2, "a", 1, "c", 3);
            Vector<Integer> values = byName.values();
            Tuple2<String, Integer> first = byName.head();
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
            Vector.Builder<String> builder = Vector.newBuilder();
            for (String word : "the quick brown fox".split(" ")) {
                builder.add(word.toUpperCase());
            }
            Vector<String> words = builder.result();
            // Vector(THE, QUICK, BROWN, FOX)

            Vector.Builder<Integer> both = Vector.newBuilder(8);
            both.addAll(Vector.of(1, 2, 3)).add(4);
            int added = both.size();
            Vector<Integer> result = both.result();
            // added is 4, result is Vector(1, 2, 3, 4)

            assertThat(words).hasToString("Vector(THE, QUICK, BROWN, FOX)");
            assertThat(added).isEqualTo(4);
            assertThat(result).hasToString("Vector(1, 2, 3, 4)");
            assertThatThrownBy(() -> both.add(5)).isInstanceOf(IllegalStateException.class);
        }

        @Test
        void collectors() {
            Vector<Integer> lengths = java.util.stream.Stream.of("a", "bb", "ccc").map(String::length).collect(Vector.collector());
            TreeSet<String> sorted = java.util.stream.Stream.of("b", "a", "b").collect(TreeSet.collector());
            // Vector(1, 2, 3), TreeSet(a, b)

            assertThat(lengths).hasToString("Vector(1, 2, 3)");
            assertThat(sorted).hasToString("TreeSet(a, b)");
        }
    }

    @Nested
    class JavaInteropPage {

        @Test
        void views() {
            Vector<String> names = Vector.of("Ada", "Grace");
            java.util.List<String> view = names.asJava();
            String second = view.get(1);
            boolean rejected = Try.run(() -> view.add("Linus")).getCause() instanceof UnsupportedOperationException;
            // second is "Grace", rejected is true

            assertThat(second).isEqualTo("Grace");
            assertThat(rejected).isTrue();
            assertThat(view.reversed()).containsExactly("Grace", "Ada");
            assertThat(HashSet.of(1).asJava()).isEqualTo(java.util.Set.of(1));
            assertThat(Vector.ofAll(names.asJava())).isSameAs(names);
        }

        @Test
        void sortedViews() {
            java.util.NavigableSet<Integer> scores = TreeSet.of(10, 20, 30, 40).asJava();
            Integer atLeast25 = scores.ceiling(25);
            java.util.NavigableSet<Integer> top = scores.tailSet(20, true);
            java.util.NavigableMap<String, Integer> ages = TreeMap.of("Ada", 36, "Grace", 85).asJavaMap();
            Integer grace = ages.get("Grace");
            // atLeast25 is 30, top is [20, 30, 40], grace is 85

            assertThat(atLeast25).isEqualTo(30);
            assertThat(top).hasToString("[20, 30, 40]");
            assertThat(grace).isEqualTo(85);
        }

        @Test
        void copies() {
            java.util.ArrayList<Integer> mutable = new java.util.ArrayList<>(Vector.of(1, 2).asJava());
            mutable.add(3);
            java.util.Set<String> jdkSet = new java.util.HashSet<>(HashSet.of("a", "b").asJava());
            // mutable is [1, 2, 3], jdkSet holds a and b

            assertThat(mutable).hasToString("[1, 2, 3]");
            assertThat(jdkSet).containsExactlyInAnyOrder("a", "b");
        }

        @Test
        void theWayBack() {
            Vector<Integer> fromJdk = Vector.ofAll(java.util.List.of(3, 1, 2));
            int sum = fromJdk.stream().mapToInt(Integer::intValue).sum();
            Option<Integer> fromOptional = Option.ofOptional(java.util.Optional.of(4));
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
            Option<String> maybe = Option.some("Zazr");
            int length = switch (maybe) {
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
        String trimmed = input.trim();
        return trimmed.contains("@")
            ? Validation.valid(new Email(trimmed))
            : Validation.invalid("email has no @");
    }

    String value() {
        return value;
    }
}
