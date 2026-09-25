package com.guizmaii.zazr.collection.internal;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ArrayTypeTest {

    static Stream<Object> primitiveArrays() {
        return Stream.of(
                new boolean[] { true },
                new byte[] { 1 },
                new char[] { 'a' },
                new double[] { 1.0 },
                new float[] { 1.0f },
                new int[] { 1 },
                new long[] { 1L },
                new short[] { 1 });
    }

    @ParameterizedTest
    @MethodSource("primitiveArrays")
    public void shouldGiveTheLengthOfAPrimitiveArrayAndZeroForNull(Object array) {
        final ArrayType<Object> type = ArrayType.of(array);
        assertThat(type.type()).isEqualTo(array.getClass().getComponentType());
        assertThat(type.lengthOf(array)).isEqualTo(1);
        assertThat(type.lengthOf(null)).isEqualTo(0);
    }

    @ParameterizedTest
    @MethodSource("primitiveArrays")
    public void shouldRejectNullInAPrimitiveArray(Object array) {
        final ArrayType<Object> type = ArrayType.of(array);
        final Object element = type.getAt(array, 0);
        assertThrows(ClassCastException.class, () -> type.setAt(array, 0, null));
        assertThat(type.getAt(array, 0)).isEqualTo(element);
    }
}
