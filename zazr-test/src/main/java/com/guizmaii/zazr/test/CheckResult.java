package com.guizmaii.zazr.test;

import com.guizmaii.zazr.Tuple;
import com.guizmaii.zazr.control.Option;

import java.util.Objects;

/**
 * The result of a check, one of three records:
 *
 * <ul>
 * <li>{@link Satisfied}: every sample passed;</li>
 * <li>{@link Falsified}: a sample made the check return {@code false} or throw an {@link AssertionError};</li>
 * <li>{@link Erroneous}: a generator threw, or the check threw something other than an {@link AssertionError}.</li>
 * </ul>
 *
 * A failed result carries the seed of the run and the number of the failing sample: running the same check with
 * the same seed ({@link CheckConfig#withSeed(long)}, or the system property {@value CheckConfig#SEED_PROPERTY})
 * fails at the same sample again.
 */
public sealed interface CheckResult permits CheckResult.Satisfied, CheckResult.Falsified, CheckResult.Erroneous {

    /**
     * Whether every sample passed.
     *
     * @return true for a {@link Satisfied} result
     */
    default boolean isSatisfied() {
        return this instanceof Satisfied;
    }

    /**
     * Whether a sample falsified the check.
     *
     * @return true for a {@link Falsified} result
     */
    default boolean isFalsified() {
        return this instanceof Falsified;
    }

    /**
     * Whether a generator or the check threw.
     *
     * @return true for an {@link Erroneous} result
     */
    default boolean isErroneous() {
        return this instanceof Erroneous;
    }

    /**
     * The values of the sample that failed, one component per generator of the check.
     *
     * @return the counterexample of a falsified result, the sample of an erroneous one when a generator did not
     * throw, none otherwise
     */
    Option<Tuple> sample();

    /**
     * The error thrown by a generator or the check.
     *
     * @return the cause of an erroneous result, none otherwise
     */
    default Option<Throwable> error() {
        return Option.none();
    }

    /**
     * The message of the {@link AssertionError} that falsified the check.
     *
     * @return the message of a falsified result when the check threw one with a message, none otherwise
     */
    default Option<String> message() {
        return Option.none();
    }

    /**
     * Throws unless every sample passed.
     *
     * @throws AssertionError with the counterexample or the error, the sample number and the seed; the error of an
     *                        erroneous result is its cause
     */
    default void assertIsSatisfied() {
        switch (this) {
            case Satisfied ignored -> {
            }
            case Falsified falsified -> throw new AssertionError(falsified.describe());
            case Erroneous erroneous -> throw new AssertionError(erroneous.describe(), erroneous.cause());
        }
    }

    /**
     * Throws unless a sample falsified the check.
     *
     * @throws AssertionError if this result is not falsified
     */
    default void assertIsFalsified() {
        if (!isFalsified()) {
            throw new AssertionError("expected a falsified check, but it was " + this);
        }
    }

    /**
     * Throws unless a generator or the check threw.
     *
     * @throws AssertionError if this result is not erroneous
     */
    default void assertIsErroneous() {
        if (!isErroneous()) {
            throw new AssertionError("expected an erroneous check, but it was " + this);
        }
    }

    /**
     * Every sample passed.
     *
     * @param samples the number of samples checked
     */
    record Satisfied(int samples) implements CheckResult {

        /**
         * Creates a satisfied result.
         *
         * @param samples the number of samples checked
         * @throws IllegalArgumentException if {@code samples} is negative
         */
        public Satisfied {
            Gen.requireNonNegative(samples, "samples");
        }

        @Override
        public Option<Tuple> sample() {
            return Option.none();
        }
    }

    /**
     * A sample made the check return {@code false} or throw an {@link AssertionError}.
     *
     * @param sampleNumber   the number of the sample, from 1
     * @param seed           the seed of the run
     * @param counterexample the values of the sample
     * @param message        the message of the {@link AssertionError}, if the check threw one with a message
     */
    record Falsified(int sampleNumber, long seed, Tuple counterexample, Option<String> message) implements CheckResult {

        /**
         * Creates a falsified result.
         *
         * @param sampleNumber   the number of the sample, from 1
         * @param seed           the seed of the run
         * @param counterexample the values of the sample
         * @param message        the message of the {@link AssertionError}, if any
         * @throws NullPointerException     if {@code counterexample} or {@code message} is null
         * @throws IllegalArgumentException if {@code sampleNumber} is below 1
         */
        public Falsified {
            requirePositive(sampleNumber);
            Objects.requireNonNull(counterexample, "counterexample is null");
            Objects.requireNonNull(message, "message is null");
        }

        @Override
        public Option<Tuple> sample() {
            return Option.some(counterexample);
        }

        @Override
        public Option<String> message() {
            return message;
        }

        String describe() {
            return "falsified at sample " + sampleNumber + " by " + counterexample
                    + message.map(m -> ": " + m).getOrElse("") + replay(seed);
        }
    }

    /**
     * A generator threw, or the check threw something other than an {@link AssertionError}. Two erroneous results
     * are equal when their causes have the same class and message along the whole cause chain.
     *
     * @param sampleNumber the number of the sample being generated or checked, from 1
     * @param seed         the seed of the run
     * @param cause        what was thrown
     * @param sample       the values of the sample, when the check threw; none when a generator did
     */
    record Erroneous(int sampleNumber, long seed, Throwable cause, Option<Tuple> sample) implements CheckResult {

        /**
         * Creates an erroneous result.
         *
         * @param sampleNumber the number of the sample, from 1
         * @param seed         the seed of the run
         * @param cause        what was thrown
         * @param sample       the values of the sample, if any
         * @throws NullPointerException     if {@code cause} or {@code sample} is null
         * @throws IllegalArgumentException if {@code sampleNumber} is below 1
         */
        public Erroneous {
            requirePositive(sampleNumber);
            Objects.requireNonNull(cause, "cause is null");
            Objects.requireNonNull(sample, "sample is null");
        }

        @Override
        public Option<Throwable> error() {
            return Option.some(cause);
        }

        @Override
        public boolean equals(Object o) {
            return o == this || (o instanceof Erroneous that
                    && this.sampleNumber == that.sampleNumber
                    && this.seed == that.seed
                    && sameCauses(this.cause, that.cause)
                    && this.sample.equals(that.sample));
        }

        @Override
        public int hashCode() {
            return Objects.hash(sampleNumber, seed, causesHashCode(cause), sample);
        }

        String describe() {
            return "erroneous at sample " + sampleNumber
                    + sample.map(values -> " with " + values).getOrElse(", while generating it")
                    + ": " + cause + replay(seed);
        }

        private static boolean sameCauses(Throwable t1, Throwable t2) {
            while (t1 != null && t2 != null) {
                if (t1.getClass() != t2.getClass() || !Objects.equals(t1.getMessage(), t2.getMessage())) {
                    return false;
                }
                t1 = t1.getCause();
                t2 = t2.getCause();
            }
            return t1 == null && t2 == null;
        }

        private static int causesHashCode(Throwable t) {
            int hash = 0;
            while (t != null) {
                hash = 31 * hash + Objects.hash(t.getClass(), t.getMessage());
                t = t.getCause();
            }
            return hash;
        }
    }

    private static void requirePositive(int sampleNumber) {
        if (sampleNumber < 1) {
            throw new IllegalArgumentException("sampleNumber is below 1: " + sampleNumber);
        }
    }

    private static String replay(long seed) {
        return " (seed " + seed + ", replay with -D" + CheckConfig.SEED_PROPERTY + "=" + seed + ")";
    }
}
