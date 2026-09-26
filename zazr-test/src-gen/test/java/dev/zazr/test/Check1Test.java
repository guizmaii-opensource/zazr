
package dev.zazr.test;

/*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-*\
   G E N E R A T O R   C R A F T E D
\*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-*/

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.zazr.Tuple;
import dev.zazr.control.Option;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class Check1Test {

    static final CheckConfig CONFIG = new CheckConfig(20, 10, 42L, 100);
    static final Gen<Integer> TWO = Gen.fromIterable(List.of(0, 1));
    static final IllegalStateException BOOM = new IllegalStateException("boom");
    static final Gen<Integer> FAILING = Gen.fromRandom(random -> {
        throw BOOM;
    });

    @Test
    void passesTheValuesInOrder() {
        final ArrayList<Object> seen = new ArrayList<>();
        final CheckResult result = Check.evaluate(CONFIG, Gen.constant(1), (v1) -> seen.add(Tuple.of(v1)));
        assertThat(result).isEqualTo(new CheckResult.Satisfied(20));
        assertThat(seen).hasSize(20).containsOnly(Tuple.of(1));
    }

    @Test
    void checkUsesTheDefaultConfiguration() {
        assertThat(Check.evaluate(Gen.constant(1), (v1) -> true)).isEqualTo(new CheckResult.Satisfied(CheckConfig.defaults().samples()));
    }

    @Test
    void checkNRunsNSamples() {
        assertThat(Check.evaluateN(3, Gen.constant(1), (v1) -> true)).isEqualTo(new CheckResult.Satisfied(3));
        assertThatThrownBy(() -> Check.evaluateN(-1, Gen.constant(1), (v1) -> true)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void checkAllRunsEveryCombinationOnce() {
        final ArrayList<Object> seen = new ArrayList<>();
        assertThat(Check.evaluateAll(TWO, (v1) -> seen.add(Tuple.of(v1)))).isEqualTo(new CheckResult.Satisfied(2));
        assertThat(seen).hasSize(2).doesNotHaveDuplicates();
        seen.clear();
        assertThat(Check.evaluateAll(CONFIG, TWO, (v1) -> seen.add(Tuple.of(v1)))).isEqualTo(new CheckResult.Satisfied(2));
        assertThat(seen).hasSize(2).doesNotHaveDuplicates();
    }

    @Test
    void falseFalsifiesTheCheck() {
        assertThat(Check.evaluate(CONFIG, Gen.constant(1), (v1) -> false))
                .isEqualTo(new CheckResult.Falsified(1, 42L, Tuple.of(1), Option.none()));
        assertThat(Check.evaluateAll(CONFIG, TWO, (v1) -> v1 < 1))
                .isEqualTo(new CheckResult.Falsified(2, 42L, Tuple.of(1), Option.none()));
    }

    @Test
    void anAssertionErrorFalsifiesTheCheck() {
        assertThat(Check.evaluate(CONFIG, Gen.constant(1), (v1) -> {
            throw new AssertionError("sum " + (v1));
        })).isEqualTo(new CheckResult.Falsified(1, 42L, Tuple.of(1), Option.some("sum 1")));
    }

    @Test
    void anExceptionMakesTheCheckErroneous() {
        assertThat(Check.evaluate(CONFIG, Gen.constant(1), (v1) -> {
            throw BOOM;
        })).isEqualTo(new CheckResult.Erroneous(1, 42L, BOOM, Option.some(Tuple.of(1))));
        assertThat(Check.evaluate(CONFIG, Gen.constant(1), (v1) -> null).isErroneous()).isTrue();
    }

    @Test
    void aFailingGeneratorMakesTheCheckErroneous() {
        assertThat(Check.evaluate(CONFIG, FAILING, (v1) -> true)).isEqualTo(new CheckResult.Erroneous(1, 42L, BOOM, Option.none()));
        assertThat(Check.evaluateAll(CONFIG, FAILING, (v1) -> true)).isEqualTo(new CheckResult.Erroneous(1, 42L, BOOM, Option.none()));
    }

    @Test
    void checkReturnsWhenEveryValuePasses() {
        final ArrayList<Object> seen = new ArrayList<>();
        Check.check(CONFIG, Gen.constant(1), (v1) -> seen.add(Tuple.of(v1)));
        assertThat(seen).hasSize(20);
        Check.check(Gen.constant(1), (v1) -> true);
        Check.checkN(3, Gen.constant(1), (v1) -> true);
        Check.checkAll(TWO, (v1) -> true);
        Check.checkAll(CONFIG, TWO, (v1) -> true);
    }

    @Test
    void checkThrowsTheAssertionErrorOfTheResult() {
        assertThatThrownBy(() -> Check.check(CONFIG, Gen.constant(1), (v1) -> false))
                .isExactlyInstanceOf(AssertionError.class)
                .hasMessage("falsified at sample 1 by (1) (seed 42, replay with -Dzazr.check.seed=42)");
        assertThatThrownBy(() -> Check.check(Gen.constant(1), (v1) -> false)).isExactlyInstanceOf(AssertionError.class);
        assertThatThrownBy(() -> Check.checkN(3, Gen.constant(1), (v1) -> false)).isExactlyInstanceOf(AssertionError.class);
        assertThatThrownBy(() -> Check.checkAll(TWO, (v1) -> v1 < 1)).isExactlyInstanceOf(AssertionError.class)
                .hasMessageStartingWith("falsified at sample 2 by (");
        assertThatThrownBy(() -> Check.checkAll(CONFIG, Gen.constant(1), (v1) -> {
            throw BOOM;
        })).isExactlyInstanceOf(AssertionError.class).hasCause(BOOM)
                .hasMessage("erroneous at sample 1 with (1): java.lang.IllegalStateException: boom (seed 42, replay with -Dzazr.check.seed=42)");
    }

    @Test
    void rejectsNulls() {
        assertThatThrownBy(() -> Check.evaluate((CheckConfig) null, Gen.constant(1), (v1) -> true)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Check.evaluateAll((CheckConfig) null, Gen.constant(1), (v1) -> true)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Check.evaluate(CONFIG, Gen.constant(1), null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Check.check(CONFIG, Gen.constant(1), null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Check.checkAll(CONFIG, Gen.constant(1), null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Check.check((CheckConfig) null, Gen.constant(1), (v1) -> true)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Check.evaluateAll(CONFIG, Gen.constant(1), null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Check.evaluate(CONFIG, null, (v1) -> true)).isInstanceOf(NullPointerException.class).hasMessage("g1 is null");
        assertThatThrownBy(() -> Check.evaluateAll(CONFIG, null, (v1) -> true)).isInstanceOf(NullPointerException.class).hasMessage("g1 is null");
    }
}