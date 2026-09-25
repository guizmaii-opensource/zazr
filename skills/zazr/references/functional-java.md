# Functional Java with Zazr

How to write functional Java 25 code, with Zazr as the toolkit: each rule with the imperative code to avoid and the
functional code to write instead.

The ideas are explained for a human reader at https://zazr.dev/new-to-fp/. Follow the same rules here.

Imports: Zazr's `List`, `Stream`, `Map`, `Set` and `Queue` share their names with JDK types. Import Zazr's and write
the JDK ones in full (`java.util.List`, `java.util.stream.Stream`). Pattern matching on Zazr values needs the case
records imported: `Option.Some`, `Option.None`, `Either.Left`, `Either.Right`, `Try.Success`, `Try.Failure`,
`Validation.Valid`, `Validation.Invalid`.

## Immutable data

Model data as records whose components are immutable: Zazr collections, other records, strings, numbers. A change
returns a new value. Never write a setter, and never mutate a collection you were given or that you return.

```java
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
```

```java
// after
record Order(Vector<String> items, boolean paid) {
    Order addItem(String item) {
        return new Order(items.append(item), paid);
    }

    Order markPaid() {
        return new Order(items, true);
    }
}
```

```java
var empty = new Order(Vector.empty(), false);
var order = empty.addItem("book").addItem("pen").markPaid(); // Order
// order.items() is Vector(book, pen), and empty is unchanged
```

A record holding a `java.util.List` is not immutable: the list can still change. Use `Vector`, `HashMap`, `HashSet`
and the other Zazr collections for components; they share structure, so a new version is cheap.

Mutation is fine when nobody can see it. To build a collection in a loop, use a builder (`Vector.newBuilder()`,
`HashMap.newBuilder()`) inside the function and return its `result()`. See https://zazr.dev/builders/.

## Expressions over statements

Compute a value with an expression instead of declaring a variable and assigning it in branches. An expression
cannot forget a branch, and the variable is assigned once.

```java
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
```

After, a `switch` expression, checked for exhaustiveness by the compiler:

```java
static int discountPercentOf(Tier tier) {
    return switch (tier) {
        case GOLD -> 20;
        case SILVER -> 10;
        case BRONZE -> 0;
    };
}
```

For a value that may be absent, use `map` and `getOrElse`, or `fold`, instead of an `if` on `null`.

```java
// before
var nickname = java.util.Map.of("ada", "Countess").get("alan"); // String, null here
String display;
if (nickname != null) {
    display = nickname.toUpperCase();
} else {
    display = "anonymous";
}
```

```java
// after
var nickname = HashMap.of("ada", "Countess").get("alan"); // Option<String>
var display = nickname.map(String::toUpperCase).getOrElse("anonymous");
var length = nickname.fold(() -> 0, String::length); // Integer
// display is "anonymous", length is 0
```

A loop that accumulates into a variable is a `foldLeft`:

```java
var prices = Vector.of(1_200, 850, 4_000);
var total = prices.foldLeft(0, Integer::sum); // Integer
// 6050
```

## Pure functions, effects at the edges

A pure function computes its result from its arguments only: it reads no clock, no file, no global state, and
changes nothing. Pure functions are tested with plain values, no mocks, and can be called twice safely.

Keep the decisions in pure functions. Read the clock, the database and the network, and send the emails, in a thin
layer at the edge that calls them.

Before, the rule, the clock and the sending are mixed:

```java
record Invoice(String customer, LocalDate due, boolean paid) {}

static void remindLatePayers(java.util.List<Invoice> invoices, java.util.List<String> outbox) {
    for (var invoice : invoices) {
        if (!invoice.paid() && invoice.due().isBefore(LocalDate.now())) {
            outbox.add("Reminder to " + invoice.customer());
        }
    }
}
```

After, the rule is a pure function and `today` is an argument:

```java
static Vector<String> reminders(Vector<Invoice> invoices, LocalDate today) {
    return invoices
        .filter(i -> !i.paid() && i.due().isBefore(today))
        .map(i -> "Reminder to " + i.customer());
}
```

```java
var invoices = Vector.of(
    new Invoice("Ada", LocalDate.of(2026, 1, 10), false),
    new Invoice("Alan", LocalDate.of(2026, 1, 10), true),
    new Invoice("Grace", LocalDate.of(2026, 3, 1), false));
var late = reminders(invoices, LocalDate.of(2026, 2, 1)); // Vector<String>
// Vector(Reminder to Ada), every time

// the edge: the one line that reads the clock and acts
var outbox = new java.util.ArrayList<String>();
reminders(invoices, LocalDate.now()).forEach(outbox::add);
```

## Errors as values

A failure the business expects (bad input, unknown customer, declined payment) is a value in the return type, not an
exception. The caller sees it in the signature and cannot forget it.

```java
// before
// throws NumberFormatException or IllegalArgumentException, and only this comment says so
static int parsePortOrThrow(String input) {
    var port = Integer.parseInt(input.trim());
    if (port < 1 || port > 65_535) {
        throw new IllegalArgumentException("port out of range: " + port);
    }
    return port;
}
```

```java
// after
static Either<String, Integer> parsePort(String input) {
    return Try.of(() -> Integer.parseInt(input.trim()))
        .toEither()
        .mapLeft(e -> "not a number: " + input)
        .filterOrElse(p -> p >= 1 && p <= 65_535, p -> "port out of range: " + p);
}
```

```java
var message = switch (parsePort("80800")) {
    case Right(var port) -> "listening on " + port;
    case Left(var error) -> "bad config: " + error;
};
// "bad config: port out of range: 80800"
```

Pick the type by what the caller needs:

- `Option`: the value may be absent, and there is nothing to say about why.
- `Either`: a result or an error; the first error stops the chain. The default for business failures.
- `Validation`: independent checks; every error is kept, as for a form.
- `Try`: wraps code that throws, typically a JDK or library call, at the boundary. Convert it with `toEither()`
  right away.

```java
record ServerConfig(String host, int port) {}

static Validation<String, String> checkHost(String host) {
    return Validation.fromPredicate(host.trim(), h -> !h.isEmpty(), h -> "host is required");
}
```

```java
var config = Validation.zipWith( // Validation<String, ServerConfig>
    checkHost(" "), parsePort("http").toValidation(), ServerConfig::new);
// Invalid(host is required, not a number: http)
```

Throw an exception only for a bug (a broken invariant, an argument no caller should pass) or a failing environment
(database down, disk full), which no caller can handle where it happens. Let those reach the layer that retries or
reports them.

Pages: https://zazr.dev/control/either/, https://zazr.dev/control/validation/, https://zazr.dev/control/try/.

## Parse, don't validate

A check that returns `boolean` forgets what it learnt: the caller still holds a `String`, and every method after it
checks again or hopes. Parse the input once, at the edge, into a type that can only hold good data.

```java
// before
static boolean isValidSku(String input) {
    return input.matches("[A-Z]{3}-\\d{4}");
}
```

After, the compact constructor rejects bad data (reaching it with bad data is a bug), and `parse` is how untrusted
input comes in:

```java
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
```

```java
var good = Sku.parse(" abc-1234 "); // Either<String, Sku>
var bad = Sku.parse("abc");         // Either<String, Sku>
// Right(Sku[value=ABC-1234]), Left(not a SKU: abc)
```

Methods downstream take a `Sku`, never a `String`. The same goes for collections: parse a `Vector` into a
`NonEmptyVector` with `toNonEmptyVector()` (see Total functions).

## Make invalid states impossible to represent

Fields that must agree with each other (a flag plus nullable fields) let wrong combinations exist. Model each state
as its own record under a sealed interface, holding exactly the data of that state.

```java
// before
final class Payment {
    String method;     // "card" or "transfer"
    String cardNumber; // set when method is "card", hopefully
    String iban;       // set when method is "transfer", hopefully
}
```

```java
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
```

Pattern matching over a sealed interface is exhaustive. Write no `default` branch: without it, adding a case later
makes every `switch` that misses it stop compiling:

```text
error: the switch expression does not cover all possible input values
```

`Option`, `Either`, `Try` and `Validation` are built the same way. Use an `enum` when the states carry no data.

## Total functions

A total function returns a result for every input: no exception for an empty list, no `null` for a missing key. Make
the type say what can be absent, so the compiler checks it.

```java
// before
static int highestScoreOrThrow(java.util.List<Integer> scores) {
    return scores.stream().max(Integer::compare).orElseThrow(); // throws on an empty list
}
```

After, the argument cannot be empty, so `max` cannot fail:

```java
static int highestScore(NonEmptyVector<Integer> scores) {
    return scores.max(Integer::compare);
}
```

```java
var scores = Vector.of(12, 40, 7).toNonEmptyVector(); // Option<NonEmptyVector<Integer>>
var best = scores.map(s -> highestScore(s));        // Option<Integer>
// Some(40); an empty Vector gives None
```

Return `Option` where the JDK returns `null` or throws. Zazr already does: `HashMap.get`, `find`, `headOption`,
`lastOption`, `reduceOption`, `maxBy` on a `Vector`. No Zazr value or collection holds `null`; turn a nullable value
from a JDK or library call into an `Option` with `Option.ofNullable`, e.g.
`Option.ofNullable(System.getenv("HOME"))`.

See https://zazr.dev/non-empty-vector/ for which `NonEmptyVector` operations keep it non-empty.

## Composition

Write small functions that each do one thing, then combine them. `map` transforms a value, `flatMap` chains a step
that may itself fail, `zipWith` combines independent values. No `if` on `null`, no early `return`.

```java
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
```

```java
// after
static String greet(String name, String language) {
    return language.equals("fr") ? "Bonjour " + name : "Hello " + name;
}

static Option<String> greeting(HashMap<String, String> names, HashMap<String, String> languages, String id) {
    return names.get(id).zipWith(languages.get(id), (name, language) -> greet(name, language));
}
```

`flatMap` for steps that depend on the previous result, the static `forEach` to apply a failing step to every element:

```java
var address = parsePort("8080") // Either<String, String>
    .flatMap(p -> p < 1_024 ? Either.left("privileged port") : Either.right("localhost:" + p));
var ports = Either.forEach(Vector.of("80", "443", "x"), s -> parsePort(s)); // Either<String, Vector<Integer>>
// Right(localhost:8080), Left(not a number: x)
```

The static `zipWith` takes up to eight values at once (`Option.zipWith`, `Either.zipWith`, `Try.zipWith`,
`Validation.zipWith`); `collectAll` turns a collection of values into one value of a `Vector`. See
https://zazr.dev/zip/ and https://zazr.dev/control/. The names come from ZIO: `zip`, `zipWith`, `collectAll`,
`forEach`, `mapBoth`, `tap`, `catchAll`, `flip`.

## Laziness where it helps

Compute a costly value only when it is needed, and only once. Do not hand-write the cache.

```java
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
```

```java
// after
final class LazyCatalogue {
    private final Lazy<Vector<String>> items = Lazy.of(() -> Vector.of("book", "pen"));

    Vector<String> items() {
        return items.get();
    }
}
```

`Lazy` computes on the first `get()`, caches the result, and is safe to share between threads. A computation that
throws is not cached; the next `get()` retries. See https://zazr.dev/control/lazy/.

For a sequence computed on demand, possibly infinite, use Zazr's `Stream` instead of a `while` loop with a counter.
The two snippets below use a static `isPrime(int)` method.

```java
// before
var found = new java.util.ArrayList<Integer>();
var candidate = 2;
while (found.size() < 5) {
    if (isPrime(candidate)) {
        found.add(candidate);
    }
    candidate++;
}
```

```java
// after
var primes = Stream.from(2).filter(n -> isPrime(n)).take(5).toVector(); // Vector<Integer>
// Vector(2, 3, 5, 7, 11)
```

Zazr's `Stream` is a lazy list that keeps what it computed, so it can be read many times; `java.util.stream.Stream`
is a one-shot pipeline. On an infinite `Stream`, call `take` or `takeWhile` before anything that reads every
element (`toVector`, `foldLeft`, `size`). See https://zazr.dev/collections/stream/.

## Java 25 features to use

- Records for data, with a compact constructor for invariants.
- Sealed interfaces for a closed set of cases.
- Pattern matching with a `switch` expression; record patterns take a case apart in one step, nested as deep as
  needed.
- `when` guards on a case.
- The unnamed pattern `_` for a component you do not read, and for an unused lambda parameter.
- `var` for locals, with the type in a trailing comment when it is not obvious.

Before, type tests, casts, `null` and a `throw` for the case nobody can check:

```java
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
```

After, no cast and no `throw`, and the compiler checks that every case is covered:

```java
static String summary(Option<Payment> payment) {
    return switch (payment) {
        case Some(Card(var number)) when number.startsWith("4") -> "Visa card";
        case Some(Card(_)) -> "other card";
        case Some(Transfer(_)) -> "bank transfer";
        case None() -> "not paid yet";
    };
}
```

```java
var paid = summary(Option.some(new Transfer("FR76 3000 6000 0112 3456 7890 189")));
var unpaid = summary(Option.none());
var count = Vector.of("a", "b", "c").foldLeft(0, (n, _) -> n + 1); // Integer
// "bank transfer", "not paid yet", 3
```

All of these are final in Java 25: they need no `--enable-preview`.

## Read more

- https://zazr.dev/new-to-fp/: the same ideas, explained step by step.
- https://zazr.dev/principles/: the design of Zazr's API.
- https://zazr.dev/control/: which control type to pick.
- https://zazr.dev/collections/: which collection to pick.
