package com.guizmaii.zazr.test.laws;

import java.util.Random;

/**
 * Runs one law with a fixed seed, so that a failure in CI replays locally. {@code -Dzazr.laws.seed=...} and
 * {@code -Dzazr.laws.tries=...} change the seed and the number of samples.
 */
final class LawChecks {

    static final long SEED = Long.getLong("zazr.laws.seed", 20260925L);
    static final int SIZE = Integer.getInteger("zazr.laws.size", 100);
    static final int TRIES = Integer.getInteger("zazr.laws.tries", 1000);

    private LawChecks() {
    }

    static <S> void check(Law<? super S> law, S subject) {
        Laws.<S>of(law).assertSatisfied(subject, new Random(SEED ^ law.name().hashCode()), SIZE, TRIES);
    }
}
