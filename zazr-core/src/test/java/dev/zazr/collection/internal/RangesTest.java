package dev.zazr.collection.internal;

import java.math.BigInteger;
import java.util.Random;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// The size of a closed range, [Ranges#sizeOf(long, long, long)], at the bounds of `int` and `long`, around
/// `Integer.MAX_VALUE` elements, and against the count of the elements of `Iterator.rangeClosedBy`.
public class RangesTest {

    private static final String TOO_MANY = "a Vector cannot hold more than Integer.MAX_VALUE elements";

    private static final long INT_MIN = Integer.MIN_VALUE;
    private static final long INT_MAX = Integer.MAX_VALUE;

    private static void assertTooMany(long from, long toInclusive, long step) {
        assertThatThrownBy(() -> Ranges.sizeOf(from, toInclusive, step))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(TOO_MANY);
    }

    @Test
    public void aZeroStepThrows() {
        for (long[] bounds : new long[][] {{0, 0}, {0, 10}, {10, 0}, {Long.MIN_VALUE, Long.MAX_VALUE}}) {
            assertThatThrownBy(() -> Ranges.sizeOf(bounds[0], bounds[1], 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("step cannot be 0");
        }
    }

    @Test
    public void equalBoundsHoldOneElementWhateverTheStep() {
        for (long step : new long[] {1, -1, Long.MIN_VALUE, Long.MAX_VALUE, INT_MIN, INT_MAX}) {
            assertThat(Ranges.sizeOf(0, 0, step)).isEqualTo(1);
            assertThat(Ranges.sizeOf(Long.MIN_VALUE, Long.MIN_VALUE, step)).isEqualTo(1);
            assertThat(Ranges.sizeOf(Long.MAX_VALUE, Long.MAX_VALUE, step)).isEqualTo(1);
        }
    }

    @Test
    public void aStepAwayFromTheEndGivesNoElement() {
        assertThat(Ranges.sizeOf(1, 0, 1)).isZero();
        assertThat(Ranges.sizeOf(0, 1, -1)).isZero();
        assertThat(Ranges.sizeOf(Long.MAX_VALUE, Long.MIN_VALUE, Long.MAX_VALUE))
                .isZero();
        assertThat(Ranges.sizeOf(Long.MIN_VALUE, Long.MAX_VALUE, Long.MIN_VALUE))
                .isZero();
    }

    @Test
    public void stepsThatDoNotDivideTheSpan() {
        assertThat(Ranges.sizeOf(0, 9, 3)).isEqualTo(4);
        assertThat(Ranges.sizeOf(0, 10, 3)).isEqualTo(4);
        assertThat(Ranges.sizeOf(0, 11, 3)).isEqualTo(4);
        assertThat(Ranges.sizeOf(0, 12, 3)).isEqualTo(5);
        assertThat(Ranges.sizeOf(12, 0, -5)).isEqualTo(3);
        assertThat(Ranges.sizeOf(0, 5, 100)).isEqualTo(1);
        assertThat(Ranges.sizeOf(5, 0, -100)).isEqualTo(1);
    }

    @Test
    public void intBounds() {
        assertThat(Ranges.sizeOf(INT_MIN, INT_MAX, INT_MAX)).isEqualTo(3);
        assertThat(Ranges.sizeOf(INT_MAX, INT_MIN, INT_MIN)).isEqualTo(2);
        assertThat(Ranges.sizeOf(INT_MIN, INT_MAX, 3)).isEqualTo(1431655766);
        assertThat(Ranges.sizeOf(INT_MAX - 2, INT_MAX, 1)).isEqualTo(3);
        assertThat(Ranges.sizeOf(INT_MIN + 2, INT_MIN, -1)).isEqualTo(3);
        // 2^32 elements, 2^31 elements: just over
        assertTooMany(INT_MIN, INT_MAX, 1);
        assertTooMany(INT_MAX, INT_MIN, -1);
        assertTooMany(INT_MIN, INT_MAX, 2);
        assertTooMany(INT_MAX, INT_MIN, -2);
    }

    @Test
    public void longBounds() {
        assertThat(Ranges.sizeOf(Long.MIN_VALUE, Long.MAX_VALUE, Long.MAX_VALUE))
                .isEqualTo(3);
        assertThat(Ranges.sizeOf(Long.MAX_VALUE, Long.MIN_VALUE, Long.MIN_VALUE))
                .isEqualTo(2);
        assertThat(Ranges.sizeOf(Long.MAX_VALUE, Long.MIN_VALUE, -Long.MAX_VALUE))
                .isEqualTo(3);
        assertThat(Ranges.sizeOf(0, Long.MAX_VALUE, Long.MAX_VALUE)).isEqualTo(2);
        assertThat(Ranges.sizeOf(Long.MAX_VALUE - 2, Long.MAX_VALUE, 1)).isEqualTo(3);
        assertThat(Ranges.sizeOf(Long.MIN_VALUE + 2, Long.MIN_VALUE, -1)).isEqualTo(3);
        assertThat(Ranges.sizeOf(Long.MIN_VALUE, Long.MAX_VALUE, 1L << 62)).isEqualTo(4);
        // 2^64 elements: the quotient is 2^64 - 1, whose successor wraps to 0
        assertTooMany(Long.MIN_VALUE, Long.MAX_VALUE, 1);
        assertTooMany(Long.MAX_VALUE, Long.MIN_VALUE, -1);
        assertTooMany(Long.MIN_VALUE, Long.MAX_VALUE, 2);
        assertTooMany(0, Long.MAX_VALUE, 1);
        assertTooMany(Long.MIN_VALUE, 0, 1);
    }

    @Test
    public void exactlyIntegerMaxValueElementsAreAccepted() {
        assertThat(Ranges.sizeOf(0, INT_MAX - 1, 1)).isEqualTo(Integer.MAX_VALUE);
        assertThat(Ranges.sizeOf(INT_MAX - 1, 0, -1)).isEqualTo(Integer.MAX_VALUE);
        assertThat(Ranges.sizeOf(INT_MIN, -2, 1)).isEqualTo(Integer.MAX_VALUE);
        assertThat(Ranges.sizeOf(Long.MIN_VALUE, Long.MIN_VALUE + INT_MAX - 1, 1))
                .isEqualTo(Integer.MAX_VALUE);
        long wide = 1L << 32;
        assertThat(Ranges.sizeOf(0, (INT_MAX - 1) * wide, wide)).isEqualTo(Integer.MAX_VALUE);
        assertThat(Ranges.sizeOf(0, INT_MAX * wide - 1, wide)).isEqualTo(Integer.MAX_VALUE);
        assertThat(Ranges.sizeOf(INT_MAX * wide - 1, 0, -wide)).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    public void oneElementOverIntegerMaxValueIsRejected() {
        assertTooMany(0, INT_MAX, 1);
        assertTooMany(INT_MAX, 0, -1);
        assertTooMany(INT_MIN, -1, 1);
        assertTooMany(Long.MIN_VALUE, Long.MIN_VALUE + INT_MAX, 1);
        long wide = 1L << 32;
        assertTooMany(0, INT_MAX * wide, wide);
        assertTooMany(INT_MAX * wide, 0, -wide);
    }

    /// Many small ranges, near 0 and the bounds of `long`, with small and extreme steps: the size is the number
    /// of elements `Iterator.rangeClosedBy` yields, and the exact count computed with `BigInteger`.
    @Test
    public void theSizeIsTheNumberOfElementsOfTheIterator() {
        Random random = new Random(365);
        long checked = IntStream.range(0, 20000).filter(i -> checkOne(random)).count();
        assertThat(checked).isGreaterThan(5000);
    }

    private static final long[] ANCHORS = {0, Long.MIN_VALUE, Long.MAX_VALUE, INT_MIN, INT_MAX};
    private static final long[] EXTREME_STEPS = {Long.MIN_VALUE, Long.MAX_VALUE, Long.MIN_VALUE + 1, INT_MIN, INT_MAX};

    /// Checks one random range; true when its size was compared with the iterator's.
    private static boolean checkOne(Random random) {
        long from = ANCHORS[random.nextInt(ANCHORS.length)] + random.nextInt(201) - 100;
        long to = ANCHORS[random.nextInt(ANCHORS.length)] + random.nextInt(201) - 100;
        long step = random.nextBoolean()
                ? EXTREME_STEPS[random.nextInt(EXTREME_STEPS.length)]
                : (random.nextBoolean() ? 1 : -1) * (1 + random.nextInt(40));
        BigInteger span = BigInteger.valueOf(to).subtract(BigInteger.valueOf(from));
        BigInteger expected = span.signum() != 0 && span.signum() != Long.signum(step)
                ? BigInteger.ZERO
                : span.divide(BigInteger.valueOf(step)).add(BigInteger.ONE);
        if (expected.compareTo(BigInteger.valueOf(2000)) <= 0) {
            int size = Ranges.sizeOf(from, to, step);
            assertThat(size).as("%d %d %d", from, to, step).isEqualTo(expected.intValueExact());
            assertThat(Iterator.rangeClosedBy(from, to, step).toVector().size())
                    .as("%d %d %d", from, to, step)
                    .isEqualTo(size);
            return true;
        }
        if (expected.compareTo(BigInteger.valueOf(INT_MAX)) > 0) {
            assertTooMany(from, to, step);
        }
        return false;
    }
}
