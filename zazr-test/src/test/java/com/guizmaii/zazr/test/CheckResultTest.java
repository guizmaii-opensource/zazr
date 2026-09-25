package com.guizmaii.zazr.test;

import com.guizmaii.zazr.Tuple;
import com.guizmaii.zazr.control.Option;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CheckResultTest {

    private static final CheckResult SATISFIED = new CheckResult.Satisfied(200);
    private static final CheckResult FALSIFIED = new CheckResult.Falsified(3, 42L, Tuple.of(1, "a"), Option.some("left = 1, right = 2"));
    private static final CheckResult FALSIFIED_WITHOUT_MESSAGE = new CheckResult.Falsified(3, 42L, Tuple.of(1, "a"), Option.none());
    private static final IllegalStateException CAUSE = new IllegalStateException("boom");
    private static final CheckResult ERRONEOUS = new CheckResult.Erroneous(5, -7L, CAUSE, Option.some(Tuple.of(9)));
    private static final CheckResult ERRONEOUS_IN_GENERATOR = new CheckResult.Erroneous(1, -7L, CAUSE, Option.none());

    // -- kinds

    @Test
    void satisfiedIsOnlySatisfied() {
        assertThat(SATISFIED.isSatisfied()).isTrue();
        assertThat(SATISFIED.isFalsified()).isFalse();
        assertThat(SATISFIED.isErroneous()).isFalse();
    }

    @Test
    void falsifiedIsOnlyFalsified() {
        assertThat(FALSIFIED.isSatisfied()).isFalse();
        assertThat(FALSIFIED.isFalsified()).isTrue();
        assertThat(FALSIFIED.isErroneous()).isFalse();
    }

    @Test
    void erroneousIsOnlyErroneous() {
        assertThat(ERRONEOUS.isSatisfied()).isFalse();
        assertThat(ERRONEOUS.isFalsified()).isFalse();
        assertThat(ERRONEOUS.isErroneous()).isTrue();
    }

    // -- accessors

    @Test
    void satisfiedHasNoSampleErrorOrMessage() {
        assertThat(SATISFIED.sample()).isEqualTo(Option.none());
        assertThat(SATISFIED.error()).isEqualTo(Option.none());
        assertThat(SATISFIED.message()).isEqualTo(Option.none());
        assertThat(((CheckResult.Satisfied) SATISFIED).samples()).isEqualTo(200);
    }

    @Test
    void falsifiedHasItsCounterexampleAndMessage() {
        assertThat(FALSIFIED.sample()).isEqualTo(Option.some(Tuple.of(1, "a")));
        assertThat(FALSIFIED.error()).isEqualTo(Option.none());
        assertThat(FALSIFIED.message()).isEqualTo(Option.some("left = 1, right = 2"));
        assertThat(FALSIFIED_WITHOUT_MESSAGE.message()).isEqualTo(Option.none());
        final CheckResult.Falsified falsified = (CheckResult.Falsified) FALSIFIED;
        assertThat(falsified.sampleNumber()).isEqualTo(3);
        assertThat(falsified.seed()).isEqualTo(42L);
    }

    @Test
    void erroneousHasItsCauseAndSample() {
        assertThat(ERRONEOUS.sample()).isEqualTo(Option.some(Tuple.of(9)));
        assertThat(ERRONEOUS_IN_GENERATOR.sample()).isEqualTo(Option.none());
        assertThat(ERRONEOUS.error()).isEqualTo(Option.some(CAUSE));
        assertThat(ERRONEOUS.message()).isEqualTo(Option.none());
        final CheckResult.Erroneous erroneous = (CheckResult.Erroneous) ERRONEOUS;
        assertThat(erroneous.sampleNumber()).isEqualTo(5);
        assertThat(erroneous.seed()).isEqualTo(-7L);
    }

    // -- validation

    @Test
    void rejectsInvalidComponents() {
        assertThatThrownBy(() -> new CheckResult.Satisfied(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatCode(() -> new CheckResult.Satisfied(0)).doesNotThrowAnyException();
        assertThatThrownBy(() -> new CheckResult.Falsified(0, 1L, Tuple.of(1), Option.none())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CheckResult.Falsified(1, 1L, null, Option.none())).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CheckResult.Falsified(1, 1L, Tuple.of(1), null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CheckResult.Erroneous(0, 1L, CAUSE, Option.none())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CheckResult.Erroneous(1, 1L, null, Option.none())).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CheckResult.Erroneous(1, 1L, CAUSE, null)).isInstanceOf(NullPointerException.class);
    }

    // -- assertions

    @Test
    void assertIsSatisfiedPassesOnlyForSatisfied() {
        assertThatCode(SATISFIED::assertIsSatisfied).doesNotThrowAnyException();
        assertThatThrownBy(FALSIFIED::assertIsSatisfied).isInstanceOf(AssertionError.class);
        assertThatThrownBy(ERRONEOUS::assertIsSatisfied).isInstanceOf(AssertionError.class);
    }

    @Test
    void aFalsifiedAssertionNamesTheCounterexampleTheSampleAndTheSeed() {
        assertThatThrownBy(FALSIFIED::assertIsSatisfied).isInstanceOf(AssertionError.class)
                .hasMessage("falsified at sample 3 by (1, a): left = 1, right = 2 (seed 42, replay with -Dzazr.check.seed=42)")
                .hasNoCause();
        assertThatThrownBy(FALSIFIED_WITHOUT_MESSAGE::assertIsSatisfied)
                .hasMessage("falsified at sample 3 by (1, a) (seed 42, replay with -Dzazr.check.seed=42)");
    }

    @Test
    void anErroneousAssertionNamesTheErrorTheSampleAndTheSeedAndKeepsTheCause() {
        assertThatThrownBy(ERRONEOUS::assertIsSatisfied).isInstanceOf(AssertionError.class)
                .hasMessage("erroneous at sample 5 with (9): java.lang.IllegalStateException: boom (seed -7, replay with -Dzazr.check.seed=-7)")
                .hasCause(CAUSE);
        assertThatThrownBy(ERRONEOUS_IN_GENERATOR::assertIsSatisfied)
                .hasMessage("erroneous at sample 1, while generating it: java.lang.IllegalStateException: boom (seed -7, replay with -Dzazr.check.seed=-7)");
    }

    @Test
    void assertIsFalsifiedPassesOnlyForFalsified() {
        assertThatCode(FALSIFIED::assertIsFalsified).doesNotThrowAnyException();
        assertThatThrownBy(SATISFIED::assertIsFalsified).isInstanceOf(AssertionError.class)
                .hasMessage("expected a falsified check, but it was Satisfied[samples=200]");
        assertThatThrownBy(ERRONEOUS::assertIsFalsified).isInstanceOf(AssertionError.class);
    }

    @Test
    void assertIsErroneousPassesOnlyForErroneous() {
        assertThatCode(ERRONEOUS::assertIsErroneous).doesNotThrowAnyException();
        assertThatThrownBy(SATISFIED::assertIsErroneous).isInstanceOf(AssertionError.class)
                .hasMessage("expected an erroneous check, but it was Satisfied[samples=200]");
        assertThatThrownBy(FALSIFIED::assertIsErroneous).isInstanceOf(AssertionError.class);
    }

    // -- equality

    @Test
    void satisfiedAndFalsifiedCompareByValue() {
        assertThat(SATISFIED).isEqualTo(new CheckResult.Satisfied(200)).isNotEqualTo(new CheckResult.Satisfied(199));
        assertThat(FALSIFIED).isEqualTo(new CheckResult.Falsified(3, 42L, Tuple.of(1, "a"), Option.some("left = 1, right = 2")))
                .hasSameHashCodeAs(new CheckResult.Falsified(3, 42L, Tuple.of(1, "a"), Option.some("left = 1, right = 2")))
                .isNotEqualTo(FALSIFIED_WITHOUT_MESSAGE)
                .isNotEqualTo(new CheckResult.Falsified(3, 43L, Tuple.of(1, "a"), Option.some("left = 1, right = 2")))
                .isNotEqualTo(new CheckResult.Falsified(4, 42L, Tuple.of(1, "a"), Option.some("left = 1, right = 2")));
    }

    @Test
    void erroneousComparesCausesByClassAndMessageAlongTheChain() {
        final CheckResult same = new CheckResult.Erroneous(5, -7L, new IllegalStateException("boom"), Option.some(Tuple.of(9)));
        assertThat(ERRONEOUS).isEqualTo(same).hasSameHashCodeAs(same);
        assertThat(ERRONEOUS).isEqualTo(ERRONEOUS);
        assertThat(ERRONEOUS).isNotEqualTo(new CheckResult.Erroneous(5, -7L, new IllegalArgumentException("boom"), Option.some(Tuple.of(9))));
        assertThat(ERRONEOUS).isNotEqualTo(new CheckResult.Erroneous(5, -7L, new IllegalStateException("bang"), Option.some(Tuple.of(9))));
        assertThat(ERRONEOUS).isNotEqualTo(new CheckResult.Erroneous(5, -7L, new IllegalStateException("boom", new RuntimeException()), Option.some(Tuple.of(9))));
        assertThat(ERRONEOUS).isNotEqualTo(new CheckResult.Erroneous(6, -7L, CAUSE, Option.some(Tuple.of(9))));
        assertThat(ERRONEOUS).isNotEqualTo(new CheckResult.Erroneous(5, -8L, CAUSE, Option.some(Tuple.of(9))));
        assertThat(ERRONEOUS).isNotEqualTo(ERRONEOUS_IN_GENERATOR);
        assertThat(ERRONEOUS).isNotEqualTo(FALSIFIED);
        assertThat(ERRONEOUS.equals(null)).isFalse();
        final CheckResult chained = new CheckResult.Erroneous(1, 1L, new RuntimeException("a", new IllegalStateException("b")), Option.none());
        final CheckResult sameChain = new CheckResult.Erroneous(1, 1L, new RuntimeException("a", new IllegalStateException("b")), Option.none());
        final CheckResult otherChain = new CheckResult.Erroneous(1, 1L, new RuntimeException("a", new IllegalStateException("c")), Option.none());
        assertThat(chained).isEqualTo(sameChain).hasSameHashCodeAs(sameChain).isNotEqualTo(otherChain);
    }
}
