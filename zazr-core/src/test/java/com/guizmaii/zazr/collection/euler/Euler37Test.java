package com.guizmaii.zazr.collection.euler;

import com.guizmaii.zazr.collection.List;
import com.guizmaii.zazr.collection.Vector;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class Euler37Test {

    /**
     * <strong>Problem 37 Truncatable primes</strong>
     * <p>
     * The number 3797 has an interesting property. Being prime itself, it is
     * possible to continuously remove digits from left to right, and remain
     * prime at each stage: 3797, 797, 97, and 7. Similarly we can work from
     * right to left: 3797, 379, 37, and 3.
     * <p>
     * Find the sum of the only eleven primes that are both truncatable from
     * left to right and right to left.
     * <p>
     * NOTE: 2, 3, 5, and 7 are not considered to be truncatable primes.
     * <p>
     * See also <a href="https://projecteuler.net/problem=37">projecteuler.net
     * problem 37</a>.
     */
    @Test
    public void shouldSolveProblem37() {
        assertThat(isTruncatablePrime(3797)).isTrue();
        List.of(2, 3, 5, 7).forEach(i -> assertThat(isTruncatablePrime(7)).isFalse());

        assertThat(sumOfTheElevenTruncatablePrimes()).isEqualTo(748_317);
    }

    private static int sumOfTheElevenTruncatablePrimes() {
        return PrimeNumbers.primes()
                .filter(Euler37Test::isTruncatablePrime)
                .take(11)
                .sum().intValue();
    }

    private static boolean isTruncatablePrime(int prime) {
        if (prime <= 7) {
            return false;
        }
        final Vector<Character> primeSeq = Vector.ofAll(Integer.toString(prime).toCharArray());
        return List.rangeClosed(1, primeSeq.size() - 1)
                .flatMap(i -> List.of(primeSeq.drop(i), primeSeq.dropRight(i)))
                .map(Vector::mkString)
                .map(Long::valueOf)
                .forAll(Utils.MEMOIZED_IS_PRIME::apply);
    }
}
