package com.guizmaii.zazr.test.laws;

import com.guizmaii.zazr.collection.Vector;
import com.guizmaii.zazr.test.CheckConfig;

/**
 * Runs the laws with a fixed seed per law, derived from its name, and 1,000 samples, so that a failure in CI replays
 * locally. {@code -Dzazr.check.seed=...} replaces the seed with the one a failure reports, and
 * {@code -Dzazr.check.samples=...} and {@code -Dzazr.check.size=...} the number of samples and the size.
 */
final class LawChecks {

    /// The seed every law's seed is derived from.
    static final long SEED = 20260925L;

    /// The number of samples of each law, unless {@value CheckConfig#SAMPLES_PROPERTY} is set.
    static final int SAMPLES = 1000;

    private LawChecks() {
    }

    /// The configuration of a check named `name`: the seed of {@value CheckConfig#SEED_PROPERTY} when it is set,
    /// otherwise one derived from the name; {@link #SAMPLES} samples unless {@value CheckConfig#SAMPLES_PROPERTY} is
    /// set; the rest from {@link CheckConfig#defaults()}.
    static CheckConfig config(String name) {
        final CheckConfig defaults = CheckConfig.defaults();
        final CheckConfig seeded = System.getProperty(CheckConfig.SEED_PROPERTY) == null
                ? defaults.withSeed(seed(name))
                : defaults;
        return System.getProperty(CheckConfig.SAMPLES_PROPERTY) == null ? seeded.withSamples(SAMPLES) : seeded;
    }

    /// The fixed seed of a check named `name`.
    static long seed(String name) {
        return Functions.mix(SEED + name.hashCode());
    }

    /// Checks one law with its own configuration.
    static <S> void check(Law<? super S> law, S subject) {
        Laws.<S>of(law).assertSatisfied(subject, config(law.name()));
    }

    /// Checks every law of a set, each with its own configuration, and reports every failing law.
    static <S> void check(Laws<S> laws, S subject) {
        final Vector<LawResult> failures = laws.laws()
                .map(law -> new LawResult(law.name(), law.check(subject, config(law.name()))))
                .filter(result -> !result.isSatisfied());
        if (!failures.isEmpty()) {
            throw new AssertionError(failures.map(LawResult::describe).mkString(failures.size() + " law(s) failed:\n", "\n", ""));
        }
    }
}
