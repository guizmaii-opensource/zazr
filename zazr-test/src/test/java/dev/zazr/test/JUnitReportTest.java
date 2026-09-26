package dev.zazr.test;

import dev.zazr.collection.List;
import org.junit.jupiter.api.Test;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import static dev.zazr.test.Assertion.assertThat;
import static dev.zazr.test.Assertion.equalTo;
import static dev.zazr.test.Assertion.forall;
import static dev.zazr.test.Assertion.isLessThan;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * A failing check in a real JUnit run: the test fails with the check's {@link AssertionError}, whose message holds
 * the explanation of the failing assertions. This file imports both {@code assertThat}s, AssertJ's and
 * {@link Assertion#assertThat}, by name: Java picks one by its arguments.
 */
class JUnitReportTest {

    /// The tests the launcher runs; skipped by the build's own run, which runs only top-level test classes.
    static class Failing {

        @Test
        void everyElementIsSmall() {
            Check.check(CheckConfig.defaults().withSeed(42), Gen.constant(List.of(1, 20)), forall(isLessThan(10)));
        }

        @Test
        void aBodyWithTwoResults() {
            Check.check(CheckConfig.defaults().withSeed(42), Gen.constant(5), n -> assertThat(n, equalTo(4)).and(assertThat(n * 2, equalTo(11))));
        }
    }

    private static TestExecutionSummary run(Class<?> type) {
        final LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request().selectors(selectClass(type)).build();
        final Launcher launcher = LauncherFactory.create();
        final SummaryGeneratingListener listener = new SummaryGeneratingListener();
        launcher.execute(request, listener);
        return listener.getSummary();
    }

    @Test
    void aFailingCheckFailsTheTestWithTheExplanation() {
        final TestExecutionSummary summary = run(Failing.class);
        assertThat(summary.getTestsFailedCount()).isEqualTo(2);
        assertThat(summary.getFailures()).extracting(failure -> failure.getTestIdentifier().getDisplayName() + " -> "
                        + failure.getException().getClass().getName() + ": " + failure.getException().getMessage())
                .containsExactlyInAnyOrder(
                        "everyElementIsSmall() -> java.lang.AssertionError: falsified at sample 1 by (List(1, 20)) (seed 42, replay with -Dzazr.check.seed=42):\n"
                                + "  List(1, 20) has 20 at index 1:\n"
                                + "    20 is not less than 10",
                        "aBodyWithTwoResults() -> java.lang.AssertionError: falsified at sample 1 by (5) (seed 42, replay with -Dzazr.check.seed=42):\n"
                                + "  5 is not equal to 4\n"
                                + "  10 is not equal to 11");
    }

    @Test
    void bothAssertThatsResolveByTheirArguments() {
        assertThat(assertThat(5, equalTo(5)).isSuccess()).isTrue();
        assertThat(assertThat(() -> Integer.parseInt("x"), Assertion.throwsA(NumberFormatException.class)).isSuccess()).isTrue();
    }
}
