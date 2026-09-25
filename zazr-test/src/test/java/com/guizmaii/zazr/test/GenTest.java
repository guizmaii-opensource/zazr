package com.guizmaii.zazr.test;

import com.guizmaii.zazr.Tuple;
import com.guizmaii.zazr.Tuple2;
import com.guizmaii.zazr.collection.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The generator mechanics: passes, finite and random generators, sizes, combinators and the discard budget.
 */
class GenTest {

    static CheckConfig config(long seed) {
        return new CheckConfig(200, 100, seed, 1000);
    }

    static <A> List<A> values(Gen<A> gen, int n) {
        return gen.runCollectN(n, config(1));
    }

    static <A> List<A> pass(Gen<A> gen) {
        return gen.runCollect(config(1));
    }

    // -- replay

    @Test
    void aSeedReplaysTheSameValues() {
        final Gen<Tuple2<Integer, String>> gen = Gen.zip(Gen.integers(), Gen.alphaNumericStrings());
        assertThat(gen.runCollectN(300, config(7))).isEqualTo(gen.runCollectN(300, config(7)));
        assertThat(gen.runCollect(config(7))).isEqualTo(gen.runCollect(config(7)));
    }

    @Test
    void anotherSeedGivesOtherValues() {
        assertThat(Gen.integers().runCollectN(50, config(7))).isNotEqualTo(Gen.integers().runCollectN(50, config(8)));
    }

    @Test
    void aGeneratorHoldsNoState() {
        final Gen<Integer> gen = Gen.integers(0, 1_000_000).map(i -> i * 2);
        final List<Integer> first = gen.runCollectN(100, config(3));
        Gen.integers().runCollectN(100, config(4));
        assertThat(gen.runCollectN(100, config(3))).isEqualTo(first);
    }

    @Test
    void theDefaultConfigurationRuns() {
        assertThat(Gen.integers(0, 9).runCollectN(10)).hasSize(10).allMatch(i -> i >= 0 && i <= 9);
        assertThat(Gen.constant(1).runCollect()).isEqualTo(List.of(1));
    }

    // -- constructors

    @Test
    void constantGivesItsValueOnce() {
        assertThat(pass(Gen.constant("a"))).isEqualTo(List.of("a"));
        assertThat(values(Gen.constant("a"), 3)).isEqualTo(List.of("a", "a", "a"));
    }

    @Test
    void aNullValueReachesTheCheckButNotAList() {
        final java.util.List<Object> seen = new ArrayList<>();
        Check.checkAll(Gen.fromIterable(Arrays.asList(1, null)), value -> seen.add(value)).assertIsSatisfied();
        assertThat(seen).containsExactly(1, null);
        assertThatThrownBy(() -> pass(Gen.constant(null))).isInstanceOf(NullPointerException.class);
    }

    @Test
    void emptyGivesNothing() {
        assertThat(pass(Gen.empty())).isEmpty();
    }

    @Test
    void anEmptyGeneratorExhaustsTheDiscardBudgetOfARepeatedRun() {
        assertThatThrownBy(() -> Gen.empty().runCollectN(1, config(1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("the generator produced no value: 1001 discards since the last sample, more than the discard budget of 1000");
        assertThatThrownBy(() -> Gen.empty().runCollectN(1, config(1).withMaxDiscards(0)))
                .hasMessage("the generator produced no value: 1 discards since the last sample, more than the discard budget of 0");
        assertThat(Gen.empty().runCollectN(0, config(1))).isEmpty();
    }

    @Test
    void fromIterableGivesItsValuesInOrderInOnePass() {
        assertThat(pass(Gen.fromIterable(Arrays.asList(3, 2, 1)))).isEqualTo(List.of(3, 2, 1));
        assertThat(pass(Gen.fromIterable(java.util.List.of()))).isEmpty();
        assertThat(values(Gen.fromIterable(java.util.List.of(1, 2, 3)), 7)).isEqualTo(List.of(1, 2, 3, 1, 2, 3, 1));
    }

    @Test
    void fromIterableCopiesItsInput() {
        final java.util.List<Integer> source = new ArrayList<>(java.util.List.of(1, 2));
        final Gen<Integer> gen = Gen.fromIterable(source);
        source.add(3);
        assertThat(pass(gen)).isEqualTo(List.of(1, 2));
    }

    @Test
    void fromIterableAcceptsAOneShotIterable() {
        final Iterator<Integer> iterator = java.util.List.of(1, 2).iterator();
        final Gen<Integer> gen = Gen.fromIterable(() -> iterator);
        assertThat(pass(gen)).isEqualTo(List.of(1, 2));
        assertThat(pass(gen)).isEqualTo(List.of(1, 2));
        assertThat(values(gen, 4)).isEqualTo(List.of(1, 2, 1, 2));
    }

    @Test
    void elementsChoosesEveryValue() {
        assertThat(values(Gen.elements("a", "b", "c"), 200)).containsOnly("a", "b", "c").contains("a", "b", "c");
        assertThat(pass(Gen.<String>elements())).isEmpty();
    }

    @Test
    void elementsCopiesItsArray() {
        final String[] array = { "a" };
        final Gen<String> gen = Gen.elements(array);
        array[0] = "b";
        assertThat(values(gen, 5)).containsOnly("a");
    }

    @Test
    void fromRandomDrawsFromTheSeededSource() {
        final Gen<Long> gen = Gen.fromRandom(random -> random.nextLong());
        final List<Long> drawn = gen.runCollectN(5, config(11));
        final java.util.SplittableRandom random = new java.util.SplittableRandom(11);
        assertThat(drawn).isEqualTo(List.of(random.nextLong(), random.nextLong(), random.nextLong(), random.nextLong(), random.nextLong()));
    }

    @Test
    void suspendBuildsTheGeneratorWhenItRuns() {
        final AtomicInteger built = new AtomicInteger();
        final Gen<Integer> gen = Gen.suspend(() -> {
            built.incrementAndGet();
            return Gen.constant(1);
        });
        assertThat(built).hasValue(0);
        assertThat(values(gen, 3)).isEqualTo(List.of(1, 1, 1));
        assertThat(built).hasValue(3);
    }

    /// A generator of nested lists that refers to itself.
    private static Gen<List<Object>> tree(int depth) {
        return depth == 0
                ? Gen.constant(List.empty())
                : Gen.oneOf(Gen.constant(List.empty()), Gen.suspend(() -> tree(depth - 1)).map(child -> List.<Object>of(child)));
    }

    @Test
    void suspendLetsAGeneratorReferToItself() {
        assertThat(values(tree(5), 100)).allMatch(t -> t.size() <= 1).anyMatch(t -> !t.isEmpty());
    }

    @Test
    void oneOfChoosesEveryGenerator() {
        assertThat(values(Gen.oneOf(Gen.constant(1), Gen.constant(2), Gen.fromIterable(java.util.List.of(3, 4))), 300))
                .containsOnly(1, 2, 3, 4).contains(1, 2, 3, 4);
        assertThat(pass(Gen.<Integer>oneOf())).isEmpty();
    }

    @Test
    void weightedFollowsTheWeights() {
        final List<Boolean> drawn = values(Gen.weighted(Tuple.of(Gen.constant(true), 9.0), Tuple.of(Gen.constant(false), 1.0)), 10_000);
        final long trues = drawn.count(b -> b);
        assertThat(trues).isBetween(8_700L, 9_300L);
    }

    @Test
    void weightedNeverChoosesAWeightOfZero() {
        assertThat(values(Gen.weighted(Tuple.of(Gen.constant(0), 0.0), Tuple.of(Gen.constant(1), 1.0), Tuple.of(Gen.constant(2), 0.0),
                Tuple.of(Gen.constant(3), 1.0), Tuple.of(Gen.constant(4), 0.0)), 2_000)).containsOnly(1, 3).contains(1, 3);
        assertThat(values(Gen.weighted(Tuple.of(Gen.constant(1), 0.0), Tuple.of(Gen.constant(2), 5.0)), 100)).containsOnly(2);
        assertThat(values(Gen.weighted(Tuple.of(Gen.constant(1), 5.0), Tuple.of(Gen.constant(2), 0.0)), 100)).containsOnly(1);
    }

    @Test
    void weightedWithoutGeneratorIsEmpty() {
        assertThat(pass(Gen.<Integer>weighted())).isEmpty();
    }

    @Test
    void weightedRejectsInvalidWeights() {
        assertThatThrownBy(() -> Gen.weighted(Tuple.of(Gen.constant(1), -1.0))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Gen.weighted(Tuple.of(Gen.constant(1), Double.NaN))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Gen.weighted(Tuple.of(Gen.constant(1), Double.POSITIVE_INFINITY))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Gen.weighted(Tuple.of(Gen.constant(1), 0.0), Tuple.of(Gen.constant(2), 0.0)))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("the weights add up to 0.0");
        assertThatThrownBy(() -> Gen.weighted(Tuple.of(Gen.constant(1), Double.MAX_VALUE), Tuple.of(Gen.constant(2), Double.MAX_VALUE)))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("the weights add up to Infinity");
        assertThatThrownBy(() -> Gen.weighted(Tuple.of(null, 1.0))).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Gen.weighted(Tuple.of(Gen.constant(1), null))).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Gen.weighted((Tuple2<Gen<Integer>, Double>) null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void unfoldGenNThreadsTheState() {
        assertThat(pass(Gen.unfoldGenN(5, 0, s -> Gen.constant(Tuple.of(s + 1, "x" + s)))))
                .isEqualTo(List.of(List.of("x0", "x1", "x2", "x3", "x4")));
        assertThat(pass(Gen.unfoldGenN(0, 0, s -> Gen.constant(Tuple.of(s + 1, s))))).isEqualTo(List.of(List.empty()));
    }

    @Test
    void unfoldGenNDrawsOneValuePerStep() {
        assertThat(pass(Gen.unfoldGenN(3, 0, s -> Gen.fromIterable(java.util.List.of(Tuple.of(s + 1, s), Tuple.of(s + 2, -s))))))
                .isEqualTo(List.of(List.of(0, 1, 2)));
    }

    @Test
    void unfoldGenIsSmall() {
        final List<List<Integer>> drawn = Gen.unfoldGen(0, s -> Gen.constant(Tuple.of(s + 1, s))).runCollect(config(1)).appendAll(
                Gen.unfoldGen(0, s -> Gen.constant(Tuple.of(s + 1, s))).runCollectN(200, config(2)));
        assertThat(drawn).allMatch(list -> list.size() <= 100);
        assertThat(drawn.map(List::size).max().get()).isGreaterThan(0);
    }

    @Test
    void unfoldGenRejectsInvalidArguments() {
        assertThatThrownBy(() -> Gen.unfoldGenN(-1, 0, s -> Gen.constant(Tuple.of(s, s)))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Gen.unfoldGenN(1, 0, null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Gen.unfoldGen(0, null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> pass(Gen.unfoldGenN(1, 0, s -> null))).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> pass(Gen.unfoldGenN(1, 0, s -> Gen.constant((Tuple2<Integer, Integer>) null))))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void collectAllGivesEveryCombinationOfFiniteGenerators() {
        assertThat(pass(Gen.collectAll(java.util.List.of(Gen.fromIterable(java.util.List.of(1, 2)), Gen.fromIterable(java.util.List.of(3, 4))))))
                .isEqualTo(List.of(List.of(1, 3), List.of(1, 4), List.of(2, 3), List.of(2, 4)));
        assertThat(pass(Gen.collectAll(java.util.List.<Gen<Integer>>of()))).isEqualTo(List.of(List.empty()));
        assertThat(pass(Gen.collectAll(java.util.List.of(Gen.constant(1), Gen.<Integer>empty())))).isEmpty();
    }

    @Test
    void collectAllCopiesItsInput() {
        final java.util.List<Gen<Integer>> gens = new ArrayList<>(java.util.List.of(Gen.constant(1)));
        final Gen<List<Integer>> gen = Gen.collectAll(gens);
        gens.add(Gen.constant(2));
        assertThat(pass(gen)).isEqualTo(List.of(List.of(1)));
    }

    // -- size

    @Test
    void sizeIsTheConfiguredSizeInOnePass() {
        assertThat(Gen.size().runCollect(config(1).withSize(7))).isEqualTo(List.of(7));
    }

    @Test
    void theSizeGrowsFromZeroToTheConfiguredSizeOverARun() {
        assertThat(Gen.size().runCollectN(5, config(1))).isEqualTo(List.of(0, 25, 50, 75, 100));
        assertThat(Gen.size().runCollectN(1, config(1))).isEqualTo(List.of(100));
        assertThat(Gen.size().runCollectN(3, config(1).withSize(0))).isEqualTo(List.of(0, 0, 0));
        final List<Integer> sizes = Gen.size().runCollectN(200, config(1));
        assertThat(sizes.asJava()).isSorted().startsWith(0, 0, 1, 1, 2).endsWith(99, 100);
    }

    @Test
    void theSizeOfARunDoesNotOverflow() {
        assertThat(Gen.size().runCollectN(3, config(1).withSize(Integer.MAX_VALUE)))
                .isEqualTo(List.of(0, Integer.MAX_VALUE / 2, Integer.MAX_VALUE));
    }

    @Test
    void sizedReadsTheSize() {
        assertThat(Gen.sized(n -> Gen.constant(n * 2)).runCollect(config(1).withSize(21))).isEqualTo(List.of(42));
        assertThatThrownBy(() -> Gen.sized(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> pass(Gen.sized(n -> null))).isInstanceOf(NullPointerException.class);
    }

    @Test
    void smallFavoursSmallSizes() {
        final List<Integer> sizes = Gen.small(Gen::constant).withSize(100).runCollectN(2_000, config(5));
        assertThat(sizes).allMatch(n -> n >= 0 && n <= 100);
        // an exponential distribution of mean 4
        assertThat(sizes.count(n -> n <= 10)).isGreaterThan(1_800);
        assertThat(sizes.count(n -> n == 0)).isGreaterThan(100);
        assertThat(sizes.average().get()).isBetween(3.0, 5.0);
    }

    @Test
    void smallRespectsItsMinimum() {
        assertThat(Gen.small(Gen::constant, 10).withSize(100).runCollectN(500, config(5))).allMatch(n -> n >= 10 && n <= 100);
        assertThat(Gen.small(Gen::constant, 10).withSize(3).runCollectN(20, config(5))).containsOnly(10);
        assertThat(Gen.small(Gen::constant).withSize(0).runCollectN(20, config(5))).containsOnly(0);
        assertThatThrownBy(() -> Gen.small(Gen::constant, -1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Gen.small(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> pass(Gen.small(n -> null))).isInstanceOf(NullPointerException.class);
    }

    @Test
    void largeIsUniformUpToTheSize() {
        final List<Integer> sizes = Gen.large(Gen::constant).withSize(100).runCollectN(5_000, config(5));
        assertThat(sizes).allMatch(n -> n >= 0 && n <= 100).contains(0, 100);
        assertThat(sizes.average().get()).isBetween(45.0, 55.0);
        assertThat(Gen.large(Gen::constant, 10).withSize(100).runCollectN(500, config(5))).allMatch(n -> n >= 10).contains(10, 100);
        assertThat(Gen.large(Gen::constant, 10).withSize(3).runCollectN(20, config(5))).containsOnly(10);
        assertThat(Gen.large(Gen::constant).withSize(Integer.MAX_VALUE).runCollectN(20, config(5))).allMatch(n -> n >= 0);
        assertThatThrownBy(() -> Gen.large(Gen::constant, -1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Gen.large(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> pass(Gen.large(n -> null))).isInstanceOf(NullPointerException.class);
    }

    @Test
    void withSizeFixesTheSizeOfOneGenerator() {
        assertThat(Gen.size().withSize(3).runCollectN(5, config(1))).containsOnly(3);
        assertThat(Gen.size().withSize(3).withSize(4).runCollect(config(1))).isEqualTo(List.of(3));
        assertThatThrownBy(() -> Gen.size().withSize(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void withSizeDoesNotLeakIntoTheNextGenerators() {
        final Gen<Tuple2<Integer, Integer>> gen = Gen.size().withSize(3).flatMap(inner -> Gen.size().map(outer -> Tuple.of(inner, outer)));
        assertThat(gen.runCollect(config(1).withSize(50))).isEqualTo(List.of(Tuple.of(3, 50)));
        assertThat(Gen.zip(Gen.size().withSize(3), Gen.size()).runCollect(config(1).withSize(50))).isEqualTo(List.of(Tuple.of(3, 50)));
    }

    // -- combinators

    @Test
    void mapTransformsEveryValue() {
        assertThat(pass(Gen.fromIterable(java.util.List.of(1, 2)).map(i -> i * 10))).isEqualTo(List.of(10, 20));
        assertThatThrownBy(() -> Gen.constant(1).map(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void flatMapRunsTheNextGeneratorForEveryValue() {
        final Gen<String> gen = Gen.fromIterable(java.util.List.of(1, 2)).flatMap(i -> Gen.fromIterable(java.util.List.of(i + "a", i + "b")));
        assertThat(pass(gen)).isEqualTo(List.of("1a", "1b", "2a", "2b"));
        assertThat(values(gen, 3)).isEqualTo(List.of("1a", "1b", "2a"));
        assertThat(pass(Gen.fromIterable(java.util.List.of(1, 2)).flatMap(i -> Gen.empty()))).isEmpty();
        assertThatThrownBy(() -> Gen.constant(1).flatMap(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> pass(Gen.constant(1).flatMap(i -> null))).isInstanceOf(NullPointerException.class)
                .hasMessage("flatMap: f returned null");
    }

    @Test
    void flatMapOfARandomGeneratorGivesOneValuePerPass() {
        assertThat(pass(Gen.integers().flatMap(i -> Gen.constant(i)))).hasSize(1);
        assertThat(pass(Gen.integers(0, 9).flatMap(i -> Gen.fromIterable(java.util.List.of(i, i))))).hasSize(2);
    }

    @Test
    void theRunStopsInTheMiddleOfAPass() {
        final AtomicInteger generated = new AtomicInteger();
        final Gen<Integer> gen = Gen.fromIterable(java.util.List.of(1, 2, 3, 4, 5)).map(i -> {
            generated.incrementAndGet();
            return i;
        });
        assertThat(values(gen, 2)).isEqualTo(List.of(1, 2));
        assertThat(generated).hasValue(2);
    }

    @Test
    void concatGivesBothSequencesInOnePass() {
        assertThat(pass(Gen.fromIterable(java.util.List.of(1, 2)).concat(Gen.constant(3)))).isEqualTo(List.of(1, 2, 3));
        assertThat(values(Gen.fromIterable(java.util.List.of(1, 2)).concat(Gen.constant(3)), 2)).isEqualTo(List.of(1, 2));
        assertThat(pass(Gen.<Integer>empty().concat(Gen.empty()))).isEmpty();
        assertThatThrownBy(() -> Gen.constant(1).concat(null)).isInstanceOf(NullPointerException.class);
    }

    // -- filter

    @Test
    void filterKeepsTheValuesThatSatisfyThePredicate() {
        assertThat(pass(Gen.fromIterable(java.util.List.of(1, 2, 3, 4, 5, 6)).filter(i -> i % 2 == 0))).isEqualTo(List.of(2, 4, 6));
        assertThat(pass(Gen.fromIterable(java.util.List.of(1, 2, 3, 4, 5, 6)).filterNot(i -> i % 2 == 0))).isEqualTo(List.of(1, 3, 5));
        assertThatThrownBy(() -> Gen.constant(1).filter(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Gen.constant(1).filterNot(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void aFilteredRandomGeneratorStillGivesOneValuePerPass() {
        final Gen<Integer> evens = Gen.integers().filter(i -> i % 2 == 0);
        assertThat(pass(evens)).hasSize(1);
        assertThat(values(evens, 500)).hasSize(500).allMatch(i -> i % 2 == 0);
    }

    @Test
    void aFilteredElementGeneratorStillFillsAString() {
        final List<String> strings = Gen.stringsN(100, Gen.alphaNumericChars().filter(Character::isDigit)).runCollectN(50, config(1));
        assertThat(strings).hasSize(50).allMatch(s -> s.length() == 100 && s.chars().allMatch(Character::isDigit));
    }

    @Test
    void aFilteredFiniteGeneratorIsNotRunAgain() {
        final AtomicInteger runs = new AtomicInteger();
        final Gen<Integer> gen = Gen.suspend(() -> {
            runs.incrementAndGet();
            return Gen.fromIterable(java.util.List.of(1, 3));
        }).filter(i -> i % 2 == 0);
        assertThat(pass(gen)).isEmpty();
        assertThat(runs).hasValue(1);
    }

    @Test
    void filterGivesUpAfterItsDiscardBudget() {
        assertThatThrownBy(() -> Gen.integers().filter(i -> false).runCollectN(1, config(1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Gen.filter rejected too many values: 1001 discards since the last sample, more than the discard budget of 1000;"
                        + " generate the wanted values with map or flatMap instead of filtering them");
        // one pass: the filter gives its pass up after a sixteenth of the budget, so a value is missing
        assertThatThrownBy(() -> Gen.integers().filter(i -> false).runCollect(config(1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Gen.filter gave a pass up after rejecting 62 values in a row;"
                        + " generate the wanted values with map or flatMap instead of filtering them");
    }

    /// A random generator of 0, 1, 2, ... in order, built afresh by each call.
    private static Gen<Integer> counter() {
        return Gen.fromRandom(new Function<java.util.random.RandomGenerator, Integer>() {
            private int next;

            @Override
            public Integer apply(java.util.random.RandomGenerator random) {
                return next++;
            }
        });
    }

    @Test
    void aFilterGivesItsPassUpAfterASixteenthOfTheBudget() {
        // 0, 1 and 2 are rejected before 3 passes: a budget of 64 lets a filter reject 3 in a row, 48 does not
        assertThat(counter().filter(i -> i % 4 == 3).runCollect(config(1).withMaxDiscards(64))).isEqualTo(List.of(3));
        assertThatThrownBy(() -> counter().filter(i -> i % 4 == 3).runCollect(config(1).withMaxDiscards(48)))
                .isInstanceOf(IllegalStateException.class).hasMessageStartingWith("Gen.filter gave a pass up after rejecting 3 values in a row");
        assertThat(counter().filter(i -> true).runCollect(config(1).withMaxDiscards(0))).isEqualTo(List.of(0));
        assertThatThrownBy(() -> counter().filter(i -> i > 0).runCollect(config(1).withMaxDiscards(0)))
                .hasMessageStartingWith("Gen.filter rejected too many values: 1 discards since the last sample");
    }

    @Test
    void aFilterThatGaveUpAPassIsRetriedAtTheNextPass() {
        // a budget of 32: a filter gives its pass up after 2 rejections; the next pass goes on from the counter
        assertThat(counter().filter(i -> i % 4 == 3).runCollectN(3, config(1).withMaxDiscards(32))).isEqualTo(List.of(3, 7, 11));
    }

    @Test
    void theBudgetStartsAgainAtEachSample() {
        // 3 discards per sample (2 rejections and an empty pass) fit a budget of 32 however many samples run
        assertThat(counter().filter(i -> i % 4 == 3).runCollectN(100, config(1).withMaxDiscards(32))).hasSize(100);
    }

    /// A predicate that counts its calls.
    private static <A> java.util.function.Predicate<A> counting(java.util.concurrent.atomic.AtomicLong calls, java.util.function.Predicate<A> predicate) {
        return a -> {
            calls.incrementAndGet();
            return predicate.test(a);
        };
    }

    @Test
    void aFilterThatNeverPassesCallsItsPredicateAtMostOnceMoreThanTheBudget() {
        for (int budget : new int[] { 0, 1, 2, 15, 16, 17, 100, 1000 }) {
            final java.util.concurrent.atomic.AtomicLong calls = new java.util.concurrent.atomic.AtomicLong();
            assertThatThrownBy(() -> Gen.integers().filter(counting(calls, i -> false)).runCollectN(10, config(1).withMaxDiscards(budget)))
                    .isInstanceOf(IllegalStateException.class).hasMessageStartingWith("Gen.filter rejected too many values");
            assertThat(calls.get()).as("budget " + budget).isPositive().isLessThanOrEqualTo(budget + 1L);
        }
    }

    @Test
    void nestedFiltersShareOneBudget() {
        final java.util.concurrent.atomic.AtomicLong calls = new java.util.concurrent.atomic.AtomicLong();
        final Gen<Integer> nested = Gen.fromIterable(java.util.List.of(1, 2))
                .flatMap(i -> i == 1 ? Gen.integers().filter(counting(calls, x -> false)) : Gen.integers())
                .filter(counting(calls, x -> false));
        assertThatThrownBy(() -> nested.runCollectN(200, config(1))).isInstanceOf(IllegalStateException.class)
                .hasMessageStartingWith("Gen.filter rejected too many values: 1001 discards");
        assertThat(calls.get()).isLessThanOrEqualTo(1001L);

        final java.util.concurrent.atomic.AtomicLong outer = new java.util.concurrent.atomic.AtomicLong();
        final java.util.concurrent.atomic.AtomicLong inner = new java.util.concurrent.atomic.AtomicLong();
        final Gen<Integer> threeLevels = Gen.integers().filter(counting(inner, x -> x % 2 == 0))
                .filter(counting(inner, x -> x % 3 == 0)).filter(counting(outer, x -> false));
        assertThatThrownBy(() -> threeLevels.runCollectN(200, config(1))).isInstanceOf(IllegalStateException.class);
        assertThat(outer.get()).isLessThanOrEqualTo(1001L);
        assertThat(inner.get()).isLessThanOrEqualTo(20_000L);
    }

    @Test
    void anInnerFilterThatAcceptsDoesNotRefillTheBudgetOfAnOuterOne() {
        final java.util.concurrent.atomic.AtomicLong outer = new java.util.concurrent.atomic.AtomicLong();
        final Gen<Integer> gen = Gen.integers().filter(i -> i % 2 == 0).filter(counting(outer, i -> false));
        assertThatThrownBy(() -> gen.runCollectN(10, config(1))).isInstanceOf(IllegalStateException.class)
                .hasMessageStartingWith("Gen.filter rejected too many values");
        assertThat(outer.get()).isLessThanOrEqualTo(1001L);
    }

    @Test
    void theDiscardBudgetDoesNotOverflowAtItsLargestValue() {
        assertThat(Sampling.exceeds(Integer.MAX_VALUE, Integer.MAX_VALUE)).isFalse();
        assertThat(Sampling.exceeds(Integer.MAX_VALUE + 1L, Integer.MAX_VALUE)).isTrue();
        assertThat(Sampling.exceeds(1, 0)).isTrue();
        assertThat(Sampling.exceeds(0, 0)).isFalse();
        final CheckConfig largest = config(1).withMaxDiscards(Integer.MAX_VALUE);
        assertThat(Gen.integers().filter(i -> i % 2 == 0).runCollectN(20, largest)).hasSize(20);
        assertThat(Gen.sized(n -> n < 5 ? Gen.<Integer>empty() : Gen.constant(n)).runCollectN(2, largest.withSize(10))).isEqualTo(List.of(7, 10));
    }

    @Test
    void aFilterThatRejectsEveryValueOfAFiniteGeneratorExhaustsTheRun() {
        assertThatThrownBy(() -> Gen.fromIterable(java.util.List.of(1, 3)).filter(i -> i % 2 == 0).runCollectN(1, config(1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("the generator produced no value: 1001 discards since the last sample, more than the discard budget of 1000");
        // a finite generator only loses the rejected values, however many in a row, and spends no budget on them
        assertThat(pass(Gen.fromIterable(List.range(0, 3_000)).filter(i -> i >= 2_500))).hasSize(500);
    }

    // -- the size after a pass without a value

    @Test
    void anEmptyPassMovesTheNextOneToADoubledSize() {
        final Gen<Integer> fromSizeThree = Gen.sized(n -> n < 3 ? Gen.<Integer>empty() : Gen.constant(n));
        // the first sample runs at size 0, finds nothing at 0 and 1, and is taken at size 3
        assertThat(fromSizeThree.runCollectN(5, config(1))).isEqualTo(List.of(3, 25, 50, 75, 100));
        // 0, 1, 3, 7, 15, 31, 63, then the configured size
        assertThat(Gen.sized(n -> Gen.constant(n)).filter(n -> n >= 64).runCollectN(2, config(1))).isEqualTo(List.of(100, 100));
    }

    @Test
    void theSizeAfterEmptyPassesStopsAtTheConfiguredSize() {
        final Gen<Integer> never = Gen.sized(n -> n <= 5 ? Gen.<Integer>empty() : Gen.constant(n));
        assertThatThrownBy(() -> never.runCollectN(1, config(1).withSize(5).withMaxDiscards(50)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("the generator produced no value: 51 discards since the last sample, more than the discard budget of 50");
    }

    @Test
    void theGrowthDoublesUpToTheLargestSize() {
        final Sampling sampling = new Sampling(1, 10, 100);
        assertThat(sampling.grow(0, 0)).isEqualTo(0);
        assertThat(sampling.grow(0, 1)).isEqualTo(1);
        assertThat(sampling.grow(0, 2)).isEqualTo(3);
        assertThat(sampling.grow(0, 6)).isEqualTo(63);
        assertThat(sampling.grow(0, 7)).isEqualTo(100);
        assertThat(sampling.grow(50, 3)).isEqualTo(57);
        assertThat(sampling.grow(0, 62)).isEqualTo(100);
        assertThat(sampling.grow(0, Long.MAX_VALUE)).isEqualTo(100);
        // a size set above the largest one by withSize is not lowered
        assertThat(sampling.grow(300, 5)).isEqualTo(300);
        assertThat(new Sampling(1, 10).grow(Integer.MAX_VALUE - 1, 5)).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void aFilterThatRejectsTheSmallestValuesWorksWithTheDefaultConfiguration() {
        for (long seed = 0; seed < 20; seed++) {
            final CheckConfig config = CheckConfig.defaults().withSeed(seed);
            assertThat(Gen.alphaNumericStrings().filter(s -> !s.isEmpty()).runCollectN(200, config)).hasSize(200).noneMatch(String::isEmpty);
            assertThat(Gen.strings().filter(s -> !s.isEmpty()).runCollectN(200, config)).hasSize(200);
            assertThat(Gen.sized(n -> Gen.integers(0, n)).filter(x -> x > 0).runCollectN(200, config)).hasSize(200);
            assertThat(Gen.sized(n -> Gen.integers(0, n)).filter(x -> x > 90).runCollectN(200, config)).hasSize(200).allMatch(x -> x > 90);
            assertThat(Gen.sized(n -> Gen.integers(0, n)).filter(x -> x >= 100).runCollectN(200, config)).hasSize(200).allMatch(x -> x == 100);
        }
    }

    // -- zip

    @Test
    void zipGivesEveryPairOfFiniteGenerators() {
        final Gen<Integer> ab = Gen.fromIterable(java.util.List.of(1, 2));
        assertThat(pass(ab.zip(Gen.fromIterable(java.util.List.of("a", "b")))))
                .isEqualTo(List.of(Tuple.of(1, "a"), Tuple.of(1, "b"), Tuple.of(2, "a"), Tuple.of(2, "b")));
        assertThat(pass(ab.zipWith(Gen.constant(10), Integer::sum))).isEqualTo(List.of(11, 12));
        assertThat(pass(Gen.zip(ab, Gen.empty()))).isEmpty();
    }

    @Test
    void zipAtEveryArityGivesTheComponentsInOrder() {
        final Gen<Integer> g1 = Gen.constant(1);
        final Gen<Integer> g2 = Gen.constant(2);
        final Gen<Integer> g3 = Gen.constant(3);
        final Gen<Integer> g4 = Gen.constant(4);
        final Gen<Integer> g5 = Gen.constant(5);
        final Gen<Integer> g6 = Gen.constant(6);
        final Gen<Integer> g7 = Gen.constant(7);
        final Gen<Integer> g8 = Gen.constant(8);
        assertThat(pass(Gen.zip(g1, g2))).isEqualTo(List.of(Tuple.of(1, 2)));
        assertThat(pass(Gen.zip(g1, g2, g3))).isEqualTo(List.of(Tuple.of(1, 2, 3)));
        assertThat(pass(Gen.zip(g1, g2, g3, g4))).isEqualTo(List.of(Tuple.of(1, 2, 3, 4)));
        assertThat(pass(Gen.zip(g1, g2, g3, g4, g5))).isEqualTo(List.of(Tuple.of(1, 2, 3, 4, 5)));
        assertThat(pass(Gen.zip(g1, g2, g3, g4, g5, g6))).isEqualTo(List.of(Tuple.of(1, 2, 3, 4, 5, 6)));
        assertThat(pass(Gen.zip(g1, g2, g3, g4, g5, g6, g7))).isEqualTo(List.of(Tuple.of(1, 2, 3, 4, 5, 6, 7)));
        assertThat(pass(Gen.zip(g1, g2, g3, g4, g5, g6, g7, g8))).isEqualTo(List.of(Tuple.of(1, 2, 3, 4, 5, 6, 7, 8)));
        assertThat(pass(Gen.zipWith(g1, g2, (a, b) -> "" + a + b))).isEqualTo(List.of("12"));
        assertThat(pass(Gen.zipWith(g1, g2, g3, (a, b, c) -> "" + a + b + c))).isEqualTo(List.of("123"));
        assertThat(pass(Gen.zipWith(g1, g2, g3, g4, (a, b, c, d) -> "" + a + b + c + d))).isEqualTo(List.of("1234"));
        assertThat(pass(Gen.zipWith(g1, g2, g3, g4, g5, (a, b, c, d, e) -> "" + a + b + c + d + e))).isEqualTo(List.of("12345"));
        assertThat(pass(Gen.zipWith(g1, g2, g3, g4, g5, g6, (a, b, c, d, e, f) -> "" + a + b + c + d + e + f))).isEqualTo(List.of("123456"));
        assertThat(pass(Gen.zipWith(g1, g2, g3, g4, g5, g6, g7, (a, b, c, d, e, f, g) -> "" + a + b + c + d + e + f + g)))
                .isEqualTo(List.of("1234567"));
        assertThat(pass(Gen.zipWith(g1, g2, g3, g4, g5, g6, g7, g8, (a, b, c, d, e, f, g, h) -> "" + a + b + c + d + e + f + g + h)))
                .isEqualTo(List.of("12345678"));
    }

    @Test
    void zipAtEveryArityGivesEveryCombination() {
        final Gen<Integer> two = Gen.fromIterable(java.util.List.of(0, 1));
        assertThat(pass(Gen.zip(two, two, two))).hasSize(8).doesNotHaveDuplicates();
        assertThat(pass(Gen.zip(two, two, two, two, two, two, two, two))).hasSize(256).doesNotHaveDuplicates();
        assertThat(pass(Gen.zipWith(two, two, two, two, two, (a, b, c, d, e) -> a + 2 * b + 4 * c + 8 * d + 16 * e)))
                .isEqualTo(List.rangeClosed(0, 31).map(i -> Integer.reverse(i) >>> 27));
    }

    @Test
    void zipRejectsNulls() {
        final Gen<Integer> g = Gen.constant(1);
        assertThatThrownBy(() -> g.zip(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> g.zipWith(g, null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Gen.zip(null, g)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Gen.zip(g, g, null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Gen.zip(g, g, g, null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Gen.zip(g, g, g, g, null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Gen.zip(g, g, g, g, g, null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Gen.zip(g, g, g, g, g, g, null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Gen.zip(g, g, g, g, g, g, g, null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Gen.zipWith(g, g, g, g, g, g, g, g, null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Gen.zipWith(null, g, g, g, g, g, g, g, (a, b, c, d, e, f, h, i) -> a)).isInstanceOf(NullPointerException.class);
    }

    // -- draw

    @Test
    void drawTakesTheFirstValueOfAPass() {
        final Sampling sampling = new Sampling(1, 10);
        assertThat(Gen.fromIterable(java.util.List.of(5, 6)).draw(sampling, 0)).isEqualTo(5);
    }

    @Test
    void drawRunsARandomPassAgainUntilItGivesAValue() {
        final Sampling sampling = new Sampling(1, 1000);
        final Gen<Integer> sometimes = Gen.oneOf(Gen.empty(), Gen.constant(1));
        for (int i = 0; i < 100; i++) {
            assertThat(sometimes.draw(sampling, 0)).isEqualTo(1);
        }
    }

    @Test
    void drawGivesUpOnAnEmptyGenerator() {
        assertThatThrownBy(() -> Gen.empty().draw(new Sampling(1, 1000), 4))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("the generator produced no value: 1001 discards since the last sample, more than the discard budget of 1000");
        assertThatThrownBy(() -> Gen.oneOf(Gen.empty(), Gen.empty()).draw(new Sampling(1, 3), 4))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("the generator produced no value: 4 discards since the last sample, more than the discard budget of 3");
        assertThatThrownBy(() -> Gen.integers().filter(i -> false).draw(new Sampling(1, 3), 4))
                .isInstanceOf(IllegalStateException.class).hasMessageStartingWith("Gen.filter rejected too many values: 4 discards");
    }

    @Test
    void drawRetriesAnEmptyPassAtALargerSize() {
        final Gen<Integer> fromSizeThree = Gen.sized(n -> n < 3 ? Gen.<Integer>empty() : Gen.constant(n));
        assertThat(fromSizeThree.draw(new Sampling(1, 10, 10), 0)).isEqualTo(3);
        final Gen<Character> firstLetters = Gen.strings(Gen.alphaChars()).filter(s -> !s.isEmpty()).map(s -> s.charAt(0));
        assertThat(Gen.stringsN(5, firstLetters).withSize(0).runCollect(config(1).withSize(10))).hasSize(1).allMatch(s -> s.length() == 5);
    }

    @Test
    void drawDoesNotGoAboveTheConfiguredSize() {
        final Gen<Integer> fromSizeThree = Gen.sized(n -> n < 3 ? Gen.<Integer>empty() : Gen.constant(n));
        assertThatThrownBy(() -> fromSizeThree.draw(new Sampling(1, 10, 2), 0)).isInstanceOf(IllegalStateException.class)
                .hasMessage("the generator produced no value: 11 discards since the last sample, more than the discard budget of 10");
        final Gen<Character> firstLetters = Gen.strings(Gen.alphaChars()).filter(s -> !s.isEmpty()).map(s -> s.charAt(0));
        assertThatThrownBy(() -> Gen.stringsN(5, firstLetters).runCollect(config(1).withSize(0))).isInstanceOf(IllegalStateException.class);
        final CheckResult result = Check.check(config(1),
                Gen.stringsN(1, Gen.sized(n -> Gen.constant((char) (int) n)).filter(c -> c > 150)).map(s -> (int) s.charAt(0)), x -> x <= 100);
        assertThat(result.isErroneous()).as(result.toString()).isTrue();
    }

    @Test
    void aDrawThatSucceedsAfterAFilterGaveUpLosesNoValue() {
        // the element filter gives its first pass up (budget 32: after 2 rejections), the draw runs it again
        final Gen<String> gen = Gen.stringsN(1, counter().filter(i -> i % 4 == 3).map(i -> (char) ('a' + i)));
        assertThat(gen.runCollect(config(1).withMaxDiscards(32))).isEqualTo(List.of("d"));
    }

    // -- runs

    @Test
    void runCollectNRejectsInvalidArguments() {
        assertThatThrownBy(() -> Gen.constant(1).runCollectN(-1, config(1))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Gen.constant(1).runCollectN(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Gen.constant(1).runCollectN(1, null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Gen.constant(1).runCollect(null)).isInstanceOf(NullPointerException.class);
        assertThat(Gen.constant(1).runCollectN(0, config(1))).isEmpty();
    }

    @Test
    void constructorsRejectNulls() {
        assertThatThrownBy(() -> Gen.fromIterable(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Gen.elements((Object[]) null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Gen.fromRandom(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Gen.suspend(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> pass(Gen.suspend(() -> null))).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Gen.oneOf((Gen<Integer>[]) null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Gen.oneOf(Gen.constant(1), null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Gen.weighted((Tuple2<Gen<Integer>, Double>[]) null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Gen.collectAll(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Gen.collectAll(Arrays.asList(Gen.constant(1), null))).isInstanceOf(NullPointerException.class);
    }

    @Test
    void anIteratorThatEndsEarlyIsCopiedOnce() {
        final Iterable<Integer> twice = () -> new Iterator<>() {
            private int left = 2;

            @Override
            public boolean hasNext() {
                return left > 0;
            }

            @Override
            public Integer next() {
                if (left == 0) {
                    throw new NoSuchElementException();
                }
                return left--;
            }
        };
        assertThat(pass(Gen.fromIterable(twice))).isEqualTo(List.of(2, 1));
    }

    @Test
    void everyValueOfARandomGeneratorComesFromTheSameRun() {
        // two draws in one pass differ: the source is shared, not reset
        final Set<Tuple2<Long, Long>> pairs = new HashSet<>(Gen.zip(Gen.fromRandom(r -> r.nextLong()), Gen.fromRandom(r -> r.nextLong())).runCollectN(100, config(1)).asJava());
        assertThat(pairs).hasSize(100).allMatch(pair -> !pair._1().equals(pair._2()));
        final Map<Long, Integer> counts = new HashMap<>();
        Gen.longs(0, 1_000_000_000L).runCollectN(1_000, config(1)).forEach(l -> counts.merge(l, 1, Integer::sum));
        assertThat(counts.size()).isGreaterThan(400);
    }
}
