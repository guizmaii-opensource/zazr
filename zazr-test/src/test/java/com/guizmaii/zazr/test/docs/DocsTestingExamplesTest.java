package com.guizmaii.zazr.test.docs;

import com.guizmaii.zazr.Tuple;
import com.guizmaii.zazr.Tuple2;
import com.guizmaii.zazr.collection.Vector;
import com.guizmaii.zazr.test.Arbitrary;
import com.guizmaii.zazr.test.CheckResult;
import com.guizmaii.zazr.test.Checkable;
import com.guizmaii.zazr.test.Gen;
import com.guizmaii.zazr.test.Property;
import com.guizmaii.zazr.test.laws.EqualityLaws;
import com.guizmaii.zazr.test.laws.EqualitySubject;
import java.util.Random;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
    void laws() {
        EqualityLaws.<Vector<Integer>>all().assertSatisfied(
            new EqualitySubject<>(Arbitrary.vector(Arbitrary.integer()), v -> Vector.ofAll(v.toList())),
            new Random(42));
    }
}
