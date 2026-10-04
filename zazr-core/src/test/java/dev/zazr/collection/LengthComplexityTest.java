package dev.zazr.collection;

import dev.zazr.collection.internal.Iterator;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

/**
 * {@code List.Cons.size()} walks the list (a record has no field for a cached size), so every caller
 * must measure a length once, never per element. These costs are walks over the cells of a List or a Queue, which no
 * count can observe, so each case is timed: its workload is sized so that the slow path its comment names needs over a
 * minute and the fixed code well under a second, both measured with the same workload; the bound sits between the
 * two. The class runs alone, so that the bound does not measure the other test classes running in
 * parallel.
 */
@Isolated
public class LengthComplexityTest {

    private static final Duration WIDE_BOUND = Duration.ofSeconds(20);
    private static final int MILLION = 1_000_000;
    private static final int HALF = MILLION / 2;

    @Test
    public void shouldDropRightAndTakeRightOfListInLinearTime() {
        // dropRight and takeRight of half a List of 1,000,000: over a minute when the copy measures the List per
        // element, 75 ms fixed
        List<Integer> list = List.range(0, MILLION);
        assertTimeoutPreemptively(WIDE_BOUND, () -> {
            assertThat(list.dropRight(HALF).size()).isEqualTo(MILLION - HALF);
            assertThat(list.takeRight(HALF).size()).isEqualTo(HALF);
        });
    }

    @Test
    public void shouldDropRightAndTakeRightOfQueueInLinearTime() {
        // dropRight and takeRight of half a Queue of 1,000,000: over a minute when the copy measures the List per
        // element, 123 ms fixed
        Queue<Integer> front = Queue.ofAll(List.range(0, MILLION));
        Queue<Integer> rear = Queue.<Integer>empty().enqueueAll(List.range(0, MILLION));
        assertTimeoutPreemptively(WIDE_BOUND, () -> {
            assertThat(front.dropRight(HALF).size()).isEqualTo(MILLION - HALF);
            assertThat(front.takeRight(HALF).size()).isEqualTo(HALF);
            assertThat(rear.dropRight(HALF).size()).isEqualTo(MILLION - HALF);
        });
    }

    @Test
    public void shouldTakeRightAndDropRightOfIteratorInLinearTime() {
        // takeRight and dropRight of half of 1,000,000 elements: over a minute when the buffer is measured per element,
        // 268 ms fixed
        List<Integer> list = List.range(0, MILLION);
        assertTimeoutPreemptively(WIDE_BOUND, () -> {
            assertThat(Iterator.range(0, MILLION).takeRight(HALF).toVector().size())
                    .isEqualTo(HALF);
            assertThat(Iterator.range(0, MILLION).dropRight(HALF).toVector().size())
                    .isEqualTo(MILLION - HALF);
            assertThat(Iterator.ofAll(list).takeRight(HALF).toVector().size()).isEqualTo(HALF);
            assertThat(Iterator.ofAll(list).dropRight(HALF).toVector().size()).isEqualTo(MILLION - HALF);
        });
    }

    @Test
    public void shouldGetQueueElementsWithoutMeasuringTheFront() {
        // 100,000 reads near the front of a Queue of 1,000,000: over a minute when each read measures the front,
        // 666 ms fixed
        Queue<Integer> front = Queue.ofAll(List.range(0, MILLION));
        // an index held at the back costs O(n) by design, so this Queue is smaller and read once
        Queue<Integer> rear = Queue.<Integer>empty().enqueueAll(List.range(0, 100_000));
        assertTimeoutPreemptively(WIDE_BOUND, () -> {
            for (int round = 0; round < 100; round++) {
                for (int i = 0; i < 1_000; i++) {
                    assertThat(front.get(i)).isEqualTo(i);
                }
            }
            for (int i = 0; i < 1_000; i++) {
                assertThat(rear.get(i)).isEqualTo(i);
            }
        });
    }

    @Test
    public void shouldWalkOnlyThePrefixOfAList() {
        // 1,000 rounds of ten prefix operations on a List of 1,000,000: 84 s when each walks the whole List, 2 ms fixed
        List<Integer> list = List.range(0, MILLION);
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
        int size = 150_000;
        List<Integer> ones = List.fill(size, 1);
        List<Integer> range = List.range(0, size);
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
        Queue<Integer> queue = Queue.ofAll(List.range(0, MILLION)).enqueueAll(List.range(MILLION, 2 * MILLION));
        List<Integer> one = List.of(0);
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
        Queue<Integer> start = Queue.ofAll(List.range(0, MILLION));
        assertTimeoutPreemptively(WIDE_BOUND, () -> {
            Queue<Integer> queue = Vector.range(0, 6_000).foldLeft(start, (acc, i) -> acc.init());
            assertThat(queue.size()).isEqualTo(MILLION - 6_000);
            assertThat(queue.last()).isEqualTo(MILLION - 6_001);
        });
    }

    @Test
    public void shouldCountTheSizeOfAJavaListViewOnce() {
        // 2,500 size() calls on each of five views of 1,000,000 elements: 80 s when every call counts the sequence,
        // 59 ms fixed (one count per view)
        java.util.List<java.util.List<Integer>> views = java.util.List.of(
                List.range(0, MILLION).asJava(),
                Queue.ofAll(List.range(0, MILLION)).asJava(),
                Queue.<Integer>empty().enqueueAll(List.range(0, MILLION)).asJava(),
                LazyList.range(0, MILLION).asJava(),
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
