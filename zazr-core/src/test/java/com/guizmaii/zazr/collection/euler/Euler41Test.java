package com.guizmaii.zazr.collection.euler;

import com.guizmaii.zazr.collection.List;
import com.guizmaii.zazr.collection.Stream;
import org.junit.jupiter.api.Test;

import static com.guizmaii.zazr.collection.euler.Utils.isPrime;
import static java.util.Comparator.reverseOrder;
import static org.assertj.core.api.Assertions.assertThat;

public class Euler41Test {

    /**
     * <strong>Problem 41 Pandigital prime</strong>
     * <p>
     * We shall say that an <i>n</i>-digit number is pandigital if it makes use
     * of all the digits 1 to <i>n</i> exactly once. For example, 2143 is a
     * 4-digit pandigital and is also prime.
     *
     * What is the largest <i>n</i>-digit pandigital prime that exists?
     * <p>
     * See also <a href="https://projecteuler.net/problem=41">projecteuler.net
     * problem 41</a>.
     */
    @Test
    public void shouldSolveProblem41() {
        assertThat(nDigitPandigitalNumbers(4)).contains(2143);
        assertThat(isPrime(2143)).isTrue();

        assertThat(largestNPandigitalPrime()).isEqualTo(7652413);
    }

    private static int largestNPandigitalPrime() {
        return Stream.rangeClosedBy(9, 1, -1)
                .flatMap(n -> nDigitPandigitalNumbers(n)
                        .filter(Utils::isPrime)
                        .sorted(reverseOrder()))
                .head();
    }

    private static List<Integer> nDigitPandigitalNumbers(int n) {
        return List.rangeClosed(1, n)
                .permutations()
                .map(List::mkString)
                .map(Integer::valueOf);
    }
}
