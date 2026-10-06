package dev.zazr.collection;

import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// `combinations`, `permutations`, `rotateRight` and the iterator of `Queue`, compared with their recursive
/// definitions ([ArrangementModels]) on many inputs: the same results in the same order, holding the same element
/// objects, and on a LazyList the same cells read at every step.
class ArrangementsTest {

    private static final long SEED = 20261006L;

    /// An element equal to any other of the same value, so that equal elements are told apart by identity.
    static final class Box {
        final int value;

        Box(int value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Box other && other.value == value;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(value);
        }

        @Override
        public String toString() {
            return "Box(" + value + ")@" + Integer.toHexString(System.identityHashCode(this));
        }
    }

    // n boxes of values below `distinct`: duplicates whenever distinct < n
    private static java.util.List<Box> boxes(Random random, int n, int distinct) {
        java.util.List<Box> result = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            result.add(new Box(random.nextInt(distinct)));
        }
        return result;
    }

    // the sources of a test: every size up to `maxSize`, with all-distinct, some-equal and all-equal elements
    private static java.util.List<java.util.List<Box>> sources(int maxSize, int perShape) {
        Random random = new Random(SEED);
        java.util.List<java.util.List<Box>> result = new ArrayList<>();
        for (int n = 0; n <= maxSize; n++) {
            for (int i = 0; i < perShape; i++) {
                result.add(boxes(random, n, Integer.MAX_VALUE));
                result.add(boxes(random, n, Math.max(1, n / 2)));
                result.add(boxes(random, n, 2));
                result.add(boxes(random, n, 1));
            }
        }
        return result;
    }

    // the sizes of combination asked for a source of n elements: out of range on both sides, and every one in between
    private static int[] ks(int n) {
        int[] result = new int[n + 7];
        result[0] = Integer.MIN_VALUE;
        result[1] = -1;
        result[2] = Integer.MAX_VALUE;
        result[3] = n + 1;
        result[4] = n + 2;
        for (int k = 0; k <= n + 1; k++) {
            result[5 + k] = k;
        }
        return result;
    }

    // asserts the same elements in the same order, the same objects
    private static void assertSameObjects(Iterable<?> expected, Iterable<?> actual) {
        java.util.List<Object> e = new ArrayList<>();
        expected.forEach(e::add);
        java.util.List<Object> a = new ArrayList<>();
        actual.forEach(a::add);
        assertThat(a).hasSameSizeAs(e);
        for (int i = 0; i < e.size(); i++) {
            assertThat(a.get(i)).as("element %d", i).isSameAs(e.get(i));
        }
    }

    // asserts the same collections in the same order, each holding the same objects
    private static void assertSameNested(
            Iterable<? extends Iterable<?>> expected, Iterable<? extends Iterable<?>> actual) {
        java.util.List<Iterable<?>> e = new ArrayList<>();
        expected.forEach(e::add);
        java.util.List<Iterable<?>> a = new ArrayList<>();
        actual.forEach(a::add);
        assertThat(a).hasSameSizeAs(e);
        for (int i = 0; i < e.size(); i++) {
            assertSameObjects(e.get(i), a.get(i));
        }
    }

    // -- combinations

    @Test
    void combinationsOfKAreThoseOfTheDefinition() {
        for (java.util.List<Box> source : sources(9, 2)) {
            Vector<Box> vector = Vector.ofAll(source);
            List<Box> list = List.ofAll(source);
            LazyList<Box> lazy = LazyList.ofAll(source);
            for (int k : ks(source.size())) {
                int kk = Math.max(k, 0);
                Vector<Vector<Box>> expectedVector = ArrangementModels.combinations(vector, kk);
                assertSameNested(expectedVector, vector.combinations(k));
                assertThat(vector.combinations(k)).isEqualTo(expectedVector);
                assertSameNested(ArrangementModels.combinations(list, kk), list.combinations(k));
                assertSameNested(ArrangementModels.combinations(lazy, kk), lazy.combinations(k));
                assertSameNested(expectedVector, Queue.ofAll(source).combinations(k));
                if (!source.isEmpty()) {
                    assertSameNested(
                            expectedVector,
                            NonEmptyVector.unsafeFromVector(vector).combinations(k));
                }
            }
        }
    }

    @Test
    void allCombinationsAreThoseOfTheDefinition() {
        for (java.util.List<Box> source : sources(9, 1)) {
            Vector<Box> vector = Vector.ofAll(source);
            Vector<Vector<Box>> expected =
                    Vector.rangeClosed(0, source.size()).flatMap(k -> ArrangementModels.combinations(vector, k));
            assertSameNested(expected, vector.combinations());
            assertSameNested(expected, List.ofAll(source).combinations());
            assertSameNested(expected, LazyList.ofAll(source).combinations());
            assertSameNested(expected, Queue.ofAll(source).combinations());
            if (!source.isEmpty()) {
                assertSameNested(
                        expected, NonEmptyVector.unsafeFromVector(vector).combinations());
            }
        }
    }

    @Test
    void combinationsOfALongSequenceNearItsLength() {
        // the definition visits 2^n partial choices for k near n; the count alone is checked
        Vector<Integer> vector = Vector.range(0, 40);
        assertThat(vector.combinations(39).size()).isEqualTo(40);
        assertThat(vector.combinations(40)).containsExactly(vector);
        assertThat(List.ofAll(vector).combinations(38).size()).isEqualTo(780);
        assertThat(LazyList.ofAll(vector).combinations(39).size()).isEqualTo(40);
        assertThat(vector.combinations(2).last()).isEqualTo(Vector.of(38, 39));
    }

    // -- permutations

    @Test
    void permutationsAreThoseOfTheDefinition() {
        for (java.util.List<Box> source : sources(6, 3)) {
            Vector<Box> vector = Vector.ofAll(source);
            List<Box> list = List.ofAll(source);
            LazyList<Box> lazy = LazyList.ofAll(source);
            Vector<Vector<Box>> expected = ArrangementModels.permutations(vector);
            assertSameNested(expected, vector.permutations());
            assertThat(vector.permutations()).isEqualTo(expected);
            assertSameNested(ArrangementModels.permutations(list), list.permutations());
            assertSameNested(expected, ArrangementModels.permutations(list));
            assertSameNested(ArrangementModels.permutations(lazy), lazy.permutations());
            assertSameNested(expected, Queue.ofAll(source).permutations());
            if (!source.isEmpty()) {
                assertSameNested(
                        expected, NonEmptyVector.unsafeFromVector(vector).permutations());
            }
        }
    }

    @Test
    void permutationsOfSevenElementsWithRepeats() {
        Random random = new Random(SEED);
        for (int round = 0; round < 20; round++) {
            java.util.List<Box> source = boxes(random, 7, 2 + random.nextInt(5));
            Vector<Box> vector = Vector.ofAll(source);
            assertSameNested(ArrangementModels.permutations(vector), vector.permutations());
            List<Box> list = List.ofAll(source);
            assertSameNested(ArrangementModels.permutations(list), list.permutations());
        }
    }

    @Test
    void permutationsFollowTheFirstOccurrenceInWhatIsLeft() {
        // b, a, b: after the first b, the a comes before the second b
        assertThat(Vector.of("b", "a", "b").permutations())
                .containsExactly(Vector.of("b", "a", "b"), Vector.of("b", "b", "a"), Vector.of("a", "b", "b"));
        assertThat(List.of("b", "a", "b").permutations())
                .containsExactly(List.of("b", "a", "b"), List.of("b", "b", "a"), List.of("a", "b", "b"));
        assertThat(LazyList.of("b", "a", "b").permutations())
                .containsExactly(LazyList.of("b", "a", "b"), LazyList.of("b", "b", "a"), LazyList.of("a", "b", "b"));
    }

    @Test
    void theOnlyPermutationOfOneElementIsTheSequenceItself() {
        Vector<Integer> vector = Vector.of(1);
        assertThat(vector.permutations().head()).isSameAs(vector);
        List<Integer> list = List.of(1);
        assertThat(list.permutations().head()).isSameAs(list);
        LazyList<Integer> lazy = LazyList.of(1);
        assertThat(lazy.permutations().head()).isSameAs(lazy);
        assertThat(Vector.empty().permutations()).isEmpty();
        assertThat(List.empty().permutations()).isEmpty();
        assertThat(LazyList.empty().permutations()).isEmpty();
    }

    // -- rotateRight

    // distances for a sequence of n elements: 0, every one in [-2n - 1, 2n + 1], and the extremes
    private static java.util.List<Integer> distances(int n) {
        java.util.List<Integer> result = new ArrayList<>();
        for (int d = -2 * n - 1; d <= 2 * n + 1; d++) {
            result.add(d);
        }
        result.add(Integer.MIN_VALUE);
        result.add(Integer.MAX_VALUE);
        result.add(Integer.MIN_VALUE + 1);
        return result;
    }

    @Test
    void rotateRightIsThatOfTheDefinition() {
        Random random = new Random(SEED);
        for (int n : new int[] {0, 1, 2, 3, 5, 8, 31, 32, 33, 100}) {
            java.util.List<Box> source = boxes(random, n, 3);
            List<Box> list = List.ofAll(source);
            LazyList<Box> lazy = LazyList.ofAll(source);
            for (Queue<Box> queue : queues(source)) {
                for (int d : distances(n)) {
                    assertSameObjects(ArrangementModels.rotateRight(queue, d), queue.rotateRight(d));
                    assertThat(queue.rotateRight(d)).isEqualTo(ArrangementModels.rotateRight(queue, d));
                }
            }
            for (int d : distances(n)) {
                List<Box> expected = ArrangementModels.rotateRight(list, d);
                List<Box> actual = list.rotateRight(d);
                assertSameObjects(expected, actual);
                if (expected == list) {
                    assertThat(actual).isSameAs(list);
                }
                assertSameObjects(ArrangementModels.rotateRight(lazy, d), lazy.rotateRight(d));
            }
        }
    }

    @Test
    void rotateRightByAMultipleOfTheLengthReturnsTheReceiver() {
        List<Integer> list = List.of(1, 2, 3);
        assertThat(list.rotateRight(3)).isSameAs(list);
        assertThat(list.rotateRight(-6)).isSameAs(list);
        Queue<Integer> queue = Queue.of(1).enqueue(2).enqueue(3);
        assertThat(queue.rotateRight(3)).isSameAs(queue);
        assertThat(queue.rotateRight(0)).isSameAs(queue);
        LazyList<Integer> lazy = LazyList.of(1, 2, 3);
        LazyList<Integer> rotated = lazy.rotateRight(3);
        assertThat(rotated).containsExactly(1, 2, 3);
        assertThat(lazy.rotateRight(0)).isSameAs(lazy);
    }

    // -- the iterator of Queue

    // queues of the elements of `source`, with every split of them between the front and the back that a sequence of
    // enqueues and dequeues gives
    @SuppressWarnings("Var")
    private static java.util.List<Queue<Box>> queues(java.util.List<Box> source) {
        java.util.List<Queue<Box>> result = new ArrayList<>();
        result.add(Queue.ofAll(source));
        // one at a time: the first element in front, the others at the back
        result.add(source.stream().reduce(Queue.empty(), Queue::enqueue, (a, b) -> a));
        for (int split = 0; split <= source.size(); split++) {
            Queue<Box> queue = Queue.ofAll(source.subList(0, split));
            for (Box box : source.subList(split, source.size())) {
                queue = queue.enqueue(box);
            }
            result.add(queue);
        }
        // extra elements put in, then taken out after the back is moved to the front
        Queue<Box> mixed = Queue.of(new Box(-1)).enqueue(new Box(-2));
        int half = source.size() / 2;
        for (Box box : source.subList(0, half)) {
            mixed = mixed.enqueue(box);
        }
        mixed = mixed.tail().tail();
        for (Box box : source.subList(half, source.size())) {
            mixed = mixed.enqueue(box);
        }
        result.add(mixed);
        return result;
    }

    @Test
    void queueIteratesItsFrontThenItsBack() {
        Random random = new Random(SEED);
        for (int n : new int[] {0, 1, 2, 3, 31, 32, 33, 100}) {
            java.util.List<Box> source = boxes(random, n, Integer.MAX_VALUE);
            for (Queue<Box> queue : queues(source)) {
                Iterable<Box> viaIterator = queue::iterator;
                assertSameObjects(source, viaIterator);
                java.util.Iterator<Box> iterator = queue.iterator();
                for (int i = 0; i < n; i++) {
                    assertThat(iterator.hasNext()).isTrue();
                    assertThat(iterator.hasNext()).isTrue();
                    assertThat(iterator.next()).isSameAs(source.get(i));
                }
                assertThat(iterator.hasNext()).isFalse();
                assertThatThrownBy(iterator::next).isInstanceOf(NoSuchElementException.class);
                assertThat(iterator.hasNext()).isFalse();
            }
        }
    }

    @Test
    void queueIteratorsAreIndependent() {
        Queue<Integer> queue = Queue.of(1).enqueue(2).enqueue(3);
        java.util.Iterator<Integer> first = queue.iterator();
        java.util.Iterator<Integer> second = queue.iterator();
        assertThat(first.next()).isEqualTo(1);
        assertThat(first.next()).isEqualTo(2);
        assertThat(second.next()).isEqualTo(1);
        assertThat(first.next()).isEqualTo(3);
        assertThat(second.next()).isEqualTo(2);
        assertThat(second.next()).isEqualTo(3);
        assertThat(first.hasNext()).isFalse();
        assertThat(second.hasNext()).isFalse();
    }

    // -- LazyList: the cells read

    // 0, 1, ..., n - 1 as boxes of values mod `distinct`, counting its evaluated cells
    private static LazyList<Box> counted(int n, int distinct, AtomicInteger evaluated) {
        return LazyList.tabulate(n, i -> {
            evaluated.incrementAndGet();
            return new Box(i % distinct);
        });
    }

    // the cells read after the call, then after reading each element of the result and checking for the next
    private static java.util.List<Integer> trace(java.util.function.Function<AtomicInteger, LazyList<?>> call) {
        AtomicInteger evaluated = new AtomicInteger();
        LazyList<?> result = call.apply(evaluated);
        java.util.List<Integer> counts = new ArrayList<>();
        counts.add(evaluated.get());
        for (LazyList<?> cell = result; !cell.isEmpty(); cell = cell.tail()) {
            counts.add(evaluated.get());
            cell.head();
            counts.add(evaluated.get());
        }
        counts.add(evaluated.get());
        return counts;
    }

    @Test
    void lazyCombinationsReadTheCellsTheDefinitionReads() {
        for (int n = 0; n <= 8; n++) {
            for (int distinct : new int[] {1, 2, 100}) {
                int size = n;
                for (int k : ks(n)) {
                    int kk = Math.max(k, 0);
                    assertThat(trace(e -> counted(size, distinct, e).combinations(k)))
                            .as("n = %d, k = %d", n, k)
                            .isEqualTo(trace(e -> ArrangementModels.combinations(counted(size, distinct, e), kk)));
                }
                assertThat(trace(e -> counted(size, distinct, e).combinations()))
                        .isEqualTo(trace(e -> allCombinations(counted(size, distinct, e))));
            }
        }
    }

    // every combination of the definition, shortest first, the length counted when the result is first read
    private static LazyList<LazyList<Box>> allCombinations(LazyList<Box> source) {
        return LazyList.defer(() -> LazyList.rangeClosed(0, source.size())
                .map(k -> ArrangementModels.combinations(source, k))
                .flatMap(java.util.function.Function.identity()));
    }

    @Test
    void lazyPermutationsReadTheCellsTheDefinitionReads() {
        for (int n = 0; n <= 5; n++) {
            for (int distinct : new int[] {1, 2, 100}) {
                int size = n;
                assertThat(trace(e -> counted(size, distinct, e).permutations()))
                        .as("n = %d", n)
                        .isEqualTo(trace(e -> ArrangementModels.permutations(counted(size, distinct, e))));
            }
        }
    }

    @Test
    void lazyRotateRightReadsTheCellsTheDefinitionReads() {
        for (int n = 0; n <= 6; n++) {
            int size = n;
            for (int d : distances(n)) {
                assertThat(trace(e -> counted(size, 3, e).rotateRight(d)))
                        .as("n = %d, d = %d", n, d)
                        .isEqualTo(trace(e -> ArrangementModels.rotateRight(counted(size, 3, e), d)));
            }
        }
    }

    @Test
    void lazyCombinationsOfAnInfiniteList() {
        LazyList<Integer> naturals = LazyList.iterate(0, i -> i + 1);
        assertThat(naturals.combinations(2).take(4))
                .containsExactly(LazyList.of(0, 1), LazyList.of(0, 2), LazyList.of(0, 3), LazyList.of(0, 4));
        assertThat(naturals.combinations(1).take(3)).containsExactly(LazyList.of(0), LazyList.of(1), LazyList.of(2));
    }

    @Test
    void lazyCombinationsOfALongListKeepTheStackFlat() {
        LazyList<Integer> range = LazyList.range(0, 10_000);
        assertThat(range.combinations(1).size()).isEqualTo(10_000);
        assertThat(range.combinations(1).last()).containsExactly(9_999);
        LazyList<Integer> shorter = LazyList.range(0, 1_000);
        assertThat(shorter.combinations(2).size()).isEqualTo(1_000 * 999 / 2);
        assertThat(shorter.combinations(999).size()).isEqualTo(1_000);
    }
}
