package com.guizmaii.zazr.collection.internal;

import java.util.Comparator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.guizmaii.zazr.collection.internal.Comparators.naturalComparator;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ComparatorsTest {

    @Nested
    class NaturalcomparatorTests {
        @Test
        public void shouldCompareTwoIntegersUsingNaturalOrder() {
            final Comparator<Integer> comparator = naturalComparator();
            assertThat(comparator.compare(0, 1)).isEqualTo(-1);
            assertThat(comparator.compare(2, -1)).isEqualTo(1);
            assertThat(comparator.compare(3, 3)).isEqualTo(0);
        }

        @Test
        public void shouldThrowNPEWhenComparingNullAndIntegerUsingNaturalOrder() {
            assertThrows(NullPointerException.class, () -> naturalComparator().compare(null, 1));
        }

        @Test
        public void shouldThrowNPEWhenComparingIntegerAndNullUsingNaturalOrder() {
            assertThrows(NullPointerException.class, () -> naturalComparator().compare(1, null));
        }
    }
}
