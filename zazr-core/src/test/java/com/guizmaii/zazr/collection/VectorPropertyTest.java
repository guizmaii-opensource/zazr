package com.guizmaii.zazr.collection;

import com.guizmaii.zazr.Tuple;
import com.guizmaii.zazr.Tuple2;
import com.guizmaii.zazr.collection.internal.Iterator;
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
            final com.guizmaii.zazr.collection.List<Integer> expected = com.guizmaii.zazr.collection.List.range(0, i);
            final Vector<Integer> actual = Vector.ofAll(expected);
            for (int j = 0; j < actual.size(); j++) {
                assertThat(expected.get(j)).isEqualTo(actual.get(j));
            }

            /* boolean */
            final com.guizmaii.zazr.collection.List<Boolean> expectedBoolean = expected.map(v -> v > 0);
            final Vector<Boolean> actualBoolean = Vector.ofAll(booleans(expectedBoolean));
            assertAreEqual(expectedBoolean, actualBoolean);

            /* byte */
            final com.guizmaii.zazr.collection.List<Byte> expectedByte = expected.map(Integer::byteValue);
            final Vector<Byte> actualByte = Vector.ofAll(bytes(expectedByte));
            assertAreEqual(expectedByte, actualByte);

            /* char */
            final com.guizmaii.zazr.collection.List<Character> expectedChar = expected.map(v -> (char) v.intValue());
            final Vector<Character> actualChar = Vector.ofAll(chars(expectedChar));
            assertAreEqual(expectedChar, actualChar);

            /* double */
            final com.guizmaii.zazr.collection.List<Double> expectedDouble = expected.map(Integer::doubleValue);
            final Vector<Double> actualDouble = Vector.ofAll(doubles(expectedDouble));
            assertAreEqual(expectedDouble, actualDouble);

            /* float */
            final com.guizmaii.zazr.collection.List<Float> expectedFloat = expected.map(Integer::floatValue);
            final Vector<Float> actualFloat = Vector.ofAll(floats(expectedFloat));
            assertAreEqual(expectedFloat, actualFloat);

            /* int */
            final Vector<Integer> actualInt = Vector.ofAll(ints(expected));
            assertAreEqual(expected, actualInt);

            /* long */
            final com.guizmaii.zazr.collection.List<Long> expectedLong = expected.map(Integer::longValue);
            final Vector<Long> actualLong = Vector.ofAll(longs(expectedLong));
            assertAreEqual(expectedLong, actualLong);

            /* short */
            final com.guizmaii.zazr.collection.List<Short> expectedShort = expected.map(Integer::shortValue);
            final Vector<Short> actualShort = Vector.ofAll(shorts(expectedShort));
            assertAreEqual(expectedShort, actualShort);
        }
    }

    @Test
    public void shouldIterate() {
        for (byte depth = 0; depth <= 2; depth++) {
            for (int i = 0; i < 5000; i++) {
                final com.guizmaii.zazr.collection.List<Integer> expected = com.guizmaii.zazr.collection.List.range(0, i);
                final Vector<Integer> actual = Vector.ofAll(expected);
                assertAreEqual(actual, expected);
            }
        }

        com.guizmaii.zazr.collection.List<Integer> expected = com.guizmaii.zazr.collection.List.range(0, 1000);
        Vector<Integer> actual = Vector.ofAll(ints(expected));
        for (int drop = 0; drop <= (WIDTH + 1); drop++) {
            assertAreEqual(actual, expected);

            expected = expected.tail().init();
            actual = actual.tail().init();
        }
    }

    @Test
    public void shouldPrepend() {
        com.guizmaii.zazr.collection.List<Integer> expected = com.guizmaii.zazr.collection.List.empty();
        Vector<Integer> actual = Vector.empty();

        for (int drop = 0; drop <= (WIDTH + 1); drop++) {
            for (Integer value : Iterator.range(0, 1000)) {
                expected = expected.drop(drop);
                actual = assertAreEqual(actual, drop, Vector::drop, expected);

                expected = expected.prepend(value);
                actual = assertAreEqual(actual, value, Vector::prepend, expected);
            }
        }
    }

    @Test
    public void shouldAppend() {
        com.guizmaii.zazr.collection.List<Integer> expected = com.guizmaii.zazr.collection.List.empty();
        Vector<Integer> actual = Vector.empty();

        for (int drop = 0; drop <= (WIDTH + 1); drop++) {
            for (Integer value : Iterator.range(0, 500)) {
                expected = expected.drop(drop);
                actual = assertAreEqual(actual, drop, Vector::drop, expected);

                expected = expected.append(value);
                actual = assertAreEqual(actual, value, Vector::append, expected);
            }
        }
    }

    @Test
    public void shouldUpdate() {
        final Function<Integer, Integer> mapper = i -> i + 1;

        for (byte depth = 0; depth <= 2; depth++) {
            final int length = 10_000;

            for (int drop = 0; drop <= (WIDTH + 1); drop++) {
                com.guizmaii.zazr.collection.List<Integer> expected = com.guizmaii.zazr.collection.List.range(0, length);
                Vector<Integer> actual = Vector.ofAll(expected);

                expected = expected.drop(drop); // test the `trailing` drops and the internal tree offset
                actual = assertAreEqual(actual, drop, Vector::drop, expected);

                for (int i = 0; i < actual.size(); i++) {
                    final Integer newValue = mapper.apply(actual.get(i));
                    actual = actual.update(i, newValue);
                }

                assertAreEqual(actual, 0, (a, p) -> a, expected.map(mapper));
            }
        }
    }

    @Test
    public void shouldDrop() {
        final com.guizmaii.zazr.collection.List<Integer> expected = com.guizmaii.zazr.collection.List.range(0, 2_000);
        final Vector<Integer> actual = Vector.ofAll(expected);

        Vector<Integer> actualSingleDrop = actual;
        for (int i = 0; i <= expected.size(); i++) {
            final com.guizmaii.zazr.collection.List<Integer> expectedDrop = expected.drop(i);

            assertAreEqual(actual, i, Vector::drop, expectedDrop);
            assertAreEqual(actualSingleDrop, null, (a, p) -> a, expectedDrop);

            actualSingleDrop = actualSingleDrop.drop(1);
        }
    }

    @Test
    public void shouldDropRight() {
        final com.guizmaii.zazr.collection.List<Integer> expected = com.guizmaii.zazr.collection.List.range(0, 2_000);
        final Vector<Integer> actual = Vector.ofAll(expected);

        Vector<Integer> actualSingleDrop = actual;
        for (int i = 0; i <= expected.size(); i++) {
            final com.guizmaii.zazr.collection.List<Integer> expectedDrop = expected.dropRight(i);

            assertAreEqual(actual, i, Vector::dropRight, expectedDrop);
            assertAreEqual(actualSingleDrop, null, (a, p) -> a, expectedDrop);

            actualSingleDrop = actualSingleDrop.dropRight(1);
        }
    }

    @Test
    public void shouldSlice() {
        for (int length = 1, end = 500; length <= end; length++) {
            com.guizmaii.zazr.collection.List<Integer> expected = com.guizmaii.zazr.collection.List.range(0, length);
            Vector<Integer> actual = Vector.ofAll(expected);

            for (int i = 0; i <= expected.size(); i++) {
                expected = expected.slice(1, expected.size() - 1);
                actual = assertAreEqual(actual, i, (a, p) -> a.slice(1, a.size() - 1), expected);
            }
        }
    }

    @Test
    public void shouldBehaveLikeArray() {
        final Random random = new Random(13579);

        for (int i = 1; i < 10; i++) {
            com.guizmaii.zazr.collection.List<Object> expected = com.guizmaii.zazr.collection.List.empty();
            Vector<Object> actual = Vector.empty();
            for (int j = 0; j < 20_000; j++) {
                com.guizmaii.zazr.collection.List<Tuple2<com.guizmaii.zazr.collection.List<Object>, Vector<Object>>> history = com.guizmaii.zazr.collection.List.empty();

                if (percent(random) < 20) {
                    expected = com.guizmaii.zazr.collection.List.ofAll(Vector.ofAll(randomValues(random, 100)).filter(v -> v instanceof Integer));
                    actual = (percent(random) < 30) ? Vector.narrow(Vector.ofAll(ints(expected))) : Vector.ofAll(expected);
                    assertAreEqual(expected, actual);
                    history = history.append(Tuple.of(expected, actual));
                }

                if (percent(random) < 50) {
                    final Object value = randomValue(random);
                    expected = expected.append(value);
                    actual = assertAreEqual(actual, value, Vector::append, expected);
                    history = history.append(Tuple.of(expected, actual));
                }
                if (percent(random) < 10) {
                    Iterable<Object> values = randomValues(random, random.nextInt(2 * WIDTH));
                    expected = expected.appendAll(values);

                    values = (percent(random) < 50) ? Iterator.ofAll(values.iterator()) : values;  /* not traversable again */
                    actual = assertAreEqual(actual, values, Vector::appendAll, expected);
                    history = history.append(Tuple.of(expected, actual));
                }

                if (percent(random) < 50) {
                    final Object value = randomValue(random);
                    expected = expected.prepend(value);
                    actual = assertAreEqual(actual, value, Vector::prepend, expected);
                    history = history.append(Tuple.of(expected, actual));
                }
                if (percent(random) < 10) {
                    Iterable<Object> values = randomValues(random, random.nextInt(2 * WIDTH));
                    expected = expected.prependAll(values);

                    values = (percent(random) < 50) ? Iterator.ofAll(values) : values;  /* not traversable again */
                    actual = assertAreEqual(actual, values, Vector::prependAll, expected);
                    history = history.append(Tuple.of(expected, actual));
                }

                if (percent(random) < 30) {
                    final int n = random.nextInt(expected.size() + 1);
                    expected = expected.drop(n);
                    actual = assertAreEqual(actual, n, Vector::drop, expected);
                    history = history.append(Tuple.of(expected, actual));
                }

                if (percent(random) < 10) {
                    final int index = random.nextInt(expected.size() + 1);
                    Iterable<Object> values = randomValues(random, random.nextInt(2 * WIDTH));
                    expected = expected.insertAll(index, values);

                    values = (percent(random) < 50) ? Iterator.ofAll(values) : values;  /* not traversable again */
                    actual = assertAreEqual(actual, values, (a, p) -> a.insertAll(index, p), expected);
                    history = history.append(Tuple.of(expected, actual));
                }

                if (percent(random) < 30) {
                    final int n = random.nextInt(expected.size() + 1);
                    expected = expected.take(n);
                    actual = assertAreEqual(actual, n, Vector::take, expected);
                    history = history.append(Tuple.of(expected, actual));
                }

                if (!expected.isEmpty()) {
                    assertThat(actual.head()).isEqualTo(expected.head());
                    Assertions.assertThat(new java.util.ArrayList<>(actual.tail().asJava())).isEqualTo(new java.util.ArrayList<>(expected.tail().asJava()));
                    history = history.append(Tuple.of(expected, actual));
                }

                if (!expected.isEmpty()) {
                    final int index = random.nextInt(expected.size());
                    assertThat(actual.get(index)).isEqualTo(expected.get(index));
                    history = history.append(Tuple.of(expected, actual));
                }

                if (percent(random) < 50) {
                    if (!expected.isEmpty()) {
                        final int index = random.nextInt(expected.size());
                        final Object value = randomValue(random);
                        expected = expected.update(index, value);
                        actual = assertAreEqual(actual, null, (a, p) -> a.update(index, value), expected);
                        history = history.append(Tuple.of(expected, actual));
                    }
                }

                if (percent(random) < 20) {
                    final Function<Object, Object> mapper = val -> (val instanceof Integer) ? ((Integer) val + 1) : val;
                    expected = expected.map(mapper);
                    actual = assertAreEqual(actual, null, (a, p) -> a.map(mapper), expected);
                    history = history.append(Tuple.of(expected, actual));
                }

                if (percent(random) < 30) {
                    final Predicate<Object> filter = val -> (String.valueOf(val).length() % 10) == 0;
                    expected = expected.filter(filter);
                    actual = assertAreEqual(actual, null, (a, p) -> a.filter(filter), expected);
                    history = history.append(Tuple.of(expected, actual));
                }

                if (percent(random) < 30) {
                    for (int k = 0; k < 2; k++) {
                        if (!expected.isEmpty()) {
                            final int to = random.nextInt(expected.size());
                            final int from = random.nextInt(to + 1);
                            expected = expected.slice(from, to);
                            actual = assertAreEqual(actual, null, (a, p) -> a.slice(from, to), expected);
                            history = history.append(Tuple.of(expected, actual));
                        }
                    }
                }

                history.forEach(t -> assertAreEqual(t._1(), t._2())); // test that the modifications are persistent
            }
        }
    }

    private int percent(Random random) { return random.nextInt(101); }
    private Iterable<Object> randomValues(Random random, int count) {
        final Vector<Object> values = Vector.range(0, count).map(v -> randomValue(random));
        final int percent = percent(random);
        if (percent < 30) {
            return new java.util.ArrayList<>(values.asJava());  /* not Traversable */
        } else {
            return values;
        }
    }
    private Object randomValue(Random random) {
        final int percent = percent(random);
        if (percent < 10) {
            return "String";
        } else {
            return random.nextInt();
        }
    }

    private static <T extends Traversable<?>, P> T assertAreEqual(T previousActual, P param, BiFunction<T, P, T> actualProvider, Traversable<?> expected) {
        final T actual = actualProvider.apply(previousActual, param);
        assertAreEqual(expected, actual);
        return actual; // makes debugging a lot easier, as the frame can be dropped and rerun on AssertError
    }

    private static void assertAreEqual(Traversable<?> expected, Traversable<?> actual) {
        final java.util.List<?> actualList = new java.util.ArrayList<>(actual.asJava());
        final java.util.List<?> expectedList = new java.util.ArrayList<>(expected.asJava());
        assertThat(actualList).isEqualTo(expectedList); // a lot faster than `hasSameElementsAs`
    }

    private static boolean[] booleans(com.guizmaii.zazr.collection.List<?> values) {
        final boolean[] array = new boolean[values.size()];
        int i = 0;
        for (Object value : values) {
            array[i++] = (Boolean) value;
        }
        return array;
    }

    private static byte[] bytes(com.guizmaii.zazr.collection.List<?> values) {
        final byte[] array = new byte[values.size()];
        int i = 0;
        for (Object value : values) {
            array[i++] = (Byte) value;
        }
        return array;
    }

    private static char[] chars(com.guizmaii.zazr.collection.List<?> values) {
        final char[] array = new char[values.size()];
        int i = 0;
        for (Object value : values) {
            array[i++] = (Character) value;
        }
        return array;
    }

    private static double[] doubles(com.guizmaii.zazr.collection.List<?> values) {
        final double[] array = new double[values.size()];
        int i = 0;
        for (Object value : values) {
            array[i++] = (Double) value;
        }
        return array;
    }

    private static float[] floats(com.guizmaii.zazr.collection.List<?> values) {
        final float[] array = new float[values.size()];
        int i = 0;
        for (Object value : values) {
            array[i++] = (Float) value;
        }
        return array;
    }

    private static int[] ints(com.guizmaii.zazr.collection.List<?> values) {
        final int[] array = new int[values.size()];
        int i = 0;
        for (Object value : values) {
            array[i++] = (Integer) value;
        }
        return array;
    }

    private static long[] longs(com.guizmaii.zazr.collection.List<?> values) {
        final long[] array = new long[values.size()];
        int i = 0;
        for (Object value : values) {
            array[i++] = (Long) value;
        }
        return array;
    }

    private static short[] shorts(com.guizmaii.zazr.collection.List<?> values) {
        final short[] array = new short[values.size()];
        int i = 0;
        for (Object value : values) {
            array[i++] = (Short) value;
        }
        return array;
    }
}
