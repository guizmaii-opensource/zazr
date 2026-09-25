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
            assertThat(List.range(0, N).dropRight(K).size()).isEqualTo(N - K);
            assertThat(List.range(0, N).takeRight(K).size()).isEqualTo(K);
        });
    }

    @Test
    public void shouldDropRightAndTakeRightOfQueueInLinearTime() {
        assertTimeoutPreemptively(BOUND, () -> {
            assertThat(Queue.ofAll(List.range(0, N)).dropRight(K).size()).isEqualTo(N - K);
            assertThat(Queue.ofAll(List.range(0, N)).takeRight(K).size()).isEqualTo(K);
            assertThat(Queue.<Integer>empty().enqueueAll(List.range(0, N)).dropRight(K).size()).isEqualTo(N - K);
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

    // The tests below pin costs that no count can observe (walking cells of a List or a Queue). Each workload is
    // sized so that the code before the fix needs over a minute on the maintainer's machine and the fixed code well
    // under a second, both measured there with the same workload outside JUnit; the bound sits between the two.

    private static final Duration WIDE_BOUND = Duration.ofSeconds(20);
    private static final int MILLION = 1_000_000;

    @Test
    public void shouldWalkOnlyThePrefixOfAList() {
        // 1,000 rounds of ten prefix operations on a List of 1,000,000: 84 s when each walks the whole List, 2 ms fixed
        final List<Integer> list = List.range(0, MILLION);
        assertTimeoutPreemptively(WIDE_BOUND, () -> {
            for (int i = 0; i < 1_000; i++) {
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
        // two lastIndexOfSlice and one combinations(1) on 150,000 elements: 99 s when quadratic, 22 ms fixed
        final int size = 150_000;
        final List<Integer> ones = List.fill(size, 1);
        final List<Integer> range = List.range(0, size);
        assertTimeoutPreemptively(WIDE_BOUND, () -> {
            assertThat(ones.lastIndexOfSlice(List.of(1))).isEqualTo(size - 1);
            assertThat(ones.lastIndexOfSlice(List.of(1), size / 2)).isEqualTo(size / 2);
            assertThat(range.combinations(1).size()).isEqualTo(size);
        });
    }

    @Test
    public void shouldWalkOnlyTheFrontOfAQueue() {
        // 100 rounds of six prefix reads on a Queue of 2,000,000 (half in the rear): 81 s when each copies the Queue
        // first, 11 ms fixed
        final Queue<Integer> queue = Queue.ofAll(List.range(0, MILLION)).enqueueAll(List.range(MILLION, 2 * MILLION));
        final List<Integer> one = List.of(0);
        assertTimeoutPreemptively(WIDE_BOUND, () -> {
            for (int i = 0; i < 100; i++) {
                assertThat(queue.startsWith(one)).isTrue();
                assertThat(queue.startsWith(one, 0)).isTrue();
                assertThat(queue.zip(one).size()).isEqualTo(1);
                assertThat(queue.zipWith(one, Integer::sum).head()).isEqualTo(0);
                assertThat(queue.prefixLength(x -> false)).isEqualTo(0);
                assertThat(queue.segmentLength(x -> false, 0)).isEqualTo(0);
            }
        });
    }

    @Test
    public void shouldTakeChainedInitsOfAQueueFromTheRear() {
        // 6,000 chained init() on a Queue of 1,000,000 with an empty rear: 80 s when each copies the front, 16 ms
        // fixed (one split, then O(1) each)
        final Queue<Integer> start = Queue.ofAll(List.range(0, MILLION));
        assertTimeoutPreemptively(WIDE_BOUND, () -> {
            Queue<Integer> queue = start;
            for (int i = 0; i < 6_000; i++) {
                queue = queue.init();
            }
            assertThat(queue.size()).isEqualTo(MILLION - 6_000);
            assertThat(queue.last()).isEqualTo(MILLION - 6_001);
        });
    }

    @Test
    public void shouldCountTheSizeOfAJavaListViewOnce() {
        // 2,500 size() calls on each of five views of 1,000,000 elements: 80 s when every call counts the sequence,
        // 59 ms fixed (one count per view)
        final java.util.List<java.util.List<Integer>> views = java.util.List.of(
                List.range(0, MILLION).asJava(),
                Queue.ofAll(List.range(0, MILLION)).asJava(),
                Queue.<Integer> empty().enqueueAll(List.range(0, MILLION)).asJava(),
                Stream.range(0, MILLION).asJava(),
                List.range(0, MILLION).asJava().reversed());
        assertTimeoutPreemptively(WIDE_BOUND, () -> {
            for (java.util.List<Integer> view : views) {
                for (int i = 0; i < 2_500; i++) {
                    assertThat(view.size()).isEqualTo(MILLION);
                }
            }
        });
    }

}
