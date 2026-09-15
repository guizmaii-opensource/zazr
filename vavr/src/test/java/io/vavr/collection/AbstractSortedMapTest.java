package io.vavr.collection;

import java.math.BigDecimal;
import java.util.Spliterator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public abstract class AbstractSortedMapTest extends AbstractMapTest {

    @Override
    abstract protected <K extends Comparable<? super K>, V> SortedMap<K, V> mapOf(K k1, V v1);

    @Nested
    class IsorderedTests {
        @Test
        public void shouldReturnOrdered() {
            final Map<Integer, String> actual = mapOf(1, "1", 1, "2");
            assertThat(actual.isOrdered()).isTrue();
        }
    }

    // -- narrow

    @Test
    public void shouldNarrowMap() {
        final SortedMap<Integer, Double> int2doubleMap = mapOf(1, 1.0d);
        final SortedMap<Integer, Number> number2numberMap = SortedMap.narrow(int2doubleMap);
        final int actual = number2numberMap.put(2, new BigDecimal("2.0")).values().sum().intValue();
        assertThat(actual).isEqualTo(3);
    }

    @Nested
    class SpliteratorTests {
        @Test
        public void shouldHaveSortedSpliterator() {
            assertThat(of(1, 2, 3).spliterator().hasCharacteristics(Spliterator.SORTED)).isTrue();
        }

        @Test
        public void shouldHaveOrderedSpliterator() {
            assertThat(of(1, 2, 3).spliterator().hasCharacteristics(Spliterator.ORDERED)).isTrue();
        }
    }

    @Nested
    class IssequentialTests {
        @Test
        public void shouldReturnFalseWhenIsSequentialCalled() {
            assertThat(of(1, 2, 3).isSequential()).isFalse();
        }
    }

}
