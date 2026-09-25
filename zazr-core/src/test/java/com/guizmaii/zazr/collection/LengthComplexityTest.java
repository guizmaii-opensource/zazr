package com.guizmaii.zazr.collection;

import com.guizmaii.zazr.collection.internal.Iterator;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

/**
 * {@code List.Cons.length()} walks the list (a record has no field for a cached length), so every caller
 * must measure a length once, never per element. Each case below is milliseconds when linear and times out
 * when quadratic. The class runs alone, so that the time bound does not measure the other test classes running in
 * parallel.
 */
@Isolated
public class LengthComplexityTest {

    private static final int N = 100_000;
    private static final int K = 50_000;
    private static final Duration BOUND = Duration.ofSeconds(5);

    @Test
    public void shouldDropRightAndTakeRightOfListInLinearTime() {
        assertTimeoutPreemptively(BOUND, () -> {
            assertThat(List.range(0, N).dropRight(K).length()).isEqualTo(N - K);
            assertThat(List.range(0, N).takeRight(K).length()).isEqualTo(K);
        });
    }

    @Test
    public void shouldDropRightAndTakeRightOfQueueInLinearTime() {
        assertTimeoutPreemptively(BOUND, () -> {
            assertThat(Queue.ofAll(List.range(0, N)).dropRight(K).length()).isEqualTo(N - K);
            assertThat(Queue.ofAll(List.range(0, N)).takeRight(K).length()).isEqualTo(K);
            assertThat(Queue.<Integer>empty().enqueueAll(List.range(0, N)).dropRight(K).length()).isEqualTo(N - K);
        });
    }

    @Test
    public void shouldTakeRightAndDropRightOfIteratorInLinearTime() {
        assertTimeoutPreemptively(BOUND, () -> {
            assertThat(Iterator.range(0, N).takeRight(K).toVector().size()).isEqualTo(K);
            assertThat(Iterator.range(0, N).dropRight(K).toVector().size()).isEqualTo(N - K);
            assertThat(Iterator.ofAll(List.range(0, N)).takeRight(K).toVector().size()).isEqualTo(K);
            assertThat(Iterator.ofAll(List.range(0, N)).dropRight(K).toVector().size()).isEqualTo(N - K);
        });
    }

    @Test
    public void shouldGetQueueElementsWithoutMeasuringTheFront() {
        assertTimeoutPreemptively(BOUND, () -> {
            final Queue<Integer> front = Queue.ofAll(List.range(0, N));
            final Queue<Integer> rear = Queue.<Integer>empty().enqueueAll(List.range(0, N));
            for (int i = 0; i < 1_000; i++) {
                assertThat(front.get(i)).isEqualTo(i);
                assertThat(rear.get(i)).isEqualTo(i);
            }
        });
    }

}
