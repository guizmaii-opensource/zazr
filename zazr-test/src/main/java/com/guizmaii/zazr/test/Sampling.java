package com.guizmaii.zazr.test;

import java.util.SplittableRandom;

/**
 * The state shared by the generators of one run: the seeded source of randomness, the number of random values
 * drawn so far, and the discard budget.
 * <p>
 * The draw count tells a pass that used randomness from one that did not: running a pass again only gives other
 * values when it drew random values, so {@link Gen#filter} retries only those.
 */
final class Sampling {

    final SplittableRandom random;
    final int maxDiscards;
    int draws;

    /// The filters that gave up a pass, each after rejecting more values in a row than the discard budget, since the
    /// run last delivered a value.
    int filtersGaveUp;

    /// The number of values the last filter to give up rejected in a row.
    long lastRejected;

    Sampling(long seed, int maxDiscards) {
        this.random = new SplittableRandom(seed);
        this.maxDiscards = maxDiscards;
    }

    /// Whether `count` discards in a row exceed the budget. The count is a `long`, so a budget of
    /// `Integer.MAX_VALUE` is exceeded like any other instead of wrapping around.
    static boolean exceeds(long count, int maxDiscards) {
        return count > maxDiscards;
    }

    /// The error of a run whose filters kept rejecting everything: named after the filter when one gave up.
    IllegalStateException noValue(long passes) {
        if (filtersGaveUp > 0) {
            return new IllegalStateException("Gen.filter rejected every value it tried: " + lastRejected
                    + " values in a row, more than the discard budget of " + maxDiscards
                    + (passes > 1 ? ", in " + passes + " passes in a row at growing sizes" : "")
                    + "; generate the wanted values with map or flatMap instead of filtering them");
        }
        return new IllegalStateException("the generator produced no value in " + passes
                + " passes in a row at growing sizes, more than the discard budget of " + maxDiscards);
    }

    SplittableRandom draw() {
        draws++;
        return random;
    }
}
