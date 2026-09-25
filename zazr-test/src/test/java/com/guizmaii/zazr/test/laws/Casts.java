package com.guizmaii.zazr.test.laws;

/**
 * The unchecked casts the subjects need where a type's wildcard cannot be captured.
 */
final class Casts {

    private Casts() {
    }

    @SuppressWarnings("unchecked")
    static <T> T cast(Object value) {
        return (T) value;
    }
}
