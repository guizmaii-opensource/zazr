package com.guizmaii.zazr.test.laws;

import com.guizmaii.zazr.Tuple2;
import com.guizmaii.zazr.collection.LinkedHashSet;
import com.guizmaii.zazr.collection.NonEmptyVector;
import com.guizmaii.zazr.collection.TreeSet;
import com.guizmaii.zazr.collection.Vector;
import com.guizmaii.zazr.control.Option;
import com.guizmaii.zazr.test.legacy.Arbitrary;
import com.guizmaii.zazr.test.legacy.CheckResult;
import com.guizmaii.zazr.test.legacy.Gen;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The law framework: a law reports its name and counterexample, law sets compose in order and report every failing
 * law, and each law family catches a broken implementation.
 */
class LawsTest {

    /// A vector whose `map` drops the last element: it breaks `mapIdentity`.
    private static final MapSubject<Vector<?>> BROKEN_MAP = new MapSubject<>() {

        @Override
        public Arbitrary<Vector<?>> values() {
            return Arbitrary.vector(Arbitrary.integer()).map(v -> v);
        }

        @Override
        public Vector<?> map(Vector<?> fa, Function<Object, Object> f) {
            return fa.isEmpty() ? fa.map(f) : fa.map(f).dropRight(1);
        }
    };

    /// A vector whose `zip` pairs in reverse order: it breaks `zipAssociativity`.
    private static final ZipSubject<Vector<?>> BROKEN_ZIP = new ZipSubject<>() {

        @Override
        public Arbitrary<Vector<?>> values() {
            return Arbitrary.vector(Arbitrary.integer()).map(v -> v);
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

    private static final Random RANDOM = new Random(1);

    @Test
    void failingLawReportsItsNameAndCounterexample() {
        final CheckResult result = MapLaws.<Vector<?>>mapIdentity().check(BROKEN_MAP, RANDOM, 10, 100);
        assertThat(result.isFalsified()).isTrue();
        assertThat(result.propertyName()).isEqualTo("mapIdentity");
        assertThat(result.sample().isDefined()).isTrue();
        assertThat(result.message().get()).startsWith("left = Vector(");
    }

    @Test
    void assertSatisfiedListsEveryFailingLaw() {
        final Laws<MapSubject<Vector<?>>> laws = MapLaws.all();
        assertThatThrownBy(() -> laws.assertSatisfied(BROKEN_MAP, RANDOM, 10, 100))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("2 law(s) failed")
                .hasMessageContaining("mapIdentity: falsified at check")
                .hasMessageContaining("mapComposition: falsified at check")
                .hasMessageContaining("by (Vector(");
    }

    @Test
    void zipLawsCatchABrokenZip() {
        assertThat(ZipLaws.<Vector<?>>zipAssociativity().check(BROKEN_ZIP, RANDOM, 10, 100).isFalsified()).isTrue();
    }

    @Test
    void lawSetsComposeInOrder() {
        final Laws<ZipSubject<Vector<?>>> laws = MapLaws.<Vector<?>>all().and(ZipLaws.<Vector<?>>zip());
        assertThat(laws.laws().map(Law::name)).containsExactly("mapIdentity", "mapComposition", "zipAssociativity");
        assertThat(laws.check(BROKEN_ZIP, RANDOM, 10, 100).map(CheckResult::isSatisfied)).containsExactly(true, true, false);
        assertThat(MapLaws.<Vector<?>>mapIdentity().and(MapLaws.mapComposition()).laws().map(Law::name))
                .containsExactly("mapIdentity", "mapComposition");
    }

    @Test
    void satisfiedLawsDoNotThrow() {
        MapLaws.<Vector<?>>all().assertSatisfied(new VectorLawsTest().subject(), RANDOM, 10, 100);
    }

    @Test
    void erroneousLawIsReported() {
        final Law<String> throwing = Law.of("throwing", subject -> com.guizmaii.zazr.test.legacy.Property.named("throwing")
                .forAll(Arbitrary.integer())
                .suchThat(i -> {
                    throw new IllegalStateException("boom");
                }));
        assertThatThrownBy(() -> Laws.<String>of(throwing).assertSatisfied("subject", RANDOM, 10, 10))
                .hasMessageContaining("throwing: erroneous at check 1")
                .hasMessageContaining("boom");
    }

    @Test
    void lawNamesAreRequired() {
        assertThatThrownBy(() -> Law.of(" ", subject -> null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Law.of(null, subject -> null)).isInstanceOf(NullPointerException.class);
        assertThat(MapLaws.mapIdentity().toString()).isEqualTo("Law(mapIdentity)");
        assertThat(MapLaws.all().toString()).isEqualTo("Laws(mapIdentity, mapComposition)");
    }

    // -- every law family catches a broken implementation

    private static <S> void assertFalsified(Law<? super S> law, S subject) {
        final CheckResult result = law.check(subject, new Random(3), 20, 500);
        assertThat(result.isFalsified()).as(law.name() + " catches the defect").isTrue();
        assertThat(result.propertyName()).isEqualTo(law.name());
    }

    /// A vector whose `flatMap` drops the last element of its result.
    private static final FlatMapSubject<Vector<?>> BROKEN_FLAT_MAP = new FlatMapSubject<>() {

        @Override
        public Arbitrary<Vector<?>> values() {
            return Arbitrary.vector(Arbitrary.integer()).map(v -> v);
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
        public Arbitrary<Option<?>> values() {
            return Arbitrary.option(Arbitrary.integer()).map(o -> o);
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
                new EqualitySubject<>(Arbitrary.integer().map(IdentityHashed::new), h -> new IdentityHashed(h.value()), IdentityHashed::value));
        assertFalsified(EqualityLaws.<Integer>equalsHashCodeConsistency(),
                new EqualitySubject<>(Arbitrary.integer(), i -> i + 1, i -> i));
    }

    @Test
    void collectionLawsCatchBrokenContracts() {
        final Arbitrary<Vector<Integer>> vectors = Arbitrary.vector(Arbitrary.integer());
        assertFalsified(CollectionLaws.<Integer, Vector<Integer>>sizeEqualsIterationCount(),
                new CollectionSubject<>(vectors, Vector::ofAll, v -> v.size() + 1, Vector::toList, true));
        assertFalsified(CollectionLaws.<Integer, Vector<Integer>>toListRoundTrip(),
                new CollectionSubject<>(vectors, xs -> Vector.ofAll(xs).reverse(), Vector::size, Vector::toList, true));
        // a vector's equals depends on the order, a linked hash set's does not: declaring the opposite is caught
        assertFalsified(CollectionLaws.<Integer, Vector<Integer>>equalsAgreesWithElements(),
                new CollectionSubject<>(vectors, Vector::ofAll, Vector::size, Vector::toList, false));
        assertFalsified(CollectionLaws.<Integer, LinkedHashSet<Integer>>equalsAgreesWithElements(),
                new CollectionSubject<>(Arbitrary.linkedHashSet(Arbitrary.integer()), LinkedHashSet::ofAll, LinkedHashSet::size,
                        LinkedHashSet::toList, true));
        assertFalsified(CollectionLaws.<Integer, NonEmptyVector<Integer>>sequenceEqualsAcrossTypes(),
                new CollectionSubject<>(Arbitrary.nonEmptyVector(Arbitrary.integer()), xs -> NonEmptyVector.fromVector(Vector.ofAll(xs)).get(),
                        NonEmptyVector::size, NonEmptyVector::toList, true));
        assertFalsified(CollectionLaws.<Integer, Vector<Integer>>setEqualsAcrossTypes(),
                new CollectionSubject<>(vectors, Vector::ofAll, Vector::size, Vector::toList, false));
        assertFalsified(CollectionLaws.<Tuple2<Integer, Integer>, Vector<Tuple2<Integer, Integer>>>mapEqualsAcrossTypes(),
                new CollectionSubject<>(Arbitrary.vector(Arbitrary.tuple2(Arbitrary.integer(), Arbitrary.integer())), Vector::ofAll,
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
                new BuilderLaws.CollectorSubject<>(Arbitrary.list(Arbitrary.integer()), reversing, Vector::ofAll));
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
        final Arbitrary<FirstOnly> firstOnly = Arbitrary.tuple2(Arbitrary.integer(), Arbitrary.integer()).map(t -> new FirstOnly(t._1(), t._2()));
        final EqualitySubject<FirstOnly> firstOnlySubject = new EqualitySubject<>(firstOnly, x -> new FirstOnly(x.a(), x.b()),
                x -> java.util.List.of(x.a(), x.b()));
        final Arbitrary<AllEqual> allEqual = Arbitrary.tuple2(Arbitrary.integer(), Arbitrary.integer()).map(t -> new AllEqual(t._1(), t._2()));
        final EqualitySubject<AllEqual> allEqualSubject = new EqualitySubject<>(allEqual, x -> new AllEqual(x.a(), x.b()),
                x -> java.util.List.of(x.a(), x.b()));
        // both pass the hash-code law: nothing there requires different values to be unequal
        assertThat(EqualityLaws.<FirstOnly>equalsHashCodeConsistency().check(firstOnlySubject, new Random(3), 100, 1000).isSatisfied()).isTrue();
        assertThat(EqualityLaws.<AllEqual>equalsHashCodeConsistency().check(allEqualSubject, new Random(3), 100, 1000).isSatisfied()).isTrue();
        assertFalsified(EqualityLaws.<FirstOnly>equalsAgreesWithModel(), firstOnlySubject);
        assertFalsified(EqualityLaws.<AllEqual>equalsAgreesWithModel(), allEqualSubject);
    }

    @Test
    void iterationOrderLawCatchesALostOrder() {
        final Arbitrary<LinkedHashSet<Integer>> linked = Arbitrary.linkedHashSet(Arbitrary.integer());
        final CollectionSubject<Integer, LinkedHashSet<Integer>> reversedLinked = new CollectionSubject<>(linked,
                xs -> LinkedHashSet.ofAll(Vector.ofAll(xs).reverse()), LinkedHashSet::size, LinkedHashSet::toList, false,
                Option.some(IterationOrder.firstOccurrence()));
        final Arbitrary<TreeSet<Integer>> sorted = Arbitrary.treeSet(Arbitrary.integer());
        final CollectionSubject<Integer, TreeSet<Integer>> unsortedTree = new CollectionSubject<>(sorted,
                xs -> TreeSet.ofAll(Comparator.<Integer>reverseOrder(), xs), TreeSet::size, TreeSet::toList, false,
                Option.some(IterationOrder.sorted(Comparator.<Integer>naturalOrder())));
        // every other collection law is satisfied by both: they compare sets as sets
        assertThat(CollectionLaws.<Integer, LinkedHashSet<Integer>>set().check(reversedLinked, new Random(3), 100, 200)
                .map(CheckResult::isSatisfied)).containsExactly(true, true, true, false, true);
        assertThat(CollectionLaws.<Integer, TreeSet<Integer>>set().check(unsortedTree, new Random(3), 100, 200)
                .map(CheckResult::isSatisfied)).containsExactly(true, true, true, false, true);
        assertFalsified(CollectionLaws.<Integer, LinkedHashSet<Integer>>iterationOrder(), reversedLinked);
        assertFalsified(CollectionLaws.<Integer, TreeSet<Integer>>iterationOrder(), unsortedTree);

        final Collector<Integer, ?, LinkedHashSet<Integer>> reversing = Collectors.collectingAndThen(Collectors.toList(),
                list -> LinkedHashSet.ofAll(Vector.ofAll(list).reverse()));
        assertFalsified(BuilderLaws.<Integer, LinkedHashSet<Integer>>collectorResultEqualsOfAll(), new BuilderLaws.CollectorSubject<>(
                Arbitrary.list(Arbitrary.integer()), reversing, LinkedHashSet::ofAll, Option.some(IterationOrder.firstOccurrence())));
    }

    @Test
    void generatedFunctionsDependOnTheirArgument() {
        final Gen<Function<Object, Option<?>>> functions = Functions.to(Arbitrary.option(Arbitrary.integer()).map(o -> (Option<?>) o), 8);
        final Random random = new Random(11);
        int varying = 0;
        for (int i = 0; i < 1000; i++) {
            final Function<Object, Option<?>> f = functions.apply(random);
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
