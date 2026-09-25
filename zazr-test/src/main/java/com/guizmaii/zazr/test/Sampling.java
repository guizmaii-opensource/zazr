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

    Sampling(long seed, int maxDiscards) {
        this.random = new SplittableRandom(seed);
        this.maxDiscards = maxDiscards;
    }

    SplittableRandom draw() {
        draws++;
        return random;
    }
}
