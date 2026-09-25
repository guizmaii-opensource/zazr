package com.guizmaii.zazr.test.docs;

import com.guizmaii.zazr.Tuple;
import com.guizmaii.zazr.Tuple2;
import com.guizmaii.zazr.control.Validation;
import com.guizmaii.zazr.test.legacy.Arbitrary;
import com.guizmaii.zazr.test.legacy.CheckResult;
import com.guizmaii.zazr.test.legacy.Checkable;
import com.guizmaii.zazr.test.legacy.Gen;
import com.guizmaii.zazr.test.legacy.Property;
import java.util.Random;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The fenced {@code java} blocks of docs/testing.md, pasted verbatim, compiled and run (zazr-core's
 * {@code DocsExamplesTest} cannot see zazr-test, which depends on zazr-core). {@code make docs-examples} fails when a
 * block of that page is missing from this file.
 */
public class DocsTestingExamplesTest {

    @Test
    void aProperty() {
        var result = Property.named("reversing twice gives the list back")
            .forAll(Arbitrary.list(Arbitrary.integer()))
            .suchThat(list -> list.reverse().reverse().equals(list))
            .check(); // CheckResult
        result.assertIsSatisfied();

        var broken = Property.named("every list is short")
            .forAll(Arbitrary.list(Arbitrary.integer()))
            .suchThat(list -> list.size() < 5)
            .check(100, 1_000); // CheckResult
        var falsified = broken.isFalsified();
        // true, and broken.sample() holds the first list of 5 elements or more

        assertThat(result.count()).isEqualTo(1_000);
        // the static types the comments state
        CheckResult typedResult = result;
        CheckResult typedBroken = broken;
        assertThat(typedResult).isNotNull();
        assertThat(typedBroken).isNotNull();
        assertThat(falsified).isTrue();
        assertThat(broken.sample().isDefined()).isTrue();
    }

    @Test
    void generators() {
        var dice = Gen.choose(1, 6); // Gen<Integer>
        var pairs = dice.flatMap(a -> dice.map(b -> Tuple.of(a, b))).arbitrary(); // Arbitrary<Tuple2<Integer, Integer>>
        var sums = Property.named("two dice sum to 2..12")
            .forAll(pairs)
            .suchThat(p -> p._1() + p._2() >= 2 && p._1() + p._2() <= 12)
            .check(); // CheckResult
        sums.assertIsSatisfied();

        assertThat(sums.isSatisfied()).isTrue();
        Gen<Integer> typedDice = dice;
        Arbitrary<Tuple2<Integer, Integer>> typedPairs = pairs;
        CheckResult typedSums = sums;
        assertThat(typedDice).isNotNull();
        assertThat(typedPairs).isNotNull();
        assertThat(typedSums).isNotNull();
    }

    @Test
    void preconditions() {
        var halving = Property.named("an even number is twice its half")
            .forAll(Arbitrary.integer())
            .suchThat(n -> n % 2 == 0)
            .implies(n -> (n / 2) * 2 == n); // Checkable
        halving.check().assertIsSatisfied();

        assertThat(halving.check().isSatisfied()).isTrue();
        Checkable typedHalving = halving;
        assertThat(typedHalving).isNotNull();
    }

    @Test
    void readingAResult() {
        var outcome = Property.named("doubling gives an even number")
            .forAll(Arbitrary.integer())
            .suchThat(n -> (n * 2) % 2 == 0)
            .check(); // CheckResult
        var summary = switch (outcome) {
            case CheckResult.Satisfied satisfied -> "passed " + satisfied.count() + " samples";
            case CheckResult.Falsified falsified -> "broken by " + falsified.counterexample();
            case CheckResult.Erroneous erroneous -> "failed with " + erroneous.cause().getMessage();
        };
        // "passed 1000 samples"

        assertThat(summary).isEqualTo("passed 1000 samples");
        CheckResult typedOutcome = outcome;
        assertThat(typedOutcome).isNotNull();
    }

    @Test
    void arbitrariesForEveryType() {
        var checks = Arbitrary.validation(Arbitrary.of("too short", "no digit"), Arbitrary.integer()); // Arbitrary<Validation<String, Integer>>
        Property.named("zip is valid only when both sides are")
            .forAll(checks, checks)
            .suchThat((a, b) -> a.zip(b).isValid() == (a.isValid() && b.isValid()))
            .check()
            .assertIsSatisfied();

        assertThat(checks.apply(10).apply(new Random(1))).isNotNull();
        Arbitrary<Validation<String, Integer>> typedChecks = checks;
        assertThat(typedChecks).isNotNull();
    }
}
