package dev.zazr.collection.euler;

import dev.zazr.Tuple;
import dev.zazr.collection.HashMap;
import dev.zazr.collection.LazyList;
import dev.zazr.collection.Set;
import dev.zazr.collection.TreeSet;

final class PrimeNumbers {

    private static final Set<Integer> PRIMES_2_000_000 = Sieve.fillSieve(2_000_000, TreeSet.empty());

    private PrimeNumbers() {}

    static LazyList<Integer> primes() {
        return LazyList.ofAll(PRIMES_2_000_000);
    }

    static HashMap<Long, Long> factorization(long num) {
        if (num == 1) {
            return HashMap.empty();
        } else {
            return primeFactors(num)
                    .map(p -> HashMap.of(Tuple.of(p, 1L)).merge(factorization(num / p), (a, b) -> a + b))
                    .headOption()
                    .getOrElse(HashMap::empty);
        }
    }

    static LazyList<Long> primeFactors(long num) {
        return LazyList.rangeClosed(2L, (int) Math.sqrt(num))
                .find(d -> num % d == 0)
                .map(d -> LazyList.cons(d, () -> primeFactors(num / d)))
                .getOrElse(() -> LazyList.of(num));
    }
}
