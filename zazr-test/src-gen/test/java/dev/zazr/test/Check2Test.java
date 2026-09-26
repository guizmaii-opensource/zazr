
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

class Check2Test {

    static final CheckConfig CONFIG = new CheckConfig(20, 10, 42L, 100);
    static final Gen<Integer> TWO = Gen.fromIterable(List.of(0, 1));
    static final IllegalStateException BOOM = new IllegalStateException("boom");
    static final Gen<Integer> FAILING = Gen.fromRandom(random -> {
        throw BOOM;
    });

    @Test
    void passesTheValuesInOrder() {
        final ArrayList<Object> seen = new ArrayList<>();
        final CheckResult result = Check.evaluate(CONFIG, Gen.constant(1), Gen.constant(2), (v1, v2) -> seen.add(Tuple.of(v1, v2)));
        assertThat(result).isEqualTo(new CheckResult.Satisfied(20));
        assertThat(seen).hasSize(20).containsOnly(Tuple.of(1, 2));
    }

    @Test
    void checkUsesTheDefaultConfiguration() {
        assertThat(Check.evaluate(Gen.constant(1), Gen.constant(2), (v1, v2) -> true)).isEqualTo(new CheckResult.Satisfied(CheckConfig.defaults().samples()));
    }

    @Test
    void checkNRunsNSamples() {
        assertThat(Check.evaluateN(3, Gen.constant(1), Gen.constant(2), (v1, v2) -> true)).isEqualTo(new CheckResult.Satisfied(3));
        assertThatThrownBy(() -> Check.evaluateN(-1, Gen.constant(1), Gen.constant(2), (v1, v2) -> true)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void checkAllRunsEveryCombinationOnce() {
        final ArrayList<Object> seen = new ArrayList<>();
        assertThat(Check.evaluateAll(TWO, TWO, (v1, v2) -> seen.add(Tuple.of(v1, v2)))).isEqualTo(new CheckResult.Satisfied(4));
        assertThat(seen).hasSize(4).doesNotHaveDuplicates();
        seen.clear();
        assertThat(Check.evaluateAll(CONFIG, TWO, TWO, (v1, v2) -> seen.add(Tuple.of(v1, v2)))).isEqualTo(new CheckResult.Satisfied(4));
        assertThat(seen).hasSize(4).doesNotHaveDuplicates();
    }

    @Test
    void falseFalsifiesTheCheck() {
        assertThat(Check.evaluate(CONFIG, Gen.constant(1), Gen.constant(2), (v1, v2) -> false))
                .isEqualTo(new CheckResult.Falsified(1, 42L, Tuple.of(1, 2), Option.none()));
        assertThat(Check.evaluateAll(CONFIG, TWO, TWO, (v1, v2) -> v1 + v2 < 2))
                .isEqualTo(new CheckResult.Falsified(4, 42L, Tuple.of(1, 1), Option.none()));
    }

    @Test
    void anAssertionErrorFalsifiesTheCheck() {
        assertThat(Check.evaluate(CONFIG, Gen.constant(1), Gen.constant(2), (v1, v2) -> {
            throw new AssertionError("sum " + (v1 + v2));
        })).isEqualTo(new CheckResult.Falsified(1, 42L, Tuple.of(1, 2), Option.some("sum 3")));
    }

    @Test
    void anExceptionMakesTheCheckErroneous() {
        assertThat(Check.evaluate(CONFIG, Gen.constant(1), Gen.constant(2), (v1, v2) -> {
            throw BOOM;
        })).isEqualTo(new CheckResult.Erroneous(1, 42L, BOOM, Option.some(Tuple.of(1, 2))));
        assertThat(Check.evaluate(CONFIG, Gen.constant(1), Gen.constant(2), (v1, v2) -> null).isErroneous()).isTrue();
    }

    @Test
    void aFailingGeneratorMakesTheCheckErroneous() {
        assertThat(Check.evaluate(CONFIG, FAILING, Gen.constant(2), (v1, v2) -> true)).isEqualTo(new CheckResult.Erroneous(1, 42L, BOOM, Option.none()));
        assertThat(Check.evaluateAll(CONFIG, FAILING, Gen.constant(2), (v1, v2) -> true)).isEqualTo(new CheckResult.Erroneous(1, 42L, BOOM, Option.none()));
        assertThat(Check.evaluate(CONFIG, Gen.constant(1), FAILING, (v1, v2) -> true)).isEqualTo(new CheckResult.Erroneous(1, 42L, BOOM, Option.none()));
        assertThat(Check.evaluateAll(CONFIG, Gen.constant(1), FAILING, (v1, v2) -> true)).isEqualTo(new CheckResult.Erroneous(1, 42L, BOOM, Option.none()));
    }

    @Test
    void checkReturnsWhenEveryValuePasses() {
        final ArrayList<Object> seen = new ArrayList<>();
        Check.check(CONFIG, Gen.constant(1), Gen.constant(2), (v1, v2) -> seen.add(Tuple.of(v1, v2)));
        assertThat(seen).hasSize(20);
        Check.check(Gen.constant(1), Gen.constant(2), (v1, v2) -> true);
        Check.checkN(3, Gen.constant(1), Gen.constant(2), (v1, v2) -> true);
        Check.checkAll(TWO, TWO, (v1, v2) -> true);
        Check.checkAll(CONFIG, TWO, TWO, (v1, v2) -> true);
    }

    @Test
    void checkThrowsTheAssertionErrorOfTheResult() {
        assertThatThrownBy(() -> Check.check(CONFIG, Gen.constant(1), Gen.constant(2), (v1, v2) -> false))
                .isExactlyInstanceOf(AssertionError.class)
                .hasMessage("falsified at sample 1 by (1, 2) (seed 42, replay with -Dzazr.check.seed=42)");
        assertThatThrownBy(() -> Check.check(Gen.constant(1), Gen.constant(2), (v1, v2) -> false)).isExactlyInstanceOf(AssertionError.class);
        assertThatThrownBy(() -> Check.checkN(3, Gen.constant(1), Gen.constant(2), (v1, v2) -> false)).isExactlyInstanceOf(AssertionError.class);
        assertThatThrownBy(() -> Check.checkAll(TWO, TWO, (v1, v2) -> v1 + v2 < 2)).isExactlyInstanceOf(AssertionError.class)
                .hasMessageStartingWith("falsified at sample 4 by (");
        assertThatThrownBy(() -> Check.checkAll(CONFIG, Gen.constant(1), Gen.constant(2), (v1, v2) -> {
            throw BOOM;
        })).isExactlyInstanceOf(AssertionError.class).hasCause(BOOM)
                .hasMessage("erroneous at sample 1 with (1, 2): java.lang.IllegalStateException: boom (seed 42, replay with -Dzazr.check.seed=42)");
    }

    @Test
    void rejectsNulls() {
        assertThatThrownBy(() -> Check.evaluate((CheckConfig) null, Gen.constant(1), Gen.constant(2), (v1, v2) -> true)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Check.evaluateAll((CheckConfig) null, Gen.constant(1), Gen.constant(2), (v1, v2) -> true)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Check.evaluate(CONFIG, Gen.constant(1), Gen.constant(2), null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Check.check(CONFIG, Gen.constant(1), Gen.constant(2), null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Check.checkAll(CONFIG, Gen.constant(1), Gen.constant(2), null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Check.check((CheckConfig) null, Gen.constant(1), Gen.constant(2), (v1, v2) -> true)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Check.evaluateAll(CONFIG, Gen.constant(1), Gen.constant(2), null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Check.evaluate(CONFIG, null, Gen.constant(2), (v1, v2) -> true)).isInstanceOf(NullPointerException.class).hasMessage("g1 is null");
        assertThatThrownBy(() -> Check.evaluateAll(CONFIG, null, Gen.constant(2), (v1, v2) -> true)).isInstanceOf(NullPointerException.class).hasMessage("g1 is null");
        assertThatThrownBy(() -> Check.evaluate(CONFIG, Gen.constant(1), null, (v1, v2) -> true)).isInstanceOf(NullPointerException.class).hasMessage("g2 is null");
        assertThatThrownBy(() -> Check.evaluateAll(CONFIG, Gen.constant(1), null, (v1, v2) -> true)).isInstanceOf(NullPointerException.class).hasMessage("g2 is null");
    }
}