package dev.zazr.collection;

import dev.zazr.collection.internal.Iterator;
import dev.zazr.control.Either;
import java.time.Duration;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

/// The `range`, `rangeBy`, `rangeClosed` and `rangeClosedBy` factories of `Vector` for `int`, `long` and `char`,
/// which build with the exact size of the range, against the `Vector` built one element at a time from the
/// `Iterator` of the same range: the same elements in the same order, the same empty results and the same
/// exceptions.
public class VectorRangeTest {

    private static final String TOO_MANY = "a Vector cannot hold more than Integer.MAX_VALUE elements";

    /// The Vector of the elements of `elements`, built one element at a time.
    private static <T> Vector<T> reference(Iterator<T> elements) {
        Vector.Builder<T> builder = Vector.newBuilder();
        while (elements.hasNext()) {
            builder.add(elements.next());
        }
        return builder.result();
    }

    /// The result of `build`, or the message of the `IllegalArgumentException` it throws.
    private static <T> Either<String, Vector<T>> attempt(Supplier<Vector<T>> build) {
        try {
            return Either.right(build.get());
        } catch (IllegalArgumentException e) {
            return Either.left(e.getMessage());
        }
    }

    private static <T> void assertSameAs(Supplier<Vector<T>> actual, Supplier<Iterator<T>> expected, String where) {
        Either<String, Vector<T>> built = attempt(actual);
        Either<String, Vector<T>> referenceBuilt = attempt(() -> reference(expected.get()));
        assertThat(built).as(where).isEqualTo(referenceBuilt);
        if (built instanceof Either.Right<String, Vector<T>>(var vector)
                && referenceBuilt instanceof Either.Right<String, Vector<T>>(var other)) {
            assertThat(vector.size()).as(where).isEqualTo(other.size());
            assertThat(vector.asJava()).as(where).containsExactlyElementsOf(other.asJava());
            if (!vector.isEmpty()) {
                T last = vector.last();
                assertThat(vector.append(last)).as(where).isEqualTo(other.append(last));
                assertThat(vector.prepend(last)).as(where).isEqualTo(other.prepend(last));
                int middle = vector.size() / 2;
                assertThat(vector.update(middle, last)).as(where).isEqualTo(other.update(middle, last));
                assertThat(vector.drop(middle)).as(where).isEqualTo(other.drop(middle));
            }
        }
    }

    private static void assertIntRanges(int from, int to, int step) {
        String where = from + " " + to + " " + step;
        assertSameAs(() -> Vector.rangeBy(from, to, step), () -> Iterator.rangeBy(from, to, step), "rangeBy " + where);
        assertSameAs(
                () -> Vector.rangeClosedBy(from, to, step),
                () -> Iterator.rangeClosedBy(from, to, step),
                "rangeClosedBy " + where);
        if (step == 1) {
            assertSameAs(() -> Vector.range(from, to), () -> Iterator.range(from, to), "range " + where);
            assertSameAs(
                    () -> Vector.rangeClosed(from, to), () -> Iterator.rangeClosed(from, to), "rangeClosed " + where);
        }
    }

    private static void assertLongRanges(long from, long to, long step) {
        String where = from + " " + to + " " + step;
        assertSameAs(() -> Vector.rangeBy(from, to, step), () -> Iterator.rangeBy(from, to, step), "rangeBy " + where);
        assertSameAs(
                () -> Vector.rangeClosedBy(from, to, step),
                () -> Iterator.rangeClosedBy(from, to, step),
                "rangeClosedBy " + where);
        if (step == 1) {
            assertSameAs(() -> Vector.range(from, to), () -> Iterator.range(from, to), "range " + where);
            assertSameAs(
                    () -> Vector.rangeClosed(from, to), () -> Iterator.rangeClosed(from, to), "rangeClosed " + where);
        }
    }

    private static void assertCharRanges(char from, char to, int step) {
        String where = (int) from + " " + (int) to + " " + step;
        assertSameAs(() -> Vector.rangeBy(from, to, step), () -> Iterator.rangeBy(from, to, step), "rangeBy " + where);
        assertSameAs(
                () -> Vector.rangeClosedBy(from, to, step),
                () -> Iterator.rangeClosedBy(from, to, step),
                "rangeClosedBy " + where);
        if (step == 1) {
            assertSameAs(() -> Vector.range(from, to), () -> Iterator.range(from, to), "range " + where);
            assertSameAs(
                    () -> Vector.rangeClosed(from, to), () -> Iterator.rangeClosed(from, to), "rangeClosed " + where);
        }
    }

    // empty ranges

    @Test
    public void emptyRanges() {
        assertThat(Vector.range(5, 5)).isEmpty();
        assertThat(Vector.range(6, 5)).isEmpty();
        assertThat(Vector.rangeBy(5, 5, 1)).isEmpty();
        assertThat(Vector.rangeBy(5, 5, -1)).isEmpty();
        assertThat(Vector.rangeBy(6, 5, 2)).isEmpty();
        assertThat(Vector.rangeBy(5, 6, -2)).isEmpty();
        assertThat(Vector.rangeClosed(6, 5)).isEmpty();
        assertThat(Vector.rangeClosedBy(6, 5, 1)).isEmpty();
        assertThat(Vector.rangeClosedBy(5, 6, -1)).isEmpty();
        assertThat(Vector.range(5L, 5L)).isEmpty();
        assertThat(Vector.rangeBy(6L, 5L, 2L)).isEmpty();
        assertThat(Vector.rangeBy(5L, 6L, -2L)).isEmpty();
        assertThat(Vector.rangeClosedBy(6L, 5L, 1L)).isEmpty();
        assertThat(Vector.rangeClosedBy(5L, 6L, -1L)).isEmpty();
        assertThat(Vector.range('b', 'b')).isEmpty();
        assertThat(Vector.rangeBy('c', 'b', 1)).isEmpty();
        assertThat(Vector.rangeBy('b', 'c', -1)).isEmpty();
        assertThat(Vector.rangeClosed('c', 'b')).isEmpty();
        assertThat(Vector.rangeClosedBy('b', 'c', -1)).isEmpty();
        for (int[] range : new int[][] {{5, 5, 1}, {5, 5, -1}, {6, 5, 1}, {5, 6, -1}, {6, 5, 3}, {5, 6, -3}}) {
            assertIntRanges(range[0], range[1], range[2]);
            assertLongRanges(range[0], range[1], range[2]);
            assertCharRanges((char) range[0], (char) range[1], range[2]);
        }
    }

    @Test
    public void anExclusiveBoundAtTheTypeBoundInTheStepDirectionGivesNoElement() {
        assertThat(Vector.rangeBy(0, Integer.MIN_VALUE, 1)).isEmpty();
        assertThat(Vector.rangeBy(0, Integer.MAX_VALUE, -1)).isEmpty();
        assertThat(Vector.range(Integer.MIN_VALUE, Integer.MIN_VALUE)).isEmpty();
        assertThat(Vector.rangeBy(Integer.MAX_VALUE, Integer.MAX_VALUE, -1)).isEmpty();
        assertThat(Vector.rangeBy(0L, Long.MIN_VALUE, 1L)).isEmpty();
        assertThat(Vector.rangeBy(0L, Long.MAX_VALUE, -1L)).isEmpty();
        assertThat(Vector.range(Long.MIN_VALUE, Long.MIN_VALUE)).isEmpty();
        assertThat(Vector.range(Character.MIN_VALUE, Character.MIN_VALUE)).isEmpty();
        assertThat(Vector.rangeBy(Character.MAX_VALUE, Character.MAX_VALUE, -1)).isEmpty();
    }

    // a zero step

    @Test
    public void aZeroStepThrows() {
        for (Supplier<Vector<?>> range : java.util.List.<Supplier<Vector<?>>>of(
                () -> Vector.rangeBy(1, 5, 0),
                () -> Vector.rangeBy(5, 5, 0),
                () -> Vector.rangeBy(0, Integer.MIN_VALUE, 0),
                () -> Vector.rangeBy(0, Integer.MAX_VALUE, 0),
                () -> Vector.rangeClosedBy(1, 5, 0),
                () -> Vector.rangeClosedBy(5, 5, 0),
                () -> Vector.rangeBy(1L, 5L, 0L),
                () -> Vector.rangeBy(5L, 5L, 0L),
                () -> Vector.rangeBy(0L, Long.MAX_VALUE, 0L),
                () -> Vector.rangeClosedBy(1L, 5L, 0L),
                () -> Vector.rangeClosedBy(5L, 5L, 0L),
                () -> Vector.rangeBy('a', 'e', 0),
                () -> Vector.rangeBy('a', 'a', 0),
                () -> Vector.rangeClosedBy('a', 'e', 0),
                () -> Vector.rangeClosedBy('a', 'a', 0))) {
            assertThatThrownBy(range::get)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("step cannot be 0");
        }
    }

    // elements

    @Test
    public void stepsThatDoNotDivideTheSpan() {
        assertThat(Vector.rangeBy(0, 10, 3)).containsExactly(0, 3, 6, 9);
        assertThat(Vector.rangeBy(0, 9, 3)).containsExactly(0, 3, 6);
        assertThat(Vector.rangeClosedBy(0, 10, 3)).containsExactly(0, 3, 6, 9);
        assertThat(Vector.rangeClosedBy(10, 0, -3)).containsExactly(10, 7, 4, 1);
        assertThat(Vector.rangeBy(10L, 0L, -4L)).containsExactly(10L, 6L, 2L);
        assertThat(Vector.rangeClosedBy('a', 'e', 3)).containsExactly('a', 'd');
        assertThat(Vector.rangeBy('e', 'a', -3)).containsExactly('e', 'b');
    }

    @Test
    public void stepsOfOneAndMinusOne() {
        assertThat(Vector.range(-2, 2)).containsExactly(-2, -1, 0, 1);
        assertThat(Vector.rangeClosed(-2, 2)).containsExactly(-2, -1, 0, 1, 2);
        assertThat(Vector.rangeBy(2, -2, -1)).containsExactly(2, 1, 0, -1);
        assertThat(Vector.rangeClosedBy(2, -2, -1)).containsExactly(2, 1, 0, -1, -2);
        assertThat(Vector.range(-2L, 2L)).containsExactly(-2L, -1L, 0L, 1L);
        assertThat(Vector.rangeClosedBy(2L, -2L, -1L)).containsExactly(2L, 1L, 0L, -1L, -2L);
        assertThat(Vector.range('a', 'd')).containsExactly('a', 'b', 'c');
        assertThat(Vector.rangeClosedBy('d', 'a', -1)).containsExactly('d', 'c', 'b', 'a');
    }

    @Test
    public void stepsLargerThanTheSpan() {
        assertThat(Vector.rangeBy(0, 5, 100)).containsExactly(0);
        assertThat(Vector.rangeClosedBy(5, 0, -100)).containsExactly(5);
        assertThat(Vector.rangeBy(0L, 5L, 100L)).containsExactly(0L);
        assertThat(Vector.rangeClosedBy('a', 'z', 1000)).containsExactly('a');
    }

    @Test
    public void intBoundsAndExtremeSteps() {
        int min = Integer.MIN_VALUE;
        int max = Integer.MAX_VALUE;
        assertThat(Vector.rangeClosedBy(min, max, max)).containsExactly(min, -1, max - 1);
        assertThat(Vector.rangeBy(min, max, max)).containsExactly(min, -1, max - 1);
        assertThat(Vector.rangeClosedBy(max, min, min)).containsExactly(max, -1);
        assertThat(Vector.rangeBy(max, min, min)).containsExactly(max, -1);
        assertThat(Vector.rangeClosedBy(max, min, -max)).containsExactly(max, 0, -max);
        assertThat(Vector.rangeClosedBy(min, max, min)).isEmpty();
        assertThat(Vector.rangeClosedBy(max, min, max)).isEmpty();
        assertThat(Vector.rangeClosedBy(max, max, min)).containsExactly(max);
        assertThat(Vector.rangeClosedBy(min, min, max)).containsExactly(min);
        // closed ranges that end exactly at a bound
        assertThat(Vector.rangeClosed(max - 2, max)).containsExactly(max - 2, max - 1, max);
        assertThat(Vector.rangeClosedBy(max - 4, max, 2)).containsExactly(max - 4, max - 2, max);
        assertThat(Vector.rangeClosedBy(min + 2, min, -1)).containsExactly(min + 2, min + 1, min);
        assertThat(Vector.rangeClosedBy(min + 4, min, -2)).containsExactly(min + 4, min + 2, min);
        assertThat(Vector.range(max - 2, max)).containsExactly(max - 2, max - 1);
        assertThat(Vector.rangeBy(min + 2, min, -1)).containsExactly(min + 2, min + 1);
        for (int[] range : new int[][] {
            {min, max, max},
            {max, min, min},
            {max, min, -max},
            {max - 2, max, 1},
            {min + 2, min, -1},
            {max - 5, max, 2},
            {min + 5, min, -2},
            {max, max, 1},
            {min, min, -1},
            {min, max, min},
            {max, min, max}
        }) {
            assertIntRanges(range[0], range[1], range[2]);
        }
    }

    @Test
    public void longBoundsAndExtremeSteps() {
        long min = Long.MIN_VALUE;
        long max = Long.MAX_VALUE;
        assertThat(Vector.rangeClosedBy(min, max, max)).containsExactly(min, -1L, max - 1);
        assertThat(Vector.rangeBy(min, max, max)).containsExactly(min, -1L, max - 1);
        assertThat(Vector.rangeClosedBy(max, min, min)).containsExactly(max, -1L);
        assertThat(Vector.rangeClosedBy(max, min, -max)).containsExactly(max, 0L, -max);
        assertThat(Vector.rangeClosedBy(min, max, 1L << 62)).containsExactly(min, -(1L << 62), 0L, 1L << 62);
        assertThat(Vector.rangeClosedBy(min, max, min)).isEmpty();
        assertThat(Vector.rangeClosedBy(max, max, min)).containsExactly(max);
        assertThat(Vector.rangeClosed(max - 2, max)).containsExactly(max - 2, max - 1, max);
        assertThat(Vector.rangeClosedBy(min + 2, min, -1L)).containsExactly(min + 2, min + 1, min);
        assertThat(Vector.range(max - 2, max)).containsExactly(max - 2, max - 1);
        // int steps on a long range
        assertThat(Vector.rangeClosedBy(0L, 3L * Integer.MAX_VALUE, Integer.MAX_VALUE))
                .containsExactly(0L, (long) Integer.MAX_VALUE, 2L * Integer.MAX_VALUE, 3L * Integer.MAX_VALUE);
        for (long[] range : new long[][] {
            {min, max, max}, {max, min, min}, {max, min, -max}, {min, max, 1L << 62}, {max - 2, max, 1},
            {min + 2, min, -1}, {max - 5, max, 2}, {min + 5, min, -2}, {max, max, 1}, {min, max, min},
            {Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE}
        }) {
            assertLongRanges(range[0], range[1], range[2]);
        }
    }

    @Test
    public void charBoundsAndExtremeSteps() {
        char min = Character.MIN_VALUE;
        char max = Character.MAX_VALUE;
        assertThat(Vector.rangeClosed(min, max)).hasSize(65536);
        assertThat(Vector.range(min, max)).hasSize(65535);
        assertThat(Vector.rangeBy(max, min, -1)).hasSize(65535);
        assertThat(Vector.rangeClosedBy(max, min, -1)).hasSize(65536);
        assertThat(Vector.rangeClosed((char) (max - 2), max)).containsExactly((char) (max - 2), (char) (max - 1), max);
        assertThat(Vector.rangeClosedBy((char) 2, min, -1)).containsExactly((char) 2, (char) 1, min);
        assertThat(Vector.rangeClosedBy(min, max, Integer.MAX_VALUE)).containsExactly(min);
        assertThat(Vector.rangeClosedBy(max, min, Integer.MIN_VALUE)).containsExactly(max);
        assertThat(Vector.rangeClosedBy(min, max, Integer.MIN_VALUE)).isEmpty();
        assertThat(Vector.rangeClosedBy(min, max, 0x8000)).containsExactly(min, (char) 0x8000);
        assertThat(Vector.rangeClosedBy(min, max, 0x7fff)).containsExactly(min, (char) 0x7fff, (char) 0xfffe);
        for (int step : new int[] {
            1, -1, 2, -2, 0x7fff, -0x7fff, 0x8000, 0xffff, 0x10000, -0x10000, Integer.MIN_VALUE, Integer.MAX_VALUE
        }) {
            assertCharRanges(min, max, step);
            assertCharRanges(max, min, step);
            assertCharRanges((char) (max - 3), max, step);
            assertCharRanges((char) 3, min, step);
        }
    }

    // the sizes around the leaf and branch boundaries

    @Test
    public void sizesAroundTheLeafAndBranchBoundaries() {
        for (int size : new int[] {1, 2, 31, 32, 33, 1023, 1024, 1025, 32768, 32769, 40000}) {
            assertIntRanges(0, size, 1);
            assertIntRanges(Integer.MAX_VALUE - size + 1, Integer.MAX_VALUE, 1);
            assertIntRanges(size, 0, -1);
            assertLongRanges(Long.MIN_VALUE, Long.MIN_VALUE + size, 1);
            assertLongRanges(Long.MAX_VALUE, Long.MAX_VALUE - 3L * size, -3);
            assertCharRanges((char) 0, (char) Math.min(size, Character.MAX_VALUE), 1);
        }
    }

    // more than Integer.MAX_VALUE elements

    private static void assertTooMany(Supplier<Vector<?>> range) {
        // the size is checked before any element is built: a build would take far longer, or run out of memory
        assertTimeoutPreemptively(
                Duration.ofSeconds(10),
                () -> assertThatThrownBy(range::get)
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage(TOO_MANY));
    }

    @Test
    public void aRangeOfMoreThanIntegerMaxValueElementsIsRejected() {
        int min = Integer.MIN_VALUE;
        int max = Integer.MAX_VALUE;
        // 2^32 and 2^32 - 1 elements
        assertTooMany(() -> Vector.rangeClosed(min, max));
        assertTooMany(() -> Vector.range(min, max));
        assertTooMany(() -> Vector.rangeClosedBy(max, min, -1));
        assertTooMany(() -> Vector.rangeBy(max, min, -1));
        // 2^31 elements: just over
        assertTooMany(() -> Vector.rangeClosedBy(min, max, 2));
        assertTooMany(() -> Vector.rangeClosedBy(max, min, -2));
        assertTooMany(() -> Vector.rangeClosed(0, max));
        assertTooMany(() -> Vector.range(min, 0));
        assertTooMany(() -> Vector.rangeClosedBy(-1, min, -1));
        // long ranges: 2^64 elements, whose count wraps a long, and just over
        assertTooMany(() -> Vector.rangeClosed(Long.MIN_VALUE, Long.MAX_VALUE));
        assertTooMany(() -> Vector.range(Long.MIN_VALUE, Long.MAX_VALUE));
        assertTooMany(() -> Vector.rangeClosedBy(Long.MAX_VALUE, Long.MIN_VALUE, -1L));
        assertTooMany(() -> Vector.rangeClosedBy(Long.MIN_VALUE, Long.MAX_VALUE, 2L));
        assertTooMany(() -> Vector.rangeClosed(0L, (long) max));
        assertTooMany(() -> Vector.range(0L, (long) max + 1));
        assertTooMany(() -> Vector.rangeBy((long) max, -1L, -1L));
        long wide = 1L << 32;
        assertTooMany(() -> Vector.rangeClosedBy(0L, max * wide, wide));
        assertTooMany(() -> Vector.rangeBy(0L, max * wide + 1, wide));
    }

    // many random small ranges

    private static final int[] INT_ANCHORS = {0, Integer.MIN_VALUE, Integer.MAX_VALUE};
    private static final int[] INT_EXTREME_STEPS = {Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE + 1};
    private static final long[] LONG_ANCHORS = {0, Long.MIN_VALUE, Long.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE
    };
    private static final long[] LONG_EXTREME_STEPS = {Long.MIN_VALUE, Long.MAX_VALUE, Long.MIN_VALUE + 1};

    /// A step: zero now and then, a small one, an extreme one or any one; `extreme` gives the extreme ones.
    private static long step(Random random, long[] extreme) {
        return switch (random.nextInt(8)) {
            case 0 -> 0;
            case 1, 2 -> extreme[random.nextInt(extreme.length)];
            case 3 -> random.nextLong();
            default -> (random.nextBoolean() ? 1 : -1) * (1 + random.nextInt(40));
        };
    }

    /// A small bound on the number of elements, for the ranges worth building.
    private static boolean small(double from, double to, double step) {
        return step == 0 || Math.abs(to - from) / Math.abs(step) < 3000;
    }

    @Test
    public void randomIntRanges() {
        Random random = new Random(3651);
        long built = IntStream.range(0, 4000)
                .filter(i -> {
                    int from = INT_ANCHORS[random.nextInt(INT_ANCHORS.length)] + random.nextInt(201) - 100;
                    int to = random.nextInt(4) == 0
                            ? INT_ANCHORS[random.nextInt(INT_ANCHORS.length)] + random.nextInt(201) - 100
                            : from + random.nextInt(201) - 100;
                    int step = random.nextInt(4) == 0
                            ? INT_EXTREME_STEPS[random.nextInt(INT_EXTREME_STEPS.length)]
                            : (int) step(random, new long[] {Integer.MIN_VALUE, Integer.MAX_VALUE});
                    if (!small(from, to, step)) {
                        return false;
                    }
                    assertIntRanges(from, to, step);
                    if (small(from, to, 1)) {
                        assertIntRanges(from, to, 1);
                    }
                    return true;
                })
                .count();
        assertThat(built).isGreaterThan(2000);
    }

    @Test
    public void randomLongRanges() {
        Random random = new Random(3652);
        long built = IntStream.range(0, 4000)
                .filter(i -> {
                    long from = LONG_ANCHORS[random.nextInt(LONG_ANCHORS.length)] + random.nextInt(201) - 100;
                    long to = random.nextInt(4) == 0
                            ? LONG_ANCHORS[random.nextInt(LONG_ANCHORS.length)] + random.nextInt(201) - 100
                            : from + random.nextInt(201) - 100;
                    long step = step(random, LONG_EXTREME_STEPS);
                    if (!small(from, to, step)) {
                        return false;
                    }
                    assertLongRanges(from, to, step);
                    if (small(from, to, 1)) {
                        assertLongRanges(from, to, 1);
                    }
                    return true;
                })
                .count();
        assertThat(built).isGreaterThan(2000);
    }

    @Test
    public void randomCharRanges() {
        Random random = new Random(3653);
        long built = IntStream.range(0, 4000)
                .filter(i -> {
                    char from = (char) (random.nextBoolean() ? random.nextInt(201) : 0xffff - random.nextInt(201));
                    char to = random.nextInt(4) == 0
                            ? (char) random.nextInt(0x10000)
                            : (char) Math.clamp(from + random.nextInt(201) - 100, 0, 0xffff);
                    int step = (int) step(random, new long[] {Integer.MIN_VALUE, Integer.MAX_VALUE, 0x10000});
                    if (!small(from, to, step)) {
                        return false;
                    }
                    assertCharRanges(from, to, step);
                    if (small(from, to, 1)) {
                        assertCharRanges(from, to, 1);
                    }
                    return true;
                })
                .count();
        assertThat(built).isGreaterThan(2000);
    }
}
