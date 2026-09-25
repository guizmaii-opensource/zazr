package com.guizmaii.zazr.test.docs;

import com.guizmaii.zazr.Tuple;
import com.guizmaii.zazr.Tuple2;
import com.guizmaii.zazr.collection.Vector;
import com.guizmaii.zazr.control.Validation;
import com.guizmaii.zazr.test.Arbitrary;
import com.guizmaii.zazr.test.CheckResult;
import com.guizmaii.zazr.test.Checkable;
import com.guizmaii.zazr.test.Gen;
import com.guizmaii.zazr.test.Property;
import com.guizmaii.zazr.test.laws.MapLaws;
import com.guizmaii.zazr.test.laws.MapSubject;
import java.util.Random;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The fenced {@code java} blocks of docs/testing.md, pasted verbatim, compiled and run (zazr-core's
 * {@code DocsExamplesTest} cannot see zazr-test, which depends on zazr-core). {@code make docs-examples} fails when a
 * block of that page is missing from this file.
 */
public class DocsTestingExamplesTest {

    @Test
    void aProperty() {
        CheckResult result = Property.named("reversing twice gives the list back")
            .forAll(Arbitrary.list(Arbitrary.integer()))
            .suchThat(list -> list.reverse().reverse().equals(list))
            .check();
        result.assertIsSatisfied();

        CheckResult broken = Property.named("every list is short")
            .forAll(Arbitrary.list(Arbitrary.integer()))
            .suchThat(list -> list.length() < 5)
            .check(100, 1_000);
        boolean falsified = broken.isFalsified();
        // true, and broken.sample() holds the first list of 5 elements or more

        assertThat(result.count()).isEqualTo(1_000);
        assertThat(falsified).isTrue();
        assertThat(broken.sample().isDefined()).isTrue();
    }

    @Test
    void generators() {
        Gen<Integer> dice = Gen.choose(1, 6);
        Arbitrary<Tuple2<Integer, Integer>> pairs = dice.flatMap(a -> dice.map(b -> Tuple.of(a, b))).arbitrary();
        CheckResult sums = Property.named("two dice sum to 2..12")
            .forAll(pairs)
            .suchThat(p -> p._1() + p._2() >= 2 && p._1() + p._2() <= 12)
            .check();
        sums.assertIsSatisfied();

        assertThat(sums.isSatisfied()).isTrue();
    }

    @Test
    void preconditions() {
        Checkable halving = Property.named("an even number is twice its half")
            .forAll(Arbitrary.integer())
            .suchThat(n -> n % 2 == 0)
            .implies(n -> (n / 2) * 2 == n);
        halving.check().assertIsSatisfied();

        assertThat(halving.check().isSatisfied()).isTrue();
    }

    @Test
    void readingAResult() {
        CheckResult outcome = Property.named("doubling gives an even number")
            .forAll(Arbitrary.integer())
            .suchThat(n -> (n * 2) % 2 == 0)
            .check();
        String summary = switch (outcome) {
            case CheckResult.Satisfied satisfied -> "passed " + satisfied.count() + " samples";
            case CheckResult.Falsified falsified -> "broken by " + falsified.counterexample();
            case CheckResult.Erroneous erroneous -> "failed with " + erroneous.cause().getMessage();
        };
        // "passed 1000 samples"

        assertThat(summary).isEqualTo("passed 1000 samples");
    }

    @Test
    void arbitrariesForEveryType() {
        Arbitrary<Validation<String, Integer>> checks =
            Arbitrary.validation(Arbitrary.of("too short", "no digit"), Arbitrary.integer());
        Property.named("zip is valid only when both sides are")
            .forAll(checks, checks)
            .suchThat((a, b) -> a.zip(b).isValid() == (a.isValid() && b.isValid()))
            .check()
            .assertIsSatisfied();

        assertThat(checks.apply(10).apply(new Random(1))).isNotNull();
    }

    @Test
    void checkingYourOwnType() {
        record Box(Vector<Object> items) {
            Box map(Function<Object, Object> f) { return new Box(items.map(f)); }
        }
        MapSubject<Box> boxes = new MapSubject<>() {
            public Arbitrary<Box> values() { return Arbitrary.vector(Arbitrary.integer()).map(v -> new Box(v.map(x -> (Object) x))); }
            public Box map(Box box, Function<Object, Object> f) { return box.map(f); }
        };
        MapLaws.<Box>all().assertSatisfied(boxes, new Random(42));

        MapSubject<Box> broken = new MapSubject<>() {
            public Arbitrary<Box> values() { return boxes.values(); }
            public Box map(Box box, Function<Object, Object> f) { return new Box(box.map(f).items().dropRight(1)); }
        };
        assertThatThrownBy(() -> MapLaws.<Box>all().assertSatisfied(broken, new Random(42), 5, 100))
            .isInstanceOf(AssertionError.class)
            .hasMessageStartingWith("2 law(s) failed:\nmapIdentity: falsified at check ")
            .hasMessageContaining("\nmapComposition: falsified at check ")
            .hasMessageContaining("mapIdentity: falsified at check 3 by (Box[items=Vector(0, 1)]) (left = Box[items=Vector(0)], right = Box[items=Vector(0, 1)])");
    }
}
