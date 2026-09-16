package com.guizmaii.zazr.collection;

import java.math.BigDecimal;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public abstract class AbstractLinearSeqTest extends AbstractSeqTest {

    @Override
    abstract protected <T> LinearSeq<T> of(T element);

    @Nested
    class LinearSeqStaticNarrowTests {
        @Test
        public void shouldNarrowIndexedSeq() {
            final LinearSeq<Double> doubles = of(1.0d);
            final LinearSeq<Number> numbers = LinearSeq.narrow(doubles);
            final int actual = numbers.append(new BigDecimal("2.0")).sum().intValue();
            assertThat(actual).isEqualTo(3);
        }
    }
}
