package com.guizmaii.zazr.test.docs;

import com.guizmaii.zazr.Tuple;
import com.guizmaii.zazr.test.Arbitrary;
import com.guizmaii.zazr.test.Gen;
import com.guizmaii.zazr.test.Property;
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
        var result = Property.def("reversing twice gives the list back")
            .forAll(Arbitrary.list(Arbitrary.integer()))
            .suchThat(list -> list.reverse().reverse().equals(list))
            .check(); // CheckResult
        result.assertIsSatisfied();

        var broken = Property.def("every list is short")
            .forAll(Arbitrary.list(Arbitrary.integer()))
            .suchThat(list -> list.length() < 5)
            .check(100, 1_000); // CheckResult
        var falsified = broken.isFalsified();
        // true, and broken.sample() holds the first list of 5 elements or more

        assertThat(result.count()).isEqualTo(1_000);
        assertThat(falsified).isTrue();
        assertThat(broken.sample().isDefined()).isTrue();
    }

    @Test
    void generators() {
        var dice = Gen.choose(1, 6); // Gen<Integer>
        var pairs = dice.flatMap(a -> dice.map(b -> Tuple.of(a, b)))
            .arbitrary(); // Arbitrary<Tuple2<Integer, Integer>>
        var sums = Property.def("two dice sum to 2..12")
            .forAll(pairs)
            .suchThat(p -> p._1() + p._2() >= 2 && p._1() + p._2() <= 12)
            .check(); // CheckResult
        sums.assertIsSatisfied();

        assertThat(sums.isSatisfied()).isTrue();
    }

    @Test
    void preconditions() {
        var halving = Property.def("an even number is twice its half")
            .forAll(Arbitrary.integer())
            .suchThat(n -> n % 2 == 0)
            .implies(n -> (n / 2) * 2 == n); // Checkable
        halving.check().assertIsSatisfied();

        assertThat(halving.check().isSatisfied()).isTrue();
    }
}
