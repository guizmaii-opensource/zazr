package com.guizmaii.zazr.test;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.UnaryOperator;

/**
 * How a check runs: the number of samples, the largest size, the seed of the random source, and the discard budget.
 * <p>
 * {@link #defaults()} gives 200 samples, a size of 100, a fresh random seed and a budget of 1,000 discards. The
 * system properties {@value #SAMPLES_PROPERTY}, {@value #SIZE_PROPERTY}, {@value #SEED_PROPERTY} and
 * {@value #MAX_DISCARDS_PROPERTY} replace these defaults, so a failing check replays with
 * {@code -Dzazr.check.seed=<the reported seed>}. A value set in the code with {@link #withSamples(int)},
 * {@link #withSize(int)}, {@link #withSeed(long)} or {@link #withMaxDiscards(int)} replaces the property.
 *
 * @param samples     the number of samples {@link Check#check} runs, at least 0
 * @param size        the largest size, at least 0: the size grows from 0 to this value over a check, and
 *                    {@link Check#checkAll} runs at this size
 * @param seed        the seed of the random source; the same seed gives the same values
 * @param maxDiscards the number of values {@link Gen#filter} may reject in a row, and of passes without a value a
 *                    check may run in a row, before it gives up; at least 0
 */
public record CheckConfig(int samples, int size, long seed, int maxDiscards) {

    /// The default number of samples.
    public static final int DEFAULT_SAMPLES = 200;

    /// The default largest size.
    public static final int DEFAULT_SIZE = 100;

    /// The default discard budget.
    public static final int DEFAULT_MAX_DISCARDS = 1000;

    /// The system property that sets the default number of samples.
    public static final String SAMPLES_PROPERTY = "zazr.check.samples";

    /// The system property that sets the default largest size.
    public static final String SIZE_PROPERTY = "zazr.check.size";

    /// The system property that sets the seed, to replay a check.
    public static final String SEED_PROPERTY = "zazr.check.seed";

    /// The system property that sets the default discard budget.
    public static final String MAX_DISCARDS_PROPERTY = "zazr.check.maxDiscards";

    /**
     * Creates a configuration.
     *
     * @param samples     the number of samples
     * @param size        the largest size
     * @param seed        the seed of the random source
     * @param maxDiscards the discard budget
     * @throws IllegalArgumentException if {@code samples}, {@code size} or {@code maxDiscards} is negative
     */
    public CheckConfig {
        Gen.requireNonNegative(samples, "samples");
        Gen.requireNonNegative(size, "size");
        Gen.requireNonNegative(maxDiscards, "maxDiscards");
    }

    /**
     * The default configuration, read from the system properties each time it is called: 200 samples, a size of
     * 100, a fresh random seed and a budget of 1,000 discards unless a property says otherwise.
     *
     * @return a configuration
     * @throws IllegalArgumentException if a property is not a number, or a negative one
     */
    public static CheckConfig defaults() {
        return fromProperties(System::getProperty);
    }

    static CheckConfig fromProperties(UnaryOperator<String> properties) {
        final String seed = properties.apply(SEED_PROPERTY);
        return new CheckConfig(
                intProperty(properties, SAMPLES_PROPERTY, DEFAULT_SAMPLES),
                intProperty(properties, SIZE_PROPERTY, DEFAULT_SIZE),
                seed == null ? ThreadLocalRandom.current().nextLong() : parse(SEED_PROPERTY, seed),
                intProperty(properties, MAX_DISCARDS_PROPERTY, DEFAULT_MAX_DISCARDS));
    }

    private static int intProperty(UnaryOperator<String> properties, String name, int fallback) {
        final String value = properties.apply(name);
        if (value == null) {
            return fallback;
        }
        final long parsed = parse(name, value);
        if (parsed < 0 || parsed > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(name + " is not an int >= 0: " + value);
        }
        return (int) parsed;
    }

    private static long parse(String name, String value) {
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(name + " is not a number: " + value, e);
        }
    }

    /**
     * This configuration with another number of samples.
     *
     * @param samples the number of samples
     * @return a new configuration
     * @throws IllegalArgumentException if {@code samples} is negative
     */
    public CheckConfig withSamples(int samples) {
        return new CheckConfig(samples, size, seed, maxDiscards);
    }

    /**
     * This configuration with another largest size.
     *
     * @param size the largest size
     * @return a new configuration
     * @throws IllegalArgumentException if {@code size} is negative
     */
    public CheckConfig withSize(int size) {
        return new CheckConfig(samples, size, seed, maxDiscards);
    }

    /**
     * This configuration with another seed.
     *
     * @param seed the seed
     * @return a new configuration
     */
    public CheckConfig withSeed(long seed) {
        return new CheckConfig(samples, size, seed, maxDiscards);
    }

    /**
     * This configuration with another discard budget.
     *
     * @param maxDiscards the discard budget
     * @return a new configuration
     * @throws IllegalArgumentException if {@code maxDiscards} is negative
     */
    public CheckConfig withMaxDiscards(int maxDiscards) {
        return new CheckConfig(samples, size, seed, maxDiscards);
    }
}
