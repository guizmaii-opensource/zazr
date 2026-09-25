package com.guizmaii.zazr.test.laws;

import com.guizmaii.zazr.Tuple2;
import com.guizmaii.zazr.collection.LinkedHashSet;
import com.guizmaii.zazr.collection.NonEmptyVector;
import com.guizmaii.zazr.collection.TreeSet;
import com.guizmaii.zazr.collection.Vector;
import com.guizmaii.zazr.control.Option;
import com.guizmaii.zazr.test.Check;
import com.guizmaii.zazr.test.CheckConfig;
import com.guizmaii.zazr.test.CheckResult;
import com.guizmaii.zazr.test.Gen;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The law framework: a law reports its name, its counterexample and the seed that replays it, law sets compose in order and report every failing
 * law, and each law family catches a broken implementation.
 */
class LawsTest {

    /// A vector whose `map` drops the last element: it breaks `mapIdentity`.
    private static final MapSubject<Vector<?>> BROKEN_MAP = new MapSubject<>() {

        @Override
        public Gen<Vector<?>> values() {
            return Gen.vector(Values.integers()).map(v -> v);
        }

        @Override
        public Vector<?> map(Vector<?> fa, Function<Object, Object> f) {
            return fa.isEmpty() ? fa.map(f) : fa.map(f).dropRight(1);
        }
    };

    /// A vector whose `zip` pairs in reverse order: it breaks `zipAssociativity`.
    private static final ZipSubject<Vector<?>> BROKEN_ZIP = new ZipSubject<>() {

        @Override
        public Gen<Vector<?>> values() {
            return Gen.vector(Values.integers()).map(v -> v);
        }

        @Override
        public Vector<?> map(Vector<?> fa, Function<Object, Object> f) {
            return fa.map(f);
        }

        @Override
        public Vector<?> zip(Vector<?> fa, Vector<?> fb) {
            return fa.reverse().zip(fb);
        }
    };

    /// A small check: 100 samples up to size 10.
    private static final CheckConfig SMALL = new CheckConfig(100, 10, 1, 1000);

    @Test
    void failingLawReportsItsCounterexampleAndExplanation() {
        final CheckResult result = MapLaws.<Vector<?>>mapIdentity().check(BROKEN_MAP, SMALL);
        assertThat(result).isInstanceOf(CheckResult.Falsified.class);
        final CheckResult.Falsified falsified = (CheckResult.Falsified) result;
        assertThat(falsified.seed()).isEqualTo(SMALL.seed());
        assertThat(falsified.counterexample().toString()).startsWith("(Vector(");
        assertThat(result.message().get()).startsWith("left = Vector(");
    }

    @Test
    void lawResultsCarryTheLawName() {
        final Vector<LawResult> results = MapLaws.<Vector<?>>all().check(BROKEN_MAP, SMALL);
        assertThat(results.map(LawResult::name)).containsExactly("mapIdentity", "mapComposition");
        assertThat(results.map(LawResult::isSatisfied)).containsExactly(false, false);
        assertThat(results.head().result()).isEqualTo(MapLaws.<Vector<?>>mapIdentity().check(BROKEN_MAP, SMALL));
    }

    @Test
    void assertSatisfiedListsEveryFailingLaw() {
        final Laws<MapSubject<Vector<?>>> laws = MapLaws.all();
        assertThatThrownBy(() -> laws.assertSatisfied(BROKEN_MAP, SMALL))
                .isInstanceOf(AssertionError.class)
                .hasMessageStartingWith("2 law(s) failed:\nmapIdentity: falsified at sample ")
                .hasMessageContaining("\nmapComposition: falsified at sample ")
                .hasMessageContaining(" by (Vector(")
                .hasMessageContaining(": left = Vector(")
                .hasMessageContaining("(seed 1, replay with -Dzazr.check.seed=1)");
    }

    @Test
    void theReportedSeedReplaysTheFailure() {
        final CheckResult.Falsified first = (CheckResult.Falsified) MapLaws.<Vector<?>>mapIdentity()
                .check(BROKEN_MAP, LawChecks.config("mapIdentity"));
        assertThat(first.seed()).isEqualTo(LawChecks.seed("mapIdentity"));
        final CheckResult replayed = MapLaws.<Vector<?>>mapIdentity()
                .check(BROKEN_MAP, CheckConfig.defaults().withSamples(LawChecks.SAMPLES).withSeed(first.seed()));
        assertThat(replayed).isEqualTo(first);
    }

    @Test
    void lawChecksReportEveryFailingLawWithItsOwnSeed() {
        assertThatThrownBy(() -> LawChecks.check(MapLaws.<Vector<?>>all(), BROKEN_MAP))
                .hasMessageStartingWith("2 law(s) failed:\nmapIdentity: falsified at sample ")
                .hasMessageContaining("-Dzazr.check.seed=" + LawChecks.seed("mapIdentity") + ")")
                .hasMessageContaining("-Dzazr.check.seed=" + LawChecks.seed("mapComposition") + ")");
    }

    @Test
    void zipLawsCatchABrokenZip() {
        assertThat(ZipLaws.<Vector<?>>zipAssociativity().check(BROKEN_ZIP, SMALL).isFalsified()).isTrue();
    }

    @Test
    void lawSetsComposeInOrder() {
        final Laws<ZipSubject<Vector<?>>> laws = MapLaws.<Vector<?>>all().and(ZipLaws.<Vector<?>>zip());
        assertThat(laws.laws().map(Law::name)).containsExactly("mapIdentity", "mapComposition", "zipAssociativity");
        assertThat(laws.check(BROKEN_ZIP, SMALL).map(LawResult::isSatisfied)).containsExactly(true, true, false);
        assertThat(MapLaws.<Vector<?>>mapIdentity().and(MapLaws.mapComposition()).laws().map(Law::name))
                .containsExactly("mapIdentity", "mapComposition");
    }

    @Test
    void satisfiedLawsDoNotThrow() {
        MapLaws.<Vector<?>>all().assertSatisfied(new VectorLawsTest().subject(), SMALL);
        MapLaws.<Vector<?>>all().assertSatisfied(new VectorLawsTest().subject());
        assertThat(MapLaws.<Vector<?>>all().check(new VectorLawsTest().subject()).map(LawResult::isSatisfied))
                .containsExactly(true, true);
        assertThat(MapLaws.<Vector<?>>mapIdentity().check(new VectorLawsTest().subject()).isSatisfied()).isTrue();
        assertThat(new LawResult("law", new CheckResult.Satisfied(3)).describe()).isEqualTo("law: satisfied (3 samples)");
    }

    @Test
    void erroneousLawIsReported() {
        final Law<String> throwing = Law.of("throwing", (subject, config) -> Check.check(config, Values.integers(), i -> {
            throw new IllegalStateException("boom");
        }));
        assertThatThrownBy(() -> Laws.<String>of(throwing).assertSatisfied("subject", SMALL))
                .hasMessageStartingWith("1 law(s) failed:\nthrowing: erroneous at sample 1 with (0): ")
                .hasMessageContaining("boom")
                .hasMessageEndingWith("(seed 1, replay with -Dzazr.check.seed=1)");
        final Law<String> generating = Law.of("generating", (subject, config) -> Check.check(config,
                Values.integers().map(i -> {
                    throw new IllegalStateException("no value");
                }), i -> true));
        assertThatThrownBy(() -> Laws.<String>of(generating).assertSatisfied("subject", SMALL))
                .hasMessageContaining("generating: erroneous at sample 1, while generating it: ")
                .hasMessageContaining("no value");
    }

    @Test
    void lawNamesAreRequired() {
        assertThatThrownBy(() -> Law.of(" ", (subject, config) -> null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Law.of(null, (subject, config) -> null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Law.of("null", (subject, config) -> null).check("subject", SMALL))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("the check of null returned null");
        assertThatThrownBy(() -> new LawResult(null, new CheckResult.Satisfied(0))).isInstanceOf(NullPointerException.class);
        assertThat(MapLaws.mapIdentity().toString()).isEqualTo("Law(mapIdentity)");
        assertThat(MapLaws.all().toString()).isEqualTo("Laws(mapIdentity, mapComposition)");
    }

    // -- every law family catches a broken implementation

    /// 500 samples up to size 20.
    private static final CheckConfig MUTANTS = new CheckConfig(500, 20, 3, 1000);

    private static <S> void assertFalsified(Law<? super S> law, S subject) {
        final CheckResult result = law.check(subject, MUTANTS);
        assertThat(result.isFalsified()).as(law.name() + " catches the defect: " + result).isTrue();
    }

    /// A vector whose `flatMap` drops the last element of its result.
    private static final FlatMapSubject<Vector<?>> BROKEN_FLAT_MAP = new FlatMapSubject<>() {

        @Override
        public Gen<Vector<?>> values() {
            return Gen.vector(Values.integers()).map(v -> v);
        }

        @Override
        public Vector<?> map(Vector<?> fa, Function<Object, Object> f) {
            return fa.map(f);
        }

        @Override
        public Vector<?> succeed(Object a) {
            return Vector.of(a);
        }

        @Override
        public Vector<?> flatMap(Vector<?> fa, Function<Object, Vector<?>> f) {
            final Vector<?> result = fa.flatMap(f);
            return result.isEmpty() ? result : result.dropRight(1);
        }
    };

    @Test
    void flatMapLawsCatchABrokenFlatMap() {
        assertFalsified(FlatMapLaws.<Vector<?>>flatMapAssociativity(), BROKEN_FLAT_MAP);
        assertFalsified(FlatMapLaws.<Vector<?>>flatMapLeftIdentity(), BROKEN_FLAT_MAP);
        assertFalsified(FlatMapLaws.<Vector<?>>flatMapRightIdentity(), BROKEN_FLAT_MAP);
        assertFalsified(FlatMapLaws.<Vector<?>>mapIsFlatMapSucceed(), BROKEN_FLAT_MAP);
    }

    /// An option whose `zipLeft` keeps the right side and whose `zipRight` keeps the left side.
    private static final ZipSidesSubject<Option<?>> SWAPPED_SIDES = new ZipSidesSubject<>() {

        @Override
        public Gen<Option<?>> values() {
            return Gen.option(Values.integers()).map(o -> o);
        }

        @Override
        public Option<?> map(Option<?> fa, Function<Object, Object> f) {
            return fa.map(f);
        }

        @Override
        public Option<?> zip(Option<?> fa, Option<?> fb) {
            return fa.zip(fb);
        }

        @Override
        public Option<?> succeed(Object a) {
            return Option.some(a);
        }

        @Override
        public Option<?> zipLeft(Option<?> fa, Option<?> fb) {
            return fb;
        }

        @Override
        public Option<?> zipRight(Option<?> fa, Option<?> fb) {
            return fa;
        }
    };

    @Test
    void zipSideLawsCatchSwappedSides() {
        assertFalsified(ZipLaws.<Option<?>>zipLeftIdentity(), SWAPPED_SIDES);
        assertFalsified(ZipLaws.<Option<?>>zipRightIdentity(), SWAPPED_SIDES);
        assertFalsified(ZipLaws.<Option<?>>zipLeftIsZipThenFirst(), SWAPPED_SIDES);
        assertFalsified(ZipLaws.<Option<?>>zipRightIsZipThenSecond(), SWAPPED_SIDES);
    }

    /// Equal by value, hashed by identity.
    private record IdentityHashed(int value) {

        @Override
        public int hashCode() {
            return System.identityHashCode(this);
        }
    }

    @Test
    void equalityLawCatchesInconsistentHashCodesAndUnequalCopies() {
        assertFalsified(EqualityLaws.<IdentityHashed>equalsHashCodeConsistency(),
                new EqualitySubject<>(Values.integers().map(IdentityHashed::new), h -> new IdentityHashed(h.value()), IdentityHashed::value));
        assertFalsified(EqualityLaws.<Integer>equalsHashCodeConsistency(),
                new EqualitySubject<>(Values.integers(), i -> i + 1, i -> i));
    }

    @Test
    void collectionLawsCatchBrokenContracts() {
        final Gen<Vector<Integer>> vectors = Gen.vector(Values.integers());
        assertFalsified(CollectionLaws.<Integer, Vector<Integer>>sizeEqualsIterationCount(),
                new CollectionSubject<>(vectors, Vector::ofAll, v -> v.size() + 1, Vector::toList, true));
        assertFalsified(CollectionLaws.<Integer, Vector<Integer>>toListRoundTrip(),
                new CollectionSubject<>(vectors, xs -> Vector.ofAll(xs).reverse(), Vector::size, Vector::toList, true));
        // a vector's equals depends on the order, a linked hash set's does not: declaring the opposite is caught
        assertFalsified(CollectionLaws.<Integer, Vector<Integer>>equalsAgreesWithElements(),
                new CollectionSubject<>(vectors, Vector::ofAll, Vector::size, Vector::toList, false));
        assertFalsified(CollectionLaws.<Integer, LinkedHashSet<Integer>>equalsAgreesWithElements(),
                new CollectionSubject<>(Gen.linkedHashSet(Values.integers()), LinkedHashSet::ofAll, LinkedHashSet::size,
                        LinkedHashSet::toList, true));
        assertFalsified(CollectionLaws.<Integer, NonEmptyVector<Integer>>sequenceEqualsAcrossTypes(),
                new CollectionSubject<>(Gen.nonEmptyVector(Values.integers()), xs -> NonEmptyVector.fromVector(Vector.ofAll(xs)).get(),
                        NonEmptyVector::size, NonEmptyVector::toList, true));
        assertFalsified(CollectionLaws.<Integer, Vector<Integer>>setEqualsAcrossTypes(),
                new CollectionSubject<>(vectors, Vector::ofAll, Vector::size, Vector::toList, false));
        assertFalsified(CollectionLaws.<Tuple2<Integer, Integer>, Vector<Tuple2<Integer, Integer>>>mapEqualsAcrossTypes(),
                new CollectionSubject<>(Gen.vector(Gen.tuple2(Values.integers(), Values.integers())), Vector::ofAll,
                        Vector::size, Vector::toList, false));
    }

    @Test
    void collectorLawCatchesABrokenCollector() {
        final Collector<Integer, ArrayList<Integer>, Vector<Integer>> reversing = Collector.of(ArrayList::new, ArrayList::add,
                (left, right) -> {
                    left.addAll(right);
                    return left;
                }, list -> Vector.ofAll(list).reverse());
        assertFalsified(BuilderLaws.<Integer, Vector<Integer>>collectorResultEqualsOfAll(),
                new BuilderLaws.CollectorSubject<>(Gen.list(Values.integers()), reversing, Vector::ofAll));
    }

    /// Equal when the first components are: too coarse.
    private record FirstOnly(int a, int b) {

        @Override
        public boolean equals(Object o) {
            return o instanceof FirstOnly that && that.a == a;
        }

        @Override
        public int hashCode() {
            return a;
        }
    }

    /// Every value equal to every other, hashed to 0.
    private record AllEqual(int a, int b) {

        @Override
        public boolean equals(Object o) {
            return o instanceof AllEqual;
        }

        @Override
        public int hashCode() {
            return 0;
        }
    }

    @Test
    void modelLawCatchesATooCoarseEquals() {
        final Gen<FirstOnly> firstOnly = Gen.tuple2(Values.integers(), Values.integers()).map(t -> new FirstOnly(t._1(), t._2()));
        final EqualitySubject<FirstOnly> firstOnlySubject = new EqualitySubject<>(firstOnly, x -> new FirstOnly(x.a(), x.b()),
                x -> java.util.List.of(x.a(), x.b()));
        final Gen<AllEqual> allEqual = Gen.tuple2(Values.integers(), Values.integers()).map(t -> new AllEqual(t._1(), t._2()));
        final EqualitySubject<AllEqual> allEqualSubject = new EqualitySubject<>(allEqual, x -> new AllEqual(x.a(), x.b()),
                x -> java.util.List.of(x.a(), x.b()));
        // both pass the hash-code law: nothing there requires different values to be unequal
        final CheckConfig thorough = new CheckConfig(1000, 100, 3, 1000);
        assertThat(EqualityLaws.<FirstOnly>equalsHashCodeConsistency().check(firstOnlySubject, thorough).isSatisfied()).isTrue();
        assertThat(EqualityLaws.<AllEqual>equalsHashCodeConsistency().check(allEqualSubject, thorough).isSatisfied()).isTrue();
        assertFalsified(EqualityLaws.<FirstOnly>equalsAgreesWithModel(), firstOnlySubject);
        assertFalsified(EqualityLaws.<AllEqual>equalsAgreesWithModel(), allEqualSubject);
    }

    @Test
    void iterationOrderLawCatchesALostOrder() {
        final Gen<LinkedHashSet<Integer>> linked = Gen.linkedHashSet(Values.integers());
        final CollectionSubject<Integer, LinkedHashSet<Integer>> reversedLinked = new CollectionSubject<>(linked,
                xs -> LinkedHashSet.ofAll(Vector.ofAll(xs).reverse()), LinkedHashSet::size, LinkedHashSet::toList, false,
                Option.some(IterationOrder.firstOccurrence()));
        final Gen<TreeSet<Integer>> sorted = Gen.treeSet(Values.integers());
        final CollectionSubject<Integer, TreeSet<Integer>> unsortedTree = new CollectionSubject<>(sorted,
                xs -> TreeSet.ofAll(Comparator.<Integer>reverseOrder(), xs), TreeSet::size, TreeSet::toList, false,
                Option.some(IterationOrder.sorted(Comparator.<Integer>naturalOrder())));
        // every other collection law is satisfied by both: they compare sets as sets
        final CheckConfig config = new CheckConfig(200, 100, 3, 1000);
        assertThat(CollectionLaws.<Integer, LinkedHashSet<Integer>>set().check(reversedLinked, config)
                .map(LawResult::isSatisfied)).containsExactly(true, true, true, false, true);
        assertThat(CollectionLaws.<Integer, TreeSet<Integer>>set().check(unsortedTree, config)
                .map(LawResult::isSatisfied)).containsExactly(true, true, true, false, true);
        assertFalsified(CollectionLaws.<Integer, LinkedHashSet<Integer>>iterationOrder(), reversedLinked);
        assertFalsified(CollectionLaws.<Integer, TreeSet<Integer>>iterationOrder(), unsortedTree);

        final Collector<Integer, ?, LinkedHashSet<Integer>> reversing = Collectors.collectingAndThen(Collectors.toList(),
                list -> LinkedHashSet.ofAll(Vector.ofAll(list).reverse()));
        assertFalsified(BuilderLaws.<Integer, LinkedHashSet<Integer>>collectorResultEqualsOfAll(), new BuilderLaws.CollectorSubject<>(
                Gen.list(Values.integers()), reversing, LinkedHashSet::ofAll, Option.some(IterationOrder.firstOccurrence())));
    }

    @Test
    void generatedFunctionsDependOnTheirArgument() {
        final Gen<Function<Object, Option<?>>> functions = Functions.to(Gen.option(Values.integers()).map(o -> (Option<?>) o), 8);
        int varying = 0;
        for (final Function<Object, Option<?>> f : functions.runCollectN(1000, new CheckConfig(1000, 100, 11, 1000))) {
            boolean some = false;
            boolean none = false;
            for (int x = -8; x <= 8; x++) {
                if (f.apply(x).isDefined()) {
                    some = true;
                } else {
                    none = true;
                }
            }
            if (some && none) {
                varying++;
            }
        }
        // a function is constant when all 17 draws are Some (3 in 4 each): 0.75^17, under 1%
        assertThat(varying).isGreaterThan(970);
    }
}
