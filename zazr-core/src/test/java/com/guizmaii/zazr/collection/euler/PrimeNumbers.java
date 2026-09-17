package com.guizmaii.zazr.collection.euler;

import com.guizmaii.zazr.Tuple;
import com.guizmaii.zazr.collection.HashMap;
import com.guizmaii.zazr.collection.Set;
import com.guizmaii.zazr.collection.Stream;
import com.guizmaii.zazr.collection.TreeSet;

final class PrimeNumbers {

    private static final Set<Integer> PRIMES_2_000_000 = Sieve.fillSieve(2_000_000, TreeSet.empty());

    private PrimeNumbers() {
    }

    static Stream<Integer> primes() {
        return Stream.ofAll(PRIMES_2_000_000);
    }

    static HashMap<Long, Long> factorization(long num) {
        if (num == 1) {
            return HashMap.empty();
        } else {
            return primeFactors(num)
                    .map(p -> HashMap.of(Tuple.of(p, 1L))
                            .merge(factorization(num / p), (a, b) -> a + b))
                    .headOption()
                    .getOrElse(HashMap::empty);
        }
    }

    static Stream<Long> primeFactors(long num) {
        return Stream.rangeClosed(2L, (int) Math.sqrt(num))
                .find(d -> num % d == 0)
                .map(d -> Stream.cons(d, () -> primeFactors(num / d)))
                .getOrElse(() -> Stream.of(num));
    }
}
