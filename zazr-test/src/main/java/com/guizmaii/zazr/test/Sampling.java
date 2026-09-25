package com.guizmaii.zazr.test;

import java.util.SplittableRandom;

/**
 * The state shared by the generators of one run: the seeded source of randomness, the number of random values
 * drawn so far, the largest size, and the discard budget.
 * <p>
 * The draw count tells a pass that used randomness from one that did not: running a pass again only gives other
 * values when it drew random values, so {@link Gen#filter} retries only those.
 * <p>
 * The discard budget is one counter for the whole run, shared by every filter at every nesting level, every empty
 * pass and every failed draw of an element: a filter's rejected values in a random pass, a pass that gave no value
 * and a draw that found none all spend it. It starts again when the run delivers a sample, so a run makes at most
 * {@code maxDiscards + 1} discards per sample however the generators nest, and ends with an error beyond.
 */
final class Sampling {

    final SplittableRandom random;
    final int maxDiscards;
    final int maxSize;
    int draws;

    /// The discards since the run last delivered a sample.
    long discards;

    /// Whether the last discard was a value rejected by a filter.
    boolean lastDiscardByFilter;

    /// The filters that gave a pass up since the run last delivered a sample, and the values the last one rejected.
    int filtersGaveUp;
    long lastRejected;

    Sampling(long seed, int maxDiscards) {
        this(seed, maxDiscards, Integer.MAX_VALUE);
    }

    Sampling(long seed, int maxDiscards, int maxSize) {
        this.random = new SplittableRandom(seed);
        this.maxDiscards = maxDiscards;
        this.maxSize = maxSize;
    }

    /// Whether `count` discards exceed the budget. The count is a `long`, so a budget of `Integer.MAX_VALUE` is
    /// exceeded like any other instead of wrapping around.
    static boolean exceeds(long count, int maxDiscards) {
        return count > maxDiscards;
    }

    /// The number of values a filter rejects in a row, in the passes it runs again, before it gives its pass up so
    /// that the run can try a larger size: a sixteenth of the budget, at least one. Sizes double from one attempt to
    /// the next, so the run goes from 0 to a size of 100 in 8 attempts, well within the budget.
    long filterGiveUp() {
        return Math.max(1, maxDiscards / 16);
    }

    /// The size of the attempt that follows `failed` attempts without a value from `size`: `size + 2^failed - 1`, up
    /// to the largest size (or `size` when it is larger).
    int grow(int size, long failed) {
        final long cap = Math.max(size, maxSize);
        final long bump = failed >= 62 ? Long.MAX_VALUE : (1L << failed) - 1;
        return (int) (bump >= cap - size ? cap : size + bump);
    }

    /// Spends `count` discards of the budget.
    ///
    /// @throws IllegalStateException when the budget is exceeded
    void discard(long count, boolean byFilter) {
        discards += count;
        lastDiscardByFilter = byFilter;
        if (exceeds(discards, maxDiscards)) {
            throw exhausted();
        }
    }

    /// The run delivered a sample: the budget starts again.
    void delivered() {
        discards = 0;
        filtersGaveUp = 0;
    }

    private IllegalStateException exhausted() {
        if (lastDiscardByFilter) {
            return new IllegalStateException("Gen.filter rejected too many values: " + discards
                    + " discards since the last sample, more than the discard budget of " + maxDiscards
                    + "; generate the wanted values with map or flatMap instead of filtering them");
        }
        return new IllegalStateException("the generator produced no value: " + discards
                + " discards since the last sample, more than the discard budget of " + maxDiscards);
    }

    /// The error of a single pass in which a filter gave its pass up, so a value is missing.
    IllegalStateException filterGaveUp() {
        return new IllegalStateException("Gen.filter gave a pass up after rejecting " + lastRejected
                + " values in a row; generate the wanted values with map or flatMap instead of filtering them");
    }

    SplittableRandom draw() {
        draws++;
        return random;
    }
}
