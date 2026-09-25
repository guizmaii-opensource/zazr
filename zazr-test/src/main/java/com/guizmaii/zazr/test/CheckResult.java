package com.guizmaii.zazr.test;

import com.guizmaii.zazr.Tuple;
import com.guizmaii.zazr.control.Option;

import java.util.Objects;

/**
 * Represents the result of a property check which is
 *
 * <ul>
 * <li>{@link Satisfied}, if all tests satisfied the given property</li>
 * <li>{@link Falsified}, if a counter-example could be discovered that falsified the given property</li>
 * <li>{@link Erroneous}, if an exception occurred executing the property check</li>
 * </ul>
 *
 * Please note that a {@code Satisfied} property check may be {@code Exhausted}, if the property is an implication
 * and no sample could be found that satisfied the pre-condition. In this case the post-condition is satisfied by
 * definition (see <a href="http://en.wikipedia.org/wiki/Principle_of_explosion">ex falso quodlibet</a>).
 */
public sealed interface CheckResult permits CheckResult.Satisfied, CheckResult.Falsified, CheckResult.Erroneous {

    /**
     * If this check result is satisfied as specified above.
     *
     * @return true, if this check result is satisfied, false otherwise
     */
    default boolean isSatisfied() {
        return this instanceof Satisfied;
    }

    /**
     * If this check result is falsified as specified above.
     *
     * @return true, if this check result is falsified, false otherwise
     */
    default boolean isFalsified() {
        return this instanceof Falsified;
    }

    /**
     * If this check result is erroneous as specified above.
     *
     * @return true, if this check result is erroneous, false otherwise
     */
    default boolean isErroneous() {
        return this instanceof Erroneous;
    }

    /**
     * If this check result is exhausted as specified above.
     *
     * @return true, if this check result is exhausted, false otherwise
     */
    default boolean isExhausted() {
        return this instanceof Satisfied satisfied && satisfied.exhausted();
    }

    /**
     * The name of the checked property this result refers to.
     *
     * @return a property name
     */
    String propertyName();

    /**
     * The number of checks performed using random generated input data.
     *
     * @return the number of checks performed
     */
    int count();

    /**
     * An optional sample which falsified the property or which lead to an error.
     *
     * @return an optional sample
     */
    Option<Tuple> sample();

    /**
     * An optional error.
     *
     * @return an optional error
     */
    Option<Error> error();

    /**
     * An optional explanation supplied by a predicate that falsified the property.
     *
     * @return the predicate's failure message, or none when no message was supplied
     */
    default Option<String> message() {
        return Option.none();
    }

    /**
     * Asserts that this CheckResult is satisfied.
     *
     * @throws AssertionError if this CheckResult is not satisfied.
     */
    default void assertIsSatisfied() {
        if (!isSatisfied()) {
            throw new AssertionError("Expected satisfied check result but was " + this);
        }
    }

    /**
     * Asserts that this CheckResult is satisfied with a given exhausted state.
     *
     * @param exhausted The exhausted state to be checked in the case of a satisfied CheckResult.
     * @throws AssertionError if this CheckResult is not satisfied or the exhausted state does not match.
     */
    default void assertIsSatisfiedWithExhaustion(boolean exhausted) {
        if (!isSatisfied()) {
            throw new AssertionError("Expected satisfied check result but was " + this);
        } else if (isExhausted() != exhausted) {
            throw new AssertionError("Expected satisfied check result to be " + (exhausted ? "" : "not ") + "exhausted but was: " + this);
        }
    }

    /**
     * Asserts that this CheckResult is falsified.
     *
     * @throws AssertionError if this CheckResult is not falsified.
     */
    default void assertIsFalsified() {
        if (!isFalsified()) {
            throw new AssertionError("Expected falsified check result but was " + this);
        }
    }

    /**
     * Asserts that this CheckResult is erroneous.
     *
     * @throws AssertionError if this CheckResult is not erroneous.
     */
    default void assertIsErroneous() {
        if (!isErroneous()) {
            throw new AssertionError("Expected erroneous check result but was " + this);
        }
    }

    /**
     * A satisfied property check.
     *
     * @param propertyName the name of the checked property
     * @param count        the number of checks performed
     * @param exhausted    whether no sample satisfied the precondition of an implication
     */
    record Satisfied(String propertyName, int count, boolean exhausted) implements CheckResult {

        /**
         * Creates a satisfied result.
         *
         * @param propertyName the name of the checked property
         * @param count        the number of checks performed
         * @param exhausted    whether no sample satisfied the precondition of an implication
         * @throws NullPointerException if {@code propertyName} is null
         */
        public Satisfied {
            Objects.requireNonNull(propertyName, "propertyName is null");
        }

        @Override
        public Option<Tuple> sample() {
            return Option.none();
        }

        @Override
        public Option<Error> error() {
            return Option.none();
        }

        @Override
        public String toString() {
            return String.format("Satisfied(propertyName = %s, count = %s, exhausted = %s)", propertyName, count, exhausted);
        }
    }

    /**
     * A falsified property check.
     *
     * @param propertyName   the name of the checked property
     * @param count          the number of the check that found the counterexample
     * @param counterexample the generated values that falsified the property
     * @param message        the explanation supplied by the predicate, if any
     */
    record Falsified(String propertyName, int count, Tuple counterexample, Option<String> message) implements CheckResult {

        /**
         * Creates a falsified result.
         *
         * @param propertyName   the name of the checked property
         * @param count          the number of the check that found the counterexample
         * @param counterexample the generated values that falsified the property
         * @param message        the explanation supplied by the predicate, if any
         * @throws NullPointerException if an argument is null
         */
        public Falsified {
            Objects.requireNonNull(propertyName, "propertyName is null");
            Objects.requireNonNull(counterexample, "counterexample is null");
            Objects.requireNonNull(message, "message is null");
        }

        /**
         * Creates a falsified result without an explanation.
         *
         * @param propertyName   the name of the checked property
         * @param count          the number of the check that found the counterexample
         * @param counterexample the generated values that falsified the property
         * @throws NullPointerException if an argument is null
         */
        public Falsified(String propertyName, int count, Tuple counterexample) {
            this(propertyName, count, counterexample, Option.none());
        }

        @Override
        public Option<Tuple> sample() {
            return Option.some(counterexample);
        }

        @Override
        public Option<Error> error() {
            return Option.none();
        }

        @Override
        public String toString() {
            return String.format("Falsified(propertyName = %s, count = %s, sample = %s%s)", propertyName, count, counterexample,
                    message.map(m -> ", message = " + m).getOrElse(""));
        }
    }

    /**
     * An erroneous property check. Two erroneous results are equal when their causes have the same messages along
     * the whole cause chain.
     *
     * @param propertyName the name of the checked property
     * @param count        the number of the check that failed
     * @param cause        the error thrown by an arbitrary, a generator or the predicate
     * @param sample       the generated values, when the error was thrown by the predicate
     */
    record Erroneous(String propertyName, int count, Error cause, Option<Tuple> sample) implements CheckResult {

        /**
         * Creates an erroneous result.
         *
         * @param propertyName the name of the checked property
         * @param count        the number of the check that failed
         * @param cause        the error thrown by an arbitrary, a generator or the predicate
         * @param sample       the generated values, when the error was thrown by the predicate
         * @throws NullPointerException if {@code propertyName} or {@code sample} is null
         */
        public Erroneous {
            Objects.requireNonNull(propertyName, "propertyName is null");
            Objects.requireNonNull(sample, "sample is null");
        }

        @Override
        public Option<Error> error() {
            return cause == null ? Option.none() : Option.some(cause);
        }

        @Override
        public boolean equals(Object o) {
            return o == this || (o instanceof Erroneous that
                    && Objects.equals(this.propertyName, that.propertyName)
                    && this.count == that.count
                    && deepEquals(this.cause, that.cause)
                    && Objects.equals(this.sample, that.sample));
        }

        @Override
        public int hashCode() {
            return Objects.hash(propertyName, count, deepHashCode(cause), sample);
        }

        @Override
        public String toString() {
            return String.format("Erroneous(propertyName = %s, count = %s, error = %s, sample = %s)", propertyName, count,
                    cause == null ? null : cause.getMessage(), sample);
        }

        static boolean deepEquals(Throwable t1, Throwable t2) {
            return (t1 == null && t2 == null) || (
                    t1 != null && t2 != null
                            && Objects.equals(t1.getMessage(), t2.getMessage())
                            && deepEquals(t1.getCause(), t2.getCause())
            );
        }

        static int deepHashCode(Throwable t) {
            return t == null ? 0 : Objects.hash(t.getMessage(), deepHashCode(t.getCause()));
        }
    }
}
