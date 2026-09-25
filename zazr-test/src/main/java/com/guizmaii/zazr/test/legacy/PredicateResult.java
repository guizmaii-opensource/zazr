package com.guizmaii.zazr.test.legacy;

import com.guizmaii.zazr.control.Option;

import java.util.Objects;

/**
 * The result of evaluating a predicate for one sample, with a message explaining a failure.
 * Use with {@code suchThatResult} or {@code impliesResult} on a property builder.
 */
public final class PredicateResult {

    private static final PredicateResult SUCCESS = new PredicateResult(null);

    private final String message;

    private PredicateResult(String message) {
        this.message = message;
    }

    /**
     * Returns a successful predicate result.
     *
     * @return a successful result without a message
     */
    public static PredicateResult success() {
        return SUCCESS;
    }

    /**
     * Returns a failed predicate result with an explanation.
     *
     * @param message the explanation of the failure
     * @return a failed result with the given message
     * @throws NullPointerException if message is null
     */
    public static PredicateResult failure(String message) {
        return new PredicateResult(Objects.requireNonNull(message, "message is null"));
    }

    /**
     * Tests whether the predicate succeeded.
     *
     * @return true if the predicate succeeded, false otherwise
     */
    public boolean isSuccess() {
        return message == null;
    }

    /**
     * Returns the explanation of a predicate failure, if any.
     *
     * @return the failure message, or none for a successful result
     */
    public Option<String> message() {
        return Option.ofNullable(message);
    }
}
