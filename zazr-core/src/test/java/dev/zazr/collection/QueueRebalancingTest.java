package dev.zazr.collection;

import dev.zazr.Tuple2;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertSame;

/// A Queue about to be rebalanced keeps the result of its rebalancing: `tail`, `dequeue` and `init` called again on
/// that same older Queue return it, and every other version behaves as before.
public class QueueRebalancingTest {

    // one element at the front, the others at the back: tail() puts the back in order
    private static Queue<Integer> enqueued(int size) {
        Queue<Integer> none = Queue.empty();
        return List.range(0, size).foldLeft(none, (q, i) -> q.enqueue(i));
    }

    // every element at the front: init() moves the second half to the back
    private static Queue<Integer> ofAll(int size) {
        return Queue.ofAll(List.range(0, size));
    }

    @Test
    public void tailOfAnOlderQueueAboutToBeRebalancedIsComputedOnce() {
        Queue<Integer> queue = enqueued(5);
        Queue<Integer> first = queue.tail();
        assertThat(first).containsExactly(1, 2, 3, 4);
        assertSame(first, queue.tail());
        assertSame(first, queue.tailOption().get());
        assertThat(queue).containsExactly(0, 1, 2, 3, 4);
    }

    @Test
    public void dequeueOfAnOlderQueueAboutToBeRebalancedSharesTheKeptTail() {
        Queue<Integer> queue = enqueued(4);
        Tuple2<Integer, Queue<Integer>> first = queue.dequeue();
        Tuple2<Integer, Queue<Integer>> second = queue.dequeue();
        assertThat(first._1()).isEqualTo(0);
        assertThat(first._2()).containsExactly(1, 2, 3);
        assertSame(first._2(), second._2());
        assertSame(first._2(), queue.dequeueOption().get()._2());
        assertSame(first._2(), queue.tail());
    }

    @Test
    public void initOfAnOlderQueueAboutToBeRebalancedIsComputedOnce() {
        Queue<Integer> queue = ofAll(6);
        Queue<Integer> first = queue.init();
        assertThat(first).containsExactly(0, 1, 2, 3, 4);
        assertSame(first, queue.init());
        assertSame(first, queue.initOption().get());
        assertThat(queue).containsExactly(0, 1, 2, 3, 4, 5);
    }

    @Test
    public void initAfterTheKeptTailIsComputedFromTheQueueItself() {
        // the front holds one element and the rear is not empty: init() takes from the rear, tail() rebalances
        Queue<Integer> queue = enqueued(4);
        Queue<Integer> tail = queue.tail();
        Queue<Integer> init = queue.init();
        assertThat(init).containsExactly(0, 1, 2);
        assertThat(tail).containsExactly(1, 2, 3);
        assertSame(tail, queue.tail());
        assertThat(queue.init()).isNotSameAs(tail).containsExactly(0, 1, 2);
    }

    @Test
    public void tailAfterTheKeptInitIsComputedFromTheQueueItself() {
        // the rear is empty and the front holds several elements: tail() takes from the front, init() rebalances
        Queue<Integer> queue = ofAll(4);
        Queue<Integer> init = queue.init();
        Queue<Integer> tail = queue.tail();
        assertThat(tail).containsExactly(1, 2, 3);
        assertThat(init).containsExactly(0, 1, 2);
        assertSame(init, queue.init());
        assertThat(queue.tail()).isNotSameAs(init).containsExactly(1, 2, 3);
    }

    @Test
    public void aQueueThatDoesNotRebalanceKeepsNothing() {
        // tail() of a front of three and init() of a rear of one: nothing to put in order
        Queue<Integer> twoAtTheFront = ofAll(3);
        assertThat(twoAtTheFront.tail()).isNotSameAs(twoAtTheFront.tail()).containsExactly(1, 2);
        Queue<Integer> oneAtTheBack = Queue.of(0).enqueue(1);
        assertThat(oneAtTheBack.init()).isNotSameAs(oneAtTheBack.init()).containsExactly(0);
        assertThat(Queue.of(0).tail()).isEmpty();
        assertThat(Queue.of(0).init()).isEmpty();
    }

    @Test
    public void aNewVersionOfAnOlderQueueRebalancesItself() {
        Queue<Integer> queue = enqueued(4);
        Queue<Integer> kept = queue.tail();
        Queue<Integer> branch = queue.enqueue(9);
        assertThat(branch.tail()).containsExactly(1, 2, 3, 9);
        assertSame(branch.tail(), branch.tail());
        assertSame(kept, queue.tail());
        assertThat(kept).containsExactly(1, 2, 3);
        Queue<Integer> wide = ofAll(5);
        Queue<Integer> keptInit = wide.init();
        assertThat(wide.prepend(-1).init()).containsExactly(-1, 0, 1, 2, 3);
        assertSame(keptInit, wide.init());
    }

    @Test
    public void aChainOfTailsAndInitsOnKeptResultsStaysInOrder() {
        Queue<Integer> queue = enqueued(40);
        java.util.List<Integer> expected = new ArrayList<>(List.range(0, 40).asJava());
        @SuppressWarnings("Var")
        Queue<Integer> current = queue;
        for (int i = 0; !expected.isEmpty(); i++) {
            // every call is made twice on the same receiver, so the second one reads what the first one kept
            if (i % 3 == 0) {
                expected.remove(expected.size() - 1);
                current.init();
                current = current.init();
            } else {
                expected.remove(0);
                current.tail();
                current = current.tail();
            }
            assertThat(current).containsExactlyElementsOf(expected);
        }
        assertThat(queue).containsExactlyElementsOf(List.range(0, 40));
    }

    @Test
    public void threadsReadingTheKeptTailAllSeeAWholeQueue() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            for (int round = 0; round < 50; round++) {
                Queue<Integer> queue = enqueued(200);
                java.util.List<Future<Queue<Integer>>> results = new ArrayList<>();
                for (int t = 0; t < 4; t++) {
                    results.add(pool.submit(queue::tail));
                }
                for (Future<Queue<Integer>> result : results) {
                    assertThat(result.get()).containsExactlyElementsOf(List.range(1, 200));
                }
            }
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    public void randomOperationsOnOlderVersionsMatchAModel() {
        Random random = new Random(173);
        java.util.List<Queue<Integer>> versions = new ArrayList<>();
        java.util.List<java.util.List<Integer>> models = new ArrayList<>();
        versions.add(Queue.empty());
        models.add(new ArrayList<>());
        for (int step = 0; step < 4000; step++) {
            int pick = random.nextInt(versions.size());
            Queue<Integer> queue = versions.get(pick);
            java.util.List<Integer> model = new ArrayList<>(models.get(pick));
            int operation = random.nextInt(model.isEmpty() ? 4 : 9);
            Queue<Integer> next;
            switch (operation) {
                case 0, 1 -> {
                    next = queue.enqueue(step);
                    model.add(step);
                }
                case 2 -> {
                    next = queue.enqueueAll(List.of(step, -step));
                    model.add(step);
                    model.add(-step);
                }
                case 3 -> {
                    next = queue.prepend(step);
                    model.add(0, step);
                }
                case 4, 5 -> {
                    next = queue.tail();
                    model.remove(0);
                }
                case 6 -> {
                    Tuple2<Integer, Queue<Integer>> dequeued = queue.dequeue();
                    assertThat(dequeued._1()).isEqualTo(model.remove(0));
                    next = dequeued._2();
                }
                case 7 -> {
                    next = queue.init();
                    model.remove(model.size() - 1);
                }
                default -> {
                    next = queue.drop(1).append(step);
                    model.remove(0);
                    model.add(step);
                }
            }
            check(next, model);
            versions.add(next);
            models.add(model);
        }
        for (int i = 0; i < versions.size(); i++) {
            check(versions.get(i), models.get(i));
        }
    }

    private static void check(Queue<Integer> queue, java.util.List<Integer> model) {
        Queue<Integer> rebuilt = Queue.ofAll(model);
        assertThat(queue).containsExactlyElementsOf(model);
        assertThat(queue.size()).isEqualTo(model.size());
        assertThat(queue).isEqualTo(rebuilt);
        assertThat(queue.hashCode()).isEqualTo(rebuilt.hashCode());
        assertThat(queue.toString()).isEqualTo(rebuilt.toString());
        if (!model.isEmpty()) {
            assertThat(queue.head()).isEqualTo(model.get(0));
            assertThat(queue.last()).isEqualTo(model.get(model.size() - 1));
            assertThat(queue.tail()).containsExactlyElementsOf(model.subList(1, model.size()));
            assertThat(queue.init()).containsExactlyElementsOf(model.subList(0, model.size() - 1));
        }
    }
}
