
package com.guizmaii.zazr.test;

/*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-*\
   G E N E R A T O R   C R A F T E D
\*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-*/

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.guizmaii.zazr.Tuple;
import com.guizmaii.zazr.control.Option;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class Check3Test {

    static final CheckConfig CONFIG = new CheckConfig(20, 10, 42L, 100);
    static final Gen<Integer> TWO = Gen.fromIterable(List.of(0, 1));
    static final IllegalStateException BOOM = new IllegalStateException("boom");
    static final Gen<Integer> FAILING = Gen.fromRandom(random -> {
        throw BOOM;
    });

    @Test
    void passesTheValuesInOrder() {
        final ArrayList<Object> seen = new ArrayList<>();
        final CheckResult result = Check.check(CONFIG, Gen.constant(1), Gen.constant(2), Gen.constant(3), (v1, v2, v3) -> seen.add(Tuple.of(v1, v2, v3)));
        assertThat(result).isEqualTo(new CheckResult.Satisfied(20));
        assertThat(seen).hasSize(20).containsOnly(Tuple.of(1, 2, 3));
    }

    @Test
    void checkUsesTheDefaultConfiguration() {
        assertThat(Check.check(Gen.constant(1), Gen.constant(2), Gen.constant(3), (v1, v2, v3) -> true)).isEqualTo(new CheckResult.Satisfied(CheckConfig.defaults().samples()));
    }

    @Test
    void checkNRunsNSamples() {
        assertThat(Check.checkN(3, Gen.constant(1), Gen.constant(2), Gen.constant(3), (v1, v2, v3) -> true)).isEqualTo(new CheckResult.Satisfied(3));
        assertThatThrownBy(() -> Check.checkN(-1, Gen.constant(1), Gen.constant(2), Gen.constant(3), (v1, v2, v3) -> true)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void checkAllRunsEveryCombinationOnce() {
        final ArrayList<Object> seen = new ArrayList<>();
        assertThat(Check.checkAll(TWO, TWO, TWO, (v1, v2, v3) -> seen.add(Tuple.of(v1, v2, v3)))).isEqualTo(new CheckResult.Satisfied(8));
        assertThat(seen).hasSize(8).doesNotHaveDuplicates();
        seen.clear();
        assertThat(Check.checkAll(CONFIG, TWO, TWO, TWO, (v1, v2, v3) -> seen.add(Tuple.of(v1, v2, v3)))).isEqualTo(new CheckResult.Satisfied(8));
        assertThat(seen).hasSize(8).doesNotHaveDuplicates();
    }

    @Test
    void falseFalsifiesTheCheck() {
        assertThat(Check.check(CONFIG, Gen.constant(1), Gen.constant(2), Gen.constant(3), (v1, v2, v3) -> false))
                .isEqualTo(new CheckResult.Falsified(1, 42L, Tuple.of(1, 2, 3), Option.none()));
        assertThat(Check.checkAll(CONFIG, TWO, TWO, TWO, (v1, v2, v3) -> v1 + v2 + v3 < 3))
                .isEqualTo(new CheckResult.Falsified(8, 42L, Tuple.of(1, 1, 1), Option.none()));
    }

    @Test
    void anAssertionErrorFalsifiesTheCheck() {
        assertThat(Check.check(CONFIG, Gen.constant(1), Gen.constant(2), Gen.constant(3), (v1, v2, v3) -> {
            throw new AssertionError("sum " + (v1 + v2 + v3));
        })).isEqualTo(new CheckResult.Falsified(1, 42L, Tuple.of(1, 2, 3), Option.some("sum 6")));
    }

    @Test
    void anExceptionMakesTheCheckErroneous() {
        assertThat(Check.check(CONFIG, Gen.constant(1), Gen.constant(2), Gen.constant(3), (v1, v2, v3) -> {
            throw BOOM;
        })).isEqualTo(new CheckResult.Erroneous(1, 42L, BOOM, Option.some(Tuple.of(1, 2, 3))));
        assertThat(Check.check(CONFIG, Gen.constant(1), Gen.constant(2), Gen.constant(3), (v1, v2, v3) -> null).isErroneous()).isTrue();
    }

    @Test
    void aFailingGeneratorMakesTheCheckErroneous() {
        assertThat(Check.check(CONFIG, FAILING, Gen.constant(2), Gen.constant(3), (v1, v2, v3) -> true)).isEqualTo(new CheckResult.Erroneous(1, 42L, BOOM, Option.none()));
        assertThat(Check.checkAll(CONFIG, FAILING, Gen.constant(2), Gen.constant(3), (v1, v2, v3) -> true)).isEqualTo(new CheckResult.Erroneous(1, 42L, BOOM, Option.none()));
        assertThat(Check.check(CONFIG, Gen.constant(1), FAILING, Gen.constant(3), (v1, v2, v3) -> true)).isEqualTo(new CheckResult.Erroneous(1, 42L, BOOM, Option.none()));
        assertThat(Check.checkAll(CONFIG, Gen.constant(1), FAILING, Gen.constant(3), (v1, v2, v3) -> true)).isEqualTo(new CheckResult.Erroneous(1, 42L, BOOM, Option.none()));
        assertThat(Check.check(CONFIG, Gen.constant(1), Gen.constant(2), FAILING, (v1, v2, v3) -> true)).isEqualTo(new CheckResult.Erroneous(1, 42L, BOOM, Option.none()));
        assertThat(Check.checkAll(CONFIG, Gen.constant(1), Gen.constant(2), FAILING, (v1, v2, v3) -> true)).isEqualTo(new CheckResult.Erroneous(1, 42L, BOOM, Option.none()));
    }

    @Test
    void rejectsNulls() {
        assertThatThrownBy(() -> Check.check((CheckConfig) null, Gen.constant(1), Gen.constant(2), Gen.constant(3), (v1, v2, v3) -> true)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Check.checkAll((CheckConfig) null, Gen.constant(1), Gen.constant(2), Gen.constant(3), (v1, v2, v3) -> true)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Check.check(CONFIG, Gen.constant(1), Gen.constant(2), Gen.constant(3), null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Check.checkAll(CONFIG, Gen.constant(1), Gen.constant(2), Gen.constant(3), null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Check.check(CONFIG, null, Gen.constant(2), Gen.constant(3), (v1, v2, v3) -> true)).isInstanceOf(NullPointerException.class).hasMessage("g1 is null");
        assertThatThrownBy(() -> Check.checkAll(CONFIG, null, Gen.constant(2), Gen.constant(3), (v1, v2, v3) -> true)).isInstanceOf(NullPointerException.class).hasMessage("g1 is null");
        assertThatThrownBy(() -> Check.check(CONFIG, Gen.constant(1), null, Gen.constant(3), (v1, v2, v3) -> true)).isInstanceOf(NullPointerException.class).hasMessage("g2 is null");
        assertThatThrownBy(() -> Check.checkAll(CONFIG, Gen.constant(1), null, Gen.constant(3), (v1, v2, v3) -> true)).isInstanceOf(NullPointerException.class).hasMessage("g2 is null");
        assertThatThrownBy(() -> Check.check(CONFIG, Gen.constant(1), Gen.constant(2), null, (v1, v2, v3) -> true)).isInstanceOf(NullPointerException.class).hasMessage("g3 is null");
        assertThatThrownBy(() -> Check.checkAll(CONFIG, Gen.constant(1), Gen.constant(2), null, (v1, v2, v3) -> true)).isInstanceOf(NullPointerException.class).hasMessage("g3 is null");
    }
}