package com.guizmaii.zazr.test.docs;

import com.guizmaii.zazr.Tuple;
import com.guizmaii.zazr.Tuple2;
import com.guizmaii.zazr.collection.List;
import com.guizmaii.zazr.collection.Vector;
import com.guizmaii.zazr.control.Option;
import com.guizmaii.zazr.control.Validation;
import com.guizmaii.zazr.test.Check;
import com.guizmaii.zazr.test.CheckConfig;
import com.guizmaii.zazr.test.CheckResult;
import com.guizmaii.zazr.test.Gen;
import com.guizmaii.zazr.test.laws.MapLaws;
import com.guizmaii.zazr.test.laws.MapSubject;
import java.time.DayOfWeek;
import java.util.EnumSet;
import java.util.function.Function;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The fenced {@code java} blocks of docs/testing.md, pasted verbatim, compiled and run (zazr-core's
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
            Check.check(lists, list -> list.reverse().reverse().equals(list)).assertIsSatisfied();
        }
    }

    @Test
    void aFailingCheckIsAnAssertionErrorWithTheCounterexampleTheSampleNumberAndTheSeed() {
        // the failure the page shows in Maven's output, after "ListShortTest.everyListIsShort:13 "
        assertThatThrownBy(() -> Check.check(CheckConfig.defaults().withSeed(42), Gen.list(Gen.integers()), list -> list.size() < 5)
            .assertIsSatisfied())
            .isExactlyInstanceOf(AssertionError.class)
            .hasMessage("falsified at sample 14 by (List(1064429137, -1, 2147483646, -499641955, 2147483647)) (seed 42, replay with -Dzazr.check.seed=42)");
    }

    @Test
    void aFirstProperty() {
        var lists = Gen.list(Gen.integers()); // Gen<List<Integer>>
        Check.check(lists, list -> list.reverse().reverse().equals(list)).assertIsSatisfied();

        Gen<List<Integer>> typed = lists;
        assertThat(Check.check(typed, list -> list.reverse().reverse().equals(list)))
            .isEqualTo(new CheckResult.Satisfied(200));
    }

    @Test
    void whatAFailurePrints() {
        assertThatThrownBy(() -> {
            var config = CheckConfig.defaults().withSeed(42); // CheckConfig
            // CheckResult
            var shortLists = Check.check(config, Gen.list(Gen.integers()), list -> list.size() < 5);
            shortLists.assertIsSatisfied(); // throws an AssertionError

            CheckConfig typedConfig = config;
            CheckResult typedResult = shortLists;
            assertThat(typedConfig).isNotNull();
            assertThat(typedResult).isNotNull();
        })
            .isInstanceOf(AssertionError.class)
            .hasMessage("falsified at sample 14 by (List(1064429137, -1, 2147483646, -499641955, 2147483647)) (seed 42, replay with -Dzazr.check.seed=42)");
    }

    @Test
    void smallCounterexamplesFirst() {
        var result = Check.check(CheckConfig.defaults().withSeed(42), Gen.list(Gen.integers()), list -> list.size() < 5);
        // the first list that breaks the property has exactly 5 elements
        assertThat(result).isInstanceOfSatisfying(CheckResult.Falsified.class,
            falsified -> assertThat(((List<?>) falsified.counterexample().toVector().head()).size()).isEqualTo(5));
    }

    @Test
    void readingAResult() {
        var result = Check.check(CheckConfig.defaults().withSeed(42), Gen.integers(0, 1000), n -> n < 500); // CheckResult
        var summary = switch (result) {
            case CheckResult.Satisfied(var samples) -> "passed " + samples + " samples";
            case CheckResult.Falsified(var sampleNumber, _, var counterexample, _) -> "broken at sample " + sampleNumber + " by " + counterexample;
            case CheckResult.Erroneous(var sampleNumber, _, var cause, _) -> "failed at sample " + sampleNumber + " with " + cause;
        };
        // "broken at sample 3 by (1000)"

        CheckResult typed = result;
        assertThat(typed.isFalsified()).isTrue();
        assertThat(summary).isEqualTo("broken at sample 3 by (1000)");
    }

    @Test
    void assertionsInTheProperty() {
        var digits = Gen.vector(Gen.integers(0, 9)); // Gen<Vector<Integer>>
        var result = Check.check(CheckConfig.defaults().withSeed(42), digits, vector -> {
            assertThat(vector.distinct()).isEqualTo(vector);
            return true;
        }); // CheckResult
        var message = result.message(); // Option<String>
        // Some("expected: Vector(9, 1, 2, 9, 8, 9) but was: Vector(9, 1, 2, 8)"), AssertJ's message on three lines

        Gen<Vector<Integer>> typedDigits = digits;
        CheckResult typedResult = result;
        Option<String> typedMessage = message;
        assertThat(typedDigits).isNotNull();
        assertThat(typedResult.isFalsified()).isTrue();
        assertThat(typedMessage.get()).isEqualToIgnoringWhitespace("expected: Vector(9, 1, 2, 9, 8, 9) but was: Vector(9, 1, 2, 8)");

        var thrown = Check.check(Gen.integers(), n -> {
            throw new IllegalStateException("boom");
        });
        assertThat(thrown).isInstanceOfSatisfying(CheckResult.Erroneous.class,
            erroneous -> assertThat(erroneous.cause()).isInstanceOf(IllegalStateException.class));
    }

    @Test
    void combiningGenerators() {
        var dice = Gen.integers(1, 6); // Gen<Integer>
        var twoDice = dice.zipWith(dice, Integer::sum); // Gen<Integer>
        var coin = Gen.elements("heads", "tails"); // Gen<String>
        // Gen<String>
        var loadedCoin = Gen.weighted(Tuple.of(Gen.constant("heads"), 9.0), Tuple.of(Gen.constant("tails"), 1.0));
        Check.check(twoDice, sum -> sum >= 2 && sum <= 12).assertIsSatisfied();

        Gen<Integer> typedDice = dice;
        Gen<Integer> typedTwoDice = twoDice;
        Gen<String> typedCoin = coin;
        Gen<String> typedLoadedCoin = loadedCoin;
        assertThat(typedDice.runCollectN(200).toSet()).containsExactlyInAnyOrder(1, 2, 3, 4, 5, 6);
        assertThat(typedTwoDice.runCollectN(200).forAll(sum -> sum >= 2 && sum <= 12)).isTrue();
        assertThat(typedCoin.runCollectN(200).toSet()).containsExactlyInAnyOrder("heads", "tails");
        assertThat(typedLoadedCoin.runCollectN(1_000, CheckConfig.defaults().withSeed(42)).count("heads"::equals))
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
        var result = Check.checkAll(days, day -> day.plus(7) == day); // CheckResult
        // Satisfied[samples=7]

        Gen<DayOfWeek> typedDays = days;
        CheckResult typedResult = result;
        assertThat(typedDays).isNotNull();
        assertThat(typedResult).hasToString("Satisfied[samples=7]");

        // elements is random: checkAll sees one of its values, not each of them
        assertThat(Check.checkAll(Gen.elements("a", "b", "c"), s -> true)).isEqualTo(new CheckResult.Satisfied(1));
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
        assertThat(typedDepth.runCollectN(200, config).forAll(d -> d >= 0 && d <= 50)).isTrue();
        assertThat(typedWords.runCollectN(200, config).forAll(w -> w.length() <= 50)).isTrue();
        assertThat(typedShortLists.runCollectN(200, config).forAll(l -> l.size() <= 3)).isTrue();
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
        var impossible = Check.check(Gen.integers().filter(n -> false), n -> true);
        // Erroneous: Gen.filter rejected too many values: 1001 discards since the last sample, more than the discard budget of 1000; ...

        Gen<Integer> typedEvens = evens;
        Gen<Integer> typedAlsoEvens = alsoEvens;
        Gen<List<Integer>> typedNonEmpty = nonEmpty;
        CheckResult typedImpossible = impossible;
        Check.check(typedNonEmpty, list -> !list.isEmpty()).assertIsSatisfied();
        assertThat(typedEvens.runCollectN(200).forAll(n -> n % 2 == 0)).isTrue();
        assertThat(typedAlsoEvens.runCollectN(200).forAll(n -> n % 2 == 0)).isTrue();
        assertThat(typedImpossible).isInstanceOfSatisfying(CheckResult.Erroneous.class,
            erroneous -> assertThat(erroneous.cause())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageStartingWith("Gen.filter rejected too many values: 1001 discards since the last sample, more than the discard budget of 1000; "));
    }

    @Test
    void configuration() {
        var config = CheckConfig.defaults().withSamples(1_000).withSeed(42); // CheckConfig
        Check.check(config, Gen.integers(), n -> Integer.parseInt(Integer.toString(n)) == n).assertIsSatisfied();
        Check.checkN(50, Gen.alphaNumericStrings(), s -> s.strip().equals(s)).assertIsSatisfied();

        CheckConfig typed = config;
        assertThat(typed).isEqualTo(new CheckConfig(1_000, 100, 42, 1_000));
        assertThat(Check.check(config, Gen.integers(), n -> true)).isEqualTo(new CheckResult.Satisfied(1_000));
        assertThat(Check.checkN(50, Gen.integers(), n -> true)).isEqualTo(new CheckResult.Satisfied(50));
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
        var first = Check.check(CheckConfig.defaults().withSeed(42), Gen.list(Gen.integers()), list -> list.size() < 5);
        var again = Check.check(CheckConfig.defaults().withSeed(42), Gen.list(Gen.integers()), list -> list.size() < 5);
        assertThat(again).isEqualTo(first);
    }

    @Test
    void generatorsForEveryZazrType() {
        var checks = Gen.validation(Gen.elements("too short", "no digit"), Gen.integers()); // Gen<Validation<String, Integer>>
        Check.check(checks, checks, (a, b) -> a.zip(b).isValid() == (a.isValid() && b.isValid())).assertIsSatisfied();

        Gen<Validation<String, Integer>> typed = checks;
        assertThat(typed).isNotNull();

        // lengths favour 0, 1, the size and the size minus one
        var lengths = Gen.vector(Gen.integers()).withSize(20).runCollectN(1_000, CheckConfig.defaults().withSeed(42))
            .map(Vector::size);
        assertThat(lengths.count(n -> n == 0 || n == 1 || n == 19 || n == 20)).isBetween(450, 650);
    }

    @Test
    void checkingYourOwnType() {
        record Box(Vector<Object> items) {
            Box map(Function<Object, Object> f) { return new Box(items.map(f)); }
        }
        var boxes = new MapSubject<Box>() {
            public Gen<Box> values() { return Gen.vector(Gen.integers(-100, 100)).map(v -> new Box(v.map(x -> (Object) x))); }
            public Box map(Box box, Function<Object, Object> f) { return box.map(f); }
        }; // MapSubject<Box>
        MapLaws.<Box>all().assertSatisfied(boxes);
    }

    @Test
    void whenALawFails() {
        // the subject of the previous example, which the page's broken subject reuses
        record Box(Vector<Object> items) {
            Box map(Function<Object, Object> f) { return new Box(items.map(f)); }
        }
        var boxes = new MapSubject<Box>() {
            public Gen<Box> values() { return Gen.vector(Gen.integers(-100, 100)).map(v -> new Box(v.map(x -> (Object) x))); }
            public Box map(Box box, Function<Object, Object> f) { return box.map(f); }
        }; // MapSubject<Box>

        assertThatThrownBy(() -> {
            var broken = new MapSubject<Box>() {
                public Gen<Box> values() { return boxes.values(); }
                public Box map(Box box, Function<Object, Object> f) { return new Box(box.map(f).items().dropRight(1)); }
            }; // MapSubject<Box>
            MapLaws.<Box>all().assertSatisfied(broken, CheckConfig.defaults().withSeed(42)); // throws an AssertionError
        })
            .isInstanceOf(AssertionError.class)
            .hasMessage("""
                2 law(s) failed:
                mapIdentity: falsified at sample 5 by (Box[items=Vector(6)]): left = Box[items=Vector()], right = Box[items=Vector(6)] (seed 42, replay with -Dzazr.check.seed=42)
                mapComposition: falsified at sample 7 by (Box[items=Vector(99, -6)], x -> -1 * x + -9, x -> -10 * x + 80): left = Box[items=Vector()], right = Box[items=Vector(1160)] (seed 42, replay with -Dzazr.check.seed=42)""");
    }
}
