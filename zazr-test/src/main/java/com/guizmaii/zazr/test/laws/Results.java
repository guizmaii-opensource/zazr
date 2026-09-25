package com.guizmaii.zazr.test.laws;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * The checks of a law body: each returns {@code true} when it holds and throws an {@link AssertionError} that
 * explains the difference otherwise, so a falsified law reports what differed. They chain with {@code &&}: the first
 * failure ends the sample.
 */
final class Results {

    private Results() {
    }

    /// True when both values are equal; otherwise throws with both sides.
    static boolean equal(Object left, Object right) {
        if (!Objects.equals(left, right)) {
            throw new AssertionError("left = " + left + ", right = " + right);
        }
        return true;
    }

    /// True when `holds`; otherwise throws with the explanation, built only then.
    static boolean check(boolean holds, Supplier<String> explanation) {
        if (!holds) {
            throw new AssertionError(explanation.get());
        }
        return true;
    }
}
