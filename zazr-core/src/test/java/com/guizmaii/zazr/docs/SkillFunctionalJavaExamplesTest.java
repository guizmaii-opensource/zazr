package com.guizmaii.zazr.docs;

import com.guizmaii.zazr.Lazy;
import com.guizmaii.zazr.collection.HashMap;
import com.guizmaii.zazr.collection.NonEmptyVector;
import com.guizmaii.zazr.collection.Stream;
import com.guizmaii.zazr.collection.Vector;
import com.guizmaii.zazr.control.Either;
import com.guizmaii.zazr.control.Either.Left;
import com.guizmaii.zazr.control.Either.Right;
import com.guizmaii.zazr.control.Option;
import com.guizmaii.zazr.control.Option.None;
import com.guizmaii.zazr.control.Option.Some;
import com.guizmaii.zazr.control.Try;
import com.guizmaii.zazr.control.Validation;
import java.time.LocalDate;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Every fenced {@code java} block of {@code skills/zazr/references/functional-java.md} is pasted here verbatim,
 * compiled and run.
 */
public class SkillFunctionalJavaExamplesTest {

    // -- shared by several sections: the parser of "Errors as values" and the sealed type of "Make invalid states
    // impossible to represent"

    // before
    // throws NumberFormatException or IllegalArgumentException, and only this comment says so
    static int parsePortOrThrow(String input) {
        var port = Integer.parseInt(input.trim());
        if (port < 1 || port > 65_535) {
            throw new IllegalArgumentException("port out of range: " + port);
        }
        return port;
    }

    // after
    static Either<String, Integer> parsePort(String input) {
        return Try.of(() -> Integer.parseInt(input.trim()))
            .toEither()
            .mapLeft(e -> "not a number: " + input)
            .filterOrElse(p -> p >= 1 && p <= 65_535, p -> "port out of range: " + p);
    }

    record ServerConfig(String host, int port) {}

    static Validation<String, String> checkHost(String host) {
        return Validation.fromPredicate(host.trim(), h -> !h.isEmpty(), h -> "host is required");
    }

    // after
    sealed interface Payment {}
    record Card(String number) implements Payment {}
    record Transfer(String iban) implements Payment {}

    static String describe(Payment payment) {
        return switch (payment) {
            case Card(var number) -> "card " + number;
            case Transfer(var iban) -> "transfer from " + iban;
        };
    }

    @Nested
    class ImmutableDataBefore {

        // before
        final class Order {
            private final java.util.List<String> items = new java.util.ArrayList<>();
            private boolean paid;

            void addItem(String item) {
                items.add(item);
            }

            void markPaid() {
                paid = true;
            }

            java.util.List<String> items() {
                return items; // any caller can change the order through this list
            }
        }

        @Test
        void mutableOrder() {
            var order = new Order();
            order.addItem("book");
            order.items().add("pen");
            order.markPaid();

            assertThat(order.items()).containsExactly("book", "pen");
            assertThat(order.paid).isTrue();
        }
    }

    @Nested
    class ImmutableDataAfter {

        // after
        record Order(Vector<String> items, boolean paid) {
            Order addItem(String item) {
                return new Order(items.append(item), paid);
            }

            Order markPaid() {
                return new Order(items, true);
            }
        }

        @Test
        void immutableOrder() {
            var empty = new Order(Vector.empty(), false);
            var order = empty.addItem("book").addItem("pen").markPaid(); // Order
            // order.items() is Vector(book, pen), and empty is unchanged

            assertThat(order).isEqualTo(new Order(Vector.of("book", "pen"), true));
            assertThat(empty).isEqualTo(new Order(Vector.empty(), false));
        }
    }

    @Nested
    class ExpressionsOverStatements {

        // before
        enum Tier { BRONZE, SILVER, GOLD }

        static int discountPercent(Tier tier) {
            int discount;
            if (tier == Tier.GOLD) {
                discount = 20;
            } else if (tier == Tier.SILVER) {
                discount = 10;
            } else {
                discount = 0;
            }
            return discount;
        }

        static int discountPercentOf(Tier tier) {
            return switch (tier) {
                case GOLD -> 20;
                case SILVER -> 10;
                case BRONZE -> 0;
            };
        }

        @Test
        void switchExpression() {
            for (var tier : Tier.values()) {
                assertThat(discountPercentOf(tier)).isEqualTo(discountPercent(tier));
            }
            assertThat(discountPercentOf(Tier.GOLD)).isEqualTo(20);
            assertThat(discountPercentOf(Tier.SILVER)).isEqualTo(10);
            assertThat(discountPercentOf(Tier.BRONZE)).isEqualTo(0);
        }

        @Test
        void ifOnNull() {
            // before
            var nickname = java.util.Map.of("ada", "Countess").get("alan"); // String, null here
            String display;
            if (nickname != null) {
                display = nickname.toUpperCase();
            } else {
                display = "anonymous";
            }

            assertThat(display).isEqualTo("anonymous");
        }

        @Test
        void mapAndFold() {
            // after
            var nickname = HashMap.of("ada", "Countess").get("alan"); // Option<String>
            var display = nickname.map(String::toUpperCase).getOrElse("anonymous");
            var length = nickname.fold(() -> 0, String::length); // Integer
            // display is "anonymous", length is 0

            assertThat(nickname).isEqualTo(Option.none());
            assertThat(display).isEqualTo("anonymous");
            assertThat(length).isEqualTo(0);
        }

        @Test
        void foldLeft() {
            var prices = Vector.of(1_200, 850, 4_000);
            var total = prices.foldLeft(0, Integer::sum); // Integer
            // 6050

            assertThat(total).isEqualTo(6050);
        }
    }

    @Nested
    class PureFunctions {

        record Invoice(String customer, LocalDate due, boolean paid) {}

        static void remindLatePayers(java.util.List<Invoice> invoices, java.util.List<String> outbox) {
            for (var invoice : invoices) {
                if (!invoice.paid() && invoice.due().isBefore(LocalDate.now())) {
                    outbox.add("Reminder to " + invoice.customer());
                }
            }
        }

        static Vector<String> reminders(Vector<Invoice> invoices, LocalDate today) {
            return invoices
                .filter(i -> !i.paid() && i.due().isBefore(today))
                .map(i -> "Reminder to " + i.customer());
        }

        @Test
        void impure() {
            var outbox = new java.util.ArrayList<String>();
            remindLatePayers(
                java.util.List.of(
                    new Invoice("Ada", LocalDate.of(2000, 1, 1), false),
                    new Invoice("Alan", LocalDate.of(2000, 1, 1), true),
                    new Invoice("Grace", LocalDate.of(9999, 1, 1), false)),
                outbox);

            assertThat(outbox).containsExactly("Reminder to Ada");
        }

        @Test
        void pureAndEdge() {
            var invoices = Vector.of(
                new Invoice("Ada", LocalDate.of(2026, 1, 10), false),
                new Invoice("Alan", LocalDate.of(2026, 1, 10), true),
                new Invoice("Grace", LocalDate.of(2026, 3, 1), false));
            var late = reminders(invoices, LocalDate.of(2026, 2, 1)); // Vector<String>
            // Vector(Reminder to Ada), every time

            // the edge: the one line that reads the clock and acts
            var outbox = new java.util.ArrayList<String>();
            reminders(invoices, LocalDate.now()).forEach(outbox::add);

            assertThat(late).isEqualTo(Vector.of("Reminder to Ada"));
            assertThat(reminders(invoices, LocalDate.of(2026, 2, 1))).isEqualTo(late);

            // every invoice is due by now, and Alan's is paid
            assertThat(outbox).containsExactly("Reminder to Ada", "Reminder to Grace");
        }
    }

    @Nested
    class ErrorsAsValues {

        @Test
        void throwing() {
            assertThat(parsePortOrThrow(" 443 ")).isEqualTo(443);
            assertThatThrownBy(() -> parsePortOrThrow("http")).isInstanceOf(NumberFormatException.class);
            assertThatThrownBy(() -> parsePortOrThrow("80800"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("port out of range: 80800");
        }

        @Test
        void either() {
            var message = switch (parsePort("80800")) {
                case Right(var port) -> "listening on " + port;
                case Left(var error) -> "bad config: " + error;
            };
            // "bad config: port out of range: 80800"

            assertThat(message).isEqualTo("bad config: port out of range: 80800");
            assertThat(parsePort(" 443 ")).isEqualTo(Either.right(443));
            assertThat(parsePort("0")).isEqualTo(Either.left("port out of range: 0"));
            assertThat(parsePort("http")).isEqualTo(Either.left("not a number: http"));
        }

        @Test
        void validation() {
            var config = Validation.zipWith( // Validation<String, ServerConfig>
                checkHost(" "), parsePort("http").toValidation(), ServerConfig::new);
            // Invalid(host is required, not a number: http)

            assertThat(config)
                .isEqualTo(Validation.invalidAll(NonEmptyVector.of("host is required", "not a number: http")));
            assertThat(config.toString()).isEqualTo("Invalid(host is required, not a number: http)");
            var valid = Validation.zipWith(checkHost(" example.com "), parsePort("8080").toValidation(), ServerConfig::new);
            assertThat(valid).isEqualTo(Validation.valid(new ServerConfig("example.com", 8080)));
        }
    }

    @Nested
    class ParseDontValidate {

        // before
        static boolean isValidSku(String input) {
            return input.matches("[A-Z]{3}-\\d{4}");
        }

        record Sku(String value) {
            Sku {
                if (!value.matches("[A-Z]{3}-\\d{4}")) {
                    throw new IllegalArgumentException("not a SKU: " + value);
                }
            }

            static Either<String, Sku> parse(String input) {
                var normalised = input.trim().toUpperCase();
                return normalised.matches("[A-Z]{3}-\\d{4}")
                    ? Either.right(new Sku(normalised))
                    : Either.left("not a SKU: " + input);
            }
        }

        @Test
        void parse() {
            var good = Sku.parse(" abc-1234 "); // Either<String, Sku>
            var bad = Sku.parse("abc");         // Either<String, Sku>
            // Right(Sku[value=ABC-1234]), Left(not a SKU: abc)

            assertThat(isValidSku("ABC-1234")).isTrue();
            assertThat(isValidSku("abc")).isFalse();
            assertThat(good).isEqualTo(Either.right(new Sku("ABC-1234")));
            assertThat(good.toString()).isEqualTo("Right(Sku[value=ABC-1234])");
            assertThat(bad).isEqualTo(Either.left("not a SKU: abc"));
            assertThat(bad.toString()).isEqualTo("Left(not a SKU: abc)");
            assertThatThrownBy(() -> new Sku("abc")).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class InvalidStatesBefore {

        // before
        final class Payment {
            String method;     // "card" or "transfer"
            String cardNumber; // set when method is "card", hopefully
            String iban;       // set when method is "transfer", hopefully
        }

        @Test
        void fieldsThatDisagree() {
            var payment = new Payment();
            payment.method = "card";
            payment.iban = "FR76";

            // nothing stops a card payment with an IBAN and no card number
            assertThat(payment.cardNumber).isNull();
        }
    }

    @Nested
    class InvalidStatesAfter {

        @Test
        void describeEveryCase() {
            assertThat(describe(new Card("4970101234567890"))).isEqualTo("card 4970101234567890");
            assertThat(describe(new Transfer("FR76 3000"))).isEqualTo("transfer from FR76 3000");
        }
    }

    @Nested
    class TotalFunctions {

        // before
        static int highestScoreOrThrow(java.util.List<Integer> scores) {
            return scores.stream().max(Integer::compare).orElseThrow(); // throws on an empty list
        }

        static int highestScore(NonEmptyVector<Integer> scores) {
            return scores.max(Integer::compare);
        }

        @Test
        void partial() {
            assertThat(highestScoreOrThrow(java.util.List.of(12, 40, 7))).isEqualTo(40);
            assertThatThrownBy(() -> highestScoreOrThrow(java.util.List.of()))
                .isInstanceOf(java.util.NoSuchElementException.class);
        }

        @Test
        void total() {
            var scores = Vector.of(12, 40, 7).toNonEmptyVector(); // Option<NonEmptyVector<Integer>>
            var best = scores.map(s -> highestScore(s));        // Option<Integer>
            // Some(40); an empty Vector gives None

            assertThat(best).isEqualTo(Option.some(40));
            assertThat(Vector.<Integer>empty().toNonEmptyVector().map(s -> highestScore(s))).isEqualTo(Option.none());
        }

        @Test
        void optionInsteadOfNull() {
            assertThat(Option.ofNullable(System.getenv("NO_SUCH_VARIABLE"))).isEqualTo(Option.none());
        }
    }

    @Nested
    class Composition {

        // before
        static String greetingOrNull(java.util.Map<String, String> names, java.util.Map<String, String> languages, String id) {
            var name = names.get(id);
            if (name == null) {
                return null;
            }
            var language = languages.get(id);
            if (language == null) {
                return null;
            }
            return language.equals("fr") ? "Bonjour " + name : "Hello " + name;
        }

        // after
        static String greet(String name, String language) {
            return language.equals("fr") ? "Bonjour " + name : "Hello " + name;
        }

        static Option<String> greeting(HashMap<String, String> names, HashMap<String, String> languages, String id) {
            return names.get(id).zipWith(languages.get(id), (name, language) -> greet(name, language));
        }

        @Test
        void nullChecks() {
            var names = java.util.Map.of("1", "Ada", "2", "Alan");
            var languages = java.util.Map.of("1", "fr");
            assertThat(greetingOrNull(names, languages, "1")).isEqualTo("Bonjour Ada");
            assertThat(greetingOrNull(names, languages, "2")).isNull();
            assertThat(greetingOrNull(names, languages, "3")).isNull();
        }

        @Test
        void zipWith() {
            var names = HashMap.of("1", "Ada", "2", "Alan", "3", "Grace");
            var languages = HashMap.of("1", "fr", "3", "en");
            assertThat(greeting(names, languages, "1")).isEqualTo(Option.some("Bonjour Ada"));
            assertThat(greeting(names, languages, "2")).isEqualTo(Option.none());
            assertThat(greeting(names, languages, "3")).isEqualTo(Option.some("Hello Grace"));
            assertThat(greeting(names, languages, "4")).isEqualTo(Option.none());
        }

        @Test
        void flatMapAndForEach() {
            var address = parsePort("8080") // Either<String, String>
                .flatMap(p -> p < 1_024 ? Either.left("privileged port") : Either.right("localhost:" + p));
            var ports = Either.forEach(Vector.of("80", "443", "x"), s -> parsePort(s)); // Either<String, Vector<Integer>>
            // Right(localhost:8080), Left(not a number: x)

            assertThat(address).isEqualTo(Either.right("localhost:8080"));
            assertThat(ports).isEqualTo(Either.left("not a number: x"));
            assertThat(Either.forEach(Vector.of("80", "443"), s -> parsePort(s)))
                .isEqualTo(Either.right(Vector.of(80, 443)));
        }
    }

    @Nested
    class Laziness {

        // before
        final class Catalogue {
            private Vector<String> items; // null until the first call

            Vector<String> items() {
                if (items == null) {
                    items = Vector.of("book", "pen");
                }
                return items;
            }
        }

        // after
        final class LazyCatalogue {
            private final Lazy<Vector<String>> items = Lazy.of(() -> Vector.of("book", "pen"));

            Vector<String> items() {
                return items.get();
            }
        }

        static boolean isPrime(int n) {
            return n > 1 && java.util.stream.IntStream.rangeClosed(2, (int) Math.sqrt(n)).noneMatch(d -> n % d == 0);
        }

        @Test
        void cachedOnce() {
            var catalogue = new Catalogue();
            assertThat(catalogue.items).isNull();
            assertThat(catalogue.items()).isSameAs(catalogue.items());

            var lazy = new LazyCatalogue();
            assertThat(lazy.items.isEvaluated()).isFalse();
            assertThat(lazy.items()).isSameAs(lazy.items()).isEqualTo(Vector.of("book", "pen"));
            assertThat(lazy.items.isEvaluated()).isTrue();
        }

        @Test
        void whileLoop() {
            // before
            var found = new java.util.ArrayList<Integer>();
            var candidate = 2;
            while (found.size() < 5) {
                if (isPrime(candidate)) {
                    found.add(candidate);
                }
                candidate++;
            }

            assertThat(found).containsExactly(2, 3, 5, 7, 11);
        }

        @Test
        void stream() {
            // after
            var primes = Stream.from(2).filter(n -> isPrime(n)).take(5).toVector(); // Vector<Integer>
            // Vector(2, 3, 5, 7, 11)

            assertThat(primes).isEqualTo(Vector.of(2, 3, 5, 7, 11));
        }
    }

    @Nested
    class Java25Features {

        static String summaryOld(Payment payment) {
            if (payment == null) {
                return "not paid yet";
            }
            if (payment instanceof Card) {
                var card = (Card) payment;
                return card.number().startsWith("4") ? "Visa card" : "other card";
            }
            if (payment instanceof Transfer) {
                return "bank transfer";
            }
            throw new IllegalStateException("unknown payment: " + payment);
        }

        static String summary(Option<Payment> payment) {
            return switch (payment) {
                case Some(Card(var number)) when number.startsWith("4") -> "Visa card";
                case Some(Card(_)) -> "other card";
                case Some(Transfer(_)) -> "bank transfer";
                case None() -> "not paid yet";
            };
        }

        @Test
        void patterns() {
            var paid = summary(Option.some(new Transfer("FR76 3000 6000 0112 3456 7890 189")));
            var unpaid = summary(Option.none());
            var count = Vector.of("a", "b", "c").foldLeft(0, (n, _) -> n + 1); // Integer
            // "bank transfer", "not paid yet", 3

            assertThat(paid).isEqualTo("bank transfer");
            assertThat(unpaid).isEqualTo("not paid yet");
            assertThat(count).isEqualTo(3);
            assertThat(summary(Option.some(new Card("4970101234567890")))).isEqualTo("Visa card");
            assertThat(summary(Option.some(new Card("5100101234567890")))).isEqualTo("other card");
            assertThat(summaryOld(null)).isEqualTo("not paid yet");
            assertThat(summaryOld(new Card("4970101234567890"))).isEqualTo("Visa card");
            assertThat(summaryOld(new Card("5100101234567890"))).isEqualTo("other card");
            assertThat(summaryOld(new Transfer("FR76"))).isEqualTo("bank transfer");
        }
    }
}
