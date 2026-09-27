package dev.zazr.collection;

import dev.zazr.collection.internal.Iterator;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class VectorPropertyTest {

    /* the width of a leaf */
    private static final int WIDTH = 32;

    @Test
    public void shouldCreateAndGet() {
        for (int i = 0; i < 500; i++) {
            dev.zazr.collection.List<Integer> expected = dev.zazr.collection.List.range(0, i);
            Vector<Integer> actual = Vector.ofAll(expected);
            for (int j = 0; j < actual.size(); j++) {
                assertThat(expected.get(j)).isEqualTo(actual.get(j));
            }

            /* boolean */
            dev.zazr.collection.List<Boolean> expectedBoolean = expected.map(v -> v > 0);
            Vector<Boolean> actualBoolean = Vector.ofAll(booleans(expectedBoolean));
            assertAreEqual(expectedBoolean, actualBoolean);

            /* byte */
            dev.zazr.collection.List<Byte> expectedByte = expected.map(Integer::byteValue);
            Vector<Byte> actualByte = Vector.ofAll(bytes(expectedByte));
            assertAreEqual(expectedByte, actualByte);

            /* char */
            dev.zazr.collection.List<Character> expectedChar = expected.map(v -> (char) v.intValue());
            Vector<Character> actualChar = Vector.ofAll(chars(expectedChar));
            assertAreEqual(expectedChar, actualChar);

            /* double */
            dev.zazr.collection.List<Double> expectedDouble = expected.map(Integer::doubleValue);
            Vector<Double> actualDouble = Vector.ofAll(doubles(expectedDouble));
            assertAreEqual(expectedDouble, actualDouble);

            /* float */
            dev.zazr.collection.List<Float> expectedFloat = expected.map(Integer::floatValue);
            Vector<Float> actualFloat = Vector.ofAll(floats(expectedFloat));
            assertAreEqual(expectedFloat, actualFloat);

            /* int */
            Vector<Integer> actualInt = Vector.ofAll(ints(expected));
            assertAreEqual(expected, actualInt);

            /* long */
            dev.zazr.collection.List<Long> expectedLong = expected.map(Integer::longValue);
            Vector<Long> actualLong = Vector.ofAll(longs(expectedLong));
            assertAreEqual(expectedLong, actualLong);

            /* short */
            dev.zazr.collection.List<Short> expectedShort = expected.map(Integer::shortValue);
            Vector<Short> actualShort = Vector.ofAll(shorts(expectedShort));
            assertAreEqual(expectedShort, actualShort);
        }
    }

    @Test
    public void shouldIterate() {
        for (byte depth = 0; depth <= 2; depth++) {
            for (int i = 0; i < 5000; i++) {
                dev.zazr.collection.List<Integer> expected = dev.zazr.collection.List.range(0, i);
                Vector<Integer> actual = Vector.ofAll(expected);
                assertAreEqual(actual, expected);
            }
        }

        dev.zazr.collection.List<Integer> start = dev.zazr.collection.List.range(0, 1000);
        Vector.rangeClosed(0, WIDTH + 1).foldLeft(new Both<>(start, Vector.ofAll(ints(start))), (both, drop) -> {
            assertAreEqual(both.actual(), both.expected());
            return new Both<>(
                    both.expected().tail().init(), both.actual().tail().init());
        });
    }

    @Test
    public void shouldPrepend() {
        Vector.rangeClosed(0, WIDTH + 1)
                .foldLeft(
                        new Both<Integer>(dev.zazr.collection.List.empty(), Vector.empty()),
                        (outer, drop) -> Vector.range(0, 1000).foldLeft(outer, (both, value) -> {
                            dev.zazr.collection.List<Integer> dropped =
                                    both.expected().drop(drop);
                            Vector<Integer> actual = assertAreEqual(both.actual(), drop, Vector::drop, dropped);

                            dev.zazr.collection.List<Integer> expected = dropped.prepend(value);
                            return new Both<>(expected, assertAreEqual(actual, value, Vector::prepend, expected));
                        }));
    }

    @Test
    public void shouldAppend() {
        Vector.rangeClosed(0, WIDTH + 1)
                .foldLeft(
                        new Both<Integer>(dev.zazr.collection.List.empty(), Vector.empty()),
                        (outer, drop) -> Vector.range(0, 500).foldLeft(outer, (both, value) -> {
                            dev.zazr.collection.List<Integer> dropped =
                                    both.expected().drop(drop);
                            Vector<Integer> actual = assertAreEqual(both.actual(), drop, Vector::drop, dropped);

                            dev.zazr.collection.List<Integer> expected = dropped.append(value);
                            return new Both<>(expected, assertAreEqual(actual, value, Vector::append, expected));
                        }));
    }

    @Test
    public void shouldUpdate() {
        Function<Integer, Integer> mapper = i -> i + 1;

        for (byte depth = 0; depth <= 2; depth++) {
            int length = 10_000;

            for (int drop = 0; drop <= (WIDTH + 1); drop++) {
                dev.zazr.collection.List<Integer> all = dev.zazr.collection.List.range(0, length);
                dev.zazr.collection.List<Integer> expected =
                        all.drop(drop); // test the `trailing` drops and the internal tree offset
                Vector<Integer> actual = assertAreEqual(Vector.ofAll(all), drop, Vector::drop, expected);

                Vector<Integer> updated = Vector.range(0, actual.size())
                        .foldLeft(actual, (acc, i) -> acc.update(i, mapper.apply(acc.get(i))));

                assertAreEqual(updated, 0, (a, p) -> a, expected.map(mapper));
            }
        }
    }

    @Test
    public void shouldDrop() {
        dev.zazr.collection.List<Integer> expected = dev.zazr.collection.List.range(0, 2_000);
        Vector<Integer> actual = Vector.ofAll(expected);

        Vector.rangeClosed(0, expected.size()).foldLeft(actual, (actualSingleDrop, i) -> {
            dev.zazr.collection.List<Integer> expectedDrop = expected.drop(i);

            assertAreEqual(actual, i, Vector::drop, expectedDrop);
            assertAreEqual(actualSingleDrop, null, (a, p) -> a, expectedDrop);

            return actualSingleDrop.drop(1);
        });
    }

    @Test
    public void shouldDropRight() {
        dev.zazr.collection.List<Integer> expected = dev.zazr.collection.List.range(0, 2_000);
        Vector<Integer> actual = Vector.ofAll(expected);

        Vector.rangeClosed(0, expected.size()).foldLeft(actual, (actualSingleDrop, i) -> {
            dev.zazr.collection.List<Integer> expectedDrop = expected.dropRight(i);

            assertAreEqual(actual, i, Vector::dropRight, expectedDrop);
            assertAreEqual(actualSingleDrop, null, (a, p) -> a, expectedDrop);

            return actualSingleDrop.dropRight(1);
        });
    }

    @Test
    public void shouldSlice() {
        for (int length = 1, end = 500; length <= end; length++) {
            dev.zazr.collection.List<Integer> start = dev.zazr.collection.List.range(0, length);

            Vector.rangeClosed(0, start.size()).foldLeft(new Both<>(start, Vector.ofAll(start)), (both, i) -> {
                dev.zazr.collection.List<Integer> expected =
                        both.expected().slice(1, both.expected().size() - 1);
                return new Both<>(
                        expected, assertAreEqual(both.actual(), i, (a, p) -> a.slice(1, a.size() - 1), expected));
            });
        }
    }

    @Test
    public void shouldBehaveLikeArray() {
        Random random = new Random(13579);

        for (int i = 1; i < 10; i++) {
            Vector.range(0, 20_000)
                    .foldLeft(
                            new Both<Object>(dev.zazr.collection.List.empty(), Vector.empty()),
                            (both, j) -> randomSteps(random, both));
        }
    }

    // the expected list and the vector under test, whose elements must be equal
    private record Both<T>(dev.zazr.collection.List<T> expected, Vector<T> actual) {}

    // `both` after `onExpected` and `onActual`, checked and added to `history`
    private static <P> Both<Object> step(
            Both<Object> both,
            java.util.List<Both<Object>> history,
            Function<dev.zazr.collection.List<Object>, dev.zazr.collection.List<Object>> onExpected,
            P param,
            BiFunction<Vector<Object>, P, Vector<Object>> onActual) {
        dev.zazr.collection.List<Object> expected = onExpected.apply(both.expected());
        Both<Object> next = new Both<>(expected, assertAreEqual(both.actual(), param, onActual, expected));
        history.add(next);
        return next;
    }

    // a round of random operations on `start`; every pair reached is checked again at the end, which shows that the
    // operations are persistent
    private Both<Object> randomSteps(Random random, Both<Object> start) {
        java.util.List<Both<Object>> history = new java.util.ArrayList<>();

        Both<Object> reset = percent(random) < 20 ? fresh(random, history) : start;

        Both<Object> appended = percent(random) < 50 ? appendValue(random, reset, history) : reset;

        Both<Object> appendedAll = percent(random) < 10 ? appendValues(random, appended, history) : appended;

        Both<Object> prepended = percent(random) < 50 ? prependValue(random, appendedAll, history) : appendedAll;

        Both<Object> prependedAll = percent(random) < 10 ? prependValues(random, prepended, history) : prepended;

        Both<Object> dropped = percent(random) < 30 ? drop(random, prependedAll, history) : prependedAll;

        Both<Object> inserted = percent(random) < 10 ? insertValues(random, dropped, history) : dropped;

        Both<Object> taken = percent(random) < 30 ? take(random, inserted, history) : inserted;

        if (!taken.expected().isEmpty()) {
            assertThat(taken.actual().head()).isEqualTo(taken.expected().head());
            Assertions.assertThat(
                            new java.util.ArrayList<>(taken.actual().tail().asJava()))
                    .isEqualTo(new java.util.ArrayList<>(taken.expected().tail().asJava()));
            history.add(taken);
        }

        if (!taken.expected().isEmpty()) {
            int index = random.nextInt(taken.expected().size());
            assertThat(taken.actual().get(index)).isEqualTo(taken.expected().get(index));
            history.add(taken);
        }

        Both<Object> updated =
                percent(random) < 50 && !taken.expected().isEmpty() ? update(random, taken, history) : taken;

        Function<Object, Object> mapper = val -> (val instanceof Integer) ? ((Integer) val + 1) : val;
        Both<Object> mapped = percent(random) < 20
                ? step(updated, history, e -> e.map(mapper), null, (a, p) -> a.map(mapper))
                : updated;

        Predicate<Object> filter = val -> (String.valueOf(val).length() % 10) == 0;
        Both<Object> filtered = percent(random) < 30
                ? step(mapped, history, e -> e.filter(filter), null, (a, p) -> a.filter(filter))
                : mapped;

        Both<Object> sliced = percent(random) < 30
                ? Vector.range(0, 2)
                        .foldLeft(filtered, (acc, k) -> acc.expected().isEmpty() ? acc : slice(random, acc, history))
                : filtered;

        history.forEach(t -> assertAreEqual(t.expected(), t.actual())); // test that the modifications are persistent
        return sliced;
    }

    private Both<Object> fresh(Random random, java.util.List<Both<Object>> history) {
        dev.zazr.collection.List<Object> expected = dev.zazr.collection.List.ofAll(
                Vector.ofAll(randomValues(random, 100)).filter(v -> v instanceof Integer));
        Vector<Object> actual =
                (percent(random) < 30) ? Vector.narrow(Vector.ofAll(ints(expected))) : Vector.ofAll(expected);
        assertAreEqual(expected, actual);
        Both<Object> both = new Both<>(expected, actual);
        history.add(both);
        return both;
    }

    private Both<Object> appendValue(Random random, Both<Object> both, java.util.List<Both<Object>> history) {
        Object value = randomValue(random);
        return step(both, history, e -> e.append(value), value, Vector::append);
    }

    private Both<Object> appendValues(Random random, Both<Object> both, java.util.List<Both<Object>> history) {
        Iterable<Object> values = randomValues(random, random.nextInt(2 * WIDTH));
        dev.zazr.collection.List<Object> expected = both.expected().appendAll(values);
        Iterable<Object> given =
                (percent(random) < 50) ? Iterator.ofAll(values.iterator()) : values; /* not traversable again */
        return step(both, history, e -> expected, given, Vector::appendAll);
    }

    private Both<Object> prependValue(Random random, Both<Object> both, java.util.List<Both<Object>> history) {
        Object value = randomValue(random);
        return step(both, history, e -> e.prepend(value), value, Vector::prepend);
    }

    private Both<Object> prependValues(Random random, Both<Object> both, java.util.List<Both<Object>> history) {
        Iterable<Object> values = randomValues(random, random.nextInt(2 * WIDTH));
        dev.zazr.collection.List<Object> expected = both.expected().prependAll(values);
        Iterable<Object> given = (percent(random) < 50) ? Iterator.ofAll(values) : values; /* not traversable again */
        return step(both, history, e -> expected, given, Vector::prependAll);
    }

    private Both<Object> drop(Random random, Both<Object> both, java.util.List<Both<Object>> history) {
        int n = random.nextInt(both.expected().size() + 1);
        return step(both, history, e -> e.drop(n), n, Vector::drop);
    }

    private Both<Object> insertValues(Random random, Both<Object> both, java.util.List<Both<Object>> history) {
        int index = random.nextInt(both.expected().size() + 1);
        Iterable<Object> values = randomValues(random, random.nextInt(2 * WIDTH));
        dev.zazr.collection.List<Object> expected = both.expected().insertAll(index, values);
        Iterable<Object> given = (percent(random) < 50) ? Iterator.ofAll(values) : values; /* not traversable again */
        return step(both, history, e -> expected, given, (a, p) -> a.insertAll(index, p));
    }

    private Both<Object> take(Random random, Both<Object> both, java.util.List<Both<Object>> history) {
        int n = random.nextInt(both.expected().size() + 1);
        return step(both, history, e -> e.take(n), n, Vector::take);
    }

    private Both<Object> update(Random random, Both<Object> both, java.util.List<Both<Object>> history) {
        int index = random.nextInt(both.expected().size());
        Object value = randomValue(random);
        return step(both, history, e -> e.update(index, value), null, (a, p) -> a.update(index, value));
    }

    private Both<Object> slice(Random random, Both<Object> both, java.util.List<Both<Object>> history) {
        int to = random.nextInt(both.expected().size());
        int from = random.nextInt(to + 1);
        return step(both, history, e -> e.slice(from, to), null, (a, p) -> a.slice(from, to));
    }

    private int percent(Random random) {
        return random.nextInt(101);
    }

    private Iterable<Object> randomValues(Random random, int count) {
        Vector<Object> values = Vector.range(0, count).map(v -> randomValue(random));
        int percent = percent(random);
        if (percent < 30) {
            return new java.util.ArrayList<>(values.asJava()); /* not Traversable */
        } else {
            return values;
        }
    }

    private Object randomValue(Random random) {
        int percent = percent(random);
        if (percent < 10) {
            return "String";
        } else {
            return random.nextInt();
        }
    }

    private static <T extends Traversable<?>, P> T assertAreEqual(
            T previousActual, P param, BiFunction<T, P, T> actualProvider, Traversable<?> expected) {
        T actual = actualProvider.apply(previousActual, param);
        assertAreEqual(expected, actual);
        return actual; // makes debugging a lot easier, as the frame can be dropped and rerun on AssertError
    }

    private static void assertAreEqual(Traversable<?> expected, Traversable<?> actual) {
        java.util.List<?> actualList = new java.util.ArrayList<>(actual.asJava());
        java.util.List<?> expectedList = new java.util.ArrayList<>(expected.asJava());
        assertThat(actualList).isEqualTo(expectedList); // a lot faster than `hasSameElementsAs`
    }

    private static boolean[] booleans(dev.zazr.collection.List<?> values) {
        boolean[] array = new boolean[values.size()];
        java.util.Iterator<?> iterator = values.iterator();
        for (int i = 0; i < array.length; i++) {
            array[i] = (Boolean) iterator.next();
        }
        return array;
    }

    private static byte[] bytes(dev.zazr.collection.List<?> values) {
        byte[] array = new byte[values.size()];
        java.util.Iterator<?> iterator = values.iterator();
        for (int i = 0; i < array.length; i++) {
            array[i] = (Byte) iterator.next();
        }
        return array;
    }

    private static char[] chars(dev.zazr.collection.List<?> values) {
        char[] array = new char[values.size()];
        java.util.Iterator<?> iterator = values.iterator();
        for (int i = 0; i < array.length; i++) {
            array[i] = (Character) iterator.next();
        }
        return array;
    }

    private static double[] doubles(dev.zazr.collection.List<?> values) {
        double[] array = new double[values.size()];
        java.util.Iterator<?> iterator = values.iterator();
        for (int i = 0; i < array.length; i++) {
            array[i] = (Double) iterator.next();
        }
        return array;
    }

    private static float[] floats(dev.zazr.collection.List<?> values) {
        float[] array = new float[values.size()];
        java.util.Iterator<?> iterator = values.iterator();
        for (int i = 0; i < array.length; i++) {
            array[i] = (Float) iterator.next();
        }
        return array;
    }

    private static int[] ints(dev.zazr.collection.List<?> values) {
        int[] array = new int[values.size()];
        java.util.Iterator<?> iterator = values.iterator();
        for (int i = 0; i < array.length; i++) {
            array[i] = (Integer) iterator.next();
        }
        return array;
    }

    private static long[] longs(dev.zazr.collection.List<?> values) {
        long[] array = new long[values.size()];
        java.util.Iterator<?> iterator = values.iterator();
        for (int i = 0; i < array.length; i++) {
            array[i] = (Long) iterator.next();
        }
        return array;
    }

    private static short[] shorts(dev.zazr.collection.List<?> values) {
        short[] array = new short[values.size()];
        java.util.Iterator<?> iterator = values.iterator();
        for (int i = 0; i < array.length; i++) {
            array[i] = (Short) iterator.next();
        }
        return array;
    }
}
