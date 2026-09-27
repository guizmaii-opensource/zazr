package dev.zazr.test;

import dev.zazr.control.Option;

import java.util.Objects;

/**
 * The outcome of {@link Assertion#assertThat}: a success, or a failure that explains, in plain text, why the value
 * did not satisfy the assertions. A property returns it from its body, and {@link Check} reports the explanation of a
 * failing sample next to the counterexample and the seed.
 * <p>
 * Results combine with {@link #and(TestResult)} and {@link #or(TestResult)}; {@link #label(String)} names a result
 * in its explanation.
 */
public sealed interface TestResult permits TestResult.Success, TestResult.Failure {

    /**
     * A success.
     *
     * @return the successful result
     */
    static TestResult succeed() {
        return Success.INSTANCE;
    }

    /**
     * A failure with an explanation.
     *
     * @param explanation why the check failed, possibly on several lines
     * @return a failed result
     * @throws NullPointerException if {@code explanation} is null
     */
    static TestResult fail(String explanation) {
        return new Failure(explanation);
    }

    /**
     * Whether this result is a success.
     *
     * @return true for a {@link Success}
     */
    default boolean isSuccess() {
        return this instanceof Success;
    }

    /**
     * Whether this result is a failure.
     *
     * @return true for a {@link Failure}
     */
    default boolean isFailure() {
        return this instanceof Failure;
    }

    /**
     * The explanation of a failure.
     *
     * @return the explanation of a {@link Failure}, none for a success
     */
    default Option<String> message() {
        return this instanceof Failure(var explanation) ? Option.some(explanation) : Option.none();
    }

    /**
     * A result that succeeds when both succeed. A failure explains every failing side.
     *
     * @param that the other result
     * @return the combined result
     * @throws NullPointerException if {@code that} is null
     */
    default TestResult and(TestResult that) {
        Objects.requireNonNull(that, "that is null");
        return switch (this) {
            case Success ignored -> that;
            case Failure(var first) -> that instanceof Failure(var second) ? new Failure(first + "\n" + second) : this;
        };
    }

    /**
     * A result that succeeds when either succeeds. A failure explains both sides.
     *
     * @param that the other result
     * @return the combined result
     * @throws NullPointerException if {@code that} is null
     */
    default TestResult or(TestResult that) {
        Objects.requireNonNull(that, "that is null");
        return switch (this) {
            case Success ignored -> this;
            case Failure(var first) -> that instanceof Failure(var second)
                    ? new Failure("neither of these holds:\n" + indent(first) + "\n" + indent(second))
                    : that;
        };
    }

    /**
     * This result, with {@code label} in front of the explanation of a failure.
     *
     * @param label a name for what was checked
     * @return a labelled result
     * @throws NullPointerException if {@code label} is null
     */
    default TestResult label(String label) {
        Objects.requireNonNull(label, "label is null");
        return switch (this) {
            case Success ignored -> this;
            case Failure(var explanation) -> new Failure(explanation.contains("\n")
                    ? label + ":\n" + indent(explanation)
                    : label + ": " + explanation);
        };
    }

    /**
     * A success.
     */
    record Success() implements TestResult {

        private static final Success INSTANCE = new Success();
    }

    /**
     * A failure.
     *
     * @param explanation why the check failed, possibly on several lines
     */
    record Failure(String explanation) implements TestResult {

        /**
         * Creates a failure.
         *
         * @param explanation why the check failed
         * @throws NullPointerException if {@code explanation} is null
         */
        public Failure {
            Objects.requireNonNull(explanation, "explanation is null");
        }
    }

    /// Every line of `text` indented by two spaces.
    private static String indent(String text) {
        return "  " + text.replace("\n", "\n  ");
    }
}
