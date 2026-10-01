package dev.zazr.test.docs;

import dev.zazr.Tuple;
import dev.zazr.Tuple2;
import dev.zazr.collection.List;
import dev.zazr.collection.Vector;
import dev.zazr.control.Option;
import dev.zazr.control.Validation;
import dev.zazr.test.Assertion;
import dev.zazr.test.Check;
import dev.zazr.test.CheckConfig;
import dev.zazr.test.CheckResult;
import dev.zazr.test.Gen;
import dev.zazr.test.TestResult;
import dev.zazr.test.laws.MapLaws;
import dev.zazr.test.laws.MapSubject;
import java.time.DayOfWeek;
import java.util.EnumSet;
import java.util.function.Function;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static dev.zazr.test.Assertion.*;
import static dev.zazr.test.Assertion.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The fenced {@code java} blocks of docs/testing.md and of the blog post docs/blog/posts/property-based-testing.md,
 * pasted verbatim, compiled and run (zazr-core's
 * {@code DocsExamplesTest} cannot see zazr-test, which depends on zazr-core). {@code make docs-examples} fails when a
 * block of that page is missing from this file. Each test also checks the static types and the results the page
 * states in its comments, and the text blocks of the page.
 */
public class DocsTestingExamplesTest {

    /// The example of "Running in your test suite": an ordinary JUnit test class, run here as a nested test class.
    @Nested
    class ListReverseTest {

        @Test
        void reversingTwiceGivesTheListBack() {
            var lists = Gen.list(Gen.integers()); // Gen<List<Integer>>
            Check.check(lists, list -> assertThat(list.reverse().reverse(), equalTo(list)));
        }
    }

    @Test
    void aFailingCheckIsAnAssertionErrorWithTheCounterexampleTheSampleNumberAndTheSeed() {
        // the failure the page shows in Maven's output, after "ListShortTest.everyListIsShort:13 "
        assertThatThrownBy(() -> Check.check(
                        CheckConfig.defaults().withSeed(42), Gen.list(Gen.integers()), list -> list.size() < 5))
                .isExactlyInstanceOf(AssertionError.class)
                .hasMessage(
                        "falsified at sample 14 by (List(1064429137, -1, 2147483646, -499641955, 2147483647)) (seed 42, replay with -Dzazr.check.seed=42)");
    }

    @Test
    void aFirstProperty() {
        var lists = Gen.list(Gen.integers()); // Gen<List<Integer>>
        Check.check(lists, list -> assertThat(list.reverse().reverse(), equalTo(list)));

        Gen<List<Integer>> typed = lists;
        assertThat(Check.evaluate(typed, list -> list.reverse().reverse().equals(list)))
                .isEqualTo(new CheckResult.Satisfied(200));
    }

    @Test
    void whatAFailurePrints() {
        assertThatThrownBy(() -> {
                    var config = CheckConfig.defaults().withSeed(42); // CheckConfig
                    Check.check(config, Gen.list(Gen.integers()), hasSize(isLessThan(5))); // throws an AssertionError
                })
                .isExactlyInstanceOf(AssertionError.class)
                .hasMessage(
                        "falsified at sample 14 by (List(1064429137, -1, 2147483646, -499641955, 2147483647)) (seed 42, replay with -Dzazr.check.seed=42):\n"
                                + "  List(1064429137, -1, 2147483646, -499641955, 2147483647) has size 5:\n"
                                + "    5 is not less than 5");
    }

    @Test
    void assertions() {
        var result = assertThat(Option.some(4), isSome(isGreaterThan(5))); // TestResult
        // Failure: "Some(4) holds 4:\n  4 is not greater than 5"

        TestResult typed = result;
        assertThat(typed).isEqualTo(TestResult.fail("Some(4) holds 4:\n  4 is not greater than 5"));
    }

    @Test
    void combiningAssertions() {
        var digit = isGreaterThanOrEqualTo(0).and(isLessThan(10)).label("a digit"); // Assertion<Integer>
        var result = assertThat(12, digit); // TestResult
        // Failure: "a digit: 12 is not less than 10"

        Assertion<Integer> typedDigit = digit;
        TestResult typedResult = result;
        assertThat(typedDigit).hasToString("a digit");
        assertThat(typedResult).isEqualTo(TestResult.fail("a digit: 12 is not less than 10"));
    }

    @Test
    void severalAssertionsAtOnce() {
        var result = assertThat(15, isLessThan(10), not(equalTo(15))); // TestResult
        // Failure: "15 is not less than 10\n15 satisfies equalTo(15), but must not"
        Check.check(Gen.integers(0, 9), isGreaterThanOrEqualTo(0), isLessThan(10));

        TestResult typed = result;
        assertThat(typed).isEqualTo(TestResult.fail("15 is not less than 10\n15 satisfies equalTo(15), but must not"));
    }

    @Test
    void resultsCombineInABody() {
        var ints = Gen.integers(); // Gen<Integer>
        Check.check(ints, ints, (a, b) -> assertThat(a + b, equalTo(b + a)).and(assertThat(a * b, equalTo(b * a))));

        Gen<Integer> typed = ints;
        assertThat(typed).isNotNull();
    }

    @Test
    void codeThatMustThrow() {
        var result = assertThat(() -> Integer.parseInt("x"), throwsA(NumberFormatException.class)); // TestResult
        // Success

        TestResult typed = result;
        assertThat(typed).isEqualTo(TestResult.succeed());
    }

    @Test
    void smallCounterexamplesFirst() {
        var result =
                Check.evaluate(CheckConfig.defaults().withSeed(42), Gen.list(Gen.integers()), list -> list.size() < 5);
        // the first list that breaks the property has exactly 5 elements
        assertThat(result)
                .isInstanceOfSatisfying(
                        CheckResult.Falsified.class,
                        falsified -> assertThat(((List<?>) falsified
                                                .counterexample()
                                                .toVector()
                                                .head())
                                        .size())
                                .isEqualTo(5));
    }

    @Test
    void readingAResult() {
        var result =
                Check.evaluate(CheckConfig.defaults().withSeed(42), Gen.integers(0, 1000), n -> n < 500); // CheckResult
        var summary = switch (result) {
            case CheckResult.Satisfied(var samples) -> "passed " + samples + " samples";
            case CheckResult.Falsified(var sampleNumber, var _, var counterexample, var _) ->
                "broken at sample " + sampleNumber + " by " + counterexample;
            case CheckResult.Erroneous(var sampleNumber, var _, var cause, var _) ->
                "failed at sample " + sampleNumber + " with " + cause;
        };
        // "broken at sample 3 by (1000)"

        CheckResult typed = result;
        assertThat(typed.isFalsified()).isTrue();
        assertThat(summary).isEqualTo("broken at sample 3 by (1000)");
    }

    @Test
    void assertionsInTheProperty() {
        var digits = Gen.vector(Gen.integers(0, 9)); // Gen<Vector<Integer>>
        var config = CheckConfig.defaults().withSeed(42); // CheckConfig
        // CheckResult
        var result = Check.evaluate(
                config, digits, vector -> assertThat(vector.distinct()).isEqualTo(vector));
        var message = result.message(); // Option<String>
        // Some("expected: Vector(9, 1, 2, 9, 8, 9) but was: Vector(9, 1, 2, 8)"), AssertJ's message on three lines

        Gen<Vector<Integer>> typedDigits = digits;
        CheckConfig typedConfig = config;
        assertThat(typedConfig.seed()).isEqualTo(42L);
        CheckResult typedResult = result;
        Option<String> typedMessage = message;
        assertThat(typedDigits).isNotNull();
        assertThat(typedResult.isFalsified()).isTrue();
        assertThat(typedMessage.get())
                .isEqualToIgnoringWhitespace("expected: Vector(9, 1, 2, 9, 8, 9) but was: Vector(9, 1, 2, 8)");

        var thrown = Check.evaluate(Gen.integers(), n -> {
            throw new IllegalStateException("boom");
        });
        assertThat(thrown)
                .isInstanceOfSatisfying(
                        CheckResult.Erroneous.class,
                        erroneous -> assertThat(erroneous.cause()).isInstanceOf(IllegalStateException.class));
    }

    @Test
    void combiningGenerators() {
        var dice = Gen.integers(1, 6); // Gen<Integer>
        var twoDice = dice.zipWith(dice, Integer::sum); // Gen<Integer>
        var coin = Gen.elements("heads", "tails"); // Gen<String>
        // Gen<String>
        var loadedCoin = Gen.weighted(Tuple.of(Gen.constant("heads"), 9.0), Tuple.of(Gen.constant("tails"), 1.0));
        Check.check(twoDice, isWithin(2, 12));

        Gen<Integer> typedDice = dice;
        Gen<Integer> typedTwoDice = twoDice;
        Gen<String> typedCoin = coin;
        Gen<String> typedLoadedCoin = loadedCoin;
        assertThat(typedDice.runCollectN(200).toSet()).containsExactlyInAnyOrder(1, 2, 3, 4, 5, 6);
        assertThat(typedTwoDice.runCollectN(200).forAll(sum -> sum >= 2 && sum <= 12))
                .isTrue();
        assertThat(typedCoin.runCollectN(200).toSet()).containsExactlyInAnyOrder("heads", "tails");
        assertThat(typedLoadedCoin
                        .runCollectN(1_000, CheckConfig.defaults().withSeed(42))
                        .count("heads"::equals))
                .isBetween(850, 950);
    }

    @Test
    void finiteGenerators() {
        var sizes = Gen.fromIterable(Vector.of("S", "M")); // Gen<String>
        var colours = Gen.fromIterable(Vector.of("red", "blue")); // Gen<String>
        var variants = sizes.zip(colours).runCollect(); // List<Tuple2<String, String>>
        // List((S, red), (S, blue), (M, red), (M, blue))

        Gen<String> typedSizes = sizes;
        Gen<String> typedColours = colours;
        List<Tuple2<String, String>> typedVariants = variants;
        assertThat(typedSizes).isNotNull();
        assertThat(typedColours).isNotNull();
        assertThat(typedVariants).hasToString("List((S, red), (S, blue), (M, red), (M, blue))");
    }

    @Test
    void checkAll() {
        var days = Gen.fromIterable(EnumSet.allOf(DayOfWeek.class)); // Gen<DayOfWeek>
        var result = Check.evaluateAll(days, day -> day.plus(7) == day); // CheckResult
        // Satisfied[samples=7]

        Gen<DayOfWeek> typedDays = days;
        CheckResult typedResult = result;
        assertThat(typedDays).isNotNull();
        assertThat(typedResult).hasToString("Satisfied[samples=7]");

        // elements is random: checkAll sees one of its values, not each of them
        assertThat(Check.evaluateAll(Gen.elements("a", "b", "c"), s -> true)).isEqualTo(new CheckResult.Satisfied(1));
    }

    @Test
    void sizedGenerators() {
        var depth = Gen.sized(size -> Gen.integers(0, size)); // Gen<Integer>
        var words = Gen.small(size -> Gen.stringsN(size, Gen.alphaChars())); // Gen<String>
        // Gen<List<Integer>>, at most 3 elements
        var shortLists = Gen.list(Gen.integers()).withSize(3);

        var config = CheckConfig.defaults().withSize(50);
        Gen<Integer> typedDepth = depth;
        Gen<String> typedWords = words;
        Gen<List<Integer>> typedShortLists = shortLists;
        assertThat(typedDepth.runCollectN(200, config).forAll(d -> d >= 0 && d <= 50))
                .isTrue();
        assertThat(typedWords.runCollectN(200, config).forAll(w -> w.length() <= 50))
                .isTrue();
        assertThat(typedShortLists.runCollectN(200, config).forAll(l -> l.size() <= 3))
                .isTrue();
    }

    @Test
    void theSizeGrowsOverACheck() {
        var sizes = Gen.size().runCollectN(5, CheckConfig.defaults().withSize(100)); // List<Integer>
        // List(0, 25, 50, 75, 100)

        List<Integer> typedSizes = sizes;
        assertThat(typedSizes).isEqualTo(List.of(0, 25, 50, 75, 100));
    }

    @Test
    void filtering() {
        var evens = Gen.integers(-1000, 1000).filter(n -> n % 2 == 0); // Gen<Integer>
        // Gen<Integer>, with no rejected value
        var alsoEvens = Gen.integers(-500, 500).map(n -> n * 2);
        var nonEmpty = Gen.list(Gen.integers()).filter(list -> !list.isEmpty()); // Gen<List<Integer>>
        // CheckResult
        var impossible = Check.evaluate(Gen.integers().filter(n -> false), n -> true);
        // Erroneous: Gen.filter rejected too many values: 1001 discards since the last sample, more than the discard
        // budget of 1000; ...

        Gen<Integer> typedEvens = evens;
        Gen<Integer> typedAlsoEvens = alsoEvens;
        Gen<List<Integer>> typedNonEmpty = nonEmpty;
        CheckResult typedImpossible = impossible;
        Check.check(typedNonEmpty, list -> !list.isEmpty());
        assertThat(typedEvens.runCollectN(200).forAll(n -> n % 2 == 0)).isTrue();
        assertThat(typedAlsoEvens.runCollectN(200).forAll(n -> n % 2 == 0)).isTrue();
        assertThat(typedImpossible)
                .isInstanceOfSatisfying(
                        CheckResult.Erroneous.class,
                        erroneous -> assertThat(erroneous.cause())
                                .isInstanceOf(IllegalStateException.class)
                                .hasMessageStartingWith(
                                        "Gen.filter rejected too many values: 1001 discards since the last sample, more than the discard budget of 1000; "));
    }

    @Test
    void configuration() {
        var config = CheckConfig.defaults().withSamples(1_000).withSeed(42); // CheckConfig
        Check.check(config, Gen.integers(), n -> assertThat(Integer.parseInt(Integer.toString(n)), equalTo(n)));
        Check.checkN(50, Gen.alphaNumericStrings(), s -> s.strip().equals(s));

        CheckConfig typed = config;
        assertThat(typed).isEqualTo(new CheckConfig(1_000, 100, 42, 1_000));
        assertThat(Check.evaluate(config, Gen.integers(), n -> true)).isEqualTo(new CheckResult.Satisfied(1_000));
        assertThat(Check.evaluateN(50, Gen.integers(), n -> true)).isEqualTo(new CheckResult.Satisfied(50));
        // the defaults the page's table lists
        assertThat(CheckConfig.DEFAULT_SAMPLES).isEqualTo(200);
        assertThat(CheckConfig.DEFAULT_SIZE).isEqualTo(100);
        assertThat(CheckConfig.DEFAULT_MAX_DISCARDS).isEqualTo(1000);
        assertThat(CheckConfig.SAMPLES_PROPERTY).isEqualTo("zazr.check.samples");
        assertThat(CheckConfig.SIZE_PROPERTY).isEqualTo("zazr.check.size");
        assertThat(CheckConfig.SEED_PROPERTY).isEqualTo("zazr.check.seed");
        assertThat(CheckConfig.MAX_DISCARDS_PROPERTY).isEqualTo("zazr.check.maxDiscards");
    }

    @Test
    void replayingAFailure() {
        var first =
                Check.evaluate(CheckConfig.defaults().withSeed(42), Gen.list(Gen.integers()), list -> list.size() < 5);
        var again =
                Check.evaluate(CheckConfig.defaults().withSeed(42), Gen.list(Gen.integers()), list -> list.size() < 5);
        assertThat(again).isEqualTo(first);
    }

    @Test
    void generatorsForEveryZazrType() {
        var checks = Gen.validation(
                Gen.elements("too short", "no digit"), Gen.integers()); // Gen<Validation<String, Integer>>
        Check.check(checks, checks, (a, b) -> assertThat(a.zip(b).isValid(), equalTo(a.isValid() && b.isValid())));

        Gen<Validation<String, Integer>> typed = checks;
        assertThat(typed).isNotNull();

        // lengths favour 0, 1, the size and the size minus one
        var lengths = Gen.vector(Gen.integers())
                .withSize(20)
                .runCollectN(1_000, CheckConfig.defaults().withSeed(42))
                .map(Vector::size);
        assertThat(lengths.count(n -> n == 0 || n == 1 || n == 19 || n == 20)).isBetween(450, 650);
    }

    @Test
    void checkingYourOwnType() {
        record Box(Vector<Object> items) {
            Box map(Function<Object, Object> f) {
                return new Box(items.map(f));
            }
        }
        var boxes = new MapSubject<Box>() {
            public Gen<Box> values() {
                return Gen.vector(Gen.integers(-100, 100)).map(v -> new Box(v.map(x -> (Object) x)));
            }

            public Box map(Box box, Function<Object, Object> f) {
                return box.map(f);
            }
        }; // MapSubject<Box>
        MapLaws.<Box>all().assertSatisfied(boxes);
    }

    @Test
    void whenALawFails() {
        // the subject of the previous example, which the page's broken subject reuses
        record Box(Vector<Object> items) {
            Box map(Function<Object, Object> f) {
                return new Box(items.map(f));
            }
        }
        var boxes = new MapSubject<Box>() {
            public Gen<Box> values() {
                return Gen.vector(Gen.integers(-100, 100)).map(v -> new Box(v.map(x -> (Object) x)));
            }

            public Box map(Box box, Function<Object, Object> f) {
                return box.map(f);
            }
        }; // MapSubject<Box>

        assertThatThrownBy(() -> {
                    var broken = new MapSubject<Box>() {
                        public Gen<Box> values() {
                            return boxes.values();
                        }

                        public Box map(Box box, Function<Object, Object> f) {
                            return new Box(box.map(f).items().dropRight(1));
                        }
                    }; // MapSubject<Box>
                    MapLaws.<Box>all()
                            .assertSatisfied(broken, CheckConfig.defaults().withSeed(42)); // throws an AssertionError
                })
                .isInstanceOf(AssertionError.class)
                .hasMessage("""
                2 law(s) failed:
                mapIdentity: falsified at sample 5 by (Box[items=Vector(6)]): left = Box[items=Vector()], right = Box[items=Vector(6)] (seed 42, replay with -Dzazr.check.seed=42)
                mapComposition: falsified at sample 7 by (Box[items=Vector(99, -6)], x -> -1 * x + -9, x -> -10 * x + 80): left = Box[items=Vector()], right = Box[items=Vector(1160)] (seed 42, replay with -Dzazr.check.seed=42)""");
    }

    /// The blog post "Property-based testing with zazr-test" (docs/blog/posts/property-based-testing.md). The
    /// failures the post prints are checked with the seed 42, as the post says.
    @Nested
    class BlogPropertyBasedTesting {

        static int midpoint(int a, int b) {
            return (a + b) / 2;
        }

        private static final Gen<Integer> NATURALS = Gen.integers(0, Integer.MAX_VALUE);

        @Nested
        class ReverseTest {

            @Test
            void reversingTwiceGivesTheListBack() {
                var lists = Gen.list(Gen.integers()); // Gen<List<Integer>>
                Check.check(lists, list -> list.reverse().reverse().equals(list));
            }
        }

        @Test
        void whenAPropertyBreaks() {
            class MidpointTest {

                static int midpoint(int a, int b) {
                    return (a + b) / 2;
                }

                @Test
                void theMidpointIsBetweenItsArguments() {
                    var naturals = Gen.integers(0, Integer.MAX_VALUE); // Gen<Integer>
                    Check.check(naturals, naturals, (a, b) -> {
                        var mid = midpoint(a, b);
                        return mid >= Math.min(a, b) && mid <= Math.max(a, b);
                    });
                }
            }

            assertThatThrownBy(() -> new MidpointTest().theMidpointIsBetweenItsArguments())
                    .isExactlyInstanceOf(AssertionError.class)
                    .hasMessageStartingWith("falsified at sample ");
            // the message Maven prints after "MidpointTest.theMidpointIsBetweenItsArguments:14 ", with the seed 42
            assertThatThrownBy(() -> Check.check(CheckConfig.defaults().withSeed(42), NATURALS, NATURALS, (a, b) -> {
                        var mid = midpoint(a, b);
                        return mid >= Math.min(a, b) && mid <= Math.max(a, b);
                    }))
                    .isExactlyInstanceOf(AssertionError.class)
                    .hasMessage(
                            "falsified at sample 2 by (2147483647, 513683364) (seed 42, replay with -Dzazr.check.seed=42)");
            // the fix the post gives
            assertThat(Check.evaluate(CheckConfig.defaults().withSeed(42), NATURALS, NATURALS, (a, b) -> {
                        var mid = a + (b - a) / 2;
                        return mid >= Math.min(a, b) && mid <= Math.max(a, b);
                    }))
                    .isEqualTo(new CheckResult.Satisfied(200));
        }

        @Test
        void generators() {
            record User(String name, int age) {}

            var names = Gen.alphaNumericStrings(); // Gen<String>
            var ages = Gen.integers(0, 120); // Gen<Integer>
            var users = names.zipWith(ages, User::new); // Gen<User>
            var teams = Gen.vector(users); // Gen<Vector<User>>
            var lookups = Gen.option(users); // Gen<Option<User>>
            var byName = Gen.hashMap(names, users); // Gen<HashMap<String, User>>
            Check.check(
                    teams,
                    team -> team.sortBy(User::age)
                            .map(User::age)
                            .equals(team.map(User::age).sorted()));

            Gen<Vector<User>> typedTeams = teams;
            Gen<Option<User>> typedLookups = lookups;
            Gen<dev.zazr.collection.HashMap<String, User>> typedByName = byName;
            assertThat(typedTeams.runCollectN(3, CheckConfig.defaults())).hasSize(3);
            assertThat(typedLookups.runCollectN(3, CheckConfig.defaults())).hasSize(3);
            assertThat(typedByName.runCollectN(3, CheckConfig.defaults())).hasSize(3);
        }

        @Test
        void finiteGenerators() {
            var days = Gen.fromIterable(EnumSet.allOf(DayOfWeek.class)); // Gen<DayOfWeek>
            Check.checkAll(days, day -> DayOfWeek.of(day.getValue()) == day);

            assertThat(Check.evaluateAll(days, day -> DayOfWeek.of(day.getValue()) == day))
                    .isEqualTo(new CheckResult.Satisfied(7));
        }

        @Test
        void assertionsThatExplainFailures() {
            assertThatThrownBy(() -> {
                        var naturals = Gen.integers(0, Integer.MAX_VALUE); // Gen<Integer>
                        Check.check(
                                naturals,
                                naturals,
                                (a, b) -> assertThat(midpoint(a, b), isWithin(Math.min(a, b), Math.max(a, b))));
                    })
                    .isExactlyInstanceOf(AssertionError.class);
            assertThatThrownBy(() -> Check.check(
                            CheckConfig.defaults().withSeed(42),
                            NATURALS,
                            NATURALS,
                            (a, b) -> assertThat(midpoint(a, b), isWithin(Math.min(a, b), Math.max(a, b)))))
                    .isExactlyInstanceOf(AssertionError.class)
                    .hasMessage(
                            "falsified at sample 2 by (2147483647, 513683364): -816900142 is not within 513683364 and 2147483647 (seed 42, replay with -Dzazr.check.seed=42)");
        }

        @Test
        void combiningAssertions() {
            // Assertion<Integer>
            var isPercentage =
                    isGreaterThanOrEqualTo(0).and(isLessThanOrEqualTo(100)).label("a percentage");
            var result = assertThat(Vector.of(20, 150, -3), forall(isPercentage), hasSize(isLessThan(3)));

            var sortedScores = Gen.vector(Gen.integers(0, 100)).map(Vector::sorted); // Gen<Vector<Integer>>
            Check.check(sortedScores, forall(isPercentage), isSorted());

            Assertion<Integer> typed = isPercentage;
            assertThat(assertThat(150, typed))
                    .isEqualTo(new TestResult.Failure("a percentage: 150 is greater than 100"));
            assertThat(result).isEqualTo(new TestResult.Failure("""
                    Vector(20, 150, -3) has 150 at index 1:
                      a percentage: 150 is greater than 100
                    Vector(20, 150, -3) has size 3:
                      3 is not less than 3"""));
            Gen<Vector<Integer>> typedScores = sortedScores;
            assertThat(Check.evaluate(typedScores, forall(isPercentage), isSorted()))
                    .isEqualTo(new CheckResult.Satisfied(200));
        }

        @Test
        void readingTheResult() {
            var naturals = NATURALS;
            var config = CheckConfig.defaults().withSeed(42); // CheckConfig
            var result = Check.evaluate(config, naturals, naturals, (a, b) -> midpoint(a, b) >= 0); // CheckResult
            var summary = switch (result) {
                case CheckResult.Satisfied(var samples) -> "passed " + samples + " samples";
                case CheckResult.Falsified(var sample, var seed, var counterexample, var _) ->
                    "sample " + sample + " broke it: " + counterexample + ", seed " + seed;
                case CheckResult.Erroneous(var sample, var _, var cause, var _) ->
                    "sample " + sample + " threw " + cause;
            };
            // "sample 2 broke it: (2147483647, 513683364), seed 42"

            assertThat(summary).isEqualTo("sample 2 broke it: (2147483647, 513683364), seed 42");
            assertThat(result.isFalsified()).isTrue();
        }

        @Test
        void replayingAFailure() {
            var naturals = NATURALS;
            assertThatThrownBy(() -> {
                        var config = CheckConfig.defaults().withSeed(42); // CheckConfig
                        Check.check(
                                config, naturals, naturals, (a, b) -> midpoint(a, b) >= 0); // throws an AssertionError
                    })
                    .isExactlyInstanceOf(AssertionError.class)
                    .hasMessage(
                            "falsified at sample 2 by (2147483647, 513683364) (seed 42, replay with -Dzazr.check.seed=42)");
        }

        @Test
        void lawsForYourOwnTypes() {
            assertThatThrownBy(() -> {
                        record Bag(Vector<?> items) {
                            Bag map(Function<Object, Object> f) {
                                return new Bag(items.map(f).distinct());
                            }
                        }
                        var bags = new MapSubject<Bag>() {
                            public Gen<Bag> values() {
                                return Gen.vector(Gen.integers(0, 9)).map(Bag::new);
                            }

                            public Bag map(Bag bag, Function<Object, Object> f) {
                                return bag.map(f);
                            }
                        }; // MapSubject<Bag>
                        MapLaws.<Bag>all().assertSatisfied(bags); // throws an AssertionError
                    })
                    .isInstanceOf(AssertionError.class)
                    .hasMessageStartingWith("1 law(s) failed:\nmapIdentity: falsified at sample ");

            // the same, with the seed 42 of the failure the post prints
            assertThatThrownBy(() -> {
                        record Bag(Vector<?> items) {
                            Bag map(Function<Object, Object> f) {
                                return new Bag(items.map(f).distinct());
                            }
                        }
                        var bags = new MapSubject<Bag>() {
                            public Gen<Bag> values() {
                                return Gen.vector(Gen.integers(0, 9)).map(Bag::new);
                            }

                            public Bag map(Bag bag, Function<Object, Object> f) {
                                return bag.map(f);
                            }
                        }; // MapSubject<Bag>
                        MapLaws.<Bag>all()
                                .assertSatisfied(bags, CheckConfig.defaults().withSeed(42)); // throws an AssertionError
                    })
                    .isInstanceOf(AssertionError.class)
                    .hasMessage("""
                    1 law(s) failed:
                    mapIdentity: falsified at sample 13 by (Bag[items=Vector(9, 1, 2, 9, 8, 9)]): left = Bag[items=Vector(9, 1, 2, 8)], right = Bag[items=Vector(9, 1, 2, 9, 8, 9)] (seed 42, replay with -Dzazr.check.seed=42)""");
        }
    }
}
