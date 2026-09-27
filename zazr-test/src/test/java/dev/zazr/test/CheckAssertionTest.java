package dev.zazr.test;

import dev.zazr.Tuple;
import dev.zazr.collection.List;
import dev.zazr.control.Option;
import org.junit.jupiter.api.Test;

import static dev.zazr.test.Assertion.equalTo;
import static dev.zazr.test.Assertion.isGreaterThan;
import static dev.zazr.test.Assertion.isLessThan;
import static dev.zazr.test.Assertion.isNonEmpty;
import static dev.zazr.test.Assertion.not;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Checks whose property is an assertion: the {@code check(gen, assertions...)} shortcut and bodies that return a
 * {@link TestResult}.
 */
class CheckAssertionTest {

    private static final CheckConfig CONFIG = new CheckConfig(50, 10, 7L, 100);
    private static final Gen<Integer> FIVE = Gen.constant(5);

    @Test
    void theShortcutPassesWhenEveryValueSatisfiesEveryAssertion() {
        Check.check(Gen.integers(0, 9), isLessThan(10), not(equalTo(-1)));
        Check.check(CONFIG, Gen.integers(0, 9), isLessThan(10));
        Check.checkN(3, Gen.integers(0, 9), isLessThan(10));
        Check.checkAll(Gen.fromIterable(java.util.List.of(1, 2)), isGreaterThan(0));
        Check.checkAll(CONFIG, Gen.fromIterable(java.util.List.of(1, 2)), isGreaterThan(0));
        assertThat(Check.evaluate(Gen.integers(0, 9), isLessThan(10))).isEqualTo(new CheckResult.Satisfied(200));
        assertThat(Check.evaluate(CONFIG, Gen.integers(0, 9), isLessThan(10))).isEqualTo(new CheckResult.Satisfied(50));
        assertThat(Check.evaluateN(3, Gen.integers(0, 9), isLessThan(10))).isEqualTo(new CheckResult.Satisfied(3));
        assertThat(Check.evaluateAll(Gen.fromIterable(java.util.List.of(1, 2)), isGreaterThan(0)))
                .isEqualTo(new CheckResult.Satisfied(2));
        assertThat(Check.evaluateAll(CONFIG, Gen.fromIterable(java.util.List.of(1, 2)), isGreaterThan(0)))
                .isEqualTo(new CheckResult.Satisfied(2));
    }

    @Test
    void aFailureReportsTheExplanationOfEveryFailingAssertion() {
        assertThat(Check.evaluate(CONFIG, FIVE, isGreaterThan(6), isLessThan(9), equalTo(4)))
                .isEqualTo(new CheckResult.Falsified(
                        1, 7L, Tuple.of(5), Option.some("5 is not greater than 6\n5 is not equal to 4")));
        assertThatThrownBy(() -> Check.check(CONFIG, FIVE, isGreaterThan(6), isLessThan(9), equalTo(4)))
                .isExactlyInstanceOf(AssertionError.class)
                .hasMessage("falsified at sample 1 by (5) (seed 7, replay with -Dzazr.check.seed=7):\n"
                        + "  5 is not greater than 6\n"
                        + "  5 is not equal to 4");
        assertThatThrownBy(() -> Check.check(CONFIG, FIVE, equalTo(4)))
                .hasMessage(
                        "falsified at sample 1 by (5): 5 is not equal to 4 (seed 7, replay with -Dzazr.check.seed=7)");
    }

    @Test
    void aNestedFailureIsIndentedUnderTheSeed() {
        assertThatThrownBy(() -> Check.check(CONFIG, Gen.constant(List.of(1, 20)), Assertion.forall(isLessThan(10))))
                .hasMessage("falsified at sample 1 by (List(1, 20)) (seed 7, replay with -Dzazr.check.seed=7):\n"
                        + "  List(1, 20) has 20 at index 1:\n"
                        + "    20 is not less than 10");
    }

    @Test
    void theShortcutNeedsAnAssertion() {
        assertThatThrownBy(() -> Check.check(FIVE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("at least one assertion is needed");
        assertThatThrownBy(() -> Check.evaluate(CONFIG, FIVE)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Check.checkN(3, FIVE, (Assertion<Integer>) null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Check.check((Gen<Integer>) null, equalTo(1))).isInstanceOf(NullPointerException.class);
    }

    @Test
    void aBodyReturnsAnAssertionResult() {
        Check.check(
                Gen.list(Gen.integers()),
                Gen.integers(),
                (list, n) -> Assertion.assertThat(list.prepend(n), isNonEmpty()));
        assertThatThrownBy(() -> Check.check(
                        CONFIG,
                        FIVE,
                        Gen.constant(6),
                        (a, b) ->
                                Assertion.assertThat(a + b, equalTo(12)).and(Assertion.assertThat(a * b, equalTo(31)))))
                .hasMessage("falsified at sample 1 by (5, 6) (seed 7, replay with -Dzazr.check.seed=7):\n"
                        + "  11 is not equal to 12\n"
                        + "  30 is not equal to 31");
    }

    @Test
    void plainAssertionErrorsKeepWorking() {
        assertThatThrownBy(() -> Check.check(CONFIG, FIVE, n -> {
                    assertThat(n).isEqualTo(4);
                    return true;
                }))
                .isExactlyInstanceOf(AssertionError.class)
                .hasMessageStartingWith("falsified at sample 1 by (5)");
    }

    @Test
    void anAssertJChainBodyIsCheckedByItsAssertions() {
        Check.check(Gen.integers(1, 9), n -> assertThat(n).isPositive());
        assertThat(Check.evaluate(CONFIG, Gen.integers(1, 9), n -> assertThat(n).isPositive()))
                .isEqualTo(new CheckResult.Satisfied(50));
        CheckResult failing = Check.evaluate(CONFIG, FIVE, n -> assertThat(n).isNegative());
        assertThat(failing.isFalsified()).isTrue();
        // AssertJ's blank first and last lines are left out of the report
        assertThatThrownBy(() -> Check.check(CONFIG, FIVE, n -> assertThat(n).isNegative()))
                .hasMessage("falsified at sample 1 by (5) (seed 7, replay with -Dzazr.check.seed=7):\n"
                        + "  Expecting actual:\n"
                        + "    5\n"
                        + "  to be less than:\n"
                        + "    0");
    }

    @Test
    void anyOtherResultPassesAndNullIsErroneous() {
        assertThat(Check.evaluate(CONFIG, FIVE, n -> "done")).isEqualTo(new CheckResult.Satisfied(50));
        assertThat(Check.evaluate(CONFIG, FIVE, n -> Option.none())).isEqualTo(new CheckResult.Satisfied(50));
        CheckResult nothing = Check.evaluate(CONFIG, FIVE, n -> null);
        assertThat(nothing.isErroneous()).isTrue();
        assertThat(nothing.error().get()).hasMessage("the check returned null");
    }

    @Test
    void anEmptyExplanationLeavesTheReportClean() {
        assertThatThrownBy(() -> Check.check(CONFIG, FIVE, n -> TestResult.fail("")))
                .hasMessage("falsified at sample 1 by (5) (seed 7, replay with -Dzazr.check.seed=7)");
    }
}
