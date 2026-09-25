package com.guizmaii.zazr.test.laws;

import com.guizmaii.zazr.collection.Vector;
import com.guizmaii.zazr.test.Arbitrary;
import com.guizmaii.zazr.test.CheckResult;
import java.util.Random;
import java.util.function.Function;
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
}
