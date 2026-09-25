package com.guizmaii.zazr.test;

import com.guizmaii.zazr.Tuple;
import com.guizmaii.zazr.control.Option;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The runner: samples, sizes, seeds, the three results and the discard budget.
 */
class CheckTest {

    private static CheckConfig config(long seed) {
        return new CheckConfig(200, 100, seed, 1000);
    }

    // -- replay

    @Test
    void aSeedReplaysAFailure() {
        final Gen<Integer> ints = Gen.intValue(0, 1_000);
        final CheckResult first = Check.check(config(9), ints, ints, (a, b) -> a + b < 1_500);
        final CheckResult second = Check.check(config(9), ints, ints, (a, b) -> a + b < 1_500);
        assertThat(first.isFalsified()).isTrue();
        assertThat(second).isEqualTo(first);
        assertThat(((CheckResult.Falsified) first).seed()).isEqualTo(9L);
    }

    @Test
    void theSeedOfTheDefaultConfigurationIsReported() {
        final CheckResult result = Check.check(Gen.intValue(), i -> false);
        final long seed = ((CheckResult.Falsified) result).seed();
        assertThat(Check.check(CheckConfig.defaults().withSeed(seed), Gen.intValue(), i -> false)).isEqualTo(result);
    }

    // -- samples

    @Test
    void checkRunsTheConfiguredNumberOfSamples() {
        final AtomicInteger calls = new AtomicInteger();
        assertThat(Check.check(config(1).withSamples(37), Gen.intValue(), i -> calls.incrementAndGet() > 0))
                .isEqualTo(new CheckResult.Satisfied(37));
        assertThat(calls).hasValue(37);
    }

    @Test
    void checkRunsTheDefaultNumberOfSamples() {
        assertThat(Check.check(Gen.intValue(), i -> true)).isEqualTo(new CheckResult.Satisfied(CheckConfig.defaults().samples()));
    }

    @Test
    void checkNRunsNSamples() {
        assertThat(Check.checkN(3, Gen.intValue(), i -> true)).isEqualTo(new CheckResult.Satisfied(3));
        assertThat(Check.checkN(0, Gen.intValue(), i -> false)).isEqualTo(new CheckResult.Satisfied(0));
        assertThatThrownBy(() -> Check.checkN(-1, Gen.intValue(), i -> true)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void zeroSamplesRunNothing() {
        final AtomicInteger generated = new AtomicInteger();
        final Gen<Integer> gen = Gen.fromRandom(random -> generated.incrementAndGet());
        assertThat(Check.check(config(1).withSamples(0), gen, i -> false)).isEqualTo(new CheckResult.Satisfied(0));
        assertThat(generated).hasValue(0);
    }

    @Test
    void checkRepeatsTheValuesOfAFiniteGenerator() {
        final ArrayList<Integer> seen = new ArrayList<>();
        Check.check(config(1).withSamples(7), Gen.fromIterable(java.util.List.of(1, 2, 3)), seen::add).assertIsSatisfied();
        assertThat(seen).containsExactly(1, 2, 3, 1, 2, 3, 1);
        seen.clear();
        Check.check(config(1).withSamples(4), Gen.constant(5), seen::add).assertIsSatisfied();
        assertThat(seen).containsExactly(5, 5, 5, 5);
    }

    @Test
    void checkAllRunsEachValueOfAFiniteGeneratorOnce() {
        final ArrayList<Integer> seen = new ArrayList<>();
        assertThat(Check.checkAll(Gen.fromIterable(java.util.List.of(1, 2, 3)), seen::add)).isEqualTo(new CheckResult.Satisfied(3));
        assertThat(seen).containsExactly(1, 2, 3);
    }

    @Test
    void checkAllIgnoresTheNumberOfSamples() {
        assertThat(Check.checkAll(config(1).withSamples(1), Gen.fromIterable(java.util.List.of(1, 2, 3)), i -> true))
                .isEqualTo(new CheckResult.Satisfied(3));
    }

    @Test
    void checkAllRunsEveryCombinationOfFiniteGenerators() {
        final ArrayList<String> seen = new ArrayList<>();
        final CheckResult result = Check.checkAll(Gen.fromIterable(java.util.List.of(1, 2)), Gen.fromIterable(java.util.List.of("a", "b", "c")),
                (i, s) -> seen.add(i + s));
        assertThat(result).isEqualTo(new CheckResult.Satisfied(6));
        assertThat(seen).containsExactly("1a", "1b", "1c", "2a", "2b", "2c");
    }

    @Test
    void checkAllOfARandomGeneratorRunsOneSample() {
        assertThat(Check.checkAll(Gen.intValue(), i -> true)).isEqualTo(new CheckResult.Satisfied(1));
    }

    @Test
    void checkAllOfAnEmptyGeneratorRunsNothing() {
        assertThat(Check.checkAll(Gen.<Integer>empty(), i -> false)).isEqualTo(new CheckResult.Satisfied(0));
    }

    @Test
    void checkAllRunsAtTheConfiguredSize() {
        final ArrayList<Integer> seen = new ArrayList<>();
        Check.checkAll(config(1).withSize(42), Gen.size(), seen::add).assertIsSatisfied();
        assertThat(seen).containsExactly(42);
    }

    // -- sizes

    @Test
    void theSizeGrowsOverARun() {
        final ArrayList<Integer> seen = new ArrayList<>();
        Check.check(config(1).withSamples(5), Gen.size(), seen::add).assertIsSatisfied();
        assertThat(seen).containsExactly(0, 25, 50, 75, 100);
    }

    @Test
    void theSizeOfAPassDependsOnTheSamplesBeforeIt() {
        final ArrayList<String> seen = new ArrayList<>();
        final Gen<String> pairs = Gen.size().flatMap(size -> Gen.fromIterable(java.util.List.of(size + "a", size + "b")));
        Check.check(config(1).withSamples(5).withSize(4), pairs, seen::add).assertIsSatisfied();
        // the second pass starts after two samples: 4 * 2 / 4 = 2; the third after four: 4
        assertThat(seen).containsExactly("0a", "0b", "2a", "2b", "4a");
    }

    @Test
    void smallCounterexamplesComeFirst() {
        final Gen<Integer> upToSize = Gen.sized(size -> Gen.intValue(0, size));
        for (long seed = 0; seed < 20; seed++) {
            final CheckResult.Falsified falsified = (CheckResult.Falsified) Check.check(config(seed), upToSize, n -> n < 30);
            final int counterexample = (Integer) ((com.guizmaii.zazr.Tuple1<?>) falsified.counterexample())._1();
            // the counterexample cannot exceed the size of its sample, which grows by half a unit per sample
            assertThat(counterexample).isBetween(30, Runner.size(config(seed), falsified.sampleNumber() - 1));
            assertThat(falsified.sampleNumber()).isGreaterThanOrEqualTo(60);
        }
    }

    // -- results

    @Test
    void falseFalsifiesTheSampleAndStopsTheCheck() {
        final AtomicInteger calls = new AtomicInteger();
        final CheckResult result = Check.check(config(3), Gen.fromIterable(java.util.List.of(1, 2, 3, 4)), i -> {
            calls.incrementAndGet();
            return i < 3;
        });
        assertThat(result).isEqualTo(new CheckResult.Falsified(3, 3L, Tuple.of(3), Option.none()));
        assertThat(calls).hasValue(3);
    }

    @Test
    void anAssertionErrorFalsifiesTheSampleWithItsMessage() {
        final CheckResult result = Check.check(config(3), Gen.constant(7), i -> {
            throw new AssertionError("expected 8 but was " + i);
        });
        assertThat(result).isEqualTo(new CheckResult.Falsified(1, 3L, Tuple.of(7), Option.some("expected 8 but was 7")));
        final CheckResult silent = Check.check(config(3), Gen.constant(7), i -> {
            throw new AssertionError();
        });
        assertThat(silent).isEqualTo(new CheckResult.Falsified(1, 3L, Tuple.of(7), Option.none()));
    }

    @Test
    void anAssertionErrorSubclassFalsifiesTheSample() {
        final CheckResult result = Check.check(config(3), Gen.constant(7), i -> {
            throw new org.opentest4j.AssertionFailedError("not equal");
        });
        assertThat(result.isFalsified()).isTrue();
        assertThat(result.message()).isEqualTo(Option.some("not equal"));
    }

    @Test
    void anExceptionOfTheCheckMakesItErroneous() {
        final IllegalStateException boom = new IllegalStateException("boom");
        final CheckResult result = Check.check(config(3), Gen.fromIterable(java.util.List.of(1, 2)), i -> {
            if (i == 2) {
                throw boom;
            }
            return true;
        });
        assertThat(result).isEqualTo(new CheckResult.Erroneous(2, 3L, boom, Option.some(Tuple.of(2))));
        assertThat(result.error().get()).isSameAs(boom);
    }

    @Test
    void aCheckedExceptionOfTheCheckMakesItErroneous() {
        final CheckResult result = Check.check(config(3), Gen.constant(1), i -> {
            throw new java.io.IOException("io");
        });
        assertThat(result.isErroneous()).isTrue();
        assertThat(result.error().get()).isInstanceOf(java.io.IOException.class);
    }

    @Test
    void anInterruptMakesTheCheckErroneousAndStaysVisible() {
        try {
            final CheckResult result = Check.check(config(3), Gen.constant(1), i -> {
                throw new InterruptedException("stop");
            });
            assertThat(result.isErroneous()).isTrue();
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void aNullResultMakesTheCheckErroneous() {
        final CheckResult result = Check.check(config(3), Gen.constant(1), i -> null);
        assertThat(result.isErroneous()).isTrue();
        assertThat(result.error().get()).isInstanceOf(NullPointerException.class).hasMessage("the check returned null");
        assertThat(result.sample()).isEqualTo(Option.some(Tuple.of(1)));
    }

    @Test
    void aStackOverflowMakesTheCheckErroneous() {
        final CheckResult result = Check.check(config(3), Gen.constant(1), i -> {
            throw new StackOverflowError();
        });
        assertThat(result.isErroneous()).isTrue();
    }

    @Test
    void anOutOfMemoryErrorEndsTheCheck() {
        assertThatThrownBy(() -> Check.check(config(3), Gen.constant(1), i -> {
            throw new OutOfMemoryError("full");
        })).isInstanceOf(OutOfMemoryError.class).hasMessage("full");
        assertThatThrownBy(() -> Check.check(config(3), Gen.fromRandom(random -> {
            throw new OutOfMemoryError("full");
        }), i -> true)).isInstanceOf(OutOfMemoryError.class);
    }

    @Test
    void aGeneratorThatThrowsMakesTheCheckErroneousWithoutSample() {
        final IllegalArgumentException boom = new IllegalArgumentException("gen");
        final Gen<Integer> failsOnThird = Gen.fromIterable(java.util.List.of(1, 2, 3)).map(i -> {
            if (i == 3) {
                throw boom;
            }
            return i;
        });
        assertThat(Check.check(config(4), failsOnThird, i -> true)).isEqualTo(new CheckResult.Erroneous(3, 4L, boom, Option.none()));
        assertThat(Check.checkAll(config(4), failsOnThird, i -> true)).isEqualTo(new CheckResult.Erroneous(3, 4L, boom, Option.none()));
    }

    @Test
    void anEmptyGeneratorMakesACheckErroneous() {
        final CheckResult result = Check.check(config(4), Gen.<Integer>empty(), i -> true);
        assertThat(result.isErroneous()).isTrue();
        assertThat(result.error().get()).isInstanceOf(IllegalStateException.class)
                .hasMessage("the generator produced no value in 1001 passes in a row, more than the discard budget of 1000");
        assertThat(((CheckResult.Erroneous) result).sampleNumber()).isEqualTo(1);
    }

    @Test
    void aFilterBeyondItsBudgetMakesTheCheckErroneous() {
        final CheckResult result = Check.check(config(4), Gen.intValue(), Gen.intValue().filter(i -> false), (a, b) -> true);
        assertThat(result.isErroneous()).isTrue();
        assertThat(result.error().get()).isInstanceOf(IllegalStateException.class).hasMessageStartingWith("Gen.filter rejected 1001 values in a row");
        assertThat(result.sample()).isEqualTo(Option.none());
    }

    @Test
    void theDiscardBudgetCountsEmptyPassesInARow() {
        // half of the passes are empty
        final Gen<Integer> sometimes = Gen.oneOf(Gen.empty(), Gen.constant(1));
        assertThat(Check.check(config(1), sometimes, i -> true)).isEqualTo(new CheckResult.Satisfied(200));
        final CheckResult result = Check.check(config(1).withMaxDiscards(0), sometimes, i -> true);
        assertThat(result.error().get()).isInstanceOf(IllegalStateException.class)
                .hasMessage("the generator produced no value in 1 passes in a row, more than the discard budget of 0");
    }

    @Test
    void assertIsSatisfiedReportsTheFailure() {
        assertThatThrownBy(() -> Check.check(config(12), Gen.constant(5), i -> i < 5).assertIsSatisfied())
                .isInstanceOf(AssertionError.class)
                .hasMessage("falsified at sample 1 by (5) (seed 12, replay with -Dzazr.check.seed=12)");
    }

    @Test
    void checkRejectsNulls() {
        assertThatThrownBy(() -> Check.check((CheckConfig) null, Gen.constant(1), i -> true)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Check.check(config(1), (Gen<Integer>) null, i -> true)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Check.check(config(1), Gen.constant(1), null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Check.checkAll((CheckConfig) null, Gen.constant(1), i -> true)).isInstanceOf(NullPointerException.class);
    }
}
