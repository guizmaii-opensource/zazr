package dev.zazr.collection.internal;

/// The number of elements of the integral ranges the `range*` factories of `Vector` build: the values
/// `from, from + step, from + 2 * step, ...` that do not pass `toInclusive`, the same values as the
/// `Iterator.rangeClosedBy` of the same arguments.
public final class Ranges {

    private Ranges() {}

    /// The number of elements of the closed range from `from` to `toInclusive` by `step`, computed without
    /// overflow: 1 when `from == toInclusive`, 0 when `step` points away from `toInclusive`. An `int` or `char`
    /// range is given widened to `long`.
    ///
    /// The span `|toInclusive - from|` and `|step|` are read as unsigned `long` values, which holds them exactly
    /// (the span of two `long` values is below 2^64, and `-Long.MIN_VALUE` reads as 2^63): the count is the
    /// unsigned quotient plus one.
    ///
    /// @throws IllegalArgumentException if `step` is zero, or if the range holds more than `Integer.MAX_VALUE`
    /// elements
    public static int sizeOf(long from, long toInclusive, long step) {
        if (step == 0) {
            throw new IllegalArgumentException("step cannot be 0");
        } else if (from == toInclusive) {
            return 1;
        } else if ((step > 0) == (from > toInclusive)) {
            return 0;
        }
        long steps = (step > 0)
                ? Long.divideUnsigned(toInclusive - from, step)
                : Long.divideUnsigned(from - toInclusive, -step);
        // the count is steps + 1, which must not exceed Integer.MAX_VALUE; steps + 1 itself wraps when steps is
        // 2^64 - 1, so the quotient is checked
        if (Long.compareUnsigned(steps, Integer.MAX_VALUE) >= 0) {
            throw new IllegalArgumentException("a Vector cannot hold more than Integer.MAX_VALUE elements");
        }
        return (int) steps + 1;
    }
}
