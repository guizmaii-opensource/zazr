package com.guizmaii.zazr.test.laws;

import com.guizmaii.zazr.test.legacy.PredicateResult;

import java.util.Objects;

/**
 * Predicate results that explain what differed.
 */
final class Results {

    private Results() {
    }

    static PredicateResult equal(Object left, Object right) {
        return Objects.equals(left, right)
                ? PredicateResult.success()
                : PredicateResult.failure("left = " + left + ", right = " + right);
    }

    static PredicateResult check(boolean holds, String explanation) {
        return holds ? PredicateResult.success() : PredicateResult.failure(explanation);
    }

    /// The first failure of the two, or success.
    static PredicateResult both(PredicateResult first, PredicateResult second) {
        return first.isSuccess() ? second : first;
    }
}
