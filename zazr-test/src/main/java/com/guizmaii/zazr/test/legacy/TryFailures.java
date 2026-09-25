package com.guizmaii.zazr.test.legacy;

/**
 * The exceptions {@link Arbitrary#tryOf(Arbitrary)} fails with, shared so that equal failures can be drawn twice.
 */
final class TryFailures {

    static final Exception[] ALL = {
            new IllegalStateException("arbitrary failure 1"),
            new IllegalArgumentException("arbitrary failure 2"),
            new java.io.IOException("arbitrary failure 3"),
    };

    private TryFailures() {
    }
}
