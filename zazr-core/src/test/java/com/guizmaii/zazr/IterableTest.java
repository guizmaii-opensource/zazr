package com.guizmaii.zazr;

import com.guizmaii.zazr.collection.List;
import com.guizmaii.zazr.collection.Queue;
import com.guizmaii.zazr.collection.Stream;
import com.guizmaii.zazr.collection.Vector;
import com.guizmaii.zazr.control.Option;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class IterableTest {

    @Nested
    class EqTests {
        @Test
        public void shouldEqNoneAndEmptyList() {
            assertThat(Option.none().eq(List.empty())).isTrue();
            assertThat(Option.none().eq(List.of(1))).isFalse();
        }

        @Test
        public void shouldEqSomeAndNonEmptyList() {
            assertThat(Option.some(1).eq(List.of(1))).isTrue();
            assertThat(Option.some(1).eq(List.of(2))).isFalse();
            assertThat(Option.some(1).eq(List.empty())).isFalse();
        }

        @Test
        public void shouldEqIterableAndJavaIterable() {
            assertThat(List.of(1, 2, 3).eq(Arrays.asList(1, 2, 3))).isTrue();
        }

        @Test
        public void shouldEqNestedIterables() {
            // ((1, 2), ((3)))
            final Value<?> i1 = List.of(List.of(1, 2), Collections.singletonList(List.of(3)));
            final Value<?> i2 = Queue.of(Stream.of(1, 2), List.of(Vector.of(3)));
            final Value<?> i3 = Queue.of(Stream.of(1, 2), List.of(List.of()));
            assertThat(i1.eq(i2)).isTrue();
            assertThat(i1.eq(i3)).isFalse();
        }
    }

}
