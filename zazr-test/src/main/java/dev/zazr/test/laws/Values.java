package dev.zazr.test.laws;

import dev.zazr.test.Gen;

/**
 * The generators of the values the laws draw themselves.
 */
final class Values {

    private Values() {
    }

    /// Integers between minus the size and the size: a small range, so that equal values are frequent.
    static Gen<Integer> integers() {
        return Gen.sized(n -> Gen.integers(-n, n));
    }
}
