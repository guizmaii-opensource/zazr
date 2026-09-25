package com.guizmaii.zazr.test.laws;

import com.guizmaii.zazr.Tuple2;
import com.guizmaii.zazr.collection.LinkedHashSet;
import com.guizmaii.zazr.collection.NonEmptyVector;
import com.guizmaii.zazr.collection.Vector;
import com.guizmaii.zazr.control.Option;
import com.guizmaii.zazr.test.Arbitrary;
import com.guizmaii.zazr.test.CheckResult;
import java.util.ArrayList;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collector;
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
        final Law<String> throwing = Law.of("throwing", subject -> com.guizmaii.zazr.test.Property.named("throwing")
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
                new EqualitySubject<>(Arbitrary.integer().map(IdentityHashed::new), h -> new IdentityHashed(h.value())));
        assertFalsified(EqualityLaws.<Integer>equalsHashCodeConsistency(),
                new EqualitySubject<>(Arbitrary.integer(), i -> i + 1));
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
}
