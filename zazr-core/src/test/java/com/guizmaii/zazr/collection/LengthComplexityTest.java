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

    @Test
    public void shouldWalkOnlyThePrefixOfAList() {
        // K calls on a List of N elements: milliseconds when each call walks a few cells, far past the bound when
        // each call walks the whole List
        final List<Integer> list = List.range(0, N);
        assertTimeoutPreemptively(BOUND, () -> {
            for (int i = 0; i < K; i++) {
                assertThat(list.take(1).head()).isEqualTo(0);
                assertThat(list.drop(1).head()).isEqualTo(1);
                assertThat(list.takeWhile(x -> x < 1).head()).isEqualTo(0);
                assertThat(list.takeUntil(x -> x >= 1).head()).isEqualTo(0);
                assertThat(list.slice(0, 1).head()).isEqualTo(0);
                assertThat(list.subSequence(1).head()).isEqualTo(1);
                assertThat(list.subSequence(0, 1).head()).isEqualTo(0);
                assertThat(list.remove(0).head()).isEqualTo(1);
                assertThat(list.leftPadTo(2, -1)).isSameAs(list);
                assertThat(list.segmentLength(x -> x < 2, 1)).isEqualTo(1);
            }
        });
    }

    @Test
    public void shouldFindTheLastSliceAndTheCombinationsOfAListInLinearTime() {
        assertTimeoutPreemptively(BOUND, () -> {
            final List<Integer> ones = List.fill(2 * N, 1);
            assertThat(ones.lastIndexOfSlice(List.of(1))).isEqualTo(2 * N - 1);
            assertThat(ones.lastIndexOfSlice(List.of(1), N)).isEqualTo(N);
            assertThat(List.range(0, N).combinations(1).length()).isEqualTo(N);
        });
    }

    @Test
    public void shouldWalkOnlyTheFrontOfAQueue() {
        // K calls on a Queue of 2N elements: milliseconds when each call reads the first elements, far past the bound
        // when each call copies the whole Queue first
        final Queue<Integer> queue = Queue.ofAll(List.range(0, N)).enqueueAll(List.range(N, 2 * N));
        final List<Integer> one = List.of(0);
        assertTimeoutPreemptively(BOUND, () -> {
            for (int i = 0; i < K; i++) {
                assertThat(queue.startsWith(one)).isTrue();
                assertThat(queue.startsWith(one, 0)).isTrue();
                assertThat(queue.zip(one).length()).isEqualTo(1);
                assertThat(queue.zipWith(one, Integer::sum).head()).isEqualTo(0);
                assertThat(queue.prefixLength(x -> false)).isEqualTo(0);
                assertThat(queue.segmentLength(x -> false, 0)).isEqualTo(0);
            }
        });
    }

}
